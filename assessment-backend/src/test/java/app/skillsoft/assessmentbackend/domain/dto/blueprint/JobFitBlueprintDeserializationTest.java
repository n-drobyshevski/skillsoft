package app.skillsoft.assessmentbackend.domain.dto.blueprint;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests that Jackson correctly deserializes competencyIds into JobFitBlueprint
 * when received via the polymorphic TestBlueprintDto type hierarchy.
 *
 * This test reproduces the exact JSON payload the frontend sends to
 * POST /api/v1/tests/templates/simulate and verifies competencyIds is populated.
 */
@DisplayName("JobFitBlueprint Jackson Deserialization")
class JobFitBlueprintDeserializationTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        // Match production ObjectMapper config from JsonConfig.java
        mapper = new ObjectMapper();
        mapper.registerModule(new ParameterNamesModule());
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    @Test
    @DisplayName("should deserialize competencyIds when blueprint type is JOB_FIT")
    void shouldDeserializeCompetencyIds() throws Exception {
        UUID id1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID id2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID id3 = UUID.fromString("33333333-3333-3333-3333-333333333333");

        String json = """
            {
                "strategy": "JOB_FIT",
                "onetSocCode": "15-1252.00",
                "strictnessLevel": 50,
                "competencyIds": [
                    "11111111-1111-1111-1111-111111111111",
                    "22222222-2222-2222-2222-222222222222",
                    "33333333-3333-3333-3333-333333333333"
                ]
            }
            """;

        TestBlueprintDto result = mapper.readValue(json, TestBlueprintDto.class);

        assertThat(result).isInstanceOf(JobFitBlueprint.class);
        JobFitBlueprint jobFit = (JobFitBlueprint) result;
        assertThat(jobFit.getCompetencyIds())
            .as("competencyIds must be deserialized from JSON")
            .containsExactly(id1, id2, id3);
        assertThat(jobFit.getOnetSocCode()).isEqualTo("15-1252.00");
        assertThat(jobFit.getStrictnessLevel()).isEqualTo(50);
    }

    @Test
    @DisplayName("should deserialize competencyIds as empty list when not provided")
    void shouldDefaultToEmptyWhenNotProvided() throws Exception {
        String json = """
            {
                "strategy": "JOB_FIT",
                "onetSocCode": "15-1252.00",
                "strictnessLevel": 50
            }
            """;

        TestBlueprintDto result = mapper.readValue(json, TestBlueprintDto.class);

        assertThat(result).isInstanceOf(JobFitBlueprint.class);
        JobFitBlueprint jobFit = (JobFitBlueprint) result;
        assertThat(jobFit.getCompetencyIds())
            .as("competencyIds should default to empty list when not in JSON")
            .isEmpty();
    }

    @Test
    @DisplayName("should ignore unknown properties from other blueprint types")
    void shouldIgnoreUnknownProperties() throws Exception {
        // Frontend sends ALL blueprint properties regardless of strategy type
        String json = """
            {
                "strategy": "JOB_FIT",
                "onetSocCode": "15-1252.00",
                "strictnessLevel": 50,
                "competencyIds": ["11111111-1111-1111-1111-111111111111"],
                "includeBigFive": true,
                "teamId": "some-team-id",
                "saturationThreshold": 0.8
            }
            """;

        TestBlueprintDto result = mapper.readValue(json, TestBlueprintDto.class);

        assertThat(result).isInstanceOf(JobFitBlueprint.class);
        JobFitBlueprint jobFit = (JobFitBlueprint) result;
        assertThat(jobFit.getCompetencyIds()).hasSize(1);
    }

    @Test
    @DisplayName("should deserialize full SimulateRequest-like payload with nested blueprint")
    void shouldDeserializeNestedInSimulateRequest() throws Exception {
        // This mirrors the exact JSON the frontend sends
        String json = """
            {
                "templateId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
                "blueprint": {
                    "strategy": "JOB_FIT",
                    "onetSocCode": "15-1252.00",
                    "strictnessLevel": 50,
                    "competencyIds": [
                        "11111111-1111-1111-1111-111111111111",
                        "22222222-2222-2222-2222-222222222222",
                        "33333333-3333-3333-3333-333333333333",
                        "44444444-4444-4444-4444-444444444444",
                        "55555555-5555-5555-5555-555555555555"
                    ],
                    "adaptivity": null
                },
                "profile": "RANDOM_GUESSER",
                "abilityLevel": 50
            }
            """;

        // Deserialize the nested blueprint directly
        var tree = mapper.readTree(json);
        var blueprintNode = tree.get("blueprint");
        TestBlueprintDto blueprint = mapper.treeToValue(blueprintNode, TestBlueprintDto.class);

        assertThat(blueprint).isInstanceOf(JobFitBlueprint.class);
        JobFitBlueprint jobFit = (JobFitBlueprint) blueprint;
        assertThat(jobFit.getCompetencyIds())
            .as("competencyIds must survive nested deserialization")
            .hasSize(5);
    }

    @Test
    @DisplayName("should round-trip serialize and deserialize competencyIds")
    void shouldRoundTripCompetencyIds() throws Exception {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        JobFitBlueprint original = new JobFitBlueprint();
        original.setOnetSocCode("15-1252.00");
        original.setStrictnessLevel(50);
        original.setCompetencyIds(List.of(id1, id2));

        String json = mapper.writeValueAsString(original);
        TestBlueprintDto deserialized = mapper.readValue(json, TestBlueprintDto.class);

        assertThat(deserialized).isInstanceOf(JobFitBlueprint.class);
        JobFitBlueprint jobFit = (JobFitBlueprint) deserialized;
        assertThat(jobFit.getCompetencyIds()).containsExactly(id1, id2);
    }
}
