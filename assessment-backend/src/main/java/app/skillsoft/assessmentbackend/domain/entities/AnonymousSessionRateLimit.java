package app.skillsoft.assessmentbackend.domain.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity for rate limiting anonymous session creation by IP address.
 *
 * <p>Tracks the number of anonymous sessions created from each IP address
 * within a rolling time window to prevent abuse of the anonymous test-taking
 * feature.</p>
 *
 * <p>Rate Limiting Strategy:</p>
 * <ul>
 *   <li>Window: 1 hour rolling window</li>
 *   <li>Limit: 10 sessions per IP per window</li>
 *   <li>Block Duration: 1 hour after exceeding limit</li>
 * </ul>
 *
 * <p>The table is periodically cleaned up to remove expired entries
 * (entries where windowStart is older than the cleanup threshold).</p>
 *
 * @see app.skillsoft.assessmentbackend.repository.AnonymousSessionRateLimitRepository
 */
@Entity
@Table(name = "anonymous_session_rate_limits",
    indexes = {
        @Index(name = "idx_rate_limits_window", columnList = "window_start")
    })
public class AnonymousSessionRateLimit {

    /**
     * Maximum sessions allowed per IP per window.
     */
    public static final int MAX_SESSIONS_PER_WINDOW = 10;

    /**
     * Window duration in hours.
     */
    public static final int WINDOW_HOURS = 1;

    /**
     * Block duration in hours after exceeding limit.
     */
    public static final int BLOCK_HOURS = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Client IP address (supports both IPv4 and IPv6).
     * IPv6 addresses can be up to 45 characters.
     */
    @Column(name = "ip_address", nullable = false, unique = true, length = 45)
    private String ipAddress;

    /**
     * Number of sessions created in the current window.
     */
    @Column(name = "session_count", nullable = false)
    private Integer sessionCount = 1;

    /**
     * Start of the current rate limiting window.
     */
    @Column(name = "window_start", nullable = false)
    private LocalDateTime windowStart;

    /**
     * If set, the IP is blocked until this time.
     * Null means not currently blocked.
     */
    @Column(name = "blocked_until")
    private LocalDateTime blockedUntil;

    // ========================================
    // Constructors
    // ========================================

    /**
     * Default constructor required by JPA.
     */
    public AnonymousSessionRateLimit() {
        // Required for JPA
    }

    /**
     * Create a new rate limit entry for an IP address.
     *
     * @param ipAddress The client IP address
     */
    public AnonymousSessionRateLimit(String ipAddress) {
        this.ipAddress = ipAddress;
        this.sessionCount = 1;
        this.windowStart = LocalDateTime.now();
    }

    // ========================================
    // Business Methods
    // ========================================

    /**
     * Check if this IP is currently blocked.
     *
     * @return true if blocked and block hasn't expired
     */
    public boolean isBlocked() {
        return blockedUntil != null && LocalDateTime.now().isBefore(blockedUntil);
    }

    /**
     * Check if the current window has expired.
     *
     * @return true if windowStart is older than WINDOW_HOURS
     */
    public boolean isWindowExpired() {
        return windowStart.plusHours(WINDOW_HOURS).isBefore(LocalDateTime.now());
    }

    /**
     * Check if the session limit has been reached for this window.
     *
     * @return true if sessionCount >= MAX_SESSIONS_PER_WINDOW
     */
    public boolean isLimitReached() {
        return sessionCount >= MAX_SESSIONS_PER_WINDOW;
    }

    /**
     * Increment the session count and check if limit is exceeded.
     * If limit is exceeded, sets blockedUntil.
     *
     * @return true if session was allowed, false if blocked
     */
    public boolean incrementAndCheck() {
        // If blocked, deny
        if (isBlocked()) {
            return false;
        }

        // If window expired, reset
        if (isWindowExpired()) {
            resetWindow();
        }

        // Increment count
        this.sessionCount++;

        // Check if limit exceeded
        if (isLimitReached()) {
            this.blockedUntil = LocalDateTime.now().plusHours(BLOCK_HOURS);
            return false;
        }

        return true;
    }

    /**
     * Reset the rate limiting window.
     * Called when the previous window has expired.
     */
    public void resetWindow() {
        this.sessionCount = 0;
        this.windowStart = LocalDateTime.now();
        this.blockedUntil = null;
    }

    /**
     * Get remaining sessions allowed in current window.
     *
     * @return Number of sessions remaining, 0 if limit reached or blocked
     */
    public int getRemainingAllowed() {
        if (isBlocked()) {
            return 0;
        }
        if (isWindowExpired()) {
            return MAX_SESSIONS_PER_WINDOW;
        }
        return Math.max(0, MAX_SESSIONS_PER_WINDOW - sessionCount);
    }

    /**
     * Get seconds until block expires.
     *
     * @return Seconds until unblocked, or 0 if not blocked
     */
    public long getSecondsUntilUnblocked() {
        if (!isBlocked()) {
            return 0;
        }
        return java.time.Duration.between(LocalDateTime.now(), blockedUntil).getSeconds();
    }

    // ========================================
    // Getters and Setters
    // ========================================

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Integer getSessionCount() {
        return sessionCount;
    }

    public void setSessionCount(Integer sessionCount) {
        this.sessionCount = sessionCount;
    }

    public LocalDateTime getWindowStart() {
        return windowStart;
    }

    public void setWindowStart(LocalDateTime windowStart) {
        this.windowStart = windowStart;
    }

    public LocalDateTime getBlockedUntil() {
        return blockedUntil;
    }

    public void setBlockedUntil(LocalDateTime blockedUntil) {
        this.blockedUntil = blockedUntil;
    }

    // ========================================
    // Object Overrides
    // ========================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnonymousSessionRateLimit that = (AnonymousSessionRateLimit) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AnonymousSessionRateLimit{" +
                "id=" + id +
                ", ipAddress='" + ipAddress + '\'' +
                ", sessionCount=" + sessionCount +
                ", windowStart=" + windowStart +
                ", blockedUntil=" + blockedUntil +
                ", isBlocked=" + isBlocked() +
                '}';
    }
}
