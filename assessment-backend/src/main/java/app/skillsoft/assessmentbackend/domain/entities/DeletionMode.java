package app.skillsoft.assessmentbackend.domain.entities;

/**
 * Mode for template deletion operations.
 * Determines how the template and related data are handled during deletion.
 */
public enum DeletionMode {
    /**
     * Soft delete - set deletedAt timestamp, preserve all data.
     * Template and all related data remain in database but are filtered out.
     * Recommended for templates with completed test sessions.
     * Can be restored later.
     */
    SOFT_DELETE,

    /**
     * Archive template and cleanup incomplete sessions only.
     * Preserves completed sessions and results for historical reference.
     * Deletes: NOT_STARTED and IN_PROGRESS sessions without results.
     * Template is soft-deleted and can be restored.
     */
    ARCHIVE_AND_CLEANUP,

    /**
     * Force delete - permanently remove template and ALL related data.
     * Requires explicit confirmation. Cannot be undone.
     * Use only for cleanup of test/draft templates.
     *
     * Deletion order (to avoid FK constraint violations):
     * 1. TestActivityEvents
     * 2. TestResults (via JPA cascade)
     * 3. TestAnswers (via JPA cascade)
     * 4. TestSessions
     * 5. TemplateShareLinks
     * 6. TemplateShares
     * 7. TestTemplate
     */
    FORCE_DELETE
}
