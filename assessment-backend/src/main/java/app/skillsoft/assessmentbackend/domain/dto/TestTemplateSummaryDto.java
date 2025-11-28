package app.skillsoft.assessmentbackend.domain.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Lightweight DTO for listing test templates.
 * Contains summary information without full configuration details.
 */
public record TestTemplateSummaryDto(
        UUID id,
        String name,
        String description,
        Integer competencyCount,
        Integer timeLimitMinutes,
        Double passingScore,
        Boolean isActive,
        LocalDateTime createdAt
) {
}
