-- ================================================================
-- TEST SESSION SIMULATION DATA - COMPACT REALISTIC DATASET
-- ================================================================
-- Version: V15__test_session_simulation_data.sql
-- Description: Generates realistic test session data with 7 fake users
--              simulating 6 months of production usage for demo/testing.
--
-- Key Features:
--   - 7 fake users with Clerk-style IDs
--   - 23 test sessions across all templates
--   - ~500 test answers with realistic distributions
--   - 15 test results with Big Five profiles
--   - All session statuses represented
--
-- User Profiles (7 users):
--   user_fake_001: Perfect performer (100%) - Tech Lead persona
--   user_fake_002: High performer (85%) - Senior Developer
--   user_fake_003: Above average (78%) - Mid-level Developer
--   user_fake_004: Average (65%) - Junior Developer
--   user_fake_005: Borderline pass (71%) - Career Changer
--   user_fake_006: Borderline fail (68%) - Entry Level
--   user_fake_007: Low performer (35%) - Needs Development
--
-- Session Distribution (23 sessions):
--   COMPLETED: 15 (65%) - Full results with Big Five profiles
--   IN_PROGRESS: 3 (13%) - Active sessions
--   ABANDONED: 2 (9%) - Stopped mid-test
--   TIMED_OUT: 2 (9%) - Ran out of time
--   NOT_STARTED: 1 (4%) - Created but never started
--
-- Template Distribution:
--   OVERVIEW (Competency Passport): 12 sessions
--   JOB_FIT (Software Developer): 6 sessions
--   TEAM_FIT (Team Compatibility): 5 sessions
--
-- Author: SkillSoft Team
-- Date: 2026-01-03
-- ================================================================

-- ================================================================
-- PHASE 1: CLEAR EXISTING SIMULATION DATA
-- ================================================================
-- Only delete sessions/results for fake users to preserve real data

DELETE FROM test_results WHERE clerk_user_id LIKE 'user_fake_%';
DELETE FROM test_answers WHERE session_id IN (
    SELECT id FROM test_sessions WHERE clerk_user_id LIKE 'user_fake_%'
);
DELETE FROM test_sessions WHERE clerk_user_id LIKE 'user_fake_%';

-- ================================================================
-- PHASE 2: CREATE COMPLETED TEST SESSIONS (15 sessions)
-- ================================================================

-- Session 1: user_fake_001 - OVERVIEW - Perfect performer
INSERT INTO test_sessions (id, template_id, clerk_user_id, status, started_at, completed_at, current_question_index, time_remaining_seconds, last_activity_at, created_at)
VALUES (
    'cccccccc-cccc-cccc-cccc-000000000001'::uuid,
    'aaaaaaaa-aaaa-aaaa-aaaa-000000000001'::uuid,
    'user_fake_001',
    'COMPLETED',
    CURRENT_TIMESTAMP - interval '180 days' - interval '35 minutes',
    CURRENT_TIMESTAMP - interval '180 days',
    50, 300,
    CURRENT_TIMESTAMP - interval '180 days',
    CURRENT_TIMESTAMP - interval '180 days' - interval '1 hour'
);

-- Session 2: user_fake_001 - JOB_FIT - Second test
INSERT INTO test_sessions (id, template_id, clerk_user_id, status, started_at, completed_at, current_question_index, time_remaining_seconds, last_activity_at, created_at)
VALUES (
    'cccccccc-cccc-cccc-cccc-000000000002'::uuid,
    'aaaaaaaa-aaaa-aaaa-aaaa-000000000002'::uuid,
    'user_fake_001',
    'COMPLETED',
    CURRENT_TIMESTAMP - interval '150 days' - interval '55 minutes',
    CURRENT_TIMESTAMP - interval '150 days',
    45, 180,
    CURRENT_TIMESTAMP - interval '150 days',
    CURRENT_TIMESTAMP - interval '150 days' - interval '2 hours'
);

-- Session 3: user_fake_002 - OVERVIEW - High performer
INSERT INTO test_sessions (id, template_id, clerk_user_id, status, started_at, completed_at, current_question_index, time_remaining_seconds, last_activity_at, created_at)
VALUES (
    'cccccccc-cccc-cccc-cccc-000000000003'::uuid,
    'aaaaaaaa-aaaa-aaaa-aaaa-000000000001'::uuid,
    'user_fake_002',
    'COMPLETED',
    CURRENT_TIMESTAMP - interval '160 days' - interval '42 minutes',
    CURRENT_TIMESTAMP - interval '160 days',
    50, 180,
    CURRENT_TIMESTAMP - interval '160 days',
    CURRENT_TIMESTAMP - interval '160 days' - interval '1 hour'
);

-- Session 4: user_fake_002 - JOB_FIT
INSERT INTO test_sessions (id, template_id, clerk_user_id, status, started_at, completed_at, current_question_index, time_remaining_seconds, last_activity_at, created_at)
VALUES (
    'cccccccc-cccc-cccc-cccc-000000000004'::uuid,
    'aaaaaaaa-aaaa-aaaa-aaaa-000000000002'::uuid,
    'user_fake_002',
    'COMPLETED',
    CURRENT_TIMESTAMP - interval '130 days' - interval '58 minutes',
    CURRENT_TIMESTAMP - interval '130 days',
    45, 120,
    CURRENT_TIMESTAMP - interval '130 days',
    CURRENT_TIMESTAMP - interval '130 days' - interval '2 hours'
);

-- Session 5: user_fake_002 - TEAM_FIT
INSERT INTO test_sessions (id, template_id, clerk_user_id, status, started_at, completed_at, current_question_index, time_remaining_seconds, last_activity_at, created_at)
VALUES (
    'cccccccc-cccc-cccc-cccc-000000000005'::uuid,
    'aaaaaaaa-aaaa-aaaa-aaaa-000000000003'::uuid,
    'user_fake_002',
    'COMPLETED',
    CURRENT_TIMESTAMP - interval '100 days' - interval '28 minutes',
    CURRENT_TIMESTAMP - interval '100 days',
    30, 600,
    CURRENT_TIMESTAMP - interval '100 days',
    CURRENT_TIMESTAMP - interval '100 days' - interval '1 hour'
);

-- Session 6: user_fake_003 - OVERVIEW - Above average
INSERT INTO test_sessions (id, template_id, clerk_user_id, status, started_at, completed_at, current_question_index, time_remaining_seconds, last_activity_at, created_at)
VALUES (
    'cccccccc-cccc-cccc-cccc-000000000006'::uuid,
    'aaaaaaaa-aaaa-aaaa-aaaa-000000000001'::uuid,
    'user_fake_003',
    'COMPLETED',
    CURRENT_TIMESTAMP - interval '140 days' - interval '40 minutes',
    CURRENT_TIMESTAMP - interval '140 days',
    50, 300,
    CURRENT_TIMESTAMP - interval '140 days',
    CURRENT_TIMESTAMP - interval '140 days' - interval '1 hour'
);

