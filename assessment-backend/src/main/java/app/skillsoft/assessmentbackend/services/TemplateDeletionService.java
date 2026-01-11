package app.skillsoft.assessmentbackend.services;

import app.skillsoft.assessmentbackend.domain.dto.DeletionPreviewDto;
import app.skillsoft.assessmentbackend.domain.dto.DeletionResultDto;
import app.skillsoft.assessmentbackend.domain.entities.DeletionMode;
import app.skillsoft.assessmentbackend.domain.entities.User;

import java.util.UUID;

/**
 * Service for safe deletion of test templates.
 * Handles dependency chain analysis and provides preview before deletion.
 *
 * <p>Supports three deletion modes:
 * <ul>
 *   <li>{@link DeletionMode#SOFT_DELETE} - Mark as deleted, preserve all data</li>
 *   <li>{@link DeletionMode#ARCHIVE_AND_CLEANUP} - Archive template, delete incomplete sessions</li>
 *   <li>{@link DeletionMode#FORCE_DELETE} - Permanently delete template and all related data</li>
 * </ul>
 */
public interface TemplateDeletionService {

    /**
     * Get a preview of what will be affected by deleting a template.
     * Does not modify any data.
     *
     * @param templateId The template to analyze
     * @return Preview with counts and recommendations
     * @throws app.skillsoft.assessmentbackend.exception.ResourceNotFoundException if template not found
     */
    DeletionPreviewDto previewDeletion(UUID templateId);

    /**
     * Delete a template using the specified mode.
     *
     * @param templateId The template to delete
     * @param mode The deletion mode (SOFT_DELETE, ARCHIVE_AND_CLEANUP, FORCE_DELETE)
     * @param confirmedByUser True if user explicitly confirmed the deletion
     * @return Result of the deletion operation
     * @throws app.skillsoft.assessmentbackend.exception.ResourceNotFoundException if template not found
     * @throws IllegalStateException if confirmation is required but not provided
     */
    DeletionResultDto deleteTemplate(UUID templateId, DeletionMode mode, boolean confirmedByUser);

    /**
     * Delete a template using the specified mode with user context.
     * Records who performed the deletion for audit purposes.
     *
     * @param templateId The template to delete
     * @param mode The deletion mode
     * @param confirmedByUser True if user explicitly confirmed the deletion
     * @param deletedBy The user performing the deletion
     * @return Result of the deletion operation
     */
    DeletionResultDto deleteTemplate(UUID templateId, DeletionMode mode, boolean confirmedByUser, User deletedBy);

    /**
     * Restore a soft-deleted template.
     *
     * @param templateId The template to restore
     * @return True if restored successfully, false if template not found or not soft-deleted
     */
    boolean restoreTemplate(UUID templateId);
}
