package app.skillsoft.assessmentbackend.domain.dto;

import app.skillsoft.assessmentbackend.events.assembly.AssemblyProgress;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for exposing assembly progress via REST endpoint.
 *
 * Provides visibility into long-running assembly operations.
 *
 * @param sessionId The test session being assembled
 * @param templateId The template used for assembly
 * @param phase Current assembly phase
 * @param totalCompetencies Total competencies to process
 * @param processedCompetencies Competencies processed so far
 * @param questionsSelected Total questions selected
 * @param percentComplete Completion percentage (0-100)
 * @param elapsedMillis Elapsed time in milliseconds
 * @param message Human-readable status message
 * @param inProgress Whether assembly is still running
 */
public record AssemblyProgressDto(
        UUID sessionId,
        UUID templateId,
        String phase,
        int totalCompetencies,
        int processedCompetencies,
        int questionsSelected,
        double percentComplete,
        long elapsedMillis,
        String message,
        boolean inProgress
) {
    /**
     * Factory method to create DTO from AssemblyProgress.
     *
     * @param progress The assembly progress state
     * @return DTO representation
     */
    public static AssemblyProgressDto from(AssemblyProgress progress) {
        String message = buildStatusMessage(progress);
        return new AssemblyProgressDto(
                progress.sessionId(),
                progress.templateId(),
                progress.currentPhase().name(),
                progress.totalCompetencies(),
                progress.processedCompetencies(),
                progress.totalQuestionsSelected(),
                progress.percentComplete(),
                progress.getElapsedDuration().toMillis(),
                message,
                progress.isInProgress()
        );
    }

    /**
     * Build a human-readable status message.
     */
    private static String buildStatusMessage(AssemblyProgress progress) {
        return switch (progress.currentPhase()) {
            case INITIALIZING -> "Initializing assembly...";
            case SELECTING -> String.format(
                    "Selecting questions (%d/%d competencies, %d questions)",
                    progress.processedCompetencies(),
                    progress.totalCompetencies(),
                    progress.totalQuestionsSelected()
            );
            case VALIDATING -> "Validating selected questions...";
            case SHUFFLING -> "Shuffling question order...";
            case COMPLETE -> String.format(
                    "Assembly complete: %d questions ready",
                    progress.totalQuestionsSelected()
            );
            case FAILED -> "Assembly failed";
        };
    }

    /**
     * Create a "not found" response for when no assembly is in progress.
     */
    public static AssemblyProgressDto notFound(UUID sessionId) {
        return new AssemblyProgressDto(
                sessionId,
                null,
                "NOT_FOUND",
                0,
                0,
                0,
                0.0,
                0,
                "No assembly in progress for this session",
                false
        );
    }
}
