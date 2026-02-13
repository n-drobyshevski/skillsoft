package app.skillsoft.assessmentbackend.domain.dto.team;

import java.util.UUID;

/**
 * Summary DTO for User information in team context.
 */
public record UserSummaryDto(
        UUID id,
        String clerkId,
        String fullName,
        String email,
        String imageUrl
) {}
