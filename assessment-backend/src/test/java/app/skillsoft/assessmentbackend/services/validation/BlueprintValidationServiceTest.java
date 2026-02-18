package app.skillsoft.assessmentbackend.services.validation;

import app.skillsoft.assessmentbackend.domain.dto.blueprint.JobFitBlueprint;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.OverviewBlueprint;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.TeamFitBlueprint;
import app.skillsoft.assessmentbackend.domain.dto.validation.BlueprintValidationResult;
import app.skillsoft.assessmentbackend.domain.dto.validation.ValidationIssue;
import app.skillsoft.assessmentbackend.domain.dto.validation.ValidationSeverity;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;
import app.skillsoft.assessmentbackend.domain.entities.TestTemplate;
import app.skillsoft.assessmentbackend.repository.AssessmentQuestionRepository;
import app.skillsoft.assessmentbackend.services.BlueprintConversionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BlueprintValidationServiceImpl.
 *
 * Tests verify:
 * - Null/missing template handling
 * - Template name validation
 * - Assessment goal validation
 * - Time limit validation (> 0)
 * - Passing score validation (0-100)
 * - Blueprint existence and strategy checks
 * - Competency presence checks
 * - Question inventory checks per competency
 * - Weight distribution warnings
 * - Low question count warnings
 * - Simulation vs publishing validation differences
 * - Backward-compatible error message extraction
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BlueprintValidationService Tests")
class BlueprintValidationServiceTest {

    @Mock
    private AssessmentQuestionRepository questionRepository;

    @Mock
    private BlueprintConversionService blueprintConversionService;

    @InjectMocks
    private BlueprintValidationServiceImpl validationService;

    private UUID competencyId1;
    private UUID competencyId2;
    private UUID competencyId3;

    @BeforeEach
    void setUp() {
        competencyId1 = UUID.randomUUID();
        competencyId2 = UUID.randomUUID();
        competencyId3 = UUID.randomUUID();
    }

    /**
     * Create a valid template with all required fields set.
     */
    private TestTemplate createValidTemplate(List<UUID> competencyIds) {
        TestTemplate template = new TestTemplate();
        template.setId(UUID.randomUUID());
        template.setName("Test Template");
        template.setGoal(AssessmentGoal.OVERVIEW);
        template.setTimeLimitMinutes(60);
        template.setPassingScore(70.0);

        OverviewBlueprint blueprint = new OverviewBlueprint();
        blueprint.setCompetencyIds(new ArrayList<>(competencyIds));
        template.setTypedBlueprint(blueprint);

        // Mock ensureTypedBlueprint to return true (blueprint already valid)
        when(blueprintConversionService.ensureTypedBlueprint(template)).thenReturn(true);

        return template;
    }

    @Nested
    @DisplayName("validateForPublishing")
    class ValidateForPublishingTests {

        @Test
        @DisplayName("Should return valid result for a fully configured template")
        void shouldReturnValidResultForFullyConfiguredTemplate() {
            // Given
            TestTemplate template = createValidTemplate(List.of(competencyId1, competencyId2));
            when(questionRepository.countActiveQuestionsForCompetency(competencyId1)).thenReturn(10L);
            when(questionRepository.countActiveQuestionsForCompetency(competencyId2)).thenReturn(8L);

            // When
            BlueprintValidationResult result = validationService.validateForPublishing(template);

            // Then
            assertThat(result.valid()).isTrue();
            assertThat(result.canPublish()).isTrue();
            assertThat(result.canSimulate()).isTrue();
            assertThat(result.errors()).isEmpty();
        }

        @Test
        @DisplayName("Should fail when template is null")
        void shouldFailWhenTemplateIsNull() {
            // When
            BlueprintValidationResult result = validationService.validateForPublishing(null);

            // Then
            assertThat(result.valid()).isFalse();
            assertThat(result.canPublish()).isFalse();
            assertThat(result.errors()).isNotEmpty();
            assertThat(result.errors())
                    .anyMatch(e -> "null-blueprint".equals(e.id()));
        }

