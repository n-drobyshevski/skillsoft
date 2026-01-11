package app.skillsoft.assessmentbackend.repository;

import app.skillsoft.assessmentbackend.domain.entities.SessionStatus;
import app.skillsoft.assessmentbackend.domain.entities.TestSession;
import app.skillsoft.assessmentbackend.domain.projections.TemplateActivityStatsProjection;
import app.skillsoft.assessmentbackend.domain.projections.TemplateScoreTimeProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestSessionRepository extends JpaRepository<TestSession, UUID> {

    /**
     * Find all sessions for a specific user
     */
    List<TestSession> findByClerkUserId(String clerkUserId);

    /**
     * Find all sessions for a specific user with pagination
     */
    Page<TestSession> findByClerkUserId(String clerkUserId, Pageable pageable);

    /**
     * Find sessions by user and status
     */
    List<TestSession> findByClerkUserIdAndStatus(String clerkUserId, SessionStatus status);

    /**
     * Find sessions by user and multiple statuses
     */
    List<TestSession> findByClerkUserIdAndStatusIn(String clerkUserId, List<SessionStatus> statuses);

    /**
     * Find all sessions for a specific template
     */
    List<TestSession> findByTemplate_Id(UUID templateId);

    /**
     * Find sessions by template with pagination
     */
    Page<TestSession> findByTemplate_Id(UUID templateId, Pageable pageable);

    /**
     * Find the most recent active (in-progress) session for a user
     */
    Optional<TestSession> findFirstByClerkUserIdAndStatusOrderByCreatedAtDesc(
            String clerkUserId, SessionStatus status);

    /**
     * Find in-progress sessions for a user on a specific template
     * Useful for resuming tests
     */
    Optional<TestSession> findByClerkUserIdAndTemplate_IdAndStatus(
            String clerkUserId, UUID templateId, SessionStatus status);

    /**
     * Count completed sessions for a template (for statistics)
     */
    long countByTemplate_IdAndStatus(UUID templateId, SessionStatus status);

    /**
     * Count sessions by user and status
     */
    long countByClerkUserIdAndStatus(String clerkUserId, SessionStatus status);

    /**
     * Find sessions that are in progress but inactive for too long (for cleanup/timeout)
     */
    @Query("SELECT s FROM TestSession s WHERE s.status = :status AND s.lastActivityAt < :cutoffTime")
    List<TestSession> findStaleSessions(
            @Param("status") SessionStatus status, 
            @Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Find all in-progress sessions that should be timed out
     */
    @Query("SELECT s FROM TestSession s WHERE s.status = 'IN_PROGRESS' AND s.timeRemainingSeconds <= 0")
    List<TestSession> findSessionsToTimeout();

    /**
     * Update session activity timestamp
     */
    @Modifying
    @Query("UPDATE TestSession s SET s.lastActivityAt = :timestamp WHERE s.id = :sessionId")
    int updateLastActivityAt(@Param("sessionId") UUID sessionId, @Param("timestamp") LocalDateTime timestamp);

    /**
     * Check if user has any completed sessions for a template
     */
    boolean existsByClerkUserIdAndTemplate_IdAndStatus(String clerkUserId, UUID templateId, SessionStatus status);

    /**
     * Find sessions created within a date range (for reporting)
     */
    List<TestSession> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Count sessions by status (for dashboard statistics)
     */
    long countByStatus(SessionStatus status);

    // ============================================
    // OPTIMIZED QUERIES (N+1 Prevention)
    // ============================================

    /**
     * Find sessions for a user with template eagerly loaded.
     * Avoids N+1 query when accessing session.template in mapping loops.
     */
    @Query("SELECT s FROM TestSession s JOIN FETCH s.template WHERE s.clerkUserId = :userId")
    List<TestSession> findByClerkUserIdWithTemplate(@Param("userId") String userId);

    /**
     * Find sessions for a user with template eagerly loaded (paginated).
     * Note: COUNT query is separate to avoid pagination issues with JOIN FETCH.
     */
    @Query(value = "SELECT s FROM TestSession s JOIN FETCH s.template WHERE s.clerkUserId = :userId",
           countQuery = "SELECT COUNT(s) FROM TestSession s WHERE s.clerkUserId = :userId")
    Page<TestSession> findByClerkUserIdWithTemplate(@Param("userId") String userId, Pageable pageable);

    /**
     * Find sessions by user and status with template eagerly loaded.
     */
    @Query("SELECT s FROM TestSession s JOIN FETCH s.template WHERE s.clerkUserId = :userId AND s.status = :status")
    List<TestSession> findByClerkUserIdAndStatusWithTemplate(
            @Param("userId") String userId,
            @Param("status") SessionStatus status);

    /**
     * Find sessions by user and multiple statuses with template eagerly loaded.
     */
    @Query("SELECT s FROM TestSession s JOIN FETCH s.template WHERE s.clerkUserId = :userId AND s.status IN :statuses")
    List<TestSession> findByClerkUserIdAndStatusInWithTemplate(
            @Param("userId") String userId,
            @Param("statuses") List<SessionStatus> statuses);

    /**
     * Find session by ID with template eagerly loaded.
     */
    @Query("SELECT s FROM TestSession s JOIN FETCH s.template WHERE s.id = :sessionId")
    Optional<TestSession> findByIdWithTemplate(@Param("sessionId") UUID sessionId);

    // ============================================
    // ACTIVITY TRACKING QUERIES
    // ============================================

    /**
     * Find recent completed/abandoned/timed-out sessions with template for activity feed.
     * Returns sessions ordered by completion time (most recent first).
     */
    @Query("SELECT s FROM TestSession s JOIN FETCH s.template " +
           "WHERE s.status IN :statuses AND s.completedAt IS NOT NULL " +
           "ORDER BY s.completedAt DESC LIMIT :limit")
    List<TestSession> findRecentCompletedSessions(
            @Param("statuses") List<SessionStatus> statuses,
            @Param("limit") int limit);

    /**
     * Find activity for a specific template with pagination.
     */
    @Query(value = "SELECT s FROM TestSession s JOIN FETCH s.template " +
                   "WHERE s.template.id = :templateId " +
                   "AND s.status IN :statuses " +
                   "ORDER BY s.completedAt DESC",
           countQuery = "SELECT COUNT(s) FROM TestSession s " +
                        "WHERE s.template.id = :templateId AND s.status IN :statuses")
    Page<TestSession> findActivityByTemplateId(
            @Param("templateId") UUID templateId,
            @Param("statuses") List<SessionStatus> statuses,
            Pageable pageable);

    /**
     * Find activity for a specific template with status filter.
     */
    @Query(value = "SELECT s FROM TestSession s JOIN FETCH s.template " +
                   "WHERE s.template.id = :templateId " +
                   "AND s.status = :status " +
                   "ORDER BY s.completedAt DESC",
           countQuery = "SELECT COUNT(s) FROM TestSession s " +
                        "WHERE s.template.id = :templateId AND s.status = :status")
    Page<TestSession> findActivityByTemplateIdAndStatus(
            @Param("templateId") UUID templateId,
            @Param("status") SessionStatus status,
            Pageable pageable);

    /**
     * Aggregate activity stats for a template.
     * Returns a type-safe projection with totalSessions, completedCount, abandonedCount,
     * timedOutCount, and lastActivity.
     * Prevents ClassCastException from raw Object[].
     *
     * @param templateId the template UUID
     * @return TemplateActivityStatsProjection with aggregate statistics
     */
    @Query("""
        SELECT COUNT(s) AS totalSessions,
               COUNT(CASE WHEN s.status = 'COMPLETED' THEN 1 END) AS completedCount,
               COUNT(CASE WHEN s.status = 'ABANDONED' THEN 1 END) AS abandonedCount,
               COUNT(CASE WHEN s.status = 'TIMED_OUT' THEN 1 END) AS timedOutCount,
               MAX(s.completedAt) AS lastActivity
        FROM TestSession s
        WHERE s.template.id = :templateId
        AND s.status IN ('COMPLETED', 'ABANDONED', 'TIMED_OUT')
        """)
    TemplateActivityStatsProjection getTemplateActivityStats(@Param("templateId") UUID templateId);

    /**
     * Count passed sessions for a template (join with TestResult).
     */
    @Query("""
        SELECT COUNT(r)
        FROM TestResult r
        WHERE r.session.template.id = :templateId
        AND r.passed = true
        """)
    long countPassedSessionsByTemplateId(@Param("templateId") UUID templateId);

    /**
     * Get sum of scores and time for average calculation.
     * Returns a type-safe projection with totalScore, totalTimeSeconds, and resultCount.
     * Prevents ClassCastException from raw Object[].
     *
     * @param templateId the template UUID
     * @return TemplateScoreTimeProjection with aggregate statistics
     */
    @Query("""
        SELECT SUM(r.overallPercentage) AS totalScore,
               SUM(r.totalTimeSeconds) AS totalTimeSeconds,
               COUNT(r) AS resultCount
        FROM TestResult r
        WHERE r.session.template.id = :templateId
        """)
    TemplateScoreTimeProjection getTemplateScoreAndTimeAggregates(@Param("templateId") UUID templateId);
    // ============================================
    // ANONYMOUS SESSION QUERIES
    // ============================================

    Optional<TestSession> findBySessionAccessTokenHash(String tokenHash);

    @Query(value = "SELECT s FROM TestSession s JOIN FETCH s.template " +
                   "WHERE s.template.id = :templateId AND s.clerkUserId IS NULL ORDER BY s.createdAt DESC",
           countQuery = "SELECT COUNT(s) FROM TestSession s WHERE s.template.id = :templateId AND s.clerkUserId IS NULL")
    Page<TestSession> findAnonymousByTemplateId(@Param("templateId") UUID templateId, Pageable pageable);

    List<TestSession> findByShareLink_Id(UUID shareLinkId);

    @Query("SELECT COUNT(s) FROM TestSession s WHERE s.ipAddress = :ip AND s.createdAt > :after AND s.clerkUserId IS NULL")
    long countAnonymousByIpAddressAfter(@Param("ip") String ipAddress, @Param("after") LocalDateTime after);

    @Query("SELECT s FROM TestSession s JOIN FETCH s.template LEFT JOIN FETCH s.shareLink WHERE s.id = :sessionId AND s.clerkUserId IS NULL")
    Optional<TestSession> findAnonymousByIdWithShareLink(@Param("sessionId") UUID sessionId);

    @Query("SELECT s FROM TestSession s JOIN FETCH s.template LEFT JOIN FETCH s.shareLink WHERE s.id = :sessionId")
    Optional<TestSession> findByIdWithTemplateAndShareLink(@Param("sessionId") UUID sessionId);

    @Query("SELECT COUNT(s) FROM TestSession s WHERE s.template.id = :templateId AND s.clerkUserId IS NULL")
    long countAnonymousByTemplateId(@Param("templateId") UUID templateId);

    @Query("SELECT s FROM TestSession s WHERE s.clerkUserId IS NULL AND s.status IN ('NOT_STARTED', 'IN_PROGRESS') AND s.lastActivityAt < :cutoff")
    List<TestSession> findStaleAnonymousSessions(@Param("cutoff") LocalDateTime cutoffTime);

    @Query("SELECT COUNT(s) FROM TestSession s WHERE s.template.id = :templateId AND s.clerkUserId IS NULL AND s.status = 'IN_PROGRESS'")
    long countAnonymousInProgressByTemplateId(@Param("templateId") UUID templateId);

    @Query("SELECT COUNT(s) FROM TestSession s WHERE s.shareLink.id = :shareLinkId")
    long countByShareLinkId(@Param("shareLinkId") UUID shareLinkId);
}
