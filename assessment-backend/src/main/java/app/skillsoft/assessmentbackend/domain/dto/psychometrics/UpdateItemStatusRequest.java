package app.skillsoft.assessmentbackend.domain.dto.psychometrics;

import app.skillsoft.assessmentbackend.domain.entities.ItemValidityStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for manually updating an item's validity status.
 * <p>
 * Used by HR administrators to override automated status determinations
 * when manual review determines a different action is appropriate.
 *
 * @param newStatus the new validity status to set
 * @param reason    human-readable reason for the status change (required for audit trail)
 */
public record UpdateItemStatusRequest(
        @NotNull(message = "New status is required")
        ItemValidityStatus newStatus,

        @NotNull(message = "Reason is required for status changes")
        @Size(min = 10, max = 500, message = "Reason must be between 10 and 500 characters")
        String reason
) {

    /**
     * Check if this is a retirement request.
     *
     * @return true if newStatus is RETIRED
     */
    public boolean isRetirement() {
        return newStatus == ItemValidityStatus.RETIRED;
    }

    /**
     * Check if this is an activation request.
     *
     * @return true if newStatus is ACTIVE
     */
    public boolean isActivation() {
        return newStatus == ItemValidityStatus.ACTIVE;
    }

    /**
     * Check if this is a flag for review request.
     *
     * @return true if newStatus is FLAGGED_FOR_REVIEW
     */
    public boolean isFlagForReview() {
        return newStatus == ItemValidityStatus.FLAGGED_FOR_REVIEW;
    }
}
