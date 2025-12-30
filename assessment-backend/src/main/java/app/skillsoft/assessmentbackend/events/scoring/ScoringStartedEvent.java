package app.skillsoft.assessmentbackend.events.scoring;

import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when scoring calculation begins.
 * Used for observability and metrics collection on scoring performance.
 *
 * @param sessionId The test session being scored
 * @param resultId The result entity ID (may be null if not yet created)
 * @param goal The assessment goal determining scoring strategy
 * @param answerCount Number of answers to be scored
 * @param timestamp When scoring started
 */
public record ScoringStartedEvent(
        UUID sessionId,
        UUID resultId,
        AssessmentGoal goal,
        int answerCount,
        Instant timestamp
) {
    /**
     * Factory method for creating an event with the current timestamp.
     */
    public static ScoringStartedEvent now(UUID sessionId, UUID resultId,
                                           AssessmentGoal goal, int answerCount) {
        return new ScoringStartedEvent(sessionId, resultId, goal, answerCount, Instant.now());
    }

    /**
     * Factory method for creating an event before result is created.
     */
    public static ScoringStartedEvent beforeResult(UUID sessionId, AssessmentGoal goal,
                                                    int answerCount) {
        return new ScoringStartedEvent(sessionId, null, goal, answerCount, Instant.now());
    }
}
