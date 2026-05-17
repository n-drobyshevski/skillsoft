package app.skillsoft.assessmentbackend.services.scoring;

import app.skillsoft.assessmentbackend.domain.dto.CompetencyScoreDto;
import app.skillsoft.assessmentbackend.domain.entities.CompetencyReliability;
import app.skillsoft.assessmentbackend.repository.CompetencyReliabilityRepository;
import app.skillsoft.assessmentbackend.repository.TestResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Post-processing step that calculates confidence intervals for competency scores.
 *
 * Uses Cronbach's alpha from CompetencyReliability to compute:
 * - SEM (Standard Error of Measurement) = SD * sqrt(1 - alpha)
 * - 95% CI = score +/- 1.96 * SEM
 *
 * When alpha data is unavailable or insufficient, CI fields remain null
 * (graceful degradation - scores are still valid without CI).
 *
 * Called by ScoringOrchestrationServiceImpl after scoring strategies compute scores.
 */
@Service
public class ConfidenceIntervalCalculator {

    private static final Logger log = LoggerFactory.getLogger(ConfidenceIntervalCalculator.class);

    /**
     * Z-score for 95% confidence interval (two-tailed).
     */
    private static final double Z_95 = 1.96;

    /**
     * Default standard deviation assumption for percentage scores (0-100 scale).
     * Used when actual SD cannot be computed from sample data.
     * Based on typical assessment score distributions.
     */
    private static final double DEFAULT_SD = 15.0;

    private final CompetencyReliabilityRepository reliabilityRepository;
    private final TestResultRepository testResultRepository;

    public ConfidenceIntervalCalculator(CompetencyReliabilityRepository reliabilityRepository,
                                        TestResultRepository testResultRepository) {
        this.reliabilityRepository = reliabilityRepository;
        this.testResultRepository = testResultRepository;
    }

    /**
     * Minimum sample size for computing actual SD from historical data.
     * Below this threshold, a bootstrap estimate is used.
     */
    private static final int MIN_SAMPLE_SIZE_FOR_SD = 30;

    /**
     * Sample size below which DEFAULT_SD is used with a log warning.
     * Between this and MIN_SAMPLE_SIZE_FOR_SD, a bootstrap estimate is applied.
     */
    private static final int MIN_SAMPLE_SIZE_FOR_BOOTSTRAP = 5;

    /**
     * Enrich competency scores with confidence interval data.
     * Modifies the DTOs in-place.
     *
     * SD estimation strategy (three-tier):
     * - Sample >= 30: Use actual computed SD from historical data
     * - Sample 6..29: Use bootstrap estimate: DEFAULT_SD * sqrt(30 / sampleSize)
     *   (conservatively inflates the default to account for small-sample uncertainty)
     * - Sample <= 5: Use DEFAULT_SD as-is with a log warning
     *
     * @param competencyScores List of competency score DTOs to enrich
     */
    public void enrichWithConfidenceIntervals(List<CompetencyScoreDto> competencyScores) {
        if (competencyScores == null || competencyScores.isEmpty()) {
            return;
        }

        // Batch load reliability data for all competencies (single query)
        Set<UUID> competencyIds = new HashSet<>();
        for (CompetencyScoreDto cs : competencyScores) {
            if (cs.getCompetencyId() != null) {
                competencyIds.add(cs.getCompetencyId());
            }
        }

        Map<UUID, CompetencyReliability> reliabilityMap = loadReliabilityMap(competencyIds);

        // Compute SD per competency with three-tier fallback
        Map<UUID, Double> sdMap = computeSdMap(competencyIds);

        int enrichedCount = 0;
        for (CompetencyScoreDto cs : competencyScores) {
            if (cs.getCompetencyId() == null || cs.getPercentage() == null) {
                continue;
            }

            CompetencyReliability reliability = reliabilityMap.get(cs.getCompetencyId());
            if (reliability == null || reliability.getCronbachAlpha() == null) {
                log.debug("No reliability data for competency {}, skipping CI calculation",
                        cs.getCompetencyId());
                continue;
            }

            double alpha = reliability.getCronbachAlpha().doubleValue();

            // Skip if alpha is invalid (negative or > 1.0)
            if (alpha <= 0 || alpha > 1.0) {
                log.debug("Invalid alpha {} for competency {}, skipping CI",
                        alpha, cs.getCompetencyId());
                continue;
            }

            // SEM = SD * sqrt(1 - alpha), using tiered SD estimation
            double sd = sdMap.getOrDefault(cs.getCompetencyId(), DEFAULT_SD);
            double sem = sd * Math.sqrt(1.0 - alpha);
            double score = cs.getPercentage();

            // 95% CI = score +/- 1.96 * SEM
            double ciLower = Math.max(0.0, score - Z_95 * sem);
            double ciUpper = Math.min(100.0, score + Z_95 * sem);

            cs.setSem(round2(sem));
            cs.setCiLower(round2(ciLower));
            cs.setCiUpper(round2(ciUpper));
            cs.setCronbachAlpha(round4(alpha));
            enrichedCount++;
        }

        log.debug("Enriched {}/{} competency scores with confidence intervals",
                enrichedCount, competencyScores.size());
    }

