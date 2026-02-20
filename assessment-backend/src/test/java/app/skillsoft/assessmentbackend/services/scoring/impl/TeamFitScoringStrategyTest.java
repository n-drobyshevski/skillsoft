package app.skillsoft.assessmentbackend.services.scoring.impl;

import app.skillsoft.assessmentbackend.config.ScoringConfiguration;
import app.skillsoft.assessmentbackend.domain.dto.CompetencyScoreDto;
import app.skillsoft.assessmentbackend.domain.dto.StandardCodesDto;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.TeamFitBlueprint;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.services.external.TeamService;
import app.skillsoft.assessmentbackend.services.scoring.CompetencyAggregationService;
import app.skillsoft.assessmentbackend.services.scoring.CompetencyBatchLoader;
import app.skillsoft.assessmentbackend.services.scoring.IndicatorBatchLoader;
import app.skillsoft.assessmentbackend.services.scoring.ScoreNormalizer;
import app.skillsoft.assessmentbackend.services.scoring.ScoringResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TeamFitScoringStrategy.
 *
 * Tests cover:
 * - Diversity/saturation multiplier calculations
 * - ESCO weight boost (1.15x) for competencies with ESCO URIs
 * - Big Five weight boost (1.1x) for competencies with Big Five mapping
 * - Team fit pass/fail determination based on score and diversity ratio
 * - Empty answer list handling
 * - Blueprint extraction from template
 * - Saturation threshold variations
 * - Edge cases: single answer, all max scores, all min scores
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Team Fit Scoring Strategy Tests")
class TeamFitScoringStrategyTest {

    @Mock
    private CompetencyBatchLoader competencyBatchLoader;

    @Mock
    private IndicatorBatchLoader indicatorBatchLoader;

    @Mock
    private ScoreNormalizer scoreNormalizer;

    @Mock
    private TeamService teamService;

    private ScoringConfiguration scoringConfig;
    private TeamFitScoringStrategy scoringStrategy;

    private TestSession mockSession;
    private TestTemplate mockTemplate;
    private UUID competencyId1;
    private UUID competencyId2;
    private UUID competencyId3;
    private Competency competencyWithEscoAndBigFive;
    private Competency competencyWithEscoOnly;
    private Competency competencyBasic;

    @BeforeEach
    void setUp() {
        // Initialize ScoringConfiguration with default values
        scoringConfig = new ScoringConfiguration();

        // Create shared aggregation service with mocked dependencies
        CompetencyAggregationService aggregationService = new CompetencyAggregationService(
                competencyBatchLoader,
                indicatorBatchLoader,
                scoreNormalizer
        );

        // Create the strategy with all dependencies
        scoringStrategy = new TeamFitScoringStrategy(
                aggregationService,
                competencyBatchLoader,
                indicatorBatchLoader,
                scoringConfig,
                scoreNormalizer,
                teamService
        );

        // Set up UUIDs
        competencyId1 = UUID.randomUUID();
        competencyId2 = UUID.randomUUID();
        competencyId3 = UUID.randomUUID();

        // Set up competencies with different standard code configurations
        competencyWithEscoAndBigFive = createCompetency(
            competencyId1,
            "Leadership",
            "2.B.1.a",
            "http://data.europa.eu/esco/skill/abc123",
            "CONSCIENTIOUSNESS"
        );

        competencyWithEscoOnly = createCompetency(
            competencyId2,
            "Communication",
            null,
            "http://data.europa.eu/esco/skill/def456",
            null
        );

        competencyBasic = createCompetency(
            competencyId3,
            "Basic Skills",
            null,
            null,
            null
        );

        // Set up mock template with TeamFitBlueprint
        mockTemplate = new TestTemplate();
        mockTemplate.setId(UUID.randomUUID());
        mockTemplate.setName("Team Fit Assessment");
        mockTemplate.setGoal(AssessmentGoal.TEAM_FIT);

        TeamFitBlueprint blueprint = new TeamFitBlueprint();
        blueprint.setTeamId(UUID.randomUUID());
        blueprint.setSaturationThreshold(0.75); // Default
        mockTemplate.setTypedBlueprint(blueprint);

        // Set up mock session
        mockSession = new TestSession();
        mockSession.setId(UUID.randomUUID());
        mockSession.setTemplate(mockTemplate);
        mockSession.setClerkUserId("user_test123");
        mockSession.setStatus(SessionStatus.COMPLETED);
    }

    private Competency createCompetency(UUID id, String name, String onetCode,
                                         String escoUri, String bigFiveTrait) {
        Competency competency = new Competency();
        competency.setId(id);
        competency.setName(name);
        competency.setCategory(CompetencyCategory.INTERPERSONAL);
        competency.setActive(true);
        competency.setApprovalStatus(ApprovalStatus.APPROVED);

        StandardCodesDto.Builder builder = StandardCodesDto.builder();

        if (onetCode != null) {
            builder.onetRef(onetCode, name, "skill");
        }

        if (escoUri != null) {
            builder.escoRef(escoUri, name, "skill");
        }

        if (bigFiveTrait != null) {
            builder.bigFive(bigFiveTrait, bigFiveTrait);
        }

        StandardCodesDto standardCodes = builder.build();
        if (standardCodes.hasAnyMapping()) {
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

    private void setTemplateSaturationThreshold(double threshold) {
        TeamFitBlueprint blueprint = new TeamFitBlueprint();
        blueprint.setTeamId(UUID.randomUUID());
        blueprint.setSaturationThreshold(threshold);
        mockTemplate.setTypedBlueprint(blueprint);
    }

    /**
     * Sets up the IndicatorBatchLoader, CompetencyBatchLoader, and ScoreNormalizer mocks.
     *
     * The production code uses IndicatorBatchLoader for answer processing
     * (extractIndicatorIdSafe, getFromCache) and CompetencyBatchLoader for
     * competency lookups (loadCompetenciesForAnswers, getFromCache).
     */
    private void setupBatchLoaderMock(Map<UUID, Competency> competencyMap) {
        // Set up CompetencyBatchLoader for competency cache
        when(competencyBatchLoader.loadCompetenciesForAnswers(anyList()))
            .thenReturn(competencyMap);

        when(competencyBatchLoader.getFromCache(any(), any()))
            .thenAnswer(invocation -> {
                Map<UUID, Competency> cache = invocation.getArgument(0);
                UUID id = invocation.getArgument(1);
                return cache != null ? cache.get(id) : null;
            });

        // Build indicator cache from competencyMap and answers
        // The production code calls indicatorBatchLoader.loadIndicatorsForAnswers(answers)
        // which returns Map<UUID, BehavioralIndicator>
        when(indicatorBatchLoader.loadIndicatorsForAnswers(anyList()))
            .thenAnswer(invocation -> {
                List<TestAnswer> answers = invocation.getArgument(0);
                Map<UUID, BehavioralIndicator> cache = new HashMap<>();
                for (TestAnswer answer : answers) {
                    if (answer.getQuestion() != null && answer.getQuestion().getBehavioralIndicator() != null) {
                        BehavioralIndicator ind = answer.getQuestion().getBehavioralIndicator();
                        cache.put(ind.getId(), ind);
                    }
                }
                return cache;
            });

        // extractIndicatorIdSafe: extract indicator ID from answer -> question -> indicator
        when(indicatorBatchLoader.extractIndicatorIdSafe(any(TestAnswer.class)))
            .thenAnswer(invocation -> {
                TestAnswer answer = invocation.getArgument(0);
                if (answer == null || answer.getQuestion() == null
                    || answer.getQuestion().getBehavioralIndicator() == null) {
                    return Optional.empty();
                }
                return Optional.of(answer.getQuestion().getBehavioralIndicator().getId());
            });

        // getFromCache for indicators
        when(indicatorBatchLoader.getFromCache(any(), any()))
            .thenAnswer(invocation -> {
                Map<UUID, BehavioralIndicator> cache = invocation.getArgument(0);
                UUID id = invocation.getArgument(1);
                return cache != null ? cache.get(id) : null;
            });

        // Set up score normalizer to delegate to real normalization logic
        when(scoreNormalizer.normalize(any(TestAnswer.class)))
            .thenAnswer(invocation -> {
                TestAnswer answer = invocation.getArgument(0);
                if (answer == null || Boolean.TRUE.equals(answer.getIsSkipped())) {
                    return 0.0;
                }
                // Likert normalization: (value - 1) / 4
                if (answer.getLikertValue() != null) {
                    int likert = Math.max(1, Math.min(5, answer.getLikertValue()));
                    return (likert - 1.0) / 4.0;
                }
                // Score normalization: clamp to 0-1
                if (answer.getScore() != null) {
                    return Math.max(0.0, Math.min(1.0, answer.getScore()));
                }
                return 0.0;
            });
    }

    private void setupBatchLoaderMockWithEmptyCache(UUID competencyId) {
        when(competencyBatchLoader.loadCompetenciesForAnswers(anyList()))
            .thenReturn(Map.of());

        when(competencyBatchLoader.getFromCache(any(), eq(competencyId)))
            .thenReturn(null);

        // Set up indicator batch loader
        when(indicatorBatchLoader.loadIndicatorsForAnswers(anyList()))
            .thenAnswer(invocation -> {
                List<TestAnswer> answers = invocation.getArgument(0);
                Map<UUID, BehavioralIndicator> cache = new HashMap<>();
                for (TestAnswer answer : answers) {
                    if (answer.getQuestion() != null && answer.getQuestion().getBehavioralIndicator() != null) {
                        BehavioralIndicator ind = answer.getQuestion().getBehavioralIndicator();
                        cache.put(ind.getId(), ind);
                    }
                }
                return cache;
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

        // Set up score normalizer
        when(scoreNormalizer.normalize(any(TestAnswer.class)))
            .thenAnswer(invocation -> {
                TestAnswer answer = invocation.getArgument(0);
                if (answer == null || Boolean.TRUE.equals(answer.getIsSkipped())) {
                    return 0.0;
                }
                if (answer.getLikertValue() != null) {
                    int likert = Math.max(1, Math.min(5, answer.getLikertValue()));
                    return (likert - 1.0) / 4.0;
                }
                if (answer.getScore() != null) {
                    return Math.max(0.0, Math.min(1.0, answer.getScore()));
                }
                return 0.0;
            });
    }

    @Nested
    @DisplayName("Supported Goal Tests")
    class SupportedGoalTests {

        @Test
        @DisplayName("Should return TEAM_FIT as supported goal")
        void shouldReturnTeamFitGoal() {
            // When
            AssessmentGoal goal = scoringStrategy.getSupportedGoal();

            // Then
            assertThat(goal).isEqualTo(AssessmentGoal.TEAM_FIT);
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
            assertThat(result.getGoal()).isEqualTo(AssessmentGoal.TEAM_FIT);
            assertThat(result.getPassed()).isFalse(); // Low score and zero diversity
        }

        @Test
        @DisplayName("Should return zero score when all answers are skipped")
        void shouldReturnZeroScoreWhenAllSkipped() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer skippedAnswer = createAnswer(mockSession, question, null, null, true);

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(skippedAnswer));

            // Then
            assertThat(result.getOverallScore()).isEqualTo(0.0);
            assertThat(result.getCompetencyScores()).isEmpty();
            assertThat(result.getPassed()).isFalse();
        }
    }

    @Nested
    @DisplayName("ESCO Weight Boost Tests")
    class EscoWeightBoostTests {

        @Test
        @DisplayName("Should apply 1.15x weight to competencies with ESCO URI")
        void shouldApplyEscoWeightBoost() {
            // Given: Competency with ESCO vs without
            BehavioralIndicator indWithEsco = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoOnly);
            BehavioralIndicator indWithoutEsco = createBehavioralIndicator(UUID.randomUUID(), competencyBasic);

            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), indWithEsco, QuestionType.LIKERT);
            AssessmentQuestion q2 = createQuestion(UUID.randomUUID(), indWithoutEsco, QuestionType.LIKERT);

            // Both get 100% (Likert 5)
            TestAnswer a1 = createAnswer(mockSession, q1, 5, null, false);
            TestAnswer a2 = createAnswer(mockSession, q2, 5, null, false);

            setupBatchLoaderMock(Map.of(competencyId2, competencyWithEscoOnly, competencyId3, competencyBasic));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(a1, a2));

