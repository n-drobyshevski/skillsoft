package app.skillsoft.assessmentbackend.domain.dto.simulation;

import java.util.Map;
import java.util.UUID;

/**
 * Warning record for inventory issues detected during simulation.
 *
 * The {@code code} + {@code params} fields enable frontend i18n:
 * the frontend uses {@code code} to look up a localized message template
 * and interpolates {@code params} into it. The {@code message} field
 * serves as an English fallback for logging and debugging.
 */
public record InventoryWarning(
    WarningLevel level,
    WarningCode code,
    UUID competencyId,
    String competencyName,
    String difficulty,
    int availableQuestions,
    int requiredQuestions,
    String message,
    Map<String, String> params
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
            WarningCode.INVENTORY_CRITICAL,
            competencyId,
            competencyName,
            difficulty,
            available,
            required,
            String.format("Critical: Only %d questions available for %s (%s), need %d",
                available, competencyName, difficulty, required),
            Map.of(
                "available", String.valueOf(available),
                "competencyName", competencyName,
                "difficulty", difficulty,
                "required", String.valueOf(required)
            )
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
            WarningCode.INVENTORY_LIMITED,
            competencyId,
            competencyName,
            difficulty,
            available,
            required,
            String.format("Warning: Limited questions (%d) for %s (%s), recommended %d",
                available, competencyName, difficulty, required),
            Map.of(
                "available", String.valueOf(available),
                "competencyName", competencyName,
                "difficulty", difficulty,
                "recommended", String.valueOf(required)
            )
        );
    }

    /**
     * Create an info-level inventory message.
     */
    public static InventoryWarning info(String message) {
        return new InventoryWarning(
            WarningLevel.INFO,
            WarningCode.GENERIC,
            null,
            null,
            null,
            0,
            0,
            message,
            null
        );
    }

    /**
     * Create an assembly-level warning with code and i18n params.
     */
    public static InventoryWarning assemblyWarning(
            WarningLevel level, WarningCode code, String message, Map<String, String> params) {
        return new InventoryWarning(
            level,
            code,
            null,
            null,
            null,
            0,
            0,
            message,
            params
        );
    }

    /**
     * Create an assembly-level warning with code (no params).
     */
    public static InventoryWarning assemblyWarning(WarningLevel level, WarningCode code, String message) {
        return assemblyWarning(level, code, message, null);
    }

    /**
     * Create an assembly-level warning with GENERIC code (no params).
     */
    public static InventoryWarning assemblyWarning(WarningLevel level, String message) {
        return assemblyWarning(level, WarningCode.GENERIC, message, null);
    }
}
