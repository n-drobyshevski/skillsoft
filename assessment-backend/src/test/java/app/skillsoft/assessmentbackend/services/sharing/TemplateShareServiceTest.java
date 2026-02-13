package app.skillsoft.assessmentbackend.services.sharing;

import app.skillsoft.assessmentbackend.domain.dto.sharing.BulkShareRequest;
import app.skillsoft.assessmentbackend.domain.dto.sharing.BulkShareResponse;
import app.skillsoft.assessmentbackend.domain.dto.sharing.ShareTeamRequest;
import app.skillsoft.assessmentbackend.domain.dto.sharing.ShareUserRequest;
import app.skillsoft.assessmentbackend.domain.dto.sharing.SharedTemplatesResponseDto;
import app.skillsoft.assessmentbackend.domain.dto.sharing.TemplateShareDto;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;
import app.skillsoft.assessmentbackend.domain.entities.GranteeType;
import app.skillsoft.assessmentbackend.domain.entities.SharePermission;
import app.skillsoft.assessmentbackend.domain.entities.Team;
import app.skillsoft.assessmentbackend.domain.entities.TemplateShare;
import app.skillsoft.assessmentbackend.domain.entities.TemplateStatus;
import app.skillsoft.assessmentbackend.domain.entities.TestTemplate;
import app.skillsoft.assessmentbackend.domain.entities.User;
import app.skillsoft.assessmentbackend.domain.entities.UserRole;
import app.skillsoft.assessmentbackend.exception.ResourceNotFoundException;
import app.skillsoft.assessmentbackend.repository.TeamMemberRepository;
import app.skillsoft.assessmentbackend.repository.TeamRepository;
import app.skillsoft.assessmentbackend.repository.TemplateShareRepository;
import app.skillsoft.assessmentbackend.repository.TestTemplateRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TemplateShareServiceImpl.
 *
 * Tests cover all sharing operations including:
 * - User and team share creation
 * - Permission validation and escalation prevention
 * - Share updates and revocation
 * - Access checks (direct and via team membership)
 * - Bulk sharing operations
 * - "Shared with me" queries
 *
 * Uses bilingual test data to verify proper handling of
 * English/Russian content throughout the system.
 */
@DisplayName("TemplateShareService Tests")
class TemplateShareServiceTest extends BaseUnitTest {

    @Mock
    private TemplateShareRepository shareRepository;

    @Mock
    private TestTemplateRepository templateRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @InjectMocks
    private TemplateShareServiceImpl shareService;

    private User owner;
    private User grantor;
    private User grantee;
    private TestTemplate template;
    private Team team;

    @BeforeEach
    void setUp() {
        // Create test users with bilingual names
        owner = TestDataFactory.createUser(UserRole.ADMIN);
        owner.setId(UUID.randomUUID());
        owner.setClerkId("clerk_owner_001");

        grantor = TestDataFactory.createUser(UserRole.EDITOR);
        grantor.setId(UUID.randomUUID());
        grantor.setClerkId("clerk_grantor_001");

        grantee = TestDataFactory.createUser(UserRole.USER);
        grantee.setId(UUID.randomUUID());
        grantee.setClerkId("clerk_grantee_001");

        // Create test template
        template = TestDataFactory.createTestTemplate(AssessmentGoal.OVERVIEW, owner);
        template.setId(UUID.randomUUID());
        template.setStatus(TemplateStatus.PUBLISHED);

        // Create test team
        team = TestDataFactory.createTeam(owner);
        team.setId(UUID.randomUUID());
    }

    // ============================================
    // SHARE WITH USER TESTS
    // ============================================

    @Nested
    @DisplayName("shareWithUser")
    class ShareWithUserTests {

