# Safe Test Template Deletion Workflow

## Implementation Plan

**Version:** 1.0
**Date:** 2026-01-11
**Status:** Design Complete

---

## 1. Problem Analysis

### 1.1 Root Cause

The FK constraint error occurs because:

```
update or delete on table "test_sessions" violates foreign key constraint
"fkrm997wevayfae5ty1w9wmput5" on table "test_results"
```

**Entity Dependency Graph:**

```
TestTemplate (to delete)
├── TestSession (FK: template_id -> test_templates.id)
│   ├── TestAnswer (FK: session_id -> test_sessions.id) [cascades via JPA]
│   └── TestResult (FK: session_id -> test_sessions.id) [cascades via JPA, but blocks at DB level]
├── TemplateShare (FK: template_id -> test_templates.id)
├── TemplateShareLink (FK: template_id -> test_templates.id)
└── TestActivityEvent (stores template_id as UUID, no FK constraint)
```

**Key Issue:** JPA cascade annotations (`CascadeType.ALL`, `orphanRemoval = true`) work when deleting entities through the EntityManager, but **not** when using bulk JPQL/SQL queries or when the database enforces FK constraints independently.

### 1.2 Current Entity Cascade Configuration

| Entity | Relationship | Cascade Config |
|--------|-------------|----------------|
| TestSession -> TestAnswer | @OneToMany | CascadeType.ALL, orphanRemoval=true |
| TestSession -> TestResult | @OneToOne | CascadeType.ALL, orphanRemoval=true |
| TestTemplate -> TestSession | No mapping | No cascade (sessions reference template) |
| TestTemplate -> TemplateShare | No mapping | No cascade |
| TestTemplate -> TemplateShareLink | No mapping | No cascade |

---

## 2. Solution Design

### 2.1 Hybrid Deletion Strategy

```
┌─────────────────────────────────────────────────────────────────┐
│                     DELETION DECISION TREE                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Template Status?                                                │
│       │                                                          │
│       ├─► DRAFT (no sessions) ──────► HARD DELETE allowed       │
│       │                                                          │
│       └─► PUBLISHED/ARCHIVED ───────► Check dependencies        │
│               │                                                  │
│               ├─► Has active sessions? ──► Block or Force       │
│               │                                                  │
│               └─► Has completed sessions?                        │
│                       │                                          │
│                       ├─► Soft Delete (preserve history)         │
│                       │                                          │
│                       └─► Force Delete (requires confirmation)   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 Deletion Modes

| Mode | Description | Use Case |
|------|-------------|----------|
| `SOFT_DELETE` | Sets `deletedAt` timestamp, preserves all data | Default for templates with results |
| `ARCHIVE_AND_CLEANUP` | Archives template + deletes incomplete sessions only | Clean up while preserving completed tests |
| `FORCE_DELETE` | Permanently deletes template and ALL related data | Admin cleanup, requires explicit confirmation |

### 2.3 Deletion Execution Order (FORCE_DELETE)

To avoid FK constraint violations, deletions must occur in dependency order:

```sql
-- Phase 1: Delete audit events (no FK constraint)
DELETE FROM test_activity_events WHERE template_id = ?

-- Phase 2: Delete sessions with proper cascade
-- Option A: Use JPA cascade (load entities, then delete)
-- Option B: Manual cascade in correct order:

DELETE FROM test_results WHERE session_id IN (
    SELECT id FROM test_sessions WHERE template_id = ?
)

DELETE FROM test_answers WHERE session_id IN (
    SELECT id FROM test_sessions WHERE template_id = ?
)

DELETE FROM test_sessions WHERE template_id = ?

-- Phase 3: Delete sharing data
DELETE FROM template_share_links WHERE template_id = ?
DELETE FROM template_shares WHERE template_id = ?

-- Phase 4: Delete template
DELETE FROM test_templates WHERE id = ?
```

---

## 3. Implementation Components

### 3.1 Entity Changes

#### TestTemplate.java - Add Soft Delete Support

```java
// Add to TestTemplate entity

/**
 * Soft delete timestamp.
 * When set, the template is considered deleted but data is preserved.
 * Null means the template is active.
 */
@Column(name = "deleted_at")
private LocalDateTime deletedAt;

/**
 * User who deleted this template.
 */
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "deleted_by_id")
private User deletedBy;

// Transient helper
@Transient
public boolean isDeleted() {
    return deletedAt != null;
}

public void softDelete(User deletedBy) {
    this.deletedAt = LocalDateTime.now();
    this.deletedBy = deletedBy;
    this.isActive = false;
}

