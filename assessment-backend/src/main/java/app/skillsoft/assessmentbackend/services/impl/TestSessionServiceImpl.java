package app.skillsoft.assessmentbackend.services.impl;

import app.skillsoft.assessmentbackend.domain.dto.*;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.repository.*;
import app.skillsoft.assessmentbackend.services.TestSessionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class TestSessionServiceImpl implements TestSessionService {

    private final TestSessionRepository sessionRepository;
    private final TestTemplateRepository templateRepository;
    private final TestAnswerRepository answerRepository;
    private final TestResultRepository resultRepository;
    private final AssessmentQuestionRepository questionRepository;
    private final BehavioralIndicatorRepository indicatorRepository;

    public TestSessionServiceImpl(
            TestSessionRepository sessionRepository,
            TestTemplateRepository templateRepository,
            TestAnswerRepository answerRepository,
            TestResultRepository resultRepository,
            AssessmentQuestionRepository questionRepository,
            BehavioralIndicatorRepository indicatorRepository) {
        this.sessionRepository = sessionRepository;
        this.templateRepository = templateRepository;
        this.answerRepository = answerRepository;
        this.resultRepository = resultRepository;
        this.questionRepository = questionRepository;
        this.indicatorRepository = indicatorRepository;
    }

    @Override
    @Transactional
    public TestSessionDto startSession(StartTestSessionRequest request) {
        // Verify template exists and is active
        TestTemplate template = templateRepository.findById(request.templateId())
                .orElseThrow(() -> new RuntimeException("Template not found with id: " + request.templateId()));
        
        if (!template.getIsActive()) {
            throw new IllegalStateException("Cannot start session for inactive template");
        }

        // Check if user already has an in-progress session for this template
        Optional<TestSession> existingSession = sessionRepository
                .findByClerkUserIdAndTemplate_IdAndStatus(request.clerkUserId(), request.templateId(), SessionStatus.IN_PROGRESS);
        
        if (existingSession.isPresent()) {
            throw new IllegalStateException("User already has an in-progress session for this template");
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
                .orElseThrow(() -> new RuntimeException("Session not found with id: " + request.sessionId()));

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Cannot submit answer for a session that is not in progress");
        }

        AssessmentQuestion question = questionRepository.findById(request.questionId())
                .orElseThrow(() -> new RuntimeException("Question not found with id: " + request.questionId()));

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
        TestSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found with id: " + sessionId));

        if (session.getQuestionOrder() == null || session.getQuestionOrder().isEmpty()) {
            throw new IllegalStateException("Session has no questions");
        }

        int currentIndex = session.getCurrentQuestionIndex();
        if (currentIndex >= session.getQuestionOrder().size()) {
            throw new IllegalStateException("No more questions in session");
        }

        UUID questionId = session.getQuestionOrder().get(currentIndex);
        AssessmentQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found with id: " + questionId));

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
                .orElseThrow(() -> new RuntimeException("Session not found with id: " + sessionId));

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
                .orElseThrow(() -> new RuntimeException("Session not found with id: " + sessionId));

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
                .orElseThrow(() -> new RuntimeException("Session not found with id: " + sessionId));

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
                .orElseThrow(() -> new RuntimeException("Session not found with id: " + sessionId));

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
        List<UUID> questionIds = new ArrayList<>();

        // For each competency in the template
        for (UUID competencyId : template.getCompetencyIds()) {
            // Get behavioral indicators for this competency
            List<BehavioralIndicator> indicators = indicatorRepository.findByCompetency_Id(competencyId);

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
            case SINGLE_CHOICE:
                answer.setSelectedOptionIds(request.selectedOptionIds());
                break;
            case MULTIPLE_CHOICE:
                answer.setSelectedOptionIds(request.selectedOptionIds());
                break;
            case LIKERT_SCALE:
                answer.setLikertValue(request.likertValue());
                break;
            case RANKING:
                answer.setRankingOrder(request.rankingOrder());
                break;
            case OPEN_ENDED:
                answer.setTextResponse(request.textResponse());
                break;
            case SITUATIONAL:
                answer.setSelectedOptionIds(request.selectedOptionIds());
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

        // Calculate scores (simplified - TODO: implement proper scoring)
        Double totalScore = answerRepository.sumScoreBySessionId(session.getId());
        Double maxScore = answerRepository.sumMaxScoreBySessionId(session.getId());
        
        double percentage = 0.0;
        if (maxScore != null && maxScore > 0) {
            percentage = (totalScore != null ? totalScore / maxScore : 0) * 100;
        }

        // Create result
        TestResult result = new TestResult(session, session.getClerkUserId());
        result.setOverallScore(totalScore != null ? totalScore : 0.0);
        result.setOverallPercentage(percentage);
        result.setQuestionsAnswered((int) answered);
        result.setQuestionsSkipped((int) skipped);
        result.setTotalTimeSeconds(totalTime);
        result.setCompletedAt(LocalDateTime.now());

        // Calculate passed/failed
        result.calculatePassed(session.getTemplate().getPassingScore());

        // TODO: Calculate competency scores and percentile

        TestResult saved = resultRepository.save(result);
        return toResultDto(saved, session);
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
