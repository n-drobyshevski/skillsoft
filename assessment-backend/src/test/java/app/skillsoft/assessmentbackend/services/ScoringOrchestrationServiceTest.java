package app.skillsoft.assessmentbackend.services;

import app.skillsoft.assessmentbackend.domain.dto.CompetencyScoreDto;
import app.skillsoft.assessmentbackend.domain.dto.TestResultDto;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.exception.ResourceNotFoundException;
import app.skillsoft.assessmentbackend.repository.TestAnswerRepository;
import app.skillsoft.assessmentbackend.repository.TestResultRepository;
import app.skillsoft.assessmentbackend.repository.TestSessionRepository;
import app.skillsoft.assessmentbackend.services.impl.ScoringOrchestrationServiceImpl;
import app.skillsoft.assessmentbackend.services.scoring.ConfidenceIntervalCalculator;
import app.skillsoft.assessmentbackend.services.scoring.ScoringResult;
import app.skillsoft.assessmentbackend.services.scoring.ScoringStrategy;
import app.skillsoft.assessmentbackend.services.scoring.ResponseConsistencyAnalyzer;
import app.skillsoft.assessmentbackend.services.scoring.SubscalePercentileCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ScoringOrchestrationService.
 *
 * Tests verify:
 * - Successful scoring calculation with different strategies
 * - Fallback behavior when scoring fails
 * - PENDING result creation on failure
 * - Event publishing for observability
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ScoringOrchestrationService Tests")
class ScoringOrchestrationServiceTest {

    @Mock
    private TestSessionRepository sessionRepository;

    @Mock
    private TestAnswerRepository answerRepository;

    @Mock
    private TestResultRepository resultRepository;

    @Mock
    private ScoringStrategy overviewStrategy;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ConfidenceIntervalCalculator confidenceIntervalCalculator;

    @Mock
    private SubscalePercentileCalculator subscalePercentileCalculator;

    @Mock
    private ResponseConsistencyAnalyzer responseConsistencyAnalyzer;

    private ScoringOrchestrationServiceImpl scoringOrchestrationService;

    private UUID sessionId;
    private UUID templateId;
    private String clerkUserId;
    private TestSession mockSession;
    private TestTemplate mockTemplate;