        @Test
        @DisplayName("should create new share when no existing share exists")
        void shouldCreateNewShareWhenNoExistingShare() {
            // Given
            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
            when(userRepository.findByClerkId(owner.getClerkId())).thenReturn(Optional.of(owner));
            when(userRepository.findById(grantee.getId())).thenReturn(Optional.of(grantee));
            when(shareRepository.findActiveByTemplateAndUser(template.getId(), grantee.getId()))
                    .thenReturn(Optional.empty());
            when(shareRepository.save(any(TemplateShare.class))).thenAnswer(invocation -> {
                TemplateShare share = invocation.getArgument(0);
                share.setId(UUID.randomUUID());
                return share;
            });

            // When
            TemplateShareDto result = shareService.shareWithUser(
                    template.getId(),
                    grantee.getId(),
                    SharePermission.VIEW,
                    null,
                    owner.getClerkId()
            );

            // Then
            assertThat(result).isNotNull();
            assertThat(result.permission()).isEqualTo(SharePermission.VIEW);
            assertThat(result.granteeType()).isEqualTo(GranteeType.USER);

            ArgumentCaptor<TemplateShare> captor = ArgumentCaptor.forClass(TemplateShare.class);
            verify(shareRepository).save(captor.capture());
            TemplateShare savedShare = captor.getValue();
            assertThat(savedShare.getPermission()).isEqualTo(SharePermission.VIEW);
            assertThat(savedShare.getUser()).isEqualTo(grantee);
        }

