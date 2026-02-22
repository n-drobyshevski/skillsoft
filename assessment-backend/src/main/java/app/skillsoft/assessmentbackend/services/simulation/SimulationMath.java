package app.skillsoft.assessmentbackend.services.simulation;

import app.skillsoft.assessmentbackend.domain.dto.simulation.SimulationProfile;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentQuestion;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Package-private utility for IRT-inspired probability calculations
 * used by the persona simulation engine.
 */
final class SimulationMath {

    static final double COMPETENCY_NOISE_AMPLITUDE = 0.10;
    static final double PROBABILITY_FLOOR = 0.01;
    static final double PROBABILITY_CEILING = 0.99;

    private SimulationMath() {}

    /**
     * Apply a logit-space shift to a probability.
     * Transforms p to logit space, adds shift, transforms back via sigmoid.
     * Result is clamped to [{@value PROBABILITY_FLOOR}, {@value PROBABILITY_CEILING}].
     *
     * @param probability base probability in (0, 1)
     * @param shift       logit-space shift (positive increases, negative decreases)
     * @return adjusted probability
     */
    static double applyLogitShift(double probability, double shift) {
        double p = Math.max(0.001, Math.min(0.999, probability));
        double logit = Math.log(p / (1.0 - p));
        double shifted = logit + shift;
        double result = 1.0 / (1.0 + Math.exp(-shifted));
        return Math.max(PROBABILITY_FLOOR, Math.min(PROBABILITY_CEILING, result));
    }

    /**
     * Compute a deterministic seed from simulation inputs.
     * Same inputs always produce the same seed for reproducible results.
     */
    static long computeSimulationSeed(
            List<AssessmentQuestion> questions,
            SimulationProfile profile,
            int abilityLevel
    ) {
        long seed = profile.ordinal() * 31L + abilityLevel;
        for (var q : questions) {
            seed = seed * 31 + q.getId().hashCode();
        }
        return seed;
    }

    /**
     * Compute per-competency noise offsets.
     * Each unique competency gets a deterministic value in
     * [-{@value COMPETENCY_NOISE_AMPLITUDE}, +{@value COMPETENCY_NOISE_AMPLITUDE}].
     */
    static Map<UUID, Double> computeCompetencyNoise(
            List<AssessmentQuestion> questions,
            long baseSeed
    ) {
        return questions.stream()
            .map(q -> q.getBehavioralIndicator().getCompetency().getId())
            .collect(Collectors.toSet())
            .stream()
            .collect(Collectors.toMap(
                compId -> compId,
                compId -> {
                    Random rng = new Random(baseSeed ^ compId.hashCode());
                    return (rng.nextDouble() * 2 - 1) * COMPETENCY_NOISE_AMPLITUDE;
                }
            ));
    }

    /**
     * Convert ability slider (0-100) to a logit-space modifier in [-2.0, +2.0].
     */
    static double abilityToModifier(int abilityLevel) {
        return (abilityLevel - 50) / 25.0;
    }
}
