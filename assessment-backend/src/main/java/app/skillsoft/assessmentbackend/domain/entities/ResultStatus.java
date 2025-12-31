package app.skillsoft.assessmentbackend.domain.entities;

/**
 * Enum representing the status of a test result calculation.
 *
 * Used to track the scoring lifecycle, especially for retry scenarios
 * where transient failures may leave results in a pending state.
 *
 * Статус расчета результата теста:
 * - PENDING: Ожидает обработки (для повторных попыток)
 * - COMPLETED: Расчет успешно завершен
 * - FAILED: Расчет не удался после всех попыток
 */
public enum ResultStatus {
    /**
     * Result is pending calculation or retry.
     * Ожидает расчета или повторной попытки.
     */
    PENDING,

    /**
     * Result calculation completed successfully.
     * Расчет успешно завершен.
     */
    COMPLETED,

    /**
     * Result calculation failed after all retry attempts.
     * Расчет не удался после всех попыток.
     */
    FAILED
}
