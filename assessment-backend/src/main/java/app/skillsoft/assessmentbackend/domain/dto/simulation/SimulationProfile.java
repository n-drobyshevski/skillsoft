package app.skillsoft.assessmentbackend.domain.dto.simulation;

import app.skillsoft.assessmentbackend.domain.entities.DifficultyLevel;

import java.util.Map;

/**
 * Simulation profile for test dry runs.
 * Each persona defines an IRT-inspired difficulty response curve
 * mapping DifficultyLevel to a base probability of correct answer.
 */
public enum SimulationProfile {

    PERFECT_CANDIDATE(
        "Perfect Candidate",
        "High-ability candidate; strong across all difficulty levels",
        Map.of(
            DifficultyLevel.FOUNDATIONAL,  0.98,
            DifficultyLevel.INTERMEDIATE,  0.95,
            DifficultyLevel.ADVANCED,      0.90,
            DifficultyLevel.EXPERT,        0.85,
            DifficultyLevel.SPECIALIZED,   0.80
        )
    ),

    RANDOM_GUESSER(
        "Random Guesser",
        "Average candidate; moderate accuracy that drops with difficulty",
        Map.of(
            DifficultyLevel.FOUNDATIONAL,  0.70,
            DifficultyLevel.INTERMEDIATE,  0.55,
            DifficultyLevel.ADVANCED,      0.40,
            DifficultyLevel.EXPERT,        0.25,
            DifficultyLevel.SPECIALIZED,   0.20
        )
    ),

    FAILING_CANDIDATE(
        "Failing Candidate",
        "Low-ability candidate; struggles with most content",
        Map.of(
            DifficultyLevel.FOUNDATIONAL,  0.30,
            DifficultyLevel.INTERMEDIATE,  0.20,
            DifficultyLevel.ADVANCED,      0.10,
            DifficultyLevel.EXPERT,        0.05,
            DifficultyLevel.SPECIALIZED,   0.03
        )
    );

    private final String displayName;
    private final String description;
    private final Map<DifficultyLevel, Double> baseProbabilities;

    SimulationProfile(String displayName, String description,
                      Map<DifficultyLevel, Double> baseProbabilities) {
        this.displayName = displayName;
        this.description = description;
        this.baseProbabilities = baseProbabilities;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public Map<DifficultyLevel, Double> getBaseProbabilities() {
        return baseProbabilities;
    }

    /**
     * Get base probability for a specific difficulty level.
     * Falls back to 0.50 if difficulty is somehow unmapped.
     */
    public double getBaseProbability(DifficultyLevel difficulty) {
        return baseProbabilities.getOrDefault(difficulty, 0.50);
    }

    /**
     * @deprecated Use {@link #getBaseProbability(DifficultyLevel)} instead.
     * Returns the INTERMEDIATE base probability as a flat approximation.
     */
    @Deprecated(forRemoval = true)
    public double getCorrectAnswerProbability() {
        return getBaseProbability(DifficultyLevel.INTERMEDIATE);
    }
}
