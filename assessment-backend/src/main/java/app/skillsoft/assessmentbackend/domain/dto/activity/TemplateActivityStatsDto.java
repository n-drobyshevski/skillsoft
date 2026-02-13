package app.skillsoft.assessmentbackend.domain.dto.activity;

import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO representing aggregated activity statistics for a test template.
 * Used in template activity dashboard pages.
 */
public record TemplateActivityStatsDto(
        UUID templateId,
        String templateName,
        AssessmentGoal goal,
        long totalSessions,
        long completedCount,
        long abandonedCount,
        long timedOutCount,
        Double completionRate,
        Double passRate,
        Double averageScore,
        Double averageTimeSeconds,
        LocalDateTime lastActivity
) {
    /**
     * Factory method for empty stats (no activity yet).
     */
    public static TemplateActivityStatsDto empty(UUID templateId, String templateName, AssessmentGoal goal) {
        return new TemplateActivityStatsDto(
                templateId,
                templateName,
                goal,
                0L,
                0L,
                0L,
                0L,
                0.0,
                0.0,
                0.0,
                0.0,
                null
        );
    }

    /**
     * Factory method to calculate rates from raw counts.
     */
    public static TemplateActivityStatsDto fromCounts(
            UUID templateId,
            String templateName,
            AssessmentGoal goal,
            long totalSessions,
            long completedCount,
            long abandonedCount,
            long timedOutCount,
            long passedCount,
            Double totalScore,
            Double totalTimeSeconds,
            LocalDateTime lastActivity
    ) {
        double completionRate = totalSessions > 0
                ? (completedCount * 100.0 / totalSessions)
                : 0.0;
        double passRate = completedCount > 0
                ? (passedCount * 100.0 / completedCount)
                : 0.0;
        double averageScore = completedCount > 0 && totalScore != null
                ? (totalScore / completedCount)
                : 0.0;
        double averageTime = completedCount > 0 && totalTimeSeconds != null
                ? (totalTimeSeconds / completedCount)
                : 0.0;

        return new TemplateActivityStatsDto(
                templateId,
                templateName,
                goal,
                totalSessions,
                completedCount,
                abandonedCount,
                timedOutCount,
                Math.round(completionRate * 10.0) / 10.0,
                Math.round(passRate * 10.0) / 10.0,
                Math.round(averageScore * 10.0) / 10.0,
                Math.round(averageTime * 10.0) / 10.0,
                lastActivity
        );
    }
}
