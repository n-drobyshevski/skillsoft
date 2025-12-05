package app.skillsoft.assessmentbackend.domain.entities;

import app.skillsoft.assessmentbackend.domain.dto.StandardCodesDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.time.LocalDateTime;
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
    private StandardCodesDto standardCodes;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        
        // Setup standard codes using the new DTO structure
        standardCodes = StandardCodesDto.builder()
                .escoRef("http://data.europa.eu/esco/skill/abc123-def456-789",
                        "develop organisational strategies", "skill")
                .onetRef("2.B.3.c", "Leadership requirements", "ability")
                .globalCategory("leadership", "strategic_thinking", null)
                .build();
        
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
            assertThat(fullCompetency.getStandardCodes().hasEscoMapping()).isTrue();
            assertThat(fullCompetency.getStandardCodes().hasOnetMapping()).isTrue();
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
            assertThat(competency.getStandardCodes().hasEscoMapping()).isTrue();
            assertThat(competency.getStandardCodes().hasOnetMapping()).isTrue();
            
            assertThat(competency.getStandardCodes().escoRef().uri())
                    .isEqualTo("http://data.europa.eu/esco/skill/abc123-def456-789");
            assertThat(competency.getStandardCodes().escoRef().title())
                    .isEqualTo("develop organisational strategies");
            assertThat(competency.getStandardCodes().onetRef().code())
                    .isEqualTo("2.B.3.c");
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
            StandardCodesDto emptyDto = new StandardCodesDto();
            competency.setStandardCodes(emptyDto);
            
            assertThat(competency.getStandardCodes()).isNotNull();
            assertThat(competency.getStandardCodes().hasAnyMapping()).isFalse();
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
            // Create complex standard codes with all fields
            StandardCodesDto complexCodes = StandardCodesDto.builder()
                    .escoRef("http://data.europa.eu/esco/skill/abc123-def456-789",
                            "develop organisational strategies", "skill")
                    .onetRef("2.B.3.c", "Leadership requirements", "ability")
                    .globalCategory("big_five", "extraversion", null)
                    .build();
            
            competency.setStandardCodes(complexCodes);
            
            assertThat(competency.getStandardCodes().hasAnyMapping()).isTrue();
            assertThat(competency.getStandardCodes().hasEscoMapping()).isTrue();
            assertThat(competency.getStandardCodes().hasOnetMapping()).isTrue();
            assertThat(competency.getStandardCodes().globalCategory()).isNotNull();
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