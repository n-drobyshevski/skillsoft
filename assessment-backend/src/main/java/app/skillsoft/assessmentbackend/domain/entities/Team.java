package app.skillsoft.assessmentbackend.domain.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Team entity representing a group of users for TEAM_FIT assessments.
 *
 * Teams group existing users to calculate collective competency profiles,
 * identify skill gaps, and enable team-based candidate fit scoring.
 *
 * Lifecycle:
 * - DRAFT: Team is being configured, members can be added/removed
 * - ACTIVE: Team is operational, can be used for TEAM_FIT tests
 * - ARCHIVED: Team is decommissioned, preserved for history
 *
 * @see TeamMember for many-to-many relationship with User
 * @see TeamStatus for lifecycle states
 */
@Entity
@Table(name = "teams", indexes = {
    @Index(name = "idx_teams_status", columnList = "status"),
    @Index(name = "idx_teams_leader", columnList = "leader_id"),
    @Index(name = "idx_teams_created_by", columnList = "created_by_id")
})
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    @NotBlank(message = "Team name is required")
    @Size(max = 200, message = "Team name must not exceed 200 characters")
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TeamStatus status = TeamStatus.DRAFT;

    /**
     * Team leader - must be an active member of the team.
     * Only one leader per team is allowed.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leader_id")
    private User leader;

    /**
     * User who created this team (for audit purposes).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    /**
     * Team members with their roles.
     * Uses orphanRemoval to clean up when team is deleted.
     */
    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TeamMember> members = new HashSet<>();

    /**
     * Additional team metadata stored as JSON.
     * Can include department, project code, tags, etc.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb default '{}'::jsonb")
    private String metadata = "{}";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * When the team was activated (status changed to ACTIVE).
     */
    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    /**
     * When the team was archived (status changed to ARCHIVED).
     */
    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    // Constructors

    public Team() {
        // Default constructor required by JPA
    }

    public Team(String name, String description, User createdBy) {
        this.name = name;
        this.description = description;
        this.createdBy = createdBy;
        this.status = TeamStatus.DRAFT;
    }

    // Business Methods

    /**
     * Activates the team, transitioning from DRAFT to ACTIVE.
     * Requires at least one active member.
     *
     * @throws IllegalStateException if team is not in DRAFT status
     * @throws IllegalStateException if team has no active members
     */
    public void activate() {
        if (this.status != TeamStatus.DRAFT) {
            throw new IllegalStateException("Can only activate a DRAFT team, current status: " + status);
        }
        long activeMemberCount = members.stream().filter(TeamMember::isActive).count();
        if (activeMemberCount == 0) {
            throw new IllegalStateException("Team must have at least one active member to activate");
        }
        this.status = TeamStatus.ACTIVE;
        this.activatedAt = LocalDateTime.now();
    }

    /**
     * Archives the team, making it inactive.
     * Archived teams cannot be reactivated.
     *
     * @throws IllegalStateException if team is already archived
     */
    public void archive() {
        if (this.status == TeamStatus.ARCHIVED) {
            throw new IllegalStateException("Team is already archived");
        }
        this.status = TeamStatus.ARCHIVED;
        this.archivedAt = LocalDateTime.now();
    }

    /**
     * Sets the team leader.
     * Leader must be an active member of the team.
     *
     * @param newLeader The user to set as leader, or null to remove leader
     * @throws IllegalArgumentException if the user is not an active team member
     */
    public void setLeader(User newLeader) {
        if (newLeader != null) {
            boolean isActiveMember = members.stream()
                .anyMatch(m -> m.getUser().getId().equals(newLeader.getId()) && m.isActive());
            if (!isActiveMember) {
                throw new IllegalArgumentException("Leader must be an active team member");
            }
        }
        this.leader = newLeader;
    }

    /**
     * Adds a member to the team.
     *
     * @param member The TeamMember to add
     */
    public void addMember(TeamMember member) {
        members.add(member);
        member.setTeam(this);
    }

    /**
     * Gets the count of active members in the team.
     *
     * @return Number of active members
     */
    public int getActiveMemberCount() {
        return (int) members.stream().filter(TeamMember::isActive).count();
    }

    /**
     * Checks if the team is in ACTIVE status.
     */
    public boolean isActive() {
        return status == TeamStatus.ACTIVE;
    }

    /**
     * Checks if the team is in DRAFT status.
     */
    public boolean isDraft() {
        return status == TeamStatus.DRAFT;
    }

    /**
     * Checks if the team is in ARCHIVED status.
     */
    public boolean isArchived() {
        return status == TeamStatus.ARCHIVED;
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

    public TeamStatus getStatus() {
        return status;
    }

    public User getLeader() {
        return leader;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public Set<TeamMember> getMembers() {
        return members;
    }

    public void setMembers(Set<TeamMember> members) {
        this.members = members;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getActivatedAt() {
        return activatedAt;
    }

    public LocalDateTime getArchivedAt() {
        return archivedAt;
    }

    // Object overrides

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Team team = (Team) o;
        return Objects.equals(id, team.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Team{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", status=" + status +
                ", memberCount=" + getActiveMemberCount() +
                ", createdAt=" + createdAt +
                '}';
    }
}
