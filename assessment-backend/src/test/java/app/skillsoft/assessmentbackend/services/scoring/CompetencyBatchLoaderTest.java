package app.skillsoft.assessmentbackend.services.scoring;

import app.skillsoft.assessmentbackend.domain.entities.ApprovalStatus;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentQuestion;
import app.skillsoft.assessmentbackend.domain.entities.BehavioralIndicator;
import app.skillsoft.assessmentbackend.domain.entities.Competency;
import app.skillsoft.assessmentbackend.domain.entities.CompetencyCategory;
import app.skillsoft.assessmentbackend.domain.entities.ObservabilityLevel;
import app.skillsoft.assessmentbackend.domain.entities.QuestionType;
import app.skillsoft.assessmentbackend.domain.entities.TestAnswer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CompetencyBatchLoader component.
 *
 * Tests cover:
 * - Delegation to ResilientCompetencyLoader for circuit breaker protection
 * - Competency ID extraction from test answers
 * - Null and edge case handling
 * - Skipped answer filtering
 *
 * Per ROADMAP.md Task 1.3 - N+1 Query Optimization
 * Per ROADMAP.md Task 3.2 - Circuit breaker delegation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CompetencyBatchLoader Tests")
class CompetencyBatchLoaderTest {

    @Mock
    private ResilientCompetencyLoader resilientCompetencyLoader;

    @Captor
    private ArgumentCaptor<Set<UUID>> idsCaptor;

    private CompetencyBatchLoader competencyBatchLoader;

    @BeforeEach
    void setUp() {
        competencyBatchLoader = new CompetencyBatchLoader(resilientCompetencyLoader);
    }

    // Helper methods to create test entities
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

    private BehavioralIndicator createTestIndicator(UUID id, Competency competency) {
        BehavioralIndicator indicator = new BehavioralIndicator();
        indicator.setId(id);
        indicator.setCompetency(competency);
        indicator.setDescription("Test indicator");
        indicator.setObservabilityLevel(ObservabilityLevel.DIRECTLY_OBSERVABLE);
        return indicator;
    }

    private AssessmentQuestion createTestQuestion(UUID id, BehavioralIndicator indicator) {
        AssessmentQuestion question = new AssessmentQuestion();
        question.setId(id);
        question.setBehavioralIndicator(indicator);
        question.setQuestionType(QuestionType.LIKERT);
        question.setQuestionText("Test question");
        return question;
    }

    private TestAnswer createTestAnswer(UUID id, AssessmentQuestion question, boolean isSkipped) {
        TestAnswer answer = new TestAnswer();
        answer.setId(id);
        answer.setQuestion(question);
        answer.setIsSkipped(isSkipped);
        answer.setLikertValue(3);
        answer.setAnsweredAt(LocalDateTime.now());
        return answer;
    }

    @Nested
    @DisplayName("loadCompetenciesForAnswers - Delegation Tests")
    class DelegationTests {

        @Test
        @DisplayName("Should delegate to ResilientCompetencyLoader")
        void loadCompetenciesForAnswers_delegatesToResilientLoader() {
            // Arrange
            UUID compId = UUID.randomUUID();
            Competency competency = createTestCompetency(compId, "Communication");
            BehavioralIndicator indicator = createTestIndicator(UUID.randomUUID(), competency);
            AssessmentQuestion question = createTestQuestion(UUID.randomUUID(), indicator);
            TestAnswer answer = createTestAnswer(UUID.randomUUID(), question, false);

            Map<UUID, Competency> expectedResult = Map.of(compId, competency);
            when(resilientCompetencyLoader.loadCompetencies(anySet())).thenReturn(expectedResult);

            // Act
            Map<UUID, Competency> result = competencyBatchLoader.loadCompetenciesForAnswers(List.of(answer));

            // Assert
            assertThat(result).isEqualTo(expectedResult);
            verify(resilientCompetencyLoader).loadCompetencies(idsCaptor.capture());
            assertThat(idsCaptor.getValue()).containsExactly(compId);
        }

        @Test
        @DisplayName("Should pass correct competency IDs to resilient loader")
        void loadCompetenciesForAnswers_passesCorrectIds() {
            // Arrange
            UUID compId1 = UUID.randomUUID();
            UUID compId2 = UUID.randomUUID();

            Competency comp1 = createTestCompetency(compId1, "Communication");
            Competency comp2 = createTestCompetency(compId2, "Problem Solving");

            BehavioralIndicator ind1 = createTestIndicator(UUID.randomUUID(), comp1);
            BehavioralIndicator ind2 = createTestIndicator(UUID.randomUUID(), comp2);

            AssessmentQuestion q1 = createTestQuestion(UUID.randomUUID(), ind1);
            AssessmentQuestion q2 = createTestQuestion(UUID.randomUUID(), ind2);

            TestAnswer ans1 = createTestAnswer(UUID.randomUUID(), q1, false);
            TestAnswer ans2 = createTestAnswer(UUID.randomUUID(), q2, false);

            when(resilientCompetencyLoader.loadCompetencies(anySet()))
                    .thenReturn(Map.of(compId1, comp1, compId2, comp2));

            // Act
            competencyBatchLoader.loadCompetenciesForAnswers(List.of(ans1, ans2));

            // Assert
            verify(resilientCompetencyLoader).loadCompetencies(idsCaptor.capture());
            assertThat(idsCaptor.getValue()).containsExactlyInAnyOrder(compId1, compId2);
        }
    }

