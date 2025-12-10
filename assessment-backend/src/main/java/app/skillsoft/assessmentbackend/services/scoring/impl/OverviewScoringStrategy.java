package app.skillsoft.assessmentbackend.services.scoring.impl;

import app.skillsoft.assessmentbackend.domain.dto.CompetencyScoreDto;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.repository.CompetencyRepository;
import app.skillsoft.assessmentbackend.services.scoring.ScoringResult;
import app.skillsoft.assessmentbackend.services.scoring.ScoringStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Scoring strategy for OVERVIEW (Universal Baseline / Competency Passport) assessments.
 * 
 * Implements Scenario A scoring logic:
 * 1. Normalize raw answer scores (Likert 1-5 → 0-1, SJT weights → 0-1)
 * 2. Aggregate by competency (sum normalized scores)
 * 3. Calculate percentage (average * 100)
 * 4. Generate competency score breakdown
 * 
 * Note: Big Five personality projection is handled on the frontend using
 * O*NET → Big Five mapping. This preserves server security while allowing
 * real-time recalibration on the client.
 * 
 * Per ROADMAP.md Section 1.2 - Universal Baseline Strategy
 */
@Service
public class OverviewScoringStrategy implements ScoringStrategy {
    
    private static final Logger log = LoggerFactory.getLogger(OverviewScoringStrategy.class);
    
    private final CompetencyRepository competencyRepository;
    
    public OverviewScoringStrategy(CompetencyRepository competencyRepository) {
        this.competencyRepository = competencyRepository;
    }
    
    @Override
    public ScoringResult calculate(TestSession session, List<TestAnswer> answers) {
        log.info("Calculating Scenario A (Overview) score for session: {}", session.getId());
        
        // Step 1: Normalize & Aggregate Scores by Competency
        Map<UUID, Double> rawCompetencyScores = new HashMap<>();
        Map<UUID, Integer> counts = new HashMap<>();
        Map<UUID, Integer> questionsAnswered = new HashMap<>();
        
        for (TestAnswer answer : answers) {
            // Skip unanswered or skipped questions
            if (answer.getIsSkipped() || answer.getAnsweredAt() == null) {
                continue;
            }
            
            UUID compId = answer.getQuestion()
                .getBehavioralIndicator()
                .getCompetency()
                .getId();
            
            double normalizedScore = normalize(answer);
            
            rawCompetencyScores.merge(compId, normalizedScore, Double::sum);
            counts.merge(compId, 1, Integer::sum);
            questionsAnswered.merge(compId, 1, Integer::sum);
        }
        
        // Step 2: Calculate Percentages and Create Score DTOs
        List<CompetencyScoreDto> finalScores = new ArrayList<>();
        double totalPercentage = 0.0;
        int competencyCount = 0;
        
        for (var entry : rawCompetencyScores.entrySet()) {
            UUID competencyId = entry.getKey();
            double sumScore = entry.getValue();
            int count = counts.get(competencyId);
            
            // Calculate average score (0-1 scale)
            double average = sumScore / count;
            
            // Convert to percentage (0-100 scale)
            double percentage = average * 100.0;
            
            // Get competency details
            Competency competency = competencyRepository.findById(competencyId).orElse(null);
            String competencyName = competency != null ? competency.getName() : "Unknown Competency";
            String onetCode = competency != null ? competency.getOnetCode() : null;
            
            // Calculate max score (count of questions * 1.0)
            double maxScore = count * 1.0;
            double actualScore = sumScore;
            
            CompetencyScoreDto scoreDto = new CompetencyScoreDto();
            scoreDto.setCompetencyId(competencyId);
            scoreDto.setCompetencyName(competencyName);
            scoreDto.setScore(actualScore);
            scoreDto.setMaxScore(maxScore);
            scoreDto.setPercentage(percentage);
            scoreDto.setQuestionsAnswered(questionsAnswered.get(competencyId));
            scoreDto.setOnetCode(onetCode); // Set O*NET code for Big Five projection
            
            finalScores.add(scoreDto);
            
            totalPercentage += percentage;
            competencyCount++;
            
            log.debug("Competency {}: {} questions, score {}/{} ({}%)", 
                competencyName, count, actualScore, maxScore, String.format("%.2f", percentage));
        }
        
        // Step 3: Calculate Overall Score
        double overallPercentage = competencyCount > 0 
            ? totalPercentage / competencyCount 
            : 0.0;
        
        double overallScore = competencyCount > 0 
            ? rawCompetencyScores.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum() / competencyCount
            : 0.0;
        
        log.info("Overall score calculated: {} ({}/100)", 
            String.format("%.2f", overallScore), String.format("%.2f", overallPercentage));
        
        // Step 4: Create Result
        ScoringResult result = new ScoringResult();
        result.setOverallScore(overallScore);
        result.setOverallPercentage(overallPercentage);
        result.setCompetencyScores(finalScores);
        result.setGoal(AssessmentGoal.OVERVIEW);
        
        // Pass/fail will be set by service layer based on template passing score
        
        return result;
    }
    
    /**
     * Normalize raw answer score to 0-1 scale.
     * 
     * Handles different question types:
     * - Likert (1-5): (value - 1) / 4
     * - SJT: pre-calculated weight (0-1)
     * - Multiple Choice: correct=1, incorrect=0
     * 
     * @param answer The test answer to normalize
     * @return Normalized score (0-1)
     */
    private double normalize(TestAnswer answer) {
        // Likert scale normalization (1-5 → 0-1)
        if (answer.getLikertValue() != null) {
            int likert = answer.getLikertValue();
            // Clamp to valid range
            likert = Math.max(1, Math.min(5, likert));
            return (likert - 1.0) / 4.0;
        }
        
        // SJT weight normalization (already 0-1, but check score field)
        if (answer.getScore() != null) {
            double score = answer.getScore();
            // Ensure in range [0, 1]
            return Math.max(0.0, Math.min(1.0, score));
        }
        
        // Multiple choice: correct=1, incorrect=0
        // Check if question type is MC and option is correct
        if (answer.getQuestion().getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
            // Simplified: assume score field is populated by answer submission logic
            return answer.getScore() != null ? answer.getScore() : 0.0;
        }
        
        // Default to 0 if no score available
        log.warn("No score available for answer {}, defaulting to 0", answer.getId());
        return 0.0;
    }
    
    @Override
    public AssessmentGoal getSupportedGoal() {
        return AssessmentGoal.OVERVIEW;
    }
}
