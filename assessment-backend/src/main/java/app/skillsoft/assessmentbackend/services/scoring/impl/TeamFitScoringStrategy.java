package app.skillsoft.assessmentbackend.services.scoring.impl;

import app.skillsoft.assessmentbackend.config.ScoringConfiguration;
import app.skillsoft.assessmentbackend.domain.dto.CompetencyScoreDto;
import app.skillsoft.assessmentbackend.domain.dto.IndicatorScoreDto;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.TeamFitBlueprint;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.TestBlueprintDto;
import app.skillsoft.assessmentbackend.domain.entities.*;
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
 * Scoring strategy for TEAM_FIT (Dynamic Gap Analysis) assessments.
 *
 * Implements Scenario C scoring logic with two-level aggregation:
 * 1. Normalize raw answer scores (Likert 1-5 -> 0-1, SJT weights -> 0-1)
 * 2. Aggregate by indicator (sum normalized scores per indicator)
 * 3. Roll up to competency (sum indicator scores per competency)
 * 4. Use ESCO URIs for skill normalization across team members
 * 5. Analyze personality compatibility using Big Five mappings
 * 6. Implement Role Saturation scoring to identify team gaps
 *
 * Per ROADMAP.md Section 1.2 - Dynamic Gap Analysis Strategy:
 * - Uses ESCO URIs for skill normalization across team members
 * - Analyzes personality compatibility using Big Five from Competency Passport
 * - Implements Role Saturation scoring to identify team gaps and redundancies
 */
@Service
@Transactional(readOnly = true)
public class TeamFitScoringStrategy implements ScoringStrategy {

    private static final Logger log = LoggerFactory.getLogger(TeamFitScoringStrategy.class);

    private final CompetencyBatchLoader competencyBatchLoader;
    private final IndicatorBatchLoader indicatorBatchLoader;
    private final ScoringConfiguration scoringConfig;
    private final ScoreNormalizer scoreNormalizer;

    public TeamFitScoringStrategy(
            CompetencyBatchLoader competencyBatchLoader,
            IndicatorBatchLoader indicatorBatchLoader,
            ScoringConfiguration scoringConfig,
            ScoreNormalizer scoreNormalizer) {
        this.competencyBatchLoader = competencyBatchLoader;
        this.indicatorBatchLoader = indicatorBatchLoader;
        this.scoringConfig = scoringConfig;
        this.scoreNormalizer = scoreNormalizer;
    }

