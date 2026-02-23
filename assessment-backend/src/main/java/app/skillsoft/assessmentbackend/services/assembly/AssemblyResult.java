package app.skillsoft.assessmentbackend.services.assembly;

import app.skillsoft.assessmentbackend.domain.dto.simulation.InventoryWarning;

import java.util.List;
import java.util.UUID;

/**
 * Result of test assembly: selected question IDs + any diagnostic warnings.
 */
public record AssemblyResult(
    List<UUID> questionIds,
    List<InventoryWarning> warnings
) {
    /** Convenience factory for results with no warnings. */
    public static AssemblyResult of(List<UUID> questionIds) {
        return new AssemblyResult(questionIds, List.of());
    }

    /** Convenience factory for empty results with no warnings. */
    public static AssemblyResult empty() {
        return new AssemblyResult(List.of(), List.of());
    }
}
