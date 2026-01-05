package app.skillsoft.assessmentbackend.domain.entities;

/**
 * Visibility modes for TestTemplate access control.
 *
 * Determines who can view and use a test template:
 * - PUBLIC: Any authenticated user can access
 * - PRIVATE: Only owner and explicitly shared users/teams
 * - LINK: Anyone with a valid share link (including anonymous users)
 */
public enum TemplateVisibility {
    /**
     * Any authenticated user can view and use the template.
     * Results are private to the test-taker.
     */
    PUBLIC("Public", "All authenticated users can access"),

    /**
     * Only the owner and explicitly shared users/teams can access.
     * Default visibility for new templates.
     */
    PRIVATE("Private", "Only shared users can access"),

    /**
     * Anyone with a valid share link can access.
     * Supports anonymous access (no authentication required).
     * Links can be time-limited and usage-tracked.
     */
    LINK("Link", "Accessible via share link");

    private final String displayName;
    private final String description;

    TemplateVisibility(String displayName, String description) {
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
     * Check if this visibility allows open access without explicit sharing.
     * @return true if PUBLIC (authenticated) or LINK (with valid token)
     */
    public boolean isOpenAccess() {
        return this == PUBLIC;
    }

    /**
     * Check if this visibility requires authentication.
     * @return true if PUBLIC or PRIVATE, false for LINK (allows anonymous)
     */
    public boolean requiresAuthentication() {
        return this != LINK;
    }

    /**
     * Check if this visibility allows anonymous access.
     * @return true only for LINK visibility
     */
    public boolean allowsAnonymousAccess() {
        return this == LINK;
    }
}
