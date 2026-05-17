package app.skillsoft.assessmentbackend.events.listeners;

import app.skillsoft.assessmentbackend.domain.dto.CompetencyScoreDto;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;
import app.skillsoft.assessmentbackend.domain.entities.TestResult;
import app.skillsoft.assessmentbackend.events.scoring.ScoringCompletedEvent;
import app.skillsoft.assessmentbackend.repository.TestResultRepository;
import app.skillsoft.assessmentbackend.services.external.impl.PassportServiceImpl;
import app.skillsoft.assessmentbackend.testutil.PassportTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PassportUpdateListener.
 */
@DisplayName("PassportUpdateListener Unit Tests")
@ExtendWith(MockitoExtension.class)
class PassportUpdateListenerTest {

    @Mock
    private TestResultRepository resultRepository;

    @Mock
    private PassportServiceImpl passportService;

    @InjectMocks
    private PassportUpdateListener listener;

    // ---- Happy path ----

    @Nested
    @DisplayName("Happy Path")
    class HappyPath {

        @Test
        @DisplayName("should create passport from OVERVIEW result with valid scores")
        void shouldCreatePassportFromOverviewResult() {
            // Given
            UUID resultId = UUID.randomUUID();
            ScoringCompletedEvent event = ScoringCompletedEvent.now(
                UUID.randomUUID(), resultId, AssessmentGoal.OVERVIEW, 85.0, true, Duration.ofMillis(500));

            TestResult result = new TestResult();
            result.setId(resultId);
            result.setClerkUserId(PassportTestFixtures.CLERK_USER_ID);

            CompetencyScoreDto score1 = new CompetencyScoreDto(PassportTestFixtures.COMPETENCY_1, "Leadership", 24.0, 30.0, 80.0);
            CompetencyScoreDto score2 = new CompetencyScoreDto(PassportTestFixtures.COMPETENCY_2, "Communication", 18.0, 30.0, 60.0);
            result.setCompetencyScores(List.of(score1, score2));
            result.setBigFiveProfile(PassportTestFixtures.DEFAULT_BIG_FIVE);

            when(resultRepository.findByIdWithSessionAndTemplate(resultId))
                .thenReturn(Optional.of(result));

            // When
            listener.onScoringCompleted(event);

            // Then
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<UUID, Double>> scoresCaptor = ArgumentCaptor.forClass(Map.class);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Double>> bigFiveCaptor = ArgumentCaptor.forClass(Map.class);

            verify(passportService).savePassportForClerkUser(
                eq(PassportTestFixtures.CLERK_USER_ID),
                scoresCaptor.capture(),
                bigFiveCaptor.capture(),
                eq(resultId)
            );

            Map<UUID, Double> passportScores = scoresCaptor.getValue();
            assertThat(passportScores).hasSize(2);
            // 80.0 / 20.0 = 4.0
            assertThat(passportScores.get(PassportTestFixtures.COMPETENCY_1)).isEqualTo(4.0);
            // 60.0 / 20.0 = 3.0
            assertThat(passportScores.get(PassportTestFixtures.COMPETENCY_2)).isEqualTo(3.0);

            assertThat(bigFiveCaptor.getValue()).isEqualTo(PassportTestFixtures.DEFAULT_BIG_FIVE);
        }
    }

    // ---- Skip non-OVERVIEW ----

    @Nested
    @DisplayName("Goal Filtering")
    class GoalFiltering {

        @Test
        @DisplayName("should skip JOB_FIT events")
        void shouldSkipJobFitEvents() {
            // Given
            ScoringCompletedEvent event = ScoringCompletedEvent.now(
                UUID.randomUUID(), UUID.randomUUID(), AssessmentGoal.JOB_FIT, 85.0, true, Duration.ofMillis(100));

            // When
            listener.onScoringCompleted(event);

            // Then
            verifyNoInteractions(resultRepository);
            verifyNoInteractions(passportService);
        }

        @Test
        @DisplayName("should skip TEAM_FIT events")
        void shouldSkipTeamFitEvents() {
            // Given
            ScoringCompletedEvent event = ScoringCompletedEvent.now(
                UUID.randomUUID(), UUID.randomUUID(), AssessmentGoal.TEAM_FIT, 75.0, true, Duration.ofMillis(100));

            // When
            listener.onScoringCompleted(event);

            // Then
            verifyNoInteractions(resultRepository);
            verifyNoInteractions(passportService);
        }
    }

    // ---- Skip anonymous ----

    @Nested
    @DisplayName("Anonymous Filtering")
    class AnonymousFiltering {

        @Test
        @DisplayName("should skip when clerkUserId is null (anonymous)")
        void shouldSkipWhenClerkUserIdIsNull() {
            // Given
            UUID resultId = UUID.randomUUID();
            ScoringCompletedEvent event = ScoringCompletedEvent.now(
                UUID.randomUUID(), resultId, AssessmentGoal.OVERVIEW, 85.0, true, Duration.ofMillis(100));

            TestResult result = new TestResult();
            result.setClerkUserId(null); // anonymous session

            when(resultRepository.findByIdWithSessionAndTemplate(resultId))
                .thenReturn(Optional.of(result));

            // When
            listener.onScoringCompleted(event);

            // Then
            verifyNoInteractions(passportService);
        }
    }

