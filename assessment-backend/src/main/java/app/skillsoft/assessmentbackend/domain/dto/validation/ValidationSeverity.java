package app.skillsoft.assessmentbackend.domain.dto.validation;

/**
 * Severity levels for blueprint validation issues.
 *
 * <ul>
 *   <li>{@link #ERROR} - Blocking issue that prevents the operation (publish, simulate)</li>
 *   <li>{@link #WARNING} - Non-blocking issue that may affect quality but does not prevent the operation</li>
 *   <li>{@link #INFO} - Informational notice for the user</li>
 * </ul>
 */
public enum ValidationSeverity {
    ERROR,
    WARNING,
    INFO
}
