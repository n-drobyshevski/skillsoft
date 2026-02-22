package app.skillsoft.assessmentbackend.services.impl;

import app.skillsoft.assessmentbackend.config.CacheConfig;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;
import app.skillsoft.assessmentbackend.domain.entities.TemplateStatus;
import app.skillsoft.assessmentbackend.domain.entities.TemplateVisibility;
import app.skillsoft.assessmentbackend.domain.dto.CreateTestTemplateRequest;
import app.skillsoft.assessmentbackend.domain.dto.TestTemplateDto;
import app.skillsoft.assessmentbackend.domain.dto.TestTemplateSummaryDto;
import app.skillsoft.assessmentbackend.domain.dto.UpdateTestTemplateRequest;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.TestBlueprintDto;
import app.skillsoft.assessmentbackend.domain.dto.validation.BlueprintValidationResult;
import app.skillsoft.assessmentbackend.domain.entities.TestTemplate;
import app.skillsoft.assessmentbackend.exception.ResourceNotFoundException;
import app.skillsoft.assessmentbackend.repository.TestTemplateRepository;
import app.skillsoft.assessmentbackend.repository.UserRepository;
import app.skillsoft.assessmentbackend.services.BlueprintConversionService;
import app.skillsoft.assessmentbackend.services.TestTemplateService;
import app.skillsoft.assessmentbackend.services.validation.BlueprintValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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
    private final BlueprintValidationService blueprintValidationService;

    public TestTemplateServiceImpl(
            TestTemplateRepository templateRepository,
            UserRepository userRepository,
            BlueprintConversionService blueprintConversionService,
            BlueprintValidationService blueprintValidationService) {
        this.templateRepository = templateRepository;
        this.userRepository = userRepository;
        this.blueprintConversionService = blueprintConversionService;
        this.blueprintValidationService = blueprintValidationService;
    }

    @Override
    public Page<TestTemplateSummaryDto> listTemplates(Pageable pageable) {
        // Exclude soft-deleted templates from listing
        return templateRepository.findByDeletedAtIsNull(pageable)
                .map(this::toSummaryDto);
    }

    @Override
    @Cacheable(value = CacheConfig.ACTIVE_TEMPLATES_CACHE)
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
    @CacheEvict(value = CacheConfig.ACTIVE_TEMPLATES_CACHE, allEntries = true)
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

        // Note: competencyIds is deprecated; not populated for new templates.
        // Existing templates may still have data in this field for backward compatibility.

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
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.TEMPLATE_METADATA_CACHE, key = "#id"),
        @CacheEvict(value = CacheConfig.ACTIVE_TEMPLATES_CACHE, allEntries = true)
    })
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
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.TEMPLATE_METADATA_CACHE, key = "#id"),
        @CacheEvict(value = CacheConfig.ACTIVE_TEMPLATES_CACHE, allEntries = true)
    })
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
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.TEMPLATE_METADATA_CACHE, key = "#id"),
        @CacheEvict(value = CacheConfig.ACTIVE_TEMPLATES_CACHE, allEntries = true)
    })
    public TestTemplateDto activateTemplate(UUID id) {
        TestTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TestTemplate", id));
        template.setIsActive(true);
        return toDto(templateRepository.save(template));
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.TEMPLATE_METADATA_CACHE, key = "#id"),
        @CacheEvict(value = CacheConfig.ACTIVE_TEMPLATES_CACHE, allEntries = true)
    })
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
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.TEMPLATE_METADATA_CACHE, key = "#templateId"),
        @CacheEvict(value = CacheConfig.ACTIVE_TEMPLATES_CACHE, allEntries = true)
    })
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

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.ACTIVE_TEMPLATES_CACHE, allEntries = true)
    public TestTemplateDto cloneTemplate(UUID templateId) {
        log.info("Cloning template: {}", templateId);

        // 1. Find the source template
        TestTemplate source = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("TestTemplate", templateId));

        // 2. Determine a unique name for the clone
        String cloneName = "Copy of " + source.getName();
        if (templateRepository.existsByNameIgnoreCaseAndDeletedAtIsNull(cloneName)) {
            cloneName = cloneName + " (" + System.currentTimeMillis() + ")";
        }

        // 3. Create new template with deep-copied fields and reset metadata
        TestTemplate clone = new TestTemplate();
        clone.setName(cloneName);
        clone.setDescription(source.getDescription());
        clone.setGoal(source.getGoal());

        // Deep copy blueprint configurations
        if (source.getBlueprint() != null) {
            clone.setBlueprint(new java.util.HashMap<>(source.getBlueprint()));
        }
        if (source.getTypedBlueprint() != null) {
            clone.setTypedBlueprint(source.getTypedBlueprint().deepCopy());
        }
        if (source.getCompetencyIds() != null) {
            clone.setCompetencyIds(new ArrayList<>(source.getCompetencyIds()));
        }

        // Copy test configuration settings
        clone.setQuestionsPerIndicator(source.getQuestionsPerIndicator());
        clone.setTimeLimitMinutes(source.getTimeLimitMinutes());
        clone.setPassingScore(source.getPassingScore());
        clone.setShuffleQuestions(source.getShuffleQuestions());
        clone.setShuffleOptions(source.getShuffleOptions());
        clone.setAllowSkip(source.getAllowSkip());
        clone.setAllowBackNavigation(source.getAllowBackNavigation());
        clone.setShowResultsImmediately(source.getShowResultsImmediately());

        // Reset metadata: brand new template, not a version
        clone.setVersion(1);
        clone.setParentId(null);
        clone.setStatus(TemplateStatus.DRAFT);
        clone.setIsActive(true);
        clone.setVisibility(TemplateVisibility.PRIVATE);

        // Auto-convert legacy blueprint to typed blueprint for test assembly
        blueprintConversionService.ensureTypedBlueprint(clone);

        TestTemplate saved = templateRepository.save(clone);
        log.info("Cloned template {} -> new template {} (name: '{}')",
                templateId, saved.getId(), saved.getName());
        return toDto(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.ACTIVE_TEMPLATES_CACHE, allEntries = true)
    public TestTemplateDto createNextVersion(UUID templateId, boolean archiveOriginal) {
        log.info("Creating next version for template: {}, archiveOriginal: {}", templateId, archiveOriginal);

        // 1. Find the source template
        TestTemplate source = templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("TestTemplate", templateId));

        // 2. Only non-DRAFT templates should be versioned (DRAFT can be edited directly)
        if (source.getStatus() == TemplateStatus.DRAFT) {
            throw new IllegalStateException("Template is already in DRAFT status. Edit it directly.");
        }

        // 3. Create next version using the entity method (preserves parentId chain)
        TestTemplate nextVersion = source.createNextVersion();

        // 4. Ensure blueprint conversion for the new version
        blueprintConversionService.ensureTypedBlueprint(nextVersion);

        // 5. Persist the new version
        TestTemplate savedVersion = templateRepository.save(nextVersion);
        log.info("Created template version {} (id: {}) from parent {} (id: {})",
                savedVersion.getVersion(), savedVersion.getId(), source.getVersion(), source.getId());

        // 6. Optionally archive the original
        if (archiveOriginal) {
            source.archive();
            templateRepository.save(source);
            log.info("Archived original template: {}", templateId);
        }

        return toDto(savedVersion);
    }

    /**
     * Validates that a template has all required configuration to be published.
     *
     * <p>Delegates to {@link BlueprintValidationService} for centralized validation.
     * Returns a flat list of error message strings for backward compatibility with
     * the existing publish flow.</p>
     *
     * @param template The template to validate
     * @return List of validation error messages (empty if valid)
     */
    private List<String> validateTemplateForPublishing(TestTemplate template) {
        BlueprintValidationResult result = blueprintValidationService.validateForPublishing(template);

        // Log warnings even though they don't block publishing
        if (result.hasWarnings()) {
            log.info("Template {} has {} validation warnings: {}",
                    template.getId(),
                    result.warnings().size(),
                    result.warnings().stream()
                            .map(w -> w.id() + ": " + w.message())
                            .toList());
        }

        return result.errorMessages();
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
                blueprintConversionService.hasValidBlueprint(template),
                template.getVersion(),
                template.getParentId(),
                template.getStatus() != null ? template.getStatus().name() : null
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
                template.getCreatedAt(),
                template.getStatus() != null ? template.getStatus().name() : null
        );
    }
}
