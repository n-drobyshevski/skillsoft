package app.skillsoft.assessmentbackend.domain.dto.psychometrics;

import app.skillsoft.assessmentbackend.domain.entities.ReliabilityStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Detailed DTO for competency-level reliability statistics.
 * <p>
 * Extends CompetencyReliabilityDto with additional details for single-competency views,
 * including alpha-if-deleted analysis and items lowering reliability.
 *
 * @param id                    UUID of the CompetencyReliability record
 * @param competencyId          UUID of the associated competency
 * @param competencyName        name of the competency
 * @param competencyCategory    category of the competency
 * @param cronbachAlpha         Cronbach's Alpha coefficient (internal consistency)
 * @param sampleSize            number of respondents used in calculation
 * @param itemCount             number of assessment items included
 * @param reliabilityStatus     current reliability status
 * @param alphaIfDeleted        map of question UUID to alpha-if-deleted value
 * @param itemsLoweringAlpha    list of items that, if removed, would improve alpha
 * @param lastCalculatedAt      timestamp of last reliability calculation
 */
public record CompetencyReliabilityDetailDto(
        // Core Identifiers
        UUID id,
        UUID competencyId,
        String competencyName,
        String competencyCategory,

        // Reliability Metrics
        BigDecimal cronbachAlpha,
        Integer sampleSize,
        Integer itemCount,
        ReliabilityStatus reliabilityStatus,

        // Alpha-if-Deleted Analysis
        Map<UUID, AlphaIfDeletedEntry> alphaIfDeleted,
        List<ItemLoweringAlpha> itemsLoweringAlpha,

        // Metadata
        LocalDateTime lastCalculatedAt
) {

    /**
     * Calculate potential alpha improvement if worst item is removed.
     *
     * @return improvement amount, or null if no improvement possible
     */
    public BigDecimal getPotentialImprovement() {
        if (itemsLoweringAlpha == null || itemsLoweringAlpha.isEmpty() || cronbachAlpha == null) {
            return null;
        }

        // Find maximum improvement
        BigDecimal maxImprovement = BigDecimal.ZERO;
        for (ItemLoweringAlpha item : itemsLoweringAlpha) {
            BigDecimal improvement = item.alphaIfDeleted().subtract(cronbachAlpha);
            if (improvement.compareTo(maxImprovement) > 0) {
                maxImprovement = improvement;
            }
        }

        return maxImprovement.compareTo(BigDecimal.ZERO) > 0 ? maxImprovement : null;
    }

    /**
     * Check if any items are lowering overall reliability.
     *
     * @return true if removing any item would increase alpha
     */
    public boolean hasProblematicItems() {
        return itemsLoweringAlpha != null && !itemsLoweringAlpha.isEmpty();
    }

    /**
     * Get the number of items lowering reliability.
     *
     * @return count of problematic items
     */
    public int getProblematicItemCount() {
        return itemsLoweringAlpha != null ? itemsLoweringAlpha.size() : 0;
    }

    /**
     * Get the alpha interpretation category.
     *
     * @return category string (Excellent, Good, Acceptable, Questionable, Poor)
     */
    public String getAlphaCategory() {
        if (cronbachAlpha == null) {
            return "Insufficient Data";
        }

        double alpha = cronbachAlpha.doubleValue();
        if (alpha >= 0.9) {
            return "Excellent";
        } else if (alpha >= 0.8) {
            return "Good";
        } else if (alpha >= 0.7) {
            return "Acceptable";
        } else if (alpha >= 0.6) {
            return "Questionable";
        } else {
            return "Poor";
        }
    }

    /**
     * Get alpha interpretation text for display.
     *
     * @return human-readable alpha interpretation
     */
    public String getAlphaInterpretation() {
        if (cronbachAlpha == null) {
            return "Insufficient data for reliability analysis (need minimum 50 responses)";
        }

        double alpha = cronbachAlpha.doubleValue();
        String category = getAlphaCategory();

        if (alpha >= 0.9) {
            return category + " internal consistency (" + String.format("%.3f", alpha) + "). " +
                    "Items measure the same underlying construct with very high consistency.";
        } else if (alpha >= 0.8) {
            return category + " internal consistency (" + String.format("%.3f", alpha) + "). " +
                    "Items are well-correlated and reliably measure the competency.";
        } else if (alpha >= 0.7) {
            return category + " internal consistency (" + String.format("%.3f", alpha) + "). " +
                    "Meets minimum threshold for reliable measurement.";
        } else if (alpha >= 0.6) {
            return category + " internal consistency (" + String.format("%.3f", alpha) + "). " +
                    "Below recommended threshold. Consider revising low-performing items.";
        } else {
            return category + " internal consistency (" + String.format("%.3f", alpha) + "). " +
                    "Items may not reliably measure the same construct. Significant revision needed.";
        }
    }

    /**
     * Entry representing alpha-if-deleted information for a single item.
     */
    public record AlphaIfDeletedEntry(
            UUID questionId,
            String questionText,
            BigDecimal alphaIfDeleted,
            BigDecimal currentContribution
    ) {
        /**
         * Check if removing this item would improve alpha.
         *
         * @param currentAlpha current Cronbach's Alpha
         * @return true if alphaIfDeleted > currentAlpha
         */
        public boolean wouldImproveAlpha(BigDecimal currentAlpha) {
            if (alphaIfDeleted == null || currentAlpha == null) {
                return false;
            }
            return alphaIfDeleted.compareTo(currentAlpha) > 0;
        }
    }

    /**
     * Summary of an item that is lowering overall reliability.
     */
    public record ItemLoweringAlpha(
            UUID questionId,
            String questionText,
            BigDecimal alphaIfDeleted,
            BigDecimal improvementAmount,
            String recommendation
    ) {}

    /**
     * Builder for constructing CompetencyReliabilityDetailDto.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for CompetencyReliabilityDetailDto.
     */
    public static class Builder {
        private UUID id;
        private UUID competencyId;
        private String competencyName;
        private String competencyCategory;
        private BigDecimal cronbachAlpha;
        private Integer sampleSize;
        private Integer itemCount;
        private ReliabilityStatus reliabilityStatus;
        private Map<UUID, AlphaIfDeletedEntry> alphaIfDeleted;
        private List<ItemLoweringAlpha> itemsLoweringAlpha;
        private LocalDateTime lastCalculatedAt;

        public Builder id(UUID id) {
            this.id = id;
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

        public Builder competencyCategory(String competencyCategory) {
            this.competencyCategory = competencyCategory;
            return this;
        }

        public Builder cronbachAlpha(BigDecimal cronbachAlpha) {
            this.cronbachAlpha = cronbachAlpha;
            return this;
        }

        public Builder sampleSize(Integer sampleSize) {
            this.sampleSize = sampleSize;
            return this;
        }

        public Builder itemCount(Integer itemCount) {
            this.itemCount = itemCount;
            return this;
        }

        public Builder reliabilityStatus(ReliabilityStatus reliabilityStatus) {
            this.reliabilityStatus = reliabilityStatus;
            return this;
        }

        public Builder alphaIfDeleted(Map<UUID, AlphaIfDeletedEntry> alphaIfDeleted) {
            this.alphaIfDeleted = alphaIfDeleted;
            return this;
        }

        public Builder itemsLoweringAlpha(List<ItemLoweringAlpha> itemsLoweringAlpha) {
            this.itemsLoweringAlpha = itemsLoweringAlpha;
            return this;
        }

        public Builder lastCalculatedAt(LocalDateTime lastCalculatedAt) {
            this.lastCalculatedAt = lastCalculatedAt;
            return this;
        }

        public CompetencyReliabilityDetailDto build() {
            return new CompetencyReliabilityDetailDto(
                    id, competencyId, competencyName, competencyCategory,
                    cronbachAlpha, sampleSize, itemCount, reliabilityStatus,
                    alphaIfDeleted, itemsLoweringAlpha, lastCalculatedAt
            );
        }
    }
}
