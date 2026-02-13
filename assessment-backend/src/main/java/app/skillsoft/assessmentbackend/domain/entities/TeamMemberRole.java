package app.skillsoft.assessmentbackend.domain.entities;

/**
 * Enum representing the role of a user within a Team.
 *
 * Roles:
 * - LEADER: Team lead with management capabilities (only one per team)
 * - MEMBER: Regular team member
 */
public enum TeamMemberRole {
    LEADER("Leader", "Team lead with management capabilities"),
    MEMBER("Member", "Regular team member");

    private final String displayName;
    private final String description;

    TeamMemberRole(String displayName, String description) {
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
