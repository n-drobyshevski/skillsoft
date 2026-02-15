package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.dto.QuestionScoreDto;
import app.skillsoft.assessmentbackend.domain.dto.TestResultDto;
import app.skillsoft.assessmentbackend.domain.dto.TestResultSummaryDto;
import app.skillsoft.assessmentbackend.domain.dto.TrendDataPointDto;
import app.skillsoft.assessmentbackend.domain.dto.comparison.CandidateComparisonDto;
import app.skillsoft.assessmentbackend.services.CandidateComparisonService;
import app.skillsoft.assessmentbackend.services.TestResultService;
import app.skillsoft.assessmentbackend.services.scoring.QuestionScoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Test Result management.
 * 
 * Provides endpoints for retrieving test results and statistics.
 * Results are created automatically when a session is completed.
 * 
 * API Base Path: /api/v1/tests/results
 */
@RestController
@RequestMapping("/api/v1/tests/results")
public class TestResultController {

    private static final Logger logger = LoggerFactory.getLogger(TestResultController.class);

    private final TestResultService testResultService;
    private final QuestionScoreService questionScoreService;
    private final CandidateComparisonService candidateComparisonService;

    public TestResultController(TestResultService testResultService,
                                QuestionScoreService questionScoreService,
                                CandidateComparisonService candidateComparisonService) {
        this.testResultService = testResultService;
        this.questionScoreService = questionScoreService;
        this.candidateComparisonService = candidateComparisonService;
    }

    // ==================== ADMIN OPERATIONS ====================
    // NOTE: Literal path mappings (/compare, /report, /recent) MUST be declared
    // before the wildcard /{resultId} to prevent Spring from matching "compare"
    // as a resultId path variable.

