package app.skillsoft.assessmentbackend.services.impl;

import app.skillsoft.assessmentbackend.domain.dto.CreateTestTemplateRequest;
import app.skillsoft.assessmentbackend.domain.dto.TestTemplateDto;
import app.skillsoft.assessmentbackend.domain.dto.TestTemplateSummaryDto;
import app.skillsoft.assessmentbackend.domain.dto.UpdateTestTemplateRequest;
import app.skillsoft.assessmentbackend.domain.entities.TestTemplate;
import app.skillsoft.assessmentbackend.repository.TestTemplateRepository;
import app.skillsoft.assessmentbackend.services.TestTemplateService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TestTemplateServiceImpl implements TestTemplateService {

    private final TestTemplateRepository templateRepository;

    public TestTemplateServiceImpl(TestTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    @Override
    public Page<TestTemplateSummaryDto> listTemplates(Pageable pageable) {
        return templateRepository.findAll(pageable)
                .map(this::toSummaryDto);
    }

    @Override
    public List<TestTemplateSummaryDto> listActiveTemplates() {
        return templateRepository.findByIsActiveTrue().stream()
                .map(this::toSummaryDto)
                .toList();
    }

    @Override
    public Optional<TestTemplateDto> findById(UUID id) {
        return templateRepository.findById(id)
                .map(this::toDto);
    }

    @Override
    @Transactional
    public TestTemplateDto createTemplate(CreateTestTemplateRequest request) {
        // Validate that template name is unique
        if (templateRepository.existsByNameIgnoreCase(request.name())) {
            throw new IllegalArgumentException("Template with name '" + request.name() + "' already exists");
        }

        TestTemplate template = new TestTemplate();
        template.setName(request.name());
        template.setDescription(request.description());
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

        TestTemplate saved = templateRepository.save(template);
        return toDto(saved);
    }

    @Override
    @Transactional
    public TestTemplateDto updateTemplate(UUID id, UpdateTestTemplateRequest request) {
        TestTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found with id: " + id));

        // Update only provided fields
        if (request.name() != null) {
            // Check uniqueness if name is being changed
            if (!template.getName().equalsIgnoreCase(request.name()) 
                    && templateRepository.existsByNameIgnoreCase(request.name())) {
                throw new IllegalArgumentException("Template with name '" + request.name() + "' already exists");
            }
            template.setName(request.name());
        }
        if (request.description() != null) {
            template.setDescription(request.description());
        }
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

        TestTemplate saved = templateRepository.save(template);
        return toDto(saved);
    }

    @Override
    @Transactional
    public boolean deleteTemplate(UUID id) {
        if (templateRepository.existsById(id)) {
            templateRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public TestTemplateDto activateTemplate(UUID id) {
        TestTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found with id: " + id));
        template.setIsActive(true);
        return toDto(templateRepository.save(template));
    }

    @Override
    @Transactional
    public TestTemplateDto deactivateTemplate(UUID id) {
        TestTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found with id: " + id));
        template.setIsActive(false);
        return toDto(templateRepository.save(template));
    }

    @Override
    public List<TestTemplateSummaryDto> searchByName(String name) {
        return templateRepository.findByNameContainingIgnoreCaseAndIsActiveTrue(name).stream()
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
        long total = templateRepository.count();
        long active = templateRepository.countByIsActiveTrue();
        return new TemplateStatistics(total, active, total - active);
    }

    // Mapping methods
    private TestTemplateDto toDto(TestTemplate template) {
        return new TestTemplateDto(
                template.getId(),
                template.getName(),
                template.getDescription(),
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
                template.getUpdatedAt()
        );
    }

    private TestTemplateSummaryDto toSummaryDto(TestTemplate template) {
        return new TestTemplateSummaryDto(
                template.getId(),
                template.getName(),
                template.getDescription(),
                template.getCompetencyIds() != null ? template.getCompetencyIds().size() : 0,
                template.getTimeLimitMinutes(),
                template.getPassingScore(),
                template.getIsActive(),
                template.getCreatedAt()
        );
    }
}
