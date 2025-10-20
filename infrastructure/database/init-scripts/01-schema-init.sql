-- ================================================================
-- SKILLSOFT ASSESSMENT PLATFORM - DATABASE INITIALIZATION SCRIPT
-- ================================================================
-- Author: SkillSoft Development Team
-- Date: March 20, 2024
-- Purpose: Initialize database schema for the SkillSoft Assessment Platform

-- ----------------------------------------------------------------
-- 1. CREATE EXTENSION FOR UUID GENERATION
-- ----------------------------------------------------------------

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ----------------------------------------------------------------
-- 2. CREATE ENUM TYPES (USING VARCHAR FOR JPA COMPATIBILITY)
-- ----------------------------------------------------------------

-- We're using VARCHAR instead of PostgreSQL ENUMs for better compatibility with JPA/Hibernate
-- This allows the Java @Enumerated(EnumType.STRING) to work properly without type conflicts
-- These comments document the valid values for each "enum-like" column

/* Competency categorization:
   - COGNITIVE
   - INTERPERSONAL
   - LEADERSHIP
   - ADAPTABILITY
   - EMOTIONAL_INTELLIGENCE
   - COMMUNICATION
   - COLLABORATION
   - CRITICAL_THINKING
   - TIME_MANAGEMENT
*/

/* Proficiency assessment levels:
   - NOVICE
   - DEVELOPING
   - PROFICIENT
   - ADVANCED
   - EXPERT
*/

/* Approval workflow status:
   - DRAFT
   - PENDING_REVIEW
   - APPROVED
   - REJECTED
   - ARCHIVED
   - UNDER_REVISION
*/

/* Behavioral indicator observability:
   - DIRECTLY_OBSERVABLE
   - PARTIALLY_OBSERVABLE
   - INFERRED
   - SELF_REPORTED
   - REQUIRES_DOCUMENTATION
*/

/* Measurement approaches for behavioral indicators:
   - FREQUENCY
   - QUALITY
   - IMPACT
   - CONSISTENCY
   - IMPROVEMENT
*/

/* Assessment question formats:
   - LIKERT_SCALE
   - SITUATIONAL_JUDGMENT
   - BEHAVIORAL_EXAMPLE
   - MULTIPLE_CHOICE
   - CAPABILITY_ASSESSMENT
   - SELF_REFLECTION
   - PEER_FEEDBACK
   - FREQUENCY_SCALE
*/

/* Question difficulty classification:
   - FOUNDATIONAL
   - INTERMEDIATE
   - ADVANCED
   - EXPERT
   - SPECIALIZED
*/

/* Standard mapping type:
   - ESCO
   - ONET
   - BIG_FIVE
   - ORGANIZATIONAL_FRAMEWORK
   - INDUSTRY_STANDARD
   - REGULATORY_REQUIREMENT
   - PROFESSIONAL_CERTIFICATION
   - COMPETENCY_MODEL
*/

/* Mapping confidence level:
   - VERIFIED
   - HIGH_CONFIDENCE
   - MODERATE_CONFIDENCE
   - LOW_CONFIDENCE
   - PRELIMINARY
*/

-- ----------------------------------------------------------------
-- 3. CREATE TABLES
-- ----------------------------------------------------------------

-- Core competency definitions
CREATE TABLE IF NOT EXISTS competencies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT NOT NULL,
    category VARCHAR(50) NOT NULL,
    level VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    approval_status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Business constraints
    CONSTRAINT competency_name_min_length CHECK (LENGTH(TRIM(name)) >= 3),
    CONSTRAINT competency_description_min_length CHECK (LENGTH(TRIM(description)) >= 10)
);

-- Behavioral indicators for competency measurement
CREATE TABLE IF NOT EXISTS behavioral_indicators (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    competency_id UUID NOT NULL REFERENCES competencies(id) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    observability_level VARCHAR(50) NOT NULL,
    measurement_type VARCHAR(50) NOT NULL,
    weight DECIMAL(5,2) NOT NULL,
    examples TEXT,
    counter_examples TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    approval_status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    order_index INTEGER NOT NULL,
    
    -- Business constraints
    CONSTRAINT behavioral_indicator_weight_range CHECK (weight >= 0.01 AND weight <= 1.00),
    CONSTRAINT behavioral_indicator_title_min_length CHECK (LENGTH(TRIM(title)) >= 5),
    CONSTRAINT behavioral_indicator_description_min_length CHECK (LENGTH(TRIM(description)) >= 10),
    CONSTRAINT behavioral_indicator_order_positive CHECK (order_index > 0),
    CONSTRAINT behavioral_indicator_order_max CHECK (order_index <= 20),
    
    -- Unique ordering per competency
    CONSTRAINT behavioral_indicator_competency_order UNIQUE (competency_id, order_index)
);

