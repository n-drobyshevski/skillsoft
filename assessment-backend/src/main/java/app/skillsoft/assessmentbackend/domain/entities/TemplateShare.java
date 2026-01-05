package app.skillsoft.assessmentbackend.domain.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a share grant for a TestTemplate.
 *
 * A TemplateShare grants access to a template for either:
 * - A specific user (granteeType = USER, user_id is set)
 * - An entire team (granteeType = TEAM, team_id is set)
 *
 * Features:
 * - Permission levels: VIEW, EDIT, MANAGE
 * - Optional expiration date
 * - Soft delete via revoked_at timestamp
 * - Audit trail (granted_by, granted_at)
 */
@Entity
@Table(name = "template_shares",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_template_share_user",
            columnNames = {"template_id", "user_id"}
        ),
        @UniqueConstraint(
            name = "uk_template_share_team",
            columnNames = {"template_id", "team_id"}
        )
    },
    indexes = {
        @Index(name = "idx_template_shares_template", columnList = "template_id"),
        @Index(name = "idx_template_shares_user", columnList = "user_id"),
        @Index(name = "idx_template_shares_team", columnList = "team_id"),
        @Index(name = "idx_template_shares_active", columnList = "template_id, is_active")
    })
public class TemplateShare {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The template being shared.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private TestTemplate template;

    /**
     * Type of grantee: USER or TEAM.
     * Determines which of user_id or team_id is populated.
     */
    @Column(name = "grantee_type", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private GranteeType granteeType;

    /**
     * The user being granted access.
     * Only set when granteeType = USER.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * The team being granted access.
     * Only set when granteeType = TEAM.
     * All active members of the team inherit access.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    /**
     * Permission level granted: VIEW, EDIT, or MANAGE.
     */
    @Column(name = "permission", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private SharePermission permission = SharePermission.VIEW;

    /**
     * User who granted this share.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_by_id", nullable = false)
    private User grantedBy;

    /**
     * When this share was granted.
     */
    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt = LocalDateTime.now();

    /**
     * Optional expiration date for the share.
     * Null means no expiration.
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * When this share was revoked (soft delete).
     * Null if not revoked.
     */
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    /**
     * Whether this share is currently active.
     * Set to false when revoked.
     */
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    /**
     * Optimistic locking version.
     */
    @Version
    private Long version;

    // Constructors

    public TemplateShare() {
        // Default constructor required by JPA
    }

    /**
     * Create a user share.
     */
    public TemplateShare(TestTemplate template, User user, SharePermission permission, User grantedBy) {
        this.template = template;
        this.user = user;
        this.granteeType = GranteeType.USER;
        this.permission = permission;
        this.grantedBy = grantedBy;
        this.grantedAt = LocalDateTime.now();
    }

    /**
     * Create a team share.
     */
    public TemplateShare(TestTemplate template, Team team, SharePermission permission, User grantedBy) {
        this.template = template;
        this.team = team;
        this.granteeType = GranteeType.TEAM;
        this.permission = permission;
        this.grantedBy = grantedBy;
        this.grantedAt = LocalDateTime.now();
    }

    // Business methods

    /**
     * Check if this share has expired.
     * @return true if expiresAt is set and has passed
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Check if this share is currently valid (active and not expired).
     * @return true if the share grants access
     */
    public boolean isValid() {
        return isActive && !isExpired() && revokedAt == null;
    }

    /**
     * Revoke this share (soft delete).
     */
    public void revoke() {
        this.isActive = false;
        this.revokedAt = LocalDateTime.now();
    }

    /**
     * Check if this share's permission is sufficient for the required level.
     * @param required The required permission
     * @return true if this share meets the requirement
     */
    public boolean hasPermission(SharePermission required) {
        return permission != null && permission.includes(required);
    }

    /**
     * Get the name of the grantee (user or team).
     * @return Display name for the grantee
     */
    @Transient
    public String getGranteeName() {
        if (granteeType == GranteeType.USER && user != null) {
            return user.getFullName();
        } else if (granteeType == GranteeType.TEAM && team != null) {
            return team.getName();
        }
        return "Unknown";
    }

    /**
     * Get the grantee ID (user ID or team ID).
     * @return The UUID of the grantee
     */
    @Transient
    public UUID getGranteeId() {
        if (granteeType == GranteeType.USER && user != null) {
            return user.getId();
        } else if (granteeType == GranteeType.TEAM && team != null) {
            return team.getId();
        }
        return null;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public TestTemplate getTemplate() {
        return template;
    }

    public void setTemplate(TestTemplate template) {
        this.template = template;
    }

    public GranteeType getGranteeType() {
        return granteeType;
    }

    public void setGranteeType(GranteeType granteeType) {
        this.granteeType = granteeType;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public SharePermission getPermission() {
        return permission;
    }

    public void setPermission(SharePermission permission) {
        this.permission = permission;
    }

    public User getGrantedBy() {
        return grantedBy;
    }

    public void setGrantedBy(User grantedBy) {
        this.grantedBy = grantedBy;
    }

    public LocalDateTime getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(LocalDateTime grantedAt) {
        this.grantedAt = grantedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    // Object overrides

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TemplateShare that = (TemplateShare) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "TemplateShare{" +
                "id=" + id +
                ", templateId=" + (template != null ? template.getId() : null) +
                ", granteeType=" + granteeType +
                ", userId=" + (user != null ? user.getId() : null) +
                ", teamId=" + (team != null ? team.getId() : null) +
                ", permission=" + permission +
                ", grantedAt=" + grantedAt +
                ", expiresAt=" + expiresAt +
                ", isActive=" + isActive +
                '}';
    }
}
