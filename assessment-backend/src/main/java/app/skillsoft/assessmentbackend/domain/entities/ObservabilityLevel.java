package app.skillsoft.assessmentbackend.domain.entities;

public enum ObservabilityLevel {
    DIRECTLY_OBSERVABLE("Напрямую наблюдаемо", "Четко видимое поведение"),
    PARTIALLY_OBSERVABLE("Частично наблюдаемо", "Некоторые аспекты видимы"),
    INFERRED("Выводимо", "Выводится из множественных поведений"),
    SELF_REPORTED("Самоотчет", "Основано на личном раскрытии"),
    REQUIRES_DOCUMENTATION("Требует документирования", "Нужны доказательства");

    ObservabilityLevel(String s, String s1) {
    }
}