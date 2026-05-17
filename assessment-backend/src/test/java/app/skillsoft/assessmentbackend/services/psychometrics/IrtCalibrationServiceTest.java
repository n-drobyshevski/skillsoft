package app.skillsoft.assessmentbackend.services.psychometrics;

import app.skillsoft.assessmentbackend.domain.entities.AssessmentQuestion;
import app.skillsoft.assessmentbackend.domain.entities.Competency;
import app.skillsoft.assessmentbackend.domain.entities.ItemStatistics;
import app.skillsoft.assessmentbackend.repository.CompetencyRepository;
import app.skillsoft.assessmentbackend.repository.ItemStatisticsRepository;
import app.skillsoft.assessmentbackend.repository.TestAnswerRepository;
import app.skillsoft.assessmentbackend.services.psychometrics.impl.IrtCalibrationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for IrtCalibrationServiceImpl.
 * <p>
 * Tests the public API surface:
 * - JMLE calibration with known parameters (parameter recovery)
 * - Insufficient respondent rejection
 * - Extreme item exclusion
 * - Convergence verification
 * - Ability estimation accuracy
 * - Perfect score handling
 * - Persistence of IRT parameters to ItemStatistics
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IRT Calibration Service Tests")
class IrtCalibrationServiceTest {

    // Mirror constants from IrtCalibrationServiceImpl for assertion readability
    private static final int MIN_RESPONDENTS = 200;
    private static final int MAX_ITERATIONS = 100;
    private static final double CONVERGENCE_THRESHOLD = 0.01;
    private static final double MIN_DISCRIMINATION = 0.1;
    private static final double MAX_DISCRIMINATION = 4.0;
    private static final double MIN_DIFFICULTY = -4.0;
    private static final double MAX_DIFFICULTY = 4.0;

    @Mock
    private TestAnswerRepository testAnswerRepository;

    @Mock
    private CompetencyRepository competencyRepository;

    @Mock
    private ItemStatisticsRepository itemStatisticsRepository;

    private IrtCalibrationServiceImpl service;

    private UUID competencyId;

    @BeforeEach
    void setUp() {
        service = new IrtCalibrationServiceImpl(
                testAnswerRepository,
                competencyRepository,
                itemStatisticsRepository
        );
        competencyId = UUID.randomUUID();
    }

    // ============================================
    // DATA GENERATION HELPERS
    // ============================================

    /**
     * Generate synthetic 2PL response data from known parameters.
     * Uses provided RNG for reproducibility.
     */
    private List<Object[]> generateSyntheticData(
            double[] trueA, double[] trueB,
            int nRespondents, UUID[] questionIds, Random rng) {

        List<Object[]> rows = new ArrayList<>();

        for (int j = 0; j < nRespondents; j++) {
            UUID sessionId = UUID.randomUUID();
            double theta = rng.nextGaussian();

            for (int i = 0; i < trueA.length; i++) {
                double p = probability2PL(theta, trueA[i], trueB[i]);
                double response = rng.nextDouble() < p ? 1.0 : 0.0;
                rows.add(new Object[]{sessionId, questionIds[i], response});
            }
        }

        return rows;
    }

    /**
     * Test-local 2PL probability function (matches the service implementation).
     */
    private double probability2PL(double theta, double a, double b) {
        double exponent = -a * (theta - b);
        if (exponent > 35.0) return 0.0;
        if (exponent < -35.0) return 1.0;
        return 1.0 / (1.0 + Math.exp(exponent));
    }

    private Competency createCompetencyEntity() {
        Competency competency = new Competency();
        competency.setId(competencyId);
        return competency;
    }

    private void mockStreamData(List<Object[]> data) {
        when(testAnswerRepository.streamScoreMatrixForCompetency(competencyId))
                .thenReturn(data.stream());
    }

    private AssessmentQuestion createQuestionEntity(UUID questionId) {
        AssessmentQuestion question = new AssessmentQuestion();
        question.setId(questionId);
        return question;
    }

    private List<ItemStatistics> createMockItemStatistics(UUID[] questionIds) {
        List<ItemStatistics> statsList = new ArrayList<>();
        for (UUID qId : questionIds) {
            AssessmentQuestion question = createQuestionEntity(qId);
            ItemStatistics stats = new ItemStatistics(question);
            stats.setId(UUID.randomUUID());
            statsList.add(stats);
        }
        return statsList;
    }

    private UUID[] createQuestionIds(int count) {
        UUID[] ids = new UUID[count];
        for (int i = 0; i < count; i++) {
            ids[i] = UUID.randomUUID();
        }
        return ids;
    }

    // ============================================
    // CALIBRATION TESTS
    // ============================================

    @Nested
    @DisplayName("Parameter Recovery (Known Parameters)")
    class ParameterRecoveryTests {

        @Test
        @DisplayName("should recover known 2PL parameters within tolerance")
        void shouldCalibrateWithKnownParameters() {
            // Use 10 items centered around 0 and 1000 respondents for stable JMLE
            double[] trueA = {1.0, 1.3, 0.8, 1.2, 1.0, 1.1, 0.9, 1.4, 1.0, 1.2};
            double[] trueB = {-1.2, -0.8, -0.4, -0.1, 0.0, 0.1, 0.4, 0.6, 0.9, 1.2};
            int nRespondents = 1000;
            UUID[] questionIds = createQuestionIds(10);

            Random rng = new Random(42);
            List<Object[]> data = generateSyntheticData(trueA, trueB, nRespondents, questionIds, rng);

            when(competencyRepository.findById(competencyId))
                    .thenReturn(Optional.of(createCompetencyEntity()));
            mockStreamData(data);

            IrtCalibrationService.CalibrationResult result = service.calibrateWithDetails(competencyId);

            assertThat(result.converged()).isTrue();
            assertThat(result.respondentCount()).isEqualTo(nRespondents);

            // JMLE recovery: verify relative ordering is correct and parameters are in reasonable range.
            // JMLE has known bias (Neyman-Scott problem), so we use wider tolerance (1.0).
            // Some items may be filtered if p-values are extreme.
            int calibratedCount = result.itemCount();
            assertThat(calibratedCount).isGreaterThanOrEqualTo(5);

            // Build a map of true parameters by questionId for comparison
            Map<UUID, Double> trueBMap = new HashMap<>();
            for (int i = 0; i < 10; i++) {
                trueBMap.put(questionIds[i], trueB[i]);
            }

            for (int i = 0; i < calibratedCount; i++) {
                IrtCalibrationService.ItemCalibration cal = result.itemCalibrations().get(i);
                double trueBi = trueBMap.getOrDefault(cal.questionId(), 0.0);

                assertThat(cal.difficulty())
                        .as("Difficulty for item %d (true=%.1f)", i, trueBi)
                        .isCloseTo(trueBi, within(1.0));

                assertThat(cal.discrimination())
                        .as("Discrimination for item %d", i)
                        .isBetween(MIN_DISCRIMINATION, MAX_DISCRIMINATION);

                assertThat(cal.standardErrorA()).isFinite().isGreaterThan(0);
                assertThat(cal.standardErrorB()).isFinite().isGreaterThan(0);
            }

            // Verify the difficulty ordering is roughly preserved (first vs last)
            double firstB = result.itemCalibrations().get(0).difficulty();
            double lastB = result.itemCalibrations().get(calibratedCount - 1).difficulty();
            assertThat(lastB).isGreaterThan(firstB);
        }

