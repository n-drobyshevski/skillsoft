package app.skillsoft.assessmentbackend.events.resilience;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a fallback method is invoked after resilience mechanisms are exhausted.
 * Used for observability and alerting on degraded service operation.
 *
 * @param operationName The name of the resilience4j instance (e.g., "scoringCalculation")
 * @param sessionId Optional session ID for context (may be null for non-session operations)
 * @param reason The reason for fallback invocation (e.g., "retry_exhausted", "circuit_open")
 * @param errorMessage The final error message that triggered the fallback
 * @param errorType The exception class name that triggered the fallback
 * @param totalAttempts Total number of attempts made before fallback (including initial attempt)
 * @param timestamp When the fallback was invoked
 */
public record ResilienceFallbackEvent(
        String operationName,
        UUID sessionId,
        FallbackReason reason,
        String errorMessage,
        String errorType,
        int totalAttempts,
        Instant timestamp
) {
    /**
     * Enum representing reasons for fallback invocation.
     */
    public enum FallbackReason {
        /** All retry attempts exhausted */
        RETRY_EXHAUSTED,
        /** Circuit breaker is open */
        CIRCUIT_OPEN,
        /** Timeout exceeded */
        TIMEOUT,
        /** Bulkhead rejected */
        BULKHEAD_REJECTED,
        /** Other/unknown reason */
        OTHER
    }

    /**
     * Factory method for creating a fallback event after retry exhaustion.
     */
    public static ResilienceFallbackEvent retryExhausted(
            String operationName,
            UUID sessionId,
            int totalAttempts,
            Throwable exception) {
        String errorType = exception != null ? exception.getClass().getSimpleName() : "Unknown";
        String errorMessage = exception != null && exception.getMessage() != null
                ? exception.getMessage()
                : "Unknown error";

        return new ResilienceFallbackEvent(
                operationName,
                sessionId,
                FallbackReason.RETRY_EXHAUSTED,
                errorMessage,
                errorType,
                totalAttempts,
                Instant.now()
        );
    }

    /**
     * Factory method for creating a fallback event due to open circuit.
     */
    public static ResilienceFallbackEvent circuitOpen(
            String operationName,
            UUID sessionId,
            Throwable exception) {
        String errorType = exception != null ? exception.getClass().getSimpleName() : "CircuitBreakerOpenException";
        String errorMessage = exception != null && exception.getMessage() != null
                ? exception.getMessage()
                : "Circuit breaker is open";

        return new ResilienceFallbackEvent(
                operationName,
                sessionId,
                FallbackReason.CIRCUIT_OPEN,
                errorMessage,
                errorType,
                0,
                Instant.now()
        );
    }

    /**
     * Factory method for scoring calculation fallback.
     */
    public static ResilienceFallbackEvent forScoringFallback(
            UUID sessionId,
            int totalAttempts,
            Throwable exception) {
        return retryExhausted("scoringCalculation", sessionId, totalAttempts, exception);
    }
}
