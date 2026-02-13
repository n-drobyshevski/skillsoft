package app.skillsoft.assessmentbackend.services.sharing;

import app.skillsoft.assessmentbackend.domain.dto.sharing.VisibilityInfoDto;
import app.skillsoft.assessmentbackend.domain.entities.TemplateVisibility;

import java.util.UUID;

/**
 * Service interface for managing template visibility settings.
 *
 * Provides operations for:
 * - Getting visibility information
 * - Changing visibility settings
 * - Enforcing visibility rules based on template status
 *
 * Visibility Rules:
 * - DRAFT templates can only be PRIVATE
 * - ARCHIVED templates force PRIVATE and revoke all links
 * - Changing visibility from LINK revokes all share links
 */
public interface TemplateVisibilityService {

    /**
     * Get visibility information for a template.
     *
     * @param templateId The template UUID
     * @return Visibility info including owner, visibility, and share counts
     * @throws IllegalArgumentException if template not found
     */
    VisibilityInfoDto getVisibilityInfo(UUID templateId);

    /**
     * Change the visibility setting for a template.
     *
     * Business rules:
     * - DRAFT templates cannot be PUBLIC or LINK
     * - ARCHIVED templates cannot change visibility
     * - Changing from LINK to another visibility revokes all share links
     *
     * @param templateId The template UUID
     * @param visibility The new visibility setting
     * @param changedByClerkId Clerk ID of the user making the change
     * @return Updated visibility info
     * @throws IllegalArgumentException if template not found
     * @throws IllegalStateException if visibility change violates business rules
     */
    VisibilityInfoDto changeVisibility(UUID templateId, TemplateVisibility visibility, String changedByClerkId);

    /**
     * Check if a template can change to a specific visibility.
     *
     * @param templateId The template UUID
     * @param targetVisibility The target visibility to check
     * @return true if the change is allowed
     */
    boolean canChangeToVisibility(UUID templateId, TemplateVisibility targetVisibility);

    /**
     * Get the current visibility for a template.
     *
     * @param templateId The template UUID
     * @return The current visibility, or null if template not found
     */
    TemplateVisibility getCurrentVisibility(UUID templateId);
}
