package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.dto.OnetOccupationDto;
import app.skillsoft.assessmentbackend.services.external.OnetService;
import app.skillsoft.assessmentbackend.services.external.OnetService.OnetProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for O*NET occupation data.
 *
 * Provides endpoints for searching and retrieving O*NET occupation profiles
 * used in JOB_FIT assessments for benchmark-based scoring.
 *
 * API Base Path: /api/v1/onet
 */
@RestController
@RequestMapping("/api/v1/onet")
public class OnetController {

    private static final Logger logger = LoggerFactory.getLogger(OnetController.class);

    private final OnetService onetService;

    public OnetController(OnetService onetService) {
        this.onetService = onetService;
    }

    /**
     * Search O*NET occupation profiles by keyword.
     * Matches against occupation title and description (case-insensitive).
     *
     * @param keyword Search keyword (required)
     * @return List of matching occupations as compact DTOs (up to 10 results)
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<List<OnetOccupationDto>> searchOccupations(
            @RequestParam String keyword) {
        logger.info("GET /api/v1/onet/search?keyword={}", keyword);

        List<OnetProfile> profiles = onetService.searchProfiles(keyword);
        List<OnetOccupationDto> results = profiles.stream()
            .map(p -> new OnetOccupationDto(
                p.socCode(),
                p.occupationTitle(),
                p.description(),
                p.benchmarks().size()
            ))
            .toList();

        logger.info("Found {} O*NET occupations matching keyword '{}'", results.size(), keyword);
        return ResponseEntity.ok(results);
    }

    /**
     * Get full O*NET profile for a specific SOC code.
     *
     * @param socCode Standard Occupational Classification code (e.g., "15-1252.00")
     * @return Full occupation profile or 404 if not found
     */
    @GetMapping("/profiles/{socCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<OnetProfile> getProfile(@PathVariable String socCode) {
        logger.info("GET /api/v1/onet/profiles/{}", socCode);

        return onetService.getProfile(socCode)
            .map(profile -> {
                logger.info("Found O*NET profile: {} ({})", profile.occupationTitle(), socCode);
                return ResponseEntity.ok(profile);
            })
            .orElseGet(() -> {
                logger.warn("O*NET profile not found for SOC code: {}", socCode);
                return ResponseEntity.notFound().build();
            });
    }

    /**
     * Validate whether a SOC code exists and has profile data.
     *
     * @param socCode Standard Occupational Classification code to validate
     * @return true if valid and has data, false otherwise
     */
    @GetMapping("/validate/{socCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<Boolean> validateSocCode(@PathVariable String socCode) {
        logger.info("GET /api/v1/onet/validate/{}", socCode);

        boolean valid = onetService.isValidSocCode(socCode);
        logger.info("SOC code {} is {}", socCode, valid ? "valid" : "invalid");

        return ResponseEntity.ok(valid);
    }
}
