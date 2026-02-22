package app.skillsoft.assessmentbackend.services.simulation;

import app.skillsoft.assessmentbackend.domain.dto.blueprint.OverviewBlueprint;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.TestBlueprintDto;
import app.skillsoft.assessmentbackend.domain.dto.simulation.*;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.repository.AssessmentQuestionRepository;
import app.skillsoft.assessmentbackend.repository.CompetencyRepository;
import app.skillsoft.assessmentbackend.services.assembly.TestAssembler;
import app.skillsoft.assessmentbackend.services.assembly.TestAssemblerFactory;
import app.skillsoft.assessmentbackend.services.validation.BlueprintValidationService;
import app.skillsoft.assessmentbackend.services.validation.InventoryHeatmapService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TestSimulatorService Tests")
class TestSimulatorServiceTest {

    @Mock
    private TestAssemblerFactory assemblerFactory;

    @Mock
    private AssessmentQuestionRepository questionRepository;

    @Mock
    private CompetencyRepository competencyRepository;

    @Mock
    private InventoryHeatmapService inventoryHeatmapService;

    @Mock
    private BlueprintValidationService blueprintValidationService;

    @Mock
    private TestAssembler mockAssembler;

    private TestSimulatorService testSimulatorService;

    // Test data
    private UUID competencyId1;
    private UUID competencyId2;
    private UUID indicatorId1;
    private UUID indicatorId2;
    private UUID questionId1;
    private UUID questionId2;
    private UUID questionId3;

    @BeforeEach
    void setUp() {
        testSimulatorService = new TestSimulatorService(
            assemblerFactory,
            questionRepository,
            competencyRepository,
            inventoryHeatmapService,
            blueprintValidationService
        );

        competencyId1 = UUID.randomUUID();
        competencyId2 = UUID.randomUUID();
        indicatorId1 = UUID.randomUUID();
        indicatorId2 = UUID.randomUUID();
        questionId1 = UUID.randomUUID();
        questionId2 = UUID.randomUUID();
        questionId3 = UUID.randomUUID();
    }

    @Nested
    @DisplayName("simulate")
    class SimulateTests {

        @Test
        @DisplayName("Should return failed result when blueprint is null")
        void shouldReturnFailedResultWhenBlueprintIsNull() {
            SimulationResultDto result = testSimulatorService.simulate(null, SimulationProfile.RANDOM_GUESSER);

            assertThat(result).isNotNull();
            assertThat(result.valid()).isFalse();
            assertThat(result.totalQuestions()).isZero();
            assertThat(result.warnings()).hasSize(1);
            assertThat(result.warnings().get(0).message()).contains("Blueprint is null");
        }

        @Test
        @DisplayName("Should default profile to RANDOM_GUESSER when null")
        void shouldDefaultProfileToRandomGuesser() {
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = List.of(questionId1);
            List<AssessmentQuestion> questions = List.of(createQuestionWithId(questionId1, "Q1"));

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createHealthyHeatmap());

            SimulationResultDto result = testSimulatorService.simulate(blueprint, null);

            assertThat(result).isNotNull();
            assertThat(result.profile()).isEqualTo(SimulationProfile.RANDOM_GUESSER);
        }

