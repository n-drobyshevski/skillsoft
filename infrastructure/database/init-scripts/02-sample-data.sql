-- ================================================================
-- SKILLSOFT ASSESSMENT PLATFORM - SAMPLE DATA
-- ================================================================
-- Author: SkillSoft Development Team
-- Date: March 20, 2024
-- Purpose: Populate database with sample data for development and testing

-- ----------------------------------------------------------------
-- 1. SAMPLE COMPETENCIES
-- ----------------------------------------------------------------

-- Leadership competency
INSERT INTO competencies (
    name, 
    description, 
    category, 
    level, 
    is_active, 
    approval_status, 
    version
) VALUES (
    'Стратегическое лидерство',
    'Способность определять долгосрочные цели, вдохновлять команду на их достижение и принимать стратегические решения в условиях неопределенности',
    'LEADERSHIP',
    'ADVANCED',
    true,
    'APPROVED',
    1
);

-- Communication competency
INSERT INTO competencies (
    name, 
    description, 
    category, 
    level, 
    is_active, 
    approval_status, 
    version
) VALUES (
    'Эффективная коммуникация',
    'Навыки четкого изложения идей, активного слушания и адаптации стиля общения к различным аудиториям. Включает как устную, так и письменную коммуникацию',
    'COMMUNICATION',
    'PROFICIENT',
    true,
    'APPROVED',
    1
);

-- Emotional intelligence competency
INSERT INTO competencies (
    name, 
    description, 
    category, 
    level, 
    is_active, 
    approval_status, 
    version
) VALUES (
    'Эмпатия и социальная осознанность',
    'Способность понимать эмоции других людей, учитывать разные точки зрения и проявлять искреннюю заинтересованность в потребностях коллег и клиентов',
    'EMOTIONAL_INTELLIGENCE',
    'DEVELOPING',
    true,
    'APPROVED',
    1
);

-- Critical thinking competency
INSERT INTO competencies (
    name, 
    description, 
    category, 
    level, 
    is_active, 
    approval_status, 
    version
) VALUES (
    'Критическое мышление',
    'Способность объективно анализировать информацию, оценивать аргументы, распознавать шаблоны и делать обоснованные выводы для принятия решений',
    'COGNITIVE',
    'PROFICIENT',
    true,
    'APPROVED',
    1
);

-- Time management competency
INSERT INTO competencies (
    name, 
    description, 
    category, 
    level, 
    is_active, 
    approval_status, 
    version
) VALUES (
    'Управление временем',
    'Способность эффективно планировать, приоритизировать задачи и распределять время для достижения целей в установленные сроки',
    'TIME_MANAGEMENT',
    'DEVELOPING',
    true,
    'PENDING_REVIEW',
    1
);

-- Problem solving competency
INSERT INTO competencies (
    name, 
    description, 
    category, 
    level, 
    is_active, 
    approval_status, 
    version
) VALUES (
    'Решение проблем',
    'Способность идентифицировать, анализировать и решать сложные проблемы, разрабатывая эффективные решения в условиях неопределенности и изменений',
    'COGNITIVE',
    'PROFICIENT',
    true,
    'APPROVED',
    1
);

-- ----------------------------------------------------------------
-- 2. SAMPLE BEHAVIORAL INDICATORS FOR LEADERSHIP
-- ----------------------------------------------------------------

-- Get the ID of the Leadership competency
DO $$
DECLARE
    leadership_id UUID;
    communication_id UUID;
    emotional_id UUID;
    critical_id UUID;
    time_management_id UUID;
    problem_solving_id UUID;
