-- ================================================================
-- FIX LIKERT QUESTION DIFFICULTY LEVELS
-- ================================================================
-- Version: V12__fix_likert_difficulty_levels.sql
-- Description: Corrects difficulty levels for LIKERT questions that
--              were marked ADVANCED but are too simple for that level.
--
-- Analysis Criteria (from workflow-architect):
-- - FOUNDATIONAL: Simple self-report, single behavior, no context
-- - INTERMEDIATE: Applied behavior, single conditional, basic complexity
-- - ADVANCED: Multiple stakeholders, trade-offs, uncertainty handling,
--             systems thinking, strategic context
--
-- Rule Violations Fixed:
-- LE-001: Simple agreement statement marked ADVANCED
-- LE-002: Generic statement without situational complexity
-- LE-003: No multi-stakeholder context for ADVANCED
-- LE-004: No conditional/situation markers
-- LE-005: Single behavior without organizational context
-- ================================================================

-- ================================================================
-- PHASE 1: V5 QUESTIONS - Competencies 1-6 (24 ADVANCED → 17 INTERMEDIATE)
-- ================================================================

-- COMPETENCY 1: Analytical Thinking (Аналитическое мышление)

-- ID: 33333333-0001-0004-0001-000000000001
-- Text: "Я часто замечаю связи между событиями, которые другие не видят"
-- Rule: LE-002 (Generic pattern recognition without situational context)
-- Reason: Simple self-report of pattern recognition ability
UPDATE assessment_questions
SET difficulty_level = 'INTERMEDIATE',
    metadata = jsonb_set(metadata, '{complexity_score}', '0.5')
WHERE id = '33333333-0001-0004-0001-000000000001';

-- ID: 33333333-0001-0004-0003-000000000001
-- Text: "Я способен увидеть, как изменения в одной области повлияют на другие"
-- Rule: LE-002 (Generic causal thinking without specific complexity)
-- Reason: Basic systems awareness, no multi-stakeholder or uncertainty context
UPDATE assessment_questions
SET difficulty_level = 'INTERMEDIATE',
    metadata = jsonb_set(metadata, '{complexity_score}', '0.5')
WHERE id = '33333333-0001-0004-0003-000000000001';

-- ID: 33333333-0001-0005-0003-000000000001
-- Text: "Я использую тестирование и эксперименты для подтверждения своих идей"
-- Rule: LE-005 (Single behavior without organizational impact)
-- Reason: Basic scientific method, no strategic context
UPDATE assessment_questions
SET difficulty_level = 'INTERMEDIATE',
    metadata = jsonb_set(metadata, '{complexity_score}', '0.5')
WHERE id = '33333333-0001-0005-0003-000000000001';

-- COMPETENCY 2: Problem Solving (Решение проблем)

-- ID: 33333333-0002-0001-0003-000000000001
-- Text: "Мне удаётся докопаться до сути проблемы даже в сложных ситуациях"
-- KEEP ADVANCED: Has explicit complexity marker "в сложных ситуациях"

-- ID: 33333333-0002-0003-0002-000000000001
-- Text: "Я продумываю последствия своих действий на несколько шагов вперёд"
-- Rule: LE-005 (Basic planning behavior)
-- Reason: Simple forward-thinking without organizational/strategic scope
UPDATE assessment_questions
SET difficulty_level = 'INTERMEDIATE',
    metadata = jsonb_set(metadata, '{complexity_score}', '0.5')
WHERE id = '33333333-0002-0003-0002-000000000001';

-- ID: 33333333-0002-0005-0003-000000000001
-- Text: "Я продолжаю искать решение даже когда другие опускают руки"
-- Rule: LE-001 (Simple persistence statement)
-- Reason: Describes basic persistence, not advanced problem-solving
UPDATE assessment_questions
SET difficulty_level = 'INTERMEDIATE',
    metadata = jsonb_set(metadata, '{complexity_score}', '0.5')
WHERE id = '33333333-0002-0005-0003-000000000001';

-- COMPETENCY 3: Decision Making (Принятие решений)

-- ID: 33333333-0003-0002-0003-000000000001
-- Text: "Я учитываю не только очевидные, но и скрытые факторы при оценке"
-- KEEP ADVANCED: Involves hidden/non-obvious factors - cognitive complexity

-- ID: 33333333-0003-0003-0003-000000000001
-- Text: "Мне удаётся найти баланс между скоростью и качеством решений"
-- KEEP ADVANCED: Trade-off management is advanced skill

