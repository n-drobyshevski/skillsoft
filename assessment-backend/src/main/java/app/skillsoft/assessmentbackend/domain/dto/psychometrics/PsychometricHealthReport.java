package app.skillsoft.assessmentbackend.domain.dto.psychometrics;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Comprehensive psychometric health report for the assessment system.
 * <p>
 * Provides aggregated metrics and summaries for monitoring test quality
 * and identifying items/competencies requiring attention.
 *
 * @param totalItems              total number of assessment questions with statistics
 * @param activeItems             count of validated items (ACTIVE status)
 * @param probationItems          count of new items gathering data (PROBATION status)
 * @param flaggedItems            count of items requiring HR review (FLAGGED_FOR_REVIEW status)
 * @param retiredItems            count of deactivated items (RETIRED status)
 * @param totalCompetencies       total number of competencies with reliability data
 * @param reliableCompetencies    count of competencies with alpha >= 0.7
 * @param acceptableCompetencies  count of competencies with 0.6 <= alpha < 0.7
 * @param unreliableCompetencies  count of competencies with alpha < 0.6
 * @param insufficientDataCompetencies count of competencies without enough data
 * @param averageAlpha            average Cronbach's Alpha across all competencies
 * @param averageDiscrimination   average discrimination index across all items
 * @param topFlaggedItems         list of highest priority flagged items (up to 10)
 * @param bigFiveReliabilitySummary summary of Big Five trait reliability
 * @param lastAuditRun            timestamp of the last full audit run
 * @param itemsAnalyzedSinceLastAudit count of items analyzed since last full audit
 */
