package app.skillsoft.assessmentbackend.services.scoring.impl;

import app.skillsoft.assessmentbackend.config.ScoringConfiguration;
import app.skillsoft.assessmentbackend.domain.dto.CompetencyScoreDto;
import app.skillsoft.assessmentbackend.domain.dto.IndicatorScoreDto;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.JobFitBlueprint;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.TestBlueprintDto;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.services.external.OnetService;
import app.skillsoft.assessmentbackend.services.scoring.CompetencyAggregation;
import app.skillsoft.assessmentbackend.services.scoring.CompetencyBatchLoader;
import app.skillsoft.assessmentbackend.services.scoring.IndicatorAggregation;
import app.skillsoft.assessmentbackend.services.scoring.IndicatorBatchLoader;
import app.skillsoft.assessmentbackend.services.scoring.ScoreNormalizer;
import app.skillsoft.assessmentbackend.services.scoring.ScoringResult;
import app.skillsoft.assessmentbackend.services.scoring.ScoringStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Scoring strategy for JOB_FIT (Targeted Fit / O*NET Benchmark) assessments.
 *
 * Implements Scenario B scoring logic with two-level aggregation:
 * 1. Normalize raw answer scores (Likert 1-5 -> 0-1, SJT weights -> 0-1)
 * 2. Aggregate by indicator (sum normalized scores per indicator)
 * 3. Roll up to competency (sum indicator scores per competency)
 * 4. Compare scores against O*NET occupation benchmarks if available
 * 5. Apply strictness level to determine pass/fail thresholds
 * 6. Calculate overall job fit percentage using weighted cosine similarity
 *
 * Per ROADMAP.md Section 1.2 - Targeted Fit Strategy:
 * - Uses O*NET SOC code to load benchmark requirements
 * - Implements Delta Testing - reuses Competency Passport data if available
 * - Applies Weighted Cosine Similarity scoring against occupation profile
 */
@Service
@Transactional(readOnly = true)
public class JobFitScoringStrategy implements ScoringStrategy {

    private static final Logger log = LoggerFactory.getLogger(JobFitScoringStrategy.class);

    private final CompetencyBatchLoader competencyBatchLoader;
    private final IndicatorBatchLoader indicatorBatchLoader;
    private final ScoringConfiguration scoringConfig;
    private final ScoreNormalizer scoreNormalizer;
    private final OnetService onetService;

    public JobFitScoringStrategy(
            CompetencyBatchLoader competencyBatchLoader,
            IndicatorBatchLoader indicatorBatchLoader,
            ScoringConfiguration scoringConfig,
            ScoreNormalizer scoreNormalizer,
            OnetService onetService) {
        this.competencyBatchLoader = competencyBatchLoader;
        this.indicatorBatchLoader = indicatorBatchLoader;
        this.scoringConfig = scoringConfig;
        this.scoreNormalizer = scoreNormalizer;
        this.onetService = onetService;
    }

    @Override
    public ScoringResult calculate(TestSession session, List<TestAnswer> answers) {
        log.info("Calculating Scenario B (Job Fit) score with indicator breakdown for session: {}", session.getId());

        TestTemplate template = session.getTemplate();
        JobFitBlueprint blueprint = extractJobFitBlueprint(template);

        String onetSocCode = blueprint != null ? blueprint.getOnetSocCode() : null;
        int strictnessLevel = blueprint != null ? blueprint.getStrictnessLevel() : 50;

        log.debug("Job Fit parameters - O*NET SOC: {}, Strictness: {}", onetSocCode, strictnessLevel);

        // S1: Load O*NET benchmark lookup for propagation to CompetencyScoreDto
        Map<String, Double> benchmarkLookup = buildBenchmarkLookup(onetSocCode);

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
        }

        // Step 3: Create Score DTOs with Job Fit Analysis
        List<CompetencyScoreDto> finalScores = new ArrayList<>();
        double totalWeightedScore = 0.0;

        // Get threshold configuration
        ScoringConfiguration.Thresholds.JobFit jobFitConfig = scoringConfig.getThresholds().getJobFit();

        // Calculate strictness-adjusted threshold (higher strictness = higher threshold)
        double baseThreshold = jobFitConfig.getBaseThreshold();
        double strictnessAdjustment = (strictnessLevel / 100.0) * jobFitConfig.getStrictnessMaxAdjustment();
        double effectiveThreshold = baseThreshold + strictnessAdjustment;

