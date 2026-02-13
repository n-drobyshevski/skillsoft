package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.dto.*;
import app.skillsoft.assessmentbackend.services.TestResultService;
import app.skillsoft.assessmentbackend.services.scoring.QuestionScoreService;
import app.skillsoft.assessmentbackend.services.TestResultService.UserTestStatistics;
import app.skillsoft.assessmentbackend.services.TestResultService.TemplateTestStatistics;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for TestResultController using @WebMvcTest.
 * 
 * Tests cover:
 * - Result retrieval by ID and session
 * - User result queries
 * - Statistics endpoints
 * - Percentile calculation
 */
@WebMvcTest(TestResultController.class)
@DisplayName("TestResult Controller Tests")
class TestResultControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TestResultService testResultService;

    @MockBean
    private QuestionScoreService questionScoreService;

    private UUID resultId;
    private UUID sessionId;
    private UUID templateId;
    private String clerkUserId;
    private TestResultDto testResultDto;
    private TestResultSummaryDto testResultSummaryDto;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        resultId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
        templateId = UUID.randomUUID();
        clerkUserId = "user_test123";
        now = LocalDateTime.now();

        // CompetencyScoreDto
        CompetencyScoreDto competencyScore = new CompetencyScoreDto(
                UUID.randomUUID(),
                "Leadership",
                45.0,
                60.0,
                75.0
        );

        // TestResultDto: id, sessionId, templateId, templateName, clerkUserId, overallScore, overallPercentage,
        //                percentile, passed, competencyScores, totalTimeSeconds, questionsAnswered,
        //                questionsSkipped, totalQuestions, completedAt
        testResultDto = new TestResultDto(
                resultId,
                sessionId,
                templateId,
                "Leadership Assessment",
                clerkUserId,
                75.0,             // overallScore
                75.0,             // overallPercentage
                65,               // percentile
                true,             // passed
                List.of(competencyScore),
                1800,             // totalTimeSeconds (30 min)
                10,               // questionsAnswered
                0,                // questionsSkipped
                10,               // totalQuestions
                now               // completedAt
        );

        // TestResultSummaryDto: id, sessionId, templateId, templateName, overallPercentage, passed, completedAt
        testResultSummaryDto = new TestResultSummaryDto(
                resultId,
                sessionId,
                templateId,
                "Leadership Assessment",
                75.0,             // overallPercentage
                true,             // passed
                now               // completedAt
        );
    }

    @Nested
    @DisplayName("GET /api/v1/tests/results/{resultId} - Get Result By ID Tests")
    class GetResultByIdTests {

        @Test
        @WithMockUser
        @DisplayName("Should return result by id")
        void shouldReturnResultById() throws Exception {
            // Given
            when(testResultService.findById(resultId)).thenReturn(Optional.of(testResultDto));

            // When & Then
            mockMvc.perform(get("/api/v1/tests/results/{resultId}", resultId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(resultId.toString()))
                    .andExpect(jsonPath("$.overallPercentage").value(75.0))
                    .andExpect(jsonPath("$.passed").value(true))
                    .andExpect(jsonPath("$.competencyScores").isArray());

            verify(testResultService).findById(resultId);
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 when result not found")
        void shouldReturn404WhenNotFound() throws Exception {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(testResultService.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(get("/api/v1/tests/results/{resultId}", nonExistentId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/tests/results/session/{sessionId} - Get Result By Session Tests")
    class GetResultBySessionTests {

        @Test
        @WithMockUser
        @DisplayName("Should return result by session id")
        void shouldReturnResultBySession() throws Exception {
            // Given
            when(testResultService.findBySessionId(sessionId)).thenReturn(Optional.of(testResultDto));

            // When & Then
            mockMvc.perform(get("/api/v1/tests/results/session/{sessionId}", sessionId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionId").value(sessionId.toString()))
                    .andExpect(jsonPath("$.templateName").value("Leadership Assessment"));

            verify(testResultService).findBySessionId(sessionId);
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 when no result for session")
        void shouldReturn404WhenNoResultForSession() throws Exception {
            // Given
            UUID nonExistentSessionId = UUID.randomUUID();
            when(testResultService.findBySessionId(nonExistentSessionId)).thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(get("/api/v1/tests/results/session/{sessionId}", nonExistentSessionId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/tests/results/{resultId}/percentile - Get Percentile Tests")
    class GetPercentileTests {

        @Test
        @WithMockUser
        @DisplayName("Should return percentile for result")
        void shouldReturnPercentile() throws Exception {
            // Given
            when(testResultService.calculatePercentile(resultId)).thenReturn(75);

            // When & Then
            mockMvc.perform(get("/api/v1/tests/results/{resultId}/percentile", resultId))
                    .andExpect(status().isOk())
                    .andExpect(content().string("75"));

            verify(testResultService).calculatePercentile(resultId);
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 when result not found for percentile")
        void shouldReturn404WhenResultNotFoundForPercentile() throws Exception {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(testResultService.calculatePercentile(nonExistentId))
                    .thenThrow(new RuntimeException("Result not found"));

            // When & Then
            mockMvc.perform(get("/api/v1/tests/results/{resultId}/percentile", nonExistentId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/tests/results/user/{clerkUserId} - User Results Tests")
    class UserResultsTests {

        @Test
        @WithMockUser
        @DisplayName("Should return paginated user results")
        void shouldReturnUserResults() throws Exception {
            // Given
            Page<TestResultSummaryDto> resultPage = new PageImpl<>(
                    List.of(testResultSummaryDto),
                    Pageable.unpaged(),
                    1
            );
            when(testResultService.findByUser(eq(clerkUserId), any(Pageable.class)))
                    .thenReturn(resultPage);

            // When & Then
            mockMvc.perform(get("/api/v1/tests/results/user/{clerkUserId}", clerkUserId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].id").value(resultId.toString()));

            verify(testResultService).findByUser(eq(clerkUserId), any(Pageable.class));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return all user results")
        void shouldReturnAllUserResults() throws Exception {
            // Given
            when(testResultService.findByUserOrderByDate(clerkUserId))
                    .thenReturn(List.of(testResultSummaryDto));

            // When & Then
            mockMvc.perform(get("/api/v1/tests/results/user/{clerkUserId}/all", clerkUserId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].templateName").value("Leadership Assessment"));

            verify(testResultService).findByUserOrderByDate(clerkUserId);
        }

        @Test
        @WithMockUser
        @DisplayName("Should return user results for template")
        void shouldReturnUserResultsForTemplate() throws Exception {
            // Given
            when(testResultService.findByUserAndTemplate(clerkUserId, templateId))
                    .thenReturn(List.of(testResultSummaryDto));

            // When & Then
            mockMvc.perform(get("/api/v1/tests/results/user/{clerkUserId}/template/{templateId}",
                            clerkUserId, templateId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].templateId").value(templateId.toString()));

            verify(testResultService).findByUserAndTemplate(clerkUserId, templateId);
        }

        @Test
        @WithMockUser
        @DisplayName("Should return latest result for user on template")
        void shouldReturnLatestResult() throws Exception {
            // Given
            when(testResultService.findLatestByUserAndTemplate(clerkUserId, templateId))
                    .thenReturn(Optional.of(testResultDto));

            // When & Then
            mockMvc.perform(get("/api/v1/tests/results/user/{clerkUserId}/template/{templateId}/latest",
                            clerkUserId, templateId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(resultId.toString()));

            verify(testResultService).findLatestByUserAndTemplate(clerkUserId, templateId);
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 when no latest result")
        void shouldReturn404WhenNoLatestResult() throws Exception {
            // Given
            when(testResultService.findLatestByUserAndTemplate(clerkUserId, templateId))
                    .thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(get("/api/v1/tests/results/user/{clerkUserId}/template/{templateId}/latest",
                            clerkUserId, templateId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        @DisplayName("Should return passed results for user")
        void shouldReturnPassedResults() throws Exception {
            // Given
            when(testResultService.findPassedByUser(clerkUserId))
                    .thenReturn(List.of(testResultSummaryDto));

            // When & Then
            mockMvc.perform(get("/api/v1/tests/results/user/{clerkUserId}/passed", clerkUserId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].passed").value(true));

            verify(testResultService).findPassedByUser(clerkUserId);
        }
    }

    @Nested
    @DisplayName("Statistics Tests")
    class StatisticsTests {

        @Test
        @WithMockUser
        @DisplayName("Should return user statistics")
        void shouldReturnUserStatistics() throws Exception {
            // Given
            UserTestStatistics stats = new UserTestStatistics(
                    clerkUserId,
                    10L,           // totalTests
                    8L,            // passedTests
                    2L,            // failedTests
                    78.5,          // averageScore
                    95.0,          // bestScore
                    now            // lastTestDate
            );
            when(testResultService.getUserStatistics(clerkUserId)).thenReturn(stats);

            // When & Then
            mockMvc.perform(get("/api/v1/tests/results/user/{clerkUserId}/statistics", clerkUserId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.clerkUserId").value(clerkUserId))
                    .andExpect(jsonPath("$.totalTests").value(10))
                    .andExpect(jsonPath("$.passedTests").value(8))
                    .andExpect(jsonPath("$.averageScore").value(78.5));

            verify(testResultService).getUserStatistics(clerkUserId);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return template statistics for admin")
        void shouldReturnTemplateStatisticsForAdmin() throws Exception {
            // Given
            TemplateTestStatistics stats = new TemplateTestStatistics(
                    templateId,
                    "Leadership Assessment",
                    100L,          // totalAttempts
                    75.5,          // averageScore
                    80.0,          // passRate
                    80L,           // passedAttempts
                    20L            // failedAttempts
            );
            when(testResultService.getTemplateStatistics(templateId)).thenReturn(stats);

            // When & Then
            mockMvc.perform(get("/api/v1/tests/results/template/{templateId}/statistics", templateId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.templateId").value(templateId.toString()))
                    .andExpect(jsonPath("$.templateName").value("Leadership Assessment"))
                    .andExpect(jsonPath("$.totalAttempts").value(100))
                    .andExpect(jsonPath("$.passRate").value(80.0));

            verify(testResultService).getTemplateStatistics(templateId);
        }

        @Test
        @DisplayName("Should return 401 for template statistics without authentication")
        void shouldReturn401ForTemplateStatisticsWithoutAuth() throws Exception {
            // When & Then - no @WithMockUser annotation means no authentication
            mockMvc.perform(get("/api/v1/tests/results/template/{templateId}/statistics", templateId))
                    .andExpect(status().isUnauthorized());

            verify(testResultService, never()).getTemplateStatistics(any());
        }
    }
}
