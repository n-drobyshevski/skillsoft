package app.skillsoft.assessmentbackend.domain.dto;

import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for creating a new TestTemplate.
 * Per ROADMAP.md Section 1.C: Supports goal and blueprint configuration.
 * 
 * <p><b>Note:</b> The {@code competencyIds} field is deprecated and will be removed
 * in a future version. Use {@code blueprint} configuration instead.
 */
public record CreateTestTemplateRequest(
        @NotBlank(message = "Template name is required")
        @Size(max = 255, message = "Template name must not exceed 255 characters")
        String name,

        String description,

        /** Assessment goal type: OVERVIEW, JOB_FIT, or TEAM_FIT */
        AssessmentGoal goal,

        /**
         * Blueprint JSONB configuration for test assembly.
         * Structure varies by goal:
         * - OVERVIEW: { strategy, competencies, aggregationTargets, saveAsPassport }
         * - JOB_FIT: { strategy, onetSocCode, useOnetBenchmarks, requiredTags, excludeTypes }
         * - TEAM_FIT: { strategy, teamId, normalizationStandard, checks, saturationThreshold }
         */
        Map<String, Object> blueprint,

        /** Deprecated: Use blueprint instead. Kept for backward compatibility. */
        List<UUID> competencyIds,

        @Positive(message = "Questions per indicator must be positive")
        Integer questionsPerIndicator,

        @Positive(message = "Time limit must be positive")
        Integer timeLimitMinutes,

        Double passingScore,

        Boolean shuffleQuestions,

        Boolean shuffleOptions,

        Boolean allowSkip,

        Boolean allowBackNavigation,

        Boolean showResultsImmediately
) {
    /**
     * Constructor with defaults for optional fields.
     */
    public CreateTestTemplateRequest {
        if (goal == null) goal = AssessmentGoal.OVERVIEW;
        if (questionsPerIndicator == null) questionsPerIndicator = 3;
        if (timeLimitMinutes == null) timeLimitMinutes = 60;
        if (passingScore == null) passingScore = 70.0;
        if (shuffleQuestions == null) shuffleQuestions = true;
        if (shuffleOptions == null) shuffleOptions = true;
        if (allowSkip == null) allowSkip = true;
        if (allowBackNavigation == null) allowBackNavigation = true;
        if (showResultsImmediately == null) showResultsImmediately = true;
    }
}
