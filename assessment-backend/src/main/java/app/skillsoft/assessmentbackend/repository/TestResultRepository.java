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
}