-- Assessment questions for competency evaluation
CREATE TABLE IF NOT EXISTS assessment_questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    behavioral_indicator_id UUID NOT NULL REFERENCES behavioral_indicators(id) ON DELETE CASCADE,
    question_text TEXT NOT NULL,
    question_type VARCHAR(50) NOT NULL,
    answer_options JSONB, -- JSON structure for answer options
    scoring_rubric TEXT NOT NULL,
    time_limit INTEGER,
    difficulty_level VARCHAR(50) NOT NULL DEFAULT 'INTERMEDIATE',
    discrimination_index DECIMAL(5,2),
    is_active BOOLEAN NOT NULL DEFAULT true,
    order_index INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Business constraints
    CONSTRAINT assessment_question_text_min_length CHECK (LENGTH(TRIM(question_text)) >= 10),
    CONSTRAINT assessment_question_time_limit_positive CHECK (time_limit IS NULL OR time_limit > 0),
    CONSTRAINT assessment_question_order_positive CHECK (order_index > 0),
    
    -- Unique ordering per behavioral indicator
    CONSTRAINT assessment_question_indicator_order UNIQUE (behavioral_indicator_id, order_index)
);

-- Standard mappings for competencies
CREATE TABLE IF NOT EXISTS standard_mappings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    competency_id UUID NOT NULL REFERENCES competencies(id) ON DELETE CASCADE,
    standard_type VARCHAR(50) NOT NULL,
    standard_code VARCHAR(50) NOT NULL,
    standard_name VARCHAR(200) NOT NULL,
    mapping_confidence VARCHAR(50) NOT NULL DEFAULT 'PRELIMINARY',
    validated_by UUID, -- Reference to user who validated the mapping
    validation_date TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Business constraints
    CONSTRAINT standard_mapping_code_min_length CHECK (LENGTH(TRIM(standard_code)) >= 1),
    CONSTRAINT standard_mapping_name_min_length CHECK (LENGTH(TRIM(standard_name)) >= 3),
    
    -- Unique mapping per competency and standard
    CONSTRAINT standard_mapping_unique UNIQUE (competency_id, standard_type, standard_code)
);

-- ----------------------------------------------------------------
-- 4. CREATE INDEXES
-- ----------------------------------------------------------------

-- Competency lookup optimization
CREATE INDEX idx_competencies_category ON competencies(category);
CREATE INDEX idx_competencies_level ON competencies(level);
CREATE INDEX idx_competencies_approval_status ON competencies(approval_status);
CREATE INDEX idx_competencies_active_approved ON competencies(is_active, approval_status) 
    WHERE is_active = true AND approval_status = 'APPROVED';

-- Behavioral indicator query optimization
CREATE INDEX idx_behavioral_indicators_competency ON behavioral_indicators(competency_id);
CREATE INDEX idx_behavioral_indicators_measurement ON behavioral_indicators(measurement_type);
CREATE INDEX idx_behavioral_indicators_observability ON behavioral_indicators(observability_level);
CREATE INDEX idx_behavioral_indicators_approval ON behavioral_indicators(approval_status);
CREATE INDEX idx_behavioral_indicators_active_approved ON behavioral_indicators(is_active, approval_status) 
    WHERE is_active = true AND approval_status = 'APPROVED';

-- Assessment question lookup optimization
CREATE INDEX idx_assessment_questions_indicator ON assessment_questions(behavioral_indicator_id);
CREATE INDEX idx_assessment_questions_type ON assessment_questions(question_type);
CREATE INDEX idx_assessment_questions_difficulty ON assessment_questions(difficulty_level);
CREATE INDEX idx_assessment_questions_active ON assessment_questions(is_active) 
    WHERE is_active = true;

-- Standard mapping optimization
CREATE INDEX idx_standard_mappings_competency ON standard_mappings(competency_id);
CREATE INDEX idx_standard_mappings_type_code ON standard_mappings(standard_type, standard_code);
CREATE INDEX idx_standard_mappings_confidence ON standard_mappings(mapping_confidence);

