package app.skillsoft.assessmentbackend.events.scoring;

import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published when scoring calculation completes successfully.
 * Used for observability and metrics collection on scoring performance.
 *
 * @param sessionId The test session that was scored
 * @param resultId The result entity ID
 * @param goal The assessment goal that determined scoring strategy
 * @param overallScore The calculated overall score
 * @param passed Whether the test was passed
 * @param duration How long the scoring calculation took
 * @param timestamp When scoring completed
 */
public record ScoringCompletedEvent(
        UUID sessionId,
        UUID resultId,
        AssessmentGoal goal,
        double overallScore,
        boolean passed,
        Duration duration,
        Instant timestamp
) {
    /**
     * Factory method for creating an event with the current timestamp.
     */
    public static ScoringCompletedEvent now(UUID sessionId, UUID resultId,
                                             AssessmentGoal goal, double overallScore,
                                             boolean passed, Duration duration) {
        return new ScoringCompletedEvent(sessionId, resultId, goal,
                overallScore, passed, duration, Instant.now());
    }

    /**
     * Factory method for creating an event calculated from a start time.
     */
    public static ScoringCompletedEvent fromStart(UUID sessionId, UUID resultId,
                                                   AssessmentGoal goal, double overallScore,
                                                   boolean passed, Instant startTime) {
        Duration duration = Duration.between(startTime, Instant.now());
        return new ScoringCompletedEvent(sessionId, resultId, goal,
                overallScore, passed, duration, Instant.now());
    }
}
