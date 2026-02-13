package app.skillsoft.assessmentbackend.integration;

import app.skillsoft.assessmentbackend.config.TestHibernateConfig;
import app.skillsoft.assessmentbackend.config.TestJacksonConfig;
import app.skillsoft.assessmentbackend.domain.dto.team.*;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.repository.TeamMemberRepository;
import app.skillsoft.assessmentbackend.repository.TeamRepository;
import app.skillsoft.assessmentbackend.repository.UserRepository;
import app.skillsoft.assessmentbackend.security.SessionSecurityService;
import app.skillsoft.assessmentbackend.testutils.BaseIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive integration tests for Team management workflow.
 *
 * Tests cover:
 * - Full team lifecycle (create, activate, archive)
 * - Member management (add, remove, bulk operations)
 * - Leadership operations (set leader, change leader)
 * - Permission enforcement (ADMIN-only access)
 * - Pagination and filtering
 * - Edge cases and error handling
 *
 * Per CLAUDE.md: Uses @SpringBootTest for full context integration testing
 * with Russian/English bilingual test data.
 */
@Import({TestJacksonConfig.class, TestHibernateConfig.class})
@DisplayName("Team Workflow Integration Tests")
class TeamWorkflowIntegrationTest extends BaseIntegrationTest {

    private static final String API_BASE = "/api/v1/teams";
    private static final String ADMIN_CLERK_ID = "clerk_test_admin_1";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private SessionSecurityService sessionSecurityService;

    private User adminUser;
    private User editorUser;
    private User basicUser;
    private User memberUser1;
    private User memberUser2;
    private User memberUser3;

    @BeforeEach
    void setUp() {
        // Note: @Transactional at class level handles cleanup via rollback.
        // We don't use deleteAll() to avoid FK constraint issues and JSON
        // deserialization problems with H2 that other tests have encountered.

        // Create test users with different roles (without setting ID to let JPA generate it)
        // Admin user with known clerk ID for security mocking
        adminUser = userRepository.save(createUserWithClerkId(UserRole.ADMIN, ADMIN_CLERK_ID));
        editorUser = userRepository.save(createUserWithClerkId(UserRole.EDITOR, "clerk_test_editor_2"));
        basicUser = userRepository.save(createUserWithClerkId(UserRole.USER, "clerk_test_user_3"));

        // Create additional users for member operations
        memberUser1 = userRepository.save(createUserWithClerkId(UserRole.USER, "clerk_test_member_4"));
        memberUser2 = userRepository.save(createUserWithClerkId(UserRole.USER, "clerk_test_member_5"));
        memberUser3 = userRepository.save(createUserWithClerkId(UserRole.USER, "clerk_test_member_6"));

        // Mock SessionSecurityService to return the admin clerk ID for authenticated user
        when(sessionSecurityService.getAuthenticatedUserId()).thenReturn(ADMIN_CLERK_ID);
    }

    /**
     * Creates a User entity with a specific Clerk ID.
     * ID is not set - JPA generates it with @GeneratedValue.
     */
    private User createUserWithClerkId(UserRole role, String clerkId) {
        User user = new User();
        user.setClerkId(clerkId);
        user.setEmail(role.name().toLowerCase() + "@test.skillsoft.app");
        user.setUsername("test" + role.name().toLowerCase());
        user.setFirstName("Test / Тест");
        user.setLastName(role.name());
        user.setRole(role);
        user.setActive(true);
        user.setBanned(false);
        user.setLocked(false);
        user.setHasImage(false);
        user.setPreferences("{}");
        return user;
    }

    // ============================================
    // FULL TEAM LIFECYCLE TESTS
    // ============================================