    @Override
    public ScoringResult calculate(TestSession session, List<TestAnswer> answers) {
        log.info("Calculating Scenario C (Team Fit) score with indicator breakdown for session: {}", session.getId());

        TestTemplate template = session.getTemplate();
        TeamFitBlueprint blueprint = extractTeamFitBlueprint(template);

        UUID teamId = blueprint != null ? blueprint.getTeamId() : null;

        // Get Team Fit configuration
        ScoringConfiguration.Thresholds.TeamFit teamFitConfig = scoringConfig.getThresholds().getTeamFit();
        ScoringConfiguration.Weights weights = scoringConfig.getWeights();

        // Saturation threshold can be overridden by blueprint, otherwise use config default
        double saturationThreshold = blueprint != null
                ? blueprint.getSaturationThreshold()
                : teamFitConfig.getSaturationThreshold();

        log.debug("Team Fit parameters - Team ID: {}, Saturation Threshold: {}", teamId, saturationThreshold);

        // Batch load all competencies and indicators upfront to prevent N+1 queries
        Map<UUID, Competency> competencyCache = competencyBatchLoader.loadCompetenciesForAnswers(answers);
        Map<UUID, BehavioralIndicator> indicatorCache = indicatorBatchLoader.loadIndicatorsForAnswers(answers);

        // Step 1: Normalize & Aggregate Scores by Indicator (first level)
        Map<UUID, IndicatorAggregation> indicatorAggs = new HashMap<>();

        // Track Big Five and ESCO mappings for team compatibility analysis
        Map<String, Double> bigFiveScores = new HashMap<>();
        Map<String, Integer> bigFiveCounts = new HashMap<>();
        Map<String, Double> escoScores = new HashMap<>();
        Map<String, Integer> escoCounts = new HashMap<>();

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

            // Get competency from indicator for Big Five and ESCO tracking
            BehavioralIndicator indicator = indicatorBatchLoader.getFromCache(indicatorCache, indicatorId);
            Competency competency = (indicator != null) ? indicator.getCompetency() : null;

            if (competency != null) {
                // Aggregate Big Five scores for personality compatibility
                String bigFiveCategory = competency.getBigFiveCategory();
                if (bigFiveCategory != null) {
                    bigFiveScores.merge(bigFiveCategory, normalizedScore, Double::sum);
                    bigFiveCounts.merge(bigFiveCategory, 1, Integer::sum);
                }

                // Aggregate ESCO scores for skill normalization
                String escoUri = competency.getEscoUri();
                if (escoUri != null) {
                    escoScores.merge(escoUri, normalizedScore, Double::sum);
                    escoCounts.merge(escoUri, 1, Integer::sum);
                }
            }
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

        // Step 3: Create Score DTOs with Team Fit Analysis
        List<CompetencyScoreDto> finalScores = new ArrayList<>();
        double totalWeightedScore = 0.0;

        // Track competencies that contribute to team diversity vs saturation
        int diversityContributors = 0;
        int saturationContributors = 0;

        for (var entry : competencyAggs.entrySet()) {
            UUID competencyId = entry.getKey();
            CompetencyAggregation compAgg = entry.getValue();

            // Get competency details from preloaded cache
            Competency competency = competencyBatchLoader.getFromCache(competencyCache, competencyId);
            String competencyName = competency != null ? competency.getName() : "Unknown Competency";
            String onetCode = competency != null ? competency.getOnetCode() : null;
            String escoUri = competency != null ? competency.getEscoUri() : null;

            double percentage = compAgg.getWeightedPercentage();
            double average = percentage / 100.0;

            // For Team Fit, track questions that demonstrate competency
            int questionsCorrect = (int) Math.round(average * compAgg.getQuestionCount());

            CompetencyScoreDto scoreDto = new CompetencyScoreDto();
            scoreDto.setCompetencyId(competencyId);
            scoreDto.setCompetencyName(competencyName);
            scoreDto.setScore(compAgg.getTotalScore());
            scoreDto.setMaxScore(compAgg.getTotalMaxScore());
            scoreDto.setPercentage(percentage);
            scoreDto.setQuestionsAnswered(compAgg.getQuestionCount());
            scoreDto.setQuestionsCorrect(questionsCorrect);
            scoreDto.setOnetCode(onetCode);
            scoreDto.setIndicatorScores(compAgg.getIndicatorScores());

            finalScores.add(scoreDto);

            // Determine if this competency contributes to team diversity or saturation
            double diversityThreshold = teamFitConfig.getDiversityThreshold();
            if (average >= saturationThreshold) {
                saturationContributors++;
            } else if (average >= diversityThreshold) {
                diversityContributors++;
            }

            // Weight based on ESCO mapping
            double weight = (escoUri != null && !escoUri.isEmpty()) ? weights.getEscoBoost() : 1.0;

            // Additional weight for competencies with Big Five mapping
            String bigFive = competency != null ? competency.getBigFiveCategory() : null;
            if (bigFive != null) {
                weight *= weights.getBigFiveBoost();
            }

            totalWeightedScore += (percentage * weight);

            log.debug("Competency {} (ESCO: {}, Big Five: {}): {} indicators, score {}%, contribution: {}",
                    competencyName, escoUri, bigFive, compAgg.getIndicatorScores().size(),
                    String.format("%.2f", percentage),
                    average >= saturationThreshold ? "SATURATION" : (average >= diversityThreshold ? "DIVERSITY" : "GAP"));
        }

        // Step 4: Calculate Big Five Personality Profile Summary
        Map<String, Double> bigFiveAverages = new HashMap<>();
        for (var bigFiveEntry : bigFiveScores.entrySet()) {
            String trait = bigFiveEntry.getKey();
            double avg = bigFiveEntry.getValue() / bigFiveCounts.get(trait);
            bigFiveAverages.put(trait, avg * 100.0);
        }

        log.debug("Big Five profile: {}", bigFiveAverages);

        // Track gap count (competencies where candidate scores below diversity threshold)
        int competencyCount = finalScores.size();
        int gapContributors = competencyCount - diversityContributors - saturationContributors;

        // Step 5: Calculate Overall Team Fit Score
        double escoBoostForWeighting = weights.getEscoBoost();
        double totalWeight = competencyCount > 0
                ? finalScores.stream()
                .mapToDouble(s -> {
                    double w = 1.0;
                    if (s.getOnetCode() != null && !s.getOnetCode().isEmpty()) w *= escoBoostForWeighting;
                    return w;
                })
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

        // Calculate team compatibility factor
        double diversityRatio = competencyCount > 0
                ? (double) diversityContributors / competencyCount
                : 0.0;

        double saturationRatio = competencyCount > 0
                ? (double) saturationContributors / competencyCount
                : 0.0;

        // Adjust score based on diversity/saturation balance
        double teamFitMultiplier = 1.0;
        double diversityBonusThreshold = teamFitConfig.getDiversityBonusThreshold();
        double saturationPenaltyThreshold = teamFitConfig.getSaturationPenaltyThreshold();
        double diversityBonus = teamFitConfig.getDiversityBonus();
        double saturationPenalty = teamFitConfig.getSaturationPenalty();

        if (diversityRatio > diversityBonusThreshold && saturationRatio < (1.0 - diversityBonusThreshold)) {
            teamFitMultiplier = diversityBonus;
        } else if (saturationRatio > saturationPenaltyThreshold) {
            teamFitMultiplier = saturationPenalty;
        }

        double adjustedPercentage = overallPercentage * teamFitMultiplier;

        log.info("Team Fit score calculated: {} (adjusted: {}%), diversity: {}%, saturation: {}%, indicators: {}",
                String.format("%.2f", overallScore),
                String.format("%.2f", adjustedPercentage),
                String.format("%.2f", diversityRatio * 100),
                String.format("%.2f", saturationRatio * 100),
                indicatorAggs.size());

        // Step 6: Create Result
        ScoringResult result = new ScoringResult();
        result.setOverallScore(overallScore);
        result.setOverallPercentage(adjustedPercentage);
        result.setCompetencyScores(finalScores);
        result.setGoal(AssessmentGoal.TEAM_FIT);

        // Set Big Five personality profile (only populated for TEAM_FIT)
        result.setBigFiveProfile(bigFiveAverages.isEmpty() ? null : bigFiveAverages);

        // Set Team Fit metrics for detailed analysis
        ScoringResult.TeamFitMetrics teamFitMetrics = ScoringResult.TeamFitMetrics.builder()
                .diversityRatio(diversityRatio)
                .saturationRatio(saturationRatio)
                .teamFitMultiplier(teamFitMultiplier)
                .diversityCount(diversityContributors)
                .saturationCount(saturationContributors)
                .gapCount(gapContributors)
                .build();
        result.setTeamFitMetrics(teamFitMetrics);

        // For Team Fit, "pass" means the candidate would add value to the team
        double passThreshold = teamFitConfig.getPassThreshold() * 100.0;
        double minDiversityRatio = teamFitConfig.getMinDiversityRatio();
        boolean addsTeamValue = adjustedPercentage >= passThreshold && diversityRatio >= minDiversityRatio;
        result.setPassed(addsTeamValue);

        log.info("Team Fit assessment: {} (score: {}%, diversity: {}%, Big Five traits: {})",
                addsTeamValue ? "ADDS VALUE" : "LIMITED FIT",
                String.format("%.2f", adjustedPercentage),
                String.format("%.2f", diversityRatio * 100),
                bigFiveAverages.size());

        return result;
    }

