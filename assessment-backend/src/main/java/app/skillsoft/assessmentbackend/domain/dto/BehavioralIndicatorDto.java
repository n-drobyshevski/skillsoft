package app.skillsoft.assessmentbackend.domain.dto;

import app.skillsoft.assessmentbackend.domain.entities.ApprovalStatus;
import app.skillsoft.assessmentbackend.domain.entities.IndicatorMeasurementType;
import app.skillsoft.assessmentbackend.domain.entities.ProficiencyLevel;

import java.util.UUID;

public record BehavioralIndicatorDto(
        UUID id,
        UUID competencyId,
        String title,
        String description,
        ProficiencyLevel observabilityLevel,
        IndicatorMeasurementType measurementType,
        float weight,
        String examples,
        String counterExamples,
        boolean isActive,
        ApprovalStatus approvalStatus,
        Integer orderIndex
) {
}