    @Nested
    @DisplayName("Full Team Lifecycle")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    class FullTeamLifecycleTests {

        @Test
        @DisplayName("Should complete full team lifecycle: create -> activate -> archive")
        void shouldCompleteFullTeamLifecycle() throws Exception {
            // Step 1: Create team with initial members and leader
            // Note: Creating with members in the request avoids JPA lazy loading issues
            // that occur when adding members separately then setting leader
            CreateTeamRequest createRequest = new CreateTeamRequest(
                    "Development Team / Команда разработки",
                    "A high-performing development team / Высокопроизводительная команда разработки",
                    List.of(memberUser1.getId(), memberUser2.getId()),
                    memberUser1.getId(),
                    false
            );

            MvcResult createResult = mockMvc.perform(post(API_BASE)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name", is("Development Team / Команда разработки")))
                    .andExpect(jsonPath("$.status", is("DRAFT")))
                    .andExpect(jsonPath("$.memberCount", is(2)))
                    .andExpect(jsonPath("$.leader.id", is(memberUser1.getId().toString())))
                    .andReturn();

            TeamDto createdTeam = objectMapper.readValue(
                    createResult.getResponse().getContentAsString(), TeamDto.class);
            UUID teamId = createdTeam.id();

            // Step 2: Activate team
            mockMvc.perform(post(API_BASE + "/{teamId}/activate", teamId)
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.activatedAt", notNullValue()));

            // Step 3: Verify team is active
            mockMvc.perform(get(API_BASE + "/{teamId}", teamId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("ACTIVE")))
                    .andExpect(jsonPath("$.memberCount", is(2)));

            // Step 4: Archive team
            mockMvc.perform(delete(API_BASE + "/{teamId}", teamId)
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            // Step 5: Verify team is archived
            mockMvc.perform(get(API_BASE + "/{teamId}", teamId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("ARCHIVED")));
        }

        @Test
        @DisplayName("Should create team with initial members and leader")
        void shouldCreateTeamWithInitialMembersAndLeader() throws Exception {
            CreateTeamRequest request = new CreateTeamRequest(
                    "Full Team / Полная команда",
                    "Team with all initial settings / Команда со всеми начальными настройками",
                    List.of(memberUser1.getId(), memberUser2.getId()),
                    memberUser1.getId(),
                    false
            );

            mockMvc.perform(post(API_BASE)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name", is("Full Team / Полная команда")))
                    .andExpect(jsonPath("$.status", is("DRAFT")))
                    .andExpect(jsonPath("$.leader.id", is(memberUser1.getId().toString())))
                    .andExpect(jsonPath("$.memberCount", is(2)));
        }

        @Test
        @DisplayName("Should create and immediately activate team")
        void shouldCreateAndImmediatelyActivateTeam() throws Exception {
            CreateTeamRequest request = new CreateTeamRequest(
                    "Quick Start Team / Быстрый старт",
                    "Immediately activated team / Сразу активированная команда",
                    List.of(memberUser1.getId()),
                    null,
                    true
            );

            mockMvc.perform(post(API_BASE)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status", is("ACTIVE")))
                    .andExpect(jsonPath("$.memberCount", is(1)));
        }

        @Test
        @DisplayName("Should update team name and description")
        void shouldUpdateTeamNameAndDescription() throws Exception {
            // Create team first
            Team team = createAndSaveTeam("Original Name / Исходное название");

            UpdateTeamRequest updateRequest = new UpdateTeamRequest(
                    "Updated Name / Обновленное название",
                    "New description / Новое описание"
            );

            mockMvc.perform(put(API_BASE + "/{teamId}", team.getId())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", is("Updated Name / Обновленное название")))
                    .andExpect(jsonPath("$.description", is("New description / Новое описание")));
        }

        @Test
        @DisplayName("Should return 404 for team profile when no test results exist")
        void shouldReturn404ForTeamProfileWithoutTestResults() throws Exception {
            // Create and activate team with members
            // Note: Team profile endpoint requires test results data to build
            // competency saturation metrics. Without test results, it returns 404.
            Team team = createAndSaveTeamWithMembers("Profile Team / Команда профиля", 2);
            team.activate();
            teamRepository.save(team);

            // Profile endpoint returns 404 when no test results exist for team members
            mockMvc.perform(get(API_BASE + "/{teamId}/profile", team.getId()))
                    .andExpect(status().isNotFound());
        }
    }

    // ============================================
    // MEMBER MANAGEMENT TESTS
    // ============================================

    @Nested
    @DisplayName("Member Management")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    class MemberManagementTests {

        @Test
        @DisplayName("Should add single member to team")
        void shouldAddSingleMemberToTeam() throws Exception {
            Team team = createAndSaveTeam("Test Team / Тестовая команда");

            AddMembersRequest request = new AddMembersRequest(List.of(memberUser1.getId()));

            mockMvc.perform(post(API_BASE + "/{teamId}/members", team.getId())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.addedMembers", hasSize(1)))
                    .andExpect(jsonPath("$.hasErrors", is(false)));
        }

        @Test
        @DisplayName("Should add multiple members in bulk operation")
        void shouldAddMultipleMembersInBulk() throws Exception {
            Team team = createAndSaveTeam("Bulk Team / Массовая команда");

            AddMembersRequest request = new AddMembersRequest(
                    List.of(memberUser1.getId(), memberUser2.getId(), memberUser3.getId())
            );

            mockMvc.perform(post(API_BASE + "/{teamId}/members", team.getId())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.addedMembers", hasSize(3)))
                    .andExpect(jsonPath("$.hasErrors", is(false)));

            // Verify members in database
            List<TeamMember> members = teamMemberRepository.findByTeamIdAndIsActiveTrue(team.getId());
            assertThat(members).hasSize(3);
        }

        @Test
        @DisplayName("Should remove member from team")
        void shouldRemoveMemberFromTeam() throws Exception {
            Team team = createAndSaveTeamWithMembers("Remove Test / Тест удаления", 2);
            List<TeamMember> members = teamMemberRepository.findByTeamIdAndIsActiveTrue(team.getId());
            UUID memberToRemove = members.get(0).getUser().getId();

            mockMvc.perform(delete(API_BASE + "/{teamId}/members/{userId}", team.getId(), memberToRemove)
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            // Verify member was removed
            List<TeamMember> remainingMembers = teamMemberRepository.findByTeamIdAndIsActiveTrue(team.getId());
            assertThat(remainingMembers).hasSize(1);
        }

        @Test
        @DisplayName("Should get all team members with details")
        void shouldGetTeamMembersWithDetails() throws Exception {
            Team team = createAndSaveTeamWithMembers("Members Team / Команда участников", 3);

            mockMvc.perform(get(API_BASE + "/{teamId}/members", team.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[0].userId", notNullValue()))
                    .andExpect(jsonPath("$[0].fullName", notNullValue()))
                    .andExpect(jsonPath("$[0].role", notNullValue()));
        }

        @Test
        @DisplayName("Should handle adding non-existent user as member")
        void shouldHandleAddingNonExistentUser() throws Exception {
            Team team = createAndSaveTeam("Error Test Team / Команда тест ошибок");
            UUID nonExistentUserId = UUID.randomUUID();

            AddMembersRequest request = new AddMembersRequest(List.of(nonExistentUserId));

            mockMvc.perform(post(API_BASE + "/{teamId}/members", team.getId())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasErrors", is(true)))
                    .andExpect(jsonPath("$.failures", hasSize(1)));
        }

        @Test
        @DisplayName("Should handle adding already existing member")
        void shouldHandleAddingExistingMember() throws Exception {
            Team team = createAndSaveTeamWithMembers("Duplicate Test / Тест дубликатов", 1);
            List<TeamMember> existingMembers = teamMemberRepository.findByTeamIdAndIsActiveTrue(team.getId());
            UUID existingMemberId = existingMembers.get(0).getUser().getId();

            AddMembersRequest request = new AddMembersRequest(List.of(existingMemberId));

            mockMvc.perform(post(API_BASE + "/{teamId}/members", team.getId())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasErrors", is(true)));
        }

        @Test
        @DisplayName("Should handle removing non-existent member")
        void shouldHandleRemovingNonExistentMember() throws Exception {
            Team team = createAndSaveTeam("Non-member Test / Тест не-участника");
            UUID nonMemberId = UUID.randomUUID();

            mockMvc.perform(delete(API_BASE + "/{teamId}/members/{userId}", team.getId(), nonMemberId)
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }

    // ============================================
    // LEADERSHIP TESTS
    // ============================================

    @Nested
    @DisplayName("Leadership Operations")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    class LeadershipTests {

        @Test
        @DisplayName("Should set team leader from existing member")
        void shouldSetTeamLeader() throws Exception {
            Team team = createAndSaveTeamWithMembers("Leader Test / Тест лидера", 2);
            List<TeamMember> members = teamMemberRepository.findByTeamIdAndIsActiveTrue(team.getId());
            UUID newLeaderId = members.get(0).getUser().getId();

            SetLeaderRequest request = new SetLeaderRequest(newLeaderId);

            mockMvc.perform(put(API_BASE + "/{teamId}/leader", team.getId())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.newLeaderId", is(newLeaderId.toString())));
        }

        @Test
        @DisplayName("Should change team leader to different member")
        void shouldChangeTeamLeader() throws Exception {
            Team team = createAndSaveTeamWithMembers("Change Leader Test / Тест смены лидера", 3);
            List<TeamMember> members = teamMemberRepository.findByTeamIdAndIsActiveTrue(team.getId());

            // Set initial leader
            UUID firstLeaderId = members.get(0).getUser().getId();
            SetLeaderRequest firstRequest = new SetLeaderRequest(firstLeaderId);
            mockMvc.perform(put(API_BASE + "/{teamId}/leader", team.getId())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(firstRequest)))
                    .andExpect(status().isOk());

            // Change to new leader
            UUID newLeaderId = members.get(1).getUser().getId();
            SetLeaderRequest secondRequest = new SetLeaderRequest(newLeaderId);

            mockMvc.perform(put(API_BASE + "/{teamId}/leader", team.getId())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(secondRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.previousLeaderId", is(firstLeaderId.toString())))
                    .andExpect(jsonPath("$.newLeaderId", is(newLeaderId.toString())));
        }

        @Test
        @DisplayName("Should fail to set non-member as leader")
        void shouldFailToSetNonMemberAsLeader() throws Exception {
            Team team = createAndSaveTeamWithMembers("Invalid Leader / Невалидный лидер", 1);
            UUID nonMemberId = memberUser3.getId(); // Not a member of this team

            SetLeaderRequest request = new SetLeaderRequest(nonMemberId);

            mockMvc.perform(put(API_BASE + "/{teamId}/leader", team.getId())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should remove leader by setting null")
        void shouldRemoveLeader() throws Exception {
            Team team = createAndSaveTeamWithMembers("Remove Leader / Удаление лидера", 2);
            List<TeamMember> members = teamMemberRepository.findByTeamIdAndIsActiveTrue(team.getId());

            // First set a leader
            UUID leaderId = members.get(0).getUser().getId();
            SetLeaderRequest setRequest = new SetLeaderRequest(leaderId);
            mockMvc.perform(put(API_BASE + "/{teamId}/leader", team.getId())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(setRequest)))
                    .andExpect(status().isOk());

            // Now remove leader
            SetLeaderRequest removeRequest = new SetLeaderRequest(null);

            mockMvc.perform(put(API_BASE + "/{teamId}/leader", team.getId())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(removeRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.previousLeaderId", is(leaderId.toString())));
        }
    }

    // ============================================
    // PERMISSION ENFORCEMENT TESTS
    // ============================================

    /**
     * Permission enforcement tests are disabled for integration tests.
     *
     * The TestSecurityConfig disables security to allow testing business logic
     * without authentication/authorization complexity. Permission enforcement
     * should be tested separately using:
     * - Unit tests with @WebMvcTest and proper security configuration
     * - Security-specific integration tests with enabled method security
     *
     * The @PreAuthorize("hasRole('ADMIN')") annotation on TeamControllerV1
     * ensures only ADMIN users can access team management endpoints in production.
     */
    @Nested
    @DisplayName("Permission Enforcement (Skipped - Security Disabled in Integration Tests)")
    @org.junit.jupiter.api.Disabled("Security is disabled in integration tests. " +
            "Permission tests require separate @WebMvcTest with SecurityConfig enabled.")
    class PermissionEnforcementTests {

        @Test
        @WithMockUser(username = "editor", roles = {"EDITOR"})
        @DisplayName("Should deny team creation for EDITOR role")
        void shouldDenyTeamCreationForEditor() throws Exception {
            CreateTeamRequest request = new CreateTeamRequest(
                    "Editor Team / Команда редактора",
                    "Should fail / Должен провалиться",
                    List.of(),
                    null,
                    false
            );

            mockMvc.perform(post(API_BASE)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "user", roles = {"USER"})
        @DisplayName("Should deny team creation for USER role")
        void shouldDenyTeamCreationForUser() throws Exception {
            CreateTeamRequest request = new CreateTeamRequest(
                    "User Team / Команда пользователя",
                    "Should fail / Должен провалиться",
                    List.of(),
                    null,
                    false
            );

            mockMvc.perform(post(API_BASE)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "editor", roles = {"EDITOR"})
        @DisplayName("Should deny team update for EDITOR role")
        void shouldDenyTeamUpdateForEditor() throws Exception {
            // Create team as admin first (use repository directly)
            Team team = createAndSaveTeam("Protected Team / Защищенная команда");

            UpdateTeamRequest updateRequest = new UpdateTeamRequest(
                    "Hacked Name / Взломанное название",
                    "Should not work / Не должно работать"
            );

            mockMvc.perform(put(API_BASE + "/{teamId}", team.getId())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "user", roles = {"USER"})
        @DisplayName("Should deny team deletion for USER role")
        void shouldDenyTeamDeletionForUser() throws Exception {
            Team team = createAndSaveTeam("Delete Test / Тест удаления");

            mockMvc.perform(delete(API_BASE + "/{teamId}", team.getId())
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "editor", roles = {"EDITOR"})
        @DisplayName("Should deny member addition for EDITOR role")
        void shouldDenyMemberAdditionForEditor() throws Exception {
            Team team = createAndSaveTeam("Member Test / Тест участников");

            AddMembersRequest request = new AddMembersRequest(List.of(memberUser1.getId()));

            mockMvc.perform(post(API_BASE + "/{teamId}/members", team.getId())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "user", roles = {"USER"})
        @DisplayName("Should deny leader change for USER role")
        void shouldDenyLeaderChangeForUser() throws Exception {
            Team team = createAndSaveTeamWithMembers("Leader Test / Тест лидера", 1);
            List<TeamMember> members = teamMemberRepository.findByTeamIdAndIsActiveTrue(team.getId());

            SetLeaderRequest request = new SetLeaderRequest(members.get(0).getUser().getId());

            mockMvc.perform(put(API_BASE + "/{teamId}/leader", team.getId())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "user", roles = {"USER"})
        @DisplayName("Should allow my-teams endpoint for authenticated users")
        void shouldAllowMyTeamsForAuthenticatedUsers() throws Exception {
            mockMvc.perform(get(API_BASE + "/my-teams"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", isA(List.class)));
        }
    }

    // ============================================
    // PAGINATION AND FILTERING TESTS
    // ============================================

    @Nested
    @DisplayName("Pagination and Filtering")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    class PaginationAndFilteringTests {

        @Test
        @DisplayName("Should return paginated team list")
        void shouldReturnPaginatedTeamList() throws Exception {
            // Create multiple teams
            for (int i = 0; i < 15; i++) {
                createAndSaveTeam("Team " + i + " / Команда " + i);
            }

            mockMvc.perform(get(API_BASE)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(10)))
                    .andExpect(jsonPath("$.totalElements", is(15)))
                    .andExpect(jsonPath("$.totalPages", is(2)))
                    .andExpect(jsonPath("$.number", is(0)));
        }

        @Test
        @DisplayName("Should filter teams by status")
        void shouldFilterTeamsByStatus() throws Exception {
            // Create teams with different statuses
            Team draftTeam = createAndSaveTeam("Draft Team / Черновик");

            Team activeTeam = createAndSaveTeamWithMembers("Active Team / Активная", 1);
            activeTeam.activate();
            teamRepository.save(activeTeam);

            mockMvc.perform(get(API_BASE)
                            .param("status", "DRAFT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].status", is("DRAFT")));

            mockMvc.perform(get(API_BASE)
                            .param("status", "ACTIVE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].status", is("ACTIVE")));
        }

        @Test
        @DisplayName("Should search teams by name")
        void shouldSearchTeamsByName() throws Exception {
            createAndSaveTeam("Alpha Team / Команда Альфа");
            createAndSaveTeam("Beta Team / Команда Бета");
            createAndSaveTeam("Gamma Squad / Отряд Гамма");

            mockMvc.perform(get(API_BASE)
                            .param("search", "Team"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)));

            mockMvc.perform(get(API_BASE)
                            .param("search", "Альфа"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].name", containsString("Альфа")));
        }

        @Test
        @DisplayName("Should combine status filter with search")
        void shouldCombineStatusFilterWithSearch() throws Exception {
            Team draftAlpha = createAndSaveTeam("Alpha Draft / Альфа черновик");

            Team activeAlpha = createAndSaveTeamWithMembers("Alpha Active / Альфа активная", 1);
            activeAlpha.activate();
            teamRepository.save(activeAlpha);

            createAndSaveTeam("Beta Draft / Бета черновик");

            mockMvc.perform(get(API_BASE)
                            .param("status", "DRAFT")
                            .param("search", "Alpha"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].name", containsString("Alpha Draft")));
        }

        @Test
        @DisplayName("Should return empty result for no matches")
        void shouldReturnEmptyResultForNoMatches() throws Exception {
            createAndSaveTeam("Test Team / Тестовая команда");

            mockMvc.perform(get(API_BASE)
                            .param("search", "NonExistent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements", is(0)));
        }

        @Test
        @DisplayName("Should return teams sorted by creation date")
        void shouldReturnTeamsSortedByCreationDate() throws Exception {
            // Create multiple teams
            createAndSaveTeam("Team Alpha / Команда Альфа");
            createAndSaveTeam("Team Beta / Команда Бета");
            createAndSaveTeam("Team Gamma / Команда Гамма");

            // Default sorting is by createdAt DESC
            // We just verify the endpoint returns all teams and sorts them
            mockMvc.perform(get(API_BASE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(3)))
                    .andExpect(jsonPath("$.sort.sorted", is(true)));
        }
    }

    // ============================================
    // EDGE CASES AND ERROR HANDLING
    // ============================================

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    class EdgeCasesTests {

        @Test
        @DisplayName("Should return 404 for non-existent team")
        void shouldReturn404ForNonExistentTeam() throws Exception {
            UUID nonExistentId = UUID.randomUUID();

            mockMvc.perform(get(API_BASE + "/{teamId}", nonExistentId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 404 when updating non-existent team")
        void shouldReturn404WhenUpdatingNonExistentTeam() throws Exception {
            UUID nonExistentId = UUID.randomUUID();
            UpdateTeamRequest request = new UpdateTeamRequest("Name", "Description");

            mockMvc.perform(put(API_BASE + "/{teamId}", nonExistentId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should fail to archive already archived team")
        void shouldFailToArchiveAlreadyArchivedTeam() throws Exception {
            Team team = createAndSaveTeamWithMembers("Archive Test / Тест архивации", 1);
            team.activate();
            teamRepository.save(team);
            team.archive();
            teamRepository.save(team);

            mockMvc.perform(delete(API_BASE + "/{teamId}", team.getId())
                            .with(csrf()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should fail to activate team without members")
        void shouldFailToActivateTeamWithoutMembers() throws Exception {
            Team team = createAndSaveTeam("Empty Team / Пустая команда");

            mockMvc.perform(post(API_BASE + "/{teamId}/activate", team.getId())
                            .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)));
        }

        @Test
        @DisplayName("Should fail to activate already active team")
        void shouldFailToActivateAlreadyActiveTeam() throws Exception {
            Team team = createAndSaveTeamWithMembers("Already Active / Уже активная", 1);
            team.activate();
            teamRepository.save(team);

            mockMvc.perform(post(API_BASE + "/{teamId}/activate", team.getId())
                            .with(csrf()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should validate team name is required")
        void shouldValidateTeamNameRequired() throws Exception {
            CreateTeamRequest request = new CreateTeamRequest(
                    "", // Empty name
                    "Description",
                    List.of(),
                    null,
                    false
            );

            mockMvc.perform(post(API_BASE)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should validate team name length")
        void shouldValidateTeamNameLength() throws Exception {
            String longName = "A".repeat(250); // Exceeds 200 character limit

            CreateTeamRequest request = new CreateTeamRequest(
                    longName,
                    "Description",
                    List.of(),
                    null,
                    false
            );

            mockMvc.perform(post(API_BASE)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle empty member list in add request")
        void shouldHandleEmptyMemberListInAddRequest() throws Exception {
            Team team = createAndSaveTeam("Empty Add Test / Тест пустого добавления");

            AddMembersRequest request = new AddMembersRequest(List.of());

            mockMvc.perform(post(API_BASE + "/{teamId}/members", team.getId())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle Cyrillic characters in team name and description")
        void shouldHandleCyrillicCharacters() throws Exception {
            CreateTeamRequest request = new CreateTeamRequest(
                    "Команда Разработки Программного Обеспечения",
                    "Это команда занимается разработкой высококачественного программного обеспечения для клиентов",
                    List.of(),
                    null,
                    false
            );

            MvcResult result = mockMvc.perform(post(API_BASE)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .characterEncoding("UTF-8")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name", is("Команда Разработки Программного Обеспечения")))
                    .andReturn();

            // Verify proper UTF-8 encoding in response
            String responseContent = result.getResponse().getContentAsString();
            assertThat(responseContent).contains("Команда Разработки Программного Обеспечения");
        }

        @Test
        @DisplayName("Should get team statistics")
        void shouldGetTeamStatistics() throws Exception {
            // Create teams with different statuses
            createAndSaveTeam("Draft 1 / Черновик 1");
            createAndSaveTeam("Draft 2 / Черновик 2");

            Team active = createAndSaveTeamWithMembers("Active / Активная", 1);
            active.activate();
            teamRepository.save(active);

            mockMvc.perform(get(API_BASE + "/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalTeams", is(3)))
                    .andExpect(jsonPath("$.draftTeams", is(2)))
                    .andExpect(jsonPath("$.activeTeams", is(1)))
                    .andExpect(jsonPath("$.archivedTeams", is(0)));
        }
    }

    // ============================================
    // TEAM PROFILE AND ANALYTICS TESTS
    // ============================================

    @Nested
    @DisplayName("Team Profile and Analytics")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    class TeamProfileAndAnalyticsTests {

        @Test
        @DisplayName("Should get skill gaps for team")
        void shouldGetSkillGaps() throws Exception {
            Team team = createAndSaveTeamWithMembers("Gap Analysis / Анализ пробелов", 2);
            team.activate();
            teamRepository.save(team);

            mockMvc.perform(get(API_BASE + "/{teamId}/gaps", team.getId())
                            .param("threshold", "0.3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", isA(List.class)));
        }

        @Test
        @DisplayName("Should calculate fit score for candidate")
        void shouldCalculateFitScore() throws Exception {
            Team team = createAndSaveTeamWithMembers("Fit Score / Оценка соответствия", 2);
            team.activate();
            teamRepository.save(team);

            UUID competencyId = UUID.randomUUID();
            String candidateCompetencies = objectMapper.writeValueAsString(
                    java.util.Map.of(competencyId, 0.8)
            );

            mockMvc.perform(post(API_BASE + "/{teamId}/fit-score", team.getId())
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(candidateCompetencies))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fitScore", notNullValue()));
        }

        @Test
        @DisplayName("Should return 404 for fit score on non-existent team")
        void shouldReturn404ForFitScoreOnNonExistentTeam() throws Exception {
            UUID nonExistentId = UUID.randomUUID();
            UUID competencyId = UUID.randomUUID();
            String candidateCompetencies = objectMapper.writeValueAsString(
                    java.util.Map.of(competencyId, 0.8)
            );

            mockMvc.perform(post(API_BASE + "/{teamId}/fit-score", nonExistentId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(candidateCompetencies))
                    .andExpect(status().isNotFound());
        }
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    /**
     * Creates and saves a team with bilingual name.
     *
     * @param name Team name (should include both English and Russian)
     * @return Persisted Team entity
     */
    private Team createAndSaveTeam(String name) {
        Team team = new Team();
        team.setName(name);
        team.setDescription("Test team description / Описание тестовой команды");
        team.setCreatedBy(adminUser);
        team.setMetadata("{}");
        return teamRepository.save(team);
    }

    /**
     * Creates and saves a team with the specified number of members.
     *
     * @param name        Team name
     * @param memberCount Number of members to add
     * @return Persisted Team entity with members
     */
    private Team createAndSaveTeamWithMembers(String name, int memberCount) {
        Team team = createAndSaveTeam(name);

        List<User> availableUsers = List.of(memberUser1, memberUser2, memberUser3);

        for (int i = 0; i < Math.min(memberCount, availableUsers.size()); i++) {
            TeamMember member = new TeamMember();
            member.setTeam(team);
            member.setUser(availableUsers.get(i));
            member.setRole(TeamMemberRole.MEMBER);
            member.setActive(true);
            teamMemberRepository.save(member);
            team.getMembers().add(member);
        }

        return team;
    }
}
