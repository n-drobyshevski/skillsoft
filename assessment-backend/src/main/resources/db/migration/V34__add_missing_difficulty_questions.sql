-- ================================================================
-- ADD MISSING DIFFICULTY-LEVEL QUESTIONS
-- ================================================================
-- Version: V34__add_missing_difficulty_questions.sql
-- Description: Adds FOUNDATIONAL and ADVANCED questions to indicators
--              that currently lack them, resolving assembly borrowing
--              warnings for graduated difficulty distribution.
--
-- Root Cause: V12 correctly downgraded inflated ADVANCED LIKERT items
--             to INTERMEDIATE, leaving many indicators without coverage
--             across all 3 difficulty bands (FOUNDATIONAL / INTERMEDIATE
--             / ADVANCED). The selection service falls back to sibling
--             indicator borrowing when a band is empty.
--
-- Psychometric Design Principles:
--   FOUNDATIONAL: Simple, direct self-report. Baseline validation items.
--                 Obvious "correct" answer is acceptable at this tier.
--   ADVANCED:     Genuine trade-off items where both "agree" and "disagree"
--                 have reasonable merit. Reduces social desirability bias.
--                 Techniques used:
--                 - Speed vs. thoroughness dilemmas
--                 - Intellectual honesty vs. persuasiveness tension
--                 - Self-awareness of analytical limitations
--                 - Counter-intuitive metacognitive behaviors
--
-- Affected Competencies:
--   C1  Analytical Thinking  (6 questions)
--   C2  Problem Solving      (6 questions)
--   C22 Logical Reasoning    (6 questions)
--
-- Total: 18 new questions
-- ================================================================

-- ================================================================
-- COMPETENCY 1: Analytical Thinking (Аналитическое мышление)
-- ================================================================

-- ----------------------------------------------------------------
-- Indicator 1.1: Разбивает сложные проблемы на составные части
-- Gap: No ADVANCED questions
-- ----------------------------------------------------------------
INSERT INTO assessment_questions
    (id, behavioral_indicator_id, question_text, question_type,
     answer_options, scoring_rubric, time_limit, difficulty_level,
     metadata, is_active, order_index)
VALUES
('33333333-0001-0001-0005-000000000001',
 '22222222-0001-0001-0001-000000000001',
 'При анализе комплексных проектов я обнаруживаю, что первоначальная декомпозиция задач часто оказывается неточной и требует существенного пересмотра после погружения в детали',
 'LIKERT',
 '[{"label": "Совершенно не согласен", "score": 1}, {"label": "Не согласен", "score": 2}, {"label": "Нейтрально", "score": 3}, {"label": "Согласен", "score": 4}, {"label": "Совершенно согласен", "score": 5}]'::jsonb,
 'DIRECT_SCALE', 30, 'ADVANCED',
 '{"tags": ["GENERAL", "SENIOR"], "complexity_score": 0.7, "scenario_type": "WORKPLACE"}'::jsonb,
 true, 5)
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------
-- Indicator 1.2: Выявляет причинно-следственные связи
-- Gap: No ADVANCED questions
-- ----------------------------------------------------------------
INSERT INTO assessment_questions
    (id, behavioral_indicator_id, question_text, question_type,
     answer_options, scoring_rubric, time_limit, difficulty_level,
     metadata, is_active, order_index)
VALUES
('33333333-0001-0002-0005-000000000001',
 '22222222-0001-0001-0001-000000000002',
 'Мне случается настолько углубляться в анализ причин проблемы, что я откладываю начало работы над практическим решением',
 'LIKERT',
 '[{"label": "Совершенно не согласен", "score": 1}, {"label": "Не согласен", "score": 2}, {"label": "Нейтрально", "score": 3}, {"label": "Согласен", "score": 4}, {"label": "Совершенно согласен", "score": 5}]'::jsonb,
 'DIRECT_SCALE', 30, 'ADVANCED',
 '{"tags": ["GENERAL", "SENIOR"], "complexity_score": 0.7, "scenario_type": "WORKPLACE"}'::jsonb,
 true, 5)
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------
-- Indicator 1.3: Использует данные для обоснования выводов
-- Gap: No ADVANCED questions
-- ----------------------------------------------------------------
INSERT INTO assessment_questions
    (id, behavioral_indicator_id, question_text, question_type,
     answer_options, scoring_rubric, time_limit, difficulty_level,
     metadata, is_active, order_index)
