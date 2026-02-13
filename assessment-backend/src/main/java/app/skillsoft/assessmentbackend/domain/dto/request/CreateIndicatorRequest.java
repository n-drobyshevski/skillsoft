package app.skillsoft.assessmentbackend.domain.dto.request;

import app.skillsoft.assessmentbackend.domain.entities.ContextScope;
import app.skillsoft.assessmentbackend.domain.entities.IndicatorMeasurementType;
import app.skillsoft.assessmentbackend.domain.entities.ObservabilityLevel;
import jakarta.validation.constraints.*;

import java.util.UUID;

/**
 * Request DTO for creating a new Behavioral Indicator.
 *
 * Part of API Standardization per docs/API_STANDARDIZATION_STRATEGY.md
 */
public record CreateIndicatorRequest(
        @NotNull(message = "Competency ID is required")
        UUID competencyId,

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

        Boolean isActive,

        @Min(value = 0, message = "Order index must be non-negative")
        Integer orderIndex,

        ContextScope contextScope
) {
    /**
     * Returns isActive with default value of true if not specified.
     */
    public boolean isActiveOrDefault() {
        return isActive != null ? isActive : true;
    }

    /**
     * Returns orderIndex with default value of 0 if not specified.
     */
    public int orderIndexOrDefault() {
        return orderIndex != null ? orderIndex : 0;
    }
}
