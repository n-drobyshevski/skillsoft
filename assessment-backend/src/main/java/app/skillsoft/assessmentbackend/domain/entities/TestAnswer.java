package app.skillsoft.assessmentbackend.domain.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing an answer to a question within a test session.
 * Supports multiple question types: single choice, multiple choice, Likert scale, ranking, and text.
 */
@Entity
@Table(name = "test_answers", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"session_id", "question_id"})
}, indexes = {
    @Index(name = "idx_test_answer_session_id", columnList = "session_id")
})
public class TestAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private TestSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private AssessmentQuestion question;

    /**
     * Selected option IDs for single/multiple choice questions.
     * Stored as JSONB array for flexibility.
     */
    @Column(name = "selected_option_ids", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> selectedOptionIds;

    /**
     * Value for Likert scale questions (typically 1-5 or 1-7).
     */
    @Column(name = "likert_value")
    private Integer likertValue;

    /**
     * Ordered list of option IDs for ranking questions.
     * The order represents the user's ranking preference.
     */
    @Column(name = "ranking_order", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> rankingOrder;

    /**
     * Free-text response for open-ended questions.
     */
    @Column(name = "text_response", columnDefinition = "TEXT")
    private String textResponse;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    /**
     * Time spent on this question in seconds.
     */
    @Column(name = "time_spent_seconds")
    private Integer timeSpentSeconds = 0;

    @Column(name = "is_skipped")
    private Boolean isSkipped = false;

    /**
     * Score achieved for this answer (null until graded).
     */
    @Column(name = "score")
    private Double score;

    /**
     * Maximum possible score for this question.
     */
    @Column(name = "max_score")
    private Double maxScore;

    // Constructors
    public TestAnswer() {
        // Default constructor required by JPA
    }

    public TestAnswer(TestSession session, AssessmentQuestion question) {
        this.session = session;
        this.question = question;
    }

    // Business methods
    public void skip() {
        this.isSkipped = true;
        this.answeredAt = LocalDateTime.now();
    }

    public void answerSingleChoice(String optionId, int timeSpentSeconds) {
        this.selectedOptionIds = List.of(optionId);
        this.timeSpentSeconds = timeSpentSeconds;
        this.answeredAt = LocalDateTime.now();
        this.isSkipped = false;
    }

    public void answerMultipleChoice(List<String> optionIds, int timeSpentSeconds) {
        this.selectedOptionIds = optionIds;
        this.timeSpentSeconds = timeSpentSeconds;
        this.answeredAt = LocalDateTime.now();
        this.isSkipped = false;
    }

    public void answerLikert(int value, int timeSpentSeconds) {
        this.likertValue = value;
        this.timeSpentSeconds = timeSpentSeconds;
        this.answeredAt = LocalDateTime.now();
        this.isSkipped = false;
    }

    public void answerRanking(List<String> orderedOptionIds, int timeSpentSeconds) {
        this.rankingOrder = orderedOptionIds;
        this.timeSpentSeconds = timeSpentSeconds;
        this.answeredAt = LocalDateTime.now();
        this.isSkipped = false;
    }

    public void answerText(String response, int timeSpentSeconds) {
        this.textResponse = response;
        this.timeSpentSeconds = timeSpentSeconds;
        this.answeredAt = LocalDateTime.now();
        this.isSkipped = false;
    }

    public boolean isAnswered() {
        return answeredAt != null && !isSkipped;
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

    public AssessmentQuestion getQuestion() {
        return question;
    }

    public void setQuestion(AssessmentQuestion question) {
        this.question = question;
    }

    public List<String> getSelectedOptionIds() {
        return selectedOptionIds;
    }

    public void setSelectedOptionIds(List<String> selectedOptionIds) {
        this.selectedOptionIds = selectedOptionIds;
    }

    public Integer getLikertValue() {
        return likertValue;
    }

    public void setLikertValue(Integer likertValue) {
        this.likertValue = likertValue;
    }

    public List<String> getRankingOrder() {
        return rankingOrder;
    }

    public void setRankingOrder(List<String> rankingOrder) {
        this.rankingOrder = rankingOrder;
    }

    public String getTextResponse() {
        return textResponse;
    }

    public void setTextResponse(String textResponse) {
        this.textResponse = textResponse;
    }

    public LocalDateTime getAnsweredAt() {
        return answeredAt;
    }

    public void setAnsweredAt(LocalDateTime answeredAt) {
        this.answeredAt = answeredAt;
    }

    public Integer getTimeSpentSeconds() {
        return timeSpentSeconds;
    }

    public void setTimeSpentSeconds(Integer timeSpentSeconds) {
        this.timeSpentSeconds = timeSpentSeconds;
    }

    public Boolean getIsSkipped() {
        return isSkipped;
    }

    public void setIsSkipped(Boolean isSkipped) {
        this.isSkipped = isSkipped;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Double getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(Double maxScore) {
        this.maxScore = maxScore;
    }

    // Convenience methods
    @Transient
    public UUID getSessionId() {
        return session != null ? session.getId() : null;
    }

    @Transient
    public UUID getQuestionId() {
        return question != null ? question.getId() : null;
    }

    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestAnswer that = (TestAnswer) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "TestAnswer{" +
                "id=" + id +
                ", isSkipped=" + isSkipped +
                ", score=" + score +
                ", maxScore=" + maxScore +
                '}';
    }
}
