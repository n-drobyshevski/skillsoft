package app.skillsoft.assessmentbackend.services.scoring;

import app.skillsoft.assessmentbackend.domain.dto.CompetencyScoreDto;
import app.skillsoft.assessmentbackend.domain.dto.IndicatorScoreDto;
import app.skillsoft.assessmentbackend.domain.dto.StandardCodesDto;
import app.skillsoft.assessmentbackend.domain.entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CompetencyAggregationService.
 *
 * This is the shared aggregation pipeline used by all three scoring strategies
 * (Overview, JobFit, TeamFit). Tests cover each pipeline stage individually
 * and the full end-to-end aggregate() method.
 *
 * Pipeline stages tested:
 * 1. normalizeAndAggregate - normalize answers, group by indicator
 * 2. rollUpIndicatorsToCompetencies - indicator -> competency rollup
 * 3. buildCompetencyScores - competency aggregation -> DTO conversion
 * 4. applyEvidenceSufficiency - flag under-evidenced competencies
 * 5. aggregate - full pipeline end-to-end
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CompetencyAggregationService Tests")
class CompetencyAggregationServiceTest {

    @Mock
    private CompetencyBatchLoader competencyBatchLoader;

    @Mock
    private IndicatorBatchLoader indicatorBatchLoader;

    @Spy
    private ScoreNormalizer scoreNormalizer = new ScoreNormalizer();

    private CompetencyAggregationService aggregationService;

    // Shared test data
    private UUID competencyId1;
    private UUID competencyId2;
    private Competency competency1;
    private Competency competency2;
    private TestSession mockSession;

    @BeforeEach
    void setUp() {
        aggregationService = new CompetencyAggregationService(
                competencyBatchLoader,
                indicatorBatchLoader,
                scoreNormalizer
        );

        competencyId1 = UUID.randomUUID();
        competencyId2 = UUID.randomUUID();

        competency1 = createCompetency(competencyId1, "Leadership", "2.B.1.a");
        competency2 = createCompetency(competencyId2, "Communication", "2.A.1.b");

        mockSession = new TestSession();
        mockSession.setId(UUID.randomUUID());
    }

    // ===== Factory methods following existing codebase patterns =====

    private Competency createCompetency(UUID id, String name, String onetCode) {
        Competency competency = new Competency();
        competency.setId(id);
        competency.setName(name);
        competency.setCategory(CompetencyCategory.INTERPERSONAL);
        competency.setActive(true);
        competency.setApprovalStatus(ApprovalStatus.APPROVED);

        if (onetCode != null) {
            StandardCodesDto standardCodes = StandardCodesDto.builder()
                    .onetRef(onetCode, name, "skill")
                    .build();
            competency.setStandardCodes(standardCodes);
        }

        return competency;
    }

    private BehavioralIndicator createBehavioralIndicator(UUID id, Competency competency, float weight) {
        BehavioralIndicator indicator = new BehavioralIndicator();
        indicator.setId(id);
        indicator.setCompetency(competency);
        indicator.setTitle("Indicator " + id.toString().substring(0, 8));
        indicator.setWeight(weight);
        indicator.setActive(true);
        indicator.setApprovalStatus(ApprovalStatus.APPROVED);
        return indicator;
    }

    private BehavioralIndicator createBehavioralIndicator(UUID id, Competency competency) {
        return createBehavioralIndicator(id, competency, 1.0f);
    }

    private AssessmentQuestion createQuestion(UUID id, BehavioralIndicator indicator, QuestionType type) {
        AssessmentQuestion question = new AssessmentQuestion();
        question.setId(id);
        question.setBehavioralIndicator(indicator);
        question.setQuestionType(type);
        question.setQuestionText("Test question");
        question.setScoringRubric("Standard rubric");
        question.setDifficultyLevel(DifficultyLevel.INTERMEDIATE);
        question.setActive(true);
        question.setOrderIndex(1);
        return question;
    }

    private TestAnswer createAnswer(AssessmentQuestion question, Integer likertValue,
                                    Double score, boolean isSkipped) {
        TestAnswer answer = new TestAnswer();
        answer.setId(UUID.randomUUID());
        answer.setSession(mockSession);
        answer.setQuestion(question);
        answer.setLikertValue(likertValue);
        answer.setScore(score);
        answer.setIsSkipped(isSkipped);
        answer.setAnsweredAt(isSkipped ? null : LocalDateTime.now());
        return answer;
    }

    /**
     * Sets up IndicatorBatchLoader mocks to dynamically extract indicator IDs from
     * the answer -> question -> indicator chain.
     */
    private void setupIndicatorBatchLoaderMock() {
        lenient().when(indicatorBatchLoader.extractIndicatorIdSafe(any(TestAnswer.class)))
                .thenAnswer(invocation -> {
                    TestAnswer answer = invocation.getArgument(0);
                    if (answer == null || answer.getQuestion() == null
                            || answer.getQuestion().getBehavioralIndicator() == null) {
                        return Optional.empty();
                    }
                    return Optional.of(answer.getQuestion().getBehavioralIndicator().getId());
                });

        lenient().when(indicatorBatchLoader.loadIndicatorsForAnswers(anyList()))
                .thenAnswer(invocation -> {
                    List<TestAnswer> answers = invocation.getArgument(0);
                    Map<UUID, BehavioralIndicator> map = new HashMap<>();
                    for (TestAnswer a : answers) {
                        if (a != null && a.getQuestion() != null
                                && a.getQuestion().getBehavioralIndicator() != null) {
                            BehavioralIndicator ind = a.getQuestion().getBehavioralIndicator();
                            map.put(ind.getId(), ind);
                        }
                    }
                    return map;
                });

        lenient().when(indicatorBatchLoader.getFromCache(any(), any()))
                .thenAnswer(invocation -> {
                    Map<UUID, BehavioralIndicator> cache = invocation.getArgument(0);
                    UUID id = invocation.getArgument(1);
                    return cache != null ? cache.get(id) : null;
                });
    }

    /**
     * Sets up CompetencyBatchLoader mocks with a provided competency map.
     */
    private void setupCompetencyBatchLoaderMock(Map<UUID, Competency> competencyMap) {
        lenient().when(competencyBatchLoader.loadCompetenciesForAnswers(anyList()))
                .thenReturn(competencyMap);

        lenient().when(competencyBatchLoader.getFromCache(any(), any()))
                .thenAnswer(invocation -> {
                    Map<UUID, Competency> cache = invocation.getArgument(0);
                    UUID id = invocation.getArgument(1);
                    return cache != null ? cache.get(id) : null;
                });
    }

