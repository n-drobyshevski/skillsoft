package app.skillsoft.assessmentbackend.domain.dto;

import app.skillsoft.assessmentbackend.domain.entities.DifficultyLevel;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Indicator-level question inventory for a competency.
 * Used by the blueprint builder's library panel expansion.
 */
public record IndicatorInventoryDto(
    UUID competencyId,
    List<IndicatorQuestionStats> indicators
) {
    public record IndicatorQuestionStats(
        UUID indicatorId,
        String title,
        float weight,
        boolean isActive,
        int totalQuestions,
        Map<DifficultyLevel, Integer> questionsByDifficulty
    ) {}
}