VALUES
('33333333-0001-0003-0005-000000000001',
 '22222222-0001-0001-0001-000000000003',
 'При недостатке данных для обоснованного вывода я предпочитаю приостановить анализ, даже если это создаёт задержку для заинтересованных сторон',
 'LIKERT',
 '[{"label": "Совершенно не согласен", "score": 1}, {"label": "Не согласен", "score": 2}, {"label": "Нейтрально", "score": 3}, {"label": "Согласен", "score": 4}, {"label": "Совершенно согласен", "score": 5}]'::jsonb,
 'DIRECT_SCALE', 30, 'ADVANCED',
 '{"tags": ["GENERAL", "SENIOR"], "complexity_score": 0.7, "scenario_type": "WORKPLACE"}'::jsonb,
 true, 5)
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------
-- Indicator 1.4: Оценивает альтернативные решения по критериям
-- Gap: No FOUNDATIONAL questions
-- ----------------------------------------------------------------
INSERT INTO assessment_questions
    (id, behavioral_indicator_id, question_text, question_type,
     answer_options, scoring_rubric, time_limit, difficulty_level,
     metadata, is_active, order_index)
VALUES
('33333333-0001-0004-0005-000000000001',
 '22222222-0001-0001-0001-000000000004',
 'Когда мне нужно сделать выбор, я обычно рассматриваю несколько вариантов, прежде чем остановиться на одном',
 'LIKERT',
 '[{"label": "Совершенно не согласен", "score": 1}, {"label": "Не согласен", "score": 2}, {"label": "Нейтрально", "score": 3}, {"label": "Согласен", "score": 4}, {"label": "Совершенно согласен", "score": 5}]'::jsonb,
 'DIRECT_SCALE', 30, 'FOUNDATIONAL',
 '{"tags": ["GENERAL", "JUNIOR"], "complexity_score": 0.3, "scenario_type": "WORKPLACE"}'::jsonb,
 true, 5)
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------
-- Indicator 1.5: Прогнозирует последствия принимаемых решений
-- Gap: No FOUNDATIONAL or ADVANCED questions (all INTERMEDIATE)
-- ----------------------------------------------------------------
INSERT INTO assessment_questions
    (id, behavioral_indicator_id, question_text, question_type,
     answer_options, scoring_rubric, time_limit, difficulty_level,
     metadata, is_active, order_index)
VALUES
('33333333-0001-0005-0005-000000000001',
 '22222222-0001-0001-0001-000000000005',
 'Перед важным решением я задумываюсь о том, к каким последствиям оно может привести',
 'LIKERT',
 '[{"label": "Совершенно не согласен", "score": 1}, {"label": "Не согласен", "score": 2}, {"label": "Нейтрально", "score": 3}, {"label": "Согласен", "score": 4}, {"label": "Совершенно согласен", "score": 5}]'::jsonb,
 'DIRECT_SCALE', 30, 'FOUNDATIONAL',
 '{"tags": ["GENERAL", "JUNIOR"], "complexity_score": 0.3, "scenario_type": "WORKPLACE"}'::jsonb,
 true, 5),

('33333333-0001-0005-0006-000000000001',
 '22222222-0001-0001-0001-000000000005',
 'Я учитываю не только прямые, но и вторичные последствия решений для смежных процессов, даже если это значительно замедляет согласование',
 'LIKERT',
 '[{"label": "Совершенно не согласен", "score": 1}, {"label": "Не согласен", "score": 2}, {"label": "Нейтрально", "score": 3}, {"label": "Согласен", "score": 4}, {"label": "Совершенно согласен", "score": 5}]'::jsonb,
 'DIRECT_SCALE', 30, 'ADVANCED',
 '{"tags": ["GENERAL", "SENIOR"], "complexity_score": 0.7, "scenario_type": "WORKPLACE"}'::jsonb,
 true, 6)
ON CONFLICT (id) DO NOTHING;


-- ================================================================
-- COMPETENCY 2: Problem Solving (Решение проблем)
-- ================================================================

