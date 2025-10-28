package app.skillsoft.assessmentbackend.services;

import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.repository.AssessmentQuestionRepository;
import app.skillsoft.assessmentbackend.services.impl.AssessmentQuestionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for AssessmentQuestionService
 * 
 * Tests cover:
 * - CRUD operations with mocked dependencies
 * - JSONB answer options handling and validation
 * - Business logic for question assessment
 * - Russian text processing and validation
 * - Error handling and edge cases
 * - Entity relationship management
 * - Service-level data transformations
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AssessmentQuestion Service Tests")
class AssessmentQuestionServiceTest {

    @Mock
    private AssessmentQuestionRepository assessmentQuestionRepository;

    @InjectMocks
    private AssessmentQuestionServiceImpl assessmentQuestionService;

    private UUID assessmentQuestionId;
    private AssessmentQuestion mockAssessmentQuestion;
    private BehavioralIndicator mockBehavioralIndicator;
    private UUID behavioralIndicatorId;

    @BeforeEach
    void setUp() {
        behavioralIndicatorId = UUID.randomUUID();
        assessmentQuestionId = UUID.randomUUID();

        mockBehavioralIndicator = new BehavioralIndicator();
        mockBehavioralIndicator.setId(behavioralIndicatorId);
        mockBehavioralIndicator.setName("Test Behavioral Indicator");

        mockAssessmentQuestion = new AssessmentQuestion();
        mockAssessmentQuestion.setId(assessmentQuestionId);
        mockAssessmentQuestion.setQuestionText("Test Question");
        mockAssessmentQuestion.setBehavioralIndicator(mockBehavioralIndicator);
        List<Map<String, Object>> answerOptions = new ArrayList<>();
        Map<String, Object> option1 = new HashMap<>();
        option1.put("label", "Option 1");
        option1.put("value", 1);
        answerOptions.add(option1);
        Map<String, Object> option2 = new HashMap<>();
        option2.put("label", "Option 2");
        option2.put("value", 2);
        answerOptions.add(option2);
        mockAssessmentQuestion.setAnswerOptions(answerOptions);
    }

    @Nested
    @DisplayName("List Assessment Questions Tests")
    class ListAssessmentQuestionsTests {

        @Test
        @DisplayName("Should return all assessment questions for behavioral indicator")
        void shouldReturnAllAssessmentQuestionsForBehavioralIndicator() {
            // Given
            List<AssessmentQuestion> expectedQuestions = Arrays.asList(
                    mockAssessmentQuestion,
                    createTestQuestion("Second Question", QuestionType.MULTIPLE_CHOICE, 2),
                    createTestQuestion("Third Question", QuestionType.SITUATIONAL_JUDGMENT, 3)
            );
            when(assessmentQuestionRepository.findByBehavioralIndicatorId(behavioralIndicatorId))
                    .thenReturn(expectedQuestions);

            // When
            List<AssessmentQuestion> actualQuestions = assessmentQuestionService
                    .listIndicatorAssessmentQuestions(behavioralIndicatorId);

            // Then
            assertThat(actualQuestions).isNotNull();
            assertThat(actualQuestions).hasSize(3);
            assertThat(actualQuestions).containsExactlyElementsOf(expectedQuestions);
            
            verify(assessmentQuestionRepository).findByBehavioralIndicatorId(behavioralIndicatorId);
            verifyNoMoreInteractions(assessmentQuestionRepository);
        }

        @Test
        @DisplayName("Should return empty list when no assessment questions exist")
        void shouldReturnEmptyListWhenNoAssessmentQuestionsExist() {
            // Given
            when(assessmentQuestionRepository.findByBehavioralIndicatorId(behavioralIndicatorId))
                    .thenReturn(Collections.emptyList());

            // When
            List<AssessmentQuestion> actualQuestions = assessmentQuestionService
                    .listIndicatorAssessmentQuestions(behavioralIndicatorId);

            // Then
            assertThat(actualQuestions).isNotNull();
            assertThat(actualQuestions).isEmpty();
            
            verify(assessmentQuestionRepository).findByBehavioralIndicatorId(behavioralIndicatorId);
        }

