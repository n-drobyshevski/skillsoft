package app.skillsoft.assessmentbackend.services.scoring.impl;

import app.skillsoft.assessmentbackend.domain.dto.CompetencyScoreDto;
import app.skillsoft.assessmentbackend.domain.dto.IndicatorScoreDto;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.services.scoring.CompetencyAggregation;
import app.skillsoft.assessmentbackend.services.scoring.CompetencyBatchLoader;
import app.skillsoft.assessmentbackend.services.scoring.IndicatorAggregation;
import app.skillsoft.assessmentbackend.services.scoring.IndicatorBatchLoader;
import app.skillsoft.assessmentbackend.services.scoring.ScoreNormalizer;
import app.skillsoft.assessmentbackend.services.scoring.ScoringResult;
import app.skillsoft.assessmentbackend.services.scoring.ScoringStrategy;
import app.skillsoft.assessmentbackend.util.LoggingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Scoring strategy for OVERVIEW (Universal Baseline / Competency Passport) assessments.
 *
 * Implements Scenario A scoring logic with two-level aggregation:
 * 1. Normalize raw answer scores (Likert 1-5 → 0-1, SJT weights → 0-1)
 * 2. Aggregate by indicator (sum normalized scores per indicator)
 * 3. Roll up to competency (sum indicator scores per competency)
 * 4. Calculate percentages at both indicator and competency levels
 * 5. Generate nested score breakdown (Competency → Indicators)
 *
 * Note: Big Five personality projection is handled on the frontend using
 * O*NET → Big Five mapping. This preserves server security while allowing
 * real-time recalibration on the client.
 *
 * Per ROADMAP.md Section 1.2 - Universal Baseline Strategy
 */
@Service
@Transactional(readOnly = true)
public class OverviewScoringStrategy implements ScoringStrategy {

    private static final Logger log = LoggerFactory.getLogger(OverviewScoringStrategy.class);

    private final CompetencyBatchLoader competencyBatchLoader;
    private final IndicatorBatchLoader indicatorBatchLoader;
    private final ScoreNormalizer scoreNormalizer;

    public OverviewScoringStrategy(CompetencyBatchLoader competencyBatchLoader,
                                   IndicatorBatchLoader indicatorBatchLoader,
                                   ScoreNormalizer scoreNormalizer) {
        this.competencyBatchLoader = competencyBatchLoader;
        this.indicatorBatchLoader = indicatorBatchLoader;
        this.scoreNormalizer = scoreNormalizer;
    }

    @Override
    public ScoringResult calculate(TestSession session, List<TestAnswer> answers) {
        // Set session context for all scoring log messages
        LoggingContext.setSessionId(session.getId());
        LoggingContext.setOperation("overview-scoring");

        log.info("Calculating Scenario A (Overview) score with indicator breakdown: session={} answers={} user={}",
                session.getId(), answers.size(), session.getClerkUserId());

        // Batch load all competencies and indicators upfront to prevent N+1 queries
        Map<UUID, Competency> competencyCache = competencyBatchLoader.loadCompetenciesForAnswers(answers);
        Map<UUID, BehavioralIndicator> indicatorCache = indicatorBatchLoader.loadIndicatorsForAnswers(answers);

        // Step 1: Normalize & Aggregate Scores by Indicator (first level)
        Map<UUID, IndicatorAggregation> indicatorAggs = new HashMap<>();

        for (TestAnswer answer : answers) {
            // Skip unanswered or skipped questions
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

        // Step 2: Roll up indicators to competencies (second level)
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

        // Step 3: Create final CompetencyScoreDto list with nested indicators
        List<CompetencyScoreDto> finalScores = new ArrayList<>();
        double totalPercentage = 0.0;

        for (var entry : competencyAggs.entrySet()) {
            UUID competencyId = entry.getKey();
            CompetencyAggregation compAgg = entry.getValue();

            Competency competency = competencyBatchLoader.getFromCache(competencyCache, competencyId);
            CompetencyScoreDto scoreDto = compAgg.toDto(competency);

            finalScores.add(scoreDto);
            totalPercentage += scoreDto.getPercentage();

            log.debug("Competency {}: {} indicators, {} questions, score {}/{} ({}%)",
                    scoreDto.getCompetencyName(), scoreDto.getIndicatorScores().size(),
                    scoreDto.getQuestionsAnswered(), scoreDto.getScore(),
                    scoreDto.getMaxScore(), String.format("%.2f", scoreDto.getPercentage()));
        }

        // Step 4: Calculate Overall Score
        int competencyCount = finalScores.size();
        double overallPercentage = competencyCount > 0
                ? totalPercentage / competencyCount
                : 0.0;

        double overallScore = competencyCount > 0
                ? finalScores.stream()
                .mapToDouble(CompetencyScoreDto::getScore)
                .sum() / competencyCount
                : 0.0;

        log.info("Overall score calculated: {} ({}/100) with {} competencies, {} indicators",
                String.format("%.2f", overallScore), String.format("%.2f", overallPercentage),
                competencyCount, indicatorAggs.size());

        // Step 5: Create Result
        ScoringResult result = new ScoringResult();
        result.setOverallScore(overallScore);
        result.setOverallPercentage(overallPercentage);
        result.setCompetencyScores(finalScores);
        result.setGoal(AssessmentGoal.OVERVIEW);

        // Pass/fail will be set by service layer based on template passing score

        return result;
    }

    @Override
    public AssessmentGoal getSupportedGoal() {
        return AssessmentGoal.OVERVIEW;
    }
}
