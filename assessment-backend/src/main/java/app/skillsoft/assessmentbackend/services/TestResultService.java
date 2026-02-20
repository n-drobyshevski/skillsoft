package app.skillsoft.assessmentbackend.services;

import app.skillsoft.assessmentbackend.domain.dto.TestResultDto;
import app.skillsoft.assessmentbackend.domain.dto.TestResultSummaryDto;
import app.skillsoft.assessmentbackend.domain.dto.TrendDataPointDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for managing test results.
 */
public interface TestResultService {

    /**
     * Get a test result by ID.
     */
    Optional<TestResultDto> findById(UUID resultId);

    /**
     * Get result for a specific session.
     */
    Optional<TestResultDto> findBySessionId(UUID sessionId);

    /**
     * Get all results for a user with pagination.
     */
    Page<TestResultSummaryDto> findByUser(String clerkUserId, Pageable pageable);

    /**
     * Get all results for a user (ordered by completion date) with pagination.
     */
    Page<TestResultSummaryDto> findByUserOrderByDate(String clerkUserId, Pageable pageable);

    /**
     * Get user's results for a specific template.
     */
    List<TestResultSummaryDto> findByUserAndTemplate(String clerkUserId, UUID templateId);

    /**
     * Get the most recent result for a user on a specific template.
     */
    Optional<TestResultDto> findLatestByUserAndTemplate(String clerkUserId, UUID templateId);

    /**
     * Get passed results for a user with pagination.
     */
    Page<TestResultSummaryDto> findPassedByUser(String clerkUserId, Pageable pageable);

    /**
     * Get historical trend data for a user, optionally filtered by template.
     * Returns lightweight data points optimized for time-series visualization.
     */
    List<TrendDataPointDto> getUserHistory(String clerkUserId, UUID templateId);

    /**
     * Get user statistics.
     */
    UserTestStatistics getUserStatistics(String clerkUserId);

    /**
     * Get template statistics.
     */
    TemplateTestStatistics getTemplateStatistics(UUID templateId);

    /**
     * Get results within a date range (for reporting) with pagination.
     */
    Page<TestResultSummaryDto> findByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Get recent results (for dashboard).
     */
    List<TestResultSummaryDto> getRecentResults(int limit);

    /**
     * Calculate percentile for a result.
     */
    int calculatePercentile(UUID resultId);

    /**
     * Statistics record for user.
     */
    record UserTestStatistics(
            String clerkUserId,
            long totalTests,
            long passedTests,
            long failedTests,
            Double averageScore,
            Double bestScore,
            LocalDateTime lastTestDate
    ) {}

    /**
     * Statistics record for template.
     */
    record TemplateTestStatistics(
            UUID templateId,
            String templateName,
            long totalAttempts,
            Double averageScore,
            Double passRate,
            long passedAttempts,
            long failedAttempts
    ) {}
}
