package app.skillsoft.assessmentbackend.services.scoring;

import app.skillsoft.assessmentbackend.domain.entities.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Batch loader for competencies to prevent N+1 query problem in scoring strategies.
 *
 * Problem: Each scoring strategy makes individual repository calls inside loops.
 * For tests with 20 competencies, this causes 20+ extra DB queries per scoring.
 *
 * Solution: Load all referenced competencies in a single query upfront,
 * then use an in-memory map for lookups during scoring calculations.
 *
 * Per ROADMAP.md Task 1.3 - N+1 Query Optimization
 * Per ROADMAP.md Task 3.2 - Now delegates to ResilientCompetencyLoader for circuit breaker protection
 */
@Component
public class CompetencyBatchLoader {

    private static final Logger log = LoggerFactory.getLogger(CompetencyBatchLoader.class);

    private final ResilientCompetencyLoader resilientCompetencyLoader;

    public CompetencyBatchLoader(ResilientCompetencyLoader resilientCompetencyLoader) {
        this.resilientCompetencyLoader = resilientCompetencyLoader;
    }

    /**
     * Batch load all competencies referenced by answers in a single query.
     * Prevents N+1 problem when scoring tests with many competencies.
     *
     * Delegates to ResilientCompetencyLoader for circuit breaker protection.
     * When database is slow/unavailable, falls back to cached competencies.
     *
     * @param answers List of test answers to extract competency IDs from
     * @return Map of competency ID to Competency entity for O(1) lookups
     */
    public Map<UUID, Competency> loadCompetenciesForAnswers(List<TestAnswer> answers) {
        if (answers == null || answers.isEmpty()) {
            log.debug("No answers provided for competency batch loading");
            return Map.of();
        }

        Set<UUID> competencyIds = answers.stream()
                .filter(a -> a != null && !Boolean.TRUE.equals(a.getIsSkipped()))
                .map(this::extractCompetencyIdSafe)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());

        if (competencyIds.isEmpty()) {
            log.debug("No competency IDs found in answers");
            return Map.of();
        }

        log.debug("Batch loading {} competencies for {} answers", competencyIds.size(), answers.size());

        // Delegate to resilient loader with circuit breaker protection
        Map<UUID, Competency> competencyMap = resilientCompetencyLoader.loadCompetencies(competencyIds);

        // Log if any competencies were not found (data integrity issue or cache miss during fallback)
        if (competencyMap.size() < competencyIds.size()) {
            Set<UUID> missingIds = new HashSet<>(competencyIds);
            missingIds.removeAll(competencyMap.keySet());
            log.warn("Missing competencies during batch load: {}", missingIds);
        }

        log.debug("Successfully loaded {} competencies", competencyMap.size());
        return competencyMap;
    }

    /**
     * Safely extract competency ID from a test answer with full null checks.
     * Navigates the entity chain: TestAnswer -> AssessmentQuestion -> BehavioralIndicator -> Competency
     *
     * @param answer The test answer to extract competency ID from
     * @return Optional containing competency ID, or empty if any part of chain is null
     */
    public Optional<UUID> extractCompetencyIdSafe(TestAnswer answer) {
        Optional<UUID> result = Optional.ofNullable(answer)
                .map(TestAnswer::getQuestion)
                .map(AssessmentQuestion::getBehavioralIndicator)
                .map(BehavioralIndicator::getCompetency)
                .map(Competency::getId);

        if (result.isEmpty() && answer != null) {
            log.warn("Failed to extract competency ID from answer {}. " +
                    "Check question/indicator/competency chain integrity.", answer.getId());
        }

        return result;
    }

    /**
     * Get a competency from the preloaded cache with fallback logging.
     *
     * @param competencyCache The preloaded competency map
     * @param competencyId The competency ID to look up
     * @return The competency or null if not found
     */
    public Competency getFromCache(Map<UUID, Competency> competencyCache, UUID competencyId) {
        if (competencyCache == null || competencyId == null) {
            return null;
        }

        Competency competency = competencyCache.get(competencyId);
        if (competency == null) {
            log.warn("Competency {} not found in preloaded cache. " +
                    "This may indicate a data integrity issue or concurrent modification.", competencyId);
        }

        return competency;
    }
}
