package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.dto.blueprint.TestBlueprintDto;
import app.skillsoft.assessmentbackend.domain.dto.simulation.*;
import app.skillsoft.assessmentbackend.services.simulation.TestSimulatorService;
import app.skillsoft.assessmentbackend.services.validation.InventoryHeatmapService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for Blueprint Simulation and Inventory Management.
 * 
 * Provides endpoints for:
 * - Simulating test execution ("dry run")
 * - Fetching inventory heatmap for question availability
 * 
 * These endpoints support the Blueprint Canvas frontend.
 * 
 * API Base Path: /api/v1/tests
 */
@RestController
@RequestMapping("/api/v1/tests")
@RequiredArgsConstructor
@Slf4j
public class SimulationController {

    private final TestSimulatorService simulatorService;
    private final InventoryHeatmapService heatmapService;

    // ==================== SIMULATION ENDPOINTS ====================

    /**
     * Request DTO for simulation.
     */
    public record SimulateRequest(
        UUID templateId,
        TestBlueprintDto blueprint,
        SimulationProfile profile
    ) {}

    /**
     * Simulate test execution with the given blueprint configuration.
     * 
     * This is a "dry run" that validates the blueprint and shows:
     * - Question composition and distribution
     * - Estimated duration
     * - Simulated scores based on persona
     * - Inventory warnings
     * 
     * @param request The simulation request with blueprint and profile
     * @return Simulation results
     */
    @PostMapping("/templates/simulate")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<SimulationResultDto> simulateBlueprint(
            @RequestBody @Valid SimulateRequest request) {
        log.info("POST /api/v1/tests/templates/simulate - Profile: {}, TemplateId: {}", 
            request.profile(), request.templateId());

        if (request.blueprint() == null) {
            log.warn("Simulation request missing blueprint");
            return ResponseEntity.badRequest().body(
                SimulationResultDto.failed(List.of(
                    InventoryWarning.info("Blueprint is required")
                ))
            );
        }

        var result = simulatorService.simulate(request.blueprint(), request.profile());
        
        log.info("Simulation complete: valid={}, questions={}, warnings={}", 
            result.valid(), 
            result.sampleQuestions() != null ? result.sampleQuestions().size() : 0,
            result.warnings() != null ? result.warnings().size() : 0);

        return ResponseEntity.ok(result);
    }

    // ==================== INVENTORY ENDPOINTS ====================

    /**
     * Get inventory heatmap showing question availability health per competency.
     * 
     * Returns health status (CRITICAL, MODERATE, HEALTHY) for each competency
     * based on available question counts at different difficulty levels.
     * 
     * @return Inventory heatmap with health status per competency
     */
    @GetMapping("/inventory/heatmap")
    public ResponseEntity<InventoryHeatmapDto> getInventoryHeatmap() {
        log.info("GET /api/v1/tests/inventory/heatmap");

        var heatmap = heatmapService.generateHeatmap();
        
        log.info("Heatmap generated: {} competencies", 
            heatmap.competencyHealth() != null ? heatmap.competencyHealth().size() : 0);

        return ResponseEntity.ok(heatmap);
    }

    /**
     * Get inventory heatmap for specific competencies.
     * 
     * @param competencyIds List of competency UUIDs to analyze
     * @return Filtered inventory heatmap
     */
    @PostMapping("/inventory/heatmap")
    public ResponseEntity<InventoryHeatmapDto> getInventoryHeatmapFor(
            @RequestBody List<UUID> competencyIds) {
        log.info("POST /api/v1/tests/inventory/heatmap - {} competencies", 
            competencyIds != null ? competencyIds.size() : 0);

        var heatmap = heatmapService.generateHeatmapFor(competencyIds);

        return ResponseEntity.ok(heatmap);
    }

    /**
     * Simple response DTO for publish action.
     */
    public record PublishResponse(
        boolean published,
        int version,
        String message
    ) {}

    /**
     * Publish a template (locks the version and makes it available).
     * 
     * Note: This is a stub endpoint. Full implementation requires
     * the versioning logic in TestTemplateService.
     * 
     * @param templateId The template to publish
     * @return Publish result with version number
     */
    @PostMapping("/templates/{templateId}/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<PublishResponse> publishTemplate(
            @PathVariable UUID templateId) {
        log.info("POST /api/v1/tests/templates/{}/publish", templateId);

        // TODO: Implement full versioning logic via TestTemplateService
        // For now, return a successful response to unblock frontend
        
        return ResponseEntity.ok(new PublishResponse(true, 1, "Template published successfully"));
    }
}
