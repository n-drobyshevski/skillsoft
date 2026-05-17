package app.skillsoft.assessmentbackend.domain.dto.passport;

/**
 * Big Five personality profile with frontend-compatible field names.
 * All scores on 0-100 scale. emotionalStability is the inverse of backend NEUROTICISM.
 */
public record BigFiveProfileDto(
    double openness,
    double conscientiousness,
    double extraversion,
    double agreeableness,
    double emotionalStability
) {}
