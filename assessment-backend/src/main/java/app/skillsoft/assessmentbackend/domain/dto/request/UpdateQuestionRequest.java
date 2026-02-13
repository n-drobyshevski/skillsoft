package app.skillsoft.assessmentbackend.domain.dto.request;

import app.skillsoft.assessmentbackend.domain.entities.DifficultyLevel;
import app.skillsoft.assessmentbackend.domain.entities.QuestionType;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for updating an existing Assessment Question.
 *
 * Unlike CreateQuestionRequest, this DTO does not require behavioralIndicatorId
 * as the question's parent indicator is already established and should not change.
 *
 * Part of API Standardization per docs/API_STANDARDIZATION_STRATEGY.md
 */
public record UpdateQuestionRequest(
        @NotBlank(message = "Question text is required")
        @Size(min = 10, max = 2000, message = "Question text must be between 10 and 2000 characters")
        String questionText,

        @NotNull(message = "Question type is required")
        QuestionType questionType,

        @NotEmpty(message = "At least one answer option is required")
        List<Map<String, Object>> answerOptions,

        String scoringRubric,

        @Min(value = 10, message = "Time limit must be at least 10 seconds")
        @Max(value = 600, message = "Time limit cannot exceed 600 seconds")
        Integer timeLimit,

        @NotNull(message = "Difficulty level is required")
        DifficultyLevel difficultyLevel,

        Map<String, Object> metadata,

        @NotNull(message = "Active status is required")
        Boolean isActive,

        @Min(value = 0, message = "Order index must be non-negative")
        @Max(value = 50, message = "Order index cannot exceed 50")
        Integer orderIndex
) {
}