-- ID: 33333333-0003-0004-0001-000000000001
-- Text: "Я способен принимать решения даже при неполной информации"
-- KEEP ADVANCED: Uncertainty handling is advanced cognitive skill

-- ID: 33333333-0003-0004-0002-000000000001
-- Text: "Неопределённость не парализует меня, а мотивирует искать решения"
-- KEEP ADVANCED: Dealing with uncertainty is advanced

-- COMPETENCY 4: Relationship Building (Построение отношений)

-- ID: 33333333-0004-0004-0003-000000000001
-- Text: "Коллеги доверяют мне конфиденциальную информацию"
-- Rule: LE-001 (Passive statement about others' behavior)
-- Reason: Describes outcome, not active behavior requiring skill
UPDATE assessment_questions
SET difficulty_level = 'INTERMEDIATE',
    metadata = jsonb_set(metadata, '{complexity_score}', '0.5')
WHERE id = '33333333-0004-0004-0003-000000000001';

-- ID: 33333333-0004-0005-0003-000000000001
-- Text: "Моя профессиональная сеть контактов помогает мне в работе"
-- Rule: LE-001 (Passive statement about network utility)
-- Reason: Result statement, not active skill demonstration
UPDATE assessment_questions
SET difficulty_level = 'INTERMEDIATE',
    metadata = jsonb_set(metadata, '{complexity_score}', '0.5')
WHERE id = '33333333-0004-0005-0003-000000000001';

-- COMPETENCY 5: Conflict Resolution (Разрешение конфликтов)

-- ID: 33333333-0005-0001-0002-000000000001
-- Text: "Я чувствую, когда назревает конфликт, даже если он пока не высказан"
-- KEEP ADVANCED: Sensing hidden signals is advanced emotional intelligence

-- ID: 33333333-0005-0002-0002-000000000001
-- Text: "Я могу объяснить позицию другой стороны её же словами"
-- KEEP ADVANCED: Advanced perspective-taking and empathy skill

-- ID: 33333333-0005-0003-0002-000000000001
-- Text: "Мне удаётся находить компромиссы, которые удовлетворяют всех"
-- Rule: LE-002 (Generic compromise statement)
-- Reason: Simple compromise finding, no multi-party complexity described
UPDATE assessment_questions
SET difficulty_level = 'INTERMEDIATE',
    metadata = jsonb_set(metadata, '{complexity_score}', '0.5')
WHERE id = '33333333-0005-0003-0002-000000000001';

-- ID: 33333333-0005-0004-0002-000000000001
-- Text: "Я не позволяю эмоциям влиять на моё поведение в конфликте"
-- Rule: LE-005 (Basic emotional self-regulation)
-- Reason: Standard emotional control, not advanced conflict navigation
UPDATE assessment_questions
SET difficulty_level = 'INTERMEDIATE',
    metadata = jsonb_set(metadata, '{complexity_score}', '0.5')
WHERE id = '33333333-0005-0004-0002-000000000001';

-- ID: 33333333-0005-0004-0003-000000000001
-- Text: "Моё спокойствие помогает другим тоже успокоиться"
-- Rule: LE-001 (Passive influence statement)
-- Reason: Describes effect, not active de-escalation skill
UPDATE assessment_questions
SET difficulty_level = 'INTERMEDIATE',
    metadata = jsonb_set(metadata, '{complexity_score}', '0.5')
WHERE id = '33333333-0005-0004-0003-000000000001';

-- ID: 33333333-0005-0005-0001-000000000001
-- Text: "Я вмешиваюсь в конфликты на ранней стадии, чтобы предотвратить их обострение"
-- Rule: LE-004 (Simple proactive behavior)
-- Reason: Basic early intervention, no complex negotiation context
UPDATE assessment_questions
SET difficulty_level = 'INTERMEDIATE',
    metadata = jsonb_set(metadata, '{complexity_score}', '0.5')
WHERE id = '33333333-0005-0005-0001-000000000001';

-- ID: 33333333-0005-0005-0002-000000000001
-- Text: "Я умею снизить накал страстей в напряжённой ситуации"
-- Rule: LE-005 (Basic de-escalation skill)
-- Reason: Standard de-escalation, no multi-party complexity
UPDATE assessment_questions
SET difficulty_level = 'INTERMEDIATE',
    metadata = jsonb_set(metadata, '{complexity_score}', '0.5')
WHERE id = '33333333-0005-0005-0002-000000000001';

-- ID: 33333333-0005-0005-0003-000000000001
-- Text: "Моё участие помогает предотвратить переход конфликтов в открытое противостояние"
-- Rule: LE-001 (Passive outcome statement)
-- Reason: Describes result, not advanced mediation skill
UPDATE assessment_questions
SET difficulty_level = 'INTERMEDIATE',
    metadata = jsonb_set(metadata, '{complexity_score}', '0.5')
WHERE id = '33333333-0005-0005-0003-000000000001';

-- COMPETENCY 6: Influence (Влияние и убеждение)

-- ID: 33333333-0006-0002-0002-000000000001
-- Text: "Перед важным разговором я анализирую, что важно для собеседника"
-- Rule: LE-004 (Basic preparation behavior)
-- Reason: Standard communication preparation, not advanced influence
UPDATE assessment_questions
SET difficulty_level = 'INTERMEDIATE',
    metadata = jsonb_set(metadata, '{complexity_score}', '0.5')
WHERE id = '33333333-0006-0002-0002-000000000001';

-- ID: 33333333-0006-0003-0003-000000000001
-- Text: "Я использую визуализацию данных для усиления своих аргументов"
-- Rule: LE-005 (Simple tool usage)
-- Reason: Basic presentation skill, not advanced persuasion strategy
UPDATE assessment_questions
SET difficulty_level = 'INTERMEDIATE',
    metadata = jsonb_set(metadata, '{complexity_score}', '0.5')
WHERE id = '33333333-0006-0003-0003-000000000001';

-- ID: 33333333-0006-0004-0001-000000000001
-- Text: "Перед продвижением идеи я заручаюсь поддержкой ключевых людей"
-- KEEP ADVANCED: Coalition building is advanced organizational skill

-- ID: 33333333-0006-0004-0002-000000000001
-- Text: "Я знаю, чья поддержка важна для принятия решений в компании"
-- Rule: LE-002 (Basic organizational awareness)
-- Reason: Static knowledge, not active influence skill
UPDATE assessment_questions
SET difficulty_level = 'INTERMEDIATE',
    metadata = jsonb_set(metadata, '{complexity_score}', '0.5')
WHERE id = '33333333-0006-0004-0002-000000000001';

-- ================================================================
-- PHASE 2: V6 QUESTIONS - Competencies 7-27 (36 ADVANCED → 20 INTERMEDIATE)
-- ================================================================

-- COMPETENCY 8: Strategic Vision (Стратегическое видение)

-- ID: 33333333-0008-0001-0002-000000000001
-- Text: "Я замечаю изменения на рынке раньше, чем они становятся очевидными"
-- KEEP ADVANCED: Early signal detection is advanced strategic skill

-- ID: 33333333-0008-0002-0002-000000000001
-- Text: "Моё видение будущего вызывает эмоциональный отклик у людей"
-- Rule: LE-001 (Passive outcome statement)
-- Reason: Describes effect, not active vision communication skill
UPDATE assessment_questions
SET difficulty_level = 'INTERMEDIATE',
    metadata = jsonb_set(metadata, '{complexity_score}', '0.5')
WHERE id = '33333333-0008-0002-0002-000000000001';

-- ID: 33333333-0008-0004-0002-000000000001
-- Text: "Я гибко меняю тактику, сохраняя верность видению"
-- KEEP ADVANCED: Strategic flexibility with vision alignment is advanced

-- ID: 33333333-0008-0005-0001-000000000001
-- Text: "Я адаптирую сообщение о стратегии для разных аудиторий"
-- KEEP ADVANCED: Stakeholder-specific communication is advanced

-- COMPETENCY 10: Change Management (Управление изменениями)

-- ID: 33333333-0010-0005-0002-000000000001
-- Text: "Я беру на себя лидерство в трансформационных инициативах"
-- KEEP ADVANCED: Leading transformation is advanced leadership

-- COMPETENCY 11: Learning Agility (Обучаемость)

-- ID: 33333333-0011-0003-0003-000000000001
-- Text: "Мой кросс-функциональный опыт помогает находить нестандартные решения"
-- Rule: LE-001 (Passive result statement)
-- Reason: Describes outcome of experience, not active learning skill
UPDATE assessment_questions
SET difficulty_level = 'INTERMEDIATE',
    metadata = jsonb_set(metadata, '{complexity_score}', '0.5')
WHERE id = '33333333-0011-0003-0003-000000000001';

-- ID: 33333333-0011-0005-0003-000000000001
-- Text: "Я провожу обучение для коллег по новым темам"
-- Rule: LE-005 (Basic teaching behavior)
-- Reason: Standard knowledge sharing, not advanced learning leadership
UPDATE assessment_questions
SET difficulty_level = 'INTERMEDIATE',
    metadata = jsonb_set(metadata, '{complexity_score}', '0.5')
WHERE id = '33333333-0011-0005-0003-000000000001';

-- COMPETENCY 12: Resilience (Устойчивость)

-- ID: 33333333-0012-0003-0003-000000000001
-- Text: "Мой оптимизм вдохновляет окружающих"
-- Rule: LE-001 (Passive influence statement)
-- Reason: Describes effect on others, not active resilience skill
UPDATE assessment_questions
SET difficulty_level = 'INTERMEDIATE',
    metadata = jsonb_set(metadata, '{complexity_score}', '0.5')
WHERE id = '33333333-0012-0003-0003-000000000001';

-- ID: 33333333-0012-0005-0003-000000000001
-- Text: "Я использую кризисы как возможность для трансформации"
-- KEEP ADVANCED: Crisis-to-opportunity transformation is advanced

-- COMPETENCY 13: Self-Awareness (Самосознание)

-- ID: 33333333-0013-0005-0003-000000000001
-- Text: "Я инициирую обратную связь 360 градусов"
-- Rule: LE-005 (Simple feedback-seeking behavior)
-- Reason: Procedural action, not deep self-awareness skill
UPDATE assessment_questions
SET difficulty_level = 'INTERMEDIATE',
    metadata = jsonb_set(metadata, '{complexity_score}', '0.5')
WHERE id = '33333333-0013-0005-0003-000000000001';

-- COMPETENCY 14: Empathy (Эмпатия)

-- ID: 33333333-0014-0005-0001-000000000001
-- Text: "Я чувствую, что нужно человеку, даже если он не просит"
-- KEEP ADVANCED: Sensing unstated needs is advanced empathy

-- COMPETENCY 15: Emotional Regulation (Эмоциональная регуляция)

-- ID: 33333333-0015-0005-0002-000000000001
-- Text: "Моё присутствие создаёт спокойную атмосферу"
-- Rule: LE-001 (Passive effect statement)
-- Reason: Describes result of presence, not active regulation skill
UPDATE assessment_questions
SET difficulty_level = 'INTERMEDIATE',
    metadata = jsonb_set(metadata, '{complexity_score}', '0.5')
WHERE id = '33333333-0015-0005-0002-000000000001';

-- COMPETENCY 17: Negotiation (Переговоры)

-- ID: 33333333-0017-0005-0002-000000000001
-- Text: "Мои предложения обычно принимаются"
-- Rule: LE-001 (Passive outcome statement)
-- Reason: Describes result, not negotiation skill itself
UPDATE assessment_questions
SET difficulty_level = 'INTERMEDIATE',
    metadata = jsonb_set(metadata, '{complexity_score}', '0.5')
WHERE id = '33333333-0017-0005-0002-000000000001';

-- COMPETENCY 20: Collaboration (Сотрудничество)

-- ID: 33333333-0020-0004-0001-000000000001
-- Text: "Я нахожу способы сотрудничать, несмотря на бюрократические препятствия"
-- KEEP ADVANCED: Navigating organizational barriers is advanced

-- ID: 33333333-0020-0005-0001-000000000001
-- Text: "Я ищу возможности для совместных проектов между отделами"
-- Rule: LE-004 (Basic collaboration seeking)
-- Reason: Standard cross-functional awareness, not advanced collaboration
UPDATE assessment_questions
SET difficulty_level = 'INTERMEDIATE',
    metadata = jsonb_set(metadata, '{complexity_score}', '0.5')
WHERE id = '33333333-0020-0005-0001-000000000001';

-- ID: 33333333-0020-0005-0003-000000000001
-- Text: "Я способствую тому, чтобы совместная работа давала больше, чем отдельные усилия"
-- KEEP ADVANCED: Synergy creation is advanced collaboration

-- COMPETENCY 21: Knowledge Sharing (Обмен знаниями)

-- ID: 33333333-0021-0004-0002-000000000001
-- Text: "Я предлагаю форматы для коллективного обучения"
-- Rule: LE-005 (Basic process suggestion)
-- Reason: Procedural contribution, not advanced knowledge leadership
UPDATE assessment_questions
SET difficulty_level = 'INTERMEDIATE',
    metadata = jsonb_set(metadata, '{complexity_score}', '0.5')
WHERE id = '33333333-0021-0004-0002-000000000001';

-- ID: 33333333-0021-0004-0003-000000000001
-- Text: "Я организую внутренние презентации и митапы"
-- Rule: LE-005 (Basic organizational behavior)
-- Reason: Standard knowledge sharing activity
UPDATE assessment_questions
SET difficulty_level = 'INTERMEDIATE',
    metadata = jsonb_set(metadata, '{complexity_score}', '0.5')
WHERE id = '33333333-0021-0004-0003-000000000001';

-- COMPETENCY 22: Critical Thinking (Критическое мышление)

-- ID: 33333333-0022-0002-0002-000000000001
-- Text: "Я вижу, когда выводы не следуют из посылок"
-- Rule: LE-002 (Basic logic skill)
-- Reason: Standard logical analysis, not advanced critical thinking
UPDATE assessment_questions
SET difficulty_level = 'INTERMEDIATE',
    metadata = jsonb_set(metadata, '{complexity_score}', '0.5')
WHERE id = '33333333-0022-0002-0002-000000000001';

-- ID: 33333333-0022-0002-0003-000000000001
-- Text: "Я замечаю подмену тезиса в дискуссиях"
-- KEEP ADVANCED: Detecting logical fallacies in real-time is advanced

-- ID: 33333333-0022-0004-0001-000000000001
-- Text: "Я не путаю связь между явлениями с причинно-следственными отношениями"
-- Rule: LE-002 (Basic correlation/causation awareness)
-- Reason: Standard analytical skill, taught at undergraduate level
UPDATE assessment_questions
SET difficulty_level = 'INTERMEDIATE',
    metadata = jsonb_set(metadata, '{complexity_score}', '0.5')
WHERE id = '33333333-0022-0004-0001-000000000001';

-- ID: 33333333-0022-0004-0002-000000000001
-- Text: "Я ищу альтернативные объяснения связей между событиями"
-- KEEP ADVANCED: Generating alternative hypotheses is advanced

-- ID: 33333333-0022-0005-0001-000000000001
-- Text: "Я формулирую проверяемые гипотезы перед исследованием"
-- Rule: LE-005 (Basic scientific method)
-- Reason: Standard research methodology, not advanced critical thinking
UPDATE assessment_questions
SET difficulty_level = 'INTERMEDIATE',
    metadata = jsonb_set(metadata, '{complexity_score}', '0.5')
WHERE id = '33333333-0022-0005-0001-000000000001';

-- COMPETENCY 23: Information Literacy (Информационная грамотность)

-- ID: 33333333-0023-0004-0003-000000000001
-- Text: "Я адаптирую информацию к текущему контексту"
-- Rule: LE-004 (Basic information processing)
-- Reason: Standard contextual adaptation
UPDATE assessment_questions
SET difficulty_level = 'INTERMEDIATE',
    metadata = jsonb_set(metadata, '{complexity_score}', '0.5')
WHERE id = '33333333-0023-0004-0003-000000000001';

-- ID: 33333333-0023-0005-0003-000000000001
-- Text: "Я учитываю возможные конфликты интересов у авторов"
-- KEEP ADVANCED: Source bias evaluation is advanced literacy

-- COMPETENCY 24: Systems Thinking (Системное мышление)

-- ID: 33333333-0024-0001-0002-000000000001
-- Text: "Я строю карты взаимосвязей для понимания проблем"
-- Rule: LE-005 (Tool usage)
-- Reason: Standard visualization technique, not advanced systems thinking
UPDATE assessment_questions
SET difficulty_level = 'INTERMEDIATE',
    metadata = jsonb_set(metadata, '{complexity_score}', '0.5')
WHERE id = '33333333-0024-0001-0002-000000000001';

-- ID: 33333333-0024-0002-0002-000000000001
-- Text: "Я учитываю системные последствия при планировании изменений"
-- KEEP ADVANCED: Systemic impact consideration is advanced

-- ID: 33333333-0024-0003-0001-000000000001
-- Text: "Я замечаю самоусиливающиеся процессы"
-- KEEP ADVANCED: Identifying feedback loops is advanced

-- ID: 33333333-0024-0003-0002-000000000001
-- Text: "Я идентифицирую порочные круги в процессах"
-- KEEP ADVANCED: Identifying vicious cycles is advanced

-- ID: 33333333-0024-0003-0003-000000000001
-- Text: "Я понимаю, как обратная связь влияет на поведение системы"
-- KEEP ADVANCED: Understanding feedback dynamics is advanced

-- ID: 33333333-0024-0004-0001-000000000001
-- Text: "Я использую диаграммы для визуализации сложных систем"
-- Rule: LE-005 (Tool usage)
-- Reason: Standard visualization technique
UPDATE assessment_questions
SET difficulty_level = 'INTERMEDIATE',
    metadata = jsonb_set(metadata, '{complexity_score}', '0.5')
WHERE id = '33333333-0024-0004-0001-000000000001';

-- ID: 33333333-0024-0004-0002-000000000001
-- Text: "Я строю модели для понимания поведения систем"
-- Rule: LE-005 (Tool usage)
-- Reason: Model building is a technique, complexity depends on model type
UPDATE assessment_questions
SET difficulty_level = 'INTERMEDIATE',
    metadata = jsonb_set(metadata, '{complexity_score}', '0.5')
WHERE id = '33333333-0024-0004-0002-000000000001';

-- ID: 33333333-0024-0004-0003-000000000001
-- Text: "Я симулирую последствия решений перед внедрением"
-- KEEP ADVANCED: Mental/actual simulation is advanced planning

-- ID: 33333333-0024-0005-0001-000000000001
-- Text: "Я ищу ключевые точки влияния в системах"
-- KEEP ADVANCED: Leverage point identification is advanced systems thinking

-- ID: 33333333-0024-0005-0002-000000000001
-- Text: "Я фокусирую усилия там, где малые изменения дают большой эффект"
-- KEEP ADVANCED: Leverage application is advanced

-- ID: 33333333-0024-0005-0003-000000000001
-- Text: "Я приоритизирую воздействия по их системному эффекту"
-- KEEP ADVANCED: Systems-based prioritization is advanced

-- ================================================================
-- PHASE 3: UPDATE METADATA TAGS TO REFLECT DIFFICULTY CHANGES
-- ================================================================

-- Update tags from SENIOR to MID for downgraded questions
UPDATE assessment_questions
SET metadata = jsonb_set(metadata, '{tags}', '["GENERAL", "MID"]'::jsonb)
WHERE id IN (
    '33333333-0001-0004-0001-000000000001',
    '33333333-0001-0004-0003-000000000001',
    '33333333-0001-0005-0003-000000000001',
    '33333333-0002-0003-0002-000000000001',
    '33333333-0002-0005-0003-000000000001',
    '33333333-0004-0004-0003-000000000001',
    '33333333-0004-0005-0003-000000000001',
    '33333333-0005-0003-0002-000000000001',
    '33333333-0005-0004-0002-000000000001',
    '33333333-0005-0004-0003-000000000001',
    '33333333-0005-0005-0001-000000000001',
    '33333333-0005-0005-0002-000000000001',
    '33333333-0005-0005-0003-000000000001',
    '33333333-0006-0002-0002-000000000001',
    '33333333-0006-0003-0003-000000000001',
    '33333333-0006-0004-0002-000000000001'
)
AND metadata->>'tags' LIKE '%SENIOR%';

-- ================================================================
-- SUMMARY
-- ================================================================
-- Total ADVANCED LIKERT questions analyzed: 60
-- Questions downgraded to INTERMEDIATE: 29
-- Questions kept at ADVANCED: 31
--
-- Downgrade criteria applied:
-- - LE-001: Simple agreement/passive statements (9 questions)
-- - LE-002: Generic statements without situational complexity (5 questions)
-- - LE-004: No conditional/situational markers (4 questions)
-- - LE-005: Single behavior without organizational context (11 questions)
--
-- Questions retained as ADVANCED have:
-- - Explicit complexity markers (e.g., "в сложных ситуациях")
-- - Hidden/non-obvious factor consideration
-- - Trade-off management requirements
-- - Uncertainty handling
-- - Multi-stakeholder context
-- - Systems-level thinking
-- - Strategic/organizational impact
-- ================================================================

-- Verification query (uncomment to check results):
-- SELECT
--     difficulty_level,
--     COUNT(*) as count,
--     ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER(), 1) as percentage
-- FROM assessment_questions
-- WHERE question_type = 'LIKERT'
-- GROUP BY difficulty_level
-- ORDER BY CASE difficulty_level
--     WHEN 'FOUNDATIONAL' THEN 1
--     WHEN 'INTERMEDIATE' THEN 2
--     WHEN 'ADVANCED' THEN 3
--     WHEN 'EXPERT' THEN 4
--     WHEN 'SPECIALIZED' THEN 5
-- END;
