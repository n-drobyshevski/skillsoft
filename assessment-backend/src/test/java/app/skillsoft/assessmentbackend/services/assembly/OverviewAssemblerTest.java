package app.skillsoft.assessmentbackend.services.assembly;

import app.skillsoft.assessmentbackend.domain.dto.blueprint.JobFitBlueprint;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.OverviewBlueprint;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;
import app.skillsoft.assessmentbackend.domain.entities.DifficultyLevel;
import app.skillsoft.assessmentbackend.services.selection.QuestionSelectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OverviewAssembler.
 *
 * Tests the OVERVIEW (Universal Baseline) assessment assembler after refactoring
 * to use QuestionSelectionService for centralized question selection.
 *
 * Test coverage:
 * - Happy path with valid blueprint and questions
 * - Empty competency IDs
 * - Delegation to QuestionSelectionService
 * - Blueprint parameter extraction (questionsPerIndicator, difficulty, shuffle)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OverviewAssembler Tests")
class OverviewAssemblerTest {

    @Mock
    private QuestionSelectionService questionSelectionService;

    @InjectMocks
    private OverviewAssembler assembler;

    // Test data
    private UUID competencyId1;
    private UUID competencyId2;
    private UUID questionId1;
    private UUID questionId2;
    private UUID questionId3;

    @BeforeEach
    void setUp() {
        competencyId1 = UUID.randomUUID();
        competencyId2 = UUID.randomUUID();
        questionId1 = UUID.randomUUID();
        questionId2 = UUID.randomUUID();
        questionId3 = UUID.randomUUID();
    }

    @Nested
    @DisplayName("getSupportedGoal Tests")
    class GetSupportedGoalTests {

        @Test
        @DisplayName("should return OVERVIEW as supported goal")
        void shouldReturnOverviewGoal() {
            assertThat(assembler.getSupportedGoal()).isEqualTo(AssessmentGoal.OVERVIEW);
        }
    }

    @Nested
    @DisplayName("supports Tests")
    class SupportsTests {

        @Test
        @DisplayName("should support OverviewBlueprint")
        void shouldSupportOverviewBlueprint() {
            OverviewBlueprint blueprint = new OverviewBlueprint();
            assertThat(assembler.supports(blueprint)).isTrue();
        }

        @Test
        @DisplayName("should not support JobFitBlueprint")
        void shouldNotSupportJobFitBlueprint() {
            JobFitBlueprint blueprint = new JobFitBlueprint();
            assertThat(assembler.supports(blueprint)).isFalse();
        }

