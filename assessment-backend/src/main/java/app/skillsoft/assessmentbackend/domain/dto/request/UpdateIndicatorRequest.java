package app.skillsoft.assessmentbackend.domain.dto.request;

import app.skillsoft.assessmentbackend.domain.entities.ApprovalStatus;
import app.skillsoft.assessmentbackend.domain.entities.ContextScope;
import app.skillsoft.assessmentbackend.domain.entities.IndicatorMeasurementType;
import app.skillsoft.assessmentbackend.domain.entities.ObservabilityLevel;
import jakarta.validation.constraints.*;

/**
 * Request DTO for updating an existing Behavioral Indicator.
 *
 * Unlike CreateIndicatorRequest, competencyId is not included as
 * the indicator's parent competency should not change after creation.
 *
 * Part of API Standardization per docs/API_STANDARDIZATION_STRATEGY.md
 */
public record UpdateIndicatorRequest(
        @NotBlank(message = "Title is required")
        @Size(min = 5, max = 150, message = "Title must be between 5 and 150 characters")
        String title,

        @NotBlank(message = "Description is required")
        @Size(min = 20, max = 500, message = "Description must be between 20 and 500 characters")
        String description,

        @NotNull(message = "Observability level is required")
        ObservabilityLevel observabilityLevel,

        @NotNull(message = "Measurement type is required")
        IndicatorMeasurementType measurementType,

        @DecimalMin(value = "0.0", message = "Weight must be at least 0.0")
        @DecimalMax(value = "1.0", message = "Weight cannot exceed 1.0")
        float weight,

        @Size(max = 1000, message = "Examples cannot exceed 1000 characters")
        String examples,

        @Size(max = 1000, message = "Counter examples cannot exceed 1000 characters")
        String counterExamples,

        @NotNull(message = "Active status is required")
        Boolean isActive,

        ApprovalStatus approvalStatus,

        @Min(value = 0, message = "Order index must be non-negative")
        Integer orderIndex,

        ContextScope contextScope
) {
}
