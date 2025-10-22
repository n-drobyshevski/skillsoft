package app.skillsoft.assessmentbackend.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;
import java.util.UUID;

/**
 * Helper class for testing JSONB functionality using native SQL queries.
 * This bypasses Hibernate's entity mapping issues with H2 JSONB deserialization.
 */
public class JsonbTestHelper {
    
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    
    public JsonbTestHelper(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Retrieves and deserializes JSONB standard_codes field directly from database
     */
    public Map<String, Object> getStandardCodesById(UUID competencyId) {
        try {
            String jsonString = jdbcTemplate.queryForObject(
                "SELECT standard_codes FROM competencies WHERE id = ?",
                String.class,
                competencyId
            );
            
            if (jsonString == null) {
                return null;
            }
            
            // Handle the case where H2 might be storing JSON as a string-wrapped value
            // First try to parse as is
            try {
                return objectMapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                // If that fails, try to check if it's a quoted JSON string and unwrap it
                if (jsonString.startsWith("\"") && jsonString.endsWith("\"")) {
                    // Remove the outer quotes and unescape
                    String unquoted = jsonString.substring(1, jsonString.length() - 1)
                            .replace("\\\"", "\"")
                            .replace("\\\\", "\\");
                    return objectMapper.readValue(unquoted, new TypeReference<Map<String, Object>>() {});
                }
                throw e;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve standard codes for competency " + competencyId, e);
        }
    }
    
    /**
     * Verifies that a competency exists in the database with the expected name
     */
    public boolean competencyExistsWithName(UUID competencyId, String expectedName) {
        try {
            String actualName = jdbcTemplate.queryForObject(
                "SELECT name FROM competencies WHERE id = ?",
                String.class,
                competencyId
            );
            return expectedName.equals(actualName);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Gets the count of competencies in the database
     */
    public int getCompetencyCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM competencies", Integer.class);
    }
    
    /**
     * Verifies JSONB content contains expected key-value pairs
     */
    public boolean jsonbContains(UUID competencyId, String jsonPath, Object expectedValue) {
        try {
            Map<String, Object> standardCodes = getStandardCodesById(competencyId);
            if (standardCodes == null) {
                return expectedValue == null;
            }
            
            // Navigate the JSON path (simplified for common use cases)
            String[] pathParts = jsonPath.split("\\.");
            Object current = standardCodes;
            
            for (String part : pathParts) {
                if (current instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) current;
                    current = map.get(part);
                } else {
                    return false;
                }
            }
            
            return expectedValue != null ? expectedValue.equals(current) : current == null;
        } catch (Exception e) {
            return false;
        }
    }
}