        @Test
        @DisplayName("should update existing share when duplicate share attempted")
        void shouldUpdateExistingShareWhenDuplicate() {
            // Given
            TemplateShare existingShare = new TemplateShare(template, grantee, SharePermission.VIEW, owner);
            existingShare.setId(UUID.randomUUID());

            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
            when(userRepository.findByClerkId(owner.getClerkId())).thenReturn(Optional.of(owner));
            when(userRepository.findById(grantee.getId())).thenReturn(Optional.of(grantee));
            when(shareRepository.findActiveByTemplateAndUser(template.getId(), grantee.getId()))
                    .thenReturn(Optional.of(existingShare));
            when(shareRepository.save(any(TemplateShare.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            TemplateShareDto result = shareService.shareWithUser(
                    template.getId(),
                    grantee.getId(),
                    SharePermission.EDIT,
                    null,
                    owner.getClerkId()
            );

            // Then
            assertThat(result).isNotNull();
            assertThat(result.permission()).isEqualTo(SharePermission.EDIT);

            verify(shareRepository).save(existingShare);
            assertThat(existingShare.getPermission()).isEqualTo(SharePermission.EDIT);
        }

        @Test
        @DisplayName("should throw exception when sharing DRAFT template")
        void shouldThrowExceptionWhenSharingDraftTemplate() {
            // Given - use non-owner, non-admin grantor to trigger the DRAFT restriction
            // (owners and admins are allowed to share DRAFT templates)
            template.setStatus(TemplateStatus.DRAFT);

            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
            when(userRepository.findByClerkId(grantor.getClerkId())).thenReturn(Optional.of(grantor));
            when(userRepository.findById(grantee.getId())).thenReturn(Optional.of(grantee));

            // When & Then
            assertThatThrownBy(() -> shareService.shareWithUser(
                    template.getId(),
                    grantee.getId(),
                    SharePermission.VIEW,
                    null,
                    grantor.getClerkId()
            ))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DRAFT");
        }

        @Test
        @DisplayName("should throw exception when sharing with self")
        void shouldThrowExceptionWhenSharingWithSelf() {
            // Given
            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
            when(userRepository.findByClerkId(owner.getClerkId())).thenReturn(Optional.of(owner));
            when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

            // When & Then
            assertThatThrownBy(() -> shareService.shareWithUser(
                    template.getId(),
                    owner.getId(),
                    SharePermission.VIEW,
                    null,
                    owner.getClerkId()
            ))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("yourself");
        }

        @Test
        @DisplayName("should throw exception when sharing with template owner")
        void shouldThrowExceptionWhenSharingWithOwner() {
            // Given - grantor is not owner, but trying to share with owner
            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
            when(userRepository.findByClerkId(grantor.getClerkId())).thenReturn(Optional.of(grantor));
            when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

            // When & Then - service should detect sharing with owner before permission checks
            assertThatThrownBy(() -> shareService.shareWithUser(
                    template.getId(),
                    owner.getId(),
                    SharePermission.VIEW,
                    null,
                    grantor.getClerkId()
            ))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("owner");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when template not found")
        void shouldThrowExceptionWhenTemplateNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(templateRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> shareService.shareWithUser(
                    nonExistentId,
                    grantee.getId(),
                    SharePermission.VIEW,
                    null,
                    owner.getClerkId()
            ))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should set expiration date when provided")
        void shouldSetExpirationDateWhenProvided() {
            // Given
            LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
            when(userRepository.findByClerkId(owner.getClerkId())).thenReturn(Optional.of(owner));
            when(userRepository.findById(grantee.getId())).thenReturn(Optional.of(grantee));
            when(shareRepository.findActiveByTemplateAndUser(template.getId(), grantee.getId()))
                    .thenReturn(Optional.empty());
            when(shareRepository.save(any(TemplateShare.class))).thenAnswer(invocation -> {
                TemplateShare share = invocation.getArgument(0);
                share.setId(UUID.randomUUID());
                return share;
            });

            // When
            shareService.shareWithUser(
                    template.getId(),
                    grantee.getId(),
                    SharePermission.VIEW,
                    expiresAt,
                    owner.getClerkId()
            );

            // Then
            ArgumentCaptor<TemplateShare> captor = ArgumentCaptor.forClass(TemplateShare.class);
            verify(shareRepository).save(captor.capture());
            assertThat(captor.getValue().getExpiresAt()).isEqualTo(expiresAt);
        }
    }

    // ============================================
    // SHARE WITH TEAM TESTS
    // ============================================

    @Nested
    @DisplayName("shareWithTeam")
    class ShareWithTeamTests {

        @Test
        @DisplayName("should create new team share successfully")
        void shouldCreateNewTeamShare() {
            // Given
            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
            when(userRepository.findByClerkId(owner.getClerkId())).thenReturn(Optional.of(owner));
            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
            when(shareRepository.findActiveByTemplateAndTeam(template.getId(), team.getId()))
                    .thenReturn(Optional.empty());
            when(shareRepository.save(any(TemplateShare.class))).thenAnswer(invocation -> {
                TemplateShare share = invocation.getArgument(0);
                share.setId(UUID.randomUUID());
                return share;
            });

            // When
            TemplateShareDto result = shareService.shareWithTeam(
                    template.getId(),
                    team.getId(),
                    SharePermission.VIEW,
                    null,
                    owner.getClerkId()
            );

            // Then
            assertThat(result).isNotNull();
            assertThat(result.permission()).isEqualTo(SharePermission.VIEW);
            assertThat(result.granteeType()).isEqualTo(GranteeType.TEAM);

            ArgumentCaptor<TemplateShare> captor = ArgumentCaptor.forClass(TemplateShare.class);
            verify(shareRepository).save(captor.capture());
            assertThat(captor.getValue().getTeam()).isEqualTo(team);
        }

        @Test
        @DisplayName("should throw exception when team not found")
        void shouldThrowExceptionWhenTeamNotFound() {
            // Given
            UUID nonExistentTeamId = UUID.randomUUID();

            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
            when(userRepository.findByClerkId(owner.getClerkId())).thenReturn(Optional.of(owner));
            when(teamRepository.findById(nonExistentTeamId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> shareService.shareWithTeam(
                    template.getId(),
                    nonExistentTeamId,
                    SharePermission.VIEW,
                    null,
                    owner.getClerkId()
            ))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ============================================
    // PERMISSION ESCALATION TESTS
    // ============================================

    @Nested
    @DisplayName("Permission Escalation Prevention")
    class PermissionEscalationTests {

        @Test
        @DisplayName("should throw exception when non-owner tries to share without MANAGE permission")
        void shouldThrowExceptionWhenNoManagePermission() {
            // Given - grantor has only VIEW permission
            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
            when(userRepository.findByClerkId(grantor.getClerkId())).thenReturn(Optional.of(grantor));
            when(userRepository.findById(grantee.getId())).thenReturn(Optional.of(grantee));
            when(shareRepository.findActiveByTemplateAndUser(template.getId(), grantor.getId()))
                    .thenReturn(Optional.of(createShareWithPermission(grantor, SharePermission.VIEW)));
            when(teamMemberRepository.findActiveTeamIdsByUserId(grantor.getId())).thenReturn(List.of());

            // When & Then
            assertThatThrownBy(() -> shareService.shareWithUser(
                    template.getId(),
                    grantee.getId(),
                    SharePermission.VIEW,
                    null,
                    grantor.getClerkId()
            ))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("MANAGE permission");
        }

        @Test
        @DisplayName("should throw exception when user without MANAGE permission tries to share")
        void shouldThrowExceptionWhenGrantingHigherPermission() {
            // Given - grantor has EDIT permission (via team), trying to grant MANAGE
            // The service requires MANAGE permission to share - EDIT is not sufficient
            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
            when(userRepository.findByClerkId(grantor.getClerkId())).thenReturn(Optional.of(grantor));
            when(userRepository.findById(grantee.getId())).thenReturn(Optional.of(grantee));

            // Grantor has no direct share, only team share with EDIT
            when(shareRepository.findActiveByTemplateAndUser(template.getId(), grantor.getId()))
                    .thenReturn(Optional.empty());
            when(shareRepository.findHighestPermissionByTemplateAndTeams(eq(template.getId()), any()))
                    .thenReturn(Optional.of(SharePermission.EDIT));
            when(teamMemberRepository.findActiveTeamIdsByUserId(grantor.getId())).thenReturn(List.of(UUID.randomUUID()));

            // When & Then - EDIT user cannot share at all (needs MANAGE)
            assertThatThrownBy(() -> shareService.shareWithUser(
                    template.getId(),
                    grantee.getId(),
                    SharePermission.MANAGE,
                    null,
                    grantor.getClerkId()
            ))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("MANAGE permission");
        }

        @Test
        @DisplayName("should allow owner to grant any permission")
        void shouldAllowOwnerToGrantAnyPermission() {
            // Given
            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
            when(userRepository.findByClerkId(owner.getClerkId())).thenReturn(Optional.of(owner));
            when(userRepository.findById(grantee.getId())).thenReturn(Optional.of(grantee));
            when(shareRepository.findActiveByTemplateAndUser(template.getId(), grantee.getId()))
                    .thenReturn(Optional.empty());
            when(shareRepository.save(any(TemplateShare.class))).thenAnswer(invocation -> {
                TemplateShare share = invocation.getArgument(0);
                share.setId(UUID.randomUUID());
                return share;
            });

            // When
            TemplateShareDto result = shareService.shareWithUser(
                    template.getId(),
                    grantee.getId(),
                    SharePermission.MANAGE,
                    null,
                    owner.getClerkId()
            );

            // Then
            assertThat(result.permission()).isEqualTo(SharePermission.MANAGE);
        }
    }

    // ============================================
    // UPDATE SHARE TESTS
    // ============================================

    @Nested
    @DisplayName("updateShare")
    class UpdateShareTests {

        @Test
        @DisplayName("should update share permission successfully")
        void shouldUpdateSharePermission() {
            // Given
            TemplateShare existingShare = new TemplateShare(template, grantee, SharePermission.VIEW, owner);
            existingShare.setId(UUID.randomUUID());

            when(shareRepository.findById(existingShare.getId())).thenReturn(Optional.of(existingShare));
            when(shareRepository.save(any(TemplateShare.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            TemplateShareDto result = shareService.updateShare(
                    existingShare.getId(),
                    SharePermission.EDIT,
                    null
            );

            // Then
            assertThat(result.permission()).isEqualTo(SharePermission.EDIT);
            verify(shareRepository).save(existingShare);
        }

        @Test
        @DisplayName("should throw exception when updating revoked share")
        void shouldThrowExceptionWhenUpdatingRevokedShare() {
            // Given
            TemplateShare revokedShare = new TemplateShare(template, grantee, SharePermission.VIEW, owner);
            revokedShare.setId(UUID.randomUUID());
            revokedShare.revoke();

            when(shareRepository.findById(revokedShare.getId())).thenReturn(Optional.of(revokedShare));

            // When & Then
            assertThatThrownBy(() -> shareService.updateShare(
                    revokedShare.getId(),
                    SharePermission.EDIT,
                    null
            ))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("revoked");
        }
    }

    // ============================================
    // REVOKE SHARE TESTS
    // ============================================

    @Nested
    @DisplayName("revokeShare")
    class RevokeShareTests {

        @Test
        @DisplayName("should revoke active share")
        void shouldRevokeActiveShare() {
            // Given
            TemplateShare activeShare = new TemplateShare(template, grantee, SharePermission.VIEW, owner);
            activeShare.setId(UUID.randomUUID());

            when(shareRepository.findById(activeShare.getId())).thenReturn(Optional.of(activeShare));
            when(shareRepository.save(any(TemplateShare.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            shareService.revokeShare(activeShare.getId());

            // Then
            assertThat(activeShare.isActive()).isFalse();
            assertThat(activeShare.getRevokedAt()).isNotNull();
            verify(shareRepository).save(activeShare);
        }

        @Test
        @DisplayName("should do nothing when share already revoked")
        void shouldDoNothingWhenShareAlreadyRevoked() {
            // Given
            TemplateShare revokedShare = new TemplateShare(template, grantee, SharePermission.VIEW, owner);
            revokedShare.setId(UUID.randomUUID());
            revokedShare.revoke();

            when(shareRepository.findById(revokedShare.getId())).thenReturn(Optional.of(revokedShare));

            // When
            shareService.revokeShare(revokedShare.getId());

            // Then - save not called because share was already revoked
            verify(shareRepository, never()).save(any());
        }
    }

    // ============================================
    // LIST SHARES TESTS
    // ============================================

    @Nested
    @DisplayName("listShares")
    class ListSharesTests {

        @Test
        @DisplayName("should return all active shares for template")
        void shouldReturnAllActiveShares() {
            // Given
            TemplateShare userShare = new TemplateShare(template, grantee, SharePermission.VIEW, owner);
            userShare.setId(UUID.randomUUID());

            TemplateShare teamShare = new TemplateShare(template, team, SharePermission.EDIT, owner);
            teamShare.setId(UUID.randomUUID());

            when(templateRepository.existsById(template.getId())).thenReturn(true);
            when(shareRepository.findActiveByTemplateId(template.getId()))
                    .thenReturn(List.of(userShare, teamShare));

            // When
            List<TemplateShareDto> result = shareService.listShares(template.getId());

            // Then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("should throw exception when template not found")
        void shouldThrowExceptionWhenTemplateNotFoundForListShares() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(templateRepository.existsById(nonExistentId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> shareService.listShares(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ============================================
    // BULK SHARE TESTS
    // ============================================

    @Nested
    @DisplayName("bulkShare")
    class BulkShareTests {

        @Test
        @DisplayName("should process bulk share with mixed results")
        void shouldProcessBulkShareWithMixedResults() {
            // Given
            User validUser = TestDataFactory.createUser(UserRole.USER);
            validUser.setId(UUID.randomUUID());

            BulkShareRequest request = new BulkShareRequest(
                    List.of(
                            new ShareUserRequest(validUser.getId(), SharePermission.VIEW, null),
                            new ShareUserRequest(UUID.randomUUID(), SharePermission.VIEW, null) // Non-existent
                    ),
                    List.of(
                            new ShareTeamRequest(team.getId(), SharePermission.VIEW, null)
                    )
            );

            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
            when(userRepository.findByClerkId(owner.getClerkId())).thenReturn(Optional.of(owner));
            when(userRepository.findById(validUser.getId())).thenReturn(Optional.of(validUser));
            when(userRepository.findById(request.userShares().get(1).userId())).thenReturn(Optional.empty());
            when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
            when(shareRepository.findActiveByTemplateAndUser(template.getId(), validUser.getId()))
                    .thenReturn(Optional.empty());
            when(shareRepository.findActiveByTemplateAndTeam(template.getId(), team.getId()))
                    .thenReturn(Optional.empty());
            when(shareRepository.save(any(TemplateShare.class))).thenAnswer(invocation -> {
                TemplateShare share = invocation.getArgument(0);
                share.setId(UUID.randomUUID());
                return share;
            });

            // When
            BulkShareResponse response = shareService.bulkShare(template.getId(), request, owner.getClerkId());

            // Then
            assertThat(response.createdCount()).isEqualTo(2); // 1 user + 1 team
            assertThat(response.failedCount()).isEqualTo(1); // 1 non-existent user
        }

        @Test
        @DisplayName("should throw exception when bulk sharing DRAFT template")
        void shouldThrowExceptionWhenBulkSharingDraftTemplate() {
            // Given
            template.setStatus(TemplateStatus.DRAFT);

            BulkShareRequest request = new BulkShareRequest(
                    List.of(new ShareUserRequest(grantee.getId(), SharePermission.VIEW, null)),
                    List.of()
            );

            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
            when(userRepository.findByClerkId(owner.getClerkId())).thenReturn(Optional.of(owner));

            // When & Then
            assertThatThrownBy(() -> shareService.bulkShare(template.getId(), request, owner.getClerkId()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DRAFT");
        }

        @Test
        @DisplayName("should skip self-share in bulk operation")
        void shouldSkipSelfShareInBulkOperation() {
            // Given
            BulkShareRequest request = new BulkShareRequest(
                    List.of(new ShareUserRequest(owner.getId(), SharePermission.VIEW, null)),
                    List.of()
            );

            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
            when(userRepository.findByClerkId(owner.getClerkId())).thenReturn(Optional.of(owner));
            when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

            // When
            BulkShareResponse response = shareService.bulkShare(template.getId(), request, owner.getClerkId());

            // Then
            assertThat(response.skippedCount()).isEqualTo(1);
            assertThat(response.createdCount()).isZero();
        }
    }

    // ============================================
    // ACCESS CHECK TESTS
    // ============================================

    @Nested
    @DisplayName("hasAccess")
    class HasAccessTests {

        @Test
        @DisplayName("should return true when user has direct share")
        void shouldReturnTrueWhenUserHasDirectShare() {
            // Given
            TemplateShare directShare = new TemplateShare(template, grantee, SharePermission.VIEW, owner);
            directShare.setId(UUID.randomUUID());

            when(shareRepository.findActiveByTemplateAndUser(template.getId(), grantee.getId()))
                    .thenReturn(Optional.of(directShare));

            // When
            boolean result = shareService.hasAccess(template.getId(), grantee.getId());

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return true when user has access via team membership")
        void shouldReturnTrueWhenUserHasTeamAccess() {
            // Given
            UUID teamId = UUID.randomUUID();

            when(shareRepository.findActiveByTemplateAndUser(template.getId(), grantee.getId()))
                    .thenReturn(Optional.empty());
            when(teamMemberRepository.findActiveTeamIdsByUserId(grantee.getId()))
                    .thenReturn(List.of(teamId));
            when(shareRepository.hasTeamPermission(template.getId(), List.of(teamId), SharePermission.VIEW))
                    .thenReturn(true);

            // When
            boolean result = shareService.hasAccess(template.getId(), grantee.getId());

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when user has no access")
        void shouldReturnFalseWhenUserHasNoAccess() {
            // Given
            when(shareRepository.findActiveByTemplateAndUser(template.getId(), grantee.getId()))
                    .thenReturn(Optional.empty());
            when(teamMemberRepository.findActiveTeamIdsByUserId(grantee.getId()))
                    .thenReturn(List.of());

            // When
            boolean result = shareService.hasAccess(template.getId(), grantee.getId());

            // Then
            assertThat(result).isFalse();
        }
    }

    // ============================================
    // GET HIGHEST PERMISSION TESTS
    // ============================================

    @Nested
    @DisplayName("getHighestPermission")
    class GetHighestPermissionTests {

        @Test
        @DisplayName("should return highest permission from direct and team shares")
        void shouldReturnHighestPermission() {
            // Given
            UUID teamId = UUID.randomUUID();

            TemplateShare directShare = new TemplateShare(template, grantee, SharePermission.VIEW, owner);
            directShare.setId(UUID.randomUUID());

            when(shareRepository.findActiveByTemplateAndUser(template.getId(), grantee.getId()))
                    .thenReturn(Optional.of(directShare));
            when(teamMemberRepository.findActiveTeamIdsByUserId(grantee.getId()))
                    .thenReturn(List.of(teamId));
            when(shareRepository.findHighestPermissionByTemplateAndTeams(template.getId(), List.of(teamId)))
                    .thenReturn(Optional.of(SharePermission.EDIT));

            // When
            SharePermission result = shareService.getHighestPermission(template.getId(), grantee.getId());

            // Then - EDIT (2) > VIEW (1)
            assertThat(result).isEqualTo(SharePermission.EDIT);
        }

        @Test
        @DisplayName("should return null when user has no permissions")
        void shouldReturnNullWhenNoPermissions() {
            // Given
            when(shareRepository.findActiveByTemplateAndUser(template.getId(), grantee.getId()))
                    .thenReturn(Optional.empty());
            when(teamMemberRepository.findActiveTeamIdsByUserId(grantee.getId()))
                    .thenReturn(List.of());

            // When
            SharePermission result = shareService.getHighestPermission(template.getId(), grantee.getId());

            // Then
            assertThat(result).isNull();
        }
    }

    // ============================================
    // CAN GRANT PERMISSION TESTS
    // ============================================

    @Nested
    @DisplayName("canGrantPermission")
    class CanGrantPermissionTests {

        @Test
        @DisplayName("should return true for owner granting any permission")
        void shouldReturnTrueForOwner() {
            // Given
            when(userRepository.findByClerkId(owner.getClerkId())).thenReturn(Optional.of(owner));
            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));

            // When
            boolean result = shareService.canGrantPermission(template.getId(), owner.getClerkId(), SharePermission.MANAGE);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false for user without access")
        void shouldReturnFalseForUserWithoutAccess() {
            // Given
            when(userRepository.findByClerkId(grantee.getClerkId())).thenReturn(Optional.of(grantee));
            when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
            when(shareRepository.findActiveByTemplateAndUser(template.getId(), grantee.getId()))
                    .thenReturn(Optional.empty());
            when(teamMemberRepository.findActiveTeamIdsByUserId(grantee.getId()))
                    .thenReturn(List.of());

            // When
            boolean result = shareService.canGrantPermission(template.getId(), grantee.getClerkId(), SharePermission.VIEW);

            // Then
            assertThat(result).isFalse();
        }
    }

    // ============================================
    // COUNT AND REVOKE ALL TESTS
    // ============================================

    @Nested
    @DisplayName("countShares and revokeAllShares")
    class CountAndRevokeAllTests {

        @Test
        @DisplayName("should count active shares")
        void shouldCountActiveShares() {
            // Given
            when(shareRepository.countActiveByTemplateId(template.getId())).thenReturn(5L);

            // When
            long result = shareService.countShares(template.getId());

            // Then
            assertThat(result).isEqualTo(5L);
        }

        @Test
        @DisplayName("should revoke all shares for template")
        void shouldRevokeAllShares() {
            // Given
            TemplateShare share1 = new TemplateShare(template, grantee, SharePermission.VIEW, owner);
            share1.setId(UUID.randomUUID());

            User anotherUser = TestDataFactory.createUser(UserRole.USER);
            anotherUser.setId(UUID.randomUUID());
            TemplateShare share2 = new TemplateShare(template, anotherUser, SharePermission.EDIT, owner);
            share2.setId(UUID.randomUUID());

            when(shareRepository.findActiveByTemplateId(template.getId()))
                    .thenReturn(List.of(share1, share2));
            when(shareRepository.save(any(TemplateShare.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            int result = shareService.revokeAllShares(template.getId());

            // Then
            assertThat(result).isEqualTo(2);
            assertThat(share1.isActive()).isFalse();
            assertThat(share2.isActive()).isFalse();
        }
    }

    // ============================================
    // SHARED WITH ME TESTS
    // ============================================

    @Nested
    @DisplayName("getTemplatesSharedWithMe")
    class SharedWithMeTests {

        @Test
        @DisplayName("should return templates shared with user")
        void shouldReturnTemplatesSharedWithUser() {
            // Given
            TemplateShare directShare = new TemplateShare(template, grantee, SharePermission.VIEW, owner);
            directShare.setId(UUID.randomUUID());

            when(userRepository.findByClerkId(grantee.getClerkId())).thenReturn(Optional.of(grantee));
            when(teamMemberRepository.findActiveTeamIdsByUserId(grantee.getId())).thenReturn(List.of());
            when(shareRepository.findActiveSharesForUserWithDetails(grantee.getId()))
                    .thenReturn(List.of(directShare));

            // When
            SharedTemplatesResponseDto result = shareService.getTemplatesSharedWithMe(grantee.getClerkId());

            // Then
            assertThat(result.items()).hasSize(1);
            assertThat(result.total()).isEqualTo(1);
        }

        @Test
        @DisplayName("should return empty result when user not found")
        void shouldReturnEmptyWhenUserNotFound() {
            // Given
            when(userRepository.findByClerkId("unknown_clerk_id")).thenReturn(Optional.empty());

            // When
            SharedTemplatesResponseDto result = shareService.getTemplatesSharedWithMe("unknown_clerk_id");

            // Then
            assertThat(result.items()).isEmpty();
            assertThat(result.total()).isZero();
        }

        @Test
        @DisplayName("should merge direct and team shares keeping highest permission")
        void shouldMergeSharesKeepingHighestPermission() {
            // Given
            UUID teamId = UUID.randomUUID();

            TemplateShare directShare = new TemplateShare(template, grantee, SharePermission.VIEW, owner);
            directShare.setId(UUID.randomUUID());

            TemplateShare teamShare = new TemplateShare(template, team, SharePermission.EDIT, owner);
            teamShare.setId(UUID.randomUUID());

            when(userRepository.findByClerkId(grantee.getClerkId())).thenReturn(Optional.of(grantee));
            when(teamMemberRepository.findActiveTeamIdsByUserId(grantee.getId())).thenReturn(List.of(teamId));
            when(shareRepository.findActiveSharesForUserWithDetails(grantee.getId()))
                    .thenReturn(List.of(directShare));
            when(shareRepository.findActiveTeamSharesForUserWithDetails(List.of(teamId), grantee.getId()))
                    .thenReturn(List.of(teamShare));

            // When
            SharedTemplatesResponseDto result = shareService.getTemplatesSharedWithMe(grantee.getClerkId());

            // Then
            assertThat(result.items()).hasSize(1);
            // Should keep higher permission (EDIT from team share)
            assertThat(result.items().get(0).permission()).isEqualTo(SharePermission.EDIT);
        }
    }

    @Nested
    @DisplayName("countTemplatesSharedWithMe")
    class CountSharedWithMeTests {

        @Test
        @DisplayName("should count templates shared with user")
        void shouldCountTemplatesSharedWithUser() {
            // Given
            UUID teamId = UUID.randomUUID();

            when(userRepository.findByClerkId(grantee.getClerkId())).thenReturn(Optional.of(grantee));
            when(teamMemberRepository.findActiveTeamIdsByUserId(grantee.getId())).thenReturn(List.of(teamId));
            when(shareRepository.countTemplatesSharedWithUser(grantee.getId(), List.of(teamId))).thenReturn(5L);

            // When
            long result = shareService.countTemplatesSharedWithMe(grantee.getClerkId());

            // Then
            assertThat(result).isEqualTo(5L);
        }

        @Test
        @DisplayName("should return zero when user not found")
        void shouldReturnZeroWhenUserNotFound() {
            // Given
            when(userRepository.findByClerkId("unknown_clerk_id")).thenReturn(Optional.empty());

            // When
            long result = shareService.countTemplatesSharedWithMe("unknown_clerk_id");

            // Then
            assertThat(result).isZero();
        }
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    private TemplateShare createShareWithPermission(User user, SharePermission permission) {
        TemplateShare share = new TemplateShare(template, user, permission, owner);
        share.setId(UUID.randomUUID());
        return share;
    }
}
