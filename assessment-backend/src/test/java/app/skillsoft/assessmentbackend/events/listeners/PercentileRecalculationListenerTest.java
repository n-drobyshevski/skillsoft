package app.skillsoft.assessmentbackend.events.listeners;

import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;
import app.skillsoft.assessmentbackend.domain.entities.TestResult;
import app.skillsoft.assessmentbackend.domain.entities.TestSession;
import app.skillsoft.assessmentbackend.domain.entities.TestTemplate;
import app.skillsoft.assessmentbackend.events.scoring.ScoringCompletedEvent;
import app.skillsoft.assessmentbackend.repository.TestResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PercentileRecalculationListener.
 * Verifies that percentiles are correctly recalculated asynchronously
 * after scoring completes.
 */
@ExtendWith(MockitoExtension.class)
class PercentileRecalculationListenerTest {

    @Mock
    private TestResultRepository resultRepository;

    @InjectMocks
    private PercentileRecalculationListener listener;

    @Captor
    private ArgumentCaptor<TestResult> resultCaptor;

    private UUID templateId;
    private UUID sessionId;
    private UUID resultId;
    private TestTemplate template;
    private TestSession session;
    private TestResult result;

    @BeforeEach
    void setUp() {
        templateId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
        resultId = UUID.randomUUID();

        template = new TestTemplate();
        template.setId(templateId);

        session = new TestSession();
        session.setId(sessionId);
        session.setTemplate(template);

        result = new TestResult(session, "user_123");
        result.setId(resultId);
        result.setOverallPercentage(75.0);
        result.setPercentile(50);
        result.setCompletedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("Event Handling")
    class EventHandling {

        @Test
        @DisplayName("should skip recalculation when resultId is null")
        void shouldSkipWhenResultIdIsNull() {
            // Given
            ScoringCompletedEvent event = new ScoringCompletedEvent(
                    sessionId, null, AssessmentGoal.OVERVIEW,
                    75.0, true, Duration.ofMillis(100), java.time.Instant.now());

            // When
            listener.onScoringCompleted(event);

            // Then
            verifyNoInteractions(resultRepository);
        }

        @Test
        @DisplayName("should handle result not found gracefully")
        void shouldHandleResultNotFound() {
            // Given
            ScoringCompletedEvent event = createEvent(resultId);
            when(resultRepository.findByIdWithSessionAndTemplate(resultId))
                    .thenReturn(Optional.empty());

            // When
            listener.onScoringCompleted(event);

            // Then
            verify(resultRepository).findByIdWithSessionAndTemplate(resultId);
            verify(resultRepository, never()).findByTemplateIdAndCompletedAtAfter(any(), any());
        }

        @Test
        @DisplayName("should handle result with null session gracefully")
        void shouldHandleNullSession() {
            // Given
            TestResult resultWithNullSession = new TestResult();
            resultWithNullSession.setId(resultId);

            ScoringCompletedEvent event = createEvent(resultId);
            when(resultRepository.findByIdWithSessionAndTemplate(resultId))
                    .thenReturn(Optional.of(resultWithNullSession));

            // When
            listener.onScoringCompleted(event);

            // Then
            verify(resultRepository).findByIdWithSessionAndTemplate(resultId);
            verify(resultRepository, never()).findByTemplateIdAndCompletedAtAfter(any(), any());
        }

        @Test
        @DisplayName("should recalculate percentiles for recent results")
        void shouldRecalculatePercentilesForRecentResults() {
            // Given
            ScoringCompletedEvent event = createEvent(resultId);

            TestResult anotherResult = new TestResult(session, "user_456");
            anotherResult.setId(UUID.randomUUID());
            anotherResult.setOverallPercentage(60.0);
            anotherResult.setPercentile(40);

            when(resultRepository.findByIdWithSessionAndTemplate(resultId))
                    .thenReturn(Optional.of(result));
            when(resultRepository.findByTemplateIdAndCompletedAtAfter(eq(templateId), any()))
                    .thenReturn(List.of(result, anotherResult));
            when(resultRepository.countResultsBelowScore(eq(templateId), eq(75.0)))
                    .thenReturn(8L);
            when(resultRepository.countResultsBelowScore(eq(templateId), eq(60.0)))
                    .thenReturn(3L);
            when(resultRepository.countResultsByTemplateId(templateId))
                    .thenReturn(10L);

            // When
            listener.onScoringCompleted(event);

            // Then
            verify(resultRepository, times(2)).save(resultCaptor.capture());
            List<TestResult> savedResults = resultCaptor.getAllValues();

            // 75.0 score: 8 below out of 10 = 80th percentile
            TestResult saved75 = savedResults.stream()
                    .filter(r -> r.getOverallPercentage().equals(75.0))
                    .findFirst()
                    .orElseThrow();
            assertThat(saved75.getPercentile()).isEqualTo(80);

            // 60.0 score: 3 below out of 10 = 30th percentile
            TestResult saved60 = savedResults.stream()
                    .filter(r -> r.getOverallPercentage().equals(60.0))
                    .findFirst()
                    .orElseThrow();
            assertThat(saved60.getPercentile()).isEqualTo(30);
        }

        @Test
        @DisplayName("should not save result if percentile unchanged")
        void shouldNotSaveIfPercentileUnchanged() {
            // Given
            ScoringCompletedEvent event = createEvent(resultId);
            result.setPercentile(80); // Already correct

            when(resultRepository.findByIdWithSessionAndTemplate(resultId))
                    .thenReturn(Optional.of(result));
            when(resultRepository.findByTemplateIdAndCompletedAtAfter(eq(templateId), any()))
                    .thenReturn(List.of(result));
            when(resultRepository.countResultsBelowScore(eq(templateId), eq(75.0)))
                    .thenReturn(8L);
            when(resultRepository.countResultsByTemplateId(templateId))
                    .thenReturn(10L);

            // When
            listener.onScoringCompleted(event);

            // Then
            verify(resultRepository, never()).save(any());
        }

        @Test
        @DisplayName("should handle empty recent results list")
        void shouldHandleEmptyRecentResults() {
            // Given
            ScoringCompletedEvent event = createEvent(resultId);

            when(resultRepository.findByIdWithSessionAndTemplate(resultId))
                    .thenReturn(Optional.of(result));
            when(resultRepository.findByTemplateIdAndCompletedAtAfter(eq(templateId), any()))
                    .thenReturn(Collections.emptyList());

            // When
            listener.onScoringCompleted(event);

            // Then
            verify(resultRepository).findByTemplateIdAndCompletedAtAfter(eq(templateId), any());
            verify(resultRepository, never()).countResultsBelowScore(any(), any());
            verify(resultRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Percentile Calculation")
    class PercentileCalculation {

        @Test
        @DisplayName("should return 50 for null score")
        void shouldReturn50ForNullScore() {
            // When
            int percentile = listener.calculateAccuratePercentile(templateId, null);

            // Then
            assertThat(percentile).isEqualTo(50);
        }

        @Test
        @DisplayName("should return 50 for first result")
        void shouldReturn50ForFirstResult() {
            // Given
            when(resultRepository.countResultsBelowScore(templateId, 75.0)).thenReturn(0L);
            when(resultRepository.countResultsByTemplateId(templateId)).thenReturn(1L);

            // When
            int percentile = listener.calculateAccuratePercentile(templateId, 75.0);

            // Then
            assertThat(percentile).isEqualTo(50);
        }

        @Test
        @DisplayName("should calculate correct percentile for median score")
        void shouldCalculateCorrectPercentileForMedian() {
            // Given - 5 out of 10 scores below
            when(resultRepository.countResultsBelowScore(templateId, 50.0)).thenReturn(5L);
            when(resultRepository.countResultsByTemplateId(templateId)).thenReturn(10L);

            // When
            int percentile = listener.calculateAccuratePercentile(templateId, 50.0);

            // Then
            assertThat(percentile).isEqualTo(50);
        }

        @Test
        @DisplayName("should calculate correct percentile for top score")
        void shouldCalculateCorrectPercentileForTopScore() {
            // Given - 99 out of 100 scores below
            when(resultRepository.countResultsBelowScore(templateId, 100.0)).thenReturn(99L);
            when(resultRepository.countResultsByTemplateId(templateId)).thenReturn(100L);

            // When
            int percentile = listener.calculateAccuratePercentile(templateId, 100.0);

            // Then
            assertThat(percentile).isEqualTo(99);
        }

        @Test
        @DisplayName("should calculate correct percentile for lowest score")
        void shouldCalculateCorrectPercentileForLowestScore() {
            // Given - 0 out of 100 scores below
            when(resultRepository.countResultsBelowScore(templateId, 10.0)).thenReturn(0L);
            when(resultRepository.countResultsByTemplateId(templateId)).thenReturn(100L);

            // When
            int percentile = listener.calculateAccuratePercentile(templateId, 10.0);

            // Then
            assertThat(percentile).isEqualTo(0);
        }

        @Test
        @DisplayName("should clamp percentile between 0 and 100")
        void shouldClampPercentile() {
            // This is mainly for edge cases with rounding
            when(resultRepository.countResultsBelowScore(templateId, 100.0)).thenReturn(100L);
            when(resultRepository.countResultsByTemplateId(templateId)).thenReturn(100L);

            // When
            int percentile = listener.calculateAccuratePercentile(templateId, 100.0);

            // Then
            assertThat(percentile).isLessThanOrEqualTo(100);
            assertThat(percentile).isGreaterThanOrEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("should catch and log exceptions without rethrowing")
        void shouldCatchExceptionsGracefully() {
            // Given
            ScoringCompletedEvent event = createEvent(resultId);
            when(resultRepository.findByIdWithSessionAndTemplate(resultId))
                    .thenThrow(new RuntimeException("Database error"));

            // When - should not throw
            listener.onScoringCompleted(event);

            // Then - should not propagate exception
            verify(resultRepository).findByIdWithSessionAndTemplate(resultId);
        }
    }

    // Helper methods

    private ScoringCompletedEvent createEvent(UUID resultId) {
        return new ScoringCompletedEvent(
                sessionId, resultId, AssessmentGoal.OVERVIEW,
                75.0, true, Duration.ofMillis(100), java.time.Instant.now());
    }
}
