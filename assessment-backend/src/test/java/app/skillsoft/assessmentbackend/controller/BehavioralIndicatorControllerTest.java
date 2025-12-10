package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.dto.AssessmentQuestionDto;
import app.skillsoft.assessmentbackend.domain.dto.BehavioralIndicatorDto;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.domain.mapper.AssessmentQuestionMapper;
import app.skillsoft.assessmentbackend.domain.mapper.BehavioralIndicatorMapper;
import app.skillsoft.assessmentbackend.services.AssessmentQuestionService;
import app.skillsoft.assessmentbackend.services.BehavioralIndicatorService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BehavioralIndicatorController.class)
@DisplayName("Behavioral Indicator Controller Tests")
class BehavioralIndicatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BehavioralIndicatorService behavioralIndicatorService;

    @MockBean
    private AssessmentQuestionService assessmentQuestionService;

    @MockBean
    private BehavioralIndicatorMapper behavioralIndicatorMapper;

    private BehavioralIndicator testIndicator;
    private BehavioralIndicatorDto testIndicatorDto;
    private UUID competencyId;
    private Competency mockCompetency;

    @BeforeEach
    void setUp() {
        competencyId = UUID.randomUUID();
        mockCompetency = new Competency();
        mockCompetency.setId(competencyId);

        testIndicator = new BehavioralIndicator();
        testIndicator.setId(UUID.randomUUID());
        testIndicator.setTitle("Test Indicator");
        testIndicator.setDescription("Test Description");
        testIndicator.setObservabilityLevel(ProficiencyLevel.PROFICIENT);
        testIndicator.setMeasurementType(IndicatorMeasurementType.FREQUENCY);
        testIndicator.setWeight(1.0f);
        testIndicator.setCompetency(mockCompetency);
        testIndicator.setExamples("Test Examples");
        testIndicator.setActive(true);
        testIndicator.setApprovalStatus(ApprovalStatus.PENDING_REVIEW);
        testIndicator.setOrderIndex(1);
        testIndicator.setContextScope(ContextScope.UNIVERSAL);

        testIndicatorDto = new BehavioralIndicatorDto(
            testIndicator.getId(),
            testIndicator.getCompetency().getId(),
            testIndicator.getTitle(),
            testIndicator.getDescription(),
            testIndicator.getObservabilityLevel(),
            testIndicator.getMeasurementType(),
            testIndicator.getWeight(),
            testIndicator.getExamples(),
            testIndicator.getCounterExamples(),
            testIndicator.isActive(),
            testIndicator.getApprovalStatus(),
            testIndicator.getOrderIndex(),
            testIndicator.getContextScope()
        );
    }

    @Test
    @DisplayName("GET /api/behavioral-indicators - Should return all indicators")
    void shouldReturnAllIndicators() throws Exception {
        when(behavioralIndicatorService.listAllBehavioralIndicators())
                .thenReturn(Arrays.asList(testIndicator));
        when(behavioralIndicatorMapper.toDto(any(BehavioralIndicator.class)))
                .thenReturn(testIndicatorDto);

        mockMvc.perform(get("/api/behavioral-indicators"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(testIndicator.getId().toString()))
                .andExpect(jsonPath("$[0].title").value(testIndicator.getTitle()));
    }

    @Test
    @DisplayName("GET /api/behavioral-indicators/{id} - Should return indicator by id")
    void shouldReturnIndicatorById() throws Exception {
        when(behavioralIndicatorService.findBehavioralIndicatorById(testIndicator.getId()))
                .thenReturn(Optional.of(testIndicator));
        when(behavioralIndicatorMapper.toDto(testIndicator))
                .thenReturn(testIndicatorDto);

        mockMvc.perform(get("/api/behavioral-indicators/" + testIndicator.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(testIndicator.getId().toString()))
                .andExpect(jsonPath("$.title").value(testIndicator.getTitle()));
    }

    @Test
    @DisplayName("POST /api/behavioral-indicators - Should create new indicator")
    void shouldCreateNewIndicator() throws Exception {
        when(behavioralIndicatorService.createBehavioralIndicator(any(UUID.class), any(BehavioralIndicator.class)))
                .thenReturn(testIndicator);
        when(behavioralIndicatorMapper.toDto(any(BehavioralIndicator.class)))
                .thenReturn(testIndicatorDto);

        String requestBody = """
                {
                    "title": "Test Indicator",
                    "description": "Test Description",
                    "observabilityLevel": "PROFICIENT",
                    "measurementType": "FREQUENCY",
                    "weight": 1.0,
                    "examples": "Test Examples",
                    "active": true,
                    "approvalStatus": "PENDING",
                    "orderIndex": 1
                }""";

        mockMvc.perform(post("/api/behavioral-indicators")
                .param("competencyId", competencyId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title").value(testIndicator.getTitle()));
    }

    @Test
    @DisplayName("PUT /api/behavioral-indicators/{id} - Should update existing indicator")
    void shouldUpdateExistingIndicator() throws Exception {
        when(behavioralIndicatorService.updateBehavioralIndicator(any(UUID.class), any(BehavioralIndicator.class)))
                .thenReturn(testIndicator);
        when(behavioralIndicatorMapper.toDto(any(BehavioralIndicator.class)))
                .thenReturn(testIndicatorDto);

        String requestBody = """
                {
                    "title": "Updated Indicator",
                    "description": "Updated Description",
                    "observabilityLevel": "ADVANCED",
                    "measurementType": "SCALE",
                    "weight": 2.0,
                    "examples": "Updated Examples",
                    "active": false,
                    "approvalStatus": "APPROVED",
                    "orderIndex": 2
                }""";

        mockMvc.perform(put("/api/behavioral-indicators/" + testIndicator.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title").value(testIndicator.getTitle()));
    }

    @Test
    @DisplayName("DELETE /api/behavioral-indicators/{id} - Should delete indicator")
    void shouldDeleteIndicator() throws Exception {
        mockMvc.perform(delete("/api/behavioral-indicators/" + testIndicator.getId()))
                .andExpect(status().isNoContent());
    }
}
