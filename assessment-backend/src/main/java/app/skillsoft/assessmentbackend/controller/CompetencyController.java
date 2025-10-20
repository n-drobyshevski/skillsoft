package app.skillsoft.assessmentbackend.controller;


import app.skillsoft.assessmentbackend.domain.dto.CompetencyDto;
import app.skillsoft.assessmentbackend.domain.entities.Competency;
import app.skillsoft.assessmentbackend.domain.mapper.CompetencyMapper;
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
    private final CompetencyMapper competencyMapper;

    @Autowired
    public CompetencyController(CompetencyService competencyService, CompetencyMapper competencyMapper) {
        this.competencyService = competencyService;
        this.competencyMapper = competencyMapper;
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

    @PostMapping
    public CompetencyDto createCompetency(@RequestBody CompetencyDto competencyDto) {
        logger.info("POST /api/competencies endpoint called");

        Competency createdCompetency = competencyService.createCompetency(competencyMapper.fromDto(competencyDto));
        logger.info("Created competency with id: {}", createdCompetency.getId());
        return competencyMapper.toDto(createdCompetency);
    }


    @PutMapping("/{id}")
    public CompetencyDto updateCompetency(
            @PathVariable UUID id,
            @RequestBody CompetencyDto competencyDto) {
        logger.info("PUT /api/competencies/{} endpoint called", id);
        try {
            Competency competencyDetails = competencyMapper.fromDto(competencyDto);
            Competency updatedCompetency = competencyService.updateCompetency(id, competencyDetails);
            logger.info("Updated competency with id: {}", updatedCompetency.getId());
            return competencyMapper.toDto(updatedCompetency);
        } catch (RuntimeException e) {
            logger.error("Error updating competency with id {}: {}", id, e.getMessage());
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    public void deleteCompetency(@PathVariable UUID id) {
        logger.info("DELETE /api/competencies/{} endpoint called", id);
        try {
            competencyService.deleteCompetency(id);
            logger.info("Deleted competency with id: {}", id);
        } catch (RuntimeException e) {
            logger.error("Error deleting competency with id {}: {}", id, e.getMessage());
            throw e;
        }
    }
}
