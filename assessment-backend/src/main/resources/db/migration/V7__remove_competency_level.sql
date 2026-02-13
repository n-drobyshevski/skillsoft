-- ================================================================
-- V7: Remove level column from competencies table
-- ================================================================
-- Reason: The 'level' field (ProficiencyLevel) was never used for any
-- business logic. Test assembly uses Question.difficultyLevel instead.
-- This field caused confusion with DifficultyLevel enum and migration errors.
-- ================================================================

ALTER TABLE competencies DROP COLUMN IF EXISTS level;
