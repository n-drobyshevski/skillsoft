package app.skillsoft.assessmentbackend.service;

import app.skillsoft.assessmentbackend.domain.dto.StandardCodesDto;
import app.skillsoft.assessmentbackend.domain.entities.ApprovalStatus;
import app.skillsoft.assessmentbackend.domain.entities.Competency;
import app.skillsoft.assessmentbackend.domain.entities.CompetencyCategory;
import app.skillsoft.assessmentbackend.domain.entities.ProficiencyLevel;
import app.skillsoft.assessmentbackend.repository.CompetencyRepository;
import app.skillsoft.assessmentbackend.services.impl.CompetencyServiceImpl;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
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
 * Unit tests for CompetencyService
 * Tests service layer business logic with mocked dependencies:
 * - Repository layer mocking
 * - Mapper functionality validation
 * - Business rule enforcement
 * - Error handling scenarios
 * - Russian content processing
 * - Standard codes handling
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Competency Service Unit Tests")
class CompetencyServiceTest {

    @Mock
    private CompetencyRepository competencyRepository;

    @InjectMocks
    private CompetencyServiceImpl competencyService;

    private Competency sampleCompetency;
    private StandardCodesDto standardCodes;

    @BeforeEach
    void setUp() {
        // Setup standard codes using the new DTO structure
        standardCodes = StandardCodesDto.builder()
                .escoRef("http://data.europa.eu/esco/skill/abc123-def456-789",
                        "develop organisational strategies", "skill")
                .globalCategory("leadership", "strategic_thinking", null)
                .build();

        // Setup sample competency entity
        sampleCompetency = new Competency();
        sampleCompetency.setId(UUID.randomUUID());
        sampleCompetency.setName("Стратегическое планирование");
        sampleCompetency.setDescription("Способность разрабатывать долгосрочные стратегии организации");
        sampleCompetency.setCategory(CompetencyCategory.LEADERSHIP);
        sampleCompetency.setLevel(ProficiencyLevel.ADVANCED);
        sampleCompetency.setStandardCodes(standardCodes);
        sampleCompetency.setActive(true);
        sampleCompetency.setApprovalStatus(ApprovalStatus.APPROVED);
        sampleCompetency.setVersion(1);
        sampleCompetency.setCreatedAt(LocalDateTime.now());
        sampleCompetency.setLastModified(LocalDateTime.now());


    }

    @Nested
    @DisplayName("Create Competency Tests")
    class CreateCompetencyTests {

        @Test
        @DisplayName("Should create competency with Russian content and standard codes")
        void shouldCreateCompetencyWithRussianContentAndStandardCodes() {
            // Given
            Competency createEntity = new Competency();
            createEntity.setName("Эмоциональный интеллект");
            createEntity.setDescription("Способность понимать и управлять эмоциями");
            createEntity.setCategory(CompetencyCategory.EMOTIONAL_INTELLIGENCE);
            createEntity.setLevel(ProficiencyLevel.PROFICIENT);
            createEntity.setStandardCodes(standardCodes);
            createEntity.setActive(true);
            createEntity.setApprovalStatus(ApprovalStatus.DRAFT);

            Competency savedEntity = new Competency();
            savedEntity.setId(UUID.randomUUID());
            savedEntity.setName("Эмоциональный интеллект");
            savedEntity.setDescription("Способность понимать и управлять эмоциями");
            savedEntity.setCategory(CompetencyCategory.EMOTIONAL_INTELLIGENCE);
            savedEntity.setLevel(ProficiencyLevel.PROFICIENT);
            savedEntity.setStandardCodes(standardCodes);
            savedEntity.setActive(true);
            savedEntity.setApprovalStatus(ApprovalStatus.DRAFT);
            savedEntity.setVersion(1);
            savedEntity.setCreatedAt(LocalDateTime.now());
            savedEntity.setLastModified(LocalDateTime.now());

            when(competencyRepository.save(any(Competency.class))).thenReturn(savedEntity);

            // When
            Competency result = competencyService.createCompetency(createEntity);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Эмоциональный интеллект");
            assertThat(result.getDescription()).contains("эмоциями");
            assertThat(result.getCategory()).isEqualTo(CompetencyCategory.EMOTIONAL_INTELLIGENCE);
            assertThat(result.getLevel()).isEqualTo(ProficiencyLevel.PROFICIENT);
            assertThat(result.getStandardCodes()).isNotNull();
            assertThat(result.getStandardCodes().escoRef()).isNotNull();
            assertThat(result.isActive()).isTrue();
            assertThat(result.getVersion()).isEqualTo(1);
            assertThat(result.getId()).isNotNull();

            verify(competencyRepository).save(any(Competency.class));
        }