    /**
     * Extract TeamFitBlueprint from template configuration.
     *
     * @param template The test template
     * @return TeamFitBlueprint or null if not available
     */
    private TeamFitBlueprint extractTeamFitBlueprint(TestTemplate template) {
        if (template == null) {
            return null;
        }

        TestBlueprintDto typedBlueprint = template.getTypedBlueprint();
        if (typedBlueprint instanceof TeamFitBlueprint) {
            return (TeamFitBlueprint) typedBlueprint;
        }

        // Fallback: create from legacy blueprint if available
        if (template.getBlueprint() != null) {
            Map<String, Object> blueprint = template.getBlueprint();
            TeamFitBlueprint teamFitBlueprint = new TeamFitBlueprint();

            Object teamIdObj = blueprint.get("teamId");
            if (teamIdObj != null) {
                try {
                    teamFitBlueprint.setTeamId(UUID.fromString(teamIdObj.toString()));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid team ID format in blueprint: {}", teamIdObj);
                }
            }

            Object threshold = blueprint.get("saturationThreshold");
            if (threshold instanceof Number) {
                teamFitBlueprint.setSaturationThreshold(((Number) threshold).doubleValue());
            }

            return teamFitBlueprint;
        }

        return null;
    }

    @Override
    public AssessmentGoal getSupportedGoal() {
        return AssessmentGoal.TEAM_FIT;
    }
}
