package app.skillsoft.assessmentbackend.services.validation;

import app.skillsoft.assessmentbackend.domain.dto.simulation.HealthStatus;
import app.skillsoft.assessmentbackend.domain.dto.simulation.InventoryHeatmapDto;
import app.skillsoft.assessmentbackend.domain.dto.simulation.InventoryHeatmapDto.HeatmapSummary;
import app.skillsoft.assessmentbackend.domain.entities.DifficultyLevel;
import app.skillsoft.assessmentbackend.repository.AssessmentQuestionRepository;
import app.skillsoft.assessmentbackend.repository.CompetencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InventoryHeatmapService.
 *
 * Tests verify:
 * - Heatmap generation for all competencies
 * - Filtered heatmap generation for specific competencies
 * - Health status thresholds (CRITICAL < 3, MODERATE 3-5, HEALTHY > 5)
 * - Summary statistics calculation (counts, percentages)
 * - Sufficiency checking for test configurations
 * - Edge cases (empty database, invalid UUIDs, null inputs)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryHeatmapService Tests")
class InventoryHeatmapServiceTest {

    @Mock
    private AssessmentQuestionRepository questionRepository;

    @Mock
    private CompetencyRepository competencyRepository;

    @InjectMocks
    private InventoryHeatmapService inventoryHeatmapService;

    // Test UUIDs
    private UUID competencyId1;
    private UUID competencyId2;
    private UUID competencyId3;

    @BeforeEach
    void setUp() {
        competencyId1 = UUID.randomUUID();
        competencyId2 = UUID.randomUUID();
        competencyId3 = UUID.randomUUID();
    }

    // Helper method to create raw count data
    private List<Object[]> createRawCounts(Object[]... entries) {
        return Arrays.asList(entries);
    }

    @Nested
    @DisplayName("generateHeatmap")
    class GenerateHeatmapTests {

        @Test
        @DisplayName("Should return empty heatmap when no questions exist")
        void shouldReturnEmptyHeatmapWhenNoQuestionsExist() {
            // Given
            when(questionRepository.countQuestionsByCompetencyAndDifficulty()).thenReturn(Collections.emptyList());

            // When
            InventoryHeatmapDto result = inventoryHeatmapService.generateHeatmap();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.competencyHealth()).isEmpty();
            assertThat(result.detailedCounts()).isEmpty();
            assertThat(result.summary().totalCompetencies()).isZero();
            assertThat(result.summary().totalQuestions()).isZero();
        }

        @Test
        @DisplayName("Should correctly calculate health for single competency with CRITICAL status")
        void shouldCalculateCriticalHealthForSingleCompetency() {
            // Given - 2 questions total (CRITICAL threshold)
            List<Object[]> rawCounts = createRawCounts(
                new Object[]{competencyId1, DifficultyLevel.FOUNDATIONAL, 2L}
            );
            when(questionRepository.countQuestionsByCompetencyAndDifficulty()).thenReturn(rawCounts);

            // When
            InventoryHeatmapDto result = inventoryHeatmapService.generateHeatmap();

            // Then
            assertThat(result.competencyHealth()).containsEntry(competencyId1, HealthStatus.CRITICAL);
            assertThat(result.summary().criticalCount()).isEqualTo(1);
            assertThat(result.summary().moderateCount()).isZero();
            assertThat(result.summary().healthyCount()).isZero();
        }