        @Test
        @DisplayName("should not support null blueprint")
        void shouldNotSupportNullBlueprint() {
            assertThat(assembler.supports(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("assemble - Input Validation Tests")
    class AssembleInputValidationTests {

        @Test
        @DisplayName("should throw exception for non-OverviewBlueprint")
        void shouldThrowExceptionForWrongBlueprintType() {
            JobFitBlueprint wrongBlueprint = new JobFitBlueprint();

            assertThatThrownBy(() -> assembler.assemble(wrongBlueprint))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("OverviewAssembler requires OverviewBlueprint");
        }

        @Test
        @DisplayName("should throw exception for null blueprint")
        void shouldThrowExceptionForNullBlueprint() {
            assertThatThrownBy(() -> assembler.assemble(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("OverviewAssembler requires OverviewBlueprint");
        }

        @Test
        @DisplayName("should return empty list for null competency IDs")
        void shouldReturnEmptyListForNullCompetencyIds() {
            OverviewBlueprint blueprint = new OverviewBlueprint();
            blueprint.setCompetencyIds(null);

            List<UUID> result = assembler.assemble(blueprint);

            assertThat(result).isEmpty();
            verifyNoInteractions(questionSelectionService);
        }

        @Test
        @DisplayName("should return empty list for empty competency IDs")
        void shouldReturnEmptyListForEmptyCompetencyIds() {
            OverviewBlueprint blueprint = new OverviewBlueprint();
            blueprint.setCompetencyIds(List.of());

            List<UUID> result = assembler.assemble(blueprint);

            assertThat(result).isEmpty();
            verifyNoInteractions(questionSelectionService);
        }
    }

    @Nested
    @DisplayName("assemble - Delegation Tests")
    class AssembleDelegationTests {

        @Test
        @DisplayName("should delegate to QuestionSelectionService with blueprint parameters")
        void shouldDelegateToQuestionSelectionService() {
            // Given
            OverviewBlueprint blueprint = createBlueprint(competencyId1);
            blueprint.setQuestionsPerIndicator(5);
            blueprint.setPreferredDifficulty(DifficultyLevel.ADVANCED);
            blueprint.setShuffleQuestions(false);

            List<UUID> expectedQuestions = List.of(questionId1, questionId2, questionId3);
            when(questionSelectionService.selectQuestionsForCompetencies(
                anyList(), anyInt(), any(DifficultyLevel.class), anyBoolean(), anyBoolean()))
                .thenReturn(expectedQuestions);

            // When
            List<UUID> result = assembler.assemble(blueprint);

            // Then
            assertThat(result).isEqualTo(expectedQuestions);
            verify(questionSelectionService).selectQuestionsForCompetencies(
                List.of(competencyId1),
                5,
                DifficultyLevel.ADVANCED,
                false,
                true // contextNeutralOnly is always true for OVERVIEW
            );
        }

        @Test
        @DisplayName("should use default questionsPerIndicator when not specified")
        void shouldUseDefaultQuestionsPerIndicator() {
            // Given
            OverviewBlueprint blueprint = createBlueprint(competencyId1);
            blueprint.setQuestionsPerIndicator(0); // Will trigger default

            when(questionSelectionService.selectQuestionsForCompetencies(
                anyList(), anyInt(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(List.of(questionId1));

            // When
            assembler.assemble(blueprint);

            // Then - DEFAULT_QUESTIONS_PER_INDICATOR = 3
            verify(questionSelectionService).selectQuestionsForCompetencies(
                anyList(),
                eq(3),
                any(),
                anyBoolean(),
                eq(true) // contextNeutralOnly
            );
        }

        @Test
        @DisplayName("should use default difficulty when not specified")
        void shouldUseDefaultDifficulty() {
            // Given
            OverviewBlueprint blueprint = createBlueprint(competencyId1);
            blueprint.setPreferredDifficulty(null);

            when(questionSelectionService.selectQuestionsForCompetencies(
                anyList(), anyInt(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(List.of(questionId1));

            // When
            assembler.assemble(blueprint);

            // Then - DEFAULT_DIFFICULTY = INTERMEDIATE
            verify(questionSelectionService).selectQuestionsForCompetencies(
                anyList(),
                anyInt(),
                eq(DifficultyLevel.INTERMEDIATE),
                anyBoolean(),
                eq(true) // contextNeutralOnly
            );
        }

        @Test
        @DisplayName("should pass shuffle preference to selection service")
        void shouldPassShufflePreference() {
            // Given
            OverviewBlueprint blueprint = createBlueprint(competencyId1);
            blueprint.setShuffleQuestions(true);

            when(questionSelectionService.selectQuestionsForCompetencies(
                anyList(), anyInt(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(List.of(questionId1));

            // When
            assembler.assemble(blueprint);

            // Then
            verify(questionSelectionService).selectQuestionsForCompetencies(
                anyList(),
                anyInt(),
                any(),
                eq(true),
                eq(true) // contextNeutralOnly
            );
        }

        @Test
        @DisplayName("should pass multiple competency IDs to selection service")
        void shouldPassMultipleCompetencies() {
            // Given
            OverviewBlueprint blueprint = createBlueprint(competencyId1, competencyId2);

            when(questionSelectionService.selectQuestionsForCompetencies(
                anyList(), anyInt(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(List.of(questionId1, questionId2, questionId3));

            // When
            List<UUID> result = assembler.assemble(blueprint);

            // Then
            assertThat(result).hasSize(3);
            verify(questionSelectionService).selectQuestionsForCompetencies(
                eq(List.of(competencyId1, competencyId2)),
                anyInt(),
                any(),
                anyBoolean(),
                eq(true) // contextNeutralOnly
            );
        }
    }

    @Nested
    @DisplayName("assemble - Result Handling Tests")
    class AssembleResultHandlingTests {

        @Test
        @DisplayName("should return empty list when service returns empty")
        void shouldReturnEmptyWhenServiceReturnsEmpty() {
            // Given
            OverviewBlueprint blueprint = createBlueprint(competencyId1);
            when(questionSelectionService.selectQuestionsForCompetencies(
                anyList(), anyInt(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(List.of());

            // When
            List<UUID> result = assembler.assemble(blueprint);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return questions as provided by service")
        void shouldReturnQuestionsFromService() {
            // Given
            OverviewBlueprint blueprint = createBlueprint(competencyId1);
            List<UUID> expectedQuestions = List.of(questionId1, questionId2, questionId3);
            when(questionSelectionService.selectQuestionsForCompetencies(
                anyList(), anyInt(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(expectedQuestions);

            // When
            List<UUID> result = assembler.assemble(blueprint);

            // Then
            assertThat(result).containsExactlyElementsOf(expectedQuestions);
        }
    }

    @Nested
    @DisplayName("assemble - Graduated Difficulty Distribution Tests")
    class GraduatedDifficultyTests {

        @Test
        @DisplayName("should pass questionsPerIndicator >= 3 to service for graduated difficulty")
        void shouldPassQuestionsPerIndicatorForGraduatedDifficulty() {
            // Given: questionsPerIndicator = 3, which triggers graduated difficulty
            // in the QuestionSelectionServiceImpl
            OverviewBlueprint blueprint = createBlueprint(competencyId1, competencyId2);
            blueprint.setQuestionsPerIndicator(3);
            blueprint.setPreferredDifficulty(DifficultyLevel.INTERMEDIATE);
            blueprint.setShuffleQuestions(false);

            List<UUID> expectedQuestions = List.of(questionId1, questionId2, questionId3);
            when(questionSelectionService.selectQuestionsForCompetencies(
                anyList(), anyInt(), any(DifficultyLevel.class), anyBoolean(), anyBoolean()))
                .thenReturn(expectedQuestions);

            // When
            List<UUID> result = assembler.assemble(blueprint);

            // Then: verify the delegation passes questionsPerIndicator=3
            // which enables graduated difficulty inside QuestionSelectionServiceImpl
            assertThat(result).hasSize(3);
            verify(questionSelectionService).selectQuestionsForCompetencies(
                eq(List.of(competencyId1, competencyId2)),
                eq(3),
                eq(DifficultyLevel.INTERMEDIATE),
                eq(false),
                eq(true) // contextNeutralOnly always true for OVERVIEW
            );
        }

        @Test
        @DisplayName("should pass questionsPerIndicator = 5 for larger graduated difficulty spread")
        void shouldPassLargerQuestionsPerIndicatorForGraduatedDifficulty() {
            // Given: questionsPerIndicator = 5, should still trigger graduated difficulty
            OverviewBlueprint blueprint = createBlueprint(competencyId1);
            blueprint.setQuestionsPerIndicator(5);
            blueprint.setShuffleQuestions(true);

            List<UUID> fiveQuestions = List.of(
                    questionId1, questionId2, questionId3,
                    UUID.randomUUID(), UUID.randomUUID());
            when(questionSelectionService.selectQuestionsForCompetencies(
                anyList(), anyInt(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(fiveQuestions);

            // When
            List<UUID> result = assembler.assemble(blueprint);

            // Then
            assertThat(result).hasSize(5);
            verify(questionSelectionService).selectQuestionsForCompetencies(
                anyList(),
                eq(5),
                any(),
                eq(true),
                eq(true)
            );
        }

        @Test
        @DisplayName("should use default questionsPerIndicator=3 which enables graduated difficulty")
        void shouldDefaultToThreeQuestionsPerIndicatorEnablingGraduatedDifficulty() {
            // Given: default questionsPerIndicator=0 triggers DEFAULT_QUESTIONS_PER_INDICATOR=3
            OverviewBlueprint blueprint = createBlueprint(competencyId1);
            blueprint.setQuestionsPerIndicator(0);

            when(questionSelectionService.selectQuestionsForCompetencies(
                anyList(), anyInt(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(List.of(questionId1, questionId2, questionId3));

            // When
            assembler.assemble(blueprint);

            // Then: default of 3 should be passed, enabling graduated difficulty
            verify(questionSelectionService).selectQuestionsForCompetencies(
                anyList(),
                eq(3), // DEFAULT_QUESTIONS_PER_INDICATOR
                any(),
                anyBoolean(),
                eq(true)
            );
        }

        @Test
        @DisplayName("should pass questionsPerIndicator < 3 for legacy single-difficulty path")
        void shouldPassSmallQuestionsPerIndicatorForLegacyPath() {
            // Given: questionsPerIndicator = 2, should NOT trigger graduated difficulty
            OverviewBlueprint blueprint = createBlueprint(competencyId1);
            blueprint.setQuestionsPerIndicator(2);
            blueprint.setPreferredDifficulty(DifficultyLevel.FOUNDATIONAL);

            when(questionSelectionService.selectQuestionsForCompetencies(
                anyList(), anyInt(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(List.of(questionId1, questionId2));

            // When
            List<UUID> result = assembler.assemble(blueprint);

            // Then: questionsPerIndicator=2 uses legacy single-difficulty path
            assertThat(result).hasSize(2);
            verify(questionSelectionService).selectQuestionsForCompetencies(
                anyList(),
                eq(2),
                eq(DifficultyLevel.FOUNDATIONAL),
                anyBoolean(),
                eq(true)
            );
        }
    }

    @Nested
    @DisplayName("assemble - Exposure Tracking Delegation Tests")
    class ExposureTrackingDelegationTests {

        @Test
        @DisplayName("should invoke service which internally calls ExposureTrackingService")
        void shouldDelegateExposureTrackingThroughService() {
            // Given: The OverviewAssembler delegates to QuestionSelectionService,
            // which internally uses ExposureTrackingService (not a self-invocation).
            // We verify the full delegation path is invoked.
            OverviewBlueprint blueprint = createBlueprint(competencyId1);
            blueprint.setQuestionsPerIndicator(3);

            List<UUID> selectedQuestions = List.of(questionId1, questionId2, questionId3);
            when(questionSelectionService.selectQuestionsForCompetencies(
                anyList(), anyInt(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(selectedQuestions);

            // When
            List<UUID> result = assembler.assemble(blueprint);

            // Then: QuestionSelectionService was called (exposure tracking happens inside it)
            assertThat(result).hasSize(3);
            verify(questionSelectionService, times(1)).selectQuestionsForCompetencies(
                anyList(), anyInt(), any(), anyBoolean(), eq(true));
        }

        @Test
        @DisplayName("should always pass contextNeutralOnly=true for OVERVIEW assembly")
        void shouldAlwaysPassContextNeutralOnlyTrue() {
            // Given: OVERVIEW assessments always require context-neutral items
            OverviewBlueprint blueprint = createBlueprint(competencyId1, competencyId2);

            when(questionSelectionService.selectQuestionsForCompetencies(
                anyList(), anyInt(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(List.of(questionId1));

            // When
            assembler.assemble(blueprint);

            // Then: The 5th parameter (contextNeutralOnly) must always be true
            verify(questionSelectionService).selectQuestionsForCompetencies(
                anyList(), anyInt(), any(), anyBoolean(),
                eq(true) // contextNeutralOnly is always true for OVERVIEW
            );
        }
    }

    // Helper methods

    private OverviewBlueprint createBlueprint(UUID... competencyIds) {
        OverviewBlueprint blueprint = new OverviewBlueprint();
        blueprint.setCompetencyIds(List.of(competencyIds));
        return blueprint;
    }
}
