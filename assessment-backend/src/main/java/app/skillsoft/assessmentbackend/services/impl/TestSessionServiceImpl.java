package app.skillsoft.assessmentbackend.services.impl;

import app.skillsoft.assessmentbackend.domain.dto.*;
import app.skillsoft.assessmentbackend.domain.dto.TemplateReadinessResponse.CompetencyReadiness;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.JobFitBlueprint;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.TestBlueprintDto;
import app.skillsoft.assessmentbackend.domain.dto.simulation.HealthStatus;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.exception.DuplicateSessionException;
import app.skillsoft.assessmentbackend.exception.ResourceNotFoundException;
import app.skillsoft.assessmentbackend.exception.TestNotReadyException;
import app.skillsoft.assessmentbackend.services.validation.InventoryHeatmapService;
import app.skillsoft.assessmentbackend.services.psychometrics.PsychometricAuditJob;
import app.skillsoft.assessmentbackend.services.assembly.TestAssembler;
import app.skillsoft.assessmentbackend.services.assembly.TestAssemblerFactory;
import app.skillsoft.assessmentbackend.repository.*;
import app.skillsoft.assessmentbackend.services.ActivityTrackingService;
import app.skillsoft.assessmentbackend.services.BlueprintConversionService;
import app.skillsoft.assessmentbackend.services.ScoringOrchestrationService;
import app.skillsoft.assessmentbackend.services.TestSessionService;
import app.skillsoft.assessmentbackend.services.selection.QuestionSelectionService;
import app.skillsoft.assessmentbackend.events.assembly.AssemblyCompletedEvent;
import app.skillsoft.assessmentbackend.events.assembly.AssemblyFailedEvent;
import app.skillsoft.assessmentbackend.events.assembly.AssemblyProgress;
import app.skillsoft.assessmentbackend.events.assembly.AssemblyStartedEvent;
import app.skillsoft.assessmentbackend.services.assembly.AssemblyProgressTracker;
import app.skillsoft.assessmentbackend.util.LoggingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class TestSessionServiceImpl implements TestSessionService {

    private static final Logger log = LoggerFactory.getLogger(TestSessionServiceImpl.class);

    private final TestSessionRepository sessionRepository;
    private final TestTemplateRepository templateRepository;
    private final TestAnswerRepository answerRepository;
    private final AssessmentQuestionRepository questionRepository;
    private final BehavioralIndicatorRepository indicatorRepository;
    private final CompetencyRepository competencyRepository;
    private final InventoryHeatmapService inventoryHeatmapService;
    private final PsychometricAuditJob psychometricAuditJob;
    private final TestAssemblerFactory assemblerFactory;
    private final ApplicationEventPublisher eventPublisher;
    private final AssemblyProgressTracker assemblyProgressTracker;
    private final ScoringOrchestrationService scoringOrchestrationService;
    private final ActivityTrackingService activityTrackingService;
    private final BlueprintConversionService blueprintConversionService;
    private final QuestionSelectionService questionSelectionService;

    public TestSessionServiceImpl(
            TestSessionRepository sessionRepository,
            TestTemplateRepository templateRepository,
            TestAnswerRepository answerRepository,
            AssessmentQuestionRepository questionRepository,
            BehavioralIndicatorRepository indicatorRepository,
            CompetencyRepository competencyRepository,
            InventoryHeatmapService inventoryHeatmapService,
            PsychometricAuditJob psychometricAuditJob,
            TestAssemblerFactory assemblerFactory,
            ApplicationEventPublisher eventPublisher,
            AssemblyProgressTracker assemblyProgressTracker,
            ScoringOrchestrationService scoringOrchestrationService,
            ActivityTrackingService activityTrackingService,
            BlueprintConversionService blueprintConversionService,
            QuestionSelectionService questionSelectionService) {
        this.sessionRepository = sessionRepository;
        this.templateRepository = templateRepository;
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
        this.indicatorRepository = indicatorRepository;
        this.competencyRepository = competencyRepository;
        this.inventoryHeatmapService = inventoryHeatmapService;
        this.psychometricAuditJob = psychometricAuditJob;
        this.assemblerFactory = assemblerFactory;
        this.eventPublisher = eventPublisher;
        this.assemblyProgressTracker = assemblyProgressTracker;
        this.scoringOrchestrationService = scoringOrchestrationService;
        this.activityTrackingService = activityTrackingService;
        this.blueprintConversionService = blueprintConversionService;
        this.questionSelectionService = questionSelectionService;
    }

    @Override
    @Transactional
    public TestSessionDto startSession(StartTestSessionRequest request) {
        // Set logging context for this operation
        LoggingContext.setUserId(request.clerkUserId());
        LoggingContext.setTemplateId(request.templateId());
        LoggingContext.setOperation("startSession");

        log.info("Starting new test session for user={} template={}",
                request.clerkUserId(), request.templateId());

        // Verify template exists and is active
        TestTemplate template = templateRepository.findById(request.templateId())
                .orElseThrow(() -> new ResourceNotFoundException("Template", request.templateId()));

        if (!template.getIsActive()) {
            log.warn("Attempted to start session for inactive template={}", request.templateId());
            throw new IllegalStateException("Cannot start session for inactive template");
        }

        // Check if user already has an in-progress session for this template
        Optional<TestSession> existingSession = sessionRepository
                .findByClerkUserIdAndTemplate_IdAndStatus(request.clerkUserId(), request.templateId(), SessionStatus.IN_PROGRESS);

        if (existingSession.isPresent()) {
            TestSession existing = existingSession.get();
            log.info("Duplicate session detected: user={} has existing session={} for template={}",
                    request.clerkUserId(), existing.getId(), request.templateId());
            throw new DuplicateSessionException(existing.getId(), request.templateId(), request.clerkUserId());
        }

        // Create new session with pre-assigned UUID for deterministic question ordering (BE-008)
        TestSession session = new TestSession(template, request.clerkUserId());
        UUID sessionId = UUID.randomUUID();
        session.setId(sessionId);

        // Seed the question selection service for reproducible test form generation.
        // The same sessionId always produces the same question order, enabling
        // psychometric validation of test forms.
        questionSelectionService.setSessionSeed(sessionId);

        // Generate question order based on template configuration
        // Pass clerkUserId for Delta Testing (gap-based question selection)
        List<UUID> questionOrder;
        try {
            questionOrder = generateQuestionOrder(template, request.clerkUserId());
        } finally {
            questionSelectionService.clearSessionSeed();
        }

        // CRITICAL VALIDATION: Prevent sessions with empty question order
        if (questionOrder == null || questionOrder.isEmpty()) {
            log.error("Cannot start session: No questions available for template {}. " +
                     "Competencies: {}, QuestionsPerIndicator: {}, Goal: {}",
                     template.getId(), template.getCompetencyIds(),
                     template.getQuestionsPerIndicator(), template.getGoal());

            // Build detailed error info using readiness check
            TemplateReadinessResponse readiness = checkTemplateReadiness(template.getId());

            List<TestNotReadyException.CompetencyIssue> issues = readiness.competencyReadiness().stream()
                    .filter(cr -> !cr.issues().isEmpty())
                    .map(cr -> new TestNotReadyException.CompetencyIssue(
                            cr.competencyId(),
                            cr.competencyName(),
                            cr.questionsAvailable(),
                            cr.questionsRequired(),
                            cr.healthStatus(),
                            cr.issues()
                    ))
                    .toList();

            throw new TestNotReadyException(
                    template.getId(),
                    issues,
                    readiness.totalQuestionsAvailable(),
                    readiness.questionsRequired()
            );
        }

        log.info("Generated {} questions for test session (template: {}, goal: {})",
                 questionOrder.size(), template.getId(), template.getGoal());

        session.setQuestionOrder(questionOrder);

        // Set initial time remaining
        if (template.getTimeLimitMinutes() != null) {
            session.setTimeRemainingSeconds(template.getTimeLimitMinutes() * 60);
        }

        // Start the session
        session.start();

        TestSession saved = sessionRepository.save(session);

        // Record activity event for audit trail
        activityTrackingService.recordSessionStarted(saved);

        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TestSessionDto> findById(UUID sessionId) {
        return sessionRepository.findByIdWithTemplate(sessionId)
                .map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TestSessionSummaryDto> findByUser(String clerkUserId, Pageable pageable) {
        // Use JOIN FETCH to avoid N+1 when accessing template
        Page<TestSession> sessions = sessionRepository.findByClerkUserIdWithTemplate(clerkUserId, pageable);

        // Batch fetch answer counts to avoid N+1 queries
        List<UUID> sessionIds = sessions.getContent().stream()
                .map(TestSession::getId)
                .toList();

        Map<UUID, Long> answerCounts = batchFetchAnswerCounts(sessionIds);

        return sessions.map(session -> toSummaryDto(session, answerCounts.getOrDefault(session.getId(), 0L)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestSessionSummaryDto> findByUserAndStatus(String clerkUserId, SessionStatus status) {
        // Use JOIN FETCH to avoid N+1 when accessing template
        List<TestSession> sessions = sessionRepository.findByClerkUserIdAndStatusWithTemplate(clerkUserId, status);

        // Batch fetch answer counts to avoid N+1 queries
        List<UUID> sessionIds = sessions.stream()
                .map(TestSession::getId)
                .toList();

        Map<UUID, Long> answerCounts = batchFetchAnswerCounts(sessionIds);

        return sessions.stream()
                .map(session -> toSummaryDto(session, answerCounts.getOrDefault(session.getId(), 0L)))
                .toList();
    }

    /**
     * Batch fetch answer counts for multiple sessions in a single query.
     * Prevents N+1 queries when mapping session lists.
     */
    private Map<UUID, Long> batchFetchAnswerCounts(List<UUID> sessionIds) {
        if (sessionIds.isEmpty()) {
            return Map.of();
        }

        List<Object[]> results = answerRepository.countAnsweredBySessionIds(sessionIds);
        Map<UUID, Long> countMap = new HashMap<>();

        for (Object[] row : results) {
            UUID sessionId = (UUID) row[0];
            Long count = (Long) row[1];
            countMap.put(sessionId, count);
        }

        return countMap;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TestSessionDto> findInProgressSession(String clerkUserId, UUID templateId) {
        return sessionRepository.findByClerkUserIdAndTemplate_IdAndStatus(
                clerkUserId, templateId, SessionStatus.IN_PROGRESS)
                .map(this::toDto);
    }

    @Override
    @Transactional
    public TestAnswerDto submitAnswer(SubmitAnswerRequest request) {
        TestSession session = sessionRepository.findById(request.sessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Session", request.sessionId()));

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Cannot submit answer for a session that is not in progress");
        }

        AssessmentQuestion question = questionRepository.findById(request.questionId())
                .orElseThrow(() -> new ResourceNotFoundException("Question", request.questionId()));

        // Check if answer already exists
        Optional<TestAnswer> existingAnswer = answerRepository
                .findBySession_IdAndQuestion_Id(session.getId(), question.getId());

        TestAnswer answer;
        if (existingAnswer.isPresent()) {
            answer = existingAnswer.get();
            // Update existing answer
            if (request.skip()) {
                answer.skip();
            } else {
                updateAnswer(answer, request, question);
            }
        } else {
            // Create new answer
            answer = new TestAnswer(session, question);
            if (request.skip()) {
                answer.skip();
            } else {
                updateAnswer(answer, request, question);
            }
        }

        // Update session activity
        session.updateActivity();
        sessionRepository.save(session);

        TestAnswer saved = answerRepository.save(answer);

        // Trigger psychometric analysis on answer submission
        // This will recalculate item statistics if the question reaches a response threshold (50, 100, 150, etc.)
        psychometricAuditJob.onAnswerSubmitted(question.getId());

        return toAnswerDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CurrentQuestionDto getCurrentQuestion(UUID sessionId) {
        // ===== 5-LAYER VALIDATION FOR GETCURRENTQUESTION =====

        // Layer 1: Session Exists Validation → 404 if not found
        TestSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));

        // Layer 2: Session Status Validation → 400 if COMPLETED/ABANDONED
        if (session.getStatus() == SessionStatus.COMPLETED) {
            throw new IllegalStateException("Cannot get current question for a completed session");
        }
        if (session.getStatus() == SessionStatus.ABANDONED) {
            throw new IllegalStateException("Cannot get current question for an abandoned session");
        }
        if (session.getStatus() == SessionStatus.TIMED_OUT) {
            throw new IllegalStateException("Cannot get current question for a timed out session");
        }

        // Layer 3: QuestionOrder Not Null/Empty Validation → 400 if empty
        if (session.getQuestionOrder() == null || session.getQuestionOrder().isEmpty()) {
            log.error("Session {} has no questions in questionOrder. This indicates a question generation failure.",
                    sessionId);
            throw new IllegalStateException("Session has no questions. Please contact support.");
        }

        // Layer 4: Current Question Index In Bounds Validation → 400 if out of bounds
        int currentIndex = session.getCurrentQuestionIndex();
        if (currentIndex < 0) {
            log.error("Session {} has negative current question index: {}", sessionId, currentIndex);
            throw new IllegalStateException("Invalid question index. Please contact support.");
        }
        if (currentIndex >= session.getQuestionOrder().size()) {
            throw new IllegalStateException(
                    String.format("No more questions in session (index %d of %d)",
                            currentIndex + 1, session.getQuestionOrder().size()));
        }

        // Layer 5: Question Exists in Database → 404 if missing
        UUID questionId = session.getQuestionOrder().get(currentIndex);
        AssessmentQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> {
                    log.error("Question {} from session {} questionOrder not found in database",
                            questionId, sessionId);
                    return new ResourceNotFoundException("Question", questionId);
                });

        // Get previous answer if exists
        TestAnswerDto previousAnswer = answerRepository
                .findBySession_IdAndQuestion_Id(sessionId, questionId)
                .map(this::toAnswerDto)
                .orElse(null);

        // Get template settings for navigation controls
        TestTemplate template = session.getTemplate();

        return new CurrentQuestionDto(
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
    @Transactional
    public TestSessionDto navigateToQuestion(UUID sessionId, int questionIndex) {
        TestSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Cannot navigate in a session that is not in progress");
        }

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
        return toDto(saved);
    }

    @Override
    @Transactional
    public TestSessionDto updateTimeRemaining(UUID sessionId, int timeRemainingSeconds) {
        TestSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Cannot update time for a session that is not in progress");
        }

        session.setTimeRemainingSeconds(timeRemainingSeconds);
        session.updateActivity();

        // Check if time has run out
        if (timeRemainingSeconds <= 0) {
            session.timeout();
            sessionRepository.save(session);

            // Record activity event for audit trail
            activityTrackingService.recordSessionTimedOut(session);

            // Calculate results in a separate transaction for isolation
            // This ensures the timeout status is committed first
            scoringOrchestrationService.calculateAndSaveResult(sessionId);
        }

        TestSession saved = sessionRepository.save(session);
        return toDto(saved);
    }

    /**
     * Complete a test session and calculate results.
     *
     * TRANSACTION BOUNDARY:
     * This method runs in its own transaction (TX #1) to mark the session as COMPLETED.
     * Once the session is saved, scoring is delegated to ScoringOrchestrationService
     * which runs in a NEW transaction (TX #2 with REQUIRES_NEW propagation).
     *
     * This separation ensures:
     * 1. Session completion is committed independently
     * 2. If scoring fails, session stays COMPLETED (not rolled back to IN_PROGRESS)
     * 3. Retry logic applies only to scoring operations
     *
     * @param sessionId The ID of the session to complete
     * @return TestResultDto with scoring results (or PENDING status if scoring failed)
     */
    @Override
    @Transactional
    public TestResultDto completeSession(UUID sessionId) {
        // Set logging context for session completion
        LoggingContext.setSessionId(sessionId);
        LoggingContext.setOperation("completeSession");

        log.info("Completing test session sessionId={}", sessionId);

        TestSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));

        // Enrich logging context with session details
        LoggingContext.setUserId(session.getClerkUserId());
        LoggingContext.setTemplateId(session.getTemplate().getId());

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            log.warn("Attempted to complete session in invalid state: sessionId={} status={}",
                    sessionId, session.getStatus());
            throw new IllegalStateException("Cannot complete a session that is not in progress");
        }

        // Mark session as COMPLETED and commit (TX #1)
        // Optimistic lock protects against concurrent complete + abandon/timeout
        session.complete();
        try {
            sessionRepository.save(session);
        } catch (ObjectOptimisticLockingFailureException ex) {
            log.warn("Optimistic lock conflict while completing session={}, re-reading to resolve", sessionId);
            return handleCompleteConflict(sessionId);
        }

        log.info("Session marked as COMPLETED (TX #1 committed), delegating to scoring orchestration sessionId={}", sessionId);

        // Calculate and save results in a NEW transaction (TX #2)
        // If scoring fails, session remains COMPLETED and a PENDING result is created
        TestResultDto result = scoringOrchestrationService.calculateAndSaveResult(sessionId);

        // Record activity event for audit trail
        activityTrackingService.recordSessionCompleted(session, result.overallPercentage(), result.passed());

        return result;
    }

    /**
     * Handle optimistic lock conflict during session completion.
     *
     * Re-reads the session from the database. If another request already completed
     * the session, this returns the existing result (idempotent). If the session
     * was moved to an incompatible terminal state (ABANDONED, TIMED_OUT), throws
     * IllegalStateException since the complete operation can no longer succeed.
     */
    private TestResultDto handleCompleteConflict(UUID sessionId) {
        TestSession freshSession = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));

        if (freshSession.getStatus() == SessionStatus.COMPLETED) {
            // Another request already completed -- idempotent success
            log.info("Session {} was already completed by a concurrent request, returning existing result", sessionId);
            return scoringOrchestrationService.calculateAndSaveResult(sessionId);
        }

        // Session was moved to a different terminal state by a concurrent request
        log.warn("Session {} moved to {} by concurrent request while attempting completion",
                sessionId, freshSession.getStatus());
        throw new IllegalStateException(
                "Session was concurrently modified and is now " + freshSession.getStatus()
                        + ". Cannot complete.");
    }

    @Override
    @Transactional
    public TestSessionDto abandonSession(UUID sessionId) {
        TestSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));

        session.abandon();
        try {
            TestSession saved = sessionRepository.save(session);

            // Record activity event for audit trail
            activityTrackingService.recordSessionAbandoned(saved);

            return toDto(saved);
        } catch (ObjectOptimisticLockingFailureException ex) {
            log.warn("Optimistic lock conflict while abandoning session={}, re-reading to resolve", sessionId);
            return handleAbandonConflict(sessionId);
        }
    }

    /**
     * Handle optimistic lock conflict during session abandonment.
     *
     * Re-reads the session. If it is already in a terminal state (COMPLETED,
     * ABANDONED, TIMED_OUT), returns the current state (idempotent for ABANDONED,
     * graceful acknowledgement for other terminal states). If still IN_PROGRESS,
     * lets the exception propagate since the conflict is unexpected.
     */
    private TestSessionDto handleAbandonConflict(UUID sessionId) {
        TestSession freshSession = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));

        SessionStatus currentStatus = freshSession.getStatus();
        if (currentStatus == SessionStatus.ABANDONED) {
            // Already abandoned by concurrent request -- idempotent success
            log.info("Session {} was already abandoned by a concurrent request", sessionId);
            return toDto(freshSession);
        }

        if (currentStatus == SessionStatus.COMPLETED || currentStatus == SessionStatus.TIMED_OUT) {
            // Session reached a different terminal state -- cannot abandon
            log.warn("Session {} moved to {} by concurrent request while attempting abandon",
                    sessionId, currentStatus);
            throw new IllegalStateException(
                    "Session was concurrently modified and is now " + currentStatus
                            + ". Cannot abandon.");
        }

        // Unexpected: session is still in a non-terminal state but version conflicted
        throw new IllegalStateException(
                "Concurrent modification detected for session " + sessionId
                        + ". Please retry.");
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestAnswerDto> getSessionAnswers(UUID sessionId) {
        return answerRepository.findBySession_IdOrderByAnsweredAtAsc(sessionId).stream()
                .map(this::toAnswerDto)
                .toList();
    }

    @Override
    @Transactional
    public int timeoutStaleSessions() {
        // Find sessions that are in progress but haven't had activity for over 30 minutes
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(30);
        List<TestSession> staleSessions = sessionRepository.findStaleSessions(SessionStatus.IN_PROGRESS, cutoffTime);

        int count = 0;
        for (TestSession session : staleSessions) {
            session.timeout();
            sessionRepository.save(session);

            // Record activity event for audit trail
            activityTrackingService.recordSessionTimedOut(session);

            // Scoring runs in a separate transaction for isolation
            // If scoring fails, session stays TIMED_OUT and a PENDING result is created
            scoringOrchestrationService.calculateAndSaveResult(session.getId());
            count++;
        }

        return count;
    }

    // Helper methods

    /**
     * Generate question order for a test session.
     *
     * Uses the Strategy Pattern with TestAssemblers for templates with typed blueprints.
     * For JOB_FIT assessments, injects the candidate's clerkUserId into the blueprint
     * to enable Delta Testing (gap-based question selection using Competency Passport).
     *
     * Publishes observability events for assembly tracking:
     * - AssemblyStartedEvent before assembly begins
     * - AssemblyCompletedEvent on success with question count and duration
     * - AssemblyFailedEvent on failure with error details
     *
     * @param template The test template
     * @param clerkUserId The Clerk User ID of the candidate taking the test
     * @return Ordered list of question UUIDs for the session
     * @throws IllegalStateException if template has no typed blueprint configured
     */
    private List<UUID> generateQuestionOrder(TestTemplate template, String clerkUserId) {
        var typedBlueprint = template.getTypedBlueprint();
        Instant assemblyStartTime = Instant.now();

        // Attempt auto-conversion if typedBlueprint is missing
        if (typedBlueprint == null) {
            log.info("Template {} has no typed blueprint, attempting auto-conversion from legacy data",
                    template.getId());

            if (blueprintConversionService.ensureTypedBlueprint(template)) {
                typedBlueprint = template.getTypedBlueprint();
                log.info("Successfully auto-converted legacy blueprint for template {}", template.getId());
            } else {
                log.error("Template {} has no typed blueprint configured and auto-conversion failed. " +
                        "Template goal: {}, Legacy blueprint: {}, Legacy competencyIds: {}",
                        template.getId(),
                        template.getGoal(),
                        template.getBlueprint() != null ? "present" : "null",
                        template.getCompetencyIds() != null && !template.getCompetencyIds().isEmpty()
                                ? template.getCompetencyIds().size() + " items" : "empty");

                throw new IllegalStateException(
                        "Template must have a valid blueprint for test assembly. " +
                        "Please go to the template's Blueprint tab and add at least one competency. " +
                        "Template ID: " + template.getId());
            }
        }

        // Inject candidate context into the blueprint for Delta Testing
        // This allows assemblers to use existing passport scores for gap analysis
        TestBlueprintDto enrichedBlueprint = enrichBlueprintWithCandidateContext(typedBlueprint, clerkUserId);

        TestAssembler assembler = assemblerFactory.getAssembler(enrichedBlueprint);
        String assemblerType = assembler.getClass().getSimpleName();

        // Use template ID as tracking ID since session ID is not yet available
        // This allows clients to poll for assembly progress using template ID
        UUID trackingId = template.getId();
        int totalCompetencies = template.getCompetencyIds() != null ? template.getCompetencyIds().size() : 0;

        // Start progress tracking
        assemblyProgressTracker.start(
                trackingId,
                template.getId(),
                enrichedBlueprint.getStrategy(),
                totalCompetencies
        );

        // Publish assembly started event
        eventPublisher.publishEvent(AssemblyStartedEvent.now(
                null, // Session ID not yet available
                template.getId(),
                enrichedBlueprint.getStrategy(),
                assemblerType
        ));

        log.info("Using TestAssembler for goal: {} (blueprint type: {}, candidateId: {})",
            enrichedBlueprint.getStrategy(),
            enrichedBlueprint.getClass().getSimpleName(),
            clerkUserId);

        try {
            // Update progress to SELECTING phase
            assemblyProgressTracker.updatePhase(
                    trackingId,
                    AssemblyProgress.AssemblyPhase.SELECTING,
                    5.0,
                    "Starting question selection"
            );

            List<UUID> questions = assembler.assemble(enrichedBlueprint);

            // Update progress to VALIDATING phase
            assemblyProgressTracker.updatePhase(
                    trackingId,
                    AssemblyProgress.AssemblyPhase.VALIDATING,
                    90.0,
                    "Validating selected questions"
            );

            // Complete progress tracking
            assemblyProgressTracker.complete(trackingId, questions.size());

            // Publish assembly completed event
            eventPublisher.publishEvent(AssemblyCompletedEvent.fromStart(
                    null, // Session ID not yet available
                    template.getId(),
                    enrichedBlueprint.getStrategy(),
                    questions.size(),
                    assemblyStartTime
            ));

            log.info("TestAssembler produced {} questions for goal: {}",
                questions.size(), enrichedBlueprint.getStrategy());

            return questions;
        } catch (Exception e) {
            // Mark progress as failed
            assemblyProgressTracker.fail(trackingId, e.getMessage());

            // Publish assembly failed event
            eventPublisher.publishEvent(AssemblyFailedEvent.fromException(
                    null, // Session ID not yet available
                    template.getId(),
                    enrichedBlueprint.getStrategy(),
                    e,
                    assemblyStartTime
            ));

            log.error("Assembly failed for template {} with goal {}: {}",
                template.getId(), enrichedBlueprint.getStrategy(), e.getMessage());
            throw e; // Re-throw to preserve original behavior
        }
    }

    /**
     * Enrich a blueprint with candidate context for Delta Testing.
     *
     * For JOB_FIT blueprints, sets the candidateClerkUserId to enable
     * the assembler to fetch passport data and perform gap analysis.
     *
     * Creates a deep copy to avoid mutating the original template blueprint.
     *
     * @param blueprint The original blueprint from the template
     * @param clerkUserId The Clerk User ID of the candidate
     * @return An enriched copy of the blueprint with candidate context
     */
    private TestBlueprintDto enrichBlueprintWithCandidateContext(TestBlueprintDto blueprint, String clerkUserId) {
        // Create a deep copy to avoid mutating the template's stored blueprint
        TestBlueprintDto enrichedBlueprint = blueprint.deepCopy();

        // Inject candidate context into JOB_FIT blueprints for Delta Testing
        if (enrichedBlueprint instanceof JobFitBlueprint jobFitBlueprint) {
            jobFitBlueprint.setCandidateClerkUserId(clerkUserId);
            log.debug("Injected candidateClerkUserId '{}' into JobFitBlueprint for Delta Testing", clerkUserId);
        }

        // TODO: Future enhancement - inject candidate context for TEAM_FIT blueprints as well

        return enrichedBlueprint;
    }

    private void updateAnswer(TestAnswer answer, SubmitAnswerRequest request, AssessmentQuestion question) {
        answer.setTimeSpentSeconds(request.timeSpentSeconds());
        answer.setAnsweredAt(LocalDateTime.now());
        answer.setIsSkipped(false);

        // Set the appropriate answer field based on question type
        switch (question.getQuestionType()) {
            case MCQ:
            case MULTIPLE_CHOICE:
            case SJT:
            case SITUATIONAL_JUDGMENT:
                answer.setSelectedOptionIds(request.selectedOptionIds());
                // Calculate score from selected option
                Double optionScore = extractScoreFromSelectedOption(question, request.selectedOptionIds());
                answer.setScore(optionScore);
                break;
            case LIKERT:
            case LIKERT_SCALE:
            case FREQUENCY_SCALE:
                answer.setLikertValue(request.likertValue());
                // Likert scores are normalized later in scoring strategy
                break;
            case OPEN_TEXT:
            case BEHAVIORAL_EXAMPLE:
            case SELF_REFLECTION:
                answer.setTextResponse(request.textResponse());
                break;
            case CAPABILITY_ASSESSMENT:
            case PEER_FEEDBACK:
                // These may require specific handling
                if (request.selectedOptionIds() != null) {
                    answer.setSelectedOptionIds(request.selectedOptionIds());
                    Double capScore = extractScoreFromSelectedOption(question, request.selectedOptionIds());
                    answer.setScore(capScore);
                } else if (request.textResponse() != null) {
                    answer.setTextResponse(request.textResponse());
                }
                break;
            default:
                // Handle unknown or future question types gracefully
                log.warn("Unhandled question type '{}' for question {}. Storing raw response data.",
                        question.getQuestionType(), question.getId());
                // Store any available response data
                if (request.selectedOptionIds() != null && !request.selectedOptionIds().isEmpty()) {
                    answer.setSelectedOptionIds(request.selectedOptionIds());
                } else if (request.likertValue() != null) {
                    answer.setLikertValue(request.likertValue());
                } else if (request.textResponse() != null) {
                    answer.setTextResponse(request.textResponse());
                }
                break;
        }

        answer.setMaxScore(1.0); // Default max score (normalized)
    }

    /**
     * Extract score from the selected option in a question's answer options.
     *
     * Handles both SJT and MCQ question types where options have a "score" field.
     * For SJT: score represents effectiveness (0-1 scale, or integer 0-4)
     * For MCQ: score typically 1 for correct, 0 for incorrect
     *
     * @param question The assessment question with answer options
     * @param selectedOptionIds The list of selected option IDs (typically one element)
     * @return The score value from the matched option, or 0.0 if not found
     */
    private Double extractScoreFromSelectedOption(AssessmentQuestion question, List<String> selectedOptionIds) {
        if (selectedOptionIds == null || selectedOptionIds.isEmpty()) {
            log.debug("No selected option IDs provided for question {}", question.getId());
            return 0.0;
        }

        List<Map<String, Object>> answerOptions = question.getAnswerOptions();
        if (answerOptions == null || answerOptions.isEmpty()) {
            log.debug("No answer options available for question {}", question.getId());
            return 0.0;
        }

        // Get the first selected option ID (most questions are single-select)
        String selectedId = selectedOptionIds.get(0);

        // Search for matching option by ID
        for (int i = 0; i < answerOptions.size(); i++) {
            Map<String, Object> option = answerOptions.get(i);

            // Check for matching ID (options may have "id" field or use index-based ID)
            String optionId = null;
            if (option.containsKey("id")) {
                optionId = String.valueOf(option.get("id"));
            } else {
                // Fallback to index-based ID (consistent with transformAnswerOptions)
                optionId = "option-" + i;
            }

            if (selectedId.equals(optionId)) {
                // Extract score from option
                // SJT uses "effectiveness" field, MCQ uses "score" field
                Object scoreValue = null;
                if (option.containsKey("effectiveness")) {
                    scoreValue = option.get("effectiveness");
                } else if (option.containsKey("score")) {
                    scoreValue = option.get("score");
                }

                if (scoreValue != null) {
                    try {
                        double score = ((Number) scoreValue).doubleValue();
                        // Normalize score to 0-1 range if it's on a scale greater than 1
                        if (score > 1.0) {
                            // Detect the max score from answer options
                            double maxOptionScore = findMaxScoreInOptions(answerOptions);
                            if (maxOptionScore > 1.0) {
                                score = score / maxOptionScore;
                            }
                        }
                        log.debug("Extracted score {} for option {} in question {}",
                            score, selectedId, question.getId());
                        return score;
                    } catch (ClassCastException | NullPointerException e) {
                        log.warn("Could not parse score value '{}' for option {} in question {}",
                            scoreValue, selectedId, question.getId());
                        return 0.0;
                    }
                } else {
                    log.debug("No score field found for option {} in question {}",
                        selectedId, question.getId());
                    return 0.0;
                }
            }
        }

        log.warn("Selected option {} not found in question {} options", selectedId, question.getId());
        return 0.0;
    }

    /**
     * Find the maximum score value among all answer options.
     * Used to dynamically determine the scale for score normalization.
     *
     * @param answerOptions The list of answer options to scan
     * @return The maximum score found, or 1.0 if no scores are found
     */
    private double findMaxScoreInOptions(List<Map<String, Object>> answerOptions) {
        if (answerOptions == null || answerOptions.isEmpty()) {
            return 1.0;
        }

        double maxScore = 0.0;
        for (Map<String, Object> option : answerOptions) {
            Object scoreValue = null;
            if (option.containsKey("effectiveness")) {
                scoreValue = option.get("effectiveness");
            } else if (option.containsKey("score")) {
                scoreValue = option.get("score");
            }

            if (scoreValue != null) {
                try {
                    double score = ((Number) scoreValue).doubleValue();
                    if (score > maxScore) {
                        maxScore = score;
                    }
                } catch (ClassCastException | NullPointerException e) {
                    // Ignore invalid values
                }
            }
        }

        // Return at least 1.0 to avoid division by zero
        return maxScore > 0 ? maxScore : 1.0;
    }

    // Mapping methods
    private TestSessionDto toDto(TestSession session) {
        long answered = answerRepository.countAnsweredBySessionId(session.getId());
        return new TestSessionDto(
                session.getId(),
                session.getTemplate().getId(),
                session.getTemplate().getName(),
                session.getClerkUserId(),
                session.getStatus(),
                session.getStartedAt(),
                session.getCompletedAt(),
                session.getCurrentQuestionIndex(),
                session.getTimeRemainingSeconds(),
                session.getQuestionOrder(),
                session.getQuestionOrder() != null ? session.getQuestionOrder().size() : 0,
                (int) answered,
                session.getLastActivityAt(),
                session.getCreatedAt()
        );
    }

    private TestSessionSummaryDto toSummaryDto(TestSession session) {
        long answered = 0;
        if (session.getQuestionOrder() != null && !session.getQuestionOrder().isEmpty()) {
            answered = answerRepository.countAnsweredBySessionId(session.getId());
        }
        return toSummaryDto(session, answered);
    }

    /**
     * Overloaded toSummaryDto that accepts pre-fetched answer count.
     * Use this when mapping multiple sessions to avoid N+1 queries.
     */
    private TestSessionSummaryDto toSummaryDto(TestSession session, long answeredCount) {
        int progress = 0;
        if (session.getQuestionOrder() != null && !session.getQuestionOrder().isEmpty()) {
            progress = (int) ((answeredCount * 100) / session.getQuestionOrder().size());
        }

        return new TestSessionSummaryDto(
                session.getId(),
                session.getTemplate().getId(),
                session.getTemplate().getName(),
                session.getStatus(),
                progress,
                session.getTimeRemainingSeconds(),
                session.getStartedAt(),
                session.getCompletedAt(),
                session.getCreatedAt()
        );
    }

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

    private AssessmentQuestionDto toQuestionDto(AssessmentQuestion question) {
        // Transform answer options based on question type
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
     * Transform answer options based on question type to match frontend expectations.
     * For SJT questions, maps backend storage format to frontend format:
     * - "action" field → "text" field (the option text)
     * - Generates deterministic "id" for each option based on index
     * - Preserves "effectiveness" as "score"
     * - Preserves "explanation" field
     *
     * @param options The raw answer options from database
     * @param questionType The type of question
     * @return Transformed options suitable for frontend consumption
     */
    private List<Map<String, Object>> transformAnswerOptions(
            List<Map<String, Object>> options,
            QuestionType questionType
    ) {
        if (options == null || options.isEmpty()) {
            return options;
        }

        // Transform SJT questions to match frontend expectations
        if (questionType == QuestionType.SJT || questionType == QuestionType.SITUATIONAL_JUDGMENT) {
            List<Map<String, Object>> transformedList = new ArrayList<>();
            for (int i = 0; i < options.size(); i++) {
                Map<String, Object> option = options.get(i);
                Map<String, Object> transformed = new HashMap<>();

                // Generate deterministic ID based on index (stable across fetches)
                // Use index-based ID to ensure consistency when user navigates back/forth
                transformed.put("id", "option-" + i);

                // Map "action" field to "text" field (frontend expects "text")
                // Also preserve "text" field if already present (for backwards compatibility)
                if (option.containsKey("action")) {
                    transformed.put("text", option.get("action"));
                } else if (option.containsKey("text")) {
                    transformed.put("text", option.get("text"));
                }

                // Preserve effectiveness as score
                // Also preserve "score" field if already present (for backwards compatibility)
                if (option.containsKey("effectiveness")) {
                    transformed.put("score", option.get("effectiveness"));
                } else if (option.containsKey("score")) {
                    transformed.put("score", option.get("score"));
                }

                // Preserve explanation for feedback
                if (option.containsKey("explanation")) {
                    transformed.put("explanation", option.get("explanation"));
                }

                // Map option letter to "label" field (frontend expects "label")
                // V5 data uses "label" field, V6 data uses "option" field
                if (option.containsKey("label")) {
                    transformed.put("label", option.get("label"));
                } else if (option.containsKey("option")) {
                    transformed.put("label", option.get("option"));
                }

                transformedList.add(transformed);
            }
            return transformedList;
        }

        // For other question types, ensure each option has an ID
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            Map<String, Object> option = options.get(i);
            if (!option.containsKey("id")) {
                Map<String, Object> withId = new HashMap<>(option);
                // Use deterministic index-based ID for consistency
                withId.put("id", "option-" + i);
                result.add(withId);
            } else {
                result.add(option);
            }
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public TemplateReadinessResponse checkTemplateReadiness(UUID templateId) {
        log.debug("Checking readiness for template: {}", templateId);

        TestTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template", templateId));

        List<UUID> competencyIds = template.getCompetencyIds();
        int questionsPerIndicator = template.getQuestionsPerIndicator();

        if (competencyIds == null || competencyIds.isEmpty()) {
            log.warn("Template {} has no competencies configured", templateId);
            return TemplateReadinessResponse.notReady(
                "Template has no competencies configured",
                List.of(),
                0,
                0
            );
        }

        // Use InventoryHeatmapService to check sufficiency
        var heatmap = inventoryHeatmapService.generateHeatmapFor(competencyIds);
        var sufficiency = inventoryHeatmapService.checkSufficiency(competencyIds, questionsPerIndicator);

        // Build per-competency readiness info
        List<CompetencyReadiness> competencyReadiness = new ArrayList<>();
        int totalQuestionsAvailable = 0;
        int totalQuestionsRequired = competencyIds.size() * questionsPerIndicator;
        boolean allReady = true;

        for (UUID compId : competencyIds) {
            // Get competency name
            String compName = competencyRepository.findById(compId)
                    .map(Competency::getName)
                    .orElse("Unknown Competency");

            // Get health status from heatmap
            HealthStatus health = heatmap.competencyHealth().getOrDefault(compId, HealthStatus.CRITICAL);

            // Get available questions count from heatmap
            int available = (int) heatmap.detailedCounts().entrySet().stream()
                    .filter(e -> e.getKey().startsWith(compId.toString()))
                    .mapToLong(Map.Entry::getValue)
                    .sum();

            totalQuestionsAvailable += available;

            // Get shortage from sufficiency check
            int shortage = sufficiency.getOrDefault(compId, 0);

            // Build issues list
            List<String> issues = new ArrayList<>();
            if (available == 0) {
                issues.add("No questions available for this competency");
                allReady = false;
            } else if (shortage > 0) {
                issues.add(String.format("Need %d more questions (have %d, need %d)",
                        shortage, available, questionsPerIndicator));
                allReady = false;
            }

            // Check for behavioral indicators
            List<BehavioralIndicator> indicators = indicatorRepository.findByCompetencyId(compId);
            if (indicators.isEmpty()) {
                issues.add("No behavioral indicators defined");
                allReady = false;
            }

            competencyReadiness.add(new CompetencyReadiness(
                    compId,
                    compName,
                    available,
                    questionsPerIndicator,
                    health.name(),
                    issues
            ));
        }

        if (allReady) {
            log.info("Template {} is ready: {} questions available for {} competencies",
                    templateId, totalQuestionsAvailable, competencyIds.size());
            return TemplateReadinessResponse.ready(
                    competencyReadiness,
                    totalQuestionsAvailable,
                    totalQuestionsRequired
            );
        } else {
            long criticalCount = competencyReadiness.stream()
                    .filter(cr -> "CRITICAL".equals(cr.healthStatus()))
                    .count();

            String message = criticalCount > 0
                    ? String.format("%d competenc%s missing questions",
                            criticalCount, criticalCount == 1 ? "y is" : "ies are")
                    : "Insufficient questions for some competencies";

            log.warn("Template {} is NOT ready: {}", templateId, message);
            return TemplateReadinessResponse.notReady(
                    message,
                    competencyReadiness,
                    totalQuestionsAvailable,
                    totalQuestionsRequired
            );
        }
    }

    @Override
    public Optional<AssemblyProgress> getAssemblyProgress(UUID templateId) {
        // Template ID is used as tracking ID during assembly
        return assemblyProgressTracker.getProgress(templateId);
    }
}
