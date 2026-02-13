-- ================================================================
-- PSYCHOMETRIC FAKE DATA GENERATION - ENHANCED REALISTIC DATASET
-- ================================================================
-- Version: V13__ensure_all_big_five_available.sql (Rewritten)
-- Description: Generates deterministic, statistically realistic psychometric
--              data simulating 6-12 months of real-world test usage.
--
-- Key Features:
--   - Hash-based bucketing for true reproducibility
--   - Difficulty-discrimination correlation (r ~ -0.3)
--   - Comprehensive trend analysis items (improving/degrading)
--   - All boundary value edge cases for UI testing
--   - Complete status lifecycle histories
--
-- Distribution Strategy (540 items total):
--   ACTIVE: ~65% (351 items) - Validated items with good metrics
--   PROBATION: ~12% (65 items) - New items gathering data
--   FLAGGED_FOR_REVIEW: ~15% (81 items) - ENHANCED for dashboard demo
--   RETIRED: ~8% (43 items) - Historical failures with audit trail
--
-- Flagged Items Breakdown:
--   - 8 NEGATIVE discrimination (rpb < 0) - Toxic zone
--   - 14 CRITICAL discrimination (0 <= rpb < 0.10)
--   - 20 WARNING discrimination (0.10 <= rpb < 0.25)
--   - 14 TOO_EASY (p > 0.90)
--   - 14 TOO_HARD (p < 0.20)
--   - 10 Combined issues (multiple flags)
--   - 12 Improving trends (was bad, now good)
--   - 10 Degrading trends (was good, now bad)
--
-- Edge Cases:
--   - 15 boundary value items at exact thresholds
--   - Items at p=0.90, p=0.20, rpb=0.25, rpb=0.10, rpb=0.00, rpb=-0.01
--   - Response count edges: 49, 50, 51, 500
--
-- Competency Reliability Distribution (27 competencies):
--   RELIABLE: 20 (alpha 0.70-0.86)
--   ACCEPTABLE: 4 (alpha 0.63-0.67)
--   UNRELIABLE: 2 (alpha 0.52-0.58)
--   INSUFFICIENT_DATA: 1
--
-- Big Five Traits: ALL RELIABLE (required for test creation)
--
-- Author: SkillSoft Team
-- Date: 2025-12-31 (Enhanced Version)
-- ================================================================

-- ================================================================
-- PHASE 1: RESET ITEM STATISTICS TO KNOWN STATE
-- ================================================================
-- Clear all existing metrics to ensure clean slate for regeneration

UPDATE item_statistics SET
    difficulty_index = NULL,
    discrimination_index = NULL,
    validity_status = 'PROBATION',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NONE',
    response_count = 0,
    last_calculated_at = NULL,
    previous_discrimination_index = NULL,
    status_change_history = NULL,
    distractor_efficiency = NULL;

-- ================================================================
-- PHASE 2: UPDATE BIG FIVE RELIABILITY - ALL TRAITS MUST BE RELIABLE
-- ================================================================
-- Critical: All Big Five traits must be RELIABLE for test creation to work.
-- Values represent aggregated reliability across contributing competencies.

UPDATE big_five_reliability SET
    cronbach_alpha = 0.78,
    contributing_competencies = 8,
    total_items = 160,
    sample_size = 312,
    reliability_status = 'RELIABLE',
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE trait = 'OPENNESS';

UPDATE big_five_reliability SET
    cronbach_alpha = 0.82,
    contributing_competencies = 6,
    total_items = 120,
    sample_size = 298,
    reliability_status = 'RELIABLE',
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE trait = 'CONSCIENTIOUSNESS';

UPDATE big_five_reliability SET
    cronbach_alpha = 0.76,
    contributing_competencies = 3,
    total_items = 60,
    sample_size = 289,
    reliability_status = 'RELIABLE',
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE trait = 'EXTRAVERSION';

UPDATE big_five_reliability SET
    cronbach_alpha = 0.74,
    contributing_competencies = 7,
    total_items = 140,
    sample_size = 267,
    reliability_status = 'RELIABLE',
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE trait = 'AGREEABLENESS';

UPDATE big_five_reliability SET
    cronbach_alpha = 0.79,
    contributing_competencies = 3,
    total_items = 60,
    sample_size = 278,
    reliability_status = 'RELIABLE',
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE trait = 'EMOTIONAL_STABILITY';

-- ================================================================
-- PHASE 3: UPDATE COMPETENCY RELIABILITY
-- ================================================================
-- Distribution: 20 RELIABLE, 4 ACCEPTABLE, 2 UNRELIABLE, 1 INSUFFICIENT_DATA
-- Grouped by Big Five trait mapping for realism

-- OPENNESS Competencies (C1, C2, C8, C10, C11, C22, C23, C24) - 8 competencies
UPDATE competency_reliability SET cronbach_alpha = 0.82, sample_size = 287, item_count = 20, reliability_status = 'RELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '2 days' WHERE competency_id = '11111111-1111-1111-1111-000000000001';
UPDATE competency_reliability SET cronbach_alpha = 0.78, sample_size = 256, item_count = 20, reliability_status = 'RELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '3 days' WHERE competency_id = '11111111-1111-1111-1111-000000000002';
UPDATE competency_reliability SET cronbach_alpha = 0.73, sample_size = 198, item_count = 20, reliability_status = 'RELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '2 days' WHERE competency_id = '11111111-1111-1111-1111-000000000008';
UPDATE competency_reliability SET cronbach_alpha = 0.71, sample_size = 189, item_count = 20, reliability_status = 'RELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '4 days' WHERE competency_id = '11111111-1111-1111-1111-000000000010';
UPDATE competency_reliability SET cronbach_alpha = 0.75, sample_size = 201, item_count = 20, reliability_status = 'RELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '2 days' WHERE competency_id = '11111111-1111-1111-1111-000000000011';
UPDATE competency_reliability SET cronbach_alpha = 0.79, sample_size = 234, item_count = 20, reliability_status = 'RELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '3 days' WHERE competency_id = '11111111-1111-1111-1111-000000000022';
UPDATE competency_reliability SET cronbach_alpha = 0.76, sample_size = 212, item_count = 20, reliability_status = 'RELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '2 days' WHERE competency_id = '11111111-1111-1111-1111-000000000023';
UPDATE competency_reliability SET cronbach_alpha = 0.74, sample_size = 178, item_count = 20, reliability_status = 'RELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '5 days' WHERE competency_id = '11111111-1111-1111-1111-000000000024';

-- CONSCIENTIOUSNESS Competencies (C3, C9, C17, C25, C26, C27) - 6 competencies
-- Include 1 ACCEPTABLE (C3) and 1 UNRELIABLE (C9) for dashboard variety
UPDATE competency_reliability SET cronbach_alpha = 0.67, sample_size = 178, item_count = 20, reliability_status = 'ACCEPTABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '2 days' WHERE competency_id = '11111111-1111-1111-1111-000000000003';
UPDATE competency_reliability SET cronbach_alpha = 0.58, sample_size = 134, item_count = 20, reliability_status = 'UNRELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '3 days' WHERE competency_id = '11111111-1111-1111-1111-000000000009';
UPDATE competency_reliability SET cronbach_alpha = 0.80, sample_size = 256, item_count = 20, reliability_status = 'RELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '2 days' WHERE competency_id = '11111111-1111-1111-1111-000000000017';
UPDATE competency_reliability SET cronbach_alpha = 0.76, sample_size = 223, item_count = 20, reliability_status = 'RELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '4 days' WHERE competency_id = '11111111-1111-1111-1111-000000000025';
UPDATE competency_reliability SET cronbach_alpha = 0.79, sample_size = 234, item_count = 20, reliability_status = 'RELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '2 days' WHERE competency_id = '11111111-1111-1111-1111-000000000026';
UPDATE competency_reliability SET cronbach_alpha = 0.82, sample_size = 267, item_count = 20, reliability_status = 'RELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '3 days' WHERE competency_id = '11111111-1111-1111-1111-000000000027';

-- EXTRAVERSION Competencies (C6, C7, C16) - 3 competencies
-- C16 is INSUFFICIENT_DATA (new competency)
UPDATE competency_reliability SET cronbach_alpha = 0.74, sample_size = 198, item_count = 20, reliability_status = 'RELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '2 days' WHERE competency_id = '11111111-1111-1111-1111-000000000006';
UPDATE competency_reliability SET cronbach_alpha = 0.86, sample_size = 345, item_count = 20, reliability_status = 'RELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '1 day' WHERE competency_id = '11111111-1111-1111-1111-000000000007';
UPDATE competency_reliability SET cronbach_alpha = NULL, sample_size = 28, item_count = 20, reliability_status = 'INSUFFICIENT_DATA', last_calculated_at = CURRENT_TIMESTAMP - interval '3 days' WHERE competency_id = '11111111-1111-1111-1111-000000000016';

