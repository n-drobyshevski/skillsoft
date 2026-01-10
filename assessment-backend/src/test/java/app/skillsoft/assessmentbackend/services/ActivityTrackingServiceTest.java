package app.skillsoft.assessmentbackend.services;

import app.skillsoft.assessmentbackend.domain.dto.activity.ActivityFilterParams;
import app.skillsoft.assessmentbackend.domain.dto.activity.TemplateActivityStatsDto;
import app.skillsoft.assessmentbackend.domain.dto.activity.TestActivityDto;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.domain.projections.TemplateActivityStatsProjection;
import app.skillsoft.assessmentbackend.domain.projections.TemplateScoreTimeProjection;
import app.skillsoft.assessmentbackend.repository.*;
import app.skillsoft.assessmentbackend.services.impl.ActivityTrackingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
 * Unit tests for ActivityTrackingService implementation.
 *
 * Tests cover:
 * - Recent activity retrieval with N+1 prevention
 * - Template activity with pagination and filtering
 * - Template activity statistics aggregation
 * - Idempotent event recording (SESSION_STARTED, COMPLETED, ABANDONED, TIMED_OUT)
 * - User enrichment with fallback handling
 * - Time calculation edge cases
 * - Null-safe statistics calculations
 *
 * Uses bilingual test data (English/Russian) per project requirements.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ActivityTracking Service Tests")
class ActivityTrackingServiceTest {

    @Mock
    private TestSessionRepository sessionRepository;

    @Mock
    private TestResultRepository resultRepository;

    @Mock
    private TestTemplateRepository templateRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TestActivityEventRepository eventRepository;

    @InjectMocks
    private ActivityTrackingServiceImpl activityTrackingService;

    // Test data
    private UUID sessionId;
    private UUID templateId;
    private UUID userId;
    private String clerkUserId;
    private TestSession mockSession;
    private TestTemplate mockTemplate;
    private User mockUser;
    private TestResult mockResult;

    private static final List<SessionStatus> TERMINAL_STATUSES = List.of(
            SessionStatus.COMPLETED,
            SessionStatus.ABANDONED,
            SessionStatus.TIMED_OUT
    );

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        templateId = UUID.randomUUID();
        userId = UUID.randomUUID();
        clerkUserId = "user_clerk_123";

        // Set up mock template with bilingual name
        mockTemplate = new TestTemplate();
        mockTemplate.setId(templateId);
        mockTemplate.setName("Leadership Assessment / Оценка лидерства");
        mockTemplate.setDescription("Comprehensive leadership evaluation");
        mockTemplate.setGoal(AssessmentGoal.OVERVIEW);
        mockTemplate.setPassingScore(70.0);
        mockTemplate.setTimeLimitMinutes(60);
        mockTemplate.setCreatedAt(LocalDateTime.now());
        mockTemplate.setUpdatedAt(LocalDateTime.now());

        // Set up mock user with bilingual name
        mockUser = new User();
        mockUser.setId(userId);
        mockUser.setClerkId(clerkUserId);
        mockUser.setFirstName("Иван");
        mockUser.setLastName("Петров");
        mockUser.setEmail("ivan.petrov@example.com");
        mockUser.setImageUrl("https://example.com/avatar.jpg");
        mockUser.setRole(UserRole.USER);

