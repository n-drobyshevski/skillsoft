package app.skillsoft.assessmentbackend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Test configuration that provides utilities for working with JSONB data in tests.
 * Since Hibernate's JSON deserialization has issues with H2, we provide alternative
 * verification methods using native SQL queries.
 */
@TestConfiguration
public class TestHibernateConfig {

    @Bean
    public JsonbTestHelper jsonbTestHelper(DataSource dataSource, ObjectMapper objectMapper) {
        return new JsonbTestHelper(new JdbcTemplate(dataSource), objectMapper);
    }
}