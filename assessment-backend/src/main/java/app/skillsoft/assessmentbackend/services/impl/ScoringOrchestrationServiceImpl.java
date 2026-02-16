package app.skillsoft.assessmentbackend.services.impl;

import app.skillsoft.assessmentbackend.domain.dto.CompetencyScoreDto;
import app.skillsoft.assessmentbackend.domain.dto.TestResultDto;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.events.resilience.ResilienceFallbackEvent;
import app.skillsoft.assessmentbackend.events.scoring.ScoringAuditEvent;
import app.skillsoft.assessmentbackend.events.scoring.ScoringCompletedEvent;
import app.skillsoft.assessmentbackend.events.scoring.ScoringFailedEvent;
import app.skillsoft.assessmentbackend.events.scoring.ScoringStartedEvent;
import app.skillsoft.assessmentbackend.exception.ResourceNotFoundException;
import app.skillsoft.assessmentbackend.repository.TestAnswerRepository;
import app.skillsoft.assessmentbackend.repository.TestResultRepository;
import app.skillsoft.assessmentbackend.repository.TestSessionRepository;
import app.skillsoft.assessmentbackend.services.ScoringOrchestrationService;
import app.skillsoft.assessmentbackend.services.scoring.ConfidenceIntervalCalculator;
import app.skillsoft.assessmentbackend.services.scoring.ResponseConsistencyAnalyzer;
import app.skillsoft.assessmentbackend.services.scoring.ScoringResult;
import app.skillsoft.assessmentbackend.services.scoring.ScoringStrategy;
import app.skillsoft.assessmentbackend.services.scoring.SubscalePercentileCalculator;
import app.skillsoft.assessmentbackend.util.LoggingContext;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Implementation of ScoringOrchestrationService that runs scoring in a separate transaction.
 *
 * Key Design Decisions:
 * - Uses REQUIRES_NEW propagation to ensure scoring is independent of session completion
 * - Uses Resilience4j @Retry for transient failure handling (DB timeouts, network issues)
 * - Falls back to creating PENDING result if all retry attempts fail
 * - Publishes observability events for monitoring and alerting
 *
 * Transaction Isolation:
 * When completeSession() calls calculateAndSaveResult(), a new transaction is started.
 * If scoring fails:
 * - The session remains COMPLETED (outer transaction already committed)
 * - A PENDING result is created for later retry
 * - The fallback method handles graceful degradation
 */
