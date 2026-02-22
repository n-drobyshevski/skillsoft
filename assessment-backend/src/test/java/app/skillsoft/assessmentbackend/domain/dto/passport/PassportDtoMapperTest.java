package app.skillsoft.assessmentbackend.domain.dto.passport;

import app.skillsoft.assessmentbackend.services.external.PassportService.CompetencyPassport;
import app.skillsoft.assessmentbackend.services.external.PassportService.PassportDetails;
import app.skillsoft.assessmentbackend.testutil.PassportTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for PassportDtoMapper.
 *
 * Covers scale conversion (toPercentage) and full DTO mapping (toDto),
 * including Big Five profile mapping and null/empty edge cases.
 * Pure JUnit 5 — no Spring context required.
 */
@DisplayName("PassportDtoMapper Tests")
class PassportDtoMapperTest {

    // --- toPercentage ---

    @Nested
    @DisplayName("toPercentage — scale conversion (1-5 → 0-100)")
    class ToPercentageTests {

        @Test
        @DisplayName("At minimum (1.0) returns 0.0")
        void toPercentage_atMinimum_returnsZero() {
            double result = PassportDtoMapper.toPercentage(1.0);

            assertThat(result).isEqualTo(0.0);
        }

        @Test
        @DisplayName("At midpoint (3.0) returns 50.0")
        void toPercentage_atMidpoint_returnsFifty() {
            double result = PassportDtoMapper.toPercentage(3.0);

            assertThat(result).isEqualTo(50.0);
        }

        @Test
        @DisplayName("At maximum (5.0) returns 100.0")
        void toPercentage_atMaximum_returnsHundred() {
            double result = PassportDtoMapper.toPercentage(5.0);

            assertThat(result).isEqualTo(100.0);
        }

        @Test
        @DisplayName("Intermediate value (4.2) returns 80.0")
        void toPercentage_intermediateValue_correctConversion() {
            // (4.2 - 1.0) / (5.0 - 1.0) * 100 = 3.2 / 4.0 * 100 = 80.0
            double result = PassportDtoMapper.toPercentage(4.2);

            assertThat(result).isCloseTo(80.0, within(0.01));
        }

        @Test
        @DisplayName("Below minimum (0.5) clamps to 0.0")
        void toPercentage_belowMinimum_clampsToZero() {
            double result = PassportDtoMapper.toPercentage(0.5);

            assertThat(result).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Above maximum (5.5) clamps to 100.0")
        void toPercentage_aboveMaximum_clampsToHundred() {
            double result = PassportDtoMapper.toPercentage(5.5);

            assertThat(result).isEqualTo(100.0);
        }
    }

    // --- toDto ---

    @Nested
    @DisplayName("toDto — full DTO mapping")
    class ToDtoTests {

        private static final UUID ENTITY_ID = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

        /**
         * Build a PassportDetails wrapping the given passport record.
         */
        private PassportDetails buildDetails(CompetencyPassport passport) {
            return new PassportDetails(
                    ENTITY_ID,
                    passport,
                    PassportTestFixtures.CLERK_USER_ID,
                    LocalDateTime.now().plusDays(180)
            );
        }

        @Test
        @DisplayName("Converts competency scores and Big Five to 0-100 scale")
        void toDto_convertsScoresAndBigFive() {
            // Given — DEFAULT_SCORES: COMP_1=4.2, COMP_2=3.5, COMP_3=2.8
            //         DEFAULT_BIG_FIVE: OPENNESS=3.5, CONSCIENTIOUSNESS=4.0,
            //                          EXTRAVERSION=3.2, AGREEABLENESS=3.8, NEUROTICISM=2.5
            PassportDetails details = buildDetails(PassportTestFixtures.createPassport());

            // When
            CompetencyPassportDto dto = PassportDtoMapper.toDto(details);

            // Then — competency scores (1-5 → 0-100)
            String comp1Key = PassportTestFixtures.COMPETENCY_1.toString();
            String comp2Key = PassportTestFixtures.COMPETENCY_2.toString();
            String comp3Key = PassportTestFixtures.COMPETENCY_3.toString();

            assertThat(dto.scores()).containsKey(comp1Key);
            assertThat(dto.scores()).containsKey(comp2Key);
            assertThat(dto.scores()).containsKey(comp3Key);

            // COMPETENCY_1: (4.2 - 1) / 4 * 100 = 80.0
            assertThat(dto.scores().get(comp1Key)).isCloseTo(80.0, within(0.01));
            // COMPETENCY_2: (3.5 - 1) / 4 * 100 = 62.5
            assertThat(dto.scores().get(comp2Key)).isCloseTo(62.5, within(0.01));
            // COMPETENCY_3: (2.8 - 1) / 4 * 100 = 45.0
            assertThat(dto.scores().get(comp3Key)).isCloseTo(45.0, within(0.01));

            // Big Five — openness: (3.5 - 1) / 4 * 100 = 62.5
            assertThat(dto.bigFiveProfile()).isNotNull();
            assertThat(dto.bigFiveProfile().openness()).isCloseTo(62.5, within(0.01));

            // conscientiousness: (4.0 - 1) / 4 * 100 = 75.0
            assertThat(dto.bigFiveProfile().conscientiousness()).isCloseTo(75.0, within(0.01));

            // emotionalStability = 100 - toPercentage(NEUROTICISM=2.5)
            // toPercentage(2.5) = (2.5 - 1) / 4 * 100 = 37.5
            // emotionalStability = 100 - 37.5 = 62.5
            assertThat(dto.bigFiveProfile().emotionalStability()).isCloseTo(62.5, within(0.01));
        }

        @Test
        @DisplayName("Null Big Five profile produces null bigFiveProfile in DTO")
        void toDto_nullBigFive_returnsNullBigFiveDto() {
            // Given — passport constructed with null bigFiveProfile
            CompetencyPassport passportWithNullBigFive = new CompetencyPassport(
                    PassportTestFixtures.CANDIDATE_ID,
                    PassportTestFixtures.DEFAULT_SCORES,
                    null,
                    LocalDateTime.now(),
                    true
            );
            PassportDetails details = buildDetails(passportWithNullBigFive);

            // When
            CompetencyPassportDto dto = PassportDtoMapper.toDto(details);

            // Then
            assertThat(dto.bigFiveProfile()).isNull();
        }

        @Test
        @DisplayName("Empty Big Five map produces null bigFiveProfile in DTO")
        void toDto_emptyBigFive_returnsNullBigFiveDto() {
            // Given — passport constructed with an empty bigFiveProfile map
            CompetencyPassport passportWithEmptyBigFive = new CompetencyPassport(
                    PassportTestFixtures.CANDIDATE_ID,
                    PassportTestFixtures.DEFAULT_SCORES,
                    Collections.emptyMap(),
                    LocalDateTime.now(),
                    true
            );
            PassportDetails details = buildDetails(passportWithEmptyBigFive);

            // When
            CompetencyPassportDto dto = PassportDtoMapper.toDto(details);

            // Then
            assertThat(dto.bigFiveProfile()).isNull();
        }
    }
}
