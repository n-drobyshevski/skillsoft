-- ================================================================
-- UI/UX DESIGNER JOB FIT TEMPLATE
-- ================================================================
-- Version: V32__add_ux_designer_job_fit_template.sql
-- Description: Add Job Fit template for UI/UX Designer role
--              based on O*NET SOC 15-1255.00 (Web and Digital
--              Interface Designers)
-- Author: SkillSoft Team
-- Date: 2026-02-22
--
-- O*NET Reference:
--   Code: 15-1255.00
--   Title: Web and Digital Interface Designers
--   Holland Code: IAC (Investigative, Artistic, Conventional)
--   Key Work Styles: Innovation, Adaptability, Intellectual
--                    Curiosity, Attention to Detail, Cooperation
--
-- Competency Mapping (9 competencies):
--   C1  Analytical Thinking    - UX analysis, user behavior patterns
--   C2  Problem Solving        - Design problem solving, user pain points
--   C11 Learning Agility       - O*NET "Intellectual Curiosity"
--   C12 Resilience             - O*NET "Adaptability", iterative feedback
--   C14 Empathy                - Core UX: understanding user needs
--   C16 Verbal Communication   - Stakeholder presentations, design reviews
--   C18 Active Listening       - User research, requirements gathering
--   C19 Teamwork               - O*NET "Cooperation", cross-functional work
--   C24 Systems Thinking       - Information architecture, design systems
-- ================================================================

INSERT INTO test_templates (
    id,
    name,
    description,
    version,
    parent_id,
    status,
    goal,
    typed_blueprint,
    competency_ids,
    questions_per_indicator,
    time_limit_minutes,
    passing_score,
    is_active,
    shuffle_questions,
    shuffle_options,
    allow_skip,
    allow_back_navigation,
    show_results_immediately,
    visibility,
    created_at,
    updated_at
) VALUES (
    'aaaaaaaa-aaaa-aaaa-aaaa-000000000004',
    'Оценка соответствия должности: UI/UX Дизайнер',
    'Целевая оценка соответствия кандидата требованиям позиции UI/UX Designer по стандарту O*NET (15-1255.00 — Web and Digital Interface Designers). '
    || 'Оценивает ключевые компетенции для дизайна цифровых интерфейсов: аналитическое мышление, эмпатию к пользователям, решение проблем, системное мышление и навыки межфункционального сотрудничества. '
    || 'Использует взвешенное косинусное сходство для сопоставления профиля кандидата с эталонным профилем должности.',
    1,
    NULL,
    'PUBLISHED',
    'JOB_FIT',
    '{
        "strategy": "JOB_FIT",
        "onetSocCode": "15-1255.00",
        "strictnessLevel": 65,
        "passportMaxAgeDays": 180,
        "adaptivity": {
            "mode": "LINEAR",
            "allowBacktracking": true
        }
    }'::jsonb,
    '[
        "11111111-1111-1111-1111-000000000001",
        "11111111-1111-1111-1111-000000000002",
        "11111111-1111-1111-1111-000000000011",
        "11111111-1111-1111-1111-000000000012",
        "11111111-1111-1111-1111-000000000014",
        "11111111-1111-1111-1111-000000000016",
        "11111111-1111-1111-1111-000000000018",
        "11111111-1111-1111-1111-000000000019",
        "11111111-1111-1111-1111-000000000024"
    ]'::jsonb,
    3,
    45,
    65.0,
    true,
    true,
    true,
    true,
    true,
    true,
    'PUBLIC',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- ================================================================
-- VERIFICATION
-- ================================================================
DO $$
DECLARE
    v_name TEXT;
    v_goal TEXT;
    v_soc TEXT;
    v_comp_count INTEGER;
BEGIN
    SELECT
        name,
        goal,
        typed_blueprint->>'onetSocCode',
        jsonb_array_length(competency_ids)
    INTO v_name, v_goal, v_soc, v_comp_count
    FROM test_templates
    WHERE id = 'aaaaaaaa-aaaa-aaaa-aaaa-000000000004';

    RAISE NOTICE 'Template created: % | Goal: % | O*NET: % | Competencies: %',
        v_name, v_goal, v_soc, v_comp_count;
END $$;

-- ================================================================
-- END OF MIGRATION
-- ================================================================
