package app.skillsoft.assessmentbackend.domain.entities;

/**
 * Lifecycle status for TestTemplate versioning.
 * Part of the Immutable Versioning pattern for template management.
 * 
 * State transitions:
 * - DRAFT -> PUBLISHED (via publish())
 * - PUBLISHED -> ARCHIVED (via archive())
 * - DRAFT can be modified, others are immutable
 */
public enum TemplateStatus {
    /**
     * Initial state for new templates and new versions.
     * Templates in DRAFT status can be modified.
     */
    DRAFT("Draft", "Template is being configured and can be modified"),
    
    /**
     * Template is active and available for test sessions.
     * Published templates are immutable - create a new version to make changes.
     */
    PUBLISHED("Published", "Template is active and immutable"),
    
    /**
     * Template is no longer active but preserved for historical records.
     * Archived templates cannot be used for new test sessions.
     */
    ARCHIVED("Archived", "Template is inactive and preserved for history");

    private final String displayName;
    private final String description;

    TemplateStatus(String displayName, String description) {
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
     * Check if the template can be modified in this status.
     * @return true if modifications are allowed
     */
    public boolean isEditable() {
        return this == DRAFT;
    }

    /**
     * Check if the template can be used for new test sessions.
     * @return true if the template is available for testing
     */
    public boolean isActive() {
        return this == PUBLISHED;
    }
}