    @Nested
    @DisplayName("loadCompetenciesForAnswers - Input Handling")
    class InputHandlingTests {

        @Test
        @DisplayName("Should return empty map for null input")
        void loadCompetenciesForAnswers_nullInput_returnsEmptyMap() {
            // Act
            Map<UUID, Competency> result = competencyBatchLoader.loadCompetenciesForAnswers(null);

            // Assert
            assertThat(result).isEmpty();
            verify(resilientCompetencyLoader, never()).loadCompetencies(anySet());
        }

        @Test
        @DisplayName("Should return empty map for empty list")
        void loadCompetenciesForAnswers_emptyInput_returnsEmptyMap() {
            // Act
            Map<UUID, Competency> result = competencyBatchLoader.loadCompetenciesForAnswers(List.of());

            // Assert
            assertThat(result).isEmpty();
            verify(resilientCompetencyLoader, never()).loadCompetencies(anySet());
        }

        @Test
        @DisplayName("Should filter out null answers")
        void loadCompetenciesForAnswers_withNullAnswers_filtersOut() {
            // Arrange
            UUID compId = UUID.randomUUID();
            Competency competency = createTestCompetency(compId, "Communication");
            BehavioralIndicator indicator = createTestIndicator(UUID.randomUUID(), competency);
            AssessmentQuestion question = createTestQuestion(UUID.randomUUID(), indicator);
            TestAnswer validAnswer = createTestAnswer(UUID.randomUUID(), question, false);

            List<TestAnswer> answers = new ArrayList<>();
            answers.add(null);
            answers.add(validAnswer);
            answers.add(null);

            when(resilientCompetencyLoader.loadCompetencies(anySet()))
                    .thenReturn(Map.of(compId, competency));

            // Act
            Map<UUID, Competency> result = competencyBatchLoader.loadCompetenciesForAnswers(answers);

            // Assert
            assertThat(result).hasSize(1);
            verify(resilientCompetencyLoader).loadCompetencies(idsCaptor.capture());
            assertThat(idsCaptor.getValue()).containsExactly(compId);
        }

        @Test
        @DisplayName("Should filter out skipped answers")
        void loadCompetenciesForAnswers_withSkippedAnswers_filtersOut() {
            // Arrange
            UUID compId = UUID.randomUUID();
            Competency competency = createTestCompetency(compId, "Communication");
            BehavioralIndicator indicator = createTestIndicator(UUID.randomUUID(), competency);
            AssessmentQuestion question = createTestQuestion(UUID.randomUUID(), indicator);

            TestAnswer validAnswer = createTestAnswer(UUID.randomUUID(), question, false);
            TestAnswer skippedAnswer = createTestAnswer(UUID.randomUUID(), question, true);

            when(resilientCompetencyLoader.loadCompetencies(anySet()))
                    .thenReturn(Map.of(compId, competency));

            // Act
            competencyBatchLoader.loadCompetenciesForAnswers(List.of(validAnswer, skippedAnswer));

            // Assert
            verify(resilientCompetencyLoader).loadCompetencies(idsCaptor.capture());
            // Only one unique competency ID since both answers reference the same competency
            assertThat(idsCaptor.getValue()).containsExactly(compId);
        }

        @Test
        @DisplayName("Should return empty map when all answers are skipped")
        void loadCompetenciesForAnswers_allSkipped_returnsEmptyMap() {
            // Arrange
            UUID compId = UUID.randomUUID();
            Competency competency = createTestCompetency(compId, "Communication");
            BehavioralIndicator indicator = createTestIndicator(UUID.randomUUID(), competency);
            AssessmentQuestion question = createTestQuestion(UUID.randomUUID(), indicator);

            TestAnswer skippedAnswer1 = createTestAnswer(UUID.randomUUID(), question, true);
            TestAnswer skippedAnswer2 = createTestAnswer(UUID.randomUUID(), question, true);

            // Act
            Map<UUID, Competency> result = competencyBatchLoader.loadCompetenciesForAnswers(
                    List.of(skippedAnswer1, skippedAnswer2));

            // Assert
            assertThat(result).isEmpty();
            verify(resilientCompetencyLoader, never()).loadCompetencies(anySet());
        }
    }

    @Nested
    @DisplayName("extractCompetencyIdSafe Tests")
    class ExtractCompetencyIdSafeTests {

