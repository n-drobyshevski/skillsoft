package app.skillsoft.assessmentbackend.repository;


import app.skillsoft.assessmentbackend.domain.entities.BehavioralIndicator;
import app.skillsoft.assessmentbackend.domain.entities.ContextScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BehavioralIndicatorRepository extends JpaRepository<BehavioralIndicator, UUID> {
        public List<BehavioralIndicator> findByCompetencyId(UUID competencyId);
        
        public Optional<BehavioralIndicator> findByIdAndCompetencyId(UUID id, UUID competencyId);
        
        /**
         * Find behavioral indicators by context scope.
         * Used for Smart Assessment filtering to select context-appropriate indicators.
         * 
         * Example: findByContextScope(ContextScope.UNIVERSAL) returns indicators
         * suitable for Scenario A (Universal Baseline / Competency Passport).
         * 
         * @param contextScope The scope to filter by (UNIVERSAL, PROFESSIONAL, TECHNICAL, MANAGERIAL)
         * @return List of indicators matching the specified scope
         */
        public List<BehavioralIndicator> findByContextScope(ContextScope contextScope);
}