        @Test
        @DisplayName("Should set default values when creating competency")
        void shouldSetDefaultValuesWhenCreatingCompetency() {
            // Given
            Competency createEntity = new Competency();
            createEntity.setName("Базовая компетенция");
            createEntity.setDescription("Простое описание");
            createEntity.setCategory(CompetencyCategory.CRITICAL_THINKING);
            createEntity.setLevel(ProficiencyLevel.NOVICE);

            // When
            ArgumentCaptor<Competency> competencyCaptor = ArgumentCaptor.forClass(Competency.class);
            when(competencyRepository.save(competencyCaptor.capture())).thenAnswer(invocation -> {
                Competency saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                saved.setCreatedAt(LocalDateTime.now());
                saved.setLastModified(LocalDateTime.now());
                return saved;
            });

            competencyService.createCompetency(createEntity);

            // Then
            Competency capturedEntity = competencyCaptor.getValue();
            assertThat(capturedEntity.getVersion()).isEqualTo(1);
            assertThat(capturedEntity.getCreatedAt()).isNotNull();
            assertThat(capturedEntity.getLastModified()).isNotNull();
            
            verify(competencyRepository).save(any(Competency.class));
        }
    }

    @Nested
    @DisplayName("Get All Competencies Tests")
    class GetAllCompetenciesTests {

        @Test
        @DisplayName("Should return all competencies")
        void shouldReturnAllCompetencies() {
            // Given
            List<Competency> entities = Collections.singletonList(sampleCompetency);
            when(competencyRepository.findAll()).thenReturn(entities);

            // When
            List<Competency> result = competencyService.listCompetencies();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getName()).isEqualTo("Стратегическое планирование");
            assertThat(result.getFirst().getStandardCodes().hasEscoMapping()).isTrue();

            verify(competencyRepository).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no competencies exist")
        void shouldReturnEmptyListWhenNoCompetenciesExist() {
            // Given
            when(competencyRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            List<Competency> result = competencyService.listCompetencies();

            // Then
            assertThat(result).isEmpty();
            verify(competencyRepository).findAll();
        }
    }

    @Nested
    @DisplayName("Get Competency By ID Tests")
    class GetCompetencyByIdTests {

        @Test
        @DisplayName("Should return competency when ID exists")
        void shouldReturnCompetencyWhenIdExists() {
            // Given
            UUID competencyId = sampleCompetency.getId();
            when(competencyRepository.findById(competencyId)).thenReturn(Optional.of(sampleCompetency));

            // When
            Optional<Competency> result = competencyService.findCompetencyById(competencyId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(competencyId);
            assertThat(result.get().getName()).isEqualTo("Стратегическое планирование");
            assertThat(result.get().getStandardCodes().hasEscoMapping()).isTrue();

            verify(competencyRepository).findById(competencyId);
        }

        @Test
        @DisplayName("Should return empty when ID does not exist")
        void shouldReturnEmptyWhenIdDoesNotExist() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(competencyRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When
            Optional<Competency> result = competencyService.findCompetencyById(nonExistentId);

            // Then
            assertThat(result).isEmpty();
            verify(competencyRepository).findById(nonExistentId);
        }
    }

    @Nested
    @DisplayName("Update Competency Tests")
    class UpdateCompetencyTests {

        @Test
        @DisplayName("Should update competency with new Russian content and increment version")
        void shouldUpdateCompetencyWithNewRussianContentAndIncrementVersion() {
            // Given
            UUID competencyId = sampleCompetency.getId();
            
            StandardCodesDto newStandardCodes = StandardCodesDto.builder()
                    .escoRef("http://data.europa.eu/esco/skill/def456-ghi789-012",
                            "demonstrate empathy", "skill")
                    .globalCategory("emotional_intelligence", "empathy", null)
                    .build();

            Competency updateEntity = new Competency();
            updateEntity.setId(competencyId);
            updateEntity.setName("Обновленное название");
            updateEntity.setDescription("Обновленное описание: новая функциональность");
            updateEntity.setCategory(CompetencyCategory.EMOTIONAL_INTELLIGENCE);
            updateEntity.setLevel(ProficiencyLevel.EXPERT);
            updateEntity.setStandardCodes(newStandardCodes);
            updateEntity.setActive(false);
            updateEntity.setApprovalStatus(ApprovalStatus.PENDING_REVIEW);

            Competency updatedEntity = new Competency();
            updatedEntity.setId(competencyId);
            updatedEntity.setName("Обновленное название");
            updatedEntity.setDescription("Обновленное описание: новая функциональность");
            updatedEntity.setCategory(CompetencyCategory.EMOTIONAL_INTELLIGENCE);
            updatedEntity.setLevel(ProficiencyLevel.EXPERT);
            updatedEntity.setStandardCodes(newStandardCodes);
            updatedEntity.setActive(false);
            updatedEntity.setApprovalStatus(ApprovalStatus.PENDING_REVIEW);
            updatedEntity.setVersion(2); // Incremented version
            updatedEntity.setCreatedAt(sampleCompetency.getCreatedAt());
            updatedEntity.setLastModified(LocalDateTime.now());

            when(competencyRepository.findById(competencyId)).thenReturn(Optional.of(sampleCompetency));
            when(competencyRepository.save(any(Competency.class))).thenReturn(updatedEntity);

            // When
            Competency result = competencyService.updateCompetency(competencyId, updateEntity);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Обновленное название");
            assertThat(result.getDescription()).contains("новая функциональность");
            assertThat(result.getVersion()).isEqualTo(2);
            assertThat(result.getStandardCodes().escoRef()).isNotNull();

            verify(competencyRepository).findById(competencyId);
            verify(competencyRepository).save(any(Competency.class));
        }

