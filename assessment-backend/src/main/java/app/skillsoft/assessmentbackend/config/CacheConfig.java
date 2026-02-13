package app.skillsoft.assessmentbackend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for Caffeine in-memory caching layer.
 *
 * Provides caching for expensive external service calls:
 * - O*NET profiles: Static occupation data with 24-hour TTL
 * - Team profiles: Team saturation data with 15-minute TTL
 * - Passport scores: Candidate competency data with 1-hour TTL
 *
 * Cache statistics are recorded for monitoring via Spring Actuator.
 */
@EnableCaching
@Configuration
@Slf4j
public class CacheConfig {

    /**
     * Cache name constants for type-safe cache references.
     */
    public static final String ONET_PROFILES_CACHE = "onet-profiles";
    public static final String TEAM_PROFILES_CACHE = "team-profiles";
    public static final String PASSPORT_SCORES_CACHE = "passport-scores";
    public static final String COMPETENCIES_CACHE = "competencies";
    public static final String QUESTION_POOL_COUNTS_CACHE = "questionPoolCounts";
    public static final String TEMPLATE_METADATA_CACHE = "templateMetadata";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();

        // O*NET profiles - relatively static occupation data
        // 24-hour TTL, max 500 entries (SOC codes)
        manager.registerCustomCache(ONET_PROFILES_CACHE,
            Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofHours(24))
                .maximumSize(500)
                .recordStats()
                .build());

        // Team profiles - changes more frequently with team composition
        // 15-minute TTL, max 200 entries (active teams)
        manager.registerCustomCache(TEAM_PROFILES_CACHE,
            Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(15))
                .maximumSize(200)
                .recordStats()
                .build());

        // Passport scores - per-candidate competency data
        // 1-hour TTL, max 1000 entries (active candidates)
        manager.registerCustomCache(PASSPORT_SCORES_CACHE,
            Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofHours(1))
                .maximumSize(1000)
                .recordStats()
                .build());

        // Competencies - frequently listed during assembly/scoring
        // 10-minute TTL, max 200 entries
        manager.registerCustomCache(COMPETENCIES_CACHE,
            Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(10))
                .maximumSize(200)
                .recordStats()
                .build());

        // Question pool counts - availability checks during test assembly
        // 5-minute TTL, max 500 entries (per indicator/difficulty combos)
        manager.registerCustomCache(QUESTION_POOL_COUNTS_CACHE,
            Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(5))
                .maximumSize(500)
                .recordStats()
                .build());

        // Template metadata - config lookups during session creation
        // 5-minute TTL, max 200 entries
        manager.registerCustomCache(TEMPLATE_METADATA_CACHE,
            Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(5))
                .maximumSize(200)
                .recordStats()
                .build());

        log.info("Initialized Caffeine caches: {}, {}, {}, {}, {}, {}",
            ONET_PROFILES_CACHE, TEAM_PROFILES_CACHE, PASSPORT_SCORES_CACHE,
            COMPETENCIES_CACHE, QUESTION_POOL_COUNTS_CACHE, TEMPLATE_METADATA_CACHE);

        return manager;
    }
}
