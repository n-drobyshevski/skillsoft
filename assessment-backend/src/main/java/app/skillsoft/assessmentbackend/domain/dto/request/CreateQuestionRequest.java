package app.skillsoft.assessmentbackend.domain.dto.request;

import app.skillsoft.assessmentbackend.domain.entities.DifficultyLevel;
import app.skillsoft.assessmentbackend.domain.entities.QuestionType;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for creating a new Assessment Question.
 *
 * This DTO enforces validation rules at the API layer to prevent
 * raw JPA entities from being accepted in controllers.
 *
 * Part of API Standardization per docs/API_STANDARDIZATION_STRATEGY.md
 */
public record CreateQuestionRequest(
        @NotNull(message = "Behavioral indicator ID is required")
        UUID behavioralIndicatorId,

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

        Boolean isActive,

        @Min(value = 0, message = "Order index must be non-negative")
        @Max(value = 50, message = "Order index cannot exceed 50")
        Integer orderIndex
) {
    /**
     * Returns isActive with default value of true if not specified.
     */
    public boolean isActiveOrDefault() {
        return isActive != null ? isActive : true;
    }

    /**
     * Returns orderIndex with default value of 0 if not specified.
     */
    public int orderIndexOrDefault() {
        return orderIndex != null ? orderIndex : 0;
    }

    /**
     * Returns timeLimit with default value of 60 seconds if not specified.
     */
    public int timeLimitOrDefault() {
        return timeLimit != null ? timeLimit : 60;
    }
}
