package app.skillsoft.assessmentbackend.services.scoring.impl;

import app.skillsoft.assessmentbackend.config.ScoringConfiguration;
import app.skillsoft.assessmentbackend.domain.dto.CompetencyScoreDto;
import app.skillsoft.assessmentbackend.domain.dto.StandardCodesDto;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.JobFitBlueprint;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.services.scoring.CompetencyBatchLoader;
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
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JobFitScoringStrategy.
 *
 * Tests cover:
 * - Strictness-adjusted threshold calculations
 * - O*NET weight boost for competencies with O*NET codes (1.2x weight)
 * - Pass/fail determination based on effective threshold
 * - Empty answer list handling
 * - Blueprint extraction from template
 * - Edge cases: single answer, all max scores, all min scores
 * - Weighted percentage calculations
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Job Fit Scoring Strategy Tests")
class JobFitScoringStrategyTest {

    @Mock
    private CompetencyBatchLoader competencyBatchLoader;

    @Mock
    private ScoreNormalizer scoreNormalizer;

    private ScoringConfiguration scoringConfig;
    private JobFitScoringStrategy scoringStrategy;

    private TestSession mockSession;
    private TestTemplate mockTemplate;
    private UUID competencyId1;
    private UUID competencyId2;
    private Competency competencyWithOnet;
    private Competency competencyWithoutOnet;

    @BeforeEach
    void setUp() {
        // Initialize ScoringConfiguration with default values
        scoringConfig = new ScoringConfiguration();

        // Create the strategy with all dependencies
        scoringStrategy = new JobFitScoringStrategy(
                competencyBatchLoader,
                scoringConfig,
                scoreNormalizer
        );

        // Set up UUIDs
        competencyId1 = UUID.randomUUID();
        competencyId2 = UUID.randomUUID();

        // Set up competencies - one with O*NET code, one without
        competencyWithOnet = createCompetency(competencyId1, "Technical Skills", "2.B.1.a");
        competencyWithoutOnet = createCompetency(competencyId2, "Soft Skills", null);

        // Set up mock template with JobFitBlueprint
        mockTemplate = new TestTemplate();
        mockTemplate.setId(UUID.randomUUID());
        mockTemplate.setName("Job Fit Assessment");
        mockTemplate.setGoal(AssessmentGoal.JOB_FIT);

        JobFitBlueprint blueprint = new JobFitBlueprint();
        blueprint.setOnetSocCode("15-1252.00");
        blueprint.setStrictnessLevel(50); // Default strictness
        mockTemplate.setTypedBlueprint(blueprint);

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
        competency.setCategory(CompetencyCategory.COGNITIVE);
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

    private void setTemplateStrictnessLevel(int strictnessLevel) {
        JobFitBlueprint blueprint = new JobFitBlueprint();
        blueprint.setOnetSocCode("15-1252.00");
        blueprint.setStrictnessLevel(strictnessLevel);
        mockTemplate.setTypedBlueprint(blueprint);
    }

    /**
     * Sets up the CompetencyBatchLoader and ScoreNormalizer mocks.
     */
    private void setupBatchLoaderMock(Map<UUID, Competency> competencyMap) {
        when(competencyBatchLoader.loadCompetenciesForAnswers(anyList()))
            .thenReturn(competencyMap);

        when(competencyBatchLoader.extractCompetencyIdSafe(any(TestAnswer.class)))
            .thenAnswer(invocation -> {
                TestAnswer answer = invocation.getArgument(0);
                if (answer == null || answer.getQuestion() == null
                    || answer.getQuestion().getBehavioralIndicator() == null
                    || answer.getQuestion().getBehavioralIndicator().getCompetency() == null) {
                    return Optional.empty();
                }
                return Optional.of(answer.getQuestion().getBehavioralIndicator().getCompetency().getId());
            });

        when(competencyBatchLoader.getFromCache(any(), any()))
            .thenAnswer(invocation -> {
                Map<UUID, Competency> cache = invocation.getArgument(0);
                UUID competencyId = invocation.getArgument(1);
                return cache != null ? cache.get(competencyId) : null;
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

        when(competencyBatchLoader.extractCompetencyIdSafe(any(TestAnswer.class)))
            .thenReturn(Optional.of(competencyId));

        when(competencyBatchLoader.getFromCache(any(), eq(competencyId)))
            .thenReturn(null);

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
        @DisplayName("Should return JOB_FIT as supported goal")
        void shouldReturnJobFitGoal() {
            // When
            AssessmentGoal goal = scoringStrategy.getSupportedGoal();

            // Then
            assertThat(goal).isEqualTo(AssessmentGoal.JOB_FIT);
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
            assertThat(result.getGoal()).isEqualTo(AssessmentGoal.JOB_FIT);
            assertThat(result.getPassed()).isFalse(); // Score below threshold
        }

        @Test
        @DisplayName("Should return zero score when all answers are skipped")
        void shouldReturnZeroScoreWhenAllSkipped() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithOnet);
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
    @DisplayName("Strictness Level Threshold Tests")
    class StrictnessThresholdTests {

        @ParameterizedTest(name = "Strictness {0} should yield threshold {1}%")
        @CsvSource({
            "0, 50",    // Base threshold: 0.5 + (0/100)*0.3 = 0.5 = 50%
            "50, 65",   // 0.5 + (50/100)*0.3 = 0.65 = 65%
            "100, 80"   // 0.5 + (100/100)*0.3 = 0.8 = 80%
        })
        @DisplayName("Should calculate effective threshold based on strictness level")
        void shouldCalculateEffectiveThreshold(int strictnessLevel, double expectedThresholdPercent) {
            // Given
            setTemplateStrictnessLevel(strictnessLevel);

            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithOnet);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);

            // Score that's between thresholds (e.g., 70%) - will pass at low strictness, fail at high
            TestAnswer answer = createAnswer(mockSession, question, 4, null, false); // 0.75 = 75%

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithOnet));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            // Verify pass/fail based on effective threshold
            double scorePercent = result.getOverallPercentage();
            double effectiveThreshold = expectedThresholdPercent / 100.0;
            boolean shouldPass = (scorePercent / 100.0) >= effectiveThreshold;
            assertThat(result.getPassed()).isEqualTo(shouldPass);
        }

