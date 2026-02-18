package app.skillsoft.assessmentbackend.services.validation;

import app.skillsoft.assessmentbackend.domain.dto.blueprint.OverviewBlueprint;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.TestBlueprintDto;
import app.skillsoft.assessmentbackend.domain.dto.validation.BlueprintValidationResult;
import app.skillsoft.assessmentbackend.domain.dto.validation.ValidationIssue;
import app.skillsoft.assessmentbackend.domain.entities.TestTemplate;
import app.skillsoft.assessmentbackend.repository.AssessmentQuestionRepository;
import app.skillsoft.assessmentbackend.services.BlueprintConversionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Centralized implementation of blueprint validation.
 *
 * <p>Consolidates validation logic from {@code TestTemplateServiceImpl},
 * {@code TestSimulatorService}, and frontend preflight checks into a
 * single source of truth.</p>
 *
 * <p>All validation checks produce structured {@link ValidationIssue} objects
 * with machine-readable IDs, enabling the frontend to map issues to specific
 * UI elements (e.g., highlighting a competency row with insufficient questions).</p>
 *
 * <h3>Issue ID Conventions:</h3>
 * <ul>
 *   <li>{@code null-blueprint} - Blueprint object is missing</li>
 *   <li>{@code no-competencies} - No competencies configured</li>
 *   <li>{@code no-strategy} - Strategy/goal not set</li>
 *   <li>{@code invalid-time-limit} - Time limit is missing or non-positive</li>
 *   <li>{@code invalid-passing-score} - Passing score outside 0-100 range</li>
 *   <li>{@code no-template-name} - Template name is blank</li>
 *   <li>{@code no-assessment-goal} - Assessment goal not defined</li>
 *   <li>{@code no-questions-for-competency} - Zero questions available for a competency</li>
 *   <li>{@code low-question-count} - Fewer than 3 questions for a competency</li>
 *   <li>{@code weight-concentration} - Single competency weight exceeds 50% of total</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BlueprintValidationServiceImpl implements BlueprintValidationService {

    private static final int MIN_QUESTIONS_PER_COMPETENCY = 3;
    private static final double WEIGHT_CONCENTRATION_THRESHOLD = 0.50;

    private final AssessmentQuestionRepository questionRepository;
    private final BlueprintConversionService blueprintConversionService;

    @Override
    public BlueprintValidationResult validateForPublishing(TestTemplate template) {
        log.debug("Validating template for publishing: {}", template != null ? template.getId() : "null");

        var errors = new ArrayList<ValidationIssue>();
        var warnings = new ArrayList<ValidationIssue>();

        // Run all validation checks
        validateTemplateName(template, errors);
        validateAssessmentGoal(template, errors);
        validateTimeLimit(template, errors);
        validatePassingScore(template, errors);

        // Attempt blueprint conversion and validate
        TestBlueprintDto blueprint = ensureAndGetBlueprint(template, errors);

        if (blueprint != null) {
            validateStrategy(blueprint, errors);
            List<UUID> competencyIds = extractCompetencyIds(blueprint);
            validateCompetencies(competencyIds, errors);

            // Inventory checks only if we have competencies to check
            if (!competencyIds.isEmpty()) {
                validateQuestionInventory(competencyIds, errors, warnings);
                checkWeightDistribution(blueprint, competencyIds, warnings);
            }
        }

        boolean hasErrors = !errors.isEmpty();
        boolean canPublish = !hasErrors;
        // Simulation is possible with a valid blueprint even if there are warnings
        boolean canSimulate = blueprint != null && errors.stream()
                .noneMatch(e -> "null-blueprint".equals(e.id()) || "no-competencies".equals(e.id()));

        var result = new BlueprintValidationResult(
                !hasErrors,
                Collections.unmodifiableList(errors),
                Collections.unmodifiableList(warnings),
                canSimulate,
                canPublish
        );

        log.debug("Validation result for template {}: valid={}, errors={}, warnings={}",
                template != null ? template.getId() : "null",
                result.valid(), errors.size(), warnings.size());

        return result;
    }

    @Override
    public BlueprintValidationResult validateForSimulation(TestTemplate template) {
        log.debug("Validating template for simulation: {}", template != null ? template.getId() : "null");

        var errors = new ArrayList<ValidationIssue>();
        var warnings = new ArrayList<ValidationIssue>();

        // Simulation requires a valid blueprint but is less strict on template metadata
        TestBlueprintDto blueprint = ensureAndGetBlueprint(template, errors);

        if (blueprint != null) {
            validateStrategy(blueprint, errors);
            List<UUID> competencyIds = extractCompetencyIds(blueprint);
            validateCompetencies(competencyIds, errors);

            if (!competencyIds.isEmpty()) {
                // For simulation, low question count is a warning, not blocking
                validateQuestionInventoryForSimulation(competencyIds, errors, warnings);
            }
        }

        boolean hasErrors = !errors.isEmpty();
        boolean canSimulate = !hasErrors;

        var result = new BlueprintValidationResult(
                !hasErrors,
                Collections.unmodifiableList(errors),
                Collections.unmodifiableList(warnings),
                canSimulate,
                false // canPublish is not relevant for simulation validation
        );

        log.debug("Simulation validation result for template {}: canSimulate={}, errors={}, warnings={}",
                template != null ? template.getId() : "null",
                result.canSimulate(), errors.size(), warnings.size());

        return result;
    }

    // ========================================================================
    // Individual Validation Checks
    // ========================================================================

    /**
     * Validate that the template has a non-blank name.
     */
    private void validateTemplateName(TestTemplate template, List<ValidationIssue> errors) {
        if (template == null) {
            return; // Template-level null is handled elsewhere
        }
        if (template.getName() == null || template.getName().trim().isEmpty()) {
            errors.add(ValidationIssue.error("no-template-name", "Template must have a name"));
        }
    }

    /**
     * Validate that the assessment goal is set.
     */
    private void validateAssessmentGoal(TestTemplate template, List<ValidationIssue> errors) {
        if (template == null) {
            return;
        }
        if (template.getGoal() == null) {
            errors.add(ValidationIssue.error("no-assessment-goal", "Template must have an assessment goal defined"));
        }
    }

    /**
     * Validate that the time limit is positive.
     */
    private void validateTimeLimit(TestTemplate template, List<ValidationIssue> errors) {
        if (template == null) {
            return;
        }
        if (template.getTimeLimitMinutes() == null || template.getTimeLimitMinutes() <= 0) {
            errors.add(ValidationIssue.error("invalid-time-limit",
                    "Template must have a valid time limit (> 0 minutes)"));
        }
    }

    /**
     * Validate that the passing score is within 0-100 range.
     */
    private void validatePassingScore(TestTemplate template, List<ValidationIssue> errors) {
        if (template == null) {
            return;
        }
        if (template.getPassingScore() == null
                || template.getPassingScore() < 0
                || template.getPassingScore() > 100) {
            errors.add(ValidationIssue.error("invalid-passing-score",
                    "Template must have a valid passing score (0-100)"));
        }
    }

    /**
     * Ensure the template has a typed blueprint, attempting conversion if necessary.
     *
     * @return The typed blueprint, or null if no valid blueprint exists
     */
    private TestBlueprintDto ensureAndGetBlueprint(TestTemplate template, List<ValidationIssue> errors) {
        if (template == null) {
            errors.add(ValidationIssue.error("null-blueprint", "Template is null"));
            return null;
        }

        // Attempt auto-conversion of legacy blueprint
        blueprintConversionService.ensureTypedBlueprint(template);

        TestBlueprintDto blueprint = template.getTypedBlueprint();
        if (blueprint == null) {
            errors.add(ValidationIssue.error("null-blueprint",
                    "Template must have a valid blueprint configured. "
                            + "Please go to the Blueprint tab and add at least one competency."));
            return null;
        }

        return blueprint;
    }

    /**
     * Validate that the blueprint has a strategy set.
     */
    private void validateStrategy(TestBlueprintDto blueprint, List<ValidationIssue> errors) {
        if (blueprint.getStrategy() == null) {
            errors.add(ValidationIssue.error("no-strategy", "Blueprint must have a strategy defined"));
        }
    }

    /**
     * Validate that the blueprint has at least one competency.
     */
    private void validateCompetencies(List<UUID> competencyIds, List<ValidationIssue> errors) {
        if (competencyIds.isEmpty()) {
            errors.add(ValidationIssue.error("no-competencies",
                    "Blueprint must include at least one competency"));
        }
    }

    /**
     * Validate question inventory for each competency (publishing mode).
     *
     * <p>Zero questions for a competency is an ERROR (blocks publishing).
     * Fewer than {@value MIN_QUESTIONS_PER_COMPETENCY} questions is a WARNING.</p>
     */
    private void validateQuestionInventory(
            List<UUID> competencyIds,
            List<ValidationIssue> errors,
            List<ValidationIssue> warnings
    ) {
        for (UUID competencyId : competencyIds) {
            long questionCount = questionRepository.countActiveQuestionsForCompetency(competencyId);

            if (questionCount == 0) {
                errors.add(ValidationIssue.errorForCompetency(
                        "no-questions-for-competency",
                        String.format("Competency %s has no available questions", competencyId),
                        competencyId.toString()
                ));
            } else if (questionCount < MIN_QUESTIONS_PER_COMPETENCY) {
                warnings.add(ValidationIssue.warningForCompetency(
                        "low-question-count",
                        String.format("Competency %s has only %d questions (recommended minimum: %d)",
                                competencyId, questionCount, MIN_QUESTIONS_PER_COMPETENCY),
                        competencyId.toString()
                ));
            }
        }
    }

    /**
     * Validate question inventory for simulation mode.
     *
     * <p>For simulation, zero questions is still an ERROR (simulation cannot assemble questions).
     * Low question count is a WARNING (simulation can still proceed with limited data).</p>
     */
    private void validateQuestionInventoryForSimulation(
            List<UUID> competencyIds,
            List<ValidationIssue> errors,
            List<ValidationIssue> warnings
    ) {
        for (UUID competencyId : competencyIds) {
            long questionCount = questionRepository.countActiveQuestionsForCompetency(competencyId);

            if (questionCount == 0) {
                errors.add(ValidationIssue.errorForCompetency(
                        "no-questions-for-competency",
                        String.format("Competency %s has no available questions for simulation", competencyId),
                        competencyId.toString()
                ));
            } else if (questionCount < MIN_QUESTIONS_PER_COMPETENCY) {
                warnings.add(ValidationIssue.warningForCompetency(
                        "low-question-count",
                        String.format("Competency %s has only %d questions (recommended minimum: %d)",
                                competencyId, questionCount, MIN_QUESTIONS_PER_COMPETENCY),
                        competencyId.toString()
                ));
            }
        }
    }

    /**
     * Check for weight concentration -- warn if any single competency's weight
     * exceeds {@value WEIGHT_CONCENTRATION_THRESHOLD} of total weight.
     *
     * <p>Currently only applicable to {@link OverviewBlueprint} where competencies
     * are equally weighted (so this triggers when there is only 1 competency).
     * When per-competency weights are added, this will check actual weight values.</p>
     */
    private void checkWeightDistribution(
            TestBlueprintDto blueprint,
            List<UUID> competencyIds,
            List<ValidationIssue> warnings
    ) {
        if (competencyIds.size() <= 1) {
            // Single competency inherently has 100% weight, warn about concentration
            if (competencyIds.size() == 1) {
                warnings.add(ValidationIssue.warning("weight-concentration",
                        "Single competency carries 100% of assessment weight. "
                                + "Consider adding more competencies for a balanced assessment."));
            }
            return;
        }

        // For equal-weight blueprints: each competency has weight = 1/N
        // Concentration warning triggers if any single weight > 50%
        // With equal weights, this only happens when N < 2 (handled above)
        // This section is a placeholder for future per-competency weight support

        // For OverviewBlueprint: all competencies have equal weight, no concentration issue with N >= 2
        // For future weighted blueprints: iterate weight map and check threshold
    }

    // ========================================================================
    // Blueprint Competency Extraction
    // ========================================================================

    /**
     * Extract competency IDs from the polymorphic blueprint.
     *
     * <p>Currently, only {@link OverviewBlueprint} has a direct {@code competencyIds} list.
     * {@code JobFitBlueprint} and {@code TeamFitBlueprint} derive competencies from
     * O*NET codes and team configurations respectively -- those are resolved at assembly time,
     * so this method returns an empty list for those strategies.</p>
     *
     * @param blueprint The typed blueprint
     * @return List of competency UUIDs, or empty list if not directly available
     */
    List<UUID> extractCompetencyIds(TestBlueprintDto blueprint) {
        if (blueprint instanceof OverviewBlueprint overviewBlueprint) {
            List<UUID> ids = overviewBlueprint.getCompetencyIds();
            return ids != null ? ids : Collections.emptyList();
        }

        // JobFitBlueprint and TeamFitBlueprint resolve competencies at assembly time
        // via O*NET SOC code or team configuration. We cannot validate inventory here.
        return Collections.emptyList();
    }
}
