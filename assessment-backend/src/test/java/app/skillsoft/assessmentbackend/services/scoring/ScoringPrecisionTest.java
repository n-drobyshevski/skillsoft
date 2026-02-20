package app.skillsoft.assessmentbackend.services.scoring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for ScoringPrecision utility.
 *
 * Verifies that floating-point rounding eliminates IEEE 754 boundary errors
 * while preserving meaningful precision for psychometric scoring.
 */
@DisplayName("Scoring Precision Utility Tests")
class ScoringPrecisionTest {

    @Nested
    @DisplayName("round4 Tests")
    class Round4Tests {

        @ParameterizedTest(name = "round4({0}) should be {1}")
        @CsvSource({
            "0.49995, 0.5",
            "0.49994, 0.4999",
            "0.50000000001, 0.5",
            "0.49999999999, 0.5",
            "0.64999999999, 0.65",
            "0.65000000001, 0.65",
            "0.0, 0.0",
            "1.0, 1.0",
            "0.12345, 0.1235",
            "0.12344, 0.1234",
            "0.99995, 1.0",
            "0.99994, 0.9999"
        })
        @DisplayName("Should round to 4 decimal places using HALF_UP")
        void shouldRoundTo4DecimalPlaces(double input, double expected) {
            assertThat(ScoringPrecision.round4(input)).isCloseTo(expected, within(1e-10));
        }

        @Test
        @DisplayName("Should handle negative values")
        void shouldHandleNegativeValues() {
            assertThat(ScoringPrecision.round4(-0.49995)).isCloseTo(-0.5, within(1e-10));
            assertThat(ScoringPrecision.round4(-0.49994)).isCloseTo(-0.4999, within(1e-10));
        }

        @Test
        @DisplayName("Should handle exact IEEE 754 representable values")
        void shouldHandleExactValues() {
            assertThat(ScoringPrecision.round4(0.5)).isCloseTo(0.5, within(1e-10));
            assertThat(ScoringPrecision.round4(0.25)).isCloseTo(0.25, within(1e-10));
            assertThat(ScoringPrecision.round4(0.75)).isCloseTo(0.75, within(1e-10));
        }
    }

    @Nested
    @DisplayName("meetsThreshold Tests")
    class MeetsThresholdTests {

        @Test
        @DisplayName("Should return true when value exactly equals threshold")
        void shouldReturnTrueWhenExactlyEqual() {
            assertThat(ScoringPrecision.meetsThreshold(0.5, 0.5)).isTrue();
            assertThat(ScoringPrecision.meetsThreshold(0.65, 0.65)).isTrue();
        }

        @Test
        @DisplayName("Should return true when value exceeds threshold")
        void shouldReturnTrueWhenExceeds() {
            assertThat(ScoringPrecision.meetsThreshold(0.75, 0.5)).isTrue();
            assertThat(ScoringPrecision.meetsThreshold(1.0, 0.65)).isTrue();
        }

        @Test
        @DisplayName("Should return false when value is below threshold")
        void shouldReturnFalseWhenBelow() {
            assertThat(ScoringPrecision.meetsThreshold(0.4999, 0.5)).isFalse();
            assertThat(ScoringPrecision.meetsThreshold(0.6499, 0.65)).isFalse();
        }

        @Test
        @DisplayName("Should handle IEEE 754 boundary: 0.49999999999 vs 0.5 threshold")
        void shouldHandleIeee754BoundaryJustBelow() {
            // Without rounding, 0.49999999999 < 0.5 could be ambiguous
            // After rounding to 4dp: 0.5 >= 0.5 => true
            assertThat(ScoringPrecision.meetsThreshold(0.49999999999, 0.5)).isTrue();
        }

        @Test
        @DisplayName("Should handle IEEE 754 boundary: 0.50000000001 vs 0.5 threshold")
        void shouldHandleIeee754BoundaryJustAbove() {
            // 0.50000000001 rounds to 0.5, 0.5 rounds to 0.5 => true
            assertThat(ScoringPrecision.meetsThreshold(0.50000000001, 0.5)).isTrue();
        }

        @Test
        @DisplayName("Should correctly fail when value is meaningfully below threshold")
        void shouldFailWhenMeaningfullyBelow() {
            // 0.4994 rounds to 0.4994, 0.5 rounds to 0.5 => false
            assertThat(ScoringPrecision.meetsThreshold(0.4994, 0.5)).isFalse();
        }

        @Test
        @DisplayName("Should handle the 0.49995 boundary correctly (rounds up to 0.5)")
        void shouldHandleMidpointRounding() {
            // 0.49995 rounds to 0.5 (HALF_UP), threshold 0.5 => true
            assertThat(ScoringPrecision.meetsThreshold(0.49995, 0.5)).isTrue();
            // 0.49994 rounds to 0.4999, threshold 0.5 => false
            assertThat(ScoringPrecision.meetsThreshold(0.49994, 0.5)).isFalse();
        }

        @Test
        @DisplayName("Should handle zero values")
        void shouldHandleZeroValues() {
            assertThat(ScoringPrecision.meetsThreshold(0.0, 0.0)).isTrue();
            assertThat(ScoringPrecision.meetsThreshold(0.0, 0.5)).isFalse();
            assertThat(ScoringPrecision.meetsThreshold(0.5, 0.0)).isTrue();
        }
    }
}
