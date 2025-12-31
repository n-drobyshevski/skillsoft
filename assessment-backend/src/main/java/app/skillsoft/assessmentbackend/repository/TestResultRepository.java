package app.skillsoft.assessmentbackend.repository;

import app.skillsoft.assessmentbackend.domain.entities.TestResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestResultRepository extends JpaRepository<TestResult, UUID> {

    /**
     * Find result by session ID
     */
    Optional<TestResult> findBySession_Id(UUID sessionId);

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
    @Query("SELECT r FROM TestResult r JOIN FETCH r.session s JOIN FETCH s.template WHERE r.clerkUserId = :userId ORDER BY r.completedAt DESC")
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
     * Find user's results for a specific template with session eagerly loaded.
     */
    @Query("SELECT r FROM TestResult r JOIN FETCH r.session s JOIN FETCH s.template WHERE r.clerkUserId = :userId AND s.template.id = :templateId ORDER BY r.completedAt DESC")
    List<TestResult> findByUserAndTemplateWithSession(
            @Param("userId") String clerkUserId,
            @Param("templateId") UUID templateId);

    /**
     * Aggregate user test statistics in a single query.
     * Returns [totalTests, passedTests, avgScore, bestScore].
     * Avoids multiple COUNT/AVG queries.
     */
    @Query("""
        SELECT COUNT(r),
               COUNT(CASE WHEN r.passed = true THEN 1 END),
               COALESCE(AVG(r.overallPercentage), 0.0),
               COALESCE(MAX(r.overallPercentage), 0.0)
        FROM TestResult r
        WHERE r.clerkUserId = :userId
        """)
    Object[] getUserStatisticsAggregate(@Param("userId") String clerkUserId);

    /**
     * Aggregate template test statistics in a single query.
     * Returns [totalAttempts, passedCount, avgScore, minScore, maxScore].
     */
    @Query("""
        SELECT COUNT(r),
               COUNT(CASE WHEN r.passed = true THEN 1 END),
               COALESCE(AVG(r.overallPercentage), 0.0),
               COALESCE(MIN(r.overallPercentage), 0.0),
               COALESCE(MAX(r.overallPercentage), 0.0)
        FROM TestResult r
        WHERE r.session.template.id = :templateId
        """)
    Object[] getTemplateStatisticsAggregate(@Param("templateId") UUID templateId);

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
}
