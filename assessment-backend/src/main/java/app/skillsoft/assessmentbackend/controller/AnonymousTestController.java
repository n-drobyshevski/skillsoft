package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.dto.AnonymousCompletionResponse;
import app.skillsoft.assessmentbackend.domain.dto.AnonymousResultDetailDto;
import app.skillsoft.assessmentbackend.domain.dto.AnonymousSessionRequest;
import app.skillsoft.assessmentbackend.domain.dto.AnonymousSessionResponse;
import app.skillsoft.assessmentbackend.domain.dto.AnonymousTakerInfoRequest;
import app.skillsoft.assessmentbackend.domain.dto.TestAnswerDto;
import app.skillsoft.assessmentbackend.domain.dto.TestResultDto;
import app.skillsoft.assessmentbackend.services.AnonymousTestService;
import app.skillsoft.assessmentbackend.services.CaptchaVerificationService;
import app.skillsoft.assessmentbackend.services.ResultTokenService;
import app.skillsoft.assessmentbackend.services.TestSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for anonymous test-taking via share links.
 *
 * <p>Provides public endpoints for anonymous users to take tests without
 * Clerk authentication. Uses session access tokens for authentication.</p>
 *
 * <h2>Authentication Model</h2>
 * <ul>
 *   <li>Session creation: Requires valid share link token in request body</li>
 *   <li>Subsequent requests: Requires X-Session-Token header with session access token</li>
 *   <li>IP-based rate limiting: 10 sessions per hour per IP address</li>
 * </ul>
 *
 * <h2>Security</h2>
 * <ul>
 *   <li>All endpoints are public (no Clerk JWT required)</li>
 *   <li>Session tokens are 256-bit cryptographically secure</li>
 *   <li>Token hashes stored in DB, originals never persisted</li>
 *   <li>Sessions expire 24 hours after creation if not completed</li>
 * </ul>
 *
 * <p>API Base Path: /api/v1/anonymous</p>
 *
 * @author SkillSoft Development Team
 * @see AnonymousTestService
 */
@RestController
@RequestMapping("/api/v1/anonymous")
@Tag(name = "Anonymous Testing", description = "Public endpoints for test-taking via share links")
public class AnonymousTestController {

    private static final Logger log = LoggerFactory.getLogger(AnonymousTestController.class);

    /**
     * Header name for session access token.
     */
    private static final String SESSION_TOKEN_HEADER = "X-Session-Token";

    private final AnonymousTestService anonymousTestService;
    private final ResultTokenService resultTokenService;
    private final CaptchaVerificationService captchaVerificationService;

    public AnonymousTestController(AnonymousTestService anonymousTestService,
                                   ResultTokenService resultTokenService,
                                   CaptchaVerificationService captchaVerificationService) {
        this.anonymousTestService = anonymousTestService;
        this.resultTokenService = resultTokenService;
        this.captchaVerificationService = captchaVerificationService;
    }

    // ==================== SESSION LIFECYCLE ====================

