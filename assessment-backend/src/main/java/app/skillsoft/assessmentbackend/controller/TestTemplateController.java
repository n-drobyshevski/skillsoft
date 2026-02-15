package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.dto.*;
import app.skillsoft.assessmentbackend.domain.dto.sharing.ChangeVisibilityRequest;
import app.skillsoft.assessmentbackend.domain.dto.sharing.SharedTemplatesResponseDto;
import app.skillsoft.assessmentbackend.domain.dto.sharing.SharedWithMeCountDto;
import app.skillsoft.assessmentbackend.domain.dto.sharing.VisibilityInfoDto;
import app.skillsoft.assessmentbackend.domain.entities.DeletionMode;
import app.skillsoft.assessmentbackend.domain.entities.TemplateVisibility;
import app.skillsoft.assessmentbackend.services.AnonymousTestService;
import app.skillsoft.assessmentbackend.services.TemplateDeletionService;
import app.skillsoft.assessmentbackend.services.TestTemplateService;
import app.skillsoft.assessmentbackend.services.security.TemplateSecurityService;
import app.skillsoft.assessmentbackend.services.sharing.TemplateShareService;
import app.skillsoft.assessmentbackend.services.sharing.TemplateVisibilityService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Test Template management.
 * 
 * Provides endpoints for creating, updating, and managing test templates
 * that define the structure and configuration of competency assessments.
 * 
 * Security:
 * - GET endpoints: All authenticated users
 * - POST/PUT/DELETE: ADMIN or EDITOR role required
 * 
 * API Base Path: /api/v1/tests/templates
 */
@RestController
@RequestMapping("/api/v1/tests/templates")
public class TestTemplateController {

    private static final Logger logger = LoggerFactory.getLogger(TestTemplateController.class);

    private final TestTemplateService testTemplateService;
    private final TemplateVisibilityService visibilityService;
    private final TemplateSecurityService securityService;
    private final TemplateShareService templateShareService;
    private final TemplateDeletionService deletionService;
    private final AnonymousTestService anonymousTestService;

    public TestTemplateController(TestTemplateService testTemplateService,
                                  TemplateVisibilityService visibilityService,
                                  TemplateSecurityService securityService,
                                  TemplateShareService templateShareService,
                                  TemplateDeletionService deletionService,
                                  AnonymousTestService anonymousTestService) {
        this.testTemplateService = testTemplateService;
        this.visibilityService = visibilityService;
        this.securityService = securityService;
        this.templateShareService = templateShareService;
        this.deletionService = deletionService;
        this.anonymousTestService = anonymousTestService;
    }

    // ==================== READ OPERATIONS ====================