        @Test
        @DisplayName("Should pass with high score at maximum strictness")
        void shouldPassWithHighScoreAtMaxStrictness() {
            // Given: Strictness 100 = 80% threshold
            setTemplateStrictnessLevel(100);

            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithOnet);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(mockSession, question, 5, null, false); // 100%

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithOnet));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            assertThat(result.getPassed()).isTrue();
        }

        @Test
        @DisplayName("Should fail with borderline score at high strictness")
        void shouldFailWithBorderlineScoreAtHighStrictness() {
            // Given: Strictness 100 = 80% threshold, score at 75%
            setTemplateStrictnessLevel(100);

            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithOnet);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(mockSession, question, 4, null, false); // 75%

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithOnet));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            assertThat(result.getPassed()).isFalse(); // 75% < 80% threshold
        }

        @Test
        @DisplayName("Should pass with low score at minimum strictness")
        void shouldPassWithLowScoreAtMinStrictness() {
            // Given: Strictness 0 = 50% threshold
            setTemplateStrictnessLevel(0);

            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithOnet);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(mockSession, question, 3, null, false); // 50%

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithOnet));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            assertThat(result.getPassed()).isTrue(); // 50% >= 50% threshold
        }

        @Test
        @DisplayName("Should use default strictness 50 when blueprint is null")
        void shouldUseDefaultStrictnessWhenBlueprintIsNull() {
            // Given: No blueprint
            mockTemplate.setTypedBlueprint(null);
            mockTemplate.setBlueprint(null);

            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithOnet);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(mockSession, question, 4, null, false); // 75%

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithOnet));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            // Default strictness 50 = 65% threshold, 75% >= 65% should pass
            assertThat(result.getPassed()).isTrue();
        }
    }

    @Nested
    @DisplayName("O*NET Weight Boost Tests")
    class OnetWeightBoostTests {

        @Test
        @DisplayName("Should apply 1.2x weight to competencies with O*NET code")
        void shouldApplyOnetWeightBoost() {
            // Given: Two competencies, one with O*NET code (1.2x), one without (1.0x)
            BehavioralIndicator indWithOnet = createBehavioralIndicator(UUID.randomUUID(), competencyWithOnet);
            BehavioralIndicator indWithoutOnet = createBehavioralIndicator(UUID.randomUUID(), competencyWithoutOnet);

            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), indWithOnet, QuestionType.LIKERT);
            AssessmentQuestion q2 = createQuestion(UUID.randomUUID(), indWithoutOnet, QuestionType.LIKERT);

            // Both get same score: Likert 5 = 100%
            TestAnswer a1 = createAnswer(mockSession, q1, 5, null, false);
            TestAnswer a2 = createAnswer(mockSession, q2, 5, null, false);

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithOnet, competencyId2, competencyWithoutOnet));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(a1, a2));

            // Then
            // Both at 100%, weighted: (100*1.2 + 100*1.0) / (1.2 + 1.0) = 220/2.2 = 100%
            assertThat(result.getOverallPercentage()).isCloseTo(100.0, within(0.1));
            assertThat(result.getCompetencyScores()).hasSize(2);

            // Verify O*NET code is set in DTO
            Optional<CompetencyScoreDto> onetScore = result.getCompetencyScores().stream()
                .filter(s -> s.getOnetCode() != null)
                .findFirst();
            assertThat(onetScore).isPresent();
            assertThat(onetScore.get().getOnetCode()).isEqualTo("2.B.1.a");
        }

        @Test
        @DisplayName("Should weight O*NET competencies higher in overall calculation")
        void shouldWeightOnetCompetenciesHigher() {
            // Given: O*NET competency at 100%, non-O*NET at 50%
            BehavioralIndicator indWithOnet = createBehavioralIndicator(UUID.randomUUID(), competencyWithOnet);
            BehavioralIndicator indWithoutOnet = createBehavioralIndicator(UUID.randomUUID(), competencyWithoutOnet);

            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), indWithOnet, QuestionType.LIKERT);
            AssessmentQuestion q2 = createQuestion(UUID.randomUUID(), indWithoutOnet, QuestionType.LIKERT);

            TestAnswer a1 = createAnswer(mockSession, q1, 5, null, false); // 100%
            TestAnswer a2 = createAnswer(mockSession, q2, 3, null, false); // 50%

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithOnet, competencyId2, competencyWithoutOnet));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(a1, a2));

            // Then
            // Weighted: (100*1.2 + 50*1.0) / (1.2 + 1.0) = 170/2.2 = 77.27%
            // Without weighting would be: (100 + 50) / 2 = 75%
            assertThat(result.getOverallPercentage()).isCloseTo(77.27, within(0.5));
        }

        @Test
        @DisplayName("Should handle all competencies without O*NET codes")
        void shouldHandleAllCompetenciesWithoutOnet() {
            // Given: Only competency without O*NET
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithoutOnet);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(mockSession, question, 4, null, false); // 75%

            setupBatchLoaderMock(Map.of(competencyId2, competencyWithoutOnet));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            assertThat(result.getOverallPercentage()).isCloseTo(75.0, within(0.1));
            assertThat(result.getCompetencyScores().get(0).getOnetCode()).isNull();
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
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithOnet);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(mockSession, question, likertValue, null, false);

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithOnet));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getScore()).isCloseTo(expectedNormalized, within(0.001));
        }

        @Test
        @DisplayName("Should clamp Likert values to valid range")
        void shouldClampLikertValues() {
            // Given: Likert value 0 (invalid, should clamp to 1)
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithOnet);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(mockSession, question, 0, null, false);

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithOnet));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getScore()).isCloseTo(0.0, within(0.001)); // Clamped to 1, normalized to 0
        }
    }

    @Nested
    @DisplayName("Score Fallback Tests")
    class ScoreFallbackTests {

        @Test
        @DisplayName("Should use score field when Likert value is null")
        void shouldUseScoreWhenLikertNull() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithOnet);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.SJT);
            TestAnswer answer = createAnswer(mockSession, question, null, 0.8, false);

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithOnet));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getScore()).isCloseTo(0.8, within(0.001));
        }

        @Test
        @DisplayName("Should clamp score to 0-1 range")
        void shouldClampScoreToValidRange() {
            // Given: Score above 1.0
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithOnet);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.SJT);
            TestAnswer answer = createAnswer(mockSession, question, null, 1.5, false);

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithOnet));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getScore()).isCloseTo(1.0, within(0.001));
        }

        @Test
        @DisplayName("Should return 0 for MCQ without score")
        void shouldReturnZeroForMcqWithoutScore() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithOnet);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.MULTIPLE_CHOICE);
            TestAnswer answer = createAnswer(mockSession, question, null, null, false);

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithOnet));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getScore()).isCloseTo(0.0, within(0.001));
        }

        @Test
        @DisplayName("Should default to 0 when no score available")
        void shouldDefaultToZeroWhenNoScore() {
            // Given: No Likert, no score
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithOnet);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.OPEN_TEXT);
            TestAnswer answer = createAnswer(mockSession, question, null, null, false);

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithOnet));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getScore()).isCloseTo(0.0, within(0.001));
        }
    }

    @Nested
    @DisplayName("Blueprint Extraction Tests")
    class BlueprintExtractionTests {

        @Test
        @DisplayName("Should extract from typed blueprint")
        void shouldExtractFromTypedBlueprint() {
            // Given
            JobFitBlueprint blueprint = new JobFitBlueprint();
            blueprint.setOnetSocCode("15-1252.00");
            blueprint.setStrictnessLevel(75);
            mockTemplate.setTypedBlueprint(blueprint);

            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithOnet);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(mockSession, question, 5, null, false); // 100%

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithOnet));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            // Strictness 75 = 72.5% threshold, 100% should pass
            assertThat(result.getPassed()).isTrue();
        }

        @Test
        @DisplayName("Should fall back to legacy blueprint map")
        void shouldFallbackToLegacyBlueprint() {
            // Given
            mockTemplate.setTypedBlueprint(null);
            Map<String, Object> legacyBlueprint = new HashMap<>();
            legacyBlueprint.put("onetSocCode", "15-1252.00");
            legacyBlueprint.put("strictnessLevel", 30);
            mockTemplate.setBlueprint(legacyBlueprint);

            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithOnet);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(mockSession, question, 3, null, false); // 50%

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithOnet));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            // Strictness 30 = 59% threshold, 50% < 59% should fail
            assertThat(result.getPassed()).isFalse();
        }

        @Test
        @DisplayName("Should handle null template gracefully")
        void shouldHandleNullTemplateGracefully() {
            // Given
            mockSession.setTemplate(null);

            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithOnet);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(mockSession, question, 4, null, false); // 75%

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithOnet));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            // Uses default strictness 50 = 65% threshold
            assertThat(result.getPassed()).isTrue();
        }
    }

    @Nested
    @DisplayName("Competency Aggregation Tests")
    class CompetencyAggregationTests {

        @Test
        @DisplayName("Should aggregate multiple answers per competency")
        void shouldAggregateMultipleAnswersPerCompetency() {
            // Given: 3 Likert answers for same competency: 3, 4, 5
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithOnet);

            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            AssessmentQuestion q2 = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            AssessmentQuestion q3 = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);

            TestAnswer a1 = createAnswer(mockSession, q1, 3, null, false); // 0.5
            TestAnswer a2 = createAnswer(mockSession, q2, 4, null, false); // 0.75
            TestAnswer a3 = createAnswer(mockSession, q3, 5, null, false); // 1.0

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithOnet));

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
            // Given: 4 questions, average score 0.75
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithOnet);

            List<TestAnswer> answers = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                AssessmentQuestion q = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
                answers.add(createAnswer(mockSession, q, 4, null, false)); // 0.75 each
            }

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithOnet));

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
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithOnet);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(mockSession, question, 5, null, false); // 100%

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithOnet));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            assertThat(result.getCompetencyScores()).hasSize(1);
            assertThat(result.getOverallPercentage()).isCloseTo(100.0, within(0.1));
            assertThat(result.getPassed()).isTrue();
        }

        @Test
        @DisplayName("Should handle all maximum scores")
        void shouldHandleAllMaximumScores() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithOnet);

            List<TestAnswer> answers = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                AssessmentQuestion q = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
                answers.add(createAnswer(mockSession, q, 5, null, false));
            }

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithOnet));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, answers);

            // Then
            assertThat(result.getOverallPercentage()).isCloseTo(100.0, within(0.1));
            assertThat(result.getPassed()).isTrue();
        }

        @Test
        @DisplayName("Should handle all minimum scores")
        void shouldHandleAllMinimumScores() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithOnet);

            List<TestAnswer> answers = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                AssessmentQuestion q = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
                answers.add(createAnswer(mockSession, q, 1, null, false));
            }

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithOnet));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, answers);

            // Then
            assertThat(result.getOverallPercentage()).isCloseTo(0.0, within(0.1));
            assertThat(result.getPassed()).isFalse();
        }

        @Test
        @DisplayName("Should filter out skipped answers")
        void shouldFilterSkippedAnswers() {
            // Given: 2 answered, 1 skipped
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithOnet);

            AssessmentQuestion q1 = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            AssessmentQuestion q2 = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            AssessmentQuestion q3 = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);

            TestAnswer a1 = createAnswer(mockSession, q1, 5, null, false); // 1.0
            TestAnswer a2 = createAnswer(mockSession, q2, 5, null, false); // 1.0
            TestAnswer a3 = createAnswer(mockSession, q3, null, null, true); // Skipped

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithOnet));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(a1, a2, a3));

            // Then
            CompetencyScoreDto score = result.getCompetencyScores().get(0);
            assertThat(score.getQuestionsAnswered()).isEqualTo(2);
            assertThat(score.getPercentage()).isCloseTo(100.0, within(0.1));
        }

        @Test
        @DisplayName("Should handle unknown competency gracefully")
        void shouldHandleUnknownCompetencyGracefully() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithOnet);
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
    @DisplayName("Result Metadata Tests")
    class ResultMetadataTests {

        @Test
        @DisplayName("Should set goal to JOB_FIT in result")
        void shouldSetGoalToJobFitInResult() {
            // Given
            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithOnet);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(mockSession, question, 4, null, false);

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithOnet));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            assertThat(result.getGoal()).isEqualTo(AssessmentGoal.JOB_FIT);
        }

        @Test
        @DisplayName("Should set passed flag based on threshold")
        void shouldSetPassedFlagBasedOnThreshold() {
            // Given: Default strictness 50 = 65% threshold
            setTemplateStrictnessLevel(50);

            BehavioralIndicator indicator = createBehavioralIndicator(UUID.randomUUID(), competencyWithOnet);
            AssessmentQuestion question = createQuestion(UUID.randomUUID(), indicator, QuestionType.LIKERT);
            TestAnswer answer = createAnswer(mockSession, question, 4, null, false); // 75%

            setupBatchLoaderMock(Map.of(competencyId1, competencyWithOnet));

            // When
            ScoringResult result = scoringStrategy.calculate(mockSession, List.of(answer));

            // Then
            assertThat(result.getPassed()).isTrue(); // 75% >= 65%
        }
    }
}
