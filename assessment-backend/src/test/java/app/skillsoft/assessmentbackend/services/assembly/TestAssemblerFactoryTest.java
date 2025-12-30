package app.skillsoft.assessmentbackend.services.assembly;

import app.skillsoft.assessmentbackend.domain.dto.blueprint.JobFitBlueprint;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.OverviewBlueprint;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.TeamFitBlueprint;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.TestBlueprintDto;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TestAssemblerFactory.
 *
 * Tests cover:
 * - Factory initialization with @PostConstruct
 * - Correct assembler selection by blueprint
 * - Correct assembler selection by goal
 * - Error handling for null inputs
 * - Error handling for unsupported strategies
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TestAssemblerFactory Tests")
class TestAssemblerFactoryTest {

    @Mock
    private TestAssembler overviewAssembler;

    @Mock
    private TestAssembler jobFitAssembler;

    @Mock
    private TestAssembler teamFitAssembler;

    private TestAssemblerFactory factory;

    @BeforeEach
    void setUp() {
        // Configure mock assemblers to return their supported goals
        when(overviewAssembler.getSupportedGoal()).thenReturn(AssessmentGoal.OVERVIEW);
        when(jobFitAssembler.getSupportedGoal()).thenReturn(AssessmentGoal.JOB_FIT);
        when(teamFitAssembler.getSupportedGoal()).thenReturn(AssessmentGoal.TEAM_FIT);

        // Create factory with all assemblers
        factory = new TestAssemblerFactory(List.of(overviewAssembler, jobFitAssembler, teamFitAssembler));
        factory.initializeAssemblers();
    }

    @Nested
    @DisplayName("Initialization Tests")
    class InitializationTests {

        @Test
        @DisplayName("should initialize with all assemblers registered")
        void shouldInitializeWithAllAssemblers() {
            // Then
            assertThat(factory.getAllAssemblers()).hasSize(3);
            assertThat(factory.hasAssembler(AssessmentGoal.OVERVIEW)).isTrue();
            assertThat(factory.hasAssembler(AssessmentGoal.JOB_FIT)).isTrue();
            assertThat(factory.hasAssembler(AssessmentGoal.TEAM_FIT)).isTrue();
        }

        @Test
        @DisplayName("should throw exception for duplicate assemblers")
        void shouldThrowExceptionForDuplicateAssemblers() {
            // Given
            TestAssembler duplicateOverviewAssembler = mock(TestAssembler.class);
            when(duplicateOverviewAssembler.getSupportedGoal()).thenReturn(AssessmentGoal.OVERVIEW);

            TestAssemblerFactory duplicateFactory = new TestAssemblerFactory(
                List.of(overviewAssembler, duplicateOverviewAssembler)
            );

            // When/Then
            assertThatThrownBy(duplicateFactory::initializeAssemblers)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate assembler for goal");
        }

        @Test
        @DisplayName("should handle empty assembler list")
        void shouldHandleEmptyAssemblerList() {
            // Given
            TestAssemblerFactory emptyFactory = new TestAssemblerFactory(List.of());
            emptyFactory.initializeAssemblers();

            // Then
            assertThat(emptyFactory.getAllAssemblers()).isEmpty();
            assertThat(emptyFactory.hasAssembler(AssessmentGoal.OVERVIEW)).isFalse();
        }
    }

    @Nested
    @DisplayName("Get Assembler by Blueprint Tests")
    class GetAssemblerByBlueprintTests {

        @Test
        @DisplayName("should return OverviewAssembler for OverviewBlueprint")
        void shouldReturnOverviewAssemblerForOverviewBlueprint() {
            // Given
            OverviewBlueprint blueprint = new OverviewBlueprint();
            blueprint.setCompetencyIds(List.of(UUID.randomUUID()));

            // When
            TestAssembler result = factory.getAssembler(blueprint);

            // Then
            assertThat(result).isSameAs(overviewAssembler);
        }

        @Test
        @DisplayName("should return JobFitAssembler for JobFitBlueprint")
        void shouldReturnJobFitAssemblerForJobFitBlueprint() {
            // Given
            JobFitBlueprint blueprint = new JobFitBlueprint();
            blueprint.setOnetSocCode("15-1132.00");

            // When
            TestAssembler result = factory.getAssembler(blueprint);

            // Then
            assertThat(result).isSameAs(jobFitAssembler);
        }

