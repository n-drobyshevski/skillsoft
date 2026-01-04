package app.skillsoft.assessmentbackend.domain.dto.team;

import app.skillsoft.assessmentbackend.domain.entities.TeamStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Full DTO for team details.
 */
public record TeamDto(
        UUID id,
        String name,
        String description,
        TeamStatus status,
        UserSummaryDto leader,
        UserSummaryDto createdBy,
        List<TeamMemberDto> members,
        int memberCount,
        LocalDateTime createdAt,
        LocalDateTime activatedAt,
        LocalDateTime archivedAt
) {}
