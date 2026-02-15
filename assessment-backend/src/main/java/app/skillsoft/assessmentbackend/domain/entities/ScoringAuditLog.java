package app.skillsoft.assessmentbackend.domain.entities;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Audit log entity for scoring calculations.
 * Provides full traceability of HOW a score was computed.
 *
 * Stores a snapshot of all inputs (weights, answers, config) and outputs (scores, percentiles)
 * at the time of scoring. This enables:
 * - Debugging scoring disputes
 * - Validating psychometric changes
 * - Regulatory compliance
 * - Score reconstruction from event history
 *
 * Uses JSONB columns for flexible schema-less storage of scoring details.
 */
@Entity
@Table(name = "scoring_audit_logs", indexes = {
    @Index(name = "idx_scoring_audit_session", columnList = "session_id"),
    @Index(name = "idx_scoring_audit_result", columnList = "result_id"),
    @Index(name = "idx_scoring_audit_user", columnList = "clerk_user_id"),
    @Index(name = "idx_scoring_audit_created", columnList = "created_at")
})
public class ScoringAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "result_id", nullable = false)
    private UUID resultId;

    @Column(name = "clerk_user_id")
    private String clerkUserId;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "goal", nullable = false)
    @Enumerated(EnumType.STRING)
    private AssessmentGoal goal;

    @Column(name = "strategy_class", nullable = false)
    private String strategyClass;

    @Column(name = "overall_score")
    private Double overallScore;

    @Column(name = "overall_percentage")
    private Double overallPercentage;

    @Column(name = "passed")
    private Boolean passed;

    @Column(name = "percentile")
    private Integer percentile;

    /**
     * Snapshot of indicator weights used in this calculation.
     * Map of indicatorId -> weight value.
     */
    @Type(JsonType.class)
    @Column(name = "indicator_weights", columnDefinition = "jsonb")
    private Map<String, Double> indicatorWeights;

    /**
     * Per-competency score breakdown with weighted details.
     * List of maps containing competencyId, percentage, weightedScore, indicatorCount.
     */
    @Type(JsonType.class)
    @Column(name = "competency_breakdown", columnDefinition = "jsonb")
    private List<Map<String, Object>> competencyBreakdown;

    /**
     * Scoring configuration snapshot at time of calculation.
     * Captures thresholds, boost values, and other config used.
     */
    @Type(JsonType.class)
    @Column(name = "config_snapshot", columnDefinition = "jsonb")
    private Map<String, Object> configSnapshot;

    /**
     * Answer count statistics.
     */
    @Column(name = "total_answers")
    private Integer totalAnswers;

    @Column(name = "answered_count")
    private Integer answeredCount;

    @Column(name = "skipped_count")
    private Integer skippedCount;

    /**
     * Duration of the scoring calculation in milliseconds.
     */
    @Column(name = "scoring_duration_ms")
    private Long scoringDurationMs;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Constructors
    public ScoringAuditLog() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getSessionId() { return sessionId; }
    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }

    public UUID getResultId() { return resultId; }
    public void setResultId(UUID resultId) { this.resultId = resultId; }

    public String getClerkUserId() { return clerkUserId; }
    public void setClerkUserId(String clerkUserId) { this.clerkUserId = clerkUserId; }

    public UUID getTemplateId() { return templateId; }
    public void setTemplateId(UUID templateId) { this.templateId = templateId; }

    public AssessmentGoal getGoal() { return goal; }
    public void setGoal(AssessmentGoal goal) { this.goal = goal; }

    public String getStrategyClass() { return strategyClass; }
    public void setStrategyClass(String strategyClass) { this.strategyClass = strategyClass; }

    public Double getOverallScore() { return overallScore; }
    public void setOverallScore(Double overallScore) { this.overallScore = overallScore; }

    public Double getOverallPercentage() { return overallPercentage; }
    public void setOverallPercentage(Double overallPercentage) { this.overallPercentage = overallPercentage; }

    public Boolean getPassed() { return passed; }
    public void setPassed(Boolean passed) { this.passed = passed; }

    public Integer getPercentile() { return percentile; }
    public void setPercentile(Integer percentile) { this.percentile = percentile; }

    public Map<String, Double> getIndicatorWeights() { return indicatorWeights; }
    public void setIndicatorWeights(Map<String, Double> indicatorWeights) { this.indicatorWeights = indicatorWeights; }

    public List<Map<String, Object>> getCompetencyBreakdown() { return competencyBreakdown; }
    public void setCompetencyBreakdown(List<Map<String, Object>> competencyBreakdown) { this.competencyBreakdown = competencyBreakdown; }

    public Map<String, Object> getConfigSnapshot() { return configSnapshot; }
    public void setConfigSnapshot(Map<String, Object> configSnapshot) { this.configSnapshot = configSnapshot; }

    public Integer getTotalAnswers() { return totalAnswers; }
    public void setTotalAnswers(Integer totalAnswers) { this.totalAnswers = totalAnswers; }

    public Integer getAnsweredCount() { return answeredCount; }
    public void setAnsweredCount(Integer answeredCount) { this.answeredCount = answeredCount; }

    public Integer getSkippedCount() { return skippedCount; }
    public void setSkippedCount(Integer skippedCount) { this.skippedCount = skippedCount; }

    public Long getScoringDurationMs() { return scoringDurationMs; }
    public void setScoringDurationMs(Long scoringDurationMs) { this.scoringDurationMs = scoringDurationMs; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
