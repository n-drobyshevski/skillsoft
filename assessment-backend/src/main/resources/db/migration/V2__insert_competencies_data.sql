-- Flyway Migration: Insert Initial Competencies Data
-- Version: V2__insert_competencies_data.sql
-- Description: Populate competencies table with core soft skills
-- Author: System
-- Date: 2025-10-18

-- Ensure uuid generation function is available in Postgres
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ================================================================
-- CORE SOFT SKILLS COMPETENCIES - FLYWAY MIGRATION
-- ================================================================

INSERT INTO competencies (
    id, name, description, category, level, is_active, 
    approval_status, version, created_at, last_modified
) VALUES
-- LEADERSHIP Competencies
(gen_random_uuid(), 'Стратегическое Лидерство', 'Способность определять долгосрочные цели, вдохновлять команду на их достижение и принимать стратегические решения в условиях неопределенности', 'LEADERSHIP', 'ADVANCED', true, 'APPROVED', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(gen_random_uuid(), 'Развитие Команды', 'Умение выявлять потенциал сотрудников, предоставлять конструктивную обратную связь и создавать возможности для профессионального роста', 'LEADERSHIP', 'PROFICIENT', true, 'APPROVED', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- COMMUNICATION Competencies
(gen_random_uuid(), 'Эффективная Коммуникация', 'Способность ясно и убедительно передавать информацию, адаптируя стиль общения под различные аудитории и культурные контексты', 'COMMUNICATION', 'PROFICIENT', true, 'APPROVED', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(gen_random_uuid(), 'Активное Слушание', 'Умение внимательно слушать собеседника, задавать уточняющие вопросы и демонстрировать понимание через парафразирование и обратную связь', 'COMMUNICATION', 'DEVELOPING', true, 'APPROVED', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- EMOTIONAL_INTELLIGENCE Competencies
(gen_random_uuid(), 'Эмпатия и Социальная Осознанность', 'Способность понимать эмоции других людей, проявлять сочувствие и учитывать различные точки зрения при принятии решений', 'EMOTIONAL_INTELLIGENCE', 'PROFICIENT', true, 'APPROVED', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(gen_random_uuid(), 'Управление Стрессом', 'Умение сохранять спокойствие и эффективность в стрессовых ситуациях, управлять собственными эмоциями и помогать другим справляться с напряжением', 'EMOTIONAL_INTELLIGENCE', 'DEVELOPING', true, 'APPROVED', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- CRITICAL_THINKING Competencies
(gen_random_uuid(), 'Аналитическое Решение Проблем', 'Способность систематически анализировать сложные проблемы, выявлять первопричины и разрабатывать инновационные решения на основе данных', 'CRITICAL_THINKING', 'ADVANCED', true, 'APPROVED', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- COLLABORATION Competencies
(gen_random_uuid(), 'Командная Работа', 'Умение эффективно работать в многофункциональных командах, координировать усилия, делиться знаниями и достигать общих целей', 'COLLABORATION', 'PROFICIENT', true, 'APPROVED', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

(gen_random_uuid(), 'Разрешение Конфликтов', 'Способность медиировать споры, находить компромиссы и трансформировать конфликтные ситуации в возможности для улучшения взаимоотношений', 'COLLABORATION', 'DEVELOPING', true, 'APPROVED', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- ADAPTABILITY Competencies
(gen_random_uuid(), 'Обучаемость и Гибкость', 'Готовность к постоянному обучению, быстрая адаптация к изменениям в технологиях, процессах и организационной структуре', 'ADAPTABILITY', 'DEVELOPING', true, 'APPROVED', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
