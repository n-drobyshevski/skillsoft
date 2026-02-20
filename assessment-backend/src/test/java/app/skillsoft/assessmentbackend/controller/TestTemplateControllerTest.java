package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.dto.*;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;
import app.skillsoft.assessmentbackend.domain.entities.DeletionMode;
import app.skillsoft.assessmentbackend.exception.ResourceNotFoundException;
import app.skillsoft.assessmentbackend.services.TemplateDeletionService;
import app.skillsoft.assessmentbackend.services.TestTemplateService;
import app.skillsoft.assessmentbackend.services.TestTemplateService.TemplateStatistics;
import app.skillsoft.assessmentbackend.services.security.TemplateSecurityService;
import app.skillsoft.assessmentbackend.services.sharing.TemplateShareService;
import app.skillsoft.assessmentbackend.services.sharing.TemplateVisibilityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for TestTemplateController using @WebMvcTest.
 * 
 * Tests cover:
 * - CRUD operations for test templates
 * - Template activation/deactivation
 * - Search and filtering by competency
 * - Statistics retrieval
 * - Error handling scenarios
 */
@WebMvcTest(TestTemplateController.class)
@DisplayName("TestTemplate Controller Tests")
class TestTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TestTemplateService testTemplateService;

    @MockBean
    private TemplateVisibilityService visibilityService;

    @MockBean
    private TemplateSecurityService securityService;

    @MockBean
    private TemplateShareService templateShareService;

    @MockBean
    private TemplateDeletionService deletionService;

    private UUID templateId;
    private UUID competencyId;
    private TestTemplateDto testTemplateDto;
    private TestTemplateSummaryDto testTemplateSummaryDto;
    private CreateTestTemplateRequest createRequest;
    private UpdateTestTemplateRequest updateRequest;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        templateId = UUID.randomUUID();
        competencyId = UUID.randomUUID();
        now = LocalDateTime.now();

        // TestTemplateDto: id, name, description, goal, blueprint, competencyIds, questionsPerIndicator, timeLimitMinutes,
        //                  passingScore, isActive, shuffleQuestions, shuffleOptions, allowSkip,
        //                  allowBackNavigation, showResultsImmediately, createdAt, updatedAt, hasValidBlueprint
        testTemplateDto = new TestTemplateDto(
                templateId,
                "Leadership Assessment Test",
                "Comprehensive test for leadership competencies",
                AssessmentGoal.OVERVIEW,  // goal
                Map.of("strategy", "balanced"),  // blueprint
                List.of(competencyId),
                3,           // questionsPerIndicator
                60,          // timeLimitMinutes
                70.0,        // passingScore
                true,        // isActive
                true,        // shuffleQuestions
                true,        // shuffleOptions
                true,        // allowSkip
                true,        // allowBackNavigation
                true,        // showResultsImmediately
                now,         // createdAt
                now,         // updatedAt
                true         // hasValidBlueprint
        );

        // TestTemplateSummaryDto: id, name, description, goal, competencyCount, timeLimitMinutes, passingScore, isActive, createdAt
        testTemplateSummaryDto = new TestTemplateSummaryDto(
                templateId,
                "Leadership Assessment Test",
                "Comprehensive test for leadership competencies",
                AssessmentGoal.OVERVIEW,  // goal
                3,           // competencyCount
                60,          // timeLimitMinutes
                70.0,        // passingScore
                true,        // isActive
                now          // createdAt
        );

        // CreateTestTemplateRequest: name, description, goal, blueprint, competencyIds, questionsPerIndicator, timeLimitMinutes,
        //                            passingScore, shuffleQuestions, shuffleOptions, allowSkip, allowBackNavigation, showResultsImmediately
        createRequest = new CreateTestTemplateRequest(
                "New Template",
                "Description",
                AssessmentGoal.OVERVIEW,  // goal
                Map.of("strategy", "balanced"),  // blueprint
                List.of(competencyId),
                3,           // questionsPerIndicator
                60,          // timeLimitMinutes
                70.0,        // passingScore
                true,        // shuffleQuestions
                true,        // shuffleOptions
                true,        // allowSkip
                true,        // allowBackNavigation
                true         // showResultsImmediately
        );

        // UpdateTestTemplateRequest: name, description, goal, blueprint, competencyIds, questionsPerIndicator, timeLimitMinutes,
        //                            passingScore, isActive, shuffleQuestions, shuffleOptions, allowSkip, allowBackNavigation, showResultsImmediately
        updateRequest = new UpdateTestTemplateRequest(
                "Updated Template",
                "Updated Description",
                AssessmentGoal.JOB_FIT,   // goal
                Map.of("onetSocCode", "15-1252.00"),  // blueprint
                List.of(competencyId),
                5,           // questionsPerIndicator
                90,          // timeLimitMinutes
                80.0,        // passingScore
                true,        // isActive
                false,       // shuffleQuestions
                false,       // shuffleOptions
                true,        // allowSkip
                true,        // allowBackNavigation
                true         // showResultsImmediately
        );
    }

    @Nested
    @DisplayName("GET /api/v1/tests/templates Tests")
    class GetAllTemplatesTests {

        @Test
        @WithMockUser
        @DisplayName("Should return paginated list of templates")
        void shouldReturnPaginatedTemplates() throws Exception {
            // Given
            Page<TestTemplateSummaryDto> templatePage = new PageImpl<>(
                    List.of(testTemplateSummaryDto),
                    Pageable.unpaged(),
                    1
            );
            when(testTemplateService.listTemplates(any(Pageable.class))).thenReturn(templatePage);

            // When & Then
            mockMvc.perform(get("/api/v1/tests/templates")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].id").value(templateId.toString()))
                    .andExpect(jsonPath("$.content[0].name").value("Leadership Assessment Test"));

            verify(testTemplateService).listTemplates(any(Pageable.class));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return empty page when no templates exist")
        void shouldReturnEmptyPageWhenNoTemplates() throws Exception {
            // Given
            Page<TestTemplateSummaryDto> emptyPage = new PageImpl<>(
                    Collections.emptyList(),
                    Pageable.unpaged(),
                    0
            );
            when(testTemplateService.listTemplates(any(Pageable.class))).thenReturn(emptyPage);

            // When & Then
            mockMvc.perform(get("/api/v1/tests/templates"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/tests/templates/active Tests")
    class GetActiveTemplatesTests {

        @Test
        @WithMockUser
        @DisplayName("Should return list of active templates")
        void shouldReturnActiveTemplates() throws Exception {
            // Given
            when(testTemplateService.listActiveTemplates()).thenReturn(List.of(testTemplateSummaryDto));

            // When & Then
            mockMvc.perform(get("/api/v1/tests/templates/active"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].isActive").value(true));

            verify(testTemplateService).listActiveTemplates();
        }
    }

    @Nested
    @DisplayName("GET /api/v1/tests/templates/{id} Tests")
    class GetTemplateByIdTests {

        @Test
        @WithMockUser
        @DisplayName("Should return template by id")
        void shouldReturnTemplateById() throws Exception {
            // Given
            when(testTemplateService.findById(templateId)).thenReturn(Optional.of(testTemplateDto));

            // When & Then
            mockMvc.perform(get("/api/v1/tests/templates/{id}", templateId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(templateId.toString()))
                    .andExpect(jsonPath("$.name").value("Leadership Assessment Test"))
                    .andExpect(jsonPath("$.timeLimitMinutes").value(60))
                    .andExpect(jsonPath("$.passingScore").value(70.0));

            verify(testTemplateService).findById(templateId);
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 when template not found")
        void shouldReturn404WhenTemplateNotFound() throws Exception {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(testTemplateService.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(get("/api/v1/tests/templates/{id}", nonExistentId))
                    .andExpect(status().isNotFound());

            verify(testTemplateService).findById(nonExistentId);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/tests/templates/search Tests")
    class SearchTemplatesTests {

        @Test
        @WithMockUser
        @DisplayName("Should search templates by name")
        void shouldSearchTemplatesByName() throws Exception {
            // Given
            when(testTemplateService.searchByName("Leadership"))
                    .thenReturn(List.of(testTemplateSummaryDto));

            // When & Then
            mockMvc.perform(get("/api/v1/tests/templates/search")
                            .param("name", "Leadership"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].name", containsString("Leadership")));

            verify(testTemplateService).searchByName("Leadership");
        }
    }

    @Nested
    @DisplayName("GET /api/v1/tests/templates/by-competency/{competencyId} Tests")
    class GetTemplatesByCompetencyTests {

        @Test
        @WithMockUser
        @DisplayName("Should return templates by competency id")
        void shouldReturnTemplatesByCompetency() throws Exception {
            // Given
            when(testTemplateService.findByCompetency(competencyId))
                    .thenReturn(List.of(testTemplateSummaryDto));

            // When & Then
            mockMvc.perform(get("/api/v1/tests/templates/by-competency/{competencyId}", competencyId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].id").value(templateId.toString()));

            verify(testTemplateService).findByCompetency(competencyId);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/tests/templates Tests")
    class CreateTemplateTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should create new template")
        void shouldCreateNewTemplate() throws Exception {
            // Given
            when(testTemplateService.createTemplate(any(CreateTestTemplateRequest.class)))
                    .thenReturn(testTemplateDto);

            // When & Then
            mockMvc.perform(post("/api/v1/tests/templates")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Leadership Assessment Test"))
                    .andExpect(jsonPath("$.isActive").value(true));

            verify(testTemplateService).createTemplate(any(CreateTestTemplateRequest.class));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return 400 for invalid request - missing name")
        void shouldReturn400ForMissingName() throws Exception {
            // Given - invalid request without name
            CreateTestTemplateRequest invalidRequest = new CreateTestTemplateRequest(
                    null, // missing name
                    "Description",
                    AssessmentGoal.OVERVIEW,  // goal
                    null,  // blueprint
                    List.of(competencyId),
                    3, 60, 70.0, true, true, true, true, true
            );

            // When & Then
            mockMvc.perform(post("/api/v1/tests/templates")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 401 without authentication")
        void shouldReturn401WithoutAuthentication() throws Exception {
            // When & Then - no @WithMockUser annotation means no authentication
            mockMvc.perform(post("/api/v1/tests/templates")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isUnauthorized());

            verify(testTemplateService, never()).createTemplate(any());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/tests/templates/{id} Tests")
    class UpdateTemplateTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should update existing template")
        void shouldUpdateExistingTemplate() throws Exception {
            // Given
            TestTemplateDto updatedDto = new TestTemplateDto(
                    templateId,
                    "Updated Template",
                    "Updated Description",
                    AssessmentGoal.JOB_FIT,  // goal
                    Map.of("onetSocCode", "15-1252.00"),  // blueprint
                    List.of(competencyId),
                    5, 90, 80.0, true, false, false, true, true, true,
                    now, LocalDateTime.now(),
                    true  // hasValidBlueprint
            );
            when(testTemplateService.updateTemplate(eq(templateId), any(UpdateTestTemplateRequest.class)))
                    .thenReturn(updatedDto);

            // When & Then
            mockMvc.perform(put("/api/v1/tests/templates/{id}", templateId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated Template"))
                    .andExpect(jsonPath("$.timeLimitMinutes").value(90));

            verify(testTemplateService).updateTemplate(eq(templateId), any(UpdateTestTemplateRequest.class));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return 404 when updating non-existent template")
        void shouldReturn404WhenUpdatingNonExistent() throws Exception {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(testTemplateService.updateTemplate(eq(nonExistentId), any(UpdateTestTemplateRequest.class)))
                    .thenThrow(new ResourceNotFoundException("TestTemplate", nonExistentId));

            // When & Then
            mockMvc.perform(put("/api/v1/tests/templates/{id}", nonExistentId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/tests/templates/{id}/activate Tests")
    class ActivateTemplateTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should activate template")
        void shouldActivateTemplate() throws Exception {
            // Given
            when(testTemplateService.activateTemplate(templateId)).thenReturn(testTemplateDto);

            // When & Then
            mockMvc.perform(post("/api/v1/tests/templates/{id}/activate", templateId)
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isActive").value(true));

            verify(testTemplateService).activateTemplate(templateId);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return 404 when activating non-existent template")
        void shouldReturn404WhenActivatingNonExistent() throws Exception {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(testTemplateService.activateTemplate(nonExistentId))
                    .thenThrow(new ResourceNotFoundException("TestTemplate", nonExistentId));

            // When & Then
            mockMvc.perform(post("/api/v1/tests/templates/{id}/activate", nonExistentId)
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/tests/templates/{id}/deactivate Tests")
    class DeactivateTemplateTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should deactivate template")
        void shouldDeactivateTemplate() throws Exception {
            // Given
            TestTemplateDto deactivatedDto = new TestTemplateDto(
                    templateId,
                    testTemplateDto.name(),
                    testTemplateDto.description(),
                    testTemplateDto.goal(),
                    testTemplateDto.blueprint(),
                    testTemplateDto.competencyIds(),
                    testTemplateDto.questionsPerIndicator(),
                    testTemplateDto.timeLimitMinutes(),
                    testTemplateDto.passingScore(),
                    false, // deactivated
                    testTemplateDto.shuffleQuestions(),
                    testTemplateDto.shuffleOptions(),
                    testTemplateDto.allowSkip(),
                    testTemplateDto.allowBackNavigation(),
                    testTemplateDto.showResultsImmediately(),
                    testTemplateDto.createdAt(),
                    LocalDateTime.now(),
                    true  // hasValidBlueprint
            );
            when(testTemplateService.deactivateTemplate(templateId)).thenReturn(deactivatedDto);

            // When & Then
            mockMvc.perform(post("/api/v1/tests/templates/{id}/deactivate", templateId)
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isActive").value(false));

            verify(testTemplateService).deactivateTemplate(templateId);
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/tests/templates/{id} Tests")
    class DeleteTemplateTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should delete template using soft delete")
        void shouldDeleteTemplate() throws Exception {
            // Given - mock successful soft delete
            DeletionResultDto successResult = DeletionResultDto.softDeleted(templateId, LocalDateTime.now());
            when(deletionService.deleteTemplate(eq(templateId), eq(DeletionMode.SOFT_DELETE), eq(true)))
                    .thenReturn(successResult);

            // When & Then
            mockMvc.perform(delete("/api/v1/tests/templates/{id}", templateId)
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            verify(deletionService).deleteTemplate(templateId, DeletionMode.SOFT_DELETE, true);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return 404 when deleting non-existent template")
        void shouldReturn404WhenDeletingNonExistent() throws Exception {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(deletionService.deleteTemplate(eq(nonExistentId), eq(DeletionMode.SOFT_DELETE), eq(true)))
                    .thenThrow(new ResourceNotFoundException("TestTemplate", nonExistentId));

            // When & Then
            mockMvc.perform(delete("/api/v1/tests/templates/{id}", nonExistentId)
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/tests/templates/statistics Tests")
    class GetStatisticsTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return template statistics")
        void shouldReturnTemplateStatistics() throws Exception {
            // Given
            TemplateStatistics stats = new TemplateStatistics(10L, 7L, 3L);
            when(testTemplateService.getStatistics()).thenReturn(stats);

            // When & Then
            mockMvc.perform(get("/api/v1/tests/templates/statistics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalTemplates").value(10))
                    .andExpect(jsonPath("$.activeTemplates").value(7))
                    .andExpect(jsonPath("$.inactiveTemplates").value(3));

            verify(testTemplateService).getStatistics();
        }
    }
}