        @Test
        @DisplayName("Should fail when template name is missing")
        void shouldFailWhenTemplateNameIsMissing() {
            // Given
            TestTemplate template = createValidTemplate(List.of(competencyId1));
            when(questionRepository.countActiveQuestionsForCompetency(competencyId1)).thenReturn(10L);
            template.setName(null);

            // When
            BlueprintValidationResult result = validationService.validateForPublishing(template);

            // Then
            assertThat(result.valid()).isFalse();
            assertThat(result.errors())
                    .anyMatch(e -> "no-template-name".equals(e.id()));
        }

        @Test
        @DisplayName("Should fail when template name is blank")
        void shouldFailWhenTemplateNameIsBlank() {
            // Given
            TestTemplate template = createValidTemplate(List.of(competencyId1));
            when(questionRepository.countActiveQuestionsForCompetency(competencyId1)).thenReturn(10L);
            template.setName("   ");

            // When
            BlueprintValidationResult result = validationService.validateForPublishing(template);

            // Then
            assertThat(result.valid()).isFalse();
            assertThat(result.errors())
                    .anyMatch(e -> "no-template-name".equals(e.id()));
        }

        @Test
        @DisplayName("Should fail when assessment goal is missing")
        void shouldFailWhenAssessmentGoalIsMissing() {
            // Given
            TestTemplate template = createValidTemplate(List.of(competencyId1));
            when(questionRepository.countActiveQuestionsForCompetency(competencyId1)).thenReturn(10L);
            template.setGoal(null);

            // When
            BlueprintValidationResult result = validationService.validateForPublishing(template);

            // Then
            assertThat(result.valid()).isFalse();
            assertThat(result.errors())
                    .anyMatch(e -> "no-assessment-goal".equals(e.id()));
        }

        @Test
        @DisplayName("Should fail when time limit is null")
        void shouldFailWhenTimeLimitIsNull() {
            // Given
            TestTemplate template = createValidTemplate(List.of(competencyId1));
            when(questionRepository.countActiveQuestionsForCompetency(competencyId1)).thenReturn(10L);
            template.setTimeLimitMinutes(null);

            // When
            BlueprintValidationResult result = validationService.validateForPublishing(template);

            // Then
            assertThat(result.valid()).isFalse();
            assertThat(result.errors())
                    .anyMatch(e -> "invalid-time-limit".equals(e.id()));
        }

        @Test
        @DisplayName("Should fail when time limit is zero")
        void shouldFailWhenTimeLimitIsZero() {
            // Given
            TestTemplate template = createValidTemplate(List.of(competencyId1));
            when(questionRepository.countActiveQuestionsForCompetency(competencyId1)).thenReturn(10L);
            template.setTimeLimitMinutes(0);

            // When
            BlueprintValidationResult result = validationService.validateForPublishing(template);

            // Then
            assertThat(result.valid()).isFalse();
            assertThat(result.errors())
                    .anyMatch(e -> "invalid-time-limit".equals(e.id()));
        }

        @Test
        @DisplayName("Should fail when time limit is negative")
        void shouldFailWhenTimeLimitIsNegative() {
            // Given
            TestTemplate template = createValidTemplate(List.of(competencyId1));
            when(questionRepository.countActiveQuestionsForCompetency(competencyId1)).thenReturn(10L);
            template.setTimeLimitMinutes(-10);

            // When
            BlueprintValidationResult result = validationService.validateForPublishing(template);

            // Then
            assertThat(result.valid()).isFalse();
            assertThat(result.errors())
                    .anyMatch(e -> "invalid-time-limit".equals(e.id()));
        }

        @Test
        @DisplayName("Should fail when passing score is null")
        void shouldFailWhenPassingScoreIsNull() {
            // Given
            TestTemplate template = createValidTemplate(List.of(competencyId1));
            when(questionRepository.countActiveQuestionsForCompetency(competencyId1)).thenReturn(10L);
            template.setPassingScore(null);

            // When
            BlueprintValidationResult result = validationService.validateForPublishing(template);

            // Then
            assertThat(result.valid()).isFalse();
            assertThat(result.errors())
                    .anyMatch(e -> "invalid-passing-score".equals(e.id()));
        }

