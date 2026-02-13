package app.skillsoft.assessmentbackend.repository;

import app.skillsoft.assessmentbackend.domain.entities.TestTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TestTemplateRepository extends JpaRepository<TestTemplate, UUID> {

    // ==================== NON-DELETED TEMPLATE QUERIES ====================

    /**
     * Find all non-deleted templates with pagination.
     * Used for admin listing - shows all non-deleted templates regardless of active status.
     */
    Page<TestTemplate> findByDeletedAtIsNull(Pageable pageable);

    /**
     * Find all active, non-deleted test templates.
     * Used for test-takers who can only see published templates.
     */
    List<TestTemplate> findByIsActiveTrueAndDeletedAtIsNull();

    /**
     * Find active, non-deleted templates with pagination.
     */
    Page<TestTemplate> findByIsActiveTrueAndDeletedAtIsNull(Pageable pageable);

    /**
     * Find non-deleted templates by name (case-insensitive partial match).
     */
    List<TestTemplate> findByNameContainingIgnoreCaseAndDeletedAtIsNull(String name);

    /**
     * Find active, non-deleted templates by name (case-insensitive partial match).
     */
    List<TestTemplate> findByNameContainingIgnoreCaseAndIsActiveTrueAndDeletedAtIsNull(String name);

    /**
     * Check if a template name already exists (case-insensitive) among non-deleted templates.
     */
    boolean existsByNameIgnoreCaseAndDeletedAtIsNull(String name);

    /**
     * Find non-deleted templates that include a specific competency.
     * Uses JSONB containment operator.
     */
    @Query(value = "SELECT t.* FROM test_templates t WHERE t.competency_ids @> :competencyId::jsonb " +
           "AND t.is_active = true AND t.deleted_at IS NULL", nativeQuery = true)
    List<TestTemplate> findActiveTemplatesContainingCompetency(@Param("competencyId") String competencyIdJson);

    /**
     * Count active, non-deleted templates.
     */
    long countByIsActiveTrueAndDeletedAtIsNull();

    /**
     * Count all non-deleted templates.
     */
    long countByDeletedAtIsNull();

    /**
     * Count inactive, non-deleted templates.
     */
    long countByIsActiveFalseAndDeletedAtIsNull();

    // ==================== SOFT-DELETED TEMPLATE QUERIES ====================

    /**
     * Find all soft-deleted templates (for admin restore functionality).
     */
    List<TestTemplate> findByDeletedAtIsNotNullOrderByDeletedAtDesc();

    // ==================== LEGACY QUERIES (for backwards compatibility) ====================

    /**
     * @deprecated Use findByIsActiveTrueAndDeletedAtIsNull instead
     */
    @Deprecated
    List<TestTemplate> findByIsActiveTrue();

    /**
     * @deprecated Use findByIsActiveTrueAndDeletedAtIsNull(Pageable) instead
     */
    @Deprecated
    Page<TestTemplate> findByIsActiveTrue(Pageable pageable);

    /**
     * @deprecated Use findByNameContainingIgnoreCaseAndDeletedAtIsNull instead
     */
    @Deprecated
    List<TestTemplate> findByNameContainingIgnoreCase(String name);

    /**
     * @deprecated Use findByNameContainingIgnoreCaseAndIsActiveTrueAndDeletedAtIsNull instead
     */
    @Deprecated
    List<TestTemplate> findByNameContainingIgnoreCaseAndIsActiveTrue(String name);

    /**
     * @deprecated Use existsByNameIgnoreCaseAndDeletedAtIsNull instead
     */
    @Deprecated
    boolean existsByNameIgnoreCase(String name);

    /**
     * @deprecated Use countByIsActiveTrueAndDeletedAtIsNull instead
     */
    @Deprecated
    long countByIsActiveTrue();
}
