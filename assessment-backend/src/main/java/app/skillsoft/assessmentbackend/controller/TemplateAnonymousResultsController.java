package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.dto.AnonymousResultDetailDto;
import app.skillsoft.assessmentbackend.domain.dto.AnonymousResultSummaryDto;
import app.skillsoft.assessmentbackend.services.AnonymousTestService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for Template Anonymous Results operations.
 *
 * Provides endpoints for template owners to view anonymous session statistics
 * and detailed results for their templates.
 *
 * Security:
 * - All endpoints require canViewAnonymousResults permission
 *
 * API Base Path: /api/v1/tests/templates
 */
@RestController
@RequestMapping("/api/v1/tests/templates")
@RequiredArgsConstructor
public class TemplateAnonymousResultsController {

    private static final Logger logger = LoggerFactory.getLogger(TemplateAnonymousResultsController.class);

    private final AnonymousTestService anonymousTestService;

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
