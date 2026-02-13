package app.skillsoft.assessmentbackend.domain.dto.sharing;

/**
 * Simple count response for shared-with-me badge/counter.
 * Used by navigation UI to display badge.
 */
public record SharedWithMeCountDto(long count) {
    /**
     * Create count response.
     */
    public static SharedWithMeCountDto of(long count) {
        return new SharedWithMeCountDto(count);
    }
}
