package app.skillsoft.assessmentbackend.domain.dto.team;

import java.util.List;
import java.util.UUID;

/**
 * Result DTO for adding members to a team.
 */
public record MemberAdditionResult(
        List<UUID> addedMembers,
        List<String> failures,
        boolean hasErrors
) {
    public static MemberAdditionResult success(List<UUID> addedMembers) {
        return new MemberAdditionResult(addedMembers, List.of(), false);
    }

    public static MemberAdditionResult partialSuccess(List<UUID> addedMembers, List<String> failures) {
        return new MemberAdditionResult(addedMembers, failures, !failures.isEmpty());
    }

    public static MemberAdditionResult failure(String errorMessage) {
        return new MemberAdditionResult(List.of(), List.of(errorMessage), true);
    }
}
