package app.skillsoft.assessmentbackend.repository;

import app.skillsoft.assessmentbackend.domain.entities.ScoringAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for ScoringAuditLog entity.
 * Provides access to scoring audit trail data.
 */
@Repository
public interface ScoringAuditLogRepository extends JpaRepository<ScoringAuditLog, UUID> {

    /**
     * Find audit logs for a specific session.
     */
    List<ScoringAuditLog> findBySessionId(UUID sessionId);

    /**
     * Find audit log for a specific result.
     */
    List<ScoringAuditLog> findByResultId(UUID resultId);

    /**
     * Find audit logs for a user.
     */
    List<ScoringAuditLog> findByClerkUserIdOrderByCreatedAtDesc(String clerkUserId);

    /**
     * Find audit logs within a date range (for reporting/compliance).
     */
    List<ScoringAuditLog> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
