-- V24: Add exposure count tracking to assessment questions for item exposure control.
-- Tracks how many times each question has been selected for tests.
-- Overexposed items get deprioritized to preserve test security and validity.

ALTER TABLE assessment_questions
ADD COLUMN IF NOT EXISTS exposure_count INTEGER NOT NULL DEFAULT 0;

-- Index for efficient sorting by exposure count during question selection
CREATE INDEX idx_assessment_questions_exposure ON assessment_questions (exposure_count);
