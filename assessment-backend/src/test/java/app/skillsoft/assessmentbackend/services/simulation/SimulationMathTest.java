package app.skillsoft.assessmentbackend.services.simulation;

import app.skillsoft.assessmentbackend.domain.dto.simulation.SimulationProfile;
import app.skillsoft.assessmentbackend.domain.entities.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("SimulationMath utility")
class SimulationMathTest {

    @Nested
    @DisplayName("applyLogitShift")
    class LogitShiftTests {

        @Test
        @DisplayName("Zero shift should return approximately the original probability")
        void zeroShiftShouldReturnOriginal() {
            assertThat(SimulationMath.applyLogitShift(0.5, 0.0)).isCloseTo(0.5, within(0.001));
            assertThat(SimulationMath.applyLogitShift(0.8, 0.0)).isCloseTo(0.8, within(0.001));
            assertThat(SimulationMath.applyLogitShift(0.2, 0.0)).isCloseTo(0.2, within(0.001));
            assertThat(SimulationMath.applyLogitShift(0.95, 0.0)).isCloseTo(0.95, within(0.001));
        }

        @Test
        @DisplayName("Positive shift should increase probability")
        void positiveShiftShouldIncrease() {
            assertThat(SimulationMath.applyLogitShift(0.5, 1.0)).isGreaterThan(0.5);
            assertThat(SimulationMath.applyLogitShift(0.3, 0.5)).isGreaterThan(0.3);
            assertThat(SimulationMath.applyLogitShift(0.7, 2.0)).isGreaterThan(0.7);
        }

        @Test
        @DisplayName("Negative shift should decrease probability")
        void negativeShiftShouldDecrease() {
            assertThat(SimulationMath.applyLogitShift(0.5, -1.0)).isLessThan(0.5);
            assertThat(SimulationMath.applyLogitShift(0.7, -0.5)).isLessThan(0.7);
            assertThat(SimulationMath.applyLogitShift(0.3, -2.0)).isLessThan(0.3);
        }

        @Test
        @DisplayName("Result should be bounded in [0.01, 0.99]")
        void resultShouldBeBounded() {
            assertThat(SimulationMath.applyLogitShift(0.99, 10.0))
                .isLessThanOrEqualTo(SimulationMath.PROBABILITY_CEILING);
            assertThat(SimulationMath.applyLogitShift(0.01, -10.0))
                .isGreaterThanOrEqualTo(SimulationMath.PROBABILITY_FLOOR);
            assertThat(SimulationMath.applyLogitShift(0.5, 100.0))
                .isLessThanOrEqualTo(SimulationMath.PROBABILITY_CEILING);
            assertThat(SimulationMath.applyLogitShift(0.5, -100.0))
                .isGreaterThanOrEqualTo(SimulationMath.PROBABILITY_FLOOR);
        }

        @Test
        @DisplayName("Extreme input probabilities should be handled safely")
        void extremeInputsShouldBeHandled() {
            // Near-zero and near-one inputs should not cause NaN or infinity
            assertThat(SimulationMath.applyLogitShift(0.001, 1.0)).isFinite();
            assertThat(SimulationMath.applyLogitShift(0.999, -1.0)).isFinite();
            assertThat(SimulationMath.applyLogitShift(0.0, 0.0)).isFinite();
            assertThat(SimulationMath.applyLogitShift(1.0, 0.0)).isFinite();
        }
    }

    @Nested
    @DisplayName("abilityToModifier")
    class AbilityToModifierTests {

        @Test
        @DisplayName("Slider 50 should produce zero modifier")
        void midpointShouldBeZero() {
            assertThat(SimulationMath.abilityToModifier(50)).isCloseTo(0.0, within(0.001));
        }

        @Test
        @DisplayName("Slider 0 should produce -2.0")
        void minimumShouldBeNegativeTwo() {
            assertThat(SimulationMath.abilityToModifier(0)).isCloseTo(-2.0, within(0.001));
        }

        @Test
        @DisplayName("Slider 100 should produce +2.0")
        void maximumShouldBePositiveTwo() {
            assertThat(SimulationMath.abilityToModifier(100)).isCloseTo(2.0, within(0.001));
        }

        @Test
        @DisplayName("Slider 75 should produce +1.0")
        void upperQuartileShouldBeOne() {
            assertThat(SimulationMath.abilityToModifier(75)).isCloseTo(1.0, within(0.001));
        }
    }

    @Nested
    @DisplayName("computeSimulationSeed")
    class SeedTests {

