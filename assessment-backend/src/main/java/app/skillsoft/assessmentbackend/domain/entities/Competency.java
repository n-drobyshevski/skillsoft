package app.skillsoft.assessmentbackend.domain.entities;


import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;
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

    public Competency(UUID id, String name, String description, CompetencyCategory category, ProficiencyLevel level, boolean isActive, ApprovalStatus approvalStatus, List<BehavioralIndicator> behavioralIndicators, int version, LocalDateTime createdAt, LocalDateTime lastModified) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.level = level;
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
