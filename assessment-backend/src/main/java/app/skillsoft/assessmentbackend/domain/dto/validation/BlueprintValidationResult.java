package app.skillsoft.assessmentbackend.domain.dto.validation;

import java.util.List;

/**
 * Result of blueprint validation containing structured errors and warnings.
 *
 * <p>Used by {@code BlueprintValidationService} to return a comprehensive
 * validation report that distinguishes between blocking errors and advisory warnings.</p>
 *
 * @param valid        Whether the blueprint has no blocking errors
 * @param errors       Blocking issues that prevent the operation
 * @param warnings     Non-blocking issues that may affect quality
 * @param canSimulate  Whether the blueprint is sufficiently configured for simulation
 * @param canPublish   Whether the blueprint meets all requirements for publishing
 */
public record BlueprintValidationResult(
        boolean valid,
        List<ValidationIssue> errors,
        List<ValidationIssue> warnings,
        boolean canSimulate,
        boolean canPublish
) {

    /**
     * Create a fully valid result with no issues.
     */
    public static BlueprintValidationResult allValid() {
        return new BlueprintValidationResult(true, List.of(), List.of(), true, true);
    }

    /**
     * Convenience method to get all issues (errors + warnings) combined.
     *
     * @return Combined list of all validation issues
     */
    public List<ValidationIssue> allIssues() {
        if (errors.isEmpty()) {
            return warnings;
        }
        if (warnings.isEmpty()) {
            return errors;
        }
        var combined = new java.util.ArrayList<>(errors);
        combined.addAll(warnings);
        return List.copyOf(combined);
    }

    /**
     * Get all error messages as a flat list of strings.
     * Useful for backward compatibility with methods that return {@code List<String>}.
     *
     * @return List of error message strings
     */
    public List<String> errorMessages() {
        return errors.stream()
                .map(ValidationIssue::message)
                .toList();
    }

    /**
     * Check if there are any warnings.
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    /**
     * Check if there are any errors.
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