-- Session 7: user_fake_003 - TEAM_FIT
INSERT INTO test_sessions (id, template_id, clerk_user_id, status, started_at, completed_at, current_question_index, time_remaining_seconds, last_activity_at, created_at)
VALUES (
    'cccccccc-cccc-cccc-cccc-000000000007'::uuid,
    'aaaaaaaa-aaaa-aaaa-aaaa-000000000003'::uuid,
    'user_fake_003',
    'COMPLETED',
    CURRENT_TIMESTAMP - interval '90 days' - interval '25 minutes',
    CURRENT_TIMESTAMP - interval '90 days',
    30, 480,
    CURRENT_TIMESTAMP - interval '90 days',
    CURRENT_TIMESTAMP - interval '90 days' - interval '1 hour'
);

-- Session 8: user_fake_004 - OVERVIEW - Average
INSERT INTO test_sessions (id, template_id, clerk_user_id, status, started_at, completed_at, current_question_index, time_remaining_seconds, last_activity_at, created_at)
VALUES (
    'cccccccc-cccc-cccc-cccc-000000000008'::uuid,
    'aaaaaaaa-aaaa-aaaa-aaaa-000000000001'::uuid,
    'user_fake_004',
    'COMPLETED',
    CURRENT_TIMESTAMP - interval '120 days' - interval '44 minutes',
    CURRENT_TIMESTAMP - interval '120 days',
    50, 60,
    CURRENT_TIMESTAMP - interval '120 days',
    CURRENT_TIMESTAMP - interval '120 days' - interval '1 hour'
);

-- Session 9: user_fake_004 - JOB_FIT
INSERT INTO test_sessions (id, template_id, clerk_user_id, status, started_at, completed_at, current_question_index, time_remaining_seconds, last_activity_at, created_at)
VALUES (
    'cccccccc-cccc-cccc-cccc-000000000009'::uuid,
    'aaaaaaaa-aaaa-aaaa-aaaa-000000000002'::uuid,
    'user_fake_004',
    'COMPLETED',
    CURRENT_TIMESTAMP - interval '80 days' - interval '59 minutes',
    CURRENT_TIMESTAMP - interval '80 days',
    45, 0,
    CURRENT_TIMESTAMP - interval '80 days',
    CURRENT_TIMESTAMP - interval '80 days' - interval '2 hours'
);

-- Session 10: user_fake_005 - OVERVIEW - Borderline pass
INSERT INTO test_sessions (id, template_id, clerk_user_id, status, started_at, completed_at, current_question_index, time_remaining_seconds, last_activity_at, created_at)
VALUES (
    'cccccccc-cccc-cccc-cccc-000000000010'::uuid,
    'aaaaaaaa-aaaa-aaaa-aaaa-000000000001'::uuid,
    'user_fake_005',
    'COMPLETED',
    CURRENT_TIMESTAMP - interval '110 days' - interval '44 minutes',
    CURRENT_TIMESTAMP - interval '110 days',
    50, 60,
    CURRENT_TIMESTAMP - interval '110 days',
    CURRENT_TIMESTAMP - interval '110 days' - interval '1 hour'
);

-- Session 11: user_fake_005 - JOB_FIT
INSERT INTO test_sessions (id, template_id, clerk_user_id, status, started_at, completed_at, current_question_index, time_remaining_seconds, last_activity_at, created_at)
VALUES (
    'cccccccc-cccc-cccc-cccc-000000000011'::uuid,
    'aaaaaaaa-aaaa-aaaa-aaaa-000000000002'::uuid,
    'user_fake_005',
    'COMPLETED',
    CURRENT_TIMESTAMP - interval '70 days' - interval '58 minutes',
    CURRENT_TIMESTAMP - interval '70 days',
    45, 120,
    CURRENT_TIMESTAMP - interval '70 days',
    CURRENT_TIMESTAMP - interval '70 days' - interval '2 hours'
);

-- Session 12: user_fake_006 - OVERVIEW - Borderline fail
INSERT INTO test_sessions (id, template_id, clerk_user_id, status, started_at, completed_at, current_question_index, time_remaining_seconds, last_activity_at, created_at)
VALUES (
    'cccccccc-cccc-cccc-cccc-000000000012'::uuid,
    'aaaaaaaa-aaaa-aaaa-aaaa-000000000001'::uuid,
    'user_fake_006',
    'COMPLETED',
    CURRENT_TIMESTAMP - interval '95 days' - interval '44 minutes',
    CURRENT_TIMESTAMP - interval '95 days',
    50, 0,
    CURRENT_TIMESTAMP - interval '95 days',
    CURRENT_TIMESTAMP - interval '95 days' - interval '1 hour'
);

-- Session 13: user_fake_006 - TEAM_FIT
INSERT INTO test_sessions (id, template_id, clerk_user_id, status, started_at, completed_at, current_question_index, time_remaining_seconds, last_activity_at, created_at)
VALUES (
    'cccccccc-cccc-cccc-cccc-000000000013'::uuid,
    'aaaaaaaa-aaaa-aaaa-aaaa-000000000003'::uuid,
    'user_fake_006',
    'COMPLETED',
    CURRENT_TIMESTAMP - interval '60 days' - interval '29 minutes',
    CURRENT_TIMESTAMP - interval '60 days',
    30, 120,
    CURRENT_TIMESTAMP - interval '60 days',
    CURRENT_TIMESTAMP - interval '60 days' - interval '1 hour'
);

-- Session 14: user_fake_007 - OVERVIEW - Low performer
INSERT INTO test_sessions (id, template_id, clerk_user_id, status, started_at, completed_at, current_question_index, time_remaining_seconds, last_activity_at, created_at)
VALUES (
    'cccccccc-cccc-cccc-cccc-000000000014'::uuid,
    'aaaaaaaa-aaaa-aaaa-aaaa-000000000001'::uuid,
    'user_fake_007',
    'COMPLETED',
    CURRENT_TIMESTAMP - interval '85 days' - interval '44 minutes',
    CURRENT_TIMESTAMP - interval '85 days',
    50, 0,
    CURRENT_TIMESTAMP - interval '85 days',
    CURRENT_TIMESTAMP - interval '85 days' - interval '1 hour'
);

-- Session 15: user_fake_007 - JOB_FIT
INSERT INTO test_sessions (id, template_id, clerk_user_id, status, started_at, completed_at, current_question_index, time_remaining_seconds, last_activity_at, created_at)
VALUES (
    'cccccccc-cccc-cccc-cccc-000000000015'::uuid,
    'aaaaaaaa-aaaa-aaaa-aaaa-000000000002'::uuid,
    'user_fake_007',
    'COMPLETED',
    CURRENT_TIMESTAMP - interval '50 days' - interval '60 minutes',
    CURRENT_TIMESTAMP - interval '50 days',
    45, 0,
    CURRENT_TIMESTAMP - interval '50 days',
    CURRENT_TIMESTAMP - interval '50 days' - interval '2 hours'
);

-- ================================================================
-- PHASE 3: CREATE IN_PROGRESS SESSIONS (3 sessions)
-- ================================================================

