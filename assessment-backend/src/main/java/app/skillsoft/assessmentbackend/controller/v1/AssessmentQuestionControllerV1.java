package app.skillsoft.assessmentbackend.controller.v1;


import app.skillsoft.assessmentbackend.domain.dto.AssessmentQuestionDto;
import app.skillsoft.assessmentbackend.domain.dto.request.CreateQuestionRequest;
import app.skillsoft.assessmentbackend.domain.dto.request.UpdateQuestionRequest;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentQuestion;
import app.skillsoft.assessmentbackend.domain.mapper.AssessmentQuestionMapper;
import app.skillsoft.assessmentbackend.services.AssessmentQuestionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * V1 REST Controller for Assessment Question management.
 *
 * This is the standardized API version per docs/API_STANDARDIZATION_STRATEGY.md.
 * All endpoints use validated Request DTOs and follow consistent patterns.
 *
 * Security:
 * - GET endpoints: All authenticated users (ROLE_USER)
 * - POST/PUT/DELETE: ADMIN or EDITOR role required
 */
@RestController
@RequestMapping("/api/v1/questions")
public class AssessmentQuestionControllerV1 {

    private static final Logger logger = LoggerFactory.getLogger(AssessmentQuestionControllerV1.class);

    private final AssessmentQuestionMapper assessmentQuestionMapper;
    private final AssessmentQuestionService assessmentQuestionService;

    public AssessmentQuestionControllerV1(
            AssessmentQuestionMapper assessmentQuestionMapper,
            AssessmentQuestionService assessmentQuestionService) {
        this.assessmentQuestionMapper = assessmentQuestionMapper;
        this.assessmentQuestionService = assessmentQuestionService;
    }

    @GetMapping
    public List<AssessmentQuestionDto> listAssessmentQuestions() {
        logger.info("GET /api/v1/questions endpoint called");
        List<AssessmentQuestion> questions = assessmentQuestionService.listAllQuestions();
        logger.info("Found {} assessment questions", questions.size());
        return questions.stream().map(assessmentQuestionMapper::toDto).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssessmentQuestionDto> getQuestionById(
            @PathVariable("id") UUID id) {
        logger.info("GET /api/v1/questions/{} endpoint called", id);
        Optional<AssessmentQuestion> question = assessmentQuestionService.findAssesmentQuestionById(id);
        return question.map(q -> ResponseEntity.ok(assessmentQuestionMapper.toDto(q)))
                .orElseGet(() -> {
                    logger.warn("Assessment question with id {} not found", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<AssessmentQuestionDto> createQuestion(
            @Valid @RequestBody CreateQuestionRequest request) {
        logger.info("POST /api/v1/questions endpoint called");

        try {
            AssessmentQuestion questionEntity = assessmentQuestionMapper.fromCreateRequest(request);
            AssessmentQuestion createdQuestion = assessmentQuestionService.createAssesmentQuestion(
                    request.behavioralIndicatorId(), questionEntity);
            logger.info("Created assessment question with id: {}", createdQuestion.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(assessmentQuestionMapper.toDto(createdQuestion));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid assessment question data: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<AssessmentQuestionDto> updateQuestion(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateQuestionRequest request) {
        logger.info("PUT /api/v1/questions/{} endpoint called", id);

        AssessmentQuestion questionEntity = assessmentQuestionMapper.fromUpdateRequest(request);
        AssessmentQuestion updatedQuestion = assessmentQuestionService.updateAssesmentQuestion(id, questionEntity);

        if (updatedQuestion != null) {
            logger.info("Updated assessment question with id: {}", updatedQuestion.getId());
            return ResponseEntity.ok(assessmentQuestionMapper.toDto(updatedQuestion));
        } else {
            logger.warn("Assessment question with id {} not found for update", id);
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<Void> deleteQuestion(
            @PathVariable("id") UUID id) {
        logger.info("DELETE /api/v1/questions/{} endpoint called", id);

        try {
            assessmentQuestionService.deleteAssesmentQuestion(id);
            logger.info("Deleted assessment question with id: {}", id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                logger.warn("Assessment question with id {} not found for deletion", id);
                return ResponseEntity.notFound().build();
            }
            logger.error("Error deleting assessment question with id {}: {}", id, e.getMessage());
            throw e;
        }
    }
}
