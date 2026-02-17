package app.skillsoft.assessmentbackend.services.selection;

import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.repository.AssessmentQuestionRepository;
import app.skillsoft.assessmentbackend.repository.BehavioralIndicatorRepository;
import app.skillsoft.assessmentbackend.repository.ItemStatisticsRepository;
import app.skillsoft.assessmentbackend.services.validation.PsychometricBlueprintValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for QuestionSelectionServiceImpl.
 *
 * Tests cover:
 * - Single indicator selection with difficulty preference
 * - Empty pool handling
 * - Exclusion filtering
 * - Distribution strategies (WATERFALL, WEIGHTED, PRIORITY_FIRST)
 * - Competency-level selection
 * - Psychometric filtering (RETIRED exclusion)
 * - Difficulty preference sorting (ordinal distance)
 * - No duplicate questions in results
 *
 * @see QuestionSelectionService
 * @see DistributionStrategy
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("QuestionSelectionService Tests")
class QuestionSelectionServiceTest {

    @Mock
    private AssessmentQuestionRepository questionRepository;

    @Mock
    private BehavioralIndicatorRepository indicatorRepository;

    @Mock
    private PsychometricBlueprintValidator psychometricValidator;

    @Mock
    private ItemStatisticsRepository itemStatisticsRepository;

    @Mock
    private ExposureTrackingService exposureTrackingService;

    @InjectMocks
    private QuestionSelectionServiceImpl service;

    // Test data
    private UUID indicatorId1;
    private UUID indicatorId2;
    private UUID indicatorId3;
    private UUID competencyId1;
    private UUID competencyId2;
    private List<AssessmentQuestion> sampleQuestions;

    @BeforeEach
    void setUp() {
        indicatorId1 = UUID.randomUUID();
        indicatorId2 = UUID.randomUUID();
        indicatorId3 = UUID.randomUUID();
        competencyId1 = UUID.randomUUID();
        competencyId2 = UUID.randomUUID();
        sampleQuestions = new ArrayList<>();
    }

    // =====================================================================
    // SINGLE INDICATOR SELECTION TESTS
    // =====================================================================

    @Nested
    @DisplayName("selectQuestionsForIndicator Tests")
    class SelectQuestionsForIndicatorTests {