    // ---- Skip no scores ----

    @Nested
    @DisplayName("Empty Scores Filtering")
    class EmptyScoresFiltering {

        @Test
        @DisplayName("should skip when competency scores are empty")
        void shouldSkipWhenCompetencyScoresEmpty() {
            // Given
            UUID resultId = UUID.randomUUID();
            ScoringCompletedEvent event = ScoringCompletedEvent.now(
                UUID.randomUUID(), resultId, AssessmentGoal.OVERVIEW, 85.0, true, Duration.ofMillis(100));

            TestResult result = new TestResult();
            result.setClerkUserId(PassportTestFixtures.CLERK_USER_ID);
            result.setCompetencyScores(List.of());

            when(resultRepository.findByIdWithSessionAndTemplate(resultId))
                .thenReturn(Optional.of(result));

            // When
            listener.onScoringCompleted(event);

            // Then
            verifyNoInteractions(passportService);
        }

        @Test
        @DisplayName("should skip when result not found")
        void shouldSkipWhenResultNotFound() {
            // Given
            UUID resultId = UUID.randomUUID();
            ScoringCompletedEvent event = ScoringCompletedEvent.now(
                UUID.randomUUID(), resultId, AssessmentGoal.OVERVIEW, 85.0, true, Duration.ofMillis(100));

            when(resultRepository.findByIdWithSessionAndTemplate(resultId))
                .thenReturn(Optional.empty());

            // When
            listener.onScoringCompleted(event);

            // Then
            verifyNoInteractions(passportService);
        }
    }

    // ---- Score conversion ----

    @Nested
    @DisplayName("Score Conversion")
    class ScoreConversion {

        @Test
        @DisplayName("should clamp scores to [1.0, 5.0] range")
        void shouldClampScoresToRange() {
            // Given
            UUID resultId = UUID.randomUUID();
            ScoringCompletedEvent event = ScoringCompletedEvent.now(
                UUID.randomUUID(), resultId, AssessmentGoal.OVERVIEW, 50.0, true, Duration.ofMillis(100));

            TestResult result = new TestResult();
            result.setId(resultId);
            result.setClerkUserId(PassportTestFixtures.CLERK_USER_ID);

            // 0% -> 0/20 = 0.0 -> clamped to 1.0
            CompetencyScoreDto lowScore = new CompetencyScoreDto(PassportTestFixtures.COMPETENCY_1, "Low", 0.0, 30.0, 0.0);
            // 100% -> 100/20 = 5.0 -> stays 5.0
            CompetencyScoreDto highScore = new CompetencyScoreDto(PassportTestFixtures.COMPETENCY_2, "High", 30.0, 30.0, 100.0);
            // 10% -> 10/20 = 0.5 -> clamped to 1.0
            CompetencyScoreDto veryLow = new CompetencyScoreDto(PassportTestFixtures.COMPETENCY_3, "VeryLow", 3.0, 30.0, 10.0);
            result.setCompetencyScores(List.of(lowScore, highScore, veryLow));

            when(resultRepository.findByIdWithSessionAndTemplate(resultId))
                .thenReturn(Optional.of(result));

            // When
            listener.onScoringCompleted(event);

            // Then
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<UUID, Double>> scoresCaptor = ArgumentCaptor.forClass(Map.class);
            verify(passportService).savePassportForClerkUser(
                anyString(), scoresCaptor.capture(), any(), any());

            Map<UUID, Double> scores = scoresCaptor.getValue();
            assertThat(scores.get(PassportTestFixtures.COMPETENCY_1)).isEqualTo(1.0); // clamped from 0.0
            assertThat(scores.get(PassportTestFixtures.COMPETENCY_2)).isEqualTo(5.0); // stays at max
            assertThat(scores.get(PassportTestFixtures.COMPETENCY_3)).isEqualTo(1.0); // clamped from 0.5
        }
    }

    // ---- Error handling ----

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("should not propagate exceptions from passport service")
        void shouldNotPropagateExceptions() {
            // Given
            UUID resultId = UUID.randomUUID();
            ScoringCompletedEvent event = ScoringCompletedEvent.now(
                UUID.randomUUID(), resultId, AssessmentGoal.OVERVIEW, 85.0, true, Duration.ofMillis(100));

            TestResult result = new TestResult();
            result.setId(resultId);
            result.setClerkUserId(PassportTestFixtures.CLERK_USER_ID);
            CompetencyScoreDto score = new CompetencyScoreDto(PassportTestFixtures.COMPETENCY_1, "Test", 24.0, 30.0, 80.0);
            result.setCompetencyScores(List.of(score));

            when(resultRepository.findByIdWithSessionAndTemplate(resultId))
                .thenReturn(Optional.of(result));
            doThrow(new RuntimeException("DB connection lost"))
                .when(passportService).savePassportForClerkUser(anyString(), any(), any(), any());

            // When / Then - should not throw
            listener.onScoringCompleted(event);

            // Verify the service was called (and it threw)
            verify(passportService).savePassportForClerkUser(anyString(), any(), any(), any());
        }
    }
}
