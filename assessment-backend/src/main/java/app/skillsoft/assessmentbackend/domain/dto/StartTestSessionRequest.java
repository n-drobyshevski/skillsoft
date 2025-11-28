package app.skillsoft.assessmentbackend.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request DTO for starting a new test session.
 */
public record StartTestSessionRequest(
        @NotNull(message = "Template ID is required")
        UUID templateId,

        @NotBlank(message = "User ID is required")
        String clerkUserId
) {
}
