package app.skillsoft.assessmentbackend.services.assembly;

import app.skillsoft.assessmentbackend.domain.dto.blueprint.OverviewBlueprint;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.TestBlueprintDto;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;
import app.skillsoft.assessmentbackend.domain.entities.DifficultyLevel;
import app.skillsoft.assessmentbackend.services.selection.QuestionSelectionService;
import app.skillsoft.assessmentbackend.util.LoggingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

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
 *
 * Refactored to use QuestionSelectionService for centralized question selection logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OverviewAssembler implements TestAssembler {

    private final QuestionSelectionService questionSelectionService;

    /**
     * Default number of questions per indicator if not specified.
     */
    private static final int DEFAULT_QUESTIONS_PER_INDICATOR = 3;

    /**
     * Default difficulty for OVERVIEW assessments.
     * INTERMEDIATE provides balanced assessment suitable for baseline measurement.
     */
    private static final DifficultyLevel DEFAULT_DIFFICULTY = DifficultyLevel.INTERMEDIATE;

    @Override
    public AssessmentGoal getSupportedGoal() {
        return AssessmentGoal.OVERVIEW;
    }

    @Override
    public AssemblyResult assemble(TestBlueprintDto blueprint) {
        // Set operation context for assembly logging
        LoggingContext.setOperation("overview-assembly");

        if (!(blueprint instanceof OverviewBlueprint overviewBlueprint)) {
            log.error("Invalid blueprint type for OverviewAssembler: expected=OverviewBlueprint actual={}",
                    blueprint != null ? blueprint.getClass().getSimpleName() : "null");
            throw new IllegalArgumentException(
                "OverviewAssembler requires OverviewBlueprint, got: " +
                (blueprint != null ? blueprint.getClass().getSimpleName() : "null")
            );
        }

        var competencyIds = overviewBlueprint.getCompetencyIds();
        if (competencyIds == null || competencyIds.isEmpty()) {
            log.warn("No competency IDs provided in OverviewBlueprint");
            return AssemblyResult.empty();
        }

        log.info("Assembling OVERVIEW test: competencies={} count={}",
                competencyIds, competencyIds.size());

        // Determine questions per indicator from blueprint or use default
        int questionsPerIndicator = overviewBlueprint.getQuestionsPerIndicator() > 0
                ? overviewBlueprint.getQuestionsPerIndicator()
                : DEFAULT_QUESTIONS_PER_INDICATOR;

        // Determine difficulty preference from blueprint or use default
        DifficultyLevel preferredDifficulty = overviewBlueprint.getPreferredDifficulty() != null
                ? overviewBlueprint.getPreferredDifficulty()
                : DEFAULT_DIFFICULTY;

        // Determine shuffle preference from blueprint
        boolean shuffle = overviewBlueprint.isShuffleQuestions();

        // Use QuestionSelectionService for centralized selection logic
        // This applies:
        // - Psychometric validation (excludes RETIRED items)
        // - Context neutrality filtering (OVERVIEW requires context-neutral items)
        // - WATERFALL distribution across indicators
        // - Difficulty preference with fallback
        // - Optional shuffling
        List<UUID> selectedQuestions = questionSelectionService.selectQuestionsForCompetencies(
                competencyIds,
                questionsPerIndicator,
                preferredDifficulty,
                shuffle,
                true // contextNeutralOnly: OVERVIEW assessments require context-neutral items
        );

        log.info("Assembled {} questions for OVERVIEW assessment (competencies: {}, perIndicator: {})",
                selectedQuestions.size(), competencyIds.size(), questionsPerIndicator);

        return AssemblyResult.of(selectedQuestions);
    }
}
