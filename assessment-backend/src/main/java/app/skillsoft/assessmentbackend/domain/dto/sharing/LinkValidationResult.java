package app.skillsoft.assessmentbackend.domain.dto.sharing;

import app.skillsoft.assessmentbackend.domain.entities.SharePermission;
import app.skillsoft.assessmentbackend.domain.entities.TemplateShareLink;

import java.util.UUID;

/**
 * Result of validating a share link token.
 *
 * Used when accessing a template via share link to determine if access should be granted.
 *
 * @param valid Whether the link is valid for access
 * @param templateId ID of the template (if valid)
 * @param templateName Name of the template (if valid)
 * @param permission Permission level granted by the link (if valid)
 * @param reason If invalid, the reason why (REVOKED, EXPIRED, MAX_USES_REACHED, NOT_FOUND, VISIBILITY_MISMATCH)
 */
public record LinkValidationResult(
        boolean valid,
        UUID templateId,
        String templateName,
        SharePermission permission,
        String reason
) {
    /**
     * Invalid reason constants.
     */
    public static final String REASON_NOT_FOUND = "NOT_FOUND";
    public static final String REASON_REVOKED = "REVOKED";
    public static final String REASON_EXPIRED = "EXPIRED";
    public static final String REASON_MAX_USES_REACHED = "MAX_USES_REACHED";
    public static final String REASON_VISIBILITY_MISMATCH = "VISIBILITY_MISMATCH";
    public static final String REASON_TEMPLATE_NOT_FOUND = "TEMPLATE_NOT_FOUND";
    public static final String REASON_TEMPLATE_ARCHIVED = "TEMPLATE_ARCHIVED";

    /**
     * Create a successful validation result.
     *
     * @param templateId The template ID
     * @param templateName The template name
     * @param permission The granted permission
     * @return Valid result
     */
    public static LinkValidationResult valid(UUID templateId, String templateName, SharePermission permission) {
        return new LinkValidationResult(true, templateId, templateName, permission, null);
    }

    /**
     * Create from a valid share link entity.
     *
     * @param link The valid share link
     * @return Valid result
     */
    public static LinkValidationResult fromLink(TemplateShareLink link) {
        return new LinkValidationResult(
                true,
                link.getTemplate().getId(),
                link.getTemplate().getName(),
                link.getPermission(),
                null
        );
    }

    /**
     * Create an invalid result with reason.
     *
     * @param reason The reason for invalidity
     * @return Invalid result
     */
    public static LinkValidationResult invalid(String reason) {
        return new LinkValidationResult(false, null, null, null, reason);
    }

    /**
     * Create a not found result.
     *
     * @return Invalid result with NOT_FOUND reason
     */
    public static LinkValidationResult notFound() {
        return invalid(REASON_NOT_FOUND);
    }

    /**
     * Create a revoked result.
     *
     * @return Invalid result with REVOKED reason
     */
    public static LinkValidationResult revoked() {
        return invalid(REASON_REVOKED);
    }

    /**
     * Create an expired result.
     *
     * @return Invalid result with EXPIRED reason
     */
    public static LinkValidationResult expired() {
        return invalid(REASON_EXPIRED);
    }

    /**
     * Create a max uses reached result.
     *
     * @return Invalid result with MAX_USES_REACHED reason
     */
    public static LinkValidationResult maxUsesReached() {
        return invalid(REASON_MAX_USES_REACHED);
    }

    /**
     * Create a visibility mismatch result (template is not set to LINK visibility).
     *
     * @return Invalid result with VISIBILITY_MISMATCH reason
     */
    public static LinkValidationResult visibilityMismatch() {
        return invalid(REASON_VISIBILITY_MISMATCH);
    }

    /**
     * Create a template not found result.
     *
     * @return Invalid result with TEMPLATE_NOT_FOUND reason
     */
    public static LinkValidationResult templateNotFound() {
        return invalid(REASON_TEMPLATE_NOT_FOUND);
    }

    /**
     * Create a template archived result.
     *
     * @return Invalid result with TEMPLATE_ARCHIVED reason
     */
    public static LinkValidationResult templateArchived() {
        return invalid(REASON_TEMPLATE_ARCHIVED);
    }

    /**
     * Check if access should be granted.
     *
     * @return true if valid
     */
    public boolean canAccess() {
        return valid;
    }

    /**
     * Check if the link grants the required permission.
     *
     * @param required The required permission level
     * @return true if the link grants sufficient permission
     */
    public boolean hasPermission(SharePermission required) {
        return valid && permission != null && permission.includes(required);
    }
}