-- ----------------------------------------------------------------
-- Indicator 2.1: Определяет корневую причину проблемы
-- Gap: No FOUNDATIONAL questions
-- ----------------------------------------------------------------
INSERT INTO assessment_questions
    (id, behavioral_indicator_id, question_text, question_type,
     answer_options, scoring_rubric, time_limit, difficulty_level,
     metadata, is_active, order_index)
VALUES
('33333333-0002-0001-0005-000000000001',
 '22222222-0001-0002-0001-000000000001',
 'Когда что-то идёт не так, я задаю себе вопрос «почему это произошло»',
 'LIKERT',
 '[{"label": "Совершенно не согласен", "score": 1}, {"label": "Не согласен", "score": 2}, {"label": "Нейтрально", "score": 3}, {"label": "Согласен", "score": 4}, {"label": "Совершенно согласен", "score": 5}]'::jsonb,
 'DIRECT_SCALE', 30, 'FOUNDATIONAL',
 '{"tags": ["GENERAL", "JUNIOR"], "complexity_score": 0.3, "scenario_type": "WORKPLACE"}'::jsonb,
 true, 5)
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------
-- Indicator 2.2: Генерирует несколько вариантов решения
-- Gap: No ADVANCED questions
-- ----------------------------------------------------------------
INSERT INTO assessment_questions
    (id, behavioral_indicator_id, question_text, question_type,
     answer_options, scoring_rubric, time_limit, difficulty_level,
     metadata, is_active, order_index)
VALUES
('33333333-0002-0002-0005-000000000001',
 '22222222-0001-0002-0001-000000000002',
 'Я продолжаю генерировать альтернативные подходы, даже когда уже найдено работающее решение и команда готова приступить к реализации',
 'LIKERT',
 '[{"label": "Совершенно не согласен", "score": 1}, {"label": "Не согласен", "score": 2}, {"label": "Нейтрально", "score": 3}, {"label": "Согласен", "score": 4}, {"label": "Совершенно согласен", "score": 5}]'::jsonb,
 'DIRECT_SCALE', 30, 'ADVANCED',
 '{"tags": ["GENERAL", "SENIOR"], "complexity_score": 0.7, "scenario_type": "WORKPLACE"}'::jsonb,
 true, 5)
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------
-- Indicator 2.3: Применяет структурированный подход к решению
-- Gap: No FOUNDATIONAL questions
-- ----------------------------------------------------------------
INSERT INTO assessment_questions
    (id, behavioral_indicator_id, question_text, question_type,
     answer_options, scoring_rubric, time_limit, difficulty_level,
     metadata, is_active, order_index)
VALUES
('33333333-0002-0003-0005-000000000001',
 '22222222-0001-0002-0001-000000000003',
 'Перед решением задачи я обычно продумываю последовательность шагов',
 'LIKERT',
 '[{"label": "Совершенно не согласен", "score": 1}, {"label": "Не согласен", "score": 2}, {"label": "Нейтрально", "score": 3}, {"label": "Согласен", "score": 4}, {"label": "Совершенно согласен", "score": 5}]'::jsonb,
 'DIRECT_SCALE', 30, 'FOUNDATIONAL',
 '{"tags": ["GENERAL", "JUNIOR"], "complexity_score": 0.3, "scenario_type": "WORKPLACE"}'::jsonb,
 true, 5)
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------
-- Indicator 2.4: Тестирует решения перед полным внедрением
-- Gap: No ADVANCED questions
-- ----------------------------------------------------------------
INSERT INTO assessment_questions
    (id, behavioral_indicator_id, question_text, question_type,
     answer_options, scoring_rubric, time_limit, difficulty_level,
     metadata, is_active, order_index)
VALUES
('33333333-0002-0004-0005-000000000001',
 '22222222-0001-0002-0001-000000000004',
 'Я настаиваю на пилотном тестировании решения, даже когда жёсткие сроки проекта делают это организационно затруднительным',
 'LIKERT',
 '[{"label": "Совершенно не согласен", "score": 1}, {"label": "Не согласен", "score": 2}, {"label": "Нейтрально", "score": 3}, {"label": "Согласен", "score": 4}, {"label": "Совершенно согласен", "score": 5}]'::jsonb,
 'DIRECT_SCALE', 30, 'ADVANCED',
 '{"tags": ["GENERAL", "SENIOR"], "complexity_score": 0.7, "scenario_type": "WORKPLACE"}'::jsonb,
 true, 5)
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------
-- Indicator 2.5: Извлекает уроки из неудачных решений
-- Gap: No FOUNDATIONAL or ADVANCED questions (all INTERMEDIATE)
-- ----------------------------------------------------------------
INSERT INTO assessment_questions
    (id, behavioral_indicator_id, question_text, question_type,
     answer_options, scoring_rubric, time_limit, difficulty_level,
     metadata, is_active, order_index)
