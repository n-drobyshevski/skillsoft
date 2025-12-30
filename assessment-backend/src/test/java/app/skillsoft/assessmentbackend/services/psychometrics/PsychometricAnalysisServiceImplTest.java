package app.skillsoft.assessmentbackend.services.psychometrics;

import app.skillsoft.assessmentbackend.domain.dto.psychometrics.PsychometricHealthReport;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.domain.entities.ReliabilityStatus;
import app.skillsoft.assessmentbackend.repository.*;
import app.skillsoft.assessmentbackend.services.psychometrics.impl.PsychometricAnalysisServiceImpl;
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
 * Unit tests for PsychometricAnalysisServiceImpl.
 *
 * Tests cover:
 * - Item difficulty index calculation (p-value)
 * - Item discrimination index calculation (Point-Biserial correlation)
 * - Item statistics calculation
 * - Item validity status updates
 * - Health report generation
 * - Edge cases and error handling
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PsychometricAnalysisService Tests")
class PsychometricAnalysisServiceImplTest {

    @Mock
    private ItemStatisticsRepository itemStatisticsRepository;

    @Mock
    private CompetencyReliabilityRepository competencyReliabilityRepository;

    @Mock
    private BigFiveReliabilityRepository bigFiveReliabilityRepository;

    @Mock
    private TestAnswerRepository testAnswerRepository;

    @Mock
    private AssessmentQuestionRepository assessmentQuestionRepository;

    @Mock
    private CompetencyRepository competencyRepository;

    private PsychometricAnalysisServiceImpl service;

    private UUID questionId;
    private UUID competencyId;
    private UUID indicatorId;
    private AssessmentQuestion mockQuestion;
    private BehavioralIndicator mockIndicator;
    private Competency mockCompetency;

    @BeforeEach
    void setUp() {
        service = new PsychometricAnalysisServiceImpl(
            itemStatisticsRepository,
            competencyReliabilityRepository,
            bigFiveReliabilityRepository,
            testAnswerRepository,
            assessmentQuestionRepository,
            competencyRepository
        );

        questionId = UUID.randomUUID();
        competencyId = UUID.randomUUID();
        indicatorId = UUID.randomUUID();

        // Set up mock competency
        mockCompetency = new Competency();
        mockCompetency.setId(competencyId);
        mockCompetency.setName("Communication");

        // Set up mock behavioral indicator
        mockIndicator = new BehavioralIndicator();
        mockIndicator.setId(indicatorId);
        mockIndicator.setCompetency(mockCompetency);

        // Set up mock question
        mockQuestion = new AssessmentQuestion();
        mockQuestion.setId(questionId);
        mockQuestion.setBehavioralIndicator(mockIndicator);
        mockQuestion.setQuestionType(QuestionType.LIKERT);
        mockQuestion.setActive(true);
    }

    @Nested
    @DisplayName("Calculate Difficulty Index Tests")
    class CalculateDifficultyIndexTests {

