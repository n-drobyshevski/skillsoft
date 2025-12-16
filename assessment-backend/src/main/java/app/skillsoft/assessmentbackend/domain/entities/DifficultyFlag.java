package app.skillsoft.assessmentbackend.domain.entities;

/**
 * Flag indicating difficulty index issues for assessment questions.
 * Based on the p-value (proportion of correct/positive responses).
 */
public enum DifficultyFlag {
    NONE("Нормально", "Difficulty index in acceptable range (0.2 - 0.9)"),
    TOO_HARD("Слишком сложный", "Difficulty index < 0.2 - Most respondents fail"),
    TOO_EASY("Слишком легкий", "Difficulty index > 0.9 - Most respondents succeed");

    private final String displayName;
    private final String description;

    DifficultyFlag(String displayName, String description) {
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
