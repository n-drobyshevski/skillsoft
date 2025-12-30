package app.skillsoft.assessmentbackend.util;

import app.skillsoft.assessmentbackend.config.CorrelationIdFilter;
import org.slf4j.MDC;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Utility class for managing logging context (MDC) in service methods.
 *
 * Provides convenience methods for:
 * - Setting session context for test session operations
 * - Setting user context for authenticated operations
 * - Setting competency context for competency-related operations
 * - Executing code blocks with temporary MDC context
 *
 * Usage Examples:
 *
 * 1. Execute with session context (auto-cleanup):
 *    LoggingContext.withSession(sessionId, () -> {
 *        // All log messages here include sessionId
 *        processTestSession(session);
 *    });
 *
 * 2. Execute with session context and return value:
 *    TestResult result = LoggingContext.withSession(sessionId, () -> {
 *        return calculateResults(session);
 *    });
 *
 * 3. Set context manually (remember to cleanup):
 *    try {
 *        LoggingContext.setSessionId(sessionId);
 *        LoggingContext.setUserId(userId);
 *        // ... operations
 *    } finally {
 *        LoggingContext.clearSessionId();
 *        LoggingContext.clearUserId();
 *    }
 *
 * Thread Safety:
 * - MDC is thread-local, so context is isolated per thread
 * - Use withSession/withUser wrappers to ensure cleanup
 * - For async operations, propagate MDC context manually
 *
 * @see CorrelationIdFilter for HTTP request-level context
 */
public final class LoggingContext {

    // Additional MDC keys beyond those in CorrelationIdFilter
    public static final String COMPETENCY_ID_MDC_KEY = "competencyId";
    public static final String TEMPLATE_ID_MDC_KEY = "templateId";
    public static final String OPERATION_MDC_KEY = "operation";

    private LoggingContext() {
        // Utility class - prevent instantiation
    }

    // ==================== Session Context ====================

    /**
     * Execute a runnable with session context set in MDC.
     * Automatically restores previous context after execution.
     *
     * @param sessionId The test session ID to set in MDC
     * @param action The action to execute with session context
     */
    public static void withSession(UUID sessionId, Runnable action) {
        String previous = MDC.get(CorrelationIdFilter.SESSION_ID_MDC_KEY);
        try {
            if (sessionId != null) {
                MDC.put(CorrelationIdFilter.SESSION_ID_MDC_KEY, sessionId.toString());
            }
            action.run();
        } finally {
            restoreOrRemove(CorrelationIdFilter.SESSION_ID_MDC_KEY, previous);
        }
    }

    /**
     * Execute a supplier with session context set in MDC.
     * Automatically restores previous context after execution.
     *
     * @param sessionId The test session ID to set in MDC
     * @param supplier The supplier to execute with session context
     * @param <T> The return type
     * @return The result of the supplier
     */
    public static <T> T withSession(UUID sessionId, Supplier<T> supplier) {
        String previous = MDC.get(CorrelationIdFilter.SESSION_ID_MDC_KEY);
        try {
            if (sessionId != null) {
                MDC.put(CorrelationIdFilter.SESSION_ID_MDC_KEY, sessionId.toString());
            }
            return supplier.get();
        } finally {
            restoreOrRemove(CorrelationIdFilter.SESSION_ID_MDC_KEY, previous);
        }
    }

    /**
     * Execute a callable with session context set in MDC.
     * Automatically restores previous context after execution.
     *
     * @param sessionId The test session ID to set in MDC
     * @param callable The callable to execute with session context
     * @param <T> The return type
     * @return The result of the callable
     * @throws Exception If the callable throws an exception
     */
    public static <T> T withSessionChecked(UUID sessionId, Callable<T> callable) throws Exception {
        String previous = MDC.get(CorrelationIdFilter.SESSION_ID_MDC_KEY);
        try {
            if (sessionId != null) {
                MDC.put(CorrelationIdFilter.SESSION_ID_MDC_KEY, sessionId.toString());
            }
            return callable.call();
        } finally {
            restoreOrRemove(CorrelationIdFilter.SESSION_ID_MDC_KEY, previous);
        }
    }

