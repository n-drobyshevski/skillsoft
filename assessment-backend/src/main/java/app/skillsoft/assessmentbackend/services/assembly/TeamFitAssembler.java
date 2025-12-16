package app.skillsoft.assessmentbackend.services.assembly;

import app.skillsoft.assessmentbackend.domain.dto.blueprint.TeamFitBlueprint;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.TestBlueprintDto;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentQuestion;
import app.skillsoft.assessmentbackend.domain.entities.BehavioralIndicator;
import app.skillsoft.assessmentbackend.repository.AssessmentQuestionRepository;
import app.skillsoft.assessmentbackend.repository.BehavioralIndicatorRepository;
import app.skillsoft.assessmentbackend.services.external.TeamService;
import app.skillsoft.assessmentbackend.services.validation.PsychometricBlueprintValidator;
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
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TeamFitAssembler implements TestAssembler {

    private final TeamService teamService;
    private final BehavioralIndicatorRepository indicatorRepository;
    private final AssessmentQuestionRepository questionRepository;
    private final PsychometricBlueprintValidator psychometricValidator;

    /**
     * Default saturation threshold for identifying team gaps.
     */
    private static final double DEFAULT_SATURATION_THRESHOLD = 0.3;

    /**
     * Default questions per undersaturated competency.
     */
    private static final int DEFAULT_QUESTIONS_PER_GAP = 4;

    @Override
    public AssessmentGoal getSupportedGoal() {
        return AssessmentGoal.TEAM_FIT;
    }

    @Override
    public List<UUID> assemble(TestBlueprintDto blueprint) {
        if (!(blueprint instanceof TeamFitBlueprint teamFitBlueprint)) {
            throw new IllegalArgumentException(
                "TeamFitAssembler requires TeamFitBlueprint, got: " + 
                (blueprint != null ? blueprint.getClass().getSimpleName() : "null")
            );
        }

        var teamId = teamFitBlueprint.getTeamId();
        if (teamId == null) {
            log.warn("No team ID provided in TeamFitBlueprint");
            return List.of();
        }

        var saturationThreshold = teamFitBlueprint.getSaturationThreshold();
        if (saturationThreshold <= 0 || saturationThreshold > 1) {
            saturationThreshold = DEFAULT_SATURATION_THRESHOLD;
        }

        log.info("Assembling TEAM_FIT test for team: {} (threshold: {})", 
            teamId, saturationThreshold);

        // Step 1: Fetch team profile
        var teamProfile = teamService.getTeamProfile(teamId);
        if (teamProfile.isEmpty()) {
            log.warn("No team profile found for team: {}", teamId);
            return List.of();
        }

        // Step 2: Get undersaturated competencies (team gaps)
        var undersaturatedCompetencies = teamService.getUndersaturatedCompetencies(
            teamId, 
            saturationThreshold
        );

        if (undersaturatedCompetencies.isEmpty()) {
            log.info("No undersaturated competencies found for team: {}", teamId);
            // Fall back to all competencies from the team profile
            undersaturatedCompetencies = new ArrayList<>(
                teamProfile.get().competencySaturation().keySet()
            );
        }

        log.debug("Found {} undersaturated competencies for team: {}", 
            undersaturatedCompetencies.size(), teamId);

        // Step 3: Select questions for undersaturated competencies
        var selectedQuestions = selectQuestionsForCompetencies(
            undersaturatedCompetencies,
            teamProfile.get().competencySaturation()
        );

        log.info("Assembled {} questions for TEAM_FIT assessment (team: {})", 
            selectedQuestions.size(), teamId);

        return selectedQuestions;
    }

    /**
     * Select questions for the specified competencies, prioritizing by saturation level.
     * Lower saturation = more questions (more critical gap).
     * Uses psychometric validation to exclude RETIRED items and prioritize validated questions.
     */
    private List<UUID> selectQuestionsForCompetencies(
            List<UUID> competencyIds,
            Map<UUID, Double> saturationLevels
    ) {
        var selectedQuestions = new ArrayList<UUID>();
        var usedQuestions = new HashSet<UUID>();

        // Sort competencies by saturation (lowest first - most critical gaps)
        var sortedCompetencies = competencyIds.stream()
            .sorted(Comparator.comparing(id -> saturationLevels.getOrDefault(id, 1.0)))
            .toList();

        for (var competencyId : sortedCompetencies) {
            // Get indicators for this competency
            var indicators = indicatorRepository.findByCompetencyId(competencyId)
                .stream()
                .filter(BehavioralIndicator::isActive)
                .sorted(Comparator.comparing(BehavioralIndicator::getWeight).reversed())
                .toList();

            // Calculate questions to select based on saturation
            // Lower saturation = more questions
            var saturation = saturationLevels.getOrDefault(competencyId, 1.0);
            var questionsToSelect = calculateQuestionsForSaturation(saturation);

            // Get questions from indicators
            var questionsSelected = 0;
            for (var indicator : indicators) {
                if (questionsSelected >= questionsToSelect) break;

                var questions = questionRepository.findByBehavioralIndicator_Id(indicator.getId())
                    .stream()
                    .filter(AssessmentQuestion::isActive)
                    // Filter using psychometric validation (excludes RETIRED items)
                    .filter(q -> psychometricValidator.isEligibleForAssembly(q.getId()))
                    .map(AssessmentQuestion::getId)
                    .toList();

                for (var questionId : questions) {
                    if (!usedQuestions.contains(questionId) && questionsSelected < questionsToSelect) {
                        selectedQuestions.add(questionId);
                        usedQuestions.add(questionId);
                        questionsSelected++;
                    }
                }
            }
        }

        return selectedQuestions;
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
}
