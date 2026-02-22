package app.skillsoft.assessmentbackend.repository;

import app.skillsoft.assessmentbackend.domain.entities.ApprovalStatus;
import app.skillsoft.assessmentbackend.domain.entities.Competency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface CompetencyRepository extends JpaRepository<Competency, UUID> {

    /**
     * Find competencies whose name (case-insensitive) is in the given collection.
     * Used by JobFitAssembler to scope queries to benchmark-relevant competencies only.
     */
    @Query("SELECT c FROM Competency c WHERE LOWER(c.name) IN :names")
    List<Competency> findByNameInIgnoreCase(@Param("names") Collection<String> names);

    /**
     * Find competencies mapped to a specific Big Five trait via standardCodes JSONB.
     * Replaces the previous findAll() + filter pattern to avoid full-table scans.
     * Uses native JSONB operator to query directly in PostgreSQL.
     *
     * @param trait The Big Five trait name (e.g., "OPENNESS", "CONSCIENTIOUSNESS")
     * @return List of competencies mapped to the specified trait
     */
    @Query(value = "SELECT c.* FROM competencies c WHERE c.standard_codes->'bigFiveRef'->>'trait' = :trait",
            nativeQuery = true)
    List<Competency> findByBigFiveTrait(@Param("trait") String trait);

    long countByIsActiveTrue();

    long countByApprovalStatus(ApprovalStatus status);

    @Query("SELECT c.category, COUNT(c) FROM Competency c GROUP BY c.category")
    List<Object[]> countByCategory();

    @Query("SELECT COUNT(DISTINCT c) FROM Competency c JOIN c.behavioralIndicators bi")
    long countWithIndicators();

    @Query("SELECT COALESCE(AVG(bi.weight), 0) FROM BehavioralIndicator bi")
    double averageIndicatorWeight();
}
