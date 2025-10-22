package app.skillsoft.assessmentbackend.services;

import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.repository.BehavioralIndicatorRepository;
import app.skillsoft.assessmentbackend.repository.CompetencyRepository;
import app.skillsoft.assessmentbackend.services.impl.BehavioralIndicatorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for BehavioralIndicatorService
 * 
 * Tests cover:
 * - CRUD operations with mocked dependencies
 * - Business logic validation
 * - Error handling and edge cases
 * - Transactional behavior verification
 * - Entity relationship management
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BehavioralIndicator Service Tests")
class BehavioralIndicatorServiceTest {

    @Mock
    private BehavioralIndicatorRepository behavioralIndicatorRepository;

    @Mock
    private CompetencyRepository competencyRepository;

    @InjectMocks
    private BehavioralIndicatorServiceImpl behavioralIndicatorService;

    private UUID competencyId;
    private UUID behavioralIndicatorId;
    private Competency mockCompetency;
    private BehavioralIndicator mockBehavioralIndicator;

    @BeforeEach
    void setUp() {
        competencyId = UUID.randomUUID();
        behavioralIndicatorId = UUID.randomUUID();

        // Create mock competency
        mockCompetency = new Competency();
        mockCompetency.setId(competencyId);
        mockCompetency.setName("Test Competency");
        mockCompetency.setDescription("A test competency for behavioral indicators");
        mockCompetency.setCategory(CompetencyCategory.LEADERSHIP);
        mockCompetency.setLevel(ProficiencyLevel.PROFICIENT);
        mockCompetency.setActive(true);
        mockCompetency.setApprovalStatus(ApprovalStatus.APPROVED);
        mockCompetency.setVersion(1);
        mockCompetency.setCreatedAt(LocalDateTime.now());
        mockCompetency.setLastModified(LocalDateTime.now());

        // Create mock behavioral indicator
        mockBehavioralIndicator = new BehavioralIndicator();
        mockBehavioralIndicator.setId(behavioralIndicatorId);
        mockBehavioralIndicator.setCompetency(mockCompetency);
        mockBehavioralIndicator.setTitle("Test Behavioral Indicator");
        mockBehavioralIndicator.setDescription("A test behavioral indicator");
        mockBehavioralIndicator.setObservabilityLevel(ProficiencyLevel.PROFICIENT);
        mockBehavioralIndicator.setMeasurementType(IndicatorMeasurementType.QUALITY);
        mockBehavioralIndicator.setWeight(0.8f);
        mockBehavioralIndicator.setExamples("Example behaviors");
        mockBehavioralIndicator.setCounterExamples("Counter-example behaviors");
        mockBehavioralIndicator.setActive(true);
        mockBehavioralIndicator.setApprovalStatus(ApprovalStatus.APPROVED);
        mockBehavioralIndicator.setOrderIndex(1);
    }

    @Nested
    @DisplayName("List Behavioral Indicators Tests")
    class ListBehavioralIndicatorsTests {

        @Test
        @DisplayName("Should return all behavioral indicators for competency")
        void shouldReturnAllBehavioralIndicatorsForCompetency() {
            // Given
            List<BehavioralIndicator> expectedIndicators = Arrays.asList(
                mockBehavioralIndicator,
                createTestIndicator("Second Indicator", 2),
                createTestIndicator("Third Indicator", 3)
            );
            when(behavioralIndicatorRepository.findByCompetencyId(competencyId))
                .thenReturn(expectedIndicators);

            // When
            List<BehavioralIndicator> actualIndicators = behavioralIndicatorService.listBehavioralIndicators(competencyId);

            // Then
            assertThat(actualIndicators).isNotNull();
            assertThat(actualIndicators).hasSize(3);
            assertThat(actualIndicators).containsExactlyElementsOf(expectedIndicators);
            
            verify(behavioralIndicatorRepository).findByCompetencyId(competencyId);
            verifyNoMoreInteractions(behavioralIndicatorRepository);
            verifyNoInteractions(competencyRepository);
        }

