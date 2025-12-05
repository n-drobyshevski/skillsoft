package app.skillsoft.assessmentbackend.domain.dto;

import app.skillsoft.assessmentbackend.domain.entities.ApprovalStatus;
import app.skillsoft.assessmentbackend.domain.entities.CompetencyCategory;
import app.skillsoft.assessmentbackend.domain.entities.ProficiencyLevel;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CompetencyDto(
        UUID id,
        String name,
        String description,
        CompetencyCategory category,
        ProficiencyLevel level,
        @Valid StandardCodesDto standardCodes,
        boolean isActive,
        ApprovalStatus approvalStatus,
        List<BehavioralIndicatorDto> behavioralIndicators,
        int version,
        LocalDateTime createdAt,
        LocalDateTime lastModified
) {
}
