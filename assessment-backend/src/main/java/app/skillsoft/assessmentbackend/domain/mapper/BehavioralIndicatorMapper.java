package app.skillsoft.assessmentbackend.domain.mapper;

import app.skillsoft.assessmentbackend.domain.dto.BehavioralIndicatorDto;
import app.skillsoft.assessmentbackend.domain.dto.request.CreateIndicatorRequest;
import app.skillsoft.assessmentbackend.domain.dto.request.UpdateIndicatorRequest;
import app.skillsoft.assessmentbackend.domain.entities.BehavioralIndicator;

/**
 * Mapper for BehavioralIndicator entity and its DTOs.
 *
 * Part of API Standardization per docs/API_STANDARDIZATION_STRATEGY.md
 */
public interface BehavioralIndicatorMapper {
    BehavioralIndicator fromDto(BehavioralIndicatorDto dto);
    BehavioralIndicatorDto toDto(BehavioralIndicator entity);

    /**
     * Converts CreateIndicatorRequest to BehavioralIndicator entity.
     * Note: competency relationship is set by the service layer.
     */
    BehavioralIndicator fromCreateRequest(CreateIndicatorRequest request);

    /**
     * Converts UpdateIndicatorRequest to BehavioralIndicator entity.
     * Note: ID and competency are preserved from existing entity.
     */
    BehavioralIndicator fromUpdateRequest(UpdateIndicatorRequest request);
}
