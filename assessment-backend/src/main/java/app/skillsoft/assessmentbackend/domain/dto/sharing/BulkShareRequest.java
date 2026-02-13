package app.skillsoft.assessmentbackend.domain.dto.sharing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

/**
 * Request DTO for bulk sharing a template with multiple users and/or teams.
 * Supports partial success pattern - will share with as many as possible.
 *
 * @param userShares List of user share requests
 * @param teamShares List of team share requests
 */
public record BulkShareRequest(
        @Valid
        @Size(max = 100, message = "Cannot share with more than 100 users at once")
        List<ShareUserRequest> userShares,

        @Valid
        @Size(max = 50, message = "Cannot share with more than 50 teams at once")
        List<ShareTeamRequest> teamShares
) {
    /**
     * Canonical constructor with null safety.
     */
    public BulkShareRequest {
        if (userShares == null) {
            userShares = new ArrayList<>();
        }
        if (teamShares == null) {
            teamShares = new ArrayList<>();
        }
    }

    /**
     * Check if this request has any shares to process.
     *
     * @return true if there is at least one user or team share
     */
    public boolean hasShares() {
        return !userShares.isEmpty() || !teamShares.isEmpty();
    }

    /**
     * Get total number of shares requested.
     *
     * @return Total count of user and team shares
     */
    public int getTotalShareCount() {
        return userShares.size() + teamShares.size();
    }

    /**
     * Create a request with only user shares.
     *
     * @param userShares List of user share requests
     * @return Bulk share request with users only
     */
    public static BulkShareRequest usersOnly(List<ShareUserRequest> userShares) {
        return new BulkShareRequest(userShares, null);
    }

    /**
     * Create a request with only team shares.
     *
     * @param teamShares List of team share requests
     * @return Bulk share request with teams only
     */
    public static BulkShareRequest teamsOnly(List<ShareTeamRequest> teamShares) {
        return new BulkShareRequest(null, teamShares);
    }

    /**
     * Create an empty request.
     *
     * @return Empty bulk share request
     */
    public static BulkShareRequest empty() {
        return new BulkShareRequest(null, null);
    }
}
