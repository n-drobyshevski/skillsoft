package app.skillsoft.assessmentbackend.domain.dto;

import app.skillsoft.assessmentbackend.domain.entities.DifficultyLevel;
import app.skillsoft.assessmentbackend.domain.entities.QuestionType;

import java.util.UUID;

public record AssessmentQuestionDto(
        UUID id,
        UUID behavioralIndicatorId,
        String questionText,
        QuestionType questionType,
        String answerOptions,
        String scoringRubric,
        Integer timeLimit,
        DifficultyLevel difficultyLevel,
        boolean isActive,
        int orderIndex
) {
}
