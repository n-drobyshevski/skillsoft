package app.skillsoft.assessmentbackend.domain.entities;


import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name="assessment_questions",
       uniqueConstraints = {
           @UniqueConstraint(name = "uc_assessmentquestion_indicator_text",
                             columnNames = {"behavioral_indicator_id", "question_text"}),
           @UniqueConstraint(name = "uc_assessmentquestion_indicator_order",
                             columnNames = {"behavioral_indicator_id", "order_index"})
       })
public class AssessmentQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "behavioral_indicator_id", nullable = false)
    private BehavioralIndicator behavioralIndicator;

    @Column(name = "question_text", nullable = false, length = 500)
    private String questionText;

    @Column(name = "question_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private QuestionType questionType;

    @Column(name = "answer_options", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Map<String, Object>> answerOptions;

    @Column(name="scoring_rubric", nullable = false)
    private String scoringRubric;

    @Column(name = "time_limit")
    private Integer timeLimit; // in seconds

    @Column(name="difficulty_level", nullable = false)
    @Enumerated(EnumType.STRING)
    private DifficultyLevel difficultyLevel;

    @Column(name="is_active", nullable = false)
    private boolean isActive;

    @Column(name="order_index", nullable = false)
    private int orderIndex;

    // Constructors
    public AssessmentQuestion() {
        // Default constructor required by JPA
    }

    public AssessmentQuestion(UUID id, BehavioralIndicator behavioralIndicator, String questionText, 
                             QuestionType questionType, List<Map<String, Object>> answerOptions, 
                             String scoringRubric, Integer timeLimit, DifficultyLevel difficultyLevel, 
                             boolean isActive, int orderIndex) {
        this.id = id;
        this.behavioralIndicator = behavioralIndicator;
        this.questionText = questionText;
        this.questionType = questionType;
        this.answerOptions = answerOptions;
        this.scoringRubric = scoringRubric;
        this.timeLimit = timeLimit;
        this.difficultyLevel = difficultyLevel;
        this.isActive = isActive;
        this.orderIndex = orderIndex;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public BehavioralIndicator getBehavioralIndicator() {
        return behavioralIndicator;
    }

    public void setBehavioralIndicator(BehavioralIndicator behavioralIndicator) {
        this.behavioralIndicator = behavioralIndicator;
    }
    
    /**
     * Convenience method for setting the behavioral indicator by ID
     * Used primarily in tests and when entity relationships need to be managed manually
     */
    @Transient
    public void setBehavioralIndicatorId(UUID id) {
        if (this.behavioralIndicator == null) {
            this.behavioralIndicator = new BehavioralIndicator();
        }
        this.behavioralIndicator.setId(id);
    }
    
    /**
     * Get the ID of the associated behavioral indicator
     * @return UUID of the behavioral indicator or null if not set
     */
    @Transient
    public UUID getBehavioralIndicatorId() {
        return this.behavioralIndicator != null ? this.behavioralIndicator.getId() : null;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
    }

    public List<Map<String, Object>> getAnswerOptions() {
        return answerOptions;
    }

    public void setAnswerOptions(List<Map<String, Object>> answerOptions) {
        this.answerOptions = answerOptions;
    }

    public String getScoringRubric() {
        return scoringRubric;
    }

    public void setScoringRubric(String scoringRubric) {
        this.scoringRubric = scoringRubric;
    }

    public Integer getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(Integer timeLimit) {
        this.timeLimit = timeLimit;
    }

    public DifficultyLevel getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(DifficultyLevel difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    // Object methods
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AssessmentQuestion that = (AssessmentQuestion) o;
        return isActive() == that.isActive() && getOrderIndex() == that.getOrderIndex() && 
               Objects.equals(getId(), that.getId()) && 
               Objects.equals(getBehavioralIndicator(), that.getBehavioralIndicator()) && 
               Objects.equals(getQuestionText(), that.getQuestionText()) && 
               getQuestionType() == that.getQuestionType() && 
               Objects.equals(getAnswerOptions(), that.getAnswerOptions()) && 
               Objects.equals(getScoringRubric(), that.getScoringRubric()) && 
               Objects.equals(getTimeLimit(), that.getTimeLimit()) && 
               getDifficultyLevel() == that.getDifficultyLevel();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getBehavioralIndicator(), getQuestionText(), 
                           getQuestionType(), getAnswerOptions(), getScoringRubric(), 
                           getTimeLimit(), getDifficultyLevel(), isActive(), getOrderIndex());
    }

    @Override
    public String toString() {
        return "AssessmentQuestion{" +
                "id=" + id +
                ", behavioralIndicator=" + behavioralIndicator +
                ", questionText='" + questionText + '\'' +
                ", questionType=" + questionType +
                ", answerOptions=" + answerOptions +
                ", scoringRubric='" + scoringRubric + '\'' +
                ", timeLimit=" + timeLimit +
                ", difficultyLevel=" + difficultyLevel +
                ", isActive=" + isActive +
                ", orderIndex=" + orderIndex +
                '}';
    }
}