public void restore() {
    this.deletedAt = null;
    this.deletedBy = null;
}
```

### 3.2 New Enums

#### DeletionMode.java

```java
package app.skillsoft.assessmentbackend.domain.entities;

/**
 * Mode for template deletion operations.
 */
public enum DeletionMode {
    /**
     * Soft delete - set deletedAt timestamp, preserve all data.
     * Template and all related data remain in database but are filtered out.
     * Recommended for templates with completed test sessions.
     */
    SOFT_DELETE,

    /**
     * Archive template and cleanup incomplete sessions only.
     * Preserves completed sessions and results for historical reference.
     * Deletes: NOT_STARTED and IN_PROGRESS sessions without results.
     */
    ARCHIVE_AND_CLEANUP,

    /**
     * Force delete - permanently remove template and ALL related data.
     * Requires explicit confirmation. Cannot be undone.
     * Use only for cleanup of test/draft templates.
     */
    FORCE_DELETE
}
```

### 3.3 New DTOs

#### DeletionPreviewDto.java

```java
package app.skillsoft.assessmentbackend.domain.dto;

import java.util.UUID;

/**
 * Preview of what will be affected by a template deletion.
 * Used to inform the user before they confirm deletion.
 */
public record DeletionPreviewDto(
    UUID templateId,
    String templateName,
    String templateStatus,

    // Session counts
    long activeSessions,      // IN_PROGRESS, NOT_STARTED
    long completedSessions,   // COMPLETED
    long abandonedSessions,   // ABANDONED, TIMED_OUT
    long totalSessions,

    // Related data counts
    long totalResults,
    long totalAnswers,
    long activeShares,
    long activeShareLinks,
    long activityEvents,

    // Recommendations
    String recommendedMode,
    String warningMessage,
    boolean canForceDelete,
    boolean requiresConfirmation
) {
    public static DeletionPreviewDto forDraftTemplate(UUID templateId, String name) {
        return new DeletionPreviewDto(
            templateId, name, "DRAFT",
            0, 0, 0, 0,
            0, 0, 0, 0, 0,
            "FORCE_DELETE",
            null,
            true,
            false
        );
    }
}
```

#### DeletionResultDto.java

```java
package app.skillsoft.assessmentbackend.domain.dto;

import app.skillsoft.assessmentbackend.domain.entities.DeletionMode;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Result of a template deletion operation.
 */
public record DeletionResultDto(
    UUID templateId,
    DeletionMode mode,
    boolean success,
    String message,
    LocalDateTime deletedAt,

    // Counts of deleted entities
    int sessionsDeleted,
    int resultsDeleted,
    int answersDeleted,
    int sharesDeleted,
    int shareLinksDeleted,
    int activityEventsDeleted,

    // For soft delete, can be restored
    boolean canRestore
) {
    public static DeletionResultDto softDeleted(UUID templateId, LocalDateTime deletedAt) {
        return new DeletionResultDto(
            templateId,
            DeletionMode.SOFT_DELETE,
            true,
            "Template soft deleted successfully. Data preserved for restoration.",
            deletedAt,
            0, 0, 0, 0, 0, 0,
            true
        );
    }

    public static DeletionResultDto forceDeleted(
            UUID templateId,
            int sessions, int results, int answers,
            int shares, int shareLinks, int events) {
        return new DeletionResultDto(
            templateId,
            DeletionMode.FORCE_DELETE,
            true,
            "Template and all related data permanently deleted.",
            LocalDateTime.now(),
            sessions, results, answers, shares, shareLinks, events,
            false
        );
    }
}
```

### 3.4 New Service

#### TemplateDeletionService.java (Interface)

```java
package app.skillsoft.assessmentbackend.services;

import app.skillsoft.assessmentbackend.domain.dto.DeletionPreviewDto;
import app.skillsoft.assessmentbackend.domain.dto.DeletionResultDto;
import app.skillsoft.assessmentbackend.domain.entities.DeletionMode;

import java.util.UUID;

/**
 * Service for safe deletion of test templates.
 * Handles dependency chain and provides preview before deletion.
 */
public interface TemplateDeletionService {

    /**
     * Get a preview of what will be affected by deleting a template.
     * Does not modify any data.
     *
     * @param templateId The template to analyze
     * @return Preview with counts and recommendations
     */
    DeletionPreviewDto previewDeletion(UUID templateId);

