package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.dto.CompetencyDto;
import app.skillsoft.assessmentbackend.domain.entities.Competency;
import app.skillsoft.assessmentbackend.domain.mapper.CompetencyMapper;
import app.skillsoft.assessmentbackend.services.CompetencyService;
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

@WebMvcTest(CompetencyController.class)
@DisplayName("Competency Controller Tests")
class CompetencyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompetencyService competencyService;

    @MockBean
    private CompetencyMapper competencyMapper;

    private Competency testCompetency;
    private CompetencyDto testCompetencyDto;

    @BeforeEach
    void setUp() {
        testCompetency = new Competency();
        testCompetency.setId(UUID.randomUUID());
        testCompetency.setName("Test Competency");
        testCompetency.setDescription("Test Description");

        testCompetencyDto = new CompetencyDto();
        testCompetencyDto.setId(testCompetency.getId());
        testCompetencyDto.setName(testCompetency.getName());
        testCompetencyDto.setDescription(testCompetency.getDescription());
    }

    @Test
    @DisplayName("GET /api/competencies - Should return all competencies")
    void shouldReturnAllCompetencies() throws Exception {
        when(competencyService.listAllCompetencies())
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
