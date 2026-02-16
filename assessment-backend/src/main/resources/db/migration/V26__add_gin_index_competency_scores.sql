-- V26: Add GIN index on competency_scores JSONB column in test_results
-- Accelerates JSONB queries used by:
-- - SubscalePercentileCalculator (countCompetencyScoresBelow/Total)
-- - ConfidenceIntervalCalculator (calculateCompetencyScoreSD)
-- - Any future JSONB path queries on competency_scores
--
-- Uses jsonb_path_ops for optimized containment/path queries.
-- CONCURRENTLY avoids locking the table during index creation.

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_test_results_competency_scores_gin
    ON test_results USING GIN (competency_scores jsonb_path_ops);
