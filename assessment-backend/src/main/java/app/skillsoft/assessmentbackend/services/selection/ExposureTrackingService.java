package app.skillsoft.assessmentbackend.services.selection;

import app.skillsoft.assessmentbackend.domain.entities.AssessmentQuestion;
import app.skillsoft.assessmentbackend.repository.AssessmentQuestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service responsible for tracking question exposure counts.
 *
 * Extracted from QuestionSelectionServiceImpl to resolve a self-invocation issue
 * where {@code this.trackExposure()} bypassed Spring's {@code @Transactional} proxy.
 * The parent class is {@code @Transactional(readOnly = true)}, so calling
 * {@code this.trackExposure()} (which needs write access) would silently execute
 * within the read-only transaction context.
 *
 * By extracting to a separate Spring bean, the {@code @Transactional} annotation
 * on {@link #trackExposure(List)} is properly intercepted by the AOP proxy,
 * ensuring a read-write transaction is opened for exposure count increments.
 *
 * @see QuestionSelectionServiceImpl
 */
@Service
@Transactional
public class ExposureTrackingService {

    private static final Logger log = LoggerFactory.getLogger(ExposureTrackingService.class);

    private final AssessmentQuestionRepository questionRepository;

    public ExposureTrackingService(AssessmentQuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    /**
     * Increment exposure count for selected questions.
     * Called after questions are finalized for a test assembly.
     *
     * Each question's exposure count is incremented by 1 and persisted.
     * This data feeds into the exposure control sorting in
     * {@link QuestionSelectionServiceImpl#applyDifficultyPreferenceWithExposureControl},
     * which deprioritizes overexposed items.
     *
     * @param questionIds The IDs of questions selected for the test
     */
    public void trackExposure(List<UUID> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return;
        }

        List<AssessmentQuestion> questions = questionRepository.findAllById(questionIds);
        for (AssessmentQuestion q : questions) {
            q.incrementExposureCount();
        }
        questionRepository.saveAll(questions);

        log.info("Incremented exposure count for {} questions (requested: {})",
                questions.size(), questionIds.size());
    }
}
