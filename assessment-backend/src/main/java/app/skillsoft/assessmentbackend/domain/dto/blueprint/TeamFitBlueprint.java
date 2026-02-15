package app.skillsoft.assessmentbackend.domain.dto.blueprint;

import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;
import jakarta.validation.constraints.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Blueprint configuration for TEAM_FIT (Dynamic Gap Analysis) assessment strategy.
 *
 * Uses ESCO URIs for skill normalization across team members.
 * Analyzes personality compatibility using Big Five from Competency Passport.
 * Implements Role Saturation scoring to identify team gaps and redundancies.
 *
 * JSON discriminator: "TEAM_FIT"
 */
public class TeamFitBlueprint extends TestBlueprintDto {

    private static final long serialVersionUID = 1L;

    /**
     * UUID of the team being analyzed.
     * References the team entity for member skill aggregation.
     */
    @NotNull(message = "Team ID is required for team fit analysis")
    private UUID teamId;

    /**
     * Role saturation threshold (0.0 - 1.0).
     * 
     * Determines when a skill/role is considered "saturated" in the team:
     * - Values close to 0: Quickly marks roles as saturated (prefers diversity)
     * - Values close to 1: Allows more overlap before saturation (tolerates redundancy)
     * 
     * Default: 0.75 (75% overlap before a role is considered saturated)
     */
    private double saturationThreshold = 0.75;

    /**
     * Target role for the candidate in the team.
     * Used for display purposes and role-based competency weighting.
     * Examples: "Backend Developer", "Project Manager", "UX Designer"
     */
    private String targetRole;

    /**
     * Role-specific competency weights (competency UUID -> weight multiplier).
     * When populated, these weights are applied alongside ESCO/BigFive weights
     * to emphasize competencies critical for the target role.
     *
     * Weight range: 0.5 - 2.0 (default 1.0 if not specified).
     * Example: A "Project Manager" role might weight Leadership at 2.0 and Coding at 0.5.
     */
    private Map<UUID, Double> roleCompetencyWeights;

    // Constructors
    public TeamFitBlueprint() {
        super();
        setStrategy(AssessmentGoal.TEAM_FIT);
    }

    public TeamFitBlueprint(UUID teamId, double saturationThreshold) {
        super(AssessmentGoal.TEAM_FIT, null);
        this.teamId = teamId;
        this.saturationThreshold = saturationThreshold;
    }

    public TeamFitBlueprint(UUID teamId, double saturationThreshold, AdaptivitySettings adaptivity) {
        super(AssessmentGoal.TEAM_FIT, adaptivity);
        this.teamId = teamId;
        this.saturationThreshold = saturationThreshold;
    }

    public TeamFitBlueprint(UUID teamId, double saturationThreshold, String targetRole, Map<UUID, Double> roleCompetencyWeights) {
        super();
        setStrategy(AssessmentGoal.TEAM_FIT);
        this.teamId = teamId;
        this.saturationThreshold = saturationThreshold;
        this.targetRole = targetRole;
        this.roleCompetencyWeights = roleCompetencyWeights;
    }

    // Getters and Setters
    public UUID getTeamId() {
        return teamId;
    }

    public void setTeamId(UUID teamId) {
        this.teamId = teamId;
    }

    public double getSaturationThreshold() {
        return saturationThreshold;
    }

    public void setSaturationThreshold(double saturationThreshold) {
        this.saturationThreshold = saturationThreshold;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public Map<UUID, Double> getRoleCompetencyWeights() {
        return roleCompetencyWeights;
    }

    public void setRoleCompetencyWeights(Map<UUID, Double> roleCompetencyWeights) {
        this.roleCompetencyWeights = roleCompetencyWeights;
    }

    @Override
    public TestBlueprintDto deepCopy() {
        TeamFitBlueprint copy = new TeamFitBlueprint();
        copy.setStrategy(this.getStrategy());
        copy.setTeamId(this.teamId);
        copy.setSaturationThreshold(this.saturationThreshold);
        copy.setTargetRole(this.targetRole);
        if (this.roleCompetencyWeights != null) {
            copy.setRoleCompetencyWeights(new HashMap<>(this.roleCompetencyWeights));
        }
        if (this.getAdaptivity() != null) {
            copy.setAdaptivity(this.getAdaptivity().deepCopy());
        }
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TeamFitBlueprint that = (TeamFitBlueprint) o;
        return Double.compare(that.saturationThreshold, saturationThreshold) == 0 &&
               Objects.equals(teamId, that.teamId) &&
               Objects.equals(targetRole, that.targetRole) &&
               Objects.equals(roleCompetencyWeights, that.roleCompetencyWeights);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), teamId, saturationThreshold, targetRole, roleCompetencyWeights);
    }

    @Override
    public String toString() {
        return "TeamFitBlueprint{" +
                "strategy=" + getStrategy() +
                ", teamId=" + teamId +
                ", saturationThreshold=" + saturationThreshold +
                ", targetRole='" + targetRole + '\'' +
                ", roleCompetencyWeights=" + roleCompetencyWeights +
                ", adaptivity=" + getAdaptivity() +
                '}';
    }
}
