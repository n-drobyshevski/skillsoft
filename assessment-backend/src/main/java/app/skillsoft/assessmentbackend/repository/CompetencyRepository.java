package app.skillsoft.assessmentbackend.repository;

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
}