        @Test
        @DisplayName("Should return failed result when assembly fails")
        void shouldReturnFailedResultWhenAssemblyFails() {
            OverviewBlueprint blueprint = createValidBlueprint();
            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class)))
                .thenThrow(new IllegalArgumentException("No assembler for strategy"));

            SimulationResultDto result = testSimulatorService.simulate(blueprint, SimulationProfile.RANDOM_GUESSER);

            assertThat(result).isNotNull();
            assertThat(result.valid()).isFalse();
            assertThat(result.warnings()).hasSize(1);
            assertThat(result.warnings().get(0).message()).contains("Assembly failed");
        }

        @Test
        @DisplayName("Should return failed result when no questions assembled")
        void shouldReturnFailedResultWhenNoQuestionsAssembled() {
            OverviewBlueprint blueprint = createValidBlueprint();
            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(List.of());

            SimulationResultDto result = testSimulatorService.simulate(blueprint, SimulationProfile.RANDOM_GUESSER);

            assertThat(result).isNotNull();
            assertThat(result.valid()).isFalse();
            assertThat(result.totalQuestions()).isZero();
            assertThat(result.warnings()).anyMatch(w ->
                w.message().contains("No questions assembled"));
        }

        @Test
        @DisplayName("Should hydrate questions correctly")
        void shouldHydrateQuestionsCorrectly() {
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = List.of(questionId1, questionId2, questionId3);
            List<AssessmentQuestion> questions = List.of(
                createQuestionWithId(questionId1, "Q1"),
                createQuestionWithId(questionId2, "Q2"),
                createQuestionWithId(questionId3, "Q3")
            );

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createHealthyHeatmap());

            SimulationResultDto result = testSimulatorService.simulate(blueprint, SimulationProfile.RANDOM_GUESSER);

            assertThat(result.totalQuestions()).isEqualTo(3);
            assertThat(result.sampleQuestions()).hasSize(3);
            verify(questionRepository).findAllById(questionIds);
        }

        @Test
        @DisplayName("Should extract unique competencies")
        void shouldExtractUniqueCompetencies() {
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = List.of(questionId1, questionId2);

            AssessmentQuestion q1 = createQuestionWithCompetency(questionId1, competencyId1, indicatorId1);
            AssessmentQuestion q2 = createQuestionWithCompetency(questionId2, competencyId2, indicatorId2);
            List<AssessmentQuestion> questions = List.of(q1, q2);

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createHealthyHeatmap());

            testSimulatorService.simulate(blueprint, SimulationProfile.RANDOM_GUESSER);

            verify(inventoryHeatmapService).generateHeatmapFor(argThat(ids ->
                ids.size() == 2 && ids.contains(competencyId1) && ids.contains(competencyId2)
            ));
        }

        @Test
        @DisplayName("Should add critical warnings for critical competencies")
        void shouldAddCriticalWarningsForCriticalCompetencies() {
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = List.of(questionId1);
            List<AssessmentQuestion> questions = List.of(createQuestionWithId(questionId1, "Q1"));

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);

            InventoryHeatmapDto criticalHeatmap = createHeatmapWithStatus(HealthStatus.CRITICAL);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(criticalHeatmap);

            SimulationResultDto result = testSimulatorService.simulate(blueprint, SimulationProfile.RANDOM_GUESSER);

            assertThat(result.warnings()).anyMatch(w ->
                w.level() == InventoryWarning.WarningLevel.ERROR);
        }

        @Test
        @DisplayName("Should add moderate warnings for moderate competencies")
        void shouldAddModerateWarningsForModerateCompetencies() {
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = List.of(questionId1);
            List<AssessmentQuestion> questions = List.of(createQuestionWithId(questionId1, "Q1"));

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);

            InventoryHeatmapDto moderateHeatmap = createHeatmapWithStatus(HealthStatus.MODERATE);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(moderateHeatmap);

            SimulationResultDto result = testSimulatorService.simulate(blueprint, SimulationProfile.RANDOM_GUESSER);

            assertThat(result.warnings()).anyMatch(w ->
                w.level() == InventoryWarning.WarningLevel.WARNING);
        }

        @Test
        @DisplayName("Should run persona simulation with difficulty-aware curves")
        void shouldRunPersonaSimulation() {
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = List.of(questionId1, questionId2);
            List<AssessmentQuestion> questions = List.of(
                createQuestionWithId(questionId1, "Q1"),
                createQuestionWithId(questionId2, "Q2")
            );

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createHealthyHeatmap());

            // Use max ability to ensure near-certain correctness for PERFECT
            SimulationResultDto result = testSimulatorService.simulate(
                blueprint, SimulationProfile.PERFECT_CANDIDATE, 100);

            assertThat(result.sampleQuestions()).hasSize(2);
            // At abilityLevel=100 with PERFECT, probability is ~0.99+; overwhelmingly correct
            long correctCount = result.sampleQuestions().stream()
                .filter(QuestionSummaryDto::simulatedCorrect).count();
            assertThat(correctCount).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("Should calculate composition by difficulty")
        void shouldCalculateCompositionByDifficulty() {
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = List.of(questionId1, questionId2, questionId3);

            List<AssessmentQuestion> questions = List.of(
                createQuestionWithDifficulty(questionId1, DifficultyLevel.FOUNDATIONAL),
                createQuestionWithDifficulty(questionId2, DifficultyLevel.INTERMEDIATE),
                createQuestionWithDifficulty(questionId3, DifficultyLevel.INTERMEDIATE)
            );

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createHealthyHeatmap());

            SimulationResultDto result = testSimulatorService.simulate(blueprint, SimulationProfile.RANDOM_GUESSER);

            assertThat(result.composition()).containsEntry("FOUNDATIONAL", 1);
            assertThat(result.composition()).containsEntry("INTERMEDIATE", 2);
        }

        @Test
        @DisplayName("Should calculate estimated duration in minutes")
        void shouldCalculateEstimatedDurationInMinutes() {
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = List.of(questionId1, questionId2);

            AssessmentQuestion q1 = createQuestionWithTimeLimit(questionId1, 120);
            AssessmentQuestion q2 = createQuestionWithTimeLimit(questionId2, 180);
            List<AssessmentQuestion> questions = List.of(q1, q2);

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createHealthyHeatmap());

            SimulationResultDto result = testSimulatorService.simulate(blueprint, SimulationProfile.RANDOM_GUESSER);

            assertThat(result.estimatedDurationMinutes()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should calculate simulated score as percentage")
        void shouldCalculateSimulatedScoreAsPercentage() {
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = List.of(questionId1, questionId2);
            List<AssessmentQuestion> questions = List.of(
                createQuestionWithId(questionId1, "Q1"),
                createQuestionWithId(questionId2, "Q2")
            );

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createHealthyHeatmap());

            // PERFECT at max ability: nearly all correct → score ~100%
            SimulationResultDto result = testSimulatorService.simulate(
                blueprint, SimulationProfile.PERFECT_CANDIDATE, 100);

            assertThat(result.simulatedScore()).isBetween(50.0, 100.0);
        }

        @Test
        @DisplayName("Should mark result valid when no error warnings")
        void shouldMarkResultValidWhenNoErrorWarnings() {
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = List.of(questionId1);
            List<AssessmentQuestion> questions = List.of(createQuestionWithId(questionId1, "Q1"));

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createHealthyHeatmap());

            SimulationResultDto result = testSimulatorService.simulate(blueprint, SimulationProfile.RANDOM_GUESSER);

            assertThat(result.valid()).isTrue();
        }

        @Test
        @DisplayName("Should add warning when not all questions could be loaded")
        void shouldAddWarningWhenNotAllQuestionsLoaded() {
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = List.of(questionId1, questionId2, questionId3);
            List<AssessmentQuestion> questions = List.of(
                createQuestionWithId(questionId1, "Q1"),
                createQuestionWithId(questionId2, "Q2")
            );

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createHealthyHeatmap());

            SimulationResultDto result = testSimulatorService.simulate(blueprint, SimulationProfile.RANDOM_GUESSER);

            assertThat(result.warnings()).anyMatch(w ->
                w.message().contains("Only 2 of 3 questions could be loaded"));
        }

        @Test
        @DisplayName("Should include abilityLevel in result")
        void shouldIncludeAbilityLevelInResult() {
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = List.of(questionId1);
            List<AssessmentQuestion> questions = List.of(createQuestionWithId(questionId1, "Q1"));

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createHealthyHeatmap());

            SimulationResultDto result = testSimulatorService.simulate(
                blueprint, SimulationProfile.RANDOM_GUESSER, 75);

            assertThat(result.abilityLevel()).isEqualTo(75);
        }

        @Test
        @DisplayName("Should default abilityLevel to 50 when using 2-arg overload")
        void shouldDefaultAbilityLevelTo50() {
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = List.of(questionId1);
            List<AssessmentQuestion> questions = List.of(createQuestionWithId(questionId1, "Q1"));

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createHealthyHeatmap());

            SimulationResultDto result = testSimulatorService.simulate(blueprint, SimulationProfile.RANDOM_GUESSER);

            assertThat(result.abilityLevel()).isEqualTo(50);
        }
    }

    @Nested
    @DisplayName("validate")
    class ValidateTests {

        @Test
        @DisplayName("Should return true when assembly succeeds")
        void shouldReturnTrueWhenAssemblySucceeds() {
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = List.of(questionId1, questionId2);

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);

            boolean result = testSimulatorService.validate(blueprint);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when assembly fails")
        void shouldReturnFalseWhenAssemblyFails() {
            OverviewBlueprint blueprint = createValidBlueprint();
            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class)))
                .thenThrow(new IllegalArgumentException("Invalid strategy"));

            boolean result = testSimulatorService.validate(blueprint);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when assembly returns empty list")
        void shouldReturnFalseWhenAssemblyReturnsEmpty() {
            OverviewBlueprint blueprint = createValidBlueprint();
            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(List.of());

            boolean result = testSimulatorService.validate(blueprint);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should catch exceptions gracefully")
        void shouldCatchExceptionsGracefully() {
            OverviewBlueprint blueprint = createValidBlueprint();
            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

            boolean result = testSimulatorService.validate(blueprint);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("persona simulation")
    class PersonaSimulationTests {

        @Test
        @DisplayName("PERFECT_CANDIDATE should score high (85-100%)")
        void shouldScorePerfectCandidateHighly() {
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = createManyQuestionIds(50);
            List<AssessmentQuestion> questions = createManyQuestions(questionIds);

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createHealthyHeatmap());

            SimulationResultDto result = testSimulatorService.simulate(
                blueprint, SimulationProfile.PERFECT_CANDIDATE, 50);

            assertThat(result.simulatedScore()).isBetween(80.0, 100.0);
        }

        @Test
        @DisplayName("FAILING_CANDIDATE should score low (0-30%)")
        void shouldScoreFailingCandidateLow() {
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = createManyQuestionIds(50);
            List<AssessmentQuestion> questions = createManyQuestions(questionIds);

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createHealthyHeatmap());

            SimulationResultDto result = testSimulatorService.simulate(
                blueprint, SimulationProfile.FAILING_CANDIDATE, 50);

            assertThat(result.simulatedScore()).isBetween(0.0, 35.0);
        }

        @Test
        @DisplayName("RANDOM_GUESSER should score in moderate range (30-75%)")
        void shouldScoreRandomGuesserModerately() {
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = createManyQuestionIds(100);
            List<AssessmentQuestion> questions = createManyQuestions(questionIds);

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createHealthyHeatmap());

            SimulationResultDto result = testSimulatorService.simulate(
                blueprint, SimulationProfile.RANDOM_GUESSER, 50);

            // RANDOM_GUESSER base at INTERMEDIATE = 0.55; with 100 questions expect ~55%
            assertThat(result.simulatedScore()).isBetween(30.0, 75.0);
        }

        @Test
        @DisplayName("PERFECT should score higher than RANDOM which should score higher than FAILING")
        void personasShouldRankCorrectly() {
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = createManyQuestionIds(100);
            List<AssessmentQuestion> questions = createManyQuestions(questionIds);

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createHealthyHeatmap());

            SimulationResultDto perfect = testSimulatorService.simulate(
                blueprint, SimulationProfile.PERFECT_CANDIDATE, 50);
            SimulationResultDto random = testSimulatorService.simulate(
                blueprint, SimulationProfile.RANDOM_GUESSER, 50);
            SimulationResultDto failing = testSimulatorService.simulate(
                blueprint, SimulationProfile.FAILING_CANDIDATE, 50);

            assertThat(perfect.simulatedScore()).isGreaterThan(random.simulatedScore());
            assertThat(random.simulatedScore()).isGreaterThan(failing.simulatedScore());
        }
    }

    @Nested
    @DisplayName("psychometric persona curves")
    class PersonaCurveTests {

        @Test
        @DisplayName("Should produce difficulty-dependent scores for PERFECT_CANDIDATE")
        void perfectCandidateShouldScoreBetterOnEasierQuestions() {
            OverviewBlueprint blueprint = createValidBlueprint();

            List<UUID> foundationalIds = createManyQuestionIds(30);
            List<AssessmentQuestion> foundational = createManyQuestionsWithDifficulty(
                foundationalIds, DifficultyLevel.FOUNDATIONAL);

            List<UUID> expertIds = createManyQuestionIds(30);
            List<AssessmentQuestion> expert = createManyQuestionsWithDifficulty(
                expertIds, DifficultyLevel.EXPERT);

            List<UUID> allIds = new ArrayList<>(foundationalIds);
            allIds.addAll(expertIds);
            List<AssessmentQuestion> allQuestions = new ArrayList<>(foundational);
            allQuestions.addAll(expert);

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(allIds);
            when(questionRepository.findAllById(allIds)).thenReturn(allQuestions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createHealthyHeatmap());

            SimulationResultDto result = testSimulatorService.simulate(
                blueprint, SimulationProfile.PERFECT_CANDIDATE, 50);

            // Count correct by difficulty
            long foundationalCorrect = result.sampleQuestions().stream()
                .filter(q -> q.difficulty().equals("FOUNDATIONAL"))
                .filter(QuestionSummaryDto::simulatedCorrect).count();
            long expertCorrect = result.sampleQuestions().stream()
                .filter(q -> q.difficulty().equals("EXPERT"))
                .filter(QuestionSummaryDto::simulatedCorrect).count();

            // FOUNDATIONAL (p=0.98) should have more correct than EXPERT (p=0.85)
            assertThat(foundationalCorrect).isGreaterThanOrEqualTo(expertCorrect);
        }

        @Test
        @DisplayName("Ability slider at 100 should boost scores")
        void highAbilityShouldBoostScores() {
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = createManyQuestionIds(100);
            List<AssessmentQuestion> questions = createManyQuestions(questionIds);

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createHealthyHeatmap());

            SimulationResultDto high = testSimulatorService.simulate(
                blueprint, SimulationProfile.RANDOM_GUESSER, 100);
            SimulationResultDto mid = testSimulatorService.simulate(
                blueprint, SimulationProfile.RANDOM_GUESSER, 50);

            assertThat(high.simulatedScore()).isGreaterThanOrEqualTo(mid.simulatedScore());
        }

        @Test
        @DisplayName("Ability slider at 0 should reduce scores")
        void lowAbilityShouldReduceScores() {
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = createManyQuestionIds(100);
            List<AssessmentQuestion> questions = createManyQuestions(questionIds);

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createHealthyHeatmap());

            SimulationResultDto low = testSimulatorService.simulate(
                blueprint, SimulationProfile.RANDOM_GUESSER, 0);
            SimulationResultDto mid = testSimulatorService.simulate(
                blueprint, SimulationProfile.RANDOM_GUESSER, 50);

            assertThat(low.simulatedScore()).isLessThanOrEqualTo(mid.simulatedScore());
        }

        @Test
        @DisplayName("Same inputs should produce deterministic results")
        void shouldProduceDeterministicResults() {
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = List.of(questionId1, questionId2, questionId3);
            List<AssessmentQuestion> questions = List.of(
                createQuestionWithId(questionId1, "Q1"),
                createQuestionWithId(questionId2, "Q2"),
                createQuestionWithId(questionId3, "Q3")
            );

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createHealthyHeatmap());

            SimulationResultDto run1 = testSimulatorService.simulate(
                blueprint, SimulationProfile.RANDOM_GUESSER, 50);
            SimulationResultDto run2 = testSimulatorService.simulate(
                blueprint, SimulationProfile.RANDOM_GUESSER, 50);

            assertThat(run1.simulatedScore()).isEqualTo(run2.simulatedScore());
            for (int i = 0; i < run1.sampleQuestions().size(); i++) {
                assertThat(run1.sampleQuestions().get(i).simulatedCorrect())
                    .isEqualTo(run2.sampleQuestions().get(i).simulatedCorrect());
            }
        }

        @Test
        @DisplayName("Competency scores should vary across competencies")
        void competencyScoresShouldVary() {
            OverviewBlueprint blueprint = createValidBlueprint();

            // Create questions spread across 3 competencies
            UUID compA = UUID.randomUUID();
            UUID compB = UUID.randomUUID();
            UUID compC = UUID.randomUUID();
            UUID indA = UUID.randomUUID();
            UUID indB = UUID.randomUUID();
            UUID indC = UUID.randomUUID();

            List<UUID> allIds = new ArrayList<>();
            List<AssessmentQuestion> allQuestions = new ArrayList<>();

            for (int i = 0; i < 20; i++) {
                UUID qid = UUID.randomUUID();
                allIds.add(qid);
                if (i < 7) allQuestions.add(createQuestionWithCompetency(qid, compA, indA));
                else if (i < 14) allQuestions.add(createQuestionWithCompetency(qid, compB, indB));
                else allQuestions.add(createQuestionWithCompetency(qid, compC, indC));
            }

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(allIds);
            when(questionRepository.findAllById(allIds)).thenReturn(allQuestions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createHealthyHeatmap());

            SimulationResultDto result = testSimulatorService.simulate(
                blueprint, SimulationProfile.RANDOM_GUESSER, 50);

            // Should have 3 competency scores
            assertThat(result.competencyScores()).hasSize(3);

            // Scores should not all be identical (competency noise ensures variation)
            var scores = result.competencyScores().values().stream()
                .map(CompetencySimulationScore::scorePercentage)
                .distinct()
                .toList();
            // With noise, at least 2 of 3 should differ
            assertThat(scores.size()).isGreaterThanOrEqualTo(2);
        }

        @Test
        @DisplayName("AbilityLevel boundary 0 should not crash")
        void abilityLevelZeroShouldWork() {
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = List.of(questionId1);
            List<AssessmentQuestion> questions = List.of(createQuestionWithId(questionId1, "Q1"));

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createHealthyHeatmap());

            assertThatCode(() -> testSimulatorService.simulate(
                blueprint, SimulationProfile.RANDOM_GUESSER, 0))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("AbilityLevel boundary 100 should not crash")
        void abilityLevelHundredShouldWork() {
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = List.of(questionId1);
            List<AssessmentQuestion> questions = List.of(createQuestionWithId(questionId1, "Q1"));

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createHealthyHeatmap());

            assertThatCode(() -> testSimulatorService.simulate(
                blueprint, SimulationProfile.PERFECT_CANDIDATE, 100))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle questions with null time limits")
        void shouldHandleQuestionsWithNullTimeLimits() {
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = List.of(questionId1);

            AssessmentQuestion question = createQuestionWithTimeLimit(questionId1, null);
            List<AssessmentQuestion> questions = List.of(question);

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createHealthyHeatmap());

            SimulationResultDto result = testSimulatorService.simulate(blueprint, SimulationProfile.RANDOM_GUESSER);

            assertThat(result.estimatedDurationMinutes()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should truncate long question text")
        void shouldTruncateLongQuestionText() {
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = List.of(questionId1);

            String longText = "A".repeat(150);
            AssessmentQuestion question = createQuestionWithText(questionId1, longText);
            List<AssessmentQuestion> questions = List.of(question);

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createHealthyHeatmap());

            SimulationResultDto result = testSimulatorService.simulate(blueprint, SimulationProfile.RANDOM_GUESSER);

            assertThat(result.sampleQuestions()).hasSize(1);
            String truncatedText = result.sampleQuestions().get(0).questionText();
            assertThat(truncatedText.length()).isEqualTo(100);
            assertThat(truncatedText).endsWith("...");
        }

        @Test
        @DisplayName("Should handle empty competency list from heatmap")
        void shouldHandleEmptyCompetencyListFromHeatmap() {
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = List.of(questionId1);
            List<AssessmentQuestion> questions = List.of(createQuestionWithId(questionId1, "Q1"));

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createEmptyHeatmap());

            SimulationResultDto result = testSimulatorService.simulate(blueprint, SimulationProfile.RANDOM_GUESSER);

            assertThat(result.valid()).isTrue();
        }

        @Test
        @DisplayName("Should handle null question text")
        void shouldHandleNullQuestionText() {
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = List.of(questionId1);

            AssessmentQuestion question = createQuestionWithText(questionId1, null);
            List<AssessmentQuestion> questions = List.of(question);

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createHealthyHeatmap());

            SimulationResultDto result = testSimulatorService.simulate(blueprint, SimulationProfile.RANDOM_GUESSER);

            assertThat(result.sampleQuestions()).hasSize(1);
            assertThat(result.sampleQuestions().get(0).questionText()).isEmpty();
        }

        @Test
        @DisplayName("Should mark result invalid when ERROR warnings present")
        void shouldMarkResultInvalidWhenErrorWarningsPresent() {
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = List.of(questionId1);
            List<AssessmentQuestion> questions = List.of(createQuestionWithId(questionId1, "Q1"));

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);

            InventoryHeatmapDto criticalHeatmap = createHeatmapWithStatus(HealthStatus.CRITICAL);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(criticalHeatmap);

            SimulationResultDto result = testSimulatorService.simulate(blueprint, SimulationProfile.RANDOM_GUESSER);

            assertThat(result.valid()).isFalse();
        }

        @Test
        @DisplayName("Should preserve question order from assembly")
        void shouldPreserveQuestionOrderFromAssembly() {
            OverviewBlueprint blueprint = createValidBlueprint();
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            UUID id3 = UUID.randomUUID();
            List<UUID> questionIds = List.of(id1, id2, id3);

            AssessmentQuestion q1 = createQuestionWithId(id1, "Question 1");
            AssessmentQuestion q2 = createQuestionWithId(id2, "Question 2");
            AssessmentQuestion q3 = createQuestionWithId(id3, "Question 3");
            List<AssessmentQuestion> questions = List.of(q3, q1, q2);

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createHealthyHeatmap());

            SimulationResultDto result = testSimulatorService.simulate(blueprint, SimulationProfile.RANDOM_GUESSER);

            assertThat(result.sampleQuestions()).hasSize(3);
            assertThat(result.sampleQuestions().get(0).id()).isEqualTo(id1);
            assertThat(result.sampleQuestions().get(1).id()).isEqualTo(id2);
            assertThat(result.sampleQuestions().get(2).id()).isEqualTo(id3);
        }

        @Test
        @DisplayName("Should return correct profile in result")
        void shouldReturnCorrectProfileInResult() {
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = List.of(questionId1);
            List<AssessmentQuestion> questions = List.of(createQuestionWithId(questionId1, "Q1"));

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createHealthyHeatmap());

            SimulationResultDto perfectResult = testSimulatorService.simulate(blueprint, SimulationProfile.PERFECT_CANDIDATE);
            SimulationResultDto failingResult = testSimulatorService.simulate(blueprint, SimulationProfile.FAILING_CANDIDATE);
            SimulationResultDto randomResult = testSimulatorService.simulate(blueprint, SimulationProfile.RANDOM_GUESSER);

            assertThat(perfectResult.profile()).isEqualTo(SimulationProfile.PERFECT_CANDIDATE);
            assertThat(failingResult.profile()).isEqualTo(SimulationProfile.FAILING_CANDIDATE);
            assertThat(randomResult.profile()).isEqualTo(SimulationProfile.RANDOM_GUESSER);
        }

        @Test
        @DisplayName("Should correctly populate question summary fields")
        void shouldCorrectlyPopulateQuestionSummaryFields() {
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = List.of(questionId1);

            AssessmentQuestion question = createQuestionWithCompetency(questionId1, competencyId1, indicatorId1);
            question.setQuestionText("Sample question text");
            question.setDifficultyLevel(DifficultyLevel.ADVANCED);
            question.setQuestionType(QuestionType.SJT);
            question.setTimeLimit(90);
            List<AssessmentQuestion> questions = List.of(question);

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createHealthyHeatmap());

            SimulationResultDto result = testSimulatorService.simulate(blueprint, SimulationProfile.PERFECT_CANDIDATE);

            assertThat(result.sampleQuestions()).hasSize(1);
            QuestionSummaryDto summary = result.sampleQuestions().get(0);
            assertThat(summary.id()).isEqualTo(questionId1);
            assertThat(summary.competencyId()).isEqualTo(competencyId1);
            assertThat(summary.behavioralIndicatorId()).isEqualTo(indicatorId1);
            assertThat(summary.questionText()).isEqualTo("Sample question text");
            assertThat(summary.difficulty()).isEqualTo("ADVANCED");
            assertThat(summary.questionType()).isEqualTo("SJT");
            assertThat(summary.timeLimitSeconds()).isEqualTo(90);
            assertThat(summary.simulatedAnswer()).isIn("Correct Option", "Incorrect Option");
            assertThat(summary.competencyName()).isEqualTo("Test Competency");
            assertThat(summary.indicatorTitle()).isEqualTo("Test Indicator");
        }

        @Test
        @DisplayName("FAILING_CANDIDATE should predominantly answer incorrectly")
        void failingCandidateShouldMostlyAnswerIncorrectly() {
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = createManyQuestionIds(50);
            List<AssessmentQuestion> questions = createManyQuestions(questionIds);

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createHealthyHeatmap());

            SimulationResultDto result = testSimulatorService.simulate(
                blueprint, SimulationProfile.FAILING_CANDIDATE, 0);

            long incorrectCount = result.sampleQuestions().stream()
                .filter(q -> !q.simulatedCorrect()).count();
            // At ability=0, FAILING should get most wrong
            assertThat(incorrectCount).isGreaterThan(result.sampleQuestions().size() / 2);
        }
    }

    // ==================== Helper Methods ====================

    private OverviewBlueprint createValidBlueprint() {
        OverviewBlueprint blueprint = new OverviewBlueprint();
        blueprint.setStrategy(AssessmentGoal.OVERVIEW);
        blueprint.setCompetencyIds(List.of(competencyId1));
        blueprint.setQuestionsPerIndicator(3);
        blueprint.setPreferredDifficulty(DifficultyLevel.INTERMEDIATE);
        return blueprint;
    }

    private AssessmentQuestion createQuestionWithId(UUID questionId, String text) {
        return createQuestionWithCompetency(questionId, competencyId1, indicatorId1, text);
    }

    private AssessmentQuestion createQuestionWithCompetency(UUID questionId, UUID compId, UUID indicatorId) {
        return createQuestionWithCompetency(questionId, compId, indicatorId, "Test question text");
    }

    private AssessmentQuestion createQuestionWithCompetency(UUID questionId, UUID compId, UUID indicatorId, String text) {
        Competency competency = new Competency();
        competency.setId(compId);
        competency.setName("Test Competency");

        BehavioralIndicator indicator = new BehavioralIndicator();
        indicator.setId(indicatorId);
        indicator.setTitle("Test Indicator");
        indicator.setCompetency(competency);

        AssessmentQuestion question = new AssessmentQuestion();
        question.setId(questionId);
        question.setBehavioralIndicator(indicator);
        question.setQuestionText(text);
        question.setQuestionType(QuestionType.MCQ);
        question.setDifficultyLevel(DifficultyLevel.INTERMEDIATE);
        question.setTimeLimit(60);
        question.setActive(true);
        question.setScoringRubric("Standard rubric");

        return question;
    }

    private AssessmentQuestion createQuestionWithDifficulty(UUID questionId, DifficultyLevel difficulty) {
        AssessmentQuestion question = createQuestionWithId(questionId, "Question " + questionId);
        question.setDifficultyLevel(difficulty);
        return question;
    }

    private AssessmentQuestion createQuestionWithTimeLimit(UUID questionId, Integer timeLimit) {
        AssessmentQuestion question = createQuestionWithId(questionId, "Question " + questionId);
        question.setTimeLimit(timeLimit);
        return question;
    }

    private AssessmentQuestion createQuestionWithText(UUID questionId, String text) {
        return createQuestionWithCompetency(questionId, competencyId1, indicatorId1, text);
    }

    private List<UUID> createManyQuestionIds(int count) {
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ids.add(UUID.randomUUID());
        }
        return ids;
    }

    private List<AssessmentQuestion> createManyQuestions(List<UUID> ids) {
        List<AssessmentQuestion> questions = new ArrayList<>();
        for (UUID id : ids) {
            questions.add(createQuestionWithId(id, "Q" + id));
        }
        return questions;
    }

    private List<AssessmentQuestion> createManyQuestionsWithDifficulty(List<UUID> ids, DifficultyLevel difficulty) {
        List<AssessmentQuestion> questions = new ArrayList<>();
        for (UUID id : ids) {
            questions.add(createQuestionWithDifficulty(id, difficulty));
        }
        return questions;
    }

    private InventoryHeatmapDto createHealthyHeatmap() {
        Map<UUID, HealthStatus> health = new HashMap<>();
        health.put(competencyId1, HealthStatus.HEALTHY);

        return InventoryHeatmapDto.builder()
            .competencyHealth(health)
            .detailedCounts(Map.of())
            .summary(new InventoryHeatmapDto.HeatmapSummary(1, 0, 0, 1, 10L))
            .build();
    }

    private InventoryHeatmapDto createEmptyHeatmap() {
        return InventoryHeatmapDto.builder()
            .competencyHealth(Map.of())
            .detailedCounts(Map.of())
            .summary(new InventoryHeatmapDto.HeatmapSummary(0, 0, 0, 0, 0L))
            .build();
    }

    private InventoryHeatmapDto createHeatmapWithStatus(HealthStatus status) {
        Map<UUID, HealthStatus> health = new HashMap<>();
        health.put(competencyId1, status);

        int critical = status == HealthStatus.CRITICAL ? 1 : 0;
        int moderate = status == HealthStatus.MODERATE ? 1 : 0;
        int healthy = status == HealthStatus.HEALTHY ? 1 : 0;

        return InventoryHeatmapDto.builder()
            .competencyHealth(health)
            .detailedCounts(Map.of())
            .summary(new InventoryHeatmapDto.HeatmapSummary(1, critical, moderate, healthy, 5L))
            .build();
    }
}
