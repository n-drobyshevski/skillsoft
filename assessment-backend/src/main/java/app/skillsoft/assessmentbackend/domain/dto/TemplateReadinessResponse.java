package app.skillsoft.assessmentbackend.domain.dto;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO for template readiness check endpoint.
 *
 * Provides detailed information about whether a test template
 * can be started, including per-competency health status.
 */
public record TemplateReadinessResponse(
    boolean ready,
    String message,
    List<CompetencyReadiness> competencyReadiness,
    int totalQuestionsAvailable,
    int questionsRequired
) {
    /**
     * Creates a ready response with all competencies healthy.
     */
    public static TemplateReadinessResponse ready(
            List<CompetencyReadiness> competencies,
            int totalQuestions,
            int required) {
        return new TemplateReadinessResponse(
            true,
            "Template is ready to start",
            competencies,
            totalQuestions,
            required
        );
    }

    /**
     * Creates a not-ready response with competency issues.
     */
    public static TemplateReadinessResponse notReady(
            String message,
            List<CompetencyReadiness> competencies,
            int totalQuestions,
            int required) {
        return new TemplateReadinessResponse(
            false,
            message,
            competencies,
            totalQuestions,
            required
        );
    }

    /**
     * Readiness status for a single competency.
     */
    public record CompetencyReadiness(
        UUID competencyId,
        String competencyName,
        int questionsAvailable,
        int questionsRequired,
        String healthStatus,
        List<String> issues
    ) {
        /**
         * Creates a healthy competency readiness.
         */
        public static CompetencyReadiness healthy(
                UUID id,
                String name,
                int available,
                int required) {
            return new CompetencyReadiness(
                id, name, available, required, "HEALTHY", List.of()
            );
        }

        /**
         * Creates a competency readiness with issues.
         */
        public static CompetencyReadiness withIssues(
                UUID id,
                String name,
                int available,
                int required,
                String healthStatus,
                List<String> issues) {
            return new CompetencyReadiness(
                id, name, available, required, healthStatus, issues
            );
        }
    }
}
