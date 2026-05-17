package app.skillsoft.assessmentbackend.domain.dto.export;

import app.skillsoft.assessmentbackend.domain.entities.ExportStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record PdfExportStatusResponse(
    UUID exportId,
    ExportStatus status,
    Long fileSizeBytes,
    LocalDateTime createdAt
) {}
