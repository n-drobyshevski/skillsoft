package app.skillsoft.assessmentbackend.domain.mapper.impl;

import app.skillsoft.assessmentbackend.domain.dto.AssessmentQuestionDto;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentQuestion;
import app.skillsoft.assessmentbackend.domain.mapper.AssessmentQuestionMapper;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AssessmentQuestionMapperImpl implements AssessmentQuestionMapper {

    @Override
    public AssessmentQuestion fromDto(AssessmentQuestionDto dto) {
        if (dto == null) {
            return null;
        }

        AssessmentQuestion question = new AssessmentQuestion();
        question.setId(dto.id());
        question.setQuestionText(dto.questionText());
        question.setQuestionType(dto.questionType());
        question.setAnswerOptions(dto.answerOptions());
        question.setScoringRubric(dto.scoringRubric());
        question.setTimeLimit(dto.timeLimit());
        question.setDifficultyLevel(dto.difficultyLevel());
        question.setActive(dto.isActive());
        question.setOrderIndex(dto.orderIndex());

        // DO NOT set behavioralIndicator here - it will be set by the service within transaction
        // The behavioralIndicatorId from DTO is passed separately to the service layer

        return question;
    }

    @Override
    public AssessmentQuestionDto toDto(AssessmentQuestion entity) {
        if (entity == null) {
            return null;
        }

        UUID behavioralIndicatorId = null;
        if (entity.getBehavioralIndicator() != null) {
            behavioralIndicatorId = entity.getBehavioralIndicator().getId();
        }

        return new AssessmentQuestionDto(
                entity.getId(),
                behavioralIndicatorId,
                entity.getQuestionText(),
                entity.getQuestionType(),
                entity.getAnswerOptions(),
                entity.getScoringRubric(),
                entity.getTimeLimit(),
                entity.getDifficultyLevel(),
                entity.isActive(),
                entity.getOrderIndex()
        );
    }
}
