package app.skillsoft.assessmentbackend.domain.dto.simulation;

/**
 * Simulation profile for test dry runs.
 * Represents different candidate personas for simulation testing.
 */
public enum SimulationProfile {
    /**
     * Perfect candidate: Answers all questions correctly.
     * Used to verify maximum score paths and time estimates.
     */
    PERFECT_CANDIDATE("Perfect Candidate", "Always selects the correct answer"),
    
    /**
     * Random guesser: 50% chance of correct answers.
     * Used to test average-case scenarios and statistical validity.
     */
    RANDOM_GUESSER("Random Guesser", "50% probability of correct answer"),
    
    /**
     * Failing candidate: Always selects incorrect answers.
     * Used to verify minimum score paths and failure handling.
     */
    FAILING_CANDIDATE("Failing Candidate", "Always selects incorrect answer");

    private final String displayName;
    private final String description;

    SimulationProfile(String displayName, String description) {
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
     * Get the probability of a correct answer for this profile.
     * 
     * @return Probability between 0.0 and 1.0
     */
    public double getCorrectAnswerProbability() {
        return switch (this) {
            case PERFECT_CANDIDATE -> 1.0;
            case RANDOM_GUESSER -> 0.5;
            case FAILING_CANDIDATE -> 0.0;
        };
    }
}
