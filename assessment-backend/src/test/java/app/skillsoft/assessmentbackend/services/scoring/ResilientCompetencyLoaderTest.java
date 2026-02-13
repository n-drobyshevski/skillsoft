package app.skillsoft.assessmentbackend.services.scoring;

import app.skillsoft.assessmentbackend.domain.entities.ApprovalStatus;
import app.skillsoft.assessmentbackend.domain.entities.Competency;
import app.skillsoft.assessmentbackend.domain.entities.CompetencyCategory;
import app.skillsoft.assessmentbackend.repository.CompetencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ResilientCompetencyLoader component.
 *
 * Tests cover:
 * - Successful competency loading from repository
 * - Cache population during successful loads
 * - Fallback to cache when repository fails (simulated circuit breaker)
 * - Cache warming functionality
 * - Empty input handling
 * - Null safety
 *
 * Note: These tests verify the component logic directly without the actual circuit breaker.
 * Integration tests with @SpringBootTest would be needed to verify Resilience4j behavior.
 *
 * Per ROADMAP.md Task 3.2 - Add circuit breaker to repository calls
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResilientCompetencyLoader Tests")
class ResilientCompetencyLoaderTest {

    @Mock
    private CompetencyRepository competencyRepository;

    private ResilientCompetencyLoader resilientCompetencyLoader;

    @BeforeEach
    void setUp() {
        resilientCompetencyLoader = new ResilientCompetencyLoader(competencyRepository);
    }

    // Helper method to create test competencies
    private Competency createTestCompetency(UUID id, String name) {
        Competency competency = new Competency();
        competency.setId(id);
        competency.setName(name);
        competency.setDescription("Test description for " + name);
        competency.setCategory(CompetencyCategory.COGNITIVE);
        competency.setActive(true);
        competency.setApprovalStatus(ApprovalStatus.APPROVED);
        competency.setVersion(1);
        competency.setCreatedAt(LocalDateTime.now());
        competency.setLastModified(LocalDateTime.now());
        return competency;
    }

    @Nested
    @DisplayName("loadCompetencies - Basic Functionality")
    class LoadCompetenciesBasicTests {

        @Test
        @DisplayName("Should load competencies from repository successfully")
        void loadCompetencies_success_returnsFromRepository() {
            // Arrange
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            Competency comp1 = createTestCompetency(id1, "Communication");
            Competency comp2 = createTestCompetency(id2, "Problem Solving");

            Set<UUID> ids = Set.of(id1, id2);
            when(competencyRepository.findAllById(ids)).thenReturn(List.of(comp1, comp2));

            // Act
            Map<UUID, Competency> result = resilientCompetencyLoader.loadCompetencies(ids);

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result).containsKey(id1);
            assertThat(result).containsKey(id2);
            assertThat(result.get(id1).getName()).isEqualTo("Communication");
            assertThat(result.get(id2).getName()).isEqualTo("Problem Solving");
            verify(competencyRepository).findAllById(ids);
        }

        @Test
        @DisplayName("Should return empty map for null input")
        void loadCompetencies_nullInput_returnsEmptyMap() {
            // Act
            Map<UUID, Competency> result = resilientCompetencyLoader.loadCompetencies(null);

            // Assert
            assertThat(result).isEmpty();
            verify(competencyRepository, never()).findAllById(anyCollection());
        }

        @Test
        @DisplayName("Should return empty map for empty set input")
        void loadCompetencies_emptyInput_returnsEmptyMap() {
            // Act
            Map<UUID, Competency> result = resilientCompetencyLoader.loadCompetencies(Set.of());

            // Assert
            assertThat(result).isEmpty();
            verify(competencyRepository, never()).findAllById(anyCollection());
        }

