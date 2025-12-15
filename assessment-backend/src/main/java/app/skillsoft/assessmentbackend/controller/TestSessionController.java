package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.dto.*;
import app.skillsoft.assessmentbackend.domain.entities.BehavioralIndicator;
import app.skillsoft.assessmentbackend.domain.entities.Competency;
import app.skillsoft.assessmentbackend.domain.entities.SessionStatus;
import app.skillsoft.assessmentbackend.domain.entities.TestTemplate;
import app.skillsoft.assessmentbackend.repository.AssessmentQuestionRepository;
import app.skillsoft.assessmentbackend.repository.BehavioralIndicatorRepository;
import app.skillsoft.assessmentbackend.repository.CompetencyRepository;
import app.skillsoft.assessmentbackend.repository.TestTemplateRepository;
import app.skillsoft.assessmentbackend.services.TestSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    private final TestTemplateRepository templateRepository;
    private final CompetencyRepository competencyRepository;
    private final BehavioralIndicatorRepository indicatorRepository;
    private final AssessmentQuestionRepository questionRepository;

    public TestSessionController(
            TestSessionService testSessionService,
            TestTemplateRepository templateRepository,
            CompetencyRepository competencyRepository,
            BehavioralIndicatorRepository indicatorRepository,
            AssessmentQuestionRepository questionRepository) {
        this.testSessionService = testSessionService;
        this.templateRepository = templateRepository;
        this.competencyRepository = competencyRepository;
        this.indicatorRepository = indicatorRepository;
        this.questionRepository = questionRepository;
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

        return ResponseEntity.ok(readiness);
    }

    /**
     * Get a session by ID.
     *
     * @param sessionId Session UUID
     * @return Session details or 404 if not found
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<TestSessionDto> getSession(@PathVariable UUID sessionId) {
        logger.debug("GET /api/v1/tests/sessions/{}", sessionId);

        return testSessionService.findById(sessionId)
                .map(ResponseEntity::ok)
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
    public ResponseEntity<List<TestAnswerDto>> getSessionAnswers(@PathVariable UUID sessionId) {
        logger.debug("GET /api/v1/tests/sessions/{}/answers", sessionId);

        // Let GlobalExceptionHandler handle exceptions
        List<TestAnswerDto> answers = testSessionService.getSessionAnswers(sessionId);
        logger.debug("Found {} answers for session {}", answers.size(), sessionId);
        return ResponseEntity.ok(answers);
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
    public ResponseEntity<TestSessionService.CurrentQuestionDto> getCurrentQuestion(@PathVariable UUID sessionId) {
        logger.debug("GET /api/v1/tests/sessions/{}/current-question", sessionId);

        // Let GlobalExceptionHandler handle ResourceNotFoundException and IllegalStateException
        TestSessionService.CurrentQuestionDto question = testSessionService.getCurrentQuestion(sessionId);

        logger.debug("Current question {}/{} for session {}",
                question.questionIndex() + 1, question.totalQuestions(), sessionId);

        return ResponseEntity.ok(question);
    }

    /**
     * Navigate to a specific question in the session.
     *
     * @param sessionId Session UUID
     * @param questionIndex Target question index (0-based)
     * @return Updated session with new current question index
     */
    @PostMapping("/{sessionId}/navigate")
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
    public ResponseEntity<Page<TestSessionSummaryDto>> getUserSessions(
            @PathVariable String clerkUserId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        logger.debug("GET /api/v1/tests/sessions/user/{}", clerkUserId);

        Page<TestSessionSummaryDto> sessions = testSessionService.findByUser(clerkUserId, pageable);
        logger.debug("Found {} sessions for user {}", sessions.getTotalElements(), clerkUserId);

        return ResponseEntity.ok(sessions);
    }

    /**
     * Get sessions for a user filtered by status.
     *
     * @param clerkUserId User's Clerk ID
     * @param status Session status filter
     * @return List of matching sessions
     */
    @GetMapping("/user/{clerkUserId}/status/{status}")
    public ResponseEntity<List<TestSessionSummaryDto>> getUserSessionsByStatus(
            @PathVariable String clerkUserId,
            @PathVariable SessionStatus status) {
        logger.debug("GET /api/v1/tests/sessions/user/{}/status/{}", clerkUserId, status);

        List<TestSessionSummaryDto> sessions = testSessionService.findByUserAndStatus(clerkUserId, status);
        logger.debug("Found {} {} sessions for user {}", sessions.size(), status, clerkUserId);

        return ResponseEntity.ok(sessions);
    }

    /**
     * Find an in-progress session for resuming.
     *
     * @param clerkUserId User's Clerk ID
     * @param templateId Template UUID
     * @return In-progress session if exists, 404 otherwise
     */
    @GetMapping("/user/{clerkUserId}/in-progress")
    public ResponseEntity<TestSessionDto> findInProgressSession(
            @PathVariable String clerkUserId,
            @RequestParam UUID templateId) {
        logger.debug("GET /api/v1/tests/sessions/user/{}/in-progress?templateId={}", clerkUserId, templateId);

        return testSessionService.findInProgressSession(clerkUserId, templateId)
                .map(ResponseEntity::ok)
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
    public ResponseEntity<Map<String, Object>> getTemplateDiagnostics(@PathVariable UUID templateId) {
        logger.info("GET /api/v1/tests/sessions/templates/{}/diagnostics", templateId);

        Optional<TestTemplate> templateOpt = templateRepository.findById(templateId);
        if (templateOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TestTemplate template = templateOpt.get();
        Map<String, Object> diagnostics = new LinkedHashMap<>();

        diagnostics.put("templateId", templateId);
        diagnostics.put("templateName", template.getName());
        diagnostics.put("goal", template.getGoal());
        diagnostics.put("questionsPerIndicator", template.getQuestionsPerIndicator());
        diagnostics.put("isActive", template.getIsActive());

        List<UUID> competencyIds = template.getCompetencyIds();
        diagnostics.put("competencyCount", competencyIds != null ? competencyIds.size() : 0);

        List<Map<String, Object>> competencyDiagnostics = new ArrayList<>();
        int totalAvailableQuestions = 0;
        int totalRequiredQuestions = 0;

        if (competencyIds != null) {
            for (UUID compId : competencyIds) {
                Map<String, Object> compDiag = new LinkedHashMap<>();
                compDiag.put("competencyId", compId);

                // Get competency name
                String compName = competencyRepository.findById(compId)
                        .map(Competency::getName)
                        .orElse("Unknown");
                compDiag.put("competencyName", compName);

                // Count behavioral indicators
                List<BehavioralIndicator> indicators = indicatorRepository.findByCompetencyId(compId);
                compDiag.put("indicatorCount", indicators.size());

                // Get indicator details
                List<Map<String, Object>> indicatorDetails = new ArrayList<>();
                for (BehavioralIndicator ind : indicators) {
                    Map<String, Object> indDetail = new LinkedHashMap<>();
                    indDetail.put("id", ind.getId());
                    indDetail.put("title", ind.getTitle());
                    indDetail.put("contextScope", ind.getContextScope() != null ? ind.getContextScope().name() : "NULL");
                    indDetail.put("isActive", ind.isActive());

                    long questionCount = questionRepository.findByBehavioralIndicator_Id(ind.getId())
                            .stream().filter(q -> q.isActive()).count();
                    indDetail.put("activeQuestionCount", questionCount);

                    indicatorDetails.add(indDetail);
                }
                compDiag.put("indicators", indicatorDetails);

                // Count total active questions for this competency
                long activeQuestions = questionRepository.countActiveQuestionsForCompetency(compId);
                compDiag.put("activeQuestionCount", activeQuestions);

                // Try to get Scenario A eligible questions
                try {
                    List<?> scenarioAQuestions = questionRepository.findUniversalQuestions(
                            compId, template.getQuestionsPerIndicator());
                    compDiag.put("scenarioAEligibleCount", scenarioAQuestions.size());
                } catch (Exception e) {
                    compDiag.put("scenarioAEligibleCount", "error: " + e.getMessage());
                }

                // Calculate shortfall
                int required = template.getQuestionsPerIndicator();
                int available = (int) activeQuestions;
                compDiag.put("questionsRequired", required);
                compDiag.put("questionsAvailable", available);
                compDiag.put("shortfall", Math.max(0, required - available));

                totalAvailableQuestions += available;
                totalRequiredQuestions += required;

                competencyDiagnostics.add(compDiag);
            }
        }

        diagnostics.put("competencies", competencyDiagnostics);
        diagnostics.put("totalQuestionsAvailable", totalAvailableQuestions);
        diagnostics.put("totalQuestionsRequired", totalRequiredQuestions);
        diagnostics.put("canStartSession", totalAvailableQuestions >= totalRequiredQuestions);

        // Add troubleshooting tips based on findings
        List<String> issues = new ArrayList<>();
        for (Map<String, Object> compDiag : competencyDiagnostics) {
            int shortfall = (int) compDiag.get("shortfall");
            if (shortfall > 0) {
                issues.add(String.format("Competency '%s' needs %d more questions",
                        compDiag.get("competencyName"), shortfall));
            }
            int indicatorCount = (int) compDiag.get("indicatorCount");
            if (indicatorCount == 0) {
                issues.add(String.format("Competency '%s' has no behavioral indicators",
                        compDiag.get("competencyName")));
            }
        }
        diagnostics.put("issues", issues);

        if (!issues.isEmpty()) {
            List<String> tips = new ArrayList<>();
            tips.add("1. Ensure each competency has at least one behavioral indicator");
            tips.add("2. Ensure each behavioral indicator has active questions (is_active=true)");
            tips.add("3. For Scenario A (OVERVIEW), ensure indicators have context_scope='UNIVERSAL' or NULL");
            tips.add("4. For optimal filtering, add 'GENERAL' tag to question metadata.tags");
            diagnostics.put("troubleshootingTips", tips);
        }

        logger.info("Template {} diagnostics: {} available, {} required, canStart={}",
                templateId, totalAvailableQuestions, totalRequiredQuestions,
                totalAvailableQuestions >= totalRequiredQuestions);

        return ResponseEntity.ok(diagnostics);
    }
}
