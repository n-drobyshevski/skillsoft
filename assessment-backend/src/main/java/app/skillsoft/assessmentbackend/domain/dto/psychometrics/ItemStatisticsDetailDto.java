package app.skillsoft.assessmentbackend.domain.dto.psychometrics;

import app.skillsoft.assessmentbackend.domain.entities.DifficultyFlag;
import app.skillsoft.assessmentbackend.domain.entities.DiscriminationFlag;
import app.skillsoft.assessmentbackend.domain.entities.ItemValidityStatus;
import app.skillsoft.assessmentbackend.domain.entities.QuestionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Detailed DTO for item-level psychometric statistics.
 * <p>
 * Extends ItemStatisticsDto with additional details for single-item views,
 * including distractor analysis, status change history, and recommendations.
 *
 * @param id                        UUID of the ItemStatistics record
 * @param questionId                UUID of the associated assessment question
 * @param questionText              full text of the question
 * @param questionType              type of question (LIKERT, SITUATIONAL_JUDGMENT, MULTIPLE_CHOICE)
 * @param competencyId              UUID of the parent competency
 * @param competencyName            name of the parent competency
 * @param indicatorId               UUID of the behavioral indicator
 * @param indicatorTitle            title of the behavioral indicator
 * @param difficultyIndex           p-value (0.0 - 1.0) representing item difficulty
 * @param discriminationIndex       point-biserial correlation (-1.0 to 1.0)
 * @param previousDiscriminationIndex previous discrimination value for trend analysis
 * @param responseCount             number of responses used in calculation
 * @param validityStatus            current item validity status
 * @param difficultyFlag            difficulty issue flag (TOO_HARD, TOO_EASY, NONE)
 * @param discriminationFlag        discrimination issue flag (NEGATIVE, CRITICAL, WARNING, NONE)
 * @param distractorEfficiency      map of option ID to selection percentage (MCQ/SJT only)
 * @param statusChangeHistory       history of status changes for audit trail
 * @param recommendations           list of improvement recommendations based on flags
 * @param lastCalculatedAt          timestamp of last psychometric calculation
 */
