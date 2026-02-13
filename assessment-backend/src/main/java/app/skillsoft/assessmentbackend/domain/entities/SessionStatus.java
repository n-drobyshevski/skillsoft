package app.skillsoft.assessmentbackend.domain.entities;

/**
 * Enum representing the status of a test session.
 */
public enum SessionStatus {
    NOT_STARTED,    // Не начата
    IN_PROGRESS,    // В процессе
    COMPLETED,      // Завершена
    ABANDONED,      // Отменена
    TIMED_OUT       // Время истекло
}