    /**
     * Delete a template using the specified mode.
     *
     * @param templateId The template to delete
     * @param mode The deletion mode (SOFT_DELETE, ARCHIVE_AND_CLEANUP, FORCE_DELETE)
     * @param confirmedByUser True if user explicitly confirmed force deletion
     * @return Result of the deletion operation
     */
    DeletionResultDto deleteTemplate(UUID templateId, DeletionMode mode, boolean confirmedByUser);

    /**
     * Restore a soft-deleted template.
     *
     * @param templateId The template to restore
     * @return True if restored successfully
     */
    boolean restoreTemplate(UUID templateId);
}
```

#### TemplateDeletionServiceImpl.java

```java
package app.skillsoft.assessmentbackend.services.impl;

import app.skillsoft.assessmentbackend.domain.dto.DeletionPreviewDto;
import app.skillsoft.assessmentbackend.domain.dto.DeletionResultDto;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.exception.ResourceNotFoundException;
import app.skillsoft.assessmentbackend.repository.*;
import app.skillsoft.assessmentbackend.services.TemplateDeletionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TemplateDeletionServiceImpl implements TemplateDeletionService {

    private static final Logger log = LoggerFactory.getLogger(TemplateDeletionServiceImpl.class);

    private final TestTemplateRepository templateRepository;
    private final TestSessionRepository sessionRepository;
    private final TestResultRepository resultRepository;
    private final TestAnswerRepository answerRepository;
    private final TemplateShareRepository shareRepository;
    private final TemplateShareLinkRepository shareLinkRepository;
    private final TestActivityEventRepository activityEventRepository;

    public TemplateDeletionServiceImpl(
            TestTemplateRepository templateRepository,
            TestSessionRepository sessionRepository,
            TestResultRepository resultRepository,
            TestAnswerRepository answerRepository,
            TemplateShareRepository shareRepository,
            TemplateShareLinkRepository shareLinkRepository,
            TestActivityEventRepository activityEventRepository) {
        this.templateRepository = templateRepository;
        this.sessionRepository = sessionRepository;
        this.resultRepository = resultRepository;
        this.answerRepository = answerRepository;
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

        // Count sessions by status
        List<TestSession> sessions = sessionRepository.findByTemplate_Id(templateId);

        long activeSessions = sessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.IN_PROGRESS ||
                            s.getStatus() == SessionStatus.NOT_STARTED)
                .count();

        long completedSessions = sessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.COMPLETED)
                .count();

        long abandonedSessions = sessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.ABANDONED ||
                            s.getStatus() == SessionStatus.TIMED_OUT)
                .count();

        // Count related entities
        long totalResults = resultRepository.countResultsByTemplateId(templateId);
        long totalAnswers = sessions.stream()
                .mapToLong(s -> s.getAnswers() != null ? s.getAnswers().size() : 0)
                .sum();
        long activeShares = shareRepository.countActiveByTemplateId(templateId);
        long activeShareLinks = shareLinkRepository.countActiveByTemplateId(templateId);
        long activityEvents = activityEventRepository.countByTemplateId(templateId);

        // Determine recommendations
        String recommendedMode;
        String warningMessage = null;
        boolean canForceDelete = true;
        boolean requiresConfirmation = false;

        if (template.getStatus() == TemplateStatus.DRAFT && sessions.isEmpty()) {
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
            sessions.size(),
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
    @Transactional
    public DeletionResultDto deleteTemplate(UUID templateId, DeletionMode mode, boolean confirmedByUser) {
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
            case SOFT_DELETE -> executeSoftDelete(template);
            case ARCHIVE_AND_CLEANUP -> executeArchiveAndCleanup(template);
            case FORCE_DELETE -> executeForceDelete(template, preview);
        };
    }

    private DeletionResultDto executeSoftDelete(TestTemplate template) {
        log.info("Executing soft delete for template: {}", template.getId());

        template.softDelete(null); // TODO: Pass current user from security context
        templateRepository.save(template);

        return DeletionResultDto.softDeleted(template.getId(), template.getDeletedAt());
    }

    private DeletionResultDto executeArchiveAndCleanup(TestTemplate template) {
        log.info("Executing archive and cleanup for template: {}", template.getId());

        // Archive the template
        if (template.getStatus() == TemplateStatus.PUBLISHED) {
            template.archive();
        }
        template.softDelete(null);
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

        return new DeletionResultDto(
            template.getId(),
            DeletionMode.ARCHIVE_AND_CLEANUP,
            true,
            "Template archived and incomplete sessions cleaned up.",
            template.getDeletedAt(),
            sessionsDeleted, 0, answersDeleted, 0, 0, 0,
            true
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
    @Transactional
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
```

### 3.5 Repository Additions

#### TestActivityEventRepository - Add bulk delete

```java
// Add to TestActivityEventRepository

/**
 * Delete all activity events for a template.
 * @return Count of deleted events
 */
@Modifying
@Query("DELETE FROM TestActivityEvent e WHERE e.templateId = :templateId")
int deleteByTemplateId(@Param("templateId") UUID templateId);
```

#### TemplateShareRepository - Add bulk delete

```java
// Add to TemplateShareRepository

/**
 * Delete all shares for a template.
 * @return Count of deleted shares
 */
@Modifying
@Query("DELETE FROM TemplateShare s WHERE s.template.id = :templateId")
int deleteByTemplateId(@Param("templateId") UUID templateId);
```

#### TemplateShareLinkRepository - Add bulk delete

```java
// Add to TemplateShareLinkRepository

/**
 * Delete all share links for a template.
 * @return Count of deleted links
 */
@Modifying
@Query("DELETE FROM TemplateShareLink l WHERE l.template.id = :templateId")
int deleteByTemplateId(@Param("templateId") UUID templateId);
```

---

## 4. Session Cleanup Mechanism

### 4.1 Configuration Properties

```yaml
# application.yml
skillsoft:
  session:
    cleanup:
      enabled: true
      cron: "0 0 3 * * ?"  # 3 AM daily
      stale-hours: 24       # Sessions inactive for 24h
      delete-empty-after-days: 7  # Delete empty sessions after 7 days
```

### 4.2 Configuration Class

```java
package app.skillsoft.assessmentbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "skillsoft.session.cleanup")
public class SessionCleanupConfiguration {

    private boolean enabled = true;
    private String cron = "0 0 3 * * ?";
    private int staleHours = 24;
    private int deleteEmptyAfterDays = 7;

    // Getters and setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getCron() { return cron; }
    public void setCron(String cron) { this.cron = cron; }

    public int getStaleHours() { return staleHours; }
    public void setStaleHours(int staleHours) { this.staleHours = staleHours; }

    public int getDeleteEmptyAfterDays() { return deleteEmptyAfterDays; }
    public void setDeleteEmptyAfterDays(int days) { this.deleteEmptyAfterDays = days; }
}
```

### 4.3 Enable Scheduling Configuration

```java
package app.skillsoft.assessmentbackend.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration to enable Spring's scheduled task execution.
 * Can be disabled via property: skillsoft.scheduling.enabled=false
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(
    name = "skillsoft.scheduling.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class SchedulingConfig {
    // Enables @Scheduled annotation processing
}
```

### 4.4 Session Cleanup Job

```java
package app.skillsoft.assessmentbackend.services.cleanup;

import app.skillsoft.assessmentbackend.config.SessionCleanupConfiguration;
import app.skillsoft.assessmentbackend.domain.entities.SessionStatus;
import app.skillsoft.assessmentbackend.domain.entities.TestSession;
import app.skillsoft.assessmentbackend.repository.TestSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job to cleanup stale and abandoned test sessions.
 *
 * Targets:
 * 1. IN_PROGRESS sessions inactive for > configured hours -> mark as ABANDONED
 * 2. NOT_STARTED sessions older than configured hours -> mark as ABANDONED
 * 3. ABANDONED sessions with no answers older than configured days -> delete
 */
@Service
public class SessionCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(SessionCleanupJob.class);

    private final TestSessionRepository sessionRepository;
    private final SessionCleanupConfiguration config;

    public SessionCleanupJob(
            TestSessionRepository sessionRepository,
            SessionCleanupConfiguration config) {
        this.sessionRepository = sessionRepository;
        this.config = config;
    }

    /**
     * Main cleanup job running on configured schedule.
     * Default: 3 AM daily
     */
    @Scheduled(cron = "${skillsoft.session.cleanup.cron:0 0 3 * * ?}")
    @Transactional
    public void cleanupStaleSessions() {
        if (!config.isEnabled()) {
            log.debug("Session cleanup is disabled, skipping");
            return;
        }

        log.info("Starting session cleanup job");
        var startTime = System.currentTimeMillis();

        try {
            CleanupResult result = new CleanupResult();

            // Phase 1: Mark stale IN_PROGRESS sessions as ABANDONED
            result.inProgressAbandoned = abandonStaleInProgressSessions();

            // Phase 2: Mark old NOT_STARTED sessions as ABANDONED
            result.notStartedAbandoned = abandonOldNotStartedSessions();

            // Phase 3: Delete empty abandoned sessions (optional)
            result.emptySessionsDeleted = deleteEmptyAbandonedSessions();

            var duration = System.currentTimeMillis() - startTime;
            log.info("Session cleanup completed in {}ms: {} IN_PROGRESS abandoned, " +
                    "{} NOT_STARTED abandoned, {} empty sessions deleted",
                    duration, result.inProgressAbandoned,
                    result.notStartedAbandoned, result.emptySessionsDeleted);

        } catch (Exception e) {
            log.error("Session cleanup job failed", e);
        }
    }

    /**
     * Find and abandon IN_PROGRESS sessions that have been inactive too long.
     */
    private int abandonStaleInProgressSessions() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(config.getStaleHours());

        List<TestSession> staleSessions = sessionRepository.findStaleSessions(
                SessionStatus.IN_PROGRESS, cutoffTime);

        int count = 0;
        for (TestSession session : staleSessions) {
            try {
                session.abandon();
                sessionRepository.save(session);
                count++;
                log.debug("Abandoned stale IN_PROGRESS session: {}", session.getId());
            } catch (Exception e) {
                log.warn("Failed to abandon session {}: {}", session.getId(), e.getMessage());
            }
        }

        return count;
    }

    /**
     * Find and abandon NOT_STARTED sessions that are too old.
     */
    private int abandonOldNotStartedSessions() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(config.getStaleHours());

        List<TestSession> oldSessions = sessionRepository.findStaleSessions(
                SessionStatus.NOT_STARTED, cutoffTime);

        int count = 0;
        for (TestSession session : oldSessions) {
            try {
                session.setStatus(SessionStatus.ABANDONED);
                session.setCompletedAt(LocalDateTime.now());
                sessionRepository.save(session);
                count++;
                log.debug("Abandoned old NOT_STARTED session: {}", session.getId());
            } catch (Exception e) {
                log.warn("Failed to abandon session {}: {}", session.getId(), e.getMessage());
            }
        }

        return count;
    }

    /**
     * Delete abandoned sessions that have no answers and are old enough.
     * This is an aggressive cleanup for sessions that add no value.
     */
    private int deleteEmptyAbandonedSessions() {
        if (config.getDeleteEmptyAfterDays() <= 0) {
            return 0; // Feature disabled
        }

        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(config.getDeleteEmptyAfterDays());

        List<TestSession> abandonedSessions = sessionRepository.findStaleSessions(
                SessionStatus.ABANDONED, cutoffTime);

        int count = 0;
        for (TestSession session : abandonedSessions) {
            // Only delete if session has no answers and no result
            if ((session.getAnswers() == null || session.getAnswers().isEmpty())
                    && session.getResult() == null) {
                try {
                    sessionRepository.delete(session);
                    count++;
                    log.debug("Deleted empty abandoned session: {}", session.getId());
                } catch (Exception e) {
                    log.warn("Failed to delete empty session {}: {}", session.getId(), e.getMessage());
                }
            }
        }

        return count;
    }

    /**
     * Manual trigger for cleanup (can be called from admin API).
     */
    @Transactional
    public CleanupResult triggerManualCleanup() {
        log.info("Manual session cleanup triggered");

        CleanupResult result = new CleanupResult();
        result.inProgressAbandoned = abandonStaleInProgressSessions();
        result.notStartedAbandoned = abandonOldNotStartedSessions();
        result.emptySessionsDeleted = deleteEmptyAbandonedSessions();

        return result;
    }

    /**
     * Result record for cleanup operations.
     */
    public static class CleanupResult {
        public int inProgressAbandoned = 0;
        public int notStartedAbandoned = 0;
        public int emptySessionsDeleted = 0;

        public int getTotalProcessed() {
            return inProgressAbandoned + notStartedAbandoned + emptySessionsDeleted;
        }
    }
}
```

### 4.5 Repository Query for Stale Sessions

```java
// Already exists in TestSessionRepository, verify it works:

