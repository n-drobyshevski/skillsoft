package app.skillsoft.assessmentbackend.domain.dto.simulation;

import java.util.List;
import java.util.Map;

/**
 * Result DTO for test simulation/dry run.
 * Contains validation status, question composition, and sample run data.
 */
public record SimulationResultDto(
    /**
     * Whether the simulation completed successfully with valid configuration.
     */
    boolean valid,
    
    /**
     * Question composition breakdown.
     * Key format: "difficulty" or "competencyId:difficulty"
     * Value: count of questions
     */
    Map<String, Integer> composition,
    
    /**
     * Sample questions from the simulated test run.
     */
    List<QuestionSummaryDto> sampleQuestions,
    
    /**
     * Warnings and issues detected during simulation.
     */
    List<InventoryWarning> warnings,
    
    /**
     * Simulated score based on the profile used.
     */
    Double simulatedScore,
    
    /**
     * Estimated duration in minutes.
     */
    Integer estimatedDurationMinutes,
    
    /**
     * Total questions in the assembled test.
     */
    int totalQuestions,
    
    /**
     * Simulation profile used.
     */
    SimulationProfile profile
) {
    /**
     * Builder for constructing SimulationResultDto.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean valid = true;
        private Map<String, Integer> composition = Map.of();
        private List<QuestionSummaryDto> sampleQuestions = List.of();
        private List<InventoryWarning> warnings = List.of();
        private Double simulatedScore;
        private Integer estimatedDurationMinutes;
        private int totalQuestions;
        private SimulationProfile profile;

        public Builder valid(boolean valid) {
            this.valid = valid;
            return this;
        }

        public Builder composition(Map<String, Integer> composition) {
            this.composition = composition;
            return this;
        }

        public Builder sampleQuestions(List<QuestionSummaryDto> sampleQuestions) {
            this.sampleQuestions = sampleQuestions;
            return this;
        }

        public Builder warnings(List<InventoryWarning> warnings) {
            this.warnings = warnings;
            return this;
        }

        public Builder simulatedScore(Double simulatedScore) {
            this.simulatedScore = simulatedScore;
            return this;
        }

        public Builder estimatedDurationMinutes(Integer estimatedDurationMinutes) {
            this.estimatedDurationMinutes = estimatedDurationMinutes;
            return this;
        }

        public Builder totalQuestions(int totalQuestions) {
            this.totalQuestions = totalQuestions;
            return this;
        }

        public Builder profile(SimulationProfile profile) {
            this.profile = profile;
            return this;
        }

        public SimulationResultDto build() {
            return new SimulationResultDto(
                valid, composition, sampleQuestions, warnings,
                simulatedScore, estimatedDurationMinutes, totalQuestions, profile
            );
        }
    }

    /**
     * Create a failed simulation result.
     */
    public static SimulationResultDto failed(List<InventoryWarning> warnings) {
        return new SimulationResultDto(
            false, Map.of(), List.of(), warnings,
            null, null, 0, null
        );
    }
}
