package app.skillsoft.assessmentbackend.domain.dto.team;

import app.skillsoft.assessmentbackend.domain.entities.TeamMemberRole;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for team member information.
 */
public record TeamMemberDto(
        UUID userId,
        String clerkId,
        String fullName,
        String email,
        String imageUrl,
        TeamMemberRole role,
        LocalDateTime joinedAt,
        boolean isActive
) {}