    /**
     * Create a new anonymous test session from a share link.
     *
     * <p>Validates the share link, checks rate limits, and creates a new session.
     * The session access token is returned only in this response and must be
     * stored by the client for subsequent API calls.</p>
     *
     * @param request Request containing the share link token
     * @param httpRequest HTTP request for extracting IP and User-Agent
     * @return Created session with access token (201 CREATED)
     */
    @Operation(
            summary = "Create anonymous test session",
            description = "Create a new test session from a share link. Returns session access token."
    )
    @ApiResponse(responseCode = "201", description = "Session created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid share link token")
    @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    @PostMapping("/sessions")
    public ResponseEntity<AnonymousSessionResponse> createSession(
            @Valid @RequestBody AnonymousSessionRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        log.debug("POST /api/v1/anonymous/sessions - Creating session from IP: {}", ipAddress);

        AnonymousSessionResponse response = anonymousTestService.createSession(
                request, ipAddress, userAgent
        );

        log.info("Created anonymous session {} for template {}",
                response.sessionId(), response.template().id());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get an existing anonymous session.
     *
     * @param sessionId Session UUID
     * @param sessionToken Session access token from header
     * @return Session details (200 OK)
     */
    @Operation(
            summary = "Get anonymous session",
            description = "Retrieve session details. Requires session access token."
    )
    @ApiResponse(responseCode = "200", description = "Session retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Invalid or missing session token")
    @ApiResponse(responseCode = "404", description = "Session not found")
    @ApiResponse(responseCode = "410", description = "Session expired")
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<AnonymousSessionResponse> getSession(
            @PathVariable UUID sessionId,
            @Parameter(description = "Session access token", required = true)
            @RequestHeader(SESSION_TOKEN_HEADER) String sessionToken) {

        log.debug("GET /api/v1/anonymous/sessions/{}", sessionId);

        return anonymousTestService.getSession(sessionId, sessionToken)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get the current question for the session.
     *
     * @param sessionId Session UUID
     * @param sessionToken Session access token from header
     * @return Current question with navigation context (200 OK)
     */
    @Operation(
            summary = "Get current question",
            description = "Get the current question in the test session."
    )
    @ApiResponse(responseCode = "200", description = "Question retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Invalid or missing session token")
    @ApiResponse(responseCode = "404", description = "Session not found")
    @GetMapping("/sessions/{sessionId}/current-question")
    public ResponseEntity<TestSessionService.CurrentQuestionDto> getCurrentQuestion(
            @PathVariable UUID sessionId,
            @RequestHeader(SESSION_TOKEN_HEADER) String sessionToken) {

        log.debug("GET /api/v1/anonymous/sessions/{}/current-question", sessionId);

        TestSessionService.CurrentQuestionDto question =
                anonymousTestService.getCurrentQuestion(sessionId, sessionToken);

        return ResponseEntity.ok(question);
    }

    /**
     * Submit an answer to a question.
     *
     * @param sessionId Session UUID
     * @param sessionToken Session access token from header
     * @param request Answer submission request
     * @return Submitted answer details (200 OK)
     */
    @Operation(
            summary = "Submit answer",
            description = "Submit an answer to a question in the test session."
    )
    @ApiResponse(responseCode = "200", description = "Answer submitted successfully")
    @ApiResponse(responseCode = "401", description = "Invalid or missing session token")
    @ApiResponse(responseCode = "400", description = "Invalid question or answer")
    @PostMapping("/sessions/{sessionId}/answers")
    public ResponseEntity<TestAnswerDto> submitAnswer(
            @PathVariable UUID sessionId,
            @RequestHeader(SESSION_TOKEN_HEADER) String sessionToken,
            @Valid @RequestBody AnswerSubmissionRequest request) {

        log.debug("POST /api/v1/anonymous/sessions/{}/answers - question: {}, option: {}",
                sessionId, request.questionId(), request.selectedOptionIndex());

        TestAnswerDto answer = anonymousTestService.submitAnswer(
                sessionId,
                sessionToken,
                request.questionId(),
                request.selectedOptionIndex()
        );

        return ResponseEntity.ok(answer);
    }

    /**
     * Navigate to a specific question.
     *
     * @param sessionId Session UUID
     * @param sessionToken Session access token from header
     * @param questionIndex Target question index (0-based)
     * @return Updated session details (200 OK)
     */
    @Operation(
            summary = "Navigate to question",
            description = "Navigate to a specific question by index. May be restricted by template settings."
    )
    @ApiResponse(responseCode = "200", description = "Navigation successful")
    @ApiResponse(responseCode = "401", description = "Invalid or missing session token")
    @ApiResponse(responseCode = "400", description = "Invalid question index or navigation not allowed")
    @PostMapping("/sessions/{sessionId}/navigate")
    public ResponseEntity<AnonymousSessionResponse> navigateToQuestion(
            @PathVariable UUID sessionId,
            @RequestHeader(SESSION_TOKEN_HEADER) String sessionToken,
            @RequestParam int questionIndex) {

        log.debug("POST /api/v1/anonymous/sessions/{}/navigate?questionIndex={}",
                sessionId, questionIndex);

        AnonymousSessionResponse response = anonymousTestService.navigateToQuestion(
                sessionId, sessionToken, questionIndex
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Update remaining time for a timed test.
     *
     * @param sessionId Session UUID
     * @param sessionToken Session access token from header
     * @param timeRemainingSeconds Remaining time in seconds
     * @return Updated session details (200 OK)
     */
    @Operation(
            summary = "Update time remaining",
            description = "Sync remaining time with server. Called periodically by client."
    )
    @ApiResponse(responseCode = "200", description = "Time updated successfully")
    @ApiResponse(responseCode = "401", description = "Invalid or missing session token")
    @PostMapping("/sessions/{sessionId}/time")
    public ResponseEntity<AnonymousSessionResponse> updateTimeRemaining(
            @PathVariable UUID sessionId,
            @RequestHeader(SESSION_TOKEN_HEADER) String sessionToken,
            @RequestParam int timeRemainingSeconds) {

        log.debug("POST /api/v1/anonymous/sessions/{}/time?timeRemainingSeconds={}",
                sessionId, timeRemainingSeconds);

        AnonymousSessionResponse response = anonymousTestService.updateTimeRemaining(
                sessionId, sessionToken, timeRemainingSeconds
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Complete the test session with taker information.
     *
     * <p>Collects optional taker information, calculates results, and
     * marks the session as completed. Results are returned immediately.</p>
     *
     * @param sessionId Session UUID
     * @param sessionToken Session access token from header
     * @param takerInfo Anonymous taker information (name, optional email/notes)
     * @return Test results (200 OK)
     */
    @Operation(
            summary = "Complete test session",
            description = "Submit taker information and complete the test. Returns results."
    )
    @ApiResponse(responseCode = "200", description = "Test completed successfully")
    @ApiResponse(responseCode = "401", description = "Invalid or missing session token")
    @ApiResponse(responseCode = "400", description = "Invalid taker information")
    @PostMapping("/sessions/{sessionId}/complete")
    public ResponseEntity<AnonymousCompletionResponse> completeSession(
            @PathVariable UUID sessionId,
            @RequestHeader(SESSION_TOKEN_HEADER) String sessionToken,
            @Valid @RequestBody AnonymousTakerInfoRequest takerInfo) {

        log.debug("POST /api/v1/anonymous/sessions/{}/complete - taker: {} {}",
                sessionId, takerInfo.firstName(), takerInfo.lastName());

        TestResultDto result = anonymousTestService.completeSession(
                sessionId, sessionToken, takerInfo
        );

        // Generate persistent result view token (HMAC-signed, 7-day expiry)
        String resultViewToken = resultTokenService.generateToken(result.id(), result.sessionId());

        log.info("Anonymous session {} completed with score: {}%, result token generated",
                sessionId, result.overallPercentage());

        return ResponseEntity.ok(new AnonymousCompletionResponse(result, resultViewToken));
    }

    /**
     * Get the result for a completed session.
     *
     * @param sessionId Session UUID
     * @param sessionToken Session access token from header
     * @return Detailed test result (200 OK)
     */
    @Operation(
            summary = "Get test result",
            description = "Retrieve the result for a completed test session."
    )
    @ApiResponse(responseCode = "200", description = "Result retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Invalid or missing session token")
    @ApiResponse(responseCode = "400", description = "Session not yet completed")
    @GetMapping("/sessions/{sessionId}/result")
    public ResponseEntity<AnonymousResultDetailDto> getResult(
            @PathVariable UUID sessionId,
            @RequestHeader(SESSION_TOKEN_HEADER) String sessionToken) {

        log.debug("GET /api/v1/anonymous/sessions/{}/result", sessionId);

        AnonymousResultDetailDto result = anonymousTestService.getResult(sessionId, sessionToken);

        return ResponseEntity.ok(result);
    }

    /**
     * Update advisory metadata for a session (e.g. tab switch count).
     *
     * <p>This endpoint is fire-and-forget from the client side. The data is stored
     * for template owners to review and does NOT trigger any automatic action.</p>
     *
     * @param sessionId Session UUID
     * @param sessionToken Session access token from header
     * @param request Metadata to update
     * @return 204 No Content
     */
    @Operation(
            summary = "Update session metadata",
            description = "Store advisory metadata such as tab switch count. Does not affect scoring."
    )
    @ApiResponse(responseCode = "204", description = "Metadata updated successfully")
    @ApiResponse(responseCode = "401", description = "Invalid or missing session token")
    @PatchMapping("/sessions/{sessionId}/metadata")
    public ResponseEntity<Void> updateSessionMetadata(
            @PathVariable UUID sessionId,
            @RequestHeader(SESSION_TOKEN_HEADER) String sessionToken,
            @RequestBody SessionMetadataUpdateRequest request) {

        log.debug("PATCH /api/v1/anonymous/sessions/{}/metadata - tabSwitchCount={}",
                sessionId, request.tabSwitchCount());

        anonymousTestService.updateSessionMetadata(sessionId, sessionToken, request.tabSwitchCount());

        return ResponseEntity.noContent().build();
    }

    // ==================== CONFIGURATION ====================

    /**
     * Get CAPTCHA configuration for the anonymous test landing page.
     *
     * <p>Returns whether CAPTCHA is enabled and the site key needed
     * to render the hCaptcha widget on the client.</p>
     *
     * @return CAPTCHA configuration (200 OK)
     */
    @Operation(
            summary = "Get CAPTCHA configuration",
            description = "Returns whether CAPTCHA is required and the site key for rendering."
    )
    @ApiResponse(responseCode = "200", description = "Configuration retrieved")
    @GetMapping("/config/captcha")
    public ResponseEntity<CaptchaConfigResponse> getCaptchaConfig() {
        return ResponseEntity.ok(new CaptchaConfigResponse(
                captchaVerificationService.isEnabled(),
                captchaVerificationService.isEnabled() ? captchaVerificationService.getSiteKey() : null
        ));
    }

    // ==================== HELPER METHODS ====================

    /**
     * Extract client IP address from request, handling proxies.
     *
     * <p>Checks X-Forwarded-For header first (set by proxies/load balancers),
     * falls back to remote address if not present.</p>
     *
     * @param request HTTP request
     * @return Client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // Take the first IP (original client) if multiple are present
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }

        return request.getRemoteAddr();
    }

    // ==================== REQUEST DTOs ====================

    /**
     * Request body for submitting an answer.
     *
     * @param questionId The question UUID being answered
     * @param selectedOptionIndex The selected option index (0-based)
     */
    public record AnswerSubmissionRequest(
            UUID questionId,
            int selectedOptionIndex
    ) {}

    /**
     * Request body for updating session metadata.
     *
     * @param tabSwitchCount Number of times the taker switched away from the test tab.
     *                       Nullable — a null value is a no-op for that field.
     */
    public record SessionMetadataUpdateRequest(
            Integer tabSwitchCount
    ) {}

    /**
     * Response for CAPTCHA configuration endpoint.
     *
     * @param enabled Whether CAPTCHA is required
     * @param siteKey hCaptcha site key (null when disabled)
     */
    public record CaptchaConfigResponse(
            boolean enabled,
            String siteKey
    ) {}
}