        @Test
        @DisplayName("Should handle null behavioral indicator ID gracefully")
        void shouldHandleNullBehavioralIndicatorIdGracefully() {
            // Given
            UUID nullBehavioralIndicatorId = null;
            when(assessmentQuestionRepository.findByBehavioralIndicatorId(nullBehavioralIndicatorId))
                    .thenReturn(Collections.emptyList());

            // When
            List<AssessmentQuestion> actualQuestions = assessmentQuestionService
                    .listIndicatorAssessmentQuestions(nullBehavioralIndicatorId);

            // Then
            assertThat(actualQuestions).isNotNull();
            assertThat(actualQuestions).isEmpty();
            
            verify(assessmentQuestionRepository).findByBehavioralIndicatorId(nullBehavioralIndicatorId);
        }
    }

    @Nested
    @DisplayName("Create Assessment Question Tests")
    class CreateAssessmentQuestionTests {

        @Test
        @DisplayName("Should create assessment question successfully")
        void shouldCreateAssessmentQuestionSuccessfully() {
            // Given
            AssessmentQuestion newQuestion = new AssessmentQuestion();
            newQuestion.setQuestionText("New question text");
            newQuestion.setQuestionType(QuestionType.MULTIPLE_CHOICE);
            newQuestion.setAnswerOptions(createMultipleChoiceAnswerOptions());
            newQuestion.setScoringRubric("New scoring rubric");
            newQuestion.setTimeLimit(180);
            newQuestion.setDifficultyLevel(DifficultyLevel.FOUNDATIONAL);
            newQuestion.setActive(true);
            newQuestion.setOrderIndex(2);

            when(assessmentQuestionRepository.save(any(AssessmentQuestion.class)))
                    .thenReturn(mockAssessmentQuestion);

            // When
            AssessmentQuestion createdQuestion = assessmentQuestionService
                    .createAssesmentQuestion(behavioralIndicatorId, newQuestion);

            // Then
            assertThat(createdQuestion).isNotNull();
            assertThat(createdQuestion).isEqualTo(mockAssessmentQuestion);
            
            verify(assessmentQuestionRepository).save(argThat(question -> 
                question.getBehavioralIndicatorId().equals(behavioralIndicatorId) &&
                question.getQuestionText().equals("New question text")
            ));
        }

        @Test
        @DisplayName("Should create question with Russian text successfully")
        void shouldCreateQuestionWithRussianTextSuccessfully() {
            // Given
            AssessmentQuestion russianQuestion = new AssessmentQuestion();
            russianQuestion.setQuestionText("Как часто вы демонстрируете лидерские качества?");
            russianQuestion.setQuestionType(QuestionType.FREQUENCY_SCALE);
            russianQuestion.setAnswerOptions(createRussianFrequencyScaleOptions());
            russianQuestion.setScoringRubric("Оценка от 1 до 5 баллов на основе частоты проявления поведения");
            russianQuestion.setDifficultyLevel(DifficultyLevel.INTERMEDIATE);
            russianQuestion.setActive(true);
            russianQuestion.setOrderIndex(1);

            AssessmentQuestion savedRussianQuestion = new AssessmentQuestion();
            savedRussianQuestion.setId(UUID.randomUUID());
            savedRussianQuestion.setQuestionText("Как часто вы демонстрируете лидерские качества?");
            savedRussianQuestion.setScoringRubric("Оценка от 1 до 5 баллов на основе частоты проявления поведения");

            when(assessmentQuestionRepository.save(any(AssessmentQuestion.class)))
                    .thenReturn(savedRussianQuestion);

            // When
            AssessmentQuestion createdQuestion = assessmentQuestionService
                    .createAssesmentQuestion(behavioralIndicatorId, russianQuestion);

            // Then
            assertThat(createdQuestion).isNotNull();
            assertThat(createdQuestion.getQuestionText()).contains("демонстрируете");
            assertThat(createdQuestion.getQuestionText()).contains("лидерские");
            assertThat(createdQuestion.getScoringRubric()).contains("частоты");
            
            verify(assessmentQuestionRepository).save(any(AssessmentQuestion.class));
        }

