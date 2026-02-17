package app.skillsoft.assessmentbackend.services.scoring.impl;

import app.skillsoft.assessmentbackend.config.ScoringConfiguration;
import app.skillsoft.assessmentbackend.domain.dto.CompetencyScoreDto;
import app.skillsoft.assessmentbackend.domain.dto.IndicatorScoreDto;
import app.skillsoft.assessmentbackend.domain.dto.StandardCodesDto;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.services.scoring.CompetencyBatchLoader;
import app.skillsoft.assessmentbackend.services.scoring.IndicatorBatchLoader;
import app.skillsoft.assessmentbackend.services.scoring.ScoreInterpreter;
import app.skillsoft.assessmentbackend.services.scoring.ScoreNormalizer;
import app.skillsoft.assessmentbackend.services.scoring.ScoringResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OverviewScoringStrategy.
 *
 * Tests cover:
 * - Likert value normalization (1-5 to 0-1)
 * - SJT score handling
 * - MCQ score handling
 * - Competency aggregation accuracy
 * - Empty answer list handling
 * - Skipped/unanswered questions filtering
 * - Edge cases: single answer, all max scores, all min scores
 * - Overall percentage calculation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Overview Scoring Strategy Tests")
class OverviewScoringStrategyTest {

    @Mock
    private CompetencyBatchLoader competencyBatchLoader;

    @Mock
    private IndicatorBatchLoader indicatorBatchLoader;

    @Spy
    private ScoreNormalizer scoreNormalizer = new ScoreNormalizer();

    @Spy
    private ScoreInterpreter scoreInterpreter = new ScoreInterpreter();

    private ScoringConfiguration config;
    private OverviewScoringStrategy scoringStrategy;

    private TestSession mockSession;
    private TestTemplate mockTemplate;
    private UUID competencyId1;
    private UUID competencyId2;
    private Competency competency1;
    private Competency competency2;

    @BeforeEach
    void setUp() {
        // Set up configuration with defaults
        config = new ScoringConfiguration();

        // Construct strategy manually with all dependencies (constructor injection)
        scoringStrategy = new OverviewScoringStrategy(
                competencyBatchLoader,
                indicatorBatchLoader,
                scoreNormalizer,
                config,
                scoreInterpreter
        );

        // Set up UUIDs
        competencyId1 = UUID.randomUUID();
        competencyId2 = UUID.randomUUID();

        // Set up competencies
        competency1 = createCompetency(competencyId1, "Leadership", "2.B.1.a");
        competency2 = createCompetency(competencyId2, "Communication", "2.A.1.b");

        // Set up mock template
        mockTemplate = new TestTemplate();
        mockTemplate.setId(UUID.randomUUID());
        mockTemplate.setName("Overview Assessment");
        mockTemplate.setGoal(AssessmentGoal.OVERVIEW);

        // Set up mock session
        mockSession = new TestSession();
        mockSession.setId(UUID.randomUUID());
        mockSession.setTemplate(mockTemplate);
        mockSession.setClerkUserId("user_test123");
        mockSession.setStatus(SessionStatus.COMPLETED);
    }

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

