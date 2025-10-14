package app.skillsoft.assessmentbackend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simple controller that provides health check endpoints for Docker
 */
@RestController
public class HealthController {

    /**
     * Root endpoint for Docker health checks
     * @return Simple message indicating the service is up
     */
    @GetMapping("/")
    public String healthCheck() {
        return "Backend service is up and running!";
    }
}
