-- ================================================================
-- PSYCHOMETRIC TEST DATA FOR VISUALIZATION TESTING
-- ================================================================
-- Version: V10__populate_psychometric_test_data.sql
-- Description: Populates psychometric tables with diverse test data
--              to verify dashboard visualizations work correctly
-- Author: SkillSoft Team
-- Date: 2025-12-17
--
-- Test data distribution:
-- - Toxic zone (rpb < 0): 3 items
-- - Critical discrimination (rpb < 0.1): 4 items
-- - Warning discrimination (0.1 <= rpb < 0.25): 5 items
-- - Too easy (p > 0.9): 4 items
-- - Too hard (p < 0.2): 4 items
-- - Optimal zone: 30+ items
-- ================================================================

-- ================================================================
-- PHASE 1: CLEAR EXISTING METRICS (reset to known state)
-- ================================================================
UPDATE item_statistics SET
    difficulty_index = NULL,
    discrimination_index = NULL,
    validity_status = 'PROBATION',
    difficulty_flag = NULL,
    discrimination_flag = NULL,
    response_count = 0,
    last_calculated_at = NULL,
    previous_discrimination_index = NULL,
    status_change_history = NULL;

-- ================================================================
-- PHASE 2: UPDATE SPECIFIC ITEMS WITH TEST DATA
-- Using OFFSET/LIMIT to target specific questions
-- ================================================================

-- TOXIC ZONE: Negative discrimination (3 items)
-- These should appear in the red zone below y=0 on scatter plot
UPDATE item_statistics SET
    difficulty_index = 0.45,
    discrimination_index = -0.15,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NEGATIVE',
    response_count = 127,
    last_calculated_at = CURRENT_TIMESTAMP - interval '3 days',
    previous_discrimination_index = 0.02,
    status_change_history = '[{"from": "PROBATION", "to": "FLAGGED_FOR_REVIEW", "timestamp": "2025-12-14T10:00:00", "reason": "Negative discrimination detected"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 0);

UPDATE item_statistics SET
    difficulty_index = 0.52,
    discrimination_index = -0.08,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NEGATIVE',
    response_count = 89,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days',
    previous_discrimination_index = 0.05
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 1);

UPDATE item_statistics SET
    difficulty_index = 0.38,
    discrimination_index = -0.12,
    validity_status = 'RETIRED',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NEGATIVE',
    response_count = 156,
    last_calculated_at = CURRENT_TIMESTAMP - interval '7 days',
    previous_discrimination_index = -0.05,
    status_change_history = '[{"from": "FLAGGED_FOR_REVIEW", "to": "RETIRED", "timestamp": "2025-12-10T14:30:00", "reason": "Retired - persistent negative discrimination"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 2);

-- CRITICAL ZONE: Very low discrimination (4 items)
-- rpb between 0 and 0.1
UPDATE item_statistics SET
    difficulty_index = 0.55,
    discrimination_index = 0.05,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'NONE',
    discrimination_flag = 'CRITICAL',
    response_count = 98,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 3);

UPDATE item_statistics SET
    difficulty_index = 0.48,
    discrimination_index = 0.08,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'NONE',
    discrimination_flag = 'CRITICAL',
    response_count = 112,
    last_calculated_at = CURRENT_TIMESTAMP - interval '3 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 4);

UPDATE item_statistics SET
    difficulty_index = 0.62,
    discrimination_index = 0.03,
    validity_status = 'PROBATION',
    difficulty_flag = 'NONE',
    discrimination_flag = 'CRITICAL',
    response_count = 67,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 5);

UPDATE item_statistics SET
    difficulty_index = 0.41,
    discrimination_index = 0.09,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'NONE',
    discrimination_flag = 'CRITICAL',
    response_count = 143,
    last_calculated_at = CURRENT_TIMESTAMP - interval '4 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 6);

-- WARNING ZONE: Low discrimination (5 items)
-- rpb between 0.1 and 0.25
UPDATE item_statistics SET
    difficulty_index = 0.58,
    discrimination_index = 0.15,
    validity_status = 'PROBATION',
    difficulty_flag = 'NONE',
    discrimination_flag = 'WARNING',
    response_count = 87,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 7);

UPDATE item_statistics SET
    difficulty_index = 0.44,
    discrimination_index = 0.18,
    validity_status = 'PROBATION',
    difficulty_flag = 'NONE',
    discrimination_flag = 'WARNING',
    response_count = 95,
    last_calculated_at = CURRENT_TIMESTAMP - interval '3 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 8);

