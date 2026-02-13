package app.skillsoft.assessmentbackend.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Configuration for Resilience4j metrics integration with Micrometer.
 *
 * Provides custom metrics for:
 * - Circuit breaker state (gauge: 0=closed, 1=open, 2=half-open)
 * - Circuit breaker failure rates
 * - Circuit breaker call counts
 * - Cache hit/miss counters for fallback operations
 *
 * Metrics follow the naming convention: test.scoring.*
 */
@Component
@Slf4j
public class Resilience4jMetricsConfig {

    private final MeterRegistry meterRegistry;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final CacheManager cacheManager;

    // Track cache metrics per operation
    private final Map<String, AtomicInteger> cacheHitCounters = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> cacheMissCounters = new ConcurrentHashMap<>();

    public Resilience4jMetricsConfig(
            MeterRegistry meterRegistry,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            CacheManager cacheManager) {
        this.meterRegistry = meterRegistry;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
        this.cacheManager = cacheManager;
    }

    @PostConstruct
    public void configureMetrics() {
        log.info("Configuring Resilience4j metrics for circuit breakers and retries");

        // Register metrics for all existing circuit breakers
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(this::registerCircuitBreakerMetrics);

        // Register event consumers to track new circuit breakers
        circuitBreakerRegistry.getEventPublisher()
                .onEntryAdded(event -> registerCircuitBreakerMetrics(event.getAddedEntry()));

        // Register metrics for all existing retry instances
        retryRegistry.getAllRetries().forEach(this::registerRetryMetrics);

        // Register event consumers for new retries
        retryRegistry.getEventPublisher()
                .onEntryAdded(event -> registerRetryMetrics(event.getAddedEntry()));

        // Register cache metrics
        registerCacheMetrics();

        log.info("Resilience4j metrics configuration completed");
    }

    /**
     * Register circuit breaker specific metrics.
     */
    private void registerCircuitBreakerMetrics(CircuitBreaker circuitBreaker) {
        String name = circuitBreaker.getName();
        log.debug("Registering metrics for circuit breaker: {}", name);

        // Circuit breaker state gauge (0=closed, 1=open, 2=half-open)
        Gauge.builder("test.scoring.circuit.state", circuitBreaker, cb -> {
            return switch (cb.getState()) {
                case CLOSED -> 0;
                case OPEN -> 1;
                case HALF_OPEN -> 2;
                case DISABLED -> -1;
                case FORCED_OPEN -> 3;
                case METRICS_ONLY -> 4;
            };
        })
                .description("Circuit breaker state (0=closed, 1=open, 2=half-open, -1=disabled, 3=forced_open)")
                .tag("name", name)
                .register(meterRegistry);

        // Failure rate gauge
        Gauge.builder("test.scoring.circuit.failure_rate", circuitBreaker,
                cb -> cb.getMetrics().getFailureRate())
                .description("Circuit breaker failure rate percentage")
                .tag("name", name)
                .register(meterRegistry);

        // Slow call rate gauge
        Gauge.builder("test.scoring.circuit.slow_call_rate", circuitBreaker,
                cb -> cb.getMetrics().getSlowCallRate())
                .description("Circuit breaker slow call rate percentage")
                .tag("name", name)
                .register(meterRegistry);

        // Number of successful calls gauge
        Gauge.builder("test.scoring.circuit.successful_calls", circuitBreaker,
                cb -> cb.getMetrics().getNumberOfSuccessfulCalls())
                .description("Number of successful calls in the sliding window")
                .tag("name", name)
                .register(meterRegistry);

        // Number of failed calls gauge
        Gauge.builder("test.scoring.circuit.failed_calls", circuitBreaker,
                cb -> cb.getMetrics().getNumberOfFailedCalls())
                .description("Number of failed calls in the sliding window")
                .tag("name", name)
                .register(meterRegistry);

        // Number of not permitted calls (when open)
        Gauge.builder("test.scoring.circuit.not_permitted_calls", circuitBreaker,
                cb -> cb.getMetrics().getNumberOfNotPermittedCalls())
                .description("Number of calls not permitted due to open circuit")
                .tag("name", name)
                .register(meterRegistry);

        // Register event consumers for state transitions
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> {
                    log.info("Circuit breaker '{}' state transition: {} -> {}",
                            name,
                            event.getStateTransition().getFromState(),
                            event.getStateTransition().getToState());

                    // Track state transitions as counter
                    Counter.builder("test.scoring.circuit.state_transition")
                            .description("Circuit breaker state transitions")
                            .tag("name", name)
                            .tag("from", event.getStateTransition().getFromState().name())
                            .tag("to", event.getStateTransition().getToState().name())
                            .register(meterRegistry)
                            .increment();
                });

