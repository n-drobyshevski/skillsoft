-- Flyway Migration: Create Testing System Tables
-- Version: V3__create_testing_tables.sql
-- Description: Create tables for test templates, sessions, answers, and results
-- Author: System
-- Date: 2025-01-XX

-- ================================================================
-- TESTING SYSTEM TABLES - FLYWAY MIGRATION
-- ================================================================

-- Шаблоны тестов (Test Templates)
-- Defines the structure and configuration for assessment tests
CREATE TABLE test_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    competency_ids JSONB NOT NULL DEFAULT '[]',
    questions_per_indicator INTEGER DEFAULT 3,
    time_limit_minutes INTEGER DEFAULT 60,
    passing_score DECIMAL(5,2) DEFAULT 70.00,
    is_active BOOLEAN DEFAULT TRUE,
    shuffle_questions BOOLEAN DEFAULT TRUE,
    shuffle_options BOOLEAN DEFAULT TRUE,
    allow_skip BOOLEAN DEFAULT TRUE,
    allow_back_navigation BOOLEAN DEFAULT TRUE,
    show_results_immediately BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for test_templates
CREATE INDEX idx_test_templates_active ON test_templates(is_active);
CREATE INDEX idx_test_templates_competencies ON test_templates USING GIN(competency_ids);
CREATE INDEX idx_test_templates_created ON test_templates(created_at);

COMMENT ON TABLE test_templates IS 'Test template configurations for competency assessments';
COMMENT ON COLUMN test_templates.competency_ids IS 'JSONB array of competency UUIDs to include in the test';
COMMENT ON COLUMN test_templates.questions_per_indicator IS 'Number of questions to include per behavioral indicator';
COMMENT ON COLUMN test_templates.passing_score IS 'Minimum percentage score required to pass (0-100)';

-- ================================================================

-- Сессии тестирования (Test Sessions)
-- Tracks the state and progress of a user taking a test
CREATE TABLE test_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id UUID NOT NULL REFERENCES test_templates(id) ON DELETE RESTRICT,
    clerk_user_id VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',
    started_at TIMESTAMP WITHOUT TIME ZONE,
    completed_at TIMESTAMP WITHOUT TIME ZONE,
    current_question_index INTEGER DEFAULT 0,
    time_remaining_seconds INTEGER,
    question_order JSONB,
    last_activity_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_session_status CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'ABANDONED', 'TIMED_OUT'))
);

-- Indexes for test_sessions
CREATE INDEX idx_test_sessions_user ON test_sessions(clerk_user_id);
CREATE INDEX idx_test_sessions_status ON test_sessions(status);
CREATE INDEX idx_test_sessions_template ON test_sessions(template_id);
CREATE INDEX idx_test_sessions_user_status ON test_sessions(clerk_user_id, status);
CREATE INDEX idx_test_sessions_created ON test_sessions(created_at);

COMMENT ON TABLE test_sessions IS 'Individual test taking sessions for users';
COMMENT ON COLUMN test_sessions.clerk_user_id IS 'Reference to Clerk authentication user ID';
COMMENT ON COLUMN test_sessions.question_order IS 'JSONB array of question UUIDs in presentation order (may be shuffled)';
COMMENT ON COLUMN test_sessions.time_remaining_seconds IS 'Remaining time for timed tests, updated as user progresses';

-- ================================================================

-- Ответы на вопросы (Test Answers)
-- Stores individual answers for each question in a session
CREATE TABLE test_answers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES test_sessions(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES assessment_questions(id) ON DELETE RESTRICT,
    selected_option_ids JSONB,
    likert_value INTEGER,
    ranking_order JSONB,
    text_response TEXT,
    answered_at TIMESTAMP WITHOUT TIME ZONE,
    time_spent_seconds INTEGER DEFAULT 0,
    is_skipped BOOLEAN DEFAULT FALSE,
    score DECIMAL(10,4),
    max_score DECIMAL(10,4),
    
    CONSTRAINT uq_session_question UNIQUE(session_id, question_id),
    CONSTRAINT chk_likert_range CHECK (likert_value IS NULL OR (likert_value >= 1 AND likert_value <= 7))
);

-- Indexes for test_answers
CREATE INDEX idx_test_answers_session ON test_answers(session_id);
CREATE INDEX idx_test_answers_question ON test_answers(question_id);
CREATE INDEX idx_test_answers_answered ON test_answers(answered_at);

COMMENT ON TABLE test_answers IS 'User responses to individual questions within a test session';
COMMENT ON COLUMN test_answers.selected_option_ids IS 'JSONB array of selected option IDs for choice questions';
COMMENT ON COLUMN test_answers.likert_value IS 'Numeric value (1-7) for Likert scale questions';
COMMENT ON COLUMN test_answers.ranking_order IS 'JSONB array of option IDs in ranked order';
COMMENT ON COLUMN test_answers.score IS 'Points earned for this answer (null until graded)';
COMMENT ON COLUMN test_answers.max_score IS 'Maximum possible points for this question';

-- ================================================================

-- Результаты тестирования (Test Results)
-- Final computed results after test completion
CREATE TABLE test_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL UNIQUE REFERENCES test_sessions(id) ON DELETE CASCADE,
    clerk_user_id VARCHAR(255) NOT NULL,
    overall_score DECIMAL(10,4),
    overall_percentage DECIMAL(5,2),
    percentile INTEGER,
    passed BOOLEAN,
    competency_scores JSONB,
    total_time_seconds INTEGER,
    questions_answered INTEGER,
    questions_skipped INTEGER,
    completed_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_percentage_range CHECK (overall_percentage IS NULL OR (overall_percentage >= 0 AND overall_percentage <= 100)),
    CONSTRAINT chk_percentile_range CHECK (percentile IS NULL OR (percentile >= 0 AND percentile <= 100))
);

-- Indexes for test_results
CREATE INDEX idx_test_results_user ON test_results(clerk_user_id);
CREATE INDEX idx_test_results_completed ON test_results(completed_at);
CREATE INDEX idx_test_results_passed ON test_results(passed);
CREATE INDEX idx_test_results_user_completed ON test_results(clerk_user_id, completed_at);

COMMENT ON TABLE test_results IS 'Final computed results for completed test sessions';
COMMENT ON COLUMN test_results.percentile IS 'User ranking compared to other test takers (0-100)';
COMMENT ON COLUMN test_results.competency_scores IS 'JSONB array of per-competency score breakdowns';

-- ================================================================
-- END OF MIGRATION
-- ================================================================