        // Set up mock session
        mockSession = new TestSession();
        mockSession.setId(sessionId);
        mockSession.setTemplate(mockTemplate);
        mockSession.setClerkUserId(clerkUserId);
        mockSession.setStatus(SessionStatus.COMPLETED);
        mockSession.setStartedAt(LocalDateTime.now().minusMinutes(30));
        mockSession.setCompletedAt(LocalDateTime.now());
        mockSession.setCurrentQuestionIndex(10);
        mockSession.setQuestionOrder(List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));
        mockSession.setAnswers(new ArrayList<>());

        // Set up mock result
        mockResult = new TestResult();
        mockResult.setId(UUID.randomUUID());
        mockResult.setSession(mockSession);
        mockResult.setClerkUserId(clerkUserId);
        mockResult.setOverallPercentage(85.5);
        mockResult.setPassed(true);
        mockResult.setTotalTimeSeconds(1800);
        mockResult.setCompletedAt(LocalDateTime.now());
    }

    // ============================================
    // Helper Methods
    // ============================================

    private TestSession createMockSession(UUID id, String clerkId, SessionStatus status,
                                          LocalDateTime startedAt, LocalDateTime completedAt) {
        TestSession session = new TestSession();
        session.setId(id);
        session.setTemplate(mockTemplate);
        session.setClerkUserId(clerkId);
        session.setStatus(status);
        session.setStartedAt(startedAt);
        session.setCompletedAt(completedAt);
        session.setQuestionOrder(List.of(UUID.randomUUID(), UUID.randomUUID()));
        session.setAnswers(new ArrayList<>());
        return session;
    }

    private User createMockUser(String clerkId, String firstName, String lastName) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setClerkId(clerkId);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(firstName.toLowerCase() + "@example.com");
        user.setRole(UserRole.USER);
        return user;
    }

    private TemplateActivityStatsProjection createMockStatsProjection(
            Long totalSessions, Long completedCount, Long abandonedCount,
            Long timedOutCount, LocalDateTime lastActivity) {
        return new TemplateActivityStatsProjection() {
            @Override
            public Long getTotalSessions() { return totalSessions; }
            @Override
            public Long getCompletedCount() { return completedCount; }
            @Override
            public Long getAbandonedCount() { return abandonedCount; }
            @Override
            public Long getTimedOutCount() { return timedOutCount; }
            @Override
            public LocalDateTime getLastActivity() { return lastActivity; }
        };
    }

    private TemplateScoreTimeProjection createMockScoreTimeProjection(
            Double totalScore, Long totalTimeSeconds, Long resultCount) {
        return new TemplateScoreTimeProjection() {
            @Override
            public Double getTotalScore() { return totalScore; }
            @Override
            public Long getTotalTimeSeconds() { return totalTimeSeconds; }
            @Override
            public Long getResultCount() { return resultCount; }
        };
    }

    // ============================================
    // getRecentActivity Tests
    // ============================================

    @Nested
    @DisplayName("getRecentActivity Tests")
    class GetRecentActivityTests {

        @Test
        @DisplayName("Should return empty list when no recent sessions exist")
        void shouldReturnEmptyListWhenNoSessions() {
            // Given
            when(sessionRepository.findRecentCompletedSessions(TERMINAL_STATUSES, 10))
                    .thenReturn(Collections.emptyList());

            // When
            List<TestActivityDto> result = activityTrackingService.getRecentActivity(10);

            // Then
            assertThat(result).isEmpty();
            verify(sessionRepository).findRecentCompletedSessions(TERMINAL_STATUSES, 10);
            verifyNoInteractions(userRepository);
            verifyNoInteractions(resultRepository);
        }

        @Test
        @DisplayName("Should return activities with user enrichment")
        void shouldReturnActivitiesWithUserEnrichment() {
            // Given
            when(sessionRepository.findRecentCompletedSessions(TERMINAL_STATUSES, 5))
                    .thenReturn(List.of(mockSession));
            when(userRepository.findByClerkIdIn(Set.of(clerkUserId)))
                    .thenReturn(List.of(mockUser));
            when(resultRepository.findAllById(Set.of(sessionId)))
                    .thenReturn(List.of(mockResult));

            // When
            List<TestActivityDto> result = activityTrackingService.getRecentActivity(5);

            // Then
            assertThat(result).hasSize(1);
            TestActivityDto dto = result.get(0);
            assertThat(dto.sessionId()).isEqualTo(sessionId);
            assertThat(dto.userName()).isEqualTo("Иван Петров");
            assertThat(dto.userImageUrl()).isEqualTo("https://example.com/avatar.jpg");
            assertThat(dto.templateName()).contains("Leadership");
            assertThat(dto.score()).isEqualTo(85.5);
            assertThat(dto.passed()).isTrue();
        }

        @Test
        @DisplayName("Should batch load users to prevent N+1 queries")
        void shouldBatchLoadUsersToPreventNPlusOne() {
            // Given: Multiple sessions with different users
            String clerkId1 = "user_1";
            String clerkId2 = "user_2";
            String clerkId3 = "user_3";

            TestSession session1 = createMockSession(UUID.randomUUID(), clerkId1,
                    SessionStatus.COMPLETED, LocalDateTime.now().minusHours(1), LocalDateTime.now());
            TestSession session2 = createMockSession(UUID.randomUUID(), clerkId2,
                    SessionStatus.ABANDONED, LocalDateTime.now().minusHours(2), LocalDateTime.now());
            TestSession session3 = createMockSession(UUID.randomUUID(), clerkId3,
                    SessionStatus.TIMED_OUT, LocalDateTime.now().minusHours(3), LocalDateTime.now());

            User user1 = createMockUser(clerkId1, "Alex", "Smith");
            User user2 = createMockUser(clerkId2, "Мария", "Иванова");
            User user3 = createMockUser(clerkId3, "John", "Doe");

            when(sessionRepository.findRecentCompletedSessions(TERMINAL_STATUSES, 10))
                    .thenReturn(List.of(session1, session2, session3));
            when(userRepository.findByClerkIdIn(Set.of(clerkId1, clerkId2, clerkId3)))
                    .thenReturn(List.of(user1, user2, user3));
            when(resultRepository.findAllById(Set.of(session1.getId())))
                    .thenReturn(Collections.emptyList());

            // When
            List<TestActivityDto> result = activityTrackingService.getRecentActivity(10);

            // Then
            assertThat(result).hasSize(3);
            // Verify single batch call instead of N calls
            verify(userRepository, times(1)).findByClerkIdIn(anySet());
            verify(resultRepository, times(1)).findAllById(anySet());
        }

        @Test
        @DisplayName("Should fallback to Unknown User when user not found")
        void shouldFallbackToUnknownUserWhenNotFound() {
            // Given
            when(sessionRepository.findRecentCompletedSessions(TERMINAL_STATUSES, 5))
                    .thenReturn(List.of(mockSession));
            when(userRepository.findByClerkIdIn(Set.of(clerkUserId)))
                    .thenReturn(Collections.emptyList()); // User not found
            when(resultRepository.findAllById(Set.of(sessionId)))
                    .thenReturn(Collections.emptyList());

            // When
            List<TestActivityDto> result = activityTrackingService.getRecentActivity(5);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).userName()).isEqualTo("Unknown User");
            assertThat(result.get(0).userImageUrl()).isNull();
        }

        @Test
        @DisplayName("Should not fetch results for non-completed sessions")
        void shouldNotFetchResultsForNonCompletedSessions() {
            // Given: Only abandoned sessions
            TestSession abandonedSession = createMockSession(UUID.randomUUID(), clerkUserId,
                    SessionStatus.ABANDONED, LocalDateTime.now().minusHours(1), LocalDateTime.now());

            when(sessionRepository.findRecentCompletedSessions(TERMINAL_STATUSES, 5))
                    .thenReturn(List.of(abandonedSession));
            when(userRepository.findByClerkIdIn(Set.of(clerkUserId)))
                    .thenReturn(List.of(mockUser));

            // When
            List<TestActivityDto> result = activityTrackingService.getRecentActivity(5);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).score()).isNull();
            assertThat(result.get(0).passed()).isNull();
            // Verify results not fetched for non-completed sessions
            verify(resultRepository, never()).findAllById(anySet());
        }

        @Test
        @DisplayName("Should calculate time spent correctly")
        void shouldCalculateTimeSpentCorrectly() {
            // Given
            LocalDateTime startTime = LocalDateTime.of(2024, 1, 15, 10, 0, 0);
            LocalDateTime endTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0); // 30 minutes = 1800 seconds
            mockSession.setStartedAt(startTime);
            mockSession.setCompletedAt(endTime);

            when(sessionRepository.findRecentCompletedSessions(TERMINAL_STATUSES, 5))
                    .thenReturn(List.of(mockSession));
            when(userRepository.findByClerkIdIn(Set.of(clerkUserId)))
                    .thenReturn(List.of(mockUser));
            when(resultRepository.findAllById(Set.of(sessionId)))
                    .thenReturn(List.of(mockResult));

            // When
            List<TestActivityDto> result = activityTrackingService.getRecentActivity(5);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).timeSpentSeconds()).isEqualTo(1800);
        }

        @Test
        @DisplayName("Should return null time spent when startedAt is null")
        void shouldReturnNullTimeSpentWhenStartedAtIsNull() {
            // Given
            mockSession.setStartedAt(null);
            mockSession.setCompletedAt(LocalDateTime.now());

            when(sessionRepository.findRecentCompletedSessions(TERMINAL_STATUSES, 5))
                    .thenReturn(List.of(mockSession));
            when(userRepository.findByClerkIdIn(Set.of(clerkUserId)))
                    .thenReturn(List.of(mockUser));
            when(resultRepository.findAllById(Set.of(sessionId)))
                    .thenReturn(List.of(mockResult));

            // When
            List<TestActivityDto> result = activityTrackingService.getRecentActivity(5);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).timeSpentSeconds()).isNull();
        }

        @Test
        @DisplayName("Should use startedAt as occurredAt when completedAt is null")
        void shouldUseStartedAtWhenCompletedAtIsNull() {
            // Given
            LocalDateTime startTime = LocalDateTime.of(2024, 1, 15, 10, 0, 0);
            mockSession.setStartedAt(startTime);
            mockSession.setCompletedAt(null);
            mockSession.setStatus(SessionStatus.ABANDONED);

            when(sessionRepository.findRecentCompletedSessions(TERMINAL_STATUSES, 5))
                    .thenReturn(List.of(mockSession));
            when(userRepository.findByClerkIdIn(Set.of(clerkUserId)))
                    .thenReturn(List.of(mockUser));

            // When
            List<TestActivityDto> result = activityTrackingService.getRecentActivity(5);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).occurredAt()).isEqualTo(startTime);
        }
    }

    // ============================================
    // getTemplateActivity Tests
    // ============================================

    @Nested
    @DisplayName("getTemplateActivity Tests")
    class GetTemplateActivityTests {

        @Test
        @DisplayName("Should return empty page when no activity found")
        void shouldReturnEmptyPageWhenNoActivity() {
            // Given
            ActivityFilterParams params = ActivityFilterParams.forTemplate(
                    templateId, null, null, null, null, 0, 20);
            Pageable pageable = PageRequest.of(0, 20);

            when(sessionRepository.findActivityByTemplateId(templateId, TERMINAL_STATUSES, pageable))
                    .thenReturn(Page.empty());

            // When
            Page<TestActivityDto> result = activityTrackingService.getTemplateActivity(templateId, params);

            // Then
            assertThat(result).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("Should filter by status when status parameter provided")
        void shouldFilterByStatusWhenProvided() {
            // Given
            ActivityFilterParams params = ActivityFilterParams.forTemplate(
                    templateId, SessionStatus.COMPLETED, null, null, null, 0, 20);
            Pageable pageable = PageRequest.of(0, 20);

            when(sessionRepository.findActivityByTemplateIdAndStatus(
                    templateId, SessionStatus.COMPLETED, pageable))
                    .thenReturn(new PageImpl<>(List.of(mockSession)));
            when(userRepository.findByClerkIdIn(Set.of(clerkUserId)))
                    .thenReturn(List.of(mockUser));
            when(resultRepository.findAllById(Set.of(sessionId)))
                    .thenReturn(List.of(mockResult));

            // When
            Page<TestActivityDto> result = activityTrackingService.getTemplateActivity(templateId, params);

            // Then
            assertThat(result.getContent()).hasSize(1);
            verify(sessionRepository).findActivityByTemplateIdAndStatus(
                    templateId, SessionStatus.COMPLETED, pageable);
            verify(sessionRepository, never()).findActivityByTemplateId(any(), any(), any());
        }

        @Test
        @DisplayName("Should use terminal statuses filter when status not provided")
        void shouldUseTerminalStatusesWhenStatusNotProvided() {
            // Given
            ActivityFilterParams params = ActivityFilterParams.forTemplate(
                    templateId, null, null, null, null, 0, 20);
            Pageable pageable = PageRequest.of(0, 20);

            when(sessionRepository.findActivityByTemplateId(templateId, TERMINAL_STATUSES, pageable))
                    .thenReturn(new PageImpl<>(List.of(mockSession)));
            when(userRepository.findByClerkIdIn(Set.of(clerkUserId)))
                    .thenReturn(List.of(mockUser));
            when(resultRepository.findAllById(Set.of(sessionId)))
                    .thenReturn(List.of(mockResult));

            // When
            Page<TestActivityDto> result = activityTrackingService.getTemplateActivity(templateId, params);

            // Then
            assertThat(result.getContent()).hasSize(1);
            verify(sessionRepository).findActivityByTemplateId(templateId, TERMINAL_STATUSES, pageable);
        }

        @Test
        @DisplayName("Should respect pagination parameters")
        void shouldRespectPaginationParameters() {
            // Given
            ActivityFilterParams params = ActivityFilterParams.forTemplate(
                    templateId, null, null, null, null, 2, 10); // Page 2, size 10
            Pageable expectedPageable = PageRequest.of(2, 10);

            when(sessionRepository.findActivityByTemplateId(templateId, TERMINAL_STATUSES, expectedPageable))
                    .thenReturn(Page.empty());

            // When
            activityTrackingService.getTemplateActivity(templateId, params);

            // Then
            verify(sessionRepository).findActivityByTemplateId(
                    eq(templateId), eq(TERMINAL_STATUSES), eq(expectedPageable));
        }

        @Test
        @DisplayName("Should validate page size boundaries")
        void shouldValidatePageSizeBoundaries() {
            // Given: Page size exceeds maximum (100)
            ActivityFilterParams params = new ActivityFilterParams(
                    templateId, null, null, null, null, null, 0, 200);
            Pageable expectedPageable = PageRequest.of(0, 100); // Should be capped at MAX_PAGE_SIZE

            when(sessionRepository.findActivityByTemplateId(templateId, TERMINAL_STATUSES, expectedPageable))
                    .thenReturn(Page.empty());

            // When
            activityTrackingService.getTemplateActivity(templateId, params);

            // Then
            verify(sessionRepository).findActivityByTemplateId(
                    eq(templateId), eq(TERMINAL_STATUSES), eq(expectedPageable));
        }

        @Test
        @DisplayName("Should handle negative page number gracefully")
        void shouldHandleNegativePageNumber() {
            // Given
            ActivityFilterParams params = new ActivityFilterParams(
                    templateId, null, null, null, null, null, -1, 20);
            Pageable expectedPageable = PageRequest.of(0, 20); // Should be normalized to 0

            when(sessionRepository.findActivityByTemplateId(templateId, TERMINAL_STATUSES, expectedPageable))
                    .thenReturn(Page.empty());

            // When
            activityTrackingService.getTemplateActivity(templateId, params);

            // Then
            verify(sessionRepository).findActivityByTemplateId(
                    eq(templateId), eq(TERMINAL_STATUSES), eq(expectedPageable));
        }

        @Test
        @DisplayName("Should batch load users for paginated results")
        void shouldBatchLoadUsersForPaginatedResults() {
            // Given
            String clerkId1 = "user_page_1";
            String clerkId2 = "user_page_2";

            TestSession session1 = createMockSession(UUID.randomUUID(), clerkId1,
                    SessionStatus.COMPLETED, LocalDateTime.now().minusHours(1), LocalDateTime.now());
            TestSession session2 = createMockSession(UUID.randomUUID(), clerkId2,
                    SessionStatus.COMPLETED, LocalDateTime.now().minusHours(2), LocalDateTime.now());

            ActivityFilterParams params = ActivityFilterParams.forTemplate(
                    templateId, null, null, null, null, 0, 20);
            Pageable pageable = PageRequest.of(0, 20);

            when(sessionRepository.findActivityByTemplateId(templateId, TERMINAL_STATUSES, pageable))
                    .thenReturn(new PageImpl<>(List.of(session1, session2)));
            when(userRepository.findByClerkIdIn(Set.of(clerkId1, clerkId2)))
                    .thenReturn(List.of(
                            createMockUser(clerkId1, "Alice", "Johnson"),
                            createMockUser(clerkId2, "Борис", "Смирнов")
                    ));
            when(resultRepository.findAllById(anySet()))
                    .thenReturn(Collections.emptyList());

            // When
            Page<TestActivityDto> result = activityTrackingService.getTemplateActivity(templateId, params);

            // Then
            assertThat(result.getContent()).hasSize(2);
            verify(userRepository, times(1)).findByClerkIdIn(anySet());
        }
    }

    // ============================================
    // getTemplateActivityStats Tests
    // ============================================

    @Nested
    @DisplayName("getTemplateActivityStats Tests")
    class GetTemplateActivityStatsTests {

        @Test
        @DisplayName("Should return null when template not found")
        void shouldReturnNullWhenTemplateNotFound() {
            // Given
            UUID nonExistentTemplateId = UUID.randomUUID();
            when(templateRepository.findById(nonExistentTemplateId))
                    .thenReturn(Optional.empty());

            // When
            TemplateActivityStatsDto result = activityTrackingService.getTemplateActivityStats(nonExistentTemplateId);

            // Then
            assertThat(result).isNull();
            verify(sessionRepository, never()).getTemplateActivityStats(any());
        }

        @Test
        @DisplayName("Should calculate completion rate correctly")
        void shouldCalculateCompletionRateCorrectly() {
            // Given: 100 total, 80 completed = 80% completion rate
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(mockTemplate));
            when(sessionRepository.getTemplateActivityStats(templateId))
                    .thenReturn(createMockStatsProjection(100L, 80L, 10L, 10L, LocalDateTime.now()));
            when(sessionRepository.countPassedSessionsByTemplateId(templateId)).thenReturn(60L);
            when(sessionRepository.getTemplateScoreAndTimeAggregates(templateId))
                    .thenReturn(createMockScoreTimeProjection(6400.0, 144000L, 80L));

            // When
            TemplateActivityStatsDto result = activityTrackingService.getTemplateActivityStats(templateId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.completionRate()).isEqualTo(80.0);
        }

        @Test
        @DisplayName("Should calculate pass rate correctly")
        void shouldCalculatePassRateCorrectly() {
            // Given: 80 completed, 60 passed = 75% pass rate
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(mockTemplate));
            when(sessionRepository.getTemplateActivityStats(templateId))
                    .thenReturn(createMockStatsProjection(100L, 80L, 10L, 10L, LocalDateTime.now()));
            when(sessionRepository.countPassedSessionsByTemplateId(templateId)).thenReturn(60L);
            when(sessionRepository.getTemplateScoreAndTimeAggregates(templateId))
                    .thenReturn(createMockScoreTimeProjection(6400.0, 144000L, 80L));

            // When
            TemplateActivityStatsDto result = activityTrackingService.getTemplateActivityStats(templateId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.passRate()).isEqualTo(75.0);
        }

        @Test
        @DisplayName("Should calculate average score correctly")
        void shouldCalculateAverageScoreCorrectly() {
            // Given: Total score 6400, 80 completed = 80 avg score
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(mockTemplate));
            when(sessionRepository.getTemplateActivityStats(templateId))
                    .thenReturn(createMockStatsProjection(100L, 80L, 10L, 10L, LocalDateTime.now()));
            when(sessionRepository.countPassedSessionsByTemplateId(templateId)).thenReturn(60L);
            when(sessionRepository.getTemplateScoreAndTimeAggregates(templateId))
                    .thenReturn(createMockScoreTimeProjection(6400.0, 144000L, 80L));

            // When
            TemplateActivityStatsDto result = activityTrackingService.getTemplateActivityStats(templateId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.averageScore()).isEqualTo(80.0);
        }

        @Test
        @DisplayName("Should calculate average time correctly")
        void shouldCalculateAverageTimeCorrectly() {
            // Given: Total time 144000 seconds, 80 completed = 1800 avg seconds
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(mockTemplate));
            when(sessionRepository.getTemplateActivityStats(templateId))
                    .thenReturn(createMockStatsProjection(100L, 80L, 10L, 10L, LocalDateTime.now()));
            when(sessionRepository.countPassedSessionsByTemplateId(templateId)).thenReturn(60L);
            when(sessionRepository.getTemplateScoreAndTimeAggregates(templateId))
                    .thenReturn(createMockScoreTimeProjection(6400.0, 144000L, 80L));

            // When
            TemplateActivityStatsDto result = activityTrackingService.getTemplateActivityStats(templateId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.averageTimeSeconds()).isEqualTo(1800.0);
        }

        @Test
        @DisplayName("Should handle zero total sessions (division by zero)")
        void shouldHandleZeroTotalSessions() {
            // Given: No sessions at all
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(mockTemplate));
            when(sessionRepository.getTemplateActivityStats(templateId))
                    .thenReturn(createMockStatsProjection(0L, 0L, 0L, 0L, null));
            when(sessionRepository.countPassedSessionsByTemplateId(templateId)).thenReturn(0L);
            when(sessionRepository.getTemplateScoreAndTimeAggregates(templateId))
                    .thenReturn(createMockScoreTimeProjection(null, null, 0L));

            // When
            TemplateActivityStatsDto result = activityTrackingService.getTemplateActivityStats(templateId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.completionRate()).isEqualTo(0.0);
            assertThat(result.passRate()).isEqualTo(0.0);
            assertThat(result.averageScore()).isEqualTo(0.0);
            assertThat(result.averageTimeSeconds()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should handle zero completed sessions for pass rate calculation")
        void shouldHandleZeroCompletedSessionsForPassRate() {
            // Given: All sessions abandoned, none completed
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(mockTemplate));
            when(sessionRepository.getTemplateActivityStats(templateId))
                    .thenReturn(createMockStatsProjection(50L, 0L, 50L, 0L, LocalDateTime.now()));
            when(sessionRepository.countPassedSessionsByTemplateId(templateId)).thenReturn(0L);
            when(sessionRepository.getTemplateScoreAndTimeAggregates(templateId))
                    .thenReturn(createMockScoreTimeProjection(null, null, 0L));

            // When
            TemplateActivityStatsDto result = activityTrackingService.getTemplateActivityStats(templateId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.passRate()).isEqualTo(0.0);
            assertThat(result.averageScore()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should handle null projection values")
        void shouldHandleNullProjectionValues() {
            // Given: Projection returns nulls
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(mockTemplate));
            when(sessionRepository.getTemplateActivityStats(templateId))
                    .thenReturn(createMockStatsProjection(null, null, null, null, null));
            when(sessionRepository.countPassedSessionsByTemplateId(templateId)).thenReturn(0L);
            when(sessionRepository.getTemplateScoreAndTimeAggregates(templateId))
                    .thenReturn(createMockScoreTimeProjection(null, null, null));

            // When
            TemplateActivityStatsDto result = activityTrackingService.getTemplateActivityStats(templateId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.totalSessions()).isZero();
            assertThat(result.completedCount()).isZero();
            assertThat(result.abandonedCount()).isZero();
            assertThat(result.timedOutCount()).isZero();
        }

        @Test
        @DisplayName("Should include template metadata in stats")
        void shouldIncludeTemplateMetadataInStats() {
            // Given
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(mockTemplate));
            when(sessionRepository.getTemplateActivityStats(templateId))
                    .thenReturn(createMockStatsProjection(10L, 8L, 1L, 1L, LocalDateTime.now()));
            when(sessionRepository.countPassedSessionsByTemplateId(templateId)).thenReturn(6L);
            when(sessionRepository.getTemplateScoreAndTimeAggregates(templateId))
                    .thenReturn(createMockScoreTimeProjection(640.0, 14400L, 8L));

            // When
            TemplateActivityStatsDto result = activityTrackingService.getTemplateActivityStats(templateId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.templateId()).isEqualTo(templateId);
            assertThat(result.templateName()).isEqualTo("Leadership Assessment / Оценка лидерства");
            assertThat(result.goal()).isEqualTo(AssessmentGoal.OVERVIEW);
        }

        @Test
        @DisplayName("Should include last activity timestamp")
        void shouldIncludeLastActivityTimestamp() {
            // Given
            LocalDateTime lastActivityTime = LocalDateTime.of(2024, 1, 15, 14, 30, 0);
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(mockTemplate));
            when(sessionRepository.getTemplateActivityStats(templateId))
                    .thenReturn(createMockStatsProjection(10L, 8L, 1L, 1L, lastActivityTime));
            when(sessionRepository.countPassedSessionsByTemplateId(templateId)).thenReturn(6L);
            when(sessionRepository.getTemplateScoreAndTimeAggregates(templateId))
                    .thenReturn(createMockScoreTimeProjection(640.0, 14400L, 8L));

            // When
            TemplateActivityStatsDto result = activityTrackingService.getTemplateActivityStats(templateId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.lastActivity()).isEqualTo(lastActivityTime);
        }
    }

    // ============================================
    // recordSessionStarted Tests
    // ============================================

    @Nested
    @DisplayName("recordSessionStarted Tests")
    class RecordSessionStartedTests {

        @Test
        @DisplayName("Should record SESSION_STARTED event for new session")
        void shouldRecordSessionStartedEvent() {
            // Given
            when(eventRepository.existsBySessionIdAndEventType(
                    sessionId, ActivityEventType.SESSION_STARTED))
                    .thenReturn(false);
            when(eventRepository.save(any(TestActivityEvent.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            activityTrackingService.recordSessionStarted(mockSession);

            // Then
            ArgumentCaptor<TestActivityEvent> eventCaptor = ArgumentCaptor.forClass(TestActivityEvent.class);
            verify(eventRepository).save(eventCaptor.capture());

            TestActivityEvent savedEvent = eventCaptor.getValue();
            assertThat(savedEvent.getSessionId()).isEqualTo(sessionId);
            assertThat(savedEvent.getEventType()).isEqualTo(ActivityEventType.SESSION_STARTED);
            assertThat(savedEvent.getClerkUserId()).isEqualTo(clerkUserId);
            assertThat(savedEvent.getTemplateId()).isEqualTo(templateId);
        }

        @Test
        @DisplayName("Should be idempotent - skip if event already exists")
        void shouldBeIdempotentForSessionStarted() {
            // Given: Event already exists
            when(eventRepository.existsBySessionIdAndEventType(
                    sessionId, ActivityEventType.SESSION_STARTED))
                    .thenReturn(true);

            // When
            activityTrackingService.recordSessionStarted(mockSession);

            // Then
            verify(eventRepository, never()).save(any(TestActivityEvent.class));
        }

        @Test
        @DisplayName("Should include template name in metadata")
        void shouldIncludeTemplateNameInMetadata() {
            // Given
            when(eventRepository.existsBySessionIdAndEventType(
                    sessionId, ActivityEventType.SESSION_STARTED))
                    .thenReturn(false);
            when(eventRepository.save(any(TestActivityEvent.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            activityTrackingService.recordSessionStarted(mockSession);

            // Then
            ArgumentCaptor<TestActivityEvent> eventCaptor = ArgumentCaptor.forClass(TestActivityEvent.class);
            verify(eventRepository).save(eventCaptor.capture());

            TestActivityEvent savedEvent = eventCaptor.getValue();
            assertThat(savedEvent.getMetadataValue("templateName"))
                    .isEqualTo("Leadership Assessment / Оценка лидерства");
        }
    }

    // ============================================
    // recordSessionCompleted Tests
    // ============================================

    @Nested
    @DisplayName("recordSessionCompleted Tests")
    class RecordSessionCompletedTests {

        @Test
        @DisplayName("Should record SESSION_COMPLETED event with score and passed status")
        void shouldRecordSessionCompletedWithScore() {
            // Given
            when(eventRepository.existsBySessionIdAndEventType(
                    sessionId, ActivityEventType.SESSION_COMPLETED))
                    .thenReturn(false);
            when(eventRepository.save(any(TestActivityEvent.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            activityTrackingService.recordSessionCompleted(mockSession, 85.5, true);

            // Then
            ArgumentCaptor<TestActivityEvent> eventCaptor = ArgumentCaptor.forClass(TestActivityEvent.class);
            verify(eventRepository).save(eventCaptor.capture());

            TestActivityEvent savedEvent = eventCaptor.getValue();
            assertThat(savedEvent.getEventType()).isEqualTo(ActivityEventType.SESSION_COMPLETED);
            assertThat(savedEvent.getMetadataValue("score")).isEqualTo(85.5);
            assertThat(savedEvent.getMetadataValue("passed")).isEqualTo(true);
        }

        @Test
        @DisplayName("Should be idempotent - skip if event already exists")
        void shouldBeIdempotentForSessionCompleted() {
            // Given: Event already exists
            when(eventRepository.existsBySessionIdAndEventType(
                    sessionId, ActivityEventType.SESSION_COMPLETED))
                    .thenReturn(true);

            // When
            activityTrackingService.recordSessionCompleted(mockSession, 85.5, true);

            // Then
            verify(eventRepository, never()).save(any(TestActivityEvent.class));
        }

        @Test
        @DisplayName("Should handle null score gracefully")
        void shouldHandleNullScore() {
            // Given
            when(eventRepository.existsBySessionIdAndEventType(
                    sessionId, ActivityEventType.SESSION_COMPLETED))
                    .thenReturn(false);
            when(eventRepository.save(any(TestActivityEvent.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            activityTrackingService.recordSessionCompleted(mockSession, null, null);

            // Then
            ArgumentCaptor<TestActivityEvent> eventCaptor = ArgumentCaptor.forClass(TestActivityEvent.class);
            verify(eventRepository).save(eventCaptor.capture());

            TestActivityEvent savedEvent = eventCaptor.getValue();
            assertThat(savedEvent.getMetadataValue("score")).isNull();
            assertThat(savedEvent.getMetadataValue("passed")).isNull();
        }

        @Test
        @DisplayName("Should include time spent in metadata when both timestamps available")
        void shouldIncludeTimeSpentInMetadata() {
            // Given
            LocalDateTime startTime = LocalDateTime.of(2024, 1, 15, 10, 0, 0);
            LocalDateTime endTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
            mockSession.setStartedAt(startTime);
            mockSession.setCompletedAt(endTime);

            when(eventRepository.existsBySessionIdAndEventType(
                    sessionId, ActivityEventType.SESSION_COMPLETED))
                    .thenReturn(false);
            when(eventRepository.save(any(TestActivityEvent.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            activityTrackingService.recordSessionCompleted(mockSession, 85.5, true);

            // Then
            ArgumentCaptor<TestActivityEvent> eventCaptor = ArgumentCaptor.forClass(TestActivityEvent.class);
            verify(eventRepository).save(eventCaptor.capture());

            TestActivityEvent savedEvent = eventCaptor.getValue();
            assertThat(savedEvent.getMetadataValue("timeSpentSeconds")).isEqualTo(1800L);
        }
    }

    // ============================================
    // recordSessionAbandoned Tests
    // ============================================

    @Nested
    @DisplayName("recordSessionAbandoned Tests")
    class RecordSessionAbandonedTests {

        @Test
        @DisplayName("Should record SESSION_ABANDONED event with progress metadata")
        void shouldRecordSessionAbandonedWithProgress() {
            // Given
            TestAnswer answer1 = new TestAnswer();
            TestAnswer answer2 = new TestAnswer();
            mockSession.setAnswers(List.of(answer1, answer2));
            mockSession.setQuestionOrder(List.of(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    UUID.randomUUID(), UUID.randomUUID()
            ));

            when(eventRepository.existsBySessionIdAndEventType(
                    sessionId, ActivityEventType.SESSION_ABANDONED))
                    .thenReturn(false);
            when(eventRepository.save(any(TestActivityEvent.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            activityTrackingService.recordSessionAbandoned(mockSession);

            // Then
            ArgumentCaptor<TestActivityEvent> eventCaptor = ArgumentCaptor.forClass(TestActivityEvent.class);
            verify(eventRepository).save(eventCaptor.capture());

            TestActivityEvent savedEvent = eventCaptor.getValue();
            assertThat(savedEvent.getEventType()).isEqualTo(ActivityEventType.SESSION_ABANDONED);
            assertThat(savedEvent.getMetadataValue("questionsAnswered")).isEqualTo(2);
            assertThat(savedEvent.getMetadataValue("totalQuestions")).isEqualTo(5);
        }

        @Test
        @DisplayName("Should be idempotent - skip if event already exists")
        void shouldBeIdempotentForSessionAbandoned() {
            // Given: Event already exists
            when(eventRepository.existsBySessionIdAndEventType(
                    sessionId, ActivityEventType.SESSION_ABANDONED))
                    .thenReturn(true);

            // When
            activityTrackingService.recordSessionAbandoned(mockSession);

            // Then
            verify(eventRepository, never()).save(any(TestActivityEvent.class));
        }

        @Test
        @DisplayName("Should handle empty answers list")
        void shouldHandleEmptyAnswersList() {
            // Given
            mockSession.setAnswers(Collections.emptyList());
            mockSession.setQuestionOrder(List.of(UUID.randomUUID(), UUID.randomUUID()));

            when(eventRepository.existsBySessionIdAndEventType(
                    sessionId, ActivityEventType.SESSION_ABANDONED))
                    .thenReturn(false);
            when(eventRepository.save(any(TestActivityEvent.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            activityTrackingService.recordSessionAbandoned(mockSession);

            // Then
            ArgumentCaptor<TestActivityEvent> eventCaptor = ArgumentCaptor.forClass(TestActivityEvent.class);
            verify(eventRepository).save(eventCaptor.capture());

            TestActivityEvent savedEvent = eventCaptor.getValue();
            assertThat(savedEvent.getMetadataValue("questionsAnswered")).isEqualTo(0);
        }
    }

    // ============================================
    // recordSessionTimedOut Tests
    // ============================================

    @Nested
    @DisplayName("recordSessionTimedOut Tests")
    class RecordSessionTimedOutTests {

        @Test
        @DisplayName("Should record SESSION_TIMED_OUT event with progress metadata")
        void shouldRecordSessionTimedOutWithProgress() {
            // Given
            TestAnswer answer = new TestAnswer();
            mockSession.setAnswers(List.of(answer));
            mockSession.setQuestionOrder(List.of(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()
            ));

            when(eventRepository.existsBySessionIdAndEventType(
                    sessionId, ActivityEventType.SESSION_TIMED_OUT))
                    .thenReturn(false);
            when(eventRepository.save(any(TestActivityEvent.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            activityTrackingService.recordSessionTimedOut(mockSession);

            // Then
            ArgumentCaptor<TestActivityEvent> eventCaptor = ArgumentCaptor.forClass(TestActivityEvent.class);
            verify(eventRepository).save(eventCaptor.capture());

            TestActivityEvent savedEvent = eventCaptor.getValue();
            assertThat(savedEvent.getEventType()).isEqualTo(ActivityEventType.SESSION_TIMED_OUT);
            assertThat(savedEvent.getMetadataValue("questionsAnswered")).isEqualTo(1);
            assertThat(savedEvent.getMetadataValue("totalQuestions")).isEqualTo(3);
        }

        @Test
        @DisplayName("Should be idempotent - skip if event already exists")
        void shouldBeIdempotentForSessionTimedOut() {
            // Given: Event already exists
            when(eventRepository.existsBySessionIdAndEventType(
                    sessionId, ActivityEventType.SESSION_TIMED_OUT))
                    .thenReturn(true);

            // When
            activityTrackingService.recordSessionTimedOut(mockSession);

            // Then
            verify(eventRepository, never()).save(any(TestActivityEvent.class));
        }

        @Test
        @DisplayName("Should include template name in metadata")
        void shouldIncludeTemplateNameInTimedOutMetadata() {
            // Given
            mockSession.setAnswers(Collections.emptyList());
            mockSession.setQuestionOrder(Collections.emptyList());

            when(eventRepository.existsBySessionIdAndEventType(
                    sessionId, ActivityEventType.SESSION_TIMED_OUT))
                    .thenReturn(false);
            when(eventRepository.save(any(TestActivityEvent.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            activityTrackingService.recordSessionTimedOut(mockSession);

            // Then
            ArgumentCaptor<TestActivityEvent> eventCaptor = ArgumentCaptor.forClass(TestActivityEvent.class);
            verify(eventRepository).save(eventCaptor.capture());

            TestActivityEvent savedEvent = eventCaptor.getValue();
            assertThat(savedEvent.getMetadataValue("templateName"))
                    .isEqualTo("Leadership Assessment / Оценка лидерства");
        }
    }

    // ============================================
    // Edge Cases and Integration Scenarios
    // ============================================

    @Nested
    @DisplayName("Edge Cases and Integration Scenarios")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle mixed terminal statuses in recent activity")
        void shouldHandleMixedTerminalStatusesInRecentActivity() {
            // Given
            TestSession completedSession = createMockSession(UUID.randomUUID(), "user_1",
                    SessionStatus.COMPLETED, LocalDateTime.now().minusHours(1), LocalDateTime.now());
            TestSession abandonedSession = createMockSession(UUID.randomUUID(), "user_2",
                    SessionStatus.ABANDONED, LocalDateTime.now().minusHours(2), LocalDateTime.now());
            TestSession timedOutSession = createMockSession(UUID.randomUUID(), "user_3",
                    SessionStatus.TIMED_OUT, LocalDateTime.now().minusHours(3), LocalDateTime.now());

            when(sessionRepository.findRecentCompletedSessions(TERMINAL_STATUSES, 10))
                    .thenReturn(List.of(completedSession, abandonedSession, timedOutSession));
            when(userRepository.findByClerkIdIn(anySet()))
                    .thenReturn(List.of(
                            createMockUser("user_1", "Алексей", "Козлов"),
                            createMockUser("user_2", "Emma", "Wilson"),
                            createMockUser("user_3", "Дмитрий", "Соколов")
                    ));
            when(resultRepository.findAllById(Set.of(completedSession.getId())))
                    .thenReturn(Collections.emptyList());

            // When
            List<TestActivityDto> result = activityTrackingService.getRecentActivity(10);

            // Then
            assertThat(result).hasSize(3);
            assertThat(result).extracting(TestActivityDto::eventType)
                    .containsExactly(
                            SessionStatus.COMPLETED,
                            SessionStatus.ABANDONED,
                            SessionStatus.TIMED_OUT
                    );
        }

        @Test
        @DisplayName("Should handle cyrillic characters in user names correctly")
        void shouldHandleCyrillicCharactersInUserNames() {
            // Given
            User russianUser = createMockUser(clerkUserId, "Владимир", "Путинский");

            when(sessionRepository.findRecentCompletedSessions(TERMINAL_STATUSES, 5))
                    .thenReturn(List.of(mockSession));
            when(userRepository.findByClerkIdIn(Set.of(clerkUserId)))
                    .thenReturn(List.of(russianUser));
            when(resultRepository.findAllById(Set.of(sessionId)))
                    .thenReturn(List.of(mockResult));

            // When
            List<TestActivityDto> result = activityTrackingService.getRecentActivity(5);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).userName()).isEqualTo("Владимир Путинский");
        }

        @Test
        @DisplayName("Should handle large limit values in getRecentActivity")
        void shouldHandleLargeLimitValues() {
            // Given
            when(sessionRepository.findRecentCompletedSessions(TERMINAL_STATUSES, 1000))
                    .thenReturn(Collections.emptyList());

            // When
            List<TestActivityDto> result = activityTrackingService.getRecentActivity(1000);

            // Then
            assertThat(result).isEmpty();
            verify(sessionRepository).findRecentCompletedSessions(TERMINAL_STATUSES, 1000);
        }

        @Test
        @DisplayName("Should handle statistics rounding correctly")
        void shouldHandleStatisticsRoundingCorrectly() {
            // Given: Values that require rounding
            // 33 completed out of 100 = 33.0%
            // 22 passed out of 33 = 66.666...% should round to 66.7%
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(mockTemplate));
            when(sessionRepository.getTemplateActivityStats(templateId))
                    .thenReturn(createMockStatsProjection(100L, 33L, 33L, 34L, LocalDateTime.now()));
            when(sessionRepository.countPassedSessionsByTemplateId(templateId)).thenReturn(22L);
            when(sessionRepository.getTemplateScoreAndTimeAggregates(templateId))
                    .thenReturn(createMockScoreTimeProjection(2475.5, 59400L, 33L));

            // When
            TemplateActivityStatsDto result = activityTrackingService.getTemplateActivityStats(templateId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.completionRate()).isEqualTo(33.0);
            assertThat(result.passRate()).isEqualTo(66.7); // Rounded to 1 decimal
            assertThat(result.averageScore()).isEqualTo(75.0); // 2475.5/33 = 75.015... rounded
        }

        @Test
        @DisplayName("Should handle concurrent idempotent event recording")
        void shouldHandleConcurrentIdempotentRecording() {
            // Given: First call returns false, second call returns true (simulating concurrent recording)
            when(eventRepository.existsBySessionIdAndEventType(
                    sessionId, ActivityEventType.SESSION_STARTED))
                    .thenReturn(false)
                    .thenReturn(true);
            when(eventRepository.save(any(TestActivityEvent.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When: First call should save
            activityTrackingService.recordSessionStarted(mockSession);

            // Then: Second call should be skipped
            activityTrackingService.recordSessionStarted(mockSession);

            // Verify only one save occurred (first call)
            verify(eventRepository, times(1)).save(any(TestActivityEvent.class));
        }

        @Test
        @DisplayName("Should handle user with only first name")
        void shouldHandleUserWithOnlyFirstName() {
            // Given
            User userWithFirstNameOnly = new User();
            userWithFirstNameOnly.setId(userId);
            userWithFirstNameOnly.setClerkId(clerkUserId);
            userWithFirstNameOnly.setFirstName("Alice");
            userWithFirstNameOnly.setLastName(null);
            userWithFirstNameOnly.setRole(UserRole.USER);

            when(sessionRepository.findRecentCompletedSessions(TERMINAL_STATUSES, 5))
                    .thenReturn(List.of(mockSession));
            when(userRepository.findByClerkIdIn(Set.of(clerkUserId)))
                    .thenReturn(List.of(userWithFirstNameOnly));
            when(resultRepository.findAllById(Set.of(sessionId)))
                    .thenReturn(List.of(mockResult));

            // When
            List<TestActivityDto> result = activityTrackingService.getRecentActivity(5);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).userName()).isEqualTo("Alice");
        }

        @Test
        @DisplayName("Should handle user with only username when names are null")
        void shouldHandleUserWithOnlyUsername() {
            // Given
            User userWithUsernameOnly = new User();
            userWithUsernameOnly.setId(userId);
            userWithUsernameOnly.setClerkId(clerkUserId);
            userWithUsernameOnly.setFirstName(null);
            userWithUsernameOnly.setLastName(null);
            userWithUsernameOnly.setUsername("cooluser123");
            userWithUsernameOnly.setRole(UserRole.USER);

            when(sessionRepository.findRecentCompletedSessions(TERMINAL_STATUSES, 5))
                    .thenReturn(List.of(mockSession));
            when(userRepository.findByClerkIdIn(Set.of(clerkUserId)))
                    .thenReturn(List.of(userWithUsernameOnly));
            when(resultRepository.findAllById(Set.of(sessionId)))
                    .thenReturn(List.of(mockResult));

            // When
            List<TestActivityDto> result = activityTrackingService.getRecentActivity(5);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).userName()).isEqualTo("cooluser123");
        }

        @Test
        @DisplayName("Should correctly map result to session when result has session reference")
        void shouldCorrectlyMapResultToSession() {
            // Given: Multiple sessions with results
            UUID sessionId1 = UUID.randomUUID();
            UUID sessionId2 = UUID.randomUUID();

            TestSession session1 = createMockSession(sessionId1, "user_1",
                    SessionStatus.COMPLETED, LocalDateTime.now().minusHours(1), LocalDateTime.now());
            TestSession session2 = createMockSession(sessionId2, "user_2",
                    SessionStatus.COMPLETED, LocalDateTime.now().minusHours(2), LocalDateTime.now());

            TestResult result1 = new TestResult();
            result1.setId(UUID.randomUUID());
            result1.setSession(session1);
            result1.setOverallPercentage(90.0);
            result1.setPassed(true);

            TestResult result2 = new TestResult();
            result2.setId(UUID.randomUUID());
            result2.setSession(session2);
            result2.setOverallPercentage(60.0);
            result2.setPassed(false);

            when(sessionRepository.findRecentCompletedSessions(TERMINAL_STATUSES, 10))
                    .thenReturn(List.of(session1, session2));
            when(userRepository.findByClerkIdIn(anySet()))
                    .thenReturn(List.of(
                            createMockUser("user_1", "User", "One"),
                            createMockUser("user_2", "User", "Two")
                    ));
            when(resultRepository.findAllById(Set.of(sessionId1, sessionId2)))
                    .thenReturn(List.of(result1, result2));

            // When
            List<TestActivityDto> result = activityTrackingService.getRecentActivity(10);

            // Then
            assertThat(result).hasSize(2);

            TestActivityDto dto1 = result.stream()
                    .filter(d -> d.sessionId().equals(sessionId1))
                    .findFirst().orElseThrow();
            assertThat(dto1.score()).isEqualTo(90.0);
            assertThat(dto1.passed()).isTrue();

            TestActivityDto dto2 = result.stream()
                    .filter(d -> d.sessionId().equals(sessionId2))
                    .findFirst().orElseThrow();
            assertThat(dto2.score()).isEqualTo(60.0);
            assertThat(dto2.passed()).isFalse();
        }
    }
}
