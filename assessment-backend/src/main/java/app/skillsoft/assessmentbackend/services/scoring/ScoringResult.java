package app.skillsoft.assessmentbackend.services.scoring;

import app.skillsoft.assessmentbackend.domain.dto.CompetencyScoreDto;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    /**
     * Big Five personality profile (only populated for TEAM_FIT goal).
     * Maps trait names (e.g., "Openness", "Conscientiousness") to percentage scores (0-100).
     */
    private Map<String, Double> bigFiveProfile;

    /**
     * Team fit specific metrics (only populated for TEAM_FIT goal).
     * Contains diversity/saturation analysis results.
     */
    private TeamFitMetrics teamFitMetrics;
    
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

    public Map<String, Double> getBigFiveProfile() {
        return bigFiveProfile;
    }

    public void setBigFiveProfile(Map<String, Double> bigFiveProfile) {
        this.bigFiveProfile = bigFiveProfile;
    }

    public TeamFitMetrics getTeamFitMetrics() {
        return teamFitMetrics;
    }

    public void setTeamFitMetrics(TeamFitMetrics teamFitMetrics) {
        this.teamFitMetrics = teamFitMetrics;
    }

    /**
     * Nested class for Team Fit specific metrics.
     * Captures diversity/saturation analysis results from TEAM_FIT scoring.
     */
    public static class TeamFitMetrics {
        /**
         * Ratio of competencies contributing to team diversity (0.0-1.0).
         */
        private double diversityRatio;

        /**
         * Ratio of competencies already saturated in the team (0.0-1.0).
         */
        private double saturationRatio;

        /**
         * Multiplier applied to overall score based on diversity/saturation balance.
         */
        private double teamFitMultiplier;

        /**
         * Count of competencies contributing to team diversity.
         */
        private int diversityCount;

        /**
         * Count of competencies contributing to team saturation.
         */
        private int saturationCount;

        /**
         * Count of competencies identified as team gaps (low scores).
         */
        private int gapCount;

        // Default constructor
        public TeamFitMetrics() {
        }

        // All-args constructor
        public TeamFitMetrics(double diversityRatio, double saturationRatio, double teamFitMultiplier,
                              int diversityCount, int saturationCount, int gapCount) {
            this.diversityRatio = diversityRatio;
            this.saturationRatio = saturationRatio;
            this.teamFitMultiplier = teamFitMultiplier;
            this.diversityCount = diversityCount;
            this.saturationCount = saturationCount;
            this.gapCount = gapCount;
        }

        // Builder pattern for convenience
        public static Builder builder() {
            return new Builder();
        }

        // Getters and Setters
        public double getDiversityRatio() {
            return diversityRatio;
        }

        public void setDiversityRatio(double diversityRatio) {
            this.diversityRatio = diversityRatio;
        }

        public double getSaturationRatio() {
            return saturationRatio;
        }

        public void setSaturationRatio(double saturationRatio) {
            this.saturationRatio = saturationRatio;
        }

        public double getTeamFitMultiplier() {
            return teamFitMultiplier;
        }

        public void setTeamFitMultiplier(double teamFitMultiplier) {
            this.teamFitMultiplier = teamFitMultiplier;
        }

        public int getDiversityCount() {
            return diversityCount;
        }

        public void setDiversityCount(int diversityCount) {
            this.diversityCount = diversityCount;
        }

        public int getSaturationCount() {
            return saturationCount;
        }

        public void setSaturationCount(int saturationCount) {
            this.saturationCount = saturationCount;
        }

        public int getGapCount() {
            return gapCount;
        }

        public void setGapCount(int gapCount) {
            this.gapCount = gapCount;
        }

        /**
         * Builder class for TeamFitMetrics.
         */
        public static class Builder {
            private double diversityRatio;
            private double saturationRatio;
            private double teamFitMultiplier;
            private int diversityCount;
            private int saturationCount;
            private int gapCount;

            public Builder diversityRatio(double diversityRatio) {
                this.diversityRatio = diversityRatio;
                return this;
            }

            public Builder saturationRatio(double saturationRatio) {
                this.saturationRatio = saturationRatio;
                return this;
            }

            public Builder teamFitMultiplier(double teamFitMultiplier) {
                this.teamFitMultiplier = teamFitMultiplier;
                return this;
            }

            public Builder diversityCount(int diversityCount) {
                this.diversityCount = diversityCount;
                return this;
            }

            public Builder saturationCount(int saturationCount) {
                this.saturationCount = saturationCount;
                return this;
            }

            public Builder gapCount(int gapCount) {
                this.gapCount = gapCount;
                return this;
            }

            public TeamFitMetrics build() {
                return new TeamFitMetrics(diversityRatio, saturationRatio, teamFitMultiplier,
                        diversityCount, saturationCount, gapCount);
            }
        }
    }
}
