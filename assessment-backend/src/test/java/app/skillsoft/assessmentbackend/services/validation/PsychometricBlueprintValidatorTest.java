package app.skillsoft.assessmentbackend.services.validation;

import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.repository.AssessmentQuestionRepository;
import app.skillsoft.assessmentbackend.repository.ItemStatisticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PsychometricBlueprintValidator.
 *
 * Tests cover:
 * - selectValidatedQuestions with psychometrics enabled/disabled
 * - Selection priority: ACTIVE > PROBATION > FLAGGED > never RETIRED
 * - Probation percentage limiting (20% cap by default)
 * - isEligibleForAssembly() behavior
 * - getAvailabilitySummary() counts per status
 * - hasSufficientQuestions() logic
 * - Missing statistics defaults to PROBATION status
 * - Edge cases (empty indicators, shuffling, etc.)
 *
 * Note: selectValidatedQuestions uses batch-loading via findByQuestionIdIn()
 * for partitioning questions by validity status (N+1 fix).
 * getAvailabilitySummary/isEligibleForAssembly use individual findByQuestion_Id().
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PsychometricBlueprintValidator Tests")
class PsychometricBlueprintValidatorTest {

    @Mock
    private ItemStatisticsRepository itemStatsRepository;

    @Mock
    private AssessmentQuestionRepository questionRepository;

    @InjectMocks
    private PsychometricBlueprintValidator validator;

    private UUID indicatorId1;
    private UUID indicatorId2;
    private UUID questionId1;
    private UUID questionId2;
    private UUID questionId3;
    private UUID questionId4;
    private UUID questionId5;

    @BeforeEach
    void setUp() {
        // Set default @Value properties
        ReflectionTestUtils.setField(validator, "psychometricsEnabled", true);
        ReflectionTestUtils.setField(validator, "probationPercentage", 20);

        // Initialize test UUIDs
        indicatorId1 = UUID.randomUUID();
        indicatorId2 = UUID.randomUUID();
        questionId1 = UUID.randomUUID();
        questionId2 = UUID.randomUUID();
        questionId3 = UUID.randomUUID();
        questionId4 = UUID.randomUUID();
        questionId5 = UUID.randomUUID();
    }

    // ================================================================================
    // Helper Methods
    // ================================================================================

    private AssessmentQuestion createQuestion(UUID id, UUID indicatorId, boolean active) {
        BehavioralIndicator indicator = new BehavioralIndicator();
        indicator.setId(indicatorId);

        AssessmentQuestion question = new AssessmentQuestion();
        question.setId(id);
        question.setBehavioralIndicator(indicator);
        question.setActive(active);
        question.setQuestionText("Test question " + id);
        question.setQuestionType(QuestionType.LIKERT);
        question.setDifficultyLevel(DifficultyLevel.INTERMEDIATE);
        question.setScoringRubric("Standard rubric");
        question.setOrderIndex(1);
        return question;
    }

    private ItemStatistics createStats(UUID questionId, ItemValidityStatus status) {
        AssessmentQuestion question = new AssessmentQuestion();
        question.setId(questionId);

        ItemStatistics stats = new ItemStatistics(question);
        stats.setId(UUID.randomUUID());
        stats.setValidityStatus(status);
        return stats;
    }

    /**
     * Helper to mock the batch findByQuestionIdIn() call used by partitionByValidityStatus.
     * Maps each questionId to the given status.
     */
    private void mockBatchStats(Map<UUID, ItemValidityStatus> questionStatuses) {
        List<ItemStatistics> statsList = new ArrayList<>();
        for (Map.Entry<UUID, ItemValidityStatus> entry : questionStatuses.entrySet()) {
            statsList.add(createStats(entry.getKey(), entry.getValue()));
        }
        when(itemStatsRepository.findByQuestionIdIn(anyCollection())).thenReturn(statsList);
    }

    // ================================================================================
    // selectValidatedQuestions Tests - Psychometrics Enabled
    // ================================================================================

    @Nested
    @DisplayName("selectValidatedQuestions with psychometrics enabled")
    class SelectValidatedQuestionsEnabledTests {

