-- V18: Create test_activity_events table for audit trail and activity tracking
-- This table stores activity events for test sessions to support:
-- 1. Dashboard activity widget (recent completions)
-- 2. Template activity page (per-template candidate tracking)
-- 3. Audit trail for compliance

CREATE TABLE test_activity_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES test_sessions(id) ON DELETE CASCADE,
    event_type VARCHAR(30) NOT NULL,
    clerk_user_id VARCHAR(100) NOT NULL,
    template_id UUID NOT NULL,
    event_timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb,

    CONSTRAINT chk_event_type CHECK (event_type IN ('SESSION_STARTED', 'SESSION_COMPLETED', 'SESSION_ABANDONED', 'SESSION_TIMED_OUT'))
);

-- Index for dashboard widget: recent activity across all templates
CREATE INDEX idx_activity_timestamp ON test_activity_events(event_timestamp DESC);

-- Index for template activity page: activity for a specific template
CREATE INDEX idx_activity_template ON test_activity_events(template_id);

-- Index for user activity: activity for a specific user
CREATE INDEX idx_activity_clerk_user ON test_activity_events(clerk_user_id);

-- Index for filtering by event type
CREATE INDEX idx_activity_event_type ON test_activity_events(event_type);

-- Composite index for template + timestamp (common query pattern)
CREATE INDEX idx_activity_template_timestamp ON test_activity_events(template_id, event_timestamp DESC);

-- Comment on table
COMMENT ON TABLE test_activity_events IS 'Audit trail for test session lifecycle events (started, completed, abandoned, timed out)';
COMMENT ON COLUMN test_activity_events.event_type IS 'Type of activity event: SESSION_STARTED, SESSION_COMPLETED, SESSION_ABANDONED, SESSION_TIMED_OUT';
COMMENT ON COLUMN test_activity_events.metadata IS 'JSON metadata containing additional event details like score, passed, timeSpentSeconds';