UPDATE item_statistics SET
    difficulty_index = 0.67,
    discrimination_index = 0.12,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'WARNING',
    response_count = 201,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 9);

UPDATE item_statistics SET
    difficulty_index = 0.53,
    discrimination_index = 0.22,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'WARNING',
    response_count = 178,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 10);

UPDATE item_statistics SET
    difficulty_index = 0.49,
    discrimination_index = 0.24,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'WARNING',
    response_count = 156,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 11);

-- TOO EASY: High p-value > 0.9 (4 items)
-- Should appear in the violet zone on scatter plot
UPDATE item_statistics SET
    difficulty_index = 0.92,
    discrimination_index = 0.28,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'TOO_EASY',
    discrimination_flag = 'NONE',
    response_count = 234,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 12);

UPDATE item_statistics SET
    difficulty_index = 0.95,
    discrimination_index = 0.15,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'TOO_EASY',
    discrimination_flag = 'WARNING',
    response_count = 189,
    last_calculated_at = CURRENT_TIMESTAMP - interval '3 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 13);

UPDATE item_statistics SET
    difficulty_index = 0.91,
    discrimination_index = 0.32,
    validity_status = 'ACTIVE',
    difficulty_flag = 'TOO_EASY',
    discrimination_flag = 'NONE',
    response_count = 276,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 14);

UPDATE item_statistics SET
    difficulty_index = 0.97,
    discrimination_index = 0.08,
    validity_status = 'RETIRED',
    difficulty_flag = 'TOO_EASY',
    discrimination_flag = 'CRITICAL',
    response_count = 145,
    last_calculated_at = CURRENT_TIMESTAMP - interval '5 days',
    status_change_history = '[{"from": "FLAGGED_FOR_REVIEW", "to": "RETIRED", "timestamp": "2025-12-12T09:00:00", "reason": "Too easy - 97% correct rate"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 15);

-- TOO HARD: Low p-value < 0.2 (4 items)
-- Should appear in the blue zone on scatter plot
UPDATE item_statistics SET
    difficulty_index = 0.12,
    discrimination_index = 0.22,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'TOO_HARD',
    discrimination_flag = 'WARNING',
    response_count = 167,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 16);

UPDATE item_statistics SET
    difficulty_index = 0.08,
    discrimination_index = 0.31,
    validity_status = 'PROBATION',
    difficulty_flag = 'TOO_HARD',
    discrimination_flag = 'NONE',
    response_count = 134,
    last_calculated_at = CURRENT_TIMESTAMP - interval '3 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 17);

UPDATE item_statistics SET
    difficulty_index = 0.15,
    discrimination_index = 0.18,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'TOO_HARD',
    discrimination_flag = 'WARNING',
    response_count = 98,
    last_calculated_at = CURRENT_TIMESTAMP - interval '4 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 18);

UPDATE item_statistics SET
    difficulty_index = 0.05,
    discrimination_index = 0.12,
    validity_status = 'RETIRED',
    difficulty_flag = 'TOO_HARD',
    discrimination_flag = 'WARNING',
    response_count = 87,
    last_calculated_at = CURRENT_TIMESTAMP - interval '6 days',
    status_change_history = '[{"from": "FLAGGED_FOR_REVIEW", "to": "RETIRED", "timestamp": "2025-12-11T16:00:00", "reason": "Too difficult - only 5% correct rate"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 19);

-- OPTIMAL ZONE: Good metrics - ACTIVE status (30 items)
-- These will fill the green zone on scatter plot
UPDATE item_statistics SET
    difficulty_index = 0.65, discrimination_index = 0.42, validity_status = 'ACTIVE',
    difficulty_flag = 'NONE', discrimination_flag = 'NONE', response_count = 312,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 20);

UPDATE item_statistics SET
    difficulty_index = 0.58, discrimination_index = 0.38, validity_status = 'ACTIVE',
    difficulty_flag = 'NONE', discrimination_flag = 'NONE', response_count = 287,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 21);

UPDATE item_statistics SET
    difficulty_index = 0.72, discrimination_index = 0.45, validity_status = 'ACTIVE',
    difficulty_flag = 'NONE', discrimination_flag = 'NONE', response_count = 298,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 22);

