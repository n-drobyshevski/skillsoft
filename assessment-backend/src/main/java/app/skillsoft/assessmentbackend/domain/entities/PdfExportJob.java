package app.skillsoft.assessmentbackend.domain.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pdf_export_jobs", indexes = {
    @Index(name = "idx_pdf_export_clerk_user_id", columnList = "clerk_user_id"),
    @Index(name = "idx_pdf_export_result_id", columnList = "result_id"),
    @Index(name = "idx_pdf_export_content_hash", columnList = "content_hash")
})
public class PdfExportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "result_id", nullable = false)
    private UUID resultId;

    @Column(name = "clerk_user_id", nullable = false)
    private String clerkUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false)
    private ExportFormat format;

    @Column(name = "locale", nullable = false, length = 5)
    private String locale;

    @Column(name = "sections")
    private String sections;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ExportStatus status = ExportStatus.QUEUED;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.expiresAt = this.createdAt.plusHours(24);
    }

    // All getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getResultId() { return resultId; }
    public void setResultId(UUID resultId) { this.resultId = resultId; }
    public String getClerkUserId() { return clerkUserId; }
    public void setClerkUserId(String clerkUserId) { this.clerkUserId = clerkUserId; }
    public ExportFormat getFormat() { return format; }
    public void setFormat(ExportFormat format) { this.format = format; }
    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }
    public String getSections() { return sections; }
    public void setSections(String sections) { this.sections = sections; }
    public ExportStatus getStatus() { return status; }
    public void setStatus(ExportStatus status) { this.status = status; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getFailedAt() { return failedAt; }
    public void setFailedAt(LocalDateTime failedAt) { this.failedAt = failedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
