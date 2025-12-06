package app.skillsoft.assessmentbackend.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
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
 * - Uses camelCase for all JSON properties (matching Java record field names)
 * - Frontend sends camelCase, backend responds with camelCase
 * - Excludes null values from JSON output
 * - Properly handles Java 8+ date/time types
 * 
 * NOTE: We intentionally do NOT use SNAKE_CASE naming strategy because
 * it doesn't work reliably with nested Java records. Instead, both frontend
 * and backend use camelCase consistently.
 */
@Configuration
public class JsonConfig {

    /**
     * Customizes the Jackson ObjectMapper builder used by Spring.
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> {
            builder.createXmlMapper(false);
            // ParameterNamesModule for better record support
            builder.modules(new JavaTimeModule(), new ParameterNamesModule());
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
        // ParameterNamesModule for better record support
        mapper.registerModule(new ParameterNamesModule());
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