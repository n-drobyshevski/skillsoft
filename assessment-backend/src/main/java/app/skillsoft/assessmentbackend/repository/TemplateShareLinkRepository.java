package app.skillsoft.assessmentbackend.repository;

import app.skillsoft.assessmentbackend.domain.entities.TemplateShareLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for TemplateShareLink entities.
 * Provides queries for token validation and link management.
 */
@Repository
public interface TemplateShareLinkRepository extends JpaRepository<TemplateShareLink, UUID> {

    // ============================================
    // TOKEN LOOKUP
    // ============================================

    /**
     * Find a share link by its token.
     */
    Optional<TemplateShareLink> findByToken(String token);

    /**
     * Find a valid (active, not expired, not used up) share link by token.
     */
    @Query("SELECT l FROM TemplateShareLink l WHERE l.token = :token " +
           "AND l.isActive = true AND l.revokedAt IS NULL " +
           "AND l.expiresAt > CURRENT_TIMESTAMP " +
           "AND (l.maxUses IS NULL OR l.currentUses < l.maxUses)")
    Optional<TemplateShareLink> findValidByToken(@Param("token") String token);

    /**
     * Check if a token exists.
     */
    boolean existsByToken(String token);

    // ============================================
    // FIND BY TEMPLATE
    // ============================================

    /**
     * Find all links for a template (including expired/revoked).
     */
    List<TemplateShareLink> findByTemplateId(UUID templateId);

    /**
     * Find all active links for a template.
     */
    @Query("SELECT l FROM TemplateShareLink l WHERE l.template.id = :templateId " +
           "AND l.isActive = true AND l.revokedAt IS NULL " +
           "AND l.expiresAt > CURRENT_TIMESTAMP " +
           "AND (l.maxUses IS NULL OR l.currentUses < l.maxUses) " +
           "ORDER BY l.createdAt DESC")
    List<TemplateShareLink> findActiveByTemplateId(@Param("templateId") UUID templateId);

    /**
     * Find all links for a template (active and inactive) ordered by creation.
     */
    @Query("SELECT l FROM TemplateShareLink l WHERE l.template.id = :templateId " +
           "ORDER BY l.createdAt DESC")
    List<TemplateShareLink> findByTemplateIdOrderByCreatedAtDesc(
            @Param("templateId") UUID templateId);

    // ============================================
    // LINK COUNTS (for limit enforcement)
    // ============================================

    /**
     * Count active links for a template.
     * Used to enforce the maximum links per template limit.
     */
    @Query("SELECT COUNT(l) FROM TemplateShareLink l WHERE l.template.id = :templateId " +
           "AND l.isActive = true AND l.revokedAt IS NULL " +
           "AND l.expiresAt > CURRENT_TIMESTAMP " +
           "AND (l.maxUses IS NULL OR l.currentUses < l.maxUses)")
    long countActiveByTemplateId(@Param("templateId") UUID templateId);

    /**
     * Count total links ever created for a template.
     */
    long countByTemplateId(UUID templateId);

    // ============================================
    // LINK VALIDATION
    // ============================================

    /**
     * Check if a valid link exists for a template.
     */
    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END " +
           "FROM TemplateShareLink l WHERE l.template.id = :templateId " +
           "AND l.isActive = true AND l.revokedAt IS NULL " +
           "AND l.expiresAt > CURRENT_TIMESTAMP " +
           "AND (l.maxUses IS NULL OR l.currentUses < l.maxUses)")
    boolean hasActiveLinks(@Param("templateId") UUID templateId);

    // ============================================
    // USAGE TRACKING
    // ============================================

    /**
     * Increment usage count for a link.
     */
    @Modifying
    @Query("UPDATE TemplateShareLink l SET l.currentUses = l.currentUses + 1, " +
           "l.lastUsedAt = CURRENT_TIMESTAMP WHERE l.id = :linkId")
    int incrementUsage(@Param("linkId") UUID linkId);

    // ============================================
    // BULK OPERATIONS
    // ============================================

    /**
     * Revoke all active links for a template.
     * Used when archiving a template or changing visibility from LINK.
     */
    @Modifying
    @Query("UPDATE TemplateShareLink l SET l.isActive = false, l.revokedAt = CURRENT_TIMESTAMP " +
           "WHERE l.template.id = :templateId AND l.isActive = true")
    int revokeAllByTemplateId(@Param("templateId") UUID templateId);

    /**
     * Revoke all expired links (cleanup job).
     */
    @Modifying
    @Query("UPDATE TemplateShareLink l SET l.isActive = false, l.revokedAt = CURRENT_TIMESTAMP " +
           "WHERE l.isActive = true AND l.expiresAt < CURRENT_TIMESTAMP")
    int revokeExpired();

    // ============================================
    // STATISTICS
    // ============================================

    /**
     * Get total usage across all links for a template.
     */
    @Query("SELECT COALESCE(SUM(l.currentUses), 0) FROM TemplateShareLink l " +
           "WHERE l.template.id = :templateId")
    long getTotalUsageByTemplateId(@Param("templateId") UUID templateId);

    /**
     * Find links created by a specific user.
     */
    @Query("SELECT l FROM TemplateShareLink l WHERE l.createdBy.id = :userId " +
           "ORDER BY l.createdAt DESC")
    List<TemplateShareLink> findByCreatedById(@Param("userId") UUID userId);

    // ============================================
    // CLEANUP QUERIES
    // ============================================

    /**
     * Find expired links older than a certain date (for cleanup).
     */
    @Query("SELECT l FROM TemplateShareLink l WHERE l.expiresAt < :cutoffDate " +
           "AND l.isActive = false")
    List<TemplateShareLink> findExpiredLinksBefore(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Delete old expired links (cleanup job).
     */
    @Modifying
    @Query("DELETE FROM TemplateShareLink l WHERE l.expiresAt < :cutoffDate " +
           "AND l.isActive = false")
    int deleteExpiredLinksBefore(@Param("cutoffDate") LocalDateTime cutoffDate);

    // ============================================
    // BULK DELETE FOR TEMPLATE DELETION
    // ============================================

    /**
     * Delete all share links for a template.
     * Used during force delete of templates.
     *
     * @param templateId The template UUID
     * @return Count of deleted links
     */
    @Modifying
    @Query("DELETE FROM TemplateShareLink l WHERE l.template.id = :templateId")
    int deleteByTemplateId(@Param("templateId") UUID templateId);
}
