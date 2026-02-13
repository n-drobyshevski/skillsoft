package app.skillsoft.assessmentbackend.services.impl;

import app.skillsoft.assessmentbackend.domain.dto.activity.ActivityFilterParams;
import app.skillsoft.assessmentbackend.domain.dto.activity.TemplateActivityStatsDto;
import app.skillsoft.assessmentbackend.domain.dto.activity.TestActivityDto;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.domain.projections.TemplateActivityStatsProjection;
import app.skillsoft.assessmentbackend.domain.projections.TemplateScoreTimeProjection;
import app.skillsoft.assessmentbackend.repository.TestActivityEventRepository;
import app.skillsoft.assessmentbackend.repository.TestResultRepository;
import app.skillsoft.assessmentbackend.repository.TestSessionRepository;
import app.skillsoft.assessmentbackend.repository.TestTemplateRepository;
import app.skillsoft.assessmentbackend.repository.UserRepository;
import app.skillsoft.assessmentbackend.services.ActivityTrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of ActivityTrackingService.
 * Handles activity tracking for dashboard widgets and template activity pages.
 */
@Service
public class ActivityTrackingServiceImpl implements ActivityTrackingService {

    private static final Logger logger = LoggerFactory.getLogger(ActivityTrackingServiceImpl.class);

    private static final List<SessionStatus> TERMINAL_STATUSES = List.of(
            SessionStatus.COMPLETED,
            SessionStatus.ABANDONED,
            SessionStatus.TIMED_OUT
    );

    private final TestSessionRepository sessionRepository;
    private final TestResultRepository resultRepository;
    private final TestTemplateRepository templateRepository;
    private final UserRepository userRepository;
    private final TestActivityEventRepository eventRepository;

