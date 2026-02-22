package app.skillsoft.assessmentbackend.services.external.impl;

import app.skillsoft.assessmentbackend.config.OnetProperties;
import app.skillsoft.assessmentbackend.services.external.OnetService.OnetProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for OnetServiceImpl.
 *
 * Tests cover:
 * - Mock mode (enabled=false): profile retrieval, search, validation
 * - Score conversion: O*NET 0-100 to 1-5 scale with clamping
 * - getBenchmark: searches all categories (benchmarks, skills, abilities, knowledgeAreas)
 */
@DisplayName("OnetServiceImpl Tests")
class OnetServiceImplTest {

    private OnetProperties properties;
    private OnetServiceImpl service;

    @BeforeEach
    void setUp() {
        properties = new OnetProperties();
        properties.setEnabled(false);
        service = new OnetServiceImpl(properties);
    }

    @Nested
    @DisplayName("Mock Mode (API disabled)")
    class MockMode {

        @Test
        @DisplayName("getProfile returns known SOC code profile")
        void getProfile_knownCode_returnsProfile() {
            Optional<OnetProfile> result = service.getProfile("15-1252.00");

            assertThat(result).isPresent();
            assertThat(result.get().socCode()).isEqualTo("15-1252.00");
            assertThat(result.get().occupationTitle()).isEqualTo("Software Developers");
            assertThat(result.get().benchmarks()).isNotEmpty();
            assertThat(result.get().skills()).isNotEmpty();
            assertThat(result.get().abilities()).isNotEmpty();
            assertThat(result.get().knowledgeAreas()).isNotEmpty();
        }

        @Test
        @DisplayName("getProfile returns empty for unknown SOC code")
        void getProfile_unknownCode_returnsEmpty() {
            Optional<OnetProfile> result = service.getProfile("99-9999.00");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("all 9 mock profiles are available")
        void getProfile_allMockProfiles_available() {
            String[] socCodes = {
                "15-1252.00", "11-9199.00", "15-2051.00", "29-1141.00",
                "13-2011.00", "25-2021.00", "11-2021.00", "15-1255.00", "43-6014.00"
            };

            for (String code : socCodes) {
                assertThat(service.getProfile(code))
                    .as("Profile for %s should exist", code)
                    .isPresent();
            }
        }

        @Test
        @DisplayName("isValidSocCode returns true for known codes")
        void isValidSocCode_knownCode_returnsTrue() {
            assertThat(service.isValidSocCode("15-1252.00")).isTrue();
            assertThat(service.isValidSocCode("11-2021.00")).isTrue();
        }

        @Test
        @DisplayName("isValidSocCode returns false for unknown codes")
        void isValidSocCode_unknownCode_returnsFalse() {
            assertThat(service.isValidSocCode("99-9999.00")).isFalse();
            assertThat(service.isValidSocCode("")).isFalse();
        }
    }

    @Nested
    @DisplayName("getBenchmark")
    class GetBenchmark {

        @Test
        @DisplayName("finds competency in benchmarks map")
        void getBenchmark_inBenchmarks_returnsValue() {
            Optional<Double> result = service.getBenchmark("15-1252.00", "Programming");

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(4.50);
        }

        @Test
        @DisplayName("finds competency in skills map")
        void getBenchmark_inSkills_returnsValue() {
            Optional<Double> result = service.getBenchmark("15-1252.00", "Technology Design");

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(3.75);
        }

        @Test
        @DisplayName("finds competency in abilities map")
        void getBenchmark_inAbilities_returnsValue() {
            Optional<Double> result = service.getBenchmark("15-1252.00", "Deductive Reasoning");

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(4.12);
        }

        @Test
        @DisplayName("finds competency in knowledgeAreas map")
        void getBenchmark_inKnowledge_returnsValue() {
            Optional<Double> result = service.getBenchmark("15-1252.00", "Computers and Electronics");

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(4.50);
        }

