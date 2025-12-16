package app.skillsoft.assessmentbackend.services.psychometrics;

import app.skillsoft.assessmentbackend.domain.dto.psychometrics.PsychometricHealthReport;
import app.skillsoft.assessmentbackend.domain.entities.BigFiveReliability;
import app.skillsoft.assessmentbackend.domain.entities.BigFiveTrait;
import app.skillsoft.assessmentbackend.domain.entities.CompetencyReliability;
import app.skillsoft.assessmentbackend.domain.entities.ItemStatistics;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for performing psychometric analysis on assessment questions and competencies.
 * <p>
 * Implements classical test theory (CTT) metrics including:
 * <ul>
 *   <li>Item-level analysis: Difficulty Index (p-value), Discrimination Index (Point-Biserial)</li>
 *   <li>Scale-level analysis: Cronbach's Alpha (internal consistency)</li>
 *   <li>Item validity status management based on psychometric thresholds</li>
 * </ul>
 * <p>
 * Thresholds follow standard psychometric guidelines:
 * <ul>
 *   <li>Difficulty: 0.2 - 0.9 acceptable range (p-value)</li>
 *   <li>Discrimination: >= 0.3 excellent, >= 0.25 good, < 0 toxic</li>
 *   <li>Cronbach's Alpha: >= 0.7 reliable, >= 0.6 acceptable</li>
 *   <li>Minimum 50 responses required for reliable analysis</li>
 * </ul>
 *
 * @see ItemStatistics
 * @see CompetencyReliability
 * @see BigFiveReliability
 */
public interface PsychometricAnalysisService {

    // ============================================
    // ITEM-LEVEL ANALYSIS
    // ============================================

    /**
     * Calculate and persist all psychometric statistics for a single question.
     * <p>
     * Performs comprehensive item analysis including:
     * <ul>
     *   <li>Difficulty index calculation</li>
     *   <li>Discrimination index calculation</li>
     *   <li>Distractor analysis (for MCQ/SJT questions)</li>
     *   <li>Validity status determination</li>
     *   <li>Flag assignment based on thresholds</li>
     * </ul>
     *
     * @param questionId the UUID of the assessment question
     * @return updated ItemStatistics entity with calculated metrics
     * @throws IllegalArgumentException if question not found
     */
    ItemStatistics calculateItemStatistics(UUID questionId);

    /**
     * Calculate the difficulty index (p-value) for a question.
     * <p>
     * The difficulty index represents the average normalized score across all responses.
     * Range: 0.0 (impossible) to 1.0 (everyone succeeds)
     * <ul>
     *   <li>p < 0.2: Too hard - most respondents fail</li>
     *   <li>0.2 <= p <= 0.9: Acceptable difficulty range</li>
     *   <li>p > 0.9: Too easy - most respondents succeed</li>
     * </ul>
     *
     * @param questionId the UUID of the assessment question
     * @return difficulty index as BigDecimal (0.0 - 1.0), or null if insufficient data
     */
    BigDecimal calculateDifficultyIndex(UUID questionId);

    /**
     * Calculate the discrimination index using Point-Biserial correlation.
     * <p>
     * The Point-Biserial correlation (rpb) measures how well an item discriminates
     * between high and low performers on the overall test.
     * <p>
     * Formula: rpb = (M_p - M_q) / S_t * sqrt(p * q)
     * <p>
     * Interpretation:
     * <ul>
     *   <li>rpb < 0: Toxic - high performers get it wrong, low performers get it right</li>
     *   <li>0 <= rpb < 0.1: Critical - poor discrimination</li>
     *   <li>0.1 <= rpb < 0.25: Warning - marginal discrimination</li>
     *   <li>rpb >= 0.25: Good discrimination</li>
     *   <li>rpb >= 0.3: Excellent discrimination (ACTIVE status threshold)</li>
     * </ul>
     *
     * @param questionId the UUID of the assessment question
     * @return discrimination index as BigDecimal (-1.0 to 1.0), or null if insufficient data
     */
    BigDecimal calculateDiscriminationIndex(UUID questionId);

    /**
     * Analyze distractor effectiveness for MCQ/SJT questions.
     * <p>
     * Returns the selection percentage for each answer option.
     * Flags non-functioning distractors (0% selection).
     *
     * @param questionId the UUID of the assessment question
     * @return map of option ID to selection percentage (0.0 - 1.0)
     */
    Map<String, Double> analyzeDistractors(UUID questionId);

    // ============================================
    // COMPETENCY-LEVEL ANALYSIS
    // ============================================

    /**
     * Calculate and persist Cronbach's Alpha for a competency.
     * <p>
     * Cronbach's Alpha measures internal consistency - how well the items
     * in a scale measure the same underlying construct.
     *
     * @param competencyId the UUID of the competency
     * @return updated CompetencyReliability entity with calculated metrics
     * @throws IllegalArgumentException if competency not found
     */
    CompetencyReliability calculateCompetencyReliability(UUID competencyId);

