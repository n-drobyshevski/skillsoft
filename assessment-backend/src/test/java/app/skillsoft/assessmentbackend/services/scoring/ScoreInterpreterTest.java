package app.skillsoft.assessmentbackend.services.scoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ScoreInterpreter bilingual proficiency classification.
 *
 * Tests cover:
 * - All 5 proficiency levels with boundary values
 * - English and Russian locale support
 * - Null and unsupported locale fallback to English
 * - Edge cases at exact boundaries
 */
@DisplayName("ScoreInterpreter Tests")
class ScoreInterpreterTest {

    private ScoreInterpreter scoreInterpreter;

    @BeforeEach
    void setUp() {
        scoreInterpreter = new ScoreInterpreter();
    }

    @Nested
    @DisplayName("English Locale Tests")
    class EnglishLocaleTests {

        @ParameterizedTest(name = "Score {0}% should be \"{1}\" ({2})")
        @CsvSource({
            "0.0, Beginning, BEGINNING",
            "15.0, Beginning, BEGINNING",
            "29.9, Beginning, BEGINNING",
            "30.0, Developing, DEVELOPING",
            "45.0, Developing, DEVELOPING",
            "49.9, Developing, DEVELOPING",
            "50.0, Proficient, PROFICIENT",
            "60.0, Proficient, PROFICIENT",
            "69.9, Proficient, PROFICIENT",
            "70.0, Advanced, ADVANCED",
            "75.0, Advanced, ADVANCED",
            "84.9, Advanced, ADVANCED",
            "85.0, Expert, EXPERT",
            "92.0, Expert, EXPERT",
            "100.0, Expert, EXPERT"
        })
        @DisplayName("Should classify score correctly in English")
        void shouldClassifyInEnglish(double percentage, String expectedLabel, String expectedLevel) {
            // When
            ScoreInterpreter.ScoreInterpretation result = scoreInterpreter.interpret(percentage, "en");

            // Then
            assertThat(result.label()).isEqualTo(expectedLabel);
            assertThat(result.level()).isEqualTo(expectedLevel);
        }
    }

    @Nested
    @DisplayName("Russian Locale Tests")
    class RussianLocaleTests {

        @ParameterizedTest(name = "Score {0}% should be \"{1}\" (ru)")
        @CsvSource({
            "0.0, Начальный",
            "29.9, Начальный",
            "30.0, Развивающийся",
            "49.9, Развивающийся",
            "50.0, Компетентный",
            "69.9, Компетентный",
            "70.0, Опытный",
            "84.9, Опытный",
            "85.0, Эксперт",
            "100.0, Эксперт"
        })
        @DisplayName("Should classify score correctly in Russian")
        void shouldClassifyInRussian(double percentage, String expectedLabel) {
            // When
            ScoreInterpreter.ScoreInterpretation result = scoreInterpreter.interpret(percentage, "ru");

            // Then
            assertThat(result.label()).isEqualTo(expectedLabel);
        }
    }

    @Nested
    @DisplayName("Locale Fallback Tests")
    class LocaleFallbackTests {

        @Test
        @DisplayName("Should default to English when locale is null")
        void shouldDefaultToEnglishWhenLocaleNull() {
            // When
            ScoreInterpreter.ScoreInterpretation result = scoreInterpreter.interpret(85.0, null);

            // Then
            assertThat(result.label()).isEqualTo("Expert");
            assertThat(result.level()).isEqualTo("EXPERT");
        }

        @Test
        @DisplayName("Should default to English for unsupported locale")
        void shouldDefaultToEnglishForUnsupportedLocale() {
            // When
            ScoreInterpreter.ScoreInterpretation result = scoreInterpreter.interpret(50.0, "fr");

            // Then
            assertThat(result.label()).isEqualTo("Proficient");
        }

        @Test
        @DisplayName("Should handle uppercase locale")
        void shouldHandleUppercaseLocale() {
            // When
            ScoreInterpreter.ScoreInterpretation result = scoreInterpreter.interpret(70.0, "RU");

            // Then
            assertThat(result.label()).isEqualTo("Опытный");
        }
    }

    @Nested
    @DisplayName("Boundary Tests")
    class BoundaryTests {

        @Test
        @DisplayName("Should classify exactly 30% as Developing, not Beginning")
        void shouldClassifyExact30AsDeveloping() {
            ScoreInterpreter.ScoreInterpretation result = scoreInterpreter.interpret(30.0, "en");
            assertThat(result.level()).isEqualTo("DEVELOPING");
        }

        @Test
        @DisplayName("Should classify exactly 50% as Proficient")
        void shouldClassifyExact50AsProficient() {
            ScoreInterpreter.ScoreInterpretation result = scoreInterpreter.interpret(50.0, "en");
            assertThat(result.level()).isEqualTo("PROFICIENT");
        }

        @Test
        @DisplayName("Should classify exactly 70% as Advanced")
        void shouldClassifyExact70AsAdvanced() {
            ScoreInterpreter.ScoreInterpretation result = scoreInterpreter.interpret(70.0, "en");
            assertThat(result.level()).isEqualTo("ADVANCED");
        }

        @Test
        @DisplayName("Should classify exactly 85% as Expert")
        void shouldClassifyExact85AsExpert() {
            ScoreInterpreter.ScoreInterpretation result = scoreInterpreter.interpret(85.0, "en");
            assertThat(result.level()).isEqualTo("EXPERT");
        }
    }
}
