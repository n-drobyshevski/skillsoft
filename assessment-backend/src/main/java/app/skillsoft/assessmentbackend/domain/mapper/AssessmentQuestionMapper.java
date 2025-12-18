package app.skillsoft.assessmentbackend.domain.mapper;

import app.skillsoft.assessmentbackend.domain.dto.AssessmentQuestionDto;
import app.skillsoft.assessmentbackend.domain.dto.request.CreateQuestionRequest;
import app.skillsoft.assessmentbackend.domain.dto.request.UpdateQuestionRequest;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentQuestion;

/**
 * Mapper for AssessmentQuestion entity and its DTOs.
 *
 * Part of API Standardization per docs/API_STANDARDIZATION_STRATEGY.md
 */
public interface AssessmentQuestionMapper {
    AssessmentQuestion fromDto(AssessmentQuestionDto dto);
    AssessmentQuestionDto toDto(AssessmentQuestion entity);

    /**
     * Converts CreateQuestionRequest to AssessmentQuestion entity.
     * Note: behavioralIndicator relationship is set by the service layer.
     */
    AssessmentQuestion fromCreateRequest(CreateQuestionRequest request);

    /**
     * Converts UpdateQuestionRequest to AssessmentQuestion entity.
     * Note: ID and behavioralIndicator are preserved from existing entity.
     */
    AssessmentQuestion fromUpdateRequest(UpdateQuestionRequest request);
}
