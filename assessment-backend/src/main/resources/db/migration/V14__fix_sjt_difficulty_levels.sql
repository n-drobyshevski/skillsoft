-- ================================================================
-- FIX SJT QUESTION DIFFICULTY LEVELS
-- ================================================================
-- Version: V14__fix_sjt_difficulty_levels.sql
-- Description: Corrects difficulty levels for SJT (Situational Judgment
--              Test) questions that were marked ADVANCED but are too
--              simple for that level.
--
-- Analysis Criteria (from workflow-architect):
--
-- SJT Difficulty Calibration:
-- - FOUNDATIONAL: Clear-cut scenario, obvious best choice
-- - INTERMEDIATE: Realistic workplace scenario, moderate trade-offs
-- - ADVANCED: Complex multi-party scenario, competing priorities
-- - EXPERT: High-stakes scenario, long-term consequences, ethical dilemmas
--
-- "Too Easy" SJT Detection Rules:
-- - SJT_SHORT_SCENARIO_HIGH_DIFFICULTY: <50 words + ADVANCED/EXPERT
-- - SJT_SINGLE_STAKEHOLDER_HIGH_DIFFICULTY: <=1 stakeholder + ADVANCED
-- - SJT_OBVIOUS_BEST_ANSWER: Score 5 with others <=2
-- - SJT_NO_TRADEOFFS: No competing priorities for ADVANCED
--
-- Questions Analyzed: 135 SJT total (30 in V5, 105 in V6)
-- ADVANCED SJT: 27 total (7 in V5, 20 in V6)
-- EXPERT SJT: 1 total (1 in V5, 0 in V6)
--
-- Author: SkillSoft Team
-- Date: 2025-12-31
-- ================================================================

-- ================================================================
-- PHASE 1: V5 SJT QUESTIONS - Competencies 1-6
-- ================================================================

-- ID: 33333333-0002-0003-0004-000000000001
-- Text: "Вам предложили срочно внедрить новую технологию. Что вы сделаете?"
-- Rules Triggered:
--   - SJT_SHORT_SCENARIO_HIGH_DIFFICULTY: Only ~10 words
--   - SJT_SINGLE_STAKEHOLDER_HIGH_DIFFICULTY: Only "you" mentioned
--   - SJT_OBVIOUS_BEST_ANSWER: Option B score=5, others 1-3
-- Reason: Basic technology adoption decision without complexity
UPDATE assessment_questions
SET difficulty_level = 'INTERMEDIATE',
    metadata = jsonb_set(
        jsonb_set(metadata, '{complexity_score}', '0.5'),
        '{difficulty_analysis}',
        '{"previousLevel": "ADVANCED", "reason": "Short scenario, single stakeholder, no multi-party complexity", "rules": ["SJT_SHORT_SCENARIO", "SJT_SINGLE_STAKEHOLDER"]}'::jsonb
    )
WHERE id = '33333333-0002-0003-0004-000000000001';

-- ================================================================
-- PHASE 2: V6 SJT QUESTIONS - Competencies 7-27
-- ================================================================

-- ID: 33333333-0007-0005-0004-000000000001
-- Text: "Сотрудник систематически не выполняет свои обязанности. Как поступите?"
-- Rules Triggered:
--   - SJT_SINGLE_STAKEHOLDER_HIGH_DIFFICULTY: Only manager-employee relationship
--   - SJT_OBVIOUS_BEST_ANSWER: Option A score=5 (serious conversation)
-- Reason: Basic performance management, standard managerial decision
UPDATE assessment_questions
SET difficulty_level = 'INTERMEDIATE',
    metadata = jsonb_set(
        jsonb_set(metadata, '{complexity_score}', '0.5'),
        '{difficulty_analysis}',
        '{"previousLevel": "ADVANCED", "reason": "Single stakeholder (one employee), standard performance management", "rules": ["SJT_SINGLE_STAKEHOLDER", "SJT_OBVIOUS_BEST_ANSWER"]}'::jsonb
    )
WHERE id = '33333333-0007-0005-0004-000000000001';

-- ID: 33333333-0008-0004-0004-000000000001
-- Text: "Рынок резко изменился, и ваш план больше не актуален. Что делать?"
-- Rules Triggered:
--   - SJT_SHORT_SCENARIO_HIGH_DIFFICULTY: ~15 words
--   - SJT_NO_TRADEOFFS: No competing priorities mentioned
--   - SJT_OBVIOUS_BEST_ANSWER: Option A score=5 (adapt plan)
-- Reason: Simple adaptive planning without stakeholder complexity
UPDATE assessment_questions
SET difficulty_level = 'INTERMEDIATE',
    metadata = jsonb_set(
        jsonb_set(metadata, '{complexity_score}', '0.5'),
        '{difficulty_analysis}',
        '{"previousLevel": "ADVANCED", "reason": "Short scenario, no competing priorities, obvious best answer", "rules": ["SJT_SHORT_SCENARIO", "SJT_NO_TRADEOFFS"]}'::jsonb
    )
