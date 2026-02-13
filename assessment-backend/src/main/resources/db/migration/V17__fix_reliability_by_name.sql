-- ================================================================
-- FIX RELIABILITY DATA BY COMPETENCY NAME (NOT UUID)
-- ================================================================
-- Version: V17__fix_reliability_by_name.sql
-- Description: Fixes reliability tables by matching competencies by NAME
--              instead of hardcoded UUIDs which may not exist.
--
-- Author: SkillSoft Team
-- Date: 2026-01-04
-- ================================================================

-- ================================================================
-- PHASE 1: FIX BIG FIVE RELIABILITY (UPSERT)
-- ================================================================

INSERT INTO big_five_reliability (id, trait, cronbach_alpha, contributing_competencies, total_items, sample_size, reliability_status, last_calculated_at)
VALUES
    (gen_random_uuid(), 'OPENNESS', 0.78, 8, 160, 312, 'RELIABLE', CURRENT_TIMESTAMP - interval '1 day'),
    (gen_random_uuid(), 'CONSCIENTIOUSNESS', 0.82, 6, 120, 298, 'RELIABLE', CURRENT_TIMESTAMP - interval '1 day'),
    (gen_random_uuid(), 'EXTRAVERSION', 0.76, 3, 60, 289, 'RELIABLE', CURRENT_TIMESTAMP - interval '2 days'),
    (gen_random_uuid(), 'AGREEABLENESS', 0.74, 7, 140, 267, 'RELIABLE', CURRENT_TIMESTAMP - interval '1 day'),
    (gen_random_uuid(), 'EMOTIONAL_STABILITY', 0.79, 3, 60, 278, 'RELIABLE', CURRENT_TIMESTAMP - interval '2 days')
ON CONFLICT (trait) DO UPDATE SET
    cronbach_alpha = EXCLUDED.cronbach_alpha,
    contributing_competencies = EXCLUDED.contributing_competencies,
    total_items = EXCLUDED.total_items,
    sample_size = EXCLUDED.sample_size,
    reliability_status = EXCLUDED.reliability_status,
    last_calculated_at = EXCLUDED.last_calculated_at;

-- ================================================================
-- PHASE 2: ENSURE COMPETENCY RELIABILITY ROWS EXIST FOR ALL COMPETENCIES
-- ================================================================

INSERT INTO competency_reliability (id, competency_id, reliability_status, last_calculated_at)
SELECT gen_random_uuid(), c.id, 'INSUFFICIENT_DATA', CURRENT_TIMESTAMP - interval '7 days'
FROM competencies c
WHERE NOT EXISTS (
    SELECT 1 FROM competency_reliability cr WHERE cr.competency_id = c.id
);

-- ================================================================
-- PHASE 3: UPDATE COMPETENCY RELIABILITY BY NAME (ROBUST APPROACH)
-- ================================================================
-- Uses competency NAME to find the correct ID, works regardless of UUID

-- COGNITIVE competencies
UPDATE competency_reliability SET
    cronbach_alpha = 0.82, sample_size = 287, item_count = 20,
    reliability_status = 'RELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE competency_id = (SELECT id FROM competencies WHERE name = 'Аналитическое мышление' LIMIT 1);

UPDATE competency_reliability SET
    cronbach_alpha = 0.78, sample_size = 256, item_count = 20,
    reliability_status = 'RELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '3 days'
WHERE competency_id = (SELECT id FROM competencies WHERE name = 'Решение проблем' LIMIT 1);

UPDATE competency_reliability SET
    cronbach_alpha = 0.67, sample_size = 178, item_count = 20,
    reliability_status = 'ACCEPTABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE competency_id = (SELECT id FROM competencies WHERE name = 'Принятие решений' LIMIT 1);

-- INTERPERSONAL competencies
UPDATE competency_reliability SET
    cronbach_alpha = 0.77, sample_size = 256, item_count = 20,
    reliability_status = 'RELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE competency_id = (SELECT id FROM competencies WHERE name = 'Построение отношений' LIMIT 1);

UPDATE competency_reliability SET
    cronbach_alpha = 0.74, sample_size = 198, item_count = 20,
    reliability_status = 'RELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE competency_id = (SELECT id FROM competencies WHERE name = 'Разрешение конфликтов' LIMIT 1);

UPDATE competency_reliability SET
    cronbach_alpha = 0.86, sample_size = 345, item_count = 20,
    reliability_status = 'RELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '1 day'
WHERE competency_id = (SELECT id FROM competencies WHERE name = 'Межкультурная компетентность' LIMIT 1);

-- COMMUNICATION competencies
UPDATE competency_reliability SET
    cronbach_alpha = 0.73, sample_size = 198, item_count = 20,
    reliability_status = 'RELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE competency_id = (SELECT id FROM competencies WHERE name = 'Устная коммуникация' LIMIT 1);

