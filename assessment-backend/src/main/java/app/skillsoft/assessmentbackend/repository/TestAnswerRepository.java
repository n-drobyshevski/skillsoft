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
}
