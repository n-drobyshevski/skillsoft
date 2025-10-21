-- Add GIN index for faster JSONB queries on answer_options
CREATE INDEX IF NOT EXISTS idx_assessment_questions_answer_options ON assessment_questions USING GIN (answer_options);

-- Comment explaining the index
COMMENT ON INDEX idx_assessment_questions_answer_options IS 'GIN index for efficient queries on JSONB answer_options field';