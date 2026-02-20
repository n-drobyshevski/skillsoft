package app.skillsoft.assessmentbackend.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * ShedLock configuration for distributed lock on scheduled tasks.
 *
 * Ensures that nightly batch jobs (e.g., PsychometricAuditJob) execute on only
 * one application instance in multi-instance deployments. Uses a database-backed
 * lock provider (JDBC) aligned with the existing PostgreSQL data source.
 *
 * Default lock-at-most: 30 minutes (PT30M) — generous for nightly jobs that may
 * process large datasets. Individual methods can override via @SchedulerLock.
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M")
public class ShedLockConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new org.springframework.jdbc.core.JdbcTemplate(dataSource))
                        .usingDbTime()
                        .build()
        );
    }
}
