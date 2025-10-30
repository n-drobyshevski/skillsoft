package app.skillsoft.assessmentbackend.controller;


import app.skillsoft.assessmentbackend.domain.dto.AssessmentQuestionDto;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentQuestion;
import app.skillsoft.assessmentbackend.domain.mapper.AssessmentQuestionMapper;
import app.skillsoft.assessmentbackend.services.AssessmentQuestionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/questions")
public class AssessmentQuestionController {

    private final AssessmentQuestionMapper assessmentQuestionMapper;
    private final AssessmentQuestionService assessmentQuestionService;

    public AssessmentQuestionController(AssessmentQuestionMapper assessmentQuestionMapper, 
                                        AssessmentQuestionService assessmentQuestionService) {
        this.assessmentQuestionMapper = assessmentQuestionMapper;
        this.assessmentQuestionService = assessmentQuestionService;
    }

    @GetMapping
    public List<AssessmentQuestionDto> listAssessmentQuestions() {
        List<AssessmentQuestion> competencies = assessmentQuestionService.listAllQuestions();
        return competencies.stream().map(assessmentQuestionMapper::toDto).toList();
    }

    
    @GetMapping("/{questionId}")
    public ResponseEntity<AssessmentQuestionDto> getQuestionById(
            @PathVariable(name="questionId") UUID questionId) {
        
        Optional<AssessmentQuestion> question = assessmentQuestionService.findAssesmentQuestionById(questionId);
        return question.map(q -> ResponseEntity.ok(assessmentQuestionMapper.toDto(q)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<AssessmentQuestionDto> createQuestion(
            @RequestParam(name="behavioralIndicatorId") UUID behavioralIndicatorId,
            @RequestBody AssessmentQuestionDto questionDto) {
        try {
            AssessmentQuestion question = assessmentQuestionMapper.fromDto(questionDto);
            AssessmentQuestion createdQuestion = assessmentQuestionService.createAssesmentQuestion(behavioralIndicatorId, question);
            return ResponseEntity.status(HttpStatus.CREATED).body(assessmentQuestionMapper.toDto(createdQuestion));
        } catch (RuntimeException e) {
            // e.g. Behavioral Indicator not found
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/{questionId}")
    public ResponseEntity<AssessmentQuestionDto> updateQuestion(
            @PathVariable(name="questionId") UUID questionId,
            @RequestBody AssessmentQuestionDto questionDto) {
        AssessmentQuestion question = assessmentQuestionMapper.fromDto(questionDto);
        AssessmentQuestion updatedQuestion = assessmentQuestionService.updateAssesmentQuestion(questionId, question);
        return ResponseEntity.ok(assessmentQuestionMapper.toDto(updatedQuestion));
    }
    
    @DeleteMapping("/{questionId}")
    public ResponseEntity<Void> deleteQuestion(
            @PathVariable(name="questionId") UUID questionId) {
        
        assessmentQuestionService.deleteAssesmentQuestion(questionId);
        return ResponseEntity.noContent().build();
    }
}
