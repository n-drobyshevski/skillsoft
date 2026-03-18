package app.skillsoft.assessmentbackend.services.export;

import app.skillsoft.assessmentbackend.domain.dto.export.PdfExportCreatedResponse;
import app.skillsoft.assessmentbackend.domain.dto.export.PdfExportRequest;
import app.skillsoft.assessmentbackend.domain.dto.export.PdfExportStatusResponse;

import java.util.UUID;

public interface PdfExportService {

    PdfExportCreatedResponse requestExport(PdfExportRequest request, String clerkUserId);

    PdfExportStatusResponse getStatus(UUID exportId);

    byte[] download(UUID exportId);
}
