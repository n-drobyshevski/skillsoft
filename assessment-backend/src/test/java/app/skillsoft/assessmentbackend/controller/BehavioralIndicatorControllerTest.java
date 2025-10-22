package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.dto.BehavioralIndicatorDto;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.domain.mapper.BehavioralIndicatorMapper;
import app.skillsoft.assessmentbackend.services.BehavioralIndicatorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
 * Comprehensive controller tests for BehavioralIndicatorController
 * 
 * Tests cover:
 * - REST endpoint functionality (@WebMvcTest)
 * - HTTP request/response validation
 * - JSON serialization/deserialization
 * - Error handling and exception scenarios
 * - Service layer integration through mocks
 * - Russian content handling in HTTP context
 */
@WebMvcTest(BehavioralIndicatorController.class)
@DisplayName("BehavioralIndicator Controller Tests")
class BehavioralIndicatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BehavioralIndicatorService behavioralIndicatorService;

    @MockBean
    private BehavioralIndicatorMapper behavioralIndicatorMapper;

    private UUID competencyId;
    private UUID behavioralIndicatorId;
    private BehavioralIndicator mockEntity;
    private BehavioralIndicatorDto mockDto;

    @BeforeEach
    void setUp() {
        competencyId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        behavioralIndicatorId = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

        // Create mock entity
        Competency mockCompetency = new Competency();
        mockCompetency.setId(competencyId);
        mockCompetency.setName("Test Competency");

        mockEntity = new BehavioralIndicator();
        mockEntity.setId(behavioralIndicatorId);
        mockEntity.setCompetency(mockCompetency);
        mockEntity.setTitle("Test Behavioral Indicator");
        mockEntity.setDescription("A test behavioral indicator for controller testing");
        mockEntity.setObservabilityLevel(ProficiencyLevel.PROFICIENT);
        mockEntity.setMeasurementType(IndicatorMeasurementType.QUALITY);
        mockEntity.setWeight(0.75f);
        mockEntity.setExamples("Example behaviors for testing");
        mockEntity.setCounterExamples("Counter-example behaviors");
        mockEntity.setActive(true);
        mockEntity.setApprovalStatus(ApprovalStatus.APPROVED);
        mockEntity.setOrderIndex(1);

        // Create mock DTO
        mockDto = new BehavioralIndicatorDto(
                behavioralIndicatorId,
                competencyId,
                "Test Behavioral Indicator",
                "A test behavioral indicator for controller testing",
                ProficiencyLevel.PROFICIENT,
                IndicatorMeasurementType.QUALITY,
                0.75f,
                "Example behaviors for testing",
                "Counter-example behaviors",
                true,
                ApprovalStatus.APPROVED,
                1
        );
    }

    @Nested
    @DisplayName("GET /api/competencies/{competencyId}/bi - List Behavioral Indicators")
    class ListBehavioralIndicatorsTests {

        @Test
        @DisplayName("Should return list of behavioral indicators")
        void shouldReturnListOfBehavioralIndicators() throws Exception {
            // Given
            List<BehavioralIndicator> entities = Arrays.asList(
                    mockEntity,
                    createTestEntity("Second Indicator", 2),
                    createTestEntity("Third Indicator", 3)
            );
            List<BehavioralIndicatorDto> dtos = Arrays.asList(
                    mockDto,
                    createTestDto("Second Indicator", 2),
                    createTestDto("Third Indicator", 3)
            );

            when(behavioralIndicatorService.listBehavioralIndicators(competencyId)).thenReturn(entities);
            when(behavioralIndicatorMapper.toDto(any(BehavioralIndicator.class)))
                    .thenReturn(dtos.get(0), dtos.get(1), dtos.get(2));

            // When & Then
            mockMvc.perform(get("/api/competencies/{competencyId}/bi", competencyId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[0].id", is(behavioralIndicatorId.toString())))
                    .andExpect(jsonPath("$[0].title", is("Test Behavioral Indicator")))
                    .andExpect(jsonPath("$[0].competencyId", is(competencyId.toString())))
                    .andExpect(jsonPath("$[1].title", is("Second Indicator")))
                    .andExpect(jsonPath("$[2].title", is("Third Indicator")));

            verify(behavioralIndicatorService).listBehavioralIndicators(competencyId);
            verify(behavioralIndicatorMapper, times(3)).toDto(any(BehavioralIndicator.class));
        }

        @Test
        @DisplayName("Should return empty list when no behavioral indicators exist")
        void shouldReturnEmptyListWhenNoBehavioralIndicatorsExist() throws Exception {
            // Given
            when(behavioralIndicatorService.listBehavioralIndicators(competencyId))
                    .thenReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(get("/api/competencies/{competencyId}/bi", competencyId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(behavioralIndicatorService).listBehavioralIndicators(competencyId);
            verifyNoInteractions(behavioralIndicatorMapper);
        }

        @Test
        @DisplayName("Should handle invalid UUID format in path variable")
        void shouldHandleInvalidUuidFormatInPathVariable() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/competencies/{competencyId}/bi", "invalid-uuid")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(behavioralIndicatorService);
        }
    }

    @Nested
    @DisplayName("GET /api/competencies/{competencyId}/bi/{biId} - Get Behavioral Indicator by ID")
    class GetBehavioralIndicatorByIdTests {

        @Test
        @DisplayName("Should return behavioral indicator when found")
        void shouldReturnBehavioralIndicatorWhenFound() throws Exception {
            // Given
            when(behavioralIndicatorService.findBehavioralIndicatorById(competencyId, behavioralIndicatorId))
                    .thenReturn(Optional.of(mockEntity));
            when(behavioralIndicatorMapper.toDto(mockEntity)).thenReturn(mockDto);

            // When & Then
            mockMvc.perform(get("/api/competencies/{competencyId}/bi/{biId}", competencyId, behavioralIndicatorId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(behavioralIndicatorId.toString())))
                    .andExpect(jsonPath("$.competencyId", is(competencyId.toString())))
                    .andExpect(jsonPath("$.title", is("Test Behavioral Indicator")))
                    .andExpect(jsonPath("$.description", is("A test behavioral indicator for controller testing")))
                    .andExpect(jsonPath("$.observabilityLevel", is("PROFICIENT")))
                    .andExpect(jsonPath("$.measurementType", is("QUALITY")))
                    .andExpect(jsonPath("$.weight", is(0.75)))
                    .andExpect(jsonPath("$.examples", is("Example behaviors for testing")))
                    .andExpect(jsonPath("$.counterExamples", is("Counter-example behaviors")))
                    .andExpect(jsonPath("$.isActive", is(true)))
                    .andExpect(jsonPath("$.approvalStatus", is("APPROVED")))
                    .andExpect(jsonPath("$.orderIndex", is(1)));

            verify(behavioralIndicatorService).findBehavioralIndicatorById(competencyId, behavioralIndicatorId);
            verify(behavioralIndicatorMapper).toDto(mockEntity);
        }

        @Test
        @DisplayName("Should throw exception when behavioral indicator not found")
        void shouldThrowExceptionWhenBehavioralIndicatorNotFound() throws Exception {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(behavioralIndicatorService.findBehavioralIndicatorById(competencyId, nonExistentId))
                    .thenReturn(Optional.empty());

            // When & Then - The controller throws RuntimeException which gets wrapped in ServletException
            mockMvc.perform(get("/api/competencies/{competencyId}/bi/{biId}", competencyId, nonExistentId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError());

            verify(behavioralIndicatorService).findBehavioralIndicatorById(competencyId, nonExistentId);
            verifyNoInteractions(behavioralIndicatorMapper);
        }

        @Test
        @DisplayName("Should handle invalid UUID format in path variables")
        void shouldHandleInvalidUuidFormatInPathVariables() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/competencies/{competencyId}/bi/{biId}", "invalid-uuid", "invalid-uuid")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(behavioralIndicatorService);
        }
    }

    @Nested
    @DisplayName("POST /api/competencies/{competencyId}/bi - Create Behavioral Indicator")
    class CreateBehavioralIndicatorTests {

        @Test
        @DisplayName("Should create behavioral indicator successfully")
        void shouldCreateBehavioralIndicatorSuccessfully() throws Exception {
            // Given
            BehavioralIndicatorDto requestDto = new BehavioralIndicatorDto(
                    null, // ID should be null for creation
                    competencyId,
                    "New Behavioral Indicator",
                    "Description for new indicator",
                    ProficiencyLevel.DEVELOPING,
                    IndicatorMeasurementType.FREQUENCY,
                    0.6f,
                    "New examples",
                    "New counter-examples",
                    true,
                    ApprovalStatus.DRAFT,
                    2
            );

            BehavioralIndicator createdEntity = new BehavioralIndicator();
            createdEntity.setId(UUID.randomUUID());
            createdEntity.setTitle("New Behavioral Indicator");

            BehavioralIndicatorDto responseDto = new BehavioralIndicatorDto(
                    createdEntity.getId(),
                    competencyId,
                    "New Behavioral Indicator",
                    "Description for new indicator",
                    ProficiencyLevel.DEVELOPING,
                    IndicatorMeasurementType.FREQUENCY,
                    0.6f,
                    "New examples",
                    "New counter-examples",
                    true,
                    ApprovalStatus.DRAFT,
                    2
            );

            when(behavioralIndicatorMapper.fromDto(any(BehavioralIndicatorDto.class))).thenReturn(mockEntity);
            when(behavioralIndicatorService.createBehavioralIndicator(eq(competencyId), any(BehavioralIndicator.class)))
                    .thenReturn(createdEntity);
            when(behavioralIndicatorMapper.toDto(createdEntity)).thenReturn(responseDto);

            // When & Then
            mockMvc.perform(post("/api/competencies/{competencyId}/bi", competencyId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(createdEntity.getId().toString())))
                    .andExpect(jsonPath("$.title", is("New Behavioral Indicator")))
                    .andExpect(jsonPath("$.competencyId", is(competencyId.toString())))
                    .andExpect(jsonPath("$.observabilityLevel", is("DEVELOPING")))
                    .andExpect(jsonPath("$.measurementType", is("FREQUENCY")))
                    .andExpect(jsonPath("$.weight", is(0.6)))
                    .andExpect(jsonPath("$.approvalStatus", is("DRAFT")));

            verify(behavioralIndicatorMapper).fromDto(any(BehavioralIndicatorDto.class));
            verify(behavioralIndicatorService).createBehavioralIndicator(eq(competencyId), any(BehavioralIndicator.class));
            verify(behavioralIndicatorMapper).toDto(createdEntity);
        }

        @Test
        @DisplayName("Should handle validation errors in request body")
        void shouldHandleValidationErrorsInRequestBody() throws Exception {
            // Given - Invalid JSON (missing required fields)
            String invalidJson = "{}";
            
            when(behavioralIndicatorMapper.fromDto(any(BehavioralIndicatorDto.class))).thenReturn(mockEntity);
            when(behavioralIndicatorService.createBehavioralIndicator(eq(competencyId), any(BehavioralIndicator.class)))
                    .thenReturn(mockEntity);
            when(behavioralIndicatorMapper.toDto(mockEntity)).thenReturn(mockDto);

            // When & Then
            mockMvc.perform(post("/api/competencies/{competencyId}/bi", competencyId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isOk()); // Controller doesn't validate, service does

            verify(behavioralIndicatorMapper).fromDto(any(BehavioralIndicatorDto.class));
        }

        @Test
        @DisplayName("Should handle service layer exceptions")
        void shouldHandleServiceLayerExceptions() throws Exception {
            // Given
            BehavioralIndicatorDto requestDto = new BehavioralIndicatorDto(
                    null, competencyId, "Test", "Description", 
                    ProficiencyLevel.PROFICIENT, IndicatorMeasurementType.QUALITY,
                    0.5f, "Examples", "Counter-examples", true, 
                    ApprovalStatus.DRAFT, 1
            );

            when(behavioralIndicatorMapper.fromDto(any(BehavioralIndicatorDto.class))).thenReturn(mockEntity);
            when(behavioralIndicatorService.createBehavioralIndicator(eq(competencyId), any(BehavioralIndicator.class)))
                    .thenThrow(new RuntimeException("Competency not found"));

            // When & Then
            mockMvc.perform(post("/api/competencies/{competencyId}/bi", competencyId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isInternalServerError());

            verify(behavioralIndicatorService).createBehavioralIndicator(eq(competencyId), any(BehavioralIndicator.class));
        }

        @Test
        @DisplayName("Should handle malformed JSON")
        void shouldHandleMalformedJson() throws Exception {
            // Given
            String malformedJson = "{\"title\": \"Test\", \"invalid\": }";

            // When & Then
            mockMvc.perform(post("/api/competencies/{competencyId}/bi", competencyId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(malformedJson))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(behavioralIndicatorService);
        }
    }

    @Nested
    @DisplayName("PUT /api/competencies/{competencyId}/bi/{biId} - Update Behavioral Indicator")
    class UpdateBehavioralIndicatorTests {

        @Test
        @DisplayName("Should update behavioral indicator successfully")
        void shouldUpdateBehavioralIndicatorSuccessfully() throws Exception {
            // Given
            BehavioralIndicatorDto updateDto = new BehavioralIndicatorDto(
                    behavioralIndicatorId,
                    competencyId,
                    "Updated Behavioral Indicator",
                    "Updated description",
                    ProficiencyLevel.ADVANCED,
                    IndicatorMeasurementType.IMPACT,
                    0.9f,
                    "Updated examples",
                    "Updated counter-examples",
                    false,
                    ApprovalStatus.PENDING_REVIEW,
                    3
            );

            BehavioralIndicator updatedEntity = new BehavioralIndicator();
            updatedEntity.setId(behavioralIndicatorId);
            updatedEntity.setTitle("Updated Behavioral Indicator");

            when(behavioralIndicatorMapper.fromDto(updateDto)).thenReturn(mockEntity);
            when(behavioralIndicatorService.updateBehavioralIndicator(competencyId, behavioralIndicatorId, mockEntity))
                    .thenReturn(updatedEntity);
            when(behavioralIndicatorMapper.toDto(updatedEntity)).thenReturn(updateDto);

            // When & Then
            mockMvc.perform(put("/api/competencies/{competencyId}/bi/{biId}", competencyId, behavioralIndicatorId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(behavioralIndicatorId.toString())))
                    .andExpect(jsonPath("$.title", is("Updated Behavioral Indicator")))
                    .andExpect(jsonPath("$.observabilityLevel", is("ADVANCED")))
                    .andExpect(jsonPath("$.measurementType", is("IMPACT")))
                    .andExpect(jsonPath("$.weight", is(0.9)))
                    .andExpect(jsonPath("$.approvalStatus", is("PENDING_REVIEW")));

            verify(behavioralIndicatorMapper).fromDto(updateDto);
            verify(behavioralIndicatorService).updateBehavioralIndicator(competencyId, behavioralIndicatorId, mockEntity);
            verify(behavioralIndicatorMapper).toDto(updatedEntity);
        }

        @Test
        @DisplayName("Should throw exception when behavioral indicator not found")
        void shouldThrowExceptionWhenBehavioralIndicatorNotFound() throws Exception {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            BehavioralIndicatorDto updateDto = new BehavioralIndicatorDto(
                    nonExistentId, competencyId, "Updated Title", "Updated Description",
                    ProficiencyLevel.PROFICIENT, IndicatorMeasurementType.QUALITY,
                    0.5f, "Examples", "Counter-examples", true,
                    ApprovalStatus.APPROVED, 1
            );

            when(behavioralIndicatorMapper.fromDto(updateDto)).thenReturn(mockEntity);
            when(behavioralIndicatorService.updateBehavioralIndicator(competencyId, nonExistentId, mockEntity))
                    .thenThrow(new RuntimeException("Behavioral indicator not found"));

            // When & Then - The controller catches and re-throws RuntimeException
            mockMvc.perform(put("/api/competencies/{competencyId}/bi/{biId}", competencyId, nonExistentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isInternalServerError());

            verify(behavioralIndicatorService).updateBehavioralIndicator(competencyId, nonExistentId, mockEntity);
        }

        @Test
        @DisplayName("Should handle invalid path variables")
        void shouldHandleInvalidPathVariables() throws Exception {
            // Given
            BehavioralIndicatorDto updateDto = new BehavioralIndicatorDto(
                    behavioralIndicatorId, competencyId, "Title", "Description",
                    ProficiencyLevel.PROFICIENT, IndicatorMeasurementType.QUALITY,
                    0.5f, "Examples", "Counter-examples", true,
                    ApprovalStatus.APPROVED, 1
            );

            // When & Then
            mockMvc.perform(put("/api/competencies/{competencyId}/bi/{biId}", "invalid-uuid", "invalid-uuid")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(behavioralIndicatorService);
        }
    }

    @Nested
    @DisplayName("DELETE /api/competencies/{competencyId}/bi/{biId} - Delete Behavioral Indicator")
    class DeleteBehavioralIndicatorTests {

        @Test
        @DisplayName("Should delete behavioral indicator successfully")
        void shouldDeleteBehavioralIndicatorSuccessfully() throws Exception {
            // Given
            doNothing().when(behavioralIndicatorService).deleteBehavioralIndicator(competencyId, behavioralIndicatorId);

            // When & Then
            mockMvc.perform(delete("/api/competencies/{competencyId}/bi/{biId}", competencyId, behavioralIndicatorId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk());

            verify(behavioralIndicatorService).deleteBehavioralIndicator(competencyId, behavioralIndicatorId);
        }

        @Test
        @DisplayName("Should throw exception when behavioral indicator not found during deletion")
        void shouldThrowExceptionWhenBehavioralIndicatorNotFoundDuringDeletion() throws Exception {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            doThrow(new RuntimeException("Behavioral indicator not found"))
                    .when(behavioralIndicatorService).deleteBehavioralIndicator(competencyId, nonExistentId);

            // When & Then - The controller catches and re-throws RuntimeException
            mockMvc.perform(delete("/api/competencies/{competencyId}/bi/{biId}", competencyId, nonExistentId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError());

            verify(behavioralIndicatorService).deleteBehavioralIndicator(competencyId, nonExistentId);
        }

        @Test
        @DisplayName("Should handle invalid UUID in delete request")
        void shouldHandleInvalidUuidInDeleteRequest() throws Exception {
            // When & Then
            mockMvc.perform(delete("/api/competencies/{competencyId}/bi/{biId}", "invalid-uuid", "invalid-uuid")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(behavioralIndicatorService);
        }
    }

    @Nested
    @DisplayName("Russian Content Handling Tests")
    class RussianContentHandlingTests {

        @Test
        @DisplayName("Should handle Russian text in request and response JSON")
        void shouldHandleRussianTextInRequestAndResponseJson() throws Exception {
            // Given
            BehavioralIndicatorDto russianDto = new BehavioralIndicatorDto(
                    null,
                    competencyId,
                    "Лидерские качества",
                    "Способность эффективно руководить командой и принимать решения",
                    ProficiencyLevel.PROFICIENT,
                    IndicatorMeasurementType.QUALITY,
                    0.8f,
                    "Демонстрирует уверенность, принимает обоснованные решения",
                    "Избегает ответственности, неуверен в принятии решений",
                    true,
                    ApprovalStatus.APPROVED,
                    1
            );

            BehavioralIndicator createdEntity = new BehavioralIndicator();
            createdEntity.setId(UUID.randomUUID());
            createdEntity.setTitle("Лидерские качества");

            BehavioralIndicatorDto responseDto = new BehavioralIndicatorDto(
                    createdEntity.getId(),
                    competencyId,
                    "Лидерские качества",
                    "Способность эффективно руководить командой и принимать решения",
                    ProficiencyLevel.PROFICIENT,
                    IndicatorMeasurementType.QUALITY,
                    0.8f,
                    "Демонстрирует уверенность, принимает обоснованные решения",
                    "Избегает ответственности, неуверен в принятии решений",
                    true,
                    ApprovalStatus.APPROVED,
                    1
            );

            when(behavioralIndicatorMapper.fromDto(any(BehavioralIndicatorDto.class))).thenReturn(mockEntity);
            when(behavioralIndicatorService.createBehavioralIndicator(eq(competencyId), any(BehavioralIndicator.class)))
                    .thenReturn(createdEntity);
            when(behavioralIndicatorMapper.toDto(createdEntity)).thenReturn(responseDto);

            // When & Then
            mockMvc.perform(post("/api/competencies/{competencyId}/bi", competencyId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding("UTF-8")
                            .content(objectMapper.writeValueAsString(russianDto)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(content().encoding("UTF-8"))
                    .andExpect(jsonPath("$.title", is("Лидерские качества")))
                    .andExpect(jsonPath("$.description", is("Способность эффективно руководить командой и принимать решения")))
                    .andExpect(jsonPath("$.examples", is("Демонстрирует уверенность, принимает обоснованные решения")))
                    .andExpect(jsonPath("$.counterExamples", is("Избегает ответственности, неуверен в принятии решений")));

            verify(behavioralIndicatorService).createBehavioralIndicator(eq(competencyId), any(BehavioralIndicator.class));
        }

        @Test
        @DisplayName("Should handle mixed Russian and English content")
        void shouldHandleMixedRussianAndEnglishContent() throws Exception {
            // Given
            BehavioralIndicatorDto mixedDto = new BehavioralIndicatorDto(
                    null,
                    competencyId,
                    "Leadership - Лидерство",
                    "Ability to lead teams effectively / Способность эффективно руководить командами",
                    ProficiencyLevel.PROFICIENT,
                    IndicatorMeasurementType.QUALITY,
                    0.7f,
                    "Shows confidence / Демонстрирует уверенность",
                    "Avoids responsibility / Избегает ответственности",
                    true,
                    ApprovalStatus.APPROVED,
                    1
            );

            BehavioralIndicator createdEntity = new BehavioralIndicator();
            createdEntity.setId(UUID.randomUUID());

            when(behavioralIndicatorMapper.fromDto(any(BehavioralIndicatorDto.class))).thenReturn(mockEntity);
            when(behavioralIndicatorService.createBehavioralIndicator(eq(competencyId), any(BehavioralIndicator.class)))
                    .thenReturn(createdEntity);
            when(behavioralIndicatorMapper.toDto(createdEntity)).thenReturn(mixedDto);

            // When & Then
            mockMvc.perform(post("/api/competencies/{competencyId}/bi", competencyId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding("UTF-8")
                            .content(objectMapper.writeValueAsString(mixedDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title", is("Leadership - Лидерство")))
                    .andExpect(jsonPath("$.description", containsString("Ability to lead teams effectively")))
                    .andExpect(jsonPath("$.description", containsString("Способность эффективно руководить командами")));
        }
    }

    @Nested
    @DisplayName("Content Type and Headers Tests")
    class ContentTypeAndHeadersTests {

        @Test
        @DisplayName("Should handle missing Content-Type header")
        void shouldHandleMissingContentTypeHeader() throws Exception {
            // Given
            BehavioralIndicatorDto requestDto = new BehavioralIndicatorDto(
                    null, competencyId, "Test", "Description",
                    ProficiencyLevel.PROFICIENT, IndicatorMeasurementType.QUALITY,
                    0.5f, "Examples", "Counter-examples", true,
                    ApprovalStatus.DRAFT, 1
            );

            // When & Then
            mockMvc.perform(post("/api/competencies/{competencyId}/bi", competencyId)
                            .content(objectMapper.writeValueAsString(requestDto))) // No Content-Type set
                    .andExpect(status().isUnsupportedMediaType());

            verifyNoInteractions(behavioralIndicatorService);
        }

        @Test
        @DisplayName("Should handle unsupported media type")
        void shouldHandleUnsupportedMediaType() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/competencies/{competencyId}/bi", competencyId)
                            .contentType(MediaType.APPLICATION_XML)
                            .content("<xml>test</xml>"))
                    .andExpect(status().isUnsupportedMediaType());

            verifyNoInteractions(behavioralIndicatorService);
        }

        @Test
        @DisplayName("Should accept application/json content type")
        void shouldAcceptApplicationJsonContentType() throws Exception {
            // Given
            BehavioralIndicatorDto requestDto = new BehavioralIndicatorDto(
                    null, competencyId, "Test", "Description",
                    ProficiencyLevel.PROFICIENT, IndicatorMeasurementType.QUALITY,
                    0.5f, "Examples", "Counter-examples", true,
                    ApprovalStatus.DRAFT, 1
            );

            when(behavioralIndicatorMapper.fromDto(any(BehavioralIndicatorDto.class))).thenReturn(mockEntity);
            when(behavioralIndicatorService.createBehavioralIndicator(eq(competencyId), any(BehavioralIndicator.class)))
                    .thenReturn(mockEntity);
            when(behavioralIndicatorMapper.toDto(mockEntity)).thenReturn(requestDto);

            // When & Then
            mockMvc.perform(post("/api/competencies/{competencyId}/bi", competencyId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }
    }

    /**
     * Helper method to create test entities
     */
    private BehavioralIndicator createTestEntity(String title, int orderIndex) {
        BehavioralIndicator entity = new BehavioralIndicator();
        entity.setId(UUID.randomUUID());
        entity.setTitle(title);
        entity.setDescription("Description for " + title);
        entity.setObservabilityLevel(ProficiencyLevel.PROFICIENT);
        entity.setMeasurementType(IndicatorMeasurementType.QUALITY);
        entity.setWeight(0.5f);
        entity.setActive(true);
        entity.setApprovalStatus(ApprovalStatus.APPROVED);
        entity.setOrderIndex(orderIndex);
        return entity;
    }

    /**
     * Helper method to create test DTOs
     */
    private BehavioralIndicatorDto createTestDto(String title, int orderIndex) {
        return new BehavioralIndicatorDto(
                UUID.randomUUID(),
                competencyId,
                title,
                "Description for " + title,
                ProficiencyLevel.PROFICIENT,
                IndicatorMeasurementType.QUALITY,
                0.5f,
                "Examples for " + title,
                "Counter-examples for " + title,
                true,
                ApprovalStatus.APPROVED,
                orderIndex
        );
    }
}