// java
package app.skillsoft.assessmentbackend.services.impl;

import app.skillsoft.assessmentbackend.config.CacheConfig;
import app.skillsoft.assessmentbackend.domain.entities.BehavioralIndicator;
import app.skillsoft.assessmentbackend.domain.entities.Competency;
import app.skillsoft.assessmentbackend.repository.BehavioralIndicatorRepository;
import app.skillsoft.assessmentbackend.repository.CompetencyRepository;
import app.skillsoft.assessmentbackend.services.BehavioralIndicatorService;
import org.springframework.cache.annotation.CacheEvict;
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
    @CacheEvict(value = CacheConfig.COMPETENCIES_CACHE, allEntries = true)
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

        // Auto-assign orderIndex if not provided, invalid, or conflicts with existing
        List<BehavioralIndicator> existingIndicators = behavioralIndicatorRepository.findByCompetencyId(competencyId);
        
        if (behavioralIndicator.getOrderIndex() == null || behavioralIndicator.getOrderIndex() <= 0) {
            // No orderIndex provided, auto-assign next available
            int maxOrder = existingIndicators.stream()
                    .filter(bi -> bi.getOrderIndex() != null)
                    .mapToInt(BehavioralIndicator::getOrderIndex)
                    .max()
                    .orElse(0);
            behavioralIndicator.setOrderIndex(maxOrder + 1);
        } else {
            // Check if the provided orderIndex conflicts with existing indicators
            boolean hasConflict = existingIndicators.stream()
                    .anyMatch(bi -> bi.getOrderIndex() != null && bi.getOrderIndex().equals(behavioralIndicator.getOrderIndex()));
            
            if (hasConflict) {
                // Conflict exists, auto-assign next available
                int maxOrder = existingIndicators.stream()
                        .filter(bi -> bi.getOrderIndex() != null)
                        .mapToInt(BehavioralIndicator::getOrderIndex)
                        .max()
                        .orElse(0);
                behavioralIndicator.setOrderIndex(maxOrder + 1);
            }
        }

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
    @CacheEvict(value = CacheConfig.COMPETENCIES_CACHE, allEntries = true)
    public BehavioralIndicator updateBehavioralIndicator(UUID behavioralIndicatorId, BehavioralIndicator behavioralIndicatorDetails) {
        final UUID currentIndicatorId = behavioralIndicatorId; // Make effectively final for lambda
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
                    
                    // Handle orderIndex with conflict detection
                    UUID competencyId = existingIndicator.getCompetency().getId();
                    List<BehavioralIndicator> otherIndicators = behavioralIndicatorRepository.findByCompetencyId(competencyId)
                            .stream()
                            .filter(bi -> !bi.getId().equals(currentIndicatorId)) // Exclude current indicator
                            .toList();
                    
                    final Integer requestedOrderIndex = behavioralIndicatorDetails.getOrderIndex();
                    Integer finalOrderIndex;
                    
                    if (requestedOrderIndex == null || requestedOrderIndex <= 0) {
                        // Auto-assign next available if invalid
                        int maxOrder = otherIndicators.stream()
                                .filter(bi -> bi.getOrderIndex() != null)
                                .mapToInt(BehavioralIndicator::getOrderIndex)
                                .max()
                                .orElse(0);
                        finalOrderIndex = maxOrder + 1;
                    } else {
                        // Check if requested orderIndex conflicts with other indicators
                        boolean hasConflict = otherIndicators.stream()
                                .anyMatch(bi -> bi.getOrderIndex() != null && bi.getOrderIndex().equals(requestedOrderIndex));
                        
                        if (hasConflict) {
                            // Auto-assign next available to avoid conflict
                            int maxOrder = otherIndicators.stream()
                                    .filter(bi -> bi.getOrderIndex() != null)
                                    .mapToInt(BehavioralIndicator::getOrderIndex)
                                    .max()
                                    .orElse(0);
                            finalOrderIndex = maxOrder + 1;
                        } else {
                            finalOrderIndex = requestedOrderIndex;
                        }
                    }
                    
                    existingIndicator.setOrderIndex(finalOrderIndex);

                    return behavioralIndicatorRepository.save(existingIndicator);
                })
                .orElseThrow(() -> new RuntimeException("Behavioral indicator not found with id: " + behavioralIndicatorId ));
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.COMPETENCIES_CACHE, allEntries = true)
    public BehavioralIndicator updateBehavioralIndicatorCompetency(UUID behavioralIndicatorId, UUID newCompetencyId, BehavioralIndicator behavioralIndicatorDetails) {
        // Find the existing indicator
        BehavioralIndicator existingIndicator = behavioralIndicatorRepository.findById(behavioralIndicatorId)
                .orElseThrow(() -> new RuntimeException("Behavioral indicator not found with id: " + behavioralIndicatorId));

        // Find the new competency
        Competency newCompetency = competencyRepository.findById(newCompetencyId)
                .orElseThrow(() -> new RuntimeException("Competency not found with id: " + newCompetencyId));

        // Check if title conflict exists in the new competency
        // A title conflict occurs when the new competency already has an indicator with the same title
        List<BehavioralIndicator> existingWithTitle = behavioralIndicatorRepository.findByCompetencyId(newCompetencyId)
                .stream()
                .filter(bi -> bi.getTitle().equalsIgnoreCase(behavioralIndicatorDetails.getTitle()) && !bi.getId().equals(behavioralIndicatorId))
                .toList();
        
        if (!existingWithTitle.isEmpty()) {
            throw new RuntimeException("An indicator with title '" + behavioralIndicatorDetails.getTitle() + "' already exists in the target competency. Please use a unique title for reattachment.");
        }

        // Update all fields including competency relationship
        existingIndicator.setCompetency(newCompetency);
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
                    existingIndicator.setContextScope(behavioralIndicatorDetails.getContextScope());        return behavioralIndicatorRepository.save(existingIndicator);
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.COMPETENCIES_CACHE, allEntries = true)
    public void deleteBehavioralIndicator(UUID behavioralIndicatorId) {
        BehavioralIndicator indicator = behavioralIndicatorRepository.findById(behavioralIndicatorId)
                .orElseThrow(() -> new RuntimeException("Behavioral indicator not found with id: " + behavioralIndicatorId));

        behavioralIndicatorRepository.delete(indicator);
    }
}
