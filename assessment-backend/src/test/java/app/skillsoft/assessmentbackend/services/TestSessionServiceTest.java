package app.skillsoft.assessmentbackend.services;

import app.skillsoft.assessmentbackend.domain.dto.*;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.repository.*;
import app.skillsoft.assessmentbackend.services.impl.TestSessionServiceImpl;
import app.skillsoft.assessmentbackend.services.scoring.ScoringStrategy;
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
 * Unit tests for TestSessionService implementation.
 * 
 * Tests cover:
 * - Session lifecycle (start, complete, abandon)
 * - Answer submission
 * - Question navigation
 * - User session queries
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TestSession Service Tests")
class TestSessionServiceTest {

    @Mock
    private TestSessionRepository sessionRepository;

    @Mock
    private TestAnswerRepository answerRepository;

    @Mock
    private TestTemplateRepository templateRepository;

    @Mock
    private TestResultRepository resultRepository;

    @Mock
    private AssessmentQuestionRepository questionRepository;

    @Mock
    private BehavioralIndicatorRepository indicatorRepository;

    @Mock
    private List<ScoringStrategy> scoringStrategies;

    @InjectMocks
    private TestSessionServiceImpl testSessionService;

    private UUID sessionId;
    private UUID templateId;
    private UUID questionId;
    private String clerkUserId;
    private TestSession mockSession;
    private TestTemplate mockTemplate;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        templateId = UUID.randomUUID();
        questionId = UUID.randomUUID();
        clerkUserId = "user_test123";

        // Set up mock template
        mockTemplate = new TestTemplate();
        mockTemplate.setId(templateId);
        mockTemplate.setName("Leadership Assessment Test");
        mockTemplate.setDescription("Test description");
        mockTemplate.setCompetencyIds(List.of(UUID.randomUUID()));
        mockTemplate.setQuestionsPerIndicator(3);
        mockTemplate.setTimeLimitMinutes(60);
        mockTemplate.setPassingScore(70.0);
        mockTemplate.setShuffleQuestions(true);
        mockTemplate.setShuffleOptions(true);
        mockTemplate.setAllowSkip(true);
        mockTemplate.setAllowBackNavigation(true);
        mockTemplate.setShowResultsImmediately(true);
        mockTemplate.setIsActive(true);
        mockTemplate.setCreatedAt(LocalDateTime.now());
        mockTemplate.setUpdatedAt(LocalDateTime.now());

        // Set up mock session
        mockSession = new TestSession();
        mockSession.setId(sessionId);
        mockSession.setTemplate(mockTemplate);
        mockSession.setClerkUserId(clerkUserId);
        mockSession.setStatus(SessionStatus.IN_PROGRESS);
        mockSession.setStartedAt(LocalDateTime.now());
        mockSession.setCurrentQuestionIndex(0);
        mockSession.setTimeRemainingSeconds(3600);
        mockSession.setQuestionOrder(List.of(questionId, UUID.randomUUID(), UUID.randomUUID()));
        mockSession.setCreatedAt(LocalDateTime.now());
        mockSession.setLastActivityAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("Find Session By ID Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Should return session when found")
        void shouldReturnSessionWhenFound() {
            // Given
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(mockSession));

            // When
            Optional<TestSessionDto> result = testSessionService.findById(sessionId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo(sessionId);
            assertThat(result.get().clerkUserId()).isEqualTo(clerkUserId);
            assertThat(result.get().status()).isEqualTo(SessionStatus.IN_PROGRESS);

            verify(sessionRepository).findById(sessionId);
        }

