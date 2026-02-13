package app.skillsoft.assessmentbackend.services.sharing;

import app.skillsoft.assessmentbackend.domain.dto.sharing.*;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.exception.ResourceNotFoundException;
import app.skillsoft.assessmentbackend.repository.TeamMemberRepository;
import app.skillsoft.assessmentbackend.repository.TeamRepository;
import app.skillsoft.assessmentbackend.repository.TemplateShareRepository;
import app.skillsoft.assessmentbackend.repository.TestTemplateRepository;
import app.skillsoft.assessmentbackend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of TemplateShareService.
 *
 * Handles all template sharing operations with comprehensive validation:
 * - Self-share prevention (cannot share with yourself)
 * - Permission escalation prevention (cannot grant higher than your own)
 * - DRAFT status restriction (only owner can access DRAFT templates)
 * - Duplicate handling (update existing shares instead of creating duplicates)
 * - User/team existence validation
 */
@Service
@Transactional
public class TemplateShareServiceImpl implements TemplateShareService {

    private static final Logger log = LoggerFactory.getLogger(TemplateShareServiceImpl.class);

    private final TemplateShareRepository shareRepository;
    private final TestTemplateRepository templateRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;

    public TemplateShareServiceImpl(
            TemplateShareRepository shareRepository,
            TestTemplateRepository templateRepository,
            UserRepository userRepository,
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository) {
        this.shareRepository = shareRepository;
        this.templateRepository = templateRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
    }

    @Override
    public TemplateShareDto shareWithUser(UUID templateId, UUID userId, SharePermission permission,
                                          LocalDateTime expiresAt, String grantedByClerkId) {
        log.info("Sharing template {} with user {} (permission: {})", templateId, userId, permission);

        // Validate template
        TestTemplate template = findTemplateOrThrow(templateId);

        // Validate grantor
        User grantor = findUserByClerkIdOrThrow(grantedByClerkId);

        // Validate grantee
        User grantee = findUserOrThrow(userId);

        // Validation checks
        validateShareRequest(template, grantor, grantee, permission);

        // Check for existing share
        Optional<TemplateShare> existingShare = shareRepository.findActiveByTemplateAndUser(templateId, userId);

        TemplateShare share;
        boolean isUpdate = false;

        if (existingShare.isPresent()) {
            // Update existing share
            share = existingShare.get();
            share.setPermission(permission);
            share.setExpiresAt(expiresAt);
            isUpdate = true;
            log.info("Updating existing share {} for user {}", share.getId(), userId);
        } else {
            // Create new share
            share = new TemplateShare(template, grantee, permission, grantor);
            share.setExpiresAt(expiresAt);
            log.info("Creating new share for user {}", userId);
        }

        share = shareRepository.save(share);

        if (isUpdate) {
            log.info("Updated share {} for template {} and user {}", share.getId(), templateId, userId);
        } else {
            log.info("Created share {} for template {} and user {}", share.getId(), templateId, userId);
        }

        return TemplateShareDto.fromEntity(share);
    }

    @Override
    public TemplateShareDto shareWithTeam(UUID templateId, UUID teamId, SharePermission permission,
                                          LocalDateTime expiresAt, String grantedByClerkId) {
        log.info("Sharing template {} with team {} (permission: {})", templateId, teamId, permission);

        // Validate template
        TestTemplate template = findTemplateOrThrow(templateId);

        // Validate grantor
        User grantor = findUserByClerkIdOrThrow(grantedByClerkId);

        // Validate team
        Team team = findTeamOrThrow(teamId);

        // Validation checks
        validateTeamShareRequest(template, grantor, permission);

        // Check for existing share
        Optional<TemplateShare> existingShare = shareRepository.findActiveByTemplateAndTeam(templateId, teamId);

        TemplateShare share;
        boolean isUpdate = false;

        if (existingShare.isPresent()) {
            // Update existing share
            share = existingShare.get();
            share.setPermission(permission);
            share.setExpiresAt(expiresAt);
            isUpdate = true;
            log.info("Updating existing share {} for team {}", share.getId(), teamId);
        } else {
            // Create new share
            share = new TemplateShare(template, team, permission, grantor);
            share.setExpiresAt(expiresAt);
            log.info("Creating new share for team {}", teamId);
        }

        share = shareRepository.save(share);

        if (isUpdate) {
            log.info("Updated share {} for template {} and team {}", share.getId(), templateId, teamId);
        } else {
            log.info("Created share {} for template {} and team {}", share.getId(), templateId, teamId);
        }

        return TemplateShareDto.fromEntity(share);
    }

