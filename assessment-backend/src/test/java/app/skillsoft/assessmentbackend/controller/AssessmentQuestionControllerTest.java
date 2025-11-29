package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.dto.AssessmentQuestionDto;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.domain.mapper.AssessmentQuestionMapper;
import app.skillsoft.assessmentbackend.services.AssessmentQuestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AssessmentQuestionController.class)
@DisplayName("Assessment Question Controller Tests")
class AssessmentQuestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AssessmentQuestionService assessmentQuestionService;

    @MockBean
    private AssessmentQuestionMapper assessmentQuestionMapper;

    private AssessmentQuestion testQuestion;
    private AssessmentQuestionDto testQuestionDto;
    private UUID behavioralIndicatorId;

    @BeforeEach
    void setUp() {
        behavioralIndicatorId = UUID.randomUUID();

        testQuestion = new AssessmentQuestion();
        testQuestion.setId(UUID.randomUUID());
        testQuestion.setQuestionText("Test Question");

        List<Map<String, Object>> answerOptions = new ArrayList<>();
        Map<String, Object> option = new HashMap<>();
        option.put("label", "Option 1");
        option.put("value", 1);
        answerOptions.add(option);

        testQuestion.setAnswerOptions(answerOptions);
        testQuestion.setQuestionType(QuestionType.MULTIPLE_CHOICE);
        testQuestion.setScoringRubric("Test Rubric");
        testQuestion.setTimeLimit(300);
        testQuestion.setDifficultyLevel(DifficultyLevel.INTERMEDIATE);
        testQuestion.setActive(true);
        testQuestion.setOrderIndex(1);

        testQuestionDto = new AssessmentQuestionDto(
            testQuestion.getId(),
            behavioralIndicatorId,
            testQuestion.getQuestionText(),
            testQuestion.getQuestionType(),
            testQuestion.getAnswerOptions(),
            testQuestion.getScoringRubric(),
            testQuestion.getTimeLimit(),
            testQuestion.getDifficultyLevel(),
            null,
            testQuestion.isActive(),
            testQuestion.getOrderIndex()
        );
    }

    @Test
    @DisplayName("GET /api/questions - Should return all questions")
    void shouldReturnAllQuestions() throws Exception {
        when(assessmentQuestionService.listAllQuestions())
                .thenReturn(Arrays.asList(testQuestion));
        when(assessmentQuestionMapper.toDto(any(AssessmentQuestion.class)))
                .thenReturn(testQuestionDto);

        mockMvc.perform(get("/api/questions"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(testQuestion.getId().toString()))
                .andExpect(jsonPath("$[0].questionText").value(testQuestion.getQuestionText()));
    }

    @Test
    @DisplayName("GET /api/questions/{id} - Should return question by id")
    void shouldReturnQuestionById() throws Exception {
        when(assessmentQuestionService.findAssesmentQuestionById(testQuestion.getId()))
                .thenReturn(Optional.of(testQuestion));
        when(assessmentQuestionMapper.toDto(testQuestion))
                .thenReturn(testQuestionDto);

        mockMvc.perform(get("/api/questions/" + testQuestion.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(testQuestion.getId().toString()))
                .andExpect(jsonPath("$.questionText").value(testQuestion.getQuestionText()));
    }

    @Test
    @DisplayName("POST /api/questions - Should create new question")
    void shouldCreateNewQuestion() throws Exception {
        when(assessmentQuestionService.createAssesmentQuestion(any(UUID.class), any(AssessmentQuestion.class)))
                .thenReturn(testQuestion);
        when(assessmentQuestionMapper.toDto(any(AssessmentQuestion.class)))
                .thenReturn(testQuestionDto);

        String requestBody = String.format("""
                {
                    "questionText": "Test Question",
                    "answerOptions": [
                        {"label": "Option 1", "value": 1},
                        {"label": "Option 2", "value": 2}
                    ],
                    "questionType": "MULTIPLE_CHOICE",
                    "scoringRubric": "Test Rubric",
                    "timeLimit": 300,
                    "difficultyLevel": "INTERMEDIATE",
                    "active": true,
                    "orderIndex": 1
                }""");

        mockMvc.perform(post("/api/questions")
                .param("behavioralIndicatorId", behavioralIndicatorId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.questionText").value(testQuestion.getQuestionText()));
    }

    @Test
    @DisplayName("PUT /api/questions/{id} - Should update existing question")
    void shouldUpdateExistingQuestion() throws Exception {
        when(assessmentQuestionService.updateAssesmentQuestion(any(UUID.class), any(AssessmentQuestion.class)))
                .thenReturn(testQuestion);
        when(assessmentQuestionMapper.toDto(any(AssessmentQuestion.class)))
                .thenReturn(testQuestionDto);

        String requestBody = String.format("""
                {
                    "questionText": "Updated Question",
                    "answerOptions": [
                        {"label": "Updated Option 1", "value": 1},
                        {"label": "Updated Option 2", "value": 2}
                    ],
                    "questionType": "MULTIPLE_CHOICE",
                    "scoringRubric": "Updated Rubric",
                    "timeLimit": 600,
                    "difficultyLevel": "EASY",
                    "active": false,
                    "orderIndex": 2
                }""");

        mockMvc.perform(put("/api/questions/" + testQuestion.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.questionText").value(testQuestion.getQuestionText()));
    }

    @Test
    @DisplayName("DELETE /api/questions/{id} - Should delete question")
    void shouldDeleteQuestion() throws Exception {
        mockMvc.perform(delete("/api/questions/" + testQuestion.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/questions/{id} - Should return 404 when question not found")
    void shouldReturn404WhenQuestionNotFound() throws Exception {
        when(assessmentQuestionService.findAssesmentQuestionById(any(UUID.class)))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/questions/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/questions/{id} - Should return 404 when updating non-existent question")
    void shouldReturn404WhenUpdatingNonExistentQuestion() throws Exception {
        when(assessmentQuestionService.updateAssesmentQuestion(any(UUID.class), any(AssessmentQuestion.class)))
                .thenReturn(null);

        String requestBody = String.format("""
                {
                    "questionText": "Updated Question",
                    "answerOptions": [
                        {"label": "Updated Option 1", "value": 1},
                        {"label": "Updated Option 2", "value": 2}
                    ],
                    "questionType": "MULTIPLE_CHOICE",
                    "scoringRubric": "Updated Rubric",
                    "timeLimit": 600,
                    "difficultyLevel": "EASY",
                    "active": false,
                    "orderIndex": 2
                }""");

        mockMvc.perform(put("/api/questions/" + UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isNotFound());
    }
}
