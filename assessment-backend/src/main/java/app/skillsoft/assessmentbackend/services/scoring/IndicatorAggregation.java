package app.skillsoft.assessmentbackend.services.scoring;

import app.skillsoft.assessmentbackend.domain.dto.IndicatorScoreDto;
import app.skillsoft.assessmentbackend.domain.entities.BehavioralIndicator;

import java.util.UUID;

/**
 * Shared helper class to aggregate scores at the indicator level.
 * Used by all scoring strategies (Overview, JobFit, TeamFit).
 *
 * Each indicator accumulates normalized answer scores (0-1 range)
 * and produces an IndicatorScoreDto with percentage calculation.
 */
public class IndicatorAggregation {

    private final UUID indicatorId;
    private double totalScore = 0;
    private double totalMaxScore = 0;
    private int questionCount = 0;

    public IndicatorAggregation(UUID indicatorId) {
        this.indicatorId = indicatorId;
    }

    public void addAnswer(double normalizedScore) {
        totalScore += normalizedScore;
        totalMaxScore += 1.0; // Each normalized answer has max 1.0
        questionCount++;
    }

    public IndicatorScoreDto toDto(BehavioralIndicator indicator) {
        double percentage = totalMaxScore > 0 ? (totalScore / totalMaxScore) * 100.0 : 0.0;

        IndicatorScoreDto dto = new IndicatorScoreDto();
        dto.setIndicatorId(indicatorId);
        dto.setIndicatorTitle(indicator != null ? indicator.getTitle() : "Unknown Indicator");
        dto.setWeight(indicator != null ? (double) indicator.getWeight() : 1.0);
        dto.setScore(totalScore);
        dto.setMaxScore(totalMaxScore);
        dto.setPercentage(percentage);
        dto.setQuestionsAnswered(questionCount);
        return dto;
    }

    // Accessors for CompetencyAggregation rollup
    public UUID getIndicatorId() {
        return indicatorId;
    }

    public double getTotalScore() {
        return totalScore;
    }

    public double getTotalMaxScore() {
        return totalMaxScore;
    }

    public int getQuestionCount() {
        return questionCount;
    }
}
