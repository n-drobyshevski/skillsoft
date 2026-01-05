package app.skillsoft.assessmentbackend.domain.dto.sharing;

import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;
import app.skillsoft.assessmentbackend.domain.entities.SharePermission;
import app.skillsoft.assessmentbackend.domain.entities.TemplateShare;
import app.skillsoft.assessmentbackend.domain.entities.TestTemplate;
import app.skillsoft.assessmentbackend.domain.entities.User;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO representing a template shared with the current user.
 * Combines template summary with sharing metadata.
 * Matches frontend SharedTemplateItem interface.
 */
public record SharedTemplateItemDto(
        TemplateSummary template,
        SharePermission permission,
        SharedByInfo sharedBy,
        LocalDateTime sharedAt,
        LocalDateTime expiresAt,
        boolean isActive
) {
    /**
     * Embedded template summary info.
     */
    public record TemplateSummary(
            UUID id,
            String name,
            String description,
            AssessmentGoal goal,
            int competencyCount,
            int timeLimitMinutes,
            double passingScore,
            boolean isActive,
            LocalDateTime createdAt
    ) {
        /**
         * Create summary from TestTemplate entity.
         */
        public static TemplateSummary fromEntity(TestTemplate template) {
            int competencyCount = 0;

            // Extract competency count from competencyIds list
            if (template.getCompetencyIds() != null) {
                competencyCount = template.getCompetencyIds().size();
            }

            return new TemplateSummary(
                    template.getId(),
                    template.getName(),
                    template.getDescription(),
                    template.getGoal(),
                    competencyCount,
                    template.getTimeLimitMinutes() != null ? template.getTimeLimitMinutes() : 60,
                    template.getPassingScore() != null ? template.getPassingScore() : 70.0,
                    Boolean.TRUE.equals(template.getIsActive()),
                    template.getCreatedAt()
            );
        }
    }

    /**
     * Info about who shared the template.
     */
    public record SharedByInfo(
            UUID id,
            String name,
            String email,
            String avatarUrl
    ) {
        /**
         * Create from User entity.
         */
        public static SharedByInfo fromEntity(User user) {
            if (user == null) {
                return new SharedByInfo(null, "Unknown", null, null);
            }
            return new SharedByInfo(
                    user.getId(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getImageUrl()
            );
        }
    }

    /**
     * Create DTO from a TemplateShare entity.
     * Requires that template and grantedBy are eagerly loaded.
     */
    public static SharedTemplateItemDto fromEntity(TemplateShare share) {
        return new SharedTemplateItemDto(
                TemplateSummary.fromEntity(share.getTemplate()),
                share.getPermission(),
                SharedByInfo.fromEntity(share.getGrantedBy()),
                share.getGrantedAt(),
                share.getExpiresAt(),
                share.isActive()
        );
    }
}
