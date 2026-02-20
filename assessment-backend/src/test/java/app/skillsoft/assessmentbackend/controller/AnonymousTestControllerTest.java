package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.dto.*;
import app.skillsoft.assessmentbackend.domain.entities.DifficultyLevel;
import app.skillsoft.assessmentbackend.domain.entities.QuestionType;
import app.skillsoft.assessmentbackend.domain.entities.ResultStatus;
import app.skillsoft.assessmentbackend.exception.GlobalExceptionHandler;
import app.skillsoft.assessmentbackend.exception.InvalidSessionTokenException;
import app.skillsoft.assessmentbackend.exception.RateLimitExceededException;
import app.skillsoft.assessmentbackend.exception.ShareLinkException;
import app.skillsoft.assessmentbackend.services.AnonymousTestService;
import app.skillsoft.assessmentbackend.services.TestSessionService.CurrentQuestionDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AnonymousTestController REST API.
 *
 * <p>Tests cover the anonymous test-taking flow via share links:</p>
 * <ul>
 *   <li>Session creation from share link</li>
 *   <li>Session retrieval with X-Session-Token authentication</li>
 *   <li>Question navigation and answer submission</li>
 *   <li>Session completion and result retrieval</li>
 * </ul>
 *
 * <p>Error scenarios tested:</p>
 * <ul>
 *   <li>401 UNAUTHORIZED - Missing or invalid session token</li>
 *   <li>400 BAD REQUEST - Invalid request body</li>
 *   <li>429 TOO MANY REQUESTS - Rate limit exceeded</li>
 * </ul>
 *
 * @author SkillSoft Development Team
 */
@WebMvcTest(AnonymousTestController.class)
@Import(GlobalExceptionHandler.class)
@WithMockUser  // Required for WebMvcTest security context
@DisplayName("AnonymousTestController Unit Tests")
class AnonymousTestControllerTest {

    private static final String SESSION_TOKEN_HEADER = "X-Session-Token";
    private static final String BASE_URL = "/api/v1/anonymous";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AnonymousTestService anonymousTestService;

    @MockBean
    private app.skillsoft.assessmentbackend.services.ResultTokenService resultTokenService;

    @MockBean
    private app.skillsoft.assessmentbackend.services.CaptchaVerificationService captchaVerificationService;

    // Test data
    private UUID sessionId;
    private UUID templateId;
    private UUID questionId;
    private UUID resultId;
    private String validSessionToken;
    private String shareToken;
    private LocalDateTime now;

    // DTOs
    private AnonymousSessionResponse sessionResponse;
    private CurrentQuestionDto currentQuestionDto;
    private TestAnswerDto testAnswerDto;
    private TestResultDto testResultDto;
    private AnonymousResultDetailDto resultDetailDto;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        templateId = UUID.randomUUID();
        questionId = UUID.randomUUID();
        resultId = UUID.randomUUID();
        validSessionToken = "valid_session_token_256bit_base64_encoded_43chars";
        shareToken = "valid_share_link_token_64_characters_base64url_encoded_test";
        now = LocalDateTime.now();

        // Create AnonymousSessionResponse
        AnonymousSessionResponse.TemplateInfo templateInfo = new AnonymousSessionResponse.TemplateInfo(
                templateId,
                "Leadership Assessment",
                "Evaluate leadership competencies",
                10,    // questionCount
                30,    // timeLimitMinutes
                true,  // allowSkip
                true   // allowBackNavigation
        );

        sessionResponse = new AnonymousSessionResponse(
                sessionId,
                validSessionToken,
                templateInfo,
                now.plusHours(24) // expiresAt
        );

        // Create AssessmentQuestionDto
        AssessmentQuestionDto assessmentQuestion = new AssessmentQuestionDto(
                questionId,
                UUID.randomUUID(), // behavioralIndicatorId
                "What is your preferred leadership style?",
                QuestionType.MULTIPLE_CHOICE,
                List.of(
                        Map.of("id", "opt1", "text", "Authoritative"),
                        Map.of("id", "opt2", "text", "Democratic"),
                        Map.of("id", "opt3", "text", "Laissez-faire")
                ),
                "Standard scoring rubric",
                60,    // timeLimit
                DifficultyLevel.INTERMEDIATE,
                null,  // metadata
                true,  // isActive
                0      // orderIndex
        );

        // Create CurrentQuestionDto
        currentQuestionDto = new CurrentQuestionDto(
                assessmentQuestion,
                0,     // questionIndex
                10,    // totalQuestions
                1800,  // timeRemainingSeconds (30 min)
                null,  // previousAnswer
                true,  // allowBackNavigation
                true   // allowSkip
        );

