package app.skillsoft.assessmentbackend.controller;


import app.skillsoft.assessmentbackend.domain.dto.BehavioralIndicatorDto;
import app.skillsoft.assessmentbackend.domain.entities.BehavioralIndicator;
import app.skillsoft.assessmentbackend.domain.mapper.BehavioralIndicatorMapper;
import app.skillsoft.assessmentbackend.services.BehavioralIndicatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/competencies/{competencyId}/bi")
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

    @GetMapping("/{biId}")
    public BehavioralIndicatorDto getBehavioralIndicatorById(
            @PathVariable("competencyId") UUID competencyId,
            @PathVariable("biId") UUID biId) {
        logger.info("GET /api/competencies/{}/bi/{} endpoint called", competencyId, biId);
        BehavioralIndicator indicator = behavioralIndicatorService.findBehavioralIndicatorById(competencyId, biId)
                .orElseThrow(() -> new RuntimeException("Behavioral Indicator not found"));
        return behavioralIndicatorMapper.toDto(indicator);
    }

    @PostMapping
    public BehavioralIndicatorDto createBehavioralIndicator(
            @PathVariable("competencyId") UUID competencyId,
            @RequestBody BehavioralIndicatorDto behavioralIndicatorDto) {
        logger.info("POST /api/competencies/{}/bi endpoint called", competencyId);

        BehavioralIndicator createdBI = behavioralIndicatorService.createBehavioralIndicator(competencyId,behavioralIndicatorMapper.fromDto(behavioralIndicatorDto));

        logger.info("Created behavioral indicator with id: {} for competency: {}", createdBI.getId(), competencyId);
        return behavioralIndicatorMapper.toDto(createdBI);
    }

    @PutMapping("/{biId}")
    public BehavioralIndicatorDto updateBehavioralIndicator(
            @PathVariable("competencyId") UUID competencyId,
            @PathVariable("biId") UUID biId,
            @RequestBody BehavioralIndicatorDto behavioralIndicatorDto) {
        logger.info("PUT /api/competencies/{}/bi/{} endpoint called", competencyId, biId);
        try {
            BehavioralIndicator indicatorDetails = behavioralIndicatorMapper.fromDto(behavioralIndicatorDto);
            BehavioralIndicator updatedIndicator = behavioralIndicatorService.updateBehavioralIndicator(competencyId, biId, indicatorDetails);
            logger.info("Updated behavioral indicator with id: {} for competency: {}", updatedIndicator.getId(), competencyId);
            return behavioralIndicatorMapper.toDto(updatedIndicator);
        } catch (RuntimeException e) {
            logger.error("Error updating behavioral indicator with id {} for competency {}: {}", biId, competencyId, e.getMessage());
            throw e;
        }
    }

    @DeleteMapping("/{biId}")
    public void deleteBehavioralIndicator(
            @PathVariable("competencyId") UUID competencyId,
            @PathVariable("biId") UUID biId) {
        logger.info("DELETE /api/competencies/{}/bi/{} endpoint called", competencyId, biId);
        try {
            behavioralIndicatorService.deleteBehavioralIndicator(competencyId, biId);
            logger.info("Deleted behavioral indicator with id: {} for competency: {}", biId, competencyId);

        } catch (RuntimeException e) {
            logger.error("Error deleting behavioral indicator with id {} for competency {}: {}", biId, competencyId, e.getMessage());
            throw e;
        }
    }
}
