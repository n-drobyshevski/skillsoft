package app.skillsoft.assessmentbackend.services.sharing;

import app.skillsoft.assessmentbackend.domain.dto.sharing.BulkShareRequest;
import app.skillsoft.assessmentbackend.domain.dto.sharing.BulkShareResponse;
import app.skillsoft.assessmentbackend.domain.dto.sharing.SharedTemplatesResponseDto;
import app.skillsoft.assessmentbackend.domain.dto.sharing.TemplateShareDto;
import app.skillsoft.assessmentbackend.domain.entities.SharePermission;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing template shares (user and team grants).
 *
 * Provides operations for:
 * - Sharing templates with individual users
 * - Sharing templates with teams
 * - Updating and revoking shares
 * - Listing shares for a template
 * - Checking user access
 *
 * Validation rules:
 * - Cannot share with yourself (owner)
 * - Cannot grant permission higher than your own
 * - Cannot share DRAFT templates (only owner can access)
 * - Prevents duplicate shares (updates existing instead)
 * - Validates user/team existence before sharing
 */
public interface TemplateShareService {

    /**
     * Share a template with a specific user.
     *
     * @param templateId The template to share
     * @param userId The user to share with
     * @param permission The permission level to grant
     * @param expiresAt Optional expiration date (null for no expiration)
     * @param grantedByClerkId Clerk ID of the user granting the share
     * @return The created or updated share
     * @throws IllegalArgumentException if template or user not found
     * @throws IllegalStateException if validation fails (self-share, permission escalation, etc.)
     */
    TemplateShareDto shareWithUser(UUID templateId, UUID userId, SharePermission permission,
                                   LocalDateTime expiresAt, String grantedByClerkId);

    /**
     * Share a template with a team.
     * All active members of the team will inherit access.
     *
     * @param templateId The template to share
     * @param teamId The team to share with
     * @param permission The permission level to grant
     * @param expiresAt Optional expiration date (null for no expiration)
     * @param grantedByClerkId Clerk ID of the user granting the share
     * @return The created or updated share
     * @throws IllegalArgumentException if template or team not found
     * @throws IllegalStateException if validation fails
     */
    TemplateShareDto shareWithTeam(UUID templateId, UUID teamId, SharePermission permission,
                                   LocalDateTime expiresAt, String grantedByClerkId);

    /**
     * Update an existing share's permission level or expiration.
     *
     * @param shareId The share to update
     * @param permission New permission level
     * @param expiresAt New expiration date (null to remove expiration)
     * @return The updated share
     * @throws IllegalArgumentException if share not found
     * @throws IllegalStateException if share is revoked
     */
    TemplateShareDto updateShare(UUID shareId, SharePermission permission, LocalDateTime expiresAt);

    /**
     * Revoke (soft delete) a share.
     * The share record is preserved but marked as inactive.
     *
     * @param shareId The share to revoke
     * @throws IllegalArgumentException if share not found
     */
    void revokeShare(UUID shareId);

    /**
     * List all active shares for a template.
     * Includes both user and team shares.
     *
     * @param templateId The template to list shares for
     * @return List of active shares
     * @throws IllegalArgumentException if template not found
     */
    List<TemplateShareDto> listShares(UUID templateId);

    /**
     * List only user shares for a template.
     *
     * @param templateId The template to list shares for
     * @return List of active user shares
     * @throws IllegalArgumentException if template not found
     */
    List<TemplateShareDto> listUserShares(UUID templateId);

    /**
     * List only team shares for a template.
     *
     * @param templateId The template to list shares for
     * @return List of active team shares
     * @throws IllegalArgumentException if template not found
     */
    List<TemplateShareDto> listTeamShares(UUID templateId);

    /**
     * Bulk share a template with multiple users and/or teams.
     * Uses partial success pattern - shares with as many as possible.
     *
     * @param templateId The template to share
     * @param request Bulk share request with user and team lists
     * @param grantedByClerkId Clerk ID of the user granting the shares
     * @return Result with counts and details of created, updated, skipped, and failed shares
     * @throws IllegalArgumentException if template not found
     */
    BulkShareResponse bulkShare(UUID templateId, BulkShareRequest request, String grantedByClerkId);

    /**
     * Get a single share by ID.
     *
     * @param shareId The share ID
     * @return The share details
     * @throws IllegalArgumentException if share not found
     */
    TemplateShareDto getShareById(UUID shareId);

    /**
     * Check if a user has any access to a template.
     * Checks both direct user shares and team membership shares.
     *
     * @param templateId The template to check
     * @param userId The user to check access for
     * @return true if the user has any level of access
     */
    boolean hasAccess(UUID templateId, UUID userId);

    /**
     * Get the highest permission level a user has for a template.
     * Considers both direct user shares and team membership shares.
     *
     * @param templateId The template to check
     * @param userId The user to check
     * @return The highest permission level, or null if no access
     */
    SharePermission getHighestPermission(UUID templateId, UUID userId);

    /**
     * Check if a user can grant a specific permission level.
     * Users can only grant permissions up to their own level.
     *
     * @param templateId The template to check
     * @param grantorClerkId Clerk ID of the user trying to grant
     * @param requestedPermission The permission they want to grant
     * @return true if the grantor can grant this permission
     */
    boolean canGrantPermission(UUID templateId, String grantorClerkId, SharePermission requestedPermission);

    /**
     * Count active shares for a template.
     *
     * @param templateId The template to count shares for
     * @return Total count of active shares
     */
    long countShares(UUID templateId);

    /**
     * Revoke all shares for a template.
     * Used when archiving or deleting a template.
     *
     * @param templateId The template to revoke all shares for
     * @return Number of shares revoked
     */
    int revokeAllShares(UUID templateId);

    // ============================================
    // SHARED WITH ME OPERATIONS
    // ============================================

    /**
     * Get all templates shared with the current user.
     * Includes both direct user shares and team membership shares.
     * Excludes templates owned by the user.
     *
     * For templates with multiple shares (e.g., direct + team),
     * returns the share with the highest permission.
     *
     * @param clerkId Clerk ID of the user
     * @return Response with list of shared templates and total count
     */
    SharedTemplatesResponseDto getTemplatesSharedWithMe(String clerkId);

    /**
     * Count unique templates shared with the current user.
     * For badge/counter display in navigation.
     *
     * @param clerkId Clerk ID of the user
     * @return Count of shared templates
     */
    long countTemplatesSharedWithMe(String clerkId);
}
