package app.skillsoft.assessmentbackend.controller;


import app.skillsoft.assessmentbackend.domain.dto.CompetencyDto;
import app.skillsoft.assessmentbackend.domain.entities.Competency;
import app.skillsoft.assessmentbackend.domain.mapper.CompetencyMapper;
import app.skillsoft.assessmentbackend.services.CompetencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        logger.info("CompetencyController initialized");
    }

    @GetMapping
    public List<CompetencyDto> listCompetencies() {
        logger.info("GET /api/competencies endpoint called");
        List<Competency> competencies = competencyService.listCompetencies();
        logger.info("Found {} competencies", competencies.size());
        return competencies.stream().map(competencyMapper::toDto).toList();
    }
}
