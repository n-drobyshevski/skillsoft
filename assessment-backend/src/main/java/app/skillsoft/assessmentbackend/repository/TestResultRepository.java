package app.skillsoft.assessmentbackend.repository;

import app.skillsoft.assessmentbackend.domain.entities.ResultStatus;
import app.skillsoft.assessmentbackend.domain.entities.TestResult;
import app.skillsoft.assessmentbackend.domain.projections.TemplateStatisticsProjection;
import app.skillsoft.assessmentbackend.domain.projections.UserStatisticsProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestResultRepository extends JpaRepository<TestResult, UUID>, JpaSpecificationExecutor<TestResult> {

    /**
     * Find result by session ID
     */
    Optional<TestResult> findBySession_Id(UUID sessionId);

    /**
     * Find result by session ID and status.
     * Used for idempotency checks to detect already-completed scoring.
     *
     * @param sessionId The session UUID
     * @param status    The result status to filter by
     * @return Optional containing the result if found with the given status
     */
    Optional<TestResult> findBySession_IdAndStatus(UUID sessionId, ResultStatus status);

    /**
     * Find all results for a user
     */
    List<TestResult> findByClerkUserId(String clerkUserId);

    /**
     * Find all results for a user with pagination
     */
    Page<TestResult> findByClerkUserId(String clerkUserId, Pageable pageable);

    /**
     * Find results for a user ordered by completion date (most recent first)
     */
    List<TestResult> findByClerkUserIdOrderByCompletedAtDesc(String clerkUserId);

    /**
     * Find user's results for a specific template
     */
    @Query("SELECT r FROM TestResult r WHERE r.clerkUserId = :userId AND r.session.template.id = :templateId ORDER BY r.completedAt DESC")
    List<TestResult> findByUserAndTemplate(
            @Param("userId") String clerkUserId, 
            @Param("templateId") UUID templateId);

    /**
     * Find the most recent result for a user on a specific template
     */
    @Query("SELECT r FROM TestResult r WHERE r.clerkUserId = :userId AND r.session.template.id = :templateId ORDER BY r.completedAt DESC LIMIT 1")
    Optional<TestResult> findLatestByUserAndTemplate(
            @Param("userId") String clerkUserId, 
            @Param("templateId") UUID templateId);

    /**
     * Find all passed results for a user
     */
    List<TestResult> findByClerkUserIdAndPassedTrue(String clerkUserId);

    /**
     * Find all failed results for a user
     */
    List<TestResult> findByClerkUserIdAndPassedFalse(String clerkUserId);

    /**
     * Count passed tests for a user
     */
    long countByClerkUserIdAndPassedTrue(String clerkUserId);

    /**
     * Count total tests for a user
     */
    long countByClerkUserId(String clerkUserId);

    /**
     * Find results within a date range (for reporting)
     */
    List<TestResult> findByCompletedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find results within a date range with session and template eagerly loaded (paginated).
     */
    @Query(value = "SELECT r FROM TestResult r JOIN FETCH r.session s JOIN FETCH s.template WHERE r.completedAt BETWEEN :startDate AND :endDate",
           countQuery = "SELECT COUNT(r) FROM TestResult r WHERE r.completedAt BETWEEN :startDate AND :endDate")
    Page<TestResult> findByCompletedAtBetweenWithSessionAndTemplate(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * Find results for a template (for template statistics)
     */
    @Query("SELECT r FROM TestResult r WHERE r.session.template.id = :templateId")
    List<TestResult> findByTemplateId(@Param("templateId") UUID templateId);

    /**
     * Calculate average score for a template
     */
    @Query("SELECT AVG(r.overallPercentage) FROM TestResult r WHERE r.session.template.id = :templateId")
    Double calculateAverageScoreByTemplateId(@Param("templateId") UUID templateId);

    /**
     * Calculate pass rate for a template
     */
    @Query("SELECT (COUNT(CASE WHEN r.passed = true THEN 1 END) * 100.0 / COUNT(*)) FROM TestResult r WHERE r.session.template.id = :templateId")
    Double calculatePassRateByTemplateId(@Param("templateId") UUID templateId);

    /**
     * Count results with score below a threshold (for percentile calculation)
     */
    @Query("SELECT COUNT(r) FROM TestResult r WHERE r.session.template.id = :templateId AND r.overallPercentage < :score")
    long countResultsBelowScore(@Param("templateId") UUID templateId, @Param("score") Double score);

    /**
     * Count total results for a template (for percentile calculation)
     */
    @Query("SELECT COUNT(r) FROM TestResult r WHERE r.session.template.id = :templateId")
    long countResultsByTemplateId(@Param("templateId") UUID templateId);

    /**
     * Find best result for a user across all templates
     */
    Optional<TestResult> findTopByClerkUserIdOrderByOverallPercentageDesc(String clerkUserId);

    /**
     * Find recent results (for dashboard)
     */
    List<TestResult> findTop10ByOrderByCompletedAtDesc();

    // ============================================
    // OPTIMIZED QUERIES (N+1 Prevention)
    // ============================================

    /**
     * Find result by ID with session and template eagerly loaded.
     */
    @Query("SELECT r FROM TestResult r JOIN FETCH r.session s JOIN FETCH s.template WHERE r.id = :resultId")
    Optional<TestResult> findByIdWithSessionAndTemplate(@Param("resultId") UUID resultId);

    /**
     * Find result by session ID with template eagerly loaded.
     */
    @Query("SELECT r FROM TestResult r JOIN FETCH r.session s JOIN FETCH s.template WHERE s.id = :sessionId")
    Optional<TestResult> findBySessionIdWithTemplate(@Param("sessionId") UUID sessionId);

    /**
     * Find results for a user with session and template eagerly loaded.
     */
    @Query("SELECT r FROM TestResult r JOIN FETCH r.session s JOIN FETCH s.template WHERE r.clerkUserId = :userId ORDER BY r.completedAt ASC")
    List<TestResult> findByClerkUserIdWithSessionAndTemplate(@Param("userId") String clerkUserId);

    /**
     * Find results for a user with session and template eagerly loaded (paginated).
     */
    @Query(value = "SELECT r FROM TestResult r JOIN FETCH r.session s JOIN FETCH s.template WHERE r.clerkUserId = :userId",
           countQuery = "SELECT COUNT(r) FROM TestResult r WHERE r.clerkUserId = :userId")
    Page<TestResult> findByClerkUserIdWithSessionAndTemplate(@Param("userId") String clerkUserId, Pageable pageable);

    /**
     * Find passed results for a user with session and template eagerly loaded.
     */
    @Query("SELECT r FROM TestResult r JOIN FETCH r.session s JOIN FETCH s.template WHERE r.clerkUserId = :userId AND r.passed = true ORDER BY r.completedAt DESC")
    List<TestResult> findPassedByClerkUserIdWithSessionAndTemplate(@Param("userId") String clerkUserId);

    /**
     * Find passed results for a user with session and template eagerly loaded (paginated).
     */
    @Query(value = "SELECT r FROM TestResult r JOIN FETCH r.session s JOIN FETCH s.template WHERE r.clerkUserId = :userId AND r.passed = true",
           countQuery = "SELECT COUNT(r) FROM TestResult r WHERE r.clerkUserId = :userId AND r.passed = true")
    Page<TestResult> findPassedByClerkUserIdWithSessionAndTemplate(@Param("userId") String clerkUserId, Pageable pageable);

    /**
     * Find user's results for a specific template with session eagerly loaded.
     */
    @Query("SELECT r FROM TestResult r JOIN FETCH r.session s JOIN FETCH s.template WHERE r.clerkUserId = :userId AND s.template.id = :templateId ORDER BY r.completedAt ASC")
    List<TestResult> findByUserAndTemplateWithSession(
            @Param("userId") String clerkUserId,
            @Param("templateId") UUID templateId);

    /**
     * Aggregate user test statistics in a single query.
     * Returns a type-safe projection with totalTests, passedTests, averageScore, bestScore.
     * Avoids multiple COUNT/AVG queries and prevents ClassCastException from raw Object[].
     *
     * @param clerkUserId the user's Clerk ID
     * @return UserStatisticsProjection with aggregate statistics
     */
    @Query("""
        SELECT COUNT(r) AS totalTests,
               COUNT(CASE WHEN r.passed = true THEN 1 END) AS passedTests,
               AVG(r.overallPercentage) AS averageScore,
               MAX(r.overallPercentage) AS bestScore
        FROM TestResult r
        WHERE r.clerkUserId = :userId
        """)
    UserStatisticsProjection getUserStatisticsAggregate(@Param("userId") String clerkUserId);

    /**
     * Aggregate template test statistics in a single query.
     * Returns a type-safe projection with totalAttempts, passedCount, averageScore, minScore, maxScore.
     * Avoids multiple COUNT/AVG queries and prevents ClassCastException from raw Object[].
     *
     * @param templateId the template UUID
     * @return TemplateStatisticsProjection with aggregate statistics
     */
    @Query("""
        SELECT COUNT(r) AS totalAttempts,
               COUNT(CASE WHEN r.passed = true THEN 1 END) AS passedCount,
               AVG(r.overallPercentage) AS averageScore,
               MIN(r.overallPercentage) AS minScore,
               MAX(r.overallPercentage) AS maxScore
        FROM TestResult r
        WHERE r.session.template.id = :templateId
        """)
    TemplateStatisticsProjection getTemplateStatisticsAggregate(@Param("templateId") UUID templateId);

    /**
     * Find recent results with session and template eagerly loaded (for dashboard).
     */
    @Query("SELECT r FROM TestResult r JOIN FETCH r.session s JOIN FETCH s.template ORDER BY r.completedAt DESC LIMIT :limit")
    List<TestResult> findRecentWithSessionAndTemplate(@Param("limit") int limit);

    // ============================================
    // PERCENTILE RECALCULATION QUERIES
    // ============================================

    /**
     * Find recent results for a template completed after a cutoff time.
     * Used for async percentile recalculation to catch concurrent scoring.
     * Eagerly loads session and template to avoid N+1 queries.
     */
    @Query("SELECT r FROM TestResult r JOIN FETCH r.session s JOIN FETCH s.template t " +
           "WHERE t.id = :templateId AND r.completedAt > :cutoff")
    List<TestResult> findByTemplateIdAndCompletedAtAfter(
            @Param("templateId") UUID templateId,
            @Param("cutoff") LocalDateTime cutoff);

    // ============================================
    // ANONYMOUS RESULT QUERIES
    // ============================================

    /**
     * Find anonymous results for a template (owner view).
     * Joins through session to filter by null clerkUserId.
     * Eagerly loads session, template, and share link for efficient DTO mapping.
     *
     * @param templateId The template UUID
     * @param pageable   Pagination parameters
     * @return Page of anonymous results
     */
    @Query(value = "SELECT r FROM TestResult r " +
                   "JOIN FETCH r.session s " +
                   "JOIN FETCH s.template t " +
                   "LEFT JOIN FETCH s.shareLink " +
                   "WHERE t.id = :templateId AND s.clerkUserId IS NULL " +
                   "ORDER BY r.completedAt DESC",
           countQuery = "SELECT COUNT(r) FROM TestResult r " +
                        "WHERE r.session.template.id = :templateId " +
                        "AND r.session.clerkUserId IS NULL")
    Page<TestResult> findAnonymousByTemplateId(
            @Param("templateId") UUID templateId,
            Pageable pageable);

    /**
     * Find results for sessions created via a specific share link.
     * Eagerly loads session and template for analytics.
     *
     * @param shareLinkId The share link UUID
     * @return List of results from sessions using this link
     */
    @Query("SELECT r FROM TestResult r " +
           "JOIN FETCH r.session s " +
           "JOIN FETCH s.template " +
           "WHERE s.shareLink.id = :shareLinkId " +
           "ORDER BY r.completedAt DESC")
    List<TestResult> findByShareLinkId(@Param("shareLinkId") UUID shareLinkId);

    /**
     * Find results by share link with pagination.
     *
     * @param shareLinkId The share link UUID
     * @param pageable    Pagination parameters
     * @return Page of results
     */
    @Query(value = "SELECT r FROM TestResult r " +
                   "JOIN FETCH r.session s " +
                   "JOIN FETCH s.template " +
                   "WHERE s.shareLink.id = :shareLinkId " +
                   "ORDER BY r.completedAt DESC",
           countQuery = "SELECT COUNT(r) FROM TestResult r " +
                        "WHERE r.session.shareLink.id = :shareLinkId")
    Page<TestResult> findByShareLinkIdWithPagination(
            @Param("shareLinkId") UUID shareLinkId,
            Pageable pageable);

    /**
     * Count anonymous results for a template.
     *
     * @param templateId The template UUID
     * @return Number of anonymous results
     */
    @Query("SELECT COUNT(r) FROM TestResult r " +
           "WHERE r.session.template.id = :templateId " +
           "AND r.session.clerkUserId IS NULL")
    long countAnonymousByTemplateId(@Param("templateId") UUID templateId);

    /**
     * Count results from a specific share link.
     *
     * @param shareLinkId The share link UUID
     * @return Number of results
     */
    @Query("SELECT COUNT(r) FROM TestResult r " +
           "WHERE r.session.shareLink.id = :shareLinkId")
    long countByShareLinkId(@Param("shareLinkId") UUID shareLinkId);

    /**
     * Calculate average score for anonymous results on a template.
     *
     * @param templateId The template UUID
     * @return Average percentage score or null if no results
     */
    @Query("SELECT AVG(r.overallPercentage) FROM TestResult r " +
           "WHERE r.session.template.id = :templateId " +
           "AND r.session.clerkUserId IS NULL")
    Double calculateAverageAnonymousScoreByTemplateId(@Param("templateId") UUID templateId);

    /**
     * Calculate pass rate for anonymous results on a template.
     *
     * @param templateId The template UUID
     * @return Pass rate percentage or null if no results
     */
    @Query("SELECT (COUNT(CASE WHEN r.passed = true THEN 1 END) * 100.0 / COUNT(*)) " +
           "FROM TestResult r " +
           "WHERE r.session.template.id = :templateId " +
           "AND r.session.clerkUserId IS NULL")
    Double calculateAnonymousPassRateByTemplateId(@Param("templateId") UUID templateId);

    /**
     * Find anonymous result by ID with full eager loading.
     * Used for detailed result view by template owner.
     *
     * @param resultId The result UUID
     * @return Optional containing the result if found and anonymous
     */
    @Query("SELECT r FROM TestResult r " +
           "JOIN FETCH r.session s " +
           "JOIN FETCH s.template " +
           "LEFT JOIN FETCH s.shareLink " +
           "WHERE r.id = :resultId AND s.clerkUserId IS NULL")
    Optional<TestResult> findAnonymousByIdWithSessionAndTemplate(@Param("resultId") UUID resultId);

    // ============================================
    // SUBSCALE PERCENTILE QUERIES (JSONB)
    // ============================================
    // TODO: These JSONB array queries cannot effectively use GIN indexes because they
    // require value extraction and numeric comparison (not just containment checks).
    // jsonb_path_query_first with @@ only supports existence/containment, not aggregation.
    // Future optimization: denormalize competency_scores into a dedicated
    // `test_result_competency_scores` table with proper indexes on (template_id, competency_id, percentage).

    /**
     * Count results where a specific competency's percentage score is below a threshold.
     * Uses JSONB array element extraction to query nested competency_scores.
     *
     * @param templateId   The template to filter by
     * @param competencyId The competency UUID as string (for JSONB comparison)
     * @param score        The score threshold
     * @return Count of results with lower competency percentage
     */
    @Query(value = """
        SELECT COUNT(*) FROM test_results tr
        JOIN test_sessions ts ON tr.session_id = ts.id
        WHERE ts.template_id = :templateId
        AND tr.competency_scores IS NOT NULL
        AND EXISTS (
            SELECT 1 FROM jsonb_array_elements(tr.competency_scores) elem
            WHERE elem->>'competencyId' = :competencyId
            AND (elem->>'percentage')::numeric < :score
        )
        """, nativeQuery = true)
    Long countCompetencyScoresBelow(
            @Param("templateId") UUID templateId,
            @Param("competencyId") String competencyId,
            @Param("score") double score);

    /**
     * Count total results that have a score for a specific competency.
     *
     * @param templateId   The template to filter by
     * @param competencyId The competency UUID as string
     * @return Total count of results containing this competency score
     */
    @Query(value = """
        SELECT COUNT(*) FROM test_results tr
        JOIN test_sessions ts ON tr.session_id = ts.id
        WHERE ts.template_id = :templateId
        AND tr.competency_scores IS NOT NULL
        AND EXISTS (
            SELECT 1 FROM jsonb_array_elements(tr.competency_scores) elem
            WHERE elem->>'competencyId' = :competencyId
        )
        """, nativeQuery = true)
    Long countCompetencyScoresTotal(
            @Param("templateId") UUID templateId,
            @Param("competencyId") String competencyId);

    // ============================================
    // HISTORICAL STATISTICS QUERIES (for CI calculation)
    // ============================================

    /**
     * Calculate standard deviation of percentage scores for a specific competency across all results.
     * Uses JSONB array element extraction. Returns null if fewer than 30 results exist (insufficient sample).
     *
     * @param competencyId The competency UUID as string (for JSONB comparison)
     * @return Population standard deviation, or null if sample too small
     */
    @Query(value = """
        SELECT CASE WHEN COUNT(*) >= 30 THEN STDDEV_POP(score) ELSE NULL END
        FROM (
            SELECT (elem->>'percentage')::numeric AS score
            FROM test_results tr
            JOIN test_sessions ts ON tr.session_id = ts.id
            CROSS JOIN LATERAL jsonb_array_elements(tr.competency_scores) elem
            WHERE tr.competency_scores IS NOT NULL
            AND elem->>'competencyId' = :competencyId
        ) sub
        """, nativeQuery = true)
    Double calculateCompetencyScoreSD(@Param("competencyId") String competencyId);

    /**
     * Count the number of results that have a score for a specific competency.
     * Used by ConfidenceIntervalCalculator to determine sample size for SD estimation.
     *
     * @param competencyId The competency UUID as string (for JSONB comparison)
     * @return Number of results containing this competency score
     */
    @Query(value = """
        SELECT COUNT(*)
        FROM (
            SELECT 1
            FROM test_results tr
            CROSS JOIN LATERAL jsonb_array_elements(tr.competency_scores) elem
            WHERE tr.competency_scores IS NOT NULL
            AND elem->>'competencyId' = :competencyId
        ) sub
        """, nativeQuery = true)
    Long countCompetencyScoreSamples(@Param("competencyId") String competencyId);

    /**
     * Calculate standard deviation of percentage scores for a specific competency,
     * regardless of sample size. Returns null only if no data exists.
     * Used with countCompetencyScoreSamples() for bootstrap SD estimation.
     *
     * @param competencyId The competency UUID as string (for JSONB comparison)
     * @return Population standard deviation, or null if no data
     */
    @Query(value = """
        SELECT STDDEV_POP(score)
        FROM (
            SELECT (elem->>'percentage')::numeric AS score
            FROM test_results tr
            CROSS JOIN LATERAL jsonb_array_elements(tr.competency_scores) elem
            WHERE tr.competency_scores IS NOT NULL
            AND elem->>'competencyId' = :competencyId
        ) sub
        """, nativeQuery = true)
    Double calculateCompetencyScoreSDUnbounded(@Param("competencyId") String competencyId);

    // ============================================
    // COMPARISON QUERIES
    // ============================================

    /**
     * Fetch multiple results by ID list with session and template eagerly loaded.
     * Used for candidate comparison mode.
     *
     * @param resultIds List of result UUIDs to fetch
     * @return List of results with session and template eagerly loaded
     */
    @Query("SELECT r FROM TestResult r JOIN FETCH r.session s JOIN FETCH s.template t WHERE r.id IN :resultIds")
    List<TestResult> findAllByIdInWithSessionAndTemplate(@Param("resultIds") List<UUID> resultIds);

    /**
     * Calculate average score for results from a specific share link.
     */
    @Query("SELECT AVG(r.overallPercentage) FROM TestResult r WHERE r.session.shareLink.id = :shareLinkId")
    Double calculateAverageScoreByShareLinkId(@Param("shareLinkId") UUID shareLinkId);

    /**
     * Calculate pass rate for results from a specific share link.
     */
    @Query("SELECT (COUNT(CASE WHEN r.passed = true THEN 1 END) * 100.0 / NULLIF(COUNT(*), 0)) FROM TestResult r WHERE r.session.shareLink.id = :shareLinkId")
    Double calculatePassRateByShareLinkId(@Param("shareLinkId") UUID shareLinkId);
}
