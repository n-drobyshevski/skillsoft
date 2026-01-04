package app.skillsoft.assessmentbackend.domain.dto.team;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Result DTO for team activation.
 */
public record ActivationResult(
        boolean success,
        LocalDateTime activatedAt,
        List<String> errors
) {
    public static ActivationResult success(LocalDateTime activatedAt) {
        return new ActivationResult(true, activatedAt, List.of());
    }

    public static ActivationResult failure(List<String> errors) {
        return new ActivationResult(false, null, errors);
    }

    public boolean isSuccess() {
        return success;
    }

    public LocalDateTime getActivatedAt() {
        return activatedAt;
    }

    public List<String> getErrors() {
        return errors;
    }
}
