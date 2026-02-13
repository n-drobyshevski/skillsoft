package app.skillsoft.assessmentbackend.services.cleanup;

import app.skillsoft.assessmentbackend.config.SessionCleanupProperties;
import app.skillsoft.assessmentbackend.domain.entities.SessionStatus;
import app.skillsoft.assessmentbackend.domain.entities.TestSession;
import app.skillsoft.assessmentbackend.repository.TestSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job to cleanup stale and abandoned test sessions.
 *
 * <p>This job runs on a configurable schedule (default: 3 AM daily) and performs:
 * <ol>
 *   <li>Mark IN_PROGRESS sessions inactive for > configured hours as ABANDONED</li>
 *   <li>Mark NOT_STARTED sessions older than configured hours as ABANDONED</li>
 *   <li>Delete empty ABANDONED sessions (no answers, no results) older than configured days</li>
 * </ol>
 *
 * <p>Configuration via {@code skillsoft.session.cleanup.*} properties.
 *
 * @see SessionCleanupProperties
 */
@Service
public class SessionCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(SessionCleanupJob.class);

    private final TestSessionRepository sessionRepository;
    private final SessionCleanupProperties config;

    public SessionCleanupJob(
            TestSessionRepository sessionRepository,
            SessionCleanupProperties config) {
        this.sessionRepository = sessionRepository;
        this.config = config;
    }

    /**
     * Main cleanup job running on configured schedule.
     * Default: 3 AM daily
     */
    @Scheduled(cron = "${skillsoft.session.cleanup.cron:0 0 3 * * ?}")
    @Transactional
    public void cleanupStaleSessions() {
        if (!config.isEnabled()) {
            log.debug("Session cleanup is disabled, skipping");
            return;
        }

        log.info("Starting session cleanup job");
        var startTime = System.currentTimeMillis();

        try {
            CleanupResult result = new CleanupResult();

            // Phase 1: Mark stale IN_PROGRESS sessions as ABANDONED
            result.inProgressAbandoned = abandonStaleInProgressSessions();

            // Phase 2: Mark old NOT_STARTED sessions as ABANDONED
            result.notStartedAbandoned = abandonOldNotStartedSessions();

            // Phase 3: Delete empty abandoned sessions (optional)
            result.emptySessionsDeleted = deleteEmptyAbandonedSessions();

            var duration = System.currentTimeMillis() - startTime;
            log.info("Session cleanup completed in {}ms: {} IN_PROGRESS abandoned, " +
                    "{} NOT_STARTED abandoned, {} empty sessions deleted",
                    duration, result.inProgressAbandoned,
                    result.notStartedAbandoned, result.emptySessionsDeleted);

        } catch (Exception e) {
            log.error("Session cleanup job failed", e);
        }
    }

    /**
     * Find and abandon IN_PROGRESS sessions that have been inactive too long.
     * Uses batch saveAll() instead of individual save() calls.
     */
    private int abandonStaleInProgressSessions() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(config.getStaleHours());

        List<TestSession> staleSessions = sessionRepository.findStaleSessions(
                SessionStatus.IN_PROGRESS, cutoffTime);

        if (staleSessions.isEmpty()) {
            return 0;
        }

        for (TestSession session : staleSessions) {
            session.abandon();
        }

        sessionRepository.saveAll(staleSessions);
        log.debug("Batch-abandoned {} stale IN_PROGRESS sessions", staleSessions.size());
        return staleSessions.size();
    }

    /**
     * Find and abandon NOT_STARTED sessions that are too old.
     * Uses batch saveAll() instead of individual save() calls.
     */
    private int abandonOldNotStartedSessions() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(config.getStaleHours());

        List<TestSession> oldSessions = sessionRepository.findStaleSessions(
                SessionStatus.NOT_STARTED, cutoffTime);

        if (oldSessions.isEmpty()) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        for (TestSession session : oldSessions) {
            session.setStatus(SessionStatus.ABANDONED);
            session.setCompletedAt(now);
        }

        sessionRepository.saveAll(oldSessions);
        log.debug("Batch-abandoned {} old NOT_STARTED sessions", oldSessions.size());
        return oldSessions.size();
    }

    /**
     * Delete abandoned sessions that have no answers and are old enough.
     * Uses batch deleteAllInBatch() instead of individual delete() calls.
     */
    private int deleteEmptyAbandonedSessions() {
        if (config.getDeleteEmptyAfterDays() <= 0) {
            return 0; // Feature disabled
        }

        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(config.getDeleteEmptyAfterDays());

        List<TestSession> abandonedSessions = sessionRepository.findStaleSessions(
                SessionStatus.ABANDONED, cutoffTime);

        // Filter to only empty sessions (no answers and no result)
        List<TestSession> emptyToDelete = abandonedSessions.stream()
                .filter(session -> (session.getAnswers() == null || session.getAnswers().isEmpty())
                        && session.getResult() == null)
                .toList();

        if (emptyToDelete.isEmpty()) {
            return 0;
        }

        sessionRepository.deleteAllInBatch(emptyToDelete);
        log.debug("Batch-deleted {} empty abandoned sessions", emptyToDelete.size());
        return emptyToDelete.size();
    }

    /**
     * Manual trigger for cleanup (can be called from admin API).
     * @return Result of the cleanup operation
     */
    @Transactional
    public CleanupResult triggerManualCleanup() {
        log.info("Manual session cleanup triggered");

        CleanupResult result = new CleanupResult();
        result.inProgressAbandoned = abandonStaleInProgressSessions();
        result.notStartedAbandoned = abandonOldNotStartedSessions();
        result.emptySessionsDeleted = deleteEmptyAbandonedSessions();

        return result;
    }

    /**
     * Result record for cleanup operations.
     */
    public static class CleanupResult {
        public int inProgressAbandoned = 0;
        public int notStartedAbandoned = 0;
        public int emptySessionsDeleted = 0;

        public int getTotalProcessed() {
            return inProgressAbandoned + notStartedAbandoned + emptySessionsDeleted;
        }

        @Override
        public String toString() {
            return String.format("CleanupResult{inProgress=%d, notStarted=%d, deleted=%d, total=%d}",
                    inProgressAbandoned, notStartedAbandoned, emptySessionsDeleted, getTotalProcessed());
        }
    }
}
