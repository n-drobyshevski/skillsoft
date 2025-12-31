-- V11: Add Big Five profile and extended metrics columns to test_results
-- Part of Task 3.1: Add bigFiveProfile to ScoringResult
-- These columns support TEAM_FIT goal personality and metrics data

-- Add Big Five personality profile column (JSONB)
-- Stores personality trait scores: {"Openness": 75.5, "Conscientiousness": 82.3, ...}
-- Only populated for TEAM_FIT assessments
ALTER TABLE test_results
    ADD COLUMN IF NOT EXISTS big_five_profile jsonb;

-- Add extended metrics column (JSONB)
-- Stores goal-specific analysis metrics
-- For TEAM_FIT: {"diversityRatio": 0.6, "saturationRatio": 0.3, "teamFitMultiplier": 1.05, ...}
ALTER TABLE test_results
    ADD COLUMN IF NOT EXISTS extended_metrics jsonb;

-- Add comment for documentation
COMMENT ON COLUMN test_results.big_five_profile IS 'Big Five personality profile (TEAM_FIT only). Maps trait names to percentage scores (0-100).';
COMMENT ON COLUMN test_results.extended_metrics IS 'Goal-specific extended metrics. For TEAM_FIT: diversityRatio, saturationRatio, teamFitMultiplier, etc.';

-- Optional: Create GIN index for JSONB queries if needed for analytics
-- CREATE INDEX IF NOT EXISTS idx_test_results_big_five_profile ON test_results USING GIN (big_five_profile);
-- CREATE INDEX IF NOT EXISTS idx_test_results_extended_metrics ON test_results USING GIN (extended_metrics);
