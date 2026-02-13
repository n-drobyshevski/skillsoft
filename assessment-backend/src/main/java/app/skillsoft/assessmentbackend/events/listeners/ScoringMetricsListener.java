package app.skillsoft.assessmentbackend.events.listeners;

import app.skillsoft.assessmentbackend.events.scoring.ScoringCompletedEvent;
import app.skillsoft.assessmentbackend.events.scoring.ScoringFailedEvent;
import app.skillsoft.assessmentbackend.events.scoring.ScoringStartedEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Event listener for scoring calculation metrics and observability.
 *
 * Collects and publishes metrics to Micrometer for:
 * - Scoring duration (timer)
 * - Pass/fail rates per goal type (counter)
 * - Score distribution (summary)
 * - Scoring failures by error type (counter)
 *
 * Metrics are tagged by assessment goal for drill-down analysis.
 */
@Component
@Slf4j
public class ScoringMetricsListener {

    private final MeterRegistry registry;

    public ScoringMetricsListener(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * Handle scoring started event for logging and tracking active scoring operations.
     */
    @EventListener
    public void onScoringStarted(ScoringStartedEvent event) {
        log.debug("Scoring started: sessionId={}, goal={}, answerCount={}",
                event.sessionId(), event.goal(), event.answerCount());

        // Track scoring operations started
        Counter.builder("test.scoring.started")
                .description("Count of scoring operations started")
                .tag("goal", event.goal().name())
                .register(registry)
                .increment();

        // Track answer volume being scored
        Counter.builder("test.scoring.answers")
                .description("Total answers processed for scoring")
                .tag("goal", event.goal().name())
                .register(registry)
                .increment(event.answerCount());
    }

    /**
     * Handle successful scoring completion.
     * Records duration, score distribution, and pass/fail metrics.
     */
    @EventListener
    public void onScoringCompleted(ScoringCompletedEvent event) {
        // Record scoring duration
        Timer.builder("test.scoring.duration")
                .description("Time taken to calculate scores")
                .tag("goal", event.goal().name())
                .register(registry)
                .record(event.duration().toNanos(), TimeUnit.NANOSECONDS);

        // Track score distribution
        DistributionSummary.builder("test.scoring.score")
                .description("Distribution of overall scores")
                .tag("goal", event.goal().name())
                .register(registry)
                .record(event.overallScore());

        // Track pass/fail rates
        String outcome = event.passed() ? "passed" : "failed";
        Counter.builder("test.scoring.outcome")
                .description("Count of test outcomes by pass/fail status")
                .tag("goal", event.goal().name())
                .tag("outcome", outcome)
                .register(registry)
                .increment();

        // Track successful completions
        Counter.builder("test.scoring.completed")
                .description("Count of successful scoring operations")
                .tag("goal", event.goal().name())
                .register(registry)
                .increment();

        log.info("Scoring completed: sessionId={}, resultId={}, goal={}, score={}, passed={}, duration={}ms",
                event.sessionId(), event.resultId(), event.goal(),
                String.format("%.2f", event.overallScore()), event.passed(),
                event.duration().toMillis());
    }

    /**
     * Handle scoring failure.
     * Records failure metrics categorized by error type and goal.
     */
    @EventListener
    public void onScoringFailed(ScoringFailedEvent event) {
        // Track failures by error type and goal
        Counter.builder("test.scoring.failures")
                .description("Count of failed scoring operations")
                .tag("goal", event.goal().name())
                .tag("errorType", event.errorType())
                .register(registry)
                .increment();

        // Record duration even for failures
        if (event.duration() != null) {
            Timer.builder("test.scoring.failure.duration")
                    .description("Time taken before scoring failure")
                    .tag("goal", event.goal().name())
                    .tag("errorType", event.errorType())
                    .register(registry)
                    .record(event.duration().toNanos(), TimeUnit.NANOSECONDS);
        }

        log.error("Scoring failed: sessionId={}, goal={}, error={}, errorType={}, duration={}ms",
                event.sessionId(), event.goal(), event.errorMessage(),
                event.errorType(),
                event.duration() != null ? event.duration().toMillis() : "unknown");
    }
}