public record ItemStatisticsDetailDto(
        // Core Identifiers
        UUID id,
        UUID questionId,
        String questionText,
        QuestionType questionType,

        // Competency/Indicator Context
        UUID competencyId,
        String competencyName,
        UUID indicatorId,
        String indicatorTitle,

        // Psychometric Metrics
        BigDecimal difficultyIndex,
        BigDecimal discriminationIndex,
        BigDecimal previousDiscriminationIndex,
        Integer responseCount,

        // Status and Flags
        ItemValidityStatus validityStatus,
        DifficultyFlag difficultyFlag,
        DiscriminationFlag discriminationFlag,

        // Extended Details
        Map<String, Double> distractorEfficiency,
        List<StatusChangeEntry> statusChangeHistory,
        List<String> recommendations,

        // Metadata
        LocalDateTime lastCalculatedAt
) {

    /**
     * Calculate discrimination trend (positive = improving, negative = declining).
     *
     * @return change in discrimination index, null if no previous value
     */
    public BigDecimal getDiscriminationTrend() {
        if (discriminationIndex == null || previousDiscriminationIndex == null) {
            return null;
        }
        return discriminationIndex.subtract(previousDiscriminationIndex);
    }

    /**
     * Check if discrimination is declining.
     *
     * @return true if current discrimination is lower than previous
     */
    public boolean isDiscriminationDeclining() {
        BigDecimal trend = getDiscriminationTrend();
        return trend != null && trend.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Check if any distractors are non-functioning (0% selection).
     *
     * @return true if any distractor has 0% selection rate
     */
    public boolean hasNonFunctioningDistractors() {
        if (distractorEfficiency == null || distractorEfficiency.isEmpty()) {
            return false;
        }
        return distractorEfficiency.values().stream()
                .anyMatch(rate -> rate != null && rate == 0.0);
    }

    /**
     * Get the count of non-functioning distractors.
     *
     * @return number of distractors with 0% selection
     */
    public int getNonFunctioningDistractorCount() {
        if (distractorEfficiency == null || distractorEfficiency.isEmpty()) {
            return 0;
        }
        return (int) distractorEfficiency.values().stream()
                .filter(rate -> rate != null && rate == 0.0)
                .count();
    }

    /**
     * Get the difficulty interpretation text.
     *
     * @return human-readable difficulty interpretation
     */
    public String getDifficultyInterpretation() {
        if (difficultyIndex == null) {
            return "Insufficient data for difficulty analysis";
        }

        double p = difficultyIndex.doubleValue();
        if (p < 0.2) {
            return "Too difficult - Only " + String.format("%.0f%%", p * 100) + " of respondents answer correctly";
        } else if (p > 0.9) {
            return "Too easy - " + String.format("%.0f%%", p * 100) + " of respondents answer correctly";
        } else if (p >= 0.4 && p <= 0.7) {
            return "Optimal difficulty - " + String.format("%.0f%%", p * 100) + " correct response rate";
        } else {
            return "Acceptable difficulty - " + String.format("%.0f%%", p * 100) + " correct response rate";
        }
    }

    /**
     * Get the discrimination interpretation text.
     *
     * @return human-readable discrimination interpretation
     */
    public String getDiscriminationInterpretation() {
        if (discriminationIndex == null) {
            return "Insufficient data for discrimination analysis";
        }

        double rpb = discriminationIndex.doubleValue();
        if (rpb < 0) {
            return "Toxic - Item negatively correlates with total score (high performers fail)";
        } else if (rpb < 0.1) {
            return "Critical - Very poor discrimination, item does not differentiate skill levels";
        } else if (rpb < 0.25) {
            return "Marginal - Limited discriminating power, consider revision";
        } else if (rpb < 0.3) {
            return "Acceptable - Adequate discrimination for use in assessments";
        } else if (rpb < 0.4) {
            return "Good - Strong discrimination between high and low performers";
        } else {
            return "Excellent - Very high discrimination power";
        }
    }

    /**
     * Record representing a status change history entry.
     */
    public record StatusChangeEntry(
            ItemValidityStatus fromStatus,
            ItemValidityStatus toStatus,
            LocalDateTime timestamp,
            String reason
    ) {}

    /**
     * Builder for constructing ItemStatisticsDetailDto.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for ItemStatisticsDetailDto.
     */
    public static class Builder {
        private UUID id;
        private UUID questionId;
        private String questionText;
        private QuestionType questionType;
        private UUID competencyId;
        private String competencyName;
        private UUID indicatorId;
        private String indicatorTitle;
        private BigDecimal difficultyIndex;
        private BigDecimal discriminationIndex;
        private BigDecimal previousDiscriminationIndex;
        private Integer responseCount;
        private ItemValidityStatus validityStatus;
        private DifficultyFlag difficultyFlag;
        private DiscriminationFlag discriminationFlag;
        private Map<String, Double> distractorEfficiency;
        private List<StatusChangeEntry> statusChangeHistory;
        private List<String> recommendations;
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

        public Builder questionType(QuestionType questionType) {
            this.questionType = questionType;
            return this;
        }

        public Builder competencyId(UUID competencyId) {
            this.competencyId = competencyId;
            return this;
        }

        public Builder competencyName(String competencyName) {
            this.competencyName = competencyName;
            return this;
        }

        public Builder indicatorId(UUID indicatorId) {
            this.indicatorId = indicatorId;
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

        public Builder previousDiscriminationIndex(BigDecimal previousDiscriminationIndex) {
            this.previousDiscriminationIndex = previousDiscriminationIndex;
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

        public Builder distractorEfficiency(Map<String, Double> distractorEfficiency) {
            this.distractorEfficiency = distractorEfficiency;
            return this;
        }

        public Builder statusChangeHistory(List<StatusChangeEntry> statusChangeHistory) {
            this.statusChangeHistory = statusChangeHistory;
            return this;
        }

        public Builder recommendations(List<String> recommendations) {
            this.recommendations = recommendations;
            return this;
        }

        public Builder lastCalculatedAt(LocalDateTime lastCalculatedAt) {
            this.lastCalculatedAt = lastCalculatedAt;
            return this;
        }

        public ItemStatisticsDetailDto build() {
            return new ItemStatisticsDetailDto(
                    id, questionId, questionText, questionType,
                    competencyId, competencyName, indicatorId, indicatorTitle,
                    difficultyIndex, discriminationIndex, previousDiscriminationIndex, responseCount,
                    validityStatus, difficultyFlag, discriminationFlag,
                    distractorEfficiency, statusChangeHistory, recommendations,
                    lastCalculatedAt
            );
        }
    }
}
