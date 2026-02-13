package app.skillsoft.assessmentbackend.services.external.impl;

import app.skillsoft.assessmentbackend.config.CacheConfig;
import app.skillsoft.assessmentbackend.services.external.PassportService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock implementation of PassportService for development and testing.
 *
 * In production, this would integrate with a persistent storage system
 * for candidate competency passports.
 *
 * Resilience patterns applied:
 * - Circuit Breaker: Opens after 50% failure rate over 10 calls, stays open 30s
 * - Retry: Up to 3 attempts with 500ms wait for transient failures
 * - Caching: L1 cache with Caffeine for frequently accessed passports
 */
@Service
@Slf4j
public class PassportServiceImpl implements PassportService {

    // In-memory storage for mock passports (keyed by candidateId UUID)
    private final Map<UUID, CompetencyPassport> passportStore = new ConcurrentHashMap<>();

    // Mapping from Clerk User ID to internal candidate UUID
    // In production, this would be handled by a UserProfile service/repository
    private final Map<String, UUID> clerkUserIdToCandidateId = new ConcurrentHashMap<>();

    @Override
    @CircuitBreaker(name = "passportService", fallbackMethod = "getPassportFallback")
    @Retry(name = "externalServices")
    @Cacheable(
        value = CacheConfig.PASSPORT_SCORES_CACHE,
        key = "#candidateId",
        unless = "#result == null || !#result.isPresent()"
    )
    public Optional<CompetencyPassport> getPassport(UUID candidateId) {
        log.debug("Fetching passport for candidate: {} (cache miss)", candidateId);
        return Optional.ofNullable(passportStore.get(candidateId))
            .filter(CompetencyPassport::isValid);
    }

    /**
     * Fallback method for getPassport when circuit breaker is open or external call fails.
     * Returns empty Optional to signal unavailability.
     */
    private Optional<CompetencyPassport> getPassportFallback(UUID candidateId, Exception e) {
        log.warn("Passport service unavailable for candidate {}: {}", candidateId, e.getMessage());
        return Optional.empty();
    }

    @Override
    @CircuitBreaker(name = "passportService", fallbackMethod = "getCompetencyScoreFallback")
    @Retry(name = "externalServices")
    public Optional<Double> getCompetencyScore(UUID candidateId, UUID competencyId) {
        return getPassportInternal(candidateId)
            .map(passport -> passport.competencyScores().get(competencyId));
    }

    /**
     * Fallback method for getCompetencyScore when circuit breaker is open or call fails.
     * Returns empty Optional to signal unavailability.
     */
    private Optional<Double> getCompetencyScoreFallback(UUID candidateId, UUID competencyId, Exception e) {
        log.warn("Passport competency score unavailable for candidate {} / competency {}: {}",
                 candidateId, competencyId, e.getMessage());
        return Optional.empty();
    }

    @Override
    @CircuitBreaker(name = "passportService", fallbackMethod = "hasValidPassportFallback")
    @Retry(name = "externalServices")
    public boolean hasValidPassport(UUID candidateId) {
        return getPassportInternal(candidateId).isPresent();
    }

    /**
     * Fallback method for hasValidPassport when circuit breaker is open or call fails.
     * Returns false to signal unable to validate.
     */
    private boolean hasValidPassportFallback(UUID candidateId, Exception e) {
        log.warn("Passport validation unavailable for candidate {}: {}", candidateId, e.getMessage());
        return false;
    }

    @Override
    @CircuitBreaker(name = "passportService", fallbackMethod = "getPassportByClerkUserIdFallback")
    @Retry(name = "externalServices")
    @Cacheable(
        value = CacheConfig.PASSPORT_SCORES_CACHE,
        key = "'clerk:' + #clerkUserId",
        unless = "#result == null || !#result.isPresent()"
    )
    public Optional<CompetencyPassport> getPassportByClerkUserId(String clerkUserId) {
        if (clerkUserId == null || clerkUserId.isBlank()) {
            log.debug("Attempted to fetch passport with null/blank clerkUserId");
            return Optional.empty();
        }

        log.debug("Fetching passport for clerkUserId: {} (cache miss)", clerkUserId);

        // Look up the internal candidate ID from the Clerk User ID mapping
        UUID candidateId = clerkUserIdToCandidateId.get(clerkUserId);
        if (candidateId == null) {
            log.debug("No candidate ID mapping found for clerkUserId: {}", clerkUserId);
            return Optional.empty();
        }

        return getPassportInternal(candidateId);
    }