        @Test
        @DisplayName("Should correctly calculate health for single competency with MODERATE status at 3 questions")
        void shouldCalculateModerateHealthAtThreeQuestions() {
            // Given - 3 questions total (MODERATE lower bound)
            List<Object[]> rawCounts = createRawCounts(
                new Object[]{competencyId1, DifficultyLevel.FOUNDATIONAL, 3L}
            );
            when(questionRepository.countQuestionsByCompetencyAndDifficulty()).thenReturn(rawCounts);

            // When
            InventoryHeatmapDto result = inventoryHeatmapService.generateHeatmap();

            // Then
            assertThat(result.competencyHealth()).containsEntry(competencyId1, HealthStatus.MODERATE);
            assertThat(result.summary().moderateCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should correctly calculate health for single competency with MODERATE status at 5 questions")
        void shouldCalculateModerateHealthAtFiveQuestions() {
            // Given - 5 questions total (MODERATE upper bound)
            List<Object[]> rawCounts = createRawCounts(
                new Object[]{competencyId1, DifficultyLevel.INTERMEDIATE, 5L}
            );
            when(questionRepository.countQuestionsByCompetencyAndDifficulty()).thenReturn(rawCounts);

            // When
            InventoryHeatmapDto result = inventoryHeatmapService.generateHeatmap();

            // Then
            assertThat(result.competencyHealth()).containsEntry(competencyId1, HealthStatus.MODERATE);
            assertThat(result.summary().moderateCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should correctly calculate health for single competency with HEALTHY status at 6 questions")
        void shouldCalculateHealthyStatusAtSixQuestions() {
            // Given - 6 questions total (HEALTHY threshold)
            List<Object[]> rawCounts = createRawCounts(
                new Object[]{competencyId1, DifficultyLevel.ADVANCED, 6L}
            );
            when(questionRepository.countQuestionsByCompetencyAndDifficulty()).thenReturn(rawCounts);

            // When
            InventoryHeatmapDto result = inventoryHeatmapService.generateHeatmap();

            // Then
            assertThat(result.competencyHealth()).containsEntry(competencyId1, HealthStatus.HEALTHY);
            assertThat(result.summary().healthyCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should aggregate questions across difficulties for same competency")
        void shouldAggregateQuestionsAcrossDifficulties() {
            // Given - 2 + 2 + 3 = 7 questions (HEALTHY)
            List<Object[]> rawCounts = createRawCounts(
                new Object[]{competencyId1, DifficultyLevel.FOUNDATIONAL, 2L},
                new Object[]{competencyId1, DifficultyLevel.INTERMEDIATE, 2L},
                new Object[]{competencyId1, DifficultyLevel.ADVANCED, 3L}
            );
            when(questionRepository.countQuestionsByCompetencyAndDifficulty()).thenReturn(rawCounts);

            // When
            InventoryHeatmapDto result = inventoryHeatmapService.generateHeatmap();

            // Then
            assertThat(result.competencyHealth()).containsEntry(competencyId1, HealthStatus.HEALTHY);
            assertThat(result.summary().totalQuestions()).isEqualTo(7);
        }

        @Test
        @DisplayName("Should handle multiple competencies with mixed health statuses")
        void shouldHandleMultipleCompetenciesWithMixedStatuses() {
            // Given
            List<Object[]> rawCounts = createRawCounts(
                new Object[]{competencyId1, DifficultyLevel.FOUNDATIONAL, 2L},   // CRITICAL (2)
                new Object[]{competencyId2, DifficultyLevel.INTERMEDIATE, 4L},   // MODERATE (4)
                new Object[]{competencyId3, DifficultyLevel.ADVANCED, 10L}       // HEALTHY (10)
            );
            when(questionRepository.countQuestionsByCompetencyAndDifficulty()).thenReturn(rawCounts);

            // When
            InventoryHeatmapDto result = inventoryHeatmapService.generateHeatmap();

            // Then
            assertThat(result.competencyHealth()).hasSize(3);
            assertThat(result.competencyHealth()).containsEntry(competencyId1, HealthStatus.CRITICAL);
            assertThat(result.competencyHealth()).containsEntry(competencyId2, HealthStatus.MODERATE);
            assertThat(result.competencyHealth()).containsEntry(competencyId3, HealthStatus.HEALTHY);
            assertThat(result.summary().criticalCount()).isEqualTo(1);
            assertThat(result.summary().moderateCount()).isEqualTo(1);
            assertThat(result.summary().healthyCount()).isEqualTo(1);
            assertThat(result.summary().totalCompetencies()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should correctly build detailed counts with proper key format")
        void shouldBuildDetailedCountsWithProperKeyFormat() {
            // Given
            List<Object[]> rawCounts = createRawCounts(
                new Object[]{competencyId1, DifficultyLevel.FOUNDATIONAL, 5L},
                new Object[]{competencyId1, DifficultyLevel.INTERMEDIATE, 3L}
            );
            when(questionRepository.countQuestionsByCompetencyAndDifficulty()).thenReturn(rawCounts);

            // When
            InventoryHeatmapDto result = inventoryHeatmapService.generateHeatmap();

            // Then
            String key1 = competencyId1.toString() + ":FOUNDATIONAL";
            String key2 = competencyId1.toString() + ":INTERMEDIATE";
            assertThat(result.detailedCounts()).containsEntry(key1, 5L);
            assertThat(result.detailedCounts()).containsEntry(key2, 3L);
        }

        @Test
        @DisplayName("Should calculate total questions correctly in summary")
        void shouldCalculateTotalQuestionsCorrectly() {
            // Given
            List<Object[]> rawCounts = createRawCounts(
                new Object[]{competencyId1, DifficultyLevel.FOUNDATIONAL, 5L},
                new Object[]{competencyId2, DifficultyLevel.INTERMEDIATE, 10L},
                new Object[]{competencyId3, DifficultyLevel.ADVANCED, 15L}
            );
            when(questionRepository.countQuestionsByCompetencyAndDifficulty()).thenReturn(rawCounts);

            // When
            InventoryHeatmapDto result = inventoryHeatmapService.generateHeatmap();

            // Then
            assertThat(result.summary().totalQuestions()).isEqualTo(30L);
        }

        @Test
        @DisplayName("Should handle zero questions for competency as CRITICAL")
        void shouldHandleZeroQuestionsAsCritical() {
            // Given - 0 questions (CRITICAL)
            List<Object[]> rawCounts = createRawCounts(
                new Object[]{competencyId1, DifficultyLevel.FOUNDATIONAL, 0L}
            );
            when(questionRepository.countQuestionsByCompetencyAndDifficulty()).thenReturn(rawCounts);

            // When
            InventoryHeatmapDto result = inventoryHeatmapService.generateHeatmap();

            // Then
            assertThat(result.competencyHealth()).containsEntry(competencyId1, HealthStatus.CRITICAL);
        }

        @Test
        @DisplayName("Should handle single question as CRITICAL")
        void shouldHandleSingleQuestionAsCritical() {
            // Given - 1 question (CRITICAL)
            List<Object[]> rawCounts = createRawCounts(
                new Object[]{competencyId1, DifficultyLevel.FOUNDATIONAL, 1L}
            );
            when(questionRepository.countQuestionsByCompetencyAndDifficulty()).thenReturn(rawCounts);

            // When
            InventoryHeatmapDto result = inventoryHeatmapService.generateHeatmap();

            // Then
            assertThat(result.competencyHealth()).containsEntry(competencyId1, HealthStatus.CRITICAL);
        }
    }

    @Nested
    @DisplayName("generateHeatmapFor")
    class GenerateHeatmapForTests {

        @Test
        @DisplayName("Should fall back to full heatmap when competencyIds is null")
        void shouldFallBackToFullHeatmapWhenNull() {
            // Given
            List<Object[]> rawCounts = createRawCounts(
                new Object[]{competencyId1, DifficultyLevel.FOUNDATIONAL, 10L}
            );
            when(questionRepository.countQuestionsByCompetencyAndDifficulty()).thenReturn(rawCounts);

            // When
            InventoryHeatmapDto result = inventoryHeatmapService.generateHeatmapFor(null);

            // Then
            assertThat(result.competencyHealth()).containsKey(competencyId1);
            verify(questionRepository, times(1)).countQuestionsByCompetencyAndDifficulty();
        }

        @Test
        @DisplayName("Should fall back to full heatmap when competencyIds is empty")
        void shouldFallBackToFullHeatmapWhenEmpty() {
            // Given
            List<Object[]> rawCounts = createRawCounts(
                new Object[]{competencyId1, DifficultyLevel.FOUNDATIONAL, 10L}
            );
            when(questionRepository.countQuestionsByCompetencyAndDifficulty()).thenReturn(rawCounts);

            // When
            InventoryHeatmapDto result = inventoryHeatmapService.generateHeatmapFor(Collections.emptyList());

            // Then
            assertThat(result.competencyHealth()).containsKey(competencyId1);
        }

        @Test
        @DisplayName("Should filter heatmap to include only specified competencies")
        void shouldFilterHeatmapToSpecifiedCompetencies() {
            // Given
            List<Object[]> rawCounts = createRawCounts(
                new Object[]{competencyId1, DifficultyLevel.FOUNDATIONAL, 10L},
                new Object[]{competencyId2, DifficultyLevel.INTERMEDIATE, 5L},
                new Object[]{competencyId3, DifficultyLevel.ADVANCED, 3L}
            );
            when(questionRepository.countQuestionsByCompetencyAndDifficulty()).thenReturn(rawCounts);

            // When - filter to only competency1 and competency2
            InventoryHeatmapDto result = inventoryHeatmapService.generateHeatmapFor(
                Arrays.asList(competencyId1, competencyId2)
            );

            // Then
            assertThat(result.competencyHealth()).hasSize(2);
            assertThat(result.competencyHealth()).containsKey(competencyId1);
            assertThat(result.competencyHealth()).containsKey(competencyId2);
            assertThat(result.competencyHealth()).doesNotContainKey(competencyId3);
        }

        @Test
        @DisplayName("Should filter detailed counts to include only specified competencies")
        void shouldFilterDetailedCountsToSpecifiedCompetencies() {
            // Given
            List<Object[]> rawCounts = createRawCounts(
                new Object[]{competencyId1, DifficultyLevel.FOUNDATIONAL, 10L},
                new Object[]{competencyId2, DifficultyLevel.INTERMEDIATE, 5L}
            );
            when(questionRepository.countQuestionsByCompetencyAndDifficulty()).thenReturn(rawCounts);

            // When - filter to only competency1
            InventoryHeatmapDto result = inventoryHeatmapService.generateHeatmapFor(
                Collections.singletonList(competencyId1)
            );

            // Then
            String includedKey = competencyId1.toString() + ":FOUNDATIONAL";
            String excludedKey = competencyId2.toString() + ":INTERMEDIATE";
            assertThat(result.detailedCounts()).containsKey(includedKey);
            assertThat(result.detailedCounts()).doesNotContainKey(excludedKey);
        }

        @Test
        @DisplayName("Should recalculate summary for filtered competencies")
        void shouldRecalculateSummaryForFilteredCompetencies() {
            // Given
            List<Object[]> rawCounts = createRawCounts(
                new Object[]{competencyId1, DifficultyLevel.FOUNDATIONAL, 10L}, // HEALTHY
                new Object[]{competencyId2, DifficultyLevel.INTERMEDIATE, 2L},  // CRITICAL
                new Object[]{competencyId3, DifficultyLevel.ADVANCED, 4L}       // MODERATE
            );
            when(questionRepository.countQuestionsByCompetencyAndDifficulty()).thenReturn(rawCounts);

            // When - filter to only include HEALTHY competency
            InventoryHeatmapDto result = inventoryHeatmapService.generateHeatmapFor(
                Collections.singletonList(competencyId1)
            );

            // Then
            assertThat(result.summary().totalCompetencies()).isEqualTo(1);
            assertThat(result.summary().healthyCount()).isEqualTo(1);
            assertThat(result.summary().criticalCount()).isZero();
            assertThat(result.summary().moderateCount()).isZero();
            assertThat(result.summary().totalQuestions()).isEqualTo(10L);
        }

        @Test
        @DisplayName("Should return empty result when specified competencies do not exist")
        void shouldReturnEmptyResultWhenCompetenciesDoNotExist() {
            // Given
            List<Object[]> rawCounts = createRawCounts(
                new Object[]{competencyId1, DifficultyLevel.FOUNDATIONAL, 10L}
            );
            when(questionRepository.countQuestionsByCompetencyAndDifficulty()).thenReturn(rawCounts);

            // When - filter with non-existent competency
            UUID nonExistentId = UUID.randomUUID();
            InventoryHeatmapDto result = inventoryHeatmapService.generateHeatmapFor(
                Collections.singletonList(nonExistentId)
            );

            // Then
            assertThat(result.competencyHealth()).isEmpty();
            assertThat(result.detailedCounts()).isEmpty();
            assertThat(result.summary().totalCompetencies()).isZero();
        }

        @Test
        @DisplayName("Should handle mix of valid and invalid competency IDs")
        void shouldHandleMixOfValidAndInvalidCompetencyIds() {
            // Given
            List<Object[]> rawCounts = createRawCounts(
                new Object[]{competencyId1, DifficultyLevel.FOUNDATIONAL, 10L}
            );
            when(questionRepository.countQuestionsByCompetencyAndDifficulty()).thenReturn(rawCounts);

            // When - include one valid and one invalid ID
            UUID nonExistentId = UUID.randomUUID();
            InventoryHeatmapDto result = inventoryHeatmapService.generateHeatmapFor(
                Arrays.asList(competencyId1, nonExistentId)
            );

            // Then - only valid competency should be included
            assertThat(result.competencyHealth()).hasSize(1);
            assertThat(result.competencyHealth()).containsKey(competencyId1);
        }
    }

    @Nested
    @DisplayName("getHealthForCompetency")
    class GetHealthForCompetencyTests {

        @Test
        @DisplayName("Should return CRITICAL for competency with 0 questions")
        void shouldReturnCriticalForZeroQuestions() {
            // Given
            when(questionRepository.countQuestionsByDifficultyForCompetency(competencyId1))
                .thenReturn(Collections.emptyList());

            // When
            HealthStatus result = inventoryHeatmapService.getHealthForCompetency(competencyId1);

            // Then
            assertThat(result).isEqualTo(HealthStatus.CRITICAL);
        }

        @Test
        @DisplayName("Should return CRITICAL for competency with 2 questions")
        void shouldReturnCriticalForTwoQuestions() {
            // Given
            List<Object[]> counts = createRawCounts(
                new Object[]{DifficultyLevel.FOUNDATIONAL, 2L}
            );
            when(questionRepository.countQuestionsByDifficultyForCompetency(competencyId1)).thenReturn(counts);

            // When
            HealthStatus result = inventoryHeatmapService.getHealthForCompetency(competencyId1);

            // Then
            assertThat(result).isEqualTo(HealthStatus.CRITICAL);
        }

        @Test
        @DisplayName("Should return MODERATE for competency with exactly 3 questions")
        void shouldReturnModerateForThreeQuestions() {
            // Given
            List<Object[]> counts = createRawCounts(
                new Object[]{DifficultyLevel.FOUNDATIONAL, 3L}
            );
            when(questionRepository.countQuestionsByDifficultyForCompetency(competencyId1)).thenReturn(counts);

            // When
            HealthStatus result = inventoryHeatmapService.getHealthForCompetency(competencyId1);

            // Then
            assertThat(result).isEqualTo(HealthStatus.MODERATE);
        }

        @Test
        @DisplayName("Should return MODERATE for competency with exactly 5 questions")
        void shouldReturnModerateForFiveQuestions() {
            // Given
            List<Object[]> counts = createRawCounts(
                new Object[]{DifficultyLevel.INTERMEDIATE, 5L}
            );
            when(questionRepository.countQuestionsByDifficultyForCompetency(competencyId1)).thenReturn(counts);

            // When
            HealthStatus result = inventoryHeatmapService.getHealthForCompetency(competencyId1);

            // Then
            assertThat(result).isEqualTo(HealthStatus.MODERATE);
        }

        @Test
        @DisplayName("Should return HEALTHY for competency with 6 questions")
        void shouldReturnHealthyForSixQuestions() {
            // Given
            List<Object[]> counts = createRawCounts(
                new Object[]{DifficultyLevel.ADVANCED, 6L}
            );
            when(questionRepository.countQuestionsByDifficultyForCompetency(competencyId1)).thenReturn(counts);

            // When
            HealthStatus result = inventoryHeatmapService.getHealthForCompetency(competencyId1);

            // Then
            assertThat(result).isEqualTo(HealthStatus.HEALTHY);
        }

        @Test
        @DisplayName("Should aggregate counts across multiple difficulties")
        void shouldAggregateCountsAcrossMultipleDifficulties() {
            // Given - 2 + 2 + 2 = 6 (HEALTHY)
            List<Object[]> counts = createRawCounts(
                new Object[]{DifficultyLevel.FOUNDATIONAL, 2L},
                new Object[]{DifficultyLevel.INTERMEDIATE, 2L},
                new Object[]{DifficultyLevel.ADVANCED, 2L}
            );
            when(questionRepository.countQuestionsByDifficultyForCompetency(competencyId1)).thenReturn(counts);

            // When
            HealthStatus result = inventoryHeatmapService.getHealthForCompetency(competencyId1);

            // Then
            assertThat(result).isEqualTo(HealthStatus.HEALTHY);
        }

        @Test
        @DisplayName("Should return HEALTHY for competency with many questions")
        void shouldReturnHealthyForManyQuestions() {
            // Given
            List<Object[]> counts = createRawCounts(
                new Object[]{DifficultyLevel.FOUNDATIONAL, 100L}
            );
            when(questionRepository.countQuestionsByDifficultyForCompetency(competencyId1)).thenReturn(counts);

            // When
            HealthStatus result = inventoryHeatmapService.getHealthForCompetency(competencyId1);

            // Then
            assertThat(result).isEqualTo(HealthStatus.HEALTHY);
        }
    }

    @Nested
    @DisplayName("checkSufficiency")
    class CheckSufficiencyTests {

        @Test
        @DisplayName("Should return zero shortage when sufficient questions available")
        void shouldReturnZeroShortageWhenSufficient() {
            // Given - 10 questions available, need 5
            List<Object[]> rawCounts = createRawCounts(
                new Object[]{competencyId1, DifficultyLevel.FOUNDATIONAL, 10L}
            );
            when(questionRepository.countQuestionsByCompetencyAndDifficulty()).thenReturn(rawCounts);

            // When
            Map<UUID, Integer> result = inventoryHeatmapService.checkSufficiency(
                Collections.singletonList(competencyId1), 5
            );

            // Then
            assertThat(result).containsEntry(competencyId1, 0);
        }

        @Test
        @DisplayName("Should return exact shortage when insufficient questions")
        void shouldReturnExactShortageWhenInsufficient() {
            // Given - 3 questions available, need 10
            List<Object[]> rawCounts = createRawCounts(
                new Object[]{competencyId1, DifficultyLevel.FOUNDATIONAL, 3L}
            );
            when(questionRepository.countQuestionsByCompetencyAndDifficulty()).thenReturn(rawCounts);

            // When
            Map<UUID, Integer> result = inventoryHeatmapService.checkSufficiency(
                Collections.singletonList(competencyId1), 10
            );

            // Then - shortage = 10 - 3 = 7
            assertThat(result).containsEntry(competencyId1, 7);
        }

        @Test
        @DisplayName("Should return zero when exactly enough questions")
        void shouldReturnZeroWhenExactlyEnough() {
            // Given - 5 questions available, need 5
            List<Object[]> rawCounts = createRawCounts(
                new Object[]{competencyId1, DifficultyLevel.FOUNDATIONAL, 5L}
            );
            when(questionRepository.countQuestionsByCompetencyAndDifficulty()).thenReturn(rawCounts);

            // When
            Map<UUID, Integer> result = inventoryHeatmapService.checkSufficiency(
                Collections.singletonList(competencyId1), 5
            );

            // Then
            assertThat(result).containsEntry(competencyId1, 0);
        }

        @Test
        @DisplayName("Should handle multiple competencies with mixed sufficiency")
        void shouldHandleMultipleCompetenciesWithMixedSufficiency() {
            // Given
            List<Object[]> rawCounts = createRawCounts(
                new Object[]{competencyId1, DifficultyLevel.FOUNDATIONAL, 10L},  // sufficient
                new Object[]{competencyId2, DifficultyLevel.INTERMEDIATE, 3L},   // insufficient
                new Object[]{competencyId3, DifficultyLevel.ADVANCED, 5L}        // sufficient
            );
            when(questionRepository.countQuestionsByCompetencyAndDifficulty()).thenReturn(rawCounts);

            // When - need 5 per competency
            Map<UUID, Integer> result = inventoryHeatmapService.checkSufficiency(
                Arrays.asList(competencyId1, competencyId2, competencyId3), 5
            );

            // Then
            assertThat(result).containsEntry(competencyId1, 0);   // 10 >= 5
            assertThat(result).containsEntry(competencyId2, 2);   // 5 - 3 = 2
            assertThat(result).containsEntry(competencyId3, 0);   // 5 >= 5
        }

        @Test
        @DisplayName("Should aggregate questions across difficulties for sufficiency check")
        void shouldAggregateAcrossDifficultiesForSufficiency() {
            // Given - 2 + 3 + 5 = 10 questions total
            List<Object[]> rawCounts = createRawCounts(
                new Object[]{competencyId1, DifficultyLevel.FOUNDATIONAL, 2L},
                new Object[]{competencyId1, DifficultyLevel.INTERMEDIATE, 3L},
                new Object[]{competencyId1, DifficultyLevel.ADVANCED, 5L}
            );
            when(questionRepository.countQuestionsByCompetencyAndDifficulty()).thenReturn(rawCounts);

            // When - need 8 questions
            Map<UUID, Integer> result = inventoryHeatmapService.checkSufficiency(
                Collections.singletonList(competencyId1), 8
            );

            // Then - 10 >= 8, no shortage
            assertThat(result).containsEntry(competencyId1, 0);
        }

        @Test
        @DisplayName("Should return full requirement as shortage when no questions exist")
        void shouldReturnFullRequirementWhenNoQuestions() {
            // Given - no questions for this competency
            when(questionRepository.countQuestionsByCompetencyAndDifficulty()).thenReturn(Collections.emptyList());

            // When - need 5 questions
            Map<UUID, Integer> result = inventoryHeatmapService.checkSufficiency(
                Collections.singletonList(competencyId1), 5
            );

            // Then
            assertThat(result).containsEntry(competencyId1, 5);
        }

        @Test
        @DisplayName("Should handle zero questions required")
        void shouldHandleZeroQuestionsRequired() {
            // Given
            List<Object[]> rawCounts = createRawCounts(
                new Object[]{competencyId1, DifficultyLevel.FOUNDATIONAL, 10L}
            );
            when(questionRepository.countQuestionsByCompetencyAndDifficulty()).thenReturn(rawCounts);

            // When - need 0 questions
            Map<UUID, Integer> result = inventoryHeatmapService.checkSufficiency(
                Collections.singletonList(competencyId1), 0
            );

            // Then
            assertThat(result).containsEntry(competencyId1, 0);
        }

        @Test
        @DisplayName("Should never return negative shortage")
        void shouldNeverReturnNegativeShortage() {
            // Given - 100 questions available, need only 5
            List<Object[]> rawCounts = createRawCounts(
                new Object[]{competencyId1, DifficultyLevel.FOUNDATIONAL, 100L}
            );
            when(questionRepository.countQuestionsByCompetencyAndDifficulty()).thenReturn(rawCounts);

            // When
            Map<UUID, Integer> result = inventoryHeatmapService.checkSufficiency(
                Collections.singletonList(competencyId1), 5
            );

            // Then - should be 0, not -95
            assertThat(result).containsEntry(competencyId1, 0);
            assertThat(result.get(competencyId1)).isGreaterThanOrEqualTo(0);
        }
    }

    @Nested
    @DisplayName("HeatmapSummary healthPercentage")
    class HealthPercentageTests {

        @Test
        @DisplayName("Should return 0% when no competencies")
        void shouldReturnZeroPercentWhenNoCompetencies() {
            // Given
            when(questionRepository.countQuestionsByCompetencyAndDifficulty()).thenReturn(Collections.emptyList());

            // When
            InventoryHeatmapDto result = inventoryHeatmapService.generateHeatmap();

            // Then
            assertThat(result.summary().healthPercentage()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return 100% when all competencies are healthy")
        void shouldReturn100PercentWhenAllHealthy() {
            // Given - all competencies have > 5 questions
            List<Object[]> rawCounts = createRawCounts(
                new Object[]{competencyId1, DifficultyLevel.FOUNDATIONAL, 10L},
                new Object[]{competencyId2, DifficultyLevel.INTERMEDIATE, 8L},
                new Object[]{competencyId3, DifficultyLevel.ADVANCED, 15L}
            );
            when(questionRepository.countQuestionsByCompetencyAndDifficulty()).thenReturn(rawCounts);

            // When
            InventoryHeatmapDto result = inventoryHeatmapService.generateHeatmap();

            // Then
            assertThat(result.summary().healthPercentage()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("Should return 0% when no competencies are healthy")
        void shouldReturnZeroPercentWhenNoneHealthy() {
            // Given - all competencies have < 6 questions
            List<Object[]> rawCounts = createRawCounts(
                new Object[]{competencyId1, DifficultyLevel.FOUNDATIONAL, 2L},  // CRITICAL
                new Object[]{competencyId2, DifficultyLevel.INTERMEDIATE, 4L}   // MODERATE
            );
            when(questionRepository.countQuestionsByCompetencyAndDifficulty()).thenReturn(rawCounts);

            // When
            InventoryHeatmapDto result = inventoryHeatmapService.generateHeatmap();

            // Then
            assertThat(result.summary().healthPercentage()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should calculate correct percentage for mixed statuses")
        void shouldCalculateCorrectPercentageForMixedStatuses() {
            // Given - 1 healthy out of 4 = 25%
            UUID id4 = UUID.randomUUID();
            List<Object[]> rawCounts = createRawCounts(
                new Object[]{competencyId1, DifficultyLevel.FOUNDATIONAL, 10L},  // HEALTHY
                new Object[]{competencyId2, DifficultyLevel.INTERMEDIATE, 2L},   // CRITICAL
                new Object[]{competencyId3, DifficultyLevel.ADVANCED, 4L},       // MODERATE
                new Object[]{id4, DifficultyLevel.EXPERT, 1L}                    // CRITICAL
            );
            when(questionRepository.countQuestionsByCompetencyAndDifficulty()).thenReturn(rawCounts);

            // When
            InventoryHeatmapDto result = inventoryHeatmapService.generateHeatmap();

            // Then - 1 healthy / 4 total = 25%
            assertThat(result.summary().healthPercentage()).isEqualTo(25.0);
        }

        @Test
        @DisplayName("Should handle single healthy competency as 100%")
        void shouldHandleSingleHealthyCompetencyAs100Percent() {
            // Given
            List<Object[]> rawCounts = createRawCounts(
                new Object[]{competencyId1, DifficultyLevel.FOUNDATIONAL, 10L}
            );
            when(questionRepository.countQuestionsByCompetencyAndDifficulty()).thenReturn(rawCounts);

            // When
            InventoryHeatmapDto result = inventoryHeatmapService.generateHeatmap();

            // Then
            assertThat(result.summary().healthPercentage()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("Should handle 50% healthy correctly")
        void shouldHandle50PercentHealthyCorrectly() {
            // Given - 2 healthy out of 4 = 50%
            UUID id4 = UUID.randomUUID();
            List<Object[]> rawCounts = createRawCounts(
                new Object[]{competencyId1, DifficultyLevel.FOUNDATIONAL, 10L},  // HEALTHY
                new Object[]{competencyId2, DifficultyLevel.INTERMEDIATE, 8L},   // HEALTHY
                new Object[]{competencyId3, DifficultyLevel.ADVANCED, 2L},       // CRITICAL
                new Object[]{id4, DifficultyLevel.EXPERT, 4L}                    // MODERATE
            );
            when(questionRepository.countQuestionsByCompetencyAndDifficulty()).thenReturn(rawCounts);

            // When
            InventoryHeatmapDto result = inventoryHeatmapService.generateHeatmap();

            // Then
            assertThat(result.summary().healthPercentage()).isEqualTo(50.0);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle all difficulty levels")
        void shouldHandleAllDifficultyLevels() {
            // Given
            List<Object[]> rawCounts = createRawCounts(
                new Object[]{competencyId1, DifficultyLevel.FOUNDATIONAL, 2L},
                new Object[]{competencyId1, DifficultyLevel.INTERMEDIATE, 2L},
                new Object[]{competencyId1, DifficultyLevel.ADVANCED, 2L},
                new Object[]{competencyId1, DifficultyLevel.EXPERT, 2L},
                new Object[]{competencyId1, DifficultyLevel.SPECIALIZED, 2L}
            );
            when(questionRepository.countQuestionsByCompetencyAndDifficulty()).thenReturn(rawCounts);

            // When
            InventoryHeatmapDto result = inventoryHeatmapService.generateHeatmap();

            // Then - 2*5 = 10 questions = HEALTHY
            assertThat(result.competencyHealth()).containsEntry(competencyId1, HealthStatus.HEALTHY);
            assertThat(result.detailedCounts()).hasSize(5);
        }

        @Test
        @DisplayName("Should verify repository is called once for generateHeatmap")
        void shouldVerifyRepositoryCalledOnce() {
            // Given
            when(questionRepository.countQuestionsByCompetencyAndDifficulty()).thenReturn(Collections.emptyList());

            // When
            inventoryHeatmapService.generateHeatmap();

            // Then
            verify(questionRepository, times(1)).countQuestionsByCompetencyAndDifficulty();
        }

        @Test
        @DisplayName("Should handle very large question counts")
        void shouldHandleVeryLargeQuestionCounts() {
            // Given
            List<Object[]> rawCounts = createRawCounts(
                new Object[]{competencyId1, DifficultyLevel.FOUNDATIONAL, 1000000L}
            );
            when(questionRepository.countQuestionsByCompetencyAndDifficulty()).thenReturn(rawCounts);

            // When
            InventoryHeatmapDto result = inventoryHeatmapService.generateHeatmap();

            // Then
            assertThat(result.competencyHealth()).containsEntry(competencyId1, HealthStatus.HEALTHY);
            assertThat(result.summary().totalQuestions()).isEqualTo(1000000L);
        }

        @Test
        @DisplayName("Should correctly count competencies across all statuses in summary")
        void shouldCorrectlyCountCompetenciesInSummary() {
            // Given - 2 critical, 1 moderate, 2 healthy
            UUID id4 = UUID.randomUUID();
            UUID id5 = UUID.randomUUID();
            List<Object[]> rawCounts = createRawCounts(
                new Object[]{competencyId1, DifficultyLevel.FOUNDATIONAL, 1L},   // CRITICAL
                new Object[]{competencyId2, DifficultyLevel.INTERMEDIATE, 2L},   // CRITICAL
                new Object[]{competencyId3, DifficultyLevel.ADVANCED, 4L},       // MODERATE
                new Object[]{id4, DifficultyLevel.EXPERT, 10L},                  // HEALTHY
                new Object[]{id5, DifficultyLevel.SPECIALIZED, 15L}              // HEALTHY
            );
            when(questionRepository.countQuestionsByCompetencyAndDifficulty()).thenReturn(rawCounts);

            // When
            InventoryHeatmapDto result = inventoryHeatmapService.generateHeatmap();

            // Then
            HeatmapSummary summary = result.summary();
            assertThat(summary.totalCompetencies()).isEqualTo(5);
            assertThat(summary.criticalCount()).isEqualTo(2);
            assertThat(summary.moderateCount()).isEqualTo(1);
            assertThat(summary.healthyCount()).isEqualTo(2);
            assertThat(summary.criticalCount() + summary.moderateCount() + summary.healthyCount())
                .isEqualTo(summary.totalCompetencies());
        }

        @Test
        @DisplayName("HealthStatus enum should have correct display names")
        void healthStatusEnumShouldHaveCorrectDisplayNames() {
            assertThat(HealthStatus.CRITICAL.getDisplayName()).isEqualTo("Critical");
            assertThat(HealthStatus.MODERATE.getDisplayName()).isEqualTo("Moderate");
            assertThat(HealthStatus.HEALTHY.getDisplayName()).isEqualTo("Healthy");
        }

        @Test
        @DisplayName("HealthStatus fromCount should handle boundary values correctly")
        void healthStatusFromCountShouldHandleBoundaryValues() {
            assertThat(HealthStatus.fromCount(0)).isEqualTo(HealthStatus.CRITICAL);
            assertThat(HealthStatus.fromCount(1)).isEqualTo(HealthStatus.CRITICAL);
            assertThat(HealthStatus.fromCount(2)).isEqualTo(HealthStatus.CRITICAL);
            assertThat(HealthStatus.fromCount(3)).isEqualTo(HealthStatus.MODERATE);
            assertThat(HealthStatus.fromCount(4)).isEqualTo(HealthStatus.MODERATE);
            assertThat(HealthStatus.fromCount(5)).isEqualTo(HealthStatus.MODERATE);
            assertThat(HealthStatus.fromCount(6)).isEqualTo(HealthStatus.HEALTHY);
            assertThat(HealthStatus.fromCount(100)).isEqualTo(HealthStatus.HEALTHY);
        }
    }
}
