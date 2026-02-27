package app.skillsoft.assessmentbackend.services.selection;

import app.skillsoft.assessmentbackend.domain.dto.simulation.InventoryWarning;
import app.skillsoft.assessmentbackend.domain.dto.simulation.InventoryWarning.WarningLevel;
import app.skillsoft.assessmentbackend.domain.dto.simulation.WarningCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * Add a warning with code and i18n params. No-op if no collector is active (safe for
     * non-assembly callers like TestSessionServiceImpl).
     */
    public static void addWarning(WarningLevel level, WarningCode code, String message, Map<String, String> params) {
        List<InventoryWarning> list = WARNINGS.get();
        if (list != null) {
            list.add(InventoryWarning.assemblyWarning(level, code, message, params));
        }
    }

    /**
     * Add a warning with a specific code (no params).
     */
    public static void addWarning(WarningLevel level, WarningCode code, String message) {
        addWarning(level, code, message, null);
    }

    /**
     * Add a warning with GENERIC code. No-op if no collector is active.
     */
    public static void addWarning(WarningLevel level, String message) {
        addWarning(level, WarningCode.GENERIC, message, null);
    }

    /** Return all collected warnings (aggregated) and clear thread-local state. */
    public static List<InventoryWarning> drain() {
        List<InventoryWarning> list = WARNINGS.get();
        WARNINGS.remove();
        return list != null ? aggregate(list) : List.of();
    }

    /**
     * Merge per-indicator INDICATOR_EXHAUSTED_BORROWING warnings into
     * one warning per competency, summing the borrowed question counts.
     */
    private static List<InventoryWarning> aggregate(List<InventoryWarning> raw) {
        List<InventoryWarning> result = new ArrayList<>();
        // competencyName -> total borrowed count
        Map<String, Integer> borrowedByCompetency = new HashMap<>();

        for (InventoryWarning w : raw) {
            if (w.code() == WarningCode.INDICATOR_EXHAUSTED_BORROWING
                    && w.params() != null && w.params().containsKey("competencyName")) {
                String name = w.params().get("competencyName");
                int count = 0;
                try { count = Integer.parseInt(w.params().get("count")); } catch (NumberFormatException ignored) {}
                borrowedByCompetency.merge(name, count, Integer::sum);
            } else {
                result.add(w);
            }
        }

        for (var entry : borrowedByCompetency.entrySet()) {
            String name = entry.getKey();
            int total = entry.getValue();
            result.add(InventoryWarning.assemblyWarning(
                    WarningLevel.WARNING,
                    WarningCode.INDICATOR_EXHAUSTED_BORROWING,
                    String.format("Borrowing %d questions from sibling indicators of %s (flagged for psychometric review)",
                            total, name),
                    Map.of("count", String.valueOf(total), "competencyName", name)
            ));
        }

        return result;
    }

    /** Clear thread-local state without returning warnings. */
    public static void clear() {
        WARNINGS.remove();
    }
}
