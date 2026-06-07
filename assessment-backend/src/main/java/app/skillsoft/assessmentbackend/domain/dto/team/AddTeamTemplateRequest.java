package app.skillsoft.assessmentbackend.domain.dto.team;

import app.skillsoft.assessmentbackend.domain.entities.SharePermission;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Request DTO for granting a team access to a test template
 * from the admin team management view ("Tests" tab).
 *
 * <p>The team is supplied via the path; this body carries the template
 * to share, the permission level to grant, and an optional expiration.</p>
 *
 * @param templateId The test template to grant the team access to
 * @param permission The permission level to grant (defaults to VIEW when null)
 * @param expiresAt  Optional expiration date (null for no expiration)
 */
public record AddTeamTemplateRequest(
        @NotNull(message = "templateId is required")
        UUID templateId,
        SharePermission permission,
        LocalDateTime expiresAt
) {
    /**
     * Resolve the permission, defaulting to VIEW when not provided.
     */
    public SharePermission permissionOrDefault() {
        return permission != null ? permission : SharePermission.VIEW;
    }
}
