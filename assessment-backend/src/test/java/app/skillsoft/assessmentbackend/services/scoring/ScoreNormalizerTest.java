package app.skillsoft.assessmentbackend.services.scoring;

import app.skillsoft.assessmentbackend.domain.entities.AssessmentQuestion;
import app.skillsoft.assessmentbackend.domain.entities.QuestionType;
import app.skillsoft.assessmentbackend.domain.entities.TestAnswer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for ScoreNormalizer component.
 *
 * Tests cover:
 * - Likert value normalization (1-5 to 0-1) for LIKERT, LIKERT_SCALE, FREQUENCY_SCALE
 * - SJT score handling (already 0-1) for SJT, SITUATIONAL_JUDGMENT
 * - MCQ score handling (0 or 1) for MCQ, MULTIPLE_CHOICE
 * - Capability assessment normalization for CAPABILITY_ASSESSMENT, PEER_FEEDBACK
 * - Text-based question handling for BEHAVIORAL_EXAMPLE, OPEN_TEXT, SELF_REFLECTION
 * - Null and edge case handling
 * - Skipped answer handling
 *
 * Per ROADMAP.md Section 2.1 - Extract shared ScoreNormalizer component
 */
@DisplayName("ScoreNormalizer Tests")
class ScoreNormalizerTest {

    private ScoreNormalizer scoreNormalizer;

    @BeforeEach
    void setUp() {
        scoreNormalizer = new ScoreNormalizer();
    }

    // Helper methods for creating test answers
    private TestAnswer createLikertAnswer(QuestionType type, Integer likertValue) {
        AssessmentQuestion question = new AssessmentQuestion();
        question.setId(UUID.randomUUID());
        question.setQuestionType(type);

        TestAnswer answer = new TestAnswer();
        answer.setId(UUID.randomUUID());
        answer.setQuestion(question);
        answer.setLikertValue(likertValue);
        answer.setAnsweredAt(LocalDateTime.now());
        answer.setIsSkipped(false);
        return answer;
    }

    private TestAnswer createScoreAnswer(QuestionType type, Double score) {
        AssessmentQuestion question = new AssessmentQuestion();
        question.setId(UUID.randomUUID());
        question.setQuestionType(type);

        TestAnswer answer = new TestAnswer();
        answer.setId(UUID.randomUUID());
        answer.setQuestion(question);
        answer.setScore(score);
        answer.setAnsweredAt(LocalDateTime.now());
        answer.setIsSkipped(false);
        return answer;
    }

    private TestAnswer createSkippedAnswer(QuestionType type) {
        AssessmentQuestion question = new AssessmentQuestion();
        question.setId(UUID.randomUUID());
        question.setQuestionType(type);

        TestAnswer answer = new TestAnswer();
        answer.setId(UUID.randomUUID());
        answer.setQuestion(question);
        answer.setIsSkipped(true);
        return answer;
    }

    private TestAnswer createAnswerWithBothValues(QuestionType type, Integer likertValue, Double score) {
        AssessmentQuestion question = new AssessmentQuestion();
        question.setId(UUID.randomUUID());
        question.setQuestionType(type);

        TestAnswer answer = new TestAnswer();
        answer.setId(UUID.randomUUID());
        answer.setQuestion(question);
        answer.setLikertValue(likertValue);
        answer.setScore(score);
        answer.setAnsweredAt(LocalDateTime.now());
        answer.setIsSkipped(false);
        return answer;
    }

    @Nested
    @DisplayName("Null and Edge Case Handling")
    class NullAndEdgeCaseTests {

