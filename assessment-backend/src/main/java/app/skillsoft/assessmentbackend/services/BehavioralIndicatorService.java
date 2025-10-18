package app.skillsoft.assessmentbackend.services;

import app.skillsoft.assessmentbackend.domain.entities.BehavioralIndicator;

import java.util.List;
import java.util.UUID;

public interface BehavioralIndicatorService {
    List<BehavioralIndicator> listBehavioralIndicators(UUID competencyId);
}
