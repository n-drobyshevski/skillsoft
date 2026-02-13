package app.skillsoft.assessmentbackend.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.type.format.jackson.JacksonJsonFormatMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Configuration to ensure Hibernate uses a properly configured ObjectMapper for JSON/JSONB columns.
 * 
 * This is critical for proper serialization of JSONB fields like `standard_codes` which use
 * @JsonNaming(SnakeCaseStrategy.class) annotations. Without this, Hibernate would use a default
 * ObjectMapper that ignores these annotations, causing camelCase to be stored instead of snake_case.
 * 
 * We set a global SNAKE_CASE property naming strategy to ensure all JSONB fields are stored
 * with snake_case keys, matching the @JsonNaming annotations on our DTOs.
 */
@Configuration
public class HibernateJsonConfig {

    private static final Logger logger = LoggerFactory.getLogger(HibernateJsonConfig.class);

    @PostConstruct
    public void init() {
        logger.info("HibernateJsonConfig initialized - snake_case JSON serialization enabled for Hibernate");
    }

    /**
     * Creates a dedicated ObjectMapper for Hibernate JSONB serialization.
     * 
     * IMPORTANT: We do NOT set a global SNAKE_CASE naming strategy here because
     * the DTOs already have @JsonNaming(SnakeCaseStrategy.class) annotations.
     * Setting it globally would cause a conflict where Jackson looks for 
     * already-converted field names (e.g., 'big_five') but finds Java record
     * accessors with original names (e.g., 'bigFive()'), causing fields to be lost.
     */
    private ObjectMapper createHibernateObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // DO NOT set PropertyNamingStrategy here - DTOs have @JsonNaming annotations
        // mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        
        // Register Java 8 date/time module
        mapper.registerModule(new JavaTimeModule());
        
        // Don't write dates as timestamps
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Don't fail on unknown properties (important for schema evolution)
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        
        // Exclude null values from JSON
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        
        // Don't fail on empty beans
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        
        // Find and register any additional modules
        mapper.findAndRegisterModules();
        
        logger.debug("Created Hibernate ObjectMapper (using @JsonNaming annotations from DTOs)");
        
        return mapper;
    }

    /**
     * Customizes Hibernate to use our configured ObjectMapper for JSON serialization.
     * This ensures JSONB columns properly use snake_case property names.
     */
    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
        logger.info("Creating HibernatePropertiesCustomizer with custom JSON format mapper");
        return properties -> {
            ObjectMapper hibernateMapper = createHibernateObjectMapper();
            properties.put(
                AvailableSettings.JSON_FORMAT_MAPPER,
                new JacksonJsonFormatMapper(hibernateMapper)
            );
        };
    }
}
