package app.skillsoft.assessmentbackend.events.listeners;

import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;
import app.skillsoft.assessmentbackend.events.scoring.ScoringCompletedEvent;
import app.skillsoft.assessmentbackend.events.scoring.ScoringFailedEvent;
import app.skillsoft.assessmentbackend.events.scoring.ScoringStartedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ScoringMetricsListener.
 *
 * Tests verify:
 * 1. Counter increments for started/completed/failed operations
 * 2. Timer recordings for scoring duration
 * 3. Distribution summary for score values
 * 4. Correct metric tags for goal types and outcomes
 *
 * Per CLAUDE.md: Uses JUnit 5 + AssertJ for unit testing.
 */
@DisplayName("ScoringMetricsListener Unit Tests")
class ScoringMetricsListenerTest {

    private MeterRegistry meterRegistry;
    private ScoringMetricsListener listener;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        listener = new ScoringMetricsListener(meterRegistry);
    }

    @Nested
    @DisplayName("ScoringStartedEvent Handling")
    class ScoringStartedEventHandling {

        @Test
        @DisplayName("should increment test.scoring.started counter on event")
        void shouldIncrementStartedCounterOnEvent() {
            // Given
            ScoringStartedEvent event = ScoringStartedEvent.now(
                    UUID.randomUUID(), null, AssessmentGoal.OVERVIEW, 10);

            // When
            listener.onScoringStarted(event);

            // Then
            double count = meterRegistry.find("test.scoring.started")
                    .tag("goal", "OVERVIEW")
                    .counter()
                    .count();
            assertThat(count).isEqualTo(1.0);
        }

        @Test
        @DisplayName("should increment test.scoring.answers counter with answer count")
        void shouldIncrementAnswersCounterWithAnswerCount() {
            // Given
            int answerCount = 25;
            ScoringStartedEvent event = ScoringStartedEvent.now(
                    UUID.randomUUID(), null, AssessmentGoal.JOB_FIT, answerCount);

            // When
            listener.onScoringStarted(event);

            // Then
            double count = meterRegistry.find("test.scoring.answers")
                    .tag("goal", "JOB_FIT")
                    .counter()
                    .count();
            assertThat(count).isEqualTo((double) answerCount);
        }

        @Test
        @DisplayName("should tag metrics with correct assessment goal")
        void shouldTagMetricsWithCorrectGoal() {
            // Given
            ScoringStartedEvent overviewEvent = ScoringStartedEvent.now(
                    UUID.randomUUID(), null, AssessmentGoal.OVERVIEW, 5);
            ScoringStartedEvent jobFitEvent = ScoringStartedEvent.now(
                    UUID.randomUUID(), null, AssessmentGoal.JOB_FIT, 5);
            ScoringStartedEvent teamFitEvent = ScoringStartedEvent.now(
                    UUID.randomUUID(), null, AssessmentGoal.TEAM_FIT, 5);

            // When
            listener.onScoringStarted(overviewEvent);
            listener.onScoringStarted(jobFitEvent);
            listener.onScoringStarted(teamFitEvent);

            // Then
            assertThat(meterRegistry.find("test.scoring.started").tag("goal", "OVERVIEW").counter().count()).isEqualTo(1.0);
            assertThat(meterRegistry.find("test.scoring.started").tag("goal", "JOB_FIT").counter().count()).isEqualTo(1.0);
            assertThat(meterRegistry.find("test.scoring.started").tag("goal", "TEAM_FIT").counter().count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("should accumulate counters across multiple events")
        void shouldAccumulateCountersAcrossMultipleEvents() {
            // Given
            ScoringStartedEvent event1 = ScoringStartedEvent.now(
                    UUID.randomUUID(), null, AssessmentGoal.OVERVIEW, 10);
            ScoringStartedEvent event2 = ScoringStartedEvent.now(
                    UUID.randomUUID(), null, AssessmentGoal.OVERVIEW, 15);

            // When
            listener.onScoringStarted(event1);
            listener.onScoringStarted(event2);

            // Then
            assertThat(meterRegistry.find("test.scoring.started").tag("goal", "OVERVIEW").counter().count()).isEqualTo(2.0);
            assertThat(meterRegistry.find("test.scoring.answers").tag("goal", "OVERVIEW").counter().count()).isEqualTo(25.0);
        }
    }

    @Nested
    @DisplayName("ScoringCompletedEvent Handling")
    class ScoringCompletedEventHandling {

        @Test
        @DisplayName("should record duration in test.scoring.duration timer")
        void shouldRecordDurationInTimer() {
            // Given
            Duration duration = Duration.ofMillis(500);
            ScoringCompletedEvent event = ScoringCompletedEvent.now(
                    UUID.randomUUID(), UUID.randomUUID(), AssessmentGoal.OVERVIEW, 85.5, true, duration);

            // When
            listener.onScoringCompleted(event);

            // Then
            var timer = meterRegistry.find("test.scoring.duration")
                    .tag("goal", "OVERVIEW")
                    .timer();
            assertThat(timer).isNotNull();
            assertThat(timer.count()).isEqualTo(1);
            assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isGreaterThanOrEqualTo(500);
        }

        @Test
        @DisplayName("should record score in distribution summary")
        void shouldRecordScoreInDistributionSummary() {
            // Given
            double score = 92.5;
            ScoringCompletedEvent event = ScoringCompletedEvent.now(
                    UUID.randomUUID(), UUID.randomUUID(), AssessmentGoal.JOB_FIT, score, true, Duration.ofMillis(100));

            // When
            listener.onScoringCompleted(event);

            // Then
            var summary = meterRegistry.find("test.scoring.score")
                    .tag("goal", "JOB_FIT")
                    .summary();
            assertThat(summary).isNotNull();
            assertThat(summary.count()).isEqualTo(1);
            assertThat(summary.totalAmount()).isEqualTo(score);
        }

        @Test
        @DisplayName("should increment test.scoring.completed counter")
        void shouldIncrementCompletedCounter() {
            // Given
            ScoringCompletedEvent event = ScoringCompletedEvent.now(
                    UUID.randomUUID(), UUID.randomUUID(), AssessmentGoal.TEAM_FIT, 75.0, true, Duration.ofMillis(200));

            // When
            listener.onScoringCompleted(event);

            // Then
            double count = meterRegistry.find("test.scoring.completed")
                    .tag("goal", "TEAM_FIT")
                    .counter()
                    .count();
            assertThat(count).isEqualTo(1.0);
        }

        @Test
        @DisplayName("should increment test.scoring.outcome counter with passed status")
        void shouldIncrementOutcomeCounterWithPassedStatus() {
            // Given
            ScoringCompletedEvent passedEvent = ScoringCompletedEvent.now(
                    UUID.randomUUID(), UUID.randomUUID(), AssessmentGoal.OVERVIEW, 85.0, true, Duration.ofMillis(100));
            ScoringCompletedEvent failedEvent = ScoringCompletedEvent.now(
                    UUID.randomUUID(), UUID.randomUUID(), AssessmentGoal.OVERVIEW, 45.0, false, Duration.ofMillis(100));

            // When
            listener.onScoringCompleted(passedEvent);
            listener.onScoringCompleted(failedEvent);

            // Then
            assertThat(meterRegistry.find("test.scoring.outcome").tag("goal", "OVERVIEW").tag("outcome", "passed").counter().count()).isEqualTo(1.0);
            assertThat(meterRegistry.find("test.scoring.outcome").tag("goal", "OVERVIEW").tag("outcome", "failed").counter().count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("should track statistics across multiple scoring operations")
        void shouldTrackStatisticsAcrossMultipleScoringOperations() {
            // Given & When
            for (int i = 0; i < 5; i++) {
                ScoringCompletedEvent event = ScoringCompletedEvent.now(
                        UUID.randomUUID(), UUID.randomUUID(), AssessmentGoal.OVERVIEW,
                        70.0 + i * 5, i >= 3, Duration.ofMillis(100 + i * 50));
                listener.onScoringCompleted(event);
            }

            // Then
            var timer = meterRegistry.find("test.scoring.duration").tag("goal", "OVERVIEW").timer();
            assertThat(timer.count()).isEqualTo(5);

            var summary = meterRegistry.find("test.scoring.score").tag("goal", "OVERVIEW").summary();
            assertThat(summary.count()).isEqualTo(5);
            assertThat(summary.mean()).isCloseTo(80.0, org.assertj.core.data.Percentage.withPercentage(1));

            assertThat(meterRegistry.find("test.scoring.completed").tag("goal", "OVERVIEW").counter().count()).isEqualTo(5.0);
        }
    }

    @Nested
    @DisplayName("ScoringFailedEvent Handling")
    class ScoringFailedEventHandling {

        @Test
        @DisplayName("should increment test.scoring.failures counter")
        void shouldIncrementFailuresCounter() {
            // Given
            ScoringFailedEvent event = ScoringFailedEvent.fromException(
                    UUID.randomUUID(), AssessmentGoal.OVERVIEW, new RuntimeException("Test error"), Instant.now());

            // When
            listener.onScoringFailed(event);

            // Then
            double count = meterRegistry.find("test.scoring.failures")
                    .tag("goal", "OVERVIEW")
                    .tag("errorType", "RuntimeException")
                    .counter()
                    .count();
            assertThat(count).isEqualTo(1.0);
        }

        @Test
        @DisplayName("should tag failures by error type")
        void shouldTagFailuresByErrorType() {
            // Given
            ScoringFailedEvent runtimeEvent = ScoringFailedEvent.fromException(
                    UUID.randomUUID(), AssessmentGoal.OVERVIEW, new RuntimeException("Runtime error"), Instant.now());
            ScoringFailedEvent illegalEvent = ScoringFailedEvent.fromException(
                    UUID.randomUUID(), AssessmentGoal.OVERVIEW, new IllegalStateException("Illegal state"), Instant.now());

            // When
            listener.onScoringFailed(runtimeEvent);
            listener.onScoringFailed(illegalEvent);

            // Then
            assertThat(meterRegistry.find("test.scoring.failures").tag("errorType", "RuntimeException").counter().count()).isEqualTo(1.0);
            assertThat(meterRegistry.find("test.scoring.failures").tag("errorType", "IllegalStateException").counter().count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("should record failure duration when provided")
        void shouldRecordFailureDurationWhenProvided() {
            // Given
            ScoringFailedEvent event = ScoringFailedEvent.withError(
                    UUID.randomUUID(), AssessmentGoal.JOB_FIT, "Test error", "TestException", Duration.ofMillis(250));

            // When
            listener.onScoringFailed(event);

            // Then
            var timer = meterRegistry.find("test.scoring.failure.duration")
                    .tag("goal", "JOB_FIT")
                    .tag("errorType", "TestException")
                    .timer();
            assertThat(timer).isNotNull();
            assertThat(timer.count()).isEqualTo(1);
            assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isGreaterThanOrEqualTo(250);
        }

        @Test
        @DisplayName("should not record failure duration when null")
        void shouldNotRecordFailureDurationWhenNull() {
            // Given - Event without duration
            ScoringFailedEvent event = new ScoringFailedEvent(
                    UUID.randomUUID(), AssessmentGoal.TEAM_FIT, "Error message", "NullPointerException", null, Instant.now());

            // When
            listener.onScoringFailed(event);

            // Then - Failures counter should be incremented
            assertThat(meterRegistry.find("test.scoring.failures").tag("errorType", "NullPointerException").counter().count()).isEqualTo(1.0);

            // But no duration timer should be recorded
            var timer = meterRegistry.find("test.scoring.failure.duration")
                    .tag("goal", "TEAM_FIT")
                    .tag("errorType", "NullPointerException")
                    .timer();
            // Timer may not exist or have 0 count
            assertThat(timer == null || timer.count() == 0).isTrue();
        }
    }

    @Nested
    @DisplayName("Metrics by Assessment Goal")
    class MetricsByAssessmentGoal {

        @Test
        @DisplayName("should track separate metrics for each goal type")
        void shouldTrackSeparateMetricsForEachGoalType() {
            // Given & When - Create events for each goal type
            for (AssessmentGoal goal : AssessmentGoal.values()) {
                listener.onScoringStarted(ScoringStartedEvent.now(
                        UUID.randomUUID(), null, goal, 10));
                listener.onScoringCompleted(ScoringCompletedEvent.now(
                        UUID.randomUUID(), UUID.randomUUID(), goal, 80.0, true, Duration.ofMillis(100)));
            }

            // Then - Each goal should have its own metrics
            for (AssessmentGoal goal : AssessmentGoal.values()) {
                assertThat(meterRegistry.find("test.scoring.started").tag("goal", goal.name()).counter().count()).isEqualTo(1.0);
                assertThat(meterRegistry.find("test.scoring.completed").tag("goal", goal.name()).counter().count()).isEqualTo(1.0);
            }
        }
    }
}
