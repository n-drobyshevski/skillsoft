package app.skillsoft.assessmentbackend.services;

import app.skillsoft.assessmentbackend.domain.dto.*;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.OverviewBlueprint;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.JobFitBlueprint;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.TestBlueprintDto;
import app.skillsoft.assessmentbackend.domain.dto.simulation.HealthStatus;
import app.skillsoft.assessmentbackend.domain.dto.simulation.InventoryHeatmapDto;
import app.skillsoft.assessmentbackend.domain.dto.simulation.InventoryHeatmapDto.HeatmapSummary;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.exception.TestNotReadyException;
import app.skillsoft.assessmentbackend.repository.*;
import app.skillsoft.assessmentbackend.services.assembly.AssemblyProgressTracker;
import app.skillsoft.assessmentbackend.services.assembly.TestAssembler;
import app.skillsoft.assessmentbackend.services.assembly.TestAssemblerFactory;
import app.skillsoft.assessmentbackend.services.impl.TestSessionServiceImpl;
import app.skillsoft.assessmentbackend.services.psychometrics.PsychometricAuditJob;
import app.skillsoft.assessmentbackend.services.scoring.ScoringStrategy;
import app.skillsoft.assessmentbackend.services.validation.InventoryHeatmapService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.Map;

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
    private CompetencyRepository competencyRepository;

    @Mock
    private InventoryHeatmapService inventoryHeatmapService;

    @Mock
    private List<ScoringStrategy> scoringStrategies;

    @Mock
    private PsychometricAuditJob psychometricAuditJob;

    @Mock
    private TestAssemblerFactory assemblerFactory;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AssemblyProgressTracker assemblyProgressTracker;

    private TestSessionServiceImpl testSessionService;

    private UUID sessionId;
    private UUID templateId;
    private UUID questionId;
    private String clerkUserId;
    private TestSession mockSession;
    private TestTemplate mockTemplate;

    @BeforeEach
    void setUp() {
        // Initialize service with all mocks
        testSessionService = new TestSessionServiceImpl(
                sessionRepository,
                templateRepository,
                answerRepository,
                resultRepository,
                questionRepository,
                indicatorRepository,
                competencyRepository,
                inventoryHeatmapService,
                scoringStrategies,
                psychometricAuditJob,
                assemblerFactory,
                eventPublisher,
                assemblyProgressTracker
        );

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

    /**
     * Helper method to create a mock InventoryHeatmapDto with specified health status for competencies.
     */
    private InventoryHeatmapDto createMockHeatmap(Map<UUID, HealthStatus> competencyHealth) {
        HeatmapSummary summary = new HeatmapSummary(
                competencyHealth.size(),
                (int) competencyHealth.values().stream().filter(h -> h == HealthStatus.CRITICAL).count(),
                (int) competencyHealth.values().stream().filter(h -> h == HealthStatus.MODERATE).count(),
                (int) competencyHealth.values().stream().filter(h -> h == HealthStatus.HEALTHY).count(),
                10 // totalQuestions
        );
        return new InventoryHeatmapDto(competencyHealth, new java.util.HashMap<>(), summary);
    }

    @Nested
    @DisplayName("Find Session By ID Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Should return session when found")
        void shouldReturnSessionWhenFound() {
            // Given - use new optimized query method
            when(sessionRepository.findByIdWithTemplate(sessionId)).thenReturn(Optional.of(mockSession));

            // When
            Optional<TestSessionDto> result = testSessionService.findById(sessionId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo(sessionId);
            assertThat(result.get().clerkUserId()).isEqualTo(clerkUserId);
            assertThat(result.get().status()).isEqualTo(SessionStatus.IN_PROGRESS);

            verify(sessionRepository).findByIdWithTemplate(sessionId);
        }

        @Test
        @DisplayName("Should return empty when session not found")
        void shouldReturnEmptyWhenNotFound() {
            // Given - use new optimized query method
            UUID nonExistentId = UUID.randomUUID();
            when(sessionRepository.findByIdWithTemplate(nonExistentId)).thenReturn(Optional.empty());

            // When
            Optional<TestSessionDto> result = testSessionService.findById(nonExistentId);

            // Then
            assertThat(result).isEmpty();

            verify(sessionRepository).findByIdWithTemplate(nonExistentId);
        }
    }

    @Nested
    @DisplayName("Find User Sessions Tests")
    class FindByUserTests {

        @Test
        @DisplayName("Should return paginated user sessions")
        void shouldReturnPaginatedUserSessions() {
            // Given - use new optimized query methods
            Pageable pageable = PageRequest.of(0, 10);
            Page<TestSession> sessionPage = new PageImpl<>(
                    List.of(mockSession),
                    pageable,
                    1
            );
            when(sessionRepository.findByClerkUserIdWithTemplate(clerkUserId, pageable)).thenReturn(sessionPage);
            // Mock batch answer count query (N+1 prevention)
            List<Object[]> countResults = new ArrayList<>();
            countResults.add(new Object[]{sessionId, 0L});
            when(answerRepository.countAnsweredBySessionIds(List.of(sessionId))).thenReturn(countResults);

            // When
            Page<TestSessionSummaryDto> result = testSessionService.findByUser(clerkUserId, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).id()).isEqualTo(sessionId);

            verify(sessionRepository).findByClerkUserIdWithTemplate(clerkUserId, pageable);
        }

        @Test
        @DisplayName("Should return user sessions filtered by status")
        void shouldReturnSessionsFilteredByStatus() {
            // Given - use new optimized query methods
            when(sessionRepository.findByClerkUserIdAndStatusWithTemplate(clerkUserId, SessionStatus.IN_PROGRESS))
                    .thenReturn(List.of(mockSession));
            // Mock batch answer count query (N+1 prevention)
            List<Object[]> countResults = new ArrayList<>();
            countResults.add(new Object[]{sessionId, 0L});
            when(answerRepository.countAnsweredBySessionIds(List.of(sessionId))).thenReturn(countResults);

            // When
            List<TestSessionSummaryDto> result = testSessionService.findByUserAndStatus(clerkUserId, SessionStatus.IN_PROGRESS);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).status()).isEqualTo(SessionStatus.IN_PROGRESS);

            verify(sessionRepository).findByClerkUserIdAndStatusWithTemplate(clerkUserId, SessionStatus.IN_PROGRESS);
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
        @DisplayName("Should throw IllegalStateException when template has no typed blueprint")
        void startSession_WithNoBlueprint_ShouldThrowIllegalStateException() {
            // Given: A template without a typed blueprint
            UUID competencyId = UUID.randomUUID();
            mockTemplate.setCompetencyIds(List.of(competencyId));
            mockTemplate.setGoal(AssessmentGoal.OVERVIEW);
            // Blueprint is null by default

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

            // When & Then: Should throw IllegalStateException
            assertThatThrownBy(() -> testSessionService.startSession(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("typed blueprint");

            // Verify that session was never saved
            verify(sessionRepository, never()).save(any(TestSession.class));
        }

        @Test
        @DisplayName("Should throw TestNotReadyException when assembler returns empty question list")
        void startSession_WithNoQuestions_ShouldThrowTestNotReadyException() {
            // Given: A template with competencies but assembler returns no questions
            UUID competencyId = UUID.randomUUID();
            mockTemplate.setCompetencyIds(List.of(competencyId));
            mockTemplate.setGoal(AssessmentGoal.OVERVIEW);

            // Create a mock blueprint
            OverviewBlueprint mockBlueprint = mock(OverviewBlueprint.class);
            when(mockBlueprint.getStrategy()).thenReturn(AssessmentGoal.OVERVIEW);
            when(mockBlueprint.deepCopy()).thenReturn(mockBlueprint);

            // Set blueprint on template
            mockTemplate.setTypedBlueprint(mockBlueprint);

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

            // Mock assembler factory to return a mock assembler that returns empty list
            TestAssembler mockAssembler = mock(TestAssembler.class);
            when(mockAssembler.assemble(any())).thenReturn(Collections.emptyList());
            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);

            // Mock heatmap service to return CRITICAL status for readiness check
            when(inventoryHeatmapService.generateHeatmapFor(anyList()))
                    .thenReturn(createMockHeatmap(Map.of(competencyId, HealthStatus.CRITICAL)));
            when(inventoryHeatmapService.checkSufficiency(anyList(), anyInt()))
                    .thenReturn(Map.of(competencyId, 3)); // shortage of 3

            // Mock indicator repository for readiness check
            when(indicatorRepository.findByCompetencyId(competencyId))
                    .thenReturn(Collections.emptyList());

            // Mock competency repository for readiness check
            Competency mockCompetency = new Competency();
            mockCompetency.setId(competencyId);
            mockCompetency.setName("Test Competency");
            when(competencyRepository.findById(competencyId)).thenReturn(Optional.of(mockCompetency));

            // When & Then: Should throw TestNotReadyException
            assertThatThrownBy(() -> testSessionService.startSession(request))
                    .isInstanceOf(TestNotReadyException.class)
                    .hasMessageContaining("Cannot start test session");

            // Verify that session was never saved
            verify(sessionRepository, never()).save(any(TestSession.class));

            // Verify that assembler was called
            verify(assemblerFactory).getAssembler(any(TestBlueprintDto.class));
            verify(mockAssembler).assemble(any());
        }

        @Test
        @DisplayName("Should throw TestNotReadyException for JOB_FIT goal with no questions")
        void startSession_JobFitGoalWithNoQuestions_ShouldThrowTestNotReadyException() {
            // Given: A template with JOB_FIT goal
            UUID competencyId = UUID.randomUUID();
            mockTemplate.setCompetencyIds(List.of(competencyId));
            mockTemplate.setGoal(AssessmentGoal.JOB_FIT);

            // Create a mock JobFit blueprint
            JobFitBlueprint mockBlueprint = mock(JobFitBlueprint.class);
            when(mockBlueprint.getStrategy()).thenReturn(AssessmentGoal.JOB_FIT);
            when(mockBlueprint.deepCopy()).thenReturn(mockBlueprint);

            // Set blueprint on template
            mockTemplate.setTypedBlueprint(mockBlueprint);

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

            // Mock assembler factory to return a mock assembler that returns empty list
            TestAssembler mockAssembler = mock(TestAssembler.class);
            when(mockAssembler.assemble(any())).thenReturn(Collections.emptyList());
            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);

            // Mock heatmap service to return CRITICAL status
            when(inventoryHeatmapService.generateHeatmapFor(anyList()))
                    .thenReturn(createMockHeatmap(Map.of(competencyId, HealthStatus.CRITICAL)));
            when(inventoryHeatmapService.checkSufficiency(anyList(), anyInt()))
                    .thenReturn(Map.of(competencyId, 3));

            // Mock indicator and competency repositories for readiness check
            when(indicatorRepository.findByCompetencyId(competencyId))
                    .thenReturn(Collections.emptyList());
            Competency mockCompetency = new Competency();
            mockCompetency.setId(competencyId);
            mockCompetency.setName("Test Competency");
            when(competencyRepository.findById(competencyId)).thenReturn(Optional.of(mockCompetency));

            // When & Then: Should throw TestNotReadyException
            assertThatThrownBy(() -> testSessionService.startSession(request))
                    .isInstanceOf(TestNotReadyException.class)
                    .hasMessageContaining("Cannot start test session");

            // Verify that session was never saved
            verify(sessionRepository, never()).save(any(TestSession.class));
        }

        @Test
        @DisplayName("Should successfully start session when assembler returns questions")
        void startSession_WithQuestionsAvailable_ShouldSucceed() {
            // Given: A template with competency and assembler returns questions
            UUID competencyId = UUID.randomUUID();
            UUID question1Id = UUID.randomUUID();
            UUID question2Id = UUID.randomUUID();
            UUID question3Id = UUID.randomUUID();

            mockTemplate.setCompetencyIds(List.of(competencyId));
            mockTemplate.setGoal(AssessmentGoal.OVERVIEW);
            mockTemplate.setQuestionsPerIndicator(3);

            // Create a mock blueprint
            OverviewBlueprint mockBlueprint = mock(OverviewBlueprint.class);
            when(mockBlueprint.getStrategy()).thenReturn(AssessmentGoal.OVERVIEW);
            when(mockBlueprint.deepCopy()).thenReturn(mockBlueprint);

            // Set blueprint on template
            mockTemplate.setTypedBlueprint(mockBlueprint);

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

            // Mock assembler factory to return a mock assembler that returns questions
            TestAssembler mockAssembler = mock(TestAssembler.class);
            when(mockAssembler.assemble(any())).thenReturn(List.of(question1Id, question2Id, question3Id));
            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);

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

            // Verify assembler was used
            verify(assemblerFactory).getAssembler(any(TestBlueprintDto.class));
            verify(mockAssembler).assemble(any());

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