-- AGREEABLENESS Competencies (C4, C5, C14, C18, C19, C20, C21) - 7 competencies
-- Include 1 UNRELIABLE (C5) and 1 ACCEPTABLE (C14) for dashboard variety
UPDATE competency_reliability SET cronbach_alpha = 0.77, sample_size = 256, item_count = 20, reliability_status = 'RELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '2 days' WHERE competency_id = '11111111-1111-1111-1111-000000000004';
UPDATE competency_reliability SET cronbach_alpha = 0.52, sample_size = 98, item_count = 20, reliability_status = 'UNRELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '4 days' WHERE competency_id = '11111111-1111-1111-1111-000000000005';
UPDATE competency_reliability SET cronbach_alpha = 0.63, sample_size = 145, item_count = 20, reliability_status = 'ACCEPTABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '3 days' WHERE competency_id = '11111111-1111-1111-1111-000000000014';
UPDATE competency_reliability SET cronbach_alpha = 0.71, sample_size = 198, item_count = 20, reliability_status = 'RELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '2 days' WHERE competency_id = '11111111-1111-1111-1111-000000000018';
UPDATE competency_reliability SET cronbach_alpha = 0.79, sample_size = 267, item_count = 20, reliability_status = 'RELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '2 days' WHERE competency_id = '11111111-1111-1111-1111-000000000019';
UPDATE competency_reliability SET cronbach_alpha = 0.74, sample_size = 189, item_count = 20, reliability_status = 'RELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '3 days' WHERE competency_id = '11111111-1111-1111-1111-000000000020';
UPDATE competency_reliability SET cronbach_alpha = 0.72, sample_size = 176, item_count = 20, reliability_status = 'RELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '2 days' WHERE competency_id = '11111111-1111-1111-1111-000000000021';

-- EMOTIONAL_STABILITY Competencies (C12, C13, C15) - 3 competencies
-- Include 1 ACCEPTABLE (C12) and 1 ACCEPTABLE (C15) for dashboard variety
UPDATE competency_reliability SET cronbach_alpha = 0.65, sample_size = 156, item_count = 20, reliability_status = 'ACCEPTABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '3 days' WHERE competency_id = '11111111-1111-1111-1111-000000000012';
UPDATE competency_reliability SET cronbach_alpha = 0.77, sample_size = 212, item_count = 20, reliability_status = 'RELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '2 days' WHERE competency_id = '11111111-1111-1111-1111-000000000013';
UPDATE competency_reliability SET cronbach_alpha = 0.64, sample_size = 167, item_count = 20, reliability_status = 'ACCEPTABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '2 days' WHERE competency_id = '11111111-1111-1111-1111-000000000015';

-- ================================================================
-- PHASE 4: ITEM STATISTICS - SET BASELINE
-- ================================================================
-- First, set all items to a baseline ACTIVE state with good metrics
-- Then override specific items with flagged/problematic data

UPDATE item_statistics SET
    difficulty_index = 0.55,
    discrimination_index = 0.38,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NONE',
    response_count = 185,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days',
    status_change_history = '[{"from_status": null, "to_status": "PROBATION", "changed_at": "2025-06-15T10:00:00", "reason": "Item created"}, {"from_status": "PROBATION", "to_status": "ACTIVE", "changed_at": "2025-08-01T14:30:00", "reason": "Reached 50 responses with acceptable metrics"}]'::jsonb;

-- ================================================================
-- PHASE 4.1: TOXIC ZONE - NEGATIVE DISCRIMINATION (8 items)
-- ================================================================
-- rpb < 0: Items that actively harm test validity
-- High-performing candidates answer incorrectly, low-performers answer correctly

UPDATE item_statistics SET
    difficulty_index = 0.48,
    discrimination_index = -0.15,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NEGATIVE',
    response_count = 156,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days',
    previous_discrimination_index = 0.03,
    status_change_history = '[{"from_status": null, "to_status": "PROBATION", "changed_at": "2025-06-01T10:00:00", "reason": "Item created"}, {"from_status": "PROBATION", "to_status": "ACTIVE", "changed_at": "2025-07-15T14:30:00", "reason": "Initial activation"}, {"from_status": "ACTIVE", "to_status": "FLAGGED_FOR_REVIEW", "changed_at": "2025-11-20T09:15:00", "reason": "Negative discrimination detected - rpb dropped below 0"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 0);

UPDATE item_statistics SET
    difficulty_index = 0.52,
    discrimination_index = -0.09,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NEGATIVE',
    response_count = 134,
    last_calculated_at = CURRENT_TIMESTAMP - interval '3 days',
    previous_discrimination_index = 0.05,
    status_change_history = '[{"from_status": null, "to_status": "PROBATION", "changed_at": "2025-06-10T10:00:00", "reason": "Item created"}, {"from_status": "PROBATION", "to_status": "ACTIVE", "changed_at": "2025-07-25T14:30:00", "reason": "Initial activation"}, {"from_status": "ACTIVE", "to_status": "FLAGGED_FOR_REVIEW", "changed_at": "2025-11-18T11:20:00", "reason": "Negative discrimination emerging"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 1);

UPDATE item_statistics SET
    difficulty_index = 0.41,
    discrimination_index = -0.12,
    validity_status = 'RETIRED',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NEGATIVE',
    response_count = 234,
    last_calculated_at = CURRENT_TIMESTAMP - interval '45 days',
    previous_discrimination_index = -0.06,
    status_change_history = '[{"from_status": null, "to_status": "PROBATION", "changed_at": "2025-04-10T10:00:00", "reason": "Item created"}, {"from_status": "PROBATION", "to_status": "ACTIVE", "changed_at": "2025-05-25T14:30:00", "reason": "Initial activation"}, {"from_status": "ACTIVE", "to_status": "FLAGGED_FOR_REVIEW", "changed_at": "2025-09-15T09:15:00", "reason": "Persistent negative discrimination"}, {"from_status": "FLAGGED_FOR_REVIEW", "to_status": "RETIRED", "changed_at": "2025-11-01T16:00:00", "reason": "Failed to improve after 45-day review - permanently retired"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 2);

UPDATE item_statistics SET
    difficulty_index = 0.55,
    discrimination_index = -0.08,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NEGATIVE',
    response_count = 112,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day',
    previous_discrimination_index = 0.08,
    status_change_history = '[{"from_status": null, "to_status": "PROBATION", "changed_at": "2025-08-15T10:00:00", "reason": "Item created"}, {"from_status": "PROBATION", "to_status": "ACTIVE", "changed_at": "2025-09-28T14:30:00", "reason": "Initial activation"}, {"from_status": "ACTIVE", "to_status": "FLAGGED_FOR_REVIEW", "changed_at": "2025-12-28T10:45:00", "reason": "Discrimination went negative"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 3);

UPDATE item_statistics SET
    difficulty_index = 0.38,
    discrimination_index = -0.14,
    validity_status = 'RETIRED',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NEGATIVE',
    response_count = 189,
    last_calculated_at = CURRENT_TIMESTAMP - interval '60 days',
    previous_discrimination_index = -0.10,
    status_change_history = '[{"from_status": null, "to_status": "PROBATION", "changed_at": "2025-05-01T10:00:00", "reason": "Item created"}, {"from_status": "PROBATION", "to_status": "ACTIVE", "changed_at": "2025-06-15T14:30:00", "reason": "Initial activation"}, {"from_status": "ACTIVE", "to_status": "FLAGGED_FOR_REVIEW", "changed_at": "2025-08-10T10:00:00", "reason": "Toxic discrimination detected"}, {"from_status": "FLAGGED_FOR_REVIEW", "to_status": "RETIRED", "changed_at": "2025-10-01T14:00:00", "reason": "Permanently retired - consistently negative rpb"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 4);

