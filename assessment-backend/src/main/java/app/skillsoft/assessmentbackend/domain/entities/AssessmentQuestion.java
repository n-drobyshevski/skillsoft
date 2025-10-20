package app.skillsoft.assessmentbackend.domain.entities;


import jakarta.persistence.*;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name="assessment_questions")
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

    @Column(name="answer_options")
    private String answerOptions;

    @Column(name="scoring_rubric", nullable = false)
    private String scoringRubric;

    @Column(name = "time_limit")
    private Integer timeLimit; // in seconds

    @Column(name="difficulty_level", nullable = false)
    @Enumerated(EnumType.STRING)
    private DifficultyLevel difficultyLevel;

    @Column(name="is_active", nullable = false)
    private boolean isActive;

    public UUID getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AssessmentQuestion that = (AssessmentQuestion) o;
        return isActive() == that.isActive() && getOrderIndex() == that.getOrderIndex() && Objects.equals(getId(), that.getId()) && Objects.equals(getBehavioralIndicator(), that.getBehavioralIndicator()) && Objects.equals(getQuestionText(), that.getQuestionText()) && getQuestionType() == that.getQuestionType() && Objects.equals(getAnswerOptions(), that.getAnswerOptions()) && Objects.equals(getScoringRubric(), that.getScoringRubric()) && Objects.equals(getTimeLimit(), that.getTimeLimit()) && getDifficultyLevel() == that.getDifficultyLevel();
    }

    @Override
    public String toString() {
        return "AssesmentQuestion{" +
                "id=" + id +
                ", behavioralIndicator=" + behavioralIndicator +
                ", questionText='" + questionText + '\'' +
                ", questionType=" + questionType +
                ", answerOptions='" + answerOptions + '\'' +
                ", scoringRubric='" + scoringRubric + '\'' +
                ", timeLimit=" + timeLimit +
                ", difficultyLevel=" + difficultyLevel +
                ", isActive=" + isActive +
                ", orderIndex=" + orderIndex +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getBehavioralIndicator(), getQuestionText(), getQuestionType(), getAnswerOptions(), getScoringRubric(), getTimeLimit(), getDifficultyLevel(), isActive(), getOrderIndex());
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public AssessmentQuestion(UUID id, BehavioralIndicator behavioralIndicator, String questionText, QuestionType questionType, String answerOptions, String scoringRubric, Integer timeLimit, DifficultyLevel difficultyLevel, boolean isActive, int orderIndex) {
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
    public AssessmentQuestion() {
        // Default constructor required by JPA
    }

    public BehavioralIndicator getBehavioralIndicator() {
        return behavioralIndicator;
    }

    public void setBehavioralIndicator(BehavioralIndicator behavioralIndicator) {
        this.behavioralIndicator = behavioralIndicator;
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

    public String getAnswerOptions() {
        return answerOptions;
    }

    public void setAnswerOptions(String answerOptions) {
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

    @Column(name="order_index", nullable = false)
    private int orderIndex;
}