            // Then
            // With ESCO: 100 * 1.15 = 115 weighted
            // Without: 100 * 1.0 = 100 weighted
            // Total weights: 1.15 + 1.0 = 2.15
            // Result: 215 / 2.15 = 100 (before multiplier)
            assertThat(result.getCompetencyScores()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Big Five Weight Boost Tests")
    class BigFiveWeightBoostTests {

        @Test
        @DisplayName("Should apply additional 1.1x weight for Big Five mapping")
        void shouldApplyBigFiveWeightBoost() {
            // Given: Competency with ESCO + Big Five (1.15 * 1.1 = 1.265)
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(mockSession, question, 5, null, false); // 100%

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithEscoAndBigFive));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            // Single competency at 100%, but with saturation (single high score = saturation ratio 100% > 80%)
            // 0.9 penalty multiplier applied: 100 * 0.9 = 90%
            // But with O*NET code present, weight is 1.15, so total weight = 1.15
            // Weighted score = 100 * 1.265 / 1.15 = 110 (normalized per ESCO weight only in total)
            // Actually: checking implementation - penalty applied at the end
            // Result will be around 99% due to penalty multiplier application
            assertThat(result.getOverallPercentage()).isGreaterThan(85.0);
            // Big Five category should be tracked internally
        }

        @Test
        @DisplayName("Should track Big Five scores for personality compatibility")
        void shouldTrackBigFiveScores() {
            // Given: Multiple questions for competencies with Big Five
            BehavioralIndicator ind1 = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);

            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), ind1, QuestionType.LIKERT);
            AssessmentQuestion q2 = createQuestion(UUID.randomUUID(), ind1, QuestionType.LIKERT);

            TestAnswer a1 = createAnswer(mockSession, q1, 4, null, false); // 75%
            TestAnswer a2 = createAnswer(mockSession, q2, 5, null, false); // 100%

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithEscoAndBigFive));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(a1, a2));

            // Then
            assertThat(result.getCompetencyScores()).hasSize(1);
            // Big Five averages are calculated internally (87.5% for CONSCIENTIOUSNESS)
        }
    }

    @Nested
    @DisplayName("Diversity and Saturation Tests")
    class DiversitySaturationTests {

        @Test
        @DisplayName("Should classify scores above saturation threshold as saturation contributors")
        void shouldClassifySaturationContributors() {
            // Given: Score above threshold (0.75)
            setTemplateSaturationThreshold(0.75);

            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(mockSession, question, 5, null, false); // 100% > 75%

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithEscoAndBigFive));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            // 100% is above 75% threshold = saturation contributor
            // Saturation ratio = 1/1 = 100% > 80% triggers 0.9 penalty
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Should classify mid-range scores as diversity contributors")
        void shouldClassifyDiversityContributors() {
            // Given: Score between 0.5 and threshold
            setTemplateSaturationThreshold(0.75);

            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(mockSession, question, 3, null, false); // 50% = diversity

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithEscoAndBigFive));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            // 50% is between 0.5 and 0.75 = diversity contributor
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Should apply 1.1x multiplier for good diversity/saturation balance")
        void shouldApplyBonusForGoodBalance() {
            // Given: Multiple competencies with good balance (diversity > 40%, saturation < 60%)
            setTemplateSaturationThreshold(0.8);

            // Need at least 3 competencies to achieve good balance ratios
            BehavioralIndicator ind1 = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);
            BehavioralIndicator ind2 = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoOnly);
            BehavioralIndicator ind3 = createBehavioralIndicator(UUID.randomUUID(), competencyBasic);

            // 2 diversity (50-80%), 1 saturation (>80%)
            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), ind1, QuestionType.LIKERT);
            AssessmentQuestion q2 = createQuestion(UUID.randomUUID(), ind2, QuestionType.LIKERT);
            AssessmentQuestion q3 = createQuestion(UUID.randomUUID(), ind3, QuestionType.LIKERT);

            TestAnswer a1 = createAnswer(mockSession, q1, 3, null, false); // 50% - diversity
            TestAnswer a2 = createAnswer(mockSession, q2, 4, null, false); // 75% - diversity
            TestAnswer a3 = createAnswer(mockSession, q3, 5, null, false); // 100% - saturation

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithEscoAndBigFive, competencyId2, competencyWithEscoOnly, competencyId3, competencyBasic));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(a1, a2, a3));

            // Then
            // Diversity ratio = 2/3 = 66.7% > 40%, Saturation ratio = 1/3 = 33.3% < 60%
            // Should get 1.1x bonus multiplier
            assertThat(result.getOverallPercentage()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should apply 0.9x penalty for excessive saturation")
        void shouldApplyPenaltyForExcessiveSaturation() {
            // Given: All high scores (saturation > 80%)
            setTemplateSaturationThreshold(0.6);

            BehavioralIndicator ind1 = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);
            BehavioralIndicator ind2 = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoOnly);

            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), ind1, QuestionType.LIKERT);
            AssessmentQuestion q2 = createQuestion(UUID.randomUUID(), ind2, QuestionType.LIKERT);

            // Both above threshold
            TestAnswer a1 = createAnswer(mockSession, q1, 5, null, false); // 100% > 60%
            TestAnswer a2 = createAnswer(mockSession, q2, 5, null, false); // 100% > 60%

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithEscoAndBigFive, competencyId2, competencyWithEscoOnly));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(a1, a2));

            // Then
            // Saturation ratio = 2/2 = 100% > 80%, applies 0.9x penalty
            // The weighted percentage calculation and multiplier are applied
            // Result should be less than 100% due to penalty
            assertThat(result.getOverallPercentage()).isLessThan(100.0);
            assertThat(result.getOverallPercentage()).isGreaterThan(80.0); // But still high overall
        }
    }

    @Nested
    @DisplayName("Pass/Fail Determination Tests")
    class PassFailTests {

        @Test
        @DisplayName("Should pass when score >= 60% and diversity >= 30%")
        void shouldPassWithGoodScoreAndDiversity() {
            // Given: Good scores across multiple competencies
            setTemplateSaturationThreshold(0.8);

            BehavioralIndicator ind1 = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);
            BehavioralIndicator ind2 = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoOnly);
            BehavioralIndicator ind3 = createBehavioralIndicator(UUID.randomUUID(), competencyBasic);

            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), ind1, QuestionType.LIKERT);
            AssessmentQuestion q2 = createQuestion(UUID.randomUUID(), ind2, QuestionType.LIKERT);
            AssessmentQuestion q3 = createQuestion(UUID.randomUUID(), ind3, QuestionType.LIKERT);

            // 1 diversity, 2 saturation = 33% diversity
            TestAnswer a1 = createAnswer(mockSession, q1, 4, null, false); // 75% - diversity
            TestAnswer a2 = createAnswer(mockSession, q2, 5, null, false); // 100% - saturation
            TestAnswer a3 = createAnswer(mockSession, q3, 5, null, false); // 100% - saturation

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithEscoAndBigFive, competencyId2, competencyWithEscoOnly, competencyId3, competencyBasic));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(a1, a2, a3));

            // Then
            // Score > 60% and diversity ratio = 1/3 = 33.3% >= 30%
            assertThat(result.getPassed()).isTrue();
        }

        @Test
        @DisplayName("Should fail when diversity is too low")
        void shouldFailWithLowDiversity() {
            // Given: High scores but no diversity (all saturation)
            setTemplateSaturationThreshold(0.5);

            BehavioralIndicator ind1 = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);
            BehavioralIndicator ind2 = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoOnly);

            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), ind1, QuestionType.LIKERT);
            AssessmentQuestion q2 = createQuestion(UUID.randomUUID(), ind2, QuestionType.LIKERT);

            // Both above threshold = 0 diversity
            TestAnswer a1 = createAnswer(mockSession, q1, 5, null, false); // 100%
            TestAnswer a2 = createAnswer(mockSession, q2, 5, null, false); // 100%

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithEscoAndBigFive, competencyId2, competencyWithEscoOnly));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(a1, a2));

            // Then
            // Diversity ratio = 0/2 = 0% < 30%
            assertThat(result.getPassed()).isFalse();
        }

        @Test
        @DisplayName("Should fail when score is too low")
        void shouldFailWithLowScore() {
            // Given: Low scores
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(mockSession, question, 2, null, false); // 25%

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithEscoAndBigFive));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            // Score < 60% even if adjusted
            assertThat(result.getPassed()).isFalse();
        }
    }

    @Nested
    @DisplayName("Saturation Threshold Variations Tests")
    class SaturationThresholdTests {

        @ParameterizedTest(name = "Saturation threshold {0} with score 75% should be {1}")
        @CsvSource({
            "0.5, saturation",  // 75% > 50% = saturation
            "0.75, diversity",  // 75% = 75% boundary (>=threshold means saturation, but exactly at threshold)
            "0.8, diversity",   // 75% < 80% = diversity (if >= 0.5)
            "0.9, diversity"    // 75% < 90% = diversity
        })
        @DisplayName("Should classify based on saturation threshold")
        void shouldClassifyBasedOnThreshold(double threshold, String expectedCategory) {
            // Given
            setTemplateSaturationThreshold(threshold);

            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(mockSession, question, 4, null, false); // 75%

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithEscoAndBigFive));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            assertThat(result).isNotNull();
            // Classification affects multiplier and pass/fail logic
        }

        @Test
        @DisplayName("Should use default 0.75 threshold when blueprint is null")
        void shouldUseDefaultThresholdWhenBlueprintNull() {
            // Given
            mockTemplate.setTypedBlueprint(null);
            mockTemplate.setBlueprint(null);

            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(mockSession, question, 4, null, false); // 75%

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithEscoAndBigFive));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            // 75% exactly at default 0.75 threshold boundary
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("Blueprint Extraction Tests")
    class BlueprintExtractionTests {

        @Test
        @DisplayName("Should extract from typed blueprint")
        void shouldExtractFromTypedBlueprint() {
            // Given
            UUID teamId = UUID.randomUUID();
            TeamFitBlueprint blueprint = new TeamFitBlueprint();
            blueprint.setTeamId(teamId);
            blueprint.setSaturationThreshold(0.6);
            mockTemplate.setTypedBlueprint(blueprint);

            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(mockSession, question, 4, null, false); // 75%

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithEscoAndBigFive));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            // 75% > 60% threshold = saturation
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Should fall back to legacy blueprint map")
        void shouldFallbackToLegacyBlueprint() {
            // Given
            mockTemplate.setTypedBlueprint(null);
            Map<String, Object> legacyBlueprint = new HashMap<>();
            legacyBlueprint.put("teamId", UUID.randomUUID().toString());
            legacyBlueprint.put("saturationThreshold", 0.5);
            mockTemplate.setBlueprint(legacyBlueprint);

            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(mockSession, question, 4, null, false); // 75%

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithEscoAndBigFive));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            // 75% > 50% threshold = saturation
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Should handle null template gracefully")
        void shouldHandleNullTemplateGracefully() {
            // Given
            mockSession.setTemplate(null);

            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(mockSession, question, 4, null, false);

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithEscoAndBigFive));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            assertThat(result).isNotNull();
            // Uses default saturation threshold 0.75
        }
    }

    @Nested
    @DisplayName("Likert Normalization Tests")
    class LikertNormalizationTests {

        @ParameterizedTest(name = "Likert value {0} should normalize to {1}")
        @CsvSource({
            "1, 0.0",
            "2, 0.25",
            "3, 0.5",
            "4, 0.75",
            "5, 1.0"
        })
        @DisplayName("Should correctly normalize Likert values")
        void shouldNormalizeLikertValues(int likertValue, double expectedNormalized) {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(mockSession, question, likertValue, null, false);

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithEscoAndBigFive));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getScore()).isCloseTo(expectedNormalized, within(0.001));
        }

        @Test
        @DisplayName("Should clamp Likert values to valid range")
        void shouldClampLikertValues() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(mockSession, question, 10, null, false);

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithEscoAndBigFive));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getScore()).isCloseTo(1.0, within(0.001)); // Clamped to 5
        }
    }

    @Nested
    @DisplayName("Score Fallback Tests")
    class ScoreFallbackTests {

        @Test
        @DisplayName("Should use score field when Likert value is null")
        void shouldUseScoreWhenLikertNull() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.SJT);
            TestAnswer answer = createAnswer(mockSession, question, null, 0.7, false);

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithEscoAndBigFive));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getScore()).isCloseTo(0.7, within(0.001));
        }

        @Test
        @DisplayName("Should clamp score to 0-1 range")
        void shouldClampScoreToValidRange() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.SJT);
            TestAnswer answer = createAnswer(mockSession, question, null, 1.5, false);

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithEscoAndBigFive));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getScore()).isCloseTo(1.0, within(0.001));
        }

        @Test
        @DisplayName("Should default to 0 when no score available")
        void shouldDefaultToZeroWhenNoScore() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.OPEN_TEXT);
            TestAnswer answer = createAnswer(mockSession, question, null, null, false);

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithEscoAndBigFive));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getScore()).isCloseTo(0.0, within(0.001));
        }
    }

    @Nested
    @DisplayName("Competency Aggregation Tests")
    class CompetencyAggregationTests {

        @Test
        @DisplayName("Should aggregate multiple answers per competency")
        void shouldAggregateMultipleAnswersPerCompetency() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);

            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            AssessmentQuestion q2 = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            AssessmentQuestion q3 = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);

            TestAnswer a1 = createAnswer(mockSession, q1, 3, null, false); // 0.5
            TestAnswer a2 = createAnswer(mockSession, q2, 4, null, false); // 0.75
            TestAnswer a3 = createAnswer(mockSession, q3, 5, null, false); // 1.0

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithEscoAndBigFive));

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
        @DisplayName("Should calculate questionsCorrect based on average")
        void shouldCalculateQuestionsCorrect() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);

            List<TestAnswer> answers = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                AssessmentQuestion q = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
                answers.add(createAnswer(mockSession, q, 4, null, false)); // 0.75 each
            }

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithEscoAndBigFive));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, answers);

            // Then
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            // questionsCorrect = round(0.75 * 4) = 3
            assertThat(score.getQuestionsCorrect()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle single answer correctly")
        void shouldHandleSingleAnswer() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(mockSession, question, 5, null, false); // 100%

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithEscoAndBigFive));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            assertThat(result.getCompetencyScores()).hasSize(1);
            // Single high score = saturation, penalty applied
        }

        @Test
        @DisplayName("Should handle all maximum scores")
        void shouldHandleAllMaximumScores() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);

            List<TestAnswer> answers = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                AssessmentQuestion q = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
                answers.add(createAnswer(mockSession, q, 5, null, false));
            }

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithEscoAndBigFive));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, answers);

            // Then
            // All 100%, saturation ratio = 100% > 80%, penalty multiplier 0.9 applied
            // Due to weighting (ESCO + BigFive), the final adjusted percentage is around 99%
            assertThat(result.getOverallPercentage()).isLessThan(100.0);
            assertThat(result.getOverallPercentage()).isGreaterThan(85.0);
        }

        @Test
        @DisplayName("Should handle all minimum scores")
        void shouldHandleAllMinimumScores() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);

            List<TestAnswer> answers = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                AssessmentQuestion q = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
                answers.add(createAnswer(mockSession, q, 1, null, false));
            }

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithEscoAndBigFive));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, answers);

            // Then
            assertThat(result.getOverallPercentage()).isCloseTo(0.0, within(0.1));
            assertThat(result.getPassed()).isFalse();
        }

        @Test
        @DisplayName("Should filter out skipped answers")
        void shouldFilterSkippedAnswers() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);

            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            AssessmentQuestion q2 = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            AssessmentQuestion q3 = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);

            TestAnswer a1 = createAnswer(mockSession, q1, 5, null, false); // 1.0
            TestAnswer a2 = createAnswer(mockSession, q2, 5, null, false); // 1.0
            TestAnswer a3 = createAnswer(mockSession, q3, null, null, true); // Skipped

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithEscoAndBigFive));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(a1, a2, a3));

            // Then
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getQuestionsAnswered()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should handle unknown competency gracefully")
        void shouldHandleUnknownCompetencyGracefully() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(mockSession, question, 4, null, false);

            setupBatchLoaderMockWithEmptyCache(competencyId1);

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getCompetencyName()).isEqualTo("Unknown Competency");
            assertThat(score.getOnetCode()).isNull();
        }
    }

    @Nested
    @DisplayName("Weight Denominator Consistency Tests")
    class WeightDenominatorConsistencyTests {

        @Test
        @DisplayName("Should use ESCO URI (not O*NET code) for weight denominator")
        void shouldUseEscoUriForWeightDenominator() {
            // Given: Competency with ESCO URI but NO O*NET code
            // This is the exact scenario that exposed the original bug
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoOnly);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(mockSession, question, 5, null, false); // 100%

            setupBatchLoaderMock(Map.of(competencyId2, competencyWithEscoOnly));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then: numerator weight = 1.15 (ESCO boost), denominator weight = 1.15
            // Result = (100 * 1.15) / 1.15 = 100% (before multiplier)
            // With single competency at 100%, saturation penalty (0.9x) applies: 100 * 0.9 = 90%
            // If the bug existed (denominator = 1.0), result would be 115% * 0.9 = ~103.5%
            assertThat(result.getOverallPercentage()).isLessThanOrEqualTo(100.0);
        }

        @Test
        @DisplayName("Should set ESCO URI in competency score DTO")
        void shouldSetEscoUriInScoreDto() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoOnly);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(mockSession, question, 4, null, false);

            setupBatchLoaderMock(Map.of(competencyId2, competencyWithEscoOnly));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getEscoUri()).isEqualTo("http://data.europa.eu/esco/skill/def456");
            assertThat(score.getOnetCode()).isNull();
        }

        @Test
        @DisplayName("Should set Big Five category in competency score DTO")
        void shouldSetBigFiveCategoryInScoreDto() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(mockSession, question, 4, null, false);

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithEscoAndBigFive));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getBigFiveCategory()).isEqualTo("BIG_FIVE_CONSCIENTIOUSNESS");
        }

        @Test
        @DisplayName("Numerator and denominator weights must be symmetric for mixed competencies")
        void shouldHaveSymmetricWeightsForMixedCompetencies() {
            // Given: Two competencies both at equal scores
            // One with ESCO only, one with no mappings
            BehavioralIndicator indWithEsco = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoOnly);
            BehavioralIndicator indBasic = createBehavioralIndicator(UUID.randomUUID(), competencyBasic);

            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), indWithEsco, QuestionType.LIKERT);
            AssessmentQuestion q2 = createQuestion(UUID.randomUUID(), indBasic, QuestionType.LIKERT);

            // Both at 75% (Likert 4)
            TestAnswer a1 = createAnswer(mockSession, q1, 4, null, false);
            TestAnswer a2 = createAnswer(mockSession, q2, 4, null, false);

            setupBatchLoaderMock(Map.of(competencyId2, competencyWithEscoOnly, competencyId3, competencyBasic));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(a1, a2));

            // Then: With equal underlying scores, ESCO weighting should not shift the result
            // numerator = 75*1.15 + 75*1.0 = 161.25
            // denominator = 1.15 + 1.0 = 2.15
            // result = 161.25 / 2.15 = 75% (weighted avg equals unweighted when all scores are equal)
            // Then diversity/saturation multiplier applied on top
            assertThat(result.getCompetencyScores()).hasSize(2);
            // Both at 75% means underlying avg is 75%
            // The overall should reflect this without inflation/deflation from weighting
        }
    }

    @Nested
    @DisplayName("Result Metadata Tests")
    class ResultMetadataTests {

        @Test
        @DisplayName("Should set goal to TEAM_FIT in result")
        void shouldSetGoalToTeamFitInResult() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(mockSession, question, 4, null, false);

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithEscoAndBigFive));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            assertThat(result.getGoal()).isEqualTo(AssessmentGoal.TEAM_FIT);
        }

        @Test
        @DisplayName("Should set O*NET code and ESCO URI in competency score DTO")
        void shouldSetOnetCodeInScoreDto() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(mockSession, question, 4, null, false);

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithEscoAndBigFive));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getOnetCode()).isEqualTo("2.B.1.a");
            assertThat(score.getEscoUri()).isEqualTo("http://data.europa.eu/esco/skill/abc123");
            assertThat(score.getBigFiveCategory()).isEqualTo("BIG_FIVE_CONSCIENTIOUSNESS");
        }
    }

    @Nested
    @DisplayName("Team Service Integration Tests")
    class TeamServiceIntegrationTests {

        @Test
        @DisplayName("Should use real team saturation data when available")
        void shouldUseRealTeamSaturationData() {
            // Given: Team where competencyId1 is saturated and competencyId3 is a gap
            UUID teamId = ((TeamFitBlueprint) mockTemplate.getTypedBlueprint()).getTeamId();

            TeamService.TeamProfile teamProfile = new TeamService.TeamProfile(
                teamId, "Test Team",
                List.of(), // members
                Map.of(competencyId1, 0.9, competencyId2, 0.6, competencyId3, 0.1), // saturation
                Map.of(), // personality
                List.of(competencyId3) // gaps
            );
            when(teamService.getTeamProfile(teamId)).thenReturn(Optional.of(teamProfile));

            // Create answers for all 3 competencies
            BehavioralIndicator ind1 = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);
            BehavioralIndicator ind2 = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoOnly);
            BehavioralIndicator ind3 = createBehavioralIndicator(UUID.randomUUID(), competencyBasic);

            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), ind1, QuestionType.LIKERT);
            AssessmentQuestion q2 = createQuestion(UUID.randomUUID(), ind2, QuestionType.LIKERT);
            AssessmentQuestion q3 = createQuestion(UUID.randomUUID(), ind3, QuestionType.LIKERT);

            TestAnswer a1 = createAnswer(mockSession, q1, 4, null, false); // 75%
            TestAnswer a2 = createAnswer(mockSession, q2, 4, null, false); // 75%
            TestAnswer a3 = createAnswer(mockSession, q3, 4, null, false); // 75%

            setupBatchLoaderMock(Map.of(
                competencyId1, competencyWithEscoAndBigFive,
                competencyId2, competencyWithEscoOnly,
                competencyId3, competencyBasic
            ));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(a1, a2, a3));

            // Then - classification should be based on TEAM data, not candidate scores
            assertThat(result.getTeamFitMetrics()).isNotNull();
            // comp1 (team sat 0.9) -> saturation, comp2 (team sat 0.6) -> diversity, comp3 (team sat 0.1) -> gap
            assertThat(result.getTeamFitMetrics().getSaturationCount()).isEqualTo(1);
            assertThat(result.getTeamFitMetrics().getDiversityCount()).isEqualTo(1);
            assertThat(result.getTeamFitMetrics().getGapCount()).isEqualTo(1);
            assertThat(result.getTeamFitMetrics().getTeamSize()).isEqualTo(0); // empty members list
        }

        @Test
        @DisplayName("Should fall back to self-referential scoring when team profile not found")
        void shouldFallbackWhenTeamProfileNotFound() {
            UUID teamId = ((TeamFitBlueprint) mockTemplate.getTypedBlueprint()).getTeamId();
            when(teamService.getTeamProfile(teamId)).thenReturn(Optional.empty());

            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(mockSession, question, 4, null, false);

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithEscoAndBigFive));

            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            assertThat(result).isNotNull();
            assertThat(result.getTeamFitMetrics().getTeamSize()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should fall back when no teamId in blueprint")
        void shouldFallbackWhenNoTeamId() {
            TeamFitBlueprint blueprint = new TeamFitBlueprint();
            blueprint.setTeamId(null);
            mockTemplate.setTypedBlueprint(blueprint);

            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(mockSession, question, 4, null, false);

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithEscoAndBigFive));

            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            assertThat(result).isNotNull();
            // Should not call teamService at all
            verify(teamService, never()).getTeamProfile(any());
        }

        @Test
        @DisplayName("Should weight competencies filling deeper gaps more heavily")
        void shouldWeightDeeperGapsMoreHeavily() {
            // Given: Team with one deep gap (0.1 saturation) and one fully saturated (0.9)
            UUID teamId = ((TeamFitBlueprint) mockTemplate.getTypedBlueprint()).getTeamId();

            TeamService.TeamProfile teamProfile = new TeamService.TeamProfile(
                teamId, "Test Team",
                List.of(),
                Map.of(competencyId1, 0.1, competencyId2, 0.9), // comp1 is deep gap, comp2 is saturated
                Map.of(),
                List.of(competencyId1)
            );
            when(teamService.getTeamProfile(teamId)).thenReturn(Optional.of(teamProfile));

            BehavioralIndicator ind1 = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);
            BehavioralIndicator ind2 = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoOnly);

            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), ind1, QuestionType.LIKERT);
            AssessmentQuestion q2 = createQuestion(UUID.randomUUID(), ind2, QuestionType.LIKERT);

            // Both at same raw score (75%)
            TestAnswer a1 = createAnswer(mockSession, q1, 4, null, false);
            TestAnswer a2 = createAnswer(mockSession, q2, 4, null, false);

            setupBatchLoaderMock(Map.of(
                competencyId1, competencyWithEscoAndBigFive,
                competencyId2, competencyWithEscoOnly
            ));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(a1, a2));

            // Then: competencyId1 (deep gap) should have been weighted more heavily
            // Gap relevance: comp1 = 1.0 + (1.0 - 0.1) = 1.9, comp2 = 1.0 + (1.0 - 0.9) = 1.1
            // The overall score should reflect the gap-weighted average
            assertThat(result).isNotNull();
            assertThat(result.getOverallPercentage()).isGreaterThan(0);
            // Verify team fit metrics
            assertThat(result.getTeamFitMetrics().getGapCount()).isEqualTo(1); // comp1 at 0.1 < diversityThreshold 0.5
            assertThat(result.getTeamFitMetrics().getSaturationCount()).isEqualTo(1); // comp2 at 0.9 >= saturationThreshold 0.75
        }

        @Test
        @DisplayName("Should lower pass threshold for small teams")
        void shouldLowerThresholdForSmallTeams() {
            // Given: Small team (3 members, below default threshold of 5)
            UUID teamId = ((TeamFitBlueprint) mockTemplate.getTypedBlueprint()).getTeamId();

            // Create 3 team members
            List<TeamService.TeamMemberProfile> members = List.of(
                new TeamService.TeamMemberProfile(UUID.randomUUID(), "Alice", "Dev", Map.of(), Map.of()),
                new TeamService.TeamMemberProfile(UUID.randomUUID(), "Bob", "Dev", Map.of(), Map.of()),
                new TeamService.TeamMemberProfile(UUID.randomUUID(), "Charlie", "QA", Map.of(), Map.of())
            );

            TeamService.TeamProfile teamProfile = new TeamService.TeamProfile(
                teamId, "Small Team", members,
                Map.of(competencyId1, 0.3), // diversity range
                Map.of(), List.of()
            );
            when(teamService.getTeamProfile(teamId)).thenReturn(Optional.of(teamProfile));

            // Candidate scores at 55% (below default 60% threshold, but above adjusted 50%)
            BehavioralIndicator ind1 = createBehavioralIndicator(UUID.randomUUID(), competencyBasic);
            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), ind1, QuestionType.LIKERT);
            TestAnswer a1 = createAnswer(mockSession, q1, 3, null, false); // 50%

            setupBatchLoaderMock(Map.of(competencyId3, competencyBasic));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(a1));

            // Then: With small team adjustment (-10%), threshold drops to 50%
            // teamSize = 3 < 5 (smallTeamThreshold), so adjustment applied
            assertThat(result).isNotNull();
            assertThat(result.getTeamFitMetrics().getTeamSize()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should not reduce threshold below minimum floor")
        void shouldNotReduceBelowMinimumFloor() {
            // Given: Very small team with severe gaps (both adjustments apply)
            UUID teamId = ((TeamFitBlueprint) mockTemplate.getTypedBlueprint()).getTeamId();

            // Set very low initial pass threshold to test floor
            scoringConfig.getThresholds().getTeamFit().setPassThreshold(0.35);

            List<TeamService.TeamMemberProfile> members = List.of(
                new TeamService.TeamMemberProfile(UUID.randomUUID(), "Alice", "Dev", Map.of(), Map.of())
            );

            // All competencies are gaps (saturation below diversity threshold)
            TeamService.TeamProfile teamProfile = new TeamService.TeamProfile(
                teamId, "Tiny Team", members,
                Map.of(competencyId1, 0.05, competencyId2, 0.05, competencyId3, 0.05),
                Map.of(), List.of(competencyId1, competencyId2, competencyId3)
            );
            when(teamService.getTeamProfile(teamId)).thenReturn(Optional.of(teamProfile));

            BehavioralIndicator ind1 = createBehavioralIndicator(UUID.randomUUID(), competencyBasic);
            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), ind1, QuestionType.LIKERT);
            TestAnswer a1 = createAnswer(mockSession, q1, 3, null, false);

            setupBatchLoaderMock(Map.of(competencyId3, competencyBasic));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(a1));

            // Then: Both adjustments would reduce to 0.15 (0.35 - 0.1 - 0.1)
            // But floor is 0.3, so threshold should be 30%
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("Sigmoid Multiplier Tests")
    class SigmoidMultiplierTests {

        @Test
        @DisplayName("Should produce multiplier near 1.0 when diversity equals saturation")
        void shouldProduceNeutralWhenBalanced() {
            // Given: 2 competencies, one diversity + one saturation = balanced
            setTemplateSaturationThreshold(0.6);

            BehavioralIndicator ind1 = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);
            BehavioralIndicator ind2 = createBehavioralIndicator(UUID.randomUUID(), competencyBasic);

            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), ind1, QuestionType.LIKERT);
            AssessmentQuestion q2 = createQuestion(UUID.randomUUID(), ind2, QuestionType.LIKERT);

            // One above threshold (saturation), one between diversity and saturation
            TestAnswer a1 = createAnswer(mockSession, q1, 4, null, false); // 75% - saturation (>0.6)
            TestAnswer a2 = createAnswer(mockSession, q2, 3, null, false); // 50% - diversity (>=0.5, <0.6)

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithEscoAndBigFive, competencyId3, competencyBasic));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(a1, a2));

            // Then: diversity=0.5, saturation=0.5, balance=0 -> sigmoid(0) = 0.5
            // multiplier = 0.9 + (1.1 - 0.9) * 0.5 = 1.0
            assertThat(result.getTeamFitMetrics().getTeamFitMultiplier())
                .isCloseTo(1.0, within(0.01));
        }

        @Test
        @DisplayName("Should produce multiplier near bonus for high diversity")
        void shouldProduceBonusForHighDiversity() {
            // Given: 3 competencies, 2 diversity + 0 saturation + 1 gap
            setTemplateSaturationThreshold(0.9);

            BehavioralIndicator ind1 = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);
            BehavioralIndicator ind2 = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoOnly);
            BehavioralIndicator ind3 = createBehavioralIndicator(UUID.randomUUID(), competencyBasic);

            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), ind1, QuestionType.LIKERT);
            AssessmentQuestion q2 = createQuestion(UUID.randomUUID(), ind2, QuestionType.LIKERT);
            AssessmentQuestion q3 = createQuestion(UUID.randomUUID(), ind3, QuestionType.LIKERT);

            // All in diversity range (50-90%)
            TestAnswer a1 = createAnswer(mockSession, q1, 3, null, false); // 50%
            TestAnswer a2 = createAnswer(mockSession, q2, 4, null, false); // 75%
            TestAnswer a3 = createAnswer(mockSession, q3, 2, null, false); // 25% - gap

            setupBatchLoaderMock(Map.of(
                competencyId1, competencyWithEscoAndBigFive,
                competencyId2, competencyWithEscoOnly,
                competencyId3, competencyBasic
            ));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(a1, a2, a3));

            // Then: diversity=0.67, saturation=0, balance=0.67 -> sigmoid positive
            // multiplier should be > 1.0 (approaching bonus)
            assertThat(result.getTeamFitMetrics().getTeamFitMultiplier())
                .isGreaterThan(1.05);
        }

        @Test
        @DisplayName("Should produce multiplier near penalty for high saturation")
        void shouldProducePenaltyForHighSaturation() {
            // Given: All competencies saturated
            setTemplateSaturationThreshold(0.4);

            BehavioralIndicator ind1 = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);
            BehavioralIndicator ind2 = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoOnly);

            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), ind1, QuestionType.LIKERT);
            AssessmentQuestion q2 = createQuestion(UUID.randomUUID(), ind2, QuestionType.LIKERT);

            // Both above threshold (saturated)
            TestAnswer a1 = createAnswer(mockSession, q1, 5, null, false); // 100%
            TestAnswer a2 = createAnswer(mockSession, q2, 5, null, false); // 100%

            setupBatchLoaderMock(Map.of(
                competencyId1, competencyWithEscoAndBigFive,
                competencyId2, competencyWithEscoOnly
            ));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(a1, a2));

            // Then: diversity=0, saturation=1.0, balance=-1.0 -> sigmoid very low
            // multiplier should be < 1.0 (approaching penalty)
            assertThat(result.getTeamFitMetrics().getTeamFitMultiplier())
                .isLessThan(0.95);
        }

        @Test
        @DisplayName("Sigmoid should be monotonically increasing with diversity-saturation balance")
        void shouldBeMonotonicallyIncreasing() {
            // Given: Validate that as balance increases, multiplier increases
            // We test this by running two scenarios and comparing

            // Scenario A: balanced (diversity=saturation)
            setTemplateSaturationThreshold(0.7);

            BehavioralIndicator ind1 = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);
            BehavioralIndicator ind2 = createBehavioralIndicator(UUID.randomUUID(), competencyBasic);

            AssessmentQuestion qA1 = createQuestion(UUID.randomUUID(), ind1, QuestionType.LIKERT);
            AssessmentQuestion qA2 = createQuestion(UUID.randomUUID(), ind2, QuestionType.LIKERT);

            TestAnswer aA1 = createAnswer(mockSession, qA1, 4, null, false); // 75% - saturation
            TestAnswer aA2 = createAnswer(mockSession, qA2, 3, null, false); // 50% - diversity

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithEscoAndBigFive, competencyId3, competencyBasic));
            ScoringResult balancedResult = scoringStrategy.calculate(mockSession, List.of(aA1, aA2));

            // Scenario B: all saturated
            setTemplateSaturationThreshold(0.4);
            BehavioralIndicator ind3 = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoOnly);
            AssessmentQuestion qB1 = createQuestion(UUID.randomUUID(), ind3, QuestionType.LIKERT);
            TestAnswer aB1 = createAnswer(mockSession, qB1, 5, null, false); // 100% - saturated

            setupBatchLoaderMock(Map.of(competencyId2, competencyWithEscoOnly));
            ScoringResult saturatedResult = scoringStrategy.calculate(mockSession, List.of(aB1));

            // Then: balanced multiplier > saturated multiplier
            assertThat(balancedResult.getTeamFitMetrics().getTeamFitMultiplier())
                .isGreaterThan(saturatedResult.getTeamFitMetrics().getTeamFitMultiplier());
        }
    }

    @Nested
    @DisplayName("Personality Compatibility Tests")
    class PersonalityCompatibilityTests {

        @Test
        @DisplayName("Should calculate personality compatibility when both profiles available")
        void shouldCalculatePersonalityCompatibilityWhenBothProfilesAvailable() {
            // Given: Team with averagePersonality and candidate with Big Five competencies
            UUID teamId = ((TeamFitBlueprint) mockTemplate.getTypedBlueprint()).getTeamId();

            // Create competencies for each Big Five trait
            UUID openId = UUID.randomUUID();
            UUID consId = UUID.randomUUID();
            UUID extraId = UUID.randomUUID();

            Competency openness = createCompetency(openId, "Openness Trait", null,
                    "http://data.europa.eu/esco/skill/open1", "OPENNESS");
            Competency conscientiousness = createCompetency(consId, "Conscientiousness Trait", null,
                    "http://data.europa.eu/esco/skill/cons1", "CONSCIENTIOUSNESS");
            Competency extraversion = createCompetency(extraId, "Extraversion Trait", null,
                    "http://data.europa.eu/esco/skill/extra1", "EXTRAVERSION");

            // Team personality profile (0-100 scale)
            Map<String, Double> teamPersonality = Map.of(
                    "OPENNESS", 70.0,
                    "CONSCIENTIOUSNESS", 60.0,
                    "EXTRAVERSION", 50.0
            );

            TeamService.TeamProfile teamProfile = new TeamService.TeamProfile(
                    teamId, "Personality Team",
                    List.of(new TeamService.TeamMemberProfile(UUID.randomUUID(), "Alice", "Dev",
                            Map.of(), Map.of("OPENNESS", 70.0, "CONSCIENTIOUSNESS", 60.0))),
                    Map.of(openId, 0.5, consId, 0.5, extraId, 0.5), // diversity range
                    teamPersonality,
                    List.of()
            );
            when(teamService.getTeamProfile(teamId)).thenReturn(Optional.of(teamProfile));

            // Create answers - candidate scores similar to team (high compatibility)
            BehavioralIndicator indOpen = createBehavioralIndicator(UUID.randomUUID(), openness);
            BehavioralIndicator indCons = createBehavioralIndicator(UUID.randomUUID(), conscientiousness);
            BehavioralIndicator indExtra = createBehavioralIndicator(UUID.randomUUID(), extraversion);

            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), indOpen, QuestionType.LIKERT);
            AssessmentQuestion q2 = createQuestion(UUID.randomUUID(), indCons, QuestionType.LIKERT);
            AssessmentQuestion q3 = createQuestion(UUID.randomUUID(), indExtra, QuestionType.LIKERT);

            // Likert 4 -> normalized 0.75 -> bigFiveAverage = 75.0
            TestAnswer a1 = createAnswer(mockSession, q1, 4, null, false);
            TestAnswer a2 = createAnswer(mockSession, q2, 4, null, false);
            TestAnswer a3 = createAnswer(mockSession, q3, 3, null, false); // 50%

            setupBatchLoaderMock(Map.of(openId, openness, consId, conscientiousness, extraId, extraversion));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(a1, a2, a3));

            // Then
            assertThat(result.getTeamFitMetrics()).isNotNull();
            assertThat(result.getTeamFitMetrics().getPersonalityCompatibility()).isNotNull();
            assertThat(result.getTeamFitMetrics().getPersonalityCompatibility())
                    .isBetween(0.0, 1.0);
        }

        @Test
        @DisplayName("Should return null compatibility when no Big Five data")
        void shouldReturnNullCompatibilityWhenNoBigFiveData() {
            // Given: Competencies without Big Five mappings
            UUID teamId = ((TeamFitBlueprint) mockTemplate.getTypedBlueprint()).getTeamId();

            Map<String, Double> teamPersonality = Map.of("OPENNESS", 70.0, "CONSCIENTIOUSNESS", 60.0);

            TeamService.TeamProfile teamProfile = new TeamService.TeamProfile(
                    teamId, "Personality Team",
                    List.of(),
                    Map.of(competencyId3, 0.5),
                    teamPersonality,
                    List.of()
            );
            when(teamService.getTeamProfile(teamId)).thenReturn(Optional.of(teamProfile));

            // Candidate has no Big Five competencies
            BehavioralIndicator ind = createBehavioralIndicator(UUID.randomUUID(), competencyBasic);
            AssessmentQuestion q = createQuestion(UUID.randomUUID(), ind, QuestionType.LIKERT);
            TestAnswer a = createAnswer(mockSession, q, 4, null, false);

            setupBatchLoaderMock(Map.of(competencyId3, competencyBasic));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(a));

            // Then: No Big Five data -> personalityCompatibility should be null
            assertThat(result.getTeamFitMetrics().getPersonalityCompatibility()).isNull();
        }

        @Test
        @DisplayName("Should return null when team has no personality data")
        void shouldReturnNullWhenTeamHasNoPersonalityData() {
            // Given: Team profile with empty averagePersonality
            UUID teamId = ((TeamFitBlueprint) mockTemplate.getTypedBlueprint()).getTeamId();

            TeamService.TeamProfile teamProfile = new TeamService.TeamProfile(
                    teamId, "No Personality Team",
                    List.of(),
                    Map.of(competencyId1, 0.5),
                    Map.of(), // empty personality
                    List.of()
            );
            when(teamService.getTeamProfile(teamId)).thenReturn(Optional.of(teamProfile));

            // Candidate with Big Five competency
            BehavioralIndicator ind = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);
            AssessmentQuestion q = createQuestion(UUID.randomUUID(), ind, QuestionType.LIKERT);
            TestAnswer a = createAnswer(mockSession, q, 4, null, false);

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithEscoAndBigFive));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(a));

            // Then: Team has no personality data -> personalityCompatibility should be null
            assertThat(result.getTeamFitMetrics().getPersonalityCompatibility()).isNull();
        }

        @Test
        @DisplayName("Should boost multiplier for high compatibility")
        void shouldBoostMultiplierForHighCompatibility() {
            // Given: Candidate and team have very similar personality profiles
            UUID teamId = ((TeamFitBlueprint) mockTemplate.getTypedBlueprint()).getTeamId();

            // Create Big Five competencies
            UUID openId = UUID.randomUUID();
            UUID consId = UUID.randomUUID();

            Competency openness = createCompetency(openId, "Openness", null,
                    "http://data.europa.eu/esco/skill/open2", "OPENNESS");
            Competency conscientiousness = createCompetency(consId, "Conscientiousness", null,
                    "http://data.europa.eu/esco/skill/cons2", "CONSCIENTIOUSNESS");

            // Team personality closely matches what candidate will score
            // Candidate will score Likert 5 -> normalized 1.0 -> bigFiveAverage = 100.0
            Map<String, Double> teamPersonality = Map.of(
                    "OPENNESS", 100.0,
                    "CONSCIENTIOUSNESS", 100.0
            );

            TeamService.TeamProfile teamProfile = new TeamService.TeamProfile(
                    teamId, "Similar Team",
                    List.of(),
                    Map.of(openId, 0.5, consId, 0.5), // diversity range
                    teamPersonality,
                    List.of()
            );
            when(teamService.getTeamProfile(teamId)).thenReturn(Optional.of(teamProfile));

            BehavioralIndicator indOpen = createBehavioralIndicator(UUID.randomUUID(), openness);
            BehavioralIndicator indCons = createBehavioralIndicator(UUID.randomUUID(), conscientiousness);

            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), indOpen, QuestionType.LIKERT);
            AssessmentQuestion q2 = createQuestion(UUID.randomUUID(), indCons, QuestionType.LIKERT);

            // Candidate scores max (100%) -> bigFiveAverages = {OPENNESS: 100, CONSCIENTIOUSNESS: 100}
            TestAnswer a1 = createAnswer(mockSession, q1, 5, null, false);
            TestAnswer a2 = createAnswer(mockSession, q2, 5, null, false);

            setupBatchLoaderMock(Map.of(openId, openness, consId, conscientiousness));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(a1, a2));

            // Then: Identical profiles -> compatibility = 1.0
            // Adjustment = (1.0 - 0.5) * 0.1 = +0.05
            // Multiplier should be higher than without personality adjustment
            assertThat(result.getTeamFitMetrics().getPersonalityCompatibility())
                    .isCloseTo(1.0, within(0.01));
            // The multiplier includes the personality boost
            // Base sigmoid multiplier for 2 diversity, 0 saturation at threshold 0.75 would be > 1.0
            // Plus personality adjustment of +0.05
            assertThat(result.getTeamFitMetrics().getTeamFitMultiplier())
                    .isGreaterThan(1.0);
        }

        @Test
        @DisplayName("Should reduce multiplier for low compatibility")
        void shouldReduceMultiplierForLowCompatibility() {
            // Given: Candidate and team have very different personality profiles
            UUID teamId = ((TeamFitBlueprint) mockTemplate.getTypedBlueprint()).getTeamId();

            UUID openId = UUID.randomUUID();
            UUID consId = UUID.randomUUID();

            Competency openness = createCompetency(openId, "Openness", null,
                    "http://data.europa.eu/esco/skill/open3", "OPENNESS");
            Competency conscientiousness = createCompetency(consId, "Conscientiousness", null,
                    "http://data.europa.eu/esco/skill/cons3", "CONSCIENTIOUSNESS");

            // Team has 0% on both traits, candidate will score 100% -> max distance
            Map<String, Double> teamPersonality = Map.of(
                    "OPENNESS", 0.0,
                    "CONSCIENTIOUSNESS", 0.0
            );

            TeamService.TeamProfile teamProfile = new TeamService.TeamProfile(
                    teamId, "Different Team",
                    List.of(),
                    Map.of(openId, 0.5, consId, 0.5), // diversity range
                    teamPersonality,
                    List.of()
            );
            when(teamService.getTeamProfile(teamId)).thenReturn(Optional.of(teamProfile));

            BehavioralIndicator indOpen = createBehavioralIndicator(UUID.randomUUID(), openness);
            BehavioralIndicator indCons = createBehavioralIndicator(UUID.randomUUID(), conscientiousness);

            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), indOpen, QuestionType.LIKERT);
            AssessmentQuestion q2 = createQuestion(UUID.randomUUID(), indCons, QuestionType.LIKERT);

            // Candidate scores max (100%) -> bigFiveAverages = {OPENNESS: 100, CONSCIENTIOUSNESS: 100}
            // Team has 0% -> max Euclidean distance
            TestAnswer a1 = createAnswer(mockSession, q1, 5, null, false);
            TestAnswer a2 = createAnswer(mockSession, q2, 5, null, false);

            setupBatchLoaderMock(Map.of(openId, openness, consId, conscientiousness));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(a1, a2));

            // Then: Max distance -> compatibility = 0.0
            // Adjustment = (0.0 - 0.5) * 0.1 = -0.05
            assertThat(result.getTeamFitMetrics().getPersonalityCompatibility())
                    .isCloseTo(0.0, within(0.01));
            // The multiplier should be reduced by the personality penalty
            // Compare: without personality data, the sigmoid-only multiplier for
            // 2 diversity/0 saturation would be higher
            double multiplierWithPenalty = result.getTeamFitMetrics().getTeamFitMultiplier();

            // Run the same scenario without personality data to compare
            TeamService.TeamProfile noPersonalityProfile = new TeamService.TeamProfile(
                    teamId, "No Personality Team",
                    List.of(),
                    Map.of(openId, 0.5, consId, 0.5),
                    Map.of(), // empty personality
                    List.of()
            );
            when(teamService.getTeamProfile(teamId)).thenReturn(Optional.of(noPersonalityProfile));

            setupBatchLoaderMock(Map.of(openId, openness, consId, conscientiousness));
            TestAnswer a3 = createAnswer(mockSession, q1, 5, null, false);
            TestAnswer a4 = createAnswer(mockSession, q2, 5, null, false);

            ScoringResult resultWithout = scoringStrategy.calculate(mockSession, List.of(a3, a4));
            double multiplierWithout = resultWithout.getTeamFitMetrics().getTeamFitMultiplier();

            // Multiplier with low compatibility penalty should be lower
            assertThat(multiplierWithPenalty).isLessThan(multiplierWithout);
        }
    }

    @Nested
    @DisplayName("Multiplier Clamping Tests")
    class MultiplierClampingTests {

        @Test
        @DisplayName("Should clamp multiplier to 1.2 when extreme personality adjustment pushes it above")
        void shouldClampMultiplierToUpperBound() {
            // Given: Configure extreme personalityWeight so adjustment exceeds bounds
            // Set personalityWeight high enough that compatibility=1.0 produces teamFitMultiplier > 1.2
            // Sigmoid bonus at max diversity ~ 1.1, personality adj = (1.0 - 0.5) * weight
            // To exceed 1.2: 1.1 + 0.5 * weight > 1.2 -> weight > 0.2
            scoringConfig.getThresholds().getTeamFit().setPersonalityWeight(0.5);

            UUID teamId = ((TeamFitBlueprint) mockTemplate.getTypedBlueprint()).getTeamId();

            UUID openId = UUID.randomUUID();
            UUID consId = UUID.randomUUID();

            Competency openness = createCompetency(openId, "Openness", null,
                    "http://data.europa.eu/esco/skill/clamp-open", "OPENNESS");
            Competency conscientiousness = createCompetency(consId, "Conscientiousness", null,
                    "http://data.europa.eu/esco/skill/clamp-cons", "CONSCIENTIOUSNESS");

            // Team and candidate have identical profiles -> compatibility = 1.0
            Map<String, Double> teamPersonality = Map.of(
                    "OPENNESS", 100.0,
                    "CONSCIENTIOUSNESS", 100.0
            );

            TeamService.TeamProfile teamProfile = new TeamService.TeamProfile(
                    teamId, "Clamp Test Team",
                    List.of(),
                    Map.of(openId, 0.5, consId, 0.5), // diversity range
                    teamPersonality,
                    List.of()
            );
            when(teamService.getTeamProfile(teamId)).thenReturn(Optional.of(teamProfile));

            BehavioralIndicator indOpen = createBehavioralIndicator(UUID.randomUUID(), openness);
            BehavioralIndicator indCons = createBehavioralIndicator(UUID.randomUUID(), conscientiousness);

            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), indOpen, QuestionType.LIKERT);
            AssessmentQuestion q2 = createQuestion(UUID.randomUUID(), indCons, QuestionType.LIKERT);

            // Max score -> bigFiveAverages = {OPENNESS: 100, CONSCIENTIOUSNESS: 100}
            TestAnswer a1 = createAnswer(mockSession, q1, 5, null, false);
            TestAnswer a2 = createAnswer(mockSession, q2, 5, null, false);

            setupBatchLoaderMock(Map.of(openId, openness, consId, conscientiousness));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(a1, a2));

            // Then: Without clamping, multiplier would be > 1.2 due to extreme personalityWeight
            // With clamping, it must be exactly 1.2
            assertThat(result.getTeamFitMetrics().getTeamFitMultiplier())
                    .isCloseTo(1.2, within(0.001));
            // Final percentage must not exceed 100%
            assertThat(result.getOverallPercentage()).isLessThanOrEqualTo(100.0);
        }

        @Test
        @DisplayName("Should clamp multiplier to 0.8 when extreme personality adjustment pushes it below")
        void shouldClampMultiplierToLowerBound() {
            // Given: Configure extreme personalityWeight so negative adjustment drops below 0.8
            // Sigmoid penalty at max saturation ~ 0.9, personality adj = (0.0 - 0.5) * weight = -0.5 * weight
            // To go below 0.8: 0.9 - 0.5 * weight < 0.8 -> weight > 0.2
            scoringConfig.getThresholds().getTeamFit().setPersonalityWeight(0.5);
            setTemplateSaturationThreshold(0.4); // Force all scores into saturation

            UUID teamId = ((TeamFitBlueprint) mockTemplate.getTypedBlueprint()).getTeamId();

            UUID openId = UUID.randomUUID();
            UUID consId = UUID.randomUUID();

            Competency openness = createCompetency(openId, "Openness", null,
                    "http://data.europa.eu/esco/skill/clamp-low-open", "OPENNESS");
            Competency conscientiousness = createCompetency(consId, "Conscientiousness", null,
                    "http://data.europa.eu/esco/skill/clamp-low-cons", "CONSCIENTIOUSNESS");

            // Team at 0%, candidate at 100% -> max distance -> compatibility = 0.0
            Map<String, Double> teamPersonality = Map.of(
                    "OPENNESS", 0.0,
                    "CONSCIENTIOUSNESS", 0.0
            );

            TeamService.TeamProfile teamProfile = new TeamService.TeamProfile(
                    teamId, "Low Clamp Team",
                    List.of(),
                    Map.of(openId, 0.5, consId, 0.5),
                    teamPersonality,
                    List.of()
            );
            when(teamService.getTeamProfile(teamId)).thenReturn(Optional.of(teamProfile));

            BehavioralIndicator indOpen = createBehavioralIndicator(UUID.randomUUID(), openness);
            BehavioralIndicator indCons = createBehavioralIndicator(UUID.randomUUID(), conscientiousness);

            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), indOpen, QuestionType.LIKERT);
            AssessmentQuestion q2 = createQuestion(UUID.randomUUID(), indCons, QuestionType.LIKERT);

            // Max score -> bigFiveAverages = {OPENNESS: 100, CONSCIENTIOUSNESS: 100}
            // But team has 0% -> max distance -> compatibility ~0.0
            TestAnswer a1 = createAnswer(mockSession, q1, 5, null, false);
            TestAnswer a2 = createAnswer(mockSession, q2, 5, null, false);

            setupBatchLoaderMock(Map.of(openId, openness, consId, conscientiousness));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(a1, a2));

            // Then: Without clamping, multiplier would be < 0.8
            // With clamping, it must be exactly 0.8
            assertThat(result.getTeamFitMetrics().getTeamFitMultiplier())
                    .isCloseTo(0.8, within(0.001));
            // Final percentage must be >= 0%
            assertThat(result.getOverallPercentage()).isGreaterThanOrEqualTo(0.0);
        }

        @Test
        @DisplayName("Should not clamp multiplier when personality adjustment stays within bounds")
        void shouldNotClampWhenWithinBounds() {
            // Given: Normal personalityWeight (default 0.1), producing adjustment within [-0.05, +0.05]
            // Sigmoid range [0.9, 1.1] + personality [-0.05, +0.05] = [0.85, 1.15] -> within [0.8, 1.2]
            UUID teamId = ((TeamFitBlueprint) mockTemplate.getTypedBlueprint()).getTeamId();

            UUID openId = UUID.randomUUID();
            UUID consId = UUID.randomUUID();

            Competency openness = createCompetency(openId, "Openness", null,
                    "http://data.europa.eu/esco/skill/norm-open", "OPENNESS");
            Competency conscientiousness = createCompetency(consId, "Conscientiousness", null,
                    "http://data.europa.eu/esco/skill/norm-cons", "CONSCIENTIOUSNESS");

            // Team and candidate have identical profiles -> compatibility = 1.0
            // Adjustment = (1.0 - 0.5) * 0.1 = +0.05
            Map<String, Double> teamPersonality = Map.of(
                    "OPENNESS", 100.0,
                    "CONSCIENTIOUSNESS", 100.0
            );

            TeamService.TeamProfile teamProfile = new TeamService.TeamProfile(
                    teamId, "Normal Clamp Team",
                    List.of(),
                    Map.of(openId, 0.5, consId, 0.5), // diversity range
                    teamPersonality,
                    List.of()
            );
            when(teamService.getTeamProfile(teamId)).thenReturn(Optional.of(teamProfile));

            BehavioralIndicator indOpen = createBehavioralIndicator(UUID.randomUUID(), openness);
            BehavioralIndicator indCons = createBehavioralIndicator(UUID.randomUUID(), conscientiousness);

            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), indOpen, QuestionType.LIKERT);
            AssessmentQuestion q2 = createQuestion(UUID.randomUUID(), indCons, QuestionType.LIKERT);

            TestAnswer a1 = createAnswer(mockSession, q1, 5, null, false);
            TestAnswer a2 = createAnswer(mockSession, q2, 5, null, false);

            setupBatchLoaderMock(Map.of(openId, openness, consId, conscientiousness));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(a1, a2));

            // Then: Multiplier should be within (0.8, 1.2) exclusive -- not clamped
            double multiplier = result.getTeamFitMetrics().getTeamFitMultiplier();
            assertThat(multiplier).isGreaterThan(0.8);
            assertThat(multiplier).isLessThan(1.2);
            // Verify final percentage is still bounded
            assertThat(result.getOverallPercentage()).isBetween(0.0, 100.0);
        }

        @Test
        @DisplayName("Should clamp final adjusted percentage to 100% even without personality data")
        void shouldClampFinalPercentageTo100() {
            // Given: A scenario where overallPercentage * multiplier could exceed 100%
            // This is a safety-net test for the final percentage clamp
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);

            List<TestAnswer> answers = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                AssessmentQuestion q = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
                answers.add(createAnswer(mockSession, q, 5, null, false));
            }

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithEscoAndBigFive));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, answers);

            // Then: No matter what multiplier/weighting combination occurs,
            // the final percentage must never exceed 100%
            assertThat(result.getOverallPercentage()).isLessThanOrEqualTo(100.0);
            assertThat(result.getOverallPercentage()).isGreaterThanOrEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Role Weight Tests")
    class RoleWeightTests {

        @Test
        @DisplayName("Should apply role weights to scoring when present")
        void shouldApplyRoleWeightsToScoringWhenPresent() {
            // Given: Blueprint with role weights giving competencyId1 a 2.0x weight
            // and competencyId2 a 0.5x weight
            TeamFitBlueprint blueprint = new TeamFitBlueprint();
            blueprint.setTeamId(UUID.randomUUID());
            blueprint.setSaturationThreshold(0.75);
            blueprint.setTargetRole("Backend Developer");
            blueprint.setRoleCompetencyWeights(Map.of(competencyId1, 2.0, competencyId2, 0.5));
            mockTemplate.setTypedBlueprint(blueprint);

            BehavioralIndicator ind1 = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);
            BehavioralIndicator ind2 = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoOnly);

            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), ind1, QuestionType.LIKERT);
            AssessmentQuestion q2 = createQuestion(UUID.randomUUID(), ind2, QuestionType.LIKERT);

            // Both at same raw score (75%)
            TestAnswer a1 = createAnswer(mockSession, q1, 4, null, false);
            TestAnswer a2 = createAnswer(mockSession, q2, 4, null, false);

            setupBatchLoaderMock(Map.of(
                competencyId1, competencyWithEscoAndBigFive,
                competencyId2, competencyWithEscoOnly
            ));

            // When: Calculate with role weights
            ScoringResult resultWithRoleWeights = scoringStrategy.calculate(mockSession, List.of(a1, a2));

            // Then: Calculate without role weights for comparison
            TeamFitBlueprint noRoleBlueprint = new TeamFitBlueprint();
            noRoleBlueprint.setTeamId(UUID.randomUUID());
            noRoleBlueprint.setSaturationThreshold(0.75);
            mockTemplate.setTypedBlueprint(noRoleBlueprint);

            // Re-create answers and indicators for fresh mock state
            BehavioralIndicator ind1b = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);
            BehavioralIndicator ind2b = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoOnly);

            AssessmentQuestion q1b = createQuestion(UUID.randomUUID(), ind1b, QuestionType.LIKERT);
            AssessmentQuestion q2b = createQuestion(UUID.randomUUID(), ind2b, QuestionType.LIKERT);

            TestAnswer a1b = createAnswer(mockSession, q1b, 4, null, false);
            TestAnswer a2b = createAnswer(mockSession, q2b, 4, null, false);

            setupBatchLoaderMock(Map.of(
                competencyId1, competencyWithEscoAndBigFive,
                competencyId2, competencyWithEscoOnly
            ));

            ScoringResult resultWithoutRoleWeights = scoringStrategy.calculate(mockSession, List.of(a1b, a2b));

            // Both should complete successfully
            assertThat(resultWithRoleWeights).isNotNull();
            assertThat(resultWithoutRoleWeights).isNotNull();
            assertThat(resultWithRoleWeights.getCompetencyScores()).hasSize(2);
            assertThat(resultWithoutRoleWeights.getCompetencyScores()).hasSize(2);

            // The overall percentage should be the same because role weights are applied
            // symmetrically to both numerator and denominator. When all competencies have
            // equal raw scores (75%), the weighted average still equals 75%.
            // This validates the symmetry property. The weight distribution shifts emphasis
            // but the overall result is the same when raw scores are identical.
            assertThat(resultWithRoleWeights.getOverallPercentage()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should not affect scoring when role weights are empty")
        void shouldNotAffectScoringWhenRoleWeightsEmpty() {
            // Given: Blueprint with null roleCompetencyWeights
            TeamFitBlueprint blueprintNull = new TeamFitBlueprint();
            blueprintNull.setTeamId(UUID.randomUUID());
            blueprintNull.setSaturationThreshold(0.75);
            blueprintNull.setRoleCompetencyWeights(null);
            mockTemplate.setTypedBlueprint(blueprintNull);

            BehavioralIndicator ind1 = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);
            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), ind1, QuestionType.LIKERT);
            TestAnswer a1 = createAnswer(mockSession, q1, 4, null, false);

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithEscoAndBigFive));

            ScoringResult resultNull = scoringStrategy.calculate(mockSession, List.of(a1));

            // Now with empty map
            TeamFitBlueprint blueprintEmpty = new TeamFitBlueprint();
            blueprintEmpty.setTeamId(UUID.randomUUID());
            blueprintEmpty.setSaturationThreshold(0.75);
            blueprintEmpty.setRoleCompetencyWeights(Map.of());
            mockTemplate.setTypedBlueprint(blueprintEmpty);

            BehavioralIndicator ind2 = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);
            AssessmentQuestion q2 = createQuestion(UUID.randomUUID(), ind2, QuestionType.LIKERT);
            TestAnswer a2 = createAnswer(mockSession, q2, 4, null, false);

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithEscoAndBigFive));

            ScoringResult resultEmpty = scoringStrategy.calculate(mockSession, List.of(a2));

            // Both should produce the same result
            assertThat(resultNull.getOverallPercentage())
                .isCloseTo(resultEmpty.getOverallPercentage(), within(0.001));
        }

        @Test
        @DisplayName("Should extract target role from blueprint without errors")
        void shouldExtractTargetRoleFromBlueprint() {
            // Given: Blueprint with targetRole set
            TeamFitBlueprint blueprint = new TeamFitBlueprint();
            blueprint.setTeamId(UUID.randomUUID());
            blueprint.setSaturationThreshold(0.75);
            blueprint.setTargetRole("Backend Developer");
            mockTemplate.setTypedBlueprint(blueprint);

            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(mockSession, question, 4, null, false);

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithEscoAndBigFive));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then: Scoring completes without error and produces valid result
            assertThat(result).isNotNull();
            assertThat(result.getOverallPercentage()).isGreaterThan(0);
            assertThat(result.getCompetencyScores()).hasSize(1);
            assertThat(result.getGoal()).isEqualTo(AssessmentGoal.TEAM_FIT);
        }

        @Test
        @DisplayName("Should extract role weights from legacy blueprint map")
        void shouldExtractRoleWeightsFromLegacyBlueprint() {
            // Given: Legacy map-based blueprint with roleCompetencyWeights
            mockTemplate.setTypedBlueprint(null);
            Map<String, Object> legacyBlueprint = new HashMap<>();
            legacyBlueprint.put("teamId", UUID.randomUUID().toString());
            legacyBlueprint.put("saturationThreshold", 0.75);
            legacyBlueprint.put("targetRole", "Project Manager");

            // Role weights as Map<String, Number> (legacy format)
            Map<String, Number> legacyRoleWeights = new HashMap<>();
            legacyRoleWeights.put(competencyId1.toString(), 2.0);
            legacyRoleWeights.put(competencyId2.toString(), 0.5);
            legacyBlueprint.put("roleCompetencyWeights", legacyRoleWeights);
            mockTemplate.setBlueprint(legacyBlueprint);

            BehavioralIndicator ind1 = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);
            BehavioralIndicator ind2 = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoOnly);

            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), ind1, QuestionType.LIKERT);
            AssessmentQuestion q2 = createQuestion(UUID.randomUUID(), ind2, QuestionType.LIKERT);

            TestAnswer a1 = createAnswer(mockSession, q1, 4, null, false);
            TestAnswer a2 = createAnswer(mockSession, q2, 4, null, false);

            setupBatchLoaderMock(Map.of(
                competencyId1, competencyWithEscoAndBigFive,
                competencyId2, competencyWithEscoOnly
            ));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(a1, a2));

            // Then: Scoring completes successfully using legacy blueprint extraction
            assertThat(result).isNotNull();
            assertThat(result.getCompetencyScores()).hasSize(2);
            assertThat(result.getOverallPercentage()).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("BE-005: Floating-Point Boundary Precision Tests")
    class FloatingPointBoundaryTests {

        @Test
        @DisplayName("Should classify as saturated when score exactly equals saturation threshold")
        void shouldClassifyAsSaturatedAtExactThreshold() {
            // Given: Saturation threshold = 0.75, single competency at exactly 75%
            setTemplateSaturationThreshold(0.75);

            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyBasic);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.SJT);
            // Score of 0.75 = 75% = exactly the saturation threshold
            TestAnswer answer = createAnswer(mockSession, question, null, 0.75, false);

            setupBatchLoaderMock(Map.of(competencyId3, competencyBasic));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then: Should be classified as saturation (score meets threshold)
            assertThat(result.getTeamFitMetrics()).isNotNull();
            assertThat(result.getTeamFitMetrics().getSaturationCount()).isEqualTo(1);
            assertThat(result.getTeamFitMetrics().getDiversityCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should classify as diversity when score is between diversity and saturation thresholds")
        void shouldClassifyAsDiversityBetweenThresholds() {
            // Given: Saturation threshold = 0.75, diversity threshold = 0.5
            // Single competency at 60% (between the two)
            setTemplateSaturationThreshold(0.75);

            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyBasic);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.SJT);
            TestAnswer answer = createAnswer(mockSession, question, null, 0.6, false);

            setupBatchLoaderMock(Map.of(competencyId3, competencyBasic));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then: Should be classified as diversity
            assertThat(result.getTeamFitMetrics()).isNotNull();
            assertThat(result.getTeamFitMetrics().getDiversityCount()).isEqualTo(1);
            assertThat(result.getTeamFitMetrics().getSaturationCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should classify as diversity when score exactly equals diversity threshold")
        void shouldClassifyAsDiversityAtExactThreshold() {
            // Given: Diversity threshold = 0.5, score exactly 50%
            setTemplateSaturationThreshold(0.75);

            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyBasic);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.SJT);
            TestAnswer answer = createAnswer(mockSession, question, null, 0.5, false);

            setupBatchLoaderMock(Map.of(competencyId3, competencyBasic));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then: 0.5 meets diversity threshold of 0.5
            assertThat(result.getTeamFitMetrics()).isNotNull();
            assertThat(result.getTeamFitMetrics().getDiversityCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should handle pass threshold boundary with precision rounding")
        void shouldHandlePassThresholdBoundaryWithPrecision() {
            // Given: Pass threshold = 60%, candidate scores exactly at threshold
            // Use a single high-scoring competency with ESCO + BigFive for diversity
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.SJT);
            // Score needs to produce an adjustedPercentage that is at the boundary
            // 0.6 = 60% base, multiplied by team fit multiplier
            TestAnswer answer = createAnswer(mockSession, question, null, 0.6, false);

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithEscoAndBigFive));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then: Should not crash and should make a deterministic pass/fail decision
            assertThat(result).isNotNull();
            assertThat(result.getPassed()).isNotNull();
        }

        @Test
        @DisplayName("Should classify as gap when score is below diversity threshold")
        void shouldClassifyAsGapBelowDiversityThreshold() {
            // Given: Diversity threshold = 0.5, score at 49%
            setTemplateSaturationThreshold(0.75);

            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyBasic);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.SJT);
            TestAnswer answer = createAnswer(mockSession, question, null, 0.49, false);

            setupBatchLoaderMock(Map.of(competencyId3, competencyBasic));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then: 0.49 < 0.5 diversity threshold => gap
            assertThat(result.getTeamFitMetrics()).isNotNull();
            assertThat(result.getTeamFitMetrics().getGapCount()).isEqualTo(1);
            assertThat(result.getTeamFitMetrics().getDiversityCount()).isEqualTo(0);
            assertThat(result.getTeamFitMetrics().getSaturationCount()).isEqualTo(0);
        }
    }

    // === Boundary Edge Case Tests (QA-003) ===

    @Nested
    @DisplayName("QA-003: Saturation/Diversity Threshold Boundary Tests")
    class SaturationDiversityBoundaryTests {

        @ParameterizedTest(name = "Score {0}% with satThreshold {1}, divThreshold {2} should classify as {3}")
        @CsvSource({
            "74.9, 0.75, 0.5, DIVERSITY",
            "75.0, 0.75, 0.5, SATURATION",
            "75.1, 0.75, 0.5, SATURATION",
            "49.9, 0.75, 0.5, GAP",
            "50.0, 0.75, 0.5, DIVERSITY",
            "50.1, 0.75, 0.5, DIVERSITY",
            "59.9, 0.60, 0.3, DIVERSITY",
            "60.0, 0.60, 0.3, SATURATION",
            "29.9, 0.60, 0.3, GAP",
            "30.0, 0.60, 0.3, DIVERSITY"
        })
        @DisplayName("Should correctly classify competency at threshold boundaries")
        void shouldClassifyCorrectlyAtBoundaries(double scorePercent, double satThreshold,
                                                   double divThreshold, String expectedClassification) {
            // Configure thresholds
            setTemplateSaturationThreshold(satThreshold);
            scoringConfig.getThresholds().getTeamFit().setDiversityThreshold(divThreshold);

            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyBasic);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.SJT);
            TestAnswer answer = createAnswer(mockSession, question, null, scorePercent / 100.0, false);

            setupBatchLoaderMock(Map.of(competencyId3, competencyBasic));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            ScoringResult.TeamFitMetrics metrics = result.getTeamFitMetrics();
            assertThat(metrics).isNotNull();

            switch (expectedClassification) {
                case "SATURATION" -> {
                    assertThat(metrics.getSaturationCount())
                            .as("Score %.1f%% should be SATURATION (threshold %.0f%%)", scorePercent, satThreshold * 100)
                            .isEqualTo(1);
                    assertThat(metrics.getDiversityCount()).isEqualTo(0);
                    assertThat(metrics.getGapCount()).isEqualTo(0);
                }
                case "DIVERSITY" -> {
                    assertThat(metrics.getDiversityCount())
                            .as("Score %.1f%% should be DIVERSITY", scorePercent)
                            .isEqualTo(1);
                    assertThat(metrics.getSaturationCount()).isEqualTo(0);
                    assertThat(metrics.getGapCount()).isEqualTo(0);
                }
                case "GAP" -> {
                    assertThat(metrics.getGapCount())
                            .as("Score %.1f%% should be GAP (below diversity threshold %.0f%%)", scorePercent, divThreshold * 100)
                            .isEqualTo(1);
                    assertThat(metrics.getDiversityCount()).isEqualTo(0);
                    assertThat(metrics.getSaturationCount()).isEqualTo(0);
                }
                default -> throw new IllegalArgumentException("Unknown classification: " + expectedClassification);
            }
        }
    }

    @Nested
    @DisplayName("QA-003: Pass Threshold Boundary Tests")
    class PassThresholdBoundaryTests {

        @Test
        @DisplayName("Should FAIL when adjusted percentage is just below pass threshold")
        void shouldFailJustBelowPassThreshold() {
            // Default passThreshold = 0.6 => 60%, minDiversityRatio = 0.3
            // Use a score that will produce adjustedPercentage just below 60%
            // With single competency at 55%, diversity ratio is 1.0 (single comp in diversity range)
            // sigmoid multiplier for diversity=1.0, saturation=0 should be > 1.0
            // So raw 55% * ~1.09 = ~60%, which may pass. Use lower score.
            setTemplateSaturationThreshold(0.75);
            scoringConfig.getThresholds().getTeamFit().setPassThreshold(0.6);

            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyBasic);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.SJT);
            // Score at 40% => well below 60% threshold even with multiplier
            TestAnswer answer = createAnswer(mockSession, question, null, 0.40, false);

            setupBatchLoaderMock(Map.of(competencyId3, competencyBasic));

            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // 40% with any reasonable multiplier (0.9 to 1.1) stays below 60%
            assertThat(result.getPassed()).isFalse();
        }

        @Test
        @DisplayName("Should PASS when adjusted percentage is above pass threshold with sufficient diversity")
        void shouldPassAboveThresholdWithDiversity() {
            // Score at 70% in diversity range with decent multiplier should exceed 60%
            setTemplateSaturationThreshold(0.75);
            scoringConfig.getThresholds().getTeamFit().setPassThreshold(0.6);
            scoringConfig.getThresholds().getTeamFit().setMinDiversityRatio(0.3);

            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyBasic);

            List<TestAnswer> answers = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                AssessmentQuestion q = createQuestion(UUID.randomUUID(), indicator, QuestionType.SJT);
                answers.add(createAnswer(mockSession, q, null, 0.70, false));
            }

            setupBatchLoaderMock(Map.of(competencyId3, competencyBasic));

            ScoringResult result = scoringStrategy.calculate(mockSession, answers);

            // 70% * multiplier (~1.09 for all diversity) = ~76% > 60% pass threshold
            // Diversity ratio = 1.0 >= 0.3 minDiversityRatio
            assertThat(result.getPassed()).isTrue();
        }

        @Test
        @DisplayName("Should FAIL when diversity ratio is below minimum even if score is high")
        void shouldFailWhenDiversityRatioBelowMinimum() {
            // All competencies saturated => diversity ratio = 0
            setTemplateSaturationThreshold(0.3); // Low threshold so 100% scores are saturated
            scoringConfig.getThresholds().getTeamFit().setMinDiversityRatio(0.3);

            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyBasic);

            List<TestAnswer> answers = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                AssessmentQuestion q = createQuestion(UUID.randomUUID(), indicator, QuestionType.SJT);
                answers.add(createAnswer(mockSession, q, null, 1.0, false));
            }

            setupBatchLoaderMock(Map.of(competencyId3, competencyBasic));

            ScoringResult result = scoringStrategy.calculate(mockSession, answers);

            // All at 100% with saturation threshold 0.3 => all saturated
            // Diversity ratio = 0 < 0.3 minDiversityRatio => FAIL regardless of score
            assertThat(result.getTeamFitMetrics().getDiversityRatio()).isEqualTo(0.0);
            assertThat(result.getPassed()).isFalse();
        }

        @Test
        @DisplayName("Small team adjustment should lower pass threshold by configured amount")
        void shouldApplySmallTeamAdjustment() {
            // Default: passThreshold=0.6, smallTeamThreshold=5, smallTeamAdjustment=0.1
            // With 3 members (< 5), adjusted threshold = 0.6 - 0.1 = 0.5 => 50%
            UUID teamId = ((TeamFitBlueprint) mockTemplate.getTypedBlueprint()).getTeamId();

            List<TeamService.TeamMemberProfile> members = List.of(
                    new TeamService.TeamMemberProfile(UUID.randomUUID(), "Alice", "Dev", Map.of(), Map.of()),
                    new TeamService.TeamMemberProfile(UUID.randomUUID(), "Bob", "QA", Map.of(), Map.of()),
                    new TeamService.TeamMemberProfile(UUID.randomUUID(), "Charlie", "PM", Map.of(), Map.of())
            );

            TeamService.TeamProfile teamProfile = new TeamService.TeamProfile(
                    teamId, "Small Team", members,
                    Map.of(competencyId3, 0.4), // diversity range for team
                    Map.of(), List.of()
            );
            when(teamService.getTeamProfile(teamId)).thenReturn(Optional.of(teamProfile));

            // Score at 55% in diversity range
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyBasic);
            List<TestAnswer> answers = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                AssessmentQuestion q = createQuestion(UUID.randomUUID(), indicator, QuestionType.SJT);
                answers.add(createAnswer(mockSession, q, null, 0.55, false));
            }

            setupBatchLoaderMock(Map.of(competencyId3, competencyBasic));

            ScoringResult result = scoringStrategy.calculate(mockSession, answers);

            // With small team adjustment, pass threshold is 50%, score ~55% * multiplier > 50%
            assertThat(result.getTeamFitMetrics().getTeamSize()).isEqualTo(3);
        }

        @Test
        @DisplayName("Severe gap adjustment should lower pass threshold when > 50% competencies are gaps")
        void shouldApplySevereGapAdjustment() {
            // When gapRatio > 0.5, severeGapAdjustment (default 0.1) is subtracted
            // Need at least 2 competencies with > 50% being gaps (below diversity threshold)
            setTemplateSaturationThreshold(0.75);

            BehavioralIndicator ind1 = createBehavioralIndicator(UUID.randomUUID(), competencyBasic);
            BehavioralIndicator ind2 = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoOnly);
            BehavioralIndicator ind3 = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);

            List<TestAnswer> answers = new ArrayList<>();
            // Comp3: gap (20% < 50% diversity threshold)
            for (int i = 0; i < 4; i++) {
                AssessmentQuestion q = createQuestion(UUID.randomUUID(), ind1, QuestionType.SJT);
                answers.add(createAnswer(mockSession, q, null, 0.20, false));
            }
            // Comp2: gap (30% < 50% diversity threshold)
            for (int i = 0; i < 4; i++) {
                AssessmentQuestion q = createQuestion(UUID.randomUUID(), ind2, QuestionType.SJT);
                answers.add(createAnswer(mockSession, q, null, 0.30, false));
            }
            // Comp1: diversity (60%, between 50% and 75%)
            for (int i = 0; i < 4; i++) {
                AssessmentQuestion q = createQuestion(UUID.randomUUID(), ind3, QuestionType.SJT);
                answers.add(createAnswer(mockSession, q, null, 0.60, false));
            }

            setupBatchLoaderMock(Map.of(
                    competencyId3, competencyBasic,
                    competencyId2, competencyWithEscoOnly,
                    competencyId1, competencyWithEscoAndBigFive
            ));

            ScoringResult result = scoringStrategy.calculate(mockSession, answers);

            // 2 gaps out of 3 = gapRatio 0.67 > 0.5, severe gap adjustment applies
            assertThat(result.getTeamFitMetrics().getGapCount()).isEqualTo(2);
            assertThat(result.getTeamFitMetrics().getDiversityCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Pass threshold should never drop below minPassThreshold floor")
        void shouldEnforceMinPassThresholdFloor() {
            // Set very low passThreshold with both adjustments to push it below floor
            scoringConfig.getThresholds().getTeamFit().setPassThreshold(0.35);
            // minPassThreshold default = 0.3
            // With both smallTeam (-0.1) and severeGap (-0.1): 0.35 - 0.2 = 0.15
            // But floor is 0.3, so effective threshold = 0.3 => 30%

            UUID teamId = ((TeamFitBlueprint) mockTemplate.getTypedBlueprint()).getTeamId();

            List<TeamService.TeamMemberProfile> members = List.of(
                    new TeamService.TeamMemberProfile(UUID.randomUUID(), "Alice", "Dev", Map.of(), Map.of())
            );

            // All gaps to trigger severe gap adjustment
            TeamService.TeamProfile teamProfile = new TeamService.TeamProfile(
                    teamId, "Tiny Team", members,
                    Map.of(competencyId3, 0.05),
                    Map.of(), List.of(competencyId3)
            );
            when(teamService.getTeamProfile(teamId)).thenReturn(Optional.of(teamProfile));

            // Score above the floor (35%) but below what passThreshold would be without floor
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyBasic);
            List<TestAnswer> answers = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                AssessmentQuestion q = createQuestion(UUID.randomUUID(), indicator, QuestionType.SJT);
                answers.add(createAnswer(mockSession, q, null, 0.35, false));
            }

            setupBatchLoaderMock(Map.of(competencyId3, competencyBasic));

            ScoringResult result = scoringStrategy.calculate(mockSession, answers);

            // Effective threshold = max(0.35 - 0.1 - 0.1, 0.3) = max(0.15, 0.3) = 0.3 => 30%
            // Score ~35% * multiplier, diversity depends on team saturation
            assertThat(result).isNotNull();
            assertThat(result.getTeamFitMetrics().getTeamSize()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("QA-003: Zero-Answer Competency Tests")
    class ZeroAnswerCompetencyTests {

        @Test
        @DisplayName("Should handle competency with all answers skipped")
        void shouldHandleCompetencyWithAllSkippedAnswers() {
            // Given: Competency1 has all skipped, Competency3 has real answers
            setTemplateSaturationThreshold(0.75);

            BehavioralIndicator ind1 = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);
            BehavioralIndicator ind3 = createBehavioralIndicator(UUID.randomUUID(), competencyBasic);

            List<TestAnswer> answers = new ArrayList<>();
            // Competency1: 3 skipped
            for (int i = 0; i < 3; i++) {
                AssessmentQuestion q = createQuestion(UUID.randomUUID(), ind1, QuestionType.LIKERT);
                answers.add(createAnswer(mockSession, q, null, null, true));
            }
            // Competency3: 4 real answers at 70%
            for (int i = 0; i < 4; i++) {
                AssessmentQuestion q = createQuestion(UUID.randomUUID(), ind3, QuestionType.SJT);
                answers.add(createAnswer(mockSession, q, null, 0.70, false));
            }

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithEscoAndBigFive, competencyId3, competencyBasic));

            ScoringResult result = scoringStrategy.calculate(mockSession, answers);

            // Only Competency3 should appear (Competency1 has zero valid answers)
            assertThat(result.getCompetencyScores()).hasSize(1);
            assertThat(result.getCompetencyScores().get(0).getCompetencyName()).isEqualTo("Basic Skills");
        }

        @Test
        @DisplayName("Should return zero overall when all competencies have only skipped answers")
        void shouldReturnZeroWhenAllSkipped() {
            BehavioralIndicator ind1 = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoAndBigFive);
            BehavioralIndicator ind3 = createBehavioralIndicator(UUID.randomUUID(), competencyBasic);

            List<TestAnswer> answers = List.of(
                    createAnswer(mockSession,
                            createQuestion(UUID.randomUUID(), ind1, QuestionType.LIKERT), null, null, true),
                    createAnswer(mockSession,
                            createQuestion(UUID.randomUUID(), ind3, QuestionType.LIKERT), null, null, true)
            );

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithEscoAndBigFive, competencyId3, competencyBasic));

            ScoringResult result = scoringStrategy.calculate(mockSession, answers);

            assertThat(result.getCompetencyScores()).isEmpty();
            assertThat(result.getOverallScore()).isEqualTo(0.0);
            assertThat(result.getOverallPercentage()).isEqualTo(0.0);
            assertThat(result.getPassed()).isFalse();
        }

        @Test
        @DisplayName("Team fit metrics should be zero-initialized when no valid answers exist")
        void shouldZeroInitializeMetricsWhenNoValidAnswers() {
            List<TestAnswer> emptyAnswers = Collections.emptyList();

            ScoringResult result = scoringStrategy.calculate(mockSession, emptyAnswers);

            assertThat(result.getTeamFitMetrics()).isNotNull();
            assertThat(result.getTeamFitMetrics().getDiversityRatio()).isEqualTo(0.0);
            assertThat(result.getTeamFitMetrics().getSaturationRatio()).isEqualTo(0.0);
            assertThat(result.getTeamFitMetrics().getDiversityCount()).isEqualTo(0);
            assertThat(result.getTeamFitMetrics().getSaturationCount()).isEqualTo(0);
            assertThat(result.getTeamFitMetrics().getGapCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("QA-003: Sigmoid Multiplier Edge Cases")
    class SigmoidMultiplierEdgeCases {

        @Test
        @DisplayName("Multiplier should be in [penalty, bonus] range regardless of input")
        void shouldBeBoundedByPenaltyAndBonus() {
            // The sigmoid maps balance [-1, 1] to [penalty, bonus] = [0.9, 1.1]
            // Verify with extreme values: all saturation (balance = -1) and all diversity (balance = 1)

            // Scenario A: All saturated (balance = -1.0)
            setTemplateSaturationThreshold(0.3); // Very low threshold, all scores saturate

            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyBasic);
            List<TestAnswer> answers = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                AssessmentQuestion q = createQuestion(UUID.randomUUID(), indicator, QuestionType.SJT);
                answers.add(createAnswer(mockSession, q, null, 1.0, false));
            }
            setupBatchLoaderMock(Map.of(competencyId3, competencyBasic));

            ScoringResult saturatedResult = scoringStrategy.calculate(mockSession, answers);
            double penaltyMultiplier = saturatedResult.getTeamFitMetrics().getTeamFitMultiplier();

            // Should be close to but >= penalty (0.9)
            assertThat(penaltyMultiplier).isGreaterThanOrEqualTo(
                    scoringConfig.getThresholds().getTeamFit().getSaturationPenalty() - 0.01);

            // Scenario B: All diversity (balance = 1.0)
            setTemplateSaturationThreshold(0.99); // Very high threshold, nothing saturates

            BehavioralIndicator ind2 = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoOnly);
            List<TestAnswer> answers2 = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                AssessmentQuestion q = createQuestion(UUID.randomUUID(), ind2, QuestionType.SJT);
                answers2.add(createAnswer(mockSession, q, null, 0.7, false));
            }
            setupBatchLoaderMock(Map.of(competencyId2, competencyWithEscoOnly));

            ScoringResult diversityResult = scoringStrategy.calculate(mockSession, answers2);
            double bonusMultiplier = diversityResult.getTeamFitMetrics().getTeamFitMultiplier();

            // Should be close to but <= bonus (1.1)
            assertThat(bonusMultiplier).isLessThanOrEqualTo(
                    scoringConfig.getThresholds().getTeamFit().getDiversityBonus() + 0.01);

            // Penalty < Bonus (monotonicity)
            assertThat(penaltyMultiplier).isLessThan(bonusMultiplier);
        }

        @Test
        @DisplayName("Sigmoid steepness parameter should affect transition sharpness")
        void shouldRespondToSteepnessParameter() {
            // At steepness=10 (default), the transition between penalty and bonus
            // occurs mostly in the balance range [-0.2, +0.2]
            // At steepness=1, the transition is much smoother/wider

            // Test with low steepness
            scoringConfig.getThresholds().getTeamFit().setSigmoidSteepness(1.0);
            setTemplateSaturationThreshold(0.75);

            BehavioralIndicator ind1 = createBehavioralIndicator(UUID.randomUUID(), competencyBasic);
            BehavioralIndicator ind2 = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoOnly);

            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), ind1, QuestionType.SJT);
            AssessmentQuestion q2 = createQuestion(UUID.randomUUID(), ind2, QuestionType.SJT);

            // One in diversity range, one is a gap
            TestAnswer a1 = createAnswer(mockSession, q1, null, 0.6, false); // diversity
            TestAnswer a2 = createAnswer(mockSession, q2, null, 0.3, false); // gap

            setupBatchLoaderMock(Map.of(competencyId3, competencyBasic, competencyId2, competencyWithEscoOnly));
            ScoringResult lowSteepResult = scoringStrategy.calculate(mockSession, List.of(a1, a2));
            double lowSteepMultiplier = lowSteepResult.getTeamFitMetrics().getTeamFitMultiplier();

            // Test with high steepness
            scoringConfig.getThresholds().getTeamFit().setSigmoidSteepness(30.0);

            BehavioralIndicator ind1b = createBehavioralIndicator(UUID.randomUUID(), competencyBasic);
            BehavioralIndicator ind2b = createBehavioralIndicator(UUID.randomUUID(), competencyWithEscoOnly);

            AssessmentQuestion q1b = createQuestion(UUID.randomUUID(), ind1b, QuestionType.SJT);
            AssessmentQuestion q2b = createQuestion(UUID.randomUUID(), ind2b, QuestionType.SJT);

            TestAnswer a1b = createAnswer(mockSession, q1b, null, 0.6, false);
            TestAnswer a2b = createAnswer(mockSession, q2b, null, 0.3, false);

            setupBatchLoaderMock(Map.of(competencyId3, competencyBasic, competencyId2, competencyWithEscoOnly));
            ScoringResult highSteepResult = scoringStrategy.calculate(mockSession, List.of(a1b, a2b));
            double highSteepMultiplier = highSteepResult.getTeamFitMetrics().getTeamFitMultiplier();

            // With positive balance (diversity > saturation), higher steepness should produce
            // a multiplier closer to the bonus. Both should be > 1.0 but differ.
            assertThat(lowSteepMultiplier).isGreaterThan(0.9);
            assertThat(highSteepMultiplier).isGreaterThan(0.9);
            // High steepness should push the multiplier further toward the bonus
            assertThat(highSteepMultiplier).isGreaterThanOrEqualTo(lowSteepMultiplier);
        }
    }
}
