package app.skillsoft.assessmentbackend.config;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for JSON handling in the application
 * Note: Basic configuration is already handled in application.properties
 */
@Configuration
public class JsonConfig {

    /**
     * Additional JSON configuration customizations
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> {
            // Additional customizations can be added here if needed
            builder.createXmlMapper(false);
        };
    }
}