    /**
     * Set session ID in MDC.
     * Caller is responsible for cleanup via clearSessionId().
     *
     * @param sessionId The session ID to set
     */
    public static void setSessionId(UUID sessionId) {
        if (sessionId != null) {
            MDC.put(CorrelationIdFilter.SESSION_ID_MDC_KEY, sessionId.toString());
        }
    }

    /**
     * Clear session ID from MDC.
     */
    public static void clearSessionId() {
        MDC.remove(CorrelationIdFilter.SESSION_ID_MDC_KEY);
    }

    // ==================== User Context ====================

    /**
     * Execute a runnable with user context set in MDC.
     * Automatically restores previous context after execution.
     *
     * @param userId The user ID (e.g., Clerk ID) to set in MDC
     * @param action The action to execute with user context
     */
    public static void withUser(String userId, Runnable action) {
        String previous = MDC.get(CorrelationIdFilter.USER_ID_MDC_KEY);
        try {
            if (userId != null && !userId.isBlank()) {
                MDC.put(CorrelationIdFilter.USER_ID_MDC_KEY, userId);
            }
            action.run();
        } finally {
            restoreOrRemove(CorrelationIdFilter.USER_ID_MDC_KEY, previous);
        }
    }

    /**
     * Execute a supplier with user context set in MDC.
     * Automatically restores previous context after execution.
     *
     * @param userId The user ID to set in MDC
     * @param supplier The supplier to execute with user context
     * @param <T> The return type
     * @return The result of the supplier
     */
    public static <T> T withUser(String userId, Supplier<T> supplier) {
        String previous = MDC.get(CorrelationIdFilter.USER_ID_MDC_KEY);
        try {
            if (userId != null && !userId.isBlank()) {
                MDC.put(CorrelationIdFilter.USER_ID_MDC_KEY, userId);
            }
            return supplier.get();
        } finally {
            restoreOrRemove(CorrelationIdFilter.USER_ID_MDC_KEY, previous);
        }
    }

    /**
     * Set user ID in MDC.
     * Caller is responsible for cleanup via clearUserId().
     *
     * @param userId The user ID to set
     */
    public static void setUserId(String userId) {
        if (userId != null && !userId.isBlank()) {
            MDC.put(CorrelationIdFilter.USER_ID_MDC_KEY, userId);
        }
    }

    /**
     * Clear user ID from MDC.
     */
    public static void clearUserId() {
        MDC.remove(CorrelationIdFilter.USER_ID_MDC_KEY);
    }

    // ==================== Template Context ====================

    /**
     * Execute a runnable with template context set in MDC.
     *
     * @param templateId The test template ID
     * @param action The action to execute
     */
    public static void withTemplate(UUID templateId, Runnable action) {
        String previous = MDC.get(TEMPLATE_ID_MDC_KEY);
        try {
            if (templateId != null) {
                MDC.put(TEMPLATE_ID_MDC_KEY, templateId.toString());
            }
            action.run();
        } finally {
            restoreOrRemove(TEMPLATE_ID_MDC_KEY, previous);
        }
    }

    /**
     * Execute a supplier with template context set in MDC.
     *
     * @param templateId The test template ID
     * @param supplier The supplier to execute
     * @param <T> The return type
     * @return The result of the supplier
     */
    public static <T> T withTemplate(UUID templateId, Supplier<T> supplier) {
        String previous = MDC.get(TEMPLATE_ID_MDC_KEY);
        try {
            if (templateId != null) {
                MDC.put(TEMPLATE_ID_MDC_KEY, templateId.toString());
            }
            return supplier.get();
        } finally {
            restoreOrRemove(TEMPLATE_ID_MDC_KEY, previous);
        }
    }

    /**
     * Set template ID in MDC.
     *
     * @param templateId The template ID to set
     */
    public static void setTemplateId(UUID templateId) {
        if (templateId != null) {
            MDC.put(TEMPLATE_ID_MDC_KEY, templateId.toString());
        }
    }

