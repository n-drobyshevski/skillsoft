package app.skillsoft.assessmentbackend.domain.entities;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a test session.
 * Tracks the state and progress of a user taking a test.
 */
@Entity
@Table(name = "test_sessions")
public class TestSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private TestTemplate template;

    /**
     * Clerk user ID for authenticated sessions.
     * NULL for anonymous sessions (share link based).
     */
    @Column(name = "clerk_user_id")
    private String clerkUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status = SessionStatus.NOT_STARTED;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "current_question_index")
    private Integer currentQuestionIndex = 0;

    @Column(name = "time_remaining_seconds")
    private Integer timeRemainingSeconds;

    @Column(name = "question_order", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<UUID> questionOrder = new ArrayList<>();

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TestAnswer> answers = new ArrayList<>();

    @OneToOne(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private TestResult result;

    // ========================================
    // ANONYMOUS SESSION FIELDS
    // ========================================

    /**
     * Reference to the share link used to create this session.
     * NULL for authenticated sessions.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "share_link_id")
    private TemplateShareLink shareLink;

    /**
     * SHA-256 hash of the session access token.
     * Used for anonymous session authentication instead of Clerk JWT.
     * 64 hex characters.
     */
    @Column(name = "session_access_token_hash", length = 64)
    private String sessionAccessTokenHash;

    /**
     * Client IP address for rate limiting and audit.
     * Supports both IPv4 (15 chars) and IPv6 (45 chars).
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * Client user agent string for analytics.
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * Information collected from anonymous takers after test completion.
     * Contains: firstName, lastName, email (optional), notes (optional), collectedAt.
     */
    @Column(name = "anonymous_taker_info", columnDefinition = "jsonb")
    @Type(JsonType.class)
    private AnonymousTakerInfo anonymousTakerInfo;

    // ========================================
    // END ANONYMOUS SESSION FIELDS
    // ========================================

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Constructors
    public TestSession() {
        // Default constructor required by JPA
    }

    /**
     * Constructor for authenticated sessions.
     *
     * @param template    The test template
     * @param clerkUserId The Clerk user ID
     */
    public TestSession(TestTemplate template, String clerkUserId) {
        this.template = template;
        this.clerkUserId = clerkUserId;
        this.status = SessionStatus.NOT_STARTED;
        if (template != null && template.getTimeLimitMinutes() != null) {
            this.timeRemainingSeconds = template.getTimeLimitMinutes() * 60;
        }
    }

    /**
     * Constructor for anonymous sessions via share link.
     *
     * @param template               The test template
     * @param shareLink              The share link used for access
     * @param sessionAccessTokenHash SHA-256 hash of the session access token
     * @param ipAddress              Client IP address
     * @param userAgent              Client user agent string
     */
    public TestSession(TestTemplate template, TemplateShareLink shareLink,
                       String sessionAccessTokenHash, String ipAddress, String userAgent) {
        this.template = template;
        this.shareLink = shareLink;
        this.sessionAccessTokenHash = sessionAccessTokenHash;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.clerkUserId = null; // Explicitly null for anonymous
        this.status = SessionStatus.NOT_STARTED;
        if (template != null && template.getTimeLimitMinutes() != null) {
            this.timeRemainingSeconds = template.getTimeLimitMinutes() * 60;
        }
    }

    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastActivityAt = LocalDateTime.now();
    }

    // Business methods
    public void start() {
        if (this.status != SessionStatus.NOT_STARTED) {
            throw new IllegalStateException("Cannot start a session that is not in NOT_STARTED status");
        }
        this.status = SessionStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
        this.lastActivityAt = LocalDateTime.now();
    }

    public void complete() {
        if (this.status != SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Cannot complete a session that is not in IN_PROGRESS status");
        }
        this.status = SessionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.lastActivityAt = LocalDateTime.now();
    }

    public void abandon() {
        if (this.status == SessionStatus.COMPLETED) {
            throw new IllegalStateException("Cannot abandon a completed session");
        }
        this.status = SessionStatus.ABANDONED;
        this.completedAt = LocalDateTime.now();
        this.lastActivityAt = LocalDateTime.now();
    }

    public void timeout() {
        if (this.status != SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Cannot timeout a session that is not in IN_PROGRESS status");
        }
        this.status = SessionStatus.TIMED_OUT;
        this.completedAt = LocalDateTime.now();
        this.lastActivityAt = LocalDateTime.now();
    }

    public void updateActivity() {
        this.lastActivityAt = LocalDateTime.now();
    }

    public void addAnswer(TestAnswer answer) {
        answers.add(answer);
        answer.setSession(this);
    }

    public void removeAnswer(TestAnswer answer) {
        answers.remove(answer);
        answer.setSession(null);
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

    public String getClerkUserId() {
        return clerkUserId;
    }

    public void setClerkUserId(String clerkUserId) {
        this.clerkUserId = clerkUserId;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Integer getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }

    public void setCurrentQuestionIndex(Integer currentQuestionIndex) {
        this.currentQuestionIndex = currentQuestionIndex;
    }

    public Integer getTimeRemainingSeconds() {
        return timeRemainingSeconds;
    }

    public void setTimeRemainingSeconds(Integer timeRemainingSeconds) {
        this.timeRemainingSeconds = timeRemainingSeconds;
    }

    public List<UUID> getQuestionOrder() {
        return questionOrder;
    }

    public void setQuestionOrder(List<UUID> questionOrder) {
        this.questionOrder = questionOrder;
    }

    public List<TestAnswer> getAnswers() {
        return answers;
    }

    public void setAnswers(List<TestAnswer> answers) {
        this.answers = answers;
    }

    public TestResult getResult() {
        return result;
    }

    public void setResult(TestResult result) {
        this.result = result;
        if (result != null) {
            result.setSession(this);
        }
    }

    public LocalDateTime getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(LocalDateTime lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Convenience method to get template ID
    @Transient
    public UUID getTemplateId() {
        return template != null ? template.getId() : null;
    }

    /**
     * Check if this is an anonymous session (no Clerk user).
     * Anonymous sessions are created via share links.
     *
     * @return true if this session has no clerkUserId and has a shareLink
     */
    @Transient
    public boolean isAnonymous() {
        return clerkUserId == null && shareLink != null;
    }

    /**
     * Get the share link ID if this is an anonymous session.
     *
     * @return Share link UUID or null for authenticated sessions
     */
    @Transient
    public UUID getShareLinkId() {
        return shareLink != null ? shareLink.getId() : null;
    }

    // ========================================
    // ANONYMOUS SESSION GETTERS/SETTERS
    // ========================================

    public TemplateShareLink getShareLink() {
        return shareLink;
    }

    public void setShareLink(TemplateShareLink shareLink) {
        this.shareLink = shareLink;
    }

    public String getSessionAccessTokenHash() {
        return sessionAccessTokenHash;
    }

    public void setSessionAccessTokenHash(String sessionAccessTokenHash) {
        this.sessionAccessTokenHash = sessionAccessTokenHash;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public AnonymousTakerInfo getAnonymousTakerInfo() {
        return anonymousTakerInfo;
    }

    public void setAnonymousTakerInfo(AnonymousTakerInfo anonymousTakerInfo) {
        this.anonymousTakerInfo = anonymousTakerInfo;
    }

    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestSession that = (TestSession) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "TestSession{" +
                "id=" + id +
                ", clerkUserId='" + clerkUserId + '\'' +
                ", status=" + status +
                ", currentQuestionIndex=" + currentQuestionIndex +
                '}';
    }
}
