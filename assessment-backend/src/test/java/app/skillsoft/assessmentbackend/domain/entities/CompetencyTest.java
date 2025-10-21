package app.skillsoft.assessmentbackend.domain.entities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Competency entity model
 * 
 * Tests cover:
 * - Entity creation and validation
 * - Standard codes JSON handling
 * - Getter/setter functionality
 * - toString() method
 * - Constructor variations
 */
@DisplayName("Competency Entity Tests")
class CompetencyTest {

    private Competency competency;
    private Map<String, Object> standardCodes;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        
        // Setup standard codes JSON structure
        standardCodes = new HashMap<>();
        Map<String, Object> escoMapping = new HashMap<>();
        escoMapping.put("code", "S7.1.1");
        escoMapping.put("name", "develop organisational strategies");
        escoMapping.put("confidence", "HIGH");
        standardCodes.put("ESCO", escoMapping);
        
        Map<String, Object> onetMapping = new HashMap<>();
        onetMapping.put("code", "2.B.3.c");
        onetMapping.put("name", "Leadership requirements");
        onetMapping.put("confidence", "VERIFIED");
        standardCodes.put("ONET", onetMapping);
        
        competency = new Competency();
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Default constructor creates valid empty competency")
        void defaultConstructor() {
            Competency emptyCompetency = new Competency();
            
            assertThat(emptyCompetency).isNotNull();
            assertThat(emptyCompetency.getId()).isNull();
            assertThat(emptyCompetency.getName()).isNull();
            assertThat(emptyCompetency.getStandardCodes()).isNull();
        }

        @Test
        @DisplayName("Full constructor creates competency with all fields")
        void fullConstructor() {
            UUID id = UUID.randomUUID();
            
            Competency fullCompetency = new Competency(
                id,
                "Стратегическое лидерство",
                "Способность определять долгосрочные цели",
                CompetencyCategory.LEADERSHIP,
                ProficiencyLevel.ADVANCED,
                standardCodes,
                true,
                ApprovalStatus.APPROVED,
                null,
                1,
                now,
                now
            );
            
            assertThat(fullCompetency.getId()).isEqualTo(id);
            assertThat(fullCompetency.getName()).isEqualTo("Стратегическое лидерство");
            assertThat(fullCompetency.getDescription()).isEqualTo("Способность определять долгосрочные цели");
            assertThat(fullCompetency.getCategory()).isEqualTo(CompetencyCategory.LEADERSHIP);
            assertThat(fullCompetency.getLevel()).isEqualTo(ProficiencyLevel.ADVANCED);
            assertThat(fullCompetency.getStandardCodes()).isEqualTo(standardCodes);
            assertThat(fullCompetency.isActive()).isTrue();
            assertThat(fullCompetency.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
            assertThat(fullCompetency.getVersion()).isEqualTo(1);
            assertThat(fullCompetency.getCreatedAt()).isEqualTo(now);
            assertThat(fullCompetency.getLastModified()).isEqualTo(now);
        }
    }

    @Nested
    @DisplayName("Setter/Getter Tests")
    class SetterGetterTests {

        @Test
        @DisplayName("Set and get basic properties")
        void setGetBasicProperties() {
            UUID id = UUID.randomUUID();
            
            competency.setId(id);
            competency.setName("Коммуникация");
            competency.setDescription("Эффективная коммуникация");
            competency.setCategory(CompetencyCategory.COMMUNICATION);
            competency.setLevel(ProficiencyLevel.PROFICIENT);
            competency.setActive(true);
            competency.setApprovalStatus(ApprovalStatus.PENDING_REVIEW);
            competency.setVersion(2);
            competency.setCreatedAt(now);
            competency.setLastModified(now);
            
            assertThat(competency.getId()).isEqualTo(id);
            assertThat(competency.getName()).isEqualTo("Коммуникация");
            assertThat(competency.getDescription()).isEqualTo("Эффективная коммуникация");
            assertThat(competency.getCategory()).isEqualTo(CompetencyCategory.COMMUNICATION);
            assertThat(competency.getLevel()).isEqualTo(ProficiencyLevel.PROFICIENT);
            assertThat(competency.isActive()).isTrue();
            assertThat(competency.getApprovalStatus()).isEqualTo(ApprovalStatus.PENDING_REVIEW);
            assertThat(competency.getVersion()).isEqualTo(2);
            assertThat(competency.getCreatedAt()).isEqualTo(now);
            assertThat(competency.getLastModified()).isEqualTo(now);
        }

