package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.dto.*;
import app.skillsoft.assessmentbackend.domain.entities.DeletionMode;
import app.skillsoft.assessmentbackend.services.TemplateDeletionService;
import app.skillsoft.assessmentbackend.services.TestTemplateService;
import app.skillsoft.assessmentbackend.services.security.TemplateSecurityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
 * Related controllers:
 * - {@link TemplateVisibilityController} - Visibility operations
 * - {@link TemplateDeletionController} - Safe deletion and restore operations
 * - {@link TemplateAnonymousResultsController} - Anonymous results viewing
 * - {@link TemplateSharingController} - Shared-with-me operations
 *
 * API Base Path: /api/v1/tests/templates
 */
@RestController
@RequestMapping("/api/v1/tests/templates")
@RequiredArgsConstructor
public class TestTemplateController {

    private static final Logger logger = LoggerFactory.getLogger(TestTemplateController.class);

    private final TestTemplateService testTemplateService;
    private final TemplateSecurityService securityService;
    private final TemplateDeletionService deletionService;

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

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).mustRevalidate())
                .body(templates);
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

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).mustRevalidate())
                .body(templates);
    }

    /**
     * List active templates owned by the current user.
     * Used for personal mode catalog -- shows only templates the user created.
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
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).mustRevalidate())
                .body(templates);
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
                    return ResponseEntity.ok()
                            .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).mustRevalidate())
                            .body(template);
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

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).mustRevalidate())
                .body(templates);
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

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).mustRevalidate())
                .body(templates);
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

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePrivate())
                .body(stats);
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

        TestTemplateDto created = testTemplateService.createTemplate(request);
        logger.info("Created template with id: {}", created.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
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

        TestTemplateDto updated = testTemplateService.updateTemplate(id, request);
        logger.info("Updated template: {}", updated.name());
        return ResponseEntity.ok(updated);
    }

    /**
     * Clone an existing test template.
     * Creates a deep copy with "Copy of" prefix and DRAFT status.
     *
     * @param id Template UUID to clone
     * @return Cloned template with 201 status
     */
    @PostMapping("/{id}/clone")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<TestTemplateDto> cloneTemplate(@PathVariable UUID id) {
        logger.info("POST /api/v1/tests/templates/{}/clone", id);

        TestTemplateDto cloned = testTemplateService.cloneTemplate(id);
        logger.info("Cloned template {} -> new template {}", id, cloned.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(cloned);
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

        DeletionResultDto result = deletionService.deleteTemplate(id, DeletionMode.SOFT_DELETE, true);
        if (result.success()) {
            logger.info("Soft-deleted template: {}", id);
            return ResponseEntity.noContent().build();
        } else {
            logger.warn("Failed to delete template {}: {}", id, result.message());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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

        TestTemplateDto activated = testTemplateService.activateTemplate(id);
        logger.info("Activated template: {}", activated.name());
        return ResponseEntity.ok(activated);
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

        TestTemplateDto deactivated = testTemplateService.deactivateTemplate(id);
        logger.info("Deactivated template: {}", deactivated.name());
        return ResponseEntity.ok(deactivated);
    }
}
