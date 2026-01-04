package app.skillsoft.assessmentbackend.services;

import app.skillsoft.assessmentbackend.domain.dto.activity.ActivityFilterParams;
import app.skillsoft.assessmentbackend.domain.dto.activity.TemplateActivityStatsDto;
import app.skillsoft.assessmentbackend.domain.dto.activity.TestActivityDto;
import app.skillsoft.assessmentbackend.domain.entities.TestSession;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for activity tracking operations.
 * Provides methods for dashboard widgets, template activity pages,
 * and audit event recording.
 */
public interface ActivityTrackingService {

    /**
     * Get recent activity for dashboard widget.
     * Enriches with user names from User entity.
     *
     * @param limit maximum number of activities to return
     * @return list of recent activities ordered by time (most recent first)
     */
    List<TestActivityDto> getRecentActivity(int limit);

    /**
     * Get activity for a specific template with filtering and pagination.
     *
     * @param templateId the template to get activity for
     * @param params     filter parameters (status, passed, date range, pagination)
     * @return paginated list of activities
     */
    Page<TestActivityDto> getTemplateActivity(UUID templateId, ActivityFilterParams params);

    /**
     * Get aggregated statistics for template activity.
     *
     * @param templateId the template to get stats for
     * @return aggregated activity statistics
     */
    TemplateActivityStatsDto getTemplateActivityStats(UUID templateId);

    /**
     * Record a session started event.
     * Called when a test session transitions to IN_PROGRESS.
     *
     * @param session the test session that started
     */
    void recordSessionStarted(TestSession session);

    /**
     * Record a session completed event.
     * Called when a test session transitions to COMPLETED.
     *
     * @param session the test session that completed
     * @param score   the final score (optional)
     * @param passed  whether the candidate passed (optional)
     */
    void recordSessionCompleted(TestSession session, Double score, Boolean passed);

    /**
     * Record a session abandoned event.
     * Called when a test session transitions to ABANDONED.
     *
     * @param session the test session that was abandoned
     */
    void recordSessionAbandoned(TestSession session);

    /**
     * Record a session timed out event.
     * Called when a test session transitions to TIMED_OUT.
     *
     * @param session the test session that timed out
     */
    void recordSessionTimedOut(TestSession session);
}
