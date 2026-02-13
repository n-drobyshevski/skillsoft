-- ================================================================
-- SKILLSOFT TEST TEMPLATES MIGRATION
-- ================================================================
-- Version: V8__add_test_templates.sql
-- Description: Add versioning columns to test_templates table and
--              insert 3 sample test templates (OVERVIEW, JOB_FIT, TEAM_FIT)
-- Author: SkillSoft Team
-- Date: 2025-12-15
-- ================================================================

-- ================================================================
-- PHASE 1: ADD MISSING COLUMNS TO test_templates TABLE
-- ================================================================
-- These columns are defined in TestTemplate entity but missing from V3

-- Add version column for Immutable Versioning pattern
ALTER TABLE test_templates
ADD COLUMN IF NOT EXISTS version INTEGER NOT NULL DEFAULT 1;

-- Add parent_id for version history tracking
ALTER TABLE test_templates
ADD COLUMN IF NOT EXISTS parent_id UUID REFERENCES test_templates(id);

-- Add status for template lifecycle (DRAFT, PUBLISHED, ARCHIVED)
ALTER TABLE test_templates
ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'DRAFT';

-- Add goal for assessment type (OVERVIEW, JOB_FIT, TEAM_FIT)
ALTER TABLE test_templates
ADD COLUMN IF NOT EXISTS goal VARCHAR(20) NOT NULL DEFAULT 'OVERVIEW';

-- Add typed_blueprint for polymorphic blueprint configuration
ALTER TABLE test_templates
ADD COLUMN IF NOT EXISTS typed_blueprint JSONB;

-- Add blueprint column (legacy, for backward compatibility)
ALTER TABLE test_templates
ADD COLUMN IF NOT EXISTS blueprint JSONB;

-- Add constraints for status and goal enums
ALTER TABLE test_templates
DROP CONSTRAINT IF EXISTS chk_template_status;

ALTER TABLE test_templates
ADD CONSTRAINT chk_template_status
CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'));

ALTER TABLE test_templates
DROP CONSTRAINT IF EXISTS chk_template_goal;

ALTER TABLE test_templates
ADD CONSTRAINT chk_template_goal
CHECK (goal IN ('OVERVIEW', 'JOB_FIT', 'TEAM_FIT'));

-- Create indexes for new columns
CREATE INDEX IF NOT EXISTS idx_test_templates_status ON test_templates(status);
CREATE INDEX IF NOT EXISTS idx_test_templates_goal ON test_templates(goal);
CREATE INDEX IF NOT EXISTS idx_test_templates_parent ON test_templates(parent_id);

-- Add comments
COMMENT ON COLUMN test_templates.version IS 'Version number for Immutable Versioning pattern (starts at 1)';
COMMENT ON COLUMN test_templates.parent_id IS 'Reference to previous version (null for original templates)';
COMMENT ON COLUMN test_templates.status IS 'Lifecycle status: DRAFT, PUBLISHED, or ARCHIVED';
COMMENT ON COLUMN test_templates.goal IS 'Assessment type: OVERVIEW, JOB_FIT, or TEAM_FIT';
COMMENT ON COLUMN test_templates.typed_blueprint IS 'Polymorphic JSONB configuration for test assembly';

-- ================================================================
-- PHASE 2: INSERT TEST TEMPLATES
-- ================================================================

-- Clear existing test templates (optional - comment out if you want to keep existing data)
-- DELETE FROM test_templates;

