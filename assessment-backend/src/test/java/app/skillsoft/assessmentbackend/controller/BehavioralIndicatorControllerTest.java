package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.dto.AssessmentQuestionDto;
import app.skillsoft.assessmentbackend.domain.dto.BehavioralIndicatorDto;
import app.skillsoft.assessmentbackend.domain.dto.request.CreateIndicatorRequest;
import app.skillsoft.assessmentbackend.domain.dto.request.UpdateIndicatorRequest;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
        testIndicator.setObservabilityLevel(ObservabilityLevel.DIRECTLY_OBSERVABLE);
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
    @WithMockUser
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
    @WithMockUser
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
    @WithMockUser
    @DisplayName("POST /api/behavioral-indicators - Should create new indicator")
    void shouldCreateNewIndicator() throws Exception {
        when(behavioralIndicatorMapper.fromCreateRequest(any(CreateIndicatorRequest.class)))
                .thenReturn(testIndicator);
        when(behavioralIndicatorService.createBehavioralIndicator(any(UUID.class), any(BehavioralIndicator.class)))
                .thenReturn(testIndicator);
        when(behavioralIndicatorMapper.toDto(any(BehavioralIndicator.class)))
                .thenReturn(testIndicatorDto);

        // Description must be at least 20 characters per validation
        String validDescription = "This is a valid behavioral indicator description";

        String requestBody = String.format("""
                {
                    "competencyId": "%s",
                    "title": "Test Indicator Title",
                    "description": "%s",
                    "observabilityLevel": "DIRECTLY_OBSERVABLE",
                    "measurementType": "FREQUENCY",
                    "weight": 0.5
                }""", competencyId, validDescription);

        mockMvc.perform(post("/api/behavioral-indicators")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title").value(testIndicator.getTitle()));
    }

    @Test
    @WithMockUser
    @DisplayName("PUT /api/behavioral-indicators/{id} - Should update existing indicator")
    void shouldUpdateExistingIndicator() throws Exception {
        when(behavioralIndicatorMapper.fromUpdateRequest(any(UpdateIndicatorRequest.class)))
                .thenReturn(testIndicator);
        when(behavioralIndicatorService.updateBehavioralIndicator(any(UUID.class), any(BehavioralIndicator.class)))
                .thenReturn(testIndicator);
        when(behavioralIndicatorMapper.toDto(any(BehavioralIndicator.class)))
                .thenReturn(testIndicatorDto);

        // Description must be at least 20 characters per validation
        String validDescription = "This is an updated behavioral indicator description";

        String requestBody = String.format("""
                {
                    "title": "Updated Indicator Title",
                    "description": "%s",
                    "observabilityLevel": "PARTIALLY_OBSERVABLE",
                    "measurementType": "QUALITY",
                    "weight": 0.8,
                    "isActive": false
                }""", validDescription);

        mockMvc.perform(put("/api/behavioral-indicators/" + testIndicator.getId())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title").value(testIndicator.getTitle()));
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /api/behavioral-indicators/{id} - Should delete indicator")
    void shouldDeleteIndicator() throws Exception {
        mockMvc.perform(delete("/api/behavioral-indicators/" + testIndicator.getId())
                .with(csrf()))
                .andExpect(status().isNoContent());
    }
}