UPDATE item_statistics SET
    difficulty_index = 0.62,
    discrimination_index = -0.05,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NEGATIVE',
    response_count = 98,
    last_calculated_at = CURRENT_TIMESTAMP - interval '4 days',
    previous_discrimination_index = 0.12,
    status_change_history = '[{"from_status": null, "to_status": "PROBATION", "changed_at": "2025-09-01T10:00:00", "reason": "Item created"}, {"from_status": "PROBATION", "to_status": "ACTIVE", "changed_at": "2025-10-15T14:30:00", "reason": "Initial activation"}, {"from_status": "ACTIVE", "to_status": "FLAGGED_FOR_REVIEW", "changed_at": "2025-12-25T09:00:00", "reason": "Negative discrimination - requires review"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 5);

UPDATE item_statistics SET
    difficulty_index = 0.44,
    discrimination_index = -0.18,
    validity_status = 'RETIRED',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NEGATIVE',
    response_count = 267,
    last_calculated_at = CURRENT_TIMESTAMP - interval '75 days',
    previous_discrimination_index = -0.12,
    status_change_history = '[{"from_status": null, "to_status": "PROBATION", "changed_at": "2025-03-15T10:00:00", "reason": "Item created"}, {"from_status": "PROBATION", "to_status": "ACTIVE", "changed_at": "2025-04-28T14:30:00", "reason": "Initial activation"}, {"from_status": "ACTIVE", "to_status": "FLAGGED_FOR_REVIEW", "changed_at": "2025-07-20T09:15:00", "reason": "Severe negative discrimination"}, {"from_status": "FLAGGED_FOR_REVIEW", "to_status": "RETIRED", "changed_at": "2025-10-15T16:00:00", "reason": "Permanently retired after extended review"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 6);

UPDATE item_statistics SET
    difficulty_index = 0.58,
    discrimination_index = -0.03,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NEGATIVE',
    response_count = 87,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days',
    previous_discrimination_index = 0.15,
    status_change_history = '[{"from_status": null, "to_status": "PROBATION", "changed_at": "2025-10-01T10:00:00", "reason": "Item created"}, {"from_status": "PROBATION", "to_status": "ACTIVE", "changed_at": "2025-11-12T14:30:00", "reason": "Initial activation"}, {"from_status": "ACTIVE", "to_status": "FLAGGED_FOR_REVIEW", "changed_at": "2025-12-28T11:30:00", "reason": "Just went negative - monitoring"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 7);

-- ================================================================
-- PHASE 4.2: CRITICAL DISCRIMINATION (14 items)
-- ================================================================
-- 0 <= rpb < 0.10: Very poor discrimination, needs review

UPDATE item_statistics SET
    difficulty_index = 0.58,
    discrimination_index = 0.06,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'NONE',
    discrimination_flag = 'CRITICAL',
    response_count = 145,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days',
    previous_discrimination_index = 0.12,
    status_change_history = '[{"from_status": null, "to_status": "PROBATION", "changed_at": "2025-07-01T10:00:00", "reason": "Item created"}, {"from_status": "PROBATION", "to_status": "ACTIVE", "changed_at": "2025-08-15T14:30:00", "reason": "Initial activation"}, {"from_status": "ACTIVE", "to_status": "FLAGGED_FOR_REVIEW", "changed_at": "2025-12-01T09:00:00", "reason": "Critical discrimination - item fails to differentiate"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 10);

UPDATE item_statistics SET
    difficulty_index = 0.49,
    discrimination_index = 0.08,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'NONE',
    discrimination_flag = 'CRITICAL',
    response_count = 167,
    last_calculated_at = CURRENT_TIMESTAMP - interval '3 days',
    previous_discrimination_index = 0.15
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 11);

UPDATE item_statistics SET
    difficulty_index = 0.65,
    discrimination_index = 0.04,
    validity_status = 'PROBATION',
    difficulty_flag = 'NONE',
    discrimination_flag = 'CRITICAL',
    response_count = 42,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day',
    status_change_history = '[{"from_status": null, "to_status": "PROBATION", "changed_at": "2025-12-01T10:00:00", "reason": "Item created - early metrics showing critical discrimination"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 12);

UPDATE item_statistics SET
    difficulty_index = 0.43,
    discrimination_index = 0.09,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'NONE',
    discrimination_flag = 'CRITICAL',
    response_count = 189,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 13);

UPDATE item_statistics SET
    difficulty_index = 0.71,
    discrimination_index = 0.03,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'NONE',
    discrimination_flag = 'CRITICAL',
    response_count = 123,
    last_calculated_at = CURRENT_TIMESTAMP - interval '4 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 14);

UPDATE item_statistics SET
    difficulty_index = 0.55,
    discrimination_index = 0.07,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'NONE',
    discrimination_flag = 'CRITICAL',
    response_count = 178,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 15);

UPDATE item_statistics SET
    difficulty_index = 0.38,
    discrimination_index = 0.05,
    validity_status = 'RETIRED',
    difficulty_flag = 'NONE',
    discrimination_flag = 'CRITICAL',
    response_count = 256,
    last_calculated_at = CURRENT_TIMESTAMP - interval '30 days',
    status_change_history = '[{"from_status": null, "to_status": "PROBATION", "changed_at": "2025-06-01T10:00:00", "reason": "Item created"}, {"from_status": "PROBATION", "to_status": "ACTIVE", "changed_at": "2025-07-15T14:30:00", "reason": "Initial activation"}, {"from_status": "ACTIVE", "to_status": "FLAGGED_FOR_REVIEW", "changed_at": "2025-10-15T10:00:00", "reason": "Critical discrimination"}, {"from_status": "FLAGGED_FOR_REVIEW", "to_status": "RETIRED", "changed_at": "2025-11-30T14:00:00", "reason": "No improvement after review period"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 16);

UPDATE item_statistics SET
    difficulty_index = 0.62,
    discrimination_index = 0.06,
    validity_status = 'PROBATION',
    difficulty_flag = 'NONE',
    discrimination_flag = 'CRITICAL',
    response_count = 38,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 17);

UPDATE item_statistics SET
    difficulty_index = 0.47,
    discrimination_index = 0.08,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'NONE',
    discrimination_flag = 'CRITICAL',
    response_count = 134,
    last_calculated_at = CURRENT_TIMESTAMP - interval '3 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 18);

UPDATE item_statistics SET
    difficulty_index = 0.59,
    discrimination_index = 0.04,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'NONE',
    discrimination_flag = 'CRITICAL',
    response_count = 156,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 19);

UPDATE item_statistics SET
    difficulty_index = 0.68,
    discrimination_index = 0.07,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'NONE',
    discrimination_flag = 'CRITICAL',
    response_count = 112,
    last_calculated_at = CURRENT_TIMESTAMP - interval '5 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 20);

UPDATE item_statistics SET
    difficulty_index = 0.52,
    discrimination_index = 0.09,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'NONE',
    discrimination_flag = 'CRITICAL',
    response_count = 145,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 21);

UPDATE item_statistics SET
    difficulty_index = 0.35,
    discrimination_index = 0.02,
    validity_status = 'RETIRED',
    difficulty_flag = 'NONE',
    discrimination_flag = 'CRITICAL',
    response_count = 198,
    last_calculated_at = CURRENT_TIMESTAMP - interval '55 days',
    status_change_history = '[{"from_status": "ACTIVE", "to_status": "FLAGGED_FOR_REVIEW", "changed_at": "2025-09-20T10:00:00", "reason": "Critical discrimination"}, {"from_status": "FLAGGED_FOR_REVIEW", "to_status": "RETIRED", "changed_at": "2025-11-05T14:00:00", "reason": "Retired due to persistent poor performance"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 22);

UPDATE item_statistics SET
    difficulty_index = 0.74,
    discrimination_index = 0.01,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'NONE',
    discrimination_flag = 'CRITICAL',
    response_count = 89,
    last_calculated_at = CURRENT_TIMESTAMP - interval '3 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 23);

-- ================================================================
-- PHASE 4.3: WARNING DISCRIMINATION (20 items)
-- ================================================================
-- 0.10 <= rpb < 0.25: Marginal discrimination, needs monitoring

UPDATE item_statistics SET
    difficulty_index = 0.61,
    discrimination_index = 0.16,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'WARNING',
    response_count = 234,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 30);

UPDATE item_statistics SET
    difficulty_index = 0.47,
    discrimination_index = 0.19,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'WARNING',
    response_count = 198,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 31);

UPDATE item_statistics SET
    difficulty_index = 0.69,
    discrimination_index = 0.12,
    validity_status = 'PROBATION',
    difficulty_flag = 'NONE',
    discrimination_flag = 'WARNING',
    response_count = 45,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 32);

UPDATE item_statistics SET
    difficulty_index = 0.54,
    discrimination_index = 0.22,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'WARNING',
    response_count = 178,
    last_calculated_at = CURRENT_TIMESTAMP - interval '3 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 33);

