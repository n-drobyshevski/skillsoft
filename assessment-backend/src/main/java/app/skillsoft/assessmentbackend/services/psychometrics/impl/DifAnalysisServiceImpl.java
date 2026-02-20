package app.skillsoft.assessmentbackend.services.psychometrics.impl;

import app.skillsoft.assessmentbackend.domain.dto.psychometrics.DifAnalysisResult;
import app.skillsoft.assessmentbackend.domain.dto.psychometrics.DifAnalysisResult.DifClassification;
import app.skillsoft.assessmentbackend.domain.dto.psychometrics.DifAnalysisResult.ItemDifResult;
import app.skillsoft.assessmentbackend.repository.CompetencyRepository;
import app.skillsoft.assessmentbackend.repository.TestAnswerRepository;
import app.skillsoft.assessmentbackend.services.psychometrics.DifAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of DIF analysis using the Mantel-Haenszel method.
 * <p>
 * Algorithm steps:
 * <ol>
 *   <li>Build a score matrix from the database (session x question -> normalized score)</li>
 *   <li>Compute total scores per session and stratify into 5 quintiles</li>
 *   <li>For each item and each stratum, build a 2x2 contingency table
 *       (focal/reference x correct/incorrect)</li>
 *   <li>Compute the MH common odds ratio: alpha_MH = sum(A_j * D_j / N_j) / sum(B_j * C_j / N_j)</li>
 *   <li>Convert to ETS delta scale: MH_D-DIF = -2.35 * ln(alpha_MH)</li>
 *   <li>Compute MH chi-square statistic for significance testing</li>
 *   <li>Classify using ETS guidelines: A (negligible), B (moderate), C (large)</li>
 * </ol>
 * <p>
 * Minimum sample requirements:
 * <ul>
 *   <li>100 total respondents (combined focal + reference)</li>
 *   <li>20 respondents per group minimum</li>
 * </ul>
 *
 * @see <a href="https://doi.org/10.1002/j.2333-8504.1986.tb01311.x">Holland & Thayer (1988)</a>
 */
