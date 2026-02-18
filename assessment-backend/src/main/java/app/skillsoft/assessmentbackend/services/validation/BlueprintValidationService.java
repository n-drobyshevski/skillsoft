package app.skillsoft.assessmentbackend.services.validation;

import app.skillsoft.assessmentbackend.domain.dto.validation.BlueprintValidationResult;
import app.skillsoft.assessmentbackend.domain.entities.TestTemplate;

/**
 * Centralized validation service for test template blueprints.
 *
 * <p>Consolidates all blueprint validation logic that was previously scattered across
 * {@code TestTemplateServiceImpl.validateTemplateForPublishing()},
 * the frontend {@code usePreflightValidation} hook, and
 * {@code TestSimulatorService.simulate()}.</p>
 *
 * <p>Returns structured {@link BlueprintValidationResult} containing typed errors
 * and warnings instead of flat string lists, enabling the frontend to render
 * targeted validation feedback.</p>
 *
 * <h3>Validation Checks:</h3>
 * <ul>
 *   <li>Blueprint existence and structure</li>
 *   <li>Competency configuration (at least one required)</li>
 *   <li>Strategy/goal presence</li>
 *   <li>Time limit validity (&gt; 0 minutes)</li>
 *   <li>Passing score validity (0-100)</li>
 *   <li>Template name presence</li>
 *   <li>Per-competency question inventory checks</li>
 *   <li>Weight distribution warnings</li>
 *   <li>Low question count warnings</li>
 * </ul>
 */
public interface BlueprintValidationService {

    /**
     * Validate a template for publishing.
     *
     * <p>Runs all validation checks including inventory verification
     * against the question repository.</p>
     *
     * @param template The test template to validate
     * @return Structured validation result with errors and warnings
     */
    BlueprintValidationResult validateForPublishing(TestTemplate template);

    /**
     * Validate a template for simulation (dry run).
     *
     * <p>Runs a subset of validation checks sufficient for simulation.
     * Less strict than publishing validation -- for example, warnings
     * about low question counts do not block simulation.</p>
     *
     * @param template The test template to validate
     * @return Structured validation result with errors and warnings
     */
    BlueprintValidationResult validateForSimulation(TestTemplate template);
}
