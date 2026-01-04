package app.skillsoft.assessmentbackend.domain.dto.team;

import java.util.UUID;

/**
 * API response DTO for leader change.
 */
public record LeaderChangeResultDto(
        boolean success,
        UUID previousLeaderId,
        UUID newLeaderId
) {}
