package app.skillsoft.assessmentbackend.domain.entities;


import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;
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

    /**
     * Metadata JSONB field for flexible tagging and context filtering.
     * Per ROADMAP.md Section 1.B: "Allows filtering questions by context and difficulty"
     * 
     * Schema example:
     * {
     *   "tags": ["IT", "JUNIOR", "GENERAL"],
     *   "difficulty": "HARD",
     *   "time_limit_sec": 60,
     *   "context": "UNIVERSAL",
     *   "scenario_type": "WORKPLACE",
     *   "measurement_target": "BEHAVIORAL"
     * }
     * 
     * Used by Context Neutrality Filter in Scenario A (Universal Baseline)
     */
    @Column(name = "metadata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata;

    @Column(name="is_active", nullable = false)
    private boolean isActive;

    @Column(name="order_index", nullable = false)
    @Min(value = 1, message = "Order index must be positive")
    @Max(value = 50, message = "Maximum 50 questions per indicator")
    private int orderIndex;

    // Constructors
    public AssessmentQuestion() {
        // Default constructor required by JPA
    }

    public AssessmentQuestion(UUID id, BehavioralIndicator behavioralIndicator, String questionText, 
                             QuestionType questionType, List<Map<String, Object>> answerOptions, 
                             String scoringRubric, Integer timeLimit, DifficultyLevel difficultyLevel,
                             Map<String, Object> metadata, boolean isActive, int orderIndex) {
        this.id = id;
        this.behavioralIndicator = behavioralIndicator;
        this.questionText = questionText;
        this.questionType = questionType;
        this.answerOptions = answerOptions;
        this.scoringRubric = scoringRubric;
        this.timeLimit = timeLimit;
        this.difficultyLevel = difficultyLevel;
        this.metadata = metadata;
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

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    /**
     * Convenience method to get tags from metadata.
     * Returns empty list if metadata or tags are null.
     */
    @SuppressWarnings("unchecked")
    @Transient
    public List<String> getTags() {
        if (metadata == null || !metadata.containsKey("tags")) {
            return List.of();
        }
        Object tags = metadata.get("tags");
        if (tags instanceof List) {
            return (List<String>) tags;
        }
        return List.of();
    }

    /**
     * Check if question has a specific tag (case-insensitive).
     * Used for Context Neutrality Filter in test assembly.
     */
    @Transient
    public boolean hasTag(String tag) {
        return getTags().stream()
                .anyMatch(t -> t.equalsIgnoreCase(tag));
    }

    /**
     * Check if this is a context-neutral question.
     * Per ROADMAP.md: Questions with GENERAL/UNIVERSAL tags or without narrow tags (IT, SALES, FINANCE)
     */
    @Transient
    public boolean isContextNeutral() {
        List<String> tags = getTags();
        if (tags.isEmpty()) return true;
        if (hasTag("GENERAL") || hasTag("UNIVERSAL")) return true;
        // Check for narrow context tags
        return !hasTag("IT") && !hasTag("SALES") && !hasTag("FINANCE") && !hasTag("HEALTHCARE");
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
