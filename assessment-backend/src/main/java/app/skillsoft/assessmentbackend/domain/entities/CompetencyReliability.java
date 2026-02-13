package app.skillsoft.assessmentbackend.domain.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * CompetencyReliability entity storing Cronbach's Alpha and related metrics for competencies.
 *
 * Per the Test Validation Mechanic architecture:
 * - Tracks Cronbach's Alpha (internal consistency) at the competency level
 * - Provides "Alpha-if-Item-Deleted" analysis to identify problem items
 * - Determines reliability status: RELIABLE, ACCEPTABLE, UNRELIABLE, INSUFFICIENT_DATA
 *
 * Thresholds:
 * - alpha >= 0.7: RELIABLE
 * - 0.6 <= alpha < 0.7: ACCEPTABLE
 * - alpha < 0.6: UNRELIABLE
 *
 * Calculations performed by PsychometricAnalysisService.
 */
@Entity
@Table(name = "competency_reliability", indexes = {
    @Index(name = "idx_comp_reliability_competency", columnList = "competency_id"),
    @Index(name = "idx_comp_reliability_status", columnList = "reliability_status")
})
public class CompetencyReliability {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The competency these reliability metrics belong to.
     * One-to-one relationship: each competency has exactly one reliability record.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competency_id", nullable = false, unique = true)
    private Competency competency;

    /**
     * Cronbach's Alpha coefficient measuring internal consistency.
     * Range: -infinity to 1.0 (typically 0.0 to 1.0 for valid assessments).
     *
     * Formula: alpha = (k / (k-1)) * (1 - sum(var_i) / var_total)
     * where k = number of items, var_i = item variance, var_total = total score variance
     *
     * Interpretation:
     * - alpha >= 0.9: Excellent
     * - 0.8 <= alpha < 0.9: Good
     * - 0.7 <= alpha < 0.8: Acceptable
     * - 0.6 <= alpha < 0.7: Questionable
     * - alpha < 0.6: Poor/Unacceptable
     *
     * Null if insufficient data for calculation.
     */
    @Column(name = "cronbach_alpha", precision = 5, scale = 4)
    private BigDecimal cronbachAlpha;

    /**
     * Number of respondents (test sessions) used in the calculation.
     * Minimum 50 required for reliable statistics.
     */
    @Column(name = "sample_size")
    private Integer sampleSize;

    /**
     * Number of assessment items (questions) included in the calculation.
     * Minimum 2 items required for alpha calculation.
     */
    @Column(name = "item_count")
    private Integer itemCount;

    /**
     * Reliability status based on Cronbach's Alpha thresholds.
     */
    @Column(name = "reliability_status")
    @Enumerated(EnumType.STRING)
    private ReliabilityStatus reliabilityStatus;

    /**
     * Alpha-if-Item-Deleted analysis.
     * Maps question UUID to the Cronbach's Alpha that would result if that item were removed.
     * Used to identify items that are lowering overall reliability.
     *
     * If alphaIfDeleted[item] > cronbachAlpha, removing that item would improve reliability.
     */
    @Column(name = "alpha_if_deleted", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<UUID, BigDecimal> alphaIfDeleted;

    /**
     * Timestamp of the last reliability calculation.
     */
    @Column(name = "last_calculated_at")
    private LocalDateTime lastCalculatedAt;

    // Constructors
    public CompetencyReliability() {
    }

    public CompetencyReliability(Competency competency) {
        this.competency = competency;
        this.reliabilityStatus = ReliabilityStatus.INSUFFICIENT_DATA;
    }

    // Business Methods

    /**
     * Check if the competency has sufficient data for reliable analysis.
     */
    @Transient
    public boolean hasSufficientData() {
        return sampleSize != null && sampleSize >= 50 && itemCount != null && itemCount >= 2;
    }

    /**
     * Check if any items are lowering the overall reliability.
     * Returns true if removing any item would increase alpha.
     */
    @Transient
    public boolean hasProblematicItems() {
        if (cronbachAlpha == null || alphaIfDeleted == null || alphaIfDeleted.isEmpty()) {
            return false;
        }
        return alphaIfDeleted.values().stream()
            .anyMatch(alpha -> alpha != null && alpha.compareTo(cronbachAlpha) > 0);
    }

    /**
     * Get the item that most lowers reliability.
     * @return UUID of the item that, if removed, would most improve alpha; null if none
     */
    @Transient
    public UUID getMostProblematicItem() {
        if (cronbachAlpha == null || alphaIfDeleted == null || alphaIfDeleted.isEmpty()) {
            return null;
        }

        UUID worstItem = null;
        BigDecimal highestImprovement = BigDecimal.ZERO;

        for (Map.Entry<UUID, BigDecimal> entry : alphaIfDeleted.entrySet()) {
            if (entry.getValue() != null) {
                BigDecimal improvement = entry.getValue().subtract(cronbachAlpha);
                if (improvement.compareTo(highestImprovement) > 0) {
                    highestImprovement = improvement;
                    worstItem = entry.getKey();
                }
            }
        }

        return worstItem;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Competency getCompetency() {
        return competency;
    }

    public void setCompetency(Competency competency) {
        this.competency = competency;
    }

    @Transient
    public UUID getCompetencyId() {
        return competency != null ? competency.getId() : null;
    }

    public BigDecimal getCronbachAlpha() {
        return cronbachAlpha;
    }

    public void setCronbachAlpha(BigDecimal cronbachAlpha) {
        this.cronbachAlpha = cronbachAlpha;
    }

    public Integer getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(Integer sampleSize) {
        this.sampleSize = sampleSize;
    }

    public Integer getItemCount() {
        return itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }

    public ReliabilityStatus getReliabilityStatus() {
        return reliabilityStatus;
    }

    public void setReliabilityStatus(ReliabilityStatus reliabilityStatus) {
        this.reliabilityStatus = reliabilityStatus;
    }

    public Map<UUID, BigDecimal> getAlphaIfDeleted() {
        return alphaIfDeleted;
    }

    public void setAlphaIfDeleted(Map<UUID, BigDecimal> alphaIfDeleted) {
        this.alphaIfDeleted = alphaIfDeleted;
    }

    public LocalDateTime getLastCalculatedAt() {
        return lastCalculatedAt;
    }

    public void setLastCalculatedAt(LocalDateTime lastCalculatedAt) {
        this.lastCalculatedAt = lastCalculatedAt;
    }
}