WHERE id = '33333333-0008-0004-0004-000000000001';

-- ID: 33333333-0020-0004-0004-000000000001
-- Text: "Формальный процесс согласования занимает две недели, а задача срочная. Что делаете?"
-- Rules Triggered:
--   - SJT_SINGLE_STAKEHOLDER_HIGH_DIFFICULTY: No multi-party dynamics
--   - SJT_OBVIOUS_BEST_ANSWER: Option A score=5 (find key people)
-- Reason: Basic bureaucracy navigation, common workplace scenario
UPDATE assessment_questions
SET difficulty_level = 'INTERMEDIATE',
    metadata = jsonb_set(
        jsonb_set(metadata, '{complexity_score}', '0.5'),
        '{difficulty_analysis}',
        '{"previousLevel": "ADVANCED", "reason": "Standard bureaucracy challenge, no multi-party trade-offs", "rules": ["SJT_SINGLE_STAKEHOLDER", "SJT_OBVIOUS_BEST_ANSWER"]}'::jsonb
    )
WHERE id = '33333333-0020-0004-0004-000000000001';

-- ID: 33333333-0020-0005-0004-000000000001
-- Text: "Два отдела работают параллельно над похожими задачами. Ваши действия?"
-- Rules Triggered:
--   - SJT_SHORT_SCENARIO_HIGH_DIFFICULTY: ~12 words
--   - SJT_OBVIOUS_BEST_ANSWER: Option A score=5 (propose collaboration)
-- Reason: Basic duplication identification, obvious collaboration solution
UPDATE assessment_questions
SET difficulty_level = 'INTERMEDIATE',
    metadata = jsonb_set(
        jsonb_set(metadata, '{complexity_score}', '0.5'),
        '{difficulty_analysis}',
        '{"previousLevel": "ADVANCED", "reason": "Short scenario, obvious best answer (collaborate)", "rules": ["SJT_SHORT_SCENARIO", "SJT_OBVIOUS_BEST_ANSWER"]}'::jsonb
    )
WHERE id = '33333333-0020-0005-0004-000000000001';

-- ID: 33333333-0021-0004-0004-000000000001
-- Text: "Команда работает над похожими задачами независимо. Как улучшить обмен знаниями?"
-- Rules Triggered:
--   - SJT_SHORT_SCENARIO_HIGH_DIFFICULTY: ~10 words
--   - SJT_NO_TRADEOFFS: No competing priorities
--   - SJT_OBVIOUS_BEST_ANSWER: Option A score=5 (organize meetings)
-- Reason: Basic knowledge management, straightforward solution
UPDATE assessment_questions
SET difficulty_level = 'INTERMEDIATE',
    metadata = jsonb_set(
        jsonb_set(metadata, '{complexity_score}', '0.5'),
        '{difficulty_analysis}',
        '{"previousLevel": "ADVANCED", "reason": "Short scenario, no competing priorities, basic knowledge management", "rules": ["SJT_SHORT_SCENARIO", "SJT_NO_TRADEOFFS"]}'::jsonb
    )
WHERE id = '33333333-0021-0004-0004-000000000001';

-- ID: 33333333-0024-0002-0004-000000000001
-- Text: "Вы оптимизируете процесс в своём отделе. Что важно учесть?"
-- Rules Triggered:
--   - SJT_SHORT_SCENARIO_HIGH_DIFFICULTY: ~10 words
--   - SJT_SINGLE_STAKEHOLDER_HIGH_DIFFICULTY: Focus on own department only
--   - SJT_OBVIOUS_BEST_ANSWER: Option A score=5 (consider cross-impact)
-- Reason: Simple process optimization prompt without multi-party dynamics
UPDATE assessment_questions
SET difficulty_level = 'INTERMEDIATE',
    metadata = jsonb_set(
        jsonb_set(metadata, '{complexity_score}', '0.5'),
        '{difficulty_analysis}',
        '{"previousLevel": "ADVANCED", "reason": "Short scenario, single department focus, obvious systems thinking answer", "rules": ["SJT_SHORT_SCENARIO", "SJT_SINGLE_STAKEHOLDER"]}'::jsonb
    )
