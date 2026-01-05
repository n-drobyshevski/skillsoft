package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.dto.sharing.CreateShareLinkRequest;
import app.skillsoft.assessmentbackend.domain.dto.sharing.LinkValidationResult;
import app.skillsoft.assessmentbackend.domain.dto.sharing.ShareLinkDto;
import app.skillsoft.assessmentbackend.services.security.TemplateSecurityService;
import app.skillsoft.assessmentbackend.services.sharing.TemplateShareLinkService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Template Share Link operations.
 *
 * Provides endpoints for creating and managing token-based share links
 * that allow access to templates (including anonymous access for LINK visibility).
 *
 * Security:
 * - Link validation is PUBLIC (no auth required)
 * - Link creation/revocation requires MANAGE permission or ownership
 * - Listing links requires VIEW permission
 *
 * API Base Path: /api/v1/tests/templates/{templateId}/links
 *              : /api/v1/tests/templates/validate-link (public endpoint)
 */
@RestController
@RequestMapping("/api/v1/tests/templates")
public class TemplateShareLinkController {

    private static final Logger logger = LoggerFactory.getLogger(TemplateShareLinkController.class);

    private final TemplateShareLinkService linkService;
    private final TemplateSecurityService securityService;

    public TemplateShareLinkController(TemplateShareLinkService linkService,
                                       TemplateSecurityService securityService) {
        this.linkService = linkService;
        this.securityService = securityService;
    }

    // ==================== PUBLIC ENDPOINTS ====================

    /**
     * Validate a share link token.
     *
     * This is a PUBLIC endpoint - no authentication required.
     * Used by the frontend to validate a link before showing template content.
     *
     * @param token The share link token to validate
     * @return Validation result with template info if valid, or reason if invalid
     */
    @GetMapping("/validate-link")
    public ResponseEntity<LinkValidationResult> validateLink(@RequestParam String token) {
        logger.info("GET /api/v1/tests/templates/validate-link - Validating link token");

        if (token == null || token.isBlank()) {
            logger.warn("Empty token provided for validation");
            return ResponseEntity.ok(LinkValidationResult.invalid("MISSING_TOKEN"));
        }

        LinkValidationResult result = linkService.validateLink(token);

        if (result.valid()) {
            logger.info("Link validated successfully for template {}", result.templateId());
            // Record usage after successful validation
            linkService.recordUsage(token);
        } else {
            logger.info("Link validation failed: {}", result.reason());
        }

        return ResponseEntity.ok(result);
    }

    // ==================== AUTHENTICATED LINK MANAGEMENT ====================

    /**
     * List all links for a template.
     *
     * Token visibility depends on requesting user:
     * - Full token visible to link creator
     * - Masked token for others
     *
     * @param templateId Template UUID
     * @return List of all links (including expired/revoked)
     */
    @GetMapping("/{templateId}/links")
    @PreAuthorize("@templateSecurity.canAccess(#templateId, T(app.skillsoft.assessmentbackend.domain.entities.SharePermission).VIEW)")
    public ResponseEntity<List<ShareLinkDto>> listLinks(@PathVariable UUID templateId) {
        logger.info("GET /api/v1/tests/templates/{}/links - Listing all links", templateId);

        String clerkId = securityService.getAuthenticatedClerkId();
        List<ShareLinkDto> links = linkService.listLinks(templateId, clerkId);

        logger.info("Found {} links for template {}", links.size(), templateId);
        return ResponseEntity.ok(links);
    }

    /**
     * List only active (valid) links for a template.
     *
     * Active means: not expired, not revoked, not used up.
     *
     * @param templateId Template UUID
     * @return List of active links
     */
    @GetMapping("/{templateId}/links/active")
    @PreAuthorize("@templateSecurity.canAccess(#templateId, T(app.skillsoft.assessmentbackend.domain.entities.SharePermission).VIEW)")
    public ResponseEntity<List<ShareLinkDto>> listActiveLinks(@PathVariable UUID templateId) {
        logger.info("GET /api/v1/tests/templates/{}/links/active - Listing active links", templateId);

        String clerkId = securityService.getAuthenticatedClerkId();
        List<ShareLinkDto> links = linkService.listActiveLinks(templateId, clerkId);

        logger.info("Found {} active links for template {}", links.size(), templateId);
        return ResponseEntity.ok(links);
    }