    @BeforeEach
    void setUp() {
        // Initialize service with mocks
        List<ScoringStrategy> strategies = List.of(overviewStrategy);
        scoringOrchestrationService = new ScoringOrchestrationServiceImpl(
                sessionRepository,
                answerRepository,
                resultRepository,
                strategies,
                eventPublisher,
                confidenceIntervalCalculator,
                subscalePercentileCalculator,
                responseConsistencyAnalyzer
        );

        sessionId = UUID.randomUUID();
        templateId = UUID.randomUUID();
        clerkUserId = "user_test123";

        // Set up mock template
        mockTemplate = new TestTemplate();
        mockTemplate.setId(templateId);
        mockTemplate.setName("Test Assessment");
        mockTemplate.setGoal(AssessmentGoal.OVERVIEW);
        mockTemplate.setPassingScore(70.0);

        // Set up mock session
        mockSession = new TestSession();
        mockSession.setId(sessionId);
        mockSession.setTemplate(mockTemplate);
        mockSession.setClerkUserId(clerkUserId);
        mockSession.setStatus(SessionStatus.COMPLETED);
        mockSession.setQuestionOrder(List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));
    }

    @Nested
    @DisplayName("Calculate And Save Result Tests")
    class CalculateAndSaveResultTests {

        @Test
        @DisplayName("Should calculate and save result successfully with scoring strategy")
        void shouldCalculateAndSaveResultSuccessfully() {
            // Given: No existing result for this session
            when(resultRepository.findBySession_IdAndStatus(sessionId, ResultStatus.COMPLETED))
                    .thenReturn(Optional.empty());
            when(resultRepository.findBySession_Id(sessionId)).thenReturn(Optional.empty());
            when(sessionRepository.findByIdWithTemplate(sessionId)).thenReturn(Optional.of(mockSession));
            when(answerRepository.findBySession_Id(sessionId)).thenReturn(createMockAnswers(3));

            // Mock scoring strategy
            when(overviewStrategy.getSupportedGoal()).thenReturn(AssessmentGoal.OVERVIEW);
            ScoringResult scoringResult = new ScoringResult();
            scoringResult.setOverallScore(85.0);
            scoringResult.setOverallPercentage(85.0);
            scoringResult.setGoal(AssessmentGoal.OVERVIEW);
            scoringResult.setCompetencyScores(List.of());
            when(overviewStrategy.calculate(any(), any())).thenReturn(scoringResult);

            // Mock result save
            when(resultRepository.save(any(TestResult.class))).thenAnswer(invocation -> {
                TestResult result = invocation.getArgument(0);
                result.setId(UUID.randomUUID());
                return result;
            });

            // Mock percentile calculation
            when(resultRepository.countResultsBelowScore(eq(templateId), anyDouble())).thenReturn(5L);
            when(resultRepository.countResultsByTemplateId(templateId)).thenReturn(10L);

            // Mock response consistency analyzer
            when(responseConsistencyAnalyzer.analyze(anyList()))
                .thenReturn(new ResponseConsistencyAnalyzer.ConsistencyResult(0.95, List.of(), 0.0, 0.0, 0.0));

            // When
            TestResultDto result = scoringOrchestrationService.calculateAndSaveResult(sessionId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.overallScore()).isEqualTo(85.0);
            assertThat(result.overallPercentage()).isEqualTo(85.0);
            assertThat(result.status()).isEqualTo(ResultStatus.COMPLETED);
            assertThat(result.passed()).isTrue();

            // Verify scoring strategy was used
            verify(overviewStrategy).calculate(eq(mockSession), any());

            // Verify result was saved
            verify(resultRepository).save(argThat(savedResult ->
                    savedResult.getOverallScore() == 85.0 &&
                    savedResult.getStatus() == ResultStatus.COMPLETED
            ));

            // Verify events were published (Started + Completed)
            // Events are published through the ApplicationEventPublisher interface
            verify(eventPublisher, atLeast(1)).publishEvent((Object) any());
        }

        @Test
        @DisplayName("Should return existing COMPLETED result without re-scoring (idempotency guard)")
        void shouldReturnExistingCompletedResultWithoutReScoring() {
            // Given: An existing COMPLETED result for this session
            TestResult existingResult = new TestResult(mockSession, clerkUserId);
            existingResult.setId(UUID.randomUUID());
            existingResult.setStatus(ResultStatus.COMPLETED);
            existingResult.setOverallScore(90.0);
            existingResult.setOverallPercentage(90.0);
            existingResult.setPassed(true);
            existingResult.setQuestionsAnswered(5);
            existingResult.setQuestionsSkipped(0);
            existingResult.setTotalTimeSeconds(120);
            existingResult.setCompletedAt(LocalDateTime.now());

            when(resultRepository.findBySession_IdAndStatus(sessionId, ResultStatus.COMPLETED))
                    .thenReturn(Optional.of(existingResult));
            when(sessionRepository.findByIdWithTemplate(sessionId))
                    .thenReturn(Optional.of(mockSession));

            // When
            TestResultDto result = scoringOrchestrationService.calculateAndSaveResult(sessionId);

            // Then: Should return existing result
            assertThat(result).isNotNull();
            assertThat(result.overallScore()).isEqualTo(90.0);
            assertThat(result.overallPercentage()).isEqualTo(90.0);
            assertThat(result.status()).isEqualTo(ResultStatus.COMPLETED);

            // Verify no scoring strategy was invoked (idempotent return)
            verify(overviewStrategy, never()).calculate(any(), any());

            // Verify no new result was saved
            verify(resultRepository, never()).save(any(TestResult.class));

            // Verify no answers were fetched (no scoring needed)
            verify(answerRepository, never()).findBySession_Id(any());
        }

        @Test
        @DisplayName("Should delete PENDING result and re-score when no COMPLETED result exists")
        void shouldDeletePendingResultAndReScore() {
            // Given: A PENDING result exists, but no COMPLETED result
            TestResult pendingResult = new TestResult(mockSession, clerkUserId);
            pendingResult.setId(UUID.randomUUID());
            pendingResult.setStatus(ResultStatus.PENDING);

            when(resultRepository.findBySession_IdAndStatus(sessionId, ResultStatus.COMPLETED))
                    .thenReturn(Optional.empty());
            when(resultRepository.findBySession_Id(sessionId))
                    .thenReturn(Optional.of(pendingResult));
            when(sessionRepository.findByIdWithTemplate(sessionId))
                    .thenReturn(Optional.of(mockSession));
            when(answerRepository.findBySession_Id(sessionId)).thenReturn(createMockAnswers(3));

            when(overviewStrategy.getSupportedGoal()).thenReturn(AssessmentGoal.OVERVIEW);
            ScoringResult scoringResult = new ScoringResult();
            scoringResult.setOverallScore(80.0);
            scoringResult.setOverallPercentage(80.0);
            scoringResult.setGoal(AssessmentGoal.OVERVIEW);
            scoringResult.setCompetencyScores(List.of());
            when(overviewStrategy.calculate(any(), any())).thenReturn(scoringResult);

            when(resultRepository.save(any(TestResult.class))).thenAnswer(invocation -> {
                TestResult r = invocation.getArgument(0);
                r.setId(UUID.randomUUID());
                return r;
            });
            when(resultRepository.countResultsByTemplateId(templateId)).thenReturn(0L);

            when(responseConsistencyAnalyzer.analyze(anyList()))
                .thenReturn(new ResponseConsistencyAnalyzer.ConsistencyResult(0.95, List.of(), 0.0, 0.0, 0.0));

            // When
            TestResultDto result = scoringOrchestrationService.calculateAndSaveResult(sessionId);

            // Then: Should have deleted the pending result
            verify(resultRepository).delete(pendingResult);
            verify(resultRepository).flush();

            // And re-scored
            verify(overviewStrategy).calculate(eq(mockSession), any());
            assertThat(result.status()).isEqualTo(ResultStatus.COMPLETED);
            assertThat(result.overallScore()).isEqualTo(80.0);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when session not found")
        void shouldThrowExceptionWhenSessionNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(resultRepository.findBySession_IdAndStatus(nonExistentId, ResultStatus.COMPLETED))
                    .thenReturn(Optional.empty());
            when(resultRepository.findBySession_Id(nonExistentId)).thenReturn(Optional.empty());
            when(sessionRepository.findByIdWithTemplate(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> scoringOrchestrationService.calculateAndSaveResult(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Session");
        }

        @Test
        @DisplayName("Should use legacy scoring when no strategy matches")
        void shouldUseLegacyScoringWhenNoStrategyMatches() {
            // Given
            mockTemplate.setGoal(AssessmentGoal.JOB_FIT); // No matching strategy in our mock list
            when(resultRepository.findBySession_IdAndStatus(sessionId, ResultStatus.COMPLETED))
                    .thenReturn(Optional.empty());
            when(resultRepository.findBySession_Id(sessionId)).thenReturn(Optional.empty());
            when(sessionRepository.findByIdWithTemplate(sessionId)).thenReturn(Optional.of(mockSession));
            when(answerRepository.findBySession_Id(sessionId)).thenReturn(List.of());

            // Mock legacy score calculation
            when(answerRepository.sumScoreBySessionId(sessionId)).thenReturn(8.0);
            when(answerRepository.sumMaxScoreBySessionId(sessionId)).thenReturn(10.0);

            // Mock result save
            when(resultRepository.save(any(TestResult.class))).thenAnswer(invocation -> {
                TestResult result = invocation.getArgument(0);
                result.setId(UUID.randomUUID());
                return result;
            });

            when(resultRepository.countResultsByTemplateId(templateId)).thenReturn(0L);

            // Mock response consistency analyzer
            when(responseConsistencyAnalyzer.analyze(anyList()))
                .thenReturn(new ResponseConsistencyAnalyzer.ConsistencyResult(1.0, List.of(), 0.0, 0.0, 0.0));

            // When
            TestResultDto result = scoringOrchestrationService.calculateAndSaveResult(sessionId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.overallPercentage()).isEqualTo(80.0);
            assertThat(result.status()).isEqualTo(ResultStatus.COMPLETED);

            // Verify no strategy was used (legacy path)
            verify(overviewStrategy, never()).calculate(any(), any());
        }

        @Test
        @DisplayName("Should calculate passed status correctly based on passing score")
        void shouldCalculatePassedStatusCorrectly() {
            // Given
            mockTemplate.setPassingScore(80.0);
            when(resultRepository.findBySession_IdAndStatus(sessionId, ResultStatus.COMPLETED))
                    .thenReturn(Optional.empty());
            when(resultRepository.findBySession_Id(sessionId)).thenReturn(Optional.empty());
            when(sessionRepository.findByIdWithTemplate(sessionId)).thenReturn(Optional.of(mockSession));
            when(answerRepository.findBySession_Id(sessionId)).thenReturn(createMockAnswers(3));

            when(overviewStrategy.getSupportedGoal()).thenReturn(AssessmentGoal.OVERVIEW);
            ScoringResult scoringResult = new ScoringResult();
            scoringResult.setOverallScore(75.0);
            scoringResult.setOverallPercentage(75.0); // Below passing score of 80
            scoringResult.setGoal(AssessmentGoal.OVERVIEW);
            scoringResult.setCompetencyScores(List.of());
            when(overviewStrategy.calculate(any(), any())).thenReturn(scoringResult);

            when(resultRepository.save(any(TestResult.class))).thenAnswer(invocation -> {
                TestResult result = invocation.getArgument(0);
                result.setId(UUID.randomUUID());
                return result;
            });
            when(resultRepository.countResultsByTemplateId(templateId)).thenReturn(0L);

            // Mock response consistency analyzer
            when(responseConsistencyAnalyzer.analyze(anyList()))
                .thenReturn(new ResponseConsistencyAnalyzer.ConsistencyResult(0.95, List.of(), 0.0, 0.0, 0.0));

            // When
            TestResultDto result = scoringOrchestrationService.calculateAndSaveResult(sessionId);

            // Then
            assertThat(result.passed()).isFalse();
        }
    }

    @Nested
    @DisplayName("Fallback Method Tests")
    class FallbackMethodTests {

        @Test
        @DisplayName("Should create PENDING result on fallback")
        void shouldCreatePendingResultOnFallback() {
            // Given
            when(sessionRepository.findByIdWithTemplate(sessionId)).thenReturn(Optional.of(mockSession));
            when(answerRepository.findBySession_Id(sessionId)).thenReturn(createMockAnswers(3));

            when(resultRepository.save(any(TestResult.class))).thenAnswer(invocation -> {
                TestResult result = invocation.getArgument(0);
                result.setId(UUID.randomUUID());
                return result;
            });

            Exception testException = new RuntimeException("Test scoring failure");

            // When
            TestResultDto result = scoringOrchestrationService.scoringFallback(sessionId, testException);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(ResultStatus.PENDING);
            assertThat(result.overallScore()).isNull();
            assertThat(result.overallPercentage()).isNull();
            assertThat(result.passed()).isNull();
            assertThat(result.questionsAnswered()).isEqualTo(3);

            // Verify PENDING result was saved
            ArgumentCaptor<TestResult> resultCaptor = ArgumentCaptor.forClass(TestResult.class);
            verify(resultRepository).save(resultCaptor.capture());
            TestResult savedResult = resultCaptor.getValue();
            assertThat(savedResult.getStatus()).isEqualTo(ResultStatus.PENDING);

            // Verify ScoringFailedEvent was published
            verify(eventPublisher, atLeast(1)).publishEvent((Object) any());
        }

        @Test
        @DisplayName("Fallback should capture answer statistics even when scoring fails")
        void fallbackShouldCaptureAnswerStatistics() {
            // Given
            List<TestAnswer> answers = createMockAnswersWithStats(5, 2, 150);
            when(sessionRepository.findByIdWithTemplate(sessionId)).thenReturn(Optional.of(mockSession));
            when(answerRepository.findBySession_Id(sessionId)).thenReturn(answers);

            when(resultRepository.save(any(TestResult.class))).thenAnswer(invocation -> {
                TestResult result = invocation.getArgument(0);
                result.setId(UUID.randomUUID());
                return result;
            });

            Exception testException = new RuntimeException("Database timeout");

            // When
            TestResultDto result = scoringOrchestrationService.scoringFallback(sessionId, testException);

            // Then
            assertThat(result.questionsAnswered()).isEqualTo(5);
            assertThat(result.questionsSkipped()).isEqualTo(2);
            // Total time is distributed across all answers
            assertThat(result.totalTimeSeconds()).isGreaterThan(0);
        }
    }

    // Helper methods

    private List<TestAnswer> createMockAnswers(int count) {
        List<TestAnswer> answers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            TestAnswer answer = new TestAnswer();
            answer.setId(UUID.randomUUID());
            answer.setSession(mockSession);
            answer.setIsSkipped(false);
            answer.setAnsweredAt(LocalDateTime.now());
            answer.setTimeSpentSeconds(30);
            answer.setScore(1.0);
            answer.setMaxScore(1.0);
            answers.add(answer);
        }
        return answers;
    }

    private List<TestAnswer> createMockAnswersWithStats(int answered, int skipped, int totalTime) {
        List<TestAnswer> answers = new ArrayList<>();

        // Add answered questions
        for (int i = 0; i < answered; i++) {
            TestAnswer answer = new TestAnswer();
            answer.setId(UUID.randomUUID());
            answer.setSession(mockSession);
            answer.setIsSkipped(false);
            answer.setAnsweredAt(LocalDateTime.now());
            answer.setTimeSpentSeconds(totalTime / (answered + skipped));
            answer.setScore(1.0);
            answer.setMaxScore(1.0);
            answers.add(answer);
        }

        // Add skipped questions
        for (int i = 0; i < skipped; i++) {
            TestAnswer answer = new TestAnswer();
            answer.setId(UUID.randomUUID());
            answer.setSession(mockSession);
            answer.setIsSkipped(true);
            answer.setAnsweredAt(null);
            answer.setTimeSpentSeconds(totalTime / (answered + skipped));
            answers.add(answer);
        }

        return answers;
    }
}
