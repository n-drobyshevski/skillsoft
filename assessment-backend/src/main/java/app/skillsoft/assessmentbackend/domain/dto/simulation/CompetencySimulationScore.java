package app.skillsoft.assessmentbackend.domain.dto.simulation;

import java.util.Map;
import java.util.UUID;

/**
 * Per-competency breakdown of simulation results.
 * Groups simulated question outcomes by competency to give builders
 * visibility into how each competency would score under the chosen persona.
 */
public record CompetencySimulationScore(
    UUID competencyId,
    int totalQuestions,
    int correctAnswers,
    double scorePercentage,
    Map<String, Integer> difficultyBreakdown
) {
    public static CompetencySimulationScore of(
            UUID competencyId,
            int totalQuestions,
            int correctAnswers,
            Map<String, Integer> difficultyBreakdown) {
        double score = totalQuestions > 0
                ? (double) correctAnswers / totalQuestions * 100
                : 0.0;
        return new CompetencySimulationScore(
                competencyId, totalQuestions, correctAnswers, score, difficultyBreakdown);
    }
}
