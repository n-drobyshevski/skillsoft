package app.skillsoft.assessmentbackend.services.security;

import app.skillsoft.assessmentbackend.domain.entities.SharePermission;

import java.util.UUID;

/**
 * Service interface for template access control.
 *
 * Provides methods for checking user permissions on templates.
 * Used in @PreAuthorize expressions for method-level security.
 *
 * Access priority (evaluated in order):
 * 1. Valid share link token (allows anonymous access for LINK visibility)
 * 2. Authentication required for remaining checks
 * 3. ADMIN role (bypass)
 * 4. Template owner
 * 5. Direct user share
 * 6. Team membership share
 * 7. PUBLIC visibility (VIEW only)
 */
public interface TemplateSecurityService {

    /**
     * Check if the current user can access a template with the required permission.
     *
     * @param templateId The template to check access for
     * @param required The minimum permission level required
     * @return true if access is granted
     */
    boolean canAccess(UUID templateId, SharePermission required);

    /**
     * Check if the current user can access a template with a share link token.
     *
     * @param templateId The template to check access for
     * @param required The minimum permission level required
     * @param shareToken The share link token (may be null)
     * @return true if access is granted
     */
    boolean canAccess(UUID templateId, SharePermission required, String shareToken);

    /**
     * Check if a share token is valid for accessing a template.
     * Does not require authentication.
     *
     * @param templateId The template to check
     * @param shareToken The share link token
     * @return true if the token is valid for this template
     */
    boolean isValidShareToken(UUID templateId, String shareToken);

    /**
     * Check if the current user is the owner of the template.
     *
     * @param templateId The template to check
     * @return true if the current user is the owner
     */
    boolean isOwner(UUID templateId);

    /**
     * Check if the current user can manage sharing settings for a template.
     * Requires MANAGE permission or ownership.
     *
     * @param templateId The template to check
     * @return true if the user can manage sharing
     */
    boolean canManageSharing(UUID templateId);

    /**
     * Check if the current user can edit a template.
     * Requires EDIT permission or higher, or ownership.
     *
     * @param templateId The template to check
     * @return true if the user can edit
     */
    boolean canEdit(UUID templateId);

    /**
     * Check if the current user can view a template.
     * Requires VIEW permission or higher, or public access, or valid link.
     *
     * @param templateId The template to check
     * @return true if the user can view
     */
    boolean canView(UUID templateId);

    /**
     * Check if the current user can delete a template.
     * Requires ownership or ADMIN role.
     *
     * @param templateId The template to check
     * @return true if the user can delete
     */
    boolean canDelete(UUID templateId);

    /**
     * Check if the current user can change visibility settings.
     * Requires ownership or MANAGE permission.
     *
     * @param templateId The template to check
     * @return true if the user can change visibility
     */
    boolean canChangeVisibility(UUID templateId);

    /**
     * Check if the current user can create share links.
     * Requires MANAGE permission or ownership.
     *
     * @param templateId The template to check
     * @return true if the user can create links
     */
    boolean canCreateLinks(UUID templateId);

    /**
     * Get the authenticated user's clerk ID, or null if anonymous.
     *
     * @return The clerk ID or null
     */
    String getAuthenticatedClerkId();

    /**
     * Check if the current user has the ADMIN role.
     *
     * @return true if the user is an admin
     */
    boolean isAdmin();

    /**
     * Check if the current user has the EDITOR role.
     *
     * @return true if the user is an editor
     */
    boolean isEditor();

    /**
     * Check if the current request is authenticated.
     *
     * @return true if authenticated
     */
    boolean isAuthenticated();

    /**
     * Check if the current user can view anonymous test results for a template.
     * Requires ownership, ADMIN role, or MANAGE permission.
     *
     * @param templateId The template to check
     * @return true if the user can view anonymous results
     */
    boolean canViewAnonymousResults(UUID templateId);
}
