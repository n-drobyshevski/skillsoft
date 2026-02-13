package app.skillsoft.assessmentbackend.domain.dto.sharing;

import app.skillsoft.assessmentbackend.domain.entities.SharePermission;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Request DTO for sharing a template with a team.
 * All active members of the team will inherit access.
 *
 * @param teamId The UUID of the team to share with
 * @param permission The permission level to grant (VIEW, EDIT, or MANAGE)
 * @param expiresAt Optional expiration date for the share (null for no expiration)
 */
public record ShareTeamRequest(
        @NotNull(message = "Team ID is required")
        UUID teamId,

        @NotNull(message = "Permission is required")
        SharePermission permission,

        LocalDateTime expiresAt
) {
    /**
     * Create a VIEW share request with no expiration.
     *
     * @param teamId The team to share with
     * @return A share request with VIEW permission
     */
    public static ShareTeamRequest viewOnly(UUID teamId) {
        return new ShareTeamRequest(teamId, SharePermission.VIEW, null);
    }

    /**
     * Create an EDIT share request with no expiration.
     *
     * @param teamId The team to share with
     * @return A share request with EDIT permission
     */
    public static ShareTeamRequest withEdit(UUID teamId) {
        return new ShareTeamRequest(teamId, SharePermission.EDIT, null);
    }

    /**
     * Create a MANAGE share request with no expiration.
     *
     * @param teamId The team to share with
     * @return A share request with MANAGE permission
     */
    public static ShareTeamRequest withManage(UUID teamId) {
        return new ShareTeamRequest(teamId, SharePermission.MANAGE, null);
    }

    /**
     * Create a time-limited share request.
     *
     * @param teamId The team to share with
     * @param permission The permission level
     * @param daysValid Number of days the share is valid
     * @return A share request with expiration
     */
    public static ShareTeamRequest withExpiry(UUID teamId, SharePermission permission, int daysValid) {
        return new ShareTeamRequest(teamId, permission, LocalDateTime.now().plusDays(daysValid));
    }
}
