package app.skillsoft.assessmentbackend.domain.dto.team;

import java.time.LocalDateTime;

/**
 * Result DTO for team archiving.
 */
public record ArchiveResult(
        boolean success,
        LocalDateTime archivedAt,
        String errorMessage
) {
    public static ArchiveResult success(LocalDateTime archivedAt) {
        return new ArchiveResult(true, archivedAt, null);
    }

    public static ArchiveResult failure(String errorMessage) {
        return new ArchiveResult(false, null, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public LocalDateTime getArchivedAt() {
        return archivedAt;
    }
}
