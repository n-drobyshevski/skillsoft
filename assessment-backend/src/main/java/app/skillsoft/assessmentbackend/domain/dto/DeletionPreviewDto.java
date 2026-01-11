package app.skillsoft.assessmentbackend.domain.dto;

import java.util.UUID;

/**
 * Preview of what will be affected by a template deletion.
 * Used to inform the user before they confirm deletion.
 */
public record DeletionPreviewDto(
    UUID templateId,
    String templateName,
    String templateStatus,

    // Session counts
    long activeSessions,      // IN_PROGRESS, NOT_STARTED
    long completedSessions,   // COMPLETED
    long abandonedSessions,   // ABANDONED, TIMED_OUT
    long totalSessions,

    // Related data counts
    long totalResults,
    long totalAnswers,
    long activeShares,
    long activeShareLinks,
    long activityEvents,

    // Recommendations
    String recommendedMode,
    String warningMessage,
    boolean canForceDelete,
    boolean requiresConfirmation
) {
    /**
     * Create a preview for a draft template with no dependencies.
     */
    public static DeletionPreviewDto forDraftTemplate(UUID templateId, String name) {
        return new DeletionPreviewDto(
            templateId, name, "DRAFT",
            0, 0, 0, 0,
            0, 0, 0, 0, 0,
            "FORCE_DELETE",
            null,
            true,
            false
        );
    }

    /**
     * Create a preview for a template that has been soft-deleted.
     */
    public static DeletionPreviewDto forSoftDeletedTemplate(UUID templateId, String name) {
        return new DeletionPreviewDto(
            templateId, name, "SOFT_DELETED",
            0, 0, 0, 0,
            0, 0, 0, 0, 0,
            "FORCE_DELETE",
            "Template is already soft-deleted. Use FORCE_DELETE to permanently remove.",
            true,
            true
        );
    }
}
