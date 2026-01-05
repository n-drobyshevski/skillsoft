package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.dto.sharing.*;
import app.skillsoft.assessmentbackend.domain.entities.SharePermission;
import app.skillsoft.assessmentbackend.services.security.TemplateSecurityService;
import app.skillsoft.assessmentbackend.services.sharing.TemplateShareService;
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
 * REST Controller for Template Sharing operations.
 *
 * Provides endpoints for sharing templates with users and teams,
 * managing share permissions, and bulk sharing operations.
 *
 * Security:
 * - All endpoints require authentication
 * - Share operations require MANAGE permission or ownership
 * - View operations require at least VIEW permission
 *
 * API Base Path: /api/v1/tests/templates/{templateId}/shares
 */
@RestController
@RequestMapping("/api/v1/tests/templates/{templateId}/shares")
public class TemplateShareController {

    private static final Logger logger = LoggerFactory.getLogger(TemplateShareController.class);

    private final TemplateShareService shareService;
    private final TemplateSecurityService securityService;

    public TemplateShareController(TemplateShareService shareService,
                                   TemplateSecurityService securityService) {
        this.shareService = shareService;
        this.securityService = securityService;
    }

    // ==================== READ OPERATIONS ====================

    /**
     * List all active shares for a template.
     *
     * @param templateId Template UUID
     * @return List of shares (users and teams)
     */
    @GetMapping
    @PreAuthorize("@templateSecurity.canAccess(#templateId, T(app.skillsoft.assessmentbackend.domain.entities.SharePermission).VIEW)")
    public ResponseEntity<List<TemplateShareDto>> listShares(@PathVariable UUID templateId) {
        logger.info("GET /api/v1/tests/templates/{}/shares - Listing shares", templateId);

        List<TemplateShareDto> shares = shareService.listShares(templateId);
        logger.info("Found {} shares for template {}", shares.size(), templateId);

        return ResponseEntity.ok(shares);
    }

    /**
     * List only user shares for a template.
     *
     * @param templateId Template UUID
     * @return List of user shares
     */
    @GetMapping("/users")
    @PreAuthorize("@templateSecurity.canAccess(#templateId, T(app.skillsoft.assessmentbackend.domain.entities.SharePermission).VIEW)")
    public ResponseEntity<List<TemplateShareDto>> listUserShares(@PathVariable UUID templateId) {
        logger.info("GET /api/v1/tests/templates/{}/shares/users - Listing user shares", templateId);

        List<TemplateShareDto> shares = shareService.listUserShares(templateId);
        logger.info("Found {} user shares for template {}", shares.size(), templateId);

        return ResponseEntity.ok(shares);
    }

    /**
     * List only team shares for a template.
     *
     * @param templateId Template UUID
     * @return List of team shares
     */
    @GetMapping("/teams")
    @PreAuthorize("@templateSecurity.canAccess(#templateId, T(app.skillsoft.assessmentbackend.domain.entities.SharePermission).VIEW)")
    public ResponseEntity<List<TemplateShareDto>> listTeamShares(@PathVariable UUID templateId) {
        logger.info("GET /api/v1/tests/templates/{}/shares/teams - Listing team shares", templateId);

        List<TemplateShareDto> shares = shareService.listTeamShares(templateId);
        logger.info("Found {} team shares for template {}", shares.size(), templateId);

        return ResponseEntity.ok(shares);
    }

