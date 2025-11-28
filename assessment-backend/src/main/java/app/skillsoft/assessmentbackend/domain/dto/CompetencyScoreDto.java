package app.skillsoft.assessmentbackend.domain.dto;

import java.util.UUID;

/**
 * DTO for storing individual competency scores within a test result.
 * Used as an element in the JSONB competency_scores array.
 */
public class CompetencyScoreDto {
    
    private UUID competencyId;
    private String competencyName;
    private Double score;
    private Double maxScore;
    private Double percentage;
    private Integer questionsAnswered;
    private Integer questionsCorrect;

    // Constructors
    public CompetencyScoreDto() {
    }

    public CompetencyScoreDto(UUID competencyId, String competencyName, Double score, 
                              Double maxScore, Double percentage) {
        this.competencyId = competencyId;
        this.competencyName = competencyName;
        this.score = score;
        this.maxScore = maxScore;
        this.percentage = percentage;
    }

    // Getters and Setters
    public UUID getCompetencyId() {
        return competencyId;
    }

    public void setCompetencyId(UUID competencyId) {
        this.competencyId = competencyId;
    }

    public String getCompetencyName() {
        return competencyName;
    }

    public void setCompetencyName(String competencyName) {
        this.competencyName = competencyName;
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

    public Integer getQuestionsCorrect() {
        return questionsCorrect;
    }

    public void setQuestionsCorrect(Integer questionsCorrect) {
        this.questionsCorrect = questionsCorrect;
    }

    @Override
    public String toString() {
        return "CompetencyScoreDto{" +
                "competencyId=" + competencyId +
                ", competencyName='" + competencyName + '\'' +
                ", score=" + score +
                ", maxScore=" + maxScore +
                ", percentage=" + percentage +
                '}';
    }
}
