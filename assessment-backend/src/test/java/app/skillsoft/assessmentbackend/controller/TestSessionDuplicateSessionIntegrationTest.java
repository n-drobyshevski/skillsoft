package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.dto.StartTestSessionRequest;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.repository.TestSessionRepository;
import app.skillsoft.assessmentbackend.repository.TestTemplateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for duplicate session error handling.
 * Tests the complete flow from controller through service to GlobalExceptionHandler.
 *
 * DISABLED: These tests require PostgreSQL due to H2's incompatibility with Hibernate's
 * JSON deserialization for List<UUID> fields. H2 stores JSON as a single string, but
 * Hibernate's Jackson-based JSON mapper expects a JSON array token. The duplicate session
 * detection logic is tested via unit tests in TestSessionServiceTest.
 *
 * To run these tests, use PostgreSQL via TestContainers or a local PostgreSQL instance.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = {"ADMIN"})
@DisplayName("TestSessionController - Duplicate Session Integration Test")
@Disabled("Requires PostgreSQL - H2 cannot deserialize List<UUID> from JSONB columns. See TestSessionServiceTest for unit test coverage.")
class TestSessionDuplicateSessionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestTemplateRepository templateRepository;

    @Autowired
    private TestSessionRepository sessionRepository;

    private TestTemplate template;
    private String clerkUserId;
    private TestSession existingSession;

    @BeforeEach
    void setUp() {
        clerkUserId = "integration_test_user_" + UUID.randomUUID();

        // Create and save a test template
        template = new TestTemplate();
        template.setName("Integration Test Template");
        template.setGoal(AssessmentGoal.OVERVIEW);
        template.setIsActive(true);
        template.setCompetencyIds(List.of(UUID.randomUUID()));
        template.setQuestionsPerIndicator(2);
        template.setShuffleQuestions(false);
        template.setTimeLimitMinutes(30);
        template.setPassingScore(70.0);
        template = templateRepository.save(template);

        // Create an existing in-progress session
        existingSession = new TestSession(template, clerkUserId);
        existingSession.setQuestionOrder(List.of(UUID.randomUUID(), UUID.randomUUID()));
        existingSession.start();
        existingSession = sessionRepository.save(existingSession);
    }

    @AfterEach
    void tearDown() {
        // Clean up test data
        if (existingSession != null) {
            sessionRepository.deleteById(existingSession.getId());
        }
        if (template != null) {
            templateRepository.deleteById(template.getId());
        }
    }

    @Test
    @DisplayName("Should return HTTP 409 Conflict when user has in-progress session")
    void shouldReturn409ConflictWhenUserHasInProgressSession() throws Exception {
        // Arrange
        StartTestSessionRequest request = new StartTestSessionRequest(template.getId(), clerkUserId);

        // Act & Assert
        MvcResult result = mockMvc.perform(post("/api/v1/tests/sessions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("DUPLICATE_SESSION"))
                .andExpect(jsonPath("$.message").value("User already has an in-progress session for this template"))
                .andExpect(jsonPath("$.details").value("You can either resume the existing session or abandon it to start a new one"))
                .andExpect(jsonPath("$.context.existingSessionId").value(existingSession.getId().toString()))
                .andExpect(jsonPath("$.context.templateId").value(template.getId().toString()))
                .andExpect(jsonPath("$.correlationId").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").exists())
                .andReturn();

        // Verify response structure
        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).contains("DUPLICATE_SESSION");
        assertThat(responseBody).contains(existingSession.getId().toString());
    }

    @Test
    @DisplayName("Should create new session after abandoning existing session")
    void shouldCreateNewSessionAfterAbandoningExistingSession() throws Exception {
        // Arrange - First abandon the existing session
        mockMvc.perform(post("/api/v1/tests/sessions/{sessionId}/abandon", existingSession.getId())
                        .with(csrf()))
                .andExpect(status().isOk());

        StartTestSessionRequest request = new StartTestSessionRequest(template.getId(), clerkUserId);

        // Act & Assert - Now should be able to start new session
        mockMvc.perform(post("/api/v1/tests/sessions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.clerkUserId").value(clerkUserId))
                .andExpect(jsonPath("$.templateId").value(template.getId().toString()))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("Should allow starting session for different template")
    void shouldAllowStartingSessionForDifferentTemplate() throws Exception {
        // Arrange - Create a different template
        TestTemplate differentTemplate = new TestTemplate();
        differentTemplate.setName("Different Test Template");
        differentTemplate.setGoal(AssessmentGoal.JOB_FIT);
        differentTemplate.setIsActive(true);
        differentTemplate.setCompetencyIds(List.of(UUID.randomUUID()));
        differentTemplate.setQuestionsPerIndicator(3);
        differentTemplate.setShuffleQuestions(true);
        differentTemplate = templateRepository.save(differentTemplate);

        StartTestSessionRequest request = new StartTestSessionRequest(differentTemplate.getId(), clerkUserId);

        try {
            // Act & Assert - Should succeed for different template
            mockMvc.perform(post("/api/v1/tests/sessions")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.clerkUserId").value(clerkUserId))
                    .andExpect(jsonPath("$.templateId").value(differentTemplate.getId().toString()));
        } finally {
            // Clean up
            templateRepository.deleteById(differentTemplate.getId());
        }
    }

    @Test
    @DisplayName("Should allow different user to start session for same template")
    void shouldAllowDifferentUserToStartSessionForSameTemplate() throws Exception {
        // Arrange - Different user
        String differentUserId = "different_user_" + UUID.randomUUID();
        StartTestSessionRequest request = new StartTestSessionRequest(template.getId(), differentUserId);

        // Act & Assert - Should succeed for different user
        MvcResult result = mockMvc.perform(post("/api/v1/tests/sessions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clerkUserId").value(differentUserId))
                .andExpect(jsonPath("$.templateId").value(template.getId().toString()))
                .andReturn();

        // Clean up the created session
        String responseBody = result.getResponse().getContentAsString();
        String newSessionId = objectMapper.readTree(responseBody).get("id").asText();
        sessionRepository.deleteById(UUID.fromString(newSessionId));
    }

    @Test
    @DisplayName("Error response should have proper JSON structure")
    void errorResponseShouldHaveProperJsonStructure() throws Exception {
        // Arrange
        StartTestSessionRequest request = new StartTestSessionRequest(template.getId(), clerkUserId);

        // Act
        MvcResult result = mockMvc.perform(post("/api/v1/tests/sessions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andReturn();

        // Assert - Parse and verify full JSON structure
        String responseBody = result.getResponse().getContentAsString();
        var jsonNode = objectMapper.readTree(responseBody);

        // Required fields
        assertThat(jsonNode.has("status")).isTrue();
        assertThat(jsonNode.has("message")).isTrue();
        assertThat(jsonNode.has("code")).isTrue();
        assertThat(jsonNode.has("details")).isTrue();
        assertThat(jsonNode.has("timestamp")).isTrue();
        assertThat(jsonNode.has("path")).isTrue();
        assertThat(jsonNode.has("correlationId")).isTrue();
        assertThat(jsonNode.has("context")).isTrue();

        // Context structure
        assertThat(jsonNode.get("context").has("existingSessionId")).isTrue();
        assertThat(jsonNode.get("context").has("templateId")).isTrue();

        // Values
        assertThat(jsonNode.get("status").asInt()).isEqualTo(409);
        assertThat(jsonNode.get("code").asText()).isEqualTo("DUPLICATE_SESSION");
        assertThat(jsonNode.get("context").get("existingSessionId").asText())
                .isEqualTo(existingSession.getId().toString());
        assertThat(jsonNode.get("context").get("templateId").asText())
                .isEqualTo(template.getId().toString());
    }
}
