package app.skillsoft.assessmentbackend.services;

import app.skillsoft.assessmentbackend.domain.dto.stats.EntityStatsDto;
import app.skillsoft.assessmentbackend.domain.dto.stats.EntityStatsDto.CompetencyStatsDto;
import app.skillsoft.assessmentbackend.domain.dto.stats.EntityStatsDto.IndicatorStatsDto;
import app.skillsoft.assessmentbackend.domain.dto.stats.EntityStatsDto.QuestionStatsDto;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.repository.AssessmentQuestionRepository;
import app.skillsoft.assessmentbackend.repository.BehavioralIndicatorRepository;
import app.skillsoft.assessmentbackend.repository.CompetencyRepository;
import app.skillsoft.assessmentbackend.services.impl.EntityStatsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EntityStats Service Tests")
class EntityStatsServiceTest {

    @Mock
    private CompetencyRepository competencyRepository;

    @Mock
    private BehavioralIndicatorRepository indicatorRepository;

    @Mock
    private AssessmentQuestionRepository questionRepository;

    @InjectMocks
    private EntityStatsServiceImpl entityStatsService;

    @Nested
    @DisplayName("Competency Stats")
    class CompetencyStatsTests {

        @Test
        @DisplayName("should compute total and active competency counts")
        void shouldComputeTotalAndActiveCounts() {
            when(competencyRepository.count()).thenReturn(25L);
            when(competencyRepository.countByIsActiveTrue()).thenReturn(20L);
            when(competencyRepository.countWithIndicators()).thenReturn(18L);
            when(competencyRepository.averageIndicatorWeight()).thenReturn(3.456);
            when(competencyRepository.countByCategory()).thenReturn(List.of(
                    new Object[]{CompetencyCategory.COGNITIVE, 10L},
                    new Object[]{CompetencyCategory.LEADERSHIP, 8L}
            ));

            EntityStatsDto result = entityStatsService.getEntityStats();
            CompetencyStatsDto stats = result.competencies();

            assertThat(stats.total()).isEqualTo(25);
            assertThat(stats.active()).isEqualTo(20);
            assertThat(stats.withIndicators()).isEqualTo(18);
            assertThat(stats.averageIndicatorWeight()).isEqualTo(3.5);
            assertThat(stats.byCategory()).containsEntry("COGNITIVE", 10L);
            assertThat(stats.byCategory()).containsEntry("LEADERSHIP", 8L);
        }

