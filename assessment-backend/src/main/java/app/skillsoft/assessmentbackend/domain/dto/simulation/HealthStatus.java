package app.skillsoft.assessmentbackend.domain.dto.simulation;

/**
 * Health status for question inventory per competency/difficulty.
 * Used by InventoryHeatmapService to indicate question availability.
 */
public enum HealthStatus {
    /**
     * Critical: Less than 3 questions available.
     * May not have enough variety for valid assessments.
     */
    CRITICAL("Critical", "Less than 3 questions - insufficient for assessment"),
    
    /**
     * Moderate: 3-5 questions available.
     * Minimum viable but could benefit from more questions.
     */
    MODERATE("Moderate", "3-5 questions - acceptable but limited variety"),
    
    /**
     * Healthy: More than 5 questions available.
     * Good variety for reliable assessments.
     */
    HEALTHY("Healthy", "More than 5 questions - good variety available");

    private final String displayName;
    private final String description;

    HealthStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Calculate health status based on question count.
     * 
     * @param count Number of questions available
     * @return Appropriate HealthStatus
     */
    public static HealthStatus fromCount(long count) {
        return switch ((int) Math.min(count, Integer.MAX_VALUE)) {
            case 0, 1, 2 -> CRITICAL;
            case 3, 4, 5 -> MODERATE;
            default -> HEALTHY;
        };
    }
}
