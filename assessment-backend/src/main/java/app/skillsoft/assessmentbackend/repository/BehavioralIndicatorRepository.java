package app.skillsoft.assessmentbackend.repository;


import app.skillsoft.assessmentbackend.domain.entities.BehavioralIndicator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BehavioralIndicatorRepository extends JpaRepository<BehavioralIndicator, UUID> {
        public List<BehavioralIndicator> findByCompetencyId(UUID competencyId);
}