-- Session 16: user_fake_001 - Currently taking TEAM_FIT
INSERT INTO test_sessions (id, template_id, clerk_user_id, status, started_at, completed_at, current_question_index, time_remaining_seconds, last_activity_at, created_at)
VALUES (
    'cccccccc-cccc-cccc-cccc-000000000016'::uuid,
    'aaaaaaaa-aaaa-aaaa-aaaa-000000000003'::uuid,
    'user_fake_001',
    'IN_PROGRESS',
    CURRENT_TIMESTAMP - interval '25 minutes',
    NULL,
    18, 900,
    CURRENT_TIMESTAMP - interval '2 minutes',
    CURRENT_TIMESTAMP - interval '30 minutes'
);

-- Session 17: user_fake_003 - Currently taking JOB_FIT
INSERT INTO test_sessions (id, template_id, clerk_user_id, status, started_at, completed_at, current_question_index, time_remaining_seconds, last_activity_at, created_at)
VALUES (
    'cccccccc-cccc-cccc-cccc-000000000017'::uuid,
    'aaaaaaaa-aaaa-aaaa-aaaa-000000000002'::uuid,
    'user_fake_003',
    'IN_PROGRESS',
    CURRENT_TIMESTAMP - interval '40 minutes',
    NULL,
    28, 1200,
    CURRENT_TIMESTAMP - interval '5 minutes',
    CURRENT_TIMESTAMP - interval '1 hour'
);

-- Session 18: user_fake_005 - Just started TEAM_FIT
INSERT INTO test_sessions (id, template_id, clerk_user_id, status, started_at, completed_at, current_question_index, time_remaining_seconds, last_activity_at, created_at)
VALUES (
    'cccccccc-cccc-cccc-cccc-000000000018'::uuid,
    'aaaaaaaa-aaaa-aaaa-aaaa-000000000003'::uuid,
    'user_fake_005',
    'IN_PROGRESS',
    CURRENT_TIMESTAMP - interval '8 minutes',
    NULL,
    5, 1620,
    CURRENT_TIMESTAMP - interval '1 minute',
    CURRENT_TIMESTAMP - interval '15 minutes'
);

-- ================================================================
-- PHASE 4: CREATE ABANDONED SESSIONS (2 sessions)
-- ================================================================

-- Session 19: user_fake_002 - Abandoned OVERVIEW after 15 questions
INSERT INTO test_sessions (id, template_id, clerk_user_id, status, started_at, completed_at, current_question_index, time_remaining_seconds, last_activity_at, created_at)
VALUES (
    'cccccccc-cccc-cccc-cccc-000000000019'::uuid,
    'aaaaaaaa-aaaa-aaaa-aaaa-000000000001'::uuid,
    'user_fake_002',
    'ABANDONED',
    CURRENT_TIMESTAMP - interval '45 days' - interval '18 minutes',
    NULL,
    15, 1620,
    CURRENT_TIMESTAMP - interval '45 days' - interval '5 minutes',
    CURRENT_TIMESTAMP - interval '45 days' - interval '30 minutes'
);

-- Session 20: user_fake_004 - Abandoned TEAM_FIT early
INSERT INTO test_sessions (id, template_id, clerk_user_id, status, started_at, completed_at, current_question_index, time_remaining_seconds, last_activity_at, created_at)
VALUES (
    'cccccccc-cccc-cccc-cccc-000000000020'::uuid,
    'aaaaaaaa-aaaa-aaaa-aaaa-000000000003'::uuid,
    'user_fake_004',
    'ABANDONED',
    CURRENT_TIMESTAMP - interval '30 days' - interval '12 minutes',
    NULL,
    8, 1080,
    CURRENT_TIMESTAMP - interval '30 days' - interval '3 minutes',
    CURRENT_TIMESTAMP - interval '30 days' - interval '20 minutes'
);

-- ================================================================
-- PHASE 5: CREATE TIMED_OUT SESSIONS (2 sessions)
-- ================================================================

-- Session 21: user_fake_001 - Timed out on OVERVIEW (almost finished)
INSERT INTO test_sessions (id, template_id, clerk_user_id, status, started_at, completed_at, current_question_index, time_remaining_seconds, last_activity_at, created_at)
VALUES (
    'cccccccc-cccc-cccc-cccc-000000000021'::uuid,
    'aaaaaaaa-aaaa-aaaa-aaaa-000000000001'::uuid,
    'user_fake_001',
    'TIMED_OUT',
    CURRENT_TIMESTAMP - interval '75 days' - interval '45 minutes',
    CURRENT_TIMESTAMP - interval '75 days',
    42, 0,
    CURRENT_TIMESTAMP - interval '75 days',
    CURRENT_TIMESTAMP - interval '75 days' - interval '1 hour'
);

-- Session 22: user_fake_006 - Timed out on JOB_FIT
INSERT INTO test_sessions (id, template_id, clerk_user_id, status, started_at, completed_at, current_question_index, time_remaining_seconds, last_activity_at, created_at)
VALUES (
    'cccccccc-cccc-cccc-cccc-000000000022'::uuid,
    'aaaaaaaa-aaaa-aaaa-aaaa-000000000002'::uuid,
    'user_fake_006',
    'TIMED_OUT',
    CURRENT_TIMESTAMP - interval '40 days' - interval '60 minutes',
    CURRENT_TIMESTAMP - interval '40 days',
    35, 0,
    CURRENT_TIMESTAMP - interval '40 days',
    CURRENT_TIMESTAMP - interval '40 days' - interval '2 hours'
);

-- ================================================================
-- PHASE 6: CREATE NOT_STARTED SESSION (1 session)
-- ================================================================

-- Session 23: user_fake_007 - Created but never started TEAM_FIT
INSERT INTO test_sessions (id, template_id, clerk_user_id, status, started_at, completed_at, current_question_index, time_remaining_seconds, last_activity_at, created_at)
VALUES (
    'cccccccc-cccc-cccc-cccc-000000000023'::uuid,
    'aaaaaaaa-aaaa-aaaa-aaaa-000000000003'::uuid,
    'user_fake_007',
    'NOT_STARTED',
    NULL,
    NULL,
    0, 1800,
    NULL,
    CURRENT_TIMESTAMP - interval '20 days'
);

-- ================================================================
-- PHASE 7: CREATE TEST ANSWERS FOR COMPLETED SESSIONS
-- ================================================================
-- Generate realistic answers with varying quality based on user profile

DO $$
DECLARE
    session_rec RECORD;
    question_rec RECORD;
    q_index INTEGER;
    base_score DECIMAL;
    likert_val INTEGER;
    time_spent INTEGER;
