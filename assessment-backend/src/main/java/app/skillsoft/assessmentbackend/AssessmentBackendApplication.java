package app.skillsoft.assessmentbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Main application entry point for SkillSoft Assessment Backend.
 *
 * Enables configuration properties scanning to automatically detect and bind
 * @ConfigurationProperties classes like ScoringConfiguration.
 */
@SpringBootApplication
@ConfigurationPropertiesScan("app.skillsoft.assessmentbackend.config")
public class AssessmentBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssessmentBackendApplication.class, args);
    }
}
