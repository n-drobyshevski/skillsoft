package app.skillsoft.assessmentbackend.services.impl;

import app.skillsoft.assessmentbackend.config.CacheConfig;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;
import app.skillsoft.assessmentbackend.domain.entities.TemplateStatus;
import app.skillsoft.assessmentbackend.domain.dto.CreateTestTemplateRequest;
import app.skillsoft.assessmentbackend.domain.dto.TestTemplateDto;
import app.skillsoft.assessmentbackend.domain.dto.TestTemplateSummaryDto;
import app.skillsoft.assessmentbackend.domain.dto.UpdateTestTemplateRequest;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.TestBlueprintDto;
import app.skillsoft.assessmentbackend.domain.entities.TestTemplate;
import app.skillsoft.assessmentbackend.exception.ResourceNotFoundException;
import app.skillsoft.assessmentbackend.repository.TestTemplateRepository;
import app.skillsoft.assessmentbackend.repository.UserRepository;
import app.skillsoft.assessmentbackend.services.BlueprintConversionService;
import app.skillsoft.assessmentbackend.services.TestTemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TestTemplateServiceImpl implements TestTemplateService {

    private static final Logger log = LoggerFactory.getLogger(TestTemplateServiceImpl.class);

    private final TestTemplateRepository templateRepository;
    private final UserRepository userRepository;
    private final BlueprintConversionService blueprintConversionService;

    public TestTemplateServiceImpl(
            TestTemplateRepository templateRepository,
            UserRepository userRepository,
            BlueprintConversionService blueprintConversionService) {
        this.templateRepository = templateRepository;
        this.userRepository = userRepository;
        this.blueprintConversionService = blueprintConversionService;
    }

    @Override
    public Page<TestTemplateSummaryDto> listTemplates(Pageable pageable) {
        // Exclude soft-deleted templates from listing
        return templateRepository.findByDeletedAtIsNull(pageable)
                .map(this::toSummaryDto);
    }

    @Override
    public List<TestTemplateSummaryDto> listActiveTemplates() {
        // Return only active, non-deleted templates
        return templateRepository.findByIsActiveTrueAndDeletedAtIsNull().stream()
                .map(this::toSummaryDto)
                .toList();
    }

    @Override
    public List<TestTemplateSummaryDto> listMyTemplates(String clerkId) {
        return userRepository.findByClerkId(clerkId)
                .map(user -> templateRepository.findActiveOwnedByUser(user.getId()).stream()
                        .map(this::toSummaryDto)
                        .toList())
                .orElseGet(List::of);
    }

    @Override
    @Cacheable(value = CacheConfig.TEMPLATE_METADATA_CACHE, key = "#id")
    public Optional<TestTemplateDto> findById(UUID id) {
        return templateRepository.findById(id)
                .map(this::toDto);
    }

    @Override
    @Transactional
    public TestTemplateDto createTemplate(CreateTestTemplateRequest request) {
        // Validate that template name is unique among non-deleted templates
        if (templateRepository.existsByNameIgnoreCaseAndDeletedAtIsNull(request.name())) {
            throw new IllegalArgumentException("Template with name '" + request.name() + "' already exists");
        }

        TestTemplate template = new TestTemplate();
        template.setName(request.name());
        template.setDescription(request.description());
        
        // Set assessment goal (defaults to OVERVIEW if not provided)
        template.setGoal(request.goal() != null ? request.goal() : AssessmentGoal.OVERVIEW);
        
        // Set blueprint configuration (goal-specific assessment parameters)
        template.setBlueprint(request.blueprint());
        
        // Legacy: Set competencyIds for backward compatibility
        template.setCompetencyIds(request.competencyIds());
        
        template.setQuestionsPerIndicator(request.questionsPerIndicator());
        template.setTimeLimitMinutes(request.timeLimitMinutes());
        template.setPassingScore(request.passingScore());
        template.setShuffleQuestions(request.shuffleQuestions());
        template.setShuffleOptions(request.shuffleOptions());
        template.setAllowSkip(request.allowSkip());
        template.setAllowBackNavigation(request.allowBackNavigation());
        template.setShowResultsImmediately(request.showResultsImmediately());
        template.setIsActive(true);

        // Auto-convert legacy blueprint to typed blueprint for test assembly
        blueprintConversionService.ensureTypedBlueprint(template);

        TestTemplate saved = templateRepository.save(template);
        return toDto(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.TEMPLATE_METADATA_CACHE, key = "#id")
    public TestTemplateDto updateTemplate(UUID id, UpdateTestTemplateRequest request) {
        TestTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TestTemplate", id));

        // Guard: Only DRAFT templates can be modified
        if (!template.isEditable()) {
            throw new IllegalStateException(
                    String.format("Cannot modify template in %s status. Only DRAFT templates can be edited.",
                            template.getStatus()));
        }

        // Update only provided fields
        if (request.name() != null) {
            // Check uniqueness if name is being changed (among non-deleted templates)
            if (!template.getName().equalsIgnoreCase(request.name())
                    && templateRepository.existsByNameIgnoreCaseAndDeletedAtIsNull(request.name())) {
                throw new IllegalArgumentException("Template with name '" + request.name() + "' already exists");
            }
            template.setName(request.name());
        }
        if (request.description() != null) {
            template.setDescription(request.description());
        }
        
        // Update assessment goal if provided
        if (request.goal() != null) {
            template.setGoal(request.goal());
        }
        
        // Update blueprint configuration if provided
        if (request.blueprint() != null) {
            template.setBlueprint(request.blueprint());
        }
        
        // Legacy: Update competencyIds for backward compatibility
        if (request.competencyIds() != null) {
            template.setCompetencyIds(request.competencyIds());
        }
        if (request.questionsPerIndicator() != null) {
            template.setQuestionsPerIndicator(request.questionsPerIndicator());
        }
        if (request.timeLimitMinutes() != null) {
            template.setTimeLimitMinutes(request.timeLimitMinutes());
        }
        if (request.passingScore() != null) {
            template.setPassingScore(request.passingScore());
        }
        if (request.isActive() != null) {
            template.setIsActive(request.isActive());
        }
        if (request.shuffleQuestions() != null) {
            template.setShuffleQuestions(request.shuffleQuestions());
        }
        if (request.shuffleOptions() != null) {
            template.setShuffleOptions(request.shuffleOptions());
        }
        if (request.allowSkip() != null) {
            template.setAllowSkip(request.allowSkip());
        }
        if (request.allowBackNavigation() != null) {
            template.setAllowBackNavigation(request.allowBackNavigation());
        }
        if (request.showResultsImmediately() != null) {
            template.setShowResultsImmediately(request.showResultsImmediately());
        }

        // Auto-convert legacy blueprint to typed blueprint for test assembly
        // This ensures templates are upgraded when updated
        blueprintConversionService.ensureTypedBlueprint(template);

        TestTemplate saved = templateRepository.save(template);
        return toDto(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.TEMPLATE_METADATA_CACHE, key = "#id")
    public boolean deleteTemplate(UUID id) {
        return templateRepository.findById(id)
                .map(template -> {
                    template.softDelete(null);
                    templateRepository.save(template);
                    log.info("Template {} soft-deleted via deleteTemplate()", id);
                    return true;
                })
                .orElse(false);
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.TEMPLATE_METADATA_CACHE, key = "#id")
    public TestTemplateDto activateTemplate(UUID id) {
        TestTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TestTemplate", id));
        template.setIsActive(true);
        return toDto(templateRepository.save(template));
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.TEMPLATE_METADATA_CACHE, allEntries = true)
    public TestTemplateDto deactivateTemplate(UUID id) {
        TestTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TestTemplate", id));
        template.setIsActive(false);
        return toDto(templateRepository.save(template));
    }

    @Override
    public List<TestTemplateSummaryDto> searchByName(String name) {
        // Search only among active, non-deleted templates
        return templateRepository.findByNameContainingIgnoreCaseAndIsActiveTrueAndDeletedAtIsNull(name).stream()
                .map(this::toSummaryDto)
                .toList();
    }

    @Override
    public List<TestTemplateSummaryDto> findByCompetency(UUID competencyId) {
        // Format UUID as JSON array element for JSONB query
        String competencyIdJson = "[\"" + competencyId.toString() + "\"]";
        return templateRepository.findActiveTemplatesContainingCompetency(competencyIdJson).stream()
                .map(this::toSummaryDto)
                .toList();
    }

    @Override
    public TemplateStatistics getStatistics() {
        // Count only non-deleted templates
        long total = templateRepository.countByDeletedAtIsNull();
        long active = templateRepository.countByIsActiveTrueAndDeletedAtIsNull();
        return new TemplateStatistics(total, active, total - active);
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.TEMPLATE_METADATA_CACHE, key = "#templateId")
    public PublishResult publishTemplate(UUID templateId) {
        log.info("Publishing template: {}", templateId);

        // 1. Find the template or throw 404
        TestTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("TestTemplate", templateId));

        // 2. Validate template is ready to publish
        List<String> validationErrors = validateTemplateForPublishing(template);
        if (!validationErrors.isEmpty()) {
            String errorMessage = "Template is not ready to publish: " + String.join("; ", validationErrors);
            log.warn("Publishing failed for template {}: {}", templateId, errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        // 3. Check if template is in DRAFT status (can be published)
        if (template.getStatus() != TemplateStatus.DRAFT) {
            String errorMessage = String.format(
                    "Only DRAFT templates can be published. Current status: %s",
                    template.getStatus()
            );
            log.warn("Publishing failed for template {}: {}", templateId, errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        // 4. Publish the template (uses entity method which handles status and isActive)
        template.publish();
        TestTemplate savedTemplate = templateRepository.save(template);

        log.info("Template {} published successfully. Version: {}, Status: {}",
                templateId, savedTemplate.getVersion(), savedTemplate.getStatus());

        return new PublishResult(
                true,
                savedTemplate.getVersion(),
                "Template published successfully"
        );
    }

    /**
     * Validates that a template has all required configuration to be published.
     *
     * <p>This validation ensures templates are ready for test assembly by requiring
     * a typed blueprint. Legacy blueprint data will be auto-converted before validation.</p>
     *
     * @param template The template to validate
     * @return List of validation error messages (empty if valid)
     */
    private List<String> validateTemplateForPublishing(TestTemplate template) {
        List<String> errors = new ArrayList<>();

        // Check template has a name
        if (template.getName() == null || template.getName().trim().isEmpty()) {
            errors.add("Template must have a name");
        }

        // Attempt to auto-convert legacy blueprint to typed blueprint
        blueprintConversionService.ensureTypedBlueprint(template);

        // Now strictly require typed blueprint for test assembly
        if (template.getTypedBlueprint() == null) {
            errors.add("Template must have a valid blueprint configured. " +
                    "Please go to the Blueprint tab and add at least one competency.");
        }

        // Check assessment goal is set
        if (template.getGoal() == null) {
            errors.add("Template must have an assessment goal defined");
        }

        // Check time limit is reasonable
        if (template.getTimeLimitMinutes() == null || template.getTimeLimitMinutes() <= 0) {
            errors.add("Template must have a valid time limit (> 0 minutes)");
        }

        // Check passing score is reasonable (0-100%)
        if (template.getPassingScore() == null || template.getPassingScore() < 0 || template.getPassingScore() > 100) {
            errors.add("Template must have a valid passing score (0-100)");
        }

        return errors;
    }

    // Mapping methods
    private TestTemplateDto toDto(TestTemplate template) {
        return new TestTemplateDto(
                template.getId(),
                template.getName(),
                template.getDescription(),
                template.getGoal(),
                template.getBlueprint(),
                template.getCompetencyIds(),
                template.getQuestionsPerIndicator(),
                template.getTimeLimitMinutes(),
                template.getPassingScore(),
                template.getIsActive(),
                template.getShuffleQuestions(),
                template.getShuffleOptions(),
                template.getAllowSkip(),
                template.getAllowBackNavigation(),
                template.getShowResultsImmediately(),
                template.getCreatedAt(),
                template.getUpdatedAt(),
                blueprintConversionService.hasValidBlueprint(template)
        );
    }

    private TestTemplateSummaryDto toSummaryDto(TestTemplate template) {
        return new TestTemplateSummaryDto(
                template.getId(),
                template.getName(),
                template.getDescription(),
                template.getGoal(),
                template.getCompetencyIds() != null ? template.getCompetencyIds().size() : 0,
                template.getTimeLimitMinutes(),
                template.getPassingScore(),
                template.getIsActive(),
                template.getCreatedAt()
        );
    }
}
