package app.skillsoft.assessmentbackend.domain.dto.activity;

import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;
import app.skillsoft.assessmentbackend.domain.entities.SessionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO representing a single test activity event for the activity feed.
 * Used in dashboard widgets and template activity pages.
 */
public record TestActivityDto(
        UUID sessionId,
        String clerkUserId,
        String userName,
        String userImageUrl,
        UUID templateId,
        String templateName,
        AssessmentGoal templateGoal,
        SessionStatus eventType,
        LocalDateTime occurredAt,
        Double score,
        Boolean passed,
        Integer timeSpentSeconds
) {
    /**
     * Factory method to create activity from session data without user info.
     */
    public static TestActivityDto withoutUser(
            UUID sessionId,
            String clerkUserId,
            UUID templateId,
            String templateName,
            AssessmentGoal templateGoal,
            SessionStatus eventType,
            LocalDateTime occurredAt,
            Double score,
            Boolean passed,
            Integer timeSpentSeconds
    ) {
        return new TestActivityDto(
                sessionId,
                clerkUserId,
                null,
                null,
                templateId,
                templateName,
                templateGoal,
                eventType,
                occurredAt,
                score,
                passed,
                timeSpentSeconds
        );
    }

    /**
     * Creates a new DTO with enriched user information.
     */
    public TestActivityDto withUserInfo(String userName, String userImageUrl) {
        return new TestActivityDto(
                this.sessionId,
                this.clerkUserId,
                userName,
                userImageUrl,
                this.templateId,
                this.templateName,
                this.templateGoal,
                this.eventType,
                this.occurredAt,
                this.score,
                this.passed,
                this.timeSpentSeconds
        );
    }
}
