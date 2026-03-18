package app.skillsoft.assessmentbackend.domain.dto.export;

import app.skillsoft.assessmentbackend.domain.entities.ExportStatus;
import java.util.UUID;

public record PdfExportCreatedResponse(
    UUID exportId,
    ExportStatus status
) {}
