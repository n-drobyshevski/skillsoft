package app.skillsoft.assessmentbackend.services.sharing;

import app.skillsoft.assessmentbackend.domain.dto.sharing.CreateShareLinkRequest;
import app.skillsoft.assessmentbackend.domain.dto.sharing.LinkValidationResult;
import app.skillsoft.assessmentbackend.domain.dto.sharing.ShareLinkDto;
import app.skillsoft.assessmentbackend.domain.entities.SharePermission;
import app.skillsoft.assessmentbackend.domain.entities.TemplateShareLink;
import app.skillsoft.assessmentbackend.domain.entities.TemplateStatus;
import app.skillsoft.assessmentbackend.domain.entities.TemplateVisibility;
import app.skillsoft.assessmentbackend.domain.entities.TestTemplate;
import app.skillsoft.assessmentbackend.domain.entities.User;
import app.skillsoft.assessmentbackend.repository.TemplateShareLinkRepository;
import app.skillsoft.assessmentbackend.repository.TestSessionRepository;
import app.skillsoft.assessmentbackend.repository.TestTemplateRepository;
import app.skillsoft.assessmentbackend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of TemplateShareLinkService.
 *
 * Manages the lifecycle of template share links including creation,
 * validation, usage tracking, and revocation.
 *
 * Business Rules:
 * - Maximum 10 active links per template (configurable)
 * - Maximum 365 days expiry
 * - Links only work when template visibility is LINK
 * - Links can only grant VIEW or EDIT permission (not MANAGE)
 */
@Service
@Transactional
public class TemplateShareLinkServiceImpl implements TemplateShareLinkService {

    private static final Logger log = LoggerFactory.getLogger(TemplateShareLinkServiceImpl.class);

    private final TemplateShareLinkRepository linkRepository;
    private final TestSessionRepository sessionRepository;
    private final TestTemplateRepository templateRepository;
    private final UserRepository userRepository;

    @Value("${app.share.base-url:}")
    private String baseUrl;

    public TemplateShareLinkServiceImpl(
            TemplateShareLinkRepository linkRepository,
            TestSessionRepository sessionRepository,
            TestTemplateRepository templateRepository,
            UserRepository userRepository) {
        this.linkRepository = linkRepository;
        this.sessionRepository = sessionRepository;
        this.templateRepository = templateRepository;
        this.userRepository = userRepository;
    }

