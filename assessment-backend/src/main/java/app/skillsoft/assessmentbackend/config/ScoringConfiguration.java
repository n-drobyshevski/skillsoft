package app.skillsoft.assessmentbackend.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for scoring calculation parameters.
 *
 * Allows externalization of all weight multipliers and thresholds used in
 * scoring strategies, enabling tuning without code changes.
 *
 * Usage in application.properties:
 * <pre>
 * scoring.weights.onet-boost=1.2
 * scoring.weights.esco-boost=1.15
 * scoring.weights.big-five-boost=1.1
 * scoring.thresholds.job-fit.base-threshold=0.5
 * </pre>
 *
 * @see app.skillsoft.assessmentbackend.services.scoring.impl.JobFitScoringStrategy
 * @see app.skillsoft.assessmentbackend.services.scoring.impl.TeamFitScoringStrategy
 */
@Configuration
@ConfigurationProperties(prefix = "scoring")
@Validated
@Data
public class ScoringConfiguration {

    /**
     * Weight multipliers for different competency mapping types.
     */
    @Valid
    private Weights weights = new Weights();

    /**
     * Threshold configurations for different assessment goals.
     */
    @Valid
    private Thresholds thresholds = new Thresholds();

    /**
     * Weight multipliers applied to competency scores based on their mapping type.
     * Higher weights prioritize competencies with richer metadata.
     */
    @Data
    public static class Weights {
        /**
         * Weight multiplier for competencies with O*NET SOC codes.
         * Applied in Job Fit scoring to prioritize occupation-aligned competencies.
         * Default: 1.2 (20% boost)
         */
        @DecimalMin("1.0")
        @DecimalMax("2.0")
        private double onetBoost = 1.2;

        /**
         * Weight multiplier for competencies with ESCO URIs.
         * Applied in Team Fit scoring for standardized skill normalization.
         * Default: 1.15 (15% boost)
         */
        @DecimalMin("1.0")
        @DecimalMax("2.0")
        private double escoBoost = 1.15;

        /**
         * Weight multiplier for competencies with Big Five personality mappings.
         * Applied in Team Fit scoring for personality compatibility analysis.
         * Default: 1.1 (10% boost)
         */
        @DecimalMin("1.0")
        @DecimalMax("2.0")
        private double bigFiveBoost = 1.1;
    }

    /**
     * Threshold configurations for scoring calculations.
     */
    @Data
    public static class Thresholds {
        /**
         * Job Fit (Scenario B) specific thresholds.
         */
        @Valid
        private JobFit jobFit = new JobFit();

        /**
         * Team Fit (Scenario C) specific thresholds.
         */
        @Valid
        private TeamFit teamFit = new TeamFit();

        /**
         * Thresholds for Job Fit (Targeted Fit / O*NET Benchmark) scoring.
         */
        @Data
        public static class JobFit {
            /**
             * Base threshold for determining competency pass/fail.
             * Represents the minimum normalized score (0-1) required without strictness adjustment.
             * Default: 0.5 (50%)
             */
            @DecimalMin("0.0")
            @DecimalMax("1.0")
            private double baseThreshold = 0.5;

            /**
             * Maximum adjustment applied based on strictness level.
             * The actual adjustment = (strictnessLevel / 100) * strictnessMaxAdjustment
             * Default: 0.3 (up to 30% additional threshold requirement)
             */
            @DecimalMin("0.0")
            @DecimalMax("1.0")
            private double strictnessMaxAdjustment = 0.3;
        }

        /**
         * Thresholds for Team Fit (Dynamic Gap Analysis) scoring.
         */
        @Data
        public static class TeamFit {
            /**
             * Minimum diversity ratio to qualify for diversity bonus.
             * Diversity ratio = diversityContributors / totalCompetencies
             * Default: 0.4 (40% of competencies must contribute to diversity)
             */
            @DecimalMin("0.0")
            @DecimalMax("1.0")
            private double diversityBonusThreshold = 0.4;

            /**
             * Maximum saturation ratio before penalty is applied.
             * Saturation ratio = saturationContributors / totalCompetencies
             * Default: 0.8 (penalty when >80% competencies are saturated)
             */
            @DecimalMin("0.0")
            @DecimalMax("1.0")
            private double saturationPenaltyThreshold = 0.8;

            /**
             * Multiplier bonus for teams with good diversity/saturation balance.
             * Applied when diversityRatio > diversityBonusThreshold AND saturationRatio < saturationPenaltyThreshold
             * Default: 1.1 (10% score boost)
             */
            @DecimalMin("0.5")
            @DecimalMax("1.5")
            private double diversityBonus = 1.1;

            /**
             * Multiplier penalty for teams with too much overlap.
             * Applied when saturationRatio > saturationPenaltyThreshold
             * Default: 0.9 (10% score reduction)
             */
            @DecimalMin("0.5")
            @DecimalMax("1.5")
            private double saturationPenalty = 0.9;

            /**
             * Threshold for determining if a competency is saturated.
             * Competencies with average scores >= this threshold count toward saturation.
             * Default: 0.75 (75%)
             */
            @DecimalMin("0.0")
            @DecimalMax("1.0")
            private double saturationThreshold = 0.75;

            /**
             * Threshold for competencies contributing to diversity.
             * Competencies with average scores >= this threshold (but < saturationThreshold) add diversity.
             * Default: 0.5 (50%)
             */
            @DecimalMin("0.0")
            @DecimalMax("1.0")
            private double diversityThreshold = 0.5;

            /**
             * Minimum overall percentage required to pass Team Fit assessment.
             * Candidate must have adjustedPercentage >= (passThreshold * 100)
             * Default: 0.6 (60%)
             */
            @DecimalMin("0.0")
            @DecimalMax("1.0")
            private double passThreshold = 0.6;

            /**
             * Minimum diversity ratio required to pass Team Fit assessment.
             * Candidate must have diversityRatio >= minDiversityRatio
             * Default: 0.3 (30%)
             */
            @DecimalMin("0.0")
            @DecimalMax("1.0")
            private double minDiversityRatio = 0.3;
        }
    }
}