        @Test
        @DisplayName("should return empty list when no active questions for indicator")
        void shouldReturnEmptyListWhenNoActiveQuestions() {
            // Given
            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                .thenReturn(Collections.emptyList());

            // When
            List<UUID> result = validator.selectValidatedQuestions(List.of(indicatorId1), 5);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should prioritize ACTIVE items over other statuses")
        void shouldPrioritizeActiveItems() {
            // Given - 3 ACTIVE questions
            AssessmentQuestion q1 = createQuestion(questionId1, indicatorId1, true);
            AssessmentQuestion q2 = createQuestion(questionId2, indicatorId1, true);
            AssessmentQuestion q3 = createQuestion(questionId3, indicatorId1, true);

            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                .thenReturn(List.of(q1, q2, q3));

            // Mock batch stats loading (used by partitionByValidityStatus)
            mockBatchStats(Map.of(
                questionId1, ItemValidityStatus.ACTIVE,
                questionId2, ItemValidityStatus.ACTIVE,
                questionId3, ItemValidityStatus.ACTIVE
            ));

            // When - request 2 questions
            List<UUID> result = validator.selectValidatedQuestions(List.of(indicatorId1), 2);

            // Then - should select from ACTIVE pool
            assertThat(result).hasSize(2);
            assertThat(result).allMatch(id ->
                id.equals(questionId1) || id.equals(questionId2) || id.equals(questionId3));
        }

        @Test
        @DisplayName("should include limited PROBATION items for data gathering")
        void shouldIncludeLimitedProbationItems() {
            // Given - 2 ACTIVE and 3 PROBATION questions, request 10
            AssessmentQuestion qActive1 = createQuestion(questionId1, indicatorId1, true);
            AssessmentQuestion qActive2 = createQuestion(questionId2, indicatorId1, true);
            AssessmentQuestion qProb1 = createQuestion(questionId3, indicatorId1, true);
            AssessmentQuestion qProb2 = createQuestion(questionId4, indicatorId1, true);
            AssessmentQuestion qProb3 = createQuestion(questionId5, indicatorId1, true);

            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                .thenReturn(List.of(qActive1, qActive2, qProb1, qProb2, qProb3));

            // Mock batch stats
            mockBatchStats(Map.of(
                questionId1, ItemValidityStatus.ACTIVE,
                questionId2, ItemValidityStatus.ACTIVE,
                questionId3, ItemValidityStatus.PROBATION,
                questionId4, ItemValidityStatus.PROBATION,
                questionId5, ItemValidityStatus.PROBATION
            ));

            // When - request 10, probation target = max(1, 10*20/100) = 2
            List<UUID> result = validator.selectValidatedQuestions(List.of(indicatorId1), 10);

            // Then - should get 2 ACTIVE + 2 PROBATION = 4 (limited by available)
            assertThat(result).hasSize(4); // 2 active + 2 probation (20% of 10)
        }

        @Test
        @DisplayName("should never include RETIRED items")
        void shouldNeverIncludeRetiredItems() {
            // Given - 1 ACTIVE, 1 RETIRED
            AssessmentQuestion qActive = createQuestion(questionId1, indicatorId1, true);
            AssessmentQuestion qRetired = createQuestion(questionId2, indicatorId1, true);

            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                .thenReturn(List.of(qActive, qRetired));

            // Mock batch stats
            mockBatchStats(Map.of(
                questionId1, ItemValidityStatus.ACTIVE,
                questionId2, ItemValidityStatus.RETIRED
            ));

            // When
            List<UUID> result = validator.selectValidatedQuestions(List.of(indicatorId1), 5);

            // Then - only ACTIVE item selected (RETIRED is not in any selection pool)
            assertThat(result).containsExactly(questionId1);
            assertThat(result).doesNotContain(questionId2);
        }

        @Test
        @DisplayName("should include FLAGGED_FOR_REVIEW as fallback")
        void shouldIncludeFlaggedAsFallback() {
            // Given - 1 ACTIVE, 1 FLAGGED (need 3 questions)
            AssessmentQuestion qActive = createQuestion(questionId1, indicatorId1, true);
            AssessmentQuestion qFlagged = createQuestion(questionId2, indicatorId1, true);

            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                .thenReturn(List.of(qActive, qFlagged));

            // Mock batch stats
            mockBatchStats(Map.of(
                questionId1, ItemValidityStatus.ACTIVE,
                questionId2, ItemValidityStatus.FLAGGED_FOR_REVIEW
            ));

            // When - need 3 but only have 2 non-RETIRED
            List<UUID> result = validator.selectValidatedQuestions(List.of(indicatorId1), 3);

            // Then - should include FLAGGED as fallback
            assertThat(result).containsExactlyInAnyOrder(questionId1, questionId2);
        }

        @Test
        @DisplayName("should handle multiple indicators")
        void shouldHandleMultipleIndicators() {
            // Given - 2 indicators with 1 question each
            AssessmentQuestion q1 = createQuestion(questionId1, indicatorId1, true);
            AssessmentQuestion q2 = createQuestion(questionId2, indicatorId2, true);

            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                .thenReturn(List.of(q1));
            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId2))
                .thenReturn(List.of(q2));

            // Mock batch stats - called once per indicator
            when(itemStatsRepository.findByQuestionIdIn(anyCollection()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Collection<UUID> ids = (Collection<UUID>) invocation.getArgument(0);
                    List<ItemStatistics> result = new ArrayList<>();
                    for (UUID id : ids) {
                        result.add(createStats(id, ItemValidityStatus.ACTIVE));
                    }
                    return result;
                });

            // When
            List<UUID> result = validator.selectValidatedQuestions(
                List.of(indicatorId1, indicatorId2), 1);

            // Then - should get 1 from each indicator
            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyInAnyOrder(questionId1, questionId2);
        }

        @Test
        @DisplayName("should respect target count limit")
        void shouldRespectTargetCountLimit() {
            // Given - 5 ACTIVE questions, request only 2
            List<AssessmentQuestion> questions = new ArrayList<>();
            List<UUID> questionIds = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                UUID qId = UUID.randomUUID();
                questionIds.add(qId);
                questions.add(createQuestion(qId, indicatorId1, true));
            }

            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                .thenReturn(questions);

            // Mock batch stats - all ACTIVE
            when(itemStatsRepository.findByQuestionIdIn(anyCollection()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Collection<UUID> ids = (Collection<UUID>) invocation.getArgument(0);
                    List<ItemStatistics> result = new ArrayList<>();
                    for (UUID id : ids) {
                        result.add(createStats(id, ItemValidityStatus.ACTIVE));
                    }
                    return result;
                });

            // When
            List<UUID> result = validator.selectValidatedQuestions(List.of(indicatorId1), 2);

