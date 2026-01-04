package app.skillsoft.assessmentbackend.domain.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * TeamMember entity representing the many-to-many relationship between Teams and Users.
 *
 * This join entity includes additional metadata:
 * - role: LEADER or MEMBER
 * - joinedAt: When the user joined the team
 * - leftAt: When the user left (if removed)
 * - isActive: Whether the membership is currently active
 *
 * Design notes:
 * - Users can belong to multiple teams
 * - Each team has at most one LEADER
 * - Members can be deactivated without deletion (preserves history)
 * - Unique constraint on (team_id, user_id) prevents duplicates
 */
@Entity
@Table(name = "team_members",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_team_members_team_user",
        columnNames = {"team_id", "user_id"}
    ),
    indexes = {
        @Index(name = "idx_team_members_team", columnList = "team_id"),
        @Index(name = "idx_team_members_user", columnList = "user_id"),
        @Index(name = "idx_team_members_active", columnList = "team_id, is_active")
    })
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TeamMemberRole role = TeamMemberRole.MEMBER;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    // Constructors

    public TeamMember() {
        // Default constructor required by JPA
    }

    public TeamMember(Team team, User user, TeamMemberRole role) {
        this.team = team;
        this.user = user;
        this.role = role;
        this.joinedAt = LocalDateTime.now();
        this.isActive = true;
    }

    public TeamMember(Team team, User user) {
        this(team, user, TeamMemberRole.MEMBER);
    }

    // Business Methods

    /**
     * Promotes this member to team leader.
     */
    public void promoteToLeader() {
        this.role = TeamMemberRole.LEADER;
    }

    /**
     * Demotes this member to regular member role.
     */
    public void demoteToMember() {
        this.role = TeamMemberRole.MEMBER;
    }

    /**
     * Removes the member from the team (soft delete).
     * Sets isActive to false and records the leave time.
     */
    public void remove() {
        this.isActive = false;
        this.leftAt = LocalDateTime.now();
    }

    /**
     * Reactivates a previously removed member.
     * Clears the leftAt timestamp and sets isActive to true.
     */
    public void reactivate() {
        this.isActive = true;
        this.leftAt = null;
        this.joinedAt = LocalDateTime.now(); // Reset join date on reactivation
    }

    /**
     * Checks if this member is the team leader.
     */
    public boolean isLeader() {
        return role == TeamMemberRole.LEADER;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public TeamMemberRole getRole() {
        return role;
    }

    public void setRole(TeamMemberRole role) {
        this.role = role;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public LocalDateTime getLeftAt() {
        return leftAt;
    }

    public void setLeftAt(LocalDateTime leftAt) {
        this.leftAt = leftAt;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    // Object overrides

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TeamMember that = (TeamMember) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "TeamMember{" +
                "id=" + id +
                ", teamId=" + (team != null ? team.getId() : null) +
                ", userId=" + (user != null ? user.getId() : null) +
                ", role=" + role +
                ", isActive=" + isActive +
                ", joinedAt=" + joinedAt +
                '}';
    }
}
