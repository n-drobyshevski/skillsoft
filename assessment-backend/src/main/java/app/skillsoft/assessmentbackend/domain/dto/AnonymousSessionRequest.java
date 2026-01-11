package app.skillsoft.assessmentbackend.domain.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for creating an anonymous test session.
 * Used when a user accesses a test via a share link.
 */
public record AnonymousSessionRequest(
        /**
         * The share link token (64 characters, Base64 URL-safe).
         * Obtained from the share link URL.
         */
        @NotBlank(message = "Share token is required")
        String shareToken
) {
}
