package app.skillsoft.assessmentbackend.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating a new TestTemplate.
 */
public record CreateTestTemplateRequest(
        @NotBlank(message = "Template name is required")
        @Size(max = 255, message = "Template name must not exceed 255 characters")
        String name,

        String description,

        @NotEmpty(message = "At least one competency ID is required")
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
