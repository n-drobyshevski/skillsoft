package app.skillsoft.assessmentbackend.integration;

import app.skillsoft.assessmentbackend.domain.dto.AssessmentQuestionDto;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.repository.AssessmentQuestionRepository;
import app.skillsoft.assessmentbackend.repository.BehavioralIndicatorRepository;
import app.skillsoft.assessmentbackend.repository.CompetencyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Complex integration tests for AssessmentQuestion functionality
 * 
 * These tests cover:
 * - End-to-end JSONB handling with database persistence
 * - Complex multi-layered scenarios with relationships
 * - Russian text handling in full integration context
 * - Complex JSONB data structures and transformations
 * - Cross-layer data flow validation
 * - Real database interaction with JSONB serialization
 * - Performance testing with complex data structures
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AssessmentQuestion Integration Tests - Complex JSONB Scenarios")
class AssessmentQuestionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AssessmentQuestionRepository assessmentQuestionRepository;

    @Autowired
    private BehavioralIndicatorRepository behavioralIndicatorRepository;

    @Autowired
    private CompetencyRepository competencyRepository;

    private Competency testCompetency;
    private BehavioralIndicator testBehavioralIndicator;
    private UUID competencyId;
    private UUID behavioralIndicatorId;

    @BeforeEach
    void setUp() {
        // Clear data
        assessmentQuestionRepository.deleteAll();
        behavioralIndicatorRepository.deleteAll();
        competencyRepository.deleteAll();

        // Create test competency
        testCompetency = new Competency();
        testCompetency.setName("Лидерство и управление командой");
        testCompetency.setDescription("Способность эффективно руководить и мотивировать команду");
        testCompetency.setActive(true);
        testCompetency.setCategory(CompetencyCategory.LEADERSHIP);
        testCompetency.setLevel(ProficiencyLevel.PROFICIENT);
        testCompetency.setApprovalStatus(ApprovalStatus.APPROVED);
        testCompetency.setVersion(1);
        testCompetency.setCreatedAt(LocalDateTime.now());
        testCompetency.setLastModified(LocalDateTime.now());
        testCompetency = competencyRepository.save(testCompetency);
        competencyId = testCompetency.getId();

        // Create test behavioral indicator
        testBehavioralIndicator = new BehavioralIndicator();
        testBehavioralIndicator.setCompetency(testCompetency);
        testBehavioralIndicator.setTitle("Делегирование задач");
        testBehavioralIndicator.setDescription("Умение эффективно распределять задачи между членами команды");
        testBehavioralIndicator.setActive(true);
        testBehavioralIndicator.setObservabilityLevel(ProficiencyLevel.PROFICIENT);
        testBehavioralIndicator.setMeasurementType(IndicatorMeasurementType.FREQUENCY);
        testBehavioralIndicator.setWeight(1.0f);
        testBehavioralIndicator.setApprovalStatus(ApprovalStatus.APPROVED);
        testBehavioralIndicator.setOrderIndex(1);
        testBehavioralIndicator = behavioralIndicatorRepository.save(testBehavioralIndicator);
        behavioralIndicatorId = testBehavioralIndicator.getId();
    }

    @Nested
    @DisplayName("Complex JSONB Integration Scenarios")
    class ComplexJsonbIntegrationTests {

        @Test
        @DisplayName("Should handle complex situational judgment scenario with Russian text")
        void shouldHandleComplexSituationalJudgmentScenarioWithRussianText() throws Exception {
            // Given - Complex situational judgment question with detailed Russian scenarios
            AssessmentQuestion complexQuestion = new AssessmentQuestion();
            complexQuestion.setBehavioralIndicatorId(behavioralIndicatorId);
            complexQuestion.setQuestionText("Вы руководите проектной командой из 8 человек. В процессе работы над критически важным проектом один из ведущих разработчиков заболел на неопределенный срок, что ставит под угрозу сроки сдачи проекта. Как вы поступите?");
            complexQuestion.setQuestionType(QuestionType.SITUATIONAL_JUDGMENT);
            complexQuestion.setScoringRubric("Оценка эффективности решения от 1 до 5 баллов с учетом долгосрочных последствий");
            complexQuestion.setDifficultyLevel(DifficultyLevel.EXPERT);
            complexQuestion.setTimeLimit(900); // 15 minutes
            complexQuestion.setActive(true);
            complexQuestion.setOrderIndex(1);

            // Complex answer options with multi-dimensional scoring
            List<Map<String, Object>> answerOptions = new ArrayList<>();

            Map<String, Object> option1 = new HashMap<>();
            option1.put("option_id", "A");
            option1.put("action", "Перераспределить задачи заболевшего сотрудника между остальными членами команды");
            option1.put("immediate_effectiveness", 3);
            option1.put("long_term_impact", 2);
            option1.put("team_morale_impact", 2);
            option1.put("explanation", "Быстрое решение, но может привести к перегрузке команды");
            Map<String, Object> consequences = new HashMap<>();
            consequences.put("positive", Arrays.asList("Быстрое решение", "Сохранение сроков"));
            consequences.put("negative", Arrays.asList("Перегрузка команды", "Снижение качества", "Выгорание сотрудников"));
            option1.put("consequences", consequences);
            answerOptions.add(option1);

            Map<String, Object> option2 = new HashMap<>();
            option2.put("option_id", "B");
            option2.put("action", "Найти временного замещающего специалиста или фрилансера");
            option2.put("immediate_effectiveness", 4);
            option2.put("long_term_impact", 4);
            option2.put("team_morale_impact", 4);
            option2.put("explanation", "Оптимальное решение с учетом всех факторов");
            Map<String, Object> consequences2 = new HashMap<>();
            consequences2.put("positive", Arrays.asList("Сохранение нагрузки команды", "Поддержание качества", "Соблюдение сроков"));
            consequences2.put("negative", Arrays.asList("Дополнительные расходы", "Время на адаптацию"));
            option2.put("consequences", consequences2);
            answerOptions.add(option2);

            Map<String, Object> option3 = new HashMap<>();
            option3.put("option_id", "C");
            option3.put("action", "Пересмотреть объем проекта и договориться с заказчиком о продлении сроков");
            option3.put("immediate_effectiveness", 5);
            option3.put("long_term_impact", 5);
            option3.put("team_morale_impact", 5);
            option3.put("explanation", "Стратегически верное решение с максимальной долгосрочной эффективностью");
            Map<String, Object> consequences3 = new HashMap<>();
            consequences3.put("positive", Arrays.asList("Реалистичные ожидания", "Качественный результат", "Здоровая рабочая среда"));
            consequences3.put("negative", Arrays.asList("Возможное недовольство заказчика", "Репутационные риски"));
            option3.put("consequences", consequences3);
            answerOptions.add(option3);

            Map<String, Object> option4 = new HashMap<>();
            option4.put("option_id", "D");
            option4.put("action", "Ничего не предпринимать, надеясь что сотрудник скоро выздоровеет");
            option4.put("immediate_effectiveness", 1);
            option4.put("long_term_impact", 1);
            option4.put("team_morale_impact", 1);
            option4.put("explanation", "Неэффективная стратегия, избегание ответственности");
            Map<String, Object> consequences4 = new HashMap<>();
            consequences4.put("positive", Arrays.asList("Отсутствие дополнительных действий"));
            consequences4.put("negative", Arrays.asList("Высокий риск срыва сроков", "Потеря доверия команды", "Стресс всей команды"));
            option4.put("consequences", consequences4);
            answerOptions.add(option4);

            complexQuestion.setAnswerOptions(answerOptions);

            // When - Create through REST API
            String jsonRequest = objectMapper.writeValueAsString(complexQuestion);
            
            mockMvc.perform(post("/api/competencies/{competencyId}/bi/{behavioralIndicatorId}/questions",
                            competencyId, behavioralIndicatorId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding("UTF-8")
                            .content(jsonRequest))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.questionText", containsString("руководите проектной командой")))
                    .andExpect(jsonPath("$.questionType", is("SITUATIONAL_JUDGMENT")))
                    .andExpect(jsonPath("$.answerOptions", hasSize(4)))
                    .andExpect(jsonPath("$.answerOptions[0].action", containsString("Перераспределить задачи")))
                    .andExpect(jsonPath("$.answerOptions[0].immediate_effectiveness", is(3)))
                    .andExpect(jsonPath("$.answerOptions[0].consequences.positive", hasSize(2)))
                    .andExpect(jsonPath("$.answerOptions[0].consequences.negative", hasSize(3)))
                    .andExpect(jsonPath("$.answerOptions[2].immediate_effectiveness", is(5)))
                    .andExpect(jsonPath("$.answerOptions[2].long_term_impact", is(5)))
                    .andExpect(jsonPath("$.answerOptions[2].team_morale_impact", is(5)));

            // Verify database persistence
            List<AssessmentQuestion> savedQuestions = assessmentQuestionRepository.findByBehavioralIndicator_Id(behavioralIndicatorId);
            assert savedQuestions.size() == 1;
            
            AssessmentQuestion savedQuestion = savedQuestions.get(0);
            assert savedQuestion.getAnswerOptions().size() == 4;
            
            Map<String, Object> savedOption = savedQuestion.getAnswerOptions().get(2);
            assert savedOption.get("immediate_effectiveness").equals(5);
            assert savedOption.get("long_term_impact").equals(5);
            assert savedOption.get("team_morale_impact").equals(5);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> savedConsequences = (Map<String, Object>) savedOption.get("consequences");
            @SuppressWarnings("unchecked")
            List<String> positiveConsequences = (List<String>) savedConsequences.get("positive");
            assert positiveConsequences.size() == 3;
            assert positiveConsequences.contains("Реалистичные ожидания");
        }

        @Test
        @DisplayName("Should handle complex multi-dimensional peer feedback with nested JSONB")
        void shouldHandleComplexMultiDimensionalPeerFeedbackWithNestedJsonb() throws Exception {
            // Given - Complex peer feedback question with multi-dimensional evaluation
            AssessmentQuestion peerFeedbackQuestion = new AssessmentQuestion();
            peerFeedbackQuestion.setBehavioralIndicatorId(behavioralIndicatorId);
            peerFeedbackQuestion.setQuestionText("Оцените лидерские качества вашего коллеги по различным критериям");
            peerFeedbackQuestion.setQuestionType(QuestionType.PEER_FEEDBACK);
            peerFeedbackQuestion.setScoringRubric("Комплексная оценка по нескольким измерениям с весовыми коэффициентами");
            peerFeedbackQuestion.setDifficultyLevel(DifficultyLevel.SPECIALIZED);
            peerFeedbackQuestion.setActive(true);
            peerFeedbackQuestion.setOrderIndex(1);

            List<Map<String, Object>> answerOptions = new ArrayList<>();

            Map<String, Object> evaluation = new HashMap<>();
            evaluation.put("evaluation_type", "multi_dimensional_leadership");
            
            Map<String, Object> dimensions = new HashMap<>();
            
            Map<String, Object> visionaryLeadership = new HashMap<>();
            visionaryLeadership.put("name", "Визионерское лидерство");
            visionaryLeadership.put("description", "Способность создавать и транслировать видение будущего");
            visionaryLeadership.put("weight", 0.25);
            Map<String, Object> visionaryScale = new HashMap<>();
            visionaryScale.put("min", 1);
            visionaryScale.put("max", 5);
            visionaryScale.put("labels", Arrays.asList("Отсутствует", "Слабо выражено", "Умеренно", "Хорошо выражено", "Исключительно"));
            visionaryLeadership.put("scale", visionaryScale);
            dimensions.put("visionary_leadership", visionaryLeadership);

            Map<String, Object> emotionalIntelligence = new HashMap<>();
            emotionalIntelligence.put("name", "Эмоциональный интеллект");
            emotionalIntelligence.put("description", "Умение понимать и управлять эмоциями");
            emotionalIntelligence.put("weight", 0.30);
            Map<String, Object> emotionalScale = new HashMap<>();
            emotionalScale.put("min", 1);
            emotionalScale.put("max", 5);
            emotionalScale.put("labels", Arrays.asList("Очень низкий", "Низкий", "Средний", "Высокий", "Очень высокий"));
            emotionalIntelligence.put("scale", emotionalScale);
            dimensions.put("emotional_intelligence", emotionalIntelligence);

            Map<String, Object> teamBuilding = new HashMap<>();
            teamBuilding.put("name", "Построение команды");
            teamBuilding.put("description", "Способность формировать и развивать эффективные команды");
            teamBuilding.put("weight", 0.25);
            Map<String, Object> teamScale = new HashMap<>();
            teamScale.put("min", 1);
            teamScale.put("max", 5);
            teamScale.put("labels", Arrays.asList("Неэффективно", "Слабо", "Удовлетворительно", "Хорошо", "Превосходно"));
            teamBuilding.put("scale", teamScale);
            dimensions.put("team_building", teamBuilding);

            Map<String, Object> decisionMaking = new HashMap<>();
            decisionMaking.put("name", "Принятие решений");
            decisionMaking.put("description", "Качество и скорость принятия управленческих решений");
            decisionMaking.put("weight", 0.20);
            Map<String, Object> decisionScale = new HashMap<>();
            decisionScale.put("min", 1);
            decisionScale.put("max", 5);
            decisionScale.put("labels", Arrays.asList("Очень плохо", "Плохо", "Средне", "Хорошо", "Отлично"));
            decisionMaking.put("scale", decisionScale);
            dimensions.put("decision_making", decisionMaking);

            evaluation.put("dimensions", dimensions);
            
            Map<String, Object> openFeedback = new HashMap<>();
            openFeedback.put("strengths_prompt", "Перечислите основные сильные стороны лидерства коллеги");
            openFeedback.put("improvements_prompt", "Какие области лидерства требуют развития?");
            openFeedback.put("specific_examples_prompt", "Приведите конкретные примеры проявления лидерских качеств");
            openFeedback.put("max_length", 1000);
            evaluation.put("open_feedback", openFeedback);

            answerOptions.add(evaluation);
            peerFeedbackQuestion.setAnswerOptions(answerOptions);

            // When - Create and retrieve
            String jsonRequest = objectMapper.writeValueAsString(peerFeedbackQuestion);

            String response = mockMvc
                    .perform(post("/api/competencies/{competencyId}/bi/{behavioralIndicatorId}/questions",
                            competencyId, behavioralIndicatorId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding("UTF-8")
                            .content(jsonRequest))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            AssessmentQuestionDto createdDto = objectMapper.readValue(response, AssessmentQuestionDto.class);
            
            // Then - Verify complex JSONB structure
            mockMvc.perform(get("/api/competencies/{competencyId}/bi/{behavioralIndicatorId}/questions/{questionId}",
                            competencyId, behavioralIndicatorId, createdDto.id())
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding("UTF-8"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.answerOptions[0].evaluation_type", is("multi_dimensional_leadership")))
                    .andExpect(jsonPath("$.answerOptions[0].dimensions.visionary_leadership.name", is("Визионерское лидерство")))
                    .andExpect(jsonPath("$.answerOptions[0].dimensions.visionary_leadership.weight", is(0.25)))
                    .andExpect(jsonPath("$.answerOptions[0].dimensions.emotional_intelligence.weight", is(0.30)))
                    .andExpect(jsonPath("$.answerOptions[0].dimensions.emotional_intelligence.scale.labels", hasSize(5)))
                    .andExpect(jsonPath("$.answerOptions[0].dimensions.team_building.scale.max", is(5)))
                    .andExpect(jsonPath("$.answerOptions[0].open_feedback.max_length", is(1000)));

            // Verify database persistence of complex nested structure
            List<AssessmentQuestion> savedQuestions = assessmentQuestionRepository.findByBehavioralIndicator_Id(behavioralIndicatorId);
            AssessmentQuestion savedQuestion = savedQuestions.get(0);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> savedEvaluation = (Map<String, Object>) savedQuestion.getAnswerOptions().get(0);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> savedDimensions = (Map<String, Object>) savedEvaluation.get("dimensions");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> savedVisionary = (Map<String, Object>) savedDimensions.get("visionary_leadership");
            
            assert savedVisionary.get("name").equals("Визионерское лидерство");
            assert savedVisionary.get("weight").equals(0.25);
        }

        @Test
        @DisplayName("Should handle complex self-reflection with dynamic prompts and scoring")
        void shouldHandleComplexSelfReflectionWithDynamicPromptsAndScoring() throws Exception {
            // Given - Complex self-reflection with adaptive prompts
            AssessmentQuestion selfReflectionQuestion = new AssessmentQuestion();
            selfReflectionQuestion.setBehavioralIndicatorId(behavioralIndicatorId);
            selfReflectionQuestion.setQuestionText("Проведите глубокий анализ своих лидерских качеств в контексте последних рабочих ситуаций");
            selfReflectionQuestion.setQuestionType(QuestionType.SELF_REFLECTION);
            selfReflectionQuestion.setScoringRubric("Качественная оценка глубины самоанализа и реалистичности самооценки с использованием ИИ-анализа");
            selfReflectionQuestion.setDifficultyLevel(DifficultyLevel.EXPERT);
            selfReflectionQuestion.setTimeLimit(1800); // 30 minutes
            selfReflectionQuestion.setActive(true);
            selfReflectionQuestion.setOrderIndex(1);

            List<Map<String, Object>> answerOptions = new ArrayList<>();

            Map<String, Object> reflectionFramework = new HashMap<>();
            reflectionFramework.put("framework_type", "structured_leadership_reflection");
            
            List<Map<String, Object>> reflectionStages = new ArrayList<>();
            
            Map<String, Object> stage1 = new HashMap<>();
            stage1.put("stage", "situation_analysis");
            stage1.put("title", "Анализ ситуации");
            stage1.put("prompt", "Опишите 2-3 недавние рабочие ситуации, где вы выступали в роли лидера");
            stage1.put("min_length", 300);
            stage1.put("max_length", 800);
            Map<String, Object> guiding_questions1 = new HashMap<>();
            guiding_questions1.put("context", "Каков был контекст ситуации?");
            guiding_questions1.put("stakeholders", "Кто были ключевые участники?");
            guiding_questions1.put("challenges", "Какие основные вызовы вы выявили?");
            stage1.put("guiding_questions", guiding_questions1);
            reflectionStages.add(stage1);

            Map<String, Object> stage2 = new HashMap<>();
            stage2.put("stage", "behavior_analysis");
            stage2.put("title", "Анализ поведения");
            stage2.put("prompt", "Проанализируйте свое лидерское поведение в описанных ситуациях");
            stage2.put("min_length", 400);
            stage2.put("max_length", 1000);
            Map<String, Object> guiding_questions2 = new HashMap<>();
            guiding_questions2.put("actions", "Какие конкретные действия вы предприняли?");
            guiding_questions2.put("communication", "Как вы общались с командой?");
            guiding_questions2.put("decisions", "Какие ключевые решения приняли?");
            guiding_questions2.put("adaptation", "Как адаптировали стиль под ситуацию?");
            stage2.put("guiding_questions", guiding_questions2);
            reflectionStages.add(stage2);

            Map<String, Object> stage3 = new HashMap<>();
            stage3.put("stage", "impact_assessment");
            stage3.put("title", "Оценка влияния");
            stage3.put("prompt", "Оцените результаты и влияние своего лидерства");
            stage3.put("min_length", 300);
            stage3.put("max_length", 700);
            Map<String, Object> guiding_questions3 = new HashMap<>();
            guiding_questions3.put("outcomes", "Каких результатов удалось достичь?");
            guiding_questions3.put("team_response", "Как отреагировала команда?");
            guiding_questions3.put("unexpected", "Что произошло неожиданного?");
            stage3.put("guiding_questions", guiding_questions3);
            reflectionStages.add(stage3);

            Map<String, Object> stage4 = new HashMap<>();
            stage4.put("stage", "learning_insights");
            stage4.put("title", "Выводы и обучение");
            stage4.put("prompt", "Сформулируйте ключевые выводы и планы развития");
            stage4.put("min_length", 350);
            stage4.put("max_length", 900);
            Map<String, Object> guiding_questions4 = new HashMap<>();
            guiding_questions4.put("strengths", "Какие сильные стороны подтвердились?");
            guiding_questions4.put("growth_areas", "Какие области требуют развития?");
            guiding_questions4.put("lessons", "Какие уроки извлекли?");
            guiding_questions4.put("development_plan", "Как планируете развиваться дальше?");
            stage4.put("guiding_questions", guiding_questions4);
            reflectionStages.add(stage4);

            reflectionFramework.put("stages", reflectionStages);
            
            Map<String, Object> scoring_criteria = new HashMap<>();
            scoring_criteria.put("depth_of_analysis", "Глубина и детальность анализа");
            scoring_criteria.put("self_awareness", "Уровень самосознания и критического мышления");
            scoring_criteria.put("concrete_examples", "Использование конкретных примеров");
            scoring_criteria.put("learning_orientation", "Ориентация на обучение и развитие");
            scoring_criteria.put("realism", "Реалистичность самооценки");
            reflectionFramework.put("scoring_criteria", scoring_criteria);

            answerOptions.add(reflectionFramework);
            selfReflectionQuestion.setAnswerOptions(answerOptions);

            // When & Then - Create and verify
            mockMvc.perform(post("/api/competencies/{competencyId}/bi/{behavioralIndicatorId}/questions",
                            competencyId, behavioralIndicatorId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding("UTF-8")
                            .content(objectMapper.writeValueAsString(selfReflectionQuestion)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.answerOptions[0].framework_type", is("structured_leadership_reflection")))
                    .andExpect(jsonPath("$.answerOptions[0].stages", hasSize(4)))
                    .andExpect(jsonPath("$.answerOptions[0].stages[0].stage", is("situation_analysis")))
                    .andExpect(jsonPath("$.answerOptions[0].stages[0].min_length", is(300)))
                    .andExpect(jsonPath("$.answerOptions[0].stages[1].guiding_questions.communication", containsString("общались")))
                    .andExpect(jsonPath("$.answerOptions[0].stages[3].guiding_questions.development_plan", containsString("развиваться")))
                    .andExpect(jsonPath("$.answerOptions[0].scoring_criteria.depth_of_analysis", containsString("Глубина")));
        }
    }

    @Nested
    @DisplayName("Multi-Question Complex Scenarios")
    class MultiQuestionComplexScenarios {

        @Test
        @DisplayName("Should handle full assessment battery with various question types and JSONB structures")
        void shouldHandleFullAssessmentBatteryWithVariousQuestionTypesAndJsonbStructures() throws Exception {
            // Given - Create a complete assessment battery
            List<AssessmentQuestion> assessmentBattery = Arrays.asList(
                    createLikertScaleQuestion(),
                    createMultipleChoiceQuestion(),
                    createSituationalJudgmentQuestion(),
                    createFrequencyScaleQuestion(),
                    createSelfReflectionQuestion(),
                    createPeerFeedbackQuestion()
            );

            List<UUID> createdQuestionIds = new ArrayList<>();

            // When - Create all questions
            for (AssessmentQuestion question : assessmentBattery) {
                String response = mockMvc
                        .perform(post("/api/competencies/{competencyId}/bi/{behavioralIndicatorId}/questions",
                                competencyId, behavioralIndicatorId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .characterEncoding("UTF-8")
                                .content(objectMapper.writeValueAsString(question)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
                
                AssessmentQuestionDto dto = objectMapper.readValue(response, AssessmentQuestionDto.class);
                createdQuestionIds.add(dto.id());
            }

            // Then - Verify all questions exist and work together
            mockMvc.perform(get("/api/competencies/{competencyId}/bi/{behavioralIndicatorId}/questions",
                            competencyId, behavioralIndicatorId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(6)))
                    .andExpect(jsonPath("$[?(@.questionType == 'LIKERT_SCALE')]", hasSize(1)))
                    .andExpect(jsonPath("$[?(@.questionType == 'MULTIPLE_CHOICE')]", hasSize(1)))
                    .andExpect(jsonPath("$[?(@.questionType == 'SITUATIONAL_JUDGMENT')]", hasSize(1)))
                    .andExpect(jsonPath("$[?(@.questionType == 'FREQUENCY_SCALE')]", hasSize(1)))
                    .andExpect(jsonPath("$[?(@.questionType == 'SELF_REFLECTION')]", hasSize(1)))
                    .andExpect(jsonPath("$[?(@.questionType == 'PEER_FEEDBACK')]", hasSize(1)));

            // Verify database persistence
            List<AssessmentQuestion> savedQuestions = assessmentQuestionRepository.findByBehavioralIndicator_Id(behavioralIndicatorId);
            assert savedQuestions.size() == 6;

            // Verify different JSONB structures are properly persisted
            Optional<AssessmentQuestion> situationalQuestion = savedQuestions.stream()
                    .filter(q -> q.getQuestionType() == QuestionType.SITUATIONAL_JUDGMENT)
                    .findFirst();
            assert situationalQuestion.isPresent();
            assert situationalQuestion.get().getAnswerOptions().size() == 3;

            Optional<AssessmentQuestion> reflectionQuestion = savedQuestions.stream()
                    .filter(q -> q.getQuestionType() == QuestionType.SELF_REFLECTION)
                    .findFirst();
            assert reflectionQuestion.isPresent();
            assert reflectionQuestion.get().getAnswerOptions().size() == 2;
        }

        @Test
        @DisplayName("Should update questions with complex JSONB changes")
        void shouldUpdateQuestionsWithComplexJsonbChanges() throws Exception {
            // Given - Create initial question
            AssessmentQuestion initialQuestion = createSituationalJudgmentQuestion();
            
            String response = mockMvc
                    .perform(post("/api/competencies/{competencyId}/bi/{behavioralIndicatorId}/questions",
                            competencyId, behavioralIndicatorId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(initialQuestion)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            AssessmentQuestionDto createdDto = objectMapper.readValue(response, AssessmentQuestionDto.class);

            // When - Update with completely new JSONB structure
            AssessmentQuestion updateRequest = new AssessmentQuestion();
            updateRequest.setQuestionText("Обновленный сценарий лидерства в кризисной ситуации");
            updateRequest.setQuestionType(QuestionType.SITUATIONAL_JUDGMENT);
            
            List<Map<String, Object>> newAnswerOptions = new ArrayList<>();
            
            Map<String, Object> option1 = new HashMap<>();
            option1.put("scenario_id", "CRISIS_A");
            option1.put("response", "Принять решение единолично");
            option1.put("leadership_style", "авторитарный");
            option1.put("effectiveness_score", 3);
            Map<String, Object> metrics1 = new HashMap<>();
            metrics1.put("speed", 5);
            metrics1.put("team_buy_in", 2);
            metrics1.put("long_term_sustainability", 2);
            option1.put("metrics", metrics1);
            newAnswerOptions.add(option1);

            Map<String, Object> option2 = new HashMap<>();
            option2.put("scenario_id", "CRISIS_B");
            option2.put("response", "Собрать экстренное совещание команды");
            option2.put("leadership_style", "коллегиальный");
            option2.put("effectiveness_score", 5);
            Map<String, Object> metrics2 = new HashMap<>();
            metrics2.put("speed", 3);
            metrics2.put("team_buy_in", 5);
            metrics2.put("long_term_sustainability", 5);
            option2.put("metrics", metrics2);
            newAnswerOptions.add(option2);

            updateRequest.setAnswerOptions(newAnswerOptions);
            updateRequest.setScoringRubric("Новая система оценки кризисного лидерства");
            updateRequest.setDifficultyLevel(DifficultyLevel.EXPERT);

            // Then - Verify update
            mockMvc.perform(put("/api/competencies/{competencyId}/bi/{behavioralIndicatorId}/questions/{questionId}",
                            competencyId, behavioralIndicatorId, createdDto.id())
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding("UTF-8")
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.questionText", containsString("кризисной ситуации")))
                    .andExpect(jsonPath("$.answerOptions", hasSize(2)))
                    .andExpect(jsonPath("$.answerOptions[0].scenario_id", is("CRISIS_A")))
                    .andExpect(jsonPath("$.answerOptions[0].leadership_style", is("авторитарный")))
                    .andExpect(jsonPath("$.answerOptions[1].metrics.team_buy_in", is(5)))
                    .andExpect(jsonPath("$.scoringRubric", containsString("кризисного лидерства")));
        }
    }

    // Helper methods for creating different question types
    private AssessmentQuestion createLikertScaleQuestion() {
        AssessmentQuestion question = new AssessmentQuestion();
        question.setBehavioralIndicatorId(behavioralIndicatorId);
        question.setQuestionText("Я эффективно делегирую задачи членам команды");
        question.setQuestionType(QuestionType.LIKERT_SCALE);
        question.setAnswerOptions(createLikertScaleOptions());
        question.setScoringRubric("Стандартная оценка по шкале Лайкерта");
        question.setDifficultyLevel(DifficultyLevel.FOUNDATIONAL);
        question.setActive(true);
        question.setOrderIndex(1);
        return question;
    }

    private AssessmentQuestion createMultipleChoiceQuestion() {
        AssessmentQuestion question = new AssessmentQuestion();
        question.setBehavioralIndicatorId(behavioralIndicatorId);
        question.setQuestionText("Какой подход наиболее эффективен при делегировании сложной задачи?");
        question.setQuestionType(QuestionType.MULTIPLE_CHOICE);
        question.setAnswerOptions(createMultipleChoiceOptions());
        question.setScoringRubric("Одн правильный ответ из четырех вариантов");
        question.setDifficultyLevel(DifficultyLevel.INTERMEDIATE);
        question.setActive(true);
        question.setOrderIndex(2);
        return question;
    }

    private AssessmentQuestion createSituationalJudgmentQuestion() {
        AssessmentQuestion question = new AssessmentQuestion();
        question.setBehavioralIndicatorId(behavioralIndicatorId);
        question.setQuestionText("Член вашей команды постоянно не справляется с делегированными задачами");
        question.setQuestionType(QuestionType.SITUATIONAL_JUDGMENT);
        question.setAnswerOptions(createSituationalJudgmentOptions());
        question.setScoringRubric("Оценка эффективности решения проблемы");
        question.setDifficultyLevel(DifficultyLevel.ADVANCED);
        question.setActive(true);
        question.setOrderIndex(3);
        return question;
    }

    private AssessmentQuestion createFrequencyScaleQuestion() {
        AssessmentQuestion question = new AssessmentQuestion();
        question.setBehavioralIndicatorId(behavioralIndicatorId);
        question.setQuestionText("Как часто вы проверяете прогресс делегированных задач?");
        question.setQuestionType(QuestionType.FREQUENCY_SCALE);
        question.setAnswerOptions(createFrequencyScaleOptions());
        question.setScoringRubric("Оценка частоты контроля");
        question.setDifficultyLevel(DifficultyLevel.INTERMEDIATE);
        question.setActive(true);
        question.setOrderIndex(4);
        return question;
    }

    private AssessmentQuestion createSelfReflectionQuestion() {
        AssessmentQuestion question = new AssessmentQuestion();
        question.setBehavioralIndicatorId(behavioralIndicatorId);
        question.setQuestionText("Опишите ситуацию, когда ваше делегирование было особенно успешным");
        question.setQuestionType(QuestionType.SELF_REFLECTION);
        question.setAnswerOptions(createSelfReflectionOptions());
        question.setScoringRubric("Качественная оценка самоанализа");
        question.setDifficultyLevel(DifficultyLevel.ADVANCED);
        question.setActive(true);
        question.setOrderIndex(5);
        return question;
    }

    private AssessmentQuestion createPeerFeedbackQuestion() {
        AssessmentQuestion question = new AssessmentQuestion();
        question.setBehavioralIndicatorId(behavioralIndicatorId);
        question.setQuestionText("Оцените навыки делегирования вашего коллеги");
        question.setQuestionType(QuestionType.PEER_FEEDBACK);
        question.setAnswerOptions(createPeerFeedbackOptions());
        question.setScoringRubric("360-градусная оценка навыков делегирования");
        question.setDifficultyLevel(DifficultyLevel.SPECIALIZED);
        question.setActive(true);
        question.setOrderIndex(6);
        return question;
    }

    // Helper methods for answer options
    private List<Map<String, Object>> createLikertScaleOptions() {
        List<Map<String, Object>> options = new ArrayList<>();
        String[] labels = {"Совершенно не согласен", "Не согласен", "Нейтрально", "Согласен", "Полностью согласен"};
        for (int i = 0; i < 5; i++) {
            Map<String, Object> option = new HashMap<>();
            option.put("value", i + 1);
            option.put("label", labels[i]);
            option.put("score", i + 1);
            options.add(option);
        }
        return options;
    }

    private List<Map<String, Object>> createMultipleChoiceOptions() {
        List<Map<String, Object>> options = new ArrayList<>();
        
        Map<String, Object> optionA = new HashMap<>();
        optionA.put("id", "A");
        optionA.put("text", "Дать подробные инструкции и регулярно проверять прогресс");
        optionA.put("isCorrect", true);
        options.add(optionA);

        Map<String, Object> optionB = new HashMap<>();
        optionB.put("id", "B");
        optionB.put("text", "Делегировать и не вмешиваться в процесс");
        optionB.put("isCorrect", false);
        options.add(optionB);

        return options;
    }

    private List<Map<String, Object>> createSituationalJudgmentOptions() {
        List<Map<String, Object>> options = new ArrayList<>();

        Map<String, Object> option1 = new HashMap<>();
        option1.put("action", "Провести тренинг по недостающим навыкам");
        option1.put("effectiveness", 5);
        option1.put("explanation", "Развитие сотрудника");
        options.add(option1);

        Map<String, Object> option2 = new HashMap<>();
        option2.put("action", "Перераспределить задачи");
        option2.put("effectiveness", 2);
        option2.put("explanation", "Избегание проблемы");
        options.add(option2);

        Map<String, Object> option3 = new HashMap<>();
        option3.put("action", "Обсудить трудности и найти решение вместе");
        option3.put("effectiveness", 4);
        option3.put("explanation", "Совместное решение проблем");
        options.add(option3);

        return options;
    }

    private List<Map<String, Object>> createFrequencyScaleOptions() {
        List<Map<String, Object>> options = new ArrayList<>();
        String[] frequencies = {"Никогда", "Редко", "Иногда", "Регулярно", "Постоянно"};
        for (int i = 0; i < 5; i++) {
            Map<String, Object> option = new HashMap<>();
            option.put("frequency", i);
            option.put("label", frequencies[i]);
            option.put("score", i + 1);
            options.add(option);
        }
        return options;
    }

    private List<Map<String, Object>> createSelfReflectionOptions() {
        List<Map<String, Object>> options = new ArrayList<>();

        Map<String, Object> option1 = new HashMap<>();
        option1.put("reflection_type", "success_analysis");
        option1.put("prompt", "Опишите ситуацию успешного делегирования");
        option1.put("max_length", 500);
        options.add(option1);

        Map<String, Object> option2 = new HashMap<>();
        option2.put("reflection_type", "lessons_learned");
        option2.put("prompt", "Какие уроки вы извлекли из этого опыта?");
        option2.put("max_length", 300);
        options.add(option2);

        return options;
    }

    private List<Map<String, Object>> createPeerFeedbackOptions() {
        List<Map<String, Object>> options = new ArrayList<>();

        Map<String, Object> option1 = new HashMap<>();
        option1.put("feedback_type", "delegation_effectiveness");
        option1.put("prompt", "Насколько эффективно коллега делегирует задачи?");
        option1.put("scale_min", 1);
        option1.put("scale_max", 5);
        options.add(option1);

        return options;
    }
}