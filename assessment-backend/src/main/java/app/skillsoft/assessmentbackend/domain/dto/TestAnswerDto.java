package app.skillsoft.assessmentbackend.domain.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for TestAnswer entity response.
 */
public record TestAnswerDto(
        UUID id,
        UUID sessionId,
        UUID questionId,
        String questionText,
        List<String> selectedOptionIds,
        Integer likertValue,
        List<String> rankingOrder,
        String textResponse,
        LocalDateTime answeredAt,
        Integer timeSpentSeconds,
        Boolean isSkipped,
        Double score,
        Double maxScore
) {
}
