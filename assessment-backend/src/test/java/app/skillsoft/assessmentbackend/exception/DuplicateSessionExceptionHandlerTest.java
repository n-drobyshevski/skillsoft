package app.skillsoft.assessmentbackend.exception;

import app.skillsoft.assessmentbackend.domain.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for DuplicateSessionException handling in GlobalExceptionHandler.
 * Verifies that the error response has correct structure and HTTP status.
 */
@DisplayName("GlobalExceptionHandler - DuplicateSessionException")
class DuplicateSessionExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private WebRequest webRequest;
    private UUID existingSessionId;
    private UUID templateId;
    private String clerkUserId;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();

        // Create mock request
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/tests/sessions");
        request.setMethod("POST");
        webRequest = new ServletWebRequest(request);

        // Test data
        existingSessionId = UUID.randomUUID();
        templateId = UUID.randomUUID();
        clerkUserId = "test_user_123";
    }

    @Test
    @DisplayName("Should return HTTP 409 Conflict for DuplicateSessionException")
    void shouldReturn409ConflictForDuplicateSessionException() {
        // Arrange
        DuplicateSessionException exception = new DuplicateSessionException(
                existingSessionId, templateId, clerkUserId
        );

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDuplicateSessionException(
                exception, webRequest
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("Should include error code DUPLICATE_SESSION in response")
    void shouldIncludeErrorCodeInResponse() {
        // Arrange
        DuplicateSessionException exception = new DuplicateSessionException(
                existingSessionId, templateId, clerkUserId
        );

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDuplicateSessionException(
                exception, webRequest
        );

        // Assert
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("DUPLICATE_SESSION");
    }

    @Test
    @DisplayName("Should include existing session ID in context")
    void shouldIncludeExistingSessionIdInContext() {
        // Arrange
        DuplicateSessionException exception = new DuplicateSessionException(
                existingSessionId, templateId, clerkUserId
        );

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDuplicateSessionException(
                exception, webRequest
        );

        // Assert
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContext()).isNotNull();
        assertThat(response.getBody().getContext().get("existingSessionId")).isEqualTo(existingSessionId);
    }

    @Test
    @DisplayName("Should include template ID in context")
    void shouldIncludeTemplateIdInContext() {
        // Arrange
        DuplicateSessionException exception = new DuplicateSessionException(
                existingSessionId, templateId, clerkUserId
        );

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDuplicateSessionException(
                exception, webRequest
        );

        // Assert
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContext()).isNotNull();
        assertThat(response.getBody().getContext().get("templateId")).isEqualTo(templateId);
    }

    @Test
    @DisplayName("Should include clear user-facing message")
    void shouldIncludeClearUserFacingMessage() {
        // Arrange
        DuplicateSessionException exception = new DuplicateSessionException(
                existingSessionId, templateId, clerkUserId
        );

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDuplicateSessionException(
                exception, webRequest
        );

        // Assert
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage())
                .isEqualTo("User already has an in-progress session for this template");
    }

    @Test
    @DisplayName("Should include actionable details")
    void shouldIncludeActionableDetails() {
        // Arrange
        DuplicateSessionException exception = new DuplicateSessionException(
                existingSessionId, templateId, clerkUserId
        );

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDuplicateSessionException(
                exception, webRequest
        );

        // Assert
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetails())
                .isEqualTo("You can either resume the existing session or abandon it to start a new one");
    }

    @Test
    @DisplayName("Should include all required fields in error response")
    void shouldIncludeAllRequiredFieldsInErrorResponse() {
        // Arrange
        DuplicateSessionException exception = new DuplicateSessionException(
                existingSessionId, templateId, clerkUserId
        );

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDuplicateSessionException(
                exception, webRequest
        );

        // Assert
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();
        assertThat(errorResponse.getStatus()).isEqualTo(409);
        assertThat(errorResponse.getCode()).isEqualTo("DUPLICATE_SESSION");
        assertThat(errorResponse.getMessage()).isNotEmpty();
        assertThat(errorResponse.getDetails()).isNotEmpty();
        assertThat(errorResponse.getTimestamp()).isNotNull();
        assertThat(errorResponse.getPath()).isNotNull();
        assertThat(errorResponse.getCorrelationId()).isNotNull();
        assertThat(errorResponse.getContext()).isNotNull();
        assertThat(errorResponse.getContext()).hasSize(2);
    }

    @Test
    @DisplayName("Should match expected JSON structure")
    void shouldMatchExpectedJsonStructure() {
        // Arrange
        DuplicateSessionException exception = new DuplicateSessionException(
                existingSessionId, templateId, clerkUserId
        );

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDuplicateSessionException(
                exception, webRequest
        );

        // Assert - Verify the response can be used by frontend for decision making
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse).isNotNull();

        // Frontend can check the error code
        boolean isDuplicateSessionError = "DUPLICATE_SESSION".equals(errorResponse.getCode());
        assertThat(isDuplicateSessionError).isTrue();

        // Frontend can extract existing session ID for resume action
        UUID sessionIdFromResponse = (UUID) errorResponse.getContext().get("existingSessionId");
        assertThat(sessionIdFromResponse).isEqualTo(existingSessionId);

        // Frontend can display user-friendly message
        assertThat(errorResponse.getMessage()).contains("in-progress session");
        assertThat(errorResponse.getDetails()).contains("resume");
        assertThat(errorResponse.getDetails()).contains("abandon");
    }
}
