package app.skillsoft.assessmentbackend.domain.dto;

/**
 * Request body for creating a new version of an existing template.
 *
 * @param archiveOriginal If true, the original template is archived after
 *                        creating the new draft version. If false, it stays
 *                        in its current status.
 */
public record CreateVersionRequest(
        boolean archiveOriginal
) {}
