package app.skillsoft.assessmentbackend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration for proper Jackson JSON handling in H2 database tests
 */
@TestConfiguration
public class TestJacksonConfig {

    /**
     * Customizes the ObjectMapper for test environment.
     * Note: We don't use @Primary here to avoid conflicts with main JsonConfig.
     * The main JsonConfig's objectMapper will be used instead.
     */
    @Bean(name = "testObjectMapper")
    public ObjectMapper testObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Configure Jackson to handle JSON deserialization properly
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        return mapper;
    }
}