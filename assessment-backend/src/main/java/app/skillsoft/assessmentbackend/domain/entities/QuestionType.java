package app.skillsoft.assessmentbackend.domain.entities;

/**
 * Question types supporting different assessment methodologies.
 * Based on ROADMAP.md Section 1.B "Polymorphism of Question Types"
 * 
 * Primary Types (Smart Assessment):
 * - LIKERT: Scale questions (1-5) for measuring agreement/frequency
 * - SJT: Situational Judgment Tests with vector weights for ipsative scoring
 * - MCQ: Multiple Choice Questions with correct answer(s)
 * 
 * Extended Types (Legacy/Future):
 * - BEHAVIORAL_EXAMPLE, OPEN_TEXT, etc.
 */
public enum QuestionType {
    // Primary types per ROADMAP.md
    LIKERT("Likert Scale", "Measures degree of agreement on 1-5 scale"),
    SJT("Situational Judgment Test", "Scenario-based with weighted answer options"),
    MCQ("Multiple Choice Question", "Select correct answer(s) from options"),
    
    // Extended types (backwards compatibility)
    LIKERT_SCALE("Шкала Лайкерта", "Измерение степени согласия"),
    SITUATIONAL_JUDGMENT("Ситуационная оценка", "Оценка поведения в сценариях"),
    BEHAVIORAL_EXAMPLE("Поведенческий пример", "Описание прошлого опыта"),
    MULTIPLE_CHOICE("Множественный выбор", "Выбор из предложенных вариантов"),
    CAPABILITY_ASSESSMENT("Оценка способностей", "Демонстрация навыков"),
    SELF_REFLECTION("Самоанализ", "Личное понимание"),
    PEER_FEEDBACK("Обратная связь коллег", "Оценка от коллег"),
    FREQUENCY_SCALE("Шкала частоты", "Как часто проявляется поведение"),
    OPEN_TEXT("Открытый вопрос", "Свободный ответ");

    private final String displayName;
    private final String description;

    QuestionType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if this is a primary Smart Assessment type.
     * These types support advanced scoring (vector weights, ipsative measurement).
     */
    public boolean isPrimaryType() {
        return this == LIKERT || this == SJT || this == MCQ;
    }

    /**
     * Check if this type supports vector weights in answer options.
     * SJT questions can have weights like: {"Leadership": 0.8, "Empathy": -1.0}
     */
    public boolean supportsVectorWeights() {
        return this == SJT || this == SITUATIONAL_JUDGMENT;
    }
}