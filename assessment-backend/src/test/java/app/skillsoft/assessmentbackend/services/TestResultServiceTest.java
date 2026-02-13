package app.skillsoft.assessmentbackend.services;

import app.skillsoft.assessmentbackend.domain.dto.*;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.domain.projections.UserStatisticsProjection;
import app.skillsoft.assessmentbackend.repository.TestResultRepository;
import app.skillsoft.assessmentbackend.services.impl.TestResultServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TestResultService implementation.
 * 
 * Tests cover:
 * - Result retrieval by ID and session
 * - User result queries
 * - Statistics calculations
 * - Percentile calculation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TestResult Service Tests")
class TestResultServiceTest {

    @Mock
    private TestResultRepository resultRepository;

    @InjectMocks
    private TestResultServiceImpl testResultService;

    private UUID resultId;
    private UUID sessionId;
    private UUID templateId;
    private String clerkUserId;
    private TestResult mockResult;
    private TestSession mockSession;
    private TestTemplate mockTemplate;

    @BeforeEach
    void setUp() {
        resultId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
        templateId = UUID.randomUUID();
        clerkUserId = "user_test123";

        // Set up mock template
        mockTemplate = new TestTemplate();
        mockTemplate.setId(templateId);
        mockTemplate.setName("Leadership Assessment Test");
        mockTemplate.setDescription("Test description");
        mockTemplate.setPassingScore(70.0);
        mockTemplate.setIsActive(true);
        mockTemplate.setCreatedAt(LocalDateTime.now());
        mockTemplate.setUpdatedAt(LocalDateTime.now());

        // Set up mock session
        mockSession = new TestSession();
        mockSession.setId(sessionId);
        mockSession.setTemplate(mockTemplate);
        mockSession.setClerkUserId(clerkUserId);
        mockSession.setStatus(SessionStatus.COMPLETED);
        mockSession.setStartedAt(LocalDateTime.now().minusMinutes(30));
        mockSession.setCompletedAt(LocalDateTime.now());
        mockSession.setCreatedAt(LocalDateTime.now().minusMinutes(30));

        // Set up mock result
        mockResult = new TestResult();
        mockResult.setId(resultId);
        mockResult.setSession(mockSession);
        mockResult.setClerkUserId(clerkUserId);
        mockResult.setOverallScore(75.0);
        mockResult.setOverallPercentage(75.0);
        mockResult.setPercentile(65);
        mockResult.setPassed(true);
        mockResult.setCompetencyScores(List.of(
                new CompetencyScoreDto(UUID.randomUUID(), "Leadership", 45.0, 60.0, 75.0)
        ));
        mockResult.setTotalTimeSeconds(1800);
        mockResult.setQuestionsAnswered(10);
        mockResult.setQuestionsSkipped(0);
        mockResult.setCompletedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("Find Result By ID Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Should return result when found")
        void shouldReturnResultWhenFound() {
            // Given - use new optimized query with JOIN FETCH
            when(resultRepository.findByIdWithSessionAndTemplate(resultId)).thenReturn(Optional.of(mockResult));

            // When
            Optional<TestResultDto> result = testResultService.findById(resultId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo(resultId);
            assertThat(result.get().overallPercentage()).isEqualTo(75.0);
            assertThat(result.get().passed()).isTrue();

            verify(resultRepository).findByIdWithSessionAndTemplate(resultId);
        }

        @Test
        @DisplayName("Should return empty when result not found")
        void shouldReturnEmptyWhenNotFound() {
            // Given - use new optimized query with JOIN FETCH
            UUID nonExistentId = UUID.randomUUID();
            when(resultRepository.findByIdWithSessionAndTemplate(nonExistentId)).thenReturn(Optional.empty());

            // When
            Optional<TestResultDto> result = testResultService.findById(nonExistentId);

            // Then
            assertThat(result).isEmpty();

            verify(resultRepository).findByIdWithSessionAndTemplate(nonExistentId);
        }
    }

    @Nested
    @DisplayName("Find Result By Session Tests")
    class FindBySessionTests {

        @Test
        @DisplayName("Should return result by session id")
        void shouldReturnResultBySessionId() {
            // Given - use new optimized query with JOIN FETCH
            when(resultRepository.findBySessionIdWithTemplate(sessionId)).thenReturn(Optional.of(mockResult));

            // When
            Optional<TestResultDto> result = testResultService.findBySessionId(sessionId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().sessionId()).isEqualTo(sessionId);

            verify(resultRepository).findBySessionIdWithTemplate(sessionId);
        }

        @Test
        @DisplayName("Should return empty when no result for session")
        void shouldReturnEmptyWhenNoResultForSession() {
            // Given - use new optimized query with JOIN FETCH
            UUID nonExistentSessionId = UUID.randomUUID();
            when(resultRepository.findBySessionIdWithTemplate(nonExistentSessionId)).thenReturn(Optional.empty());

            // When
            Optional<TestResultDto> result = testResultService.findBySessionId(nonExistentSessionId);

            // Then
            assertThat(result).isEmpty();

            verify(resultRepository).findBySessionIdWithTemplate(nonExistentSessionId);
        }
    }

    @Nested
    @DisplayName("Find User Results Tests")
    class FindByUserTests {

        @Test
        @DisplayName("Should return paginated user results")
        void shouldReturnPaginatedUserResults() {
            // Given - use new optimized query with JOIN FETCH
            Pageable pageable = PageRequest.of(0, 10);
            Page<TestResult> resultPage = new PageImpl<>(
                    List.of(mockResult),
                    pageable,
                    1
            );
            when(resultRepository.findByClerkUserIdWithSessionAndTemplate(clerkUserId, pageable)).thenReturn(resultPage);

            // When
            Page<TestResultSummaryDto> result = testResultService.findByUser(clerkUserId, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).id()).isEqualTo(resultId);

            verify(resultRepository).findByClerkUserIdWithSessionAndTemplate(clerkUserId, pageable);
        }

        @Test
        @DisplayName("Should return user results ordered by date")
        void shouldReturnUserResultsOrderedByDate() {
            // Given - use new optimized query with JOIN FETCH
            when(resultRepository.findByClerkUserIdWithSessionAndTemplate(clerkUserId))
                    .thenReturn(List.of(mockResult));

            // When
            List<TestResultSummaryDto> result = testResultService.findByUserOrderByDate(clerkUserId);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).passed()).isTrue();