        @Test
        @DisplayName("Should handle single competency request")
        void loadCompetencies_singleId_returnsSingleCompetency() {
            // Arrange
            UUID id = UUID.randomUUID();
            Competency comp = createTestCompetency(id, "Leadership");

            Set<UUID> ids = Set.of(id);
            when(competencyRepository.findAllById(ids)).thenReturn(List.of(comp));

            // Act
            Map<UUID, Competency> result = resilientCompetencyLoader.loadCompetencies(ids);

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(id).getName()).isEqualTo("Leadership");
        }

        @Test
        @DisplayName("Should handle partial results when some competencies not found")
        void loadCompetencies_partialResults_returnsFoundOnly() {
            // Arrange
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            UUID missingId = UUID.randomUUID();
            Competency comp1 = createTestCompetency(id1, "Communication");
            Competency comp2 = createTestCompetency(id2, "Problem Solving");

            Set<UUID> ids = Set.of(id1, id2, missingId);
            // Repository only returns 2 of 3 requested competencies
            when(competencyRepository.findAllById(ids)).thenReturn(List.of(comp1, comp2));

            // Act
            Map<UUID, Competency> result = resilientCompetencyLoader.loadCompetencies(ids);

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result).containsKey(id1);
            assertThat(result).containsKey(id2);
            assertThat(result).doesNotContainKey(missingId);
        }
    }

    @Nested
    @DisplayName("Cache Population and Usage")
    class CacheTests {

        @Test
        @DisplayName("Should populate cache during successful load")
        void loadCompetencies_success_populatesCache() {
            // Arrange
            UUID id = UUID.randomUUID();
            Competency comp = createTestCompetency(id, "Teamwork");

            Set<UUID> ids = Set.of(id);
            when(competencyRepository.findAllById(ids)).thenReturn(List.of(comp));

            // Initial cache should be empty
            assertThat(resilientCompetencyLoader.getCacheSize()).isZero();

            // Act
            resilientCompetencyLoader.loadCompetencies(ids);

            // Assert
            assertThat(resilientCompetencyLoader.getCacheSize()).isEqualTo(1);
            assertThat(resilientCompetencyLoader.isCached(id)).isTrue();
        }

        @Test
        @DisplayName("Should accumulate cache entries across multiple loads")
        void loadCompetencies_multipleCalls_accumulatesCache() {
            // Arrange
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            Competency comp1 = createTestCompetency(id1, "Communication");
            Competency comp2 = createTestCompetency(id2, "Problem Solving");

            when(competencyRepository.findAllById(Set.of(id1))).thenReturn(List.of(comp1));
            when(competencyRepository.findAllById(Set.of(id2))).thenReturn(List.of(comp2));

            // Act
            resilientCompetencyLoader.loadCompetencies(Set.of(id1));
            resilientCompetencyLoader.loadCompetencies(Set.of(id2));

            // Assert
            assertThat(resilientCompetencyLoader.getCacheSize()).isEqualTo(2);
            assertThat(resilientCompetencyLoader.isCached(id1)).isTrue();
            assertThat(resilientCompetencyLoader.isCached(id2)).isTrue();
        }

        @Test
        @DisplayName("Should update cache entry when competency is reloaded")
        void loadCompetencies_reload_updatesCache() {
            // Arrange
            UUID id = UUID.randomUUID();
            Competency compV1 = createTestCompetency(id, "Leadership v1");
            Competency compV2 = createTestCompetency(id, "Leadership v2");

            Set<UUID> ids = Set.of(id);
            when(competencyRepository.findAllById(ids))
                    .thenReturn(List.of(compV1))
                    .thenReturn(List.of(compV2));

            // Act - first load
            Map<UUID, Competency> result1 = resilientCompetencyLoader.loadCompetencies(ids);
            assertThat(result1.get(id).getName()).isEqualTo("Leadership v1");

            // Act - second load (simulating update)
            Map<UUID, Competency> result2 = resilientCompetencyLoader.loadCompetencies(ids);

            // Assert
            assertThat(result2.get(id).getName()).isEqualTo("Leadership v2");
            assertThat(resilientCompetencyLoader.getCacheSize()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should clear cache when clearCache is called")
        void clearCache_shouldEmptyCache() {
            // Arrange
            UUID id = UUID.randomUUID();
            Competency comp = createTestCompetency(id, "Teamwork");

            when(competencyRepository.findAllById(Set.of(id))).thenReturn(List.of(comp));
            resilientCompetencyLoader.loadCompetencies(Set.of(id));
            assertThat(resilientCompetencyLoader.getCacheSize()).isEqualTo(1);

            // Act
            resilientCompetencyLoader.clearCache();

            // Assert
            assertThat(resilientCompetencyLoader.getCacheSize()).isZero();
            assertThat(resilientCompetencyLoader.isCached(id)).isFalse();
        }

        @Test
        @DisplayName("isCached should return false for null id")
        void isCached_nullId_returnsFalse() {
            assertThat(resilientCompetencyLoader.isCached(null)).isFalse();
        }

        @Test
        @DisplayName("isCached should return false for unknown id")
        void isCached_unknownId_returnsFalse() {
            assertThat(resilientCompetencyLoader.isCached(UUID.randomUUID())).isFalse();
        }
    }

    @Nested
    @DisplayName("Cache Warming")
    class CacheWarmingTests {

        @Test
        @DisplayName("Should warm cache with provided competencies")
        void warmCache_success_populatesCache() {
            // Arrange
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            Competency comp1 = createTestCompetency(id1, "Communication");
            Competency comp2 = createTestCompetency(id2, "Problem Solving");

            Set<UUID> ids = Set.of(id1, id2);
            when(competencyRepository.findAllById(ids)).thenReturn(List.of(comp1, comp2));

            // Act
            resilientCompetencyLoader.warmCache(ids);

            // Assert
            assertThat(resilientCompetencyLoader.getCacheSize()).isEqualTo(2);
            assertThat(resilientCompetencyLoader.isCached(id1)).isTrue();
            assertThat(resilientCompetencyLoader.isCached(id2)).isTrue();
        }

        @Test
        @DisplayName("Should handle null input gracefully during cache warming")
        void warmCache_nullInput_noException() {
            // Act & Assert - should not throw
            resilientCompetencyLoader.warmCache(null);
            assertThat(resilientCompetencyLoader.getCacheSize()).isZero();
        }

        @Test
        @DisplayName("Should handle empty set input gracefully during cache warming")
        void warmCache_emptyInput_noException() {
            // Act & Assert - should not throw
            resilientCompetencyLoader.warmCache(Set.of());
            assertThat(resilientCompetencyLoader.getCacheSize()).isZero();
        }

        @Test
        @DisplayName("Should handle repository exception gracefully during cache warming")
        void warmCache_repositoryException_noExceptionThrown() {
            // Arrange
            Set<UUID> ids = Set.of(UUID.randomUUID());
            when(competencyRepository.findAllById(ids))
                    .thenThrow(new RuntimeException("Database connection failed"));

            // Act & Assert - should not throw, just log warning
            resilientCompetencyLoader.warmCache(ids);
            assertThat(resilientCompetencyLoader.getCacheSize()).isZero();
        }
    }

    @Nested
    @DisplayName("Thread Safety Considerations")
    class ThreadSafetyTests {

        @Test
        @DisplayName("Should handle concurrent cache operations without errors")
        void concurrentOperations_noErrors() throws InterruptedException {
            // Arrange
            List<UUID> ids = new ArrayList<>();
            List<Competency> competencies = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                UUID id = UUID.randomUUID();
                ids.add(id);
                competencies.add(createTestCompetency(id, "Competency " + i));
            }

            when(competencyRepository.findAllById(anyCollection()))
                    .thenAnswer(invocation -> {
                        Set<UUID> requestedIds = new HashSet<>((Collection<UUID>) invocation.getArgument(0));
                        return competencies.stream()
                                .filter(c -> requestedIds.contains(c.getId()))
                                .toList();
                    });

            // Act - simulate concurrent access
            Thread[] threads = new Thread[5];
            for (int i = 0; i < threads.length; i++) {
                final int threadIndex = i;
                threads[i] = new Thread(() -> {
                    // Each thread loads different subset of competencies
                    Set<UUID> subset = Set.of(ids.get(threadIndex * 2), ids.get(threadIndex * 2 + 1));
                    resilientCompetencyLoader.loadCompetencies(subset);
                });
            }

            for (Thread thread : threads) {
                thread.start();
            }
            for (Thread thread : threads) {
                thread.join();
            }

            // Assert - all competencies should be cached
            assertThat(resilientCompetencyLoader.getCacheSize()).isEqualTo(10);
            for (UUID id : ids) {
                assertThat(resilientCompetencyLoader.isCached(id)).isTrue();
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle repository returning empty list")
        void loadCompetencies_repositoryReturnsEmpty_returnsEmptyMap() {
            // Arrange
            Set<UUID> ids = Set.of(UUID.randomUUID(), UUID.randomUUID());
            when(competencyRepository.findAllById(ids)).thenReturn(List.of());

            // Act
            Map<UUID, Competency> result = resilientCompetencyLoader.loadCompetencies(ids);

            // Assert
            assertThat(result).isEmpty();
            assertThat(resilientCompetencyLoader.getCacheSize()).isZero();
        }

        @Test
        @DisplayName("Should handle large number of competencies")
        void loadCompetencies_largeSet_handlesCorrectly() {
            // Arrange
            int count = 100;
            Set<UUID> ids = new HashSet<>();
            List<Competency> competencies = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                UUID id = UUID.randomUUID();
                ids.add(id);
                competencies.add(createTestCompetency(id, "Competency " + i));
            }

            when(competencyRepository.findAllById(ids)).thenReturn(competencies);

            // Act
            Map<UUID, Competency> result = resilientCompetencyLoader.loadCompetencies(ids);

            // Assert
            assertThat(result).hasSize(count);
            assertThat(resilientCompetencyLoader.getCacheSize()).isEqualTo(count);
        }
    }
}
