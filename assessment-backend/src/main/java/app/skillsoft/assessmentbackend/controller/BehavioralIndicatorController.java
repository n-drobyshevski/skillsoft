package app.skillsoft.assessmentbackend.controller;


import app.skillsoft.assessmentbackend.domain.dto.AssessmentQuestionDto;
import app.skillsoft.assessmentbackend.domain.dto.BehavioralIndicatorDto;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentQuestion;
import app.skillsoft.assessmentbackend.domain.entities.BehavioralIndicator;
import app.skillsoft.assessmentbackend.domain.mapper.AssessmentQuestionMapper;
import app.skillsoft.assessmentbackend.domain.mapper.BehavioralIndicatorMapper;
import app.skillsoft.assessmentbackend.services.AssessmentQuestionService;
import app.skillsoft.assessmentbackend.services.BehavioralIndicatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/behavioral-indicators")
public class BehavioralIndicatorController {

    private static final Logger logger = LoggerFactory.getLogger(BehavioralIndicatorController.class);

    private final BehavioralIndicatorService behavioralIndicatorService;
    private final AssessmentQuestionService assessmentQuestionService;
    private final AssessmentQuestionMapper assessmentQuestionMapper;
    private final BehavioralIndicatorMapper behavioralIndicatorMapper;

    public BehavioralIndicatorController(BehavioralIndicatorService behavioralIndicatorService, AssessmentQuestionService assessmentQuestionService, AssessmentQuestionMapper assessmentQuestionMapper, BehavioralIndicatorMapper behavioralIndicatorMapper) {
        this.behavioralIndicatorService = behavioralIndicatorService;
        this.assessmentQuestionService = assessmentQuestionService;
        this.assessmentQuestionMapper = assessmentQuestionMapper;
        this.behavioralIndicatorMapper = behavioralIndicatorMapper;
    }

    @GetMapping
    public List<BehavioralIndicatorDto> listBehavioralIndicators() {
        logger.info("GET /api/competencies endpoint called");
        List<BehavioralIndicator> competencies = behavioralIndicatorService.listAllBehavioralIndicators();
        logger.info("Found {} competencies", competencies.size());
        return competencies.stream().map(behavioralIndicatorMapper::toDto).toList();
    }


    @GetMapping("/{biId}")
    public BehavioralIndicatorDto getBehavioralIndicatorById(
            @PathVariable("biId") UUID biId) {
        logger.info("GET /api/behavioral-indicators/{} endpoint called",  biId);
        BehavioralIndicator indicator = behavioralIndicatorService.findBehavioralIndicatorById(biId)
                .orElseThrow(() -> new RuntimeException("Behavioral Indicator not found"));
        return behavioralIndicatorMapper.toDto(indicator);
    }

    @GetMapping("/{behavioralIndicatorId}/questions")
    public List<AssessmentQuestionDto> listAssessmentQuestions(@PathVariable(name="behavioralIndicatorId") UUID behavioralIndicatorId) {
        List<AssessmentQuestion> assessmentQuestions = assessmentQuestionService.listIndicatorAssessmentQuestions(behavioralIndicatorId);
        return assessmentQuestions.stream().map(assessmentQuestionMapper::toDto).toList();
    }

    @PostMapping
    public BehavioralIndicatorDto createBehavioralIndicator(
            @RequestBody UUID competencyId,
            @RequestBody BehavioralIndicatorDto behavioralIndicatorDto) {
        logger.info("POST /api/behavioral-indicators/ endpoint called");

        BehavioralIndicator createdBI = behavioralIndicatorService.createBehavioralIndicator(competencyId, behavioralIndicatorMapper.fromDto(behavioralIndicatorDto));

        logger.info("Created behavioral indicator with id: {} ", createdBI.getId());
        return behavioralIndicatorMapper.toDto(createdBI);
    }

    @PutMapping("/{biId}")
    public BehavioralIndicatorDto updateBehavioralIndicator(
            @PathVariable("biId") UUID biId,
            @RequestBody BehavioralIndicatorDto behavioralIndicatorDto) {
        logger.info("PUT /api/behavioral-indicators/{} endpoint called", biId);
        try {
            BehavioralIndicator indicatorDetails = behavioralIndicatorMapper.fromDto(behavioralIndicatorDto);
            BehavioralIndicator updatedIndicator = behavioralIndicatorService.updateBehavioralIndicator(biId, indicatorDetails);
            logger.info("Updated behavioral indicator with id: {} for competency: {}", updatedIndicator.getId());
            return behavioralIndicatorMapper.toDto(updatedIndicator);
        } catch (RuntimeException e) {
            logger.error("Error updating behavioral indicator with id {} for competency {}: {}", biId, e.getMessage());
            throw e;
        }
    }

    @DeleteMapping("/{biId}")
    public void deleteBehavioralIndicator(
            @PathVariable("biId") UUID biId) {
        logger.info("DELETE /api/behavioral-indicators/{} endpoint called",  biId);
        try {
            behavioralIndicatorService.deleteBehavioralIndicator( biId);
            logger.info("Deleted behavioral indicator with id: {} ", biId);

        } catch (RuntimeException e) {
            logger.error("Error deleting behavioral indicator with id {} : {}", biId, e.getMessage());
            throw e;
        }
    }
}
