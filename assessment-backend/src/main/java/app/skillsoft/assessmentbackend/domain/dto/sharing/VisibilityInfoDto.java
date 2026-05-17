package app.skillsoft.assessmentbackend.domain.dto.sharing;

import app.skillsoft.assessmentbackend.domain.entities.TemplateStatus;
import app.skillsoft.assessmentbackend.domain.entities.TemplateVisibility;
import app.skillsoft.assessmentbackend.domain.entities.TestTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for template visibility information.
 *
 * @param templateId The template UUID
 * @param visibility The current visibility setting
 * @param visibilityChangedAt When the visibility was last changed
 * @param ownerId The owner's UUID
 * @param ownerName The owner's display name
 * @param activeSharesCount Number of active user/team shares
 * @param activeLinksCount Number of active share links
 * @param templateStatus The template lifecycle status (DRAFT, PUBLISHED, ARCHIVED)
 */
public record VisibilityInfoDto(
        UUID templateId,
        TemplateVisibility visibility,
        LocalDateTime visibilityChangedAt,
        UUID ownerId,
        String ownerName,
        long activeSharesCount,
        long activeLinksCount,
        TemplateStatus templateStatus
) {
    /**
     * Create a VisibilityInfoDto from a template entity with counts.
     *
     * @param template The template entity
     * @param activeSharesCount The count of active shares
     * @param activeLinksCount The count of active links
     * @return The DTO
     */
    public static VisibilityInfoDto fromTemplate(TestTemplate template, long activeSharesCount, long activeLinksCount) {
        String ownerName = null;
        UUID ownerId = null;

        if (template.getOwner() != null) {
            ownerId = template.getOwner().getId();
            ownerName = template.getOwner().getFullName();
        }

        return new VisibilityInfoDto(
                template.getId(),
                template.getVisibility(),
                template.getVisibilityChangedAt(),
                ownerId,
                ownerName,
                activeSharesCount,
                activeLinksCount,
                template.getStatus()
        );
    }
}