    private BehavioralIndicator createBehavioralIndicator(UUID id, Competency competency) {
        BehavioralIndicator indicator = new BehavioralIndicator();
        indicator.setId(id);
        indicator.setCompetency(competency);
        indicator.setTitle("Test Indicator");
        indicator.setWeight(1.0f);
        indicator.setActive(true);
        indicator.setApprovalStatus(ApprovalStatus.APPROVED);
        return indicator;
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

    private TestAnswer createAnswer(TestSession session, AssessmentQuestion question,
                                    Integer likertValue, Double score, boolean isSkipped) {
        TestAnswer answer = new TestAnswer();
        answer.setId(UUID.randomUUID());
        answer.setSession(session);
        answer.setQuestion(question);
        answer.setLikertValue(likertValue);
        answer.setScore(score);
        answer.setIsSkipped(isSkipped);
        answer.setAnsweredAt(isSkipped ? null : LocalDateTime.now());
        return answer;
    }

    /**
     * Sets up the CompetencyBatchLoader and IndicatorBatchLoader mocks.
     * Configures both loaders to dynamically extract from the answer → question → indicator → competency chain.
     */
    private void setupBatchLoaderMock(Map<UUID, Competency> competencyMap) {
        // Competency batch loader mocks
        when(competencyBatchLoader.loadCompetenciesForAnswers(anyList()))
            .thenReturn(competencyMap);

        // Note: extractCompetencyIdSafe is no longer called by OverviewScoringStrategy
        // (replaced by indicator-level aggregation with indicatorBatchLoader)

        when(competencyBatchLoader.getFromCache(any(), any()))
            .thenAnswer(invocation -> {
                Map<UUID, Competency> cache = invocation.getArgument(0);
                UUID id = invocation.getArgument(1);
                return cache != null ? cache.get(id) : null;
            });

        // Indicator batch loader mocks - extract dynamically from the answer chain
        when(indicatorBatchLoader.loadIndicatorsForAnswers(anyList()))
            .thenAnswer(invocation -> {
                List<TestAnswer> answers = invocation.getArgument(0);
                Map<UUID, BehavioralIndicator> map = new HashMap<>();
                for (TestAnswer a : answers) {
                    if (a != null && a.getQuestion() != null && a.getQuestion().getBehavioralIndicator() != null) {
                        BehavioralIndicator ind = a.getQuestion().getBehavioralIndicator();
                        map.put(ind.getId(), ind);
                    }
                }
                return map;
            });

        when(indicatorBatchLoader.extractIndicatorIdSafe(any(TestAnswer.class)))
            .thenAnswer(invocation -> {
                TestAnswer answer = invocation.getArgument(0);
                if (answer == null || answer.getQuestion() == null
                    || answer.getQuestion().getBehavioralIndicator() == null) {
                    return Optional.empty();
                }
                return Optional.of(answer.getQuestion().getBehavioralIndicator().getId());
            });

        when(indicatorBatchLoader.getFromCache(any(), any()))
            .thenAnswer(invocation -> {
                Map<UUID, BehavioralIndicator> cache = invocation.getArgument(0);
                UUID id = invocation.getArgument(1);
                return cache != null ? cache.get(id) : null;
            });
    }

    /**
     * Sets up the batch loader mocks for tests where the competency is not found in the cache.
     */
    private void setupBatchLoaderMockWithEmptyCache(UUID competencyId) {
        when(competencyBatchLoader.loadCompetenciesForAnswers(anyList()))
            .thenReturn(Map.of()); // Empty cache

        when(competencyBatchLoader.getFromCache(any(), any()))
            .thenReturn(null);

        // Indicator batch loader mocks - dynamically extract indicators
        when(indicatorBatchLoader.loadIndicatorsForAnswers(anyList()))
            .thenAnswer(invocation -> {
                List<TestAnswer> answers = invocation.getArgument(0);
                Map<UUID, BehavioralIndicator> map = new HashMap<>();
                for (TestAnswer a : answers) {
                    if (a != null && a.getQuestion() != null && a.getQuestion().getBehavioralIndicator() != null) {
                        BehavioralIndicator ind = a.getQuestion().getBehavioralIndicator();
                        map.put(ind.getId(), ind);
                    }
                }
                return map;
            });

        when(indicatorBatchLoader.extractIndicatorIdSafe(any(TestAnswer.class)))
            .thenAnswer(invocation -> {
                TestAnswer answer = invocation.getArgument(0);
                if (answer == null || answer.getQuestion() == null
                    || answer.getQuestion().getBehavioralIndicator() == null) {
                    return Optional.empty();
                }
                return Optional.of(answer.getQuestion().getBehavioralIndicator().getId());
            });

        when(indicatorBatchLoader.getFromCache(any(), any()))
            .thenAnswer(invocation -> {
                Map<UUID, BehavioralIndicator> cache = invocation.getArgument(0);
                UUID id = invocation.getArgument(1);
                return cache != null ? cache.get(id) : null;
            });
    }

    @Nested
    @DisplayName("Supported Goal Tests")
    class SupportedGoalTests {

        @Test
        @DisplayName("Should return OVERVIEW as supported goal")
        void shouldReturnOverviewGoal() {
            // When
            AssessmentGoal goal = scoringStrategy.getSupportedGoal();

            // Then
            assertThat(goal).isEqualTo(AssessmentGoal.OVERVIEW);
        }
    }

    @Nested
    @DisplayName("Empty Answer List Tests")
    class EmptyAnswerListTests {

        @Test
        @DisplayName("Should return zero score for empty answer list")
        void shouldReturnZeroScoreForEmptyList() {
            // Given
            List<TestAnswer> emptyAnswers = Collections.emptyList();

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, emptyAnswers);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getOverallScore()).isEqualTo(0.0);
            assertThat(result.getOverallPercentage()).isEqualTo(0.0);
            assertThat(result.getCompetencyScores()).isEmpty();
            assertThat(result.getGoal()).isEqualTo(AssessmentGoal.OVERVIEW);
        }

