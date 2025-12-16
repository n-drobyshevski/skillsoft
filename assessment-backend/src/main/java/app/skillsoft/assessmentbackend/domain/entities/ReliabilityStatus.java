package app.skillsoft.assessmentbackend.domain.entities;

/**
 * Reliability status for competencies and traits based on Cronbach's Alpha.
 * Indicates internal consistency of assessment items.
 */
public enum ReliabilityStatus {
    RELIABLE("Надежный", "Cronbach's Alpha >= 0.7"),
    ACCEPTABLE("Приемлемый", "Cronbach's Alpha 0.6 - 0.7"),
    UNRELIABLE("Ненадежный", "Cronbach's Alpha < 0.6"),
    INSUFFICIENT_DATA("Недостаточно данных", "Not enough responses for reliable calculation");

    private final String displayName;
    private final String description;

    ReliabilityStatus(String displayName, String description) {
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
