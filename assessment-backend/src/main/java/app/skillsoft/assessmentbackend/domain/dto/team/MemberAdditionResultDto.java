package app.skillsoft.assessmentbackend.domain.dto.team;

import java.util.List;
import java.util.UUID;

/**
 * API response DTO for member addition.
 */
public record MemberAdditionResultDto(
        List<UUID> addedMembers,
        List<String> failures,
        boolean hasErrors
) {}
