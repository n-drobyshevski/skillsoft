package app.skillsoft.assessmentbackend.domain.dto.sharing;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for bulk share operations.
 * Follows partial success pattern - reports created, updated, skipped, and failed shares.
 *
 * @param createdCount Number of new shares successfully created
 * @param updatedCount Number of existing shares successfully updated
 * @param skippedCount Number of shares skipped (e.g., duplicates, self-share)
 * @param failedCount Number of shares that failed
 * @param createdShares List of successfully created shares
 * @param updatedShares List of successfully updated shares
 * @param errors List of error messages for failed operations
 */
public record BulkShareResponse(
        int createdCount,
        int updatedCount,
        int skippedCount,
        int failedCount,
        List<TemplateShareDto> createdShares,
        List<TemplateShareDto> updatedShares,
        List<ShareError> errors
) {
    /**
     * Canonical constructor with null safety.
     */
    public BulkShareResponse {
        if (createdShares == null) {
            createdShares = new ArrayList<>();
        }
        if (updatedShares == null) {
            updatedShares = new ArrayList<>();
        }
        if (errors == null) {
            errors = new ArrayList<>();
        }
    }

    /**
     * Check if all shares were successful.
     *
     * @return true if no failures occurred
     */
    public boolean isFullySuccessful() {
        return failedCount == 0;
    }

    /**
     * Check if any shares were successful.
     *
     * @return true if at least one share was created or updated
     */
    public boolean isPartiallySuccessful() {
        return createdCount > 0 || updatedCount > 0;
    }

    /**
     * Check if all shares failed.
     *
     * @return true if no shares were successful
     */
    public boolean isCompleteFailure() {
        return createdCount == 0 && updatedCount == 0 && failedCount > 0;
    }

    /**
     * Get total number of successful operations.
     *
     * @return Sum of created and updated counts
     */
    public int getSuccessCount() {
        return createdCount + updatedCount;
    }

    /**
     * Get total number of processed items.
     *
     * @return Sum of all counts
     */
    public int getTotalProcessed() {
        return createdCount + updatedCount + skippedCount + failedCount;
    }

    /**
     * Error detail for a failed share operation.
     *
     * @param identifier The user ID or team ID that failed
     * @param type Whether it was a user or team share
     * @param message Error message describing the failure
     */
    public record ShareError(
            String identifier,
            String type,
            String message
    ) {
        /**
         * Create a user share error.
         *
         * @param userId The user ID
         * @param message Error message
         * @return ShareError for a user
         */
        public static ShareError forUser(String userId, String message) {
            return new ShareError(userId, "USER", message);
        }

        /**
         * Create a team share error.
         *
         * @param teamId The team ID
         * @param message Error message
         * @return ShareError for a team
         */
        public static ShareError forTeam(String teamId, String message) {
            return new ShareError(teamId, "TEAM", message);
        }
    }

    /**
     * Builder for constructing BulkShareResponse incrementally.
     */
    public static class Builder {
        private final List<TemplateShareDto> createdShares = new ArrayList<>();
        private final List<TemplateShareDto> updatedShares = new ArrayList<>();
        private final List<ShareError> errors = new ArrayList<>();
        private int skippedCount = 0;

        public Builder addCreated(TemplateShareDto share) {
            createdShares.add(share);
            return this;
        }

        public Builder addUpdated(TemplateShareDto share) {
            updatedShares.add(share);
            return this;
        }

        public Builder addSkipped() {
            skippedCount++;
            return this;
        }

        public Builder addError(ShareError error) {
            errors.add(error);
            return this;
        }

        public Builder addUserError(String userId, String message) {
            errors.add(ShareError.forUser(userId, message));
            return this;
        }

        public Builder addTeamError(String teamId, String message) {
            errors.add(ShareError.forTeam(teamId, message));
            return this;
        }

        public BulkShareResponse build() {
            return new BulkShareResponse(
                    createdShares.size(),
                    updatedShares.size(),
                    skippedCount,
                    errors.size(),
                    createdShares,
                    updatedShares,
                    errors
            );
        }
    }

    /**
     * Create a new builder.
     *
     * @return Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create a response for a single successful creation.
     *
     * @param share The created share
     * @return Response with one created share
     */
    public static BulkShareResponse singleCreated(TemplateShareDto share) {
        return new BulkShareResponse(1, 0, 0, 0, List.of(share), null, null);
    }

    /**
     * Create a response for a single successful update.
     *
     * @param share The updated share
     * @return Response with one updated share
     */
    public static BulkShareResponse singleUpdated(TemplateShareDto share) {
        return new BulkShareResponse(0, 1, 0, 0, null, List.of(share), null);
    }

    /**
     * Create a response for a single failure.
     *
     * @param error The error details
     * @return Response with one error
     */
    public static BulkShareResponse singleFailure(ShareError error) {
        return new BulkShareResponse(0, 0, 0, 1, null, null, List.of(error));
    }
}