        @Test
        @DisplayName("Should create question with complex JSONB answer options")
        void shouldCreateQuestionWithComplexJsonbAnswerOptions() {
            // Given
            AssessmentQuestion complexQuestion = new AssessmentQuestion();
            complexQuestion.setQuestionText("Situational judgment question");
            complexQuestion.setQuestionType(QuestionType.SITUATIONAL_JUDGMENT);
            complexQuestion.setAnswerOptions(createSituationalJudgmentAnswerOptions());
            complexQuestion.setScoringRubric("Complex scoring with weighted effectiveness");
            complexQuestion.setDifficultyLevel(DifficultyLevel.ADVANCED);
            complexQuestion.setActive(true);
            complexQuestion.setOrderIndex(1);

            when(assessmentQuestionRepository.save(any(AssessmentQuestion.class)))
                    .thenReturn(mockAssessmentQuestion);

            // When
            AssessmentQuestion createdQuestion = assessmentQuestionService
                    .createAssesmentQuestion(behavioralIndicatorId, complexQuestion);

            // Then
            assertThat(createdQuestion).isNotNull();
            
            verify(assessmentQuestionRepository).save(argThat(question -> {
                List<Map<String, Object>> options = question.getAnswerOptions();
                return options != null && !options.isEmpty() &&
                       options.get(0).containsKey("action") &&
                       options.get(0).containsKey("effectiveness");
            }));
        }

        @Test
        @DisplayName("Should handle null answer options gracefully")
        void shouldHandleNullAnswerOptionsGracefully() {
            // Given
            AssessmentQuestion questionWithNullOptions = new AssessmentQuestion();
            questionWithNullOptions.setQuestionText("Question without options");
            questionWithNullOptions.setQuestionType(QuestionType.BEHAVIORAL_EXAMPLE);
            questionWithNullOptions.setAnswerOptions(null);
            questionWithNullOptions.setScoringRubric("Manual scoring");
            questionWithNullOptions.setDifficultyLevel(DifficultyLevel.EXPERT);
            questionWithNullOptions.setActive(true);
            questionWithNullOptions.setOrderIndex(1);

            when(assessmentQuestionRepository.save(any(AssessmentQuestion.class)))
                    .thenReturn(mockAssessmentQuestion);

            // When
            AssessmentQuestion createdQuestion = assessmentQuestionService
                    .createAssesmentQuestion(behavioralIndicatorId, questionWithNullOptions);

            // Then
            assertThat(createdQuestion).isNotNull();
            
            verify(assessmentQuestionRepository).save(argThat(question -> 
                question.getAnswerOptions() == null
            ));
        }
    }

    @Nested
    @DisplayName("Find Assessment Question Tests")
    class FindAssessmentQuestionTests {

        @Test
        @DisplayName("Should find assessment question by ID and behavioral indicator ID")
        void shouldFindAssessmentQuestionByIdAndBehavioralIndicatorId() {
            // Given
            when(assessmentQuestionRepository.findById(assessmentQuestionId))
                    .thenReturn(Optional.of(mockAssessmentQuestion));

            // When
            Optional<AssessmentQuestion> foundQuestion = assessmentQuestionService
                    .findAssesmentQuestionById(assessmentQuestionId);

            // Then
            assertThat(foundQuestion).isPresent();
            assertThat(foundQuestion.get()).isEqualTo(mockAssessmentQuestion);
            assertThat(foundQuestion.get().getId()).isEqualTo(assessmentQuestionId);
            assertThat(foundQuestion.get().getBehavioralIndicatorId()).isEqualTo(behavioralIndicatorId);
            
            verify(assessmentQuestionRepository).findById(assessmentQuestionId);
        }

        @Test
        @DisplayName("Should return empty when assessment question not found")
        void shouldReturnEmptyWhenAssessmentQuestionNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(assessmentQuestionRepository.findById(nonExistentId))
                    .thenReturn(Optional.empty());

            // When
            Optional<AssessmentQuestion> foundQuestion = assessmentQuestionService
                    .findAssesmentQuestionById(nonExistentId);

            // Then
            assertThat(foundQuestion).isEmpty();
            
            verify(assessmentQuestionRepository).findById(nonExistentId);
        }

