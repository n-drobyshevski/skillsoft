package app.skillsoft.assessmentbackend.domain.dto.team;

import java.util.UUID;

/**
 * Result DTO for changing team leader.
 */
public record LeaderChangeResult(
        boolean success,
        UUID previousLeaderId,
        UUID newLeaderId,
        String errorMessage
) {
    public static LeaderChangeResult success(UUID previousLeaderId, UUID newLeaderId) {
        return new LeaderChangeResult(true, previousLeaderId, newLeaderId, null);
    }

    public static LeaderChangeResult failure(String errorMessage) {
        return new LeaderChangeResult(false, null, null, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }
}
