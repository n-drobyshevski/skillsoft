package app.skillsoft.assessmentbackend.events.assembly;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published to broadcast assembly progress updates.
 *
 * Used for real-time progress tracking during test assembly.
 * Listeners can use this event for:
 * - WebSocket broadcasting to frontend
 * - Metrics collection
 * - Logging and monitoring
 *
 * @param sessionId The test session being assembled
 * @param phase Current assembly phase name
 * @param percentComplete Completion percentage (0.0 - 100.0)
 * @param questionsSelected Total questions selected so far
 * @param message Human-readable progress message
 * @param timestamp When this progress update occurred
 */
public record AssemblyProgressEvent(
        UUID sessionId,
        String phase,
        double percentComplete,
        int questionsSelected,
        String message,
        Instant timestamp
) {
    /**
     * Factory method to create an event from AssemblyProgress.
     *
     * @param progress The current progress state
     * @param message Human-readable message describing the progress
     * @return New AssemblyProgressEvent
     */
    public static AssemblyProgressEvent from(AssemblyProgress progress, String message) {
        return new AssemblyProgressEvent(
                progress.sessionId(),
                progress.currentPhase().name(),
                progress.percentComplete(),
                progress.totalQuestionsSelected(),
                message,
                Instant.now()
        );
    }

    /**
     * Factory method to create an event with current timestamp.
     *
     * @param sessionId The session ID
     * @param phase Assembly phase name
     * @param percentComplete Completion percentage
     * @param questionsSelected Questions selected count
     * @param message Progress message
     * @return New AssemblyProgressEvent
     */
    public static AssemblyProgressEvent now(UUID sessionId, String phase, double percentComplete,
                                             int questionsSelected, String message) {
        return new AssemblyProgressEvent(
                sessionId,
                phase,
                percentComplete,
                questionsSelected,
                message,
                Instant.now()
        );
    }
}
