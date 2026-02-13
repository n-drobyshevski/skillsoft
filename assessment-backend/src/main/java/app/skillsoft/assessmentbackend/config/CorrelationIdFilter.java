package app.skillsoft.assessmentbackend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTTP Filter that establishes correlation context for request tracing.
 *
 * This filter:
 * 1. Extracts or generates a correlation ID for each request
 * 2. Extracts session ID from URL path when present
 * 3. Propagates IDs via response headers for distributed tracing
 * 4. Sets MDC context for structured logging
 *
 * MDC Keys Set:
 * - correlationId: Unique ID for the request (from header or generated)
 * - sessionId: Test session ID extracted from URL path (if present)
 * - userId: User ID from X-User-Id header (if present)
 * - requestPath: The request URI
 * - requestMethod: HTTP method
 *
 * Usage:
 * - Incoming requests can provide X-Correlation-ID header for tracing
 * - If not provided, a new UUID is generated
 * - Response includes the correlation ID for client-side correlation
 * - All log messages include correlation context via MDC
 *
 * @see LoggingContext for programmatic MDC manipulation in services
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1) // After CORS filter but before security
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    // Header names
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String USER_ID_HEADER = "X-User-Id";

    // MDC key names
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";
    public static final String SESSION_ID_MDC_KEY = "sessionId";
    public static final String USER_ID_MDC_KEY = "userId";
    public static final String REQUEST_PATH_MDC_KEY = "requestPath";
    public static final String REQUEST_METHOD_MDC_KEY = "requestMethod";

    /**
     * Pattern to extract session UUID from API paths.
     * Matches patterns like:
     * - /api/v1/sessions/{uuid}/...
     * - /api/sessions/{uuid}/...
     */
    private static final Pattern SESSION_ID_PATTERN = Pattern.compile(
        "/api(?:/v\\d+)?/sessions/([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})(?:/|$)"
    );

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            // Extract or generate correlation ID
            String correlationId = extractOrGenerateCorrelationId(request);

            // Set correlation ID in MDC and response header
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
            response.setHeader(CORRELATION_ID_HEADER, correlationId);

            // Extract session ID from path if present
            String sessionId = extractSessionIdFromPath(request.getRequestURI());
            if (sessionId != null) {
                MDC.put(SESSION_ID_MDC_KEY, sessionId);
            }

            // Extract user ID from header if present
            String userId = request.getHeader(USER_ID_HEADER);
            if (userId != null && !userId.isBlank()) {
                MDC.put(USER_ID_MDC_KEY, userId);
            }

            // Set request context
            MDC.put(REQUEST_PATH_MDC_KEY, request.getRequestURI());
            MDC.put(REQUEST_METHOD_MDC_KEY, request.getMethod());

            // Log request start with correlation context
            if (log.isDebugEnabled()) {
                log.debug("Request started: {} {} [correlationId={}, sessionId={}, userId={}]",
                    request.getMethod(),
                    request.getRequestURI(),
                    correlationId,
                    sessionId != null ? sessionId : "N/A",
                    userId != null ? userId : "N/A"
                );
            }

            filterChain.doFilter(request, response);

        } finally {
            // Always clear MDC to prevent thread pollution
            MDC.clear();
        }
    }

    /**
     * Extract correlation ID from request header or generate a new one.
     *
     * @param request The HTTP request
     * @return Correlation ID (existing or newly generated)
     */
    private String extractOrGenerateCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
            log.trace("Generated new correlation ID: {}", correlationId);
        } else {
            log.trace("Using existing correlation ID from header: {}", correlationId);
        }

        return correlationId;
    }

    /**
     * Extract session UUID from the request URI.
     *
     * Parses paths like /api/v1/sessions/{uuid}/... to extract the session ID
     * for inclusion in log context.
     *
     * @param uri The request URI
     * @return Session UUID if found, null otherwise
     */
    private String extractSessionIdFromPath(String uri) {
        if (uri == null) {
            return null;
        }

        Matcher matcher = SESSION_ID_PATTERN.matcher(uri);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }
}
