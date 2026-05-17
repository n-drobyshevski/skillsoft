package app.skillsoft.assessmentbackend.domain.dto.team;

import java.util.UUID;

/**
 * DTO for skill gap in a team.
 * Represents competencies that are undersaturated.
 */
public record SkillGapDto(
        UUID competencyId,
        String competencyName,
        Double currentSaturation
) {}