    /**
     * List all test templates with pagination.
     * 
     * @param pageable Pagination parameters (page, size, sort)
     * @return Page of template summaries
     */
    @GetMapping
    public ResponseEntity<Page<TestTemplateSummaryDto>> listTemplates(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) 
            Pageable pageable) {
        logger.info("GET /api/v1/tests/templates - Listing templates, page: {}, size: {}", 
                pageable.getPageNumber(), pageable.getPageSize());
        
        Page<TestTemplateSummaryDto> templates = testTemplateService.listTemplates(pageable);
        logger.info("Found {} templates (total: {})", templates.getNumberOfElements(), templates.getTotalElements());
        
        return ResponseEntity.ok(templates);
    }

    /**
     * List only active test templates (for test-takers).
     * 
     * @return List of active template summaries
     */
    @GetMapping("/active")
    public ResponseEntity<List<TestTemplateSummaryDto>> listActiveTemplates() {
        logger.info("GET /api/v1/tests/templates/active - Listing active templates");
        
        List<TestTemplateSummaryDto> templates = testTemplateService.listActiveTemplates();
        logger.info("Found {} active templates", templates.size());
        
        return ResponseEntity.ok(templates);
    }

    /**
     * List active templates owned by the current user.
     * Used for personal mode catalog — shows only templates the user created.
     *
     * @return List of template summaries owned by the authenticated user
     */
    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TestTemplateSummaryDto>> listMyTemplates() {
        logger.info("GET /api/v1/tests/templates/mine - Listing user's own templates");

        String clerkId = securityService.getAuthenticatedClerkId();
        if (clerkId == null) {
            logger.warn("No authenticated user for my-templates request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<TestTemplateSummaryDto> templates = testTemplateService.listMyTemplates(clerkId);
        logger.info("Found {} templates owned by user", templates.size());
        return ResponseEntity.ok(templates);
    }

    /**
     * Get a specific test template by ID.
     *
     * @param id Template UUID
     * @return Template details or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<TestTemplateDto> getTemplateById(@PathVariable UUID id) {
        logger.info("GET /api/v1/tests/templates/{} - Getting template", id);
        
        return testTemplateService.findById(id)
                .map(template -> {
                    logger.info("Found template: {}", template.name());
                    return ResponseEntity.ok(template);
                })
                .orElseGet(() -> {
                    logger.warn("Template not found with id: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Search templates by name.
     * 
     * @param name Name to search for (case-insensitive partial match)
     * @return List of matching template summaries
     */
    @GetMapping("/search")
    public ResponseEntity<List<TestTemplateSummaryDto>> searchTemplates(
            @RequestParam String name) {
        logger.info("GET /api/v1/tests/templates/search?name={}", name);
        
        List<TestTemplateSummaryDto> templates = testTemplateService.searchByName(name);
        logger.info("Found {} templates matching '{}'", templates.size(), name);
        
        return ResponseEntity.ok(templates);
    }

    /**
     * Find templates that include a specific competency.
     * 
     * @param competencyId Competency UUID to search for
     * @return List of templates that assess this competency
     */
    @GetMapping("/by-competency/{competencyId}")
    public ResponseEntity<List<TestTemplateSummaryDto>> findByCompetency(
            @PathVariable UUID competencyId) {
        logger.info("GET /api/v1/tests/templates/by-competency/{}", competencyId);
        
        List<TestTemplateSummaryDto> templates = testTemplateService.findByCompetency(competencyId);
        logger.info("Found {} templates for competency {}", templates.size(), competencyId);
        
        return ResponseEntity.ok(templates);
    }

    /**
     * Get template statistics (counts).
     * 
     * @return Statistics about templates
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<TestTemplateService.TemplateStatistics> getStatistics() {
        logger.info("GET /api/v1/tests/templates/statistics");
        
        TestTemplateService.TemplateStatistics stats = testTemplateService.getStatistics();
        logger.info("Template statistics: total={}, active={}", stats.totalTemplates(), stats.activeTemplates());
        
        return ResponseEntity.ok(stats);
    }

    // ==================== WRITE OPERATIONS ====================

    /**
     * Create a new test template.
     * 
     * @param request Template creation request
     * @return Created template with 201 status
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<TestTemplateDto> createTemplate(
            @Valid @RequestBody CreateTestTemplateRequest request) {
        logger.info("POST /api/v1/tests/templates - Creating template: {}", request.name());
        
        try {
            TestTemplateDto created = testTemplateService.createTemplate(request);
            logger.info("Created template with id: {}", created.id());
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid template data: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update an existing test template.
     * 
     * @param id Template UUID to update
     * @param request Update request with fields to modify
     * @return Updated template or 404 if not found
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<TestTemplateDto> updateTemplate(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTestTemplateRequest request) {
        logger.info("PUT /api/v1/tests/templates/{} - Updating template", id);
        
        try {
            TestTemplateDto updated = testTemplateService.updateTemplate(id, request);
            logger.info("Updated template: {}", updated.name());
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                logger.warn("Template not found for update: {}", id);
                return ResponseEntity.notFound().build();
            }
            logger.error("Error updating template {}: {}", id, e.getMessage());
            throw e;
        }
    }

    /**
     * Delete a test template using soft delete.
     * This endpoint uses SOFT_DELETE mode by default, which marks the template
     * as deleted but preserves all data. Use the /safe endpoint for other modes.
     *
     * @param id Template UUID to delete
     * @return 204 No Content or 404 if not found
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID id) {
        logger.info("DELETE /api/v1/tests/templates/{} - Using soft delete", id);

        try {
            DeletionResultDto result = deletionService.deleteTemplate(id, DeletionMode.SOFT_DELETE, true);
            if (result.success()) {
                logger.info("Soft-deleted template: {}", id);
                return ResponseEntity.noContent().build();
            } else {
                logger.warn("Failed to delete template {}: {}", id, result.message());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                logger.warn("Template not found for deletion: {}", id);
                return ResponseEntity.notFound().build();
            }
            logger.error("Error deleting template {}: {}", id, e.getMessage());
            throw e;
        }
    }

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

    // ==================== ACTIVATION OPERATIONS ====================

    /**
     * Activate a test template (make it available for test-takers).
     * 
     * @param id Template UUID to activate
     * @return Updated template or 404 if not found
     */
    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<TestTemplateDto> activateTemplate(@PathVariable UUID id) {
        logger.info("POST /api/v1/tests/templates/{}/activate", id);
        
        try {
            TestTemplateDto activated = testTemplateService.activateTemplate(id);
            logger.info("Activated template: {}", activated.name());
            return ResponseEntity.ok(activated);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                logger.warn("Template not found for activation: {}", id);
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }

    /**
     * Deactivate a test template (hide from test-takers).
     *
     * @param id Template UUID to deactivate
     * @return Updated template or 404 if not found
     */
    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<TestTemplateDto> deactivateTemplate(@PathVariable UUID id) {
        logger.info("POST /api/v1/tests/templates/{}/deactivate", id);

        try {
            TestTemplateDto deactivated = testTemplateService.deactivateTemplate(id);
            logger.info("Deactivated template: {}", deactivated.name());
            return ResponseEntity.ok(deactivated);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                logger.warn("Template not found for deactivation: {}", id);
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }

    // ==================== VISIBILITY OPERATIONS ====================

    /**
     * Get visibility information for a template.
     *
     * Returns current visibility setting, owner info, and share/link counts.
     *
     * @param id Template UUID
     * @return Visibility info or 404 if not found
     */
    @GetMapping("/{id}/visibility")
    @PreAuthorize("@templateSecurity.canAccess(#id, T(app.skillsoft.assessmentbackend.domain.entities.SharePermission).VIEW)")
    public ResponseEntity<VisibilityInfoDto> getVisibility(@PathVariable UUID id) {
        logger.info("GET /api/v1/tests/templates/{}/visibility", id);

        try {
            VisibilityInfoDto info = visibilityService.getVisibilityInfo(id);
            logger.info("Visibility for template {}: {}", id, info.visibility());
            return ResponseEntity.ok(info);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                logger.warn("Template not found: {}", id);
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }

    /**
     * Change the visibility setting for a template.
     *
     * Business rules:
     * - DRAFT templates can only be PRIVATE
     * - ARCHIVED templates cannot change visibility
     * - Changing from LINK revokes all share links
     *
     * @param id Template UUID
     * @param request The new visibility setting
     * @return Updated visibility info or error
     */
    @PatchMapping("/{id}/visibility")
    @PreAuthorize("@templateSecurity.canChangeVisibility(#id)")
    public ResponseEntity<VisibilityInfoDto> changeVisibility(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeVisibilityRequest request) {
        logger.info("PATCH /api/v1/tests/templates/{}/visibility - Changing to {}",
                id, request.visibility());

        String clerkId = securityService.getAuthenticatedClerkId();
        if (clerkId == null) {
            logger.warn("No authenticated user for visibility change");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            VisibilityInfoDto info = visibilityService.changeVisibility(id, request.visibility(), clerkId);
            logger.info("Changed visibility for template {} to {}", id, info.visibility());
            return ResponseEntity.ok(info);
        } catch (IllegalStateException e) {
            logger.warn("Visibility change failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                logger.warn("Template not found: {}", id);
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }

    /**
     * Check if visibility can be changed to a specific value.
     *
     * Useful for frontend to disable invalid options.
     *
     * @param id Template UUID
     * @param visibility Target visibility to check
     * @return Boolean indicating if change is allowed
     */
    @GetMapping("/{id}/visibility/can-change")
    @PreAuthorize("@templateSecurity.canAccess(#id, T(app.skillsoft.assessmentbackend.domain.entities.SharePermission).VIEW)")
    public ResponseEntity<Boolean> canChangeToVisibility(
            @PathVariable UUID id,
            @RequestParam TemplateVisibility visibility) {
        logger.info("GET /api/v1/tests/templates/{}/visibility/can-change?visibility={}",
                id, visibility);

        boolean canChange = visibilityService.canChangeToVisibility(id, visibility);
        return ResponseEntity.ok(canChange);
    }

    // ==================== SHARED WITH ME OPERATIONS ====================

    /**
     * Get templates shared with the current user.
     * Includes both direct user shares and team membership shares.
     * Excludes templates the user owns.
     *
     * @return List of shared templates with sharing metadata
     */
    @GetMapping("/shared-with-me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SharedTemplatesResponseDto> getSharedWithMe() {
        logger.info("GET /api/v1/tests/templates/shared-with-me");

        String clerkId = securityService.getAuthenticatedClerkId();
        if (clerkId == null) {
            logger.warn("No authenticated user for shared-with-me request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        SharedTemplatesResponseDto response = templateShareService.getTemplatesSharedWithMe(clerkId);
        logger.info("Found {} templates shared with user", response.total());
        return ResponseEntity.ok(response);
    }

    /**
     * Get count of templates shared with the current user.
     * For badge/counter display in navigation.
     *
     * @return Count object
     */
    @GetMapping("/shared-with-me/count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SharedWithMeCountDto> getSharedWithMeCount() {
        logger.info("GET /api/v1/tests/templates/shared-with-me/count");

        String clerkId = securityService.getAuthenticatedClerkId();
        if (clerkId == null) {
            logger.warn("No authenticated user for shared-with-me count request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        long count = templateShareService.countTemplatesSharedWithMe(clerkId);
        logger.debug("Shared templates count for user: {}", count);
        return ResponseEntity.ok(SharedWithMeCountDto.of(count));
    }

    // ==================== ANONYMOUS RESULTS (For Template Owners) ====================

    /**
     * Get anonymous session statistics for a template.
     *
     * @param templateId Template UUID
     * @return Anonymous session statistics
     */
    @GetMapping("/{templateId}/anonymous-stats")
    @PreAuthorize("@templateSecurity.canViewAnonymousResults(#templateId)")
    public ResponseEntity<AnonymousTestService.AnonymousSessionStats> getAnonymousStats(
            @PathVariable UUID templateId) {
        logger.info("GET /api/v1/tests/templates/{}/anonymous-stats", templateId);

        AnonymousTestService.AnonymousSessionStats stats =
                anonymousTestService.getSessionStats(templateId);
        return ResponseEntity.ok(stats);
    }

    /**
     * List anonymous results for a template.
     *
     * @param templateId Template UUID
     * @param pageable Pagination parameters
     * @return Page of anonymous result summaries
     */
    @GetMapping("/{templateId}/anonymous-results")
    @PreAuthorize("@templateSecurity.canViewAnonymousResults(#templateId)")
    public ResponseEntity<Page<AnonymousResultSummaryDto>> listAnonymousResults(
            @PathVariable UUID templateId,
            @PageableDefault(size = 20, sort = "completedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        logger.info("GET /api/v1/tests/templates/{}/anonymous-results", templateId);

        Page<AnonymousResultSummaryDto> results =
                anonymousTestService.listAnonymousResults(templateId, pageable);
        return ResponseEntity.ok(results);
    }

    /**
     * Get detailed anonymous result.
     *
     * @param templateId Template UUID (for authorization check)
     * @param resultId Result UUID
     * @return Detailed anonymous result
     */
    @GetMapping("/{templateId}/anonymous-results/{resultId}")
    @PreAuthorize("@templateSecurity.canViewAnonymousResults(#templateId)")
    public ResponseEntity<AnonymousResultDetailDto> getAnonymousResultDetail(
            @PathVariable UUID templateId,
            @PathVariable UUID resultId) {
        logger.info("GET /api/v1/tests/templates/{}/anonymous-results/{}", templateId, resultId);

        AnonymousResultDetailDto result =
                anonymousTestService.getAnonymousResultDetail(resultId);
        return ResponseEntity.ok(result);
    }
}
