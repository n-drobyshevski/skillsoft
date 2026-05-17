package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.dto.CompetencyDto;
import app.skillsoft.assessmentbackend.domain.dto.StandardCodesDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests to verify JSON deserialization of CompetencyDto with nested StandardCodesDto.
 * This tests the exact JSON structure sent by the frontend (camelCase).
 * 
 * The BigFiveRefDto follows the same pattern as OnetRefDto and EscoRefDto:
 * - trait: The Big Five dimension (like code in ONET, uri in ESCO)
 * - title: Human-readable name
 * - facet: Sub-dimension (like elementType/skillType)
 */
class CompetencyDeserializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        // NO global naming strategy - using camelCase matching Java record fields
        // ParameterNamesModule for better record support
        objectMapper.registerModule(new ParameterNamesModule());
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.findAndRegisterModules();
    }

    @Nested
    @DisplayName("StandardCodesDto Deserialization Tests")
    class StandardCodesDtoTests {

        @Test
        @DisplayName("Should deserialize BigFiveRefDto correctly (same pattern as ONET/ESCO)")
        void shouldDeserializeBigFiveRef() throws Exception {
            // BigFiveRefDto follows the same pattern as OnetRefDto and EscoRefDto
            String json = """
                {
                    "bigFiveRef": {
                        "trait": "OPENNESS",
                        "title": "Openness",
                        "facet": "creativity"
                    },
                    "onetRef": {
                        "code": "1.A.1.a.1",
                        "title": "Test Ability",
                        "elementType": "ability"
                    }
                }
                """;

            StandardCodesDto dto = objectMapper.readValue(json, StandardCodesDto.class);

            assertThat(dto).isNotNull();
            assertThat(dto.bigFiveRef()).isNotNull();
            System.out.println("Deserialized BigFiveRefDto: " + dto.bigFiveRef());
            System.out.println("trait value: " + dto.bigFiveRef().trait());
            assertThat(dto.bigFiveRef().trait()).isEqualTo("OPENNESS");
            assertThat(dto.bigFiveRef().title()).isEqualTo("Openness");
            assertThat(dto.bigFiveRef().facet()).isEqualTo("creativity");
            assertThat(dto.onetRef()).isNotNull();
            assertThat(dto.onetRef().code()).isEqualTo("1.A.1.a.1");
            assertThat(dto.onetRef().elementType()).isEqualTo("ability");
        }

        @Test
        @DisplayName("Should serialize to camelCase correctly")
        void shouldSerializeToCamelCase() throws Exception {
            StandardCodesDto dto = StandardCodesDto.builder()
                    .bigFiveRef("CONSCIENTIOUSNESS", "Conscientiousness", "self_discipline")
                    .onetRef("2.B.1.a", "Test Skill", "skill")
                    .build();

            String json = objectMapper.writeValueAsString(dto);
            System.out.println("Serialized JSON: " + json);

            // Verify camelCase in output
            assertThat(json).contains("\"bigFiveRef\"");
            assertThat(json).contains("\"trait\"");
            assertThat(json).contains("\"onetRef\"");
            assertThat(json).contains("\"elementType\"");
        }
    }

    @Nested
    @DisplayName("CompetencyDto with StandardCodesDto Deserialization Tests")
    class CompetencyDtoTests {

        @Test
        @DisplayName("Should deserialize CompetencyDto with nested StandardCodesDto (BigFiveRefDto)")
        void shouldDeserializeCompetencyDtoWithStandardCodes() throws Exception {
            // This is the new JSON structure with bigFiveRef following ONET/ESCO pattern
            // Note: "level" field was removed from CompetencyDto per domain migration
            String json = """
                {
                    "name": "Test Competency",
                    "description": "A test competency description that is long enough",
                    "category": "LEADERSHIP",
                    "isActive": true,
                    "approvalStatus": "DRAFT",
                    "standardCodes": {
                        "bigFiveRef": {
                            "trait": "EXTRAVERSION",
                            "title": "Extraversion",
                            "facet": "sociability"
                        },
                        "onetRef": {
                            "code": "1.A.3.c.1",
                            "title": "Extent Flexibility",
                            "elementType": "ability"
                        },
                        "escoRef": {
                            "uri": "http://data.europa.eu/esco/skill/9349f502-9c11-476c-957d-2e016790a2ee",
                            "title": "coordinate waste management procedures",
                            "skillType": "skill"
                        }
                    }
                }
                """;

            CompetencyDto dto = objectMapper.readValue(json, CompetencyDto.class);

            assertThat(dto).isNotNull();
            assertThat(dto.name()).isEqualTo("Test Competency");
            assertThat(dto.standardCodes()).isNotNull();
            
            System.out.println("Deserialized standardCodes: " + dto.standardCodes());
            System.out.println("bigFiveRef: " + dto.standardCodes().bigFiveRef());
            
            // THIS IS THE CRITICAL ASSERTION - now using trait field like ONET/ESCO
            assertThat(dto.standardCodes().bigFiveRef()).isNotNull();
            assertThat(dto.standardCodes().bigFiveRef().trait())
                .as("trait should be EXTRAVERSION, not null")
                .isEqualTo("EXTRAVERSION");
            assertThat(dto.standardCodes().bigFiveRef().facet()).isEqualTo("sociability");
            
            // Verify other fields
            assertThat(dto.standardCodes().onetRef()).isNotNull();
            assertThat(dto.standardCodes().onetRef().code()).isEqualTo("1.A.3.c.1");
            assertThat(dto.standardCodes().onetRef().elementType()).isEqualTo("ability");
            
            assertThat(dto.standardCodes().escoRef()).isNotNull();
            assertThat(dto.standardCodes().escoRef().skillType()).isEqualTo("skill");
        }
    }

    @Nested
    @DisplayName("OnetRefDto @Pattern Validation Tests")
    class OnetRefDtoValidationTests {

        private Validator validator;

        @BeforeEach
        void setUpValidator() {
            try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
                validator = factory.getValidator();
            }
        }

        @Test
        @DisplayName("Should accept 5-segment ability element IDs (e.g. 1.A.1.a.1)")
        void shouldAcceptFiveSegmentAbilityCode() {
            // Regression test for the bug where the @Pattern was capped at 4 segments,
            // causing every O*NET ability (all 52 of them) to fail validation on submit.
            StandardCodesDto.OnetRefDto ref =
                    new StandardCodesDto.OnetRefDto("1.A.1.a.1", "Oral Comprehension", "ability");
            Set<ConstraintViolation<StandardCodesDto.OnetRefDto>> violations = validator.validate(ref);
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("Should accept 4-segment skill / work-style / knowledge codes")
        void shouldAcceptFourSegmentCodes() {
            Set<ConstraintViolation<StandardCodesDto.OnetRefDto>> skill =
                    validator.validate(new StandardCodesDto.OnetRefDto("2.B.1.a", "Active Listening", "skill"));
            Set<ConstraintViolation<StandardCodesDto.OnetRefDto>> workStyle =
                    validator.validate(new StandardCodesDto.OnetRefDto("1.C.1.a", "Achievement/Effort", "work_style"));
            Set<ConstraintViolation<StandardCodesDto.OnetRefDto>> knowledge =
                    validator.validate(new StandardCodesDto.OnetRefDto("2.C.1.a", "Administration", "knowledge"));
            assertThat(skill).isEmpty();
            assertThat(workStyle).isEmpty();
            assertThat(knowledge).isEmpty();
        }

        @Test
        @DisplayName("Should reject malformed codes (no leading digit, wrong separators)")
        void shouldRejectMalformedCodes() {
            assertThat(validator.validate(new StandardCodesDto.OnetRefDto("A.1.B", null, null))).isNotEmpty();
            assertThat(validator.validate(new StandardCodesDto.OnetRefDto("1-A-1", null, null))).isNotEmpty();
            assertThat(validator.validate(new StandardCodesDto.OnetRefDto("not.a.code!", null, null))).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Backward Compatibility Tests")
    class BackwardCompatibilityTests {

        @Test
        @DisplayName("Should deserialize old snake_case format from database")
        void shouldDeserializeOldSnakeCaseFormat() throws Exception {
            // This is the OLD format that might exist in the database
            String json = """
                {
                    "global_category": {
                        "bigFive": "CONSCIENTIOUSNESS",
                        "dimension": "achievement_striving"
                    },
                    "onet_ref": {
                        "code": "2.B.1.a",
                        "title": "Oral Comprehension",
                        "element_type": "ability"
                    },
                    "esco_ref": {
                        "uri": "http://data.europa.eu/esco/skill/abc123-def456-789",
                        "title": "Communication Skills",
                        "skill_type": "skill"
                    }
                }
                """;

            StandardCodesDto dto = objectMapper.readValue(json, StandardCodesDto.class);

            assertThat(dto).isNotNull();
            // Old global_category should map to bigFiveRef
            assertThat(dto.bigFiveRef()).isNotNull();
            assertThat(dto.bigFiveRef().trait()).isEqualTo("CONSCIENTIOUSNESS");
            assertThat(dto.bigFiveRef().facet()).isEqualTo("achievement_striving");
            
            // Old onet_ref should map to onetRef
            assertThat(dto.onetRef()).isNotNull();
            assertThat(dto.onetRef().code()).isEqualTo("2.B.1.a");
            assertThat(dto.onetRef().elementType()).isEqualTo("ability");
            
            // Old esco_ref should map to escoRef
            assertThat(dto.escoRef()).isNotNull();
            assertThat(dto.escoRef().uri()).isEqualTo("http://data.europa.eu/esco/skill/abc123-def456-789");
            assertThat(dto.escoRef().skillType()).isEqualTo("skill");
        }

        @Test
        @DisplayName("Should deserialize new camelCase format from frontend")
        void shouldDeserializeNewCamelCaseFormat() throws Exception {
            // This is the NEW format from frontend
            String json = """
                {
                    "bigFiveRef": {
                        "trait": "OPENNESS",
                        "title": "Openness",
                        "facet": "creativity"
                    },
                    "onetRef": {
                        "code": "1.A.1.a",
                        "title": "Test Ability",
                        "elementType": "ability"
                    },
                    "escoRef": {
                        "uri": "http://data.europa.eu/esco/skill/new-skill-123",
                        "title": "New Skill",
                        "skillType": "competence"
                    }
                }
                """;

            StandardCodesDto dto = objectMapper.readValue(json, StandardCodesDto.class);

            assertThat(dto).isNotNull();
            assertThat(dto.bigFiveRef()).isNotNull();
            assertThat(dto.bigFiveRef().trait()).isEqualTo("OPENNESS");
            assertThat(dto.bigFiveRef().facet()).isEqualTo("creativity");
            
            assertThat(dto.onetRef()).isNotNull();
            assertThat(dto.onetRef().code()).isEqualTo("1.A.1.a");
            assertThat(dto.onetRef().elementType()).isEqualTo("ability");
            
            assertThat(dto.escoRef()).isNotNull();
            assertThat(dto.escoRef().skillType()).isEqualTo("competence");
        }
    }
}