        @Test
        @DisplayName("should return TeamFitAssembler for TeamFitBlueprint")
        void shouldReturnTeamFitAssemblerForTeamFitBlueprint() {
            // Given
            TeamFitBlueprint blueprint = new TeamFitBlueprint();
            blueprint.setTeamId(UUID.randomUUID());

            // When
            TestAssembler result = factory.getAssembler(blueprint);

            // Then
            assertThat(result).isSameAs(teamFitAssembler);
        }

        @Test
        @DisplayName("should throw exception for null blueprint")
        void shouldThrowExceptionForNullBlueprint() {
            // When/Then
            assertThatThrownBy(() -> factory.getAssembler((TestBlueprintDto) null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Blueprint cannot be null");
        }

        @Test
        @DisplayName("should throw exception for blueprint with null strategy")
        void shouldThrowExceptionForBlueprintWithNullStrategy() {
            // Given - create a mock blueprint that returns null strategy
            TestBlueprintDto mockBlueprint = mock(TestBlueprintDto.class);
            when(mockBlueprint.getStrategy()).thenReturn(null);

            // When/Then
            assertThatThrownBy(() -> factory.getAssembler(mockBlueprint))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Blueprint strategy cannot be null");
        }
    }

    @Nested
    @DisplayName("Get Assembler by Goal Tests")
    class GetAssemblerByGoalTests {

        @Test
        @DisplayName("should return correct assembler for OVERVIEW goal")
        void shouldReturnCorrectAssemblerForOverviewGoal() {
            // When
            TestAssembler result = factory.getAssembler(AssessmentGoal.OVERVIEW);

            // Then
            assertThat(result).isSameAs(overviewAssembler);
        }

        @Test
        @DisplayName("should return correct assembler for JOB_FIT goal")
        void shouldReturnCorrectAssemblerForJobFitGoal() {
            // When
            TestAssembler result = factory.getAssembler(AssessmentGoal.JOB_FIT);

            // Then
            assertThat(result).isSameAs(jobFitAssembler);
        }

        @Test
        @DisplayName("should return correct assembler for TEAM_FIT goal")
        void shouldReturnCorrectAssemblerForTeamFitGoal() {
            // When
            TestAssembler result = factory.getAssembler(AssessmentGoal.TEAM_FIT);

            // Then
            assertThat(result).isSameAs(teamFitAssembler);
        }

        @Test
        @DisplayName("should throw exception for null goal")
        void shouldThrowExceptionForNullGoal() {
            // When/Then
            assertThatThrownBy(() -> factory.getAssembler((AssessmentGoal) null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Goal cannot be null");
        }

        @Test
        @DisplayName("should throw exception for unregistered goal")
        void shouldThrowExceptionForUnregisteredGoal() {
            // Given - factory with only overview assembler
            TestAssemblerFactory limitedFactory = new TestAssemblerFactory(List.of(overviewAssembler));
            limitedFactory.initializeAssemblers();

            // When/Then
            assertThatThrownBy(() -> limitedFactory.getAssembler(AssessmentGoal.JOB_FIT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No assembler found for goal");
        }
    }

    @Nested
    @DisplayName("Has Assembler Tests")
    class HasAssemblerTests {

        @Test
        @DisplayName("should return true for registered goals")
        void shouldReturnTrueForRegisteredGoals() {
            assertThat(factory.hasAssembler(AssessmentGoal.OVERVIEW)).isTrue();
            assertThat(factory.hasAssembler(AssessmentGoal.JOB_FIT)).isTrue();
            assertThat(factory.hasAssembler(AssessmentGoal.TEAM_FIT)).isTrue();
        }

        @Test
        @DisplayName("should return false for null goal")
        void shouldReturnFalseForNullGoal() {
            assertThat(factory.hasAssembler(null)).isFalse();
        }

        @Test
        @DisplayName("should return false for unregistered goal")
        void shouldReturnFalseForUnregisteredGoal() {
            // Given - factory with only overview assembler
            TestAssemblerFactory limitedFactory = new TestAssemblerFactory(List.of(overviewAssembler));
            limitedFactory.initializeAssemblers();

            // Then
            assertThat(limitedFactory.hasAssembler(AssessmentGoal.JOB_FIT)).isFalse();
        }
    }

    @Nested
    @DisplayName("Get All Assemblers Tests")
    class GetAllAssemblersTests {

        @Test
        @DisplayName("should return immutable copy of assemblers")
        void shouldReturnImmutableCopyOfAssemblers() {
            // When
            List<TestAssembler> assemblers = factory.getAllAssemblers();

            // Then
            assertThat(assemblers).hasSize(3);
            assertThatThrownBy(() -> assemblers.add(mock(TestAssembler.class)))
                .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
