package app.skillsoft.assessmentbackend.domain.entities;

/**
 * Assessment goal types for TestTemplate.
 * Per ROADMAP.md Section 1.C and 1.2 - defines the scoring strategy and test assembly mechanics.
 * 
 * Each goal determines:
 * - How questions are selected (Context filters, difficulty adjustment)
 * - How results are scored (Strategy Pattern)
 * - What output is generated (Competency Passport, Job Fit Score, Team Fit Analysis)
 */
public enum AssessmentGoal {
    /**
     * Scenario A: Universal Baseline (Triple-Standard Aggregation)
     * Generates a "Competency Passport" with Big Five, O*NET, and ESCO mappings.
     * Uses Context Neutrality Filter to exclude role-specific questions.
     * Results are reusable across Job Fit and Team Fit scenarios.
     */
    OVERVIEW("Universal Baseline", "Generates Competency Passport with Big Five personality profile"),
    
    /**
     * Scenario B: Job Fit (O*NET Benchmark Injection)
     * Uses O*NET SOC code to load benchmark requirements.
     * Implements Delta Testing - reuses Competency Passport data if available.
     * Applies Weighted Cosine Similarity scoring.
     */
    JOB_FIT("Job Fit Assessment", "Compares candidate against O*NET occupation benchmarks"),
    
    /**
     * Scenario C: Team Fit (ESCO Gap Analysis)
     * Uses ESCO URIs for skill normalization across team members.
     * Analyzes personality compatibility using Big Five from Passport.
     * Implements Role Saturation scoring to identify team gaps.
     */
    TEAM_FIT("Team Fit Analysis", "Analyzes skill gaps and personality fit within a team");

    private final String displayName;
    private final String description;

    AssessmentGoal(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