        @Test
        @DisplayName("Should fail when passing score is negative")
        void shouldFailWhenPassingScoreIsNegative() {
            // Given
            TestTemplate template = createValidTemplate(List.of(competencyId1));
            when(questionRepository.countActiveQuestionsForCompetency(competencyId1)).thenReturn(10L);
            template.setPassingScore(-5.0);

            // When
            BlueprintValidationResult result = validationService.validateForPublishing(template);

            // Then
            assertThat(result.valid()).isFalse();
            assertThat(result.errors())
                    .anyMatch(e -> "invalid-passing-score".equals(e.id()));
        }

        @Test
        @DisplayName("Should fail when passing score exceeds 100")
        void shouldFailWhenPassingScoreExceeds100() {
            // Given
            TestTemplate template = createValidTemplate(List.of(competencyId1));
            when(questionRepository.countActiveQuestionsForCompetency(competencyId1)).thenReturn(10L);
            template.setPassingScore(105.0);

            // When
            BlueprintValidationResult result = validationService.validateForPublishing(template);

            // Then
            assertThat(result.valid()).isFalse();
            assertThat(result.errors())
                    .anyMatch(e -> "invalid-passing-score".equals(e.id()));
        }

        @Test
        @DisplayName("Should accept passing score at boundary value 0")
        void shouldAcceptPassingScoreAtZero() {
            // Given
            TestTemplate template = createValidTemplate(List.of(competencyId1, competencyId2));
            when(questionRepository.countActiveQuestionsForCompetency(competencyId1)).thenReturn(10L);
            when(questionRepository.countActiveQuestionsForCompetency(competencyId2)).thenReturn(10L);
            template.setPassingScore(0.0);

            // When
            BlueprintValidationResult result = validationService.validateForPublishing(template);

            // Then
            assertThat(result.errors())
                    .noneMatch(e -> "invalid-passing-score".equals(e.id()));
        }

        @Test
        @DisplayName("Should accept passing score at boundary value 100")
        void shouldAcceptPassingScoreAt100() {
            // Given
            TestTemplate template = createValidTemplate(List.of(competencyId1, competencyId2));
            when(questionRepository.countActiveQuestionsForCompetency(competencyId1)).thenReturn(10L);
            when(questionRepository.countActiveQuestionsForCompetency(competencyId2)).thenReturn(10L);
            template.setPassingScore(100.0);

            // When
            BlueprintValidationResult result = validationService.validateForPublishing(template);

            // Then
            assertThat(result.errors())
                    .noneMatch(e -> "invalid-passing-score".equals(e.id()));
        }

        @Test
        @DisplayName("Should collect multiple errors simultaneously")
        void shouldCollectMultipleErrorsSimultaneously() {
            // Given - template with many issues
            TestTemplate template = createValidTemplate(List.of(competencyId1));
            when(questionRepository.countActiveQuestionsForCompetency(competencyId1)).thenReturn(10L);
            template.setName(null);
            template.setGoal(null);
            template.setTimeLimitMinutes(0);
            template.setPassingScore(150.0);

            // When
            BlueprintValidationResult result = validationService.validateForPublishing(template);

            // Then
            assertThat(result.valid()).isFalse();
            assertThat(result.errors()).hasSizeGreaterThanOrEqualTo(4);
            assertThat(result.errors())
                    .anyMatch(e -> "no-template-name".equals(e.id()))
                    .anyMatch(e -> "no-assessment-goal".equals(e.id()))
                    .anyMatch(e -> "invalid-time-limit".equals(e.id()))
                    .anyMatch(e -> "invalid-passing-score".equals(e.id()));
        }
    }

    @Nested
    @DisplayName("Blueprint and Strategy Validation")
    class BlueprintValidationTests {

        @Test
        @DisplayName("Should fail when typed blueprint is null and conversion fails")
        void shouldFailWhenBlueprintIsNullAndConversionFails() {
            // Given
            TestTemplate template = new TestTemplate();
            template.setId(UUID.randomUUID());
            template.setName("Test");
            template.setGoal(AssessmentGoal.OVERVIEW);
            template.setTimeLimitMinutes(60);
            template.setPassingScore(70.0);
            // No typed blueprint, and conversion will fail
            when(blueprintConversionService.ensureTypedBlueprint(template)).thenReturn(false);

            // When
            BlueprintValidationResult result = validationService.validateForPublishing(template);

            // Then
            assertThat(result.valid()).isFalse();
            assertThat(result.errors())
                    .anyMatch(e -> "null-blueprint".equals(e.id()));
        }

