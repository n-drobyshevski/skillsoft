-- V35: Create pdf_export_jobs table for async PDF generation tracking

CREATE TABLE pdf_export_jobs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    result_id       UUID NOT NULL,
    clerk_user_id   VARCHAR(100) NOT NULL,
    format          VARCHAR(30) NOT NULL,
    locale          VARCHAR(5) NOT NULL DEFAULT 'en',
    sections        VARCHAR(500),
    status          VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    file_path       VARCHAR(500),
    file_size_bytes BIGINT,
    error_message   VARCHAR(2000),
    content_hash    VARCHAR(64),
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    started_at      TIMESTAMP,
    completed_at    TIMESTAMP,
    failed_at       TIMESTAMP,
    expires_at      TIMESTAMP
);

CREATE INDEX idx_pdf_export_clerk_user_id ON pdf_export_jobs (clerk_user_id);
CREATE INDEX idx_pdf_export_result_id ON pdf_export_jobs (result_id);
CREATE INDEX idx_pdf_export_content_hash ON pdf_export_jobs (content_hash);
CREATE INDEX idx_pdf_export_status ON pdf_export_jobs (status);
CREATE INDEX idx_pdf_export_expires_at ON pdf_export_jobs (expires_at);
