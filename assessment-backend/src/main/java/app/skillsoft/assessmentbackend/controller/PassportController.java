package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.dto.passport.CompetencyPassportDto;
import app.skillsoft.assessmentbackend.domain.dto.passport.PassportDtoMapper;
import app.skillsoft.assessmentbackend.domain.dto.passport.PassportValidityDto;
import app.skillsoft.assessmentbackend.services.external.PassportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Competency Passport operations.
 *
 * Exposes passport data for frontend consumption with score conversion
 * (1-5 backend scale to 0-100 frontend scale) and Big Five key mapping.
 */
@RestController
@RequestMapping("/api/v1/passports")
public class PassportController {

    private static final Logger logger = LoggerFactory.getLogger(PassportController.class);

    private final PassportService passportService;

    public PassportController(PassportService passportService) {
        this.passportService = passportService;
    }

    /**
     * Get competency passport for a user by Clerk ID.
     *
     * @param clerkUserId The Clerk user ID
     * @return Passport DTO with 0-100 scores, or 404 if no valid passport exists
     */
    @GetMapping("/user/{clerkUserId}")
    @PreAuthorize("@sessionSecurity.canAccessUserData(#clerkUserId)")
    public ResponseEntity<CompetencyPassportDto> getPassportByUser(
            @PathVariable String clerkUserId) {
        logger.info("GET /api/v1/passports/user/{}", clerkUserId);

        return passportService.getPassportDetailsByClerkUserId(clerkUserId)
            .map(details -> {
                CompetencyPassportDto dto = PassportDtoMapper.toDto(details);
                return ResponseEntity.ok(dto);
            })
            .orElseGet(() -> {
                logger.info("No valid passport found for user {}", clerkUserId);
                return ResponseEntity.notFound().build();
            });
    }

    /**
     * Check if a user has a valid (non-expired) passport.
     *
     * @param clerkUserId The Clerk user ID
     * @return { valid: true/false } — always returns 200
     */
    @GetMapping("/user/{clerkUserId}/valid")
    @PreAuthorize("@sessionSecurity.canAccessUserData(#clerkUserId)")
    public ResponseEntity<PassportValidityDto> hasValidPassport(
            @PathVariable String clerkUserId) {
        logger.info("GET /api/v1/passports/user/{}/valid", clerkUserId);

        boolean isValid = passportService.hasValidPassportByClerkUserId(clerkUserId);
        return ResponseEntity.ok(new PassportValidityDto(isValid));
    }
}