        @Test
        @DisplayName("Should return empty list when no behavioral indicators exist")
        void shouldReturnEmptyListWhenNoBehavioralIndicatorsExist() {
            // Given
            when(behavioralIndicatorRepository.findByCompetencyId(competencyId))
                .thenReturn(Collections.emptyList());

            // When
            List<BehavioralIndicator> actualIndicators = behavioralIndicatorService.listBehavioralIndicators(competencyId);

            // Then
            assertThat(actualIndicators).isNotNull();
            assertThat(actualIndicators).isEmpty();
            
            verify(behavioralIndicatorRepository).findByCompetencyId(competencyId);
        }

        @Test
        @DisplayName("Should handle null competency ID gracefully")
        void shouldHandleNullCompetencyIdGracefully() {
            // Given
            UUID nullCompetencyId = null;
            when(behavioralIndicatorRepository.findByCompetencyId(nullCompetencyId))
                .thenReturn(Collections.emptyList());

            // When
            List<BehavioralIndicator> actualIndicators = behavioralIndicatorService.listBehavioralIndicators(nullCompetencyId);

            // Then
            assertThat(actualIndicators).isNotNull();
            assertThat(actualIndicators).isEmpty();
            
            verify(behavioralIndicatorRepository).findByCompetencyId(nullCompetencyId);
        }
    }

    @Nested
    @DisplayName("Create Behavioral Indicator Tests")
    class CreateBehavioralIndicatorTests {

        @Test
        @DisplayName("Should create behavioral indicator successfully")
        void shouldCreateBehavioralIndicatorSuccessfully() {
            // Given
            BehavioralIndicator newIndicator = new BehavioralIndicator();
            newIndicator.setTitle("New Indicator");
            newIndicator.setDescription("New description");
            newIndicator.setObservabilityLevel(ProficiencyLevel.DEVELOPING);
            newIndicator.setMeasurementType(IndicatorMeasurementType.FREQUENCY);
            newIndicator.setWeight(0.6f);
            newIndicator.setActive(true);
            newIndicator.setApprovalStatus(ApprovalStatus.DRAFT);
            newIndicator.setOrderIndex(2);

            when(competencyRepository.findById(competencyId)).thenReturn(Optional.of(mockCompetency));
            when(behavioralIndicatorRepository.save(any(BehavioralIndicator.class))).thenReturn(mockBehavioralIndicator);

            // When
            BehavioralIndicator createdIndicator = behavioralIndicatorService.createBehavioralIndicator(competencyId, newIndicator);

            // Then
            assertThat(createdIndicator).isNotNull();
            assertThat(createdIndicator.getCompetency()).isEqualTo(mockCompetency);
            
            verify(competencyRepository).findById(competencyId);
            verify(behavioralIndicatorRepository).save(argThat(indicator -> 
                indicator.getCompetency().equals(mockCompetency) &&
                indicator.getTitle().equals("New Indicator")
            ));
        }

        @Test
        @DisplayName("Should throw exception when competency not found")
        void shouldThrowExceptionWhenCompetencyNotFound() {
            // Given
            BehavioralIndicator newIndicator = new BehavioralIndicator();
            newIndicator.setTitle("New Indicator");
            
            when(competencyRepository.findById(competencyId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> behavioralIndicatorService.createBehavioralIndicator(competencyId, newIndicator))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Competency not found with id: " + competencyId);
            
            verify(competencyRepository).findById(competencyId);
            verifyNoInteractions(behavioralIndicatorRepository);
        }

        @Test
        @DisplayName("Should throw exception when indicator already has ID")
        void shouldThrowExceptionWhenIndicatorAlreadyHasId() {
            // Given
            BehavioralIndicator existingIndicator = new BehavioralIndicator();
            existingIndicator.setId(UUID.randomUUID()); // ID already set
            existingIndicator.setTitle("Existing Indicator");

            // When & Then
            assertThatThrownBy(() -> behavioralIndicatorService.createBehavioralIndicator(competencyId, existingIndicator))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("New behavioral indicator cannot already have an ID");
            
            verifyNoInteractions(competencyRepository);
            verifyNoInteractions(behavioralIndicatorRepository);
        }

        @Test
        @DisplayName("Should throw exception when title is null")
        void shouldThrowExceptionWhenTitleIsNull() {
            // Given
            BehavioralIndicator invalidIndicator = new BehavioralIndicator();
            invalidIndicator.setTitle(null);

            // When & Then
            assertThatThrownBy(() -> behavioralIndicatorService.createBehavioralIndicator(competencyId, invalidIndicator))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Behavioral indicator title is required");
            
            verifyNoInteractions(competencyRepository);
            verifyNoInteractions(behavioralIndicatorRepository);
        }

        @Test
        @DisplayName("Should throw exception when title is blank")
        void shouldThrowExceptionWhenTitleIsBlank() {
            // Given
            BehavioralIndicator invalidIndicator = new BehavioralIndicator();
            invalidIndicator.setTitle("   "); // Blank title

            // When & Then
            assertThatThrownBy(() -> behavioralIndicatorService.createBehavioralIndicator(competencyId, invalidIndicator))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Behavioral indicator title is required");
            
            verifyNoInteractions(competencyRepository);
            verifyNoInteractions(behavioralIndicatorRepository);
        }
    }

