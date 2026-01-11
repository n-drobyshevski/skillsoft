package app.skillsoft.assessmentbackend.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration to enable Spring's scheduled task execution.
 * Enables @Scheduled annotation processing for background jobs like:
 * - Session cleanup (abandoning stale sessions after 24h)
 * - Share link expiration
 * - Other periodic maintenance tasks
 *
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
    // Configuration is handled by individual jobs using @ConfigurationProperties
}
