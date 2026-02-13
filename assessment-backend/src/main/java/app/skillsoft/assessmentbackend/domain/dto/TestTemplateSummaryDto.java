package app.skillsoft.assessmentbackend.domain.dto;

import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lightweight DTO for listing test templates.
 * Contains summary information without full configuration details.
 * 
 * <p>The {@code goal} field indicates the assessment strategy:
 * <ul>
 *   <li>OVERVIEW - Universal baseline assessment (Competency Passport)</li>
 *   <li>JOB_FIT - Job-specific assessment against O*NET/ESCO profiles</li>
 *   <li>TEAM_FIT - Team skill gap analysis</li>
 * </ul>
 */
public record TestTemplateSummaryDto(
        UUID id,
        String name,
        String description,
        /**
         * Assessment goal/strategy for this template.
         * Determines how results are scored and interpreted.
         */
        AssessmentGoal goal,
        Integer competencyCount,
        Integer timeLimitMinutes,
        Double passingScore,
        Boolean isActive,
        LocalDateTime createdAt
) {
}
