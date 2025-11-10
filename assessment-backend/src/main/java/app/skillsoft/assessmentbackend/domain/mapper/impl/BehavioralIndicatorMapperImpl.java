package app.skillsoft.assessmentbackend.domain.mapper.impl;

import app.skillsoft.assessmentbackend.domain.dto.BehavioralIndicatorDto;
import app.skillsoft.assessmentbackend.domain.entities.BehavioralIndicator;
import app.skillsoft.assessmentbackend.domain.mapper.BehavioralIndicatorMapper;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class BehavioralIndicatorMapperImpl implements BehavioralIndicatorMapper {

    @Override
    public BehavioralIndicator fromDto(BehavioralIndicatorDto dto) {
        if (dto == null) {
            return null;
        }

        BehavioralIndicator indicator = new BehavioralIndicator();
        indicator.setId(dto.id());
        indicator.setTitle(dto.title());
        indicator.setDescription(dto.description());
        indicator.setObservabilityLevel(dto.observabilityLevel());
        indicator.setMeasurementType(dto.measurementType());
        indicator.setWeight(dto.weight());
        indicator.setExamples(dto.examples());
        indicator.setCounterExamples(dto.counterExamples());
        indicator.setActive(dto.isActive());
        indicator.setApprovalStatus(dto.approvalStatus());
        indicator.setOrderIndex(dto.orderIndex());

        // DO NOT set competency here - it will be set by the service within transaction
        // The competencyId from DTO is passed separately to the service layer

        return indicator;
    }

    @Override
    public BehavioralIndicatorDto toDto(BehavioralIndicator entity) {
        if (entity == null) {
            return null;
        }

        UUID competencyId = null;
        if (entity.getCompetency() != null) {
            competencyId = entity.getCompetency().getId();
        }

        return new BehavioralIndicatorDto(
                entity.getId(),
                competencyId,
                entity.getTitle(),
                entity.getDescription(),
                entity.getObservabilityLevel(),
                entity.getMeasurementType(),
                entity.getWeight(),
                entity.getExamples(),
                entity.getCounterExamples(),
                entity.isActive(),
                entity.getApprovalStatus(),
                entity.getOrderIndex()
        );
    }
}
