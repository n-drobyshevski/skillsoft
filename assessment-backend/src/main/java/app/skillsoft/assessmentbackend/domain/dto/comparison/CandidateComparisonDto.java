package app.skillsoft.assessmentbackend.domain.dto.comparison;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Top-level response DTO for candidate comparison mode.
 * Aggregates ranked candidate summaries, competency comparisons,
 * gap coverage matrix, and pairwise complementarity scores.
 */
public record CandidateComparisonDto(
    UUID templateId,
    String templateName,
    UUID teamId,
    String targetRole,
    int teamSize,
    boolean teamAvailable,
    List<CandidateSummaryDto> candidates,
    List<CompetencyComparisonDto> competencyComparison,
    List<GapCoverageEntryDto> gapCoverageMatrix,
    List<CandidatePairComplementarityDto> complementarityPairs,
    Map<String, Double> teamCompetencySaturation
) {}