@Service
@Transactional(readOnly = true)
public class DifAnalysisServiceImpl implements DifAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(DifAnalysisServiceImpl.class);

    /** Number of ability strata (quintiles) for MH stratification. */
    private static final int NUM_STRATA = 5;

    /** Minimum total respondents required for meaningful DIF analysis. */
    private static final int MIN_TOTAL_RESPONDENTS = 100;

    /** Minimum respondents per group for meaningful DIF analysis. */
    private static final int MIN_GROUP_SIZE = 20;

    /** Threshold for dichotomizing item scores: score >= 0.5 is "correct". */
    private static final double CORRECT_THRESHOLD = 0.5;

    /** ETS delta conversion constant: -2.35 * ln(alpha_MH). */
    private static final double ETS_DELTA_CONSTANT = -2.35;

    /** ETS Category A/B boundary: |delta| < 1.0 is negligible. */
    private static final double ETS_A_B_BOUNDARY = 1.0;

    /** ETS Category B/C boundary: |delta| >= 1.5 is large. */
    private static final double ETS_B_C_BOUNDARY = 1.5;

    /** Continuity correction constant added to zero cells. */
    private static final double CONTINUITY_CORRECTION = 0.5;

    private static final MathContext MATH_CONTEXT = new MathContext(10, RoundingMode.HALF_UP);
    private static final int SCALE = 4;

    private final TestAnswerRepository testAnswerRepository;
    private final CompetencyRepository competencyRepository;

    public DifAnalysisServiceImpl(
            TestAnswerRepository testAnswerRepository,
            CompetencyRepository competencyRepository) {
        this.testAnswerRepository = testAnswerRepository;
        this.competencyRepository = competencyRepository;
    }

    @Override
    public DifAnalysisResult analyzeCompetency(
            UUID competencyId,
            Set<UUID> focalGroupSessionIds,
            Set<UUID> referenceGroupSessionIds,
            String focalGroupLabel,
            String referenceGroupLabel) {

        // Validate competency exists
        if (!competencyRepository.existsById(competencyId)) {
            throw new IllegalArgumentException("Competency not found: " + competencyId);
        }

        validateGroupSizes(focalGroupSessionIds, referenceGroupSessionIds);

        // Load score matrix from the database for this competency
        List<Object[]> rawMatrix = testAnswerRepository.getScoreMatrixForCompetency(competencyId);

        // Parse into session -> (question -> score) map
        ScoreMatrix matrix = parseScoreMatrix(rawMatrix, focalGroupSessionIds, referenceGroupSessionIds);

        validateSufficientData(matrix);

        List<ItemDifResult> itemResults = computeDifForAllItems(matrix);

        int moderateCount = (int) itemResults.stream()
                .filter(r -> r.classification() == DifClassification.B_MODERATE)
                .count();
        int largeCount = (int) itemResults.stream()
                .filter(r -> r.classification() == DifClassification.C_LARGE)
                .count();

        if (largeCount > 0) {
            logger.warn("DIF analysis for competency {} found {} items with LARGE DIF (Category C). "
                    + "These items should be reviewed for potential bias between '{}' and '{}'.",
                    competencyId, largeCount, focalGroupLabel, referenceGroupLabel);
        }

        return new DifAnalysisResult(
                competencyId,
                focalGroupLabel,
                referenceGroupLabel,
                matrix.focalSessionCount(),
                matrix.referenceSessionCount(),
                itemResults.size(),
                moderateCount,
                largeCount,
                itemResults
        );
    }

    @Override
    public DifAnalysisResult analyzeItems(
            Set<UUID> questionIds,
            Set<UUID> focalGroupSessionIds,
            Set<UUID> referenceGroupSessionIds,
            String focalGroupLabel,
            String referenceGroupLabel) {

        if (questionIds == null || questionIds.isEmpty()) {
            throw new IllegalArgumentException("Question IDs must not be empty");
        }

        validateGroupSizes(focalGroupSessionIds, referenceGroupSessionIds);

        // Load score matrix for specific questions
        List<Object[]> rawMatrix = testAnswerRepository.getScoreMatrixForQuestions(questionIds);

        ScoreMatrix matrix = parseScoreMatrix(rawMatrix, focalGroupSessionIds, referenceGroupSessionIds);

        validateSufficientData(matrix);

        List<ItemDifResult> itemResults = computeDifForAllItems(matrix);

        int moderateCount = (int) itemResults.stream()
                .filter(r -> r.classification() == DifClassification.B_MODERATE)
                .count();
        int largeCount = (int) itemResults.stream()
                .filter(r -> r.classification() == DifClassification.C_LARGE)
                .count();

        if (largeCount > 0) {
            logger.warn("DIF analysis found {} items with LARGE DIF (Category C) "
                    + "between '{}' and '{}' groups.",
                    largeCount, focalGroupLabel, referenceGroupLabel);
        }

        return new DifAnalysisResult(
                null, // No single competency for item-based analysis
                focalGroupLabel,
                referenceGroupLabel,
                matrix.focalSessionCount(),
                matrix.referenceSessionCount(),
                itemResults.size(),
                moderateCount,
                largeCount,
                itemResults
        );
    }

    // ============================================
    // INTERNAL DATA STRUCTURES
    // ============================================

    /**
     * Parsed score matrix with group membership information.
     *
     * @param focalScores     focal group: sessionId -> (questionId -> normalizedScore)
     * @param referenceScores reference group: sessionId -> (questionId -> normalizedScore)
     * @param allQuestions    all distinct question IDs found in the data
     */
    private record ScoreMatrix(
            Map<UUID, Map<UUID, Double>> focalScores,
            Map<UUID, Map<UUID, Double>> referenceScores,
            Set<UUID> allQuestions
    ) {
        int focalSessionCount() { return focalScores.size(); }
        int referenceSessionCount() { return referenceScores.size(); }
        int totalSessionCount() { return focalSessionCount() + referenceSessionCount(); }
    }

    /**
     * A single respondent's data for stratification: total score and group membership.
     */
    private record RespondentData(UUID sessionId, double totalScore, boolean isFocal) {}

    /**
     * 2x2 contingency table cell counts for a single stratum.
     * <pre>
     *                Correct    Incorrect
     * Focal          a          b
     * Reference      c          d
     * </pre>
     */
    private record ContingencyCell(double a, double b, double c, double d) {
        double n() { return a + b + c + d; }
        boolean isValid() { return n() > 0; }
    }

    // ============================================
    // SCORE MATRIX PARSING
    // ============================================

    /**
     * Parse raw database results into a ScoreMatrix with group membership.
     * Only includes sessions that belong to one of the two specified groups.
     */
    private ScoreMatrix parseScoreMatrix(
            List<Object[]> rawMatrix,
            Set<UUID> focalSessionIds,
            Set<UUID> referenceSessionIds) {

        Map<UUID, Map<UUID, Double>> focalScores = new LinkedHashMap<>();
        Map<UUID, Map<UUID, Double>> referenceScores = new LinkedHashMap<>();
        Set<UUID> allQuestions = new LinkedHashSet<>();

        for (Object[] row : rawMatrix) {
            UUID sessionId = (UUID) row[0];
            UUID questionId = (UUID) row[1];
            double score = ((Number) row[2]).doubleValue();

            allQuestions.add(questionId);

            if (focalSessionIds.contains(sessionId)) {
                focalScores.computeIfAbsent(sessionId, k -> new HashMap<>())
                        .put(questionId, score);
            } else if (referenceSessionIds.contains(sessionId)) {
                referenceScores.computeIfAbsent(sessionId, k -> new HashMap<>())
                        .put(questionId, score);
            }
            // Sessions not in either group are ignored
        }

        return new ScoreMatrix(focalScores, referenceScores, allQuestions);
    }

    // ============================================
    // VALIDATION
    // ============================================

    private void validateGroupSizes(Set<UUID> focalGroupSessionIds, Set<UUID> referenceGroupSessionIds) {
        if (focalGroupSessionIds == null || focalGroupSessionIds.isEmpty()) {
            throw new IllegalArgumentException("Focal group session IDs must not be empty");
        }
        if (referenceGroupSessionIds == null || referenceGroupSessionIds.isEmpty()) {
            throw new IllegalArgumentException("Reference group session IDs must not be empty");
        }

        // Check for overlap
        Set<UUID> overlap = new HashSet<>(focalGroupSessionIds);
        overlap.retainAll(referenceGroupSessionIds);
        if (!overlap.isEmpty()) {
            throw new IllegalArgumentException(
                    "Focal and reference groups must not overlap. Found " + overlap.size() + " shared session IDs.");
        }
    }

    private void validateSufficientData(ScoreMatrix matrix) {
        int totalRespondents = matrix.totalSessionCount();
        if (totalRespondents < MIN_TOTAL_RESPONDENTS) {
            throw new IllegalArgumentException(
                    "Insufficient total respondents for DIF analysis: " + totalRespondents
                    + " (minimum " + MIN_TOTAL_RESPONDENTS + " required)");
        }

        if (matrix.focalSessionCount() < MIN_GROUP_SIZE) {
            throw new IllegalArgumentException(
                    "Insufficient focal group size for DIF analysis: " + matrix.focalSessionCount()
                    + " (minimum " + MIN_GROUP_SIZE + " required)");
        }

        if (matrix.referenceSessionCount() < MIN_GROUP_SIZE) {
            throw new IllegalArgumentException(
                    "Insufficient reference group size for DIF analysis: " + matrix.referenceSessionCount()
                    + " (minimum " + MIN_GROUP_SIZE + " required)");
        }

        if (matrix.allQuestions.isEmpty()) {
            throw new IllegalArgumentException("No items found for analysis");
        }
    }

    // ============================================
    // MANTEL-HAENSZEL DIF COMPUTATION
    // ============================================

    /**
     * Compute DIF statistics for all items in the score matrix.
     */
    private List<ItemDifResult> computeDifForAllItems(ScoreMatrix matrix) {
        // Step 1: Compute total scores for all respondents
        List<RespondentData> allRespondents = computeTotalScores(matrix);

        // Step 2: Assign respondents to quintile strata based on total score
        int[][] stratumAssignment = assignStrata(allRespondents);
        // stratumAssignment[0] = indices of respondents in stratum 0, etc.

        // Step 3: For each item, compute MH statistic
        List<ItemDifResult> results = new ArrayList<>();

        for (UUID questionId : matrix.allQuestions()) {
            ItemDifResult result = computeDifForItem(questionId, allRespondents, stratumAssignment, matrix);
            results.add(result);
        }

        return results;
    }

    /**
     * Compute total scores for all respondents across all items.
     */
    private List<RespondentData> computeTotalScores(ScoreMatrix matrix) {
        List<RespondentData> respondents = new ArrayList<>();

        for (Map.Entry<UUID, Map<UUID, Double>> entry : matrix.focalScores().entrySet()) {
            double totalScore = entry.getValue().values().stream()
                    .mapToDouble(Double::doubleValue)
                    .sum();
            respondents.add(new RespondentData(entry.getKey(), totalScore, true));
        }

        for (Map.Entry<UUID, Map<UUID, Double>> entry : matrix.referenceScores().entrySet()) {
            double totalScore = entry.getValue().values().stream()
                    .mapToDouble(Double::doubleValue)
                    .sum();
            respondents.add(new RespondentData(entry.getKey(), totalScore, false));
        }

        return respondents;
    }

    /**
     * Assign respondents to strata based on total score.
     * <p>
     * Uses score-based grouping: respondents with the same total score are always
     * placed in the same stratum. This prevents artificial DIF from tie-breaking
     * artifacts when respondents with identical ability levels are split across
     * stratum boundaries (a well-known issue in MH DIF analysis).
     * <p>
     * Targets approximately NUM_STRATA equal-frequency groups, but respects score
     * boundaries. If there are fewer distinct score levels than NUM_STRATA, uses
     * the actual number of distinct scores as strata.
     *
     * @return array of int arrays where each inner array contains respondent indices for one stratum
     */
    private int[][] assignStrata(List<RespondentData> respondents) {
        int n = respondents.size();

        // Create sorted indices by total score
        Integer[] sortedIndices = new Integer[n];
        for (int i = 0; i < n; i++) {
            sortedIndices[i] = i;
        }
        Arrays.sort(sortedIndices, Comparator.comparingDouble(i -> respondents.get(i).totalScore()));

        // Group by distinct total score values (preserving sort order)
        List<List<Integer>> scoreGroups = new ArrayList<>();
        List<Integer> currentGroup = new ArrayList<>();
        double currentScore = respondents.get(sortedIndices[0]).totalScore();
        currentGroup.add(sortedIndices[0]);

        for (int i = 1; i < n; i++) {
            double score = respondents.get(sortedIndices[i]).totalScore();
            if (Math.abs(score - currentScore) < 1e-9) {
                currentGroup.add(sortedIndices[i]);
            } else {
                scoreGroups.add(currentGroup);
                currentGroup = new ArrayList<>();
                currentGroup.add(sortedIndices[i]);
                currentScore = score;
            }
        }
        scoreGroups.add(currentGroup);

        // Merge score groups into approximately NUM_STRATA equal-frequency strata.
        // We never split a score group across strata.
        int targetStrata = Math.min(NUM_STRATA, scoreGroups.size());
        int targetSize = n / targetStrata;

        List<List<Integer>> mergedStrata = new ArrayList<>();
        List<Integer> currentStratum = new ArrayList<>();

        for (List<Integer> group : scoreGroups) {
            currentStratum.addAll(group);

            // Start a new stratum if we've reached the target size
            // and we haven't used up all strata slots yet
            if (currentStratum.size() >= targetSize && mergedStrata.size() < targetStrata - 1) {
                mergedStrata.add(currentStratum);
                currentStratum = new ArrayList<>();
            }
        }

        // Add the remaining respondents to the last stratum
        if (!currentStratum.isEmpty()) {
            mergedStrata.add(currentStratum);
        }

        // Convert to int[][] array format
        int[][] strata = new int[mergedStrata.size()][];
        for (int s = 0; s < mergedStrata.size(); s++) {
            List<Integer> stratum = mergedStrata.get(s);
            strata[s] = stratum.stream().mapToInt(Integer::intValue).toArray();
        }

        return strata;
    }

    /**
     * Compute DIF for a single item using the Mantel-Haenszel method.
     */
    private ItemDifResult computeDifForItem(
            UUID questionId,
            List<RespondentData> respondents,
            int[][] strata,
            ScoreMatrix matrix) {

        // Build 2x2 contingency tables for each stratum
        double sumAD_N = 0.0; // sum(A_j * D_j / N_j)
        double sumBC_N = 0.0; // sum(B_j * C_j / N_j)

        // For chi-square computation
        double sumA = 0.0;       // sum of observed A_j
        double sumE_A = 0.0;     // sum of expected A_j under H0
        double sumVar_A = 0.0;   // sum of variance of A_j under H0

        for (int[] stratum : strata) {
            ContingencyCell cell = buildContingencyCell(questionId, stratum, respondents, matrix);

            if (!cell.isValid() || cell.n() < 2) {
                // Skip empty or too-small strata
                continue;
            }

            double a = cell.a();
            double b = cell.b();
            double c = cell.c();
            double d = cell.d();
            double nj = cell.n();

            // MH odds ratio components
            sumAD_N += (a * d) / nj;
            sumBC_N += (b * c) / nj;

            // MH chi-square components
            sumA += a;
            double focalTotal = a + b;   // focal row total
            double refTotal = c + d;     // reference row total
            double correctTotal = a + c; // correct column total
            double incorrectTotal = b + d; // incorrect column total

            // Expected value of A_j under H0: E(A_j) = focalTotal * correctTotal / N_j
            double expectedA = (focalTotal * correctTotal) / nj;
            sumE_A += expectedA;

            // Variance of A_j under H0 (hypergeometric variance)
            // Var(A_j) = (focalTotal * refTotal * correctTotal * incorrectTotal) / (N_j^2 * (N_j - 1))
            if (nj > 1) {
                double varA = (focalTotal * refTotal * correctTotal * incorrectTotal)
                        / (nj * nj * (nj - 1));
                sumVar_A += varA;
            }
        }

        // Apply continuity correction to avoid division by zero
        if (sumBC_N == 0.0) {
            sumBC_N = CONTINUITY_CORRECTION;
        }

        // MH odds ratio: alpha_MH = sum(A_j * D_j / N_j) / sum(B_j * C_j / N_j)
        double alphaMH;
        if (sumAD_N == 0.0 && sumBC_N == CONTINUITY_CORRECTION) {
            // Both numerator and denominator effectively zero - no DIF detectable
            alphaMH = 1.0;
        } else {
            alphaMH = sumAD_N / sumBC_N;
        }

        // ETS delta scale: MH_D-DIF = -2.35 * ln(alpha_MH)
        double etsDelta;
        if (alphaMH <= 0) {
            // Degenerate case: use a large magnitude
            etsDelta = ETS_DELTA_CONSTANT * Math.log(CONTINUITY_CORRECTION);
        } else {
            etsDelta = ETS_DELTA_CONSTANT * Math.log(alphaMH);
        }

        // MH chi-square with continuity correction
        // chi^2_MH = (|sum(A_j) - sum(E(A_j))| - 0.5)^2 / sum(Var(A_j))
        double mhChiSquare = 0.0;
        double pValue = 1.0;

        if (sumVar_A > 0) {
            double diff = Math.abs(sumA - sumE_A) - 0.5; // continuity correction
            if (diff < 0) {
                diff = 0;
            }
            mhChiSquare = (diff * diff) / sumVar_A;
            pValue = chiSquarePValue(mhChiSquare);
        }

        // Classify using ETS guidelines
        double absDelta = Math.abs(etsDelta);
        DifClassification classification;
        if (absDelta < ETS_A_B_BOUNDARY) {
            classification = DifClassification.A_NEGLIGIBLE;
        } else if (absDelta < ETS_B_C_BOUNDARY) {
            classification = DifClassification.B_MODERATE;
        } else {
            classification = DifClassification.C_LARGE;
        }

        // Direction: positive delta means item favors reference group
        // (focal group has lower probability of correct after controlling for ability)
        String direction = etsDelta > 0 ? "favors reference" : "favors focal";

        if (classification != DifClassification.A_NEGLIGIBLE) {
            logger.info("Item {} classified as {} DIF (delta={}, direction='{}')",
                    questionId, classification, String.format("%.3f", etsDelta), direction);
        }

        return new ItemDifResult(
                questionId,
                BigDecimal.valueOf(alphaMH).setScale(SCALE, RoundingMode.HALF_UP),
                BigDecimal.valueOf(etsDelta).setScale(SCALE, RoundingMode.HALF_UP),
                BigDecimal.valueOf(mhChiSquare).setScale(SCALE, RoundingMode.HALF_UP),
                BigDecimal.valueOf(pValue).setScale(SCALE, RoundingMode.HALF_UP),
                classification,
                direction
        );
    }

    /**
     * Build a 2x2 contingency table for a single item within a single stratum.
     * <pre>
     *                Correct    Incorrect
     * Focal          A          B
     * Reference      C          D
     * </pre>
     *
     * Uses the CORRECT_THRESHOLD (0.5) to dichotomize normalized scores.
     */
    private ContingencyCell buildContingencyCell(
            UUID questionId,
            int[] stratumIndices,
            List<RespondentData> respondents,
            ScoreMatrix matrix) {

        double a = 0, b = 0, c = 0, d = 0;

        for (int idx : stratumIndices) {
            RespondentData respondent = respondents.get(idx);
            UUID sessionId = respondent.sessionId();
            boolean isFocal = respondent.isFocal();

            // Look up this respondent's score on this item
            Double score = null;
            if (isFocal) {
                Map<UUID, Double> sessionScores = matrix.focalScores().get(sessionId);
                if (sessionScores != null) {
                    score = sessionScores.get(questionId);
                }
            } else {
                Map<UUID, Double> sessionScores = matrix.referenceScores().get(sessionId);
                if (sessionScores != null) {
                    score = sessionScores.get(questionId);
                }
            }

            if (score == null) {
                // Respondent did not answer this item - skip
                continue;
            }

            boolean correct = score >= CORRECT_THRESHOLD;

            if (isFocal) {
                if (correct) a++;
                else b++;
            } else {
                if (correct) c++;
                else d++;
            }
        }

        return new ContingencyCell(a, b, c, d);
    }

    // ============================================
    // STATISTICAL HELPERS
    // ============================================

    /**
     * Compute the p-value for a chi-square statistic with 1 degree of freedom.
     * <p>
     * For chi-square(1), P(X > x) = P(|Z| > sqrt(x)) = 2 * (1 - Phi(sqrt(x)))
     * where Phi is the standard normal CDF.
     * <p>
     * Uses the Abramowitz & Stegun approximation (formula 26.2.17) for the
     * standard normal CDF, which is accurate to 7.5 x 10^-8.
     *
     * @param chiSquare the chi-square test statistic
     * @return p-value (probability of observing a value at least this extreme under H0)
     */
    private double chiSquarePValue(double chiSquare) {
        if (chiSquare <= 0) {
            return 1.0;
        }

        double z = Math.sqrt(chiSquare);

        // Abramowitz & Stegun approximation for standard normal survival function
        // P(Z > z) for z >= 0
        double t = 1.0 / (1.0 + 0.2316419 * z);
        double phi = (1.0 / Math.sqrt(2.0 * Math.PI)) * Math.exp(-z * z / 2.0);
        double survivalFunction = phi * t * (0.319381530
                + t * (-0.356563782
                + t * (1.781477937
                + t * (-1.821255978
                + t * 1.330274429))));

        // For chi-square(1), the p-value is the two-tailed probability
        // P(chi^2(1) > x) = P(|Z| > sqrt(x)) = 2 * P(Z > sqrt(x))
        // But chi-square is already one-sided (always positive), so:
        // P(chi^2(1) > x) = 2 * survivalFunction
        // However, the standard formula gives us exactly the upper tail:
        // P(chi^2(1) > x) = erfc(sqrt(x/2)) which equals 2*P(Z > sqrt(x))
        double pValue = 2.0 * survivalFunction;

        // Clamp to valid range
        return Math.max(0.0, Math.min(1.0, pValue));
    }
}
