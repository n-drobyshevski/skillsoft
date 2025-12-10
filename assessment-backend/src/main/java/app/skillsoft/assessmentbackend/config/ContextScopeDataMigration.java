package app.skillsoft.assessmentbackend.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Data migration for context_scope column in behavioral_indicators table.
 * This migration runs automatically on application startup to ensure all existing
 * behavioral indicators have a valid context_scope value.
 * 
 * After all production instances have been migrated, this class can be safely removed.
 */
@Component
public class ContextScopeDataMigration {

    private static final Logger logger = LoggerFactory.getLogger(ContextScopeDataMigration.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void migrateContextScope() {
        try {
            logger.info("Starting context_scope data migration...");
            
            // Check if column exists first
            Integer columnExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_name = 'behavioral_indicators' " +
                "AND column_name = 'context_scope'",
                Integer.class
            );

            if (columnExists == null || columnExists == 0) {
                logger.info("context_scope column does not exist yet. Migration will run after schema update.");
                return;
            }

            // Count NULL values before migration
            Integer nullCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM behavioral_indicators WHERE context_scope IS NULL",
                Integer.class
            );

            if (nullCount != null && nullCount > 0) {
                logger.info("Found {} behavioral indicators with NULL context_scope. Migrating to UNIVERSAL...", nullCount);
                
                // Update NULL values to UNIVERSAL (default for backward compatibility)
                int updatedRows = jdbcTemplate.update(
                    "UPDATE behavioral_indicators SET context_scope = 'UNIVERSAL' WHERE context_scope IS NULL"
                );

                logger.info("Successfully migrated {} behavioral indicators to UNIVERSAL context scope.", updatedRows);
            } else {
                logger.info("All behavioral indicators already have context_scope values. No migration needed.");
            }

            // Log summary statistics
            Integer totalCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM behavioral_indicators",
                Integer.class
            );
            
            Integer universalCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM behavioral_indicators WHERE context_scope = 'UNIVERSAL'",
                Integer.class
            );

            logger.info("Context scope migration complete. Total indicators: {}, UNIVERSAL: {}", 
                totalCount, universalCount);

        } catch (Exception e) {
            logger.error("Error during context_scope data migration. This is expected if the table doesn't exist yet.", e.getMessage());
            // Don't fail application startup - the column will be created by Hibernate
        }
    }
}
