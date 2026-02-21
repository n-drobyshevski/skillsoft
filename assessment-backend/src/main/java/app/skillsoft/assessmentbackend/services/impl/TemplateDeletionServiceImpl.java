package app.skillsoft.assessmentbackend.services.impl;

import app.skillsoft.assessmentbackend.config.CacheConfig;
import app.skillsoft.assessmentbackend.domain.dto.DeletionPreviewDto;
import app.skillsoft.assessmentbackend.domain.dto.DeletionResultDto;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.exception.ResourceNotFoundException;
import app.skillsoft.assessmentbackend.repository.*;
import app.skillsoft.assessmentbackend.services.TemplateDeletionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of template deletion service.
 * Handles safe deletion of templates with dependency chain management.
 */
@Service
@Transactional
public class TemplateDeletionServiceImpl implements TemplateDeletionService {

    private static final Logger log = LoggerFactory.getLogger(TemplateDeletionServiceImpl.class);

    private final TestTemplateRepository templateRepository;
    private final TestSessionRepository sessionRepository;
    private final TestResultRepository resultRepository;
    private final TemplateShareRepository shareRepository;
    private final TemplateShareLinkRepository shareLinkRepository;
    private final TestActivityEventRepository activityEventRepository;

    public TemplateDeletionServiceImpl(
            TestTemplateRepository templateRepository,
            TestSessionRepository sessionRepository,
            TestResultRepository resultRepository,
            TemplateShareRepository shareRepository,
            TemplateShareLinkRepository shareLinkRepository,
            TestActivityEventRepository activityEventRepository) {
        this.templateRepository = templateRepository;
        this.sessionRepository = sessionRepository;
        this.resultRepository = resultRepository;
        this.shareRepository = shareRepository;
        this.shareLinkRepository = shareLinkRepository;
        this.activityEventRepository = activityEventRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DeletionPreviewDto previewDeletion(UUID templateId) {
        log.info("Generating deletion preview for template: {}", templateId);

        TestTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("TestTemplate", templateId));

        // Check if already soft-deleted
        if (template.isDeleted()) {
            return DeletionPreviewDto.forSoftDeletedTemplate(templateId, template.getName());
        }

        // Count sessions by status using efficient COUNT queries (prevents OOM for popular templates)
        long activeSessions = sessionRepository.countByTemplate_IdAndStatusIn(
                templateId, List.of(SessionStatus.IN_PROGRESS, SessionStatus.NOT_STARTED));
        long completedSessions = sessionRepository.countByTemplate_IdAndStatus(
                templateId, SessionStatus.COMPLETED);
        long abandonedSessions = sessionRepository.countByTemplate_IdAndStatusIn(
                templateId, List.of(SessionStatus.ABANDONED, SessionStatus.TIMED_OUT));
        long totalSessions = activeSessions + completedSessions + abandonedSessions;

        // Count related entities
        long totalResults = resultRepository.countResultsByTemplateId(templateId);
        long totalAnswers = sessionRepository.countAnswersByTemplateId(templateId);
        long activeShares = shareRepository.countActiveByTemplateId(templateId);
        long activeShareLinks = shareLinkRepository.countActiveByTemplateId(templateId);
        long activityEvents = activityEventRepository.countByTemplateId(templateId);

        // Determine recommendations
        String recommendedMode;
        String warningMessage = null;
        boolean canForceDelete = true;
        boolean requiresConfirmation = false;

        if (template.getStatus() == TemplateStatus.DRAFT && totalSessions == 0) {
            recommendedMode = "FORCE_DELETE";
        } else if (completedSessions > 0) {
            recommendedMode = "SOFT_DELETE";
            warningMessage = String.format(
                "This template has %d completed test sessions. " +
                "Soft delete is recommended to preserve historical data.",
                completedSessions);
            requiresConfirmation = true;
        } else if (activeSessions > 0) {
            recommendedMode = "ARCHIVE_AND_CLEANUP";
            warningMessage = String.format(
                "Warning: %d sessions are currently active. " +
                "They will be marked as abandoned if you proceed.",
                activeSessions);
            requiresConfirmation = true;
        } else {
            recommendedMode = "FORCE_DELETE";
        }

        return new DeletionPreviewDto(
            templateId,
            template.getName(),
            template.getStatus().name(),
            activeSessions,
            completedSessions,
            abandonedSessions,
            totalSessions,
            totalResults,
            totalAnswers,
            activeShares,
            activeShareLinks,
            activityEvents,
            recommendedMode,
            warningMessage,
            canForceDelete,
            requiresConfirmation
        );
    }

