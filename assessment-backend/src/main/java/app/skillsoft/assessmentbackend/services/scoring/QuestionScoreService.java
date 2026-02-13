package app.skillsoft.assessmentbackend.services.scoring;

import app.skillsoft.assessmentbackend.domain.dto.QuestionScoreDto;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentQuestion;
import app.skillsoft.assessmentbackend.domain.entities.QuestionType;
import app.skillsoft.assessmentbackend.domain.entities.TestAnswer;
import app.skillsoft.assessmentbackend.domain.entities.TestResult;
import app.skillsoft.assessmentbackend.repository.TestAnswerRepository;
import app.skillsoft.assessmentbackend.repository.TestResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for loading question-level score details on demand.
 * Implements lazy-loading pattern for question details within an indicator.
 *
 * This service supports the indicator breakdown feature by providing
 * detailed question scores when a user expands an indicator row in the
 * results view. Includes correct answers for learning/review purposes.
 *
 * Per ROADMAP.md: Lazy-load question details to minimize initial payload size.
 */
@Service
@Transactional(readOnly = true)
public class QuestionScoreService {

    private static final Logger log = LoggerFactory.getLogger(QuestionScoreService.class);

    private final TestResultRepository testResultRepository;
    private final TestAnswerRepository testAnswerRepository;
    private final ScoreNormalizer scoreNormalizer;

    public QuestionScoreService(
            TestResultRepository testResultRepository,
            TestAnswerRepository testAnswerRepository,
            ScoreNormalizer scoreNormalizer) {
        this.testResultRepository = testResultRepository;
        this.testAnswerRepository = testAnswerRepository;
        this.scoreNormalizer = scoreNormalizer;
    }

    /**
     * Get question-level scores for a specific indicator within a result.
     * Includes correct answers for learning and review purposes.
     *
     * @param resultId The test result UUID
     * @param indicatorId The behavioral indicator UUID
     * @return List of QuestionScoreDto for all questions in the indicator
     * @throws IllegalArgumentException if result not found
     */
    public List<QuestionScoreDto> getQuestionScoresForIndicator(UUID resultId, UUID indicatorId) {
        log.info("Loading question scores for result {} indicator {}", resultId, indicatorId);

        // Find the result to get the session
        TestResult result = testResultRepository.findByIdWithSessionAndTemplate(resultId)
                .orElseThrow(() -> {
                    log.warn("Result not found: {}", resultId);
                    return new IllegalArgumentException("Result not found: " + resultId);
                });

        UUID sessionId = result.getSession().getId();
        log.debug("Found session {} for result {}", sessionId, resultId);

        // Fetch answers for this indicator
        List<TestAnswer> answers = testAnswerRepository.findBySessionIdAndBehavioralIndicatorId(
                sessionId, indicatorId);

        if (answers.isEmpty()) {
            log.info("No answers found for indicator {} in session {}", indicatorId, sessionId);
            return List.of();
        }

        log.debug("Found {} answers for indicator {}", answers.size(), indicatorId);

        // Map answers to DTOs
        List<QuestionScoreDto> questionScores = answers.stream()
                .filter(a -> a.getQuestion() != null)
                .map(this::mapToQuestionScoreDto)
                .sorted(Comparator.comparing(QuestionScoreDto::getQuestionId))
                .collect(Collectors.toList());

        log.info("Mapped {} question scores for indicator {}", questionScores.size(), indicatorId);
        return questionScores;
    }

    /**
     * Map a TestAnswer to QuestionScoreDto.
     * Includes correct answer extraction for learning purposes.
     *
     * @param answer The test answer to map
     * @return QuestionScoreDto with score details and correct answer
     */
    private QuestionScoreDto mapToQuestionScoreDto(TestAnswer answer) {
        AssessmentQuestion question = answer.getQuestion();

        // Calculate normalized score
        double normalizedScore = scoreNormalizer.normalize(answer);

        // Build the DTO
        QuestionScoreDto dto = new QuestionScoreDto(
                question.getId(),
                question.getQuestionText(),
                question.getQuestionType().name(),
                normalizedScore,
                1.0  // Max score is always 1.0 for normalized scores
        );

        // Add user answer
        String userAnswer = extractUserAnswer(answer);
        dto.setUserAnswer(userAnswer);

        // Add correct answer for learning/review
        String correctAnswer = extractCorrectAnswer(question);
        dto.setCorrectAnswer(correctAnswer);

        // Add time spent
        dto.setTimeSpentSeconds(answer.getTimeSpentSeconds());

        return dto;
    }

    /**
     * Extract the user's answer as a human-readable string.
     *
     * @param answer The test answer
     * @return String representation of the user's answer
     */
    private String extractUserAnswer(TestAnswer answer) {
        if (answer.getIsSkipped() != null && answer.getIsSkipped()) {
            return "Skipped";
        }

        QuestionType questionType = answer.getQuestion().getQuestionType();
        AssessmentQuestion question = answer.getQuestion();

        return switch (questionType) {
            case LIKERT, LIKERT_SCALE, FREQUENCY_SCALE -> {
                Integer likertValue = answer.getLikertValue();
                yield likertValue != null ? likertValue.toString() : "Not answered";
            }
            case MCQ, MULTIPLE_CHOICE -> {
                List<String> selectedIds = answer.getSelectedOptionIds();
                if (selectedIds == null || selectedIds.isEmpty()) {
                    yield "Not answered";
                }
                // Try to find option text from question
                yield formatSelectedOptions(question, selectedIds);
            }
            case SJT, SITUATIONAL_JUDGMENT -> {
                List<String> selectedIds = answer.getSelectedOptionIds();
                if (selectedIds == null || selectedIds.isEmpty()) {
                    yield "Not answered";
                }
                yield formatSelectedOptions(question, selectedIds);
            }
            case CAPABILITY_ASSESSMENT, PEER_FEEDBACK -> {
                if (answer.getLikertValue() != null) {
                    yield answer.getLikertValue().toString();
                }
                if (answer.getSelectedOptionIds() != null && !answer.getSelectedOptionIds().isEmpty()) {
                    yield formatSelectedOptions(question, answer.getSelectedOptionIds());
                }
                yield "Not answered";
            }
            case BEHAVIORAL_EXAMPLE, OPEN_TEXT, SELF_REFLECTION -> {
                String textResponse = answer.getTextResponse();
                yield textResponse != null ? textResponse : "Not answered";
            }
        };
    }

