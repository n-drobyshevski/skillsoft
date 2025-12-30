package app.skillsoft.assessmentbackend.events.assembly;

import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published when test assembly completes successfully.
 * Used for observability and metrics collection on assembly performance.
 *
 * @param sessionId The test session that was assembled
 * @param templateId The template used for assembly
 * @param goal The assessment goal determining assembly strategy
 * @param questionCount Number of questions assembled for the session
 * @param duration How long the assembly process took
 * @param timestamp When assembly completed
 */
public record AssemblyCompletedEvent(
        UUID sessionId,
        UUID templateId,
        AssessmentGoal goal,
        int questionCount,
        Duration duration,
        Instant timestamp
) {
    /**
     * Factory method for creating an event with the current timestamp.
     */
    public static AssemblyCompletedEvent now(UUID sessionId, UUID templateId,
                                              AssessmentGoal goal, int questionCount,
                                              Duration duration) {
        return new AssemblyCompletedEvent(sessionId, templateId, goal,
                questionCount, duration, Instant.now());
    }

    /**
     * Factory method for creating an event calculated from a start time.
     */
    public static AssemblyCompletedEvent fromStart(UUID sessionId, UUID templateId,
                                                    AssessmentGoal goal, int questionCount,
                                                    Instant startTime) {
        Duration duration = Duration.between(startTime, Instant.now());
        return new AssemblyCompletedEvent(sessionId, templateId, goal,
                questionCount, duration, Instant.now());
    }
}