    /**
     * Get a specific link by ID.
     *
     * @param templateId Template UUID
     * @param linkId Link UUID
     * @return Link details
     */
    @GetMapping("/{templateId}/links/{linkId}")
    @PreAuthorize("@templateSecurity.canAccess(#templateId, T(app.skillsoft.assessmentbackend.domain.entities.SharePermission).VIEW)")
    public ResponseEntity<ShareLinkDto> getLink(
            @PathVariable UUID templateId,
            @PathVariable UUID linkId) {
        logger.info("GET /api/v1/tests/templates/{}/links/{}", templateId, linkId);

        String clerkId = securityService.getAuthenticatedClerkId();
        ShareLinkDto link = linkService.getLinkById(linkId, clerkId);

        if (link == null) {
            logger.warn("Link not found: {}", linkId);
            return ResponseEntity.notFound().build();
        }

        // Verify link belongs to this template
        if (!link.templateId().equals(templateId)) {
            logger.warn("Link {} does not belong to template {}", linkId, templateId);
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(link);
    }

    /**
     * Create a new share link for a template.
     *
     * Enforces maximum 10 active links per template.
     *
     * @param templateId Template UUID
     * @param request Link configuration
     * @return Created link with full token visible
     */
    @PostMapping("/{templateId}/links")
    @PreAuthorize("@templateSecurity.canCreateLinks(#templateId)")
    public ResponseEntity<ShareLinkDto> createLink(
            @PathVariable UUID templateId,
            @Valid @RequestBody CreateShareLinkRequest request) {
        logger.info("POST /api/v1/tests/templates/{}/links - Creating new link", templateId);

        String clerkId = securityService.getAuthenticatedClerkId();
        if (clerkId == null) {
            logger.warn("No authenticated user for link creation");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Check link limit
        if (!linkService.canCreateLink(templateId)) {
            logger.warn("Maximum link limit reached for template {}", templateId);
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        try {
            ShareLinkDto link = linkService.createLink(templateId, request, clerkId);

            logger.info("Created link {} for template {}", link.id(), templateId);
            return ResponseEntity.status(HttpStatus.CREATED).body(link);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid link request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            logger.warn("Link creation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                logger.warn("Template not found: {}", templateId);
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }

    /**
     * Revoke (soft delete) a share link.
     *
     * @param templateId Template UUID
     * @param linkId Link UUID to revoke
     * @return 204 No Content on success
     */
    @DeleteMapping("/{templateId}/links/{linkId}")
    @PreAuthorize("@templateSecurity.canManageSharing(#templateId)")
    public ResponseEntity<Void> revokeLink(
            @PathVariable UUID templateId,
            @PathVariable UUID linkId) {
        logger.info("DELETE /api/v1/tests/templates/{}/links/{} - Revoking link", templateId, linkId);

        try {
            // First verify link belongs to template
            String clerkId = securityService.getAuthenticatedClerkId();
            ShareLinkDto existing = linkService.getLinkById(linkId, clerkId);

            if (existing == null) {
                logger.warn("Link not found: {}", linkId);
                return ResponseEntity.notFound().build();
            }

            if (!existing.templateId().equals(templateId)) {
                logger.warn("Link {} does not belong to template {}", linkId, templateId);
                return ResponseEntity.notFound().build();
            }

            linkService.revokeLink(linkId);
            logger.info("Revoked link {}", linkId);

            return ResponseEntity.noContent().build();

        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                logger.warn("Link not found: {}", linkId);
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }

    /**
     * Revoke all active links for a template.
     *
     * Used when archiving a template or changing visibility away from LINK.
     *
     * @param templateId Template UUID
     * @return Number of links revoked
     */
    @DeleteMapping("/{templateId}/links")
    @PreAuthorize("@templateSecurity.canManageSharing(#templateId)")
    public ResponseEntity<Integer> revokeAllLinks(@PathVariable UUID templateId) {
        logger.info("DELETE /api/v1/tests/templates/{}/links - Revoking all links", templateId);

        int revokedCount = linkService.revokeAllLinks(templateId);
        logger.info("Revoked {} links for template {}", revokedCount, templateId);

        return ResponseEntity.ok(revokedCount);
    }

    // ==================== UTILITY ENDPOINTS ====================

    /**
     * Check if a new link can be created for a template.
     *
     * Returns true if under the limit of 10 active links.
     *
     * @param templateId Template UUID
     * @return Boolean indicating if link creation is allowed
     */
    @GetMapping("/{templateId}/links/can-create")
    @PreAuthorize("@templateSecurity.canAccess(#templateId, T(app.skillsoft.assessmentbackend.domain.entities.SharePermission).VIEW)")
    public ResponseEntity<Boolean> canCreateLink(@PathVariable UUID templateId) {
        logger.info("GET /api/v1/tests/templates/{}/links/can-create", templateId);

        boolean canCreate = linkService.canCreateLink(templateId);
        return ResponseEntity.ok(canCreate);
    }

    /**
     * Get the count of active links for a template.
     *
     * @param templateId Template UUID
     * @return Count of active links
     */
    @GetMapping("/{templateId}/links/count")
    @PreAuthorize("@templateSecurity.canAccess(#templateId, T(app.skillsoft.assessmentbackend.domain.entities.SharePermission).VIEW)")
    public ResponseEntity<LinkCountResponse> countLinks(@PathVariable UUID templateId) {
        logger.info("GET /api/v1/tests/templates/{}/links/count", templateId);

        long activeCount = linkService.countActiveLinks(templateId);
        int maxLinks = linkService.getMaxLinksPerTemplate();

        return ResponseEntity.ok(new LinkCountResponse(activeCount, maxLinks));
    }

    /**
     * Response DTO for link count endpoint.
     */
    public record LinkCountResponse(
            long activeCount,
            int maxLinks
    ) {}
}