VALUES
('33333333-0002-0005-0005-000000000001',
 '22222222-0001-0002-0001-000000000005',
 'После завершения задачи я обычно размышляю о том, что можно было бы сделать иначе',
 'LIKERT',
 '[{"label": "Совершенно не согласен", "score": 1}, {"label": "Не согласен", "score": 2}, {"label": "Нейтрально", "score": 3}, {"label": "Согласен", "score": 4}, {"label": "Совершенно согласен", "score": 5}]'::jsonb,
 'DIRECT_SCALE', 30, 'FOUNDATIONAL',
 '{"tags": ["GENERAL", "JUNIOR"], "complexity_score": 0.3, "scenario_type": "WORKPLACE"}'::jsonb,
 true, 5),

('33333333-0002-0005-0006-000000000001',
 '22222222-0001-0002-0001-000000000005',
 'Я целенаправленно выбираю более рискованные подходы в определённых ситуациях, если это создаёт возможность для значимого профессионального обучения, даже при наличии безопасной альтернативы',
 'LIKERT',
 '[{"label": "Совершенно не согласен", "score": 1}, {"label": "Не согласен", "score": 2}, {"label": "Нейтрально", "score": 3}, {"label": "Согласен", "score": 4}, {"label": "Совершенно согласен", "score": 5}]'::jsonb,
 'DIRECT_SCALE', 30, 'ADVANCED',
 '{"tags": ["GENERAL", "SENIOR"], "complexity_score": 0.8, "scenario_type": "WORKPLACE"}'::jsonb,
 true, 6)
ON CONFLICT (id) DO NOTHING;


-- ================================================================
-- COMPETENCY 22: Logical Reasoning (Логическое рассуждение)
-- ================================================================

-- ----------------------------------------------------------------
-- Indicator 22.1: Строит аргументацию на фактах
-- Gap: No FOUNDATIONAL or ADVANCED questions (all INTERMEDIATE)
-- ----------------------------------------------------------------
INSERT INTO assessment_questions
    (id, behavioral_indicator_id, question_text, question_type,
     answer_options, scoring_rubric, time_limit, difficulty_level,
     metadata, is_active, order_index)
VALUES
('33333333-0022-0001-0005-000000000001',
 '22222222-0001-0022-0001-000000000001',
 'В дискуссиях я стараюсь опираться на факты, а не на предположения',
 'LIKERT',
 '[{"label": "Совершенно не согласен", "score": 1}, {"label": "Не согласен", "score": 2}, {"label": "Нейтрально", "score": 3}, {"label": "Согласен", "score": 4}, {"label": "Совершенно согласен", "score": 5}]'::jsonb,
 'DIRECT_SCALE', 30, 'FOUNDATIONAL',
 '{"tags": ["GENERAL", "JUNIOR"], "complexity_score": 0.3, "scenario_type": "WORKPLACE"}'::jsonb,
 true, 5),

('33333333-0022-0001-0006-000000000001',
 '22222222-0001-0022-0001-000000000001',
 'Представляя свою позицию, я включаю данные, которые её ослабляют, чтобы обеспечить полноту аргументации, даже если это снижает убедительность моих выводов',
 'LIKERT',
 '[{"label": "Совершенно не согласен", "score": 1}, {"label": "Не согласен", "score": 2}, {"label": "Нейтрально", "score": 3}, {"label": "Согласен", "score": 4}, {"label": "Совершенно согласен", "score": 5}]'::jsonb,
 'DIRECT_SCALE', 30, 'ADVANCED',
 '{"tags": ["GENERAL", "SENIOR"], "complexity_score": 0.8, "scenario_type": "WORKPLACE"}'::jsonb,
 true, 6)
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------
-- Indicator 22.2: Выявляет логические ошибки в рассуждениях
-- Gap: No FOUNDATIONAL questions
-- ----------------------------------------------------------------
INSERT INTO assessment_questions
    (id, behavioral_indicator_id, question_text, question_type,
     answer_options, scoring_rubric, time_limit, difficulty_level,
     metadata, is_active, order_index)