BEGIN
    -- Get competency IDs
    SELECT id INTO leadership_id FROM competencies WHERE name = 'Стратегическое лидерство';
    SELECT id INTO communication_id FROM competencies WHERE name = 'Эффективная коммуникация';
    SELECT id INTO emotional_id FROM competencies WHERE name = 'Эмпатия и социальная осознанность';
    SELECT id INTO critical_id FROM competencies WHERE name = 'Критическое мышление';
    SELECT id INTO time_management_id FROM competencies WHERE name = 'Управление временем';
    SELECT id INTO problem_solving_id FROM competencies WHERE name = 'Решение проблем';
    
    -- LEADERSHIP INDICATORS
    
    -- Leadership Indicator 1
    INSERT INTO behavioral_indicators (
        competency_id,
        title,
        description,
        observability_level,
        measurement_type,
        weight,
        examples,
        counter_examples,
        is_active,
        approval_status,
        order_index
    ) VALUES (
        leadership_id,
        'Формулирует долгосрочное видение',
        'Создает четкое понимание направления развития команды или организации на перспективу 3-5 лет',
        'ADVANCED',
        'QUALITY',
        0.40,
        'Проводит стратегические сессии с командой
Создает дорожную карту продукта
Коммуникирует видение на командных встречах
Увязывает ежедневные задачи с долгосрочными целями',
        'Фокусируется только на текущих задачах
Избегает долгосрочного планирования
Не объясняет общие цели команде',
        true,
        'APPROVED',
        1
    );

    -- Leadership Indicator 2
    INSERT INTO behavioral_indicators (
        competency_id,
        title,
        description,
        observability_level,
        measurement_type,
        weight,
        examples,
        counter_examples,
        is_active,
        approval_status,
        order_index
    ) VALUES (
        leadership_id,
        'Принимает решения в условиях неопределенности',
        'Способен анализировать неполную информацию и принимать обоснованные решения в сложных ситуациях',
        'EXPERT',
        'QUALITY',
        0.30,
        'Анализирует риски и возможности
Консультируется с экспертами перед принятием решений
Оперативно реагирует на изменения рынка
Принимает на себя ответственность за результаты',
        'Откладывает принятие решений
Действует импульсивно без анализа
Избегает ответственности за решения',
        true,
        'APPROVED',
        2
    );

    -- Leadership Indicator 3
    INSERT INTO behavioral_indicators (
        competency_id,
        title,
        description,
        observability_level,
        measurement_type,
        weight,
        examples,
        counter_examples,
        is_active,
        approval_status,
        order_index
    ) VALUES (
        leadership_id,
        'Вдохновляет и мотивирует команду',
        'Умеет мотивировать сотрудников для достижения амбициозных целей и поддерживать высокий моральный дух',
        'ADVANCED',
        'IMPACT',
        0.30,
        'Проводит мотивационные встречи
Публично признает достижения команды
Поддерживает сотрудников в сложные периоды
Создает вдохновляющую рабочую атмосферу',
        'Критикует без конструктивных предложений
Игнорирует достижения команды
Демотивирует негативными комментариями',
        true,
        'APPROVED',
        3
    );
    
    -- COMMUNICATION INDICATORS
    
    -- Communication Indicator 1
    INSERT INTO behavioral_indicators (
        competency_id,
        title,
        description,
        observability_level,
        measurement_type,
        weight,
        examples,
        counter_examples,
        is_active,
        approval_status,
        order_index
    ) VALUES (
        communication_id,
        'Адаптирует стиль коммуникации под аудиторию',
        'Меняет способ изложения информации в зависимости от знаний, интересов и культурных особенностей слушателей',
        'PROFICIENT',
        'QUALITY',
        0.35,
        'Использует технические термины при общении с разработчиками
Упрощает объяснения при взаимодействии с клиентами
Адаптирует презентации под уровень аудитории',
        'Использует один стиль общения для всех аудиторий
Игнорирует уровень знаний слушателей
Не учитывает обратную связь от аудитории',
        true,
        'APPROVED',
        1
    );

    -- Communication Indicator 2
    INSERT INTO behavioral_indicators (
        competency_id,
        title,
        description,
        observability_level,
        measurement_type,
        weight,
        examples,
        counter_examples,
        is_active,
        approval_status,
        order_index
    ) VALUES (
        communication_id,
        'Активно слушает собеседника',
        'Демонстрирует полное внимание к говорящему, задает уточняющие вопросы и подтверждает понимание',
        'PROFICIENT',
        'FREQUENCY',
        0.35,
        'Поддерживает зрительный контакт
Перефразирует услышанное для проверки понимания
Задает открытые вопросы для углубления обсуждения',
        'Перебивает собеседника
Отвлекается на телефон или компьютер во время разговора
Не реагирует на эмоциональный контекст',
        true,
        'APPROVED',
        2
    );

    -- Communication Indicator 3
    INSERT INTO behavioral_indicators (
        competency_id,
        title,
        description,
        observability_level,
        measurement_type,
        weight,
        examples,
        counter_examples,
        is_active,
        approval_status,
        order_index
    ) VALUES (
        communication_id,
        'Структурирует информацию логически',
        'Организует сообщения в логической последовательности с четкими ключевыми моментами и выводами',
        'ADVANCED',
        'QUALITY',
        0.30,
        'Начинает с ключевых выводов
Использует маркированные списки и заголовки
Группирует связанные идеи
Завершает четкими следующими шагами',
        'Представляет информацию хаотично
Перескакивает между несвязанными темами
Не выделяет ключевые моменты',
        true,
        'APPROVED',
        3
    );
    
    -- EMOTIONAL INTELLIGENCE INDICATORS
    
    -- Emotional Intelligence Indicator 1
    INSERT INTO behavioral_indicators (
        competency_id,
        title,
        description,
        observability_level,
        measurement_type,
        weight,
        examples,
        counter_examples,
        is_active,
        approval_status,
        order_index
    ) VALUES (
        emotional_id,
        'Распознает эмоциональное состояние других',
        'Точно определяет чувства и настроение других людей, основываясь на вербальных и невербальных сигналах',
        'DEVELOPING',
        'QUALITY',
        0.40,
        'Замечает изменения в тоне голоса и языке тела
Спрашивает о самочувствии при признаках стресса
Корректирует подход в зависимости от настроения коллег',
        'Игнорирует эмоциональные сигналы
Неправильно интерпретирует эмоциональные состояния
Фокусируется только на содержании, игнорируя эмоции',
        true,
        'APPROVED',
        1
    );

    -- Emotional Intelligence Indicator 2
    INSERT INTO behavioral_indicators (
        competency_id,
        title,
        description,
        observability_level,
        measurement_type,
        weight,
        examples,
        counter_examples,
        is_active,
        approval_status,
        order_index
    ) VALUES (
        emotional_id,
        'Проявляет эмпатию в сложных ситуациях',
        'Демонстрирует понимание и принятие чувств других людей в эмоционально напряженных обстоятельствах',
        'DEVELOPING',
        'FREQUENCY',
        0.35,
        'Выражает поддержку коллегам в стрессовых ситуациях
Признает легитимность разных точек зрения
Предлагает помощь без осуждения',
        'Проявляет безразличие к проблемам других
Обесценивает чувства коллег
Предлагает поверхностные решения без понимания',
        true,
        'APPROVED',
        2
    );

    -- Emotional Intelligence Indicator 3
    INSERT INTO behavioral_indicators (
        competency_id,
        title,
        description,
        observability_level,
        measurement_type,
        weight,
        examples,
        counter_examples,
        is_active,
        approval_status,
        order_index
    ) VALUES (
        emotional_id,
        'Управляет собственными эмоциями',
        'Осознает и адекватно регулирует свои эмоциональные реакции, особенно в стрессовых ситуациях',
        'NOVICE',
        'CONSISTENCY',
        0.25,
        'Сохраняет спокойствие под давлением
Берет паузу перед ответом в эмоциональных ситуациях
Признает свои эмоции и их влияние',
        'Проявляет вспыльчивость при несогласии
Подавляет эмоции до точки срыва
Отрицает влияние эмоций на решения',
        true,
        'APPROVED',
        3
    );

    -- CRITICAL THINKING INDICATORS
    
    -- Critical Thinking Indicator 1
    INSERT INTO behavioral_indicators (
        competency_id,
        title,
        description,
        observability_level,
        measurement_type,
        weight,
        examples,
        counter_examples,
        is_active,
        approval_status,
        order_index
    ) VALUES (
        critical_id,
        'Анализирует информацию из разных источников',
        'Систематически собирает и сопоставляет данные из различных источников для формирования объективной картины',
        'PROFICIENT',
        'QUALITY',
        0.35,
        'Использует несколько источников данных для принятия решений
Сравнивает противоречивую информацию
Определяет достоверность источников',
        'Полагается на один источник информации
Не проверяет факты перед выводами
Игнорирует противоречащие данные',
        true,
        'APPROVED',
        1
    );

    -- Critical Thinking Indicator 2
    INSERT INTO behavioral_indicators (
        competency_id,
        title,
        description,
        observability_level,
        measurement_type,
        weight,
        examples,
        counter_examples,
        is_active,
        approval_status,
        order_index
    ) VALUES (
        critical_id,
        'Выявляет когнитивные искажения',
        'Определяет и корректирует логические ошибки и предубеждения в собственном мышлении и аргументах других',
        'ADVANCED',
        'QUALITY',
        0.30,
        'Распознает подтверждающее предубеждение в аргументах
Проверяет собственные предположения
Рассматривает альтернативные объяснения',
        'Принимает аргументы без критической оценки
Не осознает собственных предубеждений
Игнорирует контр-аргументы',
        true,
        'APPROVED',
        2
    );

    -- Critical Thinking Indicator 3
    INSERT INTO behavioral_indicators (
        competency_id,
        title,
        description,
        observability_level,
        measurement_type,
        weight,
        examples,
        counter_examples,
        is_active,
        approval_status,
        order_index
    ) VALUES (
        critical_id,
        'Формулирует обоснованные выводы',
        'На основе анализа данных делает логические выводы, подкрепленные доказательствами',
        'PROFICIENT',
        'QUALITY',
        0.35,
        'Представляет выводы с подтверждающими фактами
Указывает ограничения своих выводов
Разделяет факты и мнения в аргументации',
        'Делает необоснованные заявления
Игнорирует доказательства против своей позиции
Смешивает факты и предположения',
        true,
        'APPROVED',
        3
    );
    
    -- TIME MANAGEMENT INDICATORS
    
    -- Time Management Indicator 1
    INSERT INTO behavioral_indicators (
        competency_id,
        title,
        description,
        observability_level,
        measurement_type,
        weight,
        examples,
        counter_examples,
        is_active,
        approval_status,
        order_index
    ) VALUES (
        time_management_id,
        'Эффективно приоритизирует задачи',
        'Правильно определяет порядок выполнения задач на основе их важности, срочности и стратегической ценности',
        'DEVELOPING',
        'QUALITY',
        0.40,
        'Использует матрицу Эйзенхауэра для сортировки задач
Фокусируется на задачах с высокой ценностью
Регулярно пересматривает приоритеты',
        'Работает по принципу "кто громче кричит"
Постоянно отвлекается на несущественные задачи
Не различает срочное и важное',
        true,
        'PENDING_REVIEW',
        1
    );

    -- Time Management Indicator 2
    INSERT INTO behavioral_indicators (
        competency_id,
        title,
        description,
        observability_level,
        measurement_type,
        weight,
        examples,
        counter_examples,
        is_active,
        approval_status,
        order_index
    ) VALUES (
        time_management_id,
        'Планирует время с учетом энергетических циклов',
        'Распределяет задачи в течение дня с учетом собственных пиков продуктивности и энергии',
        'PROFICIENT',
        'IMPACT',
        0.30,
        'Выполняет сложные задачи в периоды высокой энергии
Группирует похожие задачи для эффективности
Планирует перерывы для восстановления',
        'Работает хаотично без учета биоритмов
Не отслеживает периоды продуктивности
Игнорирует признаки усталости',
        true,
        'PENDING_REVIEW',
        2
    );

    -- Time Management Indicator 3
    INSERT INTO behavioral_indicators (
        competency_id,
        title,
        description,
        observability_level,
        measurement_type,
        weight,
        examples,
        counter_examples,
        is_active,
        approval_status,
        order_index
    ) VALUES (
        time_management_id,
        'Устанавливает четкие границы и защищает время',
        'Умеет отказываться от лишних задач и защищать время для важной работы',
        'EXPERT',
        'FREQUENCY',
        0.30,
        'Блокирует время для сфокусированной работы
Вежливо отказывается от несущественных встреч
Коммуницирует свою доступность',
        'Соглашается на все запросы без оценки
Постоянно прерывается и отвлекается
Не создает условия для глубокой работы',
        true,
        'PENDING_REVIEW',
        3
    );
    
    -- PROBLEM SOLVING INDICATORS
    
    -- Problem Solving Indicator 1
    INSERT INTO behavioral_indicators (
        competency_id,
        title,
        description,
        observability_level,
        measurement_type,
        weight,
        examples,
        counter_examples,
        is_active,
        approval_status,
        order_index
    ) VALUES (
        problem_solving_id,
        'Анализирует проблему системно',
        'Эффективно идентифицирует корневые причины проблем, используя структурированный подход к сбору и анализу данных',
        'PROFICIENT',
        'QUALITY',
        0.35,
        'Применяет методики анализа корневых причин (5 почему, диаграмма Ишикавы)
Собирает и систематизирует разнородные данные перед формулировкой выводов
Документирует процесс анализа для повышения прозрачности решений',
        'Останавливается на поверхностных симптомах проблемы
Принимает решения без достаточного анализа данных
Не учитывает взаимосвязи между различными аспектами проблемы',
        true,
        'APPROVED',
        1
    );

    -- Problem Solving Indicator 2
    INSERT INTO behavioral_indicators (
        competency_id,
        title,
        description,
        observability_level,
        measurement_type,
        weight,
        examples,
        counter_examples,
        is_active,
        approval_status,
        order_index
    ) VALUES (
        problem_solving_id,
        'Генерирует разнообразные решения',
        'Разрабатывает несколько альтернативных подходов к решению проблемы, демонстрируя гибкость мышления',
        'ADVANCED',
        'IMPACT',
        0.35,
        'Организует сессии брейнсторминга для поиска нестандартных решений
Комбинирует идеи из разных областей знаний
Рассматривает как минимум 3-5 альтернативных вариантов',
        'Сразу фиксируется на первом пришедшем в голову решении
Отвергает необычные идеи без анализа
Применяет одни и те же шаблонные подходы ко всем проблемам',
        true,
        'APPROVED',
        2
    );

    -- Problem Solving Indicator 3
    INSERT INTO behavioral_indicators (
        competency_id,
        title,
        description,
        observability_level,
        measurement_type,
        weight,
        examples,
        counter_examples,
        is_active,
        approval_status,
        order_index
    ) VALUES (
        problem_solving_id,
        'Оценивает и внедряет решения',
        'Систематически оценивает потенциальные решения на основе четких критериев и эффективно реализует выбранный план действий',
        'EXPERT',
        'CONSISTENCY',
        0.30,
        'Использует матрицу критериев для объективной оценки вариантов
Разрабатывает детальный план внедрения решения с контрольными точками
Отслеживает результаты внедрения и корректирует подход при необходимости',
        'Принимает решения на основе субъективных предпочтений
Не продумывает детали реализации решения
Не проводит оценку эффективности принятого решения',
        true,
        'PENDING_REVIEW',
        3
    );
    
    -- ----------------------------------------------------------------
    -- 3. SAMPLE ASSESSMENT QUESTIONS
    -- ----------------------------------------------------------------
    
    -- Get the ID of the first behavioral indicator for Leadership competency
    DECLARE
        leadership_indicator_id UUID;
        communication_indicator_id UUID;
        emotional_indicator_id UUID;
        problem_solving_indicator_id UUID;
    BEGIN
        -- Get the IDs of the first indicators for each competency
        SELECT id INTO leadership_indicator_id 
        FROM behavioral_indicators 
        WHERE competency_id = leadership_id 
        AND title = 'Формулирует долгосрочное видение';
        
        SELECT id INTO communication_indicator_id 
        FROM behavioral_indicators 
        WHERE competency_id = communication_id 
        AND title = 'Адаптирует стиль коммуникации под аудиторию';
        
        SELECT id INTO emotional_indicator_id 
        FROM behavioral_indicators 
        WHERE competency_id = emotional_id 
        AND title = 'Распознает эмоциональное состояние других';
        
        SELECT id INTO problem_solving_indicator_id 
        FROM behavioral_indicators 
        WHERE competency_id = problem_solving_id 
        AND title = 'Анализирует проблему системно';
        
        -- LEADERSHIP QUESTIONS
        
        -- Leadership Question 1 (Likert Scale)
        INSERT INTO assessment_questions (
            behavioral_indicator_id,
            question_text,
            question_type,
            answer_options,
            scoring_rubric,
            time_limit,
            difficulty_level,
            is_active,
            order_index
        ) VALUES (
            leadership_indicator_id,
            'Как часто вы проводите стратегические сессии с командой для обсуждения долгосрочных целей?',
            'LIKERT_SCALE',
            '[
                {"label": "Никогда", "value": 1},
                {"label": "Редко (раз в год)", "value": 2},
                {"label": "Иногда (раз в полгода)", "value": 3},
                {"label": "Регулярно (ежеквартально)", "value": 4},
                {"label": "Часто (ежемесячно)", "value": 5}
            ]',
            'Прямая оценка: баллы соответствуют выбранному значению от 1 до 5',
            180,
            'INTERMEDIATE',
            true,
            1
        );
        
        -- Leadership Question 2 (Situational Judgment)
        INSERT INTO assessment_questions (
            behavioral_indicator_id,
            question_text,
            question_type,
            answer_options,
            scoring_rubric,
            time_limit,
            difficulty_level,
            is_active,
            order_index
        ) VALUES (
            leadership_indicator_id,
            'Ваша команда работает над важным проектом. Какой подход вы выберете для определения стратегии развития?',
            'SITUATIONAL_JUDGMENT',
            '[
                {
                    "text": "Сосредоточусь на текущих задачах, не тратя время на долгосрочное планирование",
                    "score": 1,
                    "explanation": "Отсутствие стратегического мышления"
                },
                {
                    "text": "Создам план на ближайший месяц с конкретными задачами",
                    "score": 2,
                    "explanation": "Краткосрочное тактическое планирование"
                },
                {
                    "text": "Разработаю дорожную карту на полгода с ключевыми этапами",
                    "score": 3,
                    "explanation": "Среднесрочное планирование с элементами стратегии"
                },
                {
                    "text": "Создам стратегию на 2-3 года с учетом рыночных тенденций",
                    "score": 4,
                    "explanation": "Хорошее стратегическое мышление"
                },
                {
                    "text": "Сформулирую видение на 5+ лет и спущу его до конкретных тактических шагов",
                    "score": 5,
                    "explanation": "Отличное стратегическое лидерство с балансом долгосрочного видения и конкретных действий"
                }
            ]',
            'Оценка по шкале от 1 до 5 в зависимости от выбранного ответа',
            300,
            'ADVANCED',
            true,
            2
        );
        
        -- COMMUNICATION QUESTIONS
        
        -- Communication Question 1 (Likert Scale)
        INSERT INTO assessment_questions (
            behavioral_indicator_id,
            question_text,
            question_type,
            answer_options,
            scoring_rubric,
            time_limit,
            difficulty_level,
            is_active,
            order_index
        ) VALUES (
            communication_indicator_id,
            'Я меняю способ объяснения в зависимости от уровня знаний слушателей',
            'LIKERT_SCALE',
            '[
                {"label": "Полностью не согласен", "value": 1},
                {"label": "Скорее не согласен", "value": 2},
                {"label": "Частично согласен", "value": 3},
                {"label": "Скорее согласен", "value": 4},
                {"label": "Полностью согласен", "value": 5}
            ]',
            'Прямая оценка: 1=1 балл, 2=2 балла, 3=3 балла, 4=4 балла, 5=5 баллов',
            60,
            'INTERMEDIATE',
            true,
            1
        );
        
        -- Communication Question 2 (Situational Judgment)
        INSERT INTO assessment_questions (
            behavioral_indicator_id,
            question_text,
            question_type,
            answer_options,
            scoring_rubric,
            time_limit,
            difficulty_level,
            is_active,
            order_index
        ) VALUES (
            communication_indicator_id,
            'Вам нужно объяснить техническую концепцию команде, которая включает как опытных разработчиков, так и новых маркетологов. Как вы поступите?',
            'SITUATIONAL_JUDGMENT',
            '[
                {
                    "text": "Проведу две отдельные встречи с разным уровнем детализации",
                    "score": 5,
                    "explanation": "Оптимальный подход - адаптация под аудиторию"
                },
                {
                    "text": "Начну с базовых понятий, затем углублюсь в детали",
                    "score": 4,
                    "explanation": "Хороший подход с постепенным усложнением"
                },
                {
                    "text": "Подготовлю дополнительные материалы для новичков",
                    "score": 3,
                    "explanation": "Частично учитывает потребности аудитории"
                },
                {
                    "text": "Объясню на техническом уровне, как обычно",
                    "score": 1,
                    "explanation": "Не учитывает разный уровень аудитории"
                }
            ]',
            'Оценка от 1 до 5 в зависимости от выбранного варианта',
            180,
            'INTERMEDIATE',
            true,
            2
        );
        
        -- Communication Question 3 (Multiple Choice)
        INSERT INTO assessment_questions (
            behavioral_indicator_id,
            question_text,
            question_type,
            answer_options,
            scoring_rubric,
            time_limit,
            difficulty_level,
            is_active,
            order_index
        ) VALUES (
            communication_indicator_id,
            'Какой фактор НАИБОЛЕЕ важен при адаптации стиля коммуникации?',
            'MULTIPLE_CHOICE',
            '[
                {"text": "Личные предпочтения спикера", "correct": false},
                {"text": "Уровень знаний аудитории", "correct": true},
                {"text": "Время проведения встречи", "correct": false},
                {"text": "Размер аудитории", "correct": false}
            ]',
            'Правильный ответ = 5 баллов, неправильный = 0 баллов',
            45,
            'FOUNDATIONAL',
            true,
            3
        );
        
        -- EMOTIONAL INTELLIGENCE QUESTIONS
        
        -- Emotional Intelligence Question 1 (Likert Scale)
        INSERT INTO assessment_questions (
            behavioral_indicator_id,
            question_text,
            question_type,
            answer_options,
            scoring_rubric,
            time_limit,
            difficulty_level,
            is_active,
            order_index
        ) VALUES (
            emotional_indicator_id,
            'Я хорошо определяю эмоциональное состояние коллег по невербальным сигналам',
            'LIKERT_SCALE',
            '[
                {"label": "Полностью не согласен", "value": 1},
                {"label": "Скорее не согласен", "value": 2},
                {"label": "Частично согласен", "value": 3},
                {"label": "Скорее согласен", "value": 4},
                {"label": "Полностью согласен", "value": 5}
            ]',
            'Прямая оценка: баллы соответствуют выбранному значению от 1 до 5',
            60,
            'INTERMEDIATE',
            true,
            1
        );
        
        -- Emotional Intelligence Question 2 (Situational Judgment)
        INSERT INTO assessment_questions (
            behavioral_indicator_id,
            question_text,
            question_type,
            answer_options,
            scoring_rubric,
            time_limit,
            difficulty_level,
            is_active,
            order_index
        ) VALUES (
            emotional_indicator_id,
            'Вы замечаете, что коллега, обычно активный и энергичный, сегодня молчалив и избегает контактов. Как вы поступите?',
            'SITUATIONAL_JUDGMENT',
            '[
                {
                    "text": "Не буду обращать внимания, у каждого бывают плохие дни",
                    "score": 1,
                    "explanation": "Игнорирование эмоционального состояния"
                },
                {
                    "text": "Спрошу напрямую при всех, что с ним случилось",
                    "score": 2,
                    "explanation": "Неуместное внимание к эмоциональному состоянию"
                },
                {
                    "text": "Отправлю сообщение с вопросом все ли в порядке",
                    "score": 3,
                    "explanation": "Базовое внимание к эмоциональному состоянию"
                },
                {
                    "text": "Найду момент поговорить наедине и выразить поддержку",
                    "score": 5,
                    "explanation": "Оптимальный подход с уважением к личным границам"
                },
                {
                    "text": "Сообщу руководителю о странном поведении коллеги",
                    "score": 1,
                    "explanation": "Нарушение конфиденциальности, отсутствие эмпатии"
                }
            ]',
            'Оценка от 1 до 5 в зависимости от выбранного варианта',
            120,
            'INTERMEDIATE',
            true,
            2
        );
        
        -- PROBLEM SOLVING QUESTIONS
        
        -- Problem Solving Question 1 (Likert Scale)
        INSERT INTO assessment_questions (
            behavioral_indicator_id,
            question_text,
            question_type,
            answer_options,
            scoring_rubric,
            time_limit,
            difficulty_level,
            is_active,
            order_index
        ) VALUES (
            problem_solving_indicator_id,
            'Как часто вы используете структурированные методы анализа корневых причин проблем (например, диаграмма Ишикавы, метод 5 Почему)?',
            'LIKERT_SCALE',
            '[
                {"label": "Никогда", "value": 1},
                {"label": "Редко (в исключительных случаях)", "value": 2},
                {"label": "Иногда (для сложных проблем)", "value": 3},
                {"label": "Часто (для большинства задач)", "value": 4},
                {"label": "Всегда (систематический подход)", "value": 5}
            ]',
            'Прямая оценка: баллы соответствуют выбранному значению от 1 до 5',
            90,
            'INTERMEDIATE',
            true,
            1
        );
        
        -- Problem Solving Question 2 (Situational Judgment)
        INSERT INTO assessment_questions (
            behavioral_indicator_id,
            question_text,
            question_type,
            answer_options,
            scoring_rubric,
            time_limit,
            difficulty_level,
            is_active,
            order_index
        ) VALUES (
            problem_solving_indicator_id,
            'Ваша команда столкнулась с непредвиденной проблемой, которая препятствует выполнению проекта в срок. Как вы подойдете к решению?',
            'SITUATIONAL_JUDGMENT',
            '[
                {
                    "text": "Немедленно приступлю к реализации первого пришедшего в голову решения",
                    "score": 1,
                    "explanation": "Отсутствие системного анализа проблемы"
                },
                {
                    "text": "Поручу решение проблемы самому опытному специалисту в команде",
                    "score": 2,
                    "explanation": "Делегирование без системного анализа"
                },
                {
                    "text": "Проведу совещание для идентификации корневых причин и возможных решений",
                    "score": 4,
                    "explanation": "Хороший подход с вовлечением команды и анализом"
                },
                {
                    "text": "Соберу все доступные данные, проанализирую первопричины и разработаю несколько вариантов решения",
                    "score": 5,
                    "explanation": "Оптимальный структурированный подход"
                },
                {
                    "text": "Перенесу сроки проекта, чтобы у команды было достаточно времени решить проблему",
                    "score": 2,
                    "explanation": "Избегание решения проблемы"
                }
            ]',
            'Оценка от 1 до 5 в зависимости от выбранного варианта',
            180,
            'ADVANCED',
            true,
            2
        );
        
        -- Problem Solving Question 3 (Multiple Choice)
        INSERT INTO assessment_questions (
            behavioral_indicator_id,
            question_text,
            question_type,
            answer_options,
            scoring_rubric,
            time_limit,
            difficulty_level,
            is_active,
            order_index
        ) VALUES (
            problem_solving_indicator_id,
            'Какой из следующих шагов является НАИБОЛЕЕ важным при системном анализе проблемы?',
            'MULTIPLE_CHOICE',
            '[
                {"text": "Быстрое внедрение решения", "correct": false},
                {"text": "Сбор и анализ данных перед формулировкой выводов", "correct": true},
                {"text": "Определение виновных в возникновении проблемы", "correct": false},
                {"text": "Оценка стоимости решения проблемы", "correct": false}
            ]',
            'Правильный ответ = 5 баллов, неправильный = 0 баллов',
            60,
            'FOUNDATIONAL',
            true,
            3
        );
    END;
