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
import java.util.List;

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

    @MockBean
    private AssessmentQuestionMapper assessmentQuestionMapper;

    private BehavioralIndicator testIndicator;
    private BehavioralIndicatorDto testIndicatorDto;
    private UUID competencyId;

    @BeforeEach
    void setUp() {
        competencyId = UUID.randomUUID();

        testIndicator = new BehavioralIndicator();
        testIndicator.setId(UUID.randomUUID());
        testIndicator.setIndicatorText("Test Indicator");
        testIndicator.setDescription("Test Description");
        testIndicator.setProficiencyLevel(ProficiencyLevel.INTERMEDIATE);
        testIndicator.setMeasurementType(IndicatorMeasurementType.FREQUENCY);
        testIndicator.setWeight(1.0f);
        testIndicator.setCompetencyId(competencyId);
        testIndicator.setNotes("Test Notes");
        testIndicator.setExamples("Test Examples");
        testIndicator.setActive(true);
        testIndicator.setApprovalStatus(ApprovalStatus.PENDING);
        testIndicator.setOrderIndex(1);

        testIndicatorDto = new BehavioralIndicatorDto(
            testIndicator.getId(),
            testIndicator.getCompetencyId(),
            testIndicator.getIndicatorText(),
            testIndicator.getDescription(),
            testIndicator.getProficiencyLevel(),
            testIndicator.getMeasurementType(),
            testIndicator.getWeight(),
            testIndicator.getNotes(),
            testIndicator.getExamples(),
            testIndicator.isActive(),
            testIndicator.getApprovalStatus(),
            testIndicator.getOrderIndex()
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
                .andExpect(jsonPath("$[0].indicatorText").value(testIndicator.getIndicatorText()));
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
                .andExpect(jsonPath("$.indicatorText").value(testIndicator.getIndicatorText()));
    }

    @Test
    @DisplayName("POST /api/behavioral-indicators - Should create new indicator")
    void shouldCreateNewIndicator() throws Exception {
        when(behavioralIndicatorService.createBehavioralIndicator(any(UUID.class), any(BehavioralIndicator.class)))
                .thenReturn(testIndicator);
        when(behavioralIndicatorMapper.toDto(any(BehavioralIndicator.class)))
                .thenReturn(testIndicatorDto);

        String requestBody = String.format("""
                {
                    "indicatorText": "Test Indicator",
                    "description": "Test Description",
                    "proficiencyLevel": "INTERMEDIATE",
                    "measurementType": "FREQUENCY",
                    "weight": 1.0,
                    "notes": "Test Notes",
                    "examples": "Test Examples",
                    "active": true,
                    "approvalStatus": "PENDING",
                    "orderIndex": 1
                }""");

        mockMvc.perform(post("/api/behavioral-indicators")
                .param("competencyId", competencyId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.indicatorText").value(testIndicator.getIndicatorText()));
    }

    @Test
    @DisplayName("PUT /api/behavioral-indicators/{id} - Should update existing indicator")
    void shouldUpdateExistingIndicator() throws Exception {
        when(behavioralIndicatorService.updateBehavioralIndicator(any(UUID.class), any(BehavioralIndicator.class)))
                .thenReturn(testIndicator);
        when(behavioralIndicatorMapper.toDto(any(BehavioralIndicator.class)))
                .thenReturn(testIndicatorDto);

        String requestBody = String.format("""
                {
                    "indicatorText": "Updated Indicator",
                    "description": "Updated Description",
                    "proficiencyLevel": "ADVANCED",
                    "measurementType": "SCALE",
                    "weight": 2.0,
                    "notes": "Updated Notes",
                    "examples": "Updated Examples",
                    "active": false,
                    "approvalStatus": "APPROVED",
                    "orderIndex": 2
                }""");

        mockMvc.perform(put("/api/behavioral-indicators/" + testIndicator.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.indicatorText").value(testIndicator.getIndicatorText()));
    }

    @Test
    @DisplayName("DELETE /api/behavioral-indicators/{id} - Should delete indicator")
    void shouldDeleteIndicator() throws Exception {
        mockMvc.perform(delete("/api/behavioral-indicators/" + testIndicator.getId()))
                .andExpect(status().isNoContent());
    }
}
