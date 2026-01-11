package app.skillsoft.assessmentbackend.domain.entities;

import app.skillsoft.assessmentbackend.domain.dto.blueprint.TestBlueprintDto;
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
 * Entity representing a test template configuration.
 * Per ROADMAP.md Section 1.C: Stores the "recipe" for dynamic test generation.
 * 
 * Implements Immutable Versioning pattern:
 * - version: Sequential version number starting from 1
 * - parentId: Reference to previous version (null for original templates)
 * - status: Lifecycle state (DRAFT, PUBLISHED, ARCHIVED)
 * 
 * Key fields:
 * - goal: Assessment type (OVERVIEW, JOB_FIT, TEAM_FIT) - determines scoring strategy
 * - typedBlueprint: Polymorphic JSONB configuration (TestBlueprintDto hierarchy)
 * 
 * Blueprint schema varies by goal (polymorphic DTOs):
 * - OverviewBlueprint (UNIVERSAL_BASELINE): Competency Passport generation
 * - JobFitBlueprint (TARGETED_FIT): O*NET occupation matching
 * - TeamFitBlueprint (DYNAMIC_GAP_ANALYSIS): Team skill gap analysis
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

    // ============================================
    // VERSIONING FIELDS
    // Immutable Versioning Pattern
    // ============================================

    /**
     * Version number for this template.
     * Starts at 1 for new templates, increments with each new version.
     */
    @Column(name = "version", nullable = false)
    private Integer version = 1;

    /**
     * Reference to the parent template (previous version).
     * Null for original templates (version 1).
     */
    @Column(name = "parent_id")
    private UUID parentId;

    /**
     * Lifecycle status of this template version.
     * DRAFT: Can be modified
     * PUBLISHED: Immutable, available for test sessions
     * ARCHIVED: Immutable, no longer available for new sessions
     */
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private TemplateStatus status = TemplateStatus.DRAFT;

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
     * 
     * @deprecated Use typedBlueprint field instead for type-safe polymorphic access.
     */
    @Deprecated
    @Column(name = "blueprint", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> blueprint;

    /**
     * Type-safe polymorphic blueprint configuration.
     * Uses Jackson polymorphism to deserialize into the correct subclass:
     * - OverviewBlueprint for OVERVIEW strategy
     * - JobFitBlueprint for JOB_FIT strategy
     * - TeamFitBlueprint for TEAM_FIT strategy
     *
     * Stored in the same JSONB column as legacy blueprint field.
     * When both are set, typedBlueprint takes precedence.
     */
    @Type(JsonType.class)
    @Column(name = "typed_blueprint", columnDefinition = "jsonb")
    private TestBlueprintDto typedBlueprint;

    /**
     * @deprecated Use typedBlueprint field instead. Kept for backward compatibility.
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

    // ============================================
    // VISIBILITY & OWNERSHIP FIELDS
    // Per visibility system implementation
    // ============================================

    /**
     * Owner of this template (creator by default).
     * Required for visibility and sharing features.
     * The owner always has full access to the template.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    /**
     * Visibility mode for this template.
     * Controls who can access the template:
     * - PUBLIC: All authenticated users can access
     * - PRIVATE: Only owner and explicitly shared users/teams (default)
     * - LINK: Anyone with a valid share link (supports anonymous access)
     */
    @Column(name = "visibility", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TemplateVisibility visibility = TemplateVisibility.PRIVATE;

    /**
     * Timestamp when visibility was last changed.
     * Used for audit trails and link invalidation.
     */
    @Column(name = "visibility_changed_at")
    private LocalDateTime visibilityChangedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ============================================
    // SOFT DELETE FIELDS
    // ============================================

    /**
     * Soft delete timestamp.
     * When set, the template is considered deleted but data is preserved.
     * Null means the template is active (not deleted).
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * User who deleted this template.
     * Tracks who performed the soft delete for audit purposes.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by_id")
    private User deletedBy;

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

    // ============================================
    // VERSIONING GETTERS/SETTERS
    // ============================================

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public UUID getParentId() {
        return parentId;
    }

    public void setParentId(UUID parentId) {
        this.parentId = parentId;
    }

    public TemplateStatus getStatus() {
        return status;
    }

    public void setStatus(TemplateStatus status) {
        this.status = status;
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

    /**
     * Get the type-safe polymorphic blueprint configuration.
     * @return TestBlueprintDto subclass or null
     */
    public TestBlueprintDto getTypedBlueprint() {
        return typedBlueprint;
    }

    /**
     * Set the type-safe polymorphic blueprint configuration.
     * Also syncs the goal field based on the blueprint strategy.
     */
    public void setTypedBlueprint(TestBlueprintDto typedBlueprint) {
        this.typedBlueprint = typedBlueprint;
        if (typedBlueprint != null && typedBlueprint.getStrategy() != null) {
            this.goal = typedBlueprint.getStrategy();
        }
    }

    // ============================================
    // VERSIONING METHODS
    // Immutable Versioning Pattern
    // ============================================

    /**
     * Create a new version of this template.
     * 
     * This method does NOT modify the current entity. It returns a new
     * transient (not persisted) TestTemplate that:
     * - References this template as its parent (parentId = this.id)
     * - Has an incremented version number
     * - Starts in DRAFT status
     * - Contains a deep copy of the blueprint configuration
     * 
     * @return A new transient TestTemplate instance representing the next version
     * @throws IllegalStateException if the current template has not been persisted (no ID)
     */
    public TestTemplate createNextVersion() {
        if (this.id == null) {
            throw new IllegalStateException("Cannot create next version: current template has no ID");
        }

        TestTemplate nextVersion = new TestTemplate();
        
        // Set versioning fields
        nextVersion.setParentId(this.id);
        nextVersion.setVersion(this.version + 1);
        nextVersion.setStatus(TemplateStatus.DRAFT);
        
        // Copy basic fields
        nextVersion.setName(this.name);
        nextVersion.setDescription(this.description);
        nextVersion.setGoal(this.goal);
        
        // Deep copy typed blueprint if present
        if (this.typedBlueprint != null) {
            nextVersion.setTypedBlueprint(this.typedBlueprint.deepCopy());
        }
        
        // Copy legacy blueprint (shallow copy - Map contents)
        if (this.blueprint != null) {
            nextVersion.setBlueprint(new java.util.HashMap<>(this.blueprint));
        }
        
        // Copy legacy competencyIds
        if (this.competencyIds != null) {
            nextVersion.setCompetencyIds(new ArrayList<>(this.competencyIds));
        }
        
        // Copy test configuration settings
        nextVersion.setQuestionsPerIndicator(this.questionsPerIndicator);
        nextVersion.setTimeLimitMinutes(this.timeLimitMinutes);
        nextVersion.setPassingScore(this.passingScore);
        nextVersion.setShuffleQuestions(this.shuffleQuestions);
        nextVersion.setShuffleOptions(this.shuffleOptions);
        nextVersion.setAllowSkip(this.allowSkip);
        nextVersion.setAllowBackNavigation(this.allowBackNavigation);
        nextVersion.setShowResultsImmediately(this.showResultsImmediately);
        
        // New version starts inactive (DRAFT status controls this now)
        nextVersion.setIsActive(false);

        // Copy ownership and set default visibility for new version
        nextVersion.setOwner(this.owner);
        nextVersion.setVisibility(TemplateVisibility.PRIVATE); // New versions start as PRIVATE

        return nextVersion;
    }

    /**
     * Check if this template can be modified.
     * Only DRAFT templates can be modified.
     * 
     * @return true if the template is in DRAFT status
     */
    @Transient
    public boolean isEditable() {
        return status == null || status.isEditable();
    }

    /**
     * Publish this template, making it available for test sessions.
     * After publishing, the template becomes immutable.
     * 
     * @throws IllegalStateException if the template is not in DRAFT status
     */
    public void publish() {
        if (status != TemplateStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT templates can be published. Current status: " + status);
        }
        this.status = TemplateStatus.PUBLISHED;
        this.isActive = true;
    }

    /**
     * Archive this template, making it unavailable for new test sessions.
     * Archived templates are preserved for historical reference.
     * Visibility is automatically set to PRIVATE when archiving.
     *
     * @throws IllegalStateException if the template is not PUBLISHED
     */
    public void archive() {
        if (status != TemplateStatus.PUBLISHED) {
            throw new IllegalStateException("Only PUBLISHED templates can be archived. Current status: " + status);
        }
        this.status = TemplateStatus.ARCHIVED;
        this.isActive = false;
        // Force PRIVATE visibility when archiving
        if (this.visibility != TemplateVisibility.PRIVATE) {
            this.visibility = TemplateVisibility.PRIVATE;
            this.visibilityChangedAt = LocalDateTime.now();
        }
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

    // ============================================
    // VISIBILITY & OWNERSHIP GETTERS/SETTERS
    // ============================================

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public TemplateVisibility getVisibility() {
        return visibility;
    }

    /**
     * Set the visibility mode for this template.
     * Automatically updates the visibilityChangedAt timestamp.
     *
     * @param visibility The new visibility mode
     */
    public void setVisibility(TemplateVisibility visibility) {
        if (this.visibility != visibility) {
            this.visibility = visibility;
            this.visibilityChangedAt = LocalDateTime.now();
        }
    }

    public LocalDateTime getVisibilityChangedAt() {
        return visibilityChangedAt;
    }

    public void setVisibilityChangedAt(LocalDateTime visibilityChangedAt) {
        this.visibilityChangedAt = visibilityChangedAt;
    }

    /**
     * Check if the template is publicly accessible.
     * @return true if visibility is PUBLIC
     */
    @Transient
    public boolean isPubliclyAccessible() {
        return visibility == TemplateVisibility.PUBLIC;
    }

    /**
     * Check if the template allows anonymous access via links.
     * @return true if visibility is LINK
     */
    @Transient
    public boolean allowsLinkAccess() {
        return visibility == TemplateVisibility.LINK;
    }

    /**
     * Check if the given user is the owner of this template.
     * @param user The user to check
     * @return true if the user is the owner
     */
    public boolean isOwnedBy(User user) {
        if (user == null || owner == null) {
            return false;
        }
        return owner.getId().equals(user.getId());
    }

    /**
     * Check if the given user (by clerkId) is the owner of this template.
     * @param clerkId The clerkId to check
     * @return true if the user is the owner
     */
    public boolean isOwnedByClerkId(String clerkId) {
        if (clerkId == null || owner == null) {
            return false;
        }
        return clerkId.equals(owner.getClerkId());
    }

    // ============================================
    // SOFT DELETE GETTERS/SETTERS AND METHODS
    // ============================================

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public User getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(User deletedBy) {
        this.deletedBy = deletedBy;
    }

    /**
     * Check if this template has been soft-deleted.
     * @return true if deletedAt is set
     */
    @Transient
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Soft delete this template.
     * Sets the deletedAt timestamp and marks the template as inactive.
     *
     * @param deletedBy The user who performed the deletion (can be null)
     */
    public void softDelete(User deletedBy) {
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
        this.isActive = false;
    }

    /**
     * Restore a soft-deleted template.
     * Clears the deletedAt timestamp but does NOT automatically reactivate the template.
     * The caller should decide on the appropriate status.
     */
    public void restore() {
        this.deletedAt = null;
        this.deletedBy = null;
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
                ", version=" + version +
                ", parentId=" + parentId +
                ", status=" + status +
                ", goal=" + goal +
                ", typedBlueprint=" + typedBlueprint +
                ", blueprint=" + blueprint +
                ", isActive=" + isActive +
                ", visibility=" + visibility +
                ", ownerId=" + (owner != null ? owner.getId() : null) +
                '}';
    }
}
