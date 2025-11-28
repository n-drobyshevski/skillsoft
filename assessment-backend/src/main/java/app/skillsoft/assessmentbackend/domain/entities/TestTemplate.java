package app.skillsoft.assessmentbackend.domain.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a test template configuration.
 * Templates define the structure and rules for competency assessment tests.
 */
@Entity
@Table(name = "test_templates")
public class TestTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "competency_ids", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<UUID> competencyIds = new ArrayList<>();

    @Column(name = "questions_per_indicator")
    private Integer questionsPerIndicator = 3;

    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes = 60;

    @Column(name = "passing_score")
    private Double passingScore = 70.0;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "shuffle_questions")
    private Boolean shuffleQuestions = true;

    @Column(name = "shuffle_options")
    private Boolean shuffleOptions = true;

    @Column(name = "allow_skip")
    private Boolean allowSkip = true;

    @Column(name = "allow_back_navigation")
    private Boolean allowBackNavigation = true;

    @Column(name = "show_results_immediately")
    private Boolean showResultsImmediately = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public TestTemplate() {
        // Default constructor required by JPA
    }

    public TestTemplate(String name, String description, List<UUID> competencyIds) {
        this.name = name;
        this.description = description;
        this.competencyIds = competencyIds != null ? competencyIds : new ArrayList<>();
    }

    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<UUID> getCompetencyIds() {
        return competencyIds;
    }

    public void setCompetencyIds(List<UUID> competencyIds) {
        this.competencyIds = competencyIds;
    }

    public Integer getQuestionsPerIndicator() {
        return questionsPerIndicator;
    }

    public void setQuestionsPerIndicator(Integer questionsPerIndicator) {
        this.questionsPerIndicator = questionsPerIndicator;
    }

    public Integer getTimeLimitMinutes() {
        return timeLimitMinutes;
    }

    public void setTimeLimitMinutes(Integer timeLimitMinutes) {
        this.timeLimitMinutes = timeLimitMinutes;
    }

    public Double getPassingScore() {
        return passingScore;
    }

    public void setPassingScore(Double passingScore) {
        this.passingScore = passingScore;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getShuffleQuestions() {
        return shuffleQuestions;
    }

    public void setShuffleQuestions(Boolean shuffleQuestions) {
        this.shuffleQuestions = shuffleQuestions;
    }

    public Boolean getShuffleOptions() {
        return shuffleOptions;
    }

    public void setShuffleOptions(Boolean shuffleOptions) {
        this.shuffleOptions = shuffleOptions;
    }

    public Boolean getAllowSkip() {
        return allowSkip;
    }

    public void setAllowSkip(Boolean allowSkip) {
        this.allowSkip = allowSkip;
    }

    public Boolean getAllowBackNavigation() {
        return allowBackNavigation;
    }

    public void setAllowBackNavigation(Boolean allowBackNavigation) {
        this.allowBackNavigation = allowBackNavigation;
    }

    public Boolean getShowResultsImmediately() {
        return showResultsImmediately;
    }

    public void setShowResultsImmediately(Boolean showResultsImmediately) {
        this.showResultsImmediately = showResultsImmediately;
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
        TestTemplate that = (TestTemplate) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "TestTemplate{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", competencyIds=" + competencyIds +
                ", isActive=" + isActive +
                '}';
    }
}
