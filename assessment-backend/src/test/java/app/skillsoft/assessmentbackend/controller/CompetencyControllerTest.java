package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.dto.CompetencyDto;
import app.skillsoft.assessmentbackend.domain.dto.StandardCodesDto;
import app.skillsoft.assessmentbackend.domain.entities.ApprovalStatus;
import app.skillsoft.assessmentbackend.domain.entities.Competency;
import app.skillsoft.assessmentbackend.domain.entities.CompetencyCategory;
import app.skillsoft.assessmentbackend.domain.mapper.BehavioralIndicatorMapper;
import app.skillsoft.assessmentbackend.domain.mapper.CompetencyMapper;
import app.skillsoft.assessmentbackend.services.BehavioralIndicatorService;
import app.skillsoft.assessmentbackend.services.CompetencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CompetencyController.class)
@DisplayName("Competency Controller Tests")
class CompetencyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompetencyService competencyService;

    @MockBean
    private BehavioralIndicatorService behavioralIndicatorService;

    @MockBean
    private CompetencyMapper competencyMapper;

    @MockBean
    private BehavioralIndicatorMapper behavioralIndicatorMapper;

    private Competency testCompetency;
    private CompetencyDto testCompetencyDto;

    @BeforeEach
    void setUp() {
        StandardCodesDto standardCodes = new StandardCodesDto(); // Empty DTO

        testCompetency = new Competency();
        testCompetency.setId(UUID.randomUUID());
        testCompetency.setName("Test Competency");
        testCompetency.setDescription("Test Description");
        testCompetency.setCategory(CompetencyCategory.LEADERSHIP);
        testCompetency.setStandardCodes(standardCodes);
        testCompetency.setActive(true);
        testCompetency.setApprovalStatus(ApprovalStatus.APPROVED);
        testCompetency.setBehavioralIndicators(new ArrayList<>());
        testCompetency.setVersion(1);
        testCompetency.setCreatedAt(LocalDateTime.now());
        testCompetency.setLastModified(LocalDateTime.now());

        testCompetencyDto = new CompetencyDto(
                testCompetency.getId(),
                testCompetency.getName(),
                testCompetency.getDescription(),
                testCompetency.getCategory(),
                testCompetency.getStandardCodes(),
                testCompetency.isActive(),
                testCompetency.getApprovalStatus(),
                new ArrayList<>(),
                testCompetency.getVersion(),
                testCompetency.getCreatedAt(),
                testCompetency.getLastModified()
        );
    }

    @Test
    @DisplayName("GET /api/competencies - Should return all competencies")
    void shouldReturnAllCompetencies() throws Exception {
        when(competencyService.listCompetencies())
                .thenReturn(Arrays.asList(testCompetency));
        when(competencyMapper.toDto(any(Competency.class)))
                .thenReturn(testCompetencyDto);

        mockMvc.perform(get("/api/competencies"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(testCompetency.getId().toString()))
                .andExpect(jsonPath("$[0].name").value(testCompetency.getName()))
                .andExpect(jsonPath("$[0].description").value(testCompetency.getDescription()));
    }

    @Test
    @DisplayName("GET /api/competencies/{id} - Should return competency by id")
    void shouldReturnCompetencyById() throws Exception {
        when(competencyService.findCompetencyById(testCompetency.getId()))
                .thenReturn(Optional.of(testCompetency));
        when(competencyMapper.toDto(testCompetency))
                .thenReturn(testCompetencyDto);

        mockMvc.perform(get("/api/competencies/" + testCompetency.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(testCompetency.getId().toString()))
                .andExpect(jsonPath("$.name").value(testCompetency.getName()))
                .andExpect(jsonPath("$.description").value(testCompetency.getDescription()));
    }

    @Test
    @DisplayName("POST /api/competencies - Should create new competency")
    void shouldCreateNewCompetency() throws Exception {
        when(competencyMapper.fromDto(any(CompetencyDto.class)))
                .thenReturn(testCompetency);
        when(competencyService.createCompetency(any(Competency.class)))
                .thenReturn(testCompetency);
        when(competencyMapper.toDto(any(Competency.class)))
                .thenReturn(testCompetencyDto);

        mockMvc.perform(post("/api/competencies")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test Competency\",\"description\":\"Test Description\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value(testCompetency.getName()))
                .andExpect(jsonPath("$.description").value(testCompetency.getDescription()));
    }

    @Test
    @DisplayName("PUT /api/competencies/{id} - Should update existing competency")
    void shouldUpdateExistingCompetency() throws Exception {
        when(competencyMapper.fromDto(any(CompetencyDto.class)))
                .thenReturn(testCompetency);
        when(competencyService.updateCompetency(any(UUID.class), any(Competency.class)))
                .thenReturn(testCompetency);
        when(competencyMapper.toDto(any(Competency.class)))
                .thenReturn(testCompetencyDto);

        mockMvc.perform(put("/api/competencies/" + testCompetency.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Updated Competency\",\"description\":\"Updated Description\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value(testCompetency.getName()))
                .andExpect(jsonPath("$.description").value(testCompetency.getDescription()));
    }

    @Test
    @DisplayName("DELETE /api/competencies/{id} - Should delete competency")
    void shouldDeleteCompetency() throws Exception {
        mockMvc.perform(delete("/api/competencies/" + testCompetency.getId()))
                .andExpect(status().isNoContent());
    }
}
