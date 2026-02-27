package app.skillsoft.assessmentbackend.services.selection;

import app.skillsoft.assessmentbackend.domain.entities.AssessmentQuestion;
import app.skillsoft.assessmentbackend.domain.entities.DifficultyLevel;
import app.skillsoft.assessmentbackend.domain.entities.ItemValidityStatus;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Service for selecting questions for test assembly.
 *
 * Consolidates common question selection logic previously scattered across:
 * - TestSessionServiceImpl (legacy methods)
 * - OverviewAssembler
 * - JobFitAssembler
 * - TeamFitAssembler
 *
 * Responsibilities:
 * - Question filtering by indicator/competency
 * - Psychometric validation (RETIRED exclusion)
 * - Difficulty level preference with fallback
 * - Distribution strategies (waterfall, weighted, priority-first)
 *
 * @see DistributionStrategy
 */
public interface QuestionSelectionService {

    // ========== SINGLE INDICATOR SELECTION ==========

    /**
     * Select questions for a specific behavioral indicator with constraints.
     *
     * Selection criteria:
     * 1. Questions must be active (is_active = true)
     * 2. Questions must pass psychometric validation (not RETIRED)
     * 3. Prefer questions matching the specified difficulty
     * 4. Fall back to other difficulties if preferred not available
     * 5. Exclude any previously selected questions
     *
     * @param indicatorId         The behavioral indicator UUID
     * @param maxQuestions        Maximum number of questions to select
     * @param preferredDifficulty Preferred difficulty level (null for any)
     * @param excludeQuestionIds  Question IDs to exclude (already selected)
     * @return List of selected question UUIDs (may be fewer than maxQuestions if insufficient)
     */
    List<UUID> selectQuestionsForIndicator(
            UUID indicatorId,
            int maxQuestions,
            DifficultyLevel preferredDifficulty,
            Set<UUID> excludeQuestionIds
    );

    /**
     * Select questions for an indicator without exclusions.
     * Convenience overload for initial selection.
     */
    default List<UUID> selectQuestionsForIndicator(
            UUID indicatorId,
            int maxQuestions,
            DifficultyLevel preferredDifficulty
    ) {
        return selectQuestionsForIndicator(indicatorId, maxQuestions, preferredDifficulty, Set.of());
    }

    /**
     * Select questions for an indicator with any difficulty.
     * Convenience overload for non-difficulty-constrained selection.
     */
    default List<UUID> selectQuestionsForIndicator(UUID indicatorId, int maxQuestions) {
        return selectQuestionsForIndicator(indicatorId, maxQuestions, null, Set.of());
    }

    // ========== MULTI-INDICATOR DISTRIBUTION ==========

    /**
     * Select questions across multiple indicators with a distribution strategy.
     *
     * Distribution strategies:
     * - WATERFALL: Even allocation, cycling through indicators in rounds
     * - WEIGHTED: Proportional to indicator weights
     * - PRIORITY_FIRST: Fill highest-priority indicators first
     *
     * @param indicatorIds     List of behavioral indicator UUIDs
     * @param totalQuestions   Total number of questions to select
     * @param questionsPerRound Questions per indicator per round (for WATERFALL)
     * @param strategy         Distribution strategy to use
     * @param preferredDifficulty Preferred difficulty (null for any)
     * @return List of selected question UUIDs
     */
    List<UUID> selectQuestionsWithDistribution(
            List<UUID> indicatorIds,
            int totalQuestions,
            int questionsPerRound,
            DistributionStrategy strategy,
            DifficultyLevel preferredDifficulty
    );

    /**
     * Select questions with distribution using indicator weights for allocation.
     *
     * Used for WEIGHTED distribution where each indicator has an associated weight.
     * Higher weights result in proportionally more questions.
     *
     * @param indicatorWeights Map of indicator UUIDs to their weights
     * @param totalQuestions   Total number of questions to select
     * @param preferredDifficulty Preferred difficulty (null for any)
     * @return List of selected question UUIDs
     */
    List<UUID> selectQuestionsWeighted(
            Map<UUID, Double> indicatorWeights,
            int totalQuestions,
            DifficultyLevel preferredDifficulty
    );

    // ========== COMPETENCY-LEVEL SELECTION ==========

    /**
     * Select questions for a competency (via all its indicators).
     *
     * Fetches all active behavioral indicators for the competency,
     * then applies the specified distribution strategy across them.
     *
     * @param competencyId        The competency UUID
     * @param totalQuestions      Total questions to select
     * @param questionsPerIndicator Questions per indicator (for even distribution)
     * @param preferredDifficulty Preferred difficulty (null for any)
     * @return List of selected question UUIDs
     */
    List<UUID> selectQuestionsForCompetency(
            UUID competencyId,
            int totalQuestions,
            int questionsPerIndicator,
            DifficultyLevel preferredDifficulty
    );

    /**
     * Select questions for multiple competencies with waterfall distribution.
     *
     * Used by OVERVIEW assessments for balanced coverage across competencies.
     *
     * @param competencyIds       List of competency UUIDs
     * @param questionsPerIndicator Questions per indicator
     * @param preferredDifficulty Preferred difficulty (null for any)
     * @param shuffle            Whether to shuffle the final selection
     * @return List of selected question UUIDs
     */
    List<UUID> selectQuestionsForCompetencies(
            List<UUID> competencyIds,
            int questionsPerIndicator,
            DifficultyLevel preferredDifficulty,
            boolean shuffle
    );

