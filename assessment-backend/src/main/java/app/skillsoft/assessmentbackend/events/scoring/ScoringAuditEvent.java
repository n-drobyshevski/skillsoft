package app.skillsoft.assessmentbackend.events.scoring;

import app.skillsoft.assessmentbackend.domain.dto.CompetencyScoreDto;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Event published after scoring calculation for audit trail persistence.
 * Contains a snapshot of all scoring inputs and outputs for traceability.
 *
 * Published by ScoringOrchestrationServiceImpl, consumed asynchronously
 * by ScoringAuditListener to avoid blocking the scoring transaction.
 *
 * @param sessionId           The test session that was scored
 * @param resultId            The result entity ID
 * @param clerkUserId         The user ID (null for anonymous)
 * @param templateId          The test template ID
 * @param goal                The assessment goal
 * @param strategyClass       The scoring strategy class name
 * @param overallScore        The calculated overall score
 * @param overallPercentage   The overall percentage
 * @param passed              Whether the test was passed
 * @param percentile          The percentile rank
 * @param competencyScores    Detailed competency score breakdown
 * @param indicatorWeights    Snapshot of indicator weights used
 * @param configSnapshot      Scoring configuration at time of calculation
 * @param totalAnswers        Total number of answers
 * @param answeredCount       Number of answered questions
 * @param skippedCount        Number of skipped questions
 * @param scoringDuration     How long the scoring took
 * @param timestamp           When the event was created
 */
public record ScoringAuditEvent(
        UUID sessionId,
        UUID resultId,
        String clerkUserId,
        UUID templateId,
        AssessmentGoal goal,
        String strategyClass,
        Double overallScore,
        Double overallPercentage,
        Boolean passed,
        Integer percentile,
        List<CompetencyScoreDto> competencyScores,
        Map<String, Double> indicatorWeights,
        Map<String, Object> configSnapshot,
        int totalAnswers,
        int answeredCount,
        int skippedCount,
        Duration scoringDuration,
        Instant timestamp
) {
    /**
     * Factory method for creating an audit event from scoring results.
     */
    public static ScoringAuditEvent from(
            UUID sessionId,
            UUID resultId,
            String clerkUserId,
            UUID templateId,
            AssessmentGoal goal,
            String strategyClass,
            Double overallScore,
            Double overallPercentage,
            Boolean passed,
            Integer percentile,
            List<CompetencyScoreDto> competencyScores,
            Map<String, Double> indicatorWeights,
            Map<String, Object> configSnapshot,
            int totalAnswers,
            int answeredCount,
            int skippedCount,
            Instant scoringStartTime) {

        Duration duration = Duration.between(scoringStartTime, Instant.now());
        return new ScoringAuditEvent(
                sessionId, resultId, clerkUserId, templateId,
                goal, strategyClass, overallScore, overallPercentage,
                passed, percentile, competencyScores,
                indicatorWeights, configSnapshot,
                totalAnswers, answeredCount, skippedCount,
                duration, Instant.now()
        );
    }
}
