package app.skillsoft.assessmentbackend.domain.dto.team;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating a new Team.
 */
public record CreateTeamRequest(
        @NotBlank(message = "Team name is required")
        @Size(max = 200, message = "Team name must not exceed 200 characters")
        String name,

        String description,

        /** List of user IDs to add as initial members */
        List<UUID> memberIds,

        /** User ID to set as team leader (must be in memberIds) */
        UUID leaderId,

        /** Whether to activate the team immediately after creation */
        boolean activateImmediately
) {
    public CreateTeamRequest {
        if (memberIds == null) memberIds = List.of();
    }
}
