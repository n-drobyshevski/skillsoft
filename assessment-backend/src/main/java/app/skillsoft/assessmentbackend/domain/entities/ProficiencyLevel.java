package app.skillsoft.assessmentbackend.domain.entities;

public enum ProficiencyLevel {
    NOVICE("Новичок", "Базовое понимание, требует supervision"),
    DEVELOPING("Развивающийся", "Растущая компетентность, иногда нужна помощь"),
    PROFICIENT("Компетентный", "Независимая работа, стабильные результаты"),
    ADVANCED("Продвинутый", "Экспертные знания, может обучать других"),
    EXPERT("Эксперт", "Лидер мнений, создает инновации в области");

    ProficiencyLevel(String компетентный, String s) {
    }
}