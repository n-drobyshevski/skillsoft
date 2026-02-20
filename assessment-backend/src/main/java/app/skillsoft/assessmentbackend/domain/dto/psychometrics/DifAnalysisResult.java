package app.skillsoft.assessmentbackend.domain.dto.psychometrics;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Results of Differential Item Functioning (DIF) analysis for a set of items.
 * Uses the Mantel-Haenszel method with ETS delta classification.
 * <p>
 * DIF occurs when examinees from different groups with the same overall ability
 * have different probabilities of answering an item correctly. Detecting DIF is
 * critical for ensuring test fairness and legal defensibility.
 * <p>
 * Reference: Holland, P.W. & Thayer, D.T. (1988). Differential Item Functioning
 * and the Mantel-Haenszel Procedure. In H. Wainer & H.I. Braun (Eds.),
 * Test Validity (pp. 129-145). Lawrence Erlbaum Associates.
 */
public record DifAnalysisResult(
    UUID competencyId,
    String focalGroupLabel,
    String referenceGroupLabel,
    int focalGroupSize,
    int referenceGroupSize,
    int totalItems,
    int itemsWithModerateDif,
    int itemsWithLargeDif,
    List<ItemDifResult> itemResults
) {
    /**
     * DIF result for a single item.
     *
     * @param questionId       UUID of the assessment question
     * @param mhOddsRatio      Mantel-Haenszel common odds ratio (alpha_MH)
     * @param etsDelta         ETS delta scale value: -2.35 * ln(alpha_MH)
     * @param mhChiSquare      Mantel-Haenszel chi-square test statistic (1 df)
     * @param pValue           p-value from the MH chi-square test
     * @param classification   ETS DIF classification (A, B, or C)
     * @param direction        Direction of DIF: "favors focal" or "favors reference"
     */
    public record ItemDifResult(
        UUID questionId,
        BigDecimal mhOddsRatio,
        BigDecimal etsDelta,
        BigDecimal mhChiSquare,
        BigDecimal pValue,
        DifClassification classification,
        String direction
    ) {}

    /**
     * ETS DIF classification based on MH D-DIF magnitude.
     * <ul>
     *   <li>A = negligible: |delta| < 1.0 - item functions similarly across groups</li>
     *   <li>B = moderate: 1.0 <= |delta| < 1.5 - requires expert review</li>
     *   <li>C = large: |delta| >= 1.5 - should not be used without strong justification</li>
     * </ul>
     */
    public enum DifClassification {
        A_NEGLIGIBLE,
        B_MODERATE,
        C_LARGE
    }
}
