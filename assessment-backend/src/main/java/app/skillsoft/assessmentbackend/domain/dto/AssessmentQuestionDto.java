package app.skillsoft.assessmentbackend.domain.dto;

import app.skillsoft.assessmentbackend.domain.entities.DifficultyLevel;
import app.skillsoft.assessmentbackend.domain.entities.QuestionType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AssessmentQuestionDto(
        UUID id,
        UUID behavioralIndicatorId,
        String questionText,
        QuestionType questionType,
        List<Map<String, Object>> answerOptions,
        String scoringRubric,
        Integer timeLimit,
        DifficultyLevel difficultyLevel,
        boolean isActive,
        int orderIndex
) {
}
