package app.skillsoft.assessmentbackend.events.resilience;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a retry attempt occurs in resilience-protected operations.
 * Used for observability and tracking retry behavior in scoring and other critical paths.
 *
 * @param operationName The name of the resilience4j retry instance (e.g., "scoringCalculation")
 * @param sessionId Optional session ID for context (may be null for non-session operations)
 * @param attemptNumber The current retry attempt number (1-based, where 1 is the first retry after initial failure)
 * @param maxAttempts The maximum number of retry attempts configured
 * @param errorMessage The error message from the failed attempt
 * @param errorType The exception class name that caused the retry
 * @param timestamp When the retry occurred
 */
public record ResilienceRetryEvent(
        String operationName,
        UUID sessionId,
        int attemptNumber,
        int maxAttempts,
        String errorMessage,
        String errorType,
        Instant timestamp
) {
    /**
     * Factory method for creating a retry event from an exception.
     */
    public static ResilienceRetryEvent fromException(
            String operationName,
            UUID sessionId,
            int attemptNumber,
            int maxAttempts,
            Throwable exception) {
        String errorType = exception != null ? exception.getClass().getSimpleName() : "Unknown";
        String errorMessage = exception != null && exception.getMessage() != null
                ? exception.getMessage()
                : "Unknown error";

        return new ResilienceRetryEvent(
                operationName,
                sessionId,
                attemptNumber,
                maxAttempts,
                errorMessage,
                errorType,
                Instant.now()
        );
    }

    /**
     * Factory method for scoring calculation retries.
     */
    public static ResilienceRetryEvent forScoringRetry(
            UUID sessionId,
            int attemptNumber,
            int maxAttempts,
            Throwable exception) {
        return fromException("scoringCalculation", sessionId, attemptNumber, maxAttempts, exception);
    }
}
