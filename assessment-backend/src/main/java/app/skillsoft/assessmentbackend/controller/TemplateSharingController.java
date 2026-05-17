package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.dto.sharing.SharedTemplatesResponseDto;
import app.skillsoft.assessmentbackend.domain.dto.sharing.SharedWithMeCountDto;
import app.skillsoft.assessmentbackend.services.security.TemplateSecurityService;
import app.skillsoft.assessmentbackend.services.sharing.TemplateShareService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for "Shared With Me" operations.
 *
 * Provides endpoints for viewing templates shared with the current user
 * and counting shared template notifications.
 *
 * Security:
 * - All endpoints require authentication
 *
 * API Base Path: /api/v1/tests/templates
 */
@RestController
@RequestMapping("/api/v1/tests/templates")
@RequiredArgsConstructor
public class TemplateSharingController {

    private static final Logger logger = LoggerFactory.getLogger(TemplateSharingController.class);

    private final TemplateShareService templateShareService;
    private final TemplateSecurityService securityService;

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
