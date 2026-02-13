package app.skillsoft.assessmentbackend.services.scoring;

import app.skillsoft.assessmentbackend.domain.entities.AssessmentQuestion;
import app.skillsoft.assessmentbackend.domain.entities.QuestionType;
import app.skillsoft.assessmentbackend.domain.entities.TestAnswer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Shared component for normalizing raw answer scores to a 0-1 scale.
 *
 * Extracted from scoring strategies to fix DRY violation (Task 2.1).
 * Handles different question types with consistent normalization logic:
 * - Likert/LIKERT_SCALE/FREQUENCY_SCALE (1-5): (value - 1) / 4
 * - SJT/SITUATIONAL_JUDGMENT: pre-calculated score (0-1)
 * - MCQ/MULTIPLE_CHOICE: score field (0 or 1)
 * - CAPABILITY_ASSESSMENT/PEER_FEEDBACK: Likert-like or score
 * - Text-based (BEHAVIORAL_EXAMPLE, OPEN_TEXT, SELF_REFLECTION): score field
 *
 * Per ROADMAP.md Section 2.1 - Extract shared ScoreNormalizer component
 */
@Component
public class ScoreNormalizer {

    private static final Logger log = LoggerFactory.getLogger(ScoreNormalizer.class);

    /**
     * Normalize raw answer score to 0-1 scale.
     *
     * @param answer The test answer to normalize
     * @return Normalized score (0-1), or 0.0 if no score available
     */
    public double normalize(TestAnswer answer) {
        if (answer == null || Boolean.TRUE.equals(answer.getIsSkipped())) {
            return 0.0;
        }

        QuestionType questionType = getQuestionTypeSafe(answer);
        if (questionType == null) {
            log.warn("Cannot determine question type for answer {}", answer.getId());
            return getScoreFallback(answer);
        }

        return switch (questionType) {
            case LIKERT, LIKERT_SCALE, FREQUENCY_SCALE -> normalizeLikert(answer);
            case SJT, SITUATIONAL_JUDGMENT -> normalizeSjt(answer);
            case MCQ, MULTIPLE_CHOICE -> normalizeMcq(answer);
            case CAPABILITY_ASSESSMENT, PEER_FEEDBACK -> normalizeCapability(answer);
            case BEHAVIORAL_EXAMPLE, OPEN_TEXT, SELF_REFLECTION -> normalizeTextBased(answer);
        };
    }

    /**
     * Safely extract question type from answer, handling null cases.
     *
     * @param answer The test answer
     * @return QuestionType or null if not available
     */
    private QuestionType getQuestionTypeSafe(TestAnswer answer) {
        return Optional.ofNullable(answer)
            .map(TestAnswer::getQuestion)
            .map(AssessmentQuestion::getQuestionType)
            .orElse(null);
    }

    /**
     * Normalize Likert scale answers (1-5 -> 0-1).
     * Formula: (value - 1) / 4
     *
     * @param answer The test answer with likertValue
     * @return Normalized score (0-1)
     */
    private double normalizeLikert(TestAnswer answer) {
        if (answer.getLikertValue() == null) {
            log.debug("Likert answer {} has no likertValue, defaulting to 0", answer.getId());
            return 0.0;
        }
        int value = Math.max(1, Math.min(5, answer.getLikertValue()));
        return (value - 1.0) / 4.0;
    }

    /**
     * Normalize SJT answers (already 0-1 in score field).
     * Ensures score is clamped to valid range.
     *
     * @param answer The test answer with score
     * @return Normalized score (0-1)
     */
    private double normalizeSjt(TestAnswer answer) {
        if (answer.getScore() != null) {
            return Math.max(0.0, Math.min(1.0, answer.getScore()));
        }
        log.debug("SJT answer {} has no score, defaulting to 0", answer.getId());
        return 0.0;
    }

    /**
     * Normalize MCQ answers (correct=1, incorrect=0).
     * Uses score field populated by answer submission logic.
     *
     * @param answer The test answer with score
     * @return Normalized score (0 or 1)
     */
    private double normalizeMcq(TestAnswer answer) {
        return answer.getScore() != null ? answer.getScore() : 0.0;
    }

    /**
     * Normalize capability assessment and peer feedback answers.
     * Tries likertValue first (Likert-like), then falls back to score.
     *
     * @param answer The test answer
     * @return Normalized score (0-1)
     */
    private double normalizeCapability(TestAnswer answer) {
        if (answer.getLikertValue() != null) {
            int likert = Math.max(1, Math.min(5, answer.getLikertValue()));
            return (likert - 1.0) / 4.0;
        }
        if (answer.getScore() != null) {
            return Math.max(0.0, Math.min(1.0, answer.getScore()));
        }
        log.debug("Capability answer {} has no likertValue or score, defaulting to 0", answer.getId());
        return 0.0;
    }

    /**
     * Normalize text-based answers (requires manual scoring).
     * Uses score field if available.
     *
     * @param answer The test answer with score (if graded)
     * @return Normalized score (0-1), or 0.0 if not yet graded
     */
    private double normalizeTextBased(TestAnswer answer) {
        if (answer.getScore() != null) {
            return Math.max(0.0, Math.min(1.0, answer.getScore()));
        }
        log.debug("Text-based answer {} has no score (not yet graded), defaulting to 0", answer.getId());
        return 0.0;
    }

    /**
     * Fallback scoring for unknown question types or missing type information.
     * Attempts to use score field if available.
     *
     * @param answer The test answer
     * @return Normalized score (0-1), or 0.0 if no score
     */
    private double getScoreFallback(TestAnswer answer) {
        if (answer != null && answer.getScore() != null) {
            return Math.max(0.0, Math.min(1.0, answer.getScore()));
        }
        log.warn("No score available for answer {}, defaulting to 0",
            answer != null ? answer.getId() : "null");
        return 0.0;
    }
}
