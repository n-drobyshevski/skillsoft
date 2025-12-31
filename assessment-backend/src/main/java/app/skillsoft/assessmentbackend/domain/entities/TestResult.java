package app.skillsoft.assessmentbackend.domain.entities;

import app.skillsoft.assessmentbackend.domain.dto.CompetencyScoreDto;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing the final result of a completed test session.
 * Contains overall scores, per-competency breakdown, and summary statistics.
 */
@Entity
@Table(name = "test_results")
public class TestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private TestSession session;

    @Column(name = "clerk_user_id", nullable = false)
    private String clerkUserId;

    /**
     * Total score achieved across all questions.
     */
    @Column(name = "overall_score")
    private Double overallScore;

    /**
     * Overall percentage score (0-100).
     */
    @Column(name = "overall_percentage")
    private Double overallPercentage;

    /**
     * Percentile ranking compared to other test takers.
     * Calculated asynchronously based on historical data.
     */
    @Column(name = "percentile")
    private Integer percentile;

    /**
     * Whether the user passed based on the template's passing score.
     */
    @Column(name = "passed")
    private Boolean passed;

    /**
     * Status of the result calculation.
     * Used to track pending results awaiting retry after transient failures.
     * Defaults to COMPLETED for backward compatibility with existing results.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ResultStatus status = ResultStatus.COMPLETED;

    /**
     * Detailed scores broken down by competency.
     * Stored as JSONB for flexibility.
     */
    @Column(name = "competency_scores", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<CompetencyScoreDto> competencyScores = new ArrayList<>();

    /**
     * Big Five personality profile (only populated for TEAM_FIT goal).
     * Maps trait names (e.g., "Openness", "Conscientiousness") to percentage scores (0-100).
     * Stored as JSONB for flexible personality analysis storage.
     */
    @Column(name = "big_five_profile", columnDefinition = "jsonb")
    @Type(JsonType.class)
    private Map<String, Double> bigFiveProfile;

    /**
     * Extended metrics for goal-specific analysis.
     * For TEAM_FIT: Contains TeamFitMetrics (diversityRatio, saturationRatio, etc.)
     * Stored as JSONB for flexible extension without schema changes.
     */
    @Column(name = "extended_metrics", columnDefinition = "jsonb")
    @Type(JsonType.class)
    private Map<String, Object> extendedMetrics;

    /**
     * Total time spent on the test in seconds.
     */
    @Column(name = "total_time_seconds")
    private Integer totalTimeSeconds;

    /**
     * Number of questions actually answered (not skipped).
     */
    @Column(name = "questions_answered")
    private Integer questionsAnswered;

    /**
     * Number of questions that were skipped.
     */
    @Column(name = "questions_skipped")
    private Integer questionsSkipped;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // Constructors
    public TestResult() {
        // Default constructor required by JPA
    }

    public TestResult(TestSession session, String clerkUserId) {
        this.session = session;
        this.clerkUserId = clerkUserId;
    }

    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        if (completedAt == null) {
            completedAt = LocalDateTime.now();
        }
    }

    // Business methods
    public void calculatePassed(Double passingScore) {
        this.passed = this.overallPercentage != null && this.overallPercentage >= passingScore;
    }

    public void addCompetencyScore(CompetencyScoreDto competencyScore) {
        if (this.competencyScores == null) {
            this.competencyScores = new ArrayList<>();
        }
        this.competencyScores.add(competencyScore);
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public TestSession getSession() {
        return session;
    }

    public void setSession(TestSession session) {
        this.session = session;
    }

    public String getClerkUserId() {
        return clerkUserId;
    }

    public void setClerkUserId(String clerkUserId) {
        this.clerkUserId = clerkUserId;
    }

    public Double getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(Double overallScore) {
        this.overallScore = overallScore;
    }

    public Double getOverallPercentage() {
        return overallPercentage;
    }

    public void setOverallPercentage(Double overallPercentage) {
        this.overallPercentage = overallPercentage;
    }

    public Integer getPercentile() {
        return percentile;
    }

    public void setPercentile(Integer percentile) {
        this.percentile = percentile;
    }

    public Boolean getPassed() {
        return passed;
    }

    public void setPassed(Boolean passed) {
        this.passed = passed;
    }

    public ResultStatus getStatus() {
        return status;
    }

    public void setStatus(ResultStatus status) {
        this.status = status;
    }

    public List<CompetencyScoreDto> getCompetencyScores() {
        return competencyScores;
    }

    public void setCompetencyScores(List<CompetencyScoreDto> competencyScores) {
        this.competencyScores = competencyScores;
    }

    public Map<String, Double> getBigFiveProfile() {
        return bigFiveProfile;
    }

    public void setBigFiveProfile(Map<String, Double> bigFiveProfile) {
        this.bigFiveProfile = bigFiveProfile;
    }

    public Map<String, Object> getExtendedMetrics() {
        return extendedMetrics;
    }

    public void setExtendedMetrics(Map<String, Object> extendedMetrics) {
        this.extendedMetrics = extendedMetrics;
    }

    public Integer getTotalTimeSeconds() {
        return totalTimeSeconds;
    }

    public void setTotalTimeSeconds(Integer totalTimeSeconds) {
        this.totalTimeSeconds = totalTimeSeconds;
    }

    public Integer getQuestionsAnswered() {
        return questionsAnswered;
    }

    public void setQuestionsAnswered(Integer questionsAnswered) {
        this.questionsAnswered = questionsAnswered;
    }

    public Integer getQuestionsSkipped() {
        return questionsSkipped;
    }

    public void setQuestionsSkipped(Integer questionsSkipped) {
        this.questionsSkipped = questionsSkipped;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    // Convenience methods
    @Transient
    public UUID getSessionId() {
        return session != null ? session.getId() : null;
    }

    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestResult that = (TestResult) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "TestResult{" +
                "id=" + id +
                ", clerkUserId='" + clerkUserId + '\'' +
                ", overallPercentage=" + overallPercentage +
                ", passed=" + passed +
                ", status=" + status +
                '}';
    }
}
