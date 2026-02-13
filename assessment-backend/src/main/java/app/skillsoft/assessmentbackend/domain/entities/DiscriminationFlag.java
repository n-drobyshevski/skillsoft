package app.skillsoft.assessmentbackend.domain.entities;

/**
 * Flag indicating discrimination index issues for assessment questions.
 * Based on the point-biserial correlation (rpb) between item score and total test score.
 */
public enum DiscriminationFlag {
    NONE("Хорошо", "Good discrimination (rpb >= 0.25)"),
    WARNING("Предупреждение", "Marginal discrimination (0.1 <= rpb < 0.25)"),
    CRITICAL("Критично", "Poor discrimination (0 < rpb < 0.1)"),
    NEGATIVE("Токсичный", "Negative discrimination (rpb < 0) - Actively harms test validity");

    private final String displayName;
    private final String description;

    DiscriminationFlag(String displayName, String description) {
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
