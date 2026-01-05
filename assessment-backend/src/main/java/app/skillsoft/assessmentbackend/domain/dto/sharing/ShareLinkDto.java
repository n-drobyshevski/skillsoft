package app.skillsoft.assessmentbackend.domain.dto.sharing;

import app.skillsoft.assessmentbackend.domain.entities.SharePermission;
import app.skillsoft.assessmentbackend.domain.entities.TemplateShareLink;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO representing a template share link.
 *
 * Token is either fully visible (to creator) or masked (to others with MANAGE permission).
 *
 * @param id Unique identifier of the link
 * @param templateId ID of the template this link provides access to
 * @param templateName Name of the template (for display purposes)
 * @param token The share token (full or masked based on viewer)
 * @param tokenMasked Whether the token is masked
 * @param permission Permission level granted by this link
 * @param label Optional human-readable label
 * @param createdByClerkId Clerk ID of the user who created this link
 * @param createdByName Name of the creator (for display)
 * @param createdAt When the link was created
 * @param expiresAt When the link expires
 * @param maxUses Maximum number of uses (null for unlimited)
 * @param currentUses Current number of times the link has been used
 * @param remainingUses Remaining uses (null for unlimited)
 * @param lastUsedAt When the link was last used
 * @param isActive Whether the link is currently active
 * @param isExpired Whether the link has expired
 * @param isUsedUp Whether the link has reached max uses
 * @param isValid Whether the link is currently valid (active, not expired, not used up)
 * @param invalidReason If not valid, the reason why
 * @param shareUrl The complete URL for sharing (if available)
 */
public record ShareLinkDto(
        UUID id,
        UUID templateId,
        String templateName,
        String token,
        boolean tokenMasked,
        SharePermission permission,
        String label,
        String createdByClerkId,
        String createdByName,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        Integer maxUses,
        Integer currentUses,
        Integer remainingUses,
        LocalDateTime lastUsedAt,
        boolean isActive,
        boolean isExpired,
        boolean isUsedUp,
        boolean isValid,
        String invalidReason,
        String shareUrl
) {
    /**
     * Create a DTO from an entity with full token visibility (for creator).
     *
     * @param link The share link entity
     * @param baseUrl Base URL for generating share URL
     * @return DTO with full token visible
     */
    public static ShareLinkDto fromEntity(TemplateShareLink link, String baseUrl) {
        return fromEntity(link, baseUrl, false);
    }

    /**
     * Create a DTO from an entity with optional token masking.
     *
     * @param link The share link entity
     * @param baseUrl Base URL for generating share URL (may be null)
     * @param maskToken Whether to mask the token
     * @return DTO with token masked or visible
     */
    public static ShareLinkDto fromEntity(TemplateShareLink link, String baseUrl, boolean maskToken) {
        String token = link.getToken();
        String displayToken = maskToken ? maskToken(token) : token;
        String shareUrl = baseUrl != null ? link.getShareUrl(baseUrl) : null;

        String creatorName = null;
        String creatorClerkId = null;
        if (link.getCreatedBy() != null) {
            creatorClerkId = link.getCreatedBy().getClerkId();
            creatorName = link.getCreatedBy().getFullName();
        }

        String templateName = link.getTemplate() != null ? link.getTemplate().getName() : null;

        return new ShareLinkDto(
                link.getId(),
                link.getTemplate() != null ? link.getTemplate().getId() : null,
                templateName,
                displayToken,
                maskToken,
                link.getPermission(),
                link.getLabel(),
                creatorClerkId,
                creatorName,
                link.getCreatedAt(),
                link.getExpiresAt(),
                link.getMaxUses(),
                link.getCurrentUses(),
                link.getRemainingUses(),
                link.getLastUsedAt(),
                link.isActive(),
                link.isExpired(),
                link.hasReachedMaxUses(),
                link.isValid(),
                link.getInvalidReason(),
                shareUrl
        );
    }

    /**
     * Mask a token showing only the first and last 4 characters.
     * Example: "abc123xyz789" -> "abc1...789"
     *
     * @param token The full token
     * @return Masked token string
     */
    private static String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }
}
