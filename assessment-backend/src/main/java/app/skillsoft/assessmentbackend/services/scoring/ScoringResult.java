package app.skillsoft.assessmentbackend.services.scoring;

import app.skillsoft.assessmentbackend.domain.dto.CompetencyScoreDto;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;

import java.util.ArrayList;
import java.util.List;

/**
 * Result object from scoring strategy calculation.
 * Contains overall score and detailed competency breakdown.
 */
public class ScoringResult {
    
    private Double overallScore;
    private Double overallPercentage;
    private Boolean passed;
    private AssessmentGoal goal;
    private List<CompetencyScoreDto> competencyScores = new ArrayList<>();
    
    // Constructors
    public ScoringResult() {
    }
    
    public ScoringResult(Double overallScore, Double overallPercentage, List<CompetencyScoreDto> competencyScores) {
        this.overallScore = overallScore;
        this.overallPercentage = overallPercentage;
        this.competencyScores = competencyScores;
    }
    
    // Getters and Setters
    public Double getOverallScore() {
        return overallScore;
    }
    
    public void setOverallScore(Double overallScore) {
        this.overallScore = overallScore;
    }
    
    public Double getOverallPercentage() {
        return overallPercentage;
    }
    
    public void setOverallPercentage(Double overallPercentage) {
        this.overallPercentage = overallPercentage;
    }
    
    public Boolean getPassed() {
        return passed;
    }
    
    public void setPassed(Boolean passed) {
        this.passed = passed;
    }
    
    public AssessmentGoal getGoal() {
        return goal;
    }
    
    public void setGoal(AssessmentGoal goal) {
        this.goal = goal;
    }
    
    public List<CompetencyScoreDto> getCompetencyScores() {
        return competencyScores;
    }
    
    public void setCompetencyScores(List<CompetencyScoreDto> competencyScores) {
        this.competencyScores = competencyScores;
    }
}
