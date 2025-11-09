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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<BehavioralIndicatorDto> createBehavioralIndicator(
            @RequestParam("competencyId") UUID competencyId,
            @RequestBody BehavioralIndicatorDto behavioralIndicatorDto) {
        logger.info("POST /api/behavioral-indicators/ endpoint called");
        try {
            BehavioralIndicator createdBI = behavioralIndicatorService.createBehavioralIndicator(competencyId, behavioralIndicatorMapper.fromDto(behavioralIndicatorDto));
            logger.info("Created behavioral indicator with id: {} ", createdBI.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(behavioralIndicatorMapper.toDto(createdBI));
        } catch (RuntimeException e) {
            logger.error("Error creating behavioral indicator: {}", e.getMessage());
            // Assuming other runtime exceptions could be "Competency not found"
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{biId}")
    public ResponseEntity<BehavioralIndicatorDto> updateBehavioralIndicator(
            @PathVariable("biId") UUID biId,
            @RequestParam(value = "competencyId", required = false) UUID competencyId,
            @RequestBody BehavioralIndicatorDto behavioralIndicatorDto) {
        logger.info("PUT /api/behavioral-indicators/{} endpoint called with competencyId: {}", biId, competencyId);
        try {
            BehavioralIndicator indicatorDetails = behavioralIndicatorMapper.fromDto(behavioralIndicatorDto);
            
            BehavioralIndicator updatedIndicator;
            if (competencyId != null) {
                // Update with competency change (reattachment)
                updatedIndicator = behavioralIndicatorService.updateBehavioralIndicatorCompetency(biId, competencyId, indicatorDetails);
                logger.info("Updated behavioral indicator {} and reattached to competency {}", updatedIndicator.getId(), competencyId);
            } else {
                // Regular update without competency change
                updatedIndicator = behavioralIndicatorService.updateBehavioralIndicator(biId, indicatorDetails);
                logger.info("Updated behavioral indicator with id: {}", updatedIndicator.getId());
            }
            
            return ResponseEntity.ok(behavioralIndicatorMapper.toDto(updatedIndicator));
        } catch (RuntimeException e) {
            logger.error("Error updating behavioral indicator: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{biId}")
    public ResponseEntity<Void> deleteBehavioralIndicator(
            @PathVariable("biId") UUID biId) {
        logger.info("DELETE /api/behavioral-indicators/{} endpoint called",  biId);
        behavioralIndicatorService.deleteBehavioralIndicator(biId);
        logger.info("Deleted behavioral indicator with id: {} ", biId);
        return ResponseEntity.noContent().build();
    }
}
