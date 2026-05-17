package app.skillsoft.assessmentbackend.domain.dto.comparison;

import java.util.Map;
import java.util.UUID;

/**
 * Per-candidate summary within a comparison.
 * Contains overall scores, team fit metrics, personality profile,
 * and multiple ranking dimensions.
 */
public record CandidateSummaryDto(
    UUID resultId,
    String displayName,
    Double overallPercentage,
    Boolean passed,
    int overallRank,
    int diversityRank,
    int personalityRank,
    Double diversityRatio,
    Double saturationRatio,
    Double teamFitMultiplier,
    Double personalityCompatibility,
    Map<String, Double> bigFiveProfile,
    Map<String, Double> competencySaturation,
    String completedAt
) {}
