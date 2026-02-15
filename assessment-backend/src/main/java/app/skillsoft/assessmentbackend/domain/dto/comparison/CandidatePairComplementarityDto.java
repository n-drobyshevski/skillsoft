package app.skillsoft.assessmentbackend.domain.dto.comparison;

import java.util.UUID;

/**
 * Complementarity score for a pair of candidates.
 * Measures how many team gaps are collectively covered by the pair,
 * producing a combined coverage percentage.
 */
public record CandidatePairComplementarityDto(
    UUID candidateA,
    UUID candidateB,
    String candidateAName,
    String candidateBName,
    double complementarityScore,
    int combinedGapsCovered,
    int totalTeamGaps
) {}
