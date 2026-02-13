-- V19: Create teams tables for TEAM_FIT assessments
-- Teams allow grouping users for collective competency profile analysis

-- Teams table
CREATE TABLE teams (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    leader_id UUID REFERENCES users(id) ON DELETE SET NULL,
    created_by_id UUID NOT NULL REFERENCES users(id),
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activated_at TIMESTAMP,
    archived_at TIMESTAMP,

    -- Status constraint
    CONSTRAINT chk_teams_status CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED'))
);

-- Team members join table (many-to-many with extra columns)
CREATE TABLE team_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    left_at TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Unique constraint to prevent duplicate memberships
    CONSTRAINT uk_team_members_team_user UNIQUE (team_id, user_id),

    -- Role constraint
    CONSTRAINT chk_team_members_role CHECK (role IN ('LEADER', 'MEMBER'))
);

-- Indexes for teams table
CREATE INDEX idx_teams_status ON teams(status);
CREATE INDEX idx_teams_leader ON teams(leader_id);
CREATE INDEX idx_teams_created_by ON teams(created_by_id);
CREATE INDEX idx_teams_name ON teams(name);

-- Indexes for team_members table
CREATE INDEX idx_team_members_team ON team_members(team_id);
CREATE INDEX idx_team_members_user ON team_members(user_id);
CREATE INDEX idx_team_members_active ON team_members(team_id, is_active);
CREATE INDEX idx_team_members_role ON team_members(team_id, role);

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_teams_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to auto-update updated_at on teams
CREATE TRIGGER trigger_teams_updated_at
    BEFORE UPDATE ON teams
    FOR EACH ROW
    EXECUTE FUNCTION update_teams_updated_at();

-- Comments for documentation
COMMENT ON TABLE teams IS 'Teams for grouping users in TEAM_FIT assessments';
COMMENT ON COLUMN teams.status IS 'Team lifecycle status: DRAFT, ACTIVE, ARCHIVED';
COMMENT ON COLUMN teams.leader_id IS 'Team leader (must be an active member)';
COMMENT ON COLUMN teams.metadata IS 'Additional team data stored as JSON (department, tags, etc.)';

COMMENT ON TABLE team_members IS 'Many-to-many relationship between teams and users';
COMMENT ON COLUMN team_members.role IS 'Member role: LEADER or MEMBER';
COMMENT ON COLUMN team_members.is_active IS 'Whether membership is currently active';
COMMENT ON COLUMN team_members.left_at IS 'Timestamp when member left (if removed)';