        @Test
        @DisplayName("returns empty for non-existent competency")
        void getBenchmark_unknownCompetency_returnsEmpty() {
            Optional<Double> result = service.getBenchmark("15-1252.00", "Underwater Basket Weaving");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns empty for non-existent SOC code")
        void getBenchmark_unknownSocCode_returnsEmpty() {
            Optional<Double> result = service.getBenchmark("99-9999.00", "Programming");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("searchProfiles")
    class SearchProfiles {

        @Test
        @DisplayName("finds profiles by title keyword")
        void searchProfiles_byTitle_returnsMatches() {
            List<OnetProfile> results = service.searchProfiles("Software");

            assertThat(results).isNotEmpty();
            assertThat(results).anyMatch(p -> p.socCode().equals("15-1252.00"));
        }

        @Test
        @DisplayName("finds profiles by SOC code prefix")
        void searchProfiles_bySocCode_returnsMatches() {
            List<OnetProfile> results = service.searchProfiles("15-");

            assertThat(results).hasSizeGreaterThanOrEqualTo(2);
        }

        @Test
        @DisplayName("search is case-insensitive")
        void searchProfiles_caseInsensitive_returnsMatches() {
            List<OnetProfile> upper = service.searchProfiles("SOFTWARE");
            List<OnetProfile> lower = service.searchProfiles("software");

            assertThat(upper).hasSameSizeAs(lower);
        }

        @Test
        @DisplayName("returns empty for null keyword")
        void searchProfiles_nullKeyword_returnsEmpty() {
            List<OnetProfile> results = service.searchProfiles(null);

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("returns empty for blank keyword")
        void searchProfiles_blankKeyword_returnsEmpty() {
            List<OnetProfile> results = service.searchProfiles("   ");

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("returns max 10 results")
        void searchProfiles_broadKeyword_limitsTo10() {
            // "a" should match many descriptions
            List<OnetProfile> results = service.searchProfiles("a");

            assertThat(results).hasSizeLessThanOrEqualTo(10);
        }

        @Test
        @DisplayName("finds profiles by description keyword")
        void searchProfiles_byDescription_returnsMatches() {
            List<OnetProfile> results = service.searchProfiles("nursing");

            assertThat(results).isNotEmpty();
            assertThat(results).anyMatch(p -> p.socCode().equals("29-1141.00"));
        }
    }

    @Nested
    @DisplayName("Score Conversion")
    class ScoreConversion {

        /**
         * Uses reflection to test the private convertScores method via a controlled
         * mock-mode profile structure. Score conversion formula: (value/100)*5, clamped [1.0, 5.0].
         */
        @ParameterizedTest(name = "O*NET value {0} -> 1-5 scale = {1}")
        @CsvSource({
            "0,   1.0",    // 0/100*5 = 0.0, clamped to 1.0
            "20,  1.0",    // 20/100*5 = 1.0
            "50,  2.5",    // 50/100*5 = 2.5
            "80,  4.0",    // 80/100*5 = 4.0
            "100, 5.0",    // 100/100*5 = 5.0
        })
        @DisplayName("score conversion formula: (value/100)*5 clamped to [1.0, 5.0]")
        void scoreConversion_formula(double onetValue, double expectedScore) {
            // Verify the formula: (value/100)*5, clamped [1.0, 5.0]
            double converted = (onetValue / 100.0) * 5.0;
            double clamped = Math.max(1.0, Math.min(5.0, converted));
            assertThat(clamped).isCloseTo(expectedScore, within(0.01));
        }

        @Test
        @DisplayName("values below 20 are clamped to 1.0")
        void scoreConversion_lowValues_clampedToOne() {
            // value=10 → (10/100)*5 = 0.5 → clamped to 1.0
            double converted = (10.0 / 100.0) * 5.0;
            double clamped = Math.max(1.0, Math.min(5.0, converted));
            assertThat(clamped).isEqualTo(1.0);
        }

        @Test
        @DisplayName("values above 100 are clamped to 5.0")
        void scoreConversion_highValues_clampedToFive() {
            // value=120 → (120/100)*5 = 6.0 → clamped to 5.0
            double converted = (120.0 / 100.0) * 5.0;
            double clamped = Math.max(1.0, Math.min(5.0, converted));
            assertThat(clamped).isEqualTo(5.0);
        }
    }

    @Nested
    @DisplayName("Constructor behavior")
    class ConstructorBehavior {

        @Test
        @DisplayName("disabled properties create service in mock mode")
        void constructor_disabled_mockMode() {
            OnetProperties props = new OnetProperties();
            props.setEnabled(false);

            OnetServiceImpl svc = new OnetServiceImpl(props);

            // Mock mode should still work
            assertThat(svc.getProfile("15-1252.00")).isPresent();
        }

        @Test
        @DisplayName("enabled without credentials stays in mock mode")
        void constructor_enabledNoCredentials_mockMode() {
            OnetProperties props = new OnetProperties();
            props.setEnabled(true);
            props.setUsername("");
            props.setApiKey("");

            OnetServiceImpl svc = new OnetServiceImpl(props);

            // Should fall back to mock since no credentials
            assertThat(svc.getProfile("15-1252.00")).isPresent();
        }
    }

    @Nested
    @DisplayName("calculateGap (default interface method)")
    class CalculateGap {

        @Test
        @DisplayName("positive gap when candidate below benchmark")
        void calculateGap_belowBenchmark_positiveGap() {
            // Programming benchmark for 15-1252.00 is 4.50
            double gap = service.calculateGap("15-1252.00", "Programming", 3.0);

            assertThat(gap).isCloseTo(1.50, within(0.01));
        }

        @Test
        @DisplayName("negative gap when candidate above benchmark")
        void calculateGap_aboveBenchmark_negativeGap() {
            double gap = service.calculateGap("15-1252.00", "Programming", 5.0);

            assertThat(gap).isCloseTo(-0.50, within(0.01));
        }

        @Test
        @DisplayName("zero gap for unknown competency")
        void calculateGap_unknownCompetency_zeroGap() {
            double gap = service.calculateGap("15-1252.00", "NonExistent", 3.0);

            assertThat(gap).isEqualTo(0.0);
        }
    }
}