/**
 * Find sessions that are in progress but inactive for too long (for cleanup/timeout)
 */
@Query("SELECT s FROM TestSession s WHERE s.status = :status AND s.lastActivityAt < :cutoffTime")
List<TestSession> findStaleSessions(
        @Param("status") SessionStatus status,
        @Param("cutoffTime") LocalDateTime cutoffTime);
```

---

## 5. Database Migration

### 5.1 Flyway Migration Script

```sql
-- V20260111__add_soft_delete_to_templates.sql

-- Add soft delete columns to test_templates
ALTER TABLE test_templates
ADD COLUMN deleted_at TIMESTAMP NULL,
ADD COLUMN deleted_by_id UUID NULL;

-- Add FK constraint for deleted_by
ALTER TABLE test_templates
ADD CONSTRAINT fk_template_deleted_by
FOREIGN KEY (deleted_by_id) REFERENCES users(id);

-- Index for efficient filtering of non-deleted templates
CREATE INDEX idx_templates_deleted_at ON test_templates(deleted_at)
WHERE deleted_at IS NULL;

-- Index for cleanup job queries
CREATE INDEX idx_sessions_last_activity_status
ON test_sessions(last_activity_at, status)
WHERE status IN ('IN_PROGRESS', 'NOT_STARTED');

