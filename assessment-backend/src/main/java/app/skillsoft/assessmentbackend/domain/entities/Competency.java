package app.skillsoft.assessmentbackend.domain.entities;


import app.skillsoft.assessmentbackend.domain.dto.StandardCodesDto;
import jakarta.persistence.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Competency entity representing a skill or ability that can be assessed.
 * 
 * The standardCodes field uses JSONB storage for flexible taxonomy mappings.
 * Uses @JdbcTypeCode(SqlTypes.JSON) with HibernateJsonConfig for proper snake_case serialization.
 */
@Entity
@Table(name = "competencies")
@DynamicUpdate
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

    /**
     * Triple Standard Mapping for global competency alignment.
     * Per ROADMAP.md Section 1.A "Mapping Strategy":
     * 
     * Schema (JSONB structure):
     * {
     *   // 1. Psychological Standard (Team Fit / Big Five)
     *   "global_category": { "big_five": "CONSCIENTIOUSNESS" },
     *   
     *   // 2. Occupational Standard (Job Fit Baseline - O*NET)
     *   "onet_ref": {
     *     "code": "2.B.1.a",
     *     "name": "Social Perceptiveness"
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
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name="standard_codes", columnDefinition = "jsonb")
    private StandardCodesDto standardCodes;

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

    public Competency(UUID id, String name, String description, CompetencyCategory category, StandardCodesDto standardCodes, boolean isActive, ApprovalStatus approvalStatus, List<BehavioralIndicator> behavioralIndicators, int version, LocalDateTime createdAt, LocalDateTime lastModified) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
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

    public StandardCodesDto getStandardCodes() {
        return standardCodes;
    }

    public void setStandardCodes(StandardCodesDto standardCodes) {
        this.standardCodes = standardCodes;
    }

    /**
     * Get the Big Five psychological category mapping.
     * Per ROADMAP.md: e.g., "BIG_FIVE_CONSCIENTIOUSNESS", "BIG_FIVE_AGREEABLENESS"
     * @return Big Five category string or null if not mapped
     */
    @Transient
    public String getBigFiveCategory() {
        if (standardCodes == null || standardCodes.bigFiveRef() == null) return null;
        StandardCodesDto.BigFiveRefDto bigFiveRef = standardCodes.bigFiveRef();
        if (bigFiveRef.trait() != null) {
            return "BIG_FIVE_" + bigFiveRef.trait().toUpperCase();
        }
        return null;
    }

    /**
     * Get the O*NET reference mapping.
     * Per ROADMAP.md: Contains code (e.g., "2.B.1.a"), title, and element type
     * @return O*NET reference DTO or null if not mapped
     */
    @Transient
    public StandardCodesDto.OnetRefDto getOnetRef() {
        if (standardCodes == null) return null;
        return standardCodes.onetRef();
    }

    /**
     * Get the O*NET code (e.g., "2.B.1.a")
     * @return O*NET code string or null
     */
    @Transient
    public String getOnetCode() {
        StandardCodesDto.OnetRefDto onetRef = getOnetRef();
        return onetRef != null ? onetRef.code() : null;
    }

    /**
     * Get the ESCO reference mapping.
     * Per ROADMAP.md: Contains URI and label for transversal skill
     * @return ESCO reference DTO or null if not mapped
     */
    @Transient
    public StandardCodesDto.EscoRefDto getEscoRef() {
        if (standardCodes == null) return null;
        return standardCodes.escoRef();
    }

    /**
     * Get the ESCO URI (e.g., "http://data.europa.eu/esco/skill/abc123")
     * @return ESCO URI string or null
     */
    @Transient
    public String getEscoUri() {
        StandardCodesDto.EscoRefDto escoRef = getEscoRef();
        return escoRef != null ? escoRef.uri() : null;
    }

    /**
     * Check if this competency has complete Triple Standard mapping.
     * Per ROADMAP.md: Required for Scenario A (Universal Baseline) to generate Competency Passport
     * @return true if Big Five, O*NET, and ESCO mappings are all present
     */
    @Transient
    public boolean hasTripleStandardMapping() {
        return standardCodes != null 
            && standardCodes.hasBigFiveMapping() 
            && standardCodes.hasOnetMapping() 
            && standardCodes.hasEscoMapping();
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