UPDATE item_statistics SET
    difficulty_index = 0.48, discrimination_index = 0.52, validity_status = 'ACTIVE',
    difficulty_flag = 'NONE', discrimination_flag = 'NONE', response_count = 345,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 23);

UPDATE item_statistics SET
    difficulty_index = 0.55, discrimination_index = 0.35, validity_status = 'ACTIVE',
    difficulty_flag = 'NONE', discrimination_flag = 'NONE', response_count = 267,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 24);

UPDATE item_statistics SET
    difficulty_index = 0.62, discrimination_index = 0.41, validity_status = 'ACTIVE',
    difficulty_flag = 'NONE', discrimination_flag = 'NONE', response_count = 289,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 25);

UPDATE item_statistics SET
    difficulty_index = 0.45, discrimination_index = 0.48, validity_status = 'ACTIVE',
    difficulty_flag = 'NONE', discrimination_flag = 'NONE', response_count = 312,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 26);

UPDATE item_statistics SET
    difficulty_index = 0.68, discrimination_index = 0.36, validity_status = 'ACTIVE',
    difficulty_flag = 'NONE', discrimination_flag = 'NONE', response_count = 234,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 27);

UPDATE item_statistics SET
    difficulty_index = 0.52, discrimination_index = 0.44, validity_status = 'ACTIVE',
    difficulty_flag = 'NONE', discrimination_flag = 'NONE', response_count = 256,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 28);

UPDATE item_statistics SET
    difficulty_index = 0.75, discrimination_index = 0.39, validity_status = 'ACTIVE',
    difficulty_flag = 'NONE', discrimination_flag = 'NONE', response_count = 278,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 29);

-- More optimal items with varying metrics
UPDATE item_statistics SET
    difficulty_index = 0.42, discrimination_index = 0.37, validity_status = 'ACTIVE',
    difficulty_flag = 'NONE', discrimination_flag = 'NONE', response_count = 189,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 30);

UPDATE item_statistics SET
    difficulty_index = 0.56, discrimination_index = 0.43, validity_status = 'ACTIVE',
    difficulty_flag = 'NONE', discrimination_flag = 'NONE', response_count = 234,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 31);

UPDATE item_statistics SET
    difficulty_index = 0.63, discrimination_index = 0.29, validity_status = 'ACTIVE',
    difficulty_flag = 'NONE', discrimination_flag = 'NONE', response_count = 267,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 32);

UPDATE item_statistics SET
    difficulty_index = 0.71, discrimination_index = 0.34, validity_status = 'ACTIVE',
    difficulty_flag = 'NONE', discrimination_flag = 'NONE', response_count = 198,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 33);

UPDATE item_statistics SET
    difficulty_index = 0.38, discrimination_index = 0.41, validity_status = 'ACTIVE',
    difficulty_flag = 'NONE', discrimination_flag = 'NONE', response_count = 212,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 34);

UPDATE item_statistics SET
    difficulty_index = 0.59, discrimination_index = 0.38, validity_status = 'ACTIVE',
    difficulty_flag = 'NONE', discrimination_flag = 'NONE', response_count = 245,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 35);

UPDATE item_statistics SET
    difficulty_index = 0.66, discrimination_index = 0.32, validity_status = 'ACTIVE',
    difficulty_flag = 'NONE', discrimination_flag = 'NONE', response_count = 178,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 36);

UPDATE item_statistics SET
    difficulty_index = 0.47, discrimination_index = 0.46, validity_status = 'ACTIVE',
    difficulty_flag = 'NONE', discrimination_flag = 'NONE', response_count = 289,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 37);

UPDATE item_statistics SET
    difficulty_index = 0.54, discrimination_index = 0.28, validity_status = 'ACTIVE',
    difficulty_flag = 'NONE', discrimination_flag = 'NONE', response_count = 156,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 38);

UPDATE item_statistics SET
    difficulty_index = 0.73, discrimination_index = 0.35, validity_status = 'ACTIVE',
    difficulty_flag = 'NONE', discrimination_flag = 'NONE', response_count = 234,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 39);

-- PROBATION items (recently added, good metrics but gathering data)
UPDATE item_statistics SET
    difficulty_index = 0.58, discrimination_index = 0.32, validity_status = 'PROBATION',
    difficulty_flag = 'NONE', discrimination_flag = 'NONE', response_count = 52,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 40);

