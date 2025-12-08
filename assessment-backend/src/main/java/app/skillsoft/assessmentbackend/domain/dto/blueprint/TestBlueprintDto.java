package app.skillsoft.assessmentbackend.domain.dto.blueprint;

import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;
import app.skillsoft.assessmentbackend.domain.entities.BranchingMode;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.Objects;

/**
 * Abstract base class for polymorphic test blueprint configurations.
 * 
 * Uses Jackson's polymorphic type handling to serialize/deserialize
 * the correct concrete type based on the 'strategy' discriminator property.
 * 
 * Each subclass represents a specific assessment strategy:
 * - OverviewBlueprint (UNIVERSAL_BASELINE): Universal baseline for Competency Passport
 * - JobFitBlueprint (TARGETED_FIT): O*NET-based job matching
 * - TeamFitBlueprint (DYNAMIC_GAP_ANALYSIS): Team skill gap analysis
 * 
 * Stored as JSONB in PostgreSQL for flexible schema evolution.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "strategy",
    visible = true
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = OverviewBlueprint.class, name = "UNIVERSAL_BASELINE"),
    @JsonSubTypes.Type(value = JobFitBlueprint.class, name = "TARGETED_FIT"),
    @JsonSubTypes.Type(value = TeamFitBlueprint.class, name = "DYNAMIC_GAP_ANALYSIS")
})
public abstract class TestBlueprintDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Assessment strategy type - serves as the polymorphic discriminator.
     * Maps to AssessmentGoal enum values for consistency.
     */
    @NotNull(message = "Strategy is required")
    private AssessmentGoal strategy;

    /**
     * Adaptivity settings controlling question flow and branching behavior.
     */
    @Valid
    private AdaptivitySettings adaptivity;

    // Constructors
    protected TestBlueprintDto() {
        // Default constructor for Jackson deserialization
    }

    protected TestBlueprintDto(AssessmentGoal strategy, AdaptivitySettings adaptivity) {
        this.strategy = strategy;
        this.adaptivity = adaptivity;
    }

    // Getters and Setters
    public AssessmentGoal getStrategy() {
        return strategy;
    }

    public void setStrategy(AssessmentGoal strategy) {
        this.strategy = strategy;
    }

    public AdaptivitySettings getAdaptivity() {
        return adaptivity;
    }

    public void setAdaptivity(AdaptivitySettings adaptivity) {
        this.adaptivity = adaptivity;
    }

    /**
     * Create a deep copy of this blueprint.
     * Used when creating new template versions to ensure immutability.
     * 
     * @return A new instance with the same configuration
     */
    public abstract TestBlueprintDto deepCopy();

    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestBlueprintDto that = (TestBlueprintDto) o;
        return strategy == that.strategy && 
               Objects.equals(adaptivity, that.adaptivity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(strategy, adaptivity);
    }

    /**
     * Nested class for adaptivity/branching configuration.
     */
    public static class AdaptivitySettings implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * The branching mode controlling question flow.
         * Defaults to LINEAR for sequential question presentation.
         */
        private BranchingMode mode = BranchingMode.LINEAR;

        /**
         * Whether users can navigate back to previous questions.
         * May be restricted in adaptive modes for test integrity.
         */
        private boolean allowBacktracking = true;

        // Constructors
        public AdaptivitySettings() {
            // Default constructor
        }

        public AdaptivitySettings(BranchingMode mode, boolean allowBacktracking) {
            this.mode = mode;
            this.allowBacktracking = allowBacktracking;
        }

        // Getters and Setters
        public BranchingMode getMode() {
            return mode;
        }

        public void setMode(BranchingMode mode) {
            this.mode = mode;
        }

        public boolean isAllowBacktracking() {
            return allowBacktracking;
        }

        public void setAllowBacktracking(boolean allowBacktracking) {
            this.allowBacktracking = allowBacktracking;
        }

        /**
         * Create a deep copy of this settings object.
         */
        public AdaptivitySettings deepCopy() {
            return new AdaptivitySettings(this.mode, this.allowBacktracking);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AdaptivitySettings that = (AdaptivitySettings) o;
            return allowBacktracking == that.allowBacktracking && mode == that.mode;
        }

        @Override
        public int hashCode() {
            return Objects.hash(mode, allowBacktracking);
        }

        @Override
        public String toString() {
            return "AdaptivitySettings{" +
                    "mode=" + mode +
                    ", allowBacktracking=" + allowBacktracking +
                    '}';
        }
    }
}
