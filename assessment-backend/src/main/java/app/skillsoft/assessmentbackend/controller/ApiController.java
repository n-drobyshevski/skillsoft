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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ApiController {

    private static final Logger logger = LoggerFactory.getLogger(CompetencyController.class);

    private final AssessmentQuestionService assessmentQuestionService;
    private final BehavioralIndicatorService behavioralIndicatorService;
    private final AssessmentQuestionMapper assessmentQuestionMapper;
    private final BehavioralIndicatorMapper behavioralIndicatorMapper;


    public ApiController(AssessmentQuestionService assessmentQuestionService, BehavioralIndicatorService behavioralIndicatorService, AssessmentQuestionMapper assessmentQuestionMapper, BehavioralIndicatorMapper behavioralIndicatorMapper) {
        this.assessmentQuestionService = assessmentQuestionService;
        this.behavioralIndicatorService = behavioralIndicatorService;
        this.assessmentQuestionMapper = assessmentQuestionMapper;
        this.behavioralIndicatorMapper = behavioralIndicatorMapper;
    }
    @GetMapping("/behavioral-indicators")
    public List<BehavioralIndicatorDto> listAllBehavioralIndicators() {
        logger.info("GET /api/competencies endpoint called");
        List<BehavioralIndicator> competencies = behavioralIndicatorService.listAllBehavioralIndicators();
        logger.info("Found {} competencies", competencies.size());
        return competencies.stream().map(behavioralIndicatorMapper::toDto).toList();
    }
    @GetMapping("/assessment-questions")
    public List<AssessmentQuestionDto> listCompetencies() {
        logger.info("GET /api/competencies endpoint called");
        List<AssessmentQuestion> competencies = assessmentQuestionService.listAllQuestions();
        logger.info("Found {} competencies", competencies.size());
        return competencies.stream().map(assessmentQuestionMapper::toDto).toList();
    }
}
