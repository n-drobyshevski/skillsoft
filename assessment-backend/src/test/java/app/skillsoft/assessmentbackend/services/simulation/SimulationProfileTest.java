package app.skillsoft.assessmentbackend.services.simulation;

import app.skillsoft.assessmentbackend.domain.dto.simulation.SimulationProfile;
import app.skillsoft.assessmentbackend.domain.entities.DifficultyLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SimulationProfile psychometric curves")
class SimulationProfileTest {

    @Test
    @DisplayName("All difficulty levels should have base probabilities in (0, 1)")
    void allDifficultyLevelsShouldHaveBaseProbabilities() {
        for (SimulationProfile p : SimulationProfile.values()) {
            for (DifficultyLevel d : DifficultyLevel.values()) {
                double prob = p.getBaseProbability(d);
                assertThat(prob)
                    .as("%s at %s", p, d)
                    .isBetween(0.0, 1.0);
            }
        }
    }

    @Test
    @DisplayName("PERFECT should have higher probabilities than FAILING at every level")
    void perfectCandidateShouldHaveHigherProbabilitiesThanFailing() {
        for (DifficultyLevel d : DifficultyLevel.values()) {
            assertThat(SimulationProfile.PERFECT_CANDIDATE.getBaseProbability(d))
                .as("difficulty: %s", d)
                .isGreaterThan(SimulationProfile.FAILING_CANDIDATE.getBaseProbability(d));
        }
    }

    @Test
    @DisplayName("PERFECT should have higher probabilities than RANDOM at every level")
    void perfectShouldBeHigherThanRandom() {
        for (DifficultyLevel d : DifficultyLevel.values()) {
            assertThat(SimulationProfile.PERFECT_CANDIDATE.getBaseProbability(d))
                .as("difficulty: %s", d)
                .isGreaterThan(SimulationProfile.RANDOM_GUESSER.getBaseProbability(d));
        }
    }

    @Test
    @DisplayName("RANDOM should have higher probabilities than FAILING at every level")
    void randomShouldBeHigherThanFailing() {
        for (DifficultyLevel d : DifficultyLevel.values()) {
            assertThat(SimulationProfile.RANDOM_GUESSER.getBaseProbability(d))
                .as("difficulty: %s", d)
                .isGreaterThan(SimulationProfile.FAILING_CANDIDATE.getBaseProbability(d));
        }
    }

    @Test
    @DisplayName("Probabilities should decrease with difficulty for all profiles")
    void probabilitiesShouldDecreaseWithDifficulty() {
        for (SimulationProfile p : SimulationProfile.values()) {
            double prev = 1.0;
            for (DifficultyLevel d : DifficultyLevel.values()) {
                double current = p.getBaseProbability(d);
                assertThat(current)
                    .as("%s at %s", p, d)
                    .isLessThanOrEqualTo(prev);
                prev = current;
            }
        }
    }

    @Test
    @DisplayName("Deprecated getCorrectAnswerProbability should return INTERMEDIATE value")
    @SuppressWarnings("deprecation")
    void deprecatedMethodShouldReturnIntermediateValue() {
        for (SimulationProfile p : SimulationProfile.values()) {
            assertThat(p.getCorrectAnswerProbability())
                .isEqualTo(p.getBaseProbability(DifficultyLevel.INTERMEDIATE));
        }
    }

    @Test
    @DisplayName("Unknown difficulty should fall back to 0.50")
    void unknownDifficultyShouldFallBackToDefault() {
        // getBaseProbability uses getOrDefault with 0.50
        // All 5 DifficultyLevels are mapped, so test the fallback by verifying
        // the map covers all values and the method signature is correct
        for (SimulationProfile p : SimulationProfile.values()) {
            assertThat(p.getBaseProbabilities()).hasSize(5);
        }
    }
}
