package app.skillsoft.assessmentbackend.domain.entities;

/**
 * Enum representing the roles available in the assessment application.
 * 
 * These roles correspond to different permission levels:
 * - USER: Basic access to take assessments and view personal results
 * - EDITOR: Can create and modify assessments, competencies, and indicators  
 * - ADMIN: Full system access including user management and system configuration
 */
public enum UserRole {
    USER("User", "Basic user with assessment taking privileges"),
    EDITOR("Editor", "Can create and modify assessment content"),
    ADMIN("Admin", "Full administrative access to the system");

    private final String displayName;
    private final String description;

    UserRole(String displayName, String description) {
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