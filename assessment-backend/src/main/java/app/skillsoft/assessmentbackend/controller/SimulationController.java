package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.dto.blueprint.TestBlueprintDto;
import app.skillsoft.assessmentbackend.domain.dto.simulation.*;
import app.skillsoft.assessmentbackend.services.TestTemplateService;
import app.skillsoft.assessmentbackend.services.simulation.TestSimulatorService;
import app.skillsoft.assessmentbackend.services.validation.InventoryHeatmapService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    private final TestTemplateService templateService;

    // ==================== SIMULATION ENDPOINTS ====================

    public record SimulateRequest(
        UUID templateId,
        TestBlueprintDto blueprint,
        SimulationProfile profile,
        Integer abilityLevel
    ) {}

    /**
     * Simulate test execution with the given blueprint configuration.
     *
     * This is a "dry run" that validates the blueprint and shows:
     * - Question composition and distribution
     * - Estimated duration
     * - Simulated scores based on persona and ability level
     * - Inventory warnings
     *
     * @param request The simulation request with blueprint, profile, and optional abilityLevel (0-100, default 50)
     * @return Simulation results
     */
    @PostMapping("/templates/simulate")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<SimulationResultDto> simulateBlueprint(
            @RequestBody @Valid SimulateRequest request) {
        log.info("POST /api/v1/tests/templates/simulate - Profile: {}, AbilityLevel: {}, TemplateId: {}",
            request.profile(), request.abilityLevel(), request.templateId());

        if (request.blueprint() == null) {
            log.warn("Simulation request missing blueprint");
            return ResponseEntity.badRequest().body(
                SimulationResultDto.failed(List.of(
                    InventoryWarning.info("Blueprint is required")
                ))
            );
        }

        if (request.abilityLevel() != null && (request.abilityLevel() < 0 || request.abilityLevel() > 100)) {
            return ResponseEntity.badRequest().body(
                SimulationResultDto.failed(List.of(
                    InventoryWarning.info("abilityLevel must be between 0 and 100")
                ))
            );
        }

        int resolvedAbility = request.abilityLevel() != null ? request.abilityLevel() : 50;
        var result = simulatorService.simulate(request.blueprint(), request.profile(), resolvedAbility);

        log.info("Simulation complete: valid={}, questions={}, warnings={}",
            result.valid(),
            result.sampleQuestions() != null ? result.sampleQuestions().size() : 0,
            result.warnings() != null ? result.warnings().size() : 0);

        return ResponseEntity.ok(result);
    }

    // ==================== INVENTORY ENDPOINTS ====================

    @GetMapping("/inventory/heatmap")
    public ResponseEntity<InventoryHeatmapDto> getInventoryHeatmap() {
        log.info("GET /api/v1/tests/inventory/heatmap");

        var heatmap = heatmapService.generateHeatmap();

        log.info("Heatmap generated: {} competencies",
            heatmap.competencyHealth() != null ? heatmap.competencyHealth().size() : 0);

        return ResponseEntity.ok(heatmap);
    }

    @PostMapping("/inventory/heatmap")
    public ResponseEntity<InventoryHeatmapDto> getInventoryHeatmapFor(
            @RequestBody List<UUID> competencyIds) {
        log.info("POST /api/v1/tests/inventory/heatmap - {} competencies",
            competencyIds != null ? competencyIds.size() : 0);

        var heatmap = heatmapService.generateHeatmapFor(competencyIds);

        return ResponseEntity.ok(heatmap);
    }

    public record PublishResponse(
        boolean published,
        int version,
        String message
    ) {}

    @PostMapping("/templates/{templateId}/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<PublishResponse> publishTemplate(
            @PathVariable UUID templateId) {
        log.info("POST /api/v1/tests/templates/{}/publish", templateId);

        TestTemplateService.PublishResult result = templateService.publishTemplate(templateId);

        log.info("Template {} publish result: published={}, version={}",
                templateId, result.published(), result.version());

        return ResponseEntity.ok(new PublishResponse(
                result.published(),
                result.version(),
                result.message()
        ));
    }
}