@Service
public class ScoringOrchestrationServiceImpl implements ScoringOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(ScoringOrchestrationServiceImpl.class);

    private final TestSessionRepository sessionRepository;
    private final TestAnswerRepository answerRepository;
    private final TestResultRepository resultRepository;
    private final List<ScoringStrategy> scoringStrategies;
    private final ApplicationEventPublisher eventPublisher;
    private final ConfidenceIntervalCalculator confidenceIntervalCalculator;
    private final SubscalePercentileCalculator subscalePercentileCalculator;
    private final ResponseConsistencyAnalyzer responseConsistencyAnalyzer;

    public ScoringOrchestrationServiceImpl(
            TestSessionRepository sessionRepository,
            TestAnswerRepository answerRepository,
            TestResultRepository resultRepository,
            List<ScoringStrategy> scoringStrategies,
            ApplicationEventPublisher eventPublisher,
            ConfidenceIntervalCalculator confidenceIntervalCalculator,
            SubscalePercentileCalculator subscalePercentileCalculator,
            ResponseConsistencyAnalyzer responseConsistencyAnalyzer) {
        this.sessionRepository = sessionRepository;
        this.answerRepository = answerRepository;
        this.resultRepository = resultRepository;
        this.scoringStrategies = scoringStrategies;
        this.eventPublisher = eventPublisher;
        this.confidenceIntervalCalculator = confidenceIntervalCalculator;
        this.subscalePercentileCalculator = subscalePercentileCalculator;
        this.responseConsistencyAnalyzer = responseConsistencyAnalyzer;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retry(name = "scoringCalculation", fallbackMethod = "scoringFallback")
    public TestResultDto calculateAndSaveResult(UUID sessionId) {
        // Set operation context for scoring
        LoggingContext.setSessionId(sessionId);
        LoggingContext.setOperation("scoring");
        Instant scoringStartTime = Instant.now();

        log.info("Starting scoring calculation in new transaction for session={}", sessionId);

        // Fetch session with template for scoring
        TestSession session = sessionRepository.findByIdWithTemplate(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));

        // Enrich logging context
        LoggingContext.setUserId(session.getClerkUserId());
        LoggingContext.setTemplateId(session.getTemplate().getId());

        log.info("Calculating results for session={} goal={} user={}",
                session.getId(), session.getTemplate().getGoal(), session.getClerkUserId());

        List<TestAnswer> answers = answerRepository.findBySession_Id(session.getId());

        // Get template goal for strategy selection
        AssessmentGoal goal = session.getTemplate().getGoal();

        // Publish scoring started event
        eventPublisher.publishEvent(ScoringStartedEvent.beforeResult(
                session.getId(),
                goal,
                answers.size()
        ));

        // Calculate statistics
        long answered = answers.stream().filter(a -> !a.getIsSkipped() && a.getAnsweredAt() != null).count();
        long skipped = answers.stream().filter(TestAnswer::getIsSkipped).count();
        int totalTime = answers.stream()
                .mapToInt(a -> a.getTimeSpentSeconds() != null ? a.getTimeSpentSeconds() : 0)
                .sum();

        log.debug("Session statistics: answered={} skipped={} totalTimeSeconds={}",
                answered, skipped, totalTime);

        // Find appropriate scoring strategy
        ScoringStrategy strategy = scoringStrategies.stream()
                .filter(s -> s.getSupportedGoal() == goal)
                .findFirst()
                .orElse(null);

        ScoringResult scoringResult;
        if (strategy != null) {
            log.info("Using scoring strategy={} for goal={}",
                    strategy.getClass().getSimpleName(), goal);
            scoringResult = strategy.calculate(session, answers);
        } else {
            log.warn("No scoring strategy found for goal={}, using legacy calculation", goal);
            // Fallback to legacy scoring
            scoringResult = calculateLegacyScore(session, answers);
        }

        // Enrich competency scores with confidence intervals (post-processing)
        confidenceIntervalCalculator.enrichWithConfidenceIntervals(scoringResult.getCompetencyScores());

        // Enrich competency scores with per-competency percentile ranks
        subscalePercentileCalculator.enrichWithPercentiles(
                scoringResult.getCompetencyScores(), session.getTemplate().getId());

        // Run response consistency analysis on all answers (all assessment goals)
        ResponseConsistencyAnalyzer.ConsistencyResult consistencyResult =
                responseConsistencyAnalyzer.analyze(answers);
        scoringResult.setConsistencyScore(consistencyResult.consistencyScore());
        scoringResult.setConsistencyFlags(consistencyResult.flags());

        // Create result entity
        TestResult result = new TestResult(session, session.getClerkUserId());
        result.setOverallScore(scoringResult.getOverallScore());
        result.setOverallPercentage(scoringResult.getOverallPercentage());
        result.setCompetencyScores(scoringResult.getCompetencyScores());
        result.setQuestionsAnswered((int) answered);
        result.setQuestionsSkipped((int) skipped);
        result.setTotalTimeSeconds(totalTime);
        result.setCompletedAt(LocalDateTime.now());
        result.setStatus(ResultStatus.COMPLETED);

        // Set Big Five profile (only populated for TEAM_FIT goal)
        result.setBigFiveProfile(scoringResult.getBigFiveProfile());

        // Set extended metrics (e.g., TeamFitMetrics for TEAM_FIT goal, confidence for JOB_FIT)
        Map<String, Object> extendedMetrics = new LinkedHashMap<>();

        if (scoringResult.getTeamFitMetrics() != null) {
            extendedMetrics.putAll(convertTeamFitMetricsToMap(scoringResult.getTeamFitMetrics()));
        }

        // Propagate decision confidence metrics (populated by JOB_FIT scoring)
        if (scoringResult.getDecisionConfidence() != null) {
            extendedMetrics.put("decisionConfidence", scoringResult.getDecisionConfidence());
            extendedMetrics.put("confidenceLevel", scoringResult.getConfidenceLevel());
            extendedMetrics.put("confidenceMessage", scoringResult.getConfidenceMessage());
        }

        // Propagate response consistency metrics (populated for all goals)
        if (scoringResult.getConsistencyScore() != null) {
            extendedMetrics.put("consistencyScore", scoringResult.getConsistencyScore());
            extendedMetrics.put("consistencyFlags", scoringResult.getConsistencyFlags());
            extendedMetrics.put("speedAnomalyRate", consistencyResult.speedAnomalyRate());
            extendedMetrics.put("straightLiningRate", consistencyResult.straightLiningRate());
        }

        if (!extendedMetrics.isEmpty()) {
            result.setExtendedMetrics(extendedMetrics);
        }

        // Calculate passed/failed
        result.calculatePassed(session.getTemplate().getPassingScore());

        // Calculate percentile based on historical results for this template
        Integer percentile = calculatePercentile(session.getTemplate().getId(), result.getOverallPercentage());
        result.setPercentile(percentile);

        TestResult saved = resultRepository.save(result);

        // Publish scoring completed event
        eventPublisher.publishEvent(ScoringCompletedEvent.fromStart(
                session.getId(),
                saved.getId(),
                goal,
                saved.getOverallScore(),
                Boolean.TRUE.equals(saved.getPassed()),
                scoringStartTime
        ));

        // Publish scoring audit event for traceability (persisted asynchronously)
        publishAuditEvent(session, saved, goal, strategy, scoringResult,
                answers.size(), (int) answered, (int) skipped, scoringStartTime);

        log.info("Scoring completed successfully for session={} resultId={} score={}%",
                sessionId, saved.getId(), saved.getOverallPercentage());

        return toResultDto(saved, session);
    }

    /**
     * Fallback method for scoring calculation when all retry attempts fail.
     *
     * This method is called by Resilience4j when the scoring calculation fails after
     * all configured retry attempts. It creates a PENDING TestResult that can be
     * retried later by a scheduled job or manual intervention.
     *
     * Implementation notes:
     * - Logs the error with full context for debugging
     * - Publishes ScoringFailedEvent for observability/alerting
     * - Publishes ResilienceFallbackEvent for resilience metrics
     * - Creates a PENDING TestResult with basic session metadata
     * - Returns the pending result DTO so the user knows their test was received
     *
     * @param sessionId The session ID that failed scoring
     * @param exception The exception that caused the final failure
     * @return TestResultDto with PENDING status for later retry
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TestResultDto scoringFallback(UUID sessionId, Exception exception) {
        Instant failureTime = Instant.now();

        log.error("Scoring calculation exhausted all retry attempts for session={}. " +
                  "Creating PENDING result for later retry. Error: {}",
                sessionId, exception.getMessage(), exception);

        // Publish resilience fallback event for metrics
        // Note: max-attempts is 3 from application.properties, total attempts = initial + retries
        eventPublisher.publishEvent(ResilienceFallbackEvent.forScoringFallback(
                sessionId,
                3, // Total attempts (configured in application.properties)
                exception
        ));

        // Fetch session for creating pending result
        TestSession session = sessionRepository.findByIdWithTemplate(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));

        AssessmentGoal goal = session.getTemplate().getGoal();

        // Publish ScoringFailedEvent for observability and alerting
        eventPublisher.publishEvent(ScoringFailedEvent.fromException(
                session.getId(),
                goal,
                exception,
                failureTime
        ));

        // Fetch answers to capture basic statistics even for pending result
        List<TestAnswer> answers = answerRepository.findBySession_Id(session.getId());
        long answered = answers.stream()
                .filter(a -> !a.getIsSkipped() && a.getAnsweredAt() != null)
                .count();
        long skipped = answers.stream()
                .filter(TestAnswer::getIsSkipped)
                .count();
        int totalTime = answers.stream()
                .mapToInt(a -> a.getTimeSpentSeconds() != null ? a.getTimeSpentSeconds() : 0)
                .sum();

        // Create a PENDING result for later retry
        TestResult pendingResult = new TestResult(session, session.getClerkUserId());
        pendingResult.setStatus(ResultStatus.PENDING);
        pendingResult.setQuestionsAnswered((int) answered);
        pendingResult.setQuestionsSkipped((int) skipped);
        pendingResult.setTotalTimeSeconds(totalTime);
        // Scores will be null - to be calculated on retry
        pendingResult.setOverallScore(null);
        pendingResult.setOverallPercentage(null);
        pendingResult.setCompetencyScores(null);
        pendingResult.setPassed(null);
        pendingResult.setPercentile(null);
        pendingResult.setCompletedAt(LocalDateTime.now());

        TestResult saved = resultRepository.save(pendingResult);

        log.info("Created PENDING result id={} for session={} - will be retried by scheduled job",
                saved.getId(), session.getId());

        return toResultDto(saved, session);
    }

    /**
     * Calculate the percentile rank for a given score based on historical results.
     * Percentile indicates what percentage of test takers scored below this score.
     *
     * @param templateId The template ID to compare against
     * @param score The current score to calculate percentile for
     * @return Percentile rank (0-100), or 50 if no historical data exists
     */
    private Integer calculatePercentile(UUID templateId, Double score) {
        if (score == null) {
            log.debug("Cannot calculate percentile: score is null");
            return null;
        }

        try {
            // Get count of results below this score
            long belowCount = resultRepository.countResultsBelowScore(templateId, score);

            // Get total count of results for this template
            long totalCount = resultRepository.countResultsByTemplateId(templateId);

            // Handle edge cases
            if (totalCount == 0) {
                // First result for this template - default to 50th percentile
                log.debug("First result for template {}, defaulting to 50th percentile", templateId);
                return 50;
            }

            if (totalCount == 1) {
                // Only one result (the current one being saved)
                // Return 50 as baseline
                log.debug("Only one result for template {}, defaulting to 50th percentile", templateId);
                return 50;
            }

            // Calculate percentile: (count below / total) * 100
            // Using (totalCount - 1) to exclude the current result from denominator
            double percentile = ((double) belowCount / (totalCount - 1)) * 100;

            // Round and clamp to 0-100 range
            int result = (int) Math.round(percentile);
            result = Math.max(0, Math.min(100, result));

            log.debug("Calculated percentile for template {}: {} (below: {}, total: {})",
                    templateId, result, belowCount, totalCount);

            return result;
        } catch (Exception e) {
            log.warn("Error calculating percentile for template {}: {}", templateId, e.getMessage());
            return null;
        }
    }

    /**
     * Legacy scoring calculation for backward compatibility.
     * Used when no specific strategy is available for the goal.
     * Calculates basic competency scores by grouping answers by their question's competency.
     */
    private ScoringResult calculateLegacyScore(TestSession session, List<TestAnswer> answers) {
        Double totalScore = answerRepository.sumScoreBySessionId(session.getId());
        Double maxScore = answerRepository.sumMaxScoreBySessionId(session.getId());

        double percentage = 0.0;
        if (maxScore != null && maxScore > 0) {
            percentage = (totalScore != null ? totalScore / maxScore : 0) * 100;
        }

        ScoringResult result = new ScoringResult();
        result.setOverallScore(totalScore != null ? totalScore : 0.0);
        result.setOverallPercentage(percentage);
        result.setGoal(session.getTemplate().getGoal());

        // Calculate basic competency scores even in legacy mode
        List<CompetencyScoreDto> competencyScores = calculateLegacyCompetencyScores(answers);
        result.setCompetencyScores(competencyScores);

        return result;
    }

    /**
     * Calculate competency scores for legacy scoring mode.
     * Groups answers by their question's behavioral indicator's competency and calculates averages.
     *
     * @param answers List of test answers
     * @return List of competency scores
     */
    private List<CompetencyScoreDto> calculateLegacyCompetencyScores(List<TestAnswer> answers) {
        // Map to track scores per competency: competencyId -> (totalScore, count, maxScore)
        Map<UUID, double[]> competencyAggregates = new HashMap<>();
        Map<UUID, String> competencyNames = new HashMap<>();
        Map<UUID, String> competencyOnetCodes = new HashMap<>();

        for (TestAnswer answer : answers) {
            // Skip unanswered questions
            if (answer.getIsSkipped() || answer.getScore() == null) {
                continue;
            }

            AssessmentQuestion question = answer.getQuestion();
            if (question == null) {
                continue;
            }

            BehavioralIndicator indicator = question.getBehavioralIndicator();
            if (indicator == null) {
                continue;
            }

            Competency competency = indicator.getCompetency();
            if (competency == null) {
                continue;
            }

            UUID compId = competency.getId();

            // Initialize aggregates if first encounter
            if (!competencyAggregates.containsKey(compId)) {
                competencyAggregates.put(compId, new double[]{0.0, 0.0, 0.0}); // totalScore, count, maxScore
                competencyNames.put(compId, competency.getName());
                // Get O*NET code if available
                if (competency.getOnetCode() != null) {
                    competencyOnetCodes.put(compId, competency.getOnetCode());
                }
            }

            // Aggregate scores
            double[] aggregate = competencyAggregates.get(compId);
            aggregate[0] += answer.getScore() != null ? answer.getScore() : 0.0;
            aggregate[1] += 1.0;
            aggregate[2] += answer.getMaxScore() != null ? answer.getMaxScore() : 1.0;
        }

        // Convert aggregates to CompetencyScoreDto
        List<CompetencyScoreDto> competencyScores = new ArrayList<>();
        for (Map.Entry<UUID, double[]> entry : competencyAggregates.entrySet()) {
            UUID compId = entry.getKey();
            double[] aggregate = entry.getValue();
            double totalScore = aggregate[0];
            int count = (int) aggregate[1];
            double maxScore = aggregate[2];

            double compPercentage = maxScore > 0 ? (totalScore / maxScore) * 100 : 0.0;

            CompetencyScoreDto dto = new CompetencyScoreDto(
                    compId,
                    competencyNames.get(compId),
                    totalScore,
                    maxScore,
                    compPercentage
            );
            dto.setQuestionsAnswered(count);
            dto.setOnetCode(competencyOnetCodes.get(compId));

            competencyScores.add(dto);
        }

        log.debug("Calculated {} competency scores in legacy mode", competencyScores.size());
        return competencyScores;
    }

    /**
     * Publish audit event with scoring snapshot for traceability.
     * Extracts indicator weights from competency score DTOs.
     */
    private void publishAuditEvent(TestSession session, TestResult saved,
                                    AssessmentGoal goal, ScoringStrategy strategy,
                                    ScoringResult scoringResult,
                                    int totalAnswers, int answeredCount, int skippedCount,
                                    Instant scoringStartTime) {
        try {
            // Extract indicator weights from the scoring result
            Map<String, Double> indicatorWeights = new LinkedHashMap<>();
            if (scoringResult.getCompetencyScores() != null) {
                for (var cs : scoringResult.getCompetencyScores()) {
                    if (cs.getIndicatorScores() != null) {
                        for (var is : cs.getIndicatorScores()) {
                            if (is.getIndicatorId() != null) {
                                indicatorWeights.put(is.getIndicatorId().toString(),
                                        is.getWeight() != null ? is.getWeight() : 1.0);
                            }
                        }
                    }
                }
            }

            eventPublisher.publishEvent(ScoringAuditEvent.from(
                    session.getId(),
                    saved.getId(),
                    session.getClerkUserId(),
                    session.getTemplate().getId(),
                    goal,
                    strategy != null ? strategy.getClass().getSimpleName() : "LegacyScoring",
                    saved.getOverallScore(),
                    saved.getOverallPercentage(),
                    saved.getPassed(),
                    saved.getPercentile(),
                    scoringResult.getCompetencyScores(),
                    indicatorWeights,
                    null, // config snapshot - can be enriched later
                    totalAnswers,
                    answeredCount,
                    skippedCount,
                    scoringStartTime
            ));
        } catch (Exception e) {
            // Audit event failure should never prevent scoring from completing
            log.warn("Failed to publish scoring audit event for session={}: {}",
                    session.getId(), e.getMessage());
        }
    }

    /**
     * Convert TestResult entity to DTO.
     */
    private TestResultDto toResultDto(TestResult result, TestSession session) {
        return new TestResultDto(
                result.getId(),
                result.getSessionId(),
                session.getTemplate().getId(),
                session.getTemplate().getName(),
                result.getClerkUserId(),
                result.getOverallScore(),
                result.getOverallPercentage(),
                result.getPercentile(),
                result.getPassed(),
                result.getCompetencyScores(),
                result.getTotalTimeSeconds(),
                result.getQuestionsAnswered(),
                result.getQuestionsSkipped(),
                session.getQuestionOrder() != null ? session.getQuestionOrder().size() : 0,
                result.getCompletedAt(),
                result.getStatus(),
                result.getBigFiveProfile(),
                result.getExtendedMetrics()
        );
    }

    /**
     * Convert TeamFitMetrics to a Map for JSONB storage.
     * This allows flexible storage without requiring a separate entity.
     *
     * @param metrics The TeamFitMetrics object to convert
     * @return Map representation suitable for JSONB storage
     */
    private Map<String, Object> convertTeamFitMetricsToMap(ScoringResult.TeamFitMetrics metrics) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("diversityRatio", metrics.getDiversityRatio());
        map.put("saturationRatio", metrics.getSaturationRatio());
        map.put("teamFitMultiplier", metrics.getTeamFitMultiplier());
        map.put("diversityCount", metrics.getDiversityCount());
        map.put("saturationCount", metrics.getSaturationCount());
        map.put("gapCount", metrics.getGapCount());
        map.put("competencySaturation", metrics.getCompetencySaturation());
        map.put("teamSize", metrics.getTeamSize());
        map.put("personalityCompatibility", metrics.getPersonalityCompatibility());
        return map;
    }
}
