package app.skillsoft.assessmentbackend.domain.dto;

import app.skillsoft.assessmentbackend.domain.entities.SessionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lightweight DTO for listing test sessions.
 * Contains summary information for dashboard/list views.
 */
public record TestSessionSummaryDto(
        UUID id,
        UUID templateId,
        String templateName,
        SessionStatus status,
        Integer progress,          // Percentage of questions answered
        Integer timeRemainingSeconds,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt
) {
}
