package app.skillsoft.assessmentbackend.domain.dto.team;

import java.util.UUID;

/**
 * Lightweight DTO for team member in profile context.
 */
public record TeamMemberSummaryDto(
        UUID userId,
        String name,
        String role
) {}