    @Override
    public ShareLinkDto createLink(UUID templateId, CreateShareLinkRequest request, String createdByClerkId) {
        log.debug("Creating share link for template {} by user {}", templateId, createdByClerkId);

        // Validate template exists
        TestTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));

        // Validate template is not archived
        if (template.getStatus() == TemplateStatus.ARCHIVED) {
            throw new IllegalArgumentException("Cannot create share links for archived templates");
        }

        // Validate user exists
        User createdBy = userRepository.findByClerkId(createdByClerkId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + createdByClerkId));

        // Check link limit
        if (!canCreateLink(templateId)) {
            throw new IllegalStateException(
                    "Maximum share links limit reached (" + TemplateShareLink.MAX_LINKS_PER_TEMPLATE +
                    " active links per template)");
        }

        // Validate permission (links can only grant VIEW or EDIT, not MANAGE)
        SharePermission permission = request.permission();
        if (permission == SharePermission.MANAGE) {
            throw new IllegalArgumentException("Share links cannot grant MANAGE permission");
        }

        // Validate expiry
        int expiresInDays = request.expiresInDays();
        if (expiresInDays < 1) {
            expiresInDays = TemplateShareLink.DEFAULT_EXPIRY_DAYS;
        }
        if (expiresInDays > TemplateShareLink.MAX_EXPIRY_DAYS) {
            throw new IllegalArgumentException(
                    "Expiry cannot exceed " + TemplateShareLink.MAX_EXPIRY_DAYS + " days");
        }

        // Warn if template visibility is not LINK
        if (template.getVisibility() != TemplateVisibility.LINK) {
            log.warn("Creating share link for template {} with visibility {}, " +
                    "link will not work until visibility is changed to LINK",
                    templateId, template.getVisibility());
        }

        // Create the link
        TemplateShareLink link = new TemplateShareLink(
                template,
                createdBy,
                expiresInDays,
                request.maxUses(),
                permission,
                request.label()
        );

        link = linkRepository.save(link);
        log.info("Created share link {} for template {} by user {}",
                link.getId(), templateId, createdByClerkId);

        // Return DTO with full token visible (creator can see full token)
        return ShareLinkDto.fromEntity(link, getEffectiveBaseUrl(), false);
    }

    @Override
    @Transactional(readOnly = true)
    public LinkValidationResult validateLink(String token) {
        if (token == null || token.isBlank()) {
            log.debug("Empty token provided for validation");
            return LinkValidationResult.notFound();
        }

        // Find the link by token
        Optional<TemplateShareLink> linkOpt = linkRepository.findByToken(token);
        if (linkOpt.isEmpty()) {
            log.debug("Share link not found for token");
            return LinkValidationResult.notFound();
        }

        TemplateShareLink link = linkOpt.get();

        // Check if revoked
        if (!link.isActive() || link.getRevokedAt() != null) {
            log.debug("Share link {} is revoked", link.getId());
            return LinkValidationResult.revoked();
        }

        // Check if expired
        if (link.isExpired()) {
            log.debug("Share link {} has expired", link.getId());
            return LinkValidationResult.expired();
        }

        // Check if max uses reached
        if (link.hasReachedMaxUses()) {
            log.debug("Share link {} has reached max uses", link.getId());
            return LinkValidationResult.maxUsesReached();
        }

        // Verify template exists
        TestTemplate template = link.getTemplate();
        if (template == null) {
            log.warn("Share link {} references null template", link.getId());
            return LinkValidationResult.templateNotFound();
        }

        // Check template is not archived
        if (template.getStatus() == TemplateStatus.ARCHIVED) {
            log.debug("Template {} is archived, share link {} invalid", template.getId(), link.getId());
            return LinkValidationResult.templateArchived();
        }

        // Check template visibility is LINK
        if (template.getVisibility() != TemplateVisibility.LINK) {
            log.debug("Template {} visibility is {}, not LINK. Share link {} invalid",
                    template.getId(), template.getVisibility(), link.getId());
            return LinkValidationResult.visibilityMismatch();
        }

        // Link is valid
        log.debug("Share link {} validated successfully for template {}",
                link.getId(), template.getId());
        return LinkValidationResult.fromLink(link);
    }

    @Override
    public void revokeLink(UUID linkId) {
        log.debug("Revoking share link {}", linkId);

        TemplateShareLink link = linkRepository.findById(linkId)
                .orElseThrow(() -> new IllegalArgumentException("Share link not found: " + linkId));

        if (!link.isActive()) {
            log.debug("Share link {} already revoked", linkId);
            return;
        }

        link.revoke();
        linkRepository.save(link);

        // Abandon all active anonymous sessions for this link
        int abandonedCount = sessionRepository.abandonSessionsByShareLinkId(linkId);
        if (abandonedCount > 0) {
            log.info("Revoked share link {} and abandoned {} active sessions", linkId, abandonedCount);
        } else {
            log.info("Revoked share link {}", linkId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShareLinkDto> listLinks(UUID templateId, String requestingClerkId) {
        log.debug("Listing all links for template {} requested by {}", templateId, requestingClerkId);

        List<TemplateShareLink> links = linkRepository.findByTemplateIdOrderByCreatedAtDesc(templateId);

        return links.stream()
                .map(link -> ShareLinkDto.fromEntity(
                        link,
                        getEffectiveBaseUrl(),
                        shouldMaskToken(link, requestingClerkId)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShareLinkDto> listActiveLinks(UUID templateId, String requestingClerkId) {
        log.debug("Listing active links for template {} requested by {}", templateId, requestingClerkId);

        List<TemplateShareLink> links = linkRepository.findActiveByTemplateId(templateId);

        return links.stream()
                .map(link -> ShareLinkDto.fromEntity(
                        link,
                        getEffectiveBaseUrl(),
                        shouldMaskToken(link, requestingClerkId)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ShareLinkDto getLinkById(UUID linkId, String requestingClerkId) {
        log.debug("Getting link {} requested by {}", linkId, requestingClerkId);

        return linkRepository.findById(linkId)
                .map(link -> ShareLinkDto.fromEntity(
                        link,
                        getEffectiveBaseUrl(),
                        shouldMaskToken(link, requestingClerkId)))
                .orElse(null);
    }

    @Override
    public int revokeAllLinks(UUID templateId) {
        log.debug("Revoking all active links for template {}", templateId);

        int revokedCount = linkRepository.revokeAllByTemplateId(templateId);

        // Abandon all active anonymous sessions for this template's share links
        int abandonedCount = sessionRepository.abandonAnonymousSessionsByTemplateId(templateId);
        if (abandonedCount > 0) {
            log.info("Revoked {} share links and abandoned {} active sessions for template {}",
                    revokedCount, abandonedCount, templateId);
        } else {
            log.info("Revoked {} share links for template {}", revokedCount, templateId);
        }

        return revokedCount;
    }

    @Override
    public boolean recordUsage(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        Optional<TemplateShareLink> linkOpt = linkRepository.findValidByToken(token);
        if (linkOpt.isEmpty()) {
            log.debug("Cannot record usage: share link not found or invalid for token");
            return false;
        }

        TemplateShareLink link = linkOpt.get();

        // Also verify template visibility is still LINK
        if (link.getTemplate().getVisibility() != TemplateVisibility.LINK) {
            log.debug("Cannot record usage: template visibility is not LINK");
            return false;
        }

        // Use atomic increment to prevent race conditions with maxUses limit.
        // Returns 0 if the link has reached its usage limit (concurrent request won the race).
        int updated = linkRepository.incrementUsage(link.getId());
        if (updated == 0) {
            log.debug("Cannot record usage: share link {} has reached its usage limit", link.getId());
            return false;
        }

        log.debug("Recorded usage for share link {} (atomic increment)", link.getId());
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canCreateLink(UUID templateId) {
        long activeCount = linkRepository.countActiveByTemplateId(templateId);
        return activeCount < TemplateShareLink.MAX_LINKS_PER_TEMPLATE;
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveLinks(UUID templateId) {
        return linkRepository.countActiveByTemplateId(templateId);
    }

    @Override
    public int getMaxLinksPerTemplate() {
        return TemplateShareLink.MAX_LINKS_PER_TEMPLATE;
    }

    @Override
    public int getMaxExpiryDays() {
        return TemplateShareLink.MAX_EXPIRY_DAYS;
    }

    // ============================================
    // PRIVATE HELPERS
    // ============================================

    /**
     * Determine if the token should be masked for the requesting user.
     * Only the link creator can see the full token.
     *
     * @param link The share link
     * @param requestingClerkId The clerk ID of the requesting user
     * @return true if the token should be masked
     */
    private boolean shouldMaskToken(TemplateShareLink link, String requestingClerkId) {
        if (requestingClerkId == null) {
            return true;
        }
        if (link.getCreatedBy() == null) {
            return true;
        }
        return !requestingClerkId.equals(link.getCreatedBy().getClerkId());
    }

    /**
     * Get the effective base URL for generating share URLs.
     * Falls back to empty string if not configured.
     *
     * @return The base URL or empty string
     */
    private String getEffectiveBaseUrl() {
        return baseUrl != null && !baseUrl.isBlank() ? baseUrl : null;
    }
}
