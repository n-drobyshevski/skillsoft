package app.skillsoft.assessmentbackend.services.security;

import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.repository.TeamMemberRepository;
import app.skillsoft.assessmentbackend.repository.TemplateShareLinkRepository;
import app.skillsoft.assessmentbackend.repository.TemplateShareRepository;
import app.skillsoft.assessmentbackend.repository.TestTemplateRepository;
import app.skillsoft.assessmentbackend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of template access control for resource-level authorization.
 *
 * This service is used by @PreAuthorize SpEL expressions:
 * - @PreAuthorize("@templateSecurity.canAccess(#id, T(SharePermission).VIEW)")
 * - @PreAuthorize("@templateSecurity.canManageSharing(#id)")
 * - @PreAuthorize("@templateSecurity.isOwner(#id)")
 *
 * Access priority (evaluated in order):
 * 1. Valid share link token (allows anonymous access for LINK visibility)
 * 2. Authentication required for remaining checks
 * 3. ADMIN role (bypass)
 * 4. Template owner
 * 5. Direct user share
 * 6. Team membership share
 * 7. PUBLIC visibility (VIEW only)
 */
@Service("templateSecurity")
@Transactional(readOnly = true)
public class TemplateSecurityServiceImpl implements TemplateSecurityService {

    private static final Logger log = LoggerFactory.getLogger(TemplateSecurityServiceImpl.class);

    private final TestTemplateRepository templateRepository;
    private final TemplateShareRepository shareRepository;
    private final TemplateShareLinkRepository linkRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;

    public TemplateSecurityServiceImpl(
            TestTemplateRepository templateRepository,
            TemplateShareRepository shareRepository,
            TemplateShareLinkRepository linkRepository,
            TeamMemberRepository teamMemberRepository,
            UserRepository userRepository) {
        this.templateRepository = templateRepository;
        this.shareRepository = shareRepository;
        this.linkRepository = linkRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
    }

    @Override
    public boolean canAccess(UUID templateId, SharePermission required) {
        return canAccess(templateId, required, null);
    }

    @Override
    @Transactional
    public boolean canAccess(UUID templateId, SharePermission required, String shareToken) {
        if (templateId == null) {
            log.warn("canAccess called with null templateId");
            return false;
        }

        // LEVEL 0: Check share link token first (allows anonymous access)
        if (shareToken != null && !shareToken.isBlank()) {
            if (checkShareLinkAccess(templateId, shareToken, required)) {
                log.debug("Access granted via share link for template {}", templateId);
                return true;
            }
        }

        // From here on, authentication is required
        String clerkId = getAuthenticatedClerkId();
        if (clerkId == null) {
            log.debug("No authenticated user for template access check");
            return false;
        }

        Optional<TestTemplate> templateOpt = templateRepository.findById(templateId);
        if (templateOpt.isEmpty()) {
            log.debug("Template {} not found", templateId);
            return false;
        }

        TestTemplate template = templateOpt.get();

        // LEVEL 1: Archived templates are read-only
        if (template.getStatus() == TemplateStatus.ARCHIVED && required != SharePermission.VIEW) {
            log.debug("Archived template {} is read-only", templateId);
            return false;
        }

        // LEVEL 2: ADMIN bypass
        if (isAdmin()) {
            log.debug("Admin access granted for template {}", templateId);
            return true;
        }

        // LEVEL 3: Owner check
        if (template.isOwnedByClerkId(clerkId)) {
            log.debug("Owner access granted for template {}", templateId);
            return true;
        }

        // Get user entity for share checks
        Optional<User> userOpt = userRepository.findByClerkId(clerkId);
        if (userOpt.isEmpty()) {
            log.warn("User with clerkId {} not found in database", clerkId);
            return false;
        }
        User user = userOpt.get();

        // LEVEL 4: Direct user share check
        Optional<TemplateShare> userShare = shareRepository.findActiveByTemplateAndUser(
                templateId, user.getId());
        if (userShare.isPresent() && userShare.get().hasPermission(required)) {
            log.debug("User share access granted for template {}", templateId);
            return true;
        }

        // LEVEL 5: Team share check
        List<UUID> userTeamIds = teamMemberRepository.findActiveTeamIdsByUserId(user.getId());
        if (!userTeamIds.isEmpty()) {
            Optional<SharePermission> teamPermission = shareRepository
                    .findHighestPermissionByTemplateAndTeams(templateId, userTeamIds);
            if (teamPermission.isPresent() && teamPermission.get().includes(required)) {
                log.debug("Team share access granted for template {}", templateId);
                return true;
            }
        }

        // LEVEL 6: Public visibility check (VIEW only)
        if (template.getVisibility() == TemplateVisibility.PUBLIC
                && template.getStatus() == TemplateStatus.PUBLISHED
                && required == SharePermission.VIEW) {
            log.debug("Public access granted for template {}", templateId);
            return true;
        }

        log.debug("Access denied for template {} with permission {}", templateId, required);
        return false;
    }

