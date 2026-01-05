package app.skillsoft.assessmentbackend.domain.entities;

import jakarta.persistence.*;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a shareable link for a TestTemplate.
 *
 * Share links allow access to templates via a unique token URL.
 * Features:
 * - Cryptographically secure token generation
 * - Required expiration date (max 365 days)
 * - Optional usage limit (max_uses)
 * - Usage tracking (current_uses, last_used_at)
 * - Supports anonymous access (no authentication required)
 * - Maximum 10 active links per template
 */
@Entity
@Table(name = "template_share_links",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_share_link_token", columnNames = {"token"})
    },
    indexes = {
        @Index(name = "idx_share_links_template", columnList = "template_id"),
        @Index(name = "idx_share_links_active", columnList = "template_id, is_active"),
        @Index(name = "idx_share_links_token", columnList = "token")
    })
public class TemplateShareLink {

    /**
     * Maximum number of active links allowed per template.
     */
    public static final int MAX_LINKS_PER_TEMPLATE = 10;

    /**
     * Maximum expiry in days (1 year).
     */
    public static final int MAX_EXPIRY_DAYS = 365;

    /**
     * Default expiry in days (1 week).
     */
    public static final int DEFAULT_EXPIRY_DAYS = 7;

    /**
     * Token length in bytes (before Base64 encoding).
     * 48 bytes = 384 bits of entropy, results in 64 character token.
     */
    private static final int TOKEN_BYTES = 48;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The template this link provides access to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private TestTemplate template;

    /**
     * Cryptographically secure random token (64 characters).
     * Generated using SecureRandom + Base64URL encoding.
     */
    @Column(name = "token", nullable = false, length = 64, unique = true)
    private String token;

    /**
     * User who created this link.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    /**
     * When this link was created.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Expiration timestamp (required for security).
     * Links cannot be created without an expiration date.
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Maximum number of times this link can be used.
     * Null means unlimited uses.
     */
    @Column(name = "max_uses")
    private Integer maxUses;

    /**
     * Current number of times this link has been used.
     */
    @Column(name = "current_uses", nullable = false)
    private Integer currentUses = 0;

    /**
     * Whether this link is currently active.
     * Set to false when revoked.
     */
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    /**
     * Permission level granted by this link.
     * Usually VIEW for share links.
     */
    @Column(name = "permission", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private SharePermission permission = SharePermission.VIEW;

    /**
     * Optional human-readable label for this link.
     * E.g., "HR Department Link", "Interview Candidates".
     */
    @Column(name = "label", length = 200)
    private String label;

    /**
     * When this link was last used.
     */
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    /**
     * When this link was revoked (soft delete).
     */
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    // Constructors

    public TemplateShareLink() {
        // Default constructor required by JPA
    }

    /**
     * Create a new share link with default settings.
     *
     * @param template The template to share
     * @param createdBy The user creating the link
     */
    public TemplateShareLink(TestTemplate template, User createdBy) {
        this.template = template;
        this.createdBy = createdBy;
        this.token = generateToken();
        this.createdAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusDays(DEFAULT_EXPIRY_DAYS);
        this.permission = SharePermission.VIEW;
    }

    /**
     * Create a new share link with custom settings.
     *
     * @param template The template to share
     * @param createdBy The user creating the link
     * @param expiresInDays Days until expiration (max 365)
     * @param maxUses Maximum uses (null for unlimited)
     * @param permission Permission level
     * @param label Optional label
     */
    public TemplateShareLink(TestTemplate template, User createdBy, int expiresInDays,
                             Integer maxUses, SharePermission permission, String label) {
        this.template = template;
        this.createdBy = createdBy;
        this.token = generateToken();
        this.createdAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusDays(Math.min(expiresInDays, MAX_EXPIRY_DAYS));
        this.maxUses = maxUses;
        this.permission = permission != null ? permission : SharePermission.VIEW;
        this.label = label;
    }

    // Business methods

    /**
     * Generate a cryptographically secure token.
     * Uses SecureRandom for 48 bytes (384 bits) of entropy.
     *
     * @return URL-safe Base64 encoded token (64 characters)
     */
    public static String generateToken() {
        byte[] randomBytes = new byte[TOKEN_BYTES];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Check if this link has expired.
     * @return true if the current time is after expiresAt
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Check if this link has reached its maximum usage count.
     * @return true if maxUses is set and currentUses >= maxUses
     */
    public boolean hasReachedMaxUses() {
        return maxUses != null && currentUses >= maxUses;
    }

    /**
     * Check if this link is currently valid and can grant access.
     * @return true if active, not expired, and not used up
     */
    public boolean isValid() {
        return isActive && !isExpired() && !hasReachedMaxUses() && revokedAt == null;
    }

    /**
     * Record a usage of this link.
     * Increments currentUses and updates lastUsedAt.
     */
    public void recordUsage() {
        this.currentUses++;
        this.lastUsedAt = LocalDateTime.now();
    }

    /**
     * Revoke this link (soft delete).
     */
    public void revoke() {
        this.isActive = false;
        this.revokedAt = LocalDateTime.now();
    }

    /**
     * Get the remaining uses for this link.
     * @return Remaining uses, or null if unlimited
     */
    @Transient
    public Integer getRemainingUses() {
        if (maxUses == null) {
            return null;
        }
        return Math.max(0, maxUses - currentUses);
    }

    /**
     * Check if this link's permission is sufficient for the required level.
     * @param required The required permission
     * @return true if this link meets the requirement
     */
    public boolean hasPermission(SharePermission required) {
        return permission != null && permission.includes(required);
    }

    /**
     * Get the full URL for this share link.
     * @param baseUrl The application base URL
     * @return Complete shareable URL
     */
    public String getShareUrl(String baseUrl) {
        return baseUrl + "/test?token=" + token;
    }

    /**
     * Get the validation status reason if invalid.
     * @return Status reason or null if valid
     */
    @Transient
    public String getInvalidReason() {
        if (!isActive) return "REVOKED";
        if (revokedAt != null) return "REVOKED";
        if (isExpired()) return "EXPIRED";
        if (hasReachedMaxUses()) return "MAX_USES_REACHED";
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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Integer getMaxUses() {
        return maxUses;
    }

    public void setMaxUses(Integer maxUses) {
        this.maxUses = maxUses;
    }

    public Integer getCurrentUses() {
        return currentUses;
    }

    public void setCurrentUses(Integer currentUses) {
        this.currentUses = currentUses;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public SharePermission getPermission() {
        return permission;
    }

    public void setPermission(SharePermission permission) {
        this.permission = permission;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    // Object overrides

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TemplateShareLink that = (TemplateShareLink) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "TemplateShareLink{" +
                "id=" + id +
                ", templateId=" + (template != null ? template.getId() : null) +
                ", token='" + (token != null ? token.substring(0, 8) + "..." : null) + '\'' +
                ", permission=" + permission +
                ", label='" + label + '\'' +
                ", expiresAt=" + expiresAt +
                ", maxUses=" + maxUses +
                ", currentUses=" + currentUses +
                ", isActive=" + isActive +
                '}';
    }
}