        // Track calls on success, error, and ignored errors
        circuitBreaker.getEventPublisher()
                .onSuccess(event -> Counter.builder("test.scoring.circuit.call")
                        .description("Circuit breaker call outcomes")
                        .tag("name", name)
                        .tag("outcome", "success")
                        .register(meterRegistry)
                        .increment())
                .onError(event -> Counter.builder("test.scoring.circuit.call")
                        .description("Circuit breaker call outcomes")
                        .tag("name", name)
                        .tag("outcome", "error")
                        .register(meterRegistry)
                        .increment())
                .onCallNotPermitted(event -> Counter.builder("test.scoring.circuit.call")
                        .description("Circuit breaker call outcomes")
                        .tag("name", name)
                        .tag("outcome", "not_permitted")
                        .register(meterRegistry)
                        .increment());
    }

    /**
     * Register retry specific metrics via event publisher.
     */
    private void registerRetryMetrics(Retry retry) {
        String name = retry.getName();
        log.debug("Registering metrics for retry: {}", name);

        // Track retry events
        retry.getEventPublisher()
                .onRetry(event -> {
                    log.debug("Retry '{}' attempt {} due to: {}",
                            name,
                            event.getNumberOfRetryAttempts(),
                            event.getLastThrowable() != null
                                    ? event.getLastThrowable().getMessage()
                                    : "unknown");

                    Counter.builder("test.scoring.retry.attempt")
                            .description("Individual retry attempts from Resilience4j")
                            .tag("name", name)
                            .tag("attempt", String.valueOf(event.getNumberOfRetryAttempts()))
                            .register(meterRegistry)
                            .increment();
                })
                .onSuccess(event -> Counter.builder("test.scoring.retry.outcome")
                        .description("Retry operation outcomes")
                        .tag("name", name)
                        .tag("outcome", "success")
                        .tag("attempts", String.valueOf(event.getNumberOfRetryAttempts()))
                        .register(meterRegistry)
                        .increment())
                .onError(event -> Counter.builder("test.scoring.retry.outcome")
                        .description("Retry operation outcomes")
                        .tag("name", name)
                        .tag("outcome", "exhausted")
                        .tag("attempts", String.valueOf(event.getNumberOfRetryAttempts()))
                        .register(meterRegistry)
                        .increment())
                .onIgnoredError(event -> Counter.builder("test.scoring.retry.outcome")
                        .description("Retry operation outcomes")
                        .tag("name", name)
                        .tag("outcome", "ignored_error")
                        .tag("attempts", String.valueOf(event.getNumberOfRetryAttempts()))
                        .register(meterRegistry)
                        .increment());
    }

    /**
     * Register cache hit/miss metrics for fallback operations.
     */
    private void registerCacheMetrics() {
        // Register counters for cache operations during fallback
        String[] cacheNames = {
                CacheConfig.ONET_PROFILES_CACHE,
                CacheConfig.TEAM_PROFILES_CACHE,
                CacheConfig.PASSPORT_SCORES_CACHE
        };

        for (String cacheName : cacheNames) {
            // Initialize atomic counters for tracking
            cacheHitCounters.put(cacheName, new AtomicInteger(0));
            cacheMissCounters.put(cacheName, new AtomicInteger(0));

            // Register gauges that read from atomic counters
            Gauge.builder("test.scoring.cache.hits", cacheHitCounters.get(cacheName), AtomicInteger::get)
                    .description("Cache hits during fallback operations")
                    .tag("cache", cacheName)
                    .register(meterRegistry);

            Gauge.builder("test.scoring.cache.misses", cacheMissCounters.get(cacheName), AtomicInteger::get)
                    .description("Cache misses during fallback operations")
                    .tag("cache", cacheName)
                    .register(meterRegistry);
        }
    }

    /**
     * Record a cache hit for fallback metrics.
     * Call this when using cached data during circuit breaker fallback.
     *
     * @param cacheName The name of the cache that was hit
     */
    public void recordCacheHit(String cacheName) {
        AtomicInteger counter = cacheHitCounters.get(cacheName);
        if (counter != null) {
            counter.incrementAndGet();
        }
        Counter.builder("test.scoring.cache.hit")
                .description("Cache hits during resilience fallback")
                .tag("cache", cacheName)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record a cache miss for fallback metrics.
     * Call this when cache lookup fails during circuit breaker fallback.
     *
     * @param cacheName The name of the cache that was missed
     */
    public void recordCacheMiss(String cacheName) {
        AtomicInteger counter = cacheMissCounters.get(cacheName);
        if (counter != null) {
            counter.incrementAndGet();
        }
        Counter.builder("test.scoring.cache.miss")
                .description("Cache misses during resilience fallback")
                .tag("cache", cacheName)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Get the current state of a circuit breaker by name.
     *
     * @param name The circuit breaker name
     * @return Optional containing the state, or empty if not found
     */
    public Optional<CircuitBreaker.State> getCircuitBreakerState(String name) {
        return circuitBreakerRegistry.find(name)
                .map(CircuitBreaker::getState);
    }

    /**
     * Check if a circuit breaker is currently allowing calls.
     *
     * @param name The circuit breaker name
     * @return true if calls are permitted, false otherwise
     */
    public boolean isCircuitBreakerClosed(String name) {
        return circuitBreakerRegistry.find(name)
                .map(cb -> cb.getState() == CircuitBreaker.State.CLOSED)
                .orElse(true);
    }
}
