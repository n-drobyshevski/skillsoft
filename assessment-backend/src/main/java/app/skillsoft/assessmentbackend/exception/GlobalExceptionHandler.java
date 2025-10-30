package app.skillsoft.assessmentbackend.exception;

import app.skillsoft.assessmentbackend.domain.dto.ErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.sql.SQLIntegrityConstraintViolationException;
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
     * Build enhanced error response with error code
     */
    private ErrorResponse buildErrorResponseWithCode(
            Exception ex, 
            HttpStatus status, 
            String code,
            String message, 
            String details, 
            WebRequest request) {
        
        String correlationId = getCorrelationId(request);
        String path = extractPath(request);
        
        ErrorResponse errorResponse = new ErrorResponse(status.value(), code, message, details);
        errorResponse.setPath(path);
        
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
     * Handle SQL integrity constraint violations directly
     */
    @ExceptionHandler(java.sql.SQLIntegrityConstraintViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ErrorResponse> handleSQLIntegrityConstraintViolationException(
            java.sql.SQLIntegrityConstraintViolationException ex, WebRequest request) {
        
        String correlationId = getCorrelationId(request);
        logger.warn("SQL integrity constraint violation [{}]: {}", correlationId, ex.getMessage());
        
        String message = "Data constraint violation";
        String details = "The operation violates database integrity constraints";
        String code = "SQL_CONSTRAINT_VIOLATION";
        
        String sqlMessage = ex.getMessage();
        if (sqlMessage != null) {
            if (sqlMessage.contains("Duplicate entry") && sqlMessage.contains("key 'name'")) {
                message = "A record with this name already exists.";
                details = "Names must be unique. Please choose a different name.";
                code = "DUPLICATE_NAME";
            } else if (sqlMessage.contains("Duplicate entry")) {
                message = "Duplicate data detected.";
                details = "This record already exists. Please check your data for duplicates.";
                code = "DUPLICATE_ENTRY";
            } else if (sqlMessage.contains("foreign key constraint fails")) {
                message = "Invalid reference to related data.";
                details = "The referenced entity does not exist. Please verify your selections.";
                code = "INVALID_FOREIGN_KEY";
            } else if (sqlMessage.contains("cannot be null")) {
                message = "Required field is missing.";
                details = "All mandatory fields must be provided.";
                code = "NULL_VALUE_NOT_ALLOWED";
            }
        }
        
        ErrorResponse errorResponse = buildErrorResponseWithCode(
            ex,
            HttpStatus.CONFLICT,
            code,
            message,
            details,
            request
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handle database constraint violations
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, WebRequest request) {
        
        String correlationId = getCorrelationId(request);
        logger.warn("Data integrity violation [{}]: {}", correlationId, ex.getMessage());
        
        String message = "Data integrity constraint violated";
        String details = "The operation could not be completed due to data constraints";
        String code = "DATA_INTEGRITY_VIOLATION";
        
        // --- Check for NOT-NULL violations first ---
        String rootMessage = ex.getMostSpecificCause().getMessage();
        if (rootMessage != null && rootMessage.contains("violates not-null constraint")) {
            // Attempt to parse the column name from the error message
            message = parseNotNullViolation(rootMessage);
            details = "A required field was not provided.";
            code = "REQUIRED_FIELD_MISSING";
        }
        // --- If not a not-null violation, check for unique constraints ---
        else if (ex.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
            org.hibernate.exception.ConstraintViolationException cause = (org.hibernate.exception.ConstraintViolationException) ex.getCause();
            String constraintName = cause.getConstraintName();
            logger.info("Caught ConstraintViolationException. Constraint Name: [{}], SQL State: [{}], Error Code: [{}]",
                    constraintName, cause.getSQLState(), cause.getErrorCode());
            
            if (constraintName != null) {
                if (constraintName.contains("uc_competency_name")) {
                    message = "A competency with this name already exists.";
                    details = "Competency names must be unique.";
                    code = "DUPLICATE_COMPETENCY_NAME";
                } else if (constraintName.contains("uc_behavioralindicator_competency_title")) {
                    message = "A behavioral indicator with this title already exists for this competency.";
                    details = "Behavioral indicator titles must be unique within a competency.";
                    code = "DUPLICATE_INDICATOR_TITLE";
                } else if (constraintName.contains("uc_behavioralindicator_competency_order")) {
                    message = "A behavioral indicator with this order index already exists for this competency.";
                    details = "Order index must be unique for indicators within a competency.";
                    code = "DUPLICATE_INDICATOR_ORDER";
                } else if (constraintName.contains("uc_assessmentquestion_indicator_text")) {
                    message = "This assessment question already exists for this behavioral indicator.";
                    details = "Assessment question text must be unique for an indicator.";
                    code = "DUPLICATE_QUESTION_TEXT";
                } else if (constraintName.contains("uc_assessmentquestion_indicator_order")) {
                    message = "An assessment question with this order index already exists for this behavioral indicator.";
                    details = "Order index must be unique for questions within an indicator.";
                    code = "DUPLICATE_QUESTION_ORDER";
                } else if (constraintName.contains("fk_behavioral_indicator_competency")) {
                    message = "Cannot perform operation - competency does not exist.";
                    details = "Please ensure the competency exists before creating or updating indicators.";
                    code = "INVALID_COMPETENCY_REFERENCE";
                } else if (constraintName.contains("fk_assessment_question_indicator")) {
                    message = "Cannot perform operation - behavioral indicator does not exist.";
                    details = "Please ensure the behavioral indicator exists before creating or updating questions.";
                    code = "INVALID_INDICATOR_REFERENCE";
                } else if (constraintName.contains("competencies_name_key") || constraintName.contains("idx_competency_name_unique")) {
                    message = "A competency with this name already exists.";
                    details = "Competency names must be unique across the entire system.";
                    code = "DUPLICATE_COMPETENCY_NAME";
                } else if (constraintName.contains("behavioral_indicators") && constraintName.contains("title")) {
                    message = "A behavioral indicator with this title already exists.";
                    details = "Behavioral indicator titles must be unique within their competency.";
                    code = "DUPLICATE_INDICATOR_TITLE";
                } else if (constraintName.contains("assessment_questions") && constraintName.contains("question_text")) {
                    message = "This assessment question text already exists.";
                    details = "Question text must be unique within the indicator.";
                    code = "DUPLICATE_QUESTION_TEXT";
                } else if (constraintName.contains("competency_description_min_length")) {
                    message = "Competency description is too short. (<=10 characters)";
                    details = "Description must be at least 10 characters long.";
                    code = "DESCRIPTION_TOO_SHORT";
                } else if (constraintName.contains("competency_name_min_length")) {
                    message = "Competency name is too short.";
                    details = "Name must be at least 3 characters long.";
                    code = "NAME_TOO_SHORT";
                } else if (constraintName.contains("behavioral_indicator_title_min_length")) {
                    message = "Behavioral indicator title is too short.";
                    details = "Title must be at least 5 characters long.";
                    code = "TITLE_TOO_SHORT";
                } else if (constraintName.contains("behavioral_indicator_description_min_length")) {
                    message = "Behavioral indicator description is too short.";
                    details = "Description must be at least 10 characters long.";
                    code = "DESCRIPTION_TOO_SHORT";
                } else if (constraintName.contains("assessment_question_text_min_length")) {
                    message = "Assessment question text is too short.";
                    details = "Question text must be at least 10 characters long.";
                    code = "QUESTION_TEXT_TOO_SHORT";
                } else if (constraintName.contains("_max_length")) {
                    message = "Input text exceeds maximum allowed length.";
                    details = "Please reduce the text length and try again.";
                    code = "TEXT_TOO_LONG";
                } else if (constraintName.contains("_min_length")) {
                    message = "Input text is too short.";
                    details = "Please provide more detailed text and try again.";
                    code = "TEXT_TOO_SHORT";
                } else {
                    // Generic constraint violation
                    message = "Data constraint violation - duplicate or invalid data detected.";
                    details = "Please check your data for duplicate values or invalid references.";
                    code = "CONSTRAINT_VIOLATION";
                }
            }
        }
        // --- Handle foreign key violations separately ---
        else if (rootMessage != null && (rootMessage.contains("foreign key constraint") || 
                                       rootMessage.contains("violates foreign key constraint") ||
                                       rootMessage.contains("FOREIGN KEY constraint failed"))) {
            if (rootMessage.contains("competency") || rootMessage.contains("competencies")) {
                message = "Invalid competency reference - the specified competency does not exist.";
                details = "Please ensure the competency exists before creating or updating related entities.";
                code = "INVALID_COMPETENCY_REFERENCE";
            } else if (rootMessage.contains("behavioral_indicator") || rootMessage.contains("behavioral_indicators")) {
                message = "Invalid behavioral indicator reference - the specified indicator does not exist.";
                details = "Please ensure the behavioral indicator exists before creating or updating questions.";
                code = "INVALID_INDICATOR_REFERENCE";
            } else {
                message = "Invalid reference - the related entity does not exist.";
                details = "Please check that all referenced entities exist before performing this operation.";
                code = "INVALID_FOREIGN_KEY";
            }
        }
        // --- Handle check constraint violations ---
        else if (rootMessage != null && (rootMessage.contains("check constraint") || rootMessage.contains("violates check constraint"))) {
            if (rootMessage.contains("competency_description_min_length")) {
                message = "Competency description is too short.";
                details = "Description must be at least 10 characters long.";
                code = "DESCRIPTION_TOO_SHORT";
            } else if (rootMessage.contains("competency_name_min_length")) {
                message = "Competency name is too short.";
                details = "Name must be at least 3 characters long.";
                code = "NAME_TOO_SHORT";
            } else if (rootMessage.contains("behavioral_indicator_title_min_length")) {
                message = "Behavioral indicator title is too short.";
                details = "Title must be at least 5 characters long.";
                code = "TITLE_TOO_SHORT";
            } else if (rootMessage.contains("behavioral_indicator_description_min_length")) {
                message = "Behavioral indicator description is too short.";
                details = "Description must be at least 10 characters long.";
                code = "DESCRIPTION_TOO_SHORT";
            } else if (rootMessage.contains("assessment_question_text_min_length")) {
                message = "Assessment question text is too short.";
                details = "Question text must be at least 10 characters long.";
                code = "QUESTION_TEXT_TOO_SHORT";
            } else if (rootMessage.contains("min_length")) {
                message = "Input text is too short.";
                details = "Please provide more detailed text to meet minimum length requirements.";
                code = "TEXT_TOO_SHORT";
            } else if (rootMessage.contains("max_length")) {
                message = "Input text exceeds maximum allowed length.";
                details = "Please reduce the text length and try again.";
                code = "TEXT_TOO_LONG";
            } else if (rootMessage.contains("weight")) {
                message = "Invalid weight value - must be between 0.0 and 1.0.";
                details = "Weight values must be positive decimals not exceeding 1.0.";
                code = "INVALID_WEIGHT_VALUE";
            } else if (rootMessage.contains("order_index")) {
                message = "Invalid order index - must be a positive number.";
                details = "Order index values must be non-negative integers.";
                code = "INVALID_ORDER_INDEX";
            } else {
                message = "Data validation constraint violation.";
                details = "Please check that all data values meet the required format and constraints.";
                code = "CHECK_CONSTRAINT_VIOLATION";
            }
        }
        
        ErrorResponse errorResponse = buildErrorResponseWithCode(
            ex,
            HttpStatus.CONFLICT,
            code,
            message,
            details,
            request
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Utility method to parse a user-friendly message from a not-null violation error.
     */
    private String parseNotNullViolation(String message) {
        try {
            // Example message: "ERROR: null value in column "order_index" of relation "behavioral_indicators" violates not-null constraint"
            if (message.contains("column \"")) {
                String columnName = message.split("column \"")[1].split("\"")[0];
                
                // Provide field-specific error messages
                switch (columnName.toLowerCase()) {
                    case "name":
                        return "The name field is required and cannot be empty.";
                    case "title":
                        return "The title field is required and cannot be empty.";
                    case "description":
                        return "The description field is required and cannot be empty.";
                    case "question_text":
                        return "The question text field is required and cannot be empty.";
                    case "competency_id":
                        return "A valid competency must be selected.";
                    case "behavioral_indicator_id":
                        return "A valid behavioral indicator must be selected.";
                    case "order_index":
                        return "The order index field is required - please provide a numeric value.";
                    case "weight":
                        return "The weight field is required - please provide a decimal value between 0.0 and 1.0.";
                    case "observability_level":
                        return "The observability level field is required - please select a proficiency level.";
                    case "measurement_type":
                        return "The measurement type field is required - please select a measurement type.";
                    case "question_type":
                        return "The question type field is required - please select a question type.";
                    case "difficulty_level":
                        return "The difficulty level field is required - please select a difficulty level.";
                    case "category":
                        return "The category field is required - please select a competency category.";
                    case "level":
                        return "The proficiency level field is required - please select a level.";
                    case "approval_status":
                        return "The approval status field is required - please select a status.";
                    case "is_active":
                        return "The active status field is required - please specify true or false.";
                    case "scoring_rubric":
                        return "The scoring rubric field is required and cannot be empty.";
                    case "created_at":
                        return "The creation timestamp is required but was not set automatically.";
                    case "last_modified":
                        return "The last modified timestamp is required but was not set automatically.";
                    default:
                        return String.format("The field '%s' is required and cannot be null. Please provide a value.", columnName);
                }
            }
        } catch (Exception e) {
            logger.warn("Could not parse not-null violation message: {}", message);
        }
        return "A required field is missing a value. Please ensure all mandatory fields are filled.";
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
     * Handle runtime exceptions - with special handling for entity not found cases
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex, WebRequest request) {
        
        String correlationId = getCorrelationId(request);
        
        // Check if this is a "not found" runtime exception
        String message = ex.getMessage();
        if (message != null && (message.contains("not found with id") || 
                               message.contains("does not exist") || 
                               message.contains("could not be found"))) {
            // This is an entity not found error, return 404 instead of 500
            logger.warn("Entity not found [{}]: {}", correlationId, ex.getMessage());
            
            ErrorResponse errorResponse = buildErrorResponseWithCode(
                ex,
                HttpStatus.NOT_FOUND,
                "ENTITY_NOT_FOUND",
                "The requested resource was not found",
                "Please verify the ID exists and try again",
                request
            );
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
        
        // Handle as regular runtime exception
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