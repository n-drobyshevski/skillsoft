package app.skillsoft.assessmentbackend.domain.dto.comparison;

import java.util.Map;
import java.util.UUID;

/**
 * Comparison of a single competency across all candidates.
 * Identifies the best performer and whether this competency
 * represents a team gap (low saturation).
 */
public record CompetencyComparisonDto(
    UUID competencyId,
    String competencyName,
    Double teamSaturation,
    Map<UUID, Double> candidateScores,
    UUID bestCandidateId,
    boolean isTeamGap
) {}
