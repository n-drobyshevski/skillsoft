package app.skillsoft.assessmentbackend.domain.dto.team;

import java.util.UUID;

/**
 * Request DTO for setting or changing a team leader.
 */
public record SetLeaderRequest(
        /** User ID to set as leader. Pass null to remove leader. */
        UUID leaderId
) {}
