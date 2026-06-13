package app.skillsoft.assessmentbackend.domain.dto;

import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lightweight DTO for listing test results.
 * Contains summary information for dashboard/list views.
 *
 * The goal field carries the assessment type of the originating template
 * (OVERVIEW / JOB_FIT / TEAM_FIT) so list views can categorize results by tab.
 */
public record TestResultSummaryDto(
        UUID id,
        UUID sessionId,
        UUID templateId,
        String templateName,
        AssessmentGoal goal,
        Double overallPercentage,
        Boolean passed,
        LocalDateTime completedAt
) {
}