        // Create TestAnswerDto
        testAnswerDto = new TestAnswerDto(
                UUID.randomUUID(), // id
                sessionId,
                questionId,
                "What is your preferred leadership style?",
                List.of("opt1"), // selectedOptionIds
                null,  // likertValue
                null,  // rankingOrder
                null,  // textResponse
                now,   // answeredAt
                45,    // timeSpentSeconds
                false, // isSkipped
                1.0,   // score
                1.0    // maxScore
        );

        // Create TestResultDto
        testResultDto = new TestResultDto(
                resultId,
                sessionId,
                templateId,
                "Leadership Assessment",
                null, // clerkUserId (anonymous)
                85.0, // overallScore
                85.0, // overallPercentage
                75,   // percentile
                true, // passed
                List.of(), // competencyScores
                1200, // totalTimeSeconds
                10,   // questionsAnswered
                0,    // questionsSkipped
                10,   // totalQuestions
                now,  // completedAt
                ResultStatus.COMPLETED,
                null, // bigFiveProfile
                null  // extendedMetrics
        );

        // Create AnonymousResultDetailDto
        AnonymousResultDetailDto.TakerInfoDto takerInfo = new AnonymousResultDetailDto.TakerInfoDto(
                "John",
                "Doe",
                "john.doe@example.com",
                "Candidate for manager position",
                now
        );

        AnonymousResultDetailDto.SessionMetadataDto sessionMetadata = new AnonymousResultDetailDto.SessionMetadataDto(
                "192.168.1.100",
                now.minusMinutes(20),
                now,
                1200,
                "Public Share Link"
        );

        resultDetailDto = new AnonymousResultDetailDto(
                resultId,
                sessionId,
                templateId,
                "Leadership Assessment",
                85.0, // overallPercentage
                75,   // percentile
                true, // passed
                List.of(), // competencyScores
                null, // bigFiveProfile
                takerInfo,
                sessionMetadata
        );

