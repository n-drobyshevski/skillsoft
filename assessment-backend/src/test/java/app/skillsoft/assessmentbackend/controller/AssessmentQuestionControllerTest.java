package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.dto.AssessmentQuestionDto;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.domain.mapper.AssessmentQuestionMapper;
import app.skillsoft.assessmentbackend.services.AssessmentQuestionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive controller tests for AssessmentQuestionController
 * 
 * Tests cover:
 * - REST endpoint functionality (@WebMvcTest)
 * - HTTP request/response validation with complex path variables
 * - JSONB answer options serialization/deserialization
 * - Russian content handling in HTTP context
 * - Error handling and exception scenarios
 * - Service layer integration through mocks
 * - Validation of all CRUD operations
 */
@WebMvcTest(AssessmentQuestionController.class)
@DisplayName("AssessmentQuestion Controller Tests")
class AssessmentQuestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AssessmentQuestionService assessmentQuestionService;

    @MockitoBean
    private AssessmentQuestionMapper assessmentQuestionMapper;

    private UUID competencyId;
    private UUID behavioralIndicatorId;
    private UUID assessmentQuestionId;
    private AssessmentQuestion mockEntity;
    private AssessmentQuestionDto mockDto;

    @BeforeEach
    void setUp() {
        competencyId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        behavioralIndicatorId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        assessmentQuestionId = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");

        // Create mock entity
        mockEntity = new AssessmentQuestion();
        mockEntity.setId(assessmentQuestionId);
        mockEntity.setBehavioralIndicatorId(behavioralIndicatorId);
        mockEntity.setQuestionText("Test assessment question");
        mockEntity.setQuestionType(QuestionType.LIKERT_SCALE);
        mockEntity.setAnswerOptions(createLikertScaleAnswerOptions());
        mockEntity.setScoringRubric("Test scoring rubric");
        mockEntity.setTimeLimit(300);
        mockEntity.setDifficultyLevel(DifficultyLevel.INTERMEDIATE);
        mockEntity.setActive(true);
        mockEntity.setOrderIndex(1);

        // Create mock DTO
        mockDto = new AssessmentQuestionDto(
                assessmentQuestionId,
                behavioralIndicatorId,
                "Test assessment question",
                QuestionType.LIKERT_SCALE,
                createLikertScaleAnswerOptions(),
                "Test scoring rubric",
                300,
                DifficultyLevel.INTERMEDIATE,
                true,
                1
        );
    }

    @Nested
    @DisplayName("GET /api/competencies/{competencyId}/bi/{behavioralIndicatorId}/questions - List Assessment Questions")
    class ListAssessmentQuestionsTests {

        @Test
        @DisplayName("Should return list of assessment questions")
        void shouldReturnListOfAssessmentQuestions() throws Exception {
            // Given
            List<AssessmentQuestion> entities = Arrays.asList(
                    mockEntity,
                    createTestEntity("Second Question", QuestionType.MULTIPLE_CHOICE, 2),
                    createTestEntity("Third Question", QuestionType.SITUATIONAL_JUDGMENT, 3)
            );
            List<AssessmentQuestionDto> dtos = Arrays.asList(
                    mockDto,
                    createTestDto("Second Question", QuestionType.MULTIPLE_CHOICE, 2),
                    createTestDto("Third Question", QuestionType.SITUATIONAL_JUDGMENT, 3)
            );

            when(assessmentQuestionService.listAssessmentQuestions(behavioralIndicatorId)).thenReturn(entities);
            when(assessmentQuestionMapper.toDto(any(AssessmentQuestion.class)))
                    .thenReturn(dtos.get(0), dtos.get(1), dtos.get(2));

            // When & Then
            mockMvc.perform(get("/api/competencies/{competencyId}/bi/{behavioralIndicatorId}/questions", 
                            competencyId, behavioralIndicatorId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[0].id", is(assessmentQuestionId.toString())))
                    .andExpect(jsonPath("$[0].questionText", is("Test assessment question")))
                    .andExpect(jsonPath("$[0].behavioralIndicatorId", is(behavioralIndicatorId.toString())))
                    .andExpect(jsonPath("$[1].questionText", is("Second Question")))
                    .andExpect(jsonPath("$[2].questionText", is("Third Question")));

            verify(assessmentQuestionService).listAssessmentQuestions(behavioralIndicatorId);
            verify(assessmentQuestionMapper, times(3)).toDto(any(AssessmentQuestion.class));
        }

        @Test
        @DisplayName("Should return empty list when no assessment questions exist")
        void shouldReturnEmptyListWhenNoAssessmentQuestionsExist() throws Exception {
            // Given
            when(assessmentQuestionService.listAssessmentQuestions(behavioralIndicatorId))
                    .thenReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(get("/api/competencies/{competencyId}/bi/{behavioralIndicatorId}/questions", 
                            competencyId, behavioralIndicatorId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(assessmentQuestionService).listAssessmentQuestions(behavioralIndicatorId);
            verifyNoInteractions(assessmentQuestionMapper);
        }

        @Test
        @DisplayName("Should handle invalid UUID format in path variables")
        void shouldHandleInvalidUuidFormatInPathVariables() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/competencies/{competencyId}/bi/{behavioralIndicatorId}/questions", 
                            "invalid-uuid", "invalid-uuid")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(assessmentQuestionService);
        }
    }

    @Nested
    @DisplayName("GET /api/competencies/{competencyId}/bi/{behavioralIndicatorId}/questions/{questionId} - Get Question by ID")
    class GetQuestionByIdTests {

        @Test
        @DisplayName("Should return assessment question when found")
        void shouldReturnAssessmentQuestionWhenFound() throws Exception {
            // Given
            when(assessmentQuestionService.findAssesmentQuestionById(behavioralIndicatorId, assessmentQuestionId))
                    .thenReturn(Optional.of(mockEntity));
            when(assessmentQuestionMapper.toDto(mockEntity)).thenReturn(mockDto);

            // When & Then
            mockMvc.perform(get("/api/competencies/{competencyId}/bi/{behavioralIndicatorId}/questions/{questionId}", 
                            competencyId, behavioralIndicatorId, assessmentQuestionId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(assessmentQuestionId.toString())))
                    .andExpect(jsonPath("$.behavioralIndicatorId", is(behavioralIndicatorId.toString())))
                    .andExpect(jsonPath("$.questionText", is("Test assessment question")))
                    .andExpect(jsonPath("$.questionType", is("LIKERT_SCALE")))
                    .andExpect(jsonPath("$.answerOptions", hasSize(5)))
                    .andExpect(jsonPath("$.answerOptions[0].label", is("Полностью не согласен")))
                    .andExpect(jsonPath("$.answerOptions[4].label", is("Полностью согласен")))
                    .andExpect(jsonPath("$.scoringRubric", is("Test scoring rubric")))
                    .andExpect(jsonPath("$.timeLimit", is(300)))
                    .andExpect(jsonPath("$.difficultyLevel", is("INTERMEDIATE")))
                    .andExpect(jsonPath("$.isActive", is(true)))
                    .andExpect(jsonPath("$.orderIndex", is(1)));

            verify(assessmentQuestionService).findAssesmentQuestionById(behavioralIndicatorId, assessmentQuestionId);
            verify(assessmentQuestionMapper).toDto(mockEntity);
        }

        @Test
        @DisplayName("Should return 404 when assessment question not found")
        void shouldReturn404WhenAssessmentQuestionNotFound() throws Exception {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(assessmentQuestionService.findAssesmentQuestionById(behavioralIndicatorId, nonExistentId))
                    .thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(get("/api/competencies/{competencyId}/bi/{behavioralIndicatorId}/questions/{questionId}", 
                            competencyId, behavioralIndicatorId, nonExistentId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(assessmentQuestionService).findAssesmentQuestionById(behavioralIndicatorId, nonExistentId);
            verifyNoInteractions(assessmentQuestionMapper);
        }

        @Test
        @DisplayName("Should handle Russian text in question response")
        void shouldHandleRussianTextInQuestionResponse() throws Exception {
            // Given
            AssessmentQuestion russianEntity = new AssessmentQuestion();
            russianEntity.setId(assessmentQuestionId);
            russianEntity.setQuestionText("Как часто вы демонстрируете лидерские качества?");
            russianEntity.setScoringRubric("Оценка от 1 до 5 баллов");
            russianEntity.setAnswerOptions(createRussianFrequencyScaleOptions());

            AssessmentQuestionDto russianDto = new AssessmentQuestionDto(
                    assessmentQuestionId, behavioralIndicatorId,
                    "Как часто вы демонстрируете лидерские качества?",
                    QuestionType.FREQUENCY_SCALE,
                    createRussianFrequencyScaleOptions(),
                    "Оценка от 1 до 5 баллов",
                    null, DifficultyLevel.INTERMEDIATE, true, 1
            );

            when(assessmentQuestionService.findAssesmentQuestionById(behavioralIndicatorId, assessmentQuestionId))
                    .thenReturn(Optional.of(russianEntity));
            when(assessmentQuestionMapper.toDto(russianEntity)).thenReturn(russianDto);

            // When & Then
            mockMvc.perform(get("/api/competencies/{competencyId}/bi/{behavioralIndicatorId}/questions/{questionId}", 
                            competencyId, behavioralIndicatorId, assessmentQuestionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding("UTF-8"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(content().encoding("UTF-8"))
                    .andExpect(jsonPath("$.questionText", is("Как часто вы демонстрируете лидерские качества?")))
                    .andExpect(jsonPath("$.scoringRubric", is("Оценка от 1 до 5 баллов")))
                    .andExpect(jsonPath("$.answerOptions[0].label", is("Никогда")))
                    .andExpect(jsonPath("$.answerOptions[4].label", is("Всегда")));
        }
    }

    @Nested
    @DisplayName("POST /api/competencies/{competencyId}/bi/{behavioralIndicatorId}/questions - Create Question")
    class CreateQuestionTests {

        @Test
        @DisplayName("Should create assessment question successfully")
        void shouldCreateAssessmentQuestionSuccessfully() throws Exception {
            // Given
            AssessmentQuestion requestEntity = new AssessmentQuestion();
            requestEntity.setQuestionText("New assessment question");
            requestEntity.setQuestionType(QuestionType.MULTIPLE_CHOICE);
            requestEntity.setAnswerOptions(createMultipleChoiceAnswerOptions());
            requestEntity.setScoringRubric("New scoring rubric");
            requestEntity.setTimeLimit(180);
            requestEntity.setDifficultyLevel(DifficultyLevel.FOUNDATIONAL);
            requestEntity.setActive(true);
            requestEntity.setOrderIndex(2);

            AssessmentQuestion createdEntity = new AssessmentQuestion();
            createdEntity.setId(UUID.randomUUID());
            createdEntity.setQuestionText("New assessment question");

            AssessmentQuestionDto responseDto = new AssessmentQuestionDto(
                    createdEntity.getId(), behavioralIndicatorId,
                    "New assessment question", QuestionType.MULTIPLE_CHOICE,
                    createMultipleChoiceAnswerOptions(), "New scoring rubric",
                    180, DifficultyLevel.FOUNDATIONAL, true, 2
            );

            when(assessmentQuestionService.createAssesmentQuestion(eq(behavioralIndicatorId), any(AssessmentQuestion.class)))
                    .thenReturn(createdEntity);
            when(assessmentQuestionMapper.toDto(createdEntity)).thenReturn(responseDto);

            // When & Then
            mockMvc.perform(post("/api/competencies/{competencyId}/bi/{behavioralIndicatorId}/questions", 
                            competencyId, behavioralIndicatorId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestEntity)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(createdEntity.getId().toString())))
                    .andExpect(jsonPath("$.questionText", is("New assessment question")))
                    .andExpect(jsonPath("$.behavioralIndicatorId", is(behavioralIndicatorId.toString())))
                    .andExpect(jsonPath("$.questionType", is("MULTIPLE_CHOICE")))
                    .andExpect(jsonPath("$.difficultyLevel", is("FOUNDATIONAL")))
                    .andExpect(jsonPath("$.timeLimit", is(180)));

            verify(assessmentQuestionService).createAssesmentQuestion(eq(behavioralIndicatorId), any(AssessmentQuestion.class));
            verify(assessmentQuestionMapper).toDto(createdEntity);
        }

        @Test
        @DisplayName("Should create question with Russian text successfully")
        void shouldCreateQuestionWithRussianTextSuccessfully() throws Exception {
            // Given
            AssessmentQuestion russianRequest = new AssessmentQuestion();
            russianRequest.setQuestionText("Оцените свои лидерские способности");
            russianRequest.setQuestionType(QuestionType.SELF_REFLECTION);
            russianRequest.setAnswerOptions(createSelfReflectionAnswerOptions());
            russianRequest.setScoringRubric("Качественная оценка ответа от 1 до 5 баллов");
            russianRequest.setTimeLimit(600);
            russianRequest.setDifficultyLevel(DifficultyLevel.ADVANCED);
            russianRequest.setActive(true);
            russianRequest.setOrderIndex(1);

            AssessmentQuestion createdRussianEntity = new AssessmentQuestion();
            createdRussianEntity.setId(UUID.randomUUID());
            createdRussianEntity.setQuestionText("Оцените свои лидерские способности");

            AssessmentQuestionDto russianResponseDto = new AssessmentQuestionDto(
                    createdRussianEntity.getId(), behavioralIndicatorId,
                    "Оцените свои лидерские способности", QuestionType.SELF_REFLECTION,
                    createSelfReflectionAnswerOptions(), "Качественная оценка ответа от 1 до 5 баллов",
                    600, DifficultyLevel.ADVANCED, true, 1
            );

            when(assessmentQuestionService.createAssesmentQuestion(eq(behavioralIndicatorId), any(AssessmentQuestion.class)))
                    .thenReturn(createdRussianEntity);
            when(assessmentQuestionMapper.toDto(createdRussianEntity)).thenReturn(russianResponseDto);

            // When & Then
            mockMvc.perform(post("/api/competencies/{competencyId}/bi/{behavioralIndicatorId}/questions", 
                            competencyId, behavioralIndicatorId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding("UTF-8")
                            .content(objectMapper.writeValueAsString(russianRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.questionText", is("Оцените свои лидерские способности")))
                    .andExpect(jsonPath("$.scoringRubric", is("Качественная оценка ответа от 1 до 5 баллов")))
                    .andExpect(jsonPath("$.questionType", is("SELF_REFLECTION")));

            verify(assessmentQuestionService).createAssesmentQuestion(eq(behavioralIndicatorId), any(AssessmentQuestion.class));
        }

        @Test
        @DisplayName("Should create question with complex JSONB answer options")
        void shouldCreateQuestionWithComplexJsonbAnswerOptions() throws Exception {
            // Given
            AssessmentQuestion complexRequest = new AssessmentQuestion();
            complexRequest.setQuestionText("Situational judgment scenario");
            complexRequest.setQuestionType(QuestionType.SITUATIONAL_JUDGMENT);
            complexRequest.setAnswerOptions(createSituationalJudgmentAnswerOptions());
            complexRequest.setScoringRubric("Complex effectiveness-based scoring");
            complexRequest.setDifficultyLevel(DifficultyLevel.EXPERT);

            AssessmentQuestion createdComplexEntity = new AssessmentQuestion();
            createdComplexEntity.setId(UUID.randomUUID());
            createdComplexEntity.setAnswerOptions(createSituationalJudgmentAnswerOptions());

            AssessmentQuestionDto complexResponseDto = new AssessmentQuestionDto(
                    createdComplexEntity.getId(), behavioralIndicatorId,
                    "Situational judgment scenario", QuestionType.SITUATIONAL_JUDGMENT,
                    createSituationalJudgmentAnswerOptions(), "Complex effectiveness-based scoring",
                    null, DifficultyLevel.EXPERT, true, 1
            );

            when(assessmentQuestionService.createAssesmentQuestion(eq(behavioralIndicatorId), any(AssessmentQuestion.class)))
                    .thenReturn(createdComplexEntity);
            when(assessmentQuestionMapper.toDto(createdComplexEntity)).thenReturn(complexResponseDto);

            // When & Then
            mockMvc.perform(post("/api/competencies/{competencyId}/bi/{behavioralIndicatorId}/questions", 
                            competencyId, behavioralIndicatorId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(complexRequest)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.answerOptions", hasSize(4)))
                    .andExpect(jsonPath("$.answerOptions[0].action", is("Немедленно вмешаться")))
                    .andExpect(jsonPath("$.answerOptions[0].effectiveness", is(3)))
                    .andExpect(jsonPath("$.answerOptions[1].effectiveness", is(5)));
        }

        @Test
        @DisplayName("Should handle malformed JSON")
        void shouldHandleMalformedJson() throws Exception {
            // Given
            String malformedJson = "{\"questionText\": \"Test\", \"invalid\": }";

            // When & Then
            mockMvc.perform(post("/api/competencies/{competencyId}/bi/{behavioralIndicatorId}/questions", 
                            competencyId, behavioralIndicatorId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(malformedJson))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(assessmentQuestionService);
        }
    }

    @Nested
    @DisplayName("PUT /api/competencies/{competencyId}/bi/{behavioralIndicatorId}/questions/{questionId} - Update Question")
    class UpdateQuestionTests {

        @Test
        @DisplayName("Should update assessment question successfully")
        void shouldUpdateAssessmentQuestionSuccessfully() throws Exception {
            // Given
            AssessmentQuestion updateRequest = new AssessmentQuestion();
            updateRequest.setQuestionText("Updated assessment question");
            updateRequest.setQuestionType(QuestionType.PEER_FEEDBACK);
            updateRequest.setAnswerOptions(createPeerFeedbackAnswerOptions());
            updateRequest.setScoringRubric("Updated scoring rubric");
            updateRequest.setTimeLimit(450);
            updateRequest.setDifficultyLevel(DifficultyLevel.SPECIALIZED);
            updateRequest.setActive(false);
            updateRequest.setOrderIndex(3);

            AssessmentQuestion updatedEntity = new AssessmentQuestion();
            updatedEntity.setId(assessmentQuestionId);
            updatedEntity.setQuestionText("Updated assessment question");

            AssessmentQuestionDto updateResponseDto = new AssessmentQuestionDto(
                    assessmentQuestionId, behavioralIndicatorId,
                    "Updated assessment question", QuestionType.PEER_FEEDBACK,
                    createPeerFeedbackAnswerOptions(), "Updated scoring rubric",
                    450, DifficultyLevel.SPECIALIZED, false, 3
            );

            when(assessmentQuestionService.updateAssesmentQuestion(behavioralIndicatorId, assessmentQuestionId, updateRequest))
                    .thenReturn(updatedEntity);
            when(assessmentQuestionMapper.toDto(updatedEntity)).thenReturn(updateResponseDto);

            // When & Then
            mockMvc.perform(put("/api/competencies/{competencyId}/bi/{behavioralIndicatorId}/questions/{questionId}", 
                            competencyId, behavioralIndicatorId, assessmentQuestionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(assessmentQuestionId.toString())))
                    .andExpect(jsonPath("$.questionText", is("Updated assessment question")))
                    .andExpect(jsonPath("$.questionType", is("PEER_FEEDBACK")))
                    .andExpect(jsonPath("$.difficultyLevel", is("SPECIALIZED")))
                    .andExpect(jsonPath("$.timeLimit", is(450)))
                    .andExpect(jsonPath("$.isActive", is(false)))
                    .andExpect(jsonPath("$.orderIndex", is(3)));

            verify(assessmentQuestionService).updateAssesmentQuestion(behavioralIndicatorId, assessmentQuestionId, updateRequest);
            verify(assessmentQuestionMapper).toDto(updatedEntity);
        }

        @Test
        @DisplayName("Should return 404 when question not found for update")
        void shouldReturn404WhenQuestionNotFoundForUpdate() throws Exception {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            AssessmentQuestion updateRequest = new AssessmentQuestion();
            updateRequest.setQuestionText("Updated text");

            when(assessmentQuestionService.updateAssesmentQuestion(behavioralIndicatorId, nonExistentId, updateRequest))
                    .thenReturn(null);

            // When & Then
            mockMvc.perform(put("/api/competencies/{competencyId}/bi/{behavioralIndicatorId}/questions/{questionId}", 
                            competencyId, behavioralIndicatorId, nonExistentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isNotFound());

            verify(assessmentQuestionService).updateAssesmentQuestion(behavioralIndicatorId, nonExistentId, updateRequest);
            verifyNoInteractions(assessmentQuestionMapper);
        }

        @Test
        @DisplayName("Should handle Russian text in update request")
        void shouldHandleRussianTextInUpdateRequest() throws Exception {
            // Given
            AssessmentQuestion russianUpdateRequest = new AssessmentQuestion();
            russianUpdateRequest.setQuestionText("Обновленный вопрос о командной работе");
            russianUpdateRequest.setScoringRubric("Обновленные критерии оценки");
            russianUpdateRequest.setQuestionType(QuestionType.FREQUENCY_SCALE);

            AssessmentQuestion updatedRussianEntity = new AssessmentQuestion();
            updatedRussianEntity.setId(assessmentQuestionId);
            updatedRussianEntity.setQuestionText("Обновленный вопрос о командной работе");

            AssessmentQuestionDto russianUpdateResponseDto = new AssessmentQuestionDto(
                    assessmentQuestionId, behavioralIndicatorId,
                    "Обновленный вопрос о командной работе", QuestionType.FREQUENCY_SCALE,
                    null, "Обновленные критерии оценки",
                    null, DifficultyLevel.INTERMEDIATE, true, 1
            );

            when(assessmentQuestionService.updateAssesmentQuestion(behavioralIndicatorId, assessmentQuestionId, russianUpdateRequest))
                    .thenReturn(updatedRussianEntity);
            when(assessmentQuestionMapper.toDto(updatedRussianEntity)).thenReturn(russianUpdateResponseDto);

            // When & Then
            mockMvc.perform(put("/api/competencies/{competencyId}/bi/{behavioralIndicatorId}/questions/{questionId}", 
                            competencyId, behavioralIndicatorId, assessmentQuestionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding("UTF-8")
                            .content(objectMapper.writeValueAsString(russianUpdateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.questionText", is("Обновленный вопрос о командной работе")))
                    .andExpect(jsonPath("$.scoringRubric", is("Обновленные критерии оценки")));
        }
    }

    @Nested
    @DisplayName("DELETE /api/competencies/{competencyId}/bi/{behavioralIndicatorId}/questions/{questionId} - Delete Question")
    class DeleteQuestionTests {

        @Test
        @DisplayName("Should delete assessment question successfully")
        void shouldDeleteAssessmentQuestionSuccessfully() throws Exception {
            // Given
            doNothing().when(assessmentQuestionService).deleteAssesmentQuestion(behavioralIndicatorId, assessmentQuestionId);

            // When & Then
            mockMvc.perform(delete("/api/competencies/{competencyId}/bi/{behavioralIndicatorId}/questions/{questionId}", 
                            competencyId, behavioralIndicatorId, assessmentQuestionId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isNoContent());

            verify(assessmentQuestionService).deleteAssesmentQuestion(behavioralIndicatorId, assessmentQuestionId);
        }

        @Test
        @DisplayName("Should return 204 even when question not found")
        void shouldReturn204EvenWhenQuestionNotFound() throws Exception {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            doNothing().when(assessmentQuestionService).deleteAssesmentQuestion(behavioralIndicatorId, nonExistentId);

            // When & Then
            mockMvc.perform(delete("/api/competencies/{competencyId}/bi/{behavioralIndicatorId}/questions/{questionId}", 
                            competencyId, behavioralIndicatorId, nonExistentId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            verify(assessmentQuestionService).deleteAssesmentQuestion(behavioralIndicatorId, nonExistentId);
        }

        @Test
        @DisplayName("Should handle invalid UUID in delete request")
        void shouldHandleInvalidUuidInDeleteRequest() throws Exception {
            // When & Then
            mockMvc.perform(delete("/api/competencies/{competencyId}/bi/{behavioralIndicatorId}/questions/{questionId}", 
                            "invalid-uuid", "invalid-uuid", "invalid-uuid")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(assessmentQuestionService);
        }
    }

    @Nested
    @DisplayName("Content Type and Headers Tests")
    class ContentTypeAndHeadersTests {

        @Test
        @DisplayName("Should handle missing Content-Type header")
        void shouldHandleMissingContentTypeHeader() throws Exception {
            // Given
            AssessmentQuestion requestEntity = new AssessmentQuestion();
            requestEntity.setQuestionText("Test");

            // When & Then
            mockMvc.perform(post("/api/competencies/{competencyId}/bi/{behavioralIndicatorId}/questions", 
                            competencyId, behavioralIndicatorId)
                            .content(objectMapper.writeValueAsString(requestEntity))) // No Content-Type set
                    .andExpect(status().isUnsupportedMediaType());

            verifyNoInteractions(assessmentQuestionService);
        }

        @Test
        @DisplayName("Should handle unsupported media type")
        void shouldHandleUnsupportedMediaType() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/competencies/{competencyId}/bi/{behavioralIndicatorId}/questions", 
                            competencyId, behavioralIndicatorId)
                            .contentType(MediaType.APPLICATION_XML)
                            .content("<xml>test</xml>"))
                    .andExpect(status().isUnsupportedMediaType());

            verifyNoInteractions(assessmentQuestionService);
        }

        @Test
        @DisplayName("Should accept application/json content type")
        void shouldAcceptApplicationJsonContentType() throws Exception {
            // Given
            AssessmentQuestion requestEntity = new AssessmentQuestion();
            requestEntity.setQuestionText("Test");

            when(assessmentQuestionService.createAssesmentQuestion(eq(behavioralIndicatorId), any(AssessmentQuestion.class)))
                    .thenReturn(mockEntity);
            when(assessmentQuestionMapper.toDto(mockEntity)).thenReturn(mockDto);

            // When & Then
            mockMvc.perform(post("/api/competencies/{competencyId}/bi/{behavioralIndicatorId}/questions", 
                            competencyId, behavioralIndicatorId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestEntity)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }
    }

    // Helper methods for creating test data
    private AssessmentQuestion createTestEntity(String questionText, QuestionType questionType, int orderIndex) {
        AssessmentQuestion entity = new AssessmentQuestion();
        entity.setId(UUID.randomUUID());
        entity.setBehavioralIndicatorId(behavioralIndicatorId);
        entity.setQuestionText(questionText);
        entity.setQuestionType(questionType);
        entity.setAnswerOptions(getAnswerOptionsForQuestionType(questionType));
        entity.setScoringRubric("Scoring for " + questionText);
        entity.setTimeLimit(300);
        entity.setDifficultyLevel(DifficultyLevel.INTERMEDIATE);
        entity.setActive(true);
        entity.setOrderIndex(orderIndex);
        return entity;
    }

    private AssessmentQuestionDto createTestDto(String questionText, QuestionType questionType, int orderIndex) {
        return new AssessmentQuestionDto(
                UUID.randomUUID(),
                behavioralIndicatorId,
                questionText,
                questionType,
                getAnswerOptionsForQuestionType(questionType),
                "Scoring for " + questionText,
                300,
                DifficultyLevel.INTERMEDIATE,
                true,
                orderIndex
        );
    }

    private List<Map<String, Object>> getAnswerOptionsForQuestionType(QuestionType questionType) {
        return switch (questionType) {
            case LIKERT_SCALE, FREQUENCY_SCALE -> createLikertScaleAnswerOptions();
            case MULTIPLE_CHOICE -> createMultipleChoiceAnswerOptions();
            case SITUATIONAL_JUDGMENT -> createSituationalJudgmentAnswerOptions();
            case SELF_REFLECTION -> createSelfReflectionAnswerOptions();
            case PEER_FEEDBACK -> createPeerFeedbackAnswerOptions();
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

    private List<Map<String, Object>> createPeerFeedbackAnswerOptions() {
        List<Map<String, Object>> options = new ArrayList<>();

        Map<String, Object> option1 = new HashMap<>();
        option1.put("feedback_type", "observed_behavior");
        option1.put("prompt", "Опишите наблюдаемое поведение");
        option1.put("rating_scale", 5);
        options.add(option1);

        Map<String, Object> option2 = new HashMap<>();
        option2.put("feedback_type", "improvement_suggestions");
        option2.put("prompt", "Предложения по улучшению");
        option2.put("rating_scale", 5);
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
}