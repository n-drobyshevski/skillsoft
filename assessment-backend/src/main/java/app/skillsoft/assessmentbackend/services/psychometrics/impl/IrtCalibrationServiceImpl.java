package app.skillsoft.assessmentbackend.services.psychometrics.impl;

import app.skillsoft.assessmentbackend.domain.entities.Competency;
import app.skillsoft.assessmentbackend.domain.entities.ItemStatistics;
import app.skillsoft.assessmentbackend.repository.CompetencyRepository;
import app.skillsoft.assessmentbackend.repository.ItemStatisticsRepository;
import app.skillsoft.assessmentbackend.repository.TestAnswerRepository;
import app.skillsoft.assessmentbackend.services.psychometrics.IrtCalibrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Stream;

/**
 * Implementation of IRT 2-Parameter Logistic (2PL) model calibration
 * using Joint Maximum Likelihood Estimation (JMLE).
 * <p>
 * The JMLE algorithm alternates between:
 * <ul>
 *   <li>E-step: Estimate person abilities (theta) given current item parameters</li>
 *   <li>M-step: Update item parameters (a, b) given current ability estimates</li>
 * </ul>
 * until parameter changes fall below the convergence threshold or max iterations reached.
 * <p>
 * Newton-Raphson optimization is used for all parameter estimation steps,
 * with appropriate clamping to prevent divergence.
 */
@Service
@Transactional
public class IrtCalibrationServiceImpl implements IrtCalibrationService {

    private static final Logger logger = LoggerFactory.getLogger(IrtCalibrationServiceImpl.class);

    // Data requirements
    static final int MIN_RESPONDENTS = 200;
    static final int MIN_ITEMS = 3;

    // JMLE convergence settings
    static final int MAX_ITERATIONS = 100;
    static final double CONVERGENCE_THRESHOLD = 0.01;

    // Item filtering thresholds (exclude extreme items)
    static final double MIN_P_VALUE = 0.05;
    static final double MAX_P_VALUE = 0.95;

    // Parameter bounds to prevent divergence
    static final double MIN_DISCRIMINATION = 0.1;
    static final double MAX_DISCRIMINATION = 4.0;
    static final double MIN_DIFFICULTY = -4.0;
    static final double MAX_DIFFICULTY = 4.0;
    static final double MIN_THETA = -4.0;
    static final double MAX_THETA = 4.0;

    // Newton-Raphson settings for inner loops
    private static final int NR_MAX_ITERATIONS = 20;
    private static final double NR_CONVERGENCE = 0.0001;
    private static final double HESSIAN_EPSILON = 1e-10;

    // Damping factor for M-step Newton-Raphson to prevent oscillation (0 < lambda <= 1)
    private static final double NR_DAMPING = 0.5;

    // Dichotomization threshold for normalized scores
    private static final double DICHOTOMIZE_THRESHOLD = 0.5;

    private final TestAnswerRepository testAnswerRepository;
    private final CompetencyRepository competencyRepository;
    private final ItemStatisticsRepository itemStatisticsRepository;

    public IrtCalibrationServiceImpl(
            TestAnswerRepository testAnswerRepository,
            CompetencyRepository competencyRepository,
            ItemStatisticsRepository itemStatisticsRepository) {
        this.testAnswerRepository = testAnswerRepository;
        this.competencyRepository = competencyRepository;
        this.itemStatisticsRepository = itemStatisticsRepository;
    }

