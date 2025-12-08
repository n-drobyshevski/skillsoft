package app.skillsoft.assessmentbackend.services.assembly;

import app.skillsoft.assessmentbackend.domain.dto.blueprint.TestBlueprintDto;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory for selecting the appropriate TestAssembler based on blueprint strategy.
 * 
 * Uses Spring dependency injection to collect all TestAssembler implementations
 * and routes requests to the correct one based on AssessmentGoal.
 */
@Component
@RequiredArgsConstructor
public class TestAssemblerFactory {

    private final List<TestAssembler> assemblers;

    /**
     * Lazily built map of AssessmentGoal to TestAssembler.
     */
    private Map<AssessmentGoal, TestAssembler> assemblerMap;

    /**
     * Get the appropriate assembler for the given blueprint.
     * 
     * @param blueprint The test blueprint configuration
     * @return The matching TestAssembler
     * @throws IllegalArgumentException if no assembler supports the blueprint's strategy
     */
    public TestAssembler getAssembler(TestBlueprintDto blueprint) {
        if (blueprint == null) {
            throw new IllegalArgumentException("Blueprint cannot be null");
        }
        
        var goal = blueprint.getStrategy();
        if (goal == null) {
            throw new IllegalArgumentException("Blueprint strategy cannot be null");
        }
        
        var assembler = getAssemblerMap().get(goal);
        if (assembler == null) {
            throw new IllegalArgumentException(
                "No assembler found for strategy: " + goal + 
                ". Available strategies: " + getAssemblerMap().keySet()
            );
        }
        
        return assembler;
    }

    /**
     * Get the assembler for a specific AssessmentGoal.
     * 
     * @param goal The assessment goal type
     * @return The matching TestAssembler
     * @throws IllegalArgumentException if no assembler supports the goal
     */
    public TestAssembler getAssembler(AssessmentGoal goal) {
        if (goal == null) {
            throw new IllegalArgumentException("Goal cannot be null");
        }
        
        var assembler = getAssemblerMap().get(goal);
        if (assembler == null) {
            throw new IllegalArgumentException(
                "No assembler found for goal: " + goal
            );
        }
        
        return assembler;
    }

    /**
     * Check if an assembler is available for the given strategy.
     * 
     * @param goal The assessment goal to check
     * @return true if an assembler is registered for this goal
     */
    public boolean hasAssembler(AssessmentGoal goal) {
        return goal != null && getAssemblerMap().containsKey(goal);
    }

    /**
     * Get all registered assemblers.
     * 
     * @return List of all available TestAssembler implementations
     */
    public List<TestAssembler> getAllAssemblers() {
        return List.copyOf(assemblers);
    }

    /**
     * Get or build the assembler map.
     */
    private Map<AssessmentGoal, TestAssembler> getAssemblerMap() {
        if (assemblerMap == null) {
            assemblerMap = assemblers.stream()
                .collect(Collectors.toMap(
                    TestAssembler::getSupportedGoal,
                    Function.identity(),
                    (existing, replacement) -> {
                        throw new IllegalStateException(
                            "Duplicate assembler for goal: " + existing.getSupportedGoal()
                        );
                    }
                ));
        }
        return assemblerMap;
    }
}
