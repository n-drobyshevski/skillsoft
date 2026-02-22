package app.skillsoft.assessmentbackend.domain.dto.simulation;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Result DTO for test simulation/dry run.
 * Contains validation status, question composition, and sample run data.
 */
public record SimulationResultDto(
    boolean valid,
    Map<String, Integer> composition,
    List<QuestionSummaryDto> sampleQuestions,
    List<InventoryWarning> warnings,
    Double simulatedScore,
    Integer estimatedDurationMinutes,
    int totalQuestions,
    SimulationProfile profile,
    Map<UUID, CompetencySimulationScore> competencyScores,
    Integer abilityLevel
) {

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
        private Map<UUID, CompetencySimulationScore> competencyScores;
        private Integer abilityLevel;

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

        public Builder competencyScores(Map<UUID, CompetencySimulationScore> competencyScores) {
            this.competencyScores = competencyScores;
            return this;
        }

        public Builder abilityLevel(Integer abilityLevel) {
            this.abilityLevel = abilityLevel;
            return this;
        }

        public SimulationResultDto build() {
            return new SimulationResultDto(
                valid, composition, sampleQuestions, warnings,
                simulatedScore, estimatedDurationMinutes, totalQuestions, profile,
                competencyScores, abilityLevel
            );
        }
    }

    public static SimulationResultDto failed(List<InventoryWarning> warnings) {
        return new SimulationResultDto(
            false, Map.of(), List.of(), warnings,
            null, null, 0, null, null, null
        );
    }
}
