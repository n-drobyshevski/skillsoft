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
    is_active BOOLEAN DEFAULT true,
    order_index INTEGER DEFAULT 1,
    PRIMARY KEY (id),
    FOREIGN KEY (behavioral_indicator_id) REFERENCES behavioral_indicators(id)
);