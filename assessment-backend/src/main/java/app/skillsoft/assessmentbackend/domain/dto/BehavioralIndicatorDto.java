package app.skillsoft.assessmentbackend.domain.dto;

import app.skillsoft.assessmentbackend.domain.entities.ApprovalStatus;
import app.skillsoft.assessmentbackend.domain.entities.ContextScope;
import app.skillsoft.assessmentbackend.domain.entities.IndicatorMeasurementType;
import app.skillsoft.assessmentbackend.domain.entities.ObservabilityLevel;

import java.util.UUID;

public record BehavioralIndicatorDto(
        UUID id,
        UUID competencyId,
        String title,
        String description,
        ObservabilityLevel observabilityLevel,
        IndicatorMeasurementType measurementType,
        float weight,
        String examples,
        String counterExamples,
        boolean isActive,
        ApprovalStatus approvalStatus,
        Integer orderIndex,
        ContextScope contextScope
) {
}
