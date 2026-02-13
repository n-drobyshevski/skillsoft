package app.skillsoft.assessmentbackend.services.security;

import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;
import app.skillsoft.assessmentbackend.domain.entities.SharePermission;
import app.skillsoft.assessmentbackend.domain.entities.TemplateShare;
import app.skillsoft.assessmentbackend.domain.entities.TemplateShareLink;
import app.skillsoft.assessmentbackend.domain.entities.TemplateStatus;
import app.skillsoft.assessmentbackend.domain.entities.TemplateVisibility;
import app.skillsoft.assessmentbackend.domain.entities.TestTemplate;
import app.skillsoft.assessmentbackend.domain.entities.User;
import app.skillsoft.assessmentbackend.domain.entities.UserRole;
import app.skillsoft.assessmentbackend.repository.TeamMemberRepository;
import app.skillsoft.assessmentbackend.repository.TemplateShareLinkRepository;
import app.skillsoft.assessmentbackend.repository.TemplateShareRepository;
import app.skillsoft.assessmentbackend.repository.TestTemplateRepository;
import app.skillsoft.assessmentbackend.repository.UserRepository;
import app.skillsoft.assessmentbackend.testutils.BaseUnitTest;
import app.skillsoft.assessmentbackend.testutils.TestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TemplateSecurityServiceImpl.
 *
 * Tests cover access control hierarchy:
 * 1. Share link token (anonymous)
 * 2. ADMIN bypass
 * 3. Template owner
 * 4. Direct user share
 * 5. Team membership share
 * 6. Public visibility
 *
 * Uses mock SecurityContext to simulate different user roles.
 */
@DisplayName("TemplateSecurityService Tests")
class TemplateSecurityServiceTest extends BaseUnitTest {

    @Mock
    private TestTemplateRepository templateRepository;

    @Mock
    private TemplateShareRepository shareRepository;

    @Mock
    private TemplateShareLinkRepository linkRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TemplateSecurityServiceImpl securityService;

    private User owner;
    private User regularUser;
    private TestTemplate template;

    @BeforeEach
    void setUp() {
        // Create test owner
        owner = TestDataFactory.createUser(UserRole.ADMIN);
        owner.setId(UUID.randomUUID());
        owner.setClerkId("clerk_owner_123");

        // Create regular user
        regularUser = TestDataFactory.createUser(UserRole.USER);
        regularUser.setId(UUID.randomUUID());
        regularUser.setClerkId("clerk_user_456");

        // Create test template
        template = TestDataFactory.createTestTemplate(AssessmentGoal.OVERVIEW, owner);
        template.setId(UUID.randomUUID());
        template.setStatus(TemplateStatus.PUBLISHED);
        template.setVisibility(TemplateVisibility.PRIVATE);
    }

    @AfterEach
    void tearDown() {
        // Clean up security context
        SecurityContextHolder.clearContext();
    }

    // ============================================
    // HELPER METHODS FOR SECURITY CONTEXT
    // ============================================

