package app.skillsoft.assessmentbackend.domain.dto.stats;

public record NavigationBadgeCountsDto(
    long inProgressTests,
    long sharedTemplates,
    long pendingCompetencies,
    long flaggedItems,
    long newUsers
) {}