        @Test
        @DisplayName("Should return 0.0 for null answer")
        void nullAnswer_returnsZero() {
            double result = scoreNormalizer.normalize(null);
            assertThat(result).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return 0.0 for skipped answer")
        void skippedAnswer_returnsZero() {
            TestAnswer answer = createSkippedAnswer(QuestionType.LIKERT);
            double result = scoreNormalizer.normalize(answer);
            assertThat(result).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return 0.0 when question is null")
        void nullQuestion_returnsZero() {
            TestAnswer answer = new TestAnswer();
            answer.setId(UUID.randomUUID());
            answer.setIsSkipped(false);
            // question is null

            double result = scoreNormalizer.normalize(answer);
            assertThat(result).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return 0.0 when question type is null")
        void nullQuestionType_returnsZero() {
            AssessmentQuestion question = new AssessmentQuestion();
            question.setId(UUID.randomUUID());
            // questionType is null

            TestAnswer answer = new TestAnswer();
            answer.setId(UUID.randomUUID());
            answer.setQuestion(question);
            answer.setIsSkipped(false);

            double result = scoreNormalizer.normalize(answer);
            assertThat(result).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should use fallback score when question type is null but score exists")
        void nullQuestionType_withScore_usesFallback() {
            AssessmentQuestion question = new AssessmentQuestion();
            question.setId(UUID.randomUUID());
            // questionType is null

            TestAnswer answer = new TestAnswer();
            answer.setId(UUID.randomUUID());
            answer.setQuestion(question);
            answer.setScore(0.75);
            answer.setIsSkipped(false);

            double result = scoreNormalizer.normalize(answer);
            assertThat(result).isEqualTo(0.75);
        }
    }

    @Nested
    @DisplayName("Likert Scale Normalization (LIKERT, LIKERT_SCALE, FREQUENCY_SCALE)")
    class LikertScaleTests {

        @ParameterizedTest(name = "Likert value {0} should normalize to {1}")
        @CsvSource({
            "1, 0.0",   // (1-1)/4 = 0.0
            "2, 0.25",  // (2-1)/4 = 0.25
            "3, 0.5",   // (3-1)/4 = 0.5
            "4, 0.75",  // (4-1)/4 = 0.75
            "5, 1.0"    // (5-1)/4 = 1.0
        })
        @DisplayName("Should normalize LIKERT values correctly")
        void likert_normalizesCorrectly(int likertValue, double expected) {
            TestAnswer answer = createLikertAnswer(QuestionType.LIKERT, likertValue);
            double result = scoreNormalizer.normalize(answer);
            assertThat(result).isCloseTo(expected, within(0.001));
        }

        @ParameterizedTest(name = "Likert value {0} should normalize to {1}")
        @CsvSource({
            "1, 0.0",
            "2, 0.25",
            "3, 0.5",
            "4, 0.75",
            "5, 1.0"
        })
        @DisplayName("Should normalize LIKERT_SCALE values correctly")
        void likertScale_normalizesCorrectly(int likertValue, double expected) {
            TestAnswer answer = createLikertAnswer(QuestionType.LIKERT_SCALE, likertValue);
            double result = scoreNormalizer.normalize(answer);
            assertThat(result).isCloseTo(expected, within(0.001));
        }

        @ParameterizedTest(name = "Frequency value {0} should normalize to {1}")
        @CsvSource({
            "1, 0.0",
            "2, 0.25",
            "3, 0.5",
            "4, 0.75",
            "5, 1.0"
        })
        @DisplayName("Should normalize FREQUENCY_SCALE values correctly")
        void frequencyScale_normalizesCorrectly(int likertValue, double expected) {
            TestAnswer answer = createLikertAnswer(QuestionType.FREQUENCY_SCALE, likertValue);
            double result = scoreNormalizer.normalize(answer);
            assertThat(result).isCloseTo(expected, within(0.001));
        }

        @ParameterizedTest(name = "Out-of-range Likert value {0} should clamp and normalize")
        @CsvSource({
            "0, 0.0",    // Clamped to 1, then (1-1)/4 = 0.0
            "-5, 0.0",   // Clamped to 1
            "6, 1.0",    // Clamped to 5, then (5-1)/4 = 1.0
            "100, 1.0"   // Clamped to 5
        })
        @DisplayName("Should clamp out-of-range Likert values")
        void likert_clampsOutOfRangeValues(int likertValue, double expected) {
            TestAnswer answer = createLikertAnswer(QuestionType.LIKERT, likertValue);
            double result = scoreNormalizer.normalize(answer);
            assertThat(result).isCloseTo(expected, within(0.001));
        }

        @Test
        @DisplayName("Should return 0.0 when likertValue is null")
        void likert_nullValue_returnsZero() {
            TestAnswer answer = createLikertAnswer(QuestionType.LIKERT, null);
            double result = scoreNormalizer.normalize(answer);
            assertThat(result).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("SJT Normalization (SJT, SITUATIONAL_JUDGMENT)")
    class SjtTests {

        @ParameterizedTest(name = "SJT score {0} should remain {1}")
        @CsvSource({
            "0.0, 0.0",
            "0.25, 0.25",
            "0.5, 0.5",
            "0.75, 0.75",
            "1.0, 1.0"
        })
        @DisplayName("Should pass through SJT scores in valid range")
        void sjt_passesThroughValidScores(double score, double expected) {
            TestAnswer answer = createScoreAnswer(QuestionType.SJT, score);
            double result = scoreNormalizer.normalize(answer);
            assertThat(result).isCloseTo(expected, within(0.001));
        }

        @ParameterizedTest(name = "SITUATIONAL_JUDGMENT score {0} should remain {1}")
        @CsvSource({
            "0.0, 0.0",
            "0.5, 0.5",
            "1.0, 1.0"
        })
        @DisplayName("Should pass through SITUATIONAL_JUDGMENT scores in valid range")
        void situationalJudgment_passesThroughValidScores(double score, double expected) {
            TestAnswer answer = createScoreAnswer(QuestionType.SITUATIONAL_JUDGMENT, score);
            double result = scoreNormalizer.normalize(answer);
            assertThat(result).isCloseTo(expected, within(0.001));
        }

        @ParameterizedTest(name = "Out-of-range SJT score {0} should clamp to {1}")
        @CsvSource({
            "-0.5, 0.0",  // Clamped to 0
            "-1.0, 0.0",  // Clamped to 0
            "1.5, 1.0",   // Clamped to 1
            "2.0, 1.0"    // Clamped to 1
        })
        @DisplayName("Should clamp out-of-range SJT scores")
        void sjt_clampsOutOfRangeScores(double score, double expected) {
            TestAnswer answer = createScoreAnswer(QuestionType.SJT, score);
            double result = scoreNormalizer.normalize(answer);
            assertThat(result).isCloseTo(expected, within(0.001));
        }

        @Test
        @DisplayName("Should return 0.0 when SJT score is null")
        void sjt_nullScore_returnsZero() {
            TestAnswer answer = createScoreAnswer(QuestionType.SJT, null);
            double result = scoreNormalizer.normalize(answer);
            assertThat(result).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("MCQ Normalization (MCQ, MULTIPLE_CHOICE)")
    class McqTests {

        @Test
        @DisplayName("Should return correct MCQ score (1.0)")
        void mcq_correctAnswer_returnsOne() {
            TestAnswer answer = createScoreAnswer(QuestionType.MCQ, 1.0);
            double result = scoreNormalizer.normalize(answer);
            assertThat(result).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should return incorrect MCQ score (0.0)")
        void mcq_incorrectAnswer_returnsZero() {
            TestAnswer answer = createScoreAnswer(QuestionType.MCQ, 0.0);
            double result = scoreNormalizer.normalize(answer);
            assertThat(result).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return correct MULTIPLE_CHOICE score (1.0)")
        void multipleChoice_correctAnswer_returnsOne() {
            TestAnswer answer = createScoreAnswer(QuestionType.MULTIPLE_CHOICE, 1.0);
            double result = scoreNormalizer.normalize(answer);
            assertThat(result).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should return 0.0 when MCQ score is null")
        void mcq_nullScore_returnsZero() {
            TestAnswer answer = createScoreAnswer(QuestionType.MCQ, null);
            double result = scoreNormalizer.normalize(answer);
            assertThat(result).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should handle partial MCQ scores")
        void mcq_partialScore_returnsPartialValue() {
            TestAnswer answer = createScoreAnswer(QuestionType.MCQ, 0.5);
            double result = scoreNormalizer.normalize(answer);
            assertThat(result).isEqualTo(0.5);
        }
    }

    @Nested
    @DisplayName("Capability Assessment Normalization (CAPABILITY_ASSESSMENT, PEER_FEEDBACK)")
    class CapabilityAssessmentTests {

        @ParameterizedTest(name = "Capability Likert value {0} should normalize to {1}")
        @CsvSource({
            "1, 0.0",
            "2, 0.25",
            "3, 0.5",
            "4, 0.75",
            "5, 1.0"
        })
        @DisplayName("Should normalize CAPABILITY_ASSESSMENT with likertValue")
        void capabilityAssessment_likertValue_normalizesCorrectly(int likertValue, double expected) {
            TestAnswer answer = createLikertAnswer(QuestionType.CAPABILITY_ASSESSMENT, likertValue);
            double result = scoreNormalizer.normalize(answer);
            assertThat(result).isCloseTo(expected, within(0.001));
        }

        @ParameterizedTest(name = "Peer Feedback Likert value {0} should normalize to {1}")
        @CsvSource({
            "1, 0.0",
            "3, 0.5",
            "5, 1.0"
        })
        @DisplayName("Should normalize PEER_FEEDBACK with likertValue")
        void peerFeedback_likertValue_normalizesCorrectly(int likertValue, double expected) {
            TestAnswer answer = createLikertAnswer(QuestionType.PEER_FEEDBACK, likertValue);
            double result = scoreNormalizer.normalize(answer);
            assertThat(result).isCloseTo(expected, within(0.001));
        }

        @Test
        @DisplayName("Should fall back to score when likertValue is null for CAPABILITY_ASSESSMENT")
        void capabilityAssessment_noLikert_usesScore() {
            TestAnswer answer = createScoreAnswer(QuestionType.CAPABILITY_ASSESSMENT, 0.8);
            double result = scoreNormalizer.normalize(answer);
            assertThat(result).isEqualTo(0.8);
        }

        @Test
        @DisplayName("Should fall back to score when likertValue is null for PEER_FEEDBACK")
        void peerFeedback_noLikert_usesScore() {
            TestAnswer answer = createScoreAnswer(QuestionType.PEER_FEEDBACK, 0.6);
            double result = scoreNormalizer.normalize(answer);
            assertThat(result).isEqualTo(0.6);
        }

        @Test
        @DisplayName("Should prefer likertValue over score for CAPABILITY_ASSESSMENT")
        void capabilityAssessment_bothValues_prefersLikert() {
            TestAnswer answer = createAnswerWithBothValues(QuestionType.CAPABILITY_ASSESSMENT, 5, 0.5);
            double result = scoreNormalizer.normalize(answer);
            // Should use likert (5 -> 1.0), not score (0.5)
            assertThat(result).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should return 0.0 when both likertValue and score are null")
        void capabilityAssessment_noValues_returnsZero() {
            AssessmentQuestion question = new AssessmentQuestion();
            question.setId(UUID.randomUUID());
            question.setQuestionType(QuestionType.CAPABILITY_ASSESSMENT);

            TestAnswer answer = new TestAnswer();
            answer.setId(UUID.randomUUID());
            answer.setQuestion(question);
            answer.setAnsweredAt(LocalDateTime.now());
            answer.setIsSkipped(false);
            // Both likertValue and score are null

            double result = scoreNormalizer.normalize(answer);
            assertThat(result).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Text-Based Question Normalization (BEHAVIORAL_EXAMPLE, OPEN_TEXT, SELF_REFLECTION)")
    class TextBasedTests {

        @ParameterizedTest
        @EnumSource(value = QuestionType.class, names = {"BEHAVIORAL_EXAMPLE", "OPEN_TEXT", "SELF_REFLECTION"})
        @DisplayName("Should return score value for text-based questions")
        void textBased_withScore_returnsScore(QuestionType type) {
            TestAnswer answer = createScoreAnswer(type, 0.85);
            double result = scoreNormalizer.normalize(answer);
            assertThat(result).isEqualTo(0.85);
        }

        @ParameterizedTest
        @EnumSource(value = QuestionType.class, names = {"BEHAVIORAL_EXAMPLE", "OPEN_TEXT", "SELF_REFLECTION"})
        @DisplayName("Should return 0.0 when score is null (not yet graded)")
        void textBased_nullScore_returnsZero(QuestionType type) {
            TestAnswer answer = createScoreAnswer(type, null);
            double result = scoreNormalizer.normalize(answer);
            assertThat(result).isEqualTo(0.0);
        }

        @ParameterizedTest
        @EnumSource(value = QuestionType.class, names = {"BEHAVIORAL_EXAMPLE", "OPEN_TEXT", "SELF_REFLECTION"})
        @DisplayName("Should clamp out-of-range scores for text-based questions")
        void textBased_outOfRangeScore_clamps(QuestionType type) {
            TestAnswer answerHigh = createScoreAnswer(type, 1.5);
            double resultHigh = scoreNormalizer.normalize(answerHigh);
            assertThat(resultHigh).isEqualTo(1.0);

            TestAnswer answerLow = createScoreAnswer(type, -0.5);
            double resultLow = scoreNormalizer.normalize(answerLow);
            assertThat(resultLow).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Integration-like Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should handle all question types without throwing exceptions")
        void allQuestionTypes_noExceptions() {
            for (QuestionType type : QuestionType.values()) {
                // Test with likert value
                TestAnswer likertAnswer = createLikertAnswer(type, 3);
                assertThat(scoreNormalizer.normalize(likertAnswer)).isBetween(0.0, 1.0);

                // Test with score
                TestAnswer scoreAnswer = createScoreAnswer(type, 0.5);
                assertThat(scoreNormalizer.normalize(scoreAnswer)).isBetween(0.0, 1.0);

                // Test skipped
                TestAnswer skippedAnswer = createSkippedAnswer(type);
                assertThat(scoreNormalizer.normalize(skippedAnswer)).isEqualTo(0.0);
            }
        }

        @Test
        @DisplayName("Should always return value between 0.0 and 1.0")
        void normalize_alwaysReturnsValidRange() {
            // Test extreme values
            TestAnswer extremeHigh = createLikertAnswer(QuestionType.LIKERT, Integer.MAX_VALUE);
            assertThat(scoreNormalizer.normalize(extremeHigh)).isLessThanOrEqualTo(1.0);

            TestAnswer extremeLow = createLikertAnswer(QuestionType.LIKERT, Integer.MIN_VALUE);
            assertThat(scoreNormalizer.normalize(extremeLow)).isGreaterThanOrEqualTo(0.0);

            TestAnswer extremeScoreHigh = createScoreAnswer(QuestionType.SJT, Double.MAX_VALUE);
            assertThat(scoreNormalizer.normalize(extremeScoreHigh)).isLessThanOrEqualTo(1.0);

            TestAnswer extremeScoreLow = createScoreAnswer(QuestionType.SJT, -Double.MAX_VALUE);
            assertThat(scoreNormalizer.normalize(extremeScoreLow)).isGreaterThanOrEqualTo(0.0);
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 2, 3, 4, 5})
        @DisplayName("Normalized Likert scores should be evenly distributed")
        void likert_evenDistribution(int value) {
            TestAnswer answer = createLikertAnswer(QuestionType.LIKERT, value);
            double result = scoreNormalizer.normalize(answer);
            double expected = (value - 1.0) / 4.0;
            assertThat(result).isCloseTo(expected, within(0.0001));
        }
    }
}
