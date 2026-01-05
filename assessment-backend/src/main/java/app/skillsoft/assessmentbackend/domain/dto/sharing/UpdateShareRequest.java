package app.skillsoft.assessmentbackend.domain.dto.sharing;

import app.skillsoft.assessmentbackend.domain.entities.SharePermission;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Request DTO for updating an existing template share.
 *
 * @param permission The new permission level
 * @param expiresAt Optional new expiration date (null to remove expiration)
 */
public record UpdateShareRequest(
        @NotNull(message = "Permission is required")
        SharePermission permission,

        LocalDateTime expiresAt
) {
    /**
     * Create an update request to set VIEW permission with no expiration.
     *
     * @return An update request with VIEW permission
     */
    public static UpdateShareRequest toViewOnly() {
        return new UpdateShareRequest(SharePermission.VIEW, null);
    }

    /**
     * Create an update request to set EDIT permission with no expiration.
     *
     * @return An update request with EDIT permission
     */
    public static UpdateShareRequest toEdit() {
        return new UpdateShareRequest(SharePermission.EDIT, null);
    }

    /**
     * Create an update request to set MANAGE permission with no expiration.
     *
     * @return An update request with MANAGE permission
     */
    public static UpdateShareRequest toManage() {
        return new UpdateShareRequest(SharePermission.MANAGE, null);
    }
}
