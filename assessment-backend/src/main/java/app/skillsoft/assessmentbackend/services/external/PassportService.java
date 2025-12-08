package app.skillsoft.assessmentbackend.services.external;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for accessing candidate's Competency Passport.
 * 
 * The Competency Passport stores reusable assessment results from
 * OVERVIEW (Universal Baseline) tests, enabling Delta Testing
 * where only gaps need to be reassessed.
 */
public interface PassportService {

    /**
     * Candidate's Competency Passport data.
     */
    record CompetencyPassport(
        UUID candidateId,
        /**
         * Map of competency ID to score (1-5 scale).
         */
        Map<UUID, Double> competencyScores,
        /**
         * Big Five personality profile.
         * Keys: OPENNESS, CONSCIENTIOUSNESS, EXTRAVERSION, AGREEABLENESS, NEUROTICISM
         */
        Map<String, Double> bigFiveProfile,
        /**
         * Timestamp of last assessment.
         */
        java.time.LocalDateTime lastAssessed,
        /**
         * Whether passport data is still valid (not expired).
         */
        boolean isValid
    ) {}

    /**
     * Get the Competency Passport for a candidate.
     * 
     * @param candidateId The candidate's unique identifier
     * @return The passport data, or empty if not available
     */
    Optional<CompetencyPassport> getPassport(UUID candidateId);

    /**
     * Get a specific competency score from the passport.
     * 
     * @param candidateId The candidate ID
     * @param competencyId The competency to look up
     * @return The score, or empty if not assessed
     */
    Optional<Double> getCompetencyScore(UUID candidateId, UUID competencyId);

    /**
     * Check if a candidate has valid passport data.
     * 
     * @param candidateId The candidate ID
     * @return true if valid passport exists
     */
    boolean hasValidPassport(UUID candidateId);

    /**
     * Save or update a candidate's Competency Passport.
     * 
     * @param passport The passport data to save
     */
    void savePassport(CompetencyPassport passport);
}
