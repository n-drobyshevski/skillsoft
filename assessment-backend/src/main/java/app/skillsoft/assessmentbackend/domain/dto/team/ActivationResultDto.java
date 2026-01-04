package app.skillsoft.assessmentbackend.domain.dto.team;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API response DTO for team activation.
 */
public record ActivationResultDto(
        boolean success,
        LocalDateTime activatedAt,
        List<String> errors
) {}
