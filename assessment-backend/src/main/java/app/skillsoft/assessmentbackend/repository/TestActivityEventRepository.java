package app.skillsoft.assessmentbackend.repository;

import app.skillsoft.assessmentbackend.domain.entities.ActivityEventType;
import app.skillsoft.assessmentbackend.domain.entities.TestActivityEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for TestActivityEvent entity.
 * Provides methods for activity event persistence and querying.
 */
@Repository
public interface TestActivityEventRepository extends JpaRepository<TestActivityEvent, UUID> {

    /**
     * Find recent activity events ordered by timestamp.
     */
    @Query("SELECT e FROM TestActivityEvent e ORDER BY e.eventTimestamp DESC LIMIT :limit")
    List<TestActivityEvent> findRecentEvents(@Param("limit") int limit);

    /**
     * Find activity events for a specific template.
     */
    Page<TestActivityEvent> findByTemplateIdOrderByEventTimestampDesc(UUID templateId, Pageable pageable);

    /**
     * Find activity events for a specific template and event type.
     */
    Page<TestActivityEvent> findByTemplateIdAndEventTypeOrderByEventTimestampDesc(
            UUID templateId, ActivityEventType eventType, Pageable pageable);

    /**
     * Find activity events for a specific user.
     */
    Page<TestActivityEvent> findByClerkUserIdOrderByEventTimestampDesc(String clerkUserId, Pageable pageable);

    /**
     * Find activity events for a specific session.
     */
    List<TestActivityEvent> findBySessionIdOrderByEventTimestampAsc(UUID sessionId);

    /**
     * Find events within a date range.
     */
    List<TestActivityEvent> findByEventTimestampBetweenOrderByEventTimestampDesc(
            LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find events for a template within a date range.
     */
    Page<TestActivityEvent> findByTemplateIdAndEventTimestampBetweenOrderByEventTimestampDesc(
            UUID templateId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Count events by template.
     */
    long countByTemplateId(UUID templateId);

    /**
     * Count events by template and event type.
     */
    long countByTemplateIdAndEventType(UUID templateId, ActivityEventType eventType);

    /**
     * Check if event already exists for a session and event type.
     * Prevents duplicate event recording.
     */
    boolean existsBySessionIdAndEventType(UUID sessionId, ActivityEventType eventType);

    /**
     * Aggregate event counts by type for a template.
     * Returns [eventType, count] pairs.
     */
    @Query("""
        SELECT e.eventType, COUNT(e)
        FROM TestActivityEvent e
        WHERE e.templateId = :templateId
        GROUP BY e.eventType
        """)
    List<Object[]> countEventsByTypeForTemplate(@Param("templateId") UUID templateId);

    /**
     * Find the most recent event for a template.
     */
    @Query("SELECT e FROM TestActivityEvent e WHERE e.templateId = :templateId ORDER BY e.eventTimestamp DESC LIMIT 1")
    TestActivityEvent findLatestByTemplateId(@Param("templateId") UUID templateId);
}
