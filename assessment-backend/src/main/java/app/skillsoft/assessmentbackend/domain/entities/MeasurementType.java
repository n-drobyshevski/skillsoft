package app.skillsoft.assessmentbackend.domain.entities;

public enum MeasurementType {
    LIKERT_SCALE("Шкала Лайкерта", "Оценка от 1 до 5 или 1 до 7"),
    FREQUENCY_SCALE("Шкала частоты", "От 'Никогда' до 'Всегда'"),
    PROFICIENCY_RATING("Оценка компетентности", "От базового до экспертного"),
    BINARY("Бинарная оценка", "Да/Нет"),
    RUBRIC_BASED("На основе рубрик", "Определенные критерии уровней"),
    PERCENTILE_RANK("Процентильный ранг", "Сравнительная оценка");

    MeasurementType(String s, String s1) {
    }
}