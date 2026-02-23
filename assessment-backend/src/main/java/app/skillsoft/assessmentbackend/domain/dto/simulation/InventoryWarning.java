package app.skillsoft.assessmentbackend.domain.dto.simulation;

import java.util.UUID;

/**
 * Warning record for inventory issues detected during simulation.
 */
public record InventoryWarning(
    WarningLevel level,
    UUID competencyId,
    String competencyName,
    String difficulty,
    int availableQuestions,
    int requiredQuestions,
    String message
) {
    /**
     * Warning severity levels.
     */
    public enum WarningLevel {
        INFO,
        WARNING,
        ERROR
    }

    /**
     * Create a critical inventory warning.
     */
    public static InventoryWarning critical(
            UUID competencyId,
            String competencyName,
            String difficulty,
            int available,
            int required
    ) {
        return new InventoryWarning(
            WarningLevel.ERROR,
            competencyId,
            competencyName,
            difficulty,
            available,
            required,
            String.format("Critical: Only %d questions available for %s (%s), need %d",
                available, competencyName, difficulty, required)
        );
    }

    /**
     * Create a moderate inventory warning.
     */
    public static InventoryWarning moderate(
            UUID competencyId,
            String competencyName,
            String difficulty,
            int available,
            int required
    ) {
        return new InventoryWarning(
            WarningLevel.WARNING,
            competencyId,
            competencyName,
            difficulty,
            available,
            required,
            String.format("Warning: Limited questions (%d) for %s (%s), recommended %d",
                available, competencyName, difficulty, required)
        );
    }

    /**
     * Create an info-level inventory message.
     */
    public static InventoryWarning info(String message) {
        return new InventoryWarning(
            WarningLevel.INFO,
            null,
            null,
            null,
            0,
            0,
            message
        );
    }

    /**
     * Create an assembly-level warning with a severity level.
     * Used for diagnostics like fuzzy matches, lookup fallbacks, etc.
     */
    public static InventoryWarning assemblyWarning(WarningLevel level, String message) {
        return new InventoryWarning(
            level,
            null,
            null,
            null,
            0,
            0,
            message
        );
    }
}
