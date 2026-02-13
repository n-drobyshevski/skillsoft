package app.skillsoft.assessmentbackend.domain.projections;

/**
 * Type-safe projection interface for template score and time aggregate queries.
 *
 * This interface replaces the unsafe Object[] return type to prevent ClassCastException
 * issues caused by inconsistent Hibernate type handling of aggregate functions.
 *
 * JPQL aliases must match getter names (case-insensitive):
 * - SUM(r.overallPercentage) AS totalScore -> getTotalScore()
 * - SUM(r.totalTimeSeconds) AS totalTimeSeconds -> getTotalTimeSeconds()
 * - COUNT(r) AS resultCount -> getResultCount()
 */
public interface TemplateScoreTimeProjection {

    /**
     * Sum of all scores for this template.
     * @return total score sum, may be null if no results exist
     */
    Double getTotalScore();

    /**
     * Sum of all time spent on this template in seconds.
     * @return total time in seconds, may be null if no results exist
     */
    Long getTotalTimeSeconds();

    /**
     * Count of results for this template.
     * @return count of results, never null due to COUNT behavior
     */
    Long getResultCount();
}
