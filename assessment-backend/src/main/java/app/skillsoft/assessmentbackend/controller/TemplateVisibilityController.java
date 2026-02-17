package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.dto.sharing.ChangeVisibilityRequest;
import app.skillsoft.assessmentbackend.domain.dto.sharing.VisibilityInfoDto;
import app.skillsoft.assessmentbackend.domain.entities.TemplateVisibility;
import app.skillsoft.assessmentbackend.services.security.TemplateSecurityService;
import app.skillsoft.assessmentbackend.services.sharing.TemplateVisibilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for Template Visibility operations.
 *
 * Provides endpoints for viewing and changing template visibility settings.
 *
 * Security:
 * - GET endpoints: Requires VIEW permission
 * - PATCH endpoint: Requires canChangeVisibility permission
 *
 * API Base Path: /api/v1/tests/templates
 */
@RestController
@RequestMapping("/api/v1/tests/templates")
@RequiredArgsConstructor
public class TemplateVisibilityController {

    private static final Logger logger = LoggerFactory.getLogger(TemplateVisibilityController.class);

    private final TemplateVisibilityService visibilityService;
    private final TemplateSecurityService securityService;

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
}
