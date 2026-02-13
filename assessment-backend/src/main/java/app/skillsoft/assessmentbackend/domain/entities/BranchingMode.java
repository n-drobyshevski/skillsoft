package app.skillsoft.assessmentbackend.domain.entities;

/**
 * Question branching/flow modes for test execution.
 * Determines how questions are presented and how the test adapts to responses.
 * 
 * Part of the AdaptivitySettings in TestBlueprint configuration.
 */
public enum BranchingMode {
    /**
     * Sequential question flow with no adaptive behavior.
     * All questions are presented in predefined order.
     * Suitable for standardized assessments.
     */
    LINEAR("Linear", "Questions presented sequentially without adaptation"),
    
    /**
     * Adaptive testing with standard difficulty adjustment.
     * Question difficulty adjusts based on response accuracy.
     * Implements Item Response Theory (IRT) for optimal question selection.
     */
    ADAPTIVE_STANDARD("Adaptive Standard", "Moderate adaptation based on response patterns"),
    
    /**
     * Strict adaptive mode with aggressive difficulty scaling.
     * Quickly escalates to challenging questions on correct answers.
     * Designed for precise skill ceiling detection.
     */
    RUTHLESS("Ruthless", "Aggressive adaptation for precise skill ceiling detection");

    private final String displayName;
    private final String description;

    BranchingMode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if this mode uses adaptive question selection.
     * @return true if the mode implements adaptive behavior
     */
    public boolean isAdaptive() {
        return this != LINEAR;
    }
}
