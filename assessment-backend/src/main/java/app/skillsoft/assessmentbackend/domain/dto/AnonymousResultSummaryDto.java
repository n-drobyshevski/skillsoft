package app.skillsoft.assessmentbackend.domain.dto;

import app.skillsoft.assessmentbackend.domain.entities.AnonymousTakerInfo;
import app.skillsoft.assessmentbackend.domain.entities.TestResult;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Summary DTO for anonymous test results.
 * Used by template owners to view anonymous submissions.
 */
public record AnonymousResultSummaryDto(
        /**
         * Result UUID.
         */
        UUID resultId,

        /**
         * Session UUID.
         */
        UUID sessionId,

        /**
         * Taker's full display name.
         */
        String takerName,

        /**
         * Taker's email (may be null if not provided).
         */
        String takerEmail,

        /**
         * Overall percentage score (0-100).
         */
        Double overallPercentage,

        /**
         * Whether the taker passed.
         */
        Boolean passed,

        /**
         * When the test was completed.
         */
        LocalDateTime completedAt,

        /**
         * Label of the share link used (if set).
         */
        String shareLinkLabel,

        /**
         * Total time spent in seconds.
         */
        Integer totalTimeSeconds,

        /**
         * Number of questions answered.
         */
        Integer questionsAnswered,

        /**
         * Number of questions skipped.
         */
        Integer questionsSkipped
) {
    /**
     * Create from TestResult entity.
     *
     * @param result The test result entity
     * @return Summary DTO
     */
    public static AnonymousResultSummaryDto from(TestResult result) {
        AnonymousTakerInfo takerInfo = result.getAnonymousTakerInfo();
        String takerName = takerInfo != null ? takerInfo.getDisplayName() : "Anonymous";
        String takerEmail = takerInfo != null ? takerInfo.getEmail() : null;

        String shareLinkLabel = null;
        if (result.getSession() != null && result.getSession().getShareLink() != null) {
            shareLinkLabel = result.getSession().getShareLink().getLabel();
        }

        return new AnonymousResultSummaryDto(
                result.getId(),
                result.getSessionId(),
                takerName,
                takerEmail,
                result.getOverallPercentage(),
                result.getPassed(),
                result.getCompletedAt(),
                shareLinkLabel,
                result.getTotalTimeSeconds(),
                result.getQuestionsAnswered(),
                result.getQuestionsSkipped()
        );
    }
}
