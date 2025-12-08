package app.skillsoft.assessmentbackend.domain.dto.blueprint;

import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Blueprint configuration for TEAM_FIT (Dynamic Gap Analysis) assessment strategy.
 * 
 * Uses ESCO URIs for skill normalization across team members.
 * Analyzes personality compatibility using Big Five from Competency Passport.
 * Implements Role Saturation scoring to identify team gaps and redundancies.
 * 
 * JSON discriminator: "DYNAMIC_GAP_ANALYSIS"
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

    @Override
    public TestBlueprintDto deepCopy() {
        TeamFitBlueprint copy = new TeamFitBlueprint();
        copy.setStrategy(this.getStrategy());
        copy.setTeamId(this.teamId);
        copy.setSaturationThreshold(this.saturationThreshold);
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
               Objects.equals(teamId, that.teamId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), teamId, saturationThreshold);
    }

    @Override
    public String toString() {
        return "TeamFitBlueprint{" +
                "strategy=" + getStrategy() +
                ", teamId=" + teamId +
                ", saturationThreshold=" + saturationThreshold +
                ", adaptivity=" + getAdaptivity() +
                '}';
    }
}