    /**
     * Fallback method for getPassportByClerkUserId when circuit breaker is open or call fails.
     * Returns empty Optional to signal unavailability.
     */
    private Optional<CompetencyPassport> getPassportByClerkUserIdFallback(String clerkUserId, Exception e) {
        log.warn("Passport service unavailable for Clerk user {}: {}", clerkUserId, e.getMessage());
        return Optional.empty();
    }

    @Override
    @CircuitBreaker(name = "passportService", fallbackMethod = "hasValidPassportByClerkUserIdFallback")
    @Retry(name = "externalServices")
    public boolean hasValidPassportByClerkUserId(String clerkUserId) {
        return getPassportByClerkUserIdInternal(clerkUserId).isPresent();
    }

    /**
     * Fallback method for hasValidPassportByClerkUserId when circuit breaker is open or call fails.
     * Returns false to signal unable to validate.
     */
    private boolean hasValidPassportByClerkUserIdFallback(String clerkUserId, Exception e) {
        log.warn("Passport validation unavailable for Clerk user {}: {}", clerkUserId, e.getMessage());
        return false;
    }

    @Override
    @CacheEvict(value = CacheConfig.PASSPORT_SCORES_CACHE, key = "#passport.candidateId()")
    public void savePassport(CompetencyPassport passport) {
        log.debug("Saving passport for candidate: {} (cache evicted)", passport.candidateId());
        passportStore.put(passport.candidateId(), passport);
    }

    /**
     * Invalidate the cached passport when candidate data changes.
     *
     * @param candidateId The candidate ID to evict from cache
     */
    @CacheEvict(value = CacheConfig.PASSPORT_SCORES_CACHE, key = "#candidateId")
    public void invalidatePassportCache(UUID candidateId) {
        log.debug("Invalidating passport cache for candidate: {}", candidateId);
    }

    /**
     * Internal passport lookup without circuit breaker (to avoid double-wrapping).
     * Used by other methods that already have circuit breaker protection.
     */
    private Optional<CompetencyPassport> getPassportInternal(UUID candidateId) {
        return Optional.ofNullable(passportStore.get(candidateId))
            .filter(CompetencyPassport::isValid);
    }

    /**
     * Internal passport lookup by Clerk user ID without circuit breaker.
     * Used by other methods that already have circuit breaker protection.
     */
    private Optional<CompetencyPassport> getPassportByClerkUserIdInternal(String clerkUserId) {
        if (clerkUserId == null || clerkUserId.isBlank()) {
            return Optional.empty();
        }
        UUID candidateId = clerkUserIdToCandidateId.get(clerkUserId);
        if (candidateId == null) {
            return Optional.empty();
        }
        return getPassportInternal(candidateId);
    }

    /**
     * Create a demo passport for testing purposes.
     *
     * @param candidateId The candidate ID
     * @param competencyScores Map of competency IDs to scores
     * @return The created passport
     */
    public CompetencyPassport createDemoPassport(UUID candidateId, Map<UUID, Double> competencyScores) {
        CompetencyPassport passport = new CompetencyPassport(
            candidateId,
            competencyScores,
            Map.of(
                "OPENNESS", 3.5,
                "CONSCIENTIOUSNESS", 4.0,
                "EXTRAVERSION", 3.2,
                "AGREEABLENESS", 3.8,
                "NEUROTICISM", 2.5
            ),
            LocalDateTime.now(),
            true
        );
        savePassport(passport);
        return passport;
    }

    /**
     * Create a demo passport for testing purposes with Clerk User ID mapping.
     * This allows testing the Delta Testing flow end-to-end.
     *
     * @param clerkUserId The Clerk user ID to map
     * @param competencyScores Map of competency IDs to scores
     * @return The created passport
     */
    public CompetencyPassport createDemoPassportWithClerkId(String clerkUserId, Map<UUID, Double> competencyScores) {
        UUID candidateId = UUID.randomUUID();
        clerkUserIdToCandidateId.put(clerkUserId, candidateId);
        log.debug("Mapped clerkUserId {} to candidateId {}", clerkUserId, candidateId);
        return createDemoPassport(candidateId, competencyScores);
    }

    /**
     * Register a mapping between Clerk User ID and internal candidate UUID.
     * In production, this would be handled by a UserProfile service.
     *
     * @param clerkUserId The Clerk user ID
     * @param candidateId The internal candidate UUID
     */
    public void registerClerkUserMapping(String clerkUserId, UUID candidateId) {
        clerkUserIdToCandidateId.put(clerkUserId, candidateId);
        log.debug("Registered mapping: clerkUserId {} -> candidateId {}", clerkUserId, candidateId);
    }
}
