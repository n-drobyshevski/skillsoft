-- V9__create_psychometric_tables.sql
-- Creates tables for the Test Validation Mechanic (Psychometric Analysis)
-- Per architectural design: Item Statistics, Competency Reliability, Big Five Reliability

-- ============================================
-- ITEM STATISTICS TABLE
-- Stores psychometric metrics for individual assessment questions
-- ============================================
CREATE TABLE IF NOT EXISTS item_statistics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID NOT NULL UNIQUE,

    -- Core Psychometric Metrics
    difficulty_index NUMERIC(5,4),           -- p-value: 0.0-1.0
    discrimination_index NUMERIC(5,4),        -- rpb: -1.0 to 1.0
    distractor_efficiency JSONB,              -- Map of optionId -> selection%

    -- Metadata
    response_count INTEGER NOT NULL DEFAULT 0,
    last_calculated_at TIMESTAMP,

    -- Status Management
    validity_status VARCHAR(50) NOT NULL DEFAULT 'PROBATION',
    difficulty_flag VARCHAR(20),
    discrimination_flag VARCHAR(20),

    -- Trend Analysis
    previous_discrimination_index NUMERIC(5,4),
    status_change_history JSONB,

    -- Constraints
    CONSTRAINT fk_item_stats_question FOREIGN KEY (question_id)
        REFERENCES assessment_questions(id) ON DELETE CASCADE,
    CONSTRAINT chk_difficulty_range
        CHECK (difficulty_index IS NULL OR (difficulty_index >= 0 AND difficulty_index <= 1)),
    CONSTRAINT chk_discrimination_range
        CHECK (discrimination_index IS NULL OR (discrimination_index >= -1 AND discrimination_index <= 1)),
    CONSTRAINT chk_validity_status
        CHECK (validity_status IN ('ACTIVE', 'PROBATION', 'FLAGGED_FOR_REVIEW', 'RETIRED')),
    CONSTRAINT chk_difficulty_flag
        CHECK (difficulty_flag IS NULL OR difficulty_flag IN ('NONE', 'TOO_HARD', 'TOO_EASY')),
    CONSTRAINT chk_discrimination_flag
        CHECK (discrimination_flag IS NULL OR discrimination_flag IN ('NONE', 'WARNING', 'CRITICAL', 'NEGATIVE'))
);

-- Indexes for efficient querying
CREATE INDEX IF NOT EXISTS idx_item_stats_question ON item_statistics(question_id);
CREATE INDEX IF NOT EXISTS idx_item_stats_validity ON item_statistics(validity_status);
CREATE INDEX IF NOT EXISTS idx_item_stats_last_calc ON item_statistics(last_calculated_at);
CREATE INDEX IF NOT EXISTS idx_item_stats_response_count ON item_statistics(response_count);
CREATE INDEX IF NOT EXISTS idx_item_stats_discrimination ON item_statistics(discrimination_flag);

