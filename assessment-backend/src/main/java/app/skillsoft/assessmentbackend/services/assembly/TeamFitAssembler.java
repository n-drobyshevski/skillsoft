package app.skillsoft.assessmentbackend.services.assembly;

import app.skillsoft.assessmentbackend.domain.dto.blueprint.TeamFitBlueprint;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.TestBlueprintDto;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;
import app.skillsoft.assessmentbackend.domain.entities.BehavioralIndicator;
import app.skillsoft.assessmentbackend.domain.entities.DifficultyLevel;
import app.skillsoft.assessmentbackend.repository.BehavioralIndicatorRepository;
import app.skillsoft.assessmentbackend.services.external.TeamService;
import app.skillsoft.assessmentbackend.domain.dto.simulation.InventoryWarning;
import app.skillsoft.assessmentbackend.services.selection.QuestionSelectionService;
import app.skillsoft.assessmentbackend.services.selection.SelectionWarningCollector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Assembler for TEAM_FIT (Dynamic Gap Analysis) assessment strategy.
 *
 * Algorithm:
 * 1. Fetch Team Profile for the specified team ID
 * 2. Identify traits/competencies with saturation below threshold (default 0.3)
 * 3. These are the "gaps" the team needs to fill
 * 4. Select questions for those undersaturated competencies
 * 5. Focus on competencies where the candidate could add value to the team
 *
 * This implements Role Saturation analysis - finding where the team needs help.
 *
 * Refactored to use QuestionSelectionService for centralized question selection logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TeamFitAssembler implements TestAssembler {

    private final TeamService teamService;
    private final BehavioralIndicatorRepository indicatorRepository;
    private final QuestionSelectionService questionSelectionService;

    /**
     * Default saturation threshold for identifying team gaps.
     */
    private static final double DEFAULT_SATURATION_THRESHOLD = 0.3;

    /**
     * Default questions per undersaturated competency.
     */
    private static final int DEFAULT_QUESTIONS_PER_GAP = 4;

    /**
     * Default difficulty for TEAM_FIT assessments.
     * INTERMEDIATE provides balanced assessment for team gap analysis.
     */
    private static final DifficultyLevel DEFAULT_DIFFICULTY = DifficultyLevel.INTERMEDIATE;

    @Override
    public AssessmentGoal getSupportedGoal() {
        return AssessmentGoal.TEAM_FIT;
    }

    @Override
    public AssemblyResult assemble(TestBlueprintDto blueprint) {
        if (!(blueprint instanceof TeamFitBlueprint teamFitBlueprint)) {
            throw new IllegalArgumentException(
                "TeamFitAssembler requires TeamFitBlueprint, got: " +
                (blueprint != null ? blueprint.getClass().getSimpleName() : "null")
            );
        }

        var teamId = teamFitBlueprint.getTeamId();
        if (teamId == null) {
            log.warn("No team ID provided in TeamFitBlueprint");
            return AssemblyResult.empty();
        }

        var rawThreshold = teamFitBlueprint.getSaturationThreshold();
        final double saturationThreshold = (rawThreshold <= 0 || rawThreshold > 1)
            ? DEFAULT_SATURATION_THRESHOLD
            : rawThreshold;

        var canvasCompetencyIds = teamFitBlueprint.getCompetencyIds();
        boolean hasCanvas = canvasCompetencyIds != null && !canvasCompetencyIds.isEmpty();

        log.info("Assembling TEAM_FIT test for team: {} (threshold: {}, canvas competencies: {})",
            teamId, saturationThreshold, hasCanvas ? canvasCompetencyIds.size() : 0);

        // Step 1: Fetch team profile for saturation weighting
        var teamProfile = teamService.getTeamProfile(teamId);
        Map<UUID, Double> saturationLevels = teamProfile
            .map(tp -> tp.competencySaturation())
            .filter(m -> !m.isEmpty())
            .orElse(Map.of());

        log.info("Team profile saturation data: {} competencies", saturationLevels.size());

        // Step 2: Determine competencies to assess.
        // Canvas competencies are the primary source (user-selected, known to have questions).
        // Team saturation data is used for weighting/ordering only.
        List<UUID> targetCompetencies;

        if (hasCanvas) {
            // Use canvas competencies — apply team saturation as weighting
            targetCompetencies = new ArrayList<>(canvasCompetencyIds);

            // Canvas competencies not in team profile are treated as gaps (low saturation)
            var effectiveSaturation = new HashMap<>(saturationLevels);
            for (UUID compId : canvasCompetencyIds) {
                effectiveSaturation.putIfAbsent(compId, 0.1); // Unknown = assume critical gap
            }
            saturationLevels = effectiveSaturation;

            log.info("Using {} canvas competencies with team saturation weighting", canvasCompetencyIds.size());
        } else if (!saturationLevels.isEmpty()) {
            // No canvas — fall back to team profile undersaturated competencies
            targetCompetencies = saturationLevels.entrySet().stream()
                .filter(e -> e.getValue() < saturationThreshold)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(ArrayList::new));

            if (targetCompetencies.isEmpty()) {
                targetCompetencies = new ArrayList<>(saturationLevels.keySet());
            }

            log.info("No canvas competencies, using {} from team profile", targetCompetencies.size());
        } else {
            log.warn("No canvas competencies and no team profile data for team: {}", teamId);
            return AssemblyResult.withWarnings(List.of(
                InventoryWarning.info("No competency data available - add competencies to the canvas or ensure team members have completed assessments")
            ));
        }

        log.info("Target competencies for assembly: {} (team: {})",
            targetCompetencies.size(), teamId);

        // Step 3: Select questions for target competencies using QuestionSelectionService
        List<InventoryWarning> warnings = new ArrayList<>();

        SelectionWarningCollector.begin();
        try {
            var selectedQuestions = selectQuestionsForUndersaturatedCompetencies(
                targetCompetencies,
                saturationLevels
            );

            log.info("Assembled {} questions for TEAM_FIT assessment (team: {})",
                selectedQuestions.size(), teamId);

            warnings.addAll(SelectionWarningCollector.drain());
            return new AssemblyResult(selectedQuestions, warnings);
        } catch (Exception e) {
            SelectionWarningCollector.clear();
            throw e;
        }
    }

    /**
     * Select questions for undersaturated competencies, prioritizing by saturation level.
     * Lower saturation = more questions (more critical gap).
     * Uses QuestionSelectionService for centralized selection logic.
     */
    private List<UUID> selectQuestionsForUndersaturatedCompetencies(
            List<UUID> competencyIds,
            Map<UUID, Double> saturationLevels) {

        List<UUID> selectedQuestions = new ArrayList<>();
        Set<UUID> usedQuestions = new HashSet<>();

        // Sort competencies by saturation (lowest first - most critical gaps)
        var sortedCompetencies = competencyIds.stream()
            .sorted(Comparator.comparing(id -> saturationLevels.getOrDefault(id, 1.0)))
            .toList();

        // Batch-load all indicators for all competencies in a single query (N+1 fix)
        Map<UUID, List<BehavioralIndicator>> indicatorsByCompetency = indicatorRepository
            .findByCompetencyIdIn(new HashSet<>(competencyIds))
            .stream()
            .filter(BehavioralIndicator::isActive)
            .sorted(Comparator.comparing(BehavioralIndicator::getWeight).reversed())
            .collect(Collectors.groupingBy(ind -> ind.getCompetency().getId()));

        for (var competencyId : sortedCompetencies) {
            var indicators = indicatorsByCompetency.getOrDefault(competencyId, List.of());

            if (indicators.isEmpty()) {
                log.debug("No active indicators for competency {}", competencyId);
                continue;
            }

            // Calculate questions to select based on saturation
            // Lower saturation = more questions
            var saturation = saturationLevels.getOrDefault(competencyId, 1.0);
            var questionsToSelect = calculateQuestionsForSaturation(saturation);
            var difficulty = determineDifficultyForGap(saturation);

            log.debug("Competency {} saturation={}, selecting {} questions at {} difficulty",
                competencyId, String.format("%.2f", saturation), questionsToSelect, difficulty);

            // Select questions across indicators for this competency
            var questionsForCompetency = selectQuestionsAcrossIndicators(
                indicators,
                questionsToSelect,
                difficulty,
                usedQuestions
            );

            selectedQuestions.addAll(questionsForCompetency);
            usedQuestions.addAll(questionsForCompetency);
        }

        return selectedQuestions;
    }

    /**
     * Select questions across multiple indicators using priority-first distribution.
     */
    private List<UUID> selectQuestionsAcrossIndicators(
            List<BehavioralIndicator> indicators,
            int totalQuestions,
            DifficultyLevel difficulty,
            Set<UUID> usedQuestions) {

        List<UUID> selected = new ArrayList<>();
        int questionsPerIndicator = Math.max(1, totalQuestions / indicators.size());

        for (var indicator : indicators) {
            if (selected.size() >= totalQuestions) break;

            int toSelect = Math.min(questionsPerIndicator, totalQuestions - selected.size());

            // Use QuestionSelectionService for psychometric validation and difficulty preference
            List<UUID> questions = questionSelectionService.selectQuestionsForIndicator(
                indicator.getId(),
                toSelect,
                difficulty,
                usedQuestions
            );

            selected.addAll(questions);
            usedQuestions.addAll(questions);
        }

        return selected;
    }

    /**
     * Calculate how many questions to select based on saturation level.
     * Lower saturation means more questions needed to assess the gap.
     */
    private int calculateQuestionsForSaturation(double saturation) {
        if (saturation < 0.1) {
            return DEFAULT_QUESTIONS_PER_GAP + 2; // Critical gap
        } else if (saturation < 0.3) {
            return DEFAULT_QUESTIONS_PER_GAP;     // Moderate gap
        } else if (saturation < 0.5) {
            return DEFAULT_QUESTIONS_PER_GAP - 1; // Minor gap
        } else {
            return DEFAULT_QUESTIONS_PER_GAP - 2; // Minimal gap
        }
    }

    /**
     * Determine question difficulty based on gap severity.
     * Deeper gaps (lower saturation) get harder questions to better discriminate candidate ability.
     *
     * @param saturation Team saturation level (0.0-1.0) for this competency
     * @return Appropriate difficulty level
     */
    private DifficultyLevel determineDifficultyForGap(double saturation) {
        if (saturation < 0.1) {
            return DifficultyLevel.ADVANCED;        // Critical gap: need advanced questions to discriminate
        } else if (saturation < 0.3) {
            return DifficultyLevel.INTERMEDIATE;    // Moderate gap: balanced assessment
        } else {
            return DifficultyLevel.FOUNDATIONAL;    // Minor gap: basic screening sufficient
        }
    }
}
