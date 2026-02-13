package app.skillsoft.assessmentbackend.services.team;

import app.skillsoft.assessmentbackend.domain.dto.team.*;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.repository.TeamMemberRepository;
import app.skillsoft.assessmentbackend.repository.TeamRepository;
import app.skillsoft.assessmentbackend.repository.UserRepository;
import app.skillsoft.assessmentbackend.testutils.BaseUnitTest;
import app.skillsoft.assessmentbackend.testutils.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TeamOrchestrationServiceImpl.
 *
 * Tests cover all team orchestration operations including:
 * - Team creation with saga pattern and compensation
 * - Member addition with partial success handling
 * - Leader change with promotion/demotion
 * - Team activation with validation
 * - Team archiving with member deactivation
 *
 * Uses bilingual test data (English/Russian) to verify proper handling
 * of Cyrillic characters throughout the system.
 */
@DisplayName("TeamOrchestrationService Tests")
class TeamOrchestrationServiceTest extends BaseUnitTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TeamOrchestrationServiceImpl orchestrationService;

    private User creator;
    private User member1;
    private User member2;
    private User member3;
    private User inactiveMember;
    private Team team;

    @BeforeEach
    void setUp() {
        TestDataFactory.resetCounter();

        // Create test users with bilingual names (EN/RU)
        creator = TestDataFactory.createUser(UserRole.ADMIN);
        creator.setId(UUID.randomUUID());
        creator.setFirstName("Creator / Создатель");

        member1 = TestDataFactory.createUser(UserRole.USER);
        member1.setId(UUID.randomUUID());
        member1.setFirstName("Алексей / Alexey");

        member2 = TestDataFactory.createUser(UserRole.USER);
        member2.setId(UUID.randomUUID());
        member2.setFirstName("Мария / Maria");

        member3 = TestDataFactory.createUser(UserRole.USER);
        member3.setId(UUID.randomUUID());
        member3.setFirstName("Дмитрий / Dmitry");

        inactiveMember = TestDataFactory.createUser(UserRole.USER);
        inactiveMember.setId(UUID.randomUUID());
        inactiveMember.setFirstName("Inactive / Неактивный");
        inactiveMember.setActive(false);

        // Create test team
        team = TestDataFactory.createTeam(creator);
        team.setId(UUID.randomUUID());
        team.setName("Команда разработки / Development Team");
    }

    // ============================================
    // CREATE TEAM WITH MEMBERS TESTS
    // ============================================

    @Nested
    @DisplayName("createTeamWithMembers")
    class CreateTeamWithMembersTests {

        @Test
        @DisplayName("should create team with all members successfully")
        void shouldCreateTeamWithAllMembersSuccessfully() {
            // Given
            CreateTeamCommand command = new CreateTeamCommand(
                    "Новая команда / New Team",
                    "Описание / Description",
                    List.of(member1.getId(), member2.getId()),
                    member1.getId(),
                    false,
                    creator.getId()
            );

            when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
            when(userRepository.findById(member1.getId())).thenReturn(Optional.of(member1));
            when(userRepository.findById(member2.getId())).thenReturn(Optional.of(member2));
            when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
                Team savedTeam = invocation.getArgument(0);
                if (savedTeam.getId() == null) {
                    savedTeam.setId(UUID.randomUUID());
                }
                return savedTeam;
            });
            when(teamMemberRepository.save(any(TeamMember.class))).thenAnswer(invocation -> {
                TeamMember member = invocation.getArgument(0);
                member.setId(UUID.randomUUID());
                return member;
            });
            when(teamMemberRepository.findByTeamIdAndUserId(any(UUID.class), eq(member1.getId())))
                    .thenAnswer(invocation -> {
                        TeamMember tm = new TeamMember(team, member1, TeamMemberRole.MEMBER);
                        tm.setId(UUID.randomUUID());
                        return Optional.of(tm);
                    });

            // When
            TeamCreationResult result = orchestrationService.createTeamWithMembers(command);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getTeam()).isNotNull();
            assertThat(result.getTeam().getName()).isEqualTo("Новая команда / New Team");
            assertThat(result.getWarnings()).isEmpty();

            verify(teamRepository, times(2)).save(any(Team.class));
            verify(teamMemberRepository, times(2)).save(any(TeamMember.class));
        }

        @Test
        @DisplayName("should create team with partial success when some members are inactive")
        void shouldCreateTeamWithPartialSuccessWhenSomeMembersInactive() {
            // Given
            CreateTeamCommand command = new CreateTeamCommand(
                    "Частичная команда / Partial Team",
                    "Description",
                    List.of(member1.getId(), inactiveMember.getId()),
                    null,
                    false,
                    creator.getId()
            );

            when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
            when(userRepository.findById(member1.getId())).thenReturn(Optional.of(member1));
            when(userRepository.findById(inactiveMember.getId())).thenReturn(Optional.of(inactiveMember));
            when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
                Team savedTeam = invocation.getArgument(0);
                if (savedTeam.getId() == null) {
                    savedTeam.setId(UUID.randomUUID());
                }
                return savedTeam;
            });
            when(teamMemberRepository.save(any(TeamMember.class))).thenAnswer(invocation -> {
                TeamMember member = invocation.getArgument(0);
                member.setId(UUID.randomUUID());
                return member;
            });

            // When
            TeamCreationResult result = orchestrationService.createTeamWithMembers(command);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getWarnings()).hasSize(1);
            assertThat(result.getWarnings().get(0)).contains("inactive");

            // Only one member should be saved (inactive one skipped)
            verify(teamMemberRepository, times(1)).save(any(TeamMember.class));
        }

        @Test
        @DisplayName("should throw exception when creator not found")
        void shouldThrowExceptionWhenCreatorNotFound() {
            // Given
            UUID nonExistentCreatorId = UUID.randomUUID();
            CreateTeamCommand command = new CreateTeamCommand(
                    "Test Team",
                    "Description",
                    List.of(member1.getId()),
                    null,
                    false,
                    nonExistentCreatorId
            );

            when(userRepository.findById(nonExistentCreatorId)).thenReturn(Optional.empty());

            // When
            TeamCreationResult result = orchestrationService.createTeamWithMembers(command);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("Creator not found");
        }

        @Test
        @DisplayName("should compensate and rollback when saga fails during member addition")
        void shouldCompensateAndRollbackWhenSagaFails() {
            // Given
            CreateTeamCommand command = new CreateTeamCommand(
                    "Failing Team",
                    "Description",
                    List.of(member1.getId()),
                    null,
                    false,
                    creator.getId()
            );

            when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
            // First save succeeds, second save (final state) throws
            when(teamRepository.save(any(Team.class)))
                    .thenAnswer(invocation -> {
                        Team savedTeam = invocation.getArgument(0);
                        savedTeam.setId(UUID.randomUUID());
                        return savedTeam;
                    })
                    .thenThrow(new RuntimeException("Database connection lost during final save"));
            when(userRepository.findById(member1.getId())).thenReturn(Optional.of(member1));
            when(teamMemberRepository.save(any(TeamMember.class))).thenAnswer(invocation -> {
                TeamMember tm = invocation.getArgument(0);
                tm.setId(UUID.randomUUID());
                tm.getTeam().getMembers().add(tm);
                return tm;
            });

            // When
            TeamCreationResult result = orchestrationService.createTeamWithMembers(command);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).contains("Database connection lost");
            // Compensation should have been triggered
            verify(teamRepository).deleteById(any(UUID.class));
        }

        @Test
        @DisplayName("should skip immediate activation when no active members added")
        void shouldSkipImmediateActivationWhenNoActiveMembers() {
            // Given
            CreateTeamCommand command = new CreateTeamCommand(
                    "Empty Team",
                    "Description",
                    List.of(inactiveMember.getId()), // Only inactive member
                    null,
                    true, // Request immediate activation
                    creator.getId()
            );

            when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
            when(userRepository.findById(inactiveMember.getId())).thenReturn(Optional.of(inactiveMember));
            when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
                Team savedTeam = invocation.getArgument(0);
                if (savedTeam.getId() == null) {
                    savedTeam.setId(UUID.randomUUID());
                }
                return savedTeam;
            });

            // When
            TeamCreationResult result = orchestrationService.createTeamWithMembers(command);

            // Then
            assertThat(result.isSuccess()).isTrue();
            // Team should remain in DRAFT status because no active members
            assertThat(result.getTeam().getStatus()).isEqualTo(TeamStatus.DRAFT);
        }

        @Test
        @DisplayName("should activate team immediately when requested and has active members")
        void shouldActivateTeamImmediatelyWhenRequestedAndHasActiveMembers() {
            // Given
            CreateTeamCommand command = new CreateTeamCommand(
                    "Active Team",
                    "Description",
                    List.of(member1.getId()),
                    null,
                    true, // Request immediate activation
                    creator.getId()
            );

            when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
            when(userRepository.findById(member1.getId())).thenReturn(Optional.of(member1));
            when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
                Team savedTeam = invocation.getArgument(0);
                if (savedTeam.getId() == null) {
                    savedTeam.setId(UUID.randomUUID());
                }
                return savedTeam;
            });
            when(teamMemberRepository.save(any(TeamMember.class))).thenAnswer(invocation -> {
                TeamMember tm = invocation.getArgument(0);
                tm.setId(UUID.randomUUID());
                // Simulate adding to team's members collection
                Team tmTeam = tm.getTeam();
                tmTeam.getMembers().add(tm);
                return tm;
            });

            // When
            TeamCreationResult result = orchestrationService.createTeamWithMembers(command);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getTeam().getStatus()).isEqualTo(TeamStatus.ACTIVE);
            assertThat(result.getTeam().getActivatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should set leader when leader is among added members")
        void shouldSetLeaderWhenLeaderIsAmongAddedMembers() {
            // Given
            CreateTeamCommand command = new CreateTeamCommand(
                    "Team with Leader",
                    "Description",
                    List.of(member1.getId(), member2.getId()),
                    member1.getId(), // Set member1 as leader
                    false,
                    creator.getId()
            );

            when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
            when(userRepository.findById(member1.getId())).thenReturn(Optional.of(member1));
            when(userRepository.findById(member2.getId())).thenReturn(Optional.of(member2));
            when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
                Team savedTeam = invocation.getArgument(0);
                if (savedTeam.getId() == null) {
                    savedTeam.setId(UUID.randomUUID());
                }
                return savedTeam;
            });

            TeamMember leaderMember = new TeamMember(team, member1, TeamMemberRole.MEMBER);
            leaderMember.setId(UUID.randomUUID());

            when(teamMemberRepository.save(any(TeamMember.class))).thenAnswer(invocation -> {
                TeamMember tm = invocation.getArgument(0);
                tm.setId(UUID.randomUUID());
                tm.getTeam().getMembers().add(tm);
                return tm;
            });
            when(teamMemberRepository.findByTeamIdAndUserId(any(UUID.class), eq(member1.getId())))
                    .thenReturn(Optional.of(leaderMember));

            // When
            TeamCreationResult result = orchestrationService.createTeamWithMembers(command);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getTeam().getLeader()).isEqualTo(member1);
            assertThat(leaderMember.getRole()).isEqualTo(TeamMemberRole.LEADER);
        }

        @Test
        @DisplayName("should add warning when leader is not among team members")
        void shouldAddWarningWhenLeaderIsNotAmongMembers() {
            // Given
            UUID nonMemberLeaderId = UUID.randomUUID();
            CreateTeamCommand command = new CreateTeamCommand(
                    "Team with Invalid Leader",
                    "Description",
                    List.of(member1.getId()),
                    nonMemberLeaderId, // Leader not in member list
                    false,
                    creator.getId()
            );

            when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
            when(userRepository.findById(member1.getId())).thenReturn(Optional.of(member1));
            when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
                Team savedTeam = invocation.getArgument(0);
                if (savedTeam.getId() == null) {
                    savedTeam.setId(UUID.randomUUID());
                }
                return savedTeam;
            });
            when(teamMemberRepository.save(any(TeamMember.class))).thenAnswer(invocation -> {
                TeamMember tm = invocation.getArgument(0);
                tm.setId(UUID.randomUUID());
                tm.getTeam().getMembers().add(tm);
                return tm;
            });

            // When
            TeamCreationResult result = orchestrationService.createTeamWithMembers(command);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getWarnings()).anyMatch(w -> w.contains("not a team member"));
            assertThat(result.getTeam().getLeader()).isNull();
        }

        @Test
        @DisplayName("should handle member not found gracefully")
        void shouldHandleMemberNotFoundGracefully() {
            // Given
            UUID nonExistentMemberId = UUID.randomUUID();
            CreateTeamCommand command = new CreateTeamCommand(
                    "Team with Missing Member",
                    "Description",
                    List.of(member1.getId(), nonExistentMemberId),
                    null,
                    false,
                    creator.getId()
            );

            when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
            when(userRepository.findById(member1.getId())).thenReturn(Optional.of(member1));
            when(userRepository.findById(nonExistentMemberId))
                    .thenThrow(new IllegalArgumentException("Member not found: " + nonExistentMemberId));
            when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
                Team savedTeam = invocation.getArgument(0);
                if (savedTeam.getId() == null) {
                    savedTeam.setId(UUID.randomUUID());
                }
                return savedTeam;
            });
            when(teamMemberRepository.save(any(TeamMember.class))).thenAnswer(invocation -> {
                TeamMember tm = invocation.getArgument(0);
                tm.setId(UUID.randomUUID());
                tm.getTeam().getMembers().add(tm);
                return tm;
            });

            // When
            TeamCreationResult result = orchestrationService.createTeamWithMembers(command);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getWarnings()).anyMatch(w -> w.contains("Failed to add member"));
        }
    }

    // ============================================
    // ADD MEMBERS TO TEAM TESTS
    // ============================================

    @Nested
    @DisplayName("addMembersToTeam")
    class AddMembersToTeamTests {

        @BeforeEach
        void setUpTeam() {
            team.setMembers(new HashSet<>());
        }

        @Test
        @DisplayName("should add new members successfully")
        void shouldAddNewMembersSuccessfully() {
            // Given
            AddMembersCommand command = new AddMembersCommand(
                    List.of(member1.getId(), member2.getId()),
                    creator.getClerkId()
            );

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
            when(teamMemberRepository.existsByTeamIdAndUserIdAndIsActiveTrue(team.getId(), member1.getId()))
                    .thenReturn(false);
            when(teamMemberRepository.existsByTeamIdAndUserIdAndIsActiveTrue(team.getId(), member2.getId()))
                    .thenReturn(false);
            when(userRepository.findById(member1.getId())).thenReturn(Optional.of(member1));
            when(userRepository.findById(member2.getId())).thenReturn(Optional.of(member2));
            when(teamMemberRepository.findByTeamIdAndUserId(team.getId(), member1.getId()))
                    .thenReturn(Optional.empty());
            when(teamMemberRepository.findByTeamIdAndUserId(team.getId(), member2.getId()))
                    .thenReturn(Optional.empty());
            when(teamMemberRepository.save(any(TeamMember.class))).thenAnswer(invocation -> {
                TeamMember tm = invocation.getArgument(0);
                tm.setId(UUID.randomUUID());
                return tm;
            });

            // When
            MemberAdditionResult result = orchestrationService.addMembersToTeam(team.getId(), command);

            // Then
            assertThat(result.addedMembers()).hasSize(2);
            assertThat(result.failures()).isEmpty();
            assertThat(result.hasErrors()).isFalse();

            verify(teamMemberRepository, times(2)).save(any(TeamMember.class));
        }

        @Test
        @DisplayName("should report failure for already active member")
        void shouldReportFailureForAlreadyActiveMember() {
            // Given
            AddMembersCommand command = new AddMembersCommand(
                    List.of(member1.getId()),
                    creator.getClerkId()
            );

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
            when(teamMemberRepository.existsByTeamIdAndUserIdAndIsActiveTrue(team.getId(), member1.getId()))
                    .thenReturn(true);

            // When
            MemberAdditionResult result = orchestrationService.addMembersToTeam(team.getId(), command);

            // Then
            assertThat(result.addedMembers()).isEmpty();
            assertThat(result.failures()).hasSize(1);
            assertThat(result.failures().get(0)).contains("already an active member");
        }

        @Test
        @DisplayName("should report failure for non-existent user")
        void shouldReportFailureForNonExistentUser() {
            // Given
            UUID nonExistentUserId = UUID.randomUUID();
            AddMembersCommand command = new AddMembersCommand(
                    List.of(nonExistentUserId),
                    creator.getClerkId()
            );

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
            when(teamMemberRepository.existsByTeamIdAndUserIdAndIsActiveTrue(team.getId(), nonExistentUserId))
                    .thenReturn(false);
            when(userRepository.findById(nonExistentUserId))
                    .thenThrow(new IllegalArgumentException("User not found: " + nonExistentUserId));

            // When
            MemberAdditionResult result = orchestrationService.addMembersToTeam(team.getId(), command);

            // Then
            assertThat(result.addedMembers()).isEmpty();
            assertThat(result.failures()).hasSize(1);
            assertThat(result.failures().get(0)).contains("Failed to add user");
        }

        @Test
        @DisplayName("should report failure for inactive user")
        void shouldReportFailureForInactiveUser() {
            // Given
            AddMembersCommand command = new AddMembersCommand(
                    List.of(inactiveMember.getId()),
                    creator.getClerkId()
            );

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
            when(teamMemberRepository.existsByTeamIdAndUserIdAndIsActiveTrue(team.getId(), inactiveMember.getId()))
                    .thenReturn(false);
            when(userRepository.findById(inactiveMember.getId())).thenReturn(Optional.of(inactiveMember));

            // When
            MemberAdditionResult result = orchestrationService.addMembersToTeam(team.getId(), command);

            // Then
            assertThat(result.addedMembers()).isEmpty();
            assertThat(result.failures()).hasSize(1);
            assertThat(result.failures().get(0)).contains("not active");
        }

        @Test
        @DisplayName("should reject adding members to archived team")
        void shouldRejectAddingMembersToArchivedTeam() {
            // Given - create archived team with proper state
            Team archivedTeam = new Team("Archived Team", "Description", creator);
            archivedTeam.setId(team.getId());

            // Add a member so activation works
            TeamMember existingMember = new TeamMember(archivedTeam, member2, TeamMemberRole.MEMBER);
            existingMember.setId(UUID.randomUUID());
            archivedTeam.getMembers().add(existingMember);

            // Activate first (requires members), then archive
            archivedTeam.activate();
            archivedTeam.archive();

            AddMembersCommand command = new AddMembersCommand(
                    List.of(member1.getId()),
                    creator.getClerkId()
            );

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(archivedTeam));

            // When
            MemberAdditionResult result = orchestrationService.addMembersToTeam(team.getId(), command);

            // Then
            assertThat(result.hasErrors()).isTrue();
            assertThat(result.failures()).anyMatch(f -> f.contains("archived"));
            verify(teamMemberRepository, never()).save(any(TeamMember.class));
        }

        @Test
        @DisplayName("should reactivate previously removed member")
        void shouldReactivatePreviouslyRemovedMember() {
            // Given
            TeamMember previousMember = new TeamMember(team, member1, TeamMemberRole.MEMBER);
            previousMember.setId(UUID.randomUUID());
            previousMember.remove(); // Mark as removed

            AddMembersCommand command = new AddMembersCommand(
                    List.of(member1.getId()),
                    creator.getClerkId()
            );

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
            when(teamMemberRepository.existsByTeamIdAndUserIdAndIsActiveTrue(team.getId(), member1.getId()))
                    .thenReturn(false);
            when(userRepository.findById(member1.getId())).thenReturn(Optional.of(member1));
            when(teamMemberRepository.findByTeamIdAndUserId(team.getId(), member1.getId()))
                    .thenReturn(Optional.of(previousMember));
            when(teamMemberRepository.save(any(TeamMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            MemberAdditionResult result = orchestrationService.addMembersToTeam(team.getId(), command);

            // Then
            assertThat(result.addedMembers()).containsExactly(member1.getId());
            assertThat(previousMember.isActive()).isTrue();
            assertThat(previousMember.getLeftAt()).isNull();
        }

        @Test
        @DisplayName("should handle mixed success and failures")
        void shouldHandleMixedSuccessAndFailures() {
            // Given
            AddMembersCommand command = new AddMembersCommand(
                    List.of(member1.getId(), member2.getId(), inactiveMember.getId()),
                    creator.getClerkId()
            );

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
            // member1 is already active
            when(teamMemberRepository.existsByTeamIdAndUserIdAndIsActiveTrue(team.getId(), member1.getId()))
                    .thenReturn(true);
            // member2 can be added
            when(teamMemberRepository.existsByTeamIdAndUserIdAndIsActiveTrue(team.getId(), member2.getId()))
                    .thenReturn(false);
            when(userRepository.findById(member2.getId())).thenReturn(Optional.of(member2));
            when(teamMemberRepository.findByTeamIdAndUserId(team.getId(), member2.getId()))
                    .thenReturn(Optional.empty());
            // inactiveMember cannot be added
            when(teamMemberRepository.existsByTeamIdAndUserIdAndIsActiveTrue(team.getId(), inactiveMember.getId()))
                    .thenReturn(false);
            when(userRepository.findById(inactiveMember.getId())).thenReturn(Optional.of(inactiveMember));

            when(teamMemberRepository.save(any(TeamMember.class))).thenAnswer(invocation -> {
                TeamMember tm = invocation.getArgument(0);
                tm.setId(UUID.randomUUID());
                return tm;
            });

            // When
            MemberAdditionResult result = orchestrationService.addMembersToTeam(team.getId(), command);

            // Then
            assertThat(result.addedMembers()).containsExactly(member2.getId());
            assertThat(result.failures()).hasSize(2); // member1 (already active) + inactiveMember
            assertThat(result.hasErrors()).isTrue();
        }

        @Test
        @DisplayName("should throw exception when team not found")
        void shouldThrowExceptionWhenTeamNotFound() {
            // Given
            UUID nonExistentTeamId = UUID.randomUUID();
            AddMembersCommand command = new AddMembersCommand(
                    List.of(member1.getId()),
                    creator.getClerkId()
            );

            when(teamRepository.findById(nonExistentTeamId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> orchestrationService.addMembersToTeam(nonExistentTeamId, command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Team not found");
        }
    }

    // ============================================
    // CHANGE TEAM LEADER TESTS
    // ============================================

    @Nested
    @DisplayName("changeTeamLeader")
    class ChangeTeamLeaderTests {

        private TeamMember existingLeaderMember;
        private TeamMember newLeaderMember;

        @BeforeEach
        void setUpTeamWithMembers() {
            // Setup existing leader
            existingLeaderMember = new TeamMember(team, member1, TeamMemberRole.LEADER);
            existingLeaderMember.setId(UUID.randomUUID());

            // Setup potential new leader
            newLeaderMember = new TeamMember(team, member2, TeamMemberRole.MEMBER);
            newLeaderMember.setId(UUID.randomUUID());

            team.setMembers(new HashSet<>());
            team.getMembers().add(existingLeaderMember);
            team.getMembers().add(newLeaderMember);
        }

        @Test
        @DisplayName("should change leader from one member to another")
        void shouldChangeLeaderFromOneMemberToAnother() {
            // Given
            // Manually set leader since Team.setLeader() validates membership
            existingLeaderMember.getTeam().getMembers().clear();
            existingLeaderMember.getTeam().getMembers().add(existingLeaderMember);
            existingLeaderMember.getTeam().getMembers().add(newLeaderMember);

            // Create a team with proper leader setup
            Team teamWithLeader = new Team("Test Team", "Description", creator);
            teamWithLeader.setId(team.getId());
            TeamMember oldLeader = new TeamMember(teamWithLeader, member1, TeamMemberRole.LEADER);
            oldLeader.setId(UUID.randomUUID());
            TeamMember futureLeader = new TeamMember(teamWithLeader, member2, TeamMemberRole.MEMBER);
            futureLeader.setId(UUID.randomUUID());
            teamWithLeader.getMembers().add(oldLeader);
            teamWithLeader.getMembers().add(futureLeader);
            // Force leader without validation
            try {
                java.lang.reflect.Field leaderField = Team.class.getDeclaredField("leader");
                leaderField.setAccessible(true);
                leaderField.set(teamWithLeader, member1);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(teamWithLeader));
            when(userRepository.findById(member2.getId())).thenReturn(Optional.of(member2));
            when(teamMemberRepository.findByTeamIdAndUserId(team.getId(), member1.getId()))
                    .thenReturn(Optional.of(oldLeader));
            when(teamMemberRepository.findByTeamIdAndUserId(team.getId(), member2.getId()))
                    .thenReturn(Optional.of(futureLeader));
            when(teamMemberRepository.save(any(TeamMember.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            LeaderChangeResult result = orchestrationService.changeTeamLeader(team.getId(), member2.getId());

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.previousLeaderId()).isEqualTo(member1.getId());
            assertThat(result.newLeaderId()).isEqualTo(member2.getId());

            // Old leader demoted
            assertThat(oldLeader.getRole()).isEqualTo(TeamMemberRole.MEMBER);
            // New leader promoted
            assertThat(futureLeader.getRole()).isEqualTo(TeamMemberRole.LEADER);
        }

        @Test
        @DisplayName("should remove leader when null is passed")
        void shouldRemoveLeaderWhenNullIsPassed() {
            // Given
            Team teamWithLeader = new Team("Test Team", "Description", creator);
            teamWithLeader.setId(team.getId());
            TeamMember oldLeader = new TeamMember(teamWithLeader, member1, TeamMemberRole.LEADER);
            oldLeader.setId(UUID.randomUUID());
            teamWithLeader.getMembers().add(oldLeader);
            try {
                java.lang.reflect.Field leaderField = Team.class.getDeclaredField("leader");
                leaderField.setAccessible(true);
                leaderField.set(teamWithLeader, member1);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(teamWithLeader));
            when(teamMemberRepository.findByTeamIdAndUserId(team.getId(), member1.getId()))
                    .thenReturn(Optional.of(oldLeader));
            when(teamMemberRepository.save(any(TeamMember.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            LeaderChangeResult result = orchestrationService.changeTeamLeader(team.getId(), null);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.previousLeaderId()).isEqualTo(member1.getId());
            assertThat(result.newLeaderId()).isNull();
            assertThat(oldLeader.getRole()).isEqualTo(TeamMemberRole.MEMBER);
        }

        @Test
        @DisplayName("should fail when new leader is not a team member")
        void shouldFailWhenNewLeaderIsNotTeamMember() {
            // Given
            UUID nonMemberId = UUID.randomUUID();
            User nonMember = TestDataFactory.createUser(UserRole.USER);
            nonMember.setId(nonMemberId);

            Team teamWithLeader = new Team("Test Team", "Description", creator);
            teamWithLeader.setId(team.getId());

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(teamWithLeader));
            when(userRepository.findById(nonMemberId)).thenReturn(Optional.of(nonMember));
            when(teamMemberRepository.findByTeamIdAndUserId(team.getId(), nonMemberId))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> orchestrationService.changeTeamLeader(team.getId(), nonMemberId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be a team member");
        }

        @Test
        @DisplayName("should reject leader change for archived team")
        void shouldRejectLeaderChangeForArchivedTeam() {
            // Given
            Team archivedTeam = new Team("Archived Team", "Description", creator);
            archivedTeam.setId(team.getId());
            TeamMember tm = new TeamMember(archivedTeam, member1, TeamMemberRole.MEMBER);
            tm.setId(UUID.randomUUID());
            archivedTeam.getMembers().add(tm);
            archivedTeam.activate();
            archivedTeam.archive();

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(archivedTeam));

            // When
            LeaderChangeResult result = orchestrationService.changeTeamLeader(team.getId(), member1.getId());

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.errorMessage()).contains("archived");
        }

        @Test
        @DisplayName("should fail when new leader is inactive member")
        void shouldFailWhenNewLeaderIsInactiveMember() {
            // Given
            Team teamWithInactiveMember = new Team("Test Team", "Description", creator);
            teamWithInactiveMember.setId(team.getId());
            TeamMember inactiveTeamMember = new TeamMember(teamWithInactiveMember, member1, TeamMemberRole.MEMBER);
            inactiveTeamMember.setId(UUID.randomUUID());
            inactiveTeamMember.remove(); // Make inactive
            teamWithInactiveMember.getMembers().add(inactiveTeamMember);

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(teamWithInactiveMember));
            when(userRepository.findById(member1.getId())).thenReturn(Optional.of(member1));
            when(teamMemberRepository.findByTeamIdAndUserId(team.getId(), member1.getId()))
                    .thenReturn(Optional.of(inactiveTeamMember));

            // When
            LeaderChangeResult result = orchestrationService.changeTeamLeader(team.getId(), member1.getId());

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.errorMessage()).contains("active member");
        }

        @Test
        @DisplayName("should throw exception when team not found")
        void shouldThrowExceptionWhenTeamNotFoundForLeaderChange() {
            // Given
            UUID nonExistentTeamId = UUID.randomUUID();

            when(teamRepository.findById(nonExistentTeamId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> orchestrationService.changeTeamLeader(nonExistentTeamId, member1.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Team not found");
        }

        @Test
        @DisplayName("should throw exception when new leader user not found")
        void shouldThrowExceptionWhenNewLeaderUserNotFound() {
            // Given
            UUID nonExistentUserId = UUID.randomUUID();

            Team teamWithLeader = new Team("Test Team", "Description", creator);
            teamWithLeader.setId(team.getId());

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(teamWithLeader));
            when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> orchestrationService.changeTeamLeader(team.getId(), nonExistentUserId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("New leader not found");
        }
    }

    // ============================================
    // ACTIVATE TEAM TESTS
    // ============================================

    @Nested
    @DisplayName("activateTeam")
    class ActivateTeamTests {

        @Test
        @DisplayName("should activate DRAFT team with active members")
        void shouldActivateDraftTeamWithActiveMembers() {
            // Given
            Team draftTeam = new Team("Draft Team / Черновая команда", "Description", creator);
            draftTeam.setId(team.getId());
            TeamMember tm = new TeamMember(draftTeam, member1, TeamMemberRole.MEMBER);
            tm.setId(UUID.randomUUID());
            draftTeam.getMembers().add(tm);

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(draftTeam));
            when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            ActivationResult result = orchestrationService.activateTeam(team.getId());

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getActivatedAt()).isNotNull();
            assertThat(draftTeam.getStatus()).isEqualTo(TeamStatus.ACTIVE);
        }

        @Test
        @DisplayName("should fail to activate already ACTIVE team")
        void shouldFailToActivateAlreadyActiveTeam() {
            // Given
            Team activeTeam = new Team("Active Team", "Description", creator);
            activeTeam.setId(team.getId());
            TeamMember tm = new TeamMember(activeTeam, member1, TeamMemberRole.MEMBER);
            tm.setId(UUID.randomUUID());
            activeTeam.getMembers().add(tm);
            activeTeam.activate();

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(activeTeam));

            // When
            ActivationResult result = orchestrationService.activateTeam(team.getId());

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrors()).anyMatch(e -> e.contains("DRAFT"));
        }

        @Test
        @DisplayName("should fail to activate ARCHIVED team")
        void shouldFailToActivateArchivedTeam() {
            // Given
            Team archivedTeam = new Team("Archived Team", "Description", creator);
            archivedTeam.setId(team.getId());
            TeamMember tm = new TeamMember(archivedTeam, member1, TeamMemberRole.MEMBER);
            tm.setId(UUID.randomUUID());
            archivedTeam.getMembers().add(tm);
            archivedTeam.activate();
            archivedTeam.archive();

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(archivedTeam));

            // When
            ActivationResult result = orchestrationService.activateTeam(team.getId());

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrors()).anyMatch(e -> e.contains("DRAFT"));
        }

        @Test
        @DisplayName("should fail to activate team with no active members")
        void shouldFailToActivateTeamWithNoActiveMembers() {
            // Given
            Team emptyTeam = new Team("Empty Team / Пустая команда", "Description", creator);
            emptyTeam.setId(team.getId());
            // No members added

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(emptyTeam));

            // When
            ActivationResult result = orchestrationService.activateTeam(team.getId());

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrors()).anyMatch(e -> e.contains("at least one active member"));
        }

        @Test
        @DisplayName("should fail activation when all members are inactive")
        void shouldFailActivationWhenAllMembersAreInactive() {
            // Given
            Team teamWithInactiveMembers = new Team("Team with Inactive Members", "Description", creator);
            teamWithInactiveMembers.setId(team.getId());
            TeamMember inactiveTm = new TeamMember(teamWithInactiveMembers, member1, TeamMemberRole.MEMBER);
            inactiveTm.setId(UUID.randomUUID());
            inactiveTm.remove(); // Deactivate
            teamWithInactiveMembers.getMembers().add(inactiveTm);

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(teamWithInactiveMembers));

            // When
            ActivationResult result = orchestrationService.activateTeam(team.getId());

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrors()).anyMatch(e -> e.contains("at least one active member"));
        }

        @Test
        @DisplayName("should throw exception when team not found for activation")
        void shouldThrowExceptionWhenTeamNotFoundForActivation() {
            // Given
            UUID nonExistentTeamId = UUID.randomUUID();

            when(teamRepository.findById(nonExistentTeamId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> orchestrationService.activateTeam(nonExistentTeamId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Team not found");
        }

        @Test
        @DisplayName("should return multiple validation errors")
        void shouldReturnMultipleValidationErrors() {
            // Given - ACTIVE team with no members (edge case)
            Team invalidTeam = new Team("Invalid Team", "Description", creator);
            invalidTeam.setId(team.getId());
            // Force ACTIVE status via reflection to test validation
            try {
                java.lang.reflect.Field statusField = Team.class.getDeclaredField("status");
                statusField.setAccessible(true);
                statusField.set(invalidTeam, TeamStatus.ACTIVE);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(invalidTeam));

            // When
            ActivationResult result = orchestrationService.activateTeam(team.getId());

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrors()).hasSizeGreaterThanOrEqualTo(2);
        }
    }

    // ============================================
    // ARCHIVE TEAM TESTS
    // ============================================

    @Nested
    @DisplayName("archiveTeam")
    class ArchiveTeamTests {

        @Test
        @DisplayName("should archive active team successfully")
        void shouldArchiveActiveTeamSuccessfully() {
            // Given
            Team activeTeam = new Team("Active Team / Активная команда", "Description", creator);
            activeTeam.setId(team.getId());
            TeamMember tm1 = new TeamMember(activeTeam, member1, TeamMemberRole.LEADER);
            tm1.setId(UUID.randomUUID());
            TeamMember tm2 = new TeamMember(activeTeam, member2, TeamMemberRole.MEMBER);
            tm2.setId(UUID.randomUUID());
            activeTeam.getMembers().add(tm1);
            activeTeam.getMembers().add(tm2);
            activeTeam.activate();

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(activeTeam));
            when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(teamMemberRepository.findByTeamIdAndIsActiveTrue(team.getId()))
                    .thenReturn(List.of(tm1, tm2));
            when(teamMemberRepository.save(any(TeamMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            ArchiveResult result = orchestrationService.archiveTeam(team.getId());

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getArchivedAt()).isNotNull();
            assertThat(activeTeam.getStatus()).isEqualTo(TeamStatus.ARCHIVED);

            // Verify members deactivated
            verify(teamMemberRepository, times(2)).save(any(TeamMember.class));
            assertThat(tm1.isActive()).isFalse();
            assertThat(tm2.isActive()).isFalse();
        }

        @Test
        @DisplayName("should archive DRAFT team successfully")
        void shouldArchiveDraftTeamSuccessfully() {
            // Given
            Team draftTeam = new Team("Draft Team / Черновая команда", "Description", creator);
            draftTeam.setId(team.getId());
            TeamMember tm = new TeamMember(draftTeam, member1, TeamMemberRole.MEMBER);
            tm.setId(UUID.randomUUID());
            draftTeam.getMembers().add(tm);

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(draftTeam));
            when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(teamMemberRepository.findByTeamIdAndIsActiveTrue(team.getId()))
                    .thenReturn(List.of(tm));
            when(teamMemberRepository.save(any(TeamMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            ArchiveResult result = orchestrationService.archiveTeam(team.getId());

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(draftTeam.getStatus()).isEqualTo(TeamStatus.ARCHIVED);
        }

        @Test
        @DisplayName("should fail to archive already archived team")
        void shouldFailToArchiveAlreadyArchivedTeam() {
            // Given
            Team archivedTeam = new Team("Archived Team / Архивная команда", "Description", creator);
            archivedTeam.setId(team.getId());
            TeamMember tm = new TeamMember(archivedTeam, member1, TeamMemberRole.MEMBER);
            tm.setId(UUID.randomUUID());
            archivedTeam.getMembers().add(tm);
            archivedTeam.activate();
            archivedTeam.archive();

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(archivedTeam));

            // When
            ArchiveResult result = orchestrationService.archiveTeam(team.getId());

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.errorMessage()).contains("already archived");
        }

        @Test
        @DisplayName("should deactivate all members when archiving")
        void shouldDeactivateAllMembersWhenArchiving() {
            // Given
            Team activeTeam = new Team("Team to Archive", "Description", creator);
            activeTeam.setId(team.getId());
            TeamMember tm1 = new TeamMember(activeTeam, member1, TeamMemberRole.LEADER);
            tm1.setId(UUID.randomUUID());
            TeamMember tm2 = new TeamMember(activeTeam, member2, TeamMemberRole.MEMBER);
            tm2.setId(UUID.randomUUID());
            TeamMember tm3 = new TeamMember(activeTeam, member3, TeamMemberRole.MEMBER);
            tm3.setId(UUID.randomUUID());
            activeTeam.getMembers().add(tm1);
            activeTeam.getMembers().add(tm2);
            activeTeam.getMembers().add(tm3);
            activeTeam.activate();

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(activeTeam));
            when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(teamMemberRepository.findByTeamIdAndIsActiveTrue(team.getId()))
                    .thenReturn(List.of(tm1, tm2, tm3));
            when(teamMemberRepository.save(any(TeamMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            ArchiveResult result = orchestrationService.archiveTeam(team.getId());

            // Then
            assertThat(result.isSuccess()).isTrue();

            ArgumentCaptor<TeamMember> memberCaptor = ArgumentCaptor.forClass(TeamMember.class);
            verify(teamMemberRepository, times(3)).save(memberCaptor.capture());

            List<TeamMember> savedMembers = memberCaptor.getAllValues();
            assertThat(savedMembers).allMatch(m -> !m.isActive());
            assertThat(savedMembers).allMatch(m -> m.getLeftAt() != null);
        }

        @Test
        @DisplayName("should throw exception when team not found for archiving")
        void shouldThrowExceptionWhenTeamNotFoundForArchiving() {
            // Given
            UUID nonExistentTeamId = UUID.randomUUID();

            when(teamRepository.findById(nonExistentTeamId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> orchestrationService.archiveTeam(nonExistentTeamId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Team not found");
        }

        @Test
        @DisplayName("should handle archiving team with no active members")
        void shouldHandleArchivingTeamWithNoActiveMembers() {
            // Given
            Team teamWithNoActiveMembers = new Team("Empty Team", "Description", creator);
            teamWithNoActiveMembers.setId(team.getId());
            TeamMember inactiveTm = new TeamMember(teamWithNoActiveMembers, member1, TeamMemberRole.MEMBER);
            inactiveTm.setId(UUID.randomUUID());
            inactiveTm.remove(); // Already inactive
            teamWithNoActiveMembers.getMembers().add(inactiveTm);

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(teamWithNoActiveMembers));
            when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(teamMemberRepository.findByTeamIdAndIsActiveTrue(team.getId()))
                    .thenReturn(List.of()); // No active members

            // When
            ArchiveResult result = orchestrationService.archiveTeam(team.getId());

            // Then
            assertThat(result.isSuccess()).isTrue();
            verify(teamMemberRepository, never()).save(any(TeamMember.class));
        }

        @Test
        @DisplayName("should set archived timestamp correctly")
        void shouldSetArchivedTimestampCorrectly() {
            // Given
            Team draftTeam = new Team("Team to Archive", "Description", creator);
            draftTeam.setId(team.getId());

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(draftTeam));
            when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(teamMemberRepository.findByTeamIdAndIsActiveTrue(team.getId()))
                    .thenReturn(List.of());

            // When
            ArchiveResult result = orchestrationService.archiveTeam(team.getId());

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getArchivedAt()).isNotNull();
            assertThat(draftTeam.getArchivedAt()).isNotNull();
            assertThat(result.getArchivedAt()).isEqualTo(draftTeam.getArchivedAt());
        }
    }

    // ============================================
    // EDGE CASES AND INTEGRATION SCENARIOS
    // ============================================

    @Nested
    @DisplayName("Edge Cases and Complex Scenarios")
    class EdgeCasesTests {

        @Test
        @DisplayName("should handle bilingual team names correctly")
        void shouldHandleBilingualTeamNamesCorrectly() {
            // Given
            String bilingualName = "Команда инноваций / Innovation Team";
            String bilingualDescription = "Создание инновационных продуктов / Creating innovative products";

            CreateTeamCommand command = new CreateTeamCommand(
                    bilingualName,
                    bilingualDescription,
                    List.of(member1.getId()),
                    null,
                    false,
                    creator.getId()
            );

            when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
            when(userRepository.findById(member1.getId())).thenReturn(Optional.of(member1));
            when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
                Team savedTeam = invocation.getArgument(0);
                if (savedTeam.getId() == null) {
                    savedTeam.setId(UUID.randomUUID());
                }
                return savedTeam;
            });
            when(teamMemberRepository.save(any(TeamMember.class))).thenAnswer(invocation -> {
                TeamMember tm = invocation.getArgument(0);
                tm.setId(UUID.randomUUID());
                tm.getTeam().getMembers().add(tm);
                return tm;
            });

            // When
            TeamCreationResult result = orchestrationService.createTeamWithMembers(command);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getTeam().getName()).isEqualTo(bilingualName);
            assertThat(result.getTeam().getDescription()).isEqualTo(bilingualDescription);
        }

        @Test
        @DisplayName("should handle empty member list in creation")
        void shouldHandleEmptyMemberListInCreation() {
            // Given
            CreateTeamCommand command = new CreateTeamCommand(
                    "Empty Member Team",
                    "Description",
                    List.of(), // Empty member list
                    null,
                    false,
                    creator.getId()
            );

            when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
            when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
                Team savedTeam = invocation.getArgument(0);
                if (savedTeam.getId() == null) {
                    savedTeam.setId(UUID.randomUUID());
                }
                return savedTeam;
            });

            // When
            TeamCreationResult result = orchestrationService.createTeamWithMembers(command);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getTeam().getActiveMemberCount()).isZero();
            verify(teamMemberRepository, never()).save(any(TeamMember.class));
        }

        @Test
        @DisplayName("should handle concurrent member operations gracefully")
        void shouldHandleConcurrentMemberOperationsGracefully() {
            // Given - simulate a scenario where member becomes inactive during addition
            AddMembersCommand command = new AddMembersCommand(
                    List.of(member1.getId()),
                    creator.getClerkId()
            );

            User changingMember = TestDataFactory.createUser(UserRole.USER);
            changingMember.setId(member1.getId());
            changingMember.setActive(true); // Active at first check

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
            when(teamMemberRepository.existsByTeamIdAndUserIdAndIsActiveTrue(team.getId(), member1.getId()))
                    .thenReturn(false);
            // User becomes inactive between checks
            when(userRepository.findById(member1.getId())).thenAnswer(invocation -> {
                changingMember.setActive(false); // Simulate state change
                return Optional.of(changingMember);
            });

            // When
            MemberAdditionResult result = orchestrationService.addMembersToTeam(team.getId(), command);

            // Then
            assertThat(result.addedMembers()).isEmpty();
            assertThat(result.failures()).hasSize(1);
            assertThat(result.failures().get(0)).contains("not active");
        }

        @Test
        @DisplayName("should track saga steps correctly on failure")
        void shouldTrackSagaStepsCorrectlyOnFailure() {
            // Given - force failure after team creation but during member addition
            CreateTeamCommand command = new CreateTeamCommand(
                    "Saga Failure Team",
                    "Description",
                    List.of(member1.getId()),
                    null,
                    false,
                    creator.getId()
            );

            when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
            when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
                Team savedTeam = invocation.getArgument(0);
                savedTeam.setId(UUID.randomUUID());
                return savedTeam;
            }).thenThrow(new RuntimeException("Database error during save"));
            when(userRepository.findById(member1.getId())).thenReturn(Optional.of(member1));
            when(teamMemberRepository.save(any(TeamMember.class))).thenAnswer(invocation -> {
                TeamMember tm = invocation.getArgument(0);
                tm.setId(UUID.randomUUID());
                return tm;
            });

            // When
            TeamCreationResult result = orchestrationService.createTeamWithMembers(command);

            // Then
            assertThat(result.isSuccess()).isFalse();
            verify(teamRepository).deleteById(any(UUID.class)); // Compensation executed
        }
    }
}
