package app.skillsoft.assessmentbackend.security;

import java.util.UUID;

/**
 * Service for verifying session and result ownership.
 * Used by @PreAuthorize SpEL expressions for resource-level authorization.
 *
 * Bean name: "sessionSecurity" for SpEL: @sessionSecurity.isSessionOwner(#sessionId)
 */
public interface SessionSecurityService {

    /**
     * Check if the authenticated user owns the specified session.
     * Admins and Editors can access any session.
     *
     * @param sessionId The session UUID to check
     * @return true if the authenticated user is the owner or has privileged role
     */
    boolean isSessionOwner(UUID sessionId);

    /**
     * Check if the authenticated user owns the result (via session).
     * Admins and Editors can access any result.
     *
     * @param resultId The result UUID to check
     * @return true if the authenticated user is the owner or has privileged role
     */
    boolean isResultOwner(UUID resultId);

    /**
     * Check if the authenticated user can access another user's data.
     * Users can access their own data; Admins/Editors can access anyone's data.
     *
     * @param targetClerkUserId The target user's Clerk ID
     * @return true if caller is the target user OR has ADMIN/EDITOR role
     */
    boolean canAccessUserData(String targetClerkUserId);

    /**
     * Get the authenticated user's Clerk ID from SecurityContext.
     *
     * @return The Clerk ID of the currently authenticated user, or null if not authenticated
     */
    String getAuthenticatedUserId();
}
