package app.skillsoft.assessmentbackend.domain.dto;

import app.skillsoft.assessmentbackend.domain.entities.DifficultyLevel;
import app.skillsoft.assessmentbackend.domain.entities.QuestionType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for AssessmentQuestion entity.
 * Supports Smart Assessment features per ROADMAP.md:
 * - questionType: LIKERT, SJT, MCQ (primary types)
 * - answerOptions: JSON with vector weights for SJT questions
 * - metadata: JSONB for tags, context filtering, and additional scoring info
 */
public record AssessmentQuestionDto(
        UUID id,
        UUID behavioralIndicatorId,
        String questionText,
        QuestionType questionType,
        List<Map<String, Object>> answerOptions,
        String scoringRubric,
        Integer timeLimit,
        DifficultyLevel difficultyLevel,
        Map<String, Object> metadata,
        boolean isActive,
        int orderIndex
) {
}