-- ============================================
-- COMPETENCY RELIABILITY TABLE
-- Stores Cronbach's Alpha and related metrics for competencies
-- ============================================
CREATE TABLE IF NOT EXISTS competency_reliability (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    competency_id UUID NOT NULL UNIQUE,

    -- Reliability Metrics
    cronbach_alpha NUMERIC(5,4),              -- -1.0 to 1.0 (typically 0.0-1.0)
    sample_size INTEGER,                       -- Number of respondents
    item_count INTEGER,                        -- Number of questions

    -- Status
    reliability_status VARCHAR(30),

    -- Alpha-if-Item-Deleted Analysis
    alpha_if_deleted JSONB,                    -- Map of questionId -> alpha

    -- Metadata
    last_calculated_at TIMESTAMP,

    -- Constraints
    CONSTRAINT fk_comp_reliability_competency FOREIGN KEY (competency_id)
        REFERENCES competencies(id) ON DELETE CASCADE,
    CONSTRAINT chk_alpha_range
        CHECK (cronbach_alpha IS NULL OR (cronbach_alpha >= -1 AND cronbach_alpha <= 1)),
    CONSTRAINT chk_reliability_status
        CHECK (reliability_status IS NULL OR reliability_status IN ('RELIABLE', 'ACCEPTABLE', 'UNRELIABLE', 'INSUFFICIENT_DATA'))
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_comp_reliability_competency ON competency_reliability(competency_id);
CREATE INDEX IF NOT EXISTS idx_comp_reliability_status ON competency_reliability(reliability_status);

-- ============================================
-- BIG FIVE RELIABILITY TABLE
-- Stores Cronbach's Alpha at the Big Five personality trait level
-- ============================================
CREATE TABLE IF NOT EXISTS big_five_reliability (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trait VARCHAR(30) NOT NULL UNIQUE,

    -- Reliability Metrics
    cronbach_alpha NUMERIC(5,4),
    contributing_competencies INTEGER,         -- Number of competencies mapped to this trait
    total_items INTEGER,                       -- Total questions across all contributing competencies
    sample_size INTEGER,

    -- Status
    reliability_status VARCHAR(30),

    -- Metadata
    last_calculated_at TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_big_five_trait
        CHECK (trait IN ('OPENNESS', 'CONSCIENTIOUSNESS', 'EXTRAVERSION', 'AGREEABLENESS', 'EMOTIONAL_STABILITY')),
    CONSTRAINT chk_big_five_alpha_range
        CHECK (cronbach_alpha IS NULL OR (cronbach_alpha >= -1 AND cronbach_alpha <= 1)),
    CONSTRAINT chk_big_five_reliability_status
        CHECK (reliability_status IS NULL OR reliability_status IN ('RELIABLE', 'ACCEPTABLE', 'UNRELIABLE', 'INSUFFICIENT_DATA'))
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_big_five_reliability_trait ON big_five_reliability(trait);
CREATE INDEX IF NOT EXISTS idx_big_five_reliability_status ON big_five_reliability(reliability_status);

-- ============================================
-- INITIALIZE ITEM STATISTICS FOR EXISTING QUESTIONS
-- All existing questions start in PROBATION status
-- ============================================
INSERT INTO item_statistics (id, question_id, validity_status, response_count)
SELECT gen_random_uuid(), id, 'PROBATION', 0
FROM assessment_questions
WHERE NOT EXISTS (
    SELECT 1 FROM item_statistics WHERE question_id = assessment_questions.id
);

-- ============================================
-- INITIALIZE COMPETENCY RELIABILITY RECORDS
-- All existing competencies start with INSUFFICIENT_DATA status
-- ============================================
INSERT INTO competency_reliability (id, competency_id, reliability_status)
SELECT gen_random_uuid(), id, 'INSUFFICIENT_DATA'
FROM competencies
WHERE NOT EXISTS (
    SELECT 1 FROM competency_reliability WHERE competency_id = competencies.id
);

-- ============================================
-- INITIALIZE BIG FIVE RELIABILITY RECORDS
-- One record per Big Five trait
-- ============================================
INSERT INTO big_five_reliability (id, trait, reliability_status)
VALUES
    (gen_random_uuid(), 'OPENNESS', 'INSUFFICIENT_DATA'),
    (gen_random_uuid(), 'CONSCIENTIOUSNESS', 'INSUFFICIENT_DATA'),
    (gen_random_uuid(), 'EXTRAVERSION', 'INSUFFICIENT_DATA'),
    (gen_random_uuid(), 'AGREEABLENESS', 'INSUFFICIENT_DATA'),
    (gen_random_uuid(), 'EMOTIONAL_STABILITY', 'INSUFFICIENT_DATA')
ON CONFLICT (trait) DO NOTHING;

-- ============================================
-- COMMENTS FOR DOCUMENTATION
-- ============================================
COMMENT ON TABLE item_statistics IS 'Psychometric metrics for assessment questions - tracks difficulty index, discrimination index, and validity status';
COMMENT ON COLUMN item_statistics.difficulty_index IS 'p-value: Average normalized score (0.0-1.0). < 0.2 = too hard, > 0.9 = too easy';
COMMENT ON COLUMN item_statistics.discrimination_index IS 'Point-Biserial correlation (rpb): -1.0 to 1.0. < 0 = toxic, >= 0.3 = excellent';
COMMENT ON COLUMN item_statistics.validity_status IS 'PROBATION (new), ACTIVE (validated), FLAGGED_FOR_REVIEW, RETIRED (deactivated)';

COMMENT ON TABLE competency_reliability IS 'Cronbach''s Alpha and internal consistency metrics for competencies';
COMMENT ON COLUMN competency_reliability.cronbach_alpha IS 'Internal consistency coefficient. >= 0.7 = reliable, 0.6-0.7 = acceptable, < 0.6 = unreliable';
COMMENT ON COLUMN competency_reliability.alpha_if_deleted IS 'JSONB map of questionId -> alpha. If value > cronbach_alpha, removing that item improves reliability';

COMMENT ON TABLE big_five_reliability IS 'Cronbach''s Alpha at Big Five personality trait level, aggregated from contributing competencies';
