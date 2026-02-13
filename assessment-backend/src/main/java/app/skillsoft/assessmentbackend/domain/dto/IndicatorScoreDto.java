package app.skillsoft.assessmentbackend.domain.dto;

import java.util.UUID;

/**
 * DTO for storing individual behavioral indicator scores within a competency score.
 * Provides granular breakdown of performance at the indicator level.
 *
 * Note: Question-level details are NOT included here - they are lazy-loaded
 * via a separate endpoint to optimize initial payload size.
 */
public class IndicatorScoreDto {

    private UUID indicatorId;
    private String indicatorTitle;
    private String indicatorDescription;
    private Double weight;
    private Double score;
    private Double maxScore;
    private Double percentage;
    private Integer questionsAnswered;

    // Constructors
    public IndicatorScoreDto() {
    }

    public IndicatorScoreDto(UUID indicatorId, String indicatorTitle, Double score,
                             Double maxScore, Double percentage, Integer questionsAnswered) {
        this.indicatorId = indicatorId;
        this.indicatorTitle = indicatorTitle;
        this.score = score;
        this.maxScore = maxScore;
        this.percentage = percentage;
        this.questionsAnswered = questionsAnswered;
    }

    // Builder-style setters for fluent API
    public IndicatorScoreDto withWeight(Double weight) {
        this.weight = weight;
        return this;
    }

    public IndicatorScoreDto withDescription(String description) {
        this.indicatorDescription = description;
        return this;
    }

    // Getters and Setters
    public UUID getIndicatorId() {
        return indicatorId;
    }

    public void setIndicatorId(UUID indicatorId) {
        this.indicatorId = indicatorId;
    }

    public String getIndicatorTitle() {
        return indicatorTitle;
    }

    public void setIndicatorTitle(String indicatorTitle) {
        this.indicatorTitle = indicatorTitle;
    }

    public String getIndicatorDescription() {
        return indicatorDescription;
    }

    public void setIndicatorDescription(String indicatorDescription) {
        this.indicatorDescription = indicatorDescription;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Double getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(Double maxScore) {
        this.maxScore = maxScore;
    }

    public Double getPercentage() {
        return percentage;
    }

    public void setPercentage(Double percentage) {
        this.percentage = percentage;
    }

    public Integer getQuestionsAnswered() {
        return questionsAnswered;
    }

    public void setQuestionsAnswered(Integer questionsAnswered) {
        this.questionsAnswered = questionsAnswered;
    }

    @Override
    public String toString() {
        return "IndicatorScoreDto{" +
                "indicatorId=" + indicatorId +
                ", indicatorTitle='" + indicatorTitle + '\'' +
                ", score=" + score +
                ", maxScore=" + maxScore +
                ", percentage=" + percentage +
                ", questionsAnswered=" + questionsAnswered +
                '}';
    }
}