UPDATE item_statistics SET
    difficulty_index = 0.62, discrimination_index = 0.28, validity_status = 'PROBATION',
    difficulty_flag = 'NONE', discrimination_flag = 'NONE', response_count = 61,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 41);

UPDATE item_statistics SET
    difficulty_index = 0.55, discrimination_index = 0.35, validity_status = 'PROBATION',
    difficulty_flag = 'NONE', discrimination_flag = 'NONE', response_count = 48,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 42);

UPDATE item_statistics SET
    difficulty_index = 0.49, discrimination_index = 0.31, validity_status = 'PROBATION',
    difficulty_flag = 'NONE', discrimination_flag = 'NONE', response_count = 55,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 43);

UPDATE item_statistics SET
    difficulty_index = 0.67, discrimination_index = 0.29, validity_status = 'PROBATION',
    difficulty_flag = 'NONE', discrimination_flag = 'NONE', response_count = 63,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 44);

-- Edge of zones (marginal items)
UPDATE item_statistics SET
    difficulty_index = 0.82, discrimination_index = 0.26, validity_status = 'ACTIVE',
    difficulty_flag = 'NONE', discrimination_flag = 'NONE', response_count = 198,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 45);

UPDATE item_statistics SET
    difficulty_index = 0.22, discrimination_index = 0.27, validity_status = 'ACTIVE',
    difficulty_flag = 'NONE', discrimination_flag = 'NONE', response_count = 187,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 46);

UPDATE item_statistics SET
    difficulty_index = 0.85, discrimination_index = 0.33, validity_status = 'ACTIVE',
    difficulty_flag = 'NONE', discrimination_flag = 'NONE', response_count = 212,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 47);

UPDATE item_statistics SET
    difficulty_index = 0.25, discrimination_index = 0.38, validity_status = 'ACTIVE',
    difficulty_flag = 'NONE', discrimination_flag = 'NONE', response_count = 176,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 48);

UPDATE item_statistics SET
    difficulty_index = 0.78, discrimination_index = 0.31, validity_status = 'ACTIVE',
    difficulty_flag = 'NONE', discrimination_flag = 'NONE', response_count = 223,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 49);

-- ================================================================
-- PHASE 3: UPDATE COMPETENCY RELIABILITY
-- ================================================================

-- Competency 1: Analytical Thinking - RELIABLE (excellent)
-- NOTE: alpha_if_deleted requires UUID keys - using question UUIDs from assessment_questions
-- For simplicity, we'll leave alpha_if_deleted NULL in test data (calculated dynamically at runtime)
UPDATE competency_reliability SET
    cronbach_alpha = 0.82,
    sample_size = 287,
    item_count = 20,
    reliability_status = 'RELIABLE',
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days',
    alpha_if_deleted = NULL
WHERE competency_id = '11111111-1111-1111-1111-000000000001';

-- Competency 2: Problem Solving - RELIABLE
UPDATE competency_reliability SET
    cronbach_alpha = 0.78,
    sample_size = 256,
    item_count = 20,
    reliability_status = 'RELIABLE',
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE competency_id = '11111111-1111-1111-1111-000000000002';

-- Competency 3: Decision Making - ACCEPTABLE (borderline)
UPDATE competency_reliability SET
    cronbach_alpha = 0.67,
    sample_size = 198,
    item_count = 20,
    reliability_status = 'ACCEPTABLE',
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE competency_id = '11111111-1111-1111-1111-000000000003';

-- Competency 4: Relationship Building - RELIABLE
UPDATE competency_reliability SET
    cronbach_alpha = 0.75,
    sample_size = 234,
    item_count = 20,
    reliability_status = 'RELIABLE',
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE competency_id = '11111111-1111-1111-1111-000000000004';

-- Competency 5: Conflict Resolution - UNRELIABLE (poor)
UPDATE competency_reliability SET
    cronbach_alpha = 0.52,
    sample_size = 167,
    item_count = 20,
    reliability_status = 'UNRELIABLE',
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE competency_id = '11111111-1111-1111-1111-000000000005';

-- Competency 6: Influence - ACCEPTABLE
UPDATE competency_reliability SET
    cronbach_alpha = 0.64,
    sample_size = 189,
    item_count = 20,
    reliability_status = 'ACCEPTABLE',
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE competency_id = '11111111-1111-1111-1111-000000000006';

