package app.skillsoft.assessmentbackend.domain.dto;

import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for updating an existing TestTemplate.
 * All fields are optional; only provided fields will be updated.
 * Per ROADMAP.md Section 1.C: Supports goal and blueprint updates.
 * 
 * <p><b>Note:</b> The {@code competencyIds} field is deprecated and will be removed
 * in a future version. Use {@code blueprint} configuration instead.
 */
public record UpdateTestTemplateRequest(
        @Size(max = 255, message = "Template name must not exceed 255 characters")
        String name,

        String description,

        /** Assessment goal type: OVERVIEW, JOB_FIT, or TEAM_FIT */
        AssessmentGoal goal,

        /** Blueprint JSONB configuration for test assembly */
        Map<String, Object> blueprint,

        /** Deprecated: Use blueprint instead */
        List<UUID> competencyIds,

        Integer questionsPerIndicator,

        Integer timeLimitMinutes,

        Double passingScore,

        Boolean isActive,

        Boolean shuffleQuestions,

        Boolean shuffleOptions,

        Boolean allowSkip,

        Boolean allowBackNavigation,

        Boolean showResultsImmediately
) {
}
