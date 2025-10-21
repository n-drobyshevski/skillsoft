package app.skillsoft.assessmentbackend.repository;

import app.skillsoft.assessmentbackend.domain.entities.ApprovalStatus;
import app.skillsoft.assessmentbackend.domain.entities.Competency;
import app.skillsoft.assessmentbackend.domain.entities.CompetencyCategory;
import app.skillsoft.assessmentbackend.domain.entities.ProficiencyLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Repository tests for CompetencyRepository
 * 
 * Tests JPA repository functionality:
 * - Basic CRUD operations
 * - Custom query methods (if any)
 * - Database constraints validation
 * - JSONB standard codes persistence
 * - Russian text handling in database
 * - Entity lifecycle management
 */
@DataJpaTest
@DisplayName("Competency Repository Tests")
class CompetencyRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CompetencyRepository competencyRepository;

    private Map<String, Object> standardCodes;
    private Competency sampleCompetency;

    @BeforeEach
    void setUp() {
        // Setup standard codes structure
        standardCodes = new HashMap<>();
        Map<String, Object> escoMapping = new HashMap<>();
        escoMapping.put("code", "S7.1.1");
        escoMapping.put("name", "develop organisational strategies");
        escoMapping.put("confidence", "HIGH");
        standardCodes.put("ESCO", escoMapping);

        // Create sample competency
        sampleCompetency = new Competency();
        sampleCompetency.setName("Стратегическое лидерство");
        sampleCompetency.setDescription("Способность вести команду к достижению стратегических целей");
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
    @DisplayName("Basic CRUD Operations")
    class BasicCrudOperationsTests {

        @Test
        @DisplayName("Should save competency with Russian content and standard codes")
        void shouldSaveCompetencyWithRussianContentAndStandardCodes() {
            // When
            Competency saved = competencyRepository.save(sampleCompetency);
            entityManager.flush();

            // Then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getName()).isEqualTo("Стратегическое лидерство");
            assertThat(saved.getDescription()).contains("команду");
            assertThat(saved.getStandardCodes()).isNotNull();
            assertThat(saved.getStandardCodes().get("ESCO")).isNotNull();
            
            @SuppressWarnings("unchecked")
            Map<String, Object> escoData = (Map<String, Object>) saved.getStandardCodes().get("ESCO");
            assertThat(escoData.get("code")).isEqualTo("S7.1.1");
            assertThat(escoData.get("confidence")).isEqualTo("HIGH");
        }

        @Test
        @DisplayName("Should find competency by ID")
        void shouldFindCompetencyById() {
            // Given
            Competency saved = entityManager.persistAndFlush(sampleCompetency);
            UUID competencyId = saved.getId();

            // When
            Optional<Competency> found = competencyRepository.findById(competencyId);

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("Стратегическое лидерство");
            assertThat(found.get().getStandardCodes()).containsKey("ESCO");
        }

        @Test
        @DisplayName("Should find all competencies")
        void shouldFindAllCompetencies() {
            // Given
            Competency secondCompetency = new Competency();
            secondCompetency.setName("Эмоциональный интеллект");
            secondCompetency.setDescription("Способность понимать эмоции");
            secondCompetency.setCategory(CompetencyCategory.EMOTIONAL_INTELLIGENCE);
            secondCompetency.setLevel(ProficiencyLevel.PROFICIENT);
            secondCompetency.setActive(true);
            secondCompetency.setApprovalStatus(ApprovalStatus.DRAFT);
            secondCompetency.setVersion(1);
            secondCompetency.setCreatedAt(LocalDateTime.now());
            secondCompetency.setLastModified(LocalDateTime.now());

            entityManager.persistAndFlush(sampleCompetency);
            entityManager.persistAndFlush(secondCompetency);

            // When
            List<Competency> all = competencyRepository.findAll();

            // Then
            assertThat(all).hasSize(2);
            assertThat(all).extracting("name")
                    .containsExactlyInAnyOrder("Стратегическое лидерство", "Эмоциональный интеллект");
        }

        @Test
        @DisplayName("Should update competency")
        void shouldUpdateCompetency() {
            // Given
            Competency saved = entityManager.persistAndFlush(sampleCompetency);
            entityManager.clear();

            // When
            Optional<Competency> toUpdate = competencyRepository.findById(saved.getId());
            assertThat(toUpdate).isPresent();
            
            Competency competency = toUpdate.get();
            competency.setName("Обновленное лидерство");
            competency.setDescription("Обновленное описание лидерских навыков");
            competency.setLevel(ProficiencyLevel.EXPERT);
            competency.setVersion(2);
            competency.setLastModified(LocalDateTime.now());

            Map<String, Object> newStandardCodes = new HashMap<>();
            Map<String, Object> newEsco = new HashMap<>();
            newEsco.put("code", "S8.2.1");
            newEsco.put("name", "updated leadership skills");
            newEsco.put("confidence", "VERIFIED");
            newStandardCodes.put("ESCO", newEsco);
            competency.setStandardCodes(newStandardCodes);

            Competency updated = competencyRepository.save(competency);
            entityManager.flush();

            // Then
            assertThat(updated.getName()).isEqualTo("Обновленное лидерство");
            assertThat(updated.getDescription()).contains("Обновленное");
            assertThat(updated.getLevel()).isEqualTo(ProficiencyLevel.EXPERT);
            assertThat(updated.getVersion()).isEqualTo(2);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> updatedEsco = (Map<String, Object>) updated.getStandardCodes().get("ESCO");
            assertThat(updatedEsco.get("code")).isEqualTo("S8.2.1");
            assertThat(updatedEsco.get("confidence")).isEqualTo("VERIFIED");
        }

        @Test
        @DisplayName("Should delete competency")
        void shouldDeleteCompetency() {
            // Given
            Competency saved = entityManager.persistAndFlush(sampleCompetency);
            UUID competencyId = saved.getId();

            // When
            competencyRepository.deleteById(competencyId);
            entityManager.flush();

            // Then
            Optional<Competency> deleted = competencyRepository.findById(competencyId);
            assertThat(deleted).isEmpty();
        }
    }

    @Nested
    @DisplayName("Standard Codes JSONB Tests")
    class StandardCodesJsonbTests {

        @Test
        @DisplayName("Should persist complex standard codes structure")
        void shouldPersistComplexStandardCodesStructure() {
            // Given
            Map<String, Object> complexStandardCodes = new HashMap<>();
            
            Map<String, Object> esco = new HashMap<>();
            esco.put("code", "S2.1.1");
            esco.put("name", "communicate with others");
            esco.put("confidence", "HIGH");
            esco.put("lastUpdated", "2024-01-15");
            complexStandardCodes.put("ESCO", esco);
            
            Map<String, Object> onet = new HashMap<>();
            onet.put("code", "2.A.1.b");
            onet.put("name", "Oral Comprehension");
            onet.put("confidence", "VERIFIED");
            onet.put("category", "Abilities");
            complexStandardCodes.put("ONET", onet);
            
            Map<String, Object> bigFive = new HashMap<>();
            bigFive.put("code", "EXTRAVERSION");
            bigFive.put("name", "Extraversion traits");
            bigFive.put("confidence", "MODERATE");
            Map<String, Object> facets = new HashMap<>();
            facets.put("assertiveness", "HIGH");
            facets.put("sociability", "MODERATE");
            bigFive.put("facets", facets);
            complexStandardCodes.put("BIG_FIVE", bigFive);

            sampleCompetency.setStandardCodes(complexStandardCodes);

            // When
            Competency saved = competencyRepository.save(sampleCompetency);
            entityManager.flush();
            entityManager.clear();

            // Then
            Optional<Competency> retrieved = competencyRepository.findById(saved.getId());
            assertThat(retrieved).isPresent();
            
            Map<String, Object> retrievedCodes = retrieved.get().getStandardCodes();
            assertThat(retrievedCodes).hasSize(3);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> retrievedEsco = (Map<String, Object>) retrievedCodes.get("ESCO");
            assertThat(retrievedEsco.get("code")).isEqualTo("S2.1.1");
            assertThat(retrievedEsco.get("lastUpdated")).isEqualTo("2024-01-15");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> retrievedOnet = (Map<String, Object>) retrievedCodes.get("ONET");
            assertThat(retrievedOnet.get("category")).isEqualTo("Abilities");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> retrievedBigFive = (Map<String, Object>) retrievedCodes.get("BIG_FIVE");
            @SuppressWarnings("unchecked")
            Map<String, Object> retrievedFacets = (Map<String, Object>) retrievedBigFive.get("facets");
            assertThat(retrievedFacets.get("assertiveness")).isEqualTo("HIGH");
            assertThat(retrievedFacets.get("sociability")).isEqualTo("MODERATE");
        }

        @Test
        @DisplayName("Should handle null standard codes")
        void shouldHandleNullStandardCodes() {
            // Given
            sampleCompetency.setStandardCodes(null);

            // When
            Competency saved = competencyRepository.save(sampleCompetency);
            entityManager.flush();
            entityManager.clear();

            // Then
            Optional<Competency> retrieved = competencyRepository.findById(saved.getId());
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getStandardCodes()).isNull();
        }

        @Test
        @DisplayName("Should handle empty standard codes map")
        void shouldHandleEmptyStandardCodesMap() {
            // Given
            sampleCompetency.setStandardCodes(new HashMap<>());

            // When
            Competency saved = competencyRepository.save(sampleCompetency);
            entityManager.flush();
            entityManager.clear();

            // Then
            Optional<Competency> retrieved = competencyRepository.findById(saved.getId());
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getStandardCodes()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Russian Content Tests")
    class RussianContentTests {

        @Test
        @DisplayName("Should handle various Russian characters and special symbols")
        void shouldHandleVariousRussianCharactersAndSpecialSymbols() {
            // Given
            sampleCompetency.setName("Комплексная компетенция №1: Эффективность");
            sampleCompetency.setDescription("Описание включает: цифры (123), символы (!@#$%^&*), " +
                    "кавычки (\"текст\"), апострофы ('текст'), тире — и дефисы - и многоточие...");

            // When
            Competency saved = competencyRepository.save(sampleCompetency);
            entityManager.flush();
            entityManager.clear();

            // Then
            Optional<Competency> retrieved = competencyRepository.findById(saved.getId());
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getName()).isEqualTo("Комплексная компетенция №1: Эффективность");
            assertThat(retrieved.get().getDescription()).contains("кавычки");
            assertThat(retrieved.get().getDescription()).contains("многоточие");
        }

        @Test
        @DisplayName("Should handle long Russian text")
        void shouldHandleLongRussianText() {
            // Given
            String longDescription = "Очень длинное описание компетенции на русском языке. ".repeat(50) +
                    "Этот текст содержит множество повторяющихся фраз для проверки обработки больших объемов текста. " +
                    "Компетенция должна корректно сохраняться и извлекаться из базы данных независимо от длины описания.";
            
            sampleCompetency.setDescription(longDescription);

            // When
            Competency saved = competencyRepository.save(sampleCompetency);
            entityManager.flush();
            entityManager.clear();

            // Then
            Optional<Competency> retrieved = competencyRepository.findById(saved.getId());
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getDescription()).isEqualTo(longDescription);
            assertThat(retrieved.get().getDescription().length()).isGreaterThan(1000);
        }
    }

    @Nested
    @DisplayName("Entity Lifecycle Tests")
    class EntityLifecycleTests {

        @Test
        @DisplayName("Should automatically set ID when saving new competency")
        void shouldAutomaticallySetIdWhenSavingNewCompetency() {
            // Given
            assertThat(sampleCompetency.getId()).isNull();

            // When
            Competency saved = competencyRepository.save(sampleCompetency);

            // Then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getId()).isInstanceOf(UUID.class);
        }

        @Test
        @DisplayName("Should preserve creation and modification timestamps")
        void shouldPreserveCreationAndModificationTimestamps() {
            // Given
            LocalDateTime beforeSave = LocalDateTime.now().minusSeconds(1);
            
            // When
            Competency saved = competencyRepository.save(sampleCompetency);
            entityManager.flush();
            
            LocalDateTime afterSave = LocalDateTime.now().plusSeconds(1);

            // Then
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getLastModified()).isNotNull();
            assertThat(saved.getCreatedAt()).isBetween(beforeSave, afterSave);
            assertThat(saved.getLastModified()).isBetween(beforeSave, afterSave);
        }

        @Test
        @DisplayName("Should maintain data integrity across multiple operations")
        void shouldMaintainDataIntegrityAcrossMultipleOperations() {
            // Given - Save initial competency
            Competency saved = competencyRepository.save(sampleCompetency);
            entityManager.flush();
            UUID originalId = saved.getId();
            int originalVersion = saved.getVersion();

            // When - Update the competency
            saved.setName("Обновленное название");
            saved.setVersion(originalVersion + 1);
            Competency updated = competencyRepository.save(saved);
            entityManager.flush();

            // Then - Verify integrity
            assertThat(updated.getId()).isEqualTo(originalId);
            assertThat(updated.getVersion()).isEqualTo(originalVersion + 1);
            assertThat(updated.getName()).isEqualTo("Обновленное название");
            
            // And - Verify retrieval
            Optional<Competency> retrieved = competencyRepository.findById(originalId);
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get().getName()).isEqualTo("Обновленное название");
            assertThat(retrieved.get().getVersion()).isEqualTo(originalVersion + 1);
        }
    }

    @Nested
    @DisplayName("Query Performance Tests")
    class QueryPerformanceTests {

        @Test
        @DisplayName("Should efficiently handle multiple competencies with large standard codes")
        void shouldEfficientlyHandleMultipleCompetenciesWithLargeStandardCodes() {
            // Given - Create multiple competencies with large standard codes
            for (int i = 0; i < 10; i++) {
                Competency competency = new Competency();
                competency.setName("Компетенция " + (i + 1));
                competency.setDescription("Описание компетенции номер " + (i + 1));
                competency.setCategory(CompetencyCategory.values()[i % CompetencyCategory.values().length]);
                competency.setLevel(ProficiencyLevel.values()[i % ProficiencyLevel.values().length]);
                competency.setActive(true);
                competency.setApprovalStatus(ApprovalStatus.APPROVED);
                competency.setVersion(1);
                competency.setCreatedAt(LocalDateTime.now());
                competency.setLastModified(LocalDateTime.now());

                // Create large standard codes
                Map<String, Object> largeStandardCodes = new HashMap<>();
                for (int j = 0; j < 5; j++) {
                    Map<String, Object> standardData = new HashMap<>();
                    standardData.put("code", "CODE_" + i + "_" + j);
                    standardData.put("name", "Standard " + j + " for competency " + i);
                    standardData.put("confidence", "HIGH");
                    standardData.put("description", "Detailed description ".repeat(10));
                    largeStandardCodes.put("STANDARD_" + j, standardData);
                }
                competency.setStandardCodes(largeStandardCodes);

                entityManager.persist(competency);
            }
            entityManager.flush();
            entityManager.clear();

            // When - Retrieve all competencies
            long startTime = System.currentTimeMillis();
            List<Competency> all = competencyRepository.findAll();
            long endTime = System.currentTimeMillis();

            // Then - Verify performance and data integrity
            assertThat(all).hasSize(10);
            assertThat(endTime - startTime).isLessThan(1000); // Should complete within 1 second
            
            for (Competency competency : all) {
                assertThat(competency.getStandardCodes()).isNotNull();
                assertThat(competency.getStandardCodes()).hasSize(5);
            }
        }
    }
}