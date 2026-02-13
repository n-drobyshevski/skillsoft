package app.skillsoft.assessmentbackend.services.team;

import app.skillsoft.assessmentbackend.domain.dto.team.*;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.repository.TeamMemberRepository;
import app.skillsoft.assessmentbackend.repository.TeamRepository;
import app.skillsoft.assessmentbackend.testutils.BaseUnitTest;
import app.skillsoft.assessmentbackend.testutils.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TeamQueryServiceImpl.
 *
 * Tests cover all team query operations including:
 * - Finding teams by ID (with and without members)
 * - Converting teams to DTOs
 * - Searching teams with filters
 * - Finding teams by member
 * - Getting team members
 * - Updating team details
 * - Removing members
 * - Team statistics
 *
 * Uses bilingual test data (English/Russian) to verify proper handling
 * of Cyrillic characters throughout the system.
 */
@DisplayName("TeamQueryService Tests")
class TeamQueryServiceTest extends BaseUnitTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private TeamMapper teamMapper;

    @InjectMocks
    private TeamQueryServiceImpl queryService;

    private User creator;
    private User member1;
    private User member2;
    private Team team;
    private TeamMember teamMember1;
    private TeamMember teamMember2;

    @BeforeEach
    void setUp() {
        TestDataFactory.resetCounter();

        // Create test users with bilingual names (EN/RU)
        creator = TestDataFactory.createUser(UserRole.ADMIN);
        creator.setId(UUID.randomUUID());
        creator.setFirstName("Creator / Создатель");
        creator.setClerkId("clerk_creator_123");

        member1 = TestDataFactory.createUser(UserRole.USER);
        member1.setId(UUID.randomUUID());
        member1.setFirstName("Алексей / Alexey");
        member1.setClerkId("clerk_member_1");

        member2 = TestDataFactory.createUser(UserRole.USER);
        member2.setId(UUID.randomUUID());
        member2.setFirstName("Мария / Maria");
        member2.setClerkId("clerk_member_2");

        // Create test team with bilingual name
        team = TestDataFactory.createTeam(creator);
        team.setId(UUID.randomUUID());
        team.setName("Команда разработки / Development Team");
        team.setDescription("Описание команды / Team description");

        // Create team members
        teamMember1 = new TeamMember(team, member1, TeamMemberRole.LEADER);
        teamMember1.setId(UUID.randomUUID());

        teamMember2 = new TeamMember(team, member2, TeamMemberRole.MEMBER);
        teamMember2.setId(UUID.randomUUID());

        team.getMembers().add(teamMember1);
        team.getMembers().add(teamMember2);
    }

    // ============================================
    // FIND BY ID TESTS
    // ============================================

    @Nested
    @DisplayName("findById methods")
    class FindByIdTests {

        @Test
        @DisplayName("should return team when found by ID")
        void shouldReturnTeamWhenFound() {
            // Given
            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));

            // When
            Optional<Team> result = queryService.findById(team.getId());

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(team.getId());
            assertThat(result.get().getName()).isEqualTo("Команда разработки / Development Team");
            verify(teamRepository).findById(team.getId());
        }

        @Test
        @DisplayName("should return empty when team not found")
        void shouldReturnEmptyWhenNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(teamRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When
            Optional<Team> result = queryService.findById(nonExistentId);

            // Then
            assertThat(result).isEmpty();
            verify(teamRepository).findById(nonExistentId);
        }

        @Test
        @DisplayName("should return team with members eagerly loaded")
        void shouldReturnTeamWithMembers() {
            // Given
            when(teamRepository.findByIdWithMembers(team.getId())).thenReturn(Optional.of(team));

            // When
            Optional<Team> result = queryService.findByIdWithMembers(team.getId());

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getMembers()).hasSize(2);
            verify(teamRepository).findByIdWithMembers(team.getId());
        }

        @Test
        @DisplayName("should return empty when team with members not found")
        void shouldReturnEmptyWhenTeamWithMembersNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(teamRepository.findByIdWithMembers(nonExistentId)).thenReturn(Optional.empty());

            // When
            Optional<Team> result = queryService.findByIdWithMembers(nonExistentId);

            // Then
            assertThat(result).isEmpty();
            verify(teamRepository).findByIdWithMembers(nonExistentId);
        }
    }

    // ============================================
    // GET TEAM DTO TESTS
    // ============================================

    @Nested
    @DisplayName("getTeamDto")
    class GetTeamDtoTests {

        @Test
        @DisplayName("should return DTO when team exists")
        void shouldReturnDtoWhenTeamExists() {
            // Given
            TeamDto expectedDto = new TeamDto(
                    team.getId(),
                    team.getName(),
                    team.getDescription(),
                    TeamStatus.DRAFT,
                    null,
                    new UserSummaryDto(creator.getId(), creator.getClerkId(), creator.getFullName(), creator.getEmail(), null),
                    List.of(),
                    2,
                    LocalDateTime.now(),
                    null,
                    null
            );

            when(teamRepository.findByIdWithMembers(team.getId())).thenReturn(Optional.of(team));
            when(teamMapper.toDto(team)).thenReturn(expectedDto);

            // When
            Optional<TeamDto> result = queryService.getTeamDto(team.getId());

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo(team.getId());
            assertThat(result.get().name()).isEqualTo(team.getName());
            verify(teamRepository).findByIdWithMembers(team.getId());
            verify(teamMapper).toDto(team);
        }

        @Test
        @DisplayName("should return empty when team not found")
        void shouldReturnEmptyWhenTeamNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(teamRepository.findByIdWithMembers(nonExistentId)).thenReturn(Optional.empty());

            // When
            Optional<TeamDto> result = queryService.getTeamDto(nonExistentId);

            // Then
            assertThat(result).isEmpty();
            verify(teamRepository).findByIdWithMembers(nonExistentId);
            verify(teamMapper, never()).toDto(any(Team.class));
        }

        @Test
        @DisplayName("should handle bilingual team name in DTO conversion")
        void shouldHandleBilingualTeamNameInDto() {
            // Given
            String bilingualName = "Инновационная команда / Innovation Team";
            team.setName(bilingualName);

            TeamDto expectedDto = new TeamDto(
                    team.getId(),
                    bilingualName,
                    team.getDescription(),
                    TeamStatus.DRAFT,
                    null,
                    null,
                    List.of(),
                    2,
                    LocalDateTime.now(),
                    null,
                    null
            );

            when(teamRepository.findByIdWithMembers(team.getId())).thenReturn(Optional.of(team));
            when(teamMapper.toDto(team)).thenReturn(expectedDto);

            // When
            Optional<TeamDto> result = queryService.getTeamDto(team.getId());

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().name()).isEqualTo(bilingualName);
            assertThat(result.get().name()).contains("Инновационная");
            assertThat(result.get().name()).contains("Innovation");
        }
    }

    // ============================================
    // FIND TEAMS SEARCH TESTS
    // ============================================

    @Nested
    @DisplayName("findTeams search")
    class FindTeamsTests {

        private Pageable pageable;

        @BeforeEach
        void setUpPageable() {
            pageable = PageRequest.of(0, 10);
        }

        @Test
        @DisplayName("should search by name only when search provided without status")
        void shouldSearchByNameOnly() {
            // Given
            String searchTerm = "Development";
            Page<Team> teamPage = new PageImpl<>(List.of(team), pageable, 1);
            TeamSummaryDto summaryDto = createTeamSummaryDto(team);

            when(teamRepository.searchByNameAndStatus(searchTerm, null, pageable)).thenReturn(teamPage);
            when(teamMapper.toSummaryDto(team)).thenReturn(summaryDto);

            // When
            Page<TeamSummaryDto> result = queryService.findTeams(null, searchTerm, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).name()).isEqualTo(team.getName());
            verify(teamRepository).searchByNameAndStatus(searchTerm, null, pageable);
        }

        @Test
        @DisplayName("should filter by status only when status provided without search")
        void shouldFilterByStatusOnly() {
            // Given
            Page<Team> teamPage = new PageImpl<>(List.of(team), pageable, 1);
            TeamSummaryDto summaryDto = createTeamSummaryDto(team);

            when(teamRepository.findByStatus(TeamStatus.DRAFT, pageable)).thenReturn(teamPage);
            when(teamMapper.toSummaryDto(team)).thenReturn(summaryDto);

            // When
            Page<TeamSummaryDto> result = queryService.findTeams(TeamStatus.DRAFT, null, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            verify(teamRepository).findByStatus(TeamStatus.DRAFT, pageable);
        }

        @Test
        @DisplayName("should search by name and status when both provided")
        void shouldSearchByNameAndStatus() {
            // Given
            String searchTerm = "команда";
            TeamStatus status = TeamStatus.ACTIVE;
            Page<Team> teamPage = new PageImpl<>(List.of(team), pageable, 1);
            TeamSummaryDto summaryDto = createTeamSummaryDto(team);

            when(teamRepository.searchByNameAndStatus(searchTerm, status, pageable)).thenReturn(teamPage);
            when(teamMapper.toSummaryDto(team)).thenReturn(summaryDto);

            // When
            Page<TeamSummaryDto> result = queryService.findTeams(status, searchTerm, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            verify(teamRepository).searchByNameAndStatus(searchTerm, status, pageable);
        }

        @Test
        @DisplayName("should return all teams when no filters provided")
        void shouldReturnAllWhenNoFilters() {
            // Given
            Team team2 = TestDataFactory.createTeam(creator);
            team2.setId(UUID.randomUUID());
            team2.setName("Second Team / Вторая команда");

            Page<Team> teamPage = new PageImpl<>(List.of(team, team2), pageable, 2);

            when(teamRepository.findAll(pageable)).thenReturn(teamPage);
            when(teamMapper.toSummaryDto(any(Team.class))).thenAnswer(invocation -> {
                Team t = invocation.getArgument(0);
                return createTeamSummaryDto(t);
            });

            // When
            Page<TeamSummaryDto> result = queryService.findTeams(null, null, pageable);

            // Then
            assertThat(result.getContent()).hasSize(2);
            verify(teamRepository).findAll(pageable);
        }

        @Test
        @DisplayName("should return empty page when no matching teams")
        void shouldReturnEmptyPageWhenNoMatchingTeams() {
            // Given
            String searchTerm = "nonexistent";
            Page<Team> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(teamRepository.searchByNameAndStatus(searchTerm, null, pageable)).thenReturn(emptyPage);

            // When
            Page<TeamSummaryDto> result = queryService.findTeams(null, searchTerm, pageable);

            // Then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("should handle blank search string as no search")
        void shouldHandleBlankSearchStringAsNoSearch() {
            // Given
            String blankSearch = "   ";
            Page<Team> teamPage = new PageImpl<>(List.of(team), pageable, 1);
            TeamSummaryDto summaryDto = createTeamSummaryDto(team);

            when(teamRepository.findAll(pageable)).thenReturn(teamPage);
            when(teamMapper.toSummaryDto(team)).thenReturn(summaryDto);

            // When
            Page<TeamSummaryDto> result = queryService.findTeams(null, blankSearch, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            verify(teamRepository).findAll(pageable);
            verify(teamRepository, never()).searchByNameAndStatus(anyString(), any(), any());
        }

        @Test
        @DisplayName("should search with Cyrillic characters")
        void shouldSearchWithCyrillicCharacters() {
            // Given
            String cyrillicSearch = "Команда";
            Page<Team> teamPage = new PageImpl<>(List.of(team), pageable, 1);
            TeamSummaryDto summaryDto = createTeamSummaryDto(team);

            when(teamRepository.searchByNameAndStatus(cyrillicSearch, null, pageable)).thenReturn(teamPage);
            when(teamMapper.toSummaryDto(team)).thenReturn(summaryDto);

            // When
            Page<TeamSummaryDto> result = queryService.findTeams(null, cyrillicSearch, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            verify(teamRepository).searchByNameAndStatus(cyrillicSearch, null, pageable);
        }

        private TeamSummaryDto createTeamSummaryDto(Team team) {
            return new TeamSummaryDto(
                    team.getId(),
                    team.getName(),
                    team.getDescription(),
                    team.getStatus(),
                    null,
                    team.getActiveMemberCount(),
                    team.getCreatedAt()
            );
        }
    }

    // ============================================
    // FIND TEAMS BY MEMBER TESTS
    // ============================================

    @Nested
    @DisplayName("findTeamsByMember")
    class FindTeamsByMemberTests {

        @Test
        @DisplayName("should return teams for user with multiple teams")
        void shouldReturnTeamsForUserWithMultipleTeams() {
            // Given
            String clerkId = member1.getClerkId();

            Team team2 = TestDataFactory.createTeam(creator);
            team2.setId(UUID.randomUUID());
            team2.setName("Second Team / Вторая команда");

            TeamSummaryDto summaryDto1 = new TeamSummaryDto(
                    team.getId(), team.getName(), team.getDescription(),
                    team.getStatus(), null, 2, team.getCreatedAt()
            );
            TeamSummaryDto summaryDto2 = new TeamSummaryDto(
                    team2.getId(), team2.getName(), team2.getDescription(),
                    team2.getStatus(), null, 1, team2.getCreatedAt()
            );

            when(teamRepository.findByMemberClerkId(clerkId)).thenReturn(List.of(team, team2));
            when(teamMapper.toSummaryDto(team)).thenReturn(summaryDto1);
            when(teamMapper.toSummaryDto(team2)).thenReturn(summaryDto2);

            // When
            List<TeamSummaryDto> result = queryService.findTeamsByMember(clerkId);

            // Then
            assertThat(result).hasSize(2);
            verify(teamRepository).findByMemberClerkId(clerkId);
        }

        @Test
        @DisplayName("should return empty list for user with no teams")
        void shouldReturnEmptyForUserWithNoTeams() {
            // Given
            String clerkId = "clerk_no_teams_user";
            when(teamRepository.findByMemberClerkId(clerkId)).thenReturn(List.of());

            // When
            List<TeamSummaryDto> result = queryService.findTeamsByMember(clerkId);

            // Then
            assertThat(result).isEmpty();
            verify(teamRepository).findByMemberClerkId(clerkId);
        }

        @Test
        @DisplayName("should return single team for user in one team")
        void shouldReturnSingleTeamForUserInOneTeam() {
            // Given
            String clerkId = member2.getClerkId();
            TeamSummaryDto summaryDto = new TeamSummaryDto(
                    team.getId(), team.getName(), team.getDescription(),
                    team.getStatus(), null, 2, team.getCreatedAt()
            );

            when(teamRepository.findByMemberClerkId(clerkId)).thenReturn(List.of(team));
            when(teamMapper.toSummaryDto(team)).thenReturn(summaryDto);

            // When
            List<TeamSummaryDto> result = queryService.findTeamsByMember(clerkId);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(team.getId());
        }
    }

    // ============================================
    // GET TEAM MEMBERS TESTS
    // ============================================

    @Nested
    @DisplayName("getTeamMembers")
    class GetTeamMembersTests {

        @Test
        @DisplayName("should return all active team members")
        void shouldReturnAllActiveTeamMembers() {
            // Given
            TeamMemberDto memberDto1 = new TeamMemberDto(
                    member1.getId(), member1.getClerkId(), member1.getFullName(),
                    member1.getEmail(), null, TeamMemberRole.LEADER,
                    teamMember1.getJoinedAt(), true
            );
            TeamMemberDto memberDto2 = new TeamMemberDto(
                    member2.getId(), member2.getClerkId(), member2.getFullName(),
                    member2.getEmail(), null, TeamMemberRole.MEMBER,
                    teamMember2.getJoinedAt(), true
            );

            when(teamMemberRepository.findByTeamIdWithUser(team.getId()))
                    .thenReturn(List.of(teamMember1, teamMember2));
            when(teamMapper.toMemberDto(teamMember1)).thenReturn(memberDto1);
            when(teamMapper.toMemberDto(teamMember2)).thenReturn(memberDto2);

            // When
            List<TeamMemberDto> result = queryService.getTeamMembers(team.getId());

            // Then
            assertThat(result).hasSize(2);
            verify(teamMemberRepository).findByTeamIdWithUser(team.getId());
        }

        @Test
        @DisplayName("should return empty list for team with no members")
        void shouldReturnEmptyListForTeamWithNoMembers() {
            // Given
            UUID emptyTeamId = UUID.randomUUID();
            when(teamMemberRepository.findByTeamIdWithUser(emptyTeamId)).thenReturn(List.of());

            // When
            List<TeamMemberDto> result = queryService.getTeamMembers(emptyTeamId);

            // Then
            assertThat(result).isEmpty();
            verify(teamMemberRepository).findByTeamIdWithUser(emptyTeamId);
        }

        @Test
        @DisplayName("should map member with bilingual name correctly")
        void shouldMapMemberWithBilingualNameCorrectly() {
            // Given
            member1.setFirstName("Иван / Ivan");
            member1.setLastName("Петров / Petrov");

            TeamMemberDto memberDto = new TeamMemberDto(
                    member1.getId(), member1.getClerkId(), "Иван / Ivan Петров / Petrov",
                    member1.getEmail(), null, TeamMemberRole.LEADER,
                    teamMember1.getJoinedAt(), true
            );

            when(teamMemberRepository.findByTeamIdWithUser(team.getId()))
                    .thenReturn(List.of(teamMember1));
            when(teamMapper.toMemberDto(teamMember1)).thenReturn(memberDto);

            // When
            List<TeamMemberDto> result = queryService.getTeamMembers(team.getId());

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).fullName()).contains("Иван");
            assertThat(result.get(0).fullName()).contains("Ivan");
        }
    }

    // ============================================
    // UPDATE TEAM TESTS
    // ============================================

    @Nested
    @DisplayName("updateTeam")
    class UpdateTeamTests {

        @Test
        @DisplayName("should update name successfully")
        void shouldUpdateNameSuccessfully() {
            // Given
            String newName = "Updated Team / Обновленная команда";
            UpdateTeamRequest request = new UpdateTeamRequest(newName, null);

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
            when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Optional<Team> result = queryService.updateTeam(team.getId(), request);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo(newName);
            verify(teamRepository).save(team);
        }

        @Test
        @DisplayName("should update description successfully")
        void shouldUpdateDescriptionSuccessfully() {
            // Given
            String newDescription = "New description / Новое описание";
            UpdateTeamRequest request = new UpdateTeamRequest(null, newDescription);

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
            when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Optional<Team> result = queryService.updateTeam(team.getId(), request);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getDescription()).isEqualTo(newDescription);
            verify(teamRepository).save(team);
        }

        @Test
        @DisplayName("should update both name and description")
        void shouldUpdateBothNameAndDescription() {
            // Given
            String newName = "New Name / Новое название";
            String newDescription = "New Description / Новое описание";
            UpdateTeamRequest request = new UpdateTeamRequest(newName, newDescription);

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
            when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Optional<Team> result = queryService.updateTeam(team.getId(), request);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo(newName);
            assertThat(result.get().getDescription()).isEqualTo(newDescription);
        }

        @Test
        @DisplayName("should reject update for archived team")
        void shouldRejectUpdateForArchivedTeam() {
            // Given
            Team archivedTeam = TestDataFactory.createTeam(creator);
            archivedTeam.setId(team.getId());
            TeamMember tm = new TeamMember(archivedTeam, member1, TeamMemberRole.MEMBER);
            tm.setId(UUID.randomUUID());
            archivedTeam.getMembers().add(tm);
            archivedTeam.activate();
            archivedTeam.archive();

            UpdateTeamRequest request = new UpdateTeamRequest("New Name", null);

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(archivedTeam));

            // When & Then
            assertThatThrownBy(() -> queryService.updateTeam(team.getId(), request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("archived");
        }

        @Test
        @DisplayName("should skip blank name and keep original")
        void shouldSkipBlankName() {
            // Given
            String originalName = team.getName();
            UpdateTeamRequest request = new UpdateTeamRequest("   ", null);

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
            when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Optional<Team> result = queryService.updateTeam(team.getId(), request);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo(originalName);
        }

        @Test
        @DisplayName("should return empty when team not found")
        void shouldReturnEmptyWhenTeamNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            UpdateTeamRequest request = new UpdateTeamRequest("New Name", null);

            when(teamRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When
            Optional<Team> result = queryService.updateTeam(nonExistentId, request);

            // Then
            assertThat(result).isEmpty();
            verify(teamRepository, never()).save(any());
        }

        @Test
        @DisplayName("should allow empty description to clear existing")
        void shouldAllowEmptyDescriptionToClearExisting() {
            // Given
            UpdateTeamRequest request = new UpdateTeamRequest(null, "");

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
            when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Optional<Team> result = queryService.updateTeam(team.getId(), request);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getDescription()).isEmpty();
        }
    }

    // ============================================
    // REMOVE MEMBER TESTS
    // ============================================

    @Nested
    @DisplayName("removeMember")
    class RemoveMemberTests {

        @Test
        @DisplayName("should remove active member successfully")
        void shouldRemoveActiveMemberSuccessfully() {
            // Given
            when(teamMemberRepository.findByTeamIdAndUserId(team.getId(), member2.getId()))
                    .thenReturn(Optional.of(teamMember2));
            when(teamMemberRepository.save(any(TeamMember.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            boolean result = queryService.removeMember(team.getId(), member2.getId());

            // Then
            assertThat(result).isTrue();
            assertThat(teamMember2.isActive()).isFalse();
            assertThat(teamMember2.getLeftAt()).isNotNull();
            verify(teamMemberRepository).save(teamMember2);
        }

        @Test
        @DisplayName("should return false for non-existent member")
        void shouldReturnFalseForNonExistentMember() {
            // Given
            UUID nonExistentUserId = UUID.randomUUID();
            when(teamMemberRepository.findByTeamIdAndUserId(team.getId(), nonExistentUserId))
                    .thenReturn(Optional.empty());

            // When
            boolean result = queryService.removeMember(team.getId(), nonExistentUserId);

            // Then
            assertThat(result).isFalse();
            verify(teamMemberRepository, never()).save(any());
        }

        @Test
        @DisplayName("should return false for already inactive member")
        void shouldReturnFalseForAlreadyInactiveMember() {
            // Given
            teamMember2.remove(); // Make inactive

            when(teamMemberRepository.findByTeamIdAndUserId(team.getId(), member2.getId()))
                    .thenReturn(Optional.of(teamMember2));

            // When
            boolean result = queryService.removeMember(team.getId(), member2.getId());

            // Then
            assertThat(result).isFalse();
            verify(teamMemberRepository, never()).save(any());
        }

        @Test
        @DisplayName("should clear leader reference when removing leader")
        void shouldClearLeaderReferenceWhenRemovingLeader() throws Exception {
            // Given - set up leader on team
            Field leaderField = Team.class.getDeclaredField("leader");
            leaderField.setAccessible(true);
            leaderField.set(team, member1);

            when(teamMemberRepository.findByTeamIdAndUserId(team.getId(), member1.getId()))
                    .thenReturn(Optional.of(teamMember1));
            when(teamRepository.save(any(Team.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(teamMemberRepository.save(any(TeamMember.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            boolean result = queryService.removeMember(team.getId(), member1.getId());

            // Then
            assertThat(result).isTrue();
            assertThat(team.getLeader()).isNull();
            verify(teamRepository).save(team);
            verify(teamMemberRepository).save(teamMember1);
        }

        @Test
        @DisplayName("should not modify team when removing non-leader member")
        void shouldNotModifyTeamWhenRemovingNonLeaderMember() {
            // Given - team has no leader set
            when(teamMemberRepository.findByTeamIdAndUserId(team.getId(), member2.getId()))
                    .thenReturn(Optional.of(teamMember2));
            when(teamMemberRepository.save(any(TeamMember.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            boolean result = queryService.removeMember(team.getId(), member2.getId());

            // Then
            assertThat(result).isTrue();
            verify(teamRepository, never()).save(any());
            verify(teamMemberRepository).save(teamMember2);
        }
    }

    // ============================================
    // GET STATISTICS TESTS
    // ============================================

    @Nested
    @DisplayName("getStatistics")
    class GetStatisticsTests {

        @Test
        @DisplayName("should return correct counts for all statuses")
        void shouldReturnCorrectCounts() {
            // Given
            when(teamRepository.count()).thenReturn(10L);
            when(teamRepository.countByStatus(TeamStatus.DRAFT)).thenReturn(3L);
            when(teamRepository.countByStatus(TeamStatus.ACTIVE)).thenReturn(5L);
            when(teamRepository.countByStatus(TeamStatus.ARCHIVED)).thenReturn(2L);

            // When
            TeamQueryService.TeamStatistics stats = queryService.getStatistics();

            // Then
            assertThat(stats.totalTeams()).isEqualTo(10L);
            assertThat(stats.draftTeams()).isEqualTo(3L);
            assertThat(stats.activeTeams()).isEqualTo(5L);
            assertThat(stats.archivedTeams()).isEqualTo(2L);

            verify(teamRepository).count();
            verify(teamRepository).countByStatus(TeamStatus.DRAFT);
            verify(teamRepository).countByStatus(TeamStatus.ACTIVE);
            verify(teamRepository).countByStatus(TeamStatus.ARCHIVED);
        }

        @Test
        @DisplayName("should handle zero counts correctly")
        void shouldHandleZeroCounts() {
            // Given
            when(teamRepository.count()).thenReturn(0L);
            when(teamRepository.countByStatus(TeamStatus.DRAFT)).thenReturn(0L);
            when(teamRepository.countByStatus(TeamStatus.ACTIVE)).thenReturn(0L);
            when(teamRepository.countByStatus(TeamStatus.ARCHIVED)).thenReturn(0L);

            // When
            TeamQueryService.TeamStatistics stats = queryService.getStatistics();

            // Then
            assertThat(stats.totalTeams()).isZero();
            assertThat(stats.draftTeams()).isZero();
            assertThat(stats.activeTeams()).isZero();
            assertThat(stats.archivedTeams()).isZero();
        }

        @Test
        @DisplayName("should return statistics when only draft teams exist")
        void shouldReturnStatisticsWhenOnlyDraftTeamsExist() {
            // Given
            when(teamRepository.count()).thenReturn(5L);
            when(teamRepository.countByStatus(TeamStatus.DRAFT)).thenReturn(5L);
            when(teamRepository.countByStatus(TeamStatus.ACTIVE)).thenReturn(0L);
            when(teamRepository.countByStatus(TeamStatus.ARCHIVED)).thenReturn(0L);

            // When
            TeamQueryService.TeamStatistics stats = queryService.getStatistics();

            // Then
            assertThat(stats.totalTeams()).isEqualTo(5L);
            assertThat(stats.draftTeams()).isEqualTo(5L);
            assertThat(stats.activeTeams()).isZero();
            assertThat(stats.archivedTeams()).isZero();
        }

        @Test
        @DisplayName("should calculate total correctly from individual status counts")
        void shouldCalculateTotalCorrectlyFromIndividualStatusCounts() {
            // Given
            when(teamRepository.count()).thenReturn(100L);
            when(teamRepository.countByStatus(TeamStatus.DRAFT)).thenReturn(25L);
            when(teamRepository.countByStatus(TeamStatus.ACTIVE)).thenReturn(60L);
            when(teamRepository.countByStatus(TeamStatus.ARCHIVED)).thenReturn(15L);

            // When
            TeamQueryService.TeamStatistics stats = queryService.getStatistics();

            // Then
            long sumOfStatuses = stats.draftTeams() + stats.activeTeams() + stats.archivedTeams();
            assertThat(sumOfStatuses).isEqualTo(stats.totalTeams());
        }
    }

    // ============================================
    // EDGE CASES AND INTEGRATION SCENARIOS
    // ============================================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("should handle null pageable gracefully in findTeams")
        void shouldHandleNullPageableGracefully() {
            // Note: In production, Spring provides a default pageable
            // This test verifies behavior when explicitly passed
            Pageable pageable = PageRequest.of(0, 20);
            Page<Team> teamPage = new PageImpl<>(List.of(team), pageable, 1);

            when(teamRepository.findAll(pageable)).thenReturn(teamPage);
            when(teamMapper.toSummaryDto(team)).thenReturn(
                    new TeamSummaryDto(team.getId(), team.getName(), team.getDescription(),
                            team.getStatus(), null, 2, team.getCreatedAt())
            );

            // When
            Page<TeamSummaryDto> result = queryService.findTeams(null, null, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("should handle team with special characters in name")
        void shouldHandleTeamWithSpecialCharactersInName() {
            // Given
            team.setName("Team <Dev> & QA / Команда <Разработка> и Тестирование");
            String searchTerm = "<Dev>";

            Page<Team> teamPage = new PageImpl<>(List.of(team), PageRequest.of(0, 10), 1);
            TeamSummaryDto summaryDto = new TeamSummaryDto(
                    team.getId(), team.getName(), team.getDescription(),
                    team.getStatus(), null, 2, team.getCreatedAt()
            );

            when(teamRepository.searchByNameAndStatus(searchTerm, null, PageRequest.of(0, 10)))
                    .thenReturn(teamPage);
            when(teamMapper.toSummaryDto(team)).thenReturn(summaryDto);

            // When
            Page<TeamSummaryDto> result = queryService.findTeams(null, searchTerm, PageRequest.of(0, 10));

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).name()).contains("<Dev>");
        }

        @Test
        @DisplayName("should handle concurrent member removal correctly")
        void shouldHandleConcurrentMemberRemovalCorrectly() {
            // Given - member already removed by another thread
            teamMember2.remove();
            LocalDateTime originalLeftAt = teamMember2.getLeftAt();

            when(teamMemberRepository.findByTeamIdAndUserId(team.getId(), member2.getId()))
                    .thenReturn(Optional.of(teamMember2));

            // When
            boolean result = queryService.removeMember(team.getId(), member2.getId());

            // Then
            assertThat(result).isFalse();
            // leftAt should not be modified
            assertThat(teamMember2.getLeftAt()).isEqualTo(originalLeftAt);
        }

        @Test
        @DisplayName("should preserve original values when update has null fields")
        void shouldPreserveOriginalValuesWhenUpdateHasNullFields() {
            // Given
            String originalName = team.getName();
            String originalDescription = team.getDescription();
            UpdateTeamRequest request = new UpdateTeamRequest(null, null);

            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
            when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Optional<Team> result = queryService.updateTeam(team.getId(), request);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo(originalName);
            assertThat(result.get().getDescription()).isEqualTo(originalDescription);
        }
    }
}
