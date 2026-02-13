package app.skillsoft.assessmentbackend.services.external.impl;

import app.skillsoft.assessmentbackend.config.CacheConfig;
import app.skillsoft.assessmentbackend.domain.entities.TeamStatus;
import app.skillsoft.assessmentbackend.repository.TeamRepository;
import app.skillsoft.assessmentbackend.services.external.TeamService;
import app.skillsoft.assessmentbackend.services.team.TeamProfileAggregationService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Database-backed implementation of TeamService.
 *
 * Provides team profile data from the database for TEAM_FIT assessments.
 * Uses TeamProfileAggregationService to compute profiles from TestResult data.
 *
 * Resilience patterns applied:
 * - Circuit Breaker: Opens after 50% failure rate over 10 calls, stays open 60s
 * - Retry: Up to 3 attempts with 500ms wait for transient failures
 * - Caching: L1 cache with Caffeine for frequently accessed team profiles
 */
@Service
@Slf4j
public class TeamServiceImpl implements TeamService {

    private final TeamRepository teamRepository;
    private final TeamProfileAggregationService aggregationService;

    public TeamServiceImpl(
            TeamRepository teamRepository,
            TeamProfileAggregationService aggregationService) {
        this.teamRepository = teamRepository;
        this.aggregationService = aggregationService;
    }

    @Override
    @CircuitBreaker(name = "teamService", fallbackMethod = "getTeamProfileFallback")
    @Retry(name = "externalServices")
    @Cacheable(
        value = CacheConfig.TEAM_PROFILES_CACHE,
        key = "#teamId",
        unless = "#result == null || !#result.isPresent()"
    )
    public Optional<TeamProfile> getTeamProfile(UUID teamId) {
        log.debug("Computing team profile for team: {} (cache miss)", teamId);
        return aggregationService.computeTeamProfile(teamId);
    }

    /**
     * Fallback method for getTeamProfile when circuit breaker is open or external call fails.
     * Returns empty Optional to signal unavailability.
     */
    private Optional<TeamProfile> getTeamProfileFallback(UUID teamId, Exception e) {
        log.warn("Team service unavailable for team {}: {}", teamId, e.getMessage());
        return Optional.empty();
    }

    /**
     * Invalidate the cached team profile when team composition changes.
     *
     * @param teamId The team ID to evict from cache
     */
    @CacheEvict(value = CacheConfig.TEAM_PROFILES_CACHE, key = "#teamId")
    public void invalidateTeamCache(UUID teamId) {
        log.debug("Invalidating team cache for team: {}", teamId);
    }

    @Override
    @CircuitBreaker(name = "teamService", fallbackMethod = "getSaturationFallback")
    @Retry(name = "externalServices")
    public Optional<Double> getSaturation(UUID teamId, UUID competencyId) {
        return getTeamProfileInternal(teamId)
            .map(profile -> profile.competencySaturation().get(competencyId));
    }

    /**
     * Fallback method for getSaturation when circuit breaker is open or external call fails.
     * Returns empty Optional to signal unavailability.
     */
    private Optional<Double> getSaturationFallback(UUID teamId, UUID competencyId, Exception e) {
        log.warn("Team saturation unavailable for team {} / competency {}: {}",
                 teamId, competencyId, e.getMessage());
        return Optional.empty();
    }

    @Override
    @CircuitBreaker(name = "teamService", fallbackMethod = "getUndersaturatedCompetenciesFallback")
    @Retry(name = "externalServices")
    public List<UUID> getUndersaturatedCompetencies(UUID teamId, double threshold) {
        return getTeamProfileInternal(teamId)
            .map(profile -> profile.competencySaturation().entrySet().stream()
                .filter(entry -> entry.getValue() < threshold)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList()))
            .orElse(List.of());
    }

    /**
     * Fallback method for getUndersaturatedCompetencies when circuit breaker is open or call fails.
     * Returns empty list to signal unavailability.
     */
    private List<UUID> getUndersaturatedCompetenciesFallback(UUID teamId, double threshold, Exception e) {
        log.warn("Team undersaturated competencies unavailable for team {}: {}", teamId, e.getMessage());
        return List.of();
    }

    @Override
    @CircuitBreaker(name = "teamService", fallbackMethod = "calculateTeamFitScoreFallback")
    @Retry(name = "externalServices")
    public double calculateTeamFitScore(UUID teamId, Map<UUID, Double> candidateCompetencies) {
        return getTeamProfileInternal(teamId)
            .map(profile -> {
                // Calculate fit score based on how well candidate fills gaps
                var gaps = profile.skillGaps();
                if (gaps.isEmpty()) {
                    return 50.0; // Neutral score if no gaps
                }

                double totalGapFill = 0.0;
                int matchingGaps = 0;

                for (UUID gapCompetencyId : gaps) {
                    Double candidateScore = candidateCompetencies.get(gapCompetencyId);
                    if (candidateScore != null && candidateScore >= 3.0) {
                        // Candidate has decent proficiency in this gap area
                        totalGapFill += (candidateScore / 5.0) * 100;
                        matchingGaps++;
                    }
                }

                if (matchingGaps == 0) {
                    return 25.0; // Low score if candidate doesn't fill any gaps
                }

                // Weight by percentage of gaps filled
                double gapCoverage = (double) matchingGaps / gaps.size();
                double avgGapFill = totalGapFill / matchingGaps;

                return (avgGapFill * 0.6) + (gapCoverage * 40);
            })
            .orElse(0.0);
    }

    /**
     * Fallback method for calculateTeamFitScore when circuit breaker is open or call fails.
     * Returns 0.0 to signal unable to calculate.
     */
    private double calculateTeamFitScoreFallback(UUID teamId, Map<UUID, Double> candidateCompetencies, Exception e) {
        log.warn("Team fit score calculation unavailable for team {}: {}", teamId, e.getMessage());
        return 0.0;
    }

    @Override
    @CircuitBreaker(name = "teamService", fallbackMethod = "isValidTeamFallback")
    @Retry(name = "externalServices")
    public boolean isValidTeam(UUID teamId) {
        return teamRepository.existsByIdAndStatus(teamId, TeamStatus.ACTIVE);
    }

    /**
     * Fallback method for isValidTeam when circuit breaker is open or external call fails.
     * Returns false to signal unable to validate.
     */
    private boolean isValidTeamFallback(UUID teamId, Exception e) {
        log.warn("Team validation unavailable for team {}: {}", teamId, e.getMessage());
        return false;
    }

    /**
     * Internal team profile lookup without circuit breaker (to avoid double-wrapping).
     * Used by other methods that already have circuit breaker protection.
     */
    private Optional<TeamProfile> getTeamProfileInternal(UUID teamId) {
        return aggregationService.computeTeamProfile(teamId);
    }

}
