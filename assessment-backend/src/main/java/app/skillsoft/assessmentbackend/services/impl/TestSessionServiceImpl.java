package app.skillsoft.assessmentbackend.services.impl;

import app.skillsoft.assessmentbackend.domain.dto.*;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.exception.DuplicateSessionException;
import app.skillsoft.assessmentbackend.exception.ResourceNotFoundException;
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
    private final List<ScoringStrategy> scoringStrategies;

    public TestSessionServiceImpl(
            TestSessionRepository sessionRepository,
            TestTemplateRepository templateRepository,
            TestAnswerRepository answerRepository,
            TestResultRepository resultRepository,
            AssessmentQuestionRepository questionRepository,
            BehavioralIndicatorRepository indicatorRepository,
            List<ScoringStrategy> scoringStrategies) {
        this.sessionRepository = sessionRepository;
        this.templateRepository = templateRepository;
        this.answerRepository = answerRepository;
        this.resultRepository = resultRepository;
        this.questionRepository = questionRepository;
        this.indicatorRepository = indicatorRepository;
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
     * - Only UNIVERSAL context scope indicators
     * - Only GENERAL tagged questions (context-neutral)
     * - Flat distribution (no adaptive difficulty yet)
     *
     * This ensures construct validity by filtering out role-specific content,
     * measuring transferable soft skills suitable for reuse across job roles.
     *
     * DEFENSIVE FALLBACK: If no UNIVERSAL questions found, falls back to ANY active questions
     * to prevent empty questionOrder list which causes 500 errors.
     */
    private List<UUID> generateScenarioAOrder(TestTemplate template) {
        List<UUID> questionIds = new ArrayList<>();

        // Extract competencies from template (simplified - assumes competencyIds field)
        List<UUID> targetCompetencies = template.getCompetencyIds();
        int questionsPerComp = template.getQuestionsPerIndicator();

        for (UUID compId : targetCompetencies) {
            // Use Smart Assessment repository method
            List<AssessmentQuestion> questions = questionRepository
                .findUniversalQuestions(compId, questionsPerComp);

            if (questions.isEmpty()) {
                log.warn("No UNIVERSAL + GENERAL questions found for competency ID: {}. " +
                         "Falling back to ANY active questions for this competency. " +
                         "Ensure behavioral indicators have context_scope='UNIVERSAL' and " +
                         "questions have 'GENERAL' tag in metadata for proper Scenario A assessments.", compId);

                // DEFENSIVE FALLBACK: Get ANY active questions for this competency
                List<BehavioralIndicator> indicators = indicatorRepository.findByCompetencyId(compId);
                for (BehavioralIndicator indicator : indicators) {
                    List<AssessmentQuestion> fallbackQuestions = questionRepository
                            .findByBehavioralIndicator_Id(indicator.getId()).stream()
                            .filter(AssessmentQuestion::isActive)
                            .limit(questionsPerComp)
                            .toList();

                    questions.addAll(fallbackQuestions);

                    if (questions.size() >= questionsPerComp) {
                        break; // Got enough questions
                    }
                }

                if (questions.isEmpty()) {
                    log.error("CRITICAL: No questions at all found for competency {}. " +
                            "Session will have incomplete question set.", compId);
                }
            }

            questionIds.addAll(questions.stream()
                .map(AssessmentQuestion::getId)
                .toList());
        }

        // Shuffle to prevent clustering by competency
        if (Boolean.TRUE.equals(template.getShuffleQuestions())) {
            Collections.shuffle(questionIds);
        }

        if (questionIds.isEmpty()) {
            log.error("CRITICAL: Generated empty question order for template {} (Scenario A). " +
                    "This will cause session failures. Check competency configuration and question availability.",
                    template.getId());
        }

        log.info("Generated Scenario A question order: {} questions from {} competencies",
                 questionIds.size(), targetCompetencies.size());

        return questionIds;
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

        // For each competency in the template
        for (UUID competencyId : template.getCompetencyIds()) {
            // Get behavioral indicators for this competency
            List<BehavioralIndicator> indicators = indicatorRepository.findByCompetencyId(competencyId);

            for (BehavioralIndicator indicator : indicators) {
                // Get active questions for this indicator
                List<AssessmentQuestion> questions = questionRepository
                        .findByBehavioralIndicator_Id(indicator.getId()).stream()
                        .filter(AssessmentQuestion::isActive)
                        .toList();

                // Shuffle if required
                List<AssessmentQuestion> selectedQuestions = new ArrayList<>(questions);
                if (template.getShuffleQuestions()) {
                    Collections.shuffle(selectedQuestions);
                }

                // Take only the configured number of questions per indicator
                int limit = Math.min(template.getQuestionsPerIndicator(), selectedQuestions.size());
                for (int i = 0; i < limit; i++) {
                    questionIds.add(selectedQuestions.get(i).getId());
                }
            }
        }

        // Final shuffle of all questions if configured
        if (template.getShuffleQuestions()) {
            Collections.shuffle(questionIds);
        }

        return questionIds;
    }

    private void updateAnswer(TestAnswer answer, SubmitAnswerRequest request, AssessmentQuestion question) {
        answer.setTimeSpentSeconds(request.timeSpentSeconds());
        answer.setAnsweredAt(LocalDateTime.now());
        answer.setIsSkipped(false);

        // Set the appropriate answer field based on question type
        switch (question.getQuestionType()) {
            case MULTIPLE_CHOICE:
            case SITUATIONAL_JUDGMENT:
                answer.setSelectedOptionIds(request.selectedOptionIds());
                break;
            case LIKERT_SCALE:
            case FREQUENCY_SCALE:
                answer.setLikertValue(request.likertValue());
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
                } else if (request.textResponse() != null) {
                    answer.setTextResponse(request.textResponse());
                }
                break;
        }

        // TODO: Calculate score based on scoring rubric
        // For now, set max score but leave score null until grading
        answer.setMaxScore(1.0); // Default max score
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

        // TODO: Calculate percentile

        TestResult saved = resultRepository.save(result);
        return toResultDto(saved, session);
    }
    
    /**
     * Legacy scoring calculation for backward compatibility.
     * Used when no specific strategy is available for the goal.
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
        result.setCompetencyScores(new ArrayList<>()); // Empty for legacy
        
        return result;
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
        return new AssessmentQuestionDto(
                question.getId(),
                question.getBehavioralIndicatorId(),
                question.getQuestionText(),
                question.getQuestionType(),
                question.getAnswerOptions(),
                question.getScoringRubric(),
                question.getTimeLimit(),
                question.getDifficultyLevel(),
                question.getMetadata(),
                question.isActive(),
                question.getOrderIndex()
        );
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
}
