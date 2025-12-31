package app.skillsoft.assessmentbackend.services.scoring.impl;

import app.skillsoft.assessmentbackend.domain.dto.CompetencyScoreDto;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.services.scoring.CompetencyBatchLoader;
import app.skillsoft.assessmentbackend.services.scoring.ScoreNormalizer;
import app.skillsoft.assessmentbackend.services.scoring.ScoringResult;
import app.skillsoft.assessmentbackend.services.scoring.ScoringStrategy;
import app.skillsoft.assessmentbackend.util.LoggingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
@Transactional(readOnly = true)
public class OverviewScoringStrategy implements ScoringStrategy {

    private static final Logger log = LoggerFactory.getLogger(OverviewScoringStrategy.class);

    private final CompetencyBatchLoader competencyBatchLoader;
    private final ScoreNormalizer scoreNormalizer;

    public OverviewScoringStrategy(CompetencyBatchLoader competencyBatchLoader, ScoreNormalizer scoreNormalizer) {
        this.competencyBatchLoader = competencyBatchLoader;
        this.scoreNormalizer = scoreNormalizer;
    }
    
    @Override
    public ScoringResult calculate(TestSession session, List<TestAnswer> answers) {
        // Set session context for all scoring log messages
        LoggingContext.setSessionId(session.getId());
        LoggingContext.setOperation("overview-scoring");

        log.info("Calculating Scenario A (Overview) score: session={} answers={} user={}",
                session.getId(), answers.size(), session.getClerkUserId());

        // Batch load all competencies upfront to prevent N+1 queries
        Map<UUID, Competency> competencyCache = competencyBatchLoader.loadCompetenciesForAnswers(answers);

        // Step 1: Normalize & Aggregate Scores by Competency
        Map<UUID, Double> rawCompetencyScores = new HashMap<>();
        Map<UUID, Integer> counts = new HashMap<>();
        Map<UUID, Integer> questionsAnswered = new HashMap<>();

        for (TestAnswer answer : answers) {
            // Skip unanswered or skipped questions
            if (answer.getIsSkipped() || answer.getAnsweredAt() == null) {
                continue;
            }

            Optional<UUID> compIdOpt = competencyBatchLoader.extractCompetencyIdSafe(answer);
            if (compIdOpt.isEmpty()) {
                log.warn("Skipping answer {} - unable to extract competency ID", answer.getId());
                continue;
            }
            UUID compId = compIdOpt.get();

            double normalizedScore = scoreNormalizer.normalize(answer);

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

            // Get competency details from preloaded cache (prevents N+1 queries)
            Competency competency = competencyBatchLoader.getFromCache(competencyCache, competencyId);
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

    @Override
    public AssessmentGoal getSupportedGoal() {
        return AssessmentGoal.OVERVIEW;
    }
}
