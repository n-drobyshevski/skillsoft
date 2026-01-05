package app.skillsoft.assessmentbackend.repository;

import app.skillsoft.assessmentbackend.domain.entities.GranteeType;
import app.skillsoft.assessmentbackend.domain.entities.SharePermission;
import app.skillsoft.assessmentbackend.domain.entities.TemplateShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for TemplateShare entities.
 * Provides queries for checking and managing template access grants.
 */
@Repository
public interface TemplateShareRepository extends JpaRepository<TemplateShare, UUID> {

    // ============================================
    // FIND BY TEMPLATE
    // ============================================

    /**
     * Find all shares for a template (including expired/revoked).
     */
    List<TemplateShare> findByTemplateId(UUID templateId);

    /**
     * Find all active shares for a template.
     */
    @Query("SELECT s FROM TemplateShare s WHERE s.template.id = :templateId " +
           "AND s.isActive = true AND s.revokedAt IS NULL " +
           "AND (s.expiresAt IS NULL OR s.expiresAt > CURRENT_TIMESTAMP)")
    List<TemplateShare> findActiveByTemplateId(@Param("templateId") UUID templateId);

    /**
     * Find all shares for a template by grantee type.
     */
    @Query("SELECT s FROM TemplateShare s WHERE s.template.id = :templateId " +
           "AND s.granteeType = :granteeType " +
           "AND s.isActive = true AND s.revokedAt IS NULL " +
           "AND (s.expiresAt IS NULL OR s.expiresAt > CURRENT_TIMESTAMP)")
    List<TemplateShare> findActiveByTemplateIdAndGranteeType(
            @Param("templateId") UUID templateId,
            @Param("granteeType") GranteeType granteeType);

    // ============================================
    // FIND BY USER
    // ============================================

    /**
     * Find active share for a specific template and user.
     */
    @Query("SELECT s FROM TemplateShare s WHERE s.template.id = :templateId " +
           "AND s.user.id = :userId AND s.granteeType = 'USER' " +
           "AND s.isActive = true AND s.revokedAt IS NULL " +
           "AND (s.expiresAt IS NULL OR s.expiresAt > CURRENT_TIMESTAMP)")
    Optional<TemplateShare> findActiveByTemplateAndUser(
            @Param("templateId") UUID templateId,
            @Param("userId") UUID userId);

    /**
     * Find all templates shared with a user directly.
     */
    @Query("SELECT s FROM TemplateShare s WHERE s.user.id = :userId " +
           "AND s.granteeType = 'USER' AND s.isActive = true " +
           "AND s.revokedAt IS NULL " +
           "AND (s.expiresAt IS NULL OR s.expiresAt > CURRENT_TIMESTAMP)")
    List<TemplateShare> findActiveByUserId(@Param("userId") UUID userId);

    // ============================================
    // FIND BY TEAM
    // ============================================

    /**
     * Find active share for a specific template and team.
     */
    @Query("SELECT s FROM TemplateShare s WHERE s.template.id = :templateId " +
           "AND s.team.id = :teamId AND s.granteeType = 'TEAM' " +
           "AND s.isActive = true AND s.revokedAt IS NULL " +
           "AND (s.expiresAt IS NULL OR s.expiresAt > CURRENT_TIMESTAMP)")
    Optional<TemplateShare> findActiveByTemplateAndTeam(
            @Param("templateId") UUID templateId,
            @Param("teamId") UUID teamId);

    /**
     * Find the highest permission level from team shares for a template.
     * User must be an active member of one of the teams.
     */
    @Query("SELECT MAX(s.permission) FROM TemplateShare s " +
           "WHERE s.template.id = :templateId AND s.granteeType = 'TEAM' " +
           "AND s.team.id IN :teamIds " +
           "AND s.isActive = true AND s.revokedAt IS NULL " +
           "AND (s.expiresAt IS NULL OR s.expiresAt > CURRENT_TIMESTAMP)")
    Optional<SharePermission> findHighestPermissionByTemplateAndTeams(
            @Param("templateId") UUID templateId,
            @Param("teamIds") List<UUID> teamIds);

    /**
     * Find all active shares for templates where user is a team member.
     */
    @Query("SELECT s FROM TemplateShare s WHERE s.granteeType = 'TEAM' " +
           "AND s.team.id IN :teamIds " +
           "AND s.isActive = true AND s.revokedAt IS NULL " +
           "AND (s.expiresAt IS NULL OR s.expiresAt > CURRENT_TIMESTAMP)")
    List<TemplateShare> findActiveByTeamIds(@Param("teamIds") List<UUID> teamIds);

    // ============================================
    // PERMISSION CHECKS
    // ============================================

