package app.skillsoft.assessmentbackend.services;

import app.skillsoft.assessmentbackend.domain.dto.StartTestSessionRequest;
import app.skillsoft.assessmentbackend.domain.dto.TestSessionDto;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentQuestion;
import app.skillsoft.assessmentbackend.domain.entities.SessionStatus;
import app.skillsoft.assessmentbackend.domain.entities.TestSession;
import app.skillsoft.assessmentbackend.domain.entities.TestTemplate;
import app.skillsoft.assessmentbackend.exception.DuplicateSessionException;
import app.skillsoft.assessmentbackend.repository.AssessmentQuestionRepository;
import app.skillsoft.assessmentbackend.repository.BehavioralIndicatorRepository;
import app.skillsoft.assessmentbackend.repository.TestAnswerRepository;
import app.skillsoft.assessmentbackend.repository.TestSessionRepository;
import app.skillsoft.assessmentbackend.repository.TestTemplateRepository;
import app.skillsoft.assessmentbackend.services.impl.TestSessionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Test class for duplicate session handling in TestSessionService.
 * Verifies that DuplicateSessionException is thrown with correct context
 * when a user attempts to start a new session while having an in-progress session.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TestSessionService - Duplicate Session Handling")
class TestSessionDuplicateSessionTest {

    @Mock
    private TestSessionRepository sessionRepository;

    @Mock
    private TestTemplateRepository templateRepository;

    @Mock
    private TestAnswerRepository answerRepository;

    @Mock
    private AssessmentQuestionRepository questionRepository;

    @Mock
    private BehavioralIndicatorRepository indicatorRepository;

    private TestSessionService testSessionService;

    private UUID templateId;
    private UUID existingSessionId;
    private String clerkUserId;
    private TestTemplate template;
    private TestSession existingSession;

