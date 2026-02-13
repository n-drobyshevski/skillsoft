package app.skillsoft.assessmentbackend.domain.dto.psychometrics;

import app.skillsoft.assessmentbackend.domain.entities.ReliabilityStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for competency-level reliability statistics.
 * <p>
 * Provides a summary view of Cronbach's Alpha and related metrics
 * for competencies, suitable for list views and dashboards.
 *
 * @param id                UUID of the CompetencyReliability record
 * @param competencyId      UUID of the associated competency
 * @param competencyName    name of the competency
 * @param cronbachAlpha     Cronbach's Alpha coefficient (internal consistency)
 * @param sampleSize        number of respondents used in calculation
 * @param itemCount         number of assessment items included
 * @param reliabilityStatus current reliability status (RELIABLE, ACCEPTABLE, UNRELIABLE, INSUFFICIENT_DATA)
 * @param lastCalculatedAt  timestamp of last reliability calculation
 */
public record CompetencyReliabilityDto(
        UUID id,
        UUID competencyId,
        String competencyName,
        BigDecimal cronbachAlpha,
        Integer sampleSize,
        Integer itemCount,
        ReliabilityStatus reliabilityStatus,
        LocalDateTime lastCalculatedAt
) {

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
     * Get alpha as a percentage for display (0-100).
     *
     * @return alpha multiplied by 100, or null if no alpha
     */
    public Double getAlphaPercentage() {
        if (cronbachAlpha == null) {
            return null;
        }
        return cronbachAlpha.doubleValue() * 100;
    }

    /**
     * Check if the competency has sufficient data for analysis.
     *
     * @return true if sampleSize >= 50 and itemCount >= 2
     */
    public boolean hasSufficientData() {
        return sampleSize != null && sampleSize >= 50
                && itemCount != null && itemCount >= 2;
    }

    /**
     * Check if the competency meets minimum reliability threshold.
     *
     * @return true if alpha >= 0.7
     */
    public boolean meetsMinimumReliability() {
        return cronbachAlpha != null && cronbachAlpha.compareTo(new BigDecimal("0.7")) >= 0;
    }

    /**
     * Get a human-readable status description.
     *
     * @return description of the reliability status
     */
    public String getStatusDescription() {
        if (reliabilityStatus == null) {
            return "Unknown status";
        }
        return reliabilityStatus.getDisplayName() + " - " + reliabilityStatus.getDescription();
    }

    /**
     * Builder for constructing CompetencyReliabilityDto.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for CompetencyReliabilityDto.
     */
    public static class Builder {
        private UUID id;
        private UUID competencyId;
        private String competencyName;
        private BigDecimal cronbachAlpha;
        private Integer sampleSize;
        private Integer itemCount;
        private ReliabilityStatus reliabilityStatus;
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

        public Builder lastCalculatedAt(LocalDateTime lastCalculatedAt) {
            this.lastCalculatedAt = lastCalculatedAt;
            return this;
        }

        public CompetencyReliabilityDto build() {
            return new CompetencyReliabilityDto(
                    id, competencyId, competencyName, cronbachAlpha,
                    sampleSize, itemCount, reliabilityStatus, lastCalculatedAt
            );
        }
    }
}
