package app.skillsoft.assessmentbackend.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for submitting anonymous taker information.
 * Collected after test completion.
 */
public record AnonymousTakerInfoRequest(
        /**
         * Taker's first name (required).
         */
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must be 100 characters or less")
        String firstName,

        /**
         * Taker's last name (required).
         */
        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must be 100 characters or less")
        String lastName,

        /**
         * Taker's email address (optional).
         * Used for follow-up communication if provided.
         */
        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Email must be 255 characters or less")
        String email,

        /**
         * Additional notes or context (optional).
         * Examples: department, job title, referral source.
         */
        @Size(max = 500, message = "Notes must be 500 characters or less")
        String notes
) {
}
