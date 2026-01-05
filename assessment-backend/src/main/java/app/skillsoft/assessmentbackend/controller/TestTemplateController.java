package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.dto.*;
import app.skillsoft.assessmentbackend.domain.dto.sharing.ChangeVisibilityRequest;
import app.skillsoft.assessmentbackend.domain.dto.sharing.SharedTemplatesResponseDto;
import app.skillsoft.assessmentbackend.domain.dto.sharing.SharedWithMeCountDto;
import app.skillsoft.assessmentbackend.domain.dto.sharing.VisibilityInfoDto;
import app.skillsoft.assessmentbackend.domain.entities.TemplateVisibility;
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

    public TestTemplateController(TestTemplateService testTemplateService,
                                  TemplateVisibilityService visibilityService,
                                  TemplateSecurityService securityService,
                                  TemplateShareService templateShareService) {
        this.testTemplateService = testTemplateService;
        this.visibilityService = visibilityService;
        this.securityService = securityService;
        this.templateShareService = templateShareService;
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
     * Delete a test template.
     * 
     * @param id Template UUID to delete
     * @return 204 No Content or 404 if not found
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID id) {
        logger.info("DELETE /api/v1/tests/templates/{}", id);
        
        boolean deleted = testTemplateService.deleteTemplate(id);
        if (deleted) {
            logger.info("Deleted template: {}", id);
            return ResponseEntity.noContent().build();
        } else {
            logger.warn("Template not found for deletion: {}", id);
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
}