        @Test
        @DisplayName("Should return zero score when all answers are skipped")
        void shouldReturnZeroScoreWhenAllSkipped() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competency1);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);

            TestAnswer skippedAnswer = createAnswer(mockSession, question, null, null, true);
            List<TestAnswer> answers = List.of(skippedAnswer);

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, answers);

            // Then
            assertThat(result.getOverallScore()).isEqualTo(0.0);
            assertThat(result.getOverallPercentage()).isEqualTo(0.0);
            assertThat(result.getCompetencyScores()).isEmpty();
        }

        @Test
        @DisplayName("Should return zero score when all answers have null answeredAt")
        void shouldReturnZeroScoreWhenAllUnanswered() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competency1);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);

            TestAnswer unansweredAnswer = new TestAnswer();
            unansweredAnswer.setId(UUID.randomUUID());
            unansweredAnswer.setSession(mockSession);
            unansweredAnswer.setQuestion(question);
            unansweredAnswer.setIsSkipped(false);
            unansweredAnswer.setAnsweredAt(null); // Unanswered

            List<TestAnswer> answers = List.of(unansweredAnswer);

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, answers);

            // Then
            assertThat(result.getOverallScore()).isEqualTo(0.0);
            assertThat(result.getCompetencyScores()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Likert Scale Normalization Tests")
    class LikertNormalizationTests {

        @ParameterizedTest(name = "Likert value {0} should normalize to {1}")
        @CsvSource({
            "1, 0.0",    // (1-1)/4 = 0.0
            "2, 0.25",   // (2-1)/4 = 0.25
            "3, 0.5",    // (3-1)/4 = 0.5
            "4, 0.75",   // (4-1)/4 = 0.75
            "5, 1.0"     // (5-1)/4 = 1.0
        })
        @DisplayName("Should correctly normalize Likert values")
        void shouldNormalizeLikertValues(int likertValue, double expectedNormalized) {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competency1);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(mockSession, question, likertValue, null, false);

            setupBatchLoaderMock(Map.of(competencyId1, competency1));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            assertThat(result.getCompetencyScores()).hasSize(1);
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getScore()).isCloseTo(expectedNormalized, within(0.001));
            assertThat(score.getPercentage()).isCloseTo(expectedNormalized * 100, within(0.1));
        }

        @Test
        @DisplayName("Should clamp Likert values below minimum to 1")
        void shouldClampLikertBelowMinimum() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competency1);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(mockSession, question, 0, null, false);

            setupBatchLoaderMock(Map.of(competencyId1, competency1));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getScore()).isCloseTo(0.0, within(0.001)); // Clamped to 1, normalized to 0
        }

        @Test
        @DisplayName("Should clamp Likert values above maximum to 5")
        void shouldClampLikertAboveMaximum() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competency1);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(mockSession, question, 10, null, false);

            setupBatchLoaderMock(Map.of(competencyId1, competency1));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getScore()).isCloseTo(1.0, within(0.001)); // Clamped to 5, normalized to 1
        }

        @Test
        @DisplayName("Should handle LIKERT_SCALE question type same as LIKERT")
        void shouldHandleLikertScaleType() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competency1);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT_SCALE);
            TestAnswer answer = createAnswer(mockSession, question, 3, null, false);

            setupBatchLoaderMock(Map.of(competencyId1, competency1));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getScore()).isCloseTo(0.5, within(0.001)); // (3-1)/4 = 0.5
        }

        @Test
        @DisplayName("Should handle FREQUENCY_SCALE question type same as LIKERT")
        void shouldHandleFrequencyScaleType() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competency1);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.FREQUENCY_SCALE);
            TestAnswer answer = createAnswer(mockSession, question, 4, null, false);

            setupBatchLoaderMock(Map.of(competencyId1, competency1));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getScore()).isCloseTo(0.75, within(0.001)); // (4-1)/4 = 0.75
        }
    }

    @Nested
    @DisplayName("SJT Score Normalization Tests")
    class SjtNormalizationTests {

        @Test
        @DisplayName("Should use pre-calculated score for SJT questions")
        void shouldUsePreCalculatedScoreForSjt() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competency1);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.SJT);
            TestAnswer answer = createAnswer(mockSession, question, null, 0.8, false);

            setupBatchLoaderMock(Map.of(competencyId1, competency1));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getScore()).isCloseTo(0.8, within(0.001));
            assertThat(score.getPercentage()).isCloseTo(80.0, within(0.1));
        }

        @Test
        @DisplayName("Should clamp SJT score above 1.0 to 1.0")
        void shouldClampSjtScoreAboveOne() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competency1);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.SJT);
            TestAnswer answer = createAnswer(mockSession, question, null, 1.5, false);

            setupBatchLoaderMock(Map.of(competencyId1, competency1));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getScore()).isCloseTo(1.0, within(0.001));
        }

        @Test
        @DisplayName("Should clamp SJT score below 0.0 to 0.0")
        void shouldClampSjtScoreBelowZero() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competency1);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.SJT);
            TestAnswer answer = createAnswer(mockSession, question, null, -0.5, false);

            setupBatchLoaderMock(Map.of(competencyId1, competency1));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getScore()).isCloseTo(0.0, within(0.001));
        }

        @Test
        @DisplayName("Should return 0 for SJT without score")
        void shouldReturnZeroForSjtWithoutScore() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competency1);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.SJT);
            TestAnswer answer = createAnswer(mockSession, question, null, null, false);

            setupBatchLoaderMock(Map.of(competencyId1, competency1));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getScore()).isCloseTo(0.0, within(0.001));
        }

        @Test
        @DisplayName("Should handle SITUATIONAL_JUDGMENT type same as SJT")
        void shouldHandleSituationalJudgmentType() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competency1);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.SITUATIONAL_JUDGMENT);
            TestAnswer answer = createAnswer(mockSession, question, null, 0.6, false);

            setupBatchLoaderMock(Map.of(competencyId1, competency1));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getScore()).isCloseTo(0.6, within(0.001));
        }
    }

    @Nested
    @DisplayName("MCQ Score Normalization Tests")
    class McqNormalizationTests {

        @Test
        @DisplayName("Should use score for correct MCQ answer")
        void shouldUseScoreForCorrectMcq() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competency1);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.MCQ);
            TestAnswer answer = createAnswer(mockSession, question, null, 1.0, false);

            setupBatchLoaderMock(Map.of(competencyId1, competency1));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getScore()).isCloseTo(1.0, within(0.001));
        }

        @Test
        @DisplayName("Should use score for incorrect MCQ answer")
        void shouldUseScoreForIncorrectMcq() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competency1);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.MCQ);
            TestAnswer answer = createAnswer(mockSession, question, null, 0.0, false);

            setupBatchLoaderMock(Map.of(competencyId1, competency1));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getScore()).isCloseTo(0.0, within(0.001));
        }

        @Test
        @DisplayName("Should return 0 for MCQ without score")
        void shouldReturnZeroForMcqWithoutScore() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competency1);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.MCQ);
            TestAnswer answer = createAnswer(mockSession, question, null, null, false);

            setupBatchLoaderMock(Map.of(competencyId1, competency1));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getScore()).isCloseTo(0.0, within(0.001));
        }

        @Test
        @DisplayName("Should handle MULTIPLE_CHOICE type same as MCQ")
        void shouldHandleMultipleChoiceType() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competency1);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.MULTIPLE_CHOICE);
            TestAnswer answer = createAnswer(mockSession, question, null, 1.0, false);

            setupBatchLoaderMock(Map.of(competencyId1, competency1));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getScore()).isCloseTo(1.0, within(0.001));
        }
    }

    @Nested
    @DisplayName("Competency Aggregation Tests")
    class CompetencyAggregationTests {

        @Test
        @DisplayName("Should aggregate multiple answers per competency")
        void shouldAggregateMultipleAnswersPerCompetency() {
            // Given: 3 Likert answers for same competency: 3, 4, 5
            // Normalized: 0.5, 0.75, 1.0 => Sum = 2.25, Average = 0.75
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competency1);

            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            AssessmentQuestion q2 = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            AssessmentQuestion q3 = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);

            TestAnswer a1 = createAnswer(mockSession, q1, 3, null, false);
            TestAnswer a2 = createAnswer(mockSession, q2, 4, null, false);
            TestAnswer a3 = createAnswer(mockSession, q3, 5, null, false);

            setupBatchLoaderMock(Map.of(competencyId1, competency1));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(a1, a2, a3));

            // Then
            assertThat(result.getCompetencyScores()).hasSize(1);
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getScore()).isCloseTo(2.25, within(0.001)); // Sum
            assertThat(score.getMaxScore()).isCloseTo(3.0, within(0.001)); // 3 questions
            assertThat(score.getPercentage()).isCloseTo(75.0, within(0.1)); // Average * 100
            assertThat(score.getQuestionsAnswered()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should aggregate answers across multiple competencies")
        void shouldAggregateAcrossMultipleCompetencies() {
            // Given: 2 competencies with different scores
            BehavioralIndicator indicator1 = createBehavioralIndicator(UUID.randomUUID(), competency1);
            BehavioralIndicator indicator2 = createBehavioralIndicator(UUID.randomUUID(), competency2);

            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), indicator1, QuestionType.LIKERT);
            AssessmentQuestion q2 = createQuestion(UUID.randomUUID(), indicator2, QuestionType.LIKERT);

            TestAnswer a1 = createAnswer(mockSession, q1, 5, null, false); // 1.0 normalized
            TestAnswer a2 = createAnswer(mockSession, q2, 3, null, false); // 0.5 normalized

            setupBatchLoaderMock(Map.of(competencyId1, competency1, competencyId2, competency2));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(a1, a2));

            // Then
            assertThat(result.getCompetencyScores()).hasSize(2);

            // Overall should be average of competency percentages: (100 + 50) / 2 = 75
            assertThat(result.getOverallPercentage()).isCloseTo(75.0, within(0.1));
        }

        @Test
        @DisplayName("Should set O*NET code in competency score DTO")
        void shouldSetOnetCodeInScoreDto() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competency1);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(mockSession, question, 4, null, false);

            setupBatchLoaderMock(Map.of(competencyId1, competency1));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getOnetCode()).isEqualTo("2.B.1.a");
        }

        @Test
        @DisplayName("Should handle unknown competency gracefully")
        void shouldHandleUnknownCompetencyGracefully() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competency1);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(mockSession, question, 4, null, false);

            setupBatchLoaderMockWithEmptyCache(competencyId1);

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            assertThat(result.getCompetencyScores()).hasSize(1);
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getCompetencyName()).isEqualTo("Unknown Competency");
            assertThat(score.getOnetCode()).isNull();
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle single answer correctly")
        void shouldHandleSingleAnswer() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competency1);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(mockSession, question, 4, null, false); // 0.75 normalized

            setupBatchLoaderMock(Map.of(competencyId1, competency1));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            assertThat(result.getCompetencyScores()).hasSize(1);
            assertThat(result.getOverallScore()).isCloseTo(0.75, within(0.001));
            assertThat(result.getOverallPercentage()).isCloseTo(75.0, within(0.1));
        }

        @Test
        @DisplayName("Should handle all maximum scores (100%)")
        void shouldHandleAllMaximumScores() {
            // Given: All Likert 5s
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competency1);

            List<TestAnswer> answers = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                AssessmentQuestion q = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
                answers.add(createAnswer(mockSession, q, 5, null, false));
            }

            setupBatchLoaderMock(Map.of(competencyId1, competency1));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, answers);

            // Then
            // overallScore is the average of rawCompetencyScores sums / competency count
            // For 5 questions with normalized 1.0 each: sum = 5.0, only 1 competency, so overallScore = 5.0
            assertThat(result.getOverallScore()).isCloseTo(5.0, within(0.001));
            assertThat(result.getOverallPercentage()).isCloseTo(100.0, within(0.1));
        }

        @Test
        @DisplayName("Should handle all minimum scores (0%)")
        void shouldHandleAllMinimumScores() {
            // Given: All Likert 1s
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competency1);

            List<TestAnswer> answers = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                AssessmentQuestion q = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
                answers.add(createAnswer(mockSession, q, 1, null, false));
            }

            setupBatchLoaderMock(Map.of(competencyId1, competency1));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, answers);

            // Then
            assertThat(result.getOverallScore()).isCloseTo(0.0, within(0.001));
            assertThat(result.getOverallPercentage()).isCloseTo(0.0, within(0.1));
        }

        @Test
        @DisplayName("Should filter out skipped answers from calculation")
        void shouldFilterSkippedAnswers() {
            // Given: 2 answered (Likert 4 and 5), 1 skipped
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competency1);

            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            AssessmentQuestion q2 = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            AssessmentQuestion q3 = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);

            TestAnswer a1 = createAnswer(mockSession, q1, 4, null, false); // 0.75
            TestAnswer a2 = createAnswer(mockSession, q2, 5, null, false); // 1.0
            TestAnswer a3 = createAnswer(mockSession, q3, null, null, true); // Skipped

            setupBatchLoaderMock(Map.of(competencyId1, competency1));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(a1, a2, a3));

            // Then
            assertThat(result.getCompetencyScores()).hasSize(1);
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getQuestionsAnswered()).isEqualTo(2); // Only 2 counted
            assertThat(score.getPercentage()).isCloseTo(87.5, within(0.1)); // (0.75+1.0)/2 * 100
        }

        @Test
        @DisplayName("Should handle mixed question types")
        void shouldHandleMixedQuestionTypes() {
            // Given: Mix of LIKERT, SJT, and MCQ
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competency1);

            AssessmentQuestion likertQ = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            AssessmentQuestion sjtQ = createQuestion(UUID.randomUUID(), indicator, QuestionType.SJT);
            AssessmentQuestion mcqQ = createQuestion(UUID.randomUUID(), indicator, QuestionType.MCQ);

            TestAnswer likertA = createAnswer(mockSession, likertQ, 5, null, false); // 1.0
            TestAnswer sjtA = createAnswer(mockSession, sjtQ, null, 0.8, false); // 0.8
            TestAnswer mcqA = createAnswer(mockSession, mcqQ, null, 1.0, false); // 1.0

            setupBatchLoaderMock(Map.of(competencyId1, competency1));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(likertA, sjtA, mcqA));

            // Then
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            // Sum: 1.0 + 0.8 + 1.0 = 2.8, Average: 2.8/3 = 0.933...
            assertThat(score.getPercentage()).isCloseTo(93.33, within(0.1));
        }
    }

    @Nested
    @DisplayName("Other Question Type Tests")
    class OtherQuestionTypeTests {

        @Test
        @DisplayName("Should handle CAPABILITY_ASSESSMENT with Likert value")
        void shouldHandleCapabilityAssessment() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competency1);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.CAPABILITY_ASSESSMENT);
            TestAnswer answer = createAnswer(mockSession, question, 4, null, false);

            setupBatchLoaderMock(Map.of(competencyId1, competency1));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getScore()).isCloseTo(0.75, within(0.001));
        }

        @Test
        @DisplayName("Should handle PEER_FEEDBACK with score fallback")
        void shouldHandlePeerFeedbackWithScoreFallback() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competency1);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.PEER_FEEDBACK);
            TestAnswer answer = createAnswer(mockSession, question, null, 0.9, false);

            setupBatchLoaderMock(Map.of(competencyId1, competency1));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getScore()).isCloseTo(0.9, within(0.001));
        }

        @Test
        @DisplayName("Should default to 0 for OPEN_TEXT without score")
        void shouldDefaultToZeroForOpenTextWithoutScore() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competency1);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.OPEN_TEXT);
            TestAnswer answer = createAnswer(mockSession, question, null, null, false);

            setupBatchLoaderMock(Map.of(competencyId1, competency1));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getScore()).isCloseTo(0.0, within(0.001));
        }
    }

    @Nested
    @DisplayName("Overall Score Calculation Tests")
    class OverallScoreCalculationTests {

        @Test
        @DisplayName("Should calculate overall percentage as average of competency percentages")
        void shouldCalculateOverallAsAverageOfCompetencies() {
            // Given: 3 competencies with percentages 80%, 60%, 100%
            UUID compId3 = UUID.randomUUID();
            Competency competency3 = createCompetency(compId3, "Problem Solving", "3.A.1");

            BehavioralIndicator ind1 = createBehavioralIndicator(UUID.randomUUID(), competency1);
            BehavioralIndicator ind2 = createBehavioralIndicator(UUID.randomUUID(), competency2);
            BehavioralIndicator ind3 = createBehavioralIndicator(UUID.randomUUID(), competency3);

            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), ind1, QuestionType.SJT);
            AssessmentQuestion q2 = createQuestion(UUID.randomUUID(), ind2, QuestionType.SJT);
            AssessmentQuestion q3 = createQuestion(UUID.randomUUID(), ind3, QuestionType.SJT);

            TestAnswer a1 = createAnswer(mockSession, q1, null, 0.8, false); // 80%
            TestAnswer a2 = createAnswer(mockSession, q2, null, 0.6, false); // 60%
            TestAnswer a3 = createAnswer(mockSession, q3, null, 1.0, false); // 100%

            setupBatchLoaderMock(Map.of(competencyId1, competency1, competencyId2, competency2, compId3, competency3));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(a1, a2, a3));

            // Then
            // Average: (80 + 60 + 100) / 3 = 80%
            assertThat(result.getOverallPercentage()).isCloseTo(80.0, within(0.1));
            assertThat(result.getCompetencyScores()).hasSize(3);
        }

        @Test
        @DisplayName("Should set goal to OVERVIEW in result")
        void shouldSetGoalToOverviewInResult() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competency1);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(mockSession, question, 3, null, false);

            setupBatchLoaderMock(Map.of(competencyId1, competency1));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            assertThat(result.getGoal()).isEqualTo(AssessmentGoal.OVERVIEW);
        }

        @Test
        @DisplayName("Should not set passed field (left to service layer)")
        void shouldNotSetPassedField() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competency1);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(mockSession, question, 5, null, false);

            setupBatchLoaderMock(Map.of(competencyId1, competency1));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            // Pass/fail is set by service layer based on template passing score
            assertThat(result.getPassed()).isNull();
        }
    }

    @Nested
    @DisplayName("Evidence Sufficiency Tests")
    class EvidenceSufficiencyTests {

        @Test
        @DisplayName("Should flag competency with insufficient evidence when questions below minimum")
        void shouldFlagInsufficientEvidence() {
            // Given: 2 answers for a competency (default min is 3)
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competency1);

            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            AssessmentQuestion q2 = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);

            TestAnswer a1 = createAnswer(mockSession, q1, 4, null, false);
            TestAnswer a2 = createAnswer(mockSession, q2, 5, null, false);

            setupBatchLoaderMock(Map.of(competencyId1, competency1));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(a1, a2));

            // Then
            assertThat(result.getCompetencyScores()).hasSize(1);
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getInsufficientEvidence()).isTrue();
            assertThat(score.getEvidenceNote()).contains("2 question(s)").contains("minimum 3");
        }

        @Test
        @DisplayName("Should not flag competency with sufficient evidence")
        void shouldNotFlagSufficientEvidence() {
            // Given: 3 answers for a competency (meets default min of 3)
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competency1);

            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            AssessmentQuestion q2 = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            AssessmentQuestion q3 = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);

            TestAnswer a1 = createAnswer(mockSession, q1, 3, null, false);
            TestAnswer a2 = createAnswer(mockSession, q2, 4, null, false);
            TestAnswer a3 = createAnswer(mockSession, q3, 5, null, false);

            setupBatchLoaderMock(Map.of(competencyId1, competency1));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(a1, a2, a3));

            // Then
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getInsufficientEvidence()).isNull();
            assertThat(score.getEvidenceNote()).isNull();
        }

        @Test
        @DisplayName("Should flag single-question competency as insufficient")
        void shouldFlagSingleQuestionCompetency() {
            // Given: 1 answer (well below default min of 3)
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competency1);
            AssessmentQuestion q = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer a = createAnswer(mockSession, q, 4, null, false);

            setupBatchLoaderMock(Map.of(competencyId1, competency1));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(a));

            // Then
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getInsufficientEvidence()).isTrue();
            assertThat(score.getEvidenceNote()).contains("1 question(s)");
        }

        @Test
        @DisplayName("Should respect custom minQuestionsPerCompetency config")
        void shouldRespectCustomMinQuestions() {
            // Given: Custom config with min=1
            config.getThresholds().getOverview().setMinQuestionsPerCompetency(1);

            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competency1);
            AssessmentQuestion q = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer a = createAnswer(mockSession, q, 4, null, false);

            setupBatchLoaderMock(Map.of(competencyId1, competency1));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(a));

            // Then: 1 question meets the min=1 threshold
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getInsufficientEvidence()).isNull();
        }
    }

    @Nested
    @DisplayName("Proficiency Label Tests")
    class ProficiencyLabelTests {

        @Test
        @DisplayName("Should assign proficiency labels to competency scores")
        void shouldAssignProficiencyLabelsToCompetencies() {
            // Given: Competency with 75% score (Advanced)
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competency1);

            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            AssessmentQuestion q2 = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            AssessmentQuestion q3 = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            AssessmentQuestion q4 = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);

            // Likert 3,4,4,5 => normalized 0.5,0.75,0.75,1.0 => avg 0.75 => 75%
            TestAnswer a1 = createAnswer(mockSession, q1, 3, null, false);
            TestAnswer a2 = createAnswer(mockSession, q2, 4, null, false);
            TestAnswer a3 = createAnswer(mockSession, q3, 4, null, false);
            TestAnswer a4 = createAnswer(mockSession, q4, 5, null, false);

            setupBatchLoaderMock(Map.of(competencyId1, competency1));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(a1, a2, a3, a4));

            // Then
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getProficiencyLabel()).isEqualTo("Advanced"); // 70-84% range
            assertThat(score.getPercentage()).isCloseTo(75.0, within(0.1));
        }

        @Test
        @DisplayName("Should assign proficiency labels to indicator scores")
        void shouldAssignProficiencyLabelsToIndicators() {
            // Given: Single indicator with 100% score
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competency1);
            AssessmentQuestion q = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer a = createAnswer(mockSession, q, 5, null, false); // 1.0 normalized = 100%

            setupBatchLoaderMock(Map.of(competencyId1, competency1));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(a));

            // Then
            CompetencyScoreDto compScore = result.getCompetencyScores().get(0);
            assertThat(compScore.getIndicatorScores()).isNotEmpty();
            IndicatorScoreDto indScore = compScore.getIndicatorScores().get(0);
            assertThat(indScore.getProficiencyLabel()).isEqualTo("Expert"); // 100% is Expert
        }

        @Test
        @DisplayName("Should assign Beginning label for low scores")
        void shouldAssignBeginningLabelForLowScores() {
            // Given: Competency with 0% score
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competency1);
            AssessmentQuestion q = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer a = createAnswer(mockSession, q, 1, null, false); // 0.0 normalized = 0%

            setupBatchLoaderMock(Map.of(competencyId1, competency1));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(a));

            // Then
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getProficiencyLabel()).isEqualTo("Beginning"); // 0-29% range
        }
    }

    @Nested
    @DisplayName("Profile Pattern Analysis Tests")
    class ProfilePatternTests {

        @Test
        @DisplayName("Should classify competencies into profile pattern categories")
        @SuppressWarnings("unchecked")
        void shouldClassifyCompetenciesIntoProfilePattern() {
            // Given: 2 competencies - one high (100%), one low (25%)
            // Overall average = (100 + 25) / 2 = 62.5%
            BehavioralIndicator ind1 = createBehavioralIndicator(UUID.randomUUID(), competency1);
            BehavioralIndicator ind2 = createBehavioralIndicator(UUID.randomUUID(), competency2);

            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), ind1, QuestionType.SJT);
            AssessmentQuestion q2 = createQuestion(UUID.randomUUID(), ind2, QuestionType.SJT);

            TestAnswer a1 = createAnswer(mockSession, q1, null, 1.0, false); // 100% => SIGNATURE_STRENGTH (>= 62.5+10 and >= 75)
            TestAnswer a2 = createAnswer(mockSession, q2, null, 0.25, false); // 25% => CRITICAL_GAP (< 30)

            setupBatchLoaderMock(Map.of(competencyId1, competency1, competencyId2, competency2));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(a1, a2));

            // Then
            assertThat(result.getExtendedMetrics()).isNotNull();
            Map<String, List<String>> profilePattern =
                    (Map<String, List<String>>) result.getExtendedMetrics().get("profilePattern");
            assertThat(profilePattern).isNotNull();

            // 100% >= 62.5+10=72.5 AND >= 75 => SIGNATURE_STRENGTH
            assertThat(profilePattern.getOrDefault("SIGNATURE_STRENGTH", List.of()))
                    .contains("Leadership");

            // 25% < 30 => CRITICAL_GAP
            assertThat(profilePattern.getOrDefault("CRITICAL_GAP", List.of()))
                    .contains("Communication");
        }

        @Test
        @DisplayName("Should produce extended metrics with profile pattern for empty scores")
        void shouldProduceExtendedMetricsForEmptyScores() {
            // Given: No answers
            List<TestAnswer> emptyAnswers = Collections.emptyList();

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, emptyAnswers);

            // Then: Extended metrics should still exist but with no categories
            assertThat(result.getExtendedMetrics()).isNotNull();
        }
    }
}
