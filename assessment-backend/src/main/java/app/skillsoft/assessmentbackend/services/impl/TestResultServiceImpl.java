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
    public Optional<TestResultDto> findById(UUID resultId) {
        return resultRepository.findById(resultId)
                .map(this::toDto);
    }

    @Override
    public Optional<TestResultDto> findBySessionId(UUID sessionId) {
        return resultRepository.findBySession_Id(sessionId)
                .map(this::toDto);
    }

    @Override
    public Page<TestResultSummaryDto> findByUser(String clerkUserId, Pageable pageable) {
        return resultRepository.findByClerkUserId(clerkUserId, pageable)
                .map(this::toSummaryDto);
    }

    @Override
    public List<TestResultSummaryDto> findByUserOrderByDate(String clerkUserId) {
        return resultRepository.findByClerkUserIdOrderByCompletedAtDesc(clerkUserId).stream()
                .map(this::toSummaryDto)
                .toList();
    }

    @Override
    public List<TestResultSummaryDto> findByUserAndTemplate(String clerkUserId, UUID templateId) {
        return resultRepository.findByUserAndTemplate(clerkUserId, templateId).stream()
                .map(this::toSummaryDto)
                .toList();
    }

    @Override
    public Optional<TestResultDto> findLatestByUserAndTemplate(String clerkUserId, UUID templateId) {
        return resultRepository.findLatestByUserAndTemplate(clerkUserId, templateId)
                .map(this::toDto);
    }

    @Override
    public List<TestResultSummaryDto> findPassedByUser(String clerkUserId) {
        return resultRepository.findByClerkUserIdAndPassedTrue(clerkUserId).stream()
                .map(this::toSummaryDto)
                .toList();
    }

    @Override
    public UserTestStatistics getUserStatistics(String clerkUserId) {
        long total = resultRepository.countByClerkUserId(clerkUserId);
        long passed = resultRepository.countByClerkUserIdAndPassedTrue(clerkUserId);
        
        List<TestResult> results = resultRepository.findByClerkUserIdOrderByCompletedAtDesc(clerkUserId);
        
        Double averageScore = null;
        Double bestScore = null;
        LocalDateTime lastTestDate = null;
        
        if (!results.isEmpty()) {
            lastTestDate = results.get(0).getCompletedAt();
            
            averageScore = results.stream()
                    .filter(r -> r.getOverallPercentage() != null)
                    .mapToDouble(TestResult::getOverallPercentage)
                    .average()
                    .orElse(0.0);
            
            bestScore = results.stream()
                    .filter(r -> r.getOverallPercentage() != null)
                    .mapToDouble(TestResult::getOverallPercentage)
                    .max()
                    .orElse(0.0);
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
    public TemplateTestStatistics getTemplateStatistics(UUID templateId) {
        List<TestResult> results = resultRepository.findByTemplateId(templateId);
        
        if (results.isEmpty()) {
            return new TemplateTestStatistics(
                    templateId,
                    null, // Template name would need to be fetched separately
                    0, 0.0, 0.0, 0, 0
            );
        }
        
        String templateName = results.get(0).getSession().getTemplate().getName();
        long total = results.size();
        long passed = results.stream().filter(r -> Boolean.TRUE.equals(r.getPassed())).count();
        
        Double averageScore = results.stream()
                .filter(r -> r.getOverallPercentage() != null)
                .mapToDouble(TestResult::getOverallPercentage)
                .average()
                .orElse(0.0);
        
        Double passRate = total > 0 ? (passed * 100.0 / total) : 0.0;
        
        return new TemplateTestStatistics(
                templateId,
                templateName,
                total,
                averageScore,
                passRate,
                passed,
                total - passed
        );
    }

    @Override
    public List<TestResultSummaryDto> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return resultRepository.findByCompletedAtBetween(startDate, endDate).stream()
                .map(this::toSummaryDto)
                .toList();
    }

    @Override
    public List<TestResultSummaryDto> getRecentResults(int limit) {
        return resultRepository.findTop10ByOrderByCompletedAtDesc().stream()
                .limit(limit)
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
