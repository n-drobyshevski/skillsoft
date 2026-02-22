package app.skillsoft.assessmentbackend.events.listeners;

import app.skillsoft.assessmentbackend.domain.dto.CompetencyScoreDto;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;
import app.skillsoft.assessmentbackend.domain.entities.TestResult;
import app.skillsoft.assessmentbackend.events.scoring.ScoringCompletedEvent;
import app.skillsoft.assessmentbackend.repository.TestResultRepository;
import app.skillsoft.assessmentbackend.services.external.impl.PassportServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Automatically creates or updates a Competency Passport when an OVERVIEW
 * assessment completes scoring.
 *
 * Follows the same async event pattern as {@link ScoringAuditListener}:
 * {@code @Async + @TransactionalEventListener + REQUIRES_NEW} ensures
 * passport persistence never blocks or rolls back the scoring pipeline.
 */
@Component
public class PassportUpdateListener {

    private static final Logger log = LoggerFactory.getLogger(PassportUpdateListener.class);

    private final TestResultRepository resultRepository;
    private final PassportServiceImpl passportService;

    public PassportUpdateListener(TestResultRepository resultRepository,
                                  PassportServiceImpl passportService) {
        this.resultRepository = resultRepository;
        this.passportService = passportService;
    }

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onScoringCompleted(ScoringCompletedEvent event) {
        try {
            // Only create passports from OVERVIEW assessments
            if (event.goal() != AssessmentGoal.OVERVIEW) {
                log.debug("Skipping passport update for non-OVERVIEW goal: {}", event.goal());
                return;
            }

            // Load the full result with session data
            TestResult result = resultRepository.findByIdWithSessionAndTemplate(event.resultId())
                .orElse(null);

            if (result == null) {
                log.warn("TestResult not found for passport update: resultId={}", event.resultId());
                return;
            }

            // Skip anonymous sessions (no Clerk user to associate passport with)
            String clerkUserId = result.getClerkUserId();
            if (clerkUserId == null || clerkUserId.isBlank()) {
                log.debug("Skipping passport update for anonymous session: resultId={}", event.resultId());
                return;
            }

            // Convert competency scores: percentage (0-100) -> 1.0-5.0 scale
            List<CompetencyScoreDto> competencyScores = result.getCompetencyScores();
            if (competencyScores == null || competencyScores.isEmpty()) {
                log.warn("No competency scores in result for passport update: resultId={}", event.resultId());
                return;
            }

            Map<UUID, Double> passportScores = new HashMap<>();
            for (CompetencyScoreDto cs : competencyScores) {
                if (cs.getCompetencyId() != null && cs.getPercentage() != null) {
                    // percentage / 20.0 maps 0-100 -> 0.0-5.0, clamped to [1.0, 5.0]
                    double score = Math.max(1.0, Math.min(5.0, cs.getPercentage() / 20.0));
                    passportScores.put(cs.getCompetencyId(), score);
                }
            }

            if (passportScores.isEmpty()) {
                log.warn("No valid competency scores after conversion: resultId={}", event.resultId());
                return;
            }

            // Save passport
            passportService.savePassportForClerkUser(
                clerkUserId,
                passportScores,
                result.getBigFiveProfile(),
                result.getId()
            );

            log.info("Passport updated from OVERVIEW result: clerkUserId={}, resultId={}, competencies={}",
                    clerkUserId, event.resultId(), passportScores.size());

        } catch (Exception e) {
            // Passport update failure must never block the scoring pipeline
            log.error("Failed to update passport from scoring event: resultId={}, error={}",
                    event.resultId(), e.getMessage(), e);
        }
    }
}
