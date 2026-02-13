package app.skillsoft.assessmentbackend.domain.dto.team;

import java.util.List;
import java.util.UUID;

/**
 * Internal command DTO for team creation saga.
 * Includes the creator's user ID resolved from authentication.
 */
public record CreateTeamCommand(
        String name,
        String description,
        List<UUID> memberIds,
        UUID leaderId,
        boolean activateImmediately,
        UUID createdById
) {
    public static CreateTeamCommand from(CreateTeamRequest request, UUID createdById) {
        return new CreateTeamCommand(
                request.name(),
                request.description(),
                request.memberIds(),
                request.leaderId(),
                request.activateImmediately(),
                createdById
        );
    }
}