        @Test
        @DisplayName("Should fail when blueprint has null strategy")
        void shouldFailWhenBlueprintHasNullStrategy() {
            // Given
            TestTemplate template = new TestTemplate();
            template.setId(UUID.randomUUID());
            template.setName("Test");
            template.setGoal(AssessmentGoal.OVERVIEW);
            template.setTimeLimitMinutes(60);
            template.setPassingScore(70.0);

            OverviewBlueprint blueprint = new OverviewBlueprint();
            blueprint.setStrategy(null);
            blueprint.setCompetencyIds(List.of(competencyId1));
            template.setTypedBlueprint(blueprint);

            when(blueprintConversionService.ensureTypedBlueprint(template)).thenReturn(true);
            when(questionRepository.countActiveQuestionsForCompetency(competencyId1)).thenReturn(10L);

            // When
            BlueprintValidationResult result = validationService.validateForPublishing(template);

            // Then
            assertThat(result.valid()).isFalse();
            assertThat(result.errors())
                    .anyMatch(e -> "no-strategy".equals(e.id()));
        }

        @Test
        @DisplayName("Should fail when blueprint has no competencies")
        void shouldFailWhenBlueprintHasNoCompetencies() {
            // Given
            TestTemplate template = createValidTemplate(List.of()); // empty competency list

            // When
            BlueprintValidationResult result = validationService.validateForPublishing(template);

            // Then
            assertThat(result.valid()).isFalse();
            assertThat(result.errors())
                    .anyMatch(e -> "no-competencies".equals(e.id()));
        }
    }

    @Nested
    @DisplayName("Question Inventory Validation")
    class QuestionInventoryTests {

        @Test
        @DisplayName("Should error when competency has zero questions")
        void shouldErrorWhenCompetencyHasZeroQuestions() {
            // Given
            TestTemplate template = createValidTemplate(List.of(competencyId1, competencyId2));
            when(questionRepository.countActiveQuestionsForCompetency(competencyId1)).thenReturn(10L);
            when(questionRepository.countActiveQuestionsForCompetency(competencyId2)).thenReturn(0L);

            // When
            BlueprintValidationResult result = validationService.validateForPublishing(template);

            // Then
            assertThat(result.valid()).isFalse();
            assertThat(result.errors())
                    .anyMatch(e -> "no-questions-for-competency".equals(e.id())
                            && competencyId2.toString().equals(e.competencyId()));
        }

        @Test
        @DisplayName("Should warn when competency has fewer than 3 questions")
        void shouldWarnWhenCompetencyHasLowQuestionCount() {
            // Given
            TestTemplate template = createValidTemplate(List.of(competencyId1, competencyId2));
            when(questionRepository.countActiveQuestionsForCompetency(competencyId1)).thenReturn(10L);
            when(questionRepository.countActiveQuestionsForCompetency(competencyId2)).thenReturn(2L);

            // When
            BlueprintValidationResult result = validationService.validateForPublishing(template);

            // Then
            assertThat(result.valid()).isTrue(); // warnings don't block
            assertThat(result.hasWarnings()).isTrue();
            assertThat(result.warnings())
                    .anyMatch(w -> "low-question-count".equals(w.id())
                            && competencyId2.toString().equals(w.competencyId()));
        }

        @Test
        @DisplayName("Should not warn when competency has exactly 3 questions")
        void shouldNotWarnWhenCompetencyHasExactlyThreeQuestions() {
            // Given
            TestTemplate template = createValidTemplate(List.of(competencyId1, competencyId2));
            when(questionRepository.countActiveQuestionsForCompetency(competencyId1)).thenReturn(10L);
            when(questionRepository.countActiveQuestionsForCompetency(competencyId2)).thenReturn(3L);

            // When
            BlueprintValidationResult result = validationService.validateForPublishing(template);

            // Then
            assertThat(result.warnings())
                    .noneMatch(w -> "low-question-count".equals(w.id())
                            && competencyId2.toString().equals(w.competencyId()));
        }