UPDATE item_statistics SET
    difficulty_index = 0.51,
    discrimination_index = 0.24,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'WARNING',
    response_count = 156,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 34);

UPDATE item_statistics SET
    difficulty_index = 0.73,
    discrimination_index = 0.14,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'WARNING',
    response_count = 212,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 35);

UPDATE item_statistics SET
    difficulty_index = 0.42,
    discrimination_index = 0.18,
    validity_status = 'PROBATION',
    difficulty_flag = 'NONE',
    discrimination_flag = 'WARNING',
    response_count = 48,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 36);

UPDATE item_statistics SET
    difficulty_index = 0.58,
    discrimination_index = 0.21,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'WARNING',
    response_count = 189,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 37);

UPDATE item_statistics SET
    difficulty_index = 0.65,
    discrimination_index = 0.13,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'WARNING',
    response_count = 167,
    last_calculated_at = CURRENT_TIMESTAMP - interval '4 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 38);

UPDATE item_statistics SET
    difficulty_index = 0.49,
    discrimination_index = 0.17,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'WARNING',
    response_count = 201,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 39);

UPDATE item_statistics SET
    difficulty_index = 0.56,
    discrimination_index = 0.20,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'WARNING',
    response_count = 178,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 40);

UPDATE item_statistics SET
    difficulty_index = 0.38,
    discrimination_index = 0.15,
    validity_status = 'PROBATION',
    difficulty_flag = 'NONE',
    discrimination_flag = 'WARNING',
    response_count = 35,
    last_calculated_at = CURRENT_TIMESTAMP - interval '3 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 41);

UPDATE item_statistics SET
    difficulty_index = 0.71,
    discrimination_index = 0.11,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'WARNING',
    response_count = 223,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 42);

UPDATE item_statistics SET
    difficulty_index = 0.63,
    discrimination_index = 0.23,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'WARNING',
    response_count = 145,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 43);

UPDATE item_statistics SET
    difficulty_index = 0.45,
    discrimination_index = 0.16,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'WARNING',
    response_count = 189,
    last_calculated_at = CURRENT_TIMESTAMP - interval '3 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 44);

UPDATE item_statistics SET
    difficulty_index = 0.52,
    discrimination_index = 0.19,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'WARNING',
    response_count = 167,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 45);

UPDATE item_statistics SET
    difficulty_index = 0.67,
    discrimination_index = 0.14,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'WARNING',
    response_count = 198,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 46);

UPDATE item_statistics SET
    difficulty_index = 0.59,
    discrimination_index = 0.22,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'WARNING',
    response_count = 156,
    last_calculated_at = CURRENT_TIMESTAMP - interval '4 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 47);

UPDATE item_statistics SET
    difficulty_index = 0.34,
    discrimination_index = 0.10,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'WARNING',
    response_count = 212,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 48);

UPDATE item_statistics SET
    difficulty_index = 0.78,
    discrimination_index = 0.18,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'WARNING',
    response_count = 134,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 49);

-- ================================================================
-- PHASE 4.4: TOO EASY (14 items)
-- ================================================================
-- Difficulty index p > 0.90: Most respondents succeed

UPDATE item_statistics SET
    difficulty_index = 0.92,
    discrimination_index = 0.28,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'TOO_EASY',
    discrimination_flag = 'NONE',
    response_count = 267,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days',
    status_change_history = '[{"from_status": "ACTIVE", "to_status": "FLAGGED_FOR_REVIEW", "changed_at": "2025-12-15T10:00:00", "reason": "92% success rate - item too easy"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 60);

UPDATE item_statistics SET
    difficulty_index = 0.95,
    discrimination_index = 0.15,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'TOO_EASY',
    discrimination_flag = 'WARNING',
    response_count = 234,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 61);

UPDATE item_statistics SET
    difficulty_index = 0.91,
    discrimination_index = 0.32,
    validity_status = 'ACTIVE',
    difficulty_flag = 'TOO_EASY',
    discrimination_flag = 'NONE',
    response_count = 312,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 62);

UPDATE item_statistics SET
    difficulty_index = 0.97,
    discrimination_index = 0.08,
    validity_status = 'RETIRED',
    difficulty_flag = 'TOO_EASY',
    discrimination_flag = 'CRITICAL',
    response_count = 189,
    last_calculated_at = CURRENT_TIMESTAMP - interval '40 days',
    status_change_history = '[{"from_status": null, "to_status": "PROBATION", "changed_at": "2025-06-01T10:00:00", "reason": "Item created"}, {"from_status": "PROBATION", "to_status": "ACTIVE", "changed_at": "2025-07-15T14:30:00", "reason": "Initial activation"}, {"from_status": "ACTIVE", "to_status": "FLAGGED_FOR_REVIEW", "changed_at": "2025-10-01T10:00:00", "reason": "97% success rate - too easy"}, {"from_status": "FLAGGED_FOR_REVIEW", "to_status": "RETIRED", "changed_at": "2025-11-15T14:00:00", "reason": "Retired - provides no differentiation"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 63);

UPDATE item_statistics SET
    difficulty_index = 0.93,
    discrimination_index = 0.19,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'TOO_EASY',
    discrimination_flag = 'WARNING',
    response_count = 245,
    last_calculated_at = CURRENT_TIMESTAMP - interval '3 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 64);

UPDATE item_statistics SET
    difficulty_index = 0.94,
    discrimination_index = 0.22,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'TOO_EASY',
    discrimination_flag = 'WARNING',
    response_count = 198,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 65);

UPDATE item_statistics SET
    difficulty_index = 0.91,
    discrimination_index = 0.27,
    validity_status = 'ACTIVE',
    difficulty_flag = 'TOO_EASY',
    discrimination_flag = 'NONE',
    response_count = 278,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 66);

UPDATE item_statistics SET
    difficulty_index = 0.96,
    discrimination_index = 0.12,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'TOO_EASY',
    discrimination_flag = 'WARNING',
    response_count = 212,
    last_calculated_at = CURRENT_TIMESTAMP - interval '4 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 67);

UPDATE item_statistics SET
    difficulty_index = 0.92,
    discrimination_index = 0.31,
    validity_status = 'ACTIVE',
    difficulty_flag = 'TOO_EASY',
    discrimination_flag = 'NONE',
    response_count = 256,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 68);

UPDATE item_statistics SET
    difficulty_index = 0.98,
    discrimination_index = 0.05,
    validity_status = 'RETIRED',
    difficulty_flag = 'TOO_EASY',
    discrimination_flag = 'CRITICAL',
    response_count = 167,
    last_calculated_at = CURRENT_TIMESTAMP - interval '50 days',
    status_change_history = '[{"from_status": "ACTIVE", "to_status": "FLAGGED_FOR_REVIEW", "changed_at": "2025-09-15T10:00:00", "reason": "98% success rate"}, {"from_status": "FLAGGED_FOR_REVIEW", "to_status": "RETIRED", "changed_at": "2025-11-01T14:00:00", "reason": "Permanently retired - trivial item"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 69);

UPDATE item_statistics SET
    difficulty_index = 0.93,
    discrimination_index = 0.24,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'TOO_EASY',
    discrimination_flag = 'WARNING',
    response_count = 223,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 70);

UPDATE item_statistics SET
    difficulty_index = 0.91,
    discrimination_index = 0.29,
    validity_status = 'ACTIVE',
    difficulty_flag = 'TOO_EASY',
    discrimination_flag = 'NONE',
    response_count = 289,
    last_calculated_at = CURRENT_TIMESTAMP - interval '3 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 71);

UPDATE item_statistics SET
    difficulty_index = 0.94,
    discrimination_index = 0.17,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'TOO_EASY',
    discrimination_flag = 'WARNING',
    response_count = 178,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 72);

UPDATE item_statistics SET
    difficulty_index = 0.99,
    discrimination_index = 0.03,
    validity_status = 'RETIRED',
    difficulty_flag = 'TOO_EASY',
    discrimination_flag = 'CRITICAL',
    response_count = 134,
    last_calculated_at = CURRENT_TIMESTAMP - interval '65 days',
    status_change_history = '[{"from_status": "ACTIVE", "to_status": "RETIRED", "changed_at": "2025-10-25T14:00:00", "reason": "Immediately retired - 99% success rate provides zero differentiation"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 73);

-- ================================================================
-- PHASE 4.5: TOO HARD (14 items)
-- ================================================================
-- Difficulty index p < 0.20: Most respondents fail

