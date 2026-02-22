package app.skillsoft.assessmentbackend.services;

import app.skillsoft.assessmentbackend.domain.dto.stats.EntityStatsDto;
import app.skillsoft.assessmentbackend.domain.dto.stats.NavigationBadgeCountsDto;

public interface EntityStatsService {
    EntityStatsDto getEntityStats();

    NavigationBadgeCountsDto getNavigationBadgeCounts(String clerkUserId);
}
