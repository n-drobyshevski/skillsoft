package app.skillsoft.assessmentbackend.domain.dto.team;

import app.skillsoft.assessmentbackend.domain.entities.TeamStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Summary DTO for team list views.
 */
public record TeamSummaryDto(
        UUID id,
        String name,
        String description,
        TeamStatus status,
        UserSummaryDto leader,
        int memberCount,
        LocalDateTime createdAt
) {}
