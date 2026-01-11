package app.skillsoft.assessmentbackend.domain.dto;

import app.skillsoft.assessmentbackend.domain.entities.AnonymousTakerInfo;
import app.skillsoft.assessmentbackend.domain.entities.TestResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Detailed DTO for anonymous test result.
 * Used by template owners to view full result details.
 */
public record AnonymousResultDetailDto(
        /**
         * Result UUID.
         */
        UUID id,

        /**
         * Session UUID.
         */
        UUID sessionId,

        /**
         * Template UUID.
         */
        UUID templateId,

        /**
         * Template name.
         */
        String templateName,

        /**
         * Overall percentage score (0-100).
         */
        Double overallPercentage,

        /**
         * Percentile ranking.
         */
        Integer percentile,

        /**
         * Whether the taker passed.
         */
        Boolean passed,

        /**
         * Per-competency score breakdowns.
         */
        List<CompetencyScoreDto> competencyScores,

        /**
         * Big Five personality profile (for TEAM_FIT goal).
         */
        Map<String, Double> bigFiveProfile,

        /**
         * Taker information.
         */
        TakerInfoDto takerInfo,

        /**
         * Session metadata.
         */
        SessionMetadataDto sessionMetadata
) {
    /**
     * Taker information DTO.
     */
    public record TakerInfoDto(
            String firstName,
            String lastName,
            String email,
            String notes,
            LocalDateTime collectedAt
    ) {
        public static TakerInfoDto from(AnonymousTakerInfo info) {
            if (info == null) return null;
            return new TakerInfoDto(
                    info.getFirstName(),
                    info.getLastName(),
                    info.getEmail(),
                    info.getNotes(),
                    info.getCollectedAt()
            );
        }
    }

    /**
     * Session metadata DTO.
     */
    public record SessionMetadataDto(
            String ipAddress,
            LocalDateTime startedAt,
            LocalDateTime completedAt,
            Integer totalTimeSeconds,
            String shareLinkLabel
    ) {
    }

    /**
     * Create from TestResult entity.
     *
     * @param result The test result entity
     * @return Detailed DTO
     */
    public static AnonymousResultDetailDto from(TestResult result) {
        var session = result.getSession();
        var template = session.getTemplate();
        var shareLink = session.getShareLink();

        SessionMetadataDto metadata = new SessionMetadataDto(
                session.getIpAddress(),
                session.getStartedAt(),
                session.getCompletedAt(),
                result.getTotalTimeSeconds(),
                shareLink != null ? shareLink.getLabel() : null
        );

        return new AnonymousResultDetailDto(
                result.getId(),
                session.getId(),
                template.getId(),
                template.getName(),
                result.getOverallPercentage(),
                result.getPercentile(),
                result.getPassed(),
                result.getCompetencyScores(),
                result.getBigFiveProfile(),
                TakerInfoDto.from(result.getAnonymousTakerInfo()),
                metadata
        );
    }
}
