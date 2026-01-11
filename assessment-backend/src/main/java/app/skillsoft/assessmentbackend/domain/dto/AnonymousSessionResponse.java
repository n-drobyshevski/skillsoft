package app.skillsoft.assessmentbackend.domain.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for anonymous session creation.
 * Contains the session access token needed for subsequent API calls.
 */
public record AnonymousSessionResponse(
        /**
         * The created session UUID.
         */
        UUID sessionId,

        /**
         * Session access token for authenticating subsequent requests.
         * This token should be stored securely by the client and passed
         * in the X-Session-Token header for all session operations.
         * 43 characters, Base64 URL-safe encoded.
         */
        String sessionAccessToken,

        /**
         * Summary information about the test template.
         */
        TemplateInfo template,

        /**
         * When this session expires (24 hours from creation).
         * The session must be completed before this time.
         */
        LocalDateTime expiresAt
) {
    /**
     * Nested record for template information shown to anonymous users.
     */
    public record TemplateInfo(
            UUID id,
            String name,
            String description,
            Integer questionCount,
            Integer timeLimitMinutes,
            Boolean allowSkip,
            Boolean allowBackNavigation
    ) {
    }
}