VALUES
('33333333-0022-0002-0005-000000000001',
 '22222222-0001-0022-0001-000000000002',
 'Мне важно, чтобы аргументы и выводы в рассуждениях были логически последовательны',
 'LIKERT',
 '[{"label": "Совершенно не согласен", "score": 1}, {"label": "Не согласен", "score": 2}, {"label": "Нейтрально", "score": 3}, {"label": "Согласен", "score": 4}, {"label": "Совершенно согласен", "score": 5}]'::jsonb,
 'DIRECT_SCALE', 30, 'FOUNDATIONAL',
 '{"tags": ["GENERAL", "JUNIOR"], "complexity_score": 0.3, "scenario_type": "TECHNICAL"}'::jsonb,
 true, 5)
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------
-- Indicator 22.3: Делает обоснованные выводы из данных
-- Gap: No FOUNDATIONAL questions
-- ----------------------------------------------------------------
INSERT INTO assessment_questions
    (id, behavioral_indicator_id, question_text, question_type,
     answer_options, scoring_rubric, time_limit, difficulty_level,
     metadata, is_active, order_index)
VALUES
('33333333-0022-0003-0005-000000000001',
 '22222222-0001-0022-0001-000000000003',
 'Прежде чем сделать вывод, я обычно проверяю, достаточно ли у меня информации',
 'LIKERT',
 '[{"label": "Совершенно не согласен", "score": 1}, {"label": "Не согласен", "score": 2}, {"label": "Нейтрально", "score": 3}, {"label": "Согласен", "score": 4}, {"label": "Совершенно согласен", "score": 5}]'::jsonb,
 'DIRECT_SCALE', 30, 'FOUNDATIONAL',
 '{"tags": ["GENERAL", "JUNIOR"], "complexity_score": 0.3, "scenario_type": "TECHNICAL"}'::jsonb,
 true, 5)
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------
-- Indicator 22.4: Различает корреляцию и причинность
-- Gap: No FOUNDATIONAL questions
-- ----------------------------------------------------------------
INSERT INTO assessment_questions
    (id, behavioral_indicator_id, question_text, question_type,
     answer_options, scoring_rubric, time_limit, difficulty_level,
     metadata, is_active, order_index)
VALUES
('33333333-0022-0004-0005-000000000001',
 '22222222-0001-0022-0001-000000000004',
 'Я понимаю, что совпадение двух событий не всегда означает, что одно является причиной другого',
 'LIKERT',
 '[{"label": "Совершенно не согласен", "score": 1}, {"label": "Не согласен", "score": 2}, {"label": "Нейтрально", "score": 3}, {"label": "Согласен", "score": 4}, {"label": "Совершенно согласен", "score": 5}]'::jsonb,
 'DIRECT_SCALE', 30, 'FOUNDATIONAL',
 '{"tags": ["GENERAL", "JUNIOR"], "complexity_score": 0.3, "scenario_type": "TECHNICAL"}'::jsonb,
 true, 5)
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------
-- Indicator 22.5: Проверяет гипотезы систематически
-- Gap: No FOUNDATIONAL questions
-- ----------------------------------------------------------------
INSERT INTO assessment_questions
    (id, behavioral_indicator_id, question_text, question_type,
     answer_options, scoring_rubric, time_limit, difficulty_level,
     metadata, is_active, order_index)
VALUES
('33333333-0022-0005-0005-000000000001',
 '22222222-0001-0022-0001-000000000005',
 'Перед тем как делать окончательные выводы, я стараюсь проверить свои предположения',
 'LIKERT',
 '[{"label": "Совершенно не согласен", "score": 1}, {"label": "Не согласен", "score": 2}, {"label": "Нейтрально", "score": 3}, {"label": "Согласен", "score": 4}, {"label": "Совершенно согласен", "score": 5}]'::jsonb,
 'DIRECT_SCALE', 30, 'FOUNDATIONAL',
 '{"tags": ["GENERAL", "JUNIOR"], "complexity_score": 0.3, "scenario_type": "TECHNICAL"}'::jsonb,
 true, 5)
