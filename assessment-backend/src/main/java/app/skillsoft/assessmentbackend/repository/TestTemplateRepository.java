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

    /**
     * Find all active test templates
     */
    List<TestTemplate> findByIsActiveTrue();

    /**
     * Find all active test templates with pagination
     */
    Page<TestTemplate> findByIsActiveTrue(Pageable pageable);

    /**
     * Find templates by name (case-insensitive partial match)
     */
    List<TestTemplate> findByNameContainingIgnoreCase(String name);

    /**
     * Find active templates by name (case-insensitive partial match)
     */
    List<TestTemplate> findByNameContainingIgnoreCaseAndIsActiveTrue(String name);

    /**
     * Check if a template name already exists (case-insensitive)
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Find templates that include a specific competency
     * Uses JSONB containment operator
     */
    @Query(value = "SELECT t.* FROM test_templates t WHERE t.competency_ids @> :competencyId::jsonb AND t.is_active = true", 
           nativeQuery = true)
    List<TestTemplate> findActiveTemplatesContainingCompetency(@Param("competencyId") String competencyIdJson);

    /**
     * Count active templates
     */
    long countByIsActiveTrue();
}