    public ActivityTrackingServiceImpl(
            TestSessionRepository sessionRepository,
            TestResultRepository resultRepository,
            TestTemplateRepository templateRepository,
            UserRepository userRepository,
            TestActivityEventRepository eventRepository) {
        this.sessionRepository = sessionRepository;
        this.resultRepository = resultRepository;
        this.templateRepository = templateRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestActivityDto> getRecentActivity(int limit) {
        logger.debug("Fetching {} recent activities for dashboard", limit);

        // Get recent completed/abandoned/timed-out sessions with template
        List<TestSession> sessions = sessionRepository.findRecentCompletedSessions(TERMINAL_STATUSES, limit);

        if (sessions.isEmpty()) {
            logger.debug("No recent activity found");
            return List.of();
        }

        // Batch fetch users to avoid N+1
        Set<String> clerkUserIds = sessions.stream()
                .map(TestSession::getClerkUserId)
                .collect(Collectors.toSet());
        Map<String, User> userMap = userRepository.findByClerkIdIn(clerkUserIds).stream()
                .collect(Collectors.toMap(User::getClerkId, u -> u));

        // Batch fetch results for completed sessions
        Set<UUID> sessionIds = sessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.COMPLETED)
                .map(TestSession::getId)
                .collect(Collectors.toSet());
        Map<UUID, TestResult> resultMap = new HashMap<>();
        if (!sessionIds.isEmpty()) {
            resultRepository.findAllById(sessionIds).forEach(r -> {
                if (r.getSession() != null) {
                    resultMap.put(r.getSession().getId(), r);
                }
            });
        }

        // Map to DTOs with user enrichment
        return sessions.stream()
                .map(session -> mapToActivityDto(session, userMap.get(session.getClerkUserId()),
                        resultMap.get(session.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TestActivityDto> getTemplateActivity(UUID templateId, ActivityFilterParams params) {
        logger.debug("Fetching activity for template {} with params {}", templateId, params);

        Pageable pageable = PageRequest.of(params.getValidatedPage(), params.getValidatedSize());

        // Get sessions based on filters
        Page<TestSession> sessions;
        if (params.status() != null) {
            sessions = sessionRepository.findActivityByTemplateIdAndStatus(templateId, params.status(), pageable);
        } else {
            sessions = sessionRepository.findActivityByTemplateId(templateId, TERMINAL_STATUSES, pageable);
        }

        if (sessions.isEmpty()) {
            return Page.empty();
        }

        // Batch fetch users
        Set<String> clerkUserIds = sessions.stream()
                .map(TestSession::getClerkUserId)
                .collect(Collectors.toSet());
        Map<String, User> userMap = userRepository.findByClerkIdIn(clerkUserIds).stream()
                .collect(Collectors.toMap(User::getClerkId, u -> u));

        // Batch fetch results for completed sessions
        Set<UUID> sessionIds = sessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.COMPLETED)
                .map(TestSession::getId)
                .collect(Collectors.toSet());
        Map<UUID, TestResult> resultMap = new HashMap<>();
        if (!sessionIds.isEmpty()) {
            resultRepository.findAllById(sessionIds).forEach(r -> {
                if (r.getSession() != null) {
                    resultMap.put(r.getSession().getId(), r);
                }
            });
        }

        // Map to DTOs with optional passed filter
        return sessions.map(session -> {
            TestResult result = resultMap.get(session.getId());
            TestActivityDto dto = mapToActivityDto(session, userMap.get(session.getClerkUserId()), result);

            // Apply passed filter if specified
            if (params.passed() != null && result != null) {
                if (!params.passed().equals(result.getPassed())) {
                    return null;
                }
            }
            return dto;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public TemplateActivityStatsDto getTemplateActivityStats(UUID templateId) {
        logger.debug("Fetching activity stats for template {}", templateId);

        // Get template info
        TestTemplate template = templateRepository.findById(templateId)
                .orElse(null);
        if (template == null) {
            logger.warn("Template {} not found", templateId);
            return null;
        }

        // Get aggregate stats from sessions using type-safe projection
        TemplateActivityStatsProjection sessionStats = sessionRepository.getTemplateActivityStats(templateId);
        long totalSessions = sessionStats.getTotalSessions() != null ? sessionStats.getTotalSessions() : 0L;
        long completedCount = sessionStats.getCompletedCount() != null ? sessionStats.getCompletedCount() : 0L;
        long abandonedCount = sessionStats.getAbandonedCount() != null ? sessionStats.getAbandonedCount() : 0L;
        long timedOutCount = sessionStats.getTimedOutCount() != null ? sessionStats.getTimedOutCount() : 0L;
        LocalDateTime lastActivity = sessionStats.getLastActivity();

        // Get passed count and score aggregates from results using type-safe projection
        long passedCount = sessionRepository.countPassedSessionsByTemplateId(templateId);
        TemplateScoreTimeProjection scoreAggregates = sessionRepository.getTemplateScoreAndTimeAggregates(templateId);
        Double totalScore = scoreAggregates.getTotalScore() != null ? scoreAggregates.getTotalScore() : 0.0;
        double totalTime = scoreAggregates.getTotalTimeSeconds() != null ? scoreAggregates.getTotalTimeSeconds() : 0.0;

        return TemplateActivityStatsDto.fromCounts(
                templateId,
                template.getName(),
                template.getGoal(),
                totalSessions,
                completedCount,
                abandonedCount,
                timedOutCount,
                passedCount,
                totalScore,
                totalTime,
                lastActivity
        );
    }

    @Override
    @Transactional
    public void recordSessionStarted(TestSession session) {
        if (eventRepository.existsBySessionIdAndEventType(session.getId(), ActivityEventType.SESSION_STARTED)) {
            logger.debug("SESSION_STARTED event already exists for session {}", session.getId());
            return;
        }

        TestActivityEvent event = TestActivityEvent.sessionStarted(session);
        eventRepository.save(event);
        logger.info("Recorded SESSION_STARTED event for session {}", session.getId());
    }

    @Override
    @Transactional
    public void recordSessionCompleted(TestSession session, Double score, Boolean passed) {
        if (eventRepository.existsBySessionIdAndEventType(session.getId(), ActivityEventType.SESSION_COMPLETED)) {
            logger.debug("SESSION_COMPLETED event already exists for session {}", session.getId());
            return;
        }

        TestActivityEvent event = TestActivityEvent.sessionCompleted(session, score, passed);
        eventRepository.save(event);
        logger.info("Recorded SESSION_COMPLETED event for session {} with score={}, passed={}",
                session.getId(), score, passed);
    }

    @Override
    @Transactional
    public void recordSessionAbandoned(TestSession session) {
        if (eventRepository.existsBySessionIdAndEventType(session.getId(), ActivityEventType.SESSION_ABANDONED)) {
            logger.debug("SESSION_ABANDONED event already exists for session {}", session.getId());
            return;
        }

        TestActivityEvent event = TestActivityEvent.sessionAbandoned(session);
        eventRepository.save(event);
        logger.info("Recorded SESSION_ABANDONED event for session {}", session.getId());
    }

    @Override
    @Transactional
    public void recordSessionTimedOut(TestSession session) {
        if (eventRepository.existsBySessionIdAndEventType(session.getId(), ActivityEventType.SESSION_TIMED_OUT)) {
            logger.debug("SESSION_TIMED_OUT event already exists for session {}", session.getId());
            return;
        }

        TestActivityEvent event = TestActivityEvent.sessionTimedOut(session);
        eventRepository.save(event);
        logger.info("Recorded SESSION_TIMED_OUT event for session {}", session.getId());
    }

    // ============================================
    // Private Helper Methods
    // ============================================

    private TestActivityDto mapToActivityDto(TestSession session, User user, TestResult result) {
        String userName = user != null ? user.getFullName() : "Unknown User";
        String userImageUrl = user != null ? user.getImageUrl() : null;
        Double score = result != null ? result.getOverallPercentage() : null;
        Boolean passed = result != null ? result.getPassed() : null;
        Integer timeSpentSeconds = calculateTimeSpent(session);

        return new TestActivityDto(
                session.getId(),
                session.getClerkUserId(),
                userName,
                userImageUrl,
                session.getTemplateId(),
                session.getTemplate().getName(),
                session.getTemplate().getGoal(),
                session.getStatus(),
                session.getCompletedAt() != null ? session.getCompletedAt() : session.getStartedAt(),
                score,
                passed,
                timeSpentSeconds
        );
    }

    private Integer calculateTimeSpent(TestSession session) {
        if (session.getStartedAt() == null || session.getCompletedAt() == null) {
            return null;
        }
        return (int) Duration.between(session.getStartedAt(), session.getCompletedAt()).getSeconds();
    }
}
