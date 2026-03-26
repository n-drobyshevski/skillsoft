package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.dto.BulkDeleteResultDto;
import app.skillsoft.assessmentbackend.domain.dto.BulkDeleteSessionsRequest;
import app.skillsoft.assessmentbackend.services.TestSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for Test Session Deletion operations.
 *
 * Provides endpoints for single and bulk deletion of test sessions.
 * Cascade delete removes associated TestResult and TestAnswer entities.
 *
 * Security:
 * - All endpoints require ADMIN role
 *
 * NOTE: Shares base path /api/v1/tests/sessions with TestSessionController.
 * This controller handles only DELETE methods (admin deletion concern),
 * while TestSessionController handles the test-taking lifecycle (GET/POST/PUT).
 * Spring dispatches by HTTP method + path, so there is no conflict.
 *
 * API Base Path: /api/v1/tests/sessions
 */
@RestController
@RequestMapping("/api/v1/tests/sessions")
@RequiredArgsConstructor
public class SessionDeletionController {

    private static final Logger logger = LoggerFactory.getLogger(SessionDeletionController.class);

    private final TestSessionService testSessionService;

    @DeleteMapping("/{sessionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSession(@PathVariable UUID sessionId) {
        logger.info("DELETE /api/v1/tests/sessions/{}", sessionId);

        testSessionService.deleteSession(sessionId);

        logger.info("Successfully deleted session {}", sessionId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BulkDeleteResultDto> bulkDeleteSessions(
            @Valid @RequestBody BulkDeleteSessionsRequest request) {
        logger.info("DELETE /api/v1/tests/sessions/bulk - {} sessions requested", request.sessionIds().size());

        BulkDeleteResultDto result = testSessionService.bulkDeleteSessions(request.sessionIds());

        logger.info("Bulk delete result: {} deleted, {} failed", result.deleted(), result.failed());
        return ResponseEntity.ok(result);
    }
}
