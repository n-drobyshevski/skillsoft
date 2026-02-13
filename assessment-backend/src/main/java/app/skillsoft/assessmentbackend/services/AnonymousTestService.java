package app.skillsoft.assessmentbackend.services;

import app.skillsoft.assessmentbackend.domain.dto.AnonymousResultDetailDto;
import app.skillsoft.assessmentbackend.domain.dto.AnonymousResultSummaryDto;
import app.skillsoft.assessmentbackend.domain.dto.AnonymousSessionRequest;
import app.skillsoft.assessmentbackend.domain.dto.AnonymousSessionResponse;
import app.skillsoft.assessmentbackend.domain.dto.AnonymousTakerInfoRequest;
import app.skillsoft.assessmentbackend.domain.dto.TestAnswerDto;
import app.skillsoft.assessmentbackend.domain.dto.TestResultDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for managing anonymous test sessions.
 *
 * <p>Anonymous sessions allow users to take tests via share links without
 * requiring Clerk authentication. These sessions are authenticated using
 * session access tokens that are generated at session creation and must
 * be passed in all subsequent API requests.</p>
 *
 * <p>Security Model:</p>
 * <ul>
 *   <li>Share link token validates access to template</li>
 *   <li>Session access token (256-bit) authenticates the session</li>
 *   <li>Token hash stored in DB, original returned only at creation</li>
 *   <li>IP-based rate limiting (10 sessions/hour per IP)</li>
 *   <li>Sessions expire 24 hours after creation if not completed</li>
 * </ul>
 *
 * @author SkillSoft Development Team
 */
public interface AnonymousTestService {

    /**
     * Create a new anonymous test session from a share link.
     *
     * <p>Validates the share link token, checks rate limits, generates
     * a session access token, and creates the session. The share link
     * usage is recorded upon successful session creation.</p>
     *
     * @param request Request containing the share link token
     * @param ipAddress Client IP address for rate limiting
     * @param userAgent Client user agent string
     * @return Response containing session ID, access token, and template info
     * @throws app.skillsoft.assessmentbackend.exception.ShareLinkException if share link is invalid
     * @throws app.skillsoft.assessmentbackend.exception.RateLimitExceededException if rate limit exceeded
     */
    AnonymousSessionResponse createSession(
            AnonymousSessionRequest request,
            String ipAddress,
            String userAgent
    );

    /**
     * Validate a session access token and return the session ID if valid.
     *
     * @param sessionAccessToken The token to validate
     * @return Optional containing the session ID if valid, empty otherwise
     */
    Optional<UUID> validateSessionToken(String sessionAccessToken);

    /**
     * Get an anonymous session by ID after validating the access token.
     *
     * @param sessionId The session ID
     * @param sessionAccessToken The session access token
     * @return Optional containing the session response if valid
     * @throws app.skillsoft.assessmentbackend.exception.InvalidSessionTokenException if token is invalid
     * @throws app.skillsoft.assessmentbackend.exception.SessionExpiredException if session has expired
     */
    Optional<AnonymousSessionResponse> getSession(UUID sessionId, String sessionAccessToken);

    /**
     * Get the current question for an anonymous session.
     *
     * @param sessionId The session ID
     * @param sessionAccessToken The session access token
     * @return Current question with context
     * @throws app.skillsoft.assessmentbackend.exception.InvalidSessionTokenException if token is invalid
     * @throws app.skillsoft.assessmentbackend.exception.SessionExpiredException if session has expired
     */
    TestSessionService.CurrentQuestionDto getCurrentQuestion(UUID sessionId, String sessionAccessToken);

    /**
     * Submit an answer for an anonymous session.
     *
     * @param sessionId The session ID
     * @param sessionAccessToken The session access token
     * @param questionId The question being answered
     * @param selectedOptionIndex The selected answer option index
     * @return The submitted answer
     * @throws app.skillsoft.assessmentbackend.exception.InvalidSessionTokenException if token is invalid
     * @throws app.skillsoft.assessmentbackend.exception.SessionExpiredException if session has expired
     */
    TestAnswerDto submitAnswer(
            UUID sessionId,
            String sessionAccessToken,
            UUID questionId,
            int selectedOptionIndex
    );

