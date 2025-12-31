package app.skillsoft.assessmentbackend.events.listeners;

import app.skillsoft.assessmentbackend.events.resilience.ResilienceFallbackEvent;
import app.skillsoft.assessmentbackend.events.resilience.ResilienceRetryEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Event listener for resilience metrics and observability.
 *
 * Collects and publishes metrics to Micrometer for:
 * - Retry attempts (counter by operation and attempt number)
 * - Fallback invocations (counter by operation and reason)
 *
 * Metrics are tagged by operation name for drill-down analysis.
 * Circuit breaker state metrics are handled by Resilience4jMetricsConfig.
 */
@Component
@Slf4j
public class ResilienceMetricsListener {

    private final MeterRegistry registry;

    public ResilienceMetricsListener(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * Handle retry event from resilience-protected operations.
     * Records metrics for retry attempts with operation and attempt context.
     */
    @EventListener
    public void onRetryEvent(ResilienceRetryEvent event) {
        log.warn("Resilience retry: operation={}, attempt={}/{}, sessionId={}, error={}",
                event.operationName(),
                event.attemptNumber(),
                event.maxAttempts(),
                event.sessionId(),
                event.errorMessage());

        // Track retry attempts by operation and attempt number
        Counter.builder("test.scoring.retry.count")
                .description("Count of retry attempts in resilience-protected operations")
                .tag("name", event.operationName())
                .tag("attempt", String.valueOf(event.attemptNumber()))
                .register(registry)
                .increment();

        // Track total retries by operation (for simpler queries)
        Counter.builder("test.scoring.retry.total")
                .description("Total retry attempts across all operations")
                .tag("name", event.operationName())
                .tag("errorType", event.errorType())
                .register(registry)
                .increment();
    }

    /**
     * Handle fallback event when resilience mechanisms are exhausted.
     * Records metrics for fallback invocations with operation and reason context.
     */
    @EventListener
    public void onFallbackEvent(ResilienceFallbackEvent event) {
        log.error("Resilience fallback invoked: operation={}, reason={}, sessionId={}, " +
                  "totalAttempts={}, error={}",
                event.operationName(),
                event.reason(),
                event.sessionId(),
                event.totalAttempts(),
                event.errorMessage());

        // Track fallback invocations by operation and reason
        Counter.builder("test.scoring.fallback.count")
                .description("Count of fallback method invocations")
                .tag("name", event.operationName())
                .tag("reason", event.reason().name().toLowerCase())
                .register(registry)
                .increment();

        // Track fallbacks by error type for debugging
        Counter.builder("test.scoring.fallback.by_error")
                .description("Fallback invocations by error type")
                .tag("name", event.operationName())
                .tag("errorType", event.errorType())
                .register(registry)
                .increment();
    }
}
