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

/**
 * Unit tests for TestSimulatorService.
 *
 * Tests verify:
 * - Simulation execution with different profiles (PERFECT, RANDOM, FAILING)
 * - Blueprint validation logic
 * - Question hydration and composition calculation
 * - Duration estimation and score calculation
 * - Warning generation for inventory health issues
 * - Edge cases (null inputs, empty results, truncation)
 */
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

        // Initialize test UUIDs
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
            // When
            SimulationResultDto result = testSimulatorService.simulate(null, SimulationProfile.RANDOM_GUESSER);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.valid()).isFalse();
            assertThat(result.totalQuestions()).isZero();
            assertThat(result.warnings()).hasSize(1);
            assertThat(result.warnings().get(0).message()).contains("Blueprint is null");
        }

        @Test
        @DisplayName("Should default profile to RANDOM_GUESSER when null")
        void shouldDefaultProfileToRandomGuesser() {
            // Given
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = List.of(questionId1);
            List<AssessmentQuestion> questions = List.of(createQuestionWithId(questionId1, "Q1"));

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createHealthyHeatmap());

            // When
            SimulationResultDto result = testSimulatorService.simulate(blueprint, null);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.profile()).isEqualTo(SimulationProfile.RANDOM_GUESSER);
        }

        @Test
        @DisplayName("Should return failed result when assembly fails")
        void shouldReturnFailedResultWhenAssemblyFails() {
            // Given
            OverviewBlueprint blueprint = createValidBlueprint();
            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class)))
                .thenThrow(new IllegalArgumentException("No assembler for strategy"));

            // When
            SimulationResultDto result = testSimulatorService.simulate(blueprint, SimulationProfile.RANDOM_GUESSER);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.valid()).isFalse();
            assertThat(result.warnings()).hasSize(1);
            assertThat(result.warnings().get(0).message()).contains("Assembly failed");
        }

        @Test
        @DisplayName("Should return failed result when no questions assembled")
        void shouldReturnFailedResultWhenNoQuestionsAssembled() {
            // Given
            OverviewBlueprint blueprint = createValidBlueprint();
            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(List.of());

            // When
            SimulationResultDto result = testSimulatorService.simulate(blueprint, SimulationProfile.RANDOM_GUESSER);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.valid()).isFalse();
            assertThat(result.totalQuestions()).isZero();
            assertThat(result.warnings()).anyMatch(w ->
                w.message().contains("No questions assembled"));
        }

        @Test
        @DisplayName("Should hydrate questions correctly")
        void shouldHydrateQuestionsCorrectly() {
            // Given
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

            // When
            SimulationResultDto result = testSimulatorService.simulate(blueprint, SimulationProfile.RANDOM_GUESSER);

            // Then
            assertThat(result.totalQuestions()).isEqualTo(3);
            assertThat(result.sampleQuestions()).hasSize(3);
            verify(questionRepository).findAllById(questionIds);
        }

        @Test
        @DisplayName("Should extract unique competencies")
        void shouldExtractUniqueCompetencies() {
            // Given
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = List.of(questionId1, questionId2);

            // Create questions with different competencies
            AssessmentQuestion q1 = createQuestionWithCompetency(questionId1, competencyId1, indicatorId1);
            AssessmentQuestion q2 = createQuestionWithCompetency(questionId2, competencyId2, indicatorId2);
            List<AssessmentQuestion> questions = List.of(q1, q2);

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createHealthyHeatmap());

            // When
            testSimulatorService.simulate(blueprint, SimulationProfile.RANDOM_GUESSER);

            // Then - verify heatmap service was called with unique competency IDs
            verify(inventoryHeatmapService).generateHeatmapFor(argThat(ids ->
                ids.size() == 2 && ids.contains(competencyId1) && ids.contains(competencyId2)
            ));
        }

        @Test
        @DisplayName("Should add critical warnings for critical competencies")
        void shouldAddCriticalWarningsForCriticalCompetencies() {
            // Given
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = List.of(questionId1);
            List<AssessmentQuestion> questions = List.of(createQuestionWithId(questionId1, "Q1"));

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);

            // Return heatmap with CRITICAL health status
            InventoryHeatmapDto criticalHeatmap = createHeatmapWithStatus(HealthStatus.CRITICAL);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(criticalHeatmap);

            // When
            SimulationResultDto result = testSimulatorService.simulate(blueprint, SimulationProfile.RANDOM_GUESSER);

            // Then
            assertThat(result.warnings()).anyMatch(w ->
                w.level() == InventoryWarning.WarningLevel.ERROR);
        }

        @Test
        @DisplayName("Should add moderate warnings for moderate competencies")
        void shouldAddModerateWarningsForModerateCompetencies() {
            // Given
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = List.of(questionId1);
            List<AssessmentQuestion> questions = List.of(createQuestionWithId(questionId1, "Q1"));

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);

            // Return heatmap with MODERATE health status
            InventoryHeatmapDto moderateHeatmap = createHeatmapWithStatus(HealthStatus.MODERATE);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(moderateHeatmap);

            // When
            SimulationResultDto result = testSimulatorService.simulate(blueprint, SimulationProfile.RANDOM_GUESSER);

            // Then
            assertThat(result.warnings()).anyMatch(w ->
                w.level() == InventoryWarning.WarningLevel.WARNING);
        }

        @Test
        @DisplayName("Should run persona simulation")
        void shouldRunPersonaSimulation() {
            // Given
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

            // When
            SimulationResultDto result = testSimulatorService.simulate(blueprint, SimulationProfile.PERFECT_CANDIDATE);

            // Then
            assertThat(result.sampleQuestions()).hasSize(2);
            assertThat(result.sampleQuestions()).allMatch(QuestionSummaryDto::simulatedCorrect);
        }

        @Test
        @DisplayName("Should calculate composition by difficulty")
        void shouldCalculateCompositionByDifficulty() {
            // Given
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = List.of(questionId1, questionId2, questionId3);

            // Create questions with different difficulties
            List<AssessmentQuestion> questions = List.of(
                createQuestionWithDifficulty(questionId1, DifficultyLevel.FOUNDATIONAL),
                createQuestionWithDifficulty(questionId2, DifficultyLevel.INTERMEDIATE),
                createQuestionWithDifficulty(questionId3, DifficultyLevel.INTERMEDIATE)
            );

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createHealthyHeatmap());

            // When
            SimulationResultDto result = testSimulatorService.simulate(blueprint, SimulationProfile.RANDOM_GUESSER);

            // Then
            assertThat(result.composition()).containsEntry("FOUNDATIONAL", 1);
            assertThat(result.composition()).containsEntry("INTERMEDIATE", 2);
        }

        @Test
        @DisplayName("Should calculate estimated duration in minutes")
        void shouldCalculateEstimatedDurationInMinutes() {
            // Given
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = List.of(questionId1, questionId2);

            // Create questions with time limits
            AssessmentQuestion q1 = createQuestionWithTimeLimit(questionId1, 120); // 2 minutes
            AssessmentQuestion q2 = createQuestionWithTimeLimit(questionId2, 180); // 3 minutes
            List<AssessmentQuestion> questions = List.of(q1, q2);

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createHealthyHeatmap());

            // When
            SimulationResultDto result = testSimulatorService.simulate(blueprint, SimulationProfile.RANDOM_GUESSER);

            // Then - 120 + 180 = 300 seconds = 5 minutes
            assertThat(result.estimatedDurationMinutes()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should calculate simulated score as percentage")
        void shouldCalculateSimulatedScoreAsPercentage() {
            // Given
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

            // When - PERFECT_CANDIDATE should get 100%
            SimulationResultDto result = testSimulatorService.simulate(blueprint, SimulationProfile.PERFECT_CANDIDATE);

            // Then
            assertThat(result.simulatedScore()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("Should mark result valid when no error warnings")
        void shouldMarkResultValidWhenNoErrorWarnings() {
            // Given
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = List.of(questionId1);
            List<AssessmentQuestion> questions = List.of(createQuestionWithId(questionId1, "Q1"));

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createHealthyHeatmap());

            // When
            SimulationResultDto result = testSimulatorService.simulate(blueprint, SimulationProfile.RANDOM_GUESSER);

            // Then
            assertThat(result.valid()).isTrue();
        }

        @Test
        @DisplayName("Should add warning when not all questions could be loaded")
        void shouldAddWarningWhenNotAllQuestionsLoaded() {
            // Given
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = List.of(questionId1, questionId2, questionId3);
            // Only return 2 of 3 questions (missing questionId3)
            List<AssessmentQuestion> questions = List.of(
                createQuestionWithId(questionId1, "Q1"),
                createQuestionWithId(questionId2, "Q2")
            );

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createHealthyHeatmap());

            // When
            SimulationResultDto result = testSimulatorService.simulate(blueprint, SimulationProfile.RANDOM_GUESSER);

            // Then
            assertThat(result.warnings()).anyMatch(w ->
                w.message().contains("Only 2 of 3 questions could be loaded"));
        }
    }

    @Nested
    @DisplayName("validate")
    class ValidateTests {

        @Test
        @DisplayName("Should return true when assembly succeeds")
        void shouldReturnTrueWhenAssemblySucceeds() {
            // Given
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = List.of(questionId1, questionId2);

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);

            // When
            boolean result = testSimulatorService.validate(blueprint);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when assembly fails")
        void shouldReturnFalseWhenAssemblyFails() {
            // Given
            OverviewBlueprint blueprint = createValidBlueprint();
            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class)))
                .thenThrow(new IllegalArgumentException("Invalid strategy"));

            // When
            boolean result = testSimulatorService.validate(blueprint);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when assembly returns empty list")
        void shouldReturnFalseWhenAssemblyReturnsEmpty() {
            // Given
            OverviewBlueprint blueprint = createValidBlueprint();
            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(List.of());

            // When
            boolean result = testSimulatorService.validate(blueprint);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should catch exceptions gracefully")
        void shouldCatchExceptionsGracefully() {
            // Given
            OverviewBlueprint blueprint = createValidBlueprint();
            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

            // When
            boolean result = testSimulatorService.validate(blueprint);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("persona simulation")
    class PersonaSimulationTests {

        @Test
        @DisplayName("Should score PERFECT_CANDIDATE at 100 percent")
        void shouldScorePerfectCandidateAt100Percent() {
            // Given
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

            // When
            SimulationResultDto result = testSimulatorService.simulate(blueprint, SimulationProfile.PERFECT_CANDIDATE);

            // Then
            assertThat(result.simulatedScore()).isEqualTo(100.0);
            assertThat(result.sampleQuestions()).allMatch(QuestionSummaryDto::simulatedCorrect);
        }

        @Test
        @DisplayName("Should score FAILING_CANDIDATE at 0 percent")
        void shouldScoreFailingCandidateAt0Percent() {
            // Given
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

            // When
            SimulationResultDto result = testSimulatorService.simulate(blueprint, SimulationProfile.FAILING_CANDIDATE);

            // Then
            assertThat(result.simulatedScore()).isEqualTo(0.0);
            assertThat(result.sampleQuestions()).noneMatch(QuestionSummaryDto::simulatedCorrect);
        }

        @Test
        @DisplayName("Should score RANDOM_GUESSER around 50 percent (statistical)")
        void shouldScoreRandomGuesserAroundFiftyPercent() {
            // Given
            OverviewBlueprint blueprint = createValidBlueprint();

            // Use larger sample for statistical validity
            List<UUID> questionIds = new ArrayList<>();
            List<AssessmentQuestion> questions = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                UUID qId = UUID.randomUUID();
                questionIds.add(qId);
                questions.add(createQuestionWithId(qId, "Q" + i));
            }

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createHealthyHeatmap());

            // When - run multiple simulations
            double totalScore = 0;
            int runs = 10;
            for (int i = 0; i < runs; i++) {
                SimulationResultDto result = testSimulatorService.simulate(blueprint, SimulationProfile.RANDOM_GUESSER);
                totalScore += result.simulatedScore();
            }
            double averageScore = totalScore / runs;

            // Then - average should be around 50% (within reasonable margin)
            assertThat(averageScore).isBetween(30.0, 70.0);
        }
    }

    @Nested
    @DisplayName("edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle questions with null time limits")
        void shouldHandleQuestionsWithNullTimeLimits() {
            // Given
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = List.of(questionId1);

            // Create question with null time limit
            AssessmentQuestion question = createQuestionWithTimeLimit(questionId1, null);
            List<AssessmentQuestion> questions = List.of(question);

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createHealthyHeatmap());

            // When
            SimulationResultDto result = testSimulatorService.simulate(blueprint, SimulationProfile.RANDOM_GUESSER);

            // Then - should use default time (60 seconds = 1 minute)
            assertThat(result.estimatedDurationMinutes()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should truncate long question text")
        void shouldTruncateLongQuestionText() {
            // Given
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = List.of(questionId1);

            // Create question with very long text (> 100 chars)
            String longText = "A".repeat(150);
            AssessmentQuestion question = createQuestionWithText(questionId1, longText);
            List<AssessmentQuestion> questions = List.of(question);

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createHealthyHeatmap());

            // When
            SimulationResultDto result = testSimulatorService.simulate(blueprint, SimulationProfile.RANDOM_GUESSER);

            // Then - text should be truncated to 100 chars with "..."
            assertThat(result.sampleQuestions()).hasSize(1);
            String truncatedText = result.sampleQuestions().get(0).questionText();
            assertThat(truncatedText.length()).isEqualTo(100);
            assertThat(truncatedText).endsWith("...");
        }

        @Test
        @DisplayName("Should handle empty competency list from heatmap")
        void shouldHandleEmptyCompetencyListFromHeatmap() {
            // Given
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = List.of(questionId1);
            List<AssessmentQuestion> questions = List.of(createQuestionWithId(questionId1, "Q1"));

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);
            // Return empty heatmap
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createEmptyHeatmap());

            // When
            SimulationResultDto result = testSimulatorService.simulate(blueprint, SimulationProfile.RANDOM_GUESSER);

            // Then
            assertThat(result.valid()).isTrue();
        }

        @Test
        @DisplayName("Should handle null question text")
        void shouldHandleNullQuestionText() {
            // Given
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = List.of(questionId1);

            // Create question with null text
            AssessmentQuestion question = createQuestionWithText(questionId1, null);
            List<AssessmentQuestion> questions = List.of(question);

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createHealthyHeatmap());

            // When
            SimulationResultDto result = testSimulatorService.simulate(blueprint, SimulationProfile.RANDOM_GUESSER);

            // Then - should handle gracefully with empty string
            assertThat(result.sampleQuestions()).hasSize(1);
            assertThat(result.sampleQuestions().get(0).questionText()).isEmpty();
        }

        @Test
        @DisplayName("Should mark result invalid when ERROR warnings present")
        void shouldMarkResultInvalidWhenErrorWarningsPresent() {
            // Given
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = List.of(questionId1);
            List<AssessmentQuestion> questions = List.of(createQuestionWithId(questionId1, "Q1"));

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);

            // Return heatmap with CRITICAL (ERROR level) status
            InventoryHeatmapDto criticalHeatmap = createHeatmapWithStatus(HealthStatus.CRITICAL);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(criticalHeatmap);

            // When
            SimulationResultDto result = testSimulatorService.simulate(blueprint, SimulationProfile.RANDOM_GUESSER);

            // Then
            assertThat(result.valid()).isFalse();
        }

        @Test
        @DisplayName("Should preserve question order from assembly")
        void shouldPreserveQuestionOrderFromAssembly() {
            // Given
            OverviewBlueprint blueprint = createValidBlueprint();
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            UUID id3 = UUID.randomUUID();
            List<UUID> questionIds = List.of(id1, id2, id3);

            AssessmentQuestion q1 = createQuestionWithId(id1, "Question 1");
            AssessmentQuestion q2 = createQuestionWithId(id2, "Question 2");
            AssessmentQuestion q3 = createQuestionWithId(id3, "Question 3");
            // Return questions in different order
            List<AssessmentQuestion> questions = List.of(q3, q1, q2);

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createHealthyHeatmap());

            // When
            SimulationResultDto result = testSimulatorService.simulate(blueprint, SimulationProfile.RANDOM_GUESSER);

            // Then - order should match questionIds order, not repository return order
            assertThat(result.sampleQuestions()).hasSize(3);
            assertThat(result.sampleQuestions().get(0).id()).isEqualTo(id1);
            assertThat(result.sampleQuestions().get(1).id()).isEqualTo(id2);
            assertThat(result.sampleQuestions().get(2).id()).isEqualTo(id3);
        }

        @Test
        @DisplayName("Should return correct profile in result")
        void shouldReturnCorrectProfileInResult() {
            // Given
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = List.of(questionId1);
            List<AssessmentQuestion> questions = List.of(createQuestionWithId(questionId1, "Q1"));

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createHealthyHeatmap());

            // When - test each profile
            SimulationResultDto perfectResult = testSimulatorService.simulate(blueprint, SimulationProfile.PERFECT_CANDIDATE);
            SimulationResultDto failingResult = testSimulatorService.simulate(blueprint, SimulationProfile.FAILING_CANDIDATE);
            SimulationResultDto randomResult = testSimulatorService.simulate(blueprint, SimulationProfile.RANDOM_GUESSER);

            // Then
            assertThat(perfectResult.profile()).isEqualTo(SimulationProfile.PERFECT_CANDIDATE);
            assertThat(failingResult.profile()).isEqualTo(SimulationProfile.FAILING_CANDIDATE);
            assertThat(randomResult.profile()).isEqualTo(SimulationProfile.RANDOM_GUESSER);
        }

        @Test
        @DisplayName("Should correctly populate question summary fields")
        void shouldCorrectlyPopulateQuestionSummaryFields() {
            // Given
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

            // When
            SimulationResultDto result = testSimulatorService.simulate(blueprint, SimulationProfile.PERFECT_CANDIDATE);

            // Then
            assertThat(result.sampleQuestions()).hasSize(1);
            QuestionSummaryDto summary = result.sampleQuestions().get(0);
            assertThat(summary.id()).isEqualTo(questionId1);
            assertThat(summary.competencyId()).isEqualTo(competencyId1);
            assertThat(summary.behavioralIndicatorId()).isEqualTo(indicatorId1);
            assertThat(summary.questionText()).isEqualTo("Sample question text");
            assertThat(summary.difficulty()).isEqualTo("ADVANCED");
            assertThat(summary.questionType()).isEqualTo("SJT");
            assertThat(summary.timeLimitSeconds()).isEqualTo(90);
            assertThat(summary.simulatedCorrect()).isTrue();
            assertThat(summary.simulatedAnswer()).isEqualTo("Correct Option");
        }

        @Test
        @DisplayName("Should set simulated answer to Incorrect Option for failing candidate")
        void shouldSetSimulatedAnswerToIncorrectOptionForFailingCandidate() {
            // Given
            OverviewBlueprint blueprint = createValidBlueprint();
            List<UUID> questionIds = List.of(questionId1);
            List<AssessmentQuestion> questions = List.of(createQuestionWithId(questionId1, "Q1"));

            when(assemblerFactory.getAssembler(any(TestBlueprintDto.class))).thenReturn(mockAssembler);
            when(mockAssembler.assemble(any())).thenReturn(questionIds);
            when(questionRepository.findAllById(questionIds)).thenReturn(questions);
            when(inventoryHeatmapService.generateHeatmapFor(any())).thenReturn(createHealthyHeatmap());

            // When
            SimulationResultDto result = testSimulatorService.simulate(blueprint, SimulationProfile.FAILING_CANDIDATE);

            // Then
            assertThat(result.sampleQuestions()).hasSize(1);
            assertThat(result.sampleQuestions().get(0).simulatedAnswer()).isEqualTo("Incorrect Option");
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
        AssessmentQuestion question = createQuestionWithCompetency(questionId, competencyId1, indicatorId1, text);
        return question;
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
