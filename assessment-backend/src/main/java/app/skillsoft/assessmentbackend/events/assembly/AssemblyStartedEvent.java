package app.skillsoft.assessmentbackend.events.assembly;

import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when test assembly begins.
 * Used for observability and metrics collection on assembly performance.
 *
 * @param sessionId The test session being assembled
 * @param templateId The template used for assembly
 * @param goal The assessment goal determining assembly strategy
 * @param assemblerType The type of assembler being used (e.g., "OverviewAssembler")
 * @param timestamp When assembly started
 */
public record AssemblyStartedEvent(
        UUID sessionId,
        UUID templateId,
        AssessmentGoal goal,
        String assemblerType,
        Instant timestamp
) {
    /**
     * Factory method for creating an event with the current timestamp.
     */
    public static AssemblyStartedEvent now(UUID sessionId, UUID templateId,
                                            AssessmentGoal goal, String assemblerType) {
        return new AssemblyStartedEvent(sessionId, templateId, goal, assemblerType, Instant.now());
    }
}
