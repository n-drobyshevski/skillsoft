package app.skillsoft.assessmentbackend.domain.dto;

import app.skillsoft.assessmentbackend.domain.entities.AnonymousTakerInfo;
import app.skillsoft.assessmentbackend.domain.entities.TestResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for publicly accessible anonymous test results.
 *
 * <p>This DTO is designed for viewing via persistent result URLs (HMAC-signed tokens).
 * It intentionally omits sensitive fields like email, IP address, and internal IDs
 * to protect taker privacy.</p>
 */
public record PublicAnonymousResultDto(
        UUID resultId,
        String takerName,
        String templateName,
        Double overallPercentage,
        Boolean passed,
        List<CompetencyScoreDto> competencyBreakdown,
        Integer totalTimeSeconds,
        Integer questionsAnswered,
        Integer questionsSkipped,
        Integer totalQuestions,
        LocalDateTime completedAt
) {
    /**
     * Create from TestResult entity.
     *
     * @param result The test result entity
     * @return Public-safe result DTO
     */
    public static PublicAnonymousResultDto from(TestResult result) {
        AnonymousTakerInfo takerInfo = result.getAnonymousTakerInfo();
        String takerName = takerInfo != null ? takerInfo.getDisplayName() : "Anonymous";

        String templateName = null;
        if (result.getSession() != null && result.getSession().getTemplate() != null) {
            templateName = result.getSession().getTemplate().getName();
        }

        int totalQuestions = 0;
        if (result.getQuestionsAnswered() != null) {
            totalQuestions += result.getQuestionsAnswered();
        }
        if (result.getQuestionsSkipped() != null) {
            totalQuestions += result.getQuestionsSkipped();
        }

        return new PublicAnonymousResultDto(
                result.getId(),
                takerName,
                templateName,
                result.getOverallPercentage(),
                result.getPassed(),
                result.getCompetencyScores(),
                result.getTotalTimeSeconds(),
                result.getQuestionsAnswered(),
                result.getQuestionsSkipped(),
                totalQuestions,
                result.getCompletedAt()
        );
    }
}
