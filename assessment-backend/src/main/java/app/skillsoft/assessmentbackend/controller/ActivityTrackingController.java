package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.dto.activity.ActivityFilterParams;
import app.skillsoft.assessmentbackend.domain.dto.activity.TemplateActivityStatsDto;
import app.skillsoft.assessmentbackend.domain.dto.activity.TestActivityDto;
import app.skillsoft.assessmentbackend.domain.entities.SessionStatus;
import app.skillsoft.assessmentbackend.services.ActivityTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Activity Tracking.
 *
 * Provides endpoints for:
 * - Dashboard recent activity widget
 * - Template-specific activity tracking
 * - Activity statistics
 *
 * All endpoints require ADMIN or EDITOR role.
 *
 * API Base Path: /api/v1/tests/activity
 */
@RestController
@RequestMapping("/api/v1/tests/activity")
@PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
@Tag(name = "Activity Tracking", description = "Endpoints for test activity monitoring (ADMIN/EDITOR only)")
public class ActivityTrackingController {

    private static final Logger logger = LoggerFactory.getLogger(ActivityTrackingController.class);

    private static final int DEFAULT_RECENT_LIMIT = 10;
    private static final int MAX_RECENT_LIMIT = 50;

    private final ActivityTrackingService activityService;

    public ActivityTrackingController(ActivityTrackingService activityService) {
        this.activityService = activityService;
    }

    // ==================== DASHBOARD ENDPOINTS ====================

    /**
     * Get recent activity for dashboard widget.
     * Returns recent test completions, abandonments, and timeouts.
     *
     * @param limit Maximum number of activities to return (default: 10, max: 50)
     * @return List of recent activities ordered by time (most recent first)
     */
    @GetMapping("/recent")
    @Operation(
            summary = "Get recent activity",
            description = "Returns recent test activities for the dashboard widget. Includes user name enrichment."
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved recent activities")
    @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN or EDITOR role")
    public ResponseEntity<List<TestActivityDto>> getRecentActivity(
            @Parameter(description = "Maximum number of activities to return")
            @RequestParam(defaultValue = "10") int limit) {

        logger.debug("GET /api/v1/tests/activity/recent - limit: {}", limit);

        // Validate and cap limit
        int validatedLimit = Math.min(Math.max(limit, 1), MAX_RECENT_LIMIT);

        List<TestActivityDto> activities = activityService.getRecentActivity(validatedLimit);
        logger.debug("Returning {} recent activities", activities.size());

        return ResponseEntity.ok(activities);
    }

    // ==================== TEMPLATE ACTIVITY ENDPOINTS ====================

    /**
     * Get activity for a specific template with filtering and pagination.
     *
     * @param templateId The template ID
     * @param status     Optional filter by session status (COMPLETED, ABANDONED, TIMED_OUT)
     * @param passed     Optional filter by pass/fail (true/false)
     * @param from       Optional start date for date range filter
     * @param to         Optional end date for date range filter
     * @param page       Page number (0-indexed)
     * @param size       Page size (default: 20, max: 100)
     * @return Paginated list of activities for the template
     */
    @GetMapping("/template/{templateId}")
    @Operation(
            summary = "Get template activity",
            description = "Returns paginated test activities for a specific template with optional filtering."
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved template activities")
    @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN or EDITOR role")
    @ApiResponse(responseCode = "404", description = "Template not found")
    public ResponseEntity<Page<TestActivityDto>> getTemplateActivity(
            @PathVariable UUID templateId,
            @Parameter(description = "Filter by session status")
            @RequestParam(required = false) SessionStatus status,
            @Parameter(description = "Filter by pass/fail result")
            @RequestParam(required = false) Boolean passed,
            @Parameter(description = "Start date for date range filter (ISO format)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "End date for date range filter (ISO format)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default: 20, max: 100)")
            @RequestParam(defaultValue = "20") int size) {

        logger.debug("GET /api/v1/tests/activity/template/{} - status: {}, passed: {}, page: {}, size: {}",
                templateId, status, passed, page, size);

        ActivityFilterParams params = ActivityFilterParams.forTemplate(
                templateId, status, passed, from, to, page, size);

        Page<TestActivityDto> activities = activityService.getTemplateActivity(templateId, params);
        logger.debug("Returning {} activities for template {} (page {} of {})",
                activities.getNumberOfElements(), templateId, page, activities.getTotalPages());

        return ResponseEntity.ok(activities);
    }

    /**
     * Get aggregated activity statistics for a template.
     *
     * @param templateId The template ID
     * @return Aggregated statistics including completion rate, pass rate, average score
     */
    @GetMapping("/template/{templateId}/stats")
    @Operation(
            summary = "Get template activity statistics",
            description = "Returns aggregated activity statistics for a specific template."
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved template statistics")
    @ApiResponse(responseCode = "403", description = "Forbidden - requires ADMIN or EDITOR role")
    @ApiResponse(responseCode = "404", description = "Template not found")
    public ResponseEntity<TemplateActivityStatsDto> getTemplateActivityStats(
            @PathVariable UUID templateId) {

        logger.debug("GET /api/v1/tests/activity/template/{}/stats", templateId);

        TemplateActivityStatsDto stats = activityService.getTemplateActivityStats(templateId);

        if (stats == null) {
            logger.warn("Template {} not found", templateId);
            return ResponseEntity.notFound().build();
        }

        logger.debug("Returning stats for template {}: {} total sessions, {}% completion rate",
                templateId, stats.totalSessions(), stats.completionRate());

        return ResponseEntity.ok(stats);
    }
}