    /**
     * Clear template ID from MDC.
     */
    public static void clearTemplateId() {
        MDC.remove(TEMPLATE_ID_MDC_KEY);
    }

    // ==================== Competency Context ====================

    /**
     * Execute a runnable with competency context set in MDC.
     *
     * @param competencyId The competency ID
     * @param action The action to execute
     */
    public static void withCompetency(UUID competencyId, Runnable action) {
        String previous = MDC.get(COMPETENCY_ID_MDC_KEY);
        try {
            if (competencyId != null) {
                MDC.put(COMPETENCY_ID_MDC_KEY, competencyId.toString());
            }
            action.run();
        } finally {
            restoreOrRemove(COMPETENCY_ID_MDC_KEY, previous);
        }
    }

    /**
     * Set competency ID in MDC.
     *
     * @param competencyId The competency ID to set
     */
    public static void setCompetencyId(UUID competencyId) {
        if (competencyId != null) {
            MDC.put(COMPETENCY_ID_MDC_KEY, competencyId.toString());
        }
    }

    /**
     * Clear competency ID from MDC.
     */
    public static void clearCompetencyId() {
        MDC.remove(COMPETENCY_ID_MDC_KEY);
    }

    // ==================== Operation Context ====================

    /**
     * Set a named operation in MDC for tracking logical operations.
     * Useful for tracking phases like "scoring", "assembly", "validation".
     *
     * @param operation The operation name
     */
    public static void setOperation(String operation) {
        if (operation != null && !operation.isBlank()) {
            MDC.put(OPERATION_MDC_KEY, operation);
        }
    }

    /**
     * Clear operation from MDC.
     */
    public static void clearOperation() {
        MDC.remove(OPERATION_MDC_KEY);
    }

    /**
     * Execute a runnable with operation context set in MDC.
     *
     * @param operation The operation name
     * @param action The action to execute
     */
    public static void withOperation(String operation, Runnable action) {
        String previous = MDC.get(OPERATION_MDC_KEY);
        try {
            if (operation != null && !operation.isBlank()) {
                MDC.put(OPERATION_MDC_KEY, operation);
            }
            action.run();
        } finally {
            restoreOrRemove(OPERATION_MDC_KEY, previous);
        }
    }

    /**
     * Execute a supplier with operation context set in MDC.
     *
     * @param operation The operation name
     * @param supplier The supplier to execute
     * @param <T> The return type
     * @return The result of the supplier
     */
    public static <T> T withOperation(String operation, Supplier<T> supplier) {
        String previous = MDC.get(OPERATION_MDC_KEY);
        try {
            if (operation != null && !operation.isBlank()) {
                MDC.put(OPERATION_MDC_KEY, operation);
            }
            return supplier.get();
        } finally {
            restoreOrRemove(OPERATION_MDC_KEY, previous);
        }
    }

    // ==================== Utility Methods ====================

    /**
     * Get the current correlation ID from MDC.
     *
     * @return The correlation ID, or null if not set
     */
    public static String getCorrelationId() {
        return MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
    }

    /**
     * Get the current session ID from MDC.
     *
     * @return The session ID, or null if not set
     */
    public static String getSessionId() {
        return MDC.get(CorrelationIdFilter.SESSION_ID_MDC_KEY);
    }

    /**
     * Get the current user ID from MDC.
     *
     * @return The user ID, or null if not set
     */
    public static String getUserId() {
        return MDC.get(CorrelationIdFilter.USER_ID_MDC_KEY);
    }

    /**
     * Clear all logging context.
     * Use with caution - typically CorrelationIdFilter handles this.
     */
    public static void clearAll() {
        MDC.clear();
    }

    /**
     * Restore previous value or remove key from MDC.
     *
     * @param key The MDC key
     * @param previousValue The previous value to restore, or null to remove
     */
    private static void restoreOrRemove(String key, String previousValue) {
        if (previousValue != null) {
            MDC.put(key, previousValue);
        } else {
            MDC.remove(key);
        }
    }
}