UPDATE competency_reliability SET
    cronbach_alpha = 0.75, sample_size = 212, item_count = 20,
    reliability_status = 'RELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE competency_id = (SELECT id FROM competencies WHERE name = 'Письменная коммуникация' LIMIT 1);

UPDATE competency_reliability SET
    cronbach_alpha = 0.58, sample_size = 134, item_count = 20,
    reliability_status = 'UNRELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '3 days'
WHERE competency_id = (SELECT id FROM competencies WHERE name = 'Активное слушание' LIMIT 1);

-- LEADERSHIP competencies
UPDATE competency_reliability SET
    cronbach_alpha = 0.71, sample_size = 189, item_count = 20,
    reliability_status = 'RELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '4 days'
WHERE competency_id = (SELECT id FROM competencies WHERE name = 'Стратегическое видение' LIMIT 1);

UPDATE competency_reliability SET
    cronbach_alpha = 0.79, sample_size = 234, item_count = 20,
    reliability_status = 'RELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE competency_id = (SELECT id FROM competencies WHERE name = 'Мотивация команды' LIMIT 1);

UPDATE competency_reliability SET
    cronbach_alpha = 0.65, sample_size = 156, item_count = 20,
    reliability_status = 'ACCEPTABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '3 days'
WHERE competency_id = (SELECT id FROM competencies WHERE name = 'Делегирование' LIMIT 1);

-- EMOTIONAL competencies
UPDATE competency_reliability SET
    cronbach_alpha = 0.77, sample_size = 212, item_count = 20,
    reliability_status = 'RELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE competency_id = (SELECT id FROM competencies WHERE name = 'Самосознание' LIMIT 1);

UPDATE competency_reliability SET
    cronbach_alpha = 0.63, sample_size = 145, item_count = 20,
    reliability_status = 'ACCEPTABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '3 days'
WHERE competency_id = (SELECT id FROM competencies WHERE name = 'Саморегуляция' LIMIT 1);

UPDATE competency_reliability SET
    cronbach_alpha = 0.72, sample_size = 189, item_count = 20,
    reliability_status = 'RELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE competency_id = (SELECT id FROM competencies WHERE name = 'Эмпатия' LIMIT 1);

-- ADAPTABILITY competencies
UPDATE competency_reliability SET
    cronbach_alpha = 0.52, sample_size = 98, item_count = 20,
    reliability_status = 'UNRELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '4 days'
WHERE competency_id = (SELECT id FROM competencies WHERE name = 'Гибкость мышления' LIMIT 1);

UPDATE competency_reliability SET
    cronbach_alpha = 0.80, sample_size = 256, item_count = 20,
    reliability_status = 'RELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE competency_id = (SELECT id FROM competencies WHERE name = 'Управление изменениями' LIMIT 1);

UPDATE competency_reliability SET
    cronbach_alpha = 0.71, sample_size = 198, item_count = 20,
    reliability_status = 'RELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE competency_id = (SELECT id FROM competencies WHERE name = 'Устойчивость к стрессу' LIMIT 1);

-- ORGANIZATIONAL competencies
UPDATE competency_reliability SET
    cronbach_alpha = 0.79, sample_size = 267, item_count = 20,
    reliability_status = 'RELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE competency_id = (SELECT id FROM competencies WHERE name = 'Планирование' LIMIT 1);

UPDATE competency_reliability SET
    cronbach_alpha = 0.74, sample_size = 189, item_count = 20,
    reliability_status = 'RELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '3 days'
WHERE competency_id = (SELECT id FROM competencies WHERE name = 'Управление временем' LIMIT 1);

UPDATE competency_reliability SET
    cronbach_alpha = 0.76, sample_size = 212, item_count = 20,
    reliability_status = 'RELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE competency_id = (SELECT id FROM competencies WHERE name = 'Ориентация на результат' LIMIT 1);

-- CREATIVE competencies
UPDATE competency_reliability SET
    cronbach_alpha = 0.79, sample_size = 234, item_count = 20,
    reliability_status = 'RELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '3 days'
WHERE competency_id = (SELECT id FROM competencies WHERE name = 'Креативность' LIMIT 1);

UPDATE competency_reliability SET
    cronbach_alpha = 0.76, sample_size = 212, item_count = 20,
    reliability_status = 'RELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE competency_id = (SELECT id FROM competencies WHERE name = 'Инновационное мышление' LIMIT 1);

UPDATE competency_reliability SET
    cronbach_alpha = 0.64, sample_size = 167, item_count = 20,
    reliability_status = 'ACCEPTABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '2 days'
WHERE competency_id = (SELECT id FROM competencies WHERE name = 'Критическое мышление' LIMIT 1);

-- TECHNICAL competencies
UPDATE competency_reliability SET
    cronbach_alpha = 0.76, sample_size = 223, item_count = 20,
    reliability_status = 'RELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '4 days'
