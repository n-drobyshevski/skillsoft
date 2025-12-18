package app.skillsoft.assessmentbackend.domain.mapper;

import app.skillsoft.assessmentbackend.domain.dto.CompetencyDto;
import app.skillsoft.assessmentbackend.domain.dto.request.CreateCompetencyRequest;
import app.skillsoft.assessmentbackend.domain.dto.request.UpdateCompetencyRequest;
import app.skillsoft.assessmentbackend.domain.entities.Competency;

/**
 * Mapper for Competency entity and its DTOs.
 *
 * Part of API Standardization per docs/API_STANDARDIZATION_STRATEGY.md
 */
public interface CompetencyMapper {
    Competency fromDto(CompetencyDto dto);
    CompetencyDto toDto(Competency entity);

    /**
     * Converts CreateCompetencyRequest to Competency entity.
     */
    Competency fromCreateRequest(CreateCompetencyRequest request);

    /**
     * Converts UpdateCompetencyRequest to Competency entity.
     * Note: ID and version are preserved from existing entity.
     */
    Competency fromUpdateRequest(UpdateCompetencyRequest request);
}
