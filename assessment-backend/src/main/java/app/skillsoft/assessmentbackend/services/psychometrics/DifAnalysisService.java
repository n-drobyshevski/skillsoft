package app.skillsoft.assessmentbackend.services.psychometrics;

import app.skillsoft.assessmentbackend.domain.dto.psychometrics.DifAnalysisResult;

import java.util.Set;
import java.util.UUID;

/**
 * Service for Differential Item Functioning (DIF) analysis.
 * <p>
 * DIF occurs when examinees from different groups (e.g., gender, ethnicity)
 * with the same ability level have different probabilities of answering an item correctly.
 * <p>
 * Uses the Mantel-Haenszel method, the gold standard for DIF detection in
 * educational and psychological testing (Holland & Thayer, 1988).
 * <p>
 * Classification follows ETS guidelines:
 * <ul>
 *   <li>Category A: |MH D-DIF| < 1.0 (negligible)</li>
 *   <li>Category B: 1.0 <= |MH D-DIF| < 1.5 (moderate, review recommended)</li>
 *   <li>Category C: |MH D-DIF| >= 1.5 (large, item should not be used without justification)</li>
 * </ul>
 * <p>
 * Important: The User entity has no demographic fields. Group membership is
 * provided externally as sets of session IDs, enabling flexible analysis
 * across any grouping dimension without storing sensitive demographic data.
 */
public interface DifAnalysisService {

    /**
     * Perform DIF analysis for all items in a competency.
     *
     * @param competencyId            the competency to analyze
     * @param focalGroupSessionIds    session IDs belonging to the focal (potentially disadvantaged) group
     * @param referenceGroupSessionIds session IDs belonging to the reference group
     * @param focalGroupLabel         human-readable label for the focal group (e.g., "Female")
     * @param referenceGroupLabel     human-readable label for the reference group (e.g., "Male")
     * @return DIF analysis results with per-item classifications
     * @throws IllegalArgumentException if competency not found or insufficient data
     */
    DifAnalysisResult analyzeCompetency(
        UUID competencyId,
        Set<UUID> focalGroupSessionIds,
        Set<UUID> referenceGroupSessionIds,
        String focalGroupLabel,
        String referenceGroupLabel
    );

    /**
     * Perform DIF analysis for a specific set of items.
     * Useful for analyzing items across competency boundaries.
     *
     * @param questionIds              the specific questions to analyze
     * @param focalGroupSessionIds     session IDs belonging to the focal group
     * @param referenceGroupSessionIds session IDs belonging to the reference group
     * @param focalGroupLabel          human-readable label for the focal group
     * @param referenceGroupLabel      human-readable label for the reference group
     * @return DIF analysis results
     * @throws IllegalArgumentException if insufficient data for analysis
     */
    DifAnalysisResult analyzeItems(
        Set<UUID> questionIds,
        Set<UUID> focalGroupSessionIds,
        Set<UUID> referenceGroupSessionIds,
        String focalGroupLabel,
        String referenceGroupLabel
    );
}
