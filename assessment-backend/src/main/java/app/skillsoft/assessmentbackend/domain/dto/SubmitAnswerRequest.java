package app.skillsoft.assessmentbackend.domain.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for submitting an answer to a question.
 * Only one of the answer fields should be populated based on question type.
 */
public record SubmitAnswerRequest(
        @NotNull(message = "Session ID is required")
        UUID sessionId,

        @NotNull(message = "Question ID is required")
        UUID questionId,

        /**
         * Selected option IDs for single/multiple choice questions.
         */
        List<String> selectedOptionIds,

        /**
         * Value for Likert scale questions (typically 1-5 or 1-7).
         */
        Integer likertValue,

        /**
         * Ordered list of option IDs for ranking questions.
         */
        List<String> rankingOrder,

        /**
         * Free-text response for open-ended questions.
         */
        String textResponse,

        /**
         * Time spent on this question in seconds.
         */
        Integer timeSpentSeconds,

        /**
         * Whether the user is skipping this question.
         */
        Boolean skip
) {
    public SubmitAnswerRequest {
        if (timeSpentSeconds == null) timeSpentSeconds = 0;
        if (skip == null) skip = false;
    }
}