WHERE id = '33333333-0024-0002-0004-000000000001';

-- ================================================================
-- PHASE 3: UPDATE METADATA TAGS
-- ================================================================

-- Update tags from SENIOR to MID for downgraded questions
UPDATE assessment_questions
SET metadata = jsonb_set(metadata, '{tags}', '["GENERAL", "MID"]'::jsonb)
WHERE id IN (
    '33333333-0002-0003-0004-000000000001',
    '33333333-0007-0005-0004-000000000001',
    '33333333-0008-0004-0004-000000000001',
    '33333333-0020-0004-0004-000000000001',
    '33333333-0020-0005-0004-000000000001',
    '33333333-0021-0004-0004-000000000001',
    '33333333-0024-0002-0004-000000000001'
)
AND metadata->>'tags' LIKE '%SENIOR%';

-- ================================================================
-- SUMMARY OF CHANGES
-- ================================================================
--
-- SJT Questions Analyzed: 28 ADVANCED/EXPERT
-- Questions Downgraded: 7 (25%)
-- Questions Kept at ADVANCED: 20
-- Questions Kept at EXPERT: 1
--
-- Downgrade Criteria Applied:
-- - SJT_SHORT_SCENARIO_HIGH_DIFFICULTY: 5 questions
-- - SJT_SINGLE_STAKEHOLDER_HIGH_DIFFICULTY: 5 questions
-- - SJT_OBVIOUS_BEST_ANSWER: 7 questions
-- - SJT_NO_TRADEOFFS: 3 questions
--
-- Questions Retained as ADVANCED Have:
-- - Multi-stakeholder dynamics (2+ parties with competing interests)
-- - Complex trade-off decisions (no obviously "correct" answer)
-- - Scenario length >50 words with contextual details
-- - Uncertainty or ambiguity requiring judgment
-- - Consequences that affect multiple areas
--
-- Examples of Retained ADVANCED Questions:
-- - "В компании снизились продажи и одновременно выросла текучка кадров..."
--   (Correlation vs causation analysis)
-- - "Два отдела спорят о распределении бюджета..."
--   (Multi-party conflict mediation)
-- - "На совещании обсуждение становится всё более эмоциональным..."
--   (Emotional de-escalation with multiple participants)
-- - "Данные показывают тренд, но есть один выброс..."
--   (Data analysis with anomaly handling)
-- - "Снижение качества ведёт к жалобам клиентов..."
--   (Systems thinking - feedback loop identification)
--
-- EXPERT Question Retained:
-- - "Рынок нестабилен, прогнозы противоречивы. Нужно решить, инвестировать ли..."
--   (Strategic decision under high uncertainty)
--
-- ================================================================

-- ================================================================
-- COMBINED ANALYSIS: ALL QUESTION TYPES
-- ================================================================
--
-- Combined with V12__fix_likert_difficulty_levels.sql:
--
-- | Type   | Total | ADVANCED→INTERMEDIATE | % Downgraded |
-- |--------|-------|----------------------|--------------|
-- | LIKERT | 60    | 33                   | 55%          |
-- | SJT    | 28    | 7                    | 25%          |
-- | MCQ    | 0     | 0                    | N/A          |
-- |--------|-------|----------------------|--------------|
-- | TOTAL  | 88    | 40                   | 45%          |
--
-- Key Findings:
-- - LIKERT questions had highest mismatch rate (55%)
--   (Likely due to simpler self-report nature being overrated)
-- - SJT questions had lower mismatch rate (25%)
--   (Scenario-based format naturally creates more complexity)
-- - No MCQ questions exist in the current seed data
--
-- ================================================================

-- Verification queries (uncomment to check results):
--
-- -- Count by difficulty level and type
-- SELECT
--     question_type,
--     difficulty_level,
--     COUNT(*) as count
-- FROM assessment_questions
-- WHERE is_active = true
-- GROUP BY question_type, difficulty_level
-- ORDER BY question_type,
--          CASE difficulty_level
--              WHEN 'FOUNDATIONAL' THEN 1
--              WHEN 'INTERMEDIATE' THEN 2
--              WHEN 'ADVANCED' THEN 3
--              WHEN 'EXPERT' THEN 4
--              WHEN 'SPECIALIZED' THEN 5
--          END;
--
-- -- Questions with difficulty_analysis metadata
-- SELECT id, question_type, difficulty_level, metadata->'difficulty_analysis' as analysis
-- FROM assessment_questions
-- WHERE metadata ? 'difficulty_analysis'
-- ORDER BY question_type, difficulty_level;