    /**
     * Get a specific share by ID.
     *
     * @param templateId Template UUID
     * @param shareId Share UUID
     * @return Share details
     */
    @GetMapping("/{shareId}")
    @PreAuthorize("@templateSecurity.canAccess(#templateId, T(app.skillsoft.assessmentbackend.domain.entities.SharePermission).VIEW)")
    public ResponseEntity<TemplateShareDto> getShare(
            @PathVariable UUID templateId,
            @PathVariable UUID shareId) {
        logger.info("GET /api/v1/tests/templates/{}/shares/{}", templateId, shareId);

        try {
            TemplateShareDto share = shareService.getShareById(shareId);

            // Verify share belongs to this template
            if (!share.templateId().equals(templateId)) {
                logger.warn("Share {} does not belong to template {}", shareId, templateId);
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(share);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                logger.warn("Share not found: {}", shareId);
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }

    // ==================== WRITE OPERATIONS ====================

    /**
     * Share a template with a user.
     *
     * @param templateId Template UUID
     * @param request Share request with userId, permission, optional expiresAt
     * @return Created/updated share with 201 status
     */
    @PostMapping("/users")
    @PreAuthorize("@templateSecurity.canManageSharing(#templateId)")
    public ResponseEntity<TemplateShareDto> shareWithUser(
            @PathVariable UUID templateId,
            @Valid @RequestBody ShareUserRequest request) {
        logger.info("POST /api/v1/tests/templates/{}/shares/users - Sharing with user {}",
                templateId, request.userId());

        String clerkId = securityService.getAuthenticatedClerkId();
        if (clerkId == null) {
            logger.warn("No authenticated user for share operation");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            TemplateShareDto share = shareService.shareWithUser(
                    templateId,
                    request.userId(),
                    request.permission(),
                    request.expiresAt(),
                    clerkId
            );

            logger.info("Created/updated share {} for user {}", share.id(), request.userId());
            return ResponseEntity.status(HttpStatus.CREATED).body(share);

        } catch (IllegalStateException e) {
            logger.warn("Share validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                logger.warn("Resource not found: {}", e.getMessage());
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }

    /**
     * Share a template with a team.
     *
     * @param templateId Template UUID
     * @param request Share request with teamId, permission, optional expiresAt
     * @return Created/updated share with 201 status
     */
    @PostMapping("/teams")
    @PreAuthorize("@templateSecurity.canManageSharing(#templateId)")
    public ResponseEntity<TemplateShareDto> shareWithTeam(
            @PathVariable UUID templateId,
            @Valid @RequestBody ShareTeamRequest request) {
        logger.info("POST /api/v1/tests/templates/{}/shares/teams - Sharing with team {}",
                templateId, request.teamId());

        String clerkId = securityService.getAuthenticatedClerkId();
        if (clerkId == null) {
            logger.warn("No authenticated user for share operation");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            TemplateShareDto share = shareService.shareWithTeam(
                    templateId,
                    request.teamId(),
                    request.permission(),
                    request.expiresAt(),
                    clerkId
            );

            logger.info("Created/updated share {} for team {}", share.id(), request.teamId());
            return ResponseEntity.status(HttpStatus.CREATED).body(share);

        } catch (IllegalStateException e) {
            logger.warn("Share validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                logger.warn("Resource not found: {}", e.getMessage());
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }

    /**
     * Bulk share a template with multiple users and/or teams.
     *
     * Uses partial success pattern - attempts all shares and reports results.
     *
     * @param templateId Template UUID
     * @param request Bulk share request with lists of user and team shares
     * @return Bulk response with success/failure counts
     */
    @PostMapping("/bulk")
    @PreAuthorize("@templateSecurity.canManageSharing(#templateId)")
    public ResponseEntity<BulkShareResponse> bulkShare(
            @PathVariable UUID templateId,
            @Valid @RequestBody BulkShareRequest request) {
        logger.info("POST /api/v1/tests/templates/{}/shares/bulk - Bulk sharing with {} users, {} teams",
                templateId, request.userShares().size(), request.teamShares().size());

        String clerkId = securityService.getAuthenticatedClerkId();
        if (clerkId == null) {
            logger.warn("No authenticated user for bulk share operation");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            BulkShareResponse response = shareService.bulkShare(templateId, request, clerkId);

            logger.info("Bulk share completed: {} created, {} updated, {} failed",
                    response.createdCount(), response.updatedCount(), response.failedCount());

            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            logger.warn("Bulk share validation failed: {}", e.getMessage());
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
     * Update an existing share's permission or expiration.
     *
     * @param templateId Template UUID
     * @param shareId Share UUID to update
     * @param request Update request with new permission and/or expiresAt
     * @return Updated share
     */
    @PutMapping("/{shareId}")
    @PreAuthorize("@templateSecurity.canManageSharing(#templateId)")
    public ResponseEntity<TemplateShareDto> updateShare(
            @PathVariable UUID templateId,
            @PathVariable UUID shareId,
            @Valid @RequestBody UpdateShareRequest request) {
        logger.info("PUT /api/v1/tests/templates/{}/shares/{} - Updating share", templateId, shareId);

        try {
            // First verify share belongs to template
            TemplateShareDto existing = shareService.getShareById(shareId);
            if (!existing.templateId().equals(templateId)) {
                logger.warn("Share {} does not belong to template {}", shareId, templateId);
                return ResponseEntity.notFound().build();
            }

            TemplateShareDto updated = shareService.updateShare(
                    shareId,
                    request.permission(),
                    request.expiresAt()
            );

            logger.info("Updated share {}", shareId);
            return ResponseEntity.ok(updated);

        } catch (IllegalStateException e) {
            logger.warn("Share update failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                logger.warn("Share not found: {}", shareId);
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }

    /**
     * Revoke (soft delete) a share.
     *
     * @param templateId Template UUID
     * @param shareId Share UUID to revoke
     * @return 204 No Content on success
     */
    @DeleteMapping("/{shareId}")
    @PreAuthorize("@templateSecurity.canManageSharing(#templateId)")
    public ResponseEntity<Void> revokeShare(
            @PathVariable UUID templateId,
            @PathVariable UUID shareId) {
        logger.info("DELETE /api/v1/tests/templates/{}/shares/{} - Revoking share", templateId, shareId);

        try {
            // First verify share belongs to template
            TemplateShareDto existing = shareService.getShareById(shareId);
            if (!existing.templateId().equals(templateId)) {
                logger.warn("Share {} does not belong to template {}", shareId, templateId);
                return ResponseEntity.notFound().build();
            }

            shareService.revokeShare(shareId);
            logger.info("Revoked share {}", shareId);

            return ResponseEntity.noContent().build();

        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                logger.warn("Share not found: {}", shareId);
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }

    // ==================== UTILITY ENDPOINTS ====================

    /**
     * Check if the current user can grant a specific permission.
     *
     * @param templateId Template UUID
     * @param permission Permission level to check
     * @return Boolean indicating if the permission can be granted
     */
    @GetMapping("/can-grant")
    @PreAuthorize("@templateSecurity.canAccess(#templateId, T(app.skillsoft.assessmentbackend.domain.entities.SharePermission).VIEW)")
    public ResponseEntity<Boolean> canGrantPermission(
            @PathVariable UUID templateId,
            @RequestParam SharePermission permission) {
        logger.info("GET /api/v1/tests/templates/{}/shares/can-grant?permission={}", templateId, permission);

        String clerkId = securityService.getAuthenticatedClerkId();
        if (clerkId == null) {
            return ResponseEntity.ok(false);
        }

        boolean canGrant = shareService.canGrantPermission(templateId, clerkId, permission);
        return ResponseEntity.ok(canGrant);
    }

    /**
     * Get the count of active shares for a template.
     *
     * @param templateId Template UUID
     * @return Count of active shares
     */
    @GetMapping("/count")
    @PreAuthorize("@templateSecurity.canAccess(#templateId, T(app.skillsoft.assessmentbackend.domain.entities.SharePermission).VIEW)")
    public ResponseEntity<Long> countShares(@PathVariable UUID templateId) {
        logger.info("GET /api/v1/tests/templates/{}/shares/count", templateId);

        long count = shareService.countShares(templateId);
        return ResponseEntity.ok(count);
    }
}