-- Competency 7: Team Leadership - RELIABLE (excellent)
UPDATE competency_reliability SET
    cronbach_alpha = 0.85,
    sample_size = 312,
    item_count = 20,
    reliability_status = 'RELIABLE',
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE competency_id = '11111111-1111-1111-1111-000000000007';

-- Competency 8: Strategic Vision - RELIABLE
UPDATE competency_reliability SET
    cronbach_alpha = 0.73,
    sample_size = 145,
    item_count = 20,
    reliability_status = 'RELIABLE',
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE competency_id = '11111111-1111-1111-1111-000000000008';

-- Competency 9: Delegation - UNRELIABLE
UPDATE competency_reliability SET
    cronbach_alpha = 0.58,
    sample_size = 134,
    item_count = 20,
    reliability_status = 'UNRELIABLE',
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE competency_id = '11111111-1111-1111-1111-000000000009';

-- Competency 10: Oral Communication - RELIABLE
UPDATE competency_reliability SET
    cronbach_alpha = 0.71,
    sample_size = 198,
    item_count = 20,
    reliability_status = 'RELIABLE',
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE competency_id = '11111111-1111-1111-1111-000000000010';

-- Competency 11: Written Communication - RELIABLE
UPDATE competency_reliability SET
    cronbach_alpha = 0.79,
    sample_size = 223,
    item_count = 20,
    reliability_status = 'RELIABLE',
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE competency_id = '11111111-1111-1111-1111-000000000011';

-- Competency 12: Active Listening - ACCEPTABLE
UPDATE competency_reliability SET
    cronbach_alpha = 0.62,
    sample_size = 156,
    item_count = 20,
    reliability_status = 'ACCEPTABLE',
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE competency_id = '11111111-1111-1111-1111-000000000012';

-- Competency 13: Time Management - RELIABLE (excellent)
UPDATE competency_reliability SET
    cronbach_alpha = 0.88,
    sample_size = 289,
    item_count = 20,
    reliability_status = 'RELIABLE',
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE competency_id = '11111111-1111-1111-1111-000000000013';

-- Competency 14: Stress Tolerance - UNRELIABLE
UPDATE competency_reliability SET
    cronbach_alpha = 0.55,
    sample_size = 98,
    item_count = 20,
    reliability_status = 'UNRELIABLE',
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE competency_id = '11111111-1111-1111-1111-000000000014';

-- Competency 15: Self-Development - ACCEPTABLE
UPDATE competency_reliability SET
    cronbach_alpha = 0.69,
    sample_size = 178,
    item_count = 20,
    reliability_status = 'ACCEPTABLE',
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE competency_id = '11111111-1111-1111-1111-000000000015';

-- Competency 16: Innovation - INSUFFICIENT_DATA (new competency)
UPDATE competency_reliability SET
    cronbach_alpha = NULL,
    sample_size = 23,
    item_count = 20,
    reliability_status = 'INSUFFICIENT_DATA',
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE competency_id = '11111111-1111-1111-1111-000000000016';

-- Competency 17-27: Update with RELIABLE status
UPDATE competency_reliability SET
    cronbach_alpha = 0.74,
    sample_size = 187,
    item_count = 20,
    reliability_status = 'RELIABLE',
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE competency_id = '11111111-1111-1111-1111-000000000017';

UPDATE competency_reliability SET
    cronbach_alpha = 0.77,
    sample_size = 201,
    item_count = 20,
    reliability_status = 'RELIABLE',
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE competency_id = '11111111-1111-1111-1111-000000000018';

UPDATE competency_reliability SET
    cronbach_alpha = 0.72,
    sample_size = 165,
    item_count = 20,
    reliability_status = 'RELIABLE',
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE competency_id = '11111111-1111-1111-1111-000000000019';

UPDATE competency_reliability SET
    cronbach_alpha = 0.81,
    sample_size = 234,
    item_count = 20,
    reliability_status = 'RELIABLE',
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE competency_id = '11111111-1111-1111-1111-000000000020';

UPDATE competency_reliability SET
    cronbach_alpha = 0.76,
    sample_size = 198,
    item_count = 20,
    reliability_status = 'RELIABLE',
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE competency_id = '11111111-1111-1111-1111-000000000021';

UPDATE competency_reliability SET
    cronbach_alpha = 0.70,
    sample_size = 156,
    item_count = 20,
    reliability_status = 'RELIABLE',
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE competency_id = '11111111-1111-1111-1111-000000000022';

