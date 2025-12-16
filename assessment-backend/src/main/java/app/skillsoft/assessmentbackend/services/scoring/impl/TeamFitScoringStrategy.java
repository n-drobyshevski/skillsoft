package app.skillsoft.assessmentbackend.services.scoring.impl;

import app.skillsoft.assessmentbackend.domain.dto.CompetencyScoreDto;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.TeamFitBlueprint;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.TestBlueprintDto;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.repository.CompetencyRepository;
import app.skillsoft.assessmentbackend.services.scoring.ScoringResult;
import app.skillsoft.assessmentbackend.services.scoring.ScoringStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
public class TeamFitScoringStrategy implements ScoringStrategy {

    private static final Logger log = LoggerFactory.getLogger(TeamFitScoringStrategy.class);

    private final CompetencyRepository competencyRepository;

    public TeamFitScoringStrategy(CompetencyRepository competencyRepository) {
        this.competencyRepository = competencyRepository;
    }

    @Override
    public ScoringResult calculate(TestSession session, List<TestAnswer> answers) {
        log.info("Calculating Scenario C (Team Fit) score for session: {}", session.getId());

        TestTemplate template = session.getTemplate();
        TeamFitBlueprint blueprint = extractTeamFitBlueprint(template);

        UUID teamId = blueprint != null ? blueprint.getTeamId() : null;
        double saturationThreshold = blueprint != null ? blueprint.getSaturationThreshold() : 0.75;

        log.debug("Team Fit parameters - Team ID: {}, Saturation Threshold: {}", teamId, saturationThreshold);

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

            Competency competency = answer.getQuestion()
                .getBehavioralIndicator()
                .getCompetency();

            UUID compId = competency.getId();
            double normalizedScore = normalize(answer);

            rawCompetencyScores.merge(compId, normalizedScore, Double::sum);
            counts.merge(compId, 1, Integer::sum);
            questionsAnswered.merge(compId, 1, Integer::sum);

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

            // Get competency details
            Competency competency = competencyRepository.findById(competencyId).orElse(null);
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
            if (average >= saturationThreshold) {
                saturationContributors++;
            } else if (average >= 0.5) {
                diversityContributors++;
            }

            // Weight based on ESCO mapping (standardized skills are more valuable for team analysis)
            double weight = (escoUri != null && !escoUri.isEmpty()) ? 1.15 : 1.0;

            // Additional weight for competencies with Big Five mapping (personality compatibility)
            String bigFive = competency != null ? competency.getBigFiveCategory() : null;
            if (bigFive != null) {
                weight *= 1.1;
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

        // Step 4: Calculate Overall Team Fit Score
        // Team Fit score considers:
        // 1. Overall competency performance
        // 2. Diversity vs saturation balance
        // 3. Personality profile completeness

        double totalWeight = competencyCount > 0
            ? finalScores.stream()
                .mapToDouble(s -> {
                    double w = 1.0;
                    if (s.getOnetCode() != null && !s.getOnetCode().isEmpty()) w *= 1.15;
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
        if (diversityRatio > 0.4 && saturationRatio < 0.6) {
            teamFitMultiplier = 1.1; // Bonus for good balance
        } else if (saturationRatio > 0.8) {
            teamFitMultiplier = 0.9; // Penalty for too much overlap
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

        // For Team Fit, "pass" means the candidate would add value to the team
        // This is determined by having good scores with reasonable diversity contribution
        boolean addsTeamValue = adjustedPercentage >= 60.0 && diversityRatio >= 0.3;
        result.setPassed(addsTeamValue);

        log.info("Team Fit assessment: {} (score: {}%, diversity: {}%)",
            addsTeamValue ? "ADDS VALUE" : "LIMITED FIT",
            String.format("%.2f", adjustedPercentage),
            String.format("%.2f", diversityRatio * 100));

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

    /**
     * Normalize raw answer score to 0-1 scale.
     *
     * Handles different question types:
     * - Likert (1-5): (value - 1) / 4
     * - SJT: pre-calculated weight (0-1)
     * - Multiple Choice: correct=1, incorrect=0
     *
     * @param answer The test answer to normalize
     * @return Normalized score (0-1)
     */
    private double normalize(TestAnswer answer) {
        // Likert scale normalization (1-5 -> 0-1)
        if (answer.getLikertValue() != null) {
            int likert = answer.getLikertValue();
            // Clamp to valid range
            likert = Math.max(1, Math.min(5, likert));
            return (likert - 1.0) / 4.0;
        }

        // SJT weight normalization (already 0-1, but check score field)
        if (answer.getScore() != null) {
            double score = answer.getScore();
            // Ensure in range [0, 1]
            return Math.max(0.0, Math.min(1.0, score));
        }

        // Multiple choice: correct=1, incorrect=0
        if (answer.getQuestion().getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
            return answer.getScore() != null ? answer.getScore() : 0.0;
        }

        // Default to 0 if no score available
        log.warn("No score available for answer {}, defaulting to 0", answer.getId());
        return 0.0;
    }

    @Override
    public AssessmentGoal getSupportedGoal() {
        return AssessmentGoal.TEAM_FIT;
    }
}
