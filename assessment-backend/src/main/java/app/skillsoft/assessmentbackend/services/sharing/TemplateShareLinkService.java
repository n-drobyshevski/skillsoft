package app.skillsoft.assessmentbackend.services.sharing;

import app.skillsoft.assessmentbackend.domain.dto.sharing.CreateShareLinkRequest;
import app.skillsoft.assessmentbackend.domain.dto.sharing.LinkValidationResult;
import app.skillsoft.assessmentbackend.domain.dto.sharing.ShareLinkDto;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing template share links.
 *
 * Share links provide token-based access to templates, allowing anonymous access
 * when template visibility is set to LINK. Links can be time-limited, usage-limited,
 * and labeled for organization.
 *
 * Business Rules:
 * - Maximum 10 active links per template
 * - Maximum 365 days expiry
 * - Links only work when template visibility is LINK
 * - Links can only grant VIEW or EDIT permission (not MANAGE)
 */
public interface TemplateShareLinkService {

    /**
     * Create a new share link for a template.
     *
     * @param templateId The template to create a link for
     * @param request Link configuration (expiry, maxUses, permission, label)
     * @param createdByClerkId Clerk ID of the user creating the link
     * @return The created link DTO with full token visible
     * @throws IllegalArgumentException if template not found or validation fails
     * @throws IllegalStateException if max links limit reached
     */
    ShareLinkDto createLink(UUID templateId, CreateShareLinkRequest request, String createdByClerkId);

    /**
     * Validate a share link token and return template access information.
     *
     * Does NOT record usage - use recordUsage() after granting access.
     * Does NOT require authentication.
     *
     * @param token The share link token
     * @return Validation result with template info if valid, or reason if invalid
     */
    LinkValidationResult validateLink(String token);

    /**
     * Revoke (soft delete) a single share link.
     *
     * @param linkId The link ID to revoke
     * @throws IllegalArgumentException if link not found
     */
    void revokeLink(UUID linkId);

    /**
     * Get all links for a template (including expired and revoked).
     *
     * Token visibility depends on the requesting user:
     * - Full token visible to the link creator
     * - Masked token for others with MANAGE permission
     *
     * @param templateId The template ID
     * @param requestingClerkId Clerk ID of the user requesting the list (for token masking)
     * @return List of all links ordered by creation date (newest first)
     */
    List<ShareLinkDto> listLinks(UUID templateId, String requestingClerkId);

    /**
     * Get only active (valid) links for a template.
     *
     * Active means: not revoked, not expired, not used up.
     *
     * @param templateId The template ID
     * @param requestingClerkId Clerk ID of the user requesting the list (for token masking)
     * @return List of active links ordered by creation date (newest first)
     */
    List<ShareLinkDto> listActiveLinks(UUID templateId, String requestingClerkId);

    /**
     * Get a single link by ID.
     *
     * @param linkId The link ID
     * @param requestingClerkId Clerk ID of the user requesting (for token masking)
     * @return The link DTO, or null if not found
     */
    ShareLinkDto getLinkById(UUID linkId, String requestingClerkId);

    /**
     * Revoke all active links for a template.
     *
     * Used when archiving a template or changing visibility away from LINK.
     *
     * @param templateId The template ID
     * @return Number of links revoked
     */
    int revokeAllLinks(UUID templateId);

    /**
     * Record a usage of a share link.
     *
     * Should be called after successfully granting access via the link.
     * Increments the usage counter and updates last used timestamp.
     *
     * @param token The share link token
     * @return true if usage was recorded, false if link not found or invalid
     */
    boolean recordUsage(String token);

    /**
     * Check if a new link can be created for a template.
     *
     * Enforces the maximum 10 active links per template limit.
     *
     * @param templateId The template ID
     * @return true if under the limit, false if at or over
     */
    boolean canCreateLink(UUID templateId);

    /**
     * Get the count of active links for a template.
     *
     * @param templateId The template ID
     * @return Number of active links
     */
    long countActiveLinks(UUID templateId);

    /**
     * Get the maximum number of active links allowed per template.
     *
     * @return The limit (10)
     */
    int getMaxLinksPerTemplate();

    /**
     * Get the maximum expiry days allowed for a link.
     *
     * @return The limit (365)
     */
    int getMaxExpiryDays();
}
