package app.skillsoft.assessmentbackend.services.psychometrics;

import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.repository.AssessmentQuestionRepository;
import app.skillsoft.assessmentbackend.repository.CompetencyRepository;
import app.skillsoft.assessmentbackend.repository.ItemStatisticsRepository;
import app.skillsoft.assessmentbackend.repository.TestAnswerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Scheduled service for psychometric analysis audit jobs.
 *
 * Per the Test Validation Mechanic architecture:
 * - Nightly batch job recalculates all items with new responses
 * - Real-time triggers when questions reach response thresholds (50, 100, 150...)
 * - Updates item validity statuses and competency reliability
 *
 * Configuration properties:
 * - skillsoft.psychometrics.enabled: Enable/disable psychometric features
 * - skillsoft.psychometrics.min-responses: Minimum responses for analysis (default: 50)
 * - skillsoft.psychometrics.nightly-cron: Cron expression for nightly job
 *
 * Transaction strategy (BE-006):
 * The nightly audit does NOT run inside a single long transaction. Instead, each
 * question/competency/trait is processed in its own REQUIRES_NEW micro-transaction
 * via {@link PsychometricAuditExecutor}. This prevents HikariCP connection exhaustion
 * and provides fault isolation (one failure does not abort the entire audit).
 */
@Service
public class PsychometricAuditJob {

    private static final Logger log = LoggerFactory.getLogger(PsychometricAuditJob.class);

    private final PsychometricAnalysisService analysisService;
    private final PsychometricAuditExecutor auditExecutor;
    private final ItemStatisticsRepository itemStatsRepository;
    private final TestAnswerRepository answerRepository;
    private final AssessmentQuestionRepository questionRepository;
    private final CompetencyRepository competencyRepository;

    @Value("${skillsoft.psychometrics.enabled:true}")
    private boolean psychometricsEnabled;

    @Value("${skillsoft.psychometrics.min-responses:50}")
    private int minResponses;

    @Value("${skillsoft.psychometrics.recalculation-threshold:10}")
    private int recalculationThreshold;

    public PsychometricAuditJob(
            PsychometricAnalysisService analysisService,
            PsychometricAuditExecutor auditExecutor,
            ItemStatisticsRepository itemStatsRepository,
            TestAnswerRepository answerRepository,
            AssessmentQuestionRepository questionRepository,
            CompetencyRepository competencyRepository) {
        this.analysisService = analysisService;
        this.auditExecutor = auditExecutor;
        this.itemStatsRepository = itemStatsRepository;
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
        this.competencyRepository = competencyRepository;
    }

    /**
     * Initialize item statistics on application startup.
     * Creates ItemStatistics records for any questions that don't have one yet.
     *
     * Note: This method is intentionally NOT @Transactional. The inner
     * initializeNewQuestions() method manages its own transaction. If the inner
     * transaction fails, we catch the exception here without the outer transaction
     * being marked rollback-only, which previously caused context load failures
     * (especially with H2 in integration tests).
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!psychometricsEnabled) {
            log.debug("Psychometric analysis is disabled, skipping startup initialization");
            return;
        }

        try {
            int initialized = initializeNewQuestions();
            if (initialized > 0) {
                log.info("Startup: initialized {} new question statistics records", initialized);
            }
        } catch (Exception e) {
            log.warn("Failed to initialize psychometric statistics on startup: {}", e.getMessage());
        }
    }

    /**
     * Nightly batch job - runs at 2 AM daily.
     * Recalculates psychometric metrics for all items with new responses.
     *
     * Protected by ShedLock to prevent concurrent execution across multiple instances:
     * - lockAtMostFor: 30 minutes -- upper bound; lock auto-releases even if the node crashes.
     * - lockAtLeastFor: 5 minutes -- prevents rapid re-execution if the job finishes quickly.
     *
     * This method is intentionally NOT @Transactional. Each question, competency, and trait
     * is processed in its own REQUIRES_NEW micro-transaction via {@link PsychometricAuditExecutor},
     * so the HikariCP connection is released after each unit of work (BE-006).
     */
    @Scheduled(cron = "${skillsoft.psychometrics.nightly-cron:0 0 2 * * ?}")
    @SchedulerLock(name = "psychometricNightlyAudit", lockAtMostFor = "PT30M", lockAtLeastFor = "PT5M")
    public void runNightlyAudit() {
        if (!psychometricsEnabled) {
            log.debug("Psychometric analysis is disabled, skipping nightly audit");
            return;
        }

        log.info("Starting nightly psychometric audit");
        var startTime = System.currentTimeMillis();

        int totalProcessed = 0;
        int totalSucceeded = 0;
        int totalFailed = 0;

        try {
            // Step 1: Recalculate items with new responses
            AuditStepResult itemResult = recalculateItemsWithNewResponses();
            totalProcessed += itemResult.processed;
            totalSucceeded += itemResult.succeeded;
            totalFailed += itemResult.failed;

            // Step 2: Recalculate competency reliability
            AuditStepResult competencyResult = recalculateCompetencyReliability();
            totalProcessed += competencyResult.processed;
            totalSucceeded += competencyResult.succeeded;
            totalFailed += competencyResult.failed;

            // Step 3: Recalculate Big Five trait reliability
            AuditStepResult traitResult = recalculateBigFiveReliability();
            totalProcessed += traitResult.processed;
            totalSucceeded += traitResult.succeeded;
            totalFailed += traitResult.failed;

            // Step 4: Update validity statuses for all items
            AuditStepResult statusResult = updateAllValidityStatuses();
            totalProcessed += statusResult.processed;
            totalSucceeded += statusResult.succeeded;
            totalFailed += statusResult.failed;

            var duration = System.currentTimeMillis() - startTime;

            log.info("Nightly psychometric audit completed in {}ms: {} items recalculated, "
                            + "{} competencies, {} traits, {} status updates",
                    duration, itemResult.succeeded, competencyResult.succeeded,
                    traitResult.succeeded, statusResult.succeeded);

            log.info("Processed {} questions, {} succeeded, {} failed",
                    totalProcessed, totalSucceeded, totalFailed);

        } catch (Exception e) {
            log.error("Nightly psychometric audit failed", e);
        }
    }

