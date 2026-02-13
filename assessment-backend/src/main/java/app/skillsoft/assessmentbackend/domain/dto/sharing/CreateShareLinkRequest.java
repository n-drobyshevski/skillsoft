package app.skillsoft.assessmentbackend.domain.dto.sharing;

import app.skillsoft.assessmentbackend.domain.entities.SharePermission;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new template share link.
 *
 * @param expiresInDays Number of days until the link expires (1-365, default 7)
 * @param maxUses Maximum number of times the link can be used (null for unlimited)
 * @param permission Permission level granted by this link (VIEW or EDIT only)
 * @param label Optional human-readable label for the link
 */
public record CreateShareLinkRequest(
        @Min(value = 1, message = "Expiry must be at least 1 day")
        @Max(value = 365, message = "Expiry cannot exceed 365 days")
        Integer expiresInDays,

        @Min(value = 1, message = "Max uses must be at least 1")
        Integer maxUses,

        SharePermission permission,

        @Size(max = 200, message = "Label cannot exceed 200 characters")
        String label
) {
    /**
     * Default constructor with sensible defaults.
     */
    public CreateShareLinkRequest {
        if (expiresInDays == null) {
            expiresInDays = 7;
        }
        if (permission == null) {
            permission = SharePermission.VIEW;
        }
    }

    /**
     * Create a minimal request with defaults.
     */
    public static CreateShareLinkRequest defaults() {
        return new CreateShareLinkRequest(null, null, null, null);
    }

    /**
     * Create a request with custom expiry only.
     */
    public static CreateShareLinkRequest withExpiry(int days) {
        return new CreateShareLinkRequest(days, null, null, null);
    }

    /**
     * Create a labeled link with defaults.
     */
    public static CreateShareLinkRequest withLabel(String label) {
        return new CreateShareLinkRequest(null, null, null, label);
    }
}