    @Override
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.TEMPLATE_METADATA_CACHE, key = "#templateId"),
        @CacheEvict(value = CacheConfig.ACTIVE_TEMPLATES_CACHE, allEntries = true)
    })
    public DeletionResultDto deleteTemplate(UUID templateId, DeletionMode mode, boolean confirmedByUser) {
        return deleteTemplate(templateId, mode, confirmedByUser, null);
    }

    @Override
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.TEMPLATE_METADATA_CACHE, key = "#templateId"),
        @CacheEvict(value = CacheConfig.ACTIVE_TEMPLATES_CACHE, allEntries = true)
    })
    public DeletionResultDto deleteTemplate(UUID templateId, DeletionMode mode, boolean confirmedByUser, User deletedBy) {
        log.info("Deleting template {} with mode: {}", templateId, mode);

        TestTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("TestTemplate", templateId));

        // Get preview for validation
        DeletionPreviewDto preview = previewDeletion(templateId);

        // Validate confirmation for risky operations
        if (preview.requiresConfirmation() && !confirmedByUser) {
            throw new IllegalStateException(
                "This deletion requires explicit confirmation. " +
                "Please acknowledge the impact: " + preview.warningMessage());
        }

        return switch (mode) {
            case SOFT_DELETE -> executeSoftDelete(template, deletedBy);
            case ARCHIVE_AND_CLEANUP -> executeArchiveAndCleanup(template, deletedBy);
            case FORCE_DELETE -> executeForceDelete(template, preview);
        };
    }

    private DeletionResultDto executeSoftDelete(TestTemplate template, User deletedBy) {
        log.info("Executing soft delete for template: {}", template.getId());

        template.softDelete(deletedBy);
        templateRepository.save(template);

        return DeletionResultDto.softDeleted(template.getId(), template.getDeletedAt());
    }

    private DeletionResultDto executeArchiveAndCleanup(TestTemplate template, User deletedBy) {
        log.info("Executing archive and cleanup for template: {}", template.getId());

        // Archive the template if published
        if (template.getStatus() == TemplateStatus.PUBLISHED) {
            template.archive();
        }
        template.softDelete(deletedBy);
        templateRepository.save(template);

        // Cleanup incomplete sessions
        List<TestSession> incompleteSessions = sessionRepository.findByTemplate_Id(template.getId())
                .stream()
                .filter(s -> s.getStatus() == SessionStatus.NOT_STARTED ||
                            s.getStatus() == SessionStatus.IN_PROGRESS)
                .toList();

        int sessionsDeleted = 0;
        int answersDeleted = 0;

        for (TestSession session : incompleteSessions) {
            // Mark as abandoned first
            if (session.getStatus() == SessionStatus.IN_PROGRESS) {
                session.abandon();
            } else {
                session.setStatus(SessionStatus.ABANDONED);
                session.setCompletedAt(LocalDateTime.now());
            }
            sessionRepository.save(session);

            // Delete session data if no valuable results
            if (session.getResult() == null) {
                answersDeleted += session.getAnswers() != null ? session.getAnswers().size() : 0;
                sessionRepository.delete(session);
                sessionsDeleted++;
            }
        }

        return DeletionResultDto.archivedAndCleanedUp(
            template.getId(),
            template.getDeletedAt(),
            sessionsDeleted,
            answersDeleted
        );
    }

    private DeletionResultDto executeForceDelete(TestTemplate template, DeletionPreviewDto preview) {
        log.info("Executing FORCE DELETE for template: {}. This will delete {} sessions, {} results.",
                template.getId(), preview.totalSessions(), preview.totalResults());

        UUID templateId = template.getId();

        int activityEventsDeleted = 0;
        int resultsDeleted = 0;
        int answersDeleted = 0;
        int sessionsDeleted = 0;
        int shareLinksDeleted = 0;
        int sharesDeleted = 0;

        // Step 1: Delete activity events (no FK constraint)
        activityEventsDeleted = activityEventRepository.deleteByTemplateId(templateId);
        log.debug("Deleted {} activity events", activityEventsDeleted);

        // Step 2: Delete sessions with proper cascade
        // Load sessions to trigger JPA cascade for answers and results
        List<TestSession> sessions = sessionRepository.findByTemplate_Id(templateId);
        for (TestSession session : sessions) {
            if (session.getResult() != null) {
                resultsDeleted++;
            }
            if (session.getAnswers() != null) {
                answersDeleted += session.getAnswers().size();
            }
            sessionRepository.delete(session);
            sessionsDeleted++;
        }
        sessionRepository.flush(); // Ensure deletions are committed
        log.debug("Deleted {} sessions, {} results, {} answers",
                sessionsDeleted, resultsDeleted, answersDeleted);

        // Step 3: Delete share links
        shareLinksDeleted = shareLinkRepository.deleteByTemplateId(templateId);
        log.debug("Deleted {} share links", shareLinksDeleted);

        // Step 4: Delete shares
        sharesDeleted = shareRepository.deleteByTemplateId(templateId);
        log.debug("Deleted {} shares", sharesDeleted);

        // Step 5: Delete template
        templateRepository.delete(template);
        log.info("Template {} and all related data permanently deleted", templateId);

        return DeletionResultDto.forceDeleted(
            templateId,
            sessionsDeleted,
            resultsDeleted,
            answersDeleted,
            sharesDeleted,
            shareLinksDeleted,
            activityEventsDeleted
        );
    }

    @Override
    @CacheEvict(value = CacheConfig.ACTIVE_TEMPLATES_CACHE, allEntries = true)
    public boolean restoreTemplate(UUID templateId) {
        log.info("Restoring soft-deleted template: {}", templateId);

        return templateRepository.findById(templateId)
                .filter(TestTemplate::isDeleted)
                .map(template -> {
                    template.restore();
                    templateRepository.save(template);
                    log.info("Template {} restored successfully", templateId);
                    return true;
                })
                .orElse(false);
    }
}
