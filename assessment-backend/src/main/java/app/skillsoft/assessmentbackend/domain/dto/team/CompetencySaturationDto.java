package app.skillsoft.assessmentbackend.domain.dto.team;

import java.util.UUID;

/**
 * DTO for competency saturation level within a team.
 * Value ranges from 0.0 (no coverage) to 1.0 (fully saturated).
 */
public record CompetencySaturationDto(
        UUID competencyId,
        Double saturation
) {}