    @Nested
    @DisplayName("Find Behavioral Indicator Tests")
    class FindBehavioralIndicatorTests {

        @Test
        @DisplayName("Should find behavioral indicator by ID and competency ID")
        void shouldFindBehavioralIndicatorByIdAndCompetencyId() {
            // Given
            when(behavioralIndicatorRepository.findByIdAndCompetencyId(behavioralIndicatorId, competencyId))
                .thenReturn(Optional.of(mockBehavioralIndicator));

            // When
            Optional<BehavioralIndicator> foundIndicator = behavioralIndicatorService
                .findBehavioralIndicatorById(competencyId, behavioralIndicatorId);

            // Then
            assertThat(foundIndicator).isPresent();
            assertThat(foundIndicator.get()).isEqualTo(mockBehavioralIndicator);
            assertThat(foundIndicator.get().getId()).isEqualTo(behavioralIndicatorId);
            assertThat(foundIndicator.get().getCompetency().getId()).isEqualTo(competencyId);
            
            verify(behavioralIndicatorRepository).findByIdAndCompetencyId(behavioralIndicatorId, competencyId);
        }

        @Test
        @DisplayName("Should return empty when behavioral indicator not found")
        void shouldReturnEmptyWhenBehavioralIndicatorNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(behavioralIndicatorRepository.findByIdAndCompetencyId(nonExistentId, competencyId))
                .thenReturn(Optional.empty());

            // When
            Optional<BehavioralIndicator> foundIndicator = behavioralIndicatorService
                .findBehavioralIndicatorById(competencyId, nonExistentId);

            // Then
            assertThat(foundIndicator).isEmpty();
            
            verify(behavioralIndicatorRepository).findByIdAndCompetencyId(nonExistentId, competencyId);
        }

        @Test
        @DisplayName("Should return empty when competency ID doesn't match")
        void shouldReturnEmptyWhenCompetencyIdDoesNotMatch() {
            // Given
            UUID wrongCompetencyId = UUID.randomUUID();
            when(behavioralIndicatorRepository.findByIdAndCompetencyId(behavioralIndicatorId, wrongCompetencyId))
                .thenReturn(Optional.empty());

            // When
            Optional<BehavioralIndicator> foundIndicator = behavioralIndicatorService
                .findBehavioralIndicatorById(wrongCompetencyId, behavioralIndicatorId);

            // Then
            assertThat(foundIndicator).isEmpty();
            
            verify(behavioralIndicatorRepository).findByIdAndCompetencyId(behavioralIndicatorId, wrongCompetencyId);
        }
    }

    @Nested
    @DisplayName("Update Behavioral Indicator Tests")
    class UpdateBehavioralIndicatorTests {

