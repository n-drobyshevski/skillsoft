package app.skillsoft.assessmentbackend.domain.entities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive entity tests for AssessmentQuestion
 * 
 * Tests cover:
 * - Entity construction and initialization
 * - JSONB answer options field serialization/deserialization
 * - Russian text handling in all string fields
 * - Entity relationships with BehavioralIndicator
 * - Validation and constraints
 * - Object lifecycle methods (equals, hashCode, toString)
 * - Business logic and edge cases
 */
@DisplayName("AssessmentQuestion Entity Tests")
class AssessmentQuestionTest {

    private ObjectMapper objectMapper;
    private UUID questionId;
    private UUID behavioralIndicatorId;
    private BehavioralIndicator mockBehavioralIndicator;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        questionId = UUID.randomUUID();
        behavioralIndicatorId = UUID.randomUUID();

        mockBehavioralIndicator = new BehavioralIndicator();
        mockBehavioralIndicator.setId(behavioralIndicatorId);
        mockBehavioralIndicator.setTitle("Test Behavioral Indicator");
    }

    @Nested
    @DisplayName("Constructor and Initialization Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create AssessmentQuestion with default constructor")
        void shouldCreateAssessmentQuestionWithDefaultConstructor() {
            // When
            AssessmentQuestion question = new AssessmentQuestion();

            // Then
            assertThat(question).isNotNull();
            assertThat(question.getId()).isNull();
            assertThat(question.getBehavioralIndicator()).isNull();
            assertThat(question.getQuestionText()).isNull();
            assertThat(question.getQuestionType()).isNull();
            assertThat(question.getAnswerOptions()).isNull();
            assertThat(question.getScoringRubric()).isNull();
            assertThat(question.getTimeLimit()).isNull();
            assertThat(question.getDifficultyLevel()).isNull();
            assertThat(question.isActive()).isFalse();
            assertThat(question.getOrderIndex()).isZero();
        }

        @Test
        @DisplayName("Should create AssessmentQuestion with all parameters constructor")
        void shouldCreateAssessmentQuestionWithAllParametersConstructor() {
            // Given
            List<Map<String, Object>> answerOptions = createLikertScaleAnswerOptions();

            // When
            AssessmentQuestion question = new AssessmentQuestion(
                    questionId,
                    mockBehavioralIndicator,
                    "Test question text",
                    QuestionType.LIKERT_SCALE,
                    answerOptions,
                    "Test scoring rubric",
                    300,
                    DifficultyLevel.INTERMEDIATE,
                    null,
                    true,
                    1
            );

            // Then
            assertThat(question.getId()).isEqualTo(questionId);
            assertThat(question.getBehavioralIndicator()).isEqualTo(mockBehavioralIndicator);
            assertThat(question.getQuestionText()).isEqualTo("Test question text");
            assertThat(question.getQuestionType()).isEqualTo(QuestionType.LIKERT_SCALE);
            assertThat(question.getAnswerOptions()).isEqualTo(answerOptions);
            assertThat(question.getScoringRubric()).isEqualTo("Test scoring rubric");
            assertThat(question.getTimeLimit()).isEqualTo(300);
            assertThat(question.getDifficultyLevel()).isEqualTo(DifficultyLevel.INTERMEDIATE);
            assertThat(question.isActive()).isTrue();
            assertThat(question.getOrderIndex()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should handle null values in constructor gracefully")
        void shouldHandleNullValuesInConstructorGracefully() {
            // When
            AssessmentQuestion question = new AssessmentQuestion(
                    null, null, null, null, null, null, null, null, null, false, 0
            );

            // Then
            assertThat(question).isNotNull();
            assertThat(question.getId()).isNull();
            assertThat(question.getBehavioralIndicator()).isNull();
            assertThat(question.getQuestionText()).isNull();
            assertThat(question.getQuestionType()).isNull();
            assertThat(question.getAnswerOptions()).isNull();
            assertThat(question.getScoringRubric()).isNull();
            assertThat(question.getTimeLimit()).isNull();
            assertThat(question.getDifficultyLevel()).isNull();
            assertThat(question.isActive()).isFalse();
            assertThat(question.getOrderIndex()).isZero();
        }
    }

    @Nested
    @DisplayName("JSONB Answer Options Tests")
    class JsonbAnswerOptionsTests {

        @Test
        @DisplayName("Should handle Likert scale answer options")
        void shouldHandleLikertScaleAnswerOptions() {
            // Given
            AssessmentQuestion question = new AssessmentQuestion();
            List<Map<String, Object>> likertOptions = createLikertScaleAnswerOptions();

            // When
            question.setAnswerOptions(likertOptions);

            // Then
            assertThat(question.getAnswerOptions()).isNotNull();
            assertThat(question.getAnswerOptions()).hasSize(5);
            assertThat(question.getAnswerOptions().get(0)).containsKeys("value", "label", "score");
            assertThat(question.getAnswerOptions().get(0).get("label")).isEqualTo("Полностью не согласен");
            assertThat(question.getAnswerOptions().get(4).get("label")).isEqualTo("Полностью согласен");
        }

        @Test
        @DisplayName("Should handle multiple choice answer options with Russian text")
        void shouldHandleMultipleChoiceAnswerOptionsWithRussianText() {
            // Given
            AssessmentQuestion question = new AssessmentQuestion();
            List<Map<String, Object>> multipleChoiceOptions = createMultipleChoiceAnswerOptions();

            // When
            question.setAnswerOptions(multipleChoiceOptions);

            // Then
            assertThat(question.getAnswerOptions()).isNotNull();
            assertThat(question.getAnswerOptions()).hasSize(4);
            assertThat(question.getAnswerOptions().get(0).get("text")).asString()
                    .contains("Обратиться к руководителю");
            assertThat(question.getAnswerOptions().get(0)).containsKey("isCorrect");
        }

        @Test
        @DisplayName("Should handle situational judgment answer options")
        void shouldHandleSituationalJudgmentAnswerOptions() {
            // Given
            AssessmentQuestion question = new AssessmentQuestion();
            List<Map<String, Object>> situationalOptions = createSituationalJudgmentAnswerOptions();

            // When
            question.setAnswerOptions(situationalOptions);

            // Then
            assertThat(question.getAnswerOptions()).isNotNull();
            assertThat(question.getAnswerOptions()).hasSize(4);
            
            Map<String, Object> firstOption = question.getAnswerOptions().get(0);
            assertThat(firstOption).containsKeys("action", "effectiveness", "explanation");
            assertThat(firstOption.get("action")).asString().contains("Немедленно");
            assertThat(firstOption.get("effectiveness")).isEqualTo(3);
        }

        @Test
        @DisplayName("Should serialize and deserialize complex JSONB answer options")
        void shouldSerializeAndDeserializeComplexJsonbAnswerOptions() throws JsonProcessingException {
            // Given
            AssessmentQuestion question = new AssessmentQuestion();
            List<Map<String, Object>> complexOptions = createComplexAnswerOptions();
            question.setAnswerOptions(complexOptions);

            // When - Serialize to JSON
            String jsonString = objectMapper.writeValueAsString(question.getAnswerOptions());

            // Then - Should contain Russian text and nested structures
            assertThat(jsonString).contains("вариант");
            assertThat(jsonString).contains("подробности");

            // When - Deserialize from JSON
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> deserializedOptions = objectMapper.readValue(jsonString, List.class);

            // Then - Should maintain structure and Russian text
            assertThat(deserializedOptions).hasSize(complexOptions.size());
            assertThat(deserializedOptions.get(0)).containsKey("вариант");
            assertThat(deserializedOptions.get(0)).containsKey("подробности");
        }

        @Test
        @DisplayName("Should handle null and empty answer options")
        void shouldHandleNullAndEmptyAnswerOptions() {
            // Given
            AssessmentQuestion question = new AssessmentQuestion();

            // When - Set null
            question.setAnswerOptions(null);

            // Then
            assertThat(question.getAnswerOptions()).isNull();

            // When - Set empty list
            question.setAnswerOptions(new ArrayList<>());

            // Then
            assertThat(question.getAnswerOptions()).isNotNull();
            assertThat(question.getAnswerOptions()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Russian Text Handling Tests")
    class RussianTextHandlingTests {

        @Test
        @DisplayName("Should handle Russian text in question text")
        void shouldHandleRussianTextInQuestionText() {
            // Given
            AssessmentQuestion question = new AssessmentQuestion();
            String russianQuestionText = "Как часто вы демонстрируете лидерские качества в команде?";

            // When
            question.setQuestionText(russianQuestionText);

            // Then
            assertThat(question.getQuestionText()).isEqualTo(russianQuestionText);
            assertThat(question.getQuestionText()).contains("демонстрируете");
            assertThat(question.getQuestionText()).contains("лидерские");
        }

        @Test
        @DisplayName("Should handle Russian text in scoring rubric")
        void shouldHandleRussianTextInScoringRubric() {
            // Given
            AssessmentQuestion question = new AssessmentQuestion();
            String russianScoringRubric = "Оценка от 1 до 5 баллов на основе качества ответа. " +
                    "5 баллов - превосходный ответ с конкретными примерами. " +
                    "1 балл - неудовлетворительный ответ без обоснования.";

            // When
            question.setScoringRubric(russianScoringRubric);

            // Then
            assertThat(question.getScoringRubric()).isEqualTo(russianScoringRubric);
            assertThat(question.getScoringRubric()).contains("превосходный");
            assertThat(question.getScoringRubric()).contains("неудовлетворительный");
        }

        @Test
        @DisplayName("Should handle mixed Russian and English text")
        void shouldHandleMixedRussianAndEnglishText() {
            // Given
            AssessmentQuestion question = new AssessmentQuestion();
            String mixedText = "Leadership Skills / Лидерские навыки - How often do you demonstrate / " +
                    "Как часто вы демонстрируете инициативу?";

            // When
            question.setQuestionText(mixedText);

            // Then
            assertThat(question.getQuestionText()).isEqualTo(mixedText);
            assertThat(question.getQuestionText()).contains("Leadership Skills");
            assertThat(question.getQuestionText()).contains("Лидерские навыки");
            assertThat(question.getQuestionText()).contains("демонстрируете");
        }
    }

    @Nested
    @DisplayName("Entity Relationship Tests")
    class EntityRelationshipTests {

        @Test
        @DisplayName("Should manage BehavioralIndicator relationship")
        void shouldManageBehavioralIndicatorRelationship() {
            // Given
            AssessmentQuestion question = new AssessmentQuestion();

            // When
            question.setBehavioralIndicator(mockBehavioralIndicator);

            // Then
            assertThat(question.getBehavioralIndicator()).isEqualTo(mockBehavioralIndicator);
            assertThat(question.getBehavioralIndicator().getId()).isEqualTo(behavioralIndicatorId);
        }

        @Test
        @DisplayName("Should handle BehavioralIndicator ID convenience methods")
        void shouldHandleBehavioralIndicatorIdConvenienceMethods() {
            // Given
            AssessmentQuestion question = new AssessmentQuestion();
            UUID testId = UUID.randomUUID();

            // When - Set ID using convenience method
            question.setBehavioralIndicatorId(testId);

            // Then
            assertThat(question.getBehavioralIndicator()).isNotNull();
            assertThat(question.getBehavioralIndicatorId()).isEqualTo(testId);

            // When - BehavioralIndicator is already set
            question.setBehavioralIndicator(mockBehavioralIndicator);
            UUID newId = UUID.randomUUID();
            question.setBehavioralIndicatorId(newId);

            // Then - Should update existing BehavioralIndicator's ID
            assertThat(question.getBehavioralIndicatorId()).isEqualTo(newId);
            assertThat(question.getBehavioralIndicator().getId()).isEqualTo(newId);
        }

        @Test
        @DisplayName("Should handle null BehavioralIndicator gracefully")
        void shouldHandleNullBehavioralIndicatorGracefully() {
            // Given
            AssessmentQuestion question = new AssessmentQuestion();

            // When
            question.setBehavioralIndicator(null);

            // Then
            assertThat(question.getBehavioralIndicator()).isNull();
            assertThat(question.getBehavioralIndicatorId()).isNull();
        }
    }

    @Nested
    @DisplayName("Enum Field Tests")
    class EnumFieldTests {

        @Test
        @DisplayName("Should handle all QuestionType enum values")
        void shouldHandleAllQuestionTypeEnumValues() {
            // Given
            AssessmentQuestion question = new AssessmentQuestion();

            // When & Then - Test all enum values
            for (QuestionType questionType : QuestionType.values()) {
                question.setQuestionType(questionType);
                assertThat(question.getQuestionType()).isEqualTo(questionType);
            }
        }

        @Test
        @DisplayName("Should handle all DifficultyLevel enum values")
        void shouldHandleAllDifficultyLevelEnumValues() {
            // Given
            AssessmentQuestion question = new AssessmentQuestion();

            // When & Then - Test all enum values
            for (DifficultyLevel difficultyLevel : DifficultyLevel.values()) {
                question.setDifficultyLevel(difficultyLevel);
                assertThat(question.getDifficultyLevel()).isEqualTo(difficultyLevel);
            }
        }

        @Test
        @DisplayName("Should handle null enum values")
        void shouldHandleNullEnumValues() {
            // Given
            AssessmentQuestion question = new AssessmentQuestion();

            // When
            question.setQuestionType(null);
            question.setDifficultyLevel(null);

            // Then
            assertThat(question.getQuestionType()).isNull();
            assertThat(question.getDifficultyLevel()).isNull();
        }
    }

    @Nested
    @DisplayName("Object Lifecycle Tests")
    class ObjectLifecycleTests {

        @Test
        @DisplayName("Should implement equals correctly")
        void shouldImplementEqualsCorrectly() {
            // Given
            List<Map<String, Object>> answerOptions = createLikertScaleAnswerOptions();
            
            AssessmentQuestion question1 = new AssessmentQuestion(
                    questionId, mockBehavioralIndicator, "Test question",
                    QuestionType.LIKERT_SCALE, answerOptions, "Test rubric",
                    300, DifficultyLevel.INTERMEDIATE, null, true, 1
            );

            AssessmentQuestion question2 = new AssessmentQuestion(
                    questionId, mockBehavioralIndicator, "Test question",
                    QuestionType.LIKERT_SCALE, answerOptions, "Test rubric",
                    300, DifficultyLevel.INTERMEDIATE, null, true, 1
            );

            // When & Then
            assertThat(question1).isEqualTo(question2);
            assertThat(question1).isEqualTo(question1); // reflexive
            assertThat(question2).isEqualTo(question1); // symmetric
        }

        @Test
        @DisplayName("Should implement equals with different objects correctly")
        void shouldImplementEqualsWithDifferentObjectsCorrectly() {
            // Given
            AssessmentQuestion question1 = new AssessmentQuestion();
            question1.setId(questionId);
            question1.setQuestionText("Question 1");

            AssessmentQuestion question2 = new AssessmentQuestion();
            question2.setId(UUID.randomUUID());
            question2.setQuestionText("Question 2");

            // When & Then
            assertThat(question1).isNotEqualTo(question2);
            assertThat(question1).isNotEqualTo(null);
            assertThat(question1).isNotEqualTo("not a question");
        }

        @Test
        @DisplayName("Should implement hashCode correctly")
        void shouldImplementHashCodeCorrectly() {
            // Given
            List<Map<String, Object>> answerOptions = createLikertScaleAnswerOptions();
            
            AssessmentQuestion question1 = new AssessmentQuestion(
                    questionId, mockBehavioralIndicator, "Test question",
                    QuestionType.LIKERT_SCALE, answerOptions, "Test rubric",
                    300, DifficultyLevel.INTERMEDIATE, null, true, 1
            );

            AssessmentQuestion question2 = new AssessmentQuestion(
                    questionId, mockBehavioralIndicator, "Test question",
                    QuestionType.LIKERT_SCALE, answerOptions, "Test rubric",
                    300, DifficultyLevel.INTERMEDIATE, null, true, 1
            );

            // When & Then
            assertThat(question1.hashCode()).isEqualTo(question2.hashCode());
        }

        @Test
        @DisplayName("Should implement toString correctly")
        void shouldImplementToStringCorrectly() {
            // Given
            AssessmentQuestion question = new AssessmentQuestion();
            question.setId(questionId);
            question.setQuestionText("Test question");
            question.setQuestionType(QuestionType.LIKERT_SCALE);

            // When
            String toString = question.toString();

            // Then
            assertThat(toString).contains("AssessmentQuestion{");
            assertThat(toString).contains("id=" + questionId);
            assertThat(toString).contains("questionText='Test question'");
            assertThat(toString).contains("questionType=LIKERT_SCALE");
        }
    }

    @Nested
    @DisplayName("Business Logic and Edge Cases Tests")
    class BusinessLogicTests {

        @Test
        @DisplayName("Should handle time limits correctly")
        void shouldHandleTimeLimitsCorrectly() {
            // Given
            AssessmentQuestion question = new AssessmentQuestion();

            // When & Then - Valid time limits
            question.setTimeLimit(60);
            assertThat(question.getTimeLimit()).isEqualTo(60);

            question.setTimeLimit(300);
            assertThat(question.getTimeLimit()).isEqualTo(300);

            question.setTimeLimit(null);
            assertThat(question.getTimeLimit()).isNull();
        }

        @Test
        @DisplayName("Should handle order index correctly")
        void shouldHandleOrderIndexCorrectly() {
            // Given
            AssessmentQuestion question = new AssessmentQuestion();

            // When & Then
            question.setOrderIndex(1);
            assertThat(question.getOrderIndex()).isEqualTo(1);

            question.setOrderIndex(0);
            assertThat(question.getOrderIndex()).isZero();

            question.setOrderIndex(-1);
            assertThat(question.getOrderIndex()).isEqualTo(-1);
        }

        @Test
        @DisplayName("Should handle active status correctly")
        void shouldHandleActiveStatusCorrectly() {
            // Given
            AssessmentQuestion question = new AssessmentQuestion();

            // When & Then
            assertThat(question.isActive()).isFalse(); // default

            question.setActive(true);
            assertThat(question.isActive()).isTrue();

            question.setActive(false);
            assertThat(question.isActive()).isFalse();
        }

        @Test
        @DisplayName("Should handle long text fields")
        void shouldHandleLongTextFields() {
            // Given
            AssessmentQuestion question = new AssessmentQuestion();
            String longQuestionText = "Этот очень длинный вопрос предназначен для тестирования того, " +
                    "как система обрабатывает текст, который может превышать обычные ограничения. " +
                    "Важно убедиться, что система может корректно работать с подобными текстами. " +
                    "Данный текст содержит русские символы и проверяет устойчивость системы к длинным строкам.";

            String longScoringRubric = "Подробная система оценки, которая включает в себя множество критериев " +
                    "и подробные объяснения по каждому уровню оценки. Критерии включают качество ответа, " +
                    "использование примеров, логическую структуру, и демонстрацию понимания темы.";

            // When
            question.setQuestionText(longQuestionText);
            question.setScoringRubric(longScoringRubric);

            // Then
            assertThat(question.getQuestionText()).isEqualTo(longQuestionText);
            assertThat(question.getScoringRubric()).isEqualTo(longScoringRubric);
            assertThat(question.getQuestionText().length()).isGreaterThan(100);
            assertThat(question.getScoringRubric().length()).isGreaterThan(100);
        }
    }

    // Helper methods for creating test data
    private List<Map<String, Object>> createLikertScaleAnswerOptions() {
        List<Map<String, Object>> options = new ArrayList<>();
        
        Map<String, Object> option1 = new HashMap<>();
        option1.put("value", 1);
        option1.put("label", "Полностью не согласен");
        option1.put("score", 1);
        options.add(option1);

        Map<String, Object> option2 = new HashMap<>();
        option2.put("value", 2);
        option2.put("label", "Не согласен");
        option2.put("score", 2);
        options.add(option2);

        Map<String, Object> option3 = new HashMap<>();
        option3.put("value", 3);
        option3.put("label", "Нейтрально");
        option3.put("score", 3);
        options.add(option3);

        Map<String, Object> option4 = new HashMap<>();
        option4.put("value", 4);
        option4.put("label", "Согласен");
        option4.put("score", 4);
        options.add(option4);

        Map<String, Object> option5 = new HashMap<>();
        option5.put("value", 5);
        option5.put("label", "Полностью согласен");
        option5.put("score", 5);
        options.add(option5);

        return options;
    }

    private List<Map<String, Object>> createMultipleChoiceAnswerOptions() {
        List<Map<String, Object>> options = new ArrayList<>();

        Map<String, Object> option1 = new HashMap<>();
        option1.put("id", "A");
        option1.put("text", "Обратиться к руководителю за поддержкой");
        option1.put("isCorrect", true);
        options.add(option1);

        Map<String, Object> option2 = new HashMap<>();
        option2.put("id", "B");
        option2.put("text", "Игнорировать проблему");
        option2.put("isCorrect", false);
        options.add(option2);

        Map<String, Object> option3 = new HashMap<>();
        option3.put("id", "C");
        option3.put("text", "Самостоятельно решить вопрос");
        option3.put("isCorrect", false);
        options.add(option3);

        Map<String, Object> option4 = new HashMap<>();
        option4.put("id", "D");
        option4.put("text", "Обсудить с коллегами");
        option4.put("isCorrect", false);
        options.add(option4);

        return options;
    }

    private List<Map<String, Object>> createSituationalJudgmentAnswerOptions() {
        List<Map<String, Object>> options = new ArrayList<>();

        Map<String, Object> option1 = new HashMap<>();
        option1.put("action", "Немедленно вмешаться и остановить конфликт");
        option1.put("effectiveness", 3);
        option1.put("explanation", "Прямое вмешательство может остановить конфликт, но не решит основную проблему");
        options.add(option1);

        Map<String, Object> option2 = new HashMap<>();
        option2.put("action", "Выслушать обе стороны и найти компромисс");
        option2.put("effectiveness", 5);
        option2.put("explanation", "Наиболее эффективный подход для долгосрочного решения");
        options.add(option2);

        Map<String, Object> option3 = new HashMap<>();
        option3.put("action", "Переложить ответственность на руководство");
        option3.put("effectiveness", 2);
        option3.put("explanation", "Избегание ответственности не способствует развитию лидерских качеств");
        options.add(option3);

        Map<String, Object> option4 = new HashMap<>();
        option4.put("action", "Проигнорировать ситуацию");
        option4.put("effectiveness", 1);
        option4.put("explanation", "Игнорирование может усугубить конфликт");
        options.add(option4);

        return options;
    }

    private List<Map<String, Object>> createComplexAnswerOptions() {
        List<Map<String, Object>> options = new ArrayList<>();

        Map<String, Object> option1 = new HashMap<>();
        option1.put("вариант", "Первый вариант");
        
        Map<String, Object> details1 = new HashMap<>();
        details1.put("описание", "Подробное описание варианта");
        details1.put("баллы", 5);
        details1.put("категория", "лидерство");
        
        List<String> keywords1 = Arrays.asList("инициатива", "ответственность", "команда");
        details1.put("ключевые_слова", keywords1);
        
        option1.put("подробности", details1);
        options.add(option1);

        Map<String, Object> option2 = new HashMap<>();
        option2.put("вариант", "Второй вариант");
        
        Map<String, Object> details2 = new HashMap<>();
        details2.put("описание", "Альтернативное решение");
        details2.put("баллы", 3);
        details2.put("категория", "сотрудничество");
        
        List<String> keywords2 = Arrays.asList("компромисс", "диалог", "решение");
        details2.put("ключевые_слова", keywords2);
        
        option2.put("подробности", details2);
        options.add(option2);

        return options;
    }
}