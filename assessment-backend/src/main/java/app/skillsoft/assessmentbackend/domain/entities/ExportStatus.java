package app.skillsoft.assessmentbackend.domain.entities;

public enum ExportStatus {
    QUEUED,
    GENERATING,
    COMPLETED,
    FAILED,
    CANCELLED,
    EXPIRED
}
