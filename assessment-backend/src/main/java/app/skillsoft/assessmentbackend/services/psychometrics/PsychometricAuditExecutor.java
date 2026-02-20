package app.skillsoft.assessmentbackend.services.psychometrics;

import app.skillsoft.assessmentbackend.domain.entities.BigFiveTrait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Executor service for psychometric audit micro-transactions.
 *
 * Each public method runs in its own {@code REQUIRES_NEW} transaction, ensuring:
 * <ul>
 *   <li>The HikariCP connection is acquired and released per question/competency/trait</li>
 *   <li>A failure in one item does not roll back work already committed for other items</li>
 *   <li>The outer {@code PsychometricAuditJob.runNightlyAudit()} holds no transaction
 *       (and therefore no pooled connection) for the full audit duration</li>
 * </ul>
 *
 * This class must be a separate Spring bean from {@code PsychometricAuditJob} so that
 * Spring's transactional proxy intercepts the calls (self-invocation would bypass the proxy).
 */
@Service
public class PsychometricAuditExecutor {

    private static final Logger log = LoggerFactory.getLogger(PsychometricAuditExecutor.class);

    private final PsychometricAnalysisService analysisService;

    public PsychometricAuditExecutor(PsychometricAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    /**
     * Recalculate item statistics for a single question in its own transaction.
     *
     * @param questionId the question to process
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processItemStatistics(UUID questionId) {
        log.debug("Processing item statistics for question {}", questionId);
        analysisService.calculateItemStatistics(questionId);
        log.debug("Completed item statistics for question {}", questionId);
    }

    /**
     * Recalculate reliability for a single competency in its own transaction.
     *
     * @param competencyId the competency to process
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processCompetencyReliability(UUID competencyId) {
        log.debug("Processing competency reliability for {}", competencyId);
        analysisService.calculateCompetencyReliability(competencyId);
        log.debug("Completed competency reliability for {}", competencyId);
    }

    /**
     * Recalculate reliability for a single Big Five trait in its own transaction.
     *
     * @param trait the Big Five trait to process
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processBigFiveReliability(BigFiveTrait trait) {
        log.debug("Processing Big Five reliability for trait {}", trait);
        analysisService.calculateBigFiveReliability(trait);
        log.debug("Completed Big Five reliability for trait {}", trait);
    }

    /**
     * Update the validity status for a single question in its own transaction.
     *
     * @param questionId the question whose status to update
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processValidityStatusUpdate(UUID questionId) {
        log.debug("Processing validity status update for question {}", questionId);
        analysisService.updateItemValidityStatus(questionId);
        log.debug("Completed validity status update for question {}", questionId);
    }
}
