package app.skillsoft.assessmentbackend.repository;

import app.skillsoft.assessmentbackend.domain.entities.Competency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CompetencyRepository extends JpaRepository<Competency, UUID> {
}
