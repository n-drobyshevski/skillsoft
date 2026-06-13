package app.skillsoft.assessmentbackend.domain.dto;

import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;
import app.skillsoft.assessmentbackend.domain.entities.ResultStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for TestResult entity response.
 *
 * The status field indicates the scoring calculation state:
 * - COMPLETED: Scoring finished successfully (default for backward compatibility)
 * - PENDING: Scoring failed and is awaiting retry
 * - FAILED: Scoring failed permanently after all retries
 *
 * The bigFiveProfile field is only populated for TEAM_FIT assessments and contains
 * personality trait scores (e.g., "Openness": 75.5, "Conscientiousness": 82.3).
 *
 * The extendedMetrics field contains goal-specific metrics:
 * - For TEAM_FIT: TeamFitMetrics (diversityRatio, saturationRatio, teamFitMultiplier, etc.)
 *
 * The goal field carries the assessment type of the originating template
 * (OVERVIEW / JOB_FIT / TEAM_FIT) so clients can categorize results without
 * a separate template lookup.
 */
public record TestResultDto(
        UUID id,
        UUID sessionId,
        UUID templateId,
        String templateName,
        AssessmentGoal goal,
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
        LocalDateTime completedAt,
        ResultStatus status,
        Map<String, Double> bigFiveProfile,
        Map<String, Object> extendedMetrics
) {
    /**
     * Constructor for backward compatibility - defaults status to COMPLETED, null Big Five/extended metrics.
     */
    public TestResultDto(
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
        this(id, sessionId, templateId, templateName, AssessmentGoal.OVERVIEW, clerkUserId,
                overallScore, overallPercentage, percentile, passed,
                competencyScores, totalTimeSeconds, questionsAnswered,
                questionsSkipped, totalQuestions, completedAt,
                ResultStatus.COMPLETED, null, null);
    }

    /**
     * Constructor for backward compatibility with status - defaults null Big Five/extended metrics.
     */
    public TestResultDto(
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
            LocalDateTime completedAt,
            ResultStatus status
    ) {
        this(id, sessionId, templateId, templateName, AssessmentGoal.OVERVIEW, clerkUserId,
                overallScore, overallPercentage, percentile, passed,
                competencyScores, totalTimeSeconds, questionsAnswered,
                questionsSkipped, totalQuestions, completedAt,
                status, null, null);
    }
}
