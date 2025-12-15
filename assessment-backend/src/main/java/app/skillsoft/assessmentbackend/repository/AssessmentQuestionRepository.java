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
    
    /**
     * Find questions by metadata tag (JSONB filtering).
     * Used for Smart Assessment context filtering in Scenario A (Universal Baseline).
     * 
     * Example: findByMetadataTag("GENERAL") returns context-neutral questions
     * suitable for the Competency Passport assessment.
     * 
     * PostgreSQL JSONB operator: metadata->'tags' @> '["GENERAL"]'::jsonb
     * Checks if the tags array contains the specified tag.
     * 
     * @param tag The tag to filter by (GENERAL, IT, SALES, FINANCE, JUNIOR, MID, SENIOR, etc.)
     * @return List of active questions with the specified tag in metadata.tags
     */
    @Query(value = """
        SELECT * FROM assessment_questions q
        WHERE q.is_active = true
        AND q.metadata->'tags' @> CAST(:tag AS jsonb)
        ORDER BY q.order_index
        """, nativeQuery = true)
    List<AssessmentQuestion> findByMetadataTag(@Param("tag") String tag);
    
    /**
     * Find questions by behavioral indicator ID and metadata tag.
     * Combines indicator filtering with tag-based context filtering.
     * 
     * @param indicatorId The behavioral indicator UUID
     * @param tag The tag to filter by (e.g., "GENERAL", "IT")
     * @return List of active questions for the indicator with the specified tag
     */
    @Query(value = """
        SELECT * FROM assessment_questions q
        WHERE q.behavioral_indicator_id = CAST(:indicatorId AS uuid)
        AND q.is_active = true
        AND q.metadata->'tags' @> CAST(:tag AS jsonb)
        ORDER BY q.order_index
        """, nativeQuery = true)
    List<AssessmentQuestion> findByIndicatorIdAndMetadataTag(
        @Param("indicatorId") String indicatorId, 
        @Param("tag") String tag
    );

    /**
     * Find questions for Scenario A (Universal Baseline / Competency Passport).
     *
     * This query implements Smart Assessment Strategy-Based Selection:
     * 1. Linked to specific Competency (indicator.competency_id)
     * 2. Belong to Universal Indicator (indicator.context_scope = 'UNIVERSAL' or NULL for backward compatibility)
     * 3. Have 'GENERAL' tag in metadata (context-neutral questions)
     *
     * Used by TestSessionService to ensure construct validity in Scenario A assessments.
     * By filtering for UNIVERSAL + GENERAL, we guarantee context neutrality - measuring
     * transferable soft skills rather than role-specific knowledge.
     *
     * Note: Treats NULL context_scope as UNIVERSAL for backward compatibility with existing data.
     *
     * @param competencyId The competency UUID to filter questions by
     * @param limit Maximum number of questions to return (randomized)
     * @return List of context-neutral questions suitable for Universal Baseline assessment
     */
    @Query(value = """
        SELECT q.* FROM assessment_questions q
        JOIN behavioral_indicators bi ON q.behavioral_indicator_id = bi.id
        WHERE bi.competency_id = :competencyId
          AND (bi.context_scope = 'UNIVERSAL' OR bi.context_scope IS NULL)
          AND q.is_active = true
          AND (q.metadata IS NULL OR q.metadata -> 'tags' IS NULL OR q.metadata -> 'tags' @> '["GENERAL"]'::jsonb)
        ORDER BY random()
        LIMIT :limit
        """, nativeQuery = true)
    List<AssessmentQuestion> findUniversalQuestions(
        @Param("competencyId") UUID competencyId,
        @Param("limit") int limit
    );

    /**
     * Find ANY active questions for a competency (fallback query).
     * Used when strict UNIVERSAL + GENERAL filtering returns no results.
     * This ensures test sessions can still be created with available questions.
     *
     * @param competencyId The competency UUID
     * @param limit Maximum number of questions to return
     * @return List of active questions for the competency
     */
    @Query(value = """
        SELECT q.* FROM assessment_questions q
        JOIN behavioral_indicators bi ON q.behavioral_indicator_id = bi.id
        WHERE bi.competency_id = :competencyId
          AND q.is_active = true
        ORDER BY random()
        LIMIT :limit
        """, nativeQuery = true)
    List<AssessmentQuestion> findAnyActiveQuestionsForCompetency(
        @Param("competencyId") UUID competencyId,
        @Param("limit") int limit
    );

    /**
     * Count all active questions for a competency.
     * Used for diagnostic purposes to check question availability.
     *
     * @param competencyId The competency UUID
     * @return Count of active questions
     */
    @Query(value = """
        SELECT COUNT(q.*) FROM assessment_questions q
        JOIN behavioral_indicators bi ON q.behavioral_indicator_id = bi.id
        WHERE bi.competency_id = :competencyId
          AND q.is_active = true
        """, nativeQuery = true)
    long countActiveQuestionsForCompetency(@Param("competencyId") UUID competencyId);

    /**
     * Diagnostic query to get question availability breakdown for a competency.
     * Returns counts by: total, active, with GENERAL tag, with UNIVERSAL indicator.
     *
     * @param competencyId The competency UUID
     * @return Object[] with [totalCount, activeCount, generalTagCount, universalIndicatorCount]
     */
    @Query(value = """
        SELECT
            COUNT(q.id) as total_count,
            COUNT(CASE WHEN q.is_active = true THEN 1 END) as active_count,
            COUNT(CASE WHEN q.is_active = true AND q.metadata -> 'tags' @> '["GENERAL"]'::jsonb THEN 1 END) as general_tag_count,
            COUNT(CASE WHEN q.is_active = true AND (bi.context_scope = 'UNIVERSAL' OR bi.context_scope IS NULL) THEN 1 END) as universal_indicator_count,
            COUNT(CASE WHEN q.is_active = true AND (bi.context_scope = 'UNIVERSAL' OR bi.context_scope IS NULL)
                       AND (q.metadata IS NULL OR q.metadata -> 'tags' IS NULL OR q.metadata -> 'tags' @> '["GENERAL"]'::jsonb) THEN 1 END) as scenario_a_eligible_count
        FROM assessment_questions q
        JOIN behavioral_indicators bi ON q.behavioral_indicator_id = bi.id
        WHERE bi.competency_id = :competencyId
        """, nativeQuery = true)
    Object[] getQuestionAvailabilityDiagnostics(@Param("competencyId") UUID competencyId);
}
