package app.skillsoft.assessmentbackend.domain.dto;

import app.skillsoft.assessmentbackend.domain.entities.SessionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for TestSession entity response.
 */
public record TestSessionDto(
        UUID id,
        UUID templateId,
        String templateName,
        String clerkUserId,
        SessionStatus status,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        Integer currentQuestionIndex,
        Integer timeRemainingSeconds,
        List<UUID> questionOrder,
        Integer totalQuestions,
        Integer answeredQuestions,
        LocalDateTime lastActivityAt,
        LocalDateTime createdAt
) {
}
