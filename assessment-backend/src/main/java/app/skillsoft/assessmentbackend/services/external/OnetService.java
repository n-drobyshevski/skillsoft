package app.skillsoft.assessmentbackend.services.external;

import java.util.Map;
import java.util.Optional;

/**
 * Service interface for O*NET occupation data integration.
 * 
 * O*NET (Occupational Information Network) provides occupation-specific
 * competency benchmarks used in JOB_FIT assessments.
 * 
 * This is a mock interface - actual implementation would integrate
 * with O*NET Web Services API or a cached local database.
 */
public interface OnetService {

    /**
     * O*NET occupation profile containing benchmark competency levels.
     */
    record OnetProfile(
        String socCode,
        String occupationTitle,
        String description,
        /**
         * Map of competency/skill name to benchmark level (1-5 scale).
         * Example: {"Critical Thinking": 4.2, "Leadership": 3.8}
         */
        Map<String, Double> benchmarks,
        /**
         * Required knowledge areas and their importance levels.
         */
        Map<String, Double> knowledgeAreas,
        /**
         * Required skills and their importance levels.
         */
        Map<String, Double> skills,
        /**
         * Required abilities and their importance levels.
         */
        Map<String, Double> abilities
    ) {}

    /**
     * Fetch the O*NET profile for a given SOC code.
     * 
     * @param socCode Standard Occupational Classification code (e.g., "15-1252.00")
     * @return The occupation profile, or empty if not found
     */
    Optional<OnetProfile> getProfile(String socCode);

    /**
     * Get benchmark level for a specific competency in an occupation.
     * 
     * @param socCode The occupation SOC code
     * @param competencyName The competency name to look up
     * @return The benchmark level (1-5), or empty if not mapped
     */
    Optional<Double> getBenchmark(String socCode, String competencyName);

    /**
     * Calculate the gap between a candidate's score and the occupation benchmark.
     * 
     * @param socCode The occupation SOC code
     * @param competencyName The competency name
     * @param candidateScore The candidate's current score (1-5 scale)
     * @return The gap (benchmark - candidateScore), positive means deficit
     */
    default double calculateGap(String socCode, String competencyName, double candidateScore) {
        return getBenchmark(socCode, competencyName)
            .map(benchmark -> benchmark - candidateScore)
            .orElse(0.0);
    }

    /**
     * Check if a SOC code is valid and has profile data.
     * 
     * @param socCode The SOC code to validate
     * @return true if the code is valid and has data
     */
    boolean isValidSocCode(String socCode);
}
