package app.skillsoft.assessmentbackend.domain.dto.team;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for adding members to a team.
 */
public record AddMembersRequest(
        @NotEmpty(message = "At least one user ID is required")
        List<UUID> userIds
) {}
