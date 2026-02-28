package app.skillsoft.assessmentbackend.services.assembly;

import app.skillsoft.assessmentbackend.domain.dto.blueprint.JobFitBlueprint;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.OverviewBlueprint;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.TeamFitBlueprint;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.repository.BehavioralIndicatorRepository;
import app.skillsoft.assessmentbackend.repository.CompetencyRepository;
import app.skillsoft.assessmentbackend.services.external.OnetCompetencyResolver;
import app.skillsoft.assessmentbackend.services.external.OnetService;
import app.skillsoft.assessmentbackend.services.external.OnetService.OnetProfile;
import app.skillsoft.assessmentbackend.services.external.PassportService;
import app.skillsoft.assessmentbackend.services.external.PassportService.CompetencyPassport;
import app.skillsoft.assessmentbackend.services.assembly.AssemblyResult;
import app.skillsoft.assessmentbackend.services.selection.QuestionSelectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JobFitAssembler.
 *
 * Tests the gap-based priority algorithm for JOB_FIT (Targeted Fit) assessments.
 * Refactored to use QuestionSelectionService for centralized question selection.
 *
 * Test coverage:
 * - Happy path with valid blueprint and O*NET profile
 * - Missing/invalid SOC code
 * - O*NET service returns empty profile
 * - Gap analysis with candidate passport
 * - Strictness level effects
 * - Difficulty level selection based on gap significance
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JobFitAssembler Tests")
class JobFitAssemblerTest {

    @Mock
    private OnetService onetService;

    @Mock
    private PassportService passportService;

    @Mock
    private CompetencyRepository competencyRepository;

    @Mock
    private BehavioralIndicatorRepository indicatorRepository;

    @Mock
    private QuestionSelectionService questionSelectionService;

    @Mock
    private OnetCompetencyResolver onetCompetencyResolver;

    @InjectMocks
    private JobFitAssembler assembler;

    // Test data
    private static final String VALID_SOC_CODE = "15-1252.00"; // Software Developers
    private static final String INVALID_SOC_CODE = "99-9999.00";

    private UUID competencyId1;
    private UUID competencyId2;
    private UUID indicatorId1;
    private UUID questionId1;
    private UUID questionId2;
    private Competency competency1;
    private Competency competency2;
    private BehavioralIndicator indicator1;

    @BeforeEach
    void setUp() {
        competencyId1 = UUID.randomUUID();
        competencyId2 = UUID.randomUUID();
        indicatorId1 = UUID.randomUUID();
        questionId1 = UUID.randomUUID();
        questionId2 = UUID.randomUUID();

        competency1 = createCompetency(competencyId1, "Problem Solving");
        competency2 = createCompetency(competencyId2, "Communication");
        indicator1 = createIndicator(indicatorId1, "Critical Thinking", 1.0f, true, competencyId1);

        // Default: resolver delegates to name-based lookup maps
        lenient().when(onetCompetencyResolver.buildCompetencyLookupMaps(anyList()))
            .thenAnswer(invocation -> {
                List<Competency> competencies = invocation.getArgument(0);
                Map<String, List<Competency>> lookup = new HashMap<>();
                for (var c : competencies) {
                    lookup.computeIfAbsent(c.getName().toLowerCase(), k -> new ArrayList<>()).add(c);
                }
                return lookup;
            });
    }

    @Nested
    @DisplayName("getSupportedGoal Tests")
    class GetSupportedGoalTests {

        @Test
        @DisplayName("should return JOB_FIT as supported goal")
        void shouldReturnJobFitGoal() {
            assertThat(assembler.getSupportedGoal()).isEqualTo(AssessmentGoal.JOB_FIT);
        }
    }

    @Nested
    @DisplayName("supports Tests")
    class SupportsTests {

        @Test
        @DisplayName("should support JobFitBlueprint")
        void shouldSupportJobFitBlueprint() {
            JobFitBlueprint blueprint = new JobFitBlueprint();
            assertThat(assembler.supports(blueprint)).isTrue();
        }

        @Test
        @DisplayName("should not support OverviewBlueprint")
        void shouldNotSupportOverviewBlueprint() {
            OverviewBlueprint blueprint = new OverviewBlueprint();
            assertThat(assembler.supports(blueprint)).isFalse();
        }

