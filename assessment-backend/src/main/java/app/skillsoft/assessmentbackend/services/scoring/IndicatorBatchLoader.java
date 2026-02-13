package app.skillsoft.assessmentbackend.services.scoring;

import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.repository.BehavioralIndicatorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Batch loader for behavioral indicators to prevent N+1 query problem in scoring strategies.
 *
 * Problem: When calculating indicator-level scores, individual repository calls inside loops
 * cause performance degradation. For tests with 20 questions across 10 indicators, this
 * would cause 10+ extra DB queries per scoring operation.
 *
 * Solution: Load all referenced indicators in a single query upfront,
 * then use an in-memory map for lookups during scoring calculations.
 *
 * Similar to CompetencyBatchLoader, but focuses on the indicator level of aggregation.
 */
@Component
public class IndicatorBatchLoader {

    private static final Logger log = LoggerFactory.getLogger(IndicatorBatchLoader.class);

    private final BehavioralIndicatorRepository indicatorRepository;

    public IndicatorBatchLoader(BehavioralIndicatorRepository indicatorRepository) {
        this.indicatorRepository = indicatorRepository;
    }

    /**
     * Batch load all indicators referenced by answers in a single query.
     * Prevents N+1 problem when scoring tests with many indicators.
     *
     * Uses JOIN FETCH to eagerly load competency relationships.
     *
     * @param answers List of test answers to extract indicator IDs from
     * @return Map of indicator ID to BehavioralIndicator entity for O(1) lookups
     */
    public Map<UUID, BehavioralIndicator> loadIndicatorsForAnswers(List<TestAnswer> answers) {
        if (answers == null || answers.isEmpty()) {
            log.debug("No answers provided for indicator batch loading");
            return Map.of();
        }

        Set<UUID> indicatorIds = answers.stream()
                .filter(a -> a != null && !Boolean.TRUE.equals(a.getIsSkipped()))
                .map(this::extractIndicatorIdSafe)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());

        if (indicatorIds.isEmpty()) {
            log.debug("No indicator IDs found in answers");
            return Map.of();
        }

        log.debug("Batch loading {} indicators for {} answers", indicatorIds.size(), answers.size());

        try {
            List<BehavioralIndicator> indicators = indicatorRepository.findAllByIdWithCompetency(indicatorIds);

            Map<UUID, BehavioralIndicator> indicatorMap = indicators.stream()
                    .collect(Collectors.toMap(BehavioralIndicator::getId, i -> i));

            // Log if any indicators were not found (data integrity issue)
            if (indicatorMap.size() < indicatorIds.size()) {
                Set<UUID> missingIds = new HashSet<>(indicatorIds);
                missingIds.removeAll(indicatorMap.keySet());
                log.warn("Missing indicators during batch load: {}", missingIds);
            }

            log.debug("Successfully loaded {} indicators", indicatorMap.size());
            return indicatorMap;

        } catch (Exception e) {
            log.error("Failed to batch load indicators: {}", e.getMessage(), e);
            // Return empty map - scoring strategies should handle graceful degradation
            return Map.of();
        }
    }

    /**
     * Safely extract indicator ID from a test answer with full null checks.
     * Navigates the entity chain: TestAnswer -> AssessmentQuestion -> BehavioralIndicator
     *
     * @param answer The test answer to extract indicator ID from
     * @return Optional containing indicator ID, or empty if any part of chain is null
     */
    public Optional<UUID> extractIndicatorIdSafe(TestAnswer answer) {
        Optional<UUID> result = Optional.ofNullable(answer)
                .map(TestAnswer::getQuestion)
                .map(AssessmentQuestion::getBehavioralIndicator)
                .map(BehavioralIndicator::getId);

        if (result.isEmpty() && answer != null) {
            log.warn("Failed to extract indicator ID from answer {}. " +
                    "Check question/indicator chain integrity.", answer.getId());
        }

        return result;
    }

    /**
     * Get an indicator from the preloaded cache with fallback logging.
     *
     * @param indicatorCache The preloaded indicator map
     * @param indicatorId The indicator ID to look up
     * @return The indicator or null if not found
     */
    public BehavioralIndicator getFromCache(Map<UUID, BehavioralIndicator> indicatorCache, UUID indicatorId) {
        if (indicatorCache == null || indicatorId == null) {
            return null;
        }

        BehavioralIndicator indicator = indicatorCache.get(indicatorId);
        if (indicator == null) {
            log.warn("Indicator {} not found in preloaded cache. " +
                    "This may indicate a data integrity issue or concurrent modification.", indicatorId);
        }

        return indicator;
    }
}
