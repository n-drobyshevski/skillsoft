package app.skillsoft.assessmentbackend.controller.v1;


import app.skillsoft.assessmentbackend.domain.dto.AssessmentQuestionDto;
import app.skillsoft.assessmentbackend.domain.dto.BehavioralIndicatorDto;
import app.skillsoft.assessmentbackend.domain.dto.request.CreateIndicatorRequest;
import app.skillsoft.assessmentbackend.domain.dto.request.UpdateIndicatorRequest;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentQuestion;
import app.skillsoft.assessmentbackend.domain.entities.BehavioralIndicator;
import app.skillsoft.assessmentbackend.domain.mapper.AssessmentQuestionMapper;
import app.skillsoft.assessmentbackend.domain.mapper.BehavioralIndicatorMapper;
import app.skillsoft.assessmentbackend.services.AssessmentQuestionService;
import app.skillsoft.assessmentbackend.services.BehavioralIndicatorService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * V1 REST Controller for Behavioral Indicator management.
 *
 * This is the standardized API version per docs/API_STANDARDIZATION_STRATEGY.md.
 * All endpoints use validated Request DTOs and follow consistent patterns.
 *
 * Security:
 * - GET endpoints: All authenticated users (ROLE_USER)
 * - POST/PUT/DELETE: ADMIN or EDITOR role required
 */
@RestController
@RequestMapping("/api/v1/behavioral-indicators")
public class BehavioralIndicatorControllerV1 {

    private static final Logger logger = LoggerFactory.getLogger(BehavioralIndicatorControllerV1.class);

    private final BehavioralIndicatorService behavioralIndicatorService;
    private final AssessmentQuestionService assessmentQuestionService;
    private final AssessmentQuestionMapper assessmentQuestionMapper;
    private final BehavioralIndicatorMapper behavioralIndicatorMapper;

    public BehavioralIndicatorControllerV1(
            BehavioralIndicatorService behavioralIndicatorService,
            AssessmentQuestionService assessmentQuestionService,
            AssessmentQuestionMapper assessmentQuestionMapper,
            BehavioralIndicatorMapper behavioralIndicatorMapper) {
        this.behavioralIndicatorService = behavioralIndicatorService;
        this.assessmentQuestionService = assessmentQuestionService;
        this.assessmentQuestionMapper = assessmentQuestionMapper;
        this.behavioralIndicatorMapper = behavioralIndicatorMapper;
    }

    @GetMapping
    public List<BehavioralIndicatorDto> listBehavioralIndicators() {
        logger.info("GET /api/v1/behavioral-indicators endpoint called");
        List<BehavioralIndicator> indicators = behavioralIndicatorService.listAllBehavioralIndicators();
        logger.info("Found {} behavioral indicators", indicators.size());
        return indicators.stream().map(behavioralIndicatorMapper::toDto).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<BehavioralIndicatorDto> getBehavioralIndicatorById(
            @PathVariable("id") UUID id) {
        logger.info("GET /api/v1/behavioral-indicators/{} endpoint called", id);
        return behavioralIndicatorService.findBehavioralIndicatorById(id)
                .map(indicator -> ResponseEntity.ok(behavioralIndicatorMapper.toDto(indicator)))
                .orElseGet(() -> {
                    logger.warn("Behavioral indicator with id {} not found", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @GetMapping("/{id}/questions")
    public List<AssessmentQuestionDto> listIndicatorQuestions(
            @PathVariable("id") UUID id) {
        logger.info("GET /api/v1/behavioral-indicators/{}/questions endpoint called", id);
        List<AssessmentQuestion> assessmentQuestions = assessmentQuestionService.listIndicatorAssessmentQuestions(id);
        logger.info("Found {} questions for behavioral indicator {}", assessmentQuestions.size(), id);
        return assessmentQuestions.stream().map(assessmentQuestionMapper::toDto).toList();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<BehavioralIndicatorDto> createBehavioralIndicator(
            @Valid @RequestBody CreateIndicatorRequest request) {
        logger.info("POST /api/v1/behavioral-indicators endpoint called");

        try {
            BehavioralIndicator indicatorEntity = behavioralIndicatorMapper.fromCreateRequest(request);
            BehavioralIndicator createdBI = behavioralIndicatorService.createBehavioralIndicator(
                    request.competencyId(), indicatorEntity);
            logger.info("Created behavioral indicator with id: {}", createdBI.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(behavioralIndicatorMapper.toDto(createdBI));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid behavioral indicator data: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<BehavioralIndicatorDto> updateBehavioralIndicator(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateIndicatorRequest request) {
        logger.info("PUT /api/v1/behavioral-indicators/{} endpoint called", id);
        try {
            BehavioralIndicator indicatorEntity = behavioralIndicatorMapper.fromUpdateRequest(request);
            BehavioralIndicator updatedIndicator = behavioralIndicatorService.updateBehavioralIndicator(id, indicatorEntity);
            logger.info("Updated behavioral indicator with id: {}", updatedIndicator.getId());
            return ResponseEntity.ok(behavioralIndicatorMapper.toDto(updatedIndicator));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                logger.warn("Behavioral indicator with id {} not found for update", id);
                return ResponseEntity.notFound().build();
            }
            logger.error("Error updating behavioral indicator with id {}: {}", id, e.getMessage());
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<Void> deleteBehavioralIndicator(
            @PathVariable("id") UUID id) {
        logger.info("DELETE /api/v1/behavioral-indicators/{} endpoint called", id);
        try {
            behavioralIndicatorService.deleteBehavioralIndicator(id);
            logger.info("Deleted behavioral indicator with id: {}", id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                logger.warn("Behavioral indicator with id {} not found for deletion", id);
                return ResponseEntity.notFound().build();
            }
            logger.error("Error deleting behavioral indicator with id {}: {}", id, e.getMessage());
            throw e;
        }
    }
}