BEGIN
    FOR session_rec IN
        SELECT ts.id, ts.template_id, ts.clerk_user_id, ts.started_at,
               CASE ts.clerk_user_id
                   WHEN 'user_fake_001' THEN 6.5  -- Perfect: High Likert avg
                   WHEN 'user_fake_002' THEN 5.8  -- High: Good scores
                   WHEN 'user_fake_003' THEN 5.2  -- Above avg
                   WHEN 'user_fake_004' THEN 4.5  -- Average
                   WHEN 'user_fake_005' THEN 4.8  -- Borderline pass
                   WHEN 'user_fake_006' THEN 4.6  -- Borderline fail
                   WHEN 'user_fake_007' THEN 3.2  -- Low performer
                   ELSE 4.5
               END as base_likert
        FROM test_sessions ts
        WHERE ts.status = 'COMPLETED'
        AND ts.clerk_user_id LIKE 'user_fake_%'
    LOOP
        q_index := 0;

        FOR question_rec IN
            SELECT aq.id, aq.question_type
            FROM assessment_questions aq
            JOIN behavioral_indicators bi ON aq.behavioral_indicator_id = bi.id
            JOIN competencies c ON bi.competency_id = c.id
            WHERE c.id IN (
                SELECT (jsonb_array_elements_text(tt.competency_ids))::uuid
                FROM test_templates tt
                WHERE tt.id = session_rec.template_id
            )
            ORDER BY aq.id
            LIMIT 50
        LOOP
            -- Generate Likert value based on user's base score with variance
            likert_val := LEAST(7, GREATEST(1,
                ROUND(session_rec.base_likert + (random() - 0.5) * 2)::integer
            ));

            -- Time spent varies (20-80 seconds)
            time_spent := 20 + (random() * 60)::integer;

            INSERT INTO test_answers (
                id, session_id, question_id,
                likert_value, answered_at, time_spent_seconds,
                is_skipped, score, max_score
            ) VALUES (
                gen_random_uuid(),
                session_rec.id,
                question_rec.id,
                likert_val,
                session_rec.started_at + (q_index * interval '40 seconds'),
                time_spent,
                false,
                (likert_val - 1) / 6.0,
                1.0
            )
            ON CONFLICT (session_id, question_id) DO NOTHING;

            q_index := q_index + 1;
        END LOOP;
    END LOOP;
END $$;

-- ================================================================
-- PHASE 8: CREATE TEST ANSWERS FOR IN_PROGRESS SESSIONS
-- ================================================================

DO $$
DECLARE
    session_rec RECORD;
    question_rec RECORD;
    q_index INTEGER;
BEGIN
    FOR session_rec IN
        SELECT ts.id, ts.template_id, ts.started_at, ts.current_question_index
        FROM test_sessions ts
        WHERE ts.status = 'IN_PROGRESS'
        AND ts.clerk_user_id LIKE 'user_fake_%'
    LOOP
        q_index := 0;

        FOR question_rec IN
            SELECT aq.id
            FROM assessment_questions aq
            JOIN behavioral_indicators bi ON aq.behavioral_indicator_id = bi.id
            JOIN competencies c ON bi.competency_id = c.id
            WHERE c.id IN (
                SELECT (jsonb_array_elements_text(tt.competency_ids))::uuid
                FROM test_templates tt
                WHERE tt.id = session_rec.template_id
            )
            ORDER BY aq.id
            LIMIT session_rec.current_question_index
        LOOP
            INSERT INTO test_answers (
                id, session_id, question_id,
                likert_value, answered_at, time_spent_seconds,
                is_skipped, score, max_score
            ) VALUES (
                gen_random_uuid(),
                session_rec.id,
                question_rec.id,
                LEAST(7, GREATEST(1, ROUND(4.5 + (random() - 0.5) * 2)::integer)),
                session_rec.started_at + (q_index * interval '50 seconds'),
                25 + (random() * 50)::integer,
                false,
                (LEAST(7, GREATEST(1, ROUND(4.5 + (random() - 0.5) * 2)::integer)) - 1) / 6.0,
                1.0
            )
            ON CONFLICT (session_id, question_id) DO NOTHING;

            q_index := q_index + 1;
        END LOOP;
    END LOOP;
END $$;

-- ================================================================
-- PHASE 9: CREATE TEST RESULTS FOR COMPLETED SESSIONS
-- ================================================================

-- Add status column if missing (for ResultStatus enum)
ALTER TABLE test_results ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'COMPLETED';

-- Result 1: user_fake_001 - OVERVIEW - Perfect score
INSERT INTO test_results (
    id, session_id, clerk_user_id,
    overall_score, overall_percentage, percentile, passed, status,
    competency_scores, big_five_profile, extended_metrics,
    total_time_seconds, questions_answered, questions_skipped, completed_at
) VALUES (
    gen_random_uuid(),
    'cccccccc-cccc-cccc-cccc-000000000001'::uuid,
    'user_fake_001',
    50.0, 100.0, 99, true, 'COMPLETED',
    '[{"competencyId": "11111111-1111-1111-1111-000000000001", "competencyName": "Analytical Thinking", "score": 100, "maxScore": 100, "percentage": 100, "questionsAnswered": 10, "questionsCorrect": 10},
      {"competencyId": "11111111-1111-1111-1111-000000000002", "competencyName": "Problem Solving", "score": 98, "maxScore": 100, "percentage": 98, "questionsAnswered": 10, "questionsCorrect": 10},
      {"competencyId": "11111111-1111-1111-1111-000000000003", "competencyName": "Decision Making", "score": 100, "maxScore": 100, "percentage": 100, "questionsAnswered": 10, "questionsCorrect": 10},
      {"competencyId": "11111111-1111-1111-1111-000000000004", "competencyName": "Relationship Building", "score": 99, "maxScore": 100, "percentage": 99, "questionsAnswered": 10, "questionsCorrect": 10},
      {"competencyId": "11111111-1111-1111-1111-000000000005", "competencyName": "Conflict Resolution", "score": 100, "maxScore": 100, "percentage": 100, "questionsAnswered": 10, "questionsCorrect": 10}]'::jsonb,
    '{"OPENNESS": 95.2, "CONSCIENTIOUSNESS": 98.5, "EXTRAVERSION": 88.3, "AGREEABLENESS": 92.1, "EMOTIONAL_STABILITY": 96.7}'::jsonb,
    '{"diversityRatio": 0.85, "saturationRatio": 0.92, "teamFitMultiplier": 1.15, "consistencyScore": 0.98}'::jsonb,
    2100, 50, 0,
    CURRENT_TIMESTAMP - interval '180 days'
);

