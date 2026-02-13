-- GIN indexes for JSONB columns
-- These cannot be created by Hibernate's ddl-auto=update
-- Executed on every startup via spring.sql.init.mode=always

-- Assessment questions: speeds up JSONB containment queries on answer_options
-- Used during test assembly for option filtering
CREATE INDEX IF NOT EXISTS idx_assessment_questions_answer_options_gin
    ON assessment_questions USING GIN (answer_options jsonb_path_ops);

-- Test templates: speeds up JSONB containment queries on blueprint
-- Used for template filtering by blueprint properties
CREATE INDEX IF NOT EXISTS idx_test_templates_blueprint_gin
    ON test_templates USING GIN (blueprint jsonb_path_ops);

-- Test templates: speeds up JSONB containment queries on typed_blueprint
CREATE INDEX IF NOT EXISTS idx_test_templates_typed_blueprint_gin
    ON test_templates USING GIN (typed_blueprint jsonb_path_ops);

-- Test results: speeds up JSONB containment queries on competency_scores
-- Used for results analysis and reporting
CREATE INDEX IF NOT EXISTS idx_test_results_competency_scores_gin
    ON test_results USING GIN (competency_scores jsonb_path_ops);

-- Assessment questions: speeds up JSONB queries on metadata (tags filtering)
-- Used during test assembly for context/tag-based question selection
CREATE INDEX IF NOT EXISTS idx_assessment_questions_metadata_gin
    ON assessment_questions USING GIN (metadata jsonb_path_ops);
