package app.skillsoft.assessmentbackend.services.impl;

import app.skillsoft.assessmentbackend.domain.dto.TestResultDto;
import app.skillsoft.assessmentbackend.domain.dto.TestResultSummaryDto;
import app.skillsoft.assessmentbackend.domain.entities.TestResult;
import app.skillsoft.assessmentbackend.domain.entities.TestSession;
import app.skillsoft.assessmentbackend.repository.TestResultRepository;
import app.skillsoft.assessmentbackend.services.TestResultService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TestResultServiceImpl implements TestResultService {

    private final TestResultRepository resultRepository;

    public TestResultServiceImpl(TestResultRepository resultRepository) {
        this.resultRepository = resultRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TestResultDto> findById(UUID resultId) {
        // Use JOIN FETCH to avoid N+1 when accessing session and template
        return resultRepository.findByIdWithSessionAndTemplate(resultId)
                .map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TestResultDto> findBySessionId(UUID sessionId) {
        // Use JOIN FETCH to avoid N+1 when accessing template
        return resultRepository.findBySessionIdWithTemplate(sessionId)
                .map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TestResultSummaryDto> findByUser(String clerkUserId, Pageable pageable) {
        // Use JOIN FETCH to avoid N+1 when accessing session and template
        return resultRepository.findByClerkUserIdWithSessionAndTemplate(clerkUserId, pageable)
                .map(this::toSummaryDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestResultSummaryDto> findByUserOrderByDate(String clerkUserId) {
        // Use JOIN FETCH to avoid N+1 when accessing session and template
        return resultRepository.findByClerkUserIdWithSessionAndTemplate(clerkUserId).stream()
                .map(this::toSummaryDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestResultSummaryDto> findByUserAndTemplate(String clerkUserId, UUID templateId) {
        // Use JOIN FETCH to avoid N+1 when accessing session
        return resultRepository.findByUserAndTemplateWithSession(clerkUserId, templateId).stream()
                .map(this::toSummaryDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TestResultDto> findLatestByUserAndTemplate(String clerkUserId, UUID templateId) {
        return resultRepository.findLatestByUserAndTemplate(clerkUserId, templateId)
                .map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestResultSummaryDto> findPassedByUser(String clerkUserId) {
        // Use JOIN FETCH to avoid N+1 when accessing session and template
        return resultRepository.findPassedByClerkUserIdWithSessionAndTemplate(clerkUserId).stream()
                .map(this::toSummaryDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserTestStatistics getUserStatistics(String clerkUserId) {
        // Use single aggregate query to avoid multiple COUNT/AVG queries
        Object[] stats = resultRepository.getUserStatisticsAggregate(clerkUserId);

        long total = stats[0] != null ? ((Number) stats[0]).longValue() : 0L;
        long passed = stats[1] != null ? ((Number) stats[1]).longValue() : 0L;
        Double averageScore = stats[2] != null ? ((Number) stats[2]).doubleValue() : null;
        Double bestScore = stats[3] != null ? ((Number) stats[3]).doubleValue() : null;

        // Still need one query for last test date (aggregate doesn't return this)
        LocalDateTime lastTestDate = null;
        if (total > 0) {
            // Use existing query but only fetch top 1
            List<TestResult> recentResults = resultRepository.findByClerkUserIdOrderByCompletedAtDesc(clerkUserId);
            if (!recentResults.isEmpty()) {
                lastTestDate = recentResults.get(0).getCompletedAt();
            }
        }

        // Handle zero results case
        if (total == 0) {
            averageScore = null;
            bestScore = null;
        }

        return new UserTestStatistics(
                clerkUserId,
                total,
                passed,
                total - passed,
                averageScore,
                bestScore,
                lastTestDate
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TemplateTestStatistics getTemplateStatistics(UUID templateId) {
        // Use single aggregate query to avoid loading all results
        Object[] stats = resultRepository.getTemplateStatisticsAggregate(templateId);

        long total = stats[0] != null ? ((Number) stats[0]).longValue() : 0L;
        long passed = stats[1] != null ? ((Number) stats[1]).longValue() : 0L;
        Double averageScore = stats[2] != null ? ((Number) stats[2]).doubleValue() : 0.0;
        // minScore = stats[3], maxScore = stats[4] - available if needed

        if (total == 0) {
            return new TemplateTestStatistics(
                    templateId,
                    null, // Template name not available from aggregate
                    0, 0.0, 0.0, 0, 0
            );
        }

        Double passRate = total > 0 ? (passed * 100.0 / total) : 0.0;

        // Template name requires separate query - could be cached or fetched via join
        // For now, return null as the aggregate optimization is more important
        return new TemplateTestStatistics(
                templateId,
                null, // Template name omitted to avoid extra query
                total,
                averageScore,
                passRate,
                passed,
                total - passed
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestResultSummaryDto> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return resultRepository.findByCompletedAtBetween(startDate, endDate).stream()
                .map(this::toSummaryDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestResultSummaryDto> getRecentResults(int limit) {
        // Use JOIN FETCH query to avoid N+1 when accessing session and template
        return resultRepository.findRecentWithSessionAndTemplate(limit).stream()
                .map(this::toSummaryDto)
                .toList();
    }

    @Override
    @Transactional
    public int calculatePercentile(UUID resultId) {
        TestResult result = resultRepository.findById(resultId)
                .orElseThrow(() -> new RuntimeException("Result not found with id: " + resultId));
        
        UUID templateId = result.getSession().getTemplate().getId();
        Double score = result.getOverallPercentage();
        
        if (score == null) {
            return 0;
        }
        
        long belowCount = resultRepository.countResultsBelowScore(templateId, score);
        long totalCount = resultRepository.countResultsByTemplateId(templateId);
        
        int percentile = totalCount > 0 ? (int) ((belowCount * 100) / totalCount) : 0;
        
        // Update the result with the calculated percentile
        result.setPercentile(percentile);
        resultRepository.save(result);
        
        return percentile;
    }

    // Mapping methods
    private TestResultDto toDto(TestResult result) {
        TestSession session = result.getSession();
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

    private TestResultSummaryDto toSummaryDto(TestResult result) {
        TestSession session = result.getSession();
        return new TestResultSummaryDto(
                result.getId(),
                result.getSessionId(),
                session.getTemplate().getId(),
                session.getTemplate().getName(),
                result.getOverallPercentage(),
                result.getPassed(),
                result.getCompletedAt()
        );
    }
}
