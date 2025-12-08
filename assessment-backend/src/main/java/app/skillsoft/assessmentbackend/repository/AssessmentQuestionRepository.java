package app.skillsoft.assessmentbackend.repository;

import app.skillsoft.assessmentbackend.domain.entities.AssessmentQuestion;
import app.skillsoft.assessmentbackend.domain.entities.DifficultyLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AssessmentQuestionRepository extends JpaRepository<AssessmentQuestion, UUID> {

    /**
     * Find all assessment questions for a specific behavioral indicator
     */
    List<AssessmentQuestion> findByBehavioralIndicator_Id(UUID behavioralIndicatorId);

    /**
     * Find active questions by behavioral indicator and difficulty.
     */
    List<AssessmentQuestion> findByBehavioralIndicator_IdAndDifficultyLevelAndIsActiveTrue(
            UUID behavioralIndicatorId, 
            DifficultyLevel difficultyLevel
    );

    /**
     * Count questions grouped by competency ID and difficulty level.
     * Efficient single-query for inventory heatmap generation.
     * 
     * Returns Object[] with: [competencyId (UUID), difficultyLevel (String), count (Long)]
     */
    @Query("""
        SELECT bi.competency.id, q.difficultyLevel, COUNT(q)
        FROM AssessmentQuestion q
        JOIN q.behavioralIndicator bi
        WHERE q.isActive = true
        GROUP BY bi.competency.id, q.difficultyLevel
        ORDER BY bi.competency.id, q.difficultyLevel
        """)
    List<Object[]> countQuestionsByCompetencyAndDifficulty();

    /**
     * Count questions for a specific competency grouped by difficulty.
     */
    @Query("""
        SELECT q.difficultyLevel, COUNT(q)
        FROM AssessmentQuestion q
        JOIN q.behavioralIndicator bi
        WHERE bi.competency.id = :competencyId AND q.isActive = true
        GROUP BY q.difficultyLevel
        """)
    List<Object[]> countQuestionsByDifficultyForCompetency(@Param("competencyId") UUID competencyId);

    /**
     * Find active questions for multiple behavioral indicator IDs.
     */
    @Query("""
        SELECT q FROM AssessmentQuestion q
        WHERE q.behavioralIndicator.id IN :indicatorIds AND q.isActive = true
        ORDER BY q.orderIndex
        """)
    List<AssessmentQuestion> findActiveByIndicatorIds(@Param("indicatorIds") List<UUID> indicatorIds);
}
