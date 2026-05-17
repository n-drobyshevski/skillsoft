package app.skillsoft.assessmentbackend.services.scoring;

import app.skillsoft.assessmentbackend.domain.dto.CompetencyScoreDto;
import app.skillsoft.assessmentbackend.repository.TestResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Post-processing step that calculates per-competency percentile ranks.
 *
 * Uses native JSONB queries on test_results.competency_scores to determine
 * where a candidate's competency score falls relative to other test takers
 * for the same template.
 *
 * This provides richer insight than overall percentile alone:
 * A candidate at 50th percentile overall might be 90th in Communication
 * and 10th in Leadership.
 *
 * Called by ScoringOrchestrationServiceImpl after scoring strategies compute scores.
 */
@Service
public class SubscalePercentileCalculator {

    private static final Logger log = LoggerFactory.getLogger(SubscalePercentileCalculator.class);

    private final TestResultRepository resultRepository;

    public SubscalePercentileCalculator(TestResultRepository resultRepository) {
        this.resultRepository = resultRepository;
    }

    /**
     * Enrich competency scores with per-competency percentile ranks.
     * Modifies the DTOs in-place.
     *
     * @param competencyScores List of competency score DTOs to enrich
     * @param templateId       The template ID to compare against for historical data
     */
    public void enrichWithPercentiles(List<CompetencyScoreDto> competencyScores, UUID templateId) {
        if (competencyScores == null || competencyScores.isEmpty() || templateId == null) {
            return;
        }

        int enrichedCount = 0;
        for (CompetencyScoreDto cs : competencyScores) {
            if (cs.getCompetencyId() == null || cs.getPercentage() == null) {
                continue;
            }

            try {
                Integer percentile = calculateCompetencyPercentile(
                        templateId, cs.getCompetencyId(), cs.getPercentage());
                cs.setPercentile(percentile);
                if (percentile != null) {
                    enrichedCount++;
                }
            } catch (Exception e) {
                log.debug("Could not calculate percentile for competency {}: {}",
                        cs.getCompetencyId(), e.getMessage());
            }
        }

        log.debug("Enriched {}/{} competency scores with subscale percentiles",
                enrichedCount, competencyScores.size());
    }

    /**
     * Calculate the percentile rank for a specific competency score
     * within the context of a template's historical results.
     *
     * Uses JSONB query to extract and compare competency-level percentages
     * from historical test results.
     *
     * @param templateId   The template to compare against
     * @param competencyId The competency to calculate percentile for
     * @param score        The current percentage score
     * @return Percentile rank (0-100), or 50 if insufficient data
     */
    private Integer calculateCompetencyPercentile(UUID templateId, UUID competencyId, double score) {
        Long belowCount = resultRepository.countCompetencyScoresBelow(
                templateId, competencyId.toString(), score);
        Long totalCount = resultRepository.countCompetencyScoresTotal(
                templateId, competencyId.toString());

        if (totalCount == null || totalCount <= 1) {
            return 50; // Default for first result
        }

        double percentile = ((double) belowCount / (totalCount - 1)) * 100;
        int result = (int) Math.round(percentile);
        return Math.max(0, Math.min(100, result));
    }
}