    /**
     * Check if a user has a specific permission for a template (directly).
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
           "FROM TemplateShare s WHERE s.template.id = :templateId " +
           "AND s.user.id = :userId AND s.granteeType = 'USER' " +
           "AND s.permission >= :minPermission " +
           "AND s.isActive = true AND s.revokedAt IS NULL " +
           "AND (s.expiresAt IS NULL OR s.expiresAt > CURRENT_TIMESTAMP)")
    boolean hasDirectPermission(
            @Param("templateId") UUID templateId,
            @Param("userId") UUID userId,
            @Param("minPermission") SharePermission minPermission);

    /**
     * Check if any of the user's teams have a specific permission for a template.
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
           "FROM TemplateShare s WHERE s.template.id = :templateId " +
           "AND s.team.id IN :teamIds AND s.granteeType = 'TEAM' " +
           "AND s.permission >= :minPermission " +
           "AND s.isActive = true AND s.revokedAt IS NULL " +
           "AND (s.expiresAt IS NULL OR s.expiresAt > CURRENT_TIMESTAMP)")
    boolean hasTeamPermission(
            @Param("templateId") UUID templateId,
            @Param("teamIds") List<UUID> teamIds,
            @Param("minPermission") SharePermission minPermission);

    // ============================================
    // STATISTICS & COUNTS
    // ============================================

    /**
     * Count active shares for a template.
     */
    @Query("SELECT COUNT(s) FROM TemplateShare s WHERE s.template.id = :templateId " +
           "AND s.isActive = true AND s.revokedAt IS NULL " +
           "AND (s.expiresAt IS NULL OR s.expiresAt > CURRENT_TIMESTAMP)")
    long countActiveByTemplateId(@Param("templateId") UUID templateId);

    /**
     * Count active user shares for a template.
     */
    @Query("SELECT COUNT(s) FROM TemplateShare s WHERE s.template.id = :templateId " +
           "AND s.granteeType = 'USER' " +
           "AND s.isActive = true AND s.revokedAt IS NULL " +
           "AND (s.expiresAt IS NULL OR s.expiresAt > CURRENT_TIMESTAMP)")
    long countActiveUserSharesByTemplateId(@Param("templateId") UUID templateId);

    /**
     * Count active team shares for a template.
     */
    @Query("SELECT COUNT(s) FROM TemplateShare s WHERE s.template.id = :templateId " +
           "AND s.granteeType = 'TEAM' " +
           "AND s.isActive = true AND s.revokedAt IS NULL " +
           "AND (s.expiresAt IS NULL OR s.expiresAt > CURRENT_TIMESTAMP)")
    long countActiveTeamSharesByTemplateId(@Param("templateId") UUID templateId);

    // ============================================
    // EXISTENCE CHECKS
    // ============================================

    /**
     * Check if a user share already exists (even if revoked).
     */
    boolean existsByTemplateIdAndUserIdAndGranteeType(
            UUID templateId, UUID userId, GranteeType granteeType);

    /**
     * Check if a team share already exists (even if revoked).
     */
    boolean existsByTemplateIdAndTeamIdAndGranteeType(
            UUID templateId, UUID teamId, GranteeType granteeType);

    // ============================================
    // SHARED WITH ME QUERIES
    // ============================================

    /**
     * Find all active direct shares for a user with eager loading.
     * Excludes templates owned by the user (user should see "shared with me", not their own).
     * Uses JOIN FETCH to avoid N+1 queries.
     */
    @Query("SELECT s FROM TemplateShare s " +
           "JOIN FETCH s.template t " +
           "LEFT JOIN FETCH t.owner " +
           "LEFT JOIN FETCH s.grantedBy " +
           "WHERE s.user.id = :userId AND s.granteeType = 'USER' " +
           "AND s.isActive = true AND s.revokedAt IS NULL " +
           "AND (s.expiresAt IS NULL OR s.expiresAt > CURRENT_TIMESTAMP) " +
           "AND (t.owner IS NULL OR t.owner.id <> :userId) " +
           "ORDER BY s.grantedAt DESC")
    List<TemplateShare> findActiveSharesForUserWithDetails(@Param("userId") UUID userId);

    /**
     * Find all active team shares for a user with eager loading.
     * Excludes templates owned by the user.
     * Uses JOIN FETCH to avoid N+1 queries.
     */
    @Query("SELECT s FROM TemplateShare s " +
           "JOIN FETCH s.template t " +
           "LEFT JOIN FETCH t.owner " +
           "LEFT JOIN FETCH s.grantedBy " +
           "WHERE s.granteeType = 'TEAM' AND s.team.id IN :teamIds " +
           "AND s.isActive = true AND s.revokedAt IS NULL " +
           "AND (s.expiresAt IS NULL OR s.expiresAt > CURRENT_TIMESTAMP) " +
           "AND (t.owner IS NULL OR t.owner.id <> :userId) " +
           "ORDER BY s.grantedAt DESC")
    List<TemplateShare> findActiveTeamSharesForUserWithDetails(
            @Param("teamIds") List<UUID> teamIds,
            @Param("userId") UUID userId);

    /**
     * Count unique templates shared with a user (direct + team shares).
     * Excludes templates owned by the user.
     * Used for navigation badge display.
     */
    @Query("SELECT COUNT(DISTINCT t.id) FROM TemplateShare s " +
           "JOIN s.template t " +
           "WHERE ((s.user.id = :userId AND s.granteeType = 'USER') " +
           "       OR (s.team.id IN :teamIds AND s.granteeType = 'TEAM')) " +
           "AND s.isActive = true AND s.revokedAt IS NULL " +
           "AND (s.expiresAt IS NULL OR s.expiresAt > CURRENT_TIMESTAMP) " +
           "AND (t.owner IS NULL OR t.owner.id <> :userId)")
    long countTemplatesSharedWithUser(
            @Param("userId") UUID userId,
            @Param("teamIds") List<UUID> teamIds);
}
