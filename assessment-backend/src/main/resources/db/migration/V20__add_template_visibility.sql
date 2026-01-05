-- V20__add_template_visibility.sql
-- Adds template visibility and sharing functionality
--
-- Features:
-- - Owner tracking for test templates
-- - Visibility modes: PUBLIC, PRIVATE, LINK
-- - User and team-based sharing with permission levels
-- - Shareable links with expiration and usage tracking

-- ============================================
-- 1. Add visibility fields to test_templates
-- ============================================

-- Add owner_id column (nullable initially for migration)
ALTER TABLE test_templates
ADD COLUMN IF NOT EXISTS owner_id UUID;

-- Add visibility column with default PRIVATE
ALTER TABLE test_templates
ADD COLUMN IF NOT EXISTS visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE';

-- Add visibility change timestamp
ALTER TABLE test_templates
ADD COLUMN IF NOT EXISTS visibility_changed_at TIMESTAMP;

-- Create indexes for owner and visibility lookups
CREATE INDEX IF NOT EXISTS idx_test_templates_owner ON test_templates(owner_id);
CREATE INDEX IF NOT EXISTS idx_test_templates_visibility ON test_templates(visibility);

-- ============================================
-- 2. Create template_shares table
-- ============================================

CREATE TABLE IF NOT EXISTS template_shares (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id UUID NOT NULL,
    grantee_type VARCHAR(10) NOT NULL,
    user_id UUID,
    team_id UUID,
    permission VARCHAR(10) NOT NULL DEFAULT 'VIEW',
    granted_by_id UUID NOT NULL,
    granted_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP,
    revoked_at TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT true,
    version BIGINT DEFAULT 0,

    -- Foreign key constraints
    CONSTRAINT fk_template_shares_template
        FOREIGN KEY (template_id) REFERENCES test_templates(id) ON DELETE CASCADE,
    CONSTRAINT fk_template_shares_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_template_shares_team
        FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE SET NULL,
    CONSTRAINT fk_template_shares_granted_by
        FOREIGN KEY (granted_by_id) REFERENCES users(id),

    -- Ensure exactly one of user_id or team_id is set based on grantee_type
    CONSTRAINT chk_grantee_type CHECK (
        (grantee_type = 'USER' AND user_id IS NOT NULL AND team_id IS NULL) OR
        (grantee_type = 'TEAM' AND team_id IS NOT NULL AND user_id IS NULL)
    ),

    -- Ensure valid permission values
    CONSTRAINT chk_permission CHECK (permission IN ('VIEW', 'EDIT', 'MANAGE')),

    -- Ensure valid grantee_type values
    CONSTRAINT chk_grantee_type_value CHECK (grantee_type IN ('USER', 'TEAM'))
);

-- Unique constraints to prevent duplicate shares
CREATE UNIQUE INDEX IF NOT EXISTS uk_template_share_user
    ON template_shares(template_id, user_id) WHERE user_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_template_share_team
    ON template_shares(template_id, team_id) WHERE team_id IS NOT NULL;

-- Indexes for efficient queries
CREATE INDEX IF NOT EXISTS idx_template_shares_template ON template_shares(template_id);
CREATE INDEX IF NOT EXISTS idx_template_shares_user ON template_shares(user_id);
CREATE INDEX IF NOT EXISTS idx_template_shares_team ON template_shares(team_id);
CREATE INDEX IF NOT EXISTS idx_template_shares_active ON template_shares(template_id, is_active);

-- ============================================
-- 3. Create template_share_links table
-- ============================================

CREATE TABLE IF NOT EXISTS template_share_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id UUID NOT NULL,
    token VARCHAR(64) NOT NULL UNIQUE,
    created_by_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL,
    max_uses INTEGER,
    current_uses INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    permission VARCHAR(10) NOT NULL DEFAULT 'VIEW',
    label VARCHAR(200),
    last_used_at TIMESTAMP,
    revoked_at TIMESTAMP,

    -- Foreign key constraints
    CONSTRAINT fk_share_links_template
        FOREIGN KEY (template_id) REFERENCES test_templates(id) ON DELETE CASCADE,
    CONSTRAINT fk_share_links_created_by
        FOREIGN KEY (created_by_id) REFERENCES users(id),

    -- Ensure valid permission values
    CONSTRAINT chk_link_permission CHECK (permission IN ('VIEW', 'EDIT')),

    -- Ensure expiry is in the future when created (enforced at application level)
    -- Ensure current_uses is non-negative
    CONSTRAINT chk_current_uses CHECK (current_uses >= 0),

    -- Ensure max_uses is positive if set
    CONSTRAINT chk_max_uses CHECK (max_uses IS NULL OR max_uses > 0)
);

-- Indexes for efficient queries
CREATE INDEX IF NOT EXISTS idx_share_links_template ON template_share_links(template_id);
CREATE INDEX IF NOT EXISTS idx_share_links_active ON template_share_links(template_id, is_active);
CREATE INDEX IF NOT EXISTS idx_share_links_token ON template_share_links(token);
CREATE INDEX IF NOT EXISTS idx_share_links_expires ON template_share_links(expires_at);

-- ============================================
-- 4. Migrate existing templates - assign owner
-- ============================================

-- Set owner to the first ADMIN user for existing templates
UPDATE test_templates t
SET owner_id = (
    SELECT id FROM users
    WHERE role = 'ADMIN'
    ORDER BY created_at
    LIMIT 1
)
WHERE owner_id IS NULL;

-- If no ADMIN exists, use the first user
UPDATE test_templates t
SET owner_id = (
    SELECT id FROM users
    ORDER BY created_at
    LIMIT 1
)
WHERE owner_id IS NULL;

-- ============================================
-- 5. Add foreign key constraint for owner
-- ============================================

-- Now that all templates have owners, add the foreign key
ALTER TABLE test_templates
ADD CONSTRAINT fk_test_templates_owner
    FOREIGN KEY (owner_id) REFERENCES users(id);

-- ============================================
-- 6. Add comments for documentation
-- ============================================

COMMENT ON COLUMN test_templates.owner_id IS 'Owner of the template (creator by default)';
COMMENT ON COLUMN test_templates.visibility IS 'Visibility mode: PUBLIC, PRIVATE, or LINK';
COMMENT ON COLUMN test_templates.visibility_changed_at IS 'When visibility was last changed';

COMMENT ON TABLE template_shares IS 'Grants access to templates for users or teams';
COMMENT ON COLUMN template_shares.grantee_type IS 'USER or TEAM';
COMMENT ON COLUMN template_shares.permission IS 'VIEW, EDIT, or MANAGE';

COMMENT ON TABLE template_share_links IS 'Shareable links for template access (supports anonymous)';
COMMENT ON COLUMN template_share_links.token IS 'Cryptographically secure URL token (64 chars)';
COMMENT ON COLUMN template_share_links.max_uses IS 'Maximum uses allowed (null = unlimited)';
