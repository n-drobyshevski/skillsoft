package app.skillsoft.assessmentbackend.domain.entities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BehavioralIndicator Entity Tests")
class BehavioralIndicatorTest {

    private BehavioralIndicator behavioralIndicator;
    private Competency competency;
    private UUID competencyId;
    private UUID behavioralIndicatorId;

    @BeforeEach
    void setUp() {
        competencyId = UUID.randomUUID();
        behavioralIndicatorId = UUID.randomUUID();
        
        // Create a minimal competency for relationship testing
        competency = new Competency();
        competency.setId(competencyId);
        competency.setName("Test Competency");
        
        behavioralIndicator = new BehavioralIndicator();
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create empty behavioral indicator with default constructor")
        void shouldCreateDefaultBehavioralIndicator() {
            // Given & When
            BehavioralIndicator indicator = new BehavioralIndicator();

            // Then
            assertThat(indicator.getId()).isNull();
            assertThat(indicator.getCompetency()).isNull();
            assertThat(indicator.getTitle()).isNull();
            assertThat(indicator.getDescription()).isNull();
            assertThat(indicator.getObservabilityLevel()).isNull();
            assertThat(indicator.getMeasurementType()).isNull();
            assertThat(indicator.getWeight()).isEqualTo(0.0f);
            assertThat(indicator.getExamples()).isNull();
            assertThat(indicator.getCounterExamples()).isNull();
            assertThat(indicator.isActive()).isFalse();
            assertThat(indicator.getApprovalStatus()).isNull();
            assertThat(indicator.getOrderIndex()).isNull();
        }

        @Test
        @DisplayName("Should create behavioral indicator with all parameters constructor")
        void shouldCreateBehavioralIndicatorWithAllParameters() {
            // Given
            String title = "Test Behavioral Indicator";
            String description = "This is a test behavioral indicator";
            ObservabilityLevel observabilityLevel = ObservabilityLevel.INFERRED;
            IndicatorMeasurementType measurementType = IndicatorMeasurementType.QUALITY;
            float weight = 0.8f;
            String examples = "Example behavior patterns";
            String counterExamples = "Counter example patterns";
            boolean isActive = true;
            ApprovalStatus approvalStatus = ApprovalStatus.APPROVED;
            Integer orderIndex = 1;
            ContextScope contextScope = ContextScope.UNIVERSAL;

            // When
            BehavioralIndicator indicator = new BehavioralIndicator(
                behavioralIndicatorId, competency, title, description, observabilityLevel,
                measurementType, weight, examples, counterExamples, isActive, 
                approvalStatus, orderIndex, contextScope
            );

            // Then
            assertThat(indicator.getId()).isEqualTo(behavioralIndicatorId);
            assertThat(indicator.getCompetency()).isEqualTo(competency);
            assertThat(indicator.getTitle()).isEqualTo(title);
            assertThat(indicator.getDescription()).isEqualTo(description);
            assertThat(indicator.getObservabilityLevel()).isEqualTo(observabilityLevel);
            assertThat(indicator.getMeasurementType()).isEqualTo(measurementType);
            assertThat(indicator.getWeight()).isEqualTo(weight);
            assertThat(indicator.getExamples()).isEqualTo(examples);
            assertThat(indicator.getCounterExamples()).isEqualTo(counterExamples);
            assertThat(indicator.isActive()).isEqualTo(isActive);
            assertThat(indicator.getApprovalStatus()).isEqualTo(approvalStatus);
            assertThat(indicator.getOrderIndex()).isEqualTo(orderIndex);
            assertThat(indicator.getContextScope()).isEqualTo(contextScope);
        }
    }

    @Nested
    @DisplayName("Setter and Getter Tests")
    class SetterGetterTests {

        @Test
        @DisplayName("Should set and get ID correctly")
        void shouldSetAndGetId() {
            // Given & When
            behavioralIndicator.setId(behavioralIndicatorId);

            // Then
            assertThat(behavioralIndicator.getId()).isEqualTo(behavioralIndicatorId);
        }

        @Test
        @DisplayName("Should set and get competency correctly")
        void shouldSetAndGetCompetency() {
            // Given & When
            behavioralIndicator.setCompetency(competency);

            // Then
            assertThat(behavioralIndicator.getCompetency()).isEqualTo(competency);
            assertThat(behavioralIndicator.getCompetency().getId()).isEqualTo(competencyId);
        }

        @Test
        @DisplayName("Should set and get basic properties correctly")
        void shouldSetAndGetBasicProperties() {
            // Given
            String title = "Leadership Skills";
            String description = "Demonstrates effective leadership in team environments";
            
            // When
            behavioralIndicator.setTitle(title);
            behavioralIndicator.setDescription(description);

            // Then
            assertThat(behavioralIndicator.getTitle()).isEqualTo(title);
            assertThat(behavioralIndicator.getDescription()).isEqualTo(description);
        }

