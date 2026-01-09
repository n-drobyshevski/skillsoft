package app.skillsoft.assessmentbackend.domain.projections;

/**
 * Type-safe projection interface for user test statistics aggregate queries.
 *
 * This interface replaces the unsafe Object[] return type to prevent ClassCastException
 * issues caused by inconsistent Hibernate type handling of aggregate functions.
 *
 * JPQL aliases must match getter names (case-insensitive):
 * - COUNT(r) AS totalTests -> getTotalTests()
 * - COUNT(CASE WHEN r.passed...) AS passedTests -> getPassedTests()
 * - AVG(r.overallPercentage) AS averageScore -> getAverageScore()
 * - MAX(r.overallPercentage) AS bestScore -> getBestScore()
 */
public interface UserStatisticsProjection {

    /**
     * Total number of test results for the user.
     * @return count of test results, never null due to COUNT behavior
     */
    Long getTotalTests();

    /**
     * Number of passed tests for the user.
     * @return count of passed tests, never null due to COUNT behavior
     */
    Long getPassedTests();

    /**
     * Average score across all user's tests.
     * @return average percentage score, may be null if no results exist
     */
    Double getAverageScore();

    /**
     * Best (maximum) score across all user's tests.
     * @return maximum percentage score, may be null if no results exist
     */
    Double getBestScore();
}
