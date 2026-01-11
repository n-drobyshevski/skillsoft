package app.skillsoft.assessmentbackend.services.impl;

import app.skillsoft.assessmentbackend.domain.dto.*;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.OverviewBlueprint;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.TestBlueprintDto;
import app.skillsoft.assessmentbackend.domain.dto.sharing.LinkValidationResult;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.exception.*;
import app.skillsoft.assessmentbackend.repository.*;
import app.skillsoft.assessmentbackend.services.*;
import app.skillsoft.assessmentbackend.services.assembly.TestAssembler;
import app.skillsoft.assessmentbackend.services.assembly.TestAssemblerFactory;
import app.skillsoft.assessmentbackend.services.sharing.TemplateShareLinkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
 * Unit tests for AnonymousTestServiceImpl.
 *
 * Tests cover:
 * - Session creation with share link validation
 * - Session token validation
 * - Session retrieval with token authentication
 * - Question retrieval during test
 * - Answer submission
 * - Session completion with taker info
 * - Session and share link statistics
 *
 * @author SkillSoft Development Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AnonymousTestService Implementation Tests")
class AnonymousTestServiceImplTest {

    @Mock
    private TemplateShareLinkService shareLinkService;

    @Mock
    private SessionTokenService sessionTokenService;

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private TestSessionRepository sessionRepository;

    @Mock
    private TestTemplateRepository templateRepository;

    @Mock
    private TemplateShareLinkRepository shareLinkRepository;

    @Mock
    private TestAnswerRepository answerRepository;

    @Mock
    private AssessmentQuestionRepository questionRepository;

    @Mock
    private TestResultRepository resultRepository;

    @Mock
    private ScoringOrchestrationService scoringOrchestrationService;

    @Mock
    private TestAssemblerFactory assemblerFactory;

    @Mock
    private BlueprintConversionService blueprintConversionService;

    private AnonymousTestServiceImpl anonymousTestService;

    // Test data
    private UUID sessionId;
    private UUID templateId;
    private UUID questionId;
    private UUID shareLinkId;
    private String shareToken;
    private String sessionAccessToken;
    private String tokenHash;
    private String ipAddress;
    private String userAgent;

    private TestSession mockSession;
    private TestTemplate mockTemplate;
    private TemplateShareLink mockShareLink;
    private AssessmentQuestion mockQuestion;

    @BeforeEach
    void setUp() {
        anonymousTestService = new AnonymousTestServiceImpl(
                shareLinkService,
                sessionTokenService,
                rateLimitService,
                sessionRepository,
                templateRepository,
                shareLinkRepository,
                answerRepository,
                questionRepository,
                resultRepository,
                scoringOrchestrationService,
                assemblerFactory,
                blueprintConversionService
        );

        // Initialize test data
        sessionId = UUID.randomUUID();
        templateId = UUID.randomUUID();
        questionId = UUID.randomUUID();
        shareLinkId = UUID.randomUUID();
        shareToken = "valid_share_token_base64_encoded_string_12345678901234567890";
        sessionAccessToken = "valid_session_access_token_base64_12345678901234567890123";
        tokenHash = "abc123def456hash789012345678901234567890123456789012345678901234";
        ipAddress = "192.168.1.100";
        userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

        setupMockTemplate();
        setupMockShareLink();
        setupMockSession();
        setupMockQuestion();
    }

    private void setupMockTemplate() {
        mockTemplate = new TestTemplate();
        mockTemplate.setId(templateId);
        mockTemplate.setName("Leadership Assessment");
        mockTemplate.setDescription("Test for leadership competencies");
        mockTemplate.setTimeLimitMinutes(60);
        mockTemplate.setIsActive(true);
        mockTemplate.setAllowSkip(true);
        mockTemplate.setAllowBackNavigation(true);
        mockTemplate.setPassingScore(70.0);
        mockTemplate.setGoal(AssessmentGoal.OVERVIEW);
        mockTemplate.setCreatedAt(LocalDateTime.now());
        mockTemplate.setUpdatedAt(LocalDateTime.now());
    }

    private void setupMockShareLink() {
        mockShareLink = new TemplateShareLink();
        mockShareLink.setId(shareLinkId);
        mockShareLink.setToken(shareToken);
        mockShareLink.setTemplate(mockTemplate);
        mockShareLink.setLabel("Public Assessment Link");
        mockShareLink.setPermission(SharePermission.VIEW);
        mockShareLink.setActive(true);
        mockShareLink.setCreatedAt(LocalDateTime.now());
    }

    private void setupMockSession() {
        mockSession = new TestSession();
        mockSession.setId(sessionId);
        mockSession.setTemplate(mockTemplate);
        mockSession.setShareLink(mockShareLink);
        mockSession.setSessionAccessTokenHash(tokenHash);
        mockSession.setIpAddress(ipAddress);
        mockSession.setUserAgent(userAgent);
        mockSession.setStatus(SessionStatus.IN_PROGRESS);
        mockSession.setCurrentQuestionIndex(0);
        mockSession.setQuestionOrder(List.of(questionId, UUID.randomUUID(), UUID.randomUUID()));
        mockSession.setTimeRemainingSeconds(3600);
        mockSession.setCreatedAt(LocalDateTime.now());
        mockSession.setStartedAt(LocalDateTime.now());
        mockSession.setLastActivityAt(LocalDateTime.now());
    }

    private void setupMockQuestion() {
        mockQuestion = new AssessmentQuestion();
        mockQuestion.setId(questionId);
        mockQuestion.setQuestionText("What is your leadership style?");
        mockQuestion.setQuestionType(QuestionType.SJT);
        mockQuestion.setDifficultyLevel(DifficultyLevel.INTERMEDIATE);
        mockQuestion.setActive(true);

        List<Map<String, Object>> options = new ArrayList<>();
        Map<String, Object> option1 = new HashMap<>();
        option1.put("action", "Lead by example");
        option1.put("effectiveness", 4);
        options.add(option1);

        Map<String, Object> option2 = new HashMap<>();
        option2.put("action", "Delegate tasks");
        option2.put("effectiveness", 3);
        options.add(option2);

        mockQuestion.setAnswerOptions(options);
    }

    // ========================================
    // CREATE SESSION TESTS
    // ========================================

    @Nested
    @DisplayName("Create Session Tests")
    class CreateSessionTests {

        @Test
        @DisplayName("Should successfully create anonymous session with valid share link")
        void createSession_WithValidShareLink_ShouldSucceed() {
            // Given
            AnonymousSessionRequest request = new AnonymousSessionRequest(shareToken);
            SessionTokenService.TokenWithHash tokenWithHash =
                    new SessionTokenService.TokenWithHash(sessionAccessToken, tokenHash);

            // Mock blueprint for question assembly
            OverviewBlueprint mockBlueprint = mock(OverviewBlueprint.class);
            when(mockBlueprint.getStrategy()).thenReturn(AssessmentGoal.OVERVIEW);
            when(mockBlueprint.deepCopy()).thenReturn(mockBlueprint);
            mockTemplate.setTypedBlueprint(mockBlueprint);

            // Mock rate limit check (no exception = allowed)
            doNothing().when(rateLimitService).checkRateLimit(ipAddress);

            // Mock share link validation
            when(shareLinkService.validateLink(shareToken))
                    .thenReturn(LinkValidationResult.valid(templateId, mockTemplate.getName(), SharePermission.VIEW));

            // Mock share link repository
            when(shareLinkRepository.findValidByToken(shareToken))
                    .thenReturn(Optional.of(mockShareLink));

            // Mock token generation
            when(sessionTokenService.generateTokenWithHash()).thenReturn(tokenWithHash);

            // Mock assembler
            TestAssembler mockAssembler = mock(TestAssembler.class);
            when(mockAssembler.assemble(any())).thenReturn(List.of(questionId, UUID.randomUUID()));
            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);

            // Mock session save
            when(sessionRepository.save(any(TestSession.class))).thenAnswer(invocation -> {
                TestSession session = invocation.getArgument(0);
                session.setId(sessionId);
                session.setCreatedAt(LocalDateTime.now());
                return session;
            });

            // Mock usage recording - returns boolean
            when(shareLinkService.recordUsage(shareToken)).thenReturn(true);

            // When
            AnonymousSessionResponse response = anonymousTestService.createSession(request, ipAddress, userAgent);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.sessionId()).isEqualTo(sessionId);
            assertThat(response.sessionAccessToken()).isEqualTo(sessionAccessToken);
            assertThat(response.template()).isNotNull();
            assertThat(response.template().id()).isEqualTo(templateId);
            assertThat(response.template().name()).isEqualTo("Leadership Assessment");
            assertThat(response.expiresAt()).isNotNull();

            // Verify interactions
            verify(rateLimitService).checkRateLimit(ipAddress);
            verify(shareLinkService).validateLink(shareToken);
            verify(sessionTokenService).generateTokenWithHash();
            verify(sessionRepository).save(any(TestSession.class));
            verify(shareLinkService).recordUsage(shareToken);
        }

        @Test
        @DisplayName("Should throw ShareLinkException when link not found")
        void createSession_WithInvalidShareLink_ShouldThrowException() {
            // Given
            AnonymousSessionRequest request = new AnonymousSessionRequest("invalid_token");

            doNothing().when(rateLimitService).checkRateLimit(ipAddress);
            when(shareLinkService.validateLink("invalid_token"))
                    .thenReturn(LinkValidationResult.notFound());

            // When & Then
            assertThatThrownBy(() -> anonymousTestService.createSession(request, ipAddress, userAgent))
                    .isInstanceOf(ShareLinkException.class)
                    .extracting(e -> ((ShareLinkException) e).getErrorCode())
                    .isEqualTo(ShareLinkException.ErrorCode.LINK_NOT_FOUND);

            verify(sessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw ShareLinkException when link expired")
        void createSession_WithExpiredShareLink_ShouldThrowException() {
            // Given
            AnonymousSessionRequest request = new AnonymousSessionRequest(shareToken);

            doNothing().when(rateLimitService).checkRateLimit(ipAddress);
            when(shareLinkService.validateLink(shareToken))
                    .thenReturn(LinkValidationResult.expired());

            // When & Then
            assertThatThrownBy(() -> anonymousTestService.createSession(request, ipAddress, userAgent))
                    .isInstanceOf(ShareLinkException.class)
                    .extracting(e -> ((ShareLinkException) e).getErrorCode())
                    .isEqualTo(ShareLinkException.ErrorCode.LINK_EXPIRED);

            verify(sessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw ShareLinkException when link revoked")
        void createSession_WithRevokedShareLink_ShouldThrowException() {
            // Given
            AnonymousSessionRequest request = new AnonymousSessionRequest(shareToken);

            doNothing().when(rateLimitService).checkRateLimit(ipAddress);
            when(shareLinkService.validateLink(shareToken))
                    .thenReturn(LinkValidationResult.revoked());

            // When & Then
            assertThatThrownBy(() -> anonymousTestService.createSession(request, ipAddress, userAgent))
                    .isInstanceOf(ShareLinkException.class)
                    .extracting(e -> ((ShareLinkException) e).getErrorCode())
                    .isEqualTo(ShareLinkException.ErrorCode.LINK_REVOKED);

            verify(sessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw ShareLinkException when max uses reached")
        void createSession_WithMaxUsesReached_ShouldThrowException() {
            // Given
            AnonymousSessionRequest request = new AnonymousSessionRequest(shareToken);

            doNothing().when(rateLimitService).checkRateLimit(ipAddress);
            when(shareLinkService.validateLink(shareToken))
                    .thenReturn(LinkValidationResult.maxUsesReached());

            // When & Then
            assertThatThrownBy(() -> anonymousTestService.createSession(request, ipAddress, userAgent))
                    .isInstanceOf(ShareLinkException.class)
                    .extracting(e -> ((ShareLinkException) e).getErrorCode())
                    .isEqualTo(ShareLinkException.ErrorCode.LINK_MAX_USES_REACHED);

            verify(sessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw RateLimitExceededException when rate limit exceeded")
        void createSession_WhenRateLimitExceeded_ShouldThrowException() {
            // Given
            AnonymousSessionRequest request = new AnonymousSessionRequest(shareToken);

            doThrow(new RateLimitExceededException(3600L))
                    .when(rateLimitService).checkRateLimit(ipAddress);

            // When & Then
            assertThatThrownBy(() -> anonymousTestService.createSession(request, ipAddress, userAgent))
                    .isInstanceOf(RateLimitExceededException.class);

            verify(shareLinkService, never()).validateLink(any());
            verify(sessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw ShareLinkException when template is not active")
        void createSession_WithInactiveTemplate_ShouldThrowException() {
            // Given
            mockTemplate.setIsActive(false);
            AnonymousSessionRequest request = new AnonymousSessionRequest(shareToken);

            doNothing().when(rateLimitService).checkRateLimit(ipAddress);
            when(shareLinkService.validateLink(shareToken))
                    .thenReturn(LinkValidationResult.valid(templateId, mockTemplate.getName(), SharePermission.VIEW));
            when(shareLinkRepository.findValidByToken(shareToken))
                    .thenReturn(Optional.of(mockShareLink));

            // When & Then
            assertThatThrownBy(() -> anonymousTestService.createSession(request, ipAddress, userAgent))
                    .isInstanceOf(ShareLinkException.class)
                    .extracting(e -> ((ShareLinkException) e).getErrorCode())
                    .isEqualTo(ShareLinkException.ErrorCode.TEMPLATE_NOT_READY);

            verify(sessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw ShareLinkException when no questions available")
        void createSession_WithNoQuestionsAvailable_ShouldThrowException() {
            // Given
            AnonymousSessionRequest request = new AnonymousSessionRequest(shareToken);
            SessionTokenService.TokenWithHash tokenWithHash =
                    new SessionTokenService.TokenWithHash(sessionAccessToken, tokenHash);

            // Mock blueprint
            OverviewBlueprint mockBlueprint = mock(OverviewBlueprint.class);
            when(mockBlueprint.getStrategy()).thenReturn(AssessmentGoal.OVERVIEW);
            when(mockBlueprint.deepCopy()).thenReturn(mockBlueprint);
            mockTemplate.setTypedBlueprint(mockBlueprint);

            doNothing().when(rateLimitService).checkRateLimit(ipAddress);
            when(shareLinkService.validateLink(shareToken))
                    .thenReturn(LinkValidationResult.valid(templateId, mockTemplate.getName(), SharePermission.VIEW));
            when(shareLinkRepository.findValidByToken(shareToken))
                    .thenReturn(Optional.of(mockShareLink));
            when(sessionTokenService.generateTokenWithHash()).thenReturn(tokenWithHash);

            // Mock assembler returning empty list
            TestAssembler mockAssembler = mock(TestAssembler.class);
            when(mockAssembler.assemble(any())).thenReturn(Collections.emptyList());
            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);

            // When & Then
            assertThatThrownBy(() -> anonymousTestService.createSession(request, ipAddress, userAgent))
                    .isInstanceOf(ShareLinkException.class)
                    .extracting(e -> ((ShareLinkException) e).getErrorCode())
                    .isEqualTo(ShareLinkException.ErrorCode.TEMPLATE_NOT_READY);

            verify(sessionRepository, never()).save(any());
        }
    }

    // ========================================
    // VALIDATE SESSION TOKEN TESTS
    // ========================================

    @Nested
    @DisplayName("Validate Session Token Tests")
    class ValidateSessionTokenTests {

        @Test
        @DisplayName("Should return session ID for valid token")
        void validateSessionToken_WithValidToken_ShouldReturnSessionId() {
            // Given
            when(sessionTokenService.hashToken(sessionAccessToken)).thenReturn(tokenHash);
            when(sessionRepository.findBySessionAccessTokenHash(tokenHash))
                    .thenReturn(Optional.of(mockSession));

            // When
            Optional<UUID> result = anonymousTestService.validateSessionToken(sessionAccessToken);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(sessionId);

            verify(sessionTokenService).hashToken(sessionAccessToken);
            verify(sessionRepository).findBySessionAccessTokenHash(tokenHash);
        }

        @Test
        @DisplayName("Should return empty for invalid token")
        void validateSessionToken_WithInvalidToken_ShouldReturnEmpty() {
            // Given
            String invalidToken = "invalid_token";
            String invalidHash = "invalid_hash_value";

            when(sessionTokenService.hashToken(invalidToken)).thenReturn(invalidHash);
            when(sessionRepository.findBySessionAccessTokenHash(invalidHash))
                    .thenReturn(Optional.empty());

            // When
            Optional<UUID> result = anonymousTestService.validateSessionToken(invalidToken);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty for null token")
        void validateSessionToken_WithNullToken_ShouldReturnEmpty() {
            // When
            Optional<UUID> result = anonymousTestService.validateSessionToken(null);

            // Then
            assertThat(result).isEmpty();
            verify(sessionTokenService, never()).hashToken(any());
        }

        @Test
        @DisplayName("Should return empty for blank token")
        void validateSessionToken_WithBlankToken_ShouldReturnEmpty() {
            // When
            Optional<UUID> result = anonymousTestService.validateSessionToken("   ");

            // Then
            assertThat(result).isEmpty();
            verify(sessionTokenService, never()).hashToken(any());
        }
    }

    // ========================================
    // GET SESSION TESTS
    // ========================================

    @Nested
    @DisplayName("Get Session Tests")
    class GetSessionTests {

        @Test
        @DisplayName("Should return session for valid token")
        void getSession_WithValidToken_ShouldReturnSession() {
            // Given
            when(sessionTokenService.hashToken(sessionAccessToken)).thenReturn(tokenHash);
            when(sessionRepository.findByIdWithTemplateAndShareLink(sessionId))
                    .thenReturn(Optional.of(mockSession));

            // When
            Optional<AnonymousSessionResponse> result =
                    anonymousTestService.getSession(sessionId, sessionAccessToken);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().sessionId()).isEqualTo(sessionId);
            assertThat(result.get().template()).isNotNull();
            assertThat(result.get().template().id()).isEqualTo(templateId);
        }

        @Test
        @DisplayName("Should throw InvalidSessionTokenException for invalid token")
        void getSession_WithInvalidToken_ShouldThrowException() {
            // Given
            String wrongToken = "wrong_token";
            String wrongHash = "wrong_hash_value";

            when(sessionTokenService.hashToken(wrongToken)).thenReturn(wrongHash);
            when(sessionRepository.findByIdWithTemplateAndShareLink(sessionId))
                    .thenReturn(Optional.of(mockSession));

            // When & Then
            assertThatThrownBy(() -> anonymousTestService.getSession(sessionId, wrongToken))
                    .isInstanceOf(InvalidSessionTokenException.class);
        }

        @Test
        @DisplayName("Should throw InvalidSessionTokenException for null token")
        void getSession_WithNullToken_ShouldThrowException() {
            // When & Then
            assertThatThrownBy(() -> anonymousTestService.getSession(sessionId, null))
                    .isInstanceOf(InvalidSessionTokenException.class);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when session not found")
        void getSession_WhenSessionNotFound_ShouldThrowException() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(sessionRepository.findByIdWithTemplateAndShareLink(nonExistentId))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> anonymousTestService.getSession(nonExistentId, sessionAccessToken))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw SessionExpiredException for expired session")
        void getSession_WithExpiredSession_ShouldThrowException() {
            // Given
            mockSession.setCreatedAt(LocalDateTime.now().minusHours(25)); // Expired (24h limit)
            mockSession.setStatus(SessionStatus.IN_PROGRESS); // Not completed

            when(sessionTokenService.hashToken(sessionAccessToken)).thenReturn(tokenHash);
            when(sessionRepository.findByIdWithTemplateAndShareLink(sessionId))
                    .thenReturn(Optional.of(mockSession));

            // When & Then
            assertThatThrownBy(() -> anonymousTestService.getSession(sessionId, sessionAccessToken))
                    .isInstanceOf(SessionExpiredException.class);
        }

        @Test
        @DisplayName("Should not throw for completed session even if past expiry time")
        void getSession_WithCompletedSessionPastExpiry_ShouldSucceed() {
            // Given
            mockSession.setCreatedAt(LocalDateTime.now().minusHours(25)); // Past normal expiry
            mockSession.setStatus(SessionStatus.COMPLETED); // But completed

            when(sessionTokenService.hashToken(sessionAccessToken)).thenReturn(tokenHash);
            when(sessionRepository.findByIdWithTemplateAndShareLink(sessionId))
                    .thenReturn(Optional.of(mockSession));

            // When
            Optional<AnonymousSessionResponse> result =
                    anonymousTestService.getSession(sessionId, sessionAccessToken);

            // Then
            assertThat(result).isPresent();
        }
    }

    // ========================================
    // GET CURRENT QUESTION TESTS
    // ========================================

    @Nested
    @DisplayName("Get Current Question Tests")
    class GetCurrentQuestionTests {

        @Test
        @DisplayName("Should return current question for valid session")
        void getCurrentQuestion_ForValidSession_ShouldReturnQuestion() {
            // Given
            when(sessionTokenService.hashToken(sessionAccessToken)).thenReturn(tokenHash);
            when(sessionRepository.findByIdWithTemplateAndShareLink(sessionId))
                    .thenReturn(Optional.of(mockSession));
            when(questionRepository.findById(questionId))
                    .thenReturn(Optional.of(mockQuestion));
            when(answerRepository.findBySession_IdAndQuestion_Id(sessionId, questionId))
                    .thenReturn(Optional.empty());

            // When
            TestSessionService.CurrentQuestionDto result =
                    anonymousTestService.getCurrentQuestion(sessionId, sessionAccessToken);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.question()).isNotNull();
            assertThat(result.question().id()).isEqualTo(questionId);
            assertThat(result.questionIndex()).isEqualTo(0);
            assertThat(result.totalQuestions()).isEqualTo(3);
            assertThat(result.allowBackNavigation()).isTrue();
            assertThat(result.allowSkip()).isTrue();
        }

        @Test
        @DisplayName("Should include previous answer if exists")
        void getCurrentQuestion_WithPreviousAnswer_ShouldIncludeAnswer() {
            // Given
            TestAnswer previousAnswer = new TestAnswer();
            previousAnswer.setId(UUID.randomUUID());
            previousAnswer.setSession(mockSession);
            previousAnswer.setQuestion(mockQuestion);
            previousAnswer.setSelectedOptionIds(List.of("option-0"));
            previousAnswer.setAnsweredAt(LocalDateTime.now());

            when(sessionTokenService.hashToken(sessionAccessToken)).thenReturn(tokenHash);
            when(sessionRepository.findByIdWithTemplateAndShareLink(sessionId))
                    .thenReturn(Optional.of(mockSession));
            when(questionRepository.findById(questionId))
                    .thenReturn(Optional.of(mockQuestion));
            when(answerRepository.findBySession_IdAndQuestion_Id(sessionId, questionId))
                    .thenReturn(Optional.of(previousAnswer));

            // When
            TestSessionService.CurrentQuestionDto result =
                    anonymousTestService.getCurrentQuestion(sessionId, sessionAccessToken);

            // Then
            assertThat(result.previousAnswer()).isNotNull();
            assertThat(result.previousAnswer().selectedOptionIds()).contains("option-0");
        }

        @Test
        @DisplayName("Should throw IllegalStateException for completed session")
        void getCurrentQuestion_ForCompletedSession_ShouldThrowException() {
            // Given
            mockSession.setStatus(SessionStatus.COMPLETED);

            when(sessionTokenService.hashToken(sessionAccessToken)).thenReturn(tokenHash);
            when(sessionRepository.findByIdWithTemplateAndShareLink(sessionId))
                    .thenReturn(Optional.of(mockSession));

            // When & Then
            assertThatThrownBy(() ->
                    anonymousTestService.getCurrentQuestion(sessionId, sessionAccessToken))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("completed");
        }

        @Test
        @DisplayName("Should throw IllegalStateException for timed out session")
        void getCurrentQuestion_ForTimedOutSession_ShouldThrowException() {
            // Given
            mockSession.setStatus(SessionStatus.TIMED_OUT);

            when(sessionTokenService.hashToken(sessionAccessToken)).thenReturn(tokenHash);
            when(sessionRepository.findByIdWithTemplateAndShareLink(sessionId))
                    .thenReturn(Optional.of(mockSession));

            // When & Then
            assertThatThrownBy(() ->
                    anonymousTestService.getCurrentQuestion(sessionId, sessionAccessToken))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("timed out");
        }

        @Test
        @DisplayName("Should throw IllegalStateException when no more questions")
        void getCurrentQuestion_WhenNoMoreQuestions_ShouldThrowException() {
            // Given
            mockSession.setCurrentQuestionIndex(3); // Beyond available questions

            when(sessionTokenService.hashToken(sessionAccessToken)).thenReturn(tokenHash);
            when(sessionRepository.findByIdWithTemplateAndShareLink(sessionId))
                    .thenReturn(Optional.of(mockSession));

            // When & Then
            assertThatThrownBy(() ->
                    anonymousTestService.getCurrentQuestion(sessionId, sessionAccessToken))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No more questions");
        }
    }

    // ========================================
    // SUBMIT ANSWER TESTS
    // ========================================

    @Nested
    @DisplayName("Submit Answer Tests")
    class SubmitAnswerTests {

        @Test
        @DisplayName("Should submit new answer successfully")
        void submitAnswer_NewAnswer_ShouldSucceed() {
            // Given
            when(sessionTokenService.hashToken(sessionAccessToken)).thenReturn(tokenHash);
            when(sessionRepository.findByIdWithTemplateAndShareLink(sessionId))
                    .thenReturn(Optional.of(mockSession));
            when(questionRepository.findById(questionId))
                    .thenReturn(Optional.of(mockQuestion));
            when(answerRepository.findBySession_IdAndQuestion_Id(sessionId, questionId))
                    .thenReturn(Optional.empty());
            when(sessionRepository.save(any(TestSession.class))).thenReturn(mockSession);
            when(answerRepository.save(any(TestAnswer.class))).thenAnswer(invocation -> {
                TestAnswer answer = invocation.getArgument(0);
                answer.setId(UUID.randomUUID());
                return answer;
            });

            // When
            TestAnswerDto result = anonymousTestService.submitAnswer(
                    sessionId, sessionAccessToken, questionId, 0);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.selectedOptionIds()).contains("option-0");

            verify(answerRepository).save(any(TestAnswer.class));
            verify(sessionRepository).save(mockSession);
        }

        @Test
        @DisplayName("Should update existing answer successfully")
        void submitAnswer_ExistingAnswer_ShouldUpdate() {
            // Given
            TestAnswer existingAnswer = new TestAnswer();
            existingAnswer.setId(UUID.randomUUID());
            existingAnswer.setSession(mockSession);
            existingAnswer.setQuestion(mockQuestion);
            existingAnswer.setSelectedOptionIds(List.of("option-0"));

            when(sessionTokenService.hashToken(sessionAccessToken)).thenReturn(tokenHash);
            when(sessionRepository.findByIdWithTemplateAndShareLink(sessionId))
                    .thenReturn(Optional.of(mockSession));
            when(questionRepository.findById(questionId))
                    .thenReturn(Optional.of(mockQuestion));
            when(answerRepository.findBySession_IdAndQuestion_Id(sessionId, questionId))
                    .thenReturn(Optional.of(existingAnswer));
            when(sessionRepository.save(any(TestSession.class))).thenReturn(mockSession);
            when(answerRepository.save(any(TestAnswer.class))).thenReturn(existingAnswer);

            // When
            TestAnswerDto result = anonymousTestService.submitAnswer(
                    sessionId, sessionAccessToken, questionId, 1);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.selectedOptionIds()).contains("option-1");

            verify(answerRepository).save(existingAnswer);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for invalid option index")
        void submitAnswer_InvalidOptionIndex_ShouldThrowException() {
            // Given
            when(sessionTokenService.hashToken(sessionAccessToken)).thenReturn(tokenHash);
            when(sessionRepository.findByIdWithTemplateAndShareLink(sessionId))
                    .thenReturn(Optional.of(mockSession));
            when(questionRepository.findById(questionId))
                    .thenReturn(Optional.of(mockQuestion));
            when(answerRepository.findBySession_IdAndQuestion_Id(sessionId, questionId))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> anonymousTestService.submitAnswer(
                    sessionId, sessionAccessToken, questionId, 99))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid option index");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException for unknown question")
        void submitAnswer_UnknownQuestion_ShouldThrowException() {
            // Given
            UUID unknownQuestionId = UUID.randomUUID();

            when(sessionTokenService.hashToken(sessionAccessToken)).thenReturn(tokenHash);
            when(sessionRepository.findByIdWithTemplateAndShareLink(sessionId))
                    .thenReturn(Optional.of(mockSession));
            when(questionRepository.findById(unknownQuestionId))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> anonymousTestService.submitAnswer(
                    sessionId, sessionAccessToken, unknownQuestionId, 0))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw IllegalStateException for completed session")
        void submitAnswer_CompletedSession_ShouldThrowException() {
            // Given
            mockSession.setStatus(SessionStatus.COMPLETED);

            when(sessionTokenService.hashToken(sessionAccessToken)).thenReturn(tokenHash);
            when(sessionRepository.findByIdWithTemplateAndShareLink(sessionId))
                    .thenReturn(Optional.of(mockSession));

            // When & Then
            assertThatThrownBy(() -> anonymousTestService.submitAnswer(
                    sessionId, sessionAccessToken, questionId, 0))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ========================================
    // COMPLETE SESSION TESTS
    // ========================================

    @Nested
    @DisplayName("Complete Session Tests")
    class CompleteSessionTests {

        @Test
        @DisplayName("Should complete session with taker info successfully")
        void completeSession_WithValidData_ShouldSucceed() {
            // Given
            AnonymousTakerInfoRequest takerInfo = new AnonymousTakerInfoRequest(
                    "John",
                    "Doe",
                    "john.doe@example.com",
                    "Applied for senior position"
            );

            TestResultDto mockResult = new TestResultDto(
                    UUID.randomUUID(),
                    sessionId,
                    templateId,
                    "Leadership Assessment",
                    null,
                    85.0,
                    85.0,
                    75,
                    true,
                    Collections.emptyList(),
                    1800,
                    10,
                    0,
                    10,
                    LocalDateTime.now()
            );

            when(sessionTokenService.hashToken(sessionAccessToken)).thenReturn(tokenHash);
            when(sessionRepository.findByIdWithTemplateAndShareLink(sessionId))
                    .thenReturn(Optional.of(mockSession));
            when(sessionRepository.save(any(TestSession.class))).thenReturn(mockSession);
            when(scoringOrchestrationService.calculateAndSaveResult(sessionId))
                    .thenReturn(mockResult);

            // When
            TestResultDto result = anonymousTestService.completeSession(
                    sessionId, sessionAccessToken, takerInfo);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.overallPercentage()).isEqualTo(85.0);
            assertThat(result.passed()).isTrue();

            verify(sessionRepository).save(argThat(session -> {
                AnonymousTakerInfo info = session.getAnonymousTakerInfo();
                return info != null &&
                        "John".equals(info.getFirstName()) &&
                        "Doe".equals(info.getLastName()) &&
                        "john.doe@example.com".equals(info.getEmail());
            }));
            verify(scoringOrchestrationService).calculateAndSaveResult(sessionId);
        }

        @Test
        @DisplayName("Should throw IllegalStateException for non-in-progress session")
        void completeSession_NotInProgress_ShouldThrowException() {
            // Given
            mockSession.setStatus(SessionStatus.COMPLETED);
            AnonymousTakerInfoRequest takerInfo = new AnonymousTakerInfoRequest(
                    "John", "Doe", null, null);

            when(sessionTokenService.hashToken(sessionAccessToken)).thenReturn(tokenHash);
            when(sessionRepository.findByIdWithTemplateAndShareLink(sessionId))
                    .thenReturn(Optional.of(mockSession));

            // When & Then
            assertThatThrownBy(() -> anonymousTestService.completeSession(
                    sessionId, sessionAccessToken, takerInfo))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not in progress");
        }
    }

    // ========================================
    // GET SESSION STATS TESTS
    // ========================================

    @Nested
    @DisplayName("Get Session Stats Tests")
    class GetSessionStatsTests {

        @Test
        @DisplayName("Should return session statistics for template")
        void getSessionStats_ForValidTemplate_ShouldReturnStats() {
            // Given
            when(templateRepository.existsById(templateId)).thenReturn(true);
            when(sessionRepository.countAnonymousByTemplateId(templateId)).thenReturn(100L);
            when(resultRepository.countAnonymousByTemplateId(templateId)).thenReturn(75L);
            when(sessionRepository.countAnonymousInProgressByTemplateId(templateId)).thenReturn(10L);

            // When
            AnonymousTestService.AnonymousSessionStats stats =
                    anonymousTestService.getSessionStats(templateId);

            // Then
            assertThat(stats).isNotNull();
            assertThat(stats.totalSessions()).isEqualTo(100L);
            assertThat(stats.completedSessions()).isEqualTo(75L);
            assertThat(stats.inProgressSessions()).isEqualTo(10L);
            assertThat(stats.abandonedSessions()).isEqualTo(15L); // 100 - 75 - 10
            assertThat(stats.completionRate()).isEqualTo(0.75);
        }

        @Test
        @DisplayName("Should return empty stats when no sessions")
        void getSessionStats_WithNoSessions_ShouldReturnEmptyStats() {
            // Given
            when(templateRepository.existsById(templateId)).thenReturn(true);
            when(sessionRepository.countAnonymousByTemplateId(templateId)).thenReturn(0L);

            // When
            AnonymousTestService.AnonymousSessionStats stats =
                    anonymousTestService.getSessionStats(templateId);

            // Then
            assertThat(stats).isNotNull();
            assertThat(stats.totalSessions()).isEqualTo(0L);
            assertThat(stats.completedSessions()).isEqualTo(0L);
            assertThat(stats.inProgressSessions()).isEqualTo(0L);
            assertThat(stats.abandonedSessions()).isEqualTo(0L);
            assertThat(stats.completionRate()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException for unknown template")
        void getSessionStats_ForUnknownTemplate_ShouldThrowException() {
            // Given
            UUID unknownTemplateId = UUID.randomUUID();
            when(templateRepository.existsById(unknownTemplateId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> anonymousTestService.getSessionStats(unknownTemplateId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ========================================
    // GET SHARE LINK STATS TESTS
    // ========================================

    @Nested
    @DisplayName("Get Share Link Stats Tests")
    class GetShareLinkStatsTests {

        @Test
        @DisplayName("Should return stats for valid share link")
        void getShareLinkStats_ForValidLink_ShouldReturnStats() {
            // Given
            when(shareLinkRepository.existsById(shareLinkId)).thenReturn(true);
            when(sessionRepository.countByShareLinkId(shareLinkId)).thenReturn(50L);
            when(resultRepository.countByShareLinkId(shareLinkId)).thenReturn(45L);
            when(resultRepository.calculateAverageScoreByShareLinkId(shareLinkId)).thenReturn(78.5);
            when(resultRepository.calculatePassRateByShareLinkId(shareLinkId)).thenReturn(0.85);

            // When
            AnonymousTestService.ShareLinkResultStats stats =
                    anonymousTestService.getShareLinkStats(shareLinkId);

            // Then
            assertThat(stats).isNotNull();
            assertThat(stats.shareLinkId()).isEqualTo(shareLinkId);
            assertThat(stats.totalSessions()).isEqualTo(50L);
            assertThat(stats.completedResults()).isEqualTo(45L);
            assertThat(stats.averageScore()).isEqualTo(78.5);
            assertThat(stats.passRate()).isEqualTo(0.85);
        }

        @Test
        @DisplayName("Should return empty stats for non-existent share link")
        void getShareLinkStats_ForUnknownLink_ShouldReturnEmptyStats() {
            // Given
            UUID unknownLinkId = UUID.randomUUID();
            when(shareLinkRepository.existsById(unknownLinkId)).thenReturn(false);

            // When
            AnonymousTestService.ShareLinkResultStats stats =
                    anonymousTestService.getShareLinkStats(unknownLinkId);

            // Then
            assertThat(stats).isNotNull();
            assertThat(stats.shareLinkId()).isEqualTo(unknownLinkId);
            assertThat(stats.totalSessions()).isEqualTo(0L);
            assertThat(stats.completedResults()).isEqualTo(0L);
            assertThat(stats.averageScore()).isNull();
            assertThat(stats.passRate()).isNull();
        }
    }

    // ========================================
    // NAVIGATE TO QUESTION TESTS
    // ========================================

    @Nested
    @DisplayName("Navigate To Question Tests")
    class NavigateToQuestionTests {

        @Test
        @DisplayName("Should navigate forward successfully")
        void navigateToQuestion_Forward_ShouldSucceed() {
            // Given
            when(sessionTokenService.hashToken(sessionAccessToken)).thenReturn(tokenHash);
            when(sessionRepository.findByIdWithTemplateAndShareLink(sessionId))
                    .thenReturn(Optional.of(mockSession));
            when(sessionRepository.save(any(TestSession.class))).thenReturn(mockSession);

            // When
            AnonymousSessionResponse result = anonymousTestService.navigateToQuestion(
                    sessionId, sessionAccessToken, 1);

            // Then
            assertThat(result).isNotNull();
            verify(sessionRepository).save(argThat(session ->
                    session.getCurrentQuestionIndex() == 1));
        }

        @Test
        @DisplayName("Should navigate backward when allowed")
        void navigateToQuestion_BackwardWhenAllowed_ShouldSucceed() {
            // Given
            mockSession.setCurrentQuestionIndex(2);

            when(sessionTokenService.hashToken(sessionAccessToken)).thenReturn(tokenHash);
            when(sessionRepository.findByIdWithTemplateAndShareLink(sessionId))
                    .thenReturn(Optional.of(mockSession));
            when(sessionRepository.save(any(TestSession.class))).thenReturn(mockSession);

            // When
            AnonymousSessionResponse result = anonymousTestService.navigateToQuestion(
                    sessionId, sessionAccessToken, 1);

            // Then
            assertThat(result).isNotNull();
            verify(sessionRepository).save(argThat(session ->
                    session.getCurrentQuestionIndex() == 1));
        }

        @Test
        @DisplayName("Should throw exception when back navigation not allowed")
        void navigateToQuestion_BackwardWhenNotAllowed_ShouldThrowException() {
            // Given
            mockTemplate.setAllowBackNavigation(false);
            mockSession.setCurrentQuestionIndex(2);

            when(sessionTokenService.hashToken(sessionAccessToken)).thenReturn(tokenHash);
            when(sessionRepository.findByIdWithTemplateAndShareLink(sessionId))
                    .thenReturn(Optional.of(mockSession));

            // When & Then
            assertThatThrownBy(() -> anonymousTestService.navigateToQuestion(
                    sessionId, sessionAccessToken, 1))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Back navigation is not allowed");
        }

        @Test
        @DisplayName("Should throw exception for invalid question index")
        void navigateToQuestion_InvalidIndex_ShouldThrowException() {
            // Given
            when(sessionTokenService.hashToken(sessionAccessToken)).thenReturn(tokenHash);
            when(sessionRepository.findByIdWithTemplateAndShareLink(sessionId))
                    .thenReturn(Optional.of(mockSession));

            // When & Then
            assertThatThrownBy(() -> anonymousTestService.navigateToQuestion(
                    sessionId, sessionAccessToken, 99))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid question index");
        }
    }

    // ========================================
    // UPDATE TIME REMAINING TESTS
    // ========================================

    @Nested
    @DisplayName("Update Time Remaining Tests")
    class UpdateTimeRemainingTests {

        @Test
        @DisplayName("Should update time remaining successfully")
        void updateTimeRemaining_WithValidData_ShouldSucceed() {
            // Given
            when(sessionTokenService.hashToken(sessionAccessToken)).thenReturn(tokenHash);
            when(sessionRepository.findByIdWithTemplateAndShareLink(sessionId))
                    .thenReturn(Optional.of(mockSession));
            when(sessionRepository.save(any(TestSession.class))).thenReturn(mockSession);

            // When
            AnonymousSessionResponse result = anonymousTestService.updateTimeRemaining(
                    sessionId, sessionAccessToken, 1800);

            // Then
            assertThat(result).isNotNull();
            verify(sessionRepository).save(argThat(session ->
                    session.getTimeRemainingSeconds() == 1800));
        }

        @Test
        @DisplayName("Should timeout session when time reaches zero")
        void updateTimeRemaining_WhenTimeReachesZero_ShouldTimeout() {
            // Given
            when(sessionTokenService.hashToken(sessionAccessToken)).thenReturn(tokenHash);
            when(sessionRepository.findByIdWithTemplateAndShareLink(sessionId))
                    .thenReturn(Optional.of(mockSession));
            when(sessionRepository.save(any(TestSession.class))).thenReturn(mockSession);

            // When
            anonymousTestService.updateTimeRemaining(sessionId, sessionAccessToken, 0);

            // Then
            verify(scoringOrchestrationService).calculateAndSaveResult(sessionId);
            verify(sessionRepository, atLeast(1)).save(argThat(session ->
                    session.getStatus() == SessionStatus.TIMED_OUT));
        }

        @Test
        @DisplayName("Should throw exception for non-in-progress session")
        void updateTimeRemaining_NotInProgress_ShouldThrowException() {
            // Given
            mockSession.setStatus(SessionStatus.COMPLETED);

            when(sessionTokenService.hashToken(sessionAccessToken)).thenReturn(tokenHash);
            when(sessionRepository.findByIdWithTemplateAndShareLink(sessionId))
                    .thenReturn(Optional.of(mockSession));

            // When & Then
            assertThatThrownBy(() -> anonymousTestService.updateTimeRemaining(
                    sessionId, sessionAccessToken, 1800))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not in progress");
        }
    }

    // ========================================
    // GET RESULT TESTS
    // ========================================

    @Nested
    @DisplayName("Get Result Tests")
    class GetResultTests {

        @Test
        @DisplayName("Should return result for completed session")
        void getResult_ForCompletedSession_ShouldReturnResult() {
            // Given
            mockSession.setStatus(SessionStatus.COMPLETED);

            TestResult mockResult = new TestResult();
            mockResult.setId(UUID.randomUUID());
            mockResult.setSession(mockSession);
            mockResult.setOverallScore(85.0);
            mockResult.setOverallPercentage(85.0);
            mockResult.setPassed(true);
            mockResult.setPercentile(75);
            mockResult.setCompetencyScores(Collections.emptyList());

            when(sessionTokenService.hashToken(sessionAccessToken)).thenReturn(tokenHash);
            when(sessionRepository.findByIdWithTemplateAndShareLink(sessionId))
                    .thenReturn(Optional.of(mockSession));
            when(resultRepository.findBySession_Id(sessionId))
                    .thenReturn(Optional.of(mockResult));

            // When
            AnonymousResultDetailDto result = anonymousTestService.getResult(
                    sessionId, sessionAccessToken);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.overallPercentage()).isEqualTo(85.0);
            assertThat(result.passed()).isTrue();
        }

        @Test
        @DisplayName("Should throw exception for non-completed session")
        void getResult_ForNonCompletedSession_ShouldThrowException() {
            // Given
            mockSession.setStatus(SessionStatus.IN_PROGRESS);

            when(sessionTokenService.hashToken(sessionAccessToken)).thenReturn(tokenHash);
            when(sessionRepository.findByIdWithTemplateAndShareLink(sessionId))
                    .thenReturn(Optional.of(mockSession));

            // When & Then
            assertThatThrownBy(() -> anonymousTestService.getResult(sessionId, sessionAccessToken))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not yet completed");
        }

        @Test
        @DisplayName("Should return result for timed out session")
        void getResult_ForTimedOutSession_ShouldReturnResult() {
            // Given
            mockSession.setStatus(SessionStatus.TIMED_OUT);

            TestResult mockResult = new TestResult();
            mockResult.setId(UUID.randomUUID());
            mockResult.setSession(mockSession);
            mockResult.setOverallScore(50.0);
            mockResult.setOverallPercentage(50.0);
            mockResult.setPassed(false);
            mockResult.setPercentile(30);
            mockResult.setCompetencyScores(Collections.emptyList());

            when(sessionTokenService.hashToken(sessionAccessToken)).thenReturn(tokenHash);
            when(sessionRepository.findByIdWithTemplateAndShareLink(sessionId))
                    .thenReturn(Optional.of(mockSession));
            when(resultRepository.findBySession_Id(sessionId))
                    .thenReturn(Optional.of(mockResult));

            // When
            AnonymousResultDetailDto result = anonymousTestService.getResult(
                    sessionId, sessionAccessToken);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.passed()).isFalse();
        }
    }

    // ========================================
    // LIST ANONYMOUS RESULTS TESTS
    // ========================================

    @Nested
    @DisplayName("List Anonymous Results Tests")
    class ListAnonymousResultsTests {

        @Test
        @DisplayName("Should return paginated results for template")
        void listAnonymousResults_ForValidTemplate_ShouldReturnResults() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);

            TestResult mockResult = new TestResult();
            mockResult.setId(UUID.randomUUID());
            mockResult.setSession(mockSession);
            mockResult.setOverallScore(85.0);
            mockResult.setOverallPercentage(85.0);
            mockResult.setPassed(true);
            mockResult.setCompletedAt(LocalDateTime.now());
            mockResult.setCompetencyScores(Collections.emptyList());

            Page<TestResult> resultPage = new PageImpl<>(List.of(mockResult), pageable, 1);

            when(templateRepository.existsById(templateId)).thenReturn(true);
            when(resultRepository.findAnonymousByTemplateId(templateId, pageable))
                    .thenReturn(resultPage);

            // When
            Page<AnonymousResultSummaryDto> results =
                    anonymousTestService.listAnonymousResults(templateId, pageable);

            // Then
            assertThat(results).isNotNull();
            assertThat(results.getContent()).hasSize(1);
            assertThat(results.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException for unknown template")
        void listAnonymousResults_ForUnknownTemplate_ShouldThrowException() {
            // Given
            UUID unknownTemplateId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 10);

            when(templateRepository.existsById(unknownTemplateId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() ->
                    anonymousTestService.listAnonymousResults(unknownTemplateId, pageable))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
