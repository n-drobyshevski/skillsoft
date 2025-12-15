package app.skillsoft.assessmentbackend.exception;

import java.util.List;
import java.util.UUID;

/**
 * Exception thrown when a test template cannot be started because
 * one or more competencies lack sufficient questions.
 *
 * This exception carries detailed information about which competencies
 * have issues to help HR administrators fix the problem.
 */
public class TestNotReadyException extends RuntimeException {

    private final UUID templateId;
    private final List<CompetencyIssue> competencyIssues;
    private final int totalQuestionsAvailable;
    private final int questionsRequired;

    /**
     * Details about a specific competency's readiness issues.
     */
    public record CompetencyIssue(
        UUID competencyId,
        String competencyName,
        int questionsAvailable,
        int questionsRequired,
        String healthStatus,
        List<String> issues
    ) {}

    /**
     * Constructs a new test not ready exception.
     *
     * @param templateId The template ID that cannot be started
     * @param competencyIssues List of competencies with issues
     * @param totalQuestionsAvailable Total questions found
     * @param questionsRequired Minimum questions needed
     */
    public TestNotReadyException(
            UUID templateId,
            List<CompetencyIssue> competencyIssues,
            int totalQuestionsAvailable,
            int questionsRequired) {
        super(buildMessage(competencyIssues, totalQuestionsAvailable, questionsRequired));
        this.templateId = templateId;
        this.competencyIssues = competencyIssues;
        this.totalQuestionsAvailable = totalQuestionsAvailable;
        this.questionsRequired = questionsRequired;
    }

    private static String buildMessage(
            List<CompetencyIssue> issues,
            int available,
            int required) {
        if (issues == null || issues.isEmpty()) {
            return String.format(
                "Cannot start test session: No questions available (found %d, need %d). " +
                "Please ensure competencies have behavioral indicators with active questions.",
                available, required
            );
        }

        long criticalCount = issues.stream()
            .filter(i -> "CRITICAL".equals(i.healthStatus()))
            .count();

        if (criticalCount > 0) {
            return String.format(
                "Cannot start test session: %d competenc%s missing questions. " +
                "Please add assessment questions to the affected competencies.",
                criticalCount,
                criticalCount == 1 ? "y is" : "ies are"
            );
        }

        return String.format(
            "Cannot start test session: Insufficient questions available (found %d, need %d).",
            available, required
        );
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public List<CompetencyIssue> getCompetencyIssues() {
        return competencyIssues;
    }

    public int getTotalQuestionsAvailable() {
        return totalQuestionsAvailable;
    }

    public int getQuestionsRequired() {
        return questionsRequired;
    }
}
