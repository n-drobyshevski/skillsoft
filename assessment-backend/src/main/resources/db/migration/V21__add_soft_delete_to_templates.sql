-- V21__add_soft_delete_to_templates.sql
-- Adds soft delete columns to test_templates table
-- Supports safe deletion workflow with SOFT_DELETE, ARCHIVE_AND_CLEANUP, and FORCE_DELETE modes

-- Add soft delete columns to test_templates
ALTER TABLE test_templates
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL,
ADD COLUMN IF NOT EXISTS deleted_by_id UUID NULL;

-- Add FK constraint for deleted_by reference to users table
ALTER TABLE test_templates
ADD CONSTRAINT fk_template_deleted_by
FOREIGN KEY (deleted_by_id) REFERENCES users(id)
ON DELETE SET NULL;

-- Create partial index for efficient filtering of non-deleted templates
-- This index only includes active (non-deleted) templates for faster queries
CREATE INDEX IF NOT EXISTS idx_templates_active
ON test_templates(created_at DESC)
WHERE deleted_at IS NULL;

-- Create index for finding soft-deleted templates (for admin restore functionality)
CREATE INDEX IF NOT EXISTS idx_templates_deleted
ON test_templates(deleted_at DESC)
WHERE deleted_at IS NOT NULL;

-- Create composite index for cleanup job queries on test_sessions
-- Optimizes finding stale sessions for the 24-hour cleanup job
CREATE INDEX IF NOT EXISTS idx_sessions_cleanup
ON test_sessions(status, last_activity_at)
WHERE status IN ('IN_PROGRESS', 'NOT_STARTED', 'ABANDONED');

-- Documentation comments
COMMENT ON COLUMN test_templates.deleted_at IS
'Soft delete timestamp. NULL means template is active. Non-null means template has been soft-deleted.';

COMMENT ON COLUMN test_templates.deleted_by_id IS
'Reference to the user who performed the soft delete. Used for audit trail.';
