package app.skillsoft.assessmentbackend.domain.entities;


import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "competencies")
public class Competency {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name="name", nullable = false, unique = true)
    private String name;

    @Column(name ="description", length = 1000)
    private String description;

    @Column(name="category", nullable = false)
    @Enumerated(EnumType.STRING)
    private CompetencyCategory category;

    @Column(name="level", nullable = false)
    @Enumerated(EnumType.STRING)
    private ProficiencyLevel level;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name="standard_codes", columnDefinition = "jsonb")
    /**
     * Triple Standard Mapping for global competency alignment.
     * Per ROADMAP.md Section 1.A "Mapping Strategy":
     * 
     * Schema (JSONB structure):
     * {
     *   // 1. Psychological Standard (Team Fit / Big Five)
     *   "global_category": "BIG_FIVE_CONSCIENTIOUSNESS",
     *   
     *   // 2. Occupational Standard (Job Fit Baseline - O*NET)
     *   "onet_ref": {
     *     "code": "2.B.1.a",
     *     "name": "Social Perceptiveness",
     *     "similarity": 0.95
     *   },
     *   
     *   // 3. Transversal Standard (Interoperability - ESCO)
     *   "esco_ref": {
     *     "uri": "http://data.europa.eu/esco/skill/S1.2",
     *     "label": "Working with others"
     *   }
     * }
     * 
     * Benefits:
     * - One answer feeds three analytical models simultaneously
     * - Enables Competency Passport reuse across scenarios
     * - Normalizes local competencies to global standards
     */
    private Map<String, Object> standardCodes;

    @Column(name="is_active", nullable = false)
    private boolean isActive;

    @Column(name="approval_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ApprovalStatus approvalStatus;

    @OneToMany(mappedBy = "competency", cascade = {CascadeType.REMOVE, CascadeType.PERSIST}, orphanRemoval = true)
    private List<BehavioralIndicator> behavioralIndicators;

    @Column(name="version", nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_modified", nullable = false)
    private LocalDateTime lastModified;

    public Competency() {
        // Default constructor required by JPA
    }

    public Competency(UUID id, String name, String description, CompetencyCategory category, ProficiencyLevel level, Map<String, Object> standardCodes, boolean isActive, ApprovalStatus approvalStatus, List<BehavioralIndicator> behavioralIndicators, int version, LocalDateTime createdAt, LocalDateTime lastModified) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.level = level;
        this.standardCodes = standardCodes;
        this.isActive = isActive;
        this.approvalStatus = approvalStatus;
        this.behavioralIndicators = behavioralIndicators;
        this.version = version;
        this.createdAt = createdAt;
        this.lastModified = lastModified;
    }

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

    public CompetencyCategory getCategory() {
        return category;
    }

    public void setCategory(CompetencyCategory category) {
        this.category = category;
    }

    public ProficiencyLevel getLevel() {
        return level;
    }

    public void setLevel(ProficiencyLevel level) {
        this.level = level;
    }

    public Map<String, Object> getStandardCodes() {
        return standardCodes;
    }

    public void setStandardCodes(Map<String, Object> standardCodes) {
        this.standardCodes = standardCodes;
    }

    /**
     * Get the Big Five psychological category mapping.
     * Per ROADMAP.md: e.g., "BIG_FIVE_CONSCIENTIOUSNESS", "BIG_FIVE_AGREEABLENESS"
     * @return Big Five category string or null if not mapped
     */
    @Transient
    public String getBigFiveCategory() {
        if (standardCodes == null) return null;
        Object category = standardCodes.get("global_category");
        return category != null ? category.toString() : null;
    }

    /**
     * Get the O*NET reference mapping.
     * Per ROADMAP.md: Contains code (e.g., "2.B.1.a"), name, and similarity score
     * @return O*NET reference map or null if not mapped
     */
    @SuppressWarnings("unchecked")
    @Transient
    public Map<String, Object> getOnetRef() {
        if (standardCodes == null) return null;
        Object ref = standardCodes.get("onet_ref");
        return ref instanceof Map ? (Map<String, Object>) ref : null;
    }

    /**
     * Get the O*NET code (e.g., "2.B.1.a")
     * @return O*NET code string or null
     */
    @Transient
    public String getOnetCode() {
        Map<String, Object> onetRef = getOnetRef();
        if (onetRef == null) return null;
        Object code = onetRef.get("code");
        return code != null ? code.toString() : null;
    }

    /**
     * Get the ESCO reference mapping.
     * Per ROADMAP.md: Contains URI and label for transversal skill
     * @return ESCO reference map or null if not mapped
     */
    @SuppressWarnings("unchecked")
    @Transient
    public Map<String, Object> getEscoRef() {
        if (standardCodes == null) return null;
        Object ref = standardCodes.get("esco_ref");
        return ref instanceof Map ? (Map<String, Object>) ref : null;
    }

    /**
     * Get the ESCO URI (e.g., "http://data.europa.eu/esco/skill/S1.2")
     * @return ESCO URI string or null
     */
    @Transient
    public String getEscoUri() {
        Map<String, Object> escoRef = getEscoRef();
        if (escoRef == null) return null;
        Object uri = escoRef.get("uri");
        return uri != null ? uri.toString() : null;
    }

    /**
     * Check if this competency has complete Triple Standard mapping.
     * Per ROADMAP.md: Required for Scenario A (Universal Baseline) to generate Competency Passport
     * @return true if Big Five, O*NET, and ESCO mappings are all present
     */
    @Transient
    public boolean hasTripleStandardMapping() {
        return getBigFiveCategory() != null && getOnetRef() != null && getEscoRef() != null;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public ApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(ApprovalStatus approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public List<BehavioralIndicator> getBehavioralIndicators() {
        return behavioralIndicators;
    }

    public void setBehavioralIndicators(List<BehavioralIndicator> behavioralIndicators) {
        this.behavioralIndicators = behavioralIndicators;
    }

    @Override
    public String toString() {
        return "Competency{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", category=" + category +
                ", level=" + level +
                ", standardCodes=" + standardCodes +
                ", isActive=" + isActive +
                ", approvalStatus=" + approvalStatus +
                ", behavioralIndicators=" + behavioralIndicators +
                ", version=" + version +
                ", createdAt=" + createdAt +
                ", lastModified=" + lastModified +
                '}';
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }
}
