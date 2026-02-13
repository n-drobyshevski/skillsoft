package app.skillsoft.assessmentbackend.domain.entities;

/**
 * Permission levels for template sharing.
 *
 * Follows a hierarchical model where higher permissions include all lower ones:
 * MANAGE > EDIT > VIEW
 */
public enum SharePermission {
    /**
     * Can view template details and take tests.
     * Lowest permission level.
     */
    VIEW("View", "Can view and take tests", 1),

    /**
     * Can modify template configuration.
     * Includes VIEW permission.
     */
    EDIT("Edit", "Can modify template configuration", 2),

    /**
     * Can share with others and manage sharing settings.
     * Includes VIEW and EDIT permissions.
     */
    MANAGE("Manage", "Can share with others and delete", 3);

    private final String displayName;
    private final String description;
    private final int level;

    SharePermission(String displayName, String description, int level) {
        this.displayName = displayName;
        this.description = description;
        this.level = level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getLevel() {
        return level;
    }

    /**
     * Check if this permission includes another permission.
     * Higher level permissions include all lower ones.
     *
     * @param other The permission to check
     * @return true if this permission is equal to or higher than the other
     */
    public boolean includes(SharePermission other) {
        if (other == null) {
            return false;
        }
        return this.level >= other.level;
    }

    /**
     * Check if this permission is sufficient for the required permission.
     * Alias for includes() for readability.
     *
     * @param required The required permission
     * @return true if this permission meets or exceeds the requirement
     */
    public boolean satisfies(SharePermission required) {
        return includes(required);
    }

    /**
     * Get the minimum permission level.
     * @return VIEW permission
     */
    public static SharePermission minimum() {
        return VIEW;
    }

    /**
     * Get the maximum permission level.
     * @return MANAGE permission
     */
    public static SharePermission maximum() {
        return MANAGE;
    }
}
