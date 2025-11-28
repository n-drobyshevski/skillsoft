package app.skillsoft.assessmentbackend.domain.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for TestResult entity response.
 */
public record TestResultDto(
        UUID id,
        UUID sessionId,
        UUID templateId,
        String templateName,
        String clerkUserId,
        Double overallScore,
        Double overallPercentage,
        Integer percentile,
        Boolean passed,
        List<CompetencyScoreDto> competencyScores,
        Integer totalTimeSeconds,
        Integer questionsAnswered,
        Integer questionsSkipped,
        Integer totalQuestions,
        LocalDateTime completedAt
) {
}
