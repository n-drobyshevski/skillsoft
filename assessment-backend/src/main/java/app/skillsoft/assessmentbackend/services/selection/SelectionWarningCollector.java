package app.skillsoft.assessmentbackend.services.selection;

import app.skillsoft.assessmentbackend.domain.dto.simulation.InventoryWarning;
import app.skillsoft.assessmentbackend.domain.dto.simulation.InventoryWarning.WarningLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * ThreadLocal-based warning collector for question selection diagnostics.
 *
 * Allows {@link QuestionSelectionServiceImpl} to emit warnings without
 * changing its {@code List<UUID>} return signatures. Callers (assemblers)
 * open a collection context before calling selection methods, then drain
 * accumulated warnings into their {@link app.skillsoft.assessmentbackend.services.assembly.AssemblyResult}.
 *
 * Follows the same lifecycle pattern as {@code QuestionSelectionServiceImpl.SEEDED_RANDOM}:
 * caller sets context, service uses it, caller clears context.
 */
public final class SelectionWarningCollector {

    private static final ThreadLocal<List<InventoryWarning>> WARNINGS = new ThreadLocal<>();

    private SelectionWarningCollector() {}

    /** Begin collecting warnings for the current thread. */
    public static void begin() {
        WARNINGS.set(new ArrayList<>());
    }

    /** Check whether a collector is active on this thread. */
    public static boolean isActive() {
        return WARNINGS.get() != null;
    }

    /**
     * Add a warning. No-op if no collector is active (safe for
     * non-assembly callers like TestSessionServiceImpl).
     */
    public static void addWarning(WarningLevel level, String message) {
        List<InventoryWarning> list = WARNINGS.get();
        if (list != null) {
            list.add(InventoryWarning.assemblyWarning(level, message));
        }
    }

    /** Return all collected warnings and clear thread-local state. */
    public static List<InventoryWarning> drain() {
        List<InventoryWarning> list = WARNINGS.get();
        WARNINGS.remove();
        return list != null ? list : List.of();
    }

    /** Clear thread-local state without returning warnings. */
    public static void clear() {
        WARNINGS.remove();
    }
}
