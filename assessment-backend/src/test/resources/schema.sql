-- Schema for test database with H2 and JSONB support
-- H2 doesn't need custom JSONB type, JSON works directly

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
    version INTEGER DEFAULT 1,
    parent_id UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

-- Create test_sessions table for integration tests
CREATE TABLE IF NOT EXISTS test_sessions (
    id UUID NOT NULL,
    template_id UUID,
    clerk_user_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    current_question_index INTEGER DEFAULT 0,
    time_remaining_seconds INTEGER,
    question_order JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_activity_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (template_id) REFERENCES test_templates(id)
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
    competency_scores JSON,
    total_time_seconds INTEGER,
    questions_answered INTEGER,
    questions_skipped INTEGER,
    total_questions INTEGER,
    completed_at TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (session_id) REFERENCES test_sessions(id),
    FOREIGN KEY (template_id) REFERENCES test_templates(id)
);