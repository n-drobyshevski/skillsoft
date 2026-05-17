package app.skillsoft.assessmentbackend.domain.dto.validation;

/**
 * A single validation issue detected during blueprint validation.
 *
 * @param id             Machine-readable identifier (e.g., "no-competencies", "low-question-count")
 * @param severity       Severity level: ERROR, WARNING, or INFO
 * @param message        Human-readable description of the issue
 * @param competencyId   Optional competency UUID string for competency-specific issues (null for global issues)
 */
public record ValidationIssue(
        String id,
        ValidationSeverity severity,
        String message,
        String competencyId
) {

    /**
     * Create an ERROR-level issue not tied to a specific competency.
     */
    public static ValidationIssue error(String id, String message) {
        return new ValidationIssue(id, ValidationSeverity.ERROR, message, null);
    }

    /**
     * Create a WARNING-level issue not tied to a specific competency.
     */
    public static ValidationIssue warning(String id, String message) {
        return new ValidationIssue(id, ValidationSeverity.WARNING, message, null);
    }

    /**
     * Create an INFO-level issue not tied to a specific competency.
     */
    public static ValidationIssue info(String id, String message) {
        return new ValidationIssue(id, ValidationSeverity.INFO, message, null);
    }

    /**
     * Create an ERROR-level issue tied to a specific competency.
     */
    public static ValidationIssue errorForCompetency(String id, String message, String competencyId) {
        return new ValidationIssue(id, ValidationSeverity.ERROR, message, competencyId);
    }

    /**
     * Create a WARNING-level issue tied to a specific competency.
     */
    public static ValidationIssue warningForCompetency(String id, String message, String competencyId) {
        return new ValidationIssue(id, ValidationSeverity.WARNING, message, competencyId);
    }
}
