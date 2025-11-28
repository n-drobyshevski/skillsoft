package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.dto.*;
import app.skillsoft.assessmentbackend.domain.entities.DifficultyLevel;
import app.skillsoft.assessmentbackend.domain.entities.QuestionType;
import app.skillsoft.assessmentbackend.domain.entities.SessionStatus;
import app.skillsoft.assessmentbackend.services.TestSessionService;
import app.skillsoft.assessmentbackend.services.TestSessionService.CurrentQuestionDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for TestSessionController using @WebMvcTest.
 * 
 * Tests cover:
 * - Session lifecycle (start, complete, abandon)
 * - Answer submission
 * - Question navigation
 * - User session queries
 */
@WebMvcTest(TestSessionController.class)
@DisplayName("TestSession Controller Tests")
class TestSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TestSessionService testSessionService;

    private UUID sessionId;
    private UUID templateId;
    private UUID questionId;
    private String clerkUserId;
    private TestSessionDto testSessionDto;
    private TestSessionSummaryDto testSessionSummaryDto;
    private TestAnswerDto testAnswerDto;
    private TestResultDto testResultDto;
    private AssessmentQuestionDto assessmentQuestionDto;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        templateId = UUID.randomUUID();
        questionId = UUID.randomUUID();
        clerkUserId = "user_test123";
        now = LocalDateTime.now();

        // TestSessionDto: id, templateId, templateName, clerkUserId, status, startedAt, completedAt,
        //                 currentQuestionIndex, timeRemainingSeconds, questionOrder, totalQuestions,
        //                 answeredQuestions, lastActivityAt, createdAt
        testSessionDto = new TestSessionDto(
                sessionId,
                templateId,
                "Leadership Test",
                clerkUserId,
                SessionStatus.IN_PROGRESS,
                now,              // startedAt
                null,             // completedAt
                0,                // currentQuestionIndex
                3600,             // timeRemainingSeconds (60 min)
                List.of(questionId, UUID.randomUUID(), UUID.randomUUID()),  // questionOrder
                3,                // totalQuestions
                0,                // answeredQuestions
                now,              // lastActivityAt
                now               // createdAt
        );

        // TestSessionSummaryDto: id, templateId, templateName, status, progress, timeRemainingSeconds,
        //                        startedAt, completedAt, createdAt
        testSessionSummaryDto = new TestSessionSummaryDto(
                sessionId,
                templateId,
                "Leadership Test",
                SessionStatus.IN_PROGRESS,
                33,               // progress (percentage)
                3600,             // timeRemainingSeconds
                now,              // startedAt
                null,             // completedAt
                now               // createdAt
        );

        // TestAnswerDto: id, sessionId, questionId, questionText, selectedOptionIds, likertValue,
        //                rankingOrder, textResponse, answeredAt, timeSpentSeconds, isSkipped, score, maxScore
        testAnswerDto = new TestAnswerDto(
                UUID.randomUUID(),
                sessionId,
                questionId,
                "Sample question text?",
                List.of("option1"),
                null,             // likertValue
                null,             // rankingOrder
                null,             // textResponse
                now,              // answeredAt
                30,               // timeSpentSeconds
                false,            // isSkipped
                1.0,              // score
                1.0               // maxScore
        );

        // TestResultDto: id, sessionId, templateId, templateName, clerkUserId, overallScore, overallPercentage,
        //                percentile, passed, competencyScores, totalTimeSeconds, questionsAnswered,
        //                questionsSkipped, totalQuestions, completedAt
        testResultDto = new TestResultDto(
                UUID.randomUUID(),
                sessionId,
                templateId,
                "Leadership Test",
                clerkUserId,
                75.0,             // overallScore
                75.0,             // overallPercentage
                65,               // percentile
                true,             // passed
                List.of(),        // competencyScores
                1800,             // totalTimeSeconds
                10,               // questionsAnswered
                0,                // questionsSkipped
                10,               // totalQuestions
                now               // completedAt
        );

        // AssessmentQuestionDto: id, behavioralIndicatorId, questionText, questionType, answerOptions,
        //                        scoringRubric, timeLimit, difficultyLevel, isActive, orderIndex
        assessmentQuestionDto = new AssessmentQuestionDto(
                questionId,
                UUID.randomUUID(),
                "What is your leadership style?",
                QuestionType.MULTIPLE_CHOICE,
                List.of(
                        Map.of("id", "option1", "text", "Authoritative"),
                        Map.of("id", "option2", "text", "Democratic")
                ),
                "Standard scoring",
                60,
                DifficultyLevel.INTERMEDIATE,
                true,
                1
        );
    }

    @Nested
    @DisplayName("POST /api/v1/tests/sessions - Start Session Tests")
    class StartSessionTests {

        @Test
        @WithMockUser
        @DisplayName("Should start a new session successfully")
        void shouldStartNewSession() throws Exception {
            // Given
            StartTestSessionRequest request = new StartTestSessionRequest(templateId, clerkUserId);
            when(testSessionService.startSession(any(StartTestSessionRequest.class)))
                    .thenReturn(testSessionDto);

            // When & Then
            mockMvc.perform(post("/api/v1/tests/sessions")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(sessionId.toString()))
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                    .andExpect(jsonPath("$.totalQuestions").value(3));

            verify(testSessionService).startSession(any(StartTestSessionRequest.class));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 when template not found")
        void shouldReturn404WhenTemplateNotFound() throws Exception {
            // Given
            StartTestSessionRequest request = new StartTestSessionRequest(templateId, clerkUserId);
            when(testSessionService.startSession(any(StartTestSessionRequest.class)))
                    .thenThrow(new RuntimeException("Template not found"));

            // When & Then
            mockMvc.perform(post("/api/v1/tests/sessions")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 for invalid request")
        void shouldReturn400ForInvalidRequest() throws Exception {
            // Given - invalid request with null templateId
            StartTestSessionRequest request = new StartTestSessionRequest(null, clerkUserId);

            // When & Then
            mockMvc.perform(post("/api/v1/tests/sessions")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/tests/sessions/{sessionId} - Get Session Tests")
    class GetSessionTests {

        @Test
        @WithMockUser
        @DisplayName("Should return session by id")
        void shouldReturnSessionById() throws Exception {
            // Given
            when(testSessionService.findById(sessionId)).thenReturn(Optional.of(testSessionDto));

            // When & Then
            mockMvc.perform(get("/api/v1/tests/sessions/{sessionId}", sessionId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(sessionId.toString()))
                    .andExpect(jsonPath("$.templateName").value("Leadership Test"));

            verify(testSessionService).findById(sessionId);
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 when session not found")
        void shouldReturn404WhenNotFound() throws Exception {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(testSessionService.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(get("/api/v1/tests/sessions/{sessionId}", nonExistentId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/tests/sessions/{sessionId}/complete - Complete Session Tests")
    class CompleteSessionTests {

        @Test
        @WithMockUser
        @DisplayName("Should complete session and return result")
        void shouldCompleteSession() throws Exception {
            // Given
            when(testSessionService.completeSession(sessionId)).thenReturn(testResultDto);

            // When & Then
            mockMvc.perform(post("/api/v1/tests/sessions/{sessionId}/complete", sessionId)
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionId").value(sessionId.toString()))
                    .andExpect(jsonPath("$.overallPercentage").value(75.0))
                    .andExpect(jsonPath("$.passed").value(true));

            verify(testSessionService).completeSession(sessionId);
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 when session cannot be completed")
        void shouldReturn400WhenCannotComplete() throws Exception {
            // Given
            when(testSessionService.completeSession(sessionId))
                    .thenThrow(new IllegalStateException("Session already completed"));

            // When & Then
            mockMvc.perform(post("/api/v1/tests/sessions/{sessionId}/complete", sessionId)
                            .with(csrf()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 when session not found")
        void shouldReturn404WhenSessionNotFound() throws Exception {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(testSessionService.completeSession(nonExistentId))
                    .thenThrow(new RuntimeException("Session not found"));

            // When & Then
            mockMvc.perform(post("/api/v1/tests/sessions/{sessionId}/complete", nonExistentId)
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/tests/sessions/{sessionId}/abandon - Abandon Session Tests")
    class AbandonSessionTests {

        @Test
        @WithMockUser
        @DisplayName("Should abandon session successfully")
        void shouldAbandonSession() throws Exception {
            // Given
            TestSessionDto abandonedSession = new TestSessionDto(
                    sessionId, templateId, "Leadership Test", clerkUserId,
                    SessionStatus.ABANDONED, now, now, 0, 0,
                    List.of(), 3, 0, now, now
            );
            when(testSessionService.abandonSession(sessionId)).thenReturn(abandonedSession);

            // When & Then
            mockMvc.perform(post("/api/v1/tests/sessions/{sessionId}/abandon", sessionId)
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ABANDONED"));

            verify(testSessionService).abandonSession(sessionId);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/tests/sessions/{sessionId}/answers - Submit Answer Tests")
    class SubmitAnswerTests {

        @Test
        @WithMockUser
        @DisplayName("Should submit answer successfully")
        void shouldSubmitAnswer() throws Exception {
            // Given
            SubmitAnswerRequest request = new SubmitAnswerRequest(
                    sessionId, questionId,
                    List.of("option1"),  // selectedOptionIds
                    null, null, null,    // likertValue, rankingOrder, textResponse
                    30,                  // timeSpentSeconds
                    false                // skip
            );
            when(testSessionService.submitAnswer(any(SubmitAnswerRequest.class)))
                    .thenReturn(testAnswerDto);

            // When & Then
            mockMvc.perform(post("/api/v1/tests/sessions/{sessionId}/answers", sessionId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.questionId").value(questionId.toString()))
                    .andExpect(jsonPath("$.isSkipped").value(false));

            verify(testSessionService).submitAnswer(any(SubmitAnswerRequest.class));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 400 when session ID mismatch")
        void shouldReturn400WhenSessionIdMismatch() throws Exception {
            // Given - request with different sessionId than path
            UUID differentSessionId = UUID.randomUUID();
            SubmitAnswerRequest request = new SubmitAnswerRequest(
                    differentSessionId, questionId,
                    List.of("option1"), null, null, null, 30, false
            );

            // When & Then
            mockMvc.perform(post("/api/v1/tests/sessions/{sessionId}/answers", sessionId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/tests/sessions/{sessionId}/answers - Get Answers Tests")
    class GetAnswersTests {

        @Test
        @WithMockUser
        @DisplayName("Should return session answers")
        void shouldReturnSessionAnswers() throws Exception {
            // Given
            when(testSessionService.getSessionAnswers(sessionId))
                    .thenReturn(List.of(testAnswerDto));

            // When & Then
            mockMvc.perform(get("/api/v1/tests/sessions/{sessionId}/answers", sessionId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].sessionId").value(sessionId.toString()));

            verify(testSessionService).getSessionAnswers(sessionId);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/tests/sessions/{sessionId}/current-question - Get Current Question Tests")
    class GetCurrentQuestionTests {

        @Test
        @WithMockUser
        @DisplayName("Should return current question")
        void shouldReturnCurrentQuestion() throws Exception {
            // Given
            CurrentQuestionDto currentQuestion = new CurrentQuestionDto(
                    assessmentQuestionDto, 0, 3, 3600, null
            );
            when(testSessionService.getCurrentQuestion(sessionId)).thenReturn(currentQuestion);

            // When & Then
            mockMvc.perform(get("/api/v1/tests/sessions/{sessionId}/current-question", sessionId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.questionIndex").value(0))
                    .andExpect(jsonPath("$.totalQuestions").value(3))
                    .andExpect(jsonPath("$.question.questionText").value("What is your leadership style?"));

            verify(testSessionService).getCurrentQuestion(sessionId);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/tests/sessions/{sessionId}/navigate - Navigate Question Tests")
    class NavigateQuestionTests {

        @Test
        @WithMockUser
        @DisplayName("Should navigate to specific question")
        void shouldNavigateToQuestion() throws Exception {
            // Given
            TestSessionDto updatedSession = new TestSessionDto(
                    sessionId, templateId, "Leadership Test", clerkUserId,
                    SessionStatus.IN_PROGRESS, now, null, 2, 3600,
                    List.of(questionId), 3, 1, now, now
            );
            when(testSessionService.navigateToQuestion(sessionId, 2)).thenReturn(updatedSession);

            // When & Then
            mockMvc.perform(post("/api/v1/tests/sessions/{sessionId}/navigate", sessionId)
                            .with(csrf())
                            .param("questionIndex", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currentQuestionIndex").value(2));

            verify(testSessionService).navigateToQuestion(sessionId, 2);
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/tests/sessions/{sessionId}/time - Update Time Tests")
    class UpdateTimeTests {

        @Test
        @WithMockUser
        @DisplayName("Should update time remaining")
        void shouldUpdateTimeRemaining() throws Exception {
            // Given
            TestSessionDto updatedSession = new TestSessionDto(
                    sessionId, templateId, "Leadership Test", clerkUserId,
                    SessionStatus.IN_PROGRESS, now, null, 0, 1800,
                    List.of(), 3, 0, now, now
            );
            when(testSessionService.updateTimeRemaining(sessionId, 1800)).thenReturn(updatedSession);

            // When & Then
            mockMvc.perform(put("/api/v1/tests/sessions/{sessionId}/time", sessionId)
                            .with(csrf())
                            .param("timeRemainingSeconds", "1800"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.timeRemainingSeconds").value(1800));

            verify(testSessionService).updateTimeRemaining(sessionId, 1800);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/tests/sessions/user/{clerkUserId} - User Sessions Tests")
    class UserSessionsTests {

        @Test
        @WithMockUser
        @DisplayName("Should return paginated user sessions")
        void shouldReturnUserSessions() throws Exception {
            // Given
            Page<TestSessionSummaryDto> sessionPage = new PageImpl<>(
                    List.of(testSessionSummaryDto),
                    Pageable.unpaged(),
                    1
            );
            when(testSessionService.findByUser(eq(clerkUserId), any(Pageable.class)))
                    .thenReturn(sessionPage);

            // When & Then
            mockMvc.perform(get("/api/v1/tests/sessions/user/{clerkUserId}", clerkUserId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].id").value(sessionId.toString()));

            verify(testSessionService).findByUser(eq(clerkUserId), any(Pageable.class));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return user sessions filtered by status")
        void shouldReturnUserSessionsByStatus() throws Exception {
            // Given
            when(testSessionService.findByUserAndStatus(clerkUserId, SessionStatus.IN_PROGRESS))
                    .thenReturn(List.of(testSessionSummaryDto));

            // When & Then
            mockMvc.perform(get("/api/v1/tests/sessions/user/{clerkUserId}/status/{status}",
                            clerkUserId, SessionStatus.IN_PROGRESS))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].status").value("IN_PROGRESS"));

            verify(testSessionService).findByUserAndStatus(clerkUserId, SessionStatus.IN_PROGRESS);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/tests/sessions/user/{clerkUserId}/in-progress - In-Progress Session Tests")
    class InProgressSessionTests {

        @Test
        @WithMockUser
        @DisplayName("Should return in-progress session")
        void shouldReturnInProgressSession() throws Exception {
            // Given
            when(testSessionService.findInProgressSession(clerkUserId, templateId))
                    .thenReturn(Optional.of(testSessionDto));

            // When & Then
            mockMvc.perform(get("/api/v1/tests/sessions/user/{clerkUserId}/in-progress", clerkUserId)
                            .param("templateId", templateId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(sessionId.toString()))
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

            verify(testSessionService).findInProgressSession(clerkUserId, templateId);
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 when no in-progress session")
        void shouldReturn404WhenNoInProgressSession() throws Exception {
            // Given
            when(testSessionService.findInProgressSession(clerkUserId, templateId))
                    .thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(get("/api/v1/tests/sessions/user/{clerkUserId}/in-progress", clerkUserId)
                            .param("templateId", templateId.toString()))
                    .andExpect(status().isNotFound());
        }
    }
}