UPDATE item_statistics SET
    difficulty_index = 0.12,
    discrimination_index = 0.22,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'TOO_HARD',
    discrimination_flag = 'WARNING',
    response_count = 189,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days',
    status_change_history = '[{"from_status": "ACTIVE", "to_status": "FLAGGED_FOR_REVIEW", "changed_at": "2025-12-10T10:00:00", "reason": "Only 12% correct rate - too difficult for target population"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 80);

UPDATE item_statistics SET
    difficulty_index = 0.08,
    discrimination_index = 0.31,
    validity_status = 'PROBATION',
    difficulty_flag = 'TOO_HARD',
    discrimination_flag = 'NONE',
    response_count = 45,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 81);

UPDATE item_statistics SET
    difficulty_index = 0.15,
    discrimination_index = 0.18,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'TOO_HARD',
    discrimination_flag = 'WARNING',
    response_count = 134,
    last_calculated_at = CURRENT_TIMESTAMP - interval '3 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 82);

UPDATE item_statistics SET
    difficulty_index = 0.05,
    discrimination_index = 0.12,
    validity_status = 'RETIRED',
    difficulty_flag = 'TOO_HARD',
    discrimination_flag = 'WARNING',
    response_count = 98,
    last_calculated_at = CURRENT_TIMESTAMP - interval '55 days',
    status_change_history = '[{"from_status": null, "to_status": "PROBATION", "changed_at": "2025-06-15T10:00:00", "reason": "Item created"}, {"from_status": "PROBATION", "to_status": "ACTIVE", "changed_at": "2025-07-30T14:30:00", "reason": "Initial activation"}, {"from_status": "ACTIVE", "to_status": "FLAGGED_FOR_REVIEW", "changed_at": "2025-09-15T10:00:00", "reason": "Only 5% correct rate"}, {"from_status": "FLAGGED_FOR_REVIEW", "to_status": "RETIRED", "changed_at": "2025-11-01T14:00:00", "reason": "Too difficult for target population - retired"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 83);

UPDATE item_statistics SET
    difficulty_index = 0.11,
    discrimination_index = 0.25,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'TOO_HARD',
    discrimination_flag = 'NONE',
    response_count = 156,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 84);

UPDATE item_statistics SET
    difficulty_index = 0.18,
    discrimination_index = 0.28,
    validity_status = 'ACTIVE',
    difficulty_flag = 'TOO_HARD',
    discrimination_flag = 'NONE',
    response_count = 201,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 85);

UPDATE item_statistics SET
    difficulty_index = 0.07,
    discrimination_index = 0.15,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'TOO_HARD',
    discrimination_flag = 'WARNING',
    response_count = 112,
    last_calculated_at = CURRENT_TIMESTAMP - interval '4 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 86);

UPDATE item_statistics SET
    difficulty_index = 0.14,
    discrimination_index = 0.21,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'TOO_HARD',
    discrimination_flag = 'WARNING',
    response_count = 178,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 87);

UPDATE item_statistics SET
    difficulty_index = 0.09,
    discrimination_index = 0.33,
    validity_status = 'PROBATION',
    difficulty_flag = 'TOO_HARD',
    discrimination_flag = 'NONE',
    response_count = 38,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 88);

UPDATE item_statistics SET
    difficulty_index = 0.16,
    discrimination_index = 0.19,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'TOO_HARD',
    discrimination_flag = 'WARNING',
    response_count = 145,
    last_calculated_at = CURRENT_TIMESTAMP - interval '3 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 89);

UPDATE item_statistics SET
    difficulty_index = 0.03,
    discrimination_index = 0.08,
    validity_status = 'RETIRED',
    difficulty_flag = 'TOO_HARD',
    discrimination_flag = 'CRITICAL',
    response_count = 87,
    last_calculated_at = CURRENT_TIMESTAMP - interval '65 days',
    status_change_history = '[{"from_status": "ACTIVE", "to_status": "FLAGGED_FOR_REVIEW", "changed_at": "2025-09-01T10:00:00", "reason": "3% correct rate - impossibly hard"}, {"from_status": "FLAGGED_FOR_REVIEW", "to_status": "RETIRED", "changed_at": "2025-10-20T14:00:00", "reason": "Retired - item is effectively impossible"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 90);

UPDATE item_statistics SET
    difficulty_index = 0.19,
    discrimination_index = 0.26,
    validity_status = 'ACTIVE',
    difficulty_flag = 'TOO_HARD',
    discrimination_flag = 'NONE',
    response_count = 234,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 91);

UPDATE item_statistics SET
    difficulty_index = 0.06,
    discrimination_index = 0.11,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'TOO_HARD',
    discrimination_flag = 'WARNING',
    response_count = 67,
    last_calculated_at = CURRENT_TIMESTAMP - interval '5 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 92);

UPDATE item_statistics SET
    difficulty_index = 0.01,
    discrimination_index = 0.04,
    validity_status = 'RETIRED',
    difficulty_flag = 'TOO_HARD',
    discrimination_flag = 'CRITICAL',
    response_count = 156,
    last_calculated_at = CURRENT_TIMESTAMP - interval '80 days',
    status_change_history = '[{"from_status": "ACTIVE", "to_status": "RETIRED", "changed_at": "2025-10-10T14:00:00", "reason": "Immediately retired - 1% success rate is statistically impossible to differentiate"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 93);

-- ================================================================
-- PHASE 4.6: ADDITIONAL RETIRED ITEMS (historical failures)
-- ================================================================
-- Complete lifecycle with status_change_history for audit trail

UPDATE item_statistics SET
    difficulty_index = 0.45,
    discrimination_index = -0.18,
    validity_status = 'RETIRED',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NEGATIVE',
    response_count = 298,
    last_calculated_at = CURRENT_TIMESTAMP - interval '70 days',
    previous_discrimination_index = -0.12,
    status_change_history = '[{"from_status": null, "to_status": "PROBATION", "changed_at": "2025-03-01T10:00:00", "reason": "Item created"}, {"from_status": "PROBATION", "to_status": "ACTIVE", "changed_at": "2025-04-15T14:30:00", "reason": "Initial activation"}, {"from_status": "ACTIVE", "to_status": "FLAGGED_FOR_REVIEW", "changed_at": "2025-07-20T09:15:00", "reason": "Negative discrimination emerging"}, {"from_status": "FLAGGED_FOR_REVIEW", "to_status": "RETIRED", "changed_at": "2025-10-15T16:00:00", "reason": "Permanently retired - toxic item confirmed"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 100);

UPDATE item_statistics SET
    difficulty_index = 0.52,
    discrimination_index = 0.02,
    validity_status = 'RETIRED',
    difficulty_flag = 'NONE',
    discrimination_flag = 'CRITICAL',
    response_count = 234,
    last_calculated_at = CURRENT_TIMESTAMP - interval '45 days',
    status_change_history = '[{"from_status": null, "to_status": "PROBATION", "changed_at": "2025-05-15T10:00:00", "reason": "Item created"}, {"from_status": "PROBATION", "to_status": "ACTIVE", "changed_at": "2025-06-28T14:30:00", "reason": "Initial activation"}, {"from_status": "ACTIVE", "to_status": "FLAGGED_FOR_REVIEW", "changed_at": "2025-09-01T10:00:00", "reason": "Critical discrimination - item provides no differentiation"}, {"from_status": "FLAGGED_FOR_REVIEW", "to_status": "RETIRED", "changed_at": "2025-11-10T14:00:00", "reason": "No improvement after review - retired"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 101);

UPDATE item_statistics SET
    difficulty_index = 0.94,
    discrimination_index = 0.04,
    validity_status = 'RETIRED',
    difficulty_flag = 'TOO_EASY',
    discrimination_flag = 'CRITICAL',
    response_count = 178,
    last_calculated_at = CURRENT_TIMESTAMP - interval '35 days',
    status_change_history = '[{"from_status": "ACTIVE", "to_status": "FLAGGED_FOR_REVIEW", "changed_at": "2025-10-20T10:00:00", "reason": "94% success with critical discrimination"}, {"from_status": "FLAGGED_FOR_REVIEW", "to_status": "RETIRED", "changed_at": "2025-11-25T14:00:00", "reason": "Combined issues - retired"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 102);

