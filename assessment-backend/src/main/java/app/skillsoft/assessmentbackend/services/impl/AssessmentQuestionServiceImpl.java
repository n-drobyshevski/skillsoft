package app.skillsoft.assessmentbackend.services.impl;

import app.skillsoft.assessmentbackend.domain.entities.AssessmentQuestion;
import app.skillsoft.assessmentbackend.domain.entities.BehavioralIndicator;
import app.skillsoft.assessmentbackend.repository.AssessmentQuestionRepository;
import app.skillsoft.assessmentbackend.repository.BehavioralIndicatorRepository;
import app.skillsoft.assessmentbackend.services.AssessmentQuestionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AssessmentQuestionServiceImpl implements AssessmentQuestionService {

    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final BehavioralIndicatorRepository behavioralIndicatorRepository;

    public AssessmentQuestionServiceImpl(AssessmentQuestionRepository assessmentQuestionRepository,
                                        BehavioralIndicatorRepository behavioralIndicatorRepository) {
        this.assessmentQuestionRepository = assessmentQuestionRepository;
        this.behavioralIndicatorRepository = behavioralIndicatorRepository;
    }

    @Override
    public List<AssessmentQuestion> listAllQuestions() {
        return this.assessmentQuestionRepository.findAll();
    }

    @Override
    public List<AssessmentQuestion> listIndicatorAssessmentQuestions(UUID behavioralIndicatorId) {
        return assessmentQuestionRepository.findByBehavioralIndicator_Id(behavioralIndicatorId);
    }

    @Override
    @Transactional
    public AssessmentQuestion createAssesmentQuestion(UUID behavioralIndicatorId, AssessmentQuestion assessmentQuestion) {
        // Fetch the actual BehavioralIndicator entity within transaction
        BehavioralIndicator behavioralIndicator = behavioralIndicatorRepository.findById(behavioralIndicatorId)
                .orElseThrow(() -> new RuntimeException("Behavioral indicator not found with id: " + behavioralIndicatorId));
        
        // Set the managed entity relationship
        assessmentQuestion.setBehavioralIndicator(behavioralIndicator);
        
        return assessmentQuestionRepository.save(assessmentQuestion);
    }

    @Override
    public Optional<AssessmentQuestion> findAssesmentQuestionById(UUID assessmentQuestionId) {
        return assessmentQuestionRepository.findById(assessmentQuestionId);
    }

    @Override
    public AssessmentQuestion updateAssesmentQuestion( UUID assessmentQuestionId,
            AssessmentQuestion assessmentQuestion) {
        return findAssesmentQuestionById( assessmentQuestionId)
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
    public void deleteAssesmentQuestion( UUID assessmentQuestionId) {
        findAssesmentQuestionById(assessmentQuestionId)
            .ifPresent(assessmentQuestionRepository::delete);
    }
}