        @Test
        @DisplayName("Should set and get observability level correctly")
        void shouldSetAndGetObservabilityLevel() {
            // Given
            ObservabilityLevel level = ObservabilityLevel.DIRECTLY_OBSERVABLE;

            // When
            behavioralIndicator.setObservabilityLevel(level);

            // Then
            assertThat(behavioralIndicator.getObservabilityLevel()).isEqualTo(level);
        }

        @Test
        @DisplayName("Should set and get measurement type correctly")
        void shouldSetAndGetMeasurementType() {
            // Given
            IndicatorMeasurementType type = IndicatorMeasurementType.QUALITY;

            // When
            behavioralIndicator.setMeasurementType(type);

            // Then
            assertThat(behavioralIndicator.getMeasurementType()).isEqualTo(type);
        }

        @Test
        @DisplayName("Should set and get weight correctly")
        void shouldSetAndGetWeight() {
            // Given
            float weight = 0.75f;

            // When
            behavioralIndicator.setWeight(weight);

            // Then
            assertThat(behavioralIndicator.getWeight()).isEqualTo(weight);
        }

        @Test
        @DisplayName("Should set and get examples and counter examples")
        void shouldSetAndGetExamples() {
            // Given
            String examples = "Shows initiative, mentors team members, makes decisive choices";
            String counterExamples = "Avoids responsibility, unclear communication, indecisive";

            // When
            behavioralIndicator.setExamples(examples);
            behavioralIndicator.setCounterExamples(counterExamples);

            // Then
            assertThat(behavioralIndicator.getExamples()).isEqualTo(examples);
            assertThat(behavioralIndicator.getCounterExamples()).isEqualTo(counterExamples);
        }

        @Test
        @DisplayName("Should set and get active flag correctly")
        void shouldSetAndGetActiveFlag() {
            // Given & When
            behavioralIndicator.setActive(true);

            // Then
            assertThat(behavioralIndicator.isActive()).isTrue();

            // Given & When
            behavioralIndicator.setActive(false);

            // Then
            assertThat(behavioralIndicator.isActive()).isFalse();
        }

        @Test
        @DisplayName("Should set and get approval status correctly")
        void shouldSetAndGetApprovalStatus() {
            // Given
            ApprovalStatus status = ApprovalStatus.APPROVED;

            // When
            behavioralIndicator.setApprovalStatus(status);

            // Then
            assertThat(behavioralIndicator.getApprovalStatus()).isEqualTo(status);
        }

        @Test
        @DisplayName("Should set and get order index correctly")
        void shouldSetAndGetOrderIndex() {
            // Given
            Integer orderIndex = 5;

            // When
            behavioralIndicator.setOrderIndex(orderIndex);

            // Then
            assertThat(behavioralIndicator.getOrderIndex()).isEqualTo(orderIndex);
        }
    }

    @Nested
    @DisplayName("Business Logic Tests")
    class BusinessLogicTests {

        @Test
        @DisplayName("Should handle Russian text in title and description")
        void shouldHandleRussianText() {
            // Given
            String russianTitle = "Лидерские навыки";
            String russianDescription = "Демонстрирует эффективное лидерство в командной среде";
            String russianExamples = "Проявляет инициативу, наставляет участников команды";

            // When
            behavioralIndicator.setTitle(russianTitle);
            behavioralIndicator.setDescription(russianDescription);
            behavioralIndicator.setExamples(russianExamples);

            // Then
            assertThat(behavioralIndicator.getTitle()).isEqualTo(russianTitle);
            assertThat(behavioralIndicator.getDescription()).isEqualTo(russianDescription);
            assertThat(behavioralIndicator.getExamples()).isEqualTo(russianExamples);
        }

        @Test
        @DisplayName("Should validate weight boundaries")
        void shouldValidateWeightBoundaries() {
            // Test minimum weight
            behavioralIndicator.setWeight(0.0f);
            assertThat(behavioralIndicator.getWeight()).isEqualTo(0.0f);

            // Test maximum weight
            behavioralIndicator.setWeight(1.0f);
            assertThat(behavioralIndicator.getWeight()).isEqualTo(1.0f);

            // Test decimal weight
            behavioralIndicator.setWeight(0.25f);
            assertThat(behavioralIndicator.getWeight()).isEqualTo(0.25f);
        }

