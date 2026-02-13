package app.skillsoft.assessmentbackend.exception;

/**
 * Exception thrown when a requested resource is not found in the system.
 *
 * This exception results in HTTP 404 NOT FOUND responses and should be used
 * instead of generic RuntimeException for missing entities.
 *
 * Examples:
 * - Session not found by ID
 * - Question not found by ID
 * - Template not found by ID
 * - User not found by Clerk ID
 *
 * @author SkillSoft Development Team
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceType;
    private final Object resourceId;

    /**
     * Constructs a new resource not found exception with resource type and ID.
     *
     * @param resourceType The type of resource (e.g., "Session", "Question", "Template")
     * @param resourceId The ID of the resource that was not found
     */
    public ResourceNotFoundException(String resourceType, Object resourceId) {
        super(String.format("%s not found with id: %s", resourceType, resourceId));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    /**
     * Constructs a new resource not found exception with a custom message.
     *
     * @param message Custom error message
     */
    public ResourceNotFoundException(String message) {
        super(message);
        this.resourceType = null;
        this.resourceId = null;
    }

    /**
     * Gets the resource type.
     *
     * @return The type of resource (may be null if custom message was used)
     */
    public String getResourceType() {
        return resourceType;
    }

    /**
     * Gets the resource ID.
     *
     * @return The ID of the resource (may be null if custom message was used)
     */
    public Object getResourceId() {
        return resourceId;
    }
}
