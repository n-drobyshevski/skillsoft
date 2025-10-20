package app.skillsoft.assessmentbackend.domain.entities;

public enum QuestionType {
    LIKERT_SCALE("Шкала Лайкерта", "Измерение степени согласия"),
    SITUATIONAL_JUDGMENT("Ситуационная оценка", "Оценка поведения в сценариях"),
    BEHAVIORAL_EXAMPLE("Поведенческий пример", "Описание прошлого опыта"),
    MULTIPLE_CHOICE("Множественный выбор", "Выбор из предложенных вариантов"),
    CAPABILITY_ASSESSMENT("Оценка способностей", "Демонстрация навыков"),
    SELF_REFLECTION("Самоанализ", "Личное понимание"),
    PEER_FEEDBACK("Обратная связь коллег", "Оценка от коллег"),
    FREQUENCY_SCALE("Шкала частоты", "Как часто проявляется поведение");

    QuestionType(String name, String desc) {
    }
}