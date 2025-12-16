package app.skillsoft.assessmentbackend.repository;

import app.skillsoft.assessmentbackend.domain.entities.TestAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestAnswerRepository extends JpaRepository<TestAnswer, UUID> {

    /**
     * Find all answers for a specific session
     */
    List<TestAnswer> findBySession_Id(UUID sessionId);

    /**
     * Find all answers for a session ordered by answered time
     */
    List<TestAnswer> findBySession_IdOrderByAnsweredAtAsc(UUID sessionId);

    /**
     * Find answer for a specific question in a session
     */
    Optional<TestAnswer> findBySession_IdAndQuestion_Id(UUID sessionId, UUID questionId);

    /**
     * Check if an answer exists for a question in a session
     */
    boolean existsBySession_IdAndQuestion_Id(UUID sessionId, UUID questionId);

    /**
     * Count answered (not skipped) questions in a session
     */
    @Query("SELECT COUNT(a) FROM TestAnswer a WHERE a.session.id = :sessionId AND a.isSkipped = false AND a.answeredAt IS NOT NULL")
    long countAnsweredBySessionId(@Param("sessionId") UUID sessionId);

    /**
     * Count skipped questions in a session
     */
    long countBySession_IdAndIsSkippedTrue(UUID sessionId);

    /**
     * Count total answers in a session
     */
    long countBySession_Id(UUID sessionId);

    /**
     * Find all skipped answers in a session
     */
    List<TestAnswer> findBySession_IdAndIsSkippedTrue(UUID sessionId);

    /**
     * Find all answered (not skipped) answers in a session
     */
    @Query("SELECT a FROM TestAnswer a WHERE a.session.id = :sessionId AND a.isSkipped = false AND a.answeredAt IS NOT NULL")
    List<TestAnswer> findAnsweredBySessionId(@Param("sessionId") UUID sessionId);

    /**
     * Calculate total score for a session
     */
    @Query("SELECT COALESCE(SUM(a.score), 0) FROM TestAnswer a WHERE a.session.id = :sessionId AND a.score IS NOT NULL")
    Double sumScoreBySessionId(@Param("sessionId") UUID sessionId);

    /**
     * Calculate max possible score for a session
     */
    @Query("SELECT COALESCE(SUM(a.maxScore), 0) FROM TestAnswer a WHERE a.session.id = :sessionId AND a.maxScore IS NOT NULL")
    Double sumMaxScoreBySessionId(@Param("sessionId") UUID sessionId);

    /**
     * Calculate total time spent in a session
     */
    @Query("SELECT COALESCE(SUM(a.timeSpentSeconds), 0) FROM TestAnswer a WHERE a.session.id = :sessionId")
    Integer sumTimeSpentBySessionId(@Param("sessionId") UUID sessionId);

    /**
     * Delete all answers for a session
     */
    void deleteBySession_Id(UUID sessionId);

    /**
     * Find answers for questions related to a specific behavioral indicator
     * (useful for per-indicator scoring)
     */
    @Query("SELECT a FROM TestAnswer a WHERE a.session.id = :sessionId AND a.question.behavioralIndicator.id = :indicatorId")
    List<TestAnswer> findBySessionIdAndBehavioralIndicatorId(
            @Param("sessionId") UUID sessionId, 
            @Param("indicatorId") UUID indicatorId);

    /**
     * Find answers for questions related to a specific competency
     * (useful for per-competency scoring)
     */
    @Query("SELECT a FROM TestAnswer a WHERE a.session.id = :sessionId AND a.question.behavioralIndicator.competency.id = :competencyId")
    List<TestAnswer> findBySessionIdAndCompetencyId(
            @Param("sessionId") UUID sessionId,
            @Param("competencyId") UUID competencyId);

    // ============================================
    // PSYCHOMETRIC ANALYSIS QUERIES
    // ============================================

    /**
     * Count total responses for a specific question across all sessions.
     * Used to determine if sufficient data exists for psychometric analysis.
     */
    @Query("""
        SELECT COUNT(a) FROM TestAnswer a
        WHERE a.question.id = :questionId
        AND a.isSkipped = false
        AND a.score IS NOT NULL
        """)
    long countByQuestion_Id(@Param("questionId") UUID questionId);

    /**
     * Find all answers for a specific question across all sessions.
     * Used for item-level psychometric analysis.
     */
    @Query("""
        SELECT a FROM TestAnswer a
        WHERE a.question.id = :questionId
        AND a.isSkipped = false
        AND a.score IS NOT NULL
        """)
    List<TestAnswer> findAllByQuestionId(@Param("questionId") UUID questionId);

    /**
     * Get item score paired with total test score for discrimination calculation.
     * Returns pairs of (normalized item score, total test percentage) for Point-Biserial correlation.
     *
     * Note: Uses native query for efficient aggregation.
     */
    @Query(value = """
        SELECT
            CASE WHEN a.max_score > 0 THEN a.score / a.max_score ELSE 0 END as item_score,
            r.overall_percentage / 100.0 as total_score
        FROM test_answers a
        JOIN test_sessions s ON a.session_id = s.id
        JOIN test_results r ON r.session_id = s.id
        WHERE a.question_id = :questionId
        AND a.is_skipped = false
        AND a.score IS NOT NULL
        AND r.overall_percentage IS NOT NULL
        """, nativeQuery = true)
    List<Object[]> findItemTotalScorePairs(@Param("questionId") UUID questionId);

    /**
     * Get distractor selection distribution for MCQ/SJT questions.
     * Returns option ID and selection count for analyzing distractor effectiveness.
     *
     * Note: Uses native query for JSONB array element extraction.
     */
    @Query(value = """
        SELECT
            option_id,
            COUNT(*) as selection_count
        FROM test_answers a,
             LATERAL jsonb_array_elements_text(a.selected_option_ids) as option_id
        WHERE a.question_id = :questionId
        AND a.is_skipped = false
        AND a.selected_option_ids IS NOT NULL
        GROUP BY option_id
        """, nativeQuery = true)
    List<Object[]> getDistractorDistribution(@Param("questionId") UUID questionId);

    /**
     * Find all answers for questions belonging to a competency (across all sessions).
     * Used for competency-level Cronbach's Alpha calculation.
     */
    @Query("""
        SELECT a FROM TestAnswer a
        WHERE a.question.behavioralIndicator.competency.id = :competencyId
        AND a.isSkipped = false
        AND a.score IS NOT NULL
        """)
    List<TestAnswer> findAllByCompetencyId(@Param("competencyId") UUID competencyId);

    /**
     * Get session IDs with complete answers for a competency (for reliability analysis).
     * Returns sessions where all questions for the competency were answered.
     */
    @Query("""
        SELECT DISTINCT a.session.id FROM TestAnswer a
        WHERE a.question.behavioralIndicator.competency.id = :competencyId
        AND a.isSkipped = false
        AND a.score IS NOT NULL
        """)
    List<UUID> findSessionsWithAnswersForCompetency(@Param("competencyId") UUID competencyId);

    /**
     * Get answer score matrix for Cronbach's Alpha calculation.
     * Returns normalized scores grouped by session and question.
     */
    @Query(value = """
        SELECT
            a.session_id,
            a.question_id,
            CASE WHEN a.max_score > 0 THEN a.score / a.max_score ELSE 0 END as normalized_score
        FROM test_answers a
        JOIN assessment_questions q ON a.question_id = q.id
        JOIN behavioral_indicators bi ON q.behavioral_indicator_id = bi.id
        WHERE bi.competency_id = :competencyId
        AND a.is_skipped = false
        AND a.score IS NOT NULL
        ORDER BY a.session_id, a.question_id
        """, nativeQuery = true)
    List<Object[]> getScoreMatrixForCompetency(@Param("competencyId") UUID competencyId);

    /**
     * Get answer scores for a specific session and competency.
     * Used in scoring strategies and reliability analysis.
     */
    @Query("""
        SELECT a FROM TestAnswer a
        WHERE a.session.id = :sessionId
        AND a.question.behavioralIndicator.competency.id = :competencyId
        AND a.isSkipped = false
        ORDER BY a.question.id
        """)
    List<TestAnswer> findAnsweredBySessionAndCompetency(
            @Param("sessionId") UUID sessionId,
            @Param("competencyId") UUID competencyId);

    /**
     * Count distinct sessions that have answered a specific question.
     * Used to track response count for item statistics.
     */
    @Query("""
        SELECT COUNT(DISTINCT a.session.id) FROM TestAnswer a
        WHERE a.question.id = :questionId
        AND a.isSkipped = false
        AND a.score IS NOT NULL
        """)
    long countDistinctSessionsByQuestionId(@Param("questionId") UUID questionId);
}
