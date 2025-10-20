package app.skillsoft.assessmentbackend.services;


import app.skillsoft.assessmentbackend.domain.entities.AssessmentQuestion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssessmentQuestionService {
    List<AssessmentQuestion> listAssessmentQuestions(UUID behavioralIndicatorId);

    AssessmentQuestion createAssesmentQuestion(UUID behavioralIndicatorId, AssessmentQuestion assesmentQuestion);

    Optional<AssessmentQuestion> findAssesmentQuestionById(UUID behavioralIndicatorId, UUID assesmentQuestionId);

    AssessmentQuestion updateAssesmentQuestion(UUID behavioralIndicatorId, UUID assesmentQuestionId, AssessmentQuestion assesmentQuestion);

    void deleteAssesmentQuestion(UUID behavioralIndicatorId, UUID assesmentQuestionId);
}
