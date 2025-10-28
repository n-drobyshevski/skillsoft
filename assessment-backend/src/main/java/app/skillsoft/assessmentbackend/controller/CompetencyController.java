package app.skillsoft.assessmentbackend.controller;


import app.skillsoft.assessmentbackend.domain.dto.BehavioralIndicatorDto;
import app.skillsoft.assessmentbackend.domain.dto.CompetencyDto;
import app.skillsoft.assessmentbackend.domain.entities.BehavioralIndicator;
import app.skillsoft.assessmentbackend.domain.entities.Competency;
import app.skillsoft.assessmentbackend.domain.mapper.BehavioralIndicatorMapper;
import app.skillsoft.assessmentbackend.domain.mapper.CompetencyMapper;
import app.skillsoft.assessmentbackend.services.BehavioralIndicatorService;
import app.skillsoft.assessmentbackend.services.CompetencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

import java.util.List;

@RestController
@RequestMapping("/api/competencies")
public class CompetencyController {

    private static final Logger logger = LoggerFactory.getLogger(CompetencyController.class);

    private final CompetencyService competencyService;
    private final BehavioralIndicatorService behavioralIndicatorService;
    private final CompetencyMapper competencyMapper;
    private final BehavioralIndicatorMapper behavioralIndicatorMapper;

    @Autowired
    public CompetencyController(CompetencyService competencyService, BehavioralIndicatorService behavioralIndicatorService, CompetencyMapper competencyMapper, BehavioralIndicatorMapper behavioralIndicatorMapper) {
        this.competencyService = competencyService;
        this.behavioralIndicatorService = behavioralIndicatorService;
        this.competencyMapper = competencyMapper;
        this.behavioralIndicatorMapper = behavioralIndicatorMapper;
    }

    @GetMapping
    public List<CompetencyDto> listCompetencies() {
        logger.info("GET /api/competencies endpoint called");
        List<Competency> competencies = competencyService.listCompetencies();
        logger.info("Found {} competencies", competencies.size());
        return competencies.stream().map(competencyMapper::toDto).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompetencyDto> getCompetencyById(@PathVariable UUID id) {
        logger.info("GET /api/competencies/{} endpoint called", id);
        return competencyService.findCompetencyById(id)
                .map(competency -> ResponseEntity.ok(competencyMapper.toDto(competency)))
                .orElseGet(() -> {
                    logger.warn("Competency with id {} not found", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @GetMapping("/{competencyId}/bi")
    public List<BehavioralIndicatorDto> listCompetencyBehavioralIndicators(@PathVariable("competencyId") UUID competencyId) {
        logger.info("GET /api/competencies/{}/bi endpoint called", competencyId);
        List<BehavioralIndicator> indicators = behavioralIndicatorService.listCompetencyBehavioralIndicators(competencyId);
        logger.info("Found {} behavioral indicators for competency {}", indicators.size(), competencyId);
        return indicators.stream().map(behavioralIndicatorMapper::toDto).toList();
    }

    @PostMapping
    public ResponseEntity<CompetencyDto> createCompetency(@RequestBody CompetencyDto competencyDto) {
        logger.info("POST /api/competencies endpoint called");

        try {
            Competency createdCompetency = competencyService.createCompetency(competencyMapper.fromDto(competencyDto));
            logger.info("Created competency with id: {}", createdCompetency.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(competencyMapper.toDto(createdCompetency));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid competency data: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error creating competency: {}", e.getMessage());
            throw e;
        }
    }


    @PutMapping("/{id}")
    public ResponseEntity<CompetencyDto> updateCompetency(
            @PathVariable UUID id,
            @RequestBody CompetencyDto competencyDto) {
        logger.info("PUT /api/competencies/{} endpoint called", id);
        try {
            Competency competencyDetails = competencyMapper.fromDto(competencyDto);
            Competency updatedCompetency = competencyService.updateCompetency(id, competencyDetails);
            logger.info("Updated competency with id: {}", updatedCompetency.getId());
            return ResponseEntity.ok(competencyMapper.toDto(updatedCompetency));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Competency not found")) {
                logger.warn("Competency with id {} not found for update", id);
                return ResponseEntity.notFound().build();
            }
            logger.error("Error updating competency with id {}: {}", id, e.getMessage());
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompetency(@PathVariable UUID id) {
        logger.info("DELETE /api/competencies/{} endpoint called", id);
        
        // Check if competency exists before attempting deletion
        if (competencyService.findCompetencyById(id).isEmpty()) {
            logger.warn("Competency with id {} not found for deletion", id);
            return ResponseEntity.notFound().build();
        }
        
        try {
            competencyService.deleteCompetency(id);
            logger.info("Deleted competency with id: {}", id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            logger.error("Error deleting competency with id {}: {}", id, e.getMessage());
            throw e;
        }
    }
}
