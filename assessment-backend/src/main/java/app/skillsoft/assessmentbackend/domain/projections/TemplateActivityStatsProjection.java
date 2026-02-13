package app.skillsoft.assessmentbackend.domain.projections;

import java.time.LocalDateTime;

/**
 * Type-safe projection interface for template activity statistics aggregate queries.
 *
 * This interface replaces the unsafe Object[] return type to prevent ClassCastException
 * issues caused by inconsistent Hibernate type handling of aggregate functions.
 *
 * JPQL aliases must match getter names (case-insensitive):
 * - COUNT(s) AS totalSessions -> getTotalSessions()
 * - COUNT(CASE WHEN s.status = 'COMPLETED'...) AS completedCount -> getCompletedCount()
 * - COUNT(CASE WHEN s.status = 'ABANDONED'...) AS abandonedCount -> getAbandonedCount()
 * - COUNT(CASE WHEN s.status = 'TIMED_OUT'...) AS timedOutCount -> getTimedOutCount()
 * - MAX(s.completedAt) AS lastActivity -> getLastActivity()
 */
public interface TemplateActivityStatsProjection {

    /**
     * Total number of sessions for this template (completed, abandoned, or timed out).
     * @return count of sessions, never null due to COUNT behavior
     */
    Long getTotalSessions();

    /**
     * Number of completed sessions for this template.
     * @return count of completed sessions, never null due to COUNT behavior
     */
    Long getCompletedCount();

    /**
     * Number of abandoned sessions for this template.
     * @return count of abandoned sessions, never null due to COUNT behavior
     */
    Long getAbandonedCount();

    /**
     * Number of timed-out sessions for this template.
     * @return count of timed-out sessions, never null due to COUNT behavior
     */
    Long getTimedOutCount();

    /**
     * Most recent activity timestamp for this template.
     * @return last completed/abandoned/timed-out timestamp, may be null if no activity
     */
    LocalDateTime getLastActivity();
}