    /**
     * Compute SD for each competency using a three-tier strategy:
     * - Tier 1 (n >= 30): Use actual historical STDDEV_POP
     * - Tier 2 (5 < n < 30): Bootstrap estimate: DEFAULT_SD * sqrt(30 / n)
     * - Tier 3 (n <= 5): Use DEFAULT_SD with a warning
     *
     * @param competencyIds Set of competency IDs to compute SD for
     * @return Map from competency ID to estimated SD
     */
    private Map<UUID, Double> computeSdMap(Set<UUID> competencyIds) {
        Map<UUID, Double> sdMap = new HashMap<>();

        for (UUID compId : competencyIds) {
            String compIdStr = compId.toString();

            // First, get the sample count
            Long sampleCount = testResultRepository.countCompetencyScoreSamples(compIdStr);
            long n = sampleCount != null ? sampleCount : 0;

            if (n >= MIN_SAMPLE_SIZE_FOR_SD) {
                // Tier 1: Sufficient sample - use actual SD from the existing threshold-based query
                Double sd = testResultRepository.calculateCompetencyScoreSD(compIdStr);
                if (sd != null && sd > 0) {
                    sdMap.put(compId, sd);
                    log.debug("Competency {}: using actual SD={} (n={})", compId, round2(sd), n);
                } else {
                    sdMap.put(compId, DEFAULT_SD);
                }
            } else if (n > MIN_SAMPLE_SIZE_FOR_BOOTSTRAP) {
                // Tier 2: Small sample (6-29) - use bootstrap estimate
                // Scale DEFAULT_SD conservatively upward to account for small-sample uncertainty
                double bootstrapSd = DEFAULT_SD * Math.sqrt((double) MIN_SAMPLE_SIZE_FOR_SD / n);
                sdMap.put(compId, bootstrapSd);
                log.debug("Competency {}: using bootstrap SD={} (n={}, default={}, ratio={})",
                        compId, round2(bootstrapSd), n, DEFAULT_SD,
                        round2((double) MIN_SAMPLE_SIZE_FOR_SD / n));
            } else {
                // Tier 3: Insufficient sample (0-5) - use DEFAULT_SD
                if (n > 0) {
                    log.warn("Competency {}: insufficient sample size (n={}) for SD estimation, " +
                             "using DEFAULT_SD={}. Confidence intervals may be unreliable.",
                            compId, n, DEFAULT_SD);
                }
                sdMap.put(compId, DEFAULT_SD);
            }
        }

        return sdMap;
    }

    /**
     * Batch-load reliability data for a set of competency IDs using a single query.
     * Returns a map from competency ID to reliability entity.
     *
     * Replaces the previous N+1 approach (one query per competency) with a single
     * batch query via findByCompetencyIdIn().
     */
    private Map<UUID, CompetencyReliability> loadReliabilityMap(Set<UUID> competencyIds) {
        if (competencyIds.isEmpty()) {
            return Map.of();
        }

        List<CompetencyReliability> reliabilities = reliabilityRepository.findByCompetencyIdIn(competencyIds);
        Map<UUID, CompetencyReliability> map = new HashMap<>();
        for (CompetencyReliability r : reliabilities) {
            UUID compId = r.getCompetencyId();
            if (compId != null) {
                map.put(compId, r);
            }
        }

        log.debug("Batch-loaded reliability data for {}/{} competencies",
                map.size(), competencyIds.size());
        return map;
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static double round4(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}
