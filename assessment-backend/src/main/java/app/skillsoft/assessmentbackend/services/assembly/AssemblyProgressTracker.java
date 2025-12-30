package app.skillsoft.assessmentbackend.services.assembly;

import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;
import app.skillsoft.assessmentbackend.events.assembly.AssemblyProgress;
import app.skillsoft.assessmentbackend.events.assembly.AssemblyProgress.AssemblyPhase;
import app.skillsoft.assessmentbackend.events.assembly.AssemblyProgressEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for tracking and broadcasting test assembly progress.
 *
 * Provides real-time visibility into long-running assembly operations by:
 * - Tracking progress state in memory (ConcurrentHashMap)
 * - Publishing progress events via Spring's ApplicationEventPublisher
 * - Supporting both polling (getProgress) and push (events) access patterns
 *
 * Thread-safe: Uses ConcurrentHashMap for concurrent assembly tracking.
 *
 * Usage:
 * 1. Call start() when assembly begins
 * 2. Call updatePhase() for major phase transitions
 * 3. Call incrementCompetency() after each competency is processed
 * 4. Call complete() when assembly finishes successfully
 * 5. Call fail() if assembly encounters an error
 *
 * Progress is automatically cleaned up when complete/fail is called.
 */
@Service
public class AssemblyProgressTracker {

    private static final Logger log = LoggerFactory.getLogger(AssemblyProgressTracker.class);

    private final ApplicationEventPublisher eventPublisher;

    /**
     * In-memory storage for active assembly progress.
     * Key: sessionId, Value: current progress state
     */
    private final ConcurrentHashMap<UUID, AssemblyProgress> activeAssemblies = new ConcurrentHashMap<>();

    public AssemblyProgressTracker(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Start tracking a new assembly operation.
     *
     * @param sessionId The test session being assembled
     * @param templateId The template used for assembly
     * @param goal The assessment goal determining assembly strategy
     * @param totalCompetencies Total number of competencies to process
     * @return Initial AssemblyProgress state
     */
    public AssemblyProgress start(UUID sessionId, UUID templateId,
                                   AssessmentGoal goal, int totalCompetencies) {
        log.info("Starting assembly progress tracking for session {} (goal: {}, competencies: {})",
                sessionId, goal, totalCompetencies);

        AssemblyProgress progress = AssemblyProgress.start(sessionId, templateId, goal, totalCompetencies);
        activeAssemblies.put(sessionId, progress);
        publishProgress(progress, "Assembly started");

        return progress;
    }

    /**
     * Update the assembly phase.
     *
     * @param sessionId The session being assembled
     * @param phase New assembly phase
     * @param percent Completion percentage
     * @param message Progress message
     */
    public void updatePhase(UUID sessionId, AssemblyPhase phase, double percent, String message) {
        activeAssemblies.computeIfPresent(sessionId, (id, currentProgress) -> {
            AssemblyProgress updated = currentProgress.updatePhase(phase, percent);
            log.debug("Assembly phase update for session {}: {} ({}%)",
                    sessionId, phase, String.format("%.1f", percent));
            publishProgress(updated, message);
            return updated;
        });
    }

    /**
     * Update the assembly phase using phase name string.
     *
     * @param sessionId The session being assembled
     * @param phaseName Phase name (must match AssemblyPhase enum)
     * @param percent Completion percentage
     * @param message Progress message
     */
    public void updatePhase(UUID sessionId, String phaseName, double percent, String message) {
        try {
            AssemblyPhase phase = AssemblyPhase.valueOf(phaseName.toUpperCase());
            updatePhase(sessionId, phase, percent, message);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid assembly phase name: {}. Using SELECTING as default.", phaseName);
            updatePhase(sessionId, AssemblyPhase.SELECTING, percent, message);
        }
    }

    /**
     * Increment processed competency count after processing one competency.
     *
     * @param sessionId The session being assembled
     * @param questionsAdded Number of questions added for this competency
     * @param competencyName Name of the processed competency (for logging)
     */
    public void incrementCompetency(UUID sessionId, int questionsAdded, String competencyName) {
        activeAssemblies.computeIfPresent(sessionId, (id, currentProgress) -> {
            AssemblyProgress updated = currentProgress.incrementCompetency(questionsAdded);
            log.debug("Assembly progress for session {}: processed competency '{}' (+{} questions, {}%)",
                    sessionId, competencyName, questionsAdded, String.format("%.1f", updated.percentComplete()));
            publishProgress(updated, "Processing: " + competencyName);
            return updated;
        });
    }

    /**
     * Mark assembly as complete and remove from active tracking.
     *
     * @param sessionId The session that completed assembly
     * @param totalQuestions Final total question count
     */
    public void complete(UUID sessionId, int totalQuestions) {
        AssemblyProgress progress = activeAssemblies.remove(sessionId);
        if (progress != null) {
            AssemblyProgress completed = progress.complete(totalQuestions);
            log.info("Assembly completed for session {}: {} questions in {}ms",
                    sessionId, totalQuestions, completed.getElapsedDuration().toMillis());
            publishProgress(completed, "Assembly complete: " + totalQuestions + " questions");
        } else {
            log.debug("No active assembly found for session {} to complete", sessionId);
        }
    }

    /**
     * Mark assembly as failed and remove from active tracking.
     *
     * @param sessionId The session that failed assembly
     * @param errorMessage Error description
     */
    public void fail(UUID sessionId, String errorMessage) {
        AssemblyProgress progress = activeAssemblies.remove(sessionId);
        if (progress != null) {
            AssemblyProgress failed = progress.fail();
            log.error("Assembly failed for session {}: {} (after {}ms)",
                    sessionId, errorMessage, failed.getElapsedDuration().toMillis());
            publishProgress(failed, "Assembly failed: " + errorMessage);
        } else {
            log.debug("No active assembly found for session {} to mark as failed", sessionId);
        }
    }

    /**
     * Get the current progress for a session.
     *
     * @param sessionId The session ID to look up
     * @return Current progress if assembly is active, empty otherwise
     */
    public Optional<AssemblyProgress> getProgress(UUID sessionId) {
        return Optional.ofNullable(activeAssemblies.get(sessionId));
    }

    /**
     * Check if an assembly is currently in progress for a session.
     *
     * @param sessionId The session ID to check
     * @return true if assembly is active
     */
    public boolean isAssemblyInProgress(UUID sessionId) {
        AssemblyProgress progress = activeAssemblies.get(sessionId);
        return progress != null && progress.isInProgress();
    }

    /**
     * Get count of currently active assemblies.
     * Useful for monitoring and debugging.
     *
     * @return Number of assemblies currently in progress
     */
    public int getActiveAssemblyCount() {
        return activeAssemblies.size();
    }

    /**
     * Publish a progress event.
     *
     * @param progress Current progress state
     * @param message Human-readable message
     */
    private void publishProgress(AssemblyProgress progress, String message) {
        AssemblyProgressEvent event = AssemblyProgressEvent.from(progress, message);
        eventPublisher.publishEvent(event);

        log.trace("Published assembly progress event for session {}: {} - {}",
                progress.sessionId(), progress.currentPhase(), message);
    }
}
