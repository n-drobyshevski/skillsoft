package app.skillsoft.assessmentbackend.domain.dto.comparison;

import java.util.Map;
import java.util.UUID;

/**
 * Gap coverage entry for a single team gap competency.
 * Shows which candidates can cover this gap and who covers it best.
 */
public record GapCoverageEntryDto(
    UUID competencyId,
    String competencyName,
    Double teamSaturation,
    Map<UUID, Double> candidateCoverage,
    UUID bestCandidateId
) {}