UPDATE item_statistics SET
    difficulty_index = 0.06,
    discrimination_index = 0.06,
    validity_status = 'RETIRED',
    difficulty_flag = 'TOO_HARD',
    discrimination_flag = 'CRITICAL',
    response_count = 123,
    last_calculated_at = CURRENT_TIMESTAMP - interval '60 days',
    status_change_history = '[{"from_status": "ACTIVE", "to_status": "FLAGGED_FOR_REVIEW", "changed_at": "2025-09-10T10:00:00", "reason": "Too hard with critical discrimination"}, {"from_status": "FLAGGED_FOR_REVIEW", "to_status": "RETIRED", "changed_at": "2025-10-28T14:00:00", "reason": "Combined issues unrepairable - retired"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 103);

UPDATE item_statistics SET
    difficulty_index = 0.39,
    discrimination_index = -0.07,
    validity_status = 'RETIRED',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NEGATIVE',
    response_count = 267,
    last_calculated_at = CURRENT_TIMESTAMP - interval '80 days',
    previous_discrimination_index = -0.02,
    status_change_history = '[{"from_status": "PROBATION", "to_status": "ACTIVE", "changed_at": "2025-04-01T14:30:00", "reason": "Initial activation"}, {"from_status": "ACTIVE", "to_status": "FLAGGED_FOR_REVIEW", "changed_at": "2025-07-15T09:00:00", "reason": "Discrimination went negative"}, {"from_status": "FLAGGED_FOR_REVIEW", "to_status": "RETIRED", "changed_at": "2025-10-08T16:00:00", "reason": "Permanently retired - worsening negative discrimination"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 104);

UPDATE item_statistics SET
    difficulty_index = 0.48,
    discrimination_index = -0.11,
    validity_status = 'RETIRED',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NEGATIVE',
    response_count = 312,
    last_calculated_at = CURRENT_TIMESTAMP - interval '90 days',
    status_change_history = '[{"from_status": "ACTIVE", "to_status": "FLAGGED_FOR_REVIEW", "changed_at": "2025-06-20T10:00:00", "reason": "Toxic discrimination"}, {"from_status": "FLAGGED_FOR_REVIEW", "to_status": "RETIRED", "changed_at": "2025-09-25T14:00:00", "reason": "Retired after extended monitoring"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 105);

UPDATE item_statistics SET
    difficulty_index = 0.88,
    discrimination_index = 0.07,
    validity_status = 'RETIRED',
    difficulty_flag = 'NONE',
    discrimination_flag = 'CRITICAL',
    response_count = 189,
    last_calculated_at = CURRENT_TIMESTAMP - interval '42 days',
    status_change_history = '[{"from_status": "ACTIVE", "to_status": "FLAGGED_FOR_REVIEW", "changed_at": "2025-10-01T10:00:00", "reason": "High success with critical discrimination"}, {"from_status": "FLAGGED_FOR_REVIEW", "to_status": "RETIRED", "changed_at": "2025-11-18T14:00:00", "reason": "Retired - item does not contribute to measurement"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 106);

UPDATE item_statistics SET
    difficulty_index = 0.56,
    discrimination_index = 0.01,
    validity_status = 'RETIRED',
    difficulty_flag = 'NONE',
    discrimination_flag = 'CRITICAL',
    response_count = 256,
    last_calculated_at = CURRENT_TIMESTAMP - interval '52 days',
    status_change_history = '[{"from_status": "ACTIVE", "to_status": "RETIRED", "changed_at": "2025-11-08T14:00:00", "reason": "Fast-tracked retirement - discrimination near zero"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 107);

-- ================================================================
-- PHASE 4.7: PROBATION ITEMS (new items gathering data)
-- ================================================================
-- response_count < 50, mixed early metrics
-- These items are newly created and still gathering initial data

UPDATE item_statistics SET
    difficulty_index = 0.58,
    discrimination_index = 0.34,
    validity_status = 'PROBATION',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NONE',
    response_count = 42,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day',
    status_change_history = '[{"from_status": null, "to_status": "PROBATION", "changed_at": "2025-12-15T10:00:00", "reason": "Item created - gathering initial data"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 120);

UPDATE item_statistics SET
    difficulty_index = 0.62,
    discrimination_index = 0.29,
    validity_status = 'PROBATION',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NONE',
    response_count = 38,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days',
    status_change_history = '[{"from_status": null, "to_status": "PROBATION", "changed_at": "2025-12-12T10:00:00", "reason": "Item created"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 121);

UPDATE item_statistics SET
    difficulty_index = 0.55,
    discrimination_index = 0.36,
    validity_status = 'PROBATION',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NONE',
    response_count = 47,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day',
    status_change_history = '[{"from_status": null, "to_status": "PROBATION", "changed_at": "2025-12-10T10:00:00", "reason": "Item created - promising early metrics"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 122);

UPDATE item_statistics SET
    difficulty_index = 0.49,
    discrimination_index = 0.31,
    validity_status = 'PROBATION',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NONE',
    response_count = 35,
    last_calculated_at = CURRENT_TIMESTAMP - interval '3 days',
    status_change_history = '[{"from_status": null, "to_status": "PROBATION", "changed_at": "2025-12-08T10:00:00", "reason": "Item created"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 123);

UPDATE item_statistics SET
    difficulty_index = 0.67,
    discrimination_index = 0.28,
    validity_status = 'PROBATION',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NONE',
    response_count = 41,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days',
    status_change_history = '[{"from_status": null, "to_status": "PROBATION", "changed_at": "2025-12-11T10:00:00", "reason": "Item created"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 124);

UPDATE item_statistics SET
    difficulty_index = 0.44,
    discrimination_index = 0.33,
    validity_status = 'PROBATION',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NONE',
    response_count = 29,
    last_calculated_at = CURRENT_TIMESTAMP - interval '4 days',
    status_change_history = '[{"from_status": null, "to_status": "PROBATION", "changed_at": "2025-12-05T10:00:00", "reason": "Item created"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 125);

UPDATE item_statistics SET
    difficulty_index = 0.71,
    discrimination_index = 0.25,
    validity_status = 'PROBATION',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NONE',
    response_count = 44,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day',
    status_change_history = '[{"from_status": null, "to_status": "PROBATION", "changed_at": "2025-12-14T10:00:00", "reason": "Item created"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 126);

UPDATE item_statistics SET
    difficulty_index = 0.53,
    discrimination_index = 0.38,
    validity_status = 'PROBATION',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NONE',
    response_count = 48,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days',
    status_change_history = '[{"from_status": null, "to_status": "PROBATION", "changed_at": "2025-12-13T10:00:00", "reason": "Item created - excellent early discrimination"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 127);

UPDATE item_statistics SET
    difficulty_index = 0.60,
    discrimination_index = 0.30,
    validity_status = 'PROBATION',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NONE',
    response_count = 32,
    last_calculated_at = CURRENT_TIMESTAMP - interval '3 days',
    status_change_history = '[{"from_status": null, "to_status": "PROBATION", "changed_at": "2025-12-07T10:00:00", "reason": "Item created"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 128);

UPDATE item_statistics SET
    difficulty_index = 0.46,
    discrimination_index = 0.35,
    validity_status = 'PROBATION',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NONE',
    response_count = 25,
    last_calculated_at = CURRENT_TIMESTAMP - interval '5 days',
    status_change_history = '[{"from_status": null, "to_status": "PROBATION", "changed_at": "2025-12-03T10:00:00", "reason": "Item created"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 129);

-- ================================================================
-- PHASE 4.8: EXCELLENT ACTIVE ITEMS (optimal zone)
-- ================================================================
-- rpb >= 0.35, p 0.40-0.75: Best performing items
-- These are the gold standard items with excellent psychometric properties

UPDATE item_statistics SET
    difficulty_index = 0.65,
    discrimination_index = 0.52,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NONE',
    response_count = 378,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day',
    status_change_history = '[{"from_status": "PROBATION", "to_status": "ACTIVE", "changed_at": "2025-07-20T14:30:00", "reason": "Excellent metrics - top performer"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 150);

UPDATE item_statistics SET
    difficulty_index = 0.58,
    discrimination_index = 0.48,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NONE',
    response_count = 345,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 151);

UPDATE item_statistics SET
    difficulty_index = 0.72,
    discrimination_index = 0.55,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NONE',
    response_count = 312,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 152);

UPDATE item_statistics SET
    difficulty_index = 0.48,
    discrimination_index = 0.45,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NONE',
    response_count = 401,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 153);

UPDATE item_statistics SET
    difficulty_index = 0.55,
    discrimination_index = 0.41,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NONE',
    response_count = 289,
    last_calculated_at = CURRENT_TIMESTAMP - interval '3 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 154);

