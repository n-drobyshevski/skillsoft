package app.skillsoft.assessmentbackend.services.assembly;

import app.skillsoft.assessmentbackend.domain.dto.blueprint.JobFitBlueprint;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.OverviewBlueprint;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.TeamFitBlueprint;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;
import app.skillsoft.assessmentbackend.domain.entities.BehavioralIndicator;
import app.skillsoft.assessmentbackend.domain.entities.Competency;
import app.skillsoft.assessmentbackend.domain.entities.DifficultyLevel;
import app.skillsoft.assessmentbackend.repository.BehavioralIndicatorRepository;
import app.skillsoft.assessmentbackend.services.external.TeamService;
import app.skillsoft.assessmentbackend.services.external.TeamService.TeamProfile;
import app.skillsoft.assessmentbackend.services.selection.QuestionSelectionService;
import app.skillsoft.assessmentbackend.services.assembly.AssemblyResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TeamFitAssembler.
 *
 * Tests the saturation-based priority algorithm for TEAM_FIT (Dynamic Gap Analysis) assessments.
 * Refactored to use QuestionSelectionService for centralized question selection.
 *
 * Test coverage:
 * - Happy path with valid blueprint and team profile
 * - Missing/null team ID
 * - Team service returns empty profile
 * - Undersaturated competency detection
 * - Saturation threshold effects
 * - Fallback to all competencies when none undersaturated
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TeamFitAssembler Tests")
class TeamFitAssemblerTest {

    @Mock
    private TeamService teamService;

    @Mock
    private BehavioralIndicatorRepository indicatorRepository;

    @Mock
    private QuestionSelectionService questionSelectionService;

    @InjectMocks
    private TeamFitAssembler assembler;

    // Test data
    private UUID teamId;
    private UUID competencyId1;
    private UUID competencyId2;
    private UUID indicatorId1;
    private UUID indicatorId2;
    private UUID questionId1;
    private UUID questionId2;
    private BehavioralIndicator indicator1;
    private BehavioralIndicator indicator2;

    @BeforeEach
    void setUp() {
        teamId = UUID.randomUUID();
        competencyId1 = UUID.randomUUID();
        competencyId2 = UUID.randomUUID();
        indicatorId1 = UUID.randomUUID();
        indicatorId2 = UUID.randomUUID();
        questionId1 = UUID.randomUUID();
        questionId2 = UUID.randomUUID();

        indicator1 = createIndicator(indicatorId1, "Indicator 1", 1.0f, true, competencyId1);
        indicator2 = createIndicator(indicatorId2, "Indicator 2", 0.8f, true, competencyId1);
    }

    @Nested
    @DisplayName("getSupportedGoal Tests")
    class GetSupportedGoalTests {

        @Test
        @DisplayName("should return TEAM_FIT as supported goal")
        void shouldReturnTeamFitGoal() {
            assertThat(assembler.getSupportedGoal()).isEqualTo(AssessmentGoal.TEAM_FIT);
        }
    }

    @Nested
    @DisplayName("supports Tests")
    class SupportsTests {

        @Test
        @DisplayName("should support TeamFitBlueprint")
        void shouldSupportTeamFitBlueprint() {
            TeamFitBlueprint blueprint = new TeamFitBlueprint();
            assertThat(assembler.supports(blueprint)).isTrue();
        }

        @Test
        @DisplayName("should not support JobFitBlueprint")
        void shouldNotSupportJobFitBlueprint() {
            JobFitBlueprint blueprint = new JobFitBlueprint();
            assertThat(assembler.supports(blueprint)).isFalse();
        }

