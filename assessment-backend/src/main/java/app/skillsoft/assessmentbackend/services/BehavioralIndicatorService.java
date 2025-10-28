package app.skillsoft.assessmentbackend.services;

import app.skillsoft.assessmentbackend.domain.entities.BehavioralIndicator;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BehavioralIndicatorService {

    List<BehavioralIndicator> listAllBehavioralIndicators();
    List<BehavioralIndicator> listCompetencyBehavioralIndicators(UUID competencyId);

    BehavioralIndicator createBehavioralIndicator(UUID competencyId, BehavioralIndicator behavioralIndicator);

    Optional<BehavioralIndicator> findBehavioralIndicatorById(UUID behavioralIndicatorId);

    BehavioralIndicator updateBehavioralIndicator(UUID behavioralIndicatorId, BehavioralIndicator behavioralIndicator);

    void deleteBehavioralIndicator( UUID behavioralIndicatorId);
}