-- Result 2: user_fake_001 - JOB_FIT
INSERT INTO test_results (
    id, session_id, clerk_user_id,
    overall_score, overall_percentage, percentile, passed, status,
    competency_scores, big_five_profile, extended_metrics,
    total_time_seconds, questions_answered, questions_skipped, completed_at
) VALUES (
    gen_random_uuid(),
    'cccccccc-cccc-cccc-cccc-000000000002'::uuid,
    'user_fake_001',
    48.5, 97.0, 98, true, 'COMPLETED',
    '[{"competencyId": "11111111-1111-1111-1111-000000000001", "competencyName": "Analytical Thinking", "score": 97, "maxScore": 100, "percentage": 97, "questionsAnswered": 15, "questionsCorrect": 15},
      {"competencyId": "11111111-1111-1111-1111-000000000002", "competencyName": "Problem Solving", "score": 98, "maxScore": 100, "percentage": 98, "questionsAnswered": 15, "questionsCorrect": 15},
      {"competencyId": "11111111-1111-1111-1111-000000000003", "competencyName": "Decision Making", "score": 96, "maxScore": 100, "percentage": 96, "questionsAnswered": 15, "questionsCorrect": 14}]'::jsonb,
    '{"OPENNESS": 94.0, "CONSCIENTIOUSNESS": 97.2, "EXTRAVERSION": 86.5, "AGREEABLENESS": 90.8, "EMOTIONAL_STABILITY": 95.3}'::jsonb,
    '{"diversityRatio": 0.82, "saturationRatio": 0.88, "teamFitMultiplier": 1.12, "consistencyScore": 0.96}'::jsonb,
    3300, 45, 0,
    CURRENT_TIMESTAMP - interval '150 days'
);

-- Result 3: user_fake_002 - OVERVIEW - High performer
INSERT INTO test_results (
    id, session_id, clerk_user_id,
    overall_score, overall_percentage, percentile, passed, status,
    competency_scores, big_five_profile, extended_metrics,
    total_time_seconds, questions_answered, questions_skipped, completed_at
) VALUES (
    gen_random_uuid(),
    'cccccccc-cccc-cccc-cccc-000000000003'::uuid,
    'user_fake_002',
    42.5, 85.0, 92, true, 'COMPLETED',
    '[{"competencyId": "11111111-1111-1111-1111-000000000001", "competencyName": "Analytical Thinking", "score": 88, "maxScore": 100, "percentage": 88, "questionsAnswered": 10, "questionsCorrect": 9},
      {"competencyId": "11111111-1111-1111-1111-000000000002", "competencyName": "Problem Solving", "score": 85, "maxScore": 100, "percentage": 85, "questionsAnswered": 10, "questionsCorrect": 9},
      {"competencyId": "11111111-1111-1111-1111-000000000003", "competencyName": "Decision Making", "score": 82, "maxScore": 100, "percentage": 82, "questionsAnswered": 10, "questionsCorrect": 8},
      {"competencyId": "11111111-1111-1111-1111-000000000004", "competencyName": "Relationship Building", "score": 86, "maxScore": 100, "percentage": 86, "questionsAnswered": 10, "questionsCorrect": 9},
      {"competencyId": "11111111-1111-1111-1111-000000000005", "competencyName": "Conflict Resolution", "score": 84, "maxScore": 100, "percentage": 84, "questionsAnswered": 10, "questionsCorrect": 8}]'::jsonb,
    '{"OPENNESS": 78.5, "CONSCIENTIOUSNESS": 92.0, "EXTRAVERSION": 72.3, "AGREEABLENESS": 85.6, "EMOTIONAL_STABILITY": 88.2}'::jsonb,
    '{"diversityRatio": 0.75, "saturationRatio": 0.78, "teamFitMultiplier": 1.08, "consistencyScore": 0.89}'::jsonb,
    2520, 50, 0,
    CURRENT_TIMESTAMP - interval '160 days'
);

-- Result 4: user_fake_002 - JOB_FIT
INSERT INTO test_results (
    id, session_id, clerk_user_id,
    overall_score, overall_percentage, percentile, passed, status,
    competency_scores, big_five_profile, extended_metrics,
    total_time_seconds, questions_answered, questions_skipped, completed_at
) VALUES (
    gen_random_uuid(),
    'cccccccc-cccc-cccc-cccc-000000000004'::uuid,
    'user_fake_002',
    41.0, 82.0, 88, true, 'COMPLETED',
    '[{"competencyId": "11111111-1111-1111-1111-000000000001", "competencyName": "Analytical Thinking", "score": 84, "maxScore": 100, "percentage": 84, "questionsAnswered": 15, "questionsCorrect": 13},
      {"competencyId": "11111111-1111-1111-1111-000000000002", "competencyName": "Problem Solving", "score": 82, "maxScore": 100, "percentage": 82, "questionsAnswered": 15, "questionsCorrect": 12},
      {"competencyId": "11111111-1111-1111-1111-000000000003", "competencyName": "Decision Making", "score": 80, "maxScore": 100, "percentage": 80, "questionsAnswered": 15, "questionsCorrect": 12}]'::jsonb,
    '{"OPENNESS": 76.0, "CONSCIENTIOUSNESS": 90.5, "EXTRAVERSION": 70.8, "AGREEABLENESS": 83.2, "EMOTIONAL_STABILITY": 86.5}'::jsonb,
    '{"diversityRatio": 0.72, "saturationRatio": 0.75, "teamFitMultiplier": 1.05, "consistencyScore": 0.87}'::jsonb,
    3480, 45, 0,
    CURRENT_TIMESTAMP - interval '130 days'
);

-- Result 5: user_fake_002 - TEAM_FIT
INSERT INTO test_results (
    id, session_id, clerk_user_id,
    overall_score, overall_percentage, percentile, passed, status,
    competency_scores, big_five_profile, extended_metrics,
    total_time_seconds, questions_answered, questions_skipped, completed_at
) VALUES (
    gen_random_uuid(),
    'cccccccc-cccc-cccc-cccc-000000000005'::uuid,
    'user_fake_002',
    40.0, 80.0, 85, true, 'COMPLETED',
    '[{"competencyId": "11111111-1111-1111-1111-000000000004", "competencyName": "Relationship Building", "score": 82, "maxScore": 100, "percentage": 82, "questionsAnswered": 15, "questionsCorrect": 12},
      {"competencyId": "11111111-1111-1111-1111-000000000005", "competencyName": "Conflict Resolution", "score": 78, "maxScore": 100, "percentage": 78, "questionsAnswered": 15, "questionsCorrect": 12}]'::jsonb,
    '{"OPENNESS": 74.5, "CONSCIENTIOUSNESS": 88.0, "EXTRAVERSION": 68.5, "AGREEABLENESS": 81.0, "EMOTIONAL_STABILITY": 84.2}'::jsonb,
    '{"diversityRatio": 0.70, "saturationRatio": 0.72, "teamFitMultiplier": 1.03, "consistencyScore": 0.85}'::jsonb,
    1680, 30, 0,
    CURRENT_TIMESTAMP - interval '100 days'
);

