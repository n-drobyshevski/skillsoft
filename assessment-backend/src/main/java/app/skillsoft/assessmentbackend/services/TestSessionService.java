package app.skillsoft.assessmentbackend.services;

import app.skillsoft.assessmentbackend.domain.dto.*;
import app.skillsoft.assessmentbackend.domain.entities.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for managing test sessions.
 */
public interface TestSessionService {

    /**
     * Start a new test session for a user.
     */
    TestSessionDto startSession(StartTestSessionRequest request);

    /**
     * Get a test session by ID.
     */
    Optional<TestSessionDto> findById(UUID sessionId);

    /**
     * Get all sessions for a user with pagination.
     */
    Page<TestSessionSummaryDto> findByUser(String clerkUserId, Pageable pageable);

    /**
     * Get sessions for a user filtered by status.
     */
    List<TestSessionSummaryDto> findByUserAndStatus(String clerkUserId, SessionStatus status);

    /**
     * Find an in-progress session for a user on a specific template.
     * Useful for resuming tests.
     */
    Optional<TestSessionDto> findInProgressSession(String clerkUserId, UUID templateId);

    /**
     * Submit an answer for a question.
     */
    TestAnswerDto submitAnswer(SubmitAnswerRequest request);

    /**
     * Get the current question for a session.
     */
    CurrentQuestionDto getCurrentQuestion(UUID sessionId);

    /**
     * Navigate to a specific question in the session.
     */
    TestSessionDto navigateToQuestion(UUID sessionId, int questionIndex);

    /**
     * Update the remaining time for a session.
     */
    TestSessionDto updateTimeRemaining(UUID sessionId, int timeRemainingSeconds);

    /**
     * Complete a test session and calculate results.
     */
    TestResultDto completeSession(UUID sessionId);

    /**
     * Abandon a test session.
     */
    TestSessionDto abandonSession(UUID sessionId);

    /**
     * Get all answers for a session.
     */
    List<TestAnswerDto> getSessionAnswers(UUID sessionId);

    /**
     * Timeout stale sessions (batch job).
     */
    int timeoutStaleSessions();

    /**
     * Check if a template is ready to start a test session.
     * Validates that all competencies have sufficient questions.
     *
     * @param templateId The template to check
     * @return Readiness response with per-competency status
     */
    TemplateReadinessResponse checkTemplateReadiness(UUID templateId);

    /**
     * DTO for current question with context.
     */
    record CurrentQuestionDto(
            AssessmentQuestionDto question,
            int questionIndex,
            int totalQuestions,
            Integer timeRemainingSeconds,
            TestAnswerDto previousAnswer
    ) {}
}
