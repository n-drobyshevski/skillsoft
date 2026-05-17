package app.skillsoft.assessmentbackend.domain.dto.stats;

import java.util.Map;

public record EntityStatsDto(
    CompetencyStatsDto competencies,
    IndicatorStatsDto indicators,
    QuestionStatsDto questions
) {
    public record CompetencyStatsDto(
        long total,
        long active,
        long withIndicators,
        double averageIndicatorWeight,
        Map<String, Long> byCategory
    ) {}

    public record IndicatorStatsDto(
        long total,
        long active,
        long withQuestions,
        long measurable,
        double averageComplexity,
        Map<String, Long> byContextScope
    ) {}

    public record QuestionStatsDto(
        long total,
        long active,
        long withActiveIndicators,
        long hardQuestions,
        double averageTimeLimitSeconds,
        Map<String, Long> byDifficulty,
        Map<String, Long> byQuestionType
    ) {}
}
