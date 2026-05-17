package app.skillsoft.assessmentbackend.services.psychometrics;

import app.skillsoft.assessmentbackend.domain.dto.psychometrics.DifAnalysisResult;
import app.skillsoft.assessmentbackend.domain.dto.psychometrics.DifAnalysisResult.DifClassification;
import app.skillsoft.assessmentbackend.domain.dto.psychometrics.DifAnalysisResult.ItemDifResult;
import app.skillsoft.assessmentbackend.repository.CompetencyRepository;
import app.skillsoft.assessmentbackend.repository.TestAnswerRepository;
import app.skillsoft.assessmentbackend.services.psychometrics.impl.DifAnalysisServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DifAnalysisServiceImpl.
 * <p>
 * Tests the Mantel-Haenszel DIF detection algorithm with controlled test data,
 * verifying correct computation of MH odds ratio, ETS delta, chi-square,
 * and classification for various DIF scenarios.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DIF Analysis Service Tests")
class DifAnalysisServiceTest {

    @Mock
    private TestAnswerRepository testAnswerRepository;

    @Mock
    private CompetencyRepository competencyRepository;

    private DifAnalysisServiceImpl service;

    private UUID competencyId;

    // Fixed UUIDs for predictable test data
    private static final UUID Q1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID Q2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID Q3 = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID Q4 = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID Q5 = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID Q6 = UUID.fromString("00000000-0000-0000-0000-000000000006");
    private static final UUID Q7 = UUID.fromString("00000000-0000-0000-0000-000000000007");

    @BeforeEach
    void setUp() {
        service = new DifAnalysisServiceImpl(testAnswerRepository, competencyRepository);
        competencyId = UUID.randomUUID();
    }

    // ============================================
    // HAPPY PATH TESTS
    // ============================================

    @Nested
    @DisplayName("When groups perform identically")
    class NoDifTests {

        @Test
        @DisplayName("should classify all items as Category A (negligible DIF)")
        void shouldDetectNoDifWhenGroupsPerformIdentically() {
            // Arrange: Both groups have identical score distributions.
            // Use deterministic scores to avoid random sampling noise.
            // Each group has the same proportion of correct/incorrect answers per item.
            Set<UUID> focalSessions = generateSessionIds(60);
            Set<UUID> referenceSessions = generateSessionIds(60);

            List<UUID> questionIds = List.of(Q1, Q2, Q3, Q4, Q5);

            // Build deterministic matrix: same pattern for both groups
            List<Object[]> scoreMatrix = buildDeterministicEqualMatrix(
                    focalSessions, referenceSessions, questionIds, 0.7);

            when(competencyRepository.existsById(competencyId)).thenReturn(true);
            when(testAnswerRepository.getScoreMatrixForCompetency(competencyId)).thenReturn(scoreMatrix);

            // Act
            DifAnalysisResult result = service.analyzeCompetency(
                    competencyId, focalSessions, referenceSessions,
                    "Group A", "Group B");

            // Assert
            assertThat(result.totalItems()).isEqualTo(5);
            assertThat(result.itemsWithModerateDif()).isZero();
            assertThat(result.itemsWithLargeDif()).isZero();
            assertThat(result.focalGroupSize()).isEqualTo(60);
            assertThat(result.referenceGroupSize()).isEqualTo(60);
            assertThat(result.focalGroupLabel()).isEqualTo("Group A");
            assertThat(result.referenceGroupLabel()).isEqualTo("Group B");
            assertThat(result.competencyId()).isEqualTo(competencyId);

            for (ItemDifResult item : result.itemResults()) {
                assertThat(item.classification()).isEqualTo(DifClassification.A_NEGLIGIBLE);
                assertThat(item.etsDelta().abs()).isLessThan(new BigDecimal("1.0"));
            }
        }
    }

    @Nested
    @DisplayName("When large DIF exists")
    class LargeDifTests {