    /**
     * Select questions for multiple competencies with waterfall distribution
     * and optional context neutrality filtering.
     *
     * When contextNeutralOnly is true, only questions tagged as GENERAL/UNIVERSAL
     * (or without narrow domain tags) are selected. This is critical for OVERVIEW
     * assessments generating a Competency Passport with construct validity.
     *
     * @param competencyIds        List of competency UUIDs
     * @param questionsPerIndicator Questions per indicator
     * @param preferredDifficulty  Preferred difficulty (null for any)
     * @param shuffle              Whether to shuffle the final selection
     * @param contextNeutralOnly   If true, apply context neutrality filter
     * @return List of selected question UUIDs
     */
    List<UUID> selectQuestionsForCompetencies(
            List<UUID> competencyIds,
            int questionsPerIndicator,
            DifficultyLevel preferredDifficulty,
            boolean shuffle,
            boolean contextNeutralOnly
    );

    /**
     * Select questions for multiple competencies with weighted distribution.
     *
     * Translates competency-level weights to indicator-level weights by multiplying
     * each indicator's intrinsic weight by its parent competency's weight.
     * Then delegates to {@link #selectQuestionsWeighted} for proportional allocation.
     *
     * @param competencyIds          List of competency UUIDs
     * @param competencyWeights      Map of competency UUID to weight multiplier (0.5-2.0)
     * @param questionsPerIndicator  Base questions per indicator (used for total calculation)
     * @param preferredDifficulty    Preferred difficulty (null for any)
     * @param shuffle                Whether to shuffle the final selection
     * @param contextNeutralOnly     If true, apply context neutrality filter
     * @return List of selected question UUIDs
     */
    List<UUID> selectQuestionsForCompetenciesWeighted(
            List<UUID> competencyIds,
            Map<UUID, Double> competencyWeights,
            int questionsPerIndicator,
            DifficultyLevel preferredDifficulty,
            boolean shuffle,
            boolean contextNeutralOnly
    );

    // ========== FILTERING UTILITIES ==========

    /**
     * Filter questions by psychometric validity status.
     *
     * Uses PsychometricBlueprintValidator to check each question's status.
     * By default, excludes RETIRED items.
     *
     * @param questions       List of questions to filter
     * @param allowedStatuses Set of allowed validity statuses
     * @return Filtered list of questions
     */
    List<AssessmentQuestion> filterByValidity(
            List<AssessmentQuestion> questions,
            Set<ItemValidityStatus> allowedStatuses
    );

    /**
     * Filter questions excluding RETIRED items (default psychometric filter).
     */
    default List<AssessmentQuestion> filterByValidity(List<AssessmentQuestion> questions) {
        return filterByValidity(questions, Set.of(
                ItemValidityStatus.ACTIVE,
                ItemValidityStatus.PROBATION,
                ItemValidityStatus.FLAGGED_FOR_REVIEW
        ));
    }

    /**
     * Apply difficulty preference with fallback to other difficulties.
     *
     * Returns questions in priority order:
     * 1. Questions matching preferred difficulty
     * 2. Questions with adjacent difficulties (if fallback enabled)
     * 3. All other questions
     *
     * @param questions  List of questions to sort
     * @param preferred  Preferred difficulty level
     * @return Sorted list with preferred difficulty first
     */
    List<AssessmentQuestion> applyDifficultyPreference(
            List<AssessmentQuestion> questions,
            DifficultyLevel preferred
    );

    /**
     * Filter questions by context neutrality (for OVERVIEW assessments).
     *
     * Returns questions that are context-neutral (GENERAL tag or no restrictive tags).
     * Used for Competency Passport assessments requiring construct validity.
     *
     * @param questions List of questions to filter
     * @return Filtered list of context-neutral questions
     */
    List<AssessmentQuestion> filterByContextNeutrality(List<AssessmentQuestion> questions);

    // ========== REPRODUCIBLE RANDOM (BE-008) ==========

    /**
     * Set a session-based seed for reproducible question ordering.
     *
     * When set, all subsequent shuffle operations in this thread will use
     * a {@code Random} seeded with {@code sessionId.getMostSignificantBits()},
     * producing the same question order for the same session ID.
     *
     * Must be paired with {@link #clearSessionSeed()} after question generation
     * completes (use try/finally) to prevent leaking state across pooled threads.
     *
     * @param sessionId The session UUID used to derive the seed
     */
    void setSessionSeed(UUID sessionId);

    /**
     * Clear the session-based seed, reverting to non-deterministic shuffling.
     * Must be called after {@link #setSessionSeed(UUID)} to prevent thread leaks.
     */
    void clearSessionSeed();

    // ========== ELIGIBILITY CHECKS ==========

    /**
     * Check if a question is eligible for test assembly.
     *
     * A question is eligible if:
     * 1. It exists in the database
     * 2. It is active (is_active = true)
     * 3. It is not RETIRED (psychometric validation)
     *
     * @param questionId The question UUID to check
     * @return true if eligible for assembly
     */
    boolean isEligibleForAssembly(UUID questionId);

    /**
     * Get eligible question count for an indicator.
     *
     * @param indicatorId The behavioral indicator UUID
     * @return Count of eligible questions
     */
    int getEligibleQuestionCount(UUID indicatorId);

    /**
     * Get eligible question count for a competency (all its indicators).
     *
     * @param competencyId The competency UUID
     * @return Count of eligible questions
     */
    int getEligibleQuestionCountForCompetency(UUID competencyId);
}
