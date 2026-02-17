package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.dto.DeletionPreviewDto;
import app.skillsoft.assessmentbackend.domain.dto.DeletionResultDto;
import app.skillsoft.assessmentbackend.domain.entities.DeletionMode;
import app.skillsoft.assessmentbackend.services.TemplateDeletionService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for Template Deletion operations.
 *
 * Provides endpoints for previewing deletion impact, performing safe deletion
 * with multiple modes, and restoring soft-deleted templates.
 *
 * Security:
 * - All endpoints require ADMIN role
 *
 * API Base Path: /api/v1/tests/templates
 */
@RestController
@RequestMapping("/api/v1/tests/templates")
@RequiredArgsConstructor
public class TemplateDeletionController {

    private static final Logger logger = LoggerFactory.getLogger(TemplateDeletionController.class);

    private final TemplateDeletionService deletionService;

    // ==================== SAFE DELETION OPERATIONS ====================

    /**
     * Preview deletion impact before confirming.
     * Shows what will be affected (sessions, results, shares, etc.)
     * and provides a recommended deletion mode.
     *
     * @param id Template UUID to preview deletion for
     * @return Deletion preview with counts and recommendations
     */
    @GetMapping("/{id}/deletion-preview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeletionPreviewDto> previewDeletion(@PathVariable UUID id) {
        logger.info("GET /api/v1/tests/templates/{}/deletion-preview", id);

        DeletionPreviewDto preview = deletionService.previewDeletion(id);
        logger.info("Deletion preview for {}: {} sessions, {} results, recommended: {}",
                id, preview.totalSessions(), preview.totalResults(), preview.recommendedMode());

        return ResponseEntity.ok(preview);
    }

    /**
     * Safely delete a template with specified mode.
     *
     * Modes:
     * - SOFT_DELETE: Mark as deleted, preserve all data (can be restored)
     * - ARCHIVE_AND_CLEANUP: Archive template, delete incomplete sessions
     * - FORCE_DELETE: Permanently delete template and ALL related data
     *
     * @param id Template UUID to delete
     * @param mode Deletion mode (default: SOFT_DELETE)
     * @param confirmed Required for operations that affect existing data
     * @return Deletion result with counts of affected entities
     */
    @DeleteMapping("/{id}/safe")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeletionResultDto> safeDeleteTemplate(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "SOFT_DELETE") DeletionMode mode,
            @RequestParam(defaultValue = "false") boolean confirmed) {
        logger.info("DELETE /api/v1/tests/templates/{}/safe?mode={}&confirmed={}",
                id, mode, confirmed);

        try {
            DeletionResultDto result = deletionService.deleteTemplate(id, mode, confirmed);
            logger.info("Template {} deleted with mode {}: success={}", id, mode, result.success());
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            logger.warn("Deletion requires confirmation: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(DeletionResultDto.failed(id, mode, e.getMessage()));
        }
    }

    /**
     * Restore a soft-deleted template.
     *
     * @param id Template UUID to restore
     * @return 200 if restored, 404 if not found or not soft-deleted
     */
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> restoreTemplate(@PathVariable UUID id) {
        logger.info("POST /api/v1/tests/templates/{}/restore", id);

        boolean restored = deletionService.restoreTemplate(id);
        if (restored) {
            logger.info("Restored template: {}", id);
            return ResponseEntity.ok().build();
        } else {
            logger.warn("Template not found or not soft-deleted: {}", id);
            return ResponseEntity.notFound().build();
        }
    }
}
