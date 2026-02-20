package app.skillsoft.assessmentbackend.services.scoring.impl;

import app.skillsoft.assessmentbackend.config.ScoringConfiguration;
import app.skillsoft.assessmentbackend.domain.dto.CompetencyScoreDto;
import app.skillsoft.assessmentbackend.domain.dto.IndicatorScoreDto;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.services.scoring.CompetencyAggregationService;
import app.skillsoft.assessmentbackend.services.scoring.ScoreInterpreter;
import app.skillsoft.assessmentbackend.services.scoring.ScoringPrecision;
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

    private final CompetencyAggregationService aggregationService;
    private final ScoringConfiguration config;
    private final ScoreInterpreter scoreInterpreter;

    public OverviewScoringStrategy(CompetencyAggregationService aggregationService,
                                   ScoringConfiguration config,
                                   ScoreInterpreter scoreInterpreter) {
        this.aggregationService = aggregationService;
        this.config = config;
        this.scoreInterpreter = scoreInterpreter;
    }

    @Override
    public ScoringResult calculate(TestSession session, List<TestAnswer> answers) {
        // Set session context for all scoring log messages
        LoggingContext.setSessionId(session.getId());
        LoggingContext.setOperation("overview-scoring");

        log.info("Calculating Scenario A (Overview) score with indicator breakdown: session={} answers={} user={}",
                session.getId(), answers.size(), session.getClerkUserId());

        // Steps 1-3: Shared aggregation pipeline (normalize → indicator → competency → DTOs)
        CompetencyAggregationService.AggregationResult aggResult = aggregationService.aggregate(answers);
        List<CompetencyScoreDto> finalScores = aggResult.competencyScores();

        // Step 4: Evidence sufficiency check
        int minQ = config.getThresholds().getOverview().getMinQuestionsPerCompetency();
        aggregationService.applyEvidenceSufficiency(finalScores, minQ);

        // Step 5: Calculate Overall Score (weighted by question count + evidence sufficiency)
        int competencyCount = finalScores.size();
        double lowEvidenceWeight = config.getThresholds().getOverview().getLowEvidenceWeightFactor();

        double weightedPercentageSum = 0.0;
        double weightedScoreSum = 0.0;
        double totalWeight = 0.0;

        for (CompetencyScoreDto cs : finalScores) {
            // Base weight = number of questions answered for this competency
            double weight = Math.max(cs.getQuestionsAnswered(), 1);

            // Reduce weight for competencies with insufficient evidence
            if (Boolean.TRUE.equals(cs.getInsufficientEvidence())) {
                weight *= lowEvidenceWeight;
            }

            weightedPercentageSum += cs.getPercentage() * weight;
            weightedScoreSum += cs.getScore() * weight;
            totalWeight += weight;
        }

        double overallPercentage = totalWeight > 0 ? weightedPercentageSum / totalWeight : 0.0;
        double overallScore = totalWeight > 0 ? weightedScoreSum / totalWeight : 0.0;

        log.info("Overall score calculated: {} ({}/100) with {} competencies, {} indicators (weighted, lowEvidenceFactor={})",
                String.format("%.2f", overallScore), String.format("%.2f", overallPercentage),
                competencyCount, aggResult.indicatorAggregations().size(), lowEvidenceWeight);

        // Step 6: Profile pattern analysis and proficiency labels
        ScoringConfiguration.Thresholds.Overview overviewConfig = config.getThresholds().getOverview();
        Map<String, List<String>> profilePattern = new LinkedHashMap<>();
        profilePattern.put("SIGNATURE_STRENGTH", new ArrayList<>());
        profilePattern.put("STRENGTH", new ArrayList<>());
        profilePattern.put("DEVELOPING", new ArrayList<>());
        profilePattern.put("CRITICAL_GAP", new ArrayList<>());
        profilePattern.put("AVERAGE", new ArrayList<>());

        for (CompetencyScoreDto cs : finalScores) {
            double pct = cs.getPercentage() != null ? cs.getPercentage() : 0.0;

            // Assign proficiency label to competency
            ScoreInterpreter.ScoreInterpretation interpretation = scoreInterpreter.interpret(pct, "en");
            cs.setProficiencyLabel(interpretation.label());

            // Assign proficiency labels to indicators
            if (cs.getIndicatorScores() != null) {
                for (IndicatorScoreDto is : cs.getIndicatorScores()) {
                    double indPct = is.getPercentage() != null ? is.getPercentage() : 0.0;
                    ScoreInterpreter.ScoreInterpretation indInterpretation = scoreInterpreter.interpret(indPct, "en");
                    is.setProficiencyLabel(indInterpretation.label());
                }
            }

            // Classify into profile pattern categories
            // Round both sides to 4dp to eliminate IEEE 754 floating-point boundary errors
            double roundedPct = ScoringPrecision.round4(pct);
            String category;
            if (roundedPct >= ScoringPrecision.round4(overallPercentage + overviewConfig.getProfileBandWidth())
                    && roundedPct >= ScoringPrecision.round4(overviewConfig.getStrengthThreshold())) {
                category = "SIGNATURE_STRENGTH";
            } else if (roundedPct >= ScoringPrecision.round4(overviewConfig.getStrengthThreshold())) {
                category = "STRENGTH";
            } else if (roundedPct < ScoringPrecision.round4(overviewConfig.getCriticalGapThreshold())) {
                category = "CRITICAL_GAP";
            } else if (roundedPct >= ScoringPrecision.round4(overviewConfig.getDevelopmentThreshold())) {
                category = "DEVELOPING";
            } else {
                category = "AVERAGE";
            }

            profilePattern.get(category).add(cs.getCompetencyName());
            log.debug("Profile pattern: {} -> {} (pct={}, overall={})",
                    cs.getCompetencyName(), category,
                    String.format("%.1f", pct), String.format("%.1f", overallPercentage));
        }

        // Remove empty categories for cleaner output
        profilePattern.values().removeIf(List::isEmpty);

        // Step 7: Create Result
        ScoringResult result = new ScoringResult();
        result.setOverallScore(overallScore);
        result.setOverallPercentage(overallPercentage);
        result.setCompetencyScores(finalScores);
        result.setGoal(AssessmentGoal.OVERVIEW);

        // Store profile pattern in extended metrics
        Map<String, Object> extendedMetrics = new LinkedHashMap<>();
        extendedMetrics.put("profilePattern", profilePattern);
        result.setExtendedMetrics(extendedMetrics);

        log.info("Profile pattern analysis complete: {}", profilePattern.keySet());

        // Pass/fail will be set by service layer based on template passing score

        return result;
    }

    @Override
    public AssessmentGoal getSupportedGoal() {
        return AssessmentGoal.OVERVIEW;
    }
}