    /**
     * Format selected option IDs to human-readable text.
     * Looks up option text from the question's answer options.
     *
     * @param question The assessment question
     * @param selectedIds List of selected option IDs
     * @return Formatted string of selected options
     */
    private String formatSelectedOptions(AssessmentQuestion question, List<String> selectedIds) {
        List<Map<String, Object>> options = question.getAnswerOptions();
        if (options == null || options.isEmpty()) {
            return String.join(", ", selectedIds);
        }

        // Build ID to text map
        Map<String, String> optionTextMap = new HashMap<>();
        for (Map<String, Object> option : options) {
            Object id = option.get("id");
            Object text = option.get("text");
            if (id != null && text != null) {
                optionTextMap.put(id.toString(), text.toString());
            }
        }

        // Map selected IDs to text
        return selectedIds.stream()
                .map(id -> optionTextMap.getOrDefault(id, id))
                .collect(Collectors.joining(", "));
    }

    /**
     * Extract the correct answer for a question.
     * Logic varies by question type:
     * - Likert: "5" (highest value)
     * - MCQ: Option(s) marked as isCorrect
     * - SJT: Option with highest weight
     * - Text-based: From scoring rubric
     *
     * @param question The assessment question
     * @return String representation of the correct answer
     */
    private String extractCorrectAnswer(AssessmentQuestion question) {
        QuestionType questionType = question.getQuestionType();
        List<Map<String, Object>> options = question.getAnswerOptions();

        return switch (questionType) {
            case LIKERT, LIKERT_SCALE, FREQUENCY_SCALE -> {
                // For Likert scales, highest value is typically "best"
                yield "5 (Highest)";
            }
            case MCQ, MULTIPLE_CHOICE -> {
                if (options == null || options.isEmpty()) {
                    yield "Not available";
                }
                // Find options marked as correct
                List<String> correctOptions = options.stream()
                        .filter(opt -> Boolean.TRUE.equals(opt.get("isCorrect")))
                        .map(opt -> {
                            Object text = opt.get("text");
                            return text != null ? text.toString() : "Option";
                        })
                        .collect(Collectors.toList());
                yield correctOptions.isEmpty() ? "Not available" : String.join(", ", correctOptions);
            }
            case SJT, SITUATIONAL_JUDGMENT -> {
                if (options == null || options.isEmpty()) {
                    yield "Not available";
                }
                // Find option with highest weight
                Optional<Map<String, Object>> bestOption = options.stream()
                        .filter(opt -> opt.get("weight") != null)
                        .max(Comparator.comparing(opt -> {
                            Object weight = opt.get("weight");
                            if (weight instanceof Number) {
                                return ((Number) weight).doubleValue();
                            }
                            return 0.0;
                        }));
                yield bestOption
                        .map(opt -> {
                            Object text = opt.get("text");
                            return text != null ? text.toString() : "Best option";
                        })
                        .orElse("Not available");
            }
            case CAPABILITY_ASSESSMENT, PEER_FEEDBACK -> {
                // Similar to Likert for rating-based
                if (options != null && !options.isEmpty()) {
                    Optional<Map<String, Object>> correctOpt = options.stream()
                            .filter(opt -> Boolean.TRUE.equals(opt.get("isCorrect")))
                            .findFirst();
                    if (correctOpt.isPresent()) {
                        Object text = correctOpt.get().get("text");
                        yield text != null ? text.toString() : "5 (Highest)";
                    }
                }
                yield "5 (Highest)";
            }
            case BEHAVIORAL_EXAMPLE, OPEN_TEXT, SELF_REFLECTION -> {
                // For text-based questions, refer to scoring rubric
                String rubric = question.getScoringRubric();
                yield rubric != null ? "See rubric: " + rubric : "Qualitative assessment";
            }
        };
    }

    /**
     * Check if a result exists and has questions for the specified indicator.
     * Used for validation before loading full question details.
     *
     * @param resultId The test result UUID
     * @param indicatorId The behavioral indicator UUID
     * @return true if questions exist for this indicator
     */
    public boolean hasQuestionsForIndicator(UUID resultId, UUID indicatorId) {
        Optional<TestResult> resultOpt = testResultRepository.findByIdWithSessionAndTemplate(resultId);
        if (resultOpt.isEmpty()) {
            return false;
        }

        UUID sessionId = resultOpt.get().getSession().getId();
        List<TestAnswer> answers = testAnswerRepository.findBySessionIdAndBehavioralIndicatorId(
                sessionId, indicatorId);

        return !answers.isEmpty();
    }
}
