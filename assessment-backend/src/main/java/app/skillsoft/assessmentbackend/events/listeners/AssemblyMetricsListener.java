package app.skillsoft.assessmentbackend.events.listeners;

import app.skillsoft.assessmentbackend.events.assembly.AssemblyCompletedEvent;
import app.skillsoft.assessmentbackend.events.assembly.AssemblyFailedEvent;
import app.skillsoft.assessmentbackend.events.assembly.AssemblyStartedEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Event listener for test assembly metrics and observability.
 *
 * Collects and publishes metrics to Micrometer for:
 * - Assembly duration (timer)
 * - Question counts per goal type (counter)
 * - Assembly failures by error type (counter)
 *
 * Metrics are tagged by assessment goal for drill-down analysis.
 */
@Component
@Slf4j
public class AssemblyMetricsListener {

    private final MeterRegistry registry;

    public AssemblyMetricsListener(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * Handle assembly started event for logging and tracking active assemblies.
     */
    @EventListener
    public void onAssemblyStarted(AssemblyStartedEvent event) {
        log.debug("Assembly started: sessionId={}, templateId={}, goal={}, assembler={}",
                event.sessionId(), event.templateId(), event.goal(), event.assemblerType());

        // Track active assemblies (can be used for concurrent assembly monitoring)
        Counter.builder("test.assembly.started")
                .description("Count of assembly operations started")
                .tag("goal", event.goal().name())
                .tag("assembler", event.assemblerType())
                .register(registry)
                .increment();
    }

    /**
     * Handle successful assembly completion.
     * Records duration and question count metrics.
     */
    @EventListener
    public void onAssemblyCompleted(AssemblyCompletedEvent event) {
        // Record assembly duration
        Timer.builder("test.assembly.duration")
                .description("Time taken to assemble test questions")
                .tag("goal", event.goal().name())
                .register(registry)
                .record(event.duration().toNanos(), TimeUnit.NANOSECONDS);

        // Track question counts
        Counter.builder("test.assembly.questions")
                .description("Total questions assembled across all sessions")
                .tag("goal", event.goal().name())
                .register(registry)
                .increment(event.questionCount());

        // Track successful completions
        Counter.builder("test.assembly.completed")
                .description("Count of successful assembly operations")
                .tag("goal", event.goal().name())
                .register(registry)
                .increment();

        log.info("Assembly completed: sessionId={}, goal={}, questions={}, duration={}ms",
                event.sessionId(), event.goal(), event.questionCount(),
                event.duration().toMillis());
    }

    /**
     * Handle assembly failure.
     * Records failure metrics categorized by error type and goal.
     */
    @EventListener
    public void onAssemblyFailed(AssemblyFailedEvent event) {
        // Track failures by error type and goal
        Counter.builder("test.assembly.failures")
                .description("Count of failed assembly operations")
                .tag("goal", event.goal().name())
                .tag("errorType", event.errorType())
                .register(registry)
                .increment();

        // Record duration even for failures (useful for timeout analysis)
        if (event.duration() != null) {
            Timer.builder("test.assembly.failure.duration")
                    .description("Time taken before assembly failure")
                    .tag("goal", event.goal().name())
                    .tag("errorType", event.errorType())
                    .register(registry)
                    .record(event.duration().toNanos(), TimeUnit.NANOSECONDS);
        }

        log.error("Assembly failed: sessionId={}, goal={}, error={}, errorType={}, duration={}ms",
                event.sessionId(), event.goal(), event.errorMessage(),
                event.errorType(),
                event.duration() != null ? event.duration().toMillis() : "unknown");
    }
}
