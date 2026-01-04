package app.skillsoft.assessmentbackend.domain.dto.activity;

import app.skillsoft.assessmentbackend.domain.entities.SessionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Parameters for filtering activity queries.
 * Used for template-specific activity filtering with pagination.
 */
public record ActivityFilterParams(
        UUID templateId,
        String clerkUserId,
        SessionStatus status,
        Boolean passed,
        LocalDateTime fromDate,
        LocalDateTime toDate,
        int page,
        int size
) {
    /**
     * Default page size for activity queries.
     */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * Maximum page size to prevent excessive queries.
     */
    public static final int MAX_PAGE_SIZE = 100;

    /**
     * Factory method for dashboard recent activity (no filters).
     */
    public static ActivityFilterParams forRecent(int limit) {
        return new ActivityFilterParams(
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                Math.min(limit, MAX_PAGE_SIZE)
        );
    }

    /**
     * Factory method for template-specific activity with optional filters.
     */
    public static ActivityFilterParams forTemplate(
            UUID templateId,
            SessionStatus status,
            Boolean passed,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            int page,
            int size
    ) {
        return new ActivityFilterParams(
                templateId,
                null,
                status,
                passed,
                fromDate,
                toDate,
                page,
                Math.min(size, MAX_PAGE_SIZE)
        );
    }

    /**
     * Returns validated page size (between 1 and MAX_PAGE_SIZE).
     */
    public int getValidatedSize() {
        if (size <= 0) return DEFAULT_PAGE_SIZE;
        return Math.min(size, MAX_PAGE_SIZE);
    }

    /**
     * Returns validated page number (non-negative).
     */
    public int getValidatedPage() {
        return Math.max(page, 0);
    }

    /**
     * Checks if any filters are applied.
     */
    public boolean hasFilters() {
        return status != null || passed != null || fromDate != null || toDate != null;
    }
}
