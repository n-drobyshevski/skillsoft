package app.skillsoft.assessmentbackend.domain.entities;

public enum DifficultyLevel {
    FOUNDATIONAL("Базовый", "Оценка начального уровня"),
    INTERMEDIATE("Средний", "Средняя сложность"),
    ADVANCED("Продвинутый", "Высокая сложность"),
    EXPERT("Экспертный", "Лидерский/стратегический уровень"),
    SPECIALIZED("Специализированный", "Специфичные для роли требования");

    DifficultyLevel(String name, String desc) {
    }
}