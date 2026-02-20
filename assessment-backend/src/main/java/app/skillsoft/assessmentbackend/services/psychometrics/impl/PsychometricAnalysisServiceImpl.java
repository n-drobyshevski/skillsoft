package app.skillsoft.assessmentbackend.services.psychometrics.impl;

import app.skillsoft.assessmentbackend.domain.dto.psychometrics.FlaggedItemSummary;
import app.skillsoft.assessmentbackend.domain.dto.psychometrics.PsychometricHealthReport;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.repository.*;
import app.skillsoft.assessmentbackend.services.psychometrics.PsychometricAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of PsychometricAnalysisService.
 * <p>
 * Provides classical test theory (CTT) psychometric analysis including:
 * <ul>
 *   <li>Item Difficulty Index (p-value): Average normalized score</li>
 *   <li>Item Discrimination Index (Point-Biserial Correlation): Item-total correlation</li>
 *   <li>Cronbach's Alpha: Internal consistency reliability coefficient</li>
 *   <li>Alpha-if-Item-Deleted: Item contribution analysis</li>
 * </ul>
 * <p>
 * Uses precise BigDecimal arithmetic for all statistical calculations
 * to ensure accuracy in psychometric metrics.
 */
@Service
@Transactional
public class PsychometricAnalysisServiceImpl implements PsychometricAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(PsychometricAnalysisServiceImpl.class);

    // Psychometric thresholds
    private static final int MIN_RESPONSES = 50;

    /**
     * Response completeness threshold: 90% for both competency and Big Five reliability.
     * Aligned per APA Standards - ensures comparable alpha values across measurement levels.
     * Previous values: competency=100%, Big Five=80% (inconsistent, making comparisons unreliable).
     */
    private static final double RESPONSE_COMPLETENESS_THRESHOLD = 0.9;
    private static final BigDecimal DIFFICULTY_TOO_HARD = new BigDecimal("0.2");
    private static final BigDecimal DIFFICULTY_TOO_EASY = new BigDecimal("0.9");
    private static final BigDecimal DISCRIMINATION_CRITICAL = new BigDecimal("0.1");
    private static final BigDecimal DISCRIMINATION_WARNING = new BigDecimal("0.2");
    private static final BigDecimal DISCRIMINATION_GOOD = new BigDecimal("0.25");
    private static final BigDecimal DISCRIMINATION_EXCELLENT = new BigDecimal("0.3");
    private static final BigDecimal ALPHA_RELIABLE = new BigDecimal("0.7");
    private static final BigDecimal ALPHA_ACCEPTABLE = new BigDecimal("0.6");

    // Math context for precise calculations
    private static final MathContext MATH_CONTEXT = new MathContext(10, RoundingMode.HALF_UP);
    private static final int SCALE = 4;

    private final ItemStatisticsRepository itemStatisticsRepository;
    private final CompetencyReliabilityRepository competencyReliabilityRepository;
    private final BigFiveReliabilityRepository bigFiveReliabilityRepository;
    private final TestAnswerRepository testAnswerRepository;
    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final CompetencyRepository competencyRepository;

    public PsychometricAnalysisServiceImpl(
            ItemStatisticsRepository itemStatisticsRepository,
            CompetencyReliabilityRepository competencyReliabilityRepository,
            BigFiveReliabilityRepository bigFiveReliabilityRepository,
            TestAnswerRepository testAnswerRepository,
            AssessmentQuestionRepository assessmentQuestionRepository,
            CompetencyRepository competencyRepository) {
        this.itemStatisticsRepository = itemStatisticsRepository;
        this.competencyReliabilityRepository = competencyReliabilityRepository;
        this.bigFiveReliabilityRepository = bigFiveReliabilityRepository;
        this.testAnswerRepository = testAnswerRepository;
        this.assessmentQuestionRepository = assessmentQuestionRepository;
        this.competencyRepository = competencyRepository;
    }

    // ============================================
    // ITEM-LEVEL ANALYSIS
    // ============================================

    @Override
    public ItemStatistics calculateItemStatistics(UUID questionId) {
        logger.debug("Calculating item statistics for question: {}", questionId);

        AssessmentQuestion question = assessmentQuestionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));

        // Get or create statistics record
        ItemStatistics stats = itemStatisticsRepository.findByQuestion_Id(questionId)
                .orElseGet(() -> new ItemStatistics(question));

        // Store previous discrimination for trend analysis
        if (stats.getDiscriminationIndex() != null) {
            stats.setPreviousDiscriminationIndex(stats.getDiscriminationIndex());
        }

        // Count responses
        long responseCount = testAnswerRepository.countByQuestion_Id(questionId);
        stats.setResponseCount((int) responseCount);

        if (responseCount < MIN_RESPONSES) {
            logger.info("Insufficient responses ({}) for question {}. Setting PROBATION status.",
                    responseCount, questionId);
            updateStatusWithHistory(stats, ItemValidityStatus.PROBATION,
                    "Insufficient responses: " + responseCount + " < " + MIN_RESPONSES);
            stats.setLastCalculatedAt(LocalDateTime.now());
            return itemStatisticsRepository.save(stats);
        }

        // Calculate metrics
        BigDecimal difficultyIndex = calculateDifficultyIndex(questionId);
        BigDecimal discriminationIndex = calculateDiscriminationIndex(questionId);
        Map<String, Double> distractorEfficiency = analyzeDistractors(questionId);

        stats.setDifficultyIndex(difficultyIndex);
        stats.setDiscriminationIndex(discriminationIndex);
        stats.setDistractorEfficiency(distractorEfficiency);

        // Set difficulty flag
        stats.setDifficultyFlag(determineDifficultyFlag(difficultyIndex));

        // Set discrimination flag
        stats.setDiscriminationFlag(determineDiscriminationFlag(discriminationIndex));

        // Determine validity status
        ItemValidityStatus newStatus = determineValidityStatus(difficultyIndex, discriminationIndex);
        updateStatusWithHistory(stats, newStatus, generateStatusReason(difficultyIndex, discriminationIndex));

        stats.setLastCalculatedAt(LocalDateTime.now());

        logger.info("Item statistics calculated for question {}: p={}, rpb={}, status={}",
                questionId,
                difficultyIndex != null ? difficultyIndex.setScale(3, RoundingMode.HALF_UP) : "null",
                discriminationIndex != null ? discriminationIndex.setScale(3, RoundingMode.HALF_UP) : "null",
                newStatus);

        return itemStatisticsRepository.save(stats);
    }

    @Override
    public BigDecimal calculateDifficultyIndex(UUID questionId) {
        List<TestAnswer> answers = testAnswerRepository.findAllByQuestionId(questionId);

        if (answers.isEmpty()) {
            return null;
        }

        // Calculate average normalized score
        double sumNormalized = 0.0;
        int validCount = 0;

        for (TestAnswer answer : answers) {
            Double score = answer.getScore();
            Double maxScore = answer.getMaxScore();
            if (score != null && maxScore != null && maxScore > 0) {
                double normalized = score / maxScore;
                sumNormalized += normalized;
                validCount++;
            }
        }

        if (validCount == 0) {
            return null;
        }

        double difficultyIndex = sumNormalized / validCount;
        return BigDecimal.valueOf(difficultyIndex).setScale(SCALE, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculateDiscriminationIndex(UUID questionId) {
        // Get item score paired with total test score
        List<Object[]> scorePairs = testAnswerRepository.findItemTotalScorePairs(questionId);

        if (scorePairs.size() < MIN_RESPONSES) {
            logger.debug("Insufficient score pairs ({}) for discrimination calculation", scorePairs.size());
            return null;
        }

        // Extract scores into arrays for correlation calculation
        List<Double> itemScores = new ArrayList<>();
        List<Double> totalScores = new ArrayList<>();

        for (Object[] pair : scorePairs) {
            if (pair[0] != null && pair[1] != null) {
                double itemScore = ((Number) pair[0]).doubleValue();
                double totalScore = ((Number) pair[1]).doubleValue();
                itemScores.add(itemScore);
                totalScores.add(totalScore);
            }
        }

        if (itemScores.size() < MIN_RESPONSES) {
            return null;
        }

        // Calculate Pearson correlation (Point-Biserial for dichotomous/polytomous items)
        BigDecimal correlation = calculatePearsonCorrelation(itemScores, totalScores);

        return correlation != null ? correlation.setScale(SCALE, RoundingMode.HALF_UP) : null;
    }

    @Override
    public Map<String, Double> analyzeDistractors(UUID questionId) {
        List<Object[]> distribution = testAnswerRepository.getDistractorDistribution(questionId);

        if (distribution.isEmpty()) {
            return Collections.emptyMap();
        }

        // Calculate total selections
        long totalSelections = distribution.stream()
                .mapToLong(row -> ((Number) row[1]).longValue())
                .sum();

        if (totalSelections == 0) {
            return Collections.emptyMap();
        }

        // Calculate percentage for each option
        Map<String, Double> efficiency = new HashMap<>();
        for (Object[] row : distribution) {
            String optionId = (String) row[0];
            long count = ((Number) row[1]).longValue();
            double percentage = (double) count / totalSelections;
            efficiency.put(optionId, percentage);
        }

        return efficiency;
    }

    // ============================================
    // COMPETENCY-LEVEL ANALYSIS
    // ============================================

    @Override
    public CompetencyReliability calculateCompetencyReliability(UUID competencyId) {
        logger.debug("Calculating reliability for competency: {}", competencyId);

        Competency competency = competencyRepository.findById(competencyId)
                .orElseThrow(() -> new IllegalArgumentException("Competency not found: " + competencyId));

        // Get or create reliability record
        CompetencyReliability reliability = competencyReliabilityRepository.findByCompetency_Id(competencyId)
                .orElseGet(() -> new CompetencyReliability(competency));

        // Build score matrix ONCE (streamed from DB)
        ScoreMatrixData matrix = buildScoreMatrix(competencyId);

        // Calculate Cronbach's Alpha using pre-built matrix
        BigDecimal alpha = calculateCronbachAlphaFromMatrix(matrix);
        Map<UUID, BigDecimal> alphaIfDeleted = calculateAlphaIfDeletedFromMatrix(matrix);

        reliability.setCronbachAlpha(alpha);
        reliability.setAlphaIfDeleted(alphaIfDeleted);
        reliability.setSampleSize(matrix.sessionCount());
        reliability.setItemCount(matrix.itemCount());
        reliability.setReliabilityStatus(determineReliabilityStatus(alpha, matrix.sessionCount(), matrix.itemCount()));
        reliability.setLastCalculatedAt(LocalDateTime.now());

        logger.info("Competency reliability calculated for {}: alpha={}, status={}",
                competencyId,
                alpha != null ? alpha.setScale(3, RoundingMode.HALF_UP) : "null",
                reliability.getReliabilityStatus());

        return competencyReliabilityRepository.save(reliability);
    }

    @Override
    public BigDecimal calculateCronbachAlpha(UUID competencyId) {
        ScoreMatrixData matrix = buildScoreMatrix(competencyId);
        return calculateCronbachAlphaFromMatrix(matrix);
    }

    /**
     * Calculate Cronbach's Alpha from a pre-built ScoreMatrixData.
     * Extracted to allow calculateCompetencyReliability to reuse an already-built matrix.
     */
    private BigDecimal calculateCronbachAlphaFromMatrix(ScoreMatrixData matrix) {
        if (matrix.isEmpty()) {
            logger.debug("No score data available in matrix");
            return null;
        }

        int k = matrix.itemCount(); // number of items
        int n = matrix.sessionCount(); // number of respondents

        if (k < 2 || n < MIN_RESPONSES) {
            logger.debug("Insufficient data for alpha calculation: k={}, n={}", k, n);
            return null;
        }

        // Convert to ordered list for consistent indexing
        List<UUID> questionList = new ArrayList<>(matrix.allQuestions());

        // Response completeness threshold: 90% for both competency and Big Five reliability
        // Aligned per APA Standards - ensures comparable alpha values across measurement levels.
        // Previous values: competency=100%, Big Five=80% (inconsistent, making comparisons unreliable)
        List<Map<UUID, BigDecimal>> completeScores = matrix.sessionScores().values().stream()
                .filter(scores -> scores.size() >= k * RESPONSE_COMPLETENESS_THRESHOLD)
                .collect(Collectors.toList());

        if (completeScores.size() < MIN_RESPONSES) {
            logger.debug("Insufficient complete responses for alpha: {}", completeScores.size());
            return null;
        }

        // Guard: sample variance requires n > 1 to avoid division by zero
        if (completeScores.size() <= 1) {
            logger.debug("Cannot compute sample variance with n <= 1");
            return null;
        }

        // Calculate item means and variances
        // Uses sample variance (÷(N-1)) per APA Standards for Educational and Psychological Testing.
        // Sample variance is the unbiased estimator when data represents a sample from a larger population,
        // which is the case for psychometric reliability analysis (respondents are a sample of test-takers).
        BigDecimal[] itemMeans = new BigDecimal[k];
        BigDecimal[] itemVariances = new BigDecimal[k];
        BigDecimal nDecimal = BigDecimal.valueOf(completeScores.size());
        BigDecimal nMinus1 = BigDecimal.valueOf(completeScores.size() - 1);

        for (int i = 0; i < k; i++) {
            UUID questionId = questionList.get(i);

            // Calculate mean for this item
            BigDecimal sum = BigDecimal.ZERO;
            for (Map<UUID, BigDecimal> scores : completeScores) {
                sum = sum.add(scores.getOrDefault(questionId, BigDecimal.ZERO));
            }
            itemMeans[i] = sum.divide(nDecimal, MATH_CONTEXT);

            // Calculate sample variance for this item (÷(N-1), Bessel's correction)
            BigDecimal sumSquaredDiff = BigDecimal.ZERO;
            for (Map<UUID, BigDecimal> scores : completeScores) {
                BigDecimal score = scores.getOrDefault(questionId, BigDecimal.ZERO);
                BigDecimal diff = score.subtract(itemMeans[i]);
                sumSquaredDiff = sumSquaredDiff.add(diff.multiply(diff));
            }
            itemVariances[i] = sumSquaredDiff.divide(nMinus1, MATH_CONTEXT);
        }

        // Calculate total score variance (sample variance, ÷(N-1))
        BigDecimal totalMean = BigDecimal.ZERO;
        List<BigDecimal> totalScores = new ArrayList<>();

        for (Map<UUID, BigDecimal> scores : completeScores) {
            BigDecimal total = scores.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalScores.add(total);
            totalMean = totalMean.add(total);
        }
        totalMean = totalMean.divide(nDecimal, MATH_CONTEXT);

        BigDecimal totalVariance = BigDecimal.ZERO;
        for (BigDecimal total : totalScores) {
            BigDecimal diff = total.subtract(totalMean);
            totalVariance = totalVariance.add(diff.multiply(diff));
        }
        totalVariance = totalVariance.divide(nMinus1, MATH_CONTEXT);

        if (totalVariance.compareTo(BigDecimal.ZERO) == 0) {
            logger.debug("Total variance is zero, cannot calculate alpha");
            return null;
        }

        // Sum of item variances
        BigDecimal sumItemVariances = Arrays.stream(itemVariances)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Cronbach's Alpha formula: alpha = (k / (k-1)) * (1 - sum(var_i) / var_total)
        BigDecimal kDecimal = BigDecimal.valueOf(k);
        BigDecimal kMinus1 = BigDecimal.valueOf(k - 1);
        BigDecimal factor = kDecimal.divide(kMinus1, MATH_CONTEXT);
        BigDecimal varianceRatio = sumItemVariances.divide(totalVariance, MATH_CONTEXT);
        BigDecimal alpha = factor.multiply(BigDecimal.ONE.subtract(varianceRatio));

        return alpha.setScale(SCALE, RoundingMode.HALF_UP);
    }

    @Override
    public Map<UUID, BigDecimal> calculateAlphaIfDeleted(UUID competencyId) {
        ScoreMatrixData matrix = buildScoreMatrix(competencyId);
        return calculateAlphaIfDeletedFromMatrix(matrix);
    }

    /**
     * Calculate alpha-if-item-deleted from a pre-built ScoreMatrixData.
     * Extracted to allow calculateCompetencyReliability to reuse an already-built matrix.
     */
    private Map<UUID, BigDecimal> calculateAlphaIfDeletedFromMatrix(ScoreMatrixData matrix) {
        Map<UUID, BigDecimal> alphaIfDeleted = new HashMap<>();

        if (matrix.isEmpty()) {
            return alphaIfDeleted;
        }

        int k = matrix.itemCount();
        if (k < 3) { // Need at least 3 items to calculate alpha-if-deleted meaningfully
            return alphaIfDeleted;
        }

        List<UUID> questionList = new ArrayList<>(matrix.allQuestions());

        // Pre-compute: filter complete sessions once using ALL items (90% threshold)
        List<Map<UUID, BigDecimal>> completeScores = matrix.sessionScores().values().stream()
                .filter(scores -> scores.size() >= k * RESPONSE_COMPLETENESS_THRESHOLD)
                .collect(Collectors.toList());

        if (completeScores.size() < MIN_RESPONSES) {
            return alphaIfDeleted;
        }

        // Guard: sample variance requires n > 1
        if (completeScores.size() <= 1) {
            return alphaIfDeleted;
        }

        int n = completeScores.size();
        BigDecimal nDecimal = BigDecimal.valueOf(n);
        BigDecimal nMinus1 = BigDecimal.valueOf(n - 1);

        // Pre-compute item means, item variances, and item scores per session (single pass O(k*n))
        BigDecimal[] itemMeans = new BigDecimal[k];
        BigDecimal[] itemVariances = new BigDecimal[k];
        // itemScores[i][j] = score of item i for session j
        BigDecimal[][] itemScores = new BigDecimal[k][n];

        for (int i = 0; i < k; i++) {
            UUID questionId = questionList.get(i);

            // Collect scores and compute mean
            BigDecimal sum = BigDecimal.ZERO;
            for (int j = 0; j < n; j++) {
                BigDecimal score = completeScores.get(j).getOrDefault(questionId, BigDecimal.ZERO);
                itemScores[i][j] = score;
                sum = sum.add(score);
            }
            itemMeans[i] = sum.divide(nDecimal, MATH_CONTEXT);

            // Compute sample variance (÷(N-1), Bessel's correction)
            BigDecimal sumSquaredDiff = BigDecimal.ZERO;
            for (int j = 0; j < n; j++) {
                BigDecimal diff = itemScores[i][j].subtract(itemMeans[i]);
                sumSquaredDiff = sumSquaredDiff.add(diff.multiply(diff));
            }
            itemVariances[i] = sumSquaredDiff.divide(nMinus1, MATH_CONTEXT);
        }

        // Pre-compute total scores per session and total variance
        BigDecimal[] totalScoresArr = new BigDecimal[n];
        BigDecimal totalMeanSum = BigDecimal.ZERO;

        for (int j = 0; j < n; j++) {
            BigDecimal total = BigDecimal.ZERO;
            for (int i = 0; i < k; i++) {
                total = total.add(itemScores[i][j]);
            }
            totalScoresArr[j] = total;
            totalMeanSum = totalMeanSum.add(total);
        }
        BigDecimal totalMean = totalMeanSum.divide(nDecimal, MATH_CONTEXT);

        BigDecimal totalVariance = BigDecimal.ZERO;
        for (int j = 0; j < n; j++) {
            BigDecimal diff = totalScoresArr[j].subtract(totalMean);
            totalVariance = totalVariance.add(diff.multiply(diff));
        }
        totalVariance = totalVariance.divide(nMinus1, MATH_CONTEXT);

        // Pre-compute sum of all item variances
        BigDecimal sumItemVar = BigDecimal.ZERO;
        for (int i = 0; i < k; i++) {
            sumItemVar = sumItemVar.add(itemVariances[i]);
        }

        // Pre-compute covariance of each item with the total score: cov(item_i, total)
        // cov(X, Y) = (1/(N-1)) * sum((X_j - mean_X) * (Y_j - mean_Y))
        BigDecimal[] itemCovWithTotal = new BigDecimal[k];
        for (int i = 0; i < k; i++) {
            BigDecimal covSum = BigDecimal.ZERO;
            for (int j = 0; j < n; j++) {
                BigDecimal diffItem = itemScores[i][j].subtract(itemMeans[i]);
                BigDecimal diffTotal = totalScoresArr[j].subtract(totalMean);
                covSum = covSum.add(diffItem.multiply(diffTotal));
            }
            itemCovWithTotal[i] = covSum.divide(nMinus1, MATH_CONTEXT);
        }

        // For each deleted item, compute alpha incrementally in O(1) per item
        // When removing item i:
        //   k_new = k - 1
        //   sumItemVar_new = sumItemVar - var_i
        //   totalVar_new = totalVar - 2*cov(item_i, total) + var_i
        //   alpha_without_i = (k_new / (k_new - 1)) * (1 - sumItemVar_new / totalVar_new)
        BigDecimal kNew = BigDecimal.valueOf(k - 1);
        BigDecimal kNewMinus1 = BigDecimal.valueOf(k - 2);
        BigDecimal factor = kNew.divide(kNewMinus1, MATH_CONTEXT);
        BigDecimal TWO = BigDecimal.valueOf(2);

        for (int i = 0; i < k; i++) {
            BigDecimal sumItemVarNew = sumItemVar.subtract(itemVariances[i]);
            BigDecimal totalVarNew = totalVariance
                    .subtract(TWO.multiply(itemCovWithTotal[i]))
                    .add(itemVariances[i]);

            if (totalVarNew.compareTo(BigDecimal.ZERO) == 0) {
                // Cannot calculate alpha when total variance is zero
                continue;
            }

            BigDecimal varianceRatio = sumItemVarNew.divide(totalVarNew, MATH_CONTEXT);
            BigDecimal alphaWithout = factor.multiply(BigDecimal.ONE.subtract(varianceRatio));

            alphaIfDeleted.put(questionList.get(i), alphaWithout.setScale(SCALE, RoundingMode.HALF_UP));
        }

        logger.debug("Alpha-if-deleted for {} items computed in single pass", k);

        return alphaIfDeleted;
    }

    // ============================================
    // BIG FIVE TRAIT-LEVEL ANALYSIS
    // ============================================

    @Override
    public BigFiveReliability calculateBigFiveReliability(BigFiveTrait trait) {
        logger.debug("Calculating reliability for Big Five trait: {}", trait);

        // Get or create reliability record
        BigFiveReliability reliability = bigFiveReliabilityRepository.findByTrait(trait)
                .orElseGet(() -> new BigFiveReliability(trait));

        // Find all competencies mapped to this Big Five trait
        List<Competency> mappedCompetencies = findCompetenciesByBigFiveTrait(trait);

        if (mappedCompetencies.isEmpty()) {
            logger.info("No competencies mapped to Big Five trait: {}", trait);
            reliability.setReliabilityStatus(ReliabilityStatus.INSUFFICIENT_DATA);
            reliability.setContributingCompetencies(0);
            reliability.setTotalItems(0);
            reliability.setSampleSize(0);
            reliability.setLastCalculatedAt(LocalDateTime.now());
            return bigFiveReliabilityRepository.save(reliability);
        }

        // Aggregate score matrix across all mapped competencies (streamed from DB)
        Map<UUID, Map<UUID, BigDecimal>> aggregatedScores = new LinkedHashMap<>();
        Set<UUID> allItems = new HashSet<>();

        for (Competency competency : mappedCompetencies) {
            try (Stream<Object[]> stream = testAnswerRepository.streamScoreMatrixForCompetency(competency.getId())) {
                stream.forEach(row -> {
                    UUID sessionId = (UUID) row[0];
                    UUID questionId = (UUID) row[1];
                    BigDecimal score = BigDecimal.valueOf(((Number) row[2]).doubleValue());

                    aggregatedScores.computeIfAbsent(sessionId, k -> new HashMap<>())
                            .put(questionId, score);
                    allItems.add(questionId);
                });
            }
        }

        // Calculate alpha using aggregated data
        BigDecimal alpha = calculateAlphaFromMatrix(aggregatedScores, allItems);

        // Set metrics
        reliability.setCronbachAlpha(alpha);
        reliability.setContributingCompetencies(mappedCompetencies.size());
        reliability.setTotalItems(allItems.size());
        reliability.setSampleSize(aggregatedScores.size());
        reliability.setReliabilityStatus(determineReliabilityStatus(alpha, aggregatedScores.size(), allItems.size()));
        reliability.setLastCalculatedAt(LocalDateTime.now());

        logger.info("Big Five reliability calculated for {}: alpha={}, competencies={}, items={}",
                trait, alpha, mappedCompetencies.size(), allItems.size());

        return bigFiveReliabilityRepository.save(reliability);
    }

    /**
     * Pre-parsed score matrix data structure.
     * Built once via streaming and reused across alpha/alpha-if-deleted calculations.
     */
    private record ScoreMatrixData(
            Map<UUID, Map<UUID, BigDecimal>> sessionScores,
            Set<UUID> allQuestions
    ) {
        boolean isEmpty() { return sessionScores.isEmpty(); }
        int sessionCount() { return sessionScores.size(); }
        int itemCount() { return allQuestions.size(); }
    }

    /**
     * Build a ScoreMatrixData from the streaming repository query.
     * Uses cursor-based fetching to avoid loading the entire result set into memory at once.
     */
    private ScoreMatrixData buildScoreMatrix(UUID competencyId) {
        Map<UUID, Map<UUID, BigDecimal>> sessionScores = new LinkedHashMap<>();
        Set<UUID> allQuestions = new LinkedHashSet<>();

        try (Stream<Object[]> stream = testAnswerRepository.streamScoreMatrixForCompetency(competencyId)) {
            stream.forEach(row -> {
                UUID sessionId = (UUID) row[0];
                UUID questionId = (UUID) row[1];
                BigDecimal score = BigDecimal.valueOf(((Number) row[2]).doubleValue());

                sessionScores.computeIfAbsent(sessionId, k -> new HashMap<>())
                        .put(questionId, score);
                allQuestions.add(questionId);
            });
        }

        return new ScoreMatrixData(sessionScores, allQuestions);
    }

    /**
     * Find competencies mapped to a specific Big Five trait via standardCodes.
     * Uses a native JSONB query to avoid loading all competencies into memory.
     */
    private List<Competency> findCompetenciesByBigFiveTrait(BigFiveTrait trait) {
        return competencyRepository.findByBigFiveTrait(trait.name());
    }

    /**
     * Calculate Cronbach's Alpha from a pre-built score matrix.
     * Uses aligned 90% completeness threshold and sample variance (÷(N-1)).
     */
    private BigDecimal calculateAlphaFromMatrix(
            Map<UUID, Map<UUID, BigDecimal>> sessionScores,
            Set<UUID> allQuestions) {

        int k = allQuestions.size();
        if (k < 2 || sessionScores.size() < MIN_RESPONSES) {
            return null;
        }

        List<UUID> questionList = new ArrayList<>(allQuestions);

        // Response completeness threshold: 90% for both competency and Big Five reliability
        // Aligned per APA Standards - ensures comparable alpha values across measurement levels.
        // Previous values: competency=100%, Big Five=80% (inconsistent, making comparisons unreliable)
        List<Map<UUID, BigDecimal>> completeScores = sessionScores.values().stream()
                .filter(scores -> scores.size() >= k * RESPONSE_COMPLETENESS_THRESHOLD)
                .collect(Collectors.toList());

        if (completeScores.size() < MIN_RESPONSES) {
            return null;
        }

        // Guard: sample variance requires n > 1 to avoid division by zero
        if (completeScores.size() <= 1) {
            return null;
        }

        BigDecimal nDecimal = BigDecimal.valueOf(completeScores.size());

        // Calculate item variances (sample variance, ÷(N-1))
        BigDecimal sumItemVariances = BigDecimal.ZERO;
        for (UUID questionId : questionList) {
            BigDecimal sum = BigDecimal.ZERO;
            int validCount = 0;
            for (Map<UUID, BigDecimal> scores : completeScores) {
                BigDecimal score = scores.get(questionId);
                if (score != null) {
                    sum = sum.add(score);
                    validCount++;
                }
            }

            if (validCount <= 1) continue; // Need at least 2 for sample variance

            BigDecimal mean = sum.divide(BigDecimal.valueOf(validCount), MATH_CONTEXT);

            BigDecimal variance = BigDecimal.ZERO;
            for (Map<UUID, BigDecimal> scores : completeScores) {
                BigDecimal score = scores.get(questionId);
                if (score != null) {
                    BigDecimal diff = score.subtract(mean);
                    variance = variance.add(diff.multiply(diff));
                }
            }
            // Sample variance: ÷(N-1) per APA Standards
            variance = variance.divide(BigDecimal.valueOf(validCount - 1), MATH_CONTEXT);
            sumItemVariances = sumItemVariances.add(variance);
        }

        // Calculate total variance (sample variance, ÷(N-1))
        BigDecimal totalMean = BigDecimal.ZERO;
        List<BigDecimal> totalScores = new ArrayList<>();

        for (Map<UUID, BigDecimal> scores : completeScores) {
            BigDecimal total = questionList.stream()
                    .map(q -> scores.getOrDefault(q, BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalScores.add(total);
            totalMean = totalMean.add(total);
        }
        totalMean = totalMean.divide(nDecimal, MATH_CONTEXT);

        BigDecimal nMinus1 = BigDecimal.valueOf(completeScores.size() - 1);
        BigDecimal totalVariance = BigDecimal.ZERO;
        for (BigDecimal total : totalScores) {
            BigDecimal diff = total.subtract(totalMean);
            totalVariance = totalVariance.add(diff.multiply(diff));
        }
        totalVariance = totalVariance.divide(nMinus1, MATH_CONTEXT);

        if (totalVariance.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        BigDecimal kDecimal = BigDecimal.valueOf(k);
        BigDecimal kMinus1Decimal = BigDecimal.valueOf(k - 1);
        BigDecimal factor = kDecimal.divide(kMinus1Decimal, MATH_CONTEXT);
        BigDecimal varianceRatio = sumItemVariances.divide(totalVariance, MATH_CONTEXT);

        return factor.multiply(BigDecimal.ONE.subtract(varianceRatio)).setScale(SCALE, RoundingMode.HALF_UP);
    }

    // ============================================
    // BATCH OPERATIONS
    // ============================================

    @Override
    public List<ItemStatistics> recalculateAllItems() {
        return recalculateItemsWithMinimumResponses(MIN_RESPONSES);
    }

    @Override
    public List<ItemStatistics> recalculateItemsWithMinimumResponses(int minResponses) {
        logger.info("Starting batch recalculation for items with >= {} responses", minResponses);

        List<AssessmentQuestion> questions = assessmentQuestionRepository.findAll();
        List<ItemStatistics> results = new ArrayList<>();

        int processed = 0;
        int updated = 0;

        for (AssessmentQuestion question : questions) {
            long responseCount = testAnswerRepository.countByQuestion_Id(question.getId());

            if (responseCount >= minResponses) {
                try {
                    ItemStatistics stats = calculateItemStatistics(question.getId());
                    results.add(stats);
                    updated++;
                } catch (Exception e) {
                    logger.error("Error calculating statistics for question {}: {}",
                            question.getId(), e.getMessage());
                }
            }
            processed++;

            if (processed % 100 == 0) {
                logger.info("Batch progress: {}/{} questions processed, {} updated",
                        processed, questions.size(), updated);
            }
        }

        logger.info("Batch recalculation complete: {} questions processed, {} updated",
                processed, updated);

        return results;
    }

    @Override
    public List<CompetencyReliability> recalculateAllCompetencies() {
        logger.info("Starting batch recalculation for all competency reliability");

        List<Competency> competencies = competencyRepository.findAll();
        List<CompetencyReliability> results = new ArrayList<>();

        for (Competency competency : competencies) {
            try {
                CompetencyReliability reliability = calculateCompetencyReliability(competency.getId());
                results.add(reliability);
            } catch (Exception e) {
                logger.error("Error calculating reliability for competency {}: {}",
                        competency.getId(), e.getMessage());
            }
        }

        logger.info("Competency reliability recalculation complete: {} competencies processed",
                results.size());

        return results;
    }

    @Override
    public List<BigFiveReliability> recalculateAllBigFiveTraits() {
        logger.info("Starting batch recalculation for all Big Five traits");

        List<BigFiveReliability> results = new ArrayList<>();

        for (BigFiveTrait trait : BigFiveTrait.values()) {
            try {
                BigFiveReliability reliability = calculateBigFiveReliability(trait);
                results.add(reliability);
            } catch (Exception e) {
                logger.error("Error calculating reliability for Big Five trait {}: {}",
                        trait, e.getMessage());
            }
        }

        logger.info("Big Five reliability recalculation complete: {} traits processed",
                results.size());

        return results;
    }

    // ============================================
    // ITEM STATUS MANAGEMENT
    // ============================================

    @Override
    public void updateItemValidityStatus(UUID questionId) {
        ItemStatistics stats = itemStatisticsRepository.findByQuestion_Id(questionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No statistics found for question: " + questionId));

        ItemValidityStatus newStatus = determineValidityStatus(
                stats.getDifficultyIndex(),
                stats.getDiscriminationIndex());

        if (stats.getResponseCount() < MIN_RESPONSES) {
            newStatus = ItemValidityStatus.PROBATION;
        }

        updateStatusWithHistory(stats, newStatus,
                generateStatusReason(stats.getDifficultyIndex(), stats.getDiscriminationIndex()));

        itemStatisticsRepository.save(stats);
    }

    @Override
    public List<ItemStatistics> getItemsRequiringReview() {
        return itemStatisticsRepository.findItemsRequiringReview();
    }

    @Override
    public void retireItem(UUID questionId, String reason) {
        ItemStatistics stats = itemStatisticsRepository.findByQuestion_Id(questionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No statistics found for question: " + questionId));

        updateStatusWithHistory(stats, ItemValidityStatus.RETIRED, "Manual retirement: " + reason);

        // Deactivate the question
        AssessmentQuestion question = assessmentQuestionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));
        question.setActive(false);

        assessmentQuestionRepository.save(question);
        itemStatisticsRepository.save(stats);

        logger.info("Item {} retired. Reason: {}", questionId, reason);
    }

    @Override
    public void activateItem(UUID questionId) {
        ItemStatistics stats = itemStatisticsRepository.findByQuestion_Id(questionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No statistics found for question: " + questionId));

        // Verify activation criteria
        if (stats.getResponseCount() < MIN_RESPONSES) {
            throw new IllegalStateException(
                    "Cannot activate item with insufficient responses: " + stats.getResponseCount());
        }

        if (stats.getDiscriminationIndex() != null
                && stats.getDiscriminationIndex().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Cannot activate item with negative discrimination index");
        }

        if (stats.getDiscriminationIndex() != null
                && stats.getDiscriminationIndex().compareTo(DISCRIMINATION_EXCELLENT) < 0) {
            throw new IllegalStateException(
                    "Cannot activate item with discrimination index below " + DISCRIMINATION_EXCELLENT);
        }

        updateStatusWithHistory(stats, ItemValidityStatus.ACTIVE, "Manual activation");

        // Activate the question
        AssessmentQuestion question = assessmentQuestionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));
        question.setActive(true);

        assessmentQuestionRepository.save(question);
        itemStatisticsRepository.save(stats);

        logger.info("Item {} activated", questionId);
    }

    // ============================================
    // HEALTH REPORTING
    // ============================================

    @Override
    @Transactional(readOnly = true)
    public PsychometricHealthReport generateHealthReport() {
        logger.debug("Generating psychometric health report");

        // Item statistics counts
        int totalItems = (int) itemStatisticsRepository.count();
        int activeItems = (int) itemStatisticsRepository.countByValidityStatus(ItemValidityStatus.ACTIVE);
        int probationItems = (int) itemStatisticsRepository.countByValidityStatus(ItemValidityStatus.PROBATION);
        int flaggedItems = (int) itemStatisticsRepository.countByValidityStatus(ItemValidityStatus.FLAGGED_FOR_REVIEW);
        int retiredItems = (int) itemStatisticsRepository.countByValidityStatus(ItemValidityStatus.RETIRED);

        // Competency reliability counts
        int totalCompetencies = (int) competencyReliabilityRepository.count();
        int reliableCompetencies = (int) competencyReliabilityRepository.countByReliabilityStatus(ReliabilityStatus.RELIABLE);
        int acceptableCompetencies = (int) competencyReliabilityRepository.countByReliabilityStatus(ReliabilityStatus.ACCEPTABLE);
        int unreliableCompetencies = (int) competencyReliabilityRepository.countByReliabilityStatus(ReliabilityStatus.UNRELIABLE);
        int insufficientDataCompetencies = (int) competencyReliabilityRepository.countByReliabilityStatus(ReliabilityStatus.INSUFFICIENT_DATA);

        // Average metrics
        Double avgAlpha = competencyReliabilityRepository.calculateAverageAlpha();
        BigDecimal averageAlpha = avgAlpha != null ? BigDecimal.valueOf(avgAlpha).setScale(SCALE, RoundingMode.HALF_UP) : null;

        // Calculate average discrimination from item statistics
        BigDecimal averageDiscrimination = calculateAverageDiscrimination();

        // Top flagged items
        List<FlaggedItemSummary> topFlaggedItems = getTopFlaggedItems(10);

        // Big Five summary
        PsychometricHealthReport.BigFiveReliabilitySummary bigFiveSummary = generateBigFiveSummary();

        return PsychometricHealthReport.builder()
                .totalItems(totalItems)
                .activeItems(activeItems)
                .probationItems(probationItems)
                .flaggedItems(flaggedItems)
                .retiredItems(retiredItems)
                .totalCompetencies(totalCompetencies)
                .reliableCompetencies(reliableCompetencies)
                .acceptableCompetencies(acceptableCompetencies)
                .unreliableCompetencies(unreliableCompetencies)
                .insufficientDataCompetencies(insufficientDataCompetencies)
                .averageAlpha(averageAlpha)
                .averageDiscrimination(averageDiscrimination)
                .topFlaggedItems(topFlaggedItems)
                .bigFiveReliabilitySummary(bigFiveSummary)
                .lastAuditRun(LocalDateTime.now())
                .itemsAnalyzedSinceLastAudit(
                        (int) itemStatisticsRepository.countByLastCalculatedAtAfter(
                                LocalDateTime.now().minusDays(1)))
                .build();
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    /**
     * Calculate Pearson correlation coefficient between two lists.
     */
    private BigDecimal calculatePearsonCorrelation(List<Double> x, List<Double> y) {
        if (x.size() != y.size() || x.isEmpty()) {
            return null;
        }

        int n = x.size();

        // Calculate means
        double sumX = 0, sumY = 0;
        for (int i = 0; i < n; i++) {
            sumX += x.get(i);
            sumY += y.get(i);
        }
        double meanX = sumX / n;
        double meanY = sumY / n;

        // Calculate correlation components
        double numerator = 0;
        double sumSqDiffX = 0;
        double sumSqDiffY = 0;

        for (int i = 0; i < n; i++) {
            double diffX = x.get(i) - meanX;
            double diffY = y.get(i) - meanY;
            numerator += diffX * diffY;
            sumSqDiffX += diffX * diffX;
            sumSqDiffY += diffY * diffY;
        }

        double denominator = Math.sqrt(sumSqDiffX * sumSqDiffY);

        if (denominator == 0) {
            return null;
        }

        double correlation = numerator / denominator;

        return BigDecimal.valueOf(correlation).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Determine the difficulty flag based on p-value.
     */
    private DifficultyFlag determineDifficultyFlag(BigDecimal difficultyIndex) {
        if (difficultyIndex == null) {
            return DifficultyFlag.NONE;
        }

        if (difficultyIndex.compareTo(DIFFICULTY_TOO_HARD) < 0) {
            return DifficultyFlag.TOO_HARD;
        }
        if (difficultyIndex.compareTo(DIFFICULTY_TOO_EASY) > 0) {
            return DifficultyFlag.TOO_EASY;
        }
        return DifficultyFlag.NONE;
    }

    /**
     * Determine the discrimination flag based on rpb value.
     */
    private DiscriminationFlag determineDiscriminationFlag(BigDecimal discriminationIndex) {
        if (discriminationIndex == null) {
            return DiscriminationFlag.NONE;
        }

        if (discriminationIndex.compareTo(BigDecimal.ZERO) < 0) {
            return DiscriminationFlag.NEGATIVE;
        }
        if (discriminationIndex.compareTo(DISCRIMINATION_CRITICAL) < 0) {
            return DiscriminationFlag.CRITICAL;
        }
        if (discriminationIndex.compareTo(DISCRIMINATION_GOOD) < 0) {
            return DiscriminationFlag.WARNING;
        }
        return DiscriminationFlag.NONE;
    }

    /**
     * Determine validity status based on psychometric metrics.
     */
    private ItemValidityStatus determineValidityStatus(BigDecimal difficultyIndex, BigDecimal discriminationIndex) {
        // Toxic items are auto-retired
        if (discriminationIndex != null && discriminationIndex.compareTo(BigDecimal.ZERO) < 0) {
            return ItemValidityStatus.RETIRED;
        }

        // Check for excellent discrimination (ACTIVE threshold)
        if (discriminationIndex != null && discriminationIndex.compareTo(DISCRIMINATION_EXCELLENT) >= 0) {
            // Check if difficulty is in acceptable range
            if (difficultyIndex != null
                    && difficultyIndex.compareTo(DIFFICULTY_TOO_HARD) >= 0
                    && difficultyIndex.compareTo(DIFFICULTY_TOO_EASY) <= 0) {
                return ItemValidityStatus.ACTIVE;
            }
        }

        // Flag items with marginal metrics or extreme difficulty
        if (discriminationIndex != null && discriminationIndex.compareTo(DISCRIMINATION_WARNING) >= 0) {
            if (difficultyIndex != null
                    && (difficultyIndex.compareTo(DIFFICULTY_TOO_HARD) < 0
                    || difficultyIndex.compareTo(DIFFICULTY_TOO_EASY) > 0)) {
                return ItemValidityStatus.FLAGGED_FOR_REVIEW;
            }
        }

        // Default to flagged for review if metrics don't meet ACTIVE criteria
        if (discriminationIndex != null) {
            return ItemValidityStatus.FLAGGED_FOR_REVIEW;
        }

        return ItemValidityStatus.PROBATION;
    }

    /**
     * Determine reliability status based on Cronbach's Alpha.
     */
    private ReliabilityStatus determineReliabilityStatus(BigDecimal alpha, int sampleSize, int itemCount) {
        if (sampleSize < MIN_RESPONSES || itemCount < 2) {
            return ReliabilityStatus.INSUFFICIENT_DATA;
        }

        if (alpha == null) {
            return ReliabilityStatus.INSUFFICIENT_DATA;
        }

        if (alpha.compareTo(ALPHA_RELIABLE) >= 0) {
            return ReliabilityStatus.RELIABLE;
        }
        if (alpha.compareTo(ALPHA_ACCEPTABLE) >= 0) {
            return ReliabilityStatus.ACCEPTABLE;
        }
        return ReliabilityStatus.UNRELIABLE;
    }

    /**
     * Generate a human-readable reason for status change.
     */
    private String generateStatusReason(BigDecimal difficultyIndex, BigDecimal discriminationIndex) {
        StringBuilder reason = new StringBuilder();

        if (discriminationIndex != null) {
            reason.append("rpb=").append(discriminationIndex.setScale(3, RoundingMode.HALF_UP));

            if (discriminationIndex.compareTo(BigDecimal.ZERO) < 0) {
                reason.append(" (toxic)");
            } else if (discriminationIndex.compareTo(DISCRIMINATION_EXCELLENT) >= 0) {
                reason.append(" (excellent)");
            } else if (discriminationIndex.compareTo(DISCRIMINATION_GOOD) >= 0) {
                reason.append(" (good)");
            } else if (discriminationIndex.compareTo(DISCRIMINATION_WARNING) >= 0) {
                reason.append(" (marginal)");
            } else {
                reason.append(" (poor)");
            }
        }

        if (difficultyIndex != null) {
            if (reason.length() > 0) reason.append(", ");
            reason.append("p=").append(difficultyIndex.setScale(3, RoundingMode.HALF_UP));

            if (difficultyIndex.compareTo(DIFFICULTY_TOO_HARD) < 0) {
                reason.append(" (too hard)");
            } else if (difficultyIndex.compareTo(DIFFICULTY_TOO_EASY) > 0) {
                reason.append(" (too easy)");
            } else {
                reason.append(" (acceptable)");
            }
        }

        return reason.toString();
    }

    /**
     * Update status with audit history tracking.
     */
    private void updateStatusWithHistory(ItemStatistics stats, ItemValidityStatus newStatus, String reason) {
        ItemValidityStatus oldStatus = stats.getValidityStatus();

        if (oldStatus != newStatus) {
            stats.addStatusChange(oldStatus, newStatus, LocalDateTime.now(), reason);
            stats.setValidityStatus(newStatus);
        }
    }

    /**
     * Calculate average discrimination index across all items.
     */
    private BigDecimal calculateAverageDiscrimination() {
        List<ItemStatistics> allStats = itemStatisticsRepository.findAll();

        if (allStats.isEmpty()) {
            return null;
        }

        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;

        for (ItemStatistics stats : allStats) {
            if (stats.getDiscriminationIndex() != null) {
                sum = sum.add(stats.getDiscriminationIndex());
                count++;
            }
        }

        if (count == 0) {
            return null;
        }

        return sum.divide(BigDecimal.valueOf(count), MATH_CONTEXT)
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Get top flagged items sorted by severity.
     */
    private List<FlaggedItemSummary> getTopFlaggedItems(int limit) {
        List<ItemStatistics> flaggedItems = itemStatisticsRepository.findItemsRequiringReview();

        // Also include problematic items
        List<ItemStatistics> problematicItems = itemStatisticsRepository.findProblematicItems();

        // Combine and deduplicate
        Set<UUID> seenIds = new HashSet<>();
        List<ItemStatistics> allFlagged = new ArrayList<>();

        for (ItemStatistics item : flaggedItems) {
            if (seenIds.add(item.getId())) {
                allFlagged.add(item);
            }
        }
        for (ItemStatistics item : problematicItems) {
            if (seenIds.add(item.getId())) {
                allFlagged.add(item);
            }
        }

        // Convert to summaries and sort by severity
        return allFlagged.stream()
                .map(this::toFlaggedItemSummary)
                .sorted((a, b) -> Integer.compare(b.getSeverityLevel(), a.getSeverityLevel()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Convert ItemStatistics to FlaggedItemSummary.
     */
    private FlaggedItemSummary toFlaggedItemSummary(ItemStatistics stats) {
        AssessmentQuestion question = stats.getQuestion();
        BehavioralIndicator indicator = question != null ? question.getBehavioralIndicator() : null;
        Competency competency = indicator != null ? indicator.getCompetency() : null;

        return new FlaggedItemSummary(
                stats.getQuestionId(),
                FlaggedItemSummary.truncateQuestionText(question != null ? question.getQuestionText() : null),
                competency != null ? competency.getName() : "Unknown",
                indicator != null ? indicator.getTitle() : "Unknown",
                stats.getDifficultyIndex(),
                stats.getDiscriminationIndex(),
                stats.getResponseCount(),
                stats.getValidityStatus(),
                stats.getDifficultyFlag(),
                stats.getDiscriminationFlag(),
                stats.getLastCalculatedAt()
        );
    }

    /**
     * Generate Big Five reliability summary.
     */
    private PsychometricHealthReport.BigFiveReliabilitySummary generateBigFiveSummary() {
        List<BigFiveReliability> allTraits = bigFiveReliabilityRepository.findAll();

        int total = allTraits.size();
        int reliable = (int) allTraits.stream()
                .filter(t -> t.getReliabilityStatus() == ReliabilityStatus.RELIABLE)
                .count();
        int unreliable = (int) allTraits.stream()
                .filter(t -> t.getReliabilityStatus() == ReliabilityStatus.UNRELIABLE)
                .count();
        int insufficient = (int) allTraits.stream()
                .filter(t -> t.getReliabilityStatus() == ReliabilityStatus.INSUFFICIENT_DATA)
                .count();

        Double avgAlpha = bigFiveReliabilityRepository.calculateAverageAlpha();
        BigDecimal averageAlpha = avgAlpha != null
                ? BigDecimal.valueOf(avgAlpha).setScale(SCALE, RoundingMode.HALF_UP)
                : null;

        // Find lowest alpha trait
        Optional<BigFiveReliability> lowestTrait = bigFiveReliabilityRepository.findLowestAlphaTrait();
        String lowestAlphaTrait = lowestTrait.map(t -> t.getTrait().name()).orElse(null);
        BigDecimal lowestAlphaValue = lowestTrait.map(BigFiveReliability::getCronbachAlpha).orElse(null);

        return new PsychometricHealthReport.BigFiveReliabilitySummary(
                total,
                reliable,
                unreliable,
                insufficient,
                averageAlpha,
                lowestAlphaTrait,
                lowestAlphaValue
        );
    }
}