    /**
     * Navigate to a specific question in an anonymous session.
     *
     * @param sessionId The session ID
     * @param sessionAccessToken The session access token
     * @param questionIndex The target question index
     * @return Updated session response
     * @throws app.skillsoft.assessmentbackend.exception.InvalidSessionTokenException if token is invalid
     * @throws app.skillsoft.assessmentbackend.exception.SessionExpiredException if session has expired
     */
    AnonymousSessionResponse navigateToQuestion(
            UUID sessionId,
            String sessionAccessToken,
            int questionIndex
    );

    /**
     * Update the remaining time for an anonymous session.
     *
     * @param sessionId The session ID
     * @param sessionAccessToken The session access token
     * @param timeRemainingSeconds The remaining time in seconds
     * @return Updated session response
     * @throws app.skillsoft.assessmentbackend.exception.InvalidSessionTokenException if token is invalid
     */
    AnonymousSessionResponse updateTimeRemaining(
            UUID sessionId,
            String sessionAccessToken,
            int timeRemainingSeconds
    );

    /**
     * Complete an anonymous session with taker info.
     *
     * <p>Collects the anonymous taker's information (name, optional email/notes),
     * calculates the test result, and marks the session as completed.</p>
     *
     * @param sessionId The session ID
     * @param sessionAccessToken The session access token
     * @param takerInfo The anonymous taker's information
     * @return The test result
     * @throws app.skillsoft.assessmentbackend.exception.InvalidSessionTokenException if token is invalid
     * @throws app.skillsoft.assessmentbackend.exception.SessionExpiredException if session has expired
     */
    TestResultDto completeSession(
            UUID sessionId,
            String sessionAccessToken,
            AnonymousTakerInfoRequest takerInfo
    );

    /**
     * Get the result for a completed anonymous session.
     *
     * @param sessionId The session ID
     * @param sessionAccessToken The session access token
     * @return The detailed result
     * @throws app.skillsoft.assessmentbackend.exception.InvalidSessionTokenException if token is invalid
     */
    AnonymousResultDetailDto getResult(UUID sessionId, String sessionAccessToken);

    /**
     * List all anonymous results for a template.
     *
     * <p>Used by template owners to view results from anonymous test takers.
     * Requires authenticated access with appropriate permissions.</p>
     *
     * @param templateId The template ID
     * @param pageable Pagination parameters
     * @return Page of anonymous result summaries
     */
    Page<AnonymousResultSummaryDto> listAnonymousResults(UUID templateId, Pageable pageable);

    /**
     * Get detailed result for template owners.
     *
     * <p>Returns full result details including taker info for viewing
     * by template owners with appropriate permissions.</p>
     *
     * @param resultId The result ID
     * @return The detailed result
     */
    AnonymousResultDetailDto getAnonymousResultDetail(UUID resultId);

    /**
     * Get statistics about anonymous sessions for a template.
     *
     * @param templateId The template ID
     * @return Session statistics
     */
    AnonymousSessionStats getSessionStats(UUID templateId);

    /**
     * Get statistics about anonymous results for a share link.
     *
     * @param shareLinkId The share link ID
     * @return Result statistics for the share link
     */
    ShareLinkResultStats getShareLinkStats(UUID shareLinkId);

    /**
     * Statistics about anonymous sessions for a template.
     */
    record AnonymousSessionStats(
            /**
             * Total number of anonymous sessions created.
             */
            long totalSessions,

            /**
             * Number of completed sessions.
             */
            long completedSessions,

            /**
             * Number of abandoned/expired sessions.
             */
            long abandonedSessions,

            /**
             * Number of currently in-progress sessions.
             */
            long inProgressSessions,

            /**
             * Completion rate (completed / total).
             */
            double completionRate
    ) {
        public static AnonymousSessionStats empty() {
            return new AnonymousSessionStats(0, 0, 0, 0, 0.0);
        }
    }

    /**
     * Statistics about results for a share link.
     */
    record ShareLinkResultStats(
            /**
             * Share link ID.
             */
            UUID shareLinkId,

            /**
             * Total sessions started via this link.
             */
            long totalSessions,

            /**
             * Total completed results.
             */
            long completedResults,

            /**
             * Average score (0-100).
             */
            Double averageScore,

            /**
             * Pass rate (0-1).
             */
            Double passRate
    ) {
        public static ShareLinkResultStats empty(UUID shareLinkId) {
            return new ShareLinkResultStats(shareLinkId, 0, 0, null, null);
        }
    }
}
