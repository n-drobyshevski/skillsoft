package app.skillsoft.assessmentbackend.events.assembly;

import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Tracks the progress state of a test assembly operation.
 *
 * Used to provide real-time visibility into long-running assembly processes,
 * especially for large tests with many competencies.
 *
 * Immutable record - each update creates a new instance.
 *
 * @param sessionId The test session being assembled
 * @param templateId The template used for assembly
 * @param goal The assessment goal determining assembly strategy
 * @param totalCompetencies Total number of competencies to process
 * @param processedCompetencies Number of competencies processed so far
 * @param totalQuestionsSelected Total questions selected across all competencies
 * @param currentPhase Current assembly phase (INITIALIZING, SELECTING, VALIDATING, SHUFFLING, COMPLETE)
 * @param percentComplete Completion percentage (0.0 - 100.0)
 * @param startTime When assembly started
 * @param lastUpdate When progress was last updated
 */
public record AssemblyProgress(
        UUID sessionId,
        UUID templateId,
        AssessmentGoal goal,
        int totalCompetencies,
        int processedCompetencies,
        int totalQuestionsSelected,
        AssemblyPhase currentPhase,
        double percentComplete,
        Instant startTime,
        Instant lastUpdate
) {
    /**
     * Assembly phases for tracking progress.
     */
    public enum AssemblyPhase {
        /** Initial setup and validation */
        INITIALIZING,
        /** Selecting questions for competencies */
        SELECTING,
        /** Validating selected questions */
        VALIDATING,
        /** Shuffling question order */
        SHUFFLING,
        /** Assembly complete */
        COMPLETE,
        /** Assembly failed */
        FAILED
    }

    /**
     * Factory method to start tracking a new assembly operation.
     *
     * @param sessionId The session being assembled
     * @param templateId The template ID
     * @param goal The assessment goal
     * @param totalCompetencies Total competencies to process
     * @return New AssemblyProgress in INITIALIZING phase
     */
    public static AssemblyProgress start(UUID sessionId, UUID templateId,
                                          AssessmentGoal goal, int totalCompetencies) {
        Instant now = Instant.now();
        return new AssemblyProgress(
                sessionId,
                templateId,
                goal,
                totalCompetencies,
                0,
                0,
                AssemblyPhase.INITIALIZING,
                0.0,
                now,
                now
        );
    }

    /**
     * Update the current phase and percent complete.
     *
     * @param phase New assembly phase
     * @param percent New completion percentage
     * @return Updated AssemblyProgress
     */
    public AssemblyProgress updatePhase(AssemblyPhase phase, double percent) {
        return new AssemblyProgress(
                sessionId,
                templateId,
                goal,
                totalCompetencies,
                processedCompetencies,
                totalQuestionsSelected,
                phase,
                percent,
                startTime,
                Instant.now()
        );
    }

    /**
     * Increment processed competency count and update questions selected.
     *
     * @param questionsAdded Number of questions added for this competency
     * @return Updated AssemblyProgress with incremented counts
     */
    public AssemblyProgress incrementCompetency(int questionsAdded) {
        int newProcessed = processedCompetencies + 1;
        double percent = totalCompetencies > 0
                ? (double) newProcessed / totalCompetencies * 100.0
                : 0.0;
        return new AssemblyProgress(
                sessionId,
                templateId,
                goal,
                totalCompetencies,
                newProcessed,
                totalQuestionsSelected + questionsAdded,
                AssemblyPhase.SELECTING,
                percent,
                startTime,
                Instant.now()
        );
    }

    /**
     * Mark assembly as complete.
     *
     * @param finalQuestionCount Final total question count
     * @return Updated AssemblyProgress in COMPLETE phase
     */
    public AssemblyProgress complete(int finalQuestionCount) {
        return new AssemblyProgress(
                sessionId,
                templateId,
                goal,
                totalCompetencies,
                totalCompetencies,
                finalQuestionCount,
                AssemblyPhase.COMPLETE,
                100.0,
                startTime,
                Instant.now()
        );
    }

    /**
     * Mark assembly as failed.
     *
     * @return Updated AssemblyProgress in FAILED phase
     */
    public AssemblyProgress fail() {
        return new AssemblyProgress(
                sessionId,
                templateId,
                goal,
                totalCompetencies,
                processedCompetencies,
                totalQuestionsSelected,
                AssemblyPhase.FAILED,
                percentComplete,
                startTime,
                Instant.now()
        );
    }

    /**
     * Calculate elapsed duration since assembly started.
     *
     * @return Duration since start
     */
    public Duration getElapsedDuration() {
        return Duration.between(startTime, lastUpdate);
    }

    /**
     * Check if assembly is still in progress.
     *
     * @return true if not in COMPLETE or FAILED phase
     */
    public boolean isInProgress() {
        return currentPhase != AssemblyPhase.COMPLETE && currentPhase != AssemblyPhase.FAILED;
    }
}
