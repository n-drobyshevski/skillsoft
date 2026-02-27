package app.skillsoft.assessmentbackend.controller.v1;


import app.skillsoft.assessmentbackend.domain.dto.BehavioralIndicatorDto;
import app.skillsoft.assessmentbackend.domain.dto.CompetencyDto;
import app.skillsoft.assessmentbackend.domain.dto.IndicatorInventoryDto;
import app.skillsoft.assessmentbackend.domain.dto.request.CreateCompetencyRequest;
import app.skillsoft.assessmentbackend.domain.dto.request.UpdateCompetencyRequest;
import app.skillsoft.assessmentbackend.domain.entities.BehavioralIndicator;
import app.skillsoft.assessmentbackend.domain.entities.Competency;
import app.skillsoft.assessmentbackend.domain.mapper.BehavioralIndicatorMapper;
import app.skillsoft.assessmentbackend.domain.mapper.CompetencyMapper;
import app.skillsoft.assessmentbackend.services.BehavioralIndicatorService;
import app.skillsoft.assessmentbackend.services.CompetencyService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * V1 REST Controller for Competency management.
 *
 * This is the standardized API version per docs/API_STANDARDIZATION_STRATEGY.md.
 * All endpoints use validated Request DTOs and follow consistent patterns.
 *
 * Security:
 * - GET endpoints: All authenticated users (ROLE_USER)
 * - POST/PUT/DELETE: ADMIN or EDITOR role required
 */
@RestController
@RequestMapping("/api/v1/competencies")
public class CompetencyControllerV1 {

    private static final Logger logger = LoggerFactory.getLogger(CompetencyControllerV1.class);

    private final CompetencyService competencyService;
    private final BehavioralIndicatorService behavioralIndicatorService;
    private final CompetencyMapper competencyMapper;
    private final BehavioralIndicatorMapper behavioralIndicatorMapper;

    public CompetencyControllerV1(
            CompetencyService competencyService,
            BehavioralIndicatorService behavioralIndicatorService,
            CompetencyMapper competencyMapper,
            BehavioralIndicatorMapper behavioralIndicatorMapper) {
        this.competencyService = competencyService;
        this.behavioralIndicatorService = behavioralIndicatorService;
        this.competencyMapper = competencyMapper;
        this.behavioralIndicatorMapper = behavioralIndicatorMapper;
    }

    @GetMapping
    public ResponseEntity<List<CompetencyDto>> listCompetencies() {
        logger.info("GET /api/v1/competencies endpoint called");
        List<Competency> competencies = competencyService.listCompetencies();
        logger.info("Found {} competencies", competencies.size());
        List<CompetencyDto> dtos = competencies.stream().map(competencyMapper::toDto).toList();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES).cachePublic())
                .body(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompetencyDto> getCompetencyById(@PathVariable UUID id) {
        logger.info("GET /api/v1/competencies/{} endpoint called", id);
        return competencyService.findCompetencyById(id)
                .map(competency -> ResponseEntity.ok()
                        .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES).cachePublic())
                        .body(competencyMapper.toDto(competency)))
                .orElseGet(() -> {
                    logger.warn("Competency with id {} not found", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @GetMapping("/{competencyId}/behavioral-indicators")
    public ResponseEntity<List<BehavioralIndicatorDto>> listCompetencyBehavioralIndicators(
            @PathVariable("competencyId") UUID competencyId) {
        logger.info("GET /api/v1/competencies/{}/behavioral-indicators endpoint called", competencyId);
        List<BehavioralIndicator> indicators = behavioralIndicatorService.listCompetencyBehavioralIndicators(competencyId);
        logger.info("Found {} behavioral indicators for competency {}", indicators.size(), competencyId);
        List<BehavioralIndicatorDto> dtos = indicators.stream().map(behavioralIndicatorMapper::toDto).toList();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES).cachePublic())
                .body(dtos);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<CompetencyDto> createCompetency(
            @Valid @RequestBody CreateCompetencyRequest request) {
        logger.info("POST /api/v1/competencies endpoint called");

        try {
            Competency competencyEntity = competencyMapper.fromCreateRequest(request);
            Competency createdCompetency = competencyService.createCompetency(competencyEntity);
            logger.info("Created competency with id: {}", createdCompetency.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(competencyMapper.toDto(createdCompetency));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid competency data: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<CompetencyDto> updateCompetency(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCompetencyRequest request) {
        logger.info("PUT /api/v1/competencies/{} endpoint called", id);
        try {
            Competency competencyEntity = competencyMapper.fromUpdateRequest(request);
            Competency updatedCompetency = competencyService.updateCompetency(id, competencyEntity);
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

    @GetMapping("/{competencyId}/indicator-inventory")
    public ResponseEntity<IndicatorInventoryDto> getIndicatorInventory(
            @PathVariable UUID competencyId) {
        logger.info("GET /api/v1/competencies/{}/indicator-inventory", competencyId);
        return competencyService.findCompetencyById(competencyId)
                .map(comp -> {
                    IndicatorInventoryDto inventory = competencyService.getIndicatorInventory(competencyId);
                    return ResponseEntity.ok()
                            .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
                            .body(inventory);
                })
                .orElseGet(() -> {
                    logger.warn("Competency {} not found for indicator-inventory", competencyId);
                    return ResponseEntity.notFound().build();
                });
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<Void> deleteCompetency(@PathVariable UUID id) {
        logger.info("DELETE /api/v1/competencies/{} endpoint called", id);

        try {
            boolean deleted = competencyService.deleteCompetency(id);
            if (!deleted) {
                logger.warn("Competency with id {} not found for deletion", id);
                return ResponseEntity.notFound().build();
            }
            logger.info("Deleted competency with id: {}", id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            logger.error("Error deleting competency with id {}: {}", id, e.getMessage());
            throw e;
        }
    }
}