-- ----------------------------------------------------------------
-- TEMPLATE 1: OVERVIEW - Universal Baseline Assessment
-- Generates Competency Passport with Big Five profile
-- ----------------------------------------------------------------
INSERT INTO test_templates (
    id,
    name,
    description,
    version,
    parent_id,
    status,
    goal,
    typed_blueprint,
    competency_ids,
    questions_per_indicator,
    time_limit_minutes,
    passing_score,
    is_active,
    shuffle_questions,
    shuffle_options,
    allow_skip,
    allow_back_navigation,
    show_results_immediately,
    created_at,
    updated_at
) VALUES (
    'aaaaaaaa-aaaa-aaaa-aaaa-000000000001',
    'Паспорт компетенций',
    'Универсальная базовая оценка для создания Паспорта компетенций. Включает оценку когнитивных, межличностных, коммуникативных и лидерских компетенций с проекцией на профиль Big Five.',
    1,
    NULL,
    'PUBLISHED',
    'OVERVIEW',
    '{
        "strategy": "OVERVIEW",
        "competencyIds": [
            "11111111-1111-1111-1111-000000000001",
            "11111111-1111-1111-1111-000000000002",
            "11111111-1111-1111-1111-000000000003",
            "11111111-1111-1111-1111-000000000004",
            "11111111-1111-1111-1111-000000000005"
        ],
        "includeBigFive": true,
        "adaptivity": {
            "mode": "LINEAR",
            "allowBacktracking": true
        }
    }'::jsonb,
    '["11111111-1111-1111-1111-000000000001", "11111111-1111-1111-1111-000000000002", "11111111-1111-1111-1111-000000000003", "11111111-1111-1111-1111-000000000004", "11111111-1111-1111-1111-000000000005"]'::jsonb,
    2,
    45,
    0.0,
    true,
    true,
    true,
    true,
    true,
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------
-- TEMPLATE 2: JOB_FIT - Software Developer Assessment
-- O*NET-based job matching for Software Developers (15-1252.00)
-- ----------------------------------------------------------------
INSERT INTO test_templates (
    id,
    name,
    description,
    version,
    parent_id,
    status,
    goal,
    typed_blueprint,
    competency_ids,
    questions_per_indicator,
    time_limit_minutes,
    passing_score,
    is_active,
    shuffle_questions,
    shuffle_options,
    allow_skip,
    allow_back_navigation,
    show_results_immediately,
    created_at,
    updated_at
) VALUES (
    'aaaaaaaa-aaaa-aaaa-aaaa-000000000002',
    'Оценка соответствия должности: Разработчик ПО',
    'Целевая оценка соответствия кандидата требованиям позиции Software Developer по стандарту O*NET (15-1252.00). Использует взвешенное косинусное сходство для сопоставления профиля кандидата с эталонным профилем должности.',
    1,
    NULL,
    'PUBLISHED',
    'JOB_FIT',
    '{
        "strategy": "JOB_FIT",
        "onetSocCode": "15-1252.00",
        "strictnessLevel": 60,
        "adaptivity": {
            "mode": "LINEAR",
            "allowBacktracking": true
        }
    }'::jsonb,
    '["11111111-1111-1111-1111-000000000001", "11111111-1111-1111-1111-000000000002", "11111111-1111-1111-1111-000000000003"]'::jsonb,
    3,
    60,
    70.0,
    true,
    true,
    true,
    false,
    true,
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------
-- TEMPLATE 3: TEAM_FIT - Development Team Assessment
-- ESCO-based team gap analysis with personality compatibility
-- ----------------------------------------------------------------
INSERT INTO test_templates (
    id,
    name,
    description,
    version,
    parent_id,
    status,
    goal,
    typed_blueprint,
    competency_ids,
    questions_per_indicator,
    time_limit_minutes,
    passing_score,
    is_active,
    shuffle_questions,
    shuffle_options,
    allow_skip,
    allow_back_navigation,
    show_results_immediately,
    created_at,
    updated_at
) VALUES (
    'aaaaaaaa-aaaa-aaaa-aaaa-000000000003',
    'Анализ совместимости с командой',
    'Динамический анализ соответствия кандидата команде разработки. Оценивает недостающие компетенции в команде, совместимость по профилю Big Five и уровень насыщенности ролей. Использует нормализацию навыков по стандарту ESCO.',
    1,
    NULL,
    'PUBLISHED',
    'TEAM_FIT',
    '{
        "strategy": "TEAM_FIT",
        "teamId": "bbbbbbbb-bbbb-bbbb-bbbb-000000000001",
        "saturationThreshold": 0.75,
        "adaptivity": {
            "mode": "LINEAR",
            "allowBacktracking": true
        }
    }'::jsonb,
    '["11111111-1111-1111-1111-000000000004", "11111111-1111-1111-1111-000000000005"]'::jsonb,
    2,
    30,
    0.0,
    true,
    true,
    true,
    true,
    true,
    false,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- ================================================================
-- PHASE 3: VERIFICATION
-- ================================================================

-- Log inserted templates
DO $$
DECLARE
    template_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO template_count FROM test_templates;
    RAISE NOTICE 'Total test templates in database: %', template_count;
END $$;

-- ================================================================
-- END OF MIGRATION
-- ================================================================