-- Comment for documentation
COMMENT ON COLUMN test_templates.deleted_at IS
'Soft delete timestamp. NULL means template is active.';
```

---

## 6. API Endpoints

### 6.1 TemplateDeletionController

```java
package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.dto.DeletionPreviewDto;
import app.skillsoft.assessmentbackend.domain.dto.DeletionResultDto;
import app.skillsoft.assessmentbackend.domain.entities.DeletionMode;
import app.skillsoft.assessmentbackend.services.TemplateDeletionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/templates")
public class TemplateDeletionController {

    private final TemplateDeletionService deletionService;

    public TemplateDeletionController(TemplateDeletionService deletionService) {
        this.deletionService = deletionService;
    }

    /**
     * Preview deletion impact before confirming.
     */
    @GetMapping("/{templateId}/deletion-preview")
    public ResponseEntity<DeletionPreviewDto> previewDeletion(
            @PathVariable UUID templateId) {
        return ResponseEntity.ok(deletionService.previewDeletion(templateId));
    }

    /**
     * Delete a template with specified mode.
     */
    @DeleteMapping("/{templateId}")
    public ResponseEntity<DeletionResultDto> deleteTemplate(
            @PathVariable UUID templateId,
            @RequestParam(defaultValue = "SOFT_DELETE") DeletionMode mode,
            @RequestParam(defaultValue = "false") boolean confirmed) {
        return ResponseEntity.ok(deletionService.deleteTemplate(templateId, mode, confirmed));
    }

