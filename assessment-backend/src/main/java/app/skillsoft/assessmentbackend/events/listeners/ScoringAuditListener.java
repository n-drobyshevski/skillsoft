package app.skillsoft.assessmentbackend.events.listeners;

import app.skillsoft.assessmentbackend.domain.dto.CompetencyScoreDto;
import app.skillsoft.assessmentbackend.domain.dto.IndicatorScoreDto;
import app.skillsoft.assessmentbackend.domain.entities.ScoringAuditLog;
import app.skillsoft.assessmentbackend.events.scoring.ScoringAuditEvent;
import app.skillsoft.assessmentbackend.repository.ScoringAuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.*;

/**
 * Asynchronous listener for scoring audit events.
 *
 * Persists scoring audit trail data independently of the main scoring transaction.
 * Uses @Async + REQUIRES_NEW propagation to ensure audit persistence does not
 * affect scoring performance or transaction outcome.
 */
@Component
public class ScoringAuditListener {

    private static final Logger log = LoggerFactory.getLogger(ScoringAuditListener.class);

    private final ScoringAuditLogRepository auditLogRepository;

    public ScoringAuditListener(ScoringAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleScoringAudit(ScoringAuditEvent event) {
        try {
            log.debug("Persisting scoring audit log for session={} result={}",
                    event.sessionId(), event.resultId());

            ScoringAuditLog auditLog = new ScoringAuditLog();
            auditLog.setSessionId(event.sessionId());
            auditLog.setResultId(event.resultId());
            auditLog.setClerkUserId(event.clerkUserId());
            auditLog.setTemplateId(event.templateId());
            auditLog.setGoal(event.goal());
            auditLog.setStrategyClass(event.strategyClass());
            auditLog.setOverallScore(event.overallScore());
            auditLog.setOverallPercentage(event.overallPercentage());
            auditLog.setPassed(event.passed());
            auditLog.setPercentile(event.percentile());
            auditLog.setIndicatorWeights(event.indicatorWeights());
            auditLog.setConfigSnapshot(event.configSnapshot());
            auditLog.setTotalAnswers(event.totalAnswers());
            auditLog.setAnsweredCount(event.answeredCount());
            auditLog.setSkippedCount(event.skippedCount());
            auditLog.setScoringDurationMs(event.scoringDuration().toMillis());

            // Build competency breakdown from score DTOs
            if (event.competencyScores() != null) {
                List<Map<String, Object>> breakdown = new ArrayList<>();
                for (CompetencyScoreDto cs : event.competencyScores()) {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("competencyId", cs.getCompetencyId() != null ? cs.getCompetencyId().toString() : null);
                    entry.put("competencyName", cs.getCompetencyName());
                    entry.put("percentage", cs.getPercentage());
                    entry.put("score", cs.getScore());
                    entry.put("maxScore", cs.getMaxScore());
                    entry.put("questionsAnswered", cs.getQuestionsAnswered());
                    entry.put("indicatorCount", cs.getIndicatorScores() != null ? cs.getIndicatorScores().size() : 0);

                    // Include indicator-level weights for traceability
                    if (cs.getIndicatorScores() != null) {
                        List<Map<String, Object>> indicators = new ArrayList<>();
                        for (IndicatorScoreDto is : cs.getIndicatorScores()) {
                            Map<String, Object> ind = new LinkedHashMap<>();
                            ind.put("indicatorId", is.getIndicatorId() != null ? is.getIndicatorId().toString() : null);
                            ind.put("weight", is.getWeight());
                            ind.put("percentage", is.getPercentage());
                            indicators.add(ind);
                        }
                        entry.put("indicators", indicators);
                    }
                    breakdown.add(entry);
                }
                auditLog.setCompetencyBreakdown(breakdown);
            }

            auditLogRepository.save(auditLog);

            log.info("Scoring audit log persisted: id={} session={} duration={}ms",
                    auditLog.getId(), event.sessionId(), event.scoringDuration().toMillis());

        } catch (Exception e) {
            // Audit failure should never prevent scoring from completing
            log.error("Failed to persist scoring audit log for session={}: {}",
                    event.sessionId(), e.getMessage(), e);
        }
    }
}
