package app.skillsoft.assessmentbackend.repository;


import app.skillsoft.assessmentbackend.domain.entities.BehavioralIndicator;
import app.skillsoft.assessmentbackend.domain.entities.ContextScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface BehavioralIndicatorRepository extends JpaRepository<BehavioralIndicator, UUID> {

        /**
         * Batch load indicators by IDs with their competency eagerly fetched.
         * Prevents N+1 query problem when calculating indicator-level scores.
         *
         * @param ids Set of indicator IDs to fetch
         * @return List of indicators with competency pre-loaded
         */
        @Query("SELECT bi FROM BehavioralIndicator bi LEFT JOIN FETCH bi.competency WHERE bi.id IN :ids")
        List<BehavioralIndicator> findAllByIdWithCompetency(@Param("ids") Set<UUID> ids);
        public List<BehavioralIndicator> findByCompetencyId(UUID competencyId);

        /**
         * Batch load indicators by competency IDs with their competency eagerly fetched.
         * Used by JobFitAssembler to prevent N+1 queries during gap-based question selection.
         *
         * @param competencyIds Set of competency IDs to fetch indicators for
         * @return List of indicators with competency pre-loaded
         */
        @Query("SELECT bi FROM BehavioralIndicator bi LEFT JOIN FETCH bi.competency WHERE bi.competency.id IN :competencyIds")
        List<BehavioralIndicator> findByCompetencyIdIn(@Param("competencyIds") Set<UUID> competencyIds);

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