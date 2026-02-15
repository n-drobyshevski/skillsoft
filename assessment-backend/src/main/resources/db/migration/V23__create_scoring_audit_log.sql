-- V23: Create scoring audit log table for score traceability
-- Stores a snapshot of all scoring inputs/outputs for compliance and debugging.

CREATE TABLE IF NOT EXISTS scoring_audit_logs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id          UUID NOT NULL,
    result_id           UUID NOT NULL,
    clerk_user_id       VARCHAR(255),
    template_id         UUID NOT NULL,
    goal                VARCHAR(50) NOT NULL,
    strategy_class      VARCHAR(255) NOT NULL,
    overall_score       DOUBLE PRECISION,
    overall_percentage  DOUBLE PRECISION,
    passed              BOOLEAN,
    percentile          INTEGER,
    indicator_weights   JSONB,
    competency_breakdown JSONB,
    config_snapshot     JSONB,
    total_answers       INTEGER,
    answered_count      INTEGER,
    skipped_count       INTEGER,
    scoring_duration_ms BIGINT,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Index for looking up audit by session (most common query)
CREATE INDEX idx_scoring_audit_session ON scoring_audit_logs (session_id);

-- Index for looking up audit by result
CREATE INDEX idx_scoring_audit_result ON scoring_audit_logs (result_id);

-- Index for user-level audit trail
CREATE INDEX idx_scoring_audit_user ON scoring_audit_logs (clerk_user_id) WHERE clerk_user_id IS NOT NULL;

-- Index for date-range compliance queries
CREATE INDEX idx_scoring_audit_created ON scoring_audit_logs (created_at);

-- GIN index for JSONB querying on competency breakdown
CREATE INDEX idx_scoring_audit_competency_breakdown ON scoring_audit_logs USING GIN (competency_breakdown);
