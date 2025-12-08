package app.skillsoft.assessmentbackend.domain.dto.simulation;

import java.util.Map;
import java.util.UUID;

/**
 * Heatmap DTO showing question inventory health per competency.
 * Used by InventoryHeatmapService to visualize question availability.
 */
public record InventoryHeatmapDto(
    /**
     * Map of CompetencyId to HealthStatus.
     * Indicates overall health of question inventory for each competency.
     */
    Map<UUID, HealthStatus> competencyHealth,
    
    /**
     * Detailed breakdown by competency and difficulty.
     * Key format: "competencyId:difficulty"
     * Value: Question count
     */
    Map<String, Long> detailedCounts,
    
    /**
     * Summary statistics.
     */
    HeatmapSummary summary
) {
    /**
     * Summary statistics for the heatmap.
     */
    public record HeatmapSummary(
        int totalCompetencies,
        int criticalCount,
        int moderateCount,
        int healthyCount,
        long totalQuestions
    ) {
        /**
         * Calculate overall health percentage.
         * @return Percentage of competencies in HEALTHY status
         */
        public double healthPercentage() {
            if (totalCompetencies == 0) return 0.0;
            return (double) healthyCount / totalCompetencies * 100;
        }
    }

    /**
     * Builder for constructing InventoryHeatmapDto.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Map<UUID, HealthStatus> competencyHealth = Map.of();
        private Map<String, Long> detailedCounts = Map.of();
        private HeatmapSummary summary;

        public Builder competencyHealth(Map<UUID, HealthStatus> competencyHealth) {
            this.competencyHealth = competencyHealth;
            return this;
        }

        public Builder detailedCounts(Map<String, Long> detailedCounts) {
            this.detailedCounts = detailedCounts;
            return this;
        }

        public Builder summary(HeatmapSummary summary) {
            this.summary = summary;
            return this;
        }

        public InventoryHeatmapDto build() {
            return new InventoryHeatmapDto(competencyHealth, detailedCounts, summary);
        }
    }
}
