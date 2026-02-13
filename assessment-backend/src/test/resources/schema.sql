-- Schema for test database with H2 and JSONB support
-- H2 doesn't need custom JSONB type, JSON works directly

-- Create users table (needed for template sharing and ownership)
CREATE TABLE IF NOT EXISTS users (
    id UUID NOT NULL,
    clerk_id VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255),
    username VARCHAR(100),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    image_url VARCHAR(500),
    has_image BOOLEAN DEFAULT false,
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    is_active BOOLEAN DEFAULT true,
    banned BOOLEAN DEFAULT false,
    locked BOOLEAN DEFAULT false,
    preferences JSON DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    clerk_created_at TIMESTAMP,
    last_login TIMESTAMP,
    last_sign_in_at TIMESTAMP,
    PRIMARY KEY (id)
);

-- Create teams table (needed for team sharing)
CREATE TABLE IF NOT EXISTS teams (
    id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    leader_id UUID,
    created_by_id UUID NOT NULL,
    metadata JSON DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    activated_at TIMESTAMP,
    archived_at TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (leader_id) REFERENCES users(id),
    FOREIGN KEY (created_by_id) REFERENCES users(id)
);

-- Create team_members table (many-to-many relationship between teams and users)
CREATE TABLE IF NOT EXISTS team_members (
    id UUID NOT NULL,
    team_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    left_at TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT true,
    PRIMARY KEY (id),
    FOREIGN KEY (team_id) REFERENCES teams(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE (team_id, user_id)
);

-- Create competencies table
CREATE TABLE IF NOT EXISTS competencies (
    id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    level VARCHAR(50),
    standard_codes JSON,
    approval_status VARCHAR(50),
    is_active BOOLEAN DEFAULT true,
    version INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

-- Create behavioral_indicators table first (needed for assessment_questions FK)
CREATE TABLE IF NOT EXISTS behavioral_indicators (
    id UUID NOT NULL,
    competency_id UUID,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    observability_level VARCHAR(50),
    measurement_type VARCHAR(50),
    weight FLOAT DEFAULT 1.0,
    examples TEXT,
    counter_examples TEXT,
    is_active BOOLEAN DEFAULT true,
    approval_status VARCHAR(50),
    order_index INTEGER,
    context_scope VARCHAR(50),
    PRIMARY KEY (id),
    FOREIGN KEY (competency_id) REFERENCES competencies(id)
);

-- Create assessment_questions table with proper structure matching the entity
CREATE TABLE IF NOT EXISTS assessment_questions (
    id UUID NOT NULL,
    behavioral_indicator_id UUID,
    question_text VARCHAR(500) NOT NULL,
    question_type VARCHAR(50) NOT NULL,
    answer_options JSON,
    scoring_rubric TEXT,
    time_limit INTEGER,
    difficulty_level VARCHAR(20),
    metadata JSON,
    is_active BOOLEAN DEFAULT true,
    order_index INTEGER DEFAULT 1,
    PRIMARY KEY (id),
    FOREIGN KEY (behavioral_indicator_id) REFERENCES behavioral_indicators(id)
);

-- Create test_templates table for integration tests
CREATE TABLE IF NOT EXISTS test_templates (
    id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    goal VARCHAR(50),
    competency_ids JSON,
    blueprint JSON,
    typed_blueprint JSON,
    questions_per_indicator INTEGER DEFAULT 3,
    time_limit_minutes INTEGER DEFAULT 60,
    passing_score DOUBLE DEFAULT 70.0,
    shuffle_questions BOOLEAN DEFAULT true,
    shuffle_options BOOLEAN DEFAULT true,
    allow_skip BOOLEAN DEFAULT true,
    allow_back_navigation BOOLEAN DEFAULT true,
    show_results_immediately BOOLEAN DEFAULT true,
    is_active BOOLEAN DEFAULT true,
    status VARCHAR(50),
    visibility VARCHAR(20) DEFAULT 'PRIVATE',
    visibility_changed_at TIMESTAMP,
    version INTEGER DEFAULT 1,
    parent_id UUID,
    owner_id UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    deleted_by_id UUID,
    PRIMARY KEY (id),
    FOREIGN KEY (owner_id) REFERENCES users(id),
    FOREIGN KEY (deleted_by_id) REFERENCES users(id)
);

-- Create template_shares table for sharing templates with users/teams
CREATE TABLE IF NOT EXISTS template_shares (
    id UUID NOT NULL,
    template_id UUID NOT NULL,
    grantee_type VARCHAR(10) NOT NULL,
    user_id UUID,
    team_id UUID,
    permission VARCHAR(10) NOT NULL DEFAULT 'VIEW',
    granted_by_id UUID NOT NULL,
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    revoked_at TIMESTAMP,
    is_active BOOLEAN DEFAULT true,
    version BIGINT DEFAULT 0,
    PRIMARY KEY (id),
    FOREIGN KEY (template_id) REFERENCES test_templates(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (team_id) REFERENCES teams(id),
    FOREIGN KEY (granted_by_id) REFERENCES users(id),
    UNIQUE (template_id, user_id),
    UNIQUE (template_id, team_id)
);

-- Create template_share_links table for shareable links
CREATE TABLE IF NOT EXISTS template_share_links (
    id UUID NOT NULL,
    template_id UUID NOT NULL,
    token VARCHAR(64) NOT NULL UNIQUE,
    permission VARCHAR(10) NOT NULL DEFAULT 'VIEW',
    created_by_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    is_active BOOLEAN DEFAULT true,
    max_uses INTEGER,
    current_uses INTEGER DEFAULT 0,
    PRIMARY KEY (id),
    FOREIGN KEY (template_id) REFERENCES test_templates(id),
    FOREIGN KEY (created_by_id) REFERENCES users(id)
);

-- Create test_sessions table for integration tests
-- V22: clerk_user_id nullable for anonymous sessions
CREATE TABLE IF NOT EXISTS test_sessions (
    id UUID NOT NULL,
    template_id UUID,
    clerk_user_id VARCHAR(255),  -- Nullable for anonymous sessions
    status VARCHAR(50) NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    current_question_index INTEGER DEFAULT 0,
    time_remaining_seconds INTEGER,
    question_order JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_activity_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    -- Anonymous session support (V22)
    share_link_id UUID,
    session_access_token_hash VARCHAR(64),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    anonymous_taker_info JSON,
    PRIMARY KEY (id),
    FOREIGN KEY (template_id) REFERENCES test_templates(id),
    FOREIGN KEY (share_link_id) REFERENCES template_share_links(id)
);

-- Create test_answers table for integration tests
CREATE TABLE IF NOT EXISTS test_answers (
    id UUID NOT NULL,
    session_id UUID,
    question_id UUID,
    selected_option_ids JSON,
    likert_value INTEGER,
    ranking_order JSON,
    text_response TEXT,
    answered_at TIMESTAMP,
    time_spent_seconds INTEGER,
    is_skipped BOOLEAN DEFAULT false,
    score DOUBLE,
    max_score DOUBLE,
    PRIMARY KEY (id),
    FOREIGN KEY (session_id) REFERENCES test_sessions(id),
    FOREIGN KEY (question_id) REFERENCES assessment_questions(id)
);

-- Create test_results table for integration tests
CREATE TABLE IF NOT EXISTS test_results (
    id UUID NOT NULL,
    session_id UUID UNIQUE,
    template_id UUID,
    clerk_user_id VARCHAR(255),
    overall_score DOUBLE,
    overall_percentage DOUBLE,
    percentile INTEGER,
    passed BOOLEAN,
    status VARCHAR(50) DEFAULT 'COMPLETED',
    competency_scores JSON,
    big_five_profile JSON,
    extended_metrics JSON,
    total_time_seconds INTEGER,
    questions_answered INTEGER,
    questions_skipped INTEGER,
    total_questions INTEGER,
    completed_at TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (session_id) REFERENCES test_sessions(id),
    FOREIGN KEY (template_id) REFERENCES test_templates(id)
);