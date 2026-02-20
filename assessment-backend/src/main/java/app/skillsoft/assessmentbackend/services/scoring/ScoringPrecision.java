package app.skillsoft.assessmentbackend.services.scoring;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility class for scoring threshold comparisons with controlled floating-point precision.
 *
 * IEEE 754 double arithmetic can produce micro-errors (e.g., 0.49999999999999994 instead of 0.5)
 * which cause incorrect pass/fail decisions at boundary values. This utility rounds values to
 * 4 decimal places before comparison, eliminating floating-point noise while preserving
 * meaningful precision for psychometric scoring.
 *
 * Usage: Apply at threshold comparison points only, NOT to intermediate calculations,
 * to preserve calculation precision throughout the scoring pipeline.
 *
 * @see app.skillsoft.assessmentbackend.services.scoring.impl.JobFitScoringStrategy
 * @see app.skillsoft.assessmentbackend.services.scoring.impl.OverviewScoringStrategy
 * @see app.skillsoft.assessmentbackend.services.scoring.impl.TeamFitScoringStrategy
 */
public final class ScoringPrecision {

    /** Number of decimal places to retain for threshold comparisons. */
    private static final int SCALE = 4;

    private ScoringPrecision() {
        // Utility class - prevent instantiation
    }

    /**
     * Round a double value to 4 decimal places using HALF_UP rounding.
     *
     * Examples:
     * <ul>
     *   <li>0.49995 -> 0.5</li>
     *   <li>0.49994 -> 0.4999</li>
     *   <li>0.65000000001 -> 0.65</li>
     *   <li>0.64999999999 -> 0.65</li>
     * </ul>
     *
     * @param value the raw double value
     * @return value rounded to 4 decimal places
     */
    public static double round4(double value) {
        return BigDecimal.valueOf(value)
                .setScale(SCALE, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * Compare two double values after rounding both to 4 decimal places.
     *
     * Equivalent to {@code round4(value) >= round4(threshold)} but more readable
     * at call sites where the intent is a threshold check.
     *
     * @param value     the score value to check
     * @param threshold the threshold to compare against
     * @return true if the rounded value meets or exceeds the rounded threshold
     */
    public static boolean meetsThreshold(double value, double threshold) {
        return round4(value) >= round4(threshold);
    }
}
