package app.skillsoft.assessmentbackend.domain.entities;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * BigFiveReliability entity storing Cronbach's Alpha at the Big Five trait level.
 *
 * Per the Test Validation Mechanic architecture:
 * - Tracks reliability across personality dimensions (not just competencies)
 * - Aggregates items from all competencies mapped to each Big Five trait
 * - Enables trait-level quality assurance for personality assessments
 *
 * Big Five Traits:
 * - OPENNESS: Creativity, curiosity, intellectual interests
 * - CONSCIENTIOUSNESS: Organization, dependability, self-discipline
 * - EXTRAVERSION: Energy, sociability, assertiveness
 * - AGREEABLENESS: Cooperation, trust, empathy
 * - EMOTIONAL_STABILITY: Calmness, resilience (inverse of Neuroticism)
 *
 * Calculations performed by PsychometricAnalysisService using competency standard codes mappings.
 */
@Entity
@Table(name = "big_five_reliability", indexes = {
    @Index(name = "idx_big_five_reliability_trait", columnList = "trait"),
    @Index(name = "idx_big_five_reliability_status", columnList = "reliability_status")
})
public class BigFiveReliability {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The Big Five personality trait this reliability record covers.
     * Unique constraint: one record per trait.
     */
    @Column(name = "trait", nullable = false, unique = true)
    @Enumerated(EnumType.STRING)
    private BigFiveTrait trait;

    /**
     * Cronbach's Alpha coefficient for this trait.
     * Aggregates across all competencies mapped to this Big Five dimension.
     *
     * Higher thresholds may be expected for personality traits due to
     * the larger number of contributing items.
     *
     * Null if insufficient data for calculation.
     */
    @Column(name = "cronbach_alpha", precision = 5, scale = 4)
    private BigDecimal cronbachAlpha;

    /**
     * Number of competencies contributing to this trait's assessment.
     * Based on competency.standardCodes.bigFiveRef mappings.
     */
    @Column(name = "contributing_competencies")
    private Integer contributingCompetencies;

    /**
     * Total number of assessment items (questions) across all contributing competencies.
     */
    @Column(name = "total_items")
    private Integer totalItems;

    /**
     * Number of respondents used in the calculation.
     * Requires responses across all contributing competencies.
     */
    @Column(name = "sample_size")
    private Integer sampleSize;

    /**
     * Reliability status based on Cronbach's Alpha thresholds.
     */
    @Column(name = "reliability_status")
    @Enumerated(EnumType.STRING)
    private ReliabilityStatus reliabilityStatus;

    /**
     * Timestamp of the last reliability calculation.
     */
    @Column(name = "last_calculated_at")
    private LocalDateTime lastCalculatedAt;

    // Constructors
    public BigFiveReliability() {
    }

    public BigFiveReliability(BigFiveTrait trait) {
        this.trait = trait;
        this.reliabilityStatus = ReliabilityStatus.INSUFFICIENT_DATA;
    }

    // Business Methods

    /**
     * Check if this trait has sufficient data for reliable analysis.
     */
    @Transient
    public boolean hasSufficientData() {
        return sampleSize != null && sampleSize >= 50
            && totalItems != null && totalItems >= 2
            && contributingCompetencies != null && contributingCompetencies >= 1;
    }

    /**
     * Get the average number of items per contributing competency.
     */
    @Transient
    public double getAverageItemsPerCompetency() {
        if (contributingCompetencies == null || contributingCompetencies == 0 || totalItems == null) {
            return 0.0;
        }
        return (double) totalItems / contributingCompetencies;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public BigFiveTrait getTrait() {
        return trait;
    }

    public void setTrait(BigFiveTrait trait) {
        this.trait = trait;
    }

    public BigDecimal getCronbachAlpha() {
        return cronbachAlpha;
    }

    public void setCronbachAlpha(BigDecimal cronbachAlpha) {
        this.cronbachAlpha = cronbachAlpha;
    }

    public Integer getContributingCompetencies() {
        return contributingCompetencies;
    }

    public void setContributingCompetencies(Integer contributingCompetencies) {
        this.contributingCompetencies = contributingCompetencies;
    }

    public Integer getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(Integer totalItems) {
        this.totalItems = totalItems;
    }

    public Integer getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(Integer sampleSize) {
        this.sampleSize = sampleSize;
    }

    public ReliabilityStatus getReliabilityStatus() {
        return reliabilityStatus;
    }

    public void setReliabilityStatus(ReliabilityStatus reliabilityStatus) {
        this.reliabilityStatus = reliabilityStatus;
    }

    public LocalDateTime getLastCalculatedAt() {
        return lastCalculatedAt;
    }

    public void setLastCalculatedAt(LocalDateTime lastCalculatedAt) {
        this.lastCalculatedAt = lastCalculatedAt;
    }
}
