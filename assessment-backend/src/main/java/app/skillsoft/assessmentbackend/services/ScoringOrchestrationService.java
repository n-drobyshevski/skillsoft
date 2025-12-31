package app.skillsoft.assessmentbackend.services;

import app.skillsoft.assessmentbackend.domain.dto.TestResultDto;

import java.util.UUID;

/**
 * Service responsible for orchestrating test scoring in a separate transaction.
 *
 * This service isolates scoring calculation from session completion to ensure:
 * 1. Session completion commits independently (session is marked COMPLETED first)
 * 2. Scoring failures don't roll back the session state
 * 3. Retry logic can be applied specifically to scoring operations
 *
 * Transaction Flow:
 * <pre>
 * completeSession():
 *   TX #1: session.complete() -> COMMIT  (Session is safe!)
 *
 *   | calls
 *   v
 *
 * calculateAndSaveResult() [REQUIRES_NEW]:
 *   TX #2: scoring logic -> COMMIT (or retry if fails)
 * </pre>
 *
 * If scoring fails after all retries, a PENDING result is created
 * that can be retried later by a scheduled job.
 */
public interface ScoringOrchestrationService {

    /**
     * Calculate and save test result in a NEW transaction.
     * This ensures session completion is independent of scoring success.
     *
     * Uses Resilience4j @Retry for transient failure handling:
     * - Max 3 attempts with exponential backoff
     * - Retries on DataAccessException, SQLException
     * - Falls back to creating PENDING result if all retries fail
     *
     * @param sessionId The ID of the completed test session to score
     * @return TestResultDto with the calculated results (or PENDING status if scoring failed)
     * @throws app.skillsoft.assessmentbackend.exception.ResourceNotFoundException if session not found
     */
    TestResultDto calculateAndSaveResult(UUID sessionId);
}
