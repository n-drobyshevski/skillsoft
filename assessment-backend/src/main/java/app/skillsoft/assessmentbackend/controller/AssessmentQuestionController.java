package app.skillsoft.assessmentbackend.controller;


import app.skillsoft.assessmentbackend.domain.dto.AssessmentQuestionDto;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentQuestion;
import app.skillsoft.assessmentbackend.domain.mapper.AssessmentQuestionMapper;
import app.skillsoft.assessmentbackend.services.AssessmentQuestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST Controller for Assessment Question management.
 * 
 * Security:
 * - GET endpoints: All authenticated users (ROLE_USER)
 * - POST/PUT/DELETE: ADMIN or EDITOR role required
 */
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
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public AssessmentQuestionDto createQuestion(
            @RequestParam(name="behavioralIndicatorId") UUID behavioralIndicatorId,
            @RequestBody AssessmentQuestion question) {
        
        AssessmentQuestion createdQuestion = assessmentQuestionService.createAssesmentQuestion(behavioralIndicatorId, question);
        return assessmentQuestionMapper.toDto(createdQuestion);
    }
    
    @PutMapping("/{questionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<AssessmentQuestionDto> updateQuestion(
            @PathVariable(name="questionId") UUID questionId,
            @RequestBody AssessmentQuestion question) {
        
        AssessmentQuestion updatedQuestion = assessmentQuestionService.updateAssesmentQuestion(questionId, question);
                
        if (updatedQuestion != null) {
            return ResponseEntity.ok(assessmentQuestionMapper.toDto(updatedQuestion));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{questionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<Void> deleteQuestion(
            @PathVariable(name="questionId") UUID questionId) {
        
        assessmentQuestionService.deleteAssesmentQuestion(questionId);
        return ResponseEntity.noContent().build();
    }
}
