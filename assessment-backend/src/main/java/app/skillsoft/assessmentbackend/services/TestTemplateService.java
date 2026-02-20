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
     * Get active templates owned by the authenticated user.
     * Used for personal mode catalog.
     *
     * @param clerkId The Clerk ID of the authenticated user
     * @return List of template summaries owned by the user
     */
    List<TestTemplateSummaryDto> listMyTemplates(String clerkId);

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
     * Clone an existing template with reset metadata.
     * Creates a deep copy with name "Copy of X", new UUID, DRAFT status.
     *
     * @param templateId The ID of the template to clone
     * @return The cloned template DTO
     * @throws app.skillsoft.assessmentbackend.exception.ResourceNotFoundException if template not found
     */
    TestTemplateDto cloneTemplate(UUID templateId);

    /**
     * Publish a test template, making it available for test sessions.
     *
     * Publishing transitions the template from DRAFT to PUBLISHED status,
     * making it immutable and available for use in test sessions.
     *
     * @param templateId The ID of the template to publish
     * @return PublishResult containing success status, version number, and message
     * @throws app.skillsoft.assessmentbackend.exception.ResourceNotFoundException if template not found
     * @throws IllegalStateException if template cannot be published (not in DRAFT status)
     * @throws IllegalArgumentException if template is not ready to publish (missing configuration)
     */
    PublishResult publishTemplate(UUID templateId);

    /**
     * Result record for template publishing operation.
     */
    record PublishResult(
            boolean published,
            int version,
            String message
    ) {}

    /**
     * Statistics record for templates.
     */
    record TemplateStatistics(
            long totalTemplates,
            long activeTemplates,
            long inactiveTemplates
    ) {}
}
