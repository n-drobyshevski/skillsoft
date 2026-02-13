package app.skillsoft.assessmentbackend.domain.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lightweight DTO for listing test results.
 * Contains summary information for dashboard/list views.
 */
public record TestResultSummaryDto(
        UUID id,
        UUID sessionId,
        UUID templateId,
        String templateName,
        Double overallPercentage,
        Boolean passed,
        LocalDateTime completedAt
) {
}
