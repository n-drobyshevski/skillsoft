package app.skillsoft.assessmentbackend.domain.mapper;

import app.skillsoft.assessmentbackend.domain.dto.CompetencyDto;
import app.skillsoft.assessmentbackend.domain.entities.Competency;

public interface CompetencyMapper {
    Competency fromDto(CompetencyDto dto);
    CompetencyDto toDto(Competency entity);
}
