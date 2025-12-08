package app.skillsoft.assessmentbackend.domain.dto.blueprint;

import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Blueprint configuration for OVERVIEW (Universal Baseline) assessment strategy.
 * 
 * Generates a "Competency Passport" with:
 * - Big Five personality profile
 * - O*NET cross-functional competency mappings
 * - ESCO transversal skill mappings
 * 
 * Uses Context Neutrality Filter to exclude role-specific questions.
 * Results are reusable across Job Fit and Team Fit scenarios.
 * 
 * JSON discriminator: "UNIVERSAL_BASELINE"
 */
public class OverviewBlueprint extends TestBlueprintDto {

    private static final long serialVersionUID = 1L;

    /**
     * List of competency IDs to include in the assessment.
     * At least one competency must be specified.
     */
    @NotNull(message = "Competency IDs are required")
    @Size(min = 1, message = "At least one competency must be specified")
    private List<UUID> competencyIds = new ArrayList<>();

    /**
     * Whether to include Big Five personality assessment questions.
     * When true, adds personality dimension questions to generate
     * the Big Five profile in the Competency Passport.
     */
    private boolean includeBigFive = true;

    // Constructors
    public OverviewBlueprint() {
        super();
        setStrategy(AssessmentGoal.OVERVIEW);
    }

    public OverviewBlueprint(List<UUID> competencyIds, boolean includeBigFive) {
        super(AssessmentGoal.OVERVIEW, null);
        this.competencyIds = competencyIds != null ? new ArrayList<>(competencyIds) : new ArrayList<>();
        this.includeBigFive = includeBigFive;
    }

    public OverviewBlueprint(List<UUID> competencyIds, boolean includeBigFive, AdaptivitySettings adaptivity) {
        super(AssessmentGoal.OVERVIEW, adaptivity);
        this.competencyIds = competencyIds != null ? new ArrayList<>(competencyIds) : new ArrayList<>();
        this.includeBigFive = includeBigFive;
    }

    // Getters and Setters
    public List<UUID> getCompetencyIds() {
        return competencyIds;
    }

    public void setCompetencyIds(List<UUID> competencyIds) {
        this.competencyIds = competencyIds != null ? new ArrayList<>(competencyIds) : new ArrayList<>();
    }

    public boolean isIncludeBigFive() {
        return includeBigFive;
    }

    public void setIncludeBigFive(boolean includeBigFive) {
        this.includeBigFive = includeBigFive;
    }

    @Override
    public TestBlueprintDto deepCopy() {
        OverviewBlueprint copy = new OverviewBlueprint();
        copy.setStrategy(this.getStrategy());
        copy.setCompetencyIds(new ArrayList<>(this.competencyIds));
        copy.setIncludeBigFive(this.includeBigFive);
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
        OverviewBlueprint that = (OverviewBlueprint) o;
        return includeBigFive == that.includeBigFive && 
               Objects.equals(competencyIds, that.competencyIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), competencyIds, includeBigFive);
    }

    @Override
    public String toString() {
        return "OverviewBlueprint{" +
                "strategy=" + getStrategy() +
                ", competencyIds=" + competencyIds +
                ", includeBigFive=" + includeBigFive +
                ", adaptivity=" + getAdaptivity() +
                '}';
    }
}