        @Test
        @DisplayName("Should update behavioral indicator successfully")
        void shouldUpdateBehavioralIndicatorSuccessfully() {
            // Given
            BehavioralIndicator updateDetails = new BehavioralIndicator();
            updateDetails.setTitle("Updated Title");
            updateDetails.setDescription("Updated Description");
            updateDetails.setObservabilityLevel(ProficiencyLevel.ADVANCED);
            updateDetails.setMeasurementType(IndicatorMeasurementType.IMPACT);
            updateDetails.setWeight(0.9f);
            updateDetails.setExamples("Updated examples");
            updateDetails.setCounterExamples("Updated counter-examples");
            updateDetails.setActive(false);
            updateDetails.setApprovalStatus(ApprovalStatus.PENDING_REVIEW);
            updateDetails.setOrderIndex(5);

            BehavioralIndicator existingIndicator = new BehavioralIndicator();
            existingIndicator.setId(behavioralIndicatorId);
            existingIndicator.setTitle("Original Title");
            existingIndicator.setCompetency(mockCompetency);

            when(behavioralIndicatorRepository.findByIdAndCompetencyId(behavioralIndicatorId, competencyId))
                .thenReturn(Optional.of(existingIndicator));
            when(behavioralIndicatorRepository.save(any(BehavioralIndicator.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            BehavioralIndicator updatedIndicator = behavioralIndicatorService
                .updateBehavioralIndicator(competencyId, behavioralIndicatorId, updateDetails);

            // Then
            assertThat(updatedIndicator).isNotNull();
            assertThat(updatedIndicator.getTitle()).isEqualTo("Updated Title");
            assertThat(updatedIndicator.getDescription()).isEqualTo("Updated Description");
            assertThat(updatedIndicator.getObservabilityLevel()).isEqualTo(ProficiencyLevel.ADVANCED);
            assertThat(updatedIndicator.getMeasurementType()).isEqualTo(IndicatorMeasurementType.IMPACT);
            assertThat(updatedIndicator.getWeight()).isEqualTo(0.9f);
            assertThat(updatedIndicator.getExamples()).isEqualTo("Updated examples");
            assertThat(updatedIndicator.getCounterExamples()).isEqualTo("Updated counter-examples");
            assertThat(updatedIndicator.isActive()).isFalse();
            assertThat(updatedIndicator.getApprovalStatus()).isEqualTo(ApprovalStatus.PENDING_REVIEW);
            assertThat(updatedIndicator.getOrderIndex()).isEqualTo(5);
            
            verify(behavioralIndicatorRepository).findByIdAndCompetencyId(behavioralIndicatorId, competencyId);
            verify(behavioralIndicatorRepository).save(existingIndicator);
        }

        @Test
        @DisplayName("Should throw exception when behavioral indicator not found")
        void shouldThrowExceptionWhenBehavioralIndicatorNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            BehavioralIndicator updateDetails = new BehavioralIndicator();
            updateDetails.setTitle("Updated Title");

            when(behavioralIndicatorRepository.findByIdAndCompetencyId(nonExistentId, competencyId))
                .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> behavioralIndicatorService
                .updateBehavioralIndicator(competencyId, nonExistentId, updateDetails))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Behavioral indicator not found with id: " + nonExistentId + " for competency: " + competencyId);
            
            verify(behavioralIndicatorRepository).findByIdAndCompetencyId(nonExistentId, competencyId);
            verify(behavioralIndicatorRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should handle partial updates correctly")
        void shouldHandlePartialUpdatesCorrectly() {
            // Given
            BehavioralIndicator updateDetails = new BehavioralIndicator();
            updateDetails.setTitle("Partially Updated Title");
            // Only setting title, other fields should remain unchanged

            BehavioralIndicator existingIndicator = new BehavioralIndicator();
            existingIndicator.setId(behavioralIndicatorId);
            existingIndicator.setTitle("Original Title");
            existingIndicator.setDescription("Original Description");
            existingIndicator.setWeight(0.5f);
            existingIndicator.setCompetency(mockCompetency);

            when(behavioralIndicatorRepository.findByIdAndCompetencyId(behavioralIndicatorId, competencyId))
                .thenReturn(Optional.of(existingIndicator));
            when(behavioralIndicatorRepository.save(any(BehavioralIndicator.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            BehavioralIndicator updatedIndicator = behavioralIndicatorService
                .updateBehavioralIndicator(competencyId, behavioralIndicatorId, updateDetails);

            // Then
            assertThat(updatedIndicator).isNotNull();
            assertThat(updatedIndicator.getTitle()).isEqualTo("Partially Updated Title");
            // Other fields should be updated to values from updateDetails (which may be null)
            
            verify(behavioralIndicatorRepository).save(existingIndicator);
        }
    }

    @Nested
    @DisplayName("Delete Behavioral Indicator Tests")
    class DeleteBehavioralIndicatorTests {

        @Test
        @DisplayName("Should delete behavioral indicator successfully")
        void shouldDeleteBehavioralIndicatorSuccessfully() {
            // Given
            when(behavioralIndicatorRepository.findByIdAndCompetencyId(behavioralIndicatorId, competencyId))
                .thenReturn(Optional.of(mockBehavioralIndicator));
            doNothing().when(behavioralIndicatorRepository).delete(mockBehavioralIndicator);

            // When
            assertThatCode(() -> behavioralIndicatorService.deleteBehavioralIndicator(competencyId, behavioralIndicatorId))
                .doesNotThrowAnyException();

            // Then
            verify(behavioralIndicatorRepository).findByIdAndCompetencyId(behavioralIndicatorId, competencyId);
            verify(behavioralIndicatorRepository).delete(mockBehavioralIndicator);
        }

        @Test
        @DisplayName("Should throw exception when behavioral indicator not found")
        void shouldThrowExceptionWhenBehavioralIndicatorNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(behavioralIndicatorRepository.findByIdAndCompetencyId(nonExistentId, competencyId))
                .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> behavioralIndicatorService.deleteBehavioralIndicator(competencyId, nonExistentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Behavioral indicator not found with id: " + nonExistentId + " for competency: " + competencyId);
            
            verify(behavioralIndicatorRepository).findByIdAndCompetencyId(nonExistentId, competencyId);
            verify(behavioralIndicatorRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should throw exception when competency ID doesn't match")
        void shouldThrowExceptionWhenCompetencyIdDoesNotMatch() {
            // Given
            UUID wrongCompetencyId = UUID.randomUUID();
            when(behavioralIndicatorRepository.findByIdAndCompetencyId(behavioralIndicatorId, wrongCompetencyId))
                .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> behavioralIndicatorService
                .deleteBehavioralIndicator(wrongCompetencyId, behavioralIndicatorId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Behavioral indicator not found with id: " + behavioralIndicatorId + " for competency: " + wrongCompetencyId);
            
            verify(behavioralIndicatorRepository).findByIdAndCompetencyId(behavioralIndicatorId, wrongCompetencyId);
            verify(behavioralIndicatorRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("Russian Content Handling Tests")
    class RussianContentHandlingTests {

        @Test
        @DisplayName("Should handle Russian text in behavioral indicator creation")
        void shouldHandleRussianTextInBehavioralIndicatorCreation() {
            // Given
            BehavioralIndicator russianIndicator = new BehavioralIndicator();
            russianIndicator.setTitle("Лидерские качества");
            russianIndicator.setDescription("Способность эффективно руководить командой");
            russianIndicator.setExamples("Демонстрирует уверенность, принимает решения");
            russianIndicator.setCounterExamples("Избегает ответственности, неуверен в решениях");

            when(competencyRepository.findById(competencyId)).thenReturn(Optional.of(mockCompetency));
            when(behavioralIndicatorRepository.save(any(BehavioralIndicator.class))).thenReturn(russianIndicator);

            // When
            BehavioralIndicator createdIndicator = behavioralIndicatorService
                .createBehavioralIndicator(competencyId, russianIndicator);

            // Then
            assertThat(createdIndicator).isNotNull();
            assertThat(createdIndicator.getTitle()).isEqualTo("Лидерские качества");
            assertThat(createdIndicator.getDescription()).isEqualTo("Способность эффективно руководить командой");
            assertThat(createdIndicator.getExamples()).isEqualTo("Демонстрирует уверенность, принимает решения");
            assertThat(createdIndicator.getCounterExamples()).isEqualTo("Избегает ответственности, неуверен в решениях");
        }

        @Test
        @DisplayName("Should handle mixed Russian and English text")
        void shouldHandleMixedRussianAndEnglishText() {
            // Given
            BehavioralIndicator mixedIndicator = new BehavioralIndicator();
            mixedIndicator.setTitle("Leadership - Лидерство");
            mixedIndicator.setDescription("Ability to lead teams / Способность руководить командами");

            when(competencyRepository.findById(competencyId)).thenReturn(Optional.of(mockCompetency));
            when(behavioralIndicatorRepository.save(any(BehavioralIndicator.class))).thenReturn(mixedIndicator);

            // When
            BehavioralIndicator createdIndicator = behavioralIndicatorService
                .createBehavioralIndicator(competencyId, mixedIndicator);

            // Then
            assertThat(createdIndicator).isNotNull();
            assertThat(createdIndicator.getTitle()).isEqualTo("Leadership - Лидерство");
            assertThat(createdIndicator.getDescription()).isEqualTo("Ability to lead teams / Способность руководить командами");
        }
    }

    @Nested
    @DisplayName("Business Logic Edge Cases Tests")
    class BusinessLogicEdgeCasesTests {

        @Test
        @DisplayName("Should handle indicators with extreme weight values")
        void shouldHandleIndicatorsWithExtremeWeightValues() {
            // Given
            BehavioralIndicator lowWeightIndicator = new BehavioralIndicator();
            lowWeightIndicator.setTitle("Low Weight Indicator");
            lowWeightIndicator.setWeight(0.0f);

            BehavioralIndicator highWeightIndicator = new BehavioralIndicator();
            highWeightIndicator.setTitle("High Weight Indicator");
            highWeightIndicator.setWeight(1.0f);

            when(competencyRepository.findById(competencyId)).thenReturn(Optional.of(mockCompetency));
            when(behavioralIndicatorRepository.save(any(BehavioralIndicator.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When & Then
            assertThatCode(() -> behavioralIndicatorService.createBehavioralIndicator(competencyId, lowWeightIndicator))
                .doesNotThrowAnyException();
            assertThatCode(() -> behavioralIndicatorService.createBehavioralIndicator(competencyId, highWeightIndicator))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle indicators with all enum values")
        void shouldHandleIndicatorsWithAllEnumValues() {
            // Given
            when(competencyRepository.findById(competencyId)).thenReturn(Optional.of(mockCompetency));
            when(behavioralIndicatorRepository.save(any(BehavioralIndicator.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // Test all ProficiencyLevel values
            for (ProficiencyLevel level : ProficiencyLevel.values()) {
                BehavioralIndicator indicator = new BehavioralIndicator();
                indicator.setTitle("Test " + level.name());
                indicator.setObservabilityLevel(level);

                // When & Then
                assertThatCode(() -> behavioralIndicatorService.createBehavioralIndicator(competencyId, indicator))
                    .doesNotThrowAnyException();
            }

            // Test all IndicatorMeasurementType values
            for (IndicatorMeasurementType type : IndicatorMeasurementType.values()) {
                BehavioralIndicator indicator = new BehavioralIndicator();
                indicator.setTitle("Test " + type.name());
                indicator.setMeasurementType(type);

                // When & Then
                assertThatCode(() -> behavioralIndicatorService.createBehavioralIndicator(competencyId, indicator))
                    .doesNotThrowAnyException();
            }

            // Test all ApprovalStatus values
            for (ApprovalStatus status : ApprovalStatus.values()) {
                BehavioralIndicator indicator = new BehavioralIndicator();
                indicator.setTitle("Test " + status.name());
                indicator.setApprovalStatus(status);

                // When & Then
                assertThatCode(() -> behavioralIndicatorService.createBehavioralIndicator(competencyId, indicator))
                    .doesNotThrowAnyException();
            }
        }
    }

    /**
     * Helper method to create test behavioral indicators
     */
    private BehavioralIndicator createTestIndicator(String title, int orderIndex) {
        BehavioralIndicator indicator = new BehavioralIndicator();
        indicator.setId(UUID.randomUUID());
        indicator.setCompetency(mockCompetency);
        indicator.setTitle(title);
        indicator.setDescription("Description for " + title);
        indicator.setObservabilityLevel(ProficiencyLevel.PROFICIENT);
        indicator.setMeasurementType(IndicatorMeasurementType.QUALITY);
        indicator.setWeight(0.5f);
        indicator.setActive(true);
        indicator.setApprovalStatus(ApprovalStatus.APPROVED);
        indicator.setOrderIndex(orderIndex);
        return indicator;
    }
}