            // Then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("should handle empty indicator list")
        void shouldHandleEmptyIndicatorList() {
            // When
            List<UUID> result = validator.selectValidatedQuestions(Collections.emptyList(), 5);

            // Then
            assertThat(result).isEmpty();
            verifyNoInteractions(questionRepository);
        }
    }

    // ================================================================================
    // selectValidatedQuestions Tests - Psychometrics Disabled
    // ================================================================================

    @Nested
    @DisplayName("selectValidatedQuestions with psychometrics disabled")
    class SelectValidatedQuestionsDisabledTests {

        @BeforeEach
        void disablePsychometrics() {
            ReflectionTestUtils.setField(validator, "psychometricsEnabled", false);
        }

        @Test
        @DisplayName("should fallback to random selection when disabled")
        void shouldFallbackToRandomSelection() {
            // Given - use mutable ArrayList since service calls Collections.shuffle()
            AssessmentQuestion q1 = createQuestion(questionId1, indicatorId1, true);
            AssessmentQuestion q2 = createQuestion(questionId2, indicatorId1, true);
            AssessmentQuestion q3 = createQuestion(questionId3, indicatorId1, true);

            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                .thenReturn(new ArrayList<>(List.of(q1, q2, q3)));

            // When
            List<UUID> result = validator.selectValidatedQuestions(List.of(indicatorId1), 2);

            // Then - should select 2 questions without checking stats
            assertThat(result).hasSize(2);
            verifyNoInteractions(itemStatsRepository);
        }

        @Test
        @DisplayName("should return empty list when no questions available")
        void shouldReturnEmptyWhenNoQuestions() {
            // Given
            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                .thenReturn(Collections.emptyList());

            // When
            List<UUID> result = validator.selectValidatedQuestions(List.of(indicatorId1), 5);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should respect questionsPerIndicator limit when disabled")
        void shouldRespectLimitWhenDisabled() {
            // Given - 5 questions, request 3
            List<AssessmentQuestion> questions = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                questions.add(createQuestion(UUID.randomUUID(), indicatorId1, true));
            }

            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                .thenReturn(questions);

            // When
            List<UUID> result = validator.selectValidatedQuestions(List.of(indicatorId1), 3);

            // Then
            assertThat(result).hasSize(3);
        }
    }

    // ================================================================================
    // Probation Percentage Tests
    // ================================================================================

    @Nested
    @DisplayName("Probation percentage limiting")
    class ProbationPercentageTests {

        @Test
        @DisplayName("should limit probation items to configured percentage")
        void shouldLimitProbationToPercentage() {
            // Given - 5 ACTIVE, 10 PROBATION, 30% probation percentage
            ReflectionTestUtils.setField(validator, "probationPercentage", 30);

            List<AssessmentQuestion> questions = new ArrayList<>();
            List<UUID> activeIds = new ArrayList<>();
            List<UUID> probationIds = new ArrayList<>();

            // 5 ACTIVE
            for (int i = 0; i < 5; i++) {
                UUID qId = UUID.randomUUID();
                activeIds.add(qId);
                questions.add(createQuestion(qId, indicatorId1, true));
            }
            // 10 PROBATION
            for (int i = 0; i < 10; i++) {
                UUID qId = UUID.randomUUID();
                probationIds.add(qId);
                questions.add(createQuestion(qId, indicatorId1, true));
            }

            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                .thenReturn(questions);

            // Mock batch stats
            when(itemStatsRepository.findByQuestionIdIn(anyCollection()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Collection<UUID> ids = (Collection<UUID>) invocation.getArgument(0);
                    List<ItemStatistics> result = new ArrayList<>();
                    for (UUID id : ids) {
                        ItemValidityStatus status = activeIds.contains(id)
                            ? ItemValidityStatus.ACTIVE : ItemValidityStatus.PROBATION;
                        result.add(createStats(id, status));
                    }
                    return result;
                });

            // When - request 10, probation target = max(1, 10*30/100) = 3
            List<UUID> result = validator.selectValidatedQuestions(List.of(indicatorId1), 10);

            // Then - 5 active + 3 probation = 8
            assertThat(result).hasSize(8);
        }

        @Test
        @DisplayName("should include at least 1 probation item when percentage rounds to 0")
        void shouldIncludeAtLeastOneProbation() {
            // Given - small target where 20% rounds to 0
            ReflectionTestUtils.setField(validator, "probationPercentage", 20);

            AssessmentQuestion qActive = createQuestion(questionId1, indicatorId1, true);
            AssessmentQuestion qProb = createQuestion(questionId2, indicatorId1, true);

            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                .thenReturn(List.of(qActive, qProb));

            // Mock batch stats
            mockBatchStats(Map.of(
                questionId1, ItemValidityStatus.ACTIVE,
                questionId2, ItemValidityStatus.PROBATION
            ));

            // When - request 3, probation target = max(1, 3*20/100) = max(1,0) = 1
            List<UUID> result = validator.selectValidatedQuestions(List.of(indicatorId1), 3);

            // Then - should include the PROBATION item
            assertThat(result).hasSize(2);
            assertThat(result).contains(questionId2);
        }

        @Test
        @DisplayName("should handle 0% probation percentage")
        void shouldHandleZeroProbationPercentage() {
            // Given
            ReflectionTestUtils.setField(validator, "probationPercentage", 0);

            AssessmentQuestion qActive = createQuestion(questionId1, indicatorId1, true);
            AssessmentQuestion qProb = createQuestion(questionId2, indicatorId1, true);

            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                .thenReturn(List.of(qActive, qProb));

            // Mock batch stats
            mockBatchStats(Map.of(
                questionId1, ItemValidityStatus.ACTIVE,
                questionId2, ItemValidityStatus.PROBATION
            ));

            // When - max(1, 5*0/100) = 1, still includes 1 probation
            List<UUID> result = validator.selectValidatedQuestions(List.of(indicatorId1), 5);

            // Then - still gets at least 1 probation due to max(1, ...)
            assertThat(result).hasSize(2);
        }
    }

    // ================================================================================
    // isEligibleForAssembly Tests
    // ================================================================================

    @Nested
    @DisplayName("isEligibleForAssembly Tests")
    class IsEligibleForAssemblyTests {

        @Test
        @DisplayName("should return true for ACTIVE question")
        void shouldReturnTrueForActiveQuestion() {
            // Given
            AssessmentQuestion question = createQuestion(questionId1, indicatorId1, true);
            when(questionRepository.findById(questionId1)).thenReturn(Optional.of(question));
            when(itemStatsRepository.findByQuestion_Id(questionId1))
                .thenReturn(Optional.of(createStats(questionId1, ItemValidityStatus.ACTIVE)));

            // When
            boolean result = validator.isEligibleForAssembly(questionId1);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return true for PROBATION question")
        void shouldReturnTrueForProbationQuestion() {
            // Given
            AssessmentQuestion question = createQuestion(questionId1, indicatorId1, true);
            when(questionRepository.findById(questionId1)).thenReturn(Optional.of(question));
            when(itemStatsRepository.findByQuestion_Id(questionId1))
                .thenReturn(Optional.of(createStats(questionId1, ItemValidityStatus.PROBATION)));

            // When
            boolean result = validator.isEligibleForAssembly(questionId1);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return true for FLAGGED_FOR_REVIEW question")
        void shouldReturnTrueForFlaggedQuestion() {
            // Given
            AssessmentQuestion question = createQuestion(questionId1, indicatorId1, true);
            when(questionRepository.findById(questionId1)).thenReturn(Optional.of(question));
            when(itemStatsRepository.findByQuestion_Id(questionId1))
                .thenReturn(Optional.of(createStats(questionId1, ItemValidityStatus.FLAGGED_FOR_REVIEW)));

            // When
            boolean result = validator.isEligibleForAssembly(questionId1);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false for RETIRED question")
        void shouldReturnFalseForRetiredQuestion() {
            // Given
            AssessmentQuestion question = createQuestion(questionId1, indicatorId1, true);
            when(questionRepository.findById(questionId1)).thenReturn(Optional.of(question));
            when(itemStatsRepository.findByQuestion_Id(questionId1))
                .thenReturn(Optional.of(createStats(questionId1, ItemValidityStatus.RETIRED)));

            // When
            boolean result = validator.isEligibleForAssembly(questionId1);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false for inactive question")
        void shouldReturnFalseForInactiveQuestion() {
            // Given
            AssessmentQuestion question = createQuestion(questionId1, indicatorId1, false);
            when(questionRepository.findById(questionId1)).thenReturn(Optional.of(question));

            // When
            boolean result = validator.isEligibleForAssembly(questionId1);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false for non-existent question")
        void shouldReturnFalseForNonExistentQuestion() {
            // Given
            when(questionRepository.findById(questionId1)).thenReturn(Optional.empty());

            // When
            boolean result = validator.isEligibleForAssembly(questionId1);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should only check isActive when psychometrics disabled")
        void shouldOnlyCheckActiveWhenPsychometricsDisabled() {
            // Given
            ReflectionTestUtils.setField(validator, "psychometricsEnabled", false);
            AssessmentQuestion question = createQuestion(questionId1, indicatorId1, true);
            when(questionRepository.findById(questionId1)).thenReturn(Optional.of(question));

            // When
            boolean result = validator.isEligibleForAssembly(questionId1);

            // Then
            assertThat(result).isTrue();
            verifyNoInteractions(itemStatsRepository);
        }

        @Test
        @DisplayName("should return false for inactive question when psychometrics disabled")
        void shouldReturnFalseForInactiveWhenPsychometricsDisabled() {
            // Given
            ReflectionTestUtils.setField(validator, "psychometricsEnabled", false);
            AssessmentQuestion question = createQuestion(questionId1, indicatorId1, false);
            when(questionRepository.findById(questionId1)).thenReturn(Optional.of(question));

            // When
            boolean result = validator.isEligibleForAssembly(questionId1);

            // Then
            assertThat(result).isFalse();
        }
    }

    // ================================================================================
    // getAvailabilitySummary Tests
    // ================================================================================

    @Nested
    @DisplayName("getAvailabilitySummary Tests")
    class GetAvailabilitySummaryTests {

        @Test
        @DisplayName("should return counts for each status")
        void shouldReturnCountsForEachStatus() {
            // Given - 2 ACTIVE, 1 PROBATION, 1 FLAGGED, 1 RETIRED
            AssessmentQuestion q1 = createQuestion(questionId1, indicatorId1, true);
            AssessmentQuestion q2 = createQuestion(questionId2, indicatorId1, true);
            AssessmentQuestion q3 = createQuestion(questionId3, indicatorId1, true);
            AssessmentQuestion q4 = createQuestion(questionId4, indicatorId1, true);
            AssessmentQuestion q5 = createQuestion(questionId5, indicatorId1, true);

            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                .thenReturn(List.of(q1, q2, q3, q4, q5));

            when(itemStatsRepository.findByQuestion_Id(questionId1))
                .thenReturn(Optional.of(createStats(questionId1, ItemValidityStatus.ACTIVE)));
            when(itemStatsRepository.findByQuestion_Id(questionId2))
                .thenReturn(Optional.of(createStats(questionId2, ItemValidityStatus.ACTIVE)));
            when(itemStatsRepository.findByQuestion_Id(questionId3))
                .thenReturn(Optional.of(createStats(questionId3, ItemValidityStatus.PROBATION)));
            when(itemStatsRepository.findByQuestion_Id(questionId4))
                .thenReturn(Optional.of(createStats(questionId4, ItemValidityStatus.FLAGGED_FOR_REVIEW)));
            when(itemStatsRepository.findByQuestion_Id(questionId5))
                .thenReturn(Optional.of(createStats(questionId5, ItemValidityStatus.RETIRED)));

            // When
            Map<ItemValidityStatus, Integer> summary = validator.getAvailabilitySummary(indicatorId1);

            // Then
            assertThat(summary)
                .containsEntry(ItemValidityStatus.ACTIVE, 2)
                .containsEntry(ItemValidityStatus.PROBATION, 1)
                .containsEntry(ItemValidityStatus.FLAGGED_FOR_REVIEW, 1)
                .containsEntry(ItemValidityStatus.RETIRED, 1);
        }

        @Test
        @DisplayName("should return zeros for empty indicator")
        void shouldReturnZerosForEmptyIndicator() {
            // Given
            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                .thenReturn(Collections.emptyList());

            // When
            Map<ItemValidityStatus, Integer> summary = validator.getAvailabilitySummary(indicatorId1);

            // Then
            assertThat(summary).containsEntry(ItemValidityStatus.ACTIVE, 0);
            assertThat(summary).containsEntry(ItemValidityStatus.PROBATION, 0);
            assertThat(summary).containsEntry(ItemValidityStatus.FLAGGED_FOR_REVIEW, 0);
            assertThat(summary).containsEntry(ItemValidityStatus.RETIRED, 0);
        }

        @Test
        @DisplayName("should contain all status types in map")
        void shouldContainAllStatusTypes() {
            // Given
            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                .thenReturn(Collections.emptyList());

            // When
            Map<ItemValidityStatus, Integer> summary = validator.getAvailabilitySummary(indicatorId1);

            // Then - should have all enum values
            assertThat(summary.keySet())
                .containsExactlyInAnyOrder(ItemValidityStatus.values());
        }
    }

    // ================================================================================
    // hasSufficientQuestions Tests
    // ================================================================================

    @Nested
    @DisplayName("hasSufficientQuestions Tests")
    class HasSufficientQuestionsTests {

        @Test
        @DisplayName("should return true when enough eligible questions")
        void shouldReturnTrueWhenEnoughQuestions() {
            // Given - 3 ACTIVE, 2 PROBATION = 5 eligible
            AssessmentQuestion q1 = createQuestion(questionId1, indicatorId1, true);
            AssessmentQuestion q2 = createQuestion(questionId2, indicatorId1, true);
            AssessmentQuestion q3 = createQuestion(questionId3, indicatorId1, true);
            AssessmentQuestion q4 = createQuestion(questionId4, indicatorId1, true);
            AssessmentQuestion q5 = createQuestion(questionId5, indicatorId1, true);

            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                .thenReturn(List.of(q1, q2, q3, q4, q5));

            when(itemStatsRepository.findByQuestion_Id(questionId1))
                .thenReturn(Optional.of(createStats(questionId1, ItemValidityStatus.ACTIVE)));
            when(itemStatsRepository.findByQuestion_Id(questionId2))
                .thenReturn(Optional.of(createStats(questionId2, ItemValidityStatus.ACTIVE)));
            when(itemStatsRepository.findByQuestion_Id(questionId3))
                .thenReturn(Optional.of(createStats(questionId3, ItemValidityStatus.ACTIVE)));
            when(itemStatsRepository.findByQuestion_Id(questionId4))
                .thenReturn(Optional.of(createStats(questionId4, ItemValidityStatus.PROBATION)));
            when(itemStatsRepository.findByQuestion_Id(questionId5))
                .thenReturn(Optional.of(createStats(questionId5, ItemValidityStatus.PROBATION)));

            // When
            boolean result = validator.hasSufficientQuestions(indicatorId1, 5);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when not enough eligible questions")
        void shouldReturnFalseWhenNotEnoughQuestions() {
            // Given - 2 questions, need 5
            AssessmentQuestion q1 = createQuestion(questionId1, indicatorId1, true);
            AssessmentQuestion q2 = createQuestion(questionId2, indicatorId1, true);

            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                .thenReturn(List.of(q1, q2));

            when(itemStatsRepository.findByQuestion_Id(questionId1))
                .thenReturn(Optional.of(createStats(questionId1, ItemValidityStatus.ACTIVE)));
            when(itemStatsRepository.findByQuestion_Id(questionId2))
                .thenReturn(Optional.of(createStats(questionId2, ItemValidityStatus.ACTIVE)));

            // When
            boolean result = validator.hasSufficientQuestions(indicatorId1, 5);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should exclude RETIRED from count")
        void shouldExcludeRetiredFromCount() {
            // Given - 2 ACTIVE, 3 RETIRED = only 2 eligible
            AssessmentQuestion q1 = createQuestion(questionId1, indicatorId1, true);
            AssessmentQuestion q2 = createQuestion(questionId2, indicatorId1, true);
            AssessmentQuestion q3 = createQuestion(questionId3, indicatorId1, true);
            AssessmentQuestion q4 = createQuestion(questionId4, indicatorId1, true);
            AssessmentQuestion q5 = createQuestion(questionId5, indicatorId1, true);

            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                .thenReturn(List.of(q1, q2, q3, q4, q5));

            when(itemStatsRepository.findByQuestion_Id(questionId1))
                .thenReturn(Optional.of(createStats(questionId1, ItemValidityStatus.ACTIVE)));
            when(itemStatsRepository.findByQuestion_Id(questionId2))
                .thenReturn(Optional.of(createStats(questionId2, ItemValidityStatus.ACTIVE)));
            when(itemStatsRepository.findByQuestion_Id(questionId3))
                .thenReturn(Optional.of(createStats(questionId3, ItemValidityStatus.RETIRED)));
            when(itemStatsRepository.findByQuestion_Id(questionId4))
                .thenReturn(Optional.of(createStats(questionId4, ItemValidityStatus.RETIRED)));
            when(itemStatsRepository.findByQuestion_Id(questionId5))
                .thenReturn(Optional.of(createStats(questionId5, ItemValidityStatus.RETIRED)));

            // When - need 3 but only 2 are eligible
            boolean result = validator.hasSufficientQuestions(indicatorId1, 3);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should include FLAGGED_FOR_REVIEW in count")
        void shouldIncludeFlaggedInCount() {
            // Given - 1 ACTIVE, 1 FLAGGED = 2 eligible
            AssessmentQuestion q1 = createQuestion(questionId1, indicatorId1, true);
            AssessmentQuestion q2 = createQuestion(questionId2, indicatorId1, true);

            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                .thenReturn(List.of(q1, q2));

            when(itemStatsRepository.findByQuestion_Id(questionId1))
                .thenReturn(Optional.of(createStats(questionId1, ItemValidityStatus.ACTIVE)));
            when(itemStatsRepository.findByQuestion_Id(questionId2))
                .thenReturn(Optional.of(createStats(questionId2, ItemValidityStatus.FLAGGED_FOR_REVIEW)));

            // When
            boolean result = validator.hasSufficientQuestions(indicatorId1, 2);

            // Then
            assertThat(result).isTrue();
        }
    }

    // ================================================================================
    // getActiveQuestionCount Tests
    // ================================================================================

    @Nested
    @DisplayName("getActiveQuestionCount Tests")
    class GetActiveQuestionCountTests {

        @Test
        @DisplayName("should return count of ACTIVE questions")
        void shouldReturnActiveCount() {
            // Given
            AssessmentQuestion q1 = createQuestion(questionId1, indicatorId1, true);
            AssessmentQuestion q2 = createQuestion(questionId2, indicatorId1, true);
            AssessmentQuestion q3 = createQuestion(questionId3, indicatorId1, true);

            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                .thenReturn(List.of(q1, q2, q3));

            when(itemStatsRepository.findByQuestion_Id(questionId1))
                .thenReturn(Optional.of(createStats(questionId1, ItemValidityStatus.ACTIVE)));
            when(itemStatsRepository.findByQuestion_Id(questionId2))
                .thenReturn(Optional.of(createStats(questionId2, ItemValidityStatus.ACTIVE)));
            when(itemStatsRepository.findByQuestion_Id(questionId3))
                .thenReturn(Optional.of(createStats(questionId3, ItemValidityStatus.PROBATION)));

            // When
            int count = validator.getActiveQuestionCount(indicatorId1);

            // Then
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("should return 0 when no ACTIVE questions")
        void shouldReturnZeroWhenNoActive() {
            // Given
            AssessmentQuestion q1 = createQuestion(questionId1, indicatorId1, true);

            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                .thenReturn(List.of(q1));

            when(itemStatsRepository.findByQuestion_Id(questionId1))
                .thenReturn(Optional.of(createStats(questionId1, ItemValidityStatus.PROBATION)));

            // When
            int count = validator.getActiveQuestionCount(indicatorId1);

            // Then
            assertThat(count).isZero();
        }
    }

    // ================================================================================
    // getProbationQuestionCount Tests
    // ================================================================================

    @Nested
    @DisplayName("getProbationQuestionCount Tests")
    class GetProbationQuestionCountTests {

        @Test
        @DisplayName("should return count of PROBATION questions")
        void shouldReturnProbationCount() {
            // Given
            AssessmentQuestion q1 = createQuestion(questionId1, indicatorId1, true);
            AssessmentQuestion q2 = createQuestion(questionId2, indicatorId1, true);
            AssessmentQuestion q3 = createQuestion(questionId3, indicatorId1, true);

            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                .thenReturn(List.of(q1, q2, q3));

            when(itemStatsRepository.findByQuestion_Id(questionId1))
                .thenReturn(Optional.of(createStats(questionId1, ItemValidityStatus.PROBATION)));
            when(itemStatsRepository.findByQuestion_Id(questionId2))
                .thenReturn(Optional.of(createStats(questionId2, ItemValidityStatus.PROBATION)));
            when(itemStatsRepository.findByQuestion_Id(questionId3))
                .thenReturn(Optional.of(createStats(questionId3, ItemValidityStatus.ACTIVE)));

            // When
            int count = validator.getProbationQuestionCount(indicatorId1);

            // Then
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("should return 0 when no PROBATION questions")
        void shouldReturnZeroWhenNoProbation() {
            // Given
            AssessmentQuestion q1 = createQuestion(questionId1, indicatorId1, true);

            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                .thenReturn(List.of(q1));

            when(itemStatsRepository.findByQuestion_Id(questionId1))
                .thenReturn(Optional.of(createStats(questionId1, ItemValidityStatus.ACTIVE)));

            // When
            int count = validator.getProbationQuestionCount(indicatorId1);

            // Then
            assertThat(count).isZero();
        }
    }

    // ================================================================================
    // Missing Statistics Tests (Default to PROBATION)
    // ================================================================================

    @Nested
    @DisplayName("Missing statistics defaults to PROBATION")
    class MissingStatisticsTests {

        @Test
        @DisplayName("should treat missing stats as PROBATION in selection")
        void shouldTreatMissingStatsAsProbation() {
            // Given - question without statistics
            AssessmentQuestion q1 = createQuestion(questionId1, indicatorId1, true);
            AssessmentQuestion q2 = createQuestion(questionId2, indicatorId1, true);

            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                .thenReturn(List.of(q1, q2));

            // q1 has no stats (not returned by batch query), q2 is ACTIVE
            List<ItemStatistics> batchResult = new ArrayList<>();
            batchResult.add(createStats(questionId2, ItemValidityStatus.ACTIVE));
            when(itemStatsRepository.findByQuestionIdIn(anyCollection())).thenReturn(batchResult);

            // When
            List<UUID> result = validator.selectValidatedQuestions(List.of(indicatorId1), 5);

            // Then - both should be included (q1 as PROBATION, q2 as ACTIVE)
            assertThat(result).containsExactlyInAnyOrder(questionId1, questionId2);
        }

        @Test
        @DisplayName("should count missing stats as PROBATION in summary")
        void shouldCountMissingStatsAsProbation() {
            // Given
            AssessmentQuestion q1 = createQuestion(questionId1, indicatorId1, true);
            AssessmentQuestion q2 = createQuestion(questionId2, indicatorId1, true);

            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                .thenReturn(List.of(q1, q2));

            when(itemStatsRepository.findByQuestion_Id(questionId1))
                .thenReturn(Optional.empty()); // No stats
            when(itemStatsRepository.findByQuestion_Id(questionId2))
                .thenReturn(Optional.of(createStats(questionId2, ItemValidityStatus.ACTIVE)));

            // When
            Map<ItemValidityStatus, Integer> summary = validator.getAvailabilitySummary(indicatorId1);

            // Then
            assertThat(summary)
                .containsEntry(ItemValidityStatus.ACTIVE, 1)
                .containsEntry(ItemValidityStatus.PROBATION, 1);
        }

        @Test
        @DisplayName("should consider missing stats as eligible in isEligibleForAssembly")
        void shouldConsiderMissingStatsAsEligible() {
            // Given - active question without stats
            AssessmentQuestion question = createQuestion(questionId1, indicatorId1, true);
            when(questionRepository.findById(questionId1)).thenReturn(Optional.of(question));
            when(itemStatsRepository.findByQuestion_Id(questionId1))
                .thenReturn(Optional.empty());

            // When
            boolean result = validator.isEligibleForAssembly(questionId1);

            // Then - PROBATION is eligible
            assertThat(result).isTrue();
        }
    }

    // ================================================================================
    // Edge Cases and Error Handling
    // ================================================================================

    @Nested
    @DisplayName("Edge cases and error handling")
    class EdgeCaseTests {

        @Test
        @DisplayName("should handle null indicator IDs gracefully")
        void shouldHandleNullIndicatorIds() {
            // Given - list with null elements
            List<UUID> indicatorIds = new ArrayList<>();
            indicatorIds.add(indicatorId1);
            indicatorIds.add(null);

            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                .thenReturn(Collections.emptyList());
            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(null))
                .thenReturn(Collections.emptyList());

            // When/Then - should not throw
            assertThatCode(() -> validator.selectValidatedQuestions(indicatorIds, 5))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should return empty list when target count is 0")
        void shouldReturnEmptyWhenTargetIsZero() {
            // Given
            AssessmentQuestion q1 = createQuestion(questionId1, indicatorId1, true);
            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                .thenReturn(List.of(q1));

            // Mock batch stats
            mockBatchStats(Map.of(questionId1, ItemValidityStatus.ACTIVE));

            // When
            List<UUID> result = validator.selectValidatedQuestions(List.of(indicatorId1), 0);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should handle large number of questions")
        void shouldHandleLargeNumberOfQuestions() {
            // Given - 100 questions
            List<AssessmentQuestion> questions = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                UUID qId = UUID.randomUUID();
                questions.add(createQuestion(qId, indicatorId1, true));
            }

            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                .thenReturn(questions);

            // Mock batch stats - all ACTIVE
            when(itemStatsRepository.findByQuestionIdIn(anyCollection()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Collection<UUID> ids = (Collection<UUID>) invocation.getArgument(0);
                    List<ItemStatistics> result = new ArrayList<>();
                    for (UUID id : ids) {
                        result.add(createStats(id, ItemValidityStatus.ACTIVE));
                    }
                    return result;
                });

            // When
            List<UUID> result = validator.selectValidatedQuestions(List.of(indicatorId1), 50);

            // Then
            assertThat(result).hasSize(50);
        }

        @Test
        @DisplayName("should verify pool shuffling occurs")
        void shouldShufflePool() {
            // Given - multiple questions with same status
            List<AssessmentQuestion> questions = new ArrayList<>();
            List<UUID> questionIds = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                UUID qId = UUID.randomUUID();
                questionIds.add(qId);
                questions.add(createQuestion(qId, indicatorId1, true));
            }

            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                .thenReturn(questions);

            // Mock batch stats - all ACTIVE
            when(itemStatsRepository.findByQuestionIdIn(anyCollection()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Collection<UUID> ids = (Collection<UUID>) invocation.getArgument(0);
                    List<ItemStatistics> result = new ArrayList<>();
                    for (UUID id : ids) {
                        result.add(createStats(id, ItemValidityStatus.ACTIVE));
                    }
                    return result;
                });

            // When - run multiple times
            Set<List<UUID>> results = new HashSet<>();
            for (int i = 0; i < 20; i++) {
                List<UUID> result = validator.selectValidatedQuestions(List.of(indicatorId1), 5);
                results.add(result);
            }

            // Then - should have at least some variation due to shuffling
            // Note: This is probabilistic; 20 runs should yield some variation
            assertThat(results.size()).isGreaterThan(1);
        }
    }

    // ================================================================================
    // Selection Priority Tests
    // ================================================================================

    @Nested
    @DisplayName("Selection priority verification")
    class SelectionPriorityTests {

        @Test
        @DisplayName("should fill with ACTIVE first before adding PROBATION")
        void shouldFillWithActiveFirst() {
            // Given - 3 ACTIVE, 3 PROBATION, request 4
            AssessmentQuestion qA1 = createQuestion(questionId1, indicatorId1, true);
            AssessmentQuestion qA2 = createQuestion(questionId2, indicatorId1, true);
            AssessmentQuestion qA3 = createQuestion(questionId3, indicatorId1, true);
            AssessmentQuestion qP1 = createQuestion(questionId4, indicatorId1, true);
            AssessmentQuestion qP2 = createQuestion(questionId5, indicatorId1, true);
            UUID questionId6 = UUID.randomUUID();
            AssessmentQuestion qP3 = createQuestion(questionId6, indicatorId1, true);

            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                .thenReturn(List.of(qA1, qA2, qA3, qP1, qP2, qP3));

            // Mock batch stats
            Map<UUID, ItemValidityStatus> statuses = new HashMap<>();
            statuses.put(questionId1, ItemValidityStatus.ACTIVE);
            statuses.put(questionId2, ItemValidityStatus.ACTIVE);
            statuses.put(questionId3, ItemValidityStatus.ACTIVE);
            statuses.put(questionId4, ItemValidityStatus.PROBATION);
            statuses.put(questionId5, ItemValidityStatus.PROBATION);
            statuses.put(questionId6, ItemValidityStatus.PROBATION);
            mockBatchStats(statuses);

            // When - request 4, probation target = max(1, 4*20/100) = 1
            List<UUID> result = validator.selectValidatedQuestions(List.of(indicatorId1), 4);

            // Then - should have all 3 ACTIVE + 1 PROBATION
            assertThat(result).hasSize(4);
            // All ACTIVE should be present
            assertThat(result).containsAll(List.of(questionId1, questionId2, questionId3));
        }

        @Test
        @DisplayName("should add FLAGGED only when not enough ACTIVE+PROBATION")
        void shouldAddFlaggedOnlyWhenInsufficient() {
            // Given - 1 ACTIVE, 1 FLAGGED, request 3
            AssessmentQuestion qActive = createQuestion(questionId1, indicatorId1, true);
            AssessmentQuestion qFlagged = createQuestion(questionId2, indicatorId1, true);

            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                .thenReturn(List.of(qActive, qFlagged));

            // Mock batch stats
            mockBatchStats(Map.of(
                questionId1, ItemValidityStatus.ACTIVE,
                questionId2, ItemValidityStatus.FLAGGED_FOR_REVIEW
            ));

            // When - request 3 (but pool size is only 2)
            List<UUID> result = validator.selectValidatedQuestions(List.of(indicatorId1), 3);

            // Then - FLAGGED should be included as fallback
            assertThat(result).hasSize(2);
            assertThat(result).contains(questionId2);
        }

        @Test
        @DisplayName("should skip FLAGGED when enough ACTIVE+PROBATION available")
        void shouldSkipFlaggedWhenEnoughOthers() {
            // Given - 5 ACTIVE, 1 FLAGGED, request 3
            List<AssessmentQuestion> questions = new ArrayList<>();
            List<UUID> activeIds = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                UUID qId = UUID.randomUUID();
                activeIds.add(qId);
                questions.add(createQuestion(qId, indicatorId1, true));
            }
            UUID flaggedId = UUID.randomUUID();
            questions.add(createQuestion(flaggedId, indicatorId1, true));

            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                .thenReturn(questions);

            // Mock batch stats
            when(itemStatsRepository.findByQuestionIdIn(anyCollection()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Collection<UUID> ids = (Collection<UUID>) invocation.getArgument(0);
                    List<ItemStatistics> result = new ArrayList<>();
                    for (UUID id : ids) {
                        ItemValidityStatus status = id.equals(flaggedId)
                            ? ItemValidityStatus.FLAGGED_FOR_REVIEW : ItemValidityStatus.ACTIVE;
                        result.add(createStats(id, status));
                    }
                    return result;
                });

            // When - request 3
            List<UUID> result = validator.selectValidatedQuestions(List.of(indicatorId1), 3);

            // Then - FLAGGED should NOT be included (enough ACTIVE)
            assertThat(result).hasSize(3);
            assertThat(result).doesNotContain(flaggedId);
        }
    }
}
