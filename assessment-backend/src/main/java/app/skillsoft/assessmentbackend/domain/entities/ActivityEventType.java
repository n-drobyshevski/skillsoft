package app.skillsoft.assessmentbackend.domain.entities;

/**
 * Enum representing the type of activity event in a test session lifecycle.
 */
public enum ActivityEventType {
    /**
     * User started a test session.
     */
    SESSION_STARTED("Started"),

    /**
     * User completed a test session successfully.
     */
    SESSION_COMPLETED("Completed"),

    /**
     * User abandoned a test session before completion.
     */
    SESSION_ABANDONED("Abandoned"),

    /**
     * Test session timed out before completion.
     */
    SESSION_TIMED_OUT("Timed Out");

    private final String displayName;

    ActivityEventType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