-- Result 6: user_fake_003 - OVERVIEW - Above average (High Openness)
INSERT INTO test_results (
    id, session_id, clerk_user_id,
    overall_score, overall_percentage, percentile, passed, status,
    competency_scores, big_five_profile, extended_metrics,
    total_time_seconds, questions_answered, questions_skipped, completed_at
) VALUES (
    gen_random_uuid(),
    'cccccccc-cccc-cccc-cccc-000000000006'::uuid,
    'user_fake_003',
    39.0, 78.0, 78, true, 'COMPLETED',
    '[{"competencyId": "11111111-1111-1111-1111-000000000001", "competencyName": "Analytical Thinking", "score": 82, "maxScore": 100, "percentage": 82, "questionsAnswered": 10, "questionsCorrect": 8},
      {"competencyId": "11111111-1111-1111-1111-000000000002", "competencyName": "Problem Solving", "score": 78, "maxScore": 100, "percentage": 78, "questionsAnswered": 10, "questionsCorrect": 8},
      {"competencyId": "11111111-1111-1111-1111-000000000003", "competencyName": "Decision Making", "score": 75, "maxScore": 100, "percentage": 75, "questionsAnswered": 10, "questionsCorrect": 7},
      {"competencyId": "11111111-1111-1111-1111-000000000004", "competencyName": "Relationship Building", "score": 77, "maxScore": 100, "percentage": 77, "questionsAnswered": 10, "questionsCorrect": 8},
      {"competencyId": "11111111-1111-1111-1111-000000000005", "competencyName": "Conflict Resolution", "score": 78, "maxScore": 100, "percentage": 78, "questionsAnswered": 10, "questionsCorrect": 8}]'::jsonb,
    '{"OPENNESS": 94.0, "CONSCIENTIOUSNESS": 68.5, "EXTRAVERSION": 72.0, "AGREEABLENESS": 75.5, "EMOTIONAL_STABILITY": 70.2}'::jsonb,
    '{"diversityRatio": 0.68, "saturationRatio": 0.70, "teamFitMultiplier": 1.02, "consistencyScore": 0.82}'::jsonb,
    2400, 50, 0,
    CURRENT_TIMESTAMP - interval '140 days'
);

-- Result 7: user_fake_003 - TEAM_FIT
INSERT INTO test_results (
    id, session_id, clerk_user_id,
    overall_score, overall_percentage, percentile, passed, status,
    competency_scores, big_five_profile, extended_metrics,
    total_time_seconds, questions_answered, questions_skipped, completed_at
) VALUES (
    gen_random_uuid(),
    'cccccccc-cccc-cccc-cccc-000000000007'::uuid,
    'user_fake_003',
    37.5, 75.0, 72, true, 'COMPLETED',
    '[{"competencyId": "11111111-1111-1111-1111-000000000004", "competencyName": "Relationship Building", "score": 76, "maxScore": 100, "percentage": 76, "questionsAnswered": 15, "questionsCorrect": 11},
      {"competencyId": "11111111-1111-1111-1111-000000000005", "competencyName": "Conflict Resolution", "score": 74, "maxScore": 100, "percentage": 74, "questionsAnswered": 15, "questionsCorrect": 11}]'::jsonb,
    '{"OPENNESS": 92.5, "CONSCIENTIOUSNESS": 66.0, "EXTRAVERSION": 70.5, "AGREEABLENESS": 73.0, "EMOTIONAL_STABILITY": 68.8}'::jsonb,
    '{"diversityRatio": 0.65, "saturationRatio": 0.68, "teamFitMultiplier": 1.00, "consistencyScore": 0.80}'::jsonb,
    1500, 30, 0,
    CURRENT_TIMESTAMP - interval '90 days'
);

-- Result 8: user_fake_004 - OVERVIEW - Average (High Extraversion)
INSERT INTO test_results (
    id, session_id, clerk_user_id,
    overall_score, overall_percentage, percentile, passed, status,
    competency_scores, big_five_profile, extended_metrics,
    total_time_seconds, questions_answered, questions_skipped, completed_at
) VALUES (
    gen_random_uuid(),
    'cccccccc-cccc-cccc-cccc-000000000008'::uuid,
    'user_fake_004',
    32.5, 65.0, 55, false, 'COMPLETED',
    '[{"competencyId": "11111111-1111-1111-1111-000000000001", "competencyName": "Analytical Thinking", "score": 62, "maxScore": 100, "percentage": 62, "questionsAnswered": 10, "questionsCorrect": 6},
      {"competencyId": "11111111-1111-1111-1111-000000000002", "competencyName": "Problem Solving", "score": 68, "maxScore": 100, "percentage": 68, "questionsAnswered": 10, "questionsCorrect": 7},
      {"competencyId": "11111111-1111-1111-1111-000000000003", "competencyName": "Decision Making", "score": 64, "maxScore": 100, "percentage": 64, "questionsAnswered": 10, "questionsCorrect": 6},
      {"competencyId": "11111111-1111-1111-1111-000000000004", "competencyName": "Relationship Building", "score": 70, "maxScore": 100, "percentage": 70, "questionsAnswered": 10, "questionsCorrect": 7},
      {"competencyId": "11111111-1111-1111-1111-000000000005", "competencyName": "Conflict Resolution", "score": 61, "maxScore": 100, "percentage": 61, "questionsAnswered": 10, "questionsCorrect": 6}]'::jsonb,
    '{"OPENNESS": 62.0, "CONSCIENTIOUSNESS": 58.5, "EXTRAVERSION": 92.0, "AGREEABLENESS": 68.0, "EMOTIONAL_STABILITY": 60.5}'::jsonb,
    '{"diversityRatio": 0.58, "saturationRatio": 0.60, "teamFitMultiplier": 0.95, "consistencyScore": 0.72}'::jsonb,
    2640, 50, 0,
    CURRENT_TIMESTAMP - interval '120 days'
);

-- Result 9: user_fake_004 - JOB_FIT
INSERT INTO test_results (
    id, session_id, clerk_user_id,
    overall_score, overall_percentage, percentile, passed, status,
    competency_scores, big_five_profile, extended_metrics,
    total_time_seconds, questions_answered, questions_skipped, completed_at
) VALUES (
    gen_random_uuid(),
    'cccccccc-cccc-cccc-cccc-000000000009'::uuid,
    'user_fake_004',
    31.0, 62.0, 48, false, 'COMPLETED',
    '[{"competencyId": "11111111-1111-1111-1111-000000000001", "competencyName": "Analytical Thinking", "score": 60, "maxScore": 100, "percentage": 60, "questionsAnswered": 15, "questionsCorrect": 9},
      {"competencyId": "11111111-1111-1111-1111-000000000002", "competencyName": "Problem Solving", "score": 65, "maxScore": 100, "percentage": 65, "questionsAnswered": 15, "questionsCorrect": 10},
      {"competencyId": "11111111-1111-1111-1111-000000000003", "competencyName": "Decision Making", "score": 61, "maxScore": 100, "percentage": 61, "questionsAnswered": 15, "questionsCorrect": 9}]'::jsonb,
    '{"OPENNESS": 60.0, "CONSCIENTIOUSNESS": 56.0, "EXTRAVERSION": 90.5, "AGREEABLENESS": 65.5, "EMOTIONAL_STABILITY": 58.0}'::jsonb,
    '{"diversityRatio": 0.55, "saturationRatio": 0.58, "teamFitMultiplier": 0.92, "consistencyScore": 0.70}'::jsonb,
    3540, 45, 0,
    CURRENT_TIMESTAMP - interval '80 days'
);

