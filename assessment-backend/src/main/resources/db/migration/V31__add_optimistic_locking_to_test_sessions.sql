-- Add optimistic locking version column to test_sessions.
-- DEFAULT 0 ensures existing rows get a valid version so Hibernate
-- @Version checks work correctly from the first update.

ALTER TABLE test_sessions ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
