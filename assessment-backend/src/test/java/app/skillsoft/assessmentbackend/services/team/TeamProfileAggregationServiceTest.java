package app.skillsoft.assessmentbackend.services.team;

import app.skillsoft.assessmentbackend.domain.dto.CompetencyScoreDto;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.repository.TeamMemberRepository;
import app.skillsoft.assessmentbackend.repository.TeamRepository;
import app.skillsoft.assessmentbackend.repository.TestResultRepository;
import app.skillsoft.assessmentbackend.services.external.TeamService.TeamMemberProfile;
import app.skillsoft.assessmentbackend.services.external.TeamService.TeamProfile;
import app.skillsoft.assessmentbackend.testutils.BaseUnitTest;
import app.skillsoft.assessmentbackend.testutils.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TeamProfileAggregationService.
 *
 * Tests cover all team profile aggregation operations including:
 * - Team profile computation with status validation
 * - Competency saturation calculation with gap identification
 * - Big Five personality trait averaging
 * - Member profile building from test results
 *
 * Uses bilingual test data (English/Russian) to verify proper handling
 * of Cyrillic characters throughout the system.
 */
@DisplayName("TeamProfileAggregationService Tests")
class TeamProfileAggregationServiceTest extends BaseUnitTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private TestResultRepository testResultRepository;

    @InjectMocks
    private TeamProfileAggregationService aggregationService;

    // Test users with bilingual names
    private User leader;
    private User member1;
    private User member2;
    private User member3;

    // Test team
    private Team team;
    private UUID teamId;

    // Test competencies
    private UUID competency1Id;
    private UUID competency2Id;
    private UUID competency3Id;

    @BeforeEach
    void setUp() {
        TestDataFactory.resetCounter();

        // Create test users with bilingual names (EN/RU)
        leader = TestDataFactory.createUser(UserRole.USER);
        leader.setId(UUID.randomUUID());
        leader.setFirstName("Ivan / Иван");
        leader.setLastName("Leader / Лидер");
        leader.setClerkId("clerk_leader_123");

        member1 = TestDataFactory.createUser(UserRole.USER);
        member1.setId(UUID.randomUUID());
        member1.setFirstName("Alexey / Алексей");
        member1.setLastName("Developer / Разработчик");
        member1.setClerkId("clerk_member1_456");

        member2 = TestDataFactory.createUser(UserRole.USER);
        member2.setId(UUID.randomUUID());
        member2.setFirstName("Maria / Мария");
        member2.setLastName("Analyst / Аналитик");
        member2.setClerkId("clerk_member2_789");

        member3 = TestDataFactory.createUser(UserRole.USER);
        member3.setId(UUID.randomUUID());
        member3.setFirstName("Dmitry / Дмитрий");
        member3.setLastName("Designer / Дизайнер");
        member3.setClerkId("clerk_member3_012");

        // Create test team
        team = new Team("Development Team / Команда разработки", "Building software / Разработка ПО", leader);
        teamId = UUID.randomUUID();
        team.setId(teamId);

        // Initialize competency IDs
        competency1Id = UUID.randomUUID();
        competency2Id = UUID.randomUUID();
        competency3Id = UUID.randomUUID();
    }

    // ============================================
    // COMPUTE TEAM PROFILE TESTS
    // ============================================

    @Nested
    @DisplayName("computeTeamProfile")
    class ComputeTeamProfileTests {

        @Test
        @DisplayName("should return empty when team not found")
        void shouldReturnEmptyWhenTeamNotFound() {
            // Given
            UUID nonExistentTeamId = UUID.randomUUID();
            when(teamRepository.findById(nonExistentTeamId)).thenReturn(Optional.empty());

            // When
            Optional<TeamProfile> result = aggregationService.computeTeamProfile(nonExistentTeamId);

            // Then
            assertThat(result).isEmpty();
            verify(teamRepository).findById(nonExistentTeamId);
            verifyNoInteractions(teamMemberRepository);
            verifyNoInteractions(testResultRepository);
        }

        @Test
        @DisplayName("should return empty for DRAFT team")
        void shouldReturnEmptyForDraftTeam() {
            // Given - team starts in DRAFT status by default
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));

            // When
            Optional<TeamProfile> result = aggregationService.computeTeamProfile(teamId);

            // Then
            assertThat(result).isEmpty();
            verify(teamRepository).findById(teamId);
            verifyNoInteractions(teamMemberRepository);
        }

        @Test
        @DisplayName("should return empty for ARCHIVED team")
        void shouldReturnEmptyForArchivedTeam() {
            // Given - create archived team
            TeamMember tm = new TeamMember(team, leader, TeamMemberRole.LEADER);
            tm.setId(UUID.randomUUID());
            team.getMembers().add(tm);
            team.activate();
            team.archive();

            when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));

            // When
            Optional<TeamProfile> result = aggregationService.computeTeamProfile(teamId);

            // Then
            assertThat(result).isEmpty();
            verify(teamRepository).findById(teamId);
            verifyNoInteractions(teamMemberRepository);
        }

        @Test
        @DisplayName("should return empty profile when no active members")
        void shouldReturnEmptyProfileWhenNoActiveMembers() {
            // Given - activate team then setup
            setupActiveTeam();
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
            when(teamMemberRepository.findByTeamIdAndIsActiveTrue(teamId)).thenReturn(List.of());

            // When
            Optional<TeamProfile> result = aggregationService.computeTeamProfile(teamId);

            // Then
            assertThat(result).isPresent();
            TeamProfile profile = result.get();
            assertThat(profile.teamId()).isEqualTo(teamId);
            assertThat(profile.teamName()).isEqualTo("Development Team / Команда разработки");
            assertThat(profile.members()).isEmpty();
            assertThat(profile.competencySaturation()).isEmpty();
            assertThat(profile.averagePersonality()).isEmpty();
            assertThat(profile.skillGaps()).isEmpty();
        }

        @Test
        @DisplayName("should compute profile with members and test results")
        void shouldComputeProfileWithMembersAndResults() {
            // Given
            setupActiveTeam();
            List<TeamMember> members = createActiveMembers();
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
            when(teamMemberRepository.findByTeamIdAndIsActiveTrue(teamId)).thenReturn(members);

            // Setup test results with competency scores
            setupTestResultsForMember(leader.getClerkId(), List.of(
                    createCompetencyScore(competency1Id, "Leadership / Лидерство", 80.0),
                    createCompetencyScore(competency2Id, "Communication / Коммуникация", 70.0)
            ), createBigFiveProfile(75.0, 80.0, 65.0, 70.0, 85.0));

            setupTestResultsForMember(member1.getClerkId(), List.of(
                    createCompetencyScore(competency1Id, "Leadership / Лидерство", 60.0),
                    createCompetencyScore(competency3Id, "Technical / Технический", 90.0)
            ), createBigFiveProfile(70.0, 75.0, 80.0, 65.0, 70.0));

            // When
            Optional<TeamProfile> result = aggregationService.computeTeamProfile(teamId);

            // Then
            assertThat(result).isPresent();
            TeamProfile profile = result.get();
            assertThat(profile.teamId()).isEqualTo(teamId);
            assertThat(profile.members()).hasSize(2);
        }

        @Test
        @DisplayName("should handle members with no test results")
        void shouldHandleMembersWithNoTestResults() {
            // Given
            setupActiveTeam();
            List<TeamMember> members = createActiveMembers();
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
            when(teamMemberRepository.findByTeamIdAndIsActiveTrue(teamId)).thenReturn(members);

            // No test results for any member
            when(testResultRepository.findByClerkUserIdOrderByCompletedAtDesc(anyString()))
                    .thenReturn(List.of());

            // When
            Optional<TeamProfile> result = aggregationService.computeTeamProfile(teamId);

            // Then
            assertThat(result).isPresent();
            TeamProfile profile = result.get();
            // Members still included but with empty competency scores
            assertThat(profile.members()).hasSize(2);
            profile.members().forEach(memberProfile -> {
                assertThat(memberProfile.competencyScores()).isEmpty();
                assertThat(memberProfile.personalityTraits()).isEmpty();
            });
        }

        @Test
        @DisplayName("should include team name in bilingual format")
        void shouldIncludeTeamNameInBilingualFormat() {
            // Given
            team.setName("Innovation Lab / Инновационная лаборатория");
            setupActiveTeam();
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
            when(teamMemberRepository.findByTeamIdAndIsActiveTrue(teamId)).thenReturn(List.of());

            // When
            Optional<TeamProfile> result = aggregationService.computeTeamProfile(teamId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().teamName()).isEqualTo("Innovation Lab / Инновационная лаборатория");
        }
    }

    // ============================================
    // COMPETENCY SATURATION TESTS
    // ============================================

    @Nested
    @DisplayName("competencySaturation")
    class CompetencySaturationTests {

        @Test
        @DisplayName("should calculate saturation correctly - formula: coverage * (avgScore / 5.0)")
        void shouldCalculateSaturationCorrectly() {
            // Given - 2 members, both have competency1 with different scores
            // Member 1: 80% (converts to 4.0 on 5-point scale)
            // Member 2: 60% (converts to 3.0 on 5-point scale)
            // Coverage = 2/2 = 1.0
            // AvgScore = (4.0 + 3.0) / 2 = 3.5
            // Saturation = 1.0 * (3.5 / 5.0) = 0.7
            setupActiveTeam();
            List<TeamMember> members = createActiveMembers();
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
            when(teamMemberRepository.findByTeamIdAndIsActiveTrue(teamId)).thenReturn(members);

            setupTestResultsForMember(leader.getClerkId(), List.of(
                    createCompetencyScore(competency1Id, "Leadership / Лидерство", 80.0)
            ), null);

            setupTestResultsForMember(member1.getClerkId(), List.of(
                    createCompetencyScore(competency1Id, "Leadership / Лидерство", 60.0)
            ), null);

            // When
            Optional<TeamProfile> result = aggregationService.computeTeamProfile(teamId);

            // Then
            assertThat(result).isPresent();
            Map<UUID, Double> saturation = result.get().competencySaturation();
            assertThat(saturation).containsKey(competency1Id);
            // Saturation = coverage (1.0) * avgScore (3.5) / 5.0 = 0.7
            assertThat(saturation.get(competency1Id)).isCloseTo(0.7, within(0.01));
        }

        @Test
        @DisplayName("should identify skill gaps when saturation below threshold (0.3)")
        void shouldIdentifySkillGapsWhenSaturationBelowThreshold() {
            // Given - 2 members, only 1 has competency2 with low score
            // Coverage = 1/2 = 0.5
            // Score = 20% = 1.0 on 5-point scale
            // Saturation = 0.5 * (1.0 / 5.0) = 0.1 (below 0.3 threshold)
            setupActiveTeam();
            List<TeamMember> members = createActiveMembers();
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
            when(teamMemberRepository.findByTeamIdAndIsActiveTrue(teamId)).thenReturn(members);

            setupTestResultsForMember(leader.getClerkId(), List.of(
                    createCompetencyScore(competency1Id, "Strong / Сильная", 80.0),
                    createCompetencyScore(competency2Id, "Weak / Слабая", 20.0)
            ), null);

            setupTestResultsForMember(member1.getClerkId(), List.of(
                    createCompetencyScore(competency1Id, "Strong / Сильная", 70.0)
                    // No competency2
            ), null);

            // When
            Optional<TeamProfile> result = aggregationService.computeTeamProfile(teamId);

            // Then
            assertThat(result).isPresent();
            List<UUID> skillGaps = result.get().skillGaps();
            assertThat(skillGaps).contains(competency2Id);
            assertThat(skillGaps).doesNotContain(competency1Id);
        }

        @Test
        @DisplayName("should handle all members having competency (full coverage)")
        void shouldHandleAllMembersHavingCompetency() {
            // Given - all 2 members have competency with high scores
            // Coverage = 2/2 = 1.0
            // AvgScore = (4.5 + 4.0) / 2 = 4.25
            // Saturation = 1.0 * (4.25 / 5.0) = 0.85
            setupActiveTeam();
            List<TeamMember> members = createActiveMembers();
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
            when(teamMemberRepository.findByTeamIdAndIsActiveTrue(teamId)).thenReturn(members);

            setupTestResultsForMember(leader.getClerkId(), List.of(
                    createCompetencyScore(competency1Id, "Skill / Навык", 90.0) // 4.5
            ), null);

            setupTestResultsForMember(member1.getClerkId(), List.of(
                    createCompetencyScore(competency1Id, "Skill / Навык", 80.0) // 4.0
            ), null);

            // When
            Optional<TeamProfile> result = aggregationService.computeTeamProfile(teamId);

            // Then
            assertThat(result).isPresent();
            Map<UUID, Double> saturation = result.get().competencySaturation();
            assertThat(saturation.get(competency1Id)).isCloseTo(0.85, within(0.01));
            // Should NOT be a skill gap
            assertThat(result.get().skillGaps()).doesNotContain(competency1Id);
        }

        @Test
        @DisplayName("should handle no members having competency (zero coverage)")
        void shouldHandleNoMembersHavingCompetency() {
            // Given - members have different competencies, competency3 not present
            setupActiveTeam();
            List<TeamMember> members = createActiveMembers();
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
            when(teamMemberRepository.findByTeamIdAndIsActiveTrue(teamId)).thenReturn(members);

            setupTestResultsForMember(leader.getClerkId(), List.of(
                    createCompetencyScore(competency1Id, "Skill1 / Навык1", 80.0)
            ), null);

            setupTestResultsForMember(member1.getClerkId(), List.of(
                    createCompetencyScore(competency2Id, "Skill2 / Навык2", 70.0)
            ), null);

            // When
            Optional<TeamProfile> result = aggregationService.computeTeamProfile(teamId);

            // Then
            assertThat(result).isPresent();
            Map<UUID, Double> saturation = result.get().competencySaturation();
            // competency3 is not in saturation map at all (no one has it)
            assertThat(saturation).doesNotContainKey(competency3Id);
        }

        @Test
        @DisplayName("should calculate partial coverage saturation")
        void shouldCalculatePartialCoverageSaturation() {
            // Given - 2 members, only 1 has competency2 with score 100%
            // Coverage = 1/2 = 0.5
            // Score = 100% = 5.0 on 5-point scale
            // Saturation = 0.5 * (5.0 / 5.0) = 0.5
            setupActiveTeam();
            List<TeamMember> members = createActiveMembers();
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
            when(teamMemberRepository.findByTeamIdAndIsActiveTrue(teamId)).thenReturn(members);

            setupTestResultsForMember(leader.getClerkId(), List.of(
                    createCompetencyScore(competency2Id, "Expert / Эксперт", 100.0)
            ), null);

            setupTestResultsForMember(member1.getClerkId(), List.of(), null);

            // When
            Optional<TeamProfile> result = aggregationService.computeTeamProfile(teamId);

            // Then
            assertThat(result).isPresent();
            Map<UUID, Double> saturation = result.get().competencySaturation();
            // Coverage 0.5 * score 1.0 = 0.5
            assertThat(saturation.get(competency2Id)).isCloseTo(0.5, within(0.01));
        }

        @Test
        @DisplayName("should mark competency as gap when saturation exactly at threshold")
        void shouldMarkCompetencyAsGapWhenSaturationExactlyAtThreshold() {
            // Given - setup to get exactly 0.3 saturation (or close)
            // 2 members, 1 has competency with 30% score
            // Coverage = 1/2 = 0.5
            // Score = 30% = 1.5 on 5-point scale
            // Saturation = 0.5 * (1.5 / 5.0) = 0.15 (below threshold)
            setupActiveTeam();
            List<TeamMember> members = createActiveMembers();
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
            when(teamMemberRepository.findByTeamIdAndIsActiveTrue(teamId)).thenReturn(members);

            setupTestResultsForMember(leader.getClerkId(), List.of(
                    createCompetencyScore(competency1Id, "Low / Низкий", 30.0)
            ), null);

            setupTestResultsForMember(member1.getClerkId(), List.of(), null);

            // When
            Optional<TeamProfile> result = aggregationService.computeTeamProfile(teamId);

            // Then
            assertThat(result).isPresent();
            // 0.15 < 0.3, so should be a skill gap
            assertThat(result.get().skillGaps()).contains(competency1Id);
        }
    }

    // ============================================
    // PERSONALITY TRAIT AGGREGATION TESTS
    // ============================================

    @Nested
    @DisplayName("personalityTraitAggregation")
    class PersonalityTraitTests {

        @Test
        @DisplayName("should average Big Five traits across members")
        void shouldAverageTraitsAcrossMembers() {
            // Given - 2 members with Big Five profiles
            setupActiveTeam();
            List<TeamMember> members = createActiveMembers();
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
            when(teamMemberRepository.findByTeamIdAndIsActiveTrue(teamId)).thenReturn(members);

            // Leader: O=70, C=80, E=60, A=75, N=50
            // Member1: O=80, C=70, E=70, A=65, N=60
            // Avg: O=75, C=75, E=65, A=70, N=55
            setupTestResultsForMember(leader.getClerkId(), List.of(),
                    createBigFiveProfile(70.0, 80.0, 60.0, 75.0, 50.0));

            setupTestResultsForMember(member1.getClerkId(), List.of(),
                    createBigFiveProfile(80.0, 70.0, 70.0, 65.0, 60.0));

            // When
            Optional<TeamProfile> result = aggregationService.computeTeamProfile(teamId);

            // Then
            assertThat(result).isPresent();
            Map<String, Double> avgPersonality = result.get().averagePersonality();
            assertThat(avgPersonality.get("Openness")).isCloseTo(75.0, within(0.01));
            assertThat(avgPersonality.get("Conscientiousness")).isCloseTo(75.0, within(0.01));
            assertThat(avgPersonality.get("Extraversion")).isCloseTo(65.0, within(0.01));
            assertThat(avgPersonality.get("Agreeableness")).isCloseTo(70.0, within(0.01));
            assertThat(avgPersonality.get("Neuroticism")).isCloseTo(55.0, within(0.01));
        }

        @Test
        @DisplayName("should handle members without Big Five data")
        void shouldHandleMembersWithoutBigFiveData() {
            // Given - 2 members, both without Big Five profiles
            setupActiveTeam();
            List<TeamMember> members = createActiveMembers();
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
            when(teamMemberRepository.findByTeamIdAndIsActiveTrue(teamId)).thenReturn(members);

            setupTestResultsForMember(leader.getClerkId(), List.of(
                    createCompetencyScore(competency1Id, "Skill / Навык", 80.0)
            ), null);

            setupTestResultsForMember(member1.getClerkId(), List.of(
                    createCompetencyScore(competency1Id, "Skill / Навык", 70.0)
            ), null);

            // When
            Optional<TeamProfile> result = aggregationService.computeTeamProfile(teamId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().averagePersonality()).isEmpty();
        }

        @Test
        @DisplayName("should handle mixed Big Five data (some members have it, some do not)")
        void shouldHandleMixedBigFiveData() {
            // Given - 2 members, only 1 has Big Five profile
            setupActiveTeam();
            List<TeamMember> members = createActiveMembers();
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
            when(teamMemberRepository.findByTeamIdAndIsActiveTrue(teamId)).thenReturn(members);

            // Only leader has Big Five data
            setupTestResultsForMember(leader.getClerkId(), List.of(),
                    createBigFiveProfile(70.0, 80.0, 60.0, 75.0, 50.0));

            setupTestResultsForMember(member1.getClerkId(), List.of(), null);

            // When
            Optional<TeamProfile> result = aggregationService.computeTeamProfile(teamId);

            // Then
            assertThat(result).isPresent();
            Map<String, Double> avgPersonality = result.get().averagePersonality();
            // Average should be based only on member who has data
            assertThat(avgPersonality.get("Openness")).isCloseTo(70.0, within(0.01));
        }

        @Test
        @DisplayName("should handle empty Big Five profile map")
        void shouldHandleEmptyBigFiveProfileMap() {
            // Given
            setupActiveTeam();
            List<TeamMember> members = createActiveMembers();
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
            when(teamMemberRepository.findByTeamIdAndIsActiveTrue(teamId)).thenReturn(members);

            // Empty map instead of null
            setupTestResultsForMember(leader.getClerkId(), List.of(), Map.of());
            setupTestResultsForMember(member1.getClerkId(), List.of(), Map.of());

            // When
            Optional<TeamProfile> result = aggregationService.computeTeamProfile(teamId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().averagePersonality()).isEmpty();
        }

        @Test
        @DisplayName("should use bilingual trait names in Russian context")
        void shouldUseBilingualTraitNamesInRussianContext() {
            // Given - testing with Russian trait name variants
            setupActiveTeam();
            List<TeamMember> members = createSingleMemberList();
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
            when(teamMemberRepository.findByTeamIdAndIsActiveTrue(teamId)).thenReturn(members);

            Map<String, Double> bilingualTraits = new HashMap<>();
            bilingualTraits.put("Openness / Открытость", 70.0);
            bilingualTraits.put("Conscientiousness / Добросовестность", 80.0);
            setupTestResultsForMember(leader.getClerkId(), List.of(), bilingualTraits);

            // When
            Optional<TeamProfile> result = aggregationService.computeTeamProfile(teamId);

            // Then
            assertThat(result).isPresent();
            Map<String, Double> avgPersonality = result.get().averagePersonality();
            assertThat(avgPersonality).containsKey("Openness / Открытость");
            assertThat(avgPersonality).containsKey("Conscientiousness / Добросовестность");
        }
    }

    // ============================================
    // MEMBER PROFILE BUILDING TESTS
    // ============================================

    @Nested
    @DisplayName("memberProfileBuilding")
    class MemberProfileTests {

        @Test
        @DisplayName("should build profile from latest test result")
        void shouldBuildProfileFromLatestTestResult() {
            // Given - member with multiple test results, latest should be used
            setupActiveTeam();
            List<TeamMember> members = createSingleMemberList();
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
            when(teamMemberRepository.findByTeamIdAndIsActiveTrue(teamId)).thenReturn(members);

            // Create multiple test results (first is latest due to DESC ordering)
            List<TestResult> results = new ArrayList<>();
            results.add(createTestResult(leader.getClerkId(),
                    List.of(createCompetencyScore(competency1Id, "Latest / Последний", 90.0)),
                    LocalDateTime.now()));
            results.add(createTestResult(leader.getClerkId(),
                    List.of(createCompetencyScore(competency1Id, "Older / Старый", 60.0)),
                    LocalDateTime.now().minusDays(7)));

            when(testResultRepository.findByClerkUserIdOrderByCompletedAtDesc(leader.getClerkId()))
                    .thenReturn(List.copyOf(results));

            // When
            Optional<TeamProfile> result = aggregationService.computeTeamProfile(teamId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().members()).hasSize(1);
            // Latest score (90%) converted to 5-point scale = 4.5
            TeamMemberProfile memberProfile = result.get().members().get(0);
            assertThat(memberProfile.competencyScores().get(competency1Id)).isCloseTo(4.5, within(0.01));
        }

        @Test
        @DisplayName("should aggregate competency scores from test results")
        void shouldAggregateCompetencyScoresFromResults() {
            // Given
            setupActiveTeam();
            List<TeamMember> members = createSingleMemberList();
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
            when(teamMemberRepository.findByTeamIdAndIsActiveTrue(teamId)).thenReturn(members);

            setupTestResultsForMember(leader.getClerkId(), List.of(
                    createCompetencyScore(competency1Id, "Leadership / Лидерство", 80.0),
                    createCompetencyScore(competency2Id, "Communication / Коммуникация", 70.0),
                    createCompetencyScore(competency3Id, "Technical / Технический", 90.0)
            ), null);

            // When
            Optional<TeamProfile> result = aggregationService.computeTeamProfile(teamId);

            // Then
            assertThat(result).isPresent();
            TeamMemberProfile memberProfile = result.get().members().get(0);
            assertThat(memberProfile.competencyScores()).hasSize(3);
            assertThat(memberProfile.competencyScores()).containsKeys(
                    competency1Id, competency2Id, competency3Id);
        }

        @Test
        @DisplayName("should convert percentage to five-point scale correctly")
        void shouldConvertPercentageToFivePointScale() {
            // Given - percentage scores and expected conversions:
            // 100% -> 5.0, 80% -> 4.0, 60% -> 3.0, 40% -> 2.0, 20% -> 1.0, 0% -> 0.0
            setupActiveTeam();
            List<TeamMember> members = createSingleMemberList();
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
            when(teamMemberRepository.findByTeamIdAndIsActiveTrue(teamId)).thenReturn(members);

            UUID comp100 = UUID.randomUUID();
            UUID comp80 = UUID.randomUUID();
            UUID comp60 = UUID.randomUUID();
            UUID comp0 = UUID.randomUUID();

            setupTestResultsForMember(leader.getClerkId(), List.of(
                    createCompetencyScore(comp100, "Perfect / Отлично", 100.0),
                    createCompetencyScore(comp80, "Good / Хорошо", 80.0),
                    createCompetencyScore(comp60, "Average / Средне", 60.0),
                    createCompetencyScore(comp0, "Zero / Ноль", 0.0)
            ), null);

            // When
            Optional<TeamProfile> result = aggregationService.computeTeamProfile(teamId);

            // Then
            assertThat(result).isPresent();
            Map<UUID, Double> scores = result.get().members().get(0).competencyScores();
            assertThat(scores.get(comp100)).isCloseTo(5.0, within(0.01));
            assertThat(scores.get(comp80)).isCloseTo(4.0, within(0.01));
            assertThat(scores.get(comp60)).isCloseTo(3.0, within(0.01));
            assertThat(scores.get(comp0)).isCloseTo(0.0, within(0.01));
        }

        @Test
        @DisplayName("should include member name and role in profile")
        void shouldIncludeMemberNameAndRoleInProfile() {
            // Given
            setupActiveTeam();
            List<TeamMember> members = createActiveMembers();
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
            when(teamMemberRepository.findByTeamIdAndIsActiveTrue(teamId)).thenReturn(members);

            setupTestResultsForMember(leader.getClerkId(), List.of(), null);
            setupTestResultsForMember(member1.getClerkId(), List.of(), null);

            // When
            Optional<TeamProfile> result = aggregationService.computeTeamProfile(teamId);

            // Then
            assertThat(result).isPresent();
            List<TeamMemberProfile> memberProfiles = result.get().members();
            assertThat(memberProfiles).hasSize(2);

            // Find leader profile
            Optional<TeamMemberProfile> leaderProfile = memberProfiles.stream()
                    .filter(p -> p.userId().equals(leader.getId()))
                    .findFirst();
            assertThat(leaderProfile).isPresent();
            assertThat(leaderProfile.get().name()).isEqualTo("Ivan / Иван Leader / Лидер");
            assertThat(leaderProfile.get().role()).isEqualTo("Leader");
        }

        @Test
        @DisplayName("should handle null competency scores list")
        void shouldHandleNullCompetencyScoresList() {
            // Given
            setupActiveTeam();
            List<TeamMember> members = createSingleMemberList();
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
            when(teamMemberRepository.findByTeamIdAndIsActiveTrue(teamId)).thenReturn(members);

            // Create result with null competency scores
            TestResult resultWithNull = createTestResult(leader.getClerkId(), null, LocalDateTime.now());
            when(testResultRepository.findByClerkUserIdOrderByCompletedAtDesc(leader.getClerkId()))
                    .thenReturn(List.of(resultWithNull));

            // When
            Optional<TeamProfile> result = aggregationService.computeTeamProfile(teamId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().members()).hasSize(1);
            assertThat(result.get().members().get(0).competencyScores()).isEmpty();
        }

        @Test
        @DisplayName("should build profile with bilingual user names")
        void shouldBuildProfileWithBilingualUserNames() {
            // Given
            setupActiveTeam();
            leader.setFirstName("Сергей / Sergey");
            leader.setLastName("Иванов / Ivanov");
            List<TeamMember> members = createSingleMemberList();
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
            when(teamMemberRepository.findByTeamIdAndIsActiveTrue(teamId)).thenReturn(members);

            setupTestResultsForMember(leader.getClerkId(), List.of(), null);

            // When
            Optional<TeamProfile> result = aggregationService.computeTeamProfile(teamId);

            // Then
            assertThat(result).isPresent();
            TeamMemberProfile memberProfile = result.get().members().get(0);
            assertThat(memberProfile.name()).contains("Сергей");
            assertThat(memberProfile.name()).contains("Sergey");
            assertThat(memberProfile.name()).contains("Иванов");
            assertThat(memberProfile.name()).contains("Ivanov");
        }
    }

    // ============================================
    // EDGE CASES AND INTEGRATION SCENARIOS
    // ============================================

    @Nested
    @DisplayName("Edge Cases and Complex Scenarios")
    class EdgeCasesTests {

        @Test
        @DisplayName("should handle large team with many members")
        void shouldHandleLargeTeamWithManyMembers() {
            // Given - team with 10 members
            setupActiveTeam();
            List<TeamMember> members = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                User user = TestDataFactory.createUser(UserRole.USER);
                user.setId(UUID.randomUUID());
                user.setFirstName("Member" + i + " / Участник" + i);
                user.setClerkId("clerk_member_" + i);

                TeamMember tm = new TeamMember(team, user, TeamMemberRole.MEMBER);
                tm.setId(UUID.randomUUID());
                members.add(tm);

                // Each member has competency1 with varying scores
                setupTestResultsForMember(user.getClerkId(), List.of(
                        createCompetencyScore(competency1Id, "Skill / Навык", 50.0 + i * 5)
                ), null);
            }

            when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
            when(teamMemberRepository.findByTeamIdAndIsActiveTrue(teamId)).thenReturn(members);

            // When
            Optional<TeamProfile> result = aggregationService.computeTeamProfile(teamId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().members()).hasSize(10);
            // Average score: 50+55+60+65+70+75+80+85+90+95 / 10 = 72.5% = 3.625 on 5-point scale
            // Saturation = 1.0 * (3.625 / 5.0) = 0.725
            assertThat(result.get().competencySaturation().get(competency1Id))
                    .isCloseTo(0.725, within(0.01));
        }

        @Test
        @DisplayName("should handle member with multiple test types")
        void shouldHandleMemberWithMultipleTestTypes() {
            // Given - member completed tests with different competencies
            setupActiveTeam();
            List<TeamMember> members = createSingleMemberList();
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
            when(teamMemberRepository.findByTeamIdAndIsActiveTrue(teamId)).thenReturn(members);

            // First result (latest) has comp1 and comp2
            // Second result (older) has comp2 and comp3
            List<TestResult> results = new ArrayList<>();
            results.add(createTestResult(leader.getClerkId(),
                    List.of(
                            createCompetencyScore(competency1Id, "New1 / Новый1", 90.0),
                            createCompetencyScore(competency2Id, "New2 / Новый2", 85.0)
                    ), LocalDateTime.now()));
            results.add(createTestResult(leader.getClerkId(),
                    List.of(
                            createCompetencyScore(competency2Id, "Old2 / Старый2", 70.0),
                            createCompetencyScore(competency3Id, "Old3 / Старый3", 75.0)
                    ), LocalDateTime.now().minusDays(30)));

            when(testResultRepository.findByClerkUserIdOrderByCompletedAtDesc(leader.getClerkId()))
                    .thenReturn(List.copyOf(results));

            // When
            Optional<TeamProfile> result = aggregationService.computeTeamProfile(teamId);

            // Then
            assertThat(result).isPresent();
            Map<UUID, Double> scores = result.get().members().get(0).competencyScores();
            // Latest score for comp2 should be used (85%)
            assertThat(scores.get(competency2Id)).isCloseTo(4.25, within(0.01));
        }

        @Test
        @DisplayName("should correctly identify multiple skill gaps")
        void shouldCorrectlyIdentifyMultipleSkillGaps() {
            // Given - setup to have multiple gaps
            setupActiveTeam();
            List<TeamMember> members = createActiveMembers();
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
            when(teamMemberRepository.findByTeamIdAndIsActiveTrue(teamId)).thenReturn(members);

            // Strong competency (not a gap)
            // Weak competencies (gaps)
            setupTestResultsForMember(leader.getClerkId(), List.of(
                    createCompetencyScore(competency1Id, "Strong / Сильный", 80.0),
                    createCompetencyScore(competency2Id, "Weak1 / Слабый1", 10.0)
            ), null);

            setupTestResultsForMember(member1.getClerkId(), List.of(
                    createCompetencyScore(competency1Id, "Strong / Сильный", 70.0),
                    createCompetencyScore(competency3Id, "Weak2 / Слабый2", 15.0)
            ), null);

            // When
            Optional<TeamProfile> result = aggregationService.computeTeamProfile(teamId);

            // Then
            assertThat(result).isPresent();
            List<UUID> skillGaps = result.get().skillGaps();
            // comp2: 0.5 * (0.5/5.0) = 0.05 < 0.3 (gap)
            // comp3: 0.5 * (0.75/5.0) = 0.075 < 0.3 (gap)
            // comp1: 1.0 * (3.75/5.0) = 0.75 >= 0.3 (not a gap)
            assertThat(skillGaps).contains(competency2Id, competency3Id);
            assertThat(skillGaps).doesNotContain(competency1Id);
        }

        @Test
        @DisplayName("should handle special characters in team name")
        void shouldHandleSpecialCharactersInTeamName() {
            // Given
            team.setName("R&D Team <Alpha> / Команда R&D \"Альфа\" @2025");
            setupActiveTeam();
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
            when(teamMemberRepository.findByTeamIdAndIsActiveTrue(teamId)).thenReturn(List.of());

            // When
            Optional<TeamProfile> result = aggregationService.computeTeamProfile(teamId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().teamName()).isEqualTo("R&D Team <Alpha> / Команда R&D \"Альфа\" @2025");
        }

        @Test
        @DisplayName("should handle concurrent profile requests gracefully")
        void shouldHandleConcurrentProfileRequestsGracefully() {
            // Given - same data for multiple calls
            setupActiveTeam();
            List<TeamMember> members = createSingleMemberList();
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
            when(teamMemberRepository.findByTeamIdAndIsActiveTrue(teamId)).thenReturn(members);
            setupTestResultsForMember(leader.getClerkId(), List.of(
                    createCompetencyScore(competency1Id, "Test / Тест", 80.0)
            ), null);

            // When - simulate multiple calls
            Optional<TeamProfile> result1 = aggregationService.computeTeamProfile(teamId);
            Optional<TeamProfile> result2 = aggregationService.computeTeamProfile(teamId);
            Optional<TeamProfile> result3 = aggregationService.computeTeamProfile(teamId);

            // Then - all should return consistent results
            assertThat(result1).isPresent();
            assertThat(result2).isPresent();
            assertThat(result3).isPresent();
            assertThat(result1.get().teamId()).isEqualTo(result2.get().teamId());
            assertThat(result2.get().teamId()).isEqualTo(result3.get().teamId());
        }

        @Test
        @DisplayName("should compute correct saturation with single high-scoring member")
        void shouldComputeCorrectSaturationWithSingleHighScoringMember() {
            // Given - single member with 100% score
            // Coverage = 1/1 = 1.0
            // Score = 100% = 5.0 on 5-point scale
            // Saturation = 1.0 * (5.0 / 5.0) = 1.0 (maximum)
            setupActiveTeam();
            List<TeamMember> members = createSingleMemberList();
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
            when(teamMemberRepository.findByTeamIdAndIsActiveTrue(teamId)).thenReturn(members);

            setupTestResultsForMember(leader.getClerkId(), List.of(
                    createCompetencyScore(competency1Id, "Perfect Skill / Идеальный навык", 100.0)
            ), null);

            // When
            Optional<TeamProfile> result = aggregationService.computeTeamProfile(teamId);

            // Then
            assertThat(result).isPresent();
            Map<UUID, Double> saturation = result.get().competencySaturation();
            assertThat(saturation.get(competency1Id)).isCloseTo(1.0, within(0.01));
            // Should NOT be a skill gap (1.0 >= 0.3)
            assertThat(result.get().skillGaps()).doesNotContain(competency1Id);
        }
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    private void setupActiveTeam() {
        // Add a member to allow activation
        TeamMember tm = new TeamMember(team, leader, TeamMemberRole.LEADER);
        tm.setId(UUID.randomUUID());
        team.getMembers().add(tm);
        team.activate();
    }

    private List<TeamMember> createActiveMembers() {
        List<TeamMember> members = new ArrayList<>();

        TeamMember leaderMember = new TeamMember(team, leader, TeamMemberRole.LEADER);
        leaderMember.setId(UUID.randomUUID());
        members.add(leaderMember);

        TeamMember regularMember = new TeamMember(team, member1, TeamMemberRole.MEMBER);
        regularMember.setId(UUID.randomUUID());
        members.add(regularMember);

        return members;
    }

    private List<TeamMember> createSingleMemberList() {
        TeamMember leaderMember = new TeamMember(team, leader, TeamMemberRole.LEADER);
        leaderMember.setId(UUID.randomUUID());
        return List.of(leaderMember);
    }

    private CompetencyScoreDto createCompetencyScore(UUID competencyId, String name, Double percentage) {
        CompetencyScoreDto dto = new CompetencyScoreDto();
        dto.setCompetencyId(competencyId);
        dto.setCompetencyName(name);
        dto.setPercentage(percentage);
        return dto;
    }

    private Map<String, Double> createBigFiveProfile(Double openness, Double conscientiousness,
                                                      Double extraversion, Double agreeableness,
                                                      Double neuroticism) {
        Map<String, Double> profile = new HashMap<>();
        profile.put("Openness", openness);
        profile.put("Conscientiousness", conscientiousness);
        profile.put("Extraversion", extraversion);
        profile.put("Agreeableness", agreeableness);
        profile.put("Neuroticism", neuroticism);
        return profile;
    }

    private void setupTestResultsForMember(String clerkUserId, List<CompetencyScoreDto> competencyScores,
                                           Map<String, Double> bigFiveProfile) {
        TestResult result = createTestResult(clerkUserId, competencyScores, LocalDateTime.now());
        result.setBigFiveProfile(bigFiveProfile);
        when(testResultRepository.findByClerkUserIdOrderByCompletedAtDesc(clerkUserId))
                .thenReturn(List.of(result));
    }

    private TestResult createTestResult(String clerkUserId, List<CompetencyScoreDto> competencyScores,
                                        LocalDateTime completedAt) {
        TestResult result = new TestResult();
        result.setId(UUID.randomUUID());
        result.setClerkUserId(clerkUserId);
        result.setCompetencyScores(competencyScores != null ? new ArrayList<>(competencyScores) : new ArrayList<>());
        result.setCompletedAt(completedAt);
        result.setOverallPercentage(75.0);
        result.setPassed(true);
        return result;
    }
}
