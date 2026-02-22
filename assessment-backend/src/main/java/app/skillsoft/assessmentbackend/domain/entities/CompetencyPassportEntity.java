package app.skillsoft.assessmentbackend.domain.entities;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Persistent storage for a candidate's Competency Passport.
 *
 * Stores reusable assessment results from OVERVIEW (Universal Baseline) tests,
 * enabling Delta Testing where only competency gaps need reassessment in
 * subsequent JOB_FIT assessments.
 *
 * One passport per Clerk user (upserted on each OVERVIEW completion).
 */
@Entity
@Table(name = "competency_passports", indexes = {
    @Index(name = "idx_passport_expires_at", columnList = "expires_at"),
    @Index(name = "idx_passport_source_result_id", columnList = "source_result_id")
})
public class CompetencyPassportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "clerk_user_id", nullable = false, unique = true, length = 100)
    private String clerkUserId;

    /**
     * Map of competency UUID (as String) to score (1.0–5.0 scale).
     * String keys for JSONB compatibility; UUID conversion happens in service layer.
     */
    @Column(name = "competency_scores", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Double> competencyScores = new HashMap<>();

    /**
     * Big Five personality profile.
     * Keys: OPENNESS, CONSCIENTIOUSNESS, EXTRAVERSION, AGREEABLENESS, NEUROTICISM
     */
    @Column(name = "big_five_profile", columnDefinition = "jsonb")
    @Type(JsonType.class)
    private Map<String, Double> bigFiveProfile;

    @Column(name = "last_assessed", nullable = false)
    private LocalDateTime lastAssessed;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * ID of the TestResult that produced this passport (for traceability).
     */
    @Column(name = "source_result_id")
    private UUID sourceResultId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors

    public CompetencyPassportEntity() {
    }

    // Business methods

    /**
     * Check if the passport is still valid (not expired).
     */
    @Transient
    public boolean isValid() {
        return expiresAt != null && expiresAt.isAfter(LocalDateTime.now());
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getClerkUserId() {
        return clerkUserId;
    }

    public void setClerkUserId(String clerkUserId) {
        this.clerkUserId = clerkUserId;
    }

    public Map<String, Double> getCompetencyScores() {
        return competencyScores;
    }

    public void setCompetencyScores(Map<String, Double> competencyScores) {
        this.competencyScores = competencyScores;
    }

    public Map<String, Double> getBigFiveProfile() {
        return bigFiveProfile;
    }

    public void setBigFiveProfile(Map<String, Double> bigFiveProfile) {
        this.bigFiveProfile = bigFiveProfile;
    }

    public LocalDateTime getLastAssessed() {
        return lastAssessed;
    }

    public void setLastAssessed(LocalDateTime lastAssessed) {
        this.lastAssessed = lastAssessed;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public UUID getSourceResultId() {
        return sourceResultId;
    }

    public void setSourceResultId(UUID sourceResultId) {
        this.sourceResultId = sourceResultId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // equals and hashCode

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompetencyPassportEntity that = (CompetencyPassportEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "CompetencyPassportEntity{" +
                "id=" + id +
                ", clerkUserId='" + clerkUserId + '\'' +
                ", lastAssessed=" + lastAssessed +
                ", expiresAt=" + expiresAt +
                ", valid=" + isValid() +
                '}';
    }
}
