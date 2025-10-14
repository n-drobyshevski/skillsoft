package app.skillsoft.assessmentbackend.domain.entities;

public enum CompetencyCategory {
    COGNITIVE("Когнитивные способности", "Интеллектуальные процессы и аналитическое мышление"),
    INTERPERSONAL("Межличностные навыки", "Социальные навыки и взаимодействие с людьми"),
    LEADERSHIP("Лидерство", "Лидерские качества и управленческие навыки"),
    ADAPTABILITY("Адаптивность", "Гибкость и способность к изменениям"),
    EMOTIONAL_INTELLIGENCE("Эмоциональный интеллект", "Понимание и управление эмоциями"),
    COMMUNICATION("Коммуникация", "Устное и письменное общение"),
    COLLABORATION("Сотрудничество", "Командная работа и кооперация"),
    CRITICAL_THINKING("Критическое мышление", "Анализ, оценка и инновации"),
    TIME_MANAGEMENT("Управление временем", "Планирование и организация");

    CompetencyCategory(String s, String s1) {
    }
}