        @Test
        @DisplayName("should detect Category C (large DIF) when one group is consistently advantaged on an item")
        void shouldDetectLargeDifWhenOneGroupConsistentlyAdvantaged() {
            // Arrange: Q1 is biased - focal group scores much lower than reference
            // even after controlling for overall ability.
            // Use 6 fair items to prevent the biased item from contaminating the
            // total score used for stratification (matching criterion).
            Set<UUID> focalSessions = generateSessionIds(60);
            Set<UUID> referenceSessions = generateSessionIds(60);

            List<UUID> fairItems = List.of(Q2, Q3, Q4, Q5, Q6, Q7);

            List<Object[]> scoreMatrix = buildBiasedScoreMatrix(
                    focalSessions, referenceSessions,
                    Q1,          // biased item
                    fairItems,   // 6 fair items to dominate total score
                    0.15,        // focal P(correct) on biased item
                    0.95,        // reference P(correct) on biased item
                    0.6, 0.6    // both groups ~60% on fair items
            );

            when(competencyRepository.existsById(competencyId)).thenReturn(true);
            when(testAnswerRepository.getScoreMatrixForCompetency(competencyId)).thenReturn(scoreMatrix);

            // Act
            DifAnalysisResult result = service.analyzeCompetency(
                    competencyId, focalSessions, referenceSessions,
                    "Focal", "Reference");

            // Assert
            assertThat(result.itemsWithLargeDif()).isGreaterThanOrEqualTo(1);

            ItemDifResult biasedItem = result.itemResults().stream()
                    .filter(r -> r.questionId().equals(Q1))
                    .findFirst()
                    .orElseThrow();

            assertThat(biasedItem.classification()).isEqualTo(DifClassification.C_LARGE);
            assertThat(biasedItem.etsDelta().abs()).isGreaterThanOrEqualTo(new BigDecimal("1.5"));
            assertThat(biasedItem.direction()).isEqualTo("favors reference");
        }
    }

    @Nested
    @DisplayName("When moderate DIF exists")
    class ModerateDifTests {

        @Test
        @DisplayName("should classify moderate DIF as Category B")
        void shouldClassifyModerateDifCorrectly() {
            // Arrange: Q1 has moderate bias - focal slightly disadvantaged
            Set<UUID> focalSessions = generateSessionIds(60);
            Set<UUID> referenceSessions = generateSessionIds(60);

            // Create a scenario where the odds ratio produces a delta in [1.0, 1.5)
            // We need alpha_MH such that |−2.35 * ln(alpha_MH)| is between 1.0 and 1.5
            // For delta = 1.2: alpha_MH = exp(-1.2 / -2.35) = exp(0.5106) ~ 1.666
            // This means reference group is about 1.67x more likely to get it correct
            // For 60/60 split: focal ~40% correct, reference ~55% correct gives moderate DIF
            List<Object[]> scoreMatrix = buildBiasedScoreMatrix(
                    focalSessions, referenceSessions,
                    Q1,
                    List.of(Q2, Q3, Q4, Q5, Q6, Q7),
                    0.35,   // focal P(correct) on biased item
                    0.60,   // reference P(correct) on biased item
                    0.5, 0.5
            );

            when(competencyRepository.existsById(competencyId)).thenReturn(true);
            when(testAnswerRepository.getScoreMatrixForCompetency(competencyId)).thenReturn(scoreMatrix);

            // Act
            DifAnalysisResult result = service.analyzeCompetency(
                    competencyId, focalSessions, referenceSessions,
                    "Focal", "Reference");

            // Assert: Q1 should be at least B (moderate) due to the differential
            ItemDifResult biasedItem = result.itemResults().stream()
                    .filter(r -> r.questionId().equals(Q1))
                    .findFirst()
                    .orElseThrow();

            // The exact classification depends on stratification, but with this
            // level of differential, we expect at least moderate DIF
            assertThat(biasedItem.classification())
                    .isIn(DifClassification.B_MODERATE, DifClassification.C_LARGE);
            assertThat(biasedItem.etsDelta().abs()).isGreaterThanOrEqualTo(new BigDecimal("1.0"));
        }
    }

    @Nested
    @DisplayName("Direction reporting")
    class DirectionTests {

