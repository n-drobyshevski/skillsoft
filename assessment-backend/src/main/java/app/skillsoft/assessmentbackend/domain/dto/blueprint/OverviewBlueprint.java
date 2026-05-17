package app.skillsoft.assessmentbackend.domain.dto.blueprint;

import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;
import app.skillsoft.assessmentbackend.domain.entities.DifficultyLevel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * JSON discriminator: "OVERVIEW"
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

    /**
     * Number of questions to select per behavioral indicator.
     * Default is 3, which provides sufficient data for reliable scoring
     * while keeping assessment length manageable.
     */
    @Min(value = 1, message = "At least 1 question per indicator is required")
    @Max(value = 10, message = "Maximum 10 questions per indicator allowed")
    private int questionsPerIndicator = 3;

    /**
     * Preferred difficulty level for question selection.
     * Default is INTERMEDIATE for balanced baseline assessment.
     * Questions of other difficulties are used as fallback when preferred not available.
     */
    private DifficultyLevel preferredDifficulty = DifficultyLevel.INTERMEDIATE;

    /**
     * Whether to shuffle the final question order.
     * Recommended true to prevent clustering by competency.
     */
    private boolean shuffleQuestions = true;

    /**
     * Per-competency weight multipliers (competency UUID -> weight).
     * Weight range: 0.5 - 2.0 (default 1.0 if absent).
     * Higher weight = more questions allocated to that competency's indicators.
     */
    private Map<UUID, Double> competencyWeights;

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

    public int getQuestionsPerIndicator() {
        return questionsPerIndicator;
    }

    public void setQuestionsPerIndicator(int questionsPerIndicator) {
        this.questionsPerIndicator = questionsPerIndicator;
    }

    public DifficultyLevel getPreferredDifficulty() {
        return preferredDifficulty;
    }

    public void setPreferredDifficulty(DifficultyLevel preferredDifficulty) {
        this.preferredDifficulty = preferredDifficulty;
    }

    public boolean isShuffleQuestions() {
        return shuffleQuestions;
    }

    public void setShuffleQuestions(boolean shuffleQuestions) {
        this.shuffleQuestions = shuffleQuestions;
    }

    public Map<UUID, Double> getCompetencyWeights() {
        return competencyWeights;
    }

    public void setCompetencyWeights(Map<UUID, Double> competencyWeights) {
        this.competencyWeights = competencyWeights;
    }

    @Override
    public TestBlueprintDto deepCopy() {
        OverviewBlueprint copy = new OverviewBlueprint();
        copy.setStrategy(this.getStrategy());
        copy.setCompetencyIds(new ArrayList<>(this.competencyIds));
        copy.setIncludeBigFive(this.includeBigFive);
        copy.setQuestionsPerIndicator(this.questionsPerIndicator);
        copy.setPreferredDifficulty(this.preferredDifficulty);
        copy.setShuffleQuestions(this.shuffleQuestions);
        if (this.competencyWeights != null) {
            copy.setCompetencyWeights(new HashMap<>(this.competencyWeights));
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
        OverviewBlueprint that = (OverviewBlueprint) o;
        return includeBigFive == that.includeBigFive &&
               questionsPerIndicator == that.questionsPerIndicator &&
               shuffleQuestions == that.shuffleQuestions &&
               preferredDifficulty == that.preferredDifficulty &&
               Objects.equals(competencyIds, that.competencyIds) &&
               Objects.equals(competencyWeights, that.competencyWeights);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), competencyIds, includeBigFive,
                questionsPerIndicator, preferredDifficulty, shuffleQuestions,
                competencyWeights);
    }

    @Override
    public String toString() {
        return "OverviewBlueprint{" +
                "strategy=" + getStrategy() +
                ", competencyIds=" + competencyIds +
                ", includeBigFive=" + includeBigFive +
                ", questionsPerIndicator=" + questionsPerIndicator +
                ", preferredDifficulty=" + preferredDifficulty +
                ", shuffleQuestions=" + shuffleQuestions +
                ", competencyWeights=" + competencyWeights +
                ", adaptivity=" + getAdaptivity() +
                '}';
    }
}
