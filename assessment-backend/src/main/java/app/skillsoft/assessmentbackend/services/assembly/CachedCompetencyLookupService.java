package app.skillsoft.assessmentbackend.services.assembly;

import app.skillsoft.assessmentbackend.config.CacheConfig;
import app.skillsoft.assessmentbackend.domain.entities.BehavioralIndicator;
import app.skillsoft.assessmentbackend.domain.entities.Competency;
import app.skillsoft.assessmentbackend.repository.BehavioralIndicatorRepository;
import app.skillsoft.assessmentbackend.repository.CompetencyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Cached lookup service for competencies and indicators during assembly.
 *
 * O*NET profiles and passport data are queried fresh every assembly by default.
 * This service wraps common lookups with Caffeine caching to reduce
 * database round-trips for repeated assembly operations.
 *
 * Cache invalidation occurs automatically on competency CRUD operations
 * via @CacheEvict annotations, and via TTL (10 minutes for competencies).
 */
@Service
public class CachedCompetencyLookupService {

    private static final Logger log = LoggerFactory.getLogger(CachedCompetencyLookupService.class);

    private final CompetencyRepository competencyRepository;
    private final BehavioralIndicatorRepository indicatorRepository;

    public CachedCompetencyLookupService(
            CompetencyRepository competencyRepository,
            BehavioralIndicatorRepository indicatorRepository) {
        this.competencyRepository = competencyRepository;
        this.indicatorRepository = indicatorRepository;
    }

    /**
     * Get all competencies with caching.
     * Cached for 10 minutes (configured in CacheConfig).
     */
    @Cacheable(value = CacheConfig.COMPETENCIES_CACHE, key = "'all'")
    public List<Competency> findAllCompetencies() {
        log.debug("Cache miss: loading all competencies from database");
        return competencyRepository.findAll();
    }

    /**
     * Get indicators for a competency with caching.
     */
    @Cacheable(value = CacheConfig.COMPETENCIES_CACHE, key = "'indicators-' + #competencyId")
    public List<BehavioralIndicator> findIndicatorsByCompetencyId(UUID competencyId) {
        log.debug("Cache miss: loading indicators for competency {}", competencyId);
        return indicatorRepository.findByCompetencyId(competencyId);
    }

    /**
     * Evict all competency caches.
     * Should be called on competency create/update/delete operations.
     */
    @CacheEvict(value = CacheConfig.COMPETENCIES_CACHE, allEntries = true)
    public void evictCompetencyCache() {
        log.debug("Evicted all competency caches");
    }
}