    private void setAuthenticatedUser(String clerkId, String... roles) {
        List<SimpleGrantedAuthority> authorities = java.util.Arrays.stream(roles)
                .map(SimpleGrantedAuthority::new)
                .toList();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                clerkId, null, authorities);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    private void setAnonymousUser() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "anonymousUser", null, List.of());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    private void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    // ============================================
    // ADMIN BYPASS TESTS
    // ============================================

    @Nested
    @DisplayName("Admin Bypass")
    class AdminBypassTests {

        @Test
        @DisplayName("should grant admin access to any template")
        void shouldGrantAdminAccessToAnyTemplate() {
            // Given
            setAuthenticatedUser("admin_clerk_id", "ROLE_ADMIN");
            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));

            // When
            boolean result = securityService.canAccess(template.getId(), SharePermission.MANAGE);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should allow admin to edit any template")
        void shouldAllowAdminToEdit() {
            // Given
            setAuthenticatedUser("admin_clerk_id", "ROLE_ADMIN");
            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));

            // When
            boolean result = securityService.canEdit(template.getId());

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should allow admin to delete any template")
        void shouldAllowAdminToDelete() {
            // Given
            setAuthenticatedUser("admin_clerk_id", "ROLE_ADMIN");
            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));

            // When
            boolean result = securityService.canDelete(template.getId());

            // Then
            assertThat(result).isTrue();
        }
    }

    // ============================================
    // OWNER TESTS
    // ============================================

    @Nested
    @DisplayName("Owner Access")
    class OwnerAccessTests {

        @Test
        @DisplayName("should grant owner full access")
        void shouldGrantOwnerFullAccess() {
            // Given
            setAuthenticatedUser(owner.getClerkId(), "ROLE_USER");
            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));

            // When
            boolean result = securityService.canAccess(template.getId(), SharePermission.MANAGE);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should identify owner correctly")
        void shouldIdentifyOwnerCorrectly() {
            // Given
            setAuthenticatedUser(owner.getClerkId(), "ROLE_USER");
            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));

            // When
            boolean result = securityService.isOwner(template.getId());

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should not identify non-owner as owner")
        void shouldNotIdentifyNonOwnerAsOwner() {
            // Given
            setAuthenticatedUser(regularUser.getClerkId(), "ROLE_USER");
            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));

            // When
            boolean result = securityService.isOwner(template.getId());

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should allow owner to delete")
        void shouldAllowOwnerToDelete() {
            // Given
            setAuthenticatedUser(owner.getClerkId(), "ROLE_USER");
            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));

            // When
            boolean result = securityService.canDelete(template.getId());

            // Then
            assertThat(result).isTrue();
        }
    }

    // ============================================
    // DIRECT USER SHARE TESTS
    // ============================================

    @Nested
    @DisplayName("Direct User Share")
    class DirectUserShareTests {

        @Test
        @DisplayName("should grant access when user has direct share with sufficient permission")
        void shouldGrantAccessWithSufficientPermission() {
            // Given
            setAuthenticatedUser(regularUser.getClerkId(), "ROLE_USER");

            TemplateShare share = new TemplateShare(template, regularUser, SharePermission.EDIT, owner);
            share.setId(UUID.randomUUID());

            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
            when(userRepository.findByClerkId(regularUser.getClerkId())).thenReturn(Optional.of(regularUser));
            when(shareRepository.findActiveByTemplateAndUser(template.getId(), regularUser.getId()))
                    .thenReturn(Optional.of(share));

            // When
            boolean result = securityService.canAccess(template.getId(), SharePermission.VIEW);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should deny access when user has insufficient permission")
        void shouldDenyAccessWithInsufficientPermission() {
            // Given
            setAuthenticatedUser(regularUser.getClerkId(), "ROLE_USER");

            TemplateShare share = new TemplateShare(template, regularUser, SharePermission.VIEW, owner);
            share.setId(UUID.randomUUID());

            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
            when(userRepository.findByClerkId(regularUser.getClerkId())).thenReturn(Optional.of(regularUser));
            when(shareRepository.findActiveByTemplateAndUser(template.getId(), regularUser.getId()))
                    .thenReturn(Optional.of(share));
            when(teamMemberRepository.findActiveTeamIdsByUserId(regularUser.getId())).thenReturn(List.of());

            // When
            boolean result = securityService.canAccess(template.getId(), SharePermission.MANAGE);

            // Then
            assertThat(result).isFalse();
        }
    }

    // ============================================
    // TEAM SHARE TESTS
    // ============================================

    @Nested
    @DisplayName("Team Share Access")
    class TeamShareTests {

        @Test
        @DisplayName("should grant access via team membership")
        void shouldGrantAccessViaTeamMembership() {
            // Given
            UUID teamId = UUID.randomUUID();
            setAuthenticatedUser(regularUser.getClerkId(), "ROLE_USER");

            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
            when(userRepository.findByClerkId(regularUser.getClerkId())).thenReturn(Optional.of(regularUser));
            when(shareRepository.findActiveByTemplateAndUser(template.getId(), regularUser.getId()))
                    .thenReturn(Optional.empty());
            when(teamMemberRepository.findActiveTeamIdsByUserId(regularUser.getId()))
                    .thenReturn(List.of(teamId));
            when(shareRepository.findHighestPermissionByTemplateAndTeams(template.getId(), List.of(teamId)))
                    .thenReturn(Optional.of(SharePermission.EDIT));

            // When
            boolean result = securityService.canAccess(template.getId(), SharePermission.VIEW);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should deny access when team share has insufficient permission")
        void shouldDenyAccessWhenTeamShareInsufficient() {
            // Given
            UUID teamId = UUID.randomUUID();
            setAuthenticatedUser(regularUser.getClerkId(), "ROLE_USER");

            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
            when(userRepository.findByClerkId(regularUser.getClerkId())).thenReturn(Optional.of(regularUser));
            when(shareRepository.findActiveByTemplateAndUser(template.getId(), regularUser.getId()))
                    .thenReturn(Optional.empty());
            when(teamMemberRepository.findActiveTeamIdsByUserId(regularUser.getId()))
                    .thenReturn(List.of(teamId));
            when(shareRepository.findHighestPermissionByTemplateAndTeams(template.getId(), List.of(teamId)))
                    .thenReturn(Optional.of(SharePermission.VIEW));

            // When
            boolean result = securityService.canAccess(template.getId(), SharePermission.MANAGE);

            // Then
            assertThat(result).isFalse();
        }
    }

    // ============================================
    // SHARE LINK TOKEN TESTS
    // ============================================

    @Nested
    @DisplayName("Share Link Token Access")
    class ShareLinkTests {

        @Test
        @DisplayName("should grant anonymous access with valid share link")
        void shouldGrantAnonymousAccessWithValidLink() {
            // Given
            setAnonymousUser();
            String token = "valid_token_123";

            template.setVisibility(TemplateVisibility.LINK);

            TemplateShareLink link = createMockShareLink(token, SharePermission.VIEW);

            // Template lookup may or may not happen depending on service implementation
            lenient().when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
            when(linkRepository.findValidByToken(token)).thenReturn(Optional.of(link));
            when(linkRepository.save(any(TemplateShareLink.class))).thenReturn(link);

            // When
            boolean result = securityService.canAccess(template.getId(), SharePermission.VIEW, token);

            // Then
            assertThat(result).isTrue();
            verify(linkRepository).save(link); // Verify usage was recorded
        }

        @Test
        @DisplayName("should deny access with invalid share link token")
        void shouldDenyAccessWithInvalidToken() {
            // Given
            setAnonymousUser();
            String invalidToken = "invalid_token";

            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
            when(linkRepository.findValidByToken(invalidToken)).thenReturn(Optional.empty());

            // When
            boolean result = securityService.canAccess(template.getId(), SharePermission.VIEW, invalidToken);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should deny access when template visibility is not LINK")
        void shouldDenyAccessWhenVisibilityNotLink() {
            // Given
            setAnonymousUser();
            String token = "valid_token_123";

            template.setVisibility(TemplateVisibility.PRIVATE);

            TemplateShareLink link = createMockShareLink(token, SharePermission.VIEW);
            link.getTemplate().setVisibility(TemplateVisibility.PRIVATE);

            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
            when(linkRepository.findValidByToken(token)).thenReturn(Optional.of(link));

            // When
            boolean result = securityService.canAccess(template.getId(), SharePermission.VIEW, token);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should validate share token correctly")
        void shouldValidateShareToken() {
            // Given
            String token = "valid_token_123";
            template.setVisibility(TemplateVisibility.LINK);

            TemplateShareLink link = createMockShareLink(token, SharePermission.VIEW);

            when(linkRepository.findValidByToken(token)).thenReturn(Optional.of(link));

            // When
            boolean result = securityService.isValidShareToken(template.getId(), token);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false for null token")
        void shouldReturnFalseForNullToken() {
            // When
            boolean result = securityService.isValidShareToken(template.getId(), null);

            // Then
            assertThat(result).isFalse();
        }

        private TemplateShareLink createMockShareLink(String token, SharePermission permission) {
            TemplateShareLink link = mock(TemplateShareLink.class);
            // Use lenient stubbing since not all tests use all these methods
            lenient().when(link.getToken()).thenReturn(token);
            lenient().when(link.getPermission()).thenReturn(permission);
            lenient().when(link.getTemplate()).thenReturn(template);
            lenient().when(link.hasPermission(any())).thenAnswer(invocation -> {
                SharePermission required = invocation.getArgument(0);
                return permission.includes(required);
            });
            return link;
        }
    }

    // ============================================
    // PUBLIC VISIBILITY TESTS
    // ============================================

    @Nested
    @DisplayName("Public Visibility")
    class PublicVisibilityTests {

        @Test
        @DisplayName("should grant VIEW access to public templates")
        void shouldGrantViewAccessToPublicTemplates() {
            // Given
            setAuthenticatedUser(regularUser.getClerkId(), "ROLE_USER");
            template.setVisibility(TemplateVisibility.PUBLIC);

            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
            when(userRepository.findByClerkId(regularUser.getClerkId())).thenReturn(Optional.of(regularUser));
            when(shareRepository.findActiveByTemplateAndUser(template.getId(), regularUser.getId()))
                    .thenReturn(Optional.empty());
            when(teamMemberRepository.findActiveTeamIdsByUserId(regularUser.getId())).thenReturn(List.of());

            // When
            boolean result = securityService.canView(template.getId());

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should deny EDIT access to public templates without share")
        void shouldDenyEditAccessToPublicTemplates() {
            // Given
            setAuthenticatedUser(regularUser.getClerkId(), "ROLE_USER");
            template.setVisibility(TemplateVisibility.PUBLIC);

            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
            when(userRepository.findByClerkId(regularUser.getClerkId())).thenReturn(Optional.of(regularUser));
            when(shareRepository.findActiveByTemplateAndUser(template.getId(), regularUser.getId()))
                    .thenReturn(Optional.empty());
            when(teamMemberRepository.findActiveTeamIdsByUserId(regularUser.getId())).thenReturn(List.of());

            // When
            boolean result = securityService.canEdit(template.getId());

            // Then
            assertThat(result).isFalse();
        }
    }

    // ============================================
    // ARCHIVED TEMPLATE TESTS
    // ============================================

    @Nested
    @DisplayName("Archived Templates")
    class ArchivedTemplateTests {

        @Test
        @DisplayName("should allow VIEW on archived templates")
        void shouldAllowViewOnArchivedTemplates() {
            // Given
            setAuthenticatedUser(owner.getClerkId(), "ROLE_USER");
            template.setStatus(TemplateStatus.ARCHIVED);

            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));

            // When
            boolean result = securityService.canAccess(template.getId(), SharePermission.VIEW);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should deny EDIT on archived templates")
        void shouldDenyEditOnArchivedTemplates() {
            // Given
            setAuthenticatedUser(owner.getClerkId(), "ROLE_USER");
            template.setStatus(TemplateStatus.ARCHIVED);

            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));

            // When
            boolean result = securityService.canAccess(template.getId(), SharePermission.EDIT);

            // Then
            assertThat(result).isFalse();
        }
    }

    // ============================================
    // UNAUTHENTICATED USER TESTS
    // ============================================

    @Nested
    @DisplayName("Unauthenticated Access")
    class UnauthenticatedTests {

        @Test
        @DisplayName("should deny access to unauthenticated user without token")
        void shouldDenyAccessToUnauthenticatedUser() {
            // Given
            clearAuthentication();

            // When
            boolean result = securityService.canAccess(template.getId(), SharePermission.VIEW);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false for isAuthenticated when no auth")
        void shouldReturnFalseForIsAuthenticated() {
            // Given
            clearAuthentication();

            // When
            boolean result = securityService.isAuthenticated();

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return null for getAuthenticatedClerkId when no auth")
        void shouldReturnNullForClerkIdWhenNoAuth() {
            // Given
            clearAuthentication();

            // When
            String result = securityService.getAuthenticatedClerkId();

            // Then
            assertThat(result).isNull();
        }
    }

    // ============================================
    // EDGE CASE TESTS
    // ============================================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("should return false for null templateId")
        void shouldReturnFalseForNullTemplateId() {
            // Given
            setAuthenticatedUser(owner.getClerkId(), "ROLE_ADMIN");

            // When
            boolean result = securityService.canAccess(null, SharePermission.VIEW);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false for non-existent template")
        void shouldReturnFalseForNonExistentTemplate() {
            // Given
            setAuthenticatedUser(owner.getClerkId(), "ROLE_USER");
            UUID nonExistentId = UUID.randomUUID();
            when(templateRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When
            boolean result = securityService.canAccess(nonExistentId, SharePermission.VIEW);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when user not found in database")
        void shouldReturnFalseWhenUserNotFound() {
            // Given
            setAuthenticatedUser("unknown_clerk_id", "ROLE_USER");

            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
            when(userRepository.findByClerkId("unknown_clerk_id")).thenReturn(Optional.empty());

            // When
            boolean result = securityService.canAccess(template.getId(), SharePermission.VIEW);

            // Then
            assertThat(result).isFalse();
        }
    }

    // ============================================
    // ROLE CHECK TESTS
    // ============================================

    @Nested
    @DisplayName("Role Checks")
    class RoleCheckTests {

        @Test
        @DisplayName("should identify admin role")
        void shouldIdentifyAdminRole() {
            // Given
            setAuthenticatedUser("admin_id", "ROLE_ADMIN");

            // When
            boolean result = securityService.isAdmin();

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false for isAdmin when user is not admin")
        void shouldReturnFalseForNonAdmin() {
            // Given
            setAuthenticatedUser("user_id", "ROLE_USER");

            // When
            boolean result = securityService.isAdmin();

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should identify editor role")
        void shouldIdentifyEditorRole() {
            // Given
            setAuthenticatedUser("editor_id", "ROLE_EDITOR");

            // When
            boolean result = securityService.isEditor();

            // Then
            assertThat(result).isTrue();
        }
    }

    // ============================================
    // CONVENIENCE METHOD TESTS
    // ============================================

    @Nested
    @DisplayName("Convenience Methods")
    class ConvenienceMethodTests {

        @Test
        @DisplayName("canManageSharing should return true for owner")
        void canManageSharingShouldReturnTrueForOwner() {
            // Given
            setAuthenticatedUser(owner.getClerkId(), "ROLE_USER");
            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));

            // When
            boolean result = securityService.canManageSharing(template.getId());

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("canChangeVisibility should return true for owner")
        void canChangeVisibilityShouldReturnTrueForOwner() {
            // Given
            setAuthenticatedUser(owner.getClerkId(), "ROLE_USER");
            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));

            // When
            boolean result = securityService.canChangeVisibility(template.getId());

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("canCreateLinks should return true for user with MANAGE permission")
        void canCreateLinksShouldReturnTrueForManagePermission() {
            // Given
            setAuthenticatedUser(owner.getClerkId(), "ROLE_USER");
            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));

            // When
            boolean result = securityService.canCreateLinks(template.getId());

            // Then
            assertThat(result).isTrue();
        }
    }
}