            verify(resultRepository).findByClerkUserIdWithSessionAndTemplate(clerkUserId);
        }

        @Test
        @DisplayName("Should return passed results for user")
        void shouldReturnPassedResults() {
            // Given - use new optimized query with JOIN FETCH
            when(resultRepository.findPassedByClerkUserIdWithSessionAndTemplate(clerkUserId))
                    .thenReturn(List.of(mockResult));

            // When
            List<TestResultSummaryDto> result = testResultService.findPassedByUser(clerkUserId);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).passed()).isTrue();

            verify(resultRepository).findPassedByClerkUserIdWithSessionAndTemplate(clerkUserId);
        }
    }

    @Nested
    @DisplayName("Find Results By User And Template Tests")
    class FindByUserAndTemplateTests {

        @Test
        @DisplayName("Should return user results for specific template")
        void shouldReturnResultsForTemplate() {
            // Given - use new optimized query with JOIN FETCH
            when(resultRepository.findByUserAndTemplateWithSession(clerkUserId, templateId))
                    .thenReturn(List.of(mockResult));

            // When
            List<TestResultSummaryDto> result = testResultService.findByUserAndTemplate(clerkUserId, templateId);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).templateId()).isEqualTo(templateId);

            verify(resultRepository).findByUserAndTemplateWithSession(clerkUserId, templateId);
        }

        @Test
        @DisplayName("Should return latest result for user on template")
        void shouldReturnLatestResult() {
            // Given
            when(resultRepository.findLatestByUserAndTemplate(clerkUserId, templateId))
                    .thenReturn(Optional.of(mockResult));

            // When
            Optional<TestResultDto> result = testResultService.findLatestByUserAndTemplate(clerkUserId, templateId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo(resultId);

            verify(resultRepository).findLatestByUserAndTemplate(clerkUserId, templateId);
        }
    }

    @Nested
    @DisplayName("User Statistics Tests")
    class UserStatisticsTests {

        @Test
        @DisplayName("Should return user statistics")
        void shouldReturnUserStatistics() {
            // Given - use type-safe projection mock
            UserStatisticsProjection mockProjection = mock(UserStatisticsProjection.class);
            when(mockProjection.getTotalTests()).thenReturn(10L);
            when(mockProjection.getPassedTests()).thenReturn(8L);
            when(mockProjection.getAverageScore()).thenReturn(75.0);
            when(mockProjection.getBestScore()).thenReturn(85.0);

            when(resultRepository.getUserStatisticsAggregate(clerkUserId)).thenReturn(mockProjection);
            when(resultRepository.findByClerkUserIdOrderByCompletedAtDesc(clerkUserId))
                    .thenReturn(List.of(mockResult));

            // When
            TestResultService.UserTestStatistics stats = testResultService.getUserStatistics(clerkUserId);

            // Then
            assertThat(stats).isNotNull();
            assertThat(stats.clerkUserId()).isEqualTo(clerkUserId);
            assertThat(stats.totalTests()).isEqualTo(10L);
            assertThat(stats.passedTests()).isEqualTo(8L);
            assertThat(stats.failedTests()).isEqualTo(2L);

            verify(resultRepository).getUserStatisticsAggregate(clerkUserId);
        }
    }

    @Nested
    @DisplayName("Find Results By Date Range Tests")
    class FindByDateRangeTests {

        @Test
        @DisplayName("Should return results within date range")
        void shouldReturnResultsInDateRange() {
            // Given
            LocalDateTime startDate = LocalDateTime.now().minusDays(7);
            LocalDateTime endDate = LocalDateTime.now();
            when(resultRepository.findByCompletedAtBetween(startDate, endDate))
                    .thenReturn(List.of(mockResult));

            // When
            List<TestResultSummaryDto> result = testResultService.findByDateRange(startDate, endDate);

            // Then
            assertThat(result).hasSize(1);

            verify(resultRepository).findByCompletedAtBetween(startDate, endDate);
        }
    }
}
