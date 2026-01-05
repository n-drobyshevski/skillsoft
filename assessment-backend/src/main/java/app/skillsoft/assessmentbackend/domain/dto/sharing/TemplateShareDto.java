package app.skillsoft.assessmentbackend.domain.dto.sharing;

import app.skillsoft.assessmentbackend.domain.entities.GranteeType;
import app.skillsoft.assessmentbackend.domain.entities.SharePermission;
import app.skillsoft.assessmentbackend.domain.entities.TemplateShare;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO representing a template share (user or team grant).
 * Used for API responses when listing or retrieving shares.
 *
 * @param id Unique identifier of the share
 * @param templateId ID of the shared template
 * @param templateName Name of the template (for display)
 * @param granteeType Type of grantee (USER or TEAM)
 * @param granteeId ID of the user or team
 * @param granteeName Name of the user or team (for display)
 * @param granteeEmail Email of the user (null for team shares)
 * @param permission Permission level granted
 * @param grantedById ID of the user who granted the share
 * @param grantedByName Name of the user who granted the share
 * @param grantedAt When the share was granted
 * @param expiresAt When the share expires (null for no expiration)
 * @param isActive Whether the share is currently active
 * @param isExpired Whether the share has expired
 * @param isValid Whether the share is currently valid (active and not expired)
 */
public record TemplateShareDto(
        UUID id,
        UUID templateId,
        String templateName,
        GranteeType granteeType,
        UUID granteeId,
        String granteeName,
        String granteeEmail,
        SharePermission permission,
        UUID grantedById,
        String grantedByName,
        LocalDateTime grantedAt,
        LocalDateTime expiresAt,
        boolean isActive,
        boolean isExpired,
        boolean isValid
) {
    /**
     * Create a DTO from a TemplateShare entity.
     *
     * @param share The share entity
     * @return DTO with all fields populated
     */
    public static TemplateShareDto fromEntity(TemplateShare share) {
        // Extract template info
        UUID templateId = null;
        String templateName = null;
        if (share.getTemplate() != null) {
            templateId = share.getTemplate().getId();
            templateName = share.getTemplate().getName();
        }

        // Extract grantee info based on type
        UUID granteeId = share.getGranteeId();
        String granteeName = share.getGranteeName();
        String granteeEmail = null;

        if (share.getGranteeType() == GranteeType.USER && share.getUser() != null) {
            granteeEmail = share.getUser().getEmail();
        }

        // Extract grantor info
        UUID grantedById = null;
        String grantedByName = null;
        if (share.getGrantedBy() != null) {
            grantedById = share.getGrantedBy().getId();
            grantedByName = share.getGrantedBy().getFullName();
            if (grantedByName == null) {
                grantedByName = share.getGrantedBy().getEmail();
            }
        }

        return new TemplateShareDto(
                share.getId(),
                templateId,
                templateName,
                share.getGranteeType(),
                granteeId,
                granteeName,
                granteeEmail,
                share.getPermission(),
                grantedById,
                grantedByName,
                share.getGrantedAt(),
                share.getExpiresAt(),
                share.isActive(),
                share.isExpired(),
                share.isValid()
        );
    }

    /**
     * Check if this is a user share.
     *
     * @return true if grantee type is USER
     */
    public boolean isUserShare() {
        return granteeType == GranteeType.USER;
    }

    /**
     * Check if this is a team share.
     *
     * @return true if grantee type is TEAM
     */
    public boolean isTeamShare() {
        return granteeType == GranteeType.TEAM;
    }
}