UPDATE item_statistics SET
    difficulty_index = 0.62,
    discrimination_index = 0.49,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NONE',
    response_count = 334,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 155);

UPDATE item_statistics SET
    difficulty_index = 0.45,
    discrimination_index = 0.58,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NONE',
    response_count = 367,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 156);

UPDATE item_statistics SET
    difficulty_index = 0.68,
    discrimination_index = 0.44,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NONE',
    response_count = 278,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 157);

UPDATE item_statistics SET
    difficulty_index = 0.52,
    discrimination_index = 0.51,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NONE',
    response_count = 356,
    last_calculated_at = CURRENT_TIMESTAMP - interval '3 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 158);

UPDATE item_statistics SET
    difficulty_index = 0.75,
    discrimination_index = 0.46,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NONE',
    response_count = 301,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 159);

-- ================================================================
-- PHASE 5: EDGE CASES AND TREND ANALYSIS ITEMS
-- ================================================================

-- 5.1 BOUNDARY VALUE ITEMS (exact thresholds for UI testing)

-- Exactly at TOO_EASY boundary (p = 0.90)
UPDATE item_statistics SET
    difficulty_index = 0.90,
    discrimination_index = 0.35,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NONE',
    response_count = 234,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 200);

-- Exactly at TOO_HARD boundary (p = 0.20)
UPDATE item_statistics SET
    difficulty_index = 0.20,
    discrimination_index = 0.38,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NONE',
    response_count = 198,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 201);

-- Exactly at WARNING boundary (rpb = 0.25)
UPDATE item_statistics SET
    difficulty_index = 0.55,
    discrimination_index = 0.25,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NONE',
    response_count = 178,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 202);

-- Exactly at CRITICAL boundary (rpb = 0.10)
UPDATE item_statistics SET
    difficulty_index = 0.58,
    discrimination_index = 0.10,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'WARNING',
    response_count = 145,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 203);

-- Perfect item (optimal center: p = 0.50, rpb = 0.60)
UPDATE item_statistics SET
    difficulty_index = 0.50,
    discrimination_index = 0.60,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NONE',
    response_count = 456,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day',
    status_change_history = '[{"from_status": "PROBATION", "to_status": "ACTIVE", "changed_at": "2025-06-01T14:30:00", "reason": "Perfect psychometric properties - model item"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 204);

-- Exactly at probation threshold (response_count = 50)
UPDATE item_statistics SET
    difficulty_index = 0.57,
    discrimination_index = 0.32,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NONE',
    response_count = 50,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day',
    status_change_history = '[{"from_status": "PROBATION", "to_status": "ACTIVE", "changed_at": "2025-12-30T10:00:00", "reason": "Just reached 50 responses threshold - activated"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 205);

-- Just below probation threshold (response_count = 49)
UPDATE item_statistics SET
    difficulty_index = 0.54,
    discrimination_index = 0.35,
    validity_status = 'PROBATION',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NONE',
    response_count = 49,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day',
    status_change_history = '[{"from_status": null, "to_status": "PROBATION", "changed_at": "2025-12-01T10:00:00", "reason": "Item created - one response from activation"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 206);

-- Zero discrimination (rpb = 0.00 exactly)
UPDATE item_statistics SET
    difficulty_index = 0.50,
    discrimination_index = 0.00,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'NONE',
    discrimination_flag = 'CRITICAL',
    response_count = 234,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days',
    status_change_history = '[{"from_status": "ACTIVE", "to_status": "FLAGGED_FOR_REVIEW", "changed_at": "2025-12-20T10:00:00", "reason": "Discrimination dropped to exactly zero - no differentiation power"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 207);

-- Just below zero (rpb = -0.01)
UPDATE item_statistics SET
    difficulty_index = 0.52,
    discrimination_index = -0.01,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NEGATIVE',
    response_count = 167,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day',
    previous_discrimination_index = 0.08
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 208);

-- Maximum response count
UPDATE item_statistics SET
    difficulty_index = 0.55,
    discrimination_index = 0.45,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NONE',
    response_count = 500,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day',
    status_change_history = '[{"from_status": "PROBATION", "to_status": "ACTIVE", "changed_at": "2025-03-15T14:30:00", "reason": "Initial activation"}, {"from_status": "ACTIVE", "to_status": "ACTIVE", "changed_at": "2025-12-01T10:00:00", "reason": "Reached 500 responses - most tested item in system"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 209);

-- Minimum viable active (response_count = 51)
UPDATE item_statistics SET
    difficulty_index = 0.55,
    discrimination_index = 0.26,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NONE',
    response_count = 51,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 210);

-- Combined edge: TOO_EASY + CRITICAL
UPDATE item_statistics SET
    difficulty_index = 0.91,
    discrimination_index = 0.09,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'TOO_EASY',
    discrimination_flag = 'CRITICAL',
    response_count = 134,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 211);

-- Combined edge: TOO_HARD + CRITICAL
UPDATE item_statistics SET
    difficulty_index = 0.08,
    discrimination_index = 0.05,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'TOO_HARD',
    discrimination_flag = 'CRITICAL',
    response_count = 112,
    last_calculated_at = CURRENT_TIMESTAMP - interval '3 days'
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 212);

-- Combined edge: TOO_EASY + NEGATIVE (worst case)
UPDATE item_statistics SET
    difficulty_index = 0.95,
    discrimination_index = -0.06,
    validity_status = 'RETIRED',
    difficulty_flag = 'TOO_EASY',
    discrimination_flag = 'NEGATIVE',
    response_count = 198,
    last_calculated_at = CURRENT_TIMESTAMP - interval '25 days',
    status_change_history = '[{"from_status": "ACTIVE", "to_status": "RETIRED", "changed_at": "2025-12-05T14:00:00", "reason": "Immediate retirement - too easy AND negative discrimination"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 213);

-- Combined edge: TOO_HARD + NEGATIVE (worst case)
UPDATE item_statistics SET
    difficulty_index = 0.04,
    discrimination_index = -0.09,
    validity_status = 'RETIRED',
    difficulty_flag = 'TOO_HARD',
    discrimination_flag = 'NEGATIVE',
    response_count = 87,
    last_calculated_at = CURRENT_TIMESTAMP - interval '30 days',
    status_change_history = '[{"from_status": "ACTIVE", "to_status": "RETIRED", "changed_at": "2025-11-30T14:00:00", "reason": "Immediate retirement - impossible difficulty AND toxic discrimination"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 214);

-- 5.2 TREND ANALYSIS ITEMS (improving and degrading)

-- Major improvement: was WARNING, now excellent
UPDATE item_statistics SET
    difficulty_index = 0.55,
    discrimination_index = 0.42,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NONE',
    response_count = 289,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day',
    previous_discrimination_index = 0.12,
    status_change_history = '[{"from_status": "PROBATION", "to_status": "ACTIVE", "changed_at": "2025-07-01T14:30:00", "reason": "Initial activation with WARNING discrimination"}, {"from_status": "ACTIVE", "to_status": "ACTIVE", "changed_at": "2025-12-15T10:00:00", "reason": "Discrimination improved from 0.12 to 0.42 - now excellent"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 220);

-- Slight improvement: was WARNING, now good
UPDATE item_statistics SET
    difficulty_index = 0.58,
    discrimination_index = 0.35,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NONE',
    response_count = 245,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days',
    previous_discrimination_index = 0.22
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 221);

-- Recovery from critical: was CRITICAL, now acceptable
UPDATE item_statistics SET
    difficulty_index = 0.52,
    discrimination_index = 0.28,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NONE',
    response_count = 312,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day',
    previous_discrimination_index = 0.06,
    status_change_history = '[{"from_status": "FLAGGED_FOR_REVIEW", "to_status": "ACTIVE", "changed_at": "2025-12-01T14:30:00", "reason": "Discrimination recovered from 0.06 to 0.28 - reactivated"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 222);

-- Near recovery: still WARNING but improving
UPDATE item_statistics SET
    difficulty_index = 0.48,
    discrimination_index = 0.24,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'WARNING',
    response_count = 278,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days',
    previous_discrimination_index = 0.08
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 223);

-- Stable excellent: consistent top performer
UPDATE item_statistics SET
    difficulty_index = 0.55,
    discrimination_index = 0.52,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NONE',
    response_count = 445,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day',
    previous_discrimination_index = 0.50
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 224);

-- Stable good: consistent performer
UPDATE item_statistics SET
    difficulty_index = 0.60,
    discrimination_index = 0.38,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NONE',
    response_count = 389,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days',
    previous_discrimination_index = 0.36
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 225);

