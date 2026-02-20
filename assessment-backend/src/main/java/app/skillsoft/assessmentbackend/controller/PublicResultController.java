package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.dto.PublicAnonymousResultDto;
import app.skillsoft.assessmentbackend.domain.entities.TestResult;
import app.skillsoft.assessmentbackend.repository.TestResultRepository;
import app.skillsoft.assessmentbackend.services.ResultTokenService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST Controller for publicly accessible anonymous test results.
 *
 * <p>Provides a single endpoint that validates an HMAC-signed token
 * and returns the result without requiring authentication. This enables
 * anonymous test takers to view their results via a shareable URL
 * after closing the original browser tab.</p>
 *
 * <p>Security: All access is controlled via HMAC-SHA256 signed tokens
 * with built-in expiry. No Clerk authentication required.</p>
 *
 * API Base Path: /api/v1/public/results
 */
@RestController
@RequestMapping("/api/v1/public/results")
@RequiredArgsConstructor
public class PublicResultController {

    private static final Logger logger = LoggerFactory.getLogger(PublicResultController.class);

    private final ResultTokenService resultTokenService;
    private final TestResultRepository resultRepository;

    /**
     * Get an anonymous test result by its HMAC-signed view token.
     *
     * @param token HMAC-signed Base64URL-encoded token
     * @return Public result DTO or 404/410 error
     */
    @GetMapping("/{token}")
    public ResponseEntity<PublicAnonymousResultDto> getResultByToken(@PathVariable String token) {
        logger.info("GET /api/v1/public/results/{}", token.substring(0, Math.min(8, token.length())) + "...");

        // Validate token and extract result ID
        UUID resultId = resultTokenService.validateToken(token);
        if (resultId == null) {
            logger.debug("Invalid or expired result token");
            return ResponseEntity.status(410).build(); // Gone — expired or invalid
        }

        // Fetch result with session and template eagerly loaded
        return resultRepository.findAnonymousByIdWithSessionAndTemplate(resultId)
                .map(result -> ResponseEntity.ok(PublicAnonymousResultDto.from(result)))
                .orElseGet(() -> {
                    logger.debug("Result not found for ID {}", resultId);
                    return ResponseEntity.notFound().build();
                });
    }
}
