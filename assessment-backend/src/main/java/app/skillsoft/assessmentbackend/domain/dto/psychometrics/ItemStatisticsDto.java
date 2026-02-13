package app.skillsoft.assessmentbackend.domain.dto.psychometrics;

import app.skillsoft.assessmentbackend.domain.entities.DifficultyFlag;
import app.skillsoft.assessmentbackend.domain.entities.DiscriminationFlag;
import app.skillsoft.assessmentbackend.domain.entities.ItemValidityStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for item-level psychometric statistics.
 * <p>
 * Provides a summary view of psychometric metrics for assessment questions,
 * suitable for list views and dashboards.
 *
 * @param id                  UUID of the ItemStatistics record
 * @param questionId          UUID of the associated assessment question
 * @param questionText        text of the question (first 100 characters)
 * @param competencyName      name of the parent competency
 * @param indicatorTitle      title of the behavioral indicator
 * @param difficultyIndex     p-value (0.0 - 1.0) representing item difficulty
 * @param discriminationIndex point-biserial correlation (-1.0 to 1.0)
 * @param responseCount       number of responses used in calculation
 * @param validityStatus      current item validity status
 * @param difficultyFlag      difficulty issue flag (TOO_HARD, TOO_EASY, NONE)
 * @param discriminationFlag  discrimination issue flag (NEGATIVE, CRITICAL, WARNING, NONE)
 * @param lastCalculatedAt    timestamp of last psychometric calculation
 */
public record ItemStatisticsDto(
        UUID id,
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
     * Check if this item has any psychometric issues.
     *
     * @return true if item has difficulty or discrimination flags
     */
    public boolean hasIssues() {
        return (difficultyFlag != null && difficultyFlag != DifficultyFlag.NONE)
                || (discriminationFlag != null && discriminationFlag != DiscriminationFlag.NONE);
    }

    /**
     * Check if item needs urgent attention (toxic or flagged).
     *
     * @return true if item is toxic or flagged for review
     */
    public boolean needsUrgentAttention() {
        return discriminationFlag == DiscriminationFlag.NEGATIVE
                || validityStatus == ItemValidityStatus.FLAGGED_FOR_REVIEW;
    }

    /**
     * Get a human-readable status description.
     *
     * @return description of the item's current status
     */
    public String getStatusDescription() {
        if (validityStatus == null) {
            return "Unknown status";
        }
        return validityStatus.getDisplayName() + " - " + validityStatus.getDescription();
    }

    /**
     * Check if the item has sufficient data for reliable analysis.
     *
     * @return true if responseCount >= 50
     */
    public boolean hasSufficientData() {
        return responseCount != null && responseCount >= 50;
    }

    /**
     * Builder for constructing ItemStatisticsDto.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for ItemStatisticsDto.
     */
    public static class Builder {
        private UUID id;
        private UUID questionId;
        private String questionText;
        private String competencyName;
        private String indicatorTitle;
        private BigDecimal difficultyIndex;
        private BigDecimal discriminationIndex;
        private Integer responseCount;
        private ItemValidityStatus validityStatus;
        private DifficultyFlag difficultyFlag;
        private DiscriminationFlag discriminationFlag;
        private LocalDateTime lastCalculatedAt;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder questionId(UUID questionId) {
            this.questionId = questionId;
            return this;
        }

        public Builder questionText(String questionText) {
            this.questionText = questionText;
            return this;
        }

        public Builder competencyName(String competencyName) {
            this.competencyName = competencyName;
            return this;
        }

        public Builder indicatorTitle(String indicatorTitle) {
            this.indicatorTitle = indicatorTitle;
            return this;
        }

        public Builder difficultyIndex(BigDecimal difficultyIndex) {
            this.difficultyIndex = difficultyIndex;
            return this;
        }

        public Builder discriminationIndex(BigDecimal discriminationIndex) {
            this.discriminationIndex = discriminationIndex;
            return this;
        }

        public Builder responseCount(Integer responseCount) {
            this.responseCount = responseCount;
            return this;
        }

        public Builder validityStatus(ItemValidityStatus validityStatus) {
            this.validityStatus = validityStatus;
            return this;
        }

        public Builder difficultyFlag(DifficultyFlag difficultyFlag) {
            this.difficultyFlag = difficultyFlag;
            return this;
        }

        public Builder discriminationFlag(DiscriminationFlag discriminationFlag) {
            this.discriminationFlag = discriminationFlag;
            return this;
        }

        public Builder lastCalculatedAt(LocalDateTime lastCalculatedAt) {
            this.lastCalculatedAt = lastCalculatedAt;
            return this;
        }

        public ItemStatisticsDto build() {
            return new ItemStatisticsDto(
                    id, questionId, questionText, competencyName, indicatorTitle,
                    difficultyIndex, discriminationIndex, responseCount,
                    validityStatus, difficultyFlag, discriminationFlag, lastCalculatedAt
            );
        }
    }
}
