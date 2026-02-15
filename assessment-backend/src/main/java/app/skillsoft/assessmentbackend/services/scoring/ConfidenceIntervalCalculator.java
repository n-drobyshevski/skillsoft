package app.skillsoft.assessmentbackend.services.scoring;

import app.skillsoft.assessmentbackend.domain.dto.CompetencyScoreDto;
import app.skillsoft.assessmentbackend.domain.entities.CompetencyReliability;
import app.skillsoft.assessmentbackend.repository.CompetencyReliabilityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

    public ConfidenceIntervalCalculator(CompetencyReliabilityRepository reliabilityRepository) {
        this.reliabilityRepository = reliabilityRepository;
    }

    /**
     * Enrich competency scores with confidence interval data.
     * Modifies the DTOs in-place.
     *
     * @param competencyScores List of competency score DTOs to enrich
     */
    public void enrichWithConfidenceIntervals(List<CompetencyScoreDto> competencyScores) {
        if (competencyScores == null || competencyScores.isEmpty()) {
            return;
        }

        // Batch load reliability data for all competencies
        Set<UUID> competencyIds = new HashSet<>();
        for (CompetencyScoreDto cs : competencyScores) {
            if (cs.getCompetencyId() != null) {
                competencyIds.add(cs.getCompetencyId());
            }
        }

        Map<UUID, CompetencyReliability> reliabilityMap = loadReliabilityMap(competencyIds);

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

            // SEM = SD * sqrt(1 - alpha)
            double sem = DEFAULT_SD * Math.sqrt(1.0 - alpha);
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
     * Load reliability data for a set of competency IDs.
     * Returns a map from competency ID to reliability entity.
     */
    private Map<UUID, CompetencyReliability> loadReliabilityMap(Set<UUID> competencyIds) {
        Map<UUID, CompetencyReliability> map = new HashMap<>();
        for (UUID compId : competencyIds) {
            reliabilityRepository.findByCompetency_Id(compId).ifPresent(r -> map.put(compId, r));
        }
        return map;
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static double round4(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}
