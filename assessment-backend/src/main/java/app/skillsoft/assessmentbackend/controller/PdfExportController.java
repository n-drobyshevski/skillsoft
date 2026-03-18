package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.dto.export.PdfExportCreatedResponse;
import app.skillsoft.assessmentbackend.domain.dto.export.PdfExportRequest;
import app.skillsoft.assessmentbackend.domain.dto.export.PdfExportStatusResponse;
import app.skillsoft.assessmentbackend.security.SessionSecurityService;
import app.skillsoft.assessmentbackend.services.export.PdfExportService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/exports")
public class PdfExportController {

    private static final Logger logger = LoggerFactory.getLogger(PdfExportController.class);

    private final PdfExportService exportService;
    private final SessionSecurityService sessionSecurity;

    public PdfExportController(PdfExportService exportService,
                               SessionSecurityService sessionSecurity) {
        this.exportService = exportService;
        this.sessionSecurity = sessionSecurity;
    }

    @PostMapping("/pdf")
    public ResponseEntity<PdfExportCreatedResponse> createExport(
            @Valid @RequestBody PdfExportRequest request) {
        String clerkUserId = sessionSecurity.getAuthenticatedUserId();
        logger.info("POST /api/v1/exports/pdf - resultId={}, format={}, user={}",
            request.resultId(), request.format(), clerkUserId);
        PdfExportCreatedResponse response = exportService.requestExport(request, clerkUserId);
        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/{exportId}/status")
    public ResponseEntity<PdfExportStatusResponse> getStatus(@PathVariable UUID exportId) {
        logger.info("GET /api/v1/exports/{}/status", exportId);
        PdfExportStatusResponse response = exportService.getStatus(exportId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{exportId}/download")
    public ResponseEntity<byte[]> download(@PathVariable UUID exportId) {
        logger.info("GET /api/v1/exports/{}/download", exportId);
        byte[] pdfBytes = exportService.download(exportId);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"export-" + exportId + ".pdf\"")
            .contentType(MediaType.APPLICATION_PDF)
            .contentLength(pdfBytes.length)
            .body(pdfBytes);
    }
}
