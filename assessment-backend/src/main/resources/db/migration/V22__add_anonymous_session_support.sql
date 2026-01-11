-- ================================================================
-- ANONYMOUS SESSION SUPPORT MIGRATION
-- ================================================================
-- Version: V22__add_anonymous_session_support.sql
-- Description: Enable anonymous test-taking via share links
-- Author: SkillSoft Team
-- Date: 2025-01-11
--
-- This migration adds support for anonymous test sessions where
-- users can take tests via share links without authentication.
-- After completing the test, they provide basic identification info
-- (name, email, notes) which is stored as JSONB.
-- ================================================================

-- ================================================================
-- PHASE 1: MODIFY TEST_SESSIONS TABLE
-- ================================================================

-- 1.1 Make clerk_user_id nullable for anonymous sessions
-- Existing sessions with clerk_user_id remain unchanged
ALTER TABLE test_sessions
    ALTER COLUMN clerk_user_id DROP NOT NULL;

-- 1.2 Add share link reference for anonymous sessions
-- Links to template_share_links table for tracking which link was used
ALTER TABLE test_sessions
    ADD COLUMN IF NOT EXISTS share_link_id UUID REFERENCES template_share_links(id) ON DELETE SET NULL;

-- 1.3 Add session access token hash for anonymous authentication
-- SHA-256 hash of the session token (64 hex characters)
-- Used instead of Clerk JWT for anonymous session authentication
ALTER TABLE test_sessions
    ADD COLUMN IF NOT EXISTS session_access_token_hash VARCHAR(64);

-- 1.4 Add IP address tracking for rate limiting and audit
-- Supports both IPv4 (15 chars) and IPv6 (45 chars)
ALTER TABLE test_sessions
    ADD COLUMN IF NOT EXISTS ip_address VARCHAR(45);

-- 1.5 Add user agent for analytics
-- Helps identify browser/device for anonymous sessions
ALTER TABLE test_sessions
    ADD COLUMN IF NOT EXISTS user_agent VARCHAR(500);

-- 1.6 Add anonymous taker info as JSONB
-- Structure: {firstName, lastName, email, notes, collectedAt}
-- Collected after test completion
ALTER TABLE test_sessions
    ADD COLUMN IF NOT EXISTS anonymous_taker_info JSONB;

-- ================================================================
-- PHASE 2: MODIFY TEST_RESULTS TABLE
-- ================================================================

-- 2.1 Make clerk_user_id nullable for anonymous results
-- Results from anonymous sessions will have NULL clerk_user_id
-- The taker info can be retrieved via session.anonymousTakerInfo
ALTER TABLE test_results
    ALTER COLUMN clerk_user_id DROP NOT NULL;

-- ================================================================
-- PHASE 3: CREATE RATE LIMITING TABLE
-- ================================================================

-- Rate limiting table for anonymous session creation
-- Prevents abuse by limiting sessions per IP per time window
CREATE TABLE IF NOT EXISTS anonymous_session_rate_limits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Client IP address (unique constraint prevents duplicates)
    ip_address VARCHAR(45) NOT NULL,

    -- Number of sessions created in current window
    session_count INTEGER NOT NULL DEFAULT 1,

    -- Start of the current rate limiting window
    window_start TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- If set, IP is blocked until this time
    blocked_until TIMESTAMP WITHOUT TIME ZONE,

    -- Ensure one entry per IP
    CONSTRAINT uk_rate_limit_ip UNIQUE (ip_address)
);

-- ================================================================
-- PHASE 4: CREATE INDEXES
-- ================================================================

-- 4.1 Index for token-based session lookup (anonymous auth)
-- Partial index only on rows with tokens (saves space)
CREATE INDEX IF NOT EXISTS idx_sessions_access_token_hash
    ON test_sessions(session_access_token_hash)
    WHERE session_access_token_hash IS NOT NULL;

-- 4.2 Index for share link based queries
-- Used to find all sessions created from a specific share link
CREATE INDEX IF NOT EXISTS idx_sessions_share_link
    ON test_sessions(share_link_id)
    WHERE share_link_id IS NOT NULL;

