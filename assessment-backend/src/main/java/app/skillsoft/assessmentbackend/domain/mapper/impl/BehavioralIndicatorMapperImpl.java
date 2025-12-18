package app.skillsoft.assessmentbackend.domain.mapper.impl;

import app.skillsoft.assessmentbackend.domain.dto.BehavioralIndicatorDto;
import app.skillsoft.assessmentbackend.domain.dto.request.CreateIndicatorRequest;
import app.skillsoft.assessmentbackend.domain.dto.request.UpdateIndicatorRequest;
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
        indicator.setContextScope(dto.contextScope());

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
                entity.getOrderIndex(),
                entity.getContextScope()
        );
    }

    @Override
    public BehavioralIndicator fromCreateRequest(CreateIndicatorRequest request) {
        if (request == null) {
            return null;
        }

        BehavioralIndicator indicator = new BehavioralIndicator();
        indicator.setTitle(request.title());
        indicator.setDescription(request.description());
        indicator.setObservabilityLevel(request.observabilityLevel());
        indicator.setMeasurementType(request.measurementType());
        indicator.setWeight(request.weight());
        indicator.setExamples(request.examples());
        indicator.setCounterExamples(request.counterExamples());
        indicator.setActive(request.isActiveOrDefault());
        indicator.setOrderIndex(request.orderIndexOrDefault());
        indicator.setContextScope(request.contextScope());

        // competency relationship is set by the service layer
        return indicator;
    }

    @Override
    public BehavioralIndicator fromUpdateRequest(UpdateIndicatorRequest request) {
        if (request == null) {
            return null;
        }

        BehavioralIndicator indicator = new BehavioralIndicator();
        indicator.setTitle(request.title());
        indicator.setDescription(request.description());
        indicator.setObservabilityLevel(request.observabilityLevel());
        indicator.setMeasurementType(request.measurementType());
        indicator.setWeight(request.weight());
        indicator.setExamples(request.examples());
        indicator.setCounterExamples(request.counterExamples());
        indicator.setActive(request.isActive());
        indicator.setApprovalStatus(request.approvalStatus());
        indicator.setOrderIndex(request.orderIndex());
        indicator.setContextScope(request.contextScope());

        // ID and competency are preserved from existing entity
        return indicator;
    }
}
