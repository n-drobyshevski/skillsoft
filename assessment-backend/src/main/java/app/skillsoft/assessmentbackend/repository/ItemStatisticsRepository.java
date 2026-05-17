package app.skillsoft.assessmentbackend.repository;

import app.skillsoft.assessmentbackend.domain.entities.DiscriminationFlag;
import app.skillsoft.assessmentbackend.domain.entities.ItemStatistics;
import app.skillsoft.assessmentbackend.domain.entities.ItemValidityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ItemStatistics entity.
 * Provides access to psychometric metrics for assessment questions.
 */
@Repository
public interface ItemStatisticsRepository extends JpaRepository<ItemStatistics, UUID>, JpaSpecificationExecutor<ItemStatistics> {

    /**
     * Find statistics for a specific question.
     */
    Optional<ItemStatistics> findByQuestion_Id(UUID questionId);

    /**
     * Find all statistics by validity status.
     */
    List<ItemStatistics> findByValidityStatus(ItemValidityStatus status);

    /**
     * Find all statistics by validity status (paginated).
     */
    Page<ItemStatistics> findByValidityStatus(ItemValidityStatus status, Pageable pageable);

    /**
     * Find items flagged for review.
     */
    @Query("SELECT i FROM ItemStatistics i WHERE i.validityStatus = 'FLAGGED_FOR_REVIEW'")
    List<ItemStatistics> findItemsRequiringReview();

    /**
     * Find problematic items (critical or negative discrimination).
     */
    @Query("SELECT i FROM ItemStatistics i WHERE i.discriminationFlag = 'CRITICAL' OR i.discriminationFlag = 'NEGATIVE'")
    List<ItemStatistics> findProblematicItems();

    /**
     * Find items by discrimination flag.
     */
    List<ItemStatistics> findByDiscriminationFlag(DiscriminationFlag flag);

    /**
     * Count items by validity status.
     */
    long countByValidityStatus(ItemValidityStatus status);

    /**
     * Find questions needing recalculation.
     * Returns questions with sufficient responses that haven't been calculated recently.
     */
    @Query("""
        SELECT i.question.id FROM ItemStatistics i
        WHERE i.responseCount >= :minResponses
        AND (i.lastCalculatedAt IS NULL OR i.lastCalculatedAt < :threshold)
        """)
    List<UUID> findQuestionsNeedingRecalculation(
            @Param("minResponses") int minResponses,
            @Param("threshold") LocalDateTime threshold);

    /**
     * Find items for a specific competency (via behavioral indicator).
     */
    @Query("SELECT i FROM ItemStatistics i WHERE i.question.behavioralIndicator.competency.id = :competencyId")
    List<ItemStatistics> findByCompetencyId(@Param("competencyId") UUID competencyId);

    /**
     * Get validity status distribution counts.
     */
    @Query("""
        SELECT i.validityStatus, COUNT(i)
        FROM ItemStatistics i
        GROUP BY i.validityStatus
        """)
    List<Object[]> getValidityDistribution();

    /**
     * Find active items for a behavioral indicator (for test assembly).
     */
    @Query("""
        SELECT i FROM ItemStatistics i
        WHERE i.question.behavioralIndicator.id = :indicatorId
        AND i.validityStatus = 'ACTIVE'
        """)
    List<ItemStatistics> findActiveByIndicatorId(@Param("indicatorId") UUID indicatorId);

    /**
     * Find probation items for a behavioral indicator (for data gathering).
     */
    @Query("""
        SELECT i FROM ItemStatistics i
        WHERE i.question.behavioralIndicator.id = :indicatorId
        AND i.validityStatus = 'PROBATION'
        """)
    List<ItemStatistics> findProbationByIndicatorId(@Param("indicatorId") UUID indicatorId);

    /**
     * Batch-load statistics for multiple questions at once (avoids N+1 queries).
     * Used by quality-aware question selection.
     */
    @Query("SELECT i FROM ItemStatistics i WHERE i.question.id IN :questionIds")
    List<ItemStatistics> findByQuestionIdIn(@Param("questionIds") Collection<UUID> questionIds);

    /**
     * Check if statistics exist for a question.
     */
    boolean existsByQuestion_Id(UUID questionId);

    /**
     * Find items with declining discrimination (current < previous).
     */
    @Query("""
        SELECT i FROM ItemStatistics i
        WHERE i.previousDiscriminationIndex IS NOT NULL
        AND i.discriminationIndex IS NOT NULL
        AND i.discriminationIndex < i.previousDiscriminationIndex
        """)
    List<ItemStatistics> findItemsWithDecliningDiscrimination();

    /**
     * Delete statistics for a question.
     */
    void deleteByQuestion_Id(UUID questionId);

    /**
     * Count items that have been analyzed (recalculated) since a given timestamp.
     * Used by the health report to track items analyzed since the last full audit.
     *
     * @param since The cutoff timestamp
     * @return Number of items with lastCalculatedAt after the given timestamp
     */
    long countByLastCalculatedAtAfter(LocalDateTime since);
}