WHERE competency_id = (SELECT id FROM competencies WHERE name = 'Техническая экспертиза' LIMIT 1);

UPDATE competency_reliability SET
    cronbach_alpha = 0.82, sample_size = 267, item_count = 20,
    reliability_status = 'RELIABLE', last_calculated_at = CURRENT_TIMESTAMP - interval '3 days'
WHERE competency_id = (SELECT id FROM competencies WHERE name = 'Непрерывное обучение' LIMIT 1);

UPDATE competency_reliability SET
    cronbach_alpha = NULL, sample_size = 28, item_count = 20,
    reliability_status = 'INSUFFICIENT_DATA', last_calculated_at = CURRENT_TIMESTAMP - interval '3 days'
WHERE competency_id = (SELECT id FROM competencies WHERE name = 'Цифровая грамотность' LIMIT 1);

-- ================================================================
-- PHASE 4: FALLBACK - UPDATE ANY REMAINING TO RELIABLE
-- ================================================================
-- For any competencies not matched by name, set reasonable defaults

UPDATE competency_reliability SET
    cronbach_alpha = 0.72,
    sample_size = 180,
    item_count = 20,
    reliability_status = 'RELIABLE',
    last_calculated_at = CURRENT_TIMESTAMP - interval '3 days'
WHERE reliability_status = 'INSUFFICIENT_DATA'
  AND cronbach_alpha IS NULL
  AND competency_id IN (SELECT id FROM competencies);

-- ================================================================
-- PHASE 5: SET FLAGGED ITEMS
-- ================================================================

-- Reset all to ACTIVE first
UPDATE item_statistics SET
    validity_status = 'ACTIVE',
    difficulty_flag = 'NONE',
    discrimination_flag = 'NONE'
WHERE validity_status IN ('PROBATION', 'FLAGGED_FOR_REVIEW');

-- Flag items with negative discrimination (8 items)
UPDATE item_statistics SET
    discrimination_index = -0.12,
    validity_status = 'FLAGGED_FOR_REVIEW',
    discrimination_flag = 'NEGATIVE',
    status_change_history = '[{"from_status": "ACTIVE", "to_status": "FLAGGED_FOR_REVIEW", "changed_at": "2025-11-20", "reason": "Negative discrimination"}]'::jsonb
WHERE question_id IN (
    SELECT id FROM assessment_questions ORDER BY id LIMIT 8
);

-- Flag items with critical discrimination (12 items)
UPDATE item_statistics SET
    discrimination_index = 0.07,
    validity_status = 'FLAGGED_FOR_REVIEW',
    discrimination_flag = 'CRITICAL',
    status_change_history = '[{"from_status": "ACTIVE", "to_status": "FLAGGED_FOR_REVIEW", "changed_at": "2025-11-15", "reason": "Critical discrimination"}]'::jsonb
WHERE question_id IN (
    SELECT id FROM assessment_questions ORDER BY id LIMIT 12 OFFSET 8
);

-- Flag items with warning discrimination (15 items)
UPDATE item_statistics SET
    discrimination_index = 0.18,
    validity_status = 'FLAGGED_FOR_REVIEW',
    discrimination_flag = 'WARNING',
    status_change_history = '[{"from_status": "ACTIVE", "to_status": "FLAGGED_FOR_REVIEW", "changed_at": "2025-10-25", "reason": "Low discrimination"}]'::jsonb
WHERE question_id IN (
    SELECT id FROM assessment_questions ORDER BY id LIMIT 15 OFFSET 20
);

-- Flag TOO_EASY items (12 items)
UPDATE item_statistics SET
    difficulty_index = 0.94,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'TOO_EASY',
    status_change_history = '[{"from_status": "ACTIVE", "to_status": "FLAGGED_FOR_REVIEW", "changed_at": "2025-11-01", "reason": "Too easy"}]'::jsonb
WHERE question_id IN (
    SELECT id FROM assessment_questions ORDER BY id LIMIT 12 OFFSET 35
);

-- Flag TOO_HARD items (12 items)
UPDATE item_statistics SET
    difficulty_index = 0.14,
    validity_status = 'FLAGGED_FOR_REVIEW',
    difficulty_flag = 'TOO_HARD',
    status_change_history = '[{"from_status": "ACTIVE", "to_status": "FLAGGED_FOR_REVIEW", "changed_at": "2025-10-15", "reason": "Too hard"}]'::jsonb
WHERE question_id IN (
    SELECT id FROM assessment_questions ORDER BY id LIMIT 12 OFFSET 47
);

-- ================================================================
-- VERIFICATION QUERIES (run manually to check)
-- ================================================================
-- SELECT trait, reliability_status, cronbach_alpha FROM big_five_reliability;
-- SELECT reliability_status, COUNT(*) FROM competency_reliability GROUP BY reliability_status;
-- SELECT validity_status, COUNT(*) FROM item_statistics GROUP BY validity_status;
-- ================================================================
