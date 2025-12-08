package app.skillsoft.assessmentbackend.services.assembly;

import app.skillsoft.assessmentbackend.domain.dto.blueprint.OverviewBlueprint;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.TestBlueprintDto;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentQuestion;
import app.skillsoft.assessmentbackend.domain.entities.BehavioralIndicator;
import app.skillsoft.assessmentbackend.domain.entities.DifficultyLevel;
import app.skillsoft.assessmentbackend.repository.AssessmentQuestionRepository;
import app.skillsoft.assessmentbackend.repository.BehavioralIndicatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Assembler for OVERVIEW (Universal Baseline) assessment strategy.
 * 
 * Algorithm:
 * 1. Fetch all BehavioralIndicators for the specified competencies
 * 2. Sort indicators by weight (descending) for prioritization
 * 3. Select questions with INTERMEDIATE difficulty for balanced assessment
 * 4. Apply "Waterfall" distribution: cycle through indicators to ensure coverage
 *    (Indicator 1, Indicator 2, Indicator 1, Indicator 2, ...)
 * 
 * This creates a balanced assessment suitable for generating a Competency Passport.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OverviewAssembler implements TestAssembler {

    private final BehavioralIndicatorRepository indicatorRepository;
    private final AssessmentQuestionRepository questionRepository;

    /**
     * Default number of questions per indicator if not specified.
     */
    private static final int DEFAULT_QUESTIONS_PER_INDICATOR = 3;

    @Override
    public AssessmentGoal getSupportedGoal() {
        return AssessmentGoal.OVERVIEW;
    }

    @Override
    public List<UUID> assemble(TestBlueprintDto blueprint) {
        if (!(blueprint instanceof OverviewBlueprint overviewBlueprint)) {
            throw new IllegalArgumentException(
                "OverviewAssembler requires OverviewBlueprint, got: " + 
                (blueprint != null ? blueprint.getClass().getSimpleName() : "null")
            );
        }

        var competencyIds = overviewBlueprint.getCompetencyIds();
        if (competencyIds == null || competencyIds.isEmpty()) {
            log.warn("No competency IDs provided in OverviewBlueprint");
            return List.of();
        }

        log.info("Assembling OVERVIEW test for {} competencies", competencyIds.size());

        // Step 1: Fetch all indicators for the competencies, sorted by weight DESC
        var allIndicators = fetchIndicatorsSortedByWeight(competencyIds);
        
        if (allIndicators.isEmpty()) {
            log.warn("No behavioral indicators found for competencies: {}", competencyIds);
            return List.of();
        }

        log.debug("Found {} indicators across {} competencies", 
            allIndicators.size(), competencyIds.size());

        // Step 2: Collect available questions for each indicator (INTERMEDIATE difficulty)
        var indicatorQuestions = collectQuestionsForIndicators(allIndicators);

        // Step 3: Apply waterfall distribution to select questions
        var selectedQuestions = applyWaterfallDistribution(
            allIndicators, 
            indicatorQuestions,
            DEFAULT_QUESTIONS_PER_INDICATOR
        );

        log.info("Assembled {} questions for OVERVIEW assessment", selectedQuestions.size());
        
        return selectedQuestions;
    }

    /**
     * Fetch behavioral indicators for competencies, sorted by weight descending.
     */
    private List<BehavioralIndicator> fetchIndicatorsSortedByWeight(List<UUID> competencyIds) {
        return competencyIds.stream()
            .flatMap(compId -> indicatorRepository.findByCompetencyId(compId).stream())
            .filter(BehavioralIndicator::isActive)
            .sorted(Comparator.comparing(BehavioralIndicator::getWeight).reversed())
            .collect(Collectors.toList());
    }

    /**
     * Collect questions for each indicator, preferring INTERMEDIATE difficulty.
     */
    private Map<UUID, List<UUID>> collectQuestionsForIndicators(List<BehavioralIndicator> indicators) {
        var result = new HashMap<UUID, List<UUID>>();
        
        for (var indicator : indicators) {
            var questions = questionRepository.findByBehavioralIndicator_Id(indicator.getId())
                .stream()
                .filter(AssessmentQuestion::isActive)
                // Prefer INTERMEDIATE, but include others as fallback
                .sorted(Comparator.comparing(q -> 
                    q.getDifficultyLevel() == DifficultyLevel.INTERMEDIATE ? 0 : 1))
                .map(AssessmentQuestion::getId)
                .collect(Collectors.toList());
            
            result.put(indicator.getId(), questions);
        }
        
        return result;
    }

    /**
     * Apply waterfall distribution to select questions across indicators.
     * 
     * The waterfall pattern ensures each indicator gets questions in rotation:
     * Round 1: Indicator1-Q1, Indicator2-Q1, Indicator3-Q1...
     * Round 2: Indicator1-Q2, Indicator2-Q2, Indicator3-Q2...
     * 
     * This ensures balanced coverage even if we hit limits early.
     */
    private List<UUID> applyWaterfallDistribution(
            List<BehavioralIndicator> indicators,
            Map<UUID, List<UUID>> indicatorQuestions,
            int questionsPerIndicator
    ) {
        var selectedQuestions = new ArrayList<UUID>();
        var usedQuestions = new HashSet<UUID>();
        var indicatorCursors = new HashMap<UUID, Integer>();
        
        // Initialize cursors
        indicators.forEach(ind -> indicatorCursors.put(ind.getId(), 0));
        
        // Waterfall: iterate round by round
        for (int round = 0; round < questionsPerIndicator; round++) {
            for (var indicator : indicators) {
                var questions = indicatorQuestions.getOrDefault(indicator.getId(), List.of());
                var cursor = indicatorCursors.get(indicator.getId());
                
                // Find next available question for this indicator
                while (cursor < questions.size()) {
                    var questionId = questions.get(cursor);
                    cursor++;
                    
                    if (!usedQuestions.contains(questionId)) {
                        selectedQuestions.add(questionId);
                        usedQuestions.add(questionId);
                        break;
                    }
                }
                
                indicatorCursors.put(indicator.getId(), cursor);
            }
        }
        
        return selectedQuestions;
    }
}
