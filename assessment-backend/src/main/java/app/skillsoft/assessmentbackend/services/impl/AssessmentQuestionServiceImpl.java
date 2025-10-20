package app.skillsoft.assessmentbackend.services.impl;

import app.skillsoft.assessmentbackend.domain.entities.AssessmentQuestion;
import app.skillsoft.assessmentbackend.repository.AssessmentQuestionRepository;
import app.skillsoft.assessmentbackend.services.AssessmentQuestionService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
public class AssessmentQuestionServiceImpl implements AssessmentQuestionService {

    private final AssessmentQuestionRepository assessmentQuestionRepository;

    public AssessmentQuestionServiceImpl(AssessmentQuestionRepository assessmentQuestionRepository) {
        this.assessmentQuestionRepository = assessmentQuestionRepository;
    }


    @Override
    public List<AssessmentQuestion> listAssessmentQuestions(UUID behavioralIndicatorId) {
        return assessmentQuestionRepository.findByBehavioralIndicatorId(behavioralIndicatorId);
    }

    @Override
    public AssessmentQuestion createAssesmentQuestion(UUID behavioralIndicatorId, AssessmentQuestion assesmentQuestion) {
        return null;
    }

    @Override
    public Optional<AssessmentQuestion> findAssesmentQuestionById(UUID behavioralIndicatorId, UUID assesmentQuestionId) {
        return Optional.empty();
    }

    @Override
    public AssessmentQuestion updateAssesmentQuestion(UUID behavioralIndicatorId, UUID assesmentQuestionId, AssessmentQuestion assesmentQuestion) {
        return null;
    }

    @Override
    public void deleteAssesmentQuestion(UUID behavioralIndicatorId, UUID assesmentQuestionId) {

    }
}