UPDATE competency_reliability SET
    cronbach_alpha = 0.83,
    sample_size = 267,
    item_count = 20,
    reliability_status = 'RELIABLE',
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE competency_id = '11111111-1111-1111-1111-000000000023';

UPDATE competency_reliability SET
    cronbach_alpha = 0.75,
    sample_size = 189,
    item_count = 20,
    reliability_status = 'RELIABLE',
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE competency_id = '11111111-1111-1111-1111-000000000024';

UPDATE competency_reliability SET
    cronbach_alpha = 0.78,
    sample_size = 212,
    item_count = 20,
    reliability_status = 'RELIABLE',
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE competency_id = '11111111-1111-1111-1111-000000000025';

UPDATE competency_reliability SET
    cronbach_alpha = 0.71,
    sample_size = 145,
    item_count = 20,
    reliability_status = 'RELIABLE',
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE competency_id = '11111111-1111-1111-1111-000000000026';

UPDATE competency_reliability SET
    cronbach_alpha = 0.79,
    sample_size = 178,
    item_count = 20,
    reliability_status = 'RELIABLE',
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE competency_id = '11111111-1111-1111-1111-000000000027';

-- ================================================================
-- PHASE 4: UPDATE BIG FIVE RELIABILITY
-- ================================================================

-- OPENNESS - RELIABLE (many competencies map here)
UPDATE big_five_reliability SET
    cronbach_alpha = 0.76,
    contributing_competencies = 8,
    total_items = 160,
    sample_size = 287,
    reliability_status = 'RELIABLE',
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE trait = 'OPENNESS';

-- CONSCIENTIOUSNESS - ACCEPTABLE (borderline)
UPDATE big_five_reliability SET
    cronbach_alpha = 0.68,
    contributing_competencies = 6,
    total_items = 120,
    sample_size = 234,
    reliability_status = 'ACCEPTABLE',
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE trait = 'CONSCIENTIOUSNESS';

-- EXTRAVERSION - RELIABLE
UPDATE big_five_reliability SET
    cronbach_alpha = 0.81,
    contributing_competencies = 5,
    total_items = 100,
    sample_size = 256,
    reliability_status = 'RELIABLE',
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE trait = 'EXTRAVERSION';

-- AGREEABLENESS - UNRELIABLE (needs improvement)
UPDATE big_five_reliability SET
    cronbach_alpha = 0.59,
    contributing_competencies = 4,
    total_items = 80,
    sample_size = 189,
    reliability_status = 'UNRELIABLE',
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE trait = 'AGREEABLENESS';

-- EMOTIONAL_STABILITY - RELIABLE
UPDATE big_five_reliability SET
    cronbach_alpha = 0.74,
    contributing_competencies = 4,
    total_items = 80,
    sample_size = 198,
    reliability_status = 'RELIABLE',
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE trait = 'EMOTIONAL_STABILITY';

-- ================================================================
-- TEST DATA SUMMARY
-- ================================================================
-- Item Statistics (50 items with specific metrics):
--   - 3 TOXIC (rpb < 0): FLAGGED_FOR_REVIEW/RETIRED
--   - 4 CRITICAL (rpb 0-0.1): FLAGGED_FOR_REVIEW/PROBATION
--   - 5 WARNING (rpb 0.1-0.25): PROBATION/ACTIVE
--   - 4 TOO_EASY (p > 0.9): FLAGGED_FOR_REVIEW/ACTIVE/RETIRED
--   - 4 TOO_HARD (p < 0.2): FLAGGED_FOR_REVIEW/PROBATION/RETIRED
--   - 30 OPTIMAL (p 0.2-0.8, rpb >= 0.25): ACTIVE/PROBATION
--
-- Competency Reliability (27 competencies):
--   - 3 UNRELIABLE (alpha < 0.6): IDs 5, 9, 14
--   - 4 ACCEPTABLE (alpha 0.6-0.7): IDs 3, 6, 12, 15
--   - 1 INSUFFICIENT_DATA: ID 16
--   - 19 RELIABLE (alpha >= 0.7)
--
-- Big Five Reliability:
--   - 3 RELIABLE: Openness, Extraversion, Emotional Stability
--   - 1 ACCEPTABLE: Conscientiousness
--   - 1 UNRELIABLE: Agreeableness
-- ================================================================
