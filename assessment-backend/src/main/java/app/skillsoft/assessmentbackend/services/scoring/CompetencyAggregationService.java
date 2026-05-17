package app.skillsoft.assessmentbackend.services.scoring;

import app.skillsoft.assessmentbackend.domain.dto.CompetencyScoreDto;
import app.skillsoft.assessmentbackend.domain.dto.IndicatorScoreDto;
import app.skillsoft.assessmentbackend.domain.entities.BehavioralIndicator;
import app.skillsoft.assessmentbackend.domain.entities.Competency;
import app.skillsoft.assessmentbackend.domain.entities.TestAnswer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Shared service that extracts the common aggregation pipeline used by all scoring strategies.
 *
 * The pipeline has four stages:
 * 1. **Normalize answers** - skip handling, indicator extraction, score normalization
 * 2. **Indicator aggregation** - roll up normalized scores into {@link IndicatorAggregation}
 * 3. **Competency rollup** - aggregate indicators into {@link CompetencyAggregation}, build DTOs
 * 4. **Evidence sufficiency** - flag competencies below minimum question thresholds
 *
 * Strategies that need custom logic during normalization (e.g., TeamFit tracking Big Five scores)
 * can call {@link #rollUpIndicatorsToCompetencies} and {@link #buildCompetencyScores} directly,
 * bypassing the full pipeline while still reusing the shared rollup and DTO-building code.
 *
 * Per coding standards: constructor injection, @Service annotation.
 */
@Service
public class CompetencyAggregationService {

    private static final Logger log = LoggerFactory.getLogger(CompetencyAggregationService.class);

    private final CompetencyBatchLoader competencyBatchLoader;
    private final IndicatorBatchLoader indicatorBatchLoader;
    private final ScoreNormalizer scoreNormalizer;

    public CompetencyAggregationService(CompetencyBatchLoader competencyBatchLoader,
                                         IndicatorBatchLoader indicatorBatchLoader,
                                         ScoreNormalizer scoreNormalizer) {
        this.competencyBatchLoader = competencyBatchLoader;
        this.indicatorBatchLoader = indicatorBatchLoader;
        this.scoreNormalizer = scoreNormalizer;
    }

    /**
     * Result record for the full aggregation pipeline.
     *
     * @param indicatorAggregations  per-indicator aggregation data (indicator ID -> aggregation)
     * @param competencyAggregations per-competency aggregation data (competency ID -> aggregation)
     * @param competencyScores       final DTOs ready for inclusion in {@link ScoringResult}
     * @param competencyCache        preloaded competency entities for strategy-specific lookups
     * @param indicatorCache         preloaded indicator entities for strategy-specific lookups
     */
    public record AggregationResult(
            Map<UUID, IndicatorAggregation> indicatorAggregations,
            Map<UUID, CompetencyAggregation> competencyAggregations,
            List<CompetencyScoreDto> competencyScores,
            Map<UUID, Competency> competencyCache,
            Map<UUID, BehavioralIndicator> indicatorCache
    ) {}

    /**
     * Run the full aggregation pipeline: normalize answers, aggregate by indicator,
     * roll up to competency, build DTOs.
     *
     * This is the primary entry point for strategies that do not need custom logic
     * during the normalization loop (Overview, JobFit).
     *
     * @param answers the test answers to process
     * @return complete aggregation result with all intermediate data available
     */
    public AggregationResult aggregate(List<TestAnswer> answers) {
        // 1. Batch load all competencies and indicators upfront to prevent N+1 queries
        Map<UUID, Competency> competencyCache = competencyBatchLoader.loadCompetenciesForAnswers(answers);
        Map<UUID, BehavioralIndicator> indicatorCache = indicatorBatchLoader.loadIndicatorsForAnswers(answers);

        // 2. Normalize answers and group by indicator
        Map<UUID, IndicatorAggregation> indicatorAggs = normalizeAndAggregate(answers);

        // 3. Roll up indicators to competencies
        Map<UUID, CompetencyAggregation> competencyAggs = rollUpIndicatorsToCompetencies(
                indicatorAggs, indicatorCache);

        // 4. Build CompetencyScoreDto list
        List<CompetencyScoreDto> competencyScores = buildCompetencyScores(
                competencyAggs, competencyCache);

        return new AggregationResult(indicatorAggs, competencyAggs, competencyScores,
                competencyCache, indicatorCache);
    }

    /**
     * Normalize all valid answers and aggregate by indicator.
     *
     * Skips answers that are unanswered (answeredAt == null) or explicitly skipped.
     * For each valid answer, extracts the indicator ID, normalizes the score,
     * and accumulates it into the indicator's aggregation.
     *
     * @param answers the test answers to normalize
     * @return map of indicator ID to its aggregation
     */
    public Map<UUID, IndicatorAggregation> normalizeAndAggregate(List<TestAnswer> answers) {
        Map<UUID, IndicatorAggregation> indicatorAggs = new HashMap<>();

        for (TestAnswer answer : answers) {
            if (answer.getIsSkipped() || answer.getAnsweredAt() == null) {
                continue;
            }

            Optional<UUID> indicatorIdOpt = indicatorBatchLoader.extractIndicatorIdSafe(answer);
            if (indicatorIdOpt.isEmpty()) {
                log.warn("Skipping answer {} - unable to extract indicator ID", answer.getId());
                continue;
            }
            UUID indicatorId = indicatorIdOpt.get();

            double normalizedScore = scoreNormalizer.normalize(answer);

            indicatorAggs.computeIfAbsent(indicatorId, IndicatorAggregation::new)
                    .addAnswer(normalizedScore);
        }

        log.debug("Aggregated {} indicators from {} answers", indicatorAggs.size(), answers.size());
        return indicatorAggs;
    }

    /**
     * Roll up indicator-level aggregations to competency-level aggregations.
     *
     * For each indicator aggregation, looks up the parent competency via the indicator cache,
     * creates an {@link IndicatorScoreDto}, and adds it to the corresponding
     * {@link CompetencyAggregation}.
     *
     * @param indicatorAggs  per-indicator aggregation data
     * @param indicatorCache preloaded indicator entities (with competency relationships)
     * @return map of competency ID to its aggregation
     */
    public Map<UUID, CompetencyAggregation> rollUpIndicatorsToCompetencies(
            Map<UUID, IndicatorAggregation> indicatorAggs,
            Map<UUID, BehavioralIndicator> indicatorCache) {

        Map<UUID, CompetencyAggregation> competencyAggs = new HashMap<>();

        for (var entry : indicatorAggs.entrySet()) {
            UUID indicatorId = entry.getKey();
            IndicatorAggregation indAgg = entry.getValue();

            BehavioralIndicator indicator = indicatorBatchLoader.getFromCache(indicatorCache, indicatorId);
            if (indicator == null || indicator.getCompetency() == null) {
                log.warn("Skipping indicator {} - competency not found", indicatorId);
                continue;
            }

            UUID competencyId = indicator.getCompetency().getId();
            IndicatorScoreDto indicatorDto = indAgg.toDto(indicator);

            competencyAggs.computeIfAbsent(competencyId, CompetencyAggregation::new)
                    .addIndicator(indicatorDto, indAgg);

            log.debug("Indicator {}: {} questions, score {}/{} ({}%)",
                    indicator.getTitle(), indAgg.getQuestionCount(), indAgg.getTotalScore(),
                    indAgg.getTotalMaxScore(), String.format("%.2f", indicatorDto.getPercentage()));
        }

        return competencyAggs;
    }

    /**
     * Build {@link CompetencyScoreDto} list from competency aggregations.
     *
     * For each competency aggregation, looks up the competency entity from the cache
     * and delegates to {@link CompetencyAggregation#toDto(Competency)} to create the DTO.
     *
     * @param competencyAggs per-competency aggregation data
     * @param competencyCache preloaded competency entities
     * @return list of competency score DTOs
     */
    public List<CompetencyScoreDto> buildCompetencyScores(
            Map<UUID, CompetencyAggregation> competencyAggs,
            Map<UUID, Competency> competencyCache) {

        List<CompetencyScoreDto> finalScores = new ArrayList<>();

        for (var entry : competencyAggs.entrySet()) {
            UUID competencyId = entry.getKey();
            CompetencyAggregation compAgg = entry.getValue();

            Competency competency = competencyBatchLoader.getFromCache(competencyCache, competencyId);
            CompetencyScoreDto scoreDto = compAgg.toDto(competency);

            finalScores.add(scoreDto);

            log.debug("Competency {}: {} indicators, {} questions, score {}/{} ({}%)",
                    scoreDto.getCompetencyName(), scoreDto.getIndicatorScores().size(),
                    scoreDto.getQuestionsAnswered(), scoreDto.getScore(),
                    scoreDto.getMaxScore(), String.format("%.2f", scoreDto.getPercentage()));
        }

        return finalScores;
    }

    /**
     * Apply evidence sufficiency flags to competency scores.
     *
     * For each competency where the number of questions answered is below the minimum
     * threshold, sets the insufficientEvidence flag and a human-readable evidence note.
     *
     * @param scores                    the competency scores to check
     * @param minQuestionsPerCompetency minimum number of questions required for sufficient evidence
     */
    public void applyEvidenceSufficiency(List<CompetencyScoreDto> scores, int minQuestionsPerCompetency) {
        for (CompetencyScoreDto cs : scores) {
            if (cs.getQuestionsAnswered() < minQuestionsPerCompetency) {
                cs.setInsufficientEvidence(true);
                cs.setEvidenceNote("Score based on " + cs.getQuestionsAnswered()
                        + " question(s); minimum " + minQuestionsPerCompetency + " required");
                log.debug("Insufficient evidence for competency {}: {} questions (min {})",
                        cs.getCompetencyName(), cs.getQuestionsAnswered(), minQuestionsPerCompetency);
            }
        }
    }
}
