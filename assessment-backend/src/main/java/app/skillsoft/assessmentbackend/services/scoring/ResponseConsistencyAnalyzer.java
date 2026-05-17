package app.skillsoft.assessmentbackend.services.scoring;

import app.skillsoft.assessmentbackend.domain.entities.AssessmentQuestion;
import app.skillsoft.assessmentbackend.domain.entities.BehavioralIndicator;
import app.skillsoft.assessmentbackend.domain.entities.Competency;
import app.skillsoft.assessmentbackend.domain.entities.TestAnswer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Analyzes response consistency patterns to detect potential disengagement,
 * straight-lining, speed anomalies, and random answering.
 *
 * Produces a composite consistency score (0.0-1.0) and human-readable flags
 * that are stored in extendedMetrics for all assessment goals.
 *
 * @see ScoreNormalizer used for consistent answer normalization
 */
@Service
public class ResponseConsistencyAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(ResponseConsistencyAnalyzer.class);

    private final ScoreNormalizer scoreNormalizer;

    public ResponseConsistencyAnalyzer(ScoreNormalizer scoreNormalizer) {
        this.scoreNormalizer = scoreNormalizer;
    }

    /**
     * Result of consistency analysis across all answers in a session.
     */
    public record ConsistencyResult(
        double consistencyScore,        // 0.0-1.0 overall
        List<String> flags,             // Human-readable flag descriptions
        double speedAnomalyRate,        // 0.0-1.0 proportion of too-fast answers
        double straightLiningRate,      // 0.0-1.0 Likert same-value rate
        double intraCompetencyVariance  // Average variance within competencies
    ) {}

    /**
     * Minimum seconds expected to read and answer a question.
     * Answers faster than this are flagged as speed anomalies.
     */
    private static final int MIN_RESPONSE_TIME_SECONDS = 3;

    /**
     * Maximum proportion of Likert answers with the same value before flagging.
     */
    private static final double STRAIGHT_LINING_THRESHOLD = 0.70;

    /**
     * Minimum number of answers in a competency group to compute variance.
     */
    private static final int MIN_ANSWERS_FOR_VARIANCE = 3;

    /**
     * Analyze response consistency for a list of test answers.
     *
     * @param answers All answers from a test session
     * @return ConsistencyResult with composite score and flags
     */
    public ConsistencyResult analyze(List<TestAnswer> answers) {
        if (answers == null || answers.isEmpty()) {
            return new ConsistencyResult(1.0, List.of(), 0.0, 0.0, 0.0);
        }

        double speedAnomalyRate = calculateSpeedAnomalyRate(answers);
        double straightLiningRate = calculateStraightLiningRate(answers);
        double avgVariance = calculateIntraCompetencyVariance(answers);

        // Calculate overall consistency score as a weighted combination
        double speedFactor = 1.0 - speedAnomalyRate;
        double straightLiningFactor = 1.0 - straightLiningRate;
        double varianceFactor = calculateVarianceFactor(avgVariance);

        double consistencyScore = (speedFactor * 0.3)
                                + (straightLiningFactor * 0.3)
                                + (varianceFactor * 0.4);
        consistencyScore = Math.round(consistencyScore * 100.0) / 100.0;

        // Build human-readable flags
        List<String> flags = buildFlags(answers, speedAnomalyRate, straightLiningRate, avgVariance);

        log.debug("Consistency analysis: score={}, speedAnomaly={}, straightLining={}, avgVariance={}, flags={}",
            String.format("%.2f", consistencyScore),
            String.format("%.2f", speedAnomalyRate),
            String.format("%.2f", straightLiningRate),
            String.format("%.4f", avgVariance),
            flags.size());

        return new ConsistencyResult(
            consistencyScore,
            flags,
            speedAnomalyRate,
            straightLiningRate,
            avgVariance
        );
    }

    /**
     * Calculate the proportion of non-skipped answers completed in under MIN_RESPONSE_TIME_SECONDS.
     */
    private double calculateSpeedAnomalyRate(List<TestAnswer> answers) {
        List<TestAnswer> answered = answers.stream()
            .filter(a -> !Boolean.TRUE.equals(a.getIsSkipped()) && a.getAnsweredAt() != null)
            .toList();

        if (answered.isEmpty()) {
            return 0.0;
        }

        long speedAnomalyCount = answered.stream()
            .filter(a -> a.getTimeSpentSeconds() != null && a.getTimeSpentSeconds() < MIN_RESPONSE_TIME_SECONDS)
            .count();

        return (double) speedAnomalyCount / answered.size();
    }

    /**
     * Among answers with non-null likertValue, find the most common value.
     * If its frequency / total Likert answers > STRAIGHT_LINING_THRESHOLD, return that rate.
     */
    private double calculateStraightLiningRate(List<TestAnswer> answers) {
        List<Integer> likertValues = answers.stream()
            .filter(a -> a.getLikertValue() != null)
            .map(TestAnswer::getLikertValue)
            .toList();

        if (likertValues.isEmpty()) {
            return 0.0;
        }

        // Find the most common Likert value
        Map<Integer, Long> frequencyMap = likertValues.stream()
            .collect(Collectors.groupingBy(v -> v, Collectors.counting()));

        long maxFrequency = frequencyMap.values().stream()
            .max(Long::compareTo)
            .orElse(0L);

        return (double) maxFrequency / likertValues.size();
    }

    /**
     * Group answers by competency (via question -> indicator -> competency chain).
     * For each competency group with MIN_ANSWERS_FOR_VARIANCE+ answers, compute
     * the variance of normalized scores. Average across competencies.
     */
    private double calculateIntraCompetencyVariance(List<TestAnswer> answers) {
        // Group answered (non-skipped) answers by competency UUID
        // Filter matches OverviewScoringStrategy logic: skip when isSkipped or answeredAt is null
        Map<UUID, List<Double>> competencyScores = new HashMap<>();

        for (TestAnswer answer : answers) {
            if (answer.getIsSkipped() || answer.getAnsweredAt() == null) {
                continue;
            }

            UUID competencyId = extractCompetencyId(answer);
            if (competencyId == null) {
                continue;
            }

            // Use shared ScoreNormalizer for consistent normalization across strategies
            double normalized = scoreNormalizer.normalize(answer);

            competencyScores.computeIfAbsent(competencyId, k -> new ArrayList<>())
                .add(normalized);
        }

        // Compute variance for each competency group with enough answers
        List<Double> variances = new ArrayList<>();
        for (List<Double> scores : competencyScores.values()) {
            if (scores.size() >= MIN_ANSWERS_FOR_VARIANCE) {
                double variance = computeVariance(scores);
                variances.add(variance);
            }
        }

        if (variances.isEmpty()) {
            return 0.0;
        }

        return variances.stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
    }

    /**
     * Calculate the variance factor for the consistency score.
     * Returns 1.0 for "normal" variance range (0.05-0.4),
     * decreasing toward 0 for very low or very high variance.
     */
    private double calculateVarianceFactor(double avgVariance) {
        // If no variance data available, assume neutral
        if (avgVariance == 0.0) {
            return 0.7;
        }

        // Normal range: 0.05 to 0.4
        if (avgVariance >= 0.05 && avgVariance <= 0.4) {
            return 1.0;
        }

        // Too low variance (possible straight-lining / disengagement)
        if (avgVariance < 0.05) {
            // Linear interpolation from 0.0 (variance=0) to 1.0 (variance=0.05)
            return avgVariance / 0.05;
        }

        // Too high variance (possible random answering)
        // Linear interpolation from 1.0 (variance=0.4) to 0.0 (variance=1.0)
        return Math.max(0.0, 1.0 - ((avgVariance - 0.4) / 0.6));
    }

    /**
     * Build human-readable flag descriptions based on analysis results.
     */
    private List<String> buildFlags(List<TestAnswer> answers,
                                     double speedAnomalyRate,
                                     double straightLiningRate,
                                     double avgVariance) {
        List<String> flags = new ArrayList<>();

        if (speedAnomalyRate > 0.2) {
            long answered = answers.stream()
                .filter(a -> !Boolean.TRUE.equals(a.getIsSkipped()) && a.getAnsweredAt() != null)
                .count();
            long speedAnomalyCount = Math.round(speedAnomalyRate * answered);
            flags.add(String.format(
                "Speed anomaly: %d of %d answers were completed in under %d seconds",
                speedAnomalyCount, answered, MIN_RESPONSE_TIME_SECONDS));
        }

        if (straightLiningRate > STRAIGHT_LINING_THRESHOLD) {
            int pct = (int) Math.round(straightLiningRate * 100);
            flags.add(String.format(
                "Straight-lining detected: %d%% of Likert responses used the same value",
                pct));
        }

        if (avgVariance > 0.0 && avgVariance < 0.02) {
            flags.add("Low response variance suggests possible disengagement");
        }

        if (avgVariance > 0.6) {
            flags.add("High response variance suggests inconsistent engagement");
        }

        return flags;
    }

    /**
     * Extract competency ID from the answer's entity chain:
     * TestAnswer -> AssessmentQuestion -> BehavioralIndicator -> Competency.
     */
    private UUID extractCompetencyId(TestAnswer answer) {
        AssessmentQuestion question = answer.getQuestion();
        if (question == null) {
            return null;
        }

        BehavioralIndicator indicator = question.getBehavioralIndicator();
        if (indicator == null) {
            return null;
        }

        Competency competency = indicator.getCompetency();
        if (competency == null) {
            return null;
        }

        return competency.getId();
    }

    /**
     * Compute the sample variance for a list of double values.
     */
    private double computeVariance(List<Double> values) {
        if (values.size() < 2) {
            return 0.0;
        }

        double mean = values.stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);

        double sumSquaredDiffs = values.stream()
            .mapToDouble(v -> (v - mean) * (v - mean))
            .sum();

        return sumSquaredDiffs / (values.size() - 1);
    }
}
