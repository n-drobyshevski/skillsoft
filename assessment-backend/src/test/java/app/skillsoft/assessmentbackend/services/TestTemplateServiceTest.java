package app.skillsoft.assessmentbackend.services;

import app.skillsoft.assessmentbackend.domain.dto.*;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;
import app.skillsoft.assessmentbackend.domain.entities.TestTemplate;
import app.skillsoft.assessmentbackend.repository.TestTemplateRepository;
import app.skillsoft.assessmentbackend.repository.UserRepository;
import app.skillsoft.assessmentbackend.services.BlueprintConversionService;
import app.skillsoft.assessmentbackend.services.impl.TestTemplateServiceImpl;
import app.skillsoft.assessmentbackend.services.validation.BlueprintValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TestTemplateService implementation.
 * 
 * Tests cover:
 * - Template CRUD operations
 * - Template activation/deactivation
 * - Search and filtering
 * - Validation and error handling
 * - Statistics calculation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TestTemplate Service Tests")
class TestTemplateServiceTest {

    @Mock
    private TestTemplateRepository templateRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BlueprintConversionService blueprintConversionService;

    @Mock
    private BlueprintValidationService blueprintValidationService;

    @InjectMocks
    private TestTemplateServiceImpl testTemplateService;

    private UUID templateId;
    private UUID competencyId;
    private TestTemplate mockTemplate;
    private CreateTestTemplateRequest createRequest;
    private UpdateTestTemplateRequest updateRequest;

    @BeforeEach
    void setUp() {
        templateId = UUID.randomUUID();
        competencyId = UUID.randomUUID();

        mockTemplate = new TestTemplate();
        mockTemplate.setId(templateId);
        mockTemplate.setName("Leadership Assessment Test");
        mockTemplate.setDescription("Comprehensive test for leadership competencies");
        mockTemplate.setGoal(AssessmentGoal.OVERVIEW);
        mockTemplate.setBlueprint(Map.of("strategy", "balanced"));
        mockTemplate.setCompetencyIds(List.of(competencyId));
        mockTemplate.setQuestionsPerIndicator(3);
        mockTemplate.setTimeLimitMinutes(60);
        mockTemplate.setPassingScore(70.0);
        mockTemplate.setShuffleQuestions(true);
        mockTemplate.setShuffleOptions(true);
        mockTemplate.setAllowSkip(true);
        mockTemplate.setAllowBackNavigation(true);
        mockTemplate.setShowResultsImmediately(true);
        mockTemplate.setIsActive(true);
        mockTemplate.setCreatedAt(LocalDateTime.now());
        mockTemplate.setUpdatedAt(LocalDateTime.now());

        // CreateTestTemplateRequest: name, description, goal, blueprint, competencyIds, questionsPerIndicator,
        //                            timeLimitMinutes, passingScore, shuffleQuestions, shuffleOptions,
        //                            allowSkip, allowBackNavigation, showResultsImmediately
        createRequest = new CreateTestTemplateRequest(
                "Leadership Assessment Test",
                "Comprehensive test for leadership competencies",
                AssessmentGoal.OVERVIEW, // goal
                Map.of("strategy", "balanced"), // blueprint
                List.of(competencyId), // competencyIds (deprecated)
                3,    // questionsPerIndicator
                60,   // timeLimitMinutes
                70.0, // passingScore
                true, // shuffleQuestions
                true, // shuffleOptions
                true, // allowSkip
                true, // allowBackNavigation
                true  // showResultsImmediately
        );

        // UpdateTestTemplateRequest: name, description, goal, blueprint, competencyIds, questionsPerIndicator,
        //                            timeLimitMinutes, passingScore, isActive, shuffleQuestions, shuffleOptions,
        //                            allowSkip, allowBackNavigation, showResultsImmediately
        updateRequest = new UpdateTestTemplateRequest(
                "Updated Test",
                "Updated description",
                AssessmentGoal.JOB_FIT, // goal
                Map.of("onetSocCode", "15-1252.00"), // blueprint
                List.of(competencyId, UUID.randomUUID()), // competencyIds (deprecated)
                5,
                90,
                80.0,
                null,  // isActive - not changed
                false, // shuffleQuestions
                null,  // shuffleOptions - not changed
                null,  // allowSkip - not changed
                true,  // allowBackNavigation
                null   // showResultsImmediately - not changed
        );
    }

