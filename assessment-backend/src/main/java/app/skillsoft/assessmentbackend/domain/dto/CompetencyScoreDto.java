package app.skillsoft.assessmentbackend.domain.dto;

import java.util.List;
import java.util.UUID;

/**
 * DTO for storing individual competency scores within a test result.
 * Used as an element in the JSONB competency_scores array.
 *
 * Now includes nested indicator-level breakdown for richer insights.
 */
public class CompetencyScoreDto {

    private UUID competencyId;
    private String competencyName;
    private Double score;
    private Double maxScore;
    private Double percentage;
    private Integer questionsAnswered;
    private Integer questionsCorrect;
    private String onetCode; // O*NET code for Big Five projection
    private List<IndicatorScoreDto> indicatorScores; // Nested indicator breakdown

    // Confidence interval fields (populated by ConfidenceIntervalCalculator)
    private Double sem;           // Standard Error of Measurement
    private Double ciLower;       // 95% CI lower bound
    private Double ciUpper;       // 95% CI upper bound
    private Double cronbachAlpha; // Cronbach's alpha used for calculation

    // Per-competency percentile (populated by SubscalePercentileCalculator)
    private Integer percentile;   // Percentile rank within this competency across all takers

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

    public String getOnetCode() {
        return onetCode;
    }

    public void setOnetCode(String onetCode) {
        this.onetCode = onetCode;
    }

    public List<IndicatorScoreDto> getIndicatorScores() {
        return indicatorScores;
    }

    public void setIndicatorScores(List<IndicatorScoreDto> indicatorScores) {
        this.indicatorScores = indicatorScores;
    }

    public Double getSem() {
        return sem;
    }

    public void setSem(Double sem) {
        this.sem = sem;
    }

    public Double getCiLower() {
        return ciLower;
    }

    public void setCiLower(Double ciLower) {
        this.ciLower = ciLower;
    }

    public Double getCiUpper() {
        return ciUpper;
    }

    public void setCiUpper(Double ciUpper) {
        this.ciUpper = ciUpper;
    }

    public Double getCronbachAlpha() {
        return cronbachAlpha;
    }

    public void setCronbachAlpha(Double cronbachAlpha) {
        this.cronbachAlpha = cronbachAlpha;
    }

    public Integer getPercentile() {
        return percentile;
    }

    public void setPercentile(Integer percentile) {
        this.percentile = percentile;
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