    @BeforeEach
    void setUp() {
        // Create service with mocked repositories
        testSessionService = new TestSessionServiceImpl(
                sessionRepository,
                templateRepository,
                answerRepository,
                null, // resultRepository - not needed for this test
                questionRepository,
                indicatorRepository,
                Collections.emptyList() // scoringStrategies - not needed for this test
        );

        // Initialize test data
        templateId = UUID.randomUUID();
        existingSessionId = UUID.randomUUID();
        clerkUserId = "user_test123";

        // Create test template
        template = new TestTemplate();
        template.setId(templateId);
        template.setName("Test Template");
        template.setGoal(AssessmentGoal.OVERVIEW);
        template.setIsActive(true);
        template.setCompetencyIds(List.of(UUID.randomUUID()));
        template.setQuestionsPerIndicator(2);
        template.setShuffleQuestions(false);

        // Create existing session
        existingSession = new TestSession(template, clerkUserId);
        existingSession.setId(existingSessionId);
        existingSession.setStatus(SessionStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Should throw DuplicateSessionException when user has in-progress session")
    void shouldThrowDuplicateSessionExceptionWhenUserHasInProgressSession() {
        // Arrange
        StartTestSessionRequest request = new StartTestSessionRequest(templateId, clerkUserId);

        when(templateRepository.findById(templateId))
                .thenReturn(Optional.of(template));

        when(sessionRepository.findByClerkUserIdAndTemplate_IdAndStatus(
                clerkUserId, templateId, SessionStatus.IN_PROGRESS))
                .thenReturn(Optional.of(existingSession));

        // Act & Assert
        assertThatThrownBy(() -> testSessionService.startSession(request))
                .isInstanceOf(DuplicateSessionException.class)
                .hasMessageContaining("User " + clerkUserId)
                .hasMessageContaining("already has an in-progress session")
                .hasMessageContaining("template " + templateId);

        // Verify no new session was created
        verify(sessionRepository, never()).save(any(TestSession.class));
    }

    @Test
    @DisplayName("Should include existing session ID in DuplicateSessionException")
    void shouldIncludeExistingSessionIdInException() {
        // Arrange
        StartTestSessionRequest request = new StartTestSessionRequest(templateId, clerkUserId);

        when(templateRepository.findById(templateId))
                .thenReturn(Optional.of(template));

        when(sessionRepository.findByClerkUserIdAndTemplate_IdAndStatus(
                clerkUserId, templateId, SessionStatus.IN_PROGRESS))
                .thenReturn(Optional.of(existingSession));

        // Act & Assert
        assertThatThrownBy(() -> testSessionService.startSession(request))
                .isInstanceOf(DuplicateSessionException.class)
                .satisfies(exception -> {
                    DuplicateSessionException duplicateException = (DuplicateSessionException) exception;
                    assertThat(duplicateException.getExistingSessionId()).isEqualTo(existingSessionId);
                    assertThat(duplicateException.getTemplateId()).isEqualTo(templateId);
                    assertThat(duplicateException.getClerkUserId()).isEqualTo(clerkUserId);
                });
    }

    @Test
    @DisplayName("Should create new session when no in-progress session exists")
    void shouldCreateNewSessionWhenNoInProgressSessionExists() {
        // Arrange
        StartTestSessionRequest request = new StartTestSessionRequest(templateId, clerkUserId);

        when(templateRepository.findById(templateId))
                .thenReturn(Optional.of(template));

        when(sessionRepository.findByClerkUserIdAndTemplate_IdAndStatus(
                clerkUserId, templateId, SessionStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());

        // Mock question repository for Scenario A (OVERVIEW) - return mock questions
        UUID question1Id = UUID.randomUUID();
        UUID question2Id = UUID.randomUUID();
        AssessmentQuestion mockQuestion1 = mock(AssessmentQuestion.class);
        AssessmentQuestion mockQuestion2 = mock(AssessmentQuestion.class);
        when(mockQuestion1.getId()).thenReturn(question1Id);
        when(mockQuestion2.getId()).thenReturn(question2Id);

        when(questionRepository.findUniversalQuestions(any(UUID.class), anyInt()))
                .thenReturn(List.of(mockQuestion1, mockQuestion2));

        // Mock answer repository for DTO conversion
        when(answerRepository.countAnsweredBySessionId(any(UUID.class)))
                .thenReturn(0L);

        TestSession newSession = new TestSession(template, clerkUserId);
        newSession.setId(UUID.randomUUID());
        newSession.setQuestionOrder(List.of(question1Id, question2Id));
        when(sessionRepository.save(any(TestSession.class)))
                .thenReturn(newSession);

        // Act
        TestSessionDto result = testSessionService.startSession(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.clerkUserId()).isEqualTo(clerkUserId);
        assertThat(result.templateId()).isEqualTo(templateId);
        assertThat(result.totalQuestions()).isEqualTo(2);
        verify(sessionRepository, times(1)).save(any(TestSession.class));
    }

    @Test
    @DisplayName("Should create new session when previous session was completed")
    void shouldCreateNewSessionWhenPreviousSessionWasCompleted() {
        // Arrange
        StartTestSessionRequest request = new StartTestSessionRequest(templateId, clerkUserId);

        // Previous session exists but is COMPLETED, not IN_PROGRESS
        when(templateRepository.findById(templateId))
                .thenReturn(Optional.of(template));

        when(sessionRepository.findByClerkUserIdAndTemplate_IdAndStatus(
                clerkUserId, templateId, SessionStatus.IN_PROGRESS))
                .thenReturn(Optional.empty()); // No IN_PROGRESS session

        // Mock question repository for Scenario A (OVERVIEW) - return mock questions
        UUID question1Id = UUID.randomUUID();
        UUID question2Id = UUID.randomUUID();
        AssessmentQuestion mockQuestion1 = mock(AssessmentQuestion.class);
        AssessmentQuestion mockQuestion2 = mock(AssessmentQuestion.class);
        when(mockQuestion1.getId()).thenReturn(question1Id);
        when(mockQuestion2.getId()).thenReturn(question2Id);

        when(questionRepository.findUniversalQuestions(any(UUID.class), anyInt()))
                .thenReturn(List.of(mockQuestion1, mockQuestion2));

        // Mock answer repository for DTO conversion
        when(answerRepository.countAnsweredBySessionId(any(UUID.class)))
                .thenReturn(0L);

        TestSession newSession = new TestSession(template, clerkUserId);
        newSession.setId(UUID.randomUUID());
        newSession.setQuestionOrder(List.of(question1Id, question2Id));
        when(sessionRepository.save(any(TestSession.class)))
                .thenReturn(newSession);

        // Act
        TestSessionDto result = testSessionService.startSession(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.totalQuestions()).isEqualTo(2);
        verify(sessionRepository, times(1)).save(any(TestSession.class));
    }

    @Test
    @DisplayName("Should allow different users to start sessions for same template")
    void shouldAllowDifferentUsersToStartSessionsForSameTemplate() {
        // Arrange
        String anotherUserId = "user_another456";
        StartTestSessionRequest request = new StartTestSessionRequest(templateId, anotherUserId);

        when(templateRepository.findById(templateId))
                .thenReturn(Optional.of(template));

        // Existing session is for different user
        when(sessionRepository.findByClerkUserIdAndTemplate_IdAndStatus(
                anotherUserId, templateId, SessionStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());

        // Mock question repository for Scenario A (OVERVIEW) - return mock questions
        UUID question1Id = UUID.randomUUID();
        UUID question2Id = UUID.randomUUID();
        AssessmentQuestion mockQuestion1 = mock(AssessmentQuestion.class);
        AssessmentQuestion mockQuestion2 = mock(AssessmentQuestion.class);
        when(mockQuestion1.getId()).thenReturn(question1Id);
        when(mockQuestion2.getId()).thenReturn(question2Id);

        when(questionRepository.findUniversalQuestions(any(UUID.class), anyInt()))
                .thenReturn(List.of(mockQuestion1, mockQuestion2));

        // Mock answer repository for DTO conversion
        when(answerRepository.countAnsweredBySessionId(any(UUID.class)))
                .thenReturn(0L);

        TestSession newSession = new TestSession(template, anotherUserId);
        newSession.setId(UUID.randomUUID());
        newSession.setQuestionOrder(List.of(question1Id, question2Id));
        when(sessionRepository.save(any(TestSession.class)))
                .thenReturn(newSession);

        // Act
        TestSessionDto result = testSessionService.startSession(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.clerkUserId()).isEqualTo(anotherUserId);
        assertThat(result.totalQuestions()).isEqualTo(2);
        verify(sessionRepository, times(1)).save(any(TestSession.class));
    }
}
