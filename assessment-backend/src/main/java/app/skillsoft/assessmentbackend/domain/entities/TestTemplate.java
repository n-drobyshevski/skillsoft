package app.skillsoft.assessmentbackend.domain.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a test template configuration.
 * Per ROADMAP.md Section 1.C: Stores the "recipe" for dynamic test generation.
 * 
 * Key fields:
 * - goal: Assessment type (OVERVIEW, JOB_FIT, TEAM_FIT) - determines scoring strategy
 * - blueprint: JSONB configuration for test assembly mechanics
 * 
 * Blueprint schema varies by goal:
 * - OVERVIEW: { strategy, competencies, aggregationTargets, saveAsPassport }
 * - JOB_FIT: { strategy, onetSocCode, useOnetBenchmarks, reusePassportData, requiredTags, excludeTypes }
 * - TEAM_FIT: { strategy, teamId, normalizationStandard, checks, saturationThreshold, limit }
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

    /**
     * Assessment goal type - determines the scoring strategy and test mechanics.
     * Per ROADMAP.md Section 1.2: OVERVIEW, JOB_FIT, or TEAM_FIT
     */
    @Column(name = "goal")
    @Enumerated(EnumType.STRING)
    private AssessmentGoal goal = AssessmentGoal.OVERVIEW;

    /**
     * Blueprint JSONB configuration for test assembly.
     * Per ROADMAP.md Section 1.2 - varies by goal:
     * 
     * OVERVIEW (Scenario A):
     * {
     *   "strategy": "UNIVERSAL_BASELINE",
     *   "competencies": "CROSS_FUNCTIONAL_ONLY",
     *   "aggregationTargets": ["BIG_FIVE", "ONET_CROSS_FUNCTIONAL", "ESCO_TRANSVERSAL"],
     *   "saveAsPassport": true,
     *   "indicatorsPerCompetency": 2,
     *   "questionsPerIndicator": 2
     * }
     * 
     * JOB_FIT (Scenario B):
     * {
     *   "strategy": "TARGETED_FIT",
     *   "onetSocCode": "15-1132.00",
     *   "useOnetBenchmarks": true,
     *   "reusePassportData": true,
     *   "requiredTags": ["IT", "MANAGEMENT"],
     *   "excludeTypes": ["LIKERT"],
     *   "targetProfile": { "Critical Thinking": 4, "Leadership": 3 }
     * }
     * 
     * TEAM_FIT (Scenario C):
     * {
     *   "strategy": "DYNAMIC_GAP_ANALYSIS",
     *   "teamId": "uuid-alpha-team",
     *   "normalizationStandard": "ESCO_V1",
     *   "checks": ["COMPLEMENTARY_SKILLS", "ROLE_SATURATION"],
     *   "saturationThreshold": 0.75,
     *   "limit": 20
     * }
     */
    @Column(name = "blueprint", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> blueprint;

    /**
     * @deprecated Use blueprint field instead. Kept for backward compatibility.
     * Will be removed in future versions.
     */
    @Deprecated
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

    /**
     * Full constructor with goal and blueprint.
     * Per ROADMAP.md Section 1.C
     */
    public TestTemplate(String name, String description, AssessmentGoal goal, Map<String, Object> blueprint) {
        this.name = name;
        this.description = description;
        this.goal = goal != null ? goal : AssessmentGoal.OVERVIEW;
        this.blueprint = blueprint;
    }

    /**
     * @deprecated Use constructor with goal and blueprint instead.
     */
    @Deprecated
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

    public AssessmentGoal getGoal() {
        return goal;
    }

    public void setGoal(AssessmentGoal goal) {
        this.goal = goal;
    }

    public Map<String, Object> getBlueprint() {
        return blueprint;
    }

    public void setBlueprint(Map<String, Object> blueprint) {
        this.blueprint = blueprint;
    }

    // ============================================
    // BLUEPRINT HELPER METHODS
    // Per ROADMAP.md Section 1.2
    // ============================================

    /**
     * Get the strategy from blueprint.
     * @return Strategy string (e.g., "UNIVERSAL_BASELINE", "TARGETED_FIT", "DYNAMIC_GAP_ANALYSIS")
     */
    @Transient
    public String getStrategy() {
        if (blueprint == null) return null;
        Object strategy = blueprint.get("strategy");
        return strategy != null ? strategy.toString() : null;
    }

    /**
     * Get the O*NET SOC code for JOB_FIT scenarios.
     * @return SOC code (e.g., "15-1132.00") or null
     */
    @Transient
    public String getOnetSocCode() {
        if (blueprint == null) return null;
        Object code = blueprint.get("onetSocCode");
        return code != null ? code.toString() : null;
    }

    /**
     * Get the team ID for TEAM_FIT scenarios.
     * @return Team UUID or null
     */
    @Transient
    public String getTeamId() {
        if (blueprint == null) return null;
        Object teamId = blueprint.get("teamId");
        return teamId != null ? teamId.toString() : null;
    }

    /**
     * Check if this template should save results as Competency Passport.
     * Used in OVERVIEW scenario.
     * @return true if results should be saved as reusable passport
     */
    @Transient
    public boolean shouldSaveAsPassport() {
        if (blueprint == null) return false;
        Object save = blueprint.get("saveAsPassport");
        return Boolean.TRUE.equals(save);
    }

    /**
     * Check if this template should reuse existing Passport data.
     * Used in JOB_FIT scenario for Delta Testing.
     * @return true if existing passport data should be reused
     */
    @Transient
    public boolean shouldReusePassportData() {
        if (blueprint == null) return false;
        Object reuse = blueprint.get("reusePassportData");
        return Boolean.TRUE.equals(reuse);
    }

    /**
     * @deprecated Use blueprint field instead.
     */
    @Deprecated
    public List<UUID> getCompetencyIds() {
        return competencyIds;
    }

    /**
     * @deprecated Use blueprint field instead.
     */
    @Deprecated
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
                ", goal=" + goal +
                ", blueprint=" + blueprint +
                ", isActive=" + isActive +
                '}';
    }
}
