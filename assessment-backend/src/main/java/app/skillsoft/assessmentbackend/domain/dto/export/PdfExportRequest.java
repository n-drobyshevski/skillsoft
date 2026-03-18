package app.skillsoft.assessmentbackend.domain.dto.export;

import app.skillsoft.assessmentbackend.domain.entities.ExportFormat;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record PdfExportRequest(
    @NotNull(message = "Result ID is required")
    UUID resultId,

    @NotNull(message = "Export format is required")
    ExportFormat format,

    String locale,

    List<String> sections
) {
    public PdfExportRequest {
        if (locale == null || locale.isBlank()) {
            locale = "en";
        }
    }
}