-- ----------------------------------------------------------------
-- 5. TRIGGERS FOR AUDIT AND MAINTENANCE
-- ----------------------------------------------------------------

-- Update the last_modified timestamp on competency updates
CREATE OR REPLACE FUNCTION update_competency_last_modified()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_modified = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER competency_last_modified_trigger
BEFORE UPDATE ON competencies
FOR EACH ROW
EXECUTE FUNCTION update_competency_last_modified();

-- Update the last_modified timestamp on assessment question updates
CREATE OR REPLACE FUNCTION update_assessment_question_last_modified()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_modified = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER assessment_question_last_modified_trigger
BEFORE UPDATE ON assessment_questions
FOR EACH ROW
EXECUTE FUNCTION update_assessment_question_last_modified();

-- ----------------------------------------------------------------
-- 6. FUNCTIONS FOR BUSINESS LOGIC
-- ----------------------------------------------------------------

-- Function to validate that behavioral indicator weights sum to 1.0 for a competency
CREATE OR REPLACE FUNCTION validate_behavioral_indicator_weights()
RETURNS TRIGGER AS $$
DECLARE
    total_weight DECIMAL(5,2);
    competency_id_val UUID;
BEGIN
    -- Get the competency_id to check
    IF TG_OP = 'INSERT' OR TG_OP = 'UPDATE' THEN
        competency_id_val := NEW.competency_id;
    ELSE
        competency_id_val := OLD.competency_id;
    END IF;
    
    -- Calculate the total weight for this competency
    SELECT COALESCE(SUM(weight), 0)
    INTO total_weight
    FROM behavioral_indicators
    WHERE competency_id = competency_id_val
    AND (id <> OLD.id OR OLD.id IS NULL);
    
    -- Add the new weight if this is an INSERT or UPDATE
    IF TG_OP = 'INSERT' OR TG_OP = 'UPDATE' THEN
        total_weight := total_weight + NEW.weight;
    END IF;
    
    -- Check if total weight exceeds 1.0 (with a small tolerance for floating point)
    IF total_weight > 1.001 THEN
        RAISE EXCEPTION 'Total weight for behavioral indicators in a competency cannot exceed 1.0. Current total: %', total_weight;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER check_behavioral_indicator_weights
BEFORE INSERT OR UPDATE ON behavioral_indicators
FOR EACH ROW
EXECUTE FUNCTION validate_behavioral_indicator_weights();

-- Function to reorder indicators when one is deleted
CREATE OR REPLACE FUNCTION reorder_behavioral_indicators_after_delete()
RETURNS TRIGGER AS $$
BEGIN
    -- Update order_index for remaining indicators to ensure continuity
    UPDATE behavioral_indicators
    SET order_index = order_index - 1
    WHERE competency_id = OLD.competency_id
    AND order_index > OLD.order_index;
    
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER reorder_indicators_trigger
AFTER DELETE ON behavioral_indicators
FOR EACH ROW
EXECUTE FUNCTION reorder_behavioral_indicators_after_delete();

-- Similar function for assessment questions
CREATE OR REPLACE FUNCTION reorder_assessment_questions_after_delete()
RETURNS TRIGGER AS $$
BEGIN
    -- Update order_index for remaining questions to ensure continuity
    UPDATE assessment_questions
    SET order_index = order_index - 1
    WHERE behavioral_indicator_id = OLD.behavioral_indicator_id
    AND order_index > OLD.order_index;
    
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER reorder_questions_trigger
AFTER DELETE ON assessment_questions
FOR EACH ROW
EXECUTE FUNCTION reorder_assessment_questions_after_delete();

COMMENT ON DATABASE skillsoft_assessment_dev IS 'SkillSoft Assessment Platform Database for Development';
COMMENT ON SCHEMA public IS 'Standard public schema for SkillSoft Assessment Platform';

-- Add comments to tables
COMMENT ON TABLE competencies IS 'Core competencies that represent measurable skills, abilities or knowledge';
COMMENT ON TABLE behavioral_indicators IS 'Observable behaviors that demonstrate competency proficiency';
COMMENT ON TABLE assessment_questions IS 'Questions used to evaluate behavioral indicators';
COMMENT ON TABLE standard_mappings IS 'Mappings between competencies and international standards';