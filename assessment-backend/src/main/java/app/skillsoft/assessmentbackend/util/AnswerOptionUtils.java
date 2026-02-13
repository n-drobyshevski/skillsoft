package app.skillsoft.assessmentbackend.util;

import app.skillsoft.assessmentbackend.domain.entities.QuestionType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for working with answer options in JSON format
 * Helps create properly structured answer options for different question types
 */
public class AnswerOptionUtils {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Creates answer options for a multiple choice question
     * @param options List of options
     * @param correctIndices Indices of the correct answers (for multiple correct answers)
     * @return List of answer option maps
     */
    public static List<Map<String, Object>> createMultipleChoiceOptionsFromIndices(List<String> options, List<Integer> correctIndices) {
        List<Map<String, Object>> answerOptions = new ArrayList<>();
        
        for (int i = 0; i < options.size(); i++) {
            Map<String, Object> option = new HashMap<>();
            option.put("text", options.get(i));
            option.put("correct", correctIndices.contains(i));
            answerOptions.add(option);
        }
        
        return answerOptions;
    }
    
    /**
     * Creates answer options for a multiple choice question with boolean flags for correct answers
     * @param options List of options
     * @param correctFlags List of boolean values indicating if each option is correct
     * @return List of answer option maps
     */
    public static List<Map<String, Object>> createMultipleChoiceOptions(List<String> options, List<Boolean> correctFlags) {
        List<Map<String, Object>> answerOptions = new ArrayList<>();
        
        for (int i = 0; i < options.size(); i++) {
            Map<String, Object> option = new HashMap<>();
            option.put("text", options.get(i));
            option.put("correct", correctFlags.get(i));
            answerOptions.add(option);
        }
        
        return answerOptions;
    }
    
    /**
     * Creates answer options for a Likert scale question
     * @param labels List of labels for the scale points
     * @param values List of values for the scale points
     * @return List of answer option maps
     */
    public static List<Map<String, Object>> createLikertScaleOptions(List<String> labels, List<Integer> values) {
        List<Map<String, Object>> answerOptions = new ArrayList<>();
        
        for (int i = 0; i < labels.size(); i++) {
            Map<String, Object> option = new HashMap<>();
            option.put("label", labels.get(i));
            option.put("value", values.get(i));
            answerOptions.add(option);
        }
        
        return answerOptions;
    }
    
    /**
     * Creates answer options for a situational judgment question
     * @param options List of response options
     * @param scores List of scores for each option
     * @param explanations List of explanations for each option's score
     * @return List of answer option maps
     */
    public static List<Map<String, Object>> createSituationalJudgmentOptions(
            List<String> options, List<Integer> scores, List<String> explanations) {
        List<Map<String, Object>> answerOptions = new ArrayList<>();
        
        for (int i = 0; i < options.size(); i++) {
            Map<String, Object> option = new HashMap<>();
            option.put("text", options.get(i));
            option.put("score", scores.get(i));
            option.put("explanation", explanations.get(i));
            answerOptions.add(option);
        }
        
        return answerOptions;
    }
    
    /**
     * Creates standard answer options based on question type
     * @param questionType The type of question
     * @return Default answer options for the question type
     */
    public static List<Map<String, Object>> createDefaultOptionsForType(QuestionType questionType) {
        switch (questionType) {
            // Likert scale types (1-5 agreement scale)
            case LIKERT:
            case LIKERT_SCALE:
                List<String> labels = List.of("Полностью не согласен", "Скорее не согласен",
                                             "Частично согласен", "Скорее согласен", "Полностью согласен");
                List<Integer> values = List.of(1, 2, 3, 4, 5);
                return createLikertScaleOptions(labels, values);

            // Multiple choice types
            case MCQ:
            case MULTIPLE_CHOICE:
                List<String> options = List.of("Вариант 1", "Вариант 2", "Вариант 3", "Вариант 4");
                return createMultipleChoiceOptionsFromIndices(options, List.of(0));

            // Frequency scale (how often behavior occurs)
            case FREQUENCY_SCALE:
                List<String> freqLabels = List.of("Никогда", "Редко", "Иногда", "Часто", "Всегда");
                List<Integer> freqValues = List.of(1, 2, 3, 4, 5);
                return createLikertScaleOptions(freqLabels, freqValues);

            // Situational Judgment Test types (A/B/C/D options with effectiveness scores)
            case SJT:
            case SITUATIONAL_JUDGMENT:
                List<String> sjtOptions = List.of(
                    "A) Первый вариант действия",
                    "B) Второй вариант действия",
                    "C) Третий вариант действия",
                    "D) Четвертый вариант действия"
                );
                List<Integer> sjtScores = List.of(3, 2, 1, 0); // Most to least effective
                List<String> sjtExplanations = List.of(
                    "Наиболее эффективный подход",
                    "Частично эффективный подход",
                    "Менее эффективный подход",
                    "Неэффективный подход"
                );
                return createSituationalJudgmentOptions(sjtOptions, sjtScores, sjtExplanations);

            // Capability assessment (similar to MCQ with skill demonstration)
            case CAPABILITY_ASSESSMENT:
                List<String> capOptions = List.of(
                    "Не владею",
                    "Базовый уровень",
                    "Средний уровень",
                    "Продвинутый уровень",
                    "Экспертный уровень"
                );
                List<Integer> capValues = List.of(1, 2, 3, 4, 5);
                return createLikertScaleOptions(capOptions, capValues);

            // Peer feedback (similar to Likert for rating colleagues)
            case PEER_FEEDBACK:
                List<String> peerLabels = List.of(
                    "Значительно ниже ожиданий",
                    "Ниже ожиданий",
                    "Соответствует ожиданиям",
                    "Выше ожиданий",
                    "Значительно выше ожиданий"
                );
                List<Integer> peerValues = List.of(1, 2, 3, 4, 5);
                return createLikertScaleOptions(peerLabels, peerValues);

            // Text-based question types (no predefined options)
            case BEHAVIORAL_EXAMPLE:
            case OPEN_TEXT:
            case SELF_REFLECTION:
                // These types don't have predefined options - return empty list
                // The actual response is free-form text
                return new ArrayList<>();

            default:
                return new ArrayList<>();
        }
    }
    
    /**
     * Converts a list of answer options to a JSON string
     * @param options The answer options to convert
     * @return JSON string representation
     */
    public static String toJsonString(List<Map<String, Object>> options) {
        try {
            return objectMapper.writeValueAsString(options);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting answer options to JSON", e);
        }
    }
    
    /**
     * Parse JSON string into answer options list
     * @param jsonString The JSON string to parse
     * @return List of answer option maps
     */
    public static List<Map<String, Object>> fromJsonString(String jsonString) {
        try {
            return objectMapper.readValue(jsonString, 
                objectMapper.getTypeFactory().constructCollectionType(List.class, 
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class)));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error parsing JSON answer options", e);
        }
    }
}