package app.skillsoft.assessmentbackend.services.psychometrics;

import app.skillsoft.assessmentbackend.domain.entities.ItemStatistics;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for Item Response Theory (IRT) parameter calibration.
 * <p>
 * Implements the 2-Parameter Logistic (2PL) model using Joint Maximum
 * Likelihood Estimation (JMLE). The 2PL model describes the probability
 * of a correct response as:
 * <p>
 *   P(X=1|theta) = 1 / (1 + exp(-a * (theta - b)))
 * <p>
 * where:
 * <ul>
 *   <li>theta = person ability parameter</li>
 *   <li>a = item discrimination parameter</li>
 *   <li>b = item difficulty parameter</li>
 * </ul>
 * <p>
 * Minimum requirements:
 * <ul>
 *   <li>At least 200 respondents for stable parameter estimates</li>
 *   <li>At least 3 items per competency</li>
 *   <li>Items with extreme p-values (&lt; 0.05 or &gt; 0.95) are excluded</li>
 * </ul>
 */
public interface IrtCalibrationService {

    /**
     * Calibrate IRT parameters for all items in a competency.
     * Updates the irtDiscrimination and irtDifficulty fields in ItemStatistics.
     *
     * @param competencyId The competency to calibrate
     * @return List of updated ItemStatistics with IRT parameters
     * @throws IllegalArgumentException if competency not found or insufficient data
     */
    List<ItemStatistics> calibrateCompetency(UUID competencyId);

    /**
     * Estimate ability (theta) for a respondent given their item responses.
     * Uses maximum likelihood estimation with current IRT parameters.
     *
     * @param questionScores Map of questionId to normalized score (0.0 or 1.0)
     * @return Estimated ability on the theta scale (typically -3 to +3)
     */
    double estimateAbility(Map<UUID, Double> questionScores);

    /**
     * Calibration result containing convergence information.
     */
    record CalibrationResult(
        UUID competencyId,
        int itemCount,
        int respondentCount,
        int iterations,
        boolean converged,
        double maxParameterChange,
        List<ItemCalibration> itemCalibrations
    ) {}

    /**
     * Per-item calibration result with estimated parameters and standard errors.
     */
    record ItemCalibration(
        UUID questionId,
        double discrimination,
        double difficulty,
        double standardErrorA,
        double standardErrorB
    ) {}

    /**
     * Calibrate with detailed result information including convergence diagnostics.
     *
     * @param competencyId The competency to calibrate
     * @return Detailed calibration result with convergence info
     */
    CalibrationResult calibrateWithDetails(UUID competencyId);
}
