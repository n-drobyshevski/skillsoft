package app.skillsoft.assessmentbackend.services.external;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for team profile and skill analysis.
 * 
 * Used in TEAM_FIT assessments to:
 * - Fetch team member skill profiles
 * - Calculate role saturation levels
 * - Identify skill gaps in the team
 * 
 * This is a mock interface - actual implementation would integrate
 * with team management systems or HR databases.
 */
public interface TeamService {

    /**
     * Team member profile with competency scores.
     */
    record TeamMemberProfile(
        UUID userId,
        String name,
        String role,
        /**
         * Map of competency ID to proficiency score (1-5 scale).
         */
        Map<UUID, Double> competencyScores,
        /**
         * Big Five personality traits if available.
         */
        Map<String, Double> personalityTraits
    ) {}

    /**
     * Aggregated team profile with saturation analysis.
     */
    record TeamProfile(
        UUID teamId,
        String teamName,
        List<TeamMemberProfile> members,
        /**
         * Map of competency ID to saturation level (0-1 scale).
         * 0 = no one has this skill, 1 = fully saturated
         */
        Map<UUID, Double> competencySaturation,
        /**
         * Map of trait name to team average.
         */
        Map<String, Double> averagePersonality,
        /**
         * Identified skill gaps (competencies with low saturation).
         */
        List<UUID> skillGaps
    ) {}

    /**
     * Fetch the team profile by team ID.
     * 
     * @param teamId The team's unique identifier
     * @return The team profile, or empty if not found
     */
    Optional<TeamProfile> getTeamProfile(UUID teamId);

    /**
     * Get saturation level for a specific competency in a team.
     * 
     * @param teamId The team ID
     * @param competencyId The competency to check
     * @return Saturation level (0-1), or empty if not calculated
     */
    Optional<Double> getSaturation(UUID teamId, UUID competencyId);

    /**
     * Get competencies with saturation below a threshold.
     * These represent skill gaps the team needs to fill.
     * 
     * @param teamId The team ID
     * @param threshold Saturation threshold (e.g., 0.3)
     * @return List of competency IDs below threshold
     */
    List<UUID> getUndersaturatedCompetencies(UUID teamId, double threshold);

    /**
     * Calculate fit score for a candidate joining a team.
     * 
     * @param teamId The team ID
     * @param candidateCompetencies Map of competency ID to candidate score
     * @return Fit score (0-100) based on how well candidate fills gaps
     */
    double calculateTeamFitScore(UUID teamId, Map<UUID, Double> candidateCompetencies);

    /**
     * Check if a team ID is valid and has profile data.
     * 
     * @param teamId The team ID to validate
     * @return true if the team exists and has data
     */
    boolean isValidTeam(UUID teamId);
}
