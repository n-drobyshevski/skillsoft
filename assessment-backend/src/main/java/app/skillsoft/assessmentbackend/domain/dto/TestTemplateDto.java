package app.skillsoft.assessmentbackend.domain.dto;

import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for TestTemplate entity response.
 * Per ROADMAP.md Section 1.C: Includes goal and blueprint fields.
 * 
 * <p><b>Note:</b> The {@code competencyIds} field is deprecated and will be removed
 * in a future version. Use {@code blueprint} configuration instead.
 */
public record TestTemplateDto(
        UUID id,
        String name,
        String description,
        /** Assessment goal type: OVERVIEW, JOB_FIT, or TEAM_FIT */
        AssessmentGoal goal,
        /** Blueprint JSONB configuration for test assembly */
        Map<String, Object> blueprint,
        /** Deprecated: Use blueprint instead. Will be removed in future version. */
        List<UUID> competencyIds,
        Integer questionsPerIndicator,
        Integer timeLimitMinutes,
        Double passingScore,
        Boolean isActive,
        Boolean shuffleQuestions,
        Boolean shuffleOptions,
        Boolean allowSkip,
        Boolean allowBackNavigation,
        Boolean showResultsImmediately,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