    /**
     * Restore a soft-deleted template.
     */
    @PostMapping("/{templateId}/restore")
    public ResponseEntity<Void> restoreTemplate(@PathVariable UUID templateId) {
        boolean restored = deletionService.restoreTemplate(templateId);
        return restored ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
}
```

---

## 7. Testing Strategy

### 7.1 Unit Tests

```java
package app.skillsoft.assessmentbackend.services.impl;

import app.skillsoft.assessmentbackend.domain.dto.DeletionPreviewDto;
import app.skillsoft.assessmentbackend.domain.dto.DeletionResultDto;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TemplateDeletionServiceImplTest {

    @Mock
    private TestTemplateRepository templateRepository;
    @Mock
    private TestSessionRepository sessionRepository;
    @Mock
    private TestResultRepository resultRepository;
    @Mock
    private TestAnswerRepository answerRepository;
    @Mock
    private TemplateShareRepository shareRepository;
    @Mock
    private TemplateShareLinkRepository shareLinkRepository;
    @Mock
    private TestActivityEventRepository activityEventRepository;

    @InjectMocks
    private TemplateDeletionServiceImpl deletionService;

    private TestTemplate draftTemplate;
    private TestTemplate publishedTemplate;
    private UUID templateId;

    @BeforeEach
    void setUp() {
        templateId = UUID.randomUUID();

        draftTemplate = new TestTemplate();
        draftTemplate.setId(templateId);
        draftTemplate.setName("Draft Template");
        draftTemplate.setStatus(TemplateStatus.DRAFT);

        publishedTemplate = new TestTemplate();
        publishedTemplate.setId(templateId);
        publishedTemplate.setName("Published Template");
        publishedTemplate.setStatus(TemplateStatus.PUBLISHED);
    }

    @Nested
    @DisplayName("Preview Deletion Tests")
    class PreviewDeletionTests {

        @Test
        @DisplayName("Should recommend FORCE_DELETE for draft template with no sessions")
        void previewDraftTemplateRecommendsForceDelete() {
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(draftTemplate));
            when(sessionRepository.findByTemplate_Id(templateId)).thenReturn(Collections.emptyList());
            when(resultRepository.countResultsByTemplateId(templateId)).thenReturn(0L);
            when(shareRepository.countActiveByTemplateId(templateId)).thenReturn(0L);
            when(shareLinkRepository.countActiveByTemplateId(templateId)).thenReturn(0L);
            when(activityEventRepository.countByTemplateId(templateId)).thenReturn(0L);

            DeletionPreviewDto preview = deletionService.previewDeletion(templateId);

            assertThat(preview.recommendedMode()).isEqualTo("FORCE_DELETE");
            assertThat(preview.requiresConfirmation()).isFalse();
            assertThat(preview.totalSessions()).isZero();
        }

        @Test
        @DisplayName("Should recommend SOFT_DELETE for template with completed sessions")
        void previewTemplateWithSessionsRecommendsSoftDelete() {
            TestSession completedSession = new TestSession(publishedTemplate, "user123");
            completedSession.setId(UUID.randomUUID());
            completedSession.setStatus(SessionStatus.COMPLETED);

            when(templateRepository.findById(templateId)).thenReturn(Optional.of(publishedTemplate));
            when(sessionRepository.findByTemplate_Id(templateId)).thenReturn(List.of(completedSession));
            when(resultRepository.countResultsByTemplateId(templateId)).thenReturn(1L);
            when(shareRepository.countActiveByTemplateId(templateId)).thenReturn(0L);
            when(shareLinkRepository.countActiveByTemplateId(templateId)).thenReturn(0L);
            when(activityEventRepository.countByTemplateId(templateId)).thenReturn(0L);

            DeletionPreviewDto preview = deletionService.previewDeletion(templateId);

            assertThat(preview.recommendedMode()).isEqualTo("SOFT_DELETE");
            assertThat(preview.requiresConfirmation()).isTrue();
            assertThat(preview.completedSessions()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Soft Delete Tests")
    class SoftDeleteTests {

        @Test
        @DisplayName("Should set deletedAt timestamp on soft delete")
        void softDeleteSetsTimestamp() {
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(publishedTemplate));
            when(sessionRepository.findByTemplate_Id(templateId)).thenReturn(Collections.emptyList());
            when(resultRepository.countResultsByTemplateId(templateId)).thenReturn(0L);
            when(shareRepository.countActiveByTemplateId(templateId)).thenReturn(0L);
            when(shareLinkRepository.countActiveByTemplateId(templateId)).thenReturn(0L);
            when(activityEventRepository.countByTemplateId(templateId)).thenReturn(0L);
            when(templateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DeletionResultDto result = deletionService.deleteTemplate(
                    templateId, DeletionMode.SOFT_DELETE, false);

            assertThat(result.success()).isTrue();
            assertThat(result.mode()).isEqualTo(DeletionMode.SOFT_DELETE);
            assertThat(result.canRestore()).isTrue();
            verify(templateRepository).save(argThat(t -> t.getDeletedAt() != null));
        }
    }

    @Nested
    @DisplayName("Force Delete Tests")
    class ForceDeleteTests {

        @Test
        @DisplayName("Should delete all related entities in correct order")
        void forceDeleteRemovesAllData() {
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(draftTemplate));
            when(sessionRepository.findByTemplate_Id(templateId)).thenReturn(Collections.emptyList());
            when(resultRepository.countResultsByTemplateId(templateId)).thenReturn(0L);
            when(shareRepository.countActiveByTemplateId(templateId)).thenReturn(0L);
            when(shareLinkRepository.countActiveByTemplateId(templateId)).thenReturn(0L);
            when(activityEventRepository.countByTemplateId(templateId)).thenReturn(0L);
            when(activityEventRepository.deleteByTemplateId(templateId)).thenReturn(5);
            when(shareLinkRepository.deleteByTemplateId(templateId)).thenReturn(2);
            when(shareRepository.deleteByTemplateId(templateId)).thenReturn(3);

            DeletionResultDto result = deletionService.deleteTemplate(
                    templateId, DeletionMode.FORCE_DELETE, true);

            assertThat(result.success()).isTrue();
            assertThat(result.mode()).isEqualTo(DeletionMode.FORCE_DELETE);
            assertThat(result.canRestore()).isFalse();

            // Verify deletion order
            var inOrder = inOrder(activityEventRepository, sessionRepository,
                    shareLinkRepository, shareRepository, templateRepository);
            inOrder.verify(activityEventRepository).deleteByTemplateId(templateId);
            inOrder.verify(shareLinkRepository).deleteByTemplateId(templateId);
            inOrder.verify(shareRepository).deleteByTemplateId(templateId);
            inOrder.verify(templateRepository).delete(draftTemplate);
        }

        @Test
        @DisplayName("Should throw exception if confirmation not provided for risky delete")
        void forceDeleteRequiresConfirmation() {
            TestSession activeSession = new TestSession(publishedTemplate, "user123");
            activeSession.setId(UUID.randomUUID());
            activeSession.setStatus(SessionStatus.IN_PROGRESS);

            when(templateRepository.findById(templateId)).thenReturn(Optional.of(publishedTemplate));
            when(sessionRepository.findByTemplate_Id(templateId)).thenReturn(List.of(activeSession));
            when(resultRepository.countResultsByTemplateId(templateId)).thenReturn(0L);
            when(shareRepository.countActiveByTemplateId(templateId)).thenReturn(0L);
            when(shareLinkRepository.countActiveByTemplateId(templateId)).thenReturn(0L);
            when(activityEventRepository.countByTemplateId(templateId)).thenReturn(0L);

            assertThatThrownBy(() ->
                deletionService.deleteTemplate(templateId, DeletionMode.FORCE_DELETE, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("requires explicit confirmation");
        }
    }
}
```

---

## 8. Summary

### 8.1 Files to Create/Modify

| File | Action | Description |
|------|--------|-------------|
| `DeletionMode.java` | CREATE | Enum for deletion modes |
| `DeletionPreviewDto.java` | CREATE | Preview DTO |
| `DeletionResultDto.java` | CREATE | Result DTO |
| `TemplateDeletionService.java` | CREATE | Service interface |
| `TemplateDeletionServiceImpl.java` | CREATE | Service implementation |
| `SessionCleanupJob.java` | CREATE | Scheduled cleanup job |
| `SessionCleanupConfiguration.java` | CREATE | Configuration properties |
| `SchedulingConfig.java` | CREATE | Enable scheduling |
| `TemplateDeletionController.java` | CREATE | REST endpoints |
| `TestTemplate.java` | MODIFY | Add soft delete fields |
| `TestActivityEventRepository.java` | MODIFY | Add deleteByTemplateId |
| `TemplateShareRepository.java` | MODIFY | Add deleteByTemplateId |
| `TemplateShareLinkRepository.java` | MODIFY | Add deleteByTemplateId |
| `V20260111__add_soft_delete.sql` | CREATE | DB migration |

### 8.2 Estimated Effort

| Task | Effort |
|------|--------|
| Entity/DTO changes | 2 hours |
| Deletion service | 4 hours |
| Cleanup job | 2 hours |
| Repository additions | 1 hour |
| API endpoints | 1 hour |
| DB migration | 0.5 hours |
| Unit tests | 3 hours |
| Integration tests | 2 hours |
| **Total** | **~15.5 hours** |

---

## 9. Appendix: State Machine for Deletion

```
                    ┌─────────────────────────────┐
                    │        START                │
                    └──────────────┬──────────────┘
                                   │
                                   ▼
                    ┌─────────────────────────────┐
                    │   Validate Template Exists  │
                    └──────────────┬──────────────┘
                                   │
                    ┌──────────────┴──────────────┐
                    │                              │
                    ▼                              ▼
             ┌──────────┐                 ┌────────────────┐
             │  DRAFT   │                 │ PUBLISHED/     │
             │ (no dep) │                 │ ARCHIVED       │
             └────┬─────┘                 └───────┬────────┘
                  │                               │
                  ▼                               ▼
        ┌─────────────────┐            ┌─────────────────────┐
        │  FORCE_DELETE   │            │  Check Dependencies │
        │  (immediate)    │            └──────────┬──────────┘
        └─────────────────┘                       │
                                    ┌─────────────┼─────────────┐
                                    │             │             │
                                    ▼             ▼             ▼
                             ┌──────────┐  ┌──────────┐  ┌──────────┐
                             │ Active   │  │Completed │  │ No       │
                             │ Sessions │  │ Sessions │  │ Sessions │
                             └────┬─────┘  └────┬─────┘  └────┬─────┘
                                  │             │             │
                                  ▼             ▼             ▼
                          ┌──────────────┐ ┌──────────┐  ┌──────────┐
                          │WARN + Confirm│ │SOFT_DEL  │  │FORCE_DEL │
                          │required      │ │recommend │  │ allowed  │
                          └──────────────┘ └──────────┘  └──────────┘
```
