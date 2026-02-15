package app.skillsoft.assessmentbackend.domain.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Lightweight DTO for historical trend tracking.
 * Contains only the data needed for time-series visualization,
 * avoiding the overhead of full TestResultDto.
 */
public record TrendDataPointDto(
        UUID resultId,
        UUID templateId,
        String templateName,
        Double overallPercentage,
        Boolean passed,
        LocalDateTime completedAt,
        List<CompetencyTrendPointDto> competencyScores
) {
}
