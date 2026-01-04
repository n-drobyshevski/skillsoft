package app.skillsoft.assessmentbackend.domain.dto.team;

import java.util.List;
import java.util.UUID;

/**
 * Internal command DTO for adding members to a team.
 */
public record AddMembersCommand(
        List<UUID> userIds,
        String addedByClerkId
) {}