    /**
     * Check if a share link token grants access to a template.
     */
    @Transactional
    private boolean checkShareLinkAccess(UUID templateId, String token, SharePermission required) {
        Optional<TemplateShareLink> linkOpt = linkRepository.findValidByToken(token);
        if (linkOpt.isEmpty()) {
            log.debug("Invalid or expired share link token");
            return false;
        }

        TemplateShareLink link = linkOpt.get();

        // Verify token is for the correct template
        if (!link.getTemplate().getId().equals(templateId)) {
            log.warn("Share link token used for wrong template");
            return false;
        }

        // Check template visibility allows link access
        if (link.getTemplate().getVisibility() != TemplateVisibility.LINK) {
            log.debug("Template visibility is not LINK, share link access denied");
            return false;
        }

        // Check permission level
        if (!link.hasPermission(required)) {
            log.debug("Share link permission {} insufficient for required {}",
                    link.getPermission(), required);
            return false;
        }

        // Record usage (this will update currentUses and lastUsedAt)
        link.recordUsage();
        linkRepository.save(link);

        return true;
    }

    @Override
    public boolean isValidShareToken(UUID templateId, String shareToken) {
        if (templateId == null || shareToken == null || shareToken.isBlank()) {
            return false;
        }

        Optional<TemplateShareLink> linkOpt = linkRepository.findValidByToken(shareToken);
        if (linkOpt.isEmpty()) {
            return false;
        }

        TemplateShareLink link = linkOpt.get();
        return link.getTemplate().getId().equals(templateId)
                && link.getTemplate().getVisibility() == TemplateVisibility.LINK;
    }

    @Override
    public boolean isOwner(UUID templateId) {
        if (templateId == null) {
            return false;
        }

        String clerkId = getAuthenticatedClerkId();
        if (clerkId == null) {
            return false;
        }

        Optional<TestTemplate> templateOpt = templateRepository.findById(templateId);
        return templateOpt.map(t -> t.isOwnedByClerkId(clerkId)).orElse(false);
    }

    @Override
    public boolean canManageSharing(UUID templateId) {
        return isOwner(templateId) || isAdmin() || canAccess(templateId, SharePermission.MANAGE);
    }

    @Override
    public boolean canEdit(UUID templateId) {
        return isOwner(templateId) || isAdmin() || canAccess(templateId, SharePermission.EDIT);
    }

    @Override
    public boolean canView(UUID templateId) {
        return canAccess(templateId, SharePermission.VIEW);
    }

    @Override
    public boolean canDelete(UUID templateId) {
        return isOwner(templateId) || isAdmin();
    }

    @Override
    public boolean canChangeVisibility(UUID templateId) {
        return isOwner(templateId) || isAdmin() || canAccess(templateId, SharePermission.MANAGE);
    }

    @Override
    public boolean canCreateLinks(UUID templateId) {
        return canManageSharing(templateId);
    }

    @Override
    public String getAuthenticatedClerkId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof String userId) {
            return userId;
        }

        // Handle anonymous authentication
        if ("anonymousUser".equals(principal)) {
            return null;
        }

        return principal != null ? principal.toString() : null;
    }

    @Override
    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    @Override
    public boolean isEditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_EDITOR"::equals);
    }

    @Override
    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());
    }

    @Override
    public boolean canViewAnonymousResults(UUID templateId) {
        // Must be authenticated to view anonymous results
        if (!isAuthenticated()) {
            return false;
        }

        // Admin can view all anonymous results
        if (isAdmin()) {
            return true;
        }

        // Owner can view their template's anonymous results
        if (isOwner(templateId)) {
            return true;
        }

        // Users with MANAGE permission can view anonymous results
        return canAccess(templateId, SharePermission.MANAGE);
    }
}
