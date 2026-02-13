package app.skillsoft.assessmentbackend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for CorrelationIdFilter.
 *
 * Validates:
 * - Correlation ID generation and propagation
 * - Session ID extraction from URL paths
 * - User ID extraction from headers
 * - MDC context management
 */
@ExtendWith(MockitoExtension.class)
class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;

    @Mock
    private FilterChain filterChain;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("Should generate correlation ID when not provided in request header")
    void shouldGenerateCorrelationIdWhenNotProvided() throws ServletException, IOException {
        // Given
        request.setRequestURI("/api/v1/test");
        request.setMethod("GET");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        String correlationId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(correlationId).isNotNull();
        assertThat(correlationId).isNotBlank();

        // Verify it's a valid UUID format
        assertThat(UUID.fromString(correlationId)).isNotNull();

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should use existing correlation ID from request header")
    void shouldUseExistingCorrelationIdFromHeader() throws ServletException, IOException {
        // Given
        String existingCorrelationId = "test-correlation-123";
        request.setRequestURI("/api/v1/test");
        request.setMethod("GET");
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, existingCorrelationId);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        String correlationId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(correlationId).isEqualTo(existingCorrelationId);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should extract session ID from API v1 path")
    void shouldExtractSessionIdFromV1Path() throws ServletException, IOException {
        // Given
        UUID sessionId = UUID.randomUUID();
        request.setRequestURI("/api/v1/sessions/" + sessionId + "/answers");
        request.setMethod("POST");

        // We need to capture the MDC during filter execution
        final String[] capturedSessionId = new String[1];

        // When - use a custom filter chain to capture MDC state
        filter.doFilterInternal(request, response, (req, res) -> {
            capturedSessionId[0] = MDC.get(CorrelationIdFilter.SESSION_ID_MDC_KEY);
        });

        // Then
        assertThat(capturedSessionId[0]).isEqualTo(sessionId.toString());
    }

    @Test
    @DisplayName("Should extract session ID from legacy API path without version")
    void shouldExtractSessionIdFromLegacyPath() throws ServletException, IOException {
        // Given
        UUID sessionId = UUID.randomUUID();
        request.setRequestURI("/api/sessions/" + sessionId + "/complete");
        request.setMethod("POST");

        final String[] capturedSessionId = new String[1];

        // When
        filter.doFilterInternal(request, response, (req, res) -> {
            capturedSessionId[0] = MDC.get(CorrelationIdFilter.SESSION_ID_MDC_KEY);
        });

        // Then
        assertThat(capturedSessionId[0]).isEqualTo(sessionId.toString());
    }

    @Test
    @DisplayName("Should not set session ID for non-session paths")
    void shouldNotSetSessionIdForNonSessionPaths() throws ServletException, IOException {
        // Given
        request.setRequestURI("/api/v1/competencies");
        request.setMethod("GET");

        final String[] capturedSessionId = new String[1];

        // When
        filter.doFilterInternal(request, response, (req, res) -> {
            capturedSessionId[0] = MDC.get(CorrelationIdFilter.SESSION_ID_MDC_KEY);
        });

        // Then
        assertThat(capturedSessionId[0]).isNull();
    }

    @Test
    @DisplayName("Should extract user ID from X-User-Id header")
    void shouldExtractUserIdFromHeader() throws ServletException, IOException {
        // Given
        String userId = "user_clerk_123abc";
        request.setRequestURI("/api/v1/test");
        request.setMethod("GET");
        request.addHeader(CorrelationIdFilter.USER_ID_HEADER, userId);

        final String[] capturedUserId = new String[1];

        // When
        filter.doFilterInternal(request, response, (req, res) -> {
            capturedUserId[0] = MDC.get(CorrelationIdFilter.USER_ID_MDC_KEY);
        });

        // Then
        assertThat(capturedUserId[0]).isEqualTo(userId);
    }

    @Test
    @DisplayName("Should clear MDC after filter execution")
    void shouldClearMdcAfterFilterExecution() throws ServletException, IOException {
        // Given
        request.setRequestURI("/api/v1/test");
        request.setMethod("GET");
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "test-123");
        request.addHeader(CorrelationIdFilter.USER_ID_HEADER, "user-123");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then - MDC should be cleared after filter completes
        assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)).isNull();
        assertThat(MDC.get(CorrelationIdFilter.USER_ID_MDC_KEY)).isNull();
        assertThat(MDC.get(CorrelationIdFilter.SESSION_ID_MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("Should set request path and method in MDC")
    void shouldSetRequestPathAndMethodInMdc() throws ServletException, IOException {
        // Given
        request.setRequestURI("/api/v1/templates");
        request.setMethod("POST");

        final String[] capturedPath = new String[1];
        final String[] capturedMethod = new String[1];

        // When
        filter.doFilterInternal(request, response, (req, res) -> {
            capturedPath[0] = MDC.get(CorrelationIdFilter.REQUEST_PATH_MDC_KEY);
            capturedMethod[0] = MDC.get(CorrelationIdFilter.REQUEST_METHOD_MDC_KEY);
        });

        // Then
        assertThat(capturedPath[0]).isEqualTo("/api/v1/templates");
        assertThat(capturedMethod[0]).isEqualTo("POST");
    }

    @Test
    @DisplayName("Should handle blank correlation ID header as missing")
    void shouldHandleBlankCorrelationIdHeader() throws ServletException, IOException {
        // Given
        request.setRequestURI("/api/v1/test");
        request.setMethod("GET");
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "   ");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then - should generate new correlation ID
        String correlationId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(correlationId).isNotBlank();
        assertThat(correlationId.trim()).isNotEqualTo("");

        // Should be a valid UUID
        assertThat(UUID.fromString(correlationId)).isNotNull();
    }

    @Test
    @DisplayName("Should handle session ID at end of path without trailing slash")
    void shouldHandleSessionIdAtEndOfPath() throws ServletException, IOException {
        // Given
        UUID sessionId = UUID.randomUUID();
        request.setRequestURI("/api/v1/sessions/" + sessionId);
        request.setMethod("GET");

        final String[] capturedSessionId = new String[1];

        // When
        filter.doFilterInternal(request, response, (req, res) -> {
            capturedSessionId[0] = MDC.get(CorrelationIdFilter.SESSION_ID_MDC_KEY);
        });

        // Then
        assertThat(capturedSessionId[0]).isEqualTo(sessionId.toString());
    }

    @Test
    @DisplayName("Should not extract invalid UUID from session path")
    void shouldNotExtractInvalidUuidFromSessionPath() throws ServletException, IOException {
        // Given - invalid UUID format
        request.setRequestURI("/api/v1/sessions/not-a-valid-uuid/answers");
        request.setMethod("POST");

        final String[] capturedSessionId = new String[1];

        // When
        filter.doFilterInternal(request, response, (req, res) -> {
            capturedSessionId[0] = MDC.get(CorrelationIdFilter.SESSION_ID_MDC_KEY);
        });

        // Then
        assertThat(capturedSessionId[0]).isNull();
    }
}