ON CONFLICT (id) DO NOTHING;


-- ================================================================
-- VERIFICATION
-- ================================================================
DO $$
DECLARE
    v_count INTEGER;
    v_c1_count INTEGER;
    v_c2_count INTEGER;
    v_c22_count INTEGER;
BEGIN
    -- Total new questions
    SELECT COUNT(*) INTO v_count
    FROM assessment_questions
    WHERE id IN (
        '33333333-0001-0001-0005-000000000001',
        '33333333-0001-0002-0005-000000000001',
        '33333333-0001-0003-0005-000000000001',
        '33333333-0001-0004-0005-000000000001',
        '33333333-0001-0005-0005-000000000001',
        '33333333-0001-0005-0006-000000000001',
        '33333333-0002-0001-0005-000000000001',
        '33333333-0002-0002-0005-000000000001',
        '33333333-0002-0003-0005-000000000001',
        '33333333-0002-0004-0005-000000000001',
        '33333333-0002-0005-0005-000000000001',
        '33333333-0002-0005-0006-000000000001',
        '33333333-0022-0001-0005-000000000001',
        '33333333-0022-0001-0006-000000000001',
        '33333333-0022-0002-0005-000000000001',
        '33333333-0022-0003-0005-000000000001',
        '33333333-0022-0004-0005-000000000001',
        '33333333-0022-0005-0005-000000000001'
    );

    IF v_count <> 18 THEN
        RAISE EXCEPTION 'Expected 18 questions (inserted or pre-existing), found %', v_count;
    END IF;

    -- Per-competency verification
    SELECT COUNT(*) INTO v_c1_count
    FROM assessment_questions q
    JOIN behavioral_indicators bi ON q.behavioral_indicator_id = bi.id
    WHERE bi.competency_id = '11111111-1111-1111-1111-000000000001'
      AND q.is_active = true;

    SELECT COUNT(*) INTO v_c2_count
    FROM assessment_questions q
    JOIN behavioral_indicators bi ON q.behavioral_indicator_id = bi.id
    WHERE bi.competency_id = '11111111-1111-1111-1111-000000000002'
      AND q.is_active = true;

    SELECT COUNT(*) INTO v_c22_count
    FROM assessment_questions q
    JOIN behavioral_indicators bi ON q.behavioral_indicator_id = bi.id
    WHERE bi.competency_id = '11111111-1111-1111-1111-000000000022'
      AND q.is_active = true;

    RAISE NOTICE 'V34 verification: 18 new questions inserted (found %)', v_count;
    RAISE NOTICE '  C1  Analytical Thinking: % total active questions (was 20)', v_c1_count;
    RAISE NOTICE '  C2  Problem Solving:     % total active questions (was 20)', v_c2_count;
    RAISE NOTICE '  C22 Logical Reasoning:   % total active questions (was 20)', v_c22_count;
END $$;

-- ================================================================
-- DIFFICULTY COVERAGE AUDIT
-- ================================================================
-- Uncomment to verify all indicators now have all 3 bands:
--
-- SELECT
--     c.name AS competency,
--     bi.title AS indicator,
--     q.difficulty_level,
--     COUNT(*) AS question_count
-- FROM assessment_questions q
-- JOIN behavioral_indicators bi ON q.behavioral_indicator_id = bi.id
-- JOIN competencies c ON bi.competency_id = c.id
-- WHERE c.id IN (
--     '11111111-1111-1111-1111-000000000001',
--     '11111111-1111-1111-1111-000000000002',
--     '11111111-1111-1111-1111-000000000022'
-- )
-- AND q.is_active = true
-- GROUP BY c.name, bi.title, q.difficulty_level
-- ORDER BY c.name, bi.title,
--     CASE q.difficulty_level
--         WHEN 'FOUNDATIONAL' THEN 1
--         WHEN 'INTERMEDIATE' THEN 2
--         WHEN 'ADVANCED' THEN 3
--     END;
-- ================================================================
-- END OF MIGRATION
-- ================================================================
