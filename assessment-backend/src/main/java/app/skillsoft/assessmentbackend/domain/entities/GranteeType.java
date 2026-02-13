package app.skillsoft.assessmentbackend.domain.entities;

/**
 * Type of grantee for template sharing.
 *
 * Determines whether a TemplateShare is granted to an individual user
 * or to an entire team.
 */
public enum GranteeType {
    /**
     * Share is granted to an individual user.
     * The user_id field should be set.
     */
    USER("User", "Individual user share"),

    /**
     * Share is granted to a team.
     * All active members of the team inherit access.
     * The team_id field should be set.
     */
    TEAM("Team", "Team-based share");

    private final String displayName;
    private final String description;

    GranteeType(String displayName, String description) {
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
