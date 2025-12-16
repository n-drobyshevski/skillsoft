package app.skillsoft.assessmentbackend.services.impl;

import app.skillsoft.assessmentbackend.domain.dto.*;
import app.skillsoft.assessmentbackend.domain.dto.TemplateReadinessResponse.CompetencyReadiness;
import app.skillsoft.assessmentbackend.domain.dto.simulation.HealthStatus;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.exception.DuplicateSessionException;
import app.skillsoft.assessmentbackend.exception.ResourceNotFoundException;
import app.skillsoft.assessmentbackend.exception.TestNotReadyException;
import app.skillsoft.assessmentbackend.services.validation.InventoryHeatmapService;
import app.skillsoft.assessmentbackend.repository.*;
import app.skillsoft.assessmentbackend.services.TestSessionService;
import app.skillsoft.assessmentbackend.services.scoring.ScoringResult;
import app.skillsoft.assessmentbackend.services.scoring.ScoringStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class TestSessionServiceImpl implements TestSessionService {

    private static final Logger log = LoggerFactory.getLogger(TestSessionServiceImpl.class);

    private final TestSessionRepository sessionRepository;
    private final TestTemplateRepository templateRepository;
    private final TestAnswerRepository answerRepository;
    private final TestResultRepository resultRepository;
    private final AssessmentQuestionRepository questionRepository;
    private final BehavioralIndicatorRepository indicatorRepository;
    private final CompetencyRepository competencyRepository;
    private final InventoryHeatmapService inventoryHeatmapService;
    private final List<ScoringStrategy> scoringStrategies;

    public TestSessionServiceImpl(
            TestSessionRepository sessionRepository,
            TestTemplateRepository templateRepository,
            TestAnswerRepository answerRepository,
            TestResultRepository resultRepository,
            AssessmentQuestionRepository questionRepository,
            BehavioralIndicatorRepository indicatorRepository,
            CompetencyRepository competencyRepository,
            InventoryHeatmapService inventoryHeatmapService,
            List<ScoringStrategy> scoringStrategies) {
        this.sessionRepository = sessionRepository;
        this.templateRepository = templateRepository;
        this.answerRepository = answerRepository;
        this.resultRepository = resultRepository;
        this.questionRepository = questionRepository;
        this.indicatorRepository = indicatorRepository;
        this.competencyRepository = competencyRepository;
        this.inventoryHeatmapService = inventoryHeatmapService;
        this.scoringStrategies = scoringStrategies;
    }

    @Override
    @Transactional
    public TestSessionDto startSession(StartTestSessionRequest request) {
        // Verify template exists and is active
        TestTemplate template = templateRepository.findById(request.templateId())
                .orElseThrow(() -> new ResourceNotFoundException("Template", request.templateId()));

        if (!template.getIsActive()) {
            throw new IllegalStateException("Cannot start session for inactive template");
        }

        // Check if user already has an in-progress session for this template
        Optional<TestSession> existingSession = sessionRepository
                .findByClerkUserIdAndTemplate_IdAndStatus(request.clerkUserId(), request.templateId(), SessionStatus.IN_PROGRESS);

        if (existingSession.isPresent()) {
            TestSession existing = existingSession.get();
            log.info("User {} attempted to start new session for template {} but session {} is still in progress",
                    request.clerkUserId(), request.templateId(), existing.getId());
            throw new DuplicateSessionException(existing.getId(), request.templateId(), request.clerkUserId());
        }

        // Create new session
        TestSession session = new TestSession(template, request.clerkUserId());

        // Generate question order based on template configuration
        List<UUID> questionOrder = generateQuestionOrder(template);

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
        return toDto(saved);
    }

    @Override
    public Optional<TestSessionDto> findById(UUID sessionId) {
        return sessionRepository.findById(sessionId)
                .map(this::toDto);
    }

    @Override
    public Page<TestSessionSummaryDto> findByUser(String clerkUserId, Pageable pageable) {
        return sessionRepository.findByClerkUserId(clerkUserId, pageable)
                .map(this::toSummaryDto);
    }

    @Override
    public List<TestSessionSummaryDto> findByUserAndStatus(String clerkUserId, SessionStatus status) {
        return sessionRepository.findByClerkUserIdAndStatus(clerkUserId, status).stream()
                .map(this::toSummaryDto)
                .toList();
    }

    @Override
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
        return toAnswerDto(saved);
    }

    @Override
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

        return new CurrentQuestionDto(
                toQuestionDto(question),
                currentIndex,
                session.getQuestionOrder().size(),
                session.getTimeRemainingSeconds(),
                previousAnswer
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
            // Calculate results even for timed out sessions
            calculateAndSaveResult(session);
        }

        TestSession saved = sessionRepository.save(session);
        return toDto(saved);
    }

    @Override
    @Transactional
    public TestResultDto completeSession(UUID sessionId) {
        TestSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Cannot complete a session that is not in progress");
        }

        session.complete();
        sessionRepository.save(session);

        return calculateAndSaveResult(session);
    }

    @Override
    @Transactional
    public TestSessionDto abandonSession(UUID sessionId) {
        TestSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));

        session.abandon();
        TestSession saved = sessionRepository.save(session);
        return toDto(saved);
    }

    @Override
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
            calculateAndSaveResult(session);
            count++;
        }

        return count;
    }

    // Helper methods
    private List<UUID> generateQuestionOrder(TestTemplate template) {
        // Strategy Pattern: Select questions based on assessment goal
        return switch (template.getGoal()) {
            case OVERVIEW -> generateScenarioAOrder(template);
            case JOB_FIT -> generateScenarioBOrder(template);
            case TEAM_FIT -> generateScenarioCOrder(template);
        };
    }

    /**
     * Scenario A: Universal Baseline (Competency Passport)
     *
     * Strategy:
     * - Only UNIVERSAL context scope indicators (or NULL for backward compatibility)
     * - Only GENERAL tagged questions or questions without restrictive tags
     * - Flat distribution (no adaptive difficulty yet)
     *
     * This ensures construct validity by filtering out role-specific content,
     * measuring transferable soft skills suitable for reuse across job roles.
     *
     * DEFENSIVE FALLBACK CHAIN:
     * 1. Try UNIVERSAL + GENERAL questions (strict filter)
     * 2. Try ANY active questions for the competency (direct query)
     * 3. Log detailed diagnostics if still empty
     */
    private List<UUID> generateScenarioAOrder(TestTemplate template) {
        List<UUID> questionIds = new ArrayList<>();

        List<UUID> targetCompetencies = template.getCompetencyIds();
        int questionsPerComp = template.getQuestionsPerIndicator();

        log.debug("Starting Scenario A question generation for {} competencies, {} questions per indicator",
                  targetCompetencies.size(), questionsPerComp);

        for (UUID compId : targetCompetencies) {
            List<AssessmentQuestion> questions = new ArrayList<>();

            // STEP 1: Try strict UNIVERSAL + GENERAL filter (improved query handles NULL values)
            try {
                questions = questionRepository.findUniversalQuestions(compId, questionsPerComp);
                log.debug("Found {} UNIVERSAL+GENERAL questions for competency {}", questions.size(), compId);
            } catch (Exception e) {
                log.warn("Error executing findUniversalQuestions for competency {}: {}", compId, e.getMessage());
            }

            // STEP 2: Fallback to ANY active questions if strict filter returns empty
            if (questions.isEmpty()) {
                log.warn("No UNIVERSAL+GENERAL questions for competency {}. Trying fallback query.", compId);

                try {
                    questions = questionRepository.findAnyActiveQuestionsForCompetency(compId, questionsPerComp);
                    if (!questions.isEmpty()) {
                        log.info("Fallback query found {} active questions for competency {}", questions.size(), compId);
                    }
                } catch (Exception e) {
                    log.warn("Error executing fallback query for competency {}: {}", compId, e.getMessage());
                }
            }

            // STEP 3: If still empty, log diagnostics
            if (questions.isEmpty()) {
                logQuestionDiagnostics(compId);
            }

            questionIds.addAll(questions.stream()
                .map(AssessmentQuestion::getId)
                .toList());
        }

        // Shuffle to prevent clustering by competency
        if (Boolean.TRUE.equals(template.getShuffleQuestions())) {
            Collections.shuffle(questionIds);
            log.debug("Questions shuffled");
        }

        if (questionIds.isEmpty()) {
            log.error("CRITICAL: Generated empty question order for template {} (Scenario A). " +
                    "This will cause session failures. Check competency configuration and question availability. " +
                    "Competencies: {}, QuestionsPerIndicator: {}",
                    template.getId(), targetCompetencies, questionsPerComp);
        }

        log.info("Generated Scenario A question order: {} questions from {} competencies",
                 questionIds.size(), targetCompetencies.size());

        return questionIds;
    }

    /**
     * Log detailed diagnostics for question availability issues.
     * Helps identify whether the problem is missing questions, inactive questions,
     * or missing tags/context_scope configuration.
     */
    private void logQuestionDiagnostics(UUID competencyId) {
        try {
            // Get competency name for better logging
            String compName = competencyRepository.findById(competencyId)
                    .map(Competency::getName)
                    .orElse("Unknown");

            // Count behavioral indicators
            List<BehavioralIndicator> indicators = indicatorRepository.findByCompetencyId(competencyId);
            int indicatorCount = indicators.size();

            // Count questions directly
            long activeCount = questionRepository.countActiveQuestionsForCompetency(competencyId);

            // Log diagnostic summary
            log.error("DIAGNOSTIC for competency '{}' ({}): " +
                      "indicators={}, activeQuestions={}. " +
                      "Check: 1) Are questions linked to behavioral indicators? " +
                      "2) Are questions marked as active (is_active=true)? " +
                      "3) Do behavioral indicators exist for this competency?",
                    compName, competencyId, indicatorCount, activeCount);

            // Log indicator details if we have them but no questions
            if (indicatorCount > 0 && activeCount == 0) {
                for (BehavioralIndicator ind : indicators) {
                    long indQuestionCount = questionRepository.findByBehavioralIndicator_Id(ind.getId()).size();
                    log.error("  - Indicator '{}' ({}): {} total questions, contextScope={}",
                            ind.getTitle(), ind.getId(), indQuestionCount, ind.getContextScope());
                }
            }
        } catch (Exception e) {
            log.error("Error generating diagnostics for competency {}: {}", competencyId, e.getMessage());
        }
    }

    /**
     * Scenario B: Job Fit Assessment
     * 
     * Future implementation:
     * - Filter by O*NET SOC code requirements
     * - Apply targeted context scopes (PROFESSIONAL, TECHNICAL)
     * - Use role-specific tags
     * - Implement adaptive difficulty based on passport baseline
     */
    private List<UUID> generateScenarioBOrder(TestTemplate template) {
        log.warn("Scenario B (Job Fit) question selection not yet implemented. Falling back to legacy logic.");
        return generateLegacyQuestionOrder(template);
    }

    /**
     * Scenario C: Team Fit Analysis
     * 
     * Future implementation:
     * - ESCO skill normalization
     * - Team gap analysis
     * - Personality compatibility checks
     */
    private List<UUID> generateScenarioCOrder(TestTemplate template) {
        log.warn("Scenario C (Team Fit) question selection not yet implemented. Falling back to legacy logic.");
        return generateLegacyQuestionOrder(template);
    }

    /**
     * Legacy question selection logic (pre-Smart Assessment).
     * Used as fallback for scenarios B and C until fully implemented.
     */
    private List<UUID> generateLegacyQuestionOrder(TestTemplate template) {
        List<UUID> questionIds = new ArrayList<>();

        log.debug("Starting legacy question generation for {} competencies",
                  template.getCompetencyIds().size());

        // For each competency in the template
        for (UUID competencyId : template.getCompetencyIds()) {
            // Get behavioral indicators for this competency
            List<BehavioralIndicator> indicators = indicatorRepository.findByCompetencyId(competencyId);
            log.debug("Competency {} has {} behavioral indicators", competencyId, indicators.size());

            int questionsAddedForComp = 0;

            for (BehavioralIndicator indicator : indicators) {
                // Get active questions for this indicator
                List<AssessmentQuestion> questions = questionRepository
                        .findByBehavioralIndicator_Id(indicator.getId()).stream()
                        .filter(AssessmentQuestion::isActive)
                        .toList();

                log.debug("Indicator {} has {} active questions", indicator.getId(), questions.size());

                // Shuffle if required
                List<AssessmentQuestion> selectedQuestions = new ArrayList<>(questions);
                if (template.getShuffleQuestions()) {
                    Collections.shuffle(selectedQuestions);
                }

                // Take only the configured number of questions per indicator
                int limit = Math.min(template.getQuestionsPerIndicator(), selectedQuestions.size());
                for (int i = 0; i < limit; i++) {
                    questionIds.add(selectedQuestions.get(i).getId());
                    questionsAddedForComp++;
                }
            }

            log.debug("Added {} questions for competency {}", questionsAddedForComp, competencyId);

            if (questionsAddedForComp == 0) {
                log.warn("No questions added for competency {}. This competency has no active questions.", competencyId);
            }
        }

        // Final shuffle of all questions if configured
        if (template.getShuffleQuestions()) {
            Collections.shuffle(questionIds);
            log.debug("Final shuffle applied to {} questions", questionIds.size());
        }

        log.info("Generated legacy question order: {} questions from {} competencies",
                 questionIds.size(), template.getCompetencyIds().size());

        return questionIds;
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

    private TestResultDto calculateAndSaveResult(TestSession session) {
        List<TestAnswer> answers = answerRepository.findBySession_Id(session.getId());

        // Calculate statistics
        long answered = answers.stream().filter(a -> !a.getIsSkipped() && a.getAnsweredAt() != null).count();
        long skipped = answers.stream().filter(TestAnswer::getIsSkipped).count();
        int totalTime = answers.stream()
                .mapToInt(a -> a.getTimeSpentSeconds() != null ? a.getTimeSpentSeconds() : 0)
                .sum();

        // Get template goal for strategy selection
        AssessmentGoal goal = session.getTemplate().getGoal();
        
        // Find appropriate scoring strategy
        ScoringStrategy strategy = scoringStrategies.stream()
                .filter(s -> s.getSupportedGoal() == goal)
                .findFirst()
                .orElse(null);
        
        ScoringResult scoringResult;
        if (strategy != null) {
            log.info("Using {} strategy for goal: {}", strategy.getClass().getSimpleName(), goal);
            scoringResult = strategy.calculate(session, answers);
        } else {
            log.warn("No scoring strategy found for goal: {}, using legacy calculation", goal);
            // Fallback to legacy scoring
            scoringResult = calculateLegacyScore(session, answers);
        }

        // Create result entity
        TestResult result = new TestResult(session, session.getClerkUserId());
        result.setOverallScore(scoringResult.getOverallScore());
        result.setOverallPercentage(scoringResult.getOverallPercentage());
        result.setCompetencyScores(scoringResult.getCompetencyScores());
        result.setQuestionsAnswered((int) answered);
        result.setQuestionsSkipped((int) skipped);
        result.setTotalTimeSeconds(totalTime);
        result.setCompletedAt(LocalDateTime.now());

        // Calculate passed/failed
        result.calculatePassed(session.getTemplate().getPassingScore());

        // Calculate percentile based on historical results for this template
        Integer percentile = calculatePercentile(session.getTemplate().getId(), result.getOverallPercentage());
        result.setPercentile(percentile);

        TestResult saved = resultRepository.save(result);
        return toResultDto(saved, session);
    }
    
    /**
     * Calculate the percentile rank for a given score based on historical results.
     * Percentile indicates what percentage of test takers scored below this score.
     *
     * @param templateId The template ID to compare against
     * @param score The current score to calculate percentile for
     * @return Percentile rank (0-100), or 50 if no historical data exists
     */
    private Integer calculatePercentile(UUID templateId, Double score) {
        if (score == null) {
            log.debug("Cannot calculate percentile: score is null");
            return null;
        }

        try {
            // Get count of results below this score
            long belowCount = resultRepository.countResultsBelowScore(templateId, score);

            // Get total count of results for this template
            long totalCount = resultRepository.countResultsByTemplateId(templateId);

            // Handle edge cases
            if (totalCount == 0) {
                // First result for this template - default to 50th percentile
                log.debug("First result for template {}, defaulting to 50th percentile", templateId);
                return 50;
            }

            if (totalCount == 1) {
                // Only one result (the current one being saved)
                // Return 50 as baseline
                log.debug("Only one result for template {}, defaulting to 50th percentile", templateId);
                return 50;
            }

            // Calculate percentile: (count below / total) * 100
            // Using (totalCount - 1) to exclude the current result from denominator
            double percentile = ((double) belowCount / (totalCount - 1)) * 100;

            // Round and clamp to 0-100 range
            int result = (int) Math.round(percentile);
            result = Math.max(0, Math.min(100, result));

            log.debug("Calculated percentile for template {}: {} (below: {}, total: {})",
                    templateId, result, belowCount, totalCount);

            return result;
        } catch (Exception e) {
            log.warn("Error calculating percentile for template {}: {}", templateId, e.getMessage());
            return null;
        }
    }

    /**
     * Legacy scoring calculation for backward compatibility.
     * Used when no specific strategy is available for the goal.
     * Calculates basic competency scores by grouping answers by their question's competency.
     */
    private ScoringResult calculateLegacyScore(TestSession session, List<TestAnswer> answers) {
        Double totalScore = answerRepository.sumScoreBySessionId(session.getId());
        Double maxScore = answerRepository.sumMaxScoreBySessionId(session.getId());

        double percentage = 0.0;
        if (maxScore != null && maxScore > 0) {
            percentage = (totalScore != null ? totalScore / maxScore : 0) * 100;
        }

        ScoringResult result = new ScoringResult();
        result.setOverallScore(totalScore != null ? totalScore : 0.0);
        result.setOverallPercentage(percentage);
        result.setGoal(session.getTemplate().getGoal());

        // Calculate basic competency scores even in legacy mode
        List<CompetencyScoreDto> competencyScores = calculateLegacyCompetencyScores(answers);
        result.setCompetencyScores(competencyScores);

        return result;
    }

    /**
     * Calculate competency scores for legacy scoring mode.
     * Groups answers by their question's behavioral indicator's competency and calculates averages.
     *
     * @param answers List of test answers
     * @return List of competency scores
     */
    private List<CompetencyScoreDto> calculateLegacyCompetencyScores(List<TestAnswer> answers) {
        // Map to track scores per competency: competencyId -> (totalScore, count, maxScore)
        Map<UUID, double[]> competencyAggregates = new HashMap<>();
        Map<UUID, String> competencyNames = new HashMap<>();
        Map<UUID, String> competencyOnetCodes = new HashMap<>();

        for (TestAnswer answer : answers) {
            // Skip unanswered questions
            if (answer.getIsSkipped() || answer.getScore() == null) {
                continue;
            }

            AssessmentQuestion question = answer.getQuestion();
            if (question == null) {
                continue;
            }

            BehavioralIndicator indicator = question.getBehavioralIndicator();
            if (indicator == null) {
                continue;
            }

            Competency competency = indicator.getCompetency();
            if (competency == null) {
                continue;
            }

            UUID compId = competency.getId();

            // Initialize aggregates if first encounter
            if (!competencyAggregates.containsKey(compId)) {
                competencyAggregates.put(compId, new double[]{0.0, 0.0, 0.0}); // totalScore, count, maxScore
                competencyNames.put(compId, competency.getName());
                // Get O*NET code if available
                if (competency.getOnetCode() != null) {
                    competencyOnetCodes.put(compId, competency.getOnetCode());
                }
            }

            // Aggregate scores
            double[] aggregate = competencyAggregates.get(compId);
            aggregate[0] += answer.getScore() != null ? answer.getScore() : 0.0;
            aggregate[1] += 1.0;
            aggregate[2] += answer.getMaxScore() != null ? answer.getMaxScore() : 1.0;
        }

        // Convert aggregates to CompetencyScoreDto
        List<CompetencyScoreDto> competencyScores = new ArrayList<>();
        for (Map.Entry<UUID, double[]> entry : competencyAggregates.entrySet()) {
            UUID compId = entry.getKey();
            double[] aggregate = entry.getValue();
            double totalScore = aggregate[0];
            int count = (int) aggregate[1];
            double maxScore = aggregate[2];

            double compPercentage = maxScore > 0 ? (totalScore / maxScore) * 100 : 0.0;

            CompetencyScoreDto dto = new CompetencyScoreDto(
                    compId,
                    competencyNames.get(compId),
                    totalScore,
                    maxScore,
                    compPercentage
            );
            dto.setQuestionsAnswered(count);
            dto.setOnetCode(competencyOnetCodes.get(compId));

            competencyScores.add(dto);
        }

        log.debug("Calculated {} competency scores in legacy mode", competencyScores.size());
        return competencyScores;
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
        int progress = 0;
        if (session.getQuestionOrder() != null && !session.getQuestionOrder().isEmpty()) {
            long answered = answerRepository.countAnsweredBySessionId(session.getId());
            progress = (int) ((answered * 100) / session.getQuestionOrder().size());
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

    private TestResultDto toResultDto(TestResult result, TestSession session) {
        return new TestResultDto(
                result.getId(),
                result.getSessionId(),
                session.getTemplate().getId(),
                session.getTemplate().getName(),
                result.getClerkUserId(),
                result.getOverallScore(),
                result.getOverallPercentage(),
                result.getPercentile(),
                result.getPassed(),
                result.getCompetencyScores(),
                result.getTotalTimeSeconds(),
                result.getQuestionsAnswered(),
                result.getQuestionsSkipped(),
                session.getQuestionOrder() != null ? session.getQuestionOrder().size() : 0,
                result.getCompletedAt()
        );
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
}
