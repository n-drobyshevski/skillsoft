package app.skillsoft.assessmentbackend.domain.entities;

/**
 * Validity status for assessment questions based on psychometric analysis.
 * Determines whether an item should be included in test assembly.
 */
public enum ItemValidityStatus {
    ACTIVE("Активный", "Validated item with good psychometric properties (rpb > 0.3)"),
    PROBATION("Пробационный", "New item with < 50 responses, gathering data"),
    FLAGGED_FOR_REVIEW("На проверке", "Requires manual review due to extreme metrics"),
    RETIRED("Отключен", "Removed from active use due to poor discrimination (rpb < 0)");

    private final String displayName;
    private final String description;

    ItemValidityStatus(String displayName, String description) {
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
