package app.skillsoft.assessmentbackend.domain.dto;

import app.skillsoft.assessmentbackend.domain.entities.DeletionMode;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Result of a template deletion operation.
 * Contains information about what was deleted and whether restoration is possible.
 */
public record DeletionResultDto(
    UUID templateId,
    DeletionMode mode,
    boolean success,
    String message,
    LocalDateTime deletedAt,

    // Counts of deleted/affected entities
    int sessionsDeleted,
    int resultsDeleted,
    int answersDeleted,
    int sharesDeleted,
    int shareLinksDeleted,
    int activityEventsDeleted,

    // For soft delete, can be restored
    boolean canRestore
) {
    /**
     * Create a result for a successful soft delete operation.
     */
    public static DeletionResultDto softDeleted(UUID templateId, LocalDateTime deletedAt) {
        return new DeletionResultDto(
            templateId,
            DeletionMode.SOFT_DELETE,
            true,
            "Template soft deleted successfully. Data preserved for restoration.",
            deletedAt,
            0, 0, 0, 0, 0, 0,
            true
        );
    }

    /**
     * Create a result for a successful archive and cleanup operation.
     */
    public static DeletionResultDto archivedAndCleanedUp(
            UUID templateId,
            LocalDateTime deletedAt,
            int sessionsDeleted,
            int answersDeleted) {
        return new DeletionResultDto(
            templateId,
            DeletionMode.ARCHIVE_AND_CLEANUP,
            true,
            "Template archived and incomplete sessions cleaned up.",
            deletedAt,
            sessionsDeleted, 0, answersDeleted, 0, 0, 0,
            true
        );
    }

    /**
     * Create a result for a successful force delete operation.
     */
    public static DeletionResultDto forceDeleted(
            UUID templateId,
            int sessions, int results, int answers,
            int shares, int shareLinks, int events) {
        return new DeletionResultDto(
            templateId,
            DeletionMode.FORCE_DELETE,
            true,
            "Template and all related data permanently deleted.",
            LocalDateTime.now(),
            sessions, results, answers, shares, shareLinks, events,
            false
        );
    }

    /**
     * Create a result for a failed deletion operation.
     */
    public static DeletionResultDto failed(UUID templateId, DeletionMode mode, String errorMessage) {
        return new DeletionResultDto(
            templateId,
            mode,
            false,
            errorMessage,
            null,
            0, 0, 0, 0, 0, 0,
            false
        );
    }
}
