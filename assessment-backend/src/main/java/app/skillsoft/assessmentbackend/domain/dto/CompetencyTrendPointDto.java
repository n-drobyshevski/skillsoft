package app.skillsoft.assessmentbackend.domain.dto;

import java.util.UUID;

/**
 * Lightweight competency score for trend tracking.
 * Only includes fields needed for time-series visualization.
 */
public record CompetencyTrendPointDto(
        UUID competencyId,
        String competencyName,
        Double percentage,
        Double ciLower,
        Double ciUpper
) {
}