-- Minor degradation: slight decline but still active
UPDATE item_statistics SET
    difficulty_index = 0.58,
    discrimination_index = 0.28,
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NONE',
    response_count = 334,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day',
    previous_discrimination_index = 0.35
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 226);

-- Warning slide: degraded from ACTIVE to FLAGGED
UPDATE item_statistics SET
    difficulty_index = 0.55,
    discrimination_index = 0.18,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'NONE',
    discrimination_flag = 'WARNING',
    response_count = 256,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days',
    previous_discrimination_index = 0.32,
    status_change_history = '[{"from_status": "PROBATION", "to_status": "ACTIVE", "changed_at": "2025-06-15T14:30:00", "reason": "Initial activation"}, {"from_status": "ACTIVE", "to_status": "FLAGGED_FOR_REVIEW", "changed_at": "2025-12-20T10:00:00", "reason": "Discrimination dropped from 0.32 to 0.18 - WARNING threshold crossed"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 227);

-- Critical collapse: major decline
UPDATE item_statistics SET
    difficulty_index = 0.52,
    discrimination_index = 0.05,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'NONE',
    discrimination_flag = 'CRITICAL',
    response_count = 289,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day',
    previous_discrimination_index = 0.28,
    status_change_history = '[{"from_status": "ACTIVE", "to_status": "FLAGGED_FOR_REVIEW", "changed_at": "2025-12-28T10:00:00", "reason": "Severe discrimination collapse from 0.28 to 0.05"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 228);

-- Toxic crash: went negative
UPDATE item_statistics SET
    difficulty_index = 0.48,
    discrimination_index = -0.08,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NEGATIVE',
    response_count = 312,
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day',
    previous_discrimination_index = 0.18,
    status_change_history = '[{"from_status": "ACTIVE", "to_status": "FLAGGED_FOR_REVIEW", "changed_at": "2025-12-29T09:00:00", "reason": "Discrimination went negative - was 0.18, now -0.08"}]'::jsonb
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 229);

-- Stable critical: persistently critical
UPDATE item_statistics SET
    difficulty_index = 0.62,
    discrimination_index = 0.07,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'NONE',
    discrimination_flag = 'CRITICAL',
    response_count = 345,
    last_calculated_at = CURRENT_TIMESTAMP - interval '2 days',
    previous_discrimination_index = 0.06
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 230);

-- Stable warning: persistently marginal
UPDATE item_statistics SET
    difficulty_index = 0.45,
    discrimination_index = 0.15,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'NONE',
    discrimination_flag = 'WARNING',
    response_count = 278,
    last_calculated_at = CURRENT_TIMESTAMP - interval '3 days',
    previous_discrimination_index = 0.14
WHERE question_id = (SELECT id FROM assessment_questions ORDER BY id LIMIT 1 OFFSET 231);

-- ================================================================
-- PHASE 6: FILL REMAINING ITEMS WITH HASH-BASED DISTRIBUTION
-- ================================================================
-- All remaining items get metrics based on deterministic hash bucketing
-- This ensures reproducibility and creates realistic variation

-- Fill items that still have NULL difficulty_index with varied ACTIVE metrics
UPDATE item_statistics SET
    difficulty_index = CASE
        WHEN ABS(hashtext(question_id::TEXT || 'diff')) % 10 = 0 THEN 0.42
        WHEN ABS(hashtext(question_id::TEXT || 'diff')) % 10 = 1 THEN 0.48
        WHEN ABS(hashtext(question_id::TEXT || 'diff')) % 10 = 2 THEN 0.52
        WHEN ABS(hashtext(question_id::TEXT || 'diff')) % 10 = 3 THEN 0.56
        WHEN ABS(hashtext(question_id::TEXT || 'diff')) % 10 = 4 THEN 0.60
        WHEN ABS(hashtext(question_id::TEXT || 'diff')) % 10 = 5 THEN 0.64
        WHEN ABS(hashtext(question_id::TEXT || 'diff')) % 10 = 6 THEN 0.68
        WHEN ABS(hashtext(question_id::TEXT || 'diff')) % 10 = 7 THEN 0.72
        WHEN ABS(hashtext(question_id::TEXT || 'diff')) % 10 = 8 THEN 0.76
        ELSE 0.55
    END,
    discrimination_index = CASE
        WHEN ABS(hashtext(question_id::TEXT || 'disc')) % 8 = 0 THEN 0.32
        WHEN ABS(hashtext(question_id::TEXT || 'disc')) % 8 = 1 THEN 0.35
        WHEN ABS(hashtext(question_id::TEXT || 'disc')) % 8 = 2 THEN 0.38
        WHEN ABS(hashtext(question_id::TEXT || 'disc')) % 8 = 3 THEN 0.41
        WHEN ABS(hashtext(question_id::TEXT || 'disc')) % 8 = 4 THEN 0.44
        WHEN ABS(hashtext(question_id::TEXT || 'disc')) % 8 = 5 THEN 0.47
        WHEN ABS(hashtext(question_id::TEXT || 'disc')) % 8 = 6 THEN 0.50
        ELSE 0.42
    END,
    response_count = 120 + (ABS(hashtext(question_id::TEXT || 'resp')) % 200),
    last_calculated_at = CURRENT_TIMESTAMP - interval '1 day' - (ABS(hashtext(question_id::TEXT || 'time')) % 5 || ' days')::interval
WHERE difficulty_index IS NULL;

-- ================================================================
-- PHASE 7: VERIFICATION QUERIES (for testing)
-- ================================================================
-- Run these queries after migration to verify distributions

-- Verification Query 1: Item Status Distribution
-- Expected: ~65% ACTIVE, ~12% PROBATION, ~15% FLAGGED, ~8% RETIRED
-- SELECT validity_status, COUNT(*),
--        ROUND(COUNT(*)::NUMERIC / (SELECT COUNT(*) FROM item_statistics) * 100, 1) as pct
-- FROM item_statistics
-- GROUP BY validity_status
-- ORDER BY COUNT(*) DESC;

-- Verification Query 2: Discrimination Flag Distribution
-- Expected: ~8 NEGATIVE, ~14 CRITICAL, ~20 WARNING
-- SELECT discrimination_flag, COUNT(*)
-- FROM item_statistics
-- WHERE discrimination_flag != 'NONE'
-- GROUP BY discrimination_flag
-- ORDER BY COUNT(*) DESC;

-- Verification Query 3: Difficulty Flag Distribution
-- Expected: ~14 TOO_EASY, ~14 TOO_HARD
-- SELECT difficulty_flag, COUNT(*)
-- FROM item_statistics
-- WHERE difficulty_flag != 'NONE'
-- GROUP BY difficulty_flag;

-- Verification Query 4: Competency Reliability Distribution
-- Expected: 20 RELIABLE, 4 ACCEPTABLE, 2 UNRELIABLE, 1 INSUFFICIENT_DATA
-- SELECT reliability_status, COUNT(*), ROUND(AVG(cronbach_alpha), 3) as avg_alpha
-- FROM competency_reliability
-- GROUP BY reliability_status
-- ORDER BY COUNT(*) DESC;

-- Verification Query 5: Big Five Traits (ALL must be RELIABLE)
-- SELECT trait, cronbach_alpha, reliability_status
-- FROM big_five_reliability
-- ORDER BY trait;

-- Verification Query 6: Difficulty-Discrimination Correlation
-- Expected: Negative correlation around -0.2 to -0.4
-- SELECT ROUND(CORR(difficulty_index, discrimination_index)::NUMERIC, 3) as correlation
-- FROM item_statistics
-- WHERE difficulty_index IS NOT NULL AND discrimination_index IS NOT NULL;

-- Verification Query 7: Items with Trend Data (previous_discrimination_index set)
-- Expected: ~22 items with trend data
-- SELECT COUNT(*) as trend_items
-- FROM item_statistics
-- WHERE previous_discrimination_index IS NOT NULL;

-- Verification Query 8: Edge Case Items (exact boundaries)
-- Expected: Items at p=0.90, p=0.20, rpb=0.25, rpb=0.10, rpb=0.00
-- SELECT difficulty_index, discrimination_index, validity_status
-- FROM item_statistics
-- WHERE difficulty_index IN (0.90, 0.20, 0.50)
--    OR discrimination_index IN (0.25, 0.10, 0.00, -0.01)
-- ORDER BY difficulty_index, discrimination_index;

-- ================================================================
-- END OF MIGRATION
-- ================================================================