        @Test
        @DisplayName("Should check inventory for all competencies in blueprint")
        void shouldCheckInventoryForAllCompetencies() {
            // Given
            TestTemplate template = createValidTemplate(
                    List.of(competencyId1, competencyId2, competencyId3));
            when(questionRepository.countActiveQuestionsForCompetency(competencyId1)).thenReturn(10L);
            when(questionRepository.countActiveQuestionsForCompetency(competencyId2)).thenReturn(5L);
            when(questionRepository.countActiveQuestionsForCompetency(competencyId3)).thenReturn(8L);

            // When
            validationService.validateForPublishing(template);

            // Then
            verify(questionRepository).countActiveQuestionsForCompetency(competencyId1);
            verify(questionRepository).countActiveQuestionsForCompetency(competencyId2);
            verify(questionRepository).countActiveQuestionsForCompetency(competencyId3);
        }

        @Test
        @DisplayName("Should collect errors for multiple competencies with zero questions")
        void shouldCollectErrorsForMultipleCompetenciesWithZeroQuestions() {
            // Given
            TestTemplate template = createValidTemplate(
                    List.of(competencyId1, competencyId2, competencyId3));
            when(questionRepository.countActiveQuestionsForCompetency(competencyId1)).thenReturn(0L);
            when(questionRepository.countActiveQuestionsForCompetency(competencyId2)).thenReturn(0L);
            when(questionRepository.countActiveQuestionsForCompetency(competencyId3)).thenReturn(10L);

            // When
            BlueprintValidationResult result = validationService.validateForPublishing(template);

            // Then
            assertThat(result.valid()).isFalse();
            long noQuestionErrors = result.errors().stream()
                    .filter(e -> "no-questions-for-competency".equals(e.id()))
                    .count();
            assertThat(noQuestionErrors).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Weight Distribution Warnings")
    class WeightDistributionTests {

        @Test
        @DisplayName("Should warn when single competency carries all weight")
        void shouldWarnWhenSingleCompetencyCarriesAllWeight() {
            // Given
            TestTemplate template = createValidTemplate(List.of(competencyId1));
            when(questionRepository.countActiveQuestionsForCompetency(competencyId1)).thenReturn(10L);

            // When
            BlueprintValidationResult result = validationService.validateForPublishing(template);

            // Then
            assertThat(result.warnings())
                    .anyMatch(w -> "weight-concentration".equals(w.id()));
        }

        @Test
        @DisplayName("Should not warn about weight concentration with multiple competencies")
        void shouldNotWarnWithMultipleCompetencies() {
            // Given
            TestTemplate template = createValidTemplate(List.of(competencyId1, competencyId2));
            when(questionRepository.countActiveQuestionsForCompetency(competencyId1)).thenReturn(10L);
            when(questionRepository.countActiveQuestionsForCompetency(competencyId2)).thenReturn(10L);

            // When
            BlueprintValidationResult result = validationService.validateForPublishing(template);

            // Then
            assertThat(result.warnings())
                    .noneMatch(w -> "weight-concentration".equals(w.id()));
        }
    }

    @Nested
    @DisplayName("Competency ID Extraction")
    class CompetencyIdExtractionTests {

        @Test
        @DisplayName("Should extract competency IDs from OverviewBlueprint")
        void shouldExtractCompetencyIdsFromOverviewBlueprint() {
            // Given
            OverviewBlueprint blueprint = new OverviewBlueprint();
            blueprint.setCompetencyIds(List.of(competencyId1, competencyId2));

            // When
            List<UUID> ids = validationService.extractCompetencyIds(blueprint);

            // Then
            assertThat(ids).containsExactly(competencyId1, competencyId2);
        }

        @Test
        @DisplayName("Should return empty list for JobFitBlueprint")
        void shouldReturnEmptyListForJobFitBlueprint() {
            // Given
            JobFitBlueprint blueprint = new JobFitBlueprint("15-1252.00", 50);

            // When
            List<UUID> ids = validationService.extractCompetencyIds(blueprint);

            // Then
            assertThat(ids).isEmpty();
        }

        @Test
        @DisplayName("Should return empty list for TeamFitBlueprint")
        void shouldReturnEmptyListForTeamFitBlueprint() {
            // Given
            TeamFitBlueprint blueprint = new TeamFitBlueprint(UUID.randomUUID(), 0.75);

            // When
            List<UUID> ids = validationService.extractCompetencyIds(blueprint);

            // Then
            assertThat(ids).isEmpty();
        }

        @Test
        @DisplayName("Should return empty list when OverviewBlueprint competencyIds is null")
        void shouldReturnEmptyListWhenCompetencyIdsIsNull() {
            // Given
            OverviewBlueprint blueprint = new OverviewBlueprint();
            blueprint.setCompetencyIds(null);

            // When
            List<UUID> ids = validationService.extractCompetencyIds(blueprint);

            // Then
            assertThat(ids).isEmpty();
        }
    }

    @Nested
    @DisplayName("validateForSimulation")
    class ValidateForSimulationTests {

        @Test
        @DisplayName("Should return canSimulate=true for valid template")
        void shouldReturnCanSimulateTrueForValidTemplate() {
            // Given
            TestTemplate template = createValidTemplate(List.of(competencyId1));
            when(questionRepository.countActiveQuestionsForCompetency(competencyId1)).thenReturn(10L);

            // When
            BlueprintValidationResult result = validationService.validateForSimulation(template);

            // Then
            assertThat(result.canSimulate()).isTrue();
            assertThat(result.errors()).isEmpty();
        }

        @Test
        @DisplayName("Should fail simulation when blueprint is null")
        void shouldFailSimulationWhenBlueprintIsNull() {
            // Given
            TestTemplate template = new TestTemplate();
            template.setId(UUID.randomUUID());
            when(blueprintConversionService.ensureTypedBlueprint(template)).thenReturn(false);

            // When
            BlueprintValidationResult result = validationService.validateForSimulation(template);

            // Then
            assertThat(result.canSimulate()).isFalse();
            assertThat(result.errors())
                    .anyMatch(e -> "null-blueprint".equals(e.id()));
        }

        @Test
        @DisplayName("Should not check template name for simulation")
        void shouldNotCheckTemplateNameForSimulation() {
            // Given
            TestTemplate template = createValidTemplate(List.of(competencyId1));
            when(questionRepository.countActiveQuestionsForCompetency(competencyId1)).thenReturn(10L);
            template.setName(null); // Missing name should not block simulation

            // When
            BlueprintValidationResult result = validationService.validateForSimulation(template);

            // Then
            assertThat(result.canSimulate()).isTrue();
            assertThat(result.errors())
                    .noneMatch(e -> "no-template-name".equals(e.id()));
        }

        @Test
        @DisplayName("Should not check time limit for simulation")
        void shouldNotCheckTimeLimitForSimulation() {
            // Given
            TestTemplate template = createValidTemplate(List.of(competencyId1));
            when(questionRepository.countActiveQuestionsForCompetency(competencyId1)).thenReturn(10L);
            template.setTimeLimitMinutes(null);

            // When
            BlueprintValidationResult result = validationService.validateForSimulation(template);

            // Then
            assertThat(result.canSimulate()).isTrue();
            assertThat(result.errors())
                    .noneMatch(e -> "invalid-time-limit".equals(e.id()));
        }

        @Test
        @DisplayName("Should fail simulation when competency has zero questions")
        void shouldFailSimulationWhenCompetencyHasZeroQuestions() {
            // Given
            TestTemplate template = createValidTemplate(List.of(competencyId1));
            when(questionRepository.countActiveQuestionsForCompetency(competencyId1)).thenReturn(0L);

            // When
            BlueprintValidationResult result = validationService.validateForSimulation(template);

            // Then
            assertThat(result.canSimulate()).isFalse();
            assertThat(result.errors())
                    .anyMatch(e -> "no-questions-for-competency".equals(e.id()));
        }

        @Test
        @DisplayName("Should warn about low question count in simulation mode")
        void shouldWarnAboutLowQuestionCountInSimulationMode() {
            // Given
            TestTemplate template = createValidTemplate(List.of(competencyId1));
            when(questionRepository.countActiveQuestionsForCompetency(competencyId1)).thenReturn(2L);

            // When
            BlueprintValidationResult result = validationService.validateForSimulation(template);

            // Then
            assertThat(result.canSimulate()).isTrue(); // low count is warning, not error
            assertThat(result.warnings())
                    .anyMatch(w -> "low-question-count".equals(w.id()));
        }
    }

    @Nested
    @DisplayName("BlueprintValidationResult Utility Methods")
    class ResultUtilityTests {

        @Test
        @DisplayName("Should return error messages as flat string list")
        void shouldReturnErrorMessagesAsFlatStringList() {
            // Given
            TestTemplate template = createValidTemplate(List.of(competencyId1));
            when(questionRepository.countActiveQuestionsForCompetency(competencyId1)).thenReturn(10L);
            template.setName(null);
            template.setTimeLimitMinutes(0);

            // When
            BlueprintValidationResult result = validationService.validateForPublishing(template);

            // Then
            List<String> messages = result.errorMessages();
            assertThat(messages).isNotEmpty();
            assertThat(messages).allSatisfy(msg -> assertThat(msg).isNotBlank());
        }

        @Test
        @DisplayName("Should return combined allIssues list")
        void shouldReturnCombinedAllIssuesList() {
            // Given - template with both errors and warnings
            TestTemplate template = createValidTemplate(List.of(competencyId1));
            when(questionRepository.countActiveQuestionsForCompetency(competencyId1)).thenReturn(2L);
            template.setTimeLimitMinutes(0); // error

            // When
            BlueprintValidationResult result = validationService.validateForPublishing(template);

            // Then
            assertThat(result.hasErrors()).isTrue();
            assertThat(result.hasWarnings()).isTrue();
            assertThat(result.allIssues()).hasSizeGreaterThanOrEqualTo(2);
        }

        @Test
        @DisplayName("Valid result should report no errors and no warnings")
        void validResultShouldReportNoErrorsAndNoWarnings() {
            // When
            BlueprintValidationResult result = BlueprintValidationResult.allValid();

            // Then
            assertThat(result.valid()).isTrue();
            assertThat(result.canPublish()).isTrue();
            assertThat(result.canSimulate()).isTrue();
            assertThat(result.errors()).isEmpty();
            assertThat(result.warnings()).isEmpty();
            assertThat(result.hasErrors()).isFalse();
            assertThat(result.hasWarnings()).isFalse();
            assertThat(result.errorMessages()).isEmpty();
        }
    }

    @Nested
    @DisplayName("ValidationIssue Factory Methods")
    class ValidationIssueTests {

        @Test
        @DisplayName("Should create error issue with correct severity")
        void shouldCreateErrorIssueWithCorrectSeverity() {
            ValidationIssue issue = ValidationIssue.error("test-id", "Test message");

            assertThat(issue.id()).isEqualTo("test-id");
            assertThat(issue.severity()).isEqualTo(ValidationSeverity.ERROR);
            assertThat(issue.message()).isEqualTo("Test message");
            assertThat(issue.competencyId()).isNull();
        }

        @Test
        @DisplayName("Should create warning issue with correct severity")
        void shouldCreateWarningIssueWithCorrectSeverity() {
            ValidationIssue issue = ValidationIssue.warning("test-id", "Test warning");

            assertThat(issue.severity()).isEqualTo(ValidationSeverity.WARNING);
            assertThat(issue.competencyId()).isNull();
        }

        @Test
        @DisplayName("Should create info issue with correct severity")
        void shouldCreateInfoIssueWithCorrectSeverity() {
            ValidationIssue issue = ValidationIssue.info("test-id", "Test info");

            assertThat(issue.severity()).isEqualTo(ValidationSeverity.INFO);
        }

        @Test
        @DisplayName("Should create competency-specific error with competencyId")
        void shouldCreateCompetencySpecificError() {
            String compId = UUID.randomUUID().toString();
            ValidationIssue issue = ValidationIssue.errorForCompetency(
                    "no-questions", "No questions", compId);

            assertThat(issue.severity()).isEqualTo(ValidationSeverity.ERROR);
            assertThat(issue.competencyId()).isEqualTo(compId);
        }

        @Test
        @DisplayName("Should create competency-specific warning with competencyId")
        void shouldCreateCompetencySpecificWarning() {
            String compId = UUID.randomUUID().toString();
            ValidationIssue issue = ValidationIssue.warningForCompetency(
                    "low-count", "Low count", compId);

            assertThat(issue.severity()).isEqualTo(ValidationSeverity.WARNING);
            assertThat(issue.competencyId()).isEqualTo(compId);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle template with all valid fields at boundary values")
        void shouldHandleTemplateWithBoundaryValues() {
            // Given - minimum valid configuration
            TestTemplate template = createValidTemplate(List.of(competencyId1, competencyId2));
            when(questionRepository.countActiveQuestionsForCompetency(competencyId1)).thenReturn(3L);
            when(questionRepository.countActiveQuestionsForCompetency(competencyId2)).thenReturn(3L);
            template.setTimeLimitMinutes(1); // minimum positive
            template.setPassingScore(0.0);   // minimum boundary

            // When
            BlueprintValidationResult result = validationService.validateForPublishing(template);

            // Then
            assertThat(result.valid()).isTrue();
            assertThat(result.canPublish()).isTrue();
        }

        @Test
        @DisplayName("Should handle JobFitBlueprint without competencyIds gracefully")
        void shouldHandleJobFitBlueprintGracefully() {
            // Given - JobFit blueprints don't have direct competency IDs
            TestTemplate template = new TestTemplate();
            template.setId(UUID.randomUUID());
            template.setName("Job Fit Test");
            template.setGoal(AssessmentGoal.JOB_FIT);
            template.setTimeLimitMinutes(60);
            template.setPassingScore(70.0);

            JobFitBlueprint blueprint = new JobFitBlueprint("15-1252.00", 50);
            template.setTypedBlueprint(blueprint);

            when(blueprintConversionService.ensureTypedBlueprint(template)).thenReturn(true);

            // When
            BlueprintValidationResult result = validationService.validateForPublishing(template);

            // Then
            // JobFit has no direct competencies, so no-competencies error is expected
            // This is by design: competencies are resolved at assembly time from O*NET
            assertThat(result.errors())
                    .anyMatch(e -> "no-competencies".equals(e.id()));
            // No inventory checks should be performed
            verify(questionRepository, never()).countActiveQuestionsForCompetency(any());
        }

        @Test
        @DisplayName("Should handle TeamFitBlueprint without competencyIds gracefully")
        void shouldHandleTeamFitBlueprintGracefully() {
            // Given
            TestTemplate template = new TestTemplate();
            template.setId(UUID.randomUUID());
            template.setName("Team Fit Test");
            template.setGoal(AssessmentGoal.TEAM_FIT);
            template.setTimeLimitMinutes(45);
            template.setPassingScore(60.0);

            TeamFitBlueprint blueprint = new TeamFitBlueprint(UUID.randomUUID(), 0.75);
            template.setTypedBlueprint(blueprint);

            when(blueprintConversionService.ensureTypedBlueprint(template)).thenReturn(true);

            // When
            BlueprintValidationResult result = validationService.validateForPublishing(template);

            // Then
            assertThat(result.errors())
                    .anyMatch(e -> "no-competencies".equals(e.id()));
            verify(questionRepository, never()).countActiveQuestionsForCompetency(any());
        }

        @Test
        @DisplayName("Should attempt legacy blueprint conversion before validation")
        void shouldAttemptLegacyBlueprintConversion() {
            // Given
            TestTemplate template = new TestTemplate();
            template.setId(UUID.randomUUID());
            template.setName("Legacy Template");
            template.setGoal(AssessmentGoal.OVERVIEW);
            template.setTimeLimitMinutes(60);
            template.setPassingScore(70.0);
            // No typed blueprint set

            when(blueprintConversionService.ensureTypedBlueprint(template)).thenReturn(false);

            // When
            validationService.validateForPublishing(template);

            // Then
            verify(blueprintConversionService).ensureTypedBlueprint(template);
        }
    }
}
