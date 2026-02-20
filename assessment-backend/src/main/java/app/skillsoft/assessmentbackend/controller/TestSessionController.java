package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.dto.*;
import app.skillsoft.assessmentbackend.domain.entities.SessionStatus;
import app.skillsoft.assessmentbackend.services.TestSessionService;
import app.skillsoft.assessmentbackend.services.diagnostics.TemplateDiagnosticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST Controller for Test Session management.
 * 
 * Provides endpoints for the complete test-taking lifecycle:
 * - Starting a new test session
 * - Submitting answers
 * - Navigating between questions
 * - Completing or abandoning sessions
 * 
 * API Base Path: /api/v1/tests/sessions
 */
@RestController
@RequestMapping("/api/v1/tests/sessions")
public class TestSessionController {

    private static final Logger logger = LoggerFactory.getLogger(TestSessionController.class);

    private final TestSessionService testSessionService;
    private final TemplateDiagnosticsService diagnosticsService;

    public TestSessionController(
            TestSessionService testSessionService,
            TemplateDiagnosticsService diagnosticsService) {
        this.testSessionService = testSessionService;
        this.diagnosticsService = diagnosticsService;
    }

    // ==================== SESSION LIFECYCLE ====================

    /**
     * Start a new test session.
     *
     * Creates a new session for the given template and user.
     * Generates the question order based on template configuration.
     *
     * @param request Contains templateId and clerkUserId
     * @return Created session with 201 status
     */
    @PostMapping
    @PreAuthorize("@sessionSecurity.canAccessUserData(#request.clerkUserId())")
    public ResponseEntity<TestSessionDto> startSession(
            @Valid @RequestBody StartTestSessionRequest request) {
        logger.debug("POST /api/v1/tests/sessions - Starting session for template: {}, user: {}",
                request.templateId(), request.clerkUserId());

        // Let GlobalExceptionHandler handle exceptions
        TestSessionDto session = testSessionService.startSession(request);
        logger.debug("Started session with id: {}", session.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }

    /**
     * Check if a template is ready to start a test session.
     *
     * This pre-flight check validates that all competencies in the template
     * have sufficient assessment questions. Call this before startSession
     * to provide users with actionable feedback if the test cannot start.
     *
     * @param templateId Template UUID to check
     * @return Readiness response with per-competency health status
     */
    @Operation(
        summary = "Check template readiness",
        description = "Pre-flight check to validate template has sufficient questions for all competencies"
    )
    @ApiResponse(responseCode = "200", description = "Readiness check completed")
    @ApiResponse(responseCode = "404", description = "Template not found")
    @GetMapping("/templates/{templateId}/readiness")
    public ResponseEntity<TemplateReadinessResponse> checkTemplateReadiness(
            @PathVariable UUID templateId) {
        logger.debug("GET /api/v1/tests/sessions/templates/{}/readiness", templateId);

        TemplateReadinessResponse readiness = testSessionService.checkTemplateReadiness(templateId);
        logger.debug("Template {} readiness: {}", templateId, readiness.ready() ? "READY" : "NOT READY");

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(readiness);
    }

    /**
     * Get the assembly progress for a template.
     *
     * Used during session start to track long-running assembly operations.
     * Clients can poll this endpoint to get real-time progress updates
     * during test assembly for large templates with many competencies.
     *
     * @param templateId Template UUID being assembled
     * @return Current assembly progress or 404 if no assembly is in progress
     */
    @Operation(
        summary = "Get assembly progress",
        description = "Poll assembly progress during test session start for templates with many competencies"
    )
    @ApiResponse(responseCode = "200", description = "Assembly progress retrieved")
    @ApiResponse(responseCode = "404", description = "No assembly in progress for this template")
    @GetMapping("/templates/{templateId}/assembly-progress")
    public ResponseEntity<AssemblyProgressDto> getAssemblyProgress(@PathVariable UUID templateId) {
        logger.debug("GET /api/v1/tests/sessions/templates/{}/assembly-progress", templateId);

        return testSessionService.getAssemblyProgress(templateId)
                .map(progress -> {
                    AssemblyProgressDto dto = AssemblyProgressDto.from(progress);
                    logger.debug("Assembly progress for template {}: {}% (phase: {})",
                            templateId, dto.percentComplete(), dto.phase());
                    return ResponseEntity.ok()
                            .cacheControl(CacheControl.noStore())
                            .body(dto);
                })
                .orElseGet(() -> {
                    logger.debug("No active assembly found for template {}", templateId);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Get a session by ID.
     *
     * @param sessionId Session UUID
     * @return Session details or 404 if not found
     */
    @GetMapping("/{sessionId}")
    @PreAuthorize("@sessionSecurity.isSessionOwner(#sessionId)")
    public ResponseEntity<TestSessionDto> getSession(@PathVariable UUID sessionId) {
        logger.debug("GET /api/v1/tests/sessions/{}", sessionId);

        return testSessionService.findById(sessionId)
                .map(session -> ResponseEntity.ok()
                        .cacheControl(CacheControl.noStore())
                        .body(session))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Complete a test session.
     *
     * Calculates scores and generates the test result.
     * Session must be in IN_PROGRESS status.
     *
     * @param sessionId Session UUID to complete
     * @return Test result with scores
     */
    @PostMapping("/{sessionId}/complete")
    @PreAuthorize("@sessionSecurity.isSessionOwner(#sessionId)")
    public ResponseEntity<TestResultDto> completeSession(@PathVariable UUID sessionId) {
        logger.debug("POST /api/v1/tests/sessions/{}/complete", sessionId);

        // Let GlobalExceptionHandler handle exceptions
        TestResultDto result = testSessionService.completeSession(sessionId);
        logger.info("Completed session {}, score: {}%", sessionId, result.overallPercentage());
        return ResponseEntity.ok(result);
    }

    /**
     * Abandon a test session.
     *
     * Marks the session as abandoned without calculating results.
     *
     * @param sessionId Session UUID to abandon
     * @return Updated session or 404 if not found
     */
    @PostMapping("/{sessionId}/abandon")
    @PreAuthorize("@sessionSecurity.isSessionOwner(#sessionId)")
    public ResponseEntity<TestSessionDto> abandonSession(@PathVariable UUID sessionId) {
        logger.debug("POST /api/v1/tests/sessions/{}/abandon", sessionId);

        // Let GlobalExceptionHandler handle exceptions
        TestSessionDto session = testSessionService.abandonSession(sessionId);
        logger.info("Abandoned session: {}", sessionId);
        return ResponseEntity.ok(session);
    }

    // ==================== ANSWER SUBMISSION ====================

    /**
     * Submit an answer for a question.
     *
     * Validates the answer and updates the session progress.
     *
     * @param sessionId Session UUID
     * @param request Answer submission request
     * @return Submitted answer details
     */
    @PostMapping("/{sessionId}/answers")
    @PreAuthorize("@sessionSecurity.isSessionOwner(#sessionId)")
    public ResponseEntity<TestAnswerDto> submitAnswer(
            @PathVariable UUID sessionId,
            @Valid @RequestBody SubmitAnswerRequest request) {
        logger.debug("POST /api/v1/tests/sessions/{}/answers - Question: {}",
                sessionId, request.questionId());

        // Ensure the sessionId in path matches the request body
        if (!sessionId.equals(request.sessionId())) {
            logger.warn("Session ID mismatch: path={}, body={}", sessionId, request.sessionId());
            return ResponseEntity.badRequest().build();
        }

        // Let GlobalExceptionHandler handle exceptions
        TestAnswerDto answer = testSessionService.submitAnswer(request);
        logger.debug("Answer submitted for question: {}, skipped: {}",
                request.questionId(), request.skip());
        return ResponseEntity.ok(answer);
    }

    /**
     * Get all answers for a session.
     *
     * @param sessionId Session UUID
     * @return List of all submitted answers
     */
    @GetMapping("/{sessionId}/answers")
    @PreAuthorize("@sessionSecurity.isSessionOwner(#sessionId)")
    public ResponseEntity<List<TestAnswerDto>> getSessionAnswers(@PathVariable UUID sessionId) {
        logger.debug("GET /api/v1/tests/sessions/{}/answers", sessionId);

        // Let GlobalExceptionHandler handle exceptions
        List<TestAnswerDto> answers = testSessionService.getSessionAnswers(sessionId);
        logger.debug("Found {} answers for session {}", answers.size(), sessionId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(answers);
    }

    // ==================== QUESTION NAVIGATION ====================

    /**
     * Get the current question for a session.
     *
     * Returns the question at the current index with all necessary
     * context for rendering (options, navigation state, previous answer).
     *
     * NOTE: Error handling is delegated to GlobalExceptionHandler for consistency.
     * - ResourceNotFoundException -> 404 Not Found
     * - IllegalStateException -> 400 Bad Request
     *
     * @param sessionId Session UUID
     * @return Current question details
     */
    @GetMapping("/{sessionId}/current-question")
    @PreAuthorize("@sessionSecurity.isSessionOwner(#sessionId)")
    public ResponseEntity<TestSessionService.CurrentQuestionDto> getCurrentQuestion(@PathVariable UUID sessionId) {
        logger.debug("GET /api/v1/tests/sessions/{}/current-question", sessionId);

        // Let GlobalExceptionHandler handle ResourceNotFoundException and IllegalStateException
        TestSessionService.CurrentQuestionDto question = testSessionService.getCurrentQuestion(sessionId);

        logger.debug("Current question {}/{} for session {}",
                question.questionIndex() + 1, question.totalQuestions(), sessionId);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(question);
    }

    /**
     * Navigate to a specific question in the session.
     *
     * @param sessionId Session UUID
     * @param questionIndex Target question index (0-based)
     * @return Updated session with new current question index
     */
    @PostMapping("/{sessionId}/navigate")
    @PreAuthorize("@sessionSecurity.isSessionOwner(#sessionId)")
    public ResponseEntity<TestSessionDto> navigateToQuestion(
            @PathVariable UUID sessionId,
            @RequestParam int questionIndex) {
        logger.debug("POST /api/v1/tests/sessions/{}/navigate?questionIndex={}", sessionId, questionIndex);

        // Let GlobalExceptionHandler handle exceptions
        TestSessionDto session = testSessionService.navigateToQuestion(sessionId, questionIndex);
        logger.debug("Navigated to question {} in session {}", questionIndex, sessionId);
        return ResponseEntity.ok(session);
    }

    /**
     * Update the remaining time for a session.
     *
     * Called periodically by the frontend to sync timer state.
     *
     * @param sessionId Session UUID
     * @param timeRemainingSeconds Updated time remaining
     * @return Updated session
     */
    @PutMapping("/{sessionId}/time")
    @PreAuthorize("@sessionSecurity.isSessionOwner(#sessionId)")
    public ResponseEntity<TestSessionDto> updateTimeRemaining(
            @PathVariable UUID sessionId,
            @RequestParam int timeRemainingSeconds) {
        logger.debug("PUT /api/v1/tests/sessions/{}/time?timeRemainingSeconds={}",
                sessionId, timeRemainingSeconds);

        // Let GlobalExceptionHandler handle exceptions
        TestSessionDto session = testSessionService.updateTimeRemaining(sessionId, timeRemainingSeconds);
        return ResponseEntity.ok(session);
    }

    // ==================== USER SESSION QUERIES ====================

    /**
     * Get all sessions for a user with pagination.
     *
     * @param clerkUserId User's Clerk ID
     * @param pageable Pagination parameters
     * @return Page of user's session summaries
     */
    @GetMapping("/user/{clerkUserId}")
    @PreAuthorize("@sessionSecurity.canAccessUserData(#clerkUserId)")
    public ResponseEntity<Page<TestSessionSummaryDto>> getUserSessions(
            @PathVariable String clerkUserId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        logger.debug("GET /api/v1/tests/sessions/user/{}", clerkUserId);

        Page<TestSessionSummaryDto> sessions = testSessionService.findByUser(clerkUserId, pageable);
        logger.debug("Found {} sessions for user {}", sessions.getTotalElements(), clerkUserId);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(sessions);
    }

    /**
     * Get sessions for a user filtered by status.
     *
     * @param clerkUserId User's Clerk ID
     * @param status Session status filter
     * @return List of matching sessions
     */
    @GetMapping("/user/{clerkUserId}/status/{status}")
    @PreAuthorize("@sessionSecurity.canAccessUserData(#clerkUserId)")
    public ResponseEntity<List<TestSessionSummaryDto>> getUserSessionsByStatus(
            @PathVariable String clerkUserId,
            @PathVariable SessionStatus status) {
        logger.debug("GET /api/v1/tests/sessions/user/{}/status/{}", clerkUserId, status);

        List<TestSessionSummaryDto> sessions = testSessionService.findByUserAndStatus(clerkUserId, status);
        logger.debug("Found {} {} sessions for user {}", sessions.size(), status, clerkUserId);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(sessions);
    }

    /**
     * Find an in-progress session for resuming.
     *
     * @param clerkUserId User's Clerk ID
     * @param templateId Template UUID
     * @return In-progress session if exists, 404 otherwise
     */
    @GetMapping("/user/{clerkUserId}/in-progress")
    @PreAuthorize("@sessionSecurity.canAccessUserData(#clerkUserId)")
    public ResponseEntity<TestSessionDto> findInProgressSession(
            @PathVariable String clerkUserId,
            @RequestParam UUID templateId) {
        logger.debug("GET /api/v1/tests/sessions/user/{}/in-progress?templateId={}", clerkUserId, templateId);

        return testSessionService.findInProgressSession(clerkUserId, templateId)
                .map(session -> ResponseEntity.ok()
                        .cacheControl(CacheControl.noStore())
                        .body(session))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ==================== DIAGNOSTIC ENDPOINTS ====================

    /**
     * Get detailed diagnostics for question availability in a template.
     *
     * This endpoint helps HR admins and developers understand why a template
     * might not have enough questions. It provides a breakdown of:
     * - Total questions per competency
     * - Active questions count
     * - Questions with GENERAL tag
     * - Questions with UNIVERSAL context scope
     * - Behavioral indicator counts
     *
     * @param templateId Template UUID to diagnose
     * @return Detailed diagnostic information
     */
    @Operation(
        summary = "Get template question diagnostics",
        description = "Detailed breakdown of question availability for debugging test session failures"
    )
    @ApiResponse(responseCode = "200", description = "Diagnostics retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Template not found")
    @GetMapping("/templates/{templateId}/diagnostics")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<Map<String, Object>> getTemplateDiagnostics(@PathVariable UUID templateId) {
        logger.info("GET /api/v1/tests/sessions/templates/{}/diagnostics", templateId);

        return diagnosticsService.generateDiagnostics(templateId)
                .map(diagnostics -> ResponseEntity.ok()
                        .cacheControl(CacheControl.noStore())
                        .body(diagnostics))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
