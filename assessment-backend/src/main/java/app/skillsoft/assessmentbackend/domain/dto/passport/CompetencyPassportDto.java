package app.skillsoft.assessmentbackend.domain.dto.passport;

import java.util.Map;

/**
 * Response DTO for Competency Passport API.
 * Scores are on 0-100 percentage scale (converted from backend 1-5 scale).
 */
public record CompetencyPassportDto(
    String id,
    String candidateId,
    String clerkUserId,
    String lastUpdated,
    Map<String, Double> scores,
    BigFiveProfileDto bigFiveProfile,
    boolean isValid,
    String expiresAt
) {}