    /**
     * Compare multiple candidate results for the same TEAM_FIT template.
     * Returns ranked summaries, competency comparisons, gap coverage, and complementarity scores.
     *
     * @param templateId The template UUID all results belong to
     * @param resultIds  List of result UUIDs to compare (2-5)
     * @return Comparison DTO or 400 if validation fails
     */
    @GetMapping("/compare")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<CandidateComparisonDto> compareCandidates(
            @RequestParam UUID templateId,
            @RequestParam List<UUID> resultIds) {
        logger.info("GET /api/v1/tests/results/compare?templateId={}&resultIds={}", templateId, resultIds.size());
        try {
            CandidateComparisonDto comparison = candidateComparisonService.compareResults(resultIds, templateId);
            return ResponseEntity.ok(comparison);
        } catch (IllegalArgumentException e) {
            logger.warn("Comparison validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get results within a date range (for reporting).
     *
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return List of results in the range
     */
    @GetMapping("/report")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<List<TestResultSummaryDto>> getResultsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        logger.info("GET /api/v1/tests/results/report?startDate={}&endDate={}", startDate, endDate);

        List<TestResultSummaryDto> results = testResultService.findByDateRange(startDate, endDate);
        logger.info("Found {} results between {} and {}", results.size(), startDate, endDate);

        return ResponseEntity.ok(results);
    }

    /**
     * Get recent results (for admin dashboard).
     *
     * @param limit Maximum number of results to return
     * @return List of recent results
     */
    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<List<TestResultSummaryDto>> getRecentResults(
            @RequestParam(defaultValue = "10") int limit) {
        logger.info("GET /api/v1/tests/results/recent?limit={}", limit);

        List<TestResultSummaryDto> results = testResultService.getRecentResults(limit);
        logger.info("Returning {} recent results", results.size());

        return ResponseEntity.ok(results);
    }

    // ==================== RESULT RETRIEVAL ====================

    /**
     * Get a result by ID.
     *
     * @param resultId Result UUID
     * @return Result details or 404 if not found
     */
    @GetMapping("/{resultId:[0-9a-f\\-]{36}}")
    @PreAuthorize("@sessionSecurity.isResultOwner(#resultId)")
    public ResponseEntity<TestResultDto> getResultById(@PathVariable UUID resultId) {
        logger.info("GET /api/v1/tests/results/{}", resultId);
        
        return testResultService.findById(resultId)
                .map(result -> {
                    logger.info("Found result for session: {}", result.sessionId());
                    return ResponseEntity.ok(result);
                })
                .orElseGet(() -> {
                    logger.warn("Result not found: {}", resultId);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Get result for a specific session.
     *
     * @param sessionId Session UUID
     * @return Result details or 404 if not found
     */
    @GetMapping("/session/{sessionId}")
    @PreAuthorize("@sessionSecurity.isSessionOwner(#sessionId)")
    public ResponseEntity<TestResultDto> getResultBySession(@PathVariable UUID sessionId) {
        logger.info("GET /api/v1/tests/results/session/{}", sessionId);
        
        return testResultService.findBySessionId(sessionId)
                .map(result -> {
                    logger.info("Found result for session {}, score: {}%", sessionId, result.overallPercentage());
                    return ResponseEntity.ok(result);
                })
                .orElseGet(() -> {
                    logger.warn("No result found for session: {}", sessionId);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Get the percentile rank for a result.
     *
     * Calculates where this result falls compared to all results
     * for the same template.
     *
     * @param resultId Result UUID
     * @return Percentile value (0-100)
     */
    @GetMapping("/{resultId:[0-9a-f\\-]{36}}/percentile")
    @PreAuthorize("@sessionSecurity.isResultOwner(#resultId)")
    public ResponseEntity<Integer> getPercentile(@PathVariable UUID resultId) {
        logger.info("GET /api/v1/tests/results/{}/percentile", resultId);

        try {
            int percentile = testResultService.calculatePercentile(resultId);
            logger.info("Percentile for result {}: {}", resultId, percentile);
            return ResponseEntity.ok(percentile);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                logger.warn("Result not found: {}", resultId);
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }

    /**
     * Get question-level scores for a specific indicator within a result.
     *
     * This is a lazy-loaded endpoint for detailed question breakdown.
     * Called when a user expands an indicator row in the results view.
     * Includes correct answers for learning/review purposes.
     *
     * @param resultId Result UUID
     * @param indicatorId Behavioral indicator UUID
     * @return List of question scores with correct answers
     */
    @GetMapping("/{resultId:[0-9a-f\\-]{36}}/indicators/{indicatorId}/questions")
    @PreAuthorize("@sessionSecurity.isResultOwner(#resultId)")
    public ResponseEntity<List<QuestionScoreDto>> getIndicatorQuestionScores(
            @PathVariable UUID resultId,
            @PathVariable UUID indicatorId) {
        logger.info("GET /api/v1/tests/results/{}/indicators/{}/questions", resultId, indicatorId);

        try {
            List<QuestionScoreDto> questionScores = questionScoreService.getQuestionScoresForIndicator(
                    resultId, indicatorId);
            logger.info("Returning {} question scores for indicator {} in result {}",
                    questionScores.size(), indicatorId, resultId);
            return ResponseEntity.ok(questionScores);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to get question scores: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // ==================== USER RESULTS ====================

    /**
     * Get all results for a user with pagination.
     *
     * @param clerkUserId User's Clerk ID
     * @param pageable Pagination parameters
     * @return Page of user's result summaries
     */
    @GetMapping("/user/{clerkUserId}")
    @PreAuthorize("@sessionSecurity.canAccessUserData(#clerkUserId)")
    public ResponseEntity<Page<TestResultSummaryDto>> getUserResults(
            @PathVariable String clerkUserId,
            @PageableDefault(size = 20, sort = "completedAt", direction = Sort.Direction.DESC) 
            Pageable pageable) {
        logger.info("GET /api/v1/tests/results/user/{}", clerkUserId);
        
        Page<TestResultSummaryDto> results = testResultService.findByUser(clerkUserId, pageable);
        logger.info("Found {} results for user {}", results.getTotalElements(), clerkUserId);
        
        return ResponseEntity.ok(results);
    }

    /**
     * Get all results for a user ordered by date.
     *
     * @param clerkUserId User's Clerk ID
     * @return List of result summaries
     */
    @GetMapping("/user/{clerkUserId}/all")
    @PreAuthorize("@sessionSecurity.canAccessUserData(#clerkUserId)")
    public ResponseEntity<List<TestResultSummaryDto>> getAllUserResults(@PathVariable String clerkUserId) {
        logger.info("GET /api/v1/tests/results/user/{}/all", clerkUserId);
        
        List<TestResultSummaryDto> results = testResultService.findByUserOrderByDate(clerkUserId);
        logger.info("Found {} total results for user {}", results.size(), clerkUserId);
        
        return ResponseEntity.ok(results);
    }

    /**
     * Get user's results for a specific template.
     *
     * @param clerkUserId User's Clerk ID
     * @param templateId Template UUID
     * @return List of results for this template
     */
    @GetMapping("/user/{clerkUserId}/template/{templateId}")
    @PreAuthorize("@sessionSecurity.canAccessUserData(#clerkUserId)")
    public ResponseEntity<List<TestResultSummaryDto>> getUserResultsForTemplate(
            @PathVariable String clerkUserId,
            @PathVariable UUID templateId) {
        logger.info("GET /api/v1/tests/results/user/{}/template/{}", clerkUserId, templateId);
        
        List<TestResultSummaryDto> results = testResultService.findByUserAndTemplate(clerkUserId, templateId);
        logger.info("Found {} results for user {} on template {}", results.size(), clerkUserId, templateId);
        
        return ResponseEntity.ok(results);
    }

    /**
     * Get the most recent result for a user on a template.
     *
     * @param clerkUserId User's Clerk ID
     * @param templateId Template UUID
     * @return Latest result or 404 if none
     */
    @GetMapping("/user/{clerkUserId}/template/{templateId}/latest")
    @PreAuthorize("@sessionSecurity.canAccessUserData(#clerkUserId)")
    public ResponseEntity<TestResultDto> getLatestResult(
            @PathVariable String clerkUserId,
            @PathVariable UUID templateId) {
        logger.info("GET /api/v1/tests/results/user/{}/template/{}/latest", clerkUserId, templateId);
        
        return testResultService.findLatestByUserAndTemplate(clerkUserId, templateId)
                .map(result -> {
                    logger.info("Found latest result: {}", result.id());
                    return ResponseEntity.ok(result);
                })
                .orElseGet(() -> {
                    logger.info("No results found for user {} on template {}", clerkUserId, templateId);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Get historical trend data for a user.
     * Returns lightweight time-series data points with competency-level scores.
     * Optionally filter by template to see progress on a specific assessment.
     *
     * @param clerkUserId User's Clerk ID
     * @param templateId Optional template UUID filter
     * @return List of trend data points ordered by date
     */
    @GetMapping("/user/{clerkUserId}/history")
    @PreAuthorize("@sessionSecurity.canAccessUserData(#clerkUserId)")
    public ResponseEntity<List<TrendDataPointDto>> getUserHistory(
            @PathVariable String clerkUserId,
            @RequestParam(required = false) UUID templateId) {
        logger.info("GET /api/v1/tests/results/user/{}/history?templateId={}", clerkUserId, templateId);

        List<TrendDataPointDto> history = testResultService.getUserHistory(clerkUserId, templateId);
        logger.info("Found {} history data points for user {}", history.size(), clerkUserId);

        return ResponseEntity.ok(history);
    }

    /**
     * Get passed results for a user.
     *
     * @param clerkUserId User's Clerk ID
     * @return List of results where user passed
     */
    @GetMapping("/user/{clerkUserId}/passed")
    @PreAuthorize("@sessionSecurity.canAccessUserData(#clerkUserId)")
    public ResponseEntity<List<TestResultSummaryDto>> getPassedResults(@PathVariable String clerkUserId) {
        logger.info("GET /api/v1/tests/results/user/{}/passed", clerkUserId);
        
        List<TestResultSummaryDto> results = testResultService.findPassedByUser(clerkUserId);
        logger.info("Found {} passed results for user {}", results.size(), clerkUserId);
        
        return ResponseEntity.ok(results);
    }

    // ==================== STATISTICS ====================

    /**
     * Get user statistics.
     *
     * @param clerkUserId User's Clerk ID
     * @return User's test statistics
     */
    @GetMapping("/user/{clerkUserId}/statistics")
    @PreAuthorize("@sessionSecurity.canAccessUserData(#clerkUserId)")
    public ResponseEntity<TestResultService.UserTestStatistics> getUserStatistics(
            @PathVariable String clerkUserId) {
        logger.info("GET /api/v1/tests/results/user/{}/statistics", clerkUserId);
        
        TestResultService.UserTestStatistics stats = testResultService.getUserStatistics(clerkUserId);
        logger.info("User {} statistics: {} tests completed, avg score: {}%", 
                clerkUserId, stats.totalTests(), stats.averageScore());
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Get template statistics.
     * 
     * @param templateId Template UUID
     * @return Template's result statistics
     */
    @GetMapping("/template/{templateId}/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<TestResultService.TemplateTestStatistics> getTemplateStatistics(
            @PathVariable UUID templateId) {
        logger.info("GET /api/v1/tests/results/template/{}/statistics", templateId);
        
        TestResultService.TemplateTestStatistics stats = testResultService.getTemplateStatistics(templateId);
        logger.info("Template {} statistics: {} completions, avg score: {}%", 
                templateId, stats.totalAttempts(), stats.averageScore());
        
        return ResponseEntity.ok(stats);
    }

}
