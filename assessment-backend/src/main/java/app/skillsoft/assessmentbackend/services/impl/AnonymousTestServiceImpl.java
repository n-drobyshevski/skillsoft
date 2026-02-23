package app.skillsoft.assessmentbackend.services.impl;

import app.skillsoft.assessmentbackend.domain.dto.AnonymousResultDetailDto;
import app.skillsoft.assessmentbackend.domain.dto.AnonymousResultSummaryDto;
import app.skillsoft.assessmentbackend.domain.dto.AnonymousSessionRequest;
import app.skillsoft.assessmentbackend.domain.dto.AnonymousSessionResponse;
import app.skillsoft.assessmentbackend.domain.dto.AnonymousTakerInfoRequest;
import app.skillsoft.assessmentbackend.domain.dto.AssessmentQuestionDto;
import app.skillsoft.assessmentbackend.domain.dto.TestAnswerDto;
import app.skillsoft.assessmentbackend.domain.dto.TestResultDto;
import app.skillsoft.assessmentbackend.domain.dto.sharing.LinkValidationResult;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.exception.*;
import app.skillsoft.assessmentbackend.repository.*;
import app.skillsoft.assessmentbackend.repository.spec.AnonymousResultSpecification;
import app.skillsoft.assessmentbackend.services.*;
import app.skillsoft.assessmentbackend.services.assembly.TestAssembler;
import app.skillsoft.assessmentbackend.services.assembly.TestAssemblerFactory;
import app.skillsoft.assessmentbackend.services.sharing.TemplateShareLinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of AnonymousTestService.
 *
 * <p>Manages anonymous test sessions accessed via share links.
 * Uses session access tokens for authentication instead of Clerk JWTs.</p>
 *
 * <p>Security Model:</p>
 * <ul>
 *   <li>Share link token validates access to template</li>
 *   <li>Session access token authenticates the session</li>
 *   <li>IP-based rate limiting protects against abuse</li>
 *   <li>Sessions expire 24 hours after creation</li>
 * </ul>
 *
 * @author SkillSoft Development Team
 */
@Service
@Transactional
public class AnonymousTestServiceImpl implements AnonymousTestService {

    private static final Logger log = LoggerFactory.getLogger(AnonymousTestServiceImpl.class);

    /**
     * Hours until an anonymous session expires if not completed.
     */
    private static final int SESSION_EXPIRY_HOURS = 24;

    private final TemplateShareLinkService shareLinkService;
    private final SessionTokenService sessionTokenService;
    private final RateLimitService rateLimitService;
    private final TestSessionRepository sessionRepository;
    private final TestTemplateRepository templateRepository;
    private final TemplateShareLinkRepository shareLinkRepository;
    private final TestAnswerRepository answerRepository;
    private final AssessmentQuestionRepository questionRepository;
    private final TestResultRepository resultRepository;
    private final ScoringOrchestrationService scoringOrchestrationService;
    private final TestAssemblerFactory assemblerFactory;
    private final BlueprintConversionService blueprintConversionService;
    private final CaptchaVerificationService captchaVerificationService;

    public AnonymousTestServiceImpl(
            TemplateShareLinkService shareLinkService,
            SessionTokenService sessionTokenService,
            RateLimitService rateLimitService,
            TestSessionRepository sessionRepository,
            TestTemplateRepository templateRepository,
            TemplateShareLinkRepository shareLinkRepository,
            TestAnswerRepository answerRepository,
            AssessmentQuestionRepository questionRepository,
            TestResultRepository resultRepository,
            ScoringOrchestrationService scoringOrchestrationService,
            TestAssemblerFactory assemblerFactory,
            BlueprintConversionService blueprintConversionService,
            CaptchaVerificationService captchaVerificationService) {
        this.shareLinkService = shareLinkService;
        this.sessionTokenService = sessionTokenService;
        this.rateLimitService = rateLimitService;
        this.sessionRepository = sessionRepository;
        this.templateRepository = templateRepository;
        this.shareLinkRepository = shareLinkRepository;
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
        this.resultRepository = resultRepository;
        this.scoringOrchestrationService = scoringOrchestrationService;
        this.assemblerFactory = assemblerFactory;
        this.blueprintConversionService = blueprintConversionService;
        this.captchaVerificationService = captchaVerificationService;
    }