        for (var entry : competencyAggs.entrySet()) {
            UUID competencyId = entry.getKey();
            CompetencyAggregation compAgg = entry.getValue();

            // Get competency details from preloaded cache
            Competency competency = competencyBatchLoader.getFromCache(competencyCache, competencyId);
            String competencyName = competency != null ? competency.getName() : "Unknown Competency";
            String onetCode = competency != null ? competency.getOnetCode() : null;

            double percentage = compAgg.getWeightedPercentage();

            // For Job Fit, we track how well the score meets the threshold
            int questionsCorrect = compAgg.getTotalMaxScore() > 0
                    ? (int) Math.round((percentage / 100.0) * compAgg.getQuestionCount()) : 0;

            CompetencyScoreDto scoreDto = new CompetencyScoreDto();
            scoreDto.setCompetencyId(competencyId);
            scoreDto.setCompetencyName(competencyName);
            scoreDto.setScore(compAgg.getTotalScore());
            scoreDto.setMaxScore(compAgg.getTotalMaxScore());
            scoreDto.setPercentage(percentage);
            scoreDto.setQuestionsAnswered(compAgg.getQuestionCount());
            scoreDto.setQuestionsCorrect(questionsCorrect);
            scoreDto.setOnetCode(onetCode);

            // S1: Propagate O*NET benchmark to DTO
            if (onetSocCode != null && !benchmarkLookup.isEmpty()) {
                Double benchmark = benchmarkLookup.get(competencyName);
                if (benchmark != null) {
                    scoreDto.setBenchmarkScore(benchmark * 20); // Convert 1-5 scale to 0-100 percentage
                }
            }

            scoreDto.setIndicatorScores(compAgg.getIndicatorScores());

            // Check minimum evidence threshold
            int minQuestions = scoringConfig.getThresholds().getJobFit().getMinQuestionsPerCompetency();
            if (compAgg.getQuestionCount() < minQuestions) {
                scoreDto.setInsufficientEvidence(true);
                scoreDto.setEvidenceNote(String.format(
                    "Only %d of %d minimum questions answered",
                    compAgg.getQuestionCount(), minQuestions));
                log.warn("Insufficient evidence for competency {}: {} questions (min: {})",
                    competencyName, compAgg.getQuestionCount(), minQuestions);
            }

            finalScores.add(scoreDto);

            // Weight the score based on O*NET alignment (competencies with O*NET codes are prioritized)
            double onetBoost = scoringConfig.getWeights().getOnetBoost();
            double weight = (onetCode != null && !onetCode.isEmpty()) ? onetBoost : 1.0;
            totalWeightedScore += (percentage * weight);

            log.debug("Competency {} (O*NET: {}): {} indicators, {} questions, score {}/{} ({}%), threshold met: {}",
                    competencyName, onetCode, compAgg.getIndicatorScores().size(), compAgg.getQuestionCount(),
                    compAgg.getTotalScore(), compAgg.getTotalMaxScore(), String.format("%.2f", percentage),
                    (percentage / 100.0) >= effectiveThreshold);
        }

        // Step 4: Calculate Overall Job Fit Score
        int competencyCount = finalScores.size();
        double onetBoostForWeighting = scoringConfig.getWeights().getOnetBoost();
        double totalWeight = competencyCount > 0
                ? finalScores.stream()
                .mapToDouble(s -> (s.getOnetCode() != null && !s.getOnetCode().isEmpty()) ? onetBoostForWeighting : 1.0)
                .sum()
                : 1.0;

        double overallPercentage = competencyCount > 0
                ? totalWeightedScore / totalWeight
                : 0.0;

        double overallScore = competencyCount > 0
                ? finalScores.stream()
                .mapToDouble(CompetencyScoreDto::getScore)
                .sum() / competencyCount
                : 0.0;

        log.info("Job Fit score calculated: {} ({}%), threshold: {}%, indicators: {}",
                String.format("%.2f", overallScore),
                String.format("%.2f", overallPercentage),
                String.format("%.2f", effectiveThreshold * 100),
                indicatorAggs.size());

        // Step 5: Create Result
        ScoringResult result = new ScoringResult();
        result.setOverallScore(overallScore);
        result.setOverallPercentage(overallPercentage);
        result.setCompetencyScores(finalScores);
        result.setGoal(AssessmentGoal.JOB_FIT);

        // Determine pass/fail based on strictness-adjusted threshold
        boolean meetsJobRequirements = (overallPercentage / 100.0) >= effectiveThreshold;
        result.setPassed(meetsJobRequirements);

        log.info("Job Fit assessment {} (score: {}%, required: {}%)",
                meetsJobRequirements ? "PASSED" : "FAILED",
                String.format("%.2f", overallPercentage),
                String.format("%.2f", effectiveThreshold * 100));

        return result;
    }

    /**
     * Build a benchmark lookup map from competency name to O*NET benchmark value.
     * Returns an empty map if the SOC code is null or no profile is found.
     *
     * @param onetSocCode The O*NET Standard Occupational Classification code
     * @return Map of competency name to benchmark score (1-5 scale)
     */
    private Map<String, Double> buildBenchmarkLookup(String onetSocCode) {
        if (onetSocCode == null || onetSocCode.isBlank()) {
            return Map.of();
        }

        return onetService.getProfile(onetSocCode)
                .map(OnetService.OnetProfile::benchmarks)
                .orElseGet(() -> {
                    log.debug("No O*NET profile found for SOC code: {} - skipping benchmark propagation", onetSocCode);
                    return Map.of();
                });
    }

    /**
     * Extract JobFitBlueprint from template configuration.
     *
     * @param template The test template
     * @return JobFitBlueprint or null if not available
     */
    private JobFitBlueprint extractJobFitBlueprint(TestTemplate template) {
        if (template == null) {
            return null;
        }

        TestBlueprintDto typedBlueprint = template.getTypedBlueprint();
        if (typedBlueprint instanceof JobFitBlueprint) {
            return (JobFitBlueprint) typedBlueprint;
        }

        // Fallback: create from legacy blueprint if available
        if (template.getBlueprint() != null) {
            Map<String, Object> blueprint = template.getBlueprint();
            JobFitBlueprint jobFitBlueprint = new JobFitBlueprint();

            Object socCode = blueprint.get("onetSocCode");
            if (socCode != null) {
                jobFitBlueprint.setOnetSocCode(socCode.toString());
            }

            Object strictness = blueprint.get("strictnessLevel");
            if (strictness instanceof Number) {
                jobFitBlueprint.setStrictnessLevel(((Number) strictness).intValue());
            }

            return jobFitBlueprint;
        }

        return null;
    }

    @Override
    public AssessmentGoal getSupportedGoal() {
        return AssessmentGoal.JOB_FIT;
    }
}
