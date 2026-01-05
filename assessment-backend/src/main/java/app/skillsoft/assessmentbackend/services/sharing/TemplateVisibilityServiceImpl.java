package app.skillsoft.assessmentbackend.services.sharing;

import app.skillsoft.assessmentbackend.domain.dto.sharing.VisibilityInfoDto;
import app.skillsoft.assessmentbackend.domain.entities.TemplateStatus;
import app.skillsoft.assessmentbackend.domain.entities.TemplateVisibility;
import app.skillsoft.assessmentbackend.domain.entities.TestTemplate;
import app.skillsoft.assessmentbackend.exception.ResourceNotFoundException;
import app.skillsoft.assessmentbackend.repository.TemplateShareLinkRepository;
import app.skillsoft.assessmentbackend.repository.TemplateShareRepository;
import app.skillsoft.assessmentbackend.repository.TestTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Implementation of TemplateVisibilityService.
 *
 * Manages template visibility settings with proper business rule enforcement.
 */
@Service
@Transactional
public class TemplateVisibilityServiceImpl implements TemplateVisibilityService {

    private static final Logger log = LoggerFactory.getLogger(TemplateVisibilityServiceImpl.class);

    private final TestTemplateRepository templateRepository;
    private final TemplateShareRepository shareRepository;
    private final TemplateShareLinkRepository linkRepository;

    public TemplateVisibilityServiceImpl(
            TestTemplateRepository templateRepository,
            TemplateShareRepository shareRepository,
            TemplateShareLinkRepository linkRepository) {
        this.templateRepository = templateRepository;
        this.shareRepository = shareRepository;
        this.linkRepository = linkRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public VisibilityInfoDto getVisibilityInfo(UUID templateId) {
        log.debug("Getting visibility info for template {}", templateId);

        TestTemplate template = findTemplateOrThrow(templateId);

        long activeSharesCount = shareRepository.countActiveByTemplateId(templateId);
        long activeLinksCount = linkRepository.countActiveByTemplateId(templateId);

        return VisibilityInfoDto.fromTemplate(template, activeSharesCount, activeLinksCount);
    }

    @Override
    public VisibilityInfoDto changeVisibility(UUID templateId, TemplateVisibility visibility, String changedByClerkId) {
        log.info("Changing visibility for template {} to {} by user {}",
                templateId, visibility, changedByClerkId);

        TestTemplate template = findTemplateOrThrow(templateId);

        // Validate business rules
        validateVisibilityChange(template, visibility);

        TemplateVisibility previousVisibility = template.getVisibility();

        // If changing away from LINK, revoke all share links
        if (previousVisibility == TemplateVisibility.LINK && visibility != TemplateVisibility.LINK) {
            int revokedCount = linkRepository.revokeAllByTemplateId(templateId);
            if (revokedCount > 0) {
                log.info("Revoked {} share links due to visibility change from LINK to {}",
                        revokedCount, visibility);
            }
        }

        // Update visibility
        template.setVisibility(visibility);
        template.setVisibilityChangedAt(LocalDateTime.now());

        template = templateRepository.save(template);

        log.info("Changed visibility for template {} from {} to {}",
                templateId, previousVisibility, visibility);

        // Return updated visibility info
        long activeSharesCount = shareRepository.countActiveByTemplateId(templateId);
        long activeLinksCount = linkRepository.countActiveByTemplateId(templateId);

        return VisibilityInfoDto.fromTemplate(template, activeSharesCount, activeLinksCount);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canChangeToVisibility(UUID templateId, TemplateVisibility targetVisibility) {
        try {
            TestTemplate template = findTemplateOrThrow(templateId);
            validateVisibilityChange(template, targetVisibility);
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TemplateVisibility getCurrentVisibility(UUID templateId) {
        return templateRepository.findById(templateId)
                .map(TestTemplate::getVisibility)
                .orElse(null);
    }

    // ============================================
    // PRIVATE HELPERS
    // ============================================

    private TestTemplate findTemplateOrThrow(UUID templateId) {
        return templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template", templateId));
    }

    /**
     * Validate that a visibility change is allowed based on business rules.
     *
     * @param template The template to change
     * @param newVisibility The target visibility
     * @throws IllegalStateException if the change violates business rules
     */
    private void validateVisibilityChange(TestTemplate template, TemplateVisibility newVisibility) {
        // ARCHIVED templates cannot change visibility
        if (template.getStatus() == TemplateStatus.ARCHIVED) {
            throw new IllegalStateException(
                    "Cannot change visibility of archived templates. Archived templates are always PRIVATE.");
        }

        // DRAFT templates can only be PRIVATE
        if (template.getStatus() == TemplateStatus.DRAFT) {
            if (newVisibility != TemplateVisibility.PRIVATE) {
                throw new IllegalStateException(
                        "DRAFT templates can only be PRIVATE. Publish the template first to enable PUBLIC or LINK visibility.");
            }
        }

        // Additional validation: Same visibility is a no-op but allowed
        if (template.getVisibility() == newVisibility) {
            log.debug("Visibility is already {}, no change needed", newVisibility);
        }
    }
}
