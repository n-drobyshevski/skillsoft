package app.skillsoft.assessmentbackend.domain.entities;

/**
 * Big Five personality traits for trait-level reliability tracking.
 * Used in psychometric analysis to assess reliability across personality dimensions.
 */
public enum BigFiveTrait {
    OPENNESS("Открытость опыту", "Openness to Experience - creativity, curiosity, intellectual interests"),
    CONSCIENTIOUSNESS("Добросовестность", "Conscientiousness - organization, dependability, self-discipline"),
    EXTRAVERSION("Экстраверсия", "Extraversion - energy, sociability, assertiveness"),
    AGREEABLENESS("Доброжелательность", "Agreeableness - cooperation, trust, empathy"),
    EMOTIONAL_STABILITY("Эмоциональная стабильность", "Emotional Stability - calmness, resilience (inverse of Neuroticism)");

    private final String displayName;
    private final String description;

    BigFiveTrait(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
