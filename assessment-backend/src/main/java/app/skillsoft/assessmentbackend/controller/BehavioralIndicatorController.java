package app.skillsoft.assessmentbackend.controller;


import app.skillsoft.assessmentbackend.domain.dto.BehavioralIndicatorDto;
import app.skillsoft.assessmentbackend.domain.entities.BehavioralIndicator;
import app.skillsoft.assessmentbackend.domain.mapper.BehavioralIndicatorMapper;
import app.skillsoft.assessmentbackend.services.BehavioralIndicatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/competencies/{competencyId}/bi")
public class BehavioralIndicatorController {

    private static final Logger logger = LoggerFactory.getLogger(BehavioralIndicatorController.class);

    private final BehavioralIndicatorService behavioralIndicatorService;
    private final BehavioralIndicatorMapper behavioralIndicatorMapper;

    public BehavioralIndicatorController(BehavioralIndicatorService behavioralIndicatorService, BehavioralIndicatorMapper behavioralIndicatorMapper) {
        this.behavioralIndicatorService = behavioralIndicatorService;
        this.behavioralIndicatorMapper = behavioralIndicatorMapper;
    }

    @GetMapping
    public List<BehavioralIndicatorDto> listBehavioralIndicators(@PathVariable("competencyId") UUID competencyId) {
        logger.info("GET /api/competencies/{}/bi endpoint called", competencyId);
        List<BehavioralIndicator> indicators = behavioralIndicatorService.listBehavioralIndicators(competencyId);
        logger.info("Found {} behavioral indicators for competency {}", indicators.size(), competencyId);
        return indicators.stream().map(behavioralIndicatorMapper::toDto).toList();
    }

}
