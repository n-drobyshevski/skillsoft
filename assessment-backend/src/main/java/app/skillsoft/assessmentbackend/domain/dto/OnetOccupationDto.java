package app.skillsoft.assessmentbackend.domain.dto;

/**
 * Compact DTO for O*NET occupation search results.
 * Contains only essential fields for listing/selection purposes.
 */
public record OnetOccupationDto(
    String socCode,
    String occupationTitle,
    String description,
    int benchmarkCount
) {}
