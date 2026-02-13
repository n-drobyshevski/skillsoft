package app.skillsoft.assessmentbackend.services.assembly;

import app.skillsoft.assessmentbackend.domain.dto.blueprint.TestBlueprintDto;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;

import java.util.List;
import java.util.UUID;

/**
 * Strategy interface for test question assembly.
 * 
 * Each implementation handles a specific AssessmentGoal type and
 * implements the algorithm for selecting appropriate questions
 * based on the blueprint configuration.
 * 
 * Part of the Strategy Pattern for polymorphic test assembly.
 */
public interface TestAssembler {

    /**
     * Assemble a list of question UUIDs based on the blueprint configuration.
     * 
     * The implementation should:
     * 1. Extract relevant parameters from the blueprint
     * 2. Query for appropriate questions based on strategy-specific criteria
     * 3. Apply ordering/distribution algorithms
     * 4. Return the final list of question IDs for the test
     * 
     * @param blueprint The polymorphic blueprint configuration
     * @return List of question UUIDs in presentation order
     */
    List<UUID> assemble(TestBlueprintDto blueprint);

    /**
     * Get the AssessmentGoal this assembler supports.
     * Used by TestAssemblerFactory to select the correct implementation.
     * 
     * @return The supported AssessmentGoal type
     */
    AssessmentGoal getSupportedGoal();

    /**
     * Validate that the blueprint is compatible with this assembler.
     * 
     * @param blueprint The blueprint to validate
     * @return true if the blueprint can be processed by this assembler
     */
    default boolean supports(TestBlueprintDto blueprint) {
        return blueprint != null && 
               blueprint.getStrategy() == getSupportedGoal();
    }
}