        @Test
        @DisplayName("Should extract competency ID from valid answer")
        void extractCompetencyIdSafe_validAnswer_returnsId() {
            // Arrange
            UUID compId = UUID.randomUUID();
            Competency competency = createTestCompetency(compId, "Communication");
            BehavioralIndicator indicator = createTestIndicator(UUID.randomUUID(), competency);
            AssessmentQuestion question = createTestQuestion(UUID.randomUUID(), indicator);
            TestAnswer answer = createTestAnswer(UUID.randomUUID(), question, false);

            // Act
            Optional<UUID> result = competencyBatchLoader.extractCompetencyIdSafe(answer);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(compId);
        }

        @Test
        @DisplayName("Should return empty for null answer")
        void extractCompetencyIdSafe_nullAnswer_returnsEmpty() {
            // Act
            Optional<UUID> result = competencyBatchLoader.extractCompetencyIdSafe(null);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty when question is null")
        void extractCompetencyIdSafe_nullQuestion_returnsEmpty() {
            // Arrange
            TestAnswer answer = new TestAnswer();
            answer.setId(UUID.randomUUID());
            answer.setQuestion(null);

            // Act
            Optional<UUID> result = competencyBatchLoader.extractCompetencyIdSafe(answer);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty when behavioral indicator is null")
        void extractCompetencyIdSafe_nullIndicator_returnsEmpty() {
            // Arrange
            AssessmentQuestion question = new AssessmentQuestion();
            question.setId(UUID.randomUUID());
            question.setBehavioralIndicator(null);

            TestAnswer answer = new TestAnswer();
            answer.setId(UUID.randomUUID());
            answer.setQuestion(question);

            // Act
            Optional<UUID> result = competencyBatchLoader.extractCompetencyIdSafe(answer);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty when competency is null")
        void extractCompetencyIdSafe_nullCompetency_returnsEmpty() {
            // Arrange
            BehavioralIndicator indicator = new BehavioralIndicator();
            indicator.setId(UUID.randomUUID());
            indicator.setCompetency(null);

            AssessmentQuestion question = new AssessmentQuestion();
            question.setId(UUID.randomUUID());
            question.setBehavioralIndicator(indicator);

            TestAnswer answer = new TestAnswer();
            answer.setId(UUID.randomUUID());
            answer.setQuestion(question);

            // Act
            Optional<UUID> result = competencyBatchLoader.extractCompetencyIdSafe(answer);

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getFromCache Tests")
    class GetFromCacheTests {

        @Test
        @DisplayName("Should return competency from cache")
        void getFromCache_existsInCache_returnsCompetency() {
            // Arrange
            UUID compId = UUID.randomUUID();
            Competency competency = createTestCompetency(compId, "Communication");
            Map<UUID, Competency> cache = Map.of(compId, competency);

            // Act
            Competency result = competencyBatchLoader.getFromCache(cache, compId);

            // Assert
            assertThat(result).isEqualTo(competency);
        }

        @Test
        @DisplayName("Should return null for missing competency")
        void getFromCache_notInCache_returnsNull() {
            // Arrange
            UUID compId = UUID.randomUUID();
            UUID missingId = UUID.randomUUID();
            Competency competency = createTestCompetency(compId, "Communication");
            Map<UUID, Competency> cache = Map.of(compId, competency);

            // Act
            Competency result = competencyBatchLoader.getFromCache(cache, missingId);

            // Assert
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should return null for null cache")
        void getFromCache_nullCache_returnsNull() {
            // Act
            Competency result = competencyBatchLoader.getFromCache(null, UUID.randomUUID());

            // Assert
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should return null for null competency ID")
        void getFromCache_nullId_returnsNull() {
            // Arrange
            UUID compId = UUID.randomUUID();
            Competency competency = createTestCompetency(compId, "Communication");
            Map<UUID, Competency> cache = Map.of(compId, competency);

            // Act
            Competency result = competencyBatchLoader.getFromCache(cache, null);

            // Assert
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("Deduplication Tests")
    class DeduplicationTests {

        @Test
        @DisplayName("Should deduplicate competency IDs across multiple answers")
        void loadCompetenciesForAnswers_duplicateCompetencies_deduplicates() {
            // Arrange - multiple answers referencing the same competency
            UUID compId = UUID.randomUUID();
            Competency competency = createTestCompetency(compId, "Communication");
            BehavioralIndicator indicator = createTestIndicator(UUID.randomUUID(), competency);
            AssessmentQuestion question = createTestQuestion(UUID.randomUUID(), indicator);

            TestAnswer ans1 = createTestAnswer(UUID.randomUUID(), question, false);
            TestAnswer ans2 = createTestAnswer(UUID.randomUUID(), question, false);
            TestAnswer ans3 = createTestAnswer(UUID.randomUUID(), question, false);

            when(resilientCompetencyLoader.loadCompetencies(anySet()))
                    .thenReturn(Map.of(compId, competency));

            // Act
            competencyBatchLoader.loadCompetenciesForAnswers(List.of(ans1, ans2, ans3));

            // Assert - should only request the competency once
            verify(resilientCompetencyLoader).loadCompetencies(idsCaptor.capture());
            assertThat(idsCaptor.getValue()).hasSize(1);
            assertThat(idsCaptor.getValue()).containsExactly(compId);
        }
    }
}
