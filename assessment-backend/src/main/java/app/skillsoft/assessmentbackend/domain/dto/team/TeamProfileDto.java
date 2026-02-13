package app.skillsoft.assessmentbackend.domain.dto.team;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for team profile with competency saturation and personality data.
 * Used for TEAM_FIT assessments.
 */
public record TeamProfileDto(
        UUID teamId,
        String teamName,
        List<TeamMemberSummaryDto> members,
        List<CompetencySaturationDto> competencySaturation,
        Map<String, Double> averagePersonality,
        List<SkillGapDto> skillGaps
) {}
