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
 */
@Service
public class PsychometricAuditJob {

    private static final Logger log = LoggerFactory.getLogger(PsychometricAuditJob.class);

    private final PsychometricAnalysisService analysisService;
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
            ItemStatisticsRepository itemStatsRepository,
            TestAnswerRepository answerRepository,
            AssessmentQuestionRepository questionRepository,
            CompetencyRepository competencyRepository) {
        this.analysisService = analysisService;
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
     */
    @Scheduled(cron = "${skillsoft.psychometrics.nightly-cron:0 0 2 * * ?}")
    @Transactional
    public void runNightlyAudit() {
        if (!psychometricsEnabled) {
            log.debug("Psychometric analysis is disabled, skipping nightly audit");
            return;
        }

        log.info("Starting nightly psychometric audit");
        var startTime = System.currentTimeMillis();

        try {
            // Step 1: Recalculate items with new responses
            int itemsRecalculated = recalculateItemsWithNewResponses();

            // Step 2: Recalculate competency reliability
            int competenciesRecalculated = recalculateCompetencyReliability();

            // Step 3: Recalculate Big Five trait reliability
            int traitsRecalculated = recalculateBigFiveReliability();

            // Step 4: Update validity statuses for all items
            int statusesUpdated = updateAllValidityStatuses();

            var duration = System.currentTimeMillis() - startTime;

            log.info("Nightly psychometric audit completed in {}ms: {} items, {} competencies, {} traits, {} status updates",
                    duration, itemsRecalculated, competenciesRecalculated, traitsRecalculated, statusesUpdated);

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

        int itemsRecalculated = recalculateItemsWithNewResponses();
        int competenciesRecalculated = recalculateCompetencyReliability();
        int traitsRecalculated = recalculateBigFiveReliability();
        int statusesUpdated = updateAllValidityStatuses();

        var duration = System.currentTimeMillis() - startTime;

        String message = String.format("Audit completed in %dms", duration);
        log.info(message);

        return new AuditResult(itemsRecalculated, competenciesRecalculated, traitsRecalculated, statusesUpdated, message);
    }

    /**
     * Recalculate psychometric metrics for items with new responses.
     */
    private int recalculateItemsWithNewResponses() {
        // Find items that need recalculation (have responses since last calculation)
        LocalDateTime threshold = LocalDateTime.now().minusDays(1);
        List<UUID> questionIds = itemStatsRepository.findQuestionsNeedingRecalculation(minResponses, threshold);

        int count = 0;
        for (UUID questionId : questionIds) {
            try {
                analysisService.calculateItemStatistics(questionId);
                count++;
            } catch (Exception e) {
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
                        analysisService.calculateItemStatistics(stats.getQuestionId());
                        count++;
                    } catch (Exception e) {
                        log.warn("Failed to calculate initial stats for item {}: {}", stats.getQuestionId(), e.getMessage());
                    }
                }
            }
        }

        return count;
    }

    /**
     * Recalculate reliability for all competencies.
     */
    private int recalculateCompetencyReliability() {
        List<Competency> competencies = competencyRepository.findAll();
        int count = 0;

        for (Competency competency : competencies) {
            try {
                analysisService.calculateCompetencyReliability(competency.getId());
                count++;
            } catch (Exception e) {
                log.warn("Failed to recalculate reliability for competency {}: {}", competency.getId(), e.getMessage());
            }
        }

        return count;
    }

    /**
     * Recalculate reliability for all Big Five traits.
     */
    private int recalculateBigFiveReliability() {
        int count = 0;

        for (BigFiveTrait trait : BigFiveTrait.values()) {
            try {
                analysisService.calculateBigFiveReliability(trait);
                count++;
            } catch (Exception e) {
                log.warn("Failed to recalculate reliability for trait {}: {}", trait, e.getMessage());
            }
        }

        return count;
    }

    /**
     * Update validity statuses for all items with calculated metrics.
     */
    private int updateAllValidityStatuses() {
        List<ItemStatistics> allStats = itemStatsRepository.findAll();
        int count = 0;

        for (ItemStatistics stats : allStats) {
            // Only update items that have been calculated
            if (stats.getLastCalculatedAt() != null) {
                try {
                    analysisService.updateItemValidityStatus(stats.getQuestionId());
                    count++;
                } catch (Exception e) {
                    log.warn("Failed to update status for item {}: {}", stats.getQuestionId(), e.getMessage());
                }
            }
        }

        return count;
    }

    /**
     * Initialize item statistics for new questions that don't have records yet.
     * Called during application startup or when new questions are added.
     */
    @Transactional
    public int initializeNewQuestions() {
        List<AssessmentQuestion> questions = questionRepository.findAll();
        int count = 0;

        for (AssessmentQuestion question : questions) {
            if (!itemStatsRepository.existsByQuestion_Id(question.getId())) {
                ItemStatistics stats = new ItemStatistics(question);
                itemStatsRepository.save(stats);
                count++;
            }
        }

        if (count > 0) {
            log.info("Initialized item statistics for {} new questions", count);
        }

        return count;
    }

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