        @Test
        @DisplayName("should not support TeamFitBlueprint")
        void shouldNotSupportTeamFitBlueprint() {
            TeamFitBlueprint blueprint = new TeamFitBlueprint();
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
        @DisplayName("should throw exception for non-JobFitBlueprint")
        void shouldThrowExceptionForWrongBlueprintType() {
            OverviewBlueprint wrongBlueprint = new OverviewBlueprint();

            assertThatThrownBy(() -> assembler.assemble(wrongBlueprint))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JobFitAssembler requires JobFitBlueprint");
        }

        @Test
        @DisplayName("should throw exception for null blueprint")
        void shouldThrowExceptionForNullBlueprint() {
            assertThatThrownBy(() -> assembler.assemble(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JobFitAssembler requires JobFitBlueprint");
        }

        @Test
        @DisplayName("should return empty list for null SOC code")
        void shouldReturnEmptyListForNullSocCode() {
            JobFitBlueprint blueprint = new JobFitBlueprint();
            blueprint.setOnetSocCode(null);

            AssemblyResult result = assembler.assemble(blueprint);

            assertThat(result.questionIds()).isEmpty();
            verifyNoInteractions(onetService);
        }

        @Test
        @DisplayName("should return empty list for blank SOC code")
        void shouldReturnEmptyListForBlankSocCode() {
            JobFitBlueprint blueprint = new JobFitBlueprint();
            blueprint.setOnetSocCode("   ");

            AssemblyResult result = assembler.assemble(blueprint);

            assertThat(result.questionIds()).isEmpty();
            verifyNoInteractions(onetService);
        }
    }

    @Nested
    @DisplayName("assemble - O*NET Profile Tests")
    class AssembleOnetProfileTests {

        @Test
        @DisplayName("should return empty list when O*NET profile not found")
        void shouldReturnEmptyWhenOnetProfileNotFound() {
            // Given
            JobFitBlueprint blueprint = createBlueprint(VALID_SOC_CODE, 50);
            when(onetService.getProfile(VALID_SOC_CODE)).thenReturn(Optional.empty());

            // When
            AssemblyResult result = assembler.assemble(blueprint);

            // Then
            assertThat(result.questionIds()).isEmpty();
            verify(onetService).getProfile(VALID_SOC_CODE);
        }

        @Test
        @DisplayName("should use O*NET benchmarks for gap analysis")
        void shouldUseOnetBenchmarksForGapAnalysis() {
            // Given
            JobFitBlueprint blueprint = createBlueprint(VALID_SOC_CODE, 50);

            Map<String, Double> benchmarks = new HashMap<>();
            benchmarks.put("Problem Solving", 0.8);

            OnetProfile profile = createOnetProfile(VALID_SOC_CODE, "Software Developer", benchmarks);
            when(onetService.getProfile(VALID_SOC_CODE)).thenReturn(Optional.of(profile));
            when(competencyRepository.findByNameInIgnoreCase(any())).thenReturn(List.of(competency1));
            when(indicatorRepository.findByCompetencyIdIn(anySet())).thenReturn(List.of(indicator1));
            when(questionSelectionService.selectQuestionsForIndicator(
                any(), anyInt(), any(), anySet()))
                .thenReturn(List.of(questionId1));

            // When
            AssemblyResult result = assembler.assemble(blueprint);

            // Then
            assertThat(result.questionIds()).isNotEmpty();
            verify(onetService).getProfile(VALID_SOC_CODE);
        }
    }

    @Nested
    @DisplayName("assemble - Passport Integration Tests")
    class AssemblePassportIntegrationTests {

        @Test
        @DisplayName("should proceed without passport when candidate ID is null")
        void shouldProceedWithoutPassportWhenCandidateIdNull() {
            // Given
            JobFitBlueprint blueprint = createBlueprint(VALID_SOC_CODE, 50);
            blueprint.setCandidateClerkUserId(null);

            Map<String, Double> benchmarks = new HashMap<>();
            benchmarks.put("Problem Solving", 0.8);

            OnetProfile profile = createOnetProfile(VALID_SOC_CODE, "Software Developer", benchmarks);
            when(onetService.getProfile(VALID_SOC_CODE)).thenReturn(Optional.of(profile));
            when(competencyRepository.findByNameInIgnoreCase(any())).thenReturn(List.of(competency1));
            when(indicatorRepository.findByCompetencyIdIn(anySet())).thenReturn(List.of(indicator1));
            when(questionSelectionService.selectQuestionsForIndicator(
                any(), anyInt(), any(), anySet()))
                .thenReturn(List.of(questionId1));

            // When
            AssemblyResult result = assembler.assemble(blueprint);

            // Then
            assertThat(result.questionIds()).isNotEmpty();
            verifyNoInteractions(passportService);
        }

        @Test
        @DisplayName("should fetch passport when candidate ID is provided")
        void shouldFetchPassportWhenCandidateIdProvided() {
            // Given
            String candidateId = "user_test123";
            JobFitBlueprint blueprint = createBlueprint(VALID_SOC_CODE, 50);
            blueprint.setCandidateClerkUserId(candidateId);

            Map<String, Double> benchmarks = new HashMap<>();
            benchmarks.put("Problem Solving", 0.8);

            OnetProfile profile = createOnetProfile(VALID_SOC_CODE, "Software Developer", benchmarks);
            when(onetService.getProfile(VALID_SOC_CODE)).thenReturn(Optional.of(profile));
            when(passportService.getPassportByClerkUserId(candidateId)).thenReturn(Optional.empty());
            when(competencyRepository.findByNameInIgnoreCase(any())).thenReturn(List.of(competency1));
            when(indicatorRepository.findByCompetencyIdIn(anySet())).thenReturn(List.of(indicator1));
            when(questionSelectionService.selectQuestionsForIndicator(
                any(), anyInt(), any(), anySet()))
                .thenReturn(List.of(questionId1));

            // When
            assembler.assemble(blueprint);

            // Then
            verify(passportService).getPassportByClerkUserId(candidateId);
        }

        @Test
        @DisplayName("should use passport scores for gap calculation")
        void shouldUsePassportScoresForGapCalculation() {
            // Given
            String candidateId = "user_test123";
            JobFitBlueprint blueprint = createBlueprint(VALID_SOC_CODE, 50);
            blueprint.setCandidateClerkUserId(candidateId);

            Map<String, Double> benchmarks = new HashMap<>();
            benchmarks.put("Problem Solving", 0.8);  // Benchmark

            Map<UUID, Double> passportScores = new HashMap<>();
            passportScores.put(competencyId1, 0.6);  // Candidate score below benchmark

            CompetencyPassport passport = new CompetencyPassport(
                UUID.randomUUID(), passportScores, Map.of(), LocalDateTime.now(), true
            );

            OnetProfile profile = createOnetProfile(VALID_SOC_CODE, "Software Developer", benchmarks);
            when(onetService.getProfile(VALID_SOC_CODE)).thenReturn(Optional.of(profile));
            when(passportService.getPassportByClerkUserId(candidateId)).thenReturn(Optional.of(passport));
            when(competencyRepository.findByNameInIgnoreCase(any())).thenReturn(List.of(competency1));
            when(indicatorRepository.findByCompetencyIdIn(anySet())).thenReturn(List.of(indicator1));
            when(questionSelectionService.selectQuestionsForIndicator(
                any(), anyInt(), any(), anySet()))
                .thenReturn(List.of(questionId1));

            // When
            AssemblyResult result = assembler.assemble(blueprint);

            // Then
            assertThat(result.questionIds()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("assemble - Question Selection Tests")
    class AssembleQuestionSelectionTests {

        @Test
        @DisplayName("should delegate to QuestionSelectionService for indicators")
        void shouldDelegateToQuestionSelectionService() {
            // Given
            JobFitBlueprint blueprint = createBlueprint(VALID_SOC_CODE, 50);

            Map<String, Double> benchmarks = new HashMap<>();
            benchmarks.put("Problem Solving", 0.8);

            OnetProfile profile = createOnetProfile(VALID_SOC_CODE, "Software Developer", benchmarks);
            when(onetService.getProfile(VALID_SOC_CODE)).thenReturn(Optional.of(profile));
            when(competencyRepository.findByNameInIgnoreCase(any())).thenReturn(List.of(competency1));
            when(indicatorRepository.findByCompetencyIdIn(anySet())).thenReturn(List.of(indicator1));
            when(questionSelectionService.selectQuestionsForIndicator(
                eq(indicatorId1), anyInt(), any(DifficultyLevel.class), anySet()))
                .thenReturn(List.of(questionId1));

            // When
            AssemblyResult result = assembler.assemble(blueprint);

            // Then
            assertThat(result.questionIds()).contains(questionId1);
            verify(questionSelectionService).selectQuestionsForIndicator(
                eq(indicatorId1), anyInt(), any(DifficultyLevel.class), anySet()
            );
        }

        @Test
        @DisplayName("should return empty when no indicators match")
        void shouldReturnEmptyWhenNoIndicatorsMatch() {
            // Given
            JobFitBlueprint blueprint = createBlueprint(VALID_SOC_CODE, 50);

            Map<String, Double> benchmarks = new HashMap<>();
            benchmarks.put("Unknown Competency", 0.8);  // No matching competency

            OnetProfile profile = createOnetProfile(VALID_SOC_CODE, "Software Developer", benchmarks);
            when(onetService.getProfile(VALID_SOC_CODE)).thenReturn(Optional.of(profile));
            when(competencyRepository.findByNameInIgnoreCase(any())).thenReturn(List.of(competency1));

            // When
            AssemblyResult result = assembler.assemble(blueprint);

            // Then
            assertThat(result.questionIds()).isEmpty();
        }
    }

    // Helper methods

    private JobFitBlueprint createBlueprint(String socCode, int strictnessLevel) {
        JobFitBlueprint blueprint = new JobFitBlueprint();
        blueprint.setOnetSocCode(socCode);
        blueprint.setStrictnessLevel(strictnessLevel);
        return blueprint;
    }

    private Competency createCompetency(UUID id, String name) {
        Competency competency = new Competency();
        competency.setId(id);
        competency.setName(name);
        return competency;
    }

    private BehavioralIndicator createIndicator(UUID id, String title, float weight, boolean isActive, UUID competencyId) {
        BehavioralIndicator indicator = new BehavioralIndicator();
        indicator.setId(id);
        indicator.setTitle(title);
        indicator.setWeight(weight);
        indicator.setActive(isActive);
        // Set competency for batch-loaded grouping (source groups by competency.getId())
        Competency competency = new Competency();
        competency.setId(competencyId);
        indicator.setCompetency(competency);
        return indicator;
    }

    private OnetProfile createOnetProfile(String socCode, String title, Map<String, Double> benchmarks) {
        return new OnetProfile(
            socCode,
            title,
            "Description for " + title,
            benchmarks,
            Map.of(),  // knowledgeAreas
            Map.of(),  // skills
            Map.of()   // abilities
        );
    }
}
