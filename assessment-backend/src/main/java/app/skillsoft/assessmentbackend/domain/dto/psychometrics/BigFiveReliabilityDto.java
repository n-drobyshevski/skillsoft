package app.skillsoft.assessmentbackend.domain.dto.psychometrics;

import app.skillsoft.assessmentbackend.domain.entities.BigFiveTrait;
import app.skillsoft.assessmentbackend.domain.entities.ReliabilityStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for Big Five trait-level reliability statistics.
 * <p>
 * Provides reliability metrics aggregated across all competencies
 * mapped to each Big Five personality trait.
 *
 * @param id                      UUID of the BigFiveReliability record
 * @param trait                   the Big Five personality trait
 * @param traitDisplayName        localized display name for the trait
 * @param cronbachAlpha           Cronbach's Alpha coefficient for this trait
 * @param contributingCompetencies number of competencies mapped to this trait
 * @param totalItems              total assessment items across contributing competencies
 * @param sampleSize              number of respondents used in calculation
 * @param reliabilityStatus       current reliability status
 * @param lastCalculatedAt        timestamp of last reliability calculation
 */
public record BigFiveReliabilityDto(
        UUID id,
        BigFiveTrait trait,
        String traitDisplayName,
        BigDecimal cronbachAlpha,
        Integer contributingCompetencies,
        Integer totalItems,
        Integer sampleSize,
        ReliabilityStatus reliabilityStatus,
        LocalDateTime lastCalculatedAt
) {

    /**
     * Get the trait description for display.
     *
     * @return description of the Big Five trait
     */
    public String getTraitDescription() {
        if (trait == null) {
            return "Unknown trait";
        }
        return trait.getDescription();
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
     * Check if the trait has sufficient data for analysis.
     *
     * @return true if has minimum requirements for analysis
     */
    public boolean hasSufficientData() {
        return sampleSize != null && sampleSize >= 50
                && totalItems != null && totalItems >= 2
                && contributingCompetencies != null && contributingCompetencies >= 1;
    }

    /**
     * Check if the trait meets minimum reliability threshold.
     *
     * @return true if alpha >= 0.7
     */
    public boolean meetsMinimumReliability() {
        return cronbachAlpha != null && cronbachAlpha.compareTo(new BigDecimal("0.7")) >= 0;
    }

    /**
     * Get average items per contributing competency.
     *
     * @return average item count, or 0 if no competencies
     */
    public double getAverageItemsPerCompetency() {
        if (contributingCompetencies == null || contributingCompetencies == 0 || totalItems == null) {
            return 0.0;
        }
        return (double) totalItems / contributingCompetencies;
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
     * Get alpha interpretation text for display.
     *
     * @return human-readable alpha interpretation
     */
    public String getAlphaInterpretation() {
        if (cronbachAlpha == null) {
            return "Insufficient data for reliability analysis of " + traitDisplayName;
        }

        String category = getAlphaCategory();
        String alphaStr = String.format("%.3f", cronbachAlpha.doubleValue());

        return switch (category) {
            case "Excellent" -> category + " reliability (" + alphaStr + "). " +
                    "The " + traitDisplayName + " trait is measured with very high consistency across " +
                    contributingCompetencies + " competencies.";
            case "Good" -> category + " reliability (" + alphaStr + "). " +
                    "The " + traitDisplayName + " trait is measured reliably.";
            case "Acceptable" -> category + " reliability (" + alphaStr + "). " +
                    "The " + traitDisplayName + " trait meets minimum threshold for reliable measurement.";
            case "Questionable" -> category + " reliability (" + alphaStr + "). " +
                    "The " + traitDisplayName + " trait may need additional competency mappings.";
            default -> "Poor reliability (" + alphaStr + "). " +
                    "The " + traitDisplayName + " trait measurement requires significant improvement.";
        };
    }

    /**
     * Builder for constructing BigFiveReliabilityDto.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for BigFiveReliabilityDto.
     */
    public static class Builder {
        private UUID id;
        private BigFiveTrait trait;
        private String traitDisplayName;
        private BigDecimal cronbachAlpha;
        private Integer contributingCompetencies;
        private Integer totalItems;
        private Integer sampleSize;
        private ReliabilityStatus reliabilityStatus;
        private LocalDateTime lastCalculatedAt;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder trait(BigFiveTrait trait) {
            this.trait = trait;
            if (trait != null) {
                this.traitDisplayName = trait.getDisplayName();
            }
            return this;
        }

        public Builder traitDisplayName(String traitDisplayName) {
            this.traitDisplayName = traitDisplayName;
            return this;
        }

        public Builder cronbachAlpha(BigDecimal cronbachAlpha) {
            this.cronbachAlpha = cronbachAlpha;
            return this;
        }

        public Builder contributingCompetencies(Integer contributingCompetencies) {
            this.contributingCompetencies = contributingCompetencies;
            return this;
        }

        public Builder totalItems(Integer totalItems) {
            this.totalItems = totalItems;
            return this;
        }

        public Builder sampleSize(Integer sampleSize) {
            this.sampleSize = sampleSize;
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

        public BigFiveReliabilityDto build() {
            return new BigFiveReliabilityDto(
                    id, trait, traitDisplayName, cronbachAlpha,
                    contributingCompetencies, totalItems, sampleSize,
                    reliabilityStatus, lastCalculatedAt
            );
        }
    }
}