-- Result 10: user_fake_005 - OVERVIEW - Borderline pass (Balanced)
INSERT INTO test_results (
    id, session_id, clerk_user_id,
    overall_score, overall_percentage, percentile, passed, status,
    competency_scores, big_five_profile, extended_metrics,
    total_time_seconds, questions_answered, questions_skipped, completed_at
) VALUES (
    gen_random_uuid(),
    'cccccccc-cccc-cccc-cccc-000000000010'::uuid,
    'user_fake_005',
    35.5, 71.0, 60, true, 'COMPLETED',
    '[{"competencyId": "11111111-1111-1111-1111-000000000001", "competencyName": "Analytical Thinking", "score": 72, "maxScore": 100, "percentage": 72, "questionsAnswered": 10, "questionsCorrect": 7},
      {"competencyId": "11111111-1111-1111-1111-000000000002", "competencyName": "Problem Solving", "score": 70, "maxScore": 100, "percentage": 70, "questionsAnswered": 10, "questionsCorrect": 7},
      {"competencyId": "11111111-1111-1111-1111-000000000003", "competencyName": "Decision Making", "score": 71, "maxScore": 100, "percentage": 71, "questionsAnswered": 10, "questionsCorrect": 7},
      {"competencyId": "11111111-1111-1111-1111-000000000004", "competencyName": "Relationship Building", "score": 72, "maxScore": 100, "percentage": 72, "questionsAnswered": 10, "questionsCorrect": 7},
      {"competencyId": "11111111-1111-1111-1111-000000000005", "competencyName": "Conflict Resolution", "score": 70, "maxScore": 100, "percentage": 70, "questionsAnswered": 10, "questionsCorrect": 7}]'::jsonb,
    '{"OPENNESS": 70.0, "CONSCIENTIOUSNESS": 72.0, "EXTRAVERSION": 68.5, "AGREEABLENESS": 71.0, "EMOTIONAL_STABILITY": 69.5}'::jsonb,
    '{"diversityRatio": 0.62, "saturationRatio": 0.65, "teamFitMultiplier": 0.98, "consistencyScore": 0.78}'::jsonb,
    2640, 50, 0,
    CURRENT_TIMESTAMP - interval '110 days'
);

-- Result 11: user_fake_005 - JOB_FIT
INSERT INTO test_results (
    id, session_id, clerk_user_id,
    overall_score, overall_percentage, percentile, passed, status,
    competency_scores, big_five_profile, extended_metrics,
    total_time_seconds, questions_answered, questions_skipped, completed_at
) VALUES (
    gen_random_uuid(),
    'cccccccc-cccc-cccc-cccc-000000000011'::uuid,
    'user_fake_005',
    35.0, 70.0, 58, true, 'COMPLETED',
    '[{"competencyId": "11111111-1111-1111-1111-000000000001", "competencyName": "Analytical Thinking", "score": 70, "maxScore": 100, "percentage": 70, "questionsAnswered": 15, "questionsCorrect": 10},
      {"competencyId": "11111111-1111-1111-1111-000000000002", "competencyName": "Problem Solving", "score": 71, "maxScore": 100, "percentage": 71, "questionsAnswered": 15, "questionsCorrect": 11},
      {"competencyId": "11111111-1111-1111-1111-000000000003", "competencyName": "Decision Making", "score": 69, "maxScore": 100, "percentage": 69, "questionsAnswered": 15, "questionsCorrect": 10}]'::jsonb,
    '{"OPENNESS": 68.5, "CONSCIENTIOUSNESS": 70.0, "EXTRAVERSION": 66.5, "AGREEABLENESS": 69.0, "EMOTIONAL_STABILITY": 67.8}'::jsonb,
    '{"diversityRatio": 0.60, "saturationRatio": 0.62, "teamFitMultiplier": 0.96, "consistencyScore": 0.76}'::jsonb,
    3480, 45, 0,
    CURRENT_TIMESTAMP - interval '70 days'
);

-- Result 12: user_fake_006 - OVERVIEW - Borderline fail (High Agreeableness)
INSERT INTO test_results (
    id, session_id, clerk_user_id,
    overall_score, overall_percentage, percentile, passed, status,
    competency_scores, big_five_profile, extended_metrics,
    total_time_seconds, questions_answered, questions_skipped, completed_at
) VALUES (
    gen_random_uuid(),
    'cccccccc-cccc-cccc-cccc-000000000012'::uuid,
    'user_fake_006',
    34.0, 68.0, 45, false, 'COMPLETED',
    '[{"competencyId": "11111111-1111-1111-1111-000000000001", "competencyName": "Analytical Thinking", "score": 65, "maxScore": 100, "percentage": 65, "questionsAnswered": 10, "questionsCorrect": 6},
      {"competencyId": "11111111-1111-1111-1111-000000000002", "competencyName": "Problem Solving", "score": 68, "maxScore": 100, "percentage": 68, "questionsAnswered": 10, "questionsCorrect": 7},
      {"competencyId": "11111111-1111-1111-1111-000000000003", "competencyName": "Decision Making", "score": 66, "maxScore": 100, "percentage": 66, "questionsAnswered": 10, "questionsCorrect": 7},
      {"competencyId": "11111111-1111-1111-1111-000000000004", "competencyName": "Relationship Building", "score": 72, "maxScore": 100, "percentage": 72, "questionsAnswered": 10, "questionsCorrect": 7},
      {"competencyId": "11111111-1111-1111-1111-000000000005", "competencyName": "Conflict Resolution", "score": 69, "maxScore": 100, "percentage": 69, "questionsAnswered": 10, "questionsCorrect": 7}]'::jsonb,
    '{"OPENNESS": 58.0, "CONSCIENTIOUSNESS": 62.0, "EXTRAVERSION": 55.5, "AGREEABLENESS": 95.0, "EMOTIONAL_STABILITY": 65.2}'::jsonb,
    '{"diversityRatio": 0.55, "saturationRatio": 0.58, "teamFitMultiplier": 0.92, "consistencyScore": 0.74}'::jsonb,
    2640, 50, 0,
    CURRENT_TIMESTAMP - interval '95 days'
);

-- Result 13: user_fake_006 - TEAM_FIT
INSERT INTO test_results (
    id, session_id, clerk_user_id,
    overall_score, overall_percentage, percentile, passed, status,
    competency_scores, big_five_profile, extended_metrics,
    total_time_seconds, questions_answered, questions_skipped, completed_at
) VALUES (
    gen_random_uuid(),
    'cccccccc-cccc-cccc-cccc-000000000013'::uuid,
    'user_fake_006',
    33.5, 67.0, 42, false, 'COMPLETED',
    '[{"competencyId": "11111111-1111-1111-1111-000000000004", "competencyName": "Relationship Building", "score": 70, "maxScore": 100, "percentage": 70, "questionsAnswered": 15, "questionsCorrect": 10},
      {"competencyId": "11111111-1111-1111-1111-000000000005", "competencyName": "Conflict Resolution", "score": 64, "maxScore": 100, "percentage": 64, "questionsAnswered": 15, "questionsCorrect": 10}]'::jsonb,
    '{"OPENNESS": 56.5, "CONSCIENTIOUSNESS": 60.0, "EXTRAVERSION": 53.0, "AGREEABLENESS": 93.5, "EMOTIONAL_STABILITY": 63.0}'::jsonb,
    '{"diversityRatio": 0.52, "saturationRatio": 0.55, "teamFitMultiplier": 0.90, "consistencyScore": 0.72}'::jsonb,
    1740, 30, 0,
    CURRENT_TIMESTAMP - interval '60 days'
);