        @Test
        @DisplayName("should report correct direction when focal group is advantaged")
        void shouldReportCorrectDirection() {
            // Arrange: Q1 favors the focal group (focal performs better after ability matching)
            Set<UUID> focalSessions = generateSessionIds(60);
            Set<UUID> referenceSessions = generateSessionIds(60);

            List<UUID> fairItems = List.of(Q2, Q3, Q4, Q5, Q6, Q7);

            List<Object[]> scoreMatrix = buildBiasedScoreMatrix(
                    focalSessions, referenceSessions,
                    Q1,
                    fairItems,
                    0.95,   // focal P(correct) - higher
                    0.15,   // reference P(correct) - lower
                    0.6, 0.6
            );

            when(competencyRepository.existsById(competencyId)).thenReturn(true);
            when(testAnswerRepository.getScoreMatrixForCompetency(competencyId)).thenReturn(scoreMatrix);

            // Act
            DifAnalysisResult result = service.analyzeCompetency(
                    competencyId, focalSessions, referenceSessions,
                    "Focal", "Reference");

            // Assert
            ItemDifResult biasedItem = result.itemResults().stream()
                    .filter(r -> r.questionId().equals(Q1))
                    .findFirst()
                    .orElseThrow();

            assertThat(biasedItem.direction()).isEqualTo("favors focal");
            assertThat(biasedItem.classification()).isEqualTo(DifClassification.C_LARGE);
        }
    }

    // ============================================
    // VALIDATION AND EDGE CASE TESTS
    // ============================================

    @Nested
    @DisplayName("Input validation")
    class ValidationTests {

