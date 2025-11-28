package app.skillsoft.assessmentbackend.repository;

import app.skillsoft.assessmentbackend.domain.entities.SessionStatus;
import app.skillsoft.assessmentbackend.domain.entities.TestSession;
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
}
