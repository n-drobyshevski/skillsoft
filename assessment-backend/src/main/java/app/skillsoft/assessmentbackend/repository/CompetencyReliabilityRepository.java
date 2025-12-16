package app.skillsoft.assessmentbackend.repository;

import app.skillsoft.assessmentbackend.domain.entities.CompetencyReliability;
import app.skillsoft.assessmentbackend.domain.entities.ReliabilityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CompetencyReliability entity.
 * Provides access to Cronbach's Alpha and related metrics for competencies.
 */
@Repository
public interface CompetencyReliabilityRepository extends JpaRepository<CompetencyReliability, UUID> {

    /**
     * Find reliability metrics for a specific competency.
     */
    Optional<CompetencyReliability> findByCompetency_Id(UUID competencyId);

    /**
     * Find all competencies with a specific reliability status.
     */
    List<CompetencyReliability> findByReliabilityStatus(ReliabilityStatus status);

    /**
     * Count competencies by reliability status.
     */
    long countByReliabilityStatus(ReliabilityStatus status);

    /**
     * Find unreliable competencies (alpha < 0.7).
     */
    @Query("SELECT cr FROM CompetencyReliability cr WHERE cr.reliabilityStatus = 'UNRELIABLE'")
    List<CompetencyReliability> findUnreliableCompetencies();

    /**
     * Find competencies that have problematic items (alpha would improve if item removed).
     */
    @Query("""
        SELECT cr FROM CompetencyReliability cr
        WHERE cr.alphaIfDeleted IS NOT NULL
        AND cr.cronbachAlpha IS NOT NULL
        """)
    List<CompetencyReliability> findCompetenciesWithAlphaAnalysis();

    /**
     * Get reliability status distribution.
     */
    @Query("""
        SELECT cr.reliabilityStatus, COUNT(cr)
        FROM CompetencyReliability cr
        GROUP BY cr.reliabilityStatus
        """)
    List<Object[]> getReliabilityDistribution();

    /**
     * Find competencies with insufficient data for analysis.
     */
    @Query("SELECT cr FROM CompetencyReliability cr WHERE cr.reliabilityStatus = 'INSUFFICIENT_DATA'")
    List<CompetencyReliability> findCompetenciesWithInsufficientData();

    /**
     * Find competencies ordered by alpha (ascending) for improvement prioritization.
     */
    @Query("""
        SELECT cr FROM CompetencyReliability cr
        WHERE cr.cronbachAlpha IS NOT NULL
        ORDER BY cr.cronbachAlpha ASC
        """)
    List<CompetencyReliability> findAllOrderedByAlphaAsc();

    /**
     * Check if reliability record exists for a competency.
     */
    boolean existsByCompetency_Id(UUID competencyId);

    /**
     * Delete reliability record for a competency.
     */
    void deleteByCompetency_Id(UUID competencyId);

    /**
     * Find competencies with high alpha (excellent reliability >= 0.9).
     */
    @Query("SELECT cr FROM CompetencyReliability cr WHERE cr.cronbachAlpha >= 0.9")
    List<CompetencyReliability> findExcellentReliability();

    /**
     * Calculate average alpha across all competencies with valid data.
     */
    @Query("""
        SELECT AVG(cr.cronbachAlpha)
        FROM CompetencyReliability cr
        WHERE cr.cronbachAlpha IS NOT NULL
        """)
    Double calculateAverageAlpha();
}