    /**
     * Real-time trigger called after each answer submission.
     * Only recalculates if the question has reached a response threshold.
     *
     * @param questionId The question that received a new answer
     */
    public void onAnswerSubmitted(UUID questionId) {
        if (!psychometricsEnabled) {
            return;
        }

        try {
            long responseCount = answerRepository.countByQuestion_Id(questionId);

            // Check if this is a threshold milestone (50, 100, 150, etc.)
            if (responseCount >= minResponses && responseCount % minResponses == 0) {
                log.info("Question {} reached {} responses, triggering psychometric recalculation",
                        questionId, responseCount);

                // Recalculate item statistics
                analysisService.calculateItemStatistics(questionId);

                // Update validity status
                analysisService.updateItemValidityStatus(questionId);

                // Also recalculate the parent competency's reliability
                questionRepository.findById(questionId).ifPresent(question -> {
                    UUID competencyId = question.getBehavioralIndicator().getCompetency().getId();
                    try {
                        analysisService.calculateCompetencyReliability(competencyId);
                    } catch (Exception e) {
                        log.warn("Failed to recalculate competency {} reliability after question {} update: {}",
                                competencyId, questionId, e.getMessage());
                    }
                });
            }
        } catch (Exception e) {
            log.warn("Failed to process answer submission for question {}: {}", questionId, e.getMessage());
        }
    }

    /**
     * Manual trigger for a full audit.
     * Can be called from the admin API endpoint.
     *
     * @return Summary of the audit results
     */
    @Transactional
    public AuditResult triggerManualAudit() {
        if (!psychometricsEnabled) {
            return new AuditResult(0, 0, 0, 0, "Psychometric analysis is disabled");
        }

        log.info("Manual psychometric audit triggered");
        var startTime = System.currentTimeMillis();

        // Ensure all questions have statistics records before recalculating
        initializeNewQuestions();

        AuditStepResult itemResult = recalculateItemsWithNewResponses();
        AuditStepResult competencyResult = recalculateCompetencyReliability();
        AuditStepResult traitResult = recalculateBigFiveReliability();
        AuditStepResult statusResult = updateAllValidityStatuses();

        var duration = System.currentTimeMillis() - startTime;

        String message = String.format("Audit completed in %dms", duration);
        log.info(message);

        return new AuditResult(itemResult.succeeded, competencyResult.succeeded,
                traitResult.succeeded, statusResult.succeeded, message);
    }

    /**
     * Recalculate psychometric metrics for items with new responses.
     * Each question is processed in its own REQUIRES_NEW micro-transaction.
     */
    private AuditStepResult recalculateItemsWithNewResponses() {
        // Find items that need recalculation (have responses since last calculation)
        LocalDateTime threshold = LocalDateTime.now().minusDays(1);
        List<UUID> questionIds = itemStatsRepository.findQuestionsNeedingRecalculation(minResponses, threshold);

        int succeeded = 0;
        int failed = 0;

        for (UUID questionId : questionIds) {
            try {
                auditExecutor.processItemStatistics(questionId);
                succeeded++;
            } catch (Exception e) {
                failed++;
                log.warn("Failed to recalculate item {}: {}", questionId, e.getMessage());
            }
        }

        // Also check for items that have never been calculated but now have sufficient responses
        List<ItemStatistics> probationItems = itemStatsRepository.findByValidityStatus(ItemValidityStatus.PROBATION);
        for (ItemStatistics stats : probationItems) {
            if (stats.getLastCalculatedAt() == null) {
                long responseCount = answerRepository.countByQuestion_Id(stats.getQuestionId());
                if (responseCount >= minResponses) {
                    try {
                        auditExecutor.processItemStatistics(stats.getQuestionId());
                        succeeded++;
                    } catch (Exception e) {
                        failed++;
                        log.warn("Failed to calculate initial stats for item {}: {}", stats.getQuestionId(), e.getMessage());
                    }
                }
            }
        }

        return new AuditStepResult(succeeded + failed, succeeded, failed);
    }

