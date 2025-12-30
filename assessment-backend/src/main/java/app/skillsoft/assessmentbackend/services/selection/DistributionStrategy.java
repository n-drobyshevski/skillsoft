package app.skillsoft.assessmentbackend.services.selection;

/**
 * Distribution strategy for selecting questions across multiple indicators.
 *
 * Used by QuestionSelectionService to determine how questions are allocated
 * when assembling tests that span multiple behavioral indicators.
 */
public enum DistributionStrategy {

    /**
     * Waterfall distribution - even allocation across all indicators.
     *
     * Cycles through indicators in rounds:
     * Round 1: Indicator1-Q1, Indicator2-Q1, Indicator3-Q1...
     * Round 2: Indicator1-Q2, Indicator2-Q2, Indicator3-Q2...
     *
     * Ensures balanced coverage even if total question limit is reached early.
     * Used by OVERVIEW (Universal Baseline) assessments.
     */
    WATERFALL,

    /**
     * Weighted distribution - allocation based on indicator weights.
     *
     * Higher-weighted indicators receive proportionally more questions.
     * Useful for gap-based assessments where some areas need more attention.
     * Used by JOB_FIT (Delta Testing) assessments.
     */
    WEIGHTED,

    /**
     * Priority-first distribution - fill priority indicators before others.
     *
     * Sorted by priority (e.g., saturation level), most critical first.
     * Each priority indicator gets its full allocation before moving to next.
     * Used by TEAM_FIT (Role Saturation) assessments.
     */
    PRIORITY_FIRST
}