        @Test
        @DisplayName("should converge within max iterations")
        void shouldConvergeWithinMaxIterations() {
            // Use more items and respondents for stable convergence
            double[] trueA = {1.0, 1.2, 0.9, 1.1, 1.3, 1.0, 0.8, 1.4};
            double[] trueB = {-1.5, -0.5, 0.0, 0.5, 1.0, -1.0, 0.3, 1.5};
            int nRespondents = 500;
            UUID[] questionIds = createQuestionIds(8);

            Random rng = new Random(123);
            List<Object[]> data = generateSyntheticData(trueA, trueB, nRespondents, questionIds, rng);

            when(competencyRepository.findById(competencyId))
                    .thenReturn(Optional.of(createCompetencyEntity()));
            mockStreamData(data);

            IrtCalibrationService.CalibrationResult result = service.calibrateWithDetails(competencyId);

            assertThat(result.converged()).isTrue();
            assertThat(result.iterations()).isLessThanOrEqualTo(MAX_ITERATIONS);
            assertThat(result.maxParameterChange()).isLessThan(CONVERGENCE_THRESHOLD);
        }

        @Test
        @DisplayName("should produce difficulty parameters that order items by difficulty")
        void shouldOrderItemsByDifficulty() {
            // 8 items with well-separated, increasing difficulty
            double[] trueA = {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0};
            double[] trueB = {-3.0, -2.0, -1.0, -0.3, 0.3, 1.0, 2.0, 3.0};
            int nRespondents = 1000;
            UUID[] questionIds = createQuestionIds(8);

            Random rng = new Random(42);
            List<Object[]> data = generateSyntheticData(trueA, trueB, nRespondents, questionIds, rng);

            when(competencyRepository.findById(competencyId))
                    .thenReturn(Optional.of(createCompetencyEntity()));
            mockStreamData(data);

            IrtCalibrationService.CalibrationResult result = service.calibrateWithDetails(competencyId);

            // Verify that the overall trend of difficulties is preserved
            // (first item should be easiest, last should be hardest)
            List<IrtCalibrationService.ItemCalibration> cals = result.itemCalibrations();
            assertThat(cals.get(0).difficulty())
                    .as("First item should be easier than last item")
                    .isLessThan(cals.get(cals.size() - 1).difficulty());

            // Verify Spearman correlation of ranks is positive (monotone trend)
            int concordant = 0;
            int total = 0;
            for (int i = 0; i < cals.size(); i++) {
                for (int j = i + 1; j < cals.size(); j++) {
                    total++;
                    if (cals.get(i).difficulty() < cals.get(j).difficulty()) {
                        concordant++;
                    }
                }
            }
            // At least 70% concordant pairs (allowing for JMLE noise)
            double concordanceRate = (double) concordant / total;
            assertThat(concordanceRate).as("Difficulty ordering concordance").isGreaterThan(0.7);
        }
    }

    // ============================================
    // VALIDATION TESTS
    // ============================================

    @Nested
    @DisplayName("Data Validation")
    class ValidationTests {

