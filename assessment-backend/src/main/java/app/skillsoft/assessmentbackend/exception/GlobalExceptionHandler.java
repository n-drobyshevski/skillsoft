package app.skillsoft.assessmentbackend.exception;

import app.skillsoft.assessmentbackend.domain.dto.ErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Enhanced Global Exception Handler for the SkillSoft Assessment Backend.
 * 
 * This class provides centralized exception handling across all controllers following
 * Spring Boot best practices for REST API error handling.
 * 
 * Features:
 * - Extends ResponseEntityExceptionHandler for comprehensive Spring MVC exception coverage
 * - Structured error responses using ErrorResponse DTO
 * - Configurable stack trace inclusion for development/debugging
 * - Security-aware error messages (no sensitive data exposure)
 * - Comprehensive logging with correlation IDs
 * - Validation error handling with field-specific details
 * - Database constraint violation handling
 * - Security exception handling
 * 
 * @author SkillSoft Development Team
 * @version 2.0
 * @since 1.0
 */
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    // Configuration properties
    @Value("${app.exception-handler.include-stack-trace:false}")
    private boolean includeStackTrace;
    
    @Value("${app.exception-handler.include-binding-errors:true}")
    private boolean includeBindingErrors;
    
    @Value("${app.exception-handler.include-exception-name:false}")
    private boolean includeExceptionName;
    
    private static final String TRACE_PARAM = "trace";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    // ===============================
    // UTILITY METHODS
    // ===============================

    /**
     * Extract path from WebRequest
     */
    private String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
    
    /**
     * Extract path from HttpServletRequest
     */
    private String extractPath(HttpServletRequest request) {
        return request.getRequestURI();
    }
    
    /**
     * Get or generate correlation ID for request tracing
     */
    private String getCorrelationId(WebRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = generateCorrelationId();
        }
        MDC.put("correlationId", correlationId);
        return correlationId;
    }
    
    /**
     * Generate unique correlation ID
     */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * Check if stack trace should be included in response
     */
    private boolean shouldIncludeStackTrace(WebRequest request) {
        if (!includeStackTrace) return false;
        String[] values = request.getParameterValues(TRACE_PARAM);
        return values != null && values.length > 0 && "true".equalsIgnoreCase(values[0]);
    }
    
    /**
     * Build enhanced error response with all necessary details
     */
    private ErrorResponse buildErrorResponse(
            Exception ex, 
            HttpStatus status, 
            String message, 
            String details, 
            WebRequest request) {
        
        String correlationId = getCorrelationId(request);
        String path = extractPath(request);
        
        ErrorResponse errorResponse = new ErrorResponse(status.value(), message, details, path);
        
        // Add stack trace if enabled and requested
        if (shouldIncludeStackTrace(request)) {
            errorResponse.setStackTrace(getStackTrace(ex));
        }
        
        // Add exception name if enabled (useful for debugging)
        if (includeExceptionName) {
            errorResponse.setExceptionName(ex.getClass().getSimpleName());
        }
        
        // Add correlation ID for tracing
        errorResponse.setCorrelationId(correlationId);
        
        return errorResponse;
    }
    
    /**
     * Get formatted stack trace
     */
    private String getStackTrace(Exception ex) {
        StringBuilder sb = new StringBuilder();
        sb.append(ex.getClass().getName()).append(": ").append(ex.getMessage()).append("\n");
        for (StackTraceElement element : ex.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
            // Limit stack trace length to prevent huge responses
            if (sb.length() > 2000) {
                sb.append("\t... (truncated)\n");
                break;
            }
        }
        return sb.toString();
    }

    // ===============================
    // SPRING FRAMEWORK EXCEPTIONS
    // ===============================

    /**
     * Handle validation errors from @Valid annotation
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        
        String correlationId = getCorrelationId(request);
        logger.warn("Validation failed for request [{}]: {}", correlationId, ex.getMessage());
        
        List<ValidationError> validationErrors = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            validationErrors.add(new ValidationError(
                fieldError.getField(),
                fieldError.getDefaultMessage(),
                fieldError.getRejectedValue()
            ));
        }
        
        ErrorResponse errorResponse = buildErrorResponse(
            ex, 
            HttpStatus.BAD_REQUEST,
            "Validation failed", 
            "Please check the provided data",
            request
        );
        errorResponse.setValidationErrors(validationErrors);
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle missing request parameters
     */
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        
        String correlationId = getCorrelationId(request);
        logger.warn("Missing request parameter [{}]: {}", correlationId, ex.getMessage());
        
        String message = String.format("Required parameter '%s' is missing", ex.getParameterName());
        ErrorResponse errorResponse = buildErrorResponse(
            ex, 
            HttpStatus.BAD_REQUEST, 
            message,
            "Please provide all required parameters",
            request
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle unsupported HTTP methods
     */
    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        
        String correlationId = getCorrelationId(request);
        logger.warn("HTTP method not supported [{}]: {}", correlationId, ex.getMessage());
        
        String message = String.format("HTTP method '%s' is not supported for this endpoint", ex.getMethod());
        String details = "Supported methods: " + String.join(", ", Objects.requireNonNull(ex.getSupportedMethods()));
        
        ErrorResponse errorResponse = buildErrorResponse(ex, HttpStatus.METHOD_NOT_ALLOWED, message, details, request);
        
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse);
    }

    /**
     * Handle unsupported media types
     */
    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        
        String correlationId = getCorrelationId(request);
        logger.warn("Media type not supported [{}]: {}", correlationId, ex.getMessage());
        
        String message = "Unsupported media type: " + ex.getContentType();
        String details = "Supported media types: " + ex.getSupportedMediaTypes();
        
        ErrorResponse errorResponse = buildErrorResponse(
            ex, 
            HttpStatus.UNSUPPORTED_MEDIA_TYPE, 
            message, 
            details, 
            request
        );
        
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(errorResponse);
    }

    /**
     * Handle malformed JSON requests
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        
        String correlationId = getCorrelationId(request);
        logger.warn("Message not readable [{}]: {}", correlationId, ex.getMessage());
        
        ErrorResponse errorResponse = buildErrorResponse(
            ex,
            HttpStatus.BAD_REQUEST,
            "Malformed JSON request",
            "Please check your JSON syntax and data types",
            request
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle 404 errors when Spring can't find a handler
     */
    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(
            NoResourceFoundException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        
        String correlationId = getCorrelationId(request);
        logger.warn("No resource found [{}]: {}", correlationId, ex.getMessage());
        
        ErrorResponse errorResponse = buildErrorResponse(
            ex,
            HttpStatus.NOT_FOUND,
            "Resource not found",
            "The requested endpoint does not exist",
            request
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    // ===============================
    // SECURITY EXCEPTIONS
    // ===============================

    /**
     * Handle AccessDeniedException (insufficient permissions/role).
     * Returns HTTP 403 Forbidden when user lacks required role.
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        
        String correlationId = getCorrelationId(request);
        logger.warn("Access denied [{}]: {}", correlationId, ex.getMessage());
        
        ErrorResponse errorResponse = buildErrorResponse(
            ex,
            HttpStatus.FORBIDDEN,
            "Access denied",
            "You do not have sufficient permissions to access this resource",
            request
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Handle AuthenticationException (user not authenticated).
     * Returns HTTP 401 Unauthorized when authentication fails.
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {
        
        String correlationId = getCorrelationId(request);
        logger.warn("Authentication failed [{}]: {}", correlationId, ex.getMessage());
        
        ErrorResponse errorResponse = buildErrorResponse(
            ex,
            HttpStatus.UNAUTHORIZED,
            "Authentication required",
            "You must be authenticated to access this resource",
            request
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    // ===============================
    // DOMAIN-SPECIFIC EXCEPTIONS
    // ===============================

    /**
     * Handle EntityNotFoundException (JPA/Database entity not found)
     */
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(
            EntityNotFoundException ex, WebRequest request) {
        
        String correlationId = getCorrelationId(request);
        logger.warn("Entity not found [{}]: {}", correlationId, ex.getMessage());
        
        ErrorResponse errorResponse = buildErrorResponse(
            ex,
            HttpStatus.NOT_FOUND,
            ex.getMessage(),
            "The requested resource was not found",
            request
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handle method argument type mismatches (e.g., invalid UUID format)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        
        String correlationId = getCorrelationId(request);
        logger.warn("Method argument type mismatch [{}]: {}", correlationId, ex.getMessage());
        
        String message = String.format("Invalid value '%s' for parameter '%s'", 
                ex.getValue(), ex.getName());
        
        String details = String.format("Expected type: %s", 
            Objects.requireNonNull(ex.getRequiredType()).getSimpleName());
        
        ErrorResponse errorResponse = buildErrorResponse(
            ex,
            HttpStatus.BAD_REQUEST,
            message,
            details,
            request
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle constraint violations (Bean Validation)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {
        
        String correlationId = getCorrelationId(request);
        logger.warn("Constraint violation [{}]: {}", correlationId, ex.getMessage());
        
        List<ValidationError> validationErrors = new ArrayList<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            validationErrors.add(new ValidationError(
                violation.getPropertyPath().toString(),
                violation.getMessage(),
                violation.getInvalidValue()
            ));
        }
        
        ErrorResponse errorResponse = buildErrorResponse(
            ex,
            HttpStatus.BAD_REQUEST,
            "Validation constraints violated",
            "Please check the provided data",
            request
        );
        errorResponse.setValidationErrors(validationErrors);
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle illegal arguments
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        
        String correlationId = getCorrelationId(request);
        logger.warn("Invalid argument [{}]: {}", correlationId, ex.getMessage());
        
        ErrorResponse errorResponse = buildErrorResponse(
            ex,
            HttpStatus.BAD_REQUEST,
            ex.getMessage(),
            "Please check your request parameters and data",
            request
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    // ===============================
    // DATABASE & SECURITY EXCEPTIONS
    // ===============================

    /**
     * Handle database constraint violations
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, WebRequest request) {
        
        String correlationId = getCorrelationId(request);
        logger.warn("Data integrity violation [{}]: {}", correlationId, ex.getMessage());
        
        // Don't expose internal database details to client
        String message = "Data integrity constraint violated";
        String details = "The operation could not be completed due to data constraints";
        
        ErrorResponse errorResponse = buildErrorResponse(
            ex,
            HttpStatus.CONFLICT,
            message,
            details,
            request
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handle general database access exceptions
     */
    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleDataAccessException(
            DataAccessException ex, WebRequest request) {
        
        String correlationId = getCorrelationId(request);
        logger.error("Database access error [{}]: {}", correlationId, ex.getMessage(), ex);
        
        // Don't expose internal database details to client
        ErrorResponse errorResponse = buildErrorResponse(
            ex,
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Database operation failed",
            "Please try again later or contact support if the problem persists",
            request
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    // ===============================
    // CATCH-ALL EXCEPTION HANDLERS
    // ===============================

    /**
     * Handle runtime exceptions
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex, WebRequest request) {
        
        String correlationId = getCorrelationId(request);
        logger.error("Runtime exception [{}]: {}", correlationId, ex.getMessage(), ex);
        
        ErrorResponse errorResponse = buildErrorResponse(
            ex,
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An internal error occurred",
            "Please try again later or contact support if the problem persists",
            request
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handle all other unexpected exceptions
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleGeneralException(
            Exception ex, WebRequest request) {
        
        String correlationId = getCorrelationId(request);
        logger.error("Unexpected exception [{}]: {}", correlationId, ex.getMessage(), ex);
        
        ErrorResponse errorResponse = buildErrorResponse(
            ex,
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred",
            "Please contact support if the problem persists",
            request
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    // ===============================
    // VALIDATION ERROR CLASS
    // ===============================

    /**
     * Inner class for validation error details
     */
    public static class ValidationError {
        private final String field;
        private final String message;
        private final Object rejectedValue;

        public ValidationError(String field, String message, Object rejectedValue) {
            this.field = field;
            this.message = message;
            this.rejectedValue = rejectedValue;
        }

        public String getField() { return field; }
        public String getMessage() { return message; }
        public Object getRejectedValue() { return rejectedValue; }
    }
}