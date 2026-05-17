-- V33: Create competency_passports table for persistent Competency Passport storage
-- Replaces in-memory ConcurrentHashMap mock in PassportServiceImpl
-- Enables Delta Testing (JOB_FIT gap analysis vs. existing OVERVIEW scores)

CREATE TABLE competency_passports (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version           BIGINT NOT NULL DEFAULT 0,
    clerk_user_id     VARCHAR(100) NOT NULL,
    competency_scores JSONB NOT NULL DEFAULT '{}'::jsonb,
    big_five_profile  JSONB,
    last_assessed     TIMESTAMP NOT NULL,
    expires_at        TIMESTAMP NOT NULL,
    source_result_id  UUID,
    created_at        TIMESTAMP NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_passport_clerk_user_id UNIQUE (clerk_user_id)
);

-- Primary lookup: by Clerk user ID (covered by unique constraint index)

-- Expiration queries: find non-expired passports
CREATE INDEX idx_passport_expires_at ON competency_passports (expires_at);

-- Traceability: link passport back to the TestResult that produced it
CREATE INDEX idx_passport_source_result_id ON competency_passports (source_result_id);

-- GIN index on competency_scores JSONB for key-existence queries
CREATE INDEX idx_passport_competency_scores ON competency_passports USING GIN (competency_scores);
