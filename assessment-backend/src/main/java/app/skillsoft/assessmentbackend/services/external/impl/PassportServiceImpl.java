package app.skillsoft.assessmentbackend.services.external.impl;

import app.skillsoft.assessmentbackend.config.CacheConfig;
import app.skillsoft.assessmentbackend.config.PassportProperties;
import app.skillsoft.assessmentbackend.domain.entities.CompetencyPassportEntity;
import app.skillsoft.assessmentbackend.domain.entities.User;
import app.skillsoft.assessmentbackend.repository.CompetencyPassportRepository;
import app.skillsoft.assessmentbackend.repository.UserRepository;
import app.skillsoft.assessmentbackend.services.external.PassportService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Database-backed implementation of PassportService.
 *
 * Persists Competency Passport data in PostgreSQL via {@link CompetencyPassportEntity},
 * replacing the previous in-memory ConcurrentHashMap mock.
 *
 * Resilience patterns preserved:
 * - Circuit Breaker: Opens after 50% failure rate over 10 calls, stays open 30s
 * - Retry: Up to 3 attempts with 500ms wait for transient failures
 * - Caching: L1 cache with Caffeine for frequently accessed passports
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class PassportServiceImpl implements PassportService {

    private final CompetencyPassportRepository passportRepository;
    private final UserRepository userRepository;
    private final PassportProperties passportProperties;

    public PassportServiceImpl(CompetencyPassportRepository passportRepository,
                               UserRepository userRepository,
                               PassportProperties passportProperties) {
        this.passportRepository = passportRepository;
        this.userRepository = userRepository;
        this.passportProperties = passportProperties;
    }

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

        return userRepository.findById(candidateId)
            .map(User::getClerkId)
            .flatMap(this::findValidPassportEntity)
            .map(entity -> toRecord(entity, candidateId));
    }

    private Optional<CompetencyPassport> getPassportFallback(UUID candidateId, Exception e) {
        log.warn("Passport service unavailable for candidate {}: {}", candidateId, e.getMessage());
        return Optional.empty();
    }

    @Override
    @CircuitBreaker(name = "passportService", fallbackMethod = "getCompetencyScoreFallback")
    @Retry(name = "externalServices")
    public Optional<Double> getCompetencyScore(UUID candidateId, UUID competencyId) {
        // Direct entity lookup to avoid self-invocation through proxy (cache bypass issue)
        return userRepository.findById(candidateId)
            .map(User::getClerkId)
            .flatMap(this::findValidPassportEntity)
            .map(entity -> entity.getCompetencyScores().get(competencyId.toString()));
    }

    private Optional<Double> getCompetencyScoreFallback(UUID candidateId, UUID competencyId, Exception e) {
        log.warn("Passport competency score unavailable for candidate {} / competency {}: {}",
                 candidateId, competencyId, e.getMessage());
        return Optional.empty();
    }

    @Override
    @CircuitBreaker(name = "passportService", fallbackMethod = "hasValidPassportFallback")
    @Retry(name = "externalServices")
    public boolean hasValidPassport(UUID candidateId) {
        return userRepository.findById(candidateId)
            .map(User::getClerkId)
            .map(clerkId -> passportRepository.existsValidByClerkUserId(clerkId, LocalDateTime.now()))
            .orElse(false);
    }

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

        return findValidPassportEntity(clerkUserId)
            .map(entity -> {
                // Resolve internal UUID from User entity for the record
                UUID candidateId = userRepository.findByClerkId(clerkUserId)
                    .map(User::getId)
                    .orElse(null);
                return toRecord(entity, candidateId);
            });
    }

    private Optional<CompetencyPassport> getPassportByClerkUserIdFallback(String clerkUserId, Exception e) {
        log.warn("Passport service unavailable for Clerk user {}: {}", clerkUserId, e.getMessage());
        return Optional.empty();
    }

    @Override
    @CircuitBreaker(name = "passportService", fallbackMethod = "hasValidPassportByClerkUserIdFallback")
    @Retry(name = "externalServices")
    public boolean hasValidPassportByClerkUserId(String clerkUserId) {
        if (clerkUserId == null || clerkUserId.isBlank()) {
            return false;
        }
        return passportRepository.existsValidByClerkUserId(clerkUserId, LocalDateTime.now());
    }

    private boolean hasValidPassportByClerkUserIdFallback(String clerkUserId, Exception e) {
        log.warn("Passport validation unavailable for Clerk user {}: {}", clerkUserId, e.getMessage());
        return false;
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.PASSPORT_SCORES_CACHE, key = "#passport.candidateId()")
    public void savePassport(CompetencyPassport passport) {
        log.debug("Saving passport for candidate: {} (cache evicted)", passport.candidateId());

        // Resolve clerkUserId from candidateId (internal UUID -> Clerk ID)
        String clerkUserId = userRepository.findById(passport.candidateId())
            .map(User::getClerkId)
            .orElseThrow(() -> new IllegalArgumentException(
                "No User found for candidateId: " + passport.candidateId()));

        savePassportForClerkUser(
            clerkUserId,
            passport.competencyScores(),
            passport.bigFiveProfile(),
            null // no source result ID when saving via interface method
        );
    }

    /**
     * Save or update a passport directly by Clerk user ID.
     * Used by {@link app.skillsoft.assessmentbackend.events.listeners.PassportUpdateListener}
     * for event-driven auto-creation from scoring results.
     *
     * @param clerkUserId   the Clerk user ID
     * @param scores        competency UUID to score (1.0–5.0) mapping
     * @param bigFive       Big Five personality profile (may be null)
     * @param sourceResultId the TestResult ID that produced these scores (may be null)
     */
    @Transactional
    @CacheEvict(value = CacheConfig.PASSPORT_SCORES_CACHE, key = "'clerk:' + #clerkUserId")
    public void savePassportForClerkUser(String clerkUserId,
                                         Map<UUID, Double> scores,
                                         Map<String, Double> bigFive,
                                         UUID sourceResultId) {
        log.debug("Saving passport for clerkUserId: {}", clerkUserId);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusDays(passportProperties.getValidityDays());

        // Convert UUID keys to String keys for JSONB storage
        Map<String, Double> stringKeyedScores = scores.entrySet().stream()
            .collect(Collectors.toMap(
                e -> e.getKey().toString(),
                Map.Entry::getValue
            ));

        // Upsert: find existing or create new
        CompetencyPassportEntity entity = passportRepository.findByClerkUserId(clerkUserId)
            .orElseGet(() -> {
                CompetencyPassportEntity newEntity = new CompetencyPassportEntity();
                newEntity.setClerkUserId(clerkUserId);
                return newEntity;
            });

        entity.setCompetencyScores(stringKeyedScores);
        entity.setBigFiveProfile(bigFive);
        entity.setLastAssessed(now);
        entity.setExpiresAt(expiresAt);
        entity.setSourceResultId(sourceResultId);

        passportRepository.save(entity);

        log.info("Passport saved for clerkUserId={}, expiresAt={}, competencies={}",
                clerkUserId, expiresAt, stringKeyedScores.size());
    }

    // ---- Internal helpers ----

    /**
     * Find a valid (non-expired) passport entity by Clerk user ID.
     * Private helper to avoid Spring AOP self-invocation issues.
     */
    private Optional<CompetencyPassportEntity> findValidPassportEntity(String clerkUserId) {
        return passportRepository.findValidByClerkUserId(clerkUserId, LocalDateTime.now());
    }

    /**
     * Convert a JPA entity to the public interface record.
     *
     * @param entity      the persisted passport entity
     * @param candidateId the internal User UUID (may be null if unresolvable)
     * @return the public CompetencyPassport record
     */
    private CompetencyPassport toRecord(CompetencyPassportEntity entity, UUID candidateId) {
        // Convert String keys back to UUID keys
        Map<UUID, Double> uuidKeyedScores = entity.getCompetencyScores().entrySet().stream()
            .collect(Collectors.toMap(
                e -> UUID.fromString(e.getKey()),
                Map.Entry::getValue
            ));

        return new CompetencyPassport(
            candidateId,
            uuidKeyedScores,
            entity.getBigFiveProfile() != null ? entity.getBigFiveProfile() : new HashMap<>(),
            entity.getLastAssessed(),
            entity.isValid()
        );
    }
}