-- 4.3 Index for anonymous session queries
-- Efficiently query anonymous sessions by template
CREATE INDEX IF NOT EXISTS idx_sessions_anonymous
    ON test_sessions(template_id, created_at DESC)
    WHERE clerk_user_id IS NULL;

-- 4.4 Index for IP-based rate limit checks
-- Used during anonymous session creation
CREATE INDEX IF NOT EXISTS idx_sessions_ip_created
    ON test_sessions(ip_address, created_at)
    WHERE ip_address IS NOT NULL;

-- 4.5 Index for rate limit cleanup job
-- Allows efficient cleanup of expired entries
CREATE INDEX IF NOT EXISTS idx_rate_limits_window
    ON anonymous_session_rate_limits(window_start);

-- 4.6 Index for blocked IP queries
CREATE INDEX IF NOT EXISTS idx_rate_limits_blocked
    ON anonymous_session_rate_limits(blocked_until)
    WHERE blocked_until IS NOT NULL;

-- ================================================================
-- PHASE 5: ADD DOCUMENTATION
-- ================================================================

-- Document the purpose of new columns
COMMENT ON COLUMN test_sessions.clerk_user_id IS
    'Clerk user ID. NULL for anonymous sessions (share link based)';

COMMENT ON COLUMN test_sessions.share_link_id IS
    'Reference to TemplateShareLink used for anonymous access. NULL for authenticated sessions';

COMMENT ON COLUMN test_sessions.session_access_token_hash IS
    'SHA-256 hash of session access token for anonymous authentication. 64 hex characters';

COMMENT ON COLUMN test_sessions.ip_address IS
    'Client IP address (IPv4 or IPv6) for rate limiting and audit. Max 45 characters';

COMMENT ON COLUMN test_sessions.user_agent IS
    'Client user agent string for analytics. Max 500 characters';

COMMENT ON COLUMN test_sessions.anonymous_taker_info IS
    'JSONB containing taker info: {firstName, lastName, email, notes, collectedAt}. Collected after test completion';

COMMENT ON TABLE anonymous_session_rate_limits IS
    'Rate limiting for anonymous session creation by IP address. Prevents abuse of share links';

COMMENT ON COLUMN anonymous_session_rate_limits.ip_address IS
    'Client IP address (IPv4 or IPv6). One entry per unique IP';

COMMENT ON COLUMN anonymous_session_rate_limits.session_count IS
    'Number of anonymous sessions created from this IP in current window';

COMMENT ON COLUMN anonymous_session_rate_limits.window_start IS
    'Start timestamp of the current rate limiting window (1 hour rolling)';

COMMENT ON COLUMN anonymous_session_rate_limits.blocked_until IS
    'If set, this IP is blocked from creating sessions until this timestamp';

-- ================================================================
-- PHASE 6: VERIFICATION
-- ================================================================

-- Log migration completion
DO $$
DECLARE
    sessions_with_nullable_clerk INTEGER;
    rate_limits_table_exists BOOLEAN;
BEGIN
    -- Verify clerk_user_id is now nullable
    SELECT COUNT(*) INTO sessions_with_nullable_clerk
    FROM information_schema.columns
    WHERE table_name = 'test_sessions'
    AND column_name = 'clerk_user_id'
    AND is_nullable = 'YES';

    -- Verify rate limits table exists
    SELECT EXISTS (
        SELECT FROM information_schema.tables
        WHERE table_name = 'anonymous_session_rate_limits'
    ) INTO rate_limits_table_exists;

    RAISE NOTICE 'Migration V22 completed:';
    RAISE NOTICE '  - test_sessions.clerk_user_id nullable: %', (sessions_with_nullable_clerk > 0);
    RAISE NOTICE '  - anonymous_session_rate_limits table: %', rate_limits_table_exists;
END $$;

-- ================================================================
-- END OF MIGRATION
-- ================================================================