        @Test
        @DisplayName("Set and get standard codes JSON")
        void setGetStandardCodes() {
            competency.setStandardCodes(standardCodes);
            
            assertThat(competency.getStandardCodes()).isNotNull();
            assertThat(competency.getStandardCodes()).containsKey("ESCO");
            assertThat(competency.getStandardCodes()).containsKey("ONET");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> esco = (Map<String, Object>) competency.getStandardCodes().get("ESCO");
            assertThat(esco.get("code")).isEqualTo("S7.1.1");
            assertThat(esco.get("name")).isEqualTo("develop organisational strategies");
            assertThat(esco.get("confidence")).isEqualTo("HIGH");
        }

        @Test
        @DisplayName("Handle null standard codes")
        void handleNullStandardCodes() {
            competency.setStandardCodes(null);
            
            assertThat(competency.getStandardCodes()).isNull();
        }

        @Test
        @DisplayName("Handle empty standard codes")
        void handleEmptyStandardCodes() {
            Map<String, Object> emptyMap = new HashMap<>();
            competency.setStandardCodes(emptyMap);
            
            assertThat(competency.getStandardCodes()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Business Logic Tests")
    class BusinessLogicTests {

        @Test
        @DisplayName("toString includes all fields including standard codes")
        void toStringIncludesAllFields() {
            competency.setName("Тестовая компетенция");
            competency.setStandardCodes(standardCodes);
            competency.setActive(true);
            
            String result = competency.toString();
            
            assertThat(result).contains("Тестовая компетенция");
            assertThat(result).contains("standardCodes");
            assertThat(result).contains("isActive=true");
        }

        @Test
        @DisplayName("isActive property works correctly")
        void isActiveProperty() {
            // Test default value (should be false for primitive boolean)
            assertThat(competency.isActive()).isFalse();
            
            // Test setting to true
            competency.setActive(true);
            assertThat(competency.isActive()).isTrue();
            
            // Test setting back to false
            competency.setActive(false);
            assertThat(competency.isActive()).isFalse();
        }

        @Test
        @DisplayName("Version increments work correctly")
        void versionIncrements() {
            competency.setVersion(1);
            assertThat(competency.getVersion()).isEqualTo(1);
            
            competency.setVersion(competency.getVersion() + 1);
            assertThat(competency.getVersion()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Validation")
    class EdgeCasesTests {

        @Test
        @DisplayName("Handle complex standard codes structure")
        void handleComplexStandardCodes() {
            Map<String, Object> complexCodes = new HashMap<>();
            
            // Add multiple standards
            Map<String, Object> esco = new HashMap<>();
            esco.put("code", "S7.1.1");
            esco.put("name", "develop organisational strategies");
            esco.put("confidence", "HIGH");
            complexCodes.put("ESCO", esco);
            
            Map<String, Object> bigFive = new HashMap<>();
            bigFive.put("code", "EXTRAVERSION");
            bigFive.put("name", "Extraversion traits");
            bigFive.put("confidence", "MODERATE");
            complexCodes.put("BIG_FIVE", bigFive);
            
            competency.setStandardCodes(complexCodes);
            
            assertThat(competency.getStandardCodes()).hasSize(2);
            assertThat(competency.getStandardCodes()).containsKeys("ESCO", "BIG_FIVE");
        }

        @Test
        @DisplayName("Handle Russian text in fields")
        void handleRussianText() {
            competency.setName("Эмоциональный интеллект");
            competency.setDescription("Способность понимать и управлять эмоциями себя и других людей в различных ситуациях");
            
            assertThat(competency.getName()).isEqualTo("Эмоциональный интеллект");
            assertThat(competency.getDescription()).contains("эмоциями");
            assertThat(competency.getDescription()).contains("ситуациях");
        }

        @Test
        @DisplayName("Handle all enum values")
        void handleAllEnumValues() {
            // Test all CompetencyCategory values
            for (CompetencyCategory category : CompetencyCategory.values()) {
                competency.setCategory(category);
                assertThat(competency.getCategory()).isEqualTo(category);
            }
            
            // Test all ProficiencyLevel values
            for (ProficiencyLevel level : ProficiencyLevel.values()) {
                competency.setLevel(level);
                assertThat(competency.getLevel()).isEqualTo(level);
            }
            
            // Test all ApprovalStatus values
            for (ApprovalStatus status : ApprovalStatus.values()) {
                competency.setApprovalStatus(status);
                assertThat(competency.getApprovalStatus()).isEqualTo(status);
            }
        }
    }
}