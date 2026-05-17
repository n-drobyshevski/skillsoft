package app.skillsoft.assessmentbackend.domain.dto;

/**
 * Response wrapper for anonymous session completion.
 *
 * <p>Wraps the standard TestResultDto with an additional resultViewToken
 * that allows the anonymous taker to access their results later via a
 * persistent, HMAC-signed URL.</p>
 */
public record AnonymousCompletionResponse(
        /**
         * The test result.
         */
        TestResultDto result,

        /**
         * HMAC-signed token for persistent result access.
         * Can be appended to the public result URL: /results/{resultViewToken}
         * Valid for 7 days by default.
         */
        String resultViewToken
) {
}
