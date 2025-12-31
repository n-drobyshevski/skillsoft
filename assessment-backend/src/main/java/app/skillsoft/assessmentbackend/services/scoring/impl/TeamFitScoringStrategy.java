package app.skillsoft.assessmentbackend.services.scoring.impl;

import app.skillsoft.assessmentbackend.config.ScoringConfiguration;
import app.skillsoft.assessmentbackend.domain.dto.CompetencyScoreDto;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.TeamFitBlueprint;
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
 * Scoring strategy for TEAM_FIT (Dynamic Gap Analysis) assessments.
 *
 * Implements Scenario C scoring logic:
 * 1. Normalize raw answer scores (Likert 1-5 -> 0-1, SJT weights -> 0-1)
 * 2. Aggregate by competency (sum normalized scores)
 * 3. Use ESCO URIs for skill normalization across team members
 * 4. Analyze personality compatibility using Big Five mappings
 * 5. Implement Role Saturation scoring to identify team gaps
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
    private final ScoringConfiguration scoringConfig;
    private final ScoreNormalizer scoreNormalizer;

    public TeamFitScoringStrategy(
            CompetencyBatchLoader competencyBatchLoader,
            ScoringConfiguration scoringConfig,
            ScoreNormalizer scoreNormalizer) {
        this.competencyBatchLoader = competencyBatchLoader;
        this.scoringConfig = scoringConfig;
        this.scoreNormalizer = scoreNormalizer;
    }

    @Override
    public ScoringResult calculate(TestSession session, List<TestAnswer> answers) {
        log.info("Calculating Scenario C (Team Fit) score for session: {}", session.getId());

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

        // Batch load all competencies upfront to prevent N+1 queries
        Map<UUID, Competency> competencyCache = competencyBatchLoader.loadCompetenciesForAnswers(answers);

        // Step 1: Normalize & Aggregate Scores by Competency
        Map<UUID, Double> rawCompetencyScores = new HashMap<>();
        Map<UUID, Integer> counts = new HashMap<>();
        Map<UUID, Integer> questionsAnswered = new HashMap<>();

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

            Optional<UUID> compIdOpt = competencyBatchLoader.extractCompetencyIdSafe(answer);
            if (compIdOpt.isEmpty()) {
                log.warn("Skipping answer {} - unable to extract competency ID", answer.getId());
                continue;
            }
            UUID compId = compIdOpt.get();

            // Get competency from cache for Big Five and ESCO data
            Competency competency = competencyBatchLoader.getFromCache(competencyCache, compId);
            double normalizedScore = scoreNormalizer.normalize(answer);

            rawCompetencyScores.merge(compId, normalizedScore, Double::sum);
            counts.merge(compId, 1, Integer::sum);
            questionsAnswered.merge(compId, 1, Integer::sum);

            // Aggregate Big Five scores for personality compatibility
            String bigFiveCategory = competency != null ? competency.getBigFiveCategory() : null;
            if (bigFiveCategory != null) {
                bigFiveScores.merge(bigFiveCategory, normalizedScore, Double::sum);
                bigFiveCounts.merge(bigFiveCategory, 1, Integer::sum);
            }

            // Aggregate ESCO scores for skill normalization
            String escoUri = competency != null ? competency.getEscoUri() : null;
            if (escoUri != null) {
                escoScores.merge(escoUri, normalizedScore, Double::sum);
                escoCounts.merge(escoUri, 1, Integer::sum);
            }
        }

        // Step 2: Calculate Percentages and Create Score DTOs with Team Fit Analysis
        List<CompetencyScoreDto> finalScores = new ArrayList<>();
        double totalPercentage = 0.0;
        double totalWeightedScore = 0.0;
        int competencyCount = 0;

        // Track competencies that contribute to team diversity vs saturation
        int diversityContributors = 0;
        int saturationContributors = 0;

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
            String escoUri = competency != null ? competency.getEscoUri() : null;

            // Calculate max score (count of questions * 1.0)
            double maxScore = count * 1.0;
            double actualScore = sumScore;

            // For Team Fit, track questions that demonstrate competency
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

            // Determine if this competency contributes to team diversity or saturation
            // High scores (above threshold) in less common competencies add diversity
            // High scores in already-saturated areas may indicate redundancy
            double diversityThreshold = teamFitConfig.getDiversityThreshold();
            if (average >= saturationThreshold) {
                saturationContributors++;
            } else if (average >= diversityThreshold) {
                diversityContributors++;
            }

            // Weight based on ESCO mapping (standardized skills are more valuable for team analysis)
            double weight = (escoUri != null && !escoUri.isEmpty()) ? weights.getEscoBoost() : 1.0;

            // Additional weight for competencies with Big Five mapping (personality compatibility)
            String bigFive = competency != null ? competency.getBigFiveCategory() : null;
            if (bigFive != null) {
                weight *= weights.getBigFiveBoost();
            }

            totalWeightedScore += (percentage * weight);
            totalPercentage += percentage;
            competencyCount++;

            log.debug("Competency {} (ESCO: {}, Big Five: {}): score {}%, contribution: {}",
                competencyName, escoUri, bigFive,
                String.format("%.2f", percentage),
                average >= saturationThreshold ? "SATURATION" : (average >= 0.5 ? "DIVERSITY" : "GAP"));
        }

        // Step 3: Calculate Big Five Personality Profile Summary
        Map<String, Double> bigFiveAverages = new HashMap<>();
        for (var entry : bigFiveScores.entrySet()) {
            String trait = entry.getKey();
            double avg = entry.getValue() / bigFiveCounts.get(trait);
            bigFiveAverages.put(trait, avg * 100.0);
        }

        log.debug("Big Five profile: {}", bigFiveAverages);

        // Track gap count (competencies where candidate scores below diversity threshold)
        int gapContributors = competencyCount - diversityContributors - saturationContributors;

        // Step 4: Calculate Overall Team Fit Score
        // Team Fit score considers:
        // 1. Overall competency performance
        // 2. Diversity vs saturation balance
        // 3. Personality profile completeness

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
            ? rawCompetencyScores.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum() / competencyCount
            : 0.0;

        // Calculate team compatibility factor
        // Diversity is good, but too much saturation without diversity indicates poor fit
        double diversityRatio = competencyCount > 0
            ? (double) diversityContributors / competencyCount
            : 0.0;

        double saturationRatio = competencyCount > 0
            ? (double) saturationContributors / competencyCount
            : 0.0;

        // Adjust score based on diversity/saturation balance
        // Ideal: High diversity, moderate saturation
        double teamFitMultiplier = 1.0;
        double diversityBonusThreshold = teamFitConfig.getDiversityBonusThreshold();
        double saturationPenaltyThreshold = teamFitConfig.getSaturationPenaltyThreshold();
        double diversityBonus = teamFitConfig.getDiversityBonus();
        double saturationPenalty = teamFitConfig.getSaturationPenalty();

        if (diversityRatio > diversityBonusThreshold && saturationRatio < (1.0 - diversityBonusThreshold)) {
            teamFitMultiplier = diversityBonus; // Bonus for good balance
        } else if (saturationRatio > saturationPenaltyThreshold) {
            teamFitMultiplier = saturationPenalty; // Penalty for too much overlap
        }

        double adjustedPercentage = overallPercentage * teamFitMultiplier;

        log.info("Team Fit score calculated: {} (adjusted: {}%), diversity: {}%, saturation: {}%",
            String.format("%.2f", overallScore),
            String.format("%.2f", adjustedPercentage),
            String.format("%.2f", diversityRatio * 100),
            String.format("%.2f", saturationRatio * 100));

        // Step 5: Create Result
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
        // This is determined by having good scores with reasonable diversity contribution
        double passThreshold = teamFitConfig.getPassThreshold() * 100.0; // Convert to percentage
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
