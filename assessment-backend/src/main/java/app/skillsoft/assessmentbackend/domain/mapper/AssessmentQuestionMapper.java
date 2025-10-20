package app.skillsoft.assessmentbackend.domain.mapper;

import app.skillsoft.assessmentbackend.domain.dto.AssessmentQuestionDto;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentQuestion;

public interface AssessmentQuestionMapper {
    AssessmentQuestion fromDto(AssessmentQuestionDto dto);
    AssessmentQuestionDto toDto(AssessmentQuestion entity);
}