    @Override
    public TemplateShareDto updateShare(UUID shareId, SharePermission permission, LocalDateTime expiresAt) {
        log.info("Updating share {} (permission: {}, expiresAt: {})", shareId, permission, expiresAt);

        TemplateShare share = findShareOrThrow(shareId);

        if (!share.isActive()) {
            throw new IllegalStateException("Cannot update a revoked share");
        }

        share.setPermission(permission);
        share.setExpiresAt(expiresAt);

        share = shareRepository.save(share);
        log.info("Updated share {}", shareId);

        return TemplateShareDto.fromEntity(share);
    }

    @Override
    public void revokeShare(UUID shareId) {
        log.info("Revoking share {}", shareId);

        TemplateShare share = findShareOrThrow(shareId);

        if (!share.isActive()) {
            log.warn("Share {} is already revoked", shareId);
            return;
        }

        share.revoke();
        shareRepository.save(share);

        log.info("Revoked share {}", shareId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateShareDto> listShares(UUID templateId) {
        log.debug("Listing all shares for template {}", templateId);

        validateTemplateExists(templateId);

        List<TemplateShare> shares = shareRepository.findActiveByTemplateId(templateId);

        return shares.stream()
                .map(TemplateShareDto::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateShareDto> listUserShares(UUID templateId) {
        log.debug("Listing user shares for template {}", templateId);

        validateTemplateExists(templateId);

        List<TemplateShare> shares = shareRepository.findActiveByTemplateIdAndGranteeType(
                templateId, GranteeType.USER);

        return shares.stream()
                .map(TemplateShareDto::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateShareDto> listTeamShares(UUID templateId) {
        log.debug("Listing team shares for template {}", templateId);

        validateTemplateExists(templateId);

        List<TemplateShare> shares = shareRepository.findActiveByTemplateIdAndGranteeType(
                templateId, GranteeType.TEAM);

        return shares.stream()
                .map(TemplateShareDto::fromEntity)
                .toList();
    }

    @Override
    public BulkShareResponse bulkShare(UUID templateId, BulkShareRequest request, String grantedByClerkId) {
        log.info("Bulk sharing template {} with {} users and {} teams",
                templateId, request.userShares().size(), request.teamShares().size());

        // Validate template exists
        TestTemplate template = findTemplateOrThrow(templateId);

        // Validate grantor
        User grantor = findUserByClerkIdOrThrow(grantedByClerkId);

        // Validate DRAFT restriction
        if (template.getStatus() == TemplateStatus.DRAFT) {
            throw new IllegalStateException("Cannot share DRAFT templates. Only the owner can access DRAFT templates.");
        }

        // Get grantor's permission level for escalation checks
        SharePermission grantorPermission = getGrantorPermission(template, grantor);

        BulkShareResponse.Builder responseBuilder = BulkShareResponse.builder();

        // Process user shares
        for (ShareUserRequest userRequest : request.userShares()) {
            processUserShare(templateId, template, grantor, grantorPermission, userRequest, responseBuilder);
        }

        // Process team shares
        for (ShareTeamRequest teamRequest : request.teamShares()) {
            processTeamShare(templateId, template, grantor, grantorPermission, teamRequest, responseBuilder);
        }

        BulkShareResponse response = responseBuilder.build();
        log.info("Bulk share completed: {} created, {} updated, {} skipped, {} failed",
                response.createdCount(), response.updatedCount(),
                response.skippedCount(), response.failedCount());

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public TemplateShareDto getShareById(UUID shareId) {
        log.debug("Getting share {}", shareId);

        TemplateShare share = findShareOrThrow(shareId);
        return TemplateShareDto.fromEntity(share);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAccess(UUID templateId, UUID userId) {
        log.debug("Checking access for user {} on template {}", userId, templateId);

        // Check direct user share
        Optional<TemplateShare> userShare = shareRepository.findActiveByTemplateAndUser(templateId, userId);
        if (userShare.isPresent()) {
            return true;
        }

        // Check team shares
        List<UUID> userTeamIds = teamMemberRepository.findActiveTeamIdsByUserId(userId);
        if (!userTeamIds.isEmpty()) {
            return shareRepository.hasTeamPermission(templateId, userTeamIds, SharePermission.VIEW);
        }

        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public SharePermission getHighestPermission(UUID templateId, UUID userId) {
        log.debug("Getting highest permission for user {} on template {}", userId, templateId);

        SharePermission highest = null;

        // Check direct user share
        Optional<TemplateShare> userShare = shareRepository.findActiveByTemplateAndUser(templateId, userId);
        if (userShare.isPresent()) {
            highest = userShare.get().getPermission();
        }

        // Check team shares
        List<UUID> userTeamIds = teamMemberRepository.findActiveTeamIdsByUserId(userId);
        if (!userTeamIds.isEmpty()) {
            Optional<SharePermission> teamPermission = shareRepository
                    .findHighestPermissionByTemplateAndTeams(templateId, userTeamIds);

            if (teamPermission.isPresent()) {
                if (highest == null || teamPermission.get().getLevel() > highest.getLevel()) {
                    highest = teamPermission.get();
                }
            }
        }

        return highest;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canGrantPermission(UUID templateId, String grantorClerkId, SharePermission requestedPermission) {
        log.debug("Checking if {} can grant {} on template {}",
                grantorClerkId, requestedPermission, templateId);

        Optional<User> grantorOpt = userRepository.findByClerkId(grantorClerkId);
        if (grantorOpt.isEmpty()) {
            return false;
        }

        Optional<TestTemplate> templateOpt = templateRepository.findById(templateId);
        if (templateOpt.isEmpty()) {
            return false;
        }

        TestTemplate template = templateOpt.get();
        User grantor = grantorOpt.get();

        // Owner can grant any permission
        if (template.isOwnedBy(grantor)) {
            return true;
        }

        // Get grantor's permission level
        SharePermission grantorPermission = getHighestPermission(templateId, grantor.getId());

        // Can only grant up to their own permission level
        return grantorPermission != null && grantorPermission.includes(requestedPermission);
    }

    @Override
    @Transactional(readOnly = true)
    public long countShares(UUID templateId) {
        return shareRepository.countActiveByTemplateId(templateId);
    }

    @Override
    public int revokeAllShares(UUID templateId) {
        log.info("Revoking all shares for template {}", templateId);

        List<TemplateShare> shares = shareRepository.findActiveByTemplateId(templateId);
        int count = 0;

        for (TemplateShare share : shares) {
            share.revoke();
            shareRepository.save(share);
            count++;
        }

        log.info("Revoked {} shares for template {}", count, templateId);
        return count;
    }

    // ============================================
    // PRIVATE HELPER METHODS
    // ============================================

    private TestTemplate findTemplateOrThrow(UUID templateId) {
        return templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template", templateId));
    }

    private User findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private User findUserByClerkIdOrThrow(String clerkId) {
        return userRepository.findByClerkId(clerkId)
                .orElseThrow(() -> new ResourceNotFoundException("User with clerkId: " + clerkId));
    }

    private Team findTeamOrThrow(UUID teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", teamId));
    }

    private TemplateShare findShareOrThrow(UUID shareId) {
        return shareRepository.findById(shareId)
                .orElseThrow(() -> new ResourceNotFoundException("Share", shareId));
    }

    private void validateTemplateExists(UUID templateId) {
        if (!templateRepository.existsById(templateId)) {
            throw new ResourceNotFoundException("Template", templateId);
        }
    }

    /**
     * Validate a user share request.
     */
    private void validateShareRequest(TestTemplate template, User grantor, User grantee,
                                      SharePermission requestedPermission) {
        // Cannot share DRAFT templates (unless owner or admin)
        if (template.getStatus() == TemplateStatus.DRAFT
                && !template.isOwnedBy(grantor)
                && !grantor.isAdmin()) {
            throw new IllegalStateException("Cannot share DRAFT templates. Only the owner can access DRAFT templates.");
        }

        // Cannot share with yourself
        if (grantor.getId().equals(grantee.getId())) {
            throw new IllegalStateException("Cannot share a template with yourself");
        }

        // Cannot share with the owner
        if (template.getOwner() != null && template.getOwner().getId().equals(grantee.getId())) {
            throw new IllegalStateException("Cannot share a template with its owner - they already have full access");
        }

        // Check permission escalation
        validatePermissionEscalation(template, grantor, requestedPermission);
    }

    /**
     * Validate a team share request.
     */
    private void validateTeamShareRequest(TestTemplate template, User grantor, SharePermission requestedPermission) {
        // Cannot share DRAFT templates (unless owner or admin)
        if (template.getStatus() == TemplateStatus.DRAFT
                && !template.isOwnedBy(grantor)
                && !grantor.isAdmin()) {
            throw new IllegalStateException("Cannot share DRAFT templates. Only the owner can access DRAFT templates.");
        }

        // Check permission escalation
        validatePermissionEscalation(template, grantor, requestedPermission);
    }

    /**
     * Validate that the grantor has sufficient permission to grant the requested level.
     */
    private void validatePermissionEscalation(TestTemplate template, User grantor,
                                              SharePermission requestedPermission) {
        // Owner can grant any permission
        if (template.isOwnedBy(grantor)) {
            return;
        }

        // ADMIN users can share any template with any permission
        if (grantor.isAdmin()) {
            return;
        }

        // Get grantor's highest permission
        SharePermission grantorPermission = getHighestPermission(template.getId(), grantor.getId());

        if (grantorPermission == null) {
            throw new IllegalStateException("You do not have permission to share this template");
        }

        // Must have MANAGE permission to share
        if (!grantorPermission.includes(SharePermission.MANAGE)) {
            throw new IllegalStateException("You need MANAGE permission to share this template");
        }

        // Cannot grant higher than your own permission
        if (!grantorPermission.includes(requestedPermission)) {
            throw new IllegalStateException(
                    String.format("Cannot grant %s permission - you only have %s permission",
                            requestedPermission, grantorPermission));
        }
    }

    /**
     * Get the grantor's permission level for a template.
     */
    private SharePermission getGrantorPermission(TestTemplate template, User grantor) {
        if (template.isOwnedBy(grantor)) {
            return SharePermission.MANAGE;
        }
        // ADMIN users have full MANAGE permission on all templates
        if (grantor.isAdmin()) {
            return SharePermission.MANAGE;
        }
        return getHighestPermission(template.getId(), grantor.getId());
    }

    /**
     * Process a single user share in bulk operation.
     */
    private void processUserShare(UUID templateId, TestTemplate template, User grantor,
                                  SharePermission grantorPermission, ShareUserRequest request,
                                  BulkShareResponse.Builder responseBuilder) {
        try {
            // Validate user exists
            Optional<User> granteeOpt = userRepository.findById(request.userId());
            if (granteeOpt.isEmpty()) {
                responseBuilder.addUserError(request.userId().toString(), "User not found");
                return;
            }

            User grantee = granteeOpt.get();

            // Skip self-share
            if (grantor.getId().equals(grantee.getId())) {
                responseBuilder.addSkipped();
                return;
            }

            // Skip owner share
            if (template.getOwner() != null && template.getOwner().getId().equals(grantee.getId())) {
                responseBuilder.addSkipped();
                return;
            }

            // Check permission escalation
            if (grantorPermission != null && !grantorPermission.includes(request.permission())) {
                responseBuilder.addUserError(request.userId().toString(),
                        "Cannot grant " + request.permission() + " - insufficient permissions");
                return;
            }

            // Check for existing share
            Optional<TemplateShare> existingShare = shareRepository.findActiveByTemplateAndUser(
                    templateId, request.userId());

            TemplateShare share;
            boolean isUpdate = false;

            if (existingShare.isPresent()) {
                share = existingShare.get();
                share.setPermission(request.permission());
                share.setExpiresAt(request.expiresAt());
                isUpdate = true;
            } else {
                share = new TemplateShare(template, grantee, request.permission(), grantor);
                share.setExpiresAt(request.expiresAt());
            }

            share = shareRepository.save(share);

            if (isUpdate) {
                responseBuilder.addUpdated(TemplateShareDto.fromEntity(share));
            } else {
                responseBuilder.addCreated(TemplateShareDto.fromEntity(share));
            }

        } catch (Exception e) {
            log.error("Error processing user share for {}: {}", request.userId(), e.getMessage());
            responseBuilder.addUserError(request.userId().toString(), e.getMessage());
        }
    }

    /**
     * Process a single team share in bulk operation.
     */
    private void processTeamShare(UUID templateId, TestTemplate template, User grantor,
                                  SharePermission grantorPermission, ShareTeamRequest request,
                                  BulkShareResponse.Builder responseBuilder) {
        try {
            // Validate team exists
            Optional<Team> teamOpt = teamRepository.findById(request.teamId());
            if (teamOpt.isEmpty()) {
                responseBuilder.addTeamError(request.teamId().toString(), "Team not found");
                return;
            }

            Team team = teamOpt.get();

            // Check permission escalation
            if (grantorPermission != null && !grantorPermission.includes(request.permission())) {
                responseBuilder.addTeamError(request.teamId().toString(),
                        "Cannot grant " + request.permission() + " - insufficient permissions");
                return;
            }

            // Check for existing share
            Optional<TemplateShare> existingShare = shareRepository.findActiveByTemplateAndTeam(
                    templateId, request.teamId());

            TemplateShare share;
            boolean isUpdate = false;

            if (existingShare.isPresent()) {
                share = existingShare.get();
                share.setPermission(request.permission());
                share.setExpiresAt(request.expiresAt());
                isUpdate = true;
            } else {
                share = new TemplateShare(template, team, request.permission(), grantor);
                share.setExpiresAt(request.expiresAt());
            }

            share = shareRepository.save(share);

            if (isUpdate) {
                responseBuilder.addUpdated(TemplateShareDto.fromEntity(share));
            } else {
                responseBuilder.addCreated(TemplateShareDto.fromEntity(share));
            }

        } catch (Exception e) {
            log.error("Error processing team share for {}: {}", request.teamId(), e.getMessage());
            responseBuilder.addTeamError(request.teamId().toString(), e.getMessage());
        }
    }

    // ============================================
    // SHARED WITH ME OPERATIONS
    // ============================================

    @Override
    @Transactional(readOnly = true)
    public SharedTemplatesResponseDto getTemplatesSharedWithMe(String clerkId) {
        log.info("Getting templates shared with user: {}", clerkId);

        // Resolve user from clerkId
        Optional<User> userOpt = userRepository.findByClerkId(clerkId);
        if (userOpt.isEmpty()) {
            log.warn("User not found for clerkId: {}, returning empty result", clerkId);
            return SharedTemplatesResponseDto.empty();
        }

        User user = userOpt.get();
        UUID userId = user.getId();

        // Get user's team IDs
        List<UUID> teamIds = teamMemberRepository.findActiveTeamIdsByUserId(userId);

        // Fetch direct user shares with eager loading
        List<TemplateShare> directShares = shareRepository.findActiveSharesForUserWithDetails(userId);

        // Fetch team shares with eager loading (only if user has teams)
        List<TemplateShare> teamShares = teamIds.isEmpty()
                ? List.of()
                : shareRepository.findActiveTeamSharesForUserWithDetails(teamIds, userId);

        // Merge shares, keeping highest permission per template
        // Use LinkedHashMap to preserve order by grantedAt
        Map<UUID, TemplateShare> bestShareByTemplate = new LinkedHashMap<>();

        // Process direct shares first (they take precedence for sharedBy metadata)
        for (TemplateShare share : directShares) {
            UUID templateId = share.getTemplate().getId();
            bestShareByTemplate.merge(templateId, share, this::keepHighestPermissionPreferDirect);
        }

        // Process team shares
        for (TemplateShare share : teamShares) {
            UUID templateId = share.getTemplate().getId();
            bestShareByTemplate.merge(templateId, share, this::keepHighestPermissionPreferDirect);
        }

        // Convert to DTOs
        List<SharedTemplateItemDto> items = bestShareByTemplate.values().stream()
                .map(SharedTemplateItemDto::fromEntity)
                .toList();

        log.info("Found {} templates shared with user {}", items.size(), clerkId);
        return SharedTemplatesResponseDto.of(items);
    }

    @Override
    @Transactional(readOnly = true)
    public long countTemplatesSharedWithMe(String clerkId) {
        log.debug("Counting templates shared with user: {}", clerkId);

        Optional<User> userOpt = userRepository.findByClerkId(clerkId);
        if (userOpt.isEmpty()) {
            return 0;
        }

        User user = userOpt.get();
        List<UUID> teamIds = teamMemberRepository.findActiveTeamIdsByUserId(user.getId());

        // Handle case where user has no teams - pass empty placeholder to avoid SQL issues
        if (teamIds.isEmpty()) {
            // Count only direct shares when user has no teams
            return shareRepository.findActiveSharesForUserWithDetails(user.getId()).size();
        }

        return shareRepository.countTemplatesSharedWithUser(user.getId(), teamIds);
    }

    /**
     * Helper: Keep share with highest permission, preferring the existing (direct) share for metadata.
     */
    private TemplateShare keepHighestPermissionPreferDirect(TemplateShare existing, TemplateShare incoming) {
        // If incoming has higher permission level, use it
        if (incoming.getPermission().getLevel() > existing.getPermission().getLevel()) {
            return incoming;
        }
        // Otherwise keep existing (direct share preferred for sharedBy metadata)
        return existing;
    }
}