        @Test
        @DisplayName("should handle empty competency repository")
        void shouldHandleEmptyRepository() {
            when(competencyRepository.count()).thenReturn(0L);
            when(competencyRepository.countByIsActiveTrue()).thenReturn(0L);
            when(competencyRepository.countWithIndicators()).thenReturn(0L);
            when(competencyRepository.averageIndicatorWeight()).thenReturn(0.0);
            when(competencyRepository.countByCategory()).thenReturn(List.of());

            EntityStatsDto result = entityStatsService.getEntityStats();
            CompetencyStatsDto stats = result.competencies();

            assertThat(stats.total()).isZero();
            assertThat(stats.active()).isZero();
            assertThat(stats.byCategory()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Indicator Stats")
    class IndicatorStatsTests {

        @Test
        @DisplayName("should compute indicator stats with measurable count")
        void shouldComputeIndicatorStats() {
            when(indicatorRepository.count()).thenReturn(100L);
            when(indicatorRepository.countByIsActiveTrue()).thenReturn(85L);
            when(indicatorRepository.countWithActiveQuestions()).thenReturn(72L);
            when(indicatorRepository.countByMeasurementTypeIn(anyList())).thenReturn(60L);
            when(indicatorRepository.averageObservabilityComplexity()).thenReturn(2.35);
            for (ContextScope scope : ContextScope.values()) {
                when(indicatorRepository.countByContextScope(scope)).thenReturn(25L);
            }

            EntityStatsDto result = entityStatsService.getEntityStats();
            IndicatorStatsDto stats = result.indicators();

            assertThat(stats.total()).isEqualTo(100);
            assertThat(stats.active()).isEqualTo(85);
            assertThat(stats.withQuestions()).isEqualTo(72);
            assertThat(stats.measurable()).isEqualTo(60);
            assertThat(stats.averageComplexity()).isEqualTo(2.4);
            assertThat(stats.byContextScope()).hasSize(ContextScope.values().length);
        }

        @Test
        @DisplayName("should pass correct measurement types for measurable count")
        void shouldPassCorrectMeasurementTypes() {
            // Stub all required methods to avoid UnnecessaryStubbingException
            when(indicatorRepository.count()).thenReturn(0L);
            when(indicatorRepository.countByIsActiveTrue()).thenReturn(0L);
            when(indicatorRepository.countWithActiveQuestions()).thenReturn(0L);
            when(indicatorRepository.countByMeasurementTypeIn(anyList())).thenReturn(0L);
            when(indicatorRepository.averageObservabilityComplexity()).thenReturn(0.0);
            for (ContextScope scope : ContextScope.values()) {
                when(indicatorRepository.countByContextScope(scope)).thenReturn(0L);
            }

            entityStatsService.getEntityStats();

            verify(indicatorRepository).countByMeasurementTypeIn(List.of(
                    IndicatorMeasurementType.FREQUENCY,
                    IndicatorMeasurementType.QUALITY,
                    IndicatorMeasurementType.IMPACT
            ));
        }
    }

    @Nested
    @DisplayName("Question Stats")
    class QuestionStatsTests {

        @Test
        @DisplayName("should compute question stats with hard questions count")
        void shouldComputeQuestionStats() {
            when(questionRepository.count()).thenReturn(500L);
            when(questionRepository.countByIsActiveTrue()).thenReturn(420L);
            when(questionRepository.countWithActiveIndicators()).thenReturn(400L);
            when(questionRepository.countByDifficultyLevelIn(anyList())).thenReturn(150L);
            when(questionRepository.averageTimeLimit()).thenReturn(45.67);
            for (DifficultyLevel level : DifficultyLevel.values()) {
                when(questionRepository.countByDifficultyLevel(level)).thenReturn(100L);
            }
            for (QuestionType type : QuestionType.values()) {
                when(questionRepository.countByQuestionType(type)).thenReturn(0L);
            }
            when(questionRepository.countByQuestionType(QuestionType.LIKERT)).thenReturn(200L);
            when(questionRepository.countByQuestionType(QuestionType.MCQ)).thenReturn(150L);

            EntityStatsDto result = entityStatsService.getEntityStats();
            QuestionStatsDto stats = result.questions();

            assertThat(stats.total()).isEqualTo(500);
            assertThat(stats.active()).isEqualTo(420);
            assertThat(stats.withActiveIndicators()).isEqualTo(400);
            assertThat(stats.hardQuestions()).isEqualTo(150);
            assertThat(stats.averageTimeLimitSeconds()).isEqualTo(45.7);
            assertThat(stats.byDifficulty()).hasSize(DifficultyLevel.values().length);
            // Only types with count > 0 are included
            assertThat(stats.byQuestionType()).containsEntry("LIKERT", 200L);
            assertThat(stats.byQuestionType()).containsEntry("MCQ", 150L);
            assertThat(stats.byQuestionType()).doesNotContainKey("SJT");
        }

        @Test
        @DisplayName("should pass correct difficulty levels for hard questions")
        void shouldPassCorrectDifficultyLevels() {
            when(questionRepository.count()).thenReturn(0L);
            when(questionRepository.countByIsActiveTrue()).thenReturn(0L);
            when(questionRepository.countWithActiveIndicators()).thenReturn(0L);
            when(questionRepository.countByDifficultyLevelIn(anyList())).thenReturn(0L);
            when(questionRepository.averageTimeLimit()).thenReturn(0.0);
            for (DifficultyLevel level : DifficultyLevel.values()) {
                when(questionRepository.countByDifficultyLevel(level)).thenReturn(0L);
            }
            for (QuestionType type : QuestionType.values()) {
                when(questionRepository.countByQuestionType(type)).thenReturn(0L);
            }

            entityStatsService.getEntityStats();

            verify(questionRepository).countByDifficultyLevelIn(List.of(
                    DifficultyLevel.ADVANCED,
                    DifficultyLevel.EXPERT,
                    DifficultyLevel.SPECIALIZED
            ));
        }
    }

    @Nested
    @DisplayName("Rounding")
    class RoundingTests {

        @Test
        @DisplayName("should round averages to 1 decimal place")
        void shouldRoundTo1Decimal() {
            when(competencyRepository.count()).thenReturn(1L);
            when(competencyRepository.countByIsActiveTrue()).thenReturn(1L);
            when(competencyRepository.countWithIndicators()).thenReturn(1L);
            when(competencyRepository.averageIndicatorWeight()).thenReturn(3.1499);
            when(competencyRepository.countByCategory()).thenReturn(List.of());

            EntityStatsDto result = entityStatsService.getEntityStats();

            assertThat(result.competencies().averageIndicatorWeight()).isEqualTo(3.1);
        }
    }
}
