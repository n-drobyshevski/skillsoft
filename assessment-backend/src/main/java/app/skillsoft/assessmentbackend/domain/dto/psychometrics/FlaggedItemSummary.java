package app.skillsoft.assessmentbackend.domain.dto.psychometrics;

import app.skillsoft.assessmentbackend.domain.entities.DifficultyFlag;
import app.skillsoft.assessmentbackend.domain.entities.DiscriminationFlag;
import app.skillsoft.assessmentbackend.domain.entities.ItemValidityStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Summary DTO for flagged assessment items requiring review.
 * <p>
 * Provides a concise view of problematic items for the psychometric health dashboard.
 *
 * @param questionId          UUID of the assessment question
 * @param questionText        first 100 characters of the question text
 * @param competencyName      name of the parent competency
 * @param indicatorTitle      title of the behavioral indicator
 * @param difficultyIndex     p-value (0.0 - 1.0)
 * @param discriminationIndex point-biserial correlation (-1.0 to 1.0)
 * @param responseCount       number of responses used in calculation
 * @param validityStatus      current item validity status
 * @param difficultyFlag      difficulty issue flag (TOO_HARD, TOO_EASY, NONE)
 * @param discriminationFlag  discrimination issue flag (NEGATIVE, CRITICAL, WARNING, NONE)
 * @param lastCalculatedAt    timestamp of last psychometric calculation
 */
public record FlaggedItemSummary(
        UUID questionId,
        String questionText,
        String competencyName,
        String indicatorTitle,
        BigDecimal difficultyIndex,
        BigDecimal discriminationIndex,
        Integer responseCount,
        ItemValidityStatus validityStatus,
        DifficultyFlag difficultyFlag,
        DiscriminationFlag discriminationFlag,
        LocalDateTime lastCalculatedAt
) {

    /**
     * Creates a truncated question text preview (first 100 chars).
     *
     * @param fullText the full question text
     * @return truncated text with ellipsis if needed
     */
    public static String truncateQuestionText(String fullText) {
        if (fullText == null) return "";
        if (fullText.length() <= 100) return fullText;
        return fullText.substring(0, 97) + "...";
    }

    /**
     * Determines the severity level of the flag for prioritization.
     * <p>
     * Severity levels:
     * <ul>
     *   <li>CRITICAL (3): Negative discrimination (toxic item)</li>
     *   <li>HIGH (2): Critical discrimination or extreme difficulty</li>
     *   <li>MEDIUM (1): Warning discrimination</li>
     *   <li>LOW (0): Minor issues</li>
     * </ul>
     *
     * @return severity score (0-3)
     */
    public int getSeverityLevel() {
        if (discriminationFlag == DiscriminationFlag.NEGATIVE) {
            return 3; // Critical - toxic item
        }
        if (discriminationFlag == DiscriminationFlag.CRITICAL) {
            return 2; // High - poor discrimination
        }
        if (difficultyFlag == DifficultyFlag.TOO_HARD || difficultyFlag == DifficultyFlag.TOO_EASY) {
            return 2; // High - extreme difficulty
        }
        if (discriminationFlag == DiscriminationFlag.WARNING) {
            return 1; // Medium - marginal discrimination
        }
        return 0; // Low - minor issues
    }

    /**
     * Gets a human-readable description of the primary issue.
     *
     * @return description of the main problem with this item
     */
    public String getPrimaryIssue() {
        if (discriminationFlag == DiscriminationFlag.NEGATIVE) {
            return "Toxic item: High performers fail, low performers succeed";
        }
        if (discriminationFlag == DiscriminationFlag.CRITICAL) {
            return "Poor discrimination: Item does not differentiate skill levels";
        }
        if (difficultyFlag == DifficultyFlag.TOO_HARD) {
            return "Too difficult: Most respondents fail this question";
        }
        if (difficultyFlag == DifficultyFlag.TOO_EASY) {
            return "Too easy: Most respondents answer correctly";
        }
        if (discriminationFlag == DiscriminationFlag.WARNING) {
            return "Marginal discrimination: Item has limited differentiating power";
        }
        return "Under review for potential issues";
    }
}
