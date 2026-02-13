package app.skillsoft.assessmentbackend.domain.dto.blueprint;

import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import java.util.Objects;

/**
 * Blueprint configuration for JOB_FIT (Targeted Fit) assessment strategy.
 *
 * Uses O*NET SOC code to load benchmark requirements for specific occupations.
 * Implements Delta Testing - reuses Competency Passport data if available.
 * Applies Weighted Cosine Similarity scoring against occupation profile.
 *
 * JSON discriminator: "JOB_FIT"
 */
public class JobFitBlueprint extends TestBlueprintDto {

    private static final long serialVersionUID = 1L;

    /**
     * O*NET Standard Occupational Classification (SOC) code.
     * Format: XX-XXXX.XX (e.g., "15-1252.00" for Software Developers)
     * 
     * Pattern breakdown:
     * - First two digits: Major group
     * - Hyphen separator
     * - Four digits: Detailed occupation
     * - Decimal point
     * - Two digits: O*NET-specific code
     */
    @Pattern(
        regexp = "\\d{2}-\\d{4}\\.\\d{2}",
        message = "O*NET SOC code must be in format XX-XXXX.XX (e.g., 15-1252.00)"
    )
    private String onetSocCode;

    /**
     * Strictness level for job fit matching (0-100).
     *
     * Lower values (0-30): Lenient matching, accepts broader skill ranges
     * Medium values (31-70): Standard matching, balanced requirements
     * Higher values (71-100): Strict matching, requires close skill alignment
     */
    @Min(value = 0, message = "Strictness level must be at least 0")
    @Max(value = 100, message = "Strictness level must not exceed 100")
    private int strictnessLevel = 50;

    /**
     * The Clerk User ID of the candidate taking this assessment.
     *
     * Used for Delta Testing: If the candidate has a Competency Passport
     * from previous assessments, the assembler will calculate gaps between
     * the job requirements and existing scores, prioritizing questions
     * for competencies with the largest deficits.
     *
     * If null or no passport exists, the system treats all competencies
     * as having score=0 (full assessment mode).
     *
     * This field is typically set at runtime by TestSessionService when
     * starting a new session, not stored in the template's blueprint.
     */
    private String candidateClerkUserId;

    // Constructors
    public JobFitBlueprint() {
        super();
        setStrategy(AssessmentGoal.JOB_FIT);
    }

    public JobFitBlueprint(String onetSocCode, int strictnessLevel) {
        super(AssessmentGoal.JOB_FIT, null);
        this.onetSocCode = onetSocCode;
        this.strictnessLevel = strictnessLevel;
    }

    public JobFitBlueprint(String onetSocCode, int strictnessLevel, AdaptivitySettings adaptivity) {
        super(AssessmentGoal.JOB_FIT, adaptivity);
        this.onetSocCode = onetSocCode;
        this.strictnessLevel = strictnessLevel;
    }

    // Getters and Setters
    public String getOnetSocCode() {
        return onetSocCode;
    }

    public void setOnetSocCode(String onetSocCode) {
        this.onetSocCode = onetSocCode;
    }

    public int getStrictnessLevel() {
        return strictnessLevel;
    }

    public void setStrictnessLevel(int strictnessLevel) {
        this.strictnessLevel = strictnessLevel;
    }

    public String getCandidateClerkUserId() {
        return candidateClerkUserId;
    }

    public void setCandidateClerkUserId(String candidateClerkUserId) {
        this.candidateClerkUserId = candidateClerkUserId;
    }

    @Override
    public TestBlueprintDto deepCopy() {
        JobFitBlueprint copy = new JobFitBlueprint();
        copy.setStrategy(this.getStrategy());
        copy.setOnetSocCode(this.onetSocCode);
        copy.setStrictnessLevel(this.strictnessLevel);
        copy.setCandidateClerkUserId(this.candidateClerkUserId);
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
        JobFitBlueprint that = (JobFitBlueprint) o;
        return strictnessLevel == that.strictnessLevel &&
               Objects.equals(onetSocCode, that.onetSocCode) &&
               Objects.equals(candidateClerkUserId, that.candidateClerkUserId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), onetSocCode, strictnessLevel, candidateClerkUserId);
    }

    @Override
    public String toString() {
        return "JobFitBlueprint{" +
                "strategy=" + getStrategy() +
                ", onetSocCode='" + onetSocCode + '\'' +
                ", strictnessLevel=" + strictnessLevel +
                ", candidateClerkUserId='" + candidateClerkUserId + '\'' +
                ", adaptivity=" + getAdaptivity() +
                '}';
    }
}
