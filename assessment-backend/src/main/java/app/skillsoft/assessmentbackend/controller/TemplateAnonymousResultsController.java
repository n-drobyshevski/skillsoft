package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.dto.AnonymousResultDetailDto;
import app.skillsoft.assessmentbackend.domain.dto.AnonymousResultSummaryDto;
import app.skillsoft.assessmentbackend.services.AnonymousResultExportService;
import app.skillsoft.assessmentbackend.services.AnonymousTestService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.LocalDateTime;
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
    private final AnonymousResultExportService exportService;

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
     * List anonymous results for a template with optional filters.
     *
     * @param templateId Template UUID
     * @param dateFrom Filter: start date (inclusive, ISO format)
     * @param dateTo Filter: end date (inclusive, ISO format)
     * @param minScore Filter: minimum score percentage
     * @param maxScore Filter: maximum score percentage
     * @param passed Filter: pass/fail status (true/false)
     * @param shareLinkId Filter: specific share link UUID
     * @param pageable Pagination parameters
     * @return Page of anonymous result summaries
     */
    @GetMapping("/{templateId}/anonymous-results")
    @PreAuthorize("@templateSecurity.canViewAnonymousResults(#templateId)")
    public ResponseEntity<Page<AnonymousResultSummaryDto>> listAnonymousResults(
            @PathVariable UUID templateId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @RequestParam(required = false) Double minScore,
            @RequestParam(required = false) Double maxScore,
            @RequestParam(required = false) Boolean passed,
            @RequestParam(required = false) UUID shareLinkId,
            @PageableDefault(size = 20, sort = "completedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        logger.info("GET /api/v1/tests/templates/{}/anonymous-results (filters: dateFrom={}, dateTo={}, minScore={}, maxScore={}, passed={}, shareLinkId={})",
                templateId, dateFrom, dateTo, minScore, maxScore, passed, shareLinkId);

        var filters = new AnonymousTestService.AnonymousResultFilter(
                dateFrom, dateTo, minScore, maxScore, passed, shareLinkId);

        Page<AnonymousResultSummaryDto> results =
                anonymousTestService.listAnonymousResults(templateId, filters, pageable);
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

    /**
     * Get per-link statistics for a specific share link.
     *
     * @param templateId Template UUID (for authorization check)
     * @param linkId Share link UUID
     * @return Statistics for the share link (totalSessions, completedResults, averageScore, passRate)
     */
    @GetMapping("/{templateId}/share-links/{linkId}/stats")
    @PreAuthorize("@templateSecurity.canViewAnonymousResults(#templateId)")
    public ResponseEntity<AnonymousTestService.ShareLinkResultStats> getShareLinkStats(
            @PathVariable UUID templateId,
            @PathVariable UUID linkId) {
        logger.info("GET /api/v1/tests/templates/{}/share-links/{}/stats", templateId, linkId);

        AnonymousTestService.ShareLinkResultStats stats =
                anonymousTestService.getShareLinkStats(linkId);
        return ResponseEntity.ok(stats);
    }

    // ==================== EXPORT ====================

    /**
     * Export anonymous results as CSV.
     *
     * @param templateId Template UUID
     * @return Streaming CSV response
     */
    @GetMapping("/{templateId}/anonymous-results/export")
    @PreAuthorize("@templateSecurity.canViewAnonymousResults(#templateId)")
    public ResponseEntity<StreamingResponseBody> exportAnonymousResults(
            @PathVariable UUID templateId) {
        logger.info("GET /api/v1/tests/templates/{}/anonymous-results/export", templateId);

        StreamingResponseBody body = outputStream -> exportService.exportCsv(templateId, outputStream);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"anonymous-results-" + templateId + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(body);
    }
}