        @Test
        @DisplayName("Should return empty when behavioral indicator ID doesn't match")
        void shouldReturnEmptyWhenBehavioralIndicatorIdDoesNotMatch() {
            // Given
            UUID wrongBehavioralIndicatorId = UUID.randomUUID();
            when(assessmentQuestionRepository.findById(assessmentQuestionId))
                    .thenReturn(Optional.of(mockAssessmentQuestion));

            // When
            Optional<AssessmentQuestion> foundQuestion = assessmentQuestionService
                    .findAssesmentQuestionById(assessmentQuestionId);

            // Then
            assertThat(foundQuestion).isEmpty();
            
            verify(assessmentQuestionRepository).findById(assessmentQuestionId);
        }

        @Test
        @DisplayName("Should handle question with complex JSONB answer options")
        void shouldHandleQuestionWithComplexJsonbAnswerOptions() {
            // Given
            AssessmentQuestion complexQuestion = new AssessmentQuestion();
            complexQuestion.setId(assessmentQuestionId);
            complexQuestion.setBehavioralIndicator(mockBehavioralIndicator);
            complexQuestion.setAnswerOptions(createSituationalJudgmentAnswerOptions());

            when(assessmentQuestionRepository.findById(assessmentQuestionId))
                    .thenReturn(Optional.of(complexQuestion));

            // When
            Optional<AssessmentQuestion> foundQuestion = assessmentQuestionService
                    .findAssesmentQuestionById(assessmentQuestionId);

            // Then
            assertThat(foundQuestion).isPresent();
            assertThat(foundQuestion.get().getAnswerOptions()).isNotNull();
            assertThat(foundQuestion.get().getAnswerOptions()).hasSize(4);
            assertThat(foundQuestion.get().getAnswerOptions().get(0)).containsKey("effectiveness");
        }
    }

    @Nested
    @DisplayName("Update Assessment Question Tests")
    class UpdateAssessmentQuestionTests {

        @Test
        @DisplayName("Should update assessment question successfully")
        void shouldUpdateAssessmentQuestionSuccessfully() {
            // Given
            AssessmentQuestion updateDetails = new AssessmentQuestion();
            updateDetails.setQuestionText("Updated question text");
            updateDetails.setQuestionType(QuestionType.SELF_REFLECTION);
            updateDetails.setAnswerOptions(createSelfReflectionAnswerOptions());
            updateDetails.setScoringRubric("Updated scoring rubric");
            updateDetails.setTimeLimit(600);
            updateDetails.setDifficultyLevel(DifficultyLevel.SPECIALIZED);
            updateDetails.setActive(false);
            updateDetails.setOrderIndex(5);

            when(assessmentQuestionRepository.findById(assessmentQuestionId))
                    .thenReturn(Optional.of(mockAssessmentQuestion));
            when(assessmentQuestionRepository.save(any(AssessmentQuestion.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            AssessmentQuestion updatedQuestion = assessmentQuestionService
                    .updateAssesmentQuestion(assessmentQuestionId, updateDetails);

            // Then
            assertThat(updatedQuestion).isNotNull();
            assertThat(updatedQuestion.getQuestionText()).isEqualTo("Updated question text");
            assertThat(updatedQuestion.getQuestionType()).isEqualTo(QuestionType.SELF_REFLECTION);
            assertThat(updatedQuestion.getScoringRubric()).isEqualTo("Updated scoring rubric");
            assertThat(updatedQuestion.getTimeLimit()).isEqualTo(600);
            assertThat(updatedQuestion.getDifficultyLevel()).isEqualTo(DifficultyLevel.SPECIALIZED);
            assertThat(updatedQuestion.isActive()).isFalse();
            assertThat(updatedQuestion.getOrderIndex()).isEqualTo(5);
            
            verify(assessmentQuestionRepository).findById(assessmentQuestionId);
            verify(assessmentQuestionRepository).save(mockAssessmentQuestion);
        }

        @Test
        @DisplayName("Should update question with Russian text successfully")
        void shouldUpdateQuestionWithRussianTextSuccessfully() {
            // Given
            AssessmentQuestion russianUpdate = new AssessmentQuestion();
            russianUpdate.setQuestionText("Обновленный вопрос о лидерских навыках");
            russianUpdate.setQuestionType(QuestionType.PEER_FEEDBACK);
            russianUpdate.setAnswerOptions(createRussianPeerFeedbackOptions());
            russianUpdate.setScoringRubric("Система оценки от коллег на основе наблюдений");
            russianUpdate.setTimeLimit(450);
            russianUpdate.setDifficultyLevel(DifficultyLevel.ADVANCED);
            russianUpdate.setActive(true);
            russianUpdate.setOrderIndex(3);

            when(assessmentQuestionRepository.findById(assessmentQuestionId))
                    .thenReturn(Optional.of(mockAssessmentQuestion));
            when(assessmentQuestionRepository.save(any(AssessmentQuestion.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            AssessmentQuestion updatedQuestion = assessmentQuestionService
                    .updateAssesmentQuestion(assessmentQuestionId, russianUpdate);

            // Then
            assertThat(updatedQuestion).isNotNull();
            assertThat(updatedQuestion.getQuestionText()).contains("лидерских");
            assertThat(updatedQuestion.getQuestionText()).contains("навыках");
            assertThat(updatedQuestion.getScoringRubric()).contains("коллег");
            assertThat(updatedQuestion.getScoringRubric()).contains("наблюдений");
            
            verify(assessmentQuestionRepository).save(mockAssessmentQuestion);
        }

        @Test
        @DisplayName("Should return null when assessment question not found")
        void shouldReturnNullWhenAssessmentQuestionNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            AssessmentQuestion updateDetails = new AssessmentQuestion();
            updateDetails.setQuestionText("Updated text");

            when(assessmentQuestionRepository.findById(nonExistentId))
                    .thenReturn(Optional.empty());

            // When
            AssessmentQuestion updatedQuestion = assessmentQuestionService
                    .updateAssesmentQuestion(nonExistentId, updateDetails);

            // Then
            assertThat(updatedQuestion).isNull();
            
            verify(assessmentQuestionRepository).findById(nonExistentId);
            verify(assessmentQuestionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should return null when behavioral indicator ID doesn't match")
        void shouldReturnNullWhenBehavioralIndicatorIdDoesNotMatch() {
            // Given
            UUID wrongBehavioralIndicatorId = UUID.randomUUID();
            AssessmentQuestion updateDetails = new AssessmentQuestion();
            updateDetails.setQuestionText("Updated text");

            when(assessmentQuestionRepository.findById(assessmentQuestionId))
                    .thenReturn(Optional.of(mockAssessmentQuestion));

            // When
            AssessmentQuestion updatedQuestion = assessmentQuestionService
                    .updateAssesmentQuestion(assessmentQuestionId, updateDetails);

            // Then
            assertThat(updatedQuestion).isNull();
            
            verify(assessmentQuestionRepository).findById(assessmentQuestionId);
            verify(assessmentQuestionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should handle updating complex JSONB answer options")
        void shouldHandleUpdatingComplexJsonbAnswerOptions() {
            // Given
            AssessmentQuestion complexUpdate = new AssessmentQuestion();
            complexUpdate.setQuestionText("Updated situational question");
            complexUpdate.setQuestionType(QuestionType.SITUATIONAL_JUDGMENT);
            complexUpdate.setAnswerOptions(createComplexSituationalAnswerOptions());
            complexUpdate.setScoringRubric("Updated complex scoring");
            complexUpdate.setDifficultyLevel(DifficultyLevel.EXPERT);
            complexUpdate.setActive(true);
            complexUpdate.setOrderIndex(1);

            when(assessmentQuestionRepository.findById(assessmentQuestionId))
                    .thenReturn(Optional.of(mockAssessmentQuestion));
            when(assessmentQuestionRepository.save(any(AssessmentQuestion.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            AssessmentQuestion updatedQuestion = assessmentQuestionService
                    .updateAssesmentQuestion(assessmentQuestionId, complexUpdate);

            // Then
            assertThat(updatedQuestion).isNotNull();
            assertThat(updatedQuestion.getAnswerOptions()).isNotNull();
            assertThat(updatedQuestion.getAnswerOptions()).hasSize(3);
            assertThat(updatedQuestion.getAnswerOptions().get(0)).containsKey("сценарий");
            assertThat(updatedQuestion.getAnswerOptions().get(0)).containsKey("результат");
            
            verify(assessmentQuestionRepository).save(mockAssessmentQuestion);
        }
    }

    @Nested
    @DisplayName("Delete Assessment Question Tests")
    class DeleteAssessmentQuestionTests {

        @Test
        @DisplayName("Should delete assessment question successfully")
        void shouldDeleteAssessmentQuestionSuccessfully() {
            // Given
            when(assessmentQuestionRepository.findById(assessmentQuestionId))
                    .thenReturn(Optional.of(mockAssessmentQuestion));
            doNothing().when(assessmentQuestionRepository).delete(mockAssessmentQuestion);

            // When
            assertThatCode(() -> assessmentQuestionService
                    .deleteAssesmentQuestion(assessmentQuestionId))
                    .doesNotThrowAnyException();

            // Then
            verify(assessmentQuestionRepository).findById(assessmentQuestionId);
            verify(assessmentQuestionRepository).delete(mockAssessmentQuestion);
        }

        @Test
        @DisplayName("Should not delete when assessment question not found")
        void shouldNotDeleteWhenAssessmentQuestionNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(assessmentQuestionRepository.findById(nonExistentId))
                    .thenReturn(Optional.empty());

            // When
            assertThatCode(() -> assessmentQuestionService
                    .deleteAssesmentQuestion(nonExistentId))
                    .doesNotThrowAnyException();

            // Then
            verify(assessmentQuestionRepository).findById(nonExistentId);
            verify(assessmentQuestionRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should not delete when behavioral indicator ID doesn't match")
        void shouldNotDeleteWhenBehavioralIndicatorIdDoesNotMatch() {
            // Given
            UUID wrongBehavioralIndicatorId = UUID.randomUUID();
            when(assessmentQuestionRepository.findById(assessmentQuestionId))
                    .thenReturn(Optional.of(mockAssessmentQuestion));

            // When
            assertThatCode(() -> assessmentQuestionService
                    .deleteAssesmentQuestion(assessmentQuestionId))
                    .doesNotThrowAnyException();

            // Then
            verify(assessmentQuestionRepository).findById(assessmentQuestionId);
            verify(assessmentQuestionRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("Business Logic and Edge Cases Tests")
    class BusinessLogicTests {

        @Test
        @DisplayName("Should handle all question types correctly")
        void shouldHandleAllQuestionTypesCorrectly() {
            // Given & When & Then - Test all question types
            for (QuestionType questionType : QuestionType.values()) {
                AssessmentQuestion question = new AssessmentQuestion();
                question.setQuestionText("Test question for " + questionType.name());
                question.setQuestionType(questionType);
                question.setAnswerOptions(getAnswerOptionsForQuestionType(questionType));
                question.setScoringRubric("Scoring for " + questionType.name());
                question.setDifficultyLevel(DifficultyLevel.INTERMEDIATE);
                question.setActive(true);
                question.setOrderIndex(1);

                when(assessmentQuestionRepository.save(any(AssessmentQuestion.class)))
                        .thenReturn(question);

                AssessmentQuestion created = assessmentQuestionService
                        .createAssesmentQuestion(behavioralIndicatorId, question);

                assertThat(created.getQuestionType()).isEqualTo(questionType);
            }
        }

        @Test
        @DisplayName("Should handle all difficulty levels correctly")
        void shouldHandleAllDifficultyLevelsCorrectly() {
            // Given & When & Then - Test all difficulty levels
            for (DifficultyLevel difficultyLevel : DifficultyLevel.values()) {
                AssessmentQuestion question = new AssessmentQuestion();
                question.setQuestionText("Test question for " + difficultyLevel.name());
                question.setQuestionType(QuestionType.LIKERT_SCALE);
                question.setAnswerOptions(createLikertScaleAnswerOptions());
                question.setScoringRubric("Scoring for " + difficultyLevel.name());
                question.setDifficultyLevel(difficultyLevel);
                question.setActive(true);
                question.setOrderIndex(1);

                when(assessmentQuestionRepository.save(any(AssessmentQuestion.class)))
                        .thenReturn(question);

                AssessmentQuestion created = assessmentQuestionService
                        .createAssesmentQuestion(behavioralIndicatorId, question);

                assertThat(created.getDifficultyLevel()).isEqualTo(difficultyLevel);
            }
        }

        @Test
        @DisplayName("Should handle extreme time limits")
        void shouldHandleExtremeTimeLimits() {
            // Given
            AssessmentQuestion shortTimeQuestion = new AssessmentQuestion();
            shortTimeQuestion.setQuestionText("Quick question");
            shortTimeQuestion.setQuestionType(QuestionType.MULTIPLE_CHOICE);
            shortTimeQuestion.setTimeLimit(30); // 30 seconds
            shortTimeQuestion.setDifficultyLevel(DifficultyLevel.FOUNDATIONAL);
            shortTimeQuestion.setActive(true);

            AssessmentQuestion longTimeQuestion = new AssessmentQuestion();
            longTimeQuestion.setQuestionText("Complex question");
            longTimeQuestion.setQuestionType(QuestionType.BEHAVIORAL_EXAMPLE);
            longTimeQuestion.setTimeLimit(3600); // 1 hour
            longTimeQuestion.setDifficultyLevel(DifficultyLevel.EXPERT);
            longTimeQuestion.setActive(true);

            when(assessmentQuestionRepository.save(any(AssessmentQuestion.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When & Then
            AssessmentQuestion shortCreated = assessmentQuestionService
                    .createAssesmentQuestion(behavioralIndicatorId, shortTimeQuestion);
            AssessmentQuestion longCreated = assessmentQuestionService
                    .createAssesmentQuestion(behavioralIndicatorId, longTimeQuestion);

            assertThat(shortCreated.getTimeLimit()).isEqualTo(30);
            assertThat(longCreated.getTimeLimit()).isEqualTo(3600);
        }
    }

    // Helper methods for creating test data
    private AssessmentQuestion createTestQuestion(String questionText, QuestionType questionType, int orderIndex) {
        AssessmentQuestion question = new AssessmentQuestion();
        question.setId(UUID.randomUUID());
        question.setBehavioralIndicator(mockBehavioralIndicator);
        question.setQuestionText(questionText);
        question.setQuestionType(questionType);
        question.setAnswerOptions(getAnswerOptionsForQuestionType(questionType));
        question.setScoringRubric("Scoring for " + questionText);
        question.setTimeLimit(300);
        question.setDifficultyLevel(DifficultyLevel.INTERMEDIATE);
        question.setActive(true);
        question.setOrderIndex(orderIndex);
        return question;
    }

    private List<Map<String, Object>> getAnswerOptionsForQuestionType(QuestionType questionType) {
        return switch (questionType) {
            case LIKERT_SCALE, FREQUENCY_SCALE -> createLikertScaleAnswerOptions();
            case MULTIPLE_CHOICE -> createMultipleChoiceAnswerOptions();
            case SITUATIONAL_JUDGMENT -> createSituationalJudgmentAnswerOptions();
            case SELF_REFLECTION -> createSelfReflectionAnswerOptions();
            case PEER_FEEDBACK -> createRussianPeerFeedbackOptions();
            default -> createLikertScaleAnswerOptions();
        };
    }

    private List<Map<String, Object>> createLikertScaleAnswerOptions() {
        List<Map<String, Object>> options = new ArrayList<>();
        
        String[] labels = {"Полностью не согласен", "Не согласен", "Нейтрально", "Согласен", "Полностью согласен"};
        for (int i = 0; i < 5; i++) {
            Map<String, Object> option = new HashMap<>();
            option.put("value", i + 1);
            option.put("label", labels[i]);
            option.put("score", i + 1);
            options.add(option);
        }
        return options;
    }

    private List<Map<String, Object>> createMultipleChoiceAnswerOptions() {
        List<Map<String, Object>> options = new ArrayList<>();

        Map<String, Object> option1 = new HashMap<>();
        option1.put("id", "A");
        option1.put("text", "Обратиться к руководителю");
        option1.put("isCorrect", true);
        options.add(option1);

        Map<String, Object> option2 = new HashMap<>();
        option2.put("id", "B");
        option2.put("text", "Игнорировать проблему");
        option2.put("isCorrect", false);
        options.add(option2);

        return options;
    }

    private List<Map<String, Object>> createSituationalJudgmentAnswerOptions() {
        List<Map<String, Object>> options = new ArrayList<>();

        Map<String, Object> option1 = new HashMap<>();
        option1.put("action", "Немедленно вмешаться");
        option1.put("effectiveness", 3);
        option1.put("explanation", "Прямое действие");
        options.add(option1);

        Map<String, Object> option2 = new HashMap<>();
        option2.put("action", "Выслушать стороны");
        option2.put("effectiveness", 5);
        option2.put("explanation", "Лучший подход");
        options.add(option2);

        Map<String, Object> option3 = new HashMap<>();
        option3.put("action", "Переложить ответственность");
        option3.put("effectiveness", 2);
        option3.put("explanation", "Избегание решения");
        options.add(option3);

        Map<String, Object> option4 = new HashMap<>();
        option4.put("action", "Проигнорировать");
        option4.put("effectiveness", 1);
        option4.put("explanation", "Наихудший вариант");
        options.add(option4);

        return options;
    }

    private List<Map<String, Object>> createSelfReflectionAnswerOptions() {
        List<Map<String, Object>> options = new ArrayList<>();

        Map<String, Object> option1 = new HashMap<>();
        option1.put("reflection_type", "strengths");
        option1.put("prompt", "Опишите ваши сильные стороны");
        option1.put("max_length", 500);
        options.add(option1);

        Map<String, Object> option2 = new HashMap<>();
        option2.put("reflection_type", "areas_for_improvement");
        option2.put("prompt", "Области для развития");
        option2.put("max_length", 500);
        options.add(option2);

        return options;
    }

    private List<Map<String, Object>> createRussianFrequencyScaleOptions() {
        List<Map<String, Object>> options = new ArrayList<>();
        
        String[] frequencies = {"Никогда", "Редко", "Иногда", "Часто", "Всегда"};
        for (int i = 0; i < 5; i++) {
            Map<String, Object> option = new HashMap<>();
            option.put("frequency", i + 1);
            option.put("label", frequencies[i]);
            option.put("score", i + 1);
            options.add(option);
        }
        return options;
    }

    private List<Map<String, Object>> createRussianPeerFeedbackOptions() {
        List<Map<String, Object>> options = new ArrayList<>();

        Map<String, Object> option1 = new HashMap<>();
        option1.put("feedback_type", "observed_behavior");
        option1.put("prompt", "Опишите наблюдаемое поведение коллеги");
        option1.put("rating_scale", 5);
        options.add(option1);

        Map<String, Object> option2 = new HashMap<>();
        option2.put("feedback_type", "improvement_suggestions");
        option2.put("prompt", "Предложения по улучшению");
        option2.put("rating_scale", 5);
        options.add(option2);

        return options;
    }

    private List<Map<String, Object>> createComplexSituationalAnswerOptions() {
        List<Map<String, Object>> options = new ArrayList<>();

        Map<String, Object> option1 = new HashMap<>();
        option1.put("сценарий", "Конфликт в команде");
        Map<String, Object> result1 = new HashMap<>();
        result1.put("эффективность", 4);
        result1.put("влияние_на_команду", "положительное");
        result1.put("долгосрочные_последствия", "улучшение сотрудничества");
        option1.put("результат", result1);
        options.add(option1);

        Map<String, Object> option2 = new HashMap<>();
        option2.put("сценарий", "Срочный проект");
        Map<String, Object> result2 = new HashMap<>();
        result2.put("эффективность", 5);
        result2.put("влияние_на_команду", "мотивирующее");
        result2.put("долгосрочные_последствия", "повышение доверия");
        option2.put("результат", result2);
        options.add(option2);

        Map<String, Object> option3 = new HashMap<>();
        option3.put("сценарий", "Неопределенная ситуация");
        Map<String, Object> result3 = new HashMap<>();
        result3.put("эффективность", 3);
        result3.put("влияние_на_команду", "нейтральное");
        result3.put("долгосрочные_последствия", "стабильность");
        option3.put("результат", result3);
        options.add(option3);

        return options;
    }
}
