-- Performance indexes for TestTemplate listing and deletion preview queries
-- V27: Add composite indexes for common query patterns

-- Supports: listMyTemplates (owner_id + active + soft-delete filter)
CREATE INDEX IF NOT EXISTS idx_test_template_owner_active
    ON test_templates(owner_id, is_active)
    WHERE deleted_at IS NULL;

-- Supports: template share lookup queries (active shares only)
CREATE INDEX IF NOT EXISTS idx_template_share_template_active
    ON template_shares(template_id)
    WHERE revoked_at IS NULL;

-- Supports: session count queries for deletion preview (avoids loading all sessions)
CREATE INDEX IF NOT EXISTS idx_test_session_template_status
    ON test_sessions(template_id, status);
