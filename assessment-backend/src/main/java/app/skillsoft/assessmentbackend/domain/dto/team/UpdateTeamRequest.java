package app.skillsoft.assessmentbackend.domain.dto.team;

import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing Team.
 */
public record UpdateTeamRequest(
        @Size(max = 200, message = "Team name must not exceed 200 characters")
        String name,

        String description
) {}
