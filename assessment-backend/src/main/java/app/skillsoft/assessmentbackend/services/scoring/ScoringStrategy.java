package app.skillsoft.assessmentbackend.services.scoring;

import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;
import app.skillsoft.assessmentbackend.domain.entities.TestAnswer;
import app.skillsoft.assessmentbackend.domain.entities.TestSession;

import java.util.List;

/**
 * Strategy interface for calculating test result scores.
 * 
 * Different scoring strategies implement different assessment goals:
 * - Overview: Universal Baseline / Competency Passport
 * - Job Fit: O*NET benchmark comparison
 * - Team Fit: Team gap analysis
 * 
 * Per ROADMAP.md Section 1.2 - implements Strategy Pattern for goal-based scoring.
 */
public interface ScoringStrategy {
    
    /**
     * Calculate the final result score for a completed test session.
     * 
     * @param session The completed test session
     * @param answers List of all answers from the session
     * @return ScoringResult with overall score and per-competency breakdown
     */
    ScoringResult calculate(TestSession session, List<TestAnswer> answers);
    
    /**
     * Get the assessment goal this strategy supports.
     * 
     * @return AssessmentGoal enum value
     */
    AssessmentGoal getSupportedGoal();
}
