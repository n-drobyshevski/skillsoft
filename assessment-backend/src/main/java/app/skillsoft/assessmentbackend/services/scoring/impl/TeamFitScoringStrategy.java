package app.skillsoft.assessmentbackend.services.scoring.impl;

import app.skillsoft.assessmentbackend.config.ScoringConfiguration;
import app.skillsoft.assessmentbackend.domain.dto.CompetencyScoreDto;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.TeamFitBlueprint;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.TestBlueprintDto;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.services.external.TeamService;
import app.skillsoft.assessmentbackend.services.scoring.CompetencyAggregation;
import app.skillsoft.assessmentbackend.services.scoring.CompetencyAggregationService;
import app.skillsoft.assessmentbackend.services.scoring.CompetencyBatchLoader;
import app.skillsoft.assessmentbackend.services.scoring.IndicatorAggregation;
import app.skillsoft.assessmentbackend.services.scoring.IndicatorBatchLoader;
import app.skillsoft.assessmentbackend.services.scoring.ScoreNormalizer;
import app.skillsoft.assessmentbackend.services.scoring.ScoringPrecision;
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

    private final CompetencyAggregationService aggregationService;
    private final CompetencyBatchLoader competencyBatchLoader;
    private final IndicatorBatchLoader indicatorBatchLoader;
    private final ScoringConfiguration scoringConfig;
    private final ScoreNormalizer scoreNormalizer;
    private final TeamService teamService;

    public TeamFitScoringStrategy(
            CompetencyAggregationService aggregationService,
            CompetencyBatchLoader competencyBatchLoader,
            IndicatorBatchLoader indicatorBatchLoader,
            ScoringConfiguration scoringConfig,
            ScoreNormalizer scoreNormalizer,
            TeamService teamService) {
        this.aggregationService = aggregationService;
        this.competencyBatchLoader = competencyBatchLoader;
        this.indicatorBatchLoader = indicatorBatchLoader;
        this.scoringConfig = scoringConfig;
        this.scoreNormalizer = scoreNormalizer;
        this.teamService = teamService;
    }

    @Override
    public ScoringResult calculate(TestSession session, List<TestAnswer> answers) {
        log.info("Calculating Scenario C (Team Fit) score with indicator breakdown for session: {}", session.getId());

        TestTemplate template = session.getTemplate();
        TeamFitBlueprint blueprint = extractTeamFitBlueprint(template);

        UUID teamId = blueprint != null ? blueprint.getTeamId() : null;
        Map<UUID, Double> roleWeights = (blueprint != null && blueprint.getRoleCompetencyWeights() != null)
                ? blueprint.getRoleCompetencyWeights()
                : Collections.emptyMap();
        String targetRole = blueprint != null ? blueprint.getTargetRole() : null;

        if (targetRole != null) {
            log.debug("Role context: targetRole={}, roleWeights={}", targetRole, roleWeights.size());
        }

        // Get Team Fit configuration
        ScoringConfiguration.Thresholds.TeamFit teamFitConfig = scoringConfig.getThresholds().getTeamFit();
        ScoringConfiguration.Weights weights = scoringConfig.getWeights();

        // Saturation threshold can be overridden by blueprint, otherwise use config default
        double saturationThreshold = blueprint != null
                ? blueprint.getSaturationThreshold()
                : teamFitConfig.getSaturationThreshold();

        log.debug("Team Fit parameters - Team ID: {}, Saturation Threshold: {}", teamId, saturationThreshold);

        // Fetch real team profile for saturation comparison
        Map<UUID, Double> teamCompetencySaturation = Collections.emptyMap();
        Map<String, Double> teamAveragePersonality = Collections.emptyMap();
        int teamSize = 0;
        if (teamId != null) {
            var teamProfileOpt = teamService.getTeamProfile(teamId);
            if (teamProfileOpt.isPresent()) {
                var teamProfile = teamProfileOpt.get();
                teamCompetencySaturation = teamProfile.competencySaturation();
                teamAveragePersonality = teamProfile.averagePersonality() != null
                        ? teamProfile.averagePersonality() : Collections.emptyMap();
                teamSize = teamProfile.members().size();
                log.debug("Loaded team profile: {} members, {} competency saturations, {} personality traits",
                        teamSize, teamCompetencySaturation.size(), teamAveragePersonality.size());
            } else {
                log.warn("Team profile not found for team: {}, falling back to self-referential scoring", teamId);
            }
        }

        // Batch load all competencies and indicators upfront to prevent N+1 queries
        Map<UUID, Competency> competencyCache = competencyBatchLoader.loadCompetenciesForAnswers(answers);
        Map<UUID, BehavioralIndicator> indicatorCache = indicatorBatchLoader.loadIndicatorsForAnswers(answers);

        // Step 1: Normalize & Aggregate Scores by Indicator (first level)
        // TeamFit keeps its own normalization loop to track Big Five and ESCO mappings
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

        // Step 2: Delegate indicator-to-competency rollup and DTO building to shared service
        Map<UUID, CompetencyAggregation> competencyAggs = aggregationService.rollUpIndicatorsToCompetencies(
                indicatorAggs, indicatorCache);
        List<CompetencyScoreDto> finalScores = aggregationService.buildCompetencyScores(
                competencyAggs, competencyCache);

        // Step 3: Team Fit-specific DTO enrichment and weighting
        double totalWeightedScore = 0.0;

        // Track competencies that contribute to team diversity vs saturation
        int diversityContributors = 0;
        int saturationContributors = 0;

        // Track per-competency saturation: competencyName -> candidate percentage (0-1 scale)
        Map<String, Double> competencySaturationMap = new HashMap<>();

        for (CompetencyScoreDto scoreDto : finalScores) {
            UUID competencyId = scoreDto.getCompetencyId();
            CompetencyAggregation compAgg = competencyAggs.get(competencyId);

            // Enrich with team-fit specific fields from competency entity
            Competency competency = competencyCache.get(competencyId);
            String competencyName = scoreDto.getCompetencyName();
            String onetCode = competency != null ? competency.getOnetCode() : null;
            String escoUri = competency != null ? competency.getEscoUri() : null;

            double percentage = scoreDto.getPercentage();
            double average = percentage / 100.0;

            // For Team Fit, track questions that demonstrate competency
            int questionsCorrect = (int) Math.round(average * scoreDto.getQuestionsAnswered());
            scoreDto.setQuestionsCorrect(questionsCorrect);
            scoreDto.setEscoUri(escoUri);
            String bigFive = competency != null ? competency.getBigFiveCategory() : null;
            scoreDto.setBigFiveCategory(bigFive);

            // Record candidate percentage on 0-1 scale for radar chart display
            competencySaturationMap.put(competencyName, average);

            // Determine if this competency contributes to team diversity or saturation
            double diversityThreshold = teamFitConfig.getDiversityThreshold();

            // Classify based on real team data when available, fallback to self-referential
            // Round both sides to 4dp to eliminate IEEE 754 floating-point boundary errors
            if (!teamCompetencySaturation.isEmpty() && competencyId != null) {
                // Look up team saturation for this competency
                Double teamSat = teamCompetencySaturation.get(competencyId);
                if (teamSat != null) {
                    if (ScoringPrecision.meetsThreshold(teamSat, saturationThreshold)) {
                        // Team already has this skill covered
                        saturationContributors++;
                    } else if (ScoringPrecision.meetsThreshold(teamSat, diversityThreshold)) {
                        // Team has some coverage, candidate adds diversity
                        diversityContributors++;
                    }
                    // else: gap (team lacks this skill entirely)
                }
                // Competency not in team profile - treat as gap (candidate brings new skill)
                // Don't increment either counter - falls through to gap calculation
            } else {
                // Fallback: self-referential classification (no team data available)
                if (ScoringPrecision.meetsThreshold(average, saturationThreshold)) {
                    saturationContributors++;
                } else if (ScoringPrecision.meetsThreshold(average, diversityThreshold)) {
                    diversityContributors++;
                }
            }

            // Weight based on ESCO mapping
            double weight = (escoUri != null && !escoUri.isEmpty()) ? weights.getEscoBoost() : 1.0;

            // Additional weight for competencies with Big Five mapping
            if (bigFive != null) {
                weight *= weights.getBigFiveBoost();
            }

            // Gap relevance weight: competencies filling deeper team gaps get higher weight
            // Formula: 1.0 + (1.0 - teamSaturation) -> range [1.0, 2.0]
            // A competency with 0% team saturation gets 2.0x weight (most critical gap)
            // A competency with 100% team saturation gets 1.0x weight (no gap)
            if (!teamCompetencySaturation.isEmpty() && competencyId != null) {
                Double teamSat = teamCompetencySaturation.get(competencyId);
                if (teamSat != null) {
                    double gapRelevanceWeight = 1.0 + (1.0 - teamSat);
                    weight *= gapRelevanceWeight;
                }
                // If competencyId not in team profile, no gap relevance adjustment (weight stays as-is)
            }

            // Role-based weight: emphasize competencies critical for the target role
            if (!roleWeights.isEmpty() && competencyId != null) {
                Double roleWeight = roleWeights.get(competencyId);
                if (roleWeight != null) {
                    weight *= roleWeight;
                }
            }

            // Cap maximum weight multiplier to prevent single-competency domination
            // After all boosts are compounded (ESCO + Big Five + gap relevance + role weight),
            // cap the final weight to the configured maximum
            double maxWeightMultiplier = weights.getMaxWeightMultiplier();
            weight = Math.min(weight, maxWeightMultiplier);

            totalWeightedScore += (percentage * weight);

            log.debug("Competency {} (ESCO: {}, Big Five: {}): {} indicators, score {}%, contribution: {}",
                    competencyName, escoUri, bigFive, scoreDto.getIndicatorScores().size(),
                    String.format("%.2f", percentage),
                    ScoringPrecision.meetsThreshold(average, saturationThreshold) ? "SATURATION"
                            : (ScoringPrecision.meetsThreshold(average, diversityThreshold) ? "DIVERSITY" : "GAP"));
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
        double bigFiveBoostForWeighting = weights.getBigFiveBoost();
        // Build final references for use in lambda
        final Map<UUID, Double> teamSatForDenominator = teamCompetencySaturation;
        final Map<UUID, Double> roleWeightsForDenominator = roleWeights;

        // Apply same max weight cap in denominator for symmetry
        final double maxWeightCap = weights.getMaxWeightMultiplier();

        double totalWeight = competencyCount > 0
                ? finalScores.stream()
                .mapToDouble(s -> {
                    double w = 1.0;
                    if (s.getEscoUri() != null && !s.getEscoUri().isEmpty()) w *= escoBoostForWeighting;
                    if (s.getBigFiveCategory() != null && !s.getBigFiveCategory().isEmpty()) w *= bigFiveBoostForWeighting;
                    // Apply same gap relevance weight in denominator for symmetry
                    if (!teamSatForDenominator.isEmpty() && s.getCompetencyId() != null) {
                        Double teamSat = teamSatForDenominator.get(s.getCompetencyId());
                        if (teamSat != null) {
                            w *= 1.0 + (1.0 - teamSat);
                        }
                    }
                    // Apply same role weight in denominator for symmetry
                    if (!roleWeightsForDenominator.isEmpty() && s.getCompetencyId() != null) {
                        Double roleW = roleWeightsForDenominator.get(s.getCompetencyId());
                        if (roleW != null) {
                            w *= roleW;
                        }
                    }
                    // Cap weight in denominator matching numerator cap
                    return Math.min(w, maxWeightCap);
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

        // Adjust score based on diversity/saturation balance using continuous sigmoid
        double teamFitMultiplier = calculateTeamFitMultiplier(diversityRatio, saturationRatio, teamFitConfig);

        log.debug("Sigmoid multiplier: balance={}, multiplier={}",
                String.format("%.3f", diversityRatio - saturationRatio),
                String.format("%.4f", teamFitMultiplier));

        // Step 5b: Personality compatibility adjustment
        Double personalityCompatibility = null;
        if (!bigFiveAverages.isEmpty() && !teamAveragePersonality.isEmpty()) {
            personalityCompatibility = calculatePersonalityCompatibility(bigFiveAverages, teamAveragePersonality);

            // Apply as additive multiplier adjustment
            double personalityAdjustment = (personalityCompatibility - 0.5) * teamFitConfig.getPersonalityWeight();
            teamFitMultiplier += personalityAdjustment;

            log.debug("Personality compatibility: {}, adjustment: {}, adjusted multiplier: {}",
                    String.format("%.3f", personalityCompatibility),
                    String.format("%.4f", personalityAdjustment),
                    String.format("%.4f", teamFitMultiplier));
        }

        // Clamp multiplier to [0.8, 1.2] to prevent extreme score distortions
        // from combined sigmoid + personality adjustments
        teamFitMultiplier = Math.max(0.8, Math.min(1.2, teamFitMultiplier));

        double adjustedPercentage = overallPercentage * teamFitMultiplier;

        // Clamp final adjusted percentage to valid [0.0, 100.0] range
        adjustedPercentage = Math.max(0.0, Math.min(100.0, adjustedPercentage));

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
                .competencySaturation(competencySaturationMap)
                .teamSize(teamSize)
                .personalityCompatibility(personalityCompatibility)
                .build();
        result.setTeamFitMetrics(teamFitMetrics);

        // For Team Fit, "pass" means the candidate would add value to the team
        // Adaptive threshold: lower for small teams or teams with severe gaps
        double basePassThreshold = teamFitConfig.getPassThreshold();
        double adjustedPassThreshold = basePassThreshold;

        // Small team adjustment: smaller teams benefit more from any new member
        if (teamSize > 0 && teamSize < teamFitConfig.getSmallTeamThreshold()) {
            adjustedPassThreshold -= teamFitConfig.getSmallTeamAdjustment();
            log.debug("Small team adjustment applied: threshold reduced by {} (team size: {})",
                    teamFitConfig.getSmallTeamAdjustment(), teamSize);
        }

        // Severe gap adjustment: teams with many gaps need help urgently
        double gapRatio = competencyCount > 0 ? (double) gapContributors / competencyCount : 0.0;
        if (gapRatio > 0.5) {
            adjustedPassThreshold -= teamFitConfig.getSevereGapAdjustment();
            log.debug("Severe gap adjustment applied: threshold reduced by {} (gap ratio: {}%)",
                    teamFitConfig.getSevereGapAdjustment(), String.format("%.1f", gapRatio * 100));
        }

        // Floor: never go below minimum threshold
        adjustedPassThreshold = Math.max(adjustedPassThreshold, teamFitConfig.getMinPassThreshold());

        double passThreshold = adjustedPassThreshold * 100.0;
        double minDiversityRatio = teamFitConfig.getMinDiversityRatio();
        // Round both sides to 4dp to eliminate IEEE 754 floating-point boundary errors
        boolean addsTeamValue = ScoringPrecision.meetsThreshold(adjustedPercentage, passThreshold)
                && ScoringPrecision.meetsThreshold(diversityRatio, minDiversityRatio);
        result.setPassed(addsTeamValue);

        log.debug("Pass determination: adjusted threshold={}%, score={}%, diversity={}%, result={}",
                String.format("%.1f", passThreshold),
                String.format("%.1f", adjustedPercentage),
                String.format("%.1f", diversityRatio * 100),
                addsTeamValue ? "PASS" : "FAIL");

        log.info("Team Fit assessment: {} (score: {}%, diversity: {}%, Big Five traits: {})",
                addsTeamValue ? "ADDS VALUE" : "LIMITED FIT",
                String.format("%.2f", adjustedPercentage),
                String.format("%.2f", diversityRatio * 100),
                bigFiveAverages.size());

        return result;
    }

    /**
     * Calculate personality compatibility between candidate and team using normalized Euclidean distance.
     * <p>
     * Candidate Big Five keys use the "BIG_FIVE_" prefix (e.g., "BIG_FIVE_OPENNESS") from
     * {@link app.skillsoft.assessmentbackend.domain.entities.Competency#getBigFiveCategory()},
     * while team averagePersonality uses plain trait names (e.g., "OPENNESS").
     * This method normalizes both to plain trait names before comparison.
     *
     * @param candidateProfile candidate's Big Five scores (0-100 scale), keys may be prefixed with "BIG_FIVE_"
     * @param teamProfile team's average Big Five scores (0-100 scale), plain trait name keys
     * @return compatibility score (0.0-1.0), where 1.0 = perfectly compatible
     */
    private double calculatePersonalityCompatibility(Map<String, Double> candidateProfile,
                                                      Map<String, Double> teamProfile) {
        // Normalize candidate keys: strip "BIG_FIVE_" prefix for comparison with team profile
        Map<String, Double> normalizedCandidate = new HashMap<>();
        for (var entry : candidateProfile.entrySet()) {
            String key = entry.getKey();
            String normalizedKey = key.startsWith("BIG_FIVE_") ? key.substring("BIG_FIVE_".length()) : key;
            normalizedCandidate.put(normalizedKey, entry.getValue());
        }

        // Find common traits
        Set<String> commonTraits = new HashSet<>(normalizedCandidate.keySet());
        commonTraits.retainAll(teamProfile.keySet());

        if (commonTraits.isEmpty()) {
            return 0.5; // neutral when no common traits
        }

        // Calculate Euclidean distance (scores are 0-100)
        double sumSquared = 0.0;
        for (String trait : commonTraits) {
            double diff = normalizedCandidate.get(trait) - teamProfile.get(trait);
            sumSquared += diff * diff;
        }

        // Max possible distance = sqrt(n * 100^2) where n = number of traits
        double maxDistance = Math.sqrt(commonTraits.size() * 10000.0);
        double distance = Math.sqrt(sumSquared);

        // Convert distance to compatibility (0=far, 1=identical)
        return 1.0 - (distance / maxDistance);
    }

    /**
     * Calculate team fit multiplier using a continuous sigmoid function.
     *
     * Maps the diversity-saturation balance to a smooth curve between
     * the penalty and bonus multiplier values, eliminating arbitrary thresholds.
     *
     * @param diversityRatio ratio of diverse competencies (0-1)
     * @param saturationRatio ratio of saturated competencies (0-1)
     * @param teamFitConfig configuration with bonus, penalty, and steepness values
     * @return multiplier in range [saturationPenalty, diversityBonus]
     */
    private double calculateTeamFitMultiplier(double diversityRatio, double saturationRatio,
            ScoringConfiguration.Thresholds.TeamFit teamFitConfig) {
        double balance = diversityRatio - saturationRatio; // range [-1, 1]
        double steepness = teamFitConfig.getSigmoidSteepness();
        double penalty = teamFitConfig.getSaturationPenalty();
        double bonus = teamFitConfig.getDiversityBonus();

        double sigmoid = 1.0 / (1.0 + Math.exp(-steepness * balance));
        return penalty + (bonus - penalty) * sigmoid;
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

            Object targetRoleObj = blueprint.get("targetRole");
            if (targetRoleObj instanceof String) {
                teamFitBlueprint.setTargetRole((String) targetRoleObj);
            }

            Object roleWeightsObj = blueprint.get("roleCompetencyWeights");
            if (roleWeightsObj instanceof Map) {
                Map<UUID, Double> weights = new HashMap<>();
                ((Map<?, ?>) roleWeightsObj).forEach((key, value) -> {
                    try {
                        UUID compId = UUID.fromString(key.toString());
                        double w = value instanceof Number ? ((Number) value).doubleValue() : 1.0;
                        weights.put(compId, Math.max(0.5, Math.min(2.0, w))); // clamp to valid range
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid competency ID in roleCompetencyWeights: {}", key);
                    }
                });
                if (!weights.isEmpty()) {
                    teamFitBlueprint.setRoleCompetencyWeights(weights);
                }
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
