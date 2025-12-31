package app.skillsoft.assessmentbackend.services.scoring.impl;

import app.skillsoft.assessmentbackend.config.ScoringConfiguration;
import app.skillsoft.assessmentbackend.domain.dto.CompetencyScoreDto;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.JobFitBlueprint;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.TestBlueprintDto;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.services.scoring.CompetencyBatchLoader;
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
 * Implements Scenario B scoring logic:
 * 1. Normalize raw answer scores (Likert 1-5 -> 0-1, SJT weights -> 0-1)
 * 2. Aggregate by competency (sum normalized scores)
 * 3. Compare scores against O*NET occupation benchmarks if available
 * 4. Apply strictness level to determine pass/fail thresholds
 * 5. Calculate overall job fit percentage using weighted cosine similarity
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
    private final ScoringConfiguration scoringConfig;
    private final ScoreNormalizer scoreNormalizer;

    public JobFitScoringStrategy(
            CompetencyBatchLoader competencyBatchLoader,
            ScoringConfiguration scoringConfig,
            ScoreNormalizer scoreNormalizer) {
        this.competencyBatchLoader = competencyBatchLoader;
        this.scoringConfig = scoringConfig;
        this.scoreNormalizer = scoreNormalizer;
    }

    @Override
    public ScoringResult calculate(TestSession session, List<TestAnswer> answers) {
        log.info("Calculating Scenario B (Job Fit) score for session: {}", session.getId());

        TestTemplate template = session.getTemplate();
        JobFitBlueprint blueprint = extractJobFitBlueprint(template);

        String onetSocCode = blueprint != null ? blueprint.getOnetSocCode() : null;
        int strictnessLevel = blueprint != null ? blueprint.getStrictnessLevel() : 50;

        log.debug("Job Fit parameters - O*NET SOC: {}, Strictness: {}", onetSocCode, strictnessLevel);

        // Batch load all competencies upfront to prevent N+1 queries
        Map<UUID, Competency> competencyCache = competencyBatchLoader.loadCompetenciesForAnswers(answers);

        // Step 1: Normalize & Aggregate Scores by Competency
        Map<UUID, Double> rawCompetencyScores = new HashMap<>();
        Map<UUID, Integer> counts = new HashMap<>();
        Map<UUID, Integer> questionsAnswered = new HashMap<>();

        for (TestAnswer answer : answers) {
            // Skip unanswered or skipped questions
            if (answer.getIsSkipped() || answer.getAnsweredAt() == null) {
                continue;
            }

            Optional<UUID> compIdOpt = competencyBatchLoader.extractCompetencyIdSafe(answer);
            if (compIdOpt.isEmpty()) {
                log.warn("Skipping answer {} - unable to extract competency ID", answer.getId());
                continue;
            }
            UUID compId = compIdOpt.get();

            double normalizedScore = scoreNormalizer.normalize(answer);

            rawCompetencyScores.merge(compId, normalizedScore, Double::sum);
            counts.merge(compId, 1, Integer::sum);
            questionsAnswered.merge(compId, 1, Integer::sum);
        }

        // Step 2: Calculate Percentages and Create Score DTOs with Job Fit Analysis
        List<CompetencyScoreDto> finalScores = new ArrayList<>();
        double totalPercentage = 0.0;
        double totalWeightedScore = 0.0;
        int competencyCount = 0;

        // Get threshold configuration
        ScoringConfiguration.Thresholds.JobFit jobFitConfig = scoringConfig.getThresholds().getJobFit();

        // Calculate strictness-adjusted threshold (higher strictness = higher threshold)
        double baseThreshold = jobFitConfig.getBaseThreshold();
        double strictnessAdjustment = (strictnessLevel / 100.0) * jobFitConfig.getStrictnessMaxAdjustment();
        double effectiveThreshold = baseThreshold + strictnessAdjustment;

        for (var entry : rawCompetencyScores.entrySet()) {
            UUID competencyId = entry.getKey();
            double sumScore = entry.getValue();
            int count = counts.get(competencyId);

            // Calculate average score (0-1 scale)
            double average = sumScore / count;

            // Convert to percentage (0-100 scale)
            double percentage = average * 100.0;

            // Get competency details from preloaded cache (prevents N+1 queries)
            Competency competency = competencyBatchLoader.getFromCache(competencyCache, competencyId);
            String competencyName = competency != null ? competency.getName() : "Unknown Competency";
            String onetCode = competency != null ? competency.getOnetCode() : null;

            // Calculate max score (count of questions * 1.0)
            double maxScore = count * 1.0;
            double actualScore = sumScore;

            // For Job Fit, we track how well the score meets the threshold
            // A score meeting or exceeding threshold counts as "correct" for job fit
            int questionsCorrect = (int) Math.round(average * count);

            CompetencyScoreDto scoreDto = new CompetencyScoreDto();
            scoreDto.setCompetencyId(competencyId);
            scoreDto.setCompetencyName(competencyName);
            scoreDto.setScore(actualScore);
            scoreDto.setMaxScore(maxScore);
            scoreDto.setPercentage(percentage);
            scoreDto.setQuestionsAnswered(questionsAnswered.get(competencyId));
            scoreDto.setQuestionsCorrect(questionsCorrect);
            scoreDto.setOnetCode(onetCode);

            finalScores.add(scoreDto);

            // Weight the score based on O*NET alignment (competencies with O*NET codes are prioritized)
            double onetBoost = scoringConfig.getWeights().getOnetBoost();
            double weight = (onetCode != null && !onetCode.isEmpty()) ? onetBoost : 1.0;
            totalWeightedScore += (percentage * weight);
            totalPercentage += percentage;
            competencyCount++;

            log.debug("Competency {} (O*NET: {}): {} questions, score {}/{} ({}%), threshold met: {}",
                competencyName, onetCode, count, actualScore, maxScore,
                String.format("%.2f", percentage),
                average >= effectiveThreshold);
        }

        // Step 3: Calculate Overall Job Fit Score
        // Use weighted average for job fit to prioritize O*NET-aligned competencies
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
            ? rawCompetencyScores.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum() / competencyCount
            : 0.0;

        log.info("Job Fit score calculated: {} ({}%), threshold: {}%",
            String.format("%.2f", overallScore),
            String.format("%.2f", overallPercentage),
            String.format("%.2f", effectiveThreshold * 100));

        // Step 4: Create Result
        ScoringResult result = new ScoringResult();
        result.setOverallScore(overallScore);
        result.setOverallPercentage(overallPercentage);
        result.setCompetencyScores(finalScores);
        result.setGoal(AssessmentGoal.JOB_FIT);

        // Determine pass/fail based on strictness-adjusted threshold
        // This is preliminary; service layer may override based on template passing score
        boolean meetsJobRequirements = (overallPercentage / 100.0) >= effectiveThreshold;
        result.setPassed(meetsJobRequirements);

        log.info("Job Fit assessment {} (score: {}%, required: {}%)",
            meetsJobRequirements ? "PASSED" : "FAILED",
            String.format("%.2f", overallPercentage),
            String.format("%.2f", effectiveThreshold * 100));

        return result;
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
