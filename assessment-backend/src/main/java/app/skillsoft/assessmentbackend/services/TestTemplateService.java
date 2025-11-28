package app.skillsoft.assessmentbackend.services;

import app.skillsoft.assessmentbackend.domain.dto.CreateTestTemplateRequest;
import app.skillsoft.assessmentbackend.domain.dto.TestTemplateDto;
import app.skillsoft.assessmentbackend.domain.dto.TestTemplateSummaryDto;
import app.skillsoft.assessmentbackend.domain.dto.UpdateTestTemplateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for managing test templates.
 */
public interface TestTemplateService {

    /**
     * Get all test templates with pagination.
     */
    Page<TestTemplateSummaryDto> listTemplates(Pageable pageable);

    /**
     * Get all active test templates.
     */
    List<TestTemplateSummaryDto> listActiveTemplates();

    /**
     * Get a test template by ID.
     */
    Optional<TestTemplateDto> findById(UUID id);

    /**
     * Create a new test template.
     */
    TestTemplateDto createTemplate(CreateTestTemplateRequest request);

    /**
     * Update an existing test template.
     */
    TestTemplateDto updateTemplate(UUID id, UpdateTestTemplateRequest request);

    /**
     * Delete a test template.
     */
    boolean deleteTemplate(UUID id);

    /**
     * Activate a test template.
     */
    TestTemplateDto activateTemplate(UUID id);

    /**
     * Deactivate a test template.
     */
    TestTemplateDto deactivateTemplate(UUID id);

    /**
     * Search templates by name.
     */
    List<TestTemplateSummaryDto> searchByName(String name);

    /**
     * Find templates that include a specific competency.
     */
    List<TestTemplateSummaryDto> findByCompetency(UUID competencyId);

    /**
     * Get template statistics (counts, etc.).
     */
    TemplateStatistics getStatistics();

    /**
     * Statistics record for templates.
     */
    record TemplateStatistics(
            long totalTemplates,
            long activeTemplates,
            long inactiveTemplates
    ) {}
}