    /**
     * Calculate Cronbach's Alpha coefficient for a competency.
     * <p>
     * Formula: alpha = (k / (k-1)) * (1 - sum(var_i) / var_total)
     * <p>
     * where:
     * <ul>
     *   <li>k = number of items</li>
     *   <li>var_i = variance of each item</li>
     *   <li>var_total = variance of total scores</li>
     * </ul>
     * <p>
     * Interpretation:
     * <ul>
     *   <li>alpha >= 0.9: Excellent</li>
     *   <li>0.8 <= alpha < 0.9: Good</li>
     *   <li>0.7 <= alpha < 0.8: Acceptable (RELIABLE threshold)</li>
     *   <li>0.6 <= alpha < 0.7: Questionable (ACCEPTABLE threshold)</li>
     *   <li>alpha < 0.6: Poor (UNRELIABLE)</li>
     * </ul>
     *
     * @param competencyId the UUID of the competency
     * @return Cronbach's Alpha as BigDecimal, or null if insufficient data
     */
    BigDecimal calculateCronbachAlpha(UUID competencyId);

    /**
     * Calculate Alpha-if-Item-Deleted for all items in a competency.
     * <p>
     * For each item, calculates what Cronbach's Alpha would be if that item
     * were removed from the scale. Useful for identifying items that are
     * lowering overall reliability.
     * <p>
     * If alphaIfDeleted[item] > currentAlpha, removing that item would
     * improve reliability.
     *
     * @param competencyId the UUID of the competency
     * @return map of question UUID to alpha-if-deleted value
     */
    Map<UUID, BigDecimal> calculateAlphaIfDeleted(UUID competencyId);

    // ============================================
    // BIG FIVE TRAIT-LEVEL ANALYSIS
    // ============================================

    /**
     * Calculate and persist Cronbach's Alpha for a Big Five personality trait.
     * <p>
     * Aggregates items from all competencies mapped to the specified Big Five dimension
     * via their standardCodes.bigFiveRef mapping.
     *
     * @param trait the Big Five personality trait
     * @return updated BigFiveReliability entity with calculated metrics
     */
    BigFiveReliability calculateBigFiveReliability(BigFiveTrait trait);

    // ============================================
    // BATCH OPERATIONS
    // ============================================

    /**
     * Recalculate psychometric statistics for all questions with sufficient responses.
     * <p>
     * Processes all questions that have at least 50 responses.
     * Updates ItemStatistics entities and validity statuses.
     *
     * @return list of updated ItemStatistics entities
     */
    List<ItemStatistics> recalculateAllItems();

    /**
     * Recalculate statistics for items meeting a minimum response threshold.
     * <p>
     * Used for targeted recalculation with configurable thresholds.
     *
     * @param minResponses minimum number of responses required
     * @return list of updated ItemStatistics entities
     */
    List<ItemStatistics> recalculateItemsWithMinimumResponses(int minResponses);

    /**
     * Recalculate Cronbach's Alpha for all competencies.
     *
     * @return list of updated CompetencyReliability entities
     */
    List<CompetencyReliability> recalculateAllCompetencies();

    /**
     * Recalculate Cronbach's Alpha for all Big Five traits.
     *
     * @return list of updated BigFiveReliability entities
     */
    List<BigFiveReliability> recalculateAllBigFiveTraits();

    // ============================================
    // ITEM STATUS MANAGEMENT
    // ============================================

    /**
     * Update the validity status of a question based on its psychometric metrics.
     * <p>
     * Status determination logic:
     * <ul>
     *   <li>PROBATION: responseCount < 50</li>
     *   <li>RETIRED: rpb < 0 (toxic discrimination)</li>
     *   <li>FLAGGED_FOR_REVIEW: rpb < 0.3 with extreme difficulty (p < 0.2 or p > 0.9)</li>
     *   <li>ACTIVE: rpb >= 0.3 and 0.2 <= p <= 0.9</li>
     * </ul>
     * <p>
     * Records status change in audit history.
     *
     * @param questionId the UUID of the assessment question
     */
    void updateItemValidityStatus(UUID questionId);

    /**
     * Get all items flagged for manual HR review.
     * <p>
     * Returns items with FLAGGED_FOR_REVIEW status that require
     * human evaluation before being retired or activated.
     *
     * @return list of ItemStatistics requiring review
     */
    List<ItemStatistics> getItemsRequiringReview();

    /**
     * Manually retire an item with a specified reason.
     * <p>
     * Sets validity status to RETIRED and records the reason in audit history.
     * Deactivates the associated question.
     *
     * @param questionId the UUID of the assessment question
     * @param reason human-readable reason for retirement
     */
    void retireItem(UUID questionId, String reason);

    /**
     * Manually activate an item that was previously flagged or on probation.
     * <p>
     * Requires that the item has sufficient responses and acceptable metrics.
     *
     * @param questionId the UUID of the assessment question
     * @throws IllegalStateException if item does not meet activation criteria
     */
    void activateItem(UUID questionId);

    // ============================================
    // HEALTH REPORTING
    // ============================================

    /**
     * Generate a comprehensive psychometric health report.
     * <p>
     * Provides a summary of:
     * <ul>
     *   <li>Item status distribution (active, probation, flagged, retired)</li>
     *   <li>Competency reliability distribution</li>
     *   <li>Top flagged items requiring attention</li>
     *   <li>Overall assessment health metrics</li>
     * </ul>
     *
     * @return PsychometricHealthReport with aggregated metrics
     */
    PsychometricHealthReport generateHealthReport();
}
