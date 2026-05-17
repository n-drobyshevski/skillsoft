package app.skillsoft.assessmentbackend.services.scoring.impl;

import app.skillsoft.assessmentbackend.config.ScoringConfiguration;
import app.skillsoft.assessmentbackend.domain.dto.CompetencyScoreDto;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.JobFitBlueprint;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.TestBlueprintDto;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.services.external.OnetService;
import app.skillsoft.assessmentbackend.services.scoring.CompetencyAggregation;
import app.skillsoft.assessmentbackend.services.scoring.CompetencyAggregationService;
import app.skillsoft.assessmentbackend.services.scoring.ScoringPrecision;
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

    private final CompetencyAggregationService aggregationService;
    private final ScoringConfiguration scoringConfig;
    private final OnetService onetService;

    public JobFitScoringStrategy(
            CompetencyAggregationService aggregationService,
            ScoringConfiguration scoringConfig,
            OnetService onetService) {
        this.aggregationService = aggregationService;
        this.scoringConfig = scoringConfig;
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

        // Steps 1-3: Shared aggregation pipeline (normalize → indicator → competency → DTOs)
        CompetencyAggregationService.AggregationResult aggResult = aggregationService.aggregate(answers);
        Map<UUID, CompetencyAggregation> competencyAggs = aggResult.competencyAggregations();
        List<CompetencyScoreDto> finalScores = aggResult.competencyScores();
        Map<UUID, Competency> competencyCache = aggResult.competencyCache();

        // Step 4: Job Fit-specific DTO enrichment
        double totalWeightedScore = 0.0;
        ScoringConfiguration.Thresholds.JobFit jobFitConfig = scoringConfig.getThresholds().getJobFit();

        // Calculate strictness-adjusted threshold (higher strictness = higher threshold)
        double baseThreshold = jobFitConfig.getBaseThreshold();
        double strictnessAdjustment = (strictnessLevel / 100.0) * jobFitConfig.getStrictnessMaxAdjustment();
        double effectiveThreshold = baseThreshold + strictnessAdjustment;

        for (CompetencyScoreDto scoreDto : finalScores) {
            UUID competencyId = scoreDto.getCompetencyId();
            CompetencyAggregation compAgg = competencyAggs.get(competencyId);

            // Get competency details for O*NET enrichment
            Competency competency = competencyCache.get(competencyId);
            String competencyName = scoreDto.getCompetencyName();
            String onetCode = competency != null ? competency.getOnetCode() : null;

            double percentage = scoreDto.getPercentage();

            // For Job Fit, we track how well the score meets the threshold
            int questionsCorrect = compAgg != null && compAgg.getTotalMaxScore() > 0
                    ? (int) Math.round((percentage / 100.0) * compAgg.getQuestionCount()) : 0;
            scoreDto.setQuestionsCorrect(questionsCorrect);

            // S1: Propagate O*NET benchmark to DTO
            if (onetSocCode != null && !benchmarkLookup.isEmpty()) {
                Double benchmark = benchmarkLookup.get(competencyName);
                if (benchmark != null) {
                    scoreDto.setBenchmarkScore(benchmark * 20); // Convert 1-5 scale to 0-100 percentage
                }
            }

            // Check minimum evidence threshold
            int minQuestions = jobFitConfig.getMinQuestionsPerCompetency();
            if (scoreDto.getQuestionsAnswered() < minQuestions) {
                scoreDto.setInsufficientEvidence(true);
                scoreDto.setEvidenceNote(String.format(
                    "Only %d of %d minimum questions answered",
                    scoreDto.getQuestionsAnswered(), minQuestions));
                log.warn("Insufficient evidence for competency {}: {} questions (min: {})",
                    competencyName, scoreDto.getQuestionsAnswered(), minQuestions);
            }

            // Weight the score based on O*NET alignment (competencies with O*NET codes are prioritized)
            double onetBoost = scoringConfig.getWeights().getOnetBoost();
            double weight = (onetCode != null && !onetCode.isEmpty()) ? onetBoost : 1.0;
            totalWeightedScore += (percentage * weight);

            log.debug("Competency {} (O*NET: {}): {} indicators, {} questions, score {}/{} ({}%), threshold met: {}",
                    competencyName, onetCode, scoreDto.getIndicatorScores().size(),
                    scoreDto.getQuestionsAnswered(),
                    scoreDto.getScore(), scoreDto.getMaxScore(), String.format("%.2f", percentage),
                    ScoringPrecision.meetsThreshold(percentage / 100.0, effectiveThreshold));
        }

        // Step 5: Calculate Overall Job Fit Score
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
                aggResult.indicatorAggregations().size());

        // Step 6: Create Result
        ScoringResult result = new ScoringResult();
        result.setOverallScore(overallScore);
        result.setOverallPercentage(overallPercentage);
        result.setCompetencyScores(finalScores);
        result.setGoal(AssessmentGoal.JOB_FIT);

        // Determine pass/fail based on strictness-adjusted threshold
        // Round both sides to 4dp to eliminate IEEE 754 floating-point boundary errors
        boolean meetsJobRequirements = ScoringPrecision.meetsThreshold(
                overallPercentage / 100.0, effectiveThreshold);
        result.setPassed(meetsJobRequirements);

        // S2: Calculate decision confidence score
        // Factor 1 - Margin: how far the score is from the threshold (>15% away = max confidence)
        double margin = Math.abs((overallPercentage / 100.0) - effectiveThreshold);
        double marginFactor = Math.min(1.0, margin / 0.15);

        // Factor 2 - Evidence: proportion of competencies with sufficient evidence
        long sufficientCount = finalScores.stream()
            .filter(s -> s.getInsufficientEvidence() == null || !s.getInsufficientEvidence())
            .count();
        double evidenceFactor = competencyCount > 0 ? (double) sufficientCount / competencyCount : 0.0;

        // Factor 3 - Coverage: how many competencies were assessed vs benchmarks
        double coverageFactor = benchmarkLookup.isEmpty()
            ? 1.0
            : Math.min(1.0, (double) competencyCount / benchmarkLookup.size());

        // Weighted combination: margin 50%, evidence 30%, coverage 20%
        double confidence = (marginFactor * 0.5) + (evidenceFactor * 0.3) + (coverageFactor * 0.2);
        confidence = Math.round(confidence * 100.0) / 100.0; // Round to 2 decimal places

        String confidenceLevel = confidence >= 0.7 ? "HIGH" : confidence >= 0.4 ? "MEDIUM" : "LOW";

        result.setDecisionConfidence(confidence);
        result.setConfidenceLevel(confidenceLevel);

        // S7: Generate human-readable confidence message
        long insufficientCount = competencyCount - sufficientCount;
        String confidenceMessage = generateConfidenceMessage(
            meetsJobRequirements, confidence, confidenceLevel,
            sufficientCount, competencyCount, insufficientCount, margin);
        result.setConfidenceMessage(confidenceMessage);

        log.info("Job Fit assessment {} (score: {}%, required: {}%, confidence: {} [{}])",
                meetsJobRequirements ? "PASSED" : "FAILED",
                String.format("%.2f", overallPercentage),
                String.format("%.2f", effectiveThreshold * 100),
                String.format("%.2f", confidence),
                confidenceLevel);

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

    /**
     * Generate a human-readable confidence message for hiring managers.
     *
     * The message varies based on the confidence level (HIGH/MEDIUM/LOW)
     * and the pass/fail outcome, providing actionable context for decision-making.
     *
     * @param passed            Whether the candidate passed the assessment
     * @param confidence        Raw confidence score (0.0-1.0)
     * @param confidenceLevel   "HIGH", "MEDIUM", or "LOW"
     * @param sufficientCount   Number of competencies with sufficient evidence
     * @param competencyCount   Total number of competencies assessed
     * @param insufficientCount Number of competencies lacking sufficient evidence
     * @param margin            Distance from the pass/fail threshold
     * @return Human-readable confidence message
     */
    private String generateConfidenceMessage(
            boolean passed, double confidence, String confidenceLevel,
            long sufficientCount, int competencyCount,
            long insufficientCount, double margin) {

        return switch (confidenceLevel) {
            case "HIGH" -> passed
                ? String.format("High confidence: Candidate clearly meets job requirements across %d competencies.",
                    sufficientCount)
                : String.format("High confidence: Candidate does not meet the minimum requirements. Key gaps identified in %d areas.",
                    insufficientCount > 0 ? insufficientCount : competencyCount);
            case "MEDIUM" -> passed
                ? "Moderate confidence: Candidate meets requirements but results are close to the threshold."
                : "Moderate confidence: Candidate falls slightly below requirements. Consider retesting in gap areas.";
            case "LOW" -> passed
                ? String.format("Low confidence: Candidate appears to meet requirements but evidence is limited (%d competencies lack sufficient data).",
                    insufficientCount)
                : "Low confidence: Insufficient evidence to make a definitive assessment. Recommend full retest.";
            default -> String.format("Decision confidence: %.0f%%", confidence * 100);
        };
    }

    @Override
    public AssessmentGoal getSupportedGoal() {
        return AssessmentGoal.JOB_FIT;
    }
}
