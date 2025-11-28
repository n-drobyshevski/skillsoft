package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.entities.ApprovalStatus;
import app.skillsoft.assessmentbackend.domain.entities.Competency;
import app.skillsoft.assessmentbackend.domain.entities.CompetencyCategory;
import app.skillsoft.assessmentbackend.domain.entities.ProficiencyLevel;
import app.skillsoft.assessmentbackend.repository.CompetencyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for CompetencyController
 * 
 * Tests cover:
 * - CRUD operations via HTTP endpoints
 * - Request/response validation
 * - Error handling scenarios
 * - JSON serialization/deserialization
 * - Russian content handling
 * - Standard codes JSONB functionality
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-test.properties")
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional
@DisplayName("Competency Controller Integration Tests")
class CompetencyControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;
    
    private MockMvc mockMvc;

    @Autowired
    private CompetencyRepository competencyRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Map<String, Object> standardCodes;

    @BeforeEach
    void setUp() {
        // Setup MockMvc
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // Clear any existing data
        competencyRepository.deleteAll();
        competencyRepository.flush();
        
        // Setup standard codes structure
        standardCodes = new HashMap<>();
        Map<String, Object> escoMapping = new HashMap<>();
        escoMapping.put("code", "S7.1.1");
        escoMapping.put("name", "develop organisational strategies");
        escoMapping.put("confidence", "HIGH");
        standardCodes.put("ESCO", escoMapping);
    }

    @Nested
    @DisplayName("GET /api/competencies Tests")
    class GetAllCompetenciesTests {

        @Test
        @DisplayName("Should return empty list when no competencies exist")
        void shouldReturnEmptyListWhenNoCompetencies() throws Exception {
            mockMvc.perform(get("/api/competencies"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("Should return all competencies with Russian content")
        @Transactional
        void shouldReturnAllCompetenciesWithRussianContent() throws Exception {
            // Create test competency with Russian content
            Competency competency = new Competency();
            competency.setName("Стратегическое лидерство");
            competency.setDescription("Способность определять долгосрочные цели и вдохновлять команду");
            competency.setCategory(CompetencyCategory.LEADERSHIP);
            competency.setLevel(ProficiencyLevel.ADVANCED);
            competency.setStandardCodes(standardCodes);
            competency.setActive(true);
            competency.setApprovalStatus(ApprovalStatus.APPROVED);
            competency.setVersion(1);
            competency.setCreatedAt(LocalDateTime.now());
            competency.setLastModified(LocalDateTime.now());
            
            competencyRepository.save(competency);

            mockMvc.perform(get("/api/competencies"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name", is("Стратегическое лидерство")))
                    .andExpect(jsonPath("$[0].description", containsString("цели")))
                    .andExpect(jsonPath("$[0].category", is("LEADERSHIP")))
                    .andExpect(jsonPath("$[0].level", is("ADVANCED")))
                    .andExpect(jsonPath("$[0].standardCodes.ESCO.code", is("S7.1.1")))
                    .andExpect(jsonPath("$[0].standardCodes.ESCO.confidence", is("HIGH")))
                    .andExpect(jsonPath("$[0].isActive", is(true)));
        }
    }

    @Nested
    @DisplayName("POST /api/competencies Tests")
    class CreateCompetencyTests {

        @Test
        @DisplayName("Should create competency with Russian content and standard codes")
        void shouldCreateCompetencyWithRussianContentAndStandardCodes() throws Exception {
            Map<String, Object> competencyRequest = new HashMap<>();
            competencyRequest.put("name", "Эффективная коммуникация");
            competencyRequest.put("description", "Навыки четкого изложения идей и активного слушания");
            competencyRequest.put("category", "COMMUNICATION");
            competencyRequest.put("level", "PROFICIENT");
            competencyRequest.put("standardCodes", standardCodes);
            competencyRequest.put("isActive", true);
            competencyRequest.put("approvalStatus", "PENDING_REVIEW");

            mockMvc.perform(post("/api/competencies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(competencyRequest)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name", is("Эффективная коммуникация")))
                    .andExpect(jsonPath("$.description", containsString("идей")))
                    .andExpect(jsonPath("$.category", is("COMMUNICATION")))
                    .andExpect(jsonPath("$.level", is("PROFICIENT")))
                    .andExpect(jsonPath("$.standardCodes.ESCO.code", is("S7.1.1")))
                    .andExpect(jsonPath("$.isActive", is(true)))
                    .andExpect(jsonPath("$.approvalStatus", is("PENDING_REVIEW")))
                    .andExpect(jsonPath("$.version", is(1)))
                    .andExpect(jsonPath("$.id", notNullValue()))
                    .andExpect(jsonPath("$.createdAt", notNullValue()))
                    .andExpect(jsonPath("$.lastModified", notNullValue()));
        }

        @Test
        @DisplayName("Should return 400 when creating competency without required fields")
        void shouldReturn400WhenMissingRequiredFields() throws Exception {
            Map<String, Object> invalidRequest = new HashMap<>();
            invalidRequest.put("description", "Missing name field");

            mockMvc.perform(post("/api/competencies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should create competency with complex standard codes")
        void shouldCreateCompetencyWithComplexStandardCodes() throws Exception {
            Map<String, Object> complexStandardCodes = new HashMap<>();
            
            Map<String, Object> esco = new HashMap<>();
            esco.put("code", "S2.1.1");
            esco.put("name", "communicate with others");
            esco.put("confidence", "HIGH");
            complexStandardCodes.put("ESCO", esco);
            
            Map<String, Object> onet = new HashMap<>();
            onet.put("code", "2.A.1.b");
            onet.put("name", "Oral Comprehension");
            onet.put("confidence", "VERIFIED");
            complexStandardCodes.put("ONET", onet);
            
            Map<String, Object> bigFive = new HashMap<>();
            bigFive.put("code", "EXTRAVERSION");
            bigFive.put("name", "Extraversion traits");
            bigFive.put("confidence", "MODERATE");
            complexStandardCodes.put("BIG_FIVE", bigFive);

            Map<String, Object> competencyRequest = new HashMap<>();
            competencyRequest.put("name", "Комплексная коммуникация");
            competencyRequest.put("description", "Многоуровневые навыки коммуникации");
            competencyRequest.put("category", "COMMUNICATION");
            competencyRequest.put("level", "EXPERT");
            competencyRequest.put("standardCodes", complexStandardCodes);
            competencyRequest.put("isActive", true);
            competencyRequest.put("approvalStatus", "APPROVED");

            mockMvc.perform(post("/api/competencies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(competencyRequest)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.standardCodes.ESCO.code", is("S2.1.1")))
                    .andExpect(jsonPath("$.standardCodes.ONET.code", is("2.A.1.b")))
                    .andExpect(jsonPath("$.standardCodes.BIG_FIVE.code", is("EXTRAVERSION")))
                    .andExpect(jsonPath("$.standardCodes.ESCO.confidence", is("HIGH")))
                    .andExpect(jsonPath("$.standardCodes.ONET.confidence", is("VERIFIED")))
                    .andExpect(jsonPath("$.standardCodes.BIG_FIVE.confidence", is("MODERATE")));
        }
    }

    @Nested
    @DisplayName("GET /api/competencies/{id} Tests")
    class GetCompetencyByIdTests {

        @Test
        @DisplayName("Should return competency by ID with Russian content")
        @Transactional
        void shouldReturnCompetencyByIdWithRussianContent() throws Exception {
            // Create and save competency
            Competency competency = new Competency();
            competency.setName("Критическое мышление");
            competency.setDescription("Способность анализировать информацию и принимать обоснованные решения");
            competency.setCategory(CompetencyCategory.COGNITIVE);
            competency.setLevel(ProficiencyLevel.ADVANCED);
            competency.setStandardCodes(standardCodes);
            competency.setActive(true);
            competency.setApprovalStatus(ApprovalStatus.APPROVED);
            competency.setVersion(1);
            competency.setCreatedAt(LocalDateTime.now());
            competency.setLastModified(LocalDateTime.now());
            
            Competency saved = competencyRepository.save(competency);

            mockMvc.perform(get("/api/competencies/{id}", saved.getId()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(saved.getId().toString())))
                    .andExpect(jsonPath("$.name", is("Критическое мышление")))
                    .andExpect(jsonPath("$.description", containsString("анализировать")))
                    .andExpect(jsonPath("$.category", is("COGNITIVE")))
                    .andExpect(jsonPath("$.standardCodes.ESCO.code", is("S7.1.1")));
        }

        @Test
        @DisplayName("Should return 404 when competency not found")
        void shouldReturn404WhenCompetencyNotFound() throws Exception {
            UUID nonExistentId = UUID.randomUUID();

            mockMvc.perform(get("/api/competencies/{id}", nonExistentId))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/competencies/{id} Tests")
    class UpdateCompetencyTests {

        @Test
        @DisplayName("Should update competency with new Russian content and standard codes")
        @Transactional
        void shouldUpdateCompetencyWithNewRussianContentAndStandardCodes() throws Exception {
            // Create initial competency
            Competency competency = new Competency();
            competency.setName("Старое название");
            competency.setDescription("Старое описание");
            competency.setCategory(CompetencyCategory.COMMUNICATION);
            competency.setLevel(ProficiencyLevel.DEVELOPING);
            competency.setActive(false);
            competency.setApprovalStatus(ApprovalStatus.DRAFT);
            competency.setVersion(1);
            competency.setCreatedAt(LocalDateTime.now());
            competency.setLastModified(LocalDateTime.now());
            
            Competency saved = competencyRepository.save(competency);

            // Prepare update with new standard codes
            Map<String, Object> newStandardCodes = new HashMap<>();
            Map<String, Object> newEsco = new HashMap<>();
            newEsco.put("code", "S4.7.1");
            newEsco.put("name", "demonstrate empathy");
            newEsco.put("confidence", "VERIFIED");
            newStandardCodes.put("ESCO", newEsco);

            Map<String, Object> updateRequest = new HashMap<>();
            updateRequest.put("id", saved.getId().toString());
            updateRequest.put("name", "Эмпатия и социальная осознанность");
            updateRequest.put("description", "Обновленное описание: способность понимать эмоции других");
            updateRequest.put("category", "EMOTIONAL_INTELLIGENCE");
            updateRequest.put("level", "PROFICIENT");
            updateRequest.put("standardCodes", newStandardCodes);
            updateRequest.put("isActive", true);
            updateRequest.put("approvalStatus", "APPROVED");
            updateRequest.put("version", 1);

            mockMvc.perform(put("/api/competencies/{id}", saved.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(saved.getId().toString())))
                    .andExpect(jsonPath("$.name", is("Эмпатия и социальная осознанность")))
                    .andExpect(jsonPath("$.description", containsString("эмоции")))
                    .andExpect(jsonPath("$.category", is("EMOTIONAL_INTELLIGENCE")))
                    .andExpect(jsonPath("$.level", is("PROFICIENT")))
                    .andExpect(jsonPath("$.standardCodes.ESCO.code", is("S4.7.1")))
                    .andExpect(jsonPath("$.standardCodes.ESCO.confidence", is("VERIFIED")))
                    .andExpect(jsonPath("$.isActive", is(true)))
                    .andExpect(jsonPath("$.approvalStatus", is("APPROVED")))
                    .andExpect(jsonPath("$.version", is(2))); // Version should increment
        }

        @Test
        @DisplayName("Should return 404 when updating non-existent competency")
        void shouldReturn404WhenUpdatingNonExistentCompetency() throws Exception {
            UUID nonExistentId = UUID.randomUUID();
            
            Map<String, Object> updateRequest = new HashMap<>();
            updateRequest.put("name", "Не существует");
            updateRequest.put("description", "Это не должно работать");
            updateRequest.put("category", "LEADERSHIP");
            updateRequest.put("level", "NOVICE");

            mockMvc.perform(put("/api/competencies/{id}", nonExistentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/competencies/{id} Tests")
    class DeleteCompetencyTests {

        @Test
        @DisplayName("Should delete existing competency")
        @Transactional
        void shouldDeleteExistingCompetency() throws Exception {
            // Create competency
            Competency competency = new Competency();
            competency.setName("Удаляемая компетенция");
            competency.setDescription("Эта компетенция будет удалена");
            competency.setCategory(CompetencyCategory.LEADERSHIP);
            competency.setLevel(ProficiencyLevel.NOVICE);
            competency.setActive(true);
            competency.setApprovalStatus(ApprovalStatus.DRAFT);
            competency.setVersion(1);
            competency.setCreatedAt(LocalDateTime.now());
            competency.setLastModified(LocalDateTime.now());
            
            Competency saved = competencyRepository.save(competency);

            mockMvc.perform(delete("/api/competencies/{id}", saved.getId()))
                    .andDo(print())
                    .andExpect(status().isNoContent());

            // Verify deletion
            mockMvc.perform(get("/api/competencies/{id}", saved.getId()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 404 when deleting non-existent competency")
        void shouldReturn404WhenDeletingNonExistentCompetency() throws Exception {
            UUID nonExistentId = UUID.randomUUID();

            mockMvc.perform(delete("/api/competencies/{id}", nonExistentId))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle malformed JSON gracefully")
        void shouldHandleMalformedJsonGracefully() throws Exception {
            String malformedJson = "{ name: 'missing quotes', invalid syntax }";

            mockMvc.perform(post("/api/competencies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(malformedJson))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle invalid UUID in path")
        void shouldHandleInvalidUuidInPath() throws Exception {
            mockMvc.perform(get("/api/competencies/{id}", "invalid-uuid"))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle large Russian text content")
        void shouldHandleLargeRussianTextContent() throws Exception {
            String largeDescription = "Очень длинное описание. ".repeat(100);
            
            Map<String, Object> competencyRequest = new HashMap<>();
            competencyRequest.put("name", "Тест с длинным описанием");
            competencyRequest.put("description", largeDescription);
            competencyRequest.put("category", "LEADERSHIP");
            competencyRequest.put("level", "NOVICE");
            competencyRequest.put("isActive", true);
            competencyRequest.put("approvalStatus", "DRAFT");

            mockMvc.perform(post("/api/competencies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(competencyRequest)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.description", containsString("длинное описание")));
        }
    }
}