        @Test
        @DisplayName("should not support OverviewBlueprint")
        void shouldNotSupportOverviewBlueprint() {
            OverviewBlueprint blueprint = new OverviewBlueprint();
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
        @DisplayName("should throw exception for non-TeamFitBlueprint")
        void shouldThrowExceptionForWrongBlueprintType() {
            JobFitBlueprint wrongBlueprint = new JobFitBlueprint();

            assertThatThrownBy(() -> assembler.assemble(wrongBlueprint))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TeamFitAssembler requires TeamFitBlueprint");
        }

        @Test
        @DisplayName("should throw exception for null blueprint")
        void shouldThrowExceptionForNullBlueprint() {
            assertThatThrownBy(() -> assembler.assemble(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TeamFitAssembler requires TeamFitBlueprint");
        }

        @Test
        @DisplayName("should return empty list for null team ID")
        void shouldReturnEmptyListForNullTeamId() {
            TeamFitBlueprint blueprint = new TeamFitBlueprint();
            blueprint.setTeamId(null);

            AssemblyResult result = assembler.assemble(blueprint);

            assertThat(result.questionIds()).isEmpty();
            verifyNoInteractions(teamService);
        }
    }

    @Nested
    @DisplayName("assemble - Team Profile Tests")
    class AssembleTeamProfileTests {

        @Test
        @DisplayName("should return empty list when team profile not found")
        void shouldReturnEmptyWhenTeamProfileNotFound() {
            // Given
            TeamFitBlueprint blueprint = createBlueprint(teamId, 0.3);
            when(teamService.getTeamProfile(teamId)).thenReturn(Optional.empty());

            // When
            AssemblyResult result = assembler.assemble(blueprint);

            // Then
            assertThat(result.questionIds()).isEmpty();
            verify(teamService).getTeamProfile(teamId);
        }

        @Test
        @DisplayName("should use undersaturated competencies from team service")
        void shouldUseUndersaturatedCompetencies() {
            // Given
            TeamFitBlueprint blueprint = createBlueprint(teamId, 0.3);

            Map<UUID, Double> saturation = new HashMap<>();
            saturation.put(competencyId1, 0.2);  // Undersaturated
            saturation.put(competencyId2, 0.8);  // Saturated

            TeamProfile profile = createTeamProfile(teamId, saturation);
            when(teamService.getTeamProfile(teamId)).thenReturn(Optional.of(profile));
            when(teamService.getUndersaturatedCompetencies(teamId, 0.3))
                .thenReturn(List.of(competencyId1));
            when(indicatorRepository.findByCompetencyIdIn(anySet()))
                .thenReturn(List.of(indicator1));
            when(questionSelectionService.selectQuestionsForIndicator(
                any(), anyInt(), any(), anySet()))
                .thenReturn(List.of(questionId1));

            // When
            AssemblyResult result = assembler.assemble(blueprint);

            // Then
            assertThat(result.questionIds()).isNotEmpty();
            verify(teamService).getUndersaturatedCompetencies(teamId, 0.3);
        }

        @Test
        @DisplayName("should fallback to all competencies when none undersaturated")
        void shouldFallbackToAllCompetenciesWhenNoneUndersaturated() {
            // Given
            TeamFitBlueprint blueprint = createBlueprint(teamId, 0.3);

            Map<UUID, Double> saturation = new HashMap<>();
            saturation.put(competencyId1, 0.9);  // All saturated
            saturation.put(competencyId2, 0.8);

            // Create indicators for both competencies since fallback uses all
            BehavioralIndicator ind1 = createIndicator(indicatorId1, "Ind1", 1.0f, true, competencyId1);
            BehavioralIndicator ind2 = createIndicator(indicatorId2, "Ind2", 0.8f, true, competencyId2);

            TeamProfile profile = createTeamProfile(teamId, saturation);
            when(teamService.getTeamProfile(teamId)).thenReturn(Optional.of(profile));
            when(teamService.getUndersaturatedCompetencies(teamId, 0.3))
                .thenReturn(List.of());  // None undersaturated
            when(indicatorRepository.findByCompetencyIdIn(anySet()))
                .thenReturn(List.of(ind1, ind2));
            when(questionSelectionService.selectQuestionsForIndicator(
                any(), anyInt(), any(), anySet()))
                .thenReturn(List.of(questionId1));

            // When
            AssemblyResult result = assembler.assemble(blueprint);

            // Then - Should still produce questions from all competencies
            assertThat(result.questionIds()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("assemble - Saturation Threshold Tests")
    class AssembleSaturationThresholdTests {

        @Test
        @DisplayName("should use custom saturation threshold")
        void shouldUseCustomSaturationThreshold() {
            // Given
            TeamFitBlueprint blueprint = createBlueprint(teamId, 0.5);

            Map<UUID, Double> saturation = new HashMap<>();
            saturation.put(competencyId1, 0.4);  // Undersaturated with threshold 0.5

            TeamProfile profile = createTeamProfile(teamId, saturation);
            when(teamService.getTeamProfile(teamId)).thenReturn(Optional.of(profile));
            when(teamService.getUndersaturatedCompetencies(teamId, 0.5))
                .thenReturn(List.of(competencyId1));
            when(indicatorRepository.findByCompetencyIdIn(anySet()))
                .thenReturn(List.of(indicator1));
            when(questionSelectionService.selectQuestionsForIndicator(
                any(), anyInt(), any(), anySet()))
                .thenReturn(List.of(questionId1));

            // When
            assembler.assemble(blueprint);

            // Then
            verify(teamService).getUndersaturatedCompetencies(teamId, 0.5);
        }

        @Test
        @DisplayName("should use default threshold for invalid value")
        void shouldUseDefaultForInvalidThreshold() {
            // Given
            TeamFitBlueprint blueprint = createBlueprint(teamId, 0);  // Invalid

            Map<UUID, Double> saturation = new HashMap<>();
            saturation.put(competencyId1, 0.2);

            TeamProfile profile = createTeamProfile(teamId, saturation);
            when(teamService.getTeamProfile(teamId)).thenReturn(Optional.of(profile));
            when(teamService.getUndersaturatedCompetencies(eq(teamId), anyDouble()))
                .thenReturn(List.of(competencyId1));
            when(indicatorRepository.findByCompetencyIdIn(anySet()))
                .thenReturn(List.of(indicator1));
            when(questionSelectionService.selectQuestionsForIndicator(
                any(), anyInt(), any(), anySet()))
                .thenReturn(List.of(questionId1));

            // When
            assembler.assemble(blueprint);

            // Then - Should use default 0.3
            verify(teamService).getUndersaturatedCompetencies(teamId, 0.3);
        }

        @Test
        @DisplayName("should use default threshold when value exceeds 1")
        void shouldUseDefaultForThresholdOverOne() {
            // Given
            TeamFitBlueprint blueprint = createBlueprint(teamId, 1.5);  // Invalid

            Map<UUID, Double> saturation = new HashMap<>();
            saturation.put(competencyId1, 0.2);

            TeamProfile profile = createTeamProfile(teamId, saturation);
            when(teamService.getTeamProfile(teamId)).thenReturn(Optional.of(profile));
            when(teamService.getUndersaturatedCompetencies(eq(teamId), anyDouble()))
                .thenReturn(List.of(competencyId1));
            when(indicatorRepository.findByCompetencyIdIn(anySet()))
                .thenReturn(List.of(indicator1));
            when(questionSelectionService.selectQuestionsForIndicator(
                any(), anyInt(), any(), anySet()))
                .thenReturn(List.of(questionId1));

            // When
            assembler.assemble(blueprint);

            // Then - Should use default 0.3
            verify(teamService).getUndersaturatedCompetencies(teamId, 0.3);
        }
    }

    @Nested
    @DisplayName("assemble - Question Selection Tests")
    class AssembleQuestionSelectionTests {

        @Test
        @DisplayName("should delegate to QuestionSelectionService for each indicator")
        void shouldDelegateToQuestionSelectionService() {
            // Given
            TeamFitBlueprint blueprint = createBlueprint(teamId, 0.3);

            Map<UUID, Double> saturation = new HashMap<>();
            saturation.put(competencyId1, 0.2);

            TeamProfile profile = createTeamProfile(teamId, saturation);
            when(teamService.getTeamProfile(teamId)).thenReturn(Optional.of(profile));
            when(teamService.getUndersaturatedCompetencies(teamId, 0.3))
                .thenReturn(List.of(competencyId1));
            when(indicatorRepository.findByCompetencyIdIn(anySet()))
                .thenReturn(List.of(indicator1, indicator2));
            when(questionSelectionService.selectQuestionsForIndicator(
                eq(indicatorId1), anyInt(), eq(DifficultyLevel.INTERMEDIATE), anySet()))
                .thenReturn(List.of(questionId1));
            when(questionSelectionService.selectQuestionsForIndicator(
                eq(indicatorId2), anyInt(), eq(DifficultyLevel.INTERMEDIATE), anySet()))
                .thenReturn(List.of(questionId2));

            // When
            AssemblyResult result = assembler.assemble(blueprint);

            // Then
            assertThat(result.questionIds()).contains(questionId1, questionId2);
            verify(questionSelectionService, times(2)).selectQuestionsForIndicator(
                any(), anyInt(), eq(DifficultyLevel.INTERMEDIATE), anySet()
            );
        }

        @Test
        @DisplayName("should skip inactive indicators")
        void shouldFilterInactiveIndicators() {
            // Given
            TeamFitBlueprint blueprint = createBlueprint(teamId, 0.3);

            Map<UUID, Double> saturation = new HashMap<>();
            saturation.put(competencyId1, 0.2);

            BehavioralIndicator inactiveIndicator = createIndicator(indicatorId2, "Inactive", 0.5f, false, competencyId1);

            TeamProfile profile = createTeamProfile(teamId, saturation);
            when(teamService.getTeamProfile(teamId)).thenReturn(Optional.of(profile));
            when(teamService.getUndersaturatedCompetencies(teamId, 0.3))
                .thenReturn(List.of(competencyId1));
            // Batch query returns both active and inactive; implementation filters active only
            when(indicatorRepository.findByCompetencyIdIn(anySet()))
                .thenReturn(List.of(indicator1, inactiveIndicator));
            when(questionSelectionService.selectQuestionsForIndicator(
                eq(indicatorId1), anyInt(), any(), anySet()))
                .thenReturn(List.of(questionId1));

            // When
            AssemblyResult result = assembler.assemble(blueprint);

            // Then - Only active indicator should be used
            verify(questionSelectionService, times(1)).selectQuestionsForIndicator(
                any(), anyInt(), any(), anySet()
            );
        }

        @Test
        @DisplayName("should handle no indicators for competency")
        void shouldHandleNoIndicatorsForCompetency() {
            // Given
            TeamFitBlueprint blueprint = createBlueprint(teamId, 0.3);

            Map<UUID, Double> saturation = new HashMap<>();
            saturation.put(competencyId1, 0.2);

            TeamProfile profile = createTeamProfile(teamId, saturation);
            when(teamService.getTeamProfile(teamId)).thenReturn(Optional.of(profile));
            when(teamService.getUndersaturatedCompetencies(teamId, 0.3))
                .thenReturn(List.of(competencyId1));
            when(indicatorRepository.findByCompetencyIdIn(anySet()))
                .thenReturn(List.of());  // No indicators

            // When
            AssemblyResult result = assembler.assemble(blueprint);

            // Then
            assertThat(result.questionIds()).isEmpty();
            verifyNoInteractions(questionSelectionService);
        }
    }

    @Nested
    @DisplayName("assemble - Adaptive Difficulty Tests")
    class AssembleAdaptiveDifficultyTests {

        @Test
        @DisplayName("should select ADVANCED difficulty for critical gap (saturation < 0.1)")
        void shouldSelectAdvancedDifficultyForCriticalGap() {
            // Given
            TeamFitBlueprint blueprint = createBlueprint(teamId, 0.3);

            Map<UUID, Double> saturation = new HashMap<>();
            saturation.put(competencyId1, 0.05);  // Critical gap: < 0.1

            TeamProfile profile = createTeamProfile(teamId, saturation);
            when(teamService.getTeamProfile(teamId)).thenReturn(Optional.of(profile));
            when(teamService.getUndersaturatedCompetencies(teamId, 0.3))
                .thenReturn(List.of(competencyId1));
            when(indicatorRepository.findByCompetencyIdIn(anySet()))
                .thenReturn(List.of(indicator1));
            when(questionSelectionService.selectQuestionsForIndicator(
                any(), anyInt(), any(), anySet()))
                .thenReturn(List.of(questionId1));

            // When
            assembler.assemble(blueprint);

            // Then - Critical gap should use ADVANCED difficulty
            verify(questionSelectionService).selectQuestionsForIndicator(
                eq(indicatorId1), anyInt(), eq(DifficultyLevel.ADVANCED), anySet()
            );
        }

        @Test
        @DisplayName("should select INTERMEDIATE difficulty for moderate gap (saturation 0.1-0.3)")
        void shouldSelectIntermediateDifficultyForModerateGap() {
            // Given
            TeamFitBlueprint blueprint = createBlueprint(teamId, 0.3);

            Map<UUID, Double> saturation = new HashMap<>();
            saturation.put(competencyId1, 0.2);  // Moderate gap: 0.1 <= sat < 0.3

            TeamProfile profile = createTeamProfile(teamId, saturation);
            when(teamService.getTeamProfile(teamId)).thenReturn(Optional.of(profile));
            when(teamService.getUndersaturatedCompetencies(teamId, 0.3))
                .thenReturn(List.of(competencyId1));
            when(indicatorRepository.findByCompetencyIdIn(anySet()))
                .thenReturn(List.of(indicator1));
            when(questionSelectionService.selectQuestionsForIndicator(
                any(), anyInt(), any(), anySet()))
                .thenReturn(List.of(questionId1));

            // When
            assembler.assemble(blueprint);

            // Then - Moderate gap should use INTERMEDIATE difficulty
            verify(questionSelectionService).selectQuestionsForIndicator(
                eq(indicatorId1), anyInt(), eq(DifficultyLevel.INTERMEDIATE), anySet()
            );
        }

        @Test
        @DisplayName("should select FOUNDATIONAL difficulty for minor gap (saturation >= 0.3)")
        void shouldSelectFoundationalDifficultyForMinorGap() {
            // Given
            TeamFitBlueprint blueprint = createBlueprint(teamId, 0.5);  // Higher threshold to include 0.4

            Map<UUID, Double> saturation = new HashMap<>();
            saturation.put(competencyId1, 0.4);  // Minor gap: >= 0.3

            TeamProfile profile = createTeamProfile(teamId, saturation);
            when(teamService.getTeamProfile(teamId)).thenReturn(Optional.of(profile));
            when(teamService.getUndersaturatedCompetencies(teamId, 0.5))
                .thenReturn(List.of(competencyId1));
            when(indicatorRepository.findByCompetencyIdIn(anySet()))
                .thenReturn(List.of(indicator1));
            when(questionSelectionService.selectQuestionsForIndicator(
                any(), anyInt(), any(), anySet()))
                .thenReturn(List.of(questionId1));

            // When
            assembler.assemble(blueprint);

            // Then - Minor gap should use FOUNDATIONAL difficulty
            verify(questionSelectionService).selectQuestionsForIndicator(
                eq(indicatorId1), anyInt(), eq(DifficultyLevel.FOUNDATIONAL), anySet()
            );
        }

        @Test
        @DisplayName("should use different difficulties for multiple competencies with different saturations")
        void shouldUseDifferentDifficultiesPerCompetency() {
            // Given
            TeamFitBlueprint blueprint = createBlueprint(teamId, 0.5);

            Map<UUID, Double> saturation = new HashMap<>();
            saturation.put(competencyId1, 0.05);  // Critical gap -> ADVANCED
            saturation.put(competencyId2, 0.4);   // Minor gap -> FOUNDATIONAL

            // indicator2 needs to belong to competencyId2 for this test
            BehavioralIndicator ind2ForComp2 = createIndicator(indicatorId2, "Indicator 2", 0.8f, true, competencyId2);

            TeamProfile profile = createTeamProfile(teamId, saturation);
            when(teamService.getTeamProfile(teamId)).thenReturn(Optional.of(profile));
            when(teamService.getUndersaturatedCompetencies(teamId, 0.5))
                .thenReturn(List.of(competencyId1, competencyId2));
            when(indicatorRepository.findByCompetencyIdIn(anySet()))
                .thenReturn(List.of(indicator1, ind2ForComp2));
            when(questionSelectionService.selectQuestionsForIndicator(
                any(), anyInt(), any(), anySet()))
                .thenReturn(List.of(questionId1));

            // When
            assembler.assemble(blueprint);

            // Then - Each competency should get its own difficulty based on saturation
            verify(questionSelectionService).selectQuestionsForIndicator(
                eq(indicatorId1), anyInt(), eq(DifficultyLevel.ADVANCED), anySet()
            );
            verify(questionSelectionService).selectQuestionsForIndicator(
                eq(indicatorId2), anyInt(), eq(DifficultyLevel.FOUNDATIONAL), anySet()
            );
        }

        @Test
        @DisplayName("should use ADVANCED at boundary saturation of exactly 0.0")
        void shouldSelectAdvancedAtZeroSaturation() {
            // Given
            TeamFitBlueprint blueprint = createBlueprint(teamId, 0.3);

            Map<UUID, Double> saturation = new HashMap<>();
            saturation.put(competencyId1, 0.0);  // Zero saturation -> ADVANCED

            TeamProfile profile = createTeamProfile(teamId, saturation);
            when(teamService.getTeamProfile(teamId)).thenReturn(Optional.of(profile));
            when(teamService.getUndersaturatedCompetencies(teamId, 0.3))
                .thenReturn(List.of(competencyId1));
            when(indicatorRepository.findByCompetencyIdIn(anySet()))
                .thenReturn(List.of(indicator1));
            when(questionSelectionService.selectQuestionsForIndicator(
                any(), anyInt(), any(), anySet()))
                .thenReturn(List.of(questionId1));

            // When
            assembler.assemble(blueprint);

            // Then
            verify(questionSelectionService).selectQuestionsForIndicator(
                eq(indicatorId1), anyInt(), eq(DifficultyLevel.ADVANCED), anySet()
            );
        }

        @Test
        @DisplayName("should use INTERMEDIATE at boundary saturation of exactly 0.1")
        void shouldSelectIntermediateAtBoundarySaturation() {
            // Given
            TeamFitBlueprint blueprint = createBlueprint(teamId, 0.3);

            Map<UUID, Double> saturation = new HashMap<>();
            saturation.put(competencyId1, 0.1);  // Exactly 0.1 -> INTERMEDIATE

            TeamProfile profile = createTeamProfile(teamId, saturation);
            when(teamService.getTeamProfile(teamId)).thenReturn(Optional.of(profile));
            when(teamService.getUndersaturatedCompetencies(teamId, 0.3))
                .thenReturn(List.of(competencyId1));
            when(indicatorRepository.findByCompetencyIdIn(anySet()))
                .thenReturn(List.of(indicator1));
            when(questionSelectionService.selectQuestionsForIndicator(
                any(), anyInt(), any(), anySet()))
                .thenReturn(List.of(questionId1));

            // When
            assembler.assemble(blueprint);

            // Then
            verify(questionSelectionService).selectQuestionsForIndicator(
                eq(indicatorId1), anyInt(), eq(DifficultyLevel.INTERMEDIATE), anySet()
            );
        }

        @Test
        @DisplayName("should use FOUNDATIONAL at boundary saturation of exactly 0.3")
        void shouldSelectFoundationalAtBoundarySaturation() {
            // Given
            TeamFitBlueprint blueprint = createBlueprint(teamId, 0.5);  // Higher threshold

            Map<UUID, Double> saturation = new HashMap<>();
            saturation.put(competencyId1, 0.3);  // Exactly 0.3 -> FOUNDATIONAL

            TeamProfile profile = createTeamProfile(teamId, saturation);
            when(teamService.getTeamProfile(teamId)).thenReturn(Optional.of(profile));
            when(teamService.getUndersaturatedCompetencies(teamId, 0.5))
                .thenReturn(List.of(competencyId1));
            when(indicatorRepository.findByCompetencyIdIn(anySet()))
                .thenReturn(List.of(indicator1));
            when(questionSelectionService.selectQuestionsForIndicator(
                any(), anyInt(), any(), anySet()))
                .thenReturn(List.of(questionId1));

            // When
            assembler.assemble(blueprint);

            // Then
            verify(questionSelectionService).selectQuestionsForIndicator(
                eq(indicatorId1), anyInt(), eq(DifficultyLevel.FOUNDATIONAL), anySet()
            );
        }
    }

    // Helper methods

    private TeamFitBlueprint createBlueprint(UUID teamId, double saturationThreshold) {
        TeamFitBlueprint blueprint = new TeamFitBlueprint();
        blueprint.setTeamId(teamId);
        blueprint.setSaturationThreshold(saturationThreshold);
        return blueprint;
    }

    private BehavioralIndicator createIndicator(UUID id, String title, float weight, boolean isActive, UUID competencyId) {
        BehavioralIndicator indicator = new BehavioralIndicator();
        indicator.setId(id);
        indicator.setTitle(title);
        indicator.setWeight(weight);
        indicator.setActive(isActive);
        if (competencyId != null) {
            Competency competency = new Competency();
            competency.setId(competencyId);
            indicator.setCompetency(competency);
        }
        return indicator;
    }

    private TeamProfile createTeamProfile(UUID teamId, Map<UUID, Double> saturation) {
        return new TeamProfile(
            teamId,
            "Test Team",
            List.of(),  // members
            saturation,
            Map.of(),   // averagePersonality
            List.of()   // skillGaps
        );
    }
}
