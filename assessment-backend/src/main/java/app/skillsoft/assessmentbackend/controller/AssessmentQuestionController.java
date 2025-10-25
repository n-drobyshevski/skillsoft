package app.skillsoft.assessmentbackend.controller;


import app.skillsoft.assessmentbackend.domain.dto.AssessmentQuestionDto;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentQuestion;
import app.skillsoft.assessmentbackend.domain.entities.QuestionType;
import app.skillsoft.assessmentbackend.domain.mapper.AssessmentQuestionMapper;
import app.skillsoft.assessmentbackend.services.AssessmentQuestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/competencies/{competencyId}/bi/{behavioralIndicatorId}/questions")
public class AssessmentQuestionController {

    private final AssessmentQuestionMapper assessmentQuestionMapper;
    private final AssessmentQuestionService assessmentQuestionService;

    public AssessmentQuestionController(AssessmentQuestionMapper assessmentQuestionMapper, 
                                        AssessmentQuestionService assessmentQuestionService) {
        this.assessmentQuestionMapper = assessmentQuestionMapper;
        this.assessmentQuestionService = assessmentQuestionService;
    }


    @GetMapping
    public List<AssessmentQuestionDto> listAssessmentQuestions(@PathVariable(name="behavioralIndicatorId") UUID behavioralIndicatorId) {
        List<AssessmentQuestion> assessmentQuestions = assessmentQuestionService.listAssessmentQuestions(behavioralIndicatorId);
        return assessmentQuestions.stream().map(assessmentQuestionMapper::toDto).toList();
    }
    
    @GetMapping("/{questionId}")
    public ResponseEntity<AssessmentQuestionDto> getQuestionById(
            @PathVariable(name="behavioralIndicatorId") UUID behavioralIndicatorId,
            @PathVariable(name="questionId") UUID questionId) {
        
        Optional<AssessmentQuestion> question = assessmentQuestionService.findAssesmentQuestionById(behavioralIndicatorId, questionId);
        return question.map(q -> ResponseEntity.ok(assessmentQuestionMapper.toDto(q)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public AssessmentQuestionDto createQuestion(
            @PathVariable(name="behavioralIndicatorId") UUID behavioralIndicatorId,
            @RequestBody AssessmentQuestion question) {
        
        AssessmentQuestion createdQuestion = assessmentQuestionService.createAssesmentQuestion(behavioralIndicatorId, question);
        return assessmentQuestionMapper.toDto(createdQuestion);
    }
    
    @PutMapping("/{questionId}")
    public ResponseEntity<AssessmentQuestionDto> updateQuestion(
            @PathVariable(name="behavioralIndicatorId") UUID behavioralIndicatorId,
            @PathVariable(name="questionId") UUID questionId,
            @RequestBody AssessmentQuestion question) {
        
        AssessmentQuestion updatedQuestion = assessmentQuestionService.updateAssesmentQuestion(
                behavioralIndicatorId, questionId, question);
                
        if (updatedQuestion != null) {
            return ResponseEntity.ok(assessmentQuestionMapper.toDto(updatedQuestion));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{questionId}")
    public ResponseEntity<Void> deleteQuestion(
            @PathVariable(name="behavioralIndicatorId") UUID behavioralIndicatorId,
            @PathVariable(name="questionId") UUID questionId) {
        
        assessmentQuestionService.deleteAssesmentQuestion(behavioralIndicatorId, questionId);
        return ResponseEntity.noContent().build();
    }
}