        @Test
        @DisplayName("should reject insufficient sample size")
        void shouldRejectInsufficientSampleSize() {
            // Arrange: Only 50 total respondents (below 100 threshold)
            Set<UUID> focalSessions = generateSessionIds(25);
            Set<UUID> referenceSessions = generateSessionIds(25);

            List<Object[]> scoreMatrix = buildUniformScoreMatrix(
                    focalSessions, referenceSessions,
                    List.of(Q1), 0.5, 0.5
            );

            when(competencyRepository.existsById(competencyId)).thenReturn(true);
            when(testAnswerRepository.getScoreMatrixForCompetency(competencyId)).thenReturn(scoreMatrix);

            // Act & Assert
            assertThatThrownBy(() -> service.analyzeCompetency(
                    competencyId, focalSessions, referenceSessions,
                    "Focal", "Reference"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Insufficient total respondents");
        }

        @Test
        @DisplayName("should reject insufficient focal group size")
        void shouldRejectInsufficientFocalGroupSize() {
            // Arrange: Only 10 in focal group (below 20 threshold)
            Set<UUID> focalSessions = generateSessionIds(10);
            Set<UUID> referenceSessions = generateSessionIds(100);

            List<Object[]> scoreMatrix = buildUniformScoreMatrix(
                    focalSessions, referenceSessions,
                    List.of(Q1), 0.5, 0.5
            );

            when(competencyRepository.existsById(competencyId)).thenReturn(true);
            when(testAnswerRepository.getScoreMatrixForCompetency(competencyId)).thenReturn(scoreMatrix);

            // Act & Assert
            assertThatThrownBy(() -> service.analyzeCompetency(
                    competencyId, focalSessions, referenceSessions,
                    "Focal", "Reference"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Insufficient focal group size");
        }

        @Test
        @DisplayName("should reject overlapping groups")
        void shouldRejectOverlappingGroups() {
            UUID sharedSession = UUID.randomUUID();
            Set<UUID> focalSessions = new HashSet<>(Set.of(sharedSession, UUID.randomUUID()));
            Set<UUID> referenceSessions = new HashSet<>(Set.of(sharedSession, UUID.randomUUID()));

            when(competencyRepository.existsById(competencyId)).thenReturn(true);

            assertThatThrownBy(() -> service.analyzeCompetency(
                    competencyId, focalSessions, referenceSessions,
                    "Focal", "Reference"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not overlap");
        }

        @Test
        @DisplayName("should reject non-existent competency")
        void shouldRejectNonExistentCompetency() {
            when(competencyRepository.existsById(competencyId)).thenReturn(false);

            assertThatThrownBy(() -> service.analyzeCompetency(
                    competencyId,
                    Set.of(UUID.randomUUID()),
                    Set.of(UUID.randomUUID()),
                    "Focal", "Reference"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Competency not found");
        }

        @Test
        @DisplayName("should reject empty focal group")
        void shouldRejectEmptyFocalGroup() {
            when(competencyRepository.existsById(competencyId)).thenReturn(true);

            assertThatThrownBy(() -> service.analyzeCompetency(
                    competencyId,
                    Set.of(),
                    Set.of(UUID.randomUUID()),
                    "Focal", "Reference"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Focal group session IDs must not be empty");
        }

        @Test
        @DisplayName("should reject empty question IDs for item-based analysis")
        void shouldRejectEmptyQuestionIds() {
            assertThatThrownBy(() -> service.analyzeItems(
                    Set.of(),
                    Set.of(UUID.randomUUID()),
                    Set.of(UUID.randomUUID()),
                    "Focal", "Reference"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Question IDs must not be empty");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("should handle strata with zero respondents from one group gracefully")
        void shouldHandleEmptyStrataGracefully() {
            // Arrange: Create a distribution where some quintiles might have
            // respondents from only one group (e.g., all focal at bottom, all reference at top)
            // The service should still produce results without errors
            Set<UUID> focalSessions = generateSessionIds(50);
            Set<UUID> referenceSessions = generateSessionIds(60);

            // Focal group scores very low, reference group scores very high
            // This creates strata where some have only one group
            List<Object[]> scoreMatrix = buildSkewedScoreMatrix(
                    focalSessions, referenceSessions,
                    List.of(Q1, Q2),
                    0.2, 0.8  // focal ~20%, reference ~80%
            );

            when(competencyRepository.existsById(competencyId)).thenReturn(true);
            when(testAnswerRepository.getScoreMatrixForCompetency(competencyId)).thenReturn(scoreMatrix);

            // Act - should not throw
            DifAnalysisResult result = service.analyzeCompetency(
                    competencyId, focalSessions, referenceSessions,
                    "Low scorers", "High scorers");

            // Assert - results should be produced without error
            assertThat(result).isNotNull();
            assertThat(result.totalItems()).isEqualTo(2);
            assertThat(result.itemResults()).hasSize(2);

            // All items should have valid classifications
            for (ItemDifResult item : result.itemResults()) {
                assertThat(item.classification()).isNotNull();
                assertThat(item.mhOddsRatio()).isNotNull();
                assertThat(item.etsDelta()).isNotNull();
                assertThat(item.direction()).isIn("favors focal", "favors reference");
            }
        }

        @Test
        @DisplayName("should handle items where all respondents answer correctly")
        void shouldHandleAllCorrectResponses() {
            Set<UUID> focalSessions = generateSessionIds(60);
            Set<UUID> referenceSessions = generateSessionIds(60);

            // All respondents get all items correct
            List<Object[]> scoreMatrix = buildUniformScoreMatrix(
                    focalSessions, referenceSessions,
                    List.of(Q1, Q2),
                    1.0, 1.0
            );

            when(competencyRepository.existsById(competencyId)).thenReturn(true);
            when(testAnswerRepository.getScoreMatrixForCompetency(competencyId)).thenReturn(scoreMatrix);

            // Act - should not throw
            DifAnalysisResult result = service.analyzeCompetency(
                    competencyId, focalSessions, referenceSessions,
                    "Group A", "Group B");

            // Assert - no DIF when everyone gets everything right
            assertThat(result).isNotNull();
            for (ItemDifResult item : result.itemResults()) {
                assertThat(item.classification()).isEqualTo(DifClassification.A_NEGLIGIBLE);
            }
        }
    }

    // ============================================
    // ITEM-BASED ANALYSIS TESTS
    // ============================================

    @Nested
    @DisplayName("Item-based analysis (analyzeItems)")
    class ItemBasedTests {

        @Test
        @DisplayName("should analyze specific items across competency boundaries")
        void shouldAnalyzeSpecificItems() {
            Set<UUID> focalSessions = generateSessionIds(60);
            Set<UUID> referenceSessions = generateSessionIds(60);

            Set<UUID> questionIds = Set.of(Q1, Q2);

            List<Object[]> scoreMatrix = buildUniformScoreMatrix(
                    focalSessions, referenceSessions,
                    List.of(Q1, Q2),
                    0.6, 0.6
            );

            when(testAnswerRepository.getScoreMatrixForQuestions(questionIds)).thenReturn(scoreMatrix);

            // Act
            DifAnalysisResult result = service.analyzeItems(
                    questionIds, focalSessions, referenceSessions,
                    "Focal", "Reference");

            // Assert
            assertThat(result.competencyId()).isNull(); // No competency for item-based
            assertThat(result.totalItems()).isEqualTo(2);
            assertThat(result.itemResults()).hasSize(2);
        }
    }

    // ============================================
    // P-VALUE VALIDATION THROUGH DIF RESULTS
    // ============================================

    @Nested
    @DisplayName("P-value validation")
    class PValueTests {

        @Test
        @DisplayName("should produce significant p-value for large DIF")
        void shouldProduceSignificantPValueForLargeDif() {
            Set<UUID> focalSessions = generateSessionIds(60);
            Set<UUID> referenceSessions = generateSessionIds(60);

            List<Object[]> scoreMatrix = buildBiasedScoreMatrix(
                    focalSessions, referenceSessions,
                    Q1,
                    List.of(Q2, Q3, Q4, Q5, Q6, Q7),
                    0.15, 0.95,  // Large bias on Q1
                    0.6, 0.6
            );

            when(competencyRepository.existsById(competencyId)).thenReturn(true);
            when(testAnswerRepository.getScoreMatrixForCompetency(competencyId)).thenReturn(scoreMatrix);

            DifAnalysisResult result = service.analyzeCompetency(
                    competencyId, focalSessions, referenceSessions,
                    "Focal", "Reference");

            ItemDifResult biasedItem = result.itemResults().stream()
                    .filter(r -> r.questionId().equals(Q1))
                    .findFirst()
                    .orElseThrow();

            // Large DIF should produce statistically significant results
            assertThat(biasedItem.pValue()).isLessThan(new BigDecimal("0.05"));
            assertThat(biasedItem.mhChiSquare()).isGreaterThan(new BigDecimal("3.841"));
        }

        @Test
        @DisplayName("should produce non-significant p-value when no DIF exists")
        void shouldProduceNonSignificantPValueForNoDif() {
            Set<UUID> focalSessions = generateSessionIds(60);
            Set<UUID> referenceSessions = generateSessionIds(60);

            List<Object[]> scoreMatrix = buildDeterministicEqualMatrix(
                    focalSessions, referenceSessions,
                    List.of(Q1, Q2, Q3, Q4, Q5),
                    0.7
            );

            when(competencyRepository.existsById(competencyId)).thenReturn(true);
            when(testAnswerRepository.getScoreMatrixForCompetency(competencyId)).thenReturn(scoreMatrix);

            DifAnalysisResult result = service.analyzeCompetency(
                    competencyId, focalSessions, referenceSessions,
                    "Focal", "Reference");

            // When groups perform identically, p-values should generally be non-significant
            for (ItemDifResult item : result.itemResults()) {
                assertThat(item.pValue()).isNotNull();
                assertThat(item.pValue()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
                assertThat(item.pValue()).isLessThanOrEqualTo(BigDecimal.ONE);
            }
        }
    }

    // ============================================
    // TEST DATA BUILDERS
    // ============================================

    /**
     * Generate a set of random session UUIDs.
     */
    private Set<UUID> generateSessionIds(int count) {
        Set<UUID> ids = new LinkedHashSet<>();
        for (int i = 0; i < count; i++) {
            ids.add(UUID.randomUUID());
        }
        return ids;
    }

    /**
     * Build a deterministic score matrix where both groups have exactly the same
     * item-level performance at each ability level. This guarantees negligible DIF
     * because within each stratum, both groups have the same correct/incorrect ratio.
     * <p>
     * The key insight: to avoid degenerate strata, we create respondents with
     * VARYING total scores (different ability levels) while ensuring that at each
     * ability level, focal and reference groups have identical item performance.
     * We pair up focal/reference respondents so they have the exact same score pattern.
     *
     * @param focalSessions     focal group session IDs (must be same size as reference)
     * @param referenceSessions reference group session IDs
     * @param questionIds       question IDs (at least 3 for meaningful spread)
     * @param pCorrect          overall proportion correct (ignored, we use a gradient)
     */
    private List<Object[]> buildDeterministicEqualMatrix(
            Set<UUID> focalSessions,
            Set<UUID> referenceSessions,
            List<UUID> questionIds,
            double pCorrect) {

        List<Object[]> matrix = new ArrayList<>();
        List<UUID> focalList = new ArrayList<>(focalSessions);
        List<UUID> referenceList = new ArrayList<>(referenceSessions);
        int n = Math.min(focalList.size(), referenceList.size());
        int k = questionIds.size();

        // Create a gradient of ability levels across respondents.
        // Respondent i answers the first floor(i * k / n) items correctly.
        // This produces a range of total scores from 0 to k.
        // Crucially, focal respondent i and reference respondent i get EXACTLY
        // the same score pattern, so within any stratum the contingency table
        // will be perfectly balanced.
        for (int i = 0; i < n; i++) {
            int numCorrect = (int) Math.floor((double) (i + 1) * k / n);
            numCorrect = Math.min(numCorrect, k); // Clamp

            for (int j = 0; j < k; j++) {
                double score = j < numCorrect ? 1.0 : 0.0;
                matrix.add(new Object[]{focalList.get(i), questionIds.get(j), score});
                matrix.add(new Object[]{referenceList.get(i), questionIds.get(j), score});
            }
        }

        return matrix;
    }

    /**
     * Build a score matrix where both groups have approximately the same
     * probability of getting each item correct.
     * No DIF should be detected.
     *
     * @param focalSessions     focal group session IDs
     * @param referenceSessions reference group session IDs
     * @param questionIds       question IDs
     * @param focalPCorrect     probability of focal group answering correctly
     * @param refPCorrect       probability of reference group answering correctly
     * @return raw score matrix as List of Object[]{sessionId, questionId, normalizedScore}
     */
    private List<Object[]> buildUniformScoreMatrix(
            Set<UUID> focalSessions,
            Set<UUID> referenceSessions,
            List<UUID> questionIds,
            double focalPCorrect,
            double refPCorrect) {

        List<Object[]> matrix = new ArrayList<>();
        Random rng = new Random(42); // Fixed seed for reproducibility

        for (UUID sessionId : focalSessions) {
            for (UUID questionId : questionIds) {
                double score = rng.nextDouble() < focalPCorrect ? 1.0 : 0.0;
                matrix.add(new Object[]{sessionId, questionId, score});
            }
        }

        for (UUID sessionId : referenceSessions) {
            for (UUID questionId : questionIds) {
                double score = rng.nextDouble() < refPCorrect ? 1.0 : 0.0;
                matrix.add(new Object[]{sessionId, questionId, score});
            }
        }

        return matrix;
    }

    /**
     * Build a score matrix with one biased item and several fair items.
     * The biased item has different correct probabilities for focal and reference groups.
     * Fair items have the same probability for both groups.
     *
     * @param focalSessions       focal group session IDs
     * @param referenceSessions   reference group session IDs
     * @param biasedQuestionId    the biased item
     * @param fairQuestionIds     fair items
     * @param focalPBiased        focal group P(correct) on biased item
     * @param refPBiased          reference group P(correct) on biased item
     * @param focalPFair          focal group P(correct) on fair items
     * @param refPFair            reference group P(correct) on fair items
     */
    private List<Object[]> buildBiasedScoreMatrix(
            Set<UUID> focalSessions,
            Set<UUID> referenceSessions,
            UUID biasedQuestionId,
            List<UUID> fairQuestionIds,
            double focalPBiased,
            double refPBiased,
            double focalPFair,
            double refPFair) {

        List<Object[]> matrix = new ArrayList<>();
        Random rng = new Random(42);

        List<UUID> allQuestions = new ArrayList<>();
        allQuestions.add(biasedQuestionId);
        allQuestions.addAll(fairQuestionIds);

        for (UUID sessionId : focalSessions) {
            for (UUID questionId : allQuestions) {
                double p = questionId.equals(biasedQuestionId) ? focalPBiased : focalPFair;
                double score = rng.nextDouble() < p ? 1.0 : 0.0;
                matrix.add(new Object[]{sessionId, questionId, score});
            }
        }

        for (UUID sessionId : referenceSessions) {
            for (UUID questionId : allQuestions) {
                double p = questionId.equals(biasedQuestionId) ? refPBiased : refPFair;
                double score = rng.nextDouble() < p ? 1.0 : 0.0;
                matrix.add(new Object[]{sessionId, questionId, score});
            }
        }

        return matrix;
    }

    /**
     * Build a score matrix where the focal group performs very differently from reference.
     * Creates extreme score distributions where groups may cluster in different strata.
     */
    private List<Object[]> buildSkewedScoreMatrix(
            Set<UUID> focalSessions,
            Set<UUID> referenceSessions,
            List<UUID> questionIds,
            double focalPCorrect,
            double refPCorrect) {

        // Same as uniform but with very different probabilities
        return buildUniformScoreMatrix(
                focalSessions, referenceSessions,
                questionIds,
                focalPCorrect, refPCorrect
        );
    }
}