END;
$$;

-- ----------------------------------------------------------------
-- 4. STANDARD MAPPINGS
-- ----------------------------------------------------------------

DO $$
DECLARE
    leadership_id UUID;
    communication_id UUID;
    emotional_id UUID;
    critical_id UUID;
    problem_solving_id UUID;
BEGIN
    -- Get competency IDs
    SELECT id INTO leadership_id FROM competencies WHERE name = 'Стратегическое лидерство';
    SELECT id INTO communication_id FROM competencies WHERE name = 'Эффективная коммуникация';
    SELECT id INTO emotional_id FROM competencies WHERE name = 'Эмпатия и социальная осознанность';
    SELECT id INTO critical_id FROM competencies WHERE name = 'Критическое мышление';
    SELECT id INTO problem_solving_id FROM competencies WHERE name = 'Решение проблем';
    
    -- Leadership ESCO mapping
    INSERT INTO standard_mappings (
        competency_id,
        standard_type,
        standard_code,
        standard_name,
        mapping_confidence
    ) VALUES (
        leadership_id,
        'ESCO',
        'S7.1.1',
        'develop organisational strategies and policies',
        'HIGH_CONFIDENCE'
    );
    
    -- Leadership O*NET mapping
    INSERT INTO standard_mappings (
        competency_id,
        standard_type,
        standard_code,
        standard_name,
        mapping_confidence
    ) VALUES (
        leadership_id,
        'ONET',
        '2.B.3.c',
        'Leadership - Job requires a willingness to lead, take charge, and offer opinions and direction',
        'VERIFIED'
    );
    
    -- Communication ESCO mapping
    INSERT INTO standard_mappings (
        competency_id,
        standard_type,
        standard_code,
        standard_name,
        mapping_confidence
    ) VALUES (
        communication_id,
        'ESCO',
        'S2.1.1',
        'communicate with others',
        'HIGH_CONFIDENCE'
    );
    
    -- Communication Big Five mapping
    INSERT INTO standard_mappings (
        competency_id,
        standard_type,
        standard_code,
        standard_name,
        mapping_confidence
    ) VALUES (
        communication_id,
        'BIG_FIVE',
        'EXTRAVERSION',
        'Extraversion - Communication aspects',
        'MODERATE_CONFIDENCE'
    );
    
    -- Emotional Intelligence ESCO mapping
    INSERT INTO standard_mappings (
        competency_id,
        standard_type,
        standard_code,
        standard_name,
        mapping_confidence
    ) VALUES (
        emotional_id,
        'ESCO',
        'S4.7.1',
        'demonstrate empathy',
        'HIGH_CONFIDENCE'
    );
    
    -- Emotional Intelligence Big Five mapping
    INSERT INTO standard_mappings (
        competency_id,
        standard_type,
        standard_code,
        standard_name,
        mapping_confidence
    ) VALUES (
        emotional_id,
        'BIG_FIVE',
        'AGREEABLENESS',
        'Agreeableness - Empathy and concern for others',
        'HIGH_CONFIDENCE'
    );
    
    -- Critical Thinking ESCO mapping
    INSERT INTO standard_mappings (
        competency_id,
        standard_type,
        standard_code,
        standard_name,
        mapping_confidence
    ) VALUES (
        critical_id,
        'ESCO',
        'S4.9.1',
        'apply critical thinking',
        'VERIFIED'
    );
    
    -- Critical Thinking O*NET mapping
    INSERT INTO standard_mappings (
        competency_id,
        standard_type,
        standard_code,
        standard_name,
        mapping_confidence
    ) VALUES (
        critical_id,
        'ONET',
        '2.A.2.a',
        'Critical Thinking - Using logic and reasoning to identify strengths and weaknesses of alternative solutions',
        'HIGH_CONFIDENCE'
    );
    
    -- Problem Solving ESCO mapping
    INSERT INTO standard_mappings (
        competency_id,
        standard_type,
        standard_code,
        standard_name,
        mapping_confidence
    ) VALUES (
        problem_solving_id,
        'ESCO',
        'S4.4.1',
        'problem solving - identifying and analyzing issues, developing and implementing solutions',
        'VERIFIED'
    );
    
    -- Problem Solving O*NET mapping
    INSERT INTO standard_mappings (
        competency_id,
        standard_type,
        standard_code,
        standard_name,
        mapping_confidence
    ) VALUES (
        problem_solving_id,
        'ONET',
        '2.B.2.i',
        'Complex Problem Solving - Identifying complex problems and reviewing related information to develop and evaluate options',
        'HIGH_CONFIDENCE'
    );
END;
$$;