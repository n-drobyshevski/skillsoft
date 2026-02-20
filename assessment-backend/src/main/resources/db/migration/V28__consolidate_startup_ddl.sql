-- V28: Consolidate DDL previously in data-postgresql.sql into Flyway
-- These statements were running on every startup via spring.sql.init.mode=always
-- Now managed exclusively by Flyway with ddl-auto=validate

-- Fix: exposure_count column with DEFAULT 0 for existing rows
-- (V24 may have added this, but data-postgresql.sql also handled it as fallback)
ALTER TABLE assessment_questions
ADD COLUMN IF NOT EXISTS exposure_count INTEGER NOT NULL DEFAULT 0;

-- Index for efficient sorting by exposure count during question selection
CREATE INDEX IF NOT EXISTS idx_assessment_questions_exposure
    ON assessment_questions (exposure_count);

-- GIN indexes for JSONB columns (cannot be created by Hibernate ddl-auto)

-- Assessment questions: speeds up JSONB containment queries on answer_options
CREATE INDEX IF NOT EXISTS idx_assessment_questions_answer_options_gin
    ON assessment_questions USING GIN (answer_options jsonb_path_ops);

-- Test templates: speeds up JSONB containment queries on blueprint
CREATE INDEX IF NOT EXISTS idx_test_templates_blueprint_gin
    ON test_templates USING GIN (blueprint jsonb_path_ops);

-- Test templates: speeds up JSONB containment queries on typed_blueprint
CREATE INDEX IF NOT EXISTS idx_test_templates_typed_blueprint_gin
    ON test_templates USING GIN (typed_blueprint jsonb_path_ops);

-- Test results: speeds up JSONB containment queries on competency_scores
-- (V26 also creates this, but IF NOT EXISTS makes it safe)
CREATE INDEX IF NOT EXISTS idx_test_results_competency_scores_gin
    ON test_results USING GIN (competency_scores jsonb_path_ops);

-- Assessment questions: speeds up JSONB queries on metadata (tags filtering)
CREATE INDEX IF NOT EXISTS idx_assessment_questions_metadata_gin
    ON assessment_questions USING GIN (metadata jsonb_path_ops);

-- NEW: GIN index on test_templates.competency_ids for by-competency template lookup
-- Accelerates: competency_ids @> :competencyId::jsonb queries
CREATE INDEX IF NOT EXISTS idx_tt_competency_ids_gin
    ON test_templates USING GIN (competency_ids jsonb_path_ops);
