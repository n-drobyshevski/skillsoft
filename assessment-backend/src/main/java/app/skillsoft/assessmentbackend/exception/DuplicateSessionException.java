package app.skillsoft.assessmentbackend.exception;

import java.util.UUID;

/**
 * Exception thrown when a user attempts to start a new test session
 * while already having an in-progress session for the same template.
 *
 * This exception carries the existing session ID to allow the frontend
 * to offer the user options to either resume or abandon the existing session.
 */
public class DuplicateSessionException extends RuntimeException {

    private final UUID existingSessionId;
    private final UUID templateId;
    private final String clerkUserId;

    /**
     * Constructs a new duplicate session exception.
     *
     * @param existingSessionId The ID of the existing in-progress session
     * @param templateId The template ID for which the duplicate was attempted
     * @param clerkUserId The user who attempted to start the duplicate session
     */
    public DuplicateSessionException(UUID existingSessionId, UUID templateId, String clerkUserId) {
        super(String.format("User %s already has an in-progress session for template %s", clerkUserId, templateId));
        this.existingSessionId = existingSessionId;
        this.templateId = templateId;
        this.clerkUserId = clerkUserId;
    }

    /**
     * Gets the existing session ID.
     *
     * @return UUID of the existing in-progress session
     */
    public UUID getExistingSessionId() {
        return existingSessionId;
    }

    /**
     * Gets the template ID.
     *
     * @return UUID of the template
     */
    public UUID getTemplateId() {
        return templateId;
    }

    /**
     * Gets the user ID.
     *
     * @return Clerk user ID
     */
    public String getClerkUserId() {
        return clerkUserId;
    }
}
