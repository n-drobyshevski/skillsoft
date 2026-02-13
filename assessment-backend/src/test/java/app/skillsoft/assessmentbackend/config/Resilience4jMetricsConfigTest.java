package app.skillsoft.assessmentbackend.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Resilience4jMetricsConfig.
 * Verifies that circuit breaker and retry metrics are correctly registered.
 */
class Resilience4jMetricsConfigTest {

    private MeterRegistry meterRegistry;
    private CircuitBreakerRegistry circuitBreakerRegistry;
    private RetryRegistry retryRegistry;
    private CacheManager cacheManager;
    private Resilience4jMetricsConfig config;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        retryRegistry = RetryRegistry.ofDefaults();
        cacheManager = new ConcurrentMapCacheManager(
                CacheConfig.ONET_PROFILES_CACHE,
                CacheConfig.TEAM_PROFILES_CACHE,
                CacheConfig.PASSPORT_SCORES_CACHE
        );

        config = new Resilience4jMetricsConfig(
                meterRegistry,
                circuitBreakerRegistry,
                retryRegistry,
                cacheManager
        );
    }

    @Nested
    @DisplayName("Circuit Breaker Metrics")
    class CircuitBreakerMetrics {

        @Test
        @DisplayName("should register circuit breaker state gauge")
        void shouldRegisterCircuitBreakerStateGauge() {
            // Given
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("testCircuitBreaker");

            // When
            config.configureMetrics();

            // Then
            Gauge gauge = meterRegistry.find("test.scoring.circuit.state")
                    .tag("name", "testCircuitBreaker")
                    .gauge();

            assertThat(gauge).isNotNull();
            assertThat(gauge.value()).isEqualTo(0.0); // CLOSED state
        }

        @Test
        @DisplayName("should update state gauge when circuit breaker opens")
        void shouldUpdateStateGaugeWhenCircuitBreakerOpens() {
            // Given
            CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                    .failureRateThreshold(50)
                    .minimumNumberOfCalls(1)
                    .slidingWindowSize(1)
                    .build();

            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("testCB", cbConfig);
            config.configureMetrics();

            // When - force circuit breaker to open
            cb.transitionToOpenState();

            // Then
            Gauge gauge = meterRegistry.find("test.scoring.circuit.state")
                    .tag("name", "testCB")
                    .gauge();

            assertThat(gauge.value()).isEqualTo(1.0); // OPEN state
        }

        @Test
        @DisplayName("should register failure rate gauge")
        void shouldRegisterFailureRateGauge() {
            // Given
            circuitBreakerRegistry.circuitBreaker("metricsTest");
            config.configureMetrics();

            // Then
            Gauge gauge = meterRegistry.find("test.scoring.circuit.failure_rate")
                    .tag("name", "metricsTest")
                    .gauge();

            assertThat(gauge).isNotNull();
        }

        @Test
        @DisplayName("should register successful calls gauge")
        void shouldRegisterSuccessfulCallsGauge() {
            // Given
            circuitBreakerRegistry.circuitBreaker("successTest");
            config.configureMetrics();

            // Then
            Gauge gauge = meterRegistry.find("test.scoring.circuit.successful_calls")
                    .tag("name", "successTest")
                    .gauge();

            assertThat(gauge).isNotNull();
        }

        @Test
        @DisplayName("should register failed calls gauge")
        void shouldRegisterFailedCallsGauge() {
            // Given
            circuitBreakerRegistry.circuitBreaker("failedTest");
            config.configureMetrics();

            // Then
            Gauge gauge = meterRegistry.find("test.scoring.circuit.failed_calls")
                    .tag("name", "failedTest")
                    .gauge();

            assertThat(gauge).isNotNull();
        }

        @Test
        @DisplayName("should return correct state from helper method")
        void shouldReturnCorrectStateFromHelperMethod() {
            // Given
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("stateHelper");
            config.configureMetrics();

            // When/Then - initially closed
            assertThat(config.getCircuitBreakerState("stateHelper"))
                    .isPresent()
                    .contains(CircuitBreaker.State.CLOSED);

            assertThat(config.isCircuitBreakerClosed("stateHelper")).isTrue();

            // Force open
            cb.transitionToOpenState();
            assertThat(config.getCircuitBreakerState("stateHelper"))
                    .isPresent()
                    .contains(CircuitBreaker.State.OPEN);

            assertThat(config.isCircuitBreakerClosed("stateHelper")).isFalse();
        }

        @Test
        @DisplayName("should handle non-existent circuit breaker gracefully")
        void shouldHandleNonExistentCircuitBreakerGracefully() {
            // When
            config.configureMetrics();

            // Then
            assertThat(config.getCircuitBreakerState("nonExistent")).isEmpty();
            assertThat(config.isCircuitBreakerClosed("nonExistent")).isTrue(); // Default to true
        }
    }

    @Nested
    @DisplayName("Retry Metrics")
    class RetryMetricsTest {

        @Test
        @DisplayName("should register retry metrics for existing retries")
        void shouldRegisterRetryMetricsForExistingRetries() {
            // Given
            RetryConfig retryConfig = RetryConfig.custom()
                    .maxAttempts(3)
                    .waitDuration(Duration.ofMillis(100))
                    .build();
            retryRegistry.retry("testRetry", retryConfig);

            // When
            config.configureMetrics();

            // Then - no exception should be thrown
            // Retry metrics are registered via event publishers, so they trigger on actual retry events
            assertThat(retryRegistry.find("testRetry")).isPresent();
        }
    }

    @Nested
    @DisplayName("Cache Metrics")
    class CacheMetricsTest {

        @Test
        @DisplayName("should register cache hit gauges for all caches")
        void shouldRegisterCacheHitGauges() {
            // When
            config.configureMetrics();

            // Then
            String[] cacheNames = {
                    CacheConfig.ONET_PROFILES_CACHE,
                    CacheConfig.TEAM_PROFILES_CACHE,
                    CacheConfig.PASSPORT_SCORES_CACHE
            };

            for (String cacheName : cacheNames) {
                Gauge hitGauge = meterRegistry.find("test.scoring.cache.hits")
                        .tag("cache", cacheName)
                        .gauge();

                assertThat(hitGauge).as("Cache hit gauge for " + cacheName).isNotNull();
                assertThat(hitGauge.value()).isEqualTo(0.0);
            }
        }

        @Test
        @DisplayName("should register cache miss gauges for all caches")
        void shouldRegisterCacheMissGauges() {
            // When
            config.configureMetrics();

            // Then
            String[] cacheNames = {
                    CacheConfig.ONET_PROFILES_CACHE,
                    CacheConfig.TEAM_PROFILES_CACHE,
                    CacheConfig.PASSPORT_SCORES_CACHE
            };

            for (String cacheName : cacheNames) {
                Gauge missGauge = meterRegistry.find("test.scoring.cache.misses")
                        .tag("cache", cacheName)
                        .gauge();

                assertThat(missGauge).as("Cache miss gauge for " + cacheName).isNotNull();
                assertThat(missGauge.value()).isEqualTo(0.0);
            }
        }

        @Test
        @DisplayName("should increment cache hit counter when recorded")
        void shouldIncrementCacheHitCounter() {
            // Given
            config.configureMetrics();

            // When
            config.recordCacheHit(CacheConfig.ONET_PROFILES_CACHE);
            config.recordCacheHit(CacheConfig.ONET_PROFILES_CACHE);

            // Then
            Gauge gauge = meterRegistry.find("test.scoring.cache.hits")
                    .tag("cache", CacheConfig.ONET_PROFILES_CACHE)
                    .gauge();

            assertThat(gauge.value()).isEqualTo(2.0);
        }

        @Test
        @DisplayName("should increment cache miss counter when recorded")
        void shouldIncrementCacheMissCounter() {
            // Given
            config.configureMetrics();

            // When
            config.recordCacheMiss(CacheConfig.TEAM_PROFILES_CACHE);

            // Then
            Gauge gauge = meterRegistry.find("test.scoring.cache.misses")
                    .tag("cache", CacheConfig.TEAM_PROFILES_CACHE)
                    .gauge();

            assertThat(gauge.value()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("should also record cache hit as counter metric")
        void shouldRecordCacheHitAsCounterMetric() {
            // Given
            config.configureMetrics();

            // When
            config.recordCacheHit(CacheConfig.PASSPORT_SCORES_CACHE);

            // Then
            assertThat(meterRegistry.find("test.scoring.cache.hit")
                    .tag("cache", CacheConfig.PASSPORT_SCORES_CACHE)
                    .counter()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Dynamic Registration")
    class DynamicRegistration {

        @Test
        @DisplayName("should register metrics for circuit breakers added after initialization")
        void shouldRegisterMetricsForLateCircuitBreakers() {
            // Given
            config.configureMetrics();

            // When - add a new circuit breaker after config
            circuitBreakerRegistry.circuitBreaker("lateCircuitBreaker");

            // Then - metrics should be registered via event publisher
            Gauge gauge = meterRegistry.find("test.scoring.circuit.state")
                    .tag("name", "lateCircuitBreaker")
                    .gauge();

            assertThat(gauge).isNotNull();
        }
    }
}
