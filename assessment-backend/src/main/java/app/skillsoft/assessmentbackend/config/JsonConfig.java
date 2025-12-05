package app.skillsoft.assessmentbackend.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Global Jackson JSON configuration for the application.
 * 
 * Ensures consistent JSON serialization/deserialization behavior across:
 * - Spring MVC REST endpoints
 * - Hibernate JSONB columns (via HibernateJsonConfig)
 * 
 * Key settings:
 * - Respects @JsonNaming annotations on DTOs (e.g., SnakeCaseStrategy)
 * - Excludes null values from JSON output
 * - Properly handles Java 8+ date/time types
 */
@Configuration
public class JsonConfig {

    /**
     * Customizes the Jackson ObjectMapper builder used by Spring.
     * This ensures all @JsonNaming, @JsonInclude annotations are respected.
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> {
            builder.createXmlMapper(false);
            // Ensure modules are registered for proper date/time handling
            builder.modules(new JavaTimeModule());
            // Serialize dates as ISO strings, not timestamps
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            // Don't fail on unknown properties during deserialization
            builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            // Default inclusion: exclude null values
            builder.serializationInclusion(JsonInclude.Include.NON_NULL);
        };
    }

    /**
     * Creates a primary ObjectMapper bean that can be injected throughout the application.
     * This ensures consistent JSON handling everywhere.
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Register Java 8 date/time module
        mapper.registerModule(new JavaTimeModule());
        // Don't write dates as timestamps
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // Don't fail on unknown properties
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // Exclude null values
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        // Don't fail on empty beans
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        return mapper;
    }
}