        @Test
        @DisplayName("Should handle relationship with competency correctly")
        void shouldHandleCompetencyRelationship() {
            // Given
            Competency parentCompetency = new Competency();
            parentCompetency.setId(UUID.randomUUID());
            parentCompetency.setName("Parent Competency");

            // When
            behavioralIndicator.setCompetency(parentCompetency);

            // Then
            assertThat(behavioralIndicator.getCompetency()).isNotNull();
            assertThat(behavioralIndicator.getCompetency().getId()).isEqualTo(parentCompetency.getId());
            assertThat(behavioralIndicator.getCompetency().getName()).isEqualTo("Parent Competency");
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle null values gracefully")
        void shouldHandleNullValues() {
            // When setting null values
            behavioralIndicator.setTitle(null);
            behavioralIndicator.setDescription(null);
            behavioralIndicator.setExamples(null);
            behavioralIndicator.setCounterExamples(null);
            behavioralIndicator.setCompetency(null);

            // Then should not throw exceptions
            assertThat(behavioralIndicator.getTitle()).isNull();
            assertThat(behavioralIndicator.getDescription()).isNull();
            assertThat(behavioralIndicator.getExamples()).isNull();
            assertThat(behavioralIndicator.getCounterExamples()).isNull();
            assertThat(behavioralIndicator.getCompetency()).isNull();
        }

        @Test
        @DisplayName("Should handle empty strings")
        void shouldHandleEmptyStrings() {
            // When setting empty strings
            behavioralIndicator.setTitle("");
            behavioralIndicator.setDescription("");
            behavioralIndicator.setExamples("");
            behavioralIndicator.setCounterExamples("");

            // Then
            assertThat(behavioralIndicator.getTitle()).isEmpty();
            assertThat(behavioralIndicator.getDescription()).isEmpty();
            assertThat(behavioralIndicator.getExamples()).isEmpty();
            assertThat(behavioralIndicator.getCounterExamples()).isEmpty();
        }

        @Test
        @DisplayName("Should handle long text content")
        void shouldHandleLongTextContent() {
            // Given
            String longTitle = "A".repeat(500);
            String longDescription = "B".repeat(1000);
            String longExamples = "C".repeat(1000);
            String longCounterExamples = "D".repeat(500);

            // When
            behavioralIndicator.setTitle(longTitle);
            behavioralIndicator.setDescription(longDescription);
            behavioralIndicator.setExamples(longExamples);
            behavioralIndicator.setCounterExamples(longCounterExamples);

            // Then
            assertThat(behavioralIndicator.getTitle()).hasSize(500);
            assertThat(behavioralIndicator.getDescription()).hasSize(1000);
            assertThat(behavioralIndicator.getExamples()).hasSize(1000);
            assertThat(behavioralIndicator.getCounterExamples()).hasSize(500);
        }

        @Test
        @DisplayName("Should handle extreme weight values")
        void shouldHandleExtremeWeightValues() {
            // Test with very small positive value
            behavioralIndicator.setWeight(0.001f);
            assertThat(behavioralIndicator.getWeight()).isEqualTo(0.001f);

            // Test with value close to 1
            behavioralIndicator.setWeight(0.999f);
            assertThat(behavioralIndicator.getWeight()).isEqualTo(0.999f);

            // Test with negative value (though validation might prevent this in real usage)
            behavioralIndicator.setWeight(-0.1f);
            assertThat(behavioralIndicator.getWeight()).isEqualTo(-0.1f);

            // Test with value greater than 1
            behavioralIndicator.setWeight(1.5f);
            assertThat(behavioralIndicator.getWeight()).isEqualTo(1.5f);
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should generate meaningful toString representation")
        void shouldGenerateMeaningfulToString() {
            // Given
            behavioralIndicator.setId(behavioralIndicatorId);
            behavioralIndicator.setTitle("Test Indicator");
            behavioralIndicator.setWeight(0.5f);
            behavioralIndicator.setActive(true);

            // When
            String toString = behavioralIndicator.toString();

            // Then
            assertThat(toString).contains("BehavioralIndicator");
            assertThat(toString).contains(behavioralIndicatorId.toString());
            assertThat(toString).contains("Test Indicator");
            assertThat(toString).contains("0.5");
            assertThat(toString).contains("true");
        }

        @Test
        @DisplayName("Should handle toString with null values")
        void shouldHandleToStringWithNullValues() {
            // Given - minimal behavioral indicator
            behavioralIndicator.setTitle("Minimal Test");

            // When
            String toString = behavioralIndicator.toString();

            // Then - should not throw exception and contain meaningful content
            assertThat(toString).isNotNull();
            assertThat(toString).contains("BehavioralIndicator");
            assertThat(toString).contains("Minimal Test");
        }
    }

    @Nested
    @DisplayName("Enum Integration Tests")
    class EnumIntegrationTests {

        @Test
        @DisplayName("Should work with all ObservabilityLevel values")
        void shouldWorkWithAllObservabilityLevels() {
            for (ObservabilityLevel level : ObservabilityLevel.values()) {
                behavioralIndicator.setObservabilityLevel(level);
                assertThat(behavioralIndicator.getObservabilityLevel()).isEqualTo(level);
            }
        }

        @Test
        @DisplayName("Should work with all IndicatorMeasurementType values")
        void shouldWorkWithAllIndicatorMeasurementTypes() {
            for (IndicatorMeasurementType type : IndicatorMeasurementType.values()) {
                behavioralIndicator.setMeasurementType(type);
                assertThat(behavioralIndicator.getMeasurementType()).isEqualTo(type);
            }
        }

        @Test
        @DisplayName("Should work with all ApprovalStatus values")
        void shouldWorkWithAllApprovalStatuses() {
            for (ApprovalStatus status : ApprovalStatus.values()) {
                behavioralIndicator.setApprovalStatus(status);
                assertThat(behavioralIndicator.getApprovalStatus()).isEqualTo(status);
            }
        }
    }
}