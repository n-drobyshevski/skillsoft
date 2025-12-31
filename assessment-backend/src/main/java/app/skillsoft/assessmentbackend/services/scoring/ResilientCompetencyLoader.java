package app.skillsoft.assessmentbackend.services.scoring;

import app.skillsoft.assessmentbackend.domain.entities.Competency;
import app.skillsoft.assessmentbackend.repository.CompetencyRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resilient wrapper for competency repository calls with circuit breaker protection.
 *
 * Problem: Repository calls have no circuit breaker protection. A slow database will hang
 * all scoring operations, causing cascading failures across the system.
 *
 * Solution: Wrap critical repository operations with circuit breaker pattern and local cache
 * fallback. When the database is slow or unavailable, the circuit breaker opens and serves
 * cached data instead of blocking threads.
 *
 * Per ROADMAP.md Task 3.2 - Add circuit breaker to repository calls
 *
 * Resilience patterns applied:
 * - Circuit Breaker: Opens after 50% failure rate over 5 calls, stays open 10s
 * - Local Cache Fallback: ConcurrentHashMap stores successfully loaded competencies
 * - Cache Warming: Pre-populate cache on startup for frequently used competencies
 */
@Service
@Slf4j
public class ResilientCompetencyLoader {

    private final CompetencyRepository competencyRepository;

    /**
     * Thread-safe local cache for circuit breaker fallback.
     * Populated during successful database loads, used when circuit is open.
     */
    private final ConcurrentHashMap<UUID, Competency> localCache = new ConcurrentHashMap<>();

    public ResilientCompetencyLoader(CompetencyRepository competencyRepository) {
        this.competencyRepository = competencyRepository;
    }

    /**
     * Load competencies from database with circuit breaker protection.
     *
     * On success: Returns competencies from database and populates local cache.
     * On failure: Circuit breaker triggers fallback to serve from local cache.
     *
     * @param ids Set of competency UUIDs to load
     * @return Map of competency ID to Competency entity for O(1) lookups
     */
    @CircuitBreaker(name = "competencyLoader", fallbackMethod = "loadFromCache")
    public Map<UUID, Competency> loadCompetencies(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            log.debug("No competency IDs provided for loading");
            return Map.of();
        }

        Map<UUID, Competency> result = competencyRepository.findAllById(ids)
            .stream()
            .collect(Collectors.toMap(Competency::getId, Function.identity()));

        // Populate cache for fallback - thread-safe put operations
        result.forEach(localCache::put);

        log.debug("Loaded {} competencies from database, cache size: {}",
            result.size(), localCache.size());

        return result;
    }

    /**
     * Fallback method for loadCompetencies when circuit breaker is open or database call fails.
     * Serves competencies from local cache to maintain partial functionality.
     *
     * @param ids Set of competency UUIDs requested
     * @param e Exception that triggered the fallback
     * @return Map of cached competencies (may be incomplete if cache miss occurs)
     */
    private Map<UUID, Competency> loadFromCache(Set<UUID> ids, Exception e) {
        log.warn("Loading competencies from cache due to circuit breaker: {}", e.getMessage());

        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }

        Map<UUID, Competency> cached = new HashMap<>();
        int missCount = 0;

        for (UUID id : ids) {
            Competency comp = localCache.get(id);
            if (comp != null) {
                cached.put(id, comp);
            } else {
                missCount++;
            }
        }

        if (missCount > 0) {
            log.error("Cache miss for {} competencies during circuit breaker fallback. " +
                     "Requested: {}, Found: {}", missCount, ids.size(), cached.size());
        } else {
            log.info("Successfully served {} competencies from cache during circuit breaker fallback",
                    cached.size());
        }

        return cached;
    }

    /**
     * Pre-warm cache with commonly used competencies.
     * Should be called on application startup or periodically to ensure
     * fallback data is available when circuit breaker opens.
     *
     * @param ids Set of competency UUIDs to pre-load into cache
     */
    public void warmCache(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            log.debug("No competency IDs provided for cache warming");
            return;
        }

        try {
            loadCompetencies(ids);
            log.info("Warmed competency cache with {} entries, total cache size: {}",
                    ids.size(), localCache.size());
        } catch (Exception e) {
            log.warn("Failed to warm competency cache: {}", e.getMessage());
        }
    }

    /**
     * Get current cache size for monitoring/diagnostics.
     *
     * @return Number of competencies currently in the local cache
     */
    public int getCacheSize() {
        return localCache.size();
    }

    /**
     * Clear the local cache. Useful for testing or forced cache refresh.
     * Use with caution in production as it removes fallback data.
     */
    public void clearCache() {
        int previousSize = localCache.size();
        localCache.clear();
        log.info("Cleared competency cache. Previous size: {}", previousSize);
    }

    /**
     * Check if a specific competency is in the local cache.
     * Useful for diagnostics and cache hit rate monitoring.
     *
     * @param id Competency UUID to check
     * @return true if competency is cached, false otherwise
     */
    public boolean isCached(UUID id) {
        return id != null && localCache.containsKey(id);
    }
}
