package app.skillsoft.assessmentbackend.domain.entities;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a test activity event for audit trail.
 * Records significant state changes in test sessions for activity tracking
 * and audit logging purposes.
 */
@Entity
@Table(name = "test_activity_events", indexes = {
        @Index(name = "idx_activity_template", columnList = "template_id"),
        @Index(name = "idx_activity_timestamp", columnList = "event_timestamp"),
        @Index(name = "idx_activity_clerk_user", columnList = "clerk_user_id"),
        @Index(name = "idx_activity_event_type", columnList = "event_type")
})
public class TestActivityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "event_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private ActivityEventType eventType;

    @Column(name = "clerk_user_id", nullable = false, length = 100)
    private String clerkUserId;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "event_timestamp", nullable = false)
    private LocalDateTime eventTimestamp;

    @Type(JsonType.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    // Constructors
    public TestActivityEvent() {
        // Default constructor required by JPA
    }

    public TestActivityEvent(UUID sessionId, ActivityEventType eventType, String clerkUserId, UUID templateId) {
        this.sessionId = sessionId;
        this.eventType = eventType;
        this.clerkUserId = clerkUserId;
        this.templateId = templateId;
        this.eventTimestamp = LocalDateTime.now();
    }

    // Factory methods for specific event types
    public static TestActivityEvent sessionStarted(TestSession session) {
        TestActivityEvent event = new TestActivityEvent(
                session.getId(),
                ActivityEventType.SESSION_STARTED,
                session.getClerkUserId(),
                session.getTemplateId()
        );
        event.addMetadata("templateName", session.getTemplate().getName());
        return event;
    }

    public static TestActivityEvent sessionCompleted(TestSession session, Double score, Boolean passed) {
        TestActivityEvent event = new TestActivityEvent(
                session.getId(),
                ActivityEventType.SESSION_COMPLETED,
                session.getClerkUserId(),
                session.getTemplateId()
        );
        event.addMetadata("templateName", session.getTemplate().getName());
        if (score != null) {
            event.addMetadata("score", score);
        }
        if (passed != null) {
            event.addMetadata("passed", passed);
        }
        if (session.getStartedAt() != null && session.getCompletedAt() != null) {
            long timeSpentSeconds = java.time.Duration.between(
                    session.getStartedAt(), session.getCompletedAt()
            ).getSeconds();
            event.addMetadata("timeSpentSeconds", timeSpentSeconds);
        }
        return event;
    }

    public static TestActivityEvent sessionAbandoned(TestSession session) {
        TestActivityEvent event = new TestActivityEvent(
                session.getId(),
                ActivityEventType.SESSION_ABANDONED,
                session.getClerkUserId(),
                session.getTemplateId()
        );
        event.addMetadata("templateName", session.getTemplate().getName());
        event.addMetadata("questionsAnswered", session.getAnswers().size());
        event.addMetadata("totalQuestions", session.getQuestionOrder().size());
        return event;
    }

    public static TestActivityEvent sessionTimedOut(TestSession session) {
        TestActivityEvent event = new TestActivityEvent(
                session.getId(),
                ActivityEventType.SESSION_TIMED_OUT,
                session.getClerkUserId(),
                session.getTemplateId()
        );
        event.addMetadata("templateName", session.getTemplate().getName());
        event.addMetadata("questionsAnswered", session.getAnswers().size());
        event.addMetadata("totalQuestions", session.getQuestionOrder().size());
        return event;
    }

    // Business methods
    public void addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }

    public Object getMetadataValue(String key) {
        if (this.metadata == null) {
            return null;
        }
        return this.metadata.get(key);
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public ActivityEventType getEventType() {
        return eventType;
    }

    public void setEventType(ActivityEventType eventType) {
        this.eventType = eventType;
    }

    public String getClerkUserId() {
        return clerkUserId;
    }

    public void setClerkUserId(String clerkUserId) {
        this.clerkUserId = clerkUserId;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public void setTemplateId(UUID templateId) {
        this.templateId = templateId;
    }

    public LocalDateTime getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(LocalDateTime eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestActivityEvent that = (TestActivityEvent) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "TestActivityEvent{" +
                "id=" + id +
                ", sessionId=" + sessionId +
                ", eventType=" + eventType +
                ", clerkUserId='" + clerkUserId + '\'' +
                ", templateId=" + templateId +
                ", eventTimestamp=" + eventTimestamp +
                '}';
    }
}
