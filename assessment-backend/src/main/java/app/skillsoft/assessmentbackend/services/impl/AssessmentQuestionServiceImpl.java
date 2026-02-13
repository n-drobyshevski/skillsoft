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
        
        // Auto-assign orderIndex if not provided, invalid, or conflicts with existing
        List<AssessmentQuestion> existingQuestions = assessmentQuestionRepository.findByBehavioralIndicator_Id(behavioralIndicatorId);
        
        if (assessmentQuestion.getOrderIndex() <= 0) {
            // No orderIndex provided, auto-assign next available
            int maxOrder = existingQuestions.stream()
                    .mapToInt(AssessmentQuestion::getOrderIndex)
                    .max()
                    .orElse(0);
            assessmentQuestion.setOrderIndex(maxOrder + 1);
        } else {
            // Check if the provided orderIndex conflicts with existing questions
            boolean hasConflict = existingQuestions.stream()
                    .anyMatch(q -> q.getOrderIndex() == assessmentQuestion.getOrderIndex());
            
            if (hasConflict) {
                // Conflict exists, auto-assign next available
                int maxOrder = existingQuestions.stream()
                        .mapToInt(AssessmentQuestion::getOrderIndex)
                        .max()
                        .orElse(0);
                assessmentQuestion.setOrderIndex(maxOrder + 1);
            }
        }
        
        return assessmentQuestionRepository.save(assessmentQuestion);
    }

    @Override
    public Optional<AssessmentQuestion> findAssesmentQuestionById(UUID assessmentQuestionId) {
        return assessmentQuestionRepository.findById(assessmentQuestionId);
    }

    @Override
    @Transactional
    public AssessmentQuestion updateAssesmentQuestion( UUID assessmentQuestionId,
            AssessmentQuestion assessmentQuestion) {
        final UUID currentQuestionId = assessmentQuestionId; // Make effectively final for lambda
        return findAssesmentQuestionById( assessmentQuestionId)
            .map(existingQuestion -> {
                existingQuestion.setQuestionText(assessmentQuestion.getQuestionText());
                existingQuestion.setQuestionType(assessmentQuestion.getQuestionType());
                existingQuestion.setAnswerOptions(assessmentQuestion.getAnswerOptions());
                existingQuestion.setScoringRubric(assessmentQuestion.getScoringRubric());
                existingQuestion.setTimeLimit(assessmentQuestion.getTimeLimit());
                existingQuestion.setDifficultyLevel(assessmentQuestion.getDifficultyLevel());
                existingQuestion.setActive(assessmentQuestion.isActive());
                
                // Check for orderIndex conflicts when updating
                UUID behavioralIndicatorId = existingQuestion.getBehavioralIndicator().getId();
                List<AssessmentQuestion> otherQuestions = assessmentQuestionRepository.findByBehavioralIndicator_Id(behavioralIndicatorId)
                        .stream()
                        .filter(q -> !q.getId().equals(currentQuestionId)) // Exclude current question
                        .toList();
                
                final int requestedOrderIndex = assessmentQuestion.getOrderIndex();
                int finalOrderIndex;
                
                if (requestedOrderIndex <= 0) {
                    // Auto-assign next available if invalid
                    int maxOrder = otherQuestions.stream()
                            .mapToInt(AssessmentQuestion::getOrderIndex)
                            .max()
                            .orElse(0);
                    finalOrderIndex = maxOrder + 1;
                } else {
                    // Check if requested orderIndex conflicts with other questions
                    boolean hasConflict = otherQuestions.stream()
                            .anyMatch(q -> q.getOrderIndex() == requestedOrderIndex);
                    
                    if (hasConflict) {
                        // Auto-assign next available to avoid conflict
                        int maxOrder = otherQuestions.stream()
                                .mapToInt(AssessmentQuestion::getOrderIndex)
                                .max()
                                .orElse(0);
                        finalOrderIndex = maxOrder + 1;
                    } else {
                        finalOrderIndex = requestedOrderIndex;
                    }
                }
                
                existingQuestion.setOrderIndex(finalOrderIndex);
                
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
