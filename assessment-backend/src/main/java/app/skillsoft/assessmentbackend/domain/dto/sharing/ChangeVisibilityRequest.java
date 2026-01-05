package app.skillsoft.assessmentbackend.domain.dto.sharing;

import app.skillsoft.assessmentbackend.domain.entities.TemplateVisibility;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for changing a template's visibility setting.
 *
 * @param visibility The new visibility level (PUBLIC, PRIVATE, or LINK)
 */
public record ChangeVisibilityRequest(
        @NotNull(message = "Visibility is required")
        TemplateVisibility visibility
) {
    /**
     * Create a request to set visibility to PUBLIC.
     *
     * @return A change visibility request for PUBLIC
     */
    public static ChangeVisibilityRequest toPublic() {
        return new ChangeVisibilityRequest(TemplateVisibility.PUBLIC);
    }

    /**
     * Create a request to set visibility to PRIVATE.
     *
     * @return A change visibility request for PRIVATE
     */
    public static ChangeVisibilityRequest toPrivate() {
        return new ChangeVisibilityRequest(TemplateVisibility.PRIVATE);
    }

    /**
     * Create a request to set visibility to LINK.
     *
     * @return A change visibility request for LINK
     */
    public static ChangeVisibilityRequest toLink() {
        return new ChangeVisibilityRequest(TemplateVisibility.LINK);
    }
}