        @Test
        @DisplayName("Should return empty when session not found")
        void shouldReturnEmptyWhenNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(sessionRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When
            Optional<TestSessionDto> result = testSessionService.findById(nonExistentId);

            // Then
            assertThat(result).isEmpty();

            verify(sessionRepository).findById(nonExistentId);
        }
    }

    @Nested
    @DisplayName("Find User Sessions Tests")
    class FindByUserTests {

        @Test
        @DisplayName("Should return paginated user sessions")
        void shouldReturnPaginatedUserSessions() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<TestSession> sessionPage = new PageImpl<>(
                    List.of(mockSession),
                    pageable,
                    1
            );
            when(sessionRepository.findByClerkUserId(clerkUserId, pageable)).thenReturn(sessionPage);

            // When
            Page<TestSessionSummaryDto> result = testSessionService.findByUser(clerkUserId, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).id()).isEqualTo(sessionId);

            verify(sessionRepository).findByClerkUserId(clerkUserId, pageable);
        }

        @Test
        @DisplayName("Should return user sessions filtered by status")
        void shouldReturnSessionsFilteredByStatus() {
            // Given
            when(sessionRepository.findByClerkUserIdAndStatus(clerkUserId, SessionStatus.IN_PROGRESS))
                    .thenReturn(List.of(mockSession));

            // When
            List<TestSessionSummaryDto> result = testSessionService.findByUserAndStatus(clerkUserId, SessionStatus.IN_PROGRESS);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).status()).isEqualTo(SessionStatus.IN_PROGRESS);

            verify(sessionRepository).findByClerkUserIdAndStatus(clerkUserId, SessionStatus.IN_PROGRESS);
        }
    }

    @Nested
    @DisplayName("Find In-Progress Session Tests")
    class FindInProgressSessionTests {

        @Test
        @DisplayName("Should find in-progress session for user and template")
        void shouldFindInProgressSession() {
            // Given
            when(sessionRepository.findByClerkUserIdAndTemplate_IdAndStatus(
                    clerkUserId, templateId, SessionStatus.IN_PROGRESS))
                    .thenReturn(Optional.of(mockSession));

            // When
            Optional<TestSessionDto> result = testSessionService.findInProgressSession(clerkUserId, templateId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo(sessionId);
            assertThat(result.get().status()).isEqualTo(SessionStatus.IN_PROGRESS);

            verify(sessionRepository).findByClerkUserIdAndTemplate_IdAndStatus(
                    clerkUserId, templateId, SessionStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("Should return empty when no in-progress session")
        void shouldReturnEmptyWhenNoInProgressSession() {
            // Given
            when(sessionRepository.findByClerkUserIdAndTemplate_IdAndStatus(
                    clerkUserId, templateId, SessionStatus.IN_PROGRESS))
                    .thenReturn(Optional.empty());

            // When
            Optional<TestSessionDto> result = testSessionService.findInProgressSession(clerkUserId, templateId);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Navigate To Question Tests")
    class NavigateToQuestionTests {

        @Test
        @DisplayName("Should navigate to valid question index")
        void shouldNavigateToValidQuestion() {
            // Given
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(mockSession));
            when(sessionRepository.save(any(TestSession.class))).thenReturn(mockSession);

            // When
            TestSessionDto result = testSessionService.navigateToQuestion(sessionId, 1);

            // Then
            assertThat(result).isNotNull();
            verify(sessionRepository).save(argThat(session -> 
                    session.getCurrentQuestionIndex() == 1));
        }

        @Test
        @DisplayName("Should throw exception for invalid question index")
        void shouldThrowExceptionForInvalidIndex() {
            // Given
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(mockSession));

            // When & Then
            assertThatThrownBy(() -> testSessionService.navigateToQuestion(sessionId, 100))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should throw exception when session not found")
        void shouldThrowExceptionWhenSessionNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(sessionRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> testSessionService.navigateToQuestion(nonExistentId, 0))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("Update Time Remaining Tests")
    class UpdateTimeRemainingTests {

        @Test
        @DisplayName("Should update time remaining")
        void shouldUpdateTimeRemaining() {
            // Given
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(mockSession));
            when(sessionRepository.save(any(TestSession.class))).thenAnswer(invocation -> {
                TestSession saved = invocation.getArgument(0);
                return saved;
            });

            // When
            TestSessionDto result = testSessionService.updateTimeRemaining(sessionId, 1800);

            // Then
            assertThat(result).isNotNull();
            verify(sessionRepository).save(argThat(session -> 
                    session.getTimeRemainingSeconds() == 1800));
        }
    }

    @Nested
    @DisplayName("Get Session Answers Tests")
    class GetSessionAnswersTests {

        @Test
        @DisplayName("Should return session answers")
        void shouldReturnSessionAnswers() {
            // Given
            AssessmentQuestion mockQuestion = mock(AssessmentQuestion.class);
            when(mockQuestion.getId()).thenReturn(questionId);
            when(mockQuestion.getQuestionText()).thenReturn("Test question?");
            
            TestAnswer mockAnswer = new TestAnswer();
            mockAnswer.setId(UUID.randomUUID());
            mockAnswer.setSession(mockSession);
            mockAnswer.setQuestion(mockQuestion);
            mockAnswer.setSelectedOptionIds(List.of("option1"));
            mockAnswer.setAnsweredAt(LocalDateTime.now());
            mockAnswer.setTimeSpentSeconds(30);
            mockAnswer.setIsSkipped(false);

            when(answerRepository.findBySession_IdOrderByAnsweredAtAsc(sessionId)).thenReturn(List.of(mockAnswer));

            // When
            List<TestAnswerDto> result = testSessionService.getSessionAnswers(sessionId);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).questionId()).isEqualTo(questionId);

            verify(answerRepository).findBySession_IdOrderByAnsweredAtAsc(sessionId);
        }

        @Test
        @DisplayName("Should return empty list when no answers for session")
        void shouldReturnEmptyListWhenNoAnswers() {
            // Given
            UUID sessionIdWithNoAnswers = UUID.randomUUID();
            when(answerRepository.findBySession_IdOrderByAnsweredAtAsc(sessionIdWithNoAnswers))
                    .thenReturn(Collections.emptyList());

            // When
            List<TestAnswerDto> result = testSessionService.getSessionAnswers(sessionIdWithNoAnswers);

            // Then
            assertThat(result).isEmpty();

            verify(answerRepository).findBySession_IdOrderByAnsweredAtAsc(sessionIdWithNoAnswers);
        }
    }

    @Nested
    @DisplayName("Abandon Session Tests")
    class AbandonSessionTests {

        @Test
        @DisplayName("Should abandon in-progress session")
        void shouldAbandonSession() {
            // Given
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(mockSession));
            when(sessionRepository.save(any(TestSession.class))).thenAnswer(invocation -> {
                TestSession saved = invocation.getArgument(0);
                return saved;
            });

            // When
            TestSessionDto result = testSessionService.abandonSession(sessionId);

            // Then
            assertThat(result).isNotNull();
            verify(sessionRepository).save(argThat(session ->
                    session.getStatus() == SessionStatus.ABANDONED));
        }

        @Test
        @DisplayName("Should throw exception when session already completed")
        void shouldThrowExceptionWhenSessionCompleted() {
            // Given
            mockSession.setStatus(SessionStatus.COMPLETED);
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(mockSession));

            // When & Then
            assertThatThrownBy(() -> testSessionService.abandonSession(sessionId))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("Start Session Tests - Empty Question Validation")
    class StartSessionEmptyQuestionValidationTests {

        @Test
        @DisplayName("Should throw IllegalStateException when no questions available for competencies")
        void startSession_WithNoQuestions_ShouldThrowIllegalStateException() {
            // Given: A template with competencies that have no questions
            UUID competencyWithNoQuestions = UUID.randomUUID();
            mockTemplate.setCompetencyIds(List.of(competencyWithNoQuestions));
            mockTemplate.setGoal(AssessmentGoal.OVERVIEW);

            StartTestSessionRequest request = new StartTestSessionRequest(
                    templateId,
                    clerkUserId
            );

            // Mock template repository
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(mockTemplate));

            // Mock no in-progress session
            when(sessionRepository.findByClerkUserIdAndTemplate_IdAndStatus(
                    clerkUserId, templateId, SessionStatus.IN_PROGRESS))
                    .thenReturn(Optional.empty());

            // Mock Scenario A question generation - no UNIVERSAL questions
            when(questionRepository.findUniversalQuestions(eq(competencyWithNoQuestions), anyInt()))
                    .thenReturn(Collections.emptyList());

            // Mock fallback - no behavioral indicators for competency
            when(indicatorRepository.findByCompetencyId(competencyWithNoQuestions))
                    .thenReturn(Collections.emptyList());

            // When & Then: Should throw IllegalStateException
            assertThatThrownBy(() -> testSessionService.startSession(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot start test session")
                    .hasMessageContaining("No questions are available")
                    .hasMessageContaining("competencies have behavioral indicators with active questions");

            // Verify that session was never saved
            verify(sessionRepository, never()).save(any(TestSession.class));

            // Verify that we attempted to find questions
            verify(questionRepository).findUniversalQuestions(eq(competencyWithNoQuestions), anyInt());
            verify(indicatorRepository).findByCompetencyId(competencyWithNoQuestions);
        }

        @Test
        @DisplayName("Should throw IllegalStateException when indicators exist but have no active questions")
        void startSession_WithIndicatorsButNoActiveQuestions_ShouldThrowIllegalStateException() {
            // Given: A template with competency that has indicators but no active questions
            UUID competencyId = UUID.randomUUID();
            UUID indicatorId = UUID.randomUUID();
            mockTemplate.setCompetencyIds(List.of(competencyId));
            mockTemplate.setGoal(AssessmentGoal.OVERVIEW);

            StartTestSessionRequest request = new StartTestSessionRequest(
                    templateId,
                    clerkUserId
            );

            BehavioralIndicator mockIndicator = new BehavioralIndicator();
            mockIndicator.setId(indicatorId);

            // Mock template repository
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(mockTemplate));

            // Mock no in-progress session
            when(sessionRepository.findByClerkUserIdAndTemplate_IdAndStatus(
                    clerkUserId, templateId, SessionStatus.IN_PROGRESS))
                    .thenReturn(Optional.empty());

            // Mock Scenario A question generation - no UNIVERSAL questions
            when(questionRepository.findUniversalQuestions(eq(competencyId), anyInt()))
                    .thenReturn(Collections.emptyList());

            // Mock fallback - indicator exists but has no active questions
            when(indicatorRepository.findByCompetencyId(competencyId))
                    .thenReturn(List.of(mockIndicator));
            when(questionRepository.findByBehavioralIndicator_Id(indicatorId))
                    .thenReturn(Collections.emptyList());

            // When & Then: Should throw IllegalStateException
            assertThatThrownBy(() -> testSessionService.startSession(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot start test session")
                    .hasMessageContaining("No questions are available");

            // Verify that session was never saved
            verify(sessionRepository, never()).save(any(TestSession.class));
        }

        @Test
        @DisplayName("Should throw IllegalStateException for JOB_FIT goal with no questions (legacy fallback)")
        void startSession_JobFitGoalWithNoQuestions_ShouldThrowIllegalStateException() {
            // Given: A template with JOB_FIT goal that falls back to legacy logic
            UUID competencyId = UUID.randomUUID();
            mockTemplate.setCompetencyIds(List.of(competencyId));
            mockTemplate.setGoal(AssessmentGoal.JOB_FIT);

            StartTestSessionRequest request = new StartTestSessionRequest(
                    templateId,
                    clerkUserId
            );

            // Mock template repository
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(mockTemplate));

            // Mock no in-progress session
            when(sessionRepository.findByClerkUserIdAndTemplate_IdAndStatus(
                    clerkUserId, templateId, SessionStatus.IN_PROGRESS))
                    .thenReturn(Optional.empty());

            // Mock legacy fallback - no indicators for competency
            when(indicatorRepository.findByCompetencyId(competencyId))
                    .thenReturn(Collections.emptyList());

            // When & Then: Should throw IllegalStateException
            assertThatThrownBy(() -> testSessionService.startSession(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot start test session")
                    .hasMessageContaining("No questions are available");

            // Verify that session was never saved
            verify(sessionRepository, never()).save(any(TestSession.class));
        }

        @Test
        @DisplayName("Should successfully start session when questions are available")
        void startSession_WithQuestionsAvailable_ShouldSucceed() {
            // Given: A template with competency that has active questions
            UUID competencyId = UUID.randomUUID();
            UUID question1Id = UUID.randomUUID();
            UUID question2Id = UUID.randomUUID();
            UUID question3Id = UUID.randomUUID();

            mockTemplate.setCompetencyIds(List.of(competencyId));
            mockTemplate.setGoal(AssessmentGoal.OVERVIEW);
            mockTemplate.setQuestionsPerIndicator(3);

            StartTestSessionRequest request = new StartTestSessionRequest(
                    templateId,
                    clerkUserId
            );

            // Create mock questions
            AssessmentQuestion mockQuestion1 = mock(AssessmentQuestion.class);
            when(mockQuestion1.getId()).thenReturn(question1Id);
            AssessmentQuestion mockQuestion2 = mock(AssessmentQuestion.class);
            when(mockQuestion2.getId()).thenReturn(question2Id);
            AssessmentQuestion mockQuestion3 = mock(AssessmentQuestion.class);
            when(mockQuestion3.getId()).thenReturn(question3Id);

            // Mock template repository
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(mockTemplate));

            // Mock no in-progress session
            when(sessionRepository.findByClerkUserIdAndTemplate_IdAndStatus(
                    clerkUserId, templateId, SessionStatus.IN_PROGRESS))
                    .thenReturn(Optional.empty());

            // Mock Scenario A question generation - return questions
            when(questionRepository.findUniversalQuestions(eq(competencyId), eq(3)))
                    .thenReturn(List.of(mockQuestion1, mockQuestion2, mockQuestion3));

            // Mock session save
            when(sessionRepository.save(any(TestSession.class))).thenAnswer(invocation -> {
                TestSession session = invocation.getArgument(0);
                session.setId(sessionId);
                return session;
            });

            // Mock answer count for DTO mapping
            when(answerRepository.countAnsweredBySessionId(any())).thenReturn(0L);

            // When
            TestSessionDto result = testSessionService.startSession(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(sessionId);
            assertThat(result.status()).isEqualTo(SessionStatus.IN_PROGRESS);
            assertThat(result.totalQuestions()).isEqualTo(3);

            // Verify session was saved with correct question order
            verify(sessionRepository).save(argThat(session -> {
                List<UUID> questionOrder = session.getQuestionOrder();
                return questionOrder != null
                    && questionOrder.size() == 3
                    && questionOrder.contains(question1Id)
                    && questionOrder.contains(question2Id)
                    && questionOrder.contains(question3Id);
            }));
        }
    }
}
