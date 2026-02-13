package app.skillsoft.assessmentbackend.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import app.skillsoft.assessmentbackend.exception.GlobalExceptionHandler.ValidationError;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enhanced error response DTO for API error handling.
 * Provides comprehensive error response structure across all endpoints
 * following REST API best practices.
 *
 * Features:
 * - Standard HTTP error fields (status, message, details, timestamp, path)
 * - Optional validation error details for form validation failures
 * - Optional stack trace for development/debugging (configurable)
 * - Optional exception name for debugging purposes
 * - Correlation ID for request tracing and logging
 * - Error code for programmatic error handling
 * - Additional context data for domain-specific errors
 * - Conditional field inclusion to minimize response size
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @JsonProperty("status")
    private int status;

    @JsonProperty("message")
    private String message;

    @JsonProperty("code")
    private String code;

    @JsonProperty("details")
    private String details;

    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    @JsonProperty("path")
    private String path;

    @JsonProperty("correlationId")
    private String correlationId;

    @JsonProperty("validationErrors")
    private List<ValidationError> validationErrors;

    @JsonProperty("stackTrace")
    private String stackTrace;

    @JsonProperty("exceptionName")
    private String exceptionName;

    @JsonProperty("context")
    private Map<String, Object> context;

    /**
     * Default constructor
     */
    public ErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Constructor with basic error information
     */
    public ErrorResponse(int status, String message, String details) {
        this();
        this.status = status;
        this.message = message;
        this.details = details;
    }

    /**
     * Constructor with full error information
     */
    public ErrorResponse(int status, String message, String details, String path) {
        this(status, message, details);
        this.path = path;
    }

    /**
     * Constructor with correlation ID
     */
    public ErrorResponse(int status, String message, String details, String path, String correlationId) {
        this(status, message, details, path);
        this.correlationId = correlationId;
    }

    // Getters and Setters
    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public List<ValidationError> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(List<ValidationError> validationErrors) {
        this.validationErrors = validationErrors;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getExceptionName() {
        return exceptionName;
    }

    public void setExceptionName(String exceptionName) {
        this.exceptionName = exceptionName;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }

    /**
     * Add a single context entry.
     *
     * @param key Context key
     * @param value Context value
     */
    public void addContext(String key, Object value) {
        if (this.context == null) {
            this.context = new HashMap<>();
        }
        this.context.put(key, value);
    }

    @Override
    public String toString() {
        return "ErrorResponse{" +
                "status=" + status +
                ", message='" + message + '\'' +
                ", code='" + code + '\'' +
                ", details='" + details + '\'' +
                ", timestamp=" + timestamp +
                ", path='" + path + '\'' +
                ", correlationId='" + correlationId + '\'' +
                ", validationErrors=" + validationErrors +
                ", exceptionName='" + exceptionName + '\'' +
                ", context=" + context +
                ", hasStackTrace=" + (stackTrace != null) +
                '}';
    }
}