    /**
     * Recalculate reliability for all competencies using paginated batches.
     * Each competency is processed in its own REQUIRES_NEW micro-transaction.
     */
    private AuditStepResult recalculateCompetencyReliability() {
        int succeeded = 0;
        int failed = 0;
        int page = 0;
        Pageable pageable = PageRequest.of(page, 100);
        Slice<Competency> slice = competencyRepository.findAll(pageable);

        while (true) {
            for (Competency competency : slice.getContent()) {
                try {
                    auditExecutor.processCompetencyReliability(competency.getId());
                    succeeded++;
                } catch (Exception e) {
                    failed++;
                    log.warn("Failed to recalculate reliability for competency {}: {}", competency.getId(), e.getMessage());
                }
            }
            if (!slice.hasNext()) {
                break;
            }
            page++;
            pageable = PageRequest.of(page, 100);
            slice = competencyRepository.findAll(pageable);
        }

        return new AuditStepResult(succeeded + failed, succeeded, failed);
    }

    /**
     * Recalculate reliability for all Big Five traits.
     * Each trait is processed in its own REQUIRES_NEW micro-transaction.
     */
    private AuditStepResult recalculateBigFiveReliability() {
        int succeeded = 0;
        int failed = 0;

        for (BigFiveTrait trait : BigFiveTrait.values()) {
            try {
                auditExecutor.processBigFiveReliability(trait);
                succeeded++;
            } catch (Exception e) {
                failed++;
                log.warn("Failed to recalculate reliability for trait {}: {}", trait, e.getMessage());
            }
        }

        return new AuditStepResult(succeeded + failed, succeeded, failed);
    }

    /**
     * Update validity statuses for all items with calculated metrics using paginated batches.
     * Each status update is processed in its own REQUIRES_NEW micro-transaction.
     */
    private AuditStepResult updateAllValidityStatuses() {
        int succeeded = 0;
        int failed = 0;
        int page = 0;
        Pageable pageable = PageRequest.of(page, 100);
        Slice<ItemStatistics> slice = itemStatsRepository.findAll(pageable);

        while (true) {
            for (ItemStatistics stats : slice.getContent()) {
                // Only update items that have been calculated
                if (stats.getLastCalculatedAt() != null) {
                    try {
                        auditExecutor.processValidityStatusUpdate(stats.getQuestionId());
                        succeeded++;
                    } catch (Exception e) {
                        failed++;
                        log.warn("Failed to update status for item {}: {}", stats.getQuestionId(), e.getMessage());
                    }
                }
            }
            if (!slice.hasNext()) {
                break;
            }
            page++;
            pageable = PageRequest.of(page, 100);
            slice = itemStatsRepository.findAll(pageable);
        }

        return new AuditStepResult(succeeded + failed, succeeded, failed);
    }

    /**
     * Initialize item statistics for new questions that don't have records yet.
     * Called during application startup or when new questions are added.
     * Processes questions in paginated batches to avoid loading all into memory.
     */
    @Transactional
    public int initializeNewQuestions() {
        int count = 0;
        int page = 0;
        Pageable pageable = PageRequest.of(page, 100);
        Slice<AssessmentQuestion> slice = questionRepository.findAll(pageable);

        while (true) {
            for (AssessmentQuestion question : slice.getContent()) {
                if (!itemStatsRepository.existsByQuestion_Id(question.getId())) {
                    ItemStatistics stats = new ItemStatistics(question);
                    itemStatsRepository.save(stats);
                    count++;
                }
            }
            if (!slice.hasNext()) {
                break;
            }
            page++;
            pageable = PageRequest.of(page, 100);
            slice = questionRepository.findAll(pageable);
        }

        if (count > 0) {
            log.info("Initialized item statistics for {} new questions", count);
        }

        return count;
    }

    /**
     * Internal record tracking per-step audit results (processed, succeeded, failed).
     */
    private record AuditStepResult(int processed, int succeeded, int failed) {}

    /**
     * Result record for audit operations.
     */
    public record AuditResult(
            int itemsRecalculated,
            int competenciesRecalculated,
            int traitsRecalculated,
            int statusesUpdated,
            String message
    ) {}
}
