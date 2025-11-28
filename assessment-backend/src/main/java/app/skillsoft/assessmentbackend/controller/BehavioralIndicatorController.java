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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Behavioral Indicator management.
 * 
 * Security:
 * - GET endpoints: All authenticated users (ROLE_USER)
 * - POST/PUT/DELETE: ADMIN or EDITOR role required
 */
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
    public ResponseEntity<BehavioralIndicatorDto> getBehavioralIndicatorById(
            @PathVariable("biId") UUID biId) {
        logger.info("GET /api/behavioral-indicators/{} endpoint called",  biId);
        return behavioralIndicatorService.findBehavioralIndicatorById(biId)
                .map(indicator -> ResponseEntity.ok(behavioralIndicatorMapper.toDto(indicator)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{behavioralIndicatorId}/questions")
    public List<AssessmentQuestionDto> listAssessmentQuestions(@PathVariable(name="behavioralIndicatorId") UUID behavioralIndicatorId) {
        List<AssessmentQuestion> assessmentQuestions = assessmentQuestionService.listIndicatorAssessmentQuestions(behavioralIndicatorId);
        return assessmentQuestions.stream().map(assessmentQuestionMapper::toDto).toList();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public BehavioralIndicatorDto createBehavioralIndicator(
            @RequestBody BehavioralIndicatorDto behavioralIndicatorDto) {
        logger.info("POST /api/behavioral-indicators/ endpoint called");

        // Extract competencyId from the DTO
        UUID competencyId = behavioralIndicatorDto.competencyId();
        BehavioralIndicator createdBI = behavioralIndicatorService.createBehavioralIndicator(competencyId, behavioralIndicatorMapper.fromDto(behavioralIndicatorDto));

        logger.info("Created behavioral indicator with id: {} ", createdBI.getId());
        return behavioralIndicatorMapper.toDto(createdBI);
    }

    @PutMapping("/{biId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<BehavioralIndicatorDto> updateBehavioralIndicator(
            @PathVariable("biId") UUID biId,
            @RequestParam(value = "competencyId", required = false) UUID competencyId,
            @RequestBody BehavioralIndicatorDto behavioralIndicatorDto) {
        logger.info("PUT /api/behavioral-indicators/{} endpoint called", biId);
        try {
            BehavioralIndicator indicatorDetails = behavioralIndicatorMapper.fromDto(behavioralIndicatorDto);
            BehavioralIndicator updatedIndicator = behavioralIndicatorService.updateBehavioralIndicator(biId, indicatorDetails);
            logger.info("Updated behavioral indicator with id: {}", updatedIndicator.getId());
            return ResponseEntity.ok(behavioralIndicatorMapper.toDto(updatedIndicator));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                logger.warn("Behavioral indicator with id {} not found for update", biId);
                return ResponseEntity.notFound().build();
            }
            logger.error("Error updating behavioral indicator with id {}: {}", biId, e.getMessage());
            throw e;
        }
    }

    @DeleteMapping("/{biId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<Void> deleteBehavioralIndicator(
            @PathVariable("biId") UUID biId) {
        logger.info("DELETE /api/behavioral-indicators/{} endpoint called",  biId);
        try {
            behavioralIndicatorService.deleteBehavioralIndicator( biId);
            logger.info("Deleted behavioral indicator with id: {} ", biId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                logger.warn("Behavioral indicator with id {} not found for deletion", biId);
                return ResponseEntity.notFound().build();
            }
            logger.error("Error deleting behavioral indicator with id {} : {}", biId, e.getMessage());
            throw e;
        }
    }
}