    // =========================================================================
    // Stage 1: normalizeAndAggregate
    // =========================================================================

    @Nested
    @DisplayName("normalizeAndAggregate Tests")
    class NormalizeAndAggregateTests {

        @Test
        @DisplayName("Should return empty map for empty answer list")
        void should_returnEmptyMap_when_answerListIsEmpty() {
            // Given
            List<TestAnswer> emptyAnswers = Collections.emptyList();

            // When
            Map<UUID, IndicatorAggregation> result = aggregationService.normalizeAndAggregate(emptyAnswers);

            // Then
            assertThat(result).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Should skip answers that are flagged as skipped")
        void should_skipSkippedAnswers_when_isSkippedIsTrue() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competency1);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer skippedAnswer = createAnswer(question, 4, null, true);

            setupIndicatorBatchLoaderMock();

            // When
            Map<UUID, IndicatorAggregation> result =
                    aggregationService.normalizeAndAggregate(List.of(skippedAnswer));

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should skip answers with null answeredAt timestamp")
        void should_skipUnansweredAnswers_when_answeredAtIsNull() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competency1);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);

            TestAnswer unanswered = new TestAnswer();
            unanswered.setId(UUID.randomUUID());
            unanswered.setSession(mockSession);
            unanswered.setQuestion(question);
            unanswered.setIsSkipped(false);
            unanswered.setAnsweredAt(null);

            setupIndicatorBatchLoaderMock();

            // When
            Map<UUID, IndicatorAggregation> result =
                    aggregationService.normalizeAndAggregate(List.of(unanswered));

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should skip answers when indicator ID cannot be extracted")
        void should_skipAnswer_when_indicatorIdCannotBeExtracted() {
            // Given: answer with null question (broken entity chain)
            TestAnswer brokenAnswer = new TestAnswer();
            brokenAnswer.setId(UUID.randomUUID());
            brokenAnswer.setSession(mockSession);
            brokenAnswer.setQuestion(null);
            brokenAnswer.setIsSkipped(false);
            brokenAnswer.setAnsweredAt(LocalDateTime.now());

            when(indicatorBatchLoader.extractIndicatorIdSafe(any(TestAnswer.class)))
                    .thenReturn(Optional.empty());

            // When
            Map<UUID, IndicatorAggregation> result =
                    aggregationService.normalizeAndAggregate(List.of(brokenAnswer));

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should aggregate single Likert answer correctly")
        void should_aggregateSingleAnswer_when_oneValidLikertAnswer() {
            // Given: Likert value 4 => normalized (4-1)/4 = 0.75
            UUID indicatorId = UUID.randomUUID();
            BehavioralIndicator indicator = createBehavioralIndicator(indicatorId, competency1);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(question, 4, null, false);

            setupIndicatorBatchLoaderMock();

            // When
            Map<UUID, IndicatorAggregation> result =
                    aggregationService.normalizeAndAggregate(List.of(answer));

            // Then
            assertThat(result).hasSize(1);
            assertThat(result).containsKey(indicatorId);

            IndicatorAggregation agg = result.get(indicatorId);
            assertThat(agg.getQuestionCount()).isEqualTo(1);
            assertThat(agg.getTotalScore()).isCloseTo(0.75, within(0.001));
            assertThat(agg.getTotalMaxScore()).isCloseTo(1.0, within(0.001));
        }

        @Test
        @DisplayName("Should aggregate multiple answers for the same indicator")
        void should_accumulateScores_when_multipleAnswersForSameIndicator() {
            // Given: 3 Likert answers => normalized 0.5, 0.75, 1.0
            UUID indicatorId = UUID.randomUUID();
            BehavioralIndicator indicator = createBehavioralIndicator(indicatorId, competency1);

            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            AssessmentQuestion q2 = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            AssessmentQuestion q3 = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);

            TestAnswer a1 = createAnswer(q1, 3, null, false); // 0.5
            TestAnswer a2 = createAnswer(q2, 4, null, false); // 0.75
            TestAnswer a3 = createAnswer(q3, 5, null, false); // 1.0

            setupIndicatorBatchLoaderMock();

            // When
            Map<UUID, IndicatorAggregation> result =
                    aggregationService.normalizeAndAggregate(List.of(a1, a2, a3));

            // Then
            assertThat(result).hasSize(1);
            IndicatorAggregation agg = result.get(indicatorId);
            assertThat(agg.getQuestionCount()).isEqualTo(3);
            assertThat(agg.getTotalScore()).isCloseTo(2.25, within(0.001)); // 0.5 + 0.75 + 1.0
            assertThat(agg.getTotalMaxScore()).isCloseTo(3.0, within(0.001));
        }

        @Test
        @DisplayName("Should separate answers into different indicators")
        void should_separateByIndicator_when_answersMapToDifferentIndicators() {
            // Given: 2 indicators, each with 1 answer
            UUID indicatorId1 = UUID.randomUUID();
            UUID indicatorId2 = UUID.randomUUID();
            BehavioralIndicator ind1 = createBehavioralIndicator(indicatorId1, competency1);
            BehavioralIndicator ind2 = createBehavioralIndicator(indicatorId2, competency2);

            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), ind1, QuestionType.LIKERT);
            AssessmentQuestion q2 = createQuestion(UUID.randomUUID(), ind2, QuestionType.SJT);

            TestAnswer a1 = createAnswer(q1, 5, null, false);    // Likert 5 => 1.0
            TestAnswer a2 = createAnswer(q2, null, 0.6, false);  // SJT score 0.6

            setupIndicatorBatchLoaderMock();

            // When
            Map<UUID, IndicatorAggregation> result =
                    aggregationService.normalizeAndAggregate(List.of(a1, a2));

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(indicatorId1).getTotalScore()).isCloseTo(1.0, within(0.001));
            assertThat(result.get(indicatorId2).getTotalScore()).isCloseTo(0.6, within(0.001));
        }

        @Test
        @DisplayName("Should filter skipped answers from mixed list")
        void should_filterSkippedFromMixedList_when_mixedSkippedAndValid() {
            // Given: 2 valid + 1 skipped for same indicator
            UUID indicatorId = UUID.randomUUID();
            BehavioralIndicator indicator = createBehavioralIndicator(indicatorId, competency1);

            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            AssessmentQuestion q2 = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            AssessmentQuestion q3 = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);

            TestAnswer valid1 = createAnswer(q1, 4, null, false);  // 0.75
            TestAnswer skipped = createAnswer(q2, null, null, true);
            TestAnswer valid2 = createAnswer(q3, 5, null, false);  // 1.0

            setupIndicatorBatchLoaderMock();

            // When
            Map<UUID, IndicatorAggregation> result =
                    aggregationService.normalizeAndAggregate(List.of(valid1, skipped, valid2));

            // Then
            assertThat(result).hasSize(1);
            IndicatorAggregation agg = result.get(indicatorId);
            assertThat(agg.getQuestionCount()).isEqualTo(2);
            assertThat(agg.getTotalScore()).isCloseTo(1.75, within(0.001)); // 0.75 + 1.0
        }

        @Test
        @DisplayName("Should normalize all minimum scores (Likert 1) to zero")
        void should_normalizeToZero_when_allLikertMinimumValues() {
            // Given: All Likert 1 => normalized 0.0
            UUID indicatorId = UUID.randomUUID();
            BehavioralIndicator indicator = createBehavioralIndicator(indicatorId, competency1);

            List<TestAnswer> answers = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                AssessmentQuestion q = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
                answers.add(createAnswer(q, 1, null, false));
            }

            setupIndicatorBatchLoaderMock();

            // When
            Map<UUID, IndicatorAggregation> result =
                    aggregationService.normalizeAndAggregate(answers);

            // Then
            IndicatorAggregation agg = result.get(indicatorId);
            assertThat(agg.getQuestionCount()).isEqualTo(5);
            assertThat(agg.getTotalScore()).isCloseTo(0.0, within(0.001));
            assertThat(agg.getTotalMaxScore()).isCloseTo(5.0, within(0.001));
        }

        @Test
        @DisplayName("Should normalize all maximum scores (Likert 5) to one")
        void should_normalizeToOne_when_allLikertMaximumValues() {
            // Given: All Likert 5 => normalized 1.0
            UUID indicatorId = UUID.randomUUID();
            BehavioralIndicator indicator = createBehavioralIndicator(indicatorId, competency1);

            List<TestAnswer> answers = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                AssessmentQuestion q = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
                answers.add(createAnswer(q, 5, null, false));
            }

            setupIndicatorBatchLoaderMock();

            // When
            Map<UUID, IndicatorAggregation> result =
                    aggregationService.normalizeAndAggregate(answers);

            // Then
            IndicatorAggregation agg = result.get(indicatorId);
            assertThat(agg.getQuestionCount()).isEqualTo(5);
            assertThat(agg.getTotalScore()).isCloseTo(5.0, within(0.001));
            assertThat(agg.getTotalMaxScore()).isCloseTo(5.0, within(0.001));
        }

        @Test
        @DisplayName("Should handle mixed question types in aggregation")
        void should_aggregateCorrectly_when_mixedQuestionTypes() {
            // Given: LIKERT (4 => 0.75), SJT (0.8), MCQ (1.0) for same indicator
            UUID indicatorId = UUID.randomUUID();
            BehavioralIndicator indicator = createBehavioralIndicator(indicatorId, competency1);

            AssessmentQuestion likertQ = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            AssessmentQuestion sjtQ = createQuestion(UUID.randomUUID(), indicator, QuestionType.SJT);
            AssessmentQuestion mcqQ = createQuestion(UUID.randomUUID(), indicator, QuestionType.MCQ);

            TestAnswer likertA = createAnswer(likertQ, 4, null, false);   // 0.75
            TestAnswer sjtA = createAnswer(sjtQ, null, 0.8, false);      // 0.8
            TestAnswer mcqA = createAnswer(mcqQ, null, 1.0, false);      // 1.0

            setupIndicatorBatchLoaderMock();

            // When
            Map<UUID, IndicatorAggregation> result =
                    aggregationService.normalizeAndAggregate(List.of(likertA, sjtA, mcqA));

            // Then
            IndicatorAggregation agg = result.get(indicatorId);
            assertThat(agg.getQuestionCount()).isEqualTo(3);
            assertThat(agg.getTotalScore()).isCloseTo(2.55, within(0.001)); // 0.75 + 0.8 + 1.0
        }
    }

    // =========================================================================
    // Stage 2: rollUpIndicatorsToCompetencies
    // =========================================================================

    @Nested
    @DisplayName("rollUpIndicatorsToCompetencies Tests")
    class RollUpIndicatorsToCompetenciesTests {

        @Test
        @DisplayName("Should return empty map when no indicator aggregations")
        void should_returnEmptyMap_when_noIndicatorAggregations() {
            // Given
            Map<UUID, IndicatorAggregation> emptyAggs = Collections.emptyMap();
            Map<UUID, BehavioralIndicator> emptyCache = Collections.emptyMap();

            // When
            Map<UUID, CompetencyAggregation> result =
                    aggregationService.rollUpIndicatorsToCompetencies(emptyAggs, emptyCache);

            // Then
            assertThat(result).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Should skip indicator when not found in cache")
        void should_skipIndicator_when_notFoundInCache() {
            // Given
            UUID indicatorId = UUID.randomUUID();
            IndicatorAggregation indAgg = new IndicatorAggregation(indicatorId);
            indAgg.addAnswer(0.75);

            Map<UUID, IndicatorAggregation> indicatorAggs = Map.of(indicatorId, indAgg);

            // Indicator not in cache
            when(indicatorBatchLoader.getFromCache(any(), eq(indicatorId))).thenReturn(null);

            // When
            Map<UUID, CompetencyAggregation> result =
                    aggregationService.rollUpIndicatorsToCompetencies(indicatorAggs, Map.of());

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should skip indicator when its competency is null")
        void should_skipIndicator_when_competencyIsNull() {
            // Given: Indicator with null competency (orphan)
            UUID indicatorId = UUID.randomUUID();
            BehavioralIndicator orphanIndicator = new BehavioralIndicator();
            orphanIndicator.setId(indicatorId);
            orphanIndicator.setCompetency(null);
            orphanIndicator.setTitle("Orphan Indicator");
            orphanIndicator.setWeight(1.0f);

            IndicatorAggregation indAgg = new IndicatorAggregation(indicatorId);
            indAgg.addAnswer(0.5);

            Map<UUID, IndicatorAggregation> indicatorAggs = Map.of(indicatorId, indAgg);
            Map<UUID, BehavioralIndicator> indicatorCache = Map.of(indicatorId, orphanIndicator);

            when(indicatorBatchLoader.getFromCache(indicatorCache, indicatorId))
                    .thenReturn(orphanIndicator);

            // When
            Map<UUID, CompetencyAggregation> result =
                    aggregationService.rollUpIndicatorsToCompetencies(indicatorAggs, indicatorCache);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should roll up single indicator to single competency")
        void should_rollUpSingleIndicator_when_oneIndicatorOneCompetency() {
            // Given
            UUID indicatorId = UUID.randomUUID();
            BehavioralIndicator indicator = createBehavioralIndicator(indicatorId, competency1);

            IndicatorAggregation indAgg = new IndicatorAggregation(indicatorId);
            indAgg.addAnswer(0.75);
            indAgg.addAnswer(1.0);

            Map<UUID, IndicatorAggregation> indicatorAggs = Map.of(indicatorId, indAgg);
            Map<UUID, BehavioralIndicator> indicatorCache = Map.of(indicatorId, indicator);

            when(indicatorBatchLoader.getFromCache(indicatorCache, indicatorId))
                    .thenReturn(indicator);

            // When
            Map<UUID, CompetencyAggregation> result =
                    aggregationService.rollUpIndicatorsToCompetencies(indicatorAggs, indicatorCache);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result).containsKey(competencyId1);

            CompetencyAggregation compAgg = result.get(competencyId1);
            assertThat(compAgg.getQuestionCount()).isEqualTo(2);
            assertThat(compAgg.getIndicatorScores()).hasSize(1);
        }

        @Test
        @DisplayName("Should roll up multiple indicators into their respective competencies")
        void should_rollUpToMultipleCompetencies_when_indicatorsMapToDifferentCompetencies() {
            // Given: 2 indicators for different competencies
            UUID indId1 = UUID.randomUUID();
            UUID indId2 = UUID.randomUUID();
            BehavioralIndicator ind1 = createBehavioralIndicator(indId1, competency1);
            BehavioralIndicator ind2 = createBehavioralIndicator(indId2, competency2);

            IndicatorAggregation agg1 = new IndicatorAggregation(indId1);
            agg1.addAnswer(0.75);

            IndicatorAggregation agg2 = new IndicatorAggregation(indId2);
            agg2.addAnswer(0.5);

            Map<UUID, IndicatorAggregation> indicatorAggs = Map.of(indId1, agg1, indId2, agg2);
            Map<UUID, BehavioralIndicator> indicatorCache = Map.of(indId1, ind1, indId2, ind2);

            when(indicatorBatchLoader.getFromCache(eq(indicatorCache), eq(indId1))).thenReturn(ind1);
            when(indicatorBatchLoader.getFromCache(eq(indicatorCache), eq(indId2))).thenReturn(ind2);

            // When
            Map<UUID, CompetencyAggregation> result =
                    aggregationService.rollUpIndicatorsToCompetencies(indicatorAggs, indicatorCache);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsKey(competencyId1);
            assertThat(result).containsKey(competencyId2);

            assertThat(result.get(competencyId1).getQuestionCount()).isEqualTo(1);
            assertThat(result.get(competencyId2).getQuestionCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should merge multiple indicators for the same competency")
        void should_mergeIndicators_when_multipleIndicatorsForSameCompetency() {
            // Given: 2 indicators both belonging to competency1
            UUID indId1 = UUID.randomUUID();
            UUID indId2 = UUID.randomUUID();
            BehavioralIndicator ind1 = createBehavioralIndicator(indId1, competency1);
            BehavioralIndicator ind2 = createBehavioralIndicator(indId2, competency1);

            IndicatorAggregation agg1 = new IndicatorAggregation(indId1);
            agg1.addAnswer(0.75);
            agg1.addAnswer(1.0);

            IndicatorAggregation agg2 = new IndicatorAggregation(indId2);
            agg2.addAnswer(0.5);

            Map<UUID, IndicatorAggregation> indicatorAggs = Map.of(indId1, agg1, indId2, agg2);
            Map<UUID, BehavioralIndicator> indicatorCache = Map.of(indId1, ind1, indId2, ind2);

            when(indicatorBatchLoader.getFromCache(eq(indicatorCache), eq(indId1))).thenReturn(ind1);
            when(indicatorBatchLoader.getFromCache(eq(indicatorCache), eq(indId2))).thenReturn(ind2);

            // When
            Map<UUID, CompetencyAggregation> result =
                    aggregationService.rollUpIndicatorsToCompetencies(indicatorAggs, indicatorCache);

            // Then
            assertThat(result).hasSize(1);
            CompetencyAggregation compAgg = result.get(competencyId1);
            assertThat(compAgg.getQuestionCount()).isEqualTo(3); // 2 + 1
            assertThat(compAgg.getIndicatorScores()).hasSize(2); // 2 distinct indicators
        }

        @Test
        @DisplayName("Should apply weighted aggregation when indicators have different weights")
        void should_applyWeightedAggregation_when_indicatorsHaveDifferentWeights() {
            // Given: 2 indicators for same competency with weights 2.0 and 1.0
            // Indicator 1: weight=2.0, 1 answer scoring 100% => weighted pct = 2.0 * 100 = 200
            // Indicator 2: weight=1.0, 1 answer scoring 50% => weighted pct = 1.0 * 50 = 50
            // Total weighted percentage = (200 + 50) / (2.0 + 1.0) = 83.33%
            UUID indId1 = UUID.randomUUID();
            UUID indId2 = UUID.randomUUID();
            BehavioralIndicator ind1 = createBehavioralIndicator(indId1, competency1, 2.0f);
            BehavioralIndicator ind2 = createBehavioralIndicator(indId2, competency1, 1.0f);

            IndicatorAggregation agg1 = new IndicatorAggregation(indId1);
            agg1.addAnswer(1.0); // 100%

            IndicatorAggregation agg2 = new IndicatorAggregation(indId2);
            agg2.addAnswer(0.5); // 50%

            Map<UUID, IndicatorAggregation> indicatorAggs = Map.of(indId1, agg1, indId2, agg2);
            Map<UUID, BehavioralIndicator> indicatorCache = Map.of(indId1, ind1, indId2, ind2);

            when(indicatorBatchLoader.getFromCache(eq(indicatorCache), eq(indId1))).thenReturn(ind1);
            when(indicatorBatchLoader.getFromCache(eq(indicatorCache), eq(indId2))).thenReturn(ind2);

            // When
            Map<UUID, CompetencyAggregation> result =
                    aggregationService.rollUpIndicatorsToCompetencies(indicatorAggs, indicatorCache);

            // Then
            CompetencyAggregation compAgg = result.get(competencyId1);
            // Weighted percentage: (2.0*100 + 1.0*50) / (2.0 + 1.0) = 250/3 = 83.33
            assertThat(compAgg.getWeightedPercentage()).isCloseTo(83.33, within(0.1));
            assertThat(compAgg.getTotalWeight()).isCloseTo(3.0, within(0.001));
        }
    }

    // =========================================================================
    // Stage 3: buildCompetencyScores
    // =========================================================================

    @Nested
    @DisplayName("buildCompetencyScores Tests")
    class BuildCompetencyScoresTests {

        @Test
        @DisplayName("Should return empty list when no competency aggregations")
        void should_returnEmptyList_when_noCompetencyAggregations() {
            // Given
            Map<UUID, CompetencyAggregation> emptyAggs = Collections.emptyMap();
            Map<UUID, Competency> emptyCache = Collections.emptyMap();

            // When
            List<CompetencyScoreDto> result =
                    aggregationService.buildCompetencyScores(emptyAggs, emptyCache);

            // Then
            assertThat(result).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Should use 'Unknown Competency' when competency not in cache")
        void should_useUnknownFallback_when_competencyNotInCache() {
            // Given: aggregation for a competency, but competency not in cache
            UUID unknownCompId = UUID.randomUUID();
            CompetencyAggregation compAgg = new CompetencyAggregation(unknownCompId);

            // Add a dummy indicator to give it some data
            IndicatorScoreDto indDto = new IndicatorScoreDto();
            indDto.setWeight(1.0);
            indDto.setPercentage(75.0);
            indDto.setScore(0.75);
            indDto.setMaxScore(1.0);
            indDto.setQuestionsAnswered(1);

            IndicatorAggregation indAgg = new IndicatorAggregation(UUID.randomUUID());
            indAgg.addAnswer(0.75);

            compAgg.addIndicator(indDto, indAgg);

            Map<UUID, CompetencyAggregation> competencyAggs = Map.of(unknownCompId, compAgg);

            when(competencyBatchLoader.getFromCache(any(), eq(unknownCompId))).thenReturn(null);

            // When
            List<CompetencyScoreDto> result =
                    aggregationService.buildCompetencyScores(competencyAggs, Map.of());

            // Then
            assertThat(result).hasSize(1);
            CompetencyScoreDto dto = result.get(0);
            assertThat(dto.getCompetencyName()).isEqualTo("Unknown Competency");
            assertThat(dto.getOnetCode()).isNull();
        }

        @Test
        @DisplayName("Should populate DTO fields correctly from competency and aggregation")
        void should_populateDtoCorrectly_when_competencyFoundInCache() {
            // Given
            CompetencyAggregation compAgg = new CompetencyAggregation(competencyId1);

            IndicatorScoreDto indDto = new IndicatorScoreDto();
            indDto.setWeight(1.0);
            indDto.setPercentage(75.0);
            indDto.setScore(0.75);
            indDto.setMaxScore(1.0);
            indDto.setQuestionsAnswered(1);

            IndicatorAggregation indAgg = new IndicatorAggregation(UUID.randomUUID());
            indAgg.addAnswer(0.75);

            compAgg.addIndicator(indDto, indAgg);

            Map<UUID, CompetencyAggregation> competencyAggs = Map.of(competencyId1, compAgg);
            Map<UUID, Competency> competencyCache = Map.of(competencyId1, competency1);

            when(competencyBatchLoader.getFromCache(competencyCache, competencyId1))
                    .thenReturn(competency1);

            // When
            List<CompetencyScoreDto> result =
                    aggregationService.buildCompetencyScores(competencyAggs, competencyCache);

            // Then
            assertThat(result).hasSize(1);
            CompetencyScoreDto dto = result.get(0);
            assertThat(dto.getCompetencyId()).isEqualTo(competencyId1);
            assertThat(dto.getCompetencyName()).isEqualTo("Leadership");
            assertThat(dto.getOnetCode()).isEqualTo("2.B.1.a");
            assertThat(dto.getPercentage()).isCloseTo(75.0, within(0.1));
            assertThat(dto.getQuestionsAnswered()).isEqualTo(1);
            assertThat(dto.getIndicatorScores()).hasSize(1);
        }

        @Test
        @DisplayName("Should build DTOs for multiple competencies")
        void should_buildMultipleDtos_when_multipleCompetencyAggregations() {
            // Given: 2 competencies
            CompetencyAggregation compAgg1 = new CompetencyAggregation(competencyId1);
            CompetencyAggregation compAgg2 = new CompetencyAggregation(competencyId2);

            IndicatorAggregation indAgg1 = new IndicatorAggregation(UUID.randomUUID());
            indAgg1.addAnswer(1.0);
            IndicatorScoreDto indDto1 = indAgg1.toDto(createBehavioralIndicator(UUID.randomUUID(), competency1));
            compAgg1.addIndicator(indDto1, indAgg1);

            IndicatorAggregation indAgg2 = new IndicatorAggregation(UUID.randomUUID());
            indAgg2.addAnswer(0.5);
            IndicatorScoreDto indDto2 = indAgg2.toDto(createBehavioralIndicator(UUID.randomUUID(), competency2));
            compAgg2.addIndicator(indDto2, indAgg2);

            Map<UUID, CompetencyAggregation> competencyAggs = new HashMap<>();
            competencyAggs.put(competencyId1, compAgg1);
            competencyAggs.put(competencyId2, compAgg2);

            Map<UUID, Competency> competencyCache = Map.of(
                    competencyId1, competency1,
                    competencyId2, competency2
            );

            when(competencyBatchLoader.getFromCache(competencyCache, competencyId1))
                    .thenReturn(competency1);
            when(competencyBatchLoader.getFromCache(competencyCache, competencyId2))
                    .thenReturn(competency2);

            // When
            List<CompetencyScoreDto> result =
                    aggregationService.buildCompetencyScores(competencyAggs, competencyCache);

            // Then
            assertThat(result).hasSize(2);

            List<String> names = result.stream()
                    .map(CompetencyScoreDto::getCompetencyName)
                    .toList();
            assertThat(names).containsExactlyInAnyOrder("Leadership", "Communication");
        }

        @Test
        @DisplayName("Should handle competency without O*NET code")
        void should_setNullOnetCode_when_competencyHasNoStandardCodes() {
            // Given
            Competency noCodesCompetency = createCompetency(competencyId1, "Generic Skill", null);

            CompetencyAggregation compAgg = new CompetencyAggregation(competencyId1);
            IndicatorAggregation indAgg = new IndicatorAggregation(UUID.randomUUID());
            indAgg.addAnswer(0.5);
            IndicatorScoreDto indDto = indAgg.toDto(createBehavioralIndicator(UUID.randomUUID(), noCodesCompetency));
            compAgg.addIndicator(indDto, indAgg);

            Map<UUID, Competency> competencyCache = Map.of(competencyId1, noCodesCompetency);

            when(competencyBatchLoader.getFromCache(competencyCache, competencyId1))
                    .thenReturn(noCodesCompetency);

            // When
            List<CompetencyScoreDto> result =
                    aggregationService.buildCompetencyScores(Map.of(competencyId1, compAgg), competencyCache);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getOnetCode()).isNull();
        }
    }

    // =========================================================================
    // Stage 4: applyEvidenceSufficiency
    // =========================================================================

    @Nested
    @DisplayName("applyEvidenceSufficiency Tests")
    class ApplyEvidenceSufficiencyTests {

        @Test
        @DisplayName("Should flag competency when questions answered below minimum")
        void should_flagInsufficientEvidence_when_questionsBelowMinimum() {
            // Given: competency with 2 questions, minimum threshold = 3
            CompetencyScoreDto dto = new CompetencyScoreDto();
            dto.setCompetencyName("Leadership");
            dto.setQuestionsAnswered(2);

            // When
            aggregationService.applyEvidenceSufficiency(List.of(dto), 3);

            // Then
            assertThat(dto.getInsufficientEvidence()).isTrue();
            assertThat(dto.getEvidenceNote())
                    .contains("2 question(s)")
                    .contains("minimum 3");
        }

        @Test
        @DisplayName("Should not flag competency when questions at minimum threshold")
        void should_notFlag_when_questionsAtMinimum() {
            // Given: competency with exactly 3 questions, minimum = 3
            CompetencyScoreDto dto = new CompetencyScoreDto();
            dto.setCompetencyName("Communication");
            dto.setQuestionsAnswered(3);

            // When
            aggregationService.applyEvidenceSufficiency(List.of(dto), 3);

            // Then
            assertThat(dto.getInsufficientEvidence()).isNull();
            assertThat(dto.getEvidenceNote()).isNull();
        }

        @Test
        @DisplayName("Should not flag competency when questions above minimum")
        void should_notFlag_when_questionsAboveMinimum() {
            // Given: competency with 5 questions, minimum = 3
            CompetencyScoreDto dto = new CompetencyScoreDto();
            dto.setCompetencyName("Problem Solving");
            dto.setQuestionsAnswered(5);

            // When
            aggregationService.applyEvidenceSufficiency(List.of(dto), 3);

            // Then
            assertThat(dto.getInsufficientEvidence()).isNull();
            assertThat(dto.getEvidenceNote()).isNull();
        }

        @Test
        @DisplayName("Should flag only insufficient competencies in mixed list")
        void should_flagOnlyInsufficient_when_mixedSufficiency() {
            // Given: 1 under threshold (1 question), 1 at threshold (3 questions)
            CompetencyScoreDto insufficient = new CompetencyScoreDto();
            insufficient.setCompetencyName("Low Coverage");
            insufficient.setQuestionsAnswered(1);

            CompetencyScoreDto sufficient = new CompetencyScoreDto();
            sufficient.setCompetencyName("Good Coverage");
            sufficient.setQuestionsAnswered(3);

            // When
            aggregationService.applyEvidenceSufficiency(List.of(insufficient, sufficient), 3);

            // Then
            assertThat(insufficient.getInsufficientEvidence()).isTrue();
            assertThat(insufficient.getEvidenceNote()).contains("1 question(s)");
            assertThat(sufficient.getInsufficientEvidence()).isNull();
            assertThat(sufficient.getEvidenceNote()).isNull();
        }

        @Test
        @DisplayName("Should handle empty score list gracefully")
        void should_doNothing_when_scoreListIsEmpty() {
            // Given / When / Then: no exception
            assertThatCode(() -> aggregationService.applyEvidenceSufficiency(List.of(), 3))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should flag single-question competency as insufficient when minimum is 2")
        void should_flagSingleQuestion_when_minimumIsTwo() {
            // Given
            CompetencyScoreDto dto = new CompetencyScoreDto();
            dto.setCompetencyName("Barely Assessed");
            dto.setQuestionsAnswered(1);

            // When
            aggregationService.applyEvidenceSufficiency(List.of(dto), 2);

            // Then
            assertThat(dto.getInsufficientEvidence()).isTrue();
            assertThat(dto.getEvidenceNote()).contains("1 question(s)").contains("minimum 2");
        }

        @Test
        @DisplayName("Should handle zero questions answered")
        void should_flagZeroQuestions_when_zeroQuestionsAnswered() {
            // Given
            CompetencyScoreDto dto = new CompetencyScoreDto();
            dto.setCompetencyName("No Answers");
            dto.setQuestionsAnswered(0);

            // When
            aggregationService.applyEvidenceSufficiency(List.of(dto), 1);

            // Then
            assertThat(dto.getInsufficientEvidence()).isTrue();
            assertThat(dto.getEvidenceNote()).contains("0 question(s)");
        }
    }

    // =========================================================================
    // Stage 5: Full pipeline - aggregate()
    // =========================================================================

    @Nested
    @DisplayName("Full Pipeline (aggregate) Tests")
    class FullPipelineTests {

        @Test
        @DisplayName("Should return complete result for valid answers through full pipeline")
        void should_returnCompleteResult_when_validAnswersProcessed() {
            // Given: 2 Likert answers (3 and 5) for same indicator/competency
            UUID indicatorId = UUID.randomUUID();
            BehavioralIndicator indicator = createBehavioralIndicator(indicatorId, competency1);

            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            AssessmentQuestion q2 = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);

            TestAnswer a1 = createAnswer(q1, 3, null, false); // 0.5
            TestAnswer a2 = createAnswer(q2, 5, null, false); // 1.0

            setupIndicatorBatchLoaderMock();
            setupCompetencyBatchLoaderMock(Map.of(competencyId1, competency1));

            // When
            CompetencyAggregationService.AggregationResult result =
                    aggregationService.aggregate(List.of(a1, a2));

            // Then
            assertThat(result).isNotNull();

            // Indicator aggregations
            assertThat(result.indicatorAggregations()).hasSize(1);
            IndicatorAggregation indAgg = result.indicatorAggregations().get(indicatorId);
            assertThat(indAgg.getQuestionCount()).isEqualTo(2);
            assertThat(indAgg.getTotalScore()).isCloseTo(1.5, within(0.001));

            // Competency aggregations
            assertThat(result.competencyAggregations()).hasSize(1);
            assertThat(result.competencyAggregations()).containsKey(competencyId1);

            // Competency scores (final DTOs)
            assertThat(result.competencyScores()).hasSize(1);
            CompetencyScoreDto dto = result.competencyScores().get(0);
            assertThat(dto.getCompetencyName()).isEqualTo("Leadership");
            assertThat(dto.getPercentage()).isCloseTo(75.0, within(0.1)); // (0.5+1.0)/2 * 100
            assertThat(dto.getQuestionsAnswered()).isEqualTo(2);

            // Caches exposed for strategy-specific lookups
            assertThat(result.competencyCache()).isNotNull();
            assertThat(result.indicatorCache()).isNotNull();
        }

        @Test
        @DisplayName("Should process multiple competencies through full pipeline")
        void should_processMultipleCompetencies_when_answersSpanCompetencies() {
            // Given
            UUID indId1 = UUID.randomUUID();
            UUID indId2 = UUID.randomUUID();
            BehavioralIndicator ind1 = createBehavioralIndicator(indId1, competency1);
            BehavioralIndicator ind2 = createBehavioralIndicator(indId2, competency2);

            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), ind1, QuestionType.LIKERT);
            AssessmentQuestion q2 = createQuestion(UUID.randomUUID(), ind2, QuestionType.SJT);

            TestAnswer a1 = createAnswer(q1, 5, null, false);   // Leadership: 1.0
            TestAnswer a2 = createAnswer(q2, null, 0.6, false); // Communication: 0.6

            setupIndicatorBatchLoaderMock();
            setupCompetencyBatchLoaderMock(Map.of(
                    competencyId1, competency1,
                    competencyId2, competency2
            ));

            // When
            CompetencyAggregationService.AggregationResult result =
                    aggregationService.aggregate(List.of(a1, a2));

            // Then
            assertThat(result.competencyScores()).hasSize(2);

            Map<String, Double> nameToPercentage = new HashMap<>();
            for (CompetencyScoreDto dto : result.competencyScores()) {
                nameToPercentage.put(dto.getCompetencyName(), dto.getPercentage());
            }

            assertThat(nameToPercentage.get("Leadership")).isCloseTo(100.0, within(0.1));
            assertThat(nameToPercentage.get("Communication")).isCloseTo(60.0, within(0.1));
        }

        @Test
        @DisplayName("Should return empty results for empty answer list")
        void should_returnEmptyResults_when_emptyAnswerList() {
            // Given
            setupIndicatorBatchLoaderMock();
            setupCompetencyBatchLoaderMock(Map.of());

            // When
            CompetencyAggregationService.AggregationResult result =
                    aggregationService.aggregate(Collections.emptyList());

            // Then
            assertThat(result).isNotNull();
            assertThat(result.indicatorAggregations()).isEmpty();
            assertThat(result.competencyAggregations()).isEmpty();
            assertThat(result.competencyScores()).isEmpty();
        }

        @Test
        @DisplayName("Should return empty competency scores when all answers are skipped")
        void should_returnEmptyScores_when_allAnswersSkipped() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competency1);
            AssessmentQuestion q = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer skipped = createAnswer(q, null, null, true);

            setupIndicatorBatchLoaderMock();
            setupCompetencyBatchLoaderMock(Map.of(competencyId1, competency1));

            // When
            CompetencyAggregationService.AggregationResult result =
                    aggregationService.aggregate(List.of(skipped));

            // Then
            assertThat(result.indicatorAggregations()).isEmpty();
            assertThat(result.competencyScores()).isEmpty();
        }
    }

    // =========================================================================
    // Edge cases and boundary conditions
    // =========================================================================

    @Nested
    @DisplayName("Edge Case and Boundary Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle large number of answers without performance degradation")
        void should_handleLargeVolume_when_manyAnswersProvided() {
            // Given: 1000 answers across 10 indicators, 5 competencies
            int answerCount = 1000;
            int indicatorCount = 10;
            int competencyCount = 5;

            List<Competency> competencies = new ArrayList<>();
            Map<UUID, Competency> competencyMap = new HashMap<>();
            for (int c = 0; c < competencyCount; c++) {
                UUID cId = UUID.randomUUID();
                Competency comp = createCompetency(cId, "Competency_" + c, null);
                competencies.add(comp);
                competencyMap.put(cId, comp);
            }

            List<BehavioralIndicator> indicators = new ArrayList<>();
            for (int i = 0; i < indicatorCount; i++) {
                Competency parentComp = competencies.get(i % competencyCount);
                UUID indId = UUID.randomUUID();
                indicators.add(createBehavioralIndicator(indId, parentComp));
            }

            List<TestAnswer> answers = new ArrayList<>();
            for (int a = 0; a < answerCount; a++) {
                BehavioralIndicator ind = indicators.get(a % indicatorCount);
                AssessmentQuestion q = createQuestion(UUID.randomUUID(), ind, QuestionType.LIKERT);
                int likertValue = (a % 5) + 1; // Cycle 1-5
                answers.add(createAnswer(q, likertValue, null, false));
            }

            setupIndicatorBatchLoaderMock();
            setupCompetencyBatchLoaderMock(competencyMap);

            // When
            long startTime = System.currentTimeMillis();
            CompetencyAggregationService.AggregationResult result =
                    aggregationService.aggregate(answers);
            long elapsed = System.currentTimeMillis() - startTime;

            // Then
            assertThat(result.indicatorAggregations()).hasSize(indicatorCount);
            assertThat(result.competencyAggregations()).hasSize(competencyCount);
            assertThat(result.competencyScores()).hasSize(competencyCount);

            // Sanity: should complete within a reasonable time (1 second for 1000 answers)
            assertThat(elapsed).isLessThan(1000L);

            // Verify total question count across all competencies
            int totalQuestions = result.competencyScores().stream()
                    .mapToInt(CompetencyScoreDto::getQuestionsAnswered)
                    .sum();
            assertThat(totalQuestions).isEqualTo(answerCount);
        }

        @Test
        @DisplayName("Should produce 0% percentage when all answers have minimum scores")
        void should_produceZeroPercentage_when_allMinimumScores() {
            // Given: All Likert 1 => 0% through full pipeline
            UUID indicatorId = UUID.randomUUID();
            BehavioralIndicator indicator = createBehavioralIndicator(indicatorId, competency1);

            List<TestAnswer> answers = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                AssessmentQuestion q = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
                answers.add(createAnswer(q, 1, null, false));
            }

            setupIndicatorBatchLoaderMock();
            setupCompetencyBatchLoaderMock(Map.of(competencyId1, competency1));

            // When
            CompetencyAggregationService.AggregationResult result =
                    aggregationService.aggregate(answers);

            // Then
            assertThat(result.competencyScores()).hasSize(1);
            CompetencyScoreDto dto = result.competencyScores().get(0);
            assertThat(dto.getPercentage()).isCloseTo(0.0, within(0.001));
            assertThat(dto.getScore()).isCloseTo(0.0, within(0.001));
            assertThat(dto.getQuestionsAnswered()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should produce 100% percentage when all answers have maximum scores")
        void should_produceHundredPercentage_when_allMaximumScores() {
            // Given: All Likert 5 => 100% through full pipeline
            UUID indicatorId = UUID.randomUUID();
            BehavioralIndicator indicator = createBehavioralIndicator(indicatorId, competency1);

            List<TestAnswer> answers = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                AssessmentQuestion q = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
                answers.add(createAnswer(q, 5, null, false));
            }

            setupIndicatorBatchLoaderMock();
            setupCompetencyBatchLoaderMock(Map.of(competencyId1, competency1));

            // When
            CompetencyAggregationService.AggregationResult result =
                    aggregationService.aggregate(answers);

            // Then
            assertThat(result.competencyScores()).hasSize(1);
            CompetencyScoreDto dto = result.competencyScores().get(0);
            assertThat(dto.getPercentage()).isCloseTo(100.0, within(0.001));
            assertThat(dto.getScore()).isCloseTo(5.0, within(0.001));
            assertThat(dto.getMaxScore()).isCloseTo(5.0, within(0.001));
        }

        @Test
        @DisplayName("Should handle single answer through full pipeline without errors")
        void should_handleSingleAnswer_when_onlyOneAnswerProvided() {
            // Given
            UUID indicatorId = UUID.randomUUID();
            BehavioralIndicator indicator = createBehavioralIndicator(indicatorId, competency1);
            AssessmentQuestion q = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(q, 4, null, false); // 0.75

            setupIndicatorBatchLoaderMock();
            setupCompetencyBatchLoaderMock(Map.of(competencyId1, competency1));

            // When
            CompetencyAggregationService.AggregationResult result =
                    aggregationService.aggregate(List.of(answer));

            // Then
            assertThat(result.competencyScores()).hasSize(1);
            CompetencyScoreDto dto = result.competencyScores().get(0);
            assertThat(dto.getPercentage()).isCloseTo(75.0, within(0.1));
            assertThat(dto.getQuestionsAnswered()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should propagate indicator scores correctly into competency DTO")
        void should_includeIndicatorScores_when_buildingCompetencyDto() {
            // Given: 2 indicators for same competency
            UUID indId1 = UUID.randomUUID();
            UUID indId2 = UUID.randomUUID();
            BehavioralIndicator ind1 = createBehavioralIndicator(indId1, competency1);
            BehavioralIndicator ind2 = createBehavioralIndicator(indId2, competency1);

            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), ind1, QuestionType.LIKERT);
            AssessmentQuestion q2 = createQuestion(UUID.randomUUID(), ind2, QuestionType.SJT);

            TestAnswer a1 = createAnswer(q1, 5, null, false);    // 1.0
            TestAnswer a2 = createAnswer(q2, null, 0.6, false);  // 0.6

            setupIndicatorBatchLoaderMock();
            setupCompetencyBatchLoaderMock(Map.of(competencyId1, competency1));

            // When
            CompetencyAggregationService.AggregationResult result =
                    aggregationService.aggregate(List.of(a1, a2));

            // Then
            assertThat(result.competencyScores()).hasSize(1);
            CompetencyScoreDto dto = result.competencyScores().get(0);
            assertThat(dto.getIndicatorScores()).hasSize(2);

            // Each indicator should have 1 question
            for (IndicatorScoreDto indDto : dto.getIndicatorScores()) {
                assertThat(indDto.getQuestionsAnswered()).isEqualTo(1);
            }
        }

        @Test
        @DisplayName("Should expose caches in AggregationResult for strategy-specific use")
        void should_exposeCaches_when_aggregateCompletes() {
            // Given
            UUID indicatorId = UUID.randomUUID();
            BehavioralIndicator indicator = createBehavioralIndicator(indicatorId, competency1);
            AssessmentQuestion q = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(q, 3, null, false);

            Map<UUID, Competency> competencyMap = Map.of(competencyId1, competency1);
            setupIndicatorBatchLoaderMock();
            setupCompetencyBatchLoaderMock(competencyMap);

            // When
            CompetencyAggregationService.AggregationResult result =
                    aggregationService.aggregate(List.of(answer));

            // Then
            assertThat(result.competencyCache()).isEqualTo(competencyMap);
            assertThat(result.indicatorCache()).isNotNull();
            assertThat(result.indicatorCache()).containsKey(indicatorId);
        }
    }
}
