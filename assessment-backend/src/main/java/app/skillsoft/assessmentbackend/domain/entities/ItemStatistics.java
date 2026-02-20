package app.skillsoft.assessmentbackend.domain.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ItemStatistics entity storing psychometric metrics for assessment questions.
 *
 * Per the Test Validation Mechanic architecture:
 * - Tracks difficulty index (p-value): proportion of correct/positive responses
 * - Tracks discrimination index (rpb): point-biserial correlation with total score
 * - Tracks distractor efficiency for MCQ questions
 * - Manages item validity status lifecycle: PROBATION -> ACTIVE/FLAGGED/RETIRED
 *
 * Calculations are performed by PsychometricAnalysisService and updated by PsychometricAuditJob.
 */
@Entity
@Table(name = "item_statistics", indexes = {
    @Index(name = "idx_item_stats_question", columnList = "question_id"),
    @Index(name = "idx_item_stats_validity", columnList = "validity_status"),
    @Index(name = "idx_item_stats_last_calc", columnList = "last_calculated_at"),
    @Index(name = "idx_item_stats_response_count", columnList = "response_count")
})
public class ItemStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The assessment question these statistics belong to.
     * One-to-one relationship: each question has exactly one statistics record.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false, unique = true)
    private AssessmentQuestion question;

    /**
     * Difficulty Index (p-value): Average normalized score (0.0 - 1.0).
     * - p < 0.2: Too hard (most respondents fail)
     * - 0.2 <= p <= 0.9: Acceptable range
     * - p > 0.9: Too easy (most respondents succeed)
     *
     * Null if insufficient responses for calculation.
     */
    @Column(name = "difficulty_index", precision = 5, scale = 4)
    private BigDecimal difficultyIndex;

    /**
     * Discrimination Index (rpb): Point-Biserial correlation (-1.0 to 1.0).
     * Measures correlation between item score and total test score.
     * - rpb < 0: Toxic (high-skill users get it wrong)
     * - 0 <= rpb < 0.1: Critical (poor discrimination)
     * - 0.1 <= rpb < 0.25: Warning (marginal)
     * - rpb >= 0.25: Good
     * - rpb >= 0.3: Excellent (ACTIVE status threshold)
     *
     * Null if insufficient responses for calculation.
     */
    @Column(name = "discrimination_index", precision = 5, scale = 4)
    private BigDecimal discriminationIndex;

    /**
     * Distractor Efficiency (MCQ/SJT only): Selection percentage for each option.
     * Maps option ID to selection percentage (0.0 - 1.0).
     * Flag if any distractor has 0% selection (non-functioning).
     *
     * Example: {"opt_a": 0.25, "opt_b": 0.35, "opt_c": 0.30, "opt_d": 0.10}
     */
    @Column(name = "distractor_efficiency", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Double> distractorEfficiency;

    /**
     * Total number of responses used in the last calculation.
     * Minimum 50 responses required for reliable psychometric analysis.
     */
    @Column(name = "response_count", nullable = false)
    private Integer responseCount = 0;

    /**
     * Timestamp of the last psychometric calculation.
     * Used to determine if recalculation is needed.
     */
    @Column(name = "last_calculated_at")
    private LocalDateTime lastCalculatedAt;

    /**
     * Validity status determining item eligibility for test assembly.
     * - ACTIVE: Validated, included in test assembly
     * - PROBATION: New item gathering data (< 50 responses)
     * - FLAGGED_FOR_REVIEW: Requires manual HR review
     * - RETIRED: Removed from active use (deactivates question)
     */
    @Column(name = "validity_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ItemValidityStatus validityStatus = ItemValidityStatus.PROBATION;

    /**
     * Flag indicating difficulty index issues.
     */
    @Column(name = "difficulty_flag")
    @Enumerated(EnumType.STRING)
    private DifficultyFlag difficultyFlag;

    /**
     * Flag indicating discrimination index issues.
     */
    @Column(name = "discrimination_flag")
    @Enumerated(EnumType.STRING)
    private DiscriminationFlag discriminationFlag;

    /**
     * Previous discrimination index for trend analysis.
     * Stored before each recalculation to detect degradation.
     */
    @Column(name = "previous_discrimination_index", precision = 5, scale = 4)
    private BigDecimal previousDiscriminationIndex;

    /**
     * IRT 2PL discrimination parameter (a).
     * Higher values indicate the item better differentiates between ability levels.
     * Typical range: 0.5 to 2.5
     * - a < 0.5: Poor discrimination
     * - 0.5 <= a < 1.0: Low discrimination
     * - 1.0 <= a < 1.5: Moderate discrimination
     * - a >= 1.5: High discrimination
     */
    @Column(name = "irt_discrimination", precision = 6, scale = 4)
    private BigDecimal irtDiscrimination;

    /**
     * IRT 2PL difficulty parameter (b).
     * Represents the ability level at which there is a 50% chance of correct response.
     * Measured on the same scale as ability (theta), typically -3 to +3.
     * - b < -2: Very easy
     * - -2 <= b < -1: Easy
     * - -1 <= b <= 1: Medium
     * - 1 < b <= 2: Hard
     * - b > 2: Very hard
     */
    @Column(name = "irt_difficulty", precision = 6, scale = 4)
    private BigDecimal irtDifficulty;

    /**
     * IRT 3PL guessing parameter (c).
     * Lower asymptote representing the probability of a correct response by guessing.
     * For 2PL model, this is fixed at 0.
     * Stored for future 3PL model support.
     * Typical range: 0.0 to 0.35
     */
    @Column(name = "irt_guessing", precision = 5, scale = 4)
    private BigDecimal irtGuessing;

    /**
     * History of status changes for audit trail.
     * Each entry contains: fromStatus, toStatus, timestamp, reason
     */
    @Column(name = "status_change_history", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<StatusChangeRecord> statusChangeHistory;

    // Constructors
    public ItemStatistics() {
        this.statusChangeHistory = new ArrayList<>();
    }

    public ItemStatistics(AssessmentQuestion question) {
        this();
        this.question = question;
        this.validityStatus = ItemValidityStatus.PROBATION;
        this.responseCount = 0;
    }

    // Business Methods

    /**
     * Add a status change to the history for audit purposes.
     */
    public void addStatusChange(ItemValidityStatus fromStatus, ItemValidityStatus toStatus,
                                LocalDateTime timestamp, String reason) {
        if (this.statusChangeHistory == null) {
            this.statusChangeHistory = new ArrayList<>();
        }
        this.statusChangeHistory.add(new StatusChangeRecord(fromStatus, toStatus, timestamp, reason));
    }

    /**
     * Check if the item has sufficient responses for reliable analysis.
     */
    @Transient
    public boolean hasSufficientResponses() {
        return responseCount != null && responseCount >= 50;
    }

    /**
     * Check if discrimination is toxic (negative correlation).
     */
    @Transient
    public boolean isToxic() {
        return discriminationIndex != null && discriminationIndex.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Check if item needs recalculation based on new responses.
     * @param newResponseThreshold Minimum new responses to trigger recalculation
     */
    @Transient
    public boolean needsRecalculation(int newResponseThreshold) {
        if (lastCalculatedAt == null) {
            return responseCount >= 50;
        }
        // Would need to compare with actual response count from repository
        return false;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public AssessmentQuestion getQuestion() {
        return question;
    }

    public void setQuestion(AssessmentQuestion question) {
        this.question = question;
    }

    @Transient
    public UUID getQuestionId() {
        return question != null ? question.getId() : null;
    }

    public BigDecimal getDifficultyIndex() {
        return difficultyIndex;
    }

    public void setDifficultyIndex(BigDecimal difficultyIndex) {
        this.difficultyIndex = difficultyIndex;
    }

    public BigDecimal getDiscriminationIndex() {
        return discriminationIndex;
    }

    public void setDiscriminationIndex(BigDecimal discriminationIndex) {
        this.discriminationIndex = discriminationIndex;
    }

    public Map<String, Double> getDistractorEfficiency() {
        return distractorEfficiency;
    }

    public void setDistractorEfficiency(Map<String, Double> distractorEfficiency) {
        this.distractorEfficiency = distractorEfficiency;
    }

    public Integer getResponseCount() {
        return responseCount;
    }

    public void setResponseCount(Integer responseCount) {
        this.responseCount = responseCount;
    }

    public LocalDateTime getLastCalculatedAt() {
        return lastCalculatedAt;
    }

    public void setLastCalculatedAt(LocalDateTime lastCalculatedAt) {
        this.lastCalculatedAt = lastCalculatedAt;
    }

    public ItemValidityStatus getValidityStatus() {
        return validityStatus;
    }

    public void setValidityStatus(ItemValidityStatus validityStatus) {
        this.validityStatus = validityStatus;
    }

    public DifficultyFlag getDifficultyFlag() {
        return difficultyFlag;
    }

    public void setDifficultyFlag(DifficultyFlag difficultyFlag) {
        this.difficultyFlag = difficultyFlag;
    }

    public DiscriminationFlag getDiscriminationFlag() {
        return discriminationFlag;
    }

    public void setDiscriminationFlag(DiscriminationFlag discriminationFlag) {
        this.discriminationFlag = discriminationFlag;
    }

    public BigDecimal getPreviousDiscriminationIndex() {
        return previousDiscriminationIndex;
    }

    public void setPreviousDiscriminationIndex(BigDecimal previousDiscriminationIndex) {
        this.previousDiscriminationIndex = previousDiscriminationIndex;
    }

    public BigDecimal getIrtDiscrimination() {
        return irtDiscrimination;
    }

    public void setIrtDiscrimination(BigDecimal irtDiscrimination) {
        this.irtDiscrimination = irtDiscrimination;
    }

    public BigDecimal getIrtDifficulty() {
        return irtDifficulty;
    }

    public void setIrtDifficulty(BigDecimal irtDifficulty) {
        this.irtDifficulty = irtDifficulty;
    }

    public BigDecimal getIrtGuessing() {
        return irtGuessing;
    }

    public void setIrtGuessing(BigDecimal irtGuessing) {
        this.irtGuessing = irtGuessing;
    }

    public List<StatusChangeRecord> getStatusChangeHistory() {
        return statusChangeHistory;
    }

    public void setStatusChangeHistory(List<StatusChangeRecord> statusChangeHistory) {
        this.statusChangeHistory = statusChangeHistory;
    }

    /**
     * Record for tracking status change history.
     */
    public record StatusChangeRecord(
        ItemValidityStatus fromStatus,
        ItemValidityStatus toStatus,
        LocalDateTime timestamp,
        String reason
    ) {}
}
