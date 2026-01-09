package app.skillsoft.assessmentbackend.domain.projections;

/**
 * Type-safe projection interface for template test statistics aggregate queries.
 *
 * This interface replaces the unsafe Object[] return type to prevent ClassCastException
 * issues caused by inconsistent Hibernate type handling of aggregate functions.
 *
 * JPQL aliases must match getter names (case-insensitive):
 * - COUNT(r) AS totalAttempts -> getTotalAttempts()
 * - COUNT(CASE WHEN r.passed...) AS passedCount -> getPassedCount()
 * - AVG(r.overallPercentage) AS averageScore -> getAverageScore()
 * - MIN(r.overallPercentage) AS minScore -> getMinScore()
 * - MAX(r.overallPercentage) AS maxScore -> getMaxScore()
 */
public interface TemplateStatisticsProjection {

    /**
     * Total number of test attempts for this template.
     * @return count of test attempts, never null due to COUNT behavior
     */
    Long getTotalAttempts();

    /**
     * Number of passed test attempts for this template.
     * @return count of passed attempts, never null due to COUNT behavior
     */
    Long getPassedCount();

    /**
     * Average score across all attempts on this template.
     * @return average percentage score, may be null if no results exist
     */
    Double getAverageScore();

    /**
     * Minimum score across all attempts on this template.
     * @return minimum percentage score, may be null if no results exist
     */
    Double getMinScore();

    /**
     * Maximum score across all attempts on this template.
     * @return maximum percentage score, may be null if no results exist
     */
    Double getMaxScore();
}
