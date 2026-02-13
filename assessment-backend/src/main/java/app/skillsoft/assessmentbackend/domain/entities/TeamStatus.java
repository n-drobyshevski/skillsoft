package app.skillsoft.assessmentbackend.domain.entities;

/**
 * Enum representing the lifecycle status of a Team.
 *
 * State Machine:
 * - DRAFT: Team is being configured, can add/remove members
 * - ACTIVE: Team is operational and can be used for TEAM_FIT assessments
 * - ARCHIVED: Team is no longer active, preserved for historical reference
 *
 * Valid Transitions:
 * - DRAFT -> ACTIVE (requires at least 1 active member)
 * - DRAFT -> ARCHIVED (cancel draft)
 * - ACTIVE -> ARCHIVED (decommission team)
 *
 * Invalid Transitions:
 * - ARCHIVED -> any (archived teams cannot be reactivated)
 * - ACTIVE -> DRAFT (cannot demote active team)
 */
public enum TeamStatus {
    DRAFT("Draft", "Team is being configured"),
    ACTIVE("Active", "Team is operational"),
    ARCHIVED("Archived", "Team is no longer active");

    private final String displayName;
    private final String description;

    TeamStatus(String displayName, String description) {
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
     * Checks if transition to target status is valid.
     * @param target The target status to transition to
     * @return true if the transition is allowed
     */
    public boolean canTransitionTo(TeamStatus target) {
        return switch (this) {
            case DRAFT -> target == ACTIVE || target == ARCHIVED;
            case ACTIVE -> target == ARCHIVED;
            case ARCHIVED -> false;
        };
    }
}
