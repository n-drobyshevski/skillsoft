package app.skillsoft.assessmentbackend.domain.dto.sharing;

import app.skillsoft.assessmentbackend.domain.entities.SharePermission;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Request DTO for sharing a template with a specific user.
 *
 * @param userId The UUID of the user to share with
 * @param permission The permission level to grant (VIEW, EDIT, or MANAGE)
 * @param expiresAt Optional expiration date for the share (null for no expiration)
 */
public record ShareUserRequest(
        @NotNull(message = "User ID is required")
        UUID userId,

        @NotNull(message = "Permission is required")
        SharePermission permission,

        LocalDateTime expiresAt
) {
    /**
     * Create a VIEW share request with no expiration.
     *
     * @param userId The user to share with
     * @return A share request with VIEW permission
     */
    public static ShareUserRequest viewOnly(UUID userId) {
        return new ShareUserRequest(userId, SharePermission.VIEW, null);
    }

    /**
     * Create an EDIT share request with no expiration.
     *
     * @param userId The user to share with
     * @return A share request with EDIT permission
     */
    public static ShareUserRequest withEdit(UUID userId) {
        return new ShareUserRequest(userId, SharePermission.EDIT, null);
    }

    /**
     * Create a MANAGE share request with no expiration.
     *
     * @param userId The user to share with
     * @return A share request with MANAGE permission
     */
    public static ShareUserRequest withManage(UUID userId) {
        return new ShareUserRequest(userId, SharePermission.MANAGE, null);
    }

    /**
     * Create a time-limited share request.
     *
     * @param userId The user to share with
     * @param permission The permission level
     * @param daysValid Number of days the share is valid
     * @return A share request with expiration
     */
    public static ShareUserRequest withExpiry(UUID userId, SharePermission permission, int daysValid) {
        return new ShareUserRequest(userId, permission, LocalDateTime.now().plusDays(daysValid));
    }
}
