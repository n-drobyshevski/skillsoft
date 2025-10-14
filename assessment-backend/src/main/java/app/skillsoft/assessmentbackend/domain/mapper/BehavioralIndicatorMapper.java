package app.skillsoft.assessmentbackend.domain.mapper;

import app.skillsoft.assessmentbackend.domain.dto.BehavioralIndicatorDto;
import app.skillsoft.assessmentbackend.domain.entities.BehavioralIndicator;

public interface BehavioralIndicatorMapper {
    BehavioralIndicator fromDto(BehavioralIndicatorDto dto);
    BehavioralIndicatorDto toDto(BehavioralIndicator entity);
}