    @Nested
    @DisplayName("List Templates Tests")
    class ListTemplatesTests {

        @Test
        @DisplayName("Should return paginated list of templates")
        void shouldReturnPaginatedTemplates() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<TestTemplate> templatePage = new PageImpl<>(
                    List.of(mockTemplate),
                    pageable,
                    1
            );
            when(templateRepository.findByDeletedAtIsNull(pageable)).thenReturn(templatePage);

            // When
            Page<TestTemplateSummaryDto> result = testTemplateService.listTemplates(pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).name()).isEqualTo("Leadership Assessment Test");

            verify(templateRepository).findByDeletedAtIsNull(pageable);
        }

        @Test
        @DisplayName("Should return empty page when no templates exist")
        void shouldReturnEmptyPageWhenNoTemplates() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<TestTemplate> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
            when(templateRepository.findByDeletedAtIsNull(pageable)).thenReturn(emptyPage);

            // When
            Page<TestTemplateSummaryDto> result = testTemplateService.listTemplates(pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("Should return only active non-deleted templates")
        void shouldReturnOnlyActiveTemplates() {
            // Given
            when(templateRepository.findByIsActiveTrueAndDeletedAtIsNull()).thenReturn(List.of(mockTemplate));

            // When
            List<TestTemplateSummaryDto> result = testTemplateService.listActiveTemplates();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).isActive()).isTrue();

            verify(templateRepository).findByIsActiveTrueAndDeletedAtIsNull();
        }
    }

    @Nested
    @DisplayName("Find Template By ID Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Should return template when found")
        void shouldReturnTemplateWhenFound() {
            // Given
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(mockTemplate));

            // When
            Optional<TestTemplateDto> result = testTemplateService.findById(templateId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo(templateId);
            assertThat(result.get().name()).isEqualTo("Leadership Assessment Test");
            assertThat(result.get().timeLimitMinutes()).isEqualTo(60);
            assertThat(result.get().passingScore()).isEqualTo(70.0);

            verify(templateRepository).findById(templateId);
        }

        @Test
        @DisplayName("Should return empty when template not found")
        void shouldReturnEmptyWhenNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(templateRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When
            Optional<TestTemplateDto> result = testTemplateService.findById(nonExistentId);

            // Then
            assertThat(result).isEmpty();

            verify(templateRepository).findById(nonExistentId);
        }
    }

    @Nested
    @DisplayName("Create Template Tests")
    class CreateTemplateTests {

        @Test
        @DisplayName("Should create new template successfully")
        void shouldCreateNewTemplate() {
            // Given
            when(templateRepository.existsByNameIgnoreCaseAndDeletedAtIsNull(createRequest.name())).thenReturn(false);
            when(templateRepository.save(any(TestTemplate.class))).thenReturn(mockTemplate);

            // When
            TestTemplateDto result = testTemplateService.createTemplate(createRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("Leadership Assessment Test");
            assertThat(result.description()).isEqualTo("Comprehensive test for leadership competencies");
            assertThat(result.isActive()).isTrue();

            verify(templateRepository).existsByNameIgnoreCaseAndDeletedAtIsNull(createRequest.name());
            verify(templateRepository).save(any(TestTemplate.class));
        }

        @Test
        @DisplayName("Should throw exception when template name already exists")
        void shouldThrowExceptionWhenNameExists() {
            // Given
            when(templateRepository.existsByNameIgnoreCaseAndDeletedAtIsNull(createRequest.name())).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> testTemplateService.createTemplate(createRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already exists");

            verify(templateRepository).existsByNameIgnoreCaseAndDeletedAtIsNull(createRequest.name());
            verify(templateRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should create template with all configuration options")
        void shouldCreateTemplateWithAllOptions() {
            // Given
            when(templateRepository.existsByNameIgnoreCaseAndDeletedAtIsNull(anyString())).thenReturn(false);
            when(templateRepository.save(any(TestTemplate.class))).thenAnswer(invocation -> {
                TestTemplate saved = invocation.getArgument(0);
                saved.setId(templateId);
                return saved;
            });

            // When
            TestTemplateDto result = testTemplateService.createTemplate(createRequest);

            // Then
            assertThat(result).isNotNull();
            verify(templateRepository).save(argThat(template ->
                template.getShuffleQuestions().equals(true) &&
                template.getAllowBackNavigation().equals(true) &&
                template.getShowResultsImmediately().equals(true)
            ));
        }
    }

    @Nested
    @DisplayName("Update Template Tests")
    class UpdateTemplateTests {

        @Test
        @DisplayName("Should update existing template")
        void shouldUpdateExistingTemplate() {
            // Given
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(mockTemplate));
            when(templateRepository.existsByNameIgnoreCaseAndDeletedAtIsNull(updateRequest.name())).thenReturn(false);
            when(templateRepository.save(any(TestTemplate.class))).thenReturn(mockTemplate);

            // When
            TestTemplateDto result = testTemplateService.updateTemplate(templateId, updateRequest);

            // Then
            assertThat(result).isNotNull();
            verify(templateRepository).findById(templateId);
            verify(templateRepository).save(any(TestTemplate.class));
        }

        @Test
        @DisplayName("Should throw exception when template not found")
        void shouldThrowExceptionWhenTemplateNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(templateRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> testTemplateService.updateTemplate(nonExistentId, updateRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("not found");

            verify(templateRepository).findById(nonExistentId);
            verify(templateRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should only update non-null fields")
        void shouldOnlyUpdateNonNullFields() {
            // Given
            UpdateTestTemplateRequest partialUpdate = new UpdateTestTemplateRequest(
                    null,  // name - keep original
                    "New description only",
                    null,  // goal - keep original
                    null,  // blueprint - keep original
                    null,  // competencyIds - keep original
                    null,  // questionsPerIndicator - keep original
                    null,  // timeLimitMinutes - keep original
                    null,  // passingScore - keep original
                    null,  // isActive - keep original
                    null,  // shuffleQuestions - keep original
                    null,  // shuffleOptions - keep original
                    null,  // allowSkip - keep original
                    null,  // allowBackNavigation - keep original
                    null   // showResultsImmediately - keep original
            );
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(mockTemplate));
            when(templateRepository.save(any(TestTemplate.class))).thenReturn(mockTemplate);

            // When
            testTemplateService.updateTemplate(templateId, partialUpdate);

            // Then
            verify(templateRepository).save(argThat(template ->
                    template.getName().equals("Leadership Assessment Test") &&
                    template.getDescription().equals("New description only")
            ));
        }
    }

    @Nested
    @DisplayName("Delete Template Tests")
    class DeleteTemplateTests {

        @Test
        @DisplayName("Should soft-delete existing template")
        void shouldSoftDeleteExistingTemplate() {
            // Given
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(mockTemplate));
            when(templateRepository.save(any())).thenReturn(mockTemplate);

            // When
            boolean result = testTemplateService.deleteTemplate(templateId);

            // Then
            assertThat(result).isTrue();
            verify(templateRepository).findById(templateId);
            verify(templateRepository).save(any());
        }

        @Test
        @DisplayName("Should return false when template not found")
        void shouldReturnFalseWhenTemplateNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(templateRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When
            boolean result = testTemplateService.deleteTemplate(nonExistentId);

            // Then
            assertThat(result).isFalse();
            verify(templateRepository).findById(nonExistentId);
            verify(templateRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Activate/Deactivate Template Tests")
    class ActivationTests {

        @Test
        @DisplayName("Should activate template")
        void shouldActivateTemplate() {
            // Given
            mockTemplate.setIsActive(false);
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(mockTemplate));
            when(templateRepository.save(any(TestTemplate.class))).thenAnswer(invocation -> {
                TestTemplate saved = invocation.getArgument(0);
                saved.setIsActive(true);
                return saved;
            });

            // When
            TestTemplateDto result = testTemplateService.activateTemplate(templateId);

            // Then
            assertThat(result).isNotNull();
            verify(templateRepository).save(argThat(template -> template.getIsActive().equals(true)));
        }

        @Test
        @DisplayName("Should deactivate template")
        void shouldDeactivateTemplate() {
            // Given
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(mockTemplate));
            when(templateRepository.save(any(TestTemplate.class))).thenAnswer(invocation -> {
                TestTemplate saved = invocation.getArgument(0);
                saved.setIsActive(false);
                return saved;
            });

            // When
            TestTemplateDto result = testTemplateService.deactivateTemplate(templateId);

            // Then
            assertThat(result).isNotNull();
            verify(templateRepository).save(argThat(template -> template.getIsActive().equals(false)));
        }

        @Test
        @DisplayName("Should throw exception when activating non-existent template")
        void shouldThrowExceptionWhenActivatingNonExistent() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(templateRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> testTemplateService.activateTemplate(nonExistentId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("not found");
        }
    }

    @Nested
    @DisplayName("Search Templates Tests")
    class SearchTemplatesTests {

        @Test
        @DisplayName("Should search templates by name")
        void shouldSearchTemplatesByName() {
            // Given
            when(templateRepository.findByNameContainingIgnoreCaseAndIsActiveTrueAndDeletedAtIsNull("Leadership"))
                    .thenReturn(List.of(mockTemplate));

            // When
            List<TestTemplateSummaryDto> result = testTemplateService.searchByName("Leadership");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).contains("Leadership");

            verify(templateRepository).findByNameContainingIgnoreCaseAndIsActiveTrueAndDeletedAtIsNull("Leadership");
        }

        @Test
        @DisplayName("Should return empty list when no matches")
        void shouldReturnEmptyWhenNoMatches() {
            // Given
            when(templateRepository.findByNameContainingIgnoreCaseAndIsActiveTrueAndDeletedAtIsNull("NonExistent"))
                    .thenReturn(Collections.emptyList());

            // When
            List<TestTemplateSummaryDto> result = testTemplateService.searchByName("NonExistent");

            // Then
            assertThat(result).isEmpty();

            verify(templateRepository).findByNameContainingIgnoreCaseAndIsActiveTrueAndDeletedAtIsNull("NonExistent");
        }
    }

    @Nested
    @DisplayName("Find Templates By Competency Tests")
    class FindByCompetencyTests {

        @Test
        @DisplayName("Should find templates by competency id")
        void shouldFindTemplatesByCompetency() {
            // Given
            String competencyIdJson = "[\"" + competencyId.toString() + "\"]";
            when(templateRepository.findActiveTemplatesContainingCompetency(competencyIdJson))
                    .thenReturn(List.of(mockTemplate));

            // When
            List<TestTemplateSummaryDto> result = testTemplateService.findByCompetency(competencyId);

            // Then
            assertThat(result).hasSize(1);
            verify(templateRepository).findActiveTemplatesContainingCompetency(competencyIdJson);
        }
    }

    @Nested
    @DisplayName("Statistics Tests")
    class StatisticsTests {

        @Test
        @DisplayName("Should return template statistics for non-deleted templates")
        void shouldReturnStatistics() {
            // Given
            when(templateRepository.countByDeletedAtIsNull()).thenReturn(10L);
            when(templateRepository.countByIsActiveTrueAndDeletedAtIsNull()).thenReturn(7L);

            // When
            TestTemplateService.TemplateStatistics stats = testTemplateService.getStatistics();

            // Then
            assertThat(stats).isNotNull();
            assertThat(stats.totalTemplates()).isEqualTo(10L);
            assertThat(stats.activeTemplates()).isEqualTo(7L);
            assertThat(stats.inactiveTemplates()).isEqualTo(3L);

            verify(templateRepository).countByDeletedAtIsNull();
            verify(templateRepository).countByIsActiveTrueAndDeletedAtIsNull();
        }
    }
}
