package app.skillsoft.assessmentbackend.testutil;

import app.skillsoft.assessmentbackend.domain.entities.CompetencyPassportEntity;
import app.skillsoft.assessmentbackend.services.external.PassportService.CompetencyPassport;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Test fixtures for Competency Passport tests.
 */
public final class PassportTestFixtures {

    public static final String CLERK_USER_ID = "user_test_abc123";
    public static final UUID CANDIDATE_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    public static final UUID SOURCE_RESULT_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    public static final UUID COMPETENCY_1 = UUID.fromString("c0000001-0000-0000-0000-000000000001");
    public static final UUID COMPETENCY_2 = UUID.fromString("c0000001-0000-0000-0000-000000000002");
    public static final UUID COMPETENCY_3 = UUID.fromString("c0000001-0000-0000-0000-000000000003");

    public static final Map<UUID, Double> DEFAULT_SCORES = Map.of(
        COMPETENCY_1, 4.2,
        COMPETENCY_2, 3.5,
        COMPETENCY_3, 2.8
    );

    public static final Map<String, Double> DEFAULT_BIG_FIVE = Map.of(
        "OPENNESS", 3.5,
        "CONSCIENTIOUSNESS", 4.0,
        "EXTRAVERSION", 3.2,
        "AGREEABLENESS", 3.8,
        "NEUROTICISM", 2.5
    );

    private PassportTestFixtures() {
    }

    /**
     * Create a valid passport record (interface DTO).
     */
    public static CompetencyPassport createPassport() {
        return new CompetencyPassport(
            CANDIDATE_ID,
            DEFAULT_SCORES,
            DEFAULT_BIG_FIVE,
            LocalDateTime.now(),
            true
        );
    }

    /**
     * Create an expired passport record (interface DTO).
     */
    public static CompetencyPassport createExpiredPassport() {
        return new CompetencyPassport(
            CANDIDATE_ID,
            DEFAULT_SCORES,
            DEFAULT_BIG_FIVE,
            LocalDateTime.now().minusDays(365),
            false
        );
    }

    /**
     * Create a valid passport entity (JPA).
     */
    public static CompetencyPassportEntity createEntity() {
        return createEntity(180);
    }

    /**
     * Create a valid passport entity with configurable validity days.
     */
    public static CompetencyPassportEntity createEntity(int validityDays) {
        CompetencyPassportEntity entity = new CompetencyPassportEntity();
        entity.setId(UUID.randomUUID());
        entity.setVersion(0L);
        entity.setClerkUserId(CLERK_USER_ID);
        entity.setCompetencyScores(toStringKeyedScores(DEFAULT_SCORES));
        entity.setBigFiveProfile(DEFAULT_BIG_FIVE);
        entity.setLastAssessed(LocalDateTime.now());
        entity.setExpiresAt(LocalDateTime.now().plusDays(validityDays));
        entity.setSourceResultId(SOURCE_RESULT_ID);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }

    /**
     * Create an expired passport entity.
     */
    public static CompetencyPassportEntity createExpiredEntity() {
        CompetencyPassportEntity entity = createEntity();
        entity.setLastAssessed(LocalDateTime.now().minusDays(365));
        entity.setExpiresAt(LocalDateTime.now().minusDays(185));
        return entity;
    }

    /**
     * Convert UUID-keyed map to String-keyed map (as stored in JSONB).
     */
    public static Map<String, Double> toStringKeyedScores(Map<UUID, Double> scores) {
        return scores.entrySet().stream()
            .collect(Collectors.toMap(
                e -> e.getKey().toString(),
                Map.Entry::getValue
            ));
    }
}
