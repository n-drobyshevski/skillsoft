package app.skillsoft.assessmentbackend.domain.entities;

public enum IndicatorMeasurementType {
    FREQUENCY("Частота", "Как часто проявляется поведение"),
    QUALITY("Качество", "Насколько хорошо выполняется поведение"),
    IMPACT("Влияние", "Какое влияние оказывает поведение"),
    CONSISTENCY("Постоянство", "Насколько стабильно проявление"),
    IMPROVEMENT("Улучшение", "Динамика развития во времени");

    IndicatorMeasurementType(String улучшение, String s) {
    }
}