    @Override
    public List<ItemStatistics> calibrateCompetency(UUID competencyId) {
        CalibrationResult result = calibrateWithDetails(competencyId);

        // Persist calibrated parameters to ItemStatistics
        List<ItemStatistics> updatedStats = new ArrayList<>();
        List<ItemStatistics> allStats = itemStatisticsRepository.findByCompetencyId(competencyId);
        Map<UUID, ItemStatistics> statsByQuestion = new HashMap<>();
        for (ItemStatistics stat : allStats) {
            statsByQuestion.put(stat.getQuestionId(), stat);
        }

        for (ItemCalibration cal : result.itemCalibrations()) {
            ItemStatistics stats = statsByQuestion.get(cal.questionId());
            if (stats != null) {
                stats.setIrtDiscrimination(BigDecimal.valueOf(cal.discrimination())
                        .setScale(4, RoundingMode.HALF_UP));
                stats.setIrtDifficulty(BigDecimal.valueOf(cal.difficulty())
                        .setScale(4, RoundingMode.HALF_UP));
                stats.setIrtGuessing(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
                updatedStats.add(itemStatisticsRepository.save(stats));
            }
        }

        logger.info("IRT calibration complete for competency {}: {} items calibrated, {} iterations, converged={}",
                competencyId, result.itemCount(), result.iterations(), result.converged());

        return updatedStats;
    }

    @Override
    public double estimateAbility(Map<UUID, Double> questionScores) {
        if (questionScores == null || questionScores.isEmpty()) {
            return 0.0;
        }

        // Look up IRT parameters for each question
        List<UUID> questionIds = new ArrayList<>(questionScores.keySet());
        List<ItemStatistics> statsList = itemStatisticsRepository.findByQuestionIdIn(questionIds);

        // Build parallel arrays of responses and parameters
        List<Boolean> responses = new ArrayList<>();
        List<Double> aParams = new ArrayList<>();
        List<Double> bParams = new ArrayList<>();

        for (ItemStatistics stats : statsList) {
            if (stats.getIrtDiscrimination() != null && stats.getIrtDifficulty() != null) {
                Double score = questionScores.get(stats.getQuestionId());
                if (score != null) {
                    responses.add(score >= DICHOTOMIZE_THRESHOLD);
                    aParams.add(stats.getIrtDiscrimination().doubleValue());
                    bParams.add(stats.getIrtDifficulty().doubleValue());
                }
            }
        }

        if (responses.isEmpty()) {
            return 0.0;
        }

        boolean[] responseArr = new boolean[responses.size()];
        double[] aArr = new double[aParams.size()];
        double[] bArr = new double[bParams.size()];

        for (int i = 0; i < responses.size(); i++) {
            responseArr[i] = responses.get(i);
            aArr[i] = aParams.get(i);
            bArr[i] = bParams.get(i);
        }

        return estimateTheta(responseArr, aArr, bArr);
    }

    @Override
    public CalibrationResult calibrateWithDetails(UUID competencyId) {
        // Validate competency exists
        competencyRepository.findById(competencyId)
                .orElseThrow(() -> new IllegalArgumentException("Competency not found: " + competencyId));

        // Build response matrix from score data
        ResponseMatrix matrix = buildResponseMatrix(competencyId);

        // Validate minimum data requirements
        if (matrix.respondentCount < MIN_RESPONDENTS) {
            throw new IllegalArgumentException(
                    "Insufficient respondents for IRT calibration: " + matrix.respondentCount
                            + " (minimum " + MIN_RESPONDENTS + " required)");
        }

        if (matrix.itemCount < MIN_ITEMS) {
            throw new IllegalArgumentException(
                    "Insufficient items for IRT calibration: " + matrix.itemCount
                            + " (minimum " + MIN_ITEMS + " required)");
        }

        // Run JMLE algorithm
        return runJmle(competencyId, matrix);
    }

    // ============================================
    // JMLE ALGORITHM
    // ============================================

    /**
     * Run the Joint Maximum Likelihood Estimation (JMLE) algorithm.
     * Alternates between E-step (theta estimation) and M-step (parameter update).
     */
    CalibrationResult runJmle(UUID competencyId, ResponseMatrix matrix) {
        int nItems = matrix.itemCount;
        int nRespondents = matrix.respondentCount;

        // Initialize item parameters from classical difficulty
        double[] aParams = new double[nItems];
        double[] bParams = new double[nItems];

        for (int i = 0; i < nItems; i++) {
            double p = matrix.itemPValues[i];
            // Clamp p to avoid log(0) or log(inf)
            p = Math.max(0.01, Math.min(0.99, p));
            bParams[i] = clamp(-Math.log(p / (1.0 - p)), MIN_DIFFICULTY, MAX_DIFFICULTY);
            aParams[i] = 1.0;
        }

        // Initialize theta estimates
        double[] thetas = new double[nRespondents];
        // Initialize from proportion correct (logit transform)
        for (int j = 0; j < nRespondents; j++) {
            double pCorrect = 0.0;
            int count = 0;
            for (int i = 0; i < nItems; i++) {
                if (matrix.hasResponse(j, i)) {
                    pCorrect += matrix.getResponse(j, i) ? 1.0 : 0.0;
                    count++;
                }
            }
            if (count > 0) {
                double prop = pCorrect / count;
                prop = Math.max(0.01, Math.min(0.99, prop));
                thetas[j] = clamp(Math.log(prop / (1.0 - prop)), MIN_THETA, MAX_THETA);
            }
        }

        // JMLE iterations
        boolean converged = false;
        int iteration = 0;
        double maxChange = Double.MAX_VALUE;

        while (iteration < MAX_ITERATIONS && !converged) {
            iteration++;

            // E-step: Estimate theta for each person given current a, b
            for (int j = 0; j < nRespondents; j++) {
                // Collect item responses for this person
                List<Integer> itemIndices = new ArrayList<>();
                for (int i = 0; i < nItems; i++) {
                    if (matrix.hasResponse(j, i)) {
                        itemIndices.add(i);
                    }
                }

                if (itemIndices.isEmpty()) continue;

                boolean[] personResponses = new boolean[itemIndices.size()];
                double[] personA = new double[itemIndices.size()];
                double[] personB = new double[itemIndices.size()];

                for (int idx = 0; idx < itemIndices.size(); idx++) {
                    int i = itemIndices.get(idx);
                    personResponses[idx] = matrix.getResponse(j, i);
                    personA[idx] = aParams[i];
                    personB[idx] = bParams[i];
                }

                thetas[j] = estimateTheta(personResponses, personA, personB);
            }

            // Centering constraint: fix the location indeterminacy of JMLE
            // by centering theta estimates to have mean = 0.
            double thetaSum = 0.0;
            int thetaCount = 0;
            for (int j = 0; j < nRespondents; j++) {
                thetaSum += thetas[j];
                thetaCount++;
            }
            if (thetaCount > 0) {
                double thetaMean = thetaSum / thetaCount;
                for (int j = 0; j < nRespondents; j++) {
                    thetas[j] -= thetaMean;
                }
                // Shift difficulty parameters by the same amount to maintain consistency
                for (int i = 0; i < nItems; i++) {
                    bParams[i] -= thetaMean;
                }
            }

            // M-step: Update a and b for each item given current thetas
            maxChange = 0.0;

            for (int i = 0; i < nItems; i++) {
                // Collect responses for this item
                List<Integer> respondentIndices = new ArrayList<>();
                for (int j = 0; j < nRespondents; j++) {
                    if (matrix.hasResponse(j, i)) {
                        respondentIndices.add(j);
                    }
                }

                if (respondentIndices.isEmpty()) continue;

                boolean[] itemResponses = new boolean[respondentIndices.size()];
                double[] itemThetas = new double[respondentIndices.size()];

                for (int idx = 0; idx < respondentIndices.size(); idx++) {
                    int j = respondentIndices.get(idx);
                    itemResponses[idx] = matrix.getResponse(j, i);
                    itemThetas[idx] = thetas[j];
                }

                double oldB = bParams[i];
                double oldA = aParams[i];

                // Update b first, then a
                bParams[i] = estimateB(itemResponses, aParams[i], itemThetas, bParams[i]);
                aParams[i] = estimateA(itemResponses, aParams[i], bParams[i], itemThetas);

                maxChange = Math.max(maxChange, Math.abs(bParams[i] - oldB));
                maxChange = Math.max(maxChange, Math.abs(aParams[i] - oldA));
            }

            converged = maxChange < CONVERGENCE_THRESHOLD;

            if (logger.isDebugEnabled()) {
                logger.debug("JMLE iteration {}: maxChange={}", iteration, maxChange);
            }
        }

        // Build calibration results with standard errors
        List<ItemCalibration> calibrations = new ArrayList<>();
        for (int i = 0; i < nItems; i++) {
            double[] errors = computeStandardErrors(i, aParams[i], bParams[i], thetas, matrix);
            calibrations.add(new ItemCalibration(
                    matrix.questionIds[i],
                    aParams[i],
                    bParams[i],
                    errors[0],
                    errors[1]
            ));
        }

        return new CalibrationResult(
                competencyId,
                nItems,
                nRespondents,
                iteration,
                converged,
                maxChange,
                calibrations
        );
    }

    // ============================================
    // RESPONSE MATRIX BUILDING
    // ============================================

    /**
     * Build a binary response matrix from the score data, filtering extreme items.
     */
    ResponseMatrix buildResponseMatrix(UUID competencyId) {
        // Stream score matrix: [session_id, question_id, normalized_score]
        Map<UUID, Map<UUID, Double>> sessionScores = new LinkedHashMap<>();
        Set<UUID> allQuestions = new LinkedHashSet<>();

        try (Stream<Object[]> stream = testAnswerRepository.streamScoreMatrixForCompetency(competencyId)) {
            stream.forEach(row -> {
                UUID sessionId = (UUID) row[0];
                UUID questionId = (UUID) row[1];
                double normalizedScore = ((Number) row[2]).doubleValue();

                sessionScores.computeIfAbsent(sessionId, k -> new HashMap<>())
                        .put(questionId, normalizedScore);
                allQuestions.add(questionId);
            });
        }

        // Compute p-values for each item and filter extremes
        List<UUID> questionList = new ArrayList<>(allQuestions);
        List<UUID> filteredQuestions = new ArrayList<>();
        List<Double> filteredPValues = new ArrayList<>();

        for (UUID qId : questionList) {
            double sum = 0.0;
            int count = 0;
            for (Map<UUID, Double> scores : sessionScores.values()) {
                Double score = scores.get(qId);
                if (score != null) {
                    sum += (score >= DICHOTOMIZE_THRESHOLD) ? 1.0 : 0.0;
                    count++;
                }
            }
            if (count > 0) {
                double pValue = sum / count;
                if (pValue >= MIN_P_VALUE && pValue <= MAX_P_VALUE) {
                    filteredQuestions.add(qId);
                    filteredPValues.add(pValue);
                } else {
                    logger.debug("Excluding item {} with extreme p-value: {}", qId, pValue);
                }
            }
        }

        // Build session list (only sessions with at least 1 response to filtered items)
        List<UUID> sessionList = new ArrayList<>();
        for (Map.Entry<UUID, Map<UUID, Double>> entry : sessionScores.entrySet()) {
            boolean hasFiltered = false;
            for (UUID qId : filteredQuestions) {
                if (entry.getValue().containsKey(qId)) {
                    hasFiltered = true;
                    break;
                }
            }
            if (hasFiltered) {
                sessionList.add(entry.getKey());
            }
        }

        int nItems = filteredQuestions.size();
        int nRespondents = sessionList.size();

        // Build the response matrix
        UUID[] questionIds = filteredQuestions.toArray(new UUID[0]);
        double[] itemPValues = new double[nItems];
        for (int i = 0; i < nItems; i++) {
            itemPValues[i] = filteredPValues.get(i);
        }

        // responses[j][i] = response of person j to item i; null if missing
        Boolean[][] responses = new Boolean[nRespondents][nItems];

        for (int j = 0; j < nRespondents; j++) {
            Map<UUID, Double> scores = sessionScores.get(sessionList.get(j));
            for (int i = 0; i < nItems; i++) {
                Double score = scores.get(questionIds[i]);
                if (score != null) {
                    responses[j][i] = score >= DICHOTOMIZE_THRESHOLD;
                }
            }
        }

        return new ResponseMatrix(questionIds, nItems, nRespondents, itemPValues, responses);
    }

    // ============================================
    // NEWTON-RAPHSON ESTIMATION METHODS
    // ============================================

    /**
     * Estimate theta (ability) for a respondent using Newton-Raphson on the log-likelihood.
     * <p>
     * L(theta) = sum_i [x_i * log(P_i) + (1 - x_i) * log(1 - P_i)]
     * L'(theta) = sum_i [a_i * (x_i - P_i)]
     * L''(theta) = -sum_i [a_i^2 * P_i * (1 - P_i)]
     */
    double estimateTheta(boolean[] responses, double[] aParams, double[] bParams) {
        double theta = 0.0; // Start at mean ability

        for (int iter = 0; iter < NR_MAX_ITERATIONS; iter++) {
            double dL = 0.0;  // First derivative
            double d2L = 0.0; // Second derivative

            for (int i = 0; i < responses.length; i++) {
                double p = probability(theta, aParams[i], bParams[i]);
                double x = responses[i] ? 1.0 : 0.0;
                dL += aParams[i] * (x - p);
                d2L -= aParams[i] * aParams[i] * p * (1.0 - p);
            }

            if (Math.abs(d2L) < HESSIAN_EPSILON) break;

            double delta = dL / d2L;
            theta = clamp(theta - delta, MIN_THETA, MAX_THETA);

            if (Math.abs(delta) < NR_CONVERGENCE) break;
        }

        return theta;
    }

    /**
     * Estimate difficulty parameter (b) for an item using Newton-Raphson with damping.
     * <p>
     * L'(b) = sum_j [a * (P_j - x_j)]
     * L''(b) = -sum_j [a^2 * P_j * (1 - P_j)]
     */
    double estimateB(boolean[] itemResponses, double a, double[] thetas, double currentB) {
        double b = currentB;

        for (int iter = 0; iter < NR_MAX_ITERATIONS; iter++) {
            double dL = 0.0;
            double d2L = 0.0;

            for (int j = 0; j < thetas.length; j++) {
                double p = probability(thetas[j], a, b);
                double x = itemResponses[j] ? 1.0 : 0.0;
                dL += a * (p - x);   // derivative of log-L w.r.t. b
                d2L -= a * a * p * (1.0 - p);
            }

            if (Math.abs(d2L) < HESSIAN_EPSILON) break;

            double delta = NR_DAMPING * (dL / d2L);
            b = clamp(b - delta, MIN_DIFFICULTY, MAX_DIFFICULTY);

            if (Math.abs(delta) < NR_CONVERGENCE) break;
        }

        return b;
    }

    /**
     * Estimate discrimination parameter (a) for an item using Newton-Raphson with damping.
     * <p>
     * L'(a) = sum_j [(theta_j - b) * (x_j - P_j)]
     * L''(a) = -sum_j [(theta_j - b)^2 * P_j * (1 - P_j)]
     */
    double estimateA(boolean[] itemResponses, double currentA, double b, double[] thetas) {
        double a = currentA;

        for (int iter = 0; iter < NR_MAX_ITERATIONS; iter++) {
            double dL = 0.0;
            double d2L = 0.0;

            for (int j = 0; j < thetas.length; j++) {
                double p = probability(thetas[j], a, b);
                double x = itemResponses[j] ? 1.0 : 0.0;
                double thetaMinusB = thetas[j] - b;
                dL += thetaMinusB * (x - p);
                d2L -= thetaMinusB * thetaMinusB * p * (1.0 - p);
            }

            if (Math.abs(d2L) < HESSIAN_EPSILON) break;

            double delta = NR_DAMPING * (dL / d2L);
            a = clamp(a - delta, MIN_DISCRIMINATION, MAX_DISCRIMINATION);

            if (Math.abs(delta) < NR_CONVERGENCE) break;
        }

        return a;
    }

    // ============================================
    // PROBABILITY AND UTILITY METHODS
    // ============================================

    /**
     * Compute the 2PL probability of a correct response.
     * P(correct | theta, a, b) = 1 / (1 + exp(-a * (theta - b)))
     */
    double probability(double theta, double a, double b) {
        double exponent = -a * (theta - b);
        // Prevent overflow
        if (exponent > 35.0) return 0.0;
        if (exponent < -35.0) return 1.0;
        return 1.0 / (1.0 + Math.exp(exponent));
    }

    /**
     * Compute standard errors for item parameters using the observed information matrix.
     * SE = 1 / sqrt(I), where I is the Fisher information.
     */
    private double[] computeStandardErrors(int itemIndex, double a, double b,
                                           double[] thetas, ResponseMatrix matrix) {
        double infoA = 0.0;
        double infoB = 0.0;

        for (int j = 0; j < matrix.respondentCount; j++) {
            if (!matrix.hasResponse(j, itemIndex)) continue;

            double p = probability(thetas[j], a, b);
            double pq = p * (1.0 - p);
            double thetaMinusB = thetas[j] - b;

            infoA += thetaMinusB * thetaMinusB * pq;
            infoB += a * a * pq;
        }

        double seA = (infoA > HESSIAN_EPSILON) ? 1.0 / Math.sqrt(infoA) : Double.NaN;
        double seB = (infoB > HESSIAN_EPSILON) ? 1.0 / Math.sqrt(infoB) : Double.NaN;

        return new double[]{seA, seB};
    }

    /**
     * Clamp a value to the specified range.
     */
    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    // ============================================
    // RESPONSE MATRIX DATA STRUCTURE
    // ============================================

    /**
     * Internal data structure holding the binary response matrix and item metadata.
     * <p>
     * The matrix uses Boolean[][] to support missing data (null = not administered).
     */
    static class ResponseMatrix {
        final UUID[] questionIds;
        final int itemCount;
        final int respondentCount;
        final double[] itemPValues;
        final Boolean[][] responses; // [respondent][item], null = missing

        ResponseMatrix(UUID[] questionIds, int itemCount, int respondentCount,
                       double[] itemPValues, Boolean[][] responses) {
            this.questionIds = questionIds;
            this.itemCount = itemCount;
            this.respondentCount = respondentCount;
            this.itemPValues = itemPValues;
            this.responses = responses;
        }

        boolean hasResponse(int respondent, int item) {
            return responses[respondent][item] != null;
        }

        boolean getResponse(int respondent, int item) {
            return responses[respondent][item];
        }
    }
}
