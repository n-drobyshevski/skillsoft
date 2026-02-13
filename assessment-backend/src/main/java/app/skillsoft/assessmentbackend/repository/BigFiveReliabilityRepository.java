package app.skillsoft.assessmentbackend.repository;

import app.skillsoft.assessmentbackend.domain.entities.BigFiveReliability;
import app.skillsoft.assessmentbackend.domain.entities.BigFiveTrait;
import app.skillsoft.assessmentbackend.domain.entities.ReliabilityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for BigFiveReliability entity.
 * Provides access to Cronbach's Alpha at the Big Five trait level.
 */
@Repository
public interface BigFiveReliabilityRepository extends JpaRepository<BigFiveReliability, UUID> {

    /**
     * Find reliability metrics for a specific Big Five trait.
     */
    Optional<BigFiveReliability> findByTrait(BigFiveTrait trait);

    /**
     * Find all traits with a specific reliability status.
     */
    List<BigFiveReliability> findByReliabilityStatus(ReliabilityStatus status);

    /**
     * Count traits by reliability status.
     */
    long countByReliabilityStatus(ReliabilityStatus status);

    /**
     * Check if reliability record exists for a trait.
     */
    boolean existsByTrait(BigFiveTrait trait);

    /**
     * Delete reliability record for a trait.
     */
    void deleteByTrait(BigFiveTrait trait);

    /**
     * Find all traits ordered by alpha for dashboard display.
     */
    @Query("SELECT bf FROM BigFiveReliability bf ORDER BY bf.cronbachAlpha DESC NULLS LAST")
    List<BigFiveReliability> findAllOrderedByAlphaDesc();

    /**
     * Find unreliable traits (alpha < 0.7).
     */
    @Query("SELECT bf FROM BigFiveReliability bf WHERE bf.reliabilityStatus = 'UNRELIABLE'")
    List<BigFiveReliability> findUnreliableTraits();

    /**
     * Get reliability status distribution for Big Five traits.
     */
    @Query("""
        SELECT bf.reliabilityStatus, COUNT(bf)
        FROM BigFiveReliability bf
        GROUP BY bf.reliabilityStatus
        """)
    List<Object[]> getReliabilityDistribution();

    /**
     * Calculate average alpha across all Big Five traits.
     */
    @Query("""
        SELECT AVG(bf.cronbachAlpha)
        FROM BigFiveReliability bf
        WHERE bf.cronbachAlpha IS NOT NULL
        """)
    Double calculateAverageAlpha();

    /**
     * Find trait with lowest alpha (for improvement prioritization).
     */
    @Query("""
        SELECT bf FROM BigFiveReliability bf
        WHERE bf.cronbachAlpha IS NOT NULL
        ORDER BY bf.cronbachAlpha ASC
        LIMIT 1
        """)
    Optional<BigFiveReliability> findLowestAlphaTrait();

    /**
     * Get total items across all traits.
     */
    @Query("SELECT COALESCE(SUM(bf.totalItems), 0) FROM BigFiveReliability bf")
    Integer getTotalItemsAcrossTraits();
}
