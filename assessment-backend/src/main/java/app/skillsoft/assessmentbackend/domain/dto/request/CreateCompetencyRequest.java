package app.skillsoft.assessmentbackend.domain.dto.request;

import app.skillsoft.assessmentbackend.domain.dto.StandardCodesDto;
import app.skillsoft.assessmentbackend.domain.entities.ApprovalStatus;
import app.skillsoft.assessmentbackend.domain.entities.CompetencyCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new Competency.
 *
 * Part of API Standardization per docs/API_STANDARDIZATION_STRATEGY.md
 */
public record CreateCompetencyRequest(
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @NotBlank(message = "Description is required")
        @Size(min = 50, max = 1000, message = "Description must be between 50 and 1000 characters")
        String description,

        @NotNull(message = "Category is required")
        CompetencyCategory category,

        @Valid
        StandardCodesDto standardCodes,

        Boolean isActive,

        ApprovalStatus approvalStatus
) {
    /**
     * Returns isActive with default value of true if not specified.
     */
    public boolean isActiveOrDefault() {
        return isActive != null ? isActive : true;
    }

    /**
     * Returns approvalStatus with default value of DRAFT if not specified.
     */
    public ApprovalStatus approvalStatusOrDefault() {
        return approvalStatus != null ? approvalStatus : ApprovalStatus.DRAFT;
    }
}
