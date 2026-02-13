package app.skillsoft.assessmentbackend.events.listeners;

import app.skillsoft.assessmentbackend.events.resilience.ResilienceFallbackEvent;
import app.skillsoft.assessmentbackend.events.resilience.ResilienceRetryEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ResilienceMetricsListener.
 * Verifies that retry and fallback events are correctly recorded as metrics.
 */
class ResilienceMetricsListenerTest {

    private MeterRegistry meterRegistry;
    private ResilienceMetricsListener listener;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        listener = new ResilienceMetricsListener(meterRegistry);
    }

    @Nested
    @DisplayName("Retry Event Metrics")
    class RetryEventMetrics {

        @Test
        @DisplayName("should record retry count metric with operation name and attempt number")
        void shouldRecordRetryCountMetric() {
            // Given
            UUID sessionId = UUID.randomUUID();
            ResilienceRetryEvent event = ResilienceRetryEvent.forScoringRetry(
                    sessionId, 1, 3, new RuntimeException("Database timeout"));

            // When
            listener.onRetryEvent(event);

            // Then
            Counter counter = meterRegistry.find("test.scoring.retry.count")
                    .tag("name", "scoringCalculation")
                    .tag("attempt", "1")
                    .counter();

            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("should record retry total metric with error type")
        void shouldRecordRetryTotalMetric() {
            // Given
            UUID sessionId = UUID.randomUUID();
            ResilienceRetryEvent event = ResilienceRetryEvent.forScoringRetry(
                    sessionId, 2, 3, new IllegalStateException("Connection lost"));

            // When
            listener.onRetryEvent(event);

            // Then
            Counter counter = meterRegistry.find("test.scoring.retry.total")
                    .tag("name", "scoringCalculation")
                    .tag("errorType", "IllegalStateException")
                    .counter();

            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("should track multiple retry attempts separately")
        void shouldTrackMultipleRetryAttempts() {
            // Given
            UUID sessionId = UUID.randomUUID();
            RuntimeException error = new RuntimeException("Transient failure");

            // When - simulate 3 retry attempts
            listener.onRetryEvent(ResilienceRetryEvent.forScoringRetry(sessionId, 1, 3, error));
            listener.onRetryEvent(ResilienceRetryEvent.forScoringRetry(sessionId, 2, 3, error));
            listener.onRetryEvent(ResilienceRetryEvent.forScoringRetry(sessionId, 3, 3, error));

            // Then
            Counter attempt1 = meterRegistry.find("test.scoring.retry.count")
                    .tag("name", "scoringCalculation")
                    .tag("attempt", "1")
                    .counter();
            Counter attempt2 = meterRegistry.find("test.scoring.retry.count")
                    .tag("name", "scoringCalculation")
                    .tag("attempt", "2")
                    .counter();
            Counter attempt3 = meterRegistry.find("test.scoring.retry.count")
                    .tag("name", "scoringCalculation")
                    .tag("attempt", "3")
                    .counter();

            assertThat(attempt1.count()).isEqualTo(1.0);
            assertThat(attempt2.count()).isEqualTo(1.0);
            assertThat(attempt3.count()).isEqualTo(1.0);

            // Total should be 3
            Counter total = meterRegistry.find("test.scoring.retry.total")
                    .tag("name", "scoringCalculation")
                    .counter();
            assertThat(total.count()).isEqualTo(3.0);
        }
    }

    @Nested
    @DisplayName("Fallback Event Metrics")
    class FallbackEventMetrics {

        @Test
        @DisplayName("should record fallback count metric with operation name and reason")
        void shouldRecordFallbackCountMetric() {
            // Given
            UUID sessionId = UUID.randomUUID();
            ResilienceFallbackEvent event = ResilienceFallbackEvent.forScoringFallback(
                    sessionId, 3, new RuntimeException("All retries exhausted"));

            // When
            listener.onFallbackEvent(event);

            // Then
            Counter counter = meterRegistry.find("test.scoring.fallback.count")
                    .tag("name", "scoringCalculation")
                    .tag("reason", "retry_exhausted")
                    .counter();

            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("should record fallback by error type metric")
        void shouldRecordFallbackByErrorTypeMetric() {
            // Given
            UUID sessionId = UUID.randomUUID();
            ResilienceFallbackEvent event = ResilienceFallbackEvent.forScoringFallback(
                    sessionId, 3, new IllegalStateException("Service unavailable"));

            // When
            listener.onFallbackEvent(event);

            // Then
            Counter counter = meterRegistry.find("test.scoring.fallback.by_error")
                    .tag("name", "scoringCalculation")
                    .tag("errorType", "IllegalStateException")
                    .counter();

            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("should record fallback for circuit open reason")
        void shouldRecordFallbackForCircuitOpen() {
            // Given
            UUID sessionId = UUID.randomUUID();
            ResilienceFallbackEvent event = ResilienceFallbackEvent.circuitOpen(
                    "competencyLoader", sessionId, new RuntimeException("Circuit is open"));

            // When
            listener.onFallbackEvent(event);

            // Then
            Counter counter = meterRegistry.find("test.scoring.fallback.count")
                    .tag("name", "competencyLoader")
                    .tag("reason", "circuit_open")
                    .counter();

            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("should accumulate multiple fallback events")
        void shouldAccumulateFallbackEvents() {
            // Given
            RuntimeException error = new RuntimeException("Failure");

            // When - multiple fallback invocations
            listener.onFallbackEvent(ResilienceFallbackEvent.forScoringFallback(UUID.randomUUID(), 3, error));
            listener.onFallbackEvent(ResilienceFallbackEvent.forScoringFallback(UUID.randomUUID(), 3, error));
            listener.onFallbackEvent(ResilienceFallbackEvent.forScoringFallback(UUID.randomUUID(), 3, error));

            // Then
            Counter counter = meterRegistry.find("test.scoring.fallback.count")
                    .tag("name", "scoringCalculation")
                    .tag("reason", "retry_exhausted")
                    .counter();

            assertThat(counter.count()).isEqualTo(3.0);
        }
    }

    @Nested
    @DisplayName("Event Factory Methods")
    class EventFactoryMethods {

        @Test
        @DisplayName("ResilienceRetryEvent.fromException should extract error details")
        void retryEventShouldExtractErrorDetails() {
            // Given
            UUID sessionId = UUID.randomUUID();
            Exception exception = new IllegalArgumentException("Invalid input");

            // When
            ResilienceRetryEvent event = ResilienceRetryEvent.fromException(
                    "testOperation", sessionId, 2, 5, exception);

            // Then
            assertThat(event.operationName()).isEqualTo("testOperation");
            assertThat(event.sessionId()).isEqualTo(sessionId);
            assertThat(event.attemptNumber()).isEqualTo(2);
            assertThat(event.maxAttempts()).isEqualTo(5);
            assertThat(event.errorType()).isEqualTo("IllegalArgumentException");
            assertThat(event.errorMessage()).isEqualTo("Invalid input");
            assertThat(event.timestamp()).isNotNull();
        }

        @Test
        @DisplayName("ResilienceRetryEvent should handle null exception")
        void retryEventShouldHandleNullException() {
            // When
            ResilienceRetryEvent event = ResilienceRetryEvent.fromException(
                    "testOperation", null, 1, 3, null);

            // Then
            assertThat(event.errorType()).isEqualTo("Unknown");
            assertThat(event.errorMessage()).isEqualTo("Unknown error");
        }

        @Test
        @DisplayName("ResilienceFallbackEvent.retryExhausted should set correct reason")
        void fallbackEventShouldSetRetryExhaustedReason() {
            // When
            ResilienceFallbackEvent event = ResilienceFallbackEvent.retryExhausted(
                    "scoringCalculation", UUID.randomUUID(), 3, new RuntimeException("Failed"));

            // Then
            assertThat(event.reason()).isEqualTo(ResilienceFallbackEvent.FallbackReason.RETRY_EXHAUSTED);
            assertThat(event.totalAttempts()).isEqualTo(3);
        }

        @Test
        @DisplayName("ResilienceFallbackEvent.circuitOpen should set correct reason")
        void fallbackEventShouldSetCircuitOpenReason() {
            // When
            ResilienceFallbackEvent event = ResilienceFallbackEvent.circuitOpen(
                    "competencyLoader", UUID.randomUUID(), new RuntimeException("Circuit open"));

            // Then
            assertThat(event.reason()).isEqualTo(ResilienceFallbackEvent.FallbackReason.CIRCUIT_OPEN);
            assertThat(event.totalAttempts()).isEqualTo(0);
        }
    }
}
