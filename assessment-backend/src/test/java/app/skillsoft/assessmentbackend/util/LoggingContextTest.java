package app.skillsoft.assessmentbackend.util;

import app.skillsoft.assessmentbackend.config.CorrelationIdFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for LoggingContext utility class.
 *
 * Validates:
 * - MDC context setting and clearing
 * - Context restoration after scoped execution
 * - Thread-safety of MDC operations
 */
class LoggingContextTest {

    @BeforeEach
    void setUp() {
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    // ==================== Session Context Tests ====================

    @Test
    @DisplayName("withSession should set and clear session ID for Runnable")
    void withSessionShouldSetAndClearSessionIdForRunnable() {
        // Given
        UUID sessionId = UUID.randomUUID();
        AtomicReference<String> capturedSessionId = new AtomicReference<>();

        // When
        LoggingContext.withSession(sessionId, () -> {
            capturedSessionId.set(MDC.get(CorrelationIdFilter.SESSION_ID_MDC_KEY));
        });

        // Then
        assertThat(capturedSessionId.get()).isEqualTo(sessionId.toString());
        assertThat(MDC.get(CorrelationIdFilter.SESSION_ID_MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("withSession should set and clear session ID for Supplier")
    void withSessionShouldSetAndClearSessionIdForSupplier() {
        // Given
        UUID sessionId = UUID.randomUUID();

        // When
        String result = LoggingContext.withSession(sessionId, () -> {
            return MDC.get(CorrelationIdFilter.SESSION_ID_MDC_KEY);
        });

        // Then
        assertThat(result).isEqualTo(sessionId.toString());
        assertThat(MDC.get(CorrelationIdFilter.SESSION_ID_MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("withSession should restore previous session ID after execution")
    void withSessionShouldRestorePreviousSessionId() {
        // Given
        UUID outerSessionId = UUID.randomUUID();
        UUID innerSessionId = UUID.randomUUID();
        AtomicReference<String> innerCaptured = new AtomicReference<>();
        AtomicReference<String> afterInnerCaptured = new AtomicReference<>();

        // When - nested withSession calls
        LoggingContext.withSession(outerSessionId, () -> {
            LoggingContext.withSession(innerSessionId, () -> {
                innerCaptured.set(MDC.get(CorrelationIdFilter.SESSION_ID_MDC_KEY));
            });
            afterInnerCaptured.set(MDC.get(CorrelationIdFilter.SESSION_ID_MDC_KEY));
        });

        // Then
        assertThat(innerCaptured.get()).isEqualTo(innerSessionId.toString());
        assertThat(afterInnerCaptured.get()).isEqualTo(outerSessionId.toString());
        assertThat(MDC.get(CorrelationIdFilter.SESSION_ID_MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("withSession should handle null session ID gracefully")
    void withSessionShouldHandleNullSessionId() {
        // Given
        AtomicReference<String> capturedSessionId = new AtomicReference<>();

        // When
        LoggingContext.withSession(null, () -> {
            capturedSessionId.set(MDC.get(CorrelationIdFilter.SESSION_ID_MDC_KEY));
        });

        // Then
        assertThat(capturedSessionId.get()).isNull();
    }

    @Test
    @DisplayName("setSessionId and clearSessionId should work correctly")
    void setSessionIdAndClearSessionIdShouldWork() {
        // Given
        UUID sessionId = UUID.randomUUID();

        // When
        LoggingContext.setSessionId(sessionId);

        // Then
        assertThat(MDC.get(CorrelationIdFilter.SESSION_ID_MDC_KEY)).isEqualTo(sessionId.toString());

        // When
        LoggingContext.clearSessionId();

        // Then
        assertThat(MDC.get(CorrelationIdFilter.SESSION_ID_MDC_KEY)).isNull();
    }

    // ==================== User Context Tests ====================

    @Test
    @DisplayName("withUser should set and clear user ID for Runnable")
    void withUserShouldSetAndClearUserIdForRunnable() {
        // Given
        String userId = "user_clerk_123";
        AtomicReference<String> capturedUserId = new AtomicReference<>();

        // When
        LoggingContext.withUser(userId, () -> {
            capturedUserId.set(MDC.get(CorrelationIdFilter.USER_ID_MDC_KEY));
        });

        // Then
        assertThat(capturedUserId.get()).isEqualTo(userId);
        assertThat(MDC.get(CorrelationIdFilter.USER_ID_MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("withUser should handle blank user ID gracefully")
    void withUserShouldHandleBlankUserId() {
        // Given
        AtomicReference<String> capturedUserId = new AtomicReference<>();

        // When
        LoggingContext.withUser("   ", () -> {
            capturedUserId.set(MDC.get(CorrelationIdFilter.USER_ID_MDC_KEY));
        });

        // Then
        assertThat(capturedUserId.get()).isNull();
    }

    @Test
    @DisplayName("setUserId and clearUserId should work correctly")
    void setUserIdAndClearUserIdShouldWork() {
        // Given
        String userId = "user_123";

        // When
        LoggingContext.setUserId(userId);

        // Then
        assertThat(MDC.get(CorrelationIdFilter.USER_ID_MDC_KEY)).isEqualTo(userId);

        // When
        LoggingContext.clearUserId();

        // Then
        assertThat(MDC.get(CorrelationIdFilter.USER_ID_MDC_KEY)).isNull();
    }

    // ==================== Template Context Tests ====================

    @Test
    @DisplayName("withTemplate should set and clear template ID")
    void withTemplateShouldSetAndClearTemplateId() {
        // Given
        UUID templateId = UUID.randomUUID();
        AtomicReference<String> capturedTemplateId = new AtomicReference<>();

        // When
        LoggingContext.withTemplate(templateId, () -> {
            capturedTemplateId.set(MDC.get(LoggingContext.TEMPLATE_ID_MDC_KEY));
        });

        // Then
        assertThat(capturedTemplateId.get()).isEqualTo(templateId.toString());
        assertThat(MDC.get(LoggingContext.TEMPLATE_ID_MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("setTemplateId and clearTemplateId should work correctly")
    void setTemplateIdAndClearTemplateIdShouldWork() {
        // Given
        UUID templateId = UUID.randomUUID();

        // When
        LoggingContext.setTemplateId(templateId);

        // Then
        assertThat(MDC.get(LoggingContext.TEMPLATE_ID_MDC_KEY)).isEqualTo(templateId.toString());

        // When
        LoggingContext.clearTemplateId();

        // Then
        assertThat(MDC.get(LoggingContext.TEMPLATE_ID_MDC_KEY)).isNull();
    }

    // ==================== Operation Context Tests ====================

    @Test
    @DisplayName("withOperation should set and clear operation name")
    void withOperationShouldSetAndClearOperationName() {
        // Given
        String operation = "scoring";
        AtomicReference<String> capturedOperation = new AtomicReference<>();

        // When
        LoggingContext.withOperation(operation, () -> {
            capturedOperation.set(MDC.get(LoggingContext.OPERATION_MDC_KEY));
        });

        // Then
        assertThat(capturedOperation.get()).isEqualTo(operation);
        assertThat(MDC.get(LoggingContext.OPERATION_MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("setOperation and clearOperation should work correctly")
    void setOperationAndClearOperationShouldWork() {
        // Given
        String operation = "assembly";

        // When
        LoggingContext.setOperation(operation);

        // Then
        assertThat(MDC.get(LoggingContext.OPERATION_MDC_KEY)).isEqualTo(operation);

        // When
        LoggingContext.clearOperation();

        // Then
        assertThat(MDC.get(LoggingContext.OPERATION_MDC_KEY)).isNull();
    }

    // ==================== Competency Context Tests ====================

    @Test
    @DisplayName("withCompetency should set and clear competency ID")
    void withCompetencyShouldSetAndClearCompetencyId() {
        // Given
        UUID competencyId = UUID.randomUUID();
        AtomicReference<String> capturedCompetencyId = new AtomicReference<>();

        // When
        LoggingContext.withCompetency(competencyId, () -> {
            capturedCompetencyId.set(MDC.get(LoggingContext.COMPETENCY_ID_MDC_KEY));
        });

        // Then
        assertThat(capturedCompetencyId.get()).isEqualTo(competencyId.toString());
        assertThat(MDC.get(LoggingContext.COMPETENCY_ID_MDC_KEY)).isNull();
    }

    // ==================== Utility Method Tests ====================

    @Test
    @DisplayName("getCorrelationId should return MDC value")
    void getCorrelationIdShouldReturnMdcValue() {
        // Given
        String correlationId = "test-correlation-123";
        MDC.put(CorrelationIdFilter.CORRELATION_ID_MDC_KEY, correlationId);

        // When
        String result = LoggingContext.getCorrelationId();

        // Then
        assertThat(result).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("getSessionId should return MDC value")
    void getSessionIdShouldReturnMdcValue() {
        // Given
        String sessionId = "test-session-123";
        MDC.put(CorrelationIdFilter.SESSION_ID_MDC_KEY, sessionId);

        // When
        String result = LoggingContext.getSessionId();

        // Then
        assertThat(result).isEqualTo(sessionId);
    }

    @Test
    @DisplayName("getUserId should return MDC value")
    void getUserIdShouldReturnMdcValue() {
        // Given
        String userId = "test-user-123";
        MDC.put(CorrelationIdFilter.USER_ID_MDC_KEY, userId);

        // When
        String result = LoggingContext.getUserId();

        // Then
        assertThat(result).isEqualTo(userId);
    }

    @Test
    @DisplayName("clearAll should clear all MDC values")
    void clearAllShouldClearAllMdcValues() {
        // Given
        MDC.put(CorrelationIdFilter.CORRELATION_ID_MDC_KEY, "correlation-123");
        MDC.put(CorrelationIdFilter.SESSION_ID_MDC_KEY, "session-123");
        MDC.put(CorrelationIdFilter.USER_ID_MDC_KEY, "user-123");
        MDC.put(LoggingContext.OPERATION_MDC_KEY, "test-operation");

        // When
        LoggingContext.clearAll();

        // Then
        assertThat(LoggingContext.getCorrelationId()).isNull();
        assertThat(LoggingContext.getSessionId()).isNull();
        assertThat(LoggingContext.getUserId()).isNull();
        assertThat(MDC.get(LoggingContext.OPERATION_MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("withSession should clear MDC even when exception is thrown")
    void withSessionShouldClearMdcOnException() {
        // Given
        UUID sessionId = UUID.randomUUID();

        // When/Then
        assertThatThrownBy(() -> {
            LoggingContext.withSession(sessionId, () -> {
                throw new RuntimeException("Test exception");
            });
        }).isInstanceOf(RuntimeException.class);

        // MDC should still be cleared
        assertThat(MDC.get(CorrelationIdFilter.SESSION_ID_MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("withSessionChecked should handle checked exceptions")
    void withSessionCheckedShouldHandleCheckedExceptions() {
        // Given
        UUID sessionId = UUID.randomUUID();

        // When
        assertThatThrownBy(() -> {
            LoggingContext.withSessionChecked(sessionId, () -> {
                throw new Exception("Checked exception");
            });
        }).isInstanceOf(Exception.class);

        // Then - MDC should be cleared
        assertThat(MDC.get(CorrelationIdFilter.SESSION_ID_MDC_KEY)).isNull();
    }
}
