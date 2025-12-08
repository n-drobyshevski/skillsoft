package app.skillsoft.assessmentbackend.domain.dto.simulation;

import java.util.UUID;

/**
 * Summary DTO for questions in simulation results.
 * Contains essential question info without full answer options.
 */
public record QuestionSummaryDto(
    UUID id,
    UUID competencyId,
    UUID behavioralIndicatorId,
    String questionText,
    String difficulty,
    String questionType,
    Integer timeLimitSeconds,
    boolean simulatedCorrect,
    String simulatedAnswer
) {
    /**
     * Create a summary from simulation run.
     */
    public static QuestionSummaryDto of(
            UUID id,
            UUID competencyId,
            UUID behavioralIndicatorId,
            String questionText,
            String difficulty,
            String questionType,
            Integer timeLimitSeconds,
            boolean simulatedCorrect,
            String simulatedAnswer
    ) {
        return new QuestionSummaryDto(
            id, competencyId, behavioralIndicatorId,
            questionText, difficulty, questionType,
            timeLimitSeconds, simulatedCorrect, simulatedAnswer
        );
    }
}
