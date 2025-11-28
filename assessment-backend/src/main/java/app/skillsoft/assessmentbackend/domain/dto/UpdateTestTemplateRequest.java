package app.skillsoft.assessmentbackend.domain.dto;

import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for updating an existing TestTemplate.
 * All fields are optional; only provided fields will be updated.
 */
public record UpdateTestTemplateRequest(
        @Size(max = 255, message = "Template name must not exceed 255 characters")
        String name,

        String description,

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
