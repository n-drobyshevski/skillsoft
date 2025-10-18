// java
package app.skillsoft.assessmentbackend.services.impl;

import app.skillsoft.assessmentbackend.domain.entities.BehavioralIndicator;
import app.skillsoft.assessmentbackend.repository.BehavioralIndicatorRepository;
import app.skillsoft.assessmentbackend.services.BehavioralIndicatorService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class BehavioralIndicatorServiceImpl implements BehavioralIndicatorService {

    private final BehavioralIndicatorRepository behavioralIndicatorRepository;

    public BehavioralIndicatorServiceImpl(BehavioralIndicatorRepository behavioralIndicatorRepository) {
        this.behavioralIndicatorRepository = behavioralIndicatorRepository;
    }

    @Override
    public List<BehavioralIndicator> listBehavioralIndicators(UUID competencyId) {
        return behavioralIndicatorRepository.findByCompetencyId(competencyId);
    }
}
