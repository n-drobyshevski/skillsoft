// java
package app.skillsoft.assessmentbackend.services.impl;

import app.skillsoft.assessmentbackend.domain.entities.BehavioralIndicator;
import app.skillsoft.assessmentbackend.domain.entities.Competency;
import app.skillsoft.assessmentbackend.repository.BehavioralIndicatorRepository;
import app.skillsoft.assessmentbackend.repository.CompetencyRepository;
import app.skillsoft.assessmentbackend.services.BehavioralIndicatorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BehavioralIndicatorServiceImpl implements BehavioralIndicatorService {

    private final BehavioralIndicatorRepository behavioralIndicatorRepository;
    private final CompetencyRepository competencyRepository;

    public BehavioralIndicatorServiceImpl(BehavioralIndicatorRepository behavioralIndicatorRepository,
                                        CompetencyRepository competencyRepository) {
        this.behavioralIndicatorRepository = behavioralIndicatorRepository;
        this.competencyRepository = competencyRepository;
    }

    @Override
    public List<BehavioralIndicator> listAllBehavioralIndicators() {
        return this.behavioralIndicatorRepository.findAll();
    }

    @Override
    public List<BehavioralIndicator> listCompetencyBehavioralIndicators(UUID competencyId) {
        return behavioralIndicatorRepository.findByCompetencyId(competencyId);
    }

    @Override
    @Transactional
    public BehavioralIndicator createBehavioralIndicator(UUID competencyId, BehavioralIndicator behavioralIndicator) {
        if (behavioralIndicator.getId() != null) {
            throw new IllegalArgumentException("New behavioral indicator cannot already have an ID");
        }
        if (behavioralIndicator.getTitle() == null || behavioralIndicator.getTitle().isBlank()) {
            throw new IllegalArgumentException("Behavioral indicator title is required");
        }

        // Find and validate competency exists
        Competency competency = competencyRepository.findById(competencyId)
                .orElseThrow(() -> new RuntimeException("Competency not found with id: " + competencyId));

        // Set competency relationship
        behavioralIndicator.setCompetency(competency);

        // Set timestamps
        LocalDateTime now = LocalDateTime.now();

        return behavioralIndicatorRepository.save(behavioralIndicator);
    }

    @Override
    public Optional<BehavioralIndicator> findBehavioralIndicatorById( UUID behavioralIndicatorId) {
        return behavioralIndicatorRepository.findById(behavioralIndicatorId);
    }

    @Override
    @Transactional
    public BehavioralIndicator updateBehavioralIndicator(UUID behavioralIndicatorId, BehavioralIndicator behavioralIndicatorDetails) {
        return behavioralIndicatorRepository.findById(behavioralIndicatorId)
                .map(existingIndicator -> {
                    // Update fields
                    existingIndicator.setTitle(behavioralIndicatorDetails.getTitle());
                    existingIndicator.setDescription(behavioralIndicatorDetails.getDescription());
                    existingIndicator.setObservabilityLevel(behavioralIndicatorDetails.getObservabilityLevel());
                    existingIndicator.setMeasurementType(behavioralIndicatorDetails.getMeasurementType());
                    existingIndicator.setWeight(behavioralIndicatorDetails.getWeight());
                    existingIndicator.setExamples(behavioralIndicatorDetails.getExamples());
                    existingIndicator.setCounterExamples(behavioralIndicatorDetails.getCounterExamples());
                    existingIndicator.setActive(behavioralIndicatorDetails.isActive());
                    existingIndicator.setApprovalStatus(behavioralIndicatorDetails.getApprovalStatus());
                    existingIndicator.setOrderIndex(behavioralIndicatorDetails.getOrderIndex());


                    return behavioralIndicatorRepository.save(existingIndicator);
                })
                .orElseThrow(() -> new RuntimeException("Behavioral indicator not found with id: " + behavioralIndicatorId ));
    }

    @Override
    @Transactional
    public void deleteBehavioralIndicator(UUID behavioralIndicatorId) {
        BehavioralIndicator indicator = behavioralIndicatorRepository.findById(behavioralIndicatorId)
                .orElseThrow(() -> new RuntimeException("Behavioral indicator not found with id: " + behavioralIndicatorId));

        behavioralIndicatorRepository.delete(indicator);
    }
}
