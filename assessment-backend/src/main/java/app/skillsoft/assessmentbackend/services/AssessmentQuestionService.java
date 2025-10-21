package app.skillsoft.assessmentbackend.services;

import app.skillsoft.assessmentbackend.domain.entities.AssessmentQuestion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssessmentQuestionService {
    /**
     * List all assessment questions for a specific behavioral indicator
     */
    List<AssessmentQuestion> listAssessmentQuestions(UUID behavioralIndicatorId);

    /**
     * Create a new assessment question for a behavioral indicator
     */
    AssessmentQuestion createAssesmentQuestion(UUID behavioralIndicatorId, AssessmentQuestion assesmentQuestion);

    /**
     * Find an assessment question by ID within a specific behavioral indicator
     */
    Optional<AssessmentQuestion> findAssesmentQuestionById(UUID behavioralIndicatorId, UUID assesmentQuestionId);

    /**
     * Update an existing assessment question
     */
    AssessmentQuestion updateAssesmentQuestion(UUID behavioralIndicatorId, UUID assesmentQuestionId, AssessmentQuestion assesmentQuestion);

    /**
     * Delete an assessment question
     */
    void deleteAssesmentQuestion(UUID behavioralIndicatorId, UUID assesmentQuestionId);
}
