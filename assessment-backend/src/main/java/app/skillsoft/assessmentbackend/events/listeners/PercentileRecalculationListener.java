package app.skillsoft.assessmentbackend.events.listeners;

import app.skillsoft.assessmentbackend.domain.entities.TestResult;
import app.skillsoft.assessmentbackend.events.scoring.ScoringCompletedEvent;
import app.skillsoft.assessmentbackend.repository.TestResultRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Asynchronous event listener for percentile recalculation.
 *
 * When scoring completes, this listener recalculates percentiles for all recent
 * results of the same template to ensure accurate ranking even with concurrent
 * test completions.
 *
 * The recalculation window is 5 minutes to capture results that may have been
 * scored concurrently and received stale percentile values.
 *
 * This runs asynchronously to avoid blocking the main scoring thread.
 */
@Component
@Slf4j
public class PercentileRecalculationListener {

    private static final int RECALCULATION_WINDOW_MINUTES = 5;
    private static final int DEFAULT_PERCENTILE = 50;

    private final TestResultRepository resultRepository;

    public PercentileRecalculationListener(TestResultRepository resultRepository) {
        this.resultRepository = resultRepository;
    }

    /**
     * Handle scoring completion by recalculating percentiles for recent results.
     *
     * Runs asynchronously to avoid blocking the main scoring thread.
     * Uses a separate transaction to ensure data consistency.
     *
     * @param event The scoring completed event containing the result ID
     */
    @Async
    @EventListener
    @Transactional
    public void onScoringCompleted(ScoringCompletedEvent event) {
        if (event.resultId() == null) {
            log.debug("Skipping percentile recalculation - no result ID in event");
            return;
        }

        try {
            recalculatePercentilesForTemplate(event.resultId());
        } catch (Exception ex) {
            // Log but don't rethrow - percentile recalculation is non-critical
            log.error("Failed to recalculate percentiles for result {}: {}",
                    event.resultId(), ex.getMessage(), ex);
        }
    }

    /**
     * Recalculate percentiles for all recent results of the same template.
     *
     * @param resultId The result that triggered the recalculation
     */
    private void recalculatePercentilesForTemplate(UUID resultId) {
        // Load result with session and template to get template ID
        TestResult result = resultRepository.findByIdWithSessionAndTemplate(resultId)
                .orElse(null);

        if (result == null) {
            log.warn("Cannot recalculate percentile - result not found: {}", resultId);
            return;
        }

        if (result.getSession() == null || result.getSession().getTemplate() == null) {
            log.warn("Cannot recalculate percentile - session or template not found for result: {}",
                    resultId);
            return;
        }

        UUID templateId = result.getSession().getTemplate().getId();

        // Find all results completed within the recalculation window
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(RECALCULATION_WINDOW_MINUTES);
        List<TestResult> recentResults = resultRepository
                .findByTemplateIdAndCompletedAtAfter(templateId, cutoff);

        if (recentResults.isEmpty()) {
            log.debug("No recent results to recalculate for template {}", templateId);
            return;
        }

        int updatedCount = 0;
        for (TestResult recent : recentResults) {
            int newPercentile = calculateAccuratePercentile(templateId, recent.getOverallPercentage());
            if (!Objects.equals(recent.getPercentile(), newPercentile)) {
                recent.setPercentile(newPercentile);
                resultRepository.save(recent);
                updatedCount++;
            }
        }

        log.info("Recalculated percentiles for {} of {} recent results on template {}",
                updatedCount, recentResults.size(), templateId);
    }

    /**
     * Calculate the accurate percentile for a given score within a template.
     *
     * Percentile represents the percentage of scores that fall below this score.
     * For example, a 75th percentile means the score is higher than 75% of all scores.
     *
     * @param templateId The template to calculate percentile within
     * @param score The score to calculate percentile for
     * @return The calculated percentile (0-100)
     */
    int calculateAccuratePercentile(UUID templateId, Double score) {
        if (score == null) {
            return DEFAULT_PERCENTILE;
        }

        long belowCount = resultRepository.countResultsBelowScore(templateId, score);
        long totalCount = resultRepository.countResultsByTemplateId(templateId);

        if (totalCount <= 1) {
            // First result gets 50th percentile (median)
            return DEFAULT_PERCENTILE;
        }

        double percentile = ((double) belowCount / totalCount) * 100;
        return (int) Math.round(Math.max(0, Math.min(100, percentile)));
    }
}
