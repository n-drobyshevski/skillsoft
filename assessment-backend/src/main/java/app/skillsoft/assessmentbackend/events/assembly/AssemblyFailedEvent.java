package app.skillsoft.assessmentbackend.events.assembly;

import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published when test assembly fails.
 * Used for observability, debugging, and alerting on assembly failures.
 *
 * @param sessionId The test session that failed assembly (may be null for pre-session failures)
 * @param templateId The template used for assembly
 * @param goal The assessment goal determining assembly strategy
 * @param errorMessage Human-readable error description
 * @param errorType The type of error (exception class name or category)
 * @param duration How long the assembly ran before failing
 * @param timestamp When the failure occurred
 */
public record AssemblyFailedEvent(
        UUID sessionId,
        UUID templateId,
        AssessmentGoal goal,
        String errorMessage,
        String errorType,
        Duration duration,
        Instant timestamp
) {
    /**
     * Factory method for creating an event from an exception.
     */
    public static AssemblyFailedEvent fromException(UUID sessionId, UUID templateId,
                                                     AssessmentGoal goal, Exception exception,
                                                     Instant startTime) {
        Duration duration = Duration.between(startTime, Instant.now());
        String errorType = exception.getClass().getSimpleName();
        String errorMessage = exception.getMessage() != null
                ? exception.getMessage()
                : "Unknown error";

        return new AssemblyFailedEvent(sessionId, templateId, goal,
                errorMessage, errorType, duration, Instant.now());
    }

    /**
     * Factory method for creating an event with a custom error.
     */
    public static AssemblyFailedEvent withError(UUID sessionId, UUID templateId,
                                                 AssessmentGoal goal, String errorMessage,
                                                 String errorType, Duration duration) {
        return new AssemblyFailedEvent(sessionId, templateId, goal,
                errorMessage, errorType, duration, Instant.now());
    }
}