        @Test
        @DisplayName("Should throw exception when updating non-existent competency")
        void shouldThrowExceptionWhenUpdatingNonExistentCompetency() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            Competency updateEntity = new Competency();
            updateEntity.setName("Test");
            updateEntity.setCategory(CompetencyCategory.COGNITIVE);
            when(competencyRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> {
                // Test the specific method call that should throw the exception
                competencyService.updateCompetency(nonExistentId, updateEntity);
            })
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Competency not found with id: " + nonExistentId);

            // Verify that repository was queried but no save was attempted
            verify(competencyRepository).findById(nonExistentId);
            verify(competencyRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Delete Competency Tests")
    class DeleteCompetencyTests {

        @Test
        @DisplayName("Should delete competency when ID exists")
        void shouldDeleteCompetencyWhenIdExists() {
            // Given
            UUID competencyId = sampleCompetency.getId();
            // Note: No need to stub existsById since service doesn't use it

            // When
            competencyService.deleteCompetency(competencyId);

            // Then
            verify(competencyRepository).deleteById(competencyId);
        }

        @Test
        @DisplayName("Should delete even when competency does not exist")
        void shouldDeleteEvenWhenCompetencyDoesNotExist() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            // Note: No need to stub existsById since service doesn't use it

            // When
            competencyService.deleteCompetency(nonExistentId);

            // Then
            verify(competencyRepository).deleteById(nonExistentId);
        }
    }

    @Nested
    @DisplayName("Standard Codes Handling Tests")
    class StandardCodesHandlingTests {

        @Test
        @DisplayName("Should handle complex standard codes structure")
        void shouldHandleComplexStandardCodesStructure() {
            // Given
            StandardCodesDto complexStandardCodes = StandardCodesDto.builder()
                    .escoRef("http://data.europa.eu/esco/skill/abc123-def456-789",
                            "communicate with others", "skill")
                    .onetRef("2.A.1.b", "Oral Comprehension", "ability")
                    .globalCategory("big_five", "extraversion", null)
                    .build();

            Competency createEntity = new Competency();
            createEntity.setName("Комплексная коммуникация");
            createEntity.setDescription("Многоуровневые навыки коммуникации");
            createEntity.setCategory(CompetencyCategory.COMMUNICATION);
            createEntity.setLevel(ProficiencyLevel.EXPERT);
            createEntity.setStandardCodes(complexStandardCodes);
            createEntity.setActive(true);
            createEntity.setApprovalStatus(ApprovalStatus.APPROVED);

            when(competencyRepository.save(any())).thenReturn(sampleCompetency);

            // When
            Competency result = competencyService.createCompetency(createEntity);

            // Then
            assertThat(result).isNotNull();
            verify(competencyRepository).save(any(Competency.class));
        }

        @Test
        @DisplayName("Should handle null standard codes")
        void shouldHandleNullStandardCodes() {
            // Given
            Competency createEntity = new Competency();
            createEntity.setName("Простая компетенция");
            createEntity.setDescription("Без стандартных кодов");
            createEntity.setCategory(CompetencyCategory.COGNITIVE);
            createEntity.setLevel(ProficiencyLevel.NOVICE);
            createEntity.setStandardCodes(null); // Null standard codes
            createEntity.setActive(true);
            createEntity.setApprovalStatus(ApprovalStatus.DRAFT);

            when(competencyRepository.save(any())).thenReturn(sampleCompetency);

            // When
            Competency result = competencyService.createCompetency(createEntity);

            // Then
            assertThat(result).isNotNull();
            verify(competencyRepository).save(any(Competency.class));
        }
    }
}