    @Override
    public AnonymousSessionResponse createSession(
            AnonymousSessionRequest request,
            String ipAddress,
            String userAgent) {

        log.info("Creating anonymous session from IP {} for share token {}",
                ipAddress, maskToken(request.shareToken()));

        // Step 1: Check rate limit
        rateLimitService.checkRateLimit(ipAddress);

        // Step 1b: Verify CAPTCHA (when enabled)
        if (captchaVerificationService.isEnabled()) {
            if (!captchaVerificationService.verify(request.captchaToken())) {
                log.warn("CAPTCHA verification failed for IP {}", ipAddress);
                throw new IllegalArgumentException("CAPTCHA verification failed");
            }
        }

        // Step 2: Validate share link
        LinkValidationResult validation = shareLinkService.validateLink(request.shareToken());
        if (!validation.valid()) {
            log.warn("Share link validation failed: {}", validation.reason());
            throw mapValidationToException(validation);
        }

        // Step 3: Get the share link and template with eager loading
        TemplateShareLink shareLink = shareLinkRepository.findValidByToken(request.shareToken())
                .orElseThrow(() -> new ShareLinkException(
                        ShareLinkException.ErrorCode.LINK_NOT_FOUND,
                        "Share link not found",
                        "Ссылка для доступа не найдена"
                ));

        TestTemplate template = shareLink.getTemplate();

        // Step 4: Verify template is ready for testing
        if (!template.getIsActive()) {
            log.warn("Template {} is not active", template.getId());
            throw new ShareLinkException(
                    ShareLinkException.ErrorCode.TEMPLATE_NOT_READY,
                    "This test is not currently available",
                    "Этот тест в данный момент недоступен"
            );
        }

        // Step 5: Generate session access token
        SessionTokenService.TokenWithHash tokenWithHash = sessionTokenService.generateTokenWithHash();

        // Step 6: Create session
        TestSession session = new TestSession(
                template,
                shareLink,
                tokenWithHash.hash(),
                ipAddress,
                userAgent
        );

        // Step 7: Generate question order
        List<UUID> questionOrder = generateQuestionOrder(template);
        if (questionOrder == null || questionOrder.isEmpty()) {
            log.error("Cannot create session: No questions available for template {}", template.getId());
            throw new ShareLinkException(
                    ShareLinkException.ErrorCode.TEMPLATE_NOT_READY,
                    "This test has no questions configured",
                    "В этом тесте нет настроенных вопросов"
            );
        }

        session.setQuestionOrder(questionOrder);

        // Step 8: Set time limit from template
        if (template.getTimeLimitMinutes() != null) {
            session.setTimeRemainingSeconds(template.getTimeLimitMinutes() * 60);
        }

        // Step 9: Start the session
        session.start();

        TestSession saved = sessionRepository.save(session);

        // Step 10: Record share link usage
        shareLinkService.recordUsage(request.shareToken());

        log.info("Created anonymous session {} for template {} from IP {}",
                saved.getId(), template.getId(), ipAddress);

        // Step 11: Build response with session token
        return buildSessionResponse(saved, tokenWithHash.token());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> validateSessionToken(String sessionAccessToken) {
        if (sessionAccessToken == null || sessionAccessToken.isBlank()) {
            return Optional.empty();
        }

        String tokenHash = sessionTokenService.hashToken(sessionAccessToken);
        return sessionRepository.findBySessionAccessTokenHash(tokenHash)
                .map(TestSession::getId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnonymousSessionResponse> getSession(UUID sessionId, String sessionAccessToken) {
        TestSession session = validateAndGetSession(sessionId, sessionAccessToken);
        return Optional.of(buildSessionResponse(session, null));
    }

    @Override
    @Transactional(readOnly = true)
    public TestSessionService.CurrentQuestionDto getCurrentQuestion(UUID sessionId, String sessionAccessToken) {
        TestSession session = validateAndGetSession(sessionId, sessionAccessToken);

        // Validate session state
        validateSessionState(session);

        // Get current question
        int currentIndex = session.getCurrentQuestionIndex();
        if (currentIndex >= session.getQuestionOrder().size()) {
            throw new IllegalStateException("No more questions in this session");
        }

        UUID questionId = session.getQuestionOrder().get(currentIndex);
        AssessmentQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", questionId));

        // Get previous answer if exists
        TestAnswerDto previousAnswer = answerRepository
                .findBySession_IdAndQuestion_Id(sessionId, questionId)
                .map(this::toAnswerDto)
                .orElse(null);

        TestTemplate template = session.getTemplate();

        return new TestSessionService.CurrentQuestionDto(
                toQuestionDto(question),
                currentIndex,
                session.getQuestionOrder().size(),
                session.getTimeRemainingSeconds(),
                previousAnswer,
                template.getAllowBackNavigation(),
                template.getAllowSkip()
        );
    }

    @Override
    public TestAnswerDto submitAnswer(
            UUID sessionId,
            String sessionAccessToken,
            UUID questionId,
            int selectedOptionIndex) {

        TestSession session = validateAndGetSession(sessionId, sessionAccessToken);
        validateSessionState(session);

        AssessmentQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", questionId));

        // Find or create answer
        Optional<TestAnswer> existingAnswer = answerRepository
                .findBySession_IdAndQuestion_Id(sessionId, questionId);

        TestAnswer answer;
        if (existingAnswer.isPresent()) {
            answer = existingAnswer.get();
            updateAnswerWithSelection(answer, question, selectedOptionIndex);
        } else {
            answer = new TestAnswer(session, question);
            updateAnswerWithSelection(answer, question, selectedOptionIndex);
        }

        // Update session activity
        session.updateActivity();
        sessionRepository.save(session);

        TestAnswer saved = answerRepository.save(answer);

        log.debug("Anonymous session {} answered question {} with option {}",
                sessionId, questionId, selectedOptionIndex);

        return toAnswerDto(saved);
    }

    @Override
    public AnonymousSessionResponse navigateToQuestion(
            UUID sessionId,
            String sessionAccessToken,
            int questionIndex) {

        TestSession session = validateAndGetSession(sessionId, sessionAccessToken);
        validateSessionState(session);

        // Check if back navigation is allowed
        if (questionIndex < session.getCurrentQuestionIndex() &&
                !session.getTemplate().getAllowBackNavigation()) {
            throw new IllegalStateException("Back navigation is not allowed for this test");
        }

        if (questionIndex < 0 || questionIndex >= session.getQuestionOrder().size()) {
            throw new IllegalArgumentException("Invalid question index: " + questionIndex);
        }

        session.setCurrentQuestionIndex(questionIndex);
        session.updateActivity();

        TestSession saved = sessionRepository.save(session);
        return buildSessionResponse(saved, null);
    }

    @Override
    public AnonymousSessionResponse updateTimeRemaining(
            UUID sessionId,
            String sessionAccessToken,
            int timeRemainingSeconds) {

        TestSession session = validateAndGetSession(sessionId, sessionAccessToken);

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Cannot update time for a session that is not in progress");
        }

        session.setTimeRemainingSeconds(timeRemainingSeconds);
        session.updateActivity();

        // Check if time has run out
        if (timeRemainingSeconds <= 0) {
            session.timeout();
            sessionRepository.save(session);
            scoringOrchestrationService.calculateAndSaveResult(sessionId);
        }

        TestSession saved = sessionRepository.save(session);
        return buildSessionResponse(saved, null);
    }

    @Override
    public void updateSessionMetadata(
            UUID sessionId,
            String sessionAccessToken,
            Integer tabSwitchCount) {

        TestSession session = validateAndGetSession(sessionId, sessionAccessToken);

        AnonymousTakerInfo takerInfo = session.getAnonymousTakerInfo();
        if (takerInfo == null) {
            takerInfo = new AnonymousTakerInfo();
            session.setAnonymousTakerInfo(takerInfo);
        }

        takerInfo.setTabSwitchCount(tabSwitchCount);
        sessionRepository.save(session);

        log.debug("Updated tab switch metadata for session {}: tabSwitchCount={}",
                sessionId, tabSwitchCount);
    }

    @Override
    public TestResultDto completeSession(
            UUID sessionId,
            String sessionAccessToken,
            AnonymousTakerInfoRequest takerInfo) {

        TestSession session = validateAndGetSession(sessionId, sessionAccessToken);

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Cannot complete a session that is not in progress");
        }

        log.info("Completing anonymous session {} with taker info: {} {}",
                sessionId, takerInfo.firstName(), takerInfo.lastName());

        // Set anonymous taker info
        AnonymousTakerInfo info = new AnonymousTakerInfo();
        info.setFirstName(takerInfo.firstName());
        info.setLastName(takerInfo.lastName());
        info.setEmail(takerInfo.email());
        info.setNotes(takerInfo.notes());
        info.setCollectedAt(LocalDateTime.now());
        if (Boolean.TRUE.equals(takerInfo.gdprConsentGiven())) {
            info.setGdprConsentGiven(true);
            info.setGdprConsentAt(LocalDateTime.now());
        }

        session.setAnonymousTakerInfo(info);

        // Complete the session
        session.complete();
        sessionRepository.save(session);

        log.info("Session {} marked as COMPLETED, calculating results", sessionId);

        // Calculate and save results
        TestResultDto result = scoringOrchestrationService.calculateAndSaveResult(sessionId);

        log.info("Anonymous session {} completed with score: {}%",
                sessionId, result.overallPercentage());

        // Time anomaly detection — runs after scoring to avoid interfering with score calculation
        resultRepository.findBySession_Id(sessionId).ifPresent(savedResult -> {
            if (savedResult.getTotalTimeSeconds() != null && savedResult.getQuestionsAnswered() != null
                    && savedResult.getQuestionsAnswered() > 0) {
                double avgSecondsPerQuestion =
                        (double) savedResult.getTotalTimeSeconds() / savedResult.getQuestionsAnswered();
                if (avgSecondsPerQuestion < TestResult.MIN_AVG_SECONDS_PER_QUESTION) {
                    if (savedResult.getExtendedMetrics() == null) {
                        savedResult.setExtendedMetrics(new java.util.HashMap<>());
                    }
                    savedResult.getExtendedMetrics().put(TestResult.METRIC_SUSPICIOUSLY_FAST, true);
                    savedResult.getExtendedMetrics().put(
                            "avgSecondsPerQuestion",
                            Math.round(avgSecondsPerQuestion * 10.0) / 10.0);
                    resultRepository.save(savedResult);
                    log.info("Time anomaly detected for session {}: avg {}s/question (threshold: {}s)",
                            session.getId(), avgSecondsPerQuestion,
                            TestResult.MIN_AVG_SECONDS_PER_QUESTION);
                }
            }
        });

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public AnonymousResultDetailDto getResult(UUID sessionId, String sessionAccessToken) {
        TestSession session = validateAndGetSession(sessionId, sessionAccessToken);

        if (session.getStatus() != SessionStatus.COMPLETED &&
                session.getStatus() != SessionStatus.TIMED_OUT) {
            throw new IllegalStateException("Session is not yet completed");
        }

        TestResult result = resultRepository.findBySession_Id(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Result for session", sessionId));

        return AnonymousResultDetailDto.from(result);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AnonymousResultSummaryDto> listAnonymousResults(UUID templateId, Pageable pageable) {
        // Verify template exists
        if (!templateRepository.existsById(templateId)) {
            throw new ResourceNotFoundException("Template", templateId);
        }

        Page<TestResult> results = resultRepository.findAnonymousByTemplateId(templateId, pageable);
        return results.map(AnonymousResultSummaryDto::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AnonymousResultSummaryDto> listAnonymousResults(
            UUID templateId, AnonymousResultFilter filters, Pageable pageable) {
        // Verify template exists
        if (!templateRepository.existsById(templateId)) {
            throw new ResourceNotFoundException("Template", templateId);
        }

        // If no filters, delegate to the simple query for better performance
        if (filters == null || !filters.hasFilters()) {
            return listAnonymousResults(templateId, pageable);
        }

        // Build specification chain
        Specification<TestResult> spec = Specification
                .where(AnonymousResultSpecification.anonymousForTemplate(templateId))
                .and(AnonymousResultSpecification.completedBetween(filters.dateFrom(), filters.dateTo()))
                .and(AnonymousResultSpecification.scoreBetween(filters.minScore(), filters.maxScore()))
                .and(AnonymousResultSpecification.passedIs(filters.passed()))
                .and(AnonymousResultSpecification.fromShareLink(filters.shareLinkId()));

        Page<TestResult> results = resultRepository.findAll(spec, pageable);
        return results.map(AnonymousResultSummaryDto::from);
    }

    @Override
    @Transactional(readOnly = true)
    public AnonymousResultDetailDto getAnonymousResultDetail(UUID resultId) {
        TestResult result = resultRepository.findAnonymousByIdWithSessionAndTemplate(resultId)
                .orElseThrow(() -> new ResourceNotFoundException("Result", resultId));

        return AnonymousResultDetailDto.from(result);
    }

    @Override
    @Transactional(readOnly = true)
    public AnonymousSessionStats getSessionStats(UUID templateId) {
        if (!templateRepository.existsById(templateId)) {
            throw new ResourceNotFoundException("Template", templateId);
        }

        long total = sessionRepository.countAnonymousByTemplateId(templateId);
        if (total == 0) {
            return AnonymousSessionStats.empty();
        }

        // Query completed and in-progress counts
        long completed = resultRepository.countAnonymousByTemplateId(templateId);
        long inProgress = sessionRepository.countAnonymousInProgressByTemplateId(templateId);
        long abandoned = total - completed - inProgress;

        double completionRate = total > 0 ? (double) completed / total : 0.0;

        return new AnonymousSessionStats(
                total,
                completed,
                abandoned,
                inProgress,
                completionRate
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ShareLinkResultStats getShareLinkStats(UUID shareLinkId) {
        if (!shareLinkRepository.existsById(shareLinkId)) {
            return ShareLinkResultStats.empty(shareLinkId);
        }

        long totalSessions = sessionRepository.countByShareLinkId(shareLinkId);
        long completedResults = resultRepository.countByShareLinkId(shareLinkId);

        Double averageScore = resultRepository.calculateAverageScoreByShareLinkId(shareLinkId);
        Double passRate = resultRepository.calculatePassRateByShareLinkId(shareLinkId);

        return new ShareLinkResultStats(
                shareLinkId,
                totalSessions,
                completedResults,
                averageScore,
                passRate
        );
    }

    // ========================================
    // PRIVATE HELPER METHODS
    // ========================================

    /**
     * Validate session access token and return the session.
     *
     * @param sessionId          The session ID
     * @param sessionAccessToken The session access token
     * @return The validated session
     * @throws InvalidSessionTokenException if token is invalid
     * @throws SessionExpiredException      if session has expired
     */
    private TestSession validateAndGetSession(UUID sessionId, String sessionAccessToken) {
        if (sessionAccessToken == null || sessionAccessToken.isBlank()) {
            throw new InvalidSessionTokenException();
        }

        TestSession session = sessionRepository.findByIdWithTemplateAndShareLink(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));

        // Validate token using constant-time comparison to prevent timing attacks
        if (!sessionTokenService.validateToken(sessionAccessToken, session.getSessionAccessTokenHash())) {
            log.warn("Token mismatch for session {}", sessionId);
            throw new InvalidSessionTokenException();
        }

        // Check if anonymous session has expired (24 hours since creation)
        if (session.isAnonymous() && isSessionExpired(session)) {
            log.info("Anonymous session {} has expired", sessionId);
            throw new SessionExpiredException(sessionId);
        }

        return session;
    }

    /**
     * Check if an anonymous session has expired.
     */
    private boolean isSessionExpired(TestSession session) {
        if (session.getStatus() == SessionStatus.COMPLETED ||
                session.getStatus() == SessionStatus.TIMED_OUT) {
            return false; // Completed sessions don't expire
        }

        LocalDateTime createdAt = session.getCreatedAt();
        if (createdAt == null) {
            return false;
        }

        return LocalDateTime.now().isAfter(createdAt.plusHours(SESSION_EXPIRY_HOURS));
    }

    /**
     * Validate session is in a valid state for operations.
     */
    private void validateSessionState(TestSession session) {
        switch (session.getStatus()) {
            case COMPLETED -> throw new IllegalStateException("Session is already completed");
            case ABANDONED -> throw new IllegalStateException("Session has been abandoned");
            case TIMED_OUT -> throw new IllegalStateException("Session has timed out");
            case NOT_STARTED, IN_PROGRESS -> {
                // Valid states for operations
            }
        }
    }

    /**
     * Generate question order for the template using the blueprint system.
     *
     * <p>Follows the same pattern as TestSessionServiceImpl: obtains typed blueprint,
     * attempts auto-conversion if missing, enriches with candidate context (null for anonymous),
     * then delegates to the appropriate TestAssembler.</p>
     *
     * @param template The test template to generate questions for
     * @return Ordered list of question UUIDs for the session
     * @throws IllegalStateException if template has no valid blueprint
     */
    private List<UUID> generateQuestionOrder(TestTemplate template) {
        var typedBlueprint = template.getTypedBlueprint();

        // Attempt auto-conversion if typedBlueprint is missing
        if (typedBlueprint == null) {
            log.info("Template {} has no typed blueprint, attempting auto-conversion from legacy data",
                    template.getId());

            if (blueprintConversionService.ensureTypedBlueprint(template)) {
                typedBlueprint = template.getTypedBlueprint();
                log.info("Successfully auto-converted legacy blueprint for template {}", template.getId());
            } else {
                log.error("Template {} has no typed blueprint configured and auto-conversion failed",
                        template.getId());
                throw new IllegalStateException(
                        "Template must have a valid blueprint for test assembly. " +
                        "Please configure the template with at least one competency. " +
                        "Template ID: " + template.getId());
            }
        }

        // Create deep copy for anonymous session (no candidate context to inject)
        var enrichedBlueprint = typedBlueprint.deepCopy();

        TestAssembler assembler = assemblerFactory.getAssembler(enrichedBlueprint);
        log.info("Using {} assembler for anonymous session with goal: {}",
                assembler.getClass().getSimpleName(),
                enrichedBlueprint.getStrategy());

        List<UUID> questions = assembler.assemble(enrichedBlueprint).questionIds();

        log.info("Assembled {} questions for anonymous session on template {}",
                questions.size(), template.getId());

        return questions;
    }

    /**
     * Build session response DTO.
     */
    private AnonymousSessionResponse buildSessionResponse(TestSession session, String accessToken) {
        TestTemplate template = session.getTemplate();

        AnonymousSessionResponse.TemplateInfo templateInfo = new AnonymousSessionResponse.TemplateInfo(
                template.getId(),
                template.getName(),
                template.getDescription(),
                session.getQuestionOrder() != null ? session.getQuestionOrder().size() : 0,
                template.getTimeLimitMinutes(),
                template.getAllowSkip(),
                template.getAllowBackNavigation()
        );

        LocalDateTime expiresAt = session.getCreatedAt() != null
                ? session.getCreatedAt().plusHours(SESSION_EXPIRY_HOURS)
                : LocalDateTime.now().plusHours(SESSION_EXPIRY_HOURS);

        return new AnonymousSessionResponse(
                session.getId(),
                accessToken, // Only populated on creation
                templateInfo,
                expiresAt
        );
    }

    /**
     * Map share link validation failure to exception.
     */
    private ShareLinkException mapValidationToException(LinkValidationResult validation) {
        return switch (validation.reason()) {
            case LinkValidationResult.REASON_NOT_FOUND -> new ShareLinkException(
                    ShareLinkException.ErrorCode.LINK_NOT_FOUND,
                    "Share link not found",
                    "Ссылка для доступа не найдена"
            );
            case LinkValidationResult.REASON_EXPIRED -> new ShareLinkException(
                    ShareLinkException.ErrorCode.LINK_EXPIRED,
                    "This link has expired",
                    "Срок действия ссылки истёк"
            );
            case LinkValidationResult.REASON_REVOKED -> new ShareLinkException(
                    ShareLinkException.ErrorCode.LINK_REVOKED,
                    "This link has been revoked",
                    "Ссылка была отозвана"
            );
            case LinkValidationResult.REASON_MAX_USES_REACHED -> new ShareLinkException(
                    ShareLinkException.ErrorCode.LINK_MAX_USES_REACHED,
                    "This link has reached its maximum uses",
                    "Ссылка достигла максимального количества использований"
            );
            case LinkValidationResult.REASON_VISIBILITY_MISMATCH -> new ShareLinkException(
                    ShareLinkException.ErrorCode.TEMPLATE_NOT_LINK_VISIBLE,
                    "This test is not available via link",
                    "Этот тест недоступен по ссылке"
            );
            case LinkValidationResult.REASON_TEMPLATE_ARCHIVED,
                 LinkValidationResult.REASON_TEMPLATE_NOT_FOUND -> new ShareLinkException(
                    ShareLinkException.ErrorCode.TEMPLATE_NOT_READY,
                    "This test is not available",
                    "Этот тест недоступен"
            );
            default -> new ShareLinkException(
                    ShareLinkException.ErrorCode.LINK_NOT_FOUND,
                    "Invalid share link",
                    "Неверная ссылка для доступа"
            );
        };
    }

    /**
     * Update answer with selected option.
     * Uses index-based option IDs consistent with transformAnswerOptions.
     */
    private void updateAnswerWithSelection(TestAnswer answer, AssessmentQuestion question, int optionIndex) {
        List<Map<String, Object>> options = question.getAnswerOptions();
        if (options == null || optionIndex < 0 || optionIndex >= options.size()) {
            throw new IllegalArgumentException("Invalid option index: " + optionIndex);
        }

        // Use index-based ID consistent with transformAnswerOptions
        String optionId = "option-" + optionIndex;
        answer.setSelectedOptionIds(List.of(optionId));
        answer.setAnsweredAt(LocalDateTime.now());
    }

    /**
     * Mask token for logging (show first 8 chars).
     */
    private String maskToken(String token) {
        if (token == null || token.length() < 8) {
            return "***";
        }
        return token.substring(0, 8) + "...";
    }

    /**
     * Convert TestAnswer to DTO.
     */
    private TestAnswerDto toAnswerDto(TestAnswer answer) {
        return new TestAnswerDto(
                answer.getId(),
                answer.getSessionId(),
                answer.getQuestionId(),
                answer.getQuestion() != null ? answer.getQuestion().getQuestionText() : null,
                answer.getSelectedOptionIds(),
                answer.getLikertValue(),
                answer.getRankingOrder(),
                answer.getTextResponse(),
                answer.getAnsweredAt(),
                answer.getTimeSpentSeconds(),
                answer.getIsSkipped(),
                answer.getScore(),
                answer.getMaxScore()
        );
    }

    /**
     * Convert AssessmentQuestion to DTO.
     * Transforms answer options to match frontend expectations.
     */
    private AssessmentQuestionDto toQuestionDto(AssessmentQuestion question) {
        List<Map<String, Object>> transformedOptions = transformAnswerOptions(
                question.getAnswerOptions(),
                question.getQuestionType()
        );

        return new AssessmentQuestionDto(
                question.getId(),
                question.getBehavioralIndicatorId(),
                question.getQuestionText(),
                question.getQuestionType(),
                transformedOptions,
                question.getScoringRubric(),
                question.getTimeLimit(),
                question.getDifficultyLevel(),
                question.getMetadata(),
                question.isActive(),
                question.getOrderIndex()
        );
    }

    /**
     * Transform answer options to match frontend expectations.
     * Adds deterministic IDs and normalizes field names.
     */
    private List<Map<String, Object>> transformAnswerOptions(
            List<Map<String, Object>> options,
            QuestionType questionType) {

        if (options == null || options.isEmpty()) {
            return options;
        }

        List<Map<String, Object>> transformedList = new java.util.ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            Map<String, Object> option = options.get(i);
            Map<String, Object> transformed = new java.util.HashMap<>();

            // Generate deterministic ID based on index
            transformed.put("id", "option-" + i);

            // Map text field
            if (option.containsKey("action")) {
                transformed.put("text", option.get("action"));
            } else if (option.containsKey("text")) {
                transformed.put("text", option.get("text"));
            }

            // Preserve score/effectiveness
            if (option.containsKey("effectiveness")) {
                transformed.put("score", option.get("effectiveness"));
            } else if (option.containsKey("score")) {
                transformed.put("score", option.get("score"));
            }

            // Preserve explanation
            if (option.containsKey("explanation")) {
                transformed.put("explanation", option.get("explanation"));
            }

            transformedList.add(transformed);
        }
        return transformedList;
    }
}