        @Test
        @DisplayName("Same inputs should produce same seed")
        void sameInputsShouldProduceSameSeed() {
            List<AssessmentQuestion> questions = List.of(createTestQuestion());

            long seed1 = SimulationMath.computeSimulationSeed(
                questions, SimulationProfile.RANDOM_GUESSER, 50);
            long seed2 = SimulationMath.computeSimulationSeed(
                questions, SimulationProfile.RANDOM_GUESSER, 50);

            assertThat(seed1).isEqualTo(seed2);
        }

        @Test
        @DisplayName("Different profiles should produce different seeds")
        void differentProfilesShouldProduceDifferentSeeds() {
            List<AssessmentQuestion> questions = List.of(createTestQuestion());

            long seed1 = SimulationMath.computeSimulationSeed(
                questions, SimulationProfile.PERFECT_CANDIDATE, 50);
            long seed2 = SimulationMath.computeSimulationSeed(
                questions, SimulationProfile.FAILING_CANDIDATE, 50);

            assertThat(seed1).isNotEqualTo(seed2);
        }

        @Test
        @DisplayName("Different ability levels should produce different seeds")
        void differentAbilityLevelsShouldProduceDifferentSeeds() {
            List<AssessmentQuestion> questions = List.of(createTestQuestion());

            long seed1 = SimulationMath.computeSimulationSeed(
                questions, SimulationProfile.RANDOM_GUESSER, 25);
            long seed2 = SimulationMath.computeSimulationSeed(
                questions, SimulationProfile.RANDOM_GUESSER, 75);

            assertThat(seed1).isNotEqualTo(seed2);
        }
    }

    @Nested
    @DisplayName("computeCompetencyNoise")
    class CompetencyNoiseTests {

        @Test
        @DisplayName("Should generate noise for each unique competency")
        void shouldGenerateNoiseForEachCompetency() {
            UUID compA = UUID.randomUUID();
            UUID compB = UUID.randomUUID();

            List<AssessmentQuestion> questions = List.of(
                createQuestionForCompetency(compA),
                createQuestionForCompetency(compB)
            );

            var noise = SimulationMath.computeCompetencyNoise(questions, 42L);

            assertThat(noise).hasSize(2);
            assertThat(noise).containsKeys(compA, compB);
        }

        @Test
        @DisplayName("Noise values should be within amplitude bounds")
        void noiseShouldBeWithinBounds() {
            List<AssessmentQuestion> questions = List.of(
                createQuestionForCompetency(UUID.randomUUID()),
                createQuestionForCompetency(UUID.randomUUID()),
                createQuestionForCompetency(UUID.randomUUID())
            );

            var noise = SimulationMath.computeCompetencyNoise(questions, 42L);

            for (double n : noise.values()) {
                assertThat(n).isBetween(
                    -SimulationMath.COMPETENCY_NOISE_AMPLITUDE,
                    SimulationMath.COMPETENCY_NOISE_AMPLITUDE
                );
            }
        }

        @Test
        @DisplayName("Same seed should produce same noise")
        void sameSeedShouldProduceSameNoise() {
            UUID compId = UUID.randomUUID();
            List<AssessmentQuestion> questions = List.of(createQuestionForCompetency(compId));

            var noise1 = SimulationMath.computeCompetencyNoise(questions, 42L);
            var noise2 = SimulationMath.computeCompetencyNoise(questions, 42L);

            assertThat(noise1.get(compId)).isEqualTo(noise2.get(compId));
        }
    }

    // ==================== Helpers ====================

    private AssessmentQuestion createTestQuestion() {
        return createQuestionForCompetency(UUID.randomUUID());
    }

    private AssessmentQuestion createQuestionForCompetency(UUID competencyId) {
        Competency competency = new Competency();
        competency.setId(competencyId);
        competency.setName("Test Competency");

        BehavioralIndicator indicator = new BehavioralIndicator();
        indicator.setId(UUID.randomUUID());
        indicator.setTitle("Test Indicator");
        indicator.setCompetency(competency);

        AssessmentQuestion question = new AssessmentQuestion();
        question.setId(UUID.randomUUID());
        question.setBehavioralIndicator(indicator);
        question.setQuestionText("Test question");
        question.setQuestionType(QuestionType.MCQ);
        question.setDifficultyLevel(DifficultyLevel.INTERMEDIATE);
        question.setTimeLimit(60);
        question.setActive(true);

        return question;
    }
}
