package app.skillsoft.assessmentbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for session cleanup job.
 * Configures when and how stale sessions are cleaned up.
 *
 * <p>Properties prefix: {@code skillsoft.session.cleanup}
 *
 * <p>Example configuration:
 * <pre>
 * skillsoft.session.cleanup.enabled=true
 * skillsoft.session.cleanup.cron=0 0 3 * * ?
 * skillsoft.session.cleanup.stale-hours=24
 * skillsoft.session.cleanup.delete-empty-after-days=7
 * </pre>
 */
@Configuration
@ConfigurationProperties(prefix = "skillsoft.session.cleanup")
public class SessionCleanupProperties {

    /**
     * Whether session cleanup is enabled.
     * Default: true
     */
    private boolean enabled = true;

    /**
     * Cron expression for cleanup schedule.
     * Default: "0 0 3 * * ?" (3 AM daily)
     */
    private String cron = "0 0 3 * * ?";

    /**
     * Hours after which inactive sessions are considered stale.
     * IN_PROGRESS sessions inactive for longer than this are marked as ABANDONED.
     * NOT_STARTED sessions older than this are marked as ABANDONED.
     * Default: 24 hours
     */
    private int staleHours = 24;

    /**
     * Days after which empty abandoned sessions are deleted.
     * Set to 0 or negative to disable automatic deletion.
     * Only sessions with no answers and no results are deleted.
     * Default: 7 days
     */
    private int deleteEmptyAfterDays = 7;

    // Getters and setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public int getStaleHours() {
        return staleHours;
    }

    public void setStaleHours(int staleHours) {
        this.staleHours = staleHours;
    }

    public int getDeleteEmptyAfterDays() {
        return deleteEmptyAfterDays;
    }

    public void setDeleteEmptyAfterDays(int deleteEmptyAfterDays) {
        this.deleteEmptyAfterDays = deleteEmptyAfterDays;
    }
}
