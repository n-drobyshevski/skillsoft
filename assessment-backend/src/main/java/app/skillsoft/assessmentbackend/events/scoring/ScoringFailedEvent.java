package app.skillsoft.assessmentbackend.events.scoring;

import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published when scoring calculation fails.
 * Used for observability, debugging, and alerting on scoring failures.
 *
 * @param sessionId The test session that failed scoring
 * @param goal The assessment goal that determined scoring strategy
 * @param errorMessage Human-readable error description
 * @param errorType The type of error (exception class name or category)
 * @param duration How long the scoring ran before failing
 * @param timestamp When the failure occurred
 */
public record ScoringFailedEvent(
        UUID sessionId,
        AssessmentGoal goal,
        String errorMessage,
        String errorType,
        Duration duration,
        Instant timestamp
) {
    /**
     * Factory method for creating an event from an exception.
     */
    public static ScoringFailedEvent fromException(UUID sessionId, AssessmentGoal goal,
                                                    Exception exception, Instant startTime) {
        Duration duration = Duration.between(startTime, Instant.now());
        String errorType = exception.getClass().getSimpleName();
        String errorMessage = exception.getMessage() != null
                ? exception.getMessage()
                : "Unknown error";

        return new ScoringFailedEvent(sessionId, goal, errorMessage, errorType,
                duration, Instant.now());
    }

    /**
     * Factory method for creating an event with a custom error.
     */
    public static ScoringFailedEvent withError(UUID sessionId, AssessmentGoal goal,
                                                String errorMessage, String errorType,
                                                Duration duration) {
        return new ScoringFailedEvent(sessionId, goal, errorMessage, errorType,
                duration, Instant.now());
    }
}