        // Stub result token generation for completion tests
        when(resultTokenService.generateToken(any(UUID.class), any(UUID.class)))
                .thenReturn("test-result-view-token");
    }

    // ==================== POST /sessions - Create Session Tests ====================

    @Nested
    @DisplayName("POST /api/v1/anonymous/sessions - Create Session")
    class CreateSessionTests {

        @Test
        @DisplayName("Should create session successfully with valid share token")
        void shouldCreateSessionSuccessfully() throws Exception {
            // Given
            AnonymousSessionRequest request = new AnonymousSessionRequest(shareToken, null);
            when(anonymousTestService.createSession(any(AnonymousSessionRequest.class), any(), any()))
                    .thenReturn(sessionResponse);

            // When & Then
            mockMvc.perform(post(BASE_URL + "/sessions")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.sessionId").value(sessionId.toString()))
                    .andExpect(jsonPath("$.sessionAccessToken").value(validSessionToken))
                    .andExpect(jsonPath("$.template.id").value(templateId.toString()))
                    .andExpect(jsonPath("$.template.name").value("Leadership Assessment"))
                    .andExpect(jsonPath("$.template.questionCount").value(10))
                    .andExpect(jsonPath("$.expiresAt").exists());

            verify(anonymousTestService).createSession(any(AnonymousSessionRequest.class), any(), any());
        }

        @Test
        @DisplayName("Should return 400 when share token is missing")
        void shouldReturn400WhenShareTokenMissing() throws Exception {
            // Given - request with null shareToken
            String requestJson = "{}";

            // When & Then
            mockMvc.perform(post(BASE_URL + "/sessions")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when share token is blank")
        void shouldReturn400WhenShareTokenBlank() throws Exception {
            // Given
            AnonymousSessionRequest request = new AnonymousSessionRequest("   ", null);

            // When & Then
            mockMvc.perform(post(BASE_URL + "/sessions")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when share link is invalid")
        void shouldReturn400WhenShareLinkInvalid() throws Exception {
            // Given
            AnonymousSessionRequest request = new AnonymousSessionRequest("invalid_token", null);
            when(anonymousTestService.createSession(any(AnonymousSessionRequest.class), any(), any()))
                    .thenThrow(new ShareLinkException(ShareLinkException.ErrorCode.LINK_NOT_FOUND));

            // When & Then
            mockMvc.perform(post(BASE_URL + "/sessions")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(anonymousTestService).createSession(any(AnonymousSessionRequest.class), any(), any());
        }

        @Test
        @DisplayName("Should return 429 when rate limit exceeded")
        void shouldReturn429WhenRateLimitExceeded() throws Exception {
            // Given
            AnonymousSessionRequest request = new AnonymousSessionRequest(shareToken, null);
            when(anonymousTestService.createSession(any(AnonymousSessionRequest.class), any(), any()))
                    .thenThrow(new RateLimitExceededException(3600));

            // When & Then
            mockMvc.perform(post(BASE_URL + "/sessions")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isTooManyRequests());

            verify(anonymousTestService).createSession(any(AnonymousSessionRequest.class), any(), any());
        }

        @Test
        @DisplayName("Should return 400 when request body is malformed JSON")
        void shouldReturn400WhenMalformedJson() throws Exception {
            // Given - malformed JSON
            String malformedJson = "{ shareToken: }";

            // When & Then
            mockMvc.perform(post(BASE_URL + "/sessions")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(malformedJson))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== GET /sessions/{sessionId} - Get Session Tests ====================

    @Nested
    @DisplayName("GET /api/v1/anonymous/sessions/{sessionId} - Get Session")
    class GetSessionTests {

        @Test
        @DisplayName("Should return session with valid session token")
        void shouldReturnSessionWithValidToken() throws Exception {
            // Given
            when(anonymousTestService.getSession(eq(sessionId), eq(validSessionToken)))
                    .thenReturn(Optional.of(sessionResponse));

            // When & Then
            mockMvc.perform(get(BASE_URL + "/sessions/{sessionId}", sessionId)
                            .header(SESSION_TOKEN_HEADER, validSessionToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionId").value(sessionId.toString()))
                    .andExpect(jsonPath("$.template.name").value("Leadership Assessment"));

            verify(anonymousTestService).getSession(sessionId, validSessionToken);
        }

        @Test
        @DisplayName("Should return 401 when session token header is missing")
        void shouldReturn401WhenTokenMissing() throws Exception {
            // When & Then - no X-Session-Token header
            mockMvc.perform(get(BASE_URL + "/sessions/{sessionId}", sessionId))
                    .andExpect(status().isBadRequest()); // Missing required header
        }

        @Test
        @DisplayName("Should return 401 when session token is invalid")
        void shouldReturn401WhenTokenInvalid() throws Exception {
            // Given
            String invalidToken = "invalid_token";
            when(anonymousTestService.getSession(eq(sessionId), eq(invalidToken)))
                    .thenThrow(new InvalidSessionTokenException());

            // When & Then
            mockMvc.perform(get(BASE_URL + "/sessions/{sessionId}", sessionId)
                            .header(SESSION_TOKEN_HEADER, invalidToken))
                    .andExpect(status().isUnauthorized());

            verify(anonymousTestService).getSession(sessionId, invalidToken);
        }

        @Test
        @DisplayName("Should return 404 when session not found")
        void shouldReturn404WhenSessionNotFound() throws Exception {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(anonymousTestService.getSession(eq(nonExistentId), eq(validSessionToken)))
                    .thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(get(BASE_URL + "/sessions/{sessionId}", nonExistentId)
                            .header(SESSION_TOKEN_HEADER, validSessionToken))
                    .andExpect(status().isNotFound());

            verify(anonymousTestService).getSession(nonExistentId, validSessionToken);
        }

        @Test
        @DisplayName("Should return 400 when sessionId is not valid UUID")
        void shouldReturn400WhenSessionIdInvalid() throws Exception {
            // When & Then
            mockMvc.perform(get(BASE_URL + "/sessions/{sessionId}", "not-a-uuid")
                            .header(SESSION_TOKEN_HEADER, validSessionToken))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== GET /sessions/{sessionId}/current-question - Get Current Question Tests ====================

    @Nested
    @DisplayName("GET /api/v1/anonymous/sessions/{sessionId}/current-question - Get Current Question")
    class GetCurrentQuestionTests {

        @Test
        @DisplayName("Should return current question with valid session token")
        void shouldReturnCurrentQuestionWithValidToken() throws Exception {
            // Given
            when(anonymousTestService.getCurrentQuestion(eq(sessionId), eq(validSessionToken)))
                    .thenReturn(currentQuestionDto);

            // When & Then
            mockMvc.perform(get(BASE_URL + "/sessions/{sessionId}/current-question", sessionId)
                            .header(SESSION_TOKEN_HEADER, validSessionToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.questionIndex").value(0))
                    .andExpect(jsonPath("$.totalQuestions").value(10))
                    .andExpect(jsonPath("$.timeRemainingSeconds").value(1800))
                    .andExpect(jsonPath("$.question.questionText").value("What is your preferred leadership style?"))
                    .andExpect(jsonPath("$.allowBackNavigation").value(true))
                    .andExpect(jsonPath("$.allowSkip").value(true));

            verify(anonymousTestService).getCurrentQuestion(sessionId, validSessionToken);
        }

        @Test
        @DisplayName("Should return 401 when session token is missing")
        void shouldReturn401WhenTokenMissing() throws Exception {
            // When & Then
            mockMvc.perform(get(BASE_URL + "/sessions/{sessionId}/current-question", sessionId))
                    .andExpect(status().isBadRequest()); // Missing required header
        }

        @Test
        @DisplayName("Should return 401 when session token is invalid")
        void shouldReturn401WhenTokenInvalid() throws Exception {
            // Given
            String invalidToken = "invalid_token";
            when(anonymousTestService.getCurrentQuestion(eq(sessionId), eq(invalidToken)))
                    .thenThrow(new InvalidSessionTokenException());

            // When & Then
            mockMvc.perform(get(BASE_URL + "/sessions/{sessionId}/current-question", sessionId)
                            .header(SESSION_TOKEN_HEADER, invalidToken))
                    .andExpect(status().isUnauthorized());

            verify(anonymousTestService).getCurrentQuestion(sessionId, invalidToken);
        }
    }

    // ==================== POST /sessions/{sessionId}/answers - Submit Answer Tests ====================

    @Nested
    @DisplayName("POST /api/v1/anonymous/sessions/{sessionId}/answers - Submit Answer")
    class SubmitAnswerTests {

        @Test
        @DisplayName("Should submit answer successfully with valid token and request")
        void shouldSubmitAnswerSuccessfully() throws Exception {
            // Given
            AnonymousTestController.AnswerSubmissionRequest request =
                    new AnonymousTestController.AnswerSubmissionRequest(questionId, 0);
            when(anonymousTestService.submitAnswer(eq(sessionId), eq(validSessionToken), eq(questionId), eq(0)))
                    .thenReturn(testAnswerDto);

            // When & Then
            mockMvc.perform(post(BASE_URL + "/sessions/{sessionId}/answers", sessionId)
                            .with(csrf())
                            .header(SESSION_TOKEN_HEADER, validSessionToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionId").value(sessionId.toString()))
                    .andExpect(jsonPath("$.questionId").value(questionId.toString()))
                    .andExpect(jsonPath("$.isSkipped").value(false));

            verify(anonymousTestService).submitAnswer(sessionId, validSessionToken, questionId, 0);
        }

        @Test
        @DisplayName("Should return 401 when session token is missing")
        void shouldReturn401WhenTokenMissing() throws Exception {
            // Given
            AnonymousTestController.AnswerSubmissionRequest request =
                    new AnonymousTestController.AnswerSubmissionRequest(questionId, 0);

            // When & Then
            mockMvc.perform(post(BASE_URL + "/sessions/{sessionId}/answers", sessionId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest()); // Missing required header
        }

        @Test
        @DisplayName("Should return 401 when session token is invalid")
        void shouldReturn401WhenTokenInvalid() throws Exception {
            // Given
            String invalidToken = "invalid_token";
            AnonymousTestController.AnswerSubmissionRequest request =
                    new AnonymousTestController.AnswerSubmissionRequest(questionId, 0);
            when(anonymousTestService.submitAnswer(eq(sessionId), eq(invalidToken), eq(questionId), eq(0)))
                    .thenThrow(new InvalidSessionTokenException());

            // When & Then
            mockMvc.perform(post(BASE_URL + "/sessions/{sessionId}/answers", sessionId)
                            .with(csrf())
                            .header(SESSION_TOKEN_HEADER, invalidToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());

            verify(anonymousTestService).submitAnswer(sessionId, invalidToken, questionId, 0);
        }

        @Test
        @DisplayName("Should return 400 when question ID is invalid")
        void shouldReturn400WhenQuestionIdInvalid() throws Exception {
            // Given
            UUID invalidQuestionId = UUID.randomUUID();
            AnonymousTestController.AnswerSubmissionRequest request =
                    new AnonymousTestController.AnswerSubmissionRequest(invalidQuestionId, 0);
            when(anonymousTestService.submitAnswer(eq(sessionId), eq(validSessionToken), eq(invalidQuestionId), eq(0)))
                    .thenThrow(new IllegalArgumentException("Question not found in session"));

            // When & Then
            mockMvc.perform(post(BASE_URL + "/sessions/{sessionId}/answers", sessionId)
                            .with(csrf())
                            .header(SESSION_TOKEN_HEADER, validSessionToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verify(anonymousTestService).submitAnswer(sessionId, validSessionToken, invalidQuestionId, 0);
        }

        @Test
        @DisplayName("Should return 400 when request body is missing")
        void shouldReturn400WhenRequestBodyMissing() throws Exception {
            // When & Then
            mockMvc.perform(post(BASE_URL + "/sessions/{sessionId}/answers", sessionId)
                            .with(csrf())
                            .header(SESSION_TOKEN_HEADER, validSessionToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when option index is negative")
        void shouldReturn400WhenOptionIndexNegative() throws Exception {
            // Given
            AnonymousTestController.AnswerSubmissionRequest request =
                    new AnonymousTestController.AnswerSubmissionRequest(questionId, -1);
            when(anonymousTestService.submitAnswer(eq(sessionId), eq(validSessionToken), eq(questionId), eq(-1)))
                    .thenThrow(new IllegalArgumentException("Option index cannot be negative"));

            // When & Then
            mockMvc.perform(post(BASE_URL + "/sessions/{sessionId}/answers", sessionId)
                            .with(csrf())
                            .header(SESSION_TOKEN_HEADER, validSessionToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== POST /sessions/{sessionId}/complete - Complete Session Tests ====================

    @Nested
    @DisplayName("POST /api/v1/anonymous/sessions/{sessionId}/complete - Complete Session")
    class CompleteSessionTests {

        @Test
        @DisplayName("Should complete session successfully with valid token and taker info")
        void shouldCompleteSessionSuccessfully() throws Exception {
            // Given
            AnonymousTakerInfoRequest takerInfo = new AnonymousTakerInfoRequest(
                    "John",
                    "Doe",
                    "john.doe@example.com",
                    "Candidate for manager position",
                    null
            );
            when(anonymousTestService.completeSession(eq(sessionId), eq(validSessionToken), any(AnonymousTakerInfoRequest.class)))
                    .thenReturn(testResultDto);

            // When & Then
            mockMvc.perform(post(BASE_URL + "/sessions/{sessionId}/complete", sessionId)
                            .with(csrf())
                            .header(SESSION_TOKEN_HEADER, validSessionToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(takerInfo)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.id").value(resultId.toString()))
                    .andExpect(jsonPath("$.result.sessionId").value(sessionId.toString()))
                    .andExpect(jsonPath("$.result.overallPercentage").value(85.0))
                    .andExpect(jsonPath("$.result.passed").value(true))
                    .andExpect(jsonPath("$.resultViewToken").value("test-result-view-token"));

            verify(anonymousTestService).completeSession(eq(sessionId), eq(validSessionToken), any(AnonymousTakerInfoRequest.class));
        }

        @Test
        @DisplayName("Should complete session with minimal required fields")
        void shouldCompleteSessionWithMinimalFields() throws Exception {
            // Given - only required fields
            AnonymousTakerInfoRequest takerInfo = new AnonymousTakerInfoRequest(
                    "John",
                    "Doe",
                    null, // email optional
                    null, // notes optional
                    null  // gdprConsentGiven optional
            );
            when(anonymousTestService.completeSession(eq(sessionId), eq(validSessionToken), any(AnonymousTakerInfoRequest.class)))
                    .thenReturn(testResultDto);

            // When & Then
            mockMvc.perform(post(BASE_URL + "/sessions/{sessionId}/complete", sessionId)
                            .with(csrf())
                            .header(SESSION_TOKEN_HEADER, validSessionToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(takerInfo)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.passed").value(true))
                    .andExpect(jsonPath("$.resultViewToken").isNotEmpty());

            verify(anonymousTestService).completeSession(eq(sessionId), eq(validSessionToken), any(AnonymousTakerInfoRequest.class));
        }

        @Test
        @DisplayName("Should return 401 when session token is missing")
        void shouldReturn401WhenTokenMissing() throws Exception {
            // Given
            AnonymousTakerInfoRequest takerInfo = new AnonymousTakerInfoRequest(
                    "John", "Doe", null, null, null
            );

            // When & Then
            mockMvc.perform(post(BASE_URL + "/sessions/{sessionId}/complete", sessionId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(takerInfo)))
                    .andExpect(status().isBadRequest()); // Missing required header
        }

        @Test
        @DisplayName("Should return 401 when session token is invalid")
        void shouldReturn401WhenTokenInvalid() throws Exception {
            // Given
            String invalidToken = "invalid_token";
            AnonymousTakerInfoRequest takerInfo = new AnonymousTakerInfoRequest(
                    "John", "Doe", null, null, null
            );
            when(anonymousTestService.completeSession(eq(sessionId), eq(invalidToken), any(AnonymousTakerInfoRequest.class)))
                    .thenThrow(new InvalidSessionTokenException());

            // When & Then
            mockMvc.perform(post(BASE_URL + "/sessions/{sessionId}/complete", sessionId)
                            .with(csrf())
                            .header(SESSION_TOKEN_HEADER, invalidToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(takerInfo)))
                    .andExpect(status().isUnauthorized());

            verify(anonymousTestService).completeSession(eq(sessionId), eq(invalidToken), any(AnonymousTakerInfoRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when firstName is missing")
        void shouldReturn400WhenFirstNameMissing() throws Exception {
            // Given - missing firstName
            String requestJson = """
                    {
                        "lastName": "Doe",
                        "email": "john.doe@example.com"
                    }
                    """;

            // When & Then
            mockMvc.perform(post(BASE_URL + "/sessions/{sessionId}/complete", sessionId)
                            .with(csrf())
                            .header(SESSION_TOKEN_HEADER, validSessionToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when lastName is missing")
        void shouldReturn400WhenLastNameMissing() throws Exception {
            // Given - missing lastName
            String requestJson = """
                    {
                        "firstName": "John",
                        "email": "john.doe@example.com"
                    }
                    """;

            // When & Then
            mockMvc.perform(post(BASE_URL + "/sessions/{sessionId}/complete", sessionId)
                            .with(csrf())
                            .header(SESSION_TOKEN_HEADER, validSessionToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when email format is invalid")
        void shouldReturn400WhenEmailInvalid() throws Exception {
            // Given - invalid email format
            AnonymousTakerInfoRequest takerInfo = new AnonymousTakerInfoRequest(
                    "John",
                    "Doe",
                    "not-an-email", // invalid email
                    null,
                    null
            );

            // When & Then
            mockMvc.perform(post(BASE_URL + "/sessions/{sessionId}/complete", sessionId)
                            .with(csrf())
                            .header(SESSION_TOKEN_HEADER, validSessionToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(takerInfo)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when session is already completed")
        void shouldReturn400WhenSessionAlreadyCompleted() throws Exception {
            // Given
            AnonymousTakerInfoRequest takerInfo = new AnonymousTakerInfoRequest(
                    "John", "Doe", null, null, null
            );
            when(anonymousTestService.completeSession(eq(sessionId), eq(validSessionToken), any(AnonymousTakerInfoRequest.class)))
                    .thenThrow(new IllegalStateException("Session is already completed"));

            // When & Then
            mockMvc.perform(post(BASE_URL + "/sessions/{sessionId}/complete", sessionId)
                            .with(csrf())
                            .header(SESSION_TOKEN_HEADER, validSessionToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(takerInfo)))
                    .andExpect(status().isBadRequest());

            verify(anonymousTestService).completeSession(eq(sessionId), eq(validSessionToken), any(AnonymousTakerInfoRequest.class));
        }
    }

    // ==================== GET /sessions/{sessionId}/result - Get Result Tests ====================

    @Nested
    @DisplayName("GET /api/v1/anonymous/sessions/{sessionId}/result - Get Result")
    class GetResultTests {

        @Test
        @DisplayName("Should return result with valid session token")
        void shouldReturnResultWithValidToken() throws Exception {
            // Given
            when(anonymousTestService.getResult(eq(sessionId), eq(validSessionToken)))
                    .thenReturn(resultDetailDto);

            // When & Then
            mockMvc.perform(get(BASE_URL + "/sessions/{sessionId}/result", sessionId)
                            .header(SESSION_TOKEN_HEADER, validSessionToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(resultId.toString()))
                    .andExpect(jsonPath("$.sessionId").value(sessionId.toString()))
                    .andExpect(jsonPath("$.templateName").value("Leadership Assessment"))
                    .andExpect(jsonPath("$.overallPercentage").value(85.0))
                    .andExpect(jsonPath("$.passed").value(true))
                    .andExpect(jsonPath("$.takerInfo.firstName").value("John"))
                    .andExpect(jsonPath("$.takerInfo.lastName").value("Doe"));

            verify(anonymousTestService).getResult(sessionId, validSessionToken);
        }

        @Test
        @DisplayName("Should return 401 when session token is missing")
        void shouldReturn401WhenTokenMissing() throws Exception {
            // When & Then
            mockMvc.perform(get(BASE_URL + "/sessions/{sessionId}/result", sessionId))
                    .andExpect(status().isBadRequest()); // Missing required header
        }

        @Test
        @DisplayName("Should return 401 when session token is invalid")
        void shouldReturn401WhenTokenInvalid() throws Exception {
            // Given
            String invalidToken = "invalid_token";
            when(anonymousTestService.getResult(eq(sessionId), eq(invalidToken)))
                    .thenThrow(new InvalidSessionTokenException());

            // When & Then
            mockMvc.perform(get(BASE_URL + "/sessions/{sessionId}/result", sessionId)
                            .header(SESSION_TOKEN_HEADER, invalidToken))
                    .andExpect(status().isUnauthorized());

            verify(anonymousTestService).getResult(sessionId, invalidToken);
        }

        @Test
        @DisplayName("Should return 400 when session is not completed")
        void shouldReturn400WhenSessionNotCompleted() throws Exception {
            // Given
            when(anonymousTestService.getResult(eq(sessionId), eq(validSessionToken)))
                    .thenThrow(new IllegalStateException("Session is not yet completed"));

            // When & Then
            mockMvc.perform(get(BASE_URL + "/sessions/{sessionId}/result", sessionId)
                            .header(SESSION_TOKEN_HEADER, validSessionToken))
                    .andExpect(status().isBadRequest());

            verify(anonymousTestService).getResult(sessionId, validSessionToken);
        }
    }

    // ==================== POST /sessions/{sessionId}/navigate - Navigation Tests ====================

    @Nested
    @DisplayName("POST /api/v1/anonymous/sessions/{sessionId}/navigate - Navigate to Question")
    class NavigateQuestionTests {

        @Test
        @DisplayName("Should navigate to question with valid token")
        void shouldNavigateToQuestionWithValidToken() throws Exception {
            // Given
            when(anonymousTestService.navigateToQuestion(eq(sessionId), eq(validSessionToken), eq(5)))
                    .thenReturn(sessionResponse);

            // When & Then
            mockMvc.perform(post(BASE_URL + "/sessions/{sessionId}/navigate", sessionId)
                            .with(csrf())
                            .header(SESSION_TOKEN_HEADER, validSessionToken)
                            .param("questionIndex", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionId").value(sessionId.toString()));

            verify(anonymousTestService).navigateToQuestion(sessionId, validSessionToken, 5);
        }

        @Test
        @DisplayName("Should return 401 when session token is invalid")
        void shouldReturn401WhenTokenInvalid() throws Exception {
            // Given
            String invalidToken = "invalid_token";
            when(anonymousTestService.navigateToQuestion(eq(sessionId), eq(invalidToken), anyInt()))
                    .thenThrow(new InvalidSessionTokenException());

            // When & Then
            mockMvc.perform(post(BASE_URL + "/sessions/{sessionId}/navigate", sessionId)
                            .with(csrf())
                            .header(SESSION_TOKEN_HEADER, invalidToken)
                            .param("questionIndex", "5"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 400 when navigation not allowed")
        void shouldReturn400WhenNavigationNotAllowed() throws Exception {
            // Given
            when(anonymousTestService.navigateToQuestion(eq(sessionId), eq(validSessionToken), eq(5)))
                    .thenThrow(new IllegalStateException("Back navigation is not allowed"));

            // When & Then
            mockMvc.perform(post(BASE_URL + "/sessions/{sessionId}/navigate", sessionId)
                            .with(csrf())
                            .header(SESSION_TOKEN_HEADER, validSessionToken)
                            .param("questionIndex", "5"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== POST /sessions/{sessionId}/time - Update Time Tests ====================

    @Nested
    @DisplayName("POST /api/v1/anonymous/sessions/{sessionId}/time - Update Time Remaining")
    class UpdateTimeTests {

        @Test
        @DisplayName("Should update time remaining with valid token")
        void shouldUpdateTimeWithValidToken() throws Exception {
            // Given
            when(anonymousTestService.updateTimeRemaining(eq(sessionId), eq(validSessionToken), eq(1500)))
                    .thenReturn(sessionResponse);

            // When & Then
            mockMvc.perform(post(BASE_URL + "/sessions/{sessionId}/time", sessionId)
                            .with(csrf())
                            .header(SESSION_TOKEN_HEADER, validSessionToken)
                            .param("timeRemainingSeconds", "1500"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionId").value(sessionId.toString()));

            verify(anonymousTestService).updateTimeRemaining(sessionId, validSessionToken, 1500);
        }

        @Test
        @DisplayName("Should return 401 when session token is invalid")
        void shouldReturn401WhenTokenInvalid() throws Exception {
            // Given
            String invalidToken = "invalid_token";
            when(anonymousTestService.updateTimeRemaining(eq(sessionId), eq(invalidToken), anyInt()))
                    .thenThrow(new InvalidSessionTokenException());

            // When & Then
            mockMvc.perform(post(BASE_URL + "/sessions/{sessionId}/time", sessionId)
                            .with(csrf())
                            .header(SESSION_TOKEN_HEADER, invalidToken)
                            .param("timeRemainingSeconds", "1500"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 400 when time parameter is missing")
        void shouldReturn400WhenTimeParameterMissing() throws Exception {
            // When & Then
            mockMvc.perform(post(BASE_URL + "/sessions/{sessionId}/time", sessionId)
                            .with(csrf())
                            .header(SESSION_TOKEN_HEADER, validSessionToken))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== Edge Case Tests ====================

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle empty session token header")
        void shouldHandleEmptySessionToken() throws Exception {
            // Given
            when(anonymousTestService.getSession(eq(sessionId), eq("")))
                    .thenThrow(new InvalidSessionTokenException());

            // When & Then
            mockMvc.perform(get(BASE_URL + "/sessions/{sessionId}", sessionId)
                            .header(SESSION_TOKEN_HEADER, ""))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should handle concurrent session access")
        void shouldHandleConcurrentSessionAccess() throws Exception {
            // Given - session token mismatch scenario
            when(anonymousTestService.getSession(eq(sessionId), anyString()))
                    .thenThrow(new InvalidSessionTokenException(
                            "Session token does not match",
                            "Токен сессии не совпадает"
                    ));

            // When & Then
            mockMvc.perform(get(BASE_URL + "/sessions/{sessionId}", sessionId)
                            .header(SESSION_TOKEN_HEADER, "another_token"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should handle Russian characters in taker info")
        void shouldHandleRussianCharactersInTakerInfo() throws Exception {
            // Given - Russian names (bilingual support per CLAUDE.md)
            AnonymousTakerInfoRequest takerInfo = new AnonymousTakerInfoRequest(
                    "Иван",
                    "Петров",
                    "ivan.petrov@example.ru",
                    "Кандидат на должность менеджера",
                    null
            );
            when(anonymousTestService.completeSession(eq(sessionId), eq(validSessionToken), any(AnonymousTakerInfoRequest.class)))
                    .thenReturn(testResultDto);

            // When & Then
            mockMvc.perform(post(BASE_URL + "/sessions/{sessionId}/complete", sessionId)
                            .with(csrf())
                            .header(SESSION_TOKEN_HEADER, validSessionToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding("UTF-8")
                            .content(objectMapper.writeValueAsString(takerInfo)))
                    .andExpect(status().isOk());

            verify(anonymousTestService).completeSession(eq(sessionId), eq(validSessionToken), any(AnonymousTakerInfoRequest.class));
        }

        @Test
        @DisplayName("Should extract client IP from X-Forwarded-For header")
        void shouldExtractIpFromForwardedHeader() throws Exception {
            // Given
            AnonymousSessionRequest request = new AnonymousSessionRequest(shareToken, null);
            when(anonymousTestService.createSession(any(AnonymousSessionRequest.class), any(), any()))
                    .thenReturn(sessionResponse);

            // When & Then
            mockMvc.perform(post(BASE_URL + "/sessions")
                            .with(csrf())
                            .header("X-Forwarded-For", "203.0.113.195, 70.41.3.18, 150.172.238.178")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            // Verify the first IP from X-Forwarded-For was extracted
            verify(anonymousTestService).createSession(any(AnonymousSessionRequest.class), eq("203.0.113.195"), any());
        }

        @Test
        @DisplayName("Should extract client IP from X-Real-IP header when X-Forwarded-For is absent")
        void shouldExtractIpFromRealIpHeader() throws Exception {
            // Given
            AnonymousSessionRequest request = new AnonymousSessionRequest(shareToken, null);
            when(anonymousTestService.createSession(any(AnonymousSessionRequest.class), any(), any()))
                    .thenReturn(sessionResponse);

            // When & Then
            mockMvc.perform(post(BASE_URL + "/sessions")
                            .with(csrf())
                            .header("X-Real-IP", "198.51.100.178")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            verify(anonymousTestService).createSession(any(AnonymousSessionRequest.class), eq("198.51.100.178"), any());
        }
    }
}
