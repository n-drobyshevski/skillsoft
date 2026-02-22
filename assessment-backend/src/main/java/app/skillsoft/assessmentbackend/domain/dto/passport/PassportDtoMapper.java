package app.skillsoft.assessmentbackend.domain.dto.passport;

import app.skillsoft.assessmentbackend.services.external.PassportService.CompetencyPassport;
import app.skillsoft.assessmentbackend.services.external.PassportService.PassportDetails;

import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Maps PassportService records to API response DTOs.
 * Handles score scale conversion (1-5 -> 0-100) and Big Five key mapping.
 */
public final class PassportDtoMapper {

    private PassportDtoMapper() {}

    private static final double SCALE_MIN = 1.0;
    private static final double SCALE_MAX = 5.0;
    private static final double SCALE_RANGE = SCALE_MAX - SCALE_MIN;

    /**
     * Convert a 1-5 scale score to 0-100 percentage.
     * Values outside [1, 5] are clamped.
     */
    public static double toPercentage(double rawScore) {
        double clamped = Math.max(SCALE_MIN, Math.min(SCALE_MAX, rawScore));
        return Math.round(((clamped - SCALE_MIN) / SCALE_RANGE) * 100.0 * 100.0) / 100.0;
    }

    /**
     * Map PassportDetails to the API response DTO.
     */
    public static CompetencyPassportDto toDto(PassportDetails details) {
        CompetencyPassport passport = details.passport();

        Map<String, Double> convertedScores = passport.competencyScores().entrySet().stream()
            .collect(Collectors.toMap(
                e -> e.getKey().toString(),
                e -> toPercentage(e.getValue())
            ));

        BigFiveProfileDto bigFive = mapBigFive(passport.bigFiveProfile());

        return new CompetencyPassportDto(
            details.entityId() != null ? details.entityId().toString() : null,
            passport.candidateId() != null ? passport.candidateId().toString() : null,
            details.clerkUserId(),
            passport.lastAssessed() != null
                ? passport.lastAssessed().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : null,
            convertedScores,
            bigFive,
            passport.isValid(),
            details.expiresAt() != null
                ? details.expiresAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : null
        );
    }

    private static BigFiveProfileDto mapBigFive(Map<String, Double> raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        return new BigFiveProfileDto(
            toPercentage(raw.getOrDefault("OPENNESS", SCALE_MIN)),
            toPercentage(raw.getOrDefault("CONSCIENTIOUSNESS", SCALE_MIN)),
            toPercentage(raw.getOrDefault("EXTRAVERSION", SCALE_MIN)),
            toPercentage(raw.getOrDefault("AGREEABLENESS", SCALE_MIN)),
            100.0 - toPercentage(raw.getOrDefault("NEUROTICISM", SCALE_MIN))
        );
    }
}
