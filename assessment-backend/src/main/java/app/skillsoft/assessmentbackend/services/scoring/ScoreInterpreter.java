package app.skillsoft.assessmentbackend.services.scoring;

import org.springframework.stereotype.Component;

/**
 * Bilingual score interpreter that maps percentage scores to proficiency levels.
 *
 * Supports English ("en") and Russian ("ru") locales with a 5-level scale:
 * - 0-29:  Beginning / Начальный
 * - 30-49: Developing / Развивающийся
 * - 50-69: Proficient / Компетентный
 * - 70-84: Advanced / Опытный
 * - 85-100: Expert / Эксперт
 *
 * Per CLAUDE.md bilingual requirement: all text outputs handle Cyrillic correctly.
 */
@Component
public class ScoreInterpreter {

    /**
     * Immutable record representing a proficiency interpretation.
     *
     * @param label The human-readable proficiency label (localized)
     * @param level The classification level key (e.g., "BEGINNING", "EXPERT")
     */
    public record ScoreInterpretation(String label, String level) {}

    private static final String LOCALE_RU = "ru";

    /**
     * Interpret a percentage score into a proficiency label and level.
     *
     * @param percentage Score as a percentage (0-100)
     * @param locale     Locale code ("en" or "ru"); defaults to "en" if null or unsupported
     * @return ScoreInterpretation with localized label and level key
     */
    public ScoreInterpretation interpret(double percentage, String locale) {
        String effectiveLocale = locale != null ? locale.toLowerCase() : "en";

        if (percentage >= 85.0) {
            return new ScoreInterpretation(
                    LOCALE_RU.equals(effectiveLocale) ? "Эксперт" : "Expert",
                    "EXPERT"
            );
        } else if (percentage >= 70.0) {
            return new ScoreInterpretation(
                    LOCALE_RU.equals(effectiveLocale) ? "Опытный" : "Advanced",
                    "ADVANCED"
            );
        } else if (percentage >= 50.0) {
            return new ScoreInterpretation(
                    LOCALE_RU.equals(effectiveLocale) ? "Компетентный" : "Proficient",
                    "PROFICIENT"
            );
        } else if (percentage >= 30.0) {
            return new ScoreInterpretation(
                    LOCALE_RU.equals(effectiveLocale) ? "Развивающийся" : "Developing",
                    "DEVELOPING"
            );
        } else {
            return new ScoreInterpretation(
                    LOCALE_RU.equals(effectiveLocale) ? "Начальный" : "Beginning",
                    "BEGINNING"
            );
        }
    }
}