        @Test
        @DisplayName("should return empty list when no questions exist")
        void shouldReturnEmptyWhenNoQuestions() {
            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                    .thenReturn(List.of());

            List<UUID> result = service.selectQuestionsForIndicator(
                    indicatorId1, 5, DifficultyLevel.INTERMEDIATE, Set.of());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return questions when available")
        void shouldReturnQuestionsWhenAvailable() {
            List<AssessmentQuestion> questions = createQuestions(3, DifficultyLevel.INTERMEDIATE);
            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                    .thenReturn(questions);
            mockAllQuestionsEligible(questions);

            List<UUID> result = service.selectQuestionsForIndicator(
                    indicatorId1, 5, DifficultyLevel.INTERMEDIATE, Set.of());

            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("should limit results to maxQuestions")
        void shouldLimitToMaxQuestions() {
            List<AssessmentQuestion> questions = createQuestions(10, DifficultyLevel.INTERMEDIATE);
            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                    .thenReturn(questions);
            mockAllQuestionsEligible(questions);

            List<UUID> result = service.selectQuestionsForIndicator(
                    indicatorId1, 5, DifficultyLevel.INTERMEDIATE, Set.of());

            assertThat(result).hasSize(5);
        }

        @Test
        @DisplayName("should return partial results when insufficient questions")
        void shouldReturnPartialWhenInsufficient() {
            List<AssessmentQuestion> questions = createQuestions(2, DifficultyLevel.ADVANCED);
            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                    .thenReturn(questions);
            mockAllQuestionsEligible(questions);

            List<UUID> result = service.selectQuestionsForIndicator(
                    indicatorId1, 10, DifficultyLevel.ADVANCED, Set.of());

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("should exclude questions in exclusion set")
        void shouldExcludeSpecifiedQuestions() {
            List<AssessmentQuestion> questions = createQuestions(5, DifficultyLevel.INTERMEDIATE);
            Set<UUID> exclude = Set.of(questions.get(0).getId(), questions.get(1).getId());

            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                    .thenReturn(questions);
            mockAllQuestionsEligible(questions);

            List<UUID> result = service.selectQuestionsForIndicator(
                    indicatorId1, 10, DifficultyLevel.INTERMEDIATE, exclude);

            assertThat(result).hasSize(3);
            assertThat(result).doesNotContainAnyElementsOf(exclude);
        }

        @Test
        @DisplayName("should use overload without exclusions correctly")
        void shouldWorkWithoutExclusions() {
            List<AssessmentQuestion> questions = createQuestions(3, DifficultyLevel.FOUNDATIONAL);
            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                    .thenReturn(questions);
            mockAllQuestionsEligible(questions);

            List<UUID> result = service.selectQuestionsForIndicator(
                    indicatorId1, 5, DifficultyLevel.FOUNDATIONAL);

            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("should use overload without difficulty correctly")
        void shouldWorkWithoutDifficulty() {
            List<AssessmentQuestion> questions = createQuestionsWithMixedDifficulty(5);
            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                    .thenReturn(questions);
            mockAllQuestionsEligible(questions);

            List<UUID> result = service.selectQuestionsForIndicator(indicatorId1, 5);

            assertThat(result).hasSize(5);
        }
    }

    // =====================================================================
    // DIFFICULTY PREFERENCE SORTING TESTS
    // =====================================================================

    @Nested
    @DisplayName("Difficulty Preference Sorting Tests")
    class DifficultyPreferenceSortingTests {

        @Test
        @DisplayName("should prioritize exact difficulty match")
        void shouldPrioritizeExactMatch() {
            AssessmentQuestion exactMatch = createQuestion(DifficultyLevel.INTERMEDIATE);
            AssessmentQuestion oneAway = createQuestion(DifficultyLevel.ADVANCED);
            AssessmentQuestion twoAway = createQuestion(DifficultyLevel.EXPERT);
            List<AssessmentQuestion> questions = List.of(twoAway, oneAway, exactMatch);

            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                    .thenReturn(questions);
            mockAllQuestionsEligible(questions);

            List<UUID> result = service.selectQuestionsForIndicator(
                    indicatorId1, 1, DifficultyLevel.INTERMEDIATE, Set.of());

            assertThat(result).containsExactly(exactMatch.getId());
        }

        @Test
        @DisplayName("should sort by ordinal distance from preferred difficulty")
        void shouldSortByOrdinalDistance() {
            // FOUNDATIONAL=0, INTERMEDIATE=1, ADVANCED=2, EXPERT=3, SPECIALIZED=4
            // Preferred = ADVANCED (ordinal 2)
            AssessmentQuestion foundational = createQuestion(DifficultyLevel.FOUNDATIONAL); // distance 2
            AssessmentQuestion intermediate = createQuestion(DifficultyLevel.INTERMEDIATE); // distance 1
            AssessmentQuestion advanced = createQuestion(DifficultyLevel.ADVANCED);         // distance 0
            AssessmentQuestion expert = createQuestion(DifficultyLevel.EXPERT);             // distance 1
            AssessmentQuestion specialized = createQuestion(DifficultyLevel.SPECIALIZED);   // distance 2

            List<AssessmentQuestion> questions = List.of(specialized, foundational, expert, intermediate, advanced);

            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                    .thenReturn(questions);
            mockAllQuestionsEligible(questions);

            List<UUID> result = service.selectQuestionsForIndicator(
                    indicatorId1, 3, DifficultyLevel.ADVANCED, Set.of());

            // First should be exact match, then adjacent difficulties
            assertThat(result.get(0)).isEqualTo(advanced.getId());
            // Next two should be distance 1 (INTERMEDIATE or EXPERT)
            assertThat(result.subList(1, 3)).containsExactlyInAnyOrder(intermediate.getId(), expert.getId());
        }

        @ParameterizedTest
        @EnumSource(DifficultyLevel.class)
        @DisplayName("should handle all difficulty levels as preference")
        void shouldHandleAllDifficultyLevels(DifficultyLevel preferred) {
            List<AssessmentQuestion> questions = createQuestionsWithMixedDifficulty(5);
            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                    .thenReturn(questions);
            mockAllQuestionsEligible(questions);

            List<UUID> result = service.selectQuestionsForIndicator(
                    indicatorId1, 5, preferred, Set.of());

            assertThat(result).hasSize(5);
            assertThat(result).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("should shuffle when no difficulty preference")
        void shouldShuffleWhenNoDifficultyPreference() {
            List<AssessmentQuestion> questions = createQuestions(10, DifficultyLevel.INTERMEDIATE);
            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                    .thenReturn(questions);
            mockAllQuestionsEligible(questions);

            // Multiple runs should potentially return different orders (shuffle)
            List<UUID> result1 = service.selectQuestionsForIndicator(
                    indicatorId1, 10, null, Set.of());

            assertThat(result1).hasSize(10);
            assertThat(result1).doesNotHaveDuplicates();
        }
    }

    // =====================================================================
    // PSYCHOMETRIC FILTERING TESTS
    // =====================================================================

    @Nested
    @DisplayName("Psychometric Filtering Tests")
    class PsychometricFilteringTests {

        @Test
        @DisplayName("should exclude RETIRED questions")
        void shouldExcludeRetiredQuestions() {
            List<AssessmentQuestion> questions = createQuestions(5, DifficultyLevel.INTERMEDIATE);
            // Mark 2 questions as ineligible (RETIRED)
            when(psychometricValidator.isEligibleForAssembly(questions.get(0).getId())).thenReturn(false);
            when(psychometricValidator.isEligibleForAssembly(questions.get(1).getId())).thenReturn(false);
            when(psychometricValidator.isEligibleForAssembly(questions.get(2).getId())).thenReturn(true);
            when(psychometricValidator.isEligibleForAssembly(questions.get(3).getId())).thenReturn(true);
            when(psychometricValidator.isEligibleForAssembly(questions.get(4).getId())).thenReturn(true);

            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                    .thenReturn(questions);

            List<UUID> result = service.selectQuestionsForIndicator(
                    indicatorId1, 10, DifficultyLevel.INTERMEDIATE, Set.of());

            assertThat(result).hasSize(3);
            assertThat(result).doesNotContain(questions.get(0).getId(), questions.get(1).getId());
        }

        @Test
        @DisplayName("should return empty when all questions are RETIRED")
        void shouldReturnEmptyWhenAllRetired() {
            List<AssessmentQuestion> questions = createQuestions(3, DifficultyLevel.INTERMEDIATE);
            questions.forEach(q -> when(psychometricValidator.isEligibleForAssembly(q.getId())).thenReturn(false));

            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                    .thenReturn(questions);

            List<UUID> result = service.selectQuestionsForIndicator(
                    indicatorId1, 10, DifficultyLevel.INTERMEDIATE, Set.of());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("filterByValidity should exclude ineligible questions")
        void filterByValidityShouldExcludeIneligible() {
            List<AssessmentQuestion> questions = createQuestions(4, DifficultyLevel.INTERMEDIATE);
            when(psychometricValidator.isEligibleForAssembly(questions.get(0).getId())).thenReturn(true);
            when(psychometricValidator.isEligibleForAssembly(questions.get(1).getId())).thenReturn(false);
            when(psychometricValidator.isEligibleForAssembly(questions.get(2).getId())).thenReturn(true);
            when(psychometricValidator.isEligibleForAssembly(questions.get(3).getId())).thenReturn(false);

            List<AssessmentQuestion> result = service.filterByValidity(questions);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(AssessmentQuestion::getId)
                    .containsExactlyInAnyOrder(questions.get(0).getId(), questions.get(2).getId());
        }

        @Test
        @DisplayName("filterByValidity should return empty for null input")
        void filterByValidityShouldReturnEmptyForNull() {
            List<AssessmentQuestion> result = service.filterByValidity(null);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("filterByValidity should return empty for empty input")
        void filterByValidityShouldReturnEmptyForEmpty() {
            List<AssessmentQuestion> result = service.filterByValidity(List.of());
            assertThat(result).isEmpty();
        }
    }

    // =====================================================================
    // ELIGIBILITY CHECK TESTS
    // =====================================================================

    @Nested
    @DisplayName("isEligibleForAssembly Tests")
    class IsEligibleForAssemblyTests {

        @Test
        @DisplayName("should delegate to psychometric validator")
        void shouldDelegateToValidator() {
            UUID questionId = UUID.randomUUID();
            when(psychometricValidator.isEligibleForAssembly(questionId)).thenReturn(true);

            boolean result = service.isEligibleForAssembly(questionId);

            assertThat(result).isTrue();
            verify(psychometricValidator).isEligibleForAssembly(questionId);
        }

        @Test
        @DisplayName("should return false when validator returns false")
        void shouldReturnFalseWhenNotEligible() {
            UUID questionId = UUID.randomUUID();
            when(psychometricValidator.isEligibleForAssembly(questionId)).thenReturn(false);

            boolean result = service.isEligibleForAssembly(questionId);

            assertThat(result).isFalse();
        }
    }

    // =====================================================================
    // GET ELIGIBLE QUESTION COUNT TESTS
    // =====================================================================

    @Nested
    @DisplayName("getEligibleQuestionCount Tests")
    class GetEligibleQuestionCountTests {

        @Test
        @DisplayName("should count only eligible questions")
        void shouldCountOnlyEligibleQuestions() {
            List<AssessmentQuestion> questions = createQuestions(5, DifficultyLevel.INTERMEDIATE);
            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                    .thenReturn(questions);
            // 3 eligible, 2 not
            when(psychometricValidator.isEligibleForAssembly(questions.get(0).getId())).thenReturn(true);
            when(psychometricValidator.isEligibleForAssembly(questions.get(1).getId())).thenReturn(true);
            when(psychometricValidator.isEligibleForAssembly(questions.get(2).getId())).thenReturn(false);
            when(psychometricValidator.isEligibleForAssembly(questions.get(3).getId())).thenReturn(true);
            when(psychometricValidator.isEligibleForAssembly(questions.get(4).getId())).thenReturn(false);

            int count = service.getEligibleQuestionCount(indicatorId1);

            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("should return zero when no questions exist")
        void shouldReturnZeroWhenNoQuestions() {
            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                    .thenReturn(List.of());

            int count = service.getEligibleQuestionCount(indicatorId1);

            assertThat(count).isZero();
        }
    }

    // =====================================================================
    // WATERFALL DISTRIBUTION TESTS
    // =====================================================================

    @Nested
    @DisplayName("WATERFALL Distribution Strategy Tests")
    class WaterfallDistributionTests {

        @Test
        @DisplayName("should cycle through indicators in rounds")
        void shouldCycleThroughIndicatorsInRounds() {
            // Setup 3 indicators with 2 questions each
            List<AssessmentQuestion> questionsI1 = createQuestionsForIndicator(indicatorId1, 2);
            List<AssessmentQuestion> questionsI2 = createQuestionsForIndicator(indicatorId2, 2);
            List<AssessmentQuestion> questionsI3 = createQuestionsForIndicator(indicatorId3, 2);

            mockIndicatorQuestions(indicatorId1, questionsI1);
            mockIndicatorQuestions(indicatorId2, questionsI2);
            mockIndicatorQuestions(indicatorId3, questionsI3);
            mockAllQuestionsEligible(questionsI1);
            mockAllQuestionsEligible(questionsI2);
            mockAllQuestionsEligible(questionsI3);

            List<UUID> result = service.selectQuestionsWithDistribution(
                    List.of(indicatorId1, indicatorId2, indicatorId3),
                    6,
                    1,
                    DistributionStrategy.WATERFALL,
                    DifficultyLevel.INTERMEDIATE);

            assertThat(result).hasSize(6);
            // Round 1: I1-Q1, I2-Q1, I3-Q1
            // Round 2: I1-Q2, I2-Q2, I3-Q2
            assertThat(result).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("should ensure balanced coverage even if budget reached early")
        void shouldEnsureBalancedCoverage() {
            List<AssessmentQuestion> questionsI1 = createQuestionsForIndicator(indicatorId1, 5);
            List<AssessmentQuestion> questionsI2 = createQuestionsForIndicator(indicatorId2, 5);

            mockIndicatorQuestions(indicatorId1, questionsI1);
            mockIndicatorQuestions(indicatorId2, questionsI2);
            mockAllQuestionsEligible(questionsI1);
            mockAllQuestionsEligible(questionsI2);

            // Request only 4 questions across 2 indicators
            List<UUID> result = service.selectQuestionsWithDistribution(
                    List.of(indicatorId1, indicatorId2),
                    4,
                    1,
                    DistributionStrategy.WATERFALL,
                    DifficultyLevel.INTERMEDIATE);

            // Should have 2 from each indicator (balanced)
            assertThat(result).hasSize(4);

            // Count questions from each indicator
            long fromI1 = result.stream()
                    .filter(id -> questionsI1.stream().anyMatch(q -> q.getId().equals(id)))
                    .count();
            long fromI2 = result.stream()
                    .filter(id -> questionsI2.stream().anyMatch(q -> q.getId().equals(id)))
                    .count();

            assertThat(fromI1).isEqualTo(2);
            assertThat(fromI2).isEqualTo(2);
        }

        @Test
        @DisplayName("should handle indicator with fewer questions gracefully")
        void shouldHandleIndicatorWithFewerQuestions() {
            List<AssessmentQuestion> questionsI1 = createQuestionsForIndicator(indicatorId1, 5);
            List<AssessmentQuestion> questionsI2 = createQuestionsForIndicator(indicatorId2, 3);

            mockIndicatorQuestions(indicatorId1, questionsI1);
            mockIndicatorQuestions(indicatorId2, questionsI2);
            mockAllQuestionsEligible(questionsI1);
            mockAllQuestionsEligible(questionsI2);

            List<UUID> result = service.selectQuestionsWithDistribution(
                    List.of(indicatorId1, indicatorId2),
                    6,
                    1,
                    DistributionStrategy.WATERFALL,
                    DifficultyLevel.INTERMEDIATE);

            // Waterfall: Round1(I1-Q1, I2-Q1), Round2(I1-Q2, I2-Q2), Round3(I1-Q3, I2-Q3) = 6 total
            assertThat(result).hasSize(6);
            assertThat(result).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("should return empty for empty indicator list")
        void shouldReturnEmptyForEmptyIndicators() {
            List<UUID> result = service.selectQuestionsWithDistribution(
                    List.of(),
                    10,
                    2,
                    DistributionStrategy.WATERFALL,
                    DifficultyLevel.INTERMEDIATE);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty for null indicator list")
        void shouldReturnEmptyForNullIndicators() {
            List<UUID> result = service.selectQuestionsWithDistribution(
                    null,
                    10,
                    2,
                    DistributionStrategy.WATERFALL,
                    DifficultyLevel.INTERMEDIATE);

            assertThat(result).isEmpty();
        }
    }

    // =====================================================================
    // WEIGHTED DISTRIBUTION TESTS
    // =====================================================================

    @Nested
    @DisplayName("WEIGHTED Distribution Strategy Tests")
    class WeightedDistributionTests {

        @Test
        @DisplayName("should allocate proportionally by weight")
        void shouldAllocateProportionallyByWeight() {
            List<AssessmentQuestion> questionsI1 = createQuestionsForIndicator(indicatorId1, 10);
            List<AssessmentQuestion> questionsI2 = createQuestionsForIndicator(indicatorId2, 10);

            mockIndicatorQuestions(indicatorId1, questionsI1);
            mockIndicatorQuestions(indicatorId2, questionsI2);
            mockAllQuestionsEligible(questionsI1);
            mockAllQuestionsEligible(questionsI2);

            // Weight ratio 3:1 = 75%:25%
            Map<UUID, Double> weights = Map.of(
                    indicatorId1, 3.0,
                    indicatorId2, 1.0
            );

            List<UUID> result = service.selectQuestionsWeighted(
                    weights, 8, DifficultyLevel.INTERMEDIATE);

            assertThat(result).hasSize(8);
            // Higher weight should have more questions
            long fromI1 = result.stream()
                    .filter(id -> questionsI1.stream().anyMatch(q -> q.getId().equals(id)))
                    .count();
            long fromI2 = result.stream()
                    .filter(id -> questionsI2.stream().anyMatch(q -> q.getId().equals(id)))
                    .count();

            // 8 * 0.75 = 6 for I1, 8 * 0.25 = 2 for I2 (minimum 1)
            assertThat(fromI1).isGreaterThan(fromI2);
        }

        @Test
        @DisplayName("should ensure minimum 1 question for non-zero weights")
        void shouldEnsureMinimumOneQuestion() {
            List<AssessmentQuestion> questionsI1 = createQuestionsForIndicator(indicatorId1, 10);
            List<AssessmentQuestion> questionsI2 = createQuestionsForIndicator(indicatorId2, 10);

            mockIndicatorQuestions(indicatorId1, questionsI1);
            mockIndicatorQuestions(indicatorId2, questionsI2);
            mockAllQuestionsEligible(questionsI1);
            mockAllQuestionsEligible(questionsI2);

            // Weight ratio 9:1 with total 10 questions
            // I1 gets 9 * 10 / 10 = 9, I2 gets max(1, 1*10/10) = 1
            Map<UUID, Double> weights = Map.of(
                    indicatorId1, 9.0,
                    indicatorId2, 1.0
            );

            List<UUID> result = service.selectQuestionsWeighted(
                    weights, 10, DifficultyLevel.INTERMEDIATE);

            // I2 should still get at least 1 question despite small weight
            long fromI2 = result.stream()
                    .filter(id -> questionsI2.stream().anyMatch(q -> q.getId().equals(id)))
                    .count();

            assertThat(fromI2).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("should return empty for null weights")
        void shouldReturnEmptyForNullWeights() {
            List<UUID> result = service.selectQuestionsWeighted(
                    null, 10, DifficultyLevel.INTERMEDIATE);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty for empty weights")
        void shouldReturnEmptyForEmptyWeights() {
            List<UUID> result = service.selectQuestionsWeighted(
                    Map.of(), 10, DifficultyLevel.INTERMEDIATE);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty when total weight is zero")
        void shouldReturnEmptyWhenTotalWeightIsZero() {
            Map<UUID, Double> weights = Map.of(
                    indicatorId1, 0.0,
                    indicatorId2, 0.0
            );

            List<UUID> result = service.selectQuestionsWeighted(
                    weights, 10, DifficultyLevel.INTERMEDIATE);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should use equal weights when called via distribution with WEIGHTED strategy")
        void shouldUseEqualWeightsViaDistribution() {
            List<AssessmentQuestion> questionsI1 = createQuestionsForIndicator(indicatorId1, 5);
            List<AssessmentQuestion> questionsI2 = createQuestionsForIndicator(indicatorId2, 5);

            mockIndicatorQuestions(indicatorId1, questionsI1);
            mockIndicatorQuestions(indicatorId2, questionsI2);
            mockAllQuestionsEligible(questionsI1);
            mockAllQuestionsEligible(questionsI2);

            List<UUID> result = service.selectQuestionsWithDistribution(
                    List.of(indicatorId1, indicatorId2),
                    4,
                    1,
                    DistributionStrategy.WEIGHTED,
                    DifficultyLevel.INTERMEDIATE);

            // Equal weights should give roughly equal distribution
            assertThat(result).hasSize(4);
        }
    }

    // =====================================================================
    // PRIORITY_FIRST DISTRIBUTION TESTS
    // =====================================================================

    @Nested
    @DisplayName("PRIORITY_FIRST Distribution Strategy Tests")
    class PriorityFirstDistributionTests {

        @Test
        @DisplayName("should fill highest priority indicators first")
        void shouldFillHighestPriorityFirst() {
            List<AssessmentQuestion> questionsI1 = createQuestionsForIndicator(indicatorId1, 5);
            List<AssessmentQuestion> questionsI2 = createQuestionsForIndicator(indicatorId2, 5);

            mockIndicatorQuestions(indicatorId1, questionsI1);
            mockIndicatorQuestions(indicatorId2, questionsI2);
            mockAllQuestionsEligible(questionsI1);
            mockAllQuestionsEligible(questionsI2);

            // Request 6 questions with 3 per indicator
            // Priority order: I1 > I2 (I3 not needed)
            List<UUID> result = service.selectQuestionsWithDistribution(
                    List.of(indicatorId1, indicatorId2),
                    6,
                    3,
                    DistributionStrategy.PRIORITY_FIRST,
                    DifficultyLevel.INTERMEDIATE);

            assertThat(result).hasSize(6);

            // First 3 should be from I1 (highest priority)
            long fromI1 = result.stream()
                    .filter(id -> questionsI1.stream().anyMatch(q -> q.getId().equals(id)))
                    .count();
            // Next 3 should be from I2
            long fromI2 = result.stream()
                    .filter(id -> questionsI2.stream().anyMatch(q -> q.getId().equals(id)))
                    .count();

            assertThat(fromI1).isEqualTo(3);
            assertThat(fromI2).isEqualTo(3);
        }

        @Test
        @DisplayName("should stop when budget exhausted")
        void shouldStopWhenBudgetExhausted() {
            List<AssessmentQuestion> questionsI1 = createQuestionsForIndicator(indicatorId1, 10);

            mockIndicatorQuestions(indicatorId1, questionsI1);
            mockAllQuestionsEligible(questionsI1);

            // Request 5 questions with 10 per indicator (but only 5 total)
            // Only I1 is needed since budget is 5 and I1 has 10
            List<UUID> result = service.selectQuestionsWithDistribution(
                    List.of(indicatorId1),
                    5,
                    10,
                    DistributionStrategy.PRIORITY_FIRST,
                    DifficultyLevel.INTERMEDIATE);

            assertThat(result).hasSize(5);
            // All 5 should be from I1 (first priority)
            long fromI1 = result.stream()
                    .filter(id -> questionsI1.stream().anyMatch(q -> q.getId().equals(id)))
                    .count();
            assertThat(fromI1).isEqualTo(5);
        }

        @Test
        @DisplayName("should respect budget across multiple indicators")
        void shouldRespectBudgetAcrossMultipleIndicators() {
            List<AssessmentQuestion> questionsI1 = createQuestionsForIndicator(indicatorId1, 3);
            List<AssessmentQuestion> questionsI2 = createQuestionsForIndicator(indicatorId2, 3);
            List<AssessmentQuestion> questionsI3 = createQuestionsForIndicator(indicatorId3, 3);

            mockIndicatorQuestions(indicatorId1, questionsI1);
            mockIndicatorQuestions(indicatorId2, questionsI2);
            mockIndicatorQuestions(indicatorId3, questionsI3);
            mockAllQuestionsEligible(questionsI1);
            mockAllQuestionsEligible(questionsI2);
            mockAllQuestionsEligible(questionsI3);

            // Request 7 questions with 3 per indicator
            List<UUID> result = service.selectQuestionsWithDistribution(
                    List.of(indicatorId1, indicatorId2, indicatorId3),
                    7,
                    3,
                    DistributionStrategy.PRIORITY_FIRST,
                    DifficultyLevel.INTERMEDIATE);

            assertThat(result).hasSize(7);
            assertThat(result).doesNotHaveDuplicates();

            // I1 gets 3, I2 gets 3, I3 gets 1
            long fromI1 = result.stream()
                    .filter(id -> questionsI1.stream().anyMatch(q -> q.getId().equals(id)))
                    .count();
            long fromI2 = result.stream()
                    .filter(id -> questionsI2.stream().anyMatch(q -> q.getId().equals(id)))
                    .count();
            long fromI3 = result.stream()
                    .filter(id -> questionsI3.stream().anyMatch(q -> q.getId().equals(id)))
                    .count();

            assertThat(fromI1).isEqualTo(3);
            assertThat(fromI2).isEqualTo(3);
            assertThat(fromI3).isEqualTo(1);
        }
    }

    // =====================================================================
    // COMPETENCY-LEVEL SELECTION TESTS
    // =====================================================================

    @Nested
    @DisplayName("selectQuestionsForCompetency Tests")
    class SelectQuestionsForCompetencyTests {

        @Test
        @DisplayName("should get indicators for competency and select questions")
        void shouldSelectQuestionsViaIndicators() {
            BehavioralIndicator indicator1 = createIndicator(indicatorId1, competencyId1, 1.0f);
            BehavioralIndicator indicator2 = createIndicator(indicatorId2, competencyId1, 0.5f);

            List<AssessmentQuestion> questionsI1 = createQuestionsForIndicator(indicatorId1, 3);
            List<AssessmentQuestion> questionsI2 = createQuestionsForIndicator(indicatorId2, 3);

            when(indicatorRepository.findByCompetencyId(competencyId1))
                    .thenReturn(List.of(indicator1, indicator2));
            mockIndicatorQuestions(indicatorId1, questionsI1);
            mockIndicatorQuestions(indicatorId2, questionsI2);
            mockAllQuestionsEligible(questionsI1);
            mockAllQuestionsEligible(questionsI2);

            List<UUID> result = service.selectQuestionsForCompetency(
                    competencyId1, 6, 2, DifficultyLevel.INTERMEDIATE);

            assertThat(result).isNotEmpty();
            assertThat(result).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("should return empty for competency with no active indicators")
        void shouldReturnEmptyForNoActiveIndicators() {
            BehavioralIndicator inactiveIndicator = createIndicator(indicatorId1, competencyId1, 1.0f);
            inactiveIndicator.setActive(false);

            when(indicatorRepository.findByCompetencyId(competencyId1))
                    .thenReturn(List.of(inactiveIndicator));

            List<UUID> result = service.selectQuestionsForCompetency(
                    competencyId1, 10, 2, DifficultyLevel.INTERMEDIATE);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty when no indicators found")
        void shouldReturnEmptyWhenNoIndicators() {
            when(indicatorRepository.findByCompetencyId(competencyId1))
                    .thenReturn(List.of());

            List<UUID> result = service.selectQuestionsForCompetency(
                    competencyId1, 10, 2, DifficultyLevel.INTERMEDIATE);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should sort indicators by weight descending")
        void shouldSortIndicatorsByWeightDescending() {
            BehavioralIndicator lowWeight = createIndicator(indicatorId1, competencyId1, 0.3f);
            BehavioralIndicator highWeight = createIndicator(indicatorId2, competencyId1, 0.9f);

            List<AssessmentQuestion> questionsI1 = createQuestionsForIndicator(indicatorId1, 3);
            List<AssessmentQuestion> questionsI2 = createQuestionsForIndicator(indicatorId2, 3);

            when(indicatorRepository.findByCompetencyId(competencyId1))
                    .thenReturn(List.of(lowWeight, highWeight));
            mockIndicatorQuestions(indicatorId1, questionsI1);
            mockIndicatorQuestions(indicatorId2, questionsI2);
            mockAllQuestionsEligible(questionsI1);
            mockAllQuestionsEligible(questionsI2);

            List<UUID> result = service.selectQuestionsForCompetency(
                    competencyId1, 6, 2, DifficultyLevel.INTERMEDIATE);

            assertThat(result).isNotEmpty();
        }
    }

    // =====================================================================
    // SELECT QUESTIONS FOR COMPETENCIES TESTS
    // =====================================================================

    @Nested
    @DisplayName("selectQuestionsForCompetencies Tests")
    class SelectQuestionsForCompetenciesTests {

        @Test
        @DisplayName("should select questions across multiple competencies")
        void shouldSelectAcrossMultipleCompetencies() {
            BehavioralIndicator indicator1 = createIndicator(indicatorId1, competencyId1, 1.0f);
            BehavioralIndicator indicator2 = createIndicator(indicatorId2, competencyId2, 1.0f);

            List<AssessmentQuestion> questionsI1 = createQuestionsForIndicator(indicatorId1, 3);
            List<AssessmentQuestion> questionsI2 = createQuestionsForIndicator(indicatorId2, 3);

            when(indicatorRepository.findByCompetencyIdIn(anySet()))
                    .thenReturn(List.of(indicator1, indicator2));
            mockIndicatorQuestions(indicatorId1, questionsI1);
            mockIndicatorQuestions(indicatorId2, questionsI2);
            mockAllQuestionsEligible(questionsI1);
            mockAllQuestionsEligible(questionsI2);

            List<UUID> result = service.selectQuestionsForCompetencies(
                    List.of(competencyId1, competencyId2),
                    2,
                    DifficultyLevel.INTERMEDIATE,
                    false);

            assertThat(result).hasSize(4); // 2 indicators * 2 questions each
            assertThat(result).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("should shuffle when requested")
        void shouldShuffleWhenRequested() {
            BehavioralIndicator indicator1 = createIndicator(indicatorId1, competencyId1, 1.0f);
            List<AssessmentQuestion> questionsI1 = createQuestionsForIndicator(indicatorId1, 10);

            when(indicatorRepository.findByCompetencyIdIn(anySet()))
                    .thenReturn(List.of(indicator1));
            mockIndicatorQuestions(indicatorId1, questionsI1);
            mockAllQuestionsEligible(questionsI1);

            List<UUID> result = service.selectQuestionsForCompetencies(
                    List.of(competencyId1),
                    10,
                    DifficultyLevel.INTERMEDIATE,
                    true);

            assertThat(result).hasSize(10);
            assertThat(result).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("should return empty for null competency list")
        void shouldReturnEmptyForNullCompetencies() {
            List<UUID> result = service.selectQuestionsForCompetencies(
                    null,
                    2,
                    DifficultyLevel.INTERMEDIATE,
                    false);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty for empty competency list")
        void shouldReturnEmptyForEmptyCompetencies() {
            List<UUID> result = service.selectQuestionsForCompetencies(
                    List.of(),
                    2,
                    DifficultyLevel.INTERMEDIATE,
                    false);

            assertThat(result).isEmpty();
        }
    }

    // =====================================================================
    // EXPOSURE TRACKING VIA ExposureTrackingService TESTS
    // =====================================================================

    @Nested
    @DisplayName("ExposureTrackingService Delegation Tests")
    class ExposureTrackingDelegationTests {

        @Test
        @DisplayName("should delegate exposure tracking to ExposureTrackingService")
        void shouldDelegateToExposureTrackingService() {
            BehavioralIndicator indicator = createIndicator(indicatorId1, competencyId1, 1.0f);
            List<AssessmentQuestion> questions = createContextNeutralQuestionsForIndicator(indicatorId1, 3);

            when(indicatorRepository.findByCompetencyIdIn(anySet()))
                    .thenReturn(List.of(indicator));
            mockIndicatorQuestions(indicatorId1, questions);
            mockAllQuestionsEligible(questions);

            service.selectQuestionsForCompetencies(
                    List.of(competencyId1), 3, DifficultyLevel.INTERMEDIATE, false, true);

            // Verify ExposureTrackingService.trackExposure was called (not this.trackExposure)
            verify(exposureTrackingService).trackExposure(anyList());
        }

        @Test
        @DisplayName("should not call exposure tracking when no questions selected")
        void shouldNotCallExposureTrackingWhenEmpty() {
            when(indicatorRepository.findByCompetencyIdIn(anySet()))
                    .thenReturn(List.of());

            service.selectQuestionsForCompetencies(
                    List.of(competencyId1), 3, DifficultyLevel.INTERMEDIATE, false, true);

            verifyNoInteractions(exposureTrackingService);
        }
    }

    // =====================================================================
    // GRADUATED DIFFICULTY DISTRIBUTION TESTS
    // =====================================================================

    @Nested
    @DisplayName("Graduated Difficulty Distribution Tests")
    class GraduatedDifficultyDistributionTests {

        @Test
        @DisplayName("should select across FOUNDATIONAL, INTERMEDIATE, ADVANCED when questionsPerIndicator >= 3")
        void shouldSelectAcrossDifficultyBandsWhenThreeOrMore() {
            // Setup indicator with questions at each difficulty
            BehavioralIndicator indicator = createIndicator(indicatorId1, competencyId1, 1.0f);

            AssessmentQuestion foundational = createContextNeutralQuestion(DifficultyLevel.FOUNDATIONAL);
            foundational.setBehavioralIndicatorId(indicatorId1);
            AssessmentQuestion intermediate = createContextNeutralQuestion(DifficultyLevel.INTERMEDIATE);
            intermediate.setBehavioralIndicatorId(indicatorId1);
            AssessmentQuestion advanced = createContextNeutralQuestion(DifficultyLevel.ADVANCED);
            advanced.setBehavioralIndicatorId(indicatorId1);

            List<AssessmentQuestion> allQuestions = List.of(foundational, intermediate, advanced);

            when(indicatorRepository.findByCompetencyIdIn(anySet()))
                    .thenReturn(List.of(indicator));
            mockIndicatorQuestions(indicatorId1, allQuestions);
            mockAllQuestionsEligible(allQuestions);
            when(itemStatisticsRepository.findByQuestionIdIn(anySet()))
                    .thenReturn(List.of());

            // questionsPerIndicator = 3 -> triggers graduated difficulty
            List<UUID> result = service.selectQuestionsForCompetencies(
                    List.of(competencyId1), 3, DifficultyLevel.INTERMEDIATE, false, true);

            // Should get all 3 questions (one per difficulty band)
            assertThat(result).hasSize(3);
            assertThat(result).containsExactlyInAnyOrder(
                    foundational.getId(), intermediate.getId(), advanced.getId());
        }

        @Test
        @DisplayName("should fallback to any difficulty when a band has no questions")
        void shouldFallbackWhenBandHasNoQuestions() {
            // Setup indicator with only INTERMEDIATE questions (no FOUNDATIONAL or ADVANCED)
            BehavioralIndicator indicator = createIndicator(indicatorId1, competencyId1, 1.0f);

            AssessmentQuestion q1 = createContextNeutralQuestion(DifficultyLevel.INTERMEDIATE);
            q1.setBehavioralIndicatorId(indicatorId1);
            AssessmentQuestion q2 = createContextNeutralQuestion(DifficultyLevel.INTERMEDIATE);
            q2.setBehavioralIndicatorId(indicatorId1);
            AssessmentQuestion q3 = createContextNeutralQuestion(DifficultyLevel.INTERMEDIATE);
            q3.setBehavioralIndicatorId(indicatorId1);

            List<AssessmentQuestion> allQuestions = List.of(q1, q2, q3);

            when(indicatorRepository.findByCompetencyIdIn(anySet()))
                    .thenReturn(List.of(indicator));
            mockIndicatorQuestions(indicatorId1, allQuestions);
            mockAllQuestionsEligible(allQuestions);
            when(itemStatisticsRepository.findByQuestionIdIn(anySet()))
                    .thenReturn(List.of());

            // questionsPerIndicator = 3 -> graduated difficulty, but only INTERMEDIATE available
            List<UUID> result = service.selectQuestionsForCompetencies(
                    List.of(competencyId1), 3, DifficultyLevel.INTERMEDIATE, false, true);

            // Should still get 3 questions via fallback
            assertThat(result).hasSize(3);
            assertThat(result).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("should use single difficulty when questionsPerIndicator < 3")
        void shouldUseSingleDifficultyWhenLessThanThree() {
            BehavioralIndicator indicator = createIndicator(indicatorId1, competencyId1, 1.0f);

            AssessmentQuestion q1 = createContextNeutralQuestion(DifficultyLevel.INTERMEDIATE);
            q1.setBehavioralIndicatorId(indicatorId1);
            AssessmentQuestion q2 = createContextNeutralQuestion(DifficultyLevel.INTERMEDIATE);
            q2.setBehavioralIndicatorId(indicatorId1);

            List<AssessmentQuestion> allQuestions = List.of(q1, q2);

            when(indicatorRepository.findByCompetencyIdIn(anySet()))
                    .thenReturn(List.of(indicator));
            mockIndicatorQuestions(indicatorId1, allQuestions);
            mockAllQuestionsEligible(allQuestions);
            when(itemStatisticsRepository.findByQuestionIdIn(anySet()))
                    .thenReturn(List.of());

            // questionsPerIndicator = 2 -> single-difficulty path
            List<UUID> result = service.selectQuestionsForCompetencies(
                    List.of(competencyId1), 2, DifficultyLevel.INTERMEDIATE, false, true);

            assertThat(result).hasSize(2);
            assertThat(result).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("should produce no duplicates with graduated difficulty across multiple indicators")
        void shouldProduceNoDuplicatesAcrossIndicators() {
            BehavioralIndicator indicator1 = createIndicator(indicatorId1, competencyId1, 1.0f);
            BehavioralIndicator indicator2 = createIndicator(indicatorId2, competencyId1, 0.8f);

            // 3 questions per indicator at different difficulties
            List<AssessmentQuestion> questionsI1 = List.of(
                    createContextNeutralQuestionForIndicator(indicatorId1, DifficultyLevel.FOUNDATIONAL),
                    createContextNeutralQuestionForIndicator(indicatorId1, DifficultyLevel.INTERMEDIATE),
                    createContextNeutralQuestionForIndicator(indicatorId1, DifficultyLevel.ADVANCED));

            List<AssessmentQuestion> questionsI2 = List.of(
                    createContextNeutralQuestionForIndicator(indicatorId2, DifficultyLevel.FOUNDATIONAL),
                    createContextNeutralQuestionForIndicator(indicatorId2, DifficultyLevel.INTERMEDIATE),
                    createContextNeutralQuestionForIndicator(indicatorId2, DifficultyLevel.ADVANCED));

            when(indicatorRepository.findByCompetencyIdIn(anySet()))
                    .thenReturn(List.of(indicator1, indicator2));
            mockIndicatorQuestions(indicatorId1, questionsI1);
            mockIndicatorQuestions(indicatorId2, questionsI2);
            mockAllQuestionsEligible(questionsI1);
            mockAllQuestionsEligible(questionsI2);
            when(itemStatisticsRepository.findByQuestionIdIn(anySet()))
                    .thenReturn(List.of());

            List<UUID> result = service.selectQuestionsForCompetencies(
                    List.of(competencyId1), 3, DifficultyLevel.INTERMEDIATE, false, true);

            // 2 indicators * 3 questions = 6
            assertThat(result).hasSize(6);
            assertThat(result).doesNotHaveDuplicates();
        }
    }

    // =====================================================================
    // CONTEXT NEUTRALITY FILTER TESTS
    // =====================================================================

    @Nested
    @DisplayName("filterByContextNeutrality Tests")
    class FilterByContextNeutralityTests {

        @Test
        @DisplayName("should return only context-neutral questions")
        void shouldReturnOnlyContextNeutralQuestions() {
            AssessmentQuestion generalQuestion = createQuestionWithTag("GENERAL");
            AssessmentQuestion itQuestion = createQuestionWithTag("IT");
            AssessmentQuestion noTagQuestion = createQuestion(DifficultyLevel.INTERMEDIATE);

            List<AssessmentQuestion> questions = List.of(generalQuestion, itQuestion, noTagQuestion);

            List<AssessmentQuestion> result = service.filterByContextNeutrality(questions);

            // GENERAL and no-tag questions are context-neutral
            assertThat(result).hasSize(2);
            assertThat(result).contains(generalQuestion, noTagQuestion);
            assertThat(result).doesNotContain(itQuestion);
        }

        @Test
        @DisplayName("should return empty for null input")
        void shouldReturnEmptyForNull() {
            List<AssessmentQuestion> result = service.filterByContextNeutrality(null);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty for empty input")
        void shouldReturnEmptyForEmpty() {
            List<AssessmentQuestion> result = service.filterByContextNeutrality(List.of());
            assertThat(result).isEmpty();
        }
    }

    // =====================================================================
    // APPLY DIFFICULTY PREFERENCE TESTS
    // =====================================================================

    @Nested
    @DisplayName("applyDifficultyPreference Tests")
    class ApplyDifficultyPreferenceTests {

        @Test
        @DisplayName("should return empty for null questions")
        void shouldReturnEmptyForNullQuestions() {
            List<AssessmentQuestion> result = service.applyDifficultyPreference(
                    null, DifficultyLevel.INTERMEDIATE);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty for empty questions")
        void shouldReturnEmptyForEmptyQuestions() {
            List<AssessmentQuestion> result = service.applyDifficultyPreference(
                    List.of(), DifficultyLevel.INTERMEDIATE);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should shuffle when preference is null")
        void shouldShuffleWhenPreferenceNull() {
            List<AssessmentQuestion> questions = createQuestions(5, DifficultyLevel.INTERMEDIATE);

            List<AssessmentQuestion> result = service.applyDifficultyPreference(questions, null);

            assertThat(result).hasSize(5);
            assertThat(result).containsExactlyInAnyOrderElementsOf(questions);
        }
    }

    // =====================================================================
    // NO DUPLICATE TESTS
    // =====================================================================

    @Nested
    @DisplayName("No Duplicate Questions Tests")
    class NoDuplicateTests {

        @Test
        @DisplayName("should never return duplicate question IDs in single selection")
        void shouldNeverReturnDuplicatesInSingleSelection() {
            List<AssessmentQuestion> questions = createQuestions(10, DifficultyLevel.INTERMEDIATE);
            when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId1))
                    .thenReturn(questions);
            mockAllQuestionsEligible(questions);

            List<UUID> result = service.selectQuestionsForIndicator(
                    indicatorId1, 10, DifficultyLevel.INTERMEDIATE, Set.of());

            assertThat(result).hasSize(10);
            assertThat(result).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("should never return duplicate question IDs in distribution selection")
        void shouldNeverReturnDuplicatesInDistribution() {
            List<AssessmentQuestion> questionsI1 = createQuestionsForIndicator(indicatorId1, 5);
            List<AssessmentQuestion> questionsI2 = createQuestionsForIndicator(indicatorId2, 5);

            mockIndicatorQuestions(indicatorId1, questionsI1);
            mockIndicatorQuestions(indicatorId2, questionsI2);
            mockAllQuestionsEligible(questionsI1);
            mockAllQuestionsEligible(questionsI2);

            List<UUID> result = service.selectQuestionsWithDistribution(
                    List.of(indicatorId1, indicatorId2),
                    10,
                    2,
                    DistributionStrategy.WATERFALL,
                    DifficultyLevel.INTERMEDIATE);

            assertThat(result).hasSize(10);
            assertThat(result).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("should never return duplicate question IDs in weighted selection")
        void shouldNeverReturnDuplicatesInWeighted() {
            List<AssessmentQuestion> questionsI1 = createQuestionsForIndicator(indicatorId1, 5);
            List<AssessmentQuestion> questionsI2 = createQuestionsForIndicator(indicatorId2, 5);

            mockIndicatorQuestions(indicatorId1, questionsI1);
            mockIndicatorQuestions(indicatorId2, questionsI2);
            mockAllQuestionsEligible(questionsI1);
            mockAllQuestionsEligible(questionsI2);

            Map<UUID, Double> weights = Map.of(
                    indicatorId1, 2.0,
                    indicatorId2, 1.0
            );

            List<UUID> result = service.selectQuestionsWeighted(
                    weights, 8, DifficultyLevel.INTERMEDIATE);

            assertThat(result).doesNotHaveDuplicates();
        }
    }

    // =====================================================================
    // HELPER METHODS
    // =====================================================================

    private AssessmentQuestion createQuestion(DifficultyLevel difficulty) {
        AssessmentQuestion question = new AssessmentQuestion();
        question.setId(UUID.randomUUID());
        question.setDifficultyLevel(difficulty);
        question.setActive(true);
        question.setQuestionText("Test question");
        question.setScoringRubric("{}");
        question.setOrderIndex(1);
        return question;
    }

    private AssessmentQuestion createQuestionWithTag(String tag) {
        AssessmentQuestion question = createQuestion(DifficultyLevel.INTERMEDIATE);
        question.setMetadata(Map.of("tags", List.of(tag)));
        return question;
    }

    private List<AssessmentQuestion> createQuestions(int count, DifficultyLevel difficulty) {
        return IntStream.range(0, count)
                .mapToObj(i -> createQuestion(difficulty))
                .toList();
    }

    private List<AssessmentQuestion> createQuestionsWithMixedDifficulty(int count) {
        DifficultyLevel[] levels = DifficultyLevel.values();
        return IntStream.range(0, count)
                .mapToObj(i -> createQuestion(levels[i % levels.length]))
                .toList();
    }

    private List<AssessmentQuestion> createQuestionsForIndicator(UUID indicatorId, int count) {
        BehavioralIndicator indicator = new BehavioralIndicator();
        indicator.setId(indicatorId);

        return IntStream.range(0, count)
                .mapToObj(i -> {
                    AssessmentQuestion q = createQuestion(DifficultyLevel.INTERMEDIATE);
                    q.setBehavioralIndicator(indicator);
                    return q;
                })
                .toList();
    }

    private BehavioralIndicator createIndicator(UUID id, UUID competencyId, float weight) {
        BehavioralIndicator indicator = new BehavioralIndicator();
        indicator.setId(id);
        indicator.setWeight(weight);
        indicator.setActive(true);
        indicator.setTitle("Test Indicator");
        Competency competency = new Competency();
        competency.setId(competencyId);
        indicator.setCompetency(competency);
        return indicator;
    }

    private AssessmentQuestion createContextNeutralQuestion(DifficultyLevel difficulty) {
        AssessmentQuestion question = createQuestion(difficulty);
        question.setMetadata(Map.of("tags", List.of("GENERAL")));
        return question;
    }

    private AssessmentQuestion createContextNeutralQuestionForIndicator(UUID indicatorId, DifficultyLevel difficulty) {
        AssessmentQuestion question = createContextNeutralQuestion(difficulty);
        BehavioralIndicator indicator = new BehavioralIndicator();
        indicator.setId(indicatorId);
        question.setBehavioralIndicator(indicator);
        return question;
    }

    private List<AssessmentQuestion> createContextNeutralQuestionsForIndicator(UUID indicatorId, int count) {
        BehavioralIndicator indicator = new BehavioralIndicator();
        indicator.setId(indicatorId);

        return IntStream.range(0, count)
                .mapToObj(i -> {
                    AssessmentQuestion q = createContextNeutralQuestion(DifficultyLevel.INTERMEDIATE);
                    q.setBehavioralIndicator(indicator);
                    return q;
                })
                .toList();
    }

    private void mockIndicatorQuestions(UUID indicatorId, List<AssessmentQuestion> questions) {
        when(questionRepository.findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId))
                .thenReturn(new ArrayList<>(questions));
    }

    private void mockAllQuestionsEligible(List<AssessmentQuestion> questions) {
        questions.forEach(q ->
            when(psychometricValidator.isEligibleForAssembly(q.getId())).thenReturn(true));
    }
}