        @Test
        @DisplayName("should return null when no responses exist")
        void shouldReturnNullWhenNoResponses() {
            // Given - no answers
            when(testAnswerRepository.findAllByQuestionId(questionId)).thenReturn(List.of());

            // When
            BigDecimal result = service.calculateDifficultyIndex(questionId);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should calculate difficulty index correctly for Likert questions")
        void shouldCalculateDifficultyForLikertQuestions() {
            // Given - answers with various normalized scores
            TestAnswer answer1 = createMockAnswer(4.0, 5.0); // 0.8 normalized
            TestAnswer answer2 = createMockAnswer(3.0, 5.0); // 0.6 normalized
            TestAnswer answer3 = createMockAnswer(5.0, 5.0); // 1.0 normalized

            when(testAnswerRepository.findAllByQuestionId(questionId))
                .thenReturn(List.of(answer1, answer2, answer3));

            // When
            BigDecimal result = service.calculateDifficultyIndex(questionId);

            // Then - expected average: (0.8 + 0.6 + 1.0) / 3 = 0.8
            assertThat(result).isNotNull();
            assertThat(result.doubleValue()).isBetween(0.79, 0.81);
        }

        @Test
        @DisplayName("should identify too-easy questions (p > 0.9)")
        void shouldIdentifyTooEasyQuestions() {
            // Given - very high success rate
            TestAnswer answer1 = createMockAnswer(5.0, 5.0); // 1.0 normalized
            TestAnswer answer2 = createMockAnswer(4.5, 5.0); // 0.9 normalized
            TestAnswer answer3 = createMockAnswer(5.0, 5.0); // 1.0 normalized

            when(testAnswerRepository.findAllByQuestionId(questionId))
                .thenReturn(List.of(answer1, answer2, answer3));

            // When
            BigDecimal result = service.calculateDifficultyIndex(questionId);

            // Then
            assertThat(result.doubleValue()).isGreaterThan(0.9);
        }

        @Test
        @DisplayName("should identify too-hard questions (p < 0.2)")
        void shouldIdentifyTooHardQuestions() {
            // Given - very low success rate
            TestAnswer answer1 = createMockAnswer(0.5, 5.0); // 0.1 normalized
            TestAnswer answer2 = createMockAnswer(0.5, 5.0); // 0.1 normalized
            TestAnswer answer3 = createMockAnswer(1.0, 5.0); // 0.2 normalized

            when(testAnswerRepository.findAllByQuestionId(questionId))
                .thenReturn(List.of(answer1, answer2, answer3));

            // When
            BigDecimal result = service.calculateDifficultyIndex(questionId);

            // Then
            assertThat(result.doubleValue()).isLessThan(0.2);
        }

        @Test
        @DisplayName("should handle answers with null scores")
        void shouldHandleAnswersWithNullScores() {
            // Given - some null scores
            TestAnswer answer1 = createMockAnswer(4.0, 5.0);
            TestAnswer answer2 = createMockAnswer(null, 5.0); // null score
            TestAnswer answer3 = createMockAnswer(3.0, 5.0);

            when(testAnswerRepository.findAllByQuestionId(questionId))
                .thenReturn(List.of(answer1, answer2, answer3));

            // When
            BigDecimal result = service.calculateDifficultyIndex(questionId);

            // Then - should only count valid answers
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("Calculate Discrimination Index Tests")
    class CalculateDiscriminationIndexTests {

        @Test
        @DisplayName("should return null when insufficient responses")
        void shouldReturnNullWhenInsufficientResponses() {
            // Given - less than 50 responses (MIN_RESPONSES)
            List<Object[]> scorePairs = new ArrayList<>();
            for (int i = 0; i < 30; i++) {
                scorePairs.add(new Object[]{0.5, 0.6});
            }
            when(testAnswerRepository.findItemTotalScorePairs(questionId)).thenReturn(scorePairs);

            // When
            BigDecimal result = service.calculateDiscriminationIndex(questionId);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should calculate positive discrimination for well-designed questions")
        void shouldCalculatePositiveDiscrimination() {
            // Given - item scores correlate positively with total scores
            List<Object[]> scorePairs = new ArrayList<>();
            for (int i = 0; i < 60; i++) {
                double itemScore = i / 60.0; // 0.0 to 1.0
                double totalScore = i / 60.0 + Math.random() * 0.1; // correlated with noise
                scorePairs.add(new Object[]{itemScore, totalScore});
            }
            when(testAnswerRepository.findItemTotalScorePairs(questionId)).thenReturn(scorePairs);

            // When
            BigDecimal result = service.calculateDiscriminationIndex(questionId);

            // Then - should be positive (good discrimination)
            assertThat(result).isNotNull();
            assertThat(result.doubleValue()).isPositive();
        }
    }

    @Nested
    @DisplayName("Calculate Item Statistics Tests")
    class CalculateItemStatisticsTests {

        @Test
        @DisplayName("should throw exception for non-existent question")
        void shouldThrowExceptionForNonExistentQuestion() {
            // Given
            when(assessmentQuestionRepository.findById(questionId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> service.calculateItemStatistics(questionId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Question not found");
        }

        @Test
        @DisplayName("should create new ItemStatistics if none exists")
        void shouldCreateNewItemStatisticsIfNoneExists() {
            // Given
            when(assessmentQuestionRepository.findById(questionId)).thenReturn(Optional.of(mockQuestion));
            when(itemStatisticsRepository.findByQuestion_Id(questionId)).thenReturn(Optional.empty());
            when(testAnswerRepository.countByQuestion_Id(questionId)).thenReturn(60L);
            when(testAnswerRepository.findAllByQuestionId(questionId))
                .thenReturn(List.of(createMockAnswer(3.5, 5.0)));
            when(testAnswerRepository.findItemTotalScorePairs(questionId)).thenReturn(List.of());
            when(testAnswerRepository.getDistractorDistribution(questionId)).thenReturn(List.of());
            when(itemStatisticsRepository.save(any(ItemStatistics.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            ItemStatistics result = service.calculateItemStatistics(questionId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getQuestion()).isEqualTo(mockQuestion);
            verify(itemStatisticsRepository).save(any(ItemStatistics.class));
        }

        @Test
        @DisplayName("should set PROBATION status when responses below minimum")
        void shouldSetProbationStatusWhenBelowMinimum() {
            // Given - less than 50 responses
            when(assessmentQuestionRepository.findById(questionId)).thenReturn(Optional.of(mockQuestion));
            when(itemStatisticsRepository.findByQuestion_Id(questionId)).thenReturn(Optional.empty());
            when(testAnswerRepository.countByQuestion_Id(questionId)).thenReturn(30L);
            when(itemStatisticsRepository.save(any(ItemStatistics.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            ItemStatistics result = service.calculateItemStatistics(questionId);

            // Then
            assertThat(result.getValidityStatus()).isEqualTo(ItemValidityStatus.PROBATION);
        }
    }

    @Nested
    @DisplayName("Update Item Validity Status Tests")
    class UpdateItemValidityStatusTests {

        @Test
        @DisplayName("should set PROBATION when insufficient responses")
        void shouldSetProbationWhenInsufficientResponses() {
            // Given
            ItemStatistics stats = new ItemStatistics(mockQuestion);
            stats.setResponseCount(30);

            when(itemStatisticsRepository.findByQuestion_Id(questionId)).thenReturn(Optional.of(stats));
            when(itemStatisticsRepository.save(any(ItemStatistics.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            service.updateItemValidityStatus(questionId);

            // Then
            assertThat(stats.getValidityStatus()).isEqualTo(ItemValidityStatus.PROBATION);
        }

        @Test
        @DisplayName("should set RETIRED when discrimination is negative (toxic)")
        void shouldSetRetiredWhenDiscriminationNegative() {
            // Given - toxic discrimination
            ItemStatistics stats = new ItemStatistics(mockQuestion);
            stats.setResponseCount(100);
            stats.setDiscriminationIndex(new BigDecimal("-0.15"));
            stats.setDifficultyIndex(new BigDecimal("0.50"));

            when(itemStatisticsRepository.findByQuestion_Id(questionId)).thenReturn(Optional.of(stats));
            when(itemStatisticsRepository.save(any(ItemStatistics.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            service.updateItemValidityStatus(questionId);

            // Then
            assertThat(stats.getValidityStatus()).isEqualTo(ItemValidityStatus.RETIRED);
        }

        @Test
        @DisplayName("should set ACTIVE when metrics are excellent")
        void shouldSetActiveWhenMetricsExcellent() {
            // Given - excellent metrics
            ItemStatistics stats = new ItemStatistics(mockQuestion);
            stats.setResponseCount(100);
            stats.setDiscriminationIndex(new BigDecimal("0.35"));
            stats.setDifficultyIndex(new BigDecimal("0.65"));

            when(itemStatisticsRepository.findByQuestion_Id(questionId)).thenReturn(Optional.of(stats));
            when(itemStatisticsRepository.save(any(ItemStatistics.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            service.updateItemValidityStatus(questionId);

            // Then
            assertThat(stats.getValidityStatus()).isEqualTo(ItemValidityStatus.ACTIVE);
        }

        @Test
        @DisplayName("should set FLAGGED_FOR_REVIEW when metrics are marginal")
        void shouldSetFlaggedWhenMetricsMarginal() {
            // Given - marginal metrics (low discrimination with extreme difficulty)
            ItemStatistics stats = new ItemStatistics(mockQuestion);
            stats.setResponseCount(100);
            stats.setDiscriminationIndex(new BigDecimal("0.15"));
            stats.setDifficultyIndex(new BigDecimal("0.95")); // Too easy

            when(itemStatisticsRepository.findByQuestion_Id(questionId)).thenReturn(Optional.of(stats));
            when(itemStatisticsRepository.save(any(ItemStatistics.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            service.updateItemValidityStatus(questionId);

            // Then
            assertThat(stats.getValidityStatus()).isEqualTo(ItemValidityStatus.FLAGGED_FOR_REVIEW);
        }
    }

    @Nested
    @DisplayName("Calculate Competency Reliability Tests")
    class CalculateCompetencyReliabilityTests {

        @Test
        @DisplayName("should throw exception for non-existent competency")
        void shouldThrowExceptionForNonExistentCompetency() {
            // Given
            when(competencyRepository.findById(competencyId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> service.calculateCompetencyReliability(competencyId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Competency not found");
        }

        @Test
        @DisplayName("should create new CompetencyReliability if none exists")
        void shouldCreateNewReliabilityIfNoneExists() {
            // Given
            when(competencyRepository.findById(competencyId)).thenReturn(Optional.of(mockCompetency));
            when(competencyReliabilityRepository.findByCompetency_Id(competencyId))
                .thenReturn(Optional.empty());
            // calculateCompetencyReliability uses getScoreMatrixForCompetency, not findSessionsWithAnswersForCompetency
            when(testAnswerRepository.getScoreMatrixForCompetency(competencyId))
                .thenReturn(List.of());
            when(competencyReliabilityRepository.save(any(CompetencyReliability.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            CompetencyReliability result = service.calculateCompetencyReliability(competencyId);

            // Then
            assertThat(result).isNotNull();
            verify(competencyReliabilityRepository).save(any(CompetencyReliability.class));
        }
    }

    @Nested
    @DisplayName("Retire Item Tests")
    class RetireItemTests {

        @Test
        @DisplayName("should retire item and deactivate question")
        void shouldRetireItemAndDeactivateQuestion() {
            // Given
            ItemStatistics stats = new ItemStatistics(mockQuestion);
            stats.setValidityStatus(ItemValidityStatus.FLAGGED_FOR_REVIEW);

            when(itemStatisticsRepository.findByQuestion_Id(questionId)).thenReturn(Optional.of(stats));
            when(assessmentQuestionRepository.findById(questionId)).thenReturn(Optional.of(mockQuestion));
            when(itemStatisticsRepository.save(any(ItemStatistics.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            when(assessmentQuestionRepository.save(any(AssessmentQuestion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            service.retireItem(questionId, "Poor discrimination in user testing");

            // Then
            assertThat(stats.getValidityStatus()).isEqualTo(ItemValidityStatus.RETIRED);
            assertThat(mockQuestion.isActive()).isFalse();
            verify(itemStatisticsRepository).save(stats);
            verify(assessmentQuestionRepository).save(mockQuestion);
        }
    }

    @Nested
    @DisplayName("Activate Item Tests")
    class ActivateItemTests {

        @Test
        @DisplayName("should throw exception when item does not meet criteria")
        void shouldThrowExceptionWhenItemDoesNotMeetCriteria() {
            // Given - insufficient responses
            ItemStatistics stats = new ItemStatistics(mockQuestion);
            stats.setResponseCount(30);
            stats.setValidityStatus(ItemValidityStatus.PROBATION);

            when(itemStatisticsRepository.findByQuestion_Id(questionId)).thenReturn(Optional.of(stats));

            // When/Then
            assertThatThrownBy(() -> service.activateItem(questionId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot activate item with insufficient responses");
        }

        @Test
        @DisplayName("should activate item with good metrics")
        void shouldActivateItemWithGoodMetrics() {
            // Given - good metrics
            ItemStatistics stats = new ItemStatistics(mockQuestion);
            stats.setResponseCount(100);
            stats.setDiscriminationIndex(new BigDecimal("0.30"));
            stats.setDifficultyIndex(new BigDecimal("0.55"));
            stats.setValidityStatus(ItemValidityStatus.FLAGGED_FOR_REVIEW);

            when(itemStatisticsRepository.findByQuestion_Id(questionId)).thenReturn(Optional.of(stats));
            when(assessmentQuestionRepository.findById(questionId)).thenReturn(Optional.of(mockQuestion));
            when(itemStatisticsRepository.save(any(ItemStatistics.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            when(assessmentQuestionRepository.save(any(AssessmentQuestion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            service.activateItem(questionId);

            // Then
            assertThat(stats.getValidityStatus()).isEqualTo(ItemValidityStatus.ACTIVE);
            assertThat(mockQuestion.isActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("Generate Health Report Tests")
    class GenerateHealthReportTests {

        @Test
        @DisplayName("should generate report with item status counts")
        void shouldGenerateReportWithItemStatusCounts() {
            // Given - mock all required repository calls
            when(itemStatisticsRepository.count()).thenReturn(85L);
            when(itemStatisticsRepository.countByValidityStatus(ItemValidityStatus.ACTIVE)).thenReturn(50L);
            when(itemStatisticsRepository.countByValidityStatus(ItemValidityStatus.PROBATION)).thenReturn(20L);
            when(itemStatisticsRepository.countByValidityStatus(ItemValidityStatus.FLAGGED_FOR_REVIEW)).thenReturn(5L);
            when(itemStatisticsRepository.countByValidityStatus(ItemValidityStatus.RETIRED)).thenReturn(10L);

            // Competency reliability mocks
            when(competencyReliabilityRepository.count()).thenReturn(0L);
            when(competencyReliabilityRepository.countByReliabilityStatus(ReliabilityStatus.RELIABLE)).thenReturn(0L);
            when(competencyReliabilityRepository.countByReliabilityStatus(ReliabilityStatus.ACCEPTABLE)).thenReturn(0L);
            when(competencyReliabilityRepository.countByReliabilityStatus(ReliabilityStatus.UNRELIABLE)).thenReturn(0L);
            when(competencyReliabilityRepository.countByReliabilityStatus(ReliabilityStatus.INSUFFICIENT_DATA)).thenReturn(0L);
            when(competencyReliabilityRepository.calculateAverageAlpha()).thenReturn(null);

            // Helper method mocks
            when(itemStatisticsRepository.findAll()).thenReturn(List.of());
            when(itemStatisticsRepository.findItemsRequiringReview()).thenReturn(List.of());
            when(itemStatisticsRepository.findProblematicItems()).thenReturn(List.of());
            when(bigFiveReliabilityRepository.findAll()).thenReturn(List.of());

            // When
            PsychometricHealthReport report = service.generateHealthReport();

            // Then
            assertThat(report).isNotNull();
            assertThat(report.activeItems()).isEqualTo(50);
            assertThat(report.probationItems()).isEqualTo(20);
            assertThat(report.flaggedItems()).isEqualTo(5);
            assertThat(report.retiredItems()).isEqualTo(10);
        }

        @Test
        @DisplayName("should calculate total items correctly")
        void shouldCalculateTotalItemsCorrectly() {
            // Given - totalItems comes from count(), not sum
            when(itemStatisticsRepository.count()).thenReturn(100L);
            when(itemStatisticsRepository.countByValidityStatus(ItemValidityStatus.ACTIVE)).thenReturn(40L);
            when(itemStatisticsRepository.countByValidityStatus(ItemValidityStatus.PROBATION)).thenReturn(30L);
            when(itemStatisticsRepository.countByValidityStatus(ItemValidityStatus.FLAGGED_FOR_REVIEW)).thenReturn(10L);
            when(itemStatisticsRepository.countByValidityStatus(ItemValidityStatus.RETIRED)).thenReturn(20L);

            // Competency reliability mocks
            when(competencyReliabilityRepository.count()).thenReturn(0L);
            when(competencyReliabilityRepository.countByReliabilityStatus(ReliabilityStatus.RELIABLE)).thenReturn(0L);
            when(competencyReliabilityRepository.countByReliabilityStatus(ReliabilityStatus.ACCEPTABLE)).thenReturn(0L);
            when(competencyReliabilityRepository.countByReliabilityStatus(ReliabilityStatus.UNRELIABLE)).thenReturn(0L);
            when(competencyReliabilityRepository.countByReliabilityStatus(ReliabilityStatus.INSUFFICIENT_DATA)).thenReturn(0L);
            when(competencyReliabilityRepository.calculateAverageAlpha()).thenReturn(null);

            // Helper method mocks
            when(itemStatisticsRepository.findAll()).thenReturn(List.of());
            when(itemStatisticsRepository.findItemsRequiringReview()).thenReturn(List.of());
            when(itemStatisticsRepository.findProblematicItems()).thenReturn(List.of());
            when(bigFiveReliabilityRepository.findAll()).thenReturn(List.of());

            // When
            PsychometricHealthReport report = service.generateHealthReport();

            // Then
            assertThat(report.totalItems()).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("Analyze Distractor Tests")
    class AnalyzeDistractorTests {

        @Test
        @DisplayName("should return empty map when no distribution data")
        void shouldReturnEmptyMapWhenNoDistributionData() {
            // Given
            when(testAnswerRepository.getDistractorDistribution(questionId)).thenReturn(List.of());

            // When
            Map<String, Double> result = service.analyzeDistractors(questionId);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should calculate selection percentages correctly")
        void shouldCalculateSelectionPercentagesCorrectly() {
            // Given - 3 options with different selection counts
            List<Object[]> distribution = List.of(
                new Object[]{"option-a", 50L},
                new Object[]{"option-b", 30L},
                new Object[]{"option-c", 20L}
            );
            when(testAnswerRepository.getDistractorDistribution(questionId)).thenReturn(distribution);

            // When
            Map<String, Double> result = service.analyzeDistractors(questionId);

            // Then
            assertThat(result).hasSize(3);
            assertThat(result.get("option-a")).isBetween(0.49, 0.51); // 50%
            assertThat(result.get("option-b")).isBetween(0.29, 0.31); // 30%
            assertThat(result.get("option-c")).isBetween(0.19, 0.21); // 20%
        }
    }

    // Helper method to create mock TestAnswer
    private TestAnswer createMockAnswer(Double score, Double maxScore) {
        TestAnswer answer = new TestAnswer();
        answer.setId(UUID.randomUUID());
        answer.setScore(score);
        answer.setMaxScore(maxScore);
        answer.setIsSkipped(false);
        return answer;
    }
}