public record PsychometricHealthReport(
        // Item Statistics Summary
        int totalItems,
        int activeItems,
        int probationItems,
        int flaggedItems,
        int retiredItems,

        // Competency Reliability Summary
        int totalCompetencies,
        int reliableCompetencies,
        int acceptableCompetencies,
        int unreliableCompetencies,
        int insufficientDataCompetencies,

        // Aggregate Metrics
        BigDecimal averageAlpha,
        BigDecimal averageDiscrimination,

        // Priority Items
        List<FlaggedItemSummary> topFlaggedItems,

        // Big Five Summary
        BigFiveReliabilitySummary bigFiveReliabilitySummary,

        // Audit Metadata
        LocalDateTime lastAuditRun,
        int itemsAnalyzedSinceLastAudit
) {

    /**
     * Calculate the percentage of active items.
     *
     * @return percentage of items that are ACTIVE (0-100)
     */
    public double getActiveItemPercentage() {
        if (totalItems == 0) return 0.0;
        return (activeItems * 100.0) / totalItems;
    }

    /**
     * Calculate the percentage of reliable competencies.
     *
     * @return percentage of competencies with alpha >= 0.7 (0-100)
     */
    public double getReliableCompetencyPercentage() {
        if (totalCompetencies == 0) return 0.0;
        return (reliableCompetencies * 100.0) / totalCompetencies;
    }

    /**
     * Determine the overall health status of the assessment system.
     *
     * @return health status: HEALTHY, WARNING, or CRITICAL
     */
    public HealthStatus getOverallHealthStatus() {
        // Critical if too many flagged/retired items or unreliable competencies
        if (flaggedItems > totalItems * 0.2 || retiredItems > totalItems * 0.3) {
            return HealthStatus.CRITICAL;
        }
        if (unreliableCompetencies > totalCompetencies * 0.3) {
            return HealthStatus.CRITICAL;
        }

        // Warning if moderate issues exist
        if (flaggedItems > totalItems * 0.1 || probationItems > totalItems * 0.5) {
            return HealthStatus.WARNING;
        }
        if (unreliableCompetencies > totalCompetencies * 0.1) {
            return HealthStatus.WARNING;
        }
        if (averageAlpha != null && averageAlpha.compareTo(new BigDecimal("0.7")) < 0) {
            return HealthStatus.WARNING;
        }

        return HealthStatus.HEALTHY;
    }

    /**
     * Check if immediate action is required.
     *
     * @return true if there are toxic items or critical reliability issues
     */
    public boolean requiresImmediateAction() {
        return topFlaggedItems != null && topFlaggedItems.stream()
                .anyMatch(item -> item.getSeverityLevel() >= 3);
    }

    /**
     * Get the count of items needing attention (flagged + probation).
     *
     * @return total items requiring some form of attention
     */
    public int getItemsNeedingAttention() {
        return flaggedItems + probationItems;
    }

    /**
     * Overall health status of the psychometric system.
     */
    public enum HealthStatus {
        HEALTHY("Healthy", "All metrics within acceptable ranges"),
        WARNING("Warning", "Some metrics require monitoring"),
        CRITICAL("Critical", "Immediate attention required");

        private final String displayName;
        private final String description;

        HealthStatus(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Summary of Big Five trait reliability.
     */
    public record BigFiveReliabilitySummary(
            int totalTraits,
            int reliableTraits,
            int unreliableTraits,
            int insufficientDataTraits,
            BigDecimal averageTraitAlpha,
            String lowestAlphaTrait,
            BigDecimal lowestAlphaValue
    ) {

        /**
         * Check if all Big Five traits have reliable measurement.
         *
         * @return true if all traits have alpha >= 0.7
         */
        public boolean isFullyReliable() {
            return reliableTraits == totalTraits && totalTraits == 5;
        }

        /**
         * Get the percentage of reliable Big Five traits.
         *
         * @return percentage of reliable traits (0-100)
         */
        public double getReliablePercentage() {
            if (totalTraits == 0) return 0.0;
            return (reliableTraits * 100.0) / totalTraits;
        }
    }

    /**
     * Builder for constructing PsychometricHealthReport.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for PsychometricHealthReport.
     */
    public static class Builder {
        private int totalItems;
        private int activeItems;
        private int probationItems;
        private int flaggedItems;
        private int retiredItems;
        private int totalCompetencies;
        private int reliableCompetencies;
        private int acceptableCompetencies;
        private int unreliableCompetencies;
        private int insufficientDataCompetencies;
        private BigDecimal averageAlpha;
        private BigDecimal averageDiscrimination;
        private List<FlaggedItemSummary> topFlaggedItems;
        private BigFiveReliabilitySummary bigFiveReliabilitySummary;
        private LocalDateTime lastAuditRun;
        private int itemsAnalyzedSinceLastAudit;

        public Builder totalItems(int totalItems) {
            this.totalItems = totalItems;
            return this;
        }

        public Builder activeItems(int activeItems) {
            this.activeItems = activeItems;
            return this;
        }

        public Builder probationItems(int probationItems) {
            this.probationItems = probationItems;
            return this;
        }

        public Builder flaggedItems(int flaggedItems) {
            this.flaggedItems = flaggedItems;
            return this;
        }

        public Builder retiredItems(int retiredItems) {
            this.retiredItems = retiredItems;
            return this;
        }

        public Builder totalCompetencies(int totalCompetencies) {
            this.totalCompetencies = totalCompetencies;
            return this;
        }

        public Builder reliableCompetencies(int reliableCompetencies) {
            this.reliableCompetencies = reliableCompetencies;
            return this;
        }

        public Builder acceptableCompetencies(int acceptableCompetencies) {
            this.acceptableCompetencies = acceptableCompetencies;
            return this;
        }

        public Builder unreliableCompetencies(int unreliableCompetencies) {
            this.unreliableCompetencies = unreliableCompetencies;
            return this;
        }

        public Builder insufficientDataCompetencies(int insufficientDataCompetencies) {
            this.insufficientDataCompetencies = insufficientDataCompetencies;
            return this;
        }

        public Builder averageAlpha(BigDecimal averageAlpha) {
            this.averageAlpha = averageAlpha;
            return this;
        }

        public Builder averageDiscrimination(BigDecimal averageDiscrimination) {
            this.averageDiscrimination = averageDiscrimination;
            return this;
        }

        public Builder topFlaggedItems(List<FlaggedItemSummary> topFlaggedItems) {
            this.topFlaggedItems = topFlaggedItems;
            return this;
        }

        public Builder bigFiveReliabilitySummary(BigFiveReliabilitySummary summary) {
            this.bigFiveReliabilitySummary = summary;
            return this;
        }

        public Builder lastAuditRun(LocalDateTime lastAuditRun) {
            this.lastAuditRun = lastAuditRun;
            return this;
        }

        public Builder itemsAnalyzedSinceLastAudit(int count) {
            this.itemsAnalyzedSinceLastAudit = count;
            return this;
        }

        public PsychometricHealthReport build() {
            return new PsychometricHealthReport(
                    totalItems, activeItems, probationItems, flaggedItems, retiredItems,
                    totalCompetencies, reliableCompetencies, acceptableCompetencies,
                    unreliableCompetencies, insufficientDataCompetencies,
                    averageAlpha, averageDiscrimination,
                    topFlaggedItems, bigFiveReliabilitySummary,
                    lastAuditRun, itemsAnalyzedSinceLastAudit
            );
        }
    }
}
