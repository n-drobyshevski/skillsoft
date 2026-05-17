package app.skillsoft.assessmentbackend.services.scoring;

import app.skillsoft.assessmentbackend.domain.dto.CompetencyScoreDto;
import app.skillsoft.assessmentbackend.domain.dto.IndicatorScoreDto;
import app.skillsoft.assessmentbackend.domain.entities.Competency;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Shared helper class to aggregate indicator scores at the competency level.
 * Used by all scoring strategies (Overview, JobFit, TeamFit).
 *
 * Implements weighted indicator aggregation:
 *   percentage = SUM(w_k * p_k) / SUM(w_k)
 *
 * where w_k is the indicator weight and p_k is the indicator percentage.
 * When all weights are 1.0 (default), this degrades to a simple average.
 *
 * The raw score and maxScore fields use weighted summation for accurate
 * competency-level score tracking:
 *   totalScore = SUM(w_k * indicator_score_k)
 *   totalMaxScore = SUM(w_k * indicator_maxScore_k)
 */
public class CompetencyAggregation {

    private final UUID competencyId;
    private double weightedPercentageSum = 0;
    private double totalWeight = 0;
    private double totalScore = 0;
    private double totalMaxScore = 0;
    private int questionCount = 0;
    private final List<IndicatorScoreDto> indicatorScores = new ArrayList<>();

    public CompetencyAggregation(UUID competencyId) {
        this.competencyId = competencyId;
    }

    /**
     * Add an indicator's scores to this competency aggregation using weighted formula.
     *
     * @param indicatorDto The indicator score DTO (includes weight and percentage)
     * @param agg          The raw aggregation data for this indicator
     */
    public void addIndicator(IndicatorScoreDto indicatorDto, IndicatorAggregation agg) {
        double weight = indicatorDto.getWeight() != null ? indicatorDto.getWeight() : 1.0;
        double percentage = indicatorDto.getPercentage() != null ? indicatorDto.getPercentage() : 0.0;

        weightedPercentageSum += weight * percentage;
        totalWeight += weight;

        // Track weighted raw scores for accurate breakdown
        totalScore += weight * agg.getTotalScore();
        totalMaxScore += weight * agg.getTotalMaxScore();
        questionCount += agg.getQuestionCount();
        indicatorScores.add(indicatorDto);
    }

    public CompetencyScoreDto toDto(Competency competency) {
        double percentage = totalWeight > 0 ? weightedPercentageSum / totalWeight : 0.0;

        CompetencyScoreDto dto = new CompetencyScoreDto();
        dto.setCompetencyId(competencyId);
        dto.setCompetencyName(competency != null ? competency.getName() : "Unknown Competency");
        dto.setScore(totalScore);
        dto.setMaxScore(totalMaxScore);
        dto.setPercentage(percentage);
        dto.setQuestionsAnswered(questionCount);
        dto.setOnetCode(competency != null ? competency.getOnetCode() : null);
        dto.setIndicatorScores(indicatorScores);
        return dto;
    }

    // Accessors for strategy-specific logic
    public UUID getCompetencyId() {
        return competencyId;
    }

    public double getTotalScore() {
        return totalScore;
    }

    public double getTotalMaxScore() {
        return totalMaxScore;
    }

    public double getTotalWeight() {
        return totalWeight;
    }

    public double getWeightedPercentageSum() {
        return weightedPercentageSum;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public List<IndicatorScoreDto> getIndicatorScores() {
        return indicatorScores;
    }

    /**
     * Get the weighted percentage for this competency.
     * Equivalent to SUM(w_k * p_k) / SUM(w_k).
     */
    public double getWeightedPercentage() {
        return totalWeight > 0 ? weightedPercentageSum / totalWeight : 0.0;
    }
}