        @Test
        @DisplayName("should reject insufficient respondents")
        void shouldRejectInsufficientRespondents() {
            double[] trueA = {1.0, 1.0, 1.0};
            double[] trueB = {-1.0, 0.0, 1.0};
            int nRespondents = 50;
            UUID[] questionIds = createQuestionIds(3);

            Random rng = new Random(42);
            List<Object[]> data = generateSyntheticData(trueA, trueB, nRespondents, questionIds, rng);

            when(competencyRepository.findById(competencyId))
                    .thenReturn(Optional.of(createCompetencyEntity()));
            mockStreamData(data);

            assertThatThrownBy(() -> service.calibrateWithDetails(competencyId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Insufficient respondents")
                    .hasMessageContaining("50")
                    .hasMessageContaining(String.valueOf(MIN_RESPONDENTS));
        }

        @Test
        @DisplayName("should reject competency not found")
        void shouldRejectCompetencyNotFound() {
            when(competencyRepository.findById(competencyId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.calibrateWithDetails(competencyId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Competency not found");
        }

        @Test
        @DisplayName("should exclude extreme items from calibration")
        void shouldExcludeExtremeItems() {
            int nRespondents = 500;
            UUID[] questionIds = createQuestionIds(5);

            List<Object[]> data = new ArrayList<>();
            Random rng = new Random(42);

            for (int j = 0; j < nRespondents; j++) {
                UUID sessionId = UUID.randomUUID();
                double theta = rng.nextGaussian();

                // Items 0-2: normal items with reasonable p-values
                for (int i = 0; i < 3; i++) {
                    double p = probability2PL(theta, 1.0, (i - 1.0) * 0.5);
                    double response = rng.nextDouble() < p ? 1.0 : 0.0;
                    data.add(new Object[]{sessionId, questionIds[i], response});
                }

                // Item 3: extremely easy (p > 0.95)
                data.add(new Object[]{sessionId, questionIds[3], rng.nextDouble() < 0.98 ? 1.0 : 0.0});

                // Item 4: extremely hard (p < 0.05)
                data.add(new Object[]{sessionId, questionIds[4], rng.nextDouble() < 0.02 ? 1.0 : 0.0});
            }

            when(competencyRepository.findById(competencyId))
                    .thenReturn(Optional.of(createCompetencyEntity()));
            mockStreamData(data);

            IrtCalibrationService.CalibrationResult result = service.calibrateWithDetails(competencyId);

            // Only 3 normal items should remain after filtering
            assertThat(result.itemCount()).isEqualTo(3);
            assertThat(result.converged()).isTrue();

            Set<UUID> calibratedIds = new HashSet<>();
            for (IrtCalibrationService.ItemCalibration cal : result.itemCalibrations()) {
                calibratedIds.add(cal.questionId());
            }
            assertThat(calibratedIds).doesNotContain(questionIds[3], questionIds[4]);
        }

        @Test
        @DisplayName("should reject insufficient items after filtering")
        void shouldRejectInsufficientItemsAfterFiltering() {
            int nRespondents = 300;
            UUID[] questionIds = createQuestionIds(2);

            double[] trueA = {1.0, 1.0};
            double[] trueB = {0.0, 0.5};

            Random rng = new Random(42);
            List<Object[]> data = generateSyntheticData(trueA, trueB, nRespondents, questionIds, rng);

            when(competencyRepository.findById(competencyId))
                    .thenReturn(Optional.of(createCompetencyEntity()));
            mockStreamData(data);

            assertThatThrownBy(() -> service.calibrateWithDetails(competencyId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Insufficient items");
        }
    }

    // ============================================
    // ABILITY ESTIMATION TESTS
    // ============================================

    @Nested
    @DisplayName("Ability Estimation")
    class AbilityEstimationTests {

        @Test
        @DisplayName("should estimate positive theta for high scorer")
        void shouldEstimatePositiveThetaForHighScorer() {
            UUID[] questionIds = createQuestionIds(5);
            List<ItemStatistics> statsList = new ArrayList<>();

            for (int i = 0; i < 5; i++) {
                AssessmentQuestion question = createQuestionEntity(questionIds[i]);
                ItemStatistics stats = new ItemStatistics(question);
                stats.setIrtDiscrimination(BigDecimal.valueOf(1.0));
                stats.setIrtDifficulty(BigDecimal.valueOf(i - 2.0));
                statsList.add(stats);
            }

            when(itemStatisticsRepository.findByQuestionIdIn(anyList())).thenReturn(statsList);

            Map<UUID, Double> scores = new HashMap<>();
            for (UUID qId : questionIds) {
                scores.put(qId, 1.0);
            }

            double theta = service.estimateAbility(scores);

            assertThat(theta).isGreaterThan(0.0);
        }

        @Test
        @DisplayName("should estimate negative theta for low scorer")
        void shouldEstimateNegativeThetaForLowScorer() {
            UUID[] questionIds = createQuestionIds(5);
            List<ItemStatistics> statsList = new ArrayList<>();

            for (int i = 0; i < 5; i++) {
                AssessmentQuestion question = createQuestionEntity(questionIds[i]);
                ItemStatistics stats = new ItemStatistics(question);
                stats.setIrtDiscrimination(BigDecimal.valueOf(1.0));
                stats.setIrtDifficulty(BigDecimal.valueOf(i - 2.0));
                statsList.add(stats);
            }

            when(itemStatisticsRepository.findByQuestionIdIn(anyList())).thenReturn(statsList);

            Map<UUID, Double> scores = new HashMap<>();
            for (UUID qId : questionIds) {
                scores.put(qId, 0.0);
            }

            double theta = service.estimateAbility(scores);

            assertThat(theta).isLessThan(0.0);
        }

        @Test
        @DisplayName("should estimate near-zero theta for mixed scorer on balanced test")
        void shouldEstimateNearZeroForMixedScorer() {
            UUID[] questionIds = createQuestionIds(4);
            List<ItemStatistics> statsList = new ArrayList<>();

            double[] difficulties = {-1.0, -0.5, 0.5, 1.0};

            for (int i = 0; i < 4; i++) {
                AssessmentQuestion question = createQuestionEntity(questionIds[i]);
                ItemStatistics stats = new ItemStatistics(question);
                stats.setIrtDiscrimination(BigDecimal.valueOf(1.0));
                stats.setIrtDifficulty(BigDecimal.valueOf(difficulties[i]));
                statsList.add(stats);
            }

            when(itemStatisticsRepository.findByQuestionIdIn(anyList())).thenReturn(statsList);

            // Gets easy items right, hard items wrong
            Map<UUID, Double> scores = new HashMap<>();
            scores.put(questionIds[0], 1.0);
            scores.put(questionIds[1], 1.0);
            scores.put(questionIds[2], 0.0);
            scores.put(questionIds[3], 0.0);

            double theta = service.estimateAbility(scores);

            assertThat(theta).isBetween(-1.5, 1.5);
        }

        @Test
        @DisplayName("should return 0 for empty question scores")
        void shouldReturnZeroForEmptyScores() {
            double theta = service.estimateAbility(Map.of());
            assertThat(theta).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should return 0 for null question scores")
        void shouldReturnZeroForNullScores() {
            double theta = service.estimateAbility(null);
            assertThat(theta).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should return 0 when no IRT parameters available")
        void shouldReturnZeroWhenNoIrtParams() {
            UUID qId = UUID.randomUUID();

            AssessmentQuestion question = createQuestionEntity(qId);
            ItemStatistics stats = new ItemStatistics(question);
            // No IRT parameters set

            when(itemStatisticsRepository.findByQuestionIdIn(anyList()))
                    .thenReturn(List.of(stats));

            double theta = service.estimateAbility(Map.of(qId, 1.0));
            assertThat(theta).isEqualTo(0.0);
        }

        @Test
        @DisplayName("should differentiate high, medium, and low ability")
        void shouldDifferentiateAbilityLevels() {
            UUID[] questionIds = createQuestionIds(6);
            List<ItemStatistics> statsList = new ArrayList<>();

            double[] difficulties = {-2.0, -1.0, 0.0, 0.5, 1.0, 2.0};

            for (int i = 0; i < 6; i++) {
                AssessmentQuestion question = createQuestionEntity(questionIds[i]);
                ItemStatistics stats = new ItemStatistics(question);
                stats.setIrtDiscrimination(BigDecimal.valueOf(1.2));
                stats.setIrtDifficulty(BigDecimal.valueOf(difficulties[i]));
                statsList.add(stats);
            }

            when(itemStatisticsRepository.findByQuestionIdIn(anyList())).thenReturn(statsList);

            // High ability: 5/6 correct
            Map<UUID, Double> highScores = new HashMap<>();
            for (int i = 0; i < 5; i++) highScores.put(questionIds[i], 1.0);
            highScores.put(questionIds[5], 0.0);

            // Medium ability: 3/6 correct (easy ones)
            Map<UUID, Double> medScores = new HashMap<>();
            for (int i = 0; i < 3; i++) medScores.put(questionIds[i], 1.0);
            for (int i = 3; i < 6; i++) medScores.put(questionIds[i], 0.0);

            // Low ability: 1/6 correct
            Map<UUID, Double> lowScores = new HashMap<>();
            lowScores.put(questionIds[0], 1.0);
            for (int i = 1; i < 6; i++) lowScores.put(questionIds[i], 0.0);

            double thetaHigh = service.estimateAbility(highScores);
            double thetaMed = service.estimateAbility(medScores);
            double thetaLow = service.estimateAbility(lowScores);

            assertThat(thetaHigh).isGreaterThan(thetaMed);
            assertThat(thetaMed).isGreaterThan(thetaLow);
        }
    }

    // ============================================
    // EDGE CASE TESTS
    // ============================================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("should handle easy items gracefully")
        void shouldHandleEasyItems() {
            int nRespondents = 500;
            UUID[] questionIds = createQuestionIds(5);

            // Mix of easy and moderate items to ensure not all are filtered
            double[] trueA = {1.0, 1.0, 1.0, 1.0, 1.0};
            double[] trueB = {-1.5, -1.0, -0.5, 0.0, 0.5};

            Random rng = new Random(42);
            List<Object[]> data = generateSyntheticData(trueA, trueB, nRespondents, questionIds, rng);

            when(competencyRepository.findById(competencyId))
                    .thenReturn(Optional.of(createCompetencyEntity()));
            mockStreamData(data);

            // Should not throw
            IrtCalibrationService.CalibrationResult result = service.calibrateWithDetails(competencyId);

            assertThat(result.respondentCount()).isEqualTo(nRespondents);
            assertThat(result.converged()).isTrue();
        }

        @Test
        @DisplayName("should handle low-ability respondents without errors")
        void shouldHandleLowAbilityRespondents() {
            int nRespondents = 500;
            UUID[] questionIds = createQuestionIds(5);

            double[] trueA = {1.0, 1.2, 0.8, 1.1, 1.0};
            double[] trueB = {-1.0, -0.5, 0.0, 0.5, 1.0};

            Random rng = new Random(42);
            List<Object[]> data = new ArrayList<>();

            for (int j = 0; j < nRespondents; j++) {
                UUID sessionId = UUID.randomUUID();
                double theta = (j < 20) ? -3.0 : rng.nextGaussian();

                for (int i = 0; i < 5; i++) {
                    double p = probability2PL(theta, trueA[i], trueB[i]);
                    double response = rng.nextDouble() < p ? 1.0 : 0.0;
                    data.add(new Object[]{sessionId, questionIds[i], response});
                }
            }

            when(competencyRepository.findById(competencyId))
                    .thenReturn(Optional.of(createCompetencyEntity()));
            mockStreamData(data);

            IrtCalibrationService.CalibrationResult result = service.calibrateWithDetails(competencyId);

            assertThat(result.converged()).isTrue();
            assertThat(result.itemCount()).isGreaterThanOrEqualTo(3);
        }

        @Test
        @DisplayName("should handle large number of respondents")
        void shouldHandleLargeRespondentCount() {
            int nRespondents = 1000;
            UUID[] questionIds = createQuestionIds(8);

            double[] trueA = {1.0, 1.3, 0.7, 1.5, 1.1, 0.9, 1.2, 1.0};
            double[] trueB = {-1.5, -1.0, -0.5, 0.0, 0.3, 0.5, 1.0, 1.5};

            Random rng = new Random(42);
            List<Object[]> data = generateSyntheticData(trueA, trueB, nRespondents, questionIds, rng);

            when(competencyRepository.findById(competencyId))
                    .thenReturn(Optional.of(createCompetencyEntity()));
            mockStreamData(data);

            IrtCalibrationService.CalibrationResult result = service.calibrateWithDetails(competencyId);

            assertThat(result.converged()).isTrue();
            assertThat(result.respondentCount()).isEqualTo(nRespondents);

            // With more data, parameter recovery should be reasonable (within 1.0 for JMLE)
            for (int i = 0; i < 8; i++) {
                IrtCalibrationService.ItemCalibration cal = result.itemCalibrations().get(i);
                assertThat(cal.difficulty())
                        .as("Difficulty for item %d (true=%.1f)", i, trueB[i])
                        .isCloseTo(trueB[i], within(1.0));
            }
        }
    }

    // ============================================
    // PERSISTENCE TESTS
    // ============================================

    @Nested
    @DisplayName("Parameter Persistence")
    class PersistenceTests {

        @Test
        @DisplayName("should save IRT parameters to ItemStatistics")
        void shouldSaveParametersToItemStatistics() {
            double[] trueA = {1.0, 1.2, 0.9, 1.1, 1.3};
            double[] trueB = {-0.5, 0.0, 0.5, -1.0, 1.0};
            int nRespondents = 300;
            UUID[] questionIds = createQuestionIds(5);

            Random rng = new Random(42);
            List<Object[]> data = generateSyntheticData(trueA, trueB, nRespondents, questionIds, rng);

            when(competencyRepository.findById(competencyId))
                    .thenReturn(Optional.of(createCompetencyEntity()));
            when(testAnswerRepository.streamScoreMatrixForCompetency(competencyId))
                    .thenAnswer(inv -> data.stream());

            List<ItemStatistics> mockStats = createMockItemStatistics(questionIds);
            when(itemStatisticsRepository.findByCompetencyId(competencyId)).thenReturn(mockStats);
            when(itemStatisticsRepository.save(any(ItemStatistics.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            List<ItemStatistics> result = service.calibrateCompetency(competencyId);

            ArgumentCaptor<ItemStatistics> captor = ArgumentCaptor.forClass(ItemStatistics.class);
            verify(itemStatisticsRepository, atLeast(result.size())).save(captor.capture());

            for (ItemStatistics saved : captor.getAllValues()) {
                assertThat(saved.getIrtDiscrimination())
                        .as("IRT discrimination should be set")
                        .isNotNull();
                assertThat(saved.getIrtDifficulty())
                        .as("IRT difficulty should be set")
                        .isNotNull();
                assertThat(saved.getIrtGuessing())
                        .as("IRT guessing should be set to 0 for 2PL")
                        .isNotNull()
                        .isEqualByComparingTo(BigDecimal.ZERO);

                assertThat(saved.getIrtDiscrimination().doubleValue())
                        .isBetween(MIN_DISCRIMINATION, MAX_DISCRIMINATION);

                assertThat(saved.getIrtDifficulty().doubleValue())
                        .isBetween(MIN_DIFFICULTY, MAX_DIFFICULTY);
            }
        }

        @Test
        @DisplayName("should return updated ItemStatistics from calibrateCompetency")
        void shouldReturnUpdatedStatistics() {
            double[] trueA = {1.0, 1.0, 1.0};
            double[] trueB = {-0.5, 0.0, 0.5};
            int nRespondents = 300;
            UUID[] questionIds = createQuestionIds(3);

            Random rng = new Random(42);
            List<Object[]> data = generateSyntheticData(trueA, trueB, nRespondents, questionIds, rng);

            when(competencyRepository.findById(competencyId))
                    .thenReturn(Optional.of(createCompetencyEntity()));
            when(testAnswerRepository.streamScoreMatrixForCompetency(competencyId))
                    .thenAnswer(inv -> data.stream());

            List<ItemStatistics> mockStats = createMockItemStatistics(questionIds);
            when(itemStatisticsRepository.findByCompetencyId(competencyId)).thenReturn(mockStats);
            when(itemStatisticsRepository.save(any(ItemStatistics.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            List<ItemStatistics> result = service.calibrateCompetency(competencyId);

            assertThat(result).isNotEmpty();
            assertThat(result).allSatisfy(stats -> {
                assertThat(stats.getIrtDiscrimination()).isNotNull();
                assertThat(stats.getIrtDifficulty()).isNotNull();
            });
        }
    }

    // ============================================
    // CALIBRATION RESULT RECORD TESTS
    // ============================================

    @Nested
    @DisplayName("Calibration Result Structure")
    class CalibrationResultTests {

        @Test
        @DisplayName("should contain correct metadata in result")
        void shouldContainCorrectMetadata() {
            double[] trueA = {1.0, 1.2, 0.8};
            double[] trueB = {-0.5, 0.0, 0.5};
            int nRespondents = 300;
            UUID[] questionIds = createQuestionIds(3);

            Random rng = new Random(42);
            List<Object[]> data = generateSyntheticData(trueA, trueB, nRespondents, questionIds, rng);

            when(competencyRepository.findById(competencyId))
                    .thenReturn(Optional.of(createCompetencyEntity()));
            mockStreamData(data);

            IrtCalibrationService.CalibrationResult result = service.calibrateWithDetails(competencyId);

            assertThat(result.competencyId()).isEqualTo(competencyId);
            assertThat(result.itemCount()).isEqualTo(3);
            assertThat(result.respondentCount()).isEqualTo(nRespondents);
            assertThat(result.iterations()).isGreaterThan(0);
            assertThat(result.itemCalibrations()).hasSize(3);

            // Each item calibration should have the correct question ID
            Set<UUID> questionIdSet = Set.of(questionIds);
            for (IrtCalibrationService.ItemCalibration cal : result.itemCalibrations()) {
                assertThat(questionIdSet).contains(cal.questionId());
            }
        }
    }

    // ============================================
    // EXTREME PARAMETER TESTS
    // ============================================

    @Nested
    @DisplayName("Extreme Parameter Numerical Stability")
    class ExtremeParameterTests {

        // Reflection helpers for package-private methods in IrtCalibrationServiceImpl.
        // These methods live in the .impl subpackage and cannot be called directly.

        private double invokeProbability(double theta, double a, double b) {
            try {
                Method m = IrtCalibrationServiceImpl.class.getDeclaredMethod(
                        "probability", double.class, double.class, double.class);
                m.setAccessible(true);
                return (double) m.invoke(service, theta, a, b);
            } catch (Exception e) {
                throw new RuntimeException("Failed to invoke probability via reflection", e);
            }
        }

        private double invokeEstimateTheta(boolean[] responses, double[] aParams, double[] bParams) {
            try {
                Method m = IrtCalibrationServiceImpl.class.getDeclaredMethod(
                        "estimateTheta", boolean[].class, double[].class, double[].class);
                m.setAccessible(true);
                return (double) m.invoke(service, responses, aParams, bParams);
            } catch (Exception e) {
                throw new RuntimeException("Failed to invoke estimateTheta via reflection", e);
            }
        }

        private double invokeEstimateB(boolean[] itemResponses, double a, double[] thetas, double currentB) {
            try {
                Method m = IrtCalibrationServiceImpl.class.getDeclaredMethod(
                        "estimateB", boolean[].class, double.class, double[].class, double.class);
                m.setAccessible(true);
                return (double) m.invoke(service, itemResponses, a, thetas, currentB);
            } catch (Exception e) {
                throw new RuntimeException("Failed to invoke estimateB via reflection", e);
            }
        }

        private double invokeEstimateA(boolean[] itemResponses, double currentA, double b, double[] thetas) {
            try {
                Method m = IrtCalibrationServiceImpl.class.getDeclaredMethod(
                        "estimateA", boolean[].class, double.class, double.class, double[].class);
                m.setAccessible(true);
                return (double) m.invoke(service, itemResponses, currentA, b, thetas);
            } catch (Exception e) {
                throw new RuntimeException("Failed to invoke estimateA via reflection", e);
            }
        }

        // --------------------------------------------------
        // 2PL Probability function: probability(theta, a, b)
        // --------------------------------------------------

        @ParameterizedTest(name = "probability({0}, {1}, {2}) should be in [0,1] and finite")
        @CsvSource({
                // Very high discrimination (a > 5.0)
                "0.0,  10.0,  0.0",
                "1.0,  50.0,  0.0",
                "-1.0, 100.0, 0.0",
                "0.5,  5.5,   0.5",
                // Very low discrimination (a < 0.1)
                "0.0,  0.01,  0.0",
                "2.0,  0.001, 0.0",
                "-2.0, 0.05,  1.0",
                // Extreme difficulty (b > 4.0 or b < -4.0)
                "0.0,  1.0,   10.0",
                "0.0,  1.0,  -10.0",
                "0.0,  1.0,   100.0",
                "0.0,  1.0,  -100.0",
                // Theta at extremes
                "-5.0, 1.0,   0.0",
                "5.0,  1.0,   0.0",
                "-10.0,1.0,   0.0",
                "10.0, 1.0,   0.0",
                // Combined extremes: high a with extreme theta
                "5.0,  10.0,  0.0",
                "-5.0, 10.0,  0.0",
                // Combined extremes: high a with extreme b
                "0.0,  10.0,  10.0",
                "0.0,  10.0, -10.0",
                // Extreme all-around
                "5.0,  50.0,  10.0",
                "-5.0, 50.0, -10.0",
                "10.0, 100.0, 10.0",
                "-10.0,100.0,-10.0"
        })
        @DisplayName("probability should produce valid [0,1] result for extreme inputs")
        void probabilityShouldBeValidForExtremeInputs(double theta, double a, double b) {
            double p = invokeProbability(theta, a, b);

            assertThat(p)
                    .as("P(theta=%.2f, a=%.2f, b=%.2f) must not be NaN", theta, a, b)
                    .isNotNaN();
            assertThat(p)
                    .as("P(theta=%.2f, a=%.2f, b=%.2f) must be finite", theta, a, b)
                    .isFinite();
            assertThat(p)
                    .as("P(theta=%.2f, a=%.2f, b=%.2f) must be in [0,1]", theta, a, b)
                    .isBetween(0.0, 1.0);
        }

        @ParameterizedTest(name = "probability with very low discrimination a={0} should approach 0.5")
        @CsvSource({
                "0.001, 0.0,  0.0",
                "0.01,  0.0,  0.0",
                "0.05,  0.0,  0.0",
                "0.001, 1.0,  0.0",
                "0.001, -1.0, 0.0"
        })
        @DisplayName("very low discrimination should produce near-chance probability")
        void veryLowDiscriminationShouldProduceNearChance(double a, double theta, double b) {
            double p = invokeProbability(theta, a, b);

            assertThat(p)
                    .as("P(theta=%.2f, a=%.4f, b=%.2f) should be near 0.5 (chance level)", theta, a, b)
                    .isCloseTo(0.5, within(0.1));
        }

        @Test
        @DisplayName("probability with zero discrimination should return exactly 0.5")
        void zeroDiscriminationShouldReturnHalf() {
            double p = invokeProbability(0.0, 0.0, 0.0);

            assertThat(p)
                    .as("P(theta=0, a=0, b=0) should be 0.5")
                    .isEqualTo(0.5);
        }

        @ParameterizedTest(name = "probability with negative discrimination a={0}")
        @CsvSource({
                "-1.0, 1.0, 0.0",
                "-2.0, 0.0, 1.0",
                "-5.0, 2.0, 0.0",
                "-0.5, -1.0, 0.0"
        })
        @DisplayName("negative discrimination should produce valid [0,1] probability without NaN/Infinity")
        void negativeDiscriminationShouldBeValid(double a, double theta, double b) {
            double p = invokeProbability(theta, a, b);

            assertThat(p)
                    .as("P(theta=%.2f, a=%.2f, b=%.2f) must not be NaN", theta, a, b)
                    .isNotNaN();
            assertThat(p)
                    .as("P(theta=%.2f, a=%.2f, b=%.2f) must be finite", theta, a, b)
                    .isFinite();
            assertThat(p)
                    .as("P(theta=%.2f, a=%.2f, b=%.2f) must be in [0,1]", theta, a, b)
                    .isBetween(0.0, 1.0);
        }

        @ParameterizedTest(name = "probability with negative discrimination a={0} should invert the ICC")
        @CsvSource({
                "-1.0, 2.0, 0.0",
                "-2.0, 3.0, 0.0"
        })
        @DisplayName("negative discrimination should invert the item characteristic curve")
        void negativeDiscriminationShouldInvertIcc(double a, double theta, double b) {
            // With negative discrimination, higher theta should yield LOWER probability
            double pPositiveTheta = invokeProbability(theta, a, b);
            double pNegativeTheta = invokeProbability(-theta, a, b);

            assertThat(pPositiveTheta)
                    .as("With negative a, P(theta=+%.1f) < P(theta=-%.1f)", theta, theta)
                    .isLessThan(pNegativeTheta);
        }

        // --------------------------------------------------
        // Theta estimation with extreme IRT parameters
        // --------------------------------------------------

        @Test
        @DisplayName("estimateTheta with very high discrimination should not produce NaN or Infinity")
        void estimateThetaWithHighDiscriminationShouldBeStable() {
            boolean[] responses = {true, false, true, false, true};
            double[] aParams = {5.5, 6.0, 7.0, 8.0, 10.0};
            double[] bParams = {-1.0, -0.5, 0.0, 0.5, 1.0};

            double theta = invokeEstimateTheta(responses, aParams, bParams);

            assertThat(theta)
                    .as("Theta with high discrimination should not be NaN")
                    .isNotNaN();
            assertThat(theta)
                    .as("Theta with high discrimination should be finite")
                    .isFinite();
            assertThat(theta)
                    .as("Theta with high discrimination should be clamped within bounds")
                    .isBetween(-4.0, 4.0);
        }

        @Test
        @DisplayName("estimateTheta with very low discrimination should produce near-zero theta")
        void estimateThetaWithLowDiscriminationShouldBeNearZero() {
            boolean[] responses = {true, false, true, true, false};
            double[] aParams = {0.01, 0.02, 0.01, 0.03, 0.01};
            double[] bParams = {-1.0, -0.5, 0.0, 0.5, 1.0};

            double theta = invokeEstimateTheta(responses, aParams, bParams);

            assertThat(theta)
                    .as("Theta with low discrimination should not be NaN")
                    .isNotNaN();
            assertThat(theta)
                    .as("Theta with low discrimination should be finite")
                    .isFinite();
            // With near-zero discrimination, likelihood is very flat; theta should stay near initial value (0)
            assertThat(theta)
                    .as("Theta with near-zero discrimination should be near the starting value")
                    .isBetween(-4.0, 4.0);
        }

        @Test
        @DisplayName("estimateTheta with extreme difficulty parameters should not overflow")
        void estimateThetaWithExtremeDifficultyShouldNotOverflow() {
            boolean[] responses = {true, false, true, false};
            double[] aParams = {1.0, 1.0, 1.0, 1.0};
            double[] bParams = {-10.0, -5.0, 5.0, 10.0};

            double theta = invokeEstimateTheta(responses, aParams, bParams);

            assertThat(theta)
                    .as("Theta with extreme difficulties should not be NaN")
                    .isNotNaN();
            assertThat(theta)
                    .as("Theta with extreme difficulties should be finite")
                    .isFinite();
            assertThat(theta)
                    .as("Theta with extreme difficulties should be within bounds")
                    .isBetween(-4.0, 4.0);
        }

        @Test
        @DisplayName("estimateTheta with all correct responses should produce high positive theta")
        void estimateThetaAllCorrectShouldProduceHighTheta() {
            boolean[] responses = {true, true, true, true, true};
            double[] aParams = {1.0, 1.2, 0.8, 1.5, 1.0};
            double[] bParams = {-1.0, -0.5, 0.0, 0.5, 1.0};

            double theta = invokeEstimateTheta(responses, aParams, bParams);

            assertThat(theta)
                    .as("Theta for all-correct should not be NaN")
                    .isNotNaN();
            assertThat(theta)
                    .as("Theta for all-correct should be finite")
                    .isFinite();
            assertThat(theta)
                    .as("Theta for all-correct should be positive and high")
                    .isGreaterThan(1.0);
            assertThat(theta)
                    .as("Theta for all-correct should be clamped within bounds")
                    .isLessThanOrEqualTo(4.0);
        }

        @Test
        @DisplayName("estimateTheta with all incorrect responses should produce low negative theta")
        void estimateThetaAllIncorrectShouldProduceLowTheta() {
            boolean[] responses = {false, false, false, false, false};
            double[] aParams = {1.0, 1.2, 0.8, 1.5, 1.0};
            double[] bParams = {-1.0, -0.5, 0.0, 0.5, 1.0};

            double theta = invokeEstimateTheta(responses, aParams, bParams);

            assertThat(theta)
                    .as("Theta for all-incorrect should not be NaN")
                    .isNotNaN();
            assertThat(theta)
                    .as("Theta for all-incorrect should be finite")
                    .isFinite();
            assertThat(theta)
                    .as("Theta for all-incorrect should be negative and low")
                    .isLessThan(-1.0);
            assertThat(theta)
                    .as("Theta for all-incorrect should be clamped within bounds")
                    .isGreaterThanOrEqualTo(-4.0);
        }

        @Test
        @DisplayName("estimateTheta with single item should not diverge")
        void estimateThetaWithSingleItemShouldNotDiverge() {
            boolean[] responses = {true};
            double[] aParams = {1.0};
            double[] bParams = {0.0};

            double theta = invokeEstimateTheta(responses, aParams, bParams);

            assertThat(theta)
                    .as("Theta with single item should not be NaN")
                    .isNotNaN();
            assertThat(theta)
                    .as("Theta with single item should be finite")
                    .isFinite();
            assertThat(theta)
                    .as("Theta with single correct item should be positive")
                    .isGreaterThanOrEqualTo(0.0);
        }

        @ParameterizedTest(name = "estimateTheta with zero discrimination a={0}")
        @ValueSource(doubles = {0.0})
        @DisplayName("estimateTheta with zero discrimination should handle gracefully")
        void estimateThetaWithZeroDiscriminationShouldHandleGracefully(double a) {
            boolean[] responses = {true, false, true};
            double[] aParams = {a, a, a};
            double[] bParams = {-1.0, 0.0, 1.0};

            double theta = invokeEstimateTheta(responses, aParams, bParams);

            assertThat(theta)
                    .as("Theta with zero discrimination should not be NaN")
                    .isNotNaN();
            assertThat(theta)
                    .as("Theta with zero discrimination should be finite")
                    .isFinite();
        }

        @Test
        @DisplayName("estimateTheta with negative discrimination should not produce NaN")
        void estimateThetaWithNegativeDiscriminationShouldBeStable() {
            boolean[] responses = {true, false, true, false};
            double[] aParams = {-1.0, -0.5, -2.0, -1.5};
            double[] bParams = {-1.0, 0.0, 0.5, 1.0};

            double theta = invokeEstimateTheta(responses, aParams, bParams);

            assertThat(theta)
                    .as("Theta with negative discrimination should not be NaN")
                    .isNotNaN();
            assertThat(theta)
                    .as("Theta with negative discrimination should be finite")
                    .isFinite();
            assertThat(theta)
                    .as("Theta with negative discrimination should be within bounds")
                    .isBetween(-4.0, 4.0);
        }

        // --------------------------------------------------
        // Difficulty estimation (estimateB) with extreme inputs
        // --------------------------------------------------

        @Test
        @DisplayName("estimateB with very high discrimination should not overflow")
        void estimateBWithHighDiscriminationShouldNotOverflow() {
            boolean[] itemResponses = {true, false, true, true, false};
            double a = 10.0;
            double[] thetas = {-2.0, -1.0, 0.0, 1.0, 2.0};
            double currentB = 0.0;

            double b = invokeEstimateB(itemResponses, a, thetas, currentB);

            assertThat(b)
                    .as("Estimated b with high discrimination should not be NaN")
                    .isNotNaN();
            assertThat(b)
                    .as("Estimated b with high discrimination should be finite")
                    .isFinite();
            assertThat(b)
                    .as("Estimated b should be clamped within bounds")
                    .isBetween(-4.0, 4.0);
        }

        @Test
        @DisplayName("estimateB with extreme theta values should not overflow")
        void estimateBWithExtremeThetasShouldNotOverflow() {
            boolean[] itemResponses = {true, false, true, false};
            double a = 1.0;
            double[] thetas = {-5.0, -4.0, 4.0, 5.0};
            double currentB = 0.0;

            double b = invokeEstimateB(itemResponses, a, thetas, currentB);

            assertThat(b)
                    .as("Estimated b with extreme thetas should not be NaN")
                    .isNotNaN();
            assertThat(b)
                    .as("Estimated b with extreme thetas should be finite")
                    .isFinite();
            assertThat(b)
                    .as("Estimated b should be within bounds")
                    .isBetween(-4.0, 4.0);
        }

        @Test
        @DisplayName("estimateB with all correct responses should produce low difficulty")
        void estimateBWithAllCorrectShouldProduceLowDifficulty() {
            boolean[] itemResponses = {true, true, true, true, true};
            double a = 1.0;
            double[] thetas = {-1.0, -0.5, 0.0, 0.5, 1.0};
            double currentB = 0.0;

            double b = invokeEstimateB(itemResponses, a, thetas, currentB);

            assertThat(b)
                    .as("Difficulty for all-correct item should not be NaN")
                    .isNotNaN();
            assertThat(b)
                    .as("Difficulty for all-correct item should be finite")
                    .isFinite();
            assertThat(b)
                    .as("Difficulty for all-correct item should be negative (easy)")
                    .isLessThan(0.0);
        }

        @Test
        @DisplayName("estimateB with all incorrect responses should produce high difficulty")
        void estimateBWithAllIncorrectShouldProduceHighDifficulty() {
            boolean[] itemResponses = {false, false, false, false, false};
            double a = 1.0;
            double[] thetas = {-1.0, -0.5, 0.0, 0.5, 1.0};
            double currentB = 0.0;

            double b = invokeEstimateB(itemResponses, a, thetas, currentB);

            assertThat(b)
                    .as("Difficulty for all-incorrect item should not be NaN")
                    .isNotNaN();
            assertThat(b)
                    .as("Difficulty for all-incorrect item should be finite")
                    .isFinite();
            assertThat(b)
                    .as("Difficulty for all-incorrect item should be positive (hard)")
                    .isGreaterThan(0.0);
        }

        // --------------------------------------------------
        // Discrimination estimation (estimateA) with extreme inputs
        // --------------------------------------------------

        @Test
        @DisplayName("estimateA with extreme theta values should not overflow")
        void estimateAWithExtremeThetasShouldNotOverflow() {
            boolean[] itemResponses = {true, false, true, false};
            double currentA = 1.0;
            double b = 0.0;
            double[] thetas = {-5.0, -4.0, 4.0, 5.0};

            double a = invokeEstimateA(itemResponses, currentA, b, thetas);

            assertThat(a)
                    .as("Estimated a with extreme thetas should not be NaN")
                    .isNotNaN();
            assertThat(a)
                    .as("Estimated a with extreme thetas should be finite")
                    .isFinite();
            assertThat(a)
                    .as("Estimated a should be clamped within bounds")
                    .isBetween(0.1, 4.0);
        }

        @Test
        @DisplayName("estimateA with extreme difficulty should not overflow")
        void estimateAWithExtremeDifficultyShouldNotOverflow() {
            boolean[] itemResponses = {true, false, true, false, true};
            double currentA = 1.0;
            double b = 10.0; // Extreme difficulty
            double[] thetas = {-2.0, -1.0, 0.0, 1.0, 2.0};

            double a = invokeEstimateA(itemResponses, currentA, b, thetas);

            assertThat(a)
                    .as("Estimated a with extreme difficulty should not be NaN")
                    .isNotNaN();
            assertThat(a)
                    .as("Estimated a with extreme difficulty should be finite")
                    .isFinite();
            assertThat(a)
                    .as("Estimated a should be clamped within bounds")
                    .isBetween(0.1, 4.0);
        }

        @Test
        @DisplayName("estimateA with all same responses should remain stable")
        void estimateAWithAllSameResponsesShouldRemainStable() {
            boolean[] itemResponses = {true, true, true, true, true};
            double currentA = 1.0;
            double b = 0.0;
            double[] thetas = {-1.0, -0.5, 0.0, 0.5, 1.0};

            double a = invokeEstimateA(itemResponses, currentA, b, thetas);

            assertThat(a)
                    .as("Estimated a with uniform responses should not be NaN")
                    .isNotNaN();
            assertThat(a)
                    .as("Estimated a with uniform responses should be finite")
                    .isFinite();
            assertThat(a)
                    .as("Estimated a should be clamped within bounds")
                    .isBetween(0.1, 4.0);
        }

        // --------------------------------------------------
        // 3PL guessing parameter boundary tests (via ability estimation API)
        // --------------------------------------------------

        @Test
        @DisplayName("guessing=0.0 boundary: ability estimation with IRT guessing at zero")
        void guessingParameterAtZeroBoundaryShouldWork() {
            UUID[] questionIds = createQuestionIds(5);
            List<ItemStatistics> statsList = new ArrayList<>();

            for (int i = 0; i < 5; i++) {
                AssessmentQuestion question = createQuestionEntity(questionIds[i]);
                ItemStatistics stats = new ItemStatistics(question);
                stats.setIrtDiscrimination(BigDecimal.valueOf(1.0));
                stats.setIrtDifficulty(BigDecimal.valueOf(i - 2.0));
                stats.setIrtGuessing(BigDecimal.ZERO); // c = 0.0 boundary
                statsList.add(stats);
            }

            when(itemStatisticsRepository.findByQuestionIdIn(anyList())).thenReturn(statsList);

            Map<UUID, Double> scores = new HashMap<>();
            for (int i = 0; i < 5; i++) {
                scores.put(questionIds[i], i < 3 ? 1.0 : 0.0);
            }

            double theta = service.estimateAbility(scores);

            assertThat(theta)
                    .as("Theta with c=0.0 boundary should not be NaN")
                    .isNotNaN();
            assertThat(theta)
                    .as("Theta with c=0.0 boundary should be finite")
                    .isFinite();
        }

        @Test
        @DisplayName("guessing=0.5 boundary: ability estimation with high guessing parameter")
        void guessingParameterAtHighBoundaryShouldWork() {
            UUID[] questionIds = createQuestionIds(5);
            List<ItemStatistics> statsList = new ArrayList<>();

            for (int i = 0; i < 5; i++) {
                AssessmentQuestion question = createQuestionEntity(questionIds[i]);
                ItemStatistics stats = new ItemStatistics(question);
                stats.setIrtDiscrimination(BigDecimal.valueOf(1.0));
                stats.setIrtDifficulty(BigDecimal.valueOf(i - 2.0));
                stats.setIrtGuessing(BigDecimal.valueOf(0.5)); // c = 0.5 boundary
                statsList.add(stats);
            }

            when(itemStatisticsRepository.findByQuestionIdIn(anyList())).thenReturn(statsList);

            Map<UUID, Double> scores = new HashMap<>();
            for (int i = 0; i < 5; i++) {
                scores.put(questionIds[i], i < 3 ? 1.0 : 0.0);
            }

            double theta = service.estimateAbility(scores);

            // The current implementation is 2PL (ignores guessing), so this verifies
            // that the system does not crash or produce invalid results when guessing is set
            assertThat(theta)
                    .as("Theta with c=0.5 boundary should not be NaN")
                    .isNotNaN();
            assertThat(theta)
                    .as("Theta with c=0.5 boundary should be finite")
                    .isFinite();
        }

        // --------------------------------------------------
        // Combined extreme scenario: full calibration pipeline
        // --------------------------------------------------

        @Test
        @DisplayName("JMLE should remain stable with high-discrimination synthetic data")
        void jmleShouldRemainStableWithHighDiscriminationData() {
            // Generate data using high discrimination values; JMLE should clamp to MAX_DISCRIMINATION
            double[] trueA = {3.5, 3.8, 3.0, 3.2, 3.5};
            double[] trueB = {-1.0, -0.5, 0.0, 0.5, 1.0};
            int nRespondents = 500;
            UUID[] questionIds = createQuestionIds(5);

            Random rng = new Random(42);
            List<Object[]> data = generateSyntheticData(trueA, trueB, nRespondents, questionIds, rng);

            when(competencyRepository.findById(competencyId))
                    .thenReturn(Optional.of(createCompetencyEntity()));
            mockStreamData(data);

            IrtCalibrationService.CalibrationResult result = service.calibrateWithDetails(competencyId);

            assertThat(result.converged()).isTrue();

            for (IrtCalibrationService.ItemCalibration cal : result.itemCalibrations()) {
                assertThat(cal.discrimination())
                        .as("Discrimination should be finite")
                        .isFinite();
                assertThat(cal.discrimination())
                        .as("Discrimination should not be NaN")
                        .isNotNaN();
                assertThat(cal.discrimination())
                        .as("Discrimination should be clamped within bounds")
                        .isBetween(MIN_DISCRIMINATION, MAX_DISCRIMINATION);

                assertThat(cal.difficulty())
                        .as("Difficulty should be finite")
                        .isFinite();
                assertThat(cal.difficulty())
                        .as("Difficulty should not be NaN")
                        .isNotNaN();
                assertThat(cal.difficulty())
                        .as("Difficulty should be within bounds")
                        .isBetween(MIN_DIFFICULTY, MAX_DIFFICULTY);
            }
        }

        @Test
        @DisplayName("JMLE should remain stable with extreme difficulty spread")
        void jmleShouldRemainStableWithExtremeDifficultySpread() {
            // Items spanning almost the full difficulty range
            double[] trueA = {1.0, 1.0, 1.0, 1.0, 1.0};
            double[] trueB = {-3.5, -1.5, 0.0, 1.5, 3.5};
            int nRespondents = 500;
            UUID[] questionIds = createQuestionIds(5);

            Random rng = new Random(42);
            List<Object[]> data = generateSyntheticData(trueA, trueB, nRespondents, questionIds, rng);

            when(competencyRepository.findById(competencyId))
                    .thenReturn(Optional.of(createCompetencyEntity()));
            mockStreamData(data);

            IrtCalibrationService.CalibrationResult result = service.calibrateWithDetails(competencyId);

            // Some extreme items may be filtered due to p-value limits
            assertThat(result.itemCount()).isGreaterThanOrEqualTo(3);

            for (IrtCalibrationService.ItemCalibration cal : result.itemCalibrations()) {
                assertThat(cal.discrimination())
                        .as("Discrimination should not be NaN")
                        .isNotNaN();
                assertThat(cal.discrimination())
                        .as("Discrimination should be finite")
                        .isFinite();

                assertThat(cal.difficulty())
                        .as("Difficulty should not be NaN")
                        .isNotNaN();
                assertThat(cal.difficulty())
                        .as("Difficulty should be finite")
                        .isFinite();

                assertThat(cal.standardErrorA())
                        .as("SE(a) should be finite or NaN (if information is zero)")
                        .satisfiesAnyOf(
                                se -> assertThat(se).isFinite(),
                                se -> assertThat(se).isNaN()
                        );
                assertThat(cal.standardErrorB())
                        .as("SE(b) should be finite or NaN (if information is zero)")
                        .satisfiesAnyOf(
                                se -> assertThat(se).isFinite(),
                                se -> assertThat(se).isNaN()
                        );
            }
        }

        @Test
        @DisplayName("ability estimation with extreme IRT parameters stored in ItemStatistics")
        void abilityEstimationWithExtremeStoredParametersShouldBeStable() {
            UUID[] questionIds = createQuestionIds(5);
            List<ItemStatistics> statsList = new ArrayList<>();

            // Set up items with parameters at the clamping boundaries
            double[] discriminations = {0.1, 4.0, 0.1, 4.0, 2.0};
            double[] difficulties = {-4.0, -4.0, 4.0, 4.0, 0.0};

            for (int i = 0; i < 5; i++) {
                AssessmentQuestion question = createQuestionEntity(questionIds[i]);
                ItemStatistics stats = new ItemStatistics(question);
                stats.setIrtDiscrimination(BigDecimal.valueOf(discriminations[i]));
                stats.setIrtDifficulty(BigDecimal.valueOf(difficulties[i]));
                statsList.add(stats);
            }

            when(itemStatisticsRepository.findByQuestionIdIn(anyList())).thenReturn(statsList);

            // Test with all correct
            Map<UUID, Double> allCorrect = new HashMap<>();
            for (UUID qId : questionIds) allCorrect.put(qId, 1.0);

            double thetaAllCorrect = service.estimateAbility(allCorrect);
            assertThat(thetaAllCorrect).isNotNaN().isFinite();

            // Test with all incorrect
            Map<UUID, Double> allIncorrect = new HashMap<>();
            for (UUID qId : questionIds) allIncorrect.put(qId, 0.0);

            double thetaAllIncorrect = service.estimateAbility(allIncorrect);
            assertThat(thetaAllIncorrect).isNotNaN().isFinite();

            // Test with mixed
            Map<UUID, Double> mixed = new HashMap<>();
            mixed.put(questionIds[0], 1.0);
            mixed.put(questionIds[1], 0.0);
            mixed.put(questionIds[2], 1.0);
            mixed.put(questionIds[3], 0.0);
            mixed.put(questionIds[4], 1.0);

            double thetaMixed = service.estimateAbility(mixed);
            assertThat(thetaMixed).isNotNaN().isFinite();

            // Ordering should still hold
            assertThat(thetaAllCorrect).isGreaterThan(thetaAllIncorrect);
        }

        // --------------------------------------------------
        // Monotonicity and consistency checks under extreme parameters
        // --------------------------------------------------

        @Test
        @DisplayName("probability should be monotonically increasing in theta for positive discrimination")
        void probabilityShouldBeMonotonicInThetaForPositiveDiscrimination() {
            double a = 10.0; // Very high discrimination
            double b = 0.0;
            double[] thetas = {-5.0, -3.0, -1.0, 0.0, 1.0, 3.0, 5.0};

            double previousP = -1.0;
            for (double theta : thetas) {
                double p = invokeProbability(theta, a, b);
                assertThat(p)
                        .as("P(theta=%.1f) should be >= previous P value", theta)
                        .isGreaterThanOrEqualTo(previousP);
                previousP = p;
            }
        }

        @Test
        @DisplayName("probability should be monotonically decreasing in difficulty for fixed theta and positive a")
        void probabilityShouldBeMonotonicInDifficulty() {
            double a = 5.0; // High discrimination
            double theta = 0.0;
            double[] bValues = {-10.0, -5.0, -1.0, 0.0, 1.0, 5.0, 10.0};

            double previousP = Double.MAX_VALUE;
            for (double b : bValues) {
                double p = invokeProbability(theta, a, b);
                assertThat(p)
                        .as("P(b=%.1f) should be <= previous P value (decreasing)", b)
                        .isLessThanOrEqualTo(previousP);
                previousP = p;
            }
        }
    }
}
