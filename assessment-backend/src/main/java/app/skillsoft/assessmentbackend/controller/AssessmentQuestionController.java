package app.skillsoft.assessmentbackend.controller;


import app.skillsoft.assessmentbackend.domain.dto.AssessmentQuestionDto;
import app.skillsoft.assessmentbackend.domain.dto.request.CreateQuestionRequest;
import app.skillsoft.assessmentbackend.domain.dto.request.UpdateQuestionRequest;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentQuestion;
import app.skillsoft.assessmentbackend.domain.mapper.AssessmentQuestionMapper;
import app.skillsoft.assessmentbackend.services.AssessmentQuestionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST Controller for Assessment Question management.
 *
 * @deprecated This controller is deprecated and will be removed in a future version.
 *             Use {@link app.skillsoft.assessmentbackend.controller.v1.AssessmentQuestionControllerV1} instead
 *             with the endpoint path /api/v1/questions.
 *
 * Security:
 * - GET endpoints: All authenticated users (ROLE_USER)
 * - POST/PUT/DELETE: ADMIN or EDITOR role required
 */
@Deprecated(since = "1.0", forRemoval = true)
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
            @Valid @RequestBody CreateQuestionRequest request) {

        AssessmentQuestion questionEntity = assessmentQuestionMapper.fromCreateRequest(request);
        AssessmentQuestion createdQuestion = assessmentQuestionService.createAssesmentQuestion(
                request.behavioralIndicatorId(), questionEntity);
        return assessmentQuestionMapper.toDto(createdQuestion);
    }
    
    @PutMapping("/{questionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<AssessmentQuestionDto> updateQuestion(
            @PathVariable(name="questionId") UUID questionId,
            @Valid @RequestBody UpdateQuestionRequest request) {

        AssessmentQuestion questionEntity = assessmentQuestionMapper.fromUpdateRequest(request);
        AssessmentQuestion updatedQuestion = assessmentQuestionService.updateAssesmentQuestion(questionId, questionEntity);

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
