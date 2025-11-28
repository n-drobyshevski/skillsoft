package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.dto.*;
import app.skillsoft.assessmentbackend.domain.entities.SessionStatus;
import app.skillsoft.assessmentbackend.services.TestSessionService;
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

import java.util.List;
import java.util.UUID;

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

    public TestSessionController(TestSessionService testSessionService) {
        this.testSessionService = testSessionService;
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
        logger.info("POST /api/v1/tests/sessions - Starting session for template: {}, user: {}", 
                request.templateId(), request.clerkUserId());
        
        try {
            TestSessionDto session = testSessionService.startSession(request);
            logger.info("Started session with id: {}", session.id());
            return ResponseEntity.status(HttpStatus.CREATED).body(session);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid session request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                logger.warn("Template not found: {}", request.templateId());
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }

    /**
     * Get a session by ID.
     * 
     * @param sessionId Session UUID
     * @return Session details or 404 if not found
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<TestSessionDto> getSession(@PathVariable UUID sessionId) {
        logger.info("GET /api/v1/tests/sessions/{}", sessionId);
        
        return testSessionService.findById(sessionId)
                .map(session -> {
                    logger.info("Found session, status: {}", session.status());
                    return ResponseEntity.ok(session);
                })
                .orElseGet(() -> {
                    logger.warn("Session not found: {}", sessionId);
                    return ResponseEntity.notFound().build();
                });
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
        logger.info("POST /api/v1/tests/sessions/{}/complete", sessionId);
        
        try {
            TestResultDto result = testSessionService.completeSession(sessionId);
            logger.info("Completed session {}, score: {}%", sessionId, result.overallPercentage());
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            logger.warn("Cannot complete session {}: {}", sessionId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                logger.warn("Session not found: {}", sessionId);
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
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
        logger.info("POST /api/v1/tests/sessions/{}/abandon", sessionId);
        
        try {
            TestSessionDto session = testSessionService.abandonSession(sessionId);
            logger.info("Abandoned session: {}", sessionId);
            return ResponseEntity.ok(session);
        } catch (IllegalStateException e) {
            logger.warn("Cannot abandon session {}: {}", sessionId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                logger.warn("Session not found: {}", sessionId);
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
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
        logger.info("POST /api/v1/tests/sessions/{}/answers - Question: {}", 
                sessionId, request.questionId());
        
        // Ensure the sessionId in path matches the request body
        if (!sessionId.equals(request.sessionId())) {
            logger.warn("Session ID mismatch: path={}, body={}", sessionId, request.sessionId());
            return ResponseEntity.badRequest().build();
        }
        
        try {
            TestAnswerDto answer = testSessionService.submitAnswer(request);
            logger.info("Answer submitted for question: {}, skipped: {}", 
                    request.questionId(), request.skip());
            return ResponseEntity.ok(answer);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid answer: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            logger.warn("Cannot submit answer: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                logger.warn("Session or question not found");
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }

    /**
     * Get all answers for a session.
     * 
     * @param sessionId Session UUID
     * @return List of all submitted answers
     */
    @GetMapping("/{sessionId}/answers")
    public ResponseEntity<List<TestAnswerDto>> getSessionAnswers(@PathVariable UUID sessionId) {
        logger.info("GET /api/v1/tests/sessions/{}/answers", sessionId);
        
        try {
            List<TestAnswerDto> answers = testSessionService.getSessionAnswers(sessionId);
            logger.info("Found {} answers for session {}", answers.size(), sessionId);
            return ResponseEntity.ok(answers);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                logger.warn("Session not found: {}", sessionId);
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }

    // ==================== QUESTION NAVIGATION ====================

    /**
     * Get the current question for a session.
     * 
     * Returns the question at the current index with all necessary
     * context for rendering (options, navigation state, previous answer).
     * 
     * @param sessionId Session UUID
     * @return Current question details
     */
    @GetMapping("/{sessionId}/current-question")
    public ResponseEntity<TestSessionService.CurrentQuestionDto> getCurrentQuestion(@PathVariable UUID sessionId) {
        logger.info("GET /api/v1/tests/sessions/{}/current-question", sessionId);
        
        try {
            TestSessionService.CurrentQuestionDto question = testSessionService.getCurrentQuestion(sessionId);
            logger.info("Current question {}/{} for session {}", 
                    question.questionIndex() + 1, question.totalQuestions(), sessionId);
            return ResponseEntity.ok(question);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                logger.warn("Session not found: {}", sessionId);
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
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
        logger.info("POST /api/v1/tests/sessions/{}/navigate?questionIndex={}", sessionId, questionIndex);
        
        try {
            TestSessionDto session = testSessionService.navigateToQuestion(sessionId, questionIndex);
            logger.info("Navigated to question {} in session {}", questionIndex, sessionId);
            return ResponseEntity.ok(session);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid navigation: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            logger.warn("Cannot navigate: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                logger.warn("Session not found: {}", sessionId);
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
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
        logger.info("PUT /api/v1/tests/sessions/{}/time?timeRemainingSeconds={}", 
                sessionId, timeRemainingSeconds);
        
        try {
            TestSessionDto session = testSessionService.updateTimeRemaining(sessionId, timeRemainingSeconds);
            return ResponseEntity.ok(session);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                logger.warn("Session not found: {}", sessionId);
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
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
        logger.info("GET /api/v1/tests/sessions/user/{}", clerkUserId);
        
        Page<TestSessionSummaryDto> sessions = testSessionService.findByUser(clerkUserId, pageable);
        logger.info("Found {} sessions for user {}", sessions.getTotalElements(), clerkUserId);
        
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
        logger.info("GET /api/v1/tests/sessions/user/{}/status/{}", clerkUserId, status);
        
        List<TestSessionSummaryDto> sessions = testSessionService.findByUserAndStatus(clerkUserId, status);
        logger.info("Found {} {} sessions for user {}", sessions.size(), status, clerkUserId);
        
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
        logger.info("GET /api/v1/tests/sessions/user/{}/in-progress?templateId={}", clerkUserId, templateId);
        
        return testSessionService.findInProgressSession(clerkUserId, templateId)
                .map(session -> {
                    logger.info("Found in-progress session: {}", session.id());
                    return ResponseEntity.ok(session);
                })
                .orElseGet(() -> {
                    logger.info("No in-progress session found for user {} on template {}", 
                            clerkUserId, templateId);
                    return ResponseEntity.notFound().build();
                });
    }
}
