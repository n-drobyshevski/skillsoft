package app.skillsoft.assessmentbackend.controller;


import app.skillsoft.assessmentbackend.domain.dto.AssessmentQuestionDto;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentQuestion;
import app.skillsoft.assessmentbackend.domain.mapper.AssessmentQuestionMapper;
import app.skillsoft.assessmentbackend.services.AssessmentQuestionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/competencies/{competencyId}/bi/{behavioralIndicatorId}/questions")
public class AssessmentQuestionController {

    private final AssessmentQuestionMapper assessmentQuestionMapper;
    private final AssessmentQuestionService assessmentQuestionService;

    public AssessmentQuestionController(AssessmentQuestionMapper assessmentQuestionMapper, AssessmentQuestionService assessmentQuestionService) {
        this.assessmentQuestionMapper = assessmentQuestionMapper;
        this.assessmentQuestionService = assessmentQuestionService;
    }

    @GetMapping
    public List<AssessmentQuestionDto> listAssessmentQuestions(@PathVariable(name="behavioralIndicatorId") UUID behavioralIndicatorId) {
        List<AssessmentQuestion> assessmentQuestions = assessmentQuestionService.listAssessmentQuestions(behavioralIndicatorId);
        return assessmentQuestions.stream().map(assessmentQuestionMapper::toDto).toList();
    }
}
