package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.dto.stats.EntityStatsDto;
import app.skillsoft.assessmentbackend.services.EntityStatsService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/stats")
public class EntityStatsController {

    private final EntityStatsService entityStatsService;

    public EntityStatsController(EntityStatsService entityStatsService) {
        this.entityStatsService = entityStatsService;
    }

    @GetMapping("/entities")
    public ResponseEntity<EntityStatsDto> getEntityStats() {
        EntityStatsDto stats = entityStatsService.getEntityStats();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS).mustRevalidate())
                .body(stats);
    }
}
