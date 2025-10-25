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
    public List<AssessmentQuestion> listAllQuestions() {
        return this.assessmentQuestionRepository.findAll();
    }

    @Override
    public List<AssessmentQuestion> listAssessmentQuestions(UUID behavioralIndicatorId) {
        return assessmentQuestionRepository.findByBehavioralIndicator_Id(behavioralIndicatorId);
    }

    @Override
    public AssessmentQuestion createAssesmentQuestion(UUID behavioralIndicatorId, AssessmentQuestion assessmentQuestion) {
        assessmentQuestion.setBehavioralIndicatorId(behavioralIndicatorId);
        return assessmentQuestionRepository.save(assessmentQuestion);
    }

    @Override
    public Optional<AssessmentQuestion> findAssesmentQuestionById(UUID behavioralIndicatorId, UUID assessmentQuestionId) {
        Optional<AssessmentQuestion> question = assessmentQuestionRepository.findById(assessmentQuestionId);
        return question.filter(q -> q.getBehavioralIndicatorId().equals(behavioralIndicatorId));
    }

    @Override
    public AssessmentQuestion updateAssesmentQuestion(UUID behavioralIndicatorId, UUID assessmentQuestionId,
            AssessmentQuestion assessmentQuestion) {
        return findAssesmentQuestionById(behavioralIndicatorId, assessmentQuestionId)
            .map(existingQuestion -> {
                existingQuestion.setQuestionText(assessmentQuestion.getQuestionText());
                existingQuestion.setQuestionType(assessmentQuestion.getQuestionType());
                existingQuestion.setAnswerOptions(assessmentQuestion.getAnswerOptions());
                existingQuestion.setScoringRubric(assessmentQuestion.getScoringRubric());
                existingQuestion.setTimeLimit(assessmentQuestion.getTimeLimit());
                existingQuestion.setDifficultyLevel(assessmentQuestion.getDifficultyLevel());
                existingQuestion.setActive(assessmentQuestion.isActive());
                existingQuestion.setOrderIndex(assessmentQuestion.getOrderIndex());
                
                return assessmentQuestionRepository.save(existingQuestion);
            })
            .orElse(null);
    }

    @Override
    public void deleteAssesmentQuestion(UUID behavioralIndicatorId, UUID assessmentQuestionId) {
        findAssesmentQuestionById(behavioralIndicatorId, assessmentQuestionId)
            .ifPresent(assessmentQuestionRepository::delete);
    }
}
