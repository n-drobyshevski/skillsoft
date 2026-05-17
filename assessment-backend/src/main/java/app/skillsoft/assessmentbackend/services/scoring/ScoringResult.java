package app.skillsoft.assessmentbackend.services.scoring;

import app.skillsoft.assessmentbackend.domain.dto.CompetencyScoreDto;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;

import java.util.ArrayList;
import java.util.HashMap;
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
     * Decision confidence score (0.0-1.0) indicating how confident the system
     * is in the pass/fail decision. Only populated for JOB_FIT goal.
     *
     * Computed from three weighted factors:
     * - Margin factor (50%): distance from pass/fail threshold
     * - Evidence factor (30%): proportion of competencies with sufficient evidence
     * - Coverage factor (20%): competencies assessed vs benchmarks required
     */
    private Double decisionConfidence;

    /**
     * Human-readable confidence level: "HIGH", "MEDIUM", or "LOW".
     * Derived from decisionConfidence thresholds (>=0.7, >=0.4, <0.4).
     */
    private String confidenceLevel;

    /**
     * Human-readable explanation of the confidence assessment.
     * Provides context-specific messaging for hiring managers based on
     * the confidence level and pass/fail outcome.
     */
    private String confidenceMessage;

    /**
     * Response consistency score (0.0-1.0) indicating how consistent
     * the candidate's response patterns were across the assessment.
     * Populated for all assessment goals by ResponseConsistencyAnalyzer.
     *
     * Computed from three weighted factors:
     * - Speed anomaly rate (30%): proportion of unusually fast responses
     * - Straight-lining rate (30%): proportion of repeated Likert values
     * - Intra-competency variance factor (40%): normal vs abnormal variance within competencies
     */
    private Double consistencyScore;

    /**
     * Human-readable flags describing response consistency concerns.
     * Empty list if no issues detected. Populated for all assessment goals.
     */
    private List<String> consistencyFlags;

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

    /**
     * Extended metrics map for strategy-specific data that flows through to TestResult.
     * Used by OverviewScoringStrategy for profile pattern analysis, etc.
     */
    private Map<String, Object> extendedMetrics;
    
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

    public Double getDecisionConfidence() {
        return decisionConfidence;
    }

    public void setDecisionConfidence(Double decisionConfidence) {
        this.decisionConfidence = decisionConfidence;
    }

    public String getConfidenceLevel() {
        return confidenceLevel;
    }

    public void setConfidenceLevel(String confidenceLevel) {
        this.confidenceLevel = confidenceLevel;
    }

    public String getConfidenceMessage() {
        return confidenceMessage;
    }

    public void setConfidenceMessage(String confidenceMessage) {
        this.confidenceMessage = confidenceMessage;
    }

    public Double getConsistencyScore() {
        return consistencyScore;
    }

    public void setConsistencyScore(Double consistencyScore) {
        this.consistencyScore = consistencyScore;
    }

    public List<String> getConsistencyFlags() {
        return consistencyFlags;
    }

    public void setConsistencyFlags(List<String> consistencyFlags) {
        this.consistencyFlags = consistencyFlags;
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

    public Map<String, Object> getExtendedMetrics() {
        return extendedMetrics;
    }

    public void setExtendedMetrics(Map<String, Object> extendedMetrics) {
        this.extendedMetrics = extendedMetrics;
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

        /**
         * Per-competency saturation map: competency name -> candidate percentage (0.0-1.0).
         * Uses competency names (not UUIDs) as keys for frontend radar chart display.
         */
        private Map<String, Double> competencySaturation = new HashMap<>();

        /**
         * Number of members in the team (from TeamService).
         * 0 if team profile was not available or no members found.
         */
        private int teamSize;

        /**
         * Personality compatibility score between candidate and team (0.0-1.0).
         * Based on Euclidean distance between candidate's Big Five profile and team's averagePersonality.
         * Higher = more compatible. Null if no personality data available.
         */
        private Double personalityCompatibility;

        // Default constructor
        public TeamFitMetrics() {
        }

        // All-args constructor
        public TeamFitMetrics(double diversityRatio, double saturationRatio, double teamFitMultiplier,
                              int diversityCount, int saturationCount, int gapCount,
                              Map<String, Double> competencySaturation, int teamSize,
                              Double personalityCompatibility) {
            this.diversityRatio = diversityRatio;
            this.saturationRatio = saturationRatio;
            this.teamFitMultiplier = teamFitMultiplier;
            this.diversityCount = diversityCount;
            this.saturationCount = saturationCount;
            this.gapCount = gapCount;
            this.competencySaturation = competencySaturation != null ? competencySaturation : new HashMap<>();
            this.teamSize = teamSize;
            this.personalityCompatibility = personalityCompatibility;
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

        public Map<String, Double> getCompetencySaturation() {
            return competencySaturation;
        }

        public void setCompetencySaturation(Map<String, Double> competencySaturation) {
            this.competencySaturation = competencySaturation != null ? competencySaturation : new HashMap<>();
        }

        public int getTeamSize() {
            return teamSize;
        }

        public void setTeamSize(int teamSize) {
            this.teamSize = teamSize;
        }

        public Double getPersonalityCompatibility() {
            return personalityCompatibility;
        }

        public void setPersonalityCompatibility(Double personalityCompatibility) {
            this.personalityCompatibility = personalityCompatibility;
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
            private Map<String, Double> competencySaturation = new HashMap<>();
            private int teamSize;
            private Double personalityCompatibility;

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

            public Builder competencySaturation(Map<String, Double> competencySaturation) {
                this.competencySaturation = competencySaturation != null ? competencySaturation : new HashMap<>();
                return this;
            }

            public Builder teamSize(int teamSize) {
                this.teamSize = teamSize;
                return this;
            }

            public Builder personalityCompatibility(Double personalityCompatibility) {
                this.personalityCompatibility = personalityCompatibility;
                return this;
            }

            public TeamFitMetrics build() {
                return new TeamFitMetrics(diversityRatio, saturationRatio, teamFitMultiplier,
                        diversityCount, saturationCount, gapCount, competencySaturation, teamSize,
                        personalityCompatibility);
            }
        }
    }
}