-- Result 14: user_fake_007 - OVERVIEW - Low performer (High Emotional Stability only)
INSERT INTO test_results (
    id, session_id, clerk_user_id,
    overall_score, overall_percentage, percentile, passed, status,
    competency_scores, big_five_profile, extended_metrics,
    total_time_seconds, questions_answered, questions_skipped, completed_at
) VALUES (
    gen_random_uuid(),
    'cccccccc-cccc-cccc-cccc-000000000014'::uuid,
    'user_fake_007',
    17.5, 35.0, 12, false, 'COMPLETED',
    '[{"competencyId": "11111111-1111-1111-1111-000000000001", "competencyName": "Analytical Thinking", "score": 32, "maxScore": 100, "percentage": 32, "questionsAnswered": 10, "questionsCorrect": 3},
      {"competencyId": "11111111-1111-1111-1111-000000000002", "competencyName": "Problem Solving", "score": 38, "maxScore": 100, "percentage": 38, "questionsAnswered": 10, "questionsCorrect": 4},
      {"competencyId": "11111111-1111-1111-1111-000000000003", "competencyName": "Decision Making", "score": 35, "maxScore": 100, "percentage": 35, "questionsAnswered": 10, "questionsCorrect": 3},
      {"competencyId": "11111111-1111-1111-1111-000000000004", "competencyName": "Relationship Building", "score": 36, "maxScore": 100, "percentage": 36, "questionsAnswered": 10, "questionsCorrect": 4},
      {"competencyId": "11111111-1111-1111-1111-000000000005", "competencyName": "Conflict Resolution", "score": 34, "maxScore": 100, "percentage": 34, "questionsAnswered": 10, "questionsCorrect": 3}]'::jsonb,
    '{"OPENNESS": 38.0, "CONSCIENTIOUSNESS": 32.5, "EXTRAVERSION": 42.0, "AGREEABLENESS": 35.0, "EMOTIONAL_STABILITY": 85.0}'::jsonb,
    '{"diversityRatio": 0.35, "saturationRatio": 0.38, "teamFitMultiplier": 0.75, "consistencyScore": 0.55}'::jsonb,
    2640, 50, 0,
    CURRENT_TIMESTAMP - interval '85 days'
);

-- Result 15: user_fake_007 - JOB_FIT - Very low
INSERT INTO test_results (
    id, session_id, clerk_user_id,
    overall_score, overall_percentage, percentile, passed, status,
    competency_scores, big_five_profile, extended_metrics,
    total_time_seconds, questions_answered, questions_skipped, completed_at
) VALUES (
    gen_random_uuid(),
    'cccccccc-cccc-cccc-cccc-000000000015'::uuid,
    'user_fake_007',
    15.0, 30.0, 8, false, 'COMPLETED',
    '[{"competencyId": "11111111-1111-1111-1111-000000000001", "competencyName": "Analytical Thinking", "score": 28, "maxScore": 100, "percentage": 28, "questionsAnswered": 15, "questionsCorrect": 4},
      {"competencyId": "11111111-1111-1111-1111-000000000002", "competencyName": "Problem Solving", "score": 32, "maxScore": 100, "percentage": 32, "questionsAnswered": 15, "questionsCorrect": 5},
      {"competencyId": "11111111-1111-1111-1111-000000000003", "competencyName": "Decision Making", "score": 30, "maxScore": 100, "percentage": 30, "questionsAnswered": 15, "questionsCorrect": 4}]'::jsonb,
    '{"OPENNESS": 35.0, "CONSCIENTIOUSNESS": 28.5, "EXTRAVERSION": 40.0, "AGREEABLENESS": 32.0, "EMOTIONAL_STABILITY": 82.5}'::jsonb,
    '{"diversityRatio": 0.30, "saturationRatio": 0.32, "teamFitMultiplier": 0.70, "consistencyScore": 0.50}'::jsonb,
    3600, 45, 0,
    CURRENT_TIMESTAMP - interval '50 days'
);

-- ================================================================
-- PHASE 10: UPDATE QUESTION_ORDER FOR ALL SESSIONS
-- ================================================================

UPDATE test_sessions ts SET
    question_order = (
        SELECT jsonb_agg(ta.question_id ORDER BY ta.answered_at)
        FROM test_answers ta
        WHERE ta.session_id = ts.id
    )
WHERE ts.clerk_user_id LIKE 'user_fake_%'
AND EXISTS (SELECT 1 FROM test_answers WHERE session_id = ts.id);

-- ================================================================
-- PHASE 11: VALIDATION SUMMARY
-- ================================================================
-- Expected counts after migration:
--
-- Sessions by status:
--   COMPLETED: 15
--   IN_PROGRESS: 3
--   ABANDONED: 2
--   TIMED_OUT: 2
--   NOT_STARTED: 1
--   TOTAL: 23
--
-- Test Results: 15
-- Big Five Profiles: 15 (all results have profiles)
--
-- User Personas:
--   user_fake_001: Perfect (100%, 97%) - Tech Lead
--   user_fake_002: High (85%, 82%, 80%) - Senior Dev
--   user_fake_003: Above Avg (78%, 75%) - Mid Dev
--   user_fake_004: Average (65%, 62%) - Junior Dev
--   user_fake_005: Borderline Pass (71%, 70%) - Career Changer
--   user_fake_006: Borderline Fail (68%, 67%) - Entry Level
--   user_fake_007: Low (35%, 30%) - Needs Development
--
-- Big Five Highlights:
--   user_fake_001: Balanced High (all 88-98%)
--   user_fake_002: High Conscientiousness (92%)
--   user_fake_003: High Openness (94%)
--   user_fake_004: High Extraversion (92%)
--   user_fake_005: Balanced (all ~68-72%)
--   user_fake_006: High Agreeableness (95%)
--   user_fake_007: High Emotional Stability (85%)
--
-- Validation Queries:
-- SELECT status, COUNT(*) FROM test_sessions WHERE clerk_user_id LIKE 'user_fake_%' GROUP BY status;
-- SELECT clerk_user_id, overall_percentage, passed FROM test_results WHERE clerk_user_id LIKE 'user_fake_%' ORDER BY overall_percentage DESC;
-- SELECT clerk_user_id, big_five_profile FROM test_results WHERE clerk_user_id LIKE 'user_fake_%';
--
-- ================================================================
-- END OF MIGRATION
-- ================================================================
