package app.skillsoft.assessmentbackend.domain.dto.simulation;

/**
 * Machine-readable codes for assembly/inventory warnings.
 * Used by the frontend to display contextual help tooltips.
 */
public enum WarningCode {
    BENCHMARK_LOOKUP_FALLBACK,
    INDICATOR_EXHAUSTED_BORROWING,
    FUZZY_MATCH_PASSPORT,
    FUZZY_MATCH_ONET,
    NO_INDICATORS_FOR_GAPS,
    NO_ACTIVE_QUESTIONS_INDICATOR,
    NO_ACTIVE_INDICATORS_COMPETENCY,
    NO_ACTIVE_INDICATORS_COMPETENCIES,
    NO_ONET_SOC_CODE,
    NO_ONET_PROFILE,
    INVENTORY_CRITICAL,
    INVENTORY_LIMITED,
    